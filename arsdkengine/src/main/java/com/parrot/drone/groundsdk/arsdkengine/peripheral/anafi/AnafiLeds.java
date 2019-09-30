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
import com.parrot.drone.groundsdk.internal.device.peripheral.LedsCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureLeds;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** Leds peripheral controller for Anafi family drones. */
public class AnafiLeds extends DronePeripheralController {

    /** Key used to access preset and range dictionaries for this peripheral controller. */
    private static final String SETTINGS_KEY = "leds";

    // preset store bindings

    /** LEDs switch state preset entry. */
    private static final StorageEntry<Boolean> STATE_PRESET = StorageEntry.ofBoolean("state");

    // device specific store bindings

    /** LEDs switch state support device setting. */
    private static final StorageEntry<Boolean> STATE_SUPPORT_SETTING = StorageEntry.ofBoolean("stateSupport");

    /** Leds peripheral for which this object is the backend. */
    @NonNull
    private final LedsCore mLeds;

    /** Dictionary containing device specific values for this peripheral, such as settings ranges, supported status. */
    @Nullable
    private final PersistentStore.Dictionary mDeviceDict;

    /** Dictionary containing current preset values for this peripheral. */
    @Nullable
    private PersistentStore.Dictionary mPresetDict;

    /** LEDs switch state. */
    @Nullable
    private Boolean mState;

    /**
     * {@code true} if connected drone supports LEDs. This means that any LEDs capability was received during
     * connection.
     */
    private boolean mSupported;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiLeds(@NonNull DroneController droneController) {
        super(droneController);
        mLeds = new LedsCore(mComponentStore, mBackend);
        mPresetDict = offlineSettingsEnabled() ? mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY) : null;
        mDeviceDict = offlineSettingsEnabled() ? mDeviceController.getDeviceDict().getDictionary(SETTINGS_KEY) : null;
        loadPersistedData();
        if (isPersisted()) {
            mLeds.publish();
        }
    }

    @Override
    protected final void onConnected() {
        applyPresets();
        if (mSupported) {
            mLeds.publish();
        } else {
            forget();
        }
    }

    @Override
    protected final void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureLeds.UID) {
            ArsdkFeatureLeds.decode(command, mLedsCallback);
        }
    }

    @Override
    protected final void onDisconnected() {
        mLeds.cancelSettingsRollbacks();

        mSupported = false;

        if (isPersisted()) {
            mLeds.notifyUpdated();
        } else {
            mLeds.unpublish();
        }
    }

    @Override
    protected final void onPresetChange() {
        mPresetDict = mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY);
        if (isConnected()) {
            applyPresets();
        }
        mLeds.notifyUpdated();
    }

    @Override
    protected final void onForgetting() {
        forget();
    }

    /**
     * Sends LEDs switch state to the device.
     *
     * @param enabled {@code true} to enable LEDs, otherwise {@code false}
     *
     * @return {@code true} if any command was sent to the device, otherwise {@code false}
     */
    private boolean sendState(boolean enabled) {
        if (enabled) {
            return sendCommand(ArsdkFeatureLeds.encodeActivate());
        } else {
            return sendCommand(ArsdkFeatureLeds.encodeDeactivate());
        }
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
        applyPresets();
    }

    /**
     * Applies component's persisted presets.
     */
    private void applyPresets() {
        applyState(STATE_PRESET.load(mPresetDict));
    }

    /**
     * Forgets the component.
     */
    private void forget() {
        if (mDeviceDict != null) {
            mDeviceDict.clear().commit();
        }
        mLeds.unpublish();
    }

    /**
     * Applies LEDs switch state.
     * <ul>
     * <li>Gets the last received value if the given one is null;</li>
     * <li>Sends the obtained value to the drone in case it differs from the last received value;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param enabled {@code true} to enable LEDs, otherwise {@code false}
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyState(@Nullable Boolean enabled) {
        if (enabled == null) {
            enabled = mState;
            if (enabled == null) {
                return false;
            }
        }

        boolean updating = !enabled.equals(mState)
                           && sendState(enabled);

        mState = enabled;
        mLeds.state().updateValue(enabled);
        return updating;
    }

    /** Backend of LedsCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final LedsCore.Backend mBackend = new LedsCore.Backend() {

        @Override
        public boolean setState(boolean enabled) {
            boolean updating = applyState(enabled);
            STATE_PRESET.save(mPresetDict, enabled);
            if (!updating) {
                mLeds.notifyUpdated();
            }
            return updating;
        }
    };

    /** Callbacks called when a command of the feature ArsdkFeatureLeds is decoded. */
    private final ArsdkFeatureLeds.Callback mLedsCallback = new ArsdkFeatureLeds.Callback() {

        @Override
        public void onCapabilities(int supportedCapabilitiesBitField) {
            if (ArsdkFeatureLeds.SupportedCapabilities.ON_OFF.inBitField(supportedCapabilitiesBitField)) {
                STATE_SUPPORT_SETTING.save(mDeviceDict, true);
                mSupported = true;
            }
        }

        @Override
        public void onSwitchState(@Nullable ArsdkFeatureLeds.SwitchState switchState) {
            if (switchState == null) {
                return;
            }

            mState = switchState == ArsdkFeatureLeds.SwitchState.ON;

            if (isConnected()) {
                mLeds.state().updateValue(mState);
                mLeds.notifyUpdated();
            }
        }
    };
}
