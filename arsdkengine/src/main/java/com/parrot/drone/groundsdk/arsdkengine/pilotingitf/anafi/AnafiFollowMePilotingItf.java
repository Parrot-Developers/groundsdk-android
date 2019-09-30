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
import com.parrot.drone.groundsdk.device.pilotingitf.FollowMePilotingItf;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.FollowMePilotingItfCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureFollowMe;

import java.util.EnumSet;
import java.util.stream.Collectors;

/** FollowMe piloting interface controller for Anafi family drones. */
public class AnafiFollowMePilotingItf extends AnafiTrackingPilotingItfBase {

    /** Piloting interface for which this object is the backend. */
    @NonNull
    private final FollowMePilotingItfCore mPilotingItf;

    /**
     * Constructor.
     *
     * @param activationController activation controller that owns this piloting interface controller
     */
    public AnafiFollowMePilotingItf(@NonNull PilotingItfActivationController activationController) {
        super(activationController, EnumSet.of(ArsdkFeatureFollowMe.Mode.GEOGRAPHIC,
                ArsdkFeatureFollowMe.Mode.RELATIVE, ArsdkFeatureFollowMe.Mode.LEASH));
        mPilotingItf = new FollowMePilotingItfCore(mComponentStore, new Backend());
    }

    @Override
    protected void onConnected() {
        mPilotingItf.mode().updateAvailableValues(mAvailableModes
                .stream()
                .map(AnafiFollowMePilotingItf::convert)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FollowMePilotingItf.Mode.class))));
        super.onConnected();
    }

    @Override
    public void requestActivation() {
        super.requestActivation();
        sendCommand(ArsdkFeatureFollowMe.encodeStart(convert(mPilotingItf.mode().getValue())));
    }

    @NonNull
    @Override
    public FollowMePilotingItfCore getPilotingItf() {
        return mPilotingItf;
    }

    @Override
    void updateMode(@NonNull ArsdkFeatureFollowMe.Mode arsdkMode) {
        FollowMePilotingItf.Mode mode = convert(arsdkMode);
        if (mode == null) {
            // even if the mode is unknown to this controller, we need to clear the updating flag if armed
            mode = mPilotingItf.mode().getValue();
        }
        mPilotingItf.mode().updateValue(mode);
    }

    @Override
    void updateBehavior(@NonNull ArsdkFeatureFollowMe.Behavior behavior) {
        mPilotingItf.updateBehavior(convert(behavior));
    }

    /** Backend of FollowMePilotingItfCore implementation. */
    private final class Backend extends AnafiTrackingPilotingItfBase.Backend
            implements FollowMePilotingItfCore.Backend {

        @Override
        public boolean setMode(@NonNull FollowMePilotingItf.Mode mode) {
            return requestModeChange(convert(mode));
        }
    }

    /**
     * Converts a groundsdk {@link FollowMePilotingItf.Mode mode} into its arsdk {@link ArsdkFeatureFollowMe.Mode
     * representation}.
     *
     * @param mode groundsdk mode to convert
     *
     * @return arsdk representation of the specified mode
     */
    @NonNull
    private static ArsdkFeatureFollowMe.Mode convert(@NonNull FollowMePilotingItf.Mode mode) {
        switch (mode) {
            case GEOGRAPHIC:
                return ArsdkFeatureFollowMe.Mode.GEOGRAPHIC;
            case RELATIVE:
                return ArsdkFeatureFollowMe.Mode.RELATIVE;
            case LEASH:
                return ArsdkFeatureFollowMe.Mode.LEASH;
        }
        return null;
    }

    /**
     * Converts an arsdk {@link ArsdkFeatureFollowMe.Mode mode} into its groundsdk {@link FollowMePilotingItf.Mode
     * representation}.
     *
     * @param mode arsdk mode to convert
     *
     * @return groundsdk representation of the specified mode
     */
    @Nullable
    private static FollowMePilotingItf.Mode convert(@NonNull ArsdkFeatureFollowMe.Mode mode) {
        switch (mode) {
            case GEOGRAPHIC:
                return FollowMePilotingItf.Mode.GEOGRAPHIC;
            case RELATIVE:
                return FollowMePilotingItf.Mode.RELATIVE;
            case LEASH:
                return FollowMePilotingItf.Mode.LEASH;
            case NONE:
            case LOOK_AT:
                return null;
        }
        return null;
    }

    /**
     * Converts an arsdk {@link ArsdkFeatureFollowMe.Behavior behavior} into its groundsdk {@link
     * FollowMePilotingItf.Behavior representation}.
     *
     * @param behavior arsdk behavior to convert
     *
     * @return groundsdk representation of the specified behavior
     */
    @NonNull
    private static FollowMePilotingItf.Behavior convert(@NonNull ArsdkFeatureFollowMe.Behavior behavior) {
        switch (behavior) {
            case IDLE:
                return FollowMePilotingItf.Behavior.INACTIVE;
            case FOLLOW:
                return FollowMePilotingItf.Behavior.FOLLOWING;
            case LOOK_AT:
                return FollowMePilotingItf.Behavior.STATIONARY;
        }
        return null;
    }
}
