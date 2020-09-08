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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.parrot.drone.groundsdk.DateParser;
import com.parrot.drone.groundsdk.MediaStoreWiperMatcher;
import com.parrot.drone.groundsdk.MockAppStorageProvider;
import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpMediaClient;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpMediaItem;
import com.parrot.drone.groundsdk.arsdkengine.http.MockHttpMedia;
import com.parrot.drone.groundsdk.arsdkengine.test.R;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.MediaStore;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDeleter;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDownloader;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaStoreWiper;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaTaskStatus;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureMediastore;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import static com.parrot.drone.groundsdk.LocationMatcher.altitudeIs;
import static com.parrot.drone.groundsdk.LocationMatcher.locationIs;
import static com.parrot.drone.groundsdk.MediaDeleterMatcher.hasCurrentDeletionIndex;
import static com.parrot.drone.groundsdk.MediaDeleterMatcher.hasDeletionStatus;
import static com.parrot.drone.groundsdk.MediaDeleterMatcher.hasTotalDeletionCount;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasCurrentFileProgress;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasCurrentMedia;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasCurrentMediaIndex;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasCurrentResourceIndex;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasDownloadStatus;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasDownloadedFile;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasTotalMediaCount;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasTotalProgress;
import static com.parrot.drone.groundsdk.MediaDownloaderMatcher.hasTotalResourceCount;
import static com.parrot.drone.groundsdk.MediaItemMatcher.containsMetadata;
import static com.parrot.drone.groundsdk.MediaItemMatcher.containsResources;
import static com.parrot.drone.groundsdk.MediaItemMatcher.hasDate;
import static com.parrot.drone.groundsdk.MediaItemMatcher.hasDuration;
import static com.parrot.drone.groundsdk.MediaItemMatcher.hasExpectedCount;
import static com.parrot.drone.groundsdk.MediaItemMatcher.hasFormat;
import static com.parrot.drone.groundsdk.MediaItemMatcher.hasName;
import static com.parrot.drone.groundsdk.MediaItemMatcher.hasPanoramaType;
import static com.parrot.drone.groundsdk.MediaItemMatcher.hasPhotoMode;
import static com.parrot.drone.groundsdk.MediaItemMatcher.hasRunId;
import static com.parrot.drone.groundsdk.MediaItemMatcher.hasSize;
import static com.parrot.drone.groundsdk.MediaItemMatcher.hasType;
import static com.parrot.drone.groundsdk.MediaItemMatcher.resourceContainsMetadata;
import static com.parrot.drone.groundsdk.MediaItemMatcher.resourceContainsTracks;
import static com.parrot.drone.groundsdk.MediaItemMatcher.resourceHasDate;
import static com.parrot.drone.groundsdk.MediaItemMatcher.resourceHasLocationSuchAs;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.media.MediaItemImplMatcher.mediaItemImplEquals;
import static com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.media.MediaItemImplMatcher.mediaResourceImplEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AnafiMediaStoreTests extends ArsdkEngineTestBase {

    private static final Date DATE_1 = DateParser.parse("2001-01-01");

    private static final Date DATE_2 = DateParser.parse("2001-02-02");

    // use large  numbers for size to ensure that all sizes are long
    private static final long JPG1_SIZE = 800L * 1024L * 1024L;

    private static final long JPG2_SIZE = 1024L * 1024L * 1024L;

    private static final long MP4_SIZE = 3L * 1024L * 1024L * 1024L;

    private static final HttpMediaItem MOCK_PHOTO = MockHttpMedia.item(
            "media1",
            HttpMediaItem.Type.PHOTO,
            DATE_1,
            1100,
            "r1",
            2,
            "/data/media/media1_thumb.jpg",
            null,
            null,
            HttpMediaItem.PhotoMode.PANORAMA,
            HttpMediaItem.PanoramaType.HORIZONTAL_180,
            true,
            Arrays.asList(
                    MockHttpMedia.resource(
                            "media1",
                            "media1-res1",
                            HttpMediaItem.Resource.Type.PHOTO,
                            HttpMediaItem.Resource.Format.JPG,
                            DATE_1,
                            JPG1_SIZE,
                            0,
                            "/data/media/media1_res1.jpg",
                            "/data/media/media1_res1_thumb.jpg",
                            null,
                            MockHttpMedia.location(10, 20, 30),
                            32,
                            32,
                            false),
                    MockHttpMedia.resource(
                            "media1",
                            "media1-res2",
                            HttpMediaItem.Resource.Type.PHOTO,
                            HttpMediaItem.Resource.Format.JPG,
                            DATE_1,
                            JPG2_SIZE,
                            0,
                            "/data/media/media1_res2.jpg",
                            "/data/media/media1_res2_thumb.jpg",
                            null,
                            null,
                            840,
                            480,
                            true),
                    null /* add an invalid resource, that should be dropped. */));

    private static final HttpMediaItem MOCK_VIDEO = MockHttpMedia.item(
            "media2",
            HttpMediaItem.Type.VIDEO,
            DATE_2,
            8000,
            "r2",
            0,
            "/data/media/media2_thumb.jpg",
            "replay/media2",
            null,
            null,
            null,
            true,
            Collections.singletonList(MockHttpMedia.resource(
                    "media2",
                    "media2-res1",
                    HttpMediaItem.Resource.Type.VIDEO,
                    HttpMediaItem.Resource.Format.MP4,
                    DATE_2,
                    MP4_SIZE, // 4G
                    600000,
                    "/data/media/media2_res1.mp4",
                    "/data/media/media2_res1_thumb.jpg",
                    "replay/media2_res1.mp4",
                    null,
                    800,
                    600,
                    true)));

    private static final HttpMediaItem MOCK_PHOTO_INVALID = MockHttpMedia.item(
            "media3",
            null, // no type: invalid
            DATE_1,
            3300,
            "r3",
            0,
            "/data/media/media3_thumb.jpg",
            null,
            null,
            HttpMediaItem.PhotoMode.SINGLE,
            null,
            false,
            Collections.singletonList(
                    MockHttpMedia.resource(
                            "media3",
                            "media3-res1",
                            HttpMediaItem.Resource.Type.PHOTO,
                            HttpMediaItem.Resource.Format.JPG,
                            DATE_1,
                            JPG1_SIZE,
                            0,
                            "/data/media/media3_res1.jpg",
                            "/data/media/media3_res1_thumb.jpg",
                            null,
                            null,
                            32,
                            32,
                            false)));

    private static final HttpMediaItem MOCK_VIDEO_INVALID = MockHttpMedia.item(
            "media4",
            null,
            DATE_1,
            4400,
            "r4",
            0,
            "/data/media/media4_thumb.jpg",
            "replay/media4",
            null,
            null,
            null,
            false,
            Collections.singletonList(
                    MockHttpMedia.resource(
                            null, // no media id: invalid
                            "media4-res1",
                            HttpMediaItem.Resource.Type.VIDEO,
                            HttpMediaItem.Resource.Format.MP4,
                            DATE_1,
                            MP4_SIZE,
                            0,
                            "/data/media/media4_res1.jpg",
                            "/data/media/media4_res1_thumb.jpg",
                            null,
                            null,
                            800,
                            600,
                            false)));

    private static final HttpRequest DUMMY_REQUEST = () -> {};

    private DroneCore mDrone;

    private MediaStore mMediaStore;

    private int mStoreChangeCnt;

    private int mChangeCnt;

    private Queue<Runnable> mOnChangeRunnables;

    @Mock
    private HttpMediaClient mMockHttpClient;

    @Captor
    private ArgumentCaptor<HttpRequest.ResultCallback<List<HttpMediaItem>>> mBrowseCb;

    @Captor
    private ArgumentCaptor<HttpMediaClient.Listener> mMediaClientListenerCaptor;

    @Captor
    private ArgumentCaptor<HttpRequest.ResultCallback<Bitmap>> mFetchCb;

    @Captor
    private ArgumentCaptor<HttpRequest.ProgressStatusCallback> mDownloadCb;

    @Captor
    private ArgumentCaptor<HttpRequest.StatusCallback> mDeleteCb;

    private void onNextChange(@NonNull Runnable runnable) {
        mOnChangeRunnables.add(runnable);
    }

    @Override
    public void setUp() {
        super.setUp();
        ApplicationStorageProvider.setInstance(new MockAppStorageProvider());

        doReturn(DUMMY_REQUEST).when(mMockHttpClient).browse(any(), any());
        doReturn(DUMMY_REQUEST).when(mMockHttpClient).fetch(any(), any(), any());
        doReturn(DUMMY_REQUEST).when(mMockHttpClient).download(any(), any(), any());
        doReturn(DUMMY_REQUEST).when(mMockHttpClient).deleteMedia(any(), any());
        doReturn(DUMMY_REQUEST).when(mMockHttpClient).deleteResource(any(), any());
        doReturn(DUMMY_REQUEST).when(mMockHttpClient).deleteAll(any());

        MockHttpSession.registerOnly(mMockHttpClient);

        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);

        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mMediaStore = mDrone.getPeripheralStore().get(mMockSession, MediaStore.class);
        mDrone.getPeripheralStore().registerObserver(MediaStore.class, () -> {
            mMediaStore = mDrone.getPeripheralStore().get(mMockSession, MediaStore.class);
            mStoreChangeCnt++;
        });

        mStoreChangeCnt = mChangeCnt = 0;
        mOnChangeRunnables = new LinkedList<>();
    }

    @Override
    public void teardown() {
        MockHttpSession.resetDefaultClients();
        ApplicationStorageProvider.setInstance(null);
        super.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mMediaStore, nullValue());
        assertThat(mStoreChangeCnt, is(0));

        connectDrone(mDrone, 1);

        assertThat(mMediaStore, notNullValue());
        assertThat(mStoreChangeCnt, is(1));

        disconnectDrone(mDrone, 1);

        assertThat(mMediaStore, nullValue());
        assertThat(mStoreChangeCnt, is(2));
    }

    @Test
    public void testIndexingState() {
        connectDrone(mDrone, 1);

        // check default value
        assertThat(mStoreChangeCnt, is(1));
        assertThat(mMediaStore.getIndexingState(), is(MediaStore.IndexingState.UNAVAILABLE));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMediastoreState(
                ArsdkFeatureMediastore.State.INDEXING));

        assertThat(mStoreChangeCnt, is(2));
        assertThat(mMediaStore.getIndexingState(), is(MediaStore.IndexingState.INDEXING));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMediastoreState(
                ArsdkFeatureMediastore.State.INDEXED));

        assertThat(mStoreChangeCnt, is(3));
        assertThat(mMediaStore.getIndexingState(), is(MediaStore.IndexingState.INDEXED));
    }

    @Test
    public void testPictureVideoCount() {
        connectDrone(mDrone, 1);

        // check default values
        assertThat(mStoreChangeCnt, is(1));
        assertThat(mMediaStore.getPhotoMediaCount(), is(0));
        assertThat(mMediaStore.getVideoMediaCount(), is(0));
        assertThat(mMediaStore.getPhotoResourceCount(), is(0));
        assertThat(mMediaStore.getVideoResourceCount(), is(0));

        // check that values are not updated with negative values (sent when media store is not indexed)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMediastoreCounters(
                -1, -1, -1, -1));

        assertThat(mStoreChangeCnt, is(1));
        assertThat(mMediaStore.getPhotoMediaCount(), is(0));
        assertThat(mMediaStore.getVideoMediaCount(), is(0));
        assertThat(mMediaStore.getPhotoResourceCount(), is(0));
        assertThat(mMediaStore.getVideoResourceCount(), is(0));

        // check update with proper values
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeMediastoreCounters(
                3, 5, 4, 6));

        assertThat(mStoreChangeCnt, is(2));
        assertThat(mMediaStore.getPhotoMediaCount(), is(5));
        assertThat(mMediaStore.getVideoMediaCount(), is(3));
        assertThat(mMediaStore.getPhotoResourceCount(), is(6));
        assertThat(mMediaStore.getVideoResourceCount(), is(4));
    }

    @Test
    public void testBrowse() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<List<MediaItem>> listRef = mMediaStore.browse(MediaStore.StorageType.INTERNAL, list -> mChangeCnt++);

        assertThat(mChangeCnt, is(0));
        assertThat(listRef.get(), nullValue());

        verify(mMockHttpClient).browse(eq(MediaStore.StorageType.INTERNAL), mBrowseCb.capture());
        mBrowseCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, Arrays.asList(
                MOCK_PHOTO, MOCK_PHOTO_INVALID, MOCK_VIDEO, MOCK_VIDEO_INVALID));

        assertThat(mChangeCnt, is(1));
        assertThat(listRef.get(), containsInAnyOrder(
                allOf(
                        hasType(MediaItem.Type.PHOTO),
                        hasName("media1"),
                        hasRunId("r1"),
                        hasExpectedCount(2),
                        hasDate(DATE_1),
                        hasPhotoMode(MediaItem.PhotoMode.PANORAMA),
                        hasPanoramaType(MediaItem.PanoramaType.HORIZONTAL_180),
                        containsMetadata(EnumSet.of(MediaItem.MetadataType.THERMAL)),
                        containsResources(
                                allOf(
                                        hasFormat(MediaItem.Resource.Format.JPG),
                                        hasSize(JPG1_SIZE),
                                        resourceHasDate(DATE_1),
                                        resourceHasLocationSuchAs(allOf(locationIs(10, 20), altitudeIs(30))),
                                        resourceContainsTracks(EnumSet.noneOf(MediaItem.Track.class)),
                                        resourceContainsMetadata(EnumSet.noneOf(MediaItem.MetadataType.class))),
                                allOf(
                                        hasFormat(MediaItem.Resource.Format.JPG),
                                        hasSize(JPG2_SIZE),
                                        resourceHasDate(DATE_1),
                                        resourceHasLocationSuchAs(nullValue()),
                                        resourceContainsTracks(EnumSet.noneOf(MediaItem.Track.class)),
                                        resourceContainsMetadata(EnumSet.of(MediaItem.MetadataType.THERMAL))))),
                allOf(
                        hasType(MediaItem.Type.VIDEO),
                        hasName("media2"),
                        hasRunId("r2"),
                        hasExpectedCount(0),
                        hasDate(DATE_2),
                        hasPhotoMode(null),
                        hasPanoramaType(null),
                        containsMetadata(EnumSet.of(MediaItem.MetadataType.THERMAL)),
                        containsResources(
                                allOf(
                                        hasFormat(MediaItem.Resource.Format.MP4),
                                        hasSize(MP4_SIZE),
                                        hasDuration(600000),
                                        resourceHasDate(DATE_2),
                                        resourceHasLocationSuchAs(nullValue()),
                                        resourceContainsTracks(EnumSet.of(MediaItem.Track.DEFAULT_VIDEO,
                                                MediaItem.Track.THERMAL_UNBLENDED)),
                                        resourceContainsMetadata(EnumSet.of(MediaItem.MetadataType.THERMAL)))))));

        // mock media added event from web-socket
        verify(mMockHttpClient).setListener(mMediaClientListenerCaptor.capture());
        clearInvocations(mMockHttpClient);
        mMediaClientListenerCaptor.getValue().onMediaAdded(MOCK_PHOTO);

        // check that media list is refreshed
        verify(mMockHttpClient).browse(eq(MediaStore.StorageType.INTERNAL), mBrowseCb.capture());
        mBrowseCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, Arrays.asList(
                MOCK_PHOTO, MOCK_VIDEO));

        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testBrowseError() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<List<MediaItem>> listRef = mMediaStore.browse(MediaStore.StorageType.REMOVABLE, list -> mChangeCnt++);

        assertThat(mChangeCnt, is(0));
        assertThat(listRef.get(), nullValue());

        verify(mMockHttpClient).browse(eq(MediaStore.StorageType.REMOVABLE),mBrowseCb.capture());
        mBrowseCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500, null);

        assertThat(mChangeCnt, is(1));
        assertThat(listRef.get(), empty());
    }

    @Test
    public void testDownloadMediaThumbnail() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<List<MediaItem>> listRef = mMediaStore.browse((list) -> {});
        verify(mMockHttpClient).browse(any(), mBrowseCb.capture());
        mBrowseCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, Collections.singletonList(MOCK_PHOTO));

        //noinspection ConstantConditions
        MediaItem item = listRef.get().get(0);

        Ref<Bitmap> thumbnailRef = mMediaStore.fetchThumbnailOf(item, obj -> mChangeCnt++);

        assertThat(mChangeCnt, is(0));
        assertThat(thumbnailRef.get(), nullValue());

        verify(mMockHttpClient).fetch(eq("/data/media/media1_thumb.jpg"), any(), mFetchCb.capture());

        Bitmap thumbnailBitmap = BitmapFactory.decodeResource(
                ApplicationProvider.getApplicationContext().getResources(), R.drawable.test_thumbnail);

        mFetchCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, thumbnailBitmap);

        assertThat(mChangeCnt, is(1));
        assertThat(thumbnailBitmap.sameAs(thumbnailRef.get()), is(true));
    }

    @Test
    public void testDownloadMediaThumbnailError() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<List<MediaItem>> listRef = mMediaStore.browse((list) -> {});
        verify(mMockHttpClient).browse(any(), mBrowseCb.capture());
        mBrowseCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, Collections.singletonList(MOCK_PHOTO));

        //noinspection ConstantConditions
        MediaItem item = listRef.get().get(0);

        Ref<Bitmap> thumbnailRef = mMediaStore.fetchThumbnailOf(item, obj -> mChangeCnt++);

        assertThat(mChangeCnt, is(0));
        assertThat(thumbnailRef.get(), nullValue());

        verify(mMockHttpClient).fetch(eq("/data/media/media1_thumb.jpg"), any(), mFetchCb.capture());

        mFetchCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500, null);

        assertThat(mChangeCnt, is(1));
        assertThat(thumbnailRef.get(), nullValue());
    }

    @Test
    public void testDownloadResourceThumbnail() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<List<MediaItem>> listRef = mMediaStore.browse((list) -> {});
        verify(mMockHttpClient).browse(any(), mBrowseCb.capture());
        mBrowseCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, Collections.singletonList(MOCK_PHOTO));

        //noinspection ConstantConditions
        MediaItem item = listRef.get().get(0);
        MediaItem.Resource resource = item.getResources().get(0);

        Ref<Bitmap> thumbnailRef = mMediaStore.fetchThumbnailOf(resource, obj -> mChangeCnt++);

        assertThat(mChangeCnt, is(0));
        assertThat(thumbnailRef.get(), nullValue());

        verify(mMockHttpClient).fetch(eq("/data/media/media1_res1_thumb.jpg"), any(), mFetchCb.capture());

        Bitmap thumbnailBitmap = BitmapFactory.decodeResource(
                ApplicationProvider.getApplicationContext().getResources(), R.drawable.test_thumbnail);

        mFetchCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, thumbnailBitmap);

        assertThat(mChangeCnt, is(1));
        assertThat(thumbnailBitmap.sameAs(thumbnailRef.get()), is(true));
    }

    @Test
    public void testDownload() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<List<MediaItem>> listRef = mMediaStore.browse((list) -> {});
        verify(mMockHttpClient).browse(any(), mBrowseCb.capture());

        mBrowseCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, Arrays.asList(MOCK_PHOTO, MOCK_VIDEO));

        List<MediaItem> list = listRef.get();
        assertThat(list, notNullValue());

        Ref<MediaDownloader> downloaderRef = mMediaStore.download(
                list.stream().flatMap(it -> it.getResources().stream()).collect(Collectors.toList()),
                MediaDestination.temporary(),
                downloader -> {
                    mChangeCnt++;
                    Runnable r = mOnChangeRunnables.poll();
                    if (r != null) {
                        r.run();
                    }
                });

        assertThat(mChangeCnt, is(1));
        //noinspection ConstantConditions
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.RUNNING),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(1),
                hasTotalResourceCount(3),
                hasTotalProgress(0),
                hasCurrentResourceIndex(1),
                hasCurrentFileProgress(0),
                hasDownloadedFile(null),
                hasCurrentMedia(listRef.get().get(0))));

        verify(mMockHttpClient).download(eq("/data/media/media1_res1.jpg"),
                eq(new File("/tmp/media1-res1")), mDownloadCb.capture());

        clearInvocations(mMockHttpClient);
        mDownloadCb.getValue().onRequestProgress(50);

        assertThat(mChangeCnt, is(2));
        //noinspection ConstantConditions
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.RUNNING),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(1),
                hasTotalResourceCount(3),
                hasTotalProgress((int) (50 * JPG1_SIZE / (JPG1_SIZE + JPG2_SIZE + MP4_SIZE))),
                hasCurrentResourceIndex(1),
                hasCurrentFileProgress(50),
                hasDownloadedFile(null),
                hasCurrentMedia(listRef.get().get(0))));

        onNextChange(() -> {
            assertThat(mChangeCnt, is(3));
            //noinspection ConstantConditions
            assertThat(downloaderRef.get(), allOf(
                    hasDownloadStatus(MediaTaskStatus.FILE_PROCESSED),
                    hasTotalMediaCount(2),
                    hasCurrentMediaIndex(1),
                    hasTotalResourceCount(3),
                    hasTotalProgress((int) (100 * JPG1_SIZE / (JPG1_SIZE + JPG2_SIZE + MP4_SIZE))),
                    hasCurrentResourceIndex(1),
                    hasCurrentFileProgress(100),
                    hasDownloadedFile(new File("/tmp/media1-res1")),
                    hasCurrentMedia(listRef.get().get(0))));
        });

        clearInvocations(mMockHttpClient);
        mDownloadCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(4));
        //noinspection ConstantConditions
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.RUNNING),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(1),
                hasTotalResourceCount(3),
                hasTotalProgress((int) (100 * JPG1_SIZE / (JPG1_SIZE + JPG2_SIZE + MP4_SIZE))),
                hasCurrentResourceIndex(2),
                hasCurrentFileProgress(0),
                hasDownloadedFile(null),
                hasCurrentMedia(listRef.get().get(0))));

        verify(mMockHttpClient).download(eq("/data/media/media1_res2.jpg"),
                eq(new File("/tmp/media1-res2")), mDownloadCb.capture());

        onNextChange(() -> {
            assertThat(mChangeCnt, is(5));
            //noinspection ConstantConditions
            assertThat(downloaderRef.get(), allOf(
                    hasDownloadStatus(MediaTaskStatus.FILE_PROCESSED),
                    hasTotalMediaCount(2),
                    hasCurrentMediaIndex(1),
                    hasTotalResourceCount(3),
                    hasTotalProgress((int) ((JPG1_SIZE + JPG2_SIZE) * 100 / (JPG1_SIZE + JPG2_SIZE + MP4_SIZE))),
                    hasCurrentResourceIndex(2),
                    hasCurrentFileProgress(100),
                    hasDownloadedFile(new File("/tmp/media1-res2")),
                    hasCurrentMedia(listRef.get().get(0))));
        });

        clearInvocations(mMockHttpClient);
        mDownloadCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(6));
        //noinspection ConstantConditions
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.RUNNING),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(2),
                hasTotalResourceCount(3),
                hasTotalProgress((int) ((JPG1_SIZE + JPG2_SIZE) * 100 / (JPG1_SIZE + JPG2_SIZE + MP4_SIZE))),
                hasCurrentResourceIndex(3),
                hasCurrentFileProgress(0),
                hasDownloadedFile(null),
                hasCurrentMedia(listRef.get().get(1))));

        verify(mMockHttpClient).download(eq("/data/media/media2_res1.mp4"),
                eq(new File("/tmp/media2-res1")), mDownloadCb.capture());

        onNextChange(() -> {
            assertThat(mChangeCnt, is(7));
            //noinspection ConstantConditions
            assertThat(downloaderRef.get(), allOf(
                    hasDownloadStatus(MediaTaskStatus.FILE_PROCESSED),
                    hasTotalMediaCount(2),
                    hasCurrentMediaIndex(2),
                    hasTotalResourceCount(3),
                    hasTotalProgress((int) ((JPG1_SIZE + JPG2_SIZE + MP4_SIZE)
                                            * 100 / (JPG1_SIZE + JPG2_SIZE + MP4_SIZE))),
                    hasCurrentResourceIndex(3),
                    hasCurrentFileProgress(100),
                    hasDownloadedFile(new File("/tmp/media2-res1")),
                    hasCurrentMedia(listRef.get().get(1))));
        });

        clearInvocations(mMockHttpClient);
        mDownloadCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(8));
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.COMPLETE),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(2),
                hasTotalResourceCount(3),
                hasTotalProgress((int) ((JPG1_SIZE + JPG2_SIZE + MP4_SIZE) * 100 / (JPG1_SIZE + JPG2_SIZE + MP4_SIZE))),
                hasCurrentResourceIndex(3),
                hasCurrentFileProgress(100),
                hasDownloadedFile(null),
                hasCurrentMedia(null)));
    }

    @Test
    public void testDownloadError() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<List<MediaItem>> listRef = mMediaStore.browse((list) -> {});
        verify(mMockHttpClient).browse(any(), mBrowseCb.capture());
        mBrowseCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, Arrays.asList(MOCK_PHOTO, MOCK_VIDEO));

        List<MediaItem> list = listRef.get();
        assertThat(list, notNullValue());

        Ref<MediaDownloader> downloaderRef = mMediaStore.download(
                list.stream().flatMap(it -> it.getResources().stream()).collect(Collectors.toList()),
                MediaDestination.temporary(),
                downloader -> {
                    mChangeCnt++;
                    Runnable r = mOnChangeRunnables.poll();
                    if (r != null) {
                        r.run();
                    }
                });

        assertThat(mChangeCnt, is(1));
        //noinspection ConstantConditions
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.RUNNING),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(1),
                hasTotalResourceCount(3),
                hasTotalProgress(0),
                hasCurrentResourceIndex(1),
                hasCurrentFileProgress(0),
                hasDownloadedFile(null),
                hasCurrentMedia(listRef.get().get(0))));

        verify(mMockHttpClient).download(eq("/data/media/media1_res1.jpg"),
                eq(new File("/tmp/media1-res1")), mDownloadCb.capture());

        clearInvocations(mMockHttpClient);
        mDownloadCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 404);

        assertThat(mChangeCnt, is(2));
        //noinspection ConstantConditions
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.RUNNING),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(1),
                hasTotalResourceCount(3),
                hasTotalProgress((int) (JPG1_SIZE * 100 / (JPG1_SIZE + JPG2_SIZE + MP4_SIZE))),
                hasCurrentResourceIndex(2),
                hasCurrentFileProgress(0),
                hasDownloadedFile(null),
                hasCurrentMedia(listRef.get().get(0))));

        verify(mMockHttpClient).download(eq("/data/media/media1_res2.jpg"),
                eq(new File("/tmp/media1-res2")), mDownloadCb.capture());

        clearInvocations(mMockHttpClient);
        mDownloadCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        assertThat(mChangeCnt, is(3));
        assertThat(downloaderRef.get(), allOf(
                hasDownloadStatus(MediaTaskStatus.ERROR),
                hasTotalMediaCount(2),
                hasCurrentMediaIndex(1),
                hasTotalResourceCount(3),
                hasTotalProgress((int) (JPG1_SIZE * 100 / (JPG1_SIZE + JPG2_SIZE + MP4_SIZE))),
                hasCurrentResourceIndex(2),
                hasCurrentFileProgress(0),
                hasDownloadedFile(null),
                hasCurrentMedia(null)));
    }

    @Test
    public void testDeleteMedia() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<List<MediaItem>> listRef = mMediaStore.browse((list) -> {});
        verify(mMockHttpClient).browse(any(), mBrowseCb.capture());

        mBrowseCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, Arrays.asList(MOCK_PHOTO, MOCK_VIDEO));

        List<MediaItem> list = listRef.get();
        assertThat(list, notNullValue());

        Ref<MediaDeleter> deleterRef = mMediaStore.delete(
                list.stream().flatMap(it -> it.getResources().stream()).collect(Collectors.toList()),
                deleter -> mChangeCnt++);

        assertThat(mChangeCnt, is(1));
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(1)));

        verify(mMockHttpClient).deleteMedia(eq("media1"), mDeleteCb.capture());

        clearInvocations(mMockHttpClient);
        mDeleteCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(2));
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));

        verify(mMockHttpClient).deleteMedia(eq("media2"), mDeleteCb.capture());

        clearInvocations(mMockHttpClient);
        mDeleteCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(3));
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.COMPLETE),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));
    }

    @Test
    public void testDeleteMediaError() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<List<MediaItem>> listRef = mMediaStore.browse((list) -> {});
        verify(mMockHttpClient).browse(any(), mBrowseCb.capture());

        mBrowseCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, Arrays.asList(MOCK_PHOTO, MOCK_VIDEO));

        List<MediaItem> list = listRef.get();
        assertThat(list, notNullValue());

        Ref<MediaDeleter> deleterRef = mMediaStore.delete(
                list.stream().flatMap(it -> it.getResources().stream()).collect(Collectors.toList()),
                deleter -> mChangeCnt++);

        assertThat(mChangeCnt, is(1));
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(1)));

        verify(mMockHttpClient).deleteMedia(eq("media1"), mDeleteCb.capture());

        clearInvocations(mMockHttpClient);
        mDeleteCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 404);

        assertThat(mChangeCnt, is(2));
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));

        verify(mMockHttpClient).deleteMedia(eq("media2"), mDeleteCb.capture());

        clearInvocations(mMockHttpClient);
        mDeleteCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        assertThat(mChangeCnt, is(3));
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.ERROR),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));
    }

    @Test
    public void testDeleteResource() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<List<MediaItem>> listRef = mMediaStore.browse((list) -> {});
        verify(mMockHttpClient).browse(any(), mBrowseCb.capture());

        mBrowseCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, Arrays.asList(MOCK_PHOTO, MOCK_VIDEO));

        List<MediaItem> list = listRef.get();
        assertThat(list, notNullValue());

        List<MediaItem.Resource> selection = new ArrayList<>();
        // add first resource of the first media
        selection.add(list.get(0).getResources().get(0));
        // add all resources of the second media
        selection.addAll(list.get(1).getResources());

        Ref<MediaDeleter> deleterRef = mMediaStore.delete(selection, deleter -> mChangeCnt++);

        assertThat(mChangeCnt, is(1));
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(1)));

        // check that first resource of media1 is deleted
        verify(mMockHttpClient).deleteResource(eq("media1-res1"), mDeleteCb.capture());

        clearInvocations(mMockHttpClient);
        mDeleteCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(2));
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));

        // check that media2 is deleted
        verify(mMockHttpClient).deleteMedia(eq("media2"), mDeleteCb.capture());

        clearInvocations(mMockHttpClient);
        mDeleteCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(3));
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.COMPLETE),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));
    }

    @Test
    public void testDeleteResourceError() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<List<MediaItem>> listRef = mMediaStore.browse((list) -> {});
        verify(mMockHttpClient).browse(any(), mBrowseCb.capture());

        mBrowseCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, Arrays.asList(MOCK_PHOTO, MOCK_VIDEO));

        List<MediaItem> list = listRef.get();
        assertThat(list, notNullValue());

        List<MediaItem.Resource> selection = new ArrayList<>();
        // add first resource of the first media
        selection.add(list.get(0).getResources().get(0));
        // add all resources of the second media
        selection.addAll(list.get(1).getResources());

        Ref<MediaDeleter> deleterRef = mMediaStore.delete(selection, deleter -> mChangeCnt++);

        assertThat(mChangeCnt, is(1));
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(1)));

        verify(mMockHttpClient).deleteResource(eq("media1-res1"), mDeleteCb.capture());

        clearInvocations(mMockHttpClient);
        mDeleteCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 404);

        assertThat(mChangeCnt, is(2));
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.RUNNING),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));

        verify(mMockHttpClient).deleteMedia(eq("media2"), mDeleteCb.capture());

        clearInvocations(mMockHttpClient);
        mDeleteCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        assertThat(mChangeCnt, is(3));
        assertThat(deleterRef.get(), allOf(
                hasDeletionStatus(MediaTaskStatus.ERROR),
                hasTotalDeletionCount(2),
                hasCurrentDeletionIndex(2)));
    }

    @Test
    public void testWipe() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<MediaStoreWiper> wiperRef = mMediaStore.wipe(deleter -> mChangeCnt++);

        assertThat(mChangeCnt, is(1));
        assertThat(wiperRef.get(), MediaStoreWiperMatcher.hasDeletionStatus(MediaTaskStatus.RUNNING));

        verify(mMockHttpClient).deleteAll(mDeleteCb.capture());

        clearInvocations(mMockHttpClient);
        mDeleteCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(2));
        assertThat(wiperRef.get(), MediaStoreWiperMatcher.hasDeletionStatus(MediaTaskStatus.COMPLETE));
    }

    @Test
    public void testWipeError() {
        connectDrone(mDrone, 1);
        clearInvocations(mMockHttpClient);

        Ref<MediaStoreWiper> wiperRef = mMediaStore.wipe(deleter -> mChangeCnt++);

        assertThat(mChangeCnt, is(1));
        assertThat(wiperRef.get(), MediaStoreWiperMatcher.hasDeletionStatus(MediaTaskStatus.RUNNING));

        verify(mMockHttpClient).deleteAll(mDeleteCb.capture());

        clearInvocations(mMockHttpClient);
        mDeleteCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 400);

        assertThat(mChangeCnt, is(2));
        assertThat(wiperRef.get(), MediaStoreWiperMatcher.hasDeletionStatus(MediaTaskStatus.ERROR));
    }

    @Test
    public void testMediaItemParcel() {
        MediaItemImpl.from(Arrays.asList(MOCK_PHOTO, MOCK_VIDEO)).forEach(media -> {

            Parcel p = Parcel.obtain();
            p.writeParcelable(media, 0);

            p.setDataPosition(0);

            assertThat(p.readParcelable(media.getClass().getClassLoader()), mediaItemImplEquals(media));
        });
    }

    @Test
    public void testMediaResourceParcel() {
        MediaItemImpl.from(Arrays.asList(MOCK_PHOTO, MOCK_VIDEO))
                     .stream().flatMap(it -> it.getResources().stream()).forEach(resource -> {

            Parcel p = Parcel.obtain();
            p.writeParcelable(resource, 0);

            p.setDataPosition(0);

            assertThat(p.readParcelable(resource.getClass().getClassLoader()), mediaResourceImplEquals(resource));
        });
    }
}
