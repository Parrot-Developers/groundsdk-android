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

package com.parrot.drone.groundsdk.internal.device.peripheral.tracking;

import com.parrot.drone.groundsdk.device.peripheral.TargetTracker;

/**
 * Core class for TargetTrajectory.
 */
final class TargetTrajectoryCore extends TargetTracker.TargetTrajectory {

    /** Trajectory latitude, in degrees. */
    private double mLatitude;

    /** Trajectory longitude, in degrees. */
    private double mLongitude;

    /** Trajectory altitude. in meters, relative to sea level. */
    private double mAltitude;

    /** Trajectory north speed, in meters per second. */
    private double mNorthSpeed;

    /** Trajectory east speed, in meters per second. */
    private double mEastSpeed;

    /** Trajectory down speed, in meters per second. */
    private double mDownSpeed;

    @Override
    public double getLatitude() {
        return mLatitude;
    }

    @Override
    public double getLongitude() {
        return mLongitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TargetTrajectoryCore that = (TargetTrajectoryCore) o;

        return Double.compare(that.mLatitude, mLatitude) == 0
               && Double.compare(that.mLongitude, mLongitude) == 0
               && Double.compare(that.mAltitude, mAltitude) == 0
               && Double.compare(that.mNorthSpeed, mNorthSpeed) == 0
               && Double.compare(that.mEastSpeed, mEastSpeed) == 0
               && Double.compare(that.mDownSpeed, mDownSpeed) == 0;
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
        temp = Double.doubleToLongBits(mNorthSpeed);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mEastSpeed);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mDownSpeed);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public double getAltitude() {
        return mAltitude;
    }

    @Override
    public double getNorthSpeed() {
        return mNorthSpeed;
    }

    @Override
    public double getEastSpeed() {
        return mEastSpeed;
    }

    @Override
    public double getDownSpeed() {
        return mDownSpeed;
    }

    /**
     * Updates trajectory.
     *
     * @param latitude   new latitude
     * @param longitude  new longitude
     * @param altitude   new altitude
     * @param northSpeed new north speed
     * @param eastSpeed  new east speed
     * @param downSpeed  new down speed
     *
     * @return {@code true} in case the trajectory did change, otherwise {@code false}
     */
    boolean update(double latitude, double longitude, double altitude,
                   double northSpeed, double eastSpeed, double downSpeed) {
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
        if (Double.compare(mNorthSpeed, northSpeed) != 0) {
            mNorthSpeed = northSpeed;
            changed = true;
        }
        if (Double.compare(mEastSpeed, eastSpeed) != 0) {
            mEastSpeed = eastSpeed;
            changed = true;
        }
        if (Double.compare(mDownSpeed, downSpeed) != 0) {
            mDownSpeed = downSpeed;
            changed = true;
        }
        return changed;
    }
}
