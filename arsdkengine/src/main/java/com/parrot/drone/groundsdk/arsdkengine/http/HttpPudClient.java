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
import com.parrot.drone.groundsdk.internal.io.IoStreams;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.groundsdk.internal.tasks.Task;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

/** Client of PUD HTTP service. */
public class HttpPudClient extends HttpClient {

    /**
     * Size of chunk of downloaded data. When a report is being downloaded, data is read from the network in chunks of
     * {@code CHUNK_SIZE} bytes and written to the file system.
     */
    private static final int CHUNK_SIZE = 8192; // we use the same size as Okio segments, for consistency

    /** Implementation of PUD REST API. */
    @NonNull
    private final ReportService mService;

    /**
     * Constructor.
     *
     * @param session HTTP session
     */
    @SuppressWarnings("WeakerAccess") // Accessed by introspection
    public HttpPudClient(@NonNull HttpSession session) {
        mService = session.create(new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()),
                ReportService.class);
    }

    /**
     * Lists available PUDs on device.
     *
     * @param callback listener for request completion
     *
     * @return a cancellable request
     */
    @NonNull
    public HttpRequest listPuds(@NonNull HttpRequest.ResultCallback<List<HttpPudInfo>> callback) {
        Call<List<HttpPudInfo>> listReportsCall = mService.getPuds();
        listReportsCall.enqueue(new Callback<List<HttpPudInfo>>() {

            @Override
            public void onResponse(Call<List<HttpPudInfo>> call, Response<List<HttpPudInfo>> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code, response.body());
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to get PUD list [code: " + code + "]");
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, code, null);
                }
            }

            @Override
            public void onFailure(Call<List<HttpPudInfo>> call, Throwable error) {
                if (call.isCanceled()) {
                    callback.onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN, null);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to get PUD list", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN, null);
                }
            }
        });
        return bookRequest(listReportsCall::cancel);
    }

    /** Allows to adapt received PUD to some custom format. */
    public interface PudAdapter {

        /**
         * Transfers PUD data while adapting it on-the-fly.
         *
         * @param input  input to read PUD data from
         * @param output output to write adapted content to
         *
         * @throws IOException          in case the operation fails for any reason
         * @throws InterruptedException in case the current thread was interrupted during the operation
         */
        void adapt(@NonNull InputStream input, @NonNull OutputStream output) throws IOException, InterruptedException;
    }

    /**
     * Downloads a PUD.
     * <p>
     * This method downloads to a temporary file named by post-fixing {@code dest} with {@code '.tmp'} that is renamed
     * to {@code dest} once the download is successful.
     * <p>
     * Received PUD data is written as-is to {@code dest}.
     *
     * @param url      relative url of the PUD, as returned by {@link HttpPudInfo#getUrl()}
     * @param dest     destination file of the downloaded PUD
     * @param callback listener for request completion
     *
     * @return a cancellable request
     *
     * @see #downloadPud(String, File, PudAdapter, HttpRequest.StatusCallback)
     */
    @NonNull
    public HttpRequest downloadPud(@NonNull String url, @NonNull File dest,
                                   @NonNull HttpRequest.StatusCallback callback) {
        return downloadPud(url, dest, (input, output) -> IoStreams.transfer(input, output, CHUNK_SIZE), callback);
    }

    /**
     * Downloads a PUD.
     * <p>
     * This method downloads to a temporary file named by post-fixing {@code dest} with {@code '.tmp'} that is renamed
     * to {@code dest} once the download is successful.
     * <p>
     * This method allows to adapt received PUD content to a custom format before writing to {@code dest}. Adapt process
     * occurs on a background thread.
     *
     * @param url      relative url of the PUD, as returned by {@link HttpPudInfo#getUrl()}
     * @param dest     destination file of the downloaded PUD
     * @param adapter  adapts received PUD content to some custom format
     * @param callback listener for request completion
     *
     * @return a cancellable request
     *
     * @see #deletePud(String, HttpRequest.StatusCallback)
     */
    @NonNull
    public HttpRequest downloadPud(@NonNull String url, @NonNull File dest,
                                   @NonNull PudAdapter adapter,
                                   @NonNull HttpRequest.StatusCallback callback) {
        Call<ResponseBody> downloadCall = mService.downloadPud(url);
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
                Files.makeDirectories(dest.getParentFile());
                File tmpDest = new File(dest.getAbsolutePath() + ".tmp");
                try (OutputStream output = new FileOutputStream(tmpDest)) {
                    adapter.adapt(body.byteStream(), output);
                    output.flush();
                }
                if (!tmpDest.renameTo(dest)) {
                    throw new IOException("Failed to rename PUD file [tmpDest: " + tmpDest
                                          + ", dest: " + dest + "]");
                }
                return null;
            } catch (IOException | InterruptedException e) {
                // ensure we cleanup the file before getting out of the background task
                if (dest.exists() && !dest.delete() && ULog.w(TAG_HTTP)) {
                    ULog.w(TAG_HTTP, "Could not clean up partially downloaded PUD: " + dest);
                }
                throw e;
            } finally {
                body.close();
            }
        }).whenComplete((result, error, canceled) -> {
            if (error != null) {
                if (ULog.e(TAG_HTTP)) {
                    ULog.e(TAG_HTTP, "PUD download request failed [url:" + url + ", dest: " + dest + "]", error);
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
     * Deletes a PUD.
     *
     * @param name     PUD name, as returned by {@link HttpPudInfo#getName()}
     * @param callback listener for request completion
     *
     * @return a cancellable request
     */
    @NonNull
    public HttpRequest deletePud(@NonNull String name, @NonNull HttpRequest.StatusCallback callback) {
        Call<Void> deleteReportCall = mService.deletePud(name);
        deleteReportCall.enqueue(new Callback<Void>() {

            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                int code = response.code();
                if (response.isSuccessful()) {
                    callback.onRequestComplete(HttpRequest.Status.SUCCESS, code);
                } else {
                    if (ULog.e(TAG_HTTP)) {
                        ULog.e(TAG_HTTP, "Failed to delete PUD [code: " + code + "]");
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
                        ULog.e(TAG_HTTP, "Failed to delete PUD", error);
                    }
                    callback.onRequestComplete(HttpRequest.Status.FAILED, HttpRequest.STATUS_CODE_UNKNOWN);
                }
            }
        });
        return bookRequest(deleteReportCall::cancel);
    }

    /** REST API. */
    private interface ReportService {

        /**
         * Retrieves the list of available PUDs on the device.
         *
         * @return report list
         */
        @GET("api/v1/pud/puds")
        Call<List<HttpPudInfo>> getPuds();

        /**
         * Downloads a PUD.
         *
         * @param url url of the PUD, as returned by {@link HttpPudInfo#getUrl()}
         *
         * @return a retrofit call with a response body containing report data
         */
        @Streaming
        @GET
        Call<ResponseBody> downloadPud(@Url String url);

        /**
         * Deletes a PUD.
         *
         * @param name name of the PUD, as returned by {@link HttpPudInfo#getName()}
         *
         * @return a retrofit call for the request
         */
        @DELETE("api/v1/pud/puds/{name}")
        Call<Void> deletePud(@Path("name") String name);
    }
}
