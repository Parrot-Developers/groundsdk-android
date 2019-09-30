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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.Updater;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Core class for Updater. */
public class UpdaterCore extends SingletonComponentCore implements Updater {

    /** Description of Updater. */
    private static final ComponentDescriptor<Peripheral, Updater> DESC =
            ComponentDescriptor.of(Updater.class);

    /** Engine-specific backend for Updater. */
    public interface Backend {

        /**
         * Requests firmware download.
         *
         * @param firmwares firmwares to be downloaded, in order
         * @param observer  observer notified when the download task state changes
         */
        void download(@NonNull Collection<FirmwareInfo> firmwares,
                      @NonNull FirmwareDownloader.Task.Observer observer);

        /**
         * Requests device firmware update.
         *
         * @param firmwares firmwares to be applied for update, in order
         */
        void updateWith(@NonNull Collection<FirmwareInfo> firmwares);

        /**
         * Cancels ongoing firmware update, if any.
         */
        void cancelUpdate();
    }

    /** Engine peripheral backend. */
    @NonNull
    private final Backend mBackend;

    /** Set of reasons why downloading firmware is currently impossible. */
    @NonNull
    private final EnumSet<Download.UnavailabilityReason> mDownloadUnavailabilityReasons;

    /** Set of reasons why applying firmware updates is currently impossible. */
    @NonNull
    private final EnumSet<Update.UnavailabilityReason> mUpdateUnavailabilityReasons;

    /** List of firmwares that must be downloaded so that applying all of them will bring the device up-to-date. */
    @NonNull
    private List<FirmwareInfo> mDownloadableFirmwares;

    /** List of firmwares that may be applied, in order, to update the device. */
    @NonNull
    private List<FirmwareInfo> mApplicableFirmwares;

    /** Current firmware download state. */
    @Nullable
    private DownloadCore mDownload;

    /** Current firmware update state. */
    @Nullable
    private UpdateCore mUpdate;

