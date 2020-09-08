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

package com.parrot.drone.groundsdk.device.pilotingitf;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;

import java.util.Objects;
import java.util.Set;

/**
 * Guided piloting interface for copters.
 * <p>
 * This piloting interface can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPilotingItf(GuidedPilotingItf.class)}</pre>
 * <p>
 * This interface is automatically activated when a guided flight is started, with
 * {@link #moveToLocation(double, double, double, LocationDirective.Orientation) moveToLocation()} or
 * {@link #moveToRelativePosition(double, double, double, double) moveToRelativePosition()}.
 *
 * @see Drone#getPilotingItf(Class)
 * @see Drone#getPilotingItf(Class, Ref.Observer)
 */
public interface GuidedPilotingItf extends PilotingItf, Activable {

    /** Guided flight type. */
    enum Type {

        /**
         * A {@link Directive} of that type is actually a {@link LocationDirective} and can be
         * safely casted as such.<br>
         * A {@link FinishedFlightInfo} of that type is actually a {@link FinishedLocationFlightInfo}
         * and can be safely casted as such.
         */
        ABSOLUTE_LOCATION,

        /**
         * A {@link Directive} of that type is actually a {@link RelativeMoveDirective} and can be
         * safely casted as such.<br>
         * A {@link FinishedFlightInfo} of that type is actually a {@link FinishedRelativeMoveFlightInfo}
         * and can be safely casted as such.
         */
        RELATIVE_MOVE
    }

    /**
     * Reason why this piloting interface is currently unavailable.
     */
    enum UnavailabilityReason {

        /** Drone GPS accuracy is too weak. */
        DRONE_GPS_INFO_INACCURATE,

        /** Drone needs to be calibrated. */
        DRONE_NOT_CALIBRATED,

        /** Drone is not flying. */
        DRONE_NOT_FLYING,

        /** Drone is currently too close to the ground. */
        DRONE_TOO_CLOSE_TO_GROUND,

        /** Drone is above maximum altitude. */
        DRONE_ABOVE_MAX_ALTITUDE,

        /** Drone is out of the geofence bounds. */
        DRONE_OUT_GEOFENCE
    }

    /** A guided flight directive. */
    class Directive {

        /**
         * Requested speed for the flight.
         * <p>
         * When attached to a flight {@link Directive}, allows to specify the desired horizontal, vertical and
         * rotation speed values for the flight. <br>
         * Note that the provided values are considered maximum values: the drone will try its best to respect the
         * specified speeds, but the actual speeds may be lower depending on the situation. <br>
         * Specifying incoherent speed values with regard to the specified location target will result in a failed move.
         */
        public static final class Speed {

            /** Requested horizontal speed, in meters/second. */
            @FloatRange(from = 0.0, fromInclusive = false)
            private final double mHorizontal;

            /** Requested vertical speed, in meters/second. */
            @FloatRange(from = 0.0, fromInclusive = false)
            private final double mVertical;

            /** Requested rotation speed, in degrees/second. */
            @FloatRange(from = 0.0, fromInclusive = false)
            private final double mRotation;

            /**
             * Retrieves requested (maximum) horizontal speed.
             *
             * @return requested horizontal speed, in meters/second
             */
            @FloatRange(from = 0.0, fromInclusive = false)
            public double getHorizontalMax() {
                return mHorizontal;
            }

            /**
             * Retrieves requested (maximum) vertical speed.
             *
             * @return requested vertical speed, in meters/second
             */
            @FloatRange(from = 0.0, fromInclusive = false)
            public double getVerticalMax() {
                return mVertical;
            }

            /**
             * Retrieves requested (maximum) rotation speed.
             *
             * @return requested rotation speed, in degrees/second
             */
            @FloatRange(from = 0.0, fromInclusive = false)
            public double getRotationMax() {
                return mRotation;
            }

            /**
             * Constructor.
             *
             * @param horizontal requested horizontal speed, in meters/second
             * @param vertical   requested vertical speed, in meters/second
             * @param rotation   requested rotation speed, in degrees/second
             */
            public Speed(@FloatRange(from = 0.0, fromInclusive = false) double horizontal,
                         @FloatRange(from = 0.0, fromInclusive = false) double vertical,
                         @FloatRange(from = 0.0, fromInclusive = false) double rotation) {
                mHorizontal = horizontal;
                mVertical = vertical;
                mRotation = rotation;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Speed speed = (Speed) o;

                return Double.compare(mHorizontal, speed.mHorizontal) == 0
                       && Double.compare(mVertical, speed.mVertical) == 0
                       && Double.compare(mRotation, speed.mRotation) == 0;
            }

