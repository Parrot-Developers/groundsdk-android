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

package com.parrot.drone.groundsdk.device.peripheral;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Updater peripheral interface for {@link Drone} and {@link RemoteControl} devices.
 * <p>
 * Allows to: <ul>
 * <li>list and download available updates for the device from remote server.</li>
 * <li>list locally available updates and apply them to the connected device</li>
 * </ul>
 * This peripheral is always available even when the device is not connected, so that remote firmware updates may be
 * downloaded at all times (unless internet connection is unavailable).
 * <p>
 * Updating requires the device to be connected; however, this peripheral provides the ability to apply several
 * firmware updates in a row (mainly used in the presence of trampoline updates), and will maintain proper state across
 * device reboot/reconnection after each update is applied.
 * <p>
 * This peripheral can be obtained from a {@link Peripheral.Provider peripheral providing device} (such as a drone or a
 * remote control) using:
 * <pre>{@code device.getPeripheral(Updater.class)}</pre>
 *
 * @see Peripheral.Provider#getPeripheral(Class)
 * @see Peripheral.Provider#getPeripheral(Class, Ref.Observer)
 */
public interface Updater extends Peripheral {

    /**
     * Lists all firmwares that require to be downloaded to update the device to the latest available version.
     * <p>
     * The returned list is ordered by firmware application order: first firmwares in the list must be applied before
     * subsequent ones in order to update the device.
     * <p>
     * The returned list cannot be modified.
     *
     * @return a list containing all firmwares that need to be downloaded in order to be capable of updating the device
     *         to the latest available firmware version
     */
    @NonNull
    List<FirmwareInfo> downloadableFirmwares();

    /**
     * Ongoing firmware download state and progress.
     */
    abstract class Download {

        /**
         * Reasons that make downloading firmware(s) impossible.
         */
        public enum UnavailabilityReason {

            /** Firmwares cannot be downloaded since there is no available internet connection. */
            INTERNET_UNAVAILABLE
        }

        /**
         * Gives information on the current firmware being downloaded.
         *
         * @return currently downloaded firmware info
         */
        @NonNull
        public abstract FirmwareInfo currentFirmware();

        /**
         * Gives current firmware file download progress, in percent.
         *
         * @return current firmware file download progress
         */
        @IntRange(from = 0, to = 100)
        public abstract int currentFirmwareProgress();

        /**
         * Gives the index of the currently downloaded firmware file.
         * <p>
         * The returned index is in range [1, {@link #totalFirmwareCount()}].
         *
         * @return current downloaded firmware index
         */
        @IntRange(from = 1)
        public abstract int currentFirmwareIndex();

        /**
         * Gives total download progress, in percent.
         * <p>
         * This accounts for multiple firmware files that may be downloaded using {@link #downloadAllFirmwares()}.
         *
         * @return total download progress in percent
         */
        @IntRange(from = 0, to = 100)
        public abstract int totalProgress();

        /**
         * Gives the count of all firmware files that will be downloaded.
         * <p>
         * This accounts for multiple firmware files that may be downloaded using {@link #downloadAllFirmwares()}.
         *
         * @return total firmware downloads count
         */
        @IntRange(from = 1)
        public abstract int totalFirmwareCount();

        /**
         * Download state.
         */
        public enum State {

            /** Firmware files are currently being downloaded. */
            DOWNLOADING,

            /**
             * All requested firmware files have successfully been downloaded.
             * <p>
             * After this state is notified, another change will be notified and {@link #currentDownload()}
             * will return {@code null}.
             */
            SUCCESS,

            /**
             * Some requested firmware file failed to be downloaded
             * <p>
             * After this state is notified, another change will be notified and {@link #currentDownload()}
             * will return {@code null}.
             */
            FAILED,

            /**
             * Download operation was canceled by application request.
             * <p>
             * After this state is notified, another change will be notified and {@link #currentDownload()}
             * will return {@code null}.
             */
            CANCELED,
        }

        /**
         * Gives current download state.
         *
         * @return current download state
         */
        @NonNull
        public abstract State state();
    }

