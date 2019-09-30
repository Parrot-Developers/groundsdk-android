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

import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class HttpActivationClientTest {

    private static final Map<String, String> REGISTER_DEVICES;

    static {
        REGISTER_DEVICES = new HashMap<>();
        REGISTER_DEVICES.put("PI040384AH7J139040", "4.4.0");
        REGISTER_DEVICES.put("PI040409AC7J229109", "1.0.7");
    }

    private static final String EXPECTED_BODY_JSON =
            "[{\"serial\":\"PI040384AH7J139040\",\"firmware\":\"4.4.0\"},"
            + "{\"serial\":\"PI040409AC7J229109\",\"firmware\":\"1.0.7\"}]";

    private MockHttpService mMockService;

    private HttpActivationClient mClient;

    private ConditionVariable mFgLock;

    @BeforeClass
    public static void init() {
        TestExecutor.allowBackgroundTasksFromAnyThread();
    }

    @Before
    public void setUp() {
        mMockService = new MockHttpService();
        mClient = new HttpActivationClient(mMockService.mSession);
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
    public void testRegisterFailure() {
        HttpRequest.StatusCallback cb = mock(HttpRequest.StatusCallback.class);
        openLockWhen(cb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.register(REGISTER_DEVICES, cb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .post(RequestBody.create(EXPECTED_BODY_JSON, MediaType.parse("application/json")))
                .url("http://test/apiv1/activation"));

        mMockService.mockResponse(it -> it.code(500));

        mFgLock.block();

        verify(cb).onRequestComplete(HttpRequest.Status.FAILED, 500);
    }

    @Test
    public void testRegisterCancel() {
        HttpRequest.StatusCallback cb = mock(HttpRequest.StatusCallback.class);
        openLockWhen(cb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.register(REGISTER_DEVICES, cb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .post(RequestBody.create(EXPECTED_BODY_JSON, MediaType.parse("application/json")))
                .url("http://test/apiv1/activation"));

        request.cancel();
        mMockService.pingForCancel();

        mFgLock.block();

        verify(cb).onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);
    }

    @Test
    public void testRegisterSuccess() {
        HttpRequest.StatusCallback cb = mock(HttpRequest.StatusCallback.class);
        openLockWhen(cb, mFgLock).onRequestComplete(any(), anyInt());

        HttpRequest request = mClient.register(REGISTER_DEVICES, cb);
        assertThat(request, notNullValue());

        mMockService.assertPendingRequest(it -> it
                .post(RequestBody.create(EXPECTED_BODY_JSON, MediaType.parse("application/json")))
                .url("http://test/apiv1/activation"));

        mMockService.mockResponse(it -> it.code(200));

        mFgLock.block();

        verify(cb).onRequestComplete(HttpRequest.Status.SUCCESS, 200);
    }
}
