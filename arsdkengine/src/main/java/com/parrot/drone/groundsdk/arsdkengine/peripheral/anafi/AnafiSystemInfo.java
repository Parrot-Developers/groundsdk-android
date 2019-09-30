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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.SystemInfoControllerBase;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCommon;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** SystemInfo peripheral controller for Anafi family drones. */
public final class AnafiSystemInfo extends SystemInfoControllerBase {

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiSystemInfo(@NonNull DroneController droneController) {
        super(droneController);
    }

    @Override
    protected boolean sendFactoryReset() {
        return sendCommand(ArsdkFeatureCommon.Factory.encodeReset());
    }

    @Override
    protected boolean sendResetSettings() {
        return sendCommand(ArsdkFeatureCommon.Settings.encodeReset());
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureCommon.SettingsState.UID) {
            ArsdkFeatureCommon.SettingsState.decode(command, mCommonSettingsStateCallbacks);
        } else if (featureId == ArsdkFeatureArdrone3.SettingsState.UID) {
            ArsdkFeatureArdrone3.SettingsState.decode(command, mArdrone3SettingsStateCallbacks);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureCommon.SettingsState is decoded. */
    private final ArsdkFeatureCommon.SettingsState.Callback mCommonSettingsStateCallbacks =
            new ArsdkFeatureCommon.SettingsState.Callback() {

                /**
                 * Serial number high part. Both high and low parts must have been received in order to forward the
                 * serial to the upper layer, and both are reset once this is done.
                 */
                @Nullable
                private String mSerialHigh;

                /**
                 * Serial number low part. Both high and low parts must have been received in order to forward the
                 * serial to the upper layer, and both are reset once this is done.
                 */
                @Nullable
                private String mSerialLow;

                @Override
                public void onResetChanged() {
                    onSettingsReset();
                    mSystemInfo.notifyUpdated();
                }

                @Override
                public void onProductVersionChanged(String software, String hardware) {
                    onHardwareVersion(hardware);
                    onFirmwareVersion(software);
                    mSystemInfo.notifyUpdated();
                }

                @Override
                public void onProductSerialHighChanged(String high) {
                    mSerialHigh = high;
                    updateSerial();
                }

                @Override
                public void onProductSerialLowChanged(String low) {
                    mSerialLow = low;
                    updateSerial();
                }

                @Override
                public void onBoardIdChanged(String id) {
                    if (id != null) {
                        onBoardId(id);
                        mSystemInfo.notifyUpdated();
                    }
                }

                private void updateSerial() {
                    if (mSerialLow != null && mSerialHigh != null) {
                        String serial = mSerialHigh + mSerialLow;
                        mSerialHigh = mSerialLow = null;

                        onSerial(serial);
                        mSystemInfo.notifyUpdated();
                    }
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.SettingsState is decoded. */
    private final ArsdkFeatureArdrone3.SettingsState.Callback mArdrone3SettingsStateCallbacks =
            new ArsdkFeatureArdrone3.SettingsState.Callback() {

                @Override
                public void onCPUID(String id) {
                    onCpuId(id);
                    mSystemInfo.notifyUpdated();
                }
            };
}
