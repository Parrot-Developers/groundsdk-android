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
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.internal.device.peripheral.BeeperCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** Beeper peripheral controller for Anafi family drones. */
public final class AnafiBeeper extends DronePeripheralController {

    /** Beeper peripheral for which this object is the backend. */
    @NonNull
    private final BeeperCore mBeeper;

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.SoundState is decoded. */
    private final ArsdkFeatureArdrone3.SoundState.Callback mArdrone3SoundStateCallbacks =
            new ArsdkFeatureArdrone3.SoundState.Callback() {

                @Override
                public void onAlertSound(@Nullable ArsdkFeatureArdrone3.SoundstateAlertsoundState state) {
                    mBeeper.updateAlertSoundPlaying(state == ArsdkFeatureArdrone3.SoundstateAlertsoundState.PLAYING)
                           .notifyUpdated();
                }
            };

    /** Backend of BeeperCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final BeeperCore.Backend mBackend = new BeeperCore.Backend() {

        @Override
        public boolean startAlertSound() {
            sendCommand(ArsdkFeatureArdrone3.Sound.encodeStartAlertSound());
            return true;
        }

        @Override
        public boolean stopAlertSound() {
            sendCommand(ArsdkFeatureArdrone3.Sound.encodeStopAlertSound());
            return true;
        }
    };

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiBeeper(@NonNull DroneController droneController) {
        super(droneController);
        mBeeper = new BeeperCore(mComponentStore, mBackend);
    }

    @Override
    protected void onConnected() {
        mBeeper.publish();
    }

    @Override
    protected void onDisconnected() {
        mBeeper.unpublish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureArdrone3.SoundState.UID) {
            ArsdkFeatureArdrone3.SoundState.decode(command, mArdrone3SoundStateCallbacks);
        }
    }
}
