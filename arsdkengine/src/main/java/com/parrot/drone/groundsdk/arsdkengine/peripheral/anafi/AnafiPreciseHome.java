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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.device.peripheral.PreciseHome;
import com.parrot.drone.groundsdk.internal.device.peripheral.PreciseHomeCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeaturePreciseHome;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.util.EnumSet;

/** PreciseHome peripheral controller for Anafi family drones. */
public class AnafiPreciseHome extends DronePeripheralController {

    /** Key used to access preset and range dictionaries for this peripheral controller. */
    private static final String SETTINGS_KEY = "preciseHome";

    // preset store bindings

    /** Precise home mode preset entry. */
    private static final StorageEntry<PreciseHome.Mode> MODE_PRESET =
            StorageEntry.ofEnum("mode", PreciseHome.Mode.class);

    // device specific store bindings

    /** Supported precise home modes device setting. */
    private static final StorageEntry<EnumSet<PreciseHome.Mode>> SUPPORTED_MODES_SETTING =
            StorageEntry.ofEnumSet("supportedModes", PreciseHome.Mode.class);

    /** PreciseHome peripheral for which this object is the backend. */
    @NonNull
    private final PreciseHomeCore mPreciseHome;

    /** Dictionary containing device specific values for this peripheral, such as settings ranges, supported status. */
    @Nullable
    private final PersistentStore.Dictionary mDeviceDict;

    /** Dictionary containing current preset values for this peripheral. */
    @Nullable
    private PersistentStore.Dictionary mPresetDict;

    /** Precise home mode. */
    @Nullable
    private PreciseHome.Mode mMode;

