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

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.PilotingControl;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingSupports;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class PilotingControlTest {

    private MockComponentStore<Peripheral> mStore;

    private PilotingControlCore mPilotingControlImpl;

    private PilotingControl mPilotingControl;

    private Backend mBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mPilotingControlImpl = new PilotingControlCore(mStore, mBackend);
        mPilotingControl = mStore.get(PilotingControl.class);
        mStore.registerObserver(PilotingControl.class, () -> {
            mPilotingControl = mStore.get(PilotingControl.class);
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
        assertThat(mPilotingControl, nullValue());

        mPilotingControlImpl.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mPilotingControl, is(mPilotingControlImpl));

        mPilotingControlImpl.unpublish();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mPilotingControl, nullValue());
    }

    @Test
    public void testBehavior() {
        mPilotingControlImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mPilotingControl.behavior(), allOf(
                enumSettingSupports(EnumSet.of(PilotingControl.Behavior.STANDARD)),
                enumSettingIsUpToDateAt(PilotingControl.Behavior.STANDARD)));

        // change setting from the api to an unsupported value
        mPilotingControl.behavior().setValue(PilotingControl.Behavior.CAMERA_OPERATED);
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mBehavior, is(nullValue()));
        assertThat(mPilotingControl.behavior(), enumSettingIsUpToDateAt(PilotingControl.Behavior.STANDARD));

        // mock available behaviors
        mPilotingControlImpl.behavior().updateAvailableValues(EnumSet.allOf(PilotingControl.Behavior.class));
        mPilotingControlImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mPilotingControl.behavior(), allOf(
                enumSettingSupports(EnumSet.allOf(PilotingControl.Behavior.class)),
                enumSettingIsUpToDateAt(PilotingControl.Behavior.STANDARD)));

        // change setting from the api
        mPilotingControl.behavior().setValue(PilotingControl.Behavior.CAMERA_OPERATED);
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mBehavior, is(PilotingControl.Behavior.CAMERA_OPERATED));
        assertThat(mPilotingControl.behavior(), enumSettingIsUpdatingTo(PilotingControl.Behavior.CAMERA_OPERATED));

        // mock update from backend
        mPilotingControlImpl.behavior().updateValue(PilotingControl.Behavior.CAMERA_OPERATED);
        mPilotingControlImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mPilotingControlImpl.behavior(), enumSettingIsUpToDateAt(PilotingControl.Behavior.CAMERA_OPERATED));

        // change setting from the api
        mPilotingControl.behavior().setValue(PilotingControl.Behavior.STANDARD);
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mBackend.mBehavior, is(PilotingControl.Behavior.STANDARD));
        assertThat(mPilotingControl.behavior(), enumSettingIsUpdatingTo(PilotingControl.Behavior.STANDARD));

        // mock update from backend
        mPilotingControlImpl.behavior().updateValue(PilotingControl.Behavior.STANDARD);
        mPilotingControlImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mPilotingControl.behavior(), enumSettingIsUpToDateAt(PilotingControl.Behavior.STANDARD));
    }

    @Test
    public void testCancelRollbacks() {
        mPilotingControlImpl.publish();

        assertThat(mPilotingControl.behavior(), allOf(
                enumSettingValueIs(PilotingControl.Behavior.STANDARD),
                settingIsUpToDate()));

        // mock available behaviors
        mPilotingControlImpl.behavior().updateAvailableValues(EnumSet.allOf(PilotingControl.Behavior.class));

        // mock user changes settings
        mPilotingControl.behavior().setValue(PilotingControl.Behavior.CAMERA_OPERATED);

        // cancel all rollbacks
        mPilotingControlImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mPilotingControl.behavior(), allOf(
                enumSettingValueIs(PilotingControl.Behavior.CAMERA_OPERATED),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mPilotingControl.behavior(), allOf(
                enumSettingValueIs(PilotingControl.Behavior.CAMERA_OPERATED),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements PilotingControlCore.Backend {

        private PilotingControl.Behavior mBehavior;

        @Override
        public boolean setBehavior(@NonNull PilotingControl.Behavior behavior) {
            mBehavior = behavior;
            return true;
        }
    }
}
