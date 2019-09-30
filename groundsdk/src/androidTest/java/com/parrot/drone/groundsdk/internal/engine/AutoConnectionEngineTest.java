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

package com.parrot.drone.groundsdk.internal.engine;

import android.content.Context;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.facility.AutoConnection;
import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.DeviceConnectorCore;
import com.parrot.drone.groundsdk.internal.device.DeviceCore;
import com.parrot.drone.groundsdk.internal.device.DeviceStoreCore;
import com.parrot.drone.groundsdk.internal.device.MockDevice;
import com.parrot.drone.groundsdk.internal.device.MockDrone;
import com.parrot.drone.groundsdk.internal.device.MockRC;
import com.parrot.drone.groundsdk.internal.utility.DeviceStore;
import com.parrot.drone.groundsdk.internal.utility.DroneStore;
import com.parrot.drone.groundsdk.internal.utility.RemoteControlStore;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;

import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.DeviceStateMatcher.stateIs;
import static com.parrot.drone.groundsdk.DroneMatcher.droneProxy;
import static com.parrot.drone.groundsdk.RemoteControlMatcher.rcProxy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

public class AutoConnectionEngineTest {

    private DroneStore mDroneStore;

    private RemoteControlStore mRCStore;

    private AutoConnectionEngine mEngine;

    private AutoConnection mAutoConnection;

    private int mChangeCnt;

