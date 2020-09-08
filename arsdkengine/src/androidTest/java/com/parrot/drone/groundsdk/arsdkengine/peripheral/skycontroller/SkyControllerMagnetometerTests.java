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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller;

import com.parrot.drone.groundsdk.Magnetometer1StepCalibrationProcessStateMatcher;
import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.Magnetometer;
import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith1StepCalibration;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSkyctrl;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class SkyControllerMagnetometerTests extends ArsdkEngineTestBase {

    private RemoteControlCore mRemoteControl;

    private MagnetometerWith1StepCalibration mMagnetometer;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", RemoteControl.Model.SKY_CONTROLLER_3.id(), "RC", 1, Backend.TYPE_MUX);
        mRemoteControl = mRCStore.get("123");
        assert mRemoteControl != null;
        mMagnetometer = mRemoteControl.getPeripheralStore().get(mMockSession, MagnetometerWith1StepCalibration.class);
        mRemoteControl.getPeripheralStore().registerObserver(Magnetometer.class, () -> {
            mMagnetometer = mRemoteControl.getPeripheralStore().get(mMockSession,
                    MagnetometerWith1StepCalibration.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the remote controller is not connected
        assertThat(mMagnetometer, is(nullValue()));

        connectRemoteControl(mRemoteControl, 1);
        assertThat(mMagnetometer, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectRemoteControl(mRemoteControl, 1);
        assertThat(mMagnetometer, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testCalibrationState() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
        assertThat(mChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCalibrationStateMagnetoCalibrationState(
                ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus.CALIBRATED, 0, 0, 0));
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.CALIBRATED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCalibrationStateMagnetoCalibrationState(
                ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus.UNRELIABLE, 0, 0, 0));
        assertThat(mChangeCnt, is(3));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCalibrationStateMagnetoCalibrationState(
                ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus.ASSESSING, 0, 0, 0));
        assertThat(mChangeCnt, is(3));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCalibrationStateMagnetoCalibrationState(
                ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus.CALIBRATED, 0, 0, 0));
        assertThat(mChangeCnt, is(4));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.CALIBRATED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
    }

    @Test
    public void testCalibrationProcess() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
        assertThat(mChangeCnt, is(1));

        // start the calibration process
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.skyctrlCalibrationEnableMagnetoCalibrationQualityUpdates(1), true));
        mMagnetometer.startCalibrationProcess();
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer1StepCalibrationProcessStateMatcher.is(0, 0, 0)));

        // starting it again should not send a new command
        mMagnetometer.startCalibrationProcess();
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer1StepCalibrationProcessStateMatcher.is(0, 0, 0)));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCalibrationStateMagnetoCalibrationState(
                ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus.UNRELIABLE, 0, 0, 0));
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer1StepCalibrationProcessStateMatcher.is(0, 0, 0)));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCalibrationStateMagnetoCalibrationState(
                ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus.UNRELIABLE, 128, 0, 0));
        assertThat(mChangeCnt, is(3));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer1StepCalibrationProcessStateMatcher.is(50, 0, 0)));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCalibrationStateMagnetoCalibrationState(
                ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus.UNRELIABLE, 128, 128, 0));
        assertThat(mChangeCnt, is(4));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer1StepCalibrationProcessStateMatcher.is(50, 50, 0)));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCalibrationStateMagnetoCalibrationState(
                ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus.UNRELIABLE, 128, 128, 128));
        assertThat(mChangeCnt, is(5));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer1StepCalibrationProcessStateMatcher.is(50, 50, 50)));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCalibrationStateMagnetoCalibrationState(
                ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus.UNRELIABLE, -1, 256, 1234));
        assertThat(mChangeCnt, is(6));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer1StepCalibrationProcessStateMatcher.isInRange()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCalibrationStateMagnetoCalibrationState(
                ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus.UNRELIABLE, 255, 255, 255));
        assertThat(mChangeCnt, is(7));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer1StepCalibrationProcessStateMatcher.is(100, 100, 100)));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCalibrationStateMagnetoCalibrationState(
                ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus.ASSESSING, 255, 255, 255));
        assertThat(mChangeCnt, is(7));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer1StepCalibrationProcessStateMatcher.is(100, 100, 100)));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCalibrationStateMagnetoCalibrationState(
                ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus.CALIBRATED, 255, 255, 255));
        assertThat(mChangeCnt, is(8));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.CALIBRATED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer1StepCalibrationProcessStateMatcher.is(100, 100, 100)));
    }

    @Test
    public void testCalibrationProcessCancel() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
        assertThat(mChangeCnt, is(1));

        // start the calibration process
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.skyctrlCalibrationEnableMagnetoCalibrationQualityUpdates(1), true));
        mMagnetometer.startCalibrationProcess();
        assertThat(mChangeCnt, is(2));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), allOf(notNullValue(),
                Magnetometer1StepCalibrationProcessStateMatcher.is(0, 0, 0)));

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.skyctrlCalibrationEnableMagnetoCalibrationQualityUpdates(0), true));
        mMagnetometer.cancelCalibrationProcess();
        assertThat(mChangeCnt, is(3));
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));
        assertThat(mMagnetometer.getCalibrationProcessState(), nullValue());
    }
}
