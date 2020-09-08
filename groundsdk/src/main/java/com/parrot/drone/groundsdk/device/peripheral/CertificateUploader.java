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

package com.parrot.drone.groundsdk.device.peripheral;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;

import java.io.File;

/**
 * CertificateUploader peripheral interface.
 * <p>
 * This peripheral allows to upload certificates to connected devices, in order to unlock secured features on the drone.
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(CertificateUploader.class)}</pre>
 *
 * @see Drone#getPeripheral(Class)
 * @see Drone#getPeripheral(Class, Ref.Observer)
 */
public interface CertificateUploader extends Peripheral {

    /**
     * Uploads a certificate to the drone.
     * <p>
     * For a successful upload, the drone has to remain in a landed state for the whole upload time.
     *
     * @param certificate certificate to upload
     */
    void upload(@NonNull File certificate);

    /**
     * Cancels a current upload to the drone.
     */
    void cancel();

    /**
     * Tells whether a certificate is currently being uploaded to the device.
     *
     * @return {@code true} when certificate upload is ongoing, otherwise {@code false}
     */
    boolean isUploading();

    /**
     * Upload completion status.
     */
    enum CompletionStatus {

        /**
         * No known completion status.
         * <p>
         * Either an upload is {@link #isUploading()} ongoing} but has not completed yet, or no certificate
         * upload has ever been started yet.
         */
        NONE,

        /** Latest certificate upload was successful. */
        SUCCESS,

        /** Latest certificate upload has failed or was canceled before successful completion. */
        FAILED
    }

    /**
     * Retrieves latest certificate upload completion status.
     *
     * @return latest completion status
     */
    @NonNull
    CompletionStatus getCompletionStatus();
}
