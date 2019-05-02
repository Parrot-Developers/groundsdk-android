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

package com.parrot.drone.groundsdk.internal.stream;

import com.parrot.drone.groundsdk.stream.Stream;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

@RunWith(MockitoJUnitRunner.class)
public class StreamTest {

    @Spy
    StreamCore mStream;

    @Mock
    StreamCore.Observer mObserver;

    @Mock
    StreamCore.Command mCommand;

    @Mock
    SdkCoreStream mCoreStream;

    @Mock
    SdkCoreStream.PlaybackState mPlaybackState;

    @Mock
    StreamCore.Sink.Config mSinkConfig;

    @Captor
    ArgumentCaptor<SdkCoreStream.Client> mClient;

    @Before
    public void setUp() {
        doAnswer(invocation -> mock(StreamCore.Sink.class,
                withSettings().useConstructor((Stream) invocation.getArgument(0))
                              .defaultAnswer(CALLS_REAL_METHODS)))
                .when(mSinkConfig).newSink(any());
    }

    @Test
    public void testState() {
        mStream.registerObserver(mObserver);

        verify(mObserver, never()).onChange();
        assertThat(mStream.state(), is(Stream.State.STOPPED));

        doReturn(mCoreStream).when(mStream).openStream(mClient.capture());

        mStream.queueCommand(mCommand);

        verify(mObserver).onChange();
        assertThat(mStream.state(), is(Stream.State.STARTING));

        mClient.getValue().onStreamOpened(mCoreStream, mPlaybackState);

        verify(mCommand).execute(mCoreStream);

        mClient.getValue().onPlaybackStateChanged(mCoreStream, mPlaybackState);

        verify(mObserver, times(2)).onChange();
        assertThat(mStream.state(), is(Stream.State.STARTED));

        mClient.getValue().onStreamClosed(mCoreStream, SdkCoreStream.CloseReason.USER_REQUESTED);

        verify(mObserver, times(3)).onChange();
        assertThat(mStream.state(), is(Stream.State.STOPPED));
    }

    @Test
    public void testStop() {
        doReturn(mCoreStream).when(mStream).openStream(mClient.capture());
        mStream.queueCommand(mCommand);

        mStream.stop();
        verify(mCoreStream).close(SdkCoreStream.CloseReason.USER_REQUESTED);
    }

    @Test
    public void testInterrupt() {
        doReturn(mCoreStream).when(mStream).openStream(mClient.capture());
        mStream.queueCommand(mCommand);

        mStream.interrupt();
        verify(mCoreStream).close(SdkCoreStream.CloseReason.INTERRUPTED);
    }

    @Test
    public void testRelease() {
        doReturn(mCoreStream).when(mStream).openStream(mClient.capture());
        mStream.queueCommand(mCommand);

        mStream.release();
        verify(mCoreStream).close(SdkCoreStream.CloseReason.USER_REQUESTED);

        verify(mStream).onRelease();
    }

    @Test
    public void testOnStop() {
        doReturn(mCoreStream).when(mStream).openStream(mClient.capture());
        mStream.queueCommand(mCommand);

        mClient.getValue().onStreamClosed(mCoreStream, SdkCoreStream.CloseReason.UNSPECIFIED);

        verify(mStream).onStop();
    }

    @Test
    public void testSink() {
        StreamCore.Sink sink = mStream.openSink(mSinkConfig);

        doReturn(mCoreStream).when(mStream).openStream(mClient.capture());
        mStream.queueCommand(mCommand);
        mClient.getValue().onStreamOpened(mCoreStream, mPlaybackState);

        verify(sink).onSdkCoreStreamAvailable(mCoreStream);

        sink.close();

        verify(sink).onSdkCoreStreamUnavailable();

        sink = mStream.openSink(mSinkConfig);

        verify(sink).onSdkCoreStreamAvailable(mCoreStream);

        mClient.getValue().onStreamClosed(mCoreStream, SdkCoreStream.CloseReason.UNSPECIFIED);

        verify(sink).onSdkCoreStreamUnavailable();
    }

    @Test
    public void testQueueCommand() {
        mStream.registerObserver(mObserver);
        verify(mObserver, never()).onChange();

        // check queuing a null command fails when not suspended
        assertThat(mStream.queueCommand(null), is(false));
        verify(mStream, never()).openStream(any());
        verify(mObserver, never()).onChange();

        // check queue command fails when stream open returns null
        assertThat(mStream.queueCommand(mCommand), is(false));
        verify(mStream).openStream(any());
        verify(mObserver, never()).onChange();

        // allow suspension
        doReturn(true).when(mStream).onSuspension(mCommand);

        // ensure that queue command now succeeds.
        assertThat(mStream.queueCommand(mCommand), is(true));
        verify(mStream, times(2)).openStream(any());
        verify(mObserver).onChange();
        assertThat(mStream.state(), is(Stream.State.SUSPENDED));

        // allow stream to open
        doReturn(mCoreStream).when(mStream).openStream(mClient.capture());

        // queue null command to resume
        assertThat(mStream.queueCommand(null), is(true));
        verify(mObserver, times(2)).onChange();
        assertThat(mStream.state(), is(Stream.State.STARTING));

        // mock stream opens
        mClient.getValue().onStreamOpened(mCoreStream, mPlaybackState);

        // ensure original command is executed
        verify(mCommand).execute(mCoreStream);

        // queue another command while stream is open
        assertThat(mStream.queueCommand(mCommand), is(true));
        verify(mCommand, times(2)).execute(mCoreStream);
    }

    @Test
    public void testSuspensionOnStreamInterrupted() {
        mStream.registerObserver(mObserver);
        verify(mObserver, never()).onChange();

        // allow suspension
        doReturn(true).when(mStream).onSuspension(any());

        doReturn(mCoreStream).when(mStream).openStream(mClient.capture());
        mStream.queueCommand(mCommand);

        verify(mObserver).onChange();
        assertThat(mStream.state(), is(Stream.State.STARTING));

        mClient.getValue().onStreamClosed(mCoreStream, SdkCoreStream.CloseReason.INTERRUPTED);

        verify(mObserver, times(2)).onChange();
        assertThat(mStream.state(), is(Stream.State.SUSPENDED));
    }
}
