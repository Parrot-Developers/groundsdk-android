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
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.internal.GroundSdkTestBase;

import org.junit.Test;

import static com.parrot.drone.groundsdk.RemoteControlMatcher.hasUid;
import static com.parrot.drone.groundsdk.RemoteControlMatcher.isModel;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class GroundSdkRcTest extends GroundSdkTestBase {

    private GroundSdk gsdk;

    private RemoteControlCore mRc1;

    private RemoteControlCore mRc2;

    private int mForgetCnt;

    private int mConnectCnt;

    private int mDisconnectCnt;

    private String mProviderUid;

    private String mPassword;

    @Override
    public void setUp() {
        super.setUp();
        mRc1 = new RemoteControlCore("1", RemoteControl.Model.SKY_CONTROLLER_3, "RC1", new RcDelegate());
        mRc2 = new RemoteControlCore("2", RemoteControl.Model.SKY_CONTROLLER_3, "RC2", new RcDelegate());
        gsdk = GroundSdk.newSession(mContext, null);
        gsdk.resume();
        mForgetCnt = mConnectCnt = mDisconnectCnt = 0;
        mProviderUid = mPassword = null;
    }

    @Override
    public void teardown() {
        mMockEngine.removeRemoteControl(mRc1);
        mMockEngine.removeRemoteControl(mRc2);
        gsdk.close();
        super.teardown();
    }

    /**
     * Checks that getting an unknown RC returns null.
     */
    @Test
    public void testUnknown() {
        assertThat(gsdk.getRemoteControl("xxx"), is(nullValue()));
    }

    /**
     * Checks that getting a RC by uid returns the correct RC
     */
    @Test
    public void testGetRc() {
        mMockEngine.addRemoteControl(mRc1);
        RemoteControl rc = gsdk.getRemoteControl("1");
        assertThat(rc, is(notNullValue()));
        assertThat(rc, allOf(hasUid("1"), isModel(RemoteControl.Model.SKY_CONTROLLER_3)));
    }

    /**
     * Checks that RC removed callback is called when the RC is removed
     */
    @Test
    public void testGetRcWithCallback() {
        int changeCnt[] = {0};
        mMockEngine.addRemoteControl(mRc1);
        mMockEngine.addRemoteControl(mRc2);

        gsdk.getRemoteControl("1", uid -> {
            assertThat(uid, is(mRc1.getUid()));
            changeCnt[0]++;
        });

        mMockEngine.removeRemoteControl(mRc2);
        // check callback has not been called
        assertThat(changeCnt[0], is(0));

        mMockEngine.removeRemoteControl(mRc1);
        // expect removed callback to be called
        assertThat(changeCnt[0], is(1));

        // check callback is not called again if the RC reappears/disappears from the store
        mMockEngine.addRemoteControl(mRc1);
        mMockEngine.removeRemoteControl(mRc1);
        assertThat(changeCnt[0], is(1));

        gsdk.getRemoteControl("2", uid -> changeCnt[0]++);

        // check callback is never called if the RC does not exist when getRemoteControl is called
        mMockEngine.addRemoteControl(mRc2);
        mMockEngine.removeRemoteControl(mRc2);
        assertThat(changeCnt[0], is(1));
    }

    /**
     * Checks calling forget on the RC is propagated to the backend
     */
    @Test
    public void testForgetRc() {
        mMockEngine.addRemoteControl(mRc1);
        assertThat(gsdk.forgetRemoteControl("1"), is(true));
        assertThat(mForgetCnt, is(1));
        assertThat(gsdk.forgetRemoteControl("2"), is(false));
        assertThat(mForgetCnt, is(1));
    }

    /**
     * Checks calling connect on the RC is propagated to the backend
     */
    @Test
    public void testConnectRc() {
        mMockEngine.addRemoteControl(mRc1);

        assertThat(gsdk.connectRemoteControl("1", DeviceConnectorCore.LOCAL_WIFI), is(true));
        assertThat(mConnectCnt, is(1));
        assertThat(mProviderUid, is(DeviceConnectorCore.LOCAL_WIFI.getUid()));
        assertThat(mPassword, nullValue());

        assertThat(gsdk.connectRemoteControl("2", DeviceConnectorCore.LOCAL_WIFI), is(false));
        assertThat(mConnectCnt, is(1));
    }

    /**
     * Checks calling disconnect on the RC is propagated to the backend
     */
    @Test
    public void testDisconnectRc() {
        mMockEngine.addRemoteControl(mRc1);

        assertThat(gsdk.disconnectRemoteControl("1"), is(true));
        assertThat(mDisconnectCnt, is(1));
        assertThat(gsdk.disconnectRemoteControl("2"), is(false));
        assertThat(mDisconnectCnt, is(1));
    }

    private class RcDelegate implements DeviceCore.Delegate {

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

