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
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.RemoteControlListEntry;
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
import static com.parrot.drone.groundsdk.RcListEntryMatcher.hasName;
import static com.parrot.drone.groundsdk.RcListEntryMatcher.hasUid;
import static com.parrot.drone.groundsdk.RcListEntryMatcher.isModel;
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
public class GroundSdkRcListTest extends GroundSdkTestBase {

    private GroundSdk gsdk;

    private RemoteControlCore rc1;

    private RemoteControlCore rc2;

    private RemoteControlCore rc3;

    private int mChangeCnt;

    private List<RemoteControlListEntry> mChangeRcList;

    @Override
    public void setUp() {
        super.setUp();
        rc1 = new RemoteControlCore("1", RemoteControl.Model.SKY_CONTROLLER_3, "RC1", null);
        rc2 = new RemoteControlCore("2", RemoteControl.Model.SKY_CONTROLLER_3, "RC2", null);
        rc3 = new RemoteControlCore("3", RemoteControl.Model.SKY_CONTROLLER_3, "RC3", null);
        gsdk = GroundSdk.newSession(mContext, null);

        mChangeCnt = 0;
        mChangeRcList = null;
        gsdk.resume();
    }

    @Override
    public void teardown() {
        mMockEngine.removeRemoteControl(rc1);
        mMockEngine.removeRemoteControl(rc2);
        mMockEngine.removeRemoteControl(rc3);
        gsdk.close();
        super.teardown();
    }

