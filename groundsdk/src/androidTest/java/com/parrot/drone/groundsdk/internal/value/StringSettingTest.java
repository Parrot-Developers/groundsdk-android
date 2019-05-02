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
import com.parrot.drone.groundsdk.value.StringSetting;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static com.parrot.drone.groundsdk.StringSettingMatcher.stringSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.StringSettingMatcher.stringSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.StringSettingMatcher.stringSettingValueIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class StringSettingTest {

    private StringSetting mSetting;

    private StringSettingCore mImpl;

    private boolean mSendValueOk;

    private String mSentValue;

    private int mChangeCounter;

    private int mUserChangeCounter;

    private static final String DEFAULT_VALUE = "default";

    @Before
    public void setUp() {
        TestExecutor.setup();
        mSentValue = null;
        mChangeCounter = mUserChangeCounter = 0;
        SettingController controller = new SettingController(fromUser -> {
            mChangeCounter++;
            if (fromUser) {
                mUserChangeCounter++;
            }
        });
        mSetting = mImpl = new StringSettingCore(DEFAULT_VALUE, controller, value -> {
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
    public void testDefaultValue() {
        assertThat(mSetting, stringSettingIsUpToDateAt(DEFAULT_VALUE));
    }

    @Test
    public void testGetValue() {
        // init setting
        mImpl.updateValue("new value");
        assertThat(mChangeCounter, is(1));
        assertThat(mUserChangeCounter, is(0));
        // test values, setting should not be updating after an update from low-level
        assertThat(mSetting, stringSettingIsUpToDateAt("new value"));
    }

    @Test
    public void testSetValue() {
        // mock the change is ok from low-level
        mSendValueOk = true;
        // user sets some value
        mSetting.setValue("user value");
        // test that value got sent
        assertThat(mSentValue, is("user value"));
        assertThat(mChangeCounter, is(1));
        assertThat(mUserChangeCounter, is(1));
        // and setting is updating, and value is set locally while updating
        assertThat(mSetting, stringSettingIsUpdatingTo("user value"));
    }

    @Test
    public void testSetValueFailure() {
        // mock the change fails at low-level
        mSendValueOk = false;
        mSetting.setValue("user value");
        // value should not have been sent
        assertThat(mChangeCounter, is(0));
        assertThat(mUserChangeCounter, is(0));
        // setting should not be updating, and local value should still be default
        assertThat(mSetting, stringSettingIsUpToDateAt(DEFAULT_VALUE));
    }

    @Test
    public void testValueUpdate() {
        // mock the change is ok from low-level
        mSendValueOk = true;
        // first mock a user-side setValue()
        mSetting.setValue("new value");
        assertThat(mChangeCounter, is(1));
        assertThat(mUserChangeCounter, is(1));
        // mock value update from low-level
        mImpl.updateValue("backend value");
        assertThat(mChangeCounter, is(2));
        assertThat(mUserChangeCounter, is(1));
        // value should not be updating anymore and be equal to what came from low-level
        assertThat(mSetting, stringSettingIsUpToDateAt("backend value"));
    }

    @Test
    public void testUpdateChangedReport() {
        // mock an update from low-level with the same values
        mImpl.updateValue(DEFAULT_VALUE);
        // setting should not report change
        assertThat(mChangeCounter, is(0));
        assertThat(mUserChangeCounter, is(0));
        // mock an update with different value
        mImpl.updateValue("new value");
        assertThat(mChangeCounter, is(1));
        assertThat(mUserChangeCounter, is(0));
    }

    @Test
    public void testTimeouts() {
        // mock the change is ok from low-level
        mSendValueOk = true;

        assertThat(mChangeCounter, is(0));
        assertThat(mSetting, allOf(
                stringSettingValueIs(DEFAULT_VALUE),
                settingIsUpToDate()));

        // mock user sets value
        mSetting.setValue("valueA");

        assertThat(mChangeCounter, is(1));
        assertThat(mSetting, allOf(
                stringSettingValueIs("valueA"),
                settingIsUpdating()));

        // mock backend updates value
        mImpl.updateValue("valueA");

        assertThat(mChangeCounter, is(2));
        assertThat(mSetting, allOf(
                stringSettingValueIs("valueA"),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mChangeCounter, is(2));
        assertThat(mSetting, allOf(
                stringSettingValueIs("valueA"),
                settingIsUpToDate()));

        // mock user sets value
        mSetting.setValue("valueB");

        assertThat(mChangeCounter, is(3));
        assertThat(mSetting, allOf(
                stringSettingValueIs("valueB"),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mChangeCounter, is(4));
        assertThat(mSetting, allOf(
                stringSettingValueIs("valueA"),
                settingIsUpToDate()));
    }

    @Test
    public void testCancelRollback() {
        // mock the change is ok from low-level
        mSendValueOk = true;

        assertThat(mChangeCounter, is(0));
        assertThat(mSetting, allOf(
                stringSettingValueIs(DEFAULT_VALUE),
                settingIsUpToDate()));

        // mock user sets value
        mSetting.setValue("valueA");

        assertThat(mChangeCounter, is(1));
        assertThat(mSetting, allOf(
                stringSettingValueIs("valueA"),
                settingIsUpdating()));

        // cancel rollback
        mImpl.cancelRollback();

        // setting should be updated to user value
        assertThat(mChangeCounter, is(2));
        assertThat(mSetting, allOf(
                stringSettingValueIs("valueA"),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mChangeCounter, is(2));
        assertThat(mSetting, allOf(
                stringSettingValueIs("valueA"),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }
}
