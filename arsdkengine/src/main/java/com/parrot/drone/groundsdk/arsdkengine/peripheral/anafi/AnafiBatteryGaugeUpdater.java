/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.device.peripheral.BatteryGaugeUpdater;
import com.parrot.drone.groundsdk.internal.device.peripheral.BatteryGaugeUpdaterCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGaugeFwUpdater;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.util.EnumSet;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** BatteryGaugeUpdater peripheral for Anafi family drones. */
public class AnafiBatteryGaugeUpdater extends DronePeripheralController {

    /** BatteryGaugeUpdater peripheral for which this object is the backend. */
    @NonNull
    private final BatteryGaugeUpdaterCore mGaugeUpdater;

    /** Whether battery gauge firmware is updatable. */
    private boolean mUpdatable;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller
     */
    public AnafiBatteryGaugeUpdater(@NonNull DroneController droneController) {
        super(droneController);
        mGaugeUpdater = new BatteryGaugeUpdaterCore(mComponentStore, mBackend);
    }

    @Override
    protected void onConnected() {
        if (mUpdatable) {
            mGaugeUpdater.publish();
        }
    }

    @Override
    protected void onDisconnected() {
        mUpdatable = false;
        mGaugeUpdater.unpublish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureGaugeFwUpdater.UID) {
            ArsdkFeatureGaugeFwUpdater.decode(command, mGaugeUpdaterCallback);
        }
    }

    /** Backend of BatteryGaugeUpdaterCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final BatteryGaugeUpdaterCore.Backend mBackend = new BatteryGaugeUpdaterCore.Backend() {

        @Override
        public void prepareUpdate() {
            sendCommand(ArsdkFeatureGaugeFwUpdater.encodePrepare());
        }

        @Override
        public void update() {
            sendCommand(ArsdkFeatureGaugeFwUpdater.encodeUpdate());
        }
    };

    /** Callbacks called when a command of the feature ArsdkFeatureGaugeFwUpdater is decoded. */
    private final ArsdkFeatureGaugeFwUpdater.Callback mGaugeUpdaterCallback =
            new ArsdkFeatureGaugeFwUpdater.Callback() {

                @Override
                public void onStatus(@Nullable ArsdkFeatureGaugeFwUpdater.Diag diag, int missingRequirementsBitField,
                                     @Nullable ArsdkFeatureGaugeFwUpdater.State state) {
                    if (diag == null || state == null) {
                        throw new ArsdkCommand.RejectedEventException("Invalid gauge firmware updater status");
                    }

                    mUpdatable = diag == ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE;

                    switch (state) {
                        case READY_TO_PREPARE:
                            mGaugeUpdater.updateState(BatteryGaugeUpdater.State.READY_TO_PREPARE)
                                         .updateProgress(0);
                            break;
                        case PREPARATION_IN_PROGRESS:
                            mGaugeUpdater.updateState(BatteryGaugeUpdater.State.PREPARING_UPDATE);
                            break;
                        case READY_TO_UPDATE:
                            mGaugeUpdater.updateState(BatteryGaugeUpdater.State.READY_TO_UPDATE)
                                         .updateProgress(0);
                            break;
                        case UPDATE_IN_PROGRESS:
                            mGaugeUpdater.updateState(BatteryGaugeUpdater.State.UPDATING);
                            break;
                    }

                    EnumSet<ArsdkFeatureGaugeFwUpdater.Requirements> missingRequirements =
                            ArsdkFeatureGaugeFwUpdater.Requirements.fromBitfield(missingRequirementsBitField);
                    mGaugeUpdater.updateUnavailabilityReasons(
                            missingRequirements.stream()
                                               .map(AnafiBatteryGaugeUpdater::convert)
                                               .collect(Collectors.toCollection(() -> EnumSet.noneOf(
                                                       BatteryGaugeUpdater.UnavailabilityReason.class))));

                    mGaugeUpdater.notifyUpdated();
                }

                @Override
                public void onProgress(@Nullable ArsdkFeatureGaugeFwUpdater.Result result, int percent) {
                    if (result == null) {
                        throw new ArsdkCommand.RejectedEventException("Invalid gauge firmware updater progress");
                    }

                    if (result == ArsdkFeatureGaugeFwUpdater.Result.BATTERY_ERROR) {
                        mGaugeUpdater.updateState(BatteryGaugeUpdater.State.ERROR);
                    }

                    if (mGaugeUpdater.state() == BatteryGaugeUpdater.State.PREPARING_UPDATE) {
                        mGaugeUpdater.updateProgress(percent);
                    }

                    mGaugeUpdater.notifyUpdated();
                }
            };

    /**
     * Converts an arsdk {@link ArsdkFeatureGaugeFwUpdater.Requirements requirement} into its groundsdk
     * {@link BatteryGaugeUpdater.UnavailabilityReason representation}.
     *
     * @param requirement arsdk requirement to convert
     *
     * @return groundsdk representation of the specified requirement
     */
    @NonNull
    private static BatteryGaugeUpdater.UnavailabilityReason convert(
            @NonNull ArsdkFeatureGaugeFwUpdater.Requirements requirement) {
        switch (requirement) {
            case USB:
                return BatteryGaugeUpdater.UnavailabilityReason.NOT_USB_POWERED;
            case RSOC:
                return BatteryGaugeUpdater.UnavailabilityReason.INSUFFICIENT_CHARGE;
            case DRONE_STATE:
                return BatteryGaugeUpdater.UnavailabilityReason.DRONE_NOT_LANDED;
        }
        return null;
    }
}
