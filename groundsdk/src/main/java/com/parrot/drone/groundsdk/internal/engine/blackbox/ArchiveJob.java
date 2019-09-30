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

package com.parrot.drone.groundsdk.internal.engine.blackbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.internal.io.Files;
import com.parrot.drone.groundsdk.internal.tasks.Job;
import com.parrot.drone.groundsdk.internal.utility.BlackBoxStorage;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_BLACKBOX;
import static com.parrot.drone.groundsdk.internal.engine.blackbox.BlackBoxEngine.TMP_REPORT_EXT;

/**
 * Background job that archives in memory black box data to a black box file on local storage.
 */
class ArchiveJob extends Job<File> {

    /** Black box report engine to call back when the job completes. */
    private final BlackBoxEngine mEngine;

    /** Black box data to archive. */
    @NonNull
    private final BlackBoxStorage.BlackBox mBlackBox;

    /**
     * Constructor.
     *
     * @param engine   black box engine
     * @param blackBox in-memory black box to archive
     */
    ArchiveJob(@NonNull BlackBoxEngine engine, @NonNull BlackBoxStorage.BlackBox blackBox) {
        mEngine = engine;
        mBlackBox = blackBox;
    }

    @Nullable
    @Override
    protected File doInBackground() throws IOException, InterruptedException {
        File workDir = mEngine.getWorkDirectory();

        Files.makeDirectories(workDir);

        File tmpFile = File.createTempFile(".blackbox", TMP_REPORT_EXT, workDir);
        try {
            GZIPOutputStream dstStream = new GZIPOutputStream(new FileOutputStream(tmpFile));
            //noinspection TryFinallyCanBeTryWithResources
            try {
                mBlackBox.writeTo(dstStream);
            } finally {
                dstStream.close();
            }

            File blackBoxFile;
            do {
                blackBoxFile = new File(workDir, UUID.randomUUID().toString());
            } while (blackBoxFile.exists()); //  can still be racy, but very much unlikely

            if (!tmpFile.renameTo(blackBoxFile)) {
                throw new IOException("Could not rename black box report " + tmpFile + " to " + blackBoxFile);
            }

            mEngine.copyToPublicFolder(blackBoxFile);

            return blackBoxFile;
        } finally {
            if (tmpFile.exists() && !tmpFile.delete() && ULog.w(TAG_BLACKBOX)) {
                ULog.w(TAG_BLACKBOX, "Could not delete temporary black box report " + tmpFile);
            }
        }
    }

    @Override
    protected void onComplete(@Nullable File report, @Nullable Throwable error, boolean canceled) {
        if (error != null) {
            // archiving failed
            ULog.e(TAG_BLACKBOX, "Failed to archive black box report", error);
        } else if (report != null) {
            // black box file successfully archived, queue for upload
            mEngine.queueForUpload(Collections.singleton(report));
        }
    }

    @Override
    public final String toString() {
        return "Black box report archive job";
    }
}
