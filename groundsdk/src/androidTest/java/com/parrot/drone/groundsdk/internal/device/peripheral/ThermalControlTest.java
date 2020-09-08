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

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.ThermalControl;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingSupports;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ThermalControlTest {

    private MockComponentStore<Peripheral> mStore;

    private ThermalControlCore mThermalControlImpl;

    private ThermalControl mThermalControl;

    private ThermalControlCore.Backend mMockBackend;

    private int mComponentChangeCnt;

    @Before
    public void setup() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mMockBackend = mock(ThermalControlCore.Backend.class);
        mThermalControlImpl = new ThermalControlCore(mStore, mMockBackend);
        mThermalControl = mStore.get(ThermalControl.class);
        mStore.registerObserver(ThermalControl.class, () -> {
            mComponentChangeCnt++;
            mThermalControl = mStore.get(ThermalControl.class);
        });
        mComponentChangeCnt = 0;
    }

    @After
    public void teardown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mThermalControl, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mThermalControlImpl.publish();
        assertThat(mThermalControl, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mThermalControlImpl.unpublish();
        assertThat(mThermalControl, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testMode() {
        mThermalControlImpl.publish();

        verifyZeroInteractions(mMockBackend);

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mThermalControl.mode(), allOf(
                enumSettingSupports(EnumSet.noneOf(ThermalControl.Mode.class)),
                settingIsUpToDate()));

        // mock low-level sends supported modes and current mode
        mThermalControlImpl.mode()
                           .updateAvailableValues(EnumSet.allOf(ThermalControl.Mode.class))
                           .updateValue(ThermalControl.Mode.DISABLED);
        mThermalControlImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mThermalControl.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.DISABLED),
                settingIsUpToDate()));

        // test nothing changes if low-level denies
        doReturn(false).when(mMockBackend).setMode(any());
        mThermalControl.mode().setValue(ThermalControl.Mode.STANDARD);

        verify(mMockBackend, times(1)).setMode(ThermalControl.Mode.STANDARD);
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mThermalControl.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.DISABLED),
                settingIsUpToDate()));

        mThermalControl.mode().setValue(ThermalControl.Mode.EMBEDDED);

        verify(mMockBackend, times(1)).setMode(ThermalControl.Mode.EMBEDDED);

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mThermalControl.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.DISABLED),
                settingIsUpToDate()));


        // test nothing changes if values do not change
        doReturn(true).when(mMockBackend).setMode(any());
        mThermalControl.mode().setValue(ThermalControl.Mode.DISABLED);

        verify(mMockBackend, never()).setMode(ThermalControl.Mode.DISABLED);
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mThermalControl.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.DISABLED),
                settingIsUpToDate()));

        // test user changes value to standard
        mThermalControl.mode().setValue(ThermalControl.Mode.STANDARD);

        verify(mMockBackend, times(2)).setMode(ThermalControl.Mode.STANDARD);
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mThermalControl.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.STANDARD),
                settingIsUpdating()));

        // mock update from low-level
        mThermalControlImpl.mode().updateValue(ThermalControl.Mode.STANDARD);

        // verify changes are not published until notifyUpdated() is called
        assertThat(mComponentChangeCnt, is((3)));

        mThermalControlImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mThermalControl.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.STANDARD),
                settingIsUpToDate()));

        // test user changes value to embedded
        mThermalControl.mode().setValue(ThermalControl.Mode.EMBEDDED);

        verify(mMockBackend, times(2)).setMode(ThermalControl.Mode.EMBEDDED);
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mThermalControl.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.EMBEDDED),
                settingIsUpdating()));

        // mock update from low-level
        mThermalControlImpl.mode().updateValue(ThermalControl.Mode.EMBEDDED);

        // verify changes are not published until notifyUpdated() is called
        assertThat(mComponentChangeCnt, is((5)));

        mThermalControlImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mThermalControl.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.EMBEDDED),
                settingIsUpToDate()));

        verifyNoMoreInteractions(mMockBackend);
    }

    @Test
    public void testSensitivity() {
        mThermalControlImpl.publish();

        verifyZeroInteractions(mMockBackend);

        doReturn(true).when(mMockBackend).setSensitivity(any());

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mThermalControl.sensitivity(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Sensitivity.class)),
                enumSettingValueIs(ThermalControl.Sensitivity.HIGH_RANGE),
                settingIsUpToDate()));

        // test user changes value
        mThermalControl.sensitivity().setValue(ThermalControl.Sensitivity.LOW_RANGE);

        verify(mMockBackend, times(1)).setSensitivity(ThermalControl.Sensitivity.LOW_RANGE);
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mThermalControl.sensitivity(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Sensitivity.class)),
                enumSettingValueIs(ThermalControl.Sensitivity.LOW_RANGE),
                settingIsUpdating()));

        // mock update from low-level
        mThermalControlImpl.sensitivity().updateValue(ThermalControl.Sensitivity.LOW_RANGE);
        mThermalControlImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is((3)));
        assertThat(mThermalControl.sensitivity(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Sensitivity.class)),
                enumSettingValueIs(ThermalControl.Sensitivity.LOW_RANGE),
                settingIsUpToDate()));

        verifyNoMoreInteractions(mMockBackend);
    }

    @Test
    public void testCalibration() {
        mThermalControlImpl.publish();

        verifyZeroInteractions(mMockBackend);

        // test calibration is not provided by default
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mThermalControl.calibration(), nullValue());

        // mock calibration available
        ThermalControlCore.CalibrationCore calibrationImpl = mThermalControlImpl.createCalibrationIfNeeded();

        ThermalControl.Calibration calibration = mThermalControl.calibration();
        assertThat(calibration, notNullValue());

        // test calibration initial values
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.noneOf(ThermalControl.Calibration.Mode.class)),
                settingIsUpToDate()));

        // mock low-level sends supported modes and current mode
        calibrationImpl.mode()
                       .updateAvailableValues(EnumSet.allOf(ThermalControl.Calibration.Mode.class))
                       .updateValue(ThermalControl.Calibration.Mode.AUTOMATIC);
        mThermalControlImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.AUTOMATIC),
                settingIsUpToDate()));

        // test nothing changes if low-level denies
        doReturn(false).when(mMockBackend).setCalibrationMode(any());
        calibration.mode().setValue(ThermalControl.Calibration.Mode.MANUAL);

        verify(mMockBackend, times(1)).setCalibrationMode(ThermalControl.Calibration.Mode.MANUAL);
        assertThat(mComponentChangeCnt, is(2));
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.AUTOMATIC),
                settingIsUpToDate()));

        // test nothing changes if values do not change
        doReturn(true).when(mMockBackend).setCalibrationMode(any());
        calibration.mode().setValue(ThermalControl.Calibration.Mode.AUTOMATIC);

        verify(mMockBackend, never()).setCalibrationMode(ThermalControl.Calibration.Mode.AUTOMATIC);
        assertThat(mComponentChangeCnt, is(2));
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.AUTOMATIC),
                settingIsUpToDate()));

        // test user changes value
        calibration.mode().setValue(ThermalControl.Calibration.Mode.MANUAL);

        verify(mMockBackend, times(2)).setCalibrationMode(ThermalControl.Calibration.Mode.MANUAL);
        assertThat(mComponentChangeCnt, is(3));
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.MANUAL),
                settingIsUpdating()));

        // mock update from low-level
        calibrationImpl.mode().updateValue(ThermalControl.Calibration.Mode.MANUAL);

        // verify changes are not published until notifyUpdated() is called
        assertThat(mComponentChangeCnt, is((3)));

        mThermalControlImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.MANUAL),
                settingIsUpToDate()));

        // test manual calibration trigger
        calibration.calibrate();
        verify(mMockBackend).calibrate();

        verifyNoMoreInteractions(mMockBackend);
    }

    @Test
    public void testEmissivity() {
        mThermalControlImpl.publish();

        verifyZeroInteractions(mMockBackend);

        // test user sets emissivity
        mThermalControl.sendEmissivity(0.3);
        verify(mMockBackend).sendEmissivity(0.3);

        // test out of range emissivity
        mThermalControl.sendEmissivity(-0.5);
        verify(mMockBackend).sendEmissivity(0.0);

        // test out of range emissivity
        mThermalControl.sendEmissivity(1.2);
        verify(mMockBackend).sendEmissivity(1.0);
    }

    @Test
    public void testBackgroundTemperature() {
        mThermalControlImpl.publish();

        verifyZeroInteractions(mMockBackend);

        mThermalControl.sendBackgroundTemperature(123.4);
        verify(mMockBackend).sendBackgroundTemperature(123.4);
    }

    @Test
    public void testPalette() {
        mThermalControlImpl.publish();

        verifyZeroInteractions(mMockBackend);

        ThermalControl.Palette palette = mock(ThermalControl.Palette.class);

        mThermalControl.sendPalette(palette);
        verify(mMockBackend).sendPalette(palette);
    }

    @Test
    public void testRendering() {
        mThermalControlImpl.publish();

        verifyZeroInteractions(mMockBackend);

        ThermalControl.Rendering rendering = mock(ThermalControl.Rendering.class);

        mThermalControl.sendRendering(rendering);
        verify(mMockBackend).sendRendering(rendering);
    }

    @Test
    public void testCancelRollbacks() {
        doReturn(true).when(mMockBackend).setMode(any());
        doReturn(true).when(mMockBackend).setSensitivity(any());
        doReturn(true).when(mMockBackend).setCalibrationMode(any());

        mThermalControlImpl.mode()
                           .updateAvailableValues(EnumSet.allOf(ThermalControl.Mode.class))
                           .updateValue(ThermalControl.Mode.DISABLED);
        mThermalControlImpl.sensitivity()
                           .updateAvailableValues(EnumSet.allOf(ThermalControl.Sensitivity.class))
                           .updateValue(ThermalControl.Sensitivity.LOW_RANGE);

        ThermalControlCore.CalibrationCore calibrationImpl = mThermalControlImpl.createCalibrationIfNeeded();
        calibrationImpl.mode()
                       .updateAvailableValues(EnumSet.allOf(ThermalControl.Calibration.Mode.class))
                       .updateValue(ThermalControl.Calibration.Mode.AUTOMATIC);

        mThermalControlImpl.publish();

        assertThat(mThermalControl.mode(), allOf(
                enumSettingValueIs(ThermalControl.Mode.DISABLED),
                settingIsUpToDate()));

        assertThat(mThermalControl.sensitivity(), allOf(
                enumSettingValueIs(ThermalControl.Sensitivity.LOW_RANGE),
                settingIsUpToDate()));

        ThermalControl.Calibration calibration = mThermalControl.calibration();
        assertThat(calibration, notNullValue());
        assertThat(calibration.mode(), allOf(
                enumSettingValueIs(ThermalControl.Calibration.Mode.AUTOMATIC),
                settingIsUpToDate()));

        // mock user changes settings
        mThermalControl.mode().setValue(ThermalControl.Mode.STANDARD);
        mThermalControl.sensitivity().setValue(ThermalControl.Sensitivity.HIGH_RANGE);
        calibration.mode().setValue(ThermalControl.Calibration.Mode.MANUAL);

        // cancel all rollbacks
        mThermalControlImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mThermalControl.mode(), allOf(
                enumSettingValueIs(ThermalControl.Mode.STANDARD),
                settingIsUpToDate()));

        assertThat(mThermalControl.sensitivity(), allOf(
                enumSettingValueIs(ThermalControl.Sensitivity.HIGH_RANGE),
                settingIsUpToDate()));

        assertThat(calibration.mode(), allOf(
                enumSettingValueIs(ThermalControl.Calibration.Mode.MANUAL),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mThermalControl.mode(), allOf(
                enumSettingValueIs(ThermalControl.Mode.STANDARD),
                settingIsUpToDate()));

        assertThat(mThermalControl.sensitivity(), allOf(
                enumSettingValueIs(ThermalControl.Sensitivity.HIGH_RANGE),
                settingIsUpToDate()));

        assertThat(calibration.mode(), allOf(
                enumSettingValueIs(ThermalControl.Calibration.Mode.MANUAL),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }
}
