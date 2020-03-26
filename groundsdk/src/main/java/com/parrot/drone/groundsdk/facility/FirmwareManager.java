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

package com.parrot.drone.groundsdk.facility;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;

import java.util.Set;

/**
 * Facility that provides global management of firmware updates.
 * <p>
 * FirmwareManager allows to: <ul>
 * <li>Query up-to-date firmware information from remote update server; note that the application is only allowed
 * to query update once every hour.</li>
 * <li>list firmware updates for all supported device {@link DeviceModel models}, both remotely available (that
 * need to be downloaded from remote update server) and locally available, that are present on the device's
 * internal storage and are ready to be used for device update. </li>
 * <li>Download remote firmware update file from remote update server and archive them on device's internal
 * storage for later use.</li>
 * <li>Delete locally downloaded firmware update files.</li>
 * </ul>
 */
public interface FirmwareManager extends Facility {

    /**
     * Requests up to date firmware information from remote servers.
     */
    void queryRemoteFirmwares();

    /**
     * Tells whether a remote firmware information query is currently in progress.
     *
     * @return {@code true} if a query is in progress, otherwise {@code false}
     */
    boolean isQueryingRemoteFirmwares();

    /** Represents an available firmware update. */
    interface Entry {

        /**
         * Gives information about the firmware update.
         *
         * @return firmware information
         */
        @NonNull
        FirmwareInfo info();

        /**
         * Local state of the firmware update.
         */
        enum State {

            /** Firmware update is available on remote server, and may be downloaded to device's storage. */
            NOT_DOWNLOADED,

            /** Firmware update file is currently being downloaded. */
            DOWNLOADING,

            /** Firmware update is available locally from device's storage. */
            DOWNLOADED,
        }

        /**
         * Gives this firmware update's local state.
         *
         * @return firmware update local state
         */
        @NonNull
        State state();

        /**
         * Informs about firmware file current download progress, as a percentage.
         * <p>
         * Note that this is only meaningful when {@link #state() state} is {@link State#DOWNLOADING downloading},
         * otherwise this method returns {@code 0}.
         *
         * @return current download progress, {@code 0} if not applicable
         */
        @IntRange(from = 0, to = 100)
        int downloadProgress();

        /**
         * Requests download of this firmware update file.
         * <p>
         * This action is only available when {@link #state() state} is {@link State#NOT_DOWNLOADED not downloaded},
         * otherwise this method returns {@code false}.
         *
         * @return {@code true} if the download was successfully requested, otherwise {@code false}
         */
        boolean download();

        /**
         * Cancels an ongoing firmware update file download.
         * <p>
         * This action is only available when {@link #state() state} is {@link State#DOWNLOADING downloading},
         * otherwise this method returns {@code false}.
         *
         * @return {@code true} if the download was successfully canceled, otherwise {@code false}
         */
        boolean cancelDownload();

        /**
         * Tells whether any local file for this firmware update can be deleted from device's storage.
         * <p>
         * Note that this is only meaningful when {@link #state() state} is {@link State#DOWNLOADED downloaded},
         * otherwise this method returns {@code false}. <br>
         * Moreover, note that application preset firmwares cannot usually be deleted from device's storage, i.e.
         * their state reports 'downloaded' since they are locally available, but this method returns {@code false}.
         *
         * @return {@code true} if this firmware update file can be deleted from device's storage, otherwise
         *         {@code false}
         */
        boolean canDelete();

        /**
         * Requests deletion of this firmware update file from device's storage.
         * <p>
         * This action is only available when {@link #state() state} is {@link State#DOWNLOADED downloaded}, and
         * when the local file is {@link #canDelete() deletable}, otherwise this method returns {@code false}.
         *
         * @return {@code true} if the file was successfully deleted, otherwise {@code false}
         */
        boolean delete();
    }

    /**
     * Lists all available firmware updates.
     * <p>
     * Returned set cannot be modified.
     *
     * @return available firmware updates
     */
    @NonNull
    Set<Entry> firmwares();
}
