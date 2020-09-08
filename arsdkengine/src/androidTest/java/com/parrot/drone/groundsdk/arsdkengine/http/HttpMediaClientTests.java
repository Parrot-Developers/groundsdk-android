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

package com.parrot.drone.groundsdk.arsdkengine.http;

import android.os.ConditionVariable;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.parrot.drone.groundsdk.DateParser;
import com.parrot.drone.groundsdk.device.peripheral.MediaStore;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.HttpSession;
import com.parrot.drone.groundsdk.internal.http.MockHttpService;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.Okio;

import static com.parrot.drone.groundsdk.arsdkengine.http.HttpMediaItemMatcher.mediaEquals;
import static com.parrot.drone.groundsdk.arsdkengine.http.HttpMediaItemMatcher.mediaListEquals;
import static com.parrot.drone.groundsdk.arsdkengine.http.HttpMediaItemMatcher.mediaResourceEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@RunWith(MockitoJUnitRunner.class)
public class HttpMediaClientTests {

    private static final String MEDIA_ID = "media-1";

    private static final String RESOURCE_ID = "resource-1";

    private static final String MEDIA_URL = "/data/media/" + MEDIA_ID;

    private static final File DOWNLOADED_MEDIA = new File(
            ApplicationProvider.getApplicationContext().getCacheDir(), "media.test");

    // 100 / 99 => mocks 99% progress, then 100% progress
    private static final byte[] MEDIA_DATA = new byte[Math.round(100f * HttpMediaClient.CHUNK_SIZE / 99)];

    static {
        new Random().nextBytes(MEDIA_DATA);
    }

    private static final HttpMediaItem MOCK_VIDEO = new HttpMediaItem(
            "10000001", HttpMediaItem.Type.VIDEO,
            DateParser.parse("2018-06-16 14:15:16"),
            16224854,
            "F9A5BFD1CE90DF37669BF11B01932F55",
            0,
            "/data/media/10000001.JPG",
            "replay/10000001",
            new HttpMediaItem.Location(10, 20, 30),
            null,
            null,
            true,
            Collections.singletonList(
                    new HttpMediaItem.Resource(
                            "10000001",
                            "100000010001.MP4",
                            HttpMediaItem.Resource.Type.VIDEO,
                            HttpMediaItem.Resource.Format.MP4,
                            DateParser.parse("2018-06-16 14:15:16"),
                            16224854,
                            600000,
                            "/data/media/100000010001.MP4",
                            "/data/media/100000010001_thumb.JPG",
                            "replay/100000010001.MP4",
                            new HttpMediaItem.Location(10, 20, 30),
                            2840, 2160, true)));

    private static final HttpMediaItem MOCK_PHOTO = new HttpMediaItem(
            "10000019", HttpMediaItem.Type.PHOTO,
            DateParser.parse("2018-01-18 17:13:51"),
            48000 + 3498023,
            "00000000000000000000000000000000",
            0,
            "/data/media/10000019.JPG",
            null,
            new HttpMediaItem.Location(60, 50, 40),
            HttpMediaItem.PhotoMode.SINGLE,
            null,
            true,
            Arrays.asList(
                    new HttpMediaItem.Resource(
                            "10000019",
                            "100000190025.JPG",
                            HttpMediaItem.Resource.Type.PHOTO,
                            HttpMediaItem.Resource.Format.JPG,
                            DateParser.parse("2018-01-18 17:13:51"),
                            48000,
                            0,
                            "/data/media/100000190025.JPG",
                            "/data/media/100000190025_thumb.JPG",
                            null,
                            new HttpMediaItem.Location(60, 50, 40),
                            128, 128,
                            false),
                    new HttpMediaItem.Resource(
                            "10000019",
                            "100000190028.JPG",
                            HttpMediaItem.Resource.Type.PHOTO,
                            HttpMediaItem.Resource.Format.DNG,
                            DateParser.parse("2018-01-18 17:13:51"),
                            3498023,
                            0,
                            "/data/media/100000190028.JPG",
                            "/data/media/100000190028_thumb.JPG",
                            null,
                            new HttpMediaItem.Location(60, 50, 40),
                            1920, 1080,
                            true)));

