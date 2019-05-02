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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.PilotingItfActivationController;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.ActivablePilotingItfController;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.Directive;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.FinishedFlightInfo;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.LocationDirective.Orientation;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.RelativeMoveDirective;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.ActivablePilotingItfCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.GuidedPilotingItfCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.GuidedPilotingItfCore.FinishedLocationFlightInfoCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.GuidedPilotingItfCore.FinishedRelativeMoveFlightInfoCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.GuidedPilotingItfCore.LocationDirectiveCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.GuidedPilotingItfCore.RelativeMoveDirectiveCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3.Piloting;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3.PilotingMovetoOrientationMode;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3.PilotingeventMovebyendError;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3.PilotingstateMovetochangedOrientationMode;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3.PilotingstateMovetochangedStatus;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import static com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.Type.ABSOLUTE_LOCATION;
import static com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.Type.RELATIVE_MOVE;

/** Guided piloting interface controller for Anafi family drones. */
public class AnafiGuidedPilotingItf extends ActivablePilotingItfController {

    /** Piloting interface for which this object is the backend. */
    @NonNull
    private final GuidedPilotingItfCore mPilotingItf;

    /** {@code true} if the drone is flying. */
    private boolean mDroneFlying;

    /** Current guided flight directive. */
    @Nullable
    private Directive mCurrentDirective;

    /** Previous guided flight directive, used to properly manage relative move interruption. */
    @Nullable
    private Directive mPreviousDirective;

