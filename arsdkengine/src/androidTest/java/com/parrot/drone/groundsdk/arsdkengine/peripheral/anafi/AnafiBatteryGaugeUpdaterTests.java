/*
 *     Copyright (C) 2020 Parrot Drones SAS
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
import com.parrot.drone.groundsdk.device.peripheral.BatteryGaugeUpdater;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGaugeFwUpdater;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiBatteryGaugeUpdaterTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private BatteryGaugeUpdater mBatteryGaugeUpdater;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();

        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mBatteryGaugeUpdater = mDrone.getPeripheralStore().get(mMockSession, BatteryGaugeUpdater.class);
        mDrone.getPeripheralStore().registerObserver(BatteryGaugeUpdater.class, () -> {
            mBatteryGaugeUpdater = mDrone.getPeripheralStore().get(mMockSession, BatteryGaugeUpdater.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mBatteryGaugeUpdater, nullValue());
        assertThat(mChangeCnt, is(0));

        connectDrone(mDrone, 1);

        // not published if no updatable diagnostic is received
        assertThat(mBatteryGaugeUpdater, nullValue());
        assertThat(mChangeCnt, is(0));

        disconnectDrone(mDrone, 1);

        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.READY_TO_PREPARE)));

        // now published as updatable diagnostic has been received during connection
        assertThat(mBatteryGaugeUpdater, notNullValue());
        assertThat(mChangeCnt, is(1));

        disconnectDrone(mDrone, 1);

        assertThat(mBatteryGaugeUpdater, nullValue());
        assertThat(mChangeCnt, is(2));

        connectDrone(mDrone, 1);

        // check that updatable status has been reset
        assertThat(mBatteryGaugeUpdater, nullValue());
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testState() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.READY_TO_PREPARE)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_PREPARE));

        // change to preparing update from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.PREPARATION_IN_PROGRESS));
        assertThat(mChangeCnt, is(2));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.PREPARING_UPDATE));

        // change to ready to update from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.READY_TO_UPDATE));
        assertThat(mChangeCnt, is(3));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_UPDATE));

        // change to updating from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.UPDATE_IN_PROGRESS));
        assertThat(mChangeCnt, is(4));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.UPDATING));

        // received error from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterProgress(ArsdkFeatureGaugeFwUpdater.Result.BATTERY_ERROR, 0));
        assertThat(mChangeCnt, is(5));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.ERROR));
    }

    @Test
    public void testUnavailabilityReasons() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.READY_TO_PREPARE)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), empty());

        // change value from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE,
                        ArsdkFeatureGaugeFwUpdater.Requirements.toBitField(
                                ArsdkFeatureGaugeFwUpdater.Requirements.USB,
                                ArsdkFeatureGaugeFwUpdater.Requirements.DRONE_STATE),
                        ArsdkFeatureGaugeFwUpdater.State.READY_TO_PREPARE));
        assertThat(mChangeCnt, is(2));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), containsInAnyOrder(
                BatteryGaugeUpdater.UnavailabilityReason.NOT_USB_POWERED,
                BatteryGaugeUpdater.UnavailabilityReason.DRONE_NOT_LANDED));

        // change again value from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE,
                        ArsdkFeatureGaugeFwUpdater.Requirements.toBitField(
                                ArsdkFeatureGaugeFwUpdater.Requirements.RSOC),
                        ArsdkFeatureGaugeFwUpdater.State.READY_TO_PREPARE));
        assertThat(mChangeCnt, is(3));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), containsInAnyOrder(
                BatteryGaugeUpdater.UnavailabilityReason.INSUFFICIENT_CHARGE));
    }

    @Test
    public void testCurrentProgress() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.READY_TO_PREPARE)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mBatteryGaugeUpdater.currentProgress(), is(0));

        // change value from backend, current progress does not change because state is not preparing update
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterProgress(ArsdkFeatureGaugeFwUpdater.Result.IN_PROGRESS, 33));
        assertThat(mChangeCnt, is(1));
        assertThat(mBatteryGaugeUpdater.currentProgress(), is(0));

        // change state to preparing update and change progress from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.PREPARATION_IN_PROGRESS),
                ArsdkEncoder.encodeGaugeFwUpdaterProgress(ArsdkFeatureGaugeFwUpdater.Result.IN_PROGRESS, 66));
        assertThat(mChangeCnt, is(3));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.PREPARING_UPDATE));
        assertThat(mBatteryGaugeUpdater.currentProgress(), is(66));

        // check that current progress is reset when state changes to ready to prepare
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.READY_TO_PREPARE));
        assertThat(mChangeCnt, is(4));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_PREPARE));
        assertThat(mBatteryGaugeUpdater.currentProgress(), is(0));

        // change state to preparing update and change progress from backend
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.PREPARATION_IN_PROGRESS),
                ArsdkEncoder.encodeGaugeFwUpdaterProgress(ArsdkFeatureGaugeFwUpdater.Result.IN_PROGRESS, 99));
        assertThat(mChangeCnt, is(6));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.PREPARING_UPDATE));
        assertThat(mBatteryGaugeUpdater.currentProgress(), is(99));

        // check that current progress is reset when state changes to ready to update
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.READY_TO_UPDATE));
        assertThat(mChangeCnt, is(7));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_UPDATE));
        assertThat(mBatteryGaugeUpdater.currentProgress(), is(0));
    }

    @Test
    public void testPrepareUpdateProcess() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.READY_TO_PREPARE)));

        // drone is ready to prepare update
        assertThat(mChangeCnt, is(1));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_PREPARE));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), empty());

        // start prepare update process
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.gaugeFwUpdaterPrepare()));
        mBatteryGaugeUpdater.prepareUpdate();
        assertThat(mChangeCnt, is(1));

        // mock state received from backend
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGaugeFwUpdaterStatus(
                ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                ArsdkFeatureGaugeFwUpdater.State.PREPARATION_IN_PROGRESS));
        assertThat(mChangeCnt, is(2));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.PREPARING_UPDATE));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), empty());
    }

    @Test
    public void testUpdateProcess() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeGaugeFwUpdaterStatus(ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                        ArsdkFeatureGaugeFwUpdater.State.READY_TO_UPDATE)));

        // drone is ready to prepare update
        assertThat(mChangeCnt, is(1));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_UPDATE));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), empty());

        // start update process
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.gaugeFwUpdaterUpdate()));
        mBatteryGaugeUpdater.update();
        assertThat(mChangeCnt, is(1));

        // mock state received from backend
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeGaugeFwUpdaterStatus(
                ArsdkFeatureGaugeFwUpdater.Diag.UPDATABLE, 0,
                ArsdkFeatureGaugeFwUpdater.State.UPDATE_IN_PROGRESS));
        assertThat(mChangeCnt, is(2));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.UPDATING));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), empty());
    }
}
