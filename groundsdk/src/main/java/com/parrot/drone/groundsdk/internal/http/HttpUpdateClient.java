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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.io.Files;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.groundsdk.internal.tasks.Task;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_HTTP;

/**
 * Client of HTTP update service.
 * <p>
 * Allows fetching information concerning available firmware updates, blacklisted firmwares, as well as to download
 * firmware update files.
 */
public class HttpUpdateClient extends HttpClient {

    /**
     * Size of chunk of downloaded data. When a firmware is being downloaded, data is read from the network in
     * chunks of {@code CHUNK_SIZE} and written to the file system.
     */
    private static final int CHUNK_SIZE = 8192; // we use the same size as Okio segments, for consistency

    /** Implementation of update REST API. */
    @NonNull
    private final Service mService;

    /**
     * Constructor.
     *
     * @param context application context
     */
    public HttpUpdateClient(@NonNull Context context) {
        String altUrl = GroundSdkConfig.get(context).getAlternateFirmwareServer();
        HttpSession session = altUrl == null ? HttpSession.appCentral(context) : HttpSession.custom(altUrl, context);
        mService = session.create(new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()),
                Service.class);
    }

    /**
     * Fetches remote available and blacklisted firmwares info.
     *
     * @param models   device models that this query must be restricted to
     * @param callback callback notified with the request result
     *
     * @return an HTTP request, that can be canceled
     */
    @NonNull
    public HttpRequest listAvailableFirmwares(@NonNull Set<DeviceModel> models,
                                              @NonNull HttpRequest.ResultCallback<HttpFirmwaresInfo> callback) {
        Call<HttpFirmwaresInfo> call = mService.list(models
                .stream().map(model -> String.format("%04x", model.id())).collect(Collectors.joining(",")));
        call.enqueue(new Callback<HttpFirmwaresInfo>() {

            @Override
            public void onResponse(Call<HttpFirmwaresInfo> call, Response<HttpFirmwaresInfo> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code, response.body());
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to get firmware list [models: " + models + ", code: " + code + "]");
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, code, null);
                }
            }

            @Override
            public void onFailure(Call<HttpFirmwaresInfo> call, Throwable error) {
                if (call.isCanceled()) {
                    callback.onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to get firmware list [models: " + models + "]", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN, null);
                }
            }
        });

        return bookRequest(call::cancel);
    }

    /**
     * Downloads a remote firmware update file.
     *
     * @param url      URL of the remote firmware update file to download
     * @param dest     destination file where to store the firmware file
     * @param callback callback notified of request progress and status
     *
     * @return an HTTP request, that can be canceled
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
                // TODO support resuming previous download instead
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

    /** REST API. */
    private interface Service {

        /**
         * Lists remotely available firmwares.
         * <p>
         * {@code products} parameter should be a string containing product identifiers, in hexadecimal, 4 characters
         * format,
         * separated by commas, without spaces. For example: "0901,090C".
         *
         * @param products products to retrieve firmwares for
         *
         * @return a retrofit call for sending the request out
         */
        @GET("apiv1/update")
        @NonNull
        Call<HttpFirmwaresInfo> list(@Nullable @Query("product") String products);

        /**
         * Downloads a remote firmware update file.
         *
         * @param url url of the remote firmware file to download
         *
         * @return a retrofit call for sending the request out
         */
        @GET
        @Streaming
        @NonNull
        Call<ResponseBody> download(@NonNull @Url String url);
    }
}