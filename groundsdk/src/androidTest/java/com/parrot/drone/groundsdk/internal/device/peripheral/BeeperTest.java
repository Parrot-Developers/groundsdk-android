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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import com.parrot.drone.groundsdk.device.peripheral.Beeper;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class BeeperTest {

    private MockComponentStore<Peripheral> mStore;

    private BeeperCore mBeeperImpl;

    private Beeper mBeeper;

    private Backend mBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mBeeperImpl = new BeeperCore(mStore, mBackend);
        mBeeper = mStore.get(Beeper.class);
        mStore.registerObserver(Beeper.class, () -> {
            mComponentChangeCnt++;
            mBeeper = mStore.get(Beeper.class);
        });
        mComponentChangeCnt = 0;
        mBackend.reset();
    }

    @Test
    public void testPublication() {
        assertThat(mBeeper, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mBeeperImpl.publish();
        assertThat(mBeeper, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mBeeperImpl.unpublish();
        assertThat(mBeeper, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testAlertSound() {
        mBeeperImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mBeeper.isAlertSoundPlaying(), is(false));
        assertThat(mBackend.mStartAlertSoundCnt, is(0));

        // start alert sound
        assertThat(mBeeper.startAlertSound(), is(true));
        assertThat(mBackend.mStartAlertSoundCnt, is(1));

        // set alert sound state to playing
        mBeeperImpl.updateAlertSoundPlaying(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBeeper.isAlertSoundPlaying(), is(true));

        // check same alert sound state does not trigger a change
        mBeeperImpl.updateAlertSoundPlaying(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));

        // start alert sound should fail if already playing
        assertThat(mBeeper.startAlertSound(), is(false));
        assertThat(mBackend.mStartAlertSoundCnt, is(1));
    }

    @Test
    public void testStopAlertSound() {
        mBeeperImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mBeeper.isAlertSoundPlaying(), is(false));
        assertThat(mBackend.mStopAlertSoundCnt, is(0));

        // set alert sound state to playing
        mBeeperImpl.updateAlertSoundPlaying(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mBeeper.isAlertSoundPlaying(), is(true));

        // stop alert sound
        assertThat(mBeeper.stopAlertSound(), is(true));
        assertThat(mBackend.mStopAlertSoundCnt, is(1));

        // set alert sound state to not playing
        mBeeperImpl.updateAlertSoundPlaying(false).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));
        assertThat(mBeeper.isAlertSoundPlaying(), is(false));

        // check same alert sound state does not trigger a change
        mBeeperImpl.updateAlertSoundPlaying(false).notifyUpdated();
        assertThat(mComponentChangeCnt, is(3));

        // stop alert sound should fail if not currently playing
        assertThat(mBeeper.stopAlertSound(), is(false));
        assertThat(mBackend.mStopAlertSoundCnt, is(1));
    }

    private static final class Backend implements BeeperCore.Backend {

        int mStartAlertSoundCnt;

        int mStopAlertSoundCnt;

        void reset() {
            mStartAlertSoundCnt = mStopAlertSoundCnt = 0;
        }

        @Override
        public boolean startAlertSound() {
            mStartAlertSoundCnt++;
            return true;
        }

        @Override
        public boolean stopAlertSound() {
            mStopAlertSoundCnt++;
            return true;
        }
    }
}
