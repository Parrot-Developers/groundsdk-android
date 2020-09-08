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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.camera;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.MainCamera;
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
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import org.junit.Test;

import java.util.Date;
import java.util.EnumSet;

import static com.parrot.drone.groundsdk.AlignmentSettingMatcher.alignmentSettingPitchIs;
import static com.parrot.drone.groundsdk.AlignmentSettingMatcher.alignmentSettingRollIs;
import static com.parrot.drone.groundsdk.AlignmentSettingMatcher.alignmentSettingYawIs;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabled;
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
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingIsEnabling;
import static com.parrot.drone.groundsdk.OptionalBooleanSettingMatcher.optionalBooleanSettingValueIs;
import static com.parrot.drone.groundsdk.OptionalSettingMatcher.optionalSettingIsAvailable;
import static com.parrot.drone.groundsdk.OptionalSettingMatcher.optionalSettingIsUnavailable;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingBracketingValueIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingBurstValueIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingFileFormatIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingFormatIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingGpslapseIntervalIs;
import static com.parrot.drone.groundsdk.PhotoSettingMatcher.photoSettingGpslapseIntervalRangeIs;
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
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingBitrateIs;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingFramerateIs;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingHyperlapseValueIs;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingModeIs;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingResolutionIs;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingSupportsFramerates;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingSupportsHyperlapseValues;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingSupportsModes;
import static com.parrot.drone.groundsdk.RecordingSettingMatcher.recordingSettingSupportsResolutions;
import static com.parrot.drone.groundsdk.RecordingStateMatcher.recordingDurationIs;
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiCameraRouterTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private Camera mCamera;

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

        mCamera = mDrone.getPeripheralStore().get(mMockSession, MainCamera.class);
        mDrone.getPeripheralStore().registerObserver(MainCamera.class, () -> {
            mCamera = mDrone.getPeripheralStore().get(mMockSession, MainCamera.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when drone has never been connected
        assertThat(mChangeCnt, is(0));
        assertThat(mCamera, nullValue());

        // connect drone, mocking receiving online only parameters, so something changes on disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().whiteBalanceLock(ArsdkFeatureCamera.Supported.SUPPORTED).encode()));

        assertThat(mChangeCnt, is(1));
        assertThat(mCamera, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera, notNullValue());

        // forget drone
        mDrone.forget();

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera, nullValue());
    }

    @Test
    public void testActiveState() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1, caps().encode()));

        assertThat(mChangeCnt, is(1));

        // check default value
        assertThat(mCamera.isActive(), is(true));

        // mock inactive state from drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraCameraStates(0));

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.isActive(), is(false));

        // mock active state from drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraCameraStates(1));

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.isActive(), is(true));
    }

    @Test
    public void testMode() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().modes(ArsdkFeatureCamera.CameraMode.PHOTO, ArsdkFeatureCamera.CameraMode.RECORDING)
                      .encode(),
                ArsdkEncoder.encodeCameraCameraMode(0, ArsdkFeatureCamera.CameraMode.RECORDING)));

        assertThat(mChangeCnt, is(1));

        // check initial value
        assertThat(mCamera.mode(), allOf(
                enumSettingSupports(EnumSet.of(Camera.Mode.PHOTO, Camera.Mode.RECORDING)),
                enumSettingIsUpToDateAt(Camera.Mode.RECORDING)));

        // check backend change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraCameraMode(0,
                ArsdkFeatureCamera.CameraMode.PHOTO));

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.mode(), enumSettingIsUpToDateAt(Camera.Mode.PHOTO));

        // change mode
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.cameraSetCameraMode(0, ArsdkFeatureCamera.CameraMode.RECORDING)));
        mCamera.mode().setValue(Camera.Mode.RECORDING);

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.mode(), enumSettingIsUpdatingTo(Camera.Mode.RECORDING));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraCameraMode(0,
                ArsdkFeatureCamera.CameraMode.RECORDING));

        assertThat(mChangeCnt, is(4));
        assertThat(mCamera.mode(), enumSettingIsUpToDateAt(Camera.Mode.RECORDING));
        mMockArsdkCore.assertNoExpectation();

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check still in recording mode
        assertThat(mChangeCnt, is(0));
        assertThat(mCamera.mode(), allOf(
                enumSettingSupports(EnumSet.of(Camera.Mode.PHOTO, Camera.Mode.RECORDING)),
                enumSettingIsUpToDateAt(Camera.Mode.RECORDING)));

        // change mode offline
        mCamera.mode().setValue(Camera.Mode.PHOTO);

        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.mode(), enumSettingIsUpToDateAt(Camera.Mode.PHOTO));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1,
                        caps().modes(ArsdkFeatureCamera.CameraMode.PHOTO, ArsdkFeatureCamera.CameraMode.RECORDING)
                              .encode())
                // mock receiving a different mode that what is stored offline
                .commandReceived(1, ArsdkEncoder.encodeCameraCameraMode(0,
                        ArsdkFeatureCamera.CameraMode.RECORDING))
                // offline mode should be sent to drone
                .expect(new Expectation.Command(1,
                        ExpectedCmd.cameraSetCameraMode(0, ArsdkFeatureCamera.CameraMode.PHOTO))));

        mMockArsdkCore.assertNoExpectation();
        assertThat(mChangeCnt, is(2)); // active state change
        assertThat(mCamera.mode(), enumSettingIsUpToDateAt(Camera.Mode.PHOTO));
    }

    @Test
    public void testExposureSettings() {
        long supportedShutterSpeed = ArsdkFeatureCamera.ShutterSpeed.toBitField(
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_100);
        long supportedIsos = ArsdkFeatureCamera.IsoSensitivity.toBitField(
                ArsdkFeatureCamera.IsoSensitivity.ISO_100,
                ArsdkFeatureCamera.IsoSensitivity.ISO_200,
                ArsdkFeatureCamera.IsoSensitivity.ISO_320);
        long supportedMaxIsos = ArsdkFeatureCamera.IsoSensitivity.toBitField(
                ArsdkFeatureCamera.IsoSensitivity.ISO_160,
                ArsdkFeatureCamera.IsoSensitivity.ISO_320);
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().exposureModes(ArsdkFeatureCamera.ExposureMode.values())
                        .autoExposureMeteringModes(ArsdkFeatureCamera.AutoExposureMeteringMode.values())
                      .encode(),
                ArsdkEncoder.encodeCameraExposureSettings(0, ArsdkFeatureCamera.ExposureMode.MANUAL,
                        ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10, supportedShutterSpeed,
                        ArsdkFeatureCamera.IsoSensitivity.ISO_100, supportedIsos,
                        ArsdkFeatureCamera.IsoSensitivity.ISO_160, supportedMaxIsos,
                        ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD)));

        assertThat(mChangeCnt, is(1));

        // check capabilities
        assertThat(mCamera.exposure(), allOf(
                exposureSettingSupportsModes(EnumSet.allOf(CameraExposure.Mode.class)),
                exposureSettingSupportsManualShutterSpeeds(EnumSet.of(CameraExposure.ShutterSpeed.ONE,
                        CameraExposure.ShutterSpeed.ONE_OVER_10, CameraExposure.ShutterSpeed.ONE_OVER_100)),
                exposureSettingSupportsManualIsos(EnumSet.of(CameraExposure.IsoSensitivity.ISO_100,
                        CameraExposure.IsoSensitivity.ISO_200, CameraExposure.IsoSensitivity.ISO_320)),
                exposureSettingSupportsMaxIsos(EnumSet.of(CameraExposure.IsoSensitivity.ISO_160,
                        CameraExposure.IsoSensitivity.ISO_320)),
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_10),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_100),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_160),
                exposureSettingSupportsAutoExposureMeteringMode(EnumSet.of(
                        CameraExposure.AutoExposureMeteringMode.STANDARD,
                        CameraExposure.AutoExposureMeteringMode.CENTER_TOP)),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.STANDARD),
                settingIsUpToDate()));

        // change mode to automatic
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetExposureSettings(
                0, ArsdkFeatureCamera.ExposureMode.AUTOMATIC, ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10,
                ArsdkFeatureCamera.IsoSensitivity.ISO_100, ArsdkFeatureCamera.IsoSensitivity.ISO_160,
                ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD)));
        mCamera.exposure().setMode(CameraExposure.Mode.AUTOMATIC);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposureSettings(0,
                ArsdkFeatureCamera.ExposureMode.AUTOMATIC,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10, supportedShutterSpeed,
                ArsdkFeatureCamera.IsoSensitivity.ISO_100, supportedIsos,
                ArsdkFeatureCamera.IsoSensitivity.ISO_160, supportedMaxIsos,
                ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD));

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // change mode to manual shutter speed
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetExposureSettings(
                0, ArsdkFeatureCamera.ExposureMode.MANUAL_SHUTTER_SPEED,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_100, ArsdkFeatureCamera.IsoSensitivity.ISO_100,
                ArsdkFeatureCamera.IsoSensitivity.ISO_160, ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD)));
        mCamera.exposure().setManualMode(CameraExposure.ShutterSpeed.ONE_OVER_100);

        assertThat(mChangeCnt, is(4));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_100),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposureSettings(0,
                ArsdkFeatureCamera.ExposureMode.MANUAL_SHUTTER_SPEED,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_100, supportedShutterSpeed,
                ArsdkFeatureCamera.IsoSensitivity.ISO_100, supportedIsos,
                ArsdkFeatureCamera.IsoSensitivity.ISO_160, supportedMaxIsos,
                ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD));

        assertThat(mChangeCnt, is(5));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_100),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // change mode to manual ISO
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetExposureSettings(
                0, ArsdkFeatureCamera.ExposureMode.MANUAL_ISO_SENSITIVITY,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_100, ArsdkFeatureCamera.IsoSensitivity.ISO_320,
                ArsdkFeatureCamera.IsoSensitivity.ISO_160, ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD)));
        mCamera.exposure().setManualMode(CameraExposure.IsoSensitivity.ISO_320);

        assertThat(mChangeCnt, is(6));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_320),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposureSettings(0,
                ArsdkFeatureCamera.ExposureMode.MANUAL_ISO_SENSITIVITY,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_100, supportedShutterSpeed,
                ArsdkFeatureCamera.IsoSensitivity.ISO_320, supportedIsos,
                ArsdkFeatureCamera.IsoSensitivity.ISO_160, supportedMaxIsos,
                ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD));

        assertThat(mChangeCnt, is(7));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_320),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // change mode to auto, with max ISO
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetExposureSettings(
                0, ArsdkFeatureCamera.ExposureMode.AUTOMATIC,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_100, ArsdkFeatureCamera.IsoSensitivity.ISO_320,
                ArsdkFeatureCamera.IsoSensitivity.ISO_320, ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD)));
        mCamera.exposure().setAutoMode(CameraExposure.IsoSensitivity.ISO_320);

        assertThat(mChangeCnt, is(8));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_320),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposureSettings(0,
                ArsdkFeatureCamera.ExposureMode.AUTOMATIC,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_100, supportedShutterSpeed,
                ArsdkFeatureCamera.IsoSensitivity.ISO_320, supportedIsos,
                ArsdkFeatureCamera.IsoSensitivity.ISO_320, supportedMaxIsos,
                ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD));

        assertThat(mChangeCnt, is(9));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_320),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // change to manual mode with both shutter speed and iso sensitivity
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetExposureSettings(
                0, ArsdkFeatureCamera.ExposureMode.MANUAL,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10, ArsdkFeatureCamera.IsoSensitivity.ISO_200,
                ArsdkFeatureCamera.IsoSensitivity.ISO_320, ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD)));
        mCamera.exposure().setManualMode(CameraExposure.ShutterSpeed.ONE_OVER_10,
                CameraExposure.IsoSensitivity.ISO_200);

        assertThat(mChangeCnt, is(10));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_10),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_200),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposureSettings(0,
                ArsdkFeatureCamera.ExposureMode.MANUAL,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10, supportedShutterSpeed,
                ArsdkFeatureCamera.IsoSensitivity.ISO_200, supportedIsos,
                ArsdkFeatureCamera.IsoSensitivity.ISO_320, supportedMaxIsos,
                ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD));

        assertThat(mChangeCnt, is(11));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_10),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_200),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // set max ISO sensitivity in manual mode, no command is sent
        mCamera.exposure().setMaxIsoSensitivity(CameraExposure.IsoSensitivity.ISO_160);
        mMockArsdkCore.assertNoExpectation();

        assertThat(mChangeCnt, is(12));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_160),
                settingIsUpToDate()));

        // set AE metering mode in manual mode, no command is sent
        mCamera.exposure().setAutoExposureMeteringMode(CameraExposure.AutoExposureMeteringMode.CENTER_TOP);
        mMockArsdkCore.assertNoExpectation();

        assertThat(mChangeCnt, is(13));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.CENTER_TOP),
                settingIsUpToDate()));

        // change mode to auto, max ISO & AE metering mode are now sent
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetExposureSettings(
                0, ArsdkFeatureCamera.ExposureMode.AUTOMATIC,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10, ArsdkFeatureCamera.IsoSensitivity.ISO_200,
                ArsdkFeatureCamera.IsoSensitivity.ISO_160, ArsdkFeatureCamera.AutoExposureMeteringMode.CENTER_TOP)));
        mCamera.exposure().setMode(CameraExposure.Mode.AUTOMATIC);

        assertThat(mChangeCnt, is(14));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_160),
                settingIsUpdating()));

        // set shutter speed in auto mode, no command is sent
        mCamera.exposure().setManualShutterSpeed(CameraExposure.ShutterSpeed.ONE);
        mMockArsdkCore.assertNoExpectation();

        assertThat(mChangeCnt, is(15));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE),
                settingIsUpToDate()));

        // set ISO sensitivity in auto mode, no command is sent
        mCamera.exposure().setManualIsoSensitivity(CameraExposure.IsoSensitivity.ISO_100);
        mMockArsdkCore.assertNoExpectation();

        assertThat(mChangeCnt, is(16));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_100),
                settingIsUpToDate()));

        // change mode to manual, manual shutter speed and ISO sensitivity are now sent
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetExposureSettings(
                0, ArsdkFeatureCamera.ExposureMode.MANUAL,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1, ArsdkFeatureCamera.IsoSensitivity.ISO_100,
                ArsdkFeatureCamera.IsoSensitivity.ISO_160, ArsdkFeatureCamera.AutoExposureMeteringMode.CENTER_TOP)));
        mCamera.exposure().setMode(CameraExposure.Mode.MANUAL);

        assertThat(mChangeCnt, is(17));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_100),
                settingIsUpdating()));

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        assertThat(mChangeCnt, is(0));

        // check stored capabilities
        assertThat(mCamera.exposure(), allOf(
                exposureSettingSupportsModes(EnumSet.allOf(CameraExposure.Mode.class)),
                exposureSettingSupportsManualShutterSpeeds(EnumSet.of(CameraExposure.ShutterSpeed.ONE,
                        CameraExposure.ShutterSpeed.ONE_OVER_10, CameraExposure.ShutterSpeed.ONE_OVER_100)),
                exposureSettingSupportsManualIsos(EnumSet.of(CameraExposure.IsoSensitivity.ISO_100,
                        CameraExposure.IsoSensitivity.ISO_200, CameraExposure.IsoSensitivity.ISO_320)),
                exposureSettingSupportsMaxIsos(EnumSet.of(CameraExposure.IsoSensitivity.ISO_160,
                        CameraExposure.IsoSensitivity.ISO_320)),
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_100),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_160),
                exposureSettingSupportsAutoExposureMeteringMode(EnumSet.allOf(
                        CameraExposure.AutoExposureMeteringMode.class)),
                settingIsUpToDate()));

        // change shutter speed and ISO sensitivity offline
        mCamera.exposure()
               .setManualMode(CameraExposure.ShutterSpeed.ONE_OVER_10, CameraExposure.IsoSensitivity.ISO_200);

        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_10),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_200),
                settingIsUpToDate()));

        // change to automatic mode offline
        mCamera.exposure().setAutoMode(CameraExposure.IsoSensitivity.ISO_320);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_320),
                settingIsUpToDate()));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1,
                        caps().exposureModes(ArsdkFeatureCamera.ExposureMode.values())
                                .autoExposureMeteringModes(ArsdkFeatureCamera.AutoExposureMeteringMode.values())
                                .encode(),
                        ArsdkEncoder.encodeCameraExposureSettings(0, ArsdkFeatureCamera.ExposureMode.MANUAL,
                                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1, supportedShutterSpeed,
                                ArsdkFeatureCamera.IsoSensitivity.ISO_100, supportedIsos,
                                ArsdkFeatureCamera.IsoSensitivity.ISO_160, supportedMaxIsos,
                                ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD))
                .expect(new Expectation.Command(1, ExpectedCmd.cameraSetExposureSettings(0,
                        ArsdkFeatureCamera.ExposureMode.AUTOMATIC, ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10,
                        ArsdkFeatureCamera.IsoSensitivity.ISO_200, ArsdkFeatureCamera.IsoSensitivity.ISO_320,
                        ArsdkFeatureCamera.AutoExposureMeteringMode.CENTER_TOP))));

        // expect no change
        assertThat(mChangeCnt, is(3)); // active state change
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                exposureSettingManualShutterSpeedIs(CameraExposure.ShutterSpeed.ONE_OVER_10),
                exposureSettingManualIsoIs(CameraExposure.IsoSensitivity.ISO_200),
                exposureSettingMaxIsoIs(CameraExposure.IsoSensitivity.ISO_320),
                exposureSettingAutoExposureMeteringModeIs(CameraExposure.AutoExposureMeteringMode.CENTER_TOP),
                settingIsUpToDate()));
    }

    @Test
    public void testExposureLock() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().encode()));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.exposureLock(), nullValue());

        // mock lock command reception
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10, ArsdkFeatureCamera.IsoSensitivity.ISO_1200,
                ArsdkFeatureCamera.State.INACTIVE, -1, -1, -1, -1));
        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.exposureLock(), exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.NONE));

        // change mode from api
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraLockExposureOnRoi(0, 0.5f, 0.6f)));
        CameraExposureLock cameraExposureLock = mCamera.exposureLock();
        assert cameraExposureLock != null;
        cameraExposureLock.lockOnRegion(0.5, 0.6);

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.exposureLock(),
                exposureLockModeIsUpdatingTo(CameraExposureLock.Mode.REGION, 0.5, 0.6, 0, 0));

        // since event is non-ack, we can receive this event before requested value has been applied,
        // check that we are protecting changes for this case
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10, ArsdkFeatureCamera.IsoSensitivity.ISO_1200,
                ArsdkFeatureCamera.State.INACTIVE, -1, -1, -1, -1));

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.exposureLock(),
                exposureLockModeIsUpdatingTo(CameraExposureLock.Mode.REGION, 0.5, 0.6, 0, 0));

        // mock requested value applied
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10, ArsdkFeatureCamera.IsoSensitivity.ISO_1200,
                ArsdkFeatureCamera.State.ACTIVE, 0.5f, 0.6f, 0.4f, 0.2f));

        assertThat(mChangeCnt, is(4));
        assertThat(mCamera.exposureLock(),
                exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.REGION, 0.5f, 0.6f, 0.4f, 0.2f));

        // mock unrequested change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10, ArsdkFeatureCamera.IsoSensitivity.ISO_1200,
                ArsdkFeatureCamera.State.INACTIVE, 0.5f, 0.6f, 0.4f, 0.2f));

        assertThat(mChangeCnt, is(5));
        assertThat(mCamera.exposureLock(), exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.NONE));

        // mock unrequested change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10, ArsdkFeatureCamera.IsoSensitivity.ISO_1200,
                ArsdkFeatureCamera.State.ACTIVE, -1, -1, -1, -1));

        assertThat(mChangeCnt, is(6));
        assertThat(mCamera.exposureLock(), exposureLockModeIsUpdatedAt(CameraExposureLock.Mode.CURRENT_VALUES));

        // Exposure lock should be null after a disconnection
        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(7));
        assertThat(mCamera.exposureLock(), nullValue());
    }

    @Test
    public void testExposureCompensationSettings() {
        long supportedShutterSpeed = ArsdkFeatureCamera.ShutterSpeed.toBitField(
                ArsdkFeatureCamera.ShutterSpeed.values());
        long supportedIsos = ArsdkFeatureCamera.IsoSensitivity.toBitField(
                ArsdkFeatureCamera.IsoSensitivity.values());
        long supportedMaxIsos = ArsdkFeatureCamera.IsoSensitivity.toBitField(
                ArsdkFeatureCamera.IsoSensitivity.values());
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().evs(ArsdkFeatureCamera.EvCompensation.EV_MINUS_1_00, ArsdkFeatureCamera.EvCompensation.EV_0_00,
                        ArsdkFeatureCamera.EvCompensation.EV_1_00)
                        .exposureModes(ArsdkFeatureCamera.ExposureMode.values())
                        .autoExposureMeteringModes(ArsdkFeatureCamera.AutoExposureMeteringMode.values())
                        .encode(),
                ArsdkEncoder.encodeCameraExposureSettings(0, ArsdkFeatureCamera.ExposureMode.AUTOMATIC,
                        ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1, supportedShutterSpeed,
                        ArsdkFeatureCamera.IsoSensitivity.ISO_200, supportedIsos,
                        ArsdkFeatureCamera.IsoSensitivity.ISO_160, supportedMaxIsos,
                        ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD),
                ArsdkEncoder.encodeCameraEvCompensation(0, ArsdkFeatureCamera.EvCompensation.EV_0_00)));

        assertThat(mChangeCnt, is(1));

        // check capabilities
        assertThat(mCamera.exposureCompensation(), allOf(
                enumSettingSupports(EnumSet.of(CameraEvCompensation.EV_MINUS_1, CameraEvCompensation.EV_0,
                        CameraEvCompensation.EV_1)),
                enumSettingIsUpToDateAt(CameraEvCompensation.EV_0)));

        // change value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetEvCompensation(0,
                ArsdkFeatureCamera.EvCompensation.EV_1_00)));
        mCamera.exposureCompensation().setValue(CameraEvCompensation.EV_1);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.exposureCompensation(), enumSettingIsUpdatingTo(CameraEvCompensation.EV_1));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraEvCompensation(0,
                ArsdkFeatureCamera.EvCompensation.EV_1_00));

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.exposureCompensation(), enumSettingIsUpToDateAt(CameraEvCompensation.EV_1));
        mMockArsdkCore.assertNoExpectation();

        // change exposure mode to manual
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposureSettings(0,
                ArsdkFeatureCamera.ExposureMode.MANUAL,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1, supportedShutterSpeed,
                ArsdkFeatureCamera.IsoSensitivity.ISO_200, supportedIsos,
                ArsdkFeatureCamera.IsoSensitivity.ISO_160, supportedMaxIsos,
                ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD));

        // exposure compensation setting is not available, in manual exposure mode
        assertThat(mChangeCnt, is(4));
        assertThat(mCamera.exposureCompensation(), enumSettingSupports(EnumSet.noneOf(CameraEvCompensation.class)));

        // change exposure mode to automatic
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposureSettings(0,
                ArsdkFeatureCamera.ExposureMode.AUTOMATIC,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1, supportedShutterSpeed,
                ArsdkFeatureCamera.IsoSensitivity.ISO_200, supportedIsos,
                ArsdkFeatureCamera.IsoSensitivity.ISO_160, supportedMaxIsos,
                ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD));

        // exposure compensation setting is available, in automatic exposure mode and when exposure lock is inactive
        assertThat(mChangeCnt, is(5));
        assertThat(mCamera.exposureCompensation(), allOf(
                enumSettingSupports(EnumSet.of(CameraEvCompensation.EV_MINUS_1, CameraEvCompensation.EV_0,
                        CameraEvCompensation.EV_1)),
                enumSettingIsUpToDateAt(CameraEvCompensation.EV_1)));

        // exposure lock activated
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10, ArsdkFeatureCamera.IsoSensitivity.ISO_1200,
                ArsdkFeatureCamera.State.ACTIVE, -1, -1, -1, -1));

        // exposure compensation setting is not available, when exposure lock is active
        assertThat(mChangeCnt, is(6));
        assertThat(mCamera.exposureCompensation(), enumSettingSupports(EnumSet.noneOf(CameraEvCompensation.class)));

        // exposure lock deactivated
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10, ArsdkFeatureCamera.IsoSensitivity.ISO_1200,
                ArsdkFeatureCamera.State.INACTIVE, -1, -1, -1, -1));

        // exposure compensation setting is available, in automatic exposure mode and when exposure lock is inactive
        assertThat(mChangeCnt, is(7));
        assertThat(mCamera.exposureCompensation(), allOf(
                enumSettingSupports(EnumSet.of(CameraEvCompensation.EV_MINUS_1, CameraEvCompensation.EV_0,
                        CameraEvCompensation.EV_1)),
                enumSettingIsUpToDateAt(CameraEvCompensation.EV_1)));

        // change exposure mode to manual before disconnection
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposureSettings(0,
                ArsdkFeatureCamera.ExposureMode.MANUAL,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1, supportedShutterSpeed,
                ArsdkFeatureCamera.IsoSensitivity.ISO_200, supportedIsos,
                ArsdkFeatureCamera.IsoSensitivity.ISO_160, supportedMaxIsos,
                ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD));

        assertThat(mChangeCnt, is(8));

        // change exposure mode to manual before disconnection
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetExposureSettings(0,
                ArsdkFeatureCamera.ExposureMode.MANUAL, ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1,
                ArsdkFeatureCamera.IsoSensitivity.ISO_100, ArsdkFeatureCamera.IsoSensitivity.ISO_160,
                ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD)));
        mCamera.exposure().setManualMode(CameraExposure.ShutterSpeed.ONE, CameraExposure.IsoSensitivity.ISO_100);

        // exposure compensation setting is not available, in manual exposure mode
        assertThat(mCamera.exposureCompensation(), enumSettingSupports(EnumSet.noneOf(CameraEvCompensation.class)));

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        assertThat(mChangeCnt, is(0));

        // check stored capabilities
        assertThat(mCamera.exposureCompensation(), enumSettingSupports(EnumSet.noneOf(CameraEvCompensation.class)));

        // change exposure mode to automatic offline
        mCamera.exposure().setMode(CameraExposure.Mode.AUTOMATIC);

        // check stored capabilities
        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.exposure(), exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC));
        assertThat(mCamera.exposureCompensation(), allOf(
                enumSettingSupports(EnumSet.of(CameraEvCompensation.EV_MINUS_1, CameraEvCompensation.EV_0,
                        CameraEvCompensation.EV_1)),
                enumSettingIsUpToDateAt(CameraEvCompensation.EV_1))); // previously valid EV should be restored.

        // change exposure compensation offline
        mCamera.exposureCompensation().setValue(CameraEvCompensation.EV_0);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.exposureCompensation(), enumSettingIsUpToDateAt(CameraEvCompensation.EV_0));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1,
                        caps().evs(ArsdkFeatureCamera.EvCompensation.EV_MINUS_1_00,
                                ArsdkFeatureCamera.EvCompensation.EV_0_00,
                                ArsdkFeatureCamera.EvCompensation.EV_1_00)
                                .exposureModes(ArsdkFeatureCamera.ExposureMode.values())
                                .autoExposureMeteringModes(ArsdkFeatureCamera.AutoExposureMeteringMode.values())
                                .encode(),
                        ArsdkEncoder.encodeCameraEvCompensation(0, ArsdkFeatureCamera.EvCompensation.EV_1_00))
                .expect(new Expectation.Command(1, ExpectedCmd.cameraSetEvCompensation(0,
                        ArsdkFeatureCamera.EvCompensation.EV_0_00))));

        // expect no change
        assertThat(mChangeCnt, is(3)); // active state change
        assertThat(mCamera.exposureCompensation(), enumSettingIsUpToDateAt(CameraEvCompensation.EV_0));
    }

    @Test
    public void testStyle() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().styles(ArsdkFeatureCamera.Style.STANDARD, ArsdkFeatureCamera.Style.PLOG).encode(),
                ArsdkEncoder.encodeCameraStyle(0, ArsdkFeatureCamera.Style.STANDARD, 1, -2, 2, 2, -4, 4, 3, -6, 6)));

        assertThat(mChangeCnt, is(1));

        // check capabilities
        assertThat(mCamera.style(),
                styleSettingSupportsStyles(EnumSet.of(CameraStyle.Style.STANDARD, CameraStyle.Style.PLOG)));

        // check initial values
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(1, -2, 2),
                styleSettingContrastIs(2, -4, 4),
                styleSettingSharpnessIs(3, -6, 6),
                settingIsUpToDate()));

        // change style
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetStyle(0, ArsdkFeatureCamera.Style.PLOG)));
        mCamera.style().setStyle(CameraStyle.Style.PLOG);
        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.PLOG),
                styleSettingSaturationIs(1, -2, 2),
                styleSettingContrastIs(2, -4, 4),
                styleSettingSharpnessIs(3, -6, 6),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraStyle(0, ArsdkFeatureCamera.Style.PLOG, 0, -1, 1, 0, -2, 2, 0, -3, 3));
        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.PLOG),
                styleSettingSaturationIs(0, -1, 1),
                styleSettingContrastIs(0, -2, 2),
                styleSettingSharpnessIs(0, -3, 3),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        assertThat(mChangeCnt, is(0));

        // check stored capabilities
        assertThat(mCamera.style(), allOf(
                styleSettingSupportsStyles(EnumSet.of(CameraStyle.Style.STANDARD, CameraStyle.Style.PLOG)),
                styleSettingStyleIs(CameraStyle.Style.PLOG),
                styleSettingSaturationIs(0, -1, 1),
                styleSettingContrastIs(0, -2, 2),
                styleSettingSharpnessIs(0, -3, 3),
                settingIsUpToDate()));

        // change style offline
        mCamera.style().setStyle(CameraStyle.Style.STANDARD);

        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(0, -1, 1),
                styleSettingContrastIs(0, -2, 2),
                styleSettingSharpnessIs(0, -3, 3),
                settingIsUpToDate()));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1,
                        caps().styles(ArsdkFeatureCamera.Style.STANDARD, ArsdkFeatureCamera.Style.PLOG).encode(),
                        ArsdkEncoder.encodeCameraStyle(0, ArsdkFeatureCamera.Style.PLOG, 0, -1, 1, 0, -2, 2, 0, -3, 3))
                .expect(new Expectation.Command(1, ExpectedCmd.cameraSetStyle(0, ArsdkFeatureCamera.Style.STANDARD))));

        // expect no change
        assertThat(mChangeCnt, is(2)); // active state change
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(0, -1, 1),
                styleSettingContrastIs(0, -2, 2),
                styleSettingSharpnessIs(0, -3, 3),
                settingIsUpToDate()));
    }

    @Test
    public void testStyleParameter() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().styles(ArsdkFeatureCamera.Style.STANDARD, ArsdkFeatureCamera.Style.PLOG).encode(),
                ArsdkEncoder.encodeCameraStyle(0, ArsdkFeatureCamera.Style.STANDARD, 1, -2, 2, 2, -4, 4, 0, 0, 0)));

        assertThat(mChangeCnt, is(1));

        // check initial values
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(1, -2, 2),
                styleSettingContrastIs(2, -4, 4),
                styleSettingSharpnessIs(0, 0, 0),
                settingIsUpToDate()));

        // change contrast
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetStyleParams(0, 1, -2, 0)));
        mCamera.style().contrast().setValue(-2);
        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(1, -2, 2),
                styleSettingContrastIs(-2, -4, 4),
                styleSettingSharpnessIs(0, 0, 0),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraStyle(0, ArsdkFeatureCamera.Style.STANDARD, 1, -2, 2, -2, -4, 4, 0, 0, 0));
        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(1, -2, 2),
                styleSettingContrastIs(-2, -4, 4),
                styleSettingSharpnessIs(0, 0, 0),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        assertThat(mChangeCnt, is(0));

        // check stored capabilities
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.STANDARD),
                styleSettingSaturationIs(1, -2, 2),
                styleSettingContrastIs(-2, -4, 4),
                styleSettingSharpnessIs(0, 0, 0),
                settingIsUpToDate()));

        // change saturation offline
        mCamera.style().saturation().setValue(2);

        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.style(), allOf(
                styleSettingSaturationIs(2, -2, 2),
                settingIsUpToDate()));

        // change contrast offline
        mCamera.style().contrast().setValue(4);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.style(), allOf(
                styleSettingContrastIs(4, -4, 4),
                settingIsUpToDate()));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1,
                        caps().styles(ArsdkFeatureCamera.Style.STANDARD, ArsdkFeatureCamera.Style.PLOG).encode(),
                        ArsdkEncoder.encodeCameraStyle(0, ArsdkFeatureCamera.Style.STANDARD,
                                1, -2, 2, -2, -4, 4, 0, 0, 0))
                .expect(new Expectation.Command(1, ExpectedCmd.cameraSetStyleParams(0, 2, 4, 0))));

        // expect no change
        assertThat(mChangeCnt, is(3)); // active state change
        assertThat(mCamera.style(), allOf(
                styleSettingSaturationIs(2, -2, 2),
                styleSettingContrastIs(4, -4, 4),
                styleSettingSharpnessIs(0, 0, 0),
                settingIsUpToDate()));
    }

    @Test
    public void testAlignment() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1, caps().encode()));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.alignment(), nullValue());

        // mock alignment event reception
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraAlignmentOffsets(
                0, -2.0f, 2.0f, 1.0f, -4.0f, 4.0f, 2.0f, -6.0f, 6.0f, 3.0f));
        assertThat(mChangeCnt, is(2));
        CameraAlignment.Setting alignment = mCamera.alignment();
        assertThat(alignment, notNullValue());
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, 1.0, 2.0),
                alignmentSettingPitchIs(-4.0, 2.0, 4.0),
                alignmentSettingRollIs(-6.0, 3.0, 6.0),
                settingIsUpToDate()));

        // change yaw
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetAlignmentOffsets(0, 1.5f, 2.0f, 3.0f)));
        alignment.setYaw(1.5);
        assertThat(mChangeCnt, is(3));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, 1.5, 2.0),
                alignmentSettingPitchIs(-4.0, 2.0, 4.0),
                alignmentSettingRollIs(-6.0, 3.0, 6.0),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraAlignmentOffsets(
                0, -2.0f, 2.0f, 1.5f, -4.0f, 4.0f, 2.0f, -6.0f, 6.0f, 3.0f));
        assertThat(mChangeCnt, is(4));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, 1.5, 2.0),
                alignmentSettingPitchIs(-4.0, 2.0, 4.0),
                alignmentSettingRollIs(-6.0, 3.0, 6.0),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // change pitch
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetAlignmentOffsets(0, 1.5f, -3.5f, 3.0f)));
        alignment.setPitch(-3.5);
        assertThat(mChangeCnt, is(5));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, 1.5, 2.0),
                alignmentSettingPitchIs(-4.0, -3.5, 4.0),
                alignmentSettingRollIs(-6.0, 3.0, 6.0),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraAlignmentOffsets(
                0, -2.0f, 2.0f, 1.5f, -4.0f, 4.0f, -3.5f, -6.0f, 6.0f, 3.0f));
        assertThat(mChangeCnt, is(6));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, 1.5, 2.0),
                alignmentSettingPitchIs(-4.0, -3.5, 4.0),
                alignmentSettingRollIs(-6.0, 3.0, 6.0),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // change roll
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetAlignmentOffsets(0, 1.5f, -3.5f, 5.0f)));
        alignment.setRoll(5.0);
        assertThat(mChangeCnt, is(7));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, 1.5, 2.0),
                alignmentSettingPitchIs(-4.0, -3.5, 4.0),
                alignmentSettingRollIs(-6.0, 5.0, 6.0),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraAlignmentOffsets(
                0, -2.0f, 2.0f, 1.5f, -4.0f, 4.0f, -3.5f, -6.0f, 6.0f, 5.0f));
        assertThat(mChangeCnt, is(8));
        assertThat(alignment, allOf(
                alignmentSettingYawIs(-2.0, 1.5, 2.0),
                alignmentSettingPitchIs(-4.0, -3.5, 4.0),
                alignmentSettingRollIs(-6.0, 5.0, 6.0),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // reset alignment
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraResetAlignmentOffsets(0)));
        alignment.reset();
        assertThat(mChangeCnt, is(8));

        // alignment should be null after a disconnection
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(9));
        assertThat(mCamera.alignment(), nullValue());
    }

    @Test
    public void testWhiteBalanceSettings() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().whiteBalanceModes(ArsdkFeatureCamera.WhiteBalanceMode.AUTOMATIC,
                        ArsdkFeatureCamera.WhiteBalanceMode.SUNNY, ArsdkFeatureCamera.WhiteBalanceMode.SNOW,
                        ArsdkFeatureCamera.WhiteBalanceMode.CUSTOM)
                      .temperatures(ArsdkFeatureCamera.WhiteBalanceTemperature.T_3000,
                              ArsdkFeatureCamera.WhiteBalanceTemperature.T_5000,
                              ArsdkFeatureCamera.WhiteBalanceTemperature.T_7000)
                      .encode(),
                ArsdkEncoder.encodeCameraWhiteBalance(0, ArsdkFeatureCamera.WhiteBalanceMode.AUTOMATIC,
                        ArsdkFeatureCamera.WhiteBalanceTemperature.T_3000, ArsdkFeatureCamera.State.INACTIVE)));

        assertThat(mChangeCnt, is(1));

        // check capabilities
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingSupportsModes(EnumSet.of(CameraWhiteBalance.Mode.AUTOMATIC,
                        CameraWhiteBalance.Mode.SUNNY, CameraWhiteBalance.Mode.SNOW, CameraWhiteBalance.Mode.CUSTOM)),
                whiteBalanceSettingSupportsTemperatures(EnumSet.of(CameraWhiteBalance.Temperature.K_3000,
                        CameraWhiteBalance.Temperature.K_5000, CameraWhiteBalance.Temperature.K_7000)),
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.AUTOMATIC),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_3000),
                settingIsUpToDate()));

        // change mode
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetWhiteBalance(0,
                ArsdkFeatureCamera.WhiteBalanceMode.SUNNY, ArsdkFeatureCamera.WhiteBalanceTemperature.T_3000)));
        mCamera.whiteBalance().setMode(CameraWhiteBalance.Mode.SUNNY);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.SUNNY),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraWhiteBalance(0,
                ArsdkFeatureCamera.WhiteBalanceMode.SUNNY, ArsdkFeatureCamera.WhiteBalanceTemperature.T_3000,
                ArsdkFeatureCamera.State.INACTIVE));

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.SUNNY),
                settingIsUpToDate()));

        // set custom temperature in non-custom mode, no command is sent
        mCamera.whiteBalance().setCustomTemperature(CameraWhiteBalance.Temperature.K_7000);
        mMockArsdkCore.assertNoExpectation();

        assertThat(mChangeCnt, is(4));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_7000),
                settingIsUpToDate()));

        // set custom mode, new temperature is also sent
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetWhiteBalance(0,
                ArsdkFeatureCamera.WhiteBalanceMode.CUSTOM, ArsdkFeatureCamera.WhiteBalanceTemperature.T_7000)));
        mCamera.whiteBalance().setMode(CameraWhiteBalance.Mode.CUSTOM);

        assertThat(mChangeCnt, is(5));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_7000),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraWhiteBalance(0,
                ArsdkFeatureCamera.WhiteBalanceMode.CUSTOM, ArsdkFeatureCamera.WhiteBalanceTemperature.T_7000,
                ArsdkFeatureCamera.State.INACTIVE));

        assertThat(mChangeCnt, is(6));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_7000),
                settingIsUpToDate()));

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        assertThat(mChangeCnt, is(0));

        // check stored capabilities
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingSupportsModes(EnumSet.of(CameraWhiteBalance.Mode.AUTOMATIC,
                        CameraWhiteBalance.Mode.SUNNY, CameraWhiteBalance.Mode.SNOW, CameraWhiteBalance.Mode.CUSTOM)),
                whiteBalanceSettingSupportsTemperatures(EnumSet.of(CameraWhiteBalance.Temperature.K_3000,
                        CameraWhiteBalance.Temperature.K_5000, CameraWhiteBalance.Temperature.K_7000)),
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_7000),
                settingIsUpToDate()));

        // change white balance mode offline
        mCamera.whiteBalance().setMode(CameraWhiteBalance.Mode.SNOW);

        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.SNOW),
                settingIsUpToDate()));

        // change custom temperature offline
        mCamera.whiteBalance().setCustomTemperature(CameraWhiteBalance.Temperature.K_5000);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_5000),
                settingIsUpToDate()));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1,
                        caps().whiteBalanceModes(ArsdkFeatureCamera.WhiteBalanceMode.AUTOMATIC,
                                ArsdkFeatureCamera.WhiteBalanceMode.SUNNY, ArsdkFeatureCamera.WhiteBalanceMode.SNOW,
                                ArsdkFeatureCamera.WhiteBalanceMode.CUSTOM)
                              .temperatures(ArsdkFeatureCamera.WhiteBalanceTemperature.T_3000,
                                      ArsdkFeatureCamera.WhiteBalanceTemperature.T_5000,
                                      ArsdkFeatureCamera.WhiteBalanceTemperature.T_7000)
                              .encode(),
                        ArsdkEncoder.encodeCameraWhiteBalance(0, ArsdkFeatureCamera.WhiteBalanceMode.CUSTOM,
                                ArsdkFeatureCamera.WhiteBalanceTemperature.T_7000, ArsdkFeatureCamera.State.INACTIVE))
                .expect(new Expectation.Command(1, ExpectedCmd.cameraSetWhiteBalance(0,
                        ArsdkFeatureCamera.WhiteBalanceMode.SNOW, ArsdkFeatureCamera.WhiteBalanceTemperature.T_5000))));
        mMockArsdkCore.assertNoExpectation();

        // expect no change
        assertThat(mChangeCnt, is(3)); // active state change
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.SNOW),
                whiteBalanceSettingTemperatureIs(CameraWhiteBalance.Temperature.K_5000),
                settingIsUpToDate()));
    }

    @Test
    public void testWhiteBalanceLock() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().whiteBalanceLock(ArsdkFeatureCamera.Supported.NOT_SUPPORTED).encode()));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.whiteBalanceLock(), nullValue());

        // mock white balance lock now supported
        mMockArsdkCore.commandReceived(1, caps().whiteBalanceLock(ArsdkFeatureCamera.Supported.SUPPORTED).encode());

        assertThat(mChangeCnt, is(2));
        CameraWhiteBalanceLock whiteBalanceLock = mCamera.whiteBalanceLock();
        assertThat(whiteBalanceLock, notNullValue());
        assertThat(whiteBalanceLock, whiteBalanceIsLockable(false));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(false));

        // receive white balance event with non-automatic mode, white balance should not be lockable
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraWhiteBalance(0,
                ArsdkFeatureCamera.WhiteBalanceMode.SUNNY, ArsdkFeatureCamera.WhiteBalanceTemperature.T_3000,
                ArsdkFeatureCamera.State.INACTIVE));

        assertThat(mChangeCnt, is(3));
        assertThat(whiteBalanceLock, whiteBalanceIsLockable(false));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(false));

        // receive white balance event with automatic mode
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraWhiteBalance(0,
                ArsdkFeatureCamera.WhiteBalanceMode.AUTOMATIC, ArsdkFeatureCamera.WhiteBalanceTemperature.T_3000,
                ArsdkFeatureCamera.State.INACTIVE));

        assertThat(mChangeCnt, is(4));
        assertThat(whiteBalanceLock, whiteBalanceIsLockable(true));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(false));

        // lock white balance from api
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.cameraSetWhiteBalanceLock(0, ArsdkFeatureCamera.State.ACTIVE)));
        whiteBalanceLock.lockCurrentValue(true);

        assertThat(mChangeCnt, is(5));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpdatingTo(true));

        // mock requested lock is applied
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraWhiteBalance(0,
                ArsdkFeatureCamera.WhiteBalanceMode.AUTOMATIC, ArsdkFeatureCamera.WhiteBalanceTemperature.T_3000,
                ArsdkFeatureCamera.State.ACTIVE));

        assertThat(mChangeCnt, is(6));
        assertThat(whiteBalanceLock, whiteBalanceLockIsUpToDateAt(true));

        // white balance lock should be null after a disconnection
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(7));
        assertThat(mCamera.whiteBalanceLock(), nullValue());
    }

    @Test
    public void testAutoHdr() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().encode(),
                ArsdkEncoder.encodeCameraHdrSetting(0, ArsdkFeatureCamera.State.INACTIVE)));

        assertThat(mChangeCnt, is(1));

        // check initial values
        assertThat(mCamera.autoHdr(), optionalSettingIsAvailable());
        assertThat(mCamera.autoHdr(), optionalBooleanSettingIsDisabled());

        // change value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetHdrSetting(0,
                ArsdkFeatureCamera.State.ACTIVE)));
        mCamera.autoHdr().toggle();
        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.autoHdr(), optionalBooleanSettingIsEnabling());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraHdrSetting(0, ArsdkFeatureCamera.State.ACTIVE));
        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.autoHdr(), optionalBooleanSettingIsEnabled());

        // disconnect
        disconnectDrone(mDrone, 1);

        // check HDR setting did not change
        assertThat(mChangeCnt, is(4)); // active state change
        assertThat(mCamera.autoHdr(), optionalBooleanSettingIsEnabled());

        // change value offline
        mCamera.autoHdr().toggle();
        assertThat(mChangeCnt, is(5));
        assertThat(mCamera.autoHdr(), optionalBooleanSettingIsDisabled());

        // restart engine
        resetEngine();

        // reconnect
        assertThat(mChangeCnt, is(0));
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCameraHdrSetting(0, ArsdkFeatureCamera.State.ACTIVE))
                .expect(new Expectation.Command(1,
                        ExpectedCmd.cameraSetHdrSetting(0, ArsdkFeatureCamera.State.INACTIVE))));
        assertThat(mCamera.autoHdr(), optionalBooleanSettingIsDisabled());
    }

    @Test
    public void testRecordingSettings() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().hyperlapses(
                        ArsdkFeatureCamera.HyperlapseValue.RATIO_15,
                        ArsdkFeatureCamera.HyperlapseValue.RATIO_30,
                        ArsdkFeatureCamera.HyperlapseValue.RATIO_60)
                      .encode(),
                ArsdkEncoder.encodeCameraRecordingCapabilities(0,
                        ArsdkFeatureCamera.RecordingMode.toBitField(
                                ArsdkFeatureCamera.RecordingMode.STANDARD,
                                ArsdkFeatureCamera.RecordingMode.HYPERLAPSE),
                        ArsdkFeatureCamera.Resolution.toBitField(
                                ArsdkFeatureCamera.Resolution.RES_DCI_4K,
                                ArsdkFeatureCamera.Resolution.RES_UHD_4K,
                                ArsdkFeatureCamera.Resolution.RES_UHD_8K,
                                ArsdkFeatureCamera.Resolution.RES_1080P),
                        ArsdkFeatureCamera.Framerate.toBitField(
                                ArsdkFeatureCamera.Framerate.FPS_24,
                                ArsdkFeatureCamera.Framerate.FPS_25,
                                ArsdkFeatureCamera.Framerate.FPS_30),
                        ArsdkFeatureCamera.Supported.NOT_SUPPORTED,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)),
                ArsdkEncoder.encodeCameraRecordingCapabilities(1,
                        ArsdkFeatureCamera.RecordingMode.toBitField(ArsdkFeatureCamera.RecordingMode.HIGH_FRAMERATE),
                        ArsdkFeatureCamera.Resolution.toBitField(
                                ArsdkFeatureCamera.Resolution.RES_1080P,
                                ArsdkFeatureCamera.Resolution.RES_720P),
                        ArsdkFeatureCamera.Framerate.toBitField(
                                ArsdkFeatureCamera.Framerate.FPS_48,
                                ArsdkFeatureCamera.Framerate.FPS_50,
                                ArsdkFeatureCamera.Framerate.FPS_60),
                        ArsdkFeatureCamera.Supported.NOT_SUPPORTED,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)),
                ArsdkEncoder.encodeCameraRecordingMode(0,
                        ArsdkFeatureCamera.RecordingMode.STANDARD,
                        ArsdkFeatureCamera.Resolution.RES_1080P,
                        ArsdkFeatureCamera.Framerate.FPS_24,
                        ArsdkFeatureCamera.HyperlapseValue.RATIO_30, 0)));

        assertThat(mChangeCnt, is(1));

        // check capabilities
        assertThat(mCamera.recording(), allOf(
                recordingSettingSupportsModes(EnumSet.of(
                        CameraRecording.Mode.STANDARD,
                        CameraRecording.Mode.HYPERLAPSE,
                        CameraRecording.Mode.HIGH_FRAMERATE)),
                recordingSettingSupportsResolutions(
                        EnumSet.of(
                                CameraRecording.Mode.STANDARD,
                                CameraRecording.Mode.HYPERLAPSE),
                        EnumSet.of(
                                CameraRecording.Resolution.RES_DCI_4K,
                                CameraRecording.Resolution.RES_UHD_4K,
                                CameraRecording.Resolution.RES_UHD_8K,
                                CameraRecording.Resolution.RES_1080P)),
                recordingSettingSupportsResolutions(
                        CameraRecording.Mode.HIGH_FRAMERATE,
                        EnumSet.of(
                                CameraRecording.Resolution.RES_1080P,
                                CameraRecording.Resolution.RES_720P)),
                recordingSettingSupportsFramerates(
                        EnumSet.of(
                                CameraRecording.Mode.STANDARD,
                                CameraRecording.Mode.HYPERLAPSE),
                        EnumSet.of(
                                CameraRecording.Resolution.RES_DCI_4K,
                                CameraRecording.Resolution.RES_UHD_4K,
                                CameraRecording.Resolution.RES_UHD_8K,
                                CameraRecording.Resolution.RES_1080P),
                        EnumSet.of(
                                CameraRecording.Framerate.FPS_24,
                                CameraRecording.Framerate.FPS_25,
                                CameraRecording.Framerate.FPS_30)),
                recordingSettingSupportsFramerates(
                        CameraRecording.Mode.HIGH_FRAMERATE,
                        EnumSet.of(
                                CameraRecording.Resolution.RES_1080P,
                                CameraRecording.Resolution.RES_720P),
                        EnumSet.of(
                                CameraRecording.Framerate.FPS_48,
                                CameraRecording.Framerate.FPS_50,
                                CameraRecording.Framerate.FPS_60)),
                recordingSettingSupportsHyperlapseValues(EnumSet.of(
                        CameraRecording.HyperlapseValue.RATIO_15,
                        CameraRecording.HyperlapseValue.RATIO_30,
                        CameraRecording.HyperlapseValue.RATIO_60)),
                recordingSettingModeIs(CameraRecording.Mode.STANDARD),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_1080P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_15),
                recordingSettingBitrateIs(0),
                settingIsUpToDate()));

        // change mode to hyperlapse
        // Since no previous resolution & framerate hyperlapse were selected for this mode beforehand,
        // expect resolution and framerate to highest available values. Hyperlapse should be stored value (first
        // supported value)
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HYPERLAPSE, ArsdkFeatureCamera.Resolution.RES_DCI_4K,
                ArsdkFeatureCamera.Framerate.FPS_30, ArsdkFeatureCamera.HyperlapseValue.RATIO_15)));
        mCamera.recording().setMode(CameraRecording.Mode.HYPERLAPSE);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_DCI_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_30),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_15),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HYPERLAPSE, ArsdkFeatureCamera.Resolution.RES_DCI_4K,
                ArsdkFeatureCamera.Framerate.FPS_30, ArsdkFeatureCamera.HyperlapseValue.RATIO_15, 0));

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_DCI_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_30),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_15),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // change resolution in hyperlapse mode
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HYPERLAPSE, ArsdkFeatureCamera.Resolution.RES_UHD_4K,
                ArsdkFeatureCamera.Framerate.FPS_30, ArsdkFeatureCamera.HyperlapseValue.RATIO_15)));
        mCamera.recording().setResolution(CameraRecording.Resolution.RES_UHD_4K);

        assertThat(mChangeCnt, is(4));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_30),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_15),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HYPERLAPSE, ArsdkFeatureCamera.Resolution.RES_UHD_4K,
                ArsdkFeatureCamera.Framerate.FPS_30, ArsdkFeatureCamera.HyperlapseValue.RATIO_15, 0));

        assertThat(mChangeCnt, is(5));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_30),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_15),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // change framerate in hyperlapse mode
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HYPERLAPSE, ArsdkFeatureCamera.Resolution.RES_UHD_4K,
                ArsdkFeatureCamera.Framerate.FPS_24, ArsdkFeatureCamera.HyperlapseValue.RATIO_15)));
        mCamera.recording().setFramerate(CameraRecording.Framerate.FPS_24);

        assertThat(mChangeCnt, is(6));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_15),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HYPERLAPSE, ArsdkFeatureCamera.Resolution.RES_UHD_4K,
                ArsdkFeatureCamera.Framerate.FPS_24, ArsdkFeatureCamera.HyperlapseValue.RATIO_15, 0));

        assertThat(mChangeCnt, is(7));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_15),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // change hyperlapse value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HYPERLAPSE, ArsdkFeatureCamera.Resolution.RES_UHD_4K,
                ArsdkFeatureCamera.Framerate.FPS_24, ArsdkFeatureCamera.HyperlapseValue.RATIO_60)));
        mCamera.recording().setHyperlapseValue(CameraRecording.HyperlapseValue.RATIO_60);

        assertThat(mChangeCnt, is(8));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HYPERLAPSE, ArsdkFeatureCamera.Resolution.RES_UHD_4K,
                ArsdkFeatureCamera.Framerate.FPS_24, ArsdkFeatureCamera.HyperlapseValue.RATIO_60, 0));

        assertThat(mChangeCnt, is(9));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // change mode to high framerate
        // Since no previous resolution & framerate hyperlapse were selected for this mode beforehand,
        // expect resolution and framerate to highest available values. Hyperlapse should be current setting value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HIGH_FRAMERATE, ArsdkFeatureCamera.Resolution.RES_1080P,
                ArsdkFeatureCamera.Framerate.FPS_60, ArsdkFeatureCamera.HyperlapseValue.RATIO_60)));
        mCamera.recording().setMode(CameraRecording.Mode.HIGH_FRAMERATE);

        assertThat(mChangeCnt, is(10));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_1080P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_60),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HIGH_FRAMERATE, ArsdkFeatureCamera.Resolution.RES_1080P,
                ArsdkFeatureCamera.Framerate.FPS_60, ArsdkFeatureCamera.HyperlapseValue.RATIO_60, 0));

        assertThat(mChangeCnt, is(11));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_1080P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_60),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // change back to hyperlapse mode, expect previous resolution and fps for this mode
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HYPERLAPSE, ArsdkFeatureCamera.Resolution.RES_UHD_4K,
                ArsdkFeatureCamera.Framerate.FPS_24, ArsdkFeatureCamera.HyperlapseValue.RATIO_60)));
        mCamera.recording().setMode(CameraRecording.Mode.HYPERLAPSE);

        assertThat(mChangeCnt, is(12));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HYPERLAPSE, ArsdkFeatureCamera.Resolution.RES_UHD_4K,
                ArsdkFeatureCamera.Framerate.FPS_24, ArsdkFeatureCamera.HyperlapseValue.RATIO_60, 0));

        assertThat(mChangeCnt, is(13));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // test recording bitrate
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HYPERLAPSE, ArsdkFeatureCamera.Resolution.RES_UHD_4K,
                ArsdkFeatureCamera.Framerate.FPS_24, ArsdkFeatureCamera.HyperlapseValue.RATIO_60, 10000));
        assertThat(mChangeCnt, is(14));
        assertThat(mCamera.recording(), recordingSettingBitrateIs(10000));

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                recordingSettingBitrateIs(0),
                settingIsUpToDate()));
        resetEngine();

        assertThat(mChangeCnt, is(0));

        // check stored capabilities
        assertThat(mCamera.recording(), allOf(
                recordingSettingSupportsModes(EnumSet.of(
                        CameraRecording.Mode.STANDARD,
                        CameraRecording.Mode.HYPERLAPSE,
                        CameraRecording.Mode.HIGH_FRAMERATE)),
                recordingSettingSupportsResolutions(
                        EnumSet.of(
                                CameraRecording.Mode.STANDARD,
                                CameraRecording.Mode.HYPERLAPSE),
                        EnumSet.of(
                                CameraRecording.Resolution.RES_DCI_4K,
                                CameraRecording.Resolution.RES_UHD_4K,
                                CameraRecording.Resolution.RES_1080P,
                                CameraRecording.Resolution.RES_UHD_8K)),
                recordingSettingSupportsResolutions(
                        CameraRecording.Mode.HIGH_FRAMERATE,
                        EnumSet.of(
                                CameraRecording.Resolution.RES_1080P,
                                CameraRecording.Resolution.RES_720P)),
                recordingSettingSupportsFramerates(
                        EnumSet.of(
                                CameraRecording.Mode.STANDARD,
                                CameraRecording.Mode.HYPERLAPSE),
                        EnumSet.of(
                                CameraRecording.Resolution.RES_DCI_4K,
                                CameraRecording.Resolution.RES_UHD_4K,
                                CameraRecording.Resolution.RES_1080P),
                        EnumSet.of(
                                CameraRecording.Framerate.FPS_24,
                                CameraRecording.Framerate.FPS_25,
                                CameraRecording.Framerate.FPS_30)),
                recordingSettingSupportsFramerates(
                        CameraRecording.Mode.HIGH_FRAMERATE,
                        EnumSet.of(
                                CameraRecording.Resolution.RES_1080P,
                                CameraRecording.Resolution.RES_720P),
                        EnumSet.of(
                                CameraRecording.Framerate.FPS_48,
                                CameraRecording.Framerate.FPS_50,
                                CameraRecording.Framerate.FPS_60)),
                recordingSettingSupportsHyperlapseValues(EnumSet.of(
                        CameraRecording.HyperlapseValue.RATIO_15,
                        CameraRecording.HyperlapseValue.RATIO_30,
                        CameraRecording.HyperlapseValue.RATIO_60)),
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_UHD_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                settingIsUpToDate()));

        // change hyperlapse resolution offline
        mCamera.recording().setResolution(CameraRecording.Resolution.RES_DCI_4K);

        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_DCI_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_24),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                settingIsUpToDate()));

        // change hyperlapse framerate offline
        mCamera.recording().setFramerate(CameraRecording.Framerate.FPS_25);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HYPERLAPSE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_DCI_4K),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_25),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                settingIsUpToDate()));

        // change to high framerate mode, expect previously stored resolution and framerate
        mCamera.recording().setMode(CameraRecording.Mode.HIGH_FRAMERATE);

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_1080P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_60),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                settingIsUpToDate()));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCameraRecordingMode(0,
                        ArsdkFeatureCamera.RecordingMode.STANDARD, ArsdkFeatureCamera.Resolution.RES_1080P,
                        ArsdkFeatureCamera.Framerate.FPS_60, ArsdkFeatureCamera.HyperlapseValue.RATIO_60, 0))
                .expect(new Expectation.Command(1, ExpectedCmd.cameraSetRecordingMode(0,
                        ArsdkFeatureCamera.RecordingMode.HIGH_FRAMERATE, ArsdkFeatureCamera.Resolution.RES_1080P,
                        ArsdkFeatureCamera.Framerate.FPS_60, ArsdkFeatureCamera.HyperlapseValue.RATIO_60))));

        // ensure settings are restored
        assertThat(mChangeCnt, is(4)); // active state change
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                recordingSettingResolutionIs(CameraRecording.Resolution.RES_1080P),
                recordingSettingFramerateIs(CameraRecording.Framerate.FPS_60),
                recordingSettingHyperlapseValueIs(CameraRecording.HyperlapseValue.RATIO_60),
                settingIsUpToDate()));
    }

    @Test
    public void testPhotoSettings() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().bursts(
                        ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                        ArsdkFeatureCamera.BurstValue.BURST_14_OVER_4S)
                      .bracketings(
                              ArsdkFeatureCamera.BracketingPreset.PRESET_3EV,
                              ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV)
                      .minTimelapse(2f)
                      .minGpslapse(1f)
                      .encode(),
                ArsdkEncoder.encodeCameraPhotoCapabilities(0,
                        ArsdkFeatureCamera.PhotoMode.toBitField(
                                ArsdkFeatureCamera.PhotoMode.SINGLE,
                                ArsdkFeatureCamera.PhotoMode.BRACKETING),
                        ArsdkFeatureCamera.PhotoFormat.toBitField(ArsdkFeatureCamera.PhotoFormat.FULL_FRAME),
                        ArsdkFeatureCamera.PhotoFileFormat.toBitField(ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG,
                                ArsdkFeatureCamera.PhotoFileFormat.JPEG),
                        ArsdkFeatureCamera.Supported.NOT_SUPPORTED,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)),
                ArsdkEncoder.encodeCameraPhotoCapabilities(1,
                        ArsdkFeatureCamera.PhotoMode.toBitField(
                                ArsdkFeatureCamera.PhotoMode.SINGLE,
                                ArsdkFeatureCamera.PhotoMode.BRACKETING),
                        ArsdkFeatureCamera.PhotoFormat.toBitField(ArsdkFeatureCamera.PhotoFormat.RECTILINEAR),
                        ArsdkFeatureCamera.PhotoFileFormat.toBitField(ArsdkFeatureCamera.PhotoFileFormat.JPEG),
                        ArsdkFeatureCamera.Supported.NOT_SUPPORTED,
                        ArsdkFeatureGeneric.ListFlags.toBitField()),
                ArsdkEncoder.encodeCameraPhotoCapabilities(2,
                        ArsdkFeatureCamera.PhotoMode.toBitField(
                                ArsdkFeatureCamera.PhotoMode.BURST,
                                ArsdkFeatureCamera.PhotoMode.TIME_LAPSE,
                                ArsdkFeatureCamera.PhotoMode.GPS_LAPSE),
                        ArsdkFeatureCamera.PhotoFormat.toBitField(
                                ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                                ArsdkFeatureCamera.PhotoFormat.RECTILINEAR),
                        ArsdkFeatureCamera.PhotoFileFormat.toBitField(ArsdkFeatureCamera.PhotoFileFormat.JPEG),
                        ArsdkFeatureCamera.Supported.NOT_SUPPORTED,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)),
                ArsdkEncoder.encodeCameraPhotoMode(0,
                        ArsdkFeatureCamera.PhotoMode.BRACKETING,
                        ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                        ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG,
                        ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                        ArsdkFeatureCamera.BracketingPreset.PRESET_3EV,
                        5f)));

        assertThat(mChangeCnt, is(1));

        // check capabilities
        assertThat(mCamera.photo(), allOf(
                photoSettingSupportsModes(EnumSet.of(
                        CameraPhoto.Mode.SINGLE,
                        CameraPhoto.Mode.BRACKETING,
                        CameraPhoto.Mode.BURST,
                        CameraPhoto.Mode.TIME_LAPSE,
                        CameraPhoto.Mode.GPS_LAPSE)),
                photoSettingSupportsFormats(
                        EnumSet.of(
                                CameraPhoto.Mode.SINGLE,
                                CameraPhoto.Mode.BRACKETING,
                                CameraPhoto.Mode.BURST,
                                CameraPhoto.Mode.TIME_LAPSE,
                                CameraPhoto.Mode.GPS_LAPSE),
                        EnumSet.of(
                                CameraPhoto.Format.RECTILINEAR,
                                CameraPhoto.Format.FULL_FRAME)),
                photoSettingSupportsFileFormats(
                        EnumSet.of(
                                CameraPhoto.Mode.SINGLE,
                                CameraPhoto.Mode.BRACKETING,
                                CameraPhoto.Mode.BURST,
                                CameraPhoto.Mode.TIME_LAPSE,
                                CameraPhoto.Mode.GPS_LAPSE),
                        CameraPhoto.Format.RECTILINEAR,
                        EnumSet.of(CameraPhoto.FileFormat.JPEG)),
                photoSettingSupportsFileFormats(
                        EnumSet.of(
                                CameraPhoto.Mode.SINGLE,
                                CameraPhoto.Mode.BRACKETING),
                        CameraPhoto.Format.FULL_FRAME,
                        EnumSet.of(
                                CameraPhoto.FileFormat.JPEG,
                                CameraPhoto.FileFormat.DNG_AND_JPEG)),
                photoSettingSupportsFileFormats(
                        CameraPhoto.Mode.BURST,
                        CameraPhoto.Format.FULL_FRAME,
                        EnumSet.of(CameraPhoto.FileFormat.JPEG)),
                photoSettingSupportsBurstValues(EnumSet.of(
                        CameraPhoto.BurstValue.BURST_14_OVER_4S,
                        CameraPhoto.BurstValue.BURST_4_OVER_1S)),
                photoSettingSupportsBracketingValues(EnumSet.of(
                        CameraPhoto.BracketingValue.EV_3,
                        CameraPhoto.BracketingValue.EV_1_2)),
                photoSettingTimelapseIntervalRangeIs(DoubleRange.of(2, Double.MAX_VALUE)),
                photoSettingGpslapseIntervalRangeIs(DoubleRange.of(1, Double.MAX_VALUE)),
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_4S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_3),
                photoSettingTimelapseIntervalIs(2),
                photoSettingGpslapseIntervalIs(1),
                settingIsUpToDate()));

        // change to single mode
        // Since no previous format & file format were selected for this mode beforehand,
        // expect format and file format to best available values. Burst, bracketing and interval should be current
        // setting values (are meaningless in this mode).
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.SINGLE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_4S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_3EV, 1f)));
        mCamera.photo().setMode(CameraPhoto.Mode.SINGLE);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.SINGLE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_4S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_3EV, 1f));

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                settingIsUpToDate()));

        // change format
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.SINGLE, ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_4S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_3EV, 1f)));
        mCamera.photo().setFormat(CameraPhoto.Format.FULL_FRAME);

        assertThat(mChangeCnt, is(4));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.SINGLE, ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_4S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_3EV, 1f));

        assertThat(mChangeCnt, is(5));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                settingIsUpToDate()));

        // change file format
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.SINGLE, ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_4S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_3EV, 1f)));
        mCamera.photo().setFileFormat(CameraPhoto.FileFormat.DNG_AND_JPEG);

        assertThat(mChangeCnt, is(6));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.SINGLE, ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_4S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_3EV, 1f));

        assertThat(mChangeCnt, is(7));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                settingIsUpToDate()));

        // change to burst mode.
        // Since no previous format & file format were selected for this mode beforehand,
        // expect format and file format to best available values. Bracketing and interval should be current
        // setting value (meaningless in this mode). Burst should also be current setting value, since not specified.
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BURST, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_4S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_3EV, 1f)));
        mCamera.photo().setMode(CameraPhoto.Mode.BURST);

        assertThat(mChangeCnt, is(8));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_4S),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BURST, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_4S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_3EV, 1f));

        assertThat(mChangeCnt, is(9));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_4S),
                settingIsUpToDate()));

        // change burst value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BURST, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_3EV, 1f)));
        mCamera.photo().setBurstValue(CameraPhoto.BurstValue.BURST_4_OVER_1S);

        assertThat(mChangeCnt, is(10));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_4_OVER_1S),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BURST, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_3EV, 1f));

        assertThat(mChangeCnt, is(11));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_4_OVER_1S),
                settingIsUpToDate()));

        // change back to bracketing mode, expect previous format and file format for this mode
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BRACKETING, ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_3EV, 1f)));
        mCamera.photo().setMode(CameraPhoto.Mode.BRACKETING);

        assertThat(mChangeCnt, is(12));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_3),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BRACKETING, ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_3EV, 1f));

        assertThat(mChangeCnt, is(13));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_3),
                settingIsUpToDate()));

        // change bracketing value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BRACKETING, ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 1f)));
        mCamera.photo().setBracketingValue(CameraPhoto.BracketingValue.EV_1_2);

        assertThat(mChangeCnt, is(14));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BRACKETING, ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 1f));

        assertThat(mChangeCnt, is(15));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                settingIsUpToDate()));

        // change to time-lapse mode.
        // Since no previous format & file format were selected for this mode beforehand,
        // expect format and file format to best available values. Burst and bracketing should be current
        // setting value (meaningless in this mode). Interval should also be current setting value, since not specified.
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.TIME_LAPSE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 2f)));
        mCamera.photo().setMode(CameraPhoto.Mode.TIME_LAPSE);

        assertThat(mChangeCnt, is(16));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingTimelapseIntervalIs(2),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.TIME_LAPSE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 2f));

        assertThat(mChangeCnt, is(17));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingTimelapseIntervalIs(2),
                settingIsUpToDate()));

        // change interval value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.TIME_LAPSE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 5.5f)));
        mCamera.photo().setTimelapseInterval(5.5);

        assertThat(mChangeCnt, is(18));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingTimelapseIntervalIs(5.5),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.TIME_LAPSE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 5.5f));

        assertThat(mChangeCnt, is(19));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingTimelapseIntervalIs(5.5),
                settingIsUpToDate()));

        // change to GPS-lapse mode.
        // Since no previous format & file format were selected for this mode beforehand,
        // expect format and file format to best available values. Burst and bracketing should be current
        // setting value (meaningless in this mode). Interval should also be current setting value, since not specified.
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.GPS_LAPSE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 1f)));
        mCamera.photo().setMode(CameraPhoto.Mode.GPS_LAPSE);

        assertThat(mChangeCnt, is(20));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.GPS_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingGpslapseIntervalIs(1),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.GPS_LAPSE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 1f));

        assertThat(mChangeCnt, is(21));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.GPS_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingGpslapseIntervalIs(1),
                settingIsUpToDate()));

        // change interval value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.GPS_LAPSE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 10f)));
        mCamera.photo().setGpslapseInterval(10);

        assertThat(mChangeCnt, is(22));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.GPS_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingGpslapseIntervalIs(10),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.GPS_LAPSE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 10f));

        assertThat(mChangeCnt, is(23));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.GPS_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingGpslapseIntervalIs(10),
                settingIsUpToDate()));

        // change back to time-lapse mode, expect previous interval value for this mode
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.TIME_LAPSE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 5.5f)));
        mCamera.photo().setMode(CameraPhoto.Mode.TIME_LAPSE);

        // change to bracketing mode before disconnection
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BRACKETING, ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 10f)));
        mCamera.photo().setMode(CameraPhoto.Mode.BRACKETING);

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        assertThat(mChangeCnt, is(0));

        // check stored capabilities
        assertThat(mCamera.photo(), allOf(
                photoSettingSupportsModes(EnumSet.of(
                        CameraPhoto.Mode.SINGLE,
                        CameraPhoto.Mode.BRACKETING,
                        CameraPhoto.Mode.BURST,
                        CameraPhoto.Mode.TIME_LAPSE,
                        CameraPhoto.Mode.GPS_LAPSE)),
                photoSettingSupportsFormats(
                        EnumSet.of(
                                CameraPhoto.Mode.SINGLE,
                                CameraPhoto.Mode.BRACKETING,
                                CameraPhoto.Mode.BURST,
                                CameraPhoto.Mode.TIME_LAPSE,
                                CameraPhoto.Mode.GPS_LAPSE),
                        EnumSet.of(
                                CameraPhoto.Format.RECTILINEAR,
                                CameraPhoto.Format.FULL_FRAME)),
                photoSettingSupportsFileFormats(
                        EnumSet.of(
                                CameraPhoto.Mode.SINGLE,
                                CameraPhoto.Mode.BRACKETING,
                                CameraPhoto.Mode.BURST,
                                CameraPhoto.Mode.TIME_LAPSE,
                                CameraPhoto.Mode.GPS_LAPSE),
                        CameraPhoto.Format.RECTILINEAR,
                        EnumSet.of(CameraPhoto.FileFormat.JPEG)),
                photoSettingSupportsFileFormats(
                        EnumSet.of(
                                CameraPhoto.Mode.SINGLE,
                                CameraPhoto.Mode.BRACKETING),
                        CameraPhoto.Format.FULL_FRAME,
                        EnumSet.of(
                                CameraPhoto.FileFormat.JPEG,
                                CameraPhoto.FileFormat.DNG_AND_JPEG)),
                photoSettingSupportsFileFormats(
                        CameraPhoto.Mode.BURST,
                        CameraPhoto.Format.FULL_FRAME,
                        EnumSet.of(CameraPhoto.FileFormat.JPEG)),
                photoSettingSupportsBurstValues(EnumSet.of(
                        CameraPhoto.BurstValue.BURST_4_OVER_1S,
                        CameraPhoto.BurstValue.BURST_14_OVER_4S)),
                photoSettingSupportsBracketingValues(EnumSet.of(
                        CameraPhoto.BracketingValue.EV_3,
                        CameraPhoto.BracketingValue.EV_1_2)),
                photoSettingTimelapseIntervalRangeIs(DoubleRange.of(2, Double.MAX_VALUE)),
                photoSettingGpslapseIntervalRangeIs(DoubleRange.of(1, Double.MAX_VALUE)),
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_4_OVER_1S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                photoSettingTimelapseIntervalIs(5.5),
                photoSettingGpslapseIntervalIs(10),
                settingIsUpToDate()));

        // change mode to burst offline, expect previous format and file format for this mode
        mCamera.photo().setMode(CameraPhoto.Mode.BURST);

        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_4_OVER_1S),
                settingIsUpToDate()));

        // change mode to single offline, expect previous format and file format for this mode
        mCamera.photo().setMode(CameraPhoto.Mode.SINGLE);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                settingIsUpToDate()));

        // change format offline, expect file format to change accordingly
        mCamera.photo().setFormat(CameraPhoto.Format.RECTILINEAR);

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                settingIsUpToDate()));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                        ArsdkFeatureCamera.PhotoMode.BRACKETING, ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                        ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                        ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 5f))
                .expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                        ArsdkFeatureCamera.PhotoMode.SINGLE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                        ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                        ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 10f))));
        mMockArsdkCore.assertNoExpectation();

        // expect no change
        assertThat(mChangeCnt, is(4)); // active state change
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                settingIsUpToDate()));
    }

    @Test
    public void testPhotoSettingsCornerCases() {
        // tests that ensures photo settings reacts correctly in corner cases, for instance when the drone sends
        // incorrect values
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().bracketings(
                        ArsdkFeatureCamera.BracketingPreset.PRESET_2EV,
                        ArsdkFeatureCamera.BracketingPreset.PRESET_3EV,
                        ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV)
                      .bursts(
                              ArsdkFeatureCamera.BurstValue.BURST_14_OVER_1S,
                              ArsdkFeatureCamera.BurstValue.BURST_10_OVER_2S,
                              ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S)
                      .minTimelapse(5)
                      .minGpslapse(10)
                      .encode(),
                ArsdkEncoder.encodeCameraPhotoCapabilities(0,
                        ArsdkFeatureCamera.PhotoMode.toBitField(
                                ArsdkFeatureCamera.PhotoMode.SINGLE,
                                ArsdkFeatureCamera.PhotoMode.BRACKETING),
                        ArsdkFeatureCamera.PhotoFormat.toBitField(ArsdkFeatureCamera.PhotoFormat.FULL_FRAME),
                        ArsdkFeatureCamera.PhotoFileFormat.toBitField(ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG,
                                ArsdkFeatureCamera.PhotoFileFormat.JPEG),
                        ArsdkFeatureCamera.Supported.NOT_SUPPORTED,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)),
                ArsdkEncoder.encodeCameraPhotoCapabilities(1,
                        ArsdkFeatureCamera.PhotoMode.toBitField(
                                ArsdkFeatureCamera.PhotoMode.SINGLE,
                                ArsdkFeatureCamera.PhotoMode.BRACKETING),
                        ArsdkFeatureCamera.PhotoFormat.toBitField(ArsdkFeatureCamera.PhotoFormat.RECTILINEAR),
                        ArsdkFeatureCamera.PhotoFileFormat.toBitField(ArsdkFeatureCamera.PhotoFileFormat.JPEG),
                        ArsdkFeatureCamera.Supported.NOT_SUPPORTED,
                        ArsdkFeatureGeneric.ListFlags.toBitField()),
                ArsdkEncoder.encodeCameraPhotoCapabilities(2,
                        ArsdkFeatureCamera.PhotoMode.toBitField(
                                ArsdkFeatureCamera.PhotoMode.BURST,
                                ArsdkFeatureCamera.PhotoMode.TIME_LAPSE,
                                ArsdkFeatureCamera.PhotoMode.GPS_LAPSE),
                        ArsdkFeatureCamera.PhotoFormat.toBitField(
                                ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                                ArsdkFeatureCamera.PhotoFormat.RECTILINEAR),
                        ArsdkFeatureCamera.PhotoFileFormat.toBitField(ArsdkFeatureCamera.PhotoFileFormat.JPEG),
                        ArsdkFeatureCamera.Supported.NOT_SUPPORTED,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)),
                ArsdkEncoder.encodeCameraCameraMode(0, ArsdkFeatureCamera.CameraMode.PHOTO),
                ArsdkEncoder.encodeCameraPhotoMode(0,
                        ArsdkFeatureCamera.PhotoMode.SINGLE,
                        ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                        ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG,
                        ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                        ArsdkFeatureCamera.BracketingPreset.PRESET_3EV,
                        5f)));

        assertThat(mChangeCnt, is(1));

        // check capabilities
        assertThat(mCamera.photo(), allOf(
                photoSettingSupportsModes(EnumSet.of(
                        CameraPhoto.Mode.SINGLE,
                        CameraPhoto.Mode.BRACKETING,
                        CameraPhoto.Mode.BURST,
                        CameraPhoto.Mode.TIME_LAPSE,
                        CameraPhoto.Mode.GPS_LAPSE)),
                photoSettingSupportsFormats(
                        EnumSet.of(
                                CameraPhoto.Mode.SINGLE,
                                CameraPhoto.Mode.BRACKETING,
                                CameraPhoto.Mode.BURST,
                                CameraPhoto.Mode.TIME_LAPSE,
                                CameraPhoto.Mode.GPS_LAPSE),
                        EnumSet.of(
                                CameraPhoto.Format.RECTILINEAR,
                                CameraPhoto.Format.FULL_FRAME)),
                photoSettingSupportsFileFormats(
                        EnumSet.of(
                                CameraPhoto.Mode.SINGLE,
                                CameraPhoto.Mode.BRACKETING,
                                CameraPhoto.Mode.BURST,
                                CameraPhoto.Mode.TIME_LAPSE,
                                CameraPhoto.Mode.GPS_LAPSE),
                        CameraPhoto.Format.RECTILINEAR,
                        EnumSet.of(CameraPhoto.FileFormat.JPEG)),
                photoSettingSupportsFileFormats(
                        EnumSet.of(
                                CameraPhoto.Mode.SINGLE,
                                CameraPhoto.Mode.BRACKETING),
                        CameraPhoto.Format.FULL_FRAME,
                        EnumSet.of(
                                CameraPhoto.FileFormat.JPEG,
                                CameraPhoto.FileFormat.DNG_AND_JPEG)),
                photoSettingSupportsFileFormats(
                        CameraPhoto.Mode.BURST,
                        CameraPhoto.Format.FULL_FRAME,
                        EnumSet.of(CameraPhoto.FileFormat.JPEG)),
                photoSettingSupportsBurstValues(EnumSet.of(
                        CameraPhoto.BurstValue.BURST_14_OVER_1S,
                        CameraPhoto.BurstValue.BURST_10_OVER_2S,
                        CameraPhoto.BurstValue.BURST_4_OVER_1S)),
                photoSettingSupportsBracketingValues(EnumSet.of(
                        CameraPhoto.BracketingValue.EV_2,
                        CameraPhoto.BracketingValue.EV_3,
                        CameraPhoto.BracketingValue.EV_1_2)),
                photoSettingTimelapseIntervalRangeIs(DoubleRange.of(5, Double.MAX_VALUE)),
                photoSettingGpslapseIntervalRangeIs(DoubleRange.of(10, Double.MAX_VALUE)),
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_1S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                photoSettingTimelapseIntervalIs(5),
                photoSettingGpslapseIntervalIs(10),
                settingIsUpToDate()));

        // switch to burst mode, should use current setting burst value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BURST, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_2EV, 10f)));
        mCamera.photo().setMode(CameraPhoto.Mode.BURST);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_1S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BURST, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_2EV, 5f));

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_1S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpToDate()));

        // switch to single mode, should use current setting burst value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.SINGLE, ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_2EV, 10f)));
        mCamera.photo().setMode(CameraPhoto.Mode.SINGLE);

        assertThat(mChangeCnt, is(4));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_1S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.SINGLE, ArsdkFeatureCamera.PhotoFormat.FULL_FRAME,
                ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_1S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_2EV, 5f));

        assertThat(mChangeCnt, is(5));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_1S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpToDate()));

        // change burst value while not in burst mode, should not send any command, setting should update immediately
        mCamera.photo().setBurstValue(CameraPhoto.BurstValue.BURST_10_OVER_2S);

        assertThat(mChangeCnt, is(6));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // select an unsupported burst value, nothing should change
        mCamera.photo().setBurstValue(CameraPhoto.BurstValue.BURST_10_OVER_4S);

        assertThat(mChangeCnt, is(6));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                photoSettingFormatIs(CameraPhoto.Format.FULL_FRAME),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.DNG_AND_JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // switch back to burst mode, should use the previously set burst value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BURST, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_10_OVER_2S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_2EV, 10f)));
        mCamera.photo().setMode(CameraPhoto.Mode.BURST);

        assertThat(mChangeCnt, is(7));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpdating()));

        // check that if the drone sends back an invalid burst value, we still publish something valid
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BURST, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_10_OVER_4S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_2EV, 5f));

        assertThat(mChangeCnt, is(8));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_14_OVER_1S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpToDate()));

        // change burst value while in burst mode
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BURST, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_10_OVER_2S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_2EV, 10f)));
        mCamera.photo().setBurstValue(CameraPhoto.BurstValue.BURST_10_OVER_2S);

        assertThat(mChangeCnt, is(9));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BURST, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_10_OVER_2S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_2EV, 5f));

        assertThat(mChangeCnt, is(10));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpToDate()));

        // change bracketing value while not in bracketing mode, should not send any command, setting should update
        // immediately
        mCamera.photo().setBracketingValue(CameraPhoto.BracketingValue.EV_1_2);

        assertThat(mChangeCnt, is(11));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // select an unsupported bracketing value, nothing should change
        mCamera.photo().setBracketingValue(CameraPhoto.BracketingValue.EV_1_2_3);

        assertThat(mChangeCnt, is(11));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // switch to bracketing mode, should use the previously set bracketing value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BRACKETING, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_10_OVER_2S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV, 10f)));
        mCamera.photo().setMode(CameraPhoto.Mode.BRACKETING);

        assertThat(mChangeCnt, is(12));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_1_2),
                settingIsUpdating()));

        // check that if the drone sends back an invalid bracketing value, we still publish something valid
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BRACKETING, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_10_OVER_4S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV_3EV, 5f));

        assertThat(mChangeCnt, is(13));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingBurstValueIs(CameraPhoto.BurstValue.BURST_10_OVER_2S),
                photoSettingBracketingValueIs(CameraPhoto.BracketingValue.EV_2),
                settingIsUpToDate()));

        // change time-lapse interval value while not in time-lapse mode, should not send any command, setting should
        // update immediately
        mCamera.photo().setTimelapseInterval(20);

        assertThat(mChangeCnt, is(14));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BRACKETING),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingTimelapseIntervalIs(20),
                settingIsUpToDate()));
        mMockArsdkCore.assertNoExpectation();

        // switch to time-lapse mode, should use the previously set interval value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.TIME_LAPSE, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_10_OVER_2S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV_3EV, 20f)));
        mCamera.photo().setMode(CameraPhoto.Mode.TIME_LAPSE);

        assertThat(mChangeCnt, is(15));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.TIME_LAPSE),
                photoSettingFormatIs(CameraPhoto.Format.RECTILINEAR),
                photoSettingFileFormatIs(CameraPhoto.FileFormat.JPEG),
                photoSettingTimelapseIntervalIs(20),
                settingIsUpdating()));
    }

    @Test
    public void testZoom() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().encode(),
                ArsdkEncoder.encodeCameraZoomInfo(0,
                        ArsdkFeatureCamera.Availability.AVAILABLE, 2.0f, 3.0f),
                ArsdkEncoder.encodeCameraZoomLevel(0, 1.0f),
                ArsdkEncoder.encodeCameraMaxZoomSpeed(0, 0.5f, 20.0f, 10.0f),
                ArsdkEncoder.encodeCameraZoomVelocityQualityDegradation(0, 0)));

        // check initial values
        assertThat(mChangeCnt, is(1));
        CameraZoom zoom = mCamera.zoom();
        assertThat(zoom, notNullValue());
        assertThat(zoom.isAvailable(), is(true));
        assertThat(zoom.getMaxLossLessLevel(), is(2.0));
        assertThat(zoom.getMaxLossyLevel(), is(3.0));
        assertThat(zoom.getCurrentLevel(), is(1.0));
        assertThat(zoom.maxSpeed(), doubleSettingIsUpToDateAt(0.5, 10.0, 20.0));
        assertThat(zoom.velocityQualityDegradationAllowance(), booleanSettingIsDisabled());

        // change zoom level from backend
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraZoomLevel(0, 5.0f));
        assertThat(mChangeCnt, is(2));
        assertThat(zoom.getCurrentLevel(), is(5.0));

        // change max zoom velocity from api
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.cameraSetMaxZoomSpeed(0, 12.0f)));
        zoom.maxSpeed().setValue(12.0f);

        assertThat(mChangeCnt, is(3));
        assertThat(zoom.maxSpeed(), doubleSettingIsUpdatingTo(0.5, 12.0, 20.0));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraMaxZoomSpeed(0, 0.5f, 20.0f, 13.0f));

        assertThat(mChangeCnt, is(4));
        assertThat(zoom.maxSpeed(), doubleSettingIsUpToDateAt(0.5, 13.0, 20.0));
        mMockArsdkCore.assertNoExpectation();

        // change quality degradation allowance from api
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.cameraSetZoomVelocityQualityDegradation(0, 1)));
        zoom.velocityQualityDegradationAllowance().toggle();

        assertThat(mChangeCnt, is(5));
        assertThat(zoom.velocityQualityDegradationAllowance(), booleanSettingIsEnabling());

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraZoomVelocityQualityDegradation(0, 1));

        assertThat(mChangeCnt, is(6));
        assertThat(zoom.velocityQualityDegradationAllowance(), booleanSettingIsEnabled());
        mMockArsdkCore.assertNoExpectation();

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        // setting should be restored to latest user set value
        assertThat(mChangeCnt, is(0));
        zoom = mCamera.zoom();
        assertThat(zoom, notNullValue());
        assertThat(zoom.maxSpeed(), doubleSettingIsUpToDateAt(0.5, 12.0, 20.0));
        assertThat(zoom.velocityQualityDegradationAllowance(), booleanSettingIsEnabled());

        // change settings offline
        zoom.maxSpeed().setValue(3.0);
        assertThat(mChangeCnt, is(1));
        assertThat(zoom.maxSpeed(), doubleSettingIsUpToDateAt(0.5, 3.0, 20.0));

        zoom.velocityQualityDegradationAllowance().toggle();
        assertThat(mChangeCnt, is(2));
        assertThat(zoom.velocityQualityDegradationAllowance(), booleanSettingIsDisabled());

        // reconnect
        connectDrone(mDrone, 1);

        mMockArsdkCore.assertNoExpectation();
        assertThat(mChangeCnt, is(3)); // active state change
        assertThat(zoom.maxSpeed(), doubleSettingIsUpToDateAt(0.5, 3.0, 20.0));
        assertThat(zoom.velocityQualityDegradationAllowance(), booleanSettingIsDisabled());
    }

    @Test
    public void testStartStopRecording() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().modes(ArsdkFeatureCamera.CameraMode.RECORDING)
                      .encode(),
                ArsdkEncoder.encodeCameraCameraMode(0, ArsdkFeatureCamera.CameraMode.RECORDING),
                ArsdkEncoder.encodeCameraRecordingState(0, ArsdkFeatureCamera.Availability.AVAILABLE,
                        ArsdkFeatureCamera.State.INACTIVE, 0)));

        assertThat(mChangeCnt, is(1));

        // check initial values
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STOPPED),
                recordingStartTimeIs(null),
                recordingMediaIdIs(null)));

        // start recording
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraStartRecording(0)));
        mCamera.startRecording();

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STARTING),
                recordingStartTimeIs(null),
                recordingMediaIdIs(null)));

        // mock active state from drone
        Date startTime = new Date();
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraRecordingState(0, ArsdkFeatureCamera.Availability.AVAILABLE,
                        ArsdkFeatureCamera.State.ACTIVE, startTime.getTime()));

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STARTED),
                recordingStartTimeIs(startTime),
                recordingMediaIdIs(null)));

        // drone will probably send a started _event_ but we ignore it
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraRecordingProgress(0, ArsdkFeatureCamera.RecordingResult.STARTED, ""));

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STARTED),
                recordingStartTimeIs(startTime),
                recordingMediaIdIs(null)));

        // stop recording
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraStopRecording(0)));
        mCamera.stopRecording();

        assertThat(mChangeCnt, is(4));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STOPPING),
                recordingStartTimeIs(null),
                recordingMediaIdIs(null)));

        // mock stopped event from drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingProgress(0,
                ArsdkFeatureCamera.RecordingResult.STOPPED, "M123"));

        assertThat(mChangeCnt, is(5));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STOPPED),
                recordingStartTimeIs(null),
                recordingMediaIdIs("M123")));

        // mock inactive event from drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingState(0,
                ArsdkFeatureCamera.Availability.AVAILABLE, ArsdkFeatureCamera.State.INACTIVE, 0));

        // should change nothing since we already processed the event
        assertThat(mChangeCnt, is(5));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STOPPED),
                recordingStartTimeIs(null),
                recordingMediaIdIs("M123")));

        // test memory full error
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraStartRecording(0)));
        mCamera.startRecording();

        assertThat(mChangeCnt, is(6));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingProgress(0,
                ArsdkFeatureCamera.RecordingResult.STOPPED_NO_STORAGE_SPACE, "M456"));

        assertThat(mChangeCnt, is(7));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.ERROR_INSUFFICIENT_STORAGE_SPACE),
                recordingStartTimeIs(null),
                recordingMediaIdIs("M456")));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingState(0,
                ArsdkFeatureCamera.Availability.AVAILABLE, ArsdkFeatureCamera.State.INACTIVE, 0));

        assertThat(mChangeCnt, is(8));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STOPPED),
                recordingStartTimeIs(null),
                recordingMediaIdIs("M456")));

        // test stopped for reconfiguration
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraStartRecording(0)));
        mCamera.startRecording();

        assertThat(mChangeCnt, is(9));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingProgress(0,
                ArsdkFeatureCamera.RecordingResult.STOPPED_RECONFIGURED, "M456"));

        assertThat(mChangeCnt, is(10));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.CONFIGURATION_CHANGE),
                recordingStartTimeIs(null),
                recordingMediaIdIs("M456")));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraRecordingState(0,
                ArsdkFeatureCamera.Availability.AVAILABLE, ArsdkFeatureCamera.State.INACTIVE, 0));

        assertThat(mChangeCnt, is(11));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STOPPED),
                recordingStartTimeIs(null),
                recordingMediaIdIs("M456")));

        // disconnect
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(12));
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.UNAVAILABLE),
                recordingStartTimeIs(null),
                recordingMediaIdIs(null)));

        // test recording start time sent during connection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraRecordingState(0, ArsdkFeatureCamera.Availability.AVAILABLE,
                        ArsdkFeatureCamera.State.ACTIVE, startTime.getTime())));

        assertThat(mChangeCnt, is(14)); // recording state + active state change
        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.STARTED),
                recordingStartTimeIs(startTime),
                recordingMediaIdIs(null)));
    }

    @Test
    public void testAutoRecord() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().modes(ArsdkFeatureCamera.CameraMode.RECORDING)
                      .encode(),
                ArsdkEncoder.encodeCameraCameraMode(0, ArsdkFeatureCamera.CameraMode.RECORDING)));

        // check initial values
        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.autoRecord(), optionalSettingIsUnavailable());

        // receive first value
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraAutorecord(0, ArsdkFeatureCamera.State.INACTIVE));
        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.autoRecord(), optionalSettingIsAvailable());
        assertThat(mCamera.autoRecord(), optionalBooleanSettingIsDisabled());

        // change value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetAutorecord(0,
                ArsdkFeatureCamera.State.ACTIVE)));
        mCamera.autoRecord().toggle();
        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.autoRecord(), optionalBooleanSettingIsEnabling());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraAutorecord(0, ArsdkFeatureCamera.State.ACTIVE));
        assertThat(mChangeCnt, is(4));
        assertThat(mCamera.autoRecord(), optionalBooleanSettingIsEnabled());

        // disconnect
        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(5)); // active state change

        // check auto-record setting did not change
        assertThat(mCamera.autoRecord(), optionalBooleanSettingIsEnabled());

        // change value offline
        mCamera.autoRecord().toggle();
        assertThat(mChangeCnt, is(6));
        assertThat(mCamera.autoRecord(), optionalBooleanSettingIsDisabled());

        // restart engine
        resetEngine();

        // reconnect
        assertThat(mChangeCnt, is(0));
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().modes(ArsdkFeatureCamera.CameraMode.RECORDING)
                      .encode(),
                ArsdkEncoder.encodeCameraCameraMode(0, ArsdkFeatureCamera.CameraMode.RECORDING),
                ArsdkEncoder.encodeCameraAutorecord(0, ArsdkFeatureCamera.State.INACTIVE)));
        assertThat(mCamera.autoRecord(), optionalBooleanSettingIsDisabled());
    }

    @Test
    public void testStartStopPhotoCapture() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().modes(ArsdkFeatureCamera.CameraMode.PHOTO)
                      .encode(),
                ArsdkEncoder.encodeCameraPhotoCapabilities(0,
                        ArsdkFeatureCamera.PhotoMode.toBitField(ArsdkFeatureCamera.PhotoMode.TIME_LAPSE),
                        ArsdkFeatureCamera.PhotoFormat.toBitField(ArsdkFeatureCamera.PhotoFormat.RECTILINEAR),
                        ArsdkFeatureCamera.PhotoFileFormat.toBitField(ArsdkFeatureCamera.PhotoFileFormat.JPEG),
                        ArsdkFeatureCamera.Supported.NOT_SUPPORTED,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                                ArsdkFeatureGeneric.ListFlags.LAST)),
                ArsdkEncoder.encodeCameraCameraMode(0, ArsdkFeatureCamera.CameraMode.PHOTO),
                ArsdkEncoder.encodeCameraPhotoMode(0,
                        ArsdkFeatureCamera.PhotoMode.TIME_LAPSE,
                        ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                        ArsdkFeatureCamera.PhotoFileFormat.JPEG,
                        ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S,
                        ArsdkFeatureCamera.BracketingPreset.PRESET_3EV,
                        2f),
                ArsdkEncoder.encodeCameraPhotoState(0, ArsdkFeatureCamera.Availability.AVAILABLE,
                        ArsdkFeatureCamera.State.INACTIVE)));

        assertThat(mChangeCnt, is(1));

        // check initial values
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STOPPED),
                photoCountIs(0),
                photoMediaIdIs(null)));

        // start photo capture
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraTakePhoto(0)));
        mCamera.startPhotoCapture();

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STARTED),
                photoCountIs(0),
                photoMediaIdIs(null)));

        // mock active state + taking photo event from drone
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraPhotoState(0, ArsdkFeatureCamera.Availability.AVAILABLE,
                        ArsdkFeatureCamera.State.ACTIVE),
                ArsdkEncoder.encodeCameraPhotoProgress(0, ArsdkFeatureCamera.PhotoResult.TAKING_PHOTO, 0, ""));

        // should do nothing since we are already in the appropriate state
        assertThat(mChangeCnt, is(2));

        // mock photo taken event
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraPhotoProgress(0, ArsdkFeatureCamera.PhotoResult.PHOTO_TAKEN,
                        1, ""));

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STARTED),
                photoCountIs(1),
                photoMediaIdIs(null)));

        // mock more photo taken event (imagine burst mode here)
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraPhotoProgress(0, ArsdkFeatureCamera.PhotoResult.PHOTO_TAKEN,
                        4, ""));

        assertThat(mChangeCnt, is(4));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STARTED),
                photoCountIs(4),
                photoMediaIdIs(null)));

        // stop photo capture
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraStopPhoto(0)));
        mCamera.stopPhotoCapture();

        assertThat(mChangeCnt, is(5));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STOPPING),
                photoCountIs(0),
                photoMediaIdIs(null)));

        // mock photo saved event
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraPhotoProgress(0, ArsdkFeatureCamera.PhotoResult.PHOTO_SAVED, 0, "M123"));

        assertThat(mChangeCnt, is(6));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STOPPED),
                photoCountIs(0),
                photoMediaIdIs("M123")));

        // mock photo function back to ready
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraPhotoState(0, ArsdkFeatureCamera.Availability.AVAILABLE,
                        ArsdkFeatureCamera.State.INACTIVE));

        // should not change anything since we are in the proper state
        assertThat(mChangeCnt, is(6));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STOPPED),
                photoCountIs(0),
                photoMediaIdIs("M123")));

        // test memory full error
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraTakePhoto(0)));
        mCamera.startPhotoCapture();

        assertThat(mChangeCnt, is(7));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraPhotoProgress(0, ArsdkFeatureCamera.PhotoResult.ERROR_NO_STORAGE_SPACE,
                        0, ""));

        assertThat(mChangeCnt, is(8));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.ERROR_INSUFFICIENT_STORAGE),
                photoCountIs(0),
                photoMediaIdIs(null)));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraPhotoState(0, ArsdkFeatureCamera.Availability.AVAILABLE,
                        ArsdkFeatureCamera.State.INACTIVE));

        assertThat(mChangeCnt, is(9));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.STOPPED),
                photoCountIs(0),
                photoMediaIdIs(null)));

        // disconnect
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(10));
        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.UNAVAILABLE),
                photoCountIs(0),
                photoMediaIdIs(null)));
    }

    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                caps().modes(ArsdkFeatureCamera.CameraMode.values())
                      .exposureModes(ArsdkFeatureCamera.ExposureMode.values())
                      .evs(ArsdkFeatureCamera.EvCompensation.values())
                      .whiteBalanceModes(ArsdkFeatureCamera.WhiteBalanceMode.values())
                      .temperatures(ArsdkFeatureCamera.WhiteBalanceTemperature.values())
                      .styles(ArsdkFeatureCamera.Style.values())
                      .bursts(ArsdkFeatureCamera.BurstValue.values())
                      .bracketings(ArsdkFeatureCamera.BracketingPreset.values())
                      .encode(),
                ArsdkEncoder.encodeCameraCameraMode(0, ArsdkFeatureCamera.CameraMode.RECORDING),
                ArsdkEncoder.encodeCameraExposureSettings(0,
                        ArsdkFeatureCamera.ExposureMode.AUTOMATIC,
                        ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1,
                        ArsdkFeatureCamera.ShutterSpeed.toBitField(ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1),
                        ArsdkFeatureCamera.IsoSensitivity.ISO_50,
                        ArsdkFeatureCamera.IsoSensitivity.toBitField(ArsdkFeatureCamera.IsoSensitivity.ISO_50),
                        ArsdkFeatureCamera.IsoSensitivity.ISO_50,
                        ArsdkFeatureCamera.IsoSensitivity.toBitField(ArsdkFeatureCamera.IsoSensitivity.ISO_50),
                        ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD),
                ArsdkEncoder.encodeCameraEvCompensation(0, ArsdkFeatureCamera.EvCompensation.EV_0_00),
                ArsdkEncoder.encodeCameraWhiteBalance(0,
                        ArsdkFeatureCamera.WhiteBalanceMode.AUTOMATIC,
                        ArsdkFeatureCamera.WhiteBalanceTemperature.T_1500, ArsdkFeatureCamera.State.INACTIVE),
                ArsdkEncoder.encodeCameraHdrSetting(0, ArsdkFeatureCamera.State.INACTIVE),
                ArsdkEncoder.encodeCameraStyle(0, ArsdkFeatureCamera.Style.STANDARD, 1, 0, 1, 1, 0, 1, 1, 0, 1),
                ArsdkEncoder.encodeCameraPhotoCapabilities(0,
                        ArsdkFeatureCamera.PhotoMode.toBitField(ArsdkFeatureCamera.PhotoMode.values()),
                        ArsdkFeatureCamera.PhotoFormat.toBitField(ArsdkFeatureCamera.PhotoFormat.values()),
                        ArsdkFeatureCamera.PhotoFileFormat.toBitField(ArsdkFeatureCamera.PhotoFileFormat.values()),
                        ArsdkFeatureCamera.Supported.SUPPORTED,
                        ArsdkFeatureGeneric.ListFlags.toBitField(
                                ArsdkFeatureGeneric.ListFlags.FIRST,
                                ArsdkFeatureGeneric.ListFlags.LAST)),
                ArsdkEncoder.encodeCameraPhotoMode(0,
                        ArsdkFeatureCamera.PhotoMode.SINGLE,
                        ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                        ArsdkFeatureCamera.PhotoFileFormat.JPEG,
                        ArsdkFeatureCamera.BurstValue.BURST_10_OVER_1S,
                        ArsdkFeatureCamera.BracketingPreset.PRESET_1EV,
                        5f),
                ArsdkEncoder.encodeCameraRecordingCapabilities(0,
                        ArsdkFeatureCamera.RecordingMode.toBitField(ArsdkFeatureCamera.RecordingMode.values()),
                        ArsdkFeatureCamera.Resolution.toBitField(ArsdkFeatureCamera.Resolution.values()),
                        ArsdkFeatureCamera.Framerate.toBitField(ArsdkFeatureCamera.Framerate.values()),
                        ArsdkFeatureCamera.Supported.SUPPORTED,
                        ArsdkFeatureGeneric.ListFlags.toBitField(
                                ArsdkFeatureGeneric.ListFlags.FIRST,
                                ArsdkFeatureGeneric.ListFlags.LAST)),
                ArsdkEncoder.encodeCameraRecordingMode(0,
                        ArsdkFeatureCamera.RecordingMode.STANDARD,
                        ArsdkFeatureCamera.Resolution.RES_DCI_4K,
                        ArsdkFeatureCamera.Framerate.FPS_120,
                        ArsdkFeatureCamera.HyperlapseValue.RATIO_15,
                        1000000),
                ArsdkEncoder.encodeCameraAutorecord(0, ArsdkFeatureCamera.State.INACTIVE),
                ArsdkEncoder.encodeCameraMaxZoomSpeed(0, 0, 1, 0),
                ArsdkEncoder.encodeCameraZoomVelocityQualityDegradation(0, 0)));

        assertThat(mChangeCnt, is(1));
        assertThat(mCamera.mode(), allOf(
                enumSettingValueIs(Camera.Mode.RECORDING),
                settingIsUpToDate()));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.AUTOMATIC),
                settingIsUpToDate()));
        assertThat(mCamera.exposureCompensation(), allOf(
                enumSettingValueIs(CameraEvCompensation.EV_0),
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
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.SINGLE),
                settingIsUpToDate()));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.STANDARD),
                settingIsUpToDate()));
        assertThat(mCamera.autoRecord(), allOf(
                optionalBooleanSettingValueIs(false),
                settingIsUpToDate()));

        CameraZoom zoom = mCamera.zoom();
        assertThat(zoom, notNullValue());

        assertThat(zoom.velocityQualityDegradationAllowance(), allOf(
                booleanSettingValueIs(false),
                settingIsUpToDate()));
        assertThat(zoom.maxSpeed(), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));

        // mock user modifies settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetCameraMode(0,
                ArsdkFeatureCamera.CameraMode.PHOTO)));
        mCamera.mode().setValue(Camera.Mode.PHOTO);

        assertThat(mChangeCnt, is(2));
        assertThat(mCamera.mode(), allOf(
                enumSettingValueIs(Camera.Mode.PHOTO),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetExposureSettings(0,
                ArsdkFeatureCamera.ExposureMode.MANUAL_SHUTTER_SPEED,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1, ArsdkFeatureCamera.IsoSensitivity.ISO_50,
                ArsdkFeatureCamera.IsoSensitivity.ISO_50, ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD)));
        mCamera.exposure().setMode(CameraExposure.Mode.MANUAL_SHUTTER_SPEED);

        assertThat(mChangeCnt, is(3));
        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetEvCompensation(0,
                ArsdkFeatureCamera.EvCompensation.EV_0_33)));
        mCamera.exposureCompensation().setValue(CameraEvCompensation.EV_0_33);

        assertThat(mChangeCnt, is(4));
        assertThat(mCamera.exposureCompensation(), allOf(
                enumSettingValueIs(CameraEvCompensation.EV_0_33),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetWhiteBalance(0,
                ArsdkFeatureCamera.WhiteBalanceMode.CUSTOM, ArsdkFeatureCamera.WhiteBalanceTemperature.T_1500)));
        mCamera.whiteBalance().setMode(CameraWhiteBalance.Mode.CUSTOM);

        assertThat(mChangeCnt, is(5));
        assertThat(mCamera.whiteBalance(), allOf(
                whiteBalanceSettingModeIs(CameraWhiteBalance.Mode.CUSTOM),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetHdrSetting(0,
                ArsdkFeatureCamera.State.ACTIVE)));
        mCamera.autoHdr().setEnabled(true);

        assertThat(mChangeCnt, is(6));
        assertThat(mCamera.autoHdr(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetStyle(0, ArsdkFeatureCamera.Style.PLOG)));
        mCamera.style().setStyle(CameraStyle.Style.PLOG);

        assertThat(mChangeCnt, is(7));
        assertThat(mCamera.style(), allOf(
                styleSettingStyleIs(CameraStyle.Style.PLOG),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetPhotoMode(0,
                ArsdkFeatureCamera.PhotoMode.BURST, ArsdkFeatureCamera.PhotoFormat.RECTILINEAR,
                ArsdkFeatureCamera.PhotoFileFormat.JPEG, ArsdkFeatureCamera.BurstValue.BURST_14_OVER_4S,
                ArsdkFeatureCamera.BracketingPreset.PRESET_1EV, 1f)));
        mCamera.photo().setMode(CameraPhoto.Mode.BURST);

        assertThat(mChangeCnt, is(8));
        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetRecordingMode(0,
                ArsdkFeatureCamera.RecordingMode.HIGH_FRAMERATE, ArsdkFeatureCamera.Resolution.RES_DCI_4K,
                ArsdkFeatureCamera.Framerate.FPS_240, ArsdkFeatureCamera.HyperlapseValue.RATIO_15)));
        mCamera.recording().setMode(CameraRecording.Mode.HIGH_FRAMERATE);

        assertThat(mChangeCnt, is(9));
        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetAutorecord(0,
                ArsdkFeatureCamera.State.ACTIVE)));
        mCamera.autoRecord().setEnabled(true);

        assertThat(mChangeCnt, is(10));
        assertThat(mCamera.autoRecord(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetZoomVelocityQualityDegradation(0, 1)));
        zoom.velocityQualityDegradationAllowance().setEnabled(true);

        assertThat(mChangeCnt, is(11));
        assertThat(zoom.velocityQualityDegradationAllowance(), allOf(
                booleanSettingValueIs(true),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetMaxZoomSpeed(0, 1)));
        zoom.maxSpeed().setValue(1);

        assertThat(mChangeCnt, is(12));
        assertThat(zoom.maxSpeed(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpdating()));

        // disconnect
        disconnectDrone(mDrone, 1);

        // setting should be updated to user value
        assertThat(mChangeCnt, is(13));

        assertThat(mCamera.mode(), allOf(
                enumSettingValueIs(Camera.Mode.PHOTO),
                settingIsUpToDate()));

        assertThat(mCamera.exposureCompensation(), allOf(
                enumSettingValueIs(CameraEvCompensation.EV_0_33),
                settingIsUpToDate()));

        assertThat(mCamera.exposure(), allOf(
                exposureSettingModeIs(CameraExposure.Mode.MANUAL_SHUTTER_SPEED),
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

        assertThat(mCamera.photo(), allOf(
                photoSettingModeIs(CameraPhoto.Mode.BURST),
                settingIsUpToDate()));

        assertThat(mCamera.recording(), allOf(
                recordingSettingModeIs(CameraRecording.Mode.HIGH_FRAMERATE),
                settingIsUpToDate()));

        assertThat(mCamera.autoRecord(), allOf(
                optionalBooleanSettingValueIs(true),
                settingIsUpToDate()));

        assertThat(zoom.velocityQualityDegradationAllowance(), allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));

        assertThat(zoom.maxSpeed(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        // test other values are reset as they should
        assertThat(zoom.getCurrentLevel(), is(1.0));
        assertThat(zoom.getMaxLossyLevel(), is(1.0));
        assertThat(zoom.getMaxLossLessLevel(), is(1.0));

        assertThat(mCamera.recording().bitrate(), is(0));

        assertThat(mCamera.photoState(), allOf(
                photoStateIs(CameraPhoto.State.FunctionState.UNAVAILABLE),
                photoMediaIdIs(null),
                photoCountIs(0)));

        assertThat(mCamera.recordingState(), allOf(
                recordingStateIs(CameraRecording.State.FunctionState.UNAVAILABLE),
                recordingMediaIdIs(null),
                recordingStartTimeIs(null),
                recordingDurationIs(0)));
    }

    private static final class CapabilitiesEncoder {

        private static final ArsdkFeatureCamera.Supported NA = ArsdkFeatureCamera.Supported.NOT_SUPPORTED;

        private int mModes;

        private int mExposureModes;

        private int mAutoExposureMeteringModes;

        private long mEvs;

        private int mWhiteBalanceModes;

        private ArsdkFeatureCamera.Supported mWhiteBalanceLockSupported;

        private int mStyles;

        private long mTemps;

        private int mHyperlapses;

        private int mBracketings;

        private int mBursts;

        private float mMinTimelapseInterval;

        private float mMinGpslapseInterval;

        CapabilitiesEncoder modes(ArsdkFeatureCamera.CameraMode... modes) {
            mModes = ArsdkFeatureCamera.CameraMode.toBitField(modes);
            return this;
        }

        CapabilitiesEncoder exposureModes(ArsdkFeatureCamera.ExposureMode... modes) {
            mExposureModes = ArsdkFeatureCamera.ExposureMode.toBitField(modes);
            return this;
        }

        CapabilitiesEncoder autoExposureMeteringModes(ArsdkFeatureCamera.AutoExposureMeteringMode... modes) {
            mAutoExposureMeteringModes = ArsdkFeatureCamera.AutoExposureMeteringMode.toBitField(modes);
            return this;
        }

        CapabilitiesEncoder evs(ArsdkFeatureCamera.EvCompensation... evs) {
            mEvs = ArsdkFeatureCamera.EvCompensation.toBitField(evs);
            return this;
        }

        CapabilitiesEncoder whiteBalanceModes(ArsdkFeatureCamera.WhiteBalanceMode... modes) {
            mWhiteBalanceModes = ArsdkFeatureCamera.WhiteBalanceMode.toBitField(modes);
            return this;
        }

        CapabilitiesEncoder whiteBalanceLock(ArsdkFeatureCamera.Supported supported) {
            mWhiteBalanceLockSupported = supported;
            return this;
        }

        CapabilitiesEncoder styles(ArsdkFeatureCamera.Style... styles) {
            mStyles = ArsdkFeatureCamera.Style.toBitField(styles);
            return this;
        }

        CapabilitiesEncoder temperatures(ArsdkFeatureCamera.WhiteBalanceTemperature... temps) {
            mTemps = ArsdkFeatureCamera.WhiteBalanceTemperature.toBitField(temps);
            return this;
        }

        CapabilitiesEncoder hyperlapses(ArsdkFeatureCamera.HyperlapseValue... hyperlapses) {
            mHyperlapses = ArsdkFeatureCamera.HyperlapseValue.toBitField(hyperlapses);
            return this;
        }

        CapabilitiesEncoder bracketings(ArsdkFeatureCamera.BracketingPreset... bracketings) {
            mBracketings = ArsdkFeatureCamera.BracketingPreset.toBitField(bracketings);
            return this;
        }

        CapabilitiesEncoder bursts(ArsdkFeatureCamera.BurstValue... bursts) {
            mBursts = ArsdkFeatureCamera.BurstValue.toBitField(bursts);
            return this;
        }

        CapabilitiesEncoder minTimelapse(float interval) {
            mMinTimelapseInterval = interval;
            return this;
        }

        CapabilitiesEncoder minGpslapse(float interval) {
            mMinGpslapseInterval = interval;
            return this;
        }

        ArsdkCommand encode() {
            return ArsdkEncoder.encodeCameraCameraCapabilities(0, ArsdkFeatureCamera.Model.MAIN, mExposureModes,
                    NA, NA, mEvs, mWhiteBalanceModes, mTemps, mWhiteBalanceLockSupported, mStyles, mModes,
                    mHyperlapses, mBracketings, mBursts, 0, mMinTimelapseInterval, mMinGpslapseInterval,
                    mAutoExposureMeteringModes);
        }
    }

    private static CapabilitiesEncoder caps() {
        return new CapabilitiesEncoder();
    }

}
