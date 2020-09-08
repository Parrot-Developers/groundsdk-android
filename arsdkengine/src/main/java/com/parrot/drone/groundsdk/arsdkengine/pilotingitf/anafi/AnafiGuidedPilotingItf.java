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

package com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.PilotingItfActivationController;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.ActivablePilotingItfController;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.ActivablePilotingItfCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.guided.GuidedPilotingItfCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.guided.FinishedFlightInfoCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.guided.FinishedFlightInfoCore.Relative;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureMove;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Collectors;

/** Guided piloting interface controller for Anafi family drones. */
public class AnafiGuidedPilotingItf extends ActivablePilotingItfController {

    /** Piloting interface for which this object is the backend. */
    @NonNull
    private final GuidedPilotingItfCore mPilotingItf;

    /** Whether guided flight is started. */
    private boolean mGuidedFlightOngoing;

    /** {@code true} if the drone is flying. */
    private boolean mDroneFlying;

    /** Current guided flight directive. */
    @Nullable
    private GuidedPilotingItf.Directive mCurrentDirective;

    /** Previous guided flight directive, used to properly manage relative move interruption. */
    @Nullable
    private GuidedPilotingItf.Directive mPreviousDirective;

    /** Latest terminated guided flight information. */
    @Nullable
    private GuidedPilotingItf.FinishedFlightInfo mLatestFinishedFlightInfo;

    /**
     * Constructor.
     *
     * @param activationController activation controller that owns this piloting interface controller
     */
    public AnafiGuidedPilotingItf(@NonNull PilotingItfActivationController activationController) {
        super(activationController, false);
        mPilotingItf = new GuidedPilotingItfCore(mComponentStore, new Backend());
    }

    @NonNull
    @Override
    public ActivablePilotingItfCore getPilotingItf() {
        return mPilotingItf;
    }

    @Override
    public void onConnected() {
        super.onConnected();
        mPilotingItf.publish();
    }

    @Override
    public void onDisconnected() {
        mGuidedFlightOngoing = false;
        mCurrentDirective = null;
        mPreviousDirective = null;
        mPilotingItf.updateCurrentDirective(null);
        mPilotingItf.updateUnavailabilityReasons(EnumSet.noneOf(GuidedPilotingItf.UnavailabilityReason.class));
        mPilotingItf.unpublish();
        updateFlyingState(false);
        super.onDisconnected();
    }

    @Override
    protected void onForgetting() {
        mPilotingItf.unpublish();
    }

    @Override
    public boolean canActivate() {
        return super.canActivate() && isGuidedPilotingAvailable();
    }

    @Override
    public void requestActivation() {
        super.requestActivation();
        if (mCurrentDirective != null) {
            sendDirective(mCurrentDirective);
            mPilotingItf.updateCurrentDirective(mCurrentDirective);
            mGuidedFlightOngoing = true;
        }
        updateState(); // will call notifyUpdated()
    }

    @Override
    public void requestDeactivation() {
        super.requestDeactivation();
        if (mCurrentDirective == null) {
            notifyIdle();
        } else switch (mCurrentDirective.getType()) {
            case ABSOLUTE_LOCATION:
                sendCommand(ArsdkFeatureArdrone3.Piloting.encodeCancelMoveTo());
                break;
            case RELATIVE_MOVE:
                sendCommand(ArsdkFeatureArdrone3.Piloting.encodeMoveBy(0, 0, 0, 0));
                break;
        }
    }

    /**
     * Tells whether guided piloting is available.
     * <p>
     * Guided piloting is available when the drone is flying and no unavailability reason is present.
     *
     * @return {@code true} if guided piloting is available, {@code false} otherwise
     */
    private boolean isGuidedPilotingAvailable() {
        return mDroneFlying && mPilotingItf.getUnavailabilityReasons().isEmpty();
    }

