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
import com.parrot.drone.groundsdk.device.peripheral.Geofence;
import com.parrot.drone.groundsdk.internal.device.peripheral.GeofenceCore;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.ulog.ULog;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG;

/** Geofence peripheral controller for Anafi family drones. */
public final class AnafiGeofence extends DronePeripheralController {

    /** Key used to access preset and range dictionaries for this peripheral settings. */
    private static final String SETTINGS_KEY = "geofence";

    // preset store bindings

    /** Maximum altitude preset entry. */
    private static final StorageEntry<Double> MAX_ALTITUDE_PRESET = StorageEntry.ofDouble("maxAltitude");

    /** Maximum distance preset entry. */
    private static final StorageEntry<Double> MAX_DISTANCE_PRESET = StorageEntry.ofDouble("maxDistance");

    /** Geofencing mode preset entry. */
    private static final StorageEntry<Geofence.Mode> MODE_PRESET = StorageEntry.ofEnum("mode", Geofence.Mode.class);

    // device specific store bindings

    /** Maximum altitude setting range. */
    private static final StorageEntry<DoubleRange> MAX_ALTITUDE_RANGE_SETTING =
            StorageEntry.ofDoubleRange("maxAltitudeRange");

    /** Maximum distance setting range. */
    private static final StorageEntry<DoubleRange> MAX_DISTANCE_RANGE_SETTING =
            StorageEntry.ofDoubleRange("maxDistanceRange");


    /** The geofence peripheral from which this object is the backend. */
    @NonNull
    private final GeofenceCore mGeofence;

    /** Dictionary containing device specific values for this peripheral, such as settings ranges, supported status. */
    @Nullable
    private final PersistentStore.Dictionary mDeviceDict;

    /** Dictionary containing current preset values for this peripheral. */
    @Nullable
    private PersistentStore.Dictionary mPresetDict;

    /** Maximum altitude value. */
    @Nullable
    private Double mMaxAltitude;

    /** Maximum distance value. */
    @Nullable
    private Double mMaxDistance;

