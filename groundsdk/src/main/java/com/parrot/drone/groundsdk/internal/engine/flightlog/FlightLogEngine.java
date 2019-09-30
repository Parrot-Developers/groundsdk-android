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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;
import com.parrot.drone.groundsdk.internal.facility.FlightLogReporterCore;
import com.parrot.drone.groundsdk.internal.http.HttpFlightLogClient;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.tasks.Task;
import com.parrot.drone.groundsdk.internal.tasks.TaskGroup;
import com.parrot.drone.groundsdk.internal.utility.FlightLogStorage;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.groundsdk.internal.utility.UserAccountInfo;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_FLIGHTLOG;

/**
 * Monitors flight log repository on the user device's local file system, and upload flight logs to the server.
 */
public class FlightLogEngine extends EngineBase {

    /** Flight log reporter facility for which this engine is the backend. */
    @NonNull
    private final FlightLogReporterCore mFlightLogReporter;

    /** Queue of pending flight logs to be uploaded. */
    @NonNull
    private final Queue<File> mPendingFlightLogs;

    /** Collects all background tasks. */
    @NonNull
    private final TaskGroup mTasks;

    /**
     * Root directory where flight logs are stored on the user device's local file system.
     * <p>
     * This directory may contain <ul>
     * <li>the current {@link #mWorkDir}, which may itself contain temporary flight logs (being currently downloaded
     * from remote devices) and finalized flight logs (that are ready to be uploaded),</li>
     * <li> previous work directories, that may themselves contain finalized flight logs, or temporary flight logs
     * that failed to be downloaded completely. </li>
     * </ul>
     * When the engine starts, all finalized flight logs from all work directories are listed and queued for upload;
     * temporary flight logs in previous work directories (other than {@code mWorkDir}) are deleted. Temporary flight
     * logs in {@code mWorkDir} are left untouched.
     */
    @NonNull
    private final File mEngineDir;

    /** Current work directory where flight logs downloaded from remote devices get stored. */
    @Nullable
    private File mWorkDir;

    /** HTTP client, for uploading flight log files to remote server. */
    @Nullable
    private HttpFlightLogClient mHttpClient;

    /** Current flight log upload request, {@code null} when no upload is ongoing. */
    @Nullable
    private HttpRequest mCurrentUploadRequest;

    /**
     * Constructor.
     *
     * @param controller provides access to the engine's controller; opaque to subclasses, which should forward it
     *                   directly through {@code super(controller)}
     */
    public FlightLogEngine(@NonNull Controller controller) {
        super(controller);
        mFlightLogReporter = new FlightLogReporterCore(getFacilityPublisher());
        FlightLogStorageCore flightLogStorage = new FlightLogStorageCore(this);
        mEngineDir = new File(ApplicationStorageProvider.getInstance().getInternalAppFileCache(), "flightlog");
        mPendingFlightLogs = new LinkedList<>();
        mTasks = new TaskGroup();
        publishUtility(FlightLogStorage.class, flightLogStorage);
    }

    @Override
    public void onStart() {
        mTasks.add(launchCollectJob());
        getUtilityOrThrow(SystemConnectivity.class).monitorWith(mInternetMonitor);
        getUtilityOrThrow(UserAccountInfo.class).monitorWith(mAccountMonitor);
        mFlightLogReporter.publish();
    }

    @Override
    protected void onStopRequested() {
        acknowledgeStopRequest();
        getUtilityOrThrow(UserAccountInfo.class).disposeMonitor(mAccountMonitor);
        getUtilityOrThrow(SystemConnectivity.class).disposeMonitor(mInternetMonitor);
        mFlightLogReporter.unpublish();
        mTasks.cancelAll();
        if (mHttpClient != null) {
            mHttpClient.dispose();
            mHttpClient = null;
        }
        mWorkDir = null;
        mPendingFlightLogs.clear();
    }

