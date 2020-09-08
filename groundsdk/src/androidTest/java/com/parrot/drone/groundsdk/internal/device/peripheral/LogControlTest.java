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

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.LogControl;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class LogControlTest {
    private MockComponentStore<Peripheral> mStore;

    private LogControlCore mSecurityEditionImpl;

    private LogControl mLogControl;

    private Backend mMockBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mMockBackend = new LogControlTest.Backend();
        mSecurityEditionImpl = new LogControlCore(mStore, mMockBackend);
        mLogControl = mStore.get(LogControl.class);
        mStore.registerObserver(LogControl.class, () -> {
            mComponentChangeCnt++;
            mLogControl = mStore.get(LogControl.class);
        });
        mComponentChangeCnt = 0;
    }

    @After
    public void tearDown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mLogControl, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mSecurityEditionImpl.publish();

        assertThat(mLogControl, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mSecurityEditionImpl.unpublish();

        assertThat(mLogControl, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testDeactivateLogs() {
        mSecurityEditionImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mLogControl.areLogsEnabled(), is(true));
        assertThat(mLogControl.canDeactivateLogs(), is(false));

        // trying to deactivate without capability does nothing
        mLogControl.deactivateLogs();
        assertThat(mMockBackend.mDeactivated, is(false));

        // logs can be disabled from backend
        mSecurityEditionImpl.updateDeactivateLogsSupported(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mLogControl.areLogsEnabled(), is(true));
        assertThat(mLogControl.canDeactivateLogs(), is(true));

        // change setting from the api
        mLogControl.deactivateLogs();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mMockBackend.mDeactivated, is(true));

        // mock update from backend
        mSecurityEditionImpl.updateLogsState(false).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mLogControl.areLogsEnabled(), is(false));
    }


    private static final class Backend implements LogControlCore.Backend {

        boolean mDeactivated;

        @Override
        public boolean deactivateLogs() {
            mDeactivated = true;
            return true;
        }
    }
}
