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

import com.parrot.drone.groundsdk.device.peripheral.Leds;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabling;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class LedsTest {

    private MockComponentStore<Peripheral> mStore;

    private LedsCore mLedsImpl;

    private Leds mLeds;

    private Backend mBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mLedsImpl = new LedsCore(mStore, mBackend);
        mLeds = mStore.get(Leds.class);
        mStore.registerObserver(Leds.class, () -> {
            mLeds = mStore.get(Leds.class);
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
        assertThat(mLeds, nullValue());

        mLedsImpl.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mLeds, is(mLedsImpl));

        mLedsImpl.unpublish();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mLeds, nullValue());
    }

    @Test
    public void testState() {
        mLedsImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mLeds.state(), booleanSettingIsDisabled());

        // change setting from the api
        mLeds.state().toggle();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mEnabled, is(true));
        assertThat(mLeds.state(), booleanSettingIsEnabling());

        // mock update from backend
        mLedsImpl.state().updateValue(true);
        mLedsImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mLeds.state(), booleanSettingIsEnabled());
    }

    @Test
    public void testCancelRollbacks() {
        mLedsImpl.state().updateValue(true);
        mLedsImpl.publish();

        assertThat(mLeds.state(), booleanSettingIsEnabled());

        // mock user changes settings
        mLeds.state().toggle();

        // cancel all rollbacks
        mLedsImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mLeds.state(), booleanSettingIsDisabled());

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mLeds.state(), booleanSettingIsDisabled());
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements LedsCore.Backend {

        private boolean mEnabled;

        @Override
        public boolean setState(boolean enabled) {
            mEnabled = enabled;
            return true;
        }
    }
}
