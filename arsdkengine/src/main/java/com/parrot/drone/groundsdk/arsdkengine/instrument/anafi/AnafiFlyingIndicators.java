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

package com.parrot.drone.groundsdk.arsdkengine.instrument.anafi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.instrument.DroneInstrumentController;
import com.parrot.drone.groundsdk.device.instrument.FlyingIndicators;
import com.parrot.drone.groundsdk.internal.device.instrument.FlyingIndicatorsCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** Flying indicator instrument controller for the Anafi family drones. */
public class AnafiFlyingIndicators extends DroneInstrumentController {

    /** The flying indicator from which this object is the backend. */
    @NonNull
    private final FlyingIndicatorsCore mFlyingIndicator;

    /**
     * Constructor.
     *
     * @param droneController The drone controller that owns this component controller.
     */
    public AnafiFlyingIndicators(@NonNull DroneController droneController) {
        super(droneController);
        mFlyingIndicator = new FlyingIndicatorsCore(mComponentStore);
        mFlyingIndicator.updateLandedState(FlyingIndicators.LandedState.IDLE)
                        .updateFlyingState(FlyingIndicators.FlyingState.NONE)
                        .notifyUpdated();
    }

    @Override
    public void onConnected() {
        mFlyingIndicator.publish();
    }

    @Override
    public void onDisconnected() {
        mFlyingIndicator.unpublish();
    }

    @Override
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureArdrone3.PilotingState.UID) {
            ArsdkFeatureArdrone3.PilotingState.decode(command, mPilotingStateCallback);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.PilotingState is decoded. */
    private final ArsdkFeatureArdrone3.PilotingState.Callback mPilotingStateCallback =
            new ArsdkFeatureArdrone3.PilotingState.Callback() {

                @Override
                public void onFlyingStateChanged(
                        @Nullable ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState state) {
                    if (state != null) switch (state) {
                        case LANDED:
                            mFlyingIndicator.updateLandedState(FlyingIndicators.LandedState.IDLE).notifyUpdated();
                            break;
                        case USERTAKEOFF:
                            mFlyingIndicator.updateLandedState(FlyingIndicators.LandedState.WAITING_USER_ACTION)
                                            .notifyUpdated();
                            break;
                        case MOTOR_RAMPING:
                            mFlyingIndicator.updateLandedState(FlyingIndicators.LandedState.MOTOR_RAMPING)
                                            .notifyUpdated();
                            break;
                        case TAKINGOFF:
                            mFlyingIndicator.updateFlyingState(FlyingIndicators.FlyingState.TAKING_OFF)
                                            .notifyUpdated();
                            break;
                        case HOVERING:
                            mFlyingIndicator.updateFlyingState(FlyingIndicators.FlyingState.WAITING)
                                            .notifyUpdated();
                            break;
                        case FLYING:
                            mFlyingIndicator.updateFlyingState(FlyingIndicators.FlyingState.FLYING)
                                            .notifyUpdated();
                            break;
                        case LANDING:
                            mFlyingIndicator.updateFlyingState(FlyingIndicators.FlyingState.LANDING)
                                            .notifyUpdated();
                            break;
                        case EMERGENCY:
                            mFlyingIndicator.updateState(FlyingIndicators.State.EMERGENCY).notifyUpdated();
                            break;
                        case EMERGENCY_LANDING:
                            mFlyingIndicator.updateState(FlyingIndicators.State.EMERGENCY_LANDING).notifyUpdated();
                            break;
                    }
                }
            };
}
