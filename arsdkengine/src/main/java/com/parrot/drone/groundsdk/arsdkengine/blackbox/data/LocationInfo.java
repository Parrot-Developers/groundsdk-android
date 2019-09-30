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

package com.parrot.drone.groundsdk.arsdkengine.blackbox.data;

import android.location.Location;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Geo location information.
 */
public final class LocationInfo {

    /** Latitude. */
    @Expose
    @SerializedName("latitude")
    private double mLatitude;

    /** Longitude. */
    @Expose
    @SerializedName("longitude")
    private double mLongitude;

    /** Altitude. */
    @Expose
    @SerializedName("altitude")
    private double mAltitude;

    /**
     * Constructor.
     *
     * @param latitude  latitude value
     * @param longitude longitude value
     * @param altitude  altitude value
     */
    public LocationInfo(double latitude, double longitude, double altitude) {
        mLatitude = latitude;
        mLongitude = longitude;
        mAltitude = altitude;
    }

    /**
     * Default constructor.
     */
    LocationInfo() {
        this(500, 500, 500);
    }

    /**
     * Copy constructor.
     *
     * @param other location info to copy data from
     */
    LocationInfo(@NonNull LocationInfo other) {
        this(other.mLatitude, other.mLongitude, other.mAltitude);
    }

    /**
     * Updates location information.
     *
     * @param location location information to update from
     *
     * @return {@code true} if location information changed, otherwise {@code false}
     */
    boolean update(@NonNull LocationInfo location) {
        return update(location.mLatitude, location.mLongitude, location.mAltitude);
    }

    /**
     * Updates location information.
     *
     * @param location android location record to update from
     *
     * @return {@code true} if location information changed, otherwise {@code false}
     */
    boolean update(@NonNull Location location) {
        return update(location.getLatitude(), location.getLongitude(), location.getAltitude());
    }

    /**
     * Updates location information.
     *
     * @param latitude  latitude value
     * @param longitude longitude value
     * @param altitude  altitude value
     *
     * @return {@code true} if location information changed, otherwise {@code false}
     */
    private boolean update(double latitude, double longitude, double altitude) {
        boolean changed = false;
        if (Double.compare(mLatitude, latitude) != 0) {
            mLatitude = latitude;
            changed = true;
        }
        if (Double.compare(mLongitude, longitude) != 0) {
            mLongitude = longitude;
            changed = true;
        }
        if (Double.compare(mAltitude, altitude) != 0) {
            mAltitude = altitude;
            changed = true;
        }
        return changed;
    }
}
