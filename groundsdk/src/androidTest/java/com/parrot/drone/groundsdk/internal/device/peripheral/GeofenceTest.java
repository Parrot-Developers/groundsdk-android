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

import com.parrot.drone.groundsdk.LocationMatcher;
import com.parrot.drone.groundsdk.device.peripheral.Geofence;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.value.DoubleRange;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.DoubleSettingMatcher.doubleSettingValueIs;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class GeofenceTest {

    private MockComponentStore<Peripheral> mStore;

    private GeofenceCore mGeofenceImpl;

    private Geofence mGeofence;

    private Backend mBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mGeofenceImpl = new GeofenceCore(mStore, mBackend);
        mGeofence = mStore.get(Geofence.class);
        mStore.registerObserver(Geofence.class, () -> {
            mGeofence = mStore.get(Geofence.class);
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
        assertThat(mGeofence, nullValue());

        mGeofenceImpl.publish();

        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGeofence, is(mGeofenceImpl));

        mGeofenceImpl.unpublish();

        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGeofence, nullValue());
    }

    @Test
    public void testMaxAltitude() {
        mGeofenceImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGeofence.maxAltitude(), doubleSettingIsUpToDateAt(0, 0, 0));

        // test update from backend
        mGeofenceImpl.maxAltitude().updateBounds(DoubleRange.of(1, 150)).updateValue(50);
        mGeofenceImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGeofence.maxAltitude(), doubleSettingIsUpToDateAt(1, 50, 150));

        // change setting from the api
        mGeofence.maxAltitude().setValue(30);
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mMaxAltitude, is(30.0));
        assertThat(mGeofence.maxAltitude(), doubleSettingIsUpdatingTo(1, 30, 150));

        // mock update from backend
        mGeofenceImpl.maxAltitude().updateValue(29);
        mGeofenceImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mGeofence.maxAltitude(), doubleSettingIsUpToDateAt(1, 29, 150));
    }

    @Test
    public void testMaxDistance() {
        mGeofenceImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGeofence.maxDistance(), doubleSettingIsUpToDateAt(0, 0, 0));

        // test update from backend
        mGeofenceImpl.maxDistance().updateBounds(DoubleRange.of(10, 2000)).updateValue(500);
        mGeofenceImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGeofence.maxDistance(), doubleSettingIsUpToDateAt(10, 500, 2000));

        // change setting from the api
        mGeofence.maxDistance().setValue(5000);
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mMaxDistance, is(2000.0));
        assertThat(mGeofence.maxDistance(), doubleSettingIsUpdatingTo(10, 2000, 2000));

        // mock update from backend
        mGeofenceImpl.maxDistance().updateValue(1900);
        mGeofenceImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mGeofence.maxDistance(), doubleSettingIsUpToDateAt(10, 1900, 2000));
    }

    @Test
    public void testMode() {
        mGeofenceImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGeofence.mode(), enumSettingIsUpToDateAt(Geofence.Mode.ALTITUDE));

        // change setting from the api
        mGeofence.mode().setValue(Geofence.Mode.CYLINDER);
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mMode, is(Geofence.Mode.CYLINDER));
        assertThat(mGeofence.mode(), enumSettingIsUpdatingTo(Geofence.Mode.CYLINDER));

        // mock update from backend
        mGeofenceImpl.mode().updateValue(Geofence.Mode.CYLINDER);
        mGeofenceImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGeofence.mode(), enumSettingIsUpToDateAt(Geofence.Mode.CYLINDER));

        // change setting from the api
        mGeofence.mode().setValue(Geofence.Mode.ALTITUDE);
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mBackend.mMode, is(Geofence.Mode.ALTITUDE));
        assertThat(mGeofence.mode(), enumSettingIsUpdatingTo(Geofence.Mode.ALTITUDE));

        // mock update from backend
        mGeofenceImpl.mode().updateValue(Geofence.Mode.ALTITUDE);
        mGeofenceImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(5));
        assertThat(mGeofence.mode(), enumSettingIsUpToDateAt(Geofence.Mode.ALTITUDE));
    }

    @Test
    public void testCenter() {
        mGeofenceImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mGeofence.getCenter(), nullValue());

        // update from backend
        mGeofenceImpl.updateCenter(48.8795, 2.3675).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mGeofence.getCenter(), LocationMatcher.locationIs(48.8795, 2.3675));

        // new update from backend
        mGeofenceImpl.updateCenter(48.8888, 2.3675).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGeofence.getCenter(), LocationMatcher.locationIs(48.8888, 2.3675));

        // check updating with same values does not trigger a change
        mGeofenceImpl.updateCenter(48.8888, 2.3675).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mGeofence.getCenter(), LocationMatcher.locationIs(48.8888, 2.3675));

        // reset from backend
        mGeofenceImpl.resetCenter().notifyUpdated();
        assertThat(mComponentChangeCnt, is(4));
        assertThat(mGeofence.getCenter(), nullValue());
    }

    @Test
    public void testCancelRollbacks() {
        mGeofenceImpl.maxAltitude()
                     .updateBounds(DoubleRange.of(0, 1))
                     .updateValue(0);
        mGeofenceImpl.maxDistance()
                     .updateBounds(DoubleRange.of(0, 1))
                     .updateValue(0);
        mGeofenceImpl.publish();

        assertThat(mGeofence.mode(), allOf(
                enumSettingValueIs(Geofence.Mode.ALTITUDE),
                settingIsUpToDate()));

        assertThat(mGeofence.maxAltitude(), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));

        assertThat(mGeofence.maxDistance(), allOf(
                doubleSettingValueIs(0, 0, 1),
                settingIsUpToDate()));

        // mock user changes settings
        mGeofence.mode().setValue(Geofence.Mode.CYLINDER);
        mGeofence.maxAltitude().setValue(1);
        mGeofence.maxDistance().setValue(1);

        // cancel all rollbacks
        mGeofenceImpl.cancelSettingsRollbacks();

        // all settings should be updated to user values
        assertThat(mGeofence.mode(), allOf(
                enumSettingValueIs(Geofence.Mode.CYLINDER),
                settingIsUpToDate()));

        assertThat(mGeofence.maxAltitude(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        assertThat(mGeofence.maxDistance(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        // mock timeout
        mockSettingTimeout();

        // nothing should change
        assertThat(mGeofence.mode(), allOf(
                enumSettingValueIs(Geofence.Mode.CYLINDER),
                settingIsUpToDate()));

        assertThat(mGeofence.maxAltitude(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));

        assertThat(mGeofence.maxDistance(), allOf(
                doubleSettingValueIs(0, 1, 1),
                settingIsUpToDate()));
    }

    private static void mockSettingTimeout() {
        TestExecutor.mockTimePasses(5, TimeUnit.SECONDS);
    }

    private static final class Backend implements GeofenceCore.Backend {

        private double mMaxAltitude;

        private double mMaxDistance;

        private Geofence.Mode mMode;

        @Override
        public boolean setMaxAltitude(double altitude) {
            mMaxAltitude = altitude;
            return true;
        }

        @Override
        public boolean setMaxDistance(double distance) {
            mMaxDistance = distance;
            return true;
        }

        @Override
        public boolean setMode(@NonNull Geofence.Mode mode) {
            mMode = mode;
            return true;
        }
    }
}
