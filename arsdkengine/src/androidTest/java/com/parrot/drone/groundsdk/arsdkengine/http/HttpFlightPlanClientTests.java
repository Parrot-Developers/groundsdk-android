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
import androidx.test.core.app.ApplicationProvider;

import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.MockHttpService;
import com.parrot.drone.groundsdk.internal.io.Files;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HttpFlightPlanClientTests {

    private static final int OKIO_SEGMENT_SIZE = 8192; // okio directly infers how we report progress on upload

    // 100 / 99 => mocks 99% progress, then 100% progress
    private static final byte[] FILE_DATA = new byte[Math.round(100f * OKIO_SEGMENT_SIZE / 99)];

    static {
        new Random().nextBytes(FILE_DATA);
    }

    private static final File UPLOAD_FILE = new File(
            ApplicationProvider.getApplicationContext().getCacheDir(), "flightplan.test");

    private static final String FLIGHT_PLAN_UID = "flightPlanUid";


    private MockHttpService mMockService;

    private HttpFlightPlanClient mClient;

    private ConditionVariable mFgLock;

    @Mock
    private HttpRequest.ResultCallback<String> mFlightPlanIdResultCb;

    @BeforeClass
    public static void init() {
        TestExecutor.allowBackgroundTasksFromAnyThread();
    }

    @Before
    public void setUp() {
        try {
            Files.writeFile(new ByteArrayInputStream(FILE_DATA), UPLOAD_FILE);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        mMockService = new MockHttpService();
        mClient = new HttpFlightPlanClient(mMockService.mSession);
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
    public void testUploadSuccess() {
        openLockWhen(mFlightPlanIdResultCb, mFgLock).onRequestComplete(any(), anyInt(), anyString());

        HttpRequest request = mClient.uploadFlightPlan(UPLOAD_FILE, mFlightPlanIdResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .put(RequestBody.create(UPLOAD_FILE, null))
                .url("http://test/api/v1/upload/flightplan"));

        Buffer sink = mMockService.receiveFromPut(new Buffer());

        mMockService.mockResponse(it -> it
                .code(200)
                .body(ResponseBody.create(FLIGHT_PLAN_UID, MediaType.parse("text/strings"))));

        mFgLock.block();

        verify(mFlightPlanIdResultCb).onRequestComplete(HttpRequest.Status.SUCCESS, 200, FLIGHT_PLAN_UID);

        assertThat(sink.readByteArray(), is(FILE_DATA));
    }

    @Test
    public void testUploadFailure() {
        openLockWhen(mFlightPlanIdResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.uploadFlightPlan(UPLOAD_FILE, mFlightPlanIdResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .put(RequestBody.create(UPLOAD_FILE, null))
                .url("http://test/api/v1/upload/flightplan"));

        mMockService.mockResponse(it -> it.code(500));

        mFgLock.block();

        verify(mFlightPlanIdResultCb).onRequestComplete(HttpRequest.Status.FAILED, 500, null);
    }

    @Test
    public void testUploadCancelEarly() {
        openLockWhen(mFlightPlanIdResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.uploadFlightPlan(UPLOAD_FILE, mFlightPlanIdResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .put(RequestBody.create(UPLOAD_FILE, null))
                .url("http://test/api/v1/upload/flightplan"));

        request.cancel();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(mFlightPlanIdResultCb).onRequestComplete(
                HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
    }

    @Test
    public void testUploadCancelDuringUpload() {
        openLockWhen(mFlightPlanIdResultCb, mFgLock).onRequestComplete(any(), anyInt(), any());

        HttpRequest request = mClient.uploadFlightPlan(UPLOAD_FILE, mFlightPlanIdResultCb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .put(RequestBody.create(UPLOAD_FILE, null))
                .url("http://test/api/v1/upload/flightplan"));

        BlockingBufferSink sink = mMockService.receiveFromPut(new BlockingBufferSink()
                .unblockNextWrite()); // one segment, so 99% of file

        request.cancel();
        sink.unblockCompletely();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(mFlightPlanIdResultCb).onRequestComplete(
                HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
    }
}
