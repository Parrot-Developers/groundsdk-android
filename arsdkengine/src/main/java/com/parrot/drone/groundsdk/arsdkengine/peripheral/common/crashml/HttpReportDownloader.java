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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common.crashml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpReportClient;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpReportInfo;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.utility.CrashReportStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/** Implementation of crash reports downloader over SdkCore for devices supporting report download over HTTP. */
public final class HttpReportDownloader extends ReportDownloadController {

    /**
     * Creates a new {@code HttpReportDownloader} instance.
     *
     * @param controller device controller that owns this peripheral controller
     *
     * @return a new {@code HttpReportDownloader} instance if a {@link CrashReportStorage report storage utility}
     *         exists, otherwise {@code null}
     */
    @Nullable
    public static HttpReportDownloader create(@NonNull DeviceController controller) {
        CrashReportStorage storage = controller.getEngine().getUtility(CrashReportStorage.class);
        return storage == null ? null : new HttpReportDownloader(controller, storage);
    }

    /** HTTP report client. */
    @Nullable
    private HttpReportClient mHttpClient;

    /** Queue of reports to be downloaded. Empty when not downloading or done. */
    @NonNull
    private final Queue<HttpReportInfo> mPendingReports;

    /**
     * Constructor.
     *
     * @param deviceController the device controller that owns this peripheral controller.
     * @param storage          crash report storage interface
     */
    private HttpReportDownloader(@NonNull DeviceController deviceController,
                                 @NonNull CrashReportStorage storage) {
        super(deviceController, storage);
        mPendingReports = new LinkedList<>();
    }

    @Override
    protected void downloadReports() {
        mHttpClient = mDeviceController.getHttpClient(HttpReportClient.class);
        if (mHttpClient != null) {
            mPendingReports.clear();
            mHttpClient.listReports((status, code, reports) -> {
                if (status != HttpRequest.Status.SUCCESS) {
                    return;
                }
                // validate received reports
                if (reports != null) for (HttpReportInfo report : reports) {
                    if (HttpReportInfo.isValid(report)) {
                        mPendingReports.add(report);
                    }
                }
                if (!mPendingReports.isEmpty()) {
                    downloadNextReport();
                }
            });
        }
    }

    @Override
    void cancelDownload() {
        if (mHttpClient != null) {
            mHttpClient.dispose();
            mHttpClient = null;
        }
    }

    /**
     * Downloads the next available report from the drone.
     */
    private void downloadNextReport() {
        assert mHttpClient != null;

        HttpReportInfo report = mPendingReports.poll();
        if (report == null) {
            // all reports downloaded, success
            onDownloadEnd(true);
        } else {
            String url = report.getUrl();
            String name = report.getName();
            assert url != null && name != null;

            // first download full report
            File fullReport = new File(mStorage.getWorkDir(), name);
            mHttpClient.downloadReport(url, fullReport, HttpReportClient.ReportType.FULL, (statusFull, codeFull) -> {
                if (statusFull == HttpRequest.Status.CANCELED) {
                    onDownloadEnd(false);
                } else {
                    // now proceed with light report
                    File liteReport = new File(mStorage.getWorkDir(), name + CrashReportStorage.ANONYMOUS_REPORT_EXT);
                    mHttpClient.downloadReport(url, liteReport,
                            HttpReportClient.ReportType.LIGHT, (statusLite, codeLite) -> {
                                if (statusLite == HttpRequest.Status.CANCELED) {
                                    onDownloadEnd(false);
                                } else {
                                    List<File> reports = new ArrayList<>();
                                    if (statusFull == HttpRequest.Status.SUCCESS) {
                                        reports.add(fullReport);
                                    }
                                    if (statusLite == HttpRequest.Status.SUCCESS) {
                                        reports.add(liteReport);
                                    }
                                    if (!reports.isEmpty()) {
                                        onDownloaded(reports);
                                    }

                                    // delete this report
                                    mHttpClient.deleteReport(name, HttpRequest.StatusCallback.IGNORE);

                                    // process next report
                                    downloadNextReport();
                                }
                            });
                }
            });
            onDownloadingReport();
        }
    }
}
