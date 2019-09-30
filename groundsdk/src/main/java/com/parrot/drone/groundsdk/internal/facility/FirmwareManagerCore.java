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

package com.parrot.drone.groundsdk.internal.facility;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.facility.FirmwareManager;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Core class for the {@link FirmwareManager} facility. */
public class FirmwareManagerCore extends SingletonComponentCore implements FirmwareManager {

    /** Description of FirmwareManager. */
    private static final ComponentDescriptor<Facility, FirmwareManager> DESC =
            ComponentDescriptor.of(FirmwareManager.class);

    /** Engine-specific backend for the FirmwareManager. */
    public interface Backend {

        /**
         * Requests fresh firmware information from remote servers.
         */
        void queryRemoteFirmwares();

        /**
         * Requests deletion of a firmware's local file.
         *
         * @param firmware identifies the firmware whose local file must be deleted
         *
         * @return {@code true} if any local firmware file was delete for the identified firmware, otherwise
         *         {@code false}
         */
        boolean delete(@NonNull FirmwareIdentifier firmware);

        /**
         * Requests download of a remote firmware file.
         *
         * @param firmware identifies the firmware to download the remote update file of
         * @param observer observer notified when the download task state's change
         */
        void download(@NonNull FirmwareInfo firmware, @NonNull FirmwareDownloader.Task.Observer observer);
    }

    /** Engine facility backend. */
    @NonNull
    private final Backend mBackend;

    /** All firmware entries, from the store. */
    @NonNull
    private final Set<EntryCore> mEntries;

    /** Ongoing download tasks, by firmware identifier. */
    @NonNull
    private final Map<FirmwareIdentifier, FirmwareDownloader.Task> mTasks;

    /** {@code true} when a remote update information query is currently in progress. */
    private boolean mQueryingRemote;

    /**
     * Constructor.
     *
     * @param facilityStore store where this facility belongs
     * @param backend       backend used to forward actions to the engine
     */
    public FirmwareManagerCore(@NonNull ComponentStore<Facility> facilityStore, @NonNull Backend backend) {
        super(DESC, facilityStore);
        mBackend = backend;
        mEntries = new HashSet<>();
        mTasks = new HashMap<>();
    }

    @Override
    public void queryRemoteFirmwares() {
        mBackend.queryRemoteFirmwares();
    }

    @Override
    public boolean isQueryingRemoteFirmwares() {
        return mQueryingRemote;
    }

    @NonNull
    @Override
    public Set<Entry> firmwares() {
        return Collections.unmodifiableSet(mEntries);
    }

    /**
     * Updates current remote query status.
     *
     * @param querying {@code true} when a remote query is ongoing, otherwise {@code false}
     *
     * @return {@code this}, to allow call chaining
     */
    @NonNull
    public FirmwareManagerCore updateRemoteQueryFlag(boolean querying) {
        if (mQueryingRemote != querying) {
            mQueryingRemote = querying;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates all firmware entries.
     *
     * @param entries current collection of firmware entries
     *
     * @return {@code this}, to allow call chaining
     */
    public FirmwareManagerCore updateEntries(@NonNull Collection<EntryCore> entries) {
        mChanged |= mEntries.retainAll(entries) | mEntries.addAll(entries);
        return this;
    }

    /** Observes download tasks changes. */
    private final FirmwareDownloader.Task.Observer mTaskObserver = new FirmwareDownloader.Task.Observer() {

        @Override
        public void onChange(@NonNull FirmwareDownloader.Task task) {
            FirmwareIdentifier firmware = task.requested().get(0).getFirmware();
            FirmwareDownloader.Task.State state = task.state();
            if (state != FirmwareDownloader.Task.State.DOWNLOADING && state != FirmwareDownloader.Task.State.QUEUED) {
                mTasks.remove(firmware);
            } else if (!mTasks.containsValue(task)) {
                mTasks.put(firmware, task);
            }
            mChanged = true;
            notifyUpdated();
        }
    };

    /** Implementation of a {@code FirmwareManager.Entry}. */
    public final class EntryCore implements Entry {

        /** Entry firmware info. */
        @NonNull
        private final FirmwareInfo mInfo;

        /** {@code true} when a local firmware is available for this entry. */
        private final boolean mLocal;

        /** {@code true} when a local firmware is available AND can be deleted for this entry. */
        private final boolean mCanDelete;

        /**
         * Constructor.
         *
         * @param info      firmware info
         * @param local     indicates whether a local firmware is available
         * @param canDelete indicates whether a local firmware is available and can be deleted
         */
        public EntryCore(@NonNull FirmwareInfo info, boolean local, boolean canDelete) {
            mInfo = info;
            mLocal = local;
            mCanDelete = canDelete;
        }

        @NonNull
        @Override
        public FirmwareInfo info() {
            return mInfo;
        }

        @NonNull
        @Override
        public State state() {
            if (mTasks.containsKey(mInfo.getFirmware())) {
                return State.DOWNLOADING;
            } else if (mLocal) {
                return State.DOWNLOADED;
            }
            return State.NOT_DOWNLOADED;
        }

        @Override
        public int downloadProgress() {
            FirmwareDownloader.Task task = mTasks.get(mInfo.getFirmware());
            return task == null ? 0 : task.currentProgress();
        }

        @Override
        public boolean download() {
            FirmwareIdentifier firmware = mInfo.getFirmware();
            if (mLocal || mTasks.containsKey(firmware)) {
                return false;
            }
            mBackend.download(mInfo, mTaskObserver);
            return true;
        }

        @Override
        public boolean cancelDownload() {
            FirmwareDownloader.Task task = mTasks.get(mInfo.getFirmware());
            if (task != null) {
                task.cancel();
                return true;
            }
            return false;
        }

        @Override
        public boolean canDelete() {
            return mCanDelete;
        }

        @Override
        public boolean delete() {
            return canDelete() && mBackend.delete(mInfo.getFirmware());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EntryCore entryCore = (EntryCore) o;

            return mLocal == entryCore.mLocal
                   && mCanDelete == entryCore.mCanDelete
                   && mInfo.equals(entryCore.mInfo);
        }

        @Override
        public int hashCode() {
            int result = mInfo.hashCode();
            result = 31 * result + (mLocal ? 1 : 0);
            result = 31 * result + (mCanDelete ? 1 : 0);
            return result;
        }
    }
}
