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

package com.parrot.drone.groundsdk.internal.device.instrument;

import com.parrot.drone.groundsdk.device.instrument.BatteryInfo;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.OptionalIntMatcher.optionalIntValueIs;
import static com.parrot.drone.groundsdk.OptionalValueMatcher.optionalValueIsUnavailable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class BatteryInfoTest {

    private MockComponentStore<Instrument> mStore;

    private int mChangeCnt;

    private BatteryInfoCore mImpl;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mChangeCnt = 0;
        mImpl = new BatteryInfoCore(mStore);
        mStore.registerObserver(BatteryInfo.class, () -> mChangeCnt++);
    }

    @After
    public void tearDown() {
        mStore.destroy();
        mStore = null;
        mImpl = null;
    }

    /**
     * Test that publishing the component will add it to the store and
     * unpublishing it will remove it.
     */
    @Test
    public void testPublication() {
        mImpl.publish();
        assertThat(mImpl, is(mStore.get(BatteryInfo.class)));
        assertThat(mChangeCnt, is(1));

        mImpl.unpublish();
        assertThat(mStore.get(BatteryInfo.class), is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testNotification() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        // test notify without changes
        mImpl.notifyUpdated();
        assertThat(mChangeCnt, is(1));

        // test grouped change with one notify
        mImpl.updateLevel(30).updateLevel(60).updateCharging(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));

        // test setting the same value does not trigger notification
        mImpl.updateLevel(60).updateCharging(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testLevel() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        BatteryInfo instrument = mStore.get(BatteryInfo.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.getBatteryLevel(), is(0));

        // test value change
        mImpl.updateLevel(45).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getBatteryLevel(), is(45));
    }

    @Test
    public void testCharging() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        BatteryInfo instrument = mStore.get(BatteryInfo.class);
        assert instrument != null;

        // default values
        assertThat(instrument.isCharging(), is(false));

        // update charging to true
        mImpl.updateCharging(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.isCharging(), is(true));

        // update charging to same value
        mImpl.updateCharging(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.isCharging(), is(true));

        // update charging to false
        mImpl.updateCharging(false).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(instrument.isCharging(), is(false));
    }

    @Test
    public void testHealth() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        BatteryInfo instrument = mStore.get(BatteryInfo.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.getBatteryHealth(), optionalValueIsUnavailable());

        // test value change
        mImpl.updateHealth(95).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getBatteryHealth(), optionalIntValueIs(95));
    }

    @Test
    public void testCycleCount() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        BatteryInfo instrument = mStore.get(BatteryInfo.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.getBatteryCycleCount(), optionalValueIsUnavailable());

        // test value change
        mImpl.updateCycleCount(13).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getBatteryCycleCount(), optionalIntValueIs(13));
    }

    @Test
    public void testSerial() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        BatteryInfo instrument = mStore.get(BatteryInfo.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.getSerial(), nullValue());

        // test value change
        mImpl.updateSerial("test-serial").notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getSerial(), is("test-serial"));
    }
}
