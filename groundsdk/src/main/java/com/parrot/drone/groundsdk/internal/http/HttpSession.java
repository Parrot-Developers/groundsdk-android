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

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.BuildConfig;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_HTTP;

/**
 * A HTTP session.
 */
public class HttpSession {

    /**
     * Obtains a new HTTP session for communicating with parrot 'appcentral' server.
     * <p>
     * The returned session automatically injects the default user agent HTTP header in all outgoing requests.
     *
     * @param context           application context
     * @param additionalHeaders additional HTTP headers to inject in all outgoing HTTP requests
     *
     * @return a new HTTP session
     */
    @NonNull
    static HttpSession appCentral(@NonNull Context context, @NonNull HttpHeader... additionalHeaders) {
        List<HttpHeader> headers = new ArrayList<>();
        headers.add(HttpHeader.defaultUserAgent(context));
        Collections.addAll(headers, additionalHeaders);
        return new HttpSession("https://appcentral.parrot.com", null, headers);
    }

    /**
     * Obtains a new HTTP session for communicating with a custom server.
     * <p>
     * The returned session automatically injects the default user agent HTTP header in all outgoing requests.
     *
     * @param url               server URL
     * @param context           application context
     * @param additionalHeaders additional HTTP headers to inject in all outgoing HTTP requests
     *
     * @return a new HTTP session
     */
    @NonNull
    public static HttpSession custom(@NonNull String url,
                                     @NonNull Context context,
                                     @NonNull HttpHeader... additionalHeaders) {
        List<HttpHeader> headers = new ArrayList<>();
        headers.add(HttpHeader.defaultUserAgent(context));
        Collections.addAll(headers, additionalHeaders);
        return new HttpSession(url, null, headers);
    }

    /** Represents a live subscription to an event web socket. */
    public interface WebSocketSubscription {

        /** An interface to receive event messages from a subscribed web socket. */
        interface MessageListener {

            /**
             * Called back when an event message has been received from the web socket.
             *
             * @param message received message
             */
            void onMessage(@NonNull String message);
        }

        /**
         * Revokes the subscription to the web socket.
         */
        void unsubscribe();
    }

    /** Read and write timeout in seconds. */
    private static final long TIMEOUT = 30;

    /** HTTP client. */
    @NonNull
    private final OkHttpClient mHttpClient;

    /** Base URL of drone HTTP services. */
    @NonNull
    private final String mBaseUrl;

    /**
     * Constructor.
     *
     * @param address       address of proxy with device
     * @param port          port of proxy with device
     * @param socketFactory factory for creating network bound sockets for this session, may be {@code null}
     */
    public HttpSession(@NonNull String address, int port, @Nullable SocketFactory socketFactory) {
        this("http://" + address + ":" + port, socketFactory, Collections.emptyList());
    }

