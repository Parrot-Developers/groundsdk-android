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

package com.parrot.drone.groundsdk.arsdkengine;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.ProxyDeviceController;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.internal.device.DeviceConnectorCore;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.utility.DeviceStore;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static com.parrot.drone.groundsdk.DeviceCoreMatcher.hasUid;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.canBeConnected;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.canBeDisconnected;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.canBeForgotten;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.causeIs;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.hasConnectors;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.hasNoActiveConnector;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.hasNoConnectors;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.stateIs;
import static com.parrot.drone.groundsdk.DroneCoreMatcher.isModel;
import static com.parrot.drone.groundsdk.RemoteControlCoreMatcher.isModel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ArsdkEngineAddRemoveDevicesTests extends ArsdkEngineTestBase {

    private int mAddDroneCnt;

    private int mRemoveDroneCnt;

    private int mAddRcCnt;

    private int mRemoveRcCnt;

    @Override
    public void setUp() {
        super.setUp();
        mDroneStore.monitorWith(new DeviceStore.Monitor<DroneCore>() {

            @Override
            public void onDeviceAdded(@NonNull DroneCore droneCore) {
                mAddDroneCnt++;
            }

            @Override
            public void onDeviceRemoved(@NonNull DroneCore droneCore) {
                mRemoveDroneCnt++;
            }
        });

        mRCStore.monitorWith(new DeviceStore.Monitor<RemoteControlCore>() {

            @Override
            public void onDeviceAdded(@NonNull RemoteControlCore remoteControlCore) {
                mAddRcCnt++;
            }

            @Override
            public void onDeviceRemoved(@NonNull RemoteControlCore remoteControlCore) {
                mRemoveRcCnt++;
            }
        });

        mAddDroneCnt = mRemoveDroneCnt = mAddRcCnt = mRemoveRcCnt = 0;
    }

    @Test
    public void testPersistedDevice() {
        PersistentStore store = new PersistentStore(mContext);
        createDeviceDict(store, "123", Drone.Model.ANAFI_4K, "Drone1").commit();
        createDeviceDict(store, "456", RemoteControl.Model.SKY_CONTROLLER_3, "RC1").commit();
        mArsdkEngine.start();

        // check drone has been created from the store
        assertThat(mAddDroneCnt, is(1));
        DroneCore drone = mDroneStore.get("123");
        assertThat(drone, allOf(notNullValue(), hasUid("123"), isModel(Drone.Model.ANAFI_4K)));
        assert drone != null;
        assertThat(drone.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(true), canBeConnected(false), canBeDisconnected(false), hasNoConnectors(),
                hasNoActiveConnector()));

        // check RC has been created from the store
        assertThat(mAddRcCnt, is(1));
        RemoteControlCore rc = mRCStore.get("456");
        assertThat(rc, allOf(notNullValue(), hasUid("456"), isModel(RemoteControl.Model.SKY_CONTROLLER_3)));
        assert rc != null;
        assertThat(drone.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(true), canBeConnected(false), canBeDisconnected(false), hasNoConnectors(),
                hasNoActiveConnector()));
    }

    @Test
    public void testAddRemoveDrone() {
        mArsdkEngine.start();

        // add
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        assertThat(mAddDroneCnt, is(1));
        DroneCore drone = mDroneStore.get("123");
        assertThat(drone, allOf(notNullValue(), hasUid("123"), isModel(Drone.Model.ANAFI_4K)));
        assert drone != null;
        assertThat(drone.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(false), canBeConnected(true), canBeDisconnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));

        // remove
        mMockArsdkCore.removeDevice(1);
        assertThat(mAddDroneCnt, is(1));
        assertThat(mRemoveDroneCnt, is(1));
        assertThat(mDroneStore.all(), hasSize(0));
    }

    @Test
    public void testAddRemoveRC() {
        mArsdkEngine.start();

        // add
        mMockArsdkCore.addDevice("123", RemoteControl.Model.SKY_CONTROLLER_3.id(), "RC1", 1, Backend.TYPE_NET);
        assertThat(mAddRcCnt, is(1));
        RemoteControlCore rc = mRCStore.get("123");
        assertThat(rc, allOf(notNullValue(), hasUid("123"), isModel(RemoteControl.Model.SKY_CONTROLLER_3)));
        assert rc != null;
        assertThat(rc.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(false), canBeConnected(true), canBeDisconnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));

        // remove
        mMockArsdkCore.removeDevice(1);
        assertThat(mAddRcCnt, is(1));
        assertThat(mRemoveRcCnt, is(1));
        assertThat(mRCStore.all(), hasSize(0));
    }

    @Test
    public void testAddRemoveKnownDrone() {
        PersistentStore store = new PersistentStore(mContext);
        createDeviceDict(store, "123", Drone.Model.ANAFI_4K, "Drone1").commit();
        mArsdkEngine.start();

        // add
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        assertThat(mAddDroneCnt, is(1));
        DroneCore drone = mDroneStore.get("123");
        assertThat(drone, allOf(notNullValue(), hasUid("123"), isModel(Drone.Model.ANAFI_4K)));
        assert drone != null;
        assertThat(drone.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(true), canBeConnected(true), canBeDisconnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));

        // remove
        mMockArsdkCore.removeDevice(1);
        assertThat(mAddDroneCnt, is(1));
        assertThat(mRemoveDroneCnt, is(0)); // should not be notified
        assertThat(mDroneStore.all(), hasSize(1));
        drone = mDroneStore.get("123");
        assertThat(drone, allOf(notNullValue(), hasUid("123"), isModel(Drone.Model.ANAFI_4K)));
        assert drone != null;
        assertThat(drone.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(true), canBeConnected(false), canBeDisconnected(false), hasNoConnectors(),
                hasNoActiveConnector()));
    }

    @Test
    public void testAddRemoveKnownRC() {
        PersistentStore store = new PersistentStore(mContext);
        createDeviceDict(store, "123", RemoteControl.Model.SKY_CONTROLLER_3, "RC1").commit();
        mArsdkEngine.start();

        // add
        mMockArsdkCore.addDevice("123", RemoteControl.Model.SKY_CONTROLLER_3.id(), "RC1", 1, Backend.TYPE_NET);
        assertThat(mAddRcCnt, is(1));
        RemoteControlCore rc = mRCStore.get("123");
        assertThat(rc, allOf(notNullValue(), hasUid("123"), isModel(RemoteControl.Model.SKY_CONTROLLER_3)));
        assert rc != null;
        assertThat(rc.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(true), canBeConnected(true), canBeDisconnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));

        // remove
        mMockArsdkCore.removeDevice(1);
        assertThat(mAddRcCnt, is(1));
        assertThat(mRemoveRcCnt, is(0)); // should not be notified
        assertThat(mRCStore.all(), hasSize(1));
        rc = mRCStore.get("123");
        assertThat(rc, allOf(notNullValue(), hasUid("123"), isModel(RemoteControl.Model.SKY_CONTROLLER_3)));
        assert rc != null;
        assertThat(rc.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(true), canBeConnected(false), canBeDisconnected(false), hasNoConnectors(),
                hasNoActiveConnector()));
    }

    @Test
    public void testForgetDrone() {
        PersistentStore store = new PersistentStore(mContext);
        createDeviceDict(store, "123", Drone.Model.ANAFI_4K, "Drone1").commit();
        mArsdkEngine.start();

        // check device has been created from the store
        assertThat(mAddDroneCnt, is(1));
        DroneCore drone = mDroneStore.get("123");
        assert drone != null;

        drone.forget();
        assertThat(mAddDroneCnt, is(1));
        assertThat(mRemoveDroneCnt, is(1));
        assertThat(mDroneStore.all(), hasSize(0));

        // check data has been removed from the persistent store
        assertThat(store.getDevicesUid(), hasSize(0));
    }

    @Test
    public void testForgetRC() {
        PersistentStore store = new PersistentStore(mContext);
        createDeviceDict(store, "123", RemoteControl.Model.SKY_CONTROLLER_3, "RC1").commit();
        mArsdkEngine.start();

        // check device has been created from the store
        assertThat(mAddRcCnt, is(1));
        RemoteControlCore rc = mRCStore.get("123");
        assert rc != null;

        rc.forget();
        assertThat(mAddRcCnt, is(1));
        assertThat(mRemoveRcCnt, is(1));
        assertThat(mRCStore.all(), hasSize(0));

        // check data has been removed from the persistent store
        assertThat(store.getDevicesUid(), hasSize(0));
    }

    @Test
    public void testAddRemoveRcDrone() {
        PersistentStore store = new PersistentStore(mContext);
        createDeviceDict(store, "456", RemoteControl.Model.SKY_CONTROLLER_3, "RC1").commit();
        mArsdkEngine.start();

        ProxyDeviceController<?> controller = (ProxyDeviceController<?>)
                mArsdkEngine.getExistingDeviceController("456");
        assert controller != null;
        ArsdkProxy proxy = controller.getArsdkProxy();
        proxy.addRemoteDevice("123", Drone.Model.ANAFI_4K, "Drone1");

        assertThat(mAddDroneCnt, is(1));
        DroneCore drone = mDroneStore.get("123");
        assertThat(drone, allOf(notNullValue(), hasUid("123"), isModel(Drone.Model.ANAFI_4K)));
        assert drone != null;
        assertThat(drone.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(true), canBeConnected(true), canBeDisconnected(false),
                hasConnectors(DeviceConnectorCore.createRCConnector("456")), hasNoActiveConnector()));

        proxy.removeRemoteDevice("123");
        assertThat(mAddDroneCnt, is(1));
        assertThat(mRemoveDroneCnt, is(1));
        assertThat(mDroneStore.all(), hasSize(0));
    }

    @Test
    public void testAddRemoveLocalAndRcDrone() {
        PersistentStore store = new PersistentStore(mContext);
        createDeviceDict(store, "456", RemoteControl.Model.SKY_CONTROLLER_3, "RC1").commit();
        mArsdkEngine.start();

        // add local drone
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        assertThat(mAddDroneCnt, is(1));
        DroneCore drone = mDroneStore.get("123");
        assertThat(drone, allOf(notNullValue(), hasUid("123"), isModel(Drone.Model.ANAFI_4K)));
        assert drone != null;
        assertThat(drone.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(false), canBeConnected(true), canBeDisconnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));

        // add remote drone
        ProxyDeviceController<?> controller = (ProxyDeviceController<?>)
                mArsdkEngine.getExistingDeviceController("456");
        assert controller != null;
        ArsdkProxy proxy = controller.getArsdkProxy();
        proxy.addRemoteDevice("123", Drone.Model.ANAFI_4K, "Drone1");
        assertThat(mAddDroneCnt, is(1));
        assertThat(drone, allOf(notNullValue(), hasUid("123"), isModel(Drone.Model.ANAFI_4K)));
        assertThat(drone.getDeviceStateCore(), allOf(notNullValue(),
                canBeForgotten(true), canBeConnected(true), canBeDisconnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI,
                        DeviceConnectorCore.createRCConnector("456")), hasNoActiveConnector()));

        // remove remote drone
        proxy.removeRemoteDevice("123");
        assertThat(mAddDroneCnt, is(1));
        assertThat(mRemoveDroneCnt, is(0)); // should not be notified (still held by local connector)
        assertThat(drone, allOf(notNullValue(), hasUid("123"), isModel(Drone.Model.ANAFI_4K)));
        assertThat(drone.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(false), canBeConnected(true), canBeDisconnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));

        // remove local drone
        mMockArsdkCore.removeDevice(1);
        assertThat(mAddDroneCnt, is(1));
        assertThat(mRemoveDroneCnt, is(1));
        assertThat(mDroneStore.all(), hasSize(0));
    }

    @Test
    public void testAddRemoveLocalWifiAndUsbRc() {
        mArsdkEngine.start();

        // add local wifi rc
        mMockArsdkCore.addDevice("456", RemoteControl.Model.SKY_CONTROLLER_3.id(), "RC1", 1, Backend.TYPE_NET);
        assertThat(mAddRcCnt, is(1));
        RemoteControlCore rc = mRCStore.get("456");
        assertThat(rc, allOf(notNullValue(), hasUid("456"), isModel(RemoteControl.Model.SKY_CONTROLLER_3)));
        assert rc != null;
        assertThat(rc.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(false), canBeConnected(true), canBeDisconnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));

        // add local usb rc
        mMockArsdkCore.addDevice("456", RemoteControl.Model.SKY_CONTROLLER_3.id(), "RC1", 2, Backend.TYPE_MUX);
        assertThat(mAddRcCnt, is(1));
        rc = mRCStore.get("456");
        assertThat(rc, allOf(notNullValue(), hasUid("456"), isModel(RemoteControl.Model.SKY_CONTROLLER_3)));
        assert rc != null;
        assertThat(rc.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(false), canBeConnected(true), canBeDisconnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI,
                        DeviceConnectorCore.LOCAL_USB), hasNoActiveConnector()));

        // remove local usb rc
        mMockArsdkCore.removeDevice(2);
        assertThat(mAddRcCnt, is(1));
        assertThat(mRemoveRcCnt, is(0)); // should not be notified (still held by local connector)
        assertThat(rc, allOf(notNullValue(), hasUid("456"), isModel(RemoteControl.Model.SKY_CONTROLLER_3)));
        assertThat(rc.getDeviceStateCore(), allOf(notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED), causeIs(DeviceState.ConnectionStateCause.NONE),
                canBeForgotten(false), canBeConnected(true), canBeDisconnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));

        // remove local wifi rc
        mMockArsdkCore.removeDevice(1);
        assertThat(mAddRcCnt, is(1));
        assertThat(mRemoveRcCnt, is(1));
        assertThat(mRCStore.all(), hasSize(0));
    }

    @NonNull
    private static PersistentStore.Dictionary createDeviceDict(@NonNull PersistentStore store,
                                                               @NonNull String uid,
                                                               @NonNull DeviceModel model,
                                                               @NonNull String name) {
        PersistentStore.Dictionary dict = store.getDevice(uid);
        dict.put(PersistentStore.KEY_DEVICE_MODEL, model.id()).put(PersistentStore.KEY_DEVICE_NAME, name);
        return dict;
    }
}
