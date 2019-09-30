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

import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.DroneListEntry;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.GroundSdkTestBase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import static com.parrot.drone.groundsdk.DroneListEntryMatcher.hasName;
import static com.parrot.drone.groundsdk.DroneListEntryMatcher.hasUid;
import static com.parrot.drone.groundsdk.DroneListEntryMatcher.isModel;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@SuppressWarnings("ConstantConditions")
public class GroundSdkDroneListTest extends GroundSdkTestBase {

    private GroundSdk gsdk;

    private DroneCore drone1;

    private DroneCore drone2;

    private DroneCore drone3;

    private int mChangeCnt;

    private List<DroneListEntry> mChangeDroneList;

    @Override
    public void setUp() {
        super.setUp();
        drone1 = new DroneCore("1", Drone.Model.ANAFI_4K, "Drone1", null);
        drone2 = new DroneCore("2", Drone.Model.ANAFI_THERMAL, "Drone2", null);
        drone3 = new DroneCore("3", Drone.Model.ANAFI_4K, "Drone3", null);
        gsdk = GroundSdk.newSession(mContext, null);
        mChangeCnt = 0;
        mChangeDroneList = null;
        gsdk.resume();
    }

    @Override
    public void teardown() {
        mMockEngine.removeDrone(drone1);
        mMockEngine.removeDrone(drone2);
        mMockEngine.removeDrone(drone3);
        gsdk.close();
        super.teardown();
    }

