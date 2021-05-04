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

import com.parrot.drone.groundsdk.device.peripheral.DevToolbox;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import static com.parrot.drone.groundsdk.DebugSettingMatcher.hasName;
import static com.parrot.drone.groundsdk.DebugSettingMatcher.hasRange;
import static com.parrot.drone.groundsdk.DebugSettingMatcher.hasStep;
import static com.parrot.drone.groundsdk.DebugSettingMatcher.hasValue;
import static com.parrot.drone.groundsdk.DebugSettingMatcher.isReadOnly;
import static com.parrot.drone.groundsdk.DebugSettingMatcher.isUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class DevToolboxTest {

    private MockComponentStore<Peripheral> mStore;

    private DevToolboxCore mDevToolbox;

    private Backend mBackend;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
    }

    @After
    public void teardown() {
        mBackend = null;
        mStore = null;
    }

    @Test
    public void testPublication() {
        mDevToolbox = new DevToolboxCore(mStore, mBackend);
        mDevToolbox.publish();
        assertThat(mDevToolbox, is(mStore.get(DevToolbox.class)));
        mDevToolbox.unpublish();
        assertThat(mStore.get(DevToolbox.class), nullValue());
    }

    @Test
    public void testDebugSettings() {
        mDevToolbox = new DevToolboxCore(mStore, mBackend);
        mDevToolbox.publish();
        DevToolbox devToolbox = mStore.get(DevToolbox.class);
        assert devToolbox != null;
        int[] cnt = new int[1];
        mStore.registerObserver(DevToolbox.class, () -> cnt[0]++);

        assertThat(cnt[0], is(0));
        assertThat(devToolbox.getDebugSettings(), Matchers.empty());

        // mock update of the debug settings from low level
        List<DevToolbox.DebugSetting> debugSettings = new ArrayList<>();
        debugSettings.add(mDevToolbox.createDebugSetting(1, "1", false, false));
        debugSettings.add(mDevToolbox.createDebugSetting(2, "2", false, "val"));
        debugSettings.add(mDevToolbox.createDebugSetting(3, "3", false, 5.5, true, 5, 6, 7));
        mDevToolbox.updateDebugSettings(debugSettings).notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(devToolbox.getDebugSettings(), contains(
                allOf(hasName("1"), isReadOnly(false), hasValue(false)),
                allOf(hasName("2"), isReadOnly(false), hasValue("val")),
                allOf(hasName("3"), isReadOnly(false), hasValue(5.5))
        ));
    }

    @Test
    public void testWritableBooleanDebugSetting() {
        mDevToolbox = new DevToolboxCore(mStore, mBackend);
        mDevToolbox.publish();
        DevToolbox devToolbox = mStore.get(DevToolbox.class);
        assert devToolbox != null;
        int[] cnt = new int[1];
        mStore.registerObserver(DevToolbox.class, () -> cnt[0]++);

        assertThat(cnt[0], is(0));
        assertThat(devToolbox.getDebugSettings(), Matchers.empty());

        // mock update of the debug settings from low level
        List<DevToolbox.DebugSetting> debugSettings = new ArrayList<>();
        debugSettings.add(mDevToolbox.createDebugSetting(1, "1", false, false));
        mDevToolbox.updateDebugSettings(debugSettings).notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(devToolbox.getDebugSettings(), contains(
                allOf(hasName("1"), isReadOnly(false), isUpdating(false), hasValue(false))));

        assertThat(mBackend.mSetting, is(nullValue()));

        DevToolboxCore.BooleanDebugSettingCore setting =
                devToolbox.getDebugSettings().get(0).as(DevToolboxCore.BooleanDebugSettingCore.class);
        boolean result = setting.setValue(true);
        assertThat(cnt[0], is(2));
        assertThat(result, is(true));
        assertThat(mBackend.mSetting == setting, is(true));
        assertThat(setting, allOf(hasName("1"), isReadOnly(false), isUpdating(true), hasValue(true)));

        // mock update from low-level
        mDevToolbox.updateDebugSettingValue(setting, true).notifyUpdated();
        assertThat(cnt[0], is(3));
        assertThat(setting, allOf(hasName("1"), isReadOnly(false), isUpdating(false), hasValue(true)));
    }

    @Test
    public void testReadOnlyBooleanDebugSetting() {
        mDevToolbox = new DevToolboxCore(mStore, mBackend);
        mDevToolbox.publish();
        DevToolbox devToolbox = mStore.get(DevToolbox.class);
        assert devToolbox != null;
        int[] cnt = new int[1];
        mStore.registerObserver(DevToolbox.class, () -> cnt[0]++);

        assertThat(cnt[0], is(0));
        assertThat(devToolbox.getDebugSettings(), Matchers.empty());

        // mock update of the debug settings from low level
        List<DevToolbox.DebugSetting> debugSettings = new ArrayList<>();
        debugSettings.add(mDevToolbox.createDebugSetting(1, "1", true, false));
        mDevToolbox.updateDebugSettings(debugSettings).notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(devToolbox.getDebugSettings(), contains(
                allOf(hasName("1"), isReadOnly(true), isUpdating(false), hasValue(false))));

        assertThat(mBackend.mSetting, is(nullValue()));

        DevToolboxCore.BooleanDebugSettingCore setting =
                devToolbox.getDebugSettings().get(0).as(DevToolboxCore.BooleanDebugSettingCore.class);
        boolean result = setting.setValue(true);
        assertThat(cnt[0], is(1));
        assertThat(result, is(false));
        assertThat(mBackend.mSetting, is(nullValue()));
        assertThat(setting, allOf(hasName("1"), isReadOnly(true), isUpdating(false), hasValue(false)));
    }

    @Test
    public void testWritableTextDebugSetting() {
        mDevToolbox = new DevToolboxCore(mStore, mBackend);
        mDevToolbox.publish();
        DevToolbox devToolbox = mStore.get(DevToolbox.class);
        assert devToolbox != null;
        int[] cnt = new int[1];
        mStore.registerObserver(DevToolbox.class, () -> cnt[0]++);

        assertThat(cnt[0], is(0));
        assertThat(devToolbox.getDebugSettings(), Matchers.empty());

        // mock update of the debug settings from low level
        List<DevToolbox.DebugSetting> debugSettings = new ArrayList<>();
        debugSettings.add(mDevToolbox.createDebugSetting(1, "1", false, "val"));
        mDevToolbox.updateDebugSettings(debugSettings).notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(devToolbox.getDebugSettings(), contains(
                allOf(hasName("1"), isReadOnly(false), isUpdating(false), hasValue("val"))));

        assertThat(mBackend.mSetting, is(nullValue()));

        DevToolboxCore.TextDebugSettingCore setting =
                devToolbox.getDebugSettings().get(0).as(DevToolboxCore.TextDebugSettingCore.class);
        boolean result = setting.setValue("newVal");
        assertThat(cnt[0], is(2));
        assertThat(result, is(true));
        assertThat(mBackend.mSetting == setting, is(true));
        assertThat(setting, allOf(hasName("1"), isReadOnly(false), isUpdating(true), hasValue("newVal")));

        // mock update from low-level
        mDevToolbox.updateDebugSettingValue(setting, "newVal").notifyUpdated();
        assertThat(cnt[0], is(3));
        assertThat(setting, allOf(hasName("1"), isReadOnly(false), isUpdating(false), hasValue("newVal")));
    }

    @Test
    public void testReadOnlyTextDebugSetting() {
        mDevToolbox = new DevToolboxCore(mStore, mBackend);
        mDevToolbox.publish();
        DevToolbox devToolbox = mStore.get(DevToolbox.class);
        assert devToolbox != null;
        int[] cnt = new int[1];
        mStore.registerObserver(DevToolbox.class, () -> cnt[0]++);

        assertThat(cnt[0], is(0));
        assertThat(devToolbox.getDebugSettings(), Matchers.empty());

        // mock update of the debug settings from low level
        List<DevToolbox.DebugSetting> debugSettings = new ArrayList<>();
        debugSettings.add(mDevToolbox.createDebugSetting(1, "1", true, "val"));
        mDevToolbox.updateDebugSettings(debugSettings).notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(devToolbox.getDebugSettings(), contains(
                allOf(hasName("1"), isReadOnly(true), isUpdating(false), hasValue("val"))));

        assertThat(mBackend.mSetting, is(nullValue()));

        DevToolboxCore.TextDebugSettingCore setting =
                devToolbox.getDebugSettings().get(0).as(DevToolboxCore.TextDebugSettingCore.class);
        boolean result = setting.setValue("newVal");
        assertThat(cnt[0], is(1));
        assertThat(result, is(false));
        assertThat(mBackend.mSetting, is(nullValue()));
        assertThat(setting, allOf(hasName("1"), isReadOnly(true), isUpdating(false), hasValue("val")));
    }

    @Test
    public void testWritableNumericDebugSetting() {
        mDevToolbox = new DevToolboxCore(mStore, mBackend);
        mDevToolbox.publish();
        DevToolbox devToolbox = mStore.get(DevToolbox.class);
        assert devToolbox != null;
        int[] cnt = new int[1];
        mStore.registerObserver(DevToolbox.class, () -> cnt[0]++);

        assertThat(cnt[0], is(0));
        assertThat(devToolbox.getDebugSettings(), Matchers.empty());

        // mock update of the debug settings from low level
        List<DevToolbox.DebugSetting> debugSettings = new ArrayList<>();
        debugSettings.add(mDevToolbox.createDebugSetting(1, "1", false, 10, false, 0, 0, -1));
        mDevToolbox.updateDebugSettings(debugSettings).notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(devToolbox.getDebugSettings(), contains(allOf(hasName("1"), isReadOnly(false), isUpdating(false),
                hasValue(10), hasRange(false), hasStep(false))));

        assertThat(mBackend.mSetting, is(nullValue()));

        DevToolboxCore.NumericDebugSettingCore setting =
                devToolbox.getDebugSettings().get(0).as(DevToolboxCore.NumericDebugSettingCore.class);
        boolean result = setting.setValue(-10);
        assertThat(cnt[0], is(2));
        assertThat(result, is(true));
        assertThat(mBackend.mSetting == setting, is(true));
        assertThat(setting, allOf(
                hasName("1"), isReadOnly(false), isUpdating(true), hasValue(-10), hasRange(false), hasStep(false)));

        // mock update from low-level
        mDevToolbox.updateDebugSettingValue(setting, -10).notifyUpdated();
        assertThat(cnt[0], is(3));
        assertThat(setting, allOf(
                hasName("1"), isReadOnly(false), isUpdating(false), hasValue(-10), hasRange(false), hasStep(false)));
    }

    @Test
    public void testReadOnlyNumericDebugSetting() {
        mDevToolbox = new DevToolboxCore(mStore, mBackend);
        mDevToolbox.publish();
        DevToolbox devToolbox = mStore.get(DevToolbox.class);
        assert devToolbox != null;
        int[] cnt = new int[1];
        mStore.registerObserver(DevToolbox.class, () -> cnt[0]++);

        assertThat(cnt[0], is(0));
        assertThat(devToolbox.getDebugSettings(), Matchers.empty());

        // mock update of the debug settings from low level
        List<DevToolbox.DebugSetting> debugSettings = new ArrayList<>();
        debugSettings.add(mDevToolbox.createDebugSetting(1, "1", true, 10, true, -20, 20, 1));
        mDevToolbox.updateDebugSettings(debugSettings).notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(devToolbox.getDebugSettings(), contains(
                allOf(hasName("1"), isReadOnly(true), isUpdating(false), hasValue(10), hasRange(-20, 20), hasStep(1))));

        assertThat(mBackend.mSetting, is(nullValue()));

        DevToolboxCore.NumericDebugSettingCore setting =
                devToolbox.getDebugSettings().get(0).as(DevToolboxCore.NumericDebugSettingCore.class);
        boolean result = setting.setValue(-10);
        assertThat(cnt[0], is(1));
        assertThat(result, is(false));
        assertThat(mBackend.mSetting, is(nullValue()));
        assertThat(setting,
                allOf(hasName("1"), isReadOnly(true), isUpdating(false), hasValue(10), hasRange(-20, 20), hasStep(1)));
    }

    @Test
    public void testDebugTag() {
        mDevToolbox = new DevToolboxCore(mStore, mBackend);
        mDevToolbox.publish();
        DevToolbox devToolbox = mStore.get(DevToolbox.class);
        assert devToolbox != null;
        int[] cnt = new int[1];
        mStore.registerObserver(DevToolbox.class, () -> cnt[0]++);

        assertThat(cnt[0], is(0));
        assertThat(devToolbox.getLatestDebugTagId(), is(nullValue()));
        assertThat(mBackend.mDebugTag, is(nullValue()));

        // send debug tag
        devToolbox.sendDebugTag("debug tag");
        assertThat(cnt[0], is(0));
        assertThat(devToolbox.getLatestDebugTagId(), is(nullValue()));
        assertThat(mBackend.mDebugTag, is("debug tag"));

        // mock debug tag id update from low level
        mDevToolbox.updateDebugTagId("debugTagId").notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(devToolbox.getLatestDebugTagId(), is("debugTagId"));

        // mock debug tag id update from low level with same id, nothing should change
        mDevToolbox.updateDebugTagId("debugTagId").notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(devToolbox.getLatestDebugTagId(), is("debugTagId"));
    }

    private static final class Backend implements DevToolboxCore.Backend {

        private DevToolboxCore.DebugSettingCore mSetting;

        private String mDebugTag;

        @Override
        public void updateDebugSetting(DevToolboxCore.DebugSettingCore setting) {
            mSetting = setting;
        }

        @Override
        public void sendDebugTag(@NonNull String tag) {
            mDebugTag = tag;
        }
    }
}