    private static final List<HttpMediaItem> MOCK_LIST = Arrays.asList(MOCK_PHOTO, MOCK_VIDEO);

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();


    private MockHttpService mMockService;

    private HttpMediaClient mClient;

    private ConditionVariable mFgLock;

    @Mock
    private HttpRequest.ResultCallback<List<HttpMediaItem>> mMediaListResultCb;

    @Mock
    private HttpRequest.StatusCallback mStatusCb;

    @Mock
    private HttpRequest.ResultCallback<byte[]> mFetchDataResultCb;

    @Mock
    private HttpRequest.ProgressStatusCallback mProgressCb;

    @Mock
    private HttpSession.WebSocketSubscription mWebSocketSubscription;

    @Mock
    private HttpMediaClient.Listener mMediaClientListener;

    @Captor
    private ArgumentCaptor<List<HttpMediaItem>> mMediaListCaptor;

    @Captor
    private ArgumentCaptor<HttpSession.WebSocketSubscription.MessageListener> mWebSocketListenerCaptor;

    @BeforeClass
    public static void init() {
        TestExecutor.allowBackgroundTasksFromAnyThread();
        TestExecutor.setDirectMainThreadScheduler();
    }

    @Before
    public void setUp() {
        if (DOWNLOADED_MEDIA.exists()) {
            assertThat(DOWNLOADED_MEDIA.delete(), is(true));
        }
        mMockService = new MockHttpService();
        mClient = new HttpMediaClient(mMockService.mSession);
        mFgLock = new ConditionVariable();

        doReturn(mWebSocketSubscription).when(mMockService.mSession).listenToWebSocket(anyString(), any());
    }

    @AfterClass
    public static void deInit() {
        TestExecutor.teardown();
    }

    private static <T> T openLockWhen(@NonNull T cb, @NonNull ConditionVariable lock) {
        return doAnswer((invocation) -> {
            lock.open();
            return null;
        }).when(cb);
    }

