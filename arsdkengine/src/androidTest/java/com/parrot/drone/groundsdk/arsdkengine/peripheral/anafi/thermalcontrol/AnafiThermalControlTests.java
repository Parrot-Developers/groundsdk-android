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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.thermalcontrol;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.ThermalControl;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureThermal;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingSupports;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class AnafiThermalControlTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private ThermalControl mThermal;

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

        mThermal = mDrone.getPeripheralStore().get(mMockSession, ThermalControl.class);
        mDrone.getPeripheralStore().registerObserver(ThermalControl.class, () -> {
            mThermal = mDrone.getPeripheralStore().get(mMockSession, ThermalControl.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when drone has never been connected
        assertThat(mChangeCnt, is(0));
        assertThat(mThermal, nullValue());

        // connect drone
        connectDrone(mDrone, 1);

        // component should not be published if the drone does not send supported modes
        assertThat(mChangeCnt, is(0));
        assertThat(mThermal, nullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is not published
        assertThat(mChangeCnt, is(0));
        assertThat(mThermal, nullValue());

        // connect drone, receiving supported modes
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeThermalCapabilities(
                ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values()))));

        // component should be published
        assertThat(mChangeCnt, is(1));
        assertThat(mThermal, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is now persisted
        assertThat(mChangeCnt, is(1));
        assertThat(mThermal, notNullValue());

        // connect drone, not receiving supported modes
        connectDrone(mDrone, 1);

        // component should be unpublished
        assertThat(mChangeCnt, is(2));
        assertThat(mThermal, nullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is not published
        assertThat(mChangeCnt, is(2));
        assertThat(mThermal, nullValue());

        // connect drone, receiving supported modes
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeThermalCapabilities(
                ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values()))));

        // component should be published
        assertThat(mChangeCnt, is(3));
        assertThat(mThermal, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is now persisted
        assertThat(mChangeCnt, is(3));
        assertThat(mThermal, notNullValue());

        // forget drone
        mDrone.forget();

        // component should be unpublished
        assertThat(mChangeCnt, is(4));
        assertThat(mThermal, nullValue());
    }

    @Test
    public void testMode() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeThermalCapabilities(
                        ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values())),
                ArsdkEncoder.encodeThermalMode(ArsdkFeatureThermal.Mode.DISABLED)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mThermal.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.DISABLED),
                settingIsUpToDate()));

        // change mode to standard
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.thermalSetMode(ArsdkFeatureThermal.Mode.STANDARD)));
        mThermal.mode().setValue(ThermalControl.Mode.STANDARD);

        assertThat(mChangeCnt, is(2));
        assertThat(mThermal.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.STANDARD),
                settingIsUpdating()));

        // mock drone ack
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeThermalMode(
                ArsdkFeatureThermal.Mode.STANDARD));

        assertThat(mChangeCnt, is(3));
        assertThat(mThermal.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.STANDARD),
                settingIsUpToDate()));

        // change mode to embedded
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.thermalSetMode(ArsdkFeatureThermal.Mode.BLENDED)));
        mThermal.mode().setValue(ThermalControl.Mode.EMBEDDED);

        assertThat(mChangeCnt, is(4));
        assertThat(mThermal.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.EMBEDDED),
                settingIsUpdating()));

        // mock drone ack
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeThermalMode(
                ArsdkFeatureThermal.Mode.BLENDED));

        assertThat(mChangeCnt, is(5));
        assertThat(mThermal.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.EMBEDDED),
                settingIsUpToDate()));

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check still in embedded mode
        assertThat(mChangeCnt, is(0));
        assertThat(mThermal.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.EMBEDDED),
                settingIsUpToDate()));

        // change mode offline
        mThermal.mode().setValue(ThermalControl.Mode.DISABLED);

        assertThat(mChangeCnt, is(1));
        assertThat(mThermal.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.DISABLED),
                settingIsUpToDate()));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1,
                        ArsdkEncoder.encodeThermalCapabilities(
                                ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values())),
                        ArsdkEncoder.encodeThermalMode(ArsdkFeatureThermal.Mode.BLENDED))
                // disabled mode should be sent to drone
                .expect(new Expectation.Command(1, ExpectedCmd.thermalSetMode(ArsdkFeatureThermal.Mode.DISABLED))));

        mMockArsdkCore.assertNoExpectation();
        assertThat(mChangeCnt, is(1));
        assertThat(mThermal.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Mode.class)),
                enumSettingValueIs(ThermalControl.Mode.DISABLED),
                settingIsUpToDate()));
    }

    @Test
    public void testSensitivity() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeThermalCapabilities(
                        ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values())),
                ArsdkEncoder.encodeThermalSensitivity(ArsdkFeatureThermal.Range.LOW)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mThermal.sensitivity(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Sensitivity.class)),
                enumSettingValueIs(ThermalControl.Sensitivity.LOW_RANGE),
                settingIsUpToDate()));

        // change sensitivity
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.thermalSetSensitivity(ArsdkFeatureThermal.Range.HIGH)));
        mThermal.sensitivity().setValue(ThermalControl.Sensitivity.HIGH_RANGE);

        assertThat(mChangeCnt, is(2));
        assertThat(mThermal.sensitivity(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Sensitivity.class)),
                enumSettingValueIs(ThermalControl.Sensitivity.HIGH_RANGE),
                settingIsUpdating()));

        // mock drone ack
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeThermalSensitivity(
                ArsdkFeatureThermal.Range.HIGH));

        assertThat(mChangeCnt, is(3));
        assertThat(mThermal.sensitivity(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Sensitivity.class)),
                enumSettingValueIs(ThermalControl.Sensitivity.HIGH_RANGE),
                settingIsUpToDate()));

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check still in high range sensitivity
        assertThat(mChangeCnt, is(0));
        assertThat(mThermal.sensitivity(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Sensitivity.class)),
                enumSettingValueIs(ThermalControl.Sensitivity.HIGH_RANGE),
                settingIsUpToDate()));

        // change sensitivity offline
        mThermal.sensitivity().setValue(ThermalControl.Sensitivity.LOW_RANGE);

        assertThat(mChangeCnt, is(1));
        assertThat(mThermal.sensitivity(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Sensitivity.class)),
                enumSettingValueIs(ThermalControl.Sensitivity.LOW_RANGE),
                settingIsUpToDate()));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1,
                        ArsdkEncoder.encodeThermalCapabilities(
                                ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values())),
                        ArsdkEncoder.encodeThermalSensitivity(ArsdkFeatureThermal.Range.HIGH))
                // low range sensitivity should be sent to drone
                .expect(new Expectation.Command(1, ExpectedCmd.thermalSetSensitivity(ArsdkFeatureThermal.Range.LOW))));

        mMockArsdkCore.assertNoExpectation();
        assertThat(mChangeCnt, is(1));
        assertThat(mThermal.sensitivity(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Sensitivity.class)),
                enumSettingValueIs(ThermalControl.Sensitivity.LOW_RANGE),
                settingIsUpToDate()));
    }

    @Test
    public void testEmissivity() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeThermalCapabilities(
                        ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values()))));

        // change emissivity
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.thermalSetEmissivity(0.5f)));
        mThermal.sendEmissivity(0.5);

        // check that the same value is not sent twice to drone
        mThermal.sendEmissivity(0.5);

        // send another value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.thermalSetEmissivity(0.6f)));
        mThermal.sendEmissivity(0.6);

        // receive value from drone and check that this value is not sent to drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeThermalEmissivity(0.7f));
        mThermal.sendEmissivity(0.7);

        // send another value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.thermalSetEmissivity(0.6f)));
        mThermal.sendEmissivity(0.6);
    }

    @Test
    public void testCalibrationMode() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeThermalCapabilities(
                        ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values())),
                ArsdkEncoder.encodeThermalShutterMode(ArsdkFeatureThermal.ShutterTrigger.AUTO)));

        ThermalControl.Calibration calibration = mThermal.calibration();

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(calibration, notNullValue());
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.AUTOMATIC),
                settingIsUpToDate()));

        // change to manual mode
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.thermalSetShutterMode(
                ArsdkFeatureThermal.ShutterTrigger.MANUAL)));
        calibration.mode().setValue(ThermalControl.Calibration.Mode.MANUAL);

        assertThat(mChangeCnt, is(2));
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.MANUAL),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeThermalShutterMode(
                ArsdkFeatureThermal.ShutterTrigger.MANUAL));

        assertThat(mChangeCnt, is(3));
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.MANUAL),
                settingIsUpToDate()));

        // change to automatic mode
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.thermalSetShutterMode(
                ArsdkFeatureThermal.ShutterTrigger.AUTO)));
        calibration.mode().setValue(ThermalControl.Calibration.Mode.AUTOMATIC);

        assertThat(mChangeCnt, is(4));
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.AUTOMATIC),
                settingIsUpdating()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeThermalShutterMode(
                ArsdkFeatureThermal.ShutterTrigger.AUTO));

        assertThat(mChangeCnt, is(5));
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.AUTOMATIC),
                settingIsUpToDate()));

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        calibration = mThermal.calibration();
        assertThat(calibration, notNullValue());

        // check still in automatic mode
        assertThat(mChangeCnt, is(0));
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.AUTOMATIC),
                settingIsUpToDate()));

        // change calibration mode offline
        calibration.mode().setValue(ThermalControl.Calibration.Mode.MANUAL);

        assertThat(mChangeCnt, is(1));
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.MANUAL),
                settingIsUpToDate()));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1,
                        ArsdkEncoder.encodeThermalCapabilities(
                                ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values())),
                        ArsdkEncoder.encodeThermalShutterMode(ArsdkFeatureThermal.ShutterTrigger.AUTO))
                // manual calibration mode should be sent to drone
                .expect(new Expectation.Command(1, ExpectedCmd.thermalSetShutterMode(
                        ArsdkFeatureThermal.ShutterTrigger.MANUAL))));

        mMockArsdkCore.assertNoExpectation();
        assertThat(mChangeCnt, is(1));
        assertThat(calibration.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(ThermalControl.Calibration.Mode.class)),
                enumSettingValueIs(ThermalControl.Calibration.Mode.MANUAL),
                settingIsUpToDate()));
    }

    @Test
    public void testCalibrate() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeThermalCapabilities(
                        ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values())),
                ArsdkEncoder.encodeThermalShutterMode(ArsdkFeatureThermal.ShutterTrigger.AUTO)));

        ThermalControl.Calibration calibration = mThermal.calibration();

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(calibration, notNullValue());

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.thermalTriggShutter()));
        calibration.calibrate();
    }

    @Test
    public void testBackgroundTemperature() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeThermalCapabilities(
                        ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values()))));

        // change background temperature
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.thermalSetBackgroundTemperature(123.4f)));
        mThermal.sendBackgroundTemperature(123.4);

        // check that the same value is not sent twice to drone
        mThermal.sendBackgroundTemperature(123.4);

        // send another value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.thermalSetBackgroundTemperature(456.7f)));
        mThermal.sendBackgroundTemperature(456.7);

        // receive value from drone and check that this value is not sent to drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeThermalBackgroundTemperature(89.1f));
        mThermal.sendBackgroundTemperature(89.1);

        // send another value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.thermalSetBackgroundTemperature(432.1f)));
        mThermal.sendBackgroundTemperature(432.1);
    }

    @Test
    public void testPalette() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeThermalCapabilities(
                        ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values()))));

        ThermalControl.Palette.Color color1 = mock(ThermalControl.Palette.Color.class);
        doReturn(0.2).when(color1).getRed();
        doReturn(0.2).when(color1).getGreen();
        doReturn(0.9).when(color1).getBlue();
        doReturn(0.0).when(color1).getPosition();
        ThermalControl.Palette.Color color2 = mock(ThermalControl.Palette.Color.class);
        doReturn(1.0).when(color2).getRed();
        doReturn(0.2).when(color2).getGreen();
        doReturn(0.3).when(color2).getBlue();
        doReturn(0.5).when(color2).getPosition();
        ThermalControl.Palette.Color color3 = mock(ThermalControl.Palette.Color.class);
        doReturn(1.0).when(color3).getRed();
        doReturn(1.0).when(color3).getGreen();
        doReturn(1.0).when(color3).getBlue();
        doReturn(1.0).when(color3).getPosition();

        // send absolute palette
        ThermalControl.AbsolutePalette absolutePalette = mock(ThermalControl.AbsolutePalette.class);
        doReturn(Arrays.asList(color1, color2, color3)).when(absolutePalette).getColors();
        doReturn(0.2).when(absolutePalette).getLowestTemperature();
        doReturn(0.7).when(absolutePalette).getHighestTemperature();
        doReturn(ThermalControl.AbsolutePalette.ColorizationMode.LIMITED).when(absolutePalette).getColorizationMode();

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.thermalSetPalettePart(0.2f, 0.2f, 0.9f, 0f,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)),
                ExpectedCmd.thermalSetPalettePart(1.0f, 0.2f, 0.3f, 0.5f, 0),
                ExpectedCmd.thermalSetPalettePart(1.0f, 1.0f, 1.0f, 1.0f,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)),
                ExpectedCmd.thermalSetPaletteSettings(ArsdkFeatureThermal.PaletteMode.ABSOLUTE, 0.2f, 0.7f,
                        ArsdkFeatureThermal.ColorizationMode.LIMITED, ArsdkFeatureThermal.RelativeRangeMode.UNLOCKED,
                        ArsdkFeatureThermal.SpotType.HOT, 0f)));
        mThermal.sendPalette(absolutePalette);

        // check that the same palette is not sent twice to drone
        mThermal.sendPalette(absolutePalette);

        // send relative palette with only one color
        ThermalControl.RelativePalette relativePalette = mock(ThermalControl.RelativePalette.class);
        doReturn(Collections.singletonList(color1)).when(relativePalette).getColors();
        doReturn(0.0).when(relativePalette).getLowestTemperature();
        doReturn(1.0).when(relativePalette).getHighestTemperature();
        doReturn(true).when(relativePalette).isLocked();

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.thermalSetPalettePart(0.2f, 0.2f, 0.9f, 0f,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST) |
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)),
                ExpectedCmd.thermalSetPaletteSettings(ArsdkFeatureThermal.PaletteMode.RELATIVE, 0f, 1f,
                        ArsdkFeatureThermal.ColorizationMode.EXTENDED, ArsdkFeatureThermal.RelativeRangeMode.LOCKED,
                        ArsdkFeatureThermal.SpotType.HOT, 0f)));
        mThermal.sendPalette(relativePalette);

        // check that the same palette is not sent twice to drone
        mThermal.sendPalette(relativePalette);

        // send relative palette with empty color list
        ThermalControl.SpotPalette spotPalette = mock(ThermalControl.SpotPalette.class);
        doReturn(Collections.emptyList()).when(spotPalette).getColors();
        doReturn(ThermalControl.SpotPalette.SpotType.COLD).when(spotPalette).getType();
        doReturn(0.6).when(spotPalette).getThreshold();

        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.thermalSetPalettePart(0f, 0f, 0f, 0f,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.EMPTY)),
                ExpectedCmd.thermalSetPaletteSettings(ArsdkFeatureThermal.PaletteMode.SPOT, 0f, 0f,
                        ArsdkFeatureThermal.ColorizationMode.EXTENDED, ArsdkFeatureThermal.RelativeRangeMode.UNLOCKED,
                        ArsdkFeatureThermal.SpotType.COLD, 0.6f)));
        mThermal.sendPalette(spotPalette);

        // check that the same palette is not sent twice to drone
        mThermal.sendPalette(spotPalette);

        // receive colors from drone
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeThermalPalettePart(1.0f, 0.2f, 0.3f, 0.5f,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeThermalPalettePart(0.2f, 0.2f, 0.9f, 0f,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));

        // check that the same colors (as the ones given by drone) are not sent to drone
        doReturn(Arrays.asList(color1, color2)).when(spotPalette).getColors();
        mThermal.sendPalette(spotPalette);

        // check that the same colors (as the ones given by drone), in a different order, are not sent to drone
        doReturn(Arrays.asList(color2, color1)).when(spotPalette).getColors();
        mThermal.sendPalette(spotPalette);

        // send different colors
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.thermalSetPalettePart(0.2f, 0.2f, 0.9f, 0f,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)),
                ExpectedCmd.thermalSetPalettePart(1.0f, 1.0f, 1.0f, 1.0f,
                        ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST))));
        doReturn(Arrays.asList(color1, color3)).when(spotPalette).getColors();
        mThermal.sendPalette(spotPalette);

        // receive color settings from drone
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeThermalPaletteSettings(ArsdkFeatureThermal.PaletteMode.SPOT, 0f, 0f,
                        ArsdkFeatureThermal.ColorizationMode.EXTENDED, ArsdkFeatureThermal.RelativeRangeMode.UNLOCKED,
                        ArsdkFeatureThermal.SpotType.COLD, 0.7f));

        // check that the same settings (as the ones given by drone) are not sent to drone
        doReturn(0.7).when(spotPalette).getThreshold();
        mThermal.sendPalette(spotPalette);

        // send different settings
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.thermalSetPaletteSettings(ArsdkFeatureThermal.PaletteMode.SPOT, 0f, 0f,
                        ArsdkFeatureThermal.ColorizationMode.EXTENDED, ArsdkFeatureThermal.RelativeRangeMode.UNLOCKED,
                        ArsdkFeatureThermal.SpotType.COLD, 0.8f)));
        doReturn(0.8).when(spotPalette).getThreshold();
        mThermal.sendPalette(spotPalette);
    }

    @Test
    public void testRendering() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeThermalCapabilities(
                        ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values()))));

        // send rendering in visible mode
        ThermalControl.Rendering rendering = mock(ThermalControl.Rendering.class);
        doReturn(ThermalControl.Rendering.Mode.VISIBLE).when(rendering).getMode();
        doReturn(0.1).when(rendering).getBlendingRate();
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.thermalSetRendering(ArsdkFeatureThermal.RenderingMode.VISIBLE, 0.1f)));
        mThermal.sendRendering(rendering);

        // send rendering in thermal mode
        doReturn(ThermalControl.Rendering.Mode.THERMAL).when(rendering).getMode();
        doReturn(0.2).when(rendering).getBlendingRate();
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.thermalSetRendering(ArsdkFeatureThermal.RenderingMode.THERMAL, 0.2f)));
        mThermal.sendRendering(rendering);

        // send rendering in blended mode
        doReturn(ThermalControl.Rendering.Mode.BLENDED).when(rendering).getMode();
        doReturn(0.3).when(rendering).getBlendingRate();
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.thermalSetRendering(ArsdkFeatureThermal.RenderingMode.BLENDED, 0.3f)));
        mThermal.sendRendering(rendering);

        // send rendering in monochrome mode
        doReturn(ThermalControl.Rendering.Mode.MONOCHROME).when(rendering).getMode();
        doReturn(0.4).when(rendering).getBlendingRate();
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.thermalSetRendering(ArsdkFeatureThermal.RenderingMode.MONOCHROME, 0.4f)));
        mThermal.sendRendering(rendering);
    }

    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeThermalCapabilities(
                        ArsdkFeatureThermal.Mode.toBitField(ArsdkFeatureThermal.Mode.values())),
                ArsdkEncoder.encodeThermalMode(ArsdkFeatureThermal.Mode.DISABLED),
                ArsdkEncoder.encodeThermalSensitivity(ArsdkFeatureThermal.Range.LOW),
                ArsdkEncoder.encodeThermalShutterMode(ArsdkFeatureThermal.ShutterTrigger.AUTO)));

        ThermalControl.Calibration calibration = mThermal.calibration();
        assertThat(calibration, notNullValue());

        assertThat(mChangeCnt, is(1));
        assertThat(mThermal.mode(), allOf(
                enumSettingValueIs(ThermalControl.Mode.DISABLED),
                settingIsUpToDate()));
        assertThat(mThermal.sensitivity(), allOf(
                enumSettingValueIs(ThermalControl.Sensitivity.LOW_RANGE),
                settingIsUpToDate()));
        assertThat(calibration.mode(), allOf(
                enumSettingValueIs(ThermalControl.Calibration.Mode.AUTOMATIC),
                settingIsUpToDate()));

        // mock user modifies settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.thermalSetMode(
                ArsdkFeatureThermal.Mode.STANDARD)));
        mThermal.mode().setValue(ThermalControl.Mode.STANDARD);

        assertThat(mChangeCnt, is(2));
        assertThat(mThermal.mode(), allOf(
                enumSettingValueIs(ThermalControl.Mode.STANDARD),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.thermalSetSensitivity(
                ArsdkFeatureThermal.Range.HIGH)));
        mThermal.sensitivity().setValue(ThermalControl.Sensitivity.HIGH_RANGE);

        assertThat(mChangeCnt, is(3));
        assertThat(mThermal.sensitivity(), allOf(
                enumSettingValueIs(ThermalControl.Sensitivity.HIGH_RANGE),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.thermalSetShutterMode(
                ArsdkFeatureThermal.ShutterTrigger.MANUAL)));
        calibration.mode().setValue(ThermalControl.Calibration.Mode.MANUAL);

        assertThat(mChangeCnt, is(4));
        assertThat(calibration.mode(), allOf(
                enumSettingValueIs(ThermalControl.Calibration.Mode.MANUAL),
                settingIsUpdating()));

        // disconnect
        disconnectDrone(mDrone, 1);

        // setting should be updated to user value
        assertThat(mChangeCnt, is(5));

        assertThat(mThermal.mode(), allOf(
                enumSettingValueIs(ThermalControl.Mode.STANDARD),
                settingIsUpToDate()));

        assertThat(mThermal.sensitivity(), allOf(
                enumSettingValueIs(ThermalControl.Sensitivity.HIGH_RANGE),
                settingIsUpToDate()));

        assertThat(calibration.mode(), allOf(
                enumSettingValueIs(ThermalControl.Calibration.Mode.MANUAL),
                settingIsUpToDate()));
    }
}
