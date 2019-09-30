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

package com.parrot.drone.groundsdk.arsdkengine.instrument.anafi;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.instrument.DroneInstrumentController;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.internal.device.instrument.GpsCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** GPS instrument controller for Anafi family drones. */
public class AnafiGps extends DroneInstrumentController {

    /** Key used to access device specific dictionary for this component's settings. */
    private static final String SETTINGS_KEY = "gps";

    /** Location latitude setting. */
    private static final StorageEntry<Double> LATITUDE_SETTING = StorageEntry.ofDouble("latitude");

    /** Location longitude setting. */
    private static final StorageEntry<Double> LONGITUDE_SETTING = StorageEntry.ofDouble("longitude");

    /** Location altitude setting. */
    private static final StorageEntry<Double> ALTITUDE_SETTING = StorageEntry.ofDouble("altitude");

    /** Location horizontal accuracy setting. */
    private static final StorageEntry<Integer> HORIZONTAL_ACCURACY_SETTING =
            StorageEntry.ofInteger("horizontalAccuracy");

    /** Location vertical accuracy setting. */
    private static final StorageEntry<Integer> VERTICAL_ACCURACY_SETTING = StorageEntry.ofInteger("verticalAccuracy");

    /** Location timestamp setting. */
    private static final StorageEntry<Long> TIMESTAMP_SETTING = StorageEntry.ofLong("timestamp");


    /** The GPS from which this object is the backend. */
    @NonNull
    private final GpsCore mGps;

    /** Dictionary containing device specific values for this component. */
    @NonNull
    private final PersistentStore.Dictionary mDeviceDict;

    /**
     * Constructor.
     *
     * @param droneController The drone controller that owns this component controller.
     */
    public AnafiGps(@NonNull DroneController droneController) {
        super(droneController);
        mDeviceDict = mDeviceController.getDeviceDict().getDictionary(SETTINGS_KEY);
        mGps = new GpsCore(mComponentStore);

        if (!mDeviceDict.isNew()) {
            loadLastKnownLocation();
            mGps.publish();
        }
    }

    @Override
    public void onConnected() {
        mGps.publish();
    }

    @Override
    public void onDisconnected() {
        if (mDeviceDict.isNew()) {
            mGps.unpublish();
        }
        mGps.reset().notifyUpdated();
    }

    @Override
    protected void onForgetting() {
        mDeviceDict.clear();
        mGps.unpublish();
    }

