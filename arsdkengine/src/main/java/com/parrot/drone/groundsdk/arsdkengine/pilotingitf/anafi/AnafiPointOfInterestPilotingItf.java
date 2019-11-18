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
import com.parrot.drone.groundsdk.device.pilotingitf.PointOfInterestPilotingItf;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.ActivablePilotingItfCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.PointOfInterestPilotingItfCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.PointOfInterestPilotingItfCore.PointOfInterestCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3.Piloting;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.ulog.ULog;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG;

/** PointOfInterest piloting interface controller for Anafi family drones. */
public class AnafiPointOfInterestPilotingItf extends ActivablePilotingItfController {

    /** Piloting interface for which this object is the backend. */
    @NonNull
    private final PointOfInterestPilotingItfCore mPilotingItf;

    /** {@code true} if the drone is flying. */
    private boolean mDroneFlying;

    /** Tells if piloted Point Of Interest is available. */
    private boolean mAvailable;

    /** Current Point Of Interest. */
    @Nullable
    private PointOfInterestCore mCurrentPointOfInterest;

    /**
     * Pending request of Point Of Interest.
     * <p>
     * When {@link PointOfInterestPilotingItfCore.Backend#start} is called, if the piloting interface is not yet {@code
     * ACTIVE}, the requested Point Of Interest is saved in {@code mPendingPointOfInterestRequest} and the piloting
     * interface request its activation. Once the piloting interface is activated, the request to start the piloted
     * Point Of Interest is sent to the drone.
     */
    @Nullable
    private PointOfInterestCore mPendingPointOfInterestRequest;

    /** {@code true} if PilotedPOIV2 command is supported by the drone. */
    private boolean mPoiV2Supported;

    /**
     * Constructor.
     *
     * @param activationController activation controller that owns this piloting interface controller
     */
    public AnafiPointOfInterestPilotingItf(@NonNull PilotingItfActivationController activationController) {
        super(activationController, true);
        mPilotingItf = new PointOfInterestPilotingItfCore(mComponentStore, new Backend());
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
        mPilotingItf.unpublish();
        mCurrentPointOfInterest = null;
        mPendingPointOfInterestRequest = null;
        mAvailable = false;
        mDroneFlying = false;
        super.onDisconnected();
    }

    @Override
    public void requestActivation() {
        super.requestActivation();
        if (mPendingPointOfInterestRequest != null) {
            sendPointOfInterestCommand(mPendingPointOfInterestRequest.getLatitude(),
                    mPendingPointOfInterestRequest.getLongitude(), mPendingPointOfInterestRequest.getAltitude(),
                    mPendingPointOfInterestRequest.getMode());
            mPendingPointOfInterestRequest = null;
        } else if (ULog.w(TAG)) {
            ULog.w(TAG, "Cannot start piloted POI: no pending POI request");
        }
    }

    @Override
    public void requestDeactivation() {
        super.requestDeactivation();
        sendCommand(Piloting.encodeStopPilotedPOI());
        mPendingPointOfInterestRequest = null;
    }

