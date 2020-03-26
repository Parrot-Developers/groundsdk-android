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
 * MAVLink command which allows to set a Region Of Interest.
 */
public final class SetRoiCommand extends MavlinkCommand {

    /** Value always used for region of interest mode; set to MAV_ROI_LOCATION. */
    private static final double ROI_MODE = 3;

    /** Latitude of the region of interest. */
    private final double mLatitude;

    /** Longitude of the region of interest. */
    private final double mLongitude;

    /** Altitude of the region of interest. */
    private final double mAltitude;

    /**
     * Constructor.
     *
     * @param latitude  latitude of the region of interest, in degrees
     * @param longitude longitude of the region of interest, in degrees
     * @param altitude  altitude of the region of interest above take off point, in meters
     */
    public SetRoiCommand(double latitude, double longitude, double altitude) {
        super(Type.SET_ROI);
        mLatitude = latitude;
        mLongitude = longitude;
        mAltitude = altitude;
    }

    /**
     * Retrieves the latitude of the region of interest, in degrees.
     *
     * @return region of interest latitude
     */
    public double getLatitude() {
        return mLatitude;
    }

    /**
     * Retrieves the longitude of the region of interest, in degrees.
     *
     * @return region of interest longitude
     */
    public double getLongitude() {
        return mLongitude;
    }

    /**
     * Retrieves the altitude of the region of interest above take off point, in meters.
     *
     * @return region of interest altitude
     */
    public double getAltitude() {
        return mAltitude;
    }

    @Override
    void write(@NonNull Writer writer, int index) throws IOException {
        write(writer, index, ROI_MODE, 0, 0, 0, mLatitude, mLongitude, mAltitude);
    }

    /**
     * Creates a set ROI command from generic MAVLink parameters.
     *
     * @param parameters generic command parameters
     *
     * @return the created command
     */
    @NonNull
    static SetRoiCommand create(@NonNull double[] parameters) {
        return new SetRoiCommand(parameters[4], parameters[5], parameters[6]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SetRoiCommand that = (SetRoiCommand) o;

        if (Double.compare(that.mLatitude, mLatitude) != 0) return false;
        if (Double.compare(that.mLongitude, mLongitude) != 0) return false;
        return Double.compare(that.mAltitude, mAltitude) == 0;
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
        return result;
    }
}
