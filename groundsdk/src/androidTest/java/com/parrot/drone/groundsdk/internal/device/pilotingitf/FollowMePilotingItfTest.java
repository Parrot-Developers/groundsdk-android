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

package com.parrot.drone.groundsdk.internal.device.pilotingitf;

import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.FollowMePilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.tracking.TrackingIssue;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingSupports;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public class FollowMePilotingItfTest {

    private MockComponentStore<PilotingItf> mStore;

    private FollowMePilotingItfCore mPilotingItfImpl;

    private FollowMePilotingItf mPilotingItf;

    private FollowMePilotingItfCore.Backend mBackend;

    private int mChangeCnt;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mBackend = mock(FollowMePilotingItfCore.Backend.class);
        mPilotingItfImpl = new FollowMePilotingItfCore(mStore, mBackend);
        mPilotingItf = mStore.get(FollowMePilotingItf.class);
        mStore.registerObserver(FollowMePilotingItf.class, () -> {
            mPilotingItf = mStore.get(FollowMePilotingItf.class);
            mChangeCnt++;
        });
        mChangeCnt = 0;
    }

    @After
    public void tearDown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mChangeCnt, is(0));
        assertThat(mPilotingItf, nullValue());

        mPilotingItfImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf, is(mPilotingItfImpl));

        mPilotingItfImpl.unpublish();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf, nullValue());
    }

    @Test
    public void testState() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));

        // test update from low-level, no notify, no change expected
        mPilotingItfImpl.updateState(Activable.State.IDLE);

        assertThat(mChangeCnt, is(1));

        // notify, expect change
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));

        // notify again, expect no change
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));

        // test update with same value, no change expected
        mPilotingItfImpl.updateState(Activable.State.IDLE).notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));

        // test multiple updates, no change expected
        mPilotingItfImpl.updateState(Activable.State.UNAVAILABLE)
                        .updateState(Activable.State.ACTIVE);

        assertThat(mChangeCnt, is(2));

        // notify, expect a single change
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
    }

    @Test
    public void testAvailabilityIssues() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getAvailabilityIssues(), empty());

        // test update from low-level, no notify, no change expected
        mPilotingItfImpl.updateAvailabilityIssues(EnumSet.of(TrackingIssue.DRONE_NOT_FLYING));

        assertThat(mChangeCnt, is(1));

        // notify, expect change
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getAvailabilityIssues(), containsInAnyOrder(TrackingIssue.DRONE_NOT_FLYING));

        // notify again, expect no change
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getAvailabilityIssues(), containsInAnyOrder(TrackingIssue.DRONE_NOT_FLYING));

        // test update with same value, no change expected
        mPilotingItfImpl.updateAvailabilityIssues(EnumSet.of(TrackingIssue.DRONE_NOT_FLYING)).notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getAvailabilityIssues(), containsInAnyOrder(TrackingIssue.DRONE_NOT_FLYING));

        // test multiple updates, no change expected
        mPilotingItfImpl.updateAvailabilityIssues(EnumSet.of(TrackingIssue.DRONE_NOT_CALIBRATED))
                        .updateAvailabilityIssues(EnumSet.of(
                                TrackingIssue.DRONE_NOT_CALIBRATED,
                                TrackingIssue.DRONE_TOO_CLOSE_TO_GROUND));

        assertThat(mChangeCnt, is(2));

        // notify, expect a single change
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getAvailabilityIssues(), containsInAnyOrder(
                TrackingIssue.DRONE_NOT_CALIBRATED,
                TrackingIssue.DRONE_TOO_CLOSE_TO_GROUND));
    }

    @Test
    public void testQualityIssues() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getQualityIssues(), empty());

        // test update from low-level, no notify, no change expected
        mPilotingItfImpl.updateQualityIssues(EnumSet.of(TrackingIssue.DRONE_NOT_FLYING));

        assertThat(mChangeCnt, is(1));

        // notify, expect change
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getQualityIssues(), containsInAnyOrder(TrackingIssue.DRONE_NOT_FLYING));

        // notify again, expect no change
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getQualityIssues(), containsInAnyOrder(TrackingIssue.DRONE_NOT_FLYING));

        // test update with same value, no change expected
        mPilotingItfImpl.updateQualityIssues(EnumSet.of(TrackingIssue.DRONE_NOT_FLYING)).notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getQualityIssues(), containsInAnyOrder(TrackingIssue.DRONE_NOT_FLYING));

        // test multiple updates, no change expected
        mPilotingItfImpl.updateQualityIssues(EnumSet.of(TrackingIssue.DRONE_NOT_CALIBRATED))
                        .updateQualityIssues(EnumSet.of(
                                TrackingIssue.DRONE_NOT_CALIBRATED,
                                TrackingIssue.DRONE_TOO_CLOSE_TO_GROUND));

        assertThat(mChangeCnt, is(2));

        // notify, expect a single change
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getQualityIssues(), containsInAnyOrder(
                TrackingIssue.DRONE_NOT_CALIBRATED,
                TrackingIssue.DRONE_TOO_CLOSE_TO_GROUND));
    }

    @Test
    public void testActivate() {
        mPilotingItfImpl.publish();

        verifyZeroInteractions(mBackend);
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));

        // deactivate while unavailable, expect no backend call
        assertThat(mPilotingItf.deactivate(), is(false));

        verifyZeroInteractions(mBackend);

        // activate while unavailable, expect no backend call
        assertThat(mPilotingItf.activate(), is(false));

        verifyZeroInteractions(mBackend);

        // mock interface idle
        mPilotingItfImpl.updateState(Activable.State.IDLE).notifyUpdated();
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));

        // deactivate while idle, expect no backend call
        assertThat(mPilotingItf.deactivate(), is(false));

        verifyZeroInteractions(mBackend);

        // activate while idle, expect backend call (mock return false)
        doReturn(false).when(mBackend).activate();
        assertThat(mPilotingItfImpl.activate(), is(false));
        verify(mBackend, times(1)).activate();

        // activate while idle, expect backend call (mock return true)
        doReturn(true).when(mBackend).activate();
        assertThat(mPilotingItfImpl.activate(), is(true));
        verify(mBackend, times(2)).activate();

        // mock interface active
        mPilotingItfImpl.updateState(Activable.State.ACTIVE).notifyUpdated();
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));

        // activate while active, expect no backend call
        assertThat(mPilotingItf.activate(), is(false));
        verifyNoMoreInteractions(mBackend);

        // deactivate while active, expect backend call (mock return false)
        doReturn(false).when(mBackend).deactivate();
        assertThat(mPilotingItfImpl.deactivate(), is(false));
        verify(mBackend, times(1)).deactivate();

        // deactivate while active, expect backend call (mock return true)
        doReturn(true).when(mBackend).deactivate();
        assertThat(mPilotingItfImpl.deactivate(), is(true));
        verify(mBackend, times(2)).deactivate();

        verifyNoMoreInteractions(mBackend);
    }

    @Test
    public void testPiloting() {
        mPilotingItfImpl.publish();

        verifyZeroInteractions(mBackend);

        // test set values
        mPilotingItf.setPitch(1);
        mPilotingItf.setRoll(2);
        mPilotingItf.setVerticalSpeed(3);

        verify(mBackend).setPitch(1);
        verify(mBackend).setRoll(2);
        verify(mBackend).setVerticalSpeed(3);

        // test upper bound clamping

        mPilotingItf.setPitch(101);
        mPilotingItf.setRoll(102);
        mPilotingItf.setVerticalSpeed(103);

        verify(mBackend).setPitch(100);
        verify(mBackend).setRoll(100);
        verify(mBackend).setVerticalSpeed(100);

        // test lower bound clamping

        mPilotingItf.setPitch(-101);
        mPilotingItf.setRoll(-102);
        mPilotingItf.setVerticalSpeed(-103);

        verify(mBackend).setPitch(-100);
        verify(mBackend).setRoll(-100);
        verify(mBackend).setVerticalSpeed(-100);

        verifyNoMoreInteractions(mBackend);
    }

    @Test
    public void testMode() {
        mPilotingItfImpl.publish();

        verifyZeroInteractions(mBackend);

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(FollowMePilotingItf.Mode.class)),
                enumSettingValueIs(FollowMePilotingItf.Mode.GEOGRAPHIC),
                settingIsUpToDate()));

        // test user sets mode, mock backend returns false
        doReturn(false).when(mBackend).setMode(any());
        mPilotingItf.mode().setValue(FollowMePilotingItf.Mode.RELATIVE);

        verify(mBackend, times(1)).setMode(FollowMePilotingItf.Mode.RELATIVE);
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(FollowMePilotingItf.Mode.class)),
                enumSettingValueIs(FollowMePilotingItf.Mode.GEOGRAPHIC),
                settingIsUpToDate()));

        // test user sets mode, mock backend returns true
        doReturn(true).when(mBackend).setMode(any());
        mPilotingItf.mode().setValue(FollowMePilotingItf.Mode.RELATIVE);

        verify(mBackend, times(2)).setMode(FollowMePilotingItf.Mode.RELATIVE);
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(FollowMePilotingItf.Mode.class)),
                enumSettingValueIs(FollowMePilotingItf.Mode.RELATIVE),
                settingIsUpdating()));

        // test update from low-level
        mPilotingItfImpl.mode().updateValue(FollowMePilotingItf.Mode.RELATIVE);
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(FollowMePilotingItf.Mode.class)),
                enumSettingValueIs(FollowMePilotingItf.Mode.RELATIVE),
                settingIsUpToDate()));

        verifyNoMoreInteractions(mBackend);
    }

    @Test
    public void testBehavior() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getCurrentBehavior(), is(FollowMePilotingItf.Behavior.INACTIVE));

        // test update from low-level, no notify, no change expected
        mPilotingItfImpl.updateBehavior(FollowMePilotingItf.Behavior.STATIONARY);

        assertThat(mChangeCnt, is(1));

        // notify, expect change
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getCurrentBehavior(), is(FollowMePilotingItf.Behavior.STATIONARY));

        // notify again, expect no change
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getCurrentBehavior(), is(FollowMePilotingItf.Behavior.STATIONARY));

        // test update with same value, no change expected
        mPilotingItfImpl.updateBehavior(FollowMePilotingItf.Behavior.STATIONARY).notifyUpdated();

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getCurrentBehavior(), is(FollowMePilotingItf.Behavior.STATIONARY));

        // test multiple updates, no change expected
        mPilotingItfImpl.updateBehavior(FollowMePilotingItf.Behavior.INACTIVE)
                        .updateBehavior(FollowMePilotingItf.Behavior.FOLLOWING);

        assertThat(mChangeCnt, is(2));

        // notify, expect a single change
        mPilotingItfImpl.notifyUpdated();

        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getCurrentBehavior(), is(FollowMePilotingItf.Behavior.FOLLOWING));
    }

    @Test
    public void testCancelRollbacks() {
        mPilotingItfImpl.publish();

        assertThat(mPilotingItf.mode(), allOf(
                enumSettingValueIs(FollowMePilotingItf.Mode.GEOGRAPHIC),
                settingIsUpToDate()));

        // mock user changes settings
        mPilotingItf.mode().setValue(FollowMePilotingItf.Mode.RELATIVE);

        // cancel all rollbacks
        mPilotingItfImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mPilotingItf.mode(), allOf(
                enumSettingValueIs(FollowMePilotingItf.Mode.GEOGRAPHIC),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mPilotingItf.mode(), allOf(
                enumSettingValueIs(FollowMePilotingItf.Mode.GEOGRAPHIC),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }
}
