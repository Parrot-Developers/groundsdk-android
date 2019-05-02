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

import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.device.peripheral.stream.MediaReplay;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaItemCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaResourceCore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class MediaReplayTest {

    private MediaSourceCore mSource;

    private MediaReplay mMediaReplay;

    @Before
    public void setUp() {
        MediaItemCore media = mock(MediaItemCore.class);
        doReturn("mediaUid").when(media).getUid();

        MediaResourceCore resource = mock(MediaResourceCore.class);
        doReturn("resourceUid").when(resource).getUid();
        doReturn("streamUrl").when(resource).getStreamUrl();
        doReturn("thermalTrackId").when(resource).getStreamTrackIdFor(MediaItem.Track.THERMAL_UNBLENDED);
        doReturn(media).when(resource).getMedia();

        mSource = new MediaSourceCore(resource, MediaItem.Track.THERMAL_UNBLENDED);
        mMediaReplay = new MediaReplayCore(new StreamServerCore(new MockComponentStore<>(),
                mock(StreamServerCore.Backend.class)), mSource);
    }

    @Test
    public void testSource() {
        assertThat(mMediaReplay.source(), is(mSource));
    }
}
