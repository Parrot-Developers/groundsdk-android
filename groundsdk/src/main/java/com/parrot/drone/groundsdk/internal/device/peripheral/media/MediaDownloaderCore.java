/*
 *     Copyright (C) 2019 Parrot Drones SAS
 *
 *     Redistribution and use in source and binary forms, with or without
 *     modification, are permitted provided that the following conditions
 *     are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of the Parrot Company nor the names
 *       of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written
 *       permission.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *     "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *     LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *     FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *     PARROT COMPANY BE LIABLE FOR ANY DIRECT, INDIRECT,
 *     INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *     BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 *     OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 *     AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *     OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *     OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *     SUCH DAMAGE.
 *
 */

package com.parrot.drone.groundsdk.internal.device.peripheral.media;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.media.MediaDownloader;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaTaskStatus;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Implementation of the MediaDownloader task.
 */
class MediaDownloaderCore implements MediaDownloader {

    /**
     * Allows to observe the downloader task changes.
     */
    interface Observer {

        /**
         * Called back when the downloader task changes.
         *
         * @param downloader downloader task which changed
         */
        void onChanged(@NonNull MediaDownloaderCore downloader);
    }

    /** Store from which to download resources. */
    @NonNull
    private final MediaStoreCore mStore;

    /** Rest of entries to be downloaded. Also contains the current entry being downloaded. */
    @NonNull
    private final Queue<DownloadEntry> mPendingEntries;

    /** Observer notified when the status or progress changes. */
    @NonNull
    private final Observer mObserver;

    /** Destination for downloaded resource files. */
    @NonNull
    private final MediaDestinationCore mDest;

    /** Total amount of media containing resources that the task will download. */
    private final int mMediaCount;

    /** Total amount of resources that the task will download. May be greater than {@link #mMediaCount}. */
    private final int mResourceCount;

    /** Total size, in bytes, of all resource files that will be downloaded. */
    private final long mTotalSize;

    /** Current status. */
    @NonNull
    private MediaTaskStatus mStatus;

    /** Current size, in bytes, of all downloaded resources files so far. */
    private long mCurrentDownloadedSize;

    /** Completion percentage for the resource file being currently downloaded. */
    private int mCurrentFileProgress;

    /** Completion percentage for the whole task. */
    private int mCurrentOverallProgress;

    /** Index of currently processed media. */
    private int mMediaIndex;

    /** Index of currently processed resource. */
    private int mResourceIndex;

    /** Downloaded file. {@code null} unless {@link #mStatus} is {@link MediaTaskStatus#FILE_PROCESSED}. */
    @Nullable
    private File mDownloadedFile;

    /** Media being currently processed. */
    @Nullable
    private MediaItem mCurrentMedia;

    /** Media resource being currently downloaded. */
    @Nullable
    private MediaItem.Resource mCurrentResource;

    /** Current download request being processed. */
    @Nullable
    private MediaRequest mCurrentRequest;

    /** Has pending changes waiting for {@link #notifyUpdated()} call. */
    private boolean mChanged;

    /**
     * Constructor.
     *
     * @param resources resources to download
     * @param dest      destination directory where to download resource files
     * @param store     media store to download resources from
     * @param observer  observer notified of progress and status changes
     */
    MediaDownloaderCore(@NonNull Collection<MediaItem.Resource> resources, @NonNull MediaDestinationCore dest,
                        @NonNull MediaStoreCore store, @NonNull Observer observer) {
        mDest = dest;
        mStore = store;
        mObserver = observer;
        mPendingEntries = new LinkedList<>();

        Map<MediaItemCore, Set<MediaResourceCore>> resourcesByMedia = MediaResourceCore.unwrapAsMap(resources);

        long totalSize = 0;
        for (Map.Entry<MediaItemCore, Set<MediaResourceCore>> entry : resourcesByMedia.entrySet()) {
            int index = 0;
            for (MediaResourceCore resource : entry.getValue()) {
                mPendingEntries.add(new DownloadEntry(resource, index++ == 0));
                totalSize += resource.getSize();
            }
        }

        mMediaCount = resourcesByMedia.size();
        mResourceCount = mPendingEntries.size();
        mTotalSize = totalSize;
        mStatus = MediaTaskStatus.RUNNING;
    }

    @NonNull
    @Override
    public MediaTaskStatus getStatus() {
        return mStatus;
    }

    @Override
    public int getTotalMediaCount() {
        return mMediaCount;
    }

    @Override
    public int getCurrentMediaIndex() {
        return mMediaIndex;
    }

    @Override
    public int getTotalProgress() {
        return mCurrentOverallProgress;
    }

    @Override
    public int getTotalResourceCount() {
        return mResourceCount;
    }

    @Override
    public int getCurrentResourceIndex() {
        return mResourceIndex;
    }

    @Override
    public int getCurrentFileProgress() {
        return mCurrentFileProgress;
    }

    @Override
    public File getDownloadedFile() {
        return mDownloadedFile;
    }

    @Nullable
    @Override
    public MediaItem getCurrentMedia() {
        return mCurrentMedia;
    }

    @Nullable
    @Override
    public MediaItem.Resource getCurrentResource() {
        return mCurrentResource;
    }

