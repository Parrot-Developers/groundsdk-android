/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.arsdkengine.persistence.Converter;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.device.peripheral.Dri;
import com.parrot.drone.groundsdk.internal.device.peripheral.DriCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureDri;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** DRI peripheral controller for Anafi family drones. */
public class AnafiDri extends DronePeripheralController {

    /** Key used to access preset and range dictionaries for this peripheral controller. */
    private static final String SETTINGS_KEY = "dri";

    /** DRI state preset entry. */
    private static final StorageEntry<Boolean> STATE_PRESET = StorageEntry.ofBoolean("state");

    /** DRI state support device setting. */
    private static final StorageEntry<Boolean> STATE_SUPPORT_SETTING = StorageEntry.ofBoolean("stateSupport");

    /** Drone identifier device setting. */
    private static final StorageEntry<DriCore.Id> ID_SETTING = new IdStorageEntry("id");

    /** Dri peripheral for which this object is the backend. */
    @NonNull
    private final DriCore mDri;

    /** Dictionary containing device specific values for this peripheral, such as settings ranges, supported status. */
    @Nullable
    private final PersistentStore.Dictionary mDeviceDict;

    /** Dictionary containing current preset values for this peripheral. */
    @Nullable
    private PersistentStore.Dictionary mPresetDict;

    /** DRI state. */
    @Nullable
    private Boolean mState;

    /**
     * {@code true} if the connected drone has any DRI capability.
     */
    private boolean mSupported;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiDri(@NonNull DroneController droneController) {
        super(droneController);
        mDri = new DriCore(mComponentStore, mBackend);
        mPresetDict = offlineSettingsEnabled() ? mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY) : null;
        mDeviceDict = offlineSettingsEnabled() ? mDeviceController.getDeviceDict().getDictionary(SETTINGS_KEY) : null;
        loadPersistedData();
        if (isPersisted()) {
            mDri.publish();
        }
    }

    @Override
    protected void onConnected() {
        applyPresets();
        if (mSupported) {
            mDri.publish();
        } else {
            forget();
        }
    }

    @Override
    protected void onDisconnected() {
        mDri.cancelSettingsRollbacks();

        mSupported = false;

        if (isPersisted()) {
            mDri.notifyUpdated();
        } else {
            mDri.unpublish();
        }
    }

    @Override
    protected final void onPresetChange() {
        mPresetDict = mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY);
        if (isConnected()) {
            applyPresets();
        }
        mDri.notifyUpdated();
    }

    @Override
    protected final void onForgetting() {
        forget();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureDri.UID) {
            ArsdkFeatureDri.decode(command, mDriCallback);
        }
    }

    /**
     * Loads presets and settings from persistent storage and updates the component accordingly.
     */
    private void loadPersistedData() {
        DriCore.Id id = ID_SETTING.load(mDeviceDict);
        if (id != null) {
            mDri.updateDroneId(id);
        }

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
        mDri.unpublish();
    }

    /**
     * Applies DRI state.
     * <ul>
     * <li>Gets the last received value if the given one is null;</li>
     * <li>Sends the obtained value to the drone in case it differs from the last received value;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param enabled the DRI state to apply.
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
        mDri.state().updateValue(enabled);
        return updating;
    }

    /**
     * Sends DRI state to the device.
     *
     * @param enabled {@code true} to enable DRI, otherwise {@code false}
     *
     * @return {@code true} if any command was sent to the device, otherwise {@code false}
     */
    private boolean sendState(boolean enabled) {
        return sendCommand(ArsdkFeatureDri.encodeDriMode(
                enabled ? ArsdkFeatureDri.Mode.ENABLED : ArsdkFeatureDri.Mode.DISABLED)
        );
    }

    /**
     * Tells whether device specific settings are persisted for this component.
     *
     * @return {@code true} if the component has persisted device settings, otherwise {@code false}
     */
    private boolean isPersisted() {
        return mDeviceDict != null && !mDeviceDict.isNew();
    }


    /** Backend of DriCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final DriCore.Backend mBackend = new DriCore.Backend() {

        @Override
        public boolean setState(boolean state) {
            boolean updating = applyState(state);
            STATE_PRESET.save(mPresetDict, state);

            if (!updating) {
                mDri.notifyUpdated();
            }
            return updating;
        }
    };

    /** Callbacks called when a command of the feature ArsdkFeatureDri is decoded. */
    private final ArsdkFeatureDri.Callback mDriCallback = new ArsdkFeatureDri.Callback() {

        @Override
        public void onDroneId(@Nullable ArsdkFeatureDri.IdType type, @NonNull String value) {
            if (type == null) throw new ArsdkCommand.RejectedEventException("Invalid drone id type");

            DriCore.Id id = new DriCore.Id(DriTypeAdapter.from(type), value);
            ID_SETTING.save(mDeviceDict, id);
            mDri.updateDroneId(id).notifyUpdated();
        }

        @Override
        public void onCapabilities(int supportedCapabilitiesBitField) {
            mSupported = supportedCapabilitiesBitField != 0;
            if (ArsdkFeatureDri.SupportedCapabilities.ON_OFF.inBitField(supportedCapabilitiesBitField)) {
                STATE_SUPPORT_SETTING.save(mDeviceDict, true);
            }
        }

        @Override
        public void onDriState(@Nullable ArsdkFeatureDri.Mode mode) {
            mState = mode == ArsdkFeatureDri.Mode.ENABLED;
            if (isConnected()) {
                mDri.state()
                    .updateValue(mState);
                mDri.notifyUpdated();
            }
        }
    };

    /**
     * Utility class to adapt {@link ArsdkFeatureDri.IdType dri feature} to {@link Dri.IdType groundsdk} ID type.
     */
    private static final class DriTypeAdapter {

        /**
         * Converts a {@code ArsdkFeatureDri.IdType} to its {@code Dri.IdType} equivalent.
         *
         * @param idType arsdk dri ID type to convert
         *
         * @return the equivalent groundsdk dri ID type
         */
        @NonNull
        static Dri.IdType from(@NonNull ArsdkFeatureDri.IdType idType) {
            switch (idType) {
                case FR_30_OCTETS:
                    return Dri.IdType.FR_30_OCTETS;
                case ANSI_CTA_2063:
                    return Dri.IdType.ANSI_CTA_2063;
            }
            return null;
        }
    }

    /** A specific {@code StorageEntry} implementation for {@link Dri.DroneId}. */
    private static final class IdStorageEntry extends StorageEntry<DriCore.Id> {

        /** DRI format key. */
        private static final String KEY_FORMAT = "format";

        /** DRI data key. */
        private static final String KEY_DATA = "data";

        /**
         * Constructor.
         *
         * @param key storage key
         */
        IdStorageEntry(@NonNull String key) {
            super(key);
        }

        @NonNull
        @Override
        protected DriCore.Id parse(@NonNull Object serializedObject) {
            try {
                JSONObject serializedId = (JSONObject) serializedObject;
                return new DriCore.Id(
                      Converter.parseEnum(serializedId.getString(KEY_FORMAT), Dri.IdType.class),
                      serializedId.getString(KEY_DATA));
            } catch (ClassCastException | JSONException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @NonNull
        @Override
        protected Object serialize(@NonNull DriCore.Id id) {
            try {
                return new JSONObject()
                        .put(KEY_FORMAT, Converter.serializeEnum(id.getType()))
                        .put(KEY_DATA, id.getId());
            } catch (JSONException e) {
                throw new Error(e); // never happens
            }
        }
    }
}
