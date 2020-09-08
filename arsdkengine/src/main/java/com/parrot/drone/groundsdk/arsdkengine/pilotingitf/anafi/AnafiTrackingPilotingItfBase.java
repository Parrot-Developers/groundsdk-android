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

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.PilotingItfActivationController;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.ActivablePilotingItfController;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.tracking.TrackingIssue;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.TrackingPilotingItfCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureFollowMe;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.EnumMap;
import java.util.EnumSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_TRACKING_PITF;

/**
 * Base implementation for autonomous tracking piloting interface controllers.
 */
abstract class AnafiTrackingPilotingItfBase extends ActivablePilotingItfController {

    /** Tracking modes supported by this controller. */
    @NonNull
    private final EnumSet<ArsdkFeatureFollowMe.Mode> mSupportedModes;

    /** Tracking modes available on the connected device. */
    @NonNull
    final EnumSet<ArsdkFeatureFollowMe.Mode> mAvailableModes;

    /** Current availability issues, by supported mode. */
    @NonNull
    private final EnumMap<ArsdkFeatureFollowMe.Mode, EnumSet<TrackingIssue>> mAvailabilityIssues;

    /** Current quality issues, by supported mode. */
    @NonNull
    private final EnumMap<ArsdkFeatureFollowMe.Mode, EnumSet<TrackingIssue>> mQualityIssues;

    /** Current tracking mode, as received from the drone. */
    @NonNull
    private ArsdkFeatureFollowMe.Mode mCurrentMode;

    /** Current tracking behavior, as received from the drone. */
    @NonNull
    private ArsdkFeatureFollowMe.Behavior mCurrentBehavior;

    /** {@code true} when the drone is currently landed. */
    private boolean mLanded;

    /**
     * Constructor.
     *
     * @param activationController activation controller that owns this piloting interface controller
     * @param supportedModes       tracking modes this controller supports
     */
    AnafiTrackingPilotingItfBase(@NonNull PilotingItfActivationController activationController,
                                 @NonNull EnumSet<ArsdkFeatureFollowMe.Mode> supportedModes) {
        super(activationController, true);
        mSupportedModes = supportedModes;
        mAvailableModes = EnumSet.noneOf(ArsdkFeatureFollowMe.Mode.class);
        mAvailabilityIssues = new EnumMap<>(ArsdkFeatureFollowMe.Mode.class);
        mQualityIssues = new EnumMap<>(ArsdkFeatureFollowMe.Mode.class);
        mCurrentMode = ArsdkFeatureFollowMe.Mode.NONE;
        mCurrentBehavior = ArsdkFeatureFollowMe.Behavior.IDLE;
    }

    @NonNull
    @Override
    public abstract TrackingPilotingItfCore getPilotingItf();

    @Override
    protected void onConnected() {
        getPilotingItf().publish();
    }

    @Override
    protected final void onDisconnected() {
        mAvailableModes.clear();
        getPilotingItf().unpublish();
        super.onDisconnected();
    }

    @Override
    public void requestDeactivation() {
        super.requestDeactivation();
        sendCommand(ArsdkFeatureFollowMe.encodeStop());
    }

