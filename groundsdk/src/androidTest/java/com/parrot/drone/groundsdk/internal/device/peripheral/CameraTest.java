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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.MainCamera;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.camera.Camera;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraAlignment;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraEvCompensation;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposure;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposureLock;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraRecording;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraStyle;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalance;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalanceLock;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraZoom;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraAlignmentSettingCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraPhotoSettingCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraRecordingSettingCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraWhiteBalanceLockCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraZoomCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.MainCameraCore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdk.value.IntegerRange;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.AlignmentSettingMatcher.alignmentSettingPitchIs;
import static com.parrot.drone.groundsdk.AlignmentSettingMatcher.alignmentSettingRollIs;
import static com.parrot.drone.groundsdk.AlignmentSettingMatcher.alignmentSettingYawIs;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabling;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabling;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingValueIs;
import static com.parrot.drone.groundsdk.CameraExposureLockMatcher.exposureLockModeIsUpdatedAt;
import static com.parrot.drone.groundsdk.CameraExposureLockMatcher.exposureLockModeIsUpdatingTo;
import static com.parrot.drone.groundsdk.CameraWhiteBalanceLockMatcher.whiteBalanceIsLockable;
import static com.parrot.drone.groundsdk.CameraWhiteBalanceLockMatcher.whiteBalanceLockIsUpToDateAt;
import static com.parrot.drone.groundsdk.CameraWhiteBalanceLockMatcher.whiteBalanceLockIsUpdatingTo;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingValueIs;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingSupports;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.ExposureSettingMatcher.exposureSettingAutoExposureMeteringModeIs;
import static com.parrot.drone.groundsdk.ExposureSettingMatcher.exposureSettingManualIsoIs;
import static com.parrot.drone.groundsdk.ExposureSettingMatcher.exposureSettingManualShutterSpeedIs;
import static com.parrot.drone.groundsdk.ExposureSettingMatcher.exposureSettingMaxIsoIs;
import static com.parrot.drone.groundsdk.ExposureSettingMatcher.exposureSettingModeIs;
import static com.parrot.drone.groundsdk.ExposureSettingMatcher.exposureSettingSupportsAutoExposureMeteringMode;
import static com.parrot.drone.groundsdk.ExposureSettingMatcher.exposureSettingSupportsManualIsos;
import static com.parrot.drone.groundsdk.ExposureSettingMatcher.exposureSettingSupportsManualShutterSpeeds;
import static com.parrot.drone.groundsdk.ExposureSettingMatcher.exposureSettingSupportsMaxIsos;
import static com.parrot.drone.groundsdk.ExposureSettingMatcher.exposureSettingSupportsModes;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingValueIs;
import static com.parrot.drone.groundsdk.OptionalSettingMatcher.optionalSettingIsAvailable;
import static com.parrot.drone.groundsdk.OptionalSettingMatcher.optionalSettingIsUnavailable;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingBracketingValueIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingBurstValueIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingFileFormatIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingFormatIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingGpslapseIntervalIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingGpslapseIntervalRangeIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingHdrAvailableIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingModeIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingSupportsBracketingValues;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingSupportsBurstValues;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingSupportsFileFormats;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingSupportsFormats;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingSupportsModes;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingTimelapseIntervalIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingTimelapseIntervalRangeIs;
import static com.parrot.drone.groundsdk.PhotoStateMatcher.photoCountIs;
import static com.parrot.drone.groundsdk.PhotoStateMatcher.photoMediaIdIs;
import static com.parrot.drone.groundsdk.PhotoStateMatcher.photoStateIs;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingFramerateIs;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingHdrAvailableIs;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingHyperlapseValueIs;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingModeIs;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingResolutionIs;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingSupportsFramerates;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingSupportsHyperlapseValues;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingSupportsModes;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingSupportsResolutions;
import static com.parrot.drone.groundsdk.RecordingStateMatcher.recordingMediaIdIs;
import static com.parrot.drone.groundsdk.RecordingStateMatcher.recordingStartTimeIs;
import static com.parrot.drone.groundsdk.RecordingStateMatcher.recordingStateIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static com.parrot.drone.groundsdk.StyleSettingMatcher.styleSettingContrastIs;
import static com.parrot.drone.groundsdk.StyleSettingMatcher.styleSettingSaturationIs;
import static com.parrot.drone.groundsdk.StyleSettingMatcher.styleSettingSharpnessIs;
import static com.parrot.drone.groundsdk.StyleSettingMatcher.styleSettingStyleIs;
import static com.parrot.drone.groundsdk.StyleSettingMatcher.styleSettingSupportsStyles;
import static com.parrot.drone.groundsdk.WhiteBalanceSettingMatcher.whiteBalanceSettingModeIs;
import static com.parrot.drone.groundsdk.WhiteBalanceSettingMatcher.whiteBalanceSettingSupportsModes;
import static com.parrot.drone.groundsdk.WhiteBalanceSettingMatcher.whiteBalanceSettingSupportsTemperatures;
import static com.parrot.drone.groundsdk.WhiteBalanceSettingMatcher.whiteBalanceSettingTemperatureIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class CameraTest {

    private MockComponentStore<Peripheral> mStore;

    private CameraCore mCameraImpl;

    private Camera mCamera;

    private Backend mBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mCameraImpl = new MainCameraCore(mStore, mBackend);
        mCamera = mStore.get(MainCamera.class);
        mStore.registerObserver(MainCamera.class, () -> {
            mCamera = mStore.get(MainCamera.class);
            mComponentChangeCnt++;
        });

        mComponentChangeCnt = 0;
    }

    @After
    public void tearDown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mComponentChangeCnt, is(0));
        assertThat(mCamera, nullValue());

        mCameraImpl.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera, is(mCameraImpl));

        mCameraImpl.unpublish();

        assertThat(mCamera, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testActiveState() {
        mCameraImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.isActive(), is(false));

        // test backend updates value
        mCameraImpl.updateActiveFlag(true).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.isActive(), is(true));
    }

    @Test
    public void testMode() {
        mCameraImpl.mode().updateAvailableValues(EnumSet.of(Camera.Mode.RECORDING, Camera.Mode.PHOTO));
        mCameraImpl.publish();

        // test capabilities values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.mode(), enumSettingSupports(EnumSet.of(Camera.Mode.RECORDING, Camera.Mode.PHOTO)));

        // test backend change notification
        mCameraImpl.mode().updateValue(Camera.Mode.PHOTO);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.mode(), enumSettingIsUpToDateAt(Camera.Mode.PHOTO));

        // test change value
        mCamera.mode().setValue(Camera.Mode.RECORDING);

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mMode, is(Camera.Mode.RECORDING));
        assertThat(mCamera.mode(), enumSettingIsUpdatingTo(Camera.Mode.RECORDING));

        mCameraImpl.mode().updateValue(Camera.Mode.RECORDING);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.mode(), enumSettingIsUpToDateAt(Camera.Mode.RECORDING));

        // test change to unsupported value
        mCameraImpl.mode().updateAvailableValues(EnumSet.of(Camera.Mode.RECORDING));
        mCamera.mode().setValue(Camera.Mode.PHOTO);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mBackend.mMode, is(Camera.Mode.RECORDING));
        assertThat(mCamera.mode(), enumSettingIsUpToDateAt(Camera.Mode.RECORDING));
    }

    @Test
    public void testExposure() {
        mCameraImpl.exposure()
                   .updateSupportedModes(EnumSet.of(CameraExposure.Mode.AUTOMATIC,
                           CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY,
                           CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED,
                           CameraExposure.Mode.MANUAL_SHUTTER_SPEED, CameraExposure.Mode.MANUAL))
                   .updateSupportedShutterSpeeds(EnumSet.of(CameraExposure.ShutterSpeed.ONE_OVER_10000,
                           CameraExposure.ShutterSpeed.ONE_OVER_1000, CameraExposure.ShutterSpeed.ONE))
                   .updateSupportedIsoSensitivities(EnumSet.of(CameraExposure.IsoSensitivity.ISO_50,
                           CameraExposure.IsoSensitivity.ISO_200, CameraExposure.IsoSensitivity.ISO_1200))
                   .updateMaximumIsoSensitivities(EnumSet.of(CameraExposure.IsoSensitivity.ISO_64,
                           CameraExposure.IsoSensitivity.ISO_160, CameraExposure.IsoSensitivity.ISO_1600))
                   .updateSupportedAutoExposureMeteringModes(EnumSet.of(
                           CameraExposure.AutoExposureMeteringMode.STANDARD,
                           CameraExposure.AutoExposureMeteringMode.CENTER_TOP));

        mCameraImpl.publish();

        // test capabilities values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingSupportsModes(EnumSet.of(CameraExposure.Mode.AUTOMATIC,
                        CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY,
                        CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED,
                        CameraExposure.Mode.MANUAL_SHUTTER_SPEED, CameraExposure.Mode.MANUAL)),
                exposureSettingSupportsManualShutterSpeeds(EnumSet.of(CameraExposure.ShutterSpeed.ONE_OVER_10000,
                        CameraExposure.ShutterSpeed.ONE_OVER_1000, CameraExposure.ShutterSpeed.ONE)),
                exposureSettingSupportsManualIsos(EnumSet.of(CameraExposure.IsoSensitivity.ISO_50,
                        CameraExposure.IsoSensitivity.ISO_200, CameraExposure.IsoSensitivity.ISO_1200)),
                exposureSettingSupportsMaxIsos(EnumSet.of(CameraExposure.IsoSensitivity.ISO_64,
                        CameraExposure.IsoSensitivity.ISO_160, CameraExposure.IsoSensitivity.ISO_1600)),
                exposureSettingSupportsAutoExposureMeteringMode(
                        EnumSet.of(CameraExposure.AutoExposureMeteringMode.STANDARD,
                        CameraExposure.AutoExposureMeteringMode.CENTER_TOP))));

        // test backend change notification
        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.MANUAL)
                   .updateShutterSpeed(CameraExposure.ShutterSpeed.ONE_OVER_1000)
                   .updateIsoSensitivity(CameraExposure.IsoSensitivity.ISO_200)
                   .updateMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_1600)
                   .updateAutoExposureMeteringMode(CameraExposure.AutoExposureMeteringMode.CENTER_TOP);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_1000),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_200),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_1600),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.CENTER_TOP),
                settingIsUpToDate()));

        // test individual setters

        // mode
        mCamera.exposure().setMode(CameraExposure.Mode.MANUAL_SHUTTER_SPEED);

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.MANUAL_SHUTTER_SPEED));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                settingIsUpdating()));

        mCameraImpl.exposure().updateMode(CameraExposure.Mode.MANUAL_SHUTTER_SPEED);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                settingIsUpToDate()));

        // unsupported value
        mCamera.exposure().setMode(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.MANUAL_SHUTTER_SPEED));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                settingIsUpToDate()));

        // move to manual to test shutter speed and iso sensitivity
        mCamera.exposure().setMode(CameraExposure.Mode.MANUAL);

        assertThat(mComponentChangeCnt, is(5));

        mCameraImpl.exposure().updateMode(CameraExposure.Mode.MANUAL);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(6));

        // shutter speed
        mCamera.exposure().setManualShutterSpeed(CameraExposure.ShutterSpeed.ONE);

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mBackend.mShutterSpeed, is(CameraExposure.ShutterSpeed.ONE));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE),
                settingIsUpdating()));

        mCameraImpl.exposure().updateShutterSpeed(CameraExposure.ShutterSpeed.ONE);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE),
                settingIsUpToDate()));

        // unsupported value
        mCamera.exposure().setManualShutterSpeed(CameraExposure.ShutterSpeed.ONE_OVER_8);

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mBackend.mShutterSpeed, is(CameraExposure.ShutterSpeed.ONE));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE),
                settingIsUpToDate()));

        // iso sensitivity
        mCamera.exposure().setManualIsoSensitivity(CameraExposure.IsoSensitivity.ISO_1200);

        assertThat(mComponentChangeCnt, is(9));
        assertThat(mBackend.mIso, is(CameraExposure.IsoSensitivity.ISO_1200));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_1200),
                settingIsUpdating()));

        mCameraImpl.exposure().updateIsoSensitivity(CameraExposure.IsoSensitivity.ISO_1200);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_1200),
                settingIsUpToDate()));

        // unsupported value
        mCamera.exposure().setManualIsoSensitivity(CameraExposure.IsoSensitivity.ISO_400);

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mBackend.mIso, is(CameraExposure.IsoSensitivity.ISO_1200));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_1200),
                settingIsUpToDate()));

        // move to automatic to test maximum iso sensitivity & automatic exposure metering mode
        mCamera.exposure().setMode(CameraExposure.Mode.AUTOMATIC);

        assertThat(mComponentChangeCnt, is(11));

        mCameraImpl.exposure().updateMode(CameraExposure.Mode.AUTOMATIC);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(12));

        // maximum iso sensitivity
        mCamera.exposure().setMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_64);

        assertThat(mComponentChangeCnt, is(13));
        assertThat(mBackend.mMaxIso, is(CameraExposure.IsoSensitivity.ISO_64));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_64),
                settingIsUpdating()));

        mCameraImpl.exposure().updateMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_64);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(14));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_64),
                settingIsUpToDate()));

        // unsupported value
        mCamera.exposure().setMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_400);

        assertThat(mComponentChangeCnt, is(14));
        assertThat(mBackend.mMaxIso, is(CameraExposure.IsoSensitivity.ISO_64));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_64),
                settingIsUpToDate()));

        // automatic exposure metering mode
        mCamera.exposure().setAutoExposureMeteringMode(CameraExposure.AutoExposureMeteringMode.STANDARD);

        assertThat(mComponentChangeCnt, is(15));
        assertThat(mBackend.mAutoExposureMeteringMode, is(CameraExposure.AutoExposureMeteringMode.STANDARD));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.STANDARD),
                settingIsUpdating()));

        mCameraImpl.exposure().updateAutoExposureMeteringMode(CameraExposure.AutoExposureMeteringMode.STANDARD);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(16));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.STANDARD),
                settingIsUpToDate()));

        // unsupported value
        mCameraImpl.exposure().updateSupportedAutoExposureMeteringModes(EnumSet.of(
                CameraExposure.AutoExposureMeteringMode.STANDARD));
        mCameraImpl.publish();
        assertThat(mComponentChangeCnt, is(17));
        mCamera.exposure().setAutoExposureMeteringMode(CameraExposure.AutoExposureMeteringMode.CENTER_TOP);

        assertThat(mComponentChangeCnt, is(17));
        assertThat(mBackend.mAutoExposureMeteringMode, is(CameraExposure.AutoExposureMeteringMode.STANDARD));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.STANDARD),
                settingIsUpToDate()));

        // adding AutoExposureMeteringMode#CENTER_TOP capability
        mCameraImpl.exposure().updateSupportedAutoExposureMeteringModes(EnumSet.of(
                CameraExposure.AutoExposureMeteringMode.STANDARD, CameraExposure.AutoExposureMeteringMode.CENTER_TOP));
        mCameraImpl.publish();

        assertThat(mComponentChangeCnt, is(18));


        // test global setters

        // auto mode with max iso sensitivity
        mBackend.mExposureMode = null;
        mBackend.mMaxIso = null;
        mCamera.exposure().setAutoMode(CameraExposure.IsoSensitivity.ISO_160);

        assertThat(mComponentChangeCnt, is(19));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.AUTOMATIC));
        assertThat(mBackend.mMaxIso, is(CameraExposure.IsoSensitivity.ISO_160));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_160),
                settingIsUpdating()));

        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.AUTOMATIC)
                   .updateMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_160);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(20));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_160),
                settingIsUpToDate()));

        // auto mode with auto exposure metering mode
        mBackend.mExposureMode = null;
        mBackend.mAutoExposureMeteringMode = null;
        mCamera.exposure().setAutoMode(CameraExposure.AutoExposureMeteringMode.CENTER_TOP);

        assertThat(mComponentChangeCnt, is(21));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.AUTOMATIC));
        assertThat(mBackend.mAutoExposureMeteringMode, is(CameraExposure.AutoExposureMeteringMode.CENTER_TOP));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.CENTER_TOP),
                settingIsUpdating()));

        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.AUTOMATIC)
                   .updateAutoExposureMeteringMode(CameraExposure.AutoExposureMeteringMode.CENTER_TOP);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(22));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.CENTER_TOP),
                settingIsUpToDate()));

        // auto mode with both (max iso sensitivity and auto exposure metering mode)
        mBackend.mExposureMode = null;
        mBackend.mMaxIso = null;
        mBackend.mAutoExposureMeteringMode = null;
        mCamera.exposure().setAutoMode(CameraExposure.IsoSensitivity.ISO_160,
                CameraExposure.AutoExposureMeteringMode.STANDARD);

        assertThat(mComponentChangeCnt, is(23));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.AUTOMATIC));
        assertThat(mBackend.mMaxIso, is(CameraExposure.IsoSensitivity.ISO_160));
        assertThat(mBackend.mAutoExposureMeteringMode, is(CameraExposure.AutoExposureMeteringMode.STANDARD));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_160),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.STANDARD),
                settingIsUpdating()));

        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.AUTOMATIC)
                   .updateMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_160)
                   .updateAutoExposureMeteringMode(CameraExposure.AutoExposureMeteringMode.STANDARD);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(24));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_160),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.STANDARD),
                settingIsUpToDate()));

        // auto prefer iso mode with max iso sensitivity
        mBackend.mExposureMode = null;
        mBackend.mMaxIso = null;
        mCamera.exposure().setAutoPreferIsoSensitivityMode(CameraExposure.IsoSensitivity.ISO_1600);

        assertThat(mComponentChangeCnt, is(25));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY));
        assertThat(mBackend.mMaxIso, is(CameraExposure.IsoSensitivity.ISO_1600));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_1600),
                settingIsUpdating()));

        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY)
                   .updateMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_1600);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(26));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_1600),
                settingIsUpToDate()));

        // auto prefer iso mode with auto exposure metering mode
        mBackend.mExposureMode = null;
        mBackend.mAutoExposureMeteringMode = null;
        mCamera.exposure().setAutoPreferIsoSensitivityMode(CameraExposure.AutoExposureMeteringMode.CENTER_TOP);

        assertThat(mComponentChangeCnt, is(27));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY));
        assertThat(mBackend.mAutoExposureMeteringMode, is(CameraExposure.AutoExposureMeteringMode.CENTER_TOP));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.CENTER_TOP),
                settingIsUpdating()));

        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY)
                   .updateAutoExposureMeteringMode(CameraExposure.AutoExposureMeteringMode.CENTER_TOP);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(28));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.CENTER_TOP),
                settingIsUpToDate()));

        // auto prefer iso mode with both (max iso sensitivity and auto exposure metering mode)
        mBackend.mExposureMode = null;
        mBackend.mMaxIso = null;
        mBackend.mAutoExposureMeteringMode = null;
        mCamera.exposure().setAutoPreferIsoSensitivityMode(CameraExposure.IsoSensitivity.ISO_64,
                CameraExposure.AutoExposureMeteringMode.STANDARD);

        assertThat(mComponentChangeCnt, is(29));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY));
        assertThat(mBackend.mMaxIso, is(CameraExposure.IsoSensitivity.ISO_64));
        assertThat(mBackend.mAutoExposureMeteringMode, is(CameraExposure.AutoExposureMeteringMode.STANDARD));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_64),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.STANDARD),
                settingIsUpdating()));

        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY)
                   .updateMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_64)
                   .updateAutoExposureMeteringMode(CameraExposure.AutoExposureMeteringMode.CENTER_TOP);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(30));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_64),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.CENTER_TOP),
                settingIsUpToDate()));

        // auto prefer shutter speed mode with max iso sensitivity
        mBackend.mExposureMode = null;
        mBackend.mMaxIso = null;
        mCamera.exposure().setAutoPreferShutterSpeedMode(CameraExposure.IsoSensitivity.ISO_64);

        assertThat(mComponentChangeCnt, is(31));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED));
        assertThat(mBackend.mMaxIso, is(CameraExposure.IsoSensitivity.ISO_64));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_64),
                settingIsUpdating()));

        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED)
                   .updateMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_64);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(32));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_64),
                settingIsUpToDate()));

        // auto prefer shutter speed mode with auto exposure metering mode
        mBackend.mExposureMode = null;
        mBackend.mAutoExposureMeteringMode = null;
        mCamera.exposure().setAutoPreferShutterSpeedMode(CameraExposure.AutoExposureMeteringMode.STANDARD);

        assertThat(mComponentChangeCnt, is(33));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED));
        assertThat(mBackend.mAutoExposureMeteringMode, is(CameraExposure.AutoExposureMeteringMode.STANDARD));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.STANDARD),
                settingIsUpdating()));

        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED)
                   .updateAutoExposureMeteringMode(CameraExposure.AutoExposureMeteringMode.STANDARD);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(34));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.STANDARD),
                settingIsUpToDate()));

        // auto prefer shutter speed mode with both (max iso sensitivity and auto exposure metering mode)
        mBackend.mExposureMode = null;
        mBackend.mMaxIso = null;
        mBackend.mAutoExposureMeteringMode = null;
        mCamera.exposure().setAutoPreferShutterSpeedMode(CameraExposure.IsoSensitivity.ISO_1600,
                CameraExposure.AutoExposureMeteringMode.CENTER_TOP);

        assertThat(mComponentChangeCnt, is(35));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED));
        assertThat(mBackend.mAutoExposureMeteringMode, is(CameraExposure.AutoExposureMeteringMode.CENTER_TOP));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_1600),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.CENTER_TOP),
                settingIsUpdating()));

        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED)
                   .updateMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_1600)
                   .updateAutoExposureMeteringMode(CameraExposure.AutoExposureMeteringMode.CENTER_TOP);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(36));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_1600),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.CENTER_TOP),
                settingIsUpToDate()));


        // manual shutter speed mode
        mBackend.mExposureMode = null;
        mBackend.mShutterSpeed = null;
        mCamera.exposure().setManualMode(CameraExposure.ShutterSpeed.ONE_OVER_10000);

        assertThat(mComponentChangeCnt, is(37));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.MANUAL_SHUTTER_SPEED));
        assertThat(mBackend.mShutterSpeed, is(CameraExposure.ShutterSpeed.ONE_OVER_10000));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_10000),
                settingIsUpdating()));

        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.MANUAL_SHUTTER_SPEED)
                   .updateShutterSpeed(CameraExposure.ShutterSpeed.ONE_OVER_10000);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(38));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_10000),
                settingIsUpToDate()));

        // add support of manual modes
        mCameraImpl.exposure().updateSupportedModes(EnumSet.of(
                CameraExposure.Mode.MANUAL,
                CameraExposure.Mode.MANUAL_SHUTTER_SPEED,
                CameraExposure.Mode.MANUAL_ISO_SENSITIVITY));
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(39));

        // manual iso sensitivity mode
        mBackend.mExposureMode = null;
        mBackend.mIso = null;
        mCamera.exposure().setManualMode(CameraExposure.IsoSensitivity.ISO_50);

        assertThat(mComponentChangeCnt, is(40));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY));
        assertThat(mBackend.mIso, is(CameraExposure.IsoSensitivity.ISO_50));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_50),
                settingIsUpdating()));

        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY)
                   .updateIsoSensitivity(CameraExposure.IsoSensitivity.ISO_50);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(41));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_50),
                settingIsUpToDate()));

        // manual mode (both shutter speed & iso sensitivity)
        mBackend.mExposureMode = null;
        mBackend.mShutterSpeed = null;
        mBackend.mIso = null;
        mCamera.exposure().setManualMode(CameraExposure.ShutterSpeed.ONE_OVER_1000,
                CameraExposure.IsoSensitivity.ISO_200);

        assertThat(mComponentChangeCnt, is(42));
        assertThat(mBackend.mExposureMode, is(CameraExposure.Mode.MANUAL));
        assertThat(mBackend.mShutterSpeed, is(CameraExposure.ShutterSpeed.ONE_OVER_1000));
        assertThat(mBackend.mIso, is(CameraExposure.IsoSensitivity.ISO_200));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_1000),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_200),
                settingIsUpdating()));

        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.MANUAL)
                   .updateShutterSpeed(CameraExposure.ShutterSpeed.ONE_OVER_1000)
                   .updateIsoSensitivity(CameraExposure.IsoSensitivity.ISO_200);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(43));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_1000),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_200),
                settingIsUpToDate()));
    }

    @Test
    public void testExposureTimeouts() {
        mCameraImpl.exposure()
                   .updateSupportedModes(EnumSet.allOf(CameraExposure.Mode.class))
                   .updateSupportedShutterSpeeds(EnumSet.allOf(CameraExposure.ShutterSpeed.class))
                   .updateSupportedIsoSensitivities(EnumSet.allOf(CameraExposure.IsoSensitivity.class))
                   .updateMaximumIsoSensitivities(EnumSet.allOf(CameraExposure.IsoSensitivity.class));
        mCameraImpl.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_50),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_3200),
                settingIsUpToDate()));

        // --- mode ---

        // mock user sets value
        mCamera.exposure().setMode(CameraExposure.Mode.MANUAL);

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL), settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.exposure().updateMode(CameraExposure.Mode.MANUAL);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL), settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL), settingIsUpToDate()));

        // mock user sets value
        mCamera.exposure().setMode(CameraExposure.Mode.MANUAL_SHUTTER_SPEED);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED), settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL), settingIsUpToDate()));

        // --- shutter speed ---

        // mock user sets value
        mCamera.exposure().setManualShutterSpeed(CameraExposure.ShutterSpeed.ONE_OVER_1_5);

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_1_5), settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.exposure().updateShutterSpeed(CameraExposure.ShutterSpeed.ONE_OVER_1_5);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_1_5), settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(7));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_1_5), settingIsUpToDate()));

        // mock user sets value
        mCamera.exposure().setManualShutterSpeed(CameraExposure.ShutterSpeed.ONE_OVER_2);

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_2), settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(9));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_1_5), settingIsUpToDate()));

        // --- iso sensitivity ---

        // mock user sets value
        mCamera.exposure().setManualIsoSensitivity(CameraExposure.IsoSensitivity.ISO_64);

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_64), settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.exposure().updateIsoSensitivity(CameraExposure.IsoSensitivity.ISO_64);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(11));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_64), settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(11));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_64), settingIsUpToDate()));

        // mock user sets value
        mCamera.exposure().setManualIsoSensitivity(CameraExposure.IsoSensitivity.ISO_80);

        assertThat(mComponentChangeCnt, is(12));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_80), settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(13));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_64), settingIsUpToDate()));

        // --- max iso sensitivity ---

        // switch mode to automatic to allow max iso to be forwarded to drone
        mCameraImpl.exposure().updateMode(CameraExposure.Mode.AUTOMATIC);

        // mock user sets value
        mCamera.exposure().setMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_2500);

        assertThat(mComponentChangeCnt, is(14));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_2500), settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.exposure().updateMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_2500);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(15));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_2500), settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(15));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_2500), settingIsUpToDate()));

        // mock user sets value
        mCamera.exposure().setMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_1600);

        assertThat(mComponentChangeCnt, is(16));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_1600), settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(17));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_2500), settingIsUpToDate()));

        // --- set auto mode ---

        // mock user sets value
        mCamera.exposure().setAutoMode(CameraExposure.IsoSensitivity.ISO_1200);

        assertThat(mComponentChangeCnt, is(18));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_1200), settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.AUTOMATIC)
                   .updateMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_1200);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(19));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_1200), settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(19));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_1200), settingIsUpToDate()));

        // mock user sets value
        mCamera.exposure().setAutoMode(CameraExposure.IsoSensitivity.ISO_800);

        assertThat(mComponentChangeCnt, is(20));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_800), settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(21));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_1200), settingIsUpToDate()));

        // --- set auto prefer shutter speed mode ---

        // mock user sets value
        mCamera.exposure().setAutoPreferShutterSpeedMode(CameraExposure.IsoSensitivity.ISO_640);

        assertThat(mComponentChangeCnt, is(22));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_640), settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED)
                   .updateMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_640);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(23));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_640), settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(23));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_640), settingIsUpToDate()));

        // mock user sets value
        mCamera.exposure().setAutoPreferShutterSpeedMode(CameraExposure.IsoSensitivity.ISO_500);

        assertThat(mComponentChangeCnt, is(24));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_500), settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(25));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_640), settingIsUpToDate()));

        // --- set auto prefer ISO sensitivity mode ---

        // mock user sets value
        mCamera.exposure().setAutoPreferIsoSensitivityMode(CameraExposure.IsoSensitivity.ISO_400);

        assertThat(mComponentChangeCnt, is(26));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_400), settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY)
                   .updateMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_400);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(27));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_400), settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(27));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_400), settingIsUpToDate()));

        // mock user sets value
        mCamera.exposure().setAutoPreferIsoSensitivityMode(CameraExposure.IsoSensitivity.ISO_320);

        assertThat(mComponentChangeCnt, is(28));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_320), settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(29));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_400), settingIsUpToDate()));

        // --- set manual mode, shutter speed only ---

        // mock user sets value
        mCamera.exposure().setManualMode(CameraExposure.ShutterSpeed.ONE_OVER_3);

        assertThat(mComponentChangeCnt, is(30));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_3), settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.MANUAL_SHUTTER_SPEED)
                   .updateShutterSpeed(CameraExposure.ShutterSpeed.ONE_OVER_3);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(31));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_3), settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(31));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_3), settingIsUpToDate()));

        // mock user sets value
        mCamera.exposure().setManualMode(CameraExposure.ShutterSpeed.ONE_OVER_4);

        assertThat(mComponentChangeCnt, is(32));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_4), settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(33));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_3), settingIsUpToDate()));

        // --- set manual mode, ISO sensitivity only ---

        // mock user sets value
        mCamera.exposure().setManualMode(CameraExposure.IsoSensitivity.ISO_100);

        assertThat(mComponentChangeCnt, is(34));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_100), settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY)
                   .updateIsoSensitivity(CameraExposure.IsoSensitivity.ISO_100);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(35));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_100), settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(35));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_100), settingIsUpToDate()));

        // mock user sets value
        mCamera.exposure().setManualMode(CameraExposure.IsoSensitivity.ISO_125);

        assertThat(mComponentChangeCnt, is(36));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_125), settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(37));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_100), settingIsUpToDate()));

        // --- set manual mode, both shutter speed and ISO sensitivity ----

        // mock user sets value
        mCamera.exposure().setManualMode(CameraExposure.ShutterSpeed.ONE_OVER_6, CameraExposure.IsoSensitivity.ISO_160);

        assertThat(mComponentChangeCnt, is(38));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_6),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_160), settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.exposure()
                   .updateMode(CameraExposure.Mode.MANUAL)
                   .updateShutterSpeed(CameraExposure.ShutterSpeed.ONE_OVER_6)
                   .updateIsoSensitivity(CameraExposure.IsoSensitivity.ISO_160);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(39));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_6),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_160), settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(39));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_6),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_160), settingIsUpToDate()));

        // mock user sets value
        mCamera.exposure().setManualMode(CameraExposure.ShutterSpeed.ONE_OVER_8, CameraExposure.IsoSensitivity.ISO_200);
        assertThat(mComponentChangeCnt, is(40));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_8),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_200), settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(41));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_6),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_160), settingIsUpToDate()));
    }

    @Test
    public void testExposureLock() {
        // we need the camera to be active to have exposure lock
        mCameraImpl.updateActiveFlag(true).publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.exposureLock(), nullValue());

        // make lock available
        mCameraImpl.updateExposureLock(CameraExposureLock.Mode.NONE, 0, 0, 0, 0).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.exposureLock(), exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.NONE));

        // change mode from api
        CameraExposureLock cameraExposureLock = mCamera.exposureLock();
        assert cameraExposureLock != null;
        cameraExposureLock.lockCurrentValues();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.exposureLock(), exposureLockModeIsUpdatingTo(CameraExposureLock.Mode.CURRENT_VALUES));
        assertThat(mBackend.mExposureLockMode, is(CameraExposureLock.Mode.CURRENT_VALUES));

        // update mode
        mCameraImpl.updateExposureLock(CameraExposureLock.Mode.CURRENT_VALUES, 0, 0, 0, 0).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.exposureLock(), exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.CURRENT_VALUES));

        // change mode from api
        cameraExposureLock.lockOnRegion(0.4, 0.8);
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mCamera.exposureLock(),
                exposureLockModeIsUpdatingTo(CameraExposureLock.Mode.REGION, 0.4, 0.8, 0.0, 0.0));
        assertThat(mBackend.mExposureLockMode, is(CameraExposureLock.Mode.REGION));
        assertThat(mBackend.mExposureLockCenterX, is(0.4));
        assertThat(mBackend.mExposureLockCenterY, is(0.8));

        // update mode
        mCameraImpl.updateExposureLock(CameraExposureLock.Mode.REGION, 0.4, 0.8, 0.2, 0.5).notifyUpdated();
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.exposureLock(),
                exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.REGION, 0.4, 0.8, 0.2, 0.5));

        // timeout should not change anything
        mockSettingTimeout();
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.exposureLock(),
                exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.REGION, 0.4, 0.8, 0.2, 0.5));

        // update with same value
        mCameraImpl.updateExposureLock(CameraExposureLock.Mode.REGION, 0.4, 0.8, 0.2, 0.5).notifyUpdated();
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.exposureLock(),
                exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.REGION, 0.4, 0.8, 0.2, 0.5));

        // change mode from api
        cameraExposureLock.unlock();
        assertThat(mComponentChangeCnt, is(7));
        assertThat(mCamera.exposureLock(), exposureLockModeIsUpdatingTo(CameraExposureLock.Mode.NONE));
        assertThat(mBackend.mExposureLockMode, is(CameraExposureLock.Mode.NONE));

        // update mode
        mCameraImpl.updateExposureLock(CameraExposureLock.Mode.NONE, 0.0, 0.0, 0.0, 0.0).notifyUpdated();
        assertThat(mComponentChangeCnt, is(8));
        assertThat(mCamera.exposureLock(), exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.NONE));

        // change mode from api
        cameraExposureLock.lockOnRegion(0.4, 0.8);
        assertThat(mComponentChangeCnt, is(9));
        assertThat(mCamera.exposureLock(),
                exposureLockModeIsUpdatingTo(CameraExposureLock.Mode.REGION, 0.4, 0.8, 0.0, 0.0));
        assertThat(mBackend.mExposureLockMode, is(CameraExposureLock.Mode.REGION));
        assertThat(mBackend.mExposureLockCenterX, is(0.4));
        assertThat(mBackend.mExposureLockCenterY, is(0.8));

        // mock timeout
        mockSettingTimeout();
        assertThat(mComponentChangeCnt, is(10));
        assertThat(mCamera.exposureLock(), exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.NONE));

        // update mode
        mCameraImpl.updateExposureLock(CameraExposureLock.Mode.REGION, 0.4, 0.8, 0.2, 0.5).notifyUpdated();
        assertThat(mComponentChangeCnt, is(11));
        assertThat(mCamera.exposureLock(),
                exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.REGION, 0.4, 0.8, 0.2, 0.5));

        // change mode from api
        cameraExposureLock.unlock();
        assertThat(mComponentChangeCnt, is(12));
        assertThat(mCamera.exposureLock(), exposureLockModeIsUpdatingTo(CameraExposureLock.Mode.NONE));
        assertThat(mBackend.mExposureLockMode, is(CameraExposureLock.Mode.NONE));

        // mock timeout
        mockSettingTimeout();
        assertThat(mComponentChangeCnt, is(13));
        assertThat(mCamera.exposureLock(),
                exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.REGION, 0.4, 0.8, 0.2, 0.5));
    }

    @Test
    public void testEvCompensation() {
        mCameraImpl.exposureCompensation().updateAvailableValues(EnumSet.of(CameraEvCompensation.EV_MINUS_3,
                CameraEvCompensation.EV_0, CameraEvCompensation.EV_3));

        mCameraImpl.publish();

        // test capabilities values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.exposureCompensation(), enumSettingSupports(EnumSet.of(CameraEvCompensation.EV_MINUS_3,
                CameraEvCompensation.EV_0, CameraEvCompensation.EV_3)));

        // test backend change notification
        mCameraImpl.exposureCompensation().updateValue(CameraEvCompensation.EV_3);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.exposureCompensation(), enumSettingIsUpToDateAt(CameraEvCompensation.EV_3));

        // test setter
        mCamera.exposureCompensation().setValue(CameraEvCompensation.EV_MINUS_3);

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mEvCompensation, is(CameraEvCompensation.EV_MINUS_3));
        assertThat(mCamera.exposureCompensation(), enumSettingIsUpdatingTo(CameraEvCompensation.EV_MINUS_3));

        mCameraImpl.exposureCompensation().updateValue(CameraEvCompensation.EV_MINUS_3);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.exposureCompensation(), enumSettingIsUpToDateAt(CameraEvCompensation.EV_MINUS_3));

        // unsupported value
        mCamera.exposureCompensation().setValue(CameraEvCompensation.EV_0_67);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.exposureCompensation(), enumSettingIsUpToDateAt(CameraEvCompensation.EV_MINUS_3));
    }

    @Test
    public void testWhiteBalance() {
        mCameraImpl.whiteBalance()
                   .updateSupportedModes(EnumSet.of(CameraWhiteBalance.Mode.AUTOMATIC, CameraWhiteBalance.Mode.SUNNY,
                           CameraWhiteBalance.Mode.SNOW, CameraWhiteBalance.Mode.CUSTOM))
                   .updateSupportedTemperatures(EnumSet.of(CameraWhiteBalance.Temperature.K_3000,
                           CameraWhiteBalance.Temperature.K_8000, CameraWhiteBalance.Temperature.K_10000));

        mCameraImpl.publish();

        // test capabilities values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingSupportsModes(EnumSet.of(CameraWhiteBalance.Mode.AUTOMATIC,
                        CameraWhiteBalance.Mode.SUNNY, CameraWhiteBalance.Mode.SNOW, CameraWhiteBalance.Mode.CUSTOM)),
                whiteBalanceSettingSupportsTemperatures(EnumSet.of(CameraWhiteBalance.Temperature.K_3000,
                        CameraWhiteBalance.Temperature.K_8000, CameraWhiteBalance.Temperature.K_10000))));

        // test backend change notification
        mCameraImpl.whiteBalance()
                   .updateMode(CameraWhiteBalance.Mode.CUSTOM)
                   .updateTemperature(CameraWhiteBalance.Temperature.K_3000);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_3000),
                settingIsUpToDate()));

        // test individual setters

        // mode
        mCamera.whiteBalance().setMode(CameraWhiteBalance.Mode.SNOW);

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mWhiteBalanceMode, is(CameraWhiteBalance.Mode.SNOW));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.SNOW),
                settingIsUpdating()));

        mCameraImpl.whiteBalance().updateMode(CameraWhiteBalance.Mode.SNOW);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.SNOW),
                settingIsUpToDate()));

        // unsupported value
        mCamera.whiteBalance().setMode(CameraWhiteBalance.Mode.SUNSET);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mBackend.mWhiteBalanceMode, is(CameraWhiteBalance.Mode.SNOW));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.SNOW),
                settingIsUpToDate()));

        // change to custom mode
        mCamera.whiteBalance().setMode(CameraWhiteBalance.Mode.CUSTOM);

        assertThat(mComponentChangeCnt, is(5));

        mCameraImpl.whiteBalance().updateMode(CameraWhiteBalance.Mode.CUSTOM);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(6));

        // custom temperature
        mCamera.whiteBalance().setCustomTemperature(CameraWhiteBalance.Temperature.K_8000);

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mBackend.mTemperature, is(CameraWhiteBalance.Temperature.K_8000));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_8000),
                settingIsUpdating()));

        mCameraImpl.whiteBalance().updateTemperature(CameraWhiteBalance.Temperature.K_8000);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_8000),
                settingIsUpToDate()));

        // unsupported value
        mCamera.whiteBalance().setCustomTemperature(CameraWhiteBalance.Temperature.K_3500);

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_8000),
                settingIsUpToDate()));

        // test global custom mode + temperature setter
        mBackend.mWhiteBalanceMode = null;
        mBackend.mTemperature = null;
        mCamera.whiteBalance().setCustomMode(CameraWhiteBalance.Temperature.K_10000);

        assertThat(mComponentChangeCnt, is(9));
        assertThat(mBackend.mWhiteBalanceMode, is(CameraWhiteBalance.Mode.CUSTOM));
        assertThat(mBackend.mTemperature, is(CameraWhiteBalance.Temperature.K_10000));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_10000),
                settingIsUpdating()));

        mCameraImpl.whiteBalance()
                   .updateMode(CameraWhiteBalance.Mode.CUSTOM)
                   .updateTemperature(CameraWhiteBalance.Temperature.K_10000);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_10000),
                settingIsUpToDate()));
    }

    @Test
    public void testWhiteBalanceTimeouts() {
        mCameraImpl.whiteBalance()
                   .updateSupportedModes(EnumSet.allOf(CameraWhiteBalance.Mode.class))
                   .updateSupportedTemperatures(EnumSet.allOf(CameraWhiteBalance.Temperature.class));

        mCameraImpl.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.AUTOMATIC),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_1500),
                settingIsUpToDate()));

        // --- mode ---

        // mock user sets value
        mCamera.whiteBalance().setMode(CameraWhiteBalance.Mode.CUSTOM);

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.whiteBalance().updateMode(CameraWhiteBalance.Mode.CUSTOM);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.whiteBalance().setMode(CameraWhiteBalance.Mode.BLUE_SKY);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.BLUE_SKY),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                settingIsUpToDate()));

        // --- custom temperature ---

        // mock user sets value
        mCamera.whiteBalance().setCustomTemperature(CameraWhiteBalance.Temperature.K_1750);

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_1750),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.whiteBalance().updateTemperature(CameraWhiteBalance.Temperature.K_1750);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_1750),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(7));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_1750),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.whiteBalance().setCustomTemperature(CameraWhiteBalance.Temperature.K_2000);

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_2000),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(9));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_1750),
                settingIsUpToDate()));

        // --- set custom mode (with temperature) ---

        // mock user sets value
        mCamera.whiteBalance().setCustomMode(CameraWhiteBalance.Temperature.K_2250);

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_2250),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.whiteBalance()
                   .updateMode(CameraWhiteBalance.Mode.CUSTOM)
                   .updateTemperature(CameraWhiteBalance.Temperature.K_2250);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(11));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_2250),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(11));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_2250),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.whiteBalance().setCustomMode(CameraWhiteBalance.Temperature.K_2500);

        assertThat(mComponentChangeCnt, is(12));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_2500),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(13));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_2250),
                settingIsUpToDate()));
    }

    @Test
    public void testWhiteBalanceLock() {
        // we need the camera to be active to have white balance lock
        mCameraImpl.updateActiveFlag(true).publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.whiteBalanceLock(), nullValue());

        // make lock available
        CameraWhiteBalanceLockCore whiteBalanceLockImpl = mCameraImpl.createWhiteBalanceLockIfNeeded();
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        CameraWhiteBalanceLock whiteBalanceLock = mCamera.whiteBalanceLock();
        assertThat(whiteBalanceLock, notNullValue());
        assertThat(whiteBalanceLock, whiteBalanceIsLockable(false));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(false));

        // lock white balance while not lockable
        whiteBalanceLock.lockCurrentValue(true);
        assertThat(mComponentChangeCnt, is(2));
        assertThat(whiteBalanceLock, whiteBalanceIsLockable(false));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(false));

        // update lockable state from backend
        whiteBalanceLockImpl.updateLockable(true);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(whiteBalanceLock, whiteBalanceIsLockable(true));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(false));

        // lock white balance from api
        whiteBalanceLock.lockCurrentValue(true);
        assertThat(mComponentChangeCnt, is(4));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpdatingTo(true));
        assertThat(mBackend.mWhiteBalanceLock, is(true));

        whiteBalanceLockImpl.updateLocked(true);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(true));

        // timeout should not change anything
        mockSettingTimeout();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(true));

        // update with same value
        whiteBalanceLockImpl.updateLocked(true);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(true));

        // lock from api with same value
        whiteBalanceLock.lockCurrentValue(true);
        assertThat(mComponentChangeCnt, is(5));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(true));

        // change mode from api
        whiteBalanceLock.lockCurrentValue(false);
        assertThat(mComponentChangeCnt, is(6));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpdatingTo(false));
        assertThat(mBackend.mWhiteBalanceLock, is(false));

        // mock timeout
        mockSettingTimeout();
        assertThat(mComponentChangeCnt, is(7));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(true));

        // update lock state from backend
        whiteBalanceLockImpl.updateLocked(false);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(8));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(false));
    }

    @Test
    public void testAutoHdr() {
        mCameraImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mAutoHdr, is(false));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsUnavailable(),
                settingIsUpToDate()));

        // change setting, should do nothing since unavailable
        mCamera.autoHdr().toggle();
        assertThat(mBackend.mAutoHdr, is(false));
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsUnavailable(),
                settingIsUpToDate()));

        // mock backend notifies setting supported
        mCameraImpl.autoHdr().updateSupportedFlag(true);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsAvailable(),
                settingIsUpToDate()));

        // change setting
        mCamera.autoHdr().toggle();
        assertThat(mBackend.mAutoHdr, is(true));
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(true),
                optionalSettingIsAvailable(),
                settingIsUpdating()));

        // mock update from low-level
        mCameraImpl.autoHdr().updateValue(true);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(true),
                optionalSettingIsAvailable(),
                settingIsUpToDate()));

        // change setting again
        mCamera.autoHdr().toggle();
        assertThat(mBackend.mAutoHdr, is(false));
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsAvailable(),
                settingIsUpdating()));

        // mock update from low-level
        mCameraImpl.autoHdr().updateValue(false);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsAvailable(),
                settingIsUpToDate()));
    }

    @Test
    public void testHdrState() {
        // we need the camera to be active to have HDR state
        mCameraImpl.updateActiveFlag(true).publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.isHdrActive(), is(false));

        // backend change
        mCameraImpl.updateHdrActive(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.isHdrActive(), is(true));
    }

    @Test
    public void testStyle() {
        mCameraImpl.style().updateSupportedStyles(EnumSet.of(CameraStyle.Style.STANDARD, CameraStyle.Style.PLOG));

        mCameraImpl.publish();

        // test supported styles
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.style(),
                styleSettingSupportsStyles(EnumSet.of(CameraStyle.Style.STANDARD, CameraStyle.Style.PLOG)));

        // test backend change notification
        mCameraImpl.style().updateStyle(CameraStyle.Style.PLOG);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.PLOG),
                settingIsUpToDate()));

        // change style
        mCamera.style().setStyle(CameraStyle.Style.STANDARD);

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mStyle, is(CameraStyle.Style.STANDARD));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                settingIsUpdating()));

        mCameraImpl.style().updateStyle(CameraStyle.Style.STANDARD);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                settingIsUpToDate()));

        // test change to unsupported mode
        mCameraImpl.style().updateSupportedStyles(EnumSet.of(CameraStyle.Style.STANDARD));
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(5));

        mCamera.style().setStyle(CameraStyle.Style.PLOG);

        assertThat(mComponentChangeCnt, is(5));
        assertThat(mBackend.mStyle, is(CameraStyle.Style.STANDARD));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                settingIsUpToDate()));
    }

    @Test
    public void testStyleParameter() {
        mCameraImpl.style().updateSupportedStyles(EnumSet.of(CameraStyle.Style.STANDARD));

        mCameraImpl.publish();

        // test supported styles
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.style(), styleSettingSupportsStyles(EnumSet.of(CameraStyle.Style.STANDARD)));

        // test backend change notification
        mCameraImpl.style().saturation().updateBounds(IntegerRange.of(-2, 2)).updateValue(1);
        mCameraImpl.style().contrast().updateBounds(IntegerRange.of(-4, 4)).updateValue(2);
        mCameraImpl.style().sharpness().updateBounds(IntegerRange.of(-6, 6)).updateValue(3);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(1, -2, 2),
                styleSettingContrastIs(2, -4, 4),
                styleSettingSharpnessIs(3, -6, 6),
                settingIsUpToDate()));

        // change saturation
        mCamera.style().saturation().setValue(-1);

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mSaturation, is(-1));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(-1, -2, 2),
                styleSettingContrastIs(2, -4, 4),
                styleSettingSharpnessIs(3, -6, 6),
                settingIsUpdating()));


        mCameraImpl.style().saturation().updateValue(-1);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(-1, -2, 2),
                styleSettingContrastIs(2, -4, 4),
                styleSettingSharpnessIs(3, -6, 6),
                settingIsUpToDate()));

        // change contrast
        mCamera.style().contrast().setValue(-2);

        assertThat(mComponentChangeCnt, is(5));
        assertThat(mBackend.mContrast, is(-2));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(-1, -2, 2),
                styleSettingContrastIs(-2, -4, 4),
                styleSettingSharpnessIs(3, -6, 6),
                settingIsUpdating()));

        mCameraImpl.style().contrast().updateValue(-2);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(-1, -2, 2),
                styleSettingContrastIs(-2, -4, 4),
                styleSettingSharpnessIs(3, -6, 6),
                settingIsUpToDate()));

        // change sharpness
        mCamera.style().sharpness().setValue(-3);

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mBackend.mSharpness, is(-3));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(-1, -2, 2),
                styleSettingContrastIs(-2, -4, 4),
                styleSettingSharpnessIs(-3, -6, 6),
                settingIsUpdating()));

        mCameraImpl.style().sharpness().updateValue(-3);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(-1, -2, 2),
                styleSettingContrastIs(-2, -4, 4),
                styleSettingSharpnessIs(-3, -6, 6),
                settingIsUpToDate()));
    }

    @Test
    public void testStyleParameterOutOfRange() {
        mCameraImpl.style().updateSupportedStyles(EnumSet.of(CameraStyle.Style.STANDARD));

        mCameraImpl.publish();

        // test supported styles
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.style(), styleSettingSupportsStyles(EnumSet.of(CameraStyle.Style.STANDARD)));

        // test backend change notification
        mCameraImpl.style().saturation().updateBounds(IntegerRange.of(-2, 2)).updateValue(1);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(1, -2, 2),
                settingIsUpToDate()));

        // change saturation to an out of range value
        mCamera.style().saturation().setValue(-5);

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mSaturation, is(-2));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(-2, -2, 2),
                settingIsUpdating()));

        mCameraImpl.style().saturation().updateValue(-2);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(-2, -2, 2),
                settingIsUpToDate()));
    }

    @Test
    public void testStyleParameterNonMutable() {
        mCameraImpl.style().updateSupportedStyles(EnumSet.of(CameraStyle.Style.STANDARD));

        mCameraImpl.publish();

        // test supported styles
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.style(), styleSettingSupportsStyles(EnumSet.of(CameraStyle.Style.STANDARD)));

        // test backend change notification
        mCameraImpl.style().saturation().updateBounds(IntegerRange.of(0, 0)).updateValue(0);
        mCameraImpl.style().contrast().updateBounds(IntegerRange.of(-4, 4)).updateValue(2);
        mCameraImpl.style().sharpness().updateBounds(IntegerRange.of(-6, 6)).updateValue(3);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(0, 0, 0),
                styleSettingContrastIs(2, -4, 4),
                styleSettingSharpnessIs(3, -6, 6),
                settingIsUpToDate()));
    }

    @Test
    public void testStyleSettingTimeouts() {
        mCameraImpl.style().updateSupportedStyles(EnumSet.allOf(CameraStyle.Style.class));
        mCameraImpl.style().saturation().updateBounds(IntegerRange.of(-2, 2)).updateValue(1);
        mCameraImpl.style().contrast().updateBounds(IntegerRange.of(-3, 3)).updateValue(2);
        mCameraImpl.style().sharpness().updateBounds(IntegerRange.of(-4, 4)).updateValue(3);

        mCameraImpl.publish();


        /// test initial values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(1, -2, 2),
                styleSettingContrastIs(2, -3, 3),
                styleSettingSharpnessIs(3, -4, 4),
                settingIsUpToDate()
        ));

        // --- style ---

        // mock user sets value
        mCamera.style().setStyle(CameraStyle.Style.PLOG);

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.PLOG),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.style().updateStyle(CameraStyle.Style.PLOG);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.PLOG),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.PLOG),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.style().setStyle(CameraStyle.Style.STANDARD);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.PLOG),
                settingIsUpToDate()));

        // --- saturation ---

        // mock user sets value
        mCamera.style().saturation().setValue(0);

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.style(), allOf(
                styleSettingSaturationIs(0, -2, 2),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.style().saturation().updateValue(0);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mCamera.style(), allOf(
                styleSettingSaturationIs(0, -2, 2),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(7));
        assertThat(mCamera.style(), allOf(
                styleSettingSaturationIs(0, -2, 2),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.style().saturation().setValue(-1);

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mCamera.style(), allOf(
                styleSettingSaturationIs(-1, -2, 2),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(9));
        assertThat(mCamera.style(), allOf(
                styleSettingSaturationIs(0, -2, 2),
                settingIsUpToDate()));

        // --- contrast ---

        // mock user sets value
        mCamera.style().contrast().setValue(0);

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mCamera.style(), allOf(
                styleSettingContrastIs(0, -3, 3),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.style().contrast().updateValue(0);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(11));
        assertThat(mCamera.style(), allOf(
                styleSettingContrastIs(0, -3, 3),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(11));
        assertThat(mCamera.style(), allOf(
                styleSettingContrastIs(0, -3, 3),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.style().contrast().setValue(-1);

        assertThat(mComponentChangeCnt, is(12));
        assertThat(mCamera.style(), allOf(
                styleSettingContrastIs(-1, -3, 3),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(13));
        assertThat(mCamera.style(), allOf(
                styleSettingContrastIs(0, -3, 3),
                settingIsUpToDate()));

        // --- sharpness ---

        // mock user sets value
        mCamera.style().sharpness().setValue(0);

        assertThat(mComponentChangeCnt, is(14));
        assertThat(mCamera.style(), allOf(
                styleSettingSharpnessIs(0, -4, 4),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.style().sharpness().updateValue(0);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(15));
        assertThat(mCamera.style(), allOf(
                styleSettingSharpnessIs(0, -4, 4),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(15));
        assertThat(mCamera.style(), allOf(
                styleSettingSharpnessIs(0, -4, 4),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.style().sharpness().setValue(-1);

        assertThat(mComponentChangeCnt, is(16));
        assertThat(mCamera.style(), allOf(
                styleSettingSharpnessIs(-1, -4, 4),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(17));
        assertThat(mCamera.style(), allOf(
                styleSettingSharpnessIs(0, -4, 4),
                settingIsUpToDate()));
    }

    @Test
    public void testAlignment() {
        // we need the camera to be active to have alignment
        mCameraImpl.updateActiveFlag(true).publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.alignment(), is(nullValue()));

        // create alignment
        mCameraImpl.createAlignmentIfNeeded();
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        CameraAlignment.Setting alignment = mCamera.alignment();
        assertThat(alignment, is(notNullValue()));
        CameraAlignmentSettingCore alignmentImpl = mCameraImpl.alignment();
        assertThat(alignmentImpl, is(notNullValue()));

        // test backend change notification
        alignmentImpl.updateSupportedYawRange(DoubleRange.of(-2.0, 2.0)).updateYaw(1);
        alignmentImpl.updateSupportedPitchRange(DoubleRange.of(-4.0, 4.0)).updatePitch(2);
        alignmentImpl.updateSupportedRollRange(DoubleRange.of(-6.0, 6.0)).updateRoll(3);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, 1.0, 2.0),
                alignmentSettingPitchIs(-4.0, 2.0, 4.0),
                alignmentSettingRollIs(-6.0, 3.0, 6.0),
                settingIsUpToDate()));

        // change yaw
        alignment.setYaw(-1.0);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mBackend.mYawAlignment, is(-1.0));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, -1.0, 2.0),
                alignmentSettingPitchIs(-4.0, 2.0, 4.0),
                alignmentSettingRollIs(-6.0, 3.0, 6.0),
                settingIsUpdating()));


        alignmentImpl.updateYaw(-1.0);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(5));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, -1.0, 2.0),
                alignmentSettingPitchIs(-4.0, 2.0, 4.0),
                alignmentSettingRollIs(-6.0, 3.0, 6.0),
                settingIsUpToDate()));

        // change pitch
        alignment.setPitch(3.5);

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mBackend.mPitchAlignment, is(3.5));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, -1.0, 2.0),
                alignmentSettingPitchIs(-4.0, 3.5, 4.0),
                alignmentSettingRollIs(-6.0, 3.0, 6.0),
                settingIsUpdating()));

        alignmentImpl.updatePitch(3.5);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, -1.0, 2.0),
                alignmentSettingPitchIs(-4.0, 3.5, 4.0),
                alignmentSettingRollIs(-6.0, 3.0, 6.0),
                settingIsUpToDate()));

        // change roll
        alignment.setRoll(-2.6);

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mBackend.mRollAlignment, is(-2.6));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, -1.0, 2.0),
                alignmentSettingPitchIs(-4.0, 3.5, 4.0),
                alignmentSettingRollIs(-6.0, -2.6, 6.0),
                settingIsUpdating()));

        alignmentImpl.updateRoll(-2.6);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(9));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, -1.0, 2.0),
                alignmentSettingPitchIs(-4.0, 3.5, 4.0),
                alignmentSettingRollIs(-6.0, -2.6, 6.0),
                settingIsUpToDate()));

        // reset alignment
        alignment.reset();

        assertThat(mComponentChangeCnt, is(9));
        assertThat(mBackend.mAlignmentReset, is(true));

        alignmentImpl.updateYaw(0.0);
        alignmentImpl.updatePitch(0.0);
        alignmentImpl.updateRoll(0.0);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(10));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, 0.0, 2.0),
                alignmentSettingPitchIs(-4.0, 0.0, 4.0),
                alignmentSettingRollIs(-6.0, 0.0, 6.0),
                settingIsUpToDate()));

        // deactivate camera
        mCameraImpl.updateActiveFlag(false).notifyUpdated();

        assertThat(mCamera.alignment(), is(nullValue()));
        assertThat(mComponentChangeCnt, is(11));
    }

    @Test
    public void testCameraZoom() {
        mCameraImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.zoom(), is(nullValue()));

        // create zoom
        mCameraImpl.createZoomIfNeeded();
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.zoom(), is(notNullValue()));
    }

    @Test
    public void testMaxZoomSpeed() {
        mCameraImpl.createZoomIfNeeded();
        mCameraImpl.publish();

        // test initial state
        assertThat(mComponentChangeCnt, is(1));
        CameraZoom zoom = mCamera.zoom();
        assertThat(zoom, is(notNullValue()));
        CameraZoomCore zoomImpl = mCameraImpl.zoom();
        assertThat(zoomImpl, is(notNullValue()));

        // change setting from low-level (backend)
        zoomImpl.maxSpeed().updateBounds(DoubleRange.of(2.0, 10.0)).updateValue(5.0);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(zoom.maxSpeed(), doubleSettingIsUpToDateAt(2.0, 5.0, 10.0));

        // change setting from user side (sdk)
        zoom.maxSpeed().setValue(7.0);
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mMaxZoomSpeed, is(7.0));
        assertThat(zoom.maxSpeed(), doubleSettingIsUpdatingTo(2.0, 7.0, 10.0));

        // mock update from low-level
        zoomImpl.maxSpeed().updateBounds(DoubleRange.of(2.0, 10.0)).updateValue(8.0);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(zoom.maxSpeed(), doubleSettingIsUpToDateAt(2.0, 8.0, 10.0));
    }

    @Test
    public void testQualityDegradationAllowance() {
        mCameraImpl.createZoomIfNeeded();
        mCameraImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        CameraZoom zoom = mCamera.zoom();
        assertThat(zoom, is(notNullValue()));
        CameraZoomCore zoomImpl = mCameraImpl.zoom();
        assertThat(zoomImpl, is(notNullValue()));
        assertThat(zoom.velocityQualityDegradationAllowance(), booleanSettingIsDisabled());

        // change setting from user side (sdk)
        zoom.velocityQualityDegradationAllowance().toggle();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mQualityDegradationAllowance, is(true));
        assertThat(zoom.velocityQualityDegradationAllowance(), booleanSettingIsEnabling());

        // mock update from low-level
        zoomImpl.velocityQualityDegradationAllowance().updateValue(true);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(zoom.velocityQualityDegradationAllowance(), booleanSettingIsEnabled());

        // toggle setting from user side (sdk)
        zoom.velocityQualityDegradationAllowance().toggle();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mBackend.mQualityDegradationAllowance, is(false));
        assertThat(zoom.velocityQualityDegradationAllowance(), booleanSettingIsDisabling());

        // mock update from low-level
        zoomImpl.velocityQualityDegradationAllowance().updateValue(false);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(zoom.velocityQualityDegradationAllowance(), booleanSettingIsDisabled());
    }

    @Test
    public void testCurrentZoomLevel() {
        mCameraImpl.createZoomIfNeeded();
        mCameraImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        CameraZoom zoom = mCamera.zoom();
        assertThat(zoom, is(notNullValue()));
        CameraZoomCore zoomImpl = mCameraImpl.zoom();
        assertThat(zoomImpl, is(notNullValue()));
        assertThat(zoom.getCurrentLevel(), is(1.0));

        // mock update zoom level
        zoomImpl.updateCurrentLevel(5.0);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(zoom.getCurrentLevel(), is(5.0));

        // mock update zoom level with same value
        zoomImpl.updateCurrentLevel(5.0);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(zoom.getCurrentLevel(), is(5.0));
    }

    @Test
    public void testMaxLossyZoomLevel() {
        mCameraImpl.createZoomIfNeeded();
        mCameraImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        CameraZoom zoom = mCamera.zoom();
        assertThat(zoom, is(notNullValue()));
        CameraZoomCore zoomImpl = mCameraImpl.zoom();
        assertThat(zoomImpl, is(notNullValue()));
        assertThat(zoom.getMaxLossyLevel(), is(1.0));

        // mock update max lossy zoom level
        zoomImpl.updateMaxLossyLevel(10.0);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(zoom.getMaxLossyLevel(), is(10.0));

        // mock update max lossy zoom level with same value
        zoomImpl.updateMaxLossyLevel(10.0);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(zoom.getMaxLossyLevel(), is(10.0));
    }

    @Test
    public void testMaxLossLessZoomLevel() {
        mCameraImpl.createZoomIfNeeded();
        mCameraImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        CameraZoom zoom = mCamera.zoom();
        assertThat(zoom, is(notNullValue()));
        CameraZoomCore zoomImpl = mCameraImpl.zoom();
        assertThat(zoomImpl, is(notNullValue()));
        assertThat(zoom.getMaxLossLessLevel(), is(1.0));

        // mock update max loss less zoom level
        zoomImpl.updateMaxLossLessLevel(8.0);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(zoom.getMaxLossLessLevel(), is(8.0));

        // mock update max loss less zoom level with same value
        zoomImpl.updateMaxLossLessLevel(8.0);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(zoom.getMaxLossLessLevel(), is(8.0));
    }

    @Test
    public void testZoomAvailability() {
        mCameraImpl.createZoomIfNeeded();
        mCameraImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        CameraZoom zoom = mCamera.zoom();
        assertThat(zoom, is(notNullValue()));
        CameraZoomCore zoomImpl = mCameraImpl.zoom();
        assertThat(zoomImpl, is(notNullValue()));
        assertThat(zoom.isAvailable(), is(false));

        // mock update availability to true
        zoomImpl.updateAvailability(true);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(zoom.isAvailable(), is(true));

        // mock update availability to false
        zoomImpl.updateAvailability(false);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(zoom.isAvailable(), is(false));
    }

    @Test
    public void testZoomControl() {
        mCameraImpl.createZoomIfNeeded();
        mCameraImpl.publish();

        // test initial state
        assertThat(mComponentChangeCnt, is(1));
        CameraZoom zoom = mCamera.zoom();
        assertThat(zoom, is(notNullValue()));
        CameraZoomCore zoomImpl = mCameraImpl.zoom();
        assertThat(zoomImpl, is(notNullValue()));

        // mock update max zoom level
        zoomImpl.updateMaxLossyLevel(10.0);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(zoom.getMaxLossyLevel(), is(10.0));

        // change zoom level
        zoom.control(CameraZoom.ControlMode.LEVEL, 2.0);
        assertThat(mBackend.mZoomControlMode, is(CameraZoom.ControlMode.LEVEL));
        assertThat(mBackend.mZoomTarget, is(2.0));

        // change zoom level
        zoom.control(CameraZoom.ControlMode.LEVEL, 1.0);
        assertThat(mBackend.mZoomControlMode, is(CameraZoom.ControlMode.LEVEL));
        assertThat(mBackend.mZoomTarget, is(1.0));

        // change upper bound
        zoom.control(CameraZoom.ControlMode.LEVEL, 12.0);
        assertThat(mBackend.mZoomControlMode, is(CameraZoom.ControlMode.LEVEL));
        assertThat(mBackend.mZoomTarget, is(10.0));

        // check lower bound
        zoom.control(CameraZoom.ControlMode.LEVEL, 0.5);
        assertThat(mBackend.mZoomControlMode, is(CameraZoom.ControlMode.LEVEL));
        assertThat(mBackend.mZoomTarget, is(1.0));

        // change zoom velocity
        zoom.control(CameraZoom.ControlMode.VELOCITY, 0.5);
        assertThat(mBackend.mZoomControlMode, is(CameraZoom.ControlMode.VELOCITY));
        assertThat(mBackend.mZoomTarget, is(0.5));

        // change zoom velocity
        zoom.control(CameraZoom.ControlMode.VELOCITY, -1.0);
        assertThat(mBackend.mZoomControlMode, is(CameraZoom.ControlMode.VELOCITY));
        assertThat(mBackend.mZoomTarget, is(-1.0));

        // change upper bound
        zoom.control(CameraZoom.ControlMode.VELOCITY, 15.0);
        assertThat(mBackend.mZoomControlMode, is(CameraZoom.ControlMode.VELOCITY));
        assertThat(mBackend.mZoomTarget, is(1.0));

        // check lower bound
        zoom.control(CameraZoom.ControlMode.VELOCITY, -1.5);
        assertThat(mBackend.mZoomControlMode, is(CameraZoom.ControlMode.VELOCITY));
        assertThat(mBackend.mZoomTarget, is(-1.0));
    }

    @Test
    public void testRecordingSetting() {
        mCameraImpl.recording()
                   .updateCapabilities(Arrays.asList(
                           CameraRecordingSettingCore.Capability.of(
                                   EnumSet.of(CameraRecording.Mode.STANDARD, CameraRecording.Mode.HYPERLAPSE),
                                   EnumSet.of(CameraRecording.Resolution.RES_UHD_4K),
                                   EnumSet.of(CameraRecording.Framerate.FPS_24, CameraRecording.Framerate.FPS_25,
                                           CameraRecording.Framerate.FPS_30),
                                   false),
                           CameraRecordingSettingCore.Capability.of(
                                   EnumSet.of(CameraRecording.Mode.STANDARD, CameraRecording.Mode.HYPERLAPSE),
                                   EnumSet.of(CameraRecording.Resolution.RES_1080P),
                                   EnumSet.of(CameraRecording.Framerate.FPS_24, CameraRecording.Framerate.FPS_25),
                                   true),
                           CameraRecordingSettingCore.Capability.of(
                                   EnumSet.of(CameraRecording.Mode.STANDARD, CameraRecording.Mode.HYPERLAPSE),
                                   EnumSet.of(CameraRecording.Resolution.RES_1080P),
                                   EnumSet.of(CameraRecording.Framerate.FPS_30),
                                   false),
                           CameraRecordingSettingCore.Capability.of(
                                   EnumSet.of(CameraRecording.Mode.HYPERLAPSE),
                                   EnumSet.of(CameraRecording.Resolution.RES_720P),
                                   EnumSet.of(CameraRecording.Framerate.FPS_60),
                                   false),
                           CameraRecordingSettingCore.Capability.of(
                                   EnumSet.of(CameraRecording.Mode.HIGH_FRAMERATE),
                                   EnumSet.of(CameraRecording.Resolution.RES_720P),
                                   EnumSet.of(CameraRecording.Framerate.FPS_120),
                                   false)))
                   .updateSupportedHyperlapseValues(EnumSet.of(
                           CameraRecording.HyperlapseValue.RATIO_15,
                           CameraRecording.HyperlapseValue.RATIO_30,
                           CameraRecording.HyperlapseValue.RATIO_60));

        mCameraImpl.publish();

        // test capabilities values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.recording(), allOf(
                recordingSettingSupportsModes(EnumSet.of(CameraRecording.Mode.STANDARD, CameraRecording.Mode.HYPERLAPSE,
                        CameraRecording.Mode.HIGH_FRAMERATE)),
                recordingSettingSupportsHyperlapseValues(EnumSet.of(CameraRecording.HyperlapseValue.RATIO_15,
                        CameraRecording.HyperlapseValue.RATIO_30, CameraRecording.HyperlapseValue.RATIO_60)),
                recordingSettingSupportsResolutions(CameraRecording.Mode.STANDARD, EnumSet.of(
                        CameraRecording.Resolution.RES_UHD_4K, CameraRecording.Resolution.RES_1080P)),
                recordingSettingSupportsResolutions(CameraRecording.Mode.HYPERLAPSE, EnumSet.of(
                        CameraRecording.Resolution.RES_720P, CameraRecording.Resolution.RES_UHD_4K,
                        CameraRecording.Resolution.RES_1080P)),
                recordingSettingSupportsResolutions(CameraRecording.Mode.HIGH_FRAMERATE, EnumSet.of(
                        CameraRecording.Resolution.RES_720P)),
                recordingSettingSupportsResolutions(CameraRecording.Mode.SLOW_MOTION, EnumSet.noneOf(
                        CameraRecording.Resolution.class)),
                recordingSettingSupportsFramerates(CameraRecording.Mode.STANDARD, CameraRecording.Resolution.RES_UHD_4K,
                        EnumSet.of(CameraRecording.Framerate.FPS_24, CameraRecording.Framerate.FPS_25,
                                CameraRecording.Framerate.FPS_30)),
                recordingSettingSupportsFramerates(CameraRecording.Mode.STANDARD, CameraRecording.Resolution.RES_1080P,
                        EnumSet.of(CameraRecording.Framerate.FPS_24, CameraRecording.Framerate.FPS_25,
                                CameraRecording.Framerate.FPS_30)),
                recordingSettingSupportsFramerates(CameraRecording.Mode.HYPERLAPSE,
                        CameraRecording.Resolution.RES_UHD_4K, EnumSet.of(CameraRecording.Framerate.FPS_24,
                                CameraRecording.Framerate.FPS_25, CameraRecording.Framerate.FPS_30)),
                recordingSettingSupportsFramerates(CameraRecording.Mode.HYPERLAPSE,
                        CameraRecording.Resolution.RES_1080P, EnumSet.of(CameraRecording.Framerate.FPS_24,
                                CameraRecording.Framerate.FPS_25, CameraRecording.Framerate.FPS_30)),
                recordingSettingSupportsFramerates(CameraRecording.Mode.HYPERLAPSE, CameraRecording.Resolution.RES_720P,
                        EnumSet.of(CameraRecording.Framerate.FPS_60)),
                recordingSettingSupportsFramerates(CameraRecording.Mode.HIGH_FRAMERATE,
                        CameraRecording.Resolution.RES_720P, EnumSet.of(CameraRecording.Framerate.FPS_120))));

        assertThat(mCamera.recording(), allOf(
                recordingSettingHdrAvailableIs(CameraRecording.Mode.STANDARD, CameraRecording.Resolution.RES_UHD_4K,
                        CameraRecording.Framerate.FPS_24, false),
                recordingSettingHdrAvailableIs(CameraRecording.Mode.STANDARD, CameraRecording.Resolution.RES_UHD_4K,
                        CameraRecording.Framerate.FPS_25, false),
                recordingSettingHdrAvailableIs(CameraRecording.Mode.STANDARD, CameraRecording.Resolution.RES_UHD_4K,
                        CameraRecording.Framerate.FPS_30, false),
                recordingSettingHdrAvailableIs(CameraRecording.Mode.HYPERLAPSE, CameraRecording.Resolution.RES_UHD_4K,
                        CameraRecording.Framerate.FPS_24, false),
                recordingSettingHdrAvailableIs(CameraRecording.Mode.HYPERLAPSE, CameraRecording.Resolution.RES_UHD_4K,
                        CameraRecording.Framerate.FPS_25, false),
                recordingSettingHdrAvailableIs(CameraRecording.Mode.HYPERLAPSE, CameraRecording.Resolution.RES_UHD_4K,
                        CameraRecording.Framerate.FPS_30, false),
                recordingSettingHdrAvailableIs(CameraRecording.Mode.STANDARD, CameraRecording.Resolution.RES_1080P,
                        CameraRecording.Framerate.FPS_24, true),
                recordingSettingHdrAvailableIs(CameraRecording.Mode.STANDARD, CameraRecording.Resolution.RES_1080P,
                        CameraRecording.Framerate.FPS_25, true),
                recordingSettingHdrAvailableIs(CameraRecording.Mode.STANDARD, CameraRecording.Resolution.RES_1080P,
                        CameraRecording.Framerate.FPS_30, false),
                recordingSettingHdrAvailableIs(CameraRecording.Mode.HYPERLAPSE, CameraRecording.Resolution.RES_1080P,
                        CameraRecording.Framerate.FPS_24, true),
                recordingSettingHdrAvailableIs(CameraRecording.Mode.HYPERLAPSE, CameraRecording.Resolution.RES_1080P,
                        CameraRecording.Framerate.FPS_25, true),
                recordingSettingHdrAvailableIs(CameraRecording.Mode.HYPERLAPSE, CameraRecording.Resolution.RES_1080P,
                        CameraRecording.Framerate.FPS_30, false)));

        // test backend change notification
        mCameraImpl.recording()
                   .updateMode(CameraRecording.Mode.STANDARD)
                   .updateResolution(CameraRecording.Resolution.RES_UHD_4K)
                   .updateFramerate(CameraRecording.Framerate.FPS_25)
                   .updateHyperlapseValue(CameraRecording.HyperlapseValue.RATIO_30);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.STANDARD),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_25),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_30),
                recordingSettingHdrAvailableIs(false),
                settingIsUpToDate()));
        assertThat(mCamera.isHdrAvailable(), is(false));

        // test individual setters
        mCamera.recording().setMode(CameraRecording.Mode.HYPERLAPSE);

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mRecordingMode, is(CameraRecording.Mode.HYPERLAPSE));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                // check that capabilities have changed
                recordingSettingSupportsResolutions(EnumSet.of(CameraRecording.Resolution.RES_UHD_4K,
                        CameraRecording.Resolution.RES_1080P, CameraRecording.Resolution.RES_720P)),
                recordingSettingSupportsFramerates(EnumSet.of(CameraRecording.Framerate.FPS_24,
                        CameraRecording.Framerate.FPS_25, CameraRecording.Framerate.FPS_30)),
                recordingSettingHdrAvailableIs(false),
                settingIsUpdating()));

        mCameraImpl.recording().updateMode(CameraRecording.Mode.HYPERLAPSE);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingHdrAvailableIs(false),
                settingIsUpToDate()));

        // unsupported value
        mCamera.recording().setMode(CameraRecording.Mode.SLOW_MOTION);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mBackend.mRecordingMode, is(CameraRecording.Mode.HYPERLAPSE));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingHdrAvailableIs(false),
                settingIsUpToDate()));

        // resolution
        mCamera.recording().setResolution(CameraRecording.Resolution.RES_1080P);

        assertThat(mComponentChangeCnt, is(5));
        assertThat(mBackend.mResolution, is(CameraRecording.Resolution.RES_1080P));
        assertThat(mCamera.recording(), allOf(
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_1080P),
                recordingSettingHdrAvailableIs(true),
                settingIsUpdating()));

        mCameraImpl.recording().updateResolution(CameraRecording.Resolution.RES_1080P);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.recording(), allOf(
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_1080P),
                recordingSettingHdrAvailableIs(true),
                settingIsUpToDate()));
        assertThat(mCamera.isHdrAvailable(), is(true));

        // unsupported value
        mCamera.recording().setResolution(CameraRecording.Resolution.RES_2_7K);

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mBackend.mResolution, is(CameraRecording.Resolution.RES_1080P));
        assertThat(mCamera.recording(), allOf(
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_1080P),
                recordingSettingHdrAvailableIs(true),
                settingIsUpToDate()));

        // framerate
        mCamera.recording().setFramerate(CameraRecording.Framerate.FPS_30);

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mBackend.mFramerate, is(CameraRecording.Framerate.FPS_30));
        assertThat(mCamera.recording(), allOf(
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_30),
                recordingSettingHdrAvailableIs(false),
                settingIsUpdating()));

        mCameraImpl.recording().updateFramerate(CameraRecording.Framerate.FPS_30);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mCamera.recording(), allOf(
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_30),
                recordingSettingHdrAvailableIs(false),
                settingIsUpToDate()));
        assertThat(mCamera.isHdrAvailable(), is(false));

        // unsupported value
        mCamera.recording().setFramerate(CameraRecording.Framerate.FPS_120);

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mBackend.mFramerate, is(CameraRecording.Framerate.FPS_30));
        assertThat(mCamera.recording(), allOf(
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_30),
                recordingSettingHdrAvailableIs(false),
                settingIsUpToDate()));

        // hyperlapse
        mCamera.recording().setHyperlapseValue(CameraRecording.HyperlapseValue.RATIO_15);

        assertThat(mComponentChangeCnt, is(9));
        assertThat(mBackend.mHyperlapse, is(CameraRecording.HyperlapseValue.RATIO_15));
        assertThat(mCamera.recording(), allOf(
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_15),
                recordingSettingHdrAvailableIs(false),
                settingIsUpdating()));

        mCameraImpl.recording().updateHyperlapseValue(CameraRecording.HyperlapseValue.RATIO_15);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mCamera.recording(), allOf(
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_15),
                recordingSettingHdrAvailableIs(false),
                settingIsUpToDate()));

        // unsupported value
        mCamera.recording().setHyperlapseValue(CameraRecording.HyperlapseValue.RATIO_240);

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mBackend.mHyperlapse, is(CameraRecording.HyperlapseValue.RATIO_15));
        assertThat(mCamera.recording(), allOf(
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_15),
                recordingSettingHdrAvailableIs(false),
                settingIsUpToDate()));

        // test global setters

        // standard mode
        mBackend.mRecordingMode = null;
        mBackend.mResolution = null;
        mBackend.mFramerate = null;
        mCamera.recording().setStandardMode(CameraRecording.Resolution.RES_UHD_4K, CameraRecording.Framerate.FPS_24);

        assertThat(mComponentChangeCnt, is(11));
        assertThat(mBackend.mRecordingMode, is(CameraRecording.Mode.STANDARD));
        assertThat(mBackend.mResolution, is(CameraRecording.Resolution.RES_UHD_4K));
        assertThat(mBackend.mFramerate, is(CameraRecording.Framerate.FPS_24));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.STANDARD),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHdrAvailableIs(false),
                settingIsUpdating()));
        assertThat(mCamera.isHdrAvailable(), is(false));

        mCameraImpl.recording()
                   .updateMode(CameraRecording.Mode.STANDARD)
                   .updateResolution(CameraRecording.Resolution.RES_UHD_4K)
                   .updateFramerate(CameraRecording.Framerate.FPS_24);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(12));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.STANDARD),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHdrAvailableIs(false),
                settingIsUpToDate()));

        // high framerate mode
        mBackend.mRecordingMode = null;
        mBackend.mResolution = null;
        mBackend.mFramerate = null;
        mCamera.recording().setHighFramerateMode(CameraRecording.Resolution.RES_720P,
                CameraRecording.Framerate.FPS_120);

        assertThat(mComponentChangeCnt, is(13));
        assertThat(mBackend.mRecordingMode, is(CameraRecording.Mode.HIGH_FRAMERATE));
        assertThat(mBackend.mResolution, is(CameraRecording.Resolution.RES_720P));
        assertThat(mBackend.mFramerate, is(CameraRecording.Framerate.FPS_120));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_720P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_120),
                recordingSettingHdrAvailableIs(false),
                settingIsUpdating()));

        mCameraImpl.recording()
                   .updateMode(CameraRecording.Mode.HIGH_FRAMERATE)
                   .updateResolution(CameraRecording.Resolution.RES_720P)
                   .updateFramerate(CameraRecording.Framerate.FPS_120);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(14));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_720P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_120),
                recordingSettingHdrAvailableIs(false),
                settingIsUpToDate()));

        // add support of slow motion mode
        mCameraImpl.recording().updateCapabilities(Collections.singleton(CameraRecordingSettingCore.Capability.of(
                EnumSet.of(CameraRecording.Mode.SLOW_MOTION, CameraRecording.Mode.HYPERLAPSE),
                EnumSet.of(CameraRecording.Resolution.RES_UHD_4K, CameraRecording.Resolution.RES_1080P),
                EnumSet.of(CameraRecording.Framerate.FPS_24, CameraRecording.Framerate.FPS_25),
                false)));

        // slow motion mode
        mBackend.mRecordingMode = null;
        mBackend.mResolution = null;
        mBackend.mFramerate = null;
        mCamera.recording().setSlowMotionMode(CameraRecording.Resolution.RES_1080P,
                CameraRecording.Framerate.FPS_25);

        assertThat(mComponentChangeCnt, is(15));
        assertThat(mBackend.mRecordingMode, is(CameraRecording.Mode.SLOW_MOTION));
        assertThat(mBackend.mResolution, is(CameraRecording.Resolution.RES_1080P));
        assertThat(mBackend.mFramerate, is(CameraRecording.Framerate.FPS_25));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.SLOW_MOTION),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_1080P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_25),
                recordingSettingHdrAvailableIs(false),
                settingIsUpdating()));

        mCameraImpl.recording()
                   .updateMode(CameraRecording.Mode.SLOW_MOTION)
                   .updateResolution(CameraRecording.Resolution.RES_1080P)
                   .updateFramerate(CameraRecording.Framerate.FPS_25);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(16));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.SLOW_MOTION),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_1080P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_25),
                recordingSettingHdrAvailableIs(false),
                settingIsUpToDate()));

        // hyperlapse mode
        mBackend.mRecordingMode = null;
        mBackend.mResolution = null;
        mBackend.mFramerate = null;
        mBackend.mHyperlapse = null;
        mCamera.recording().setHyperlapseMode(CameraRecording.Resolution.RES_UHD_4K, CameraRecording.Framerate.FPS_24,
                CameraRecording.HyperlapseValue.RATIO_60);

        assertThat(mComponentChangeCnt, is(17));
        assertThat(mBackend.mRecordingMode, is(CameraRecording.Mode.HYPERLAPSE));
        assertThat(mBackend.mResolution, is(CameraRecording.Resolution.RES_UHD_4K));
        assertThat(mBackend.mFramerate, is(CameraRecording.Framerate.FPS_24));
        assertThat(mBackend.mHyperlapse, is(CameraRecording.HyperlapseValue.RATIO_60));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                recordingSettingHdrAvailableIs(false),
                settingIsUpdating()));

        mCameraImpl.recording()
                   .updateMode(CameraRecording.Mode.HYPERLAPSE)
                   .updateResolution(CameraRecording.Resolution.RES_UHD_4K)
                   .updateFramerate(CameraRecording.Framerate.FPS_24)
                   .updateHyperlapseValue(CameraRecording.HyperlapseValue.RATIO_60);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(18));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                recordingSettingHdrAvailableIs(false),
                settingIsUpToDate()));
    }

    @Test
    public void testRecordingSettingTimeouts() {
        mCameraImpl.recording()
                   .updateCapabilities(Collections.singletonList(
                           CameraRecordingSettingCore.Capability.of(
                                   EnumSet.allOf(CameraRecording.Mode.class),
                                   EnumSet.allOf(CameraRecording.Resolution.class),
                                   EnumSet.allOf(CameraRecording.Framerate.class),
                                   true)))
                   .updateSupportedHyperlapseValues(EnumSet.allOf(CameraRecording.HyperlapseValue.class));

        mCameraImpl.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.STANDARD),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_DCI_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_30),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_15),
                settingIsUpToDate()));

        // --- mode ---

        // mock user sets value
        mCamera.recording().setMode(CameraRecording.Mode.HIGH_FRAMERATE);

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.recording().updateMode(CameraRecording.Mode.HIGH_FRAMERATE);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.recording().setMode(CameraRecording.Mode.HYPERLAPSE);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                settingIsUpToDate()));

        // --- resolution ---

        // mock user sets value
        mCamera.recording().setResolution(CameraRecording.Resolution.RES_2_7K);

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.recording(), allOf(
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_2_7K),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.recording().updateResolution(CameraRecording.Resolution.RES_2_7K);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mCamera.recording(), allOf(
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_2_7K),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(7));
        assertThat(mCamera.recording(), allOf(
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_2_7K),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.recording().setResolution(CameraRecording.Resolution.RES_480P);

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mCamera.recording(), allOf(
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_480P),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(9));
        assertThat(mCamera.recording(), allOf(
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_2_7K),
                settingIsUpToDate()));

        // --- framerate ---

        // mock user sets value
        mCamera.recording().setFramerate(CameraRecording.Framerate.FPS_24);

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mCamera.recording(), allOf(
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.recording().updateFramerate(CameraRecording.Framerate.FPS_24);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(11));
        assertThat(mCamera.recording(), allOf(
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(11));
        assertThat(mCamera.recording(), allOf(
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.recording().setFramerate(CameraRecording.Framerate.FPS_25);

        assertThat(mComponentChangeCnt, is(12));
        assertThat(mCamera.recording(), allOf(
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_25),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(13));
        assertThat(mCamera.recording(), allOf(
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                settingIsUpToDate()));

        // --- hyperlapse ---

        // mock user sets value
        mCamera.recording().setHyperlapseValue(CameraRecording.HyperlapseValue.RATIO_30);

        assertThat(mComponentChangeCnt, is(14));
        assertThat(mCamera.recording(), allOf(
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_30),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.recording().updateHyperlapseValue(CameraRecording.HyperlapseValue.RATIO_30);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(15));
        assertThat(mCamera.recording(), allOf(
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_30),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(15));
        assertThat(mCamera.recording(), allOf(
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_30),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.recording().setHyperlapseValue(CameraRecording.HyperlapseValue.RATIO_60);

        assertThat(mComponentChangeCnt, is(16));
        assertThat(mCamera.recording(), allOf(
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(17));
        assertThat(mCamera.recording(), allOf(
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_30),
                settingIsUpToDate()));

        // --- set standard mode ---

        // mock user sets value
        mCamera.recording().setStandardMode(CameraRecording.Resolution.RES_720P, CameraRecording.Framerate.FPS_120);

        assertThat(mComponentChangeCnt, is(18));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.STANDARD),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_720P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_120),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.recording()
                   .updateMode(CameraRecording.Mode.STANDARD)
                   .updateResolution(CameraRecording.Resolution.RES_720P)
                   .updateFramerate(CameraRecording.Framerate.FPS_120);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(19));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.STANDARD),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_720P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_120),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(19));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.STANDARD),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_720P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_120),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.recording().setStandardMode(CameraRecording.Resolution.RES_1080P, CameraRecording.Framerate.FPS_96);

        assertThat(mComponentChangeCnt, is(20));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.STANDARD),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_1080P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_96),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(21));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.STANDARD),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_720P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_120),
                settingIsUpToDate()));

        // --- set slow motion mode ---

        // mock user sets value
        mCamera.recording().setSlowMotionMode(CameraRecording.Resolution.RES_UHD_4K, CameraRecording.Framerate.FPS_100);

        assertThat(mComponentChangeCnt, is(22));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.SLOW_MOTION),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_100),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.recording()
                   .updateMode(CameraRecording.Mode.SLOW_MOTION)
                   .updateResolution(CameraRecording.Resolution.RES_UHD_4K)
                   .updateFramerate(CameraRecording.Framerate.FPS_100);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(23));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.SLOW_MOTION),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_100),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(23));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.SLOW_MOTION),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_100),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.recording().setSlowMotionMode(CameraRecording.Resolution.RES_480P, CameraRecording.Framerate.FPS_48);

        assertThat(mComponentChangeCnt, is(24));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.SLOW_MOTION),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_480P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_48),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(25));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.SLOW_MOTION),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_100),
                settingIsUpToDate()));

        // --- set high framerate mode ---

        // mock user sets value
        mCamera.recording().setHighFramerateMode(CameraRecording.Resolution.RES_2_7K, CameraRecording.Framerate.FPS_96);

        assertThat(mComponentChangeCnt, is(26));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_2_7K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_96),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.recording()
                   .updateMode(CameraRecording.Mode.HIGH_FRAMERATE)
                   .updateResolution(CameraRecording.Resolution.RES_2_7K)
                   .updateFramerate(CameraRecording.Framerate.FPS_96);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(27));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_2_7K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_96),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(27));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_2_7K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_96),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.recording().setHighFramerateMode(CameraRecording.Resolution.RES_720P, CameraRecording.Framerate.FPS_24);

        assertThat(mComponentChangeCnt, is(28));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_720P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(29));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_2_7K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_96),
                settingIsUpToDate()));

        // --- set hyperlapse mode ---

        // mock user sets value
        mCamera.recording().setHyperlapseMode(CameraRecording.Resolution.RES_DCI_4K, CameraRecording.Framerate.FPS_25,
                CameraRecording.HyperlapseValue.RATIO_240);

        assertThat(mComponentChangeCnt, is(30));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_DCI_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_25),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_240),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.recording()
                   .updateMode(CameraRecording.Mode.HYPERLAPSE)
                   .updateResolution(CameraRecording.Resolution.RES_DCI_4K)
                   .updateFramerate(CameraRecording.Framerate.FPS_25)
                   .updateHyperlapseValue(CameraRecording.HyperlapseValue.RATIO_240);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(31));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_DCI_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_25),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_240),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(31));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_DCI_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_25),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_240),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.recording().setHyperlapseMode(CameraRecording.Resolution.RES_1080P, CameraRecording.Framerate.FPS_30,
                CameraRecording.HyperlapseValue.RATIO_120);

        assertThat(mComponentChangeCnt, is(32));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_1080P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_30),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_120),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(33));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_DCI_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_25),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_240),
                settingIsUpToDate()));
    }

    @Test
    public void testPhotoSetting() {
        mCameraImpl.mode().updateValue(Camera.Mode.PHOTO);
        mCameraImpl.photo()
                   .updateCapabilities(Arrays.asList(
                           CameraPhotoSettingCore.Capability.of(
                                   EnumSet.of(CameraPhoto.Mode.SINGLE, CameraPhoto.Mode.BURST),
                                   EnumSet.of(CameraPhoto.Format.RECTILINEAR),
                                   EnumSet.of(CameraPhoto.FileFormat.JPEG, CameraPhoto.FileFormat.DNG_AND_JPEG),
                                   true),
                           CameraPhotoSettingCore.Capability.of(
                                   EnumSet.of(CameraPhoto.Mode.BURST),
                                   EnumSet.of(CameraPhoto.Format.RECTILINEAR, CameraPhoto.Format.LARGE),
                                   EnumSet.of(CameraPhoto.FileFormat.JPEG),
                                   true),
                           CameraPhotoSettingCore.Capability.of(
                                   EnumSet.of(CameraPhoto.Mode.SINGLE),
                                   EnumSet.of(CameraPhoto.Format.FULL_FRAME),
                                   EnumSet.of(CameraPhoto.FileFormat.JPEG, CameraPhoto.FileFormat.DNG_AND_JPEG,
                                           CameraPhoto.FileFormat.DNG),
                                   true)))
                   .updateSupportedBurstValues(EnumSet.of(CameraPhoto.BurstValue.BURST_10_OVER_2S,
                           CameraPhoto.BurstValue.BURST_14_OVER_1S, CameraPhoto.BurstValue.BURST_4_OVER_1S))
                   .updateSupportedBracketingValues(EnumSet.of(CameraPhoto.BracketingValue.EV_1,
                           CameraPhoto.BracketingValue.EV_1_2, CameraPhoto.BracketingValue.EV_1_2_3))
                   .updateTimelapseIntervalRange(DoubleRange.of(2, Double.MAX_VALUE))
                   .updateGpslapseIntervalRange(DoubleRange.of(1, Double.MAX_VALUE));

        mCameraImpl.publish();

        // test capabilities values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.photo(), allOf(
                photoSettingSupportsModes(EnumSet.of(CameraPhoto.Mode.SINGLE, CameraPhoto.Mode.BURST)),
                photoSettingSupportsBurstValues(EnumSet.of(CameraPhoto.BurstValue.BURST_10_OVER_2S,
                        CameraPhoto.BurstValue.BURST_14_OVER_1S, CameraPhoto.BurstValue.BURST_4_OVER_1S)),
                photoSettingSupportsBracketingValues(EnumSet.of(CameraPhoto.BracketingValue.EV_1,
                        CameraPhoto.BracketingValue.EV_1_2, CameraPhoto.BracketingValue.EV_1_2_3)),
                photoSettingTimelapseIntervalRangeIs(DoubleRange.of(2, Double.MAX_VALUE)),
                photoSettingGpslapseIntervalRangeIs(DoubleRange.of(1, Double.MAX_VALUE)),
                photoSettingSupportsFormats(CameraPhoto.Mode.SINGLE, EnumSet.of(CameraPhoto.Format.FULL_FRAME,
                        CameraPhoto.Format.RECTILINEAR)),
                photoSettingSupportsFormats(CameraPhoto.Mode.BURST, EnumSet.of(CameraPhoto.Format.RECTILINEAR,
                        CameraPhoto.Format.LARGE)),
                photoSettingSupportsFormats(CameraPhoto.Mode.BRACKETING, EnumSet.noneOf(CameraPhoto.Format.class)),
                photoSettingSupportsFileFormats(CameraPhoto.Mode.SINGLE, CameraPhoto.Format.FULL_FRAME, EnumSet.of(
                        CameraPhoto.FileFormat.JPEG, CameraPhoto.FileFormat.DNG_AND_JPEG, CameraPhoto.FileFormat.DNG)),
                photoSettingSupportsFileFormats(CameraPhoto.Mode.SINGLE, CameraPhoto.Format.RECTILINEAR, EnumSet.of(
                        CameraPhoto.FileFormat.JPEG, CameraPhoto.FileFormat.DNG_AND_JPEG)),
                photoSettingSupportsFileFormats(CameraPhoto.Mode.BURST, CameraPhoto.Format.RECTILINEAR, EnumSet.of(
                        CameraPhoto.FileFormat.JPEG, CameraPhoto.FileFormat.DNG_AND_JPEG))));

        assertThat(mCamera.photo(), allOf(
                photoSettingHdrAvailableIs(CameraPhoto.Mode.SINGLE, CameraPhoto.Format.RECTILINEAR,
                        CameraPhoto.FileFormat.JPEG, true),
                photoSettingHdrAvailableIs(CameraPhoto.Mode.SINGLE, CameraPhoto.Format.FULL_FRAME,
                        CameraPhoto.FileFormat.DNG_AND_JPEG, true),
                photoSettingHdrAvailableIs(CameraPhoto.Mode.SINGLE, CameraPhoto.Format.FULL_FRAME,
                        CameraPhoto.FileFormat.JPEG, true),
                photoSettingHdrAvailableIs(CameraPhoto.Mode.BURST, CameraPhoto.Format.RECTILINEAR,
                        CameraPhoto.FileFormat.JPEG, true),
                photoSettingHdrAvailableIs(CameraPhoto.Mode.BURST, CameraPhoto.Format.LARGE,
                        CameraPhoto.FileFormat.JPEG, true)));

        // test backend change notification
        mCameraImpl.photo()
                   .updateMode(CameraPhoto.Mode.SINGLE)
                   .updateFormat(CameraPhoto.Format.FULL_FRAME)
                   .updateFileFormat(CameraPhoto.FileFormat.DNG)
                   .updateBurstValue(CameraPhoto.BurstValue.BURST_10_OVER_2S)
                   .updateBracketingValue(CameraPhoto.BracketingValue.EV_1)
                   .updateTimelapseInterval(3.5)
                   .updateGpslapseInterval(10);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1),
                photoSettingTimelapseIntervalIs(3.5),
                photoSettingGpslapseIntervalIs(10),
                settingIsUpToDate()));

        // test individual setters

        // mode
        mCamera.photo().setMode(CameraPhoto.Mode.BURST);

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mPhotoMode, is(CameraPhoto.Mode.BURST));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                // check supported format
                photoSettingSupportsFormats(EnumSet.of(CameraPhoto.Format.RECTILINEAR, CameraPhoto.Format.LARGE)),
                photoSettingHdrAvailableIs(false),
                settingIsUpdating()));
        assertThat(mCamera.isHdrAvailable(), is(false));

        mCameraImpl.photo().updateMode(CameraPhoto.Mode.BURST);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingHdrAvailableIs(false),
                settingIsUpToDate()));

        // unsupported value
        mCamera.photo().setMode(CameraPhoto.Mode.BRACKETING);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mBackend.mPhotoMode, is(CameraPhoto.Mode.BURST));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                settingIsUpToDate()));

        // format
        mCamera.photo().setFormat(CameraPhoto.Format.RECTILINEAR);

        assertThat(mComponentChangeCnt, is(5));
        assertThat(mBackend.mFormat, is(CameraPhoto.Format.RECTILINEAR));
        assertThat(mCamera.photo(), allOf(
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                // check supported file format
                photoSettingSupportsFileFormats(EnumSet.of(CameraPhoto.FileFormat.JPEG,
                        CameraPhoto.FileFormat.DNG_AND_JPEG)),
                photoSettingHdrAvailableIs(false),
                settingIsUpdating()));

        mCameraImpl.photo().updateFormat(CameraPhoto.Format.RECTILINEAR);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.photo(), allOf(
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingHdrAvailableIs(false),
                settingIsUpToDate()));

        // unsupported value
        mCamera.photo().setFormat(CameraPhoto.Format.FULL_FRAME);

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mBackend.mFormat, is(CameraPhoto.Format.RECTILINEAR));
        assertThat(mCamera.photo(), allOf(
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                settingIsUpToDate()));

        // file format
        mCamera.photo().setFileFormat(CameraPhoto.FileFormat.JPEG);

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mBackend.mFileFormat, is(CameraPhoto.FileFormat.JPEG));
        assertThat(mCamera.photo(), allOf(
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingHdrAvailableIs(true),
                settingIsUpdating()));

        mCameraImpl.photo().updateFileFormat(CameraPhoto.FileFormat.JPEG);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mCamera.photo(), allOf(
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingHdrAvailableIs(true),
                settingIsUpToDate()));
        assertThat(mCamera.isHdrAvailable(), is(true));

        // unsupported value
        mCamera.photo().setFileFormat(CameraPhoto.FileFormat.DNG);

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mBackend.mFileFormat, is(CameraPhoto.FileFormat.JPEG));
        assertThat(mCamera.photo(), allOf(
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                settingIsUpToDate()));

        // burst
        mCamera.photo().setBurstValue(CameraPhoto.BurstValue.BURST_14_OVER_1S);

        assertThat(mComponentChangeCnt, is(9));
        assertThat(mBackend.mBurst, is(CameraPhoto.BurstValue.BURST_14_OVER_1S));
        assertThat(mCamera.photo(), allOf(
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_1S),
                settingIsUpdating()));

        mCameraImpl.photo().updateBurstValue(CameraPhoto.BurstValue.BURST_14_OVER_1S);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mCamera.photo(), allOf(
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_1S),
                settingIsUpToDate()));

        // unsupported value
        mCamera.photo().setBurstValue(CameraPhoto.BurstValue.BURST_14_OVER_4S);

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mBackend.mBurst, is(CameraPhoto.BurstValue.BURST_14_OVER_1S));
        assertThat(mCamera.photo(), allOf(
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_1S),
                settingIsUpToDate()));
        // bracketing
        mCamera.photo().setBracketingValue(CameraPhoto.BracketingValue.EV_1_2);

        assertThat(mComponentChangeCnt, is(11));
        assertThat(mBackend.mBracketing, is(CameraPhoto.BracketingValue.EV_1_2));
        assertThat(mCamera.photo(), allOf(
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                settingIsUpdating()));

        mCameraImpl.photo().updateBracketingValue(CameraPhoto.BracketingValue.EV_1_2);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(12));
        assertThat(mCamera.photo(), allOf(
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                settingIsUpToDate()));

        // unsupported value
        mCamera.photo().setBracketingValue(CameraPhoto.BracketingValue.EV_2);

        assertThat(mComponentChangeCnt, is(12));
        assertThat(mBackend.mBracketing, is(CameraPhoto.BracketingValue.EV_1_2));
        assertThat(mCamera.photo(), allOf(
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                settingIsUpToDate()));

        // time-lapse
        mCamera.photo().setTimelapseInterval(5.5);

        assertThat(mComponentChangeCnt, is(13));
        assertThat(mBackend.mTimelapseInterval, is(5.5));
        assertThat(mCamera.photo(), allOf(
                photoSettingTimelapseIntervalIs(5.5),
                settingIsUpdating()));

        mCameraImpl.photo().updateTimelapseInterval(5.5);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(14));
        assertThat(mCamera.photo(), allOf(
                photoSettingTimelapseIntervalIs(5.5),
                settingIsUpToDate()));

        // value below minimum
        mCamera.photo().setTimelapseInterval(1.2);

        assertThat(mComponentChangeCnt, is(15));
        assertThat(mBackend.mTimelapseInterval, is(2.0));
        assertThat(mCamera.photo(), allOf(
                photoSettingTimelapseIntervalIs(2),
                settingIsUpdating()));

        mCameraImpl.photo().updateTimelapseInterval(2);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(16));
        assertThat(mCamera.photo(), allOf(
                photoSettingTimelapseIntervalIs(2),
                settingIsUpToDate()));

        // update range
        mCameraImpl.photo().updateTimelapseIntervalRange(DoubleRange.of(4, Double.MAX_VALUE));
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(17));
        assertThat(mCamera.photo(), allOf(
                photoSettingTimelapseIntervalRangeIs(DoubleRange.of(4, Double.MAX_VALUE)),
                photoSettingTimelapseIntervalIs(4),
                settingIsUpToDate()));

        // update value from backend
        mCameraImpl.photo().updateTimelapseInterval(7);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(18));
        assertThat(mCamera.photo(), allOf(
                photoSettingTimelapseIntervalIs(7),
                settingIsUpToDate()));

        // GPS-lapse
        mCamera.photo().setGpslapseInterval(5.5);

        assertThat(mComponentChangeCnt, is(19));
        assertThat(mBackend.mGpslapseInterval, is(5.5));
        assertThat(mCamera.photo(), allOf(
                photoSettingGpslapseIntervalIs(5.5),
                settingIsUpdating()));

        mCameraImpl.photo().updateGpslapseInterval(5.5);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(20));
        assertThat(mCamera.photo(), allOf(
                photoSettingGpslapseIntervalIs(5.5),
                settingIsUpToDate()));

        // value below minimum
        mCamera.photo().setGpslapseInterval(0.5);

        assertThat(mComponentChangeCnt, is(21));
        assertThat(mBackend.mGpslapseInterval, is(1.0));
        assertThat(mCamera.photo(), allOf(
                photoSettingGpslapseIntervalIs(1),
                settingIsUpdating()));

        mCameraImpl.photo().updateGpslapseInterval(1);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(22));
        assertThat(mCamera.photo(), allOf(
                photoSettingGpslapseIntervalIs(1),
                settingIsUpToDate()));

        // update range
        mCameraImpl.photo().updateGpslapseIntervalRange(DoubleRange.of(5, Double.MAX_VALUE));
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(23));
        assertThat(mCamera.photo(), allOf(
                photoSettingGpslapseIntervalRangeIs(DoubleRange.of(5, Double.MAX_VALUE)),
                photoSettingGpslapseIntervalIs(5),
                settingIsUpToDate()));

        // update value from backend
        mCameraImpl.photo().updateGpslapseInterval(10);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(24));
        assertThat(mCamera.photo(), allOf(
                photoSettingGpslapseIntervalIs(10),
                settingIsUpToDate()));

        // test global setters

        // single mode
        mBackend.mPhotoMode = null;
        mBackend.mFormat = null;
        mBackend.mFileFormat = null;
        mCamera.photo().setSingleMode(CameraPhoto.Format.FULL_FRAME, CameraPhoto.FileFormat.DNG);

        assertThat(mComponentChangeCnt, is(25));
        assertThat(mBackend.mPhotoMode, is(CameraPhoto.Mode.SINGLE));
        assertThat(mBackend.mFormat, is(CameraPhoto.Format.FULL_FRAME));
        assertThat(mBackend.mFileFormat, is(CameraPhoto.FileFormat.DNG));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                settingIsUpdating()));

        mCameraImpl.photo()
                   .updateMode(CameraPhoto.Mode.SINGLE)
                   .updateFormat(CameraPhoto.Format.FULL_FRAME)
                   .updateFileFormat(CameraPhoto.FileFormat.DNG);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(26));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                settingIsUpToDate()));

        // burst mode
        mBackend.mPhotoMode = null;
        mBackend.mFormat = null;
        mBackend.mFileFormat = null;
        mBackend.mBurst = null;
        mCamera.photo().setBurstMode(CameraPhoto.Format.RECTILINEAR, CameraPhoto.FileFormat.DNG_AND_JPEG,
                CameraPhoto.BurstValue.BURST_10_OVER_2S);

        assertThat(mComponentChangeCnt, is(27));
        assertThat(mBackend.mPhotoMode, is(CameraPhoto.Mode.BURST));
        assertThat(mBackend.mFormat, is(CameraPhoto.Format.RECTILINEAR));
        assertThat(mBackend.mFileFormat, is(CameraPhoto.FileFormat.DNG_AND_JPEG));
        assertThat(mBackend.mBurst, is(CameraPhoto.BurstValue.BURST_10_OVER_2S));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                settingIsUpdating()));

        mCameraImpl.photo()
                   .updateMode(CameraPhoto.Mode.BURST)
                   .updateFormat(CameraPhoto.Format.RECTILINEAR)
                   .updateFileFormat(CameraPhoto.FileFormat.DNG_AND_JPEG)
                   .updateBurstValue(CameraPhoto.BurstValue.BURST_10_OVER_2S);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(28));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                settingIsUpToDate()));

        // add support of bracketing mode
        mCameraImpl.photo().updateCapabilities(Collections.singleton(CameraPhotoSettingCore.Capability.of(
                EnumSet.of(CameraPhoto.Mode.BRACKETING), EnumSet.of(CameraPhoto.Format.LARGE),
                EnumSet.of(CameraPhoto.FileFormat.JPEG),
                false)));

        // bracketing mode
        mBackend.mPhotoMode = null;
        mBackend.mFormat = null;
        mBackend.mFileFormat = null;
        mBackend.mBracketing = null;
        mCamera.photo().setBracketingMode(CameraPhoto.Format.LARGE, CameraPhoto.FileFormat.JPEG,
                CameraPhoto.BracketingValue.EV_1_2_3);

        assertThat(mComponentChangeCnt, is(29));
        assertThat(mBackend.mPhotoMode, is(CameraPhoto.Mode.BRACKETING));
        assertThat(mBackend.mFormat, is(CameraPhoto.Format.LARGE));
        assertThat(mBackend.mFileFormat, is(CameraPhoto.FileFormat.JPEG));
        assertThat(mBackend.mBracketing, is(CameraPhoto.BracketingValue.EV_1_2_3));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2_3),
                settingIsUpdating()));

        mCameraImpl.photo()
                   .updateMode(CameraPhoto.Mode.BRACKETING)
                   .updateFormat(CameraPhoto.Format.LARGE)
                   .updateFileFormat(CameraPhoto.FileFormat.JPEG)
                   .updateBracketingValue(CameraPhoto.BracketingValue.EV_1_2_3);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(30));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2_3),
                settingIsUpToDate()));

        // add support of time-lapse mode
        mCameraImpl.photo().updateCapabilities(Collections.singleton(CameraPhotoSettingCore.Capability.of(
                EnumSet.of(CameraPhoto.Mode.TIME_LAPSE), EnumSet.of(CameraPhoto.Format.FULL_FRAME),
                EnumSet.of(CameraPhoto.FileFormat.DNG_AND_JPEG),
                false)));

        // time-lapse mode
        mBackend.mPhotoMode = null;
        mBackend.mFormat = null;
        mBackend.mFileFormat = null;
        mBackend.mTimelapseInterval = null;
        mCamera.photo().setTimelapseMode(CameraPhoto.Format.FULL_FRAME, CameraPhoto.FileFormat.DNG_AND_JPEG, 5);

        assertThat(mComponentChangeCnt, is(31));
        assertThat(mBackend.mPhotoMode, is(CameraPhoto.Mode.TIME_LAPSE));
        assertThat(mBackend.mFormat, is(CameraPhoto.Format.FULL_FRAME));
        assertThat(mBackend.mFileFormat, is(CameraPhoto.FileFormat.DNG_AND_JPEG));
        assertThat(mBackend.mTimelapseInterval, is(5.0));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingTimelapseIntervalIs(5.0),
                settingIsUpdating()));

        mCameraImpl.photo()
                   .updateMode(CameraPhoto.Mode.TIME_LAPSE)
                   .updateFormat(CameraPhoto.Format.FULL_FRAME)
                   .updateFileFormat(CameraPhoto.FileFormat.DNG_AND_JPEG)
                   .updateTimelapseInterval(5);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(32));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingTimelapseIntervalIs(5.0),
                settingIsUpToDate()));

        // add support of GPS-lapse mode
        mCameraImpl.photo().updateCapabilities(Collections.singleton(CameraPhotoSettingCore.Capability.of(
                EnumSet.of(CameraPhoto.Mode.GPS_LAPSE), EnumSet.of(CameraPhoto.Format.RECTILINEAR),
                EnumSet.of(CameraPhoto.FileFormat.JPEG),
                false)));

        // GPS-lapse mode
        mBackend.mPhotoMode = null;
        mBackend.mFormat = null;
        mBackend.mFileFormat = null;
        mBackend.mGpslapseInterval = null;
        mCamera.photo().setGpslapseMode(CameraPhoto.Format.RECTILINEAR, CameraPhoto.FileFormat.JPEG, 20);

        assertThat(mComponentChangeCnt, is(33));
        assertThat(mBackend.mPhotoMode, is(CameraPhoto.Mode.GPS_LAPSE));
        assertThat(mBackend.mFormat, is(CameraPhoto.Format.RECTILINEAR));
        assertThat(mBackend.mFileFormat, is(CameraPhoto.FileFormat.JPEG));
        assertThat(mBackend.mGpslapseInterval, is(20.0));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.GPS_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingGpslapseIntervalIs(20.0),
                settingIsUpdating()));

        mCameraImpl.photo()
                   .updateMode(CameraPhoto.Mode.GPS_LAPSE)
                   .updateFormat(CameraPhoto.Format.RECTILINEAR)
                   .updateFileFormat(CameraPhoto.FileFormat.JPEG)
                   .updateGpslapseInterval(20);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(34));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.GPS_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingGpslapseIntervalIs(20.0),
                settingIsUpToDate()));
    }

    @Test
    public void testPhotoSettingTimeouts() {
        mCameraImpl.photo()
                   .updateCapabilities(Collections.singletonList(
                           CameraPhotoSettingCore.Capability.of(
                                   EnumSet.allOf(CameraPhoto.Mode.class),
                                   EnumSet.allOf(CameraPhoto.Format.class),
                                   EnumSet.allOf(CameraPhoto.FileFormat.class),
                                   true)))
                   .updateSupportedBurstValues(EnumSet.allOf(CameraPhoto.BurstValue.class))
                   .updateSupportedBracketingValues(EnumSet.allOf(CameraPhoto.BracketingValue.class))
                   .updateTimelapseIntervalRange(DoubleRange.of(2, 60))
                   .updateGpslapseIntervalRange(DoubleRange.of(1, 100));

        mCameraImpl.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_4S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1),
                photoSettingTimelapseIntervalIs(2),
                photoSettingGpslapseIntervalIs(1),
                settingIsUpToDate()));

        // --- mode ---

        // mock user sets value
        mCamera.photo().setMode(CameraPhoto.Mode.BURST);

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.photo().updateMode(CameraPhoto.Mode.BURST);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.photo().setMode(CameraPhoto.Mode.BRACKETING);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                settingIsUpToDate()));

        // --- format ---

        // mock user sets value
        mCamera.photo().setFormat(CameraPhoto.Format.FULL_FRAME);

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.photo(), allOf(
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.photo().updateFormat(CameraPhoto.Format.FULL_FRAME);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mCamera.photo(), allOf(
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(7));
        assertThat(mCamera.photo(), allOf(
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.photo().setFormat(CameraPhoto.Format.LARGE);

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mCamera.photo(), allOf(
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(9));
        assertThat(mCamera.photo(), allOf(
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                settingIsUpToDate()));

        // --- file format ---

        // mock user sets value
        mCamera.photo().setFileFormat(CameraPhoto.FileFormat.DNG);

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mCamera.photo(), allOf(
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.photo().updateFileFormat(CameraPhoto.FileFormat.DNG);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(11));
        assertThat(mCamera.photo(), allOf(
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(11));
        assertThat(mCamera.photo(), allOf(
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.photo().setFileFormat(CameraPhoto.FileFormat.DNG_AND_JPEG);

        assertThat(mComponentChangeCnt, is(12));
        assertThat(mCamera.photo(), allOf(
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(13));
        assertThat(mCamera.photo(), allOf(
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                settingIsUpToDate()));

        // --- burst ---

        // mock user sets value
        mCamera.photo().setBurstValue(CameraPhoto.BurstValue.BURST_4_OVER_2S);

        assertThat(mComponentChangeCnt, is(14));
        assertThat(mCamera.photo(), allOf(
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_4_OVER_2S),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.photo().updateBurstValue(CameraPhoto.BurstValue.BURST_4_OVER_2S);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(15));
        assertThat(mCamera.photo(), allOf(
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_4_OVER_2S),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(15));
        assertThat(mCamera.photo(), allOf(
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_4_OVER_2S),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.photo().setBurstValue(CameraPhoto.BurstValue.BURST_4_OVER_4S);

        assertThat(mComponentChangeCnt, is(16));
        assertThat(mCamera.photo(), allOf(
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_4_OVER_4S),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(17));
        assertThat(mCamera.photo(), allOf(
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_4_OVER_2S),
                settingIsUpToDate()));

        // --- bracketing ---

        // mock user sets value
        mCamera.photo().setBracketingValue(CameraPhoto.BracketingValue.EV_2);

        assertThat(mComponentChangeCnt, is(18));
        assertThat(mCamera.photo(), allOf(
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.photo().updateBracketingValue(CameraPhoto.BracketingValue.EV_2);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(19));
        assertThat(mCamera.photo(), allOf(
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(19));
        assertThat(mCamera.photo(), allOf(
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.photo().setBracketingValue(CameraPhoto.BracketingValue.EV_3);

        assertThat(mComponentChangeCnt, is(20));
        assertThat(mCamera.photo(), allOf(
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_3),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(21));
        assertThat(mCamera.photo(), allOf(
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpToDate()));

        // --- time-lapse ---

        // mock user sets value
        mCamera.photo().setTimelapseInterval(6);

        assertThat(mComponentChangeCnt, is(22));
        assertThat(mCamera.photo(), allOf(
                photoSettingTimelapseIntervalIs(6.0),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.photo().updateTimelapseInterval(6);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(23));
        assertThat(mCamera.photo(), allOf(
                photoSettingTimelapseIntervalIs(6.0),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(23));
        assertThat(mCamera.photo(), allOf(
                photoSettingTimelapseIntervalIs(6.0),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.photo().setTimelapseInterval(3);

        assertThat(mComponentChangeCnt, is(24));
        assertThat(mCamera.photo(), allOf(
                photoSettingTimelapseIntervalIs(3.0),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(25));
        assertThat(mCamera.photo(), allOf(
                photoSettingTimelapseIntervalIs(6.0),
                settingIsUpToDate()));

        // --- GPS-lapse ---

        // mock user sets value
        mCamera.photo().setGpslapseInterval(2.5);

        assertThat(mComponentChangeCnt, is(26));
        assertThat(mCamera.photo(), allOf(
                photoSettingGpslapseIntervalIs(2.5),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.photo().updateGpslapseInterval(2.5);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(27));
        assertThat(mCamera.photo(), allOf(
                photoSettingGpslapseIntervalIs(2.5),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(27));
        assertThat(mCamera.photo(), allOf(
                photoSettingGpslapseIntervalIs(2.5),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.photo().setGpslapseInterval(7.5);

        assertThat(mComponentChangeCnt, is(28));
        assertThat(mCamera.photo(), allOf(
                photoSettingGpslapseIntervalIs(7.5),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(29));
        assertThat(mCamera.photo(), allOf(
                photoSettingGpslapseIntervalIs(2.5),
                settingIsUpToDate()));

        // --- set single mode ---

        // mock user sets value
        mCamera.photo().setSingleMode(CameraPhoto.Format.RECTILINEAR, CameraPhoto.FileFormat.JPEG);

        assertThat(mComponentChangeCnt, is(30));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.photo()
                   .updateMode(CameraPhoto.Mode.SINGLE)
                   .updateFormat(CameraPhoto.Format.RECTILINEAR)
                   .updateFileFormat(CameraPhoto.FileFormat.JPEG);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(31));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(31));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.photo().setSingleMode(CameraPhoto.Format.LARGE, CameraPhoto.FileFormat.DNG_AND_JPEG);

        assertThat(mComponentChangeCnt, is(32));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(33));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                settingIsUpToDate()));

        // --- set burst mode ---

        // mock user sets value
        mCamera.photo().setBurstMode(CameraPhoto.Format.FULL_FRAME, CameraPhoto.FileFormat.DNG,
                CameraPhoto.BurstValue.BURST_10_OVER_1S);

        assertThat(mComponentChangeCnt, is(34));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_1S),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.photo()
                   .updateMode(CameraPhoto.Mode.BURST)
                   .updateFormat(CameraPhoto.Format.FULL_FRAME)
                   .updateFileFormat(CameraPhoto.FileFormat.DNG)
                   .updateBurstValue(CameraPhoto.BurstValue.BURST_10_OVER_1S);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(35));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_1S),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(35));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_1S),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.photo().setBurstMode(CameraPhoto.Format.LARGE, CameraPhoto.FileFormat.DNG_AND_JPEG,
                CameraPhoto.BurstValue.BURST_10_OVER_2S);

        assertThat(mComponentChangeCnt, is(36));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(37));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_1S),
                settingIsUpToDate()));

        // --- set bracketing mode ---

        // mock user sets value
        mCamera.photo().setBracketingMode(CameraPhoto.Format.LARGE, CameraPhoto.FileFormat.JPEG,
                CameraPhoto.BracketingValue.EV_1_2);

        assertThat(mComponentChangeCnt, is(38));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.photo()
                   .updateMode(CameraPhoto.Mode.BRACKETING)
                   .updateFormat(CameraPhoto.Format.LARGE)
                   .updateFileFormat(CameraPhoto.FileFormat.JPEG)
                   .updateBracketingValue(CameraPhoto.BracketingValue.EV_1_2);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(39));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(39));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.photo().setBracketingMode(CameraPhoto.Format.RECTILINEAR, CameraPhoto.FileFormat.DNG_AND_JPEG,
                CameraPhoto.BracketingValue.EV_1_3);

        assertThat(mComponentChangeCnt, is(40));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_3),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(41));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                settingIsUpToDate()));

        // --- set time-lapse mode ---

        // mock user sets value
        mCamera.photo().setTimelapseMode(CameraPhoto.Format.FULL_FRAME, CameraPhoto.FileFormat.DNG_AND_JPEG, 3.1);

        assertThat(mComponentChangeCnt, is(42));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingTimelapseIntervalIs(3.1),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.photo()
                   .updateMode(CameraPhoto.Mode.TIME_LAPSE)
                   .updateFormat(CameraPhoto.Format.FULL_FRAME)
                   .updateFileFormat(CameraPhoto.FileFormat.DNG_AND_JPEG)
                   .updateTimelapseInterval(3.1);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(43));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingTimelapseIntervalIs(3.1),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(43));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingTimelapseIntervalIs(3.1),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.photo().setTimelapseMode(CameraPhoto.Format.RECTILINEAR, CameraPhoto.FileFormat.JPEG, 4.4);

        assertThat(mComponentChangeCnt, is(44));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingTimelapseIntervalIs(4.4),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(45));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingTimelapseIntervalIs(3.1),
                settingIsUpToDate()));

        // --- set GPS-lapse mode ---

        // mock user sets value
        mCamera.photo().setGpslapseMode(CameraPhoto.Format.LARGE, CameraPhoto.FileFormat.DNG, 7.2);

        assertThat(mComponentChangeCnt, is(46));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.GPS_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                photoSettingGpslapseIntervalIs(7.2),
                settingIsUpdating()));

        // mock backend updates value
        mCameraImpl.photo()
                   .updateMode(CameraPhoto.Mode.GPS_LAPSE)
                   .updateFormat(CameraPhoto.Format.LARGE)
                   .updateFileFormat(CameraPhoto.FileFormat.DNG)
                   .updateGpslapseInterval(7.2);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(47));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.GPS_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                photoSettingGpslapseIntervalIs(7.2),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(47));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.GPS_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                photoSettingGpslapseIntervalIs(7.2),
                settingIsUpToDate()));

        // mock user sets value
        mCamera.photo().setGpslapseMode(CameraPhoto.Format.RECTILINEAR, CameraPhoto.FileFormat.JPEG, 1);

        assertThat(mComponentChangeCnt, is(48));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.GPS_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingGpslapseIntervalIs(1.0),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(49));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.GPS_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.LARGE),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG),
                photoSettingGpslapseIntervalIs(7.2),
                settingIsUpToDate()));
    }

    @Test
    public void testStartStopRecording() {
        mCameraImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.UNAVAILABLE),
                recordingStartTimeIs(null),
                recordingMediaIdIs(null)));
        assertThat(mCamera.canStartRecording(), is(false));
        assertThat(mCamera.canStopRecording(), is(false));

        // backend moves to stopped
        mCameraImpl.recordingState().updateState(CameraRecording.State.FunctionState.STOPPED);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STOPPED),
                recordingStartTimeIs(null),
                recordingMediaIdIs(null)));
        assertThat(mCamera.canStartRecording(), is(true));
        assertThat(mCamera.canStopRecording(), is(false));

        // start recording
        mCamera.startRecording();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mStartRecordingCalled, is(true));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STARTING),
                recordingStartTimeIs(null),
                recordingMediaIdIs(null)));
        assertThat(mCamera.canStartRecording(), is(false));
        assertThat(mCamera.canStopRecording(), is(true));

        Date startTime = new Date();
        mCameraImpl.recordingState()
                   .updateState(CameraRecording.State.FunctionState.STARTED)
                   .updateStartTime(startTime.getTime());
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STARTED),
                recordingStartTimeIs(startTime),
                recordingMediaIdIs(null)));
        assertThat(mCamera.canStartRecording(), is(false));
        assertThat(mCamera.canStopRecording(), is(true));

        // stop recording
        mCamera.stopRecording();

        assertThat(mComponentChangeCnt, is(5));
        assertThat(mBackend.mStopRecordingCalled, is(true));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STOPPING),
                recordingStartTimeIs(null),
                recordingMediaIdIs(null)));
        assertThat(mCamera.canStartRecording(), is(false));
        assertThat(mCamera.canStopRecording(), is(false));

        mCameraImpl.recordingState()
                   .updateState(CameraRecording.State.FunctionState.STOPPED)
                   .updateMediaId("M123");
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STOPPED),
                recordingStartTimeIs(null),
                recordingMediaIdIs("M123")));
        assertThat(mCamera.canStartRecording(), is(true));
        assertThat(mCamera.canStopRecording(), is(false));

        // unavailable
        mCameraImpl.recordingState().updateState(CameraRecording.State.FunctionState.UNAVAILABLE);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.UNAVAILABLE),
                recordingStartTimeIs(null),
                recordingMediaIdIs(null)));
        assertThat(mCamera.canStartRecording(), is(false));
        assertThat(mCamera.canStopRecording(), is(false));

        mBackend.mStartRecordingCalled = false;
        mCamera.startRecording();

        assertThat(mComponentChangeCnt, is(7));
        // start recording should not go to backend when unavailable
        assertThat(mBackend.mStartRecordingCalled, is(false));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.UNAVAILABLE),
                recordingStartTimeIs(null),
                recordingMediaIdIs(null)));
        assertThat(mCamera.canStartRecording(), is(false));
        assertThat(mCamera.canStopRecording(), is(false));
    }

    @Test
    public void testAutoRecord() {
        mCameraImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mAutoRecord, is(false));
        assertThat(mCamera.autoRecord(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsUnavailable(),
                settingIsUpToDate()));

        // change setting, should do nothing since unavailable
        mCamera.autoRecord().toggle();
        assertThat(mBackend.mAutoRecord, is(false));
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.autoRecord(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsUnavailable(),
                settingIsUpToDate()));

        // mock backend notifies setting supported
        mCameraImpl.autoRecord().updateSupportedFlag(true);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.autoRecord(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsAvailable(),
                settingIsUpToDate()));

        // change setting
        mCamera.autoRecord().toggle();
        assertThat(mBackend.mAutoRecord, is(true));
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCamera.autoRecord(), allOf(
                optionalBooleanSettingValueIs(true),
                optionalSettingIsAvailable(),
                settingIsUpdating()));

        // mock update from low-level
        mCameraImpl.autoRecord().updateValue(true);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.autoRecord(), allOf(
                optionalBooleanSettingValueIs(true),
                optionalSettingIsAvailable(),
                settingIsUpToDate()));

        // change setting again
        mCamera.autoRecord().toggle();
        assertThat(mBackend.mAutoRecord, is(false));
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mCamera.autoRecord(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsAvailable(),
                settingIsUpdating()));

        // mock update from low-level
        mCameraImpl.autoRecord().updateValue(false);
        mCameraImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.autoRecord(), allOf(
                optionalBooleanSettingValueIs(false),
                optionalSettingIsAvailable(),
                settingIsUpToDate()));
    }

    @Test
    public void testStartStopPhotoCapture() {
        mCameraImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.UNAVAILABLE),
                photoCountIs(0),
                photoMediaIdIs(null)));
        assertThat(mCamera.canStartPhotoCapture(), is(false));
        assertThat(mCamera.canStopPhotoCapture(), is(false));

        // backend moves to ready
        mCameraImpl.photoState().updateState(CameraPhoto.State.FunctionState.STOPPED);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STOPPED),
                photoCountIs(0),
                photoMediaIdIs(null)));
        assertThat(mCamera.canStartPhotoCapture(), is(true));
        assertThat(mCamera.canStopPhotoCapture(), is(false));

        // take one photo
        mCamera.startPhotoCapture();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mStartPhotoCaptureCalled, is(true));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STARTED),
                photoCountIs(0),
                photoMediaIdIs(null)));
        assertThat(mCamera.canStartPhotoCapture(), is(false));
        assertThat(mCamera.canStopPhotoCapture(), is(false));

        // taken, photo count increments
        mCameraImpl.photoState().updatePhotoCount(1);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STARTED),
                photoCountIs(1),
                photoMediaIdIs(null)));
        assertThat(mCamera.canStartPhotoCapture(), is(false));
        assertThat(mCamera.canStopPhotoCapture(), is(false));

        // trying to stop photo capture should not trigger any change
        mCamera.stopPhotoCapture();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STARTED),
                photoCountIs(1),
                photoMediaIdIs(null)));
        assertThat(mCamera.canStartPhotoCapture(), is(false));
        assertThat(mCamera.canStopPhotoCapture(), is(false));

        // saved, with media id
        mCameraImpl.photoState()
                   .updateState(CameraPhoto.State.FunctionState.STOPPED)
                   .updateMediaId("M123");
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(5));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STOPPED),
                photoCountIs(0),
                photoMediaIdIs("M123")));
        assertThat(mCamera.canStartPhotoCapture(), is(true));
        assertThat(mCamera.canStopPhotoCapture(), is(false));

        // switch to time-lapse mode
        mBackend.mStartPhotoCaptureCalled = false;
        mCameraImpl.photo().updateMode(CameraPhoto.Mode.TIME_LAPSE);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(6));
        assertThat(mCamera.photo(), photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE));
        assertThat(mCamera.canStartPhotoCapture(), is(true));
        assertThat(mCamera.canStopPhotoCapture(), is(false));

        // start photo capture
        mCamera.startPhotoCapture();

        assertThat(mComponentChangeCnt, is(7));
        assertThat(mBackend.mStartPhotoCaptureCalled, is(true));
        assertThat(mCamera.photoState(), photoStateIs(CameraPhoto.State.FunctionState.STARTED));
        assertThat(mCamera.canStartPhotoCapture(), is(false));
        assertThat(mCamera.canStopPhotoCapture(), is(true));

        // stop photo capture
        mCamera.stopPhotoCapture();

        assertThat(mComponentChangeCnt, is(8));
        assertThat(mBackend.mStopPhotoCaptureCalled, is(true));
        assertThat(mCamera.photoState(), photoStateIs(CameraPhoto.State.FunctionState.STOPPING));
        assertThat(mCamera.canStartPhotoCapture(), is(false));
        assertThat(mCamera.canStopPhotoCapture(), is(false));

        // done
        mCameraImpl.photoState()
                   .updateState(CameraPhoto.State.FunctionState.STOPPED);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(9));
        assertThat(mCamera.photoState(), photoStateIs(CameraPhoto.State.FunctionState.STOPPED));
        assertThat(mCamera.canStartPhotoCapture(), is(true));
        assertThat(mCamera.canStopPhotoCapture(), is(false));

        // unavailable
        mCameraImpl.photoState().updateState(CameraPhoto.State.FunctionState.UNAVAILABLE);
        mCameraImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(10));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.UNAVAILABLE),
                photoCountIs(0),
                photoMediaIdIs(null)));
        assertThat(mCamera.canStartPhotoCapture(), is(false));
        assertThat(mCamera.canStopPhotoCapture(), is(false));

        mBackend.mStartPhotoCaptureCalled = false;
        mCamera.startPhotoCapture();

        assertThat(mComponentChangeCnt, is(10));
        // take photo should not go to backend when unavailable
        assertThat(mBackend.mStartPhotoCaptureCalled, is(false));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.UNAVAILABLE),
                photoCountIs(0),
                photoMediaIdIs(null)));
        assertThat(mCamera.canStartPhotoCapture(), is(false));
        assertThat(mCamera.canStopPhotoCapture(), is(false));
    }

    @Test
    public void testCancelRollbacks() {
        mCameraImpl.mode()
                   .updateAvailableValues(EnumSet.allOf(Camera.Mode.class));
        mCameraImpl.exposure()
                   .updateSupportedModes(EnumSet.allOf(CameraExposure.Mode.class))
                   .updateMaximumIsoSensitivities(EnumSet.allOf(CameraExposure.IsoSensitivity.class));
        mCameraImpl.whiteBalance()
                   .updateSupportedModes(EnumSet.allOf(CameraWhiteBalance.Mode.class));
        mCameraImpl.autoHdr().updateSupportedFlag(true).updateValue(false);
        mCameraImpl.style()
                   .updateSupportedStyles(EnumSet.allOf(CameraStyle.Style.class));
        mCameraImpl.exposureCompensation()
                   .updateAvailableValues(EnumSet.allOf(CameraEvCompensation.class));
        mCameraImpl.photo()
                   .updateCapabilities(Collections.singletonList(
                           CameraPhotoSettingCore.Capability.of(
                                   EnumSet.allOf(CameraPhoto.Mode.class),
                                   EnumSet.allOf(CameraPhoto.Format.class),
                                   EnumSet.allOf(CameraPhoto.FileFormat.class),
                                   true)));
        mCameraImpl.recording()
                   .updateCapabilities(Collections.singletonList(
                           CameraRecordingSettingCore.Capability.of(
                                   EnumSet.allOf(CameraRecording.Mode.class),
                                   EnumSet.allOf(CameraRecording.Resolution.class),
                                   EnumSet.allOf(CameraRecording.Framerate.class),
                                   true)));
        mCameraImpl.autoRecord().updateSupportedFlag(true).updateValue(false);
        mCameraImpl.createZoomIfNeeded().maxSpeed().updateBounds(DoubleRange.of(0, 1)).updateValue(0);

        CameraAlignmentSettingCore alignmentImpl = mCameraImpl.createAlignmentIfNeeded();
        alignmentImpl.updateSupportedYawRange(DoubleRange.of(-2.0, 2.0)).updateYaw(1.0);
        alignmentImpl.updateSupportedPitchRange(DoubleRange.of(-4.0, 4.0)).updatePitch(2.0);
        alignmentImpl.updateSupportedRollRange(DoubleRange.of(-6.0, 6.0)).updateRoll(3.0);

        mCameraImpl.updateActiveFlag(true).publish();

        assertThat(mCamera.mode(), allOf(
                enumSettingValueIs(Camera.Mode.RECORDING),
                settingIsUpToDate()));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                settingIsUpToDate()));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.AUTOMATIC),
                settingIsUpToDate()));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(false),
                settingIsUpToDate()));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                settingIsUpToDate()));
        assertThat(mCamera.exposureCompensation(), allOf(
                enumSettingValueIs(CameraEvCompensation.EV_MINUS_3),
                settingIsUpToDate()));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                settingIsUpToDate()));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.STANDARD),
                settingIsUpToDate()));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(false),
                settingIsUpToDate()));

        CameraAlignment.Setting alignment = mCamera.alignment();
        assertThat(alignment, is(notNullValue()));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, 1.0, 2.0),
                alignmentSettingPitchIs(-4.0, 2.0, 4.0),
                alignmentSettingRollIs(-6.0, 3.0, 6.0),
                settingIsUpToDate()));

        CameraZoom zoom = mCamera.zoom();
        assertThat(zoom, notNullValue());
        assertThat(zoom.maxSpeed(), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));
        assertThat(zoom.velocityQualityDegradationAllowance(), allOf(
                booleanSettingValueIs(false),
                settingIsUpToDate()));

        // mock user changes all settings
        mCamera.mode().setValue(Camera.Mode.PHOTO);
        mCamera.exposure().setMode(CameraExposure.Mode.MANUAL);
        mCamera.whiteBalance().setMode(CameraWhiteBalance.Mode.CUSTOM);
        mCamera.autoHdr().setEnabled(true);
        mCamera.style().setStyle(CameraStyle.Style.PLOG);
        mCamera.exposureCompensation().setValue(CameraEvCompensation.EV_0);
        mCamera.photo().setMode(CameraPhoto.Mode.BURST);
        mCamera.recording().setMode(CameraRecording.Mode.HYPERLAPSE);
        mCamera.autoRecord().setEnabled(true);
        alignment.setYaw(1.5);
        alignment.setPitch(2.5);
        alignment.setRoll(3.5);
        zoom.maxSpeed().setValue(1);
        zoom.velocityQualityDegradationAllowance().setEnabled(true);

        // cancel all rollbacks
        mCameraImpl.cancelSettingsRollbacks();

        // all setting should be updated to user values
        assertThat(mCamera.mode(), allOf(
                enumSettingValueIs(Camera.Mode.PHOTO),
                settingIsUpToDate()));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                settingIsUpToDate()));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                settingIsUpToDate()));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpToDate()));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.PLOG),
                settingIsUpToDate()));
        assertThat(mCamera.exposureCompensation(), allOf(
                enumSettingValueIs(CameraEvCompensation.EV_0),
                settingIsUpToDate()));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                settingIsUpToDate()));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                settingIsUpToDate()));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpToDate()));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, 1.5, 2.0),
                alignmentSettingPitchIs(-4.0, 2.5, 4.0),
                alignmentSettingRollIs(-6.0, 3.5, 6.0),
                settingIsUpToDate()));
        assertThat(zoom.maxSpeed(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));
        assertThat(zoom.velocityQualityDegradationAllowance(), allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mCamera.mode(), allOf(
                enumSettingValueIs(Camera.Mode.PHOTO),
                settingIsUpToDate()));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                settingIsUpToDate()));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                settingIsUpToDate()));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpToDate()));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.PLOG),
                settingIsUpToDate()));
        assertThat(mCamera.exposureCompensation(), allOf(
                enumSettingValueIs(CameraEvCompensation.EV_0),
                settingIsUpToDate()));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                settingIsUpToDate()));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                settingIsUpToDate()));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpToDate()));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, 1.5, 2.0),
                alignmentSettingPitchIs(-4.0, 2.5, 4.0),
                alignmentSettingRollIs(-6.0, 3.5, 6.0),
                settingIsUpToDate()));
        assertThat(zoom.maxSpeed(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));
        assertThat(zoom.velocityQualityDegradationAllowance(), allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));
    }

    @Test
    public void testEnums() {
        // this test validates the declaration order of public API enums, since we depend on that order internally
        assertThat(EnumSet.allOf(CameraRecording.Resolution.class), contains(
                CameraRecording.Resolution.RES_DCI_4K,
                CameraRecording.Resolution.RES_UHD_4K,
                CameraRecording.Resolution.RES_2_7K,
                CameraRecording.Resolution.RES_1080P,
                CameraRecording.Resolution.RES_1080P_4_3,
                CameraRecording.Resolution.RES_720P,
                CameraRecording.Resolution.RES_720P_4_3,
                CameraRecording.Resolution.RES_480P,
                CameraRecording.Resolution.RES_UHD_8K,
                CameraRecording.Resolution.RES_5K));

        assertThat(EnumSet.allOf(CameraRecording.Framerate.class), contains(
                CameraRecording.Framerate.FPS_240,
                CameraRecording.Framerate.FPS_200,
                CameraRecording.Framerate.FPS_192,
                CameraRecording.Framerate.FPS_120,
                CameraRecording.Framerate.FPS_100,
                CameraRecording.Framerate.FPS_96,
                CameraRecording.Framerate.FPS_60,
                CameraRecording.Framerate.FPS_50,
                CameraRecording.Framerate.FPS_48,
                CameraRecording.Framerate.FPS_30,
                CameraRecording.Framerate.FPS_25,
                CameraRecording.Framerate.FPS_24,
                CameraRecording.Framerate.FPS_20,
                CameraRecording.Framerate.FPS_15,
                CameraRecording.Framerate.FPS_10,
                CameraRecording.Framerate.FPS_9,
                CameraRecording.Framerate.FPS_8_6));

        assertThat(EnumSet.allOf(CameraRecording.HyperlapseValue.class), contains(
                CameraRecording.HyperlapseValue.RATIO_15,
                CameraRecording.HyperlapseValue.RATIO_30,
                CameraRecording.HyperlapseValue.RATIO_60,
                CameraRecording.HyperlapseValue.RATIO_120,
                CameraRecording.HyperlapseValue.RATIO_240));

        assertThat(EnumSet.allOf(CameraPhoto.Format.class), contains(
                CameraPhoto.Format.RECTILINEAR,
                CameraPhoto.Format.FULL_FRAME,
                CameraPhoto.Format.LARGE));

        assertThat(EnumSet.allOf(CameraPhoto.FileFormat.class), contains(
                CameraPhoto.FileFormat.JPEG,
                CameraPhoto.FileFormat.DNG,
                CameraPhoto.FileFormat.DNG_AND_JPEG));

        assertThat(EnumSet.allOf(CameraPhoto.BurstValue.class), contains(
                CameraPhoto.BurstValue.BURST_14_OVER_4S,
                CameraPhoto.BurstValue.BURST_14_OVER_2S,
                CameraPhoto.BurstValue.BURST_14_OVER_1S,
                CameraPhoto.BurstValue.BURST_10_OVER_4S,
                CameraPhoto.BurstValue.BURST_10_OVER_2S,
                CameraPhoto.BurstValue.BURST_10_OVER_1S,
                CameraPhoto.BurstValue.BURST_4_OVER_4S,
                CameraPhoto.BurstValue.BURST_4_OVER_2S,
                CameraPhoto.BurstValue.BURST_4_OVER_1S));

        assertThat(EnumSet.allOf(CameraPhoto.BracketingValue.class), contains(
                CameraPhoto.BracketingValue.EV_1,
                CameraPhoto.BracketingValue.EV_2,
                CameraPhoto.BracketingValue.EV_3,
                CameraPhoto.BracketingValue.EV_1_2,
                CameraPhoto.BracketingValue.EV_1_3,
                CameraPhoto.BracketingValue.EV_2_3,
                CameraPhoto.BracketingValue.EV_1_2_3));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements CameraCore.Backend {

        private Camera.Mode mMode;

        private CameraExposure.Mode mExposureMode;

        private CameraExposure.ShutterSpeed mShutterSpeed;

        private CameraExposure.IsoSensitivity mIso;

        private CameraExposure.IsoSensitivity mMaxIso;

        private CameraExposure.AutoExposureMeteringMode mAutoExposureMeteringMode;

        private CameraExposureLock.Mode mExposureLockMode;

        private double mExposureLockCenterX;

        private double mExposureLockCenterY;

        private CameraEvCompensation mEvCompensation;

        private CameraWhiteBalance.Mode mWhiteBalanceMode;

        private CameraWhiteBalance.Temperature mTemperature;

        private boolean mWhiteBalanceLock;

        private boolean mAutoHdr;

        private CameraStyle.Style mStyle;

        private int mSaturation;

        private int mContrast;

        private int mSharpness;

        private double mYawAlignment;

        private double mPitchAlignment;

        private double mRollAlignment;

        private boolean mAlignmentReset;

        private double mMaxZoomSpeed;

        private boolean mQualityDegradationAllowance;

        private CameraPhoto.Mode mPhotoMode;

        private CameraPhoto.Format mFormat;

        private CameraPhoto.FileFormat mFileFormat;

        private CameraPhoto.BurstValue mBurst;

        private CameraPhoto.BracketingValue mBracketing;

        private Double mTimelapseInterval;

        private Double mGpslapseInterval;

        private CameraRecording.Mode mRecordingMode;

        private CameraRecording.Resolution mResolution;

        private CameraRecording.Framerate mFramerate;

        private CameraRecording.HyperlapseValue mHyperlapse;

        private boolean mStartPhotoCaptureCalled, mStopPhotoCaptureCalled, mStartRecordingCalled, mStopRecordingCalled;

        private CameraZoom.ControlMode mZoomControlMode;

        private double mZoomTarget;

        private boolean mAutoRecord;

        @Override
        public boolean setMode(@NonNull Camera.Mode mode) {
            mMode = mode;
            return true;
        }

        @Override
        public boolean setExposure(@NonNull CameraExposure.Mode mode,
                                   @Nullable CameraExposure.ShutterSpeed manualShutterSpeed,
                                   @Nullable CameraExposure.IsoSensitivity manualIsoSensitivity,
                                   @Nullable CameraExposure.IsoSensitivity maxIsoSensitivity,
                                   @Nullable CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
            mExposureMode = mode;
            mShutterSpeed = manualShutterSpeed;
            mIso = manualIsoSensitivity;
            mMaxIso = maxIsoSensitivity;
            mAutoExposureMeteringMode = autoExposureMeteringMode;
            return true;
        }

        @Override
        public boolean setEvCompensation(@NonNull CameraEvCompensation ev) {
            mEvCompensation = ev;
            return true;
        }

        @Override
        public boolean setWhiteBalance(@NonNull CameraWhiteBalance.Mode mode,
                                       @Nullable CameraWhiteBalance.Temperature temperature) {
            mWhiteBalanceMode = mode;
            mTemperature = temperature;
            return true;
        }

        @Override
        public boolean setWhiteBalanceLock(boolean locked) {
            mWhiteBalanceLock = locked;
            return true;
        }

        @Override
        public boolean setAutoHdr(boolean enable) {
            mAutoHdr = enable;
            return true;
        }

        @Override
        public boolean setStyle(@NonNull CameraStyle.Style style) {
            mStyle = style;
            return true;
        }

        @Override
        public boolean setStyleParameters(int saturation, int contrast, int sharpness) {
            mSaturation = saturation;
            mContrast = contrast;
            mSharpness = sharpness;
            return true;
        }

        @Override
        public boolean setAlignment(double yaw, double pitch, double roll) {
            mYawAlignment = yaw;
            mPitchAlignment = pitch;
            mRollAlignment = roll;
            return true;
        }

        @Override
        public boolean resetAlignment() {
            mAlignmentReset = true;
            return true;
        }

        @Override
        public boolean setMaxZoomSpeed(double speed) {
            mMaxZoomSpeed = speed;
            return true;
        }

        @Override
        public boolean setQualityDegradationAllowance(boolean allowed) {
            mQualityDegradationAllowance = allowed;
            return true;
        }

        @Override
        public boolean setPhoto(@NonNull CameraPhoto.Mode mode, @Nullable CameraPhoto.Format format,
                                @Nullable CameraPhoto.FileFormat fileFormat, @Nullable CameraPhoto.BurstValue burst,
                                @Nullable CameraPhoto.BracketingValue bracketing, @Nullable Double timelapseInterval,
                                @Nullable Double gpslapseInterval) {
            mPhotoMode = mode;
            mFormat = format;
            mFileFormat = fileFormat;
            mBurst = burst;
            mBracketing = bracketing;
            mTimelapseInterval = timelapseInterval;
            mGpslapseInterval = gpslapseInterval;
            return true;
        }

        @Override
        public boolean setRecording(@NonNull CameraRecording.Mode mode, @Nullable CameraRecording.Resolution resolution,
                                    @Nullable CameraRecording.Framerate framerate,
                                    @Nullable CameraRecording.HyperlapseValue hyperlapse) {
            mRecordingMode = mode;
            mResolution = resolution;
            mFramerate = framerate;
            mHyperlapse = hyperlapse;
            return true;
        }

        @Override
        public void control(@NonNull CameraZoom.ControlMode mode, double target) {
            mZoomControlMode = mode;
            mZoomTarget = target;
        }

        @Override
        public boolean startPhotoCapture() {
            mStartPhotoCaptureCalled = true;
            return true;
        }

        @Override
        public boolean stopPhotoCapture() {
            mStopPhotoCaptureCalled = true;
            return true;
        }

        @Override
        public boolean startRecording() {
            mStartRecordingCalled = true;
            return true;
        }

        @Override
        public boolean stopRecording() {
            mStopRecordingCalled = true;
            return true;
        }

        @Override
        public boolean setAutoRecord(boolean enable) {
            mAutoRecord = enable;
            return true;
        }

        @Override
        public boolean setExposureLock(@NonNull CameraExposureLock.Mode mode, double centerX, double centerY) {
            mExposureLockMode = mode;
            mExposureLockCenterX = centerX;
            mExposureLockCenterY = centerY;
            return true;
        }
    }
}
