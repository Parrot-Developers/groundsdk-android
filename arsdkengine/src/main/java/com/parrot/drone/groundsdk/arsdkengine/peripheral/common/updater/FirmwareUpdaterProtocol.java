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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common.updater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpUpdateClient;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.internal.Cancelable;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.tasks.Task;
import com.parrot.drone.groundsdk.internal.utility.FirmwareStore;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkRequest;
import com.parrot.drone.sdkcore.arsdk.firmware.ArsdkFirmwareUploadRequest;

import java.io.File;
import java.io.InputStream;

/**
 * Abstract firmware updater protocol.
 */
public abstract class FirmwareUpdaterProtocol {

    /** Device controller that manages the updater protocol. */
    @NonNull
    final DeviceController<?> mController;

    /**
     * Constructor.
     *
     * @param controller device controller that manages this protocol
     */
    private FirmwareUpdaterProtocol(@NonNull DeviceController<?> controller) {
        mController = controller;
    }

    /**
     * Requests a firmware update of the device.
     *
     * @param firmware identifies the firmware to update the device to
     * @param store    store from where the firmware file can be obtained
     * @param callback callback notified when the update operation state changes
     *
     * @return a cancelable that allows to cancel the update request, if the update did properly start, otherwise {@code
     *         null}, in which case the update failed immediately and the callback has been notified with the
     *         corresponding status
     */
    @Nullable
    public Cancelable updateWith(@NonNull FirmwareIdentifier firmware, @NonNull FirmwareStore store,
                                 @NonNull Callback callback) {
        if (firmware.getDeviceModel() == mController.getDevice().getModel()) {
            return doUpdate(firmware, store, callback);
        }
        callback.onUpdateEnd(Callback.Status.FAILED);
        return null;
    }

    /**
     * Requests a firmware update of the device.
     * <p>
     * Concrete protocol implementation must override this method to implement the protocol-dependent update operation
     *
     * @param firmware identifies the firmware to update the device to
     * @param store    store from where the firmware file can be obtained
     * @param callback callback notified when the update operation state changes
     *
     * @return a cancelable that allows to cancel the update request, if the update did properly start, otherwise {@code
     *         null}, in which case the update failed immediately and the callback has been notified with the
     *         corresponding status
     */
    @Nullable
    abstract Cancelable doUpdate(@NonNull FirmwareIdentifier firmware, @NonNull FirmwareStore store,
                                 @NonNull Callback callback);

    /**
     * Firmware update callbacks.
     */
    interface Callback {

        /**
         * Called back when the firmware upload progress updates.
         *
         * @param progress current upload progress
         */
        void onUploadProgress(int progress);

        /**
         * Called back when the update operation ends.
         *
         * @param status final status
         */
        void onUpdateEnd(@NonNull Status status);

        /**
         * Update completion status.
         */
        enum Status {

            /** Update was successful. At this point, the device may reboot to install the update. */
            SUCCESS,

            /** Update failed for some reason. */
            FAILED,

            /** Update was canceled. */
            CANCELED
        }
    }

    /**
     * HTTP firmware updater protocol.
     * <p>
     * This implementation should be used by controllers whose device provide an {@link HttpUpdateClient HTTP update
     * service}.
     */
    public static final class Http extends FirmwareUpdaterProtocol {

        /**
         * Constructor.
         *
         * @param controller device controller that manages this protocol
         */
        public Http(@NonNull DeviceController<?> controller) {
            super(controller);
        }

        @Override
        @Nullable
        Cancelable doUpdate(@NonNull FirmwareIdentifier firmware, @NonNull FirmwareStore store,
                            @NonNull Callback callback) {
            HttpUpdateClient client;
            InputStream firmwareStream;
            if ((client = mController.getHttpClient(HttpUpdateClient.class)) == null
                || (firmwareStream = store.getFirmwareStream(firmware)) == null) {
                callback.onUpdateEnd(Callback.Status.FAILED);
                return null;
            }

            return client.uploadFirmware(firmwareStream, new HttpRequest.ProgressStatusCallback() {

                @Override
                public void onRequestProgress(int progress) {
                    callback.onUploadProgress(progress);
                }

                @Override
                public void onRequestComplete(@NonNull HttpRequest.Status status, int code) {
                    switch (status) {
                        case SUCCESS:
                            callback.onUpdateEnd(Callback.Status.SUCCESS);
                            break;
                        case FAILED:
                            callback.onUpdateEnd(Callback.Status.FAILED);
                            break;
                        case CANCELED:
                            callback.onUpdateEnd(Callback.Status.CANCELED);
                            break;
                    }
                }
            });
        }
    }

    /**
     * FTP firmware updater protocol.
     * <p>
     * This implementation should be used by controllers whose device only supports legacy
     * {@link ArsdkFirmwareUploadRequest arsdk-ng FTP update service}.
     */
    public static final class Ftp extends FirmwareUpdaterProtocol {

        /**
         * Constructor.
         *
         * @param controller device controller that manages this protocol
         */
        public Ftp(@NonNull DeviceController<?> controller) {
            super(controller);
        }

        @Nullable
        @Override
        Cancelable doUpdate(@NonNull FirmwareIdentifier firmware, @NonNull FirmwareStore store,
                            @NonNull Callback callback) {
            Task<File> fileRequest = store.getFirmwareFile(firmware);
            ArsdkRequest[] uploadRequest = {null};
            fileRequest.whenComplete((firmwareFile, error, canceled) -> {
                if (firmwareFile != null) {
                    uploadRequest[0] = mController.updateFirmware(firmwareFile,
                            new ArsdkFirmwareUploadRequest.Listener() {

                                @Override
                                public void onRequestProgress(float progress) {
                                    callback.onUploadProgress(Math.round(progress));
                                }

                                @Override
                                public void onRequestComplete(@ArsdkFirmwareUploadRequest.Status int status) {
                                    switch (status) {
                                        case ArsdkFirmwareUploadRequest.STATUS_OK:
                                            callback.onUpdateEnd(Callback.Status.SUCCESS);
                                            break;
                                        case ArsdkFirmwareUploadRequest.STATUS_CANCELED:
                                        case ArsdkFirmwareUploadRequest.STATUS_ABORTED:
                                            callback.onUpdateEnd(Callback.Status.CANCELED);
                                            break;
                                        case ArsdkFirmwareUploadRequest.STATUS_FAILED:
                                            callback.onUpdateEnd(Callback.Status.FAILED);
                                            break;
                                    }
                                }
                            });
                    if (uploadRequest[0] == null) {
                        callback.onUpdateEnd(Callback.Status.FAILED);
                    }
                } else {
                    callback.onUpdateEnd(Callback.Status.FAILED);
                }
            });

            return uploadRequest[0] != null ? uploadRequest[0]::cancel : fileRequest.isComplete() ? null : () -> {
                fileRequest.cancel();
                if (uploadRequest[0] != null) {
                    uploadRequest[0].cancel();
                }
            };
        }
    }
}
