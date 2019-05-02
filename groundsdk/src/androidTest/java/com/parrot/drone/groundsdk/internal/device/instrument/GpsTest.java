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

import com.parrot.drone.groundsdk.device.instrument.Gps;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.LocationMatcher.accuracyIs;
import static com.parrot.drone.groundsdk.LocationMatcher.altitudeIs;
import static com.parrot.drone.groundsdk.LocationMatcher.locationIs;
import static com.parrot.drone.groundsdk.LocationMatcher.locationIsUnavailable;
import static com.parrot.drone.groundsdk.OptionalDoubleMatcher.optionalDoubleValueIs;
import static com.parrot.drone.groundsdk.OptionalValueMatcher.optionalValueIsUnavailable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class GpsTest {

    private MockComponentStore<Instrument> mStore;

    private int mChangeCnt;

    private GpsCore mImpl;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mChangeCnt = 0;
        mImpl = new GpsCore(mStore);
        mStore.registerObserver(Gps.class, () -> mChangeCnt++);
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
        assertThat(mImpl, is(mStore.get(Gps.class)));
        assertThat(mChangeCnt, is(1));

        mImpl.unpublish();
        assertThat(mStore.get(Gps.class), is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testNotification() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Gps instrument = mStore.get(Gps.class);
        assert instrument != null;

        // test notify without changes
        mImpl.notifyUpdated();
        assertThat(mChangeCnt, is(1));

        // test grouped change with one notify
        mImpl.updateFixed(true).updateLocation(-90, 180).updateAltitude(1).updateHorizontalAccuracy(2)
             .updateVerticalAccuracy(3).updateSatelliteCount(4).notifyUpdated();
        assertThat(mChangeCnt, is(2));

        // test setting the same value does not trigger notification
        mImpl.updateFixed(true).notifyUpdated();
        mImpl.updateLocation(-90, 180).notifyUpdated();
        mImpl.updateAltitude(1).notifyUpdated();
        mImpl.updateHorizontalAccuracy(2).notifyUpdated();
        mImpl.updateVerticalAccuracy(3).notifyUpdated();
        mImpl.updateSatelliteCount(4).notifyUpdated();
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testFixed() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Gps instrument = mStore.get(Gps.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.isFixed(), is(false));

        // test value change
        mImpl.updateFixed(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.isFixed(), is(true));
    }

    @Test
    public void testLocation() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Gps instrument = mStore.get(Gps.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.lastKnownLocation(), locationIsUnavailable());

        // test value change
        mImpl.updateLocation(45, 60).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.lastKnownLocation(), locationIs(45, 60));
    }

    @Test
    public void testAltitude() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Gps instrument = mStore.get(Gps.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.lastKnownLocation(), locationIsUnavailable());

        // test altitude change, without prior lat/long info
        mImpl.updateAltitude(10).notifyUpdated();
        assertThat(mChangeCnt, is(1));
        assertThat(instrument.lastKnownLocation(), locationIsUnavailable());

        // set lat/long, check location should be published with prior altitude
        mImpl.updateLocation(45, 60).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.lastKnownLocation(), altitudeIs(10));

        // now check that altitude change is published
        mImpl.updateAltitude(20).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(instrument.lastKnownLocation(), altitudeIs(20));
    }

    @Test
    public void testHorizontalAccuracy() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Gps instrument = mStore.get(Gps.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.lastKnownLocation(), locationIsUnavailable());

        // test horizontal accuracy change, without prior lat/long info
        mImpl.updateHorizontalAccuracy(2).notifyUpdated();
        assertThat(mChangeCnt, is(1));
        assertThat(instrument.lastKnownLocation(), locationIsUnavailable());

        // set lat/long, check location should be published with prior horizontal accuracy
        mImpl.updateLocation(45, 60).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.lastKnownLocation(), accuracyIs(2));

        // now check that horizontal accuracy change is published
        mImpl.updateHorizontalAccuracy(1).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(instrument.lastKnownLocation(), accuracyIs(1));
    }

    @Test
    public void testVerticalAccuracy() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Gps instrument = mStore.get(Gps.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.getVerticalAccuracy(), optionalValueIsUnavailable());

        // test value change
        mImpl.updateVerticalAccuracy(3).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getVerticalAccuracy(), optionalDoubleValueIs(3.0));
    }

    @Test
    public void testSatelliteCount() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Gps instrument = mStore.get(Gps.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.getSatelliteCount(), is(0));

        // test value change
        mImpl.updateSatelliteCount(3).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getSatelliteCount(), is(3));
    }
}