    /**
     * Private constructor.
     *
     * @param baseUrl           base URL
     * @param socketFactory     factory for creating network bound sockets for this session, may be {@code null}
     * @param additionalHeaders additional HTTP headers to inject in all outgoing HTTP requests, may be empty
     */
    private HttpSession(@NonNull String baseUrl, @Nullable SocketFactory socketFactory,
                        @NonNull Collection<HttpHeader> additionalHeaders) {
        mBaseUrl = baseUrl;
        // Setup OkHttp
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .addNetworkInterceptor(RequestBodyInterceptor.INSTANCE)
                .addInterceptor(TIMEOUT_INTERCEPTOR);

        if (socketFactory != null) {
            builder.socketFactory(socketFactory);
        }

        if (!additionalHeaders.isEmpty()) {
            builder.addInterceptor((chain) -> {
                Request original = chain.request();
                Request.Builder request = original.newBuilder();
                for (HttpHeader httpHeader : additionalHeaders) {
                    request.header(httpHeader.getHeader(), httpHeader.getValue());
                }
                request.method(original.method(), original.body());
                return chain.proceed(request.build());
            });
        }

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                    (message -> ULog.d(TAG_HTTP, message)));
            loggingInterceptor.level(HttpLoggingInterceptor.Level.HEADERS);
            builder.addInterceptor(loggingInterceptor);
        }

        mHttpClient = builder.build();
    }

    /**
     * Disposes the session.
     * <p>
     * This forcefully aborts all ongoing HTTP requests.
     */
    public void dispose() {
        mHttpClient.dispatcher().cancelAll();
    }

    /**
     * Creates a {@link HttpClient}.
     *
     * @param clientType class of {@code HttpClient} to create
     * @param <H>        type of {@code HttpClient} class
     *
     * @return a new instance of the required {@code HttpClient} type, if available, otherwise {@code null}
     */
    @Nullable
    public final <H extends HttpClient> H client(@NonNull Class<H> clientType) {
        return FACTORY.create(this, clientType);
    }

    /**
     * Starts listening to an event web socket.
     * <p>
     * {@code listener} is called back on a <strong>background</strong> thread.
     *
     * @param endpoint url of the web socket endpoint to connect to
     * @param listener listener notified of event messages from the web socket
     *
     * @return a live subscription to the web socket, that can be revoked when not needed anymore
     */
    @NonNull
    public WebSocketSubscription listenToWebSocket(@NonNull String endpoint,
                                                   @NonNull WebSocketSubscription.MessageListener listener) {
        return mHttpClient.newWebSocket(new Request.Builder().url(mBaseUrl + endpoint).build(),
                new WebSocketListener() {

                    @Override
                    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                        listener.onMessage(text);
                    }
                })::cancel;
    }

    /**
     * Creates, with Retrofit, an implementation of the API endpoints defined by the {@code service} interface.
     *
     * @param retrofitBuilder builder used to customize retrofit before creating the endpoint implementation
     * @param service         interface defining a REST API with Retrofit annotations.
     * @param <S>             type of service class
     *
     * @return an implementation of the given API
     */
    @NonNull
    public <S> S create(@NonNull Retrofit.Builder retrofitBuilder, @NonNull Class<S> service) {
        return retrofitBuilder.baseUrl(mBaseUrl).client(mHttpClient).callbackExecutor(Executor::postOnMainThread)
                              .build().create(service);
    }

    /**
     * Creates, with Retrofit, an implementation of the API endpoints defined by the {@code service} interface.
     *
     * @param service interface defining a REST API with Retrofit annotations.
     * @param <S>     type of service class
     *
     * @return an implementation of the given API
     */
    @NonNull
    public <S> S create(@NonNull Class<S> service) {
        return create(new Retrofit.Builder(), service);
    }

    /**
     * OkHttp interceptor used to customize read and write timeouts for a request.
     * <p>
     * It parses the custom timeout defined with the {@code @Headers} annotation and with the name "Request-Timeout".
     * <p>
     * For instance, to set a 60s timeout, add {@code @Headers("Request-Timeout: 60000")} to the REST API definition.
     */
    private static final Interceptor TIMEOUT_INTERCEPTOR = new Interceptor() {

        /** Name of header defining the custom timeout. */
        private static final String HEADER_NAME_TIMEOUT = "Request-Timeout";

        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Response response;
            Request request = chain.request();
            int timeout = 0;
            String timeoutHeader = request.header(HEADER_NAME_TIMEOUT);
            if (!TextUtils.isEmpty(timeoutHeader)) {
                try {
                    assert timeoutHeader != null; // since not empty
                    timeout = Integer.parseInt(timeoutHeader);
                } catch (NumberFormatException ignored) {
                    // Invalid timeout format
                }
            }
            if (timeout > 0) {
                Request newRequest = request.newBuilder().removeHeader(HEADER_NAME_TIMEOUT).build();
                response = chain.withWriteTimeout(timeout, TimeUnit.MILLISECONDS)
                                .withReadTimeout(timeout, TimeUnit.MILLISECONDS)
                                .proceed(newRequest);
            } else {
                response = chain.proceed(request);
            }
            return response;
        }
    };

    /**
     * {@link HttpClient} factory.
     * <p>
     * Used to provide mock {@code HttpClient} in tests.
     */
    @VisibleForTesting
    public interface ClientFactory {

        /**
         * Creates a {@link HttpClient}.
         *
         * @param session    HTTP session for the client
         * @param clientType class of HTTP client to create
         * @param <H>        type of HTTP client class
         *
         * @return a new instance of the required {@code HttpClient} type, if exists, otherwise {@code null}
         */
        @Nullable
        <H extends HttpClient> H create(@NonNull HttpSession session, @NonNull Class<H> clientType);
    }

    /** Default HTTP client factory. May be overridden in tests. */
    @NonNull
    @VisibleForTesting
    @SuppressWarnings("NonConstantFieldWithUpperCaseName")
    static ClientFactory FACTORY = new ClientFactory() {

        @NonNull
        @Override
        public <H extends HttpClient> H create(@NonNull HttpSession session, @NonNull Class<H> clientType) {
            try {
                return clientType.getConstructor(HttpSession.class).newInstance(session);
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
                    | InvocationTargetException e) {
                throw new IllegalArgumentException("Unsupported HTTP client type: " + clientType.getCanonicalName(), e);
            }
        }
    };

    /**
     * Constructor for test mocks.
     *
     * @param interceptor interceptor used to mock web services, added last in the chain of interceptors
     */
    @VisibleForTesting
    public HttpSession(@NonNull Interceptor interceptor) {
        mBaseUrl = "http://test";
        mHttpClient = new OkHttpClient.Builder()
                .addInterceptor(RequestBodyInterceptor.INSTANCE)
                .addInterceptor(interceptor)
                .build();
    }
}

