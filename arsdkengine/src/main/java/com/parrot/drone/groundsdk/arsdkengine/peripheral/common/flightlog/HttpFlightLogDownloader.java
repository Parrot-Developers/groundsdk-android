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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common.flightlog;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpFdrClient;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpFdrInfo;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.utility.FlightLogStorage;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

/** Implementation of flight logs downloader over SdkCore for devices supporting flight log download over HTTP. */
public final class HttpFlightLogDownloader extends FlightLogDownloadController {

    /**
     * Creates a new {@code HttpFlightLogDownloader} instance.
     *
     * @param controller device controller that owns this peripheral controller
     *
     * @return a new {@code HttpFlightLogDownloader} instance if a {@link FlightLogStorage flight log storage utility}
     *         exists, otherwise {@code null}
     */
    @Nullable
    public static HttpFlightLogDownloader create(@NonNull DeviceController controller) {
        FlightLogStorage storage = controller.getEngine().getUtility(FlightLogStorage.class);
        return storage == null ? null : new HttpFlightLogDownloader(controller, storage);
    }

    /** HTTP flight data record client. */
    @Nullable
    private HttpFdrClient mHttpClient;

    /** Queue of flight logs to be downloaded. Empty when not downloading or done. */
    @NonNull
    private final Queue<HttpFdrInfo> mPendingFlightLogs;

    /**
     * Constructor.
     *
     * @param deviceController the device controller that owns this peripheral controller.
     * @param storage          flight log storage interface
     */
    private HttpFlightLogDownloader(@NonNull DeviceController deviceController,
                                    @NonNull FlightLogStorage storage) {
        super(deviceController, storage);
        mPendingFlightLogs = new LinkedList<>();
    }

    @Override
    protected void downloadFlightLogs() {
        mHttpClient = mDeviceController.getHttpClient(HttpFdrClient.class);
        if (mHttpClient != null) {
            mPendingFlightLogs.clear();
            mHttpClient.listRecords((status, code, records) -> {
                if (status != HttpRequest.Status.SUCCESS) {
                    return;
                }
                // validate received records
                if (records != null) for (HttpFdrInfo record : records) {
                    if (HttpFdrInfo.isValid(record)) {
                        mPendingFlightLogs.add(record);
                    }
                }
                if (!mPendingFlightLogs.isEmpty()) {
                    downloadNextFlightLog();
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
     * Downloads the next available flight log from the drone.
     */
    private void downloadNextFlightLog() {
        assert mHttpClient != null;

        HttpFdrInfo record = mPendingFlightLogs.poll();
        if (record == null) {
            // all flight logs downloaded, success
            onDownloadEnd(true);
        } else {
            String url = record.getUrl();
            String name = record.getName();
            assert url != null && name != null;

            File dest = new File(mStorage.getWorkDir(), name);
            mHttpClient.downloadRecord(url, dest, (status, code) -> {
                if (status == HttpRequest.Status.CANCELED) {
                    onDownloadEnd(false);
                } else {
                    if (status == HttpRequest.Status.SUCCESS) {
                        onDownloaded(dest);
                    }
                    // delete this record
                    mHttpClient.deleteRecord(name, HttpRequest.StatusCallback.IGNORE);
                    // process next record
                    downloadNextFlightLog();
                }
            });
            onDownloadingFlightLog();
        }
    }
}
