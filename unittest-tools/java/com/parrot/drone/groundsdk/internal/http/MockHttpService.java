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

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Locale;
import java.util.function.Consumer;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.spy;

public class MockHttpService {

    private static final ResponseBody EMPTY_RESPONSE = ResponseBody.create("", MediaType.parse(""));

    public final HttpSession mSession;

    private Request mRequest;

    private Response mResponse;

    private Sink mPutSink;

    public MockHttpService() {
        mSession = spy(new HttpSession(this::intercept));
    }

    @NonNull
    private Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Response response;
        synchronized (mSession) {
            mPutSink = null;
            while (mRequest == null) {
                try {
                    mSession.wait();
                } catch (InterruptedException ignored) {
                }
            }
            mRequest = request;
            mSession.notifyAll();

            while (mResponse == null && !chain.call().isCanceled()) {
                try {
                    mSession.wait();
                } catch (InterruptedException ignored) {
                }
                if (mPutSink != null && request.method().equals("PUT")) {
                    RequestBody body = request.body();
                    assert body != null;
                    BufferedSink bufferedSink = Okio.buffer(mPutSink);
                    body.writeTo(bufferedSink);
                    bufferedSink.flush();
                }
                mPutSink = null;
            }
            if (chain.call().isCanceled()) {
                throw new InterruptedIOException();
            }
            response = mResponse;
            mResponse = null;
        }
        return response;
    }

    public void assertPendingRequest(@NonNull Consumer<Request.Builder> requestBuilder) {
        Request.Builder builder = new Request.Builder();
        requestBuilder.accept(builder);
        Request request = builder.build();
        synchronized (mSession) {
            mRequest = request;
            mSession.notifyAll();
            while (request == mRequest) {
                try {
                    mSession.wait();
                } catch (InterruptedException ignored) {
                }
            }
            assertThat(request.method(), is(mRequest.method()));
            assertThat(request.url(), is(mRequest.url()));

            RequestBody expectedBody = request.body(), actualBody = mRequest.body();
            if (actualBody == null) {
                actualBody = RequestBody.create("", null);
            }
            if (expectedBody == null) {
                expectedBody = RequestBody.create("", null);
            }
            MediaType expectedContentType = expectedBody.contentType();
            MediaType actualContentType = actualBody.contentType();
            if (expectedContentType != null && actualContentType != null) {
                assertThat(expectedContentType.toString().toLowerCase(Locale.ROOT),
                        is(actualContentType.toString().toLowerCase(Locale.ROOT)));
            } else {
                assertThat(expectedBody.contentType(), is(actualBody.contentType()));
            }
            try {
                assertThat(expectedBody.contentLength(), is(actualBody.contentLength()));
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }
    }

    public void pingForCancel() {
        synchronized (mSession) {
            mSession.notifyAll();
        }
    }

    public <S extends Sink> S receiveFromPut(@NonNull S sink) {
        synchronized (mSession) {
            mPutSink = sink;
            mSession.notifyAll();
        }
        return sink;
    }

    public void mockResponse(@NonNull Consumer<Response.Builder> responseBuilder) {
        Response.Builder builder = new Response.Builder()
                .protocol(Protocol.HTTP_1_1)
                .message("")
                .body(EMPTY_RESPONSE)
                .request(mRequest);
        responseBuilder.accept(builder);
        synchronized (mSession) {
            mResponse = builder.build();
            mSession.notifyAll();
        }
    }
}
