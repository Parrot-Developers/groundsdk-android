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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.CertificateUploader;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

import java.io.File;

/** Core class for the CertificateUploader. */
public final class CertificateUploaderCore extends SingletonComponentCore implements CertificateUploader {

    /** Engine-specific backend for CertificateUploader. */
    public interface Backend {

        /**
         * Starts to upload a certificate to the drone.
         *
         * @param certificate certificate to upload
         */
        void upload(@NonNull File certificate);

        /** Cancels the current upload, if any. */
        void cancel();
    }

    /** Description of CertificateUploader. */
    private static final ComponentDescriptor<Peripheral, CertificateUploader> DESC =
            ComponentDescriptor.of(CertificateUploader.class);

    /** Backend of this peripheral. */
    @NonNull
    private final Backend mBackend;

    /** Latest completion status. */
    @NonNull
    private CompletionStatus mStatus;

    /** {@code true} when uploading a certificate. */
    private boolean mUploading;

    /**
     * Constructor.
     *
     * @param peripheralStore   store where this peripheral belongs
     * @param backend           backend used to forward actions to the engine
     */
    public CertificateUploaderCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mStatus = CompletionStatus.NONE;
        mBackend = backend;
    }

    @Override
    public void upload(@NonNull File certificate) {
        mBackend.upload(certificate);
    }

    @Override
    public void cancel() {
        mBackend.cancel();
    }

    @Override
    public boolean isUploading() {
        return mUploading;
    }

    @NonNull
    @Override
    public CompletionStatus getCompletionStatus() {
        return mStatus;
    }

    /**
     * Updates uploading flag.
     *
     * @param uploading {@code true} to indicate that upload is ongoing, otherwise {@code false}
     *
     * @return {@code this}, to allow chained calls
     */
    public CertificateUploaderCore updateUploadingFlag(boolean uploading) {
        if (mUploading != uploading) {
            mUploading = uploading;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates completion status.
     *
     * @param status latest completion status
     *
     * @return {@code this}, to allow chained calls
     */
    public CertificateUploaderCore updateCompletionStatus(@NonNull CompletionStatus status) {
        if (mStatus != status) {
            mStatus = status;
            mChanged = true;
        }
        return this;
    }
}