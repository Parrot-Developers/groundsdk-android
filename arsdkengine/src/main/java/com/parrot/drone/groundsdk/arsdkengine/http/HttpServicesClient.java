/*
 *     Copyright (C) 2020 Parrot Drones SAS
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
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_HTTP;

/**
 * Client of services availability device webservice.
 * <p>
 * Provides method {@link #listModules(HttpRequest.ResultCallback)} to upload a certificate over HTTP.
 */
public class HttpServicesClient extends HttpClient {

    /** Implementation of credential certificate REST API. */
    @NonNull
    private final Service mService;

    /**
     * Constructor.
     *
     * @param httpSession HTTP session
     */
    public HttpServicesClient(@NonNull HttpSession httpSession) {
        mService = httpSession.create(new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()),
                Service.class);
    }

    /**
     * Lists available modules/services from the device.
     *
     * @param callback callback notified of upload completion status
     *
     * @return an HTTP request, that can be canceled
     */
    @NonNull
    public HttpRequest listModules(@NonNull HttpRequest.ResultCallback<List<HttpService>> callback) {
        Call<List<HttpService>> uploadCall = mService.getModules();
        uploadCall.enqueue(new Callback<List<HttpService>>() {

            @Override
            public void onResponse(Call<List<HttpService>> call, Response<List<HttpService>> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code, response.body());
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to get active modules [code: " + code + "]");
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, code,  null);
                }
            }

            @Override
            public void onFailure(Call<List<HttpService>> call, Throwable error) {
                if (call.isCanceled()) {
                    callback.onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to get active modules", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN, null);
                }
            }
        });
        return bookRequest(uploadCall::cancel);
    }

    /** REST API. */
    private interface Service {

        /**
         * Get the available modules from the device.
         *
         * @return a retrofit call for sending the request out
         */
        @GET("api/v1/web/modules")
        @NonNull
        Call<List<HttpService>> getModules();
    }
}
