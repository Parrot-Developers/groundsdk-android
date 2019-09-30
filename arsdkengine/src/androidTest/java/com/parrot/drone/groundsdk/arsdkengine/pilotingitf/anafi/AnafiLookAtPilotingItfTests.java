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

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.MockPilotingCommandSeqNrGenerator;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.PilotingCommand;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.LookAtPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.tracking.TrackingIssue;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.TimeProvider;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureFollowMe;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import java.util.EnumSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiLookAtPilotingItfTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private LookAtPilotingItf mPilotingItf;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mPilotingItf = mDrone.getPilotingItfStore().get(mMockSession, LookAtPilotingItf.class);
        mDrone.getPilotingItfStore().registerObserver(LookAtPilotingItf.class, () -> {
            mPilotingItf = mDrone.getPilotingItfStore().get(mMockSession, LookAtPilotingItf.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when the drone is not connected
        assertThat(mChangeCnt, is(0));
        assertThat(mPilotingItf, is(nullValue()));

        // connect the drone
        connectDrone(mDrone, 1);

        // interface should be published
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf, is(notNullValue()));

        // disconnect the drone
        disconnectDrone(mDrone, 1);

        // interface should not be present
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf, is(nullValue()));
    }

    @Test
    public void testActivation() {
        // connect the drone
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                // mock drone landed
                .commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                        ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED))
                // and that image detection is missing
                .commandReceived(1, ArsdkEncoder.encodeFollowMeModeInfo(
                        ArsdkFeatureFollowMe.Mode.LOOK_AT,
                        missing(ArsdkFeatureFollowMe.Input.IMAGE_DETECTION),
                        noneMissing())));

        // interface should be unavailable
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.activate(), is(false));
        assertThat(mPilotingItf.deactivate(), is(false));

        // mock drone receiving valid image info for Geographic mode
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeModeInfo(
                ArsdkFeatureFollowMe.Mode.LOOK_AT,
                noneMissing(),
                noneMissing())
        );

        // interface should still be unavailable (not flying)
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.activate(), is(false));
        assertThat(mPilotingItf.deactivate(), is(false));

        // mock drone flying
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING));

        // interface should become idle
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));

        // activation should work
        assertThat(mPilotingItf.deactivate(), is(false));
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.followMeStart(ArsdkFeatureFollowMe.Mode.LOOK_AT)));
        assertThat(mPilotingItf.activate(), is(true));

        // mock activation ack
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeState(
                ArsdkFeatureFollowMe.Mode.LOOK_AT, ArsdkFeatureFollowMe.Behavior.LOOK_AT, null, 0));

        // interface should become active
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.activate(), is(false));

        // mock drone landed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED));

        // interface should become unavailable
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.activate(), is(false));
        assertThat(mPilotingItf.deactivate(), is(false));

        // mock drone flying
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING));

        // interface should become active
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.activate(), is(false));

        // deactivate
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.followMeStop()));
        assertThat(mPilotingItf.deactivate(), is(true));

        // mock deactivation ack
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeState(
                ArsdkFeatureFollowMe.Mode.NONE, ArsdkFeatureFollowMe.Behavior.IDLE, null, 0));

        // interface should become idle
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.deactivate(), is(false));

        // mock sudden unavailability
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeModeInfo(
                ArsdkFeatureFollowMe.Mode.LOOK_AT,
                missing(ArsdkFeatureFollowMe.Input.IMAGE_DETECTION),
                noneMissing()));

        // interface should become unavailable
        assertThat(mChangeCnt, is(8));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.activate(), is(false));
        assertThat(mPilotingItf.deactivate(), is(false));
    }

    @Test
    public void testIssues() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                // mock drone landed
                .commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                        ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED))
                // and that image detection is missing
                .commandReceived(1, ArsdkEncoder.encodeFollowMeModeInfo(
                        ArsdkFeatureFollowMe.Mode.LOOK_AT,
                        missing(ArsdkFeatureFollowMe.Input.IMAGE_DETECTION,
                                ArsdkFeatureFollowMe.Input.TARGET_GPS_GOOD_ACCURACY),
                        noneMissing())));

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getAvailabilityIssues(), containsInAnyOrder(
                TrackingIssue.DRONE_NOT_FLYING,
                TrackingIssue.TARGET_GPS_INFO_INACCURATE,
                TrackingIssue.TARGET_DETECTION_INFO_MISSING));
        assertThat(mPilotingItf.getQualityIssues(), empty());

        // mock image detection ok
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeModeInfo(
                ArsdkFeatureFollowMe.Mode.LOOK_AT,
                missing(ArsdkFeatureFollowMe.Input.TARGET_GPS_GOOD_ACCURACY),
                noneMissing()));

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getAvailabilityIssues(), containsInAnyOrder(
                TrackingIssue.DRONE_NOT_FLYING,
                TrackingIssue.TARGET_GPS_INFO_INACCURATE));
        assertThat(mPilotingItf.getQualityIssues(), empty());

        // mock target gps ok
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeModeInfo(
                ArsdkFeatureFollowMe.Mode.LOOK_AT,
                noneMissing(),
                noneMissing()));

        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getAvailabilityIssues(), containsInAnyOrder(
                TrackingIssue.DRONE_NOT_FLYING));
        assertThat(mPilotingItf.getQualityIssues(), empty());

        // mock flying
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING));

        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getAvailabilityIssues(), empty());
        assertThat(mPilotingItf.getQualityIssues(), empty());

        // mock drone not high enough + not far enough quality issue
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeModeInfo(
                ArsdkFeatureFollowMe.Mode.LOOK_AT,
                noneMissing(),
                missing(ArsdkFeatureFollowMe.Input.DRONE_FAR_ENOUGH,
                        ArsdkFeatureFollowMe.Input.DRONE_HIGH_ENOUGH)));

        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getAvailabilityIssues(), empty());
        assertThat(mPilotingItf.getQualityIssues(), containsInAnyOrder(
                TrackingIssue.DRONE_TOO_CLOSE_TO_TARGET,
                TrackingIssue.DRONE_TOO_CLOSE_TO_GROUND));

        // mock look at start
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeState(ArsdkFeatureFollowMe.Mode.LOOK_AT,
                ArsdkFeatureFollowMe.Behavior.LOOK_AT, null, 0));

        // quality issues should not change
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getAvailabilityIssues(), empty());
        assertThat(mPilotingItf.getQualityIssues(), containsInAnyOrder(
                TrackingIssue.DRONE_TOO_CLOSE_TO_TARGET,
                TrackingIssue.DRONE_TOO_CLOSE_TO_GROUND));

        // mock drone high enough
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeModeInfo(
                ArsdkFeatureFollowMe.Mode.LOOK_AT,
                noneMissing(),
                missing(ArsdkFeatureFollowMe.Input.DRONE_FAR_ENOUGH)));

        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getAvailabilityIssues(), empty());
        assertThat(mPilotingItf.getQualityIssues(), containsInAnyOrder(
                TrackingIssue.DRONE_TOO_CLOSE_TO_TARGET));

        // mock sudden unavailability
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeModeInfo(
                ArsdkFeatureFollowMe.Mode.LOOK_AT,
                missing(ArsdkFeatureFollowMe.Input.DRONE_GPS_GOOD_ACCURACY),
                missing(ArsdkFeatureFollowMe.Input.DRONE_FAR_ENOUGH)));

        // nothing should change until drone sends unavailable state
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getAvailabilityIssues(), empty());
        assertThat(mPilotingItf.getQualityIssues(), containsInAnyOrder(
                TrackingIssue.DRONE_TOO_CLOSE_TO_TARGET));

        // mock stopped state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeState(
                ArsdkFeatureFollowMe.Mode.LOOK_AT, ArsdkFeatureFollowMe.Behavior.IDLE, null, 0));

        assertThat(mChangeCnt, is(8));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getAvailabilityIssues(), containsInAnyOrder(
                TrackingIssue.DRONE_GPS_INFO_INACCURATE));
        assertThat(mPilotingItf.getQualityIssues(), empty());
    }

    @Test
    public void testPiloting() {
        MockPilotingCommandSeqNrGenerator seqNrGenerator = new MockPilotingCommandSeqNrGenerator();
        seqNrGenerator.syncTime();
        TimeProvider.setInstance(seqNrGenerator);

        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                        ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING))
                .commandReceived(1, ArsdkEncoder.encodeFollowMeState(ArsdkFeatureFollowMe.Mode.LOOK_AT,
                        ArsdkFeatureFollowMe.Behavior.LOOK_AT, null, 0)));

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));

        // piloting command loop should be started

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingPCMD(0, 0, 0, 0, 0, seqNrGenerator.next()), true));
        mMockArsdkCore.pollNoAckCommands(1, PilotingCommand.Encoder.class);
        mMockArsdkCore.assertNoExpectation();

        // change piloting command
        mPilotingItf.setPitch(2);
        mPilotingItf.setRoll(4);
        mPilotingItf.setVerticalSpeed(8);

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingPCMD(1, 4, -2, 0, 8, seqNrGenerator.next()), true));
        mMockArsdkCore.pollNoAckCommands(1, PilotingCommand.Encoder.class);
        mMockArsdkCore.assertNoExpectation();

        // mock stop from drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeState(ArsdkFeatureFollowMe.Mode.LOOK_AT,
                ArsdkFeatureFollowMe.Behavior.IDLE, null, 0));

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));

        // changing piloting command should do nothing
        mPilotingItf.setPitch(16);
        mPilotingItf.setRoll(32);
        mPilotingItf.setVerticalSpeed(64);

        // (note however that pcmd loop still runs because of manual piloting itf)
        seqNrGenerator.resetSequenceNumber(); // this is a proof that the pcmd loop was stopped
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingPCMD(0, 0, 0, 0, 0, seqNrGenerator.next()), true));
        mMockArsdkCore.pollNoAckCommands(1, PilotingCommand.Encoder.class);
        mMockArsdkCore.assertNoExpectation();

        TimeProvider.resetDefault();
    }

    private static int missing(@NonNull ArsdkFeatureFollowMe.Input input,
                               @NonNull ArsdkFeatureFollowMe.Input... others) {
        EnumSet<ArsdkFeatureFollowMe.Input> missing = EnumSet.complementOf(EnumSet.of(input, others));
        return ArsdkFeatureFollowMe.Input.toBitField(missing.toArray(new ArsdkFeatureFollowMe.Input[0]));
    }

    private static int noneMissing() {
        return ArsdkFeatureFollowMe.Input.toBitField(ArsdkFeatureFollowMe.Input.values());
    }
}
