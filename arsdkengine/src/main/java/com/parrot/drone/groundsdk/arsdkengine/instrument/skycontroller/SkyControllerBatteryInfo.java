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

package com.parrot.drone.groundsdk.arsdkengine.instrument.skycontroller;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.RCController;
import com.parrot.drone.groundsdk.arsdkengine.instrument.RCInstrumentController;
import com.parrot.drone.groundsdk.internal.device.instrument.BatteryInfoCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSkyctrl;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** BatteryInfo instrument controller for SkyController family remote controls. */
public class SkyControllerBatteryInfo extends RCInstrumentController {

    /** The BatteryInfo instrument from which this object is the backend. */
    @NonNull
    private final BatteryInfoCore mBatteryInfo;

    /**
     * Constructor.
     *
     * @param rcController the remote control controller that owns this instrument controller.
     */
    public SkyControllerBatteryInfo(@NonNull RCController rcController) {
        super(rcController);
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
        if (featureId == ArsdkFeatureSkyctrl.SkyControllerState.UID) {
            ArsdkFeatureSkyctrl.SkyControllerState.decode(command, mSkyControllerStateCallback);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureSkyctrl.SkyControllerState is decoded. */
    private final ArsdkFeatureSkyctrl.SkyControllerState.Callback mSkyControllerStateCallback =
            new ArsdkFeatureSkyctrl.SkyControllerState.Callback() {

                @Override
                public void onBatteryChanged(int percent) {
                    if (percent == 255) {
                        mBatteryInfo.updateLevel(100).updateCharging(true);
                    } else {
                        mBatteryInfo.updateLevel(percent).updateCharging(false);
                    }
                    mBatteryInfo.notifyUpdated();
                }
            };
}
