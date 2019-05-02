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

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive;
import com.parrot.drone.groundsdk.device.peripheral.stream.MediaReplay;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaItemCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaResourceCore;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class StreamServerTest {

    private MockComponentStore<Peripheral> mStore;

    @Mock
    private ComponentStore.Observer mObserver;

    @Mock
    private Ref.Observer<CameraLive> mCameraLiveObserver;

    @Mock
    private Ref.Observer<MediaReplay> mMediaReplayObserver;

    @Mock
    private StreamServerCore.Backend mBackend;

    @Captor
    private ArgumentCaptor<CameraLive> mCameraLive;

    @Captor
    private ArgumentCaptor<MediaReplay> mMediaReplay;

    private StreamServer mServer;

    private StreamServerCore mServerImpl;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();

        doAnswer(invocation -> {
            mServer = mStore.get(StreamServer.class);
            return null;
        }).when(mObserver).onChange();

        mStore.registerObserver(StreamServer.class, mObserver);

        mServerImpl = new StreamServerCore(mStore, mBackend);
    }

    @Test
    public void testPublication() {
        verify(mObserver, never()).onChange();
        assertThat(mServer, nullValue());

        mServerImpl.publish();

        verify(mObserver).onChange();
        assertThat(mServer, notNullValue());

        mServerImpl.unpublish();

        verify(mObserver, times(2)).onChange();
        assertThat(mServer, nullValue());
    }

    @Test
    public void testEnableStreaming() {
        mServerImpl.publish();

        // check streaming is always enabled at publication
        verify(mObserver).onChange();
        assertThat(mServer.streamingEnabled(), is(true));

        // disable streaming
        mServer.enableStreaming(false);

        verify(mObserver, times(2)).onChange();
        assertThat(mServer.streamingEnabled(), is(false));

        // enable streaming
        mServer.enableStreaming(true);

        verify(mObserver, times(3)).onChange();
        assertThat(mServer.streamingEnabled(), is(true));
    }

    @Test
    public void testCameraLive() {
        mServerImpl.publish();

        // take a ref to camera live
        Ref<CameraLive> ref1 = mServer.live(mCameraLiveObserver);

        verify(mCameraLiveObserver).onChanged(mCameraLive.capture());

        CameraLive cameraLive = mCameraLive.getValue();
        assertThat(cameraLive, notNullValue());

        // take another ref to camera live,
        Ref<CameraLive> ref2 = mServer.live(mCameraLiveObserver);

        // ensure same instance is shared
        verify(mCameraLiveObserver, times(2)).onChanged(cameraLive);

        // allow stream to open, play to open it
        SdkCoreStream stream = mock(SdkCoreStream.class);
        doReturn(stream).when(mBackend).openStream(any(), any(), any());
        cameraLive.play();

        // close all refs
        ref1.close();
        ref2.close();

        // stream should not close closed
        verify(stream, never()).close(any());
    }

    @Test
    public void testMediaReplay() {
        MediaItemCore media = mock(MediaItemCore.class);
        doReturn("mediaUid").when(media).getUid();

        MediaResourceCore resource = mock(MediaResourceCore.class);
        doReturn("resourceUid").when(resource).getUid();
        doReturn("streamUrl").when(resource).getStreamUrl();
        doReturn("thermalTrackId").when(resource).getStreamTrackIdFor(MediaItem.Track.THERMAL_UNBLENDED);
        doReturn(media).when(resource).getMedia();

        MediaReplay.Source source = MediaReplay.videoTrackOf(resource, MediaItem.Track.THERMAL_UNBLENDED);

        mServerImpl.publish();

        // take a ref to media replay
        Ref<MediaReplay> ref = mServer.replay(source, mMediaReplayObserver);

        verify(mMediaReplayObserver).onChanged(mMediaReplay.capture());

        MediaReplay mediaReplay = mMediaReplay.getValue();
        assertThat(mediaReplay, notNullValue());

        // take another ref to same media replay
        mServer.replay(source, mMediaReplayObserver);

        verify(mMediaReplayObserver, times(2)).onChanged(mMediaReplay.capture());

        // ensure media replay instance is different
        assertThat(mMediaReplay.getValue(), allOf(notNullValue(), not(mediaReplay)));

        // allow stream to open, play to open it
        SdkCoreStream stream = mock(SdkCoreStream.class);
        doReturn(stream).when(mBackend).openStream(any(), any(), any());
        mediaReplay.play();

        // close ref
        ref.close();

        // stream should be closed
        verify(stream).close(SdkCoreStream.CloseReason.USER_REQUESTED);
    }
}
