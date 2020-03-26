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

package com.parrot.drone.groundsdk.mavlink;

import java.io.IOException;
import java.io.Writer;

import androidx.annotation.NonNull;

/**
 * MAVLink command which allows to navigate to a waypoint.
 */
public final class NavigateToWaypointCommand extends MavlinkCommand {

    /** Latitude of the waypoint. */
    private final double mLatitude;

    /** Longitude of the waypoint. */
    private final double mLongitude;

    /** Altitude of the waypoint. */
    private final double mAltitude;

    /** Desired yaw angle at waypoint. */
    private final double mYaw;

    /** Hold time: time to stay at waypoint. */
    private final double mHoldTime;

    /** Acceptance radius: if the sphere with this radius is hit, the waypoint counts as reached. */
    private final double mAcceptanceRadius;

    /**
     * Constructor.
     *
     * @param latitude         latitude of the waypoint, in degrees
     * @param longitude        longitude of the waypoint, in degrees
     * @param altitude         altitude of the waypoint above take off point, in meters
     * @param yaw              desired yaw angle at waypoint, relative to the North in degrees (clockwise)
     * @param holdTime         time to stay at waypoint, in seconds
     * @param acceptanceRadius acceptance radius, in meters
     */
    public NavigateToWaypointCommand(double latitude, double longitude, double altitude, double yaw,
                                     double holdTime, double acceptanceRadius) {
        super(Type.NAVIGATE_TO_WAYPOINT);
        mLatitude = latitude;
        mLongitude = longitude;
        mAltitude = altitude;
        mYaw = yaw;
        mHoldTime = holdTime;
        mAcceptanceRadius = acceptanceRadius;
    }

    /**
     * Retrieves the latitude of the waypoint, in degrees.
     *
     * @return waypoint latitude
     */
    public double getLatitude() {
        return mLatitude;
    }

    /**
     * Retrieves the longitude of the waypoint, in degrees.
     *
     * @return waypoint longitude
     */
    public double getLongitude() {
        return mLongitude;
    }

    /**
     * Retrieves the altitude of the waypoint above take off point, in meters.
     *
     * @return waypoint altitude
     */
    public double getAltitude() {
        return mAltitude;
    }

    /**
     * Retrieves the desired yaw angle at waypoint, relative to the North in degrees (clockwise).
     *
     * @return yaw angle
     */
    public double getYaw() {
        return mYaw;
    }

    /**
     * Retrieves the time to stay at waypoint, in seconds.
     *
     * @return hold time
     */
    public double getHoldTime() {
        return mHoldTime;
    }

    /**
     * Retrieves the acceptance radius, in meters.
     * If the sphere with this radius is hit, the waypoint counts as reached.
     *
     * @return acceptance radius
     */
    public double getAcceptanceRadius() {
        return mAcceptanceRadius;
    }

    @Override
    void write(@NonNull Writer writer, int index) throws IOException {
        write(writer, index, mHoldTime, mAcceptanceRadius, 0, mYaw, mLatitude, mLongitude, mAltitude);
    }

    /**
     * Creates a navigate to waypoint command from generic MAVLink parameters.
     *
     * @param parameters generic command parameters
     *
     * @return the created command
     */
    @NonNull
    static NavigateToWaypointCommand create(@NonNull double[] parameters) {
        return new NavigateToWaypointCommand(parameters[4], parameters[5], parameters[6], parameters[3],
                parameters[0], parameters[1]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NavigateToWaypointCommand that = (NavigateToWaypointCommand) o;

        if (Double.compare(that.mLatitude, mLatitude) != 0) return false;
        if (Double.compare(that.mLongitude, mLongitude) != 0) return false;
        if (Double.compare(that.mAltitude, mAltitude) != 0) return false;
        if (Double.compare(that.mYaw, mYaw) != 0) return false;
        if (Double.compare(that.mHoldTime, mHoldTime) != 0) return false;
        return Double.compare(that.mAcceptanceRadius, mAcceptanceRadius) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(mLatitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mLongitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mAltitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mYaw);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mHoldTime);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mAcceptanceRadius);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
