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

package com.parrot.drone.groundsdk.internal.device.peripheral.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.test.core.app.ApplicationProvider;

import com.parrot.drone.groundsdk.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class MediaThumbnailCacheTest {

    private static final Bitmap BITMAP = BitmapFactory.decodeResource(
            ApplicationProvider.getApplicationContext().getResources(), R.drawable.test_thumbnail);

    @Mock
    private MediaItemCore mMedia;

    @Mock
    private MediaResourceCore mResource;

    @Mock
    private MediaThumbnailCache.Backend mBackend;

    @Mock
    private MediaThumbnailCache.ThumbnailRequest.Callback mRequestCb1, mRequestCb2;

    @Captor
    private ArgumentCaptor<MediaRequest.ResultCallback<Bitmap>> mFetchCb;

    private MediaThumbnailCache mCache;

    @Before
    public void setUp() {
        doReturn(mock(MediaRequest.class)).when(mBackend).fetchThumbnail(any(MediaItemCore.class), any());
        doReturn(mock(MediaRequest.class)).when(mBackend).fetchThumbnail(any(MediaResourceCore.class), any());

        mCache = new MediaThumbnailCache(mBackend, 3 * BITMAP.getAllocationByteCount());
    }

    @Test
    public void testBasicGet() {
        MediaThumbnailCache.ThumbnailRequest request = mCache.getThumbnail(ThumbnailProvider.wrap(mMedia),
                mRequestCb1);

        assertThat(request, notNullValue());

        verify(mBackend).fetchThumbnail(eq(mMedia), mFetchCb.capture());

        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, BITMAP);

        verify(mRequestCb1).onThumbnailAvailable(BITMAP);
    }

    @Test
    public void testBasicGetResource() {
        MediaThumbnailCache.ThumbnailRequest request = mCache.getThumbnail(ThumbnailProvider.wrap(mResource),
                mRequestCb1);

        assertThat(request, notNullValue());

        verify(mBackend).fetchThumbnail(eq(mResource), mFetchCb.capture());

        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, BITMAP);

        verify(mRequestCb1).onThumbnailAvailable(BITMAP);
    }

    @Test
    public void testBasicGetNotFound() {
        MediaThumbnailCache.ThumbnailRequest request = mCache.getThumbnail(ThumbnailProvider.wrap(mMedia),
                mRequestCb1);

        assertThat(request, notNullValue());

        verify(mBackend).fetchThumbnail(eq(mMedia), mFetchCb.capture());

        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.FAILED, null);

        verify(mRequestCb1).onThumbnailAvailable(null);
    }

    @Test
    public void testMultipleGet() {
        MediaThumbnailCache.ThumbnailRequest request1 = mCache.getThumbnail(ThumbnailProvider.wrap(mMedia),
                mRequestCb1);

        assertThat(request1, notNullValue());

        verify(mBackend).fetchThumbnail(eq(mMedia), mFetchCb.capture());

        MediaThumbnailCache.ThumbnailRequest request2 = mCache.getThumbnail(ThumbnailProvider.wrap(mMedia),
                mRequestCb2);

        assertThat(request2, notNullValue());

        // should not make a new download request
        verifyNoMoreInteractions(mBackend);

        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, BITMAP);

        verify(mRequestCb1).onThumbnailAvailable(BITMAP);
        verify(mRequestCb2).onThumbnailAvailable(BITMAP);
    }

    @Test
    public void testCancel() {
        MediaThumbnailCache.ThumbnailRequest request = mCache.getThumbnail(ThumbnailProvider.wrap(mMedia),
                mRequestCb1);

        assertThat(request, notNullValue());

        verify(mBackend).fetchThumbnail(eq(mMedia), mFetchCb.capture());

        // cancel before completion
        request.cancel();

        // fetch request completes successfully anyway
        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, BITMAP);

        verifyZeroInteractions(mRequestCb1);
    }

    @Test
    public void testMultipleGetCancel() {
        MediaThumbnailCache.ThumbnailRequest request1 = mCache.getThumbnail(ThumbnailProvider.wrap(mMedia),
                mRequestCb1);

        assertThat(request1, notNullValue());

        verify(mBackend).fetchThumbnail(eq(mMedia), mFetchCb.capture());

        MediaThumbnailCache.ThumbnailRequest request2 = mCache.getThumbnail(ThumbnailProvider.wrap(mMedia),
                mRequestCb2);

        assertThat(request2, notNullValue());

        // should not make a new download request
        verifyNoMoreInteractions(mBackend);

        request1.cancel();

        // fetch request completes successfully anyway
        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, BITMAP);

        verifyZeroInteractions(mRequestCb1);
        verify(mRequestCb2).onThumbnailAvailable(BITMAP);
    }

    @Test
    public void testMultipleGetCancelAll() {

        MediaThumbnailCache.ThumbnailRequest request1 = mCache.getThumbnail(ThumbnailProvider.wrap(mMedia),
                mRequestCb1);

        assertThat(request1, notNullValue());

        verify(mBackend).fetchThumbnail(eq(mMedia), mFetchCb.capture());

        request1.cancel();

        MediaThumbnailCache.ThumbnailRequest request2 = mCache.getThumbnail(ThumbnailProvider.wrap(mMedia),
                mRequestCb2);

        assertThat(request2, notNullValue());

        // should not make a new download request
        verifyNoMoreInteractions(mBackend);

        request2.cancel();

        // fetch request completes successfully anyway
        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, BITMAP);

        verifyZeroInteractions(mRequestCb1, mRequestCb2);
    }

    @Test
    public void testGetCached() {
        MediaThumbnailCache.ThumbnailRequest request1 = mCache.getThumbnail(ThumbnailProvider.wrap(mMedia),
                mRequestCb1);

        assertThat(request1, notNullValue());

        verify(mBackend).fetchThumbnail(eq(mMedia), mFetchCb.capture());

        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, BITMAP);

        verify(mRequestCb1).onThumbnailAvailable(BITMAP);

        // request same image again
        MediaThumbnailCache.ThumbnailRequest request2 = mCache.getThumbnail(ThumbnailProvider.wrap(mMedia),
                mRequestCb2);

        assertThat(request2, nullValue());

        // should not make a new download request
        verifyNoMoreInteractions(mBackend);

        verify(mRequestCb2).onThumbnailAvailable(BITMAP);
    }

    @Test
    public void testMultipleDownload() {
        mCache.getThumbnail(ThumbnailProvider.wrap(mMedia), mRequestCb1);

        verify(mBackend).fetchThumbnail(eq(mMedia), mFetchCb.capture());

        // download 3 other thumbnails. Cache size is 3 BITMAP, so 3rd getThumbnail should remove mMedia's thumbnail
        mCache.getThumbnail(ThumbnailProvider.wrap(mock(MediaResourceCore.class)), mRequestCb2);
        mCache.getThumbnail(ThumbnailProvider.wrap(mock(MediaResourceCore.class)), mRequestCb2);
        mCache.getThumbnail(ThumbnailProvider.wrap(mock(MediaResourceCore.class)), mRequestCb2);

        // mock first fetch request successful
        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, BITMAP);

        verify(mRequestCb1).onThumbnailAvailable(BITMAP);

        // mock other thumbnails fetch success
        for (int i = 1; i < 4; i++) {
            // subsequent fetch should start
            verify(mBackend, times(i)).fetchThumbnail(any(MediaResourceCore.class), mFetchCb.capture());

            // mock success
            mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, BITMAP);

            verify(mRequestCb2, times(i)).onThumbnailAvailable(BITMAP);
        }

        mCache.getThumbnail(ThumbnailProvider.wrap(mMedia), mRequestCb1);

        // this should trigger a fetch
        verify(mBackend, times(2)).fetchThumbnail(eq(mMedia), mFetchCb.capture());

        // mock success
        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, BITMAP);

        verify(mRequestCb1, times(2)).onThumbnailAvailable(BITMAP);
    }
}