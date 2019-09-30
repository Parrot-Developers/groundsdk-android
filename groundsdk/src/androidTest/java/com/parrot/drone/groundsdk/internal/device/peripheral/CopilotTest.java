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

import com.parrot.drone.groundsdk.device.peripheral.Copilot;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class CopilotTest {

    private MockComponentStore<Peripheral> mStore;

    private CopilotCore mCopilotImpl;

    private Copilot mCopilot;

    private Backend mBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mCopilotImpl = new CopilotCore(mStore, mBackend);
        mCopilot = mStore.get(Copilot.class);
        mStore.registerObserver(Copilot.class, () -> {
            mCopilot = mStore.get(Copilot.class);
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
        assertThat(mCopilot, nullValue());

        mCopilotImpl.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCopilot, is(mCopilotImpl));

        mCopilotImpl.unpublish();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mCopilot, nullValue());
    }

    @Test
    public void testSource() {
        mCopilotImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mCopilot.source(), enumSettingIsUpToDateAt(Copilot.Source.REMOTE_CONTROL));

        // change setting from the api
        mCopilot.source().setValue(Copilot.Source.APPLICATION);
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mSource, is(Copilot.Source.APPLICATION));
        assertThat(mCopilot.source(), enumSettingIsUpdatingTo(Copilot.Source.APPLICATION));

        // mock update from backend
        mCopilotImpl.source().updateValue(Copilot.Source.APPLICATION);
        mCopilotImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mCopilotImpl.source(), enumSettingIsUpToDateAt(Copilot.Source.APPLICATION));

        // change setting from the api
        mCopilot.source().setValue(Copilot.Source.REMOTE_CONTROL);
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mBackend.mSource, is(Copilot.Source.REMOTE_CONTROL));
        assertThat(mCopilot.source(), enumSettingIsUpdatingTo(Copilot.Source.REMOTE_CONTROL));

        // mock update from backend
        mCopilotImpl.source().updateValue(Copilot.Source.REMOTE_CONTROL);
        mCopilotImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mCopilot.source(), enumSettingIsUpToDateAt(Copilot.Source.REMOTE_CONTROL));
    }

    @Test
    public void testCancelRollbacks() {
        mCopilotImpl.publish();

        assertThat(mCopilot.source(), allOf(
                enumSettingValueIs(Copilot.Source.REMOTE_CONTROL),
                settingIsUpToDate()));

        // mock user changes settings
        mCopilot.source().setValue(Copilot.Source.APPLICATION);

        // cancel all rollbacks
        mCopilotImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mCopilot.source(), allOf(
                enumSettingValueIs(Copilot.Source.APPLICATION),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mCopilot.source(), allOf(
                enumSettingValueIs(Copilot.Source.APPLICATION),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements CopilotCore.Backend {

        private Copilot.Source mSource;

        @Override
        public boolean setSource(@NonNull Copilot.Source source) {
            mSource = source;
            return true;
        }
    }
}