            @Override
            public int hashCode() {
                int result;
                long temp;
                temp = Double.doubleToLongBits(mHorizontal);
                result = (int) (temp ^ (temp >>> 32));
                temp = Double.doubleToLongBits(mVertical);
                result = 31 * result + (int) (temp ^ (temp >>> 32));
                temp = Double.doubleToLongBits(mRotation);
                result = 31 * result + (int) (temp ^ (temp >>> 32));
                return result;
            }
        }

        /** Directive type. */
        @NonNull
        private final GuidedPilotingItf.Type mType;

        /** Directive speed, {@code null} if not specified. */
        @Nullable
        private final Speed mSpeed;

        /**
         * Constructor.
         *
         * @param type  directive type
         * @param speed directive speed, {@code null} when unspecified
         */
        private Directive(@NonNull GuidedPilotingItf.Type type, @Nullable Speed speed) {
            mType = type;
            mSpeed = speed;
        }

        /**
         * Retrieves the guided flight type.
         *
         * @return the guided flight type
         */
        public final GuidedPilotingItf.Type getType() {
            return mType;
        }

        /**
         * Retrieves the requested guided flight speed.
         *
         * @return guided flight speed
         */
        @Nullable
        public final Speed getSpeed() {
            return mSpeed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Directive that = (Directive) o;

            return mType == that.mType
                   && Objects.equals(mSpeed, that.mSpeed);
        }

        @Override
        public int hashCode() {
            int result = mType.hashCode();
            result = 31 * result + (mSpeed != null ? mSpeed.hashCode() : 0);
            return result;
        }
    }

    /**
     * A location move directive.
     * <p>
     * Allows to instruct the drone to move to a specified location, and rotate its heading to a specified value. <br>
     * Optionally, desired speed values for the move may also be specified.
     *
     * @see #move(Directive)
     */
    final class LocationDirective extends Directive {

        /**
         * Orientation of a location directive.
         */
        public static final class Orientation {

            /**
             * Orientation for which the drone won't change its heading.
             */
            @NonNull
            public static final Orientation NONE = new Orientation(Mode.NONE, 0);

            /**
             * Orientation for which the drone will make a rotation to look in direction of the given location before
             * moving to the location.
             */
            @NonNull
            public static final Orientation TO_TARGET = new Orientation(Mode.TO_TARGET, 0);

            /**
             * Creates an orientation for which the drone will orientate itself to the given
             * heading before moving to the location.
             *
             * @param heading the heading relative to the North in degrees (clockwise)
             *
             * @return the created orientation
             */
            @NonNull
            public static Orientation headingStart(double heading) {
                return new Orientation(Mode.START, heading);
            }

            /**
             * Creates an orientation for which the drone will orientate itself to the given heading
             * while moving to the location.
             *
             * @param heading the heading relative to the North in degrees (clockwise)
             *
             * @return the created orientation
             */
            @NonNull
            public static Orientation headingDuring(double heading) {
                return new Orientation(Mode.DURING, heading);
            }

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
            private Orientation(@NonNull Mode mode, double heading) {
                mMode = mode;
                mHeading = heading;
            }

            /**
             * Retrieves the orientation mode.
             *
             * @return the orientation mode
             */
            @NonNull
            public Mode getMode() {
                return mMode;
            }

            /**
             * Retrieves the heading (relative to the North in degrees, clockwise). This value is meaningful for
             * {@code START} or {@code DURING} modes only.
             *
             * @return the heading in degrees
             */
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

                Orientation that = (Orientation) o;

                return Double.compare(that.mHeading, mHeading) == 0
                       && mMode == that.mMode;
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
         * @param speed       requested movement speed; pass {@code null} to not specify any speed requirement
         */
        public LocationDirective(double latitude, double longitude, double altitude,
                                 @NonNull GuidedPilotingItf.LocationDirective.Orientation orientation,
                                 @Nullable Speed speed) {
            super(GuidedPilotingItf.Type.ABSOLUTE_LOCATION, speed);
            mLatitude = latitude;
            mLongitude = longitude;
            mAltitude = altitude;
            mOrientation = orientation;
        }

        /**
         * Retrieves the latitude of the location (in degrees) to reach.
         *
         * @return the latitude in degrees
         */
        public double getLatitude() {
            return mLatitude;
        }

        /**
         * Retrieves the longitude of the location (in degrees) to reach.
         *
         * @return the longitude in degrees
         */
        public double getLongitude() {
            return mLongitude;
        }

        /**
         * Retrieves the altitude above take off point (in meters) to reach.
         *
         * @return the altitude in meters
         */
        public double getAltitude() {
            return mAltitude;
        }

