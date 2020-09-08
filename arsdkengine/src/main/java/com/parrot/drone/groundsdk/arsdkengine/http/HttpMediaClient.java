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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.parrot.drone.groundsdk.device.peripheral.MediaStore;
import com.parrot.drone.groundsdk.internal.http.HttpClient;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.HttpSession;
import com.parrot.drone.groundsdk.internal.http.ProgressCaptor;
import com.parrot.drone.groundsdk.internal.io.Files;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.groundsdk.internal.tasks.Task;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_HTTP;

/**
 * Client of drone media HTTP service.
 * <p>
 * Allows fetching the list of media/resources available on the drone, deleting media and downloading resources.
 */
public class HttpMediaClient extends HttpClient {

    /** Base URL for the drone media service endpoint. */
    private static final String MEDIA_ENDPOINT_BASE = "/api/v1/media/";

    /**
     * Size of chunk of downloaded data. When a media/resource is being downloaded, data is read from the network in
     * chunks of {@code CHUNK_SIZE} and written to the file system.
     */
    @VisibleForTesting // tests refer to this to test progress
    static final int CHUNK_SIZE = 8192; // we use the same size as Okio segments, for consistency

    /** An interface for receiving media events. */
    public interface Listener {

        /**
         * Called back when a media has been added on the drone.
         *
         * @param media added media item
         */
        void onMediaAdded(@NonNull HttpMediaItem media);

        /**
         * Called back when a media has been removed from the drone.
         *
         * @param mediaId identifier of the removed media
         */
        void onMediaRemoved(@NonNull String mediaId);

        /**
         * Called back when all media have been removed from the drone.
         */
        void onAllMediaRemoved();

        /**
         * Called back when a resource has been added on the drone.
         *
         * @param resource added resource
         */
        void onResourceAdded(@NonNull HttpMediaItem.Resource resource);

        /**
         * Called back when a resource has been removed from the drone.
         *
         * @param resourceId identifier of the removed resource
         */
        void onResourceRemoved(@NonNull String resourceId);

        /**
         * Called back when media indexing process state changes.
         *
         * @param state new indexing state
         */
        void onIndexingStateChanged(@NonNull HttpMediaIndexingState state);
    }

    /** HTTP session. */
    @NonNull
    private final HttpSession mSession;

    /** Implementation of media REST API. */
    @NonNull
    private final Service mService;

    /** Current subscription for media events. {@code null} if no {@link #setListener listener} is installed. */
    @Nullable
    private HttpSession.WebSocketSubscription mWebSocketSubscription;

    /** GSON instance used to parse received media items. */
    @NonNull
    private final Gson mGson;