    @Override
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureArdrone3.PilotingState.UID) {
            ArsdkFeatureArdrone3.PilotingState.decode(command, mPilotingStateCallback);
        } else if (command.getFeatureId() == ArsdkFeatureArdrone3.GPSSettingsState.UID) {
            ArsdkFeatureArdrone3.GPSSettingsState.decode(command, mGpsSettingsStateCallback);
        } else if (command.getFeatureId() == ArsdkFeatureArdrone3.GPSState.UID) {
            ArsdkFeatureArdrone3.GPSState.decode(command, mGpsStateCallback);
        }
    }

    /**
     * Loads the last known location, from persistent storage.
     * <p>
     * This updates the gps instrument accordingly.
     * <p>
     * <strong>NOTE:</strong> Caller is still responsible to call {@code ComponentCore.notifyUpdated()} to publish
     * the change up to the user.
     */
    private void loadLastKnownLocation() {
        Double latitude = LATITUDE_SETTING.load(mDeviceDict);
        Double longitude = LONGITUDE_SETTING.load(mDeviceDict);
        if (latitude != null && longitude != null) {
            mGps.updateLocation(latitude, longitude);
        }
        Double altitude = ALTITUDE_SETTING.load(mDeviceDict);
        if (altitude != null) {
            mGps.updateAltitude(altitude);
        }
        Integer horizontalAccuracy = HORIZONTAL_ACCURACY_SETTING.load(mDeviceDict);
        if (horizontalAccuracy != null) {
            mGps.updateHorizontalAccuracy(horizontalAccuracy);
        }
        Integer verticalAccuracy = VERTICAL_ACCURACY_SETTING.load(mDeviceDict);
        if (verticalAccuracy != null) {
            mGps.updateVerticalAccuracy(verticalAccuracy);
        }
        Long locationTime = TIMESTAMP_SETTING.load(mDeviceDict);
        if (locationTime != null) {
            mGps.updateLocationTime(locationTime);
        }
    }

    /**
     * Saves the location received from the drone to persistent storage, so that it can be available offline.
     *
     * @param latitude  the received latitude
     * @param longitude the received longitude
     */
    private void saveLocation(double latitude, double longitude) {
        LATITUDE_SETTING.save(mDeviceDict, latitude);
        LONGITUDE_SETTING.save(mDeviceDict, longitude);
        TIMESTAMP_SETTING.save(mDeviceDict, System.currentTimeMillis());
    }

    /**
     * Saves the altitude received from the drone to persistent storage, so that it can be available offline.
     *
     * @param altitude the received altitude
     */
    private void saveAltitude(double altitude) {
        ALTITUDE_SETTING.save(mDeviceDict, altitude);
    }

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.PilotingState is decoded. */
    private final ArsdkFeatureArdrone3.PilotingState.Callback mPilotingStateCallback =
            new ArsdkFeatureArdrone3.PilotingState.Callback() {

                /** Value sent by drone when latitude/longitude or altitude are not available. */
                private static final double VALUE_UNAVAILABLE = 500;

                /** Whether the {@link #onGpsLocationChanged} callback was triggered once. */
                private boolean mUseOnGpsLocationChanged;

                @Override
                public void onPositionChanged(double latitude, double longitude, double altitude) {
                    if (mUseOnGpsLocationChanged) {
                        return;
                    }
                    if (Double.compare(latitude, VALUE_UNAVAILABLE) != 0
                        && Double.compare(longitude, VALUE_UNAVAILABLE) != 0) {
                        mGps.updateLocation(latitude, longitude);
                        saveLocation(latitude, longitude);
                    }
                    if (Double.compare(altitude, VALUE_UNAVAILABLE) != 0
                        || (Double.compare(latitude, VALUE_UNAVAILABLE) != 0
                            && Double.compare(longitude, VALUE_UNAVAILABLE) != 0)) {
                        mGps.updateAltitude(altitude);
                        saveAltitude(altitude);
                    }
                    mGps.notifyUpdated();
                }

                @Override
                public void onGpsLocationChanged(double latitude, double longitude, double altitude,
                                                 int latitudeAccuracy, int longitudeAccuracy, int altitudeAccuracy) {
                    mUseOnGpsLocationChanged = true;
                    if (Double.compare(latitude, VALUE_UNAVAILABLE) != 0
                        && Double.compare(longitude, VALUE_UNAVAILABLE) != 0) {
                        int horizontalAccuracy = Math.max(latitudeAccuracy, longitudeAccuracy);
                        mGps.updateLocation(latitude, longitude)
                            .updateAltitude(altitude)
                            .updateHorizontalAccuracy(horizontalAccuracy)
                            .updateVerticalAccuracy(altitudeAccuracy)
                            .notifyUpdated();
                        saveLocation(latitude, longitude);
                        saveAltitude(altitude);
                        HORIZONTAL_ACCURACY_SETTING.save(mDeviceDict, horizontalAccuracy);
                        VERTICAL_ACCURACY_SETTING.save(mDeviceDict, altitudeAccuracy);
                    }
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.GPSSettingState is decoded. */
    private final ArsdkFeatureArdrone3.GPSSettingsState.Callback mGpsSettingsStateCallback =
            new ArsdkFeatureArdrone3.GPSSettingsState.Callback() {

                @Override
                public void onGPSFixStateChanged(int fixed) {
                    boolean isFixed = fixed == 1;
                    mGps.updateFixed(isFixed);
                    if (!isFixed) {
                        mGps.updateSatelliteCount(0);
                    }
                    mGps.notifyUpdated();
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.GPSState is decoded. */
    private final ArsdkFeatureArdrone3.GPSState.Callback mGpsStateCallback =
            new ArsdkFeatureArdrone3.GPSState.Callback() {

                @Override
                public void onNumberOfSatelliteChanged(int satelliteCount) {
                    mGps.updateSatelliteCount(satelliteCount).notifyUpdated();
                }
            };
}