        /**
         * Retrieves the orientation of the guided flight.
         *
         * @return the orientation
         */
        @NonNull
        public GuidedPilotingItf.LocationDirective.Orientation getOrientation() {
            return mOrientation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            LocationDirective that = (LocationDirective) o;

            return Double.compare(mLatitude, that.mLatitude) == 0
                   && Double.compare(mLongitude, that.mLongitude) == 0
                   && Double.compare(mAltitude, that.mAltitude) == 0
                   && mOrientation.equals(that.mOrientation);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            long temp;
            temp = Double.doubleToLongBits(mLatitude);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(mLongitude);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(mAltitude);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + mOrientation.hashCode();
            return result;
        }
    }

    /**
     * A relative move directive.
     * <p>
     * Allows to instruct the drone to first rotate its heading by a specified angle, then to move a specified distance
     * from the current location.<br>
     * Optionally, desired speed values for the move may also be specified.
     * <p>
     * Note that moves are relative to the current drone orientation (drone's reference), and that the specified
     * rotation will not modify the move (i.e. moves are always rectilinear).
     *
     * @see #move(Directive)
     */
    final class RelativeMoveDirective extends Directive {

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
         * @param speed       requested movement speed or {@code null}
         */
        public RelativeMoveDirective(double forwardComponent, double rightComponent, double downwardComponent,
                        double headingRotation, @Nullable Speed speed) {
            super(GuidedPilotingItf.Type.RELATIVE_MOVE, speed);
            mForwardComponent = forwardComponent;
            mRightComponent = rightComponent;
            mDownwardComponent = downwardComponent;
            mHeadingRotation = headingRotation;
        }

        /**
         * Retrieves the desired displacement along the drone front axis, in meters. A negative value
         * means a backward move.
         *
         * @return the forward component of the move in meters
         */
        public double getForwardComponent() {
            return mForwardComponent;
        }

        /**
         * Retrieves the desired displacement along the drone right axis, in meters. A negative value
         * means a move to the left.
         *
         * @return the right component of the move in meters
         */
        public double getRightComponent() {
            return mRightComponent;
        }

        /**
         * Retrieves the desired displacement along the drone down axis, in meters. A negative value
         * means an upward move.
         *
         * @return the downward component of the move in meters
         */
        public double getDownwardComponent() {
            return mDownwardComponent;
        }

