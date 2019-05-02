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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Beeper;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiBeeperTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private Beeper mBeeper;

    private int mBeeperChangeCnt;

    @Override
    public void setUp() {
        super.setUp();

        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mBeeper = mDrone.getPeripheralStore().get(mMockSession, Beeper.class);
        mDrone.getPeripheralStore().registerObserver(Beeper.class, () -> {
            mBeeper = mDrone.getPeripheralStore().get(mMockSession, Beeper.class);
            mBeeperChangeCnt++;
        });

        mBeeperChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mBeeper, nullValue());
        assertThat(mBeeperChangeCnt, is(0));

        connectDrone(mDrone, 1);

        assertThat(mBeeper, notNullValue());
        assertThat(mBeeperChangeCnt, is(1));

        disconnectDrone(mDrone, 1);

        assertThat(mBeeper, nullValue());
        assertThat(mBeeperChangeCnt, is(2));
    }

    @Test
    public void testStartAlertSound() {
        connectDrone(mDrone, 1);

        assertThat(mBeeperChangeCnt, is(1));
        assertThat(mBeeper.isAlertSoundPlaying(), is(false));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3SoundStartAlertSound(), false));
        assertThat(mBeeper.startAlertSound(), is(true));
        assertThat(mBeeperChangeCnt, is(1));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SoundStateAlertSound(
                ArsdkFeatureArdrone3.SoundstateAlertsoundState.PLAYING));
        assertThat(mBeeperChangeCnt, is(2));
        assertThat(mBeeper.isAlertSoundPlaying(), is(true));

        // start alert sound should fail if already playing
        assertThat(mBeeper.startAlertSound(), is(false));
        assertThat(mBeeperChangeCnt, is(2));
    }

    @Test
    public void testStopAlertSound() {
        connectDrone(mDrone, 1);

        assertThat(mBeeperChangeCnt, is(1));
        assertThat(mBeeper.isAlertSoundPlaying(), is(false));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SoundStateAlertSound(
                ArsdkFeatureArdrone3.SoundstateAlertsoundState.PLAYING));
        assertThat(mBeeperChangeCnt, is(2));
        assertThat(mBeeper.isAlertSoundPlaying(), is(true));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ardrone3SoundStopAlertSound(), false));
        assertThat(mBeeper.stopAlertSound(), is(true));
        assertThat(mBeeperChangeCnt, is(2));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SoundStateAlertSound(
                ArsdkFeatureArdrone3.SoundstateAlertsoundState.STOPPED));
        assertThat(mBeeperChangeCnt, is(3));
        assertThat(mBeeper.isAlertSoundPlaying(), is(false));

        // stop alert sound should fail if not currently playing
        assertThat(mBeeper.stopAlertSound(), is(false));
        assertThat(mBeeperChangeCnt, is(3));
    }
}
