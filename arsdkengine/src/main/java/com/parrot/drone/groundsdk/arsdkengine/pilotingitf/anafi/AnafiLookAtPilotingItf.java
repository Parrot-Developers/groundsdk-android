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

package com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.PilotingItfActivationController;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.LookAtPilotingItfCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureFollowMe;

import java.util.EnumSet;

/** LookAt piloting interface controller for Anafi family drones. */
public class AnafiLookAtPilotingItf extends AnafiTrackingPilotingItfBase {

    /** Piloting interface for which this object is the backend. */
    @NonNull
    private final LookAtPilotingItfCore mPilotingItf;

    /**
     * Constructor.
     *
     * @param activationController activation controller that owns this piloting interface controller
     */
    public AnafiLookAtPilotingItf(@NonNull PilotingItfActivationController activationController) {
        super(activationController, EnumSet.of(ArsdkFeatureFollowMe.Mode.LOOK_AT));
        mPilotingItf = new LookAtPilotingItfCore(mComponentStore, new Backend());
    }

    @Override
    public void requestActivation() {
        super.requestActivation();
        sendCommand(ArsdkFeatureFollowMe.encodeStart(ArsdkFeatureFollowMe.Mode.LOOK_AT));
    }

    @NonNull
    @Override
    public LookAtPilotingItfCore getPilotingItf() {
        return mPilotingItf;
    }
}
