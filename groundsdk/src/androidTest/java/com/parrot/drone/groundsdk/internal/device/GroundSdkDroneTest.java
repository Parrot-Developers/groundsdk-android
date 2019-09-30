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

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.internal.GroundSdkTestBase;

import org.junit.Test;

import static com.parrot.drone.groundsdk.DroneMatcher.hasUid;
import static com.parrot.drone.groundsdk.DroneMatcher.isModel;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class GroundSdkDroneTest extends GroundSdkTestBase {

    private GroundSdk gsdk;

    private DroneCore mDrone1;

    private DroneCore mDrone2;

    private int mForgetCnt;

    private int mConnectCnt;

    private int mDisconnectCnt;

    private String mProviderUid;

    private String mPassword;

    @Override
    public void setUp() {
        super.setUp();
        mDrone1 = new DroneCore("1", Drone.Model.ANAFI_4K, "Drone1", new DroneDelegate());
        mDrone2 = new DroneCore("2", Drone.Model.ANAFI_4K, "Drone2", new DroneDelegate());
        gsdk = GroundSdk.newSession(mContext, null);
        gsdk.resume();
        mForgetCnt = mConnectCnt = mDisconnectCnt = 0;
        mProviderUid = mPassword = null;
    }

    @Override
    public void teardown() {
        mMockEngine.removeDrone(mDrone1);
        mMockEngine.removeDrone(mDrone2);
        gsdk.close();
        super.teardown();
    }

    /**
     * Checks that getting an unknown drone returns null.
     */
    @Test
    public void testUnknown() {
        assertThat(gsdk.getDrone("xxx"), is(nullValue()));
    }

    /**
     * Checks that getting a drone by uid returns the correct drone
     */
    @Test
    public void testGetDrone() {
        mMockEngine.addDrone(mDrone1);
        Drone d = gsdk.getDrone("1");
        assertThat(d, is(notNullValue()));
        assertThat(d, allOf(hasUid("1"), isModel(Drone.Model.ANAFI_4K)));
    }

    /**
     * Checks that drone removed callback is called when the drone is removed
     */
    @Test
    public void testGetDroneWithCallback() {
        int changeCnt[] = {0};
        mMockEngine.addDrone(mDrone1);
        mMockEngine.addDrone(mDrone2);

        gsdk.getDrone("1", uid -> {
            assertThat(uid, is(mDrone1.getUid()));
            changeCnt[0]++;
        });

        mMockEngine.removeDrone(mDrone2);
        // check callback has not been called
        assertThat(changeCnt[0], is(0));

        mMockEngine.removeDrone(mDrone1);
        // expect removed callback to be called
        assertThat(changeCnt[0], is(1));

        // check callback is not called again if the drone reappears/disappears from the store
        mMockEngine.addDrone(mDrone1);
        mMockEngine.removeDrone(mDrone1);
        assertThat(changeCnt[0], is(1));

        gsdk.getDrone("2", uid -> changeCnt[0]++);

        // check callback is never called if the drone does not exist when getDrone is called
        mMockEngine.addDrone(mDrone2);
        mMockEngine.removeDrone(mDrone2);
        assertThat(changeCnt[0], is(1));
    }

    /**
     * Checks calling forget on the drone is propagated to the backend
     */
    @Test
    public void testForgetDrone() {
        mMockEngine.addDrone(mDrone1);
        assertThat(gsdk.forgetDrone("1"), is(true));
        assertThat(mForgetCnt, is(1));
        assertThat(gsdk.forgetDrone("2"), is(false));
        assertThat(mForgetCnt, is(1));
    }

    /**
     * Checks calling connect on the drone is propagated to the backend
     */
    @Test
    public void testConnectDrone() {
        mMockEngine.addDrone(mDrone1);

        DeviceConnector connector = DeviceConnectorCore.LOCAL_WIFI;
        assertThat(gsdk.connectDrone("1", connector), is(true));
        assertThat(mConnectCnt, is(1));
        assertThat(mProviderUid, is(connector.getUid()));

        connector = DeviceConnectorCore.createRCConnector("456");
        assertThat(gsdk.connectDrone("1", connector), is(true));
        assertThat(mConnectCnt, is(2));
        assertThat(mProviderUid, is(connector.getUid()));

        assertThat(gsdk.connectDrone("1", connector, "password"), is(true));
        assertThat(mConnectCnt, is(3));
        assertThat(mProviderUid, is(connector.getUid()));
        assertThat(mPassword, is("password"));

        mProviderUid = mPassword = null;
        assertThat(gsdk.connectDrone("2", connector), is(false));
        assertThat(mConnectCnt, is(3));
        assertThat(mProviderUid, nullValue());
        assertThat(mPassword, nullValue());
    }

    /**
     * Checks calling disconnect on the drone is propagated to the backend
     */
    @Test
    public void testDisconnectDrone() {
        mMockEngine.addDrone(mDrone1);

        assertThat(gsdk.disconnectDrone("1"), is(true));
        assertThat(mDisconnectCnt, is(1));
        assertThat(gsdk.disconnectDrone("2"), is(false));
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
            mProviderUid = connector.getUid();
            mPassword = password;
            mConnectCnt++;
            return true;
        }

        @Override
        public boolean disconnect() {
            mDisconnectCnt++;
            return true;
        }
    }
}

