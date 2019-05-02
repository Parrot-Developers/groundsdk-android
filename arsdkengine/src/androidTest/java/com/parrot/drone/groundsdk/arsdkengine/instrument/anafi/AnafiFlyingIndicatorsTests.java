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
import com.parrot.drone.groundsdk.device.instrument.FlyingIndicators;
import com.parrot.drone.groundsdk.device.instrument.FlyingIndicators.FlyingState;
import com.parrot.drone.groundsdk.device.instrument.FlyingIndicators.LandedState;
import com.parrot.drone.groundsdk.device.instrument.FlyingIndicators.State;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static com.parrot.drone.groundsdk.FlyingIndicatorMatcher.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiFlyingIndicatorsTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private FlyingIndicators mFlyingIndicators;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mFlyingIndicators = mDrone.getInstrumentStore().get(mMockSession, FlyingIndicators.class);
        mDrone.getInstrumentStore().registerObserver(FlyingIndicators.class, () -> {
            mFlyingIndicators = mDrone.getInstrumentStore().get(mMockSession, FlyingIndicators.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the drone is not connected
        assertThat(mFlyingIndicators, is(nullValue()));

        connectDrone(mDrone, 1);
        assertThat(mFlyingIndicators, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectDrone(mDrone, 1);
        assertThat(mFlyingIndicators, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testValue() {
        connectDrone(mDrone, 1);
        // check default values
        assertThat(mFlyingIndicators, is(State.LANDED, LandedState.IDLE, FlyingState.NONE));
        assertThat(mChangeCnt, is(1));

        // Landed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED));
        assertThat(mFlyingIndicators, is(State.LANDED, LandedState.IDLE, FlyingState.NONE));
        assertThat(mChangeCnt, is(1));

        // Taking Off
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.TAKINGOFF));
        assertThat(mFlyingIndicators, is(State.FLYING, LandedState.NONE, FlyingState.TAKING_OFF));
        assertThat(mChangeCnt, is(2));

        // Hovering
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.HOVERING));
        assertThat(mFlyingIndicators, is(State.FLYING, LandedState.NONE, FlyingState.WAITING));
        assertThat(mChangeCnt, is(3));

        // Flying
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING));
        assertThat(mFlyingIndicators, is(State.FLYING, LandedState.NONE, FlyingState.FLYING));
        assertThat(mChangeCnt, is(4));

        // Landing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDING));
        assertThat(mFlyingIndicators, is(State.FLYING, LandedState.NONE, FlyingState.LANDING));
        assertThat(mChangeCnt, is(5));

        // Emergency
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.EMERGENCY));
        assertThat(mFlyingIndicators, is(State.EMERGENCY, LandedState.NONE, FlyingState.NONE));
        assertThat(mChangeCnt, is(6));

        // User take off
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.USERTAKEOFF));
        assertThat(mFlyingIndicators, is(State.LANDED, LandedState.WAITING_USER_ACTION, FlyingState.NONE));
        assertThat(mChangeCnt, is(7));

        // Motor ramping
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.MOTOR_RAMPING));
        assertThat(mFlyingIndicators, is(State.LANDED, LandedState.MOTOR_RAMPING, FlyingState.NONE));
        assertThat(mChangeCnt, is(8));

        // Emergency landing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.EMERGENCY_LANDING));
        assertThat(mFlyingIndicators, is(State.EMERGENCY_LANDING, LandedState.NONE, FlyingState.NONE));
        assertThat(mChangeCnt, is(9));

        // back to idle
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED));
        assertThat(mFlyingIndicators, is(State.LANDED, LandedState.IDLE, FlyingState.NONE));
        assertThat(mChangeCnt, is(10));
    }
}
