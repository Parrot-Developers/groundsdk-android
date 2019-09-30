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

package com.parrot.drone.groundsdk.internal.engine.flightlog;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.io.Files;
import com.parrot.drone.groundsdk.internal.tasks.Job;
import com.parrot.drone.groundsdk.internal.utility.FlightLogStorage;
import com.parrot.drone.groundsdk.internal.utility.UserAccountInfo;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_FLIGHTLOG;

/**
 * Background job that browses the flight log repository on the user device's local file system to compute the list
 * of flight logs to be uploaded to the server.
 * <p>
 * This job also deletes any not completely downloaded flight logs from the file system.
 */
final class CollectJob extends Job<Collection<File>> {

    /** Flight log engine to call back when the job completes. */
    @NonNull
    private final FlightLogEngine mEngine;

    /** Storage space quota, in bytes. */
    @IntRange(from = 0)
    private final long mSpaceQuota;

    /** Flight log validity date, with respect to collected user personal data policy. Milliseconds since epoch. */
    @IntRange(from = 0)
    private final long mValidityDate;


    /**
     * Constructor.
     *
     * @param engine flight log engine
     */
    CollectJob(@NonNull FlightLogEngine engine) {
        mEngine = engine;
        mSpaceQuota = GroundSdkConfig.get(mEngine.getContext()).getFlightLogQuota();
        mValidityDate = mEngine.getUtilityOrThrow(UserAccountInfo.class).getPersonalDataAllowanceDate().getTime();
    }

    @Override
    @NonNull
    protected Collection<File> doInBackground() throws IOException {
        File engineDir = mEngine.getEngineDirectory();

        Files.makeDirectories(engineDir);

        LinkedList<File> collected = new LinkedList<>();
        Collection<File> toPrune = new ArrayList<>();
        long totalSize = 0;

        for (File dir : engineDir.listFiles(file -> !file.equals(mEngine.getWorkDirectory()))) {
            toPrune.add(dir); // removed from prune list if we find collectible flight data inside
            File[] files = dir.listFiles();
            if (files != null) for (File file : files) {
                if (file.isFile()
                    && !file.getName().endsWith(FlightLogStorage.TMP_FILE_EXT)
                    && file.lastModified() >= mValidityDate) {
                    // keep dir away from pruning
                    toPrune.remove(dir);
                    // collect flight log for upload
                    collected.add(file);
                    totalSize += file.length();
                } else {
                    toPrune.add(file);
                }
            }
        }

        // apply quota
        Collections.sort(collected, Files.DESCENDING_DATE);
        while (totalSize > mSpaceQuota && !collected.isEmpty()) {
            File prunable = collected.removeFirst();
            toPrune.add(prunable);
            totalSize -= prunable.length();
        }

        // prune files
        for (File prunable : toPrune) {
            if (!Files.deleteDirectoryTree(prunable) && ULog.w(TAG_FLIGHTLOG)) {
                ULog.w(TAG_FLIGHTLOG, "Could not delete: " + prunable);
            }
        }

        return collected;
    }

    @Override
    protected void onComplete(@Nullable Collection<File> flightLogFiles, @Nullable Throwable error, boolean canceled) {
        if (error != null) {
            ULog.w(TAG_FLIGHTLOG, "Error collecting flight logs", error);
        } else if (flightLogFiles != null && !flightLogFiles.isEmpty()) {
            mEngine.queueForUpload(flightLogFiles);
        }
    }

    @Override
    public String toString() {
        return "Collect local flight logs job";
    }

    /**
     * Constructor for tests.
     *
     * @param engine       flight log engine
     * @param spaceQuota   storage space quota
     * @param validityDate flight log validity date
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    CollectJob(@NonNull FlightLogEngine engine, @IntRange(from = 0) long spaceQuota,
               @IntRange(from = 0) long validityDate) {
        mEngine = engine;
        mSpaceQuota = spaceQuota;
        mValidityDate = validityDate;
    }
}
