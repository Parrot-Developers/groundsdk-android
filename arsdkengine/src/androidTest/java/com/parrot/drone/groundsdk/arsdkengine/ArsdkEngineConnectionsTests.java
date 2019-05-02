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

import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.internal.device.DeviceConnectorCore;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;

import org.junit.Test;

import static com.parrot.drone.groundsdk.DeviceStateMatcher.activeConnector;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.canBeConnected;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.canBeDisconnected;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.canBeForgotten;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.causeIs;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.hasConnectors;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.hasNoActiveConnector;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.hasNoConnectors;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.stateIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;

public class ArsdkEngineConnectionsTests extends ArsdkEngineTestBase {

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
    }

    @Test
    public void testConnectDisconnectDrone() {
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        DroneCore drone = mDroneStore.get("123");
        assertThat(drone, notNullValue());

        mMockArsdkCore.expect(new Expectation.Connect(1));
        drone.connect(DeviceConnectorCore.LOCAL_WIFI, null);

        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        mMockArsdkCore.deviceConnecting(1);
        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        // after link-level connection, we expect to send a date, time and get all settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonCommonCurrentDateTime(""), false));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonSettingsAllSettings()));
        mMockArsdkCore.deviceConnected(1);

        // after receiving the all settings ended, we expect to send the get all states
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonCommonAllStates()));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateAllSettingsChanged());

        // after receiving the all states ended, we expect the state to be connected
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateAllStatesChanged());

        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.CONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(true),
                canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        mMockArsdkCore.expect(new Expectation.Disconnect(1));
        drone.disconnect();
        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(true),
                canBeDisconnected(false),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        mMockArsdkCore.deviceDisconnected(1, false);
        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(true),
                canBeDisconnected(false),
                canBeConnected(true),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));
    }

    @Test
    public void testConnectDroneCancel() {
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        DroneCore drone = mDroneStore.get("123");
        assertThat(drone, notNullValue());

        mMockArsdkCore.expect(new Expectation.Connect(1));
        drone.connect(DeviceConnectorCore.LOCAL_WIFI, null);

        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        mMockArsdkCore.deviceConnecting(1);
        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        // disconnect before getting connected
        mMockArsdkCore.expect(new Expectation.Disconnect(1));
        drone.disconnect();
        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(false),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        mMockArsdkCore.deviceDisconnected(1, false);
        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(false),
                canBeConnected(true),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));
    }

    @Test
    public void testConnectDroneRejected() {
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        DroneCore drone = mDroneStore.get("123");
        assertThat(drone, notNullValue());

        mMockArsdkCore.expect(new Expectation.Connect(1));
        drone.connect(DeviceConnectorCore.LOCAL_WIFI, null);

        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        mMockArsdkCore.deviceConnectionCanceled(1, ArsdkDevice.REASON_REJECTED_BY_REMOTE, false);
        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.REFUSED),
                canBeForgotten(false),
                canBeDisconnected(false),
                canBeConnected(true),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));
    }

    @Test
    public void testDisconnectRemovingDrone() {
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        DroneCore drone = mDroneStore.get("123");
        assertThat(drone, notNullValue());

        mMockArsdkCore.expect(new Expectation.Connect(1));
        drone.connect(DeviceConnectorCore.LOCAL_WIFI, null);
        mMockArsdkCore.deviceConnecting(1);

        // after link-level connection, we expect to send a date, time and get all settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonCommonCurrentDateTime(""), false));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonSettingsAllSettings()));
        mMockArsdkCore.deviceConnected(1);

        // after receiving the all settings ended, we expect to send the get all states
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonCommonAllStates()));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateAllSettingsChanged());

        // after receiving the all states ended, we expect the state to be connected
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateAllStatesChanged());

        // simulate disconnect with removing set to true
        // this should trigger auto-reconnection
        mMockArsdkCore.expect(new Expectation.Connect(1));
        mMockArsdkCore.deviceDisconnected(1, true);

        // expect device disconnected and not connectable (i.e. without any connectors)
        // reason should be CONNECTION_LOST
        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.CONNECTION_LOST),
                canBeForgotten(true),
                canBeDisconnected(false),
                canBeConnected(false),
                hasNoConnectors(),
                hasNoActiveConnector()));
    }

    @Test
    public void testConnectingCancelRemovingDrone() {
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        DroneCore drone = mDroneStore.get("123");
        assertThat(drone, notNullValue());

        mMockArsdkCore.expect(new Expectation.Connect(1));
        drone.connect(DeviceConnectorCore.LOCAL_WIFI, null);
        mMockArsdkCore.deviceConnecting(1);

        // simulate disconnect with removing set to true
        // this should trigger auto-reconnection
        mMockArsdkCore.expect(new Expectation.Connect(1));
        mMockArsdkCore.deviceConnectionCanceled(1, ArsdkDevice.REASON_CANCELED_LOCALLY, true);

        // expect device disconnected and not connectable (i.e. without any connectors)
        // reason should be CONNECTION_LOST
        assertThat(drone.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.CONNECTION_LOST),
                canBeForgotten(false),
                canBeDisconnected(false),
                canBeConnected(false),
                hasNoConnectors(),
                hasNoActiveConnector()));
    }

    @Test
    public void testConnectDisconnectRemoteControl() {
        mMockArsdkCore.addDevice("123", RemoteControl.Model.SKY_CONTROLLER_3.id(), "RC1", 1, Backend.TYPE_NET);
        mMockArsdkCore.addDevice("123", RemoteControl.Model.SKY_CONTROLLER_3.id(), "RC1", 2, Backend.TYPE_MUX);
        RemoteControlCore rc = mRCStore.get("123");
        assertThat(rc, notNullValue());

        mMockArsdkCore.expect(new Expectation.Connect(2));
        rc.connect(DeviceConnectorCore.LOCAL_USB, null);

        assertThat(rc.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI, DeviceConnectorCore.LOCAL_USB),
                activeConnector(DeviceConnectorCore.LOCAL_USB)));

        mMockArsdkCore.deviceConnecting(2);
        assertThat(rc.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI, DeviceConnectorCore.LOCAL_USB),
                activeConnector(DeviceConnectorCore.LOCAL_USB)));

        // after link-level connection, we expect to send a date, time and get all settings
        mMockArsdkCore.expect(new Expectation.Command(2, ExpectedCmd.skyctrlCommonCurrentDateTime(""), false));
        mMockArsdkCore.expect(new Expectation.Command(2, ExpectedCmd.skyctrlSettingsAllSettings()));
        mMockArsdkCore.deviceConnected(2);

        // after receiving the all settings ended, we expect to send the get all states
        mMockArsdkCore.expect(new Expectation.Command(2, ExpectedCmd.skyctrlCommonAllStates()));
        mMockArsdkCore.commandReceived(2, ArsdkEncoder.encodeSkyctrlSettingsStateAllSettingsChanged());

        // after receiving the all states ended, we expect the state to be connected
        mMockArsdkCore.commandReceived(2, ArsdkEncoder.encodeSkyctrlCommonStateAllStatesChanged());

        assertThat(rc.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.CONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(true),
                canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI, DeviceConnectorCore.LOCAL_USB),
                activeConnector(DeviceConnectorCore.LOCAL_USB)));

        mMockArsdkCore.expect(new Expectation.Disconnect(2));
        rc.disconnect();
        assertThat(rc.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(true),
                canBeDisconnected(false),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI, DeviceConnectorCore.LOCAL_USB),
                activeConnector(DeviceConnectorCore.LOCAL_USB)));

        mMockArsdkCore.deviceDisconnected(2, false);
        assertThat(rc.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(true),
                canBeDisconnected(false),
                canBeConnected(true),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI, DeviceConnectorCore.LOCAL_USB),
                hasNoActiveConnector()));
    }

    @Test
    public void testConnectRemoteControlCancel() {
        mMockArsdkCore.addDevice("123", RemoteControl.Model.SKY_CONTROLLER_3.id(), "RC1", 1, Backend.TYPE_NET);
        RemoteControlCore rc = mRCStore.get("123");
        assertThat(rc, notNullValue());

        mMockArsdkCore.expect(new Expectation.Connect(1));
        rc.connect(DeviceConnectorCore.LOCAL_WIFI, null);

        assertThat(rc.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        mMockArsdkCore.deviceConnecting(1);
        assertThat(rc.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        // disconnect before getting connected
        mMockArsdkCore.expect(new Expectation.Disconnect(1));
        rc.disconnect();
        assertThat(rc.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(false),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        mMockArsdkCore.deviceDisconnected(1, false);
        assertThat(rc.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(false),
                canBeConnected(true),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));
    }

    @Test
    public void testConnectRemoteControlRejected() {
        mMockArsdkCore.addDevice("123", RemoteControl.Model.SKY_CONTROLLER_3.id(), "RC1", 1, Backend.TYPE_NET);
        RemoteControlCore rc = mRCStore.get("123");
        assertThat(rc, notNullValue());

        mMockArsdkCore.expect(new Expectation.Connect(1));
        rc.connect(DeviceConnectorCore.LOCAL_WIFI, null);

        assertThat(rc.getDeviceStateCore(), allOf(
                notNullValue(), stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED),
                canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        mMockArsdkCore.deviceConnectionCanceled(1, ArsdkDevice.REASON_REJECTED_BY_REMOTE, false);
        assertThat(rc.getDeviceStateCore(), allOf(
                notNullValue(),
                stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.REFUSED),
                canBeForgotten(false),
                canBeDisconnected(false),
                canBeConnected(true),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                hasNoActiveConnector()));
    }
}
