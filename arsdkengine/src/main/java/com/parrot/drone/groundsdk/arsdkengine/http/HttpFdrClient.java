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

import com.parrot.drone.groundsdk.internal.http.HttpClient;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.HttpSession;
import com.parrot.drone.groundsdk.internal.io.Files;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.groundsdk.internal.tasks.Task;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_HTTP;

/**
 * Client of device FDR HTTP service.
 * <p>
 * Provides methods to list, download and delete flight logs from a device.
 */
public class HttpFdrClient extends HttpClient {

    /**
     * Size of chunk of downloaded data. When a record is being downloaded, data is read from the network in chunks of
     * {@code CHUNK_SIZE} bytes and written to the file system.
     */
    private static final int CHUNK_SIZE = 8192; // we use the same size as Okio segments, for consistency

    /** Implementation of FDR REST API. */
    @NonNull
    private final FdrService mService;

    /**
     * Constructor.
     *
     * @param session HTTP session
     */
    @SuppressWarnings("WeakerAccess") // Acceded by introspection
    public HttpFdrClient(@NonNull HttpSession session) {
        mService = session.create(new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()),
                FdrService.class);
    }

    /**
     * Gets record list available on device.
     *
     * @param callback listener for request completion
     *
     * @return a cancellable request
     */
    @NonNull
    public HttpRequest listRecords(@NonNull HttpRequest.ResultCallback<List<HttpFdrInfo>> callback) {
        // create the request to get lite record list
        Call<List<HttpFdrInfo>> listRecordCall = mService.getLiteRecords();
        // execute the request asynchronously
        listRecordCall.enqueue(new Callback<List<HttpFdrInfo>>() {

            @Override
            public void onResponse(Call<List<HttpFdrInfo>> call,
                                   Response<List<HttpFdrInfo>> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code, response.body());
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to get record list [code: " + code + "]");
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, code, null);
                }
            }

            @Override
            public void onFailure(Call<List<HttpFdrInfo>> call, Throwable error) {
                if (call.isCanceled()) {
                    callback.onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to get record list", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN, null);
                }
            }
        });
        return bookRequest(listRecordCall::cancel);
    }

    /**
     * Downloads a record.
     *
     * @param url       relative url of the record as returned by {@link #listRecords}
     * @param dest      destination file of the downloaded record
     * @param callback  listener for request completion
     *
     * @return a cancellable request
     */
    @NonNull
    public HttpRequest downloadRecord(@NonNull String url, @NonNull File dest,
                                      @NonNull HttpRequest.StatusCallback callback) {
        // create the request to download the record
        Call<ResponseBody> downloadCall = mService.downloadLiteRecord(url);
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
            try {
                File tmpDest = new File(dest.getAbsolutePath() + ".tmp");
                Files.writeFile(body.byteStream(), tmpDest, CHUNK_SIZE);
                long received = tmpDest.length();
                long expected = body.contentLength();
                if (received != expected) {
                    throw new IOException("Received content mismatch [expected: " + expected
                                          + ", received: " + received + "]");
                }
                if (!tmpDest.renameTo(dest)) {
                    throw new IOException("Failed to rename record file [tmpDest: " + tmpDest
                                          + ", dest: " + dest + "]");
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

    /**
     * Deletes a record.
     *
     * @param name     record name as returned by {@link #listRecords}
     * @param callback listener for request completion
     *
     * @return a cancellable request
     */
    @NonNull
    public HttpRequest deleteRecord(@NonNull String name, @NonNull HttpRequest.StatusCallback callback) {
        // create the request to delete the record
        Call<Void> deleteRecordCall = mService.deleteLiteRecord(name);
        // execute the request asynchronously
        deleteRecordCall.enqueue(new Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to delete record [code: " + code + "]");
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
                        ULog.e(TAG_HTTP, "Failed to delete record", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN);
                }
            }
        });
        return bookRequest(deleteRecordCall::cancel);
    }

    /** REST API. */
    private interface FdrService {

        /**
         * Gets lite record list available on device.
         *
         * @return lite record list
         */
        @GET("api/v1/fdr/lite_records")
        Call<List<HttpFdrInfo>> getLiteRecords();

        /**
         * Downloads a lite record.
         *
         * @param url url of the lite record, as returned by {@link #getLiteRecords()}; see {@link HttpFdrInfo#getUrl()}
         *
         * @return a retrofit call with a response body containing record file
         */
        @Streaming
        @GET
        Call<ResponseBody> downloadLiteRecord(@Url String url);

        /**
         * Deletes a lite record.
         *
         * @param name name of the lite record, as returned by {@link #getLiteRecords()}; see
         *             {@link HttpFdrInfo#getName()}
         *
         * @return a retrofit call for the request
         */
        @DELETE("/api/v1/fdr/lite_records/{name}")
        Call<Void> deleteLiteRecord(@Path("name") String name);
    }
}
