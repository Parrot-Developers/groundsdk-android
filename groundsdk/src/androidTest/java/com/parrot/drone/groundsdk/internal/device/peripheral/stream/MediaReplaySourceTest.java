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

import android.os.Parcel;

import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaItemCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaResourceCore;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MediaReplaySourceTest {

    private MediaSourceCore mSource;

    private StreamServerCore mServer;

    @Mock
    private MediaResourceCore mResource;

    @Mock
    private StreamServerCore.Backend mBackend;

    @Mock
    private SdkCoreStream.Client mClient;

    @Before
    public void setUp() {
        MediaItemCore media = mock(MediaItemCore.class);
        doReturn("mediaUid").when(media).getUid();

        doReturn("resourceUid").when(mResource).getUid();
        doReturn("streamUrl").when(mResource).getStreamUrl();
        doReturn("thermalTrackId").when(mResource).getStreamTrackIdFor(MediaItem.Track.THERMAL_UNBLENDED);
        doReturn(media).when(mResource).getMedia();

        mSource = new MediaSourceCore(mResource, MediaItem.Track.THERMAL_UNBLENDED);

        mServer = new StreamServerCore(new MockComponentStore<>(), mBackend);
        mServer.enableStreaming(true);
    }

    @Test
    public void testSource() {
        assertThat(mSource.mediaUid(), is("mediaUid"));
        assertThat(mSource.resourceUid(), is("resourceUid"));
        assertThat(mSource.track(), is(MediaItem.Track.THERMAL_UNBLENDED));

        mSource.openStream(mServer, mClient);
        verify(mBackend).openStream("streamUrl", "thermalTrackId", mClient);
    }

    @Test
    public void testSourceParcel() {
        Parcel p = Parcel.obtain();

        p.writeParcelable(mSource, 0);

        p.setDataPosition(0);

        MediaSourceCore fromParcel = p.readParcelable(MediaSourceCore.class.getClassLoader());

        assertThat(fromParcel, notNullValue());
        assertThat(fromParcel.mediaUid(), is(mSource.mediaUid()));
        assertThat(fromParcel.resourceUid(), is(mSource.resourceUid()));
        assertThat(fromParcel.track(), is(mSource.track()));

        fromParcel.openStream(mServer, mClient);
        verify(mBackend).openStream("streamUrl", "thermalTrackId", mClient);
    }
}
