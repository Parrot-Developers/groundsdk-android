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
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.Gimbal;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.value.DoubleRange;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabling;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingValueIs;
import static com.parrot.drone.groundsdk.DoubleRangeMatcher.doubleRangeIs;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class GimbalTest {

    private MockComponentStore<Peripheral> mStore;

    private GimbalCore mGimbalImpl;

    private Gimbal mGimbal;

    private Backend mBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mGimbalImpl = new GimbalCore(mStore, mBackend);
        mGimbal = mStore.get(Gimbal.class);
        mStore.registerObserver(Gimbal.class, () -> {
            mGimbal = mStore.get(Gimbal.class);
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
        assertThat(mGimbal, nullValue());

        mGimbalImpl.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbal, is(mGimbalImpl));

        mGimbalImpl.unpublish();

        assertThat(mGimbal, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testSupportedAxes() {
        mGimbalImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbal.getSupportedAxes(), empty());

        // test update from backend
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));

        // check that receiving same axes does not trigger a notification
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));

        // check that changing supported axes triggers a notification
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH, Gimbal.Axis.ROLL))
                   .notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(
                Gimbal.Axis.YAW, Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));

        // fill gimbal data
        DoubleRange bounds = DoubleRange.of(0.0, 25.0);
        EnumMap<Gimbal.Axis, Double> maxSpeeds = new EnumMap<>(Gimbal.Axis.class);
        maxSpeeds.put(Gimbal.Axis.YAW, 2.2);
        maxSpeeds.put(Gimbal.Axis.PITCH, 2.2);
        maxSpeeds.put(Gimbal.Axis.ROLL, 2.2);
        EnumMap<Gimbal.Axis, DoubleRange> maxSpeedRanges = new EnumMap<>(Gimbal.Axis.class);
        maxSpeedRanges.put(Gimbal.Axis.YAW, DoubleRange.of(0.0, 3.3));
        maxSpeedRanges.put(Gimbal.Axis.PITCH, DoubleRange.of(0.0, 3.3));
        maxSpeedRanges.put(Gimbal.Axis.ROLL, DoubleRange.of(0.0, 3.3));
        mGimbalImpl.updateAttitudeBounds(Gimbal.Axis.YAW, bounds)
                   .updateAttitudeBounds(Gimbal.Axis.PITCH, bounds)
                   .updateAttitudeBounds(Gimbal.Axis.ROLL, bounds)
                   .updateMaxSpeedRanges(maxSpeedRanges)
                   .updateMaxSpeeds(maxSpeeds)
                   .updateLockedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.ROLL))
                   .updateStabilization(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH))
                   .updateAbsoluteAttitude(Gimbal.Axis.YAW, 10.0)
                   .updateAbsoluteAttitude(Gimbal.Axis.PITCH, 10.0)
                   .updateAbsoluteAttitude(Gimbal.Axis.ROLL, 10.0)
                   .updateRelativeAttitude(Gimbal.Axis.YAW, 20.0)
                   .updateRelativeAttitude(Gimbal.Axis.PITCH, 20.0)
                   .updateRelativeAttitude(Gimbal.Axis.ROLL, 20.0)
                   .notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(
                Gimbal.Axis.YAW, Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(0.0, 25.0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0.0, 25.0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.ROLL), doubleRangeIs(0.0, 25.0));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), doubleSettingIsUpToDateAt(0.0, 2.2, 3.3));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(0.0, 2.2, 3.3));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.ROLL), doubleSettingIsUpToDateAt(0.0, 2.2, 3.3));
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.YAW, Gimbal.Axis.ROLL));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), booleanSettingIsDisabled());
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW, Gimbal.FrameOfReference.ABSOLUTE), is(10.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.ABSOLUTE), is(10.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL, Gimbal.FrameOfReference.ABSOLUTE), is(10.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW, Gimbal.FrameOfReference.RELATIVE), is(20.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.RELATIVE), is(20.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL, Gimbal.FrameOfReference.RELATIVE), is(20.0));

        // check that removing supported axes automatically removes these axes from all gimbal attributes
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.YAW));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(0.0, 25.0));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), doubleSettingIsUpToDateAt(0.0, 2.2, 3.3));
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.YAW));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsEnabled());
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW, Gimbal.FrameOfReference.ABSOLUTE), is(10.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW, Gimbal.FrameOfReference.RELATIVE), is(20.0));

        // check that adding supported axes automatically adds these axes to all gimbal attributes
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mGimbal.getSupportedAxes(), containsInAnyOrder(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(0.0, 25.0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0.0, 0.0));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), doubleSettingIsUpToDateAt(0.0, 2.2, 3.3));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(0.0, 0.0, 0.0));
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsDisabled());
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW, Gimbal.FrameOfReference.ABSOLUTE), is(10.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.ABSOLUTE), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW, Gimbal.FrameOfReference.RELATIVE), is(20.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.RELATIVE), is(0.0));
    }

    @Test
    public void testAttitudeBounds() {
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        mGimbalImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(0.0, 0.0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0.0, 0.0));

        // test update from backend
        mGimbalImpl.updateAttitudeBounds(Gimbal.Axis.YAW, DoubleRange.of(2.0, 3.0)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(2.0, 3.0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0.0, 0.0));

        // test new update from backend
        mGimbalImpl.updateAttitudeBounds(Gimbal.Axis.PITCH, DoubleRange.of(0.0, 10.0)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(2.0, 3.0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0.0, 10.0));

        // check that updating with same value does not trigger a notification
        mGimbalImpl.updateAttitudeBounds(Gimbal.Axis.PITCH, DoubleRange.of(0.0, 10.0)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(2.0, 3.0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0.0, 10.0));

        // check that updating a non-supported axis does not trigger a notification
        mGimbalImpl.updateAttitudeBounds(Gimbal.Axis.ROLL, DoubleRange.of(0.0, 50.0)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.YAW), doubleRangeIs(2.0, 3.0));
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.PITCH), doubleRangeIs(0.0, 10.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttitudeBoundsOfUnsupportedAxis() {
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        mGimbalImpl.publish();

        // check that getting attitude bounds of an unsupported axis throws IllegalArgumentException
        assertThat(mGimbal.getAttitudeBounds(Gimbal.Axis.ROLL), nullValue());
    }

    @Test
    public void testMaxSpeeds() {
        EnumMap<Gimbal.Axis, Double> maxSpeeds = new EnumMap<>(Gimbal.Axis.class);
        EnumMap<Gimbal.Axis, DoubleRange> maxSpeedRanges = new EnumMap<>(Gimbal.Axis.class);

        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        mGimbalImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), doubleSettingIsUpToDateAt(0.0, 0.0, 0.0));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(0.0, 0.0, 0.0));

        // test update from backend
        maxSpeeds.put(Gimbal.Axis.YAW, 5.0);
        maxSpeedRanges.put(Gimbal.Axis.YAW, DoubleRange.of(0.0, 10.0));
        mGimbalImpl.updateMaxSpeedRanges(maxSpeedRanges).updateMaxSpeeds(maxSpeeds).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), doubleSettingIsUpToDateAt(0.0, 5.0, 10.0));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(0.0, 0.0, 0.0));

        // test new update from backend
        maxSpeeds.put(Gimbal.Axis.PITCH, 10.0);
        maxSpeedRanges.put(Gimbal.Axis.PITCH, DoubleRange.of(5.0, 20.0));
        mGimbalImpl.updateMaxSpeedRanges(maxSpeedRanges).updateMaxSpeeds(maxSpeeds).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), doubleSettingIsUpToDateAt(0.0, 5.0, 10.0));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(5.0, 10.0, 20.0));

        // check that updating with same value does not trigger a notification
        mGimbalImpl.updateMaxSpeedRanges(maxSpeedRanges).updateMaxSpeeds(maxSpeeds).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), doubleSettingIsUpToDateAt(0.0, 5.0, 10.0));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(5.0, 10.0, 20.0));

        // check that updating a non-supported axis does not trigger a notification
        maxSpeeds.put(Gimbal.Axis.ROLL, 10.0);
        maxSpeedRanges.put(Gimbal.Axis.ROLL, DoubleRange.of(5.0, 20.0));
        mGimbalImpl.updateMaxSpeedRanges(maxSpeedRanges).updateMaxSpeeds(maxSpeeds).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), doubleSettingIsUpToDateAt(0.0, 5.0, 10.0));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(5.0, 10.0, 20.0));

        // change max speed from the api
        mGimbal.getMaxSpeed(Gimbal.Axis.YAW).setValue(15.0);
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), doubleSettingIsUpdatingTo(0.0, 10.0, 10.0));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(5.0, 10.0, 20.0));
        assertThat(mBackend.mSetMaxSpeedCalls, is(1));
        assertThat(mBackend.mLatestMaxSpeedAxis, is(Gimbal.Axis.YAW));
        assertThat(mBackend.mLatestMaxSpeedValue, is(10.0));

        // update from backend
        maxSpeeds.put(Gimbal.Axis.YAW, 10.0);
        mGimbalImpl.updateMaxSpeeds(maxSpeeds).notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.YAW), doubleSettingIsUpToDateAt(0.0, 10.0, 10.0));
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.PITCH), doubleSettingIsUpToDateAt(5.0, 10.0, 20.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxSpeedOfUnsupportedAxis() {
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        mGimbalImpl.publish();

        // check that getting max speed of an unsupported axis throws IllegalArgumentException
        assertThat(mGimbal.getMaxSpeed(Gimbal.Axis.ROLL), nullValue());
    }

    @Test
    public void testLockedAxes() {
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        mGimbalImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));

        // test update from backend
        mGimbalImpl.updateLockedAxes(EnumSet.of(Gimbal.Axis.PITCH)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH));

        // check that updating with same value does not trigger a notification
        mGimbalImpl.updateLockedAxes(EnumSet.of(Gimbal.Axis.PITCH)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH));

        // check that updating a non-supported axis does not trigger a notification
        mGimbalImpl.updateLockedAxes(EnumSet.of(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.PITCH));

        // test new update from backend
        mGimbalImpl.updateLockedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getLockedAxes(), containsInAnyOrder(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
    }

    @Test
    public void testStabilizedAxes() {
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        mGimbalImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsDisabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsDisabled());

        // test update from backend
        mGimbalImpl.updateStabilization(EnumSet.of(Gimbal.Axis.YAW)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsDisabled());

        // test new update from backend
        mGimbalImpl.updateStabilization(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());

        // check that updating with same value does not trigger a notification
        mGimbalImpl.updateStabilization(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());

        // check that updating a non-supported axis does not trigger a notification
        mGimbalImpl.updateStabilization(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH, Gimbal.Axis.ROLL))
                   .notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsEnabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());

        // change stabilized axis from the api
        mGimbal.getStabilization(Gimbal.Axis.YAW).toggle();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsDisabling());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mBackend.mSetStabilizationCalls, is(1));
        assertThat(mBackend.mLatestStabilizationAxis, is(Gimbal.Axis.YAW));
        assertThat(mBackend.mLatestStabilizationValue, is(false));

        // update from backend
        mGimbalImpl.updateStabilization(EnumSet.of(Gimbal.Axis.PITCH)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsDisabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStabilizationOfUnsupportedAxis() {
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        mGimbalImpl.publish();

        // check that getting stabilization of an unsupported axis throws IllegalArgumentException
        assertThat(mGimbal.getStabilization(Gimbal.Axis.ROLL), nullValue());
    }

    @Test
    public void testAttitude() {
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        mGimbalImpl.publish();

        // test initial values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW, Gimbal.FrameOfReference.ABSOLUTE), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.ABSOLUTE), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW, Gimbal.FrameOfReference.RELATIVE), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.RELATIVE), is(0.0));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.YAW), booleanSettingIsDisabled());
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsDisabled());

        // test update relative attitude from backend
        mGimbalImpl.updateRelativeAttitude(Gimbal.Axis.YAW, 2.0).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW, Gimbal.FrameOfReference.ABSOLUTE), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.ABSOLUTE), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW, Gimbal.FrameOfReference.RELATIVE), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.RELATIVE), is(0.0));

        // test update absolute attitude from backend
        mGimbalImpl.updateAbsoluteAttitude(Gimbal.Axis.PITCH, 3.0).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW, Gimbal.FrameOfReference.ABSOLUTE), is(0.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.ABSOLUTE), is(3.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW, Gimbal.FrameOfReference.RELATIVE), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH, Gimbal.FrameOfReference.RELATIVE), is(0.0));

        // test update stabilization from backend
        mGimbalImpl.updateStabilization(EnumSet.of(Gimbal.Axis.PITCH)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mGimbal.getStabilization(Gimbal.Axis.PITCH), booleanSettingIsEnabled());
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(3.0));

        // check that updating with same value does not trigger a notification
        mGimbalImpl.updateAbsoluteAttitude(Gimbal.Axis.PITCH, 3.0).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(3.0));

        // check that updating a non-supported axis does not trigger a notification
        mGimbalImpl.updateAbsoluteAttitude(Gimbal.Axis.ROLL, 5.0).notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.YAW), is(2.0));
        assertThat(mGimbal.getAttitude(Gimbal.Axis.PITCH), is(3.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttitudeOfUnsupportedAxis() {
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        mGimbalImpl.publish();

        // check that getting attitude of an unsupported axis throws IllegalArgumentException
        assertThat(mGimbal.getAttitude(Gimbal.Axis.ROLL), is(0.0));
    }

    @Test
    public void testOffsetCorrectionProcess() {
        mGimbalImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbal.getOffsetCorrectionProcess(), nullValue());
        assertThat(mBackend.mStartCorrectionProcessCalls, is(0));
        assertThat(mBackend.mStopCorrectionProcessCalls, is(0));

        // setting correctable axes when not started should not change anything
        mGimbalImpl.updateCorrectableAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbalImpl.getOffsetCorrectionProcess(), nullValue());
        assertThat(mBackend.mStartCorrectionProcessCalls, is(0));
        assertThat(mBackend.mStopCorrectionProcessCalls, is(0));

        // setting correction offsets when not started should not change anything
        EnumMap<Gimbal.Axis, Double> offsets = new EnumMap<>(Gimbal.Axis.class);
        offsets.put(Gimbal.Axis.PITCH, 10.2);
        EnumMap<Gimbal.Axis, DoubleRange> offsetsRanges = new EnumMap<>(Gimbal.Axis.class);
        offsetsRanges.put(Gimbal.Axis.PITCH, DoubleRange.of(5.0, 10.0));
        mGimbalImpl.updateOffsets(offsets)
                   .updateOffsetsRanges(offsetsRanges)
                   .notifyUpdated();
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbalImpl.getOffsetCorrectionProcess(), nullValue());
        assertThat(mBackend.mStartCorrectionProcessCalls, is(0));
        assertThat(mBackend.mStopCorrectionProcessCalls, is(0));

        // start correction process, nothing should change for the moment
        mGimbalImpl.startOffsetsCorrectionProcess();
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbalImpl.getOffsetCorrectionProcess(), nullValue());
        assertThat(mBackend.mStartCorrectionProcessCalls, is(1));
        assertThat(mBackend.mStopCorrectionProcessCalls, is(0));

        // mock correction process started
        mGimbalImpl.updateOffsetCorrectionProcessState(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbalImpl.getOffsetCorrectionProcess(), notNullValue());
        assertThat(mBackend.mStartCorrectionProcessCalls, is(1));
        assertThat(mBackend.mStopCorrectionProcessCalls, is(0));

        // updating with the same value should not trigger any changes
        mGimbalImpl.updateOffsetCorrectionProcessState(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbalImpl.getOffsetCorrectionProcess(), notNullValue());
        assertThat(mBackend.mStartCorrectionProcessCalls, is(1));
        assertThat(mBackend.mStopCorrectionProcessCalls, is(0));

        // stop correction process, nothing should change for the moment
        mGimbalImpl.stopOffsetsCorrectionProcess();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbalImpl.getOffsetCorrectionProcess(), notNullValue());
        assertThat(mBackend.mStartCorrectionProcessCalls, is(1));
        assertThat(mBackend.mStopCorrectionProcessCalls, is(1));

        // mock correction process stopped
        mGimbalImpl.updateOffsetCorrectionProcessState(false).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbalImpl.getOffsetCorrectionProcess(), nullValue());
        assertThat(mBackend.mStartCorrectionProcessCalls, is(1));
        assertThat(mBackend.mStopCorrectionProcessCalls, is(1));

        // updating with the same value should not trigger any changes
        mGimbalImpl.updateOffsetCorrectionProcessState(false).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbalImpl.getOffsetCorrectionProcess(), nullValue());
        assertThat(mBackend.mStartCorrectionProcessCalls, is(1));
        assertThat(mBackend.mStopCorrectionProcessCalls, is(1));
    }

    @Test
    public void testCorrectableAxes() {
        // during this test, offsets correction process is started
        // behavior when offset is not started will be tested in another test
        mGimbalImpl.updateOffsetCorrectionProcessState(true);
        mGimbalImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assert mGimbal.getOffsetCorrectionProcess() != null;
        assertThat(mGimbal.getOffsetCorrectionProcess(), notNullValue());
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(), empty());

        // test update from backend
        mGimbalImpl.updateCorrectableAxes(EnumSet.of(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(),
                containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));

        // check that receiving same axes does not trigger a notification
        mGimbalImpl.updateCorrectableAxes(EnumSet.of(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(),
                containsInAnyOrder(Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));

        // check that changing supported axes triggers a notification
        mGimbalImpl.updateCorrectableAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH, Gimbal.Axis.ROLL))
                   .notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(),
                containsInAnyOrder(Gimbal.Axis.YAW, Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));

        // fill offsets data
        EnumMap<Gimbal.Axis, Double> offsets = new EnumMap<>(Gimbal.Axis.class);
        offsets.put(Gimbal.Axis.YAW, 2.2);
        offsets.put(Gimbal.Axis.PITCH, 2.2);
        offsets.put(Gimbal.Axis.ROLL, 2.2);
        EnumMap<Gimbal.Axis, DoubleRange> offsetsRanges = new EnumMap<>(Gimbal.Axis.class);
        offsetsRanges.put(Gimbal.Axis.YAW, DoubleRange.of(0.0, 3.3));
        offsetsRanges.put(Gimbal.Axis.PITCH, DoubleRange.of(0.0, 3.3));
        offsetsRanges.put(Gimbal.Axis.ROLL, DoubleRange.of(0.0, 3.3));
        mGimbalImpl.updateOffsetsRanges(offsetsRanges).updateOffsets(offsets).notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(),
                containsInAnyOrder(Gimbal.Axis.YAW, Gimbal.Axis.PITCH, Gimbal.Axis.ROLL));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW),
                doubleSettingIsUpToDateAt(0.0, 2.2, 3.3));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH),
                doubleSettingIsUpToDateAt(0.0, 2.2, 3.3));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.ROLL),
                doubleSettingIsUpToDateAt(0.0, 2.2, 3.3));

        // check that removing calibratable axes automatically removes these axes from offsets
        mGimbalImpl.updateCorrectableAxes(EnumSet.of(Gimbal.Axis.YAW)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(), containsInAnyOrder(Gimbal.Axis.YAW));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW),
                doubleSettingIsUpToDateAt(0.0, 2.2, 3.3));

        // check that adding calibratable axes automatically adds these axes to offsets
        mGimbalImpl.updateCorrectableAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH)).notifyUpdated();
        assertThat(mComponentChangeCnt, is(6));
        assertThat(mGimbal.getOffsetCorrectionProcess().getCorrectableAxes(),
                containsInAnyOrder(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW),
                doubleSettingIsUpToDateAt(0.0, 2.2, 3.3));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH),
                doubleSettingIsUpToDateAt(0.0, 0.0, 0.0));
    }

    @Test
    public void testOffsetsCorrection() {
        EnumMap<Gimbal.Axis, Double> offsets = new EnumMap<>(Gimbal.Axis.class);
        EnumMap<Gimbal.Axis, DoubleRange> offsetsRanges = new EnumMap<>(Gimbal.Axis.class);

        // during this test, offsets correction process is started
        // behavior when offset is not started will be tested in another test
        mGimbalImpl.updateOffsetCorrectionProcessState(true);
        mGimbalImpl.updateCorrectableAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        mGimbalImpl.publish();

        // test initial values
        assertThat(mComponentChangeCnt, is(1));
        assert mGimbal.getOffsetCorrectionProcess() != null;
        assertThat(mGimbal.getOffsetCorrectionProcess(), notNullValue());
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW),
                doubleSettingIsUpToDateAt(0.0, 0.0, 0.0));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH),
                doubleSettingIsUpToDateAt(0.0, 0.0, 0.0));

        // test update from backend
        offsets.put(Gimbal.Axis.YAW, 5.0);
        offsetsRanges.put(Gimbal.Axis.YAW, DoubleRange.of(0.0, 10.0));
        mGimbalImpl.updateOffsetsRanges(offsetsRanges).updateOffsets(offsets).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW),
                doubleSettingIsUpToDateAt(0.0, 5.0, 10.0));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH),
                doubleSettingIsUpToDateAt(0.0, 0.0, 0.0));

        // test new update from backend
        offsets.put(Gimbal.Axis.PITCH, 10.0);
        offsetsRanges.put(Gimbal.Axis.PITCH, DoubleRange.of(5.0, 20.0));
        mGimbalImpl.updateOffsetsRanges(offsetsRanges).updateOffsets(offsets).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW),
                doubleSettingIsUpToDateAt(0.0, 5.0, 10.0));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH),
                doubleSettingIsUpToDateAt(5.0, 10.0, 20.0));

        // check that updating with same value does not trigger a notification
        mGimbalImpl.updateOffsetsRanges(offsetsRanges).updateOffsets(offsets).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW),
                doubleSettingIsUpToDateAt(0.0, 5.0, 10.0));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH),
                doubleSettingIsUpToDateAt(5.0, 10.0, 20.0));

        // check that updating a non-supported axis does not trigger a notification
        offsets.put(Gimbal.Axis.ROLL, 10.0);
        offsetsRanges.put(Gimbal.Axis.ROLL, DoubleRange.of(5.0, 20.0));
        mGimbalImpl.updateOffsetsRanges(offsetsRanges).updateOffsets(offsets).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW),
                doubleSettingIsUpToDateAt(0.0, 5.0, 10.0));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH),
                doubleSettingIsUpToDateAt(5.0, 10.0, 20.0));

        // change offset from the api
        mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW).setValue(15.0);
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW),
                doubleSettingIsUpdatingTo(0.0, 10.0, 10.0));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH),
                doubleSettingIsUpToDateAt(5.0, 10.0, 20.0));
        assertThat(mBackend.mSetOffsetCalls, is(1));
        assertThat(mBackend.mLatestOffsetAxis, is(Gimbal.Axis.YAW));
        assertThat(mBackend.mLatestOffsetValue, is(10.0));

        // update from backend
        offsets.put(Gimbal.Axis.YAW, 10.0);
        mGimbalImpl.updateOffsets(offsets).notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.YAW),
                doubleSettingIsUpToDateAt(0.0, 10.0, 10.0));
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.PITCH),
                doubleSettingIsUpToDateAt(5.0, 10.0, 20.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOffsetOfNotCalibratableAxis() {
        mGimbalImpl.updateOffsetCorrectionProcessState(true);
        mGimbalImpl.updateCorrectableAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        mGimbalImpl.publish();

        // check that getting attitude of an unsupported axis throws IllegalArgumentException
        assert mGimbal.getOffsetCorrectionProcess() != null;
        assertThat(mGimbal.getOffsetCorrectionProcess().getOffset(Gimbal.Axis.ROLL), is(0.0));
    }

    @Test
    public void testIsCalibrated() {
        mGimbalImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbal.isCalibrated(), is(false));

        // mock update from low-level
        mGimbalImpl.updateIsCalibrated(true).notifyUpdated();

        // test calibrated state update
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.isCalibrated(), is(true));

        // mock same update from low-level
        mGimbalImpl.updateIsCalibrated(true).notifyUpdated();

        // test nothing changes
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.isCalibrated(), is(true));
    }

    @Test
    public void testCalibrationProcessState() {
        mGimbalImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.NONE));

        // mock update from low-level
        mGimbalImpl.updateCalibrationProcessState(Gimbal.CalibrationProcessState.CALIBRATING).notifyUpdated();

        // test calibration process state update
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.CALIBRATING));

        // mock same update from low-level
        mGimbalImpl.updateCalibrationProcessState(Gimbal.CalibrationProcessState.CALIBRATING).notifyUpdated();

        // test nothing changes
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.CALIBRATING));
    }

    @Test
    public void testCalibrationStartCancel() {
        mGimbalImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mStartCalibrationCalls, is(0));
        assertThat(mBackend.mCancelCalibrationCalls, is(0));

        // start calibration
        mGimbal.startCalibration();
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mStartCalibrationCalls, is(1));
        assertThat(mBackend.mCancelCalibrationCalls, is(0));

        // update calibration state
        mGimbalImpl.updateCalibrationProcessState(Gimbal.CalibrationProcessState.CALIBRATING).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.CALIBRATING));

        // trying to start calibration while calibrating should do nothing
        mGimbal.startCalibration();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mStartCalibrationCalls, is(1));
        assertThat(mBackend.mCancelCalibrationCalls, is(0));

        // cancel calibration
        mGimbal.cancelCalibration();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mStartCalibrationCalls, is(1));
        assertThat(mBackend.mCancelCalibrationCalls, is(1));

        // update calibration state
        mGimbalImpl.updateCalibrationProcessState(Gimbal.CalibrationProcessState.NONE).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.getCalibrationProcessState(), is(Gimbal.CalibrationProcessState.NONE));

        // trying to cancel calibration while not calibrating should do nothing
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mStartCalibrationCalls, is(1));
        assertThat(mBackend.mCancelCalibrationCalls, is(1));
    }

    @Test
    public void testErrors() {
        mGimbalImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGimbal.currentErrors(), empty());

        // mock update from low-level
        mGimbalImpl.updateErrors(EnumSet.of(Gimbal.Error.CALIBRATION))
                   .notifyUpdated();

        // test value updates
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.currentErrors(), containsInAnyOrder(Gimbal.Error.CALIBRATION));

        // mock same update from low-level
        mGimbalImpl.updateErrors(EnumSet.of(Gimbal.Error.CALIBRATION))
                   .notifyUpdated();

        // test nothing changes
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGimbal.currentErrors(), containsInAnyOrder(Gimbal.Error.CALIBRATION));

        // mock update from low level
        mGimbalImpl.updateErrors(EnumSet.of(Gimbal.Error.OVERLOAD));

        // test no change before notification
        assertThat(mComponentChangeCnt, is(2));

        // mock second update from low level
        mGimbalImpl.updateErrors(EnumSet.of(Gimbal.Error.COMMUNICATION, Gimbal.Error.CRITICAL));

        // test no change before notification
        assertThat(mComponentChangeCnt, is(2));

        // notify
        mGimbalImpl.notifyUpdated();

        // test value updates to latest and only one change is notified
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGimbal.currentErrors(), containsInAnyOrder(Gimbal.Error.COMMUNICATION, Gimbal.Error.CRITICAL));
    }

    @Test
    public void testControl() {
        mGimbalImpl.updateSupportedAxes(EnumSet.of(Gimbal.Axis.YAW, Gimbal.Axis.PITCH));
        mGimbalImpl.publish();

        mGimbal.control(Gimbal.ControlMode.POSITION, 10.0, null, 20.0);
        assertThat(mBackend.mControlCalls, is(1));
        assertThat(mBackend.mControlMode, is(Gimbal.ControlMode.POSITION));
        assertThat(mBackend.mYaw, is(10.0));
        assertThat(mBackend.mPitch, nullValue());
        assertThat(mBackend.mRoll, nullValue());

        mGimbal.control(Gimbal.ControlMode.VELOCITY, null, -0.5, 20.0);
        assertThat(mBackend.mControlCalls, is(2));
        assertThat(mBackend.mControlMode, is(Gimbal.ControlMode.VELOCITY));
        assertThat(mBackend.mYaw, nullValue());
        assertThat(mBackend.mPitch, is(-0.5));
        assertThat(mBackend.mRoll, nullValue());
    }

    @Test
    public void testCancelRollbacks() {
        EnumMap<Gimbal.Axis, DoubleRange> maxs = new EnumMap<>(Gimbal.Axis.class);
        EnumMap<Gimbal.Axis, Double> values = new EnumMap<>(Gimbal.Axis.class);
        for (Gimbal.Axis axis : Gimbal.Axis.values()) {
            maxs.put(axis, DoubleRange.of(0, 1));
            values.put(axis, 0.0);
        }
        mGimbalImpl.updateSupportedAxes(EnumSet.allOf(Gimbal.Axis.class))
                   .updateMaxSpeedRanges(maxs)
                   .updateMaxSpeeds(values)
                   .updateStabilization(EnumSet.noneOf(Gimbal.Axis.class))
                   .updateOffsetCorrectionProcessState(true)
                   .updateCorrectableAxes(EnumSet.allOf(Gimbal.Axis.class))
                   .updateOffsetsRanges(maxs)
                   .updateOffsets(values);
        mGimbalImpl.publish();

        Gimbal.OffsetCorrectionProcess correctionProcess = mGimbal.getOffsetCorrectionProcess();
        assertThat(correctionProcess, notNullValue());

        for (Gimbal.Axis axis : Gimbal.Axis.values()) {
            assertThat(mGimbal.getMaxSpeed(axis), allOf(
                    doubleSettingValueIs(0, 0, 1),
                    settingIsUpToDate()));
            assertThat(mGimbal.getStabilization(axis), allOf(
                    booleanSettingValueIs(false),
                    settingIsUpToDate()));
            assertThat(correctionProcess.getOffset(axis), allOf(
                    doubleSettingValueIs(0, 0, 1),
                    settingIsUpToDate()));
        }

        // mock user changes settings
        for (Gimbal.Axis axis : Gimbal.Axis.values()) {
            mGimbal.getMaxSpeed(axis).setValue(1);
            mGimbal.getStabilization(axis).setEnabled(true);
            correctionProcess.getOffset(axis).setValue(1);
        }

        // cancel all rollbacks
        mGimbalImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        for (Gimbal.Axis axis : Gimbal.Axis.values()) {
            assertThat(mGimbal.getMaxSpeed(axis), allOf(
                    doubleSettingValueIs(0, 1, 1),
                    settingIsUpToDate()));
            assertThat(mGimbal.getStabilization(axis), allOf(
                    booleanSettingValueIs(true),
                    settingIsUpToDate()));
            assertThat(correctionProcess.getOffset(axis), allOf(
                    doubleSettingValueIs(0, 1, 1),
                    settingIsUpToDate()));
        }

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        for (Gimbal.Axis axis : Gimbal.Axis.values()) {
            assertThat(mGimbal.getMaxSpeed(axis), allOf(
                    doubleSettingValueIs(0, 1, 1),
                    settingIsUpToDate()));
            assertThat(mGimbal.getStabilization(axis), allOf(
                    booleanSettingValueIs(true),
                    settingIsUpToDate()));
            assertThat(correctionProcess.getOffset(axis), allOf(
                    doubleSettingValueIs(0, 1, 1),
                    settingIsUpToDate()));
        }
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements GimbalCore.Backend {

        private int mSetMaxSpeedCalls;

        private Gimbal.Axis mLatestMaxSpeedAxis;

        private double mLatestMaxSpeedValue;

        private int mSetStabilizationCalls;

        private Gimbal.Axis mLatestStabilizationAxis;

        private boolean mLatestStabilizationValue;

        private int mSetOffsetCalls;

        private Gimbal.Axis mLatestOffsetAxis;

        private double mLatestOffsetValue;

        private int mControlCalls;

        private Gimbal.ControlMode mControlMode;

        private Double mYaw;

        private Double mPitch;

        private Double mRoll;

        private int mStartCorrectionProcessCalls;

        private int mStopCorrectionProcessCalls;

        private int mStartCalibrationCalls;

        private int mCancelCalibrationCalls;

        @Override
        public boolean setMaxSpeed(@NonNull Gimbal.Axis axis, double speed) {
            mSetMaxSpeedCalls++;
            mLatestMaxSpeedAxis = axis;
            mLatestMaxSpeedValue = speed;
            return true;
        }

        @Override
        public boolean setStabilization(@NonNull Gimbal.Axis axis, boolean stabilized) {
            mSetStabilizationCalls++;
            mLatestStabilizationAxis = axis;
            mLatestStabilizationValue = stabilized;
            return true;
        }

        @Override
        public boolean setOffset(@NonNull Gimbal.Axis axis, double offset) {
            mSetOffsetCalls++;
            mLatestOffsetAxis = axis;
            mLatestOffsetValue = offset;
            return true;
        }

        @Override
        public void control(@NonNull Gimbal.ControlMode mode, @Nullable Double yaw, @Nullable Double pitch,
                            @Nullable Double roll) {
            mControlCalls++;
            mControlMode = mode;
            mYaw = yaw;
            mPitch = pitch;
            mRoll = roll;
        }

        @Override
        public boolean startOffsetCorrectionProcess() {
            mStartCorrectionProcessCalls++;
            return true;
        }

        @Override
        public boolean stopOffsetCorrectionProcess() {
            mStopCorrectionProcessCalls++;
            return true;
        }

        @Override
        public boolean startCalibration() {
            mStartCalibrationCalls++;
            return true;
        }

        @Override
        public boolean cancelCalibration() {
            mCancelCalibrationCalls++;
            return true;
        }
    }
}
