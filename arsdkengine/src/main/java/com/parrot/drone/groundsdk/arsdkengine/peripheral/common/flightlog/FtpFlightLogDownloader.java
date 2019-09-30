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
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.internal.utility.FlightLogStorage;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkRequest;
import com.parrot.drone.sdkcore.arsdk.flightlog.ArsdkFlightLogDownloadRequest;

import java.io.File;

/**
 * Implementation of flight logs downloader over SdkCore for devices supporting flight log download over FTP.
 */
public final class FtpFlightLogDownloader extends FlightLogDownloadController {

    /**
     * Creates a new {@code FtpFlightLogDownloader} instance.
     *
     * @param controller device controller that owns this peripheral controller
     *
     * @return a new {@code FtpFlightLogDownloader} instance if a {@link FlightLogStorage flight log storage utility}
     *         exists, otherwise {@code null}
     */
    @Nullable
    public static FtpFlightLogDownloader create(@NonNull DeviceController controller) {
        FlightLogStorage storage = controller.getEngine().getUtility(FlightLogStorage.class);
        return storage == null ? null : new FtpFlightLogDownloader(controller, storage);
    }

    /** Current download request, {@code null} if not downloading. */
    @Nullable
    private ArsdkRequest mCurrentRequest;

    /**
     * Constructor.
     *
     * @param deviceController the device controller that owns this peripheral controller.
     * @param storage          flight log storage interface
     */
    private FtpFlightLogDownloader(@NonNull DeviceController deviceController,
                                   @NonNull FlightLogStorage storage) {
        super(deviceController, storage);
    }

    @Override
    void downloadFlightLogs() {
        mCurrentRequest = mDeviceController.downloadFlightLog(mStorage.getWorkDir(),
                new ArsdkFlightLogDownloadRequest.Listener() {

                    @Override
                    public void onFlightLogDownloaded(@NonNull File flightLog) {
                        onDownloaded(flightLog);
                        onDownloadingFlightLog();
                    }

                    @Override
                    public void onRequestComplete(@ArsdkFlightLogDownloadRequest.Status int status) {
                        mCurrentRequest = null;
                        onDownloadEnd(status == ArsdkFlightLogDownloadRequest.STATUS_OK);
                    }
                });

        if (mCurrentRequest != null) {
            onDownloadingFlightLog();
        }
    }

    @Override
    void cancelDownload() {
        if (mCurrentRequest != null) {
            mCurrentRequest.cancel();
            mCurrentRequest = null;
        }
    }
}