    /**
     * Executes the download task.
     */
    void execute() {
        downloadNextEntry();
    }

    /**
     * Cancels the whole download operation.
     */
    void cancel() {
        if (mCurrentRequest != null) {
            mCurrentRequest.cancel();
            mCurrentRequest = null;
        }
        mPendingEntries.clear();
    }

    /**
     * A download entry to be processed.
     * <p>
     * Contains the media item to operate on, the resource to be downloaded for that item, and whether the entry
     * corresponds to the last resource to be downloaded for the media item.
     */
    private static final class DownloadEntry {

        /** {@code true} when the entry contains the first resource to be downloaded for the corresponding media. */
        final boolean mFirstInMedia;

        /** The resource to be downloaded. */
        @NonNull
        final MediaResourceCore mResource;

        /**
         * Constructor.
         *
         * @param resource     resource to be downloaded
         * @param firstInMedia whether the entry contains the first resource to be downloaded for the media
         */
        DownloadEntry(@NonNull MediaResourceCore resource, boolean firstInMedia) {
            mResource = resource;
            mFirstInMedia = firstInMedia;
        }
    }

    /**
     * Processes the next entry in the pending download list.
     */
    private void downloadNextEntry() {
        DownloadEntry entry = mPendingEntries.peek(); // only peek to release the media once the request completes
        if (entry == null) {
            updateStatus(MediaTaskStatus.COMPLETE);
            updateCurrentMedia(null);
            updateCurrentResource(null);
        } else {
            String path = mDest.ensurePath();
            if (path == null) {
                mPendingEntries.clear();
                updateStatus(MediaTaskStatus.ERROR);
            } else {
                mCurrentFileProgress = 0;
                mResourceIndex++;
                if (entry.mFirstInMedia) {
                    mMediaIndex++;
                }
                updateStatus(MediaTaskStatus.RUNNING);
                updateCurrentMedia(entry.mResource.getMedia());
                updateCurrentResource(entry.mResource);
                mChanged = true;
                mCurrentRequest = mStore.mBackend.download(entry.mResource, path, mRequestCallback);
            }
        }
        updateDownloadedFile(null);
        notifyUpdated();
    }

    /** Called back upon current request progress and completion. */
    private final MediaRequest.ProgressResultCallback<File> mRequestCallback =
            new MediaRequest.ProgressResultCallback<File>() {

                @Override
                public void onRequestProgress(@IntRange(from = 0, to = 100) int progress) {
                    if (mCurrentRequest != null) { // otherwise the task is canceled or not running yet
                        DownloadEntry entry = mPendingEntries.peek();
                        assert entry != null;
                        mCurrentFileProgress = progress;
                        mCurrentOverallProgress = (int) ((mCurrentDownloadedSize * 100
                                                          + entry.mResource.getSize() * progress) / mTotalSize);
                        mChanged = true;
                        notifyUpdated();
                    }
                }

                @Override
                public void onRequestComplete(@NonNull MediaRequest.Status status, @Nullable File result) {
                    mCurrentRequest = null;
                    if (status == MediaRequest.Status.CANCELED || status == MediaRequest.Status.ABORTED) {
                        mPendingEntries.clear();
                        updateCurrentMedia(null);
                        updateCurrentResource(null);
                        updateStatus(MediaTaskStatus.ERROR);
                        notifyUpdated();
                    } else {
                        if (result != null) {
                            mDest.notifyFileAdded(result);
                        }
                        DownloadEntry entry = mPendingEntries.remove();
                        mCurrentDownloadedSize += entry.mResource.getSize();
                        mCurrentFileProgress = 100;
                        mCurrentOverallProgress = (int) ((mCurrentDownloadedSize * 100) / mTotalSize);
                        if (status == MediaRequest.Status.SUCCESS) {
                            updateDownloadedFile(result);
                            updateStatus(MediaTaskStatus.FILE_PROCESSED);
                            notifyUpdated();
                        }
                        mChanged = true;
                        downloadNextEntry();
                    }

                }
            };

    /**
     * Updates the current status.
     *
     * @param status current status
     */
    private void updateStatus(@NonNull MediaTaskStatus status) {
        if (mStatus != status) {
            mStatus = status;
            mChanged = true;
        }
    }

    /**
     * Updates the downloaded file.
     *
     * @param file downloaded file
     */
    private void updateDownloadedFile(@Nullable File file) {
        if (mDownloadedFile != file) {
            mDownloadedFile = file;
            mChanged = true;
        }
    }

    /**
     * Updates the current downloaded media.
     *
     * @param media current downloaded media
     */
    private void updateCurrentMedia(@Nullable MediaItem media) {
        if (mCurrentMedia != media) {
            mCurrentMedia = media;
            mChanged = true;
        }
    }

    /**
     * Updates the current downloaded resource.
     *
     * @param resource current downloaded resource
     */
    private void updateCurrentResource(@Nullable MediaItem.Resource resource) {
        if (mCurrentResource != resource) {
            mCurrentResource = resource;
            mChanged = true;
        }
    }


    /**
     * Notifies changes made by previously called setters.
     */
    private void notifyUpdated() {
        if (mChanged) {
            mChanged = false;
            mObserver.onChanged(this);
        }
    }
}
