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

package com.parrot.drone.groundsdk.internal.device;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.component.ComponentCore;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.session.MockSession;
import com.parrot.drone.groundsdk.internal.session.Session;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.parrot.drone.groundsdk.DeviceStateMatcher.activeConnector;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.canBeConnected;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.canBeDisconnected;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.canBeForgotten;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.causeIs;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.hasConnectors;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.hasNoActiveConnector;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.hasNoConnectors;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.stateIs;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests Drone and DroneCore classes.
 */
@SuppressWarnings({"unchecked", "ConstantConditions"})
public class DroneTest {

    private DroneCore mDroneCore;

    private Drone mDrone;

    private int mForgetCnt;

    private int mConnectCnt;

    private int mDisconnectCnt;

    private String mProviderUid;

    private String mPassword;

    @Before
    public void setUp() {
        mDroneCore = new DroneCore("1", Drone.Model.ANAFI_4K, "Drone1", new DroneDelegate());
        mDrone = new DroneProxy(new MockSession(), mDroneCore);
        mForgetCnt = 0;
        mConnectCnt = 0;
        mDisconnectCnt = 0;
        mProviderUid = mPassword = null;
    }

    /**
     * Test that drone name can be get and is correctly notified when changed
     */
    @Test
    public void testDroneName() {
        int changeCnt[] = {0};
        String name[] = {null};
        mDrone.getName(droneName -> {
            changeCnt[0]++;
            name[0] = droneName;
        });

        // check original name
        assertThat(changeCnt[0], is(1));
        assertThat(name[0], is("Drone1"));

        // check name change
        mDroneCore.updateName("newName");
        assertThat(changeCnt[0], is(2));
        assertThat(name[0], is("newName"));

        // check name ref does not change when drone is removed
        mDroneCore.destroy();
        assertThat(changeCnt[0], is(2));
    }

    /**
     * Test that drone state can be get and is correctly notified when changed
     */
    @Test
    public void testDroneState() {
        int changeCnt[] = {0};
        DeviceState state[] = {null};
        mDrone.getState(deviceState -> {
            changeCnt[0]++;
            state[0] = deviceState;
        });

        DeviceStateCore stateCore = mDroneCore.getDeviceStateCore();

        // check initial state
        assertThat(changeCnt[0], is(1));
        assertThat(state[0], allOf(stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.NONE), canBeForgotten(false), canBeDisconnected(false),
                canBeConnected(false), hasNoConnectors(), hasNoActiveConnector()));

