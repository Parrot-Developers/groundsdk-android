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
import com.parrot.drone.groundsdk.internal.http.ProgressCaptor;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.InputStream;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.PUT;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_HTTP;

/**
 * Client of "update" device webservice.
 * <p>
 * Provides method {@link #uploadFirmware} to upload a firmware over HTTP.
 */
public class HttpUpdateClient extends HttpClient {

    /** Implementation of update REST API. */
    @NonNull
    private final Service mService;

    /**
     * Constructor.
     *
     * @param httpSession HTTP session
     */
    @SuppressWarnings("WeakerAccess") // Acceded by introspection
    public HttpUpdateClient(@NonNull HttpSession httpSession) {
        // Create an implementation of the update REST API
        mService = httpSession.create(Service.class);
    }

    /**
     * Upload a local firmware to the device.
     *
     * @param firmware firmware file to upload
     * @param callback callback notified of upload progress and completion status
     *
     * @return an HTTP request, that can be canceled
     */
    @NonNull
    public HttpRequest uploadFirmware(@NonNull InputStream firmware,
                                      @NonNull HttpRequest.ProgressStatusCallback callback) {
        Call<Void> uploadCall = mService.upload(ProgressCaptor.captureOf(InputStreamRequestBody.create(null, firmware),
                percent -> Executor.postOnMainThread(() -> callback.onRequestProgress(percent))));
        uploadCall.enqueue(new Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to upload firmware [file: " + firmware + ", code: " + code + "]");
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
                        ULog.e(TAG_HTTP, "Failed to upload firmware [file: " + firmware + "]", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN);
                }
            }
        });
        return bookRequest(uploadCall::cancel);
    }

    /** REST API. */
    private interface Service {

        /**
         * Uploads a firmware.
         *
         * @param file request body wrapping the firmware file to upload
         *
         * @return a retrofit call for sending the request out
         */
        @Headers("Request-Timeout: 120000")
        @PUT("api/v1/update/upload")
        @NonNull
        Call<Void> upload(@NonNull @Body RequestBody file);
    }
}
