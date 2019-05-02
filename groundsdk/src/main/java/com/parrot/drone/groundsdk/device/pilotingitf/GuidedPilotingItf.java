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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.LocationDirective.Orientation;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.GuidedPilotingItfCore.LocationDirectiveCore.OrientationCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.GuidedPilotingItfCore.LocationDirectiveCore.OrientationCore.Mode;

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

    /** A guided flight directive. */
    interface Directive {

        /**
         * Retrieves the guided flight type.
         *
         * @return the guided flight type
         */
        @NonNull
        Type getType();
    }

    /** A location directive. */
    interface LocationDirective extends Directive {

        /**
         * Orientation of a location directive.
         */
        abstract class Orientation {

            /**
             * Orientation for which the drone won't change its heading.
             */
            @SuppressWarnings("StaticInitializerReferencesSubClass")
            @NonNull
            public static final Orientation NONE = new OrientationCore(Mode.NONE, 0);

            /**
             * Orientation for which the drone will make a rotation to look in direction of the given location before
             * moving to the location.
             */
            @SuppressWarnings("StaticInitializerReferencesSubClass")
            @NonNull
            public static final Orientation TO_TARGET = new OrientationCore(Mode.TO_TARGET, 0);

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
                return new OrientationCore(Mode.START, heading);
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
                return new OrientationCore(Mode.DURING, heading);
            }

            /**
             * Retrieves the orientation mode.
             *
             * @return the orientation mode
             */
            @NonNull
            public abstract Mode getMode();

            /**
             * Retrieves the heading (relative to the North in degrees, clockwise). This value is meaningful for
             * {@code START} or {@code DURING} modes only.
             *
             * @return the heading in degrees
             */
            public abstract double getHeading();

            /**
             * Constructor.
             * <p>
             * Application <strong>MUST NOT</strong> override this class.
             */
            protected Orientation() {
            }
        }

        /**
         * Retrieves the latitude of the location (in degrees) to reach.
         *
         * @return the latitude in degrees
         */
        double getLatitude();

        /**
         * Retrieves the longitude of the location (in degrees) to reach.
         *
         * @return the longitude in degrees
         */
        double getLongitude();

        /**
         * Retrieves the altitude above take off point (in meters) to reach.
         *
         * @return the altitude in meters
         */
        double getAltitude();

        /**
         * Retrieves the orientation of the guided flight.
         *
         * @return the orientation
         */
        @NonNull
        Orientation getOrientation();
    }

    /** A relative move directive. */
    interface RelativeMoveDirective extends Directive {

        /**
         * Retrieves the desired displacement along the drone front axis, in meters. A negative value
         * means a backward move.
         *
         * @return the forward component of the move in meters
         */
        double getForwardComponent();

        /**
         * Retrieves the desired displacement along the drone right axis, in meters. A negative value
         * means a move to the left.
         *
         * @return the right component of the move in meters
         */
        double getRightComponent();

        /**
         * Retrieves the desired displacement along the drone down axis, in meters. A negative value
         * means an upward move.
         *
         * @return the downward component of the move in meters
         */
        double getDownwardComponent();

        /**
         * Retrieves the desired relative rotation of heading, in degrees (clockwise). The rotation is performed before
         * the move.
         *
         * @return the heading rotation in degrees (clockwise)
         */
        double getHeadingRotation();
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
     * @param latitude    latitude of the location (in degrees) to reach
     * @param longitude   longitude of the location (in degrees) to reach
     * @param altitude    altitude above take off point (in meters) to reach
     * @param orientation orientation of the location guided flight
     */
    void moveToLocation(double latitude, double longitude, double altitude,
                        @NonNull Orientation orientation);

    /**
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
    void moveToRelativePosition(double forwardComponent, double rightComponent, double downwardComponent,
                                double headingRotation);

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
}
