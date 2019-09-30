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

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.instrument.DroneInstrumentController;
import com.parrot.drone.groundsdk.internal.device.instrument.AltimeterCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** Altimeter instrument controller for Anafi family drones. */
public class AnafiAltimeter extends DroneInstrumentController {

    /** The altimeter from which this object is the backend. */
    @NonNull
    private final AltimeterCore mAltimeter;

    /**
     * Constructor.
     *
     * @param droneController The drone controller that owns this component controller.
     */
    public AnafiAltimeter(@NonNull DroneController droneController) {
        super(droneController);
        mAltimeter = new AltimeterCore(mComponentStore);
    }

    @Override
    public void onConnected() {
        mAltimeter.publish();
    }

    @Override
    public void onDisconnected() {
        mAltimeter.unpublish();
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

                /** Value sent by drone when latitude/longitude or altitude are not available. */
                private static final double VALUE_UNAVAILABLE = 500;

                /** Whether the {@link #onGpsLocationChanged} callback was triggered once. */
                private boolean mUseOnGpsLocationChanged;

                @Override
                public void onPositionChanged(double latitude, double longitude, double altitude) {
                    if (mUseOnGpsLocationChanged) {
                        return;
                    }
                    if (Double.compare(altitude, VALUE_UNAVAILABLE) != 0
                        || (Double.compare(latitude, VALUE_UNAVAILABLE) != 0
                            && Double.compare(longitude, VALUE_UNAVAILABLE) != 0)) {
                        mAltimeter.updateAbsoluteAltitude(altitude);
                    } else {
                        mAltimeter.resetAbsoluteAltitude();
                    }
                    mAltimeter.notifyUpdated();
                }

                @Override
                public void onSpeedChanged(float speedX, float speedY, float speedZ) {
                    // z-axis points down, yet we want positive speeds when the drone goes up, hence the minus
                    mAltimeter.updateVerticalSpeed(-speedZ).notifyUpdated();
                }

                @Override
                public void onAltitudeChanged(double altitude) {
                    mAltimeter.updateTakeOffRelativeAltitude(altitude).notifyUpdated();
                }

                @Override
                public void onGpsLocationChanged(double latitude, double longitude, double altitude,
                                                 int latitudeAccuracy, int longitudeAccuracy, int altitudeAccuracy) {
                    mUseOnGpsLocationChanged = true;
                    if (Double.compare(altitude, VALUE_UNAVAILABLE) != 0
                        || (Double.compare(latitude, VALUE_UNAVAILABLE) != 0
                            && Double.compare(longitude, VALUE_UNAVAILABLE) != 0)) {
                        mAltimeter.updateAbsoluteAltitude(altitude);
                    } else {
                        mAltimeter.resetAbsoluteAltitude();
                    }
                    mAltimeter.notifyUpdated();
                }
            };
}
