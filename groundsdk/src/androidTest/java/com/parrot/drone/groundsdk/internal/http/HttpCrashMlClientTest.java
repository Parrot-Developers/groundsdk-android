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

package com.parrot.drone.groundsdk.internal.http;

import android.os.ConditionVariable;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class HttpCrashMlClientTest {

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private static final File UPLOAD_FILE =
            new File(ApplicationProvider.getApplicationContext().getCacheDir(), "report_001.tar.gz");

    private static final String ACCOUNT = "mock-account";

    private MockHttpService mMockService;

    private HttpCrashMlClient mClient;

    private ConditionVariable mFgLock;

    private File mUploadFile;

    @BeforeClass
    public static void init() {
        TestExecutor.allowBackgroundTasksFromAnyThread();
    }

    @Before
    public void setUp() {
        mMockService = new MockHttpService();
        mClient = new HttpCrashMlClient(mMockService.mSession);
        mFgLock = new ConditionVariable();

        // mock upload file exist
        mUploadFile = spy(UPLOAD_FILE);
        //noinspection ResultOfMethodCallIgnored
        doReturn(true).when(mUploadFile).exists();
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
    public void testUploadReportFailure() {
        HttpCrashMlClient.UploadCallback cb = mock(HttpCrashMlClient.UploadCallback.class);
        openLockWhen(cb, mFgLock).onRequestComplete(any());

        HttpRequest request = mClient.upload(mUploadFile, ACCOUNT, cb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .header(HttpHeader.ACCOUNT, ACCOUNT)
                .post(RequestBody.create(mUploadFile, MediaType.parse("application/gzip")))
                .url("http://test/apiv1/crashml"));

        mMockService.mockResponse(it -> it.code(500));

        mFgLock.block();

        verify(cb).onRequestComplete(HttpCrashMlClient.UploadCallback.Status.SERVER_ERROR);
    }

    @Test
    public void testUploadReportCancel() {
        HttpCrashMlClient.UploadCallback cb = mock(HttpCrashMlClient.UploadCallback.class);
        openLockWhen(cb, mFgLock).onRequestComplete(any());

        HttpRequest request = mClient.upload(mUploadFile, null, cb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .post(RequestBody.create(mUploadFile, MediaType.parse("application/gzip")))
                .url("http://test/apiv1/crashml"));

        request.cancel();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(cb).onRequestComplete(HttpCrashMlClient.UploadCallback.Status.CANCELED);
    }

    @Test
    public void testUploadReportSuccess() {
        HttpCrashMlClient.UploadCallback cb = mock(HttpCrashMlClient.UploadCallback.class);
        openLockWhen(cb, mFgLock).onRequestComplete(any());

        HttpRequest request = mClient.upload(mUploadFile, ACCOUNT, cb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .header(HttpHeader.ACCOUNT, ACCOUNT)
                .post(RequestBody.create(mUploadFile, MediaType.parse("application/gzip")))
                .url("http://test/apiv1/crashml"));

        mMockService.mockResponse(it -> it.code(200));

        mFgLock.block();

        verify(cb).onRequestComplete(HttpCrashMlClient.UploadCallback.Status.SUCCESS);
    }
}
