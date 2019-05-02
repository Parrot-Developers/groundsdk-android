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

package com.parrot.drone.groundsdk.arsdkengine.instrument.skycontroller;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class SkyControllerBatteryInfoTests extends ArsdkEngineTestBase {

    private RemoteControlCore mRc;

    private BatteryInfo mBatteryInfo;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", RemoteControl.Model.SKY_CONTROLLER_3.id(), "Rc1", 1, Backend.TYPE_MUX);
        mRc = mRCStore.get("123");
        assert mRc != null;

        mBatteryInfo = mRc.getInstrumentStore().get(mMockSession, BatteryInfo.class);
        mRc.getInstrumentStore().registerObserver(BatteryInfo.class, () -> {
            mBatteryInfo = mRc.getInstrumentStore().get(mMockSession, BatteryInfo.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the drone is not connected
        assertThat(mBatteryInfo, is(nullValue()));

        connectRemoteControl(mRc, 1);
        assertThat(mBatteryInfo, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectRemoteControl(mRc, 1);
        assertThat(mBatteryInfo, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testLevelAndCharging() {
        connectRemoteControl(mRc, 1);

        // check default values
        assertThat(mBatteryInfo.getBatteryLevel(), is(0));
        assertThat(mBatteryInfo.isCharging(), is(false));
        assertThat(mChangeCnt, is(1));

        // check battery level and charging state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSkyControllerStateBatteryChanged(35));
        assertThat(mBatteryInfo.isCharging(), is(false));
        assertThat(mBatteryInfo.getBatteryLevel(), is(35));
        assertThat(mChangeCnt, is(2));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSkyControllerStateBatteryChanged(100));
        assertThat(mBatteryInfo.isCharging(), is(false));
        assertThat(mBatteryInfo.getBatteryLevel(), is(100));
        assertThat(mChangeCnt, is(3));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSkyControllerStateBatteryChanged(0));
        assertThat(mBatteryInfo.isCharging(), is(false));
        assertThat(mBatteryInfo.getBatteryLevel(), is(0));
        assertThat(mChangeCnt, is(4));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSkyControllerStateBatteryChanged(255));
        assertThat(mBatteryInfo.isCharging(), is(true));
        assertThat(mBatteryInfo.getBatteryLevel(), is(100));
        assertThat(mChangeCnt, is(5));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSkyControllerStateBatteryChanged(0));
        assertThat(mBatteryInfo.isCharging(), is(false));
        assertThat(mBatteryInfo.getBatteryLevel(), is(0));
        assertThat(mChangeCnt, is(6));
    }
}
