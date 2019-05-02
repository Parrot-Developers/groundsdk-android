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

import com.parrot.drone.groundsdk.device.instrument.Alarms;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests the alarms instrument.
 */
public class AlarmsTest {

    private MockComponentStore<Instrument> mStore;

    private AlarmsCore mAlarmsImpl;

    private Alarms mAlarms;

    private int mChangeCnt;

    private static final Alarms.Alarm.Kind TEST_ALARM = Alarms.Alarm.Kind.values()[0];

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mAlarmsImpl = new AlarmsCore(mStore);
        mAlarms = mStore.get(Alarms.class);
        mStore.registerObserver(Alarms.class, () -> {
            mChangeCnt++;
            mAlarms = mStore.get(Alarms.class);
        });
        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        assertThat(mAlarms, nullValue());
        assertThat(mChangeCnt, is(0));

        mAlarmsImpl.publish();

        assertThat(mAlarms, is(mAlarmsImpl));
        assertThat(mChangeCnt, is(1));

        mAlarmsImpl.unpublish();

        assertThat(mAlarms, nullValue());
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testAlarm() {
        mAlarmsImpl.publish();

        assertThat(mChangeCnt, is(1));
        for (Alarms.Alarm.Kind kind : Alarms.Alarm.Kind.values()) {
            Alarms.Alarm alarm = mAlarms.getAlarm(kind);
            assertThat(alarm.getKind(), is(kind));
            assertThat(alarm.getLevel(), is(Alarms.Alarm.Level.NOT_SUPPORTED));
        }
    }

    @Test
    public void testAlarmLevel() {
        mAlarmsImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mAlarms.getAlarm(TEST_ALARM).getLevel(), is(Alarms.Alarm.Level.NOT_SUPPORTED));

        // mock update from low-level
        mAlarmsImpl.updateAlarmLevel(TEST_ALARM, Alarms.Alarm.Level.OFF).notifyUpdated();

        // test value updates
        assertThat(mChangeCnt, is(2));
        assertThat(mAlarms.getAlarm(TEST_ALARM).getLevel(), is(Alarms.Alarm.Level.OFF));

        // mock same update from low-level
        mAlarmsImpl.updateAlarmLevel(TEST_ALARM, Alarms.Alarm.Level.OFF).notifyUpdated();

        // test nothing changes
        assertThat(mChangeCnt, is(2));
        assertThat(mAlarms.getAlarm(TEST_ALARM).getLevel(), is(Alarms.Alarm.Level.OFF));

        // mock update from low level
        mAlarmsImpl.updateAlarmLevel(TEST_ALARM, Alarms.Alarm.Level.WARNING);

        // test no change before notification
        assertThat(mChangeCnt, is(2));
        // however the value should be correct
        assertThat(mAlarms.getAlarm(TEST_ALARM).getLevel(), is(Alarms.Alarm.Level.WARNING));

        // mock second update from low level
        mAlarmsImpl.updateAlarmLevel(TEST_ALARM, Alarms.Alarm.Level.CRITICAL);

        // test no change before notification
        assertThat(mChangeCnt, is(2));

        // notify
        mAlarmsImpl.notifyUpdated();

        // test value updates to latest and only one change is notified
        assertThat(mChangeCnt, is(3));
        assertThat(mAlarms.getAlarm(TEST_ALARM).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
    }

    @Test
    public void testAutomaticLandingDelay() {
        mAlarmsImpl.publish();

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mAlarms.automaticLandingDelay(), is(0));

        // mock update from low-level
        mAlarmsImpl.updateAutoLandingDelay(60).notifyUpdated();

        // test value updates
        assertThat(mChangeCnt, is(2));
        assertThat(mAlarms.automaticLandingDelay(), is(60));

        // mock same update from low-level
        mAlarmsImpl.updateAutoLandingDelay(60).notifyUpdated();

        // test nothing changes
        assertThat(mChangeCnt, is(2));
        assertThat(mAlarms.automaticLandingDelay(), is(60));

        // mock update from low level
        mAlarmsImpl.updateAutoLandingDelay(5);

        // test no change before notification
        assertThat(mChangeCnt, is(2));
        // however the value should be correct
        assertThat(mAlarms.automaticLandingDelay(), is(5));

        // mock second update from low level
        mAlarmsImpl.updateAutoLandingDelay(3);

        // test no change before notification
        assertThat(mChangeCnt, is(2));

        // notify
        mAlarmsImpl.notifyUpdated();

        // test value updates to latest and only one change is notified
        assertThat(mChangeCnt, is(3));
        assertThat(mAlarms.automaticLandingDelay(), is(3));
    }
}
