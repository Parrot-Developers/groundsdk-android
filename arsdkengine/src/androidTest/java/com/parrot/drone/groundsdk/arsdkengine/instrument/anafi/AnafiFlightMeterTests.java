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
import com.parrot.drone.groundsdk.device.instrument.FlightMeter;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiFlightMeterTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private FlightMeter mFlightMeter;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        setupDrone();
    }

    @Override
    protected void resetEngine() {
        super.resetEngine();
        setupDrone();
    }

    private void setupDrone() {
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mFlightMeter = mDrone.getInstrumentStore().get(mMockSession, FlightMeter.class);
        mDrone.getInstrumentStore().registerObserver(FlightMeter.class, () -> {
            mFlightMeter = mDrone.getInstrumentStore().get(mMockSession, FlightMeter.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the drone is not connected
        assertThat(mFlightMeter, is(nullValue()));

        connectDrone(mDrone, 1);
        assertThat(mFlightMeter, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectDrone(mDrone, 1);
        assertThat(mFlightMeter, is(nullValue()));
        assertThat(mChangeCnt, is(2));

        // should still be available when the drone is disconnected and something has been saved
        connectDrone(mDrone, 1);
        assertThat(mFlightMeter, is(notNullValue()));
        assertThat(mChangeCnt, is(3));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3SettingsStateMotorFlightsStatusChanged(1, 17, 125));
        assertThat(mChangeCnt, is(4));
        disconnectDrone(mDrone, 1);
        assertThat(mFlightMeter, is(notNullValue()));
        assertThat(mChangeCnt, is(4));

        // should be unavailable after forgetting the drone
        mDrone.forget();
        assertThat(mFlightMeter, is(nullValue()));
        assertThat(mChangeCnt, is(5));
    }

    @Test
    public void testValues() {
        connectDrone(mDrone, 1);

        // check default values
        assertThat(mFlightMeter.getLastFlightDuration(), is(0));
        assertThat(mFlightMeter.getTotalFlightDuration(), is(0L));
        assertThat(mFlightMeter.getTotalFlightCount(), is(0));
        assertThat(mChangeCnt, is(1));

        // check new received values
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3SettingsStateMotorFlightsStatusChanged(11, 784, 11473));
        assertThat(mFlightMeter.getLastFlightDuration(), is(784));
        assertThat(mFlightMeter.getTotalFlightDuration(), is(11473L));
        assertThat(mFlightMeter.getTotalFlightCount(), is(11));
        assertThat(mChangeCnt, is(2));

        // disconnect drone and reset engine
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check flight meter values still available
        assertThat(mFlightMeter.getLastFlightDuration(), is(784));
        assertThat(mFlightMeter.getTotalFlightDuration(), is(11473L));
        assertThat(mFlightMeter.getTotalFlightCount(), is(11));
        assertThat(mChangeCnt, is(0));
    }
}
