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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.gimbal;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Gimbal;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGimbal;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGimbal.Axis;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGimbal.ControlMode;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGimbal.FrameOfReference;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import org.junit.Test;

import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabling;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabling;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingValueIs;
import static com.parrot.drone.groundsdk.DoubleRangeMatcher.doubleRangeIs;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiGimbalTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private Gimbal mGimbal;

    private int mChangeCnt;

    private Gimbal.CalibrationProcessState mExpectedCalibrationState;

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

        mGimbal = mDrone.getPeripheralStore().get(mMockSession, Gimbal.class);
        mDrone.getPeripheralStore().registerObserver(Gimbal.class, () -> {
            mGimbal = mDrone.getPeripheralStore().get(mMockSession, Gimbal.class);
            mChangeCnt++;

            if (mExpectedCalibrationState != null) {
                // check if current calibration state matches the expected state
                assertThat(mGimbal, notNullValue());
                assertThat(mGimbal.getCalibrationProcessState(), is(mExpectedCalibrationState));
                mExpectedCalibrationState = null;
            }
        });

        mChangeCnt = 0;
    }

    private static ArsdkCommand encodeCapabilities(Axis... axes) {
        return ArsdkEncoder.encodeGimbalGimbalCapabilities(0, ArsdkFeatureGimbal.Model.MAIN,
                Axis.toBitField(axes));
    }

    @Test
    public void testPublication() {
        // should be unavailable when drone has never been connected
        assertThat(mChangeCnt, is(0));
        assertThat(mGimbal, nullValue());

        // connect drone, mocking receiving online only parameters, so something changes on disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.PITCH, Axis.ROLL),
                ArsdkEncoder.encodeGimbalAxisLockState(0, Axis.toBitField(Axis.PITCH))));

        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(2)); // +1 because online-only settings have been reset
        assertThat(mGimbal, notNullValue());

        // forget drone
        mDrone.forget();

        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal, nullValue());
    }

    @Test
    public void testSupportedAxes() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.PITCH, Axis.ROLL)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));

        // check backend change
        mMockArsdkCore.commandReceived(1, encodeCapabilities(Axis.YAW));

        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.YAW));

        // disconnect
        disconnectDrone(mDrone, 1);

        // check that supported axes have been kept
        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.YAW));
    }

    @Test
    public void testStabilization() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.YAW, Axis.PITCH),
                ArsdkEncoder.encodeGimbalAttitude(0,
                        FrameOfReference.RELATIVE, FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE,
                        0, 0, 0, 0, 0, 0),
                // mock reception of bounds
                ArsdkEncoder.encodeGimbalRelativeAttitudeBounds(0, 2, 10, 3, 10, 4, 10),
                ArsdkEncoder.encodeGimbalAbsoluteAttitudeBounds(0, -100, 100, -100, 100, -100, 100)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsDisabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());

        // change stabilization info from backend
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE, FrameOfReference.ABSOLUTE,
                -10, 5, 30, 20, 80, 200));

        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsDisabled());

        // change yaw stabilization from api
        mGimbal.getStabilization(Gimbal.Axis.YAW).toggle();

        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsDisabling());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsDisabled());

        // check in the gimbal non-ack command encoder that the stab has been set
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.RELATIVE, 2, FrameOfReference.NONE, 0, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // stabilization info has not applied yet; stabilized axes should not change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE, FrameOfReference.ABSOLUTE,
                -10, 5, 30, 20, 80, 200));

        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsDisabling());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsDisabled());

        // now stabilization info has applies
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.RELATIVE, FrameOfReference.RELATIVE, FrameOfReference.ABSOLUTE,
                -10, 5, 30, 20, 80, 200));

        assertThat(mChangeCnt, is(4));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsDisabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsDisabled());

        // assert that a stab change that has not been requested by the component does notify the component
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.RELATIVE, FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE,
                -10, 5, 30, 20, 80, 200));

        assertThat(mChangeCnt, is(5));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsDisabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());

        // change stab on the yaw axis
        mGimbal.getStabilization(Gimbal.Axis.YAW).toggle();

        assertThat(mChangeCnt, is(6));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsEnabling());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        // check in the gimbal non-ack command encoder that the stab has been set
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.ABSOLUTE, 20, FrameOfReference.NONE, 0, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // mock stabilization info
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE,
                -10, 5, 30, 20, 80, 200));

        assertThat(mChangeCnt, is(7));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());

        // disconnect
        disconnectDrone(mDrone, 1);

        // check that stabilized axes have been kept
        assertThat(mChangeCnt, is(8)); // +1 because online-only settings have been reset
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());

        // change yaw stabilization offline
        mGimbal.getStabilization(Gimbal.Axis.YAW).toggle();

        assertThat(mChangeCnt, is(9));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsDisabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());

        // restart engine and reconnect, offline settings have been kept
        resetEngine();
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.YAW, Axis.PITCH),
                ArsdkEncoder.encodeGimbalAttitude(0,
                        FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE, FrameOfReference.NONE,
                        1, 2, 3, 4, 5, 6),
                // mock reception of bounds
                ArsdkEncoder.encodeGimbalAbsoluteAttitudeBounds(0, -10, 10, -10, 10, -10, 10),
                ArsdkEncoder.encodeGimbalRelativeAttitudeBounds(0, -10, 10, -10, 10, -10, 10)));

        // 4 commands received while gimbal is already published, but only 2 change the component as all supported axes
        // are currently stabilized
        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsDisabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());

        // check in the gimbal non-ack command encoder that the yaw stab has been changed
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.RELATIVE, 1, FrameOfReference.NONE, 0, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // yaw stabilization has applied
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.RELATIVE, FrameOfReference.ABSOLUTE, FrameOfReference.NONE,
                1, 2, 3, 4, 5, 6));

        // check that the non-ack command encoder still sends the same command
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.RELATIVE, 1, FrameOfReference.NONE, 0, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);
    }

    @Test
    public void testMaxSpeed() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.PITCH, Axis.ROLL),
                ArsdkEncoder.encodeGimbalMaxSpeed(0, -1, 1, 0,
                        -2, 2, 1,
                        -3, 3, 3)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(-2, 1, 2));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.ROLL), doubleSettingIsUpToDateAt(-3, 3, 3));

        // change max speed from api
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.gimbalSetMaxSpeed(0, 0, 2, 3)));
        mGimbal.getMaxSpeed(Gimbal.Axis.PITCH).setValue(2);

        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpdatingTo(-2, 2, 2));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.ROLL), doubleSettingIsUpToDateAt(-3, 3, 3));

        // max speed updated from backend
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalMaxSpeed(0,
                -1, 1, 0,
                -2, 2, 1.5f,
                -3, 3, 3));

        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(-2, 1.5, 2));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.ROLL), doubleSettingIsUpToDateAt(-3, 3, 3));

        // assert that a max speed change that has not been requested by the component does notify the component
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalMaxSpeed(0,
                -1, 1, 0,
                -2, 2, -1,
                -3, 3, 3));

        assertThat(mChangeCnt, is(4));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(-2, -1, 2));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.ROLL), doubleSettingIsUpToDateAt(-3, 3, 3));

        // disconnect
        disconnectDrone(mDrone, 1);

        // check that max speeds have been kept
        assertThat(mChangeCnt, is(4));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(-2, -1, 2));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.ROLL), doubleSettingIsUpToDateAt(-3, 3, 3));

        // change max speed offline
        mGimbal.getMaxSpeed(Gimbal.Axis.PITCH).setValue(10);

        assertThat(mChangeCnt, is(5));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(-2, 2, 2));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.ROLL), doubleSettingIsUpToDateAt(-3, 3, 3));

        // restart engine and reconnect, offline settings have been kept
        resetEngine();
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.PITCH, Axis.ROLL)));

        assertThat(mChangeCnt, is(0));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(-2, 2, 2));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.ROLL), doubleSettingIsUpToDateAt(-3, 3, 3));
    }

    @Test
    public void testAttitudeBounds() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.YAW, Axis.PITCH),
                ArsdkEncoder.encodeGimbalAttitude(0,
                        FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE, FrameOfReference.ABSOLUTE,
                        0, 0, 0, 0, 0, 0)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(0, 0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0, 0));

        // change attitude bounds from backend
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalRelativeAttitudeBounds(0,
                0, 180, 0, 90, -180, 180));

        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(0, 0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0, 90));

        // mock absolute attitude bounds change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAbsoluteAttitudeBounds(0,
                0, 360, 20, 45, -90, 90));

        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(0, 360));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0, 90));

        // check that changing the stabilization automatically changes the bounds
        mGimbal.getStabilization(Gimbal.Axis.YAW).toggle();

        assertThat(mChangeCnt, is(4));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(0, 180));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0, 90));

        // check that changing the stabilization automatically changes the bounds
        mGimbal.getStabilization(Gimbal.Axis.PITCH).toggle();

        assertThat(mChangeCnt, is(5));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(0, 180));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(20, 45));

        // check that the bounds are reset to 0 when disconnected
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(6)); // +1 because online-only settings have been reset
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0, 0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0, 0));
    }

    @Test
    public void testLockedAxes() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.PITCH, Axis.ROLL)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));

        // change locked axes from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalAxisLockState(0, Axis.toBitField(Axis.PITCH)));

        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH));

        // change again locked axes from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalAxisLockState(0, Axis.toBitField(Axis.YAW, Axis.ROLL)));

        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.ROLL));

        // check that all supported axes are locked when disconnected
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(4)); // +1 because online-only settings have been reset
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));

        // reset engine and check that all supported axes are locked
        resetEngine();

        assertThat(mChangeCnt, is(0));
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));
    }

    @Test
    public void testAttitude() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.PITCH, Axis.ROLL)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.ABSOLUTE), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL, Gimbal.FrameOfReference.ABSOLUTE), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.RELATIVE), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL, Gimbal.FrameOfReference.RELATIVE), is(0.0));

        // change attitude from backend
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE, FrameOfReference.ABSOLUTE,
                1, 2, 3, 10, 20, 30));

        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL), is(30.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.ABSOLUTE), is(20.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL, Gimbal.FrameOfReference.ABSOLUTE), is(30.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.RELATIVE), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL, Gimbal.FrameOfReference.RELATIVE), is(3.0));

        // if a stabilization change is requested, attitude should automatically match the asked frame of reference
        mGimbal.getStabilization(Gimbal.Axis.ROLL).toggle();

        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL), is(3.0));

        // roll attitude frame of reference should stay relative even if the change is not applied yet
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE, FrameOfReference.ABSOLUTE,
                1, 2, 4, 10, 20, 40));

        assertThat(mChangeCnt, is(4));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL), is(4.0));

        // stabilization change applies
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE, FrameOfReference.RELATIVE,
                1, 2, 4, 10, 20, 40));

        assertThat(mChangeCnt, is(5)); // +1 for the stabilization setting change
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL), is(4.0));

        // change attitude from backend
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE, FrameOfReference.RELATIVE,
                1, 2, 3, 10, 20, 30));

        assertThat(mChangeCnt, is(6));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL), is(3.0));

        // changing attitude of 1/1000 degree should not trigger any change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE, FrameOfReference.RELATIVE,
                1, 2, 3.001f, 10, 20, 30));

        assertThat(mChangeCnt, is(6));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL), is(3.0));

        // check that all attitudes are reset to 0 when disconnected
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(7)); // +1 because online-only settings have been reset
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL), is(0.0));
    }

    @Test
    public void testCorrectableAxes() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.PITCH, Axis.ROLL, Axis.YAW),
                ArsdkEncoder.encodeGimbalOffsets(0, ArsdkFeatureGimbal.State.ACTIVE,
                        1, 1, 1,
                        -2, 2, 1,
                        -3, 3, 3)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assert mGimbal.getOffsetCorrectionProcess() != null;
        assertThat(mGimbal.getOffsetCorrectionProcess(), notNullValue());
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(), containsInAnyOrder(
                Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));

        // check backend change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalOffsets(0, ArsdkFeatureGimbal.State.ACTIVE,
                -1, 1, 1,
                2, 2, 2,
                3, 3, 3));

        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.getOffsetCorrectionProcess(), notNullValue());
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(), containsInAnyOrder(Gimbal.Axis.YAW));

        // disconnect
        disconnectDrone(mDrone, 1);

        // check that offsets correction process stopped
        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal.getOffsetCorrectionProcess(), nullValue());
    }

    @Test
    public void testOffsets() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.PITCH, Axis.ROLL, Axis.YAW)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getOffsetCorrectionProcess(), nullValue());

        // start correction process
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.gimbalStartOffsetsUpdate(0)));
        mGimbal.startOffsetsCorrectionProcess();

        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getOffsetCorrectionProcess(), nullValue());

        // mock reception of offsets correction process started (only pitch should be correctable)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalOffsets(0, ArsdkFeatureGimbal.State.ACTIVE,
                0, 0, 0,
                -5, 5, 1,
                5, 5, 0));

        assertThat(mChangeCnt, is(2));
        assert mGimbal.getOffsetCorrectionProcess() != null;
        assertThat(mGimbal.getOffsetCorrectionProcess(), notNullValue());
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(), containsInAnyOrder(Gimbal.Axis.PITCH));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH),
                doubleSettingIsUpToDateAt(-5, 1, 5));

        // change offset on the pitch axis (also check that it is clamped)
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.gimbalSetOffsets(0, 0, 5, 0)));
        mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH).setValue(7);

        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH),
                doubleSettingIsUpdatingTo(-5, 5, 5));

        // mock correction offsets received
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalOffsets(0, ArsdkFeatureGimbal.State.ACTIVE,
                -1, 1, 0.5f,
                -5, 5, 5,
                5, 5, 0));

        assertThat(mChangeCnt, is(4));
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(), containsInAnyOrder(
                Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW),
                doubleSettingIsUpToDateAt(-1, 0.5, 1));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH),
                doubleSettingIsUpToDateAt(-5, 5, 5));

        // assert that an offset change that has not been requested by the component does notify the component
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalOffsets(0, ArsdkFeatureGimbal.State.ACTIVE,
                -1, 1, 1,
                0, 0, 0,
                5, 5, 0));

        assertThat(mChangeCnt, is(5));
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(), containsInAnyOrder(Gimbal.Axis.YAW));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW),
                doubleSettingIsUpToDateAt(-1, 1, 1));

        // mock reception of a process stopped without having asked
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalOffsets(0, ArsdkFeatureGimbal.State.INACTIVE,
                -1, 1, 1,
                0, 0, 0,
                5, 5, 0));

        assertThat(mChangeCnt, is(6));
        assertThat(mGimbal.getOffsetCorrectionProcess(), nullValue());

        // mock reception of a process started without having asked
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalOffsets(0, ArsdkFeatureGimbal.State.ACTIVE,
                -1, 1, 1,
                0, 0, 0,
                5, 5, 0));

        assertThat(mChangeCnt, is(7));
        assertThat(mGimbal.getOffsetCorrectionProcess(), notNullValue());
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(), containsInAnyOrder(Gimbal.Axis.YAW));

        // stop the process
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.gimbalStopOffsetsUpdate(0)));
        mGimbal.stopOffsetsCorrectionProcess();

        assertThat(mChangeCnt, is(7));
        assertThat(mGimbal.getOffsetCorrectionProcess(), notNullValue());

        // disconnect the drone
        disconnectDrone(mDrone, 1);

        // check that the process is stopped
        assertThat(mChangeCnt, is(8));
        assertThat(mGimbal.getOffsetCorrectionProcess(), nullValue());
    }

    @Test
    public void testCalibration() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalCalibrationState(ArsdkFeatureGimbal.CalibrationState.OK, 0)));

        // check initial values
        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.isCalibrated(), is(true));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.NONE));

        // mock reception of calibration state, need calibration
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalCalibrationState(ArsdkFeatureGimbal.CalibrationState.REQUIRED, 0));

        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.isCalibrated(), is(false));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.NONE));

        // start calibration
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.gimbalCalibrate(0)));
        mGimbal.startCalibration();

        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.isCalibrated(), is(false));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.NONE));

        // mock reception of calibration state, calibrating
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalCalibrationState(ArsdkFeatureGimbal.CalibrationState.IN_PROGRESS, 0));

        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal.isCalibrated(), is(false));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.CALIBRATING));

        // mock reception of calibration result, failure
        mExpectedCalibrationState = Gimbal.CalibrationProcessState.FAILURE; // expected transient state
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalCalibrationResult(0, ArsdkFeatureGimbal.CalibrationResult.FAILURE));

        assertThat(mChangeCnt, is(5));
        assertThat(mGimbal.isCalibrated(), is(false));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.NONE));
        assertThat(mExpectedCalibrationState, nullValue());

        // mock reception of calibration state, need format, nothing should change
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalCalibrationState(ArsdkFeatureGimbal.CalibrationState.REQUIRED, 0));

        assertThat(mChangeCnt, is(5));
        assertThat(mGimbal.isCalibrated(), is(false));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.NONE));

        // start calibration
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.gimbalCalibrate(0)));
        mGimbal.startCalibration();

        assertThat(mChangeCnt, is(5));
        assertThat(mGimbal.isCalibrated(), is(false));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.NONE));

        // mock reception of calibration state, calibrating
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalCalibrationState(ArsdkFeatureGimbal.CalibrationState.IN_PROGRESS, 0));

        assertThat(mChangeCnt, is(6));
        assertThat(mGimbal.isCalibrated(), is(false));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.CALIBRATING));

        // mock reception of calibration state, calibrated
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalCalibrationState(ArsdkFeatureGimbal.CalibrationState.OK, 0));

        assertThat(mChangeCnt, is(7));
        assertThat(mGimbal.isCalibrated(), is(true));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.CALIBRATING));

        // mock reception of calibration result, success
        mExpectedCalibrationState = Gimbal.CalibrationProcessState.SUCCESS; // expected transient state
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalCalibrationResult(0, ArsdkFeatureGimbal.CalibrationResult.SUCCESS));

        assertThat(mChangeCnt, is(9));
        assertThat(mGimbal.isCalibrated(), is(true));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.NONE));
        assertThat(mExpectedCalibrationState, nullValue());

        // mock reception of calibration state, calibrating
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalCalibrationState(ArsdkFeatureGimbal.CalibrationState.IN_PROGRESS, 0));

        assertThat(mChangeCnt, is(10));
        assertThat(mGimbal.isCalibrated(), is(true));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.CALIBRATING));

        // cancel calibration
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.gimbalCancelCalibration(0)));
        mGimbal.cancelCalibration();

        assertThat(mChangeCnt, is(10));
        assertThat(mGimbal.isCalibrated(), is(true));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.CALIBRATING));

        // mock reception of calibration result, canceled
        mExpectedCalibrationState = Gimbal.CalibrationProcessState.CANCELED; // expected transient state
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalCalibrationResult(0, ArsdkFeatureGimbal.CalibrationResult.CANCELED));

        assertThat(mChangeCnt, is(12));
        assertThat(mGimbal.isCalibrated(), is(true));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.NONE));
        assertThat(mExpectedCalibrationState, nullValue());

        // mock reception of calibration state, need calibration
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalCalibrationState(ArsdkFeatureGimbal.CalibrationState.REQUIRED, 0));

        assertThat(mChangeCnt, is(13));
        assertThat(mGimbal.isCalibrated(), is(false));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.NONE));
    }

    @Test
    public void testControl() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.PITCH, Axis.ROLL),
                // mock reception of bounds
                ArsdkEncoder.encodeGimbalAbsoluteAttitudeBounds(0, -100, 100, -100, 100, -100, 100)));

        // check that, at the beginning, no setTarget command is sent
        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsDisabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());
        mMockArsdkCore.assertNoExpectation();
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // check that receiving an attitude event does not send any setTarget command
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE,
                1, 2, 3, 10, 20, 30));

        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.NONE, 0, FrameOfReference.NONE, 0, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // check that setting the stabilization does send the setTarget command with updated attitude according to the
        // frame of reference
        mGimbal.getStabilization(Gimbal.Axis.ROLL).toggle();

        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsEnabling());
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.NONE, 0, FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 30)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // control the gimbal
        mGimbal.control(Gimbal.ControlMode.POSITION, null, 15.0, null);

        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsEnabling());
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 15, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        mGimbal.control(Gimbal.ControlMode.POSITION, null, 15.0, 10.0);

        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsEnabling());
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 15, FrameOfReference.ABSOLUTE, 10)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        mGimbal.control(Gimbal.ControlMode.VELOCITY, null, 15.0, null);

        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsEnabling());
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.VELOCITY,
                        FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 15, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // check that changing roll stabilization sends the setTarget command with the new frame of reference
        // (with last control in velocity on a different axis)
        mGimbal.getStabilization(Gimbal.Axis.ROLL).toggle();

        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabling());
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.VELOCITY,
                        FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 15, FrameOfReference.RELATIVE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // disconnect, reset and reconnect to ensure that control command encoder is properly initialized
        disconnectDrone(mDrone, 1);
        resetEngine();
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.PITCH, Axis.ROLL),
                ArsdkEncoder.encodeGimbalAttitude(0,
                        FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE,
                        1, 2, 3, 10, 20, 30)
        ));

        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.NONE, 0, FrameOfReference.NONE, 0, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // check that gimbal control command is correct
        mGimbal.control(Gimbal.ControlMode.POSITION, null, 15.0, null);

        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 15, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // mock pitch stabilization change from drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE, FrameOfReference.RELATIVE,
                1, 2, 3, 10, 20, 30));

        // check that gimbal control command does not set any pitch stabilization
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsDisabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.NONE, 0, FrameOfReference.NONE, 0, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // now control gimbal in velocity mode
        mGimbal.control(Gimbal.ControlMode.VELOCITY, null, 10.0, null);

        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsDisabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.VELOCITY,
                        FrameOfReference.NONE, 0, FrameOfReference.RELATIVE, 10, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // mock pitch stabilization change from drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE,
                1, 2, 3, 10, 20, 30));

        // check that gimbal control command has changed pitch stabilization but not pitch value
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.VELOCITY,
                        FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 10, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);
    }

    @Test
    public void testControlSendingTimes() {
        int maxRepeatedSent = 10; // should be the same as GimbalControlCommandEncoder.GIMBAL_COMMANDS_REPETITIONS

        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.PITCH, Axis.ROLL)));

        // check that, at the beginning, no setTarget command is sent
        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsDisabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());
        mMockArsdkCore.assertNoExpectation();
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // check that receiving an attitude event does not send any setTarget command
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE,
                1, 2, 3, 10, 20, 30));

        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.NONE, 0, FrameOfReference.NONE, 0, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // control the gimbal
        mGimbal.control(Gimbal.ControlMode.POSITION, null, 15.0, null);

        for (int i = 0; i < maxRepeatedSent; i++) {
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                            FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 15, FrameOfReference.NONE, 0)));
            mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);
        }
        mMockArsdkCore.assertNoExpectation();
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // send the next command only twice, then change it
        mGimbal.control(Gimbal.ControlMode.POSITION, null, 15.0, 1.0);

        for (int i = 0; i < 2; i++) {
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                            FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 15, FrameOfReference.RELATIVE, 1)));
            mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);
        }

        mGimbal.control(Gimbal.ControlMode.POSITION, null, 15.0, 2.0);

        for (int i = 0; i < maxRepeatedSent; i++) {
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                            FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 15, FrameOfReference.RELATIVE, 2)));
            mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);
        }
        mMockArsdkCore.assertNoExpectation();
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // check that command is sent forever (=> maxRepeatedSent + 1) if at least one axis target is not 0 when
        // controlling in velocity
        mGimbal.control(Gimbal.ControlMode.VELOCITY, null, 1.0, null);

        for (int i = 0; i < maxRepeatedSent + 1; i++) {
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.gimbalSetTarget(0, ControlMode.VELOCITY,
                            FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 1, FrameOfReference.NONE, 0)));
            mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);
        }

        // however, if all values are null or zero, it should be sent 10 times
        mGimbal.control(Gimbal.ControlMode.VELOCITY, null, 0.0, null);

        for (int i = 0; i < maxRepeatedSent; i++) {
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.gimbalSetTarget(0, ControlMode.VELOCITY,
                            FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 0, FrameOfReference.NONE, 0)));
            mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);
        }
        mMockArsdkCore.assertNoExpectation();
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        mGimbal.control(Gimbal.ControlMode.VELOCITY, null, null, null);

        for (int i = 0; i < maxRepeatedSent; i++) {
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.gimbalSetTarget(0, ControlMode.VELOCITY,
                            FrameOfReference.NONE, 0, FrameOfReference.NONE, 0, FrameOfReference.NONE, 0)));
            mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);
        }
        mMockArsdkCore.assertNoExpectation();
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);
    }

    @Test
    public void testAttitudeReceivedBeforeCapabilities() {
        // capabilities are received after attitude on connection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGimbalAttitude(0,
                        FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE,
                        1, 2, 3, 10, 20, 30),
                encodeCapabilities(Axis.ROLL, Axis.PITCH),
                // mock reception of bounds
                ArsdkEncoder.encodeGimbalRelativeAttitudeBounds(0, 2, 10, 3, 10, 4, 10),
                ArsdkEncoder.encodeGimbalAbsoluteAttitudeBounds(0, -100, 100, -100, 100, -100, 100)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());

        // stabilization is changed from api
        mGimbal.getStabilization(Gimbal.Axis.ROLL).toggle();

        // check that stabilization setting doesn't change and no command is sent
        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());

        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // control the gimbal
        mGimbal.control(Gimbal.ControlMode.VELOCITY, null, 1.0, null);

        // check that no command is sent
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // attitude is received again
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE,
                1, 2, 3, 10, 20, 30));

        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsEnabled());

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.NONE, 0, FrameOfReference.NONE, 0, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // now command is sent when stabilization is changed
        mGimbal.getStabilization(Gimbal.Axis.ROLL).toggle();

        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabling());

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.NONE, 0, FrameOfReference.NONE, 0, FrameOfReference.RELATIVE, 4.0f)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // command is sent on gimbal control
        mGimbal.control(Gimbal.ControlMode.VELOCITY, null, 1.0, null);

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.VELOCITY,
                        FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 1.0f, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // disconnect and reconnect, attitude is not received yet
        disconnectDrone(mDrone, 1);
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.ROLL, Axis.PITCH)));

        assertThat(mChangeCnt, is(4));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());

        // stabilization is changed from api
        mGimbal.getStabilization(Gimbal.Axis.ROLL).toggle();

        // check that stabilization setting changes (as we have saved stabilization settings) but no command is sent
        assertThat(mChangeCnt, is(5));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsEnabled());

        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // control the gimbal
        mGimbal.control(Gimbal.ControlMode.VELOCITY, null, 2.0, null);

        // check that no command is sent
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // attitude is received
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE,
                1, 2, 3, 10, 20, 30));

        // check that stabilization setting is sent as we changed it before attitude was received
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.NONE, 0, FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 30)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // command is sent on gimbal control
        mGimbal.control(Gimbal.ControlMode.VELOCITY, null, 2.0, null);

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.VELOCITY,
                        FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 2.0f, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // disconnect, reset engine and reconnect, attitude is not received yet
        disconnectDrone(mDrone, 1);
        resetEngine();
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.ROLL, Axis.PITCH),
                ArsdkEncoder.encodeGimbalAbsoluteAttitudeBounds(0, -100, 100, -100, 100, -100, 100)));

        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // attitude is received, stabilization differs from the stored one
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAttitude(0,
                FrameOfReference.ABSOLUTE, FrameOfReference.RELATIVE, FrameOfReference.ABSOLUTE,
                1, 2, 3, 10, 20, 30));

        // check that stabilization setting is sent
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.POSITION,
                        FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 20, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);

        // command is sent on gimbal control
        mGimbal.control(Gimbal.ControlMode.VELOCITY, null, 2.0, null);

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.gimbalSetTarget(0, ControlMode.VELOCITY,
                        FrameOfReference.NONE, 0, FrameOfReference.ABSOLUTE, 2.0f, FrameOfReference.NONE, 0)));
        mMockArsdkCore.pollNoAckCommands(1, GimbalControlCommandEncoder.class);
    }

    @Test
    public void testErrors() {
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.currentErrors(), empty());

        // mock CALIBRATION error
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAlert(0,
                ArsdkFeatureGimbal.Error.toBitField(ArsdkFeatureGimbal.Error.CALIBRATION_ERROR)));

        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.currentErrors(), containsInAnyOrder(Gimbal.Error.CALIBRATION));

        // mock OVERLOAD error
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAlert(0,
                ArsdkFeatureGimbal.Error.toBitField(ArsdkFeatureGimbal.Error.OVERLOAD_ERROR)));

        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal.currentErrors(), containsInAnyOrder(Gimbal.Error.OVERLOAD));

        // mock COMM error
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAlert(0,
                ArsdkFeatureGimbal.Error.toBitField(ArsdkFeatureGimbal.Error.COMM_ERROR)));

        assertThat(mChangeCnt, is(4));
        assertThat(mGimbal.currentErrors(), containsInAnyOrder(Gimbal.Error.COMMUNICATION));

        // mock CRITICAL + CALIBRATION error
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGimbalAlert(0,
                ArsdkFeatureGimbal.Error.toBitField(
                        ArsdkFeatureGimbal.Error.CALIBRATION_ERROR,
                        ArsdkFeatureGimbal.Error.CRITICAL_ERROR)));

        assertThat(mChangeCnt, is(5));
        assertThat(mGimbal.currentErrors(), containsInAnyOrder(Gimbal.Error.CALIBRATION, Gimbal.Error.CRITICAL));
    }

    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeCapabilities(Axis.values()),
                ArsdkEncoder.encodeGimbalMaxSpeed(0, 0, 1, 0, 0, 1, 0, 0, 1, 0),
                ArsdkEncoder.encodeGimbalAttitude(0,
                        FrameOfReference.RELATIVE, FrameOfReference.RELATIVE, FrameOfReference.RELATIVE,
                        0, 0, 0, 0, 0, 0),
                ArsdkEncoder.encodeGimbalAlert(0, ArsdkFeatureGimbal.Error.toBitField(
                        ArsdkFeatureGimbal.Error.COMM_ERROR))));

        assertThat(mChangeCnt, is(1));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.ROLL), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), allOf(
                booleanSettingValueIs(false),
                settingIsUpToDate()));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), allOf(
                booleanSettingValueIs(false),
                settingIsUpToDate()));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), allOf(
                booleanSettingValueIs(false),
                settingIsUpToDate()));
        assertThat(mGimbal.currentErrors(), containsInAnyOrder(Gimbal.Error.COMMUNICATION));

        // mock user modifies settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.gimbalSetMaxSpeed(0, 1, 0, 0)));
        mGimbal.getMaxSpeed(Gimbal.Axis.YAW).setValue(1);

        assertThat(mChangeCnt, is(2));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.gimbalSetMaxSpeed(0, 1, 1, 0)));
        mGimbal.getMaxSpeed(Gimbal.Axis.PITCH).setValue(1);

        assertThat(mChangeCnt, is(3));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.gimbalSetMaxSpeed(0, 1, 1, 1)));
        mGimbal.getMaxSpeed(Gimbal.Axis.ROLL).setValue(1);

        assertThat(mChangeCnt, is(4));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.ROLL), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpdating()));

        mGimbal.getStabilization(Gimbal.Axis.YAW).setEnabled(true);

        assertThat(mChangeCnt, is(5));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), allOf(
                booleanSettingValueIs(true),
                settingIsUpdating()));

        mGimbal.getStabilization(Gimbal.Axis.PITCH).setEnabled(true);

        assertThat(mChangeCnt, is(6));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), allOf(
                booleanSettingValueIs(true),
                settingIsUpdating()));

        mGimbal.getStabilization(Gimbal.Axis.ROLL).setEnabled(true);

        assertThat(mChangeCnt, is(7));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), allOf(
                booleanSettingValueIs(true),
                settingIsUpdating()));

        // disconnect
        disconnectDrone(mDrone, 1);

        // settings should be updated to user value
        assertThat(mChangeCnt, is(8));

        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.ROLL), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));

        // test other values are reset as they should
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.values()));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL), is(0.0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(0, 0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0, 0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.ROLL), doubleRangeIs(0, 0));
        assertThat(mGimbal.getOffsetCorrectionProcess(), nullValue());
        assertThat(mGimbal.currentErrors(), empty());
    }
}
