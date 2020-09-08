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

package com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpFlightPlanClient;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCommon;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabling;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingValueIs;
import static com.parrot.drone.groundsdk.ReturnHomeOnDisconnectSettingMatcher.returnHomeOnDisconnectIsImmutable;
import static com.parrot.drone.groundsdk.ReturnHomeOnDisconnectSettingMatcher.returnHomeOnDisconnectIsMutable;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class AnafiFlightPlanPilotingItfTests extends ArsdkEngineTestBase {

    private static final String PLAN_UID_1 = "uuid_1";

    private static final String PLAN_UID_2 = "uuid_2";

    private static final String PLAN_UID_3 = "uuid_3";

    private static final File MOCK_FLIGHTPLAN = new File("/tmp/fp.mavlink");

    private static final String FLIGHT_PLAN_UID = "flightPlanUid";

    private DroneCore mDrone;

    private FlightPlanPilotingItf mPilotingItf;

    private int mChangeCnt;

    @Mock
    private HttpFlightPlanClient mMockUploadClient;

    @Captor
    private ArgumentCaptor<HttpRequest.ResultCallback<String>> mStatusCallbackCaptor;

    @Captor
    private ArgumentCaptor<HttpRequest.ResultCallback<String>> mUploadCallbackCaptor;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        setupDrone();
    }

    @Override
    protected void resetEngine() {
        super.resetEngine();
        setupDrone();
    }

    private void setupDrone() {
        doReturn((HttpRequest) () -> {}).when(mMockUploadClient).uploadFlightPlan(any(), any());
        MockHttpSession.registerOnly(mMockUploadClient);

        // use anafi to mock upload using http
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mPilotingItf = mDrone.getPilotingItfStore().get(mMockSession, FlightPlanPilotingItf.class);
        mDrone.getPilotingItfStore().registerObserver(FlightPlanPilotingItf.class, () -> {
            mPilotingItf = mDrone.getPilotingItfStore().get(mMockSession, FlightPlanPilotingItf.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Override
    public void teardown() {
        MockHttpSession.resetDefaultClients();
        super.teardown();
    }

    @Test
    public void testPublication() {
        // should be unavailable when the drone is not connected and not known
        assertThat(mPilotingItf, is(nullValue()));
        // connect the drone
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock setting reception so that piloting itf is persisted
                ArsdkEncoder.encodeCommonFlightPlanSettingsStateReturnHomeOnDisconnectChanged(0, 0)));

        // interface should be published
        assertThat(mPilotingItf, is(notNullValue()));
        assertThat(mChangeCnt, is(1));
        // disconnect the drone
        disconnectDrone(mDrone, 1);
        // interface should still be there, deactivated
        assertThat(mPilotingItf, is(notNullValue()));
        assertThat(mChangeCnt, is(1));
        // forget the drone
        mDrone.forget();
        // interface should be absent now
        assertThat(mPilotingItf, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testActivation() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                // mock setting reception so that piloting itf is persisted
                .commandReceived(1, ArsdkEncoder.encodeCommonFlightPlanSettingsStateReturnHomeOnDisconnectChanged(0, 0))
                .commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, "",
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN)));

        // should be unavailable
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.NONE));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(1));

        // user upload flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);
        verify(mMockUploadClient).uploadFlightPlan(eq(MOCK_FLIGHTPLAN), mStatusCallbackCaptor.capture());
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mChangeCnt, is(2));
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, FLIGHT_PLAN_UID);

        // should still be inactive until other conditions are ok
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getUnavailabilityReasons(), empty());
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(true));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(3));

        // flight plan preconditions ok
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonFlightPlanStateAvailabilityStateChanged(1));
        // should be idle now
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mChangeCnt, is(4));

        // activate interface
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN), true));
        mPilotingItf.activate(false);

        // notify flightplan started
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(5));

        // deactivate interface
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkPause(), false));
        mPilotingItf.deactivate();

        // notify flightplan paused
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PAUSED, FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(true));
        assertThat(mChangeCnt, is(6));

        // activate interface to resume flightplan
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN), true));
        mPilotingItf.activate(false);

        // notify flightplan resumed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(7));

        // notify pause with a different flight plan file
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PAUSED, "/tmp/another.fp",
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));
        // should be unavailable now, user needs to re-upload flight plan
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(8));

        // even if conditions are ok, user still needs to upload file
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonFlightPlanStateAvailabilityStateChanged(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mChangeCnt, is(8));

        // re-upload file
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStop(), true));
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mChangeCnt, is(9));


        // notify flightplan stop
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, "/tmp/another.fp",
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // now should be idle again
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(10));

        verify(mMockUploadClient, times(2)).uploadFlightPlan(eq(MOCK_FLIGHTPLAN), mStatusCallbackCaptor.capture());
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, FLIGHT_PLAN_UID);

        // upload end
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getUnavailabilityReasons(), empty());
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(true));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(11));

        // activate interface
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN), true));
        mPilotingItf.activate(false);

        // notify flightplan started
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(12));

        // notify flightplan paused
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PAUSED, FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(true));
        assertThat(mChangeCnt, is(13));

        // activate interface to restart flightplan
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStop(), true));
        mPilotingItf.activate(true);

        // notify flightplan stopped, start command should be sent
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN), true));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(14));

        // notify flightplan started
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(15));

        // notify flightplan paused
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PAUSED, FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(true));
        assertThat(mChangeCnt, is(16));

        // values are reset when disconnected
        disconnectDrone(mDrone, 1);
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(17));

        // connecting to drone playing unknown flightplan
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanStateAvailabilityStateChanged(1),
                ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, "/tmp/another.fp",
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN)));

        // should be active
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getUnavailabilityReasons(), empty());
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.NONE));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(19)); // 2 commands received

        // notify flightplan paused
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PAUSED, FLIGHT_PLAN_UID,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mChangeCnt, is(20));
    }

    @Test
    public void testState() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, "",
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN)));

        // interface should be unavailable by default
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));

        // mock drone sends flightplan available state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonFlightPlanStateAvailabilityStateChanged(1));

        // interface should still be unavailable (no flight plan uploaded yet)
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));

        // mock user uploads flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // upload state should change
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        // interface should remain unavailable until upload completes
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_1);

        // now interface should be idle
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));

        // mock drone sends flightplan unavailable state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonFlightPlanStateAvailabilityStateChanged(0));

        // interface should become unavailable
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));

        // mock drone sends flightplan available state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonFlightPlanStateAvailabilityStateChanged(1));

        // interface should become idle again
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));

        // mock user activates interface
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN)));
        assertThat(mPilotingItf.activate(false), is(true));

        // interface should not change until answer from drone
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));

        // mock drone sends flight plan start
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should now be active
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));

        // mock user deactivates interface
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkPause()));
        assertThat(mPilotingItf.deactivate(), is(true));

        // interface should not change until answer from drone
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));

        // mock drone sends flight plan pause
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PAUSED, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should now be idle
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));

        // mock drone sends flight plan start spontaneously
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should now be active
        assertThat(mChangeCnt, is(8));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
    }

    @Test
    public void testUploadState() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, "",
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN)));

        // upload state should be none by default
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.NONE));

        // mock user uploads flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // upload state should be uploading
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_1);

        // upload state should be uploaded
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));

        // mock user uploads again
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient, times(2)).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // upload state should be uploading
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));

        // mock upload error
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 400, null);

        // upload state should be failed
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.FAILED));
    }

    @Test
    public void testLatestMissionItemExecuted() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock setting reception so that piloting itf is persisted
                ArsdkEncoder.encodeCommonFlightPlanSettingsStateReturnHomeOnDisconnectChanged(0, 0)));

        // latest mission item should be -1 by default
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getLatestMissionItemExecuted(), is(-1));

        // mock drone sends flight plan start spontaneously
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getLatestMissionItemExecuted(), is(-1));

        // mock drone sends latest mission item
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMissionItemExecuted(2));

        // latest mission item should be 2
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getLatestMissionItemExecuted(), is(2));

        // mock drone sends flight plan stops
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // latest mission item should remain
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getLatestMissionItemExecuted(), is(2));

        // mock drone sends flight plan start spontaneously
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // latest mission item should be reset to -1
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getLatestMissionItemExecuted(), is(-1));

        // mock drone sends latest mission item
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMissionItemExecuted(10));

        // latest mission item should be 10
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getLatestMissionItemExecuted(), is(10));

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // latest mission item should be reset to -1
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getLatestMissionItemExecuted(), is(-1));
    }

    @Test
    public void testUnavailabilityReasons() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, "",
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN)));

        // unavailability reasons should be MISSING_FLIGHT_PLAN_FILE by default
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE));

        // mock drone sends calibration KO
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanStateComponentStateListChanged(
                        ArsdkFeatureCommon.FlightplanstateComponentstatelistchangedComponent.CALIBRATION, 0));

        // unavailability reasons should update accordingly
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_NOT_CALIBRATED));

        // mock drone sends GPS KO
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanStateComponentStateListChanged(
                        ArsdkFeatureCommon.FlightplanstateComponentstatelistchangedComponent.GPS, 0));

        // unavailability reasons should update accordingly
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_NOT_CALIBRATED,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_GPS_INFO_INACCURATE));

        // mock drone sends take-off KO
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanStateComponentStateListChanged(
                        ArsdkFeatureCommon.FlightplanstateComponentstatelistchangedComponent.TAKEOFF, 0));

        // unavailability reasons should update accordingly
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_NOT_CALIBRATED,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_GPS_INFO_INACCURATE,
                FlightPlanPilotingItf.UnavailabilityReason.CANNOT_TAKE_OFF));

        // mock drone sends camera unavailable
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanStateComponentStateListChanged(
                        ArsdkFeatureCommon.FlightplanstateComponentstatelistchangedComponent.CAMERAAVAILABLE, 0));

        // unavailability reasons should update accordingly
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_NOT_CALIBRATED,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_GPS_INFO_INACCURATE,
                FlightPlanPilotingItf.UnavailabilityReason.CANNOT_TAKE_OFF,
                FlightPlanPilotingItf.UnavailabilityReason.CAMERA_UNAVAILABLE));

        // mock drone sends take-off OK
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanStateComponentStateListChanged(
                        ArsdkFeatureCommon.FlightplanstateComponentstatelistchangedComponent.TAKEOFF, 1));

        // unavailability reasons should update accordingly
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_NOT_CALIBRATED,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_GPS_INFO_INACCURATE,
                FlightPlanPilotingItf.UnavailabilityReason.CAMERA_UNAVAILABLE));

        // mock user uploads flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // state should change to uploading, unavailability reasons should not change until upload succeeds
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_NOT_CALIBRATED,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_GPS_INFO_INACCURATE,
                FlightPlanPilotingItf.UnavailabilityReason.CAMERA_UNAVAILABLE));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_1);

        // unavailability reasons should not contain MISSING_FLIGHT_PLAN_FILE anymore
        assertThat(mChangeCnt, is(8));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_NOT_CALIBRATED,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_GPS_INFO_INACCURATE,
                FlightPlanPilotingItf.UnavailabilityReason.CAMERA_UNAVAILABLE));

        // mock user uploads flight plan again
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient, times(2)).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // state should change to uploading, unavailability reasons should not change until upload fails
        assertThat(mChangeCnt, is(9));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_NOT_CALIBRATED,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_GPS_INFO_INACCURATE,
                FlightPlanPilotingItf.UnavailabilityReason.CAMERA_UNAVAILABLE));

        // mock upload error
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 400, null);

        // unavailability reasons should contain MISSING_FLIGHT_PLAN_FILE again
        assertThat(mChangeCnt, is(10));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.FAILED));
        assertThat(mPilotingItf.getUnavailabilityReasons(), containsInAnyOrder(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_GPS_INFO_INACCURATE,
                FlightPlanPilotingItf.UnavailabilityReason.DRONE_NOT_CALIBRATED,
                FlightPlanPilotingItf.UnavailabilityReason.CAMERA_UNAVAILABLE));

        // mock drone sends flightplan available state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonFlightPlanStateAvailabilityStateChanged(1));

        // all reasons other than MISSING_FLIGHT_PLAN_FILE should be cleared
        assertThat(mChangeCnt, is(11));
        assertThat(mPilotingItf.getUnavailabilityReasons(), contains(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE));

        // mock drone sends flight plan start spontaneously
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // all reasons should be cleared
        assertThat(mChangeCnt, is(12));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getUnavailabilityReasons(), empty());

        // mock drone sends flight plan stop
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // unavailability reasons should contain MISSING_FLIGHT_PLAN_FILE again
        assertThat(mChangeCnt, is(13));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.getUnavailabilityReasons(), contains(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE));

        // mock user uploads flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient, times(3)).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // state should change to uploading, unavailability reasons should not change until upload succeeds
        assertThat(mChangeCnt, is(14));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.getUnavailabilityReasons(), contains(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_1);

        // all reasons should be cleared
        assertThat(mChangeCnt, is(15));
        assertThat(mPilotingItf.getUnavailabilityReasons(), empty());
    }

    @Test
    public void testLatestActivationError() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, "",
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN)));

        // latest activation error should be NONE by default
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getLatestActivationError(), is(FlightPlanPilotingItf.ActivationError.NONE));

        // mock drone sends flightplan available state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonFlightPlanStateAvailabilityStateChanged(1));

        // mock user uploads flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // state should change to uploading
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.getLatestActivationError(), is(FlightPlanPilotingItf.ActivationError.NONE));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_1);

        // state should change to uploaded
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.getLatestActivationError(), is(FlightPlanPilotingItf.ActivationError.NONE));

        // mock user activates interface
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN)));
        assertThat(mPilotingItf.activate(false), is(true));

        // nothing should change until answer from drone
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestActivationError(), is(FlightPlanPilotingItf.ActivationError.NONE));

        // mock mavlink file KO
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanStateComponentStateListChanged(
                        ArsdkFeatureCommon.FlightplanstateComponentstatelistchangedComponent.MAVLINK_FILE, 0));

        // latest activation error should be INCORRECT_FLIGHT_PLAN_FILE
        // TODO: if the drone reports mavlink file KO, we better move back to UNAVAILABLE and force the user to upload
        // TODO  another flight plan
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestActivationError(), is(
                FlightPlanPilotingItf.ActivationError.INCORRECT_FLIGHT_PLAN_FILE));

        // mock geofence KO
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanStateComponentStateListChanged(
                        ArsdkFeatureCommon.FlightplanstateComponentstatelistchangedComponent.WAYPOINTSBEYONDGEOFENCE,
                        0));

        // latest activation error should be WAYPOINT_BEYOND_GEOFENCE
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestActivationError(), is(
                FlightPlanPilotingItf.ActivationError.WAYPOINT_BEYOND_GEOFENCE));

        // mock user activates interface
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN)));
        assertThat(mPilotingItf.activate(false), is(true));

        // latest activation error should be removed immediately
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestActivationError(), is(FlightPlanPilotingItf.ActivationError.NONE));

        // TODO: trying to be ISO with iOS tests here, however, as I see it, Mavlink KO is terminal, it will never
        // TODO  switch to OK spontaneously. So this test better be against GEOFENCE KO/OK which may really happen
        // mock mavlink file KO
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanStateComponentStateListChanged(
                        ArsdkFeatureCommon.FlightplanstateComponentstatelistchangedComponent.MAVLINK_FILE, 0));

        // latest activation error should be INCORRECT_FLIGHT_PLAN_FILE
        // TODO: see above, we should switch to UNAVAILABLE state
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestActivationError(), is(
                FlightPlanPilotingItf.ActivationError.INCORRECT_FLIGHT_PLAN_FILE));

        // mock mavlink file OK
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanStateComponentStateListChanged(
                        ArsdkFeatureCommon.FlightplanstateComponentstatelistchangedComponent.MAVLINK_FILE, 1));

        // latest activation error should be removed
        assertThat(mChangeCnt, is(8));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestActivationError(), is(FlightPlanPilotingItf.ActivationError.NONE));
    }

    @Test
    public void testFlightPlanFileIsKnown() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, "",
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN)));

        // flight plan file should be unknown by default
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));

        // mock user uploads flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // state should change to uploading, flight plan file should remain unknown until success
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_1);

        // flight plan file should be known
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(true));

        // mock user upload flight plan again
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient, times(2)).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // state should change to uploading, flight plan file should remain known (previous file) until failure
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(true));

        // mock upload error
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 400, null);

        // flight plan file should be unknown
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.FAILED));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));

        // mock user upload flight plan again
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient, times(3)).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // state should change to uploading, flight plan file should remain unknown until success
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_1);

        // flight plan file should be known
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(true));

        // mock drone sends unknown flight plan start spontaneously
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, "unknown_plan",
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // flight plan file should be unknown
        assertThat(mChangeCnt, is(8));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));
    }


    @Test
    public void testPauseResumeRestart() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, "",
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN)));

        // paused should be false by default
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flightplan available state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonFlightPlanStateAvailabilityStateChanged(1));

        // mock user uploads flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // state should change to uploading, pause should remain false
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_1);

        // state should change to uploaded, pause should remain false
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock user activates interface, requiring restart
        // since the flight plan is not paused, expect a direct start
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN)));
        assertThat(mPilotingItf.activate(true), is(true));

        // nothing should change until answer from drone
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan start
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should now be active, pause should remain false
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock user deactivates interface
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkPause()));
        assertThat(mPilotingItf.deactivate(), is(true));

        // nothing should change until answer from drone
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan pause
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PAUSED, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should now be idle, pause should be true
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(true));

        // mock user activates interface, not requiring restart (i.e. resume from pause)
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN)));
        assertThat(mPilotingItf.activate(false), is(true));

        // nothing should change until answer from drone
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(true));

        // mock drone sends flight plan start
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should now be active, pause should be false
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan pause spontaneously
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PAUSED, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should now be idle, pause should be true
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(true));

        // mock user activates interface, requiring restart
        // since the flight plan is paused, expect a stop first
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStop()));
        assertThat(mPilotingItf.activate(true), is(true));

        // nothing should change until answer from drone
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(true));

        // mock drone sends flight plan stop
        // since a restart was required, expect a start to follow immediately
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN)))
                      .commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                              ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, PLAN_UID_1,
                              ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should still be idle, but pause should be false
        assertThat(mChangeCnt, is(8));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan start
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should be active, pause should remain false
        assertThat(mChangeCnt, is(9));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan pause spontaneously
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PAUSED, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should be idle, pause should be true
        assertThat(mChangeCnt, is(10));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(true));

        // mock drone sends flight plan stop spontaneously
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should remain idle, but pause should be false
        assertThat(mChangeCnt, is(11));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock user activates interface, requiring restart of current file
        // since the flight plan is not paused, expect a direct start
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN)));
        assertThat(mPilotingItf.activate(true), is(true));

        // nothing should change until answer from drone
        assertThat(mChangeCnt, is(11));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan stop of unknown file
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, "unknown_plan",
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should become unavailable, file should become unknown, pause should remain false
        assertThat(mChangeCnt, is(12));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));

        // mock user activates interface, requiring restart
        // expect denial, since the  file is not known
        assertThat(mPilotingItf.activate(true), is(false));

        // nothing should change
        assertThat(mChangeCnt, is(12));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));
    }

    @Test
    public void testIsPausedAfterUpload() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, "",
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN)));

        // paused should be false by default
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flightplan available state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonFlightPlanStateAvailabilityStateChanged(1));

        // mock user uploads flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // state should change to uploading, pause should remain false
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_1);

        // state should change to uploaded, pause should remain false
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock user activates interface, not requiring restart
        // since the flight plan is not paused, expect a direct start
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN)));
        assertThat(mPilotingItf.activate(false), is(true));

        // nothing should change until answer from drone
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan start
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should now be active, pause should remain false
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan pause spontaneously
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PAUSED, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should now be idle, pause should be true
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(true));

        // mock user uploads another flight plan
        // expect a stop since a previous plan is not stopped
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStop()));
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        // mock drone sends flight plan stop
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        verify(mMockUploadClient, times(2)).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // state should change to uploading, pause should remain true
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.isPaused(), is(true));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_2);

        // state should change to uploaded, pause should be false
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock user activates interface, not requiring restart
        // since the flight plan is not paused, expect a direct start
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(PLAN_UID_2,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN)));
        assertThat(mPilotingItf.activate(false), is(true));

        // nothing should change until answer from drone
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan start
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, PLAN_UID_2,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should now be active, pause should remain false
        assertThat(mChangeCnt, is(8));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock user uploads another flight plan
        // expect a stop since a previous plan is not stopped
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStop()));
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        // upload should not start until drone sends stop state
        verifyNoMoreInteractions(mMockUploadClient);

        // state should change to uploading, interface should remain active, pause should remain false
        assertThat(mChangeCnt, is(9));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan stop
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, PLAN_UID_2,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // upload should start
        verify(mMockUploadClient, times(3)).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // interface should now be idle, pause should remain false
        assertThat(mChangeCnt, is(10));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_3);

        // state should change to uploaded, pause should be false
        assertThat(mChangeCnt, is(11));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.isPaused(), is(false));
    }

    @Test
    public void testUploadSameFlightPlan() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, "",
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN)));

        // paused should be false by default
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flightplan available state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonFlightPlanStateAvailabilityStateChanged(1));

        // mock user uploads flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        verify(mMockUploadClient).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // state should change to uploading, pause should remain false
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_1);

        // state should change to uploaded, pause should remain false
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock user activates interface, not requiring restart
        // since the flight plan is not paused, expect a direct start
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN)));
        assertThat(mPilotingItf.activate(false), is(true));

        // nothing should change until answer from drone
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan start
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should now be active, pause should remain false
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan pause spontaneously
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PAUSED, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should now be idle, pause should be true
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(true));

        // mock user uploads same flight plan again
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStop()));
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);

        // state should change to uploading, pause should remain true
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.isPaused(), is(true));

        // mock drone sends flight plan stop
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        verify(mMockUploadClient, times(2)).uploadFlightPlan(isNotNull(), mUploadCallbackCaptor.capture());

        // state should change to uploading, pause should remain true
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.isPaused(), is(true));

        // mock upload complete
        mUploadCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, PLAN_UID_1);

        // state should change to uploaded, pause should be false
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock user activates interface, requiring restart
        // since the flight plan is stopped, expect a direct start
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonMavlinkStart(PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkStartType.FLIGHTPLAN)));
        assertThat(mPilotingItf.activate(true), is(true));

        // nothing should change until answer from drone
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mPilotingItf.isPaused(), is(false));

        // mock drone sends flight plan playing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.PLAYING, PLAN_UID_1,
                ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN));

        // interface should still be active
        assertThat(mChangeCnt, is(8));
        assertThat(mPilotingItf.getState(), is(Activable.State.ACTIVE));
        assertThat(mPilotingItf.isPaused(), is(false));
    }

    @Test
    public void testUploadError() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCommonMavlinkStateMavlinkFilePlayingStateChanged(
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedState.STOPPED, "",
                        ArsdkFeatureCommon.MavlinkstateMavlinkfileplayingstatechangedType.FLIGHTPLAN)));

        // should be unavailable
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mChangeCnt, is(1));

        // flight plan available should let the state to unavailable
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonFlightPlanStateAvailabilityStateChanged(1));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mChangeCnt, is(1));

        // user upload flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);
        verify(mMockUploadClient, times(1)).uploadFlightPlan(eq(MOCK_FLIGHTPLAN), mStatusCallbackCaptor.capture());
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mChangeCnt, is(2));

        // mock upload failure
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 400, null);
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.FAILED));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mChangeCnt, is(3));

        // user upload flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);
        verify(mMockUploadClient, times(2)).uploadFlightPlan(eq(MOCK_FLIGHTPLAN), mStatusCallbackCaptor.capture());
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mChangeCnt, is(4));

        // mock upload failure
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 415, null);
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.FAILED));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mChangeCnt, is(5));

        // user upload flight plan
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);
        verify(mMockUploadClient, times(3)).uploadFlightPlan(eq(MOCK_FLIGHTPLAN), mStatusCallbackCaptor.capture());
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mChangeCnt, is(6));

        // mock upload failure
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500, null);
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.FAILED));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mChangeCnt, is(7));

        // now send with no error
        mPilotingItf.uploadFlightPlan(MOCK_FLIGHTPLAN);
        verify(mMockUploadClient, times(4)).uploadFlightPlan(eq(MOCK_FLIGHTPLAN), mStatusCallbackCaptor.capture());
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADING));
        assertThat(mPilotingItf.getState(), is(Activable.State.UNAVAILABLE));
        assertThat(mChangeCnt, is(8));

        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, FLIGHT_PLAN_UID);
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.UPLOADED));
        assertThat(mPilotingItf.getState(), is(Activable.State.IDLE));
        assertThat(mChangeCnt, is(9));
    }

    @Test
    public void testReturnHomeOnDisconnect() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock setting reception so that piloting itf is persisted
                ArsdkEncoder.encodeCommonFlightPlanSettingsStateReturnHomeOnDisconnectChanged(0, 1)));

        // setting should be disabled and immutable
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsImmutable(),
                booleanSettingIsDisabled()));

        // mock user enables setting
        mPilotingItf.getReturnHomeOnDisconnect().setEnabled(true);

        // nothing should change since the setting is immutable
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsImmutable(),
                booleanSettingIsDisabled()));

        // mock drone sends setting is mutable
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanSettingsStateReturnHomeOnDisconnectChanged(0, 0));

        // setting should be disabled but mutable
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsMutable(),
                booleanSettingIsDisabled()));

        // mock user enables setting
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.commonFlightPlanSettingsReturnHomeOnDisconnect(1)));
        mPilotingItf.getReturnHomeOnDisconnect().setEnabled(true);

        // setting should be enabling now
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsMutable(),
                booleanSettingIsEnabling()));

        // mock drone sends setting enabled
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanSettingsStateReturnHomeOnDisconnectChanged(1, 0));

        // setting should be enabled now
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsMutable(),
                booleanSettingIsEnabled()));

        // mock drone sends setting disabled spontaneously
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanSettingsStateReturnHomeOnDisconnectChanged(0, 0));

        // setting should be enabled now
        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsMutable(),
                booleanSettingIsDisabled()));

        // mock user enables setting again
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.commonFlightPlanSettingsReturnHomeOnDisconnect(1)));
        mPilotingItf.getReturnHomeOnDisconnect().setEnabled(true);

        // setting should be enabling now
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsMutable(),
                booleanSettingIsEnabling()));

        // mock drone sends setting enabled
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonFlightPlanSettingsStateReturnHomeOnDisconnectChanged(1, 0));

        // setting should be enabled now
        assertThat(mChangeCnt, is(7));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsMutable(),
                booleanSettingIsEnabled()));

        // disconnect drone
        disconnectDrone(mDrone, 1);
        resetEngine();

        // setting should not change
        assertThat(mChangeCnt, is(0));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsMutable(),
                booleanSettingIsEnabled()));

        // mock user disables setting offline
        mPilotingItf.getReturnHomeOnDisconnect().setEnabled(false);

        // setting should be disabled now
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsMutable(),
                booleanSettingIsDisabled()));
    }

    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock setting reception so that piloting itf is persisted
                ArsdkEncoder.encodeCommonFlightPlanSettingsStateReturnHomeOnDisconnectChanged(0, 0)));

        // setting should be disabled and mutable
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsMutable(),
                booleanSettingValueIs(false),
                settingIsUpToDate()));

        // mock user enables setting
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.commonFlightPlanSettingsReturnHomeOnDisconnect(1)));
        mPilotingItf.getReturnHomeOnDisconnect().setEnabled(true);

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsMutable(),
                booleanSettingValueIs(true),
                settingIsUpdating()));

        // disconnect
        disconnectDrone(mDrone, 1);

        // setting should be updated to user value
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingItf.getReturnHomeOnDisconnect(), allOf(
                returnHomeOnDisconnectIsMutable(),
                booleanSettingValueIs(true),
                settingIsUpToDate()));
        assertThat(mPilotingItf.getUnavailabilityReasons(), contains(
                FlightPlanPilotingItf.UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE));
        assertThat(mPilotingItf.getLatestActivationError(), is(FlightPlanPilotingItf.ActivationError.NONE));
        assertThat(mPilotingItf.getLatestUploadState(), is(FlightPlanPilotingItf.UploadState.NONE));
        assertThat(mPilotingItf.isFlightPlanFileKnown(), is(false));
        assertThat(mPilotingItf.isPaused(), is(false));
        assertThat(mPilotingItf.getLatestMissionItemExecuted(), is(-1));
    }
}
