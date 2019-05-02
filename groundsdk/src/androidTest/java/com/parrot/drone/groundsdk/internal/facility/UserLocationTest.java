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

package com.parrot.drone.groundsdk.internal.facility;

import android.location.Location;

import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.facility.UserLocation;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class UserLocationTest {

    private MockComponentStore<Facility> mStore;

    private UserLocationCore mUserLocation;

    private Backend mBackend;

    @Before
    public void setup() {
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mUserLocation = new UserLocationCore(mStore, mBackend);
    }

    @Test
    public void testPublication() {
        mUserLocation.publish();
        assertThat(mUserLocation, is(mStore.get(UserLocation.class)));
        mUserLocation.unpublish();
        assertThat(mStore.get(UserLocation.class), nullValue());
    }

    @Test
    public void testLastKnownLocation() {
        mUserLocation.publish();
        UserLocation userLocation = mStore.get(UserLocation.class);
        assert userLocation != null;
        int[] cnt = new int[1];
        mStore.registerObserver(UserLocation.class, () -> cnt[0]++);

        // test initial value
        assertThat(cnt[0], is(0));
        assertThat(userLocation.lastKnownLocation(), nullValue());

        // update location
        Location location = new Location("provider");
        location.setLatitude(48.8795);
        location.setLongitude(2.3675);
        mUserLocation.updateLocation(location).notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(userLocation.lastKnownLocation(), is(location));
    }

    @Test
    public void testAuthorization() {
        mUserLocation.publish();
        UserLocation userLocation = mStore.get(UserLocation.class);
        assert userLocation != null;
        int[] cnt = new int[1];
        mStore.registerObserver(UserLocation.class, () -> cnt[0]++);

        // test initial value
        assertThat(cnt[0], is(0));
        assertThat(userLocation.isAuthorized(), is(false));

        // update authorization
        mUserLocation.updateAuthorization(true).notifyUpdated();
        assertThat(cnt[0], is(1));
        assertThat(userLocation.isAuthorized(), is(true));
    }

    @Test
    public void testRestartLocationUpdates() {
        mUserLocation.restartLocationUpdates();
        assertThat(mBackend.mLocationUpdatesRestartedCalls, is(1));
    }

    private static final class Backend implements UserLocationCore.Backend {

        int mLocationUpdatesRestartedCalls;

        @Override
        public void startMonitoringLocation() {

        }

        @Override
        public void stopMonitoringLocation() {

        }

        @Override
        public void restartLocationUpdates() {
            mLocationUpdatesRestartedCalls++;
        }
    }
}
