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

import com.parrot.drone.groundsdk.device.peripheral.AntiFlicker;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
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

public class AntiFlickerTest {

    private MockComponentStore<Peripheral> mStore;

    private AntiFlickerCore mAntiFlickerImpl;

    private AntiFlicker mAntiFlicker;

    private AntiFlickerCore.Backend mMockBackend;

    private int mComponentChangeCnt;

    @Before
    public void setup() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mMockBackend = mock(AntiFlickerCore.Backend.class);
        mAntiFlickerImpl = new AntiFlickerCore(mStore, mMockBackend);
        mAntiFlicker = mStore.get(AntiFlicker.class);
        mStore.registerObserver(AntiFlicker.class, () -> {
            mComponentChangeCnt++;
            mAntiFlicker = mStore.get(AntiFlicker.class);
        });
        mComponentChangeCnt = 0;
    }

    @After
    public void teardown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mAntiFlicker, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mAntiFlickerImpl.publish();
        assertThat(mAntiFlicker, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mAntiFlickerImpl.unpublish();
        assertThat(mAntiFlicker, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testMode() {
        mAntiFlickerImpl.publish();

        // test default values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mAntiFlicker.mode(), allOf(
                enumSettingSupports(EnumSet.noneOf(AntiFlicker.Mode.class)),
                settingIsUpToDate()));

        // mock available modes
        mAntiFlickerImpl.mode().updateAvailableValues(EnumSet.allOf(AntiFlicker.Mode.class));

        assertThat(mAntiFlicker.mode(), allOf(
                enumSettingValueIs(AntiFlicker.Mode.OFF),
                settingIsUpToDate()));

        // test nothing changes if low-level denies
        doReturn(false).when(mMockBackend).setMode(any());
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.AUTO);

        assertThat(mComponentChangeCnt, is(1));
        verify(mMockBackend, times(1)).setMode(AntiFlicker.Mode.AUTO);
        assertThat(mAntiFlicker.mode(), allOf(
                enumSettingValueIs(AntiFlicker.Mode.OFF),
                settingIsUpToDate()));

        // test nothing changes if values do not change
        doReturn(true).when(mMockBackend).setMode(any());
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.OFF);

        assertThat(mComponentChangeCnt, is(1));
        verify(mMockBackend, never()).setMode(AntiFlicker.Mode.OFF);
        assertThat(mAntiFlicker.mode(), allOf(
                enumSettingValueIs(AntiFlicker.Mode.OFF),
                settingIsUpToDate()));

        // test user changes value
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.AUTO);

        assertThat(mComponentChangeCnt, is(2));
        verify(mMockBackend, times(2)).setMode(AntiFlicker.Mode.AUTO);
        assertThat(mAntiFlicker.mode(), allOf(
                enumSettingValueIs(AntiFlicker.Mode.AUTO),
                settingIsUpdating()));

        // mock update from low-level
        mAntiFlickerImpl.mode().updateValue(AntiFlicker.Mode.AUTO);

        // verify changes are not published until notifyUpdated() is called
        assertThat(mComponentChangeCnt, is((2)));

        mAntiFlickerImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mAntiFlicker.mode(), allOf(
                enumSettingValueIs(AntiFlicker.Mode.AUTO),
                settingIsUpToDate()));
    }

    @Test
    public void testValue() {
        mAntiFlickerImpl.publish();

        // test default values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.UNKNOWN));

        // mock update from low-level
        mAntiFlickerImpl.updateValue(AntiFlicker.Value.HZ_50);

        // verify changes are not published until notifyUpdated() is called
        assertThat(mComponentChangeCnt, is((1)));

        mAntiFlickerImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_50));

        // mock same value from low-level
        mAntiFlickerImpl.updateValue(AntiFlicker.Value.HZ_50).notifyUpdated();

        // nothing should change
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_50));
    }

    @Test
    public void testCancelRollbacks() {
        doReturn(true).when(mMockBackend).setMode(any());
        mAntiFlickerImpl.mode().updateAvailableValues(EnumSet.allOf(AntiFlicker.Mode.class));
        mAntiFlickerImpl.publish();

        assertThat(mAntiFlicker.mode(), allOf(
                enumSettingValueIs(AntiFlicker.Mode.OFF),
                settingIsUpToDate()));

        // mock user changes settings
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.AUTO);

        // cancel all rollbacks
        mAntiFlickerImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mAntiFlicker.mode(), allOf(
                enumSettingValueIs(AntiFlicker.Mode.AUTO),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mAntiFlicker.mode(), allOf(
                enumSettingValueIs(AntiFlicker.Mode.AUTO),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }
}
