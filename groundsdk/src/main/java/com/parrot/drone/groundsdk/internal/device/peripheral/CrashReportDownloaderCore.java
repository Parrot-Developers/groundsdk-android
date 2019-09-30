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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.CrashReportDownloader;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/** Core class for the CrashReportDownloader. */
public final class CrashReportDownloaderCore extends SingletonComponentCore implements CrashReportDownloader {

    /** Description of CrashReportDownloader. */
    private static final ComponentDescriptor<Peripheral, CrashReportDownloader> DESC =
            ComponentDescriptor.of(CrashReportDownloader.class);

    /** Latest completion status. */
    @NonNull
    private CompletionStatus mStatus;

    /** Latest downloaded crash reports count. */
    @IntRange(from = 0)
    private int mCount;

    /** {@code} true when downloading crash reports. */
    private boolean mDownloading;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     */
    public CrashReportDownloaderCore(@NonNull ComponentStore<Peripheral> peripheralStore) {
        super(DESC, peripheralStore);
        mStatus = CompletionStatus.NONE;
    }

    @Override
    public boolean isDownloading() {
        return mDownloading;
    }

    @NonNull
    @Override
    public CompletionStatus getCompletionStatus() {
        return mStatus;
    }

    @Override
    public int getLatestDownloadCount() {
        return mCount;
    }

    /**
     * Updates downloading flag.
     *
     * @param downloading {@code true} to indicate that download is ongoing, otherwise {@code false}
     *
     * @return {@code this}, to allow chained calls
     */
    public CrashReportDownloaderCore updateDownloadingFlag(boolean downloading) {
        if (mDownloading != downloading) {
            mDownloading = downloading;
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
    public CrashReportDownloaderCore updateCompletionStatus(@NonNull CompletionStatus status) {
        if (mStatus != status) {
            mStatus = status;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates downloaded crash reports count.
     *
     * @param count latest downloaded crash reports count
     *
     * @return {@code this}, to allow chained calls
     */
    public CrashReportDownloaderCore updateDownloadedCount(@IntRange(from = 0) int count) {
        if (mCount != count) {
            mCount = count;
            mChanged = true;
        }
        return this;
    }
}
