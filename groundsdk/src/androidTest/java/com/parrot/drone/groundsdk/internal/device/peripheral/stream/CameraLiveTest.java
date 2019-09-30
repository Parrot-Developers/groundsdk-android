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

package com.parrot.drone.groundsdk.internal.device.peripheral.stream;

import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.stream.StreamCore;
import com.parrot.drone.groundsdk.stream.Stream;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class CameraLiveTest {

    private CameraLiveCore mCameraLive;

    private StreamServerCore mServer;

    @Mock
    private SdkCoreStream mStream;

    @Mock
    private SdkCoreStream.PlaybackState mPlaybackState;

    @Mock
    private StreamCore.Observer mObserver;

    @Mock
    StreamServerCore.Backend mBackend;

    @Captor
    private ArgumentCaptor<SdkCoreStream.Client> mStreamClient;

    @Before
    public void setUp() {
        mServer = new StreamServerCore(new MockComponentStore<>(), mBackend);
        mServer.enableStreaming(true);

        mCameraLive = mServer.getCameraLive();

        // mock stream can be open
        doReturn(mStream).when(mBackend).openStream(any(), any(), mStreamClient.capture());
    }

    @Test
    public void testInitialState() {
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.NONE));
    }

    @Test
    public void testPlay() {
        // check play succeeds
        assertThat(mCameraLive.play(), is(true));

        // stream should have been opened
        verify(mBackend).openStream(eq(ArsdkDevice.LIVE_URL), isNull(), eq(mStreamClient.getValue()));

        // however play command shall not happen until stream reports to be opened.
        verify(mStream, never()).play();

        // mock stream opens
        mStreamClient.getValue().onStreamOpened(mStream, mPlaybackState);

        // check play state is still NONE, since we will queue a play state changing command immediately
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.NONE));

        // play command should have been issued
        verify(mStream).play();

        // mock play request acknowledge
        doReturn(1.0).when(mPlaybackState).speed();
        mCameraLive.onPlaybackStateChange(mPlaybackState);

        // check play state
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.PLAYING));

        // check that play fails when already playing
        assertThat(mCameraLive.play(), is(false));

        // mock stream playback pauses
        doReturn(0.0).when(mPlaybackState).speed();
        mCameraLive.onPlaybackStateChange(mPlaybackState);

        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.PAUSED));

        // check that play succeeds when paused
        assertThat(mCameraLive.play(), is(true));
        verify(mStream, times(2)).play();

        // ensure no unexpected interaction occurred with the stream
        verifyNoMoreInteractions(mStream);
        // ensure camera live did not try to open the stream more than expected
        verify(mBackend, atMost(2)).openStream(any(), any(), any());
    }

    @Test
    public void testPause() {
        // check pause succeeds
        assertThat(mCameraLive.pause(), is(true));

        // stream should have been opened
        verify(mBackend).openStream(eq(ArsdkDevice.LIVE_URL), isNull(), eq(mStreamClient.getValue()));

        // however pause command shall not happen until stream reports to be opened.
        verify(mStream, never()).pause();

        // mock stream opens
        mStreamClient.getValue().onStreamOpened(mStream, mPlaybackState);

        // check play state is still NONE, since we will queue a play state changing command immediately
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.NONE));

        // pause command should have been issued
        verify(mStream).pause();

        // mock pause request acknowledge
        doReturn(0.0).when(mPlaybackState).speed();
        mCameraLive.onPlaybackStateChange(mPlaybackState);

        // check play state
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.PAUSED));

        // check that pause fails when already paused
        assertThat(mCameraLive.pause(), is(false));

        // mock stream playback starts
        doReturn(1.0).when(mPlaybackState).speed();
        mCameraLive.onPlaybackStateChange(mPlaybackState);

        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.PLAYING));

        // check that pause succeeds when playing
        assertThat(mCameraLive.pause(), is(true));
        verify(mStream, times(2)).pause();

        // ensure no unexpected interaction occurred with the stream
        verifyNoMoreInteractions(mStream);
        // ensure camera live did not try to open the stream more than expected
        verify(mBackend, atMost(2)).openStream(any(), any(), any());
    }

    @Test
    public void testPlayState() {
        mCameraLive.registerObserver(mObserver);

        verify(mObserver, never()).onChange();
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.NONE));

        // pause stream to open it
        mCameraLive.pause();

        // one change because the stream moves to STARTING
        verify(mObserver).onChange();

        // give initial speed: 0x (PAUSED)
        doReturn(0.0).when(mPlaybackState).speed();
        mStreamClient.getValue().onPlaybackStateChanged(mStream, mPlaybackState);

        // check initial play state
        verify(mObserver, times(2)).onChange();
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.PAUSED));

        // change speed: 1x (PLAYING)
        doReturn(1.0).when(mPlaybackState).speed();
        mStreamClient.getValue().onPlaybackStateChanged(mStream, mPlaybackState);

        // check play state is PLAYING
        verify(mObserver, times(3)).onChange();
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.PLAYING));

        // ensure no unexpected observer notification
        verifyNoMoreInteractions(mObserver);
    }

    @Test
    public void testStop() {
        // pause stream to open it
        mCameraLive.pause();

        // give initial play state: 0x (PAUSED)
        doReturn(0.0).when(mPlaybackState).speed();

        mStreamClient.getValue().onPlaybackStateChanged(mStream, mPlaybackState);

        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.PAUSED));

        // stop stream
        mCameraLive.stop();

        // verify state reset
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.NONE));
    }

    @Test
    public void testSuspensionOnPlay() {
        mServer.enableStreaming(false);

        mCameraLive.registerObserver(mObserver);

        verify(mObserver, never()).onChange();
        assertThat(mCameraLive.state(), is(Stream.State.STOPPED));
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.NONE));

        // check that play suspends when the stream refuses to open
        assertThat(mCameraLive.play(), is(true));

        verify(mObserver).onChange();
        assertThat(mCameraLive.state(), is(Stream.State.SUSPENDED));
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.PLAYING));

        // resume stream
        mServer.enableStreaming(true);

        // stream should be starting now
        verify(mObserver, times(2)).onChange();
        assertThat(mCameraLive.state(), is(Stream.State.STARTING));
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.PLAYING));

        // stream should have been opened
        verify(mBackend).openStream(eq(ArsdkDevice.LIVE_URL), isNull(), eq(mStreamClient.getValue()));

        // ensure no unexpected observer notification
        verifyNoMoreInteractions(mObserver);
    }

    @Test
    public void testSuspensionOnPause() {
        mServer.enableStreaming(false);

        mCameraLive.registerObserver(mObserver);

        verify(mObserver, never()).onChange();
        assertThat(mCameraLive.state(), is(Stream.State.STOPPED));
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.NONE));

        // check that play suspends when the stream refuses to open
        assertThat(mCameraLive.pause(), is(true));

        verify(mObserver).onChange();
        assertThat(mCameraLive.state(), is(Stream.State.SUSPENDED));
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.PAUSED));

        // resume stream
        mServer.enableStreaming(true);

        // stream should be starting now
        verify(mObserver, times(2)).onChange();
        assertThat(mCameraLive.state(), is(Stream.State.STARTING));
        assertThat(mCameraLive.playState(), is(CameraLive.PlayState.PAUSED));

        // stream should have been opened
        verify(mBackend).openStream(eq(ArsdkDevice.LIVE_URL), isNull(), eq(mStreamClient.getValue()));

        // ensure no unexpected observer notification
        verifyNoMoreInteractions(mObserver);
    }
}