    /** Geofencing mode value. */
    @Nullable
    private Geofence.Mode mMode;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiGeofence(@NonNull DroneController droneController) {
        super(droneController);
        mGeofence = new GeofenceCore(mComponentStore, mBackend);
        mDeviceDict = offlineSettingsEnabled() ? mDeviceController.getDeviceDict().getDictionary(SETTINGS_KEY) : null;
        mPresetDict = offlineSettingsEnabled() ? mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY) : null;
        loadPersistedData();
        if (isPersisted()) {
            mGeofence.publish();
        }
    }

    @Override
    protected void onConnected() {
        applyPresets();
        mGeofence.publish();
    }

    @Override
    protected void onDisconnected() {
        mGeofence.cancelSettingsRollbacks();

        if (isPersisted()) {
            mGeofence.notifyUpdated();
        } else {
            mGeofence.unpublish();
        }
    }

    @Override
    protected void onPresetChange() {
        mPresetDict = mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY);
        if (isConnected()) {
            applyPresets();
        }
        mGeofence.notifyUpdated();
    }

    @Override
    protected void onForgetting() {
        if (mDeviceDict != null) {
            mDeviceDict.clear().commit();
        }
        mGeofence.unpublish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        switch (command.getFeatureId()) {
            case ArsdkFeatureArdrone3.GPSSettingsState.UID:
                ArsdkFeatureArdrone3.GPSSettingsState.decode(command, mGpsSettingsStateCallback);
                break;
            case ArsdkFeatureArdrone3.PilotingSettingsState.UID:
                ArsdkFeatureArdrone3.PilotingSettingsState.decode(command, mPilotingSettingsStateCallback);
                break;
        }
    }

    /**
     * Sends selected max altitude value to the device.
     *
     * @param maxAltitude max altitude to set
     *
     * @return {@code true} if any command was sent to the device, otherwise false
     */
    private boolean sendMaxAltitude(double maxAltitude) {
        return sendCommand(ArsdkFeatureArdrone3.PilotingSettings.encodeMaxAltitude((float) maxAltitude));
    }

    /**
     * Sends selected max distance value to the device.
     *
     * @param maxDistance max distance to set
     *
     * @return {@code true} if any command was sent to the device, otherwise false
     */
    private boolean sendMaxDistance(double maxDistance) {
        return sendCommand(ArsdkFeatureArdrone3.PilotingSettings.encodeMaxDistance((float) maxDistance));
    }

    /**
     * Sends selected mode to the device.
     *
     * @param mode mode to set
     *
     * @return {@code true} if any command was sent to the device, otherwise false
     */
    private boolean sendMode(@NonNull Geofence.Mode mode) {
        return sendCommand(ArsdkFeatureArdrone3.PilotingSettings.encodeNoFlyOverMaxDistance(
                mode == Geofence.Mode.CYLINDER ? 1 : 0));
    }

    /**
     * Called back by subclasses to notify reception of max altitude range from the device.
     * <p>
     * Persists the given range in the device specific settings store and updates the component's setting accordingly.
     * <p>
     * Note that this method does not call {@link GeofenceCore#notifyUpdated()}.
     *
     * @param min received device range lower bound
     * @param max received device range upper bound
     */
    private void onMaxAltitudeRange(double min, double max) {
        DoubleRange bounds = DoubleRange.of(min, max);
        MAX_ALTITUDE_RANGE_SETTING.save(mDeviceDict, bounds);
        mGeofence.maxAltitude()
                 .updateBounds(bounds);
    }

    /**
     * Called back by subclasses to notify reception of max altitude value from the device.
     * <p>
     * Updates the controller's current local value, and in case the device is connected, updates the component's
     * setting accordingly.
     * <p>
     * Note that this method does not call {@link GeofenceCore#notifyUpdated()}.
     *
     * @param value received device value
     */
    private void onMaxAltitude(double value) {
        mMaxAltitude = value;
        if (isConnected()) {
            mGeofence.maxAltitude()
                     .updateValue(value);
        }
    }

    /**
     * Called back by subclasses to notify reception of max distance range from the device.
     * <p>
     * Persists the given range in the device specific settings store and updates the component's setting accordingly.
     * <p>
     * Note that this method does not call {@link GeofenceCore#notifyUpdated()}.
     *
     * @param min received device range lower bound
     * @param max received device range upper bound
     */
    private void onMaxDistanceRange(double min, double max) {
        DoubleRange bounds = DoubleRange.of(min, max);
        MAX_DISTANCE_RANGE_SETTING.save(mDeviceDict, bounds);
        mGeofence.maxDistance()
                 .updateBounds(bounds);
    }

    /**
     * Called back by subclasses to notify reception of max distance value from the device.
     * <p>
     * Updates the controller's current local value, and in case the device is connected, updates the component's
     * setting accordingly.
     * <p>
     * Note that this method does not call {@link GeofenceCore#notifyUpdated()}.
     *
     * @param value received device value
     */
    private void onMaxDistance(double value) {
        mMaxDistance = value;
        if (isConnected()) {
            mGeofence.maxDistance()
                     .updateValue(value);
        }
    }

    /**
     * Called back by subclasses to notify reception of mode value from the device.
     * <p>
     * Updates the controller's current local value, and in case the device is connected, updates the component's
     * setting accordingly.
     * <p>
     * Note that this method does not call {@link GeofenceCore#notifyUpdated()}.
     *
     * @param value received device value
     */
    private void onMode(@NonNull Geofence.Mode value) {
        mMode = value;
        if (isConnected()) {
            mGeofence.mode()
                     .updateValue(value);
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
        DoubleRange maxAltitudeRange = MAX_ALTITUDE_RANGE_SETTING.load(mDeviceDict);
        if (maxAltitudeRange != null) {
            mGeofence.maxAltitude()
                     .updateBounds(maxAltitudeRange);
        }

        DoubleRange maxDistanceRange = MAX_DISTANCE_RANGE_SETTING.load(mDeviceDict);
        if (maxDistanceRange != null) {
            mGeofence.maxDistance()
                     .updateBounds(maxDistanceRange);
        }

        applyPresets();
    }

    /**
     * Applies component's persisted presets.
     */
    private void applyPresets() {
        applyMaxAltitude(MAX_ALTITUDE_PRESET.load(mPresetDict));
        applyMaxDistance(MAX_DISTANCE_PRESET.load(mPresetDict));
        applyMode(MODE_PRESET.load(mPresetDict));
    }

    /**
     * Applies maximum altitude.
     * <ul>
     * <li>Gets the last received value if the given one is null;</li>
     * <li>Sends the obtained value to the drone in case it differs from the last received value;</li>
     * <li>Saves the obtained value to the geofence presets and updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param altitude maximum altitude to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyMaxAltitude(@Nullable Double altitude) {
        // Validating given value
        if (altitude == null) {
            altitude = mMaxAltitude;
        }
        if (altitude == null) {
            return false;
        }

        boolean updating = !altitude.equals(mMaxAltitude)
                           && sendMaxAltitude(altitude);

        mMaxAltitude = altitude;
        mGeofence.maxAltitude()
                 .updateValue(altitude);

        return updating;
    }

    /**
     * Applies maximum distance.
     * <ul>
     * <li>Gets the last received value if the given one is null;</li>
     * <li>Sends the obtained value to the drone in case it differs from the last received value;</li>
     * <li>Saves the obtained value to the geofence presets and updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param distance maximum distance to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyMaxDistance(@Nullable Double distance) {
        // Validating given value
        if (distance == null) {
            distance = mMaxDistance;
        }
        if (distance == null) {
            return false;
        }

        boolean updating = !distance.equals(mMaxDistance)
                           && sendMaxDistance(distance);

        mMaxDistance = distance;
        mGeofence.maxDistance()
                 .updateValue(distance);

        return updating;
    }

    /**
     * Applies geofencing mode.
     * <ul>
     * <li>Gets the last received value if the given one is null;</li>
     * <li>Sends the obtained value to the drone in case it differs from the last received value;</li>
     * <li>Saves the obtained value to the geofence presets and updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param mode geofencing mode to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyMode(@Nullable Geofence.Mode mode) {
        // Validating given value
        if (mode == null) {
            mode = mMode;
        }
        if (mode == null) {
            return false;
        }

        boolean updating = mode != mMode
                           && sendMode(mode);

        mMode = mode;
        mGeofence.mode()
                 .updateValue(mode);

        return updating;
    }

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.GPSSettingsState is decoded. */
    private final ArsdkFeatureArdrone3.GPSSettingsState.Callback mGpsSettingsStateCallback =
            new ArsdkFeatureArdrone3.GPSSettingsState.Callback() {

                @Override
                public void onGeofenceCenterChanged(double latitude, double longitude) {
                    if (Double.compare(latitude, UNKNOWN_COORDINATE) != 0
                            && Double.compare(longitude, UNKNOWN_COORDINATE) != 0) {
                        mGeofence.updateCenter(latitude, longitude);
                    } else {
                        mGeofence.resetCenter();
                    }
                    mGeofence.notifyUpdated();
                }

                /** Special value sent by the drone when either latitude or longitude is not known. */
                private static final double UNKNOWN_COORDINATE = 500;
            };

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.PilotingSettingsState is decoded. */
    private final ArsdkFeatureArdrone3.PilotingSettingsState.Callback mPilotingSettingsStateCallback =
            new ArsdkFeatureArdrone3.PilotingSettingsState.Callback() {

                @Override
                public void onMaxAltitudeChanged(float current, float min, float max) {
                    if (min > max) {
                        ULog.w(TAG, "Invalid geofence max altitude bounds, skip this event");
                        return;
                    }
                    onMaxAltitudeRange(min, max);
                    onMaxAltitude(current);
                    mGeofence.notifyUpdated();
                }

                @Override
                public void onMaxDistanceChanged(float current, float min, float max) {
                    if (min > max) {
                        ULog.w(TAG, "Invalid geofence max distance bounds, skip this event");
                        return;
                    }
                    onMaxDistanceRange(min, max);
                    onMaxDistance(current);
                    mGeofence.notifyUpdated();
                }

                @Override
                public void onNoFlyOverMaxDistanceChanged(int shouldNotFlyOver) {
                    onMode(shouldNotFlyOver == 1 ? Geofence.Mode.CYLINDER : Geofence.Mode.ALTITUDE);
                    mGeofence.notifyUpdated();
                }
            };

    /** Backend of GeofenceCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final GeofenceCore.Backend mBackend = new GeofenceCore.Backend() {

        @Override
        public boolean setMaxAltitude(double altitude) {
            boolean updating = applyMaxAltitude(altitude);
            MAX_ALTITUDE_PRESET.save(mPresetDict, altitude);
            if (!updating) {
                mGeofence.notifyUpdated();
            }
            return updating;
        }

        @Override
        public boolean setMaxDistance(double distance) {
            boolean updating = applyMaxDistance(distance);
            MAX_DISTANCE_PRESET.save(mPresetDict, distance);
            if (!updating) {
                mGeofence.notifyUpdated();
            }
            return updating;
        }

        @Override
        public boolean setMode(@NonNull Geofence.Mode mode) {
            boolean updating = applyMode(mode);
            MODE_PRESET.save(mPresetDict, mode);
            if (!updating) {
                mGeofence.notifyUpdated();
            }
            return updating;
        }
    };
}
