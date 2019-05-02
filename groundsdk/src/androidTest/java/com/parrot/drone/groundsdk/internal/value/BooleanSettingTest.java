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

package com.parrot.drone.groundsdk.internal.value;

import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.value.BooleanSetting;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabling;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class BooleanSettingTest {

    private BooleanSetting mSetting;

    private BooleanSettingCore mImpl;

    private boolean mSendValueOk;

    private boolean mSentValue;

    private int mChangeCounter;

    private int mUserChangeCounter;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mSentValue = false;
        mChangeCounter = mUserChangeCounter = 0;
        SettingController controller = new SettingController(fromUser -> {
            mChangeCounter++;
            if (fromUser) {
                mUserChangeCounter++;
            }
        });
        mSetting = mImpl = new BooleanSettingCore(controller, value -> {
            mSentValue = value;
            return mSendValueOk;
        });
    }

    @After
    public void teardown() {
        mSetting = mImpl = null;
        TestExecutor.teardown();
    }

    @Test
    public void testGetValues() {
        // init setting
        mImpl.updateValue(true);
        assertThat(mChangeCounter, is(1));
        assertThat(mUserChangeCounter, is(0));
        // test values, setting should be supported, and not updating after an update from low-level
        assertThat(mSetting, booleanSettingIsEnabled());
    }

    @Test
    public void testSetValue() {
        // mock the change is ok from low-level
        mSendValueOk = true;
        // user sets some value
        mSetting.toggle();
        // test that value got sent
        assertThat(mSentValue, is(true));
        assertThat(mChangeCounter, is(1));
        assertThat(mUserChangeCounter, is(1));
        // and setting is updating, and value is set locally while updating
        assertThat(mSetting, booleanSettingIsEnabling());
    }

    @Test
    public void testSetValueFailure() {
        // mock the change fails at low-level
        mSendValueOk = false;
        mSetting.toggle();
        // value should not have been sent
        assertThat(mChangeCounter, is(0));
        assertThat(mUserChangeCounter, is(0));
        // setting should not be updating, and local value should still be default
        assertThat(mSetting, booleanSettingIsDisabled());
    }

    @Test
    public void testValueUpdate() {
        // mock the change is ok from low-level
        mSendValueOk = true;
        // first mock a user-side setValue()
        mSetting.toggle();
        assertThat(mChangeCounter, is(1));
        assertThat(mUserChangeCounter, is(1));
        // mock value update from low-level
        mImpl.updateValue(true);
        assertThat(mChangeCounter, is(2));
        assertThat(mUserChangeCounter, is(1));
        // value should not be updating anymore and be equal to what came from low-level
        assertThat(mSetting, booleanSettingIsEnabled());
    }

    @Test
    public void testUpdateChangedReport() {
        // mock an update from low-level with the same values
        mImpl.updateValue(false);
        // setting should not report change
        assertThat(mChangeCounter, is(0));
        assertThat(mUserChangeCounter, is(0));
        // mock an update with different value
        mImpl.updateValue(true);
        // setting should report change
        assertThat(mChangeCounter, is(1));
        assertThat(mUserChangeCounter, is(0));
    }

    @Test
    public void testTimeouts() {
        // mock the change is ok from low-level
        mSendValueOk = true;

        assertThat(mChangeCounter, is(0));
        assertThat(mSetting, allOf(
                booleanSettingValueIs(false),
                settingIsUpToDate()));

        // mock user sets value
        mSetting.setEnabled(true);

        assertThat(mChangeCounter, is(1));
        assertThat(mSetting, allOf(
                booleanSettingValueIs(true),
                settingIsUpdating()));

        // mock backend updates value
        mImpl.updateValue(true);

        assertThat(mChangeCounter, is(2));
        assertThat(mSetting, allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mChangeCounter, is(2));
        assertThat(mSetting, allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));

        // mock user sets value
        mSetting.setEnabled(false);

        assertThat(mChangeCounter, is(3));
        assertThat(mSetting, allOf(
                booleanSettingValueIs(false),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mChangeCounter, is(4));
        assertThat(mSetting, allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));
    }

    @Test
    public void testCancelRollback() {
        // mock the change is ok from low-level
        mSendValueOk = true;

        assertThat(mChangeCounter, is(0));
        assertThat(mSetting, allOf(
                booleanSettingValueIs(false),
                settingIsUpToDate()));

        // mock user sets value
        mSetting.setEnabled(true);

        assertThat(mChangeCounter, is(1));
        assertThat(mSetting, allOf(
                booleanSettingValueIs(true),
                settingIsUpdating()));

        // cancel rollback
        mImpl.cancelRollback();

        // setting should be updated to user value
        assertThat(mChangeCounter, is(2));
        assertThat(mSetting, allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mChangeCounter, is(2));
        assertThat(mSetting, allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }
}
