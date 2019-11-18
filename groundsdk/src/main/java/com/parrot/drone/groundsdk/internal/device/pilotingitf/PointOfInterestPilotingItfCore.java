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

package com.parrot.drone.groundsdk.internal.device.pilotingitf;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PointOfInterestPilotingItf;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;

import static com.parrot.drone.groundsdk.internal.value.IntegerRangeCore.SIGNED_PERCENTAGE;

/**
 * Core class for PointOfInterestPilotingItf.
 */
public class PointOfInterestPilotingItfCore extends ActivablePilotingItfCore implements PointOfInterestPilotingItf {

    /** Core class for Point Of Interest. */
    public static class PointOfInterestCore implements PointOfInterest {

        /** Latitude of the location to look at. */
        private final double mLatitude;

        /** Longitude of the location to look at. */
        private final double mLongitude;

        /** Altitude of the location to look at. */
        private final double mAltitude;

        /** Point Of Interest operating mode. */
        @NonNull
        private final Mode mMode;

        /**
         * Creates a target for a piloted Point Of Interest.
         *
         * @param latitude  latitude of the location (in degrees) to look at
         * @param longitude longitude of the location (in degrees) to look at
         * @param altitude  altitude above take off point (in m) to look at
         * @param mode      operating mode
         */
        public PointOfInterestCore(double latitude, double longitude, double altitude,
                                   @NonNull PointOfInterestPilotingItf.Mode mode) {
            mLatitude = latitude;
            mLongitude = longitude;
            mAltitude = altitude;
            mMode = mode;
        }

        @Override
        public double getLatitude() {
            return mLatitude;
        }

        @Override
        public double getLongitude() {
            return mLongitude;
        }

        @Override
        public double getAltitude() {
            return mAltitude;
        }

        @NonNull
        @Override
        public Mode getMode() {
            return mMode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            PointOfInterestCore that = (PointOfInterestCore) o;

            return Double.compare(that.mLatitude, mLatitude) == 0 && Double.compare(that.mLongitude, mLongitude) == 0 &&
                   Double.compare(that.mAltitude, mAltitude) == 0 && mMode == that.mMode;
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
            result = 31 * result + mMode.hashCode();
            return result;
        }
    }

    /** Description of PointOfInterestPilotingItf. */
    private static final ComponentDescriptor<PilotingItf, PointOfInterestPilotingItf> DESC =
            ComponentDescriptor.of(PointOfInterestPilotingItf.class);

    /** Backend of a PointOfInterestPilotingItf which handles the messages. */
    public interface Backend extends ActivablePilotingItfCore.Backend {

        /**
         * Starts a piloted Point Of Interest.
         *
         * @param latitude  latitude of the location (in degrees) to look at
         * @param longitude longitude of the location (in degrees) to look at
         * @param altitude  altitude above take off point (in meters) to look at
         * @param mode      Point Of Interest mode
         */
        void start(double latitude, double longitude, double altitude, @NonNull Mode mode);

        /**
         * Sets the piloting command pitch value.
         *
         * @param pitch piloting command pitch
         */
        void setPitch(int pitch);

        /**
         * Sets the piloting command roll value.
         *
         * @param roll piloting command pitch
         */
        void setRoll(int roll);

        /**
         * Sets the piloting command vertical speed value.
         *
         * @param verticalSpeed piloting command vertical speed
         */
        void setVerticalSpeed(int verticalSpeed);
    }

    /** Backend of this interface. */
    @NonNull
    private final Backend mBackend;

    /** Current Point Of Interest. */
    @Nullable
    private PointOfInterest mCurrentPointOfInterest;

    /**
     * Constructor.
     *
     * @param pilotingItfStore store where this piloting interface belongs
     * @param backend          backend used to forward actions to the engine
     */
    public PointOfInterestPilotingItfCore(@NonNull ComponentStore<PilotingItf> pilotingItfStore,
                                          @NonNull Backend backend) {
        super(DESC, pilotingItfStore, backend);
        mBackend = backend;
    }

    @Override
    public void start(double latitude, double longitude, double altitude) {
        mBackend.start(latitude, longitude, altitude, Mode.LOCKED_GIMBAL);
    }

    @Override
    public void start(double latitude, double longitude, double altitude, @NonNull Mode mode) {
        mBackend.start(latitude, longitude, altitude, mode);
    }

    @Nullable
    @Override
    public PointOfInterest getCurrentPointOfInterest() {
        return mCurrentPointOfInterest;
    }

    @Override
    public void setPitch(@IntRange(from = -100, to = 100) int value) {
        mBackend.setPitch(SIGNED_PERCENTAGE.clamp(value));
    }

    @Override
    public void setRoll(@IntRange(from = -100, to = 100) int value) {
        mBackend.setRoll(SIGNED_PERCENTAGE.clamp(value));
    }

    @Override
    public void setVerticalSpeed(@IntRange(from = -100, to = 100) int value) {
        mBackend.setVerticalSpeed(SIGNED_PERCENTAGE.clamp(value));
    }

    //region backend methods

    /**
     * Updates the current Point Of Interest.
     *
     * @param currentPointOfInterest the new current Point Of Interest, {@code null} otherwise
     *
     * @return the object, to allow chain calls
     */
    public PointOfInterestPilotingItfCore updateCurrentPointOfInterest(
            @Nullable PointOfInterest currentPointOfInterest) {
        if ((currentPointOfInterest == null && mCurrentPointOfInterest != null) ||
            (currentPointOfInterest != null && !currentPointOfInterest.equals(mCurrentPointOfInterest))) {
            mCurrentPointOfInterest = currentPointOfInterest;
            mChanged = true;
        }
        return this;
    }

    //endregion backend methods
}
