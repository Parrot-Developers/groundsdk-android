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

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.parrot.drone.groundsdk.internal.Cancelable;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.MockHttpService;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.ResponseBody;

import static com.parrot.drone.groundsdk.arsdkengine.http.HttpPudInfoMatcher.pudListEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HttpPudClientTests {

    @Rule
    public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private static final byte[] PUD_DATA = new byte[] {1, 2, 3, 4, 5};

    private static final List<HttpPudInfo> PUD_LIST = Arrays.asList(
            new HttpPudInfo(
                    "pud_1",
                    "19700103T182145+0100",
                    100,
                    "/data/report/pud_1"),

            new HttpPudInfo(
                    "pud_2",
                    "19700104T182145+0100",
                    2000,
                    "/data/report/pud_2"));

    private MockHttpService mMockService;

    private HttpPudClient mClient;

    private ConditionVariable mFgLock;

    @Mock
    private HttpRequest.ResultCallback<List<HttpPudInfo>> mPudListResultCb;

    @Mock
    private HttpRequest.StatusCallback mStatusCb;

    @Captor
    private ArgumentCaptor<List<HttpPudInfo>> mReportListCaptor;

    @BeforeClass
    public static void init() {
        TestExecutor.allowBackgroundTasksFromAnyThread();
    }

    @Before
    public void setUp() {
        mMockService = new MockHttpService();
        mClient = new HttpPudClient(mMockService.mSession);
        mFgLock = new ConditionVariable();
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
    public void testPudListSuccess() {
        openLockWhen(mPudListResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        Cancelable request = mClient.listPuds(mPudListResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/api/v1/pud/puds"));

        mMockService.mockResponse(it -> it
                .code(200)
                .body(ResponseBody.create(new Gson().toJson(PUD_LIST), MediaType.parse("application/json"))));

        mFgLock.block();

        verify(mPudListResultCb).onRequestComplete(
                eq(HttpRequest.Status.SUCCESS), eq(200), mReportListCaptor.capture());

        assertThat(mReportListCaptor.getValue(), pudListEquals(PUD_LIST));
    }

    @Test
    public void testPudListFailure() {
        openLockWhen(mPudListResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        Cancelable request = mClient.listPuds(mPudListResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/api/v1/pud/puds"));

        mMockService.mockResponse(it -> it
                .code(500));

        mFgLock.block();

        verify(mPudListResultCb).onRequestComplete(HttpRequest.Status.FAILED, 500, null);
    }

    @Test
    public void testPudListCancel() {
        openLockWhen(mPudListResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        Cancelable request = mClient.listPuds(mPudListResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/api/v1/pud/puds"));

        request.cancel();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(mPudListResultCb).onRequestComplete(
                HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
    }

    @Test
    public void testDownloadPudSuccess() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        File destFile = new File(mTemporaryFolder.getRoot(), "pud_1");

        Cancelable request = mClient.downloadPud("/data/pud/pud_1", destFile, mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/data/pud/pud_1"));

        mMockService.mockResponse(it -> it
                .code(200)
                .body(ResponseBody.create(PUD_DATA, MediaType.parse("application/octet-stream"))));

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.SUCCESS, 200);
        assertThat(destFile.exists(), is(true));
    }

    @Test
    public void testDownloadPudFailure() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        File destFile = new File(mTemporaryFolder.getRoot(), "pud_1");

        Cancelable request = mClient.downloadPud("/data/pud/pud_1", destFile, mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/data/pud/pud_1"));

        mMockService.mockResponse(it -> it
                .code(500));

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.FAILED, 500);
        assertThat(destFile.exists(), is(false));
    }

    @Test
    public void testDownloadPudCancel() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        File destFile = new File(mTemporaryFolder.getRoot(), "pud_1");

        Cancelable request = mClient.downloadPud("/data/pud/pud_1", destFile, mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .get()
                .url("http://test/data/pud/pud_1"));

        request.cancel();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);
        assertThat(destFile.exists(), is(false));
    }

    @Test
    public void testDeletePudSuccess() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        Cancelable request = mClient.deletePud("pud_1", mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .delete()
                .url("http://test/api/v1/pud/puds/pud_1"));

        mMockService.mockResponse(it -> it
                .code(200));

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.SUCCESS, 200);
    }

    @Test
    public void testDeletePudFailure() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        Cancelable request = mClient.deletePud("pud_1", mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .delete()
                .url("http://test/api/v1/pud/puds/pud_1"));

        mMockService.mockResponse(it -> it
                .code(500));

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.FAILED, 500);
    }

    @Test
    public void testDeletePudCancel() {
        openLockWhen(mStatusCb, mFgLock).onRequestComplete(any(), anyInt());

        Cancelable request = mClient.deletePud("pud_1", mStatusCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .delete()
                .url("http://test/api/v1/pud/puds/pud_1"));

        request.cancel();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(mStatusCb).onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);
    }
}
