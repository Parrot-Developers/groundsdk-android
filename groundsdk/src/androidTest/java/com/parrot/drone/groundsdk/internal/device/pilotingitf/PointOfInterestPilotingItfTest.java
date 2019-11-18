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

package com.parrot.drone.groundsdk.internal.device.pilotingitf;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PointOfInterestPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PointOfInterestPilotingItf.PointOfInterest;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.PointOfInterestPilotingItfCore.PointOfInterestCore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.PointOfInterestMatcher.matchesDirective;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class PointOfInterestPilotingItfTest {

    private MockComponentStore<PilotingItf> mStore;

    private int mChangeCnt;

    private PointOfInterestPilotingItfCore mPilotingItfImpl;

    private Backend mBackend;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mChangeCnt = 0;
        mStore.registerObserver(PointOfInterestPilotingItf.class, () -> mChangeCnt++);
        mBackend = new Backend();
        mPilotingItfImpl = new PointOfInterestPilotingItfCore(mStore, mBackend);
    }

    @After
    public void teardown() {
        mPilotingItfImpl = null;
        mBackend = null;
        mStore = null;
    }

    @Test
    public void testPublication() {
        mPilotingItfImpl.publish();
        assertThat(mPilotingItfImpl, is(mStore.get(PointOfInterestPilotingItf.class)));
        mPilotingItfImpl.unpublish();
        assertThat(mStore.get(PointOfInterestPilotingItf.class), nullValue());
    }

    @Test
    public void testPilotedPOI() {
        mPilotingItfImpl.publish();
        PointOfInterestPilotingItf itf = mStore.get(PointOfInterestPilotingItf.class);
        assert itf != null;

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(itf.getCurrentPointOfInterest(), nullValue());

        // start piloted Point Of Interest
        itf.start(48.8795, 2.3675, 5);
        assertThat(mChangeCnt, is(1));
        assertThat(mBackend.mCurrentPointOfInterest, matchesDirective(48.8795, 2.3675, 5,
                PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL));

        // update current Point Of Interest
        PointOfInterestCore poi = new PointOfInterestCore(48.0, 2.0, 10,
                PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL);
        mPilotingItfImpl.updateCurrentPointOfInterest(poi).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(itf.getCurrentPointOfInterest(), matchesDirective(48.0, 2.0, 10,
                PointOfInterestPilotingItf.Mode.LOCKED_GIMBAL));

        // start piloted Point Of Interest with free gimbal
        itf.start(50, 10, 30, PointOfInterestPilotingItf.Mode.FREE_GIMBAL);
        assertThat(mChangeCnt, is(2));
        assertThat(mBackend.mCurrentPointOfInterest, matchesDirective(50, 10, 30,
                PointOfInterestPilotingItf.Mode.FREE_GIMBAL));

        // update current Point Of Interest
        poi = new PointOfInterestCore(50.1, 10.2, 30.3, PointOfInterestPilotingItf.Mode.FREE_GIMBAL);
        mPilotingItfImpl.updateCurrentPointOfInterest(poi).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(itf.getCurrentPointOfInterest(), matchesDirective(50.1, 10.2, 30.3,
                PointOfInterestPilotingItf.Mode.FREE_GIMBAL));
    }

    @Test
    public void testPilotingCommand() {
        mPilotingItfImpl.publish();

        PointOfInterestPilotingItf itf = mStore.get(PointOfInterestPilotingItf.class);
        assert itf != null;
        assertThat(mChangeCnt, is(1));

        // test initial values
        assertThat(mBackend.mPitch, is(0));
        assertThat(mBackend.mRoll, is(0));
        assertThat(mBackend.mVerticalSpeed, is(0));

        // test set values
        itf.setPitch(2);
        itf.setRoll(3);
        itf.setVerticalSpeed(4);

        assertThat(mBackend.mPitch, is(2));
        assertThat(mBackend.mRoll, is(3));
        assertThat(mBackend.mVerticalSpeed, is(4));

        // check upper bounds
        itf.setPitch(102);
        itf.setRoll(103);
        itf.setVerticalSpeed(104);

        assertThat(mBackend.mPitch, is(100));
        assertThat(mBackend.mRoll, is(100));
        assertThat(mBackend.mVerticalSpeed, is(100));

        // check lower bounds
        itf.setPitch(-102);
        itf.setRoll(-103);
        itf.setVerticalSpeed(-104);

        assertThat(mBackend.mPitch, is(-100));
        assertThat(mBackend.mRoll, is(-100));
        assertThat(mBackend.mVerticalSpeed, is(-100));
    }

    private static final class Backend implements PointOfInterestPilotingItfCore.Backend {

        @Nullable
        private PointOfInterest mCurrentPointOfInterest;

        int mPitch, mRoll, mVerticalSpeed;

        @Override
        public boolean activate() {
            return false;
        }

        @Override
        public boolean deactivate() {
            return false;
        }

        @Override
        public void start(double latitude, double longitude, double altitude,
                          @NonNull PointOfInterestPilotingItf.Mode mode) {
            mCurrentPointOfInterest = new PointOfInterestCore(latitude, longitude, altitude, mode);
        }

        @Override
        public void setPitch(int pitch) {
            mPitch = pitch;
        }

        @Override
        public void setRoll(int roll) {
            mRoll = roll;
        }

        @Override
        public void setVerticalSpeed(int verticalSpeed) {
            mVerticalSpeed = verticalSpeed;
        }
    }
}
