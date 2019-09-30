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
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf.SmartTakeOffLandAction;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.TimeProvider;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingValueIs;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsEnabling;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingValueIs;
import static com.parrot.drone.groundsdk.OptionalDoubleSettingMatcher.optionalDoubleSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.OptionalDoubleSettingMatcher.optionalDoubleSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.OptionalDoubleSettingMatcher.optionalDoubleSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiManualPilotingItfTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private ManualCopterPilotingItf mPilotingItf;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mPilotingItf = mDrone.getPilotingItfStore().get(mMockSession, ManualCopterPilotingItf.class);
        mDrone.getPilotingItfStore().registerObserver(ManualCopterPilotingItf.class, () -> {
            mPilotingItf = mDrone.getPilotingItfStore().get(mMockSession, ManualCopterPilotingItf.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when the drone is not connected and not known
        assertThat(mPilotingItf, is(nullValue()));
        // connect the drone
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock setting reception so that piloting itf is persisted
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxTiltChanged(0, 0, 1)));
        // interface should be published
        assertThat(mPilotingItf, is(notNullValue()));
        assertThat(mChangeCnt, is(1));
        // disconnect the drone
        disconnectDrone(mDrone, 1);
        // interface should still be there, deactivated
        assertThat(mPilotingItf, is(notNullValue()));
        assertThat(mChangeCnt, is(2));
        // forget the drone
        mDrone.forget();
        // interface should be absent now
        assertThat(mPilotingItf, is(nullValue()));
        assertThat(mChangeCnt, is(3));
    }

    @Test
    public void testPilotingCmd() {
        MockPilotingCommandSeqNrGenerator seqNrGenerator = new MockPilotingCommandSeqNrGenerator();
        TimeProvider.setInstance(seqNrGenerator);

        connectDrone(mDrone, 1);

        // expect the piloting command loop to have started
        seqNrGenerator.syncTime();
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingPCMD(0, 0, 0, 0, 0, seqNrGenerator.next()), true));
        mMockArsdkCore.pollNoAckCommands(1, PilotingCommand.Encoder.class);

        // update pcmd value
        mPilotingItf.setRoll(2);
        mPilotingItf.setPitch(4);
        mPilotingItf.setYawRotationSpeed(6);
        mPilotingItf.setVerticalSpeed(8);
        seqNrGenerator.syncTime();
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingPCMD(1, 2, -4, 6, 8, seqNrGenerator.next()), true));
        mMockArsdkCore.pollNoAckCommands(1, PilotingCommand.Encoder.class);

        mPilotingItf.hover();
        seqNrGenerator.syncTime();
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingPCMD(0, 0, 0, 6, 8, seqNrGenerator.next()), true));
        mMockArsdkCore.pollNoAckCommands(1, PilotingCommand.Encoder.class);

        TimeProvider.resetDefault();
    }

    @Test
    public void testTakeOff() {
        connectDrone(mDrone, 1);
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingTakeOff()));
        mPilotingItf.takeOff();
    }

    @Test
    public void testLand() {
        connectDrone(mDrone, 1);
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingLanding()));
        mPilotingItf.land();
    }

    @Test
    public void testCanTakeOffCanLand() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock setting reception so that piloting itf is persisted
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxTiltChanged(0, 0, 1)));

        assertThat(mPilotingItf.canTakeOff(), is(false));
        assertThat(mPilotingItf.canLand(), is(false));

        // Landed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED));
        assertThat(mPilotingItf.canTakeOff(), is(true));
        assertThat(mPilotingItf.canLand(), is(false));

        // Motor ramping
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.MOTOR_RAMPING));
        assertThat(mPilotingItf.canTakeOff(), is(false));
        assertThat(mPilotingItf.canLand(), is(true));

        // User take off
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.USERTAKEOFF));
        assertThat(mPilotingItf.canTakeOff(), is(false));
        assertThat(mPilotingItf.canLand(), is(true));

        // Taking off
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.TAKINGOFF));
        assertThat(mPilotingItf.canTakeOff(), is(false));
        assertThat(mPilotingItf.canLand(), is(true));

        // Hovering
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.HOVERING));
        assertThat(mPilotingItf.canTakeOff(), is(false));
        assertThat(mPilotingItf.canLand(), is(true));

        // Flying
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING));
        assertThat(mPilotingItf.canTakeOff(), is(false));
        assertThat(mPilotingItf.canLand(), is(true));

        // Landing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDING));
        assertThat(mPilotingItf.canTakeOff(), is(true));
        assertThat(mPilotingItf.canLand(), is(false));

        // Emergency
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.EMERGENCY));
        assertThat(mPilotingItf.canTakeOff(), is(false));
        assertThat(mPilotingItf.canLand(), is(false));

        // Emergency landing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.EMERGENCY_LANDING));
        assertThat(mPilotingItf.canTakeOff(), is(false));
        assertThat(mPilotingItf.canLand(), is(false));

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mPilotingItf.canTakeOff(), is(false));
        assertThat(mPilotingItf.canLand(), is(false));
    }

    @Test
    public void testSmartTakeOffLand() {
        connectDrone(mDrone, 1);

        // move to landed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED));
        assertThat(mPilotingItf.getSmartTakeOffLandAction(), is(SmartTakeOffLandAction.TAKE_OFF));

        // take off
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingTakeOff()));
        mPilotingItf.smartTakeOffLand();

        // move to motor ramping
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.MOTOR_RAMPING));
        assertThat(mPilotingItf.getSmartTakeOffLandAction(), is(SmartTakeOffLandAction.LAND));

        // cancel take off with landing command
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingLanding()));
        mPilotingItf.smartTakeOffLand();

        // move to flying
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING));
        assertThat(mPilotingItf.getSmartTakeOffLandAction(), is(SmartTakeOffLandAction.LAND));

        // land
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingLanding()));
        mPilotingItf.smartTakeOffLand();

        // move to landed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED));
        // drone moving
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateMotionState(
                ArsdkFeatureArdrone3.PilotingstateMotionstateState.MOVING));
        assertThat(mPilotingItf.getSmartTakeOffLandAction(), is(SmartTakeOffLandAction.THROWN_TAKE_OFF));

        // thrown take off
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingUserTakeOff(1)));
        mPilotingItf.smartTakeOffLand();

        // move to user take off
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.USERTAKEOFF));
        assertThat(mPilotingItf.getSmartTakeOffLandAction(), is(SmartTakeOffLandAction.LAND));

        // move to hovering
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.HOVERING));
        assertThat(mPilotingItf.getSmartTakeOffLandAction(), is(SmartTakeOffLandAction.LAND));

        // land
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingLanding()));
        mPilotingItf.smartTakeOffLand();

        // move to landing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDING));
        assertThat(mPilotingItf.getSmartTakeOffLandAction(), is(SmartTakeOffLandAction.TAKE_OFF));

        // cancel landing with take off command
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingTakeOff()));
        mPilotingItf.smartTakeOffLand();
    }

    @Test
    public void testMaxPitchRollSetting() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // backend sends initial value
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxTiltChanged(1.2f, 0.5f, 2.5f));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getMaxPitchRoll(), doubleSettingIsUpToDateAt(0.5f, 1.2f, 2.5f));

        // user changes value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingSettingsMaxTilt(2.1f)));
        mPilotingItf.getMaxPitchRoll().setValue(2.1);
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getMaxPitchRoll(), doubleSettingIsUpdatingTo(0.5f, 2.1, 2.5f));

        // backend updates value
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxTiltChanged(2.1f, 0.5f, 2.5f));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getMaxPitchRoll(), doubleSettingIsUpToDateAt(0.5f, 2.1f, 2.5f));

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(5)); // manual piloting itf gets deactivated, hence this change
        // user changes value while disconnected
        mPilotingItf.getMaxPitchRoll().setValue(2.2);
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getMaxPitchRoll(), doubleSettingIsUpToDateAt(0.5f, 2.2, 2.5f));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                // received current drone setting differs from what is saved
                .commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxTiltChanged(1.2f, 1.1f, 2.6f))
                // connect should send the saved setting
                .expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingSettingsMaxTilt(2.2f), true)));
    }

    @Test
    public void testMaxPitchRollVelocitySetting() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // backend sends initial value
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3SpeedSettingsStateMaxPitchRollRotationSpeedChanged(5.2f, 3.3f, 9.9f));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getMaxPitchRollVelocity(), optionalDoubleSettingIsUpToDateAt(3.3f, 5.2f, 9.9f));

        // user changes value
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3SpeedSettingsMaxPitchRollRotationSpeed(6.6f), true));
        mPilotingItf.getMaxPitchRollVelocity().setValue(6.6);
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getMaxPitchRollVelocity(), optionalDoubleSettingIsUpdatingTo(3.3f, 6.6, 9.9f));

        // backend updates value
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3SpeedSettingsStateMaxPitchRollRotationSpeedChanged(6.6f, 3.3f, 9.9f));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getMaxPitchRollVelocity(), optionalDoubleSettingIsUpToDateAt(3.3f, 6.6f, 9.9f));

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(5)); // manual piloting itf gets deactivated, hence this change
        // user changes value while disconnected
        mPilotingItf.getMaxPitchRollVelocity().setValue(6.7);
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getMaxPitchRollVelocity(), optionalDoubleSettingIsUpToDateAt(3.3f, 6.7, 9.9f));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                // received current drone setting differs from what is saved
                .commandReceived(1, ArsdkEncoder.encodeArdrone3SpeedSettingsStateMaxPitchRollRotationSpeedChanged(
                        8.8f, 3.3f, 9.9f))
                // connect should send the saved setting
                .expect(new Expectation.Command(1,
                        ExpectedCmd.ardrone3SpeedSettingsMaxPitchRollRotationSpeed(6.7f), true)));
    }

    @Test
    public void testMaxVerticalSpeedSetting() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // backend sends initial value
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3SpeedSettingsStateMaxVerticalSpeedChanged(3.4f, 1.2f, 5.6f));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getMaxVerticalSpeed(), doubleSettingIsUpToDateAt(1.2f, 3.4f, 5.6f));

        // user changes value
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3SpeedSettingsMaxVerticalSpeed(4.3f), true));
        mPilotingItf.getMaxVerticalSpeed().setValue(4.3);
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getMaxVerticalSpeed(), doubleSettingIsUpdatingTo(1.2f, 4.3, 5.6f));

        // backend updates value
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3SpeedSettingsStateMaxVerticalSpeedChanged(4.3f, 1.2f, 5.6f));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getMaxVerticalSpeed(), doubleSettingIsUpToDateAt(1.2f, 4.3f, 5.6f));

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(5)); // manual piloting itf gets deactivated, hence this change
        // user changes value while disconnected
        mPilotingItf.getMaxVerticalSpeed().setValue(4.5);
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getMaxVerticalSpeed(), doubleSettingIsUpToDateAt(1.2f, 4.5, 5.6f));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                // received current drone setting differs from what is saved
                .commandReceived(1,
                        ArsdkEncoder.encodeArdrone3SpeedSettingsStateMaxVerticalSpeedChanged(2.1f, 1.2f, 5.6f))
                // connect should send the saved setting
                .expect(new Expectation.Command(1, ExpectedCmd.ardrone3SpeedSettingsMaxVerticalSpeed(4.5f), true)));
    }

    @Test
    public void testMaxYawSpeedSetting() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // backend sends initial value
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3SpeedSettingsStateMaxRotationSpeedChanged(2.2f, 1.1f, 4.4f));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getMaxYawRotationSpeed(), doubleSettingIsUpToDateAt(1.1f, 2.2f, 4.4f));

        // user changes value
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3SpeedSettingsMaxRotationSpeed(3.3f), true));
        mPilotingItf.getMaxYawRotationSpeed().setValue(3.3);
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getMaxYawRotationSpeed(), doubleSettingIsUpdatingTo(1.1f, 3.3, 4.4f));

        // backend updates value
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3SpeedSettingsStateMaxRotationSpeedChanged(3.3f, 1.1f, 4.4f));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getMaxYawRotationSpeed(), doubleSettingIsUpToDateAt(1.1f, 3.3f, 4.4f));

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(5)); // manual piloting itf gets deactivated, hence this change
        // user changes value while disconnected
        mPilotingItf.getMaxYawRotationSpeed().setValue(3.4);
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getMaxYawRotationSpeed(), doubleSettingIsUpToDateAt(1.1f, 3.4, 4.4f));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                // received current drone setting differs from what is saved
                .commandReceived(1,
                        ArsdkEncoder.encodeArdrone3SpeedSettingsStateMaxRotationSpeedChanged(4.3f, 1.1f, 4.4f))
                // connect should send the saved setting
                .expect(new Expectation.Command(1, ExpectedCmd.ardrone3SpeedSettingsMaxRotationSpeed(3.4f), true)));
    }

    @Test
    public void testBankedTurnMode() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // backend sends initial value
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateBankedTurnChanged(0));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getBankedTurnMode(), optionalBooleanSettingIsDisabled());

        // user changes value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingSettingsBankedTurn(1)));
        mPilotingItf.getBankedTurnMode().toggle();
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getBankedTurnMode(), optionalBooleanSettingIsEnabling());

        // backend updates value
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateBankedTurnChanged(1));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getBankedTurnMode(), optionalBooleanSettingIsEnabled());

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(5)); // manual piloting itf gets deactivated, hence this change
        // user changes value while disconnected
        mPilotingItf.getBankedTurnMode().toggle();
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getBankedTurnMode(), optionalBooleanSettingIsDisabled());

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                // received current drone setting differs from what is saved
                .commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingSettingsStateBankedTurnChanged(1))
                // connect should send the saved setting
                .expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingSettingsBankedTurn(0), true)));
    }

    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateBankedTurnChanged(0),
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMotionDetection(0),
                ArsdkEncoder.encodeArdrone3SpeedSettingsStateMaxPitchRollRotationSpeedChanged(0, 0, 1),
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxTiltChanged(0, 0, 1),
                ArsdkEncoder.encodeArdrone3SpeedSettingsStateMaxVerticalSpeedChanged(0, 0, 1),
                ArsdkEncoder.encodeArdrone3SpeedSettingsStateMaxRotationSpeedChanged(0, 0, 1)));

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(false),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(false),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getMaxPitchRollVelocity(), allOf(
                optionalDoubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getMaxPitchRoll(), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getMaxVerticalSpeed(), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getMaxYawRotationSpeed(), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));

        // mock user modifies settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingSettingsBankedTurn(1)));
        mPilotingItf.getBankedTurnMode().setEnabled(true);

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingSettingsSetMotionDetectionMode(1)));
        mPilotingItf.getThrownTakeOffMode().setEnabled(true);

        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3SpeedSettingsMaxPitchRollRotationSpeed(1)));
        mPilotingItf.getMaxPitchRollVelocity().setValue(1);

        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getMaxPitchRollVelocity(), allOf(
                optionalDoubleSettingValueIs(0, 1, 1),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingSettingsMaxTilt(1)));
        mPilotingItf.getMaxPitchRoll().setValue(1);

        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getMaxPitchRoll(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3SpeedSettingsMaxVerticalSpeed(1)));
        mPilotingItf.getMaxVerticalSpeed().setValue(1);

        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getMaxVerticalSpeed(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3SpeedSettingsMaxRotationSpeed(1)));
        mPilotingItf.getMaxYawRotationSpeed().setValue(1);

        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getMaxYawRotationSpeed(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpdating()));

        // disconnect
        disconnectDrone(mDrone, 1);

        // setting should be updated to user value
        assertThat(mChangeCnt, is(8));

        assertThat(mPilotingItf.getBankedTurnMode(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getThrownTakeOffMode(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getMaxPitchRollVelocity(), allOf(
                optionalDoubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getMaxPitchRoll(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getMaxVerticalSpeed(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getMaxYawRotationSpeed(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        // test other values are reset as they should
        assertThat(mPilotingItf.canLand(), is(false));
        assertThat(mPilotingItf.canTakeOff(), is(false));
        assertThat(mPilotingItf.getSmartTakeOffLandAction(), is(SmartTakeOffLandAction.NONE));
    }
}