    @Test
    public void testBrowseSuccess() {
        openLockWhen(mMediaListResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.browse(null, mMediaListResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/api/v1/media/medias"));

        mMockService.mockResponse(it -> it
                .code(200)
                .body(ResponseBody.create(GSON.toJson(MOCK_LIST), MediaType.parse("application/json"))));

        mFgLock.block();

        verify(mMediaListResultCb).onRequestComplete(eq(HttpRequest.Status.SUCCESS), eq(200),
                mMediaListCaptor.capture());

        assertThat(mMediaListCaptor.getValue(), mediaListEquals(MOCK_LIST));
    }

    @Test
    public void testBrowseFailure() {
        openLockWhen(mMediaListResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.browse(null, mMediaListResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/api/v1/media/medias"));

        mMockService.mockResponse(it -> it
                .code(500));

        mFgLock.block();

        verify(mMediaListResultCb).onRequestComplete(HttpRequest.Status.FAILED, 500, null);
    }

    @Test
    public void testBrowseCancel() {
        openLockWhen(mMediaListResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.browse(null, mMediaListResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/api/v1/media/medias"));

        request.cancel();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(mMediaListResultCb).onRequestComplete(
                HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
    }

    @Test
    public void testBrowseInternalSuccess() {
        openLockWhen(mMediaListResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.browse(MediaStore.StorageType.INTERNAL, mMediaListResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/api/v1/media/medias?storage=internal"));

        mMockService.mockResponse(it -> it
                .code(200)
                .body(ResponseBody.create(GSON.toJson(MOCK_LIST), MediaType.parse("application/json"))));

        mFgLock.block();

        verify(mMediaListResultCb).onRequestComplete(eq(HttpRequest.Status.SUCCESS), eq(200),
                mMediaListCaptor.capture());

        assertThat(mMediaListCaptor.getValue(), mediaListEquals(MOCK_LIST));
    }

    @Test
    public void testBrowseInternalFailure() {
        openLockWhen(mMediaListResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.browse(MediaStore.StorageType.INTERNAL, mMediaListResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/api/v1/media/medias?storage=internal"));

        mMockService.mockResponse(it -> it
                .code(500));

        mFgLock.block();

        verify(mMediaListResultCb).onRequestComplete(HttpRequest.Status.FAILED, 500, null);
    }

    @Test
    public void testBrowseInternalCancel() {
        openLockWhen(mMediaListResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.browse(MediaStore.StorageType.INTERNAL, mMediaListResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/api/v1/media/medias?storage=internal"));

        request.cancel();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(mMediaListResultCb).onRequestComplete(
                HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
    }

    @Test
    public void testBrowseRemovableSuccess() {
        openLockWhen(mMediaListResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.browse(MediaStore.StorageType.REMOVABLE, mMediaListResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/api/v1/media/medias?storage=sdcard"));

        mMockService.mockResponse(it -> it
                .code(200)
                .body(ResponseBody.create(GSON.toJson(MOCK_LIST), MediaType.parse("application/json"))));

        mFgLock.block();

        verify(mMediaListResultCb).onRequestComplete(eq(HttpRequest.Status.SUCCESS), eq(200),
                mMediaListCaptor.capture());

        assertThat(mMediaListCaptor.getValue(), mediaListEquals(MOCK_LIST));
    }

    @Test
    public void testBrowseRemovableFailure() {
        openLockWhen(mMediaListResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.browse(MediaStore.StorageType.REMOVABLE, mMediaListResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/api/v1/media/medias?storage=sdcard"));

        mMockService.mockResponse(it -> it
                .code(500));

        mFgLock.block();

        verify(mMediaListResultCb).onRequestComplete(HttpRequest.Status.FAILED, 500, null);
    }

    @Test
    public void testBrowseRemovableCancel() {
        openLockWhen(mMediaListResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.browse(MediaStore.StorageType.REMOVABLE, mMediaListResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/api/v1/media/medias?storage=sdcard"));

        request.cancel();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(mMediaListResultCb).onRequestComplete(
                HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
    }

    @Test
    public void testDeleteMediaSuccess() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.deleteMedia(MEDIA_ID, mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .delete()
                .url("http://test/api/v1/media/medias/" + MEDIA_ID));

        mMockService.mockResponse(it -> it
                .code(200));

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.SUCCESS, 200);
    }

    @Test
    public void testDeleteMediaFailure() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.deleteMedia(MEDIA_ID, mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .delete()
                .url("http://test/api/v1/media/medias/" + MEDIA_ID));

        mMockService.mockResponse(it -> it
                .code(500));

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.FAILED, 500);
    }

    @Test
    public void testDeleteMediaCancel() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.deleteMedia(MEDIA_ID, mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .delete()
                .url("http://test/api/v1/media/medias/" + MEDIA_ID));

        request.cancel();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);
    }

    @Test
    public void testDeleteResourceSuccess() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.deleteResource(RESOURCE_ID, mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .delete()
                .url("http://test/api/v1/media/resources/" + RESOURCE_ID));

        mMockService.mockResponse(it -> it
                .code(200));

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.SUCCESS, 200);
    }

    @Test
    public void testDeleteResourceFailure() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.deleteResource(RESOURCE_ID, mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .delete()
                .url("http://test/api/v1/media/resources/" + RESOURCE_ID));

        mMockService.mockResponse(it -> it
                .code(500));

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.FAILED, 500);
    }

    @Test
    public void testDeleteResourceCancel() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.deleteResource(RESOURCE_ID, mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .delete()
                .url("http://test/api/v1/media/resources/" + RESOURCE_ID));

        request.cancel();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);
    }

    @Test
    public void testDeleteAllSuccess() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.deleteAll(mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .delete()
                .url("http://test/api/v1/media/medias"));

        mMockService.mockResponse(it -> it
                .code(200));

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.SUCCESS, 200);
    }

    @Test
    public void testDeleteAllFailure() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.deleteAll(mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .delete()
                .url("http://test/api/v1/media/medias"));

        mMockService.mockResponse(it -> it
                .code(500));

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.FAILED, 500);
    }

    @Test
    public void testDeleteAllCancel() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.deleteAll(mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .delete()
                .url("http://test/api/v1/media/medias"));

        request.cancel();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);
    }

    @Test
    public void testFetchSuccess() {
        openLockWhen(mFetchDataResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.fetch(MEDIA_URL, mFetchDataResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test" + MEDIA_URL));

        mMockService.mockResponse(response -> response
                .code(200)
                .body(ResponseBody.create(MEDIA_DATA, MediaType.parse("application/octet-stream"))));

        mFgLock.block();

        verify(mFetchDataResultCb).onRequestComplete(HttpRequest.Status.SUCCESS, 200, MEDIA_DATA);
    }

    @Test
    public void testFetchFailure() {
        openLockWhen(mFetchDataResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.fetch(MEDIA_URL, mFetchDataResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test" + MEDIA_URL));

        mMockService.mockResponse(it -> it
                .code(500));

        mFgLock.block();

        verify(mFetchDataResultCb).onRequestComplete(HttpRequest.Status.FAILED, 500, null);
    }

    @Test
    public void testFetchCancel() {
        openLockWhen(mFetchDataResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.fetch(MEDIA_URL, mFetchDataResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test" + MEDIA_URL));

        request.cancel();

        mFgLock.block();

        verify(mFetchDataResultCb).onRequestComplete(
                HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
    }

    @Test
    public void testDownloadSuccess() {
        openLockWhen(mProgressCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.download(MEDIA_URL, DOWNLOADED_MEDIA, mProgressCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test" + MEDIA_URL));

        mMockService.mockResponse(it -> it
                .code(200)
                .body(ResponseBody.create(MEDIA_DATA, MediaType.parse("application/octet-stream"))));

        mFgLock.block();

        verify(mProgressCb).onRequestProgress(99);
        verify(mProgressCb).onRequestProgress(100);
        verify(mProgressCb).onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(DOWNLOADED_MEDIA.exists(), is(true));

        try {
            byte[] fileData = new byte[MEDIA_DATA.length];
            FileInputStream stream = new FileInputStream(DOWNLOADED_MEDIA);
            assertThat(stream.read(fileData), is(MEDIA_DATA.length));
            assertThat(fileData, is(MEDIA_DATA));
            assertThat(stream.read(), is(-1));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testDownloadServerFailure() {
        openLockWhen(mProgressCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.download(MEDIA_URL, DOWNLOADED_MEDIA, mProgressCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test" + MEDIA_URL));

        mMockService.mockResponse(it -> it
                .code(500));

        mFgLock.block();

        verify(mProgressCb).onRequestComplete(HttpRequest.Status.FAILED, 500);

        assertThat(DOWNLOADED_MEDIA.exists(), is(false));
    }

    @Test
    public void testDownloadEarlyEofFailure() {
        openLockWhen(mProgressCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.download(MEDIA_URL, DOWNLOADED_MEDIA, mProgressCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test" + MEDIA_URL));

        mMockService.mockResponse(it -> it
                .code(200)
                .body(ResponseBody.create(new Buffer().write(MEDIA_DATA),
                        MediaType.parse("application/octet-stream"),
                        MEDIA_DATA.length * 2 // mock twice longer than what is actually sent
                )));

        mFgLock.block();

        verify(mProgressCb).onRequestProgress(50);
        verify(mProgressCb).onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN);

        assertThat(DOWNLOADED_MEDIA.exists(), is(false));
    }

    @Test
    public void testDownloadCancelEarly() {
        openLockWhen(mProgressCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.download(MEDIA_URL, DOWNLOADED_MEDIA, mProgressCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test" + MEDIA_URL));

        request.cancel();

        mFgLock.block();

        verify(mProgressCb).onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);

        assertThat(DOWNLOADED_MEDIA.exists(), is(false));
    }

    @Test
    public void testDownloadCancelDuringReception() {
        openLockWhen(mProgressCb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.download(MEDIA_URL, DOWNLOADED_MEDIA, mProgressCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test" + MEDIA_URL));

        BlockingBufferSource blockingData = new BlockingBufferSource(MEDIA_DATA);
        mMockService.mockResponse(it -> it
                .code(200)
                .body(ResponseBody.create(Okio.buffer(blockingData),
                        MediaType.parse("application/octet-stream"), blockingData.size())));

        // send as much as 99% progress
        openLockWhen(mProgressCb, mFgLock).onRequestProgress(anyInt());
        blockingData.unblockNextBytes(HttpMediaClient.CHUNK_SIZE);
        mFgLock.block();

        // then cancel
        mFgLock.close();
        request.cancel();
        mFgLock.block();

        verify(mProgressCb).onRequestProgress(99);
        verify(mProgressCb).onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);

        assertThat(DOWNLOADED_MEDIA.exists(), is(false));
    }

    @Test
    public void testSetListener() {
        HttpMediaItem item = MOCK_PHOTO;
        HttpMediaItem.Resource resource = item.iterator().next();
        assert item.getId() != null;
        assert resource != null;
        assert resource.getId() != null;

        mClient.setListener(mMediaClientListener);

        verify(mMockService.mSession).listenToWebSocket(eq("/api/v1/media/notifications"),
                mWebSocketListenerCaptor.capture());

        mWebSocketListenerCaptor.getValue().onMessage(GSON.toJson(new HttpMediaEvent.MediaCreated(item)));

        verify(mMediaClientListener).onMediaAdded(argThat(mediaEquals(item)));

        mWebSocketListenerCaptor.getValue().onMessage(GSON.toJson(new HttpMediaEvent.MediaDeleted(item.getId())));

        verify(mMediaClientListener).onMediaRemoved(item.getId());

        mWebSocketListenerCaptor.getValue().onMessage(GSON.toJson(new HttpMediaEvent.AllMediaDeleted()));

        verify(mMediaClientListener).onAllMediaRemoved();

        mWebSocketListenerCaptor.getValue().onMessage(GSON.toJson(new HttpMediaEvent.ResourceCreated(resource)));

        verify(mMediaClientListener).onResourceAdded(argThat(mediaResourceEquals(resource)));

        mWebSocketListenerCaptor.getValue().onMessage(GSON.toJson(
                new HttpMediaEvent.ResourceDeleted(resource.getId())));

        verify(mMediaClientListener).onResourceRemoved(resource.getId());

        mWebSocketListenerCaptor.getValue().onMessage(GSON.toJson(new HttpMediaEvent.IndexingStateChanged(
                HttpMediaIndexingState.INDEXING, HttpMediaIndexingState.INDEXED)));

        verify(mMediaClientListener).onIndexingStateChanged(HttpMediaIndexingState.INDEXED);

        mClient.setListener(null);

        verify(mWebSocketSubscription).unsubscribe();
    }

    @Test
    public void testMediaItemParcel() {
        MOCK_LIST.forEach(media -> {
            Parcel parcel = Parcel.obtain();
            media.writeToParcel(parcel);

            parcel.setDataPosition(0);
            assertThat(media, mediaEquals(new HttpMediaItem(parcel)));
        });
    }
}
