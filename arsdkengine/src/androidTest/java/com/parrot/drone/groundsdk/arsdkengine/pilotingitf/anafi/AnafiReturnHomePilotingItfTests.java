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
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.ReturnHomePilotingItf;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureRth;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import java.util.EnumSet;

import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabling;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabling;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingValueIs;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingSupports;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.IntSettingMatcher.intSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.IntSettingMatcher.intSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.IntSettingMatcher.intSettingValueIs;
import static com.parrot.drone.groundsdk.LocationMatcher.locationIs;
import static com.parrot.drone.groundsdk.OptionalDoubleSettingMatcher.optionalDoubleSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.OptionalDoubleSettingMatcher.optionalDoubleSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.OptionalSettingMatcher.optionalSettingIsUnavailable;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiReturnHomePilotingItfTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private ReturnHomePilotingItf mPilotingItf;

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

        mPilotingItf = mDrone.getPilotingItfStore().get(mMockSession, ReturnHomePilotingItf.class);
        mDrone.getPilotingItfStore().registerObserver(ReturnHomePilotingItf.class, () -> {
            mPilotingItf = mDrone.getPilotingItfStore().get(mMockSession, ReturnHomePilotingItf.class);
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
                ArsdkEncoder.encodeRthDelay(0, 0, 1)));
        // interface should be published
        assertThat(mPilotingItf, is(notNullValue()));
        assertThat(mChangeCnt, is(1));
        // disconnect the drone
        disconnectDrone(mDrone, 1);
        // interface should still be there, deactivated
        assertThat(mPilotingItf, is(notNullValue()));
        assertThat(mChangeCnt, is(1));
        // forget the drone
        mDrone.forget();
        // interface should be absent now
        assertThat(mPilotingItf, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testActivation() {
        connectDrone(mDrone, 1);

        // should be inactive
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mChangeCnt, is(1));

        // return home available
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthState(
                ArsdkFeatureRth.State.AVAILABLE,
                ArsdkFeatureRth.StateReason.ENABLED));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getReason(), is(ReturnHomePilotingItf.Reason.NONE));
        assertThat(mChangeCnt, is(2));

        // activate return home
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingNavigateHome(1)));
        mPilotingItf.activate();
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthState(
                ArsdkFeatureRth.State.IN_PROGRESS,
                ArsdkFeatureRth.StateReason.USER_REQUEST));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getReason(), is(ReturnHomePilotingItf.Reason.USER_REQUESTED));
        assertThat(mChangeCnt, is(3));

        // deactivate return home
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingNavigateHome(0)));
        mPilotingItf.deactivate();
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthState(
                ArsdkFeatureRth.State.AVAILABLE,
                ArsdkFeatureRth.StateReason.USER_REQUEST));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getReason(), is(ReturnHomePilotingItf.Reason.NONE));
        assertThat(mChangeCnt, is(4));

        // activate return home
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingNavigateHome(1)));
        mPilotingItf.activate();
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthState(
                ArsdkFeatureRth.State.IN_PROGRESS,
                ArsdkFeatureRth.StateReason.USER_REQUEST));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getReason(), is(ReturnHomePilotingItf.Reason.USER_REQUESTED));
        assertThat(mChangeCnt, is(5));

        // return home finished
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingNavigateHome(0)));
        mPilotingItf.deactivate();
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthState(
                ArsdkFeatureRth.State.AVAILABLE,
                ArsdkFeatureRth.StateReason.FINISHED));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getReason(), is(ReturnHomePilotingItf.Reason.FINISHED));
        assertThat(mChangeCnt, is(6));

        // return home unavailable
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthState(
                ArsdkFeatureRth.State.UNAVAILABLE,
                ArsdkFeatureRth.StateReason.USER_REQUEST));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getReason(), is(ReturnHomePilotingItf.Reason.NONE));
        assertThat(mChangeCnt, is(7));
    }

    @Test
    public void testCurrentTarget() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));

        // ControllerPosition
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthHomeType(
                ArsdkFeatureRth.HomeType.PILOT));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getCurrentTarget(), is(ReturnHomePilotingItf.Target.CONTROLLER_POSITION));

        // TakeOffPosition
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthHomeType(
                ArsdkFeatureRth.HomeType.TAKEOFF));
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getCurrentTarget(), is(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));

        // Followee
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthHomeType(
                ArsdkFeatureRth.HomeType.FOLLOWEE));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getCurrentTarget(), is(ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION));

        // None
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthHomeType(
                ArsdkFeatureRth.HomeType.NONE));
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getCurrentTarget(), is(ReturnHomePilotingItf.Target.NONE));

        // TakeOffPosition
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthHomeType(
                ArsdkFeatureRth.HomeType.CUSTOM));
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getCurrentTarget(), is(ReturnHomePilotingItf.Target.CUSTOM_LOCATION));
    }

    @Test
    public void testCustomLocation() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock setting reception so that piloting itf is persisted
                ArsdkEncoder.encodeRthDelay(0, 0, 1)));

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getHomeLocation(), nullValue());

        // Ensure that position (500, 500) is ignored
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthCustomLocation(500, 500, 10));
        assertThat(mPilotingItf.getHomeLocation(), nullValue());

        // Drone sends custom location
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthCustomLocation(20, 30, 150));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getHomeLocation(), locationIs(20, 30, 150));

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mPilotingItf.getHomeLocation(), nullValue());
    }

    @Test
    public void testTakeOffLocation() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock setting reception so that piloting itf is persisted
                ArsdkEncoder.encodeRthDelay(0, 0, 1)));

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getHomeLocation(), nullValue());
        assertThat(mPilotingItf.gpsWasFixedOnTakeOff(), is(false));

        // Ensure that position (500, 500) is ignored
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeRthTakeoffLocation(500, 500,10, 1));
        assertThat(mPilotingItf.getHomeLocation(), nullValue());
        assertThat(mPilotingItf.gpsWasFixedOnTakeOff(), is(false));
        assertThat(mChangeCnt, is(1));

        // Drone sends home location
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthTakeoffLocation(20, 30, 150, 0));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getHomeLocation(), locationIs(20, 30, 150));
        assertThat(mPilotingItf.gpsWasFixedOnTakeOff(), is(false));

        // Drone sends home location
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthTakeoffLocation(20, 30, 150, 1));
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getHomeLocation(), locationIs(20, 30, 150));
        assertThat(mPilotingItf.gpsWasFixedOnTakeOff(), is(true));

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mPilotingItf.getHomeLocation(), nullValue());
    }

    @Test
    public void testFolloweeLocation() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock setting reception so that piloting itf is persisted
                ArsdkEncoder.encodeRthDelay(0, 0, 1)));

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getHomeLocation(), nullValue());

        // Ensure that position (500, 500) is ignored
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthFolloweeLocation(500, 500, 10));
        assertThat(mPilotingItf.getHomeLocation(), nullValue());

        // Drone sends followee location
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthFolloweeLocation(20, 30, 150));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getHomeLocation(), locationIs(20, 30, 150));

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mPilotingItf.getHomeLocation(), nullValue());
    }

    @Test
    public void testAutoTriggerSwitchState() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock setting reception so that piloting itf is persisted
                ArsdkEncoder.encodeRthDelay(0, 0, 1)));
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.autoTrigger(),
                booleanSettingValueIs(false));

        // backend change to enabled
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthAutoTriggerMode(
                ArsdkFeatureRth.AutoTriggerMode.ON));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.autoTrigger(), booleanSettingValueIs(true));

        // backend change to disabled
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthAutoTriggerMode(
                ArsdkFeatureRth.AutoTriggerMode.OFF));
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.autoTrigger(), booleanSettingValueIs(false));

        // user change to enabled
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.rthSetAutoTriggerMode(ArsdkFeatureRth.AutoTriggerMode.ON)));
        mPilotingItf.autoTrigger().toggle();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.autoTrigger(), booleanSettingIsEnabling());

        // backend updates auto trigger switch state
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeRthAutoTriggerMode(ArsdkFeatureRth.AutoTriggerMode.ON));
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.autoTrigger(), booleanSettingValueIs(true));

        // user change to disabled
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.rthSetAutoTriggerMode(ArsdkFeatureRth.AutoTriggerMode.OFF)));
        mPilotingItf.autoTrigger().toggle();
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.autoTrigger(), booleanSettingIsDisabling());

        // backend updates auto trigger switch state
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeRthAutoTriggerMode(ArsdkFeatureRth.AutoTriggerMode.OFF));
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.autoTrigger(), booleanSettingValueIs(false));

        // disconnect
        disconnectDrone(mDrone, 1);

        // user change while disconnected
        mPilotingItf.autoTrigger().toggle();
        assertThat(mChangeCnt, is(8));
        // reconnect
        connectDrone(mDrone, 1, () -> {
            // received current drone setting differs from what is saved
            mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthAutoTriggerMode(
                    ArsdkFeatureRth.AutoTriggerMode.OFF));
            // connect should send the saved setting
            mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetAutoTriggerMode(
                    ArsdkFeatureRth.AutoTriggerMode.ON), true));
        });
    }

    @Test
    public void testPreferredTarget() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock setting reception so that piloting itf is persisted
                ArsdkEncoder.encodeRthDelay(0, 0, 1)));
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpToDateAt(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));

        // backend change to Controller Position
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthPreferredHomeType(
                ArsdkFeatureRth.HomeType.PILOT));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpToDateAt(ReturnHomePilotingItf.Target.CONTROLLER_POSITION));

        // backend change to TakeOff Position
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthPreferredHomeType(
                ArsdkFeatureRth.HomeType.TAKEOFF));
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpToDateAt(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));

        // user change to Controller Position
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetPreferredHomeType(
                ArsdkFeatureRth.HomeType.PILOT), true));
        mPilotingItf.getPreferredTarget().setValue(ReturnHomePilotingItf.Target.CONTROLLER_POSITION);
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpdatingTo(ReturnHomePilotingItf.Target.CONTROLLER_POSITION));

        // backend updates preferred target
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthPreferredHomeType(
                ArsdkFeatureRth.HomeType.PILOT));
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpToDateAt(ReturnHomePilotingItf.Target.CONTROLLER_POSITION));

        // User changes to TakeOff Position
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetPreferredHomeType(
                ArsdkFeatureRth.HomeType.TAKEOFF), true));
        mPilotingItf.getPreferredTarget().setValue(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION);
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpdatingTo(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));

        // backend updates preferred target
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthPreferredHomeType(
                ArsdkFeatureRth.HomeType.TAKEOFF));
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpToDateAt(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));

        // User changes to tracked target position
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetPreferredHomeType(
                ArsdkFeatureRth.HomeType.FOLLOWEE), true));
        mPilotingItf.getPreferredTarget().setValue(ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION);
        assertThat(mChangeCnt, is(8));
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpdatingTo(ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION));

        // backend updates preferred target
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthPreferredHomeType(
                ArsdkFeatureRth.HomeType.FOLLOWEE));
        assertThat(mChangeCnt, is(9));
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpToDateAt(ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION));

        // backend updates supported home type capabilities
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthHomeTypeCapabilities(
                ArsdkFeatureRth.HomeType.toBitField(
                        ArsdkFeatureRth.HomeType.TAKEOFF,
                        ArsdkFeatureRth.HomeType.PILOT,
                        ArsdkFeatureRth.HomeType.FOLLOWEE
                )));
        assertThat(mChangeCnt, is(10));
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingSupports(EnumSet.of(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION,
                        ReturnHomePilotingItf.Target.CONTROLLER_POSITION,
                        ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION)));
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingValueIs(ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION));

        // user changes to unsupported target has no effect
        mPilotingItf.getPreferredTarget().setValue(ReturnHomePilotingItf.Target.CUSTOM_LOCATION);

        assertThat(mChangeCnt, is(10));

        // user changes to supported target
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.rthSetPreferredHomeType(ArsdkFeatureRth.HomeType.TAKEOFF)));
        mPilotingItf.getPreferredTarget().setValue(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION);
        assertThat(mChangeCnt, is(11));

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(12));

        // user change while disconnected
        mPilotingItf.getPreferredTarget().setValue(ReturnHomePilotingItf.Target.CONTROLLER_POSITION);
        assertThat(mChangeCnt, is(13));
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpToDateAt(ReturnHomePilotingItf.Target.CONTROLLER_POSITION));

        // reconnect
        connectDrone(mDrone, 1, () -> {
            // received current drone setting differs from what is saved
            mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthPreferredHomeType(
                    ArsdkFeatureRth.HomeType.TAKEOFF));
            // connect should send the saved setting
            mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetPreferredHomeType(
                    ArsdkFeatureRth.HomeType.PILOT), true));
        });

        // disconnect
        disconnectDrone(mDrone, 1);

        // reconnect
        connectDrone(mDrone, 1, () -> {
            // connect should send the saved setting if no setting is received from the drone
            mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetPreferredHomeType(
                    ArsdkFeatureRth.HomeType.PILOT), true));
        });
    }

    @Test
    public void testEndingBehavior() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock setting reception so that piloting itf is persisted
                ArsdkEncoder.encodeRthDelay(0, 0, 1)));
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getEndingBehavior(),
                enumSettingValueIs(ReturnHomePilotingItf.EndingBehavior.HOVERING));

        // backend change to landing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthEndingBehavior(
                ArsdkFeatureRth.EndingBehavior.LANDING));
        assertThat(mChangeCnt, is(2));

        // backend change to hovering
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthEndingBehavior(
                ArsdkFeatureRth.EndingBehavior.HOVERING));
        assertThat(mChangeCnt, is(3));

        // user change to landing
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.rthSetEndingBehavior(ArsdkFeatureRth.EndingBehavior.LANDING)));
        mPilotingItf.getEndingBehavior().setValue(ReturnHomePilotingItf.EndingBehavior.LANDING);
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getEndingBehavior(),
                enumSettingIsUpdatingTo(ReturnHomePilotingItf.EndingBehavior.LANDING));

        // backend updates the ending behaviour
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthEndingBehavior(
                ArsdkFeatureRth.EndingBehavior.LANDING));
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getEndingBehavior(),
                enumSettingIsUpToDateAt(ReturnHomePilotingItf.EndingBehavior.LANDING));

        // user change to hovering
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.rthSetEndingBehavior(ArsdkFeatureRth.EndingBehavior.HOVERING)));
        mPilotingItf.getEndingBehavior().setValue(ReturnHomePilotingItf.EndingBehavior.HOVERING);
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getEndingBehavior(),
                enumSettingIsUpdatingTo(ReturnHomePilotingItf.EndingBehavior.HOVERING));

        // backend updates the ending behaviour
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthEndingBehavior(
                ArsdkFeatureRth.EndingBehavior.HOVERING));
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getEndingBehavior(),
                enumSettingIsUpToDateAt(ReturnHomePilotingItf.EndingBehavior.HOVERING));

        // disconnect
        disconnectDrone(mDrone, 1);

        // user change while disconnected
        mPilotingItf.getEndingBehavior().setValue(ReturnHomePilotingItf.EndingBehavior.LANDING);
        assertThat(mChangeCnt, is(8));
        assertThat(mPilotingItf.getEndingBehavior(),
                enumSettingIsUpToDateAt(ReturnHomePilotingItf.EndingBehavior.LANDING));

        // reconnect
        connectDrone(mDrone, 1, () -> {
            // received current drone setting differs from what is saved
            mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthEndingBehavior(
                    ArsdkFeatureRth.EndingBehavior.HOVERING));
            // connect should send the saved setting
            mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetEndingBehavior(
                    ArsdkFeatureRth.EndingBehavior.LANDING), true));
        });
    }

    @Test
    public void testHomeReachability() {
        connectDrone(mDrone, 1);

        // initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.UNKNOWN));

        // reachable
        mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeRthHomeReachability(ArsdkFeatureRth.HomeReachability.REACHABLE));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.REACHABLE));

        // not reachable
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeRthHomeReachability(ArsdkFeatureRth.HomeReachability.NOT_REACHABLE));
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.NOT_REACHABLE));

        // critical
        mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeRthHomeReachability(ArsdkFeatureRth.HomeReachability.CRITICAL));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.CRITICAL));
    }

    @Test
    public void testWarningPlannedReturn() {
        connectDrone(mDrone, 1);

        // initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.UNKNOWN));
        assertThat(mPilotingItf.getAutoTriggerDelay(), is(0L));

        // auto trigger in 1 minutes
        mMockArsdkCore.commandReceived(1, ArsdkEncoder
                .encodeRthRthAutoTrigger(ArsdkFeatureRth.AutoTriggerReason.BATTERY_CRITICAL_SOON, 60));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.WARNING));
        assertThat(mPilotingItf.getAutoTriggerDelay(), is(60L));

        // a reachability value changed, but the drone is still in auto triggering
        mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeRthHomeReachability(ArsdkFeatureRth.HomeReachability.CRITICAL));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.WARNING));
        assertThat(mPilotingItf.getAutoTriggerDelay(), is(60L));

        // stop auto trigger
        mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeRthRthAutoTrigger(ArsdkFeatureRth.AutoTriggerReason.NONE, 0));
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.CRITICAL));
        assertThat(mPilotingItf.getAutoTriggerDelay(), is(0L));

        // auto trigger again in 30 seconds
        mMockArsdkCore.commandReceived(1, ArsdkEncoder
                .encodeRthRthAutoTrigger(ArsdkFeatureRth.AutoTriggerReason.BATTERY_CRITICAL_SOON, 30));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.WARNING));
        assertThat(mPilotingItf.getAutoTriggerDelay(), is(30L));

        // start a RTH
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthState(
                ArsdkFeatureRth.State.IN_PROGRESS,
                ArsdkFeatureRth.StateReason.LOW_BATTERY));
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        // assert that the automatic trigger delay is 0
        assertThat(mPilotingItf.getAutoTriggerDelay(), is(0L));
        // assert that we remove the ".warning" reachability and back to the previous one
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.CRITICAL));

        // stop RTH
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthState(
                ArsdkFeatureRth.State.AVAILABLE,
                ArsdkFeatureRth.StateReason.USER_REQUEST));
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        // assert that the automatic trigger delay is 0
        assertThat(mPilotingItf.getAutoTriggerDelay(), is(0L));
        // reachability does not change
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.CRITICAL));
    }

    @Test
    public void testAutoStartOnDisconnectDelay() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), intSettingIsUpToDateAt(0, 0, 0));

        // backend changes delay
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthDelay(30, 0, 120));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), intSettingIsUpToDateAt(0, 30, 120));

        // user changes delay
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetDelay(92)));
        mPilotingItf.getAutoStartOnDisconnectDelay().setValue(92);
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), intSettingIsUpdatingTo(0, 92, 120));

        // backend updates delay
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthDelay(93, 0, 120));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), intSettingIsUpToDateAt(0, 93, 120));

        // disconnect
        disconnectDrone(mDrone, 1);
        // user change while disconnected
        mPilotingItf.getAutoStartOnDisconnectDelay().setValue(101);
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), intSettingIsUpToDateAt(0, 101, 120));

        // reconnect
        connectDrone(mDrone, 1, () -> {
            // received current drone setting differs from what is saved
            mMockArsdkCore.commandReceived(1,
                    ArsdkEncoder.encodeRthDelay(30, 0, 120));
            // connect should send the saved setting
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.rthSetDelay(101), true));
        });
    }

    @Test
    public void testHoveringAltitude() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalSettingIsUnavailable());

        // backend change hovering altitude
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthEndingHoveringAltitude(40, 10, 50));

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpToDateAt(10, 40, 50));

        // user change hovering altitude
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetEndingHoveringAltitude(30)));
        mPilotingItf.getEndingHoveringAltitude().setValue(30);
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpdatingTo(10, 30, 50));

        // backend updates hovering altitude
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeRthEndingHoveringAltitude(30, 10, 50));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpToDateAt(10, 30, 50));

        // disconnect
        disconnectDrone(mDrone, 1);

        // check setting is restored to latest user set value
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpToDateAt(10, 30, 50));

        // user change while disconnected
        mPilotingItf.getEndingHoveringAltitude().setValue(45);
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpToDateAt(10, 45, 50));

        // reconnect
        connectDrone(mDrone, 1, () -> {
            // received current drone setting differs from what is saved
            mMockArsdkCore.commandReceived(1,
                    ArsdkEncoder.encodeRthEndingHoveringAltitude(30, 10, 100));
            // connect should send the saved setting
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.rthSetEndingHoveringAltitude(45), true));
        });

        // check range is updated but value is not
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpToDateAt(10, 45, 100));
    }

    @Test
    public void testMinAltitude() {
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getMinAltitude(), optionalSettingIsUnavailable());

        // backend changes value
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeRthMinAltitude(20, 10, 50));
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpToDateAt(10, 20, 50));

        // user changes value
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.rthSetMinAltitude(30)));
        mPilotingItf.getMinAltitude().setValue(30);
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpdatingTo(10, 30, 50));

        // backend updates value
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeRthMinAltitude(32, 10, 50));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpToDateAt(10, 32, 50));

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check setting is restored to latest user set value
        assertThat(mChangeCnt, is(0));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpToDateAt(10, 30, 50));

        // user change while disconnected
        mPilotingItf.getMinAltitude().setValue(40);
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpToDateAt(10, 40, 50));

        // reconnect
        connectDrone(mDrone, 1, () -> {
            // received current drone setting differs from what is saved
            mMockArsdkCore.commandReceived(1,
                    ArsdkEncoder.encodeRthMinAltitude(32, 10, 100));
            // connect should send the saved setting
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.rthSetMinAltitude(40), true));
        });

        // check range is updated but value is not
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpToDateAt(10, 40, 100));
    }

    @Test
    public void testCancelAutoTrigger() {
        connectDrone(mDrone, 1);

        // initial home reachability value
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.UNKNOWN));

        // cancelling when no auto trigger is planned should not do anything
        mPilotingItf.cancelAutoTrigger();

        // Mock reception of home reachability warning (i.e. auto trigger is planned)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder
                .encodeRthRthAutoTrigger(ArsdkFeatureRth.AutoTriggerReason.BATTERY_CRITICAL_SOON, 60));
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.WARNING));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthCancelAutoTrigger()));
        mPilotingItf.cancelAutoTrigger();
    }

    @Test
    public void testSetCustomLocation() {
        connectDrone(mDrone, 1);

        // initial preferred target value
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingValueIs(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));

        // setting custom location when preferred target is not custom shouldn't do anything
        mPilotingItf.setCustomLocation(12.0, 13.0, 14.0);

        // set preferred target to custom location
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetPreferredHomeType(
                ArsdkFeatureRth.HomeType.CUSTOM)));
        mPilotingItf.getPreferredTarget().setValue(ReturnHomePilotingItf.Target.CUSTOM_LOCATION);
        assertThat(mPilotingItf.getPreferredTarget(), enumSettingValueIs(ReturnHomePilotingItf.Target.CUSTOM_LOCATION));

        // setting custom location when preferred target is custom sends the new locations to the drone
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetCustomLocation(12.0, 13.0, 14F)));
        mPilotingItf.setCustomLocation(12.0, 13.0, 14.0);
    }

    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeRthDelay(0, 0, 120),
                ArsdkEncoder.encodeRthMinAltitude(20, 10, 50),
                ArsdkEncoder.encodeRthHomeType(ArsdkFeatureRth.HomeType.FOLLOWEE),
                ArsdkEncoder.encodeRthTakeoffLocation(2, 2, 0, 1)));

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), allOf(
                intSettingValueIs(0, 0, 120),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpToDateAt(10, 20, 50));
        assertThat(mPilotingItf.getPreferredTarget(), allOf(
                enumSettingValueIs(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION),
                settingIsUpToDate()));
        assertThat(mPilotingItf.gpsWasFixedOnTakeOff(), is(true));


        // mock user modifies settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetDelay(1)));
        mPilotingItf.getAutoStartOnDisconnectDelay().setValue(1);

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), allOf(
                intSettingValueIs(0, 1, 120),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.rthSetMinAltitude(30)));
        mPilotingItf.getMinAltitude().setValue(30);

        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpdatingTo(10, 30, 50));

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.rthSetPreferredHomeType(ArsdkFeatureRth.HomeType.PILOT)));
        mPilotingItf.getPreferredTarget().setValue(ReturnHomePilotingItf.Target.CONTROLLER_POSITION);

        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getPreferredTarget(), allOf(
                enumSettingValueIs(ReturnHomePilotingItf.Target.CONTROLLER_POSITION),
                settingIsUpdating()));

        // disconnect
        disconnectDrone(mDrone, 1);

        // setting should be updated to user value
        assertThat(mChangeCnt, is(5));

        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), allOf(
                intSettingValueIs(0, 1, 120),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpToDateAt(10, 30, 50));
        assertThat(mPilotingItf.getPreferredTarget(), allOf(
                enumSettingValueIs(ReturnHomePilotingItf.Target.CONTROLLER_POSITION),
                settingIsUpToDate()));

        // test other values are reset as they should
        assertThat(mPilotingItf.getCurrentTarget(), is(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));
        assertThat(mPilotingItf.gpsWasFixedOnTakeOff(), is(false));
        assertThat(mPilotingItf.getHomeLocation(), nullValue());
    }
}
