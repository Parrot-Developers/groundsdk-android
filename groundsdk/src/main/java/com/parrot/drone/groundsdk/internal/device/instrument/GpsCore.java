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

package com.parrot.drone.groundsdk.internal.device.instrument;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.instrument.Gps;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.value.OptionalDoubleCore;
import com.parrot.drone.groundsdk.value.OptionalDouble;

/** Core class for the GPS instrument. */
public final class GpsCore extends SingletonComponentCore implements Gps {

    /** Description of GPS. */
    private static final ComponentDescriptor<Instrument, Gps> DESC = ComponentDescriptor.of(Gps.class);

    /** Whether current latitude/longitude information can be considered valid. */
    private boolean mHasLocation;

    /** Current latitude. */
    private double mLatitude;

    /** Current longitude. */
    private double mLongitude;

    /** Whether current altitude information can be considered valid. */
    private boolean mHasAltitude;

    /** Current altitude. */
    private double mAltitude;

    /** Whether GPS is currently fixed. */
    private boolean mFixed;

    /** Current amount of satellites used. */
    private int mSatelliteCount;

    /** Time of the latest location update. UTC, milliseconds. */
    private long mLocationTime;

    /** Current horizontal accuracy, -1 if invalid. */
    private double mHorizontalAccuracy;

    /** Current vertical accuracy. */
    @NonNull
    private final OptionalDoubleCore mVerticalAccuracy;

    /**
     * Constructor.
     *
     * @param instrumentStore store where this instrument belongs.
     */
    public GpsCore(@NonNull ComponentStore<Instrument> instrumentStore) {
        super(DESC, instrumentStore);
        mLocationTime = -1;
        mHorizontalAccuracy = -1;
        mVerticalAccuracy = new OptionalDoubleCore();
    }

    @Override
    public boolean isFixed() {
        return mFixed;
    }

    @Nullable
    @Override
    public Location lastKnownLocation() {
        Location location = null;
        if (mHasLocation) {
            location = new Location((String) null);
            location.setLatitude(mLatitude);
            location.setLongitude(mLongitude);
            if (mHasAltitude) {
                location.setAltitude(mAltitude);
            }
            if (Double.compare(mHorizontalAccuracy, -1) != 0) {
                location.setAccuracy((float) mHorizontalAccuracy);
            }
            if (mLocationTime != -1) {
                location.setTime(mLocationTime);
            }
        }
        return location;
    }

    @NonNull
    @Override
    public OptionalDouble getVerticalAccuracy() {
        return mVerticalAccuracy;
    }

    @Override
    public int getSatelliteCount() {
        return mSatelliteCount;
    }

    /**
     * Updates whether the drone GPS is currently fixed.
     *
     * @param fixed {@code true} to mark the GPS as fixed, {@code false} otherwise
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public GpsCore updateFixed(boolean fixed) {
        if (mFixed != fixed) {
            mChanged = true;
            mFixed = fixed;
        }
        return this;
    }

    /**
     * Updates current drone GPS location (latitude/longitude information).
     *
     * @param latitude  new GPS latitude
     * @param longitude new GPS longitude
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public GpsCore updateLocation(double latitude, double longitude) {
        if (!mHasLocation || Double.compare(mLatitude, latitude) != 0 || Double.compare(mLongitude, longitude) != 0) {
            mLatitude = latitude;
            mLongitude = longitude;
            mChanged = mHasLocation = true;
            mLocationTime = System.currentTimeMillis();
        }
        return this;
    }

    /**
     * Updates the location time.
     *
     * @param locationTime new location time
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public GpsCore updateLocationTime(long locationTime) {
        if (mLocationTime != locationTime) {
            mLocationTime = locationTime;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the drone current altitude.
     *
     * @param altitude new altitude
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public GpsCore updateAltitude(double altitude) {
        if (!mHasAltitude || Double.compare(mAltitude, altitude) != 0) {
            mAltitude = altitude;
            mHasAltitude = true;
            mChanged = mHasLocation;
        }
        return this;
    }

    /**
     * Updates the drone current horizontal accuracy.
     *
     * @param horizontalAccuracy new horizontal accuracy
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public GpsCore updateHorizontalAccuracy(double horizontalAccuracy) {
        if (Double.compare(mHorizontalAccuracy, horizontalAccuracy) != 0) {
            mHorizontalAccuracy = horizontalAccuracy;
            mChanged = mHasLocation;
        }
        return this;
    }

    /**
     * Updates the drone current vertical accuracy.
     *
     * @param verticalAccuracy new vertical accuracy
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public GpsCore updateVerticalAccuracy(double verticalAccuracy) {
        mChanged |= mVerticalAccuracy.setValue(verticalAccuracy);
        return this;
    }

    /**
     * Updates the drone current amount of used satellites.
     *
     * @param satelliteCount new amount of used satellite
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public GpsCore updateSatelliteCount(int satelliteCount) {
        if (mSatelliteCount != satelliteCount) {
            mSatelliteCount = satelliteCount;
            mChanged = true;
        }
        return this;
    }

    /**
     * Resets this instrument on disconnection.
     *
     * @return this object to allow chain calls
     */
    public GpsCore reset() {
        if (mFixed || mSatelliteCount != 0) {
            mFixed = false;
            mSatelliteCount = 0;
            mChanged = true;
        }
        return this;
    }
}
