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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.LocationDirective.Orientation;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;

/**
 * Core class for GuidedPilotingItf.
 */
public class GuidedPilotingItfCore extends ActivablePilotingItfCore implements GuidedPilotingItf {

    /** Core class for LocationDirective. */
    public static class LocationDirectiveCore implements GuidedPilotingItf.LocationDirective {

        /** Core class for Orientation. */
        public static class OrientationCore extends Orientation {

            /** Orientation mode of the guided flight. */
            public enum Mode {
                /** The drone won't change its orientation. */
                NONE,

                /** The drone will make a rotation to look in direction of the given location. */
                TO_TARGET,

                /** The drone will orientate itself to the given heading before moving to the location. */
                START,

                /** The drone will orientate itself to the given heading while moving to the location. */
                DURING
            }

            /** Orientation mode. */
            @NonNull
            private final Mode mMode;

            /** Heading for {@code START} and {@code DURING} modes. */
            private final double mHeading;

            /**
             * Constructor.
             *
             * @param mode    orientation mode
             * @param heading heading for {@code START} and {@code DURING} modes
             */
            public OrientationCore(@NonNull Mode mode, double heading) {
                mMode = mode;
                mHeading = heading;
            }

            @NonNull
            @Override
            public Mode getMode() {
                return mMode;
            }

            @Override
            public double getHeading() {
                return mHeading;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                OrientationCore that = (OrientationCore) o;

                return Double.compare(that.mHeading, mHeading) == 0 && mMode == that.mMode;
            }

            @Override
            public int hashCode() {
                int result;
                long temp;
                result = mMode.hashCode();
                temp = Double.doubleToLongBits(mHeading);
                result = 31 * result + (int) (temp ^ (temp >>> 32));
                return result;
            }
        }

        /** Latitude of the location to reach. */
        private final double mLatitude;

        /** Longitude of the location to reach. */
        private final double mLongitude;

        /** Altitude of the location to reach. */
        private final double mAltitude;

        /** Orientation. */
        @NonNull
        private final Orientation mOrientation;

        /**
         * Creates an location guided flight.
         *
         * @param latitude    latitude of the location (in degrees) to reach
         * @param longitude   longitude of the location (in degrees) to reach
         * @param altitude    altitude above sea level (in m) to reach
         * @param orientation orientation of the location guided flight
         */
        public LocationDirectiveCore(double latitude, double longitude, double altitude,
                                     @NonNull Orientation orientation) {
            mLatitude = latitude;
            mLongitude = longitude;
            mAltitude = altitude;
            mOrientation = orientation;
        }

        @NonNull
        @Override
        public Type getType() {
            return Type.ABSOLUTE_LOCATION;
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
        public Orientation getOrientation() {
            return mOrientation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            LocationDirectiveCore that = (LocationDirectiveCore) o;

            return Double.compare(that.mLatitude, mLatitude) == 0 && Double.compare(that.mLongitude, mLongitude) == 0 &&
                   Double.compare(that.mAltitude, mAltitude) == 0 && mOrientation.equals(that.mOrientation);
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
            result = 31 * result + mOrientation.hashCode();
            return result;
        }
    }

    /** Core class for RelativeMoveDirective. */
    public static class RelativeMoveDirectiveCore implements RelativeMoveDirective {

        /** Forward component of the move. */
        private final double mForwardComponent;

        /** Right component of the move. */
        private final double mRightComponent;

        /** Downward component of the move. */
        private final double mDownwardComponent;

        /** Heading rotation. */
        private final double mHeadingRotation;

        /**
         * Creates a relative move directive.
         *
         * @param forwardComponent  desired displacement along the front axis, in meters
         * @param rightComponent    desired displacement along the right axis, in meters
         * @param downwardComponent desired displacement along the down axis, in meters
         * @param headingRotation   desired relative rotation of heading, in degrees
         */
        public RelativeMoveDirectiveCore(double forwardComponent, double rightComponent, double downwardComponent,
                                         double headingRotation) {
            mForwardComponent = forwardComponent;
            mRightComponent = rightComponent;
            mDownwardComponent = downwardComponent;
            mHeadingRotation = headingRotation;
        }

