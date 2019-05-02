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

import com.parrot.drone.sdkcore.stream.SdkCoreMediaInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class MediaRegistryTest {

    private MediaRegistry mRegistry;

    private SdkCoreMediaInfo.Video.Yuv mYuvInfo;

    private SdkCoreMediaInfo.Video.H264 mH264Info;

    @Mock
    private MediaListener<SdkCoreMediaInfo.Video.Yuv> mYuvListener;

    @Mock
    private MediaListener<SdkCoreMediaInfo.Video.H264> mH264Listener;

    @Mock
    private MediaListener<SdkCoreMediaInfo.Video.H264> mOtherH264Listener;

    @Before
    public void setUp() {
        mRegistry = new MediaRegistry();
        mYuvInfo = new SdkCoreMediaInfo.Video.Yuv(0);
        mH264Info = new SdkCoreMediaInfo.Video.H264(1);
        mRegistry.registerListener(SdkCoreMediaInfo.Video.Yuv.class, mYuvListener);
        mRegistry.registerListener(SdkCoreMediaInfo.Video.H264.class, mH264Listener);

        mRegistry.addMedia(mH264Info);
        verify(mH264Listener).onMediaAvailable(mH264Info);
    }

    @Test
    public void testAddMedia() {
        mRegistry.addMedia(mYuvInfo);

        verify(mYuvListener).onMediaAvailable(mYuvInfo);

        verifyNoMoreInteractions(mYuvListener);
        verifyZeroInteractions(mH264Listener);
    }

    @Test
    public void testRemoveMedia() {
        mRegistry.removeMedia(mH264Info.mediaId());

        verify(mH264Listener).onMediaUnavailable();

        verifyNoMoreInteractions(mH264Listener);
        verifyZeroInteractions(mYuvListener);
    }

    @Test
    public void testRemoveUnregisteredMedia() {
        mRegistry.removeMedia(2);

        verifyZeroInteractions(mH264Listener, mYuvListener);
    }

    @Test
    public void testReplaceMedia() {
        SdkCoreMediaInfo.Video.H264 otherH264Info = new SdkCoreMediaInfo.Video.H264(2);

        mRegistry.addMedia(otherH264Info);

        InOrder inOrder = inOrder(mH264Listener);
        inOrder.verify(mH264Listener).onMediaUnavailable();
        inOrder.verify(mH264Listener).onMediaAvailable(otherH264Info);

        verifyNoMoreInteractions(mH264Listener);
        verifyZeroInteractions(mYuvListener);
    }

    @Test
    public void testReplaceWithSameMediaId() {
        mRegistry.addMedia(mH264Info);

        InOrder inOrder = inOrder(mH264Listener);
        inOrder.verify(mH264Listener).onMediaUnavailable();
        inOrder.verify(mH264Listener).onMediaAvailable(mH264Info);

        verifyNoMoreInteractions(mH264Listener);
        verifyZeroInteractions(mYuvListener);
    }

    @Test
    public void testRegisterToExistingMedia() {
        mRegistry.registerListener(SdkCoreMediaInfo.Video.H264.class, mOtherH264Listener);

        verify(mOtherH264Listener).onMediaAvailable(mH264Info);

        verifyNoMoreInteractions(mOtherH264Listener);
        verifyZeroInteractions(mH264Listener, mYuvListener);
    }

    @Test
    public void testUnregisterFromExistingMedia() {
        mRegistry.unregisterListener(mH264Listener);

        verify(mH264Listener).onMediaUnavailable();

        verifyNoMoreInteractions(mH264Listener);
        verifyZeroInteractions(mYuvListener);
    }
}
