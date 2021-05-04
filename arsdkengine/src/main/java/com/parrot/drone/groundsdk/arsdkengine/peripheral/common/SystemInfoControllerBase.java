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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.PeripheralController;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.device.peripheral.SystemInfoCore;
import com.parrot.drone.groundsdk.internal.utility.FirmwareBlackList;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;

/** Abstract base implementation for SystemInfo peripheral controller. */
public abstract class SystemInfoControllerBase extends PeripheralController<DeviceController<?>> {

    /** Key used to access device specific dictionary for this component's settings. */
    private static final String SETTINGS_KEY = "systemInfo";

    /** Device serial number setting. */
    private static final StorageEntry<String> SERIAL_SETTING = StorageEntry.ofString("serial");

    /** Device firmware version setting. */
    private static final StorageEntry<String> FIRMWARE_VERSION_SETTING = StorageEntry.ofString("firmwareVersion");

    /** Device hardware version setting. */
    private static final StorageEntry<String> HARDWARE_VERSION_SETTING = StorageEntry.ofString("hardwareVersion");

    /** Device CPU identifier setting. */
    private static final StorageEntry<String> CPU_ID_SETTING = StorageEntry.ofString("cpuId");

    /** Device board identifier setting. */
    private static final StorageEntry<String> BOARD_ID_SETTING = StorageEntry.ofString("boardId");

    /** Device update requirement. */
    private static final StorageEntry<Boolean> UPDATE_REQUIREMENT_SETTING = StorageEntry.ofBoolean("updateRequirement");

    /** SystemInfo peripheral for which this object is the backend. */
    @NonNull
    protected final SystemInfoCore mSystemInfo;

    /** Dictionary containing device specific values for this component. */
    @NonNull
    private final PersistentStore.Dictionary mDeviceDict;

    /** Firmware blacklist utility, {@code null} when firmware management is unavailable. */
    @Nullable
    private final FirmwareBlackList mBlacklist;

    /** Identifies current firmware. {@code null} until firmware version is known. */
    @Nullable
    private FirmwareIdentifier mFirmware;

    /**
     * Constructor.
     *
     * @param deviceController the device controller that owns this peripheral controller.
     */
    protected SystemInfoControllerBase(@NonNull DeviceController<?> deviceController) {
        super(deviceController);
        mDeviceDict = mDeviceController.getDeviceDict().getDictionary(SETTINGS_KEY);
        mSystemInfo = new SystemInfoCore(mComponentStore, mBackend);
        mBlacklist = mDeviceController.getEngine().getUtility(FirmwareBlackList.class);
        loadPersistedData();

        if (isPersisted()) {
            startMonitoringBlacklist();
            mSystemInfo.publish();
        }
    }

    @Override
    protected final void onConnected() {
        startMonitoringBlacklist();
        mSystemInfo.publish();
    }

    @Override
    protected final void onDisconnected() {
        // clear non persisted data
        mSystemInfo.clearOngoingResetSettingsFlag()
                   .clearOngoingFactoryResetFlag();

        if (isPersisted()) {
            mSystemInfo.notifyUpdated();
        } else {
            stopMonitoringBlacklist();
            mSystemInfo.unpublish();
        }
    }

    @Override
    protected final void onForgetting() {
        mDeviceDict.clear().commit();
        stopMonitoringBlacklist();
        mSystemInfo.unpublish();
    }

    @Override
    protected void onDispose() {
        stopMonitoringBlacklist();
    }

    @Override
    protected void onApiCapabilities(@ArsdkDevice.Api int api) {
        boolean isUpdateRequired = (api == ArsdkDevice.API_UPDATE_ONLY);
        UPDATE_REQUIREMENT_SETTING.save(mDeviceDict, isUpdateRequired);
        mSystemInfo.updateIsUpdateRequired(isUpdateRequired);
        mSystemInfo.notifyUpdated();
    }

    /**
     * Called back by subclasses to notify reception of a firmware version value from the device.
     * <p>
     * Persists received value in the device specific settings store, updates the controller's current local value and
     * updates the component's setting accordingly.
     * <p>
     * Note that this method does not call {@link SystemInfoCore#notifyUpdated()}.
     *
     * @param version received firmware version
     */
    protected final void onFirmwareVersion(@NonNull String version) {
        if (processFirmwareVersion(version)) {
            FIRMWARE_VERSION_SETTING.save(mDeviceDict, version);
        }
    }

    /**
     * Called back by subclasses to notify reception of a hardware version value from the device.
     * <p>
     * Persists received value in the device specific settings store and updates the component's setting accordingly.
     * <p>
     * Note that this method does not call {@link SystemInfoCore#notifyUpdated()}.
     *
     * @param version received hardware version
     */
    protected final void onHardwareVersion(@NonNull String version) {
        HARDWARE_VERSION_SETTING.save(mDeviceDict, version);
        mSystemInfo.updateHardwareVersion(version);
    }

    /**
     * Called back by subclasses to notify reception of a serial number value from the device.
     * <p>
     * Persists received value in the device specific settings store and updates the component's setting accordingly.
     * <p>
     * Note that this method does not call {@link SystemInfoCore#notifyUpdated()}.
     *
     * @param serial received serial number
     */
    protected final void onSerial(@NonNull String serial) {
        SERIAL_SETTING.save(mDeviceDict, serial);
        mSystemInfo.updateSerial(serial);
    }

