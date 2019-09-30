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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.media.MediaDeleter;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaTaskStatus;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Implementation of the MediaDeleter task.
 */
final class MediaDeleterCore implements MediaDeleter {

    /**
     * Allows to observe the deleter task changes.
     */
    interface Observer {

        /**
         * Called back when the deleter task changes.
         *
         * @param deleter deleter task which changed
         */
        void onChanged(@NonNull MediaDeleterCore deleter);
    }

    /**
     * Represents some deletion operation.
     */
    private interface Deletion {

        /**
         * Requests deletion.
         *
         * @return a request that can be canceled, or {@code null} if the request was processed directly
         */
        @Nullable
        MediaRequest request();
    }

    /** Rest of deletion operations to request. Also contains the currently requested deletion. */
    @NonNull
    private final Queue<Deletion> mPendingEntries;

    /** Observer notified when the status or progress changes. */
    @NonNull
    private final Observer mObserver;

    /** Current delete request being processed. */
    @Nullable
    private MediaRequest mCurrentRequest;

    /** Current status. */
    @NonNull
    private MediaTaskStatus mStatus;

    /** Total amount of media containing resource(s) to delete. */
    private final int mMediaCount;

    /** Index of currently processed media. */
    private int mMediaIndex;

    /** Has pending changes waiting for {@link #notifyUpdated()} call. */
    private boolean mChanged;

    /**
     * Constructor.
     *
     * @param resources media resources to delete
     * @param store     media store to delete resources from
     * @param observer  observer notified of progress and status changes
     */
    MediaDeleterCore(@NonNull Collection<MediaItem.Resource> resources, @NonNull MediaStoreCore store,
                     @NonNull Observer observer) {
        mObserver = observer;
        mPendingEntries = new LinkedList<>();

        Map<MediaItemCore, Set<MediaResourceCore>> resourcesByMedia = MediaResourceCore.unwrapAsMap(resources);

        for (Map.Entry<MediaItemCore, Set<MediaResourceCore>> entry : resourcesByMedia.entrySet()) {
            MediaItemCore media = entry.getKey();
            Set<MediaResourceCore> selectedResources = entry.getValue();
            if (selectedResources.containsAll(media.getResources())) {
                mPendingEntries.add(() -> {
                    mMediaIndex++;
                    mChanged = true;
                    return store.mBackend.delete(media, mRequestCallback);
                });
            } else {
                int index = 0;
                for (MediaResourceCore resource : selectedResources) {
                    boolean firstInMedia = index++ == 0;
                    mPendingEntries.add(() -> {
                        if (firstInMedia) {
                            mMediaIndex++;
                            mChanged = true;
                        }
                        return store.mBackend.delete(resource, mRequestCallback);
                    });
                }
            }
        }

        mMediaCount = resourcesByMedia.size();
        mStatus = MediaTaskStatus.RUNNING;
    }

    @NonNull
    @Override
    public MediaTaskStatus getStatus() {
        return mStatus;
    }

    @Override
    public int getCurrentMediaIndex() {
        return mMediaIndex;
    }

    @Override
    public int getTotalMediaCount() {
        return mMediaCount;
    }

    /**
     * Executes the deletion task.
     */
    void execute() {
        deleteNextEntry();
    }

    /**
     * Cancels the whole delete operation.
     */
    void cancel() {
        if (mCurrentRequest != null) {
            mCurrentRequest.cancel();
            mCurrentRequest = null;
        }
        mPendingEntries.clear();
    }

    /**
     * Deletes the next entry in the pending list.
     */
    private void deleteNextEntry() {
        Deletion next = mPendingEntries.peek(); // only peek to release it once the request completes
        if (next == null) {
            updateStatus(MediaTaskStatus.COMPLETE);
        } else {
            mCurrentRequest = next.request();
        }
        notifyUpdated();
    }

    /** Called back when the current request completes successfully or fails. */
    private final MediaRequest.StatusCallback mRequestCallback = new MediaRequest.StatusCallback() {

        @Override
        public void onRequestComplete(@NonNull MediaRequest.Status status) {
            mCurrentRequest = null;
            if (status == MediaRequest.Status.CANCELED || status == MediaRequest.Status.ABORTED) {
                mPendingEntries.clear();
                updateStatus(MediaTaskStatus.ERROR);
                notifyUpdated();
            } else {
                mPendingEntries.remove();
                deleteNextEntry();
            }
        }
    };

    /**
     * Updates the current status.
     *
     * @param newStatus new status to set
     */
    private void updateStatus(@NonNull MediaTaskStatus newStatus) {
        if (mStatus != newStatus) {
            mStatus = newStatus;
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
