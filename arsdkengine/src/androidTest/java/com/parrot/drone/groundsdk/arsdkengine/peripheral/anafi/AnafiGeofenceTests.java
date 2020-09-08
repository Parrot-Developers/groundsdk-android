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
import com.parrot.drone.groundsdk.device.peripheral.Geofence;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingValueIs;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.LocationMatcher.locationIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiGeofenceTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private Geofence mGeofence;

    private int mChangeCnt;

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
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mGeofence = mDrone.getPeripheralStore().get(mMockSession, Geofence.class);
        mDrone.getPeripheralStore().registerObserver(Geofence.class, () -> {
            mGeofence = mDrone.getPeripheralStore().get(mMockSession, Geofence.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when drone has never been connected
        assertThat(mChangeCnt, is(0));
        assertThat(mGeofence, nullValue());

        // connect drone
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxAltitudeChanged(50, 1, 150)));

        assertThat(mChangeCnt, is(1));
        assertThat(mGeofence, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mGeofence, notNullValue());

        // forget drone
        mDrone.forget();

        assertThat(mChangeCnt, is(2));
        assertThat(mGeofence, nullValue());
    }

    @Test
    public void testMaxAltitude() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxAltitudeChanged(10, 5, 50)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mGeofence.maxAltitude(), doubleSettingIsUpToDateAt(5, 10, 50));

        // change value from api
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingSettingsMaxAltitude(8.3f)));
        mGeofence.maxAltitude().setValue(8.3);
        assertThat(mChangeCnt, is(2));
        assertThat(mGeofence.maxAltitude(), doubleSettingIsUpdatingTo(5, 8.3, 50));

        // update value from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxAltitudeChanged(9.4f, 5, 50));
        assertThat(mChangeCnt, is(3));
        assertThat(mGeofence.maxAltitude(), doubleSettingIsUpToDateAt(5, 9.4f, 50));

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check setting is restored to latest user set value
        assertThat(mChangeCnt, is(0));
        assertThat(mGeofence.maxAltitude(), doubleSettingIsUpToDateAt(5, 8.3, 50));

        // change setting offline
        mGeofence.maxAltitude().setValue(12.2);
        assertThat(mChangeCnt, is(1));
        assertThat(mGeofence.maxAltitude(), doubleSettingIsUpToDateAt(5, 12.2, 50));

        // reconnect
        connectDrone(mDrone, 1, () -> {
            // received current drone setting differs from what is saved
            mMockArsdkCore.commandReceived(1,
                    ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxAltitudeChanged(1, 1, 20));
            // connect should send the saved setting
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.ardrone3PilotingSettingsMaxAltitude(12.2f), true));
        });

        // check range is updated but value is not
        assertThat(mChangeCnt, is(2));
        assertThat(mGeofence.maxAltitude(), doubleSettingIsUpToDateAt(1, 12.2, 20));
    }

    @Test
    public void testMaxDistance() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxDistanceChanged(500, 10, 2000)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mGeofence.maxDistance(), doubleSettingIsUpToDateAt(10, 500, 2000));

        // change value from api
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingSettingsMaxDistance(56.7f)));
        mGeofence.maxDistance().setValue(56.7);
        assertThat(mChangeCnt, is(2));
        assertThat(mGeofence.maxDistance(), doubleSettingIsUpdatingTo(10, 56.7, 2000));

        // update value from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxDistanceChanged(67.8f, 10, 2000));
        assertThat(mChangeCnt, is(3));
        assertThat(mGeofence.maxDistance(), doubleSettingIsUpToDateAt(10, 67.8f, 2000));

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check setting is restored to latest user set value
        assertThat(mChangeCnt, is(0));
        assertThat(mGeofence.maxDistance(), doubleSettingIsUpToDateAt(10, 56.7, 2000));

        // change setting offline
        mGeofence.maxDistance().setValue(153.4);
        assertThat(mChangeCnt, is(1));
        assertThat(mGeofence.maxDistance(), doubleSettingIsUpToDateAt(10, 153.4, 2000));

        // reconnect
        connectDrone(mDrone, 1, () -> {
            // received current drone setting differs from what is saved
            mMockArsdkCore.commandReceived(1,
                    ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxDistanceChanged(20, 20, 500));
            // connect should send the saved setting
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.ardrone3PilotingSettingsMaxDistance(153.4f), true));
        });

        // check setting range is updated
        assertThat(mChangeCnt, is(2));
        assertThat(mGeofence.maxDistance(), doubleSettingIsUpToDateAt(20, 153.4, 500));
    }

    @Test
    public void testMode() {
        // connect drone, receiving drone settings (including max distance to keep component published on disconnection)
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateNoFlyOverMaxDistanceChanged(0),
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxDistanceChanged(500, 10, 2000)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mGeofence.mode(), enumSettingIsUpToDateAt(Geofence.Mode.ALTITUDE));

        // change value from api
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ardrone3PilotingSettingsNoFlyOverMaxDistance(1)));
        mGeofence.mode().setValue(Geofence.Mode.CYLINDER);
        assertThat(mChangeCnt, is(2));
        assertThat(mGeofence.mode(), enumSettingIsUpdatingTo(Geofence.Mode.CYLINDER));

        // update value from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateNoFlyOverMaxDistanceChanged(1));
        assertThat(mChangeCnt, is(3));
        assertThat(mGeofence.mode(), enumSettingIsUpToDateAt(Geofence.Mode.CYLINDER));

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check setting didn't change
        assertThat(mChangeCnt, is(0));
        assertThat(mGeofence.mode(), enumSettingIsUpToDateAt(Geofence.Mode.CYLINDER));

        // change setting offline
        mGeofence.mode().setValue(Geofence.Mode.ALTITUDE);
        assertThat(mChangeCnt, is(1));
        assertThat(mGeofence.mode(), enumSettingIsUpToDateAt(Geofence.Mode.ALTITUDE));

        // reconnect
        connectDrone(mDrone, 1, () -> {
            // received current drone setting differs from what is saved
            mMockArsdkCore.commandReceived(1,
                    ArsdkEncoder.encodeArdrone3PilotingSettingsStateNoFlyOverMaxDistanceChanged(1));
            // connect should send the saved setting
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.ardrone3PilotingSettingsNoFlyOverMaxDistance(0), true));
        });

        // check setting didn't change
        assertThat(mChangeCnt, is(1));
        assertThat(mGeofence.mode(), enumSettingIsUpToDateAt(Geofence.Mode.ALTITUDE));
    }

    @Test
    public void testCenter() {
        // connect drone, receiving drone settings (including max distance to keep component published on disconnection)
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxDistanceChanged(500, 10, 2000)));

        assertThat(mChangeCnt, is(1));
        assertThat(mGeofence.getCenter(), nullValue());

        // ensure that position (500, 500) is ignored
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3GPSSettingsStateGeofenceCenterChanged(500, 500));
        assertThat(mGeofence.getCenter(), nullValue());

        // change geofence center from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3GPSSettingsStateGeofenceCenterChanged(48.8795, 2.3675));
        assertThat(mChangeCnt, is(2));
        assertThat(mGeofence.getCenter(), locationIs(48.8795, 2.3675));

        // change geofence center latitude from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3GPSSettingsStateGeofenceCenterChanged(48.8888, 2.3675));
        assertThat(mChangeCnt, is(3));
        assertThat(mGeofence.getCenter(), locationIs(48.8888, 2.3675));

        // change geofence center longitude from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3GPSSettingsStateGeofenceCenterChanged(48.8888, 2.3333));
        assertThat(mChangeCnt, is(4));
        assertThat(mGeofence.getCenter(), locationIs(48.8888, 2.3333));

        // disconnect
        disconnectDrone(mDrone, 1);

        // geofence center should remain (we keep it so that the UI can keep the previous center while reconnecting)
        assertThat(mChangeCnt, is(4));
        assertThat(mGeofence.getCenter(), locationIs(48.8888, 2.3333));
    }

    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateNoFlyOverMaxDistanceChanged(0),
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxDistanceChanged(0, 0, 1),
                ArsdkEncoder.encodeArdrone3PilotingSettingsStateMaxAltitudeChanged(0, 0, 1)));

        assertThat(mChangeCnt, is(1));
        assertThat(mGeofence.mode(), allOf(
                enumSettingValueIs(Geofence.Mode.ALTITUDE),
                settingIsUpToDate()));

        assertThat(mGeofence.maxDistance(), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));

        assertThat(mGeofence.maxAltitude(), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));

        // mock user modifies settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingSettingsNoFlyOverMaxDistance(1)));
        mGeofence.mode().setValue(Geofence.Mode.CYLINDER);

        assertThat(mChangeCnt, is(2));
        assertThat(mGeofence.mode(), allOf(
                enumSettingValueIs(Geofence.Mode.CYLINDER),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingSettingsMaxDistance(1)));
        mGeofence.maxDistance().setValue(1);

        assertThat(mChangeCnt, is(3));
        assertThat(mGeofence.maxDistance(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpdating()));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3PilotingSettingsMaxAltitude(1)));
        mGeofence.maxAltitude().setValue(1);

        assertThat(mChangeCnt, is(4));
        assertThat(mGeofence.maxAltitude(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpdating()));

        // disconnect
        disconnectDrone(mDrone, 1);

        // setting should be updated to user value
        assertThat(mChangeCnt, is(5));

        assertThat(mGeofence.mode(), allOf(
                enumSettingValueIs(Geofence.Mode.CYLINDER),
                settingIsUpToDate()));

        assertThat(mGeofence.maxDistance(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        assertThat(mGeofence.maxAltitude(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));
    }
}
