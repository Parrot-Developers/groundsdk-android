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
import com.parrot.drone.groundsdk.device.instrument.PhotoProgressIndicator;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.OptionalDoubleMatcher.optionalDoubleValueIs;
import static com.parrot.drone.groundsdk.OptionalValueMatcher.optionalValueIsUnavailable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class PhotoProgressIndicatorTest {

    private MockComponentStore<Instrument> mStore;

    private PhotoProgressIndicator mPhotoProgressIndicator;

    private PhotoProgressIndicatorCore mPhotoProgressIndicatorImpl;

    private int mChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mPhotoProgressIndicatorImpl = new PhotoProgressIndicatorCore(mStore);
        mPhotoProgressIndicator = mStore.get(PhotoProgressIndicator.class);
        mStore.registerObserver(PhotoProgressIndicator.class, () -> {
            mPhotoProgressIndicator = mStore.get(PhotoProgressIndicator.class);
            mChangeCnt++;
        });
        mChangeCnt = 0;
    }

    @After
    public void tearDown() {
        mStore.destroy();
        mStore = null;
        mPhotoProgressIndicatorImpl = null;
        mPhotoProgressIndicator = null;
    }

    /**
     * Test that publishing the component will add it to the store and
     * unpublishing it will remove it.
     */
    @Test
    public void testPublication() {
        assertThat(mChangeCnt, is(0));
        assertThat(mPhotoProgressIndicator, is(nullValue()));

        mPhotoProgressIndicatorImpl.publish();

        assertThat(mChangeCnt, is(1));
        assertThat(mPhotoProgressIndicator, is(mPhotoProgressIndicatorImpl));

        mPhotoProgressIndicatorImpl.unpublish();

        assertThat(mChangeCnt, is(2));
        assertThat(mPhotoProgressIndicator, is(nullValue()));
    }

    @Test
    public void testRemainingTime() {
        mPhotoProgressIndicatorImpl.publish();

        // test default value
        assertThat(mChangeCnt, is(1));
        assertThat(mPhotoProgressIndicator.getRemainingTime(), optionalValueIsUnavailable());

        // mock update from low-level
        mPhotoProgressIndicatorImpl.updateRemainingTime(1.0).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPhotoProgressIndicator.getRemainingTime(), optionalDoubleValueIs(1.0));

        // mock same update from low-level
        mPhotoProgressIndicatorImpl.updateRemainingTime(1.0).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPhotoProgressIndicator.getRemainingTime(), optionalDoubleValueIs(1.0));

        // mock reset from low-level
        mPhotoProgressIndicatorImpl.resetRemainingTime().notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mPhotoProgressIndicator.getRemainingTime(), optionalValueIsUnavailable());
    }

    @Test
    public void testRemainingDistance() {
        mPhotoProgressIndicatorImpl.publish();

        // test default value
        assertThat(mChangeCnt, is(1));
        assertThat(mPhotoProgressIndicator.getRemainingDistance(), optionalValueIsUnavailable());

        // mock update from low-level
        mPhotoProgressIndicatorImpl.updateRemainingDistance(1.0).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPhotoProgressIndicator.getRemainingDistance(), optionalDoubleValueIs(1.0));

        // mock same update from low-level
        mPhotoProgressIndicatorImpl.updateRemainingDistance(1.0).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(mPhotoProgressIndicator.getRemainingDistance(), optionalDoubleValueIs(1.0));

        // mock reset from low-level
        mPhotoProgressIndicatorImpl.resetRemainingDistance().notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(mPhotoProgressIndicator.getRemainingDistance(), optionalValueIsUnavailable());
    }
}
