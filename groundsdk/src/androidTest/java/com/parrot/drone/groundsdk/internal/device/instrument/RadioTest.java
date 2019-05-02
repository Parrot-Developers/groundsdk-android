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

import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.device.instrument.Radio;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class RadioTest {

    private MockComponentStore<Instrument> mStore;

    private int mChangeCnt;

    private RadioCore mImpl;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mChangeCnt = 0;
        mImpl = new RadioCore(mStore);
        mStore.registerObserver(Radio.class, () -> mChangeCnt++);
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
        assertThat(mImpl, is(mStore.get(Radio.class)));
        assertThat(mChangeCnt, is(1));

        mImpl.unpublish();
        assertThat(mStore.get(Radio.class), is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testNotification() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Radio instrument = mStore.get(Radio.class);
        assert instrument != null;

        // test notify without changes
        mImpl.notifyUpdated();
        assertThat(mChangeCnt, is(1));

        // test grouped change with one notify
        mImpl.updateRssi(-30).updateRssi(-60).notifyUpdated();
        assertThat(mChangeCnt, is(2));

        // test setting the same value does not trigger notification
        mImpl.updateRssi(-60).notifyUpdated();
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testRssi() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Radio instrument = mStore.get(Radio.class);
        assert instrument != null;

        // test default values
        assertThat(instrument.getRssi(), is(0));

        // test value change
        mImpl.updateRssi(-45).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getRssi(), is(-45));
    }

    @Test
    public void testLinkSignalQuality() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Radio instrument = mStore.get(Radio.class);
        assert instrument != null;

        // test default value
        assertThat(instrument.getLinkSignalQuality(), is(-1));

        // test value change
        mImpl.updateLinkSignalQuality(3).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getLinkSignalQuality(), is(3));

        // test update with same value
        mImpl.updateLinkSignalQuality(3).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getLinkSignalQuality(), is(3));
    }

    @Test
    public void testLinkPerturbed() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Radio instrument = mStore.get(Radio.class);
        assert instrument != null;

        // test default value
        assertThat(instrument.isLinkPerturbed(), is(false));

        // test value change
        mImpl.updateLinkPerturbed(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.isLinkPerturbed(), is(true));

        // test update with same value
        mImpl.updateLinkPerturbed(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.isLinkPerturbed(), is(true));
    }

    @Test
    public void test4GInterfering() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        Radio instrument = mStore.get(Radio.class);
        assert instrument != null;

        // test default value
        assertThat(instrument.is4GInterfering(), is(false));

        // test value change
        mImpl.update4GInterfering(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.is4GInterfering(), is(true));

        // test update with same value
        mImpl.update4GInterfering(true).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.is4GInterfering(), is(true));
    }
}
