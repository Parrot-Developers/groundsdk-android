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
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_HTTP;

/**
 * Client of Activation web service.
 * <p>
 * Provides method {@link #register} to register a device.
 */
public class HttpActivationClient extends HttpClient {

    /** Implementation of Activation REST API. */
    @NonNull
    private final Service mService;

    /**
     * Constructor.
     *
     * @param context application context
     */
    public HttpActivationClient(@NonNull Context context) {
        HttpSession session = HttpSession.appCentral(context, HttpHeader.appKey(context));
        mService = session.create(new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()),
                Service.class);
    }

    /**
     * Registers devices on the activation server.
     *
     * @param devices  devices to registers, map key is device uid and value is firmware version.
     * @param callback callback notified of completion status
     *
     * @return an HTTP request, that can be canceled
     */
    @NonNull
    public HttpRequest register(@NonNull Map<String, String> devices, @NonNull HttpRequest.StatusCallback callback) {
        HttpDevice[] httpDevices = new HttpDevice[devices.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : devices.entrySet()) {
            httpDevices[i++] = new HttpDevice(entry.getKey(), entry.getValue());
        }
        Call<Void> call = mService.register(httpDevices);
        call.enqueue(new Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to register device [devices: " + devices
                                         + ", code: " + code + "]");
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
                        ULog.e(TAG_HTTP, "Failed to register device [devices: " + devices + "] ", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN);
                }
            }
        });
        return bookRequest(call::cancel);
    }

    /**
     * A device, as sent to activation server.
     */
    @SuppressWarnings("unused")
    static class HttpDevice {

        /** Device uid. */
        @NonNull
        @SerializedName("serial")
        private final String mSerial;

        /** Device firmware version. */
        @NonNull
        @SerializedName("firmware")
        private final String mFirmware;

        /**
         * Constructor.
         *
         * @param serial   device uid
         * @param firmware device firmware version
         */
        HttpDevice(@NonNull String serial, @NonNull String firmware) {
            mSerial = serial;
            mFirmware = firmware;
        }
    }

    /** REST API. */
    private interface Service {

        /**
         * Registers a device.
         *
         * @param devices array of devices to register
         *
         * @return a retrofit call for sending the request out
         */
        @POST("apiv1/activation")
        @NonNull
        Call<Void> register(@NonNull @Body HttpDevice[] devices);
    }

    /**
     * Constructor for tests.
     *
     * @param session HTTP session
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    HttpActivationClient(@NonNull HttpSession session) {
        mService = session.create(new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()),
                Service.class);
    }
}
