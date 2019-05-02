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
import com.parrot.drone.groundsdk.device.instrument.Altimeter;
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

public class AnafiAltimeterTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private Altimeter mAltimeter;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mAltimeter = mDrone.getInstrumentStore().get(mMockSession, Altimeter.class);
        mDrone.getInstrumentStore().registerObserver(Altimeter.class, () -> {
            mAltimeter = mDrone.getInstrumentStore().get(mMockSession, Altimeter.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the drone is not connected
        assertThat(mAltimeter, is(nullValue()));

        connectDrone(mDrone, 1);
        assertThat(mAltimeter, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectDrone(mDrone, 1);
        assertThat(mAltimeter, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testValue() {
        connectDrone(mDrone, 1);
        // check default values
        assertThat(mAltimeter.getTakeOffRelativeAltitude(), is(0.0));
        assertThat(mAltimeter.getGroundRelativeAltitude(), optionalValueIsUnavailable());
        assertThat(mAltimeter.getAbsoluteAltitude(), optionalValueIsUnavailable());
        assertThat(mAltimeter.getVerticalSpeed(), is(0.0));
        assertThat(mChangeCnt, is(1));

        // check the take off relative altitude
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAltitudeChanged(1.2));
        assertThat(mAltimeter.getTakeOffRelativeAltitude(), is(1.2));
        assertThat(mAltimeter.getGroundRelativeAltitude(), optionalValueIsUnavailable());
        assertThat(mAltimeter.getAbsoluteAltitude(), optionalValueIsUnavailable());
        assertThat(mAltimeter.getVerticalSpeed(), is(0.0));
        assertThat(mChangeCnt, is(2));

        // check the absolute (sea-level relative) altitude. Use positionChanged command
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePositionChanged(1.2, 3.4, 5.6));
        assertThat(mAltimeter.getTakeOffRelativeAltitude(), is(1.2));
        assertThat(mAltimeter.getGroundRelativeAltitude(), optionalValueIsUnavailable());
        assertThat(mAltimeter.getAbsoluteAltitude(), optionalDoubleValueIs(5.6));
        assertThat(mAltimeter.getVerticalSpeed(), is(0.0));
        assertThat(mChangeCnt, is(3));

        // Use GpsLocationChanged command now, check absolute altitude changes
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingStateGpsLocationChanged(1.2, 3.4, 7.8, 0, 0, 0));
        assertThat(mAltimeter.getTakeOffRelativeAltitude(), is(1.2));
        assertThat(mAltimeter.getGroundRelativeAltitude(), optionalValueIsUnavailable());
        assertThat(mAltimeter.getAbsoluteAltitude(), optionalDoubleValueIs(7.8));
        assertThat(mAltimeter.getVerticalSpeed(), is(0.0));
        assertThat(mChangeCnt, is(4));

        // Use positionChanged command again, check absolute altitude DOES NOT change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePositionChanged(1.2, 3.4, 9.10));
        assertThat(mAltimeter.getTakeOffRelativeAltitude(), is(1.2));
        assertThat(mAltimeter.getGroundRelativeAltitude(), optionalValueIsUnavailable());
        assertThat(mAltimeter.getAbsoluteAltitude(), optionalDoubleValueIs(7.8));
        assertThat(mAltimeter.getVerticalSpeed(), is(0.0));
        assertThat(mChangeCnt, is(4));

        // check vertical speed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateSpeedChanged(0F, 0F, -3.4F));
        assertThat(mAltimeter.getTakeOffRelativeAltitude(), is(1.2));
        assertThat(mAltimeter.getGroundRelativeAltitude(), optionalValueIsUnavailable());
        assertThat(mAltimeter.getAbsoluteAltitude(), optionalDoubleValueIs(7.8));
        assertThat(mAltimeter.getVerticalSpeed(), is((double) 3.4F));
        assertThat(mChangeCnt, is(5));
    }
}
