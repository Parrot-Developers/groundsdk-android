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

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.Geofence;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.value.DoubleSettingCore;
import com.parrot.drone.groundsdk.internal.value.EnumSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;

/** Core class for Geofence. */
public class GeofenceCore extends SingletonComponentCore implements Geofence {

    /** Description of Geofence. */
    private static final ComponentDescriptor<Peripheral, Geofence> DESC = ComponentDescriptor.of(Geofence.class);

    /** Value used to mark that the a timestamp is invalid (either reset or never set). */
    private static final int NO_TIMESTAMP = -1;

    /** Engine-specific backend for Geofence. */
    public interface Backend {

        /**
         * Sets the maximum altitude value.
         *
         * @param altitude maximum altitude value to set
         *
         * @return {@code true} if the setting could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setMaxAltitude(double altitude);

        /**
         * Sets the maximum distance value.
         *
         * @param distance maximum distance value to set
         *
         * @return {@code true} if the setting could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setMaxDistance(double distance);

        /**
         * Sets the geofencing mode.
         *
         * @param mode geofencing mode to set
         *
         * @return {@code true} if the setting could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setMode(@NonNull Mode mode);
    }

    /** Max altitude setting. */
    @NonNull
    private final DoubleSettingCore mMaxAltitude;

    /** Max distance setting. */
    @NonNull
    private final DoubleSettingCore mMaxDistance;

    /** Geofencing mode setting. */
    @NonNull
    private final EnumSettingCore<Mode> mMode;

    /** Current geofence center latitude. */
    private double mLatitude;

    /** Current geofence center longitude. */
    private double mLongitude;

    /**
     * Time of the latest geofence center location update. {@link #NO_TIMESTAMP} if location has been reset or never
     * been updated.
     */
    private long mLocationTimeStamp;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this component provider belongs
     * @param backend         backend used to forward actions to the engine
     */
    public GeofenceCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mMaxAltitude = new DoubleSettingCore(new SettingController(this::onSettingChange), backend::setMaxAltitude);
        mMaxDistance = new DoubleSettingCore(new SettingController(this::onSettingChange), backend::setMaxDistance);
        mMode = new EnumSettingCore<>(Mode.ALTITUDE, new SettingController(this::onSettingChange), backend::setMode);
        mLocationTimeStamp = NO_TIMESTAMP;
    }

    @Override
    public void unpublish() {
        super.unpublish();
        cancelSettingsRollbacks();
    }

    @NonNull
    @Override
    public DoubleSettingCore maxAltitude() {
        return mMaxAltitude;
    }

    @NonNull
    @Override
    public DoubleSettingCore maxDistance() {
        return mMaxDistance;
    }

    @NonNull
    @Override
    public EnumSettingCore<Mode> mode() {
        return mMode;
    }

    @Nullable
    @Override
    public Location getCenter() {
        Location location = null;
        if (mLocationTimeStamp != NO_TIMESTAMP) {
            location = new Location((String) null);
            location.setLatitude(mLatitude);
            location.setLongitude(mLongitude);
            location.setTime(mLocationTimeStamp);
        }
        return location;
    }

    /**
     * Updates the geofence center location after a change in the backend.
     *
     * @param latitude  the new latitude
     * @param longitude the new longitude
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GeofenceCore updateCenter(double latitude, double longitude) {
        if (mLocationTimeStamp == NO_TIMESTAMP
            || Double.compare(mLatitude, latitude) != 0 || Double.compare(mLongitude, longitude) != 0) {
            mLocationTimeStamp = System.currentTimeMillis();
            mLatitude = latitude;
            mLongitude = longitude;
            mChanged = true;
        }
        return this;
    }

    /**
     * Resets the current geofence center location.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GeofenceCore resetCenter() {
        if (mLocationTimeStamp != NO_TIMESTAMP) {
            mLocationTimeStamp = NO_TIMESTAMP;
            mLatitude = mLongitude = 0;
            mChanged = true;
        }
        return this;
    }

    /**
     * Cancels all pending settings rollbacks.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GeofenceCore cancelSettingsRollbacks() {
        mMaxAltitude.cancelRollback();
        mMaxDistance.cancelRollback();
        mMode.cancelRollback();
        return this;
    }

    /**
     * Notified when a user setting changes.
     * <p>
     * In case the change originates from the user modifying the setting value, updates the store to show the setting
     * is updating.
     *
     * @param fromUser {@code true} if the change originates from the user, otherwise {@code false}
     */
    private void onSettingChange(boolean fromUser) {
        mChanged = true;
        if (fromUser) {
            notifyUpdated();
        }
    }
}
