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

package com.parrot.drone.groundsdk.internal.engine;

import android.content.Context;

import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.utility.Utility;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

public class EngineBaseTest {

    private Context mContext;

    private UtilityRegistry mRegistry;

    private EngineBase mEngine;

    private InOrder mInOrder;

    private interface MockUtility extends Utility {
    }

    @Before
    public void setup() {
        mContext = mock(Context.class);
        mRegistry = new UtilityRegistry();
        mEngine = Mockito.spy(new EngineBase(new EngineBase.Controller(mContext, mRegistry, new ComponentStore<>())));

        // prevent engine to acknowledge stop immediately
        Mockito.doNothing().when(mEngine).onStopRequested();
        mInOrder = Mockito.inOrder(mEngine);
    }

    @Test
    public void testContext() {
        assertThat(mEngine.getContext(), is(mContext));
    }

    @Test
    public void testGetUtility() {
        MockUtility mockUtility = new MockUtility() {};
        mEngine.publishUtility(MockUtility.class, mockUtility);
        // move to started state
        mEngine.requestStart();
        assertThat(mEngine.getUtility(MockUtility.class), is(mockUtility));
        assertThat(mEngine.getUtilityOrThrow(MockUtility.class), is(mockUtility));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetUtilityFailsWhenStopped() {
        mEngine.getUtility(Utility.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetUtilityOrThrowFailsWhenStopped() {
        mEngine.getUtilityOrThrow(Utility.class);
    }

    @Test(expected = AssertionError.class)
    public void testGetUtilityOrThrowFailsWhenUtilityNotPresent() {
        // move to started state
        mEngine.requestStart();
        mEngine.getUtilityOrThrow(MockUtility.class);
    }

    @Test
    public void testPublishUtility() {
        MockUtility mockUtility = new MockUtility() {};
        mEngine.publishUtility(MockUtility.class, mockUtility);
        assertThat(mRegistry.getUtility(MockUtility.class), is(mockUtility));
    }

    @Test
    public void testIsRequestedToStop() {
        assertThat(mEngine.isRequestedToStop(), is(false));
        mEngine.requestStart();
        assertThat(mEngine.isRequestedToStop(), is(false));
        mEngine.requestStop(null);
        assertThat(mEngine.isRequestedToStop(), is(true));
        mEngine.acknowledgeStopRequest();
        assertThat(mEngine.isRequestedToStop(), is(false));
    }

    @Test
    public void testIsStoppedOrAcknowledged() {
        assertThat(mEngine.isStoppedOrAcknowledged(), is(true));
        mEngine.requestStart();
        assertThat(mEngine.isStoppedOrAcknowledged(), is(false));
        mEngine.requestStop(null);
        assertThat(mEngine.isStoppedOrAcknowledged(), is(false));
        mEngine.acknowledgeStopRequest();
        assertThat(mEngine.isStoppedOrAcknowledged(), is(true));
    }

    @Test
    public void testFreshStart() {
        // start when stopped
        mEngine.requestStart();

        mInOrder.verify(mEngine, Mockito.times(1)).onStart();
        mInOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testAlreadyStarted() {
        // move to started state
        mEngine.requestStart();

        mInOrder.verify(mEngine, Mockito.times(1)).onStart();

        // try to start again
        mEngine.requestStart();

        mInOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testStopRequest() {
        // move to started state
        mEngine.requestStart();

        mInOrder.verify(mEngine, Mockito.times(1)).onStart();

        // request stop
        mEngine.requestStop(null);

        mInOrder.verify(mEngine, Mockito.times(1)).onStopRequested();
        mInOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testStopAlreadyRequested() {
        // request stop when stopped
        mEngine.requestStop(null);

        mInOrder.verifyNoMoreInteractions();

        // move to stop requested state
        mEngine.requestStart();

        mInOrder.verify(mEngine, Mockito.times(1)).onStart();

        mEngine.requestStop(null);

        mInOrder.verify(mEngine, Mockito.times(1)).onStopRequested();

        // request stop
        mEngine.requestStop(null);

        mInOrder.verifyNoMoreInteractions();

        // move to stop acknowledged state
        mEngine.acknowledgeStopRequest();

        mInOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testStopCanceled() {
        // move to stop requested state
        mEngine.requestStart();

        mInOrder.verify(mEngine, Mockito.times(1)).onStart();

        mEngine.requestStop(null);

        mInOrder.verify(mEngine, Mockito.times(1)).onStopRequested();

        // cancel stop request
        mEngine.requestStart();

        mInOrder.verify(mEngine, Mockito.times(1)).onStopRequestCanceled();
        mInOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testStopAcknowledge() {
        // move to stop requested state
        mEngine.requestStart();

        mInOrder.verify(mEngine, Mockito.times(1)).onStart();

        EngineBase.OnStopRequestAcknowledgedListener mListener = Mockito.spy(
                EngineBase.OnStopRequestAcknowledgedListener.class);

        mEngine.requestStop(mListener);

        mInOrder.verify(mEngine, Mockito.times(1)).onStopRequested();

        // acknowledge stop
        mEngine.acknowledgeStopRequest();

        Mockito.verify(mListener, Mockito.times(1)).onStopRequestAcknowledged(mEngine);
        Mockito.verifyNoMoreInteractions(mListener);
        mInOrder.verifyNoMoreInteractions();
    }

    @Test(expected = IllegalStateException.class)
    public void testAckFailsWhenStopped() {
        mEngine.acknowledgeStopRequest();
    }

    @Test(expected = IllegalStateException.class)
    public void testAckFailsWhenStarted() {
        mEngine.requestStart();
        mEngine.acknowledgeStopRequest();
    }

    @Test(expected = IllegalStateException.class)
    public void testAckFailsWhenAlreadyAcknowledged() {
        mEngine.requestStart();
        mEngine.requestStop(null);
        mEngine.acknowledgeStopRequest();
        mEngine.acknowledgeStopRequest();
    }

    @Test
    public void testRestartAfterStopAck() {
        // move to stop acknowledged state
        mEngine.requestStart();

        mInOrder.verify(mEngine, Mockito.times(1)).onStart();

        mEngine.requestStop(null);

        mInOrder.verify(mEngine, Mockito.times(1)).onStopRequested();

        mEngine.acknowledgeStopRequest();

        // start after acknowledge
        mEngine.requestStart();

        mInOrder.verify(mEngine, Mockito.times(1)).onStart();

        mInOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFullStop() {
        // move to stop acknowledged state
        mEngine.requestStart();

        mInOrder.verify(mEngine, Mockito.times(1)).onStart();

        mEngine.requestStop(null);

        mInOrder.verify(mEngine, Mockito.times(1)).onStopRequested();

        mEngine.acknowledgeStopRequest();

        // stop after acknowledge
        mEngine.stop();

        mInOrder.verify(mEngine, Mockito.times(1)).onStop();
        mInOrder.verifyNoMoreInteractions();
    }

    @Test(expected = IllegalStateException.class)
    public void testStopFailsWhenStarted() {
        mEngine.requestStart();
        mEngine.stop();
    }

    @Test(expected = IllegalStateException.class)
    public void testStopFailsWhenOnlyRequested() {
        mEngine.requestStart();
        mEngine.requestStop(null);
        mEngine.stop();
    }

    @Test(expected = IllegalStateException.class)
    public void testStopFailsWhenStopped() {
        mEngine.requestStart();
        mEngine.requestStop(null);
        mEngine.acknowledgeStopRequest();
        mEngine.stop();
        mEngine.stop();
    }
}
