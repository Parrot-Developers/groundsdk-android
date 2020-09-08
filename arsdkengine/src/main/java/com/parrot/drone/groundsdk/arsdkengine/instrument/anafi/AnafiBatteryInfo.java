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
import com.parrot.drone.groundsdk.internal.device.instrument.BatteryInfoCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureBattery;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCommon;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** BatteryInfo instrument controller for Anafi family drones. */
public class AnafiBatteryInfo extends DroneInstrumentController {

    /** The BatteryInfo instrument from which this object is the backend. */
    @NonNull
    private final BatteryInfoCore mBatteryInfo;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiBatteryInfo(@NonNull DroneController droneController) {
        super(droneController);
        mBatteryInfo = new BatteryInfoCore(mComponentStore);
    }

    @Override
    protected void onConnected() {
        mBatteryInfo.publish();
    }

    @Override
    protected void onDisconnected() {
        mBatteryInfo.unpublish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureCommon.CommonState.UID) {
            ArsdkFeatureCommon.CommonState.decode(command, mCommonStateCallback);
        } else if (featureId == ArsdkFeatureBattery.UID) {
            ArsdkFeatureBattery.decode(command, mBatteryCallback);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureCommon.CommonState is decoded. */
    private final ArsdkFeatureCommon.CommonState.Callback mCommonStateCallback =
            new ArsdkFeatureCommon.CommonState.Callback() {

                @Override
                public void onBatteryStateChanged(int percent) {
                    mBatteryInfo.updateLevel(percent).notifyUpdated();
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureBattery is decoded. */
    private final ArsdkFeatureBattery.Callback mBatteryCallback = new ArsdkFeatureBattery.Callback() {

        @Override
        public void onHealth(int stateOfHealth) {
            mBatteryInfo.updateHealth(stateOfHealth).notifyUpdated();
        }

        @Override
        public void onCycleCount(long count) {
            mBatteryInfo.updateCycleCount((int) count).notifyUpdated();
        }

        @Override
        public void onSerial(@NonNull String serial) {
            mBatteryInfo.updateSerial(serial).notifyUpdated();
        }
    };
}