    /**
     * Tells why it is currently impossible to download remote firmwares.
     * <p>
     * If the returned set is not {@link Collection#isEmpty() empty}, then all firmware download methods
     * ({@link #downloadNextFirmware()}, {@link #downloadAllFirmwares()} won't do anything but return {@code false}.
     *
     * @return current download unavailability reasons
     */
    @NonNull
    EnumSet<Download.UnavailabilityReason> downloadUnavailabilityReasons();

    /**
     * Requests download of the next downloadable firmware that should be applied to update the device towards the
     * latest available version.
     * <p>
     * This method does nothing but return {@code false} if some {@link #downloadUnavailabilityReasons() reasons}
     * exists that make it impossible to download firmwares currently, or if there is no
     * {@link #downloadableFirmwares() available firmwares to download}.
     *
     * @return {@code true} if the download started, otherwise {@code false}
     */
    boolean downloadNextFirmware();

    /**
     * Requests download of all downloadable firmware that should be applied to update the device to the latest
     * available version.
     * <p>
     * This method does nothing but return {@code false} if some {@link #downloadUnavailabilityReasons() reasons}
     * exists that make it impossible to download firmwares currently, or if there is no
     * {@link #downloadableFirmwares() available firmwares to download}.
     *
     * @return {@code true} if the download started, otherwise {@code false}
     */
    boolean downloadAllFirmwares();

    /**
     * Gives current firmware download operation state, if any is ongoing.
     *
     * @return current firmware download state, or {@code null} in case no firmware download operation is currently in
     *         progress
     */
    @Nullable
    Download currentDownload();

    /**
     * Cancels an ongoing firmware(s) download operation.
     * <p>
     * This method does nothing but return {@code false} in case no firmware is
     * {@link #currentDownload() currently being downloaded}.
     *
     * @return {@code true} if an ongoing firmware download operation has been canceled, otherwise {@code false}
     */
    boolean cancelDownload();

    /**
     * Lists all firmwares that require to be applied to update the device to the latest available version.
     * <p>
     * The returned list is ordered by firmware application order: first firmwares in the list must be applied before
     * subsequent ones in order to update the device.
     * <p>
     * The returned list cannot be modified.
     *
     * @return a list containing all firmwares that need to be applied in order to update the device to the latest
     *         available firmware version
     */
    @NonNull
    List<FirmwareInfo> applicableFirmwares();

    /**
     * Ongoing firmware update state and progress.
     */
    abstract class Update {

        /**
         * Reasons that make applying firmware update(s) impossible.
         */
        public enum UnavailabilityReason {

            /** Updates cannot be applied because the device is currently not connected. */
            NOT_CONNECTED,

            /** Updates cannot be applied because there is not enough battery left on the device. */
            NOT_ENOUGH_BATTERY,

            /** Updates cannot be applied because the device is not landed. This applies only to drone devices. */
            NOT_LANDED
        }

        /**
         * Gives information on the current firmware update being applied.
         *
         * @return currently applied firmware update info
         */
        @NonNull
        public abstract FirmwareInfo currentFirmware();

        /**
         * Gives current firmware update upload progress, in percent.
         *
         * @return current firmware update upload progress
         */
        @IntRange(from = 0, to = 100)
        public abstract int currentFirmwareProgress();

        /**
         * Gives the index of the firmware update currently being applied.
         * <p>
         * The returned index is in range [1, {@link #totalFirmwareCount()}].
         *
         * @return current firmware update index
         */
        @IntRange(from = 1)
        public abstract int currentFirmwareIndex();

        /**
         * Gives total update progress, in percent.
         * <p>
         * This accounts for multiple firmware updates that may be applied using {@link #updateToLatestFirmware()}.
         *
         * @return total update progress in percent
         */
        @IntRange(from = 0, to = 100)
        public abstract int totalProgress();

        /**
         * Gives the count of all firmware updates that will be applied.
         * <p>
         * This accounts for multiple firmware updates that may be applied using {@link #updateToLatestFirmware()}.
         *
         * @return total firmware updates count
         */
        @IntRange(from = 1)
        public abstract int totalFirmwareCount();

        /**
         * Update state.
         */
        public enum State {

            /** Some firmware file is being uploaded to the device. */
            UPLOADING,

            /**
             * Some firmware file has been uploaded to the device, which is currently processing it.
             * <p>
             * Note that although the application may cancel an update operation in this state, the device will
             * still apply the uploaded firmware update.
             */
            PROCESSING,

