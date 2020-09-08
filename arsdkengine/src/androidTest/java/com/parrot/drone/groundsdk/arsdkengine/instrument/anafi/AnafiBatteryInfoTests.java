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

package com.parrot.drone.groundsdk.arsdkengine.instrument.anafi;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static com.parrot.drone.groundsdk.OptionalIntMatcher.optionalIntValueIs;
import static com.parrot.drone.groundsdk.OptionalValueMatcher.optionalValueIsUnavailable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiBatteryInfoTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private BatteryInfo mBatteryInfo;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mBatteryInfo = mDrone.getInstrumentStore().get(mMockSession, BatteryInfo.class);
        mDrone.getInstrumentStore().registerObserver(BatteryInfo.class, () -> {
            mBatteryInfo = mDrone.getInstrumentStore().get(mMockSession, BatteryInfo.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the drone is not connected
        assertThat(mBatteryInfo, is(nullValue()));

        connectDrone(mDrone, 1);
        assertThat(mBatteryInfo, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectDrone(mDrone, 1);
        assertThat(mBatteryInfo, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testLevelAndCharging() {
        connectDrone(mDrone, 1);

        // check default values
        assertThat(mBatteryInfo.getBatteryLevel(), is(0));
        assertThat(mBatteryInfo.isCharging(), is(false));
        assertThat(mChangeCnt, is(1));

        // check battery level and charging state
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateBatteryStateChanged(35));
        assertThat(mBatteryInfo.isCharging(), is(false));
        assertThat(mBatteryInfo.getBatteryLevel(), is(35));
        assertThat(mChangeCnt, is(2));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateBatteryStateChanged(100));
        assertThat(mBatteryInfo.isCharging(), is(false));
        assertThat(mBatteryInfo.getBatteryLevel(), is(100));
        assertThat(mChangeCnt, is(3));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateBatteryStateChanged(0));
        assertThat(mBatteryInfo.isCharging(), is(false));
        assertThat(mBatteryInfo.getBatteryLevel(), is(0));
        assertThat(mChangeCnt, is(4));
    }

    @Test
    public void testHealth() {
        connectDrone(mDrone, 1);

        // check default value
        assertThat(mBatteryInfo.getBatteryHealth(), optionalValueIsUnavailable());
        assertThat(mChangeCnt, is(1));

        // check value change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeBatteryHealth(100));
        assertThat(mBatteryInfo.getBatteryHealth(), optionalIntValueIs(100));
        assertThat(mChangeCnt, is(2));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeBatteryHealth(75));
        assertThat(mBatteryInfo.getBatteryHealth(), optionalIntValueIs(75));
        assertThat(mChangeCnt, is(3));
    }

    @Test
    public void testCycleCount() {
        connectDrone(mDrone, 1);

        // check default value
        assertThat(mBatteryInfo.getBatteryCycleCount(), optionalValueIsUnavailable());
        assertThat(mChangeCnt, is(1));

        // check value change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeBatteryCycleCount(34));
        assertThat(mBatteryInfo.getBatteryCycleCount(), optionalIntValueIs(34));
        assertThat(mChangeCnt, is(2));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeBatteryCycleCount(75));
        assertThat(mBatteryInfo.getBatteryCycleCount(), optionalIntValueIs(75));
        assertThat(mChangeCnt, is(3));
    }

    @Test
    public void testSerial() {
        connectDrone(mDrone, 1);

        // check default value
        assertThat(mBatteryInfo.getSerial(), nullValue());
        assertThat(mChangeCnt, is(1));

        // check value change
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeBatterySerial("test-serial1"));
        assertThat(mBatteryInfo.getSerial(), is("test-serial1"));
        assertThat(mChangeCnt, is(2));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeBatterySerial("test-serial2"));
        assertThat(mBatteryInfo.getSerial(), is("test-serial2"));
        assertThat(mChangeCnt, is(3));
    }
}
