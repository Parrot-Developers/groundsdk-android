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

package com.parrot.drone.groundsdk.internal.device.peripheral.media;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface for a request that can be canceled.
 */
public interface MediaRequest {

    /**
     * Termination status of a request.
     */
    enum Status {

        /** Request completed successfully. */
        SUCCESS,

        /** Request failed for an unspecified reason. */
        FAILED,

        /** Request was canceled by caller. */
        CANCELED,

        /** Request failed because it was aborted due to device disconnection. */
        ABORTED
    }

    /**
     * Allows to be notified of request completion.
     */
    interface StatusCallback {

        /**
         * Called back when the request completes.
         *
         * @param status terminal status of the request
         */
        void onRequestComplete(@NonNull Status status);
    }

    /**
     * Allows to be notified of request completion, with an optional result.
     */
    interface ResultCallback<T> {

        /**
         * Called back when the request completes.
         * <p>
         * {@code result} may be {@code null} even if {@code status == } {@link Status#SUCCESS}.
         *
         * @param status terminal status of the request
         * @param result request result, {@code null} if {@code status != } {@link Status#SUCCESS}
         */
        void onRequestComplete(@NonNull Status status, @Nullable T result);
    }

    /**
     * Allows to be notified regularly of request progress.
     */
    interface ProgressCallback {

        /**
         * Called back when the current progress of the request changes.
         *
         * @param progress current request progress
         */
        void onRequestProgress(@IntRange(from = 0, to = 100) int progress);
    }

    /**
     * Allows to be notified regularly of request progress, and of request completion with an optional result.
     */
    interface ProgressResultCallback<T> extends ResultCallback<T>, ProgressCallback {
    }

    /**
     * Allows to be notified regularly of request progress and of request completion with a status.
     */
    interface ProgressStatusCallback extends StatusCallback, ProgressCallback {
    }

    /**
     * Cancels the request.
     */
    void cancel();
}
