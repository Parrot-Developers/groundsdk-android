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
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.groundsdk.internal.tasks.Task;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.Callable;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_HTTP;

/**
 * Client of CrashML web service.
 * <p>
 * Provides method {@link #upload} to upload a report on CrashML server.
 */
public class HttpCrashMlClient extends HttpClient {

    /**
     * Allows to be notified of upload request completion.
     */
    public interface UploadCallback {

        /**
         * Termination status of an upload request.
         */
        enum Status {

            /** Upload completed successfully. */
            SUCCESS,

            /** Report is not well formed. Hence, it can be deleted. */
            BAD_REPORT,

            /** Server error. crash report should not be deleted because another try might succeed. */
            SERVER_ERROR,

            /**
             * Sent request is invalid.
             * <p>
             * Crash report can be deleted even though the file is not corrupted to avoid infinite retry.
             * This kind of error is a development error and should be fixed in the code.
             */
            BAD_REQUEST,

            /** Upload failed for an unknown reason. */
            UNKNOWN_ERROR,

            /** Upload has been canceled. Crash report should be kept in order to retry its upload later. */
            CANCELED
        }

        /**
         * Called back when the request completes.
         *
         * @param status upload request status
         */
        void onRequestComplete(@NonNull Status status);
    }

    /** Implementation of CrashML REST API. */
    @NonNull
    private final Service mService;

    /**
     * Constructor.
     *
     * @param context application context
     */
    @SuppressWarnings("WeakerAccess") // Acceded by introspection
    public HttpCrashMlClient(@NonNull Context context) {
        mService = HttpSession.appCentral(context, HttpHeader.appKey(context)).create(Service.class);
    }

    /**
     * Uploads a report on CrashML server.
     *
     * @param report   report file to upload
     * @param account  user account identifier, may be {@code null}
     * @param callback callback notified of completion status
     *
     * @return an HTTP request, that can be canceled
     */
    @NonNull
    public HttpRequest upload(@NonNull File report, @Nullable String account,
                              @NonNull UploadCallback callback) {
        Call<Void> uploadCall = mService.upload(RequestBody.create(report, MEDIA_TYPE_APPLICATION_GZIP), account);
        Task<Void> uploadTask = Executor.runInBackground((Callable<Void>) () -> {
            // check file existence to prevent infinite retry in okhttp
            // this could be removed when okhttp > 3.12.0 is used
            if (!report.exists()) {
                throw new FileNotFoundException();
            }
            Response<Void> response = uploadCall.execute();
            if (uploadCall.isCanceled()) {
                // retrofit call.execute silently eats InterruptedException, so we rely on the call canceled flag
                // to restore the interruption status after the call
                throw new InterruptedException("Canceled retrofit call");
            }

            if (!response.isSuccessful()) {
                throw new HttpException(response.message(), response.code());
            }
            return null;
        }).whenComplete((result, error, canceled) -> {
            if (error != null) {
                if (ULog.e(TAG_HTTP)) {
                    ULog.e(TAG_HTTP, "Upload report failed [report:" + report + "]", error);
                }
                UploadCallback.Status uploadStatus;
                if (error instanceof HttpException) {
                    int code = ((HttpException) error).getCode();
                    switch (code) {
                        case 400: // bad request
                        case 403: // bad api call
                            uploadStatus = UploadCallback.Status.BAD_REQUEST;
                            break;
                        case 429: // too many requests
                            uploadStatus = UploadCallback.Status.SERVER_ERROR;
                            break;
                        default:
                            if (code >= 500) { // server error
                                uploadStatus = UploadCallback.Status.SERVER_ERROR;
                            } else {
                                // by default, blame the error on the report in order to delete it
                                uploadStatus = UploadCallback.Status.BAD_REPORT;
                            }
                            break;
                    }
                } else if (error instanceof FileNotFoundException) {
                    uploadStatus = UploadCallback.Status.BAD_REPORT;
                } else {
                    uploadStatus = UploadCallback.Status.UNKNOWN_ERROR;
                }
                callback.onRequestComplete(uploadStatus);
            } else if (canceled) {
                callback.onRequestComplete(UploadCallback.Status.CANCELED);
            } else {
                callback.onRequestComplete(UploadCallback.Status.SUCCESS);
            }
        });

        return bookRequest(() -> {
            uploadCall.cancel();
            uploadTask.cancel();
        });
    }

    /** REST API. */
    private interface Service {

        /**
         * Uploads a report.
         *
         * @param file    request body wrapping the report file to upload
         * @param account user account identifier, may be {@code null}
         *
         * @return a retrofit call for sending the request out
         */
        @POST("apiv1/crashml")
        @NonNull
        Call<Void> upload(@NonNull @Body RequestBody file, @Nullable @Header(HttpHeader.ACCOUNT) String account);
    }

    /**
     * Constructor for tests.
     *
     * @param session HTTP session
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    HttpCrashMlClient(@NonNull HttpSession session) {
        mService = session.create(Service.class);
    }
}
