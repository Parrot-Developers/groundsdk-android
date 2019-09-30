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
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_HTTP;

/** Client of device report HTTP service. */
public class HttpReportClient extends HttpClient {

    /** Type of crash report. */
    public enum ReportType {

        /** Anonymous report, does not contain any user-related information. */
        LIGHT,

        /** Full report, may contain user-related information. */
        FULL
    }

    /**
     * Size of chunk of downloaded data. When a report is being downloaded, data is read from the network in chunks of
     * {@code CHUNK_SIZE} bytes and written to the file system.
     */
    private static final int CHUNK_SIZE = 8192; // we use the same size as Okio segments, for consistency

    /** Implementation of report REST API. */
    @NonNull
    private final ReportService mService;

    /**
     * Constructor.
     *
     * @param session HTTP session
     */
    @SuppressWarnings("WeakerAccess") // Acceded by introspection
    public HttpReportClient(@NonNull HttpSession session) {
        mService = session.create(new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()),
                ReportService.class);
    }

    /**
     * Gets report list available on device.
     *
     * @param callback listener for request completion
     *
     * @return a cancellable request
     */
    @NonNull
    public HttpRequest listReports(@NonNull HttpRequest.ResultCallback<List<HttpReportInfo>> callback) {
        // create the request to get report list
        Call<List<HttpReportInfo>> listReportsCall = mService.getReports();
        // execute the request asynchronously
        listReportsCall.enqueue(new Callback<List<HttpReportInfo>>() {

            @Override
            public void onResponse(Call<List<HttpReportInfo>> call, Response<List<HttpReportInfo>> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code, response.body());
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to get report list [code: " + code + "]");
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, code, null);
                }
            }

            @Override
            public void onFailure(Call<List<HttpReportInfo>> call, Throwable error) {
                if (call.isCanceled()) {
                    callback.onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to get report list", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN, null);
                }
            }
        });
        return bookRequest(listReportsCall::cancel);
    }

    /**
     * Downloads a report.
     *
     * @param url      relative url of the report as returned by {@link #listReports}
     * @param dest     destination file of the downloaded report
     * @param type     type of report to download, {@code null} for default server report type ({@link
     *                 ReportType#LIGHT})
     * @param callback listener for request completion
     *
     * @return a cancellable request
     */
    @NonNull
    public HttpRequest downloadReport(@NonNull String url, @NonNull File dest, @Nullable ReportType type,
                                      @NonNull HttpRequest.StatusCallback callback) {
        // create the request to download the report
        Call<ResponseBody> downloadCall = mService.downloadReport(url, toHttpAnonymousFlag(type));
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
                    throw new IOException("Failed to rename report file [tmpDest: " + tmpDest
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
     * Deletes a report.
     *
     * @param name     report name as returned by {@link #listReports}
     * @param callback listener for request completion
     *
     * @return a cancellable request
     */
    @NonNull
    public HttpRequest deleteReport(@NonNull String name, @NonNull HttpRequest.StatusCallback callback) {
        // create the request to delete the report
        Call<Void> deleteReportCall = mService.deleteReport(name);
        // execute the request asynchronously
        deleteReportCall.enqueue(new Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to delete report [code: " + code + "]");
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
                        ULog.e(TAG_HTTP, "Failed to delete report", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN);
                }
            }
        });
        return bookRequest(deleteReportCall::cancel);
    }

    /**
     * Obtains the HTTP anonymous flag to be used to download a report of a given type.
     *
     * @param type report type
     *
     * @return anonymous flag to use or {@code null} (anonymous report) if {@code type} is {@code null} or unknown
     */
    @Nullable
    private static String toHttpAnonymousFlag(@Nullable ReportType type) {
        if (type != null) switch (type) {
            case LIGHT:
                return "yes";
            case FULL:
                return "no";
        }
        return null;
    }

    /** REST API. */
    private interface ReportService {

        /**
         * Gets report list available on device.
         *
         * @return report list
         */
        @GET("api/v1/report/reports")
        Call<List<HttpReportInfo>> getReports();

        /**
         * Downloads a report.
         *
         * @param url       url of the report, as returned by {@link #getReports()}; see {@link HttpReportInfo#getUrl()}
         * @param anonymous whether an anonymous report should be downloaded (values is {@code "no"} for a full report,
         *                  otherwise {@code null} or {@code "yes"} for an anonymous report
         *
         * @return a retrofit call with a response body containing report data
         */
        @Streaming
        @GET
        Call<ResponseBody> downloadReport(@Url String url, @Nullable @Query("anonymous") String anonymous);

        /**
         * Deletes a report.
         *
         * @param name name of the report, as returned by {@link #getReports()}; see {@link HttpReportInfo#getName()}
         *
         * @return a retrofit call for the request
         */
        @DELETE("/api/v1/report/reports/{name}")
        Call<Void> deleteReport(@Path("name") String name);
    }
}