    /**
     * Constructor.
     *
     * @param session HTTP session providing the HTTP client to use for requests
     */
    @SuppressWarnings("WeakerAccess") // Acceded by introspection
    public HttpMediaClient(@NonNull HttpSession session) {
        mSession = session;
        mGson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        mService = session.create(new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create(mGson)),
                Service.class);
    }

    @Override
    public void dispose() {
        setListener(null);
        super.dispose();
    }

    /**
     * Installs a media event listener.
     * <p>
     * The listener is notified whenever medias are added to or deleted from the drone.
     * <p>
     * Any installed listener is uninstalled when {@link #dispose()} is called.
     *
     * @param listener listener to install, or {@code null} to uninstall a previous listener
     */
    public void setListener(@Nullable Listener listener) {
        if (mWebSocketSubscription != null) {
            mWebSocketSubscription.unsubscribe();
        }
        if (listener != null) {
            mWebSocketSubscription = mSession.listenToWebSocket(MEDIA_ENDPOINT_BASE + "notifications", message -> {
                try {
                    HttpMediaEvent event = mGson.fromJson(message, HttpMediaEvent.class);
                    if (event != null && event.getType() != null) {
                        switch (event.getType()) {
                            case MEDIA_CREATED:
                                Executor.postOnMainThread(() -> listener.onMediaAdded(mGson.fromJson(
                                        message, HttpMediaEvent.MediaCreated.class).getMedia()));
                                break;
                            case MEDIA_REMOVED:
                                Executor.postOnMainThread(() -> listener.onMediaRemoved(mGson.fromJson(
                                        message, HttpMediaEvent.MediaDeleted.class).getId()));
                                break;
                            case ALL_MEDIA_REMOVED:
                                Executor.postOnMainThread(listener::onAllMediaRemoved);
                                break;
                            case RESOURCE_CREATED:
                                Executor.postOnMainThread(() -> listener.onResourceAdded(mGson.fromJson(
                                        message, HttpMediaEvent.ResourceCreated.class).getResource()));
                                break;
                            case RESOURCE_REMOVED:
                                Executor.postOnMainThread(() -> listener.onResourceRemoved(mGson.fromJson(
                                        message, HttpMediaEvent.ResourceDeleted.class).getId()));
                                break;
                            case INDEXING_STATE_CHANGED:
                                Executor.postOnMainThread(() -> listener.onIndexingStateChanged(mGson.fromJson(
                                        message, HttpMediaEvent.IndexingStateChanged.class).getCurrentState()));
                                break;
                        }
                    }
                } catch (JsonSyntaxException e) {
                    if (ULog.w(TAG_HTTP)) {
                        ULog.w(TAG_HTTP, "Failed to parse media event [message: " + message + "]", e);
                    }
                }
            });
        }
    }

    /**
     * Browse available medias of a specific storage.
     *
     * @param storageType   targeted storage type
     * @param callback      callback notified of request completion status and result (list of medias)
     *
     * @return the ongoing request, that can be canceled
     */
    @NonNull
    public HttpRequest browse(@Nullable MediaStore.StorageType storageType,
                              @NonNull HttpRequest.ResultCallback<List<HttpMediaItem>> callback) {
        Call<List<HttpMediaItem>> call = mService.list(convert(storageType));
        call.enqueue(new Callback<List<HttpMediaItem>>() {

            @Override
            public void onResponse(Call<List<HttpMediaItem>> call, Response<List<HttpMediaItem>> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code, response.body());
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to browse media list [code: " + code + "]");
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, code, null);
                }
            }

            @Override
            public void onFailure(Call<List<HttpMediaItem>> call, Throwable error) {
                if (call.isCanceled()) {
                    callback.onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to browse media list", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN, null);
                }
            }
        });
        return bookRequest(call::cancel);
    }

    /**
     * Deletes a media.
     *
     * @param mediaId  identifier of the media to delete
     * @param callback callback notified of request completion status
     *
     * @return the ongoing request, that can be canceled
     */
    @NonNull
    public HttpRequest deleteMedia(@NonNull String mediaId, @NonNull HttpRequest.StatusCallback callback) {
        Call<Void> call = mService.deleteMedia(mediaId);
        call.enqueue(new Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to delete media [id: " + mediaId + ", code: " + code + "]");
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, code);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable error) {
                if (call.isCanceled()) {
                    callback.onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to delete media [id: " + mediaId + "]", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN);
                }
            }
        });
        return bookRequest(call::cancel);
    }

    /**
     * Deletes a resource.
     *
     * @param resourceId identifier of the resource to delete
     * @param callback   callback notified of request completion status
     *
     * @return the ongoing request, that can be canceled
     */
    @NonNull
    public HttpRequest deleteResource(@NonNull String resourceId, @NonNull HttpRequest.StatusCallback callback) {
        Call<Void> call = mService.deleteResource(resourceId);
        call.enqueue(new Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to delete resource [id: " + resourceId + ", code: " + code + "]");
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, code);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable error) {
                if (call.isCanceled()) {
                    callback.onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to delete resource [id: " + resourceId + "]", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN);
                }
            }
        });
        return bookRequest(call::cancel);
    }

    /**
     * Deletes all media.
     *
     * @param callback callback notified of request completion status
     *
     * @return the ongoing request, that can be canceled
     */
    @NonNull
    public HttpRequest deleteAll(@NonNull HttpRequest.StatusCallback callback) {
        Call<Void> call = mService.deleteAll();
        call.enqueue(new Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to delete all media [code: " + code + "]");
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, code);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable error) {
                if (call.isCanceled()) {
                    callback.onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to delete all media", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN);
                }
            }
        });
        return bookRequest(call::cancel);
    }

    /**
     * Fetches a media/resource.
     * <p>
     * The callback result, if any, is a byte array containing fetched data. <br/>
     * Use {@link #fetch(String, Function, HttpRequest.ResultCallback)} instead if you need to transform the byte array
     * into a proper object.
     *
     * @param url      url of the media/resource to download
     * @param callback callback notified of request result (media/resource data) and completion status
     *
     * @return the ongoing request, that can be canceled
     */
    @NonNull
    public HttpRequest fetch(@NonNull String url, @NonNull HttpRequest.ResultCallback<byte[]> callback) {
        return fetch(url, Function.identity(), callback);
    }

    /**
     * Fetches a media/resource.
     * <p>
     * The given {@code transform} is applied on the same background that fetches the data. It may transform the request
     * binary data into a proper object.
     *
     * @param url       url of the media/resource to download
     * @param transform function to apply to the received binary data to transform it into the desired object
     * @param callback  callback notified of request result (media/resource data) and completion status
     * @param <T>       type of result object
     *
     * @return the ongoing request, that can be canceled
     *
     * @see #fetch(String, HttpRequest.ResultCallback)
     */
    @NonNull
    public <T> HttpRequest fetch(@NonNull String url, @NonNull Function<byte[], T> transform,
                                 @NonNull HttpRequest.ResultCallback<T> callback) {
        Call<ResponseBody> fetchCall = mService.fetch(url);
        Task<T> fetchTask = Executor.runInBackground(() -> {
            Response<ResponseBody> response = fetchCall.execute();
            if (fetchCall.isCanceled()) {
                // retrofit call.execute silently eats InterruptedException, so we rely on the call canceled flag
                // to restore the interruption status after the call
                throw new InterruptedException("Canceled retrofit call");
            }
            if (!response.isSuccessful()) {
                throw new HttpException(response.message(), response.code());
            }
            ResponseBody body = response.body();
            assert body != null;
            T result = transform.apply(body.bytes());
            body.close();
            return result;

        }).whenComplete((result, error, canceled) -> {
            if (error != null) {
                if (ULog.e(TAG_HTTP)) {
                    ULog.e(TAG_HTTP, "Fetch request failed [url:" + url + "]", error);
                }
                callback.onRequestComplete(HttpRequest.Status.FAILED, error instanceof HttpException ?
                        ((HttpException) error).getCode() : HttpRequest.STATUS_CODE_UNKNOWN, null);
            } else if (canceled) {
                callback.onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
            } else {
                callback.onRequestComplete(HttpRequest.Status.SUCCESS, 200, result);
            }
        });
        return bookRequest(() -> {
            fetchCall.cancel();
            fetchTask.cancel();
        });
    }

    /**
     * Downloads a media/resource.
     *
     * @param url      url of the media/resource to download
     * @param dest     file where to store the downloaded media/resource
     * @param callback callback notified of request progress and completion status
     *
     * @return the ongoing request, that can be canceled
     */
    @NonNull
    public HttpRequest download(@NonNull String url, @NonNull File dest,
                                @NonNull HttpRequest.ProgressStatusCallback callback) {
        Call<ResponseBody> downloadCall = mService.download(url);
        Task<Void> downloadTask = Executor.runInBackground((Callable<Void>) () -> {
            Response<ResponseBody> response = downloadCall.execute();
            if (downloadCall.isCanceled()) {
                // retrofit call.execute silently eats InterruptedException, so we rely on the call canceled flag
                // to restore the interruption status after the call
                throw new InterruptedException("Canceled retrofit call");
            }

            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                throw new HttpException(response.message(), response.code());
            }

            assert body != null;
            body = ProgressCaptor.captureOf(body,
                    percent -> Executor.postOnMainThread(() -> callback.onRequestProgress(percent)));
            try {
                Files.writeFile(body.byteStream(), dest, CHUNK_SIZE);
                long received = dest.length();
                long expected = body.contentLength();
                if (received != expected) {
                    throw new IOException("Received content mismatch [expected: " + expected
                                          + ", received: " + received + "]");
                }
                return null;
            } catch (IOException | InterruptedException e) {
                // ensure we cleanup the file before getting out of the background task
                if (dest.exists() && !dest.delete() && ULog.w(TAG_HTTP)) {
                    ULog.w(TAG_HTTP, "Could not clean up partially downloaded file: " + dest);
                }
                throw e;
            } finally {
                body.close();
            }
        }).whenComplete((result, error, canceled) -> {
            if (error != null) {
                if (ULog.e(TAG_HTTP)) {
                    ULog.e(TAG_HTTP, "Download request failed [url:" + url + ", dest: " + dest + "]", error);
                }
                callback.onRequestComplete(HttpRequest.Status.FAILED, error instanceof HttpException ?
                        ((HttpException) error).getCode() : HttpRequest.STATUS_CODE_UNKNOWN);
            } else if (canceled) {
                callback.onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);
            } else {
                callback.onRequestComplete(HttpRequest.Status.SUCCESS, 200);
            }
        });

        return bookRequest(() -> {
            downloadCall.cancel();
            downloadTask.cancel();
        });
    }

    @Nullable
    private static String convert(@Nullable MediaStore.StorageType storageType) {
        if (storageType == null) return null;
        switch (storageType) {
            case INTERNAL:
                return "internal";
            case REMOVABLE:
                return "sdcard";
        }
        return null;
    }

    /** REST API. */
    private interface Service {

        /**
         * Retrieves the list of medias on the drone.
         *
         * @param storageType storage type where to search. Optional, will search in every storage if {@code null}
         *
         * @return a retrofit call for sending the request out. The request returns a media list
         */
        @NonNull
        @GET(MEDIA_ENDPOINT_BASE + "medias")
        Call<List<HttpMediaItem>> list(@Nullable @Query("storage") String storageType);

        /**
         * Downloads a media/resource file from the drone.
         * <p>
         * This endpoint is intended to be used to download large files, that are dumped directly to the system storage
         * without being held in their totality in memory.
         *
         * @param url url of the media/resource to download
         *
         * @return a retrofit call for sending the request out. The received response body does not contain any data
         *         per-se but provides a connected input stream that can be read to receive data progressively
         */
        @GET
        @Streaming
        @NonNull
        Call<ResponseBody> download(@NonNull @Url String url);

        /**
         * Fetches a media/resource file from the drone.
         * <p>
         * This endpoint is intended to be used to small files, that are collected directly in memory, such as
         * thumbnails.
         *
         * @param url url of the media/resource to fetch
         *
         * @return a retrofit call for sending the request out. The received response body contains a byte array of the
         *         requested file data
         */
        @GET
        @NonNull
        Call<ResponseBody> fetch(@NonNull @Url String url);

        /**
         * Deletes a media.
         *
         * @param mediaId identifier of the media to delete
         *
         * @return a retrofit call for sending the request out
         */
        @NonNull
        @DELETE(MEDIA_ENDPOINT_BASE + "medias/{mediaId}")
        Call<Void> deleteMedia(@NonNull @Path("mediaId") String mediaId);

        /**
         * Deletes a resource.
         *
         * @param resourceId identifier of the resource to delete
         *
         * @return a retrofit call for sending the request out
         */
        @NonNull
        @DELETE(MEDIA_ENDPOINT_BASE + "resources/{resourceId}")
        Call<Void> deleteResource(@NonNull @Path("resourceId") String resourceId);

        /**
         * Deletes all media.
         *
         * @return a retrofit call for sending the request out
         */
        @NonNull
        @DELETE(MEDIA_ENDPOINT_BASE + "medias")
        Call<Void> deleteAll();
    }

}
