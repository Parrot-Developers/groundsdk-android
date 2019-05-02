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

package com.parrot.drone.groundsdk.internal.device.peripheral.tracking;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.TargetTracker;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.FramingSettingMatcher.framingSettingPositionIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static com.parrot.drone.groundsdk.TargetTrajectoryMatcher.targetTrajectoryIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class TargetTrackerTest {

    private MockComponentStore<Peripheral> mStore;

    private TargetTrackerCore mTrackerImpl;

    private TargetTracker mTracker;

    private TargetTrackerCore.Backend mMockBackend;

    private int mComponentChangeCnt;

    @Before
    public void setup() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mMockBackend = mock(TargetTrackerCore.Backend.class);
        mStore = new MockComponentStore<>();
        mTrackerImpl = new TargetTrackerCore(mStore, mMockBackend);
        mTracker = mStore.get(TargetTracker.class);
        mStore.registerObserver(TargetTracker.class, () -> {
            mComponentChangeCnt++;
            mTracker = mStore.get(TargetTracker.class);
        });
        mComponentChangeCnt = 0;
    }

    @After
    public void teardown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mTracker, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mTrackerImpl.publish();
        assertThat(mTracker, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mTrackerImpl.unpublish();
        assertThat(mTracker, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testFraming() {
        mTrackerImpl.publish();

        // test default values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.5, 0.5),
                settingIsUpToDate()));

        // test nothing changes if low-level denies
        doReturn(false).when(mMockBackend).setTargetPosition(anyDouble(), anyDouble());
        mTracker.framing().setPosition(0.75, 0.75);

        assertThat(mComponentChangeCnt, is(1));
        verify(mMockBackend, times(1)).setTargetPosition(0.75, 0.75);
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.5, 0.5),
                settingIsUpToDate()));

        // test nothing changes if values do not change
        doReturn(true).when(mMockBackend).setTargetPosition(anyDouble(), anyDouble());
        mTracker.framing().setPosition(0.5, 0.5);

        assertThat(mComponentChangeCnt, is(1));
        verify(mMockBackend, never()).setTargetPosition(0.5, 0.5);
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.5, 0.5),
                settingIsUpToDate()));

        // test user changes value
        mTracker.framing().setPosition(0.25, 0.25);

        assertThat(mComponentChangeCnt, is(2));
        verify(mMockBackend, times(1)).setTargetPosition(0.25, 0.25);
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.25, 0.25),
                settingIsUpdating()));

        // mock update from low-level
        mTrackerImpl.updateTargetPosition(0.0, 0.0);

        // verify changes are not published until notifyUpdated() is called
        assertThat(mComponentChangeCnt, is((2)));

        mTrackerImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.0, 0.0),
                settingIsUpToDate()));
    }

    @Test
    public void testFramingSettingTimeouts() {
        doReturn(true).when(mMockBackend).setTargetPosition(anyDouble(), anyDouble());
        mTrackerImpl.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.5, 0.5),
                settingIsUpToDate()));

        // mock user sets value
        mTracker.framing().setPosition(0.1, 0.1);

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.1, 0.1),
                settingIsUpdating()));

        // mock backend updates value
        mTrackerImpl.updateTargetPosition(0.1, 0.1);
        mTrackerImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.1, 0.1),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change: setting was updated from backend before
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.1, 0.1),
                settingIsUpToDate()));

        // mock user sets value
        mTracker.framing().setPosition(0.2, 0.2);

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.2, 0.2),
                settingIsUpdating()));

        // mock timeout
        mockSettingTimeout();

        // setting should roll back to previous value
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.1, 0.1),
                settingIsUpToDate()));
    }

    @Test
    public void testControllerTracking() {
        mTrackerImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        mTracker.enableControllerTracking();

        assertThat(mComponentChangeCnt, is(1));
        verify(mMockBackend).enableControllerTracking(true);

        mTrackerImpl.updateControllerTrackingFlag(true);
        assertThat(mComponentChangeCnt, is(1));

        mTrackerImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));

        assertThat(mTracker.isControllerTrackingEnabled(), is(true));

        mTracker.enableControllerTracking();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mTracker.isControllerTrackingEnabled(), is(true));
        verifyNoMoreInteractions(mMockBackend);

        mTracker.disableControllerTracking();

        assertThat(mComponentChangeCnt, is(2));
        verify(mMockBackend).enableControllerTracking(false);

        mTrackerImpl.updateControllerTrackingFlag(false);
        assertThat(mComponentChangeCnt, is(2));

        mTrackerImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mTracker.isControllerTrackingEnabled(), is(false));

        mTracker.disableControllerTracking();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mTracker.isControllerTrackingEnabled(), is(false));
        verifyNoMoreInteractions(mMockBackend);
    }

    @Test
    public void testSendTargetDetectionInfo() {
        mTrackerImpl.publish();
        TargetTracker.TargetDetectionInfo info = mock(TargetTracker.TargetDetectionInfo.class);

        mTracker.sendTargetDetectionInfo(info);

        verify(mMockBackend).sendTargetDetectionInfo(info);
    }

    @Test
    public void testTargetTrajectory() {
        mTrackerImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mTracker.getTargetTrajectory(), nullValue());

        // test update from low-level, no notify, no change expected
        mTrackerImpl.updateTargetTrajectory(1, 2, 3, 4, 5, 6);

        assertThat(mComponentChangeCnt, is(1));

        // notify, expect change
        mTrackerImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mTracker.getTargetTrajectory(), targetTrajectoryIs(1, 2, 3, 4, 5, 6));

        // notify again, expect no change
        mTrackerImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mTracker.getTargetTrajectory(), targetTrajectoryIs(1, 2, 3, 4, 5, 6));

        // test update with same value, no change expected
        mTrackerImpl.updateTargetTrajectory(1, 2, 3, 4, 5, 6).notifyUpdated();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mTracker.getTargetTrajectory(), targetTrajectoryIs(1, 2, 3, 4, 5, 6));

        // test multiple updates, no change expected
        mTrackerImpl.updateTargetTrajectory(7, 8, 9, 10, 11, 12)
                    .updateTargetTrajectory(13, 14, 15, 16, 17, 18);

        assertThat(mComponentChangeCnt, is(2));

        // notify, expect a single change
        mTrackerImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(3));
        assertThat(mTracker.getTargetTrajectory(), targetTrajectoryIs(13, 14, 15, 16, 17, 18));

        // test clear, no notify, no change expected
        mTrackerImpl.clearTargetTrajectory();

        assertThat(mComponentChangeCnt, is(3));

        // notify, expect change
        mTrackerImpl.notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mTracker.getTargetTrajectory(), nullValue());

        // clear again, expect no change
        mTrackerImpl.clearTargetTrajectory().notifyUpdated();

        assertThat(mComponentChangeCnt, is(4));
        assertThat(mTracker.getTargetTrajectory(), nullValue());
    }

    @Test
    public void testCancelRollbacks() {
        doReturn(true).when(mMockBackend).setTargetPosition(anyDouble(), anyDouble());
        mTrackerImpl.publish();

        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.5, 0.5),
                settingIsUpToDate()));

        // mock user changes settings
        mTracker.framing().setPosition(1, 1);

        // cancel all rollbacks
        mTrackerImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(1, 1),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(1, 1),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }
}
