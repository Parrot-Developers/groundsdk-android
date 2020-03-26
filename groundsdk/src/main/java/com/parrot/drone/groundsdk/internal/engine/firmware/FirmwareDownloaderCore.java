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

package com.parrot.drone.groundsdk.internal.engine.firmware;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.HttpUpdateClient;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;
import com.parrot.drone.groundsdk.internal.utility.FirmwareStore;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Downloads firmwares from remote update server.
 * <p>
 * This class is also the implementation class for the {@code FirmwareDownloader} utility.
 */
final class FirmwareDownloaderCore implements FirmwareDownloader {

    /** Firmware engine. */
    @NonNull
    private final FirmwareEngine mEngine;

    /** Queue of firmwares to be downloaded (keys). Each mapping to the set of tasks that depends on it. */
    @NonNull
    private final Map<FirmwareIdentifier, Set<Task>> mDownloadQueue;

    /** Current HTTP firmware download request, {@code null} when not downloading any firmware. */
    @Nullable
    private HttpRequest mCurrentDownload;

    /** Current firmware download progress. */
    @IntRange(from = 0, to = 100)
    private int mCurrentProgress;

    /**
     * Constructor.
     *
     * @param engine firmware engine
     */
    FirmwareDownloaderCore(@NonNull FirmwareEngine engine) {
        mEngine = engine;
        mDownloadQueue = new LinkedHashMap<>();
    }

    /**
     * Downloads firmwares.
     * <p>
     * This method will first try to obtain {@link FirmwareInfo} for each of the specified firmware, by querying
     * the {@link FirmwareStore}. <br>
     * In case it fails to obtain such info for any of the specified firmwares, then nothing happens and this method
     * returns {@code null}; otherwise, it behaves as {@link #download(Collection, FirmwareDownloader.Task.Observer)}.
     *
     * @param firmwares set of firmwares to be downloaded.
     * @param observer  observer to notify when the download task's state changes
     *
     * @return a new task providing progress and status information about the overall download operation. {@code null}
     *         in case any of the requested firmwares is not known by {@link FirmwareEngine}
     */
    @Nullable
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    // this method was originally the main API for the downloader, but proved to be useless in practice as
    // download from FirmwareInfo can be used everywhere. Keep it for two reasons:
    // 1. It may be needed sometime in the future
    // 2. Test cases are implemented against this method; it covers more than download() (from FirmwareInfo) and
    // removing it would require many changes in tests
    FirmwareDownloader.Task downloadFromIds(@NonNull Set<FirmwareIdentifier> firmwares,
                                            @NonNull FirmwareDownloader.Task.Observer observer) {
        LinkedList<FirmwareInfo> request = new LinkedList<>();
        for (FirmwareIdentifier firmware : firmwares) {
            FirmwareStoreEntry entry = mEngine.firmwareStore().getEntry(firmware);
            if (entry == null) {
                return null;
            }
            request.add(entry.getFirmwareInfo());
        }
        return new Task(request, observer).queue();
    }

    @Override
    @NonNull
    public FirmwareDownloader.Task download(@NonNull Collection<FirmwareInfo> firmwares,
                                            @NonNull FirmwareDownloader.Task.Observer observer) {
        return new Task(new LinkedList<>(firmwares), observer).queue();
    }

    /**
     * Queues a firmware for download.
     *
     * @param firmware identifies the firmware to be downloaded
     * @param task     task that requests this firmware
     */
    private void queue(@NonNull FirmwareIdentifier firmware, @NonNull Task task) {
        FirmwareStoreEntry entry = mEngine.firmwareStore().getEntry(firmware);
        if (entry != null && entry.getLocalUri() != null) {
            task.onDownloadSuccess();
        } else {
            Set<Task> tasks = mDownloadQueue.get(firmware);
            if (tasks == null) {
                tasks = new HashSet<>();
                mDownloadQueue.put(firmware, tasks);
            }
            if (tasks.add(task) && tasks.size() == 1) {
                processQueue();
            } else if (isCurrentlyDownloading(firmware)) {
                task.onDownloadProgress(mCurrentProgress);
            }
        }
    }

