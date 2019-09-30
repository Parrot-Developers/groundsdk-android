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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.flightdata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpPudClient;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpPudInfo;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.PeripheralController;
import com.parrot.drone.groundsdk.device.peripheral.FlightDataDownloader;
import com.parrot.drone.groundsdk.internal.device.peripheral.FlightDataDownloaderCore;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.utility.FlightDataStorage;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

/** FlightDataDownloader peripheral controller for Anafi family drones. */
public final class AnafiFlightDataDownloader extends PeripheralController<DeviceController<?>> {

    /**
     * Creates a new {@code AnafiFlightDataDownloader} instance.
     *
     * @param controller device controller that owns this peripheral controller
     *
     * @return a new {@code AnafiFlightDataDownloader} instance if a {@link FlightDataStorage flight data storage
     *         utility} exists, otherwise {@code null}
     */
    @Nullable
    public static AnafiFlightDataDownloader create(@NonNull DeviceController controller) {
        FlightDataStorage storage = controller.getEngine().getUtility(FlightDataStorage.class);
        return storage == null ? null : new AnafiFlightDataDownloader(controller, storage);
    }

    /** Flight data storage. */
    @NonNull
    private final FlightDataStorage mStorage;

    /** FlightDataDownloaderCore peripheral for which this object is the backend. */
    @NonNull
    private final FlightDataDownloaderCore mDownloader;

    /** HTTP client used to access the device's PUDs. */
    @Nullable
    private HttpPudClient mHttpClient;

    /** Queue of PUD to be downloaded. Empty when not downloading or done. */
    private final Queue<HttpPudInfo> mPendingPuds;

    /** Current downloaded PUD files count. {@code 0} when not downloading. */
    private int mDownloadedCount;

    /**
     * Constructor.
     *
     * @param deviceController the device controller that owns this peripheral controller.
     * @param storage          flight data storage interface
     */
    private AnafiFlightDataDownloader(@NonNull DeviceController<?> deviceController,
                                      @NonNull FlightDataStorage storage) {
        super(deviceController);
        mStorage = storage;
        mDownloader = new FlightDataDownloaderCore(mComponentStore);
        mPendingPuds = new LinkedList<>();
    }

    @Override
    protected void onConnected() {
        mDownloader.publish();
    }

    @Override
    protected void onDisconnected() {
        mDownloader.unpublish();
    }

    @Override
    protected void onDataSyncAllowanceChanged(boolean allowed) {
        if (allowed) {
            mHttpClient = mDeviceController.getHttpClient(HttpPudClient.class);
            if (mHttpClient != null) {
                mPendingPuds.clear();
                mDownloadedCount = 0;
                mHttpClient.listPuds((status, code, puds) -> {
                    if (status != HttpRequest.Status.SUCCESS) {
                        return;
                    }
                    // validate received puds
                    if (puds != null) for (HttpPudInfo pud : puds) {
                        if (HttpPudInfo.isValid(pud)) {
                            mPendingPuds.add(pud);
                        }
                    }
                    if (!mPendingPuds.isEmpty()) {
                        mDownloader.updateDownloadingFlag(true)
                                   .updateCompletionStatus(FlightDataDownloader.CompletionStatus.NONE)
                                   .updateDownloadedCount(0)
                                   .notifyUpdated();
                        downloadNextPud();
                    }
                });
            }
        } else if (mHttpClient != null) {
            mHttpClient.dispose();
            mHttpClient = null;
        }
    }

    /**
     * Downloads the next available pud from the drone.
     */
    private void downloadNextPud() {
        assert mHttpClient != null;

        HttpPudInfo pud = mPendingPuds.poll();
        if (pud == null) {
            // all puds are downloaded, success
            mDownloader.updateDownloadingFlag(false)
                       .updateCompletionStatus(FlightDataDownloader.CompletionStatus.SUCCESS);
        } else {
            String url = pud.getUrl();
            String name = pud.getName();
            assert url != null && name != null;
            File dest = new File(mStorage.getWorkDir(), name);
            mHttpClient.downloadPud(url, dest, PudAdapter::adapt, (status, code) -> {
                if (status == HttpRequest.Status.CANCELED) {
                    mDownloader.updateDownloadingFlag(false)
                               .updateCompletionStatus(FlightDataDownloader.CompletionStatus.INTERRUPTED)
                               .notifyUpdated();
                } else {
                    if (status == HttpRequest.Status.SUCCESS) {
                        mDownloadedCount++;
                        mDownloader.updateDownloadedCount(mDownloadedCount);
                        mStorage.notifyFlightDataFileReady(dest);
                    }
                    // delete this pud
                    mHttpClient.deletePud(name, HttpRequest.StatusCallback.IGNORE);
                    // process next pud
                    downloadNextPud();
                }
            });
        }
        mDownloader.notifyUpdated();
    }
}
