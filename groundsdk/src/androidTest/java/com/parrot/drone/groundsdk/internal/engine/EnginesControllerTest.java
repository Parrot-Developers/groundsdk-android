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
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class EnginesControllerTest {

    private EnginesController mEnginesController;

    private EngineBase mEngineA;

    private EngineBase mEngineB;

    @Before
    public void setup() {
        EngineBase.Controller controller = new EngineBase.Controller(mock(Context.class), new UtilityRegistry(),
                new ComponentStore<>());
        mEngineA = spy(new EngineBase(controller));
        mEngineB = spy(new EngineBase(controller));
        mEnginesController = new EnginesController(new HashSet<>(Arrays.asList(mEngineA, mEngineB)));
    }

    @Test
    public void testStart() {
        verify(mEngineA, never()).onStart();
        verify(mEngineB, never()).onStart();

        mEnginesController.start();

        verify(mEngineA, times(1)).onStart();
        verify(mEngineB, times(1)).onStart();

        verify(mEngineA, times(1)).onAllEnginesStarted();
        verify(mEngineB, times(1)).onAllEnginesStarted();

        verifyNoMoreInteractions(mEngineA, mEngineB);
    }

    @Test
    public void testStop() {
        mEnginesController.start();

        verify(mEngineA, times(1)).onStart();
        verify(mEngineB, times(1)).onStart();

        verify(mEngineA, times(1)).onAllEnginesStarted();
        verify(mEngineB, times(1)).onAllEnginesStarted();

        verifyNoMoreInteractions(mEngineA, mEngineB);

        // prevent engine B from acknowledging immediately
        doNothing().when(mEngineB).onStopRequested();

        EnginesController.OnStopListener stopListener = spy(EnginesController.OnStopListener.class);
        mEnginesController.stop(stopListener);

        verify(mEngineA, times(1)).onStopRequested();
        verify(mEngineB, times(1)).onStopRequested();
        verifyNoMoreInteractions(mEngineA, mEngineB);
        verify(stopListener, never()).onStop();

        // now acknowledge stop from engine B
        mEngineB.acknowledgeStopRequest();

        verify(mEngineA, times(1)).onStop();
        verify(mEngineB, times(1)).onStop();
        verifyNoMoreInteractions(mEngineA, mEngineB);
        verify(stopListener, times(1)).onStop();
    }
}
