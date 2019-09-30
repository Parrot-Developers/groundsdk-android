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

package com.parrot.drone.groundsdk.internal.engine.crashreport;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.internal.utility.CrashReportStorage;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_CRASH;

/**
 * Implementation class for the {@code CrashReportStorage} utility.
 */
public class CrashReportStorageCore implements CrashReportStorage {

    /** Engine that acts as a backend for this utility. */
    @NonNull
    private final CrashReportEngine mEngine;

    /** Report entries, indexed by base path. */
    @NonNull
    private final Map<String, Entry> mReports;

    /**
     * Constructor.
     *
     * @param engine crash report engine
     */
    CrashReportStorageCore(@NonNull CrashReportEngine engine) {
        mEngine = engine;
        mReports = new LinkedHashMap<>();
    }


    @NonNull
    @Override
    public File getWorkDir() {
        return mEngine.getWorkDirectory();
    }

    @Override
    public void notifyReportsReady(@NonNull Collection<File> reportFiles) {
        registerReports(reportFiles);
    }

    /**
     * Tells current report count.
     *
     * @return report count
     */
    @IntRange(from = 0)
    final int reportCount() {
        return mReports.size();
    }

    /**
     * Registers crash reports files.
     *
     * @param reports report files to register
     */
    void registerReports(@NonNull Collection<File> reports) {
        for (File toMerge : reports) {
            String id = toMerge.getAbsolutePath();
            boolean anonymous = false;
            if (id.endsWith(ANONYMOUS_REPORT_EXT)) {
                id = id.substring(0, id.length() - ANONYMOUS_REPORT_EXT.length());
                anonymous = true;
            }

            Entry entry = mReports.get(id);
            if (entry == null) {
                entry = new Entry();
                mReports.put(id, entry);
            }

            if (anonymous) {
                entry.mAnonymousReport = toMerge;
            } else {
                entry.mPersonalReport = toMerge;
            }
        }
    }

    /**
     * Clears all crash report entries.
     */
    void clear() {
        mReports.clear();
    }

    /**
     * Crash report.
     * <p>
     * This acts as a wrapper giving access to a crash report file (either an anonymous or a personal
     * report file).
     * <p>
     * This also provides a way to delete report files from storage.
     */
    abstract static class Report {

        /** Wrapped report file. */
        @NonNull
        private final File mFile;

        /**
         * Constructor.
         *
         * @param file report file
         */
        Report(@NonNull File file) {
            mFile = file;
        }

        /**
         * Provides access to the crash report file.
         *
         * @return crash report file
         */
        @NonNull
        final File file() {
            return mFile;
        }

        /**
         * Deletes the report file from device storage.
         * <p>
         * In case of a personal report file, then the corresponding anonymous report file is also
         * deleted, if it exists.
         */
        abstract void delete();
    }

    /**
     * Retrieves next anonymous report that should be uploaded.
     *
     * @return next anonymous report, or {@code null} if no such report exists
     */
    @Nullable
    Report peekNextAnonymousReport() {
        return mReports
                .values().stream()
                .filter(it -> it.mAnonymousReport != null)
                .findFirst()
                .map(entry -> {
                    File file = entry.mAnonymousReport;
                    assert file != null;
                    return new Report(file) {

                        @Override
                        void delete() {
                            deleteReportFile(file);
                            entry.mAnonymousReport = null;
                            if (entry.mPersonalReport == null) {
                                mReports.values().remove(entry);
                            }
                        }
                    };
                }).orElse(null);
    }

    /**
     * Retrieves next personal report that should be uploaded.
     *
     * @param validityDate date starting from which a report is considered valid for upload
     *
     * @return next personal report, or {@code null} if no such report exists
     */
    @Nullable
    Report peekNextPersonalReport(@NonNull Date validityDate) {
        return mReports
                .values().stream()
                .filter(it -> it.mPersonalReport != null
                              && it.mPersonalReport.lastModified() >= validityDate.getTime())
                .findFirst()
                .map(entry -> {
                    File file = entry.mPersonalReport;
                    assert file != null;
                    return new Report(file) {

                        @Override
                        void delete() {
                            deleteReportFile(file);
                            if (entry.mAnonymousReport != null) {
                                deleteReportFile(entry.mAnonymousReport);
                            }
                            mReports.values().remove(entry);
                        }
                    };
                })
                .orElse(null);
    }

    /**
     * Deletes a report file from device storage.
     *
     * @param report report file to delete, may be {@code null}, in which case this method does nothing
     */
    private static void deleteReportFile(@Nullable File report) {
        if (report != null && !report.delete() && report.exists() && ULog.w(TAG_CRASH)) {
            ULog.w(TAG_CRASH, "Could not delete crash report: " + report);
        }
    }

    /** A report entry, linking to the personal and anonymous file variants of the report. */
    private static final class Entry {

        /** Personal report file. {@code null} if none. */
        @Nullable
        File mPersonalReport;

        /** Anonymous report file. {@code null} if none. */
        @Nullable
        File mAnonymousReport;
    }
}
