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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.RCController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.SystemInfoControllerBase;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSkyctrl;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** SystemInfo peripheral controller for SkyController family remote controls. */
public class SkyControllerSystemInfo extends SystemInfoControllerBase {

    /**
     * Constructor.
     *
     * @param rcController the device controller that owns this peripheral controller.
     */
    public SkyControllerSystemInfo(@NonNull RCController rcController) {
        super(rcController);
    }

    @Override
    protected final void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureSkyctrl.SettingsState.UID) {
            ArsdkFeatureSkyctrl.SettingsState.decode(command, mSettingsStateCallback);
        }
    }

    @Override
    protected final boolean sendFactoryReset() {
        return sendCommand(ArsdkFeatureSkyctrl.Factory.encodeReset());
    }

    @Override
    protected final boolean sendResetSettings() {
        return sendCommand(ArsdkFeatureSkyctrl.Settings.encodeReset());
    }

    /** Callbacks called when a command of the feature ArsdkFeatureSkyctrl.SettingsState is decoded. */
    private final ArsdkFeatureSkyctrl.SettingsState.Callback mSettingsStateCallback =
            new ArsdkFeatureSkyctrl.SettingsState.Callback() {

                @SuppressWarnings("deprecation")
                @Override
                public void onResetChanged() {
                    onSettingsReset();
                    mSystemInfo.notifyUpdated();
                }

                @Override
                public void onProductSerialChanged(String serialnumber) {
                    onSerial(serialnumber);
                    mSystemInfo.notifyUpdated();
                }

                @Override
                public void onProductVersionChanged(String software, String hardware) {
                    onHardwareVersion(hardware);
                    onFirmwareVersion(software);
                    mSystemInfo.notifyUpdated();
                }
            };
}