    @Before
    public void setUp() {
        GroundSdkConfig.loadDefaults();

        MockComponentStore<Facility> facilityStore = new MockComponentStore<>();
        mDroneStore = new DeviceStoreCore.Drone();
        mRCStore = new DeviceStoreCore.RemoteControl();
        mEngine = new AutoConnectionEngine(MockEngineController.create(mock(Context.class),
                new UtilityRegistry()
                        .registerUtility(DroneStore.class, mDroneStore)
                        .registerUtility(RemoteControlStore.class, mRCStore),
                facilityStore));

        facilityStore.registerObserver(AutoConnection.class, () -> {
            mAutoConnection = facilityStore.get(AutoConnection.class);
            mChangeCnt++;
        });
        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the engine is not started
        assertThat(mAutoConnection, nullValue());
        assertThat(mChangeCnt, is(0));

        mEngine.start();

        assertThat(mAutoConnection, notNullValue());
        assertThat(mChangeCnt, is(1));

        mEngine.requestStop(null);

        assertThat(mAutoConnection, nullValue());
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testAutoConnectionState() {
        GroundSdkConfig.get().autoConnectAtStartup(false);

        assertThat(mChangeCnt, is(0));
        assertThat(mAutoConnection, nullValue());

        mEngine.start();

        // when engine is started, state should be stopped
        assertThat(mChangeCnt, is(1));
        assertThat(mAutoConnection, notNullValue());
        assertThat(mAutoConnection.getStatus(), is(AutoConnection.Status.STOPPED));

        // test that stopping auto-connection while stopped does not change anything
        assertThat(mAutoConnection.stop(), is(false));

        assertThat(mChangeCnt, is(1));
        assertThat(mAutoConnection, notNullValue());
        assertThat(mAutoConnection.getStatus(), is(AutoConnection.Status.STOPPED));

        // test that starting auto-connection while started actually starts auto-connection
        assertThat(mAutoConnection.start(), is(true));

        assertThat(mChangeCnt, is(2));
        assertThat(mAutoConnection, notNullValue());
        assertThat(mAutoConnection.getStatus(), is(AutoConnection.Status.STARTED));

        // test that starting auto-connection while started does not change anything
        assertThat(mAutoConnection.start(), is(false));

        assertThat(mChangeCnt, is(2));
        assertThat(mAutoConnection, notNullValue());
        assertThat(mAutoConnection.getStatus(), is(AutoConnection.Status.STARTED));

        // test that stopping auto-connection while stopped actually stops auto-connection
        assertThat(mAutoConnection.stop(), is(true));

        assertThat(mChangeCnt, is(3));
        assertThat(mAutoConnection, notNullValue());
        assertThat(mAutoConnection.getStatus(), is(AutoConnection.Status.STOPPED));
    }

    @Test
    public void testAutoStartWhenConfigured() {
        GroundSdkConfig.get().autoConnectAtStartup(true);

        assertThat(mAutoConnection, nullValue());
        assertThat(mChangeCnt, is(0));

        mEngine.start();

        // test that auto-connection is started automatically (and notifies only once)
        assertThat(mAutoConnection, notNullValue());
        assertThat(mChangeCnt, is(1));
        assertThat(mAutoConnection.getStatus(), is(AutoConnection.Status.STARTED));

        // test that stopping auto-connection while started actually stops auto-connection
        assertThat(mAutoConnection.stop(), is(true));

        assertThat(mAutoConnection, notNullValue());
        assertThat(mChangeCnt, is(2));
        assertThat(mAutoConnection.getStatus(), is(AutoConnection.Status.STOPPED));
    }

    @Test
    public void testDroneAutoConnectWithTwoWifiDrones() {
        MockDrone[] drones = addIn(mDroneStore,
                new MockDrone("1").addConnectors(DeviceConnectorCore.LOCAL_WIFI),
                new MockDrone("2").addConnectors(DeviceConnectorCore.LOCAL_WIFI));

        assertThat(mChangeCnt, is(0));

        mEngine.start();

        assertThat(mChangeCnt, is(1)); // +1 for publish
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // prepare expectations for auto-connection
        drones[0].expectConnectOn(DeviceConnectorCore.LOCAL_WIFI).andThen(acceptFirstAndRevokeSecond(drones, false));
        drones[1].expectConnectOn(DeviceConnectorCore.LOCAL_WIFI).andThen(acceptFirstAndRevokeSecond(drones, true));

        // auto-connection should connect any of both drones. This drone is always drones[0] after the call
        mAutoConnection.start();

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(2)); // +0 for STARTED (swallowed), +1 for CONNECTING drone[0]
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[0]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // mock auto-connected drone finally connects
        drones[0].mockConnected();

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(3)); // +1 for CONNECTED drone[0]
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[0]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // mock auto-connected drone now starts disconnecting
        drones[0].mockDisconnecting();

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTING));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(4)); // +1 for DISCONNECTING drone[0]
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[0]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // prepare expectations for auto-connected drone disconnection
        drones[0].expectConnectOn(DeviceConnectorCore.LOCAL_WIFI).andThen(acceptFirstAndRevokeSecond(drones, false));
        drones[1].expectConnectOn(DeviceConnectorCore.LOCAL_WIFI).andThen(acceptFirstAndRevokeSecond(drones, true));

        // mock auto-connected drone now is disconnected
        drones[0].mockDisconnected();

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(6)); // +1 for DISCONNECTED drone[0], +1 for CONNECTING drone[0]
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[0]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());
    }

    @Test
    public void testDroneAutoConnectWithABetterConnector() {
        MockDrone[] drones = addIn(mDroneStore,
                new MockDrone("1", Drone.Model.ANAFI_4K).addConnectors(DeviceConnectorCore.LOCAL_BLE),
                new MockDrone("2", Drone.Model.ANAFI_THERMAL));

        assertThat(mChangeCnt, is(0));

        mEngine.start();

        assertThat(mChangeCnt, is(1)); // +1 for publish
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // prepare expectations for auto-connection
        drones[0].expectConnectOn(DeviceConnectorCore.LOCAL_BLE);

        // auto-connection should connect drone '1'
        mAutoConnection.start();

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(2)); // +0 for STARTED (swallowed), +1 for CONNECTING drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[0]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // mock drone '1' connected
        drones[0].mockConnected();

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(3)); // +1 for CONNECTED drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[0]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // mock that both drones are now visible through wifi (drone '2' visible after drone '1')
        // expect a disconnection attempt on drone '1'
        drones[0].expectDisconnect();

        drones[0].addConnectors(DeviceConnectorCore.LOCAL_WIFI);
        drones[1].addConnectors(DeviceConnectorCore.LOCAL_WIFI);

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTING));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(5)); // +1 for added connector on drone '1', +1 for DISCONNECT drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[0]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // mock final disconnection on drone '1'
        // expect a connection attempt on that drone
        drones[0].expectConnectOn(DeviceConnectorCore.LOCAL_WIFI);

        drones[0].mockDisconnected();

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(7)); // +1 for DISCONNECT drone '1', +1 for CONNECTING drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[0]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // mock final connection on drone '1'
        drones[0].mockConnected();

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(8)); // +1 for CONNECTED drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[0]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());
    }

    @Test
    public void testDroneAutoConnectWithABetterDrone() {
        MockDrone[] drones = addIn(mDroneStore,
                new MockDrone("1", Drone.Model.ANAFI_4K).addConnectors(DeviceConnectorCore.LOCAL_BLE),
                new MockDrone("2", Drone.Model.ANAFI_THERMAL));

        assertThat(mChangeCnt, is(0));

        mEngine.start();

        assertThat(mChangeCnt, is(1)); // +1 for publish
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // prepare expectations for auto-connection
        drones[0].expectConnectOn(DeviceConnectorCore.LOCAL_BLE);

        // auto-connection should connect drone '1'
        mAutoConnection.start();

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(2)); // +0 for STARTED (swallowed), +1 for CONNECTING drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[0]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // mock drone '1' connected
        drones[0].mockConnected();

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(3)); // +1 for CONNECTED drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[0]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // mock drone '2' is now visible through wifi
        // expect a disconnection attempt on drone '1'
        drones[0].expectDisconnect();

        drones[1].expectConnectOn(DeviceConnectorCore.LOCAL_WIFI);

        drones[1].addConnectors(DeviceConnectorCore.LOCAL_WIFI);

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTING));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));

        assertThat(mChangeCnt, is(5)); // +1 for DISCONNECTING drone '1', +1 for CONNECTING drone '2'
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[1]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // mock final disconnection on drone '1'
        drones[0].mockDisconnected();

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));

        assertThat(mChangeCnt, is(5)); // nothing should change
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[1]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // mock final connection on drone '2'
        drones[1].mockConnected();

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));

        assertThat(mChangeCnt, is(6)); // +1 for CONNECTED drone '2'
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[1]));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());
    }

    @Test
    public void testRCAutoConnectWithTwoWifiRCs() {
        MockRC[] rcs = addIn(mRCStore,
                new MockRC("1").addConnectors(DeviceConnectorCore.LOCAL_WIFI),
                new MockRC("2").addConnectors(DeviceConnectorCore.LOCAL_WIFI));

        assertThat(mChangeCnt, is(0));

        mEngine.start();

        assertThat(mChangeCnt, is(1)); // +1 for publish
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // prepare expectations for auto-connection
        rcs[0].expectConnectOn(DeviceConnectorCore.LOCAL_WIFI).andThen(acceptFirstAndRevokeSecond(rcs, false));
        rcs[1].expectConnectOn(DeviceConnectorCore.LOCAL_WIFI).andThen(acceptFirstAndRevokeSecond(rcs, true));

        // auto-connection should connect any of both rcs. This rc is always rcs[0] after the call
        mAutoConnection.start();

        rcs[0].assertNoExpectations();
        assertThat(rcs[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        rcs[1].assertNoExpectations();
        assertThat(rcs[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(2)); // +0 for STARTED (swallowed), +1 for CONNECTING rcs[0]
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rcs[0]));

        // mock auto-connected rc finally connects
        rcs[0].mockConnected();

        rcs[0].assertNoExpectations();
        assertThat(rcs[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));
        rcs[1].assertNoExpectations();
        assertThat(rcs[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(3)); // +1 for CONNECTED rcs[0]
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rcs[0]));

        // mock auto-connected rc now starts disconnecting
        rcs[0].mockDisconnecting();

        rcs[0].assertNoExpectations();
        assertThat(rcs[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTING));
        rcs[1].assertNoExpectations();
        assertThat(rcs[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(4)); // +1 for DISCONNECTING rcs[0]
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rcs[0]));

        // prepare expectations for auto-connected rc disconnection
        rcs[0].expectConnectOn(DeviceConnectorCore.LOCAL_WIFI).andThen(acceptFirstAndRevokeSecond(rcs, false));
        rcs[1].expectConnectOn(DeviceConnectorCore.LOCAL_WIFI).andThen(acceptFirstAndRevokeSecond(rcs, true));

        // mock auto-connected rc now is disconnected
        rcs[0].mockDisconnected();

        rcs[0].assertNoExpectations();
        assertThat(rcs[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        rcs[1].assertNoExpectations();
        assertThat(rcs[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(6)); // +1 for DISCONNECTED rcs[0], +1 for CONNECTING rcs[0]
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rcs[0]));
    }

    @Test
    public void testRCAutoConnectWithABetterRC() {
        MockRC[] rcs = addIn(mRCStore,
                new MockRC("1").addConnectors(DeviceConnectorCore.LOCAL_WIFI),
                new MockRC("2"));

        assertThat(mChangeCnt, is(0));

        mEngine.start();

        assertThat(mChangeCnt, is(1)); // +1 for publish
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // prepare expectations for auto-connection
        rcs[0].expectConnectOn(DeviceConnectorCore.LOCAL_WIFI);

        // auto-connection should connect rc '1'
        mAutoConnection.start();

        rcs[0].assertNoExpectations();
        assertThat(rcs[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        rcs[1].assertNoExpectations();
        assertThat(rcs[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(2)); // +0 for STARTED (swallowed), +1 for CONNECTING rc '1'
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rcs[0]));

        // mock rc '1' connected
        rcs[0].mockConnected();

        rcs[0].assertNoExpectations();
        assertThat(rcs[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));
        rcs[1].assertNoExpectations();
        assertThat(rcs[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(3)); // +1 for CONNECTED rc '1'
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rcs[0]));

        // mock rc '2' is now visible through USB
        // expect a disconnection attempt on rc '1'
        rcs[0].expectDisconnect();
        rcs[1].expectConnectOn(DeviceConnectorCore.LOCAL_USB);

        rcs[1].addConnectors(DeviceConnectorCore.LOCAL_USB);

        rcs[0].assertNoExpectations();
        assertThat(rcs[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTING));
        rcs[1].assertNoExpectations();
        assertThat(rcs[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));

        assertThat(mChangeCnt, is(5)); // +1 for DISCONNECTING rc '1', +1 for CONNECTING rc '2'
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rcs[1]));

        // mock final disconnection on rc '1'
        rcs[0].mockDisconnected();

        rcs[0].assertNoExpectations();
        assertThat(rcs[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        rcs[1].assertNoExpectations();
        assertThat(rcs[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));

        assertThat(mChangeCnt, is(5)); // nothing should change
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rcs[1]));

        // mock final connection on rc '2'
        rcs[1].mockConnected();

        rcs[0].assertNoExpectations();
        assertThat(rcs[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        rcs[1].assertNoExpectations();
        assertThat(rcs[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));

        assertThat(mChangeCnt, is(6)); // +1 for CONNECTED rc '2'
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rcs[1]));
    }

    @Test
    public void testRCAutoConnectWithADrone() {
        MockDrone drone = addIn(mDroneStore, new MockDrone("1").addConnectors(DeviceConnectorCore.LOCAL_WIFI));
        MockRC rc = addIn(mRCStore, new MockRC("2"));

        assertThat(mChangeCnt, is(0));

        mEngine.start();

        assertThat(mChangeCnt, is(1)); // +1 for publish
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // prepare expectations for auto-connection
        drone.expectConnectOn(DeviceConnectorCore.LOCAL_WIFI);

        // auto-connection should connect drone
        mAutoConnection.start();

        drone.assertNoExpectations();
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        rc.assertNoExpectations();
        assertThat(rc.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(2)); // +0 for STARTED (swallowed), +1 for CONNECTING drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drone));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // mock drone connected
        drone.mockConnected();

        drone.assertNoExpectations();
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));
        rc.assertNoExpectations();
        assertThat(rc.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(3)); // +1 for CONNECTED drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drone));
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // mock rc visible through USB
        // expect a disconnection attempt on drone
        drone.expectDisconnect();
        // expect a connection attempt on rc
        rc.expectConnectOn(DeviceConnectorCore.LOCAL_USB);

        rc.addConnectors(DeviceConnectorCore.LOCAL_USB);

        drone.assertNoExpectations();
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTING));
        rc.assertNoExpectations();
        assertThat(rc.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));

        assertThat(mChangeCnt, is(5)); // +1 for DISCONNECTING drone '1', +1 for CONNECTING rc '2'
        assertThat(mAutoConnection.getDrone(), droneProxy(drone));
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rc));

        // mock drone disconnected
        drone.mockDisconnected();

        drone.assertNoExpectations();
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        rc.assertNoExpectations();
        assertThat(rc.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));

        assertThat(mChangeCnt, is(6)); // +1 for DISCONNECTED drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drone));
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rc));

        // during connection, mock that rc advertises that it sees drone '1' and a new drone '3'
        MockDrone otherDrone = addIn(mDroneStore, new MockDrone("3").addConnectors(rc.asConnector()));
        drone.addConnectors(rc.asConnector());

        // since the rc is not connected yet, no connection on the drone should be attempted
        drone.assertNoExpectations();
        otherDrone.assertNoExpectations();
        rc.assertNoExpectations();

        assertThat(mChangeCnt, is(7)); // +1 for added drone '1' connector
        assertThat(mAutoConnection.getDrone(), droneProxy(drone));
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rc));

        // mock final connection of rc
        // expect a connection on drone '1'
        drone.expectConnectOn(rc.asConnector());
        rc.mockConnected();

        drone.assertNoExpectations();
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        otherDrone.assertNoExpectations();
        assertThat(otherDrone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        rc.assertNoExpectations();
        assertThat(rc.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));

        assertThat(mChangeCnt, is(9)); // +1 for CONNECTED rc '2', +1 for CONNECTING drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drone));
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rc));

        // mock disconnection of drone '1'
        // we don't expect auto-connection to connect any drone, instead it should let the rc do its own job.
        drone.mockDisconnecting();

        drone.assertNoExpectations();
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTING));
        otherDrone.assertNoExpectations();
        assertThat(otherDrone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        rc.assertNoExpectations();
        assertThat(rc.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));

        assertThat(mChangeCnt, is(10)); // +1 for DISCONNECTING drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drone));
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rc));

        // mock final disconnection of drone '1'
        drone.mockDisconnected();

        drone.assertNoExpectations();
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        otherDrone.assertNoExpectations();
        assertThat(otherDrone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        rc.assertNoExpectations();
        assertThat(rc.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));

        assertThat(mChangeCnt, is(11)); // +1 for DISCONNECTED drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drone));
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rc));
    }

    @Test
    public void testDisconnectOnStart() {
        MockRC rcs[] = addIn(mRCStore,
                new MockRC("1").addConnectors(DeviceConnectorCore.LOCAL_USB).mockDisconnected(),
                new MockRC("2").addConnectors(DeviceConnectorCore.LOCAL_WIFI)
                               .mockConnecting(DeviceConnectorCore.LOCAL_WIFI).mockConnected(),
                new MockRC("3").addConnectors(DeviceConnectorCore.LOCAL_BLE)
                               .mockConnecting(DeviceConnectorCore.LOCAL_BLE));

        MockDrone[] drones = addIn(mDroneStore,
                new MockDrone("4").addConnectors(DeviceConnectorCore.LOCAL_WIFI, rcs[0].asConnector())
                                  .mockConnecting(DeviceConnectorCore.LOCAL_WIFI).mockConnected(),
                new MockDrone("5").addConnectors(DeviceConnectorCore.LOCAL_BLE)
                                  .mockConnecting(DeviceConnectorCore.LOCAL_BLE),
                new MockDrone("6").addConnectors(DeviceConnectorCore.LOCAL_WIFI).mockDisconnected());

        assertThat(mChangeCnt, is(0));

        mEngine.start();

        assertThat(mChangeCnt, is(1)); // +1 for publish
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // start auto-connection. Expect only the best rc to remain connected. All drones should get disconnected
        // (drone '4' should later be reconnected through the rc).
        rcs[0].expectConnectOn(DeviceConnectorCore.LOCAL_USB);
        rcs[1].expectDisconnect();
        rcs[2].expectDisconnect();

        drones[0].expectDisconnect();
        drones[1].expectDisconnect();

        mAutoConnection.start();

        rcs[0].assertNoExpectations();
        assertThat(rcs[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        rcs[1].assertNoExpectations();
        assertThat(rcs[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTING));
        rcs[2].assertNoExpectations();
        assertThat(rcs[2].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTING));

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTING));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTING));
        drones[2].assertNoExpectations();
        assertThat(drones[2].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(2)); // +0 for STARTED (swallowed), +1 for CONNECTING rc '1'
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rcs[0]));

        // mock rc '1' connected, other rcs connected, drones disconnected (expect '4')
        rcs[0].mockConnected();
        rcs[1].mockDisconnected();
        rcs[2].mockDisconnected();
        drones[1].mockDisconnected();

        rcs[0].assertNoExpectations();
        assertThat(rcs[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));
        rcs[1].assertNoExpectations();
        assertThat(rcs[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        rcs[2].assertNoExpectations();
        assertThat(rcs[2].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTING));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        drones[2].assertNoExpectations();
        assertThat(drones[2].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(3)); // +1 for CONNECTED rc '1'
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rcs[0]));

        // mock drone '4' disconnected
        // expect an auto-connection attempt on drone '4' through rc '1'
        drones[0].expectConnectOn(rcs[0].asConnector());
        drones[0].mockDisconnected();

        rcs[0].assertNoExpectations();
        assertThat(rcs[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));
        rcs[1].assertNoExpectations();
        assertThat(rcs[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        rcs[2].assertNoExpectations();
        assertThat(rcs[2].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        drones[0].assertNoExpectations();
        assertThat(drones[0].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        drones[1].assertNoExpectations();
        assertThat(drones[1].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        drones[2].assertNoExpectations();
        assertThat(drones[2].getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));

        assertThat(mChangeCnt, is(4)); // +1 for CONNECTING drone '4'
        assertThat(mAutoConnection.getDrone(), droneProxy(drones[0]));
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rcs[0]));
    }

    @Test
    public void testStayConnectedWhenStopped() {
        MockDrone drone = addIn(mDroneStore, new MockDrone("1").addConnectors(DeviceConnectorCore.LOCAL_WIFI));
        MockRC rc = addIn(mRCStore, new MockRC("2").addConnectors(DeviceConnectorCore.LOCAL_WIFI));

        assertThat(mChangeCnt, is(0));

        mEngine.start();

        assertThat(mChangeCnt, is(1)); // +1 for publish
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // prepare expectations for auto-connection
        rc.expectConnectOn(DeviceConnectorCore.LOCAL_WIFI);

        // auto-connection should connect rc
        mAutoConnection.start();

        drone.assertNoExpectations();
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        rc.assertNoExpectations();
        assertThat(rc.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));

        assertThat(mChangeCnt, is(2)); // +0 for STARTED (swallowed), +1 for CONNECTING rc '2'
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rc));

        // mock rc is connected
        rc.mockConnected();

        drone.assertNoExpectations();
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
        rc.assertNoExpectations();
        assertThat(rc.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));

        assertThat(mChangeCnt, is(3)); // +1 for CONNECTED rc '2'
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rc));

        // mock drone is visible and connecting through rc
        drone.addConnectors(rc.asConnector()).mockConnecting(rc.asConnector());

        // no auto-connection should occur; we let the RC do is own business
        drone.assertNoExpectations();
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        rc.assertNoExpectations();
        assertThat(rc.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));

        assertThat(mChangeCnt, is(4)); // +1 for CONNECTING drone '1'
        assertThat(mAutoConnection.getDrone(), droneProxy(drone));
        assertThat(mAutoConnection.getRemoteControl(), rcProxy(rc));

        // stop auto-connection
        mAutoConnection.stop();

        // nothing should change
        drone.assertNoExpectations();
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        rc.assertNoExpectations();
        assertThat(rc.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));

        assertThat(mChangeCnt, is(5)); // +1 for STOPPED
        assertThat(mAutoConnection.getDrone(), nullValue());
        assertThat(mAutoConnection.getRemoteControl(), nullValue());

        // stop engine
        mEngine.requestStop(null);

        // nothing should change
        drone.assertNoExpectations();
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));
        rc.assertNoExpectations();
        assertThat(rc.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));

        assertThat(mChangeCnt, is(6)); // +1 for unpublish
        assertThat(mAutoConnection, nullValue());
    }

    @SafeVarargs
    @NonNull
    private static <D extends DeviceCore> D[] addIn(@NonNull DeviceStore<? super D> store, @NonNull D... devices) {
        for (D device : devices) {
            store.add(device);
        }
        return devices;
    }

    @NonNull
    private static <D extends DeviceCore> D addIn(@NonNull DeviceStore<? super D> store, @NonNull D device) {
        store.add(device);
        return device;
    }

    private static <D extends MockDevice> MockDevice.Expectation.Connect.Action<D> acceptFirstAndRevokeSecond(
            @NonNull D[] devices, boolean swap) {
        return (device, connector, password) -> {
            if (swap) {
                D tmp = devices[0];
                devices[0] = devices[1];
                devices[1] = tmp;
            }
            device.mockConnecting(connector);
            devices[1].revokeLastExpectation();
            return true;
        };
    }
}