        Set<DeviceConnectorCore> connectors = new HashSet<>();
        // add a connector. canBeConnected should switch to true
        connectors.add(DeviceConnectorCore.LOCAL_WIFI);
        stateCore.updateConnectors(connectors).notifyUpdated();
        assertThat(changeCnt[0], is(2));
        assertThat(state[0], allOf(stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.NONE), canBeForgotten(false), canBeDisconnected(false),
                canBeConnected(true), hasConnectors(DeviceConnectorCore.LOCAL_WIFI), hasNoActiveConnector()));

        // move to connecting, canBeConnected should revert to false
        stateCore.updateConnectionState(DeviceState.ConnectionState.CONNECTING,
                DeviceState.ConnectionStateCause.USER_REQUESTED).notifyUpdated();
        assertThat(changeCnt[0], is(3));
        assertThat(state[0], allOf(stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(false),
                canBeDisconnected(false),
                canBeConnected(false), hasConnectors(DeviceConnectorCore.LOCAL_WIFI), hasNoActiveConnector()));

        // move to connected with an active local connector, canBeDisconnected should switch to true
        stateCore.updateConnectionState(DeviceState.ConnectionState.CONNECTED)
                 .updateActiveConnector(DeviceConnectorCore.LOCAL_WIFI).notifyUpdated();
        assertThat(changeCnt[0], is(4));
        assertThat(state[0], allOf(stateIs(DeviceState.ConnectionState.CONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false), hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        // mark as persisted, canBeForgotten should switch to true
        stateCore.updatePersisted(true).notifyUpdated();
        assertThat(changeCnt[0], is(5));
        assertThat(state[0], allOf(stateIs(DeviceState.ConnectionState.CONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(true), canBeDisconnected(true),
                canBeConnected(false), hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        // add a connector that supports forget (RC) and remove persisted state, canBeForgotten should remain true
        connectors.add(DeviceConnectorCore.createRCConnector("456"));
        stateCore.updateConnectors(connectors).updatePersisted(false).notifyUpdated();
        assertThat(changeCnt[0], is(6));
        assertThat(state[0], allOf(stateIs(DeviceState.ConnectionState.CONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(true), canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI, DeviceConnectorCore.createRCConnector("456")),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        // make RC connector active, canDisconnect should revert to false (RC does not support disconnect)
        stateCore.updateActiveConnector(DeviceConnectorCore.createRCConnector("456")).notifyUpdated();
        assertThat(changeCnt[0], is(7));
        assertThat(state[0], allOf(stateIs(DeviceState.ConnectionState.CONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(true),
                canBeDisconnected(false),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI, DeviceConnectorCore.createRCConnector("456")),
                activeConnector(DeviceConnectorCore.createRCConnector("456"))));

        // remove all connectors and disconnect
        stateCore.updateConnectors(Collections.emptySet()).updateActiveConnector(null)
                 .updateConnectionState(DeviceState.ConnectionState.DISCONNECTED,
                         DeviceState.ConnectionStateCause.CONNECTION_LOST).notifyUpdated();
        assertThat(changeCnt[0], is(8));
        assertThat(state[0], allOf(stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.CONNECTION_LOST), canBeForgotten(false),
                canBeDisconnected(false),
                canBeConnected(false), hasNoConnectors(), hasNoActiveConnector()));

        // check state ref does not change when drone is removed
        mDroneCore.destroy();
        assertThat(changeCnt[0], is(8));
    }

    @Test
    public void testGetInstrument() {
        int[] changeCnt = {0};
        TestInstrument[] instrument = {null};

        // check getting an unknown instrument
        Ref<TestInstrument> instrumentRef = mDrone.getInstrument(TestInstrument.class,
                obj -> {
                    changeCnt[0]++;
                    instrument[0] = obj;
                });

        // check instrument not found
        assertThat(instrument[0], nullValue());
        assertThat(changeCnt[0], is(0));
        assertThat(mDrone.getInstrument(TestInstrument.class), nullValue());
        assertThat(instrumentRef, notNullValue());
        assertThat(instrumentRef.get(), nullValue());

        // add instrument
        TestInstrument storeInstrument = new TestInstrument();
        mDroneCore.getInstrumentStore().add(storeInstrument, TestInstrument.DESC);

        // check instrument present and notified
        assertThat(instrument[0], is(storeInstrument));
        assertThat(changeCnt[0], is(1));
        assertThat(mDrone.getInstrument(TestInstrument.class), is(storeInstrument));
        assertThat(instrumentRef, notNullValue());
        assertThat(instrumentRef.get(), is(storeInstrument));

        // get a ref on an existing instrument
        Ref<TestInstrument> instrumentRef2 = mDrone.getInstrument(TestInstrument.class,
                obj -> {
                    changeCnt[0]++;
                    assertThat(obj, is(storeInstrument));
                });

        // check callback called immediately
        assertThat(changeCnt[0], is(2));
        assertThat(instrumentRef2, notNullValue());
        assertThat(instrumentRef2.get(), is(storeInstrument));
        // discard second ref
        instrumentRef2.close();
        // remove instrument
        mDroneCore.getInstrumentStore().remove(TestInstrument.DESC);

        // check instrument not found and remove has been notified
        assertThat(instrument[0], nullValue());
        assertThat(changeCnt[0], is(3));
        assertThat(mDrone.getInstrument(TestInstrument.class), nullValue());
        assertThat(instrumentRef, notNullValue());
        assertThat(instrumentRef.get(), nullValue());
        assertThat(instrumentRef2, notNullValue());
        assertThat(instrumentRef2.get(), nullValue());
    }

    @Test
    public void testGetPeripheral() {
        int[] changeCnt = {0};
        TestPeripheral[] peripheral = {null};

        // check getting an unknown peripheral
        Ref<TestPeripheral> peripheralRef = mDrone.getPeripheral(TestPeripheral.class,
                obj -> {
                    changeCnt[0]++;
                    peripheral[0] = obj;
                });

        // check peripheral not found
        assertThat(peripheral[0], nullValue());
        assertThat(changeCnt[0], is(0));
        assertThat(mDrone.getPeripheral(TestPeripheral.class), nullValue());
        assertThat(peripheralRef, notNullValue());
        assertThat(peripheralRef.get(), nullValue());

        // add peripheral
        TestPeripheral storePeripheral = new TestPeripheral();
        mDroneCore.getPeripheralStore().add(storePeripheral, TestPeripheral.DESC);

        // check peripheral present and notified
        assertThat(peripheral[0], is(storePeripheral));
        assertThat(changeCnt[0], is(1));
        assertThat(mDrone.getPeripheral(TestPeripheral.class), is(storePeripheral));
        assertThat(peripheralRef, notNullValue());
        assertThat(peripheralRef.get(), is(storePeripheral));

        // get a ref on an existing peripheral
        Ref<TestPeripheral> peripheralRef2 = mDrone.getPeripheral(TestPeripheral.class,
                obj -> {
                    changeCnt[0]++;
                    assertThat(obj, is(storePeripheral));
                });

        // check callback called immediately
        assertThat(changeCnt[0], is(2));
        assertThat(peripheralRef2, notNullValue());
        assertThat(peripheralRef2.get(), is(storePeripheral));
        // discard second ref
        peripheralRef2.close();
        // remove peripheral
        mDroneCore.getPeripheralStore().remove(TestPeripheral.DESC);

        // check peripheral not found and remove has been notified
        assertThat(peripheral[0], nullValue());
        assertThat(changeCnt[0], is(3));
        assertThat(mDrone.getPeripheral(TestPeripheral.class), nullValue());
        assertThat(peripheralRef, notNullValue());
        assertThat(peripheralRef.get(), nullValue());
        assertThat(peripheralRef2, notNullValue());
        assertThat(peripheralRef2.get(), nullValue());
    }

    @Test
    public void testGetPilotingItf() {
        int[] changeCnt = {0};
        TestPilotingItf[] pilotingItf = {null};

        // check getting an unknown pilotingItf
        Ref<TestPilotingItf> pilotingItfRef = mDrone.getPilotingItf(TestPilotingItf.class,
                obj -> {
                    changeCnt[0]++;
                    pilotingItf[0] = obj;
                });

        // check pilotingItf not found
        assertThat(pilotingItf[0], nullValue());
        assertThat(changeCnt[0], is(0));
        assertThat(mDrone.getPilotingItf(TestPilotingItf.class), nullValue());
        assertThat(pilotingItfRef, notNullValue());
        assertThat(pilotingItfRef.get(), nullValue());

        // add pilotingItf
        TestPilotingItf storePilotingItf = new TestPilotingItf();
        mDroneCore.getPilotingItfStore().add(storePilotingItf, TestPilotingItf.DESC);

        // check pilotingItf present and notified
        assertThat(pilotingItf[0], is(storePilotingItf));
        assertThat(changeCnt[0], is(1));
        assertThat(mDrone.getPilotingItf(TestPilotingItf.class), is(storePilotingItf));
        assertThat(pilotingItfRef, notNullValue());
        assertThat(pilotingItfRef.get(), is(storePilotingItf));

        // get a ref on an existing pilotingItf
        Ref<TestPilotingItf> pilotingItfRef2 = mDrone.getPilotingItf(TestPilotingItf.class,
                obj -> {
                    changeCnt[0]++;
                    assertThat(obj, is(storePilotingItf));
                });

        // check callback called immediately
        assertThat(changeCnt[0], is(2));
        assertThat(pilotingItfRef2, notNullValue());
        assertThat(pilotingItfRef2.get(), is(storePilotingItf));
        // discard second ref
        pilotingItfRef2.close();
        // remove pilotingItf
        mDroneCore.getPilotingItfStore().remove(TestPilotingItf.DESC);

        // check pilotingItf not found and remove has been notified
        assertThat(pilotingItf[0], nullValue());
        assertThat(changeCnt[0], is(3));
        assertThat(mDrone.getPilotingItf(TestPilotingItf.class), nullValue());
        assertThat(pilotingItfRef, notNullValue());
        assertThat(pilotingItfRef.get(), nullValue());
        assertThat(pilotingItfRef2, notNullValue());
        assertThat(pilotingItfRef2.get(), nullValue());
    }

    @Test
    public void testForgetDrone() {
        assertThat(mDrone.forget(), is(true));
        assertThat(mForgetCnt, is(1));
    }

    @Test
    public void testConnectDrone() {
        DeviceConnector connector = DeviceConnectorCore.LOCAL_WIFI;
        assertThat(mDrone.connect(connector), is(true));
        assertThat(mConnectCnt, is(1));
        assertThat(mProviderUid, is(connector.getUid()));
        assertThat(mPassword, nullValue());

        mDrone.disconnect();

        connector = DeviceConnectorCore.createRCConnector("456");
        assertThat(mDrone.connect(connector), is(true));
        assertThat(mConnectCnt, is(2));
        assertThat(mProviderUid, is(connector.getUid()));

        mDrone.disconnect();

        assertThat(mDrone.connect(connector, "password"), is(true));
        assertThat(mConnectCnt, is(3));
        assertThat(mProviderUid, is(connector.getUid()));
        assertThat(mPassword, is("password"));
    }

    @Test
    public void testDisconnectDrone() {
        assertThat(mDrone.disconnect(), is(true));
        assertThat(mDisconnectCnt, is(1));
    }

    private class DroneDelegate implements DeviceCore.Delegate {

        @Override
        public boolean forget() {
            mForgetCnt++;
            return true;
        }

        @Override
        public boolean connect(@NonNull DeviceConnector connector, @Nullable String password) {
            mConnectCnt++;
            mProviderUid = connector.getUid();
            mPassword = password;
            return true;
        }

        @Override
        public boolean disconnect() {
            mDisconnectCnt++;
            return true;
        }
    }

    private static class TestComponent extends ComponentCore {

        TestComponent(@NonNull ComponentDescriptor descriptor) {
            super(descriptor, null);
        }

        @Override
        @NonNull
        protected Object getProxy(@NonNull Session session) {
            return this;
        }
    }

    private static class TestInstrument extends TestComponent implements Instrument {

        static final ComponentDescriptor<Instrument, TestInstrument> DESC =
                ComponentDescriptor.of(TestInstrument.class);

        TestInstrument() {
            super(DESC);
        }
    }

    private static class TestPeripheral extends TestComponent implements Peripheral {

        static final ComponentDescriptor<Peripheral, TestPeripheral> DESC =
                ComponentDescriptor.of(TestPeripheral.class);

        TestPeripheral() {
            super(DESC);
        }
    }

    private static class TestPilotingItf extends TestComponent implements PilotingItf {

        static final ComponentDescriptor<PilotingItf, TestPilotingItf> DESC =
                ComponentDescriptor.of(TestPilotingItf.class);

        TestPilotingItf() {
            super(DESC);
        }
    }
}

