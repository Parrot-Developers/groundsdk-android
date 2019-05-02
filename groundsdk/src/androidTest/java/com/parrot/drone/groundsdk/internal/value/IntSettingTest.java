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
import com.parrot.drone.groundsdk.value.IntSetting;
import com.parrot.drone.groundsdk.value.IntegerRange;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.IntSettingMatcher.intSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.IntSettingMatcher.intSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.IntSettingMatcher.intSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class IntSettingTest {

    private IntSetting mSetting;

    private IntSettingCore mImpl;

    private boolean mSendValueOk;

    private int mSentValue;

    private int mChangeCounter;

    private int mUserChangeCounter;

    private static final int DEFAULT_MIN = 1;

    private static final int DEFAULT_VALUE = 5;

    private static final int DEFAULT_MAX = 10;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mSentValue = 0;
        mChangeCounter = mUserChangeCounter = 0;
        SettingController controller = new SettingController(fromUser -> {
            mChangeCounter++;
            if (fromUser) {
                mUserChangeCounter++;
            }
        });
        mSetting = mImpl = new IntSettingCore(controller, value -> {
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
    public void testDefaultValues() {
        assertThat(mSetting, intSettingIsUpToDateAt(0, 0, 0));
    }

    @Test
    public void testGetValues() {
        // init setting
        mImpl.updateBounds(IntegerRange.of(DEFAULT_MIN, DEFAULT_MAX)).updateValue(DEFAULT_VALUE);
        assertThat(mChangeCounter, is(2));
        assertThat(mUserChangeCounter, is(0));
        // test values, setting should not be updating after an update from low-level
        assertThat(mSetting, intSettingIsUpToDateAt(DEFAULT_MIN, DEFAULT_VALUE, DEFAULT_MAX));
    }

    @Test
    public void testSetValue() {
        // init setting
        mImpl.updateBounds(IntegerRange.of(DEFAULT_MIN, DEFAULT_MAX)).updateValue(DEFAULT_VALUE);
        assertThat(mChangeCounter, is(2));
        assertThat(mUserChangeCounter, is(0));
        // mock the change is ok from low-level
        mSendValueOk = true;
        // user sets some value
        mSetting.setValue(4);
        // test that value got sent
        assertThat(mSentValue, is(4));
        assertThat(mChangeCounter, is(3));
        assertThat(mUserChangeCounter, is(1));
        // and setting is updating, and value is set locally while updating
        assertThat(mSetting, intSettingIsUpdatingTo(DEFAULT_MIN, 4, DEFAULT_MAX));
    }

    @Test
    public void testUserValueClamping() {
        // init setting
        mImpl.updateBounds(IntegerRange.of(DEFAULT_MIN, DEFAULT_MAX)).updateValue(DEFAULT_VALUE);
        assertThat(mChangeCounter, is(2));
        assertThat(mUserChangeCounter, is(0));
        // mock the change is ok from low-level
        mSendValueOk = true;
        // user sets >max value
        mSetting.setValue(11);
        // test that value got clamped to max and sent
        assertThat(mSentValue, is(DEFAULT_MAX));
        assertThat(mChangeCounter, is(3));
        assertThat(mUserChangeCounter, is(1));
        assertThat(mSetting, intSettingIsUpdatingTo(DEFAULT_MIN, DEFAULT_MAX, DEFAULT_MAX));
        // user sets <min value
        mSetting.setValue(0);
        // test that value got clamped to min and sent
        assertThat(mSentValue, is(DEFAULT_MIN));
        assertThat(mChangeCounter, is(4));
        assertThat(mUserChangeCounter, is(2));
        assertThat(mSetting, intSettingIsUpdatingTo(DEFAULT_MIN, DEFAULT_MIN, DEFAULT_MAX));
    }

    @Test
    public void testSetValueFailure() {
        // init setting
        mImpl.updateBounds(IntegerRange.of(DEFAULT_MIN, DEFAULT_MAX)).updateValue(DEFAULT_VALUE);
        assertThat(mChangeCounter, is(2));
        assertThat(mUserChangeCounter, is(0));
        // mock the change fails at low-level
        mSendValueOk = false;
        mSetting.setValue(4);
        // value should not have been sent
        assertThat(mChangeCounter, is(2));
        assertThat(mUserChangeCounter, is(0));
        // setting should not be updating, and local value should still be default
        assertThat(mSetting, intSettingIsUpToDateAt(DEFAULT_MIN, DEFAULT_VALUE, DEFAULT_MAX));
    }

    @Test
    public void testValueUpdate() {
        // init setting
        mImpl.updateBounds(IntegerRange.of(DEFAULT_MIN, DEFAULT_MAX)).updateValue(DEFAULT_VALUE);
        assertThat(mChangeCounter, is(2));
        assertThat(mUserChangeCounter, is(0));
        // mock the change is ok from low-level
        mSendValueOk = true;
        // first mock a user-side setValue()
        mSetting.setValue(4);
        assertThat(mChangeCounter, is(3));
        assertThat(mUserChangeCounter, is(1));
        // mock value update from low-level
        mImpl.updateValue(5);
        // value should not be updating anymore and be equal to what came from low-level
        assertThat(mSetting, intSettingIsUpToDateAt(DEFAULT_MIN, 5, DEFAULT_MAX));
        assertThat(mChangeCounter, is(4));
        assertThat(mUserChangeCounter, is(1));
    }

    @Test
    public void testUpdateChangedReport() {
        // init setting
        mImpl.updateBounds(IntegerRange.of(DEFAULT_MIN, DEFAULT_MAX)).updateValue(DEFAULT_VALUE);
        assertThat(mChangeCounter, is(2));
        assertThat(mUserChangeCounter, is(0));
        // mock an update from low-level with the same values
        mImpl.updateBounds(IntegerRange.of(DEFAULT_MIN, DEFAULT_MAX)).updateValue(DEFAULT_VALUE);
        // setting should not report change
        assertThat(mChangeCounter, is(2));
        assertThat(mUserChangeCounter, is(0));
        // mock an update with different min
        mImpl.updateBounds(IntegerRange.of(-1, DEFAULT_MAX));
        // setting should report change
        assertThat(mChangeCounter, is(3));
        assertThat(mUserChangeCounter, is(0));
        // mock an update with different max
        mImpl.updateBounds(IntegerRange.of(-1, 11));
        // setting should report change
        assertThat(mChangeCounter, is(4));
        assertThat(mUserChangeCounter, is(0));
        // mock an update with different value
        mImpl.updateValue(7);
        // setting should report change
        assertThat(mChangeCounter, is(5));
        assertThat(mUserChangeCounter, is(0));
    }

    @Test
    public void testTimeouts() {
        // init setting
        mImpl.updateBounds(IntegerRange.of(0, 4)).updateValue(1);
        // mock the change is ok from low-level
        mSendValueOk = true;
        mChangeCounter = 0;

        assertThat(mChangeCounter, is(0));
        assertThat(mSetting, allOf(
                intSettingValueIs(0, 1, 4),
                settingIsUpToDate()));

        // mock user sets value
        mSetting.setValue(2);

        assertThat(mChangeCounter, is(1));
        assertThat(mSetting, allOf(
                intSettingValueIs(0, 2, 4),
                settingIsUpdating()));

        // mock backend updates value
        mImpl.updateValue(2);

        assertThat(mChangeCounter, is(2));
        assertThat(mSetting, allOf(
                intSettingValueIs(0, 2, 4),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mChangeCounter, is(2));
        assertThat(mSetting, allOf(
                intSettingValueIs(0, 2, 4),
                settingIsUpToDate()));

        // mock user sets value
        mSetting.setValue(3);

        assertThat(mChangeCounter, is(3));
        assertThat(mSetting, allOf(
                intSettingValueIs(0, 3, 4),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mChangeCounter, is(4));
        assertThat(mSetting, allOf(
                intSettingValueIs(0, 2, 4),
                settingIsUpToDate()));
    }

    @Test
    public void testCancelRollback() {
        // init setting
        mImpl.updateBounds(IntegerRange.of(0, 4)).updateValue(1);
        // mock the change is ok from low-level
        mSendValueOk = true;
        mChangeCounter = 0;

        assertThat(mChangeCounter, is(0));
        assertThat(mSetting, allOf(
                intSettingValueIs(0, 1, 4),
                settingIsUpToDate()));

        // mock user sets value
        mSetting.setValue(2);

        assertThat(mChangeCounter, is(1));
        assertThat(mSetting, allOf(
                intSettingValueIs(0, 2, 4),
                settingIsUpdating()));

        // cancel rollback
        mImpl.cancelRollback();

        // setting should be updated to user value
        assertThat(mChangeCounter, is(2));
        assertThat(mSetting, allOf(
                intSettingValueIs(0, 2, 4),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mChangeCounter, is(2));
        assertThat(mSetting, allOf(
                intSettingValueIs(0, 2, 4),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }
}
