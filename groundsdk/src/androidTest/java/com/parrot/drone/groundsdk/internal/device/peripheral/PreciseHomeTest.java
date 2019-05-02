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
import com.parrot.drone.groundsdk.device.peripheral.PreciseHome;
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

public class PreciseHomeTest {

    private MockComponentStore<Peripheral> mStore;

    private PreciseHomeCore mPreciseHomeImpl;

    private PreciseHome mPreciseHome;

    private PreciseHomeCore.Backend mMockBackend;

    private int mComponentChangeCnt;

    @Before
    public void setup() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mMockBackend = mock(PreciseHomeCore.Backend.class);
        mPreciseHomeImpl = new PreciseHomeCore(mStore, mMockBackend);
        mPreciseHome = mStore.get(PreciseHome.class);
        mStore.registerObserver(PreciseHome.class, () -> {
            mComponentChangeCnt++;
            mPreciseHome = mStore.get(PreciseHome.class);
        });
        mComponentChangeCnt = 0;
    }

    @After
    public void teardown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mPreciseHome, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mPreciseHomeImpl.publish();
        assertThat(mPreciseHome, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mPreciseHomeImpl.unpublish();
        assertThat(mPreciseHome, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testMode() {
        mPreciseHomeImpl.publish();

        verifyZeroInteractions(mMockBackend);

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingSupports(EnumSet.noneOf(PreciseHome.Mode.class)),
                settingIsUpToDate()));

        // mock low-level sends supported modes and current mode
        mPreciseHomeImpl.mode()
                        .updateAvailableValues(EnumSet.allOf(PreciseHome.Mode.class))
                        .updateValue(PreciseHome.Mode.DISABLED);
        mPreciseHomeImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(PreciseHome.Mode.class)),
                enumSettingValueIs(PreciseHome.Mode.DISABLED),
                settingIsUpToDate()));

        // test nothing changes if low-level denies
        doReturn(false).when(mMockBackend).setMode(any());
        mPreciseHome.mode().setValue(PreciseHome.Mode.STANDARD);

        verify(mMockBackend, times(1)).setMode(PreciseHome.Mode.STANDARD);
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(PreciseHome.Mode.class)),
                enumSettingValueIs(PreciseHome.Mode.DISABLED),
                settingIsUpToDate()));

        // test nothing changes if values do not change
        doReturn(true).when(mMockBackend).setMode(any());
        mPreciseHome.mode().setValue(PreciseHome.Mode.DISABLED);

        verify(mMockBackend, never()).setMode(PreciseHome.Mode.DISABLED);
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(PreciseHome.Mode.class)),
                enumSettingValueIs(PreciseHome.Mode.DISABLED),
                settingIsUpToDate()));

        // test user changes value
        mPreciseHome.mode().setValue(PreciseHome.Mode.STANDARD);

        verify(mMockBackend, times(2)).setMode(PreciseHome.Mode.STANDARD);
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(PreciseHome.Mode.class)),
                enumSettingValueIs(PreciseHome.Mode.STANDARD),
                settingIsUpdating()));

        // mock update from low-level
        mPreciseHomeImpl.mode().updateValue(PreciseHome.Mode.STANDARD);

        // verify changes are not published until notifyUpdated() is called
        assertThat(mComponentChangeCnt, is((3)));

        mPreciseHomeImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(PreciseHome.Mode.class)),
                enumSettingValueIs(PreciseHome.Mode.STANDARD),
                settingIsUpToDate()));

        verifyNoMoreInteractions(mMockBackend);
    }

    @Test
    public void testState() {
        mPreciseHomeImpl.publish();

        // test default values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mPreciseHome.state(), is(PreciseHome.State.UNAVAILABLE));

        // mock update from low-level
        mPreciseHomeImpl.updateState(PreciseHome.State.AVAILABLE);

        // verify changes are not published until notifyUpdated() is called
        assertThat(mComponentChangeCnt, is((1)));

        mPreciseHomeImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mPreciseHome.state(), is(PreciseHome.State.AVAILABLE));

        // mock same value from low-level
        mPreciseHomeImpl.updateState(PreciseHome.State.AVAILABLE).notifyUpdated();

        // nothing should change
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mPreciseHome.state(), is(PreciseHome.State.AVAILABLE));
    }

    @Test
    public void testCancelRollbacks() {
        doReturn(true).when(mMockBackend).setMode(any());
        mPreciseHomeImpl.mode()
                        .updateAvailableValues(EnumSet.allOf(PreciseHome.Mode.class))
                        .updateValue(PreciseHome.Mode.DISABLED);
        mPreciseHomeImpl.publish();

        assertThat(mPreciseHome.mode(), allOf(
                enumSettingValueIs(PreciseHome.Mode.DISABLED),
                settingIsUpToDate()));

        // mock user changes settings
        mPreciseHome.mode().setValue(PreciseHome.Mode.STANDARD);

        // cancel all rollbacks
        mPreciseHomeImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingValueIs(PreciseHome.Mode.STANDARD),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingValueIs(PreciseHome.Mode.STANDARD),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }
}