    /**
     * Un-queues a firmware from download.
     *
     * @param firmware identifies the firmware to be un-queued
     * @param task     tasks that does not requests this firmware anymore
     */
    private void dequeue(@NonNull FirmwareIdentifier firmware, @NonNull Task task) {
        Set<Task> tasks = mDownloadQueue.get(firmware);
        if (tasks != null && tasks.remove(task) && tasks.isEmpty()) {
            if (isCurrentlyDownloading(firmware)) {
                assert mCurrentDownload != null;
                mCurrentDownload.cancel();
            } else {
                mDownloadQueue.remove(firmware);
            }
        }
    }

    /**
     * Processes the download queue.
     * <p>
     * Starts to download next in queue, if any and no firmware is being downloaded currently.
     */
    private void processQueue() {
        if (mCurrentDownload != null || mDownloadQueue.isEmpty()) {
            return;
        }
        //TODO: de-recursive
        FirmwareIdentifier firmware = mDownloadQueue.keySet().iterator().next();
        FirmwareStoreEntry entry = mEngine.firmwareStore().getEntry(firmware);
        if (entry == null) {
            onDownloadFailure(firmware);
        } else if (entry.getLocalUri() != null) {
            onDownloadSuccess(firmware);
        } else {
            URI uri = entry.getRemoteUri();
            HttpUpdateClient client = mEngine.httpClient();
            if (uri == null || client == null) {
                onDownloadFailure(firmware);
            } else {
                // download this entry now.
                File dest = mEngine.persistence().makeLocalFirmwarePath(firmware, uri);
                mCurrentDownload = client.download(uri.toString(), dest, new HttpRequest.ProgressStatusCallback() {

                    @Override
                    public void onRequestProgress(int progress) {
                        mCurrentProgress = progress;
                        onDownloadProgress(firmware);
                    }

                    @Override
                    public void onRequestComplete(@NonNull HttpRequest.Status status, int code) {
                        mCurrentDownload = null;
                        if (status == HttpRequest.Status.SUCCESS) {
                            mEngine.firmwareStore().addLocalFirmware(firmware, dest.toURI());
                            onDownloadSuccess(firmware);
                        } else if (status == HttpRequest.Status.CANCELED) {
                            onDownloadCanceled(firmware);
                        } else {
                            onDownloadFailure(firmware);
                        }
                    }
                });
                mCurrentProgress = 0;
                onDownloadProgress(firmware);
            }
        }
    }

    /**
     * Called back after some firmware has been successfully downloaded.
     *
     * @param firmware identifies the downloaded firmware.
     */
    private void onDownloadSuccess(@NonNull FirmwareIdentifier firmware) {
        //noinspection ConstantConditions: callback always called with a current task set in queue
        for (Task task : mDownloadQueue.remove(firmware)) {
            task.onDownloadSuccess();
        }
        processQueue();
    }

    /**
     * Called back after some firmware download failed.
     *
     * @param firmware identifies the firmware whose download did fail.
     */
    private void onDownloadFailure(@NonNull FirmwareIdentifier firmware) {
        //noinspection ConstantConditions: callback always called with a current task set in queue
        for (Task task : mDownloadQueue.remove(firmware)) {
            task.onDownloadFailure();
        }
        processQueue();
    }

    /**
     * Called back after some firmware download is canceled.
     *
     * @param firmware identifies the firmware whose download was canceled.
     */
    private void onDownloadCanceled(@NonNull FirmwareIdentifier firmware) {
        //noinspection ConstantConditions: callback always called with a current task set in queue
        for (Task task : mDownloadQueue.remove(firmware)) {
            task.onDownloadCanceled();
        }
        processQueue();
    }

    /**
     * Called back after firmware download progress updates.
     *
     * @param firmware identifies the firmware whose download did progress
     */
    private void onDownloadProgress(@NonNull FirmwareIdentifier firmware) {
        //noinspection ConstantConditions: callback always called with a current task set in queue
        for (Task task : mDownloadQueue.get(firmware)) {
            task.onDownloadProgress(mCurrentProgress);
        }
    }

    /**
     * Tells whether some firmware is currently being downloaded.
     * <p>
     * If this method returns true, then it is safe to assume that {@link #mCurrentDownload} is not {@code null}.
     *
     * @param firmware identifies the firmware to test
     *
     * @return {@code true} if the specified firmware is currently being downloaded, otherwise {@code false}
     */
    private boolean isCurrentlyDownloading(@NonNull FirmwareIdentifier firmware) {
        return mCurrentDownload != null && firmware.equals(mDownloadQueue.keySet().iterator().next());
    }

