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

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.parrot.drone.groundsdk.MediaStoreWiperMatcher;
import com.parrot.drone.groundsdk.MockAppStorageProvider;
import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.peripheral.MediaStore;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDeleter;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDownloader;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaStoreWiper;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaTaskStatus;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.test.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.parrot.drone.groundsdk.MediaDeleterMatcher.hasCurrentDeletionIndex;
import static com.parrot.drone.groundsdk.MediaDeleterMatcher.hasDeletionStatus;
import static com.parrot.drone.groundsdk.MediaDeleterMatcher.hasTotalDeletionCount;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasCurrentFileProgress;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasCurrentMedia;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasCurrentMediaIndex;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasCurrentResource;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasCurrentResourceIndex;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasDownloadStatus;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasDownloadedFile;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasTotalMediaCount;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasTotalProgress;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasTotalResourceCount;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class MediaStoreTest {

    @Mock
    private MediaItemCore mMedia1, mMedia2, mMedia3;

    @Mock
    private MediaResourceCore mResource1, mResource2, mResource3, mResource4, mResource5, mResource6;

    private MockComponentStore<Peripheral> mStore;

    private MediaStoreCore mMediaStoreImpl;

    private MediaStore mMediaStore;

    @Mock
    private MediaStoreCore.Backend mBackend;

    private int mComponentChangeCnt;

    private int mChangeCnt;

    private Queue<Runnable> mOnChangeRunnables;

    @Captor
    private ArgumentCaptor<MediaRequest.ResultCallback<List<? extends MediaItemCore>>> mBrowseCb;

    @Captor
    private ArgumentCaptor<MediaRequest.ResultCallback<Bitmap>> mFetchCb;

    @Captor
    private ArgumentCaptor<MediaRequest.ProgressResultCallback<File>> mDownloadCb;

    @Captor
    private ArgumentCaptor<MediaRequest.StatusCallback> mDeleteCb;

    private void onNextChange(@NonNull Runnable runnable) {
        mOnChangeRunnables.add(runnable);
    }

    @Before
    public void setUp() {
        GroundSdkConfig.loadDefaults();

        doReturn(Arrays.asList(mResource1, mResource2)).when(mMedia1).getResources();
        doReturn(Collections.singletonList(mResource3)).when(mMedia2).getResources();
        doReturn(Arrays.asList(mResource4, mResource5, mResource6)).when(mMedia3).getResources();

        doReturn(mMedia1).when(mResource1).getMedia();
        doReturn(MediaItem.Resource.Format.JPG).when(mResource1).getFormat();
        doReturn(20L).when(mResource1).getSize();

        doReturn(mMedia1).when(mResource2).getMedia();
        doReturn(MediaItem.Resource.Format.DNG).when(mResource2).getFormat();
        doReturn(100L).when(mResource2).getSize();

        doReturn(mMedia2).when(mResource3).getMedia();
        doReturn(MediaItem.Resource.Format.MP4).when(mResource3).getFormat();
        doReturn(1000L).when(mResource3).getSize();

        doReturn(mMedia3).when(mResource4).getMedia();
        doReturn(MediaItem.Resource.Format.JPG).when(mResource4).getFormat();
        doReturn(30L).when(mResource4).getSize();

        doReturn(mMedia3).when(mResource5).getMedia();
        doReturn(MediaItem.Resource.Format.JPG).when(mResource5).getFormat();
        doReturn(30L).when(mResource5).getSize();

        doReturn(mMedia3).when(mResource6).getMedia();
        doReturn(MediaItem.Resource.Format.JPG).when(mResource6).getFormat();
        doReturn(30L).when(mResource6).getSize();

        mStore = new MockComponentStore<>();
        mMediaStoreImpl = new MediaStoreCore(mStore, mBackend);
        mMediaStore = mStore.get(MediaStore.class);
        mStore.registerObserver(MediaStore.class, () -> {
            mComponentChangeCnt++;
            mMediaStore = mStore.get(MediaStore.class);
        });

        mComponentChangeCnt = mChangeCnt = 0;
        mOnChangeRunnables = new LinkedList<>();
    }

    @Test
    public void testPublication() {
        assertThat(mMediaStore, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mMediaStoreImpl.publish();
        assertThat(mMediaStore, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mMediaStoreImpl.unpublish();
        assertThat(mMediaStore, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testIndexingState() {
        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mMediaStore.getIndexingState(), is(MediaStore.IndexingState.UNAVAILABLE));

        mMediaStoreImpl.updateIndexingState(MediaStore.IndexingState.INDEXED).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mMediaStore.getIndexingState(), is(MediaStore.IndexingState.INDEXED));
    }

    @Test
    public void testPhotoVideoCount() {
        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));
        assertThat(mMediaStore.getPhotoMediaCount(), is(0));
        assertThat(mMediaStore.getVideoMediaCount(), is(0));
        assertThat(mMediaStore.getPhotoResourceCount(), is(0));
        assertThat(mMediaStore.getVideoResourceCount(), is(0));

        mMediaStoreImpl.updatePhotoMediaCount(3).updateVideoMediaCount(4).updatePhotoResourceCount(9)
                       .updateVideoResourceCount(5).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mMediaStore.getPhotoMediaCount(), is(3));
        assertThat(mMediaStore.getVideoMediaCount(), is(4));
        assertThat(mMediaStore.getPhotoResourceCount(), is(9));
        assertThat(mMediaStore.getVideoResourceCount(), is(5));
    }

    @Test
    public void testMediaList() {
        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        // request a media list
        Ref<List<MediaItem>> listRef = mMediaStore.browse(obj -> {
            mChangeCnt++;
            assertThat(obj, notNullValue());
        });

        assertThat(listRef, notNullValue());
        assertThat(listRef.get(), nullValue());
        assertThat(mChangeCnt, is(0));

        // content change observation should start
        verify(mBackend).startWatchingContentChange();
        // browsing should start
        verify(mBackend).browse(any(), mBrowseCb.capture());

        // mock successful list reception
        mBrowseCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, Arrays.asList(mMedia1, mMedia2));

        assertThat(mChangeCnt, is(1));
        assertThat(listRef.get(), contains(mMedia1, mMedia2));

        // mock a content change
        MediaRequest mockRequest = mock(MediaRequest.class);
        doReturn(mockRequest).when(mBackend).browse(any(), any());
        mMediaStoreImpl.notifyObservers();

        // a new browse request should be emitted
        verify(mBackend, times(2)).browse(any(), mBrowseCb.capture());

        // mock a change while waiting for the request result
        mMediaStoreImpl.notifyObservers();

        // current browse request should be canceled
        verify(mockRequest).cancel();

        // mock request cancellation callback
        mBrowseCb.getValue().onRequestComplete(MediaRequest.Status.CANCELED, null);

        // media list should not change
        assertThat(mChangeCnt, is(1));
        assertThat(listRef.get(), contains(mMedia1, mMedia2));

        // another browse request should be emitted
        verify(mBackend, times(3)).browse(any(), mBrowseCb.capture());

        // mock successful list reception
        mBrowseCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, Arrays.asList(mMedia1, mMedia2, mMedia3));

        assertThat(mChangeCnt, is(2));
        assertThat(listRef.get(), contains(mMedia1, mMedia2, mMedia3));

        // close media list
        listRef.close();

        // content change observation should stop
        verify(mBackend).stopWatchingContentChange();

        // last request should have been canceled
        verify(mockRequest, times(2)).cancel();

        // list observer should not be notified
        assertThat(mChangeCnt, is(2));
        // ref content should be null
        assertThat(listRef.get(), nullValue());
    }

    @Test
    public void testMediaListWithStorageType() {
        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        // request a media list
        Ref<List<MediaItem>> listRef = mMediaStore.browse(MediaStore.StorageType.INTERNAL, obj -> {
            mChangeCnt++;
            assertThat(obj, notNullValue());
        });

        assertThat(listRef, notNullValue());
        assertThat(listRef.get(), nullValue());
        assertThat(mChangeCnt, is(0));

        // content change observation should start
        verify(mBackend).startWatchingContentChange();
        // browsing should start
        verify(mBackend).browse(eq(MediaStore.StorageType.INTERNAL), mBrowseCb.capture());

        // mock successful list reception
        mBrowseCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, Arrays.asList(mMedia1, mMedia2));

        assertThat(mChangeCnt, is(1));
        assertThat(listRef.get(), contains(mMedia1, mMedia2));

        // mock a content change
        MediaRequest mockRequest = mock(MediaRequest.class);
        doReturn(mockRequest).when(mBackend).browse(any(), any());
        mMediaStoreImpl.notifyObservers();

        // a new browse request should be emitted
        verify(mBackend, times(2)).browse(eq(MediaStore.StorageType.INTERNAL), mBrowseCb.capture());

        // mock a change while waiting for the request result
        mMediaStoreImpl.notifyObservers();

        // current browse request should be canceled
        verify(mockRequest).cancel();

        // mock request cancellation callback
        mBrowseCb.getValue().onRequestComplete(MediaRequest.Status.CANCELED, null);

        // media list should not change
        assertThat(mChangeCnt, is(1));
        assertThat(listRef.get(), contains(mMedia1, mMedia2));

        // another browse request should be emitted
        verify(mBackend, times(3)).browse(eq(MediaStore.StorageType.INTERNAL), mBrowseCb.capture());

        // mock successful list reception
        mBrowseCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, Arrays.asList(mMedia1, mMedia2, mMedia3));

        assertThat(mChangeCnt, is(2));
        assertThat(listRef.get(), contains(mMedia1, mMedia2, mMedia3));

        // close media list
        listRef.close();

        // content change observation should stop
        verify(mBackend).stopWatchingContentChange();

        // last request should have been canceled
        verify(mockRequest, times(2)).cancel();

        // list observer should not be notified
        assertThat(mChangeCnt, is(2));
        // ref content should be null
        assertThat(listRef.get(), nullValue());
    }

    @Test
    public void testMediaThumbnail() {
        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        Ref<Bitmap> thumbnailRef = mMediaStore.fetchThumbnailOf(mMedia1, obj -> mChangeCnt++);

        assertThat(thumbnailRef, notNullValue());
        assertThat(thumbnailRef.get(), nullValue());
        assertThat(mChangeCnt, is(0));

        // fetch request should fire
        verify(mBackend).fetchThumbnail(eq(mMedia1), mFetchCb.capture());

        Bitmap thumbnailBitmap = BitmapFactory.decodeResource(
                ApplicationProvider.getApplicationContext().getResources(), R.drawable.test_thumbnail);

        // mock successful thumbnail fetch
        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, thumbnailBitmap);

        assertThat(mChangeCnt, is(1));
        assertThat(thumbnailBitmap.sameAs(thumbnailRef.get()), is(true));

        // test with null bitmap
        thumbnailRef.close();
        thumbnailRef = mMediaStore.fetchThumbnailOf(mMedia2, obj -> mChangeCnt++);

        assertThat(thumbnailRef, notNullValue());
        assertThat(thumbnailRef.get(), nullValue());
        assertThat(mChangeCnt, is(1));

        // fetch request should fire
        verify(mBackend).fetchThumbnail(eq(mMedia2), mFetchCb.capture());

        // mock successful fetch with null bitmap
        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, null);

        assertThat(mChangeCnt, is(2));
        assertThat(thumbnailRef.get(), nullValue());
    }

    @Test
    public void testResourceThumbnail() {
        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        Ref<Bitmap> thumbnailRef = mMediaStore.fetchThumbnailOf(mResource1, obj -> mChangeCnt++);

        assertThat(thumbnailRef, notNullValue());
        assertThat(thumbnailRef.get(), nullValue());
        assertThat(mChangeCnt, is(0));

        // fetch request should fire
        verify(mBackend).fetchThumbnail(eq(mResource1), mFetchCb.capture());

        Bitmap thumbnailBitmap = BitmapFactory.decodeResource(
                ApplicationProvider.getApplicationContext().getResources(), R.drawable.test_thumbnail);

        // mock successful thumbnail fetch
        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, thumbnailBitmap);

        assertThat(mChangeCnt, is(1));
        assertThat(thumbnailBitmap.sameAs(thumbnailRef.get()), is(true));

        // test with null bitmap
        thumbnailRef.close();
        thumbnailRef = mMediaStore.fetchThumbnailOf(mResource2, obj -> mChangeCnt++);

        assertThat(thumbnailRef, notNullValue());
        assertThat(thumbnailRef.get(), nullValue());
        assertThat(mChangeCnt, is(1));

        // fetch request should fire
        verify(mBackend).fetchThumbnail(eq(mResource2), mFetchCb.capture());

        // mock successful fetch with null bitmap
        mFetchCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, null);

        assertThat(mChangeCnt, is(2));
        assertThat(thumbnailRef.get(), nullValue());
    }

    @Test
    public void testDownload() {
        doReturn(mock(MediaRequest.class)).when(mBackend).download(any(), any(), any());
        doReturn(mock(MediaRequest.class)).when(mBackend).download(any(), any(), any());

        ApplicationStorageProvider.setInstance(new MockAppStorageProvider());

        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        Ref<MediaDownloader> downloaderRef = mMediaStore.download(
                Stream.of(mMedia1, mMedia2).map(MediaItem::getResources).flatMap(Collection::stream)
                      .collect(Collectors.toList()),
                MediaDestination.temporary(),
                obj -> {
                    mChangeCnt++;
                    Runnable r = mOnChangeRunnables.poll();
                    if (r != null) {
                        r.run();
                    }
                });

        // first resource download request should fire
        //noinspection ConstantConditions
        verify(mBackend).download(eq(mResource1),
                eq(ApplicationStorageProvider.getInstance().getTemporaryFileCache().getAbsolutePath()),
                mDownloadCb.capture());

        assertThat(mChangeCnt, is(1));
        assertThat(downloaderRef, notNullValue());
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.RUNNING),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(1),
                hasTotalResourceCount(3),
                hasTotalProgress(0),
                hasCurrentResourceIndex(1),
                hasCurrentFileProgress(0),
                hasDownloadedFile(null),
                hasCurrentMedia(mMedia1),
                hasCurrentResource(mResource1)));

        // mock request progress
        mDownloadCb.getValue().onRequestProgress(50);

        assertThat(mChangeCnt, is(2));
        assertThat(downloaderRef, notNullValue());
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.RUNNING),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(1),
                hasTotalResourceCount(3),
                hasTotalProgress(10 * 100 / (20 + 100 + 1000)),
                hasCurrentResourceIndex(1),
                hasCurrentFileProgress(50),
                hasDownloadedFile(null),
                hasCurrentMedia(mMedia1),
                hasCurrentResource(mResource1)));

        onNextChange(() -> {
            assertThat(mChangeCnt, is(3));
            assertThat(downloaderRef, notNullValue());
            assertThat(downloaderRef.get(), allOf(
                    hasDownloadStatus(MediaTaskStatus.FILE_PROCESSED),
                    hasTotalMediaCount(2),
                    hasCurrentMediaIndex(1),
                    hasTotalResourceCount(3),
                    hasTotalProgress(20 * 100 / (20 + 100 + 1000)),
                    hasCurrentResourceIndex(1),
                    hasCurrentFileProgress(100),
                    hasDownloadedFile(new File("/tmp/file1")),
                    hasCurrentMedia(mMedia1),
                    hasCurrentResource(mResource1)));
        });

        // mock download success
        mDownloadCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, new File("/tmp/file1"));

        assertThat(mChangeCnt, is(4));
        assertThat(downloaderRef, notNullValue());
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.RUNNING),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(1),
                hasTotalResourceCount(3),
                hasTotalProgress(20 * 100 / (20 + 100 + 1000)),
                hasCurrentResourceIndex(2),
                hasCurrentFileProgress(0),
                hasDownloadedFile(null),
                hasCurrentMedia(mMedia1),
                hasCurrentResource(mResource2)));

        // second resource download request should fire
        verify(mBackend).download(eq(mResource2),
                eq(ApplicationStorageProvider.getInstance().getTemporaryFileCache().getAbsolutePath()),
                mDownloadCb.capture());

        onNextChange(() -> {
            assertThat(mChangeCnt, is(5));
            assertThat(downloaderRef, notNullValue());
            assertThat(downloaderRef.get(), allOf(
                    hasDownloadStatus(MediaTaskStatus.FILE_PROCESSED),
                    hasTotalMediaCount(2),
                    hasCurrentMediaIndex(1),
                    hasTotalResourceCount(3),
                    hasTotalProgress((20 + 100) * 100 / (20 + 100 + 1000)),
                    hasCurrentResourceIndex(2),
                    hasCurrentFileProgress(100),
                    hasDownloadedFile(new File("/tmp/file2")),
                    hasCurrentMedia(mMedia1),
                    hasCurrentResource(mResource2)));
        });

        // mock download success
        mDownloadCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, new File("/tmp/file2"));

        assertThat(mChangeCnt, is(6));
        assertThat(downloaderRef, notNullValue());
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.RUNNING),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(2),
                hasTotalResourceCount(3),
                hasTotalProgress((20 + 100) * 100 / (20 + 100 + 1000)),
                hasCurrentResourceIndex(3),
                hasCurrentFileProgress(0),
                hasDownloadedFile(null),
                hasCurrentMedia(mMedia2),
                hasCurrentResource(mResource3)));

        // third resource download request should fire
        verify(mBackend).download(eq(mResource3),
                eq(ApplicationStorageProvider.getInstance().getTemporaryFileCache().getAbsolutePath()),
                mDownloadCb.capture());

        // mock request progress
        mDownloadCb.getValue().onRequestProgress(25);

        assertThat(mChangeCnt, is(7));
        assertThat(downloaderRef, notNullValue());
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.RUNNING),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(2),
                hasTotalResourceCount(3),
                hasTotalProgress((20 + 100 + 250) * 100 / (20 + 100 + 1000)),
                hasCurrentResourceIndex(3),
                hasCurrentFileProgress(25),
                hasDownloadedFile(null),
                hasCurrentMedia(mMedia2),
                hasCurrentResource(mResource3)));

        onNextChange(() -> {
            assertThat(mChangeCnt, is(8));
            assertThat(downloaderRef, notNullValue());
            assertThat(downloaderRef.get(), allOf(
                    hasDownloadStatus(MediaTaskStatus.FILE_PROCESSED),
                    hasTotalMediaCount(2),
                    hasCurrentMediaIndex(2),
                    hasTotalResourceCount(3),
                    hasTotalProgress(100),
                    hasCurrentResourceIndex(3),
                    hasCurrentFileProgress(100),
                    hasDownloadedFile(new File("/tmp/file3")),
                    hasCurrentMedia(mMedia2),
                    hasCurrentResource(mResource3)));
        });

        // mock download success
        mDownloadCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS, new File("/tmp/file3"));

        assertThat(mChangeCnt, is(9));
        assertThat(downloaderRef, notNullValue());
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.COMPLETE),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(2),
                hasTotalResourceCount(3),
                hasTotalProgress(100),
                hasCurrentResourceIndex(3),
                hasCurrentFileProgress(100),
                hasDownloadedFile(null),
                hasCurrentMedia(null),
                hasCurrentResource(null)));

        verifyNoMoreInteractions(mBackend);

        ApplicationStorageProvider.setInstance(null);
    }

    @Test
    public void testDownloadCancel() {
        MediaRequest mockRequest = mock(MediaRequest.class);
        doReturn(mockRequest).when(mBackend).download(any(), any(), any());

        ApplicationStorageProvider.setInstance(new MockAppStorageProvider());

        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        Ref<MediaDownloader> downloaderRef = mMediaStore.download(
                Stream.of(mMedia1, mMedia2).map(MediaItem::getResources).flatMap(Collection::stream)
                      .collect(Collectors.toList()),
                MediaDestination.temporary(),
                obj -> mChangeCnt++);

        // first resource download request should fire
        //noinspection ConstantConditions
        verify(mBackend).download(eq(mResource1),
                eq(ApplicationStorageProvider.getInstance().getTemporaryFileCache().getAbsolutePath()),
                mDownloadCb.capture());

        assertThat(mChangeCnt, is(1));
        assertThat(downloaderRef, notNullValue());
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.RUNNING),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(1),
                hasTotalResourceCount(3),
                hasTotalProgress(0),
                hasCurrentResourceIndex(1),
                hasCurrentFileProgress(0),
                hasDownloadedFile(null),
                hasCurrentMedia(mMedia1),
                hasCurrentResource(mResource1)));

        // cancel
        downloaderRef.close();

        // request should be canceled
        verify(mockRequest).cancel();

        // mock request cancel callback
        mDownloadCb.getValue().onRequestComplete(MediaRequest.Status.CANCELED, null);

        // observer should not be notified
        assertThat(mChangeCnt, is(1));

        // ref content should be null
        assertThat(downloaderRef.get(), nullValue());

        ApplicationStorageProvider.setInstance(null);
    }

    @Test
    public void testDeleteMedia() {
        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        Ref<MediaDeleter> deleterRef = mMediaStore.delete(
                Stream.of(mMedia1, mMedia2).map(MediaItem::getResources).flatMap(Collection::stream)
                      .collect(Collectors.toList()),
                obj -> mChangeCnt++);

        // first media deletion request should fire
        verify(mBackend).delete(eq(mMedia1), mDeleteCb.capture());

        assertThat(mChangeCnt, is(1));
        assertThat(deleterRef, notNullValue());
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(1)));

        // mock request success
        mDeleteCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS);

        assertThat(mChangeCnt, is(2));
        assertThat(deleterRef, notNullValue());
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));

        // second media deletion request should fire
        verify(mBackend).delete(eq(mMedia2), mDeleteCb.capture());

        // mock request success
        mDeleteCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS);

        assertThat(mChangeCnt, is(3));
        assertThat(deleterRef, notNullValue());
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.COMPLETE),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));

        verifyNoMoreInteractions(mBackend);
    }

    @Test
    public void testDeleteMediaCancel() {
        MediaRequest mockRequest = mock(MediaRequest.class);
        doReturn(mockRequest).when(mBackend).delete(any(MediaItemCore.class), any());

        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        Ref<MediaDeleter> deleterRef = mMediaStore.delete(
                Stream.of(mMedia1, mMedia2).map(MediaItem::getResources).flatMap(Collection::stream)
                      .collect(Collectors.toList()),
                obj -> mChangeCnt++);

        verify(mBackend).delete(eq(mMedia1), mDeleteCb.capture());

        assertThat(mChangeCnt, is(1));
        assertThat(deleterRef, notNullValue());
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(1)));

        deleterRef.close();

        // request should have been canceled
        verify(mockRequest).cancel();

        // mock request cancel callback
        mDeleteCb.getValue().onRequestComplete(MediaRequest.Status.CANCELED);

        // observer should not be notified
        assertThat(mChangeCnt, is(1));

        // ref content should be null
        assertThat(deleterRef.get(), nullValue());
    }

    @Test
    public void testDeleteResource() {
        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        Ref<MediaDeleter> deleterRef = mMediaStore.delete(Arrays.asList(mResource1, mResource2, mResource4, mResource5),
                obj -> mChangeCnt++);


        assertThat(mChangeCnt, is(1));
        assertThat(deleterRef, notNullValue());
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(1)));

        // requesting all resources of media1 to be deleted, so we delete the whole media
        // first media deletion request should fire
        verify(mBackend).delete(eq(mMedia1), mDeleteCb.capture());

        // mock request success
        mDeleteCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS);

        // first media is processed, start processing second media
        assertThat(mChangeCnt, is(2));
        assertThat(deleterRef, notNullValue());
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));

        // requesting some resources of media3 to be deleted, so we delete the resources one by one
        verify(mBackend).delete(eq(mResource4), mDeleteCb.capture());

        // mock request success
        mDeleteCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS);

        // deleting the second resource of media3, the progress doesn't change
        assertThat(mChangeCnt, is(2));
        assertThat(deleterRef, notNullValue());
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));

        verify(mBackend).delete(eq(mResource5), mDeleteCb.capture());

        // mock request success
        mDeleteCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS);

        // all resources have been deleted
        assertThat(mChangeCnt, is(3));
        assertThat(deleterRef, notNullValue());
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.COMPLETE),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));

        verifyNoMoreInteractions(mBackend);
    }

    @Test
    public void testDeleteResourceCancel() {
        MediaRequest mockRequest = mock(MediaRequest.class);
        doReturn(mockRequest).when(mBackend).delete(any(MediaResourceCore.class), any());

        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        Ref<MediaDeleter> deleterRef = mMediaStore.delete(Arrays.asList(mResource4, mResource5),
                obj -> mChangeCnt++);

        // deletion request should fire
        verify(mBackend).delete(eq(mResource4), mDeleteCb.capture());

        assertThat(mChangeCnt, is(1));
        assertThat(deleterRef, notNullValue());
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(1),
                hasCurrentDeletionIndex(1)));

        deleterRef.close();

        // request should have been canceled
        verify(mockRequest).cancel();

        // mock request cancel callback
        mDeleteCb.getValue().onRequestComplete(MediaRequest.Status.CANCELED);

        // observer should not be notified
        assertThat(mChangeCnt, is(1));

        // ref content should be null
        assertThat(deleterRef.get(), nullValue());
    }

    @Test
    public void testWipe() {
        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        Ref<MediaStoreWiper> wiperRef = mMediaStore.wipe(obj -> mChangeCnt++);

        // delete all request should fire
        verify(mBackend).wipe(mDeleteCb.capture());

        assertThat(mChangeCnt, is(1));
        assertThat(wiperRef, notNullValue());
        assertThat(wiperRef.get(), MediaStoreWiperMatcher.hasDeletionStatus(MediaTaskStatus.RUNNING));

        // mock request success
        mDeleteCb.getValue().onRequestComplete(MediaRequest.Status.SUCCESS);

        assertThat(mChangeCnt, is(2));
        assertThat(wiperRef, notNullValue());
        assertThat(wiperRef.get(), MediaStoreWiperMatcher.hasDeletionStatus(MediaTaskStatus.COMPLETE));

        verifyNoMoreInteractions(mBackend);
    }

    @Test
    public void testWipeCancel() {
        MediaRequest mockRequest = mock(MediaRequest.class);
        doReturn(mockRequest).when(mBackend).wipe(any());


        mMediaStoreImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        Ref<MediaStoreWiper> wiperRef = mMediaStore.wipe(obj -> mChangeCnt++);

        // delete all request should fire
        verify(mBackend).wipe(mDeleteCb.capture());

        assertThat(mChangeCnt, is(1));
        assertThat(wiperRef, notNullValue());
        assertThat(wiperRef.get(), MediaStoreWiperMatcher.hasDeletionStatus(MediaTaskStatus.RUNNING));

        wiperRef.close();

        // request should have been canceled
        verify(mockRequest).cancel();

        // mock request cancel callback
        mDeleteCb.getValue().onRequestComplete(MediaRequest.Status.CANCELED);

        // observer should not be notified
        assertThat(mChangeCnt, is(1));

        // ref content should be null
        assertThat(wiperRef.get(), nullValue());
    }
}
