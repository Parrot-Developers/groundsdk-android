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

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiFlightPlanPilotingItfNoOfflineSettingsTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private FlightPlanPilotingItf mPilotingItf;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        GroundSdkConfig.get().setOfflineSettingsMode(GroundSdkConfig.OfflineSettingsMode.OFF);
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mPilotingItf = mDrone.getPilotingItfStore().get(mMockSession, FlightPlanPilotingItf.class);
        mDrone.getPilotingItfStore().registerObserver(FlightPlanPilotingItf.class, () -> {
            mPilotingItf = mDrone.getPilotingItfStore().get(mMockSession, FlightPlanPilotingItf.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when the drone is not connected and not known
        assertThat(mPilotingItf, is(nullValue()));
        // connect the drone
        connectDrone(mDrone, 1);
        // interface should be published
        assertThat(mPilotingItf, is(notNullValue()));
        assertThat(mChangeCnt, is(1));
        // disconnect the drone
        disconnectDrone(mDrone, 1);
        // interface should be absent now
        assertThat(mPilotingItf, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }
}
