/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

import com.parrot.drone.groundsdk.DroneIdMatcher;
import com.parrot.drone.groundsdk.device.peripheral.Dri;
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
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingValueIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class DriTest {
    private MockComponentStore<Peripheral> mStore;

    private DriCore mDriImpl;

    private Dri mDri;

    private Backend mMockBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mMockBackend = new Backend();
        mDriImpl = new DriCore(mStore, mMockBackend);
        mDri = mStore.get(Dri.class);
        mStore.registerObserver(Dri.class, () -> {
            mComponentChangeCnt++;
            mDri = mStore.get(Dri.class);
        });
        mComponentChangeCnt = 0;
    }

    @After
    public void tearDown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mDri, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mDriImpl.publish();

        assertThat(mDri, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mDriImpl.unpublish();

        assertThat(mDri, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testState() {
        mDriImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mDri.state(), booleanSettingIsDisabled());

        // change setting from the api
        mDri.state().toggle();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mMockBackend.mEnabled, is(true));
        assertThat(mDri.state(), booleanSettingIsEnabling());

        // mock update from backend
        mDriImpl.state().updateValue(true);
        mDriImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mDri.state(), booleanSettingIsEnabled());
    }

    @Test
    public void testDroneId() {
        mDriImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mDri.getDroneId(), nullValue());

        // mock update from backend
        mDriImpl.updateDroneId(new DriCore.Id(Dri.IdType.ANSI_CTA_2063, "ANSIId")).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.ANSI_CTA_2063, "ANSIId"));

        // mock update from backend
        mDriImpl.updateDroneId(new DriCore.Id(Dri.IdType.FR_30_OCTETS, "ANSIId")).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.FR_30_OCTETS, "ANSIId"));

        // mock update from backend
        mDriImpl.updateDroneId(new DriCore.Id(Dri.IdType.FR_30_OCTETS, "MyFrId")).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.FR_30_OCTETS, "MyFrId"));

        // mock update from backend with same values does nothing
        mDriImpl.updateDroneId(new DriCore.Id(Dri.IdType.FR_30_OCTETS, "MyFrId")).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mDri.getDroneId(), DroneIdMatcher.is(Dri.IdType.FR_30_OCTETS, "MyFrId"));
    }

    @Test
    public void testCancelRollbacks() {

        mDriImpl.state().setEnabled(false);
        mDriImpl.publish();

        assertThat(mDri.state(), booleanSettingValueIs(false));

        // mock user changes settings
        mDri.state().setEnabled(true);

        // cancel all rollbacks
        mDriImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mDri.state(), booleanSettingValueIs(true));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mDri.state(), booleanSettingValueIs(true));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements DriCore.Backend {

        private boolean mEnabled;

        @Override
        public boolean setState(boolean enabled) {
            mEnabled = enabled;
            return true;
        }
    }
}