            /** The device has rebooted to apply an update. Waiting for reconnection. */
            WAITING_FOR_REBOOT,

            /**
             * All requested firmware updates have successfully been applied.
             * <p>
             * After this state is notified, another change will be notified and {@link #currentUpdate()}
             * will return {@code null}.
             */
            SUCCESS,

            /**
             * Some requested firmware update failed to be applied.
             * <p>
             * After this state is notified, another change will be notified and {@link #currentUpdate()}
             * will return {@code null}.
             */
            FAILED,

            /**
             * Update operation was canceled by application request.
             * <p>
             * After this state is notified, another change will be notified and {@link #currentUpdate()}
             * will return {@code null}.
             */
            CANCELED,
        }

        /**
         * Gives current update state.
         *
         * @return current update state
         */
        @NonNull
        public abstract State state();
    }

    /**
     * Tells why it is currently impossible to apply firmware updates.
     * <p>
     * If the returned set is not {@link Collection#isEmpty() empty}, then all firmware update methods
     * ({@link #updateToNextFirmware()}, {@link #updateToLatestFirmware()}) won't do anything but return {@code false}.
     * <p>
     * In case updating becomes unavailable for some reason while an update operation is ongoing
     * ({@link Update.State#UPLOADING uploading} or {@link Update.State#PROCESSING processing}), then the update will be
     * forcefully canceled.
     *
     * @return current update unavailability reasons
     */
    @NonNull
    EnumSet<Update.UnavailabilityReason> updateUnavailabilityReasons();

    /**
     * Requests device update to the next currently applicable firmware version.
     * <p>
     * This method does nothing but return {@code false} if some {@link #updateUnavailabilityReasons() reasons}
     * exists that make it impossible to apply firmware updates currently, or if there is no
     * {@link #applicableFirmwares()} available firmwares to apply}.
     *
     * @return {@code true} if the update started, otherwise {@code false}
     */
    boolean updateToNextFirmware();

    /**
     * Requests device update to the latest applicable firmware version.
     * <p>
     * This method will update the device by applying all {@link #applicableFirmwares() applicable firmware updates}
     * in order, until the device is up-to-date. <br>
     * After each firmware is applied, the device will reboot. The application has the responsibility to ensure to
     * reconnect to the device after the update, so that this peripheral may proceed automatically with the next
     * firmware update, if any.
     * <p>
     * This method does nothing but return {@code false} if some {@link #updateUnavailabilityReasons()  reasons}
     * exists that make it impossible to apply firmware updates currently, or if there is no
     * {@link #applicableFirmwares()} available firmwares to apply}.
     *
     * @return {@code true} if the update started, otherwise {@code false}
     */
    boolean updateToLatestFirmware();

    /**
     * Gives current firmware update operation state, if any is ongoing.
     *
     * @return current firmware update state, or {@code null} in case no firmware update operation is currently in
     *         progress
     */
    @Nullable
    Update currentUpdate();

    /**
     * Cancels an ongoing firmware(s) update operation.
     * <p>
     * This method does nothing but return {@code false} in case no firmware update is
     * {@link #currentUpdate() currently being applied}.
     *
     * @return {@code true} if an ongoing firmware update operation has been canceled, otherwise {@code false}
     */
    boolean cancelUpdate();

    /**
     * Tells whether the device is currently up-to-date.
     *
     * @return {@code true} if the device is up-to-date, otherwise {@code false}.
     */
    default boolean isUpToDate() {
        return downloadableFirmwares().isEmpty() && applicableFirmwares().isEmpty();
    }

    /**
     * Retrieves the ideal version.
     * <p>
     * It is the version that the device will reach if all downloadable firmwares are downloaded and if all applicable
     * updates are applied.<br>
     * This version is not necessarily local.
     * <p>
     * <strong>Note:</strong> this version might differ from the greater version of all downloadable and applicable
     * firmwares if, and only if, the ideal firmware is local but cannot be applied because an intermediate, not
     * downloaded firmware is required first.
     *
     * @return the ideal version, or {@code null} if the device is up to date
     */
    @Nullable
    FirmwareVersion idealVersion();
}