    /** Latest terminated guided flight information. */
    @Nullable
    private FinishedFlightInfo mLatestFinishedFlightInfo;

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
        mCurrentDirective = null;
        mPreviousDirective = null;
        mPilotingItf.updateCurrentDirective(null);
        mPilotingItf.unpublish();
        updateFlyingState(false);
        super.onDisconnected();
    }

    @Override
    protected void onForgetting() {
        mPilotingItf.unpublish();
    }

    @Override
    public void requestActivation() {
        super.requestActivation();
        if (mCurrentDirective != null) {
            switch (mCurrentDirective.getType()) {
                case ABSOLUTE_LOCATION:
                    LocationDirectiveCore locDir = (LocationDirectiveCore) mCurrentDirective;
                    sendCommand(Piloting.encodeMoveTo(locDir.getLatitude(), locDir.getLongitude(), locDir.getAltitude(),
                            getArsdkOrientationMode(locDir.getOrientation()),
                            (float) locDir.getOrientation().getHeading()));
                    break;
                case RELATIVE_MOVE:
                    RelativeMoveDirectiveCore relDir = (RelativeMoveDirectiveCore) mCurrentDirective;
                    sendCommand(Piloting.encodeMoveBy((float) relDir.getForwardComponent(),
                            (float) relDir.getRightComponent(),
                            (float) relDir.getDownwardComponent(),
                            (float) Math.toRadians(relDir.getHeadingRotation())));
                    break;
            }
            mPilotingItf.updateCurrentDirective(mCurrentDirective);
            notifyActive(); // calls mPilotingItf.notifyUpdated()
        }
    }

    @Override
    public void requestDeactivation() {
        super.requestDeactivation();
        if (mCurrentDirective == null) {
            notifyIdle();
        } else switch (mCurrentDirective.getType()) {
            case ABSOLUTE_LOCATION:
                sendCommand(Piloting.encodeCancelMoveTo());
                break;
            case RELATIVE_MOVE:
                sendCommand(Piloting.encodeMoveBy(0, 0, 0, 0));
                break;
        }
    }

    /**
     * Converts GroundSdk {@link LocationDirectiveCore.OrientationCore.Mode} to SdkCore {@link
     * PilotingMovetoOrientationMode}.
     *
     * @param orientation the orientation containing the mode to convert
     *
     * @return the converted {@code PilotingMovetoOrientationMode}
     */
    @NonNull
    private static PilotingMovetoOrientationMode getArsdkOrientationMode(@NonNull Orientation orientation) {
        PilotingMovetoOrientationMode mode = null;
        switch (orientation.getMode()) {
            case NONE:
                mode = PilotingMovetoOrientationMode.NONE;
                break;
            case TO_TARGET:
                mode = PilotingMovetoOrientationMode.TO_TARGET;
                break;
            case START:
                mode = PilotingMovetoOrientationMode.HEADING_START;
                break;
            case DURING:
                mode = PilotingMovetoOrientationMode.HEADING_DURING;
                break;
        }
        return mode;
    }

    /**
     * Updates the drone flying state.
     *
     * @param flying {@code true} if the drone is flying, {@code false} otherwise
     */
    private void updateFlyingState(boolean flying) {
        if (mDroneFlying != flying) {
            mDroneFlying = flying;
            if (flying) {
                notifyIdle();
            } else {
                notifyUnavailable();
            }
        }
    }

    /**
     * Called when a location move is finished.
     *
     * @param success {@code true} if the move was successful, {@code false} otherwise
     */
    private void onLocationMoveFinished(boolean success) {
        if (mCurrentDirective != null && mCurrentDirective.getType() == ABSOLUTE_LOCATION) {
            mLatestFinishedFlightInfo =
                    new FinishedLocationFlightInfoCore((LocationDirectiveCore) mCurrentDirective, success);
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
        if (mCurrentDirective != null && mCurrentDirective.getType() == RELATIVE_MOVE) {
            mLatestFinishedFlightInfo = new FinishedRelativeMoveFlightInfoCore(
                    (RelativeMoveDirective) mCurrentDirective, success, dx, dy, dz, dpsi);
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
        if (mPreviousDirective != null && mPreviousDirective.getType() == RELATIVE_MOVE) {
            mLatestFinishedFlightInfo = new FinishedRelativeMoveFlightInfoCore(
                    (RelativeMoveDirective) mPreviousDirective, false, actualDx, actualDy, actualDz, actualDpsi);
            mPreviousDirective = null;
            mPilotingItf.updateLatestFinishedFlightInfo(mLatestFinishedFlightInfo);
        } else if (mCurrentDirective != null && mCurrentDirective.getType() == RELATIVE_MOVE) {
            mLatestFinishedFlightInfo = new FinishedRelativeMoveFlightInfoCore(
                    (RelativeMoveDirective) mCurrentDirective, false, actualDx, actualDy, actualDz, actualDpsi);
            mCurrentDirective = null;
            mPilotingItf.updateCurrentDirective(null);
            mPilotingItf.updateLatestFinishedFlightInfo(mLatestFinishedFlightInfo);
        }
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
                public void onMoveToChanged(double latitude, double longitude, double altitude,
                                            @Nullable PilotingstateMovetochangedOrientationMode orientationMode,
                                            float heading, @Nullable PilotingstateMovetochangedStatus status) {
                    if (Double.compare(latitude, UNKNOWN_COORDINATE) != 0
                        && Double.compare(longitude, UNKNOWN_COORDINATE) != 0
                        && Double.compare(altitude, UNKNOWN_COORDINATE) != 0
                        && orientationMode != null) {
                        Orientation orientation = null;
                        switch (orientationMode) {
                            case NONE:
                                orientation = Orientation.NONE;
                                break;
                            case TO_TARGET:
                                orientation = Orientation.TO_TARGET;
                                break;
                            case HEADING_START:
                                orientation = Orientation.headingStart(heading);
                                break;
                            case HEADING_DURING:
                                orientation = Orientation.headingDuring(heading);
                                break;
                        }
                        mCurrentDirective = new LocationDirectiveCore(latitude, longitude, altitude, orientation);
                        mPilotingItf.updateCurrentDirective(mCurrentDirective);
                    }

                    if (status != null) switch (status) {
                        case RUNNING:
                            notifyActive();
                            break;
                        case DONE:
                            onLocationMoveFinished(true);
                            // We need to check if drone is flying because onMoveToChanged() is called on connection
                            if (mDroneFlying) {
                                notifyIdle();
                            }
                            break;
                        case CANCELED:
                        case ERROR:
                            onLocationMoveFinished(false);
                            // We need to check if drone is flying because onMoveToChanged() is called on connection
                            if (mDroneFlying) {
                                notifyIdle();
                            }
                            break;
                    }
                    mPilotingItf.notifyUpdated();
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.PilotingEvent is decoded. */
    private final ArsdkFeatureArdrone3.PilotingEvent.Callback mPilotingEventCallback =
            new ArsdkFeatureArdrone3.PilotingEvent.Callback() {

                @Override
                public void onMoveByEnd(float dx, float dy, float dz, float dpsi,
                                        @Nullable PilotingeventMovebyendError error) {
                    if (error != null) switch (error) {
                        case OK:
                            onRelativeMoveFinished(true, dx, dy, dz, dpsi);
                            notifyIdle(); // calls mPilotingItf.notifyUpdated()
                            break;
                        case UNKNOWN:
                        case BUSY:
                        case NOTAVAILABLE:
                            onRelativeMoveFinished(false, dx, dy, dz, dpsi);
                            notifyIdle(); // calls mPilotingItf.notifyUpdated()
                            break;
                        case INTERRUPTED:
                            onRelativeMoveInterrupted(dx, dy, dz, dpsi);
                            mPilotingItf.notifyUpdated();
                            break;
                    }
                }
            };

    /** Backend of GuidedPilotingItfCore implementation. */
    private final class Backend extends ActivablePilotingItfController.Backend
            implements GuidedPilotingItfCore.Backend {

        @Override
        public void moveToLocation(double latitude, double longitude, double altitude,
                                   @NonNull Orientation orientation) {
            mCurrentDirective = new LocationDirectiveCore(latitude, longitude, altitude, orientation);
            switch (mPilotingItf.getState()) {
                case IDLE:
                    activate();
                    break;
                case ACTIVE:
                    sendCommand(Piloting.encodeMoveTo(latitude, longitude, altitude,
                            getArsdkOrientationMode(orientation), (float) orientation.getHeading()));
                    mPilotingItf.updateCurrentDirective(mCurrentDirective).notifyUpdated();
                    break;
                case UNAVAILABLE:
                    break;
            }
        }

        @Override
        public void moveToRelativePosition(double dx, double dy, double dz, double dpsi) {
            mPreviousDirective = mCurrentDirective;
            mCurrentDirective = new RelativeMoveDirectiveCore(dx, dy, dz, dpsi);
            switch (mPilotingItf.getState()) {
                case IDLE:
                    activate();
                    break;
                case ACTIVE:
                    sendCommand(Piloting.encodeMoveBy((float) dx, (float) dy, (float) dz,
                            (float) Math.toRadians(dpsi)));
                    mPilotingItf.updateCurrentDirective(mCurrentDirective).notifyUpdated();
                    break;
                case UNAVAILABLE:
                    break;
            }
        }
    }
}
