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

package com.parrot.drone.groundsdk.internal.facility;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/**
 * Base implementation for 'reporter' facility core classes.
 */
public abstract class ReporterCore extends SingletonComponentCore {

    /** Count of reports pending upload. */
    private int mPendingCount;

    /** {@code true} when reports are being uploaded. */
    private boolean mUploading;

    /**
     * Constructor.
     *
     * @param descriptor    specific descriptor of the provided component
     * @param facilityStore store where this facility belongs
     */
    ReporterCore(@NonNull ComponentDescriptor<Facility, ?> descriptor,
                 @NonNull ComponentStore<Facility> facilityStore) {
        super(descriptor, facilityStore);
    }

    /**
     * Retrieves the amount of reports pending to be uploaded.
     *
     * @return pending report count
     */
    public int getPendingCount() {
        return mPendingCount;
    }

    /**
     * Tells whether a report upload is currently in progress.
     *
     * @return {@code true} when a report upload is ongoing, otherwise {@code false}
     */
    public boolean isUploading() {
        return mUploading;
    }

    /**
     * Updates pending reports count.
     *
     * @param pendingCount new pending report count
     *
     * @return {@code this}, to allow call chaining
     */
    @NonNull
    public ReporterCore updatePendingCount(int pendingCount) {
        if (mPendingCount != pendingCount) {
            mPendingCount = pendingCount;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the uploading flag.
     *
     * @param uploading new uploading flag value
     *
     * @return {@code this}, to allow call chaining
     */
    @NonNull
    public ReporterCore updateUploadingFlag(boolean uploading) {
        if (mUploading != uploading) {
            mUploading = uploading;
            mChanged = true;
        }
        return this;
    }
}