    /**
     * {@code true} if connected drone supports precise-home. This means that supported modes, other than only {@link
     * PreciseHome.Mode#DISABLED}, were received during connection.
     */
    private boolean mSupported;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiPreciseHome(@NonNull DroneController droneController) {
        super(droneController);
        mPreciseHome = new PreciseHomeCore(mComponentStore, mBackend);
        mPresetDict = offlineSettingsEnabled() ? mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY) : null;
        mDeviceDict = offlineSettingsEnabled() ? mDeviceController.getDeviceDict().getDictionary(SETTINGS_KEY) : null;
        loadPersistedData();
        if (isPersisted()) {
            mPreciseHome.publish();
        }
    }

    @Override
    protected final void onConnected() {
        applyPresets();
        if (mSupported) {
            mPreciseHome.publish();
        } else {
            forget();
        }
    }

    @Override
    protected final void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeaturePreciseHome.UID) {
            ArsdkFeaturePreciseHome.decode(command, mPreciseHomeCallback);
        }
    }

    @Override
    protected final void onDisconnected() {
        // clear all non saved settings
        mPreciseHome.cancelSettingsRollbacks()
                    .updateState(PreciseHome.State.UNAVAILABLE);

        mSupported = false;

        if (isPersisted()) {
            mPreciseHome.notifyUpdated();
        } else {
            mPreciseHome.unpublish();
        }
    }

    @Override
    protected final void onPresetChange() {
        mPresetDict = mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY);
        if (isConnected()) {
            applyPresets();
        }
        mPreciseHome.notifyUpdated();
    }

    @Override
    protected final void onForgetting() {
        forget();
    }

    /**
     * Sends selected precise home mode to the device.
     *
     * @param mode precise home mode to set
     *
     * @return {@code true} if any command was sent to the device, otherwise {@code false}
     */
    private boolean sendMode(@NonNull PreciseHome.Mode mode) {
        return sendCommand(ArsdkFeaturePreciseHome.encodeSetMode(ModeAdapter.from(mode)));
    }

    /**
     * Tells whether device specific settings are persisted for this component.
     *
     * @return {@code true} if the component has persisted device settings, otherwise {@code false}
     */
    private boolean isPersisted() {
        return mDeviceDict != null && !mDeviceDict.isNew();
    }

    /**
     * Loads presets and settings from persistent storage and updates the component accordingly.
     */
    private void loadPersistedData() {
        EnumSet<PreciseHome.Mode> supportedModes = SUPPORTED_MODES_SETTING.load(mDeviceDict);
        if (supportedModes != null) {
            mPreciseHome.mode().updateAvailableValues(supportedModes);
        }
        applyPresets();
    }

    /**
     * Applies component's persisted presets.
     */
    private void applyPresets() {
        applyMode(MODE_PRESET.load(mPresetDict));
    }

    /**
     * Forgets the component.
     */
    private void forget() {
        if (mDeviceDict != null) {
            mDeviceDict.clear().commit();
        }
        mPreciseHome.unpublish();
    }

    /**
     * Applies precise home mode
     * <ul>
     * <li>Finds an appropriate fallback value if the given value is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param mode value to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyMode(@Nullable PreciseHome.Mode mode) {
        if (mode == null || !mPreciseHome.mode().getAvailableValues().contains(mode)) {
            if (mMode == null) {
                return false;
            }
            mode = mMode;
        }

        boolean updating = mMode != mode
                           && sendMode(mode);

        mMode = mode;
        mPreciseHome.mode().updateValue(mMode);
        return updating;
    }

    /** Backend of PreciseHomeCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final PreciseHomeCore.Backend mBackend = new PreciseHomeCore.Backend() {

        @Override
        public boolean setMode(@NonNull PreciseHome.Mode mode) {
            boolean updating = applyMode(mode);
            MODE_PRESET.save(mPresetDict, mode);
            if (!updating) {
                mPreciseHome.notifyUpdated();
            }
            return updating;
        }
    };

    /** Callbacks called when a command of the feature ArsdkFeaturePreciseHome is decoded. */
    private final ArsdkFeaturePreciseHome.Callback mPreciseHomeCallback = new ArsdkFeaturePreciseHome.Callback() {

        @Override
        public void onCapabilities(int modesBitField) {
            EnumSet<PreciseHome.Mode> supportedModes = ModeAdapter.from(modesBitField);
            if (supportedModes.stream().anyMatch(mode -> mode != PreciseHome.Mode.DISABLED)) {
                // other modes than 'DISABLED' exists, peripheral is supported
                SUPPORTED_MODES_SETTING.save(mDeviceDict, supportedModes);
                mPreciseHome.mode().updateAvailableValues(supportedModes);
                mSupported = true;
            }
        }

        @Override
        public void onMode(@Nullable ArsdkFeaturePreciseHome.Mode mode) {
            if (mode == null) {
                return;
            }

            mMode = ModeAdapter.from(mode);

            if (isConnected()) {
                mPreciseHome.mode().updateValue(mMode);
                mPreciseHome.notifyUpdated();
            }
        }

        @Override
        public void onState(@Nullable ArsdkFeaturePreciseHome.State state) {
            if (state != null) switch (state) {
                case UNAVAILABLE:
                    mPreciseHome.updateState(PreciseHome.State.UNAVAILABLE)
                                .notifyUpdated();
                    break;
                case AVAILABLE:
                    mPreciseHome.updateState(PreciseHome.State.AVAILABLE)
                                .notifyUpdated();
                    break;
                case ACTIVE:
                    mPreciseHome.updateState(PreciseHome.State.ACTIVE)
                                .notifyUpdated();
                    break;
            }
        }
    };

    /**
     * Utility class to adapt {@link ArsdkFeaturePreciseHome.Mode precise home feature} to {@link PreciseHome.Mode
     * groundsdk} precise home modes.
     */
    private static final class ModeAdapter {

        /**
         * Converts an {@code ArsdkFeaturePreciseHome.Mode} to its {@code PreciseHome.Mode} equivalent.
         *
         * @param mode precise home feature mode to convert
         *
         * @return the groundsdk precise home mode equivalent
         */
        @NonNull
        static PreciseHome.Mode from(@NonNull ArsdkFeaturePreciseHome.Mode mode) {
            switch (mode) {
                case DISABLED:
                    return PreciseHome.Mode.DISABLED;
                case STANDARD:
                    return PreciseHome.Mode.STANDARD;
            }
            return null;
        }

        /**
         * Converts a {@code PreciseHome.Mode} to its {@code ArsdkFeaturePreciseHome.Mode} equivalent.
         *
         * @param mode groundsdk precise home mode to convert
         *
         * @return precise home feature mode equivalent of the given value
         */
        @NonNull
        static ArsdkFeaturePreciseHome.Mode from(@NonNull PreciseHome.Mode mode) {
            switch (mode) {
                case DISABLED:
                    return ArsdkFeaturePreciseHome.Mode.DISABLED;
                case STANDARD:
                    return ArsdkFeaturePreciseHome.Mode.STANDARD;
            }
            return null;
        }

        /**
         * Converts a bitfield representation of multiple {@code ArsdkFeaturePreciseHome.Mode} to its equivalent set of
         * {@code PreciseHome.Mode}.
         *
         * @param bitfield bitfield representation of precise home feature modes to convert
         *
         * @return the equivalent set of groundsdk precise home modes
         */
        @NonNull
        static EnumSet<PreciseHome.Mode> from(int bitfield) {
            EnumSet<PreciseHome.Mode> modes = EnumSet.noneOf(PreciseHome.Mode.class);
            ArsdkFeaturePreciseHome.Mode.each(bitfield, arsdk -> modes.add(from(arsdk)));
            return modes;
        }
    }
}