    /**
     * Called back by subclasses to notify reception of a CPU identifier value from the device.
     * <p>
     * Persists received value in the device specific settings store and updates the component's setting accordingly.
     * <p>
     * Note that this method does not call {@link SystemInfoCore#notifyUpdated()}.
     *
     * @param id received CPU identifier
     */
    protected final void onCpuId(@NonNull String id) {
        CPU_ID_SETTING.save(mDeviceDict, id);
        mSystemInfo.updateCpuId(id);
    }

    /**
     * Called back by subclasses to notify reception of a board identifier value from the device.
     * <p>
     * Persists received value in the device specific settings store and updates the component's setting accordingly.
     * <p>
     * Note that this method does not call {@link SystemInfoCore#notifyUpdated()}.
     *
     * @param id received board identifier
     */
    protected final void onBoardId(@NonNull String id) {
        BOARD_ID_SETTING.save(mDeviceDict, id);
        mSystemInfo.updateBoardId(id);
    }

    /**
     * Called back by subclasses to notify that the device is done resetting settings to defaults.
     * <p>
     * Updates the component's setting accordingly.
     * <p>
     * Note that this method does not call {@link SystemInfoCore#notifyUpdated()}.
     */
    protected final void onSettingsReset() {
        mSystemInfo.clearOngoingResetSettingsFlag();
    }

    /**
     * Requests the device to perform a factory reset.
     *
     * @return {@code true} if any command was sent to the device, otherwise false
     */
    protected abstract boolean sendFactoryReset();

    /**
     * Requests the device to reset its settings to defaults.
     *
     * @return {@code true} if any command was sent to the device, otherwise false
     */
    protected abstract boolean sendResetSettings();

    /**
     * Tells whether device specific settings are persisted for this component.
     *
     * @return {@code true} if the component has persisted device settings, otherwise {@code false}
     */
    private boolean isPersisted() {
        return !mDeviceDict.isNew();
    }

    /**
     * Loads settings from persistent storage and updates the component accordingly.
     */
    private void loadPersistedData() {
        String serial = SERIAL_SETTING.load(mDeviceDict);
        if (serial != null) {
            mSystemInfo.updateSerial(serial);
        }
        String firmwareVersion = FIRMWARE_VERSION_SETTING.load(mDeviceDict);
        if (firmwareVersion != null) {
            processFirmwareVersion(firmwareVersion);
        }
        String hardwareVersion = HARDWARE_VERSION_SETTING.load(mDeviceDict);
        if (hardwareVersion != null) {
            mSystemInfo.updateHardwareVersion(hardwareVersion);
        }
        String cpuId = CPU_ID_SETTING.load(mDeviceDict);
        if (cpuId != null) {
            mSystemInfo.updateCpuId(cpuId);
        }
        String boardId = BOARD_ID_SETTING.load(mDeviceDict);
        if (boardId != null) {
            mSystemInfo.updateBoardId(boardId);
        }
        mSystemInfo.updateIsUpdateRequired(Boolean.TRUE.equals(UPDATE_REQUIREMENT_SETTING.load(mDeviceDict)));
    }

    /**
     * Starts monitoring firmware blacklist for changes.
     * <p>
     * Immediately processes current blacklist state with regard to the current firmware and updates the component
     * accordingly the case being.
     */
    private void startMonitoringBlacklist() {
        if (mBlacklist != null) {
            mBlacklist.monitorWith(mMonitor);
            processBlacklistedState();
        }
    }

    /**
     * Stops monitoring firmware blacklist changes.
     */
    private void stopMonitoringBlacklist() {
        if (mBlacklist != null) {
            mBlacklist.disposeMonitor(mMonitor);
        }
    }

    /**
     * Processes firmware version string and updates the component accordingly.
     *
     * @param versionStr firmware version string to process
     *
     * @return {@code true} if the version could be processed and the component was update, otherwise {@code false}
     */
    private boolean processFirmwareVersion(@NonNull String versionStr) {
        FirmwareVersion version = FirmwareVersion.parse(versionStr);
        if (version != null) {
            mFirmware = new FirmwareIdentifier(mDeviceController.getDevice().getModel(), version);
            mSystemInfo.updateFirmwareVersion(version.toString());
            processBlacklistedState();
            return true;
        }
        return false;
    }

    /**
     * Computes firmware blacklisted state and updates the component accordingly.
     */
    private void processBlacklistedState() {
        if (mFirmware != null && mBlacklist != null) {
            mSystemInfo.updateFirmwareBlacklisted(mBlacklist.isFirmwareBlacklisted(mFirmware));
        }
    }

    /** Monitors firmware blacklist to tell whether the device's firmware is blacklisted. */
    private final FirmwareBlackList.Monitor mMonitor = new FirmwareBlackList.Monitor() {

        @Override
        public void onChange() {
            processBlacklistedState();
            mSystemInfo.notifyUpdated();
        }
    };

    /** Backend of SysInfoCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final SystemInfoCore.Backend mBackend = new SystemInfoCore.Backend() {

        @Override
        public boolean factoryReset() {
            return sendFactoryReset();
        }

        @Override
        public boolean resetSettings() {
            return sendResetSettings();
        }
    };
}