    /**
     * Retrieves the root directory where flight logs are stored on the device's file system.
     *
     * @return flight logs directory
     */
    @NonNull
    File getEngineDirectory() {
        return mEngineDir;
    }

    /**
     * Retrieves the directory where new flight logs may be downloaded on the device's file system.
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
     * Queues flight logs to be uploaded.
     *
     * @param flightLogs flight logs to be queued
     */
    void queueForUpload(@NonNull Collection<File> flightLogs) {
        mPendingFlightLogs.addAll(flightLogs);
        uploadNextFlightLog();
    }

    /**
     * Tries to uploads the next flight log in the pending queue, if any.
     */
    private void uploadNextFlightLog() {
        mFlightLogReporter.updatePendingCount(mPendingFlightLogs.size());
        if (mCurrentUploadRequest != null) {
            mFlightLogReporter.notifyUpdated();
            return;
        }

        UserAccountInfo accountInfo = getUtilityOrThrow(UserAccountInfo.class);
        String userAccount = accountInfo.getAccountIdentifier();
        File flightLog = mPendingFlightLogs
                .stream()
                .filter(it -> it.lastModified() >= accountInfo.getPersonalDataAllowanceDate().getTime())
                .findFirst().orElse(null);

        if (userAccount == null || mHttpClient == null || flightLog == null) {
            mFlightLogReporter.updateUploadingFlag(false).notifyUpdated();
            return;
        }

        mFlightLogReporter.updateUploadingFlag(true).notifyUpdated();

        mCurrentUploadRequest = mHttpClient.upload(flightLog, userAccount, status -> {
            mCurrentUploadRequest = null;
            switch (status) {
                case SUCCESS:
                case BAD_FLIGHT_LOG:
                    mPendingFlightLogs.remove(flightLog);
                    deleteFlightLog(flightLog);
                    uploadNextFlightLog();
                    break;
                case BAD_REQUEST:
                    if (ULog.e(TAG_FLIGHTLOG)) {
                        ULog.e(TAG_FLIGHTLOG, "Bad request sent to the server");
                    }
                    // delete file and stop uploading to avoid multiple errors
                    mPendingFlightLogs.remove(flightLog);
                    deleteFlightLog(flightLog);
                    mFlightLogReporter.updatePendingCount(mPendingFlightLogs.size());
                    mFlightLogReporter.updateUploadingFlag(false).notifyUpdated();
                    break;
                case SERVER_ERROR:
                case CANCELED:
                case UNKNOWN_ERROR:
                    // stop uploading
                    mFlightLogReporter.updateUploadingFlag(false).notifyUpdated();
                    break;
            }
        });
    }

    /** Listens to internet connection availability changes. */
    @NonNull
    private final SystemConnectivity.Monitor mInternetMonitor = available -> {
        if (available) {
            mHttpClient = createHttpClient();
            uploadNextFlightLog();
        } else if (mHttpClient != null) {
            mHttpClient.dispose();
            mHttpClient = null;
        }
    };

    /** Listens to user account changes. */
    @NonNull
    private final UserAccountInfo.Monitor mAccountMonitor = userAccountInfo -> {
        if (userAccountInfo.getAccountIdentifier() != null) {
            uploadNextFlightLog();
        }
    };

    /**
     * Deletes a flight log file from internal storage.
     *
     * @param flightLog flight log file to delete
     */
    private static void deleteFlightLog(@NonNull File flightLog) {
        if (flightLog.exists() && !flightLog.delete() && ULog.w(TAG_FLIGHTLOG)) {
            ULog.w(TAG_FLIGHTLOG, "Could not delete flight log: " + flightLog);
        }
    }

    /**
     * Launches the collect flight logs background task.
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
     * Creates the HTTP client.
     * <p>
     * Only used by tests to mock the HTTP client
     *
     * @return HTTP client
     */
    @VisibleForTesting
    @NonNull
    HttpFlightLogClient createHttpClient() {
        return new HttpFlightLogClient(getContext());
    }
}