    /**
     * Sends a Point Of Interest start command.
     *
     * @param latitude  latitude of the location (in degrees) to look at
     * @param longitude longitude of the location (in degrees) to look at
     * @param altitude  altitude above take off point (in meters) to look at
     * @param mode      Point Of Interest mode
     */
    private void sendPointOfInterestCommand(double latitude, double longitude, double altitude,
                                            @NonNull PointOfInterestPilotingItf.Mode mode) {
        if (mPoiV2Supported) {
            sendCommand(Piloting.encodeStartPilotedPOIV2(latitude, longitude, altitude, convert(mode)));
        } else if (mode == PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL) {
            sendCommand(Piloting.encodeStartPilotedPOI(latitude, longitude, altitude));
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
     * Updates the current Point Of Interest running on the drone.
     *
     * @param available              {@code true} if piloted Point Of Interest is available, otherwise {@code false}
     * @param currentPointOfInterest current Point Of Interest, {@code null} otherwise
     */
    private void updateCurrentPointOfInterest(boolean available, @Nullable PointOfInterestCore currentPointOfInterest) {
        boolean changed = false;
        if (mAvailable != available) {
            mAvailable = available;
            changed = true;
        }
        if ((currentPointOfInterest == null && mCurrentPointOfInterest != null)
            || (currentPointOfInterest != null && !currentPointOfInterest.equals(mCurrentPointOfInterest))) {
            mCurrentPointOfInterest = currentPointOfInterest;
            changed = true;
        }
        if (changed) {
            updateState();
        }
    }

    /**
     * Updates the state of the piloting interface.
     * <p>
     * If the drone is not flying or is marked as unavailable by the drone, the interface state is set to {@code
     * UNAVAILABLE}.<br> Otherwise, the interface state is set to {@code IDLE} if there is no running Point Of Interest
     * or to {@code ACTIVE} if there is a running Point Of Interest.<br>
     */
    private void updateState() {
        if (mDroneFlying && mAvailable) {
            mPilotingItf.updateCurrentPointOfInterest(mCurrentPointOfInterest);
            if (mCurrentPointOfInterest == null) {
                notifyIdle();
            } else {
                notifyActive();
            }
        } else {
            mPilotingItf.updateCurrentPointOfInterest(null);
            mPendingPointOfInterestRequest = null;
            notifyUnavailable();
        }
    }

    /**
     * Converts a groundsdk {@link PointOfInterestPilotingItf.Mode Point Of Interest mode} into its arsdk
     * {@link ArsdkFeatureArdrone3.PilotingStartpilotedpoiv2Mode representation}.
     *
     * @param mode groundsdk Point Of Interest mode to convert
     *
     * @return arsdk representation of the specified mode
     */
    @NonNull
    private static ArsdkFeatureArdrone3.PilotingStartpilotedpoiv2Mode convert(
            @NonNull PointOfInterestPilotingItf.Mode mode) {
        switch (mode) {
            case LOCKED_GIMBAL:
                return ArsdkFeatureArdrone3.PilotingStartpilotedpoiv2Mode.LOCKED_GIMBAL;
            case FREE_GIMBAL:
                return ArsdkFeatureArdrone3.PilotingStartpilotedpoiv2Mode.FREE_GIMBAL;
        }
        return null;
    }

    /**
     * Converts an arsdk {@link ArsdkFeatureArdrone3.PilotingstatePilotedpoiv2Mode Point Of Interest mode} into its
     * groundsdk {@link PointOfInterestPilotingItf.Mode representation}.
     *
     * @param mode arsdk Point Of Interest mode to convert
     *
     * @return groundsdk representation of the specified mode
     */
    @NonNull
    private static PointOfInterestPilotingItf.Mode convert(
            @NonNull ArsdkFeatureArdrone3.PilotingstatePilotedpoiv2Mode mode) {
        switch (mode) {
            case LOCKED_GIMBAL:
                return PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL;
            case FREE_GIMBAL:
                return PointOfInterestPilotingItf.Mode.FREE_GIMBAL;
        }
        return null;
    }

    @Override
    protected final void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureArdrone3.PilotingState.UID) {
            ArsdkFeatureArdrone3.PilotingState.decode(command, mPilotingStateCallback);
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
                    if (state != null) {
                        switch (state) {
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
                }

                @Override
                public void onPilotedPOI(double latitude, double longitude, double altitude, @Nullable
                        ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus status) {
                    if (status == ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus.RUNNING
                        && Double.compare(latitude, UNKNOWN_COORDINATE) != 0
                        && Double.compare(longitude, UNKNOWN_COORDINATE) != 0
                        && Double.compare(altitude, UNKNOWN_COORDINATE) != 0) {
                        updateCurrentPointOfInterest(true, new PointOfInterestCore(latitude, longitude, altitude,
                                PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL));
                    } else {
                        boolean available = status != null
                                            && status != ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus.UNAVAILABLE;
                        updateCurrentPointOfInterest(available, null);
                    }
                }

                @Override
                public void onPilotedPOIV2(double latitude, double longitude, double altitude,
                                           @Nullable ArsdkFeatureArdrone3.PilotingstatePilotedpoiv2Mode mode,
                                           @Nullable ArsdkFeatureArdrone3.PilotingstatePilotedpoiv2Status status) {
                    mPoiV2Supported = true;

                    if (mode != null
                        && status == ArsdkFeatureArdrone3.PilotingstatePilotedpoiv2Status.RUNNING
                        && Double.compare(latitude, UNKNOWN_COORDINATE) != 0
                        && Double.compare(longitude, UNKNOWN_COORDINATE) != 0
                        && Double.compare(altitude, UNKNOWN_COORDINATE) != 0) {

                        updateCurrentPointOfInterest(true, new PointOfInterestCore(latitude, longitude, altitude,
                                convert(mode)));
                    } else {
                        boolean available = status != null
                            && status != ArsdkFeatureArdrone3.PilotingstatePilotedpoiv2Status.UNAVAILABLE;
                        updateCurrentPointOfInterest(available, null);
                    }
                }
            };

    /** Backend of PointOfInterestPilotingItf implementation. */
    private final class Backend extends ActivablePilotingItfController.Backend
            implements PointOfInterestPilotingItfCore.Backend {

        @Override
        public void start(double latitude, double longitude, double altitude,
                          @NonNull PointOfInterestPilotingItf.Mode mode) {
            switch (mPilotingItf.getState()) {
                case IDLE:
                    mPendingPointOfInterestRequest = new PointOfInterestCore(latitude, longitude, altitude, mode);
                    activate();
                    break;
                case ACTIVE:
                    sendPointOfInterestCommand(latitude, longitude, altitude, mode);
                    break;
                case UNAVAILABLE:
                    break;
            }
        }

        @Override
        public void setPitch(int pitch) {
            AnafiPointOfInterestPilotingItf.this.setPitch(pitch);
        }

        @Override
        public void setRoll(int roll) {
            AnafiPointOfInterestPilotingItf.this.setRoll(roll);
        }

        @Override
        public void setVerticalSpeed(int verticalSpeed) {
            setGaz(verticalSpeed);
        }
    }
}