        /**
         * Retrieves the desired relative rotation of heading, in degrees (clockwise). The rotation is performed before
         * the move.
         *
         * @return the heading rotation in degrees (clockwise)
         */
        public double getHeadingRotation() {
            return mHeadingRotation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

           RelativeMoveDirective that = (RelativeMoveDirective) o;

            return Double.compare(that.mForwardComponent, mForwardComponent) == 0
                   && Double.compare(that.mRightComponent, mRightComponent) == 0
                   && Double.compare(that.mDownwardComponent, mDownwardComponent) == 0
                   && Double.compare(that.mHeadingRotation, mHeadingRotation) == 0;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            long temp;
            temp = Double.doubleToLongBits(mForwardComponent);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(mRightComponent);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(mDownwardComponent);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(mHeadingRotation);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

    /** Information about a finished guided flight. */
    interface FinishedFlightInfo {

        /**
         * Retrieves the guided flight type.
         *
         * @return the guided flight type
         */
        @NonNull
        Type getType();

        /**
         * Tells whether the guided flight succeeded.
         *
         * @return {@code true} if the guided flight succeeded, {@code false} otherwise
         */
        boolean wasSuccessful();
    }

    /**
     * Information about a finished location guided flight.
     * <p>
     * Contains the initial directive and the final state of the flight.
     */
    interface FinishedLocationFlightInfo extends FinishedFlightInfo {

        /**
         * Retrieves the initial guided flight directive.
         *
         * @return the guided flight directive
         */
        @NonNull
        LocationDirective getDirective();
    }

    /**
     * Information about a finished relative move guided flight.
     * <p>
     * Contains the initial directive, the move that the drone actually did and the
     * final state of the flight.
     */
    interface FinishedRelativeMoveFlightInfo extends FinishedFlightInfo {

        /**
         * Retrieves the initial guided flight directive.
         *
         * @return the guided flight directive
         */
        @NonNull
        RelativeMoveDirective getDirective();

        /**
         * Retrieves the actual displacement along the drone front axis, in meters. A negative value
         * means a backward move.
         *
         * @return the forward component of the actual move in meters
         */
        double getActualForwardComponent();

        /**
         * Retrieves the actual displacement along the drone right axis, in meters. A negative value
         * means a move to the left.
         *
         * @return the right component of the actual move in meters
         */
        double getActualRightComponent();

        /**
         * Retrieves the actual displacement along the drone down axis, in meters. A negative value
         * means an upward move.
         *
         * @return the downward component of the actual move in meters
         */
        double getActualDownwardComponent();

        /**
         * Retrieves the actual relative rotation of heading, in degrees (clockwise).
         *
         * @return the actual rotation in degrees (clockwise)
         */
        double getActualHeadingRotation();
    }

    /**
     * Tells why this piloting interface may currently be unavailable.
     * <p>
     * The returned set may contain values only if the interface is {@link State#UNAVAILABLE unavailable}; it cannot
     * be modified.
     *
     * @return the set of reasons that restrain this piloting interface from being available at present
     */
    @NonNull
    Set<UnavailabilityReason> getUnavailabilityReasons();

    /**
     * Starts a guided flight.
     * <p>
     * Moves the drone according to the specified movement directive
     * <p>
     * The interface will immediately change to {@link State#ACTIVE}, and then to
     * {@link State#IDLE} when the drone reaches its destination or is stopped. It also becomes
     * {@link State#IDLE} in case of error.
     * <p>
     * If this method is called while the previous guided flight is still in progress, it will be stopped
     * immediately and the new guided flight is started.
     * <p>
     * In case of drone disconnection, the guided flight is interrupted.
     *
     * @param directive movement directive
     *
     * @see LocationDirective
     * @see RelativeMoveDirective
     */
    void move(@NonNull Directive directive);

    /**
     * Retrieves the current guided flight directive.
     * <p>
     * This method returns the current guided flight directive, if any. It can be either
     * {@link LocationDirective LocationDirective} or {@link RelativeMoveDirective RelativeMoveDirective}.
     * The flight parameters have the values returned by the drone.
     *
     * @return the current guided flight directive, or {@code null} if there's no guided flight in progress
     */
    @Nullable
    Directive getCurrentDirective();

    /**
     * Retrieves the latest terminated guided flight information.
     * <p>
     * This method returns information about the latest finished guided flight, if any. It can be either
     * {@link FinishedLocationFlightInfo FinishedLocationFlightInfo} or
     * {@link FinishedRelativeMoveFlightInfo FinishedRelativeMoveFlightInfo}.<br>
     * It contains the initial directive, the final state of the flight and, for a relative move, the
     * move that the drone actually did.
     *
     * @return the latest terminated guided flight information, or {@code null} if no such flight has
     *         been requested
     */
    @Nullable
    FinishedFlightInfo getLatestFinishedFlightInfo();

    /**
     * <strong>DEPRECATED. Use {@link #move(Directive)} instead. </strong>
     * <p>
     * Starts a location guided flight.
     * <p>
     * Move the drone to a specified location, and rotate heading to the specified value.
     * <p>
     * The interface will immediately change to {@link State#ACTIVE}, and then to
     * {@link State#IDLE} when the drone reaches its destination or is stopped. It also becomes
     * {@link State#IDLE} in case of error.
     * <p>
     * If this method is called while the previous guided flight is still in progress, it will be stopped
     * immediately and the new guided flight is started.
     * <p>
     * In case of drone disconnection, the guided flight is interrupted.
     *
     * @param latitude        latitude of the location (in degrees) to reach
     * @param longitude       longitude of the location (in degrees) to reach
     * @param altitude        altitude above take off point (in meters) to reach
     * @param orientation     orientation of the location guided flight
     */
    @Deprecated
    default void moveToLocation(double latitude, double longitude, double altitude,
                                @NonNull LocationDirective.Orientation orientation) {
        move(new LocationDirective(latitude, longitude, altitude, orientation, null));
    }

    /**
     * <strong>DEPRECATED. Use {@link #move(Directive)} instead. </strong>
     * <p>
     * Starts a relative move guided flight.
     * <p>
     * Rotate heading by a given angle, and then move the drone to a relative position.<br>
     * Moves are relative to the current drone orientation (drone's reference).<br>
     * Also note that the given rotation will not modify the move (i.e. moves are always rectilinear).
     * <p>
     * The interface will immediately change to {@link State#ACTIVE}, and then to {@link State#IDLE}
     * when the drone reaches its destination or is stopped. It also becomes {@link State#IDLE} in
     * case of error.
     * <p>
     * If this method is called while the previous guided flight is still in progress, it will be stopped
     * immediately and the new guided flight is started.
     * <p>
     * In case of drone disconnection, the guided flight is interrupted.
     *
     * @param forwardComponent  desired displacement along the front axis, in meters. Use a negative
     *                          value for a backward move
     * @param rightComponent    desired displacement along the right axis, in meters. Use a negative
     *                          value for a left move
     * @param downwardComponent desired displacement along the down axis, in meters. Use a negative
     *                          value for an upward move
     * @param headingRotation   desired relative rotation of heading, in degrees (clockwise)
     */
    @Deprecated
    default void moveToRelativePosition(double forwardComponent, double rightComponent, double downwardComponent,
                                        double headingRotation) {
        move(new RelativeMoveDirective(forwardComponent, rightComponent, downwardComponent, headingRotation, null));
    }
}
