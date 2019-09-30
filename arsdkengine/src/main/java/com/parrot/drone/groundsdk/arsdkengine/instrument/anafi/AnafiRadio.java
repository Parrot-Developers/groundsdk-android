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
import com.parrot.drone.groundsdk.internal.device.instrument.RadioCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCommon;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureWifi;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.ulog.ULog;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG;

/** Radio instrument controller for Anafi family drones. */
public class AnafiRadio extends DroneInstrumentController {

    /** The radio instrument from which this object is the backend. */
    @NonNull
    private final RadioCore mRadio;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiRadio(@NonNull DroneController droneController) {
        super(droneController);
        mRadio = new RadioCore(mComponentStore);
    }

    @Override
    protected void onConnected() {
        mRadio.publish();
    }

    @Override
    protected void onDisconnected() {
        mRadio.unpublish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureWifi.UID) {
            ArsdkFeatureWifi.decode(command, mWifiCallback);
        } else if (featureId == ArsdkFeatureCommon.CommonState.UID) {
            // link quality is still on common commands
            ArsdkFeatureCommon.CommonState.decode(command, mCommonStateCallback);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureWifi is decoded. */
    private final ArsdkFeatureWifi.Callback mWifiCallback = new ArsdkFeatureWifi.Callback() {

        @Override
        public void onRssiChanged(int rssi) {
            mRadio.updateRssi(rssi).notifyUpdated();
        }
    };

    /** Callbacks called when a command of the feature ArsdkFeatureCommon.CommonState is decoded. */
    private final ArsdkFeatureCommon.CommonState.Callback mCommonStateCallback =
            new ArsdkFeatureCommon.CommonState.Callback() {

                @Override
                public void onLinkSignalQuality(int linkSignalQuality) {
                    int quality = linkSignalQuality & 0xF;
                    boolean perturbed = (linkSignalQuality & (1 << 7)) != 0;
                    boolean interferingWith4g = (linkSignalQuality & (1 << 6)) != 0;
                    // check range of link signal quality value
                    if (quality >= 1 && quality <= 5) {
                        mRadio.updateLinkSignalQuality(quality - 1);
                    } else if (ULog.d(TAG)) {
                        ULog.d(TAG, "onLinkSignalQuality out of range value [quality: " + quality + "]");
                    }
                    mRadio.updateLinkPerturbed(perturbed)
                          .update4GInterfering(interferingWith4g)
                          .notifyUpdated();
                }
            };
}