    /** Ideal version. */
    @Nullable
    private FirmwareVersion mIdealVersion;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public UpdaterCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
        mDownloadableFirmwares = Collections.emptyList();
        mApplicableFirmwares = Collections.emptyList();
        mDownloadUnavailabilityReasons = EnumSet.noneOf(Download.UnavailabilityReason.class);
        mUpdateUnavailabilityReasons = EnumSet.noneOf(Update.UnavailabilityReason.class);
    }

    @NonNull
    @Override
    public List<FirmwareInfo> downloadableFirmwares() {
        return Collections.unmodifiableList(mDownloadableFirmwares);
    }

    @NonNull
    @Override
    public EnumSet<Download.UnavailabilityReason> downloadUnavailabilityReasons() {
        return EnumSet.copyOf(mDownloadUnavailabilityReasons);
    }

    @Override
    public boolean downloadNextFirmware() {
        return !mDownloadableFirmwares.isEmpty() && download(Collections.singletonList(mDownloadableFirmwares.get(0)));
    }

    @Override
    public boolean downloadAllFirmwares() {
        return !mDownloadableFirmwares.isEmpty() && download(mDownloadableFirmwares);
    }

    @Nullable
    @Override
    public Download currentDownload() {
        return mDownload;
    }

    @Override
    public boolean cancelDownload() {
        if (mDownload != null) {
            mDownload.cancel();
            return true;
        }
        return false;
    }

    @NonNull
    @Override
    public List<FirmwareInfo> applicableFirmwares() {
        return Collections.unmodifiableList(mApplicableFirmwares);
    }

    @NonNull
    @Override
    public EnumSet<Update.UnavailabilityReason> updateUnavailabilityReasons() {
        return EnumSet.copyOf(mUpdateUnavailabilityReasons);
    }

    @Override
    public boolean updateToNextFirmware() {
        return !mApplicableFirmwares.isEmpty() && update(Collections.singletonList(mApplicableFirmwares.get(0)));
    }

    @Override
    public boolean updateToLatestFirmware() {
        return !mApplicableFirmwares.isEmpty() && update(mApplicableFirmwares);
    }

    @Nullable
    @Override
    public Update currentUpdate() {
        return mUpdate;
    }

    @Override
    public boolean cancelUpdate() {
        if (mUpdate != null) {
            mBackend.cancelUpdate();
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public FirmwareVersion idealVersion() {
        return mIdealVersion;
    }

    /**
     * Updates the list of downloadable firmwares.
     * <p>
     * Provided list is owned by this method after the call.
     *
     * @param firmwares downloadable firmwares, in application order
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public UpdaterCore updateDownloadableFirmwares(@NonNull List<FirmwareInfo> firmwares) {
        if (!mDownloadableFirmwares.equals(firmwares)) {
            mDownloadableFirmwares = Collections.unmodifiableList(firmwares);
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the list of applicable firmwares.
     * <p>
     * Provided list is owned by this method after the call.
     *
     * @param firmwares applicable firmwares, in application order
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public UpdaterCore updateApplicableFirmwares(@NonNull List<FirmwareInfo> firmwares) {
        if (!mApplicableFirmwares.equals(firmwares)) {
            mApplicableFirmwares = Collections.unmodifiableList(firmwares);
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the set of unavailability reasons for firmware download.
     *
     * @param reasons firmware download unavailability reasons
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public UpdaterCore updateDownloadUnavailabilityReasons(
            @NonNull Collection<Download.UnavailabilityReason> reasons) {
        mChanged |= mDownloadUnavailabilityReasons.retainAll(reasons) | mDownloadUnavailabilityReasons.addAll(reasons);
        return this;
    }

    /**
     * Updates the set of unavailability reasons for firmware update.
     *
     * @param reasons firmware update unavailability reasons
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public UpdaterCore updateUpdateUnavailabilityReasons(
            @NonNull Collection<Update.UnavailabilityReason> reasons) {
        mChanged |= mUpdateUnavailabilityReasons.retainAll(reasons) | mUpdateUnavailabilityReasons.addAll(reasons);
        return this;
    }

    /**
     * Creates a new update state.
     *
     * @param firmwares firmwares that will be applied for this update
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public UpdaterCore beginUpdate(@NonNull Set<FirmwareInfo> firmwares) {
        mUpdate = new UpdateCore(firmwares);
        mChanged = true;
        return this;
    }

    /**
     * Updates the update state.
     *
     * @param state update state
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public UpdaterCore updateUpdateState(@NonNull Update.State state) {
        mChanged |= mUpdate != null && mUpdate.updateState(state);
        return this;
    }

    /**
     * Updates the current firmware update upload progress.
     *
     * @param progress upload progress
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public UpdaterCore updateUploadProgress(int progress) {
        mChanged |= mUpdate != null && mUpdate.updateProgress(progress);
        return this;
    }

    /**
     * Moves the update state to the next firmware to apply.
     * <p>
     * This also resets the update state to {@link Update.State#UPLOADING} and upload progress to {@code 0}.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public UpdaterCore continueUpdate() {
        mChanged |= mUpdate != null && mUpdate.next();
        return this;
    }

    /**
     * Clears the update state.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public UpdaterCore endUpdate() {
        mChanged |= mUpdate != null;
        mUpdate = null;
        return this;
    }

    /**
     * Updates the ideal version.
     *
     * @param version the new version
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public UpdaterCore updateIdealVersion(@Nullable FirmwareVersion version) {
        if (!Objects.equals(mIdealVersion, version)) {
            mIdealVersion = version;
            mChanged = true;
        }
        return this;
    }

    /**
     * Requests firmware(s) download.
     *
     * @param firmwares firmwares to be downloaded, in order
     *
     * @return {@code true} if firmware download did start, otherwise {@code false}
     */
    private boolean download(@NonNull List<FirmwareInfo> firmwares) {
        if (mDownloadUnavailabilityReasons.isEmpty()
            && mDownload == null) {
            mBackend.download(firmwares, mDownloadObserver);
            return true;
        }
        return false;
    }

    /**
     * Requests firmware(s) update.
     *
     * @param firmwares firmwares to be applied, in order
     *
     * @return {@code true} if firmware update did start, otherwise {@code false}
     */
    private boolean update(@NonNull List<FirmwareInfo> firmwares) {
        if (mUpdateUnavailabilityReasons.isEmpty()
            && mUpdate == null) {
            mBackend.updateWith(firmwares);
            return true;
        }
        return false;
    }

    /** Observer notified when the ongoing firmware download task state changes. */
    private final FirmwareDownloader.Task.Observer mDownloadObserver = new FirmwareDownloader.Task.Observer() {

        @Override
        public void onChange(@NonNull FirmwareDownloader.Task task) {
            if (mDownload == null) {
                mDownload = new DownloadCore(task);
            }
            mChanged = true;
            notifyUpdated();
            switch (task.state()) {
                case QUEUED:
                case DOWNLOADING:
                    break;
                case SUCCESS:
                case FAILED:
                case CANCELED:
                    mChanged = true;
                    mDownload = null;
                    notifyUpdated();
                    break;
            }
        }
    };

    /** Implementation of download state. */
    private static final class DownloadCore extends Download {

        /** Firmware(s) download task that this state wraps. */
        @NonNull
        private final FirmwareDownloader.Task mTask;

        /**
         * Constructor.
         *
         * @param task task that informs about firmware(s) download state
         */
        DownloadCore(@NonNull FirmwareDownloader.Task task) {
            mTask = task;
        }

        @NonNull
        @Override
        public FirmwareInfo currentFirmware() {
            return mTask.current();
        }

        @Override
        public int currentFirmwareProgress() {
            return mTask.currentProgress();
        }

        @Override
        public int currentFirmwareIndex() {
            return mTask.currentCount();
        }

        @Override
        public int totalProgress() {
            return mTask.overallProgress();
        }

        @Override
        public int totalFirmwareCount() {
            return mTask.totalCount();
        }

        @NonNull
        @Override
        public State state() {
            switch (mTask.state()) {
                case QUEUED:
                case DOWNLOADING:
                    return State.DOWNLOADING;
                case SUCCESS:
                    return State.SUCCESS;
                case FAILED:
                    return State.FAILED;
                case CANCELED:
                    return State.CANCELED;
            }
            return null;
        }

        /**
         * Cancels the ongoing download.
         */
        void cancel() {
            mTask.cancel();
        }
    }

    /** Implementation of update state. */
    private static final class UpdateCore extends Update {

        /** Firmwares to be applied. */
        private final FirmwareInfo[] mFirmwares;

        /** Index of the firmware currently being applied in {@link #mFirmwares}. */
        private int mIndex;

        /** Current firmware update progress. */
        private int mProgress;

        /** Update task state. */
        @NonNull
        State mState;

        /**
         * Constructor.
         *
         * @param firmwares firmwares that will be applied, in order
         */
        UpdateCore(@NonNull Set<FirmwareInfo> firmwares) {
            mFirmwares = firmwares.toArray(new FirmwareInfo[0]);
            mState = State.UPLOADING;
        }

        @NonNull
        @Override
        public FirmwareInfo currentFirmware() {
            return mFirmwares[mIndex];
        }

        @Override
        public int currentFirmwareProgress() {
            return mProgress;
        }

        @Override
        public int currentFirmwareIndex() {
            return mIndex + 1;
        }

        @Override
        public int totalProgress() {
            long totalSize = 0;
            long doneSize = 0;
            for (int i = 0; i < mFirmwares.length; i++) {
                long size = mFirmwares[i].getSize();
                totalSize += size;
                if (mIndex > i) {
                    doneSize += size;
                } else if (mIndex == i) {
                    doneSize += mProgress * size / 100;
                }
            }
            return totalSize == 0 ? 0 : Math.round(doneSize * 100f / totalSize);
        }

        @Override
        public int totalFirmwareCount() {
            return mFirmwares.length;
        }

        @NonNull
        @Override
        public State state() {
            return mState;
        }

        /**
         * Increments current firmware index.
         * <p>
         * In case the index did increment, then the update state is reset to {@link State#UPLOADING} and progress is
         * reset to {@code 0}.
         *
         * @return {@code true} if index did change, otherwise {@code false}
         */
        boolean next() {
            if (mIndex < mFirmwares.length - 1) {
                mState = State.UPLOADING;
                mProgress = 0;
                mIndex++;
                return true;
            }
            return false;
        }

        /**
         * Updates the update state.
         *
         * @param state update state
         *
         * @return {@code true} if state did change, otherwise {@code false}
         */
        boolean updateState(@NonNull State state) {
            if (mState != state) {
                mState = state;
                return true;
            }
            return false;
        }

        /**
         * Updates upload progress.
         *
         * @param progress upload progress
         *
         * @return {@code true} if progress did change, otherwise {@code false}
         */
        boolean updateProgress(int progress) {
            if (mProgress != progress) {
                mProgress = progress;
                return true;
            }
            return false;
        }
    }
}
