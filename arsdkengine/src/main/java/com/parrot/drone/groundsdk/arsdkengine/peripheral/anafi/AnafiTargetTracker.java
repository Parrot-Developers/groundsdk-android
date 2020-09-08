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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.device.peripheral.TargetTracker;
import com.parrot.drone.groundsdk.internal.device.peripheral.tracking.TargetTrackerCore;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;
import com.parrot.drone.groundsdk.internal.utility.SystemLocation;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureFollowMe;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** TargetTracker peripheral controller for Anafi family drones. */
public class AnafiTargetTracker extends DronePeripheralController {

    /** TargetTracker peripheral for which this object is the backend. */
    @NonNull
    private final TargetTrackerCore mTracker;

    /** Latest targetIsController value received from drone. */
    private boolean mTargetIsController;

    /** Client desired tracking state when disconnected. When connected, same as {@link #mTargetIsController}. */
    private boolean mControllerTracking;

    /** Latest horizontal framing position, as received from the drone. */
    private double mHorizontalFraming;

    /** Latest vertical framing position, as received from the drone. */
    private double mVerticalFraming;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiTargetTracker(@NonNull DroneController droneController) {
        super(droneController);
        mHorizontalFraming = mVerticalFraming = TargetTrackerCore.DEFAULT_FRAMING_POSITION;
        mTracker = new TargetTrackerCore(mComponentStore, mBackend);

        if (!mDeviceController.getDeviceDict().isNew()) {
            mTracker.publish();
        }
    }

    @Override
    protected void onConnected() {
        // send controller tracking setting if different from received drone values
        if (mControllerTracking != mTargetIsController) {
            sendCommand(ArsdkFeatureFollowMe.encodeSetTargetIsController(mControllerTracking ? 1 : 0));
        } else if (mControllerTracking) { // even if drone/user values agree, we need to start monitoring as appropriate
            forwardControllerInfo(true);
        }
        // apply framing setting if different from received drone values
        TargetTracker.FramingSetting framing = mTracker.framing();
        double horizontalFraming = framing.getHorizontalPosition();
        double verticalFraming = framing.getVerticalPosition();
        if (Double.compare(mHorizontalFraming, horizontalFraming) != 0
            || Double.compare(mVerticalFraming, verticalFraming) != 0) {
            sendTargetFraming(horizontalFraming, verticalFraming);
        }
        mTracker.publish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureFollowMe.UID) {
            ArsdkFeatureFollowMe.decode(command, mFollowMeCallback);
        }
    }

    @Override
    protected void onDisconnected() {
        mTracker.cancelSettingsRollbacks()
                .clearTargetTrajectory()
                .notifyUpdated();

        if (mTargetIsController) {
            forwardControllerInfo(false);
        }
    }

    @Override
    protected void onForgetting() {
        mTracker.unpublish();
    }

    /**
     * Controls forwarding of controller info to the drone.
     * <p>
     * When enabled, if system location monitoring is available, then this method will force WIFI network usage for
     * location monitoring if possible
     * (note that sending location measurements is the responsibility of the {@link DroneController}).
     *
     * @param enable {@code true} to start forwarding controller info, {@code false} to stop
     */
    private void forwardControllerInfo(boolean enable) {
        EngineBase engine = mDeviceController.getEngine();

        SystemLocation location = engine.getUtility(SystemLocation.class);
        if (location != null) {
            if (enable) {  // location is already forwarded by DroneController, only force WIFI for increased accuracy
                location.enforceWifiUsage(this);
            } else {
                location.revokeWifiUsageEnforcement(this);
            }
        }
    }

    private boolean sendTargetFraming(double horizontalPosition, double verticalPosition) {
        return sendCommand(ArsdkFeatureFollowMe.encodeTargetFramingPosition(
                (int) Math.round(horizontalPosition * 100), (int) Math.round(verticalPosition * 100)));
    }

    /** Callbacks called when a command of the feature ArsdkFeatureFollowMe is decoded. */
    private final ArsdkFeatureFollowMe.Callback mFollowMeCallback = new ArsdkFeatureFollowMe.Callback() {

        @Override
        public void onTargetFramingPositionChanged(int horizontal, int vertical) {
            mHorizontalFraming = horizontal / 100.0;
            mVerticalFraming = vertical / 100.0;
            if (isConnected()) {
                mTracker.updateTargetPosition(mHorizontalFraming, mVerticalFraming)
                        .notifyUpdated();
            }
        }

        @Override
        public void onTargetIsController(int state) {
            boolean targetIsController = state == 1;
            if (mTargetIsController != targetIsController) {
                mTargetIsController = targetIsController;
                if (isConnected()) {
                    forwardControllerInfo(mTargetIsController);
                    mControllerTracking = mTargetIsController;
                    mTracker.updateControllerTrackingFlag(mControllerTracking).notifyUpdated();
                }
            }
        }

        @Override
        public void onTargetTrajectory(double latitude, double longitude, float altitude, float northSpeed,
                                       float eastSpeed, float downSpeed) {
            mTracker.updateTargetTrajectory(latitude, longitude, altitude, northSpeed, eastSpeed, downSpeed)
                    .notifyUpdated();
        }
    };

    /** Backend of TargetTrackerCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final TargetTrackerCore.Backend mBackend = new TargetTrackerCore.Backend() {

        @Override
        public void enableControllerTracking(boolean enable) {
            if (isConnected()) {
                if (mTargetIsController != enable) {
                    sendCommand(ArsdkFeatureFollowMe.encodeSetTargetIsController(enable ? 1 : 0));
                }
            } else {
                mControllerTracking = enable;
                mTracker.updateControllerTrackingFlag(mControllerTracking).notifyUpdated();
            }
        }

        @Override
        public boolean setTargetPosition(double horizontalPosition, double verticalPosition) {
            if (sendTargetFraming(horizontalPosition, verticalPosition)) {
                return true;
            }
            // offline, apply update
            mTracker.updateTargetPosition(horizontalPosition, verticalPosition).notifyUpdated();
            return false;
        }

        @Override
        public void sendTargetDetectionInfo(@NonNull TargetTracker.TargetDetectionInfo info) {
            // suppresses false positive lint error, to remove when lint is fixed
            @SuppressLint("Range")
            int confidence = (int) Math.round(255 * info.getConfidenceLevel());
            sendCommand(ArsdkFeatureFollowMe.encodeTargetImageDetection((float) info.getTargetAzimuth(),
                    (float) info.getTargetElevation(), (float) info.getChangeOfScale(), confidence,
                    info.isNewTarget() ? 1 : 0, info.getTimestamp()));
        }
    };
}