    /**
     * Checks that engines are started and stopped
     */
    @Test
    public void testEmptyList() {
        gsdk.getDroneList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, is(notNullValue()));
        assertThat(mChangeDroneList, is(empty()));
    }

    /**
     * Checks get list return the correct list
     */
    @Test
    public void testGetList() {
        mMockEngine.addDrone(drone1);
        mMockEngine.addDrone(drone2);
        mMockEngine.addDrone(drone3);

        gsdk.getDroneList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, is(notNullValue()));
        assertThat(mChangeDroneList, containsInAnyOrder(hasUid("1"), hasUid("2"), hasUid("3")));
    }

    /**
     * Checks list entry data
     */
    @Test
    public void testEntryData() {
        mMockEngine.addDrone(drone1);

        gsdk.getDroneList(it -> true, mObserver);
        assertThat(mChangeDroneList, hasSize(1));

        DroneListEntry entry = mChangeDroneList.get(0);
        assertThat(entry, allOf(hasUid("1"), hasName("Drone1"), isModel(Drone.Model.ANAFI_4K)));
    }

    /**
     * Checks get list return the correct list
     */
    @Test
    public void testGetListWithFilter() {
        mMockEngine.addDrone(drone1);
        mMockEngine.addDrone(drone2);
        mMockEngine.addDrone(drone3);

        gsdk.getDroneList(drone -> drone.getModel() == Drone.Model.ANAFI_4K, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, is(notNullValue()));
        assertThat(mChangeDroneList, hasSize(2));
        assertThat(mChangeDroneList, hasItems(hasUid("1"), hasUid("3")));
    }

    /**
     * Check drone added are notified
     */
    @Test
    public void testDroneAdded() {
        mMockEngine.addDrone(drone1);

        gsdk.getDroneList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, hasSize(1));
        assertThat(mChangeDroneList, hasItems(hasUid("1")));

        mMockEngine.addDrone(drone2);
        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList, hasSize(2));
        assertThat(mChangeDroneList, hasItems(hasUid("1"), hasUid("2")));
    }

    /**
     * Check added drones are properly filtered
     */
    @Test
    public void testDroneAddedFiltered() {
        drone1.updateName("accept1");
        mMockEngine.addDrone(drone1);

        gsdk.getDroneList(droneEntry -> droneEntry.getName().startsWith("accept"), mObserver);

        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, hasSize(1));
        assertThat(mChangeDroneList, hasItems(hasUid("1")));

        drone2.updateName("accept2");
        mMockEngine.addDrone(drone2);

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList, hasSize(2));
        assertThat(mChangeDroneList, hasItems(hasUid("1"), hasUid("2")));

        drone3.updateName("reject3");
        mMockEngine.addDrone(drone3);

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList, hasSize(2));
        assertThat(mChangeDroneList, hasItems(hasUid("1"), hasUid("2")));
    }

    /**
     * Check removed drones are properly filtered
     */
    @Test
    public void testDroneRemovedFiltered() {
        drone1.updateName("accept1");
        drone2.updateName("reject2");
        mMockEngine.addDrone(drone1);
        mMockEngine.addDrone(drone2);

        gsdk.getDroneList(droneEntry -> droneEntry.getName().startsWith("accept"), mObserver);

        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, hasSize(1));
        assertThat(mChangeDroneList, hasItems(hasUid("1")));

        mMockEngine.removeDrone(drone1);

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList, hasSize(0));

        mMockEngine.removeDrone(drone2);

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList, hasSize(0));
    }

    /**
     * Check drone changes are filtered-in when appropriate
     */
    @Test
    public void testDroneChangeFilteredIn() {
        mMockEngine.addDrone(drone1);

        gsdk.getDroneList(droneEntry -> droneEntry.getName().startsWith("accept"), mObserver);

        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, hasSize(0));

        drone1.updateName("accept1");

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList, hasSize(1));
        assertThat(mChangeDroneList, hasItems(hasUid("1"), hasName("accept1")));
    }

    /**
     * Check drone changes are filtered-out when appropriate
     */
    @Test
    public void testDroneChangeFilteredOut() {
        drone1.updateName("accept1");
        mMockEngine.addDrone(drone1);

        gsdk.getDroneList(droneEntry -> droneEntry.getName().startsWith("accept"), mObserver);

        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, hasSize(1));
        assertThat(mChangeDroneList, hasItems(hasUid("1"), hasName("accept1")));

        drone1.updateName("reject1");

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList, hasSize(0));
    }

    /**
     * Check that list items do not change position across drone updates
     */
    @Test
    public void testDroneChangeListPosition() {
        drone1.updateName("accept1");
        mMockEngine.addDrone(drone1);
        drone2.updateName("accept2");
        mMockEngine.addDrone(drone2);
        drone3.updateName("accept3");
        mMockEngine.addDrone(drone3);

        gsdk.getDroneList(droneEntry -> droneEntry.getName().startsWith("accept"), mObserver);

        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, hasSize(3));
        assertThat(mChangeDroneList, containsInAnyOrder(hasUid("1"), hasUid("2"), hasUid("3")));
        List<DroneListEntry> droneList = new ArrayList<>(mChangeDroneList);

        drone2.updateName("accept4");

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList, hasSize(3));
        assertThat(mChangeDroneList, contains(hasUid(droneList.get(0).getUid()), hasUid(droneList.get(1).getUid()),
                hasUid(droneList.get(2).getUid())));
    }

    /**
     * Check drone removed are notified
     */
    @Test
    public void testDroneRemoved() {
        mMockEngine.addDrone(drone1);
        mMockEngine.addDrone(drone2);

        gsdk.getDroneList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, hasSize(2));
        assertThat(mChangeDroneList, hasItems(hasUid("1"), hasUid("2")));

        mMockEngine.removeDrone(drone2);
        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList, hasSize(1));
        assertThat(mChangeDroneList, hasItems(hasUid("1")));

        // check that changes to the removed drone are not notifies
        drone2.updateName("newDrone2");
        drone2.getDeviceStateCore().updateConnectionState(DeviceState.ConnectionState.CONNECTING).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList, hasSize(1));
    }

    /**
     * Check drone name change are notified
     */
    @Test
    public void testDroneNameChanged() {
        mMockEngine.addDrone(drone1);
        mMockEngine.addDrone(drone2);
        mMockEngine.addDrone(drone3);

        gsdk.getDroneList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, hasSize(3));

        drone2.updateName("NewDrone2");
        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList, hasSize(3));
        assertThat(mChangeDroneList, hasItems(hasName("NewDrone2")));
    }

    /**
     * Check drone state change are notified
     */
    @Test
    public void testDroneStateChanged() {
        mMockEngine.addDrone(drone1);

        gsdk.getDroneList(it -> true, mObserver);
        DeviceStateCore state = drone1.getDeviceStateCore();
        // check initial state
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.NONE), canBeForgotten(false), canBeDisconnected(false),
                canBeConnected(false),
                hasNoConnectors(), hasNoActiveConnector()));

        Set<DeviceConnectorCore> connectors = new HashSet<>();
        // add a connector. canBeConnected should switch to true
        connectors.add(DeviceConnectorCore.LOCAL_WIFI);
        state.updateConnectors(connectors).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.NONE), canBeForgotten(false), canBeDisconnected(false),
                canBeConnected(true),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI), hasNoActiveConnector()));

        // move to connecting, canBeConnected should revert to false
        state.updateConnectionState(DeviceState.ConnectionState.CONNECTING,
                DeviceState.ConnectionStateCause.USER_REQUESTED).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mChangeDroneList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(false),
                canBeDisconnected(false),
                canBeConnected(false), hasConnectors(DeviceConnectorCore.LOCAL_WIFI), hasNoActiveConnector()));

        // move to connected with an active local connector, canBeDisconnected should switch to true
        state.updateConnectionState(DeviceState.ConnectionState.CONNECTED)
             .updateActiveConnector(DeviceConnectorCore.LOCAL_WIFI).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mChangeDroneList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.CONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false), hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        // mark as persisted, canBeForgotten should switch to true
        state.updatePersisted(true).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        assertThat(mChangeDroneList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.CONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(true), canBeDisconnected(true),
                canBeConnected(false), hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        // add a connector that supports forget (RC) and remove persisted state, canForget should remain true
        connectors.add(DeviceConnectorCore.createRCConnector("456"));
        state.updateConnectors(connectors).updatePersisted(false).notifyUpdated();
        assertThat(mChangeCnt, is(6));
        assertThat(mChangeDroneList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.CONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(true), canBeDisconnected(true),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI, DeviceConnectorCore.createRCConnector("456")),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        // make RC connector active, canDisconnect should revert to false (RC does not support disconnect)
        state.updateActiveConnector(DeviceConnectorCore.createRCConnector("456")).notifyUpdated();
        assertThat(mChangeCnt, is(7));
        assertThat(mChangeDroneList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.CONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(true),
                canBeDisconnected(false),
                canBeConnected(false),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI, DeviceConnectorCore.createRCConnector("456")),
                activeConnector(DeviceConnectorCore.createRCConnector("456"))));

        // remove all connectors and disconnect
        state.updateConnectors(Collections.emptySet()).updateActiveConnector(null)
             .updateConnectionState(DeviceState.ConnectionState.DISCONNECTED,
                     DeviceState.ConnectionStateCause.CONNECTION_LOST).notifyUpdated();
        assertThat(mChangeCnt, is(8));
        assertThat(mChangeDroneList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.CONNECTION_LOST), canBeForgotten(false),
                canBeDisconnected(false),
                canBeConnected(false), hasNoConnectors(), hasNoActiveConnector()));
    }

    /**
     * Check drone firmware version changes are notified.
     * This behavior is unwanted but for the moment we accept that changing the firmware version notifies a change
     * even if this change won't be seen from the API.
     */
    @Test
    public void testDroneFirmwareVersionChanged() {
        mMockEngine.addDrone(drone1);
        mMockEngine.addDrone(drone2);

        gsdk.getDroneList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, hasSize(2));

        drone2.updateFirmwareVersion(FirmwareVersion.parse("1.2.3"));
        assertThat(mChangeCnt, is(2));
        assertThat(mChangeDroneList, hasSize(2));
    }

    /**
     * Checks the obtained list is never null and never modifiable
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testListNotNullNotModifiable() {
        mMockEngine.addDrone(drone1);
        mMockEngine.addDrone(drone2);
        mMockEngine.addDrone(drone3);

        gsdk.getDroneList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeDroneList, notNullValue());

        mChangeDroneList.clear();
    }

    private final Ref.Observer<List<DroneListEntry>> mObserver = new Ref.Observer<List<DroneListEntry>>() {

        @Override
        public void onChanged(@Nullable List<DroneListEntry> list) {
            mChangeCnt++;
            mChangeDroneList = list;
        }
    };
}

