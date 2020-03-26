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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import com.parrot.drone.groundsdk.device.peripheral.BatteryGaugeUpdater;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class BatteryGaugeUpdaterTest {

    private MockComponentStore<Peripheral> mStore;

    private BatteryGaugeUpdaterCore mBatteryGaugeUpdaterImpl;

    private BatteryGaugeUpdater mBatteryGaugeUpdater;

    private Backend mBackend;

    private int mComponentChangeCnt;

    @Before
    public void setup() {
        TestExecutor.setup();
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mBatteryGaugeUpdaterImpl = new BatteryGaugeUpdaterCore(mStore, mBackend);
        mBatteryGaugeUpdater = mStore.get(BatteryGaugeUpdater.class);
        mStore.registerObserver(BatteryGaugeUpdater.class, () -> {
            mBatteryGaugeUpdater = mStore.get(BatteryGaugeUpdater.class);
            mComponentChangeCnt++;
        });
        mComponentChangeCnt = 0;
    }

    @After
    public void teardown() {
        TestExecutor.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mBatteryGaugeUpdater, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mBatteryGaugeUpdaterImpl.publish();
        assertThat(mBatteryGaugeUpdater, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mBatteryGaugeUpdaterImpl.unpublish();
        assertThat(mBatteryGaugeUpdater, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testState() {
        mBatteryGaugeUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_PREPARE));

        // update from backend
        mBatteryGaugeUpdaterImpl.updateState(BatteryGaugeUpdater.State.PREPARING_UPDATE);
        mBatteryGaugeUpdaterImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.PREPARING_UPDATE));

        // check updating with same value does not trigger a change
        mBatteryGaugeUpdaterImpl.updateState(BatteryGaugeUpdater.State.PREPARING_UPDATE);
        mBatteryGaugeUpdaterImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.PREPARING_UPDATE));
    }

    @Test
    public void testUnavailabilityReasons() {
        mBatteryGaugeUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), empty());

        // update from backend
        mBatteryGaugeUpdaterImpl.updateUnavailabilityReasons(
                EnumSet.of(BatteryGaugeUpdater.UnavailabilityReason.DRONE_NOT_LANDED,
                        BatteryGaugeUpdater.UnavailabilityReason.NOT_USB_POWERED));
        mBatteryGaugeUpdaterImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), containsInAnyOrder(
                BatteryGaugeUpdater.UnavailabilityReason.DRONE_NOT_LANDED,
                BatteryGaugeUpdater.UnavailabilityReason.NOT_USB_POWERED));

        // new update from backend
        mBatteryGaugeUpdaterImpl.updateUnavailabilityReasons(
                EnumSet.of(BatteryGaugeUpdater.UnavailabilityReason.DRONE_NOT_LANDED,
                        BatteryGaugeUpdater.UnavailabilityReason.INSUFFICIENT_CHARGE));
        mBatteryGaugeUpdaterImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), containsInAnyOrder(
                BatteryGaugeUpdater.UnavailabilityReason.DRONE_NOT_LANDED,
                BatteryGaugeUpdater.UnavailabilityReason.INSUFFICIENT_CHARGE));

        // check updating with same values does not trigger a change
        mBatteryGaugeUpdaterImpl.updateUnavailabilityReasons(
                EnumSet.of(BatteryGaugeUpdater.UnavailabilityReason.DRONE_NOT_LANDED,
                        BatteryGaugeUpdater.UnavailabilityReason.INSUFFICIENT_CHARGE));
        mBatteryGaugeUpdaterImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), containsInAnyOrder(
                BatteryGaugeUpdater.UnavailabilityReason.DRONE_NOT_LANDED,
                BatteryGaugeUpdater.UnavailabilityReason.INSUFFICIENT_CHARGE));
    }

    @Test
    public void testCurrentProgress() {
        mBatteryGaugeUpdaterImpl.publish();

        // test initial value
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBatteryGaugeUpdater.currentProgress(), is(0));

        // update from backend
        mBatteryGaugeUpdaterImpl.updateProgress(33);
        mBatteryGaugeUpdaterImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBatteryGaugeUpdater.currentProgress(), is(33));

        // check updating with same value does not trigger a change
        mBatteryGaugeUpdaterImpl.updateProgress(33);
        mBatteryGaugeUpdaterImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBatteryGaugeUpdater.currentProgress(), is(33));
    }

    @Test
    public void testPrepareUpdateProcess() {
        mBatteryGaugeUpdaterImpl.publish();
        mBatteryGaugeUpdaterImpl.updateState(BatteryGaugeUpdater.State.READY_TO_UPDATE);

        // test initial values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_UPDATE));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), empty());

        // check that update could not be prepared because drone is not ready to prepare
        assertThat(mBatteryGaugeUpdater.prepareUpdate(), is(false));
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mPrepareUpdateCalls, is(0));

        // change state and unavailability reasons
        mBatteryGaugeUpdaterImpl.updateState(BatteryGaugeUpdater.State.READY_TO_PREPARE);
        mBatteryGaugeUpdaterImpl.updateUnavailabilityReasons(
                EnumSet.of(BatteryGaugeUpdater.UnavailabilityReason.NOT_USB_POWERED));
        mBatteryGaugeUpdaterImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_PREPARE));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(),
                containsInAnyOrder(BatteryGaugeUpdater.UnavailabilityReason.NOT_USB_POWERED));

        // check that update could not be prepared because drone is not USB powered
        assertThat(mBatteryGaugeUpdater.prepareUpdate(), is(false));
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mPrepareUpdateCalls, is(0));

        // clear unavailability reasons
        mBatteryGaugeUpdaterImpl.updateUnavailabilityReasons(
                EnumSet.noneOf(BatteryGaugeUpdater.UnavailabilityReason.class));
        mBatteryGaugeUpdaterImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_PREPARE));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), empty());

        // prepare update
        assertThat(mBatteryGaugeUpdater.prepareUpdate(), is(true));
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mPrepareUpdateCalls, is(1));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_PREPARE));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), empty());
   }

    @Test
    public void testUpdateProcess() {
        mBatteryGaugeUpdaterImpl.publish();

        // test initial values
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_PREPARE));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), empty());
        assertThat(mBatteryGaugeUpdater.currentProgress(), is(0));

        // check that update could not be prepared because drone is not ready to update
        assertThat(mBatteryGaugeUpdater.update(), is(false));
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mBackend.mUpdateCalls, is(0));

        // update state and unavailability reasons
        mBatteryGaugeUpdaterImpl.updateState(BatteryGaugeUpdater.State.READY_TO_UPDATE);
        mBatteryGaugeUpdaterImpl.updateUnavailabilityReasons(EnumSet.of(
                BatteryGaugeUpdater.UnavailabilityReason.INSUFFICIENT_CHARGE));
        mBatteryGaugeUpdaterImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_UPDATE));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(),
                containsInAnyOrder(BatteryGaugeUpdater.UnavailabilityReason.INSUFFICIENT_CHARGE));

        // check that update could not be prepared because battery charge is insufficient
        assertThat(mBatteryGaugeUpdater.update(), is(false));
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBackend.mUpdateCalls, is(0));

        // clear unavailability reasons
        mBatteryGaugeUpdaterImpl.updateUnavailabilityReasons(EnumSet.noneOf(
                BatteryGaugeUpdater.UnavailabilityReason.class));
        mBatteryGaugeUpdaterImpl.notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_UPDATE));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), empty());

        // update
        assertThat(mBatteryGaugeUpdater.update(), is(true));
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBackend.mUpdateCalls, is(1));
        assertThat(mBatteryGaugeUpdater.state(), is(BatteryGaugeUpdater.State.READY_TO_UPDATE));
        assertThat(mBatteryGaugeUpdater.unavailabilityReasons(), empty());
    }

    private static final class Backend implements BatteryGaugeUpdaterCore.Backend {

        private int mPrepareUpdateCalls;

        private int mUpdateCalls;

        @Override
        public void prepareUpdate() {
            mPrepareUpdateCalls++;
        }

        @Override
        public void update() {
            mUpdateCalls++;
        }
    }
}