    /**
     * Updates the piloting interface state.
     * <p>
     * If the drone is not flying, or at least one unavailability reason is present, the interface state is set to
     * {@code UNAVAILABLE}.<br> Otherwise, the interface state is set to {@code ACTIVE} if guided piloting is started
     * or to {@code IDLE} if guided piloting is not started.
     */
    private void updateState() {
        if (!isGuidedPilotingAvailable()) {
            notifyUnavailable();
        } else if (mGuidedFlightOngoing) {
            notifyActive();
        } else {
            notifyIdle();
        }
    }

    /**
     * Updates the drone flying state.
     *
     * @param flying {@code true} if the drone is flying, {@code false} otherwise
     */
    private void updateFlyingState(boolean flying) {
        if (mDroneFlying != flying) {
            mDroneFlying = flying;
            updateState();
        }
    }

    /**
     * Called when a location move is finished.
     *
     * @param success {@code true} if the move was successful, {@code false} otherwise
     */
    private void onLocationMoveFinished(boolean success) {
        if (mCurrentDirective != null && mCurrentDirective.getType() == GuidedPilotingItf.Type.ABSOLUTE_LOCATION) {
            mLatestFinishedFlightInfo = new FinishedFlightInfoCore.Location(
                    (GuidedPilotingItf.LocationDirective) mCurrentDirective, success);
            mCurrentDirective = null;
            mPilotingItf.updateCurrentDirective(null);
            mPilotingItf.updateLatestFinishedFlightInfo(mLatestFinishedFlightInfo);
        }
    }

    /**
     * Called when a relative move is finished.
     *
     * @param success {@code true} if the move was successful, {@code false} otherwise
     * @param dx      desired displacement along the front axis, in meters
     * @param dy      desired displacement along the right axis, in meters
     * @param dz      desired displacement along the down axis, in meters
     * @param dpsi    desired relative rotation of heading, in degrees
     */
    private void onRelativeMoveFinished(boolean success, float dx, float dy, float dz, float dpsi) {
        if (mCurrentDirective != null && mCurrentDirective.getType() == GuidedPilotingItf.Type.RELATIVE_MOVE) {
            mLatestFinishedFlightInfo = new Relative(
                    (GuidedPilotingItf.RelativeMoveDirective) mCurrentDirective, success, dx, dy, dz, dpsi);
            mCurrentDirective = null;
            mPilotingItf.updateCurrentDirective(null);
            mPilotingItf.updateLatestFinishedFlightInfo(mLatestFinishedFlightInfo);
        }
    }

    /**
     * Called when a relative move has been interrupted.
     * <p>
     * If a relative move was started before the previous one ended, the interrupted move is the previous one. If a
     * relative move is interrupted because it has been manually stopped or any other reason, the interrupted move is
     * the current one.
     *
     * @param actualDx   forward component of the actual move
     * @param actualDy   right component of the actual move
     * @param actualDz   downward component of the actual move
     * @param actualDpsi heading rotation component of the actual move
     */
    private void onRelativeMoveInterrupted(double actualDx, double actualDy, double actualDz, double actualDpsi) {
        if (mPreviousDirective != null && mPreviousDirective.getType() == GuidedPilotingItf.Type.RELATIVE_MOVE) {
            mLatestFinishedFlightInfo = new Relative(
                    (GuidedPilotingItf.RelativeMoveDirective) mPreviousDirective, false, actualDx, actualDy, actualDz, actualDpsi);
            mPreviousDirective = null;
            mPilotingItf.updateLatestFinishedFlightInfo(mLatestFinishedFlightInfo);
        } else if (mCurrentDirective != null && mCurrentDirective.getType() == GuidedPilotingItf.Type.RELATIVE_MOVE) {
            mLatestFinishedFlightInfo = new Relative(
                    (GuidedPilotingItf.RelativeMoveDirective) mCurrentDirective, false, actualDx, actualDy, actualDz, actualDpsi);
            mCurrentDirective = null;
            mPilotingItf.updateCurrentDirective(null);
            mPilotingItf.updateLatestFinishedFlightInfo(mLatestFinishedFlightInfo);
        }
    }

