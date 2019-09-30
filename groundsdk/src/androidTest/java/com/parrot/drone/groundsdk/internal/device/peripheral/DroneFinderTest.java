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

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.DroneFinder;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.DiscoveredDroneMatcher.hasName;
import static com.parrot.drone.groundsdk.DiscoveredDroneMatcher.hasRssi;
import static com.parrot.drone.groundsdk.DiscoveredDroneMatcher.hasSecurity;
import static com.parrot.drone.groundsdk.DiscoveredDroneMatcher.hasUid;
import static com.parrot.drone.groundsdk.DiscoveredDroneMatcher.isKnown;
import static com.parrot.drone.groundsdk.DiscoveredDroneMatcher.isModel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class DroneFinderTest {

    private MockComponentStore<Peripheral> mStore;

    private DroneFinderCore mDroneFinderImpl;

    private DroneFinder mDroneFinder;

    private Backend mBackend;

    private int mChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mDroneFinderImpl = new DroneFinderCore(mStore, mBackend);
        mDroneFinder = mStore.get(DroneFinder.class);
        mStore.registerObserver(DroneFinder.class, () -> {
            mChangeCnt++;
            mDroneFinder = mStore.get(DroneFinder.class);
        });
        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mDroneFinder, nullValue());
        assertThat(mChangeCnt, is(0));

        mDroneFinderImpl.publish();
        assertThat(mDroneFinder, is(mDroneFinderImpl));
        assertThat(mChangeCnt, is(1));

        mDroneFinderImpl.unpublish();
        assertThat(mDroneFinder, nullValue());
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testState() {
        mDroneFinderImpl.publish();
        assertThat(mChangeCnt, is(1));

        // test initial value
        assertThat(mDroneFinder.getState(), is(DroneFinder.State.IDLE));

        mDroneFinderImpl.updateState(DroneFinder.State.SCANNING).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mDroneFinder.getState(), is(DroneFinder.State.SCANNING));
    }

    @Test
    public void testDiscoveredDrones() {
        mDroneFinderImpl.publish();
        assertThat(mChangeCnt, is(1));

        // test initial value
        assertThat(mDroneFinder.getDiscoveredDrones(), empty());

        // update list in random order
        mDroneFinderImpl.updateDiscoveredDrones(new DroneFinderCore.DiscoveredDroneCore[] {
                new DroneFinderCore.DiscoveredDroneCore("5", Drone.Model.ANAFI_THERMAL, "Anafi_Thermal_B",
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.SAVED_PASSWORD, -60, true),
                new DroneFinderCore.DiscoveredDroneCore("3", Drone.Model.ANAFI_THERMAL, "Anafi_Thermal_A",
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.NONE, -45, true),
                new DroneFinderCore.DiscoveredDroneCore("2", Drone.Model.ANAFI_4K, "Anafi_B",
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.PASSWORD, -30, false),
                new DroneFinderCore.DiscoveredDroneCore("1", Drone.Model.ANAFI_4K, "Anafi_A",
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.NONE, -30, false),
                new DroneFinderCore.DiscoveredDroneCore("4", Drone.Model.ANAFI_4K, "Anafi_C",
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.NONE, -60, false)
        }).notifyUpdated();

        // assert that list is sorted by signal level first, then name
        assertThat(mChangeCnt, is(2));
        assertThat(mDroneFinder.getDiscoveredDrones(), hasSize(5));
        assertThat(mDroneFinder.getDiscoveredDrones(), contains(
                allOf(hasUid("1"), isModel(Drone.Model.ANAFI_4K), hasName("Anafi_A"),
                        hasSecurity(DroneFinder.DiscoveredDrone.ConnectionSecurity.NONE), hasRssi(-30),
                        isKnown(false)),
                allOf(hasUid("2"), isModel(Drone.Model.ANAFI_4K), hasName("Anafi_B"),
                        hasSecurity(DroneFinder.DiscoveredDrone.ConnectionSecurity.PASSWORD), hasRssi(-30),
                        isKnown(false)),
                allOf(hasUid("3"), isModel(Drone.Model.ANAFI_THERMAL), hasName("Anafi_Thermal_A"),
                        hasSecurity(DroneFinder.DiscoveredDrone.ConnectionSecurity.NONE), hasRssi(-45),
                        isKnown(true)),
                allOf(hasUid("4"), isModel(Drone.Model.ANAFI_4K), hasName("Anafi_C"),
                        hasSecurity(DroneFinder.DiscoveredDrone.ConnectionSecurity.NONE), hasRssi(-60),
                        isKnown(false)),
                allOf(hasUid("5"), isModel(Drone.Model.ANAFI_THERMAL), hasName("Anafi_Thermal_B"),
                        hasSecurity(DroneFinder.DiscoveredDrone.ConnectionSecurity.SAVED_PASSWORD), hasRssi(-60),
                        isKnown(true))));

        // clear
        mDroneFinder.clear();
        assertThat(mChangeCnt, is(3));
        assertThat(mDroneFinder.getDiscoveredDrones(), empty());
    }

    @Test
    public void testRefresh() {
        mDroneFinderImpl.publish();

        mDroneFinder.refresh();
        assertThat(mBackend.mDiscoverDronesCnt, is(1));
    }

    @Test
    public void testConnect() {
        mDroneFinderImpl.publish();

        // test initial value
        assertThat(mDroneFinder.getDiscoveredDrones(), empty());

        mDroneFinderImpl.updateDiscoveredDrones(new DroneFinderCore.DiscoveredDroneCore[] {
                new DroneFinderCore.DiscoveredDroneCore("1", Drone.Model.ANAFI_THERMAL, "Anafi Thermal",
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.SAVED_PASSWORD, 4, false),
                new DroneFinderCore.DiscoveredDroneCore("2", Drone.Model.ANAFI_4K, "Anafi 4K",
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.PASSWORD, 3, true)
        }).notifyUpdated();
        assertThat(mDroneFinder.getDiscoveredDrones(), hasSize(2));

        mDroneFinder.connect(mDroneFinder.getDiscoveredDrones().get(0));
        assertThat(mBackend.mConnectDroneCnt, is(1));
        assertThat(mBackend.mConnectDroneUid, is("1"));
        assertThat(mBackend.mConnectDronePassword, nullValue());

        mDroneFinder.connect(mDroneFinder.getDiscoveredDrones().get(1), "password");
        assertThat(mBackend.mConnectDroneCnt, is(2));
        assertThat(mBackend.mConnectDroneUid, is("2"));
        assertThat(mBackend.mConnectDronePassword, is("password"));
    }

    private static final class Backend implements DroneFinderCore.Backend {

        int mDiscoverDronesCnt;

        int mConnectDroneCnt;

        String mConnectDroneUid;

        String mConnectDronePassword;

        @Override
        public void discoverDrones() {
            mDiscoverDronesCnt++;
        }

        @Override
        public boolean connectDrone(@NonNull String uid, @Nullable String password) {
            mConnectDroneCnt++;
            mConnectDroneUid = uid;
            mConnectDronePassword = password;
            return true;
        }
    }
}