    @Override
    protected final void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureFollowMe.UID) {
            ArsdkFeatureFollowMe.decode(command, mFollowMeCallbacks);
        } else if (featureId == ArsdkFeatureArdrone3.PilotingState.UID) {
            ArsdkFeatureArdrone3.PilotingState.decode(command, mPilotingStateCallbacks);
        }
    }

    /** TrackingPilotingItfCore base backend implementation. */
    class Backend extends ActivablePilotingItfController.Backend implements TrackingPilotingItfCore.Backend {

        @Override
        public void setPitch(int pitch) {
            AnafiTrackingPilotingItfBase.this.setPitch(pitch);
        }

        @Override
        public void setRoll(int roll) {
            AnafiTrackingPilotingItfBase.this.setRoll(roll);
        }

        @Override
        public void setVerticalSpeed(int verticalSpeed) {
            setGaz(verticalSpeed);
        }
    }

    /**
     * Updates the controlled piloting interface mode.
     * <p>
     * This method should be overridden by subclasses that control a piloting interface that provide information
     * concerning the current mode.
     * <p>
     * Default implementation does nothing.
     *
     * @param mode current mode
     */
    void updateMode(@NonNull ArsdkFeatureFollowMe.Mode mode) {
    }

    /**
     * Updates the controlled piloting interface behavior.
     * <p>
     * This method should be overridden by subclasses that control a piloting interface that provide information
     * concerning the current behavior.
     * <p>
     * Default implementation does nothing.
     *
     * @param behavior current behavior
     */
    void updateBehavior(@NonNull ArsdkFeatureFollowMe.Behavior behavior) {
    }

    /**
     * Requests a tracking mode change.
     *
     * @param mode mode to change to
     *
     * @return {@code true} in case a mode change request was sent to the drone, waiting for confirmation, otherwise
     *         {@code false}
     */
    final boolean requestModeChange(@NonNull ArsdkFeatureFollowMe.Mode mode) {
        if (mode == mCurrentMode || !mSupportedModes.contains(mode)) {
            return false;
        }

        TrackingPilotingItfCore pilotingItf = getPilotingItf();

        if (pilotingItf.getState() == Activable.State.ACTIVE) {
            return sendCommand(ArsdkFeatureFollowMe.encodeStart(mode));
        }

        updateMode(mode);
        pilotingItf.notifyUpdated();

        return false;
    }


    /** Callbacks called when a command of the feature ArsdkFeatureFollowMe is decoded. */
    private final ArsdkFeatureFollowMe.Callback mFollowMeCallbacks = new ArsdkFeatureFollowMe.Callback() {

        @Override
        public void onState(@Nullable ArsdkFeatureFollowMe.Mode mode, @Nullable ArsdkFeatureFollowMe.Behavior behavior,
                            @Nullable ArsdkFeatureFollowMe.Animation animation, int animationAvailableBitField) {
            if (ULog.d(TAG_TRACKING_PITF)) {
                ULog.d(TAG_TRACKING_PITF, "onState [mode: " + mode + ", behavior: " + behavior + "]");
            }

            mCurrentMode = mode == null ? ArsdkFeatureFollowMe.Mode.NONE : mode;
            mCurrentBehavior = behavior == null ? ArsdkFeatureFollowMe.Behavior.IDLE : behavior;
            updateState();
        }

        @Override
        public void onModeInfo(@Nullable ArsdkFeatureFollowMe.Mode mode, int missingRequirementsBitField,
                               int improvementsBitField) {

            if (!mSupportedModes.contains(mode)) {
                return;
            }

            if (!isConnected()) {
                mAvailableModes.add(mode);
            }

            EnumSet<ArsdkFeatureFollowMe.Input> missingInputs = EnumSet.complementOf(
                    ArsdkFeatureFollowMe.Input.fromBitfield(missingRequirementsBitField));

            EnumSet<ArsdkFeatureFollowMe.Input> improvableInputs = EnumSet.complementOf(
                    ArsdkFeatureFollowMe.Input.fromBitfield(improvementsBitField));

            if (ULog.d(TAG_TRACKING_PITF)) {
                ULog.d(TAG_TRACKING_PITF, "onModeInfo [mode: " + mode + ", missing inputs: " + missingInputs
                                          + ", improvable inputs: " + improvableInputs + "]");
            }

            mAvailabilityIssues.put(mode, convert(missingInputs));
            mQualityIssues.put(mode, convert(improvableInputs));

            updateState();
        }
    };

    // TODO: this should be abstracted away and be provided somehow by the drone controller, so that this component
    // TODO  becomes usable by other drone controller than 'Ardrone3' ones.

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.PilotingState is decoded. */
    private final ArsdkFeatureArdrone3.PilotingState.Callback mPilotingStateCallbacks =
            new ArsdkFeatureArdrone3.PilotingState.Callback() {

                @Override
                public void onFlyingStateChanged(
                        @Nullable ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState state) {
                    boolean landed = state == ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED;
                    if (mLanded != landed) {
                        mLanded = landed;
                        updateState();
                    }
                }
            };

    /**
     * Computes current state and updates controlled piloting interface accordingly.
     */
    private void updateState() {
        // compute whether active
        boolean active = mCurrentBehavior != ArsdkFeatureFollowMe.Behavior.IDLE
                         && mSupportedModes.contains(mCurrentMode)
                         && !mLanded;

        // compute availability issues
        EnumSet<TrackingIssue> availabilityIssues = EnumSet.noneOf(TrackingIssue.class);
        if (!active) {
            if (mLanded) {
                availabilityIssues.add(TrackingIssue.DRONE_NOT_FLYING);
            }
            for (EnumSet<TrackingIssue> issues : mAvailabilityIssues.values()) {
                if (issues != null) {
                    availabilityIssues.addAll(issues);
                }
            }
        }

        // compute quality issues
        EnumSet<TrackingIssue> qualityIssues = EnumSet.noneOf(TrackingIssue.class);
        if (availabilityIssues.isEmpty()) {
            for (EnumSet<TrackingIssue> issues : mQualityIssues.values()) {
                if (issues != null) {
                    qualityIssues.addAll(issues);
                }
            }
        }

        // update current mode & behavior
        updateMode(mCurrentMode);
        updateBehavior(active ? mCurrentBehavior : ArsdkFeatureFollowMe.Behavior.IDLE);

        // update issues
        getPilotingItf().updateAvailabilityIssues(availabilityIssues)
                        .updateQualityIssues(qualityIssues);

        if (active) {
            notifyActive();
        } else if (availabilityIssues.isEmpty()) {
            notifyIdle();
        } else {
            notifyUnavailable();
        }
    }

    /**
     * Converts an arsdk {@link ArsdkFeatureFollowMe.Input input} set into its groundsdk {@link TrackingIssue
     * representation}.
     *
     * @param inputs arsdk input set to convert
     *
     * @return groundsdk representation of the specified input set
     */
    @NonNull
    private static EnumSet<TrackingIssue> convert(@NonNull EnumSet<ArsdkFeatureFollowMe.Input> inputs) {
        EnumSet<TrackingIssue> issues = EnumSet.noneOf(TrackingIssue.class);
        for (ArsdkFeatureFollowMe.Input input : inputs) {
            switch (input) {
                case DRONE_CALIBRATED:
                    issues.add(TrackingIssue.DRONE_NOT_CALIBRATED);
                    break;
                case DRONE_GPS_GOOD_ACCURACY:
                    issues.add(TrackingIssue.DRONE_GPS_INFO_INACCURATE);
                    break;
                case TARGET_GPS_GOOD_ACCURACY:
                    issues.add(TrackingIssue.TARGET_GPS_INFO_INACCURATE);
                    break;
                case TARGET_BAROMETER_OK:
                    issues.add(TrackingIssue.TARGET_BAROMETER_INFO_INACCURATE);
                    break;
                case DRONE_FAR_ENOUGH:
                    issues.add(TrackingIssue.DRONE_TOO_CLOSE_TO_TARGET);
                    break;
                case DRONE_HIGH_ENOUGH:
                    issues.add(TrackingIssue.DRONE_TOO_CLOSE_TO_GROUND);
                    break;
                case IMAGE_DETECTION:
                    issues.add(TrackingIssue.TARGET_DETECTION_INFO_MISSING);
                    break;
                case TARGET_GOOD_SPEED:
                    issues.add(TrackingIssue.TARGET_HORIZONTAL_SPEED_TOO_HIGH);
                    issues.add(TrackingIssue.TARGET_VERTICAL_SPEED_TOO_HIGH);
                    break;
                case DRONE_CLOSE_ENOUGH:
                    issues.add(TrackingIssue.DRONE_TOO_FAR_FROM_TARGET);
                    break;
            }
        }
        return issues;
    }
}
