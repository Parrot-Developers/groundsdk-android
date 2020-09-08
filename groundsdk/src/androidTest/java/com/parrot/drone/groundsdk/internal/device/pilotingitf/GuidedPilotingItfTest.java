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

import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.guided.FinishedFlightInfoCore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.guided.GuidedPilotingItfCore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.parrot.drone.groundsdk.FinishedRelativeMoveFlightInfoMatcher.matchesFinishedRelativeMoveFlightInfo;
import static com.parrot.drone.groundsdk.LocationDirectiveMatcher.matchesLocationDirective;
import static com.parrot.drone.groundsdk.RelativeMoveDirectiveMatcher.matchesRelativeMoveDirective;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
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
        itf.moveToLocation(48.8795, 2.3675, 5,
                GuidedPilotingItf.LocationDirective.Orientation.headingDuring(90));
        assertThat(mChangeCnt, is(1));
        assertThat(mBackend.mCurrentDirective, instanceOf(GuidedPilotingItf.LocationDirective.class));
        assertThat((GuidedPilotingItf.LocationDirective) mBackend.mCurrentDirective,
                matchesLocationDirective(48.8795, 2.3675, 5,
                        GuidedPilotingItf.LocationDirective.Orientation.headingDuring(90)));

        // update current directive
        GuidedPilotingItf.LocationDirective directive = new GuidedPilotingItf.LocationDirective(
                48.0, 2.0, 10, GuidedPilotingItf.LocationDirective.Orientation.TO_TARGET, null);
        mPilotingItfImpl.updateCurrentDirective(directive).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(itf.getCurrentDirective(), instanceOf(GuidedPilotingItf.LocationDirective.class));
        assertThat((GuidedPilotingItf.LocationDirective) itf.getCurrentDirective(),
                matchesLocationDirective(48.0, 2.0, 10,
                        GuidedPilotingItf.LocationDirective.Orientation.TO_TARGET));

        // location move is finished: update current directive and latest finished flight info
        GuidedPilotingItf.FinishedLocationFlightInfo flightInfo = new FinishedFlightInfoCore.Location(directive, true);
        mPilotingItfImpl.updateCurrentDirective(null).updateLatestFinishedFlightInfo(flightInfo).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(itf.getCurrentDirective(), nullValue());
        assertThat(itf.getLatestFinishedFlightInfo(), instanceOf(GuidedPilotingItf.FinishedLocationFlightInfo.class));
        flightInfo = (GuidedPilotingItf.FinishedLocationFlightInfo) itf.getLatestFinishedFlightInfo();
        assert flightInfo != null;
        assertThat(flightInfo.getDirective(), matchesLocationDirective(48.0, 2.0, 10,
                GuidedPilotingItf.LocationDirective.Orientation.TO_TARGET));
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
        assertThat(mBackend.mCurrentDirective, instanceOf(GuidedPilotingItf.RelativeMoveDirective.class));
        assertThat((GuidedPilotingItf.RelativeMoveDirective) mBackend.mCurrentDirective,
                matchesRelativeMoveDirective(10.0, 2.5, -5.0, 45.0));

        // update current directive
        GuidedPilotingItf.RelativeMoveDirective directive = new GuidedPilotingItf.RelativeMoveDirective(
                50.0, -1.0, 0.5, 0.0, null);
        mPilotingItfImpl.updateCurrentDirective(directive).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(itf.getCurrentDirective(), instanceOf(GuidedPilotingItf.RelativeMoveDirective.class));
        assertThat((GuidedPilotingItf.RelativeMoveDirective) itf.getCurrentDirective(),
                matchesRelativeMoveDirective(50.0, -1.0, 0.5, 0.0));

        // relative move is finished: update current directive and latest finished flight info
        GuidedPilotingItf.FinishedRelativeMoveFlightInfo flightInfo =
                new FinishedFlightInfoCore.Relative(directive, true, 50.01, -0.998, 0.502, 0.0);
        mPilotingItfImpl.updateCurrentDirective(null).updateLatestFinishedFlightInfo(flightInfo).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(itf.getCurrentDirective(), nullValue());
        assertThat(itf.getLatestFinishedFlightInfo(), instanceOf(GuidedPilotingItf.FinishedRelativeMoveFlightInfo.class));
        flightInfo = (GuidedPilotingItf.FinishedRelativeMoveFlightInfo) itf.getLatestFinishedFlightInfo();
        assert flightInfo != null;
        assertThat(flightInfo.getDirective(), matchesRelativeMoveDirective(50.0, -1.0, 0.5, 0.0));
        assertThat(flightInfo, matchesFinishedRelativeMoveFlightInfo(true, 50.01, -0.998, 0.502, 0.0));
    }

    @Test
    public void testUnavailabilityReasons() {
        mPilotingItfImpl.publish();
        GuidedPilotingItf itf = mStore.get(GuidedPilotingItf.class);
        assert itf != null;

        // test initial value
        assertThat(mChangeCnt, is(1));
        assertThat(itf.getUnavailabilityReasons(), empty());

        // change unavailability reasons
        mPilotingItfImpl.updateUnavailabilityReasons(EnumSet.allOf(GuidedPilotingItf.UnavailabilityReason.class))
                        .notifyUpdated();
        assertThat(itf.getUnavailabilityReasons(),
                containsInAnyOrder(GuidedPilotingItf.UnavailabilityReason.values()));
        assertThat(mChangeCnt, is(2));

        // change unavailability reasons
        mPilotingItfImpl.updateUnavailabilityReasons(
                EnumSet.of(GuidedPilotingItf.UnavailabilityReason.DRONE_NOT_FLYING)).notifyUpdated();
        assertThat(itf.getUnavailabilityReasons(),
                containsInAnyOrder(GuidedPilotingItf.UnavailabilityReason.DRONE_NOT_FLYING));
        assertThat(mChangeCnt, is(3));

        // update to same unavailability reasons
        mPilotingItfImpl.updateUnavailabilityReasons(
                EnumSet.of(GuidedPilotingItf.UnavailabilityReason.DRONE_NOT_FLYING)).notifyUpdated();
        assertThat(itf.getUnavailabilityReasons(),
                containsInAnyOrder(GuidedPilotingItf.UnavailabilityReason.DRONE_NOT_FLYING));
        assertThat(mChangeCnt, is(3));

        // change unavailability reasons
        mPilotingItfImpl.updateUnavailabilityReasons(EnumSet.noneOf(GuidedPilotingItf.UnavailabilityReason.class))
                        .notifyUpdated();
        assertThat(itf.getUnavailabilityReasons(), empty());
        assertThat(mChangeCnt, is(4));
    }

    @Test
    public void testMove() {
        mPilotingItfImpl.publish();
        GuidedPilotingItf itf = mStore.get(GuidedPilotingItf.class);
        assert itf != null;

        // test initial value
        assertThat(mBackend.mCurrentDirective, nullValue());

        GuidedPilotingItf.Directive directive = new GuidedPilotingItf.LocationDirective(1, 2, 3,
                GuidedPilotingItf.LocationDirective.Orientation.headingStart(4),
                new GuidedPilotingItf.Directive.Speed(5, 6, 7));
        itf.move(directive);

        assertThat(mBackend.mCurrentDirective, is(directive));
    }

    private static final class Backend implements GuidedPilotingItfCore.Backend {

        @Nullable
        private GuidedPilotingItf.Directive mCurrentDirective;

        @Override
        public boolean activate() {
            return false;
        }

        @Override
        public boolean deactivate() {
            return false;
        }

        @Override
        public void move(@NonNull GuidedPilotingItf.Directive directive) {
            mCurrentDirective = directive;
        }
    }
}
