/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpService;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpServicesClient;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpCertificateClient;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.device.peripheral.CertificateUploader;
import com.parrot.drone.groundsdk.internal.Cancelable;
import com.parrot.drone.groundsdk.internal.device.peripheral.CertificateUploaderCore;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;

import java.io.File;

/** CertificateUploader peripheral for Anafi family drones. */
public class AnafiCertificateUploader extends DronePeripheralController {

    /** CertificateUploader peripheral for which this object is the backend. */
    @NonNull
    private final CertificateUploaderCore mCertificateUploader;

    /** HTTP client used to send the certificates. */
    @Nullable
    private HttpCertificateClient mHttpCertificateClient;

    /** HTTP client used to list available HTTP services on device. */
    @Nullable
    private HttpServicesClient mHttpServicesClient;

    /** Reference to the ongoing upload request, otherwise {@code null} */
    @Nullable
    private Cancelable mUploadRequest;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiCertificateUploader(@NonNull DroneController droneController) {
        super(droneController);
        mCertificateUploader = new CertificateUploaderCore(mComponentStore, mBackend);
    }

    @Override
    protected void onConnected() {
        mHttpCertificateClient = mDeviceController.getHttpClient(HttpCertificateClient.class);
        mHttpServicesClient = mDeviceController.getHttpClient(HttpServicesClient.class);

        if (mHttpServicesClient != null && mHttpCertificateClient != null) {
            mHttpServicesClient.listModules((status, code, list) -> {
                if (status == HttpRequest.Status.SUCCESS) {
                    assert list != null;
                    if (list.contains(HttpService.CREDENTIAL)) {
                        mCertificateUploader.publish();
                    }
                }
            });
        }
    }

    @Override
    protected void onDisconnected() {
        mCertificateUploader.unpublish();

        if (mHttpCertificateClient != null) {
            mHttpCertificateClient.dispose();
            mHttpCertificateClient = null;
        }

        if (mHttpServicesClient != null) {
            mHttpServicesClient.dispose();
            mHttpServicesClient = null;
        }

        mUploadRequest = null;
    }

    /** Backend of CertificateUploaderCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final CertificateUploaderCore.Backend mBackend = new CertificateUploaderCore.Backend() {

        @Override
        public void upload(@NonNull File certificate) {
            if (mHttpCertificateClient == null) return;

            if (mUploadRequest != null) {
                mUploadRequest.cancel();
            }

            mCertificateUploader.updateUploadingFlag(true)
                                .updateCompletionStatus(CertificateUploader.CompletionStatus.NONE);
            mUploadRequest = mHttpCertificateClient.uploadCertificate(certificate, (status, code) -> {
                mUploadRequest = null;
                mCertificateUploader.updateUploadingFlag(false)
                                    .updateCompletionStatus(status == HttpRequest.Status.SUCCESS
                                            ? CertificateUploader.CompletionStatus.SUCCESS
                                            : CertificateUploader.CompletionStatus.FAILED)
                                    .notifyUpdated();
            });
            mCertificateUploader.notifyUpdated();
        }

        @Override
        public void cancel() {
            if (mUploadRequest != null) {
                mUploadRequest.cancel();
                mUploadRequest = null;
            }
        }
    };
}
