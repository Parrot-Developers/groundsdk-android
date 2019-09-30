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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.PeripheralController;
import com.parrot.drone.groundsdk.device.peripheral.FlightLogDownloader;
import com.parrot.drone.groundsdk.internal.device.peripheral.FlightLogDownloaderCore;
import com.parrot.drone.groundsdk.internal.utility.FlightLogStorage;

import java.io.File;

/**
 * Base implementation of {@link FlightLogDownloader} controller.
 */
public abstract class FlightLogDownloadController extends PeripheralController<DeviceController<?>> {

    /** Flight log storage. */
    @NonNull
    final FlightLogStorage mStorage;

    /** FlightLogDownloaderCore peripheral for which this object is the backend. */
    @NonNull
    private final FlightLogDownloaderCore mDownloader;

    /** Downloaded flight logs count. */
    private int mDownloadedCount;

    /**
     * Constructor.
     *
     * @param deviceController the device controller that owns this peripheral controller
     * @param storage          flight log storage interface
     */
    FlightLogDownloadController(@NonNull DeviceController deviceController,
                                @NonNull FlightLogStorage storage) {
        super(deviceController);
        mStorage = storage;
        mDownloader = new FlightLogDownloaderCore(mComponentStore);
    }

    @Override
    protected void onConnected() {
        mDownloader.publish();
    }

    @Override
    protected void onDataSyncAllowanceChanged(boolean allowed) {
        if (allowed) {
            mDownloadedCount = 0;
            downloadFlightLogs();
        } else {
            cancelDownload();
        }
    }

    @Override
    protected void onDisconnected() {
        mDownloader.unpublish();
    }

    /**
     * Downloads flight logs from device.
     */
    abstract void downloadFlightLogs();

    /**
     * Stops ongoing flight logs download, if any.
     */
    abstract void cancelDownload();

    /**
     * Called back when some flight logs download starts.
     */
    final void onDownloadingFlightLog() {
        mDownloader.updateDownloadingFlag(true)
                   .updateCompletionStatus(FlightLogDownloader.CompletionStatus.NONE)
                   .updateDownloadedCount(mDownloadedCount)
                   .notifyUpdated();
    }

    /**
     * Called back when some flight log as been successfully downloaded.
     * <p>
     * This method modifies the controlled component state but does not notify it has changed; as a consequence,
     * concrete subclasses should ensure to always call either {@link #onDownloadEnd} or {@link
     * #onDownloadingFlightLog()} after this method.
     *
     * @param flightLog downloaded flight log
     */
    final void onDownloaded(@NonNull File flightLog) {
        mDownloader.updateDownloadedCount(++mDownloadedCount);
        mStorage.notifyFlightLogReady(flightLog);
    }

    /**
     * Called back when flight logs downloading ends.
     *
     * @param success {@code true} if all flight logs were downloaded successfully, otherwise {@code false}
     */
    final void onDownloadEnd(boolean success) {
        mDownloader.updateDownloadingFlag(false)
                   .updateCompletionStatus(success ? FlightLogDownloader.CompletionStatus.SUCCESS :
                           FlightLogDownloader.CompletionStatus.INTERRUPTED)
                   .notifyUpdated();
    }
}
