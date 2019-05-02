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
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureDroneManager;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import static com.parrot.drone.groundsdk.DeviceStateMatcher.causeIs;
import static com.parrot.drone.groundsdk.DeviceStateMatcher.stateIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class DroneManagerFeatureTests extends ArsdkEngineTestBase {

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("456", RemoteControl.Model.SKY_CONTROLLER_3.id(), "RC", 1, Backend.TYPE_MUX);
        RemoteControlCore remoteControl = mRCStore.get("456");
        assert remoteControl != null;
        connectRemoteControl(remoteControl, 1);
    }

    @Test
    public void testKnownDroneList() {
        // add one drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerKnownDroneItem("11",
                Drone.Model.ANAFI_4K.id(), "d1", ArsdkFeatureDroneManager.Security.NONE, 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));

        assertThat(mDroneStore.all(), hasSize(1));
        assertThat(mDroneStore.get("11"), notNullValue());

        // add (new list)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerKnownDroneItem("22",
                Drone.Model.ANAFI_4K.id(), "d2", ArsdkFeatureDroneManager.Security.NONE, 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));

        assertThat(mDroneStore.all(), hasSize(1));
        assertThat(mDroneStore.get("22"), notNullValue());

        // add one more device
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerKnownDroneItem("33",
                Drone.Model.ANAFI_4K.id(), "d3", ArsdkFeatureDroneManager.Security.NONE, 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));

        assertThat(mDroneStore.all(), hasSize(2));
        assertThat(mDroneStore.get("22"), notNullValue());
        assertThat(mDroneStore.get("33"), notNullValue());

        // remove one device
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerKnownDroneItem("22",
                Drone.Model.ANAFI_4K.id(), "d2", ArsdkFeatureDroneManager.Security.NONE, 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.REMOVE)));

        assertThat(mDroneStore.all(), hasSize(1));
        assertThat(mDroneStore.get("22"), nullValue());
        assertThat(mDroneStore.get("33"), notNullValue());

        // remove all
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerKnownDroneItem("33",
                Drone.Model.ANAFI_4K.id(), "d3", ArsdkFeatureDroneManager.Security.NONE, 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.EMPTY)));

        assertThat(mDroneStore.all(), empty());
        assertThat(mDroneStore.get("33"), nullValue());
    }

    @Test
    public void testDroneConnectionState() {
        // add one drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerKnownDroneItem("11",
                Drone.Model.ANAFI_4K.id(), "d1", ArsdkFeatureDroneManager.Security.NONE, 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));

        assertThat(mDroneStore.all(), hasSize(1));
        DroneCore drone = mDroneStore.get("11");
        assertThat(drone, notNullValue());

        // state connecting
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerConnectionState(
                ArsdkFeatureDroneManager.ConnectionState.CONNECTING, "11", Drone.Model.ANAFI_4K.id(), "d1"));
        // expect the drone to be connecting
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTING));

        // state connected
        // expect connection to the remote drone
        expectDateAccordingToDrone(drone, 1);
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonSettingsAllSettings()));
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonCommonAllStates()));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerConnectionState(
                ArsdkFeatureDroneManager.ConnectionState.CONNECTED, "11", Drone.Model.ANAFI_4K.id(), "d1"));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateAllSettingsChanged());
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateAllStatesChanged());
        // expect drone to be connected
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.CONNECTED));

        // state disconnecting
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerConnectionState(
                ArsdkFeatureDroneManager.ConnectionState.DISCONNECTING, "11", Drone.Model.ANAFI_4K.id(),
                "d1"));
        // expect drone to be disconnected
        assertThat(drone.getDeviceStateCore(), stateIs(DeviceState.ConnectionState.DISCONNECTED));
    }

    @Test
    public void testDroneAuthenticationFailure() {
        // add one drone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerKnownDroneItem("11",
                Drone.Model.ANAFI_4K.id(), "d1", ArsdkFeatureDroneManager.Security.NONE, 0,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));

        assertThat(mDroneStore.all(), hasSize(1));
        DroneCore drone = mDroneStore.get("11");
        assertThat(drone, notNullValue());

        // state connecting
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerConnectionState(
                ArsdkFeatureDroneManager.ConnectionState.CONNECTING, "11", Drone.Model.ANAFI_4K.id(), "d1"));

        // authentication failure
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerAuthenticationFailed("11",
                Drone.Model.ANAFI_4K.id(), "d1"));

        assertThat(drone.getDeviceStateCore(), both(stateIs(DeviceState.ConnectionState.DISCONNECTED))
                .and(causeIs(DeviceState.ConnectionStateCause.BAD_PASSWORD)));
    }
}
