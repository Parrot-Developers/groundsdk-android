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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;
import com.parrot.drone.groundsdk.internal.facility.CrashReporterCore;
import com.parrot.drone.groundsdk.internal.http.HttpCrashMlClient;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.tasks.Task;
import com.parrot.drone.groundsdk.internal.tasks.TaskGroup;
import com.parrot.drone.groundsdk.internal.utility.CrashReportStorage;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.groundsdk.internal.utility.UserAccountInfo;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.util.Collection;
import java.util.UUID;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_CRASH;

/**
 * Monitors crash report repository on the user device's local file system, and upload reports to the CrashML server.
 */
public class CrashReportEngine extends EngineBase {

    /** Crash reporter facility for which this engine is the backend. */
    @NonNull
    private final CrashReporterCore mCrashReporter;

    /** Crash report storage utility that this engine provides. */
    @NonNull
    private final CrashReportStorageCore mCrashReportStorage;

    /** Collects all background tasks. */
    @NonNull
    private final TaskGroup mTasks;

    /**
     * Root directory where reports are stored on the user device's local file system.
     * <p>
     * This directory may contain <ul>
     * <li>the current {@link #mWorkDir}, which may itself contain temporary reports (being currently downloaded
     * from remote devices) and finalized reports (that are ready to be uploaded),</li>
     * <li> previous work directories, that may themselves contain finalized reports, or temporary reports that
     * failed to be downloaded completely. </li>
     * </ul>
     * When the engine starts, all finalized reports from all work directories are listed and queued for upload;
     * temporary reports in previous work directories (other than {@code mWorkDir}) are deleted. Temporary reports
     * in {@code mWorkDir} are left untouched.
     */
    @NonNull
    private final File mEngineDir;

    /** Current work directory where reports downloaded from remote devices get stored. */
    @Nullable
    private File mWorkDir;

    /** HTTP CrashML client, for uploading report files to remote server. */
    @Nullable
    private HttpCrashMlClient mHttpClient;

    /** Current report upload request, {@code null} when no upload is ongoing. */
    @Nullable
    private HttpRequest mCurrentUploadRequest;

    /**
     * Constructor.
     *
     * @param controller provides access to the engine's controller; opaque to subclasses, which should forward it
     *                   directly through {@code super(controller)}
     */
    public CrashReportEngine(@NonNull Controller controller) {
        super(controller);
        mCrashReporter = new CrashReporterCore(getFacilityPublisher());
        mCrashReportStorage = new CrashReportStorageCore(this);
        mEngineDir = new File(ApplicationStorageProvider.getInstance().getInternalAppFileCache(), "crash");
        mTasks = new TaskGroup();
        publishUtility(CrashReportStorage.class, mCrashReportStorage);
    }

    @Override
    public void onStart() {
        mTasks.add(launchCollectJob());
        getUtilityOrThrow(SystemConnectivity.class).monitorWith(mInternetMonitor);
        getUtilityOrThrow(UserAccountInfo.class).monitorWith(mAccountMonitor);
        mCrashReporter.publish();
    }

    @Override
    protected void onStopRequested() {
        acknowledgeStopRequest();
        getUtilityOrThrow(UserAccountInfo.class).disposeMonitor(mAccountMonitor);
        getUtilityOrThrow(SystemConnectivity.class).disposeMonitor(mInternetMonitor);
        mCrashReporter.unpublish();
        mTasks.cancelAll();
        if (mHttpClient != null) {
            mHttpClient.dispose();
            mHttpClient = null;
        }
        mWorkDir = null;
        mCrashReportStorage.clear();
    }

    /**
     * Retrieves the root directory where crash reports are stored on the device's file system.
     *
     * @return crash reports directory
     */
    @NonNull
    File getEngineDirectory() {
        return mEngineDir;
    }

    /**
     * Retrieves the directory where new crash reports may be downloaded on the device's file system.
     *
     * @return work directory
     */
    @NonNull
    File getWorkDirectory() {
        if (mWorkDir == null) {
            mWorkDir = new File(mEngineDir, UUID.randomUUID().toString());
        }
        return mWorkDir;
    }

    /**
     * Queues reports to be uploaded.
     *
     * @param reportFiles report files to be queued
     */
    void queueForUpload(@NonNull Collection<File> reportFiles) {
        mCrashReportStorage.registerReports(reportFiles);
        uploadNextReport();
    }

    /**
     * Tries to upload the next report in the pending queue, if any.
     */
    private void uploadNextReport() {
        mCrashReporter.updatePendingCount(mCrashReportStorage.reportCount());
        if (mCurrentUploadRequest != null) {
            mCrashReporter.notifyUpdated();
            return;
        }

        UserAccountInfo accountInfo = getUtilityOrThrow(UserAccountInfo.class);
        String userAccount = accountInfo.getAccountIdentifier();
        CrashReportStorageCore.Report report;

        if (userAccount != null) {
            report = mCrashReportStorage.peekNextPersonalReport(accountInfo.getPersonalDataAllowanceDate());
        } else if (accountInfo.isAnonymousDataUploadAllowed()) {
            report = mCrashReportStorage.peekNextAnonymousReport();
        } else {
            report = null;
        }

        if (report == null || mHttpClient == null) {
            mCrashReporter.updateUploadingFlag(false).notifyUpdated();
            return;
        }

        mCrashReporter.updateUploadingFlag(true).notifyUpdated();

        mCurrentUploadRequest = mHttpClient.upload(report.file(), userAccount, status -> {
            mCurrentUploadRequest = null;
            switch (status) {
                case SUCCESS:
                case BAD_REPORT:
                    report.delete();
                    uploadNextReport();
                    break;
                case BAD_REQUEST:
                    if (ULog.e(TAG_CRASH)) {
                        ULog.e(TAG_CRASH, "Bad request sent to the server");
                    }
                    // delete report and stop uploading to avoid multiple errors
                    report.delete();
                    mCrashReporter.updatePendingCount(mCrashReportStorage.reportCount());
                    mCrashReporter.updateUploadingFlag(false).notifyUpdated();
                    break;
                case SERVER_ERROR:
                case CANCELED:
                case UNKNOWN_ERROR:
                    // stop uploading
                    mCrashReporter.updateUploadingFlag(false).notifyUpdated();
                    break;
            }
        });
    }

    /** Listens to internet connection availability changes. */
    @NonNull
    private final SystemConnectivity.Monitor mInternetMonitor = available -> {
        if (available) {
            mHttpClient = createHttpClient();
            uploadNextReport();
        } else if (mHttpClient != null) {
            mHttpClient.dispose();
            mHttpClient = null;
        }
    };

    /** Listens to user account changes. */
    @NonNull
    private final UserAccountInfo.Monitor mAccountMonitor = userAccountInfo -> {
        if (userAccountInfo.getAccountIdentifier() != null || userAccountInfo.isAnonymousDataUploadAllowed()) {
            uploadNextReport();
        }
    };

    /**
     * Launches the collect report background task.
     * <p>
     * Only used by tests to mock the collect task.
     *
     * @return the running collect background task
     */
    @VisibleForTesting
    Task<Collection<File>> launchCollectJob() {
        return new CollectJob(this).launch();
    }

    /**
     * Creates the HTTP CrashML client.
     * <p>
     * Only used by tests to mock the HTTP client
     *
     * @return HTTP CrashML client
     */
    @VisibleForTesting
    @NonNull
    HttpCrashMlClient createHttpClient() {
        return new HttpCrashMlClient(getContext());
    }
}
