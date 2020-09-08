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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.TargetTracker;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.utility.SystemBarometer;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static com.parrot.drone.groundsdk.FramingSettingMatcher.framingSettingPositionIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static com.parrot.drone.groundsdk.TargetTrajectoryMatcher.targetTrajectoryIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class AnafiTargetTrackerTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private TargetTracker mTracker;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();

        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mTracker = mDrone.getPeripheralStore().get(mMockSession, TargetTracker.class);
        mDrone.getPeripheralStore().registerObserver(TargetTracker.class, () -> {
            mTracker = mDrone.getPeripheralStore().get(mMockSession, TargetTracker.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // test that tracker is not available when not connected and not known
        assertThat(mChangeCnt, is(0));
        assertThat(mTracker, nullValue());

        // test that tracker becomes available after connection
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mTracker, notNullValue());

        // test that tracker is still available after disconnection
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mTracker, notNullValue());

        // test that tracker becomes unavailable after forget
        mDrone.forget();

        assertThat(mChangeCnt, is(2));
        assertThat(mTracker, nullValue());
    }

    @Test
    public void testFraming() {
        // make the drone 'known' but disconnected
        connectDrone(mDrone, 1);
        disconnectDrone(mDrone, 1);

        // test initial values
        assertThat(mChangeCnt, is(1));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.5, 0.5),
                settingIsUpToDate()));

        // test change while disconnected
        mTracker.framing().setPosition(1, 1);

        assertThat(mChangeCnt, is(2));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(1, 1),
                settingIsUpToDate()));

        // test values received at connection are ignored (we force the setting's values instead)
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeFollowMeTargetFramingPositionChanged(25, 25))
                .expect(new Expectation.Command(1, ExpectedCmd.followMeTargetFramingPosition(100, 100))));

        assertThat(mChangeCnt, is(2));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(1, 1),
                settingIsUpToDate()));

        // test change from component is sent
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.followMeTargetFramingPosition(0, 0)));
        mTracker.framing().setPosition(0, 0);

        assertThat(mChangeCnt, is(3));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0, 0),
                settingIsUpdating()));

        // test update from drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeTargetFramingPositionChanged(75, 75));

        assertThat(mChangeCnt, is(4));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.75, 0.75),
                settingIsUpToDate()));
    }

    @Test
    public void testSendTargetDetectionInfo() {
        connectDrone(mDrone, 1);

        TargetTracker.TargetDetectionInfo info = mock(TargetTracker.TargetDetectionInfo.class);
        doReturn(1.0).when(info).getTargetAzimuth();
        doReturn(2.0).when(info).getTargetElevation();
        doReturn(3.0).when(info).getChangeOfScale();
        doReturn(true).when(info).isNewTarget();
        doReturn(0.5).when(info).getConfidenceLevel();
        doReturn(4L).when(info).getTimestamp();

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.followMeTargetImageDetection(
                1.0f, 2.0f, 3.0f, 128, 1, 4L)));
        mTracker.sendTargetDetectionInfo(info);
    }

    @Test
    public void testControllerTracking() {
        ArgumentCaptor<SystemBarometer.Monitor> monitorCaptor = ArgumentCaptor.forClass(SystemBarometer.Monitor.class);
        ArgumentCaptor<Object> enforcementTokenCaptor = ArgumentCaptor.forClass(Object.class);
        SystemBarometer.Monitor barometerMonitor;
        Object enforcementToken;

        // at first, tracking should be disabled, monitoring should not have started anyhow
        verifyZeroInteractions(mMockLocation);

        // we assume that targetIsController is 0 drone side.
        // test that targetIsController command is not sent after connection.
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeFollowMeTargetIsController(0)));

        assertThat(mChangeCnt, is(1));

        // verify that WIFI location is not forced
        verify(mMockLocation, never()).enforceWifiUsage(any());

        // user enables target tracking
        // test that targetIsController command is sent
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.followMeSetTargetIsController(1)));

        mTracker.enableControllerTracking();

        // user value should not be updated until ack from drone
        assertThat(mChangeCnt, is(1));
        assertThat(mTracker.isControllerTrackingEnabled(), is(false));

        // mock setTargetIsController acknowledge from drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeTargetIsController(1));

        assertThat(mChangeCnt, is(2));
        assertThat(mTracker.isControllerTrackingEnabled(), is(true));

        // verify that WIFI location is now forced
        verify(mMockLocation).enforceWifiUsage(enforcementTokenCaptor.capture());

        enforcementToken = enforcementTokenCaptor.getValue();
        assertThat(enforcementToken, notNullValue());

        // test that all monitoring stops on disconnect
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(2));
        assertThat(mTracker.isControllerTrackingEnabled(), is(true));

        // verify that WIFI location forcing is revoked
        verify(mMockLocation).revokeWifiUsageEnforcement(enforcementToken);

        // test that monitoring is restarted after reconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeFollowMeTargetIsController(1)));

        assertThat(mChangeCnt, is(2));
        assertThat(mTracker.isControllerTrackingEnabled(), is(true));

        // verify that WIFI location is now forced
        verify(mMockLocation, times(2)).enforceWifiUsage(enforcementTokenCaptor.capture());

        enforcementToken = enforcementTokenCaptor.getValue();
        assertThat(enforcementToken, notNullValue());

        // user disables target tracking
        // test that targetIsController command is sent
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.followMeSetTargetIsController(0)));

        mTracker.disableControllerTracking();

        // user value should not be updated until ack from drone
        assertThat(mChangeCnt, is(2));
        assertThat(mTracker.isControllerTrackingEnabled(), is(true));

        // mock setTargetIsController acknowledge from drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeTargetIsController(0));

        assertThat(mChangeCnt, is(3));
        assertThat(mTracker.isControllerTrackingEnabled(), is(false));

        // verify that WIFI location forcing is revoked
        verify(mMockLocation, times(2)).revokeWifiUsageEnforcement(enforcementToken);

        // test enabling tracking while disconnected
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(3));
        assertThat(mTracker.isControllerTrackingEnabled(), is(false));

        // verify that monitoring is not started (yet)  when disconnected
        mTracker.enableControllerTracking();

        // while disconnected, user value should update immediately
        assertThat(mChangeCnt, is(4));
        assertThat(mTracker.isControllerTrackingEnabled(), is(true));

        // verify that monitoring is not started
        verify(mMockLocation, times(2)).enforceWifiUsage(enforcementTokenCaptor.capture());

        // test that setTargetController is send automatically after connection
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeFollowMeTargetIsController(0))
                .expect(new Expectation.Command(1, ExpectedCmd.followMeSetTargetIsController(1))));

        // mock setTargetIsController acknowledge from drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeTargetIsController(1));

        assertThat(mChangeCnt, is(4));
        assertThat(mTracker.isControllerTrackingEnabled(), is(true));

        // verify that WIFI location is now forced
        verify(mMockLocation, times(3)).enforceWifiUsage(enforcementTokenCaptor.capture());

        // test disabling tracking while not connected
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(4));
        assertThat(mTracker.isControllerTrackingEnabled(), is(true));

        // verify that WIFI location forcing is revoked
        verify(mMockLocation, times(3)).revokeWifiUsageEnforcement(enforcementToken);

        mTracker.disableControllerTracking();

        // while disconnected, user value should update immediately
        assertThat(mChangeCnt, is(5));
        assertThat(mTracker.isControllerTrackingEnabled(), is(false));

        // test that setTargetController is send automatically after connection
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeFollowMeTargetIsController(1))
                .expect(new Expectation.Command(1, ExpectedCmd.followMeSetTargetIsController(0))));

        // mock setTargetIsController acknowledge from drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeTargetIsController(0));

        assertThat(mChangeCnt, is(5));
        assertThat(mTracker.isControllerTrackingEnabled(), is(false));

        // verify that monitoring is not started
        verify(mMockLocation, times(3)).enforceWifiUsage(enforcementTokenCaptor.capture());

        // test spurious targetIsController notification from drone (note that this is unexpected, but we should handle
        // it properly anyway)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeTargetIsController(1));

        assertThat(mChangeCnt, is(6));
        assertThat(mTracker.isControllerTrackingEnabled(), is(true));

        // verify that WIFI location is now forced
        verify(mMockLocation, times(4)).enforceWifiUsage(enforcementTokenCaptor.capture());


        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeTargetIsController(0));

        assertThat(mChangeCnt, is(7));
        assertThat(mTracker.isControllerTrackingEnabled(), is(false));

        // verify that WIFI location forcing is revoked
        verify(mMockLocation, times(4)).enforceWifiUsage(enforcementTokenCaptor.capture());
    }

    @Test
    public void testTargetTrajectory() {
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mTracker.getTargetTrajectory(), nullValue());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeFollowMeTargetTrajectory(1d, 2d, 3f, 4f, 5f, 6f));

        assertThat(mChangeCnt, is(2));
        assertThat(mTracker.getTargetTrajectory(), targetTrajectoryIs(1d, 2d, 3f, 4f, 5f, 6f));

        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(3));
        assertThat(mTracker.getTargetTrajectory(), nullValue());
    }

    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(0.5, 0.5),
                settingIsUpToDate()));

        // mock user modifies settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.followMeTargetFramingPosition(100, 100)));
        mTracker.framing().setPosition(1, 1);

        assertThat(mChangeCnt, is(2));
        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(1, 1),
                settingIsUpdating()));

        // disconnect
        disconnectDrone(mDrone, 1);

        // settings should be updated to user value
        assertThat(mChangeCnt, is(3));

        assertThat(mTracker.framing(), allOf(
                framingSettingPositionIs(1, 1),
                settingIsUpToDate()));

        // test other values are reset as they should
        assertThat(mTracker.getTargetTrajectory(), nullValue());
    }
}
