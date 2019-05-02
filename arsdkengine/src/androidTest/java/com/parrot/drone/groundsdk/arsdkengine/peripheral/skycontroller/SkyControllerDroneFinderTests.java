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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.DroneFinder;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureDroneManager;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class SkyControllerDroneFinderTests extends ArsdkEngineTestBase {

    private RemoteControlCore mRemoteControl;

    private DroneFinder mDroneFinder;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("456", RemoteControl.Model.SKY_CONTROLLER_3.id(), "RC", 1, Backend.TYPE_MUX);
        mRemoteControl = mRCStore.get("456");
        assert mRemoteControl != null;

        mDroneFinder = mRemoteControl.getPeripheralStore().get(mMockSession, DroneFinder.class);
        mRemoteControl.getPeripheralStore().registerObserver(DroneFinder.class, () -> {
            mDroneFinder = mRemoteControl.getPeripheralStore().get(mMockSession, DroneFinder.class);
            mChangeCnt++;
        });
    }

    @Test
    public void testPublication() {
        // should be unavailable when the drone is not connected
        assertThat(mDroneFinder, is(nullValue()));

        connectRemoteControl(mRemoteControl, 1);
        assertThat(mDroneFinder, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectRemoteControl(mRemoteControl, 1);
        assertThat(mDroneFinder, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testRefreshAndState() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mChangeCnt, is(1));
        assertThat(mDroneFinder.getState(), is(DroneFinder.State.IDLE));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.droneManagerDiscoverDrones(), false));
        mDroneFinder.refresh();
        mMockArsdkCore.assertNoExpectation();
        assertThat(mChangeCnt, is(2));
        assertThat(mDroneFinder.getState(), is(DroneFinder.State.SCANNING));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerDroneListItem("1",
                Drone.Model.ANAFI_4K.id(), "Anafi", 0, 0, 1, ArsdkFeatureDroneManager.Security.NONE, 0, -20,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mChangeCnt, is(3));
        assertThat(mDroneFinder.getState(), is(DroneFinder.State.IDLE));
        assertThat(mDroneFinder.getDiscoveredDrones(), hasSize(1));
    }

    @Test
    public void testDiscoveredDroneList() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mChangeCnt, is(1));
        assertThat(mDroneFinder.getDiscoveredDrones(), empty());

        // add first
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerDroneListItem("1",
                Drone.Model.ANAFI_4K.id(), "Anafi1", 0, 0, 1, ArsdkFeatureDroneManager.Security.NONE, 0, -20,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));

        // should not be notified until 'last'
        assertThat(mChangeCnt, is(1));

        // add item (neither first, nor last)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerDroneListItem("2",
                Drone.Model.ANAFI_THERMAL.id(), "AnafiThermal", 1, 0, 1, ArsdkFeatureDroneManager.Security.WPA2, 0, -40,
                ArsdkFeatureGeneric.ListFlags.toBitField()));
        assertThat(mChangeCnt, is(1));

        // add last
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerDroneListItem("3",
                Drone.Model.ANAFI_4K.id(), "Anafi2", 0, 0, 1, ArsdkFeatureDroneManager.Security.WPA2, 1, -60,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mChangeCnt, is(2));
        assertThat(mDroneFinder.getDiscoveredDrones(), hasSize(3));
        assertThat(mDroneFinder.getDiscoveredDrones(), containsInAnyOrder(
                allOf(hasUid("1"), isModel(Drone.Model.ANAFI_4K), hasName("Anafi1"), hasSecurity(
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.NONE), hasRssi(-20), isKnown(false)),
                allOf(hasUid("2"), isModel(Drone.Model.ANAFI_THERMAL), hasName("AnafiThermal"), hasSecurity(
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.PASSWORD), hasRssi(-40), isKnown(true)),
                allOf(hasUid("3"), isModel(Drone.Model.ANAFI_4K), hasName("Anafi2"), hasSecurity(
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.SAVED_PASSWORD), hasRssi(-60),
                        isKnown(false))));

        // remove
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerDroneListItem("1",
                Drone.Model.ANAFI_4K.id(), "Anafi1", 0, 0, 1, ArsdkFeatureDroneManager.Security.NONE, 0, -20,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.REMOVE,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mChangeCnt, is(3));
        assertThat(mDroneFinder.getDiscoveredDrones(), hasSize(2));
        assertThat(mDroneFinder.getDiscoveredDrones(), containsInAnyOrder(
                allOf(hasUid("2"), isModel(Drone.Model.ANAFI_THERMAL), hasName("AnafiThermal"), hasSecurity(
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.PASSWORD), hasRssi(-40), isKnown(true)),
                allOf(hasUid("3"), isModel(Drone.Model.ANAFI_4K), hasName("Anafi2"), hasSecurity(
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.SAVED_PASSWORD), hasRssi(-60),
                        isKnown(false))));

        // empty
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerDroneListItem("1",
                Drone.Model.ANAFI_4K.id(), "Anafi1", 0, 0, 1, ArsdkFeatureDroneManager.Security.NONE, 0, -20,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.EMPTY)));
        assertThat(mChangeCnt, is(4));
        assertThat(mDroneFinder.getDiscoveredDrones(), empty());

        //first
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerDroneListItem("1",
                Drone.Model.ANAFI_4K.id(), "Anafi1", 0, 0, 1, ArsdkFeatureDroneManager.Security.NONE, 0, -20,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerDroneListItem("2",
                Drone.Model.ANAFI_THERMAL.id(), "AnafiThermal", 1, 0, 1, ArsdkFeatureDroneManager.Security.WPA2, 0, -70,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mChangeCnt, is(5));
        assertThat(mDroneFinder.getDiscoveredDrones(), hasSize(1));
        assertThat(mDroneFinder.getDiscoveredDrones(), contains(
                allOf(hasUid("2"), isModel(Drone.Model.ANAFI_THERMAL), hasName("AnafiThermal"), hasSecurity(
                        DroneFinder.DiscoveredDrone.ConnectionSecurity.PASSWORD), hasRssi(-70), isKnown(true))));
    }

    @Test
    public void testConnect() {
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeDroneManagerDroneListItem("1",
                Drone.Model.ANAFI_4K.id(), "Anafi", 0, 0, 1, ArsdkFeatureDroneManager.Security.NONE, 0, -20,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));

        assertThat(mChangeCnt, is(2));
        assertThat(mDroneFinder.getDiscoveredDrones(), hasSize(1));
        DroneFinder.DiscoveredDrone discoveredDrone = mDroneFinder.getDiscoveredDrones().get(0);

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.droneManagerConnect("1", "password"), false));
        mDroneFinder.connect(discoveredDrone, "password");
        mMockArsdkCore.assertNoExpectation();

        // mock the drone is connecting so that ArsdkProxy has an active device we can then disconnect
        discoveredDroneConnecting(1, discoveredDrone);
        // disconnect the drone so that we can connect again
        discoveredDroneDisconnected(1, discoveredDrone);

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.droneManagerConnect("1", ""), false));
        mDroneFinder.connect(discoveredDrone);
        mMockArsdkCore.assertNoExpectation();
    }
}
