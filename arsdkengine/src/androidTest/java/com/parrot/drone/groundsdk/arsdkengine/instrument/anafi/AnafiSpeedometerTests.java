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

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.Speedometer;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static com.parrot.drone.groundsdk.OptionalDoubleMatcher.optionalDoubleValueIs;
import static com.parrot.drone.groundsdk.OptionalValueMatcher.optionalValueIsUnavailable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiSpeedometerTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private Speedometer mSpeedometer;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mSpeedometer = mDrone.getInstrumentStore().get(mMockSession, Speedometer.class);
        mDrone.getInstrumentStore().registerObserver(Speedometer.class, () -> {
            mSpeedometer = mDrone.getInstrumentStore().get(mMockSession, Speedometer.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the drone is not connected
        assertThat(mSpeedometer, is(nullValue()));

        connectDrone(mDrone, 1);
        assertThat(mSpeedometer, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectDrone(mDrone, 1);
        assertThat(mSpeedometer, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testValue() {
        connectDrone(mDrone, 1);

        // check default values
        assertThat(mSpeedometer.getGroundSpeed(), is(0.0));
        assertThat(mSpeedometer.getAirSpeed(), optionalValueIsUnavailable());
        assertThat(mChangeCnt, is(1));

        // check attitude and ground speed received
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAttitudeChanged(0.0F, 0.0F, 0.2F));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateSpeedChanged(1.2F, 3.4F, 5.6F));
        assertThat(mSpeedometer.getGroundSpeed(), is(Math.sqrt(Math.pow(1.2F, 2) + Math.pow(3.4F, 2))));
        assertThat(mSpeedometer.getNorthSpeed(), is((double) 1.2F));
        assertThat(mSpeedometer.getEastSpeed(), is((double) 3.4F));
        assertThat(mSpeedometer.getDownSpeed(), is((double) 5.6F));
        assertThat(mSpeedometer.getForwardSpeed(), is(Math.cos(0.2F) * 1.2F + Math.sin(0.2F) * 3.4F));
        assertThat(mSpeedometer.getRightSpeed(), is(-Math.sin(0.2F) * 1.2F + Math.cos(0.2F) * 3.4F));
        assertThat(mSpeedometer.getAirSpeed(), optionalValueIsUnavailable());
        assertThat(mChangeCnt, is(2));

        // check air speed received
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAirSpeedChanged(7.8F));
        assertThat(mSpeedometer.getGroundSpeed(), is(Math.sqrt(Math.pow(1.2F, 2) + Math.pow(3.4F, 2))));
        assertThat(mSpeedometer.getAirSpeed(), optionalDoubleValueIs(7.8F));
        assertThat(mChangeCnt, is(3));
    }
}