    /**
     * Converts a groundsdk {@link GuidedPilotingItf.LocationDirective.Orientation orientation mode} into its arsdk
     * {@link ArsdkFeatureArdrone3.PilotingMovetoOrientationMode representation}.
     *
     * @param orientation groundsdk orientation to convert
     *
     * @return arsdk representation of the specified orientation
     */
    @NonNull
    private static ArsdkFeatureArdrone3.PilotingMovetoOrientationMode toArsdkPilotingMoveToOrientationMode(
            @NonNull GuidedPilotingItf.LocationDirective.Orientation orientation) {
        ArsdkFeatureArdrone3.PilotingMovetoOrientationMode mode = null;
        switch (orientation.getMode()) {
            case NONE:
                mode = ArsdkFeatureArdrone3.PilotingMovetoOrientationMode.NONE;
                break;
            case TO_TARGET:
                mode = ArsdkFeatureArdrone3.PilotingMovetoOrientationMode.TO_TARGET;
                break;
            case START:
                mode = ArsdkFeatureArdrone3.PilotingMovetoOrientationMode.HEADING_START;
                break;
            case DURING:
                mode = ArsdkFeatureArdrone3.PilotingMovetoOrientationMode.HEADING_DURING;
                break;
        }
        return mode;
    }

    /**
     * Converts a groundsdk {@link GuidedPilotingItf.LocationDirective.Orientation orientation mode} into its arsdk
     * {@link ArsdkFeatureMove.OrientationMode representation}.
     *
     * @param orientation groundsdk orientation to convert
     *
     * @return arsdk representation of the specified orientation
     */
    @NonNull
    private static ArsdkFeatureMove.OrientationMode toArsdkFeatureMoveOrientationMode(
            @NonNull GuidedPilotingItf.LocationDirective.Orientation orientation) {
        ArsdkFeatureMove.OrientationMode mode = null;
        switch (orientation.getMode()) {
            case NONE:
                mode = ArsdkFeatureMove.OrientationMode.NONE;
                break;
            case TO_TARGET:
                mode = ArsdkFeatureMove.OrientationMode.TO_TARGET;
                break;
            case START:
                mode = ArsdkFeatureMove.OrientationMode.HEADING_START;
                break;
            case DURING:
                mode = ArsdkFeatureMove.OrientationMode.HEADING_DURING;
                break;
        }
        return mode;
    }

    /**
     * Converts an arsdk {@link ArsdkFeatureMove.Indicator indicator} into its groundsdk
     * {@link GuidedPilotingItf.UnavailabilityReason representation}.
     *
     * @param indicator arsdk indicator to convert
     *
     * @return groundsdk representation of the specified indicator
     */
    @Nullable
    private static GuidedPilotingItf.UnavailabilityReason convert(@NonNull ArsdkFeatureMove.Indicator indicator) {
        switch (indicator) {
            case DRONE_GPS:
                return GuidedPilotingItf.UnavailabilityReason.DRONE_GPS_INFO_INACCURATE;
            case DRONE_MAGNETO:
                return GuidedPilotingItf.UnavailabilityReason.DRONE_NOT_CALIBRATED;
            case DRONE_FLYING:
                return GuidedPilotingItf.UnavailabilityReason.DRONE_NOT_FLYING;
            case DRONE_GEOFENCE:
                return GuidedPilotingItf.UnavailabilityReason.DRONE_OUT_GEOFENCE;
            case DRONE_MIN_ALTITUDE:
                return GuidedPilotingItf.UnavailabilityReason.DRONE_TOO_CLOSE_TO_GROUND;
            case DRONE_MAX_ALTITUDE:
                return GuidedPilotingItf.UnavailabilityReason.DRONE_ABOVE_MAX_ALTITUDE;
            case TARGET_POSITION_ACCURACY:
            case TARGET_IMAGE_DETECTION:
            case DRONE_TARGET_DISTANCE_MIN:
            case DRONE_TARGET_DISTANCE_MAX:
            case TARGET_HORIZ_SPEED:
            case TARGET_VERT_SPEED:
            case TARGET_ALTITUDE_ACCURACY:
                return null;
        }
        return null;
    }

