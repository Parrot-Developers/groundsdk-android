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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.ReturnHomePilotingItf;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdk.value.IntegerRange;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabling;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingValueIs;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.IntSettingMatcher.intSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.IntSettingMatcher.intSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.IntSettingMatcher.intSettingValueIs;
import static com.parrot.drone.groundsdk.LocationMatcher.locationIs;
import static com.parrot.drone.groundsdk.LocationMatcher.locationIsUnavailable;
import static com.parrot.drone.groundsdk.OptionalDoubleSettingMatcher.optionalDoubleSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.OptionalDoubleSettingMatcher.optionalDoubleSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.OptionalSettingMatcher.optionalSettingIsUnavailable;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ReturnHomePilotingItfTest {

    private MockComponentStore<PilotingItf> mStore;

    private int mChangeCnt;

    private ReturnHomePilotingItfCore mPilotingItfImpl;

    private ReturnHomePilotingItf mPilotingItf;

    private Backend mBackend;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mChangeCnt = 0;
        mPilotingItf = mStore.get(ReturnHomePilotingItf.class);
        mStore.registerObserver(ReturnHomePilotingItf.class, () -> {
            mPilotingItf = mStore.get(ReturnHomePilotingItf.class);
            mChangeCnt++;
        });
        mBackend = new Backend();
        mPilotingItfImpl = new ReturnHomePilotingItfCore(mStore, mBackend);
    }

    @After
    public void tearDown() {
        mPilotingItfImpl = null;
        mBackend = null;
        mStore = null;
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
    public void testReason() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getReason(), is(ReturnHomePilotingItf.Reason.NONE));

        // change reason
        mPilotingItfImpl.updateReason(ReturnHomePilotingItf.Reason.CONNECTION_LOST).notifyUpdated();
        assertThat(mPilotingItf.getReason(), is(ReturnHomePilotingItf.Reason.CONNECTION_LOST));
        assertThat(mChangeCnt, is(2));

        // change reason
        mPilotingItfImpl.updateReason(ReturnHomePilotingItf.Reason.POWER_LOW).notifyUpdated();
        assertThat(mPilotingItf.getReason(), is(ReturnHomePilotingItf.Reason.POWER_LOW));
        assertThat(mChangeCnt, is(3));

        // change reason
        mPilotingItfImpl.updateReason(ReturnHomePilotingItf.Reason.USER_REQUESTED).notifyUpdated();
        assertThat(mPilotingItf.getReason(), is(ReturnHomePilotingItf.Reason.USER_REQUESTED));
        assertThat(mChangeCnt, is(4));

        // change reason
        mPilotingItfImpl.updateReason(ReturnHomePilotingItf.Reason.FINISHED).notifyUpdated();
        assertThat(mPilotingItf.getReason(), is(ReturnHomePilotingItf.Reason.FINISHED));
        assertThat(mChangeCnt, is(5));

        // update to same reason
        mPilotingItfImpl.updateReason(ReturnHomePilotingItf.Reason.FINISHED).notifyUpdated();
        assertThat(mPilotingItf.getReason(), is(ReturnHomePilotingItf.Reason.FINISHED));
        assertThat(mChangeCnt, is(5));
    }

    @Test
    public void testHomeLocation() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getHomeLocation(), locationIsUnavailable());

        // change home location
        mPilotingItfImpl.updateLocation(22.2, 33.3, 44.4).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getHomeLocation(), locationIs(22.2, 33.3, 44.4));

        // reset home location
        mPilotingItfImpl.resetLocation().notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getHomeLocation(), locationIsUnavailable());
    }

    @Test
    public void testCurrentTarget() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getCurrentTarget(), is(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));

        // change current target
        mPilotingItfImpl.updateCurrentTarget(ReturnHomePilotingItf.Target.CONTROLLER_POSITION).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getCurrentTarget(), is(ReturnHomePilotingItf.Target.CONTROLLER_POSITION));

        // change current target again
        mPilotingItfImpl.updateCurrentTarget(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getCurrentTarget(), is(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));
    }

    @Test
    public void testGpsWasFixedOnTakeOff() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.gpsWasFixedOnTakeOff(), is(false));

        // change gps was fixed on take off
        mPilotingItfImpl.updateGpsFixedOnTakeOff(true).notifyUpdated();
        assertThat(mPilotingItf.gpsWasFixedOnTakeOff(), is(true));
    }

    @Test
    public void testAutoTrigger() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.autoTrigger(), booleanSettingValueIs(false));

        // notify new backend values
        mPilotingItfImpl.autoTrigger().updateValue(true);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.autoTrigger(), booleanSettingValueIs(true));

        // change setting
        mPilotingItf.autoTrigger().toggle();
        assertThat(mChangeCnt, is(3));
        assertThat(mBackend.mSentAutoTriggerSwitchState, is(false));
        assertThat(mPilotingItf.autoTrigger(), booleanSettingIsDisabling());

        // validate change from backend
        mPilotingItfImpl.autoTrigger().updateValue(false);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.autoTrigger(), booleanSettingValueIs(false));
    }

    @Test
    public void testPreferredTarget() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getPreferredTarget(), enumSettingIsUpToDateAt(
                ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));

        // notify new backend values
        mPilotingItfImpl.getPreferredTarget().updateValue(ReturnHomePilotingItf.Target.CONTROLLER_POSITION);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getPreferredTarget(), enumSettingIsUpToDateAt(
                ReturnHomePilotingItf.Target.CONTROLLER_POSITION));

        // change setting
        mPilotingItf.getPreferredTarget().setValue(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION);
        assertThat(mChangeCnt, is(3));
        assertThat(mBackend.mSentPreferredTarget, is(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));
        assertThat(mPilotingItf.getPreferredTarget(), enumSettingIsUpdatingTo(
                ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));

        // validate change from backend
        mPilotingItfImpl.getPreferredTarget().updateValue(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getPreferredTarget(), enumSettingIsUpToDateAt(
                ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));
    }

    @Test
    public void testEndingBehaviour() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getEndingBehavior(), enumSettingIsUpToDateAt(
                ReturnHomePilotingItf.EndingBehavior.HOVERING));

        // notify new backend values
        mPilotingItfImpl.getEndingBehavior().updateValue(ReturnHomePilotingItf.EndingBehavior.LANDING);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getEndingBehavior(), enumSettingIsUpToDateAt(
                ReturnHomePilotingItf.EndingBehavior.LANDING));

        // change setting
        mPilotingItf.getEndingBehavior().setValue(ReturnHomePilotingItf.EndingBehavior.HOVERING);
        assertThat(mChangeCnt, is(3));
        assertThat(mBackend.mSentEndingBehavior, is(ReturnHomePilotingItf.EndingBehavior.HOVERING));
        assertThat(mPilotingItf.getEndingBehavior(), enumSettingIsUpdatingTo(
                ReturnHomePilotingItf.EndingBehavior.HOVERING));

        // validate change from backend
        mPilotingItfImpl.getEndingBehavior().updateValue(ReturnHomePilotingItf.EndingBehavior.HOVERING);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getEndingBehavior(), enumSettingIsUpToDateAt(
                ReturnHomePilotingItf.EndingBehavior.HOVERING));
    }

    @Test
    public void testAutoStartOnDisconnectDelay() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), intSettingIsUpToDateAt(0, 0, 0));

        // notify new backend values
        mPilotingItfImpl.getAutoStartOnDisconnectDelay().updateBounds(IntegerRange.of(10, 120)).updateValue(60);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), intSettingIsUpToDateAt(10, 60, 120));

        // change setting
        mPilotingItf.getAutoStartOnDisconnectDelay().setValue(80);
        assertThat(mChangeCnt, is(3));
        assertThat(mBackend.mSentAutoStartOnDisconnectDelay, is(80));
        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), intSettingIsUpdatingTo(10, 80, 120));

        // validate change from backend
        mPilotingItfImpl.getAutoStartOnDisconnectDelay().updateBounds(IntegerRange.of(10, 120)).updateValue(82);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), intSettingIsUpToDateAt(10, 82, 120));
    }

    @Test
    public void testEndingHoveringAltitude() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalSettingIsUnavailable());

        // notify new backend values
        mPilotingItfImpl.getEndingHoveringAltitude().updateBounds(DoubleRange.of(10, 150)).updateValue(50);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpToDateAt(10, 50, 150));

        // change setting
        mPilotingItf.getEndingHoveringAltitude().setValue(80);
        assertThat(mChangeCnt, is(3));
        assertThat(mBackend.mSentEndingHoveringAltitude, is(80.0));
        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpdatingTo(10, 80, 150));

        // validate change from backend
        mPilotingItfImpl.getEndingHoveringAltitude().updateBounds(DoubleRange.of(10, 150)).updateValue(82);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpToDateAt(10, 82, 150));
    }

    @Test
    public void testMinAltitude() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getMinAltitude(), optionalSettingIsUnavailable());

        // notify new backend values
        mPilotingItfImpl.getMinAltitude().updateBounds(DoubleRange.of(10, 50)).updateValue(20);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpToDateAt(10, 20, 50));

        // change setting
        mPilotingItf.getMinAltitude().setValue(40);
        assertThat(mChangeCnt, is(3));
        assertThat(mBackend.mSentMinAltitude, is(40.0));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpdatingTo(10, 40, 50));

        // validate change from backend
        mPilotingItfImpl.getMinAltitude().updateBounds(DoubleRange.of(10, 50)).updateValue(42);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpToDateAt(10, 42, 50));
    }

    @Test
    public void testNotifyWithoutChanges() {
        mPilotingItfImpl.publish();

        // test notify without changes
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(1));
    }

    @Test
    public void testNotifyWithOneChange() {
        mPilotingItfImpl.publish();

        mPilotingItfImpl.getAutoStartOnDisconnectDelay().updateBounds(IntegerRange.of(10, 120)).updateValue(70);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), intSettingIsUpToDateAt(10, 70, 120));

        mPilotingItfImpl.getPreferredTarget().updateValue(ReturnHomePilotingItf.Target.CONTROLLER_POSITION);
        mPilotingItfImpl.getAutoStartOnDisconnectDelay().updateBounds(IntegerRange.of(10, 120)).updateValue(70);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getPreferredTarget(), enumSettingIsUpToDateAt(
                ReturnHomePilotingItf.Target.CONTROLLER_POSITION));
        assertThat(mPilotingItf.getCurrentTarget(), is(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION));
    }

    @Test
    public void testHomeReachability() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.UNKNOWN));

        // change reachability
        mPilotingItfImpl.updateHomeReachability(ReturnHomePilotingItf.Reachability.CRITICAL).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.CRITICAL));

        // set the same value
        mPilotingItfImpl.updateHomeReachability(ReturnHomePilotingItf.Reachability.CRITICAL).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.CRITICAL));
    }

    @Test
    public void testAutoTriggerDelay() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mPilotingItf.getAutoTriggerDelay(), is(0L));

        // change delay
        mPilotingItfImpl.updateAutoTriggerDelay(60).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getAutoTriggerDelay(), is(60L));

        // set an other delay
        mPilotingItfImpl.updateAutoTriggerDelay(30).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getAutoTriggerDelay(), is(30L));

        // set the same value
        mPilotingItfImpl.updateAutoTriggerDelay(30).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getAutoTriggerDelay(), is(30L));
    }

    @Test
    public void testCancelAutoTrigger() {
        mPilotingItfImpl.publish();

        // when home reachability is different from .warning, backend should not be called
        mPilotingItf.cancelAutoTrigger();
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.UNKNOWN));
        assertThat(mBackend.cancelAutoTriggerCnt, is(0));

        // change reachability
        mPilotingItfImpl.updateHomeReachability(ReturnHomePilotingItf.Reachability.WARNING).notifyUpdated();
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.WARNING));

        mPilotingItf.cancelAutoTrigger();
        assertThat(mPilotingItf.getHomeReachability(), is(ReturnHomePilotingItf.Reachability.WARNING));
        assertThat(mBackend.cancelAutoTriggerCnt, is(1));
    }

    @Test
    public void testSetCustomLocation() {
        mPilotingItfImpl.publish();

        // set preferred target to not custom value from the api
        mPilotingItf.getPreferredTarget().setValue(ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION);
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpdatingTo(ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION));

        // mock update from backend
        mPilotingItfImpl.getPreferredTarget().updateValue(ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpToDateAt(ReturnHomePilotingItf.Target.TRACKED_TARGET_POSITION));

        // when preferred location is different from custom, backend should not be called
        mPilotingItf.setCustomLocation(12.0, 12.0, 0.0);
        assertThat(mBackend.mCustomLocationLatitude, is(0.0));
        assertThat(mBackend.mCustomLocationLongitude, is(0.0));
        assertThat(mBackend.mCustomLocationAltitude, is(0.0));

        // set preferred target to custom value
        mPilotingItfImpl.getPreferredTarget().setValue(ReturnHomePilotingItf.Target.CUSTOM_LOCATION);
        assertThat(mPilotingItf.getPreferredTarget(), enumSettingIsUpdatingTo(
                ReturnHomePilotingItf.Target.CUSTOM_LOCATION));

        // mock update from backend
        mPilotingItfImpl.getPreferredTarget().updateValue(ReturnHomePilotingItf.Target.CUSTOM_LOCATION);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mPilotingItf.getPreferredTarget(),
                enumSettingIsUpToDateAt(ReturnHomePilotingItf.Target.CUSTOM_LOCATION));

        mPilotingItf.setCustomLocation(12.0, 13.0, 14.0);
        assertThat(mBackend.mCustomLocationLatitude, is(12.0));
        assertThat(mBackend.mCustomLocationLongitude, is(13.0));
        assertThat(mBackend.mCustomLocationAltitude, is(14.0));
    }


    @Test
    public void testCancelRollbacks() {
        mPilotingItfImpl.getAutoStartOnDisconnectDelay()
                        .updateBounds(IntegerRange.of(0, 1))
                        .updateValue(0);
        mPilotingItfImpl.getMinAltitude()
                        .updateBounds(DoubleRange.of(10, 50))
                        .updateValue(20);
        mPilotingItfImpl.getEndingHoveringAltitude()
                        .updateBounds(DoubleRange.of(5, 50))
                        .updateValue(20);
        mPilotingItfImpl.publish();

        assertThat(mPilotingItf.autoTrigger(), allOf(
                booleanSettingValueIs(false),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getPreferredTarget(), allOf(
                enumSettingValueIs(ReturnHomePilotingItf.Target.TAKE_OFF_POSITION),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getEndingBehavior(), allOf(
                enumSettingValueIs(ReturnHomePilotingItf.EndingBehavior.HOVERING),
                settingIsUpToDate()
        ));

        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), allOf(
                intSettingValueIs(0, 0, 1),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpToDateAt(5, 20, 50));

        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpToDateAt(10, 20, 50));

        // mock user changes settings
        mPilotingItf.autoTrigger().toggle();
        mPilotingItf.getPreferredTarget().setValue(ReturnHomePilotingItf.Target.CONTROLLER_POSITION);
        mPilotingItfImpl.getEndingBehavior().setValue(ReturnHomePilotingItf.EndingBehavior.LANDING);
        mPilotingItf.getAutoStartOnDisconnectDelay().setValue(1);
        mPilotingItfImpl.getEndingHoveringAltitude().setValue(30);
        mPilotingItf.getMinAltitude().setValue(30);

        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpdatingTo(5, 30, 50));
        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpdatingTo(10, 30, 50));

        // cancel all rollbacks
        mPilotingItfImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mPilotingItf.autoTrigger(), allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getPreferredTarget(), allOf(
                enumSettingValueIs(ReturnHomePilotingItf.Target.CONTROLLER_POSITION),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getEndingBehavior(), allOf(
                enumSettingValueIs(ReturnHomePilotingItf.EndingBehavior.LANDING),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), allOf(
                intSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpToDateAt(5, 30, 50));

        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpToDateAt(10, 30, 50));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mPilotingItf.autoTrigger(), allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getPreferredTarget(), allOf(
                enumSettingValueIs(ReturnHomePilotingItf.Target.CONTROLLER_POSITION),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getEndingBehavior(), allOf(
                enumSettingValueIs(ReturnHomePilotingItf.EndingBehavior.LANDING),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getAutoStartOnDisconnectDelay(), allOf(
                intSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        assertThat(mPilotingItf.getEndingHoveringAltitude(), optionalDoubleSettingIsUpToDateAt(5, 30, 50));

        assertThat(mPilotingItf.getMinAltitude(), optionalDoubleSettingIsUpToDateAt(10, 30, 50));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements ReturnHomePilotingItfCore.Backend {

        boolean mSentAutoTriggerSwitchState;

        int mSentAutoStartOnDisconnectDelay;

        double mSentEndingHoveringAltitude;

        double mSentMinAltitude;

        ReturnHomePilotingItf.Target mSentPreferredTarget;

        ReturnHomePilotingItf.EndingBehavior mSentEndingBehavior;

        int cancelAutoTriggerCnt;

        double mCustomLocationLatitude;

        double mCustomLocationLongitude;

        double mCustomLocationAltitude;

        @Override
        public boolean activate() {
            return false;
        }

        @Override
        public boolean deactivate() {
            return false;
        }

        @Override
        public boolean setAutoTrigger(boolean enabled) {
            mSentAutoTriggerSwitchState = enabled;
            return true;
        }

        @Override
        public boolean setPreferredTarget(@NonNull ReturnHomePilotingItf.Target preferredTarget) {
            mSentPreferredTarget = preferredTarget;
            return true;
        }

        @Override
        public boolean setEndingBehavior(@NonNull ReturnHomePilotingItf.EndingBehavior endingBehavior) {
            mSentEndingBehavior = endingBehavior;
            return true;
        }

        @Override
        public boolean setAutoStartOnDisconnectDelay(int delay) {
            mSentAutoStartOnDisconnectDelay = delay;
            return true;
        }

        @Override
        public boolean setEndingHoveringAltitude(double altitude) {
            mSentEndingHoveringAltitude = altitude;
            return true;
        }

        @Override
        public boolean setMinAltitude(double altitude) {
            mSentMinAltitude = altitude;
            return true;
        }

        @Override
        public void cancelAutoTrigger() {
            cancelAutoTriggerCnt++;
        }

        @Override
        public void setCustomLocation(double latitude, double longitude, double altitude) {
            mCustomLocationLatitude = latitude;
            mCustomLocationLongitude = longitude;
            mCustomLocationAltitude = altitude;
        }
    }
}
