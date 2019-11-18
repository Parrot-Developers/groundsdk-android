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
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.MockPilotingCommandSeqNrGenerator;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.PilotingCommand;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.PointOfInterestPilotingItf;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.TimeProvider;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import static com.parrot.drone.groundsdk.PointOfInterestMatcher.matchesDirective;
import static com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING;
import static com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiPointOfInterestPilotingItfTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private PointOfInterestPilotingItf mPilotingItf;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mPilotingItf = mDrone.getPilotingItfStore().get(mMockSession, PointOfInterestPilotingItf.class);
        mDrone.getPilotingItfStore().registerObserver(PointOfInterestPilotingItf.class, () -> {
            mPilotingItf = mDrone.getPilotingItfStore().get(mMockSession, PointOfInterestPilotingItf.class);
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
        // interface should be absent
        assertThat(mPilotingItf, is(nullValue()));
        assertThat(mChangeCnt, is(2));
        // forget the drone
        mDrone.forget();
        // interface should still be absent
        assertThat(mPilotingItf, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testPilotedPOI() {
        connectDrone(mDrone, 1);

        // should be inactive
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());

        // starting a piloted POI in unavailable state should not change anything
        mPilotingItf.start(0, 0, 0);
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());

        // drone is flying, piloting interface should stay unavailable
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(FLYING));
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());

        // drone tells that piloted POI is available, piloting interface should become idle
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOI(0.0f, 0.0f, 0.0f,
                ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus.AVAILABLE));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());

        // start a piloted POI, interface does not become active yet
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingStartPilotedPOI(10.0f, 2.5f, -5.0f), true));
        mPilotingItf.start(10.0f, 2.5f, -5.0f);
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());

        // interface not activated when piloted POI is pending on the drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOI(10.0f, 2.5f, -5.0f,
                ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus.PENDING));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());

        // interface activated when piloted POI is started on the drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOI(10.0f, 2.5f, -5.0f,
                ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus.RUNNING));
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), matchesDirective(10.0, 2.5, -5.0,
                PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL));

        // start a new piloted POI
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingStartPilotedPOI(1.0f, 2.0f, 3.0f), true));
        mPilotingItf.start(1.0f, 2.0f, 3.0f);
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), matchesDirective(10.0, 2.5, -5.0,
                PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL));

        // new POI started on the drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOI(1.0f, 2.0f, 3.0f,
                ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus.RUNNING));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), matchesDirective(1.0, 2.0, 3.0,
                PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL));

        // interface becomes unavailable as the drone is landed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(LANDED));
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());
    }

    @Test
    public void testPilotedPOIFreeGimbal() {
        connectDrone(mDrone, 1);

        // should be inactive
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());

        // drone tells that piloted POI is available, piloting interface should stay unavailable as drone is not flying
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOIV2(0.0f, 0.0f, 0.0f,
                        ArsdkFeatureArdrone3.PilotingstatePilotedpoiv2Mode.LOCKED_GIMBAL,
                        ArsdkFeatureArdrone3.PilotingstatePilotedpoiv2Status.AVAILABLE));
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());

        // drone is flying, piloting interface should now become idle
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(FLYING));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());

        // start a piloted POI with free gimbal, interface does not become active yet
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingStartPilotedPOIV2(10.0f, 2.5f, -5.0f,
                        ArsdkFeatureArdrone3.PilotingStartpilotedpoiv2Mode.FREE_GIMBAL), true));
        mPilotingItf.start(10.0f, 2.5f, -5.0f, PointOfInterestPilotingItf.Mode.FREE_GIMBAL);
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());

        // interface not activated when piloted POI is pending on the drone
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOIV2(10.0f, 2.5f, -5.0f,
                        ArsdkFeatureArdrone3.PilotingstatePilotedpoiv2Mode.FREE_GIMBAL,
                        ArsdkFeatureArdrone3.PilotingstatePilotedpoiv2Status.PENDING));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());

        // interface activated when piloted POI is started on the drone
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOIV2(10.0f, 2.5f, -5.0f,
                        ArsdkFeatureArdrone3.PilotingstatePilotedpoiv2Mode.FREE_GIMBAL,
                        ArsdkFeatureArdrone3.PilotingstatePilotedpoiv2Status.RUNNING));
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), matchesDirective(10.0, 2.5, -5.0,
                PointOfInterestPilotingItf.Mode.FREE_GIMBAL));

        // interface becomes unavailable as the drone is landed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(LANDED));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());
    }

    @Test
    public void testReconnectionWhenFlying() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(FLYING));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOI(0.0f, 0.0f, 0.0f,
                ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus.AVAILABLE));

        // should be idle
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf, is(nullValue()));

        // connect when drone is flying
        connectDrone(mDrone, 1, () -> {
            mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(FLYING));
            mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOI(0.0f, 0.0f, 0.0f,
                    ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus.AVAILABLE));
        });

        // should be idle
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), nullValue());
    }

    @Test
    public void testReconnectionWhenPOIRunning() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(FLYING));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOI(1.0f, 2.0f, 3.0f,
                ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus.RUNNING));

        // should be active
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), matchesDirective(1.0, 2.0, 3.0,
                PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL));

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf, is(nullValue()));

        // connect when drone is flying
        connectDrone(mDrone, 1, () -> {
            mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(FLYING));
            mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOI(1.0f, 2.0f, 3.0f,
                    ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus.RUNNING));
        });

        // should be active
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), matchesDirective(1.0, 2.0, 3.0,
                PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL));
    }

    @Test
    public void testPilotingCmd() {
        MockPilotingCommandSeqNrGenerator seqNrGenerator = new MockPilotingCommandSeqNrGenerator();
        TimeProvider.setInstance(seqNrGenerator);

        connectDrone(mDrone, 1);

        // should be inactive
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));

        // should be idle as the drone is flying and it tells that piloted POI is available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(FLYING));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOI(0.0f, 0.0f, 0.0f,
                ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus.AVAILABLE));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));

        // start a piloted POI, interface does not become active yet
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingStartPilotedPOI(10.0f, 2.5f, -5.0f), true));
        mPilotingItf.start(10.0f, 2.5f, -5.0f);
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));

        // interface activated when piloted POI is started on the drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePilotedPOI(10.0f, 2.5f, -5.0f,
                ArsdkFeatureArdrone3.PilotingstatePilotedpoiStatus.RUNNING));
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getCurrentPointOfInterest(), matchesDirective(10.0, 2.5, -5.0,
                PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL));

        // expect the piloting command loop to have started
        seqNrGenerator.syncTime();
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingPCMD(0, 0, 0, 0, 0, seqNrGenerator.next()), true));
        mMockArsdkCore.pollNoAckCommands(1, PilotingCommand.Encoder.class);

        // update pcmd value
        mPilotingItf.setRoll(2);
        mPilotingItf.setPitch(4);
        mPilotingItf.setVerticalSpeed(8);
        seqNrGenerator.syncTime();
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingPCMD(1, 2, -4, 0, 8, seqNrGenerator.next()), true));
        mMockArsdkCore.pollNoAckCommands(1, PilotingCommand.Encoder.class);

        TimeProvider.resetDefault();
    }
}