    @Override
    protected final void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        switch (featureId) {
            case ArsdkFeatureArdrone3.PilotingState.UID:
                ArsdkFeatureArdrone3.PilotingState.decode(command, mPilotingStateCallback);
                break;
            case ArsdkFeatureArdrone3.PilotingEvent.UID:
                ArsdkFeatureArdrone3.PilotingEvent.decode(command, mPilotingEventCallback);
                break;
            case ArsdkFeatureMove.UID:
                ArsdkFeatureMove.decode(command, mMoveCallback);
                break;
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.PilotingState is decoded. */
    private final ArsdkFeatureArdrone3.PilotingState.Callback mPilotingStateCallback =
            new ArsdkFeatureArdrone3.PilotingState.Callback() {

                /** Special value sent by the drone when latitude, longitude or altitude is not known. */
                private static final double UNKNOWN_COORDINATE = 500;

                @Override
                public void onFlyingStateChanged(
                        @Nullable ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState state) {
                    if (state != null) switch (state) {
                        case LANDED:
                        case TAKINGOFF:
                        case LANDING:
                        case EMERGENCY:
                            updateFlyingState(false);
                            break;
                        case HOVERING:
                        case FLYING:
                            updateFlyingState(true);
                            break;
                        case USERTAKEOFF:
                        case MOTOR_RAMPING:
                        case EMERGENCY_LANDING:
                            break;
                    }
                }

                @Override
                public void onMoveToChanged(
                        double latitude, double longitude, double altitude,
                        @Nullable ArsdkFeatureArdrone3.PilotingstateMovetochangedOrientationMode orientationMode,
                        float heading, @Nullable ArsdkFeatureArdrone3.PilotingstateMovetochangedStatus status) {
                    if (Double.compare(latitude, UNKNOWN_COORDINATE) != 0
                        && Double.compare(longitude, UNKNOWN_COORDINATE) != 0
                        && Double.compare(altitude, UNKNOWN_COORDINATE) != 0
                        && orientationMode != null) {
                        GuidedPilotingItf.LocationDirective.Orientation orientation = null;
                        switch (orientationMode) {
                            case NONE:
                                orientation = GuidedPilotingItf.LocationDirective.Orientation.NONE;
                                break;
                            case TO_TARGET:
                                orientation = GuidedPilotingItf.LocationDirective.Orientation.TO_TARGET;
                                break;
                            case HEADING_START:
                                orientation = GuidedPilotingItf.LocationDirective.Orientation.headingStart(heading);
                                break;
                            case HEADING_DURING:
                                orientation = GuidedPilotingItf.LocationDirective.Orientation.headingDuring(heading);
                                break;
                        }
                        mCurrentDirective = new GuidedPilotingItf.LocationDirective(latitude, longitude, altitude,
                                orientation, mCurrentDirective == null ? null : mCurrentDirective.getSpeed());
                        mPilotingItf.updateCurrentDirective(mCurrentDirective);
                    }

                    if (status != null) switch (status) {
                        case RUNNING:
                            mGuidedFlightOngoing = true;
                            break;
                        case DONE:
                            onLocationMoveFinished(true);
                            mGuidedFlightOngoing = false;
                            break;
                        case CANCELED:
                        case ERROR:
                            onLocationMoveFinished(false);
                            mGuidedFlightOngoing = false;
                            break;
                    }
                    updateState(); // will call notifyUpdated()
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.PilotingEvent is decoded. */
    private final ArsdkFeatureArdrone3.PilotingEvent.Callback mPilotingEventCallback =
            new ArsdkFeatureArdrone3.PilotingEvent.Callback() {

                @Override
                public void onMoveByEnd(float dx, float dy, float dz, float dpsi,
                                        @Nullable ArsdkFeatureArdrone3.PilotingeventMovebyendError error) {
                    if (error != null) switch (error) {
                        case OK:
                            onRelativeMoveFinished(true, dx, dy, dz, dpsi);
                            mGuidedFlightOngoing = false;
                            break;
                        case UNKNOWN:
                        case BUSY:
                        case NOTAVAILABLE:
                            onRelativeMoveFinished(false, dx, dy, dz, dpsi);
                            mGuidedFlightOngoing = false;
                            break;
                        case INTERRUPTED:
                            onRelativeMoveInterrupted(dx, dy, dz, dpsi);
                            break;
                    }
                    updateState(); // will call notifyUpdated()
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureMove is decoded. */
    private final ArsdkFeatureMove.Callback mMoveCallback =
            new ArsdkFeatureMove.Callback() {

                @Override
                public void onInfo(int missingInputsBitField) {
                    EnumSet<ArsdkFeatureMove.Indicator> missingInputs =
                            ArsdkFeatureMove.Indicator.fromBitfield(missingInputsBitField);

                    EnumSet<GuidedPilotingItf.UnavailabilityReason> reasons = missingInputs
                            .stream()
                            .map(AnafiGuidedPilotingItf::convert)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toCollection(() -> EnumSet.noneOf(
                                    GuidedPilotingItf.UnavailabilityReason.class)));
                    mPilotingItf.updateUnavailabilityReasons(reasons);
                    updateState(); // will call notifyUpdated()
                }
            };

    /** Backend of GuidedPilotingItfCore implementation. */
    private final class Backend extends ActivablePilotingItfController.Backend
            implements GuidedPilotingItfCore.Backend {

        @Override
        public void move(@NonNull GuidedPilotingItf.Directive directive) {
            mPreviousDirective = mCurrentDirective;
            mCurrentDirective = directive;
            switch (mPilotingItf.getState()) {
                case IDLE:
                    activate();
                    break;
                case ACTIVE:
                    sendDirective(mCurrentDirective);
                    mPilotingItf.updateCurrentDirective(mCurrentDirective).notifyUpdated();
                    break;
                case UNAVAILABLE:
                    break;
            }
        }
    }

    /**
     * Sends the given movement directive to the drone.
     *
     * @param directive directive to send
     */
    private void sendDirective(@NonNull GuidedPilotingItf.Directive directive) {
        GuidedPilotingItf.Directive.Speed speed = directive.getSpeed();
        switch (directive.getType()) {
            case ABSOLUTE_LOCATION:
                GuidedPilotingItf.LocationDirective locDir = (GuidedPilotingItf.LocationDirective) directive;
                if (speed == null) {
                    sendCommand(ArsdkFeatureArdrone3.Piloting.encodeMoveTo(locDir.getLatitude(), locDir.getLongitude(),
                            locDir.getAltitude(), toArsdkPilotingMoveToOrientationMode(locDir.getOrientation()),
                            (float) locDir.getOrientation().getHeading()));
                } else {
                    sendCommand(ArsdkFeatureMove.encodeExtendedMoveTo(locDir.getLatitude(), locDir.getLongitude(),
                            locDir.getAltitude(), toArsdkFeatureMoveOrientationMode(locDir.getOrientation()),
                            (float) locDir.getOrientation().getHeading(), (float) speed.getHorizontalMax(),
                            (float) speed.getVerticalMax(), (float) speed.getRotationMax()));
                }
                break;
            case RELATIVE_MOVE:
                GuidedPilotingItf.RelativeMoveDirective relDir = (GuidedPilotingItf.RelativeMoveDirective) directive;
                if (speed == null) {
                    sendCommand(ArsdkFeatureArdrone3.Piloting.encodeMoveBy((float) relDir.getForwardComponent(),
                            (float) relDir.getRightComponent(), (float) relDir.getDownwardComponent(),
                            (float) Math.toRadians(relDir.getHeadingRotation())));
                } else {
                    sendCommand(ArsdkFeatureMove.encodeExtendedMoveBy((float) relDir.getForwardComponent(),
                            (float) relDir.getRightComponent(), (float) relDir.getDownwardComponent(),
                            (float) Math.toRadians(relDir.getHeadingRotation()), (float) speed.getHorizontalMax(),
                            (float) speed.getVerticalMax(), (float) speed.getRotationMax()));
                }
                break;
        }
    }
}