        @NonNull
        @Override
        public Type getType() {
            return Type.RELATIVE_MOVE;
        }

        @Override
        public double getForwardComponent() {
            return mForwardComponent;
        }

        @Override
        public double getRightComponent() {
            return mRightComponent;
        }

        @Override
        public double getDownwardComponent() {
            return mDownwardComponent;
        }

        @Override
        public double getHeadingRotation() {
            return mHeadingRotation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            RelativeMoveDirectiveCore that = (RelativeMoveDirectiveCore) o;

            return Double.compare(that.mForwardComponent, mForwardComponent) == 0 &&
                   Double.compare(that.mRightComponent, mRightComponent) == 0 &&
                   Double.compare(that.mDownwardComponent, mDownwardComponent) == 0 &&
                   Double.compare(that.mHeadingRotation, mHeadingRotation) == 0;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(mForwardComponent);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(mRightComponent);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(mDownwardComponent);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(mHeadingRotation);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

    /** Core class for FinishedLocationFlightInfo. */
    public static class FinishedLocationFlightInfoCore implements FinishedLocationFlightInfo {

        /** Initial location directive. */
        @NonNull
        private final GuidedPilotingItf.LocationDirective mDirective;

        /** {@code true} if the guided flight succeeded. */
        private final boolean mSuccess;

        /**
         * Constructor.
         *
         * @param directive the initial directive
         * @param success   {@code true} if the guided flight succeeded
         */
        public FinishedLocationFlightInfoCore(@NonNull GuidedPilotingItf.LocationDirective directive, boolean success) {
            mDirective = directive;
            mSuccess = success;
        }

        @NonNull
        @Override
        public Type getType() {
            return Type.ABSOLUTE_LOCATION;
        }

        @Override
        public boolean wasSuccessful() {
            return mSuccess;
        }

        @NonNull
        @Override
        public GuidedPilotingItf.LocationDirective getDirective() {
            return mDirective;
        }
    }

    /** Core class for FinishedRelativeMoveFlightInfo. */
    public static class FinishedRelativeMoveFlightInfoCore implements FinishedRelativeMoveFlightInfo {

        /** Initial location directive. */
        @NonNull
        private final RelativeMoveDirective mDirective;

        /** {@code true} if the guided flight succeeded. */
        private final boolean mSuccess;

        /** Forward component of the actual move. */
        private final double mActualForwardComponent;

        /** Right component of the actual move. */
        private final double mActualRightComponent;

        /** Downward component of the actual move. */
        private final double mActualDownwardComponent;

        /** Heading rotation of the actual move. */
        private final double mActualHeadingRotation;

        /**
         * Constructor.
         *
         * @param directive               the initial directive
         * @param success                 {@code true} if the guided flight succeeded
         * @param actualForwardComponent  forward component of the actual move
         * @param actualRightComponent    right component of the actual move
         * @param actualDownwardComponent downward component of the actual move
         * @param actualHeadingRotation   heading rotation component of the actual move
         */
        public FinishedRelativeMoveFlightInfoCore(@NonNull RelativeMoveDirective directive, boolean success,
                                                  double actualForwardComponent, double actualRightComponent,
                                                  double actualDownwardComponent, double actualHeadingRotation) {
            mDirective = directive;
            mSuccess = success;
            mActualForwardComponent = actualForwardComponent;
            mActualRightComponent = actualRightComponent;
            mActualDownwardComponent = actualDownwardComponent;
            mActualHeadingRotation = actualHeadingRotation;
        }

        @NonNull
        @Override
        public Type getType() {
            return Type.RELATIVE_MOVE;
        }

        @Override
        public boolean wasSuccessful() {
            return mSuccess;
        }

        @NonNull
        @Override
        public RelativeMoveDirective getDirective() {
            return mDirective;
        }

        @Override
        public double getActualForwardComponent() {
            return mActualForwardComponent;
        }

        @Override
        public double getActualRightComponent() {
            return mActualRightComponent;
        }

        @Override
        public double getActualDownwardComponent() {
            return mActualDownwardComponent;
        }

        @Override
        public double getActualHeadingRotation() {
            return mActualHeadingRotation;
        }
    }

    /** Description of GuidedPilotingItf. */
    private static final ComponentDescriptor<PilotingItf, GuidedPilotingItf> DESC =
            ComponentDescriptor.of(GuidedPilotingItf.class);

    /** Backend of a GuidedPilotingItfCore which handles the messages. */
    public interface Backend extends ActivablePilotingItfCore.Backend {

        /**
         * Asks the drone to move to a specified location and to rotate heading to the specified value.
         *
         * @param latitude    latitude of the location (in degrees) to reach
         * @param longitude   longitude of the location (in degrees) to reach
         * @param altitude    altitude above sea level (in meters) to reach
         * @param orientation orientation of the location guided flight
         */
        void moveToLocation(double latitude, double longitude, double altitude,
                            @NonNull Orientation orientation);

        /**
         * Asks the drone to move to a relative position, and rotate heading by a given angle.
         *
         * @param dx   desired displacement along the front axis, in meters
         * @param dy   desired displacement along the right axis, in meters
         * @param dz   desired displacement along the down axis, in meters
         * @param dpsi desired relative rotation of heading, in degrees
         */
        void moveToRelativePosition(double dx, double dy, double dz, double dpsi);
    }

    /** Backend of this interface. */
    @NonNull
    private final Backend mBackend;

    /** Current guided flight directive. */
    @Nullable
    private Directive mCurrentDirective;

    /** Latest terminated guided flight information. */
    @Nullable
    private FinishedFlightInfo mLatestFinishedFlightInfo;

    /**
     * Constructor.
     *
     * @param pilotingItfStore store where this piloting interface belongs
     * @param backend          backend used to forward actions to the engine
     */
    public GuidedPilotingItfCore(@NonNull ComponentStore<PilotingItf> pilotingItfStore, @NonNull Backend backend) {
        super(DESC, pilotingItfStore, backend);
        mBackend = backend;
    }

    @Override
    public void moveToLocation(double latitude, double longitude, double altitude, @NonNull Orientation orientation) {
        mBackend.moveToLocation(latitude, longitude, altitude, orientation);
    }

    @Override
    public void moveToRelativePosition(double forwardComponent, double rightComponent, double downwardComponent,
                                       double headingRotation) {
        mBackend.moveToRelativePosition(forwardComponent, rightComponent, downwardComponent, headingRotation);
    }

    @Nullable
    @Override
    public Directive getCurrentDirective() {
        return mCurrentDirective;
    }

    @Nullable
    @Override
    public FinishedFlightInfo getLatestFinishedFlightInfo() {
        return mLatestFinishedFlightInfo;
    }

    //region backend methods

    /**
     * Updates the current directive.
     *
     * @param directive the new directive
     *
     * @return the object, to allow chain calls
     */
    public GuidedPilotingItfCore updateCurrentDirective(@Nullable Directive directive) {
        if ((directive == null && mCurrentDirective != null) ||
            (directive != null && !directive.equals(mCurrentDirective))) {
            mCurrentDirective = directive;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the latest finished flight info.
     *
     * @param flightInfo the finished flight info to update
     *
     * @return the object, to allow chain calls
     */
    public GuidedPilotingItfCore updateLatestFinishedFlightInfo(@NonNull FinishedFlightInfo flightInfo) {
        if (!flightInfo.equals(mLatestFinishedFlightInfo)) {
            mLatestFinishedFlightInfo = flightInfo;
            mChanged = true;
        }
        return this;
    }

    //endregion backend methods
}
