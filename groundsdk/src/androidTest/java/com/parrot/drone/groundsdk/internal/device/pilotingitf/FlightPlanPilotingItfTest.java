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

import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf.ActivationError;
import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf.UnavailabilityReason;
import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf.UploadState;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.flightplan.FlightPlanPilotingItfCore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class FlightPlanPilotingItfTest {

    private MockComponentStore<PilotingItf> mStore;

    private int mChangeCnt;

    private FlightPlanPilotingItfCore mPilotingItfImpl;

    private FlightPlanPilotingItf mPilotingItf;

    private Backend mBackend;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mChangeCnt = 0;
        mStore.registerObserver(FlightPlanPilotingItf.class, () -> {
            mChangeCnt++;
            mPilotingItf = mStore.get(FlightPlanPilotingItf.class);
        });
        mBackend = new Backend();
        mPilotingItfImpl = new FlightPlanPilotingItfCore(mStore, mBackend);
    }

    @After
    public void teardown() {
        mPilotingItfImpl = null;
        mPilotingItf = null;
        mBackend = null;
        mStore = null;
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        mPilotingItfImpl.publish();
        assertThat(mPilotingItfImpl, is(mPilotingItf));
        mPilotingItfImpl.unpublish();
        assertThat(mPilotingItf, nullValue());
    }

    @Test
    public void testUploadFlightPlan() {
        mPilotingItfImpl.publish();

        File flightPlan = new File("/tmp/my_plan.fp");
        mPilotingItf.uploadFlightPlan(flightPlan);
        assertThat(mBackend.mUploadedFlightPlan, equalTo(flightPlan));
    }

    @Test
    public void testActivation() {
        mPilotingItfImpl.publish();

        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));

        // assert that activation has no effect if interface is unavailable
        mPilotingItf.activate(false);
        assertThat(mBackend.mActivateCount, is(0));
        assertThat(mBackend.mRestart, is(false));

        // assert that activation has no effect if interface is active
        mPilotingItfImpl.updateState(Activable.State.ACTIVE);
        mPilotingItf.activate(false);
        assertThat(mBackend.mActivateCount, is(0));
        assertThat(mBackend.mRestart, is(false));

        // assert that activation has effect if the interface is idle
        mPilotingItfImpl.updateState(Activable.State.IDLE);
        mPilotingItf.activate(false);
        assertThat(mBackend.mActivateCount, is(1));
        assertThat(mBackend.mRestart, is(false));

        // assert that activation is called with restart set
        mPilotingItf.activate(true);
        assertThat(mBackend.mActivateCount, is(2));
        assertThat(mBackend.mRestart, is(true));
    }

    @Test
    public void testUnavailabilityReasons() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE));

        // add reason from backend
        mPilotingItfImpl.addUnavailabilityReason(UnavailabilityReason.DRONE_GPS_INFO_INACCURATE).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE, UnavailabilityReason.DRONE_GPS_INFO_INACCURATE));

        // check adding same reason does not trigger a change
        mPilotingItfImpl.addUnavailabilityReason(UnavailabilityReason.DRONE_GPS_INFO_INACCURATE).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE, UnavailabilityReason.DRONE_GPS_INFO_INACCURATE));

        // remove reason from backend
        mPilotingItfImpl.removeUnavailabilityReason(UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                UnavailabilityReason.DRONE_GPS_INFO_INACCURATE));

        // check removing same reason does not trigger a change
        mPilotingItfImpl.removeUnavailabilityReason(UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                UnavailabilityReason.DRONE_GPS_INFO_INACCURATE));
    }

    @Test
    public void testLatestActivationError() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getLatestActivationError(), is(ActivationError.NONE));

        // set activation error from backend
        mPilotingItfImpl.updateActivationError(ActivationError.WAYPOINT_BEYOND_GEOFENCE).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestActivationError(), is(ActivationError.WAYPOINT_BEYOND_GEOFENCE));

        // check setting same error does not trigger a change
        mPilotingItfImpl.updateActivationError(ActivationError.WAYPOINT_BEYOND_GEOFENCE).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestActivationError(), is(ActivationError.WAYPOINT_BEYOND_GEOFENCE));

        // check clearing another error does not trigger a change
        mPilotingItfImpl.clearActivationError(ActivationError.INCORRECT_FLIGHT_PLAN_FILE).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestActivationError(), is(ActivationError.WAYPOINT_BEYOND_GEOFENCE));

        // clear activation error from backend
        mPilotingItfImpl.clearActivationError(ActivationError.WAYPOINT_BEYOND_GEOFENCE).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getLatestActivationError(), is(ActivationError.NONE));
    }

    @Test
    public void testLatestUploadState() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getLatestUploadState(), is(UploadState.NONE));

        // update upload state from backend
        mPilotingItfImpl.updateUploadState(UploadState.UPLOADING).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestUploadState(), is(UploadState.UPLOADING));

        // check updating with same state does not trigger a change
        mPilotingItfImpl.updateUploadState(UploadState.UPLOADING).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestUploadState(), is(UploadState.UPLOADING));
    }

    @Test
    public void testFlightPlanFileKnown() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));

        // update value from backend
        mPilotingItfImpl.updateFlightPlanKnown(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(true));

        // check updating with same value does not trigger a change
        mPilotingItfImpl.updateFlightPlanKnown(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(true));
    }

    @Test
    public void testIsPaused() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.isPaused(), is(false));

        // update value from backend
        mPilotingItfImpl.updatePaused(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.isPaused(), is(true));

        // check updating with same value does not trigger a change
        mPilotingItfImpl.updatePaused(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.isPaused(), is(true));
    }

    @Test
    public void testLatestMissionItemExecuted() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getLatestMissionItemExecuted(), is(-1));

        // update mission item executed from backend
        mPilotingItfImpl.updateMissionItemExecuted(5).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestMissionItemExecuted(), is(5));

        // check updating with same mission item executed does not trigger a change
        mPilotingItfImpl.updateMissionItemExecuted(5).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestMissionItemExecuted(), is(5));
    }

    @Test
    public void testReturnHomeOnDisconnect() {
        mPilotingItfImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isEnabled(), is(false));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isMutable(), is(false));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isUpdating(), is(false));

        // assert user cannot change setting when non mutable
        mPilotingItf.getReturnHomeOnDisconnect().toggle();
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isEnabled(), is(false));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isMutable(), is(false));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isUpdating(), is(false));

        // notify new backend values
        mPilotingItfImpl.getReturnHomeOnDisconnect().updateEnabledFlag(true).updateMutableFlag(true);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isEnabled(), is(true));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isMutable(), is(true));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isUpdating(), is(false));

        // change setting
        mBackend.mSentReturnHomeOnDisconnect = true;
        mPilotingItf.getReturnHomeOnDisconnect().toggle();
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isEnabled(), is(false));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isMutable(), is(true));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isUpdating(), is(true));
        assertThat(mBackend.mSentReturnHomeOnDisconnect, is(false));

        // validate change from backend
        mPilotingItfImpl.getReturnHomeOnDisconnect().updateEnabledFlag(false);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isEnabled(), is(false));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isMutable(), is(true));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect().isUpdating(), is(false));
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

        mPilotingItfImpl.getReturnHomeOnDisconnect().updateEnabledFlag(true);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItfImpl.getReturnHomeOnDisconnect().isEnabled(), is(true));

        mPilotingItfImpl.getReturnHomeOnDisconnect().updateEnabledFlag(false).updateMutableFlag(true);
        mPilotingItfImpl.notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItfImpl.getReturnHomeOnDisconnect().isEnabled(), is(false));
        assertThat(mPilotingItfImpl.getReturnHomeOnDisconnect().isMutable(), is(true));
    }

    @Test
    public void testCancelRollbacks() {
        mPilotingItfImpl.getReturnHomeOnDisconnect()
                        .updateMutableFlag(true)
                        .updateEnabledFlag(false);

        mPilotingItfImpl.publish();

        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                booleanSettingValueIs(false),
                settingIsUpToDate()));

        // mock user changes settings
        mPilotingItf.getReturnHomeOnDisconnect().setEnabled(true);

        // cancel all rollbacks
        mPilotingItfImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                booleanSettingValueIs(true),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements FlightPlanPilotingItfCore.Backend {

        int mActivateCount;

        File mUploadedFlightPlan;

        boolean mRestart;

        boolean mSentReturnHomeOnDisconnect;

        @Override
        public boolean activate() {
            return false;
        }

        @Override
        public boolean deactivate() {
            return false;
        }

        @Override
        public void uploadFlightPlan(@NonNull File flightPlan) {
            mUploadedFlightPlan = flightPlan;
        }

        @Override
        public boolean activate(boolean restart) {
            mRestart = restart;
            mActivateCount++;
            return false;
        }

        @Override
        public boolean setReturnHomeOnDisconnect(boolean enable) {
            mSentReturnHomeOnDisconnect = enable;
            return true;
        }
    }
}
