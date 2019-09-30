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

import com.parrot.drone.groundsdk.stream.Replay;
import com.parrot.drone.sdkcore.TimeProvider;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

import org.junit.After;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ReplayTest {

    @Spy
    private ReplayCore mReplay;

    @Mock
    private SdkCoreStream mStream;

    @Mock
    private SdkCoreStream.PlaybackState mPlaybackState;

    @Mock
    private StreamCore.Observer mObserver;

    @Mock
    private TimeProvider mTime;

    @Captor
    private ArgumentCaptor<SdkCoreStream.Client> mStreamClient;

    @Before
    public void setUp() {
        TimeProvider.setInstance(mTime);
    }

    @After
    public void tearDown() {
        TimeProvider.resetDefault();
    }

    @Test
    public void testInitialState() {
        assertThat(mReplay.playState(), is(Replay.PlayState.NONE));
        assertThat(mReplay.position(), is(0L));
        assertThat(mReplay.duration(), is(0L));
    }

    @Test
    public void testPlay() {
        // check that play fails when the stream refuses to open
        assertThat(mReplay.play(), is(false));
        verify(mReplay).openStream(any());

        // mock stream can be open now
        doReturn(mStream).when(mReplay).openStream(mStreamClient.capture());

        // check that play succeeds then
        assertThat(mReplay.play(), is(true));
        verify(mReplay, times(2)).openStream(any());

        // however play command shall not happen until stream reports to be opened.
        verify(mStream, never()).play();

        // mock stream opens
        mStreamClient.getValue().onStreamOpened(mStream, mPlaybackState);

        // check play state is still NONE, since we will queue a play state changing command immediately
        assertThat(mReplay.playState(), is(Replay.PlayState.NONE));

        // play command should have been issued
        verify(mStream).play();

        // mock play request acknowledge
        doReturn(1.0).when(mPlaybackState).speed();
        mReplay.onPlaybackStateChange(mPlaybackState);

        // check play state
        assertThat(mReplay.playState(), is(Replay.PlayState.PLAYING));

        // check that play fails when already playing
        assertThat(mReplay.play(), is(false));

        // mock stream playback pauses
        doReturn(0.0).when(mPlaybackState).speed();
        mReplay.onPlaybackStateChange(mPlaybackState);

        assertThat(mReplay.playState(), is(Replay.PlayState.PAUSED));

        // check that play succeeds when paused
        assertThat(mReplay.play(), is(true));
        verify(mStream, times(2)).play();

        // ensure no unexpected interaction occurred with the stream
        verifyNoMoreInteractions(mStream);
        // ensure replay did not try to open the stream more than expected
        verify(mReplay, atMost(2)).openStream(any());
    }

    @Test
    public void testPause() {
        // check that pause fails when the stream refuses to open
        assertThat(mReplay.pause(), is(false));
        verify(mReplay).openStream(any());

        // mock stream can be open now
        doReturn(mStream).when(mReplay).openStream(mStreamClient.capture());

        // check that pause succeeds then
        assertThat(mReplay.pause(), is(true));
        verify(mReplay, times(2)).openStream(any());

        // however pause command shall not happen until stream reports to be opened.
        verify(mStream, never()).pause();

        // mock stream opens
        mStreamClient.getValue().onStreamOpened(mStream, mPlaybackState);

        // check play state is still NONE, since we will queue a play state changing command immediately
        assertThat(mReplay.playState(), is(Replay.PlayState.NONE));

        // pause command should have been issued
        verify(mStream).pause();

        // mock pause request acknowledge
        doReturn(0.0).when(mPlaybackState).speed();
        mReplay.onPlaybackStateChange(mPlaybackState);

        // check play state
        assertThat(mReplay.playState(), is(Replay.PlayState.PAUSED));

        // check that pause fails when already paused
        assertThat(mReplay.pause(), is(false));

        // mock stream playback starts
        doReturn(1.0).when(mPlaybackState).speed();
        mReplay.onPlaybackStateChange(mPlaybackState);

        assertThat(mReplay.playState(), is(Replay.PlayState.PLAYING));

        // check that pause succeeds when playing
        assertThat(mReplay.pause(), is(true));
        verify(mStream, times(2)).pause();

        // ensure no unexpected interaction occurred with the stream
        verifyNoMoreInteractions(mStream);
        // ensure replay did not try to open the stream more than expected
        verify(mReplay, atMost(2)).openStream(any());
    }

    @Test
    public void testSeek() {
        // check that seek fails when the stream refuses to open
        assertThat(mReplay.seekTo(1), is(false));
        verify(mReplay).openStream(any());

        // mock stream can be open now
        doReturn(mStream).when(mReplay).openStream(mStreamClient.capture());

        // check that seek succeeds then
        assertThat(mReplay.seekTo(1), is(true));
        verify(mReplay, times(2)).openStream(any());

        // however seek command shall not happen until stream reports to be opened.
        verify(mStream, never()).seek(anyLong());

        // mock stream opens
        mStreamClient.getValue().onStreamOpened(mStream, mPlaybackState);

        // check play state is still NONE, since we will queue a play state changing command immediately
        assertThat(mReplay.playState(), is(Replay.PlayState.NONE));

        // seek command should have been issued
        verify(mStream).seek(1L);

        // mock seek request acknowledge
        doReturn(1L).when(mPlaybackState).position();
        mReplay.onPlaybackStateChange(mPlaybackState);

        assertThat(mReplay.playState(), is(Replay.PlayState.PAUSED));

        // ensure no unexpected interaction occurred with the stream
        verifyNoMoreInteractions(mStream);
        // ensure replay did not try to open the stream more than expected
        verify(mReplay, atMost(2)).openStream(any());
    }

    @Test
    public void testDuration() {
        mReplay.registerObserver(mObserver);

        verify(mObserver, never()).onChange();
        assertThat(mReplay.duration(), is(0L));

        // mock stream can be open and pause to open it
        doReturn(mStream).when(mReplay).openStream(mStreamClient.capture());
        mReplay.pause();

        // one change because the stream moves to STARTING
        verify(mObserver).onChange();

        // give initial duration
        doReturn(10L).when(mPlaybackState).duration();
        mStreamClient.getValue().onPlaybackStateChanged(mStream, mPlaybackState);

        // check initial duration
        verify(mObserver, times(2)).onChange();
        assertThat(mReplay.duration(), is(10L));

        // duration is not supposed to update more than once, but it case it happens, check observer is notified
        doReturn(5L).when(mPlaybackState).duration();
        mStreamClient.getValue().onPlaybackStateChanged(mStream, mPlaybackState);

        verify(mObserver, times(3)).onChange();
        assertThat(mReplay.duration(), is(5L));

        // ensure no unexpected observer notification
        verifyNoMoreInteractions(mObserver);
    }

    @Test
    public void testPosition() {
        mReplay.registerObserver(mObserver);

        verify(mObserver, never()).onChange();
        assertThat(mReplay.position(), is(0L));

        // mock stream can be open and pause to open it
        doReturn(mStream).when(mReplay).openStream(mStreamClient.capture());
        mReplay.pause();

        // one change because the stream moves to STARTING
        verify(mObserver).onChange();

        // also give a sufficient duration to allow testing
        doReturn(10L).when(mPlaybackState).duration();
        mStreamClient.getValue().onPlaybackStateChanged(mStream, mPlaybackState);

        // check initial position
        verify(mObserver, times(2)).onChange();
        assertThat(mReplay.position(), is(0L));

        // change position on pause
        doReturn(1L).when(mPlaybackState).position();
        mStreamClient.getValue().onPlaybackStateChanged(mStream, mPlaybackState);

        // check observer change notification and position update
        verify(mObserver, times(3)).onChange();
        assertThat(mReplay.position(), is(1L));

        // check that in pause, position does not change when time passes
        doReturn(1L).when(mTime).getElapsedRealtime();

        assertThat(mReplay.position(), is(1L));

        // move to playing, normal speed (1x)
        doReturn(1.0).when(mPlaybackState).speed();
        doReturn(2L).when(mPlaybackState).position();
        doReturn(TimeProvider.elapsedRealtime()).when(mPlaybackState).timestamp();

        mStreamClient.getValue().onPlaybackStateChanged(mStream, mPlaybackState);

        // check observer change notification and position update
        verify(mObserver, times(4)).onChange();
        assertThat(mReplay.position(), is(2L));

        // check that in playing, position updates when time passes
        doReturn(2L).when(mTime).getElapsedRealtime();

        assertThat(mReplay.position(), is(3L));

        // move to playing, 2x speed
        doReturn(2.0).when(mPlaybackState).speed();
        doReturn(3L).when(mPlaybackState).position();
        doReturn(TimeProvider.elapsedRealtime()).when(mPlaybackState).timestamp();

        mStreamClient.getValue().onPlaybackStateChanged(mStream, mPlaybackState);

        // check observer change notification and position update
        verify(mObserver, times(5)).onChange();
        assertThat(mReplay.position(), is(3L));

        // check that in playing, position updates according to speed when time passes
        doReturn(3L).when(mTime).getElapsedRealtime();

        assertThat(mReplay.position(), is(5L));

        // ensure no unexpected observer notification
        verifyNoMoreInteractions(mObserver);
    }

    @Test
    public void testPlayState() {
        mReplay.registerObserver(mObserver);

        verify(mObserver, never()).onChange();
        assertThat(mReplay.playState(), is(Replay.PlayState.NONE));

        // mock stream can be open and pause to open it
        doReturn(mStream).when(mReplay).openStream(mStreamClient.capture());
        mReplay.pause();

        // one change because the stream moves to STARTING
        verify(mObserver).onChange();

        // give initial speed: 0x (PAUSED)
        doReturn(0.0).when(mPlaybackState).speed();
        mStreamClient.getValue().onPlaybackStateChanged(mStream, mPlaybackState);

        // check initial play state
        verify(mObserver, times(2)).onChange();
        assertThat(mReplay.playState(), is(Replay.PlayState.PAUSED));

        // change speed: 1x (PLAYING)
        doReturn(1.0).when(mPlaybackState).speed();
        mStreamClient.getValue().onPlaybackStateChanged(mStream, mPlaybackState);

        // check play state is PLAYING
        verify(mObserver, times(3)).onChange();
        assertThat(mReplay.playState(), is(Replay.PlayState.PLAYING));

        // ensure no unexpected observer notification
        verifyNoMoreInteractions(mObserver);
    }

    @Test
    public void testStop() {
        // mock stream can be open and pause to open it
        doReturn(mStream).when(mReplay).openStream(mStreamClient.capture());
        mReplay.pause();

        // give initial play state: 0x (PAUSED)
        doReturn(0.0).when(mPlaybackState).speed();
        doReturn(10L).when(mPlaybackState).duration();
        doReturn(1L).when(mPlaybackState).position();
        doReturn(TimeProvider.elapsedRealtime()).when(mPlaybackState).timestamp();

        mStreamClient.getValue().onPlaybackStateChanged(mStream, mPlaybackState);

        assertThat(mReplay.playState(), is(Replay.PlayState.PAUSED));
        assertThat(mReplay.duration(), is(10L));
        assertThat(mReplay.position(), is(1L));

        // stop stream
        mReplay.stop();

        // verify state reset
        assertThat(mReplay.playState(), is(Replay.PlayState.NONE));
        assertThat(mReplay.duration(), is(0L));
        assertThat(mReplay.position(), is(0L));
    }
}

