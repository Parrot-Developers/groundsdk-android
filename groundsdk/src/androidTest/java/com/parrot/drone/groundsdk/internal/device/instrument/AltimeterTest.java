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

import com.parrot.drone.groundsdk.device.instrument.Altimeter;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.OptionalDoubleMatcher.optionalDoubleValueIs;
import static com.parrot.drone.groundsdk.OptionalValueMatcher.optionalValueIsUnavailable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class AltimeterTest {

    private MockComponentStore<Instrument> mStore;

    private int mChangeCnt;

    private AltimeterCore mImpl;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mChangeCnt = 0;
        mImpl = new AltimeterCore(mStore);
        mStore.registerObserver(Altimeter.class, () -> mChangeCnt++);
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
        assertThat(mImpl, is(mStore.get(Altimeter.class)));
        assertThat(mChangeCnt, is(1));

        mImpl.unpublish();
        assertThat(mStore.get(Altimeter.class), is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testNotification() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Altimeter instrument = mStore.get(Altimeter.class);
        assert instrument != null;

        // test notify without changes
        mImpl.notifyUpdated();
        assertThat(mChangeCnt, is(1));

        // test grouped change with one notify
        mImpl.updateTakeOffRelativeAltitude(1.0).updateGroundRelativeAltitude(2.0).updateVerticalSpeed(3.0)
             .notifyUpdated();
        assertThat(mChangeCnt, is(2));

        // test setting the same value does not trigger notification
        mImpl.updateTakeOffRelativeAltitude(1.0).notifyUpdated();
        mImpl.updateGroundRelativeAltitude(2.0).notifyUpdated();
        mImpl.updateVerticalSpeed(3.0).notifyUpdated();
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testTakeOffAltitude() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Altimeter instrument = mStore.get(Altimeter.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.getTakeOffRelativeAltitude(), is(0.0));

        // test value change
        mImpl.updateTakeOffRelativeAltitude(1.0).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getTakeOffRelativeAltitude(), is(1.0));
    }

    @Test
    public void testGroundAltitude() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Altimeter instrument = mStore.get(Altimeter.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.getGroundRelativeAltitude(), optionalValueIsUnavailable());

        // test value change
        mImpl.updateGroundRelativeAltitude(2.0).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getGroundRelativeAltitude(), optionalDoubleValueIs(2.0));
    }

    @Test
    public void testAbsoluteAltitude() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Altimeter instrument = mStore.get(Altimeter.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.getAbsoluteAltitude(), optionalValueIsUnavailable());

        // test value change
        mImpl.updateAbsoluteAltitude(3.0).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getAbsoluteAltitude(), optionalDoubleValueIs(3.0));
    }

    @Test
    public void testVerticalSpeed() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Altimeter instrument = mStore.get(Altimeter.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.getVerticalSpeed(), is(0.0));

        // test value change
        mImpl.updateVerticalSpeed(4.0).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getVerticalSpeed(), is(4.0));
    }
}
