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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.DevToolbox;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureDebug;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import static com.parrot.drone.groundsdk.DebugSettingMatcher.hasName;
import static com.parrot.drone.groundsdk.DebugSettingMatcher.hasRange;
import static com.parrot.drone.groundsdk.DebugSettingMatcher.hasStep;
import static com.parrot.drone.groundsdk.DebugSettingMatcher.hasValue;
import static com.parrot.drone.groundsdk.DebugSettingMatcher.isReadOnly;
import static com.parrot.drone.groundsdk.DebugSettingMatcher.isUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class DebugDevToolboxTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private DevToolbox mDevToolbox;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();

        // for this test, enable dev toolbox support
        GroundSdkConfig.get().enableDevToolboxSupport(true);
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mDevToolbox = mDrone.getPeripheralStore().get(mMockSession, DevToolbox.class);
        mDrone.getPeripheralStore().registerObserver(DevToolbox.class, () -> {
            mDevToolbox = mDrone.getPeripheralStore().get(mMockSession, DevToolbox.class);
            mChangeCnt++;
        });
    }

    @Test
    public void testPublication() {
        // should be unavailable when the drone is not connected
        assertThat(mDevToolbox, is(nullValue()));

        connectDrone(mDrone, 1, mGetAllDebugSettingsRunnable);
        assertThat(mDevToolbox, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectDrone(mDrone, 1);
        assertThat(mDevToolbox, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testDebugSettings() {
        connectDrone(mDrone, 1, mGetAllDebugSettingsRunnable);
        assertThat(mDevToolbox, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.FIRST), 1, "label1",
                        ArsdkFeatureDebug.SettingType.BOOL, ArsdkFeatureDebug.SettingMode.READ_WRITE, "", "", "", "1"));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(0, 2, "label2",
                        ArsdkFeatureDebug.SettingType.BOOL, ArsdkFeatureDebug.SettingMode.READ_ONLY, "", "", "", "0"));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(0, 3, "label3",
                        ArsdkFeatureDebug.SettingType.TEXT, ArsdkFeatureDebug.SettingMode.READ_WRITE, "", "", "",
                        "val3"));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(0, 4, "label4",
                        ArsdkFeatureDebug.SettingType.TEXT, ArsdkFeatureDebug.SettingMode.READ_ONLY, "", "", "",
                        "val4"));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(0, 5, "label5",
                        ArsdkFeatureDebug.SettingType.DECIMAL, ArsdkFeatureDebug.SettingMode.READ_WRITE, "", "", "",
                        "0.5"));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(0, 6, "label6",
                        ArsdkFeatureDebug.SettingType.DECIMAL, ArsdkFeatureDebug.SettingMode.READ_ONLY, "5", "", "6",
                        "-0.5"));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(0, 7, "label7",
                        ArsdkFeatureDebug.SettingType.DECIMAL, ArsdkFeatureDebug.SettingMode.READ_ONLY, "5", "6", "",
                        "100"));
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.LAST), 8, "label8",
                        ArsdkFeatureDebug.SettingType.DECIMAL, ArsdkFeatureDebug.SettingMode.READ_ONLY, "5", "6.5",
                        "0.5", "-100"));

        assertThat(mChangeCnt, is(2));
        assertThat(mDevToolbox.getDebugSettings(), containsInAnyOrder(
                allOf(hasName("label1"), isReadOnly(false), isUpdating(false), hasValue(true)),
                allOf(hasName("label2"), isReadOnly(true), isUpdating(false), hasValue(false)),
                allOf(hasName("label3"), isReadOnly(false), isUpdating(false), hasValue("val3")),
                allOf(hasName("label4"), isReadOnly(true), isUpdating(false), hasValue("val4")),
                allOf(hasName("label5"), isReadOnly(false), isUpdating(false), hasValue(0.5), hasStep(false),
                        hasRange(false)),
                allOf(hasName("label6"), isReadOnly(true), isUpdating(false), hasValue(-0.5), hasStep(6),
                        hasRange(false)),
                allOf(hasName("label7"), isReadOnly(true), isUpdating(false), hasValue(100), hasStep(false),
                        hasRange(5, 6)),
                allOf(hasName("label8"), isReadOnly(true), isUpdating(false), hasValue(-100), hasStep(0.5),
                        hasRange(5, 6.5))));

        // empty list of debug settings
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.EMPTY, ArsdkFeatureGeneric.ListFlags.LAST), 1, "label1",
                        ArsdkFeatureDebug.SettingType.BOOL, ArsdkFeatureDebug.SettingMode.READ_WRITE, "", "", "", "1"));
        assertThat(mChangeCnt, is(3));
        assertThat(mDevToolbox.getDebugSettings(), empty());
    }

    @Test
    public void testWritableBooleanDebugSetting() {
        connectDrone(mDrone, 1, mGetAllDebugSettingsRunnable);
        assertThat(mDevToolbox, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.FIRST, ArsdkFeatureGeneric.ListFlags.LAST), 1, "label1",
                        ArsdkFeatureDebug.SettingType.BOOL, ArsdkFeatureDebug.SettingMode.READ_WRITE, "", "", "", "1"));
        assertThat(mChangeCnt, is(2));
        assertThat(mDevToolbox.getDebugSettings(), contains(
                allOf(hasName("label1"), isReadOnly(false), isUpdating(false), hasValue(true))));

        DevToolbox.BooleanDebugSetting setting =
                mDevToolbox.getDebugSettings().get(0).as(DevToolbox.BooleanDebugSetting.class);

        // change its value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.debugSetSetting(1, "0")));
        boolean result = setting.setValue(false);
        assertThat(mChangeCnt, is(3));
        assertThat(result, is(true));
        assertThat(setting, allOf(hasName("label1"), isReadOnly(false), isUpdating(true), hasValue(false)));

        // mock answer from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDebugSettingsList(1, "0"));
        assertThat(mChangeCnt, is(4));
        assertThat(setting, allOf(hasName("label1"), isReadOnly(false), isUpdating(false), hasValue(false)));

        // change its value back to true
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.debugSetSetting(1, "1")));
        result = setting.setValue(true);
        assertThat(mChangeCnt, is(5));
        assertThat(result, is(true));
        assertThat(setting, allOf(hasName("label1"), isReadOnly(false), isUpdating(true), hasValue(true)));

        // mock answer from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDebugSettingsList(1, "1"));
        assertThat(mChangeCnt, is(6));
        assertThat(setting, allOf(hasName("label1"), isReadOnly(false), isUpdating(false), hasValue(true)));
    }

    @Test
    public void testReadOnlyBooleanDebugSetting() {
        connectDrone(mDrone, 1, mGetAllDebugSettingsRunnable);
        assertThat(mDevToolbox, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.FIRST, ArsdkFeatureGeneric.ListFlags.LAST), 1, "label1",
                        ArsdkFeatureDebug.SettingType.BOOL, ArsdkFeatureDebug.SettingMode.READ_ONLY, "", "", "", "1"));
        assertThat(mChangeCnt, is(2));
        assertThat(mDevToolbox.getDebugSettings(), contains(
                allOf(hasName("label1"), isReadOnly(true), isUpdating(false), hasValue(true))));

        DevToolbox.BooleanDebugSetting setting =
                mDevToolbox.getDebugSettings().get(0).as(DevToolbox.BooleanDebugSetting.class);

        // try to change its value
        boolean result = setting.setValue(false);
        assertThat(mChangeCnt, is(2));
        assertThat(result, is(false));
        assertThat(setting, allOf(hasName("label1"), isReadOnly(true), isUpdating(false), hasValue(true)));
    }

    @Test
    public void testWritableTextDebugSetting() {
        connectDrone(mDrone, 1, mGetAllDebugSettingsRunnable);
        assertThat(mDevToolbox, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.FIRST, ArsdkFeatureGeneric.ListFlags.LAST), 1, "label1",
                        ArsdkFeatureDebug.SettingType.TEXT, ArsdkFeatureDebug.SettingMode.READ_WRITE, "", "", "",
                        "val"));
        assertThat(mChangeCnt, is(2));
        assertThat(mDevToolbox.getDebugSettings(), contains(
                allOf(hasName("label1"), isReadOnly(false), isUpdating(false), hasValue("val"))));

        DevToolbox.TextDebugSetting setting =
                mDevToolbox.getDebugSettings().get(0).as(DevToolbox.TextDebugSetting.class);

        // change its value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.debugSetSetting(1, "newVal")));
        boolean result = setting.setValue("newVal");
        assertThat(mChangeCnt, is(3));
        assertThat(result, is(true));
        assertThat(setting, allOf(hasName("label1"), isReadOnly(false), isUpdating(true), hasValue("newVal")));

        // mock answer from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDebugSettingsList(1, "newVal"));
        assertThat(mChangeCnt, is(4));
        assertThat(setting, allOf(hasName("label1"), isReadOnly(false), isUpdating(false), hasValue("newVal")));
    }

    @Test
    public void testReadOnlyTextDebugSetting() {
        connectDrone(mDrone, 1, mGetAllDebugSettingsRunnable);
        assertThat(mDevToolbox, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.FIRST, ArsdkFeatureGeneric.ListFlags.LAST), 1, "label1",
                        ArsdkFeatureDebug.SettingType.TEXT, ArsdkFeatureDebug.SettingMode.READ_ONLY, "", "", "",
                        "val"));
        assertThat(mChangeCnt, is(2));
        assertThat(mDevToolbox.getDebugSettings(), contains(
                allOf(hasName("label1"), isReadOnly(true), isUpdating(false), hasValue("val"))));

        DevToolbox.TextDebugSetting setting =
                mDevToolbox.getDebugSettings().get(0).as(DevToolbox.TextDebugSetting.class);

        // try to change its value
        boolean result = setting.setValue("newVal");
        assertThat(mChangeCnt, is(2));
        assertThat(result, is(false));
        assertThat(setting, allOf(hasName("label1"), isReadOnly(true), isUpdating(false), hasValue("val")));
    }

    @Test
    public void testWritableNumericDebugSetting() {
        connectDrone(mDrone, 1, mGetAllDebugSettingsRunnable);
        assertThat(mDevToolbox, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.FIRST, ArsdkFeatureGeneric.ListFlags.LAST), 1, "label1",
                        ArsdkFeatureDebug.SettingType.DECIMAL, ArsdkFeatureDebug.SettingMode.READ_WRITE, "", "", "",
                        "0"));
        assertThat(mChangeCnt, is(2));
        assertThat(mDevToolbox.getDebugSettings(), contains(
                allOf(hasName("label1"), isReadOnly(false), isUpdating(false), hasValue(0))));

        DevToolbox.NumericDebugSetting setting =
                mDevToolbox.getDebugSettings().get(0).as(DevToolbox.NumericDebugSetting.class);

        // change its value
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.debugSetSetting(1, "2.65")));
        boolean result = setting.setValue(2.65);
        assertThat(mChangeCnt, is(3));
        assertThat(result, is(true));
        assertThat(setting, allOf(hasName("label1"), isReadOnly(false), isUpdating(true), hasValue(2.65)));

        // mock answer from low-level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDebugSettingsList(1, "2.65"));
        assertThat(mChangeCnt, is(4));
        assertThat(setting, allOf(hasName("label1"), isReadOnly(false), isUpdating(false), hasValue(2.65)));
    }

    @Test
    public void testReadOnlyNumericDebugSetting() {
        connectDrone(mDrone, 1, mGetAllDebugSettingsRunnable);
        assertThat(mDevToolbox, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeDebugSettingsInfo(ArsdkFeatureGeneric.ListFlags.toBitField(
                        ArsdkFeatureGeneric.ListFlags.FIRST, ArsdkFeatureGeneric.ListFlags.LAST), 1, "label1",
                        ArsdkFeatureDebug.SettingType.DECIMAL, ArsdkFeatureDebug.SettingMode.READ_ONLY, "", "", "",
                        "0"));
        assertThat(mChangeCnt, is(2));
        assertThat(mDevToolbox.getDebugSettings(), contains(
                allOf(hasName("label1"), isReadOnly(true), isUpdating(false), hasValue(0))));

        DevToolbox.NumericDebugSetting setting =
                mDevToolbox.getDebugSettings().get(0).as(DevToolbox.NumericDebugSetting.class);

        // try to change its value
        boolean result = setting.setValue(2.65);
        assertThat(mChangeCnt, is(2));
        assertThat(result, is(false));
        assertThat(setting, allOf(hasName("label1"), isReadOnly(true), isUpdating(false), hasValue(0)));
    }

    @Test
    public void testDebugTag() {
        connectDrone(mDrone, 1, mGetAllDebugSettingsRunnable);
        assertThat(mChangeCnt, is(1));
        assertThat(mDevToolbox, is(notNullValue()));
        assertThat(mDevToolbox.getLatestDebugTagId(), is(nullValue()));

        // send debug tag
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.debugTag("debug tag")));
        mDevToolbox.sendDebugTag("debug tag");
        assertThat(mChangeCnt, is(1));
        assertThat(mDevToolbox.getLatestDebugTagId(), is(nullValue()));

        // mock debug tag id notification
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDebugTagNotify("debugTagId"));
        assertThat(mChangeCnt, is(2));
        assertThat(mDevToolbox.getLatestDebugTagId(), is("debugTagId"));

        // mock same debug tag id notification
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDebugTagNotify("debugTagId"));
        assertThat(mChangeCnt, is(2));
        assertThat(mDevToolbox.getLatestDebugTagId(), is("debugTagId"));
    }

    private final Runnable mGetAllDebugSettingsRunnable = () -> mMockArsdkCore.expect(
            new Expectation.Command(1, ExpectedCmd.debugGetAllSettings(), false));
}