    /**
     * Implementation of a firmware downloader task.
     */
    private class Task implements FirmwareDownloader.Task {

        /** Observer notified when the task state changes. */
        @NonNull
        private final Observer mObserver;

        /** Initial list of requested firmwares, kept for reference, immutable. */
        @NonNull
        private final List<FirmwareInfo> mRequest;

        /** Queue of firmwares to be downloaded (keys). Each mapped to relevant info on that firmware. */
        @NonNull
        private final LinkedList<FirmwareInfo> mQueue;

        /** Task state. */
        @NonNull
        private State mState;

        /** Current firmware download progress. */
        @IntRange(from = 0, to = 100)
        private int mProgress;

        /** {@code true} when there are pending state changes waiting for {@link #notifyUpdated()} call. */
        private boolean mChanged;

        /**
         * Constructor.
         *
         * @param firmwares firmwares to download, owned by the task, so it should be given a copy if appropriate
         * @param observer  observer notified when the task state changes
         */
        Task(@NonNull LinkedList<FirmwareInfo> firmwares, @NonNull Observer observer) {
            if (firmwares.isEmpty()) {
                throw new IllegalArgumentException("No firmwares to download");
            }
            mObserver = observer;
            mRequest = Collections.unmodifiableList(firmwares);
            mQueue = new LinkedList<>(firmwares);
            mState = State.FAILED;
        }

        @NonNull
        @Override
        public State state() {
            return mState;
        }

        @NonNull
        @Override
        public List<FirmwareInfo> requested() {
            return mRequest;
        }

        @NonNull
        @Override
        public List<FirmwareInfo> remaining() {
            return Collections.unmodifiableList(mQueue);
        }

        @Override
        public int currentProgress() {
            return mProgress;
        }

        @Override
        public void cancel() {
            if (mState != State.QUEUED && mState != State.DOWNLOADING) {
                return;
            }

            FirmwareInfo current = mQueue.peek();
            if (current != null) {
                dequeue(current.getFirmware(), this);
            }
            updateState(State.CANCELED);
            notifyUpdated();
        }

        /**
         * Queues the task for download.
         * <p>
         * This basically queues the first firmware in the task for download.
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        Task queue() {
            queueNext();
            return this;
        }

        /**
         * Called back after queued firmware for this task has been successfully downloaded.
         */
        void onDownloadSuccess() {
            updateProgress(100);
            mQueue.poll();
            mChanged = true;
            queueNext();
        }

        /**
         * Called back after some firmware download for this task failed.
         */
        void onDownloadFailure() {
            updateState(State.FAILED);
            notifyUpdated();
        }

        /**
         * Called back after some firmware download for this task is canceled.
         */
        void onDownloadCanceled() {
            updateState(State.CANCELED);
            notifyUpdated();
        }

        /**
         * Called back after queued firmware download progress for this task updates.
         *
         * @param progress firmware download progress
         */
        void onDownloadProgress(int progress) {
            updateState(State.DOWNLOADING);
            updateProgress(progress);
            notifyUpdated();
        }

        /**
         * Queues next firmware to download.
         */
        private void queueNext() {
            FirmwareInfo next = mQueue.peek();
            if (next != null) {
                updateState(State.QUEUED);
                updateProgress(0);
                //TODO: de-recursive
                FirmwareDownloaderCore.this.queue(next.getFirmware(), this);
            } else {
                updateState(State.SUCCESS);
            }
            notifyUpdated();
        }

        /**
         * Updates current task state.
         *
         * @param state new task state
         */
        private void updateState(@NonNull State state) {
            if (mState != state) {
                mState = state;
                mChanged = true;
            }
        }

        /**
         * Updates current download progress.
         *
         * @param progress new download progress
         */
        private void updateProgress(int progress) {
            if (mProgress != progress) {
                mProgress = progress;
                mChanged = true;
            }
        }

        /**
         * Notifies all observers of task state change, iff it did change since last call to this method.
         */
        private void notifyUpdated() {
            if (mChanged) {
                mChanged = false;
                mObserver.onChange(this);
            }
        }
    }
}