    /**
     * Checks that engines are started and stopped
     */
    @Test
    public void testEmptyList() {
        gsdk.getRemoteControlList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, is(notNullValue()));
        assertThat(mChangeRcList, is(empty()));
    }

    /**
     * Checks get list return the correct list
     */
    @Test
    public void testGetList() {
        mMockEngine.addRemoteControl(rc1);
        mMockEngine.addRemoteControl(rc2);
        mMockEngine.addRemoteControl(rc3);

        gsdk.getRemoteControlList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, is(notNullValue()));
        assertThat(mChangeRcList, containsInAnyOrder(hasUid("1"), hasUid("2"), hasUid("3")));
    }

    /**
     * Checks list entry data
     */
    @Test
    public void testEntryData() {
        mMockEngine.addRemoteControl(rc1);

        gsdk.getRemoteControlList(it -> true, mObserver);
        assertThat(mChangeRcList, hasSize(1));

        RemoteControlListEntry entry = mChangeRcList.get(0);
        assertThat(entry, allOf(hasUid("1"), hasName("RC1"), isModel(RemoteControl.Model.SKY_CONTROLLER_3)));
    }

    /**
     * Checks get list return the correct list
     */
    @Test
    public void testGetListWithFilter() {
        rc2.updateName("rejectRC2");
        mMockEngine.addRemoteControl(rc1);
        mMockEngine.addRemoteControl(rc2);
        mMockEngine.addRemoteControl(rc3);

        gsdk.getRemoteControlList(rc -> !rc.getName().startsWith("reject"), mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, is(notNullValue()));
        assertThat(mChangeRcList, hasSize(2));
        assertThat(mChangeRcList, hasItems(hasUid("1"), hasUid("3")));
    }

    /**
     * Check RCs added are notified
     */
    @Test
    public void testRcAdded() {
        mMockEngine.addRemoteControl(rc1);

        gsdk.getRemoteControlList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, hasSize(1));
        assertThat(mChangeRcList, hasItems(hasUid("1")));

        mMockEngine.addRemoteControl(rc2);
        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList, hasSize(2));
        assertThat(mChangeRcList, hasItems(hasUid("1"), hasUid("2")));
    }

    /**
     * Check added RCs are properly filtered
     */
    @Test
    public void testRcAddedFiltered() {
        rc1.updateName("accept1");
        mMockEngine.addRemoteControl(rc1);

        gsdk.getRemoteControlList(rcEntry -> rcEntry.getName().startsWith("accept"), mObserver);

        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, hasSize(1));
        assertThat(mChangeRcList, hasItems(hasUid("1")));

        rc2.updateName("accept2");
        mMockEngine.addRemoteControl(rc2);

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList, hasSize(2));
        assertThat(mChangeRcList, hasItems(hasUid("1"), hasUid("2")));

        rc3.updateName("reject3");
        mMockEngine.addRemoteControl(rc3);

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList, hasSize(2));
        assertThat(mChangeRcList, hasItems(hasUid("1"), hasUid("2")));
    }

    /**
     * Check removed RCs are properly filtered
     */
    @Test
    public void testRcRemovedFiltered() {
        rc1.updateName("accept1");
        rc2.updateName("reject2");
        mMockEngine.addRemoteControl(rc1);
        mMockEngine.addRemoteControl(rc2);

        gsdk.getRemoteControlList(rcEntry -> rcEntry.getName().startsWith("accept"), mObserver);

        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, hasSize(1));
        assertThat(mChangeRcList, hasItems(hasUid("1")));

        mMockEngine.removeRemoteControl(rc1);

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList, hasSize(0));

        mMockEngine.removeRemoteControl(rc2);

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList, hasSize(0));
    }

    /**
     * Check RC changes are filtered-in when appropriate
     */
    @Test
    public void testRcChangeFilteredIn() {
        mMockEngine.addRemoteControl(rc1);

        gsdk.getRemoteControlList(rcEntry -> rcEntry.getName().startsWith("accept"), mObserver);

        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, hasSize(0));

        rc1.updateName("accept1");

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList, hasSize(1));
        assertThat(mChangeRcList, hasItems(hasUid("1"), hasName("accept1")));
    }

    /**
     * Check RC changes are filtered-out when appropriate
     */
    @Test
    public void testRcChangeFilteredOut() {
        rc1.updateName("accept1");
        mMockEngine.addRemoteControl(rc1);

        gsdk.getRemoteControlList(rcEntry -> rcEntry.getName().startsWith("accept"), mObserver);

        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, hasSize(1));
        assertThat(mChangeRcList, hasItems(hasUid("1"), hasName("accept1")));

        rc1.updateName("reject1");

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList, hasSize(0));
    }

    /**
     * Check that list items do not change position across RC updates
     */
    @Test
    public void testRcChangeListPosition() {
        rc1.updateName("accept1");
        mMockEngine.addRemoteControl(rc1);
        rc2.updateName("accept2");
        mMockEngine.addRemoteControl(rc2);
        rc3.updateName("accept3");
        mMockEngine.addRemoteControl(rc3);

        gsdk.getRemoteControlList(rcEntry -> rcEntry.getName().startsWith("accept"), mObserver);

        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, hasSize(3));
        assertThat(mChangeRcList, containsInAnyOrder(hasUid("1"), hasUid("2"), hasUid("3")));
        List<RemoteControlListEntry> rcList = new ArrayList<>(mChangeRcList);

        rc2.updateName("accept4");

        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList, hasSize(3));
        assertThat(mChangeRcList, contains(hasUid(rcList.get(0).getUid()), hasUid(rcList.get(1).getUid()),
                hasUid(rcList.get(2).getUid())));
    }

    /**
     * Check RCs removed are notified
     */
    @Test
    public void testRcRemoved() {
        mMockEngine.addRemoteControl(rc1);
        mMockEngine.addRemoteControl(rc2);

        gsdk.getRemoteControlList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, hasSize(2));
        assertThat(mChangeRcList, hasItems(hasUid("1"), hasUid("2")));

        mMockEngine.removeRemoteControl(rc2);
        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList, hasSize(1));
        assertThat(mChangeRcList, hasItems(hasUid("1")));

        // check that changes to the removed RC are not notified
        rc2.updateName("NewRc2");
        rc2.getDeviceStateCore().updateConnectionState(DeviceState.ConnectionState.CONNECTING).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList, hasSize(1));
    }

    /**
     * Check RC name change are notified
     */
    @Test
    public void testRcNameChanged() {
        mMockEngine.addRemoteControl(rc1);
        mMockEngine.addRemoteControl(rc2);
        mMockEngine.addRemoteControl(rc3);

        gsdk.getRemoteControlList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, hasSize(3));

        rc2.updateName("NewRc2");
        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList, hasSize(3));
        assertThat(mChangeRcList, hasItems(hasName("NewRc2")));
    }

    /**
     * Check rc firmware version changes are notified.
     * This behavior is unwanted but for the moment we accept that changing the firmware version notifies a change
     * even if this change won't be seen from the API.
     */
    @Test
    public void testRcFirmwareVersionChanged() {
        mMockEngine.addRemoteControl(rc1);
        mMockEngine.addRemoteControl(rc2);

        gsdk.getRemoteControlList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, hasSize(2));

        rc2.updateFirmwareVersion(FirmwareVersion.parse("1.2.3"));
        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList, hasSize(2));
    }

    /**
     * Check RC state change are notified
     */
    @Test
    public void testRcStateChanged() {
        mMockEngine.addRemoteControl(rc1);

        gsdk.getRemoteControlList(it -> true, mObserver);
        DeviceStateCore state = rc1.getDeviceStateCore();
        // check initial state
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList.get(0).getState(), allOf(
                stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.NONE), canBeForgotten(false), canBeDisconnected(false),
                canBeConnected(false),
                hasNoConnectors(), hasNoActiveConnector()));

        Set<DeviceConnectorCore> connectors = new HashSet<>();
        // add a connector. canBeConnected should switch to true
        connectors.add(DeviceConnectorCore.LOCAL_WIFI);
        state.updateConnectors(connectors).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mChangeRcList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.NONE), canBeForgotten(false), canBeDisconnected(false),
                canBeConnected(true),
                hasConnectors(DeviceConnectorCore.LOCAL_WIFI), hasNoActiveConnector()));

        // move to connecting, canBeConnected should revert to false
        state.updateConnectionState(DeviceState.ConnectionState.CONNECTING,
                DeviceState.ConnectionStateCause.USER_REQUESTED).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mChangeRcList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.CONNECTING),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(false),
                canBeDisconnected(false),
                canBeConnected(false), hasConnectors(DeviceConnectorCore.LOCAL_WIFI), hasNoActiveConnector()));

        // move to connected with an active local connector, canBeDisconnected should switch to true
        state.updateConnectionState(DeviceState.ConnectionState.CONNECTED)
             .updateActiveConnector(DeviceConnectorCore.LOCAL_WIFI).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(mChangeRcList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.CONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(false),
                canBeDisconnected(true),
                canBeConnected(false), hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        // mark as persisted, canBeForgotten should switch to true
        state.updatePersisted(true).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        assertThat(mChangeRcList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.CONNECTED),
                causeIs(DeviceState.ConnectionStateCause.USER_REQUESTED), canBeForgotten(true), canBeDisconnected(true),
                canBeConnected(false), hasConnectors(DeviceConnectorCore.LOCAL_WIFI),
                activeConnector(DeviceConnectorCore.LOCAL_WIFI)));

        // remove all connectors and disconnect
        state.updateConnectors(Collections.emptySet()).updateActiveConnector(null)
             .updateConnectionState(DeviceState.ConnectionState.DISCONNECTED,
                     DeviceState.ConnectionStateCause.CONNECTION_LOST).notifyUpdated();
        assertThat(mChangeCnt, is(6));
        assertThat(mChangeRcList.get(0).getState(), allOf(stateIs(DeviceState.ConnectionState.DISCONNECTED),
                causeIs(DeviceState.ConnectionStateCause.CONNECTION_LOST), canBeForgotten(true),
                canBeDisconnected(false),
                canBeConnected(false), hasNoConnectors(), hasNoActiveConnector()));
    }

    /**
     * Checks the obtained list is never null and never modifiable
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testListNotNullNotModifiable() {
        mMockEngine.addRemoteControl(rc1);
        mMockEngine.addRemoteControl(rc2);
        mMockEngine.addRemoteControl(rc3);

        gsdk.getRemoteControlList(it -> true, mObserver);
        assertThat(mChangeCnt, is(1));
        assertThat(mChangeRcList, notNullValue());

        mChangeRcList.clear();
    }

    private final Ref.Observer<List<RemoteControlListEntry>> mObserver =
            new Ref.Observer<List<RemoteControlListEntry>>() {

                @Override
                public void onChanged(@Nullable List<RemoteControlListEntry> list) {
                    mChangeCnt++;
                    mChangeRcList = list;
                }
            };
}

