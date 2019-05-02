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

import com.parrot.drone.groundsdk.device.instrument.FlyingIndicators;
import com.parrot.drone.groundsdk.device.instrument.FlyingIndicators.FlyingState;
import com.parrot.drone.groundsdk.device.instrument.FlyingIndicators.LandedState;
import com.parrot.drone.groundsdk.device.instrument.FlyingIndicators.State;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static com.parrot.drone.groundsdk.FlyingIndicatorMatcher.is;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests the FlyingIndicators instrument.
 */
public class FlyingIndicatorsTest {

    private MockComponentStore<Instrument> mStore;

    private FlyingIndicatorsCore mImpl;

    private int mChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mChangeCnt = 0;
        mImpl = new FlyingIndicatorsCore(mStore);
        mStore.registerObserver(FlyingIndicators.class, () -> mChangeCnt++);
    }

    /**
     * Test that publishing the component will add it to the store and
     * unpublishing it will remove it.
     */
    @Test
    public void testPublishUnpublish() {
        assertThat(mStore.get(FlyingIndicators.class), is(nullValue()));
        mImpl.publish();
        assertThat(mStore.get(FlyingIndicators.class), is(notNullValue()));

        mImpl.unpublish();
        assertThat(mStore.get(FlyingIndicators.class), is(nullValue()));
    }

    /**
     * Test the state and the flying state
     */
    @Test
    public void testStateAndFlyingState() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        FlyingIndicators flyingIndicators = mStore.get(FlyingIndicators.class);

        // check initial value
        assertThat(flyingIndicators, is(State.LANDED, LandedState.INITIALIZING, FlyingState.NONE));

        // check set state
        mImpl.updateLandedState(LandedState.IDLE).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(flyingIndicators, is(State.LANDED, LandedState.IDLE, FlyingState.NONE));

        // check set flying state, should also change state to flying
        mImpl.updateFlyingState(FlyingIndicators.FlyingState.TAKING_OFF).notifyUpdated();
        assertThat(mChangeCnt, is(3));
        assertThat(flyingIndicators, is(State.FLYING, LandedState.NONE, FlyingState.TAKING_OFF));

        // check set state to not flying, should also change flying state to none
        mImpl.updateState(FlyingIndicators.State.EMERGENCY).notifyUpdated();
        assertThat(mChangeCnt, is(4));
        assertThat(flyingIndicators, is(State.EMERGENCY, LandedState.NONE, FlyingState.NONE));

        // back to flying state, should also change state to flying
        mImpl.updateFlyingState(FlyingIndicators.FlyingState.FLYING).notifyUpdated();
        assertThat(mChangeCnt, is(5));
        assertThat(flyingIndicators, is(State.FLYING, LandedState.NONE, FlyingState.FLYING));

        // emergency landing
        mImpl.updateState(FlyingIndicators.State.EMERGENCY_LANDING).notifyUpdated();
        assertThat(mChangeCnt, is(6));
        assertThat(flyingIndicators, is(State.EMERGENCY_LANDING, LandedState.NONE, FlyingState.NONE));

        // test notify without changes
        mImpl.notifyUpdated();
        assertThat(mChangeCnt, is(6));
    }
}
