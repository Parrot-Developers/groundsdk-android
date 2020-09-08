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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import com.parrot.drone.groundsdk.device.peripheral.Magnetometer;
import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith1StepCalibration;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.Magnetometer1StepCalibrationProcessStateMatcher.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class MagnetometerWith1StepCalibrationTest {

    private MockComponentStore<Peripheral> mStore;

    private MagnetometerWith1StepCalibrationCore mMagnetometer;

    private Backend mBackend;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mMagnetometer = new MagnetometerWith1StepCalibrationCore(mStore, mBackend);
    }

    @After
    public void teardown() {
        mMagnetometer = null;
        mBackend = null;
        mStore = null;
    }

    @Test
    public void testPublication() {
        mMagnetometer.publish();
        assertThat(mMagnetometer, is(mStore.get(Magnetometer.class)));
        mMagnetometer.unpublish();
        assertThat(mStore.get(Magnetometer.class), nullValue());
    }

    @Test
    public void testCalibrationState() {
        mMagnetometer.publish();
        Magnetometer magneto = mStore.get(Magnetometer.class);
        assert magneto != null;
        int[] cnt = new int[1];
        mStore.registerObserver(Magnetometer.class, () -> cnt[0]++);

        // test initial value
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));

        // change calibration state
        mMagnetometer.updateCalibrationState(Magnetometer.MagnetometerCalibrationState.RECOMMENDED).notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(magneto.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.RECOMMENDED));

        // change calibration state
        mMagnetometer.updateCalibrationState(Magnetometer.MagnetometerCalibrationState.REQUIRED).notifyUpdated();
        assertThat(cnt[0], is(2));
        assertThat(magneto.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));

        // change calibration state
        mMagnetometer.updateCalibrationState(Magnetometer.MagnetometerCalibrationState.CALIBRATED).notifyUpdated();
        assertThat(cnt[0], is(3));
        assertThat(magneto.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.CALIBRATED));
    }

    @Test
    public void testCalibrationProcess() {
        mMagnetometer.publish();
        MagnetometerWith1StepCalibration magneto = mStore.get(MagnetometerWith1StepCalibration.class);
        assert magneto != null;
        int[] cnt = new int[1];
        mStore.registerObserver(MagnetometerWith1StepCalibration.class, () -> cnt[0]++);

        // test initial value
        assertThat(magneto.getCalibrationProcessState(), nullValue());
        assertThat(mMagnetometer.calibrationState(), is(Magnetometer.MagnetometerCalibrationState.REQUIRED));

        // start a calibration process
        magneto.startCalibrationProcess();
        assertThat(mBackend.mCalibrationProcessStartedCalls, is(1));
        assertThat(cnt[0], is(1));
        assertThat(magneto.getCalibrationProcessState(), allOf(notNullValue(), is(0, 0, 0)));

        // start a calibration process if already started should not change anything
        magneto.startCalibrationProcess();
        assertThat(mBackend.mCalibrationProcessStartedCalls, is(1));
        assertThat(cnt[0], is(1));
        assertThat(magneto.getCalibrationProcessState(), allOf(notNullValue(), is(0, 0, 0)));

        // update roll calibration progress
        mMagnetometer.updateCalibrationProgress(50, 0, 0).notifyUpdated();
        assertThat(cnt[0], is(2));
        assertThat(magneto.getCalibrationProcessState(), allOf(notNullValue(), is(50, 0, 0)));

        // update pitch calibration progress
        mMagnetometer.updateCalibrationProgress(50, 50, 0).notifyUpdated();
        assertThat(cnt[0], is(3));
        assertThat(magneto.getCalibrationProcessState(), allOf(notNullValue(), is(50, 50, 0)));

        // update yaw calibration progress
        mMagnetometer.updateCalibrationProgress(50, 50, 50).notifyUpdated();
        assertThat(cnt[0], is(4));
        assertThat(magneto.getCalibrationProcessState(), allOf(notNullValue(), is(50, 50, 50)));

        // update all axes calibration progress
        mMagnetometer.updateCalibrationProgress(60, 70, 80).notifyUpdated();
        assertThat(cnt[0], is(5));
        assertThat(magneto.getCalibrationProcessState(), allOf(notNullValue(), is(60, 70, 80)));

        // all axes calibrated progress
        mMagnetometer.updateCalibrationProgress(100, 100, 100).notifyUpdated();
        assertThat(cnt[0], is(6));
        assertThat(magneto.getCalibrationProcessState(), allOf(notNullValue(), is(100, 100, 100)));
    }

    @Test
    public void testCalibrationProcessCancel() {
        mMagnetometer.publish();
        MagnetometerWith1StepCalibration magneto = mStore.get(MagnetometerWith1StepCalibration.class);
        assert magneto != null;
        int[] cnt = new int[1];
        mStore.registerObserver(MagnetometerWith1StepCalibration.class, () -> cnt[0]++);

        // test initial value
        assertThat(magneto.getCalibrationProcessState(), nullValue());

        // cancel a not started calibration process should not do anything
        magneto.cancelCalibrationProcess();
        assertThat(mBackend.mCalibrationProcessCanceledCalls, is(0));
        assertThat(cnt[0], is(0));
        assertThat(magneto.getCalibrationProcessState(), nullValue());

        // start a calibration process
        magneto.startCalibrationProcess();
        assertThat(mBackend.mCalibrationProcessStartedCalls, is(1));
        assertThat(cnt[0], is(1));
        assertThat(magneto.getCalibrationProcessState(), allOf(notNullValue(), is(0, 0, 0)));

        // cancel the calibration process
        magneto.cancelCalibrationProcess();
        assertThat(mBackend.mCalibrationProcessCanceledCalls, is(1));
        assertThat(cnt[0], is(2));
        assertThat(magneto.getCalibrationProcessState(), nullValue());
    }

    private static final class Backend implements MagnetometerWith1StepCalibrationCore.Backend {

        int mCalibrationProcessStartedCalls;

        int mCalibrationProcessCanceledCalls;

        @Override
        public void startCalibrationProcess() {
            mCalibrationProcessStartedCalls++;
        }

        @Override
        public void cancelCalibrationProcess() {
            mCalibrationProcessCanceledCalls++;
        }
    }
}
