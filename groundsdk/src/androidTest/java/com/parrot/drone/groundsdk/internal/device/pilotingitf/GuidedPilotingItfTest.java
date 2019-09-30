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

import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.Directive;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.FinishedLocationFlightInfo;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.FinishedRelativeMoveFlightInfo;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.LocationDirective;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.LocationDirective.Orientation;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.RelativeMoveDirective;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.GuidedPilotingItfCore.FinishedLocationFlightInfoCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.GuidedPilotingItfCore.FinishedRelativeMoveFlightInfoCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.GuidedPilotingItfCore.LocationDirectiveCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.GuidedPilotingItfCore.RelativeMoveDirectiveCore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.FinishedRelativeMoveFlightInfoMatcher.matchesFinishedRelativeMoveFlightInfo;
import static com.parrot.drone.groundsdk.LocationDirectiveMatcher.matchesLocationDirective;
import static com.parrot.drone.groundsdk.RelativeMoveDirectiveMatcher.matchesRelativeMoveDirective;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class GuidedPilotingItfTest {

    private MockComponentStore<PilotingItf> mStore;

    private int mChangeCnt;

    private GuidedPilotingItfCore mPilotingItfImpl;

    private Backend mBackend;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mChangeCnt = 0;
        mStore.registerObserver(GuidedPilotingItf.class, () -> mChangeCnt++);
        mBackend = new Backend();
        mPilotingItfImpl = new GuidedPilotingItfCore(mStore, mBackend);
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
        assertThat(mPilotingItfImpl, is(mStore.get(GuidedPilotingItf.class)));
        mPilotingItfImpl.unpublish();
        assertThat(mStore.get(GuidedPilotingItf.class), nullValue());
    }

    @Test
    public void testLocationMove() {
        mPilotingItfImpl.publish();
        GuidedPilotingItf itf = mStore.get(GuidedPilotingItf.class);
        assert itf != null;

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(itf.getCurrentDirective(), nullValue());

        // start location move
        itf.moveToLocation(48.8795, 2.3675, 5, Orientation.headingDuring(90));
        assertThat(mChangeCnt, is(1));
        assertThat(mBackend.mCurrentDirective, instanceOf(LocationDirective.class));
        assertThat((LocationDirective) mBackend.mCurrentDirective,
                matchesLocationDirective(48.8795, 2.3675, 5, Orientation.headingDuring(90)));

        // update current directive
        LocationDirectiveCore directive = new LocationDirectiveCore(48.0, 2.0, 10, Orientation.TO_TARGET);
        mPilotingItfImpl.updateCurrentDirective(directive).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(itf.getCurrentDirective(), instanceOf(LocationDirective.class));
        assertThat((LocationDirective) itf.getCurrentDirective(),
                matchesLocationDirective(48.0, 2.0, 10, Orientation.TO_TARGET));

        // location move is finished: update current directive and latest finished flight info
        FinishedLocationFlightInfo flightInfo = new FinishedLocationFlightInfoCore(directive, true);
        mPilotingItfImpl.updateCurrentDirective(null).updateLatestFinishedFlightInfo(flightInfo).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(itf.getCurrentDirective(), nullValue());
        assertThat(itf.getLatestFinishedFlightInfo(), instanceOf(FinishedLocationFlightInfo.class));
        flightInfo = (FinishedLocationFlightInfo) itf.getLatestFinishedFlightInfo();
        assert flightInfo != null;
        assertThat(flightInfo.getDirective(), matchesLocationDirective(48.0, 2.0, 10, Orientation.TO_TARGET));
        assertThat(flightInfo.wasSuccessful(), is(true));
    }

    @Test
    public void testRelativeMove() {
        mPilotingItfImpl.publish();
        GuidedPilotingItf itf = mStore.get(GuidedPilotingItf.class);
        assert itf != null;

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(itf.getCurrentDirective(), nullValue());

        // start relative move
        itf.moveToRelativePosition(10.0, 2.5, -5.0, 45.0);
        assertThat(mChangeCnt, is(1));
        assertThat(mBackend.mCurrentDirective, instanceOf(RelativeMoveDirective.class));
        assertThat((RelativeMoveDirective) mBackend.mCurrentDirective,
                matchesRelativeMoveDirective(10.0, 2.5, -5.0, 45.0));

        // update current directive
        RelativeMoveDirective directive = new RelativeMoveDirectiveCore(50.0, -1.0, 0.5, 0.0);
        mPilotingItfImpl.updateCurrentDirective(directive).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(itf.getCurrentDirective(), instanceOf(RelativeMoveDirective.class));
        assertThat((RelativeMoveDirective) itf.getCurrentDirective(),
                matchesRelativeMoveDirective(50.0, -1.0, 0.5, 0.0));

        // relative move is finished: update current directive and latest finished flight info
        FinishedRelativeMoveFlightInfo flightInfo =
                new FinishedRelativeMoveFlightInfoCore(directive, true, 50.01, -0.998, 0.502, 0.0);
        mPilotingItfImpl.updateCurrentDirective(null).updateLatestFinishedFlightInfo(flightInfo).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(itf.getCurrentDirective(), nullValue());
        assertThat(itf.getLatestFinishedFlightInfo(), instanceOf(FinishedRelativeMoveFlightInfo.class));
        flightInfo = (FinishedRelativeMoveFlightInfo) itf.getLatestFinishedFlightInfo();
        assert flightInfo != null;
        assertThat(flightInfo.getDirective(), matchesRelativeMoveDirective(50.0, -1.0, 0.5, 0.0));
        assertThat(flightInfo, matchesFinishedRelativeMoveFlightInfo(true, 50.01, -0.998, 0.502, 0.0));
    }

    private static final class Backend implements GuidedPilotingItfCore.Backend {

        @Nullable
        private Directive mCurrentDirective;

        @Override
        public boolean activate() {
            return false;
        }

        @Override
        public boolean deactivate() {
            return false;
        }

        @Override
        public void moveToLocation(double latitude, double longitude, double altitude,
                                   @NonNull Orientation orientation) {
            mCurrentDirective = new LocationDirectiveCore(latitude, longitude, altitude, orientation);
        }

        @Override
        public void moveToRelativePosition(double dx, double dy, double dz, double dpsi) {
            mCurrentDirective = new RelativeMoveDirectiveCore(dx, dy, dz, dpsi);
        }
    }
}
