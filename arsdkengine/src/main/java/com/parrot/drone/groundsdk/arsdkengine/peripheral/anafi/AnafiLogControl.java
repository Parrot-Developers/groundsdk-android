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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.internal.device.peripheral.LogControlCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSecurityEdition;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** LogControl peripheral controller for Anafi family drones. */
public class AnafiLogControl extends DronePeripheralController {

    /** LogControl peripheral for which this object is the backend. */
    @NonNull
    private final LogControlCore mLogControl;

    /** {@code true} if the connected drone has any SE capability. */
    private boolean mSupported;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiLogControl(@NonNull DroneController droneController) {
        super(droneController);
        mLogControl = new LogControlCore(mComponentStore, mBackend);
    }

    @Override
    protected void onConnected() {
        if (mSupported) {
            mLogControl.publish();
        } else {
            forget();
        }
    }

    @Override
    protected void onDisconnected() {
        mSupported = false;
        mLogControl.unpublish();
    }

    @Override
    protected final void onForgetting() {
        forget();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureSecurityEdition.UID) {
            ArsdkFeatureSecurityEdition.decode(command, mSecurityEditionCallback);
        }
    }

    /**
     * Forgets the component.
     */
    private void forget() {
        mLogControl.unpublish();
    }

    /** Backend of LogControl implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final LogControlCore.Backend mBackend = () -> sendCommand(
            ArsdkFeatureSecurityEdition.encodeDeactivateLogs());

    /** Callbacks called when a command of the feature ArsdkFeatureSecurityEdition is decoded. */
    private final ArsdkFeatureSecurityEdition.Callback mSecurityEditionCallback =
            new ArsdkFeatureSecurityEdition.Callback() {

                @Override
                public void onCapabilities(int supportedCapabilitiesBitField) {
                    mSupported = supportedCapabilitiesBitField != 0;
                    mLogControl.updateDeactivateLogsSupported(
                            ArsdkFeatureSecurityEdition.SupportedCapabilities.DEACTIVATE_LOGS.inBitField(
                                    supportedCapabilitiesBitField)).notifyUpdated();
                }

                @Override
                public void onLogStorageState(@Nullable ArsdkFeatureSecurityEdition.LogStorageState logsState) {
                    mLogControl.updateLogsState(logsState == ArsdkFeatureSecurityEdition.LogStorageState.ENABLED)
                            .notifyUpdated();
                }
            };
}
