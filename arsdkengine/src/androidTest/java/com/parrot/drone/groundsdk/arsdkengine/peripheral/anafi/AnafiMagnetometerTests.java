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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi;

import com.parrot.drone.groundsdk.Magnetometer3StepCalibrationProcessStateMatcher;
import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Magnetometer;
import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith3StepCalibration;
import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith3StepCalibration.CalibrationProcessState;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCommon;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import java.util.EnumSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiMagnetometerTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private MagnetometerWith3StepCalibration mMagnetometer;

    private int mChangeCnt;

    private boolean mLatestFailed;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;
        mLatestFailed = false;

        mMagnetometer = mDrone.getPeripheralStore().get(mMockSession, MagnetometerWith3StepCalibration.class);
        mDrone.getPeripheralStore().registerObserver(Magnetometer.class, () -> {
            mMagnetometer = mDrone.getPeripheralStore().get(mMockSession, MagnetometerWith3StepCalibration.class);
            if (mMagnetometer != null) {
                CalibrationProcessState calibrationProcessState = mMagnetometer.getCalibrationProcessState();
                if (calibrationProcessState != null) {
                    mLatestFailed = calibrationProcessState.failed();
                }
            }
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the drone is not connected
        assertThat(mMagnetometer, is(nullValue()));

        connectDrone(mDrone, 1);
        assertThat(mMagnetometer, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectDrone(mDrone, 1);
        assertThat(mMagnetometer, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testCalibrationState() {
        connectDrone(mDrone, 1);
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
        assertThat(mChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationRequiredState(0));
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.CALIBRATED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationRequiredState(1));
        assertThat(mChangeCnt, is(3));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationRequiredState(2));
        assertThat(mChangeCnt, is(4));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.RECOMMENDED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
    }

    @Test
    public void testCalibrationProcess() {
        connectDrone(mDrone, 1);
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
        assertThat(mChangeCnt, is(1));

        // start the calibration process
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonCalibrationMagnetoCalibration(1)));
        mMagnetometer.startCalibrationProcess();
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.NONE,
                        EnumSet.noneOf(CalibrationProcessState.Axis.class))));

        // starting it again should not send a new command
        mMagnetometer.startCalibrationProcess();
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.NONE,
                        EnumSet.noneOf(CalibrationProcessState.Axis.class))));

        // receiving this command should not change anything
        // as the creation of the calibrationProcessState obj is done on startCalibrationProcess()
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationStartedChanged(1));
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.NONE,
                        EnumSet.noneOf(CalibrationProcessState.Axis.class))));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChanged(
                        ArsdkFeatureCommon.CalibrationstateMagnetocalibrationaxistocalibratechangedAxis.XAXIS));
        assertThat(mChangeCnt, is(3));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.ROLL,
                        EnumSet.noneOf(CalibrationProcessState.Axis.class))));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChanged(
                        ArsdkFeatureCommon.CalibrationstateMagnetocalibrationaxistocalibratechangedAxis.YAXIS));
        assertThat(mChangeCnt, is(4));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.PITCH,
                        EnumSet.noneOf(CalibrationProcessState.Axis.class))));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChanged(
                        ArsdkFeatureCommon.CalibrationstateMagnetocalibrationaxistocalibratechangedAxis.ZAXIS));
        assertThat(mChangeCnt, is(5));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.YAW,
                        EnumSet.noneOf(CalibrationProcessState.Axis.class))));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChanged(
                        ArsdkFeatureCommon.CalibrationstateMagnetocalibrationaxistocalibratechangedAxis.NONE));
        assertThat(mChangeCnt, is(6));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.NONE,
                        EnumSet.noneOf(CalibrationProcessState.Axis.class))));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationStateChanged(1, 1, 0, 0));
        assertThat(mChangeCnt, is(7));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.NONE,
                        EnumSet.of(CalibrationProcessState.Axis.PITCH, CalibrationProcessState.Axis.ROLL))));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationStateChanged(1, 0, 1, 0));
        assertThat(mChangeCnt, is(8));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.NONE,
                        EnumSet.of(CalibrationProcessState.Axis.ROLL, CalibrationProcessState.Axis.YAW))));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationStartedChanged(0));
        assertThat(mChangeCnt, is(9));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
    }

    @Test
    public void testCalibrationProcessFailed() {
        connectDrone(mDrone, 1);
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
        assertThat(mChangeCnt, is(1));

        // start the calibration process
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonCalibrationMagnetoCalibration(1)));
        mMagnetometer.startCalibrationProcess();
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.NONE,
                        EnumSet.noneOf(CalibrationProcessState.Axis.class))));
        assertThat(mLatestFailed, is(false));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationStateChanged(1, 0, 1, 1));
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.NONE,
                        EnumSet.noneOf(CalibrationProcessState.Axis.class))));
        assertThat(mLatestFailed, is(false));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationStartedChanged(0));
        assertThat(mChangeCnt, is(4)); // 2 changes: calibration failure and calibration end
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
        assertThat(mLatestFailed, is(true));
    }

    @Test
    public void testCalibrationProcessCancel() {
        connectDrone(mDrone, 1);
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
        assertThat(mChangeCnt, is(1));

        // start the calibration process
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonCalibrationMagnetoCalibration(1)));
        mMagnetometer.startCalibrationProcess();
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.NONE,
                        EnumSet.noneOf(CalibrationProcessState.Axis.class))));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonCalibrationMagnetoCalibration(0)));
        mMagnetometer.cancelCalibrationProcess();
        assertThat(mChangeCnt, is(3));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
    }

    @Test
    public void testCalibrationProcessAxesOkButFinallyFailed() {
        connectDrone(mDrone, 1);
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
        assertThat(mChangeCnt, is(1));

        // start the calibration process
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonCalibrationMagnetoCalibration(1)));
        mMagnetometer.startCalibrationProcess();
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.NONE,
                        EnumSet.noneOf(CalibrationProcessState.Axis.class))));

        // receive all axes OK
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationStateChanged(1, 1, 1, 0));
        assertThat(mChangeCnt, is(3));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.NONE,
                        EnumSet.of(CalibrationProcessState.Axis.PITCH, CalibrationProcessState.Axis.YAW,
                                CalibrationProcessState.Axis.ROLL))));

        // current axe is yaw
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationAxisToCalibrateChanged(
                        ArsdkFeatureCommon.CalibrationstateMagnetocalibrationaxistocalibratechangedAxis.ZAXIS));
        assertThat(mChangeCnt, is(4));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.YAW,
                        EnumSet.of(CalibrationProcessState.Axis.PITCH, CalibrationProcessState.Axis.YAW,
                                CalibrationProcessState.Axis.ROLL))));

        // current axe is yaw not calibrated
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationStateChanged(1, 1, 0, 0));
        assertThat(mChangeCnt, is(5));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.YAW,
                        EnumSet.of(CalibrationProcessState.Axis.PITCH, CalibrationProcessState.Axis.ROLL))));

        // receive all axes OK
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationStateChanged(1, 1, 1, 0));
        assertThat(mChangeCnt, is(6));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher.is(CalibrationProcessState.Axis.YAW,
                        EnumSet.of(CalibrationProcessState.Axis.PITCH, CalibrationProcessState.Axis.YAW,
                                CalibrationProcessState.Axis.ROLL))));

        // the receive "all axes are ok" but calibrationFailed is true (all axes must be considered as uncalibrated)
        // The failure state will be indicated at the interface only when the calibration process stops. At this step
        // failed is still false
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationStateChanged(1, 1, 1, 1));
        assertThat(mChangeCnt, is(7));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer3StepCalibrationProcessStateMatcher
                        .is(CalibrationProcessState.Axis.YAW, EnumSet.noneOf(CalibrationProcessState.Axis.class))));
        assertThat(mLatestFailed, is(false));

        // end of the calibration process
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCalibrationStateMagnetoCalibrationStartedChanged(0));
        assertThat(mChangeCnt, is(9)); // 2 changes: calibration failure and calibration end
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
        assertThat(mLatestFailed, is(true));
    }
}
