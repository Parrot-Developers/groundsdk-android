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
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;
import com.parrot.drone.groundsdk.internal.facility.BlackBoxReporterCore;
import com.parrot.drone.groundsdk.internal.http.HttpBlackBoxClient;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.io.Files;
import com.parrot.drone.groundsdk.internal.tasks.Task;
import com.parrot.drone.groundsdk.internal.tasks.TaskGroup;
import com.parrot.drone.groundsdk.internal.utility.BlackBoxStorage;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.groundsdk.internal.utility.UserAccountInfo;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_BLACKBOX;

/**
 * Monitors black box report repository on the user device's local file system, and uploads blackbox files to server.
 */
public class BlackBoxEngine extends EngineBase {

    /** Extension for reports which have not been completely written yet. */
    static final String TMP_REPORT_EXT = ".tmp";

    /** Blackbox reporter facility for which this engine is the backend. */
    @NonNull
    private final BlackBoxReporterCore mBlackBoxReporter;

    /** Queue of pending blackboxes to be uploaded. */
    @NonNull
    private final Queue<File> mPendingBlackBoxes;

    /** Collects all background tasks. */
    @NonNull
    private final TaskGroup mTasks;

    /**
     * Root directory where blackbox files are stored on the user device's local file system.
     * <p>
     * This directory may contain <ul>
     * <li>the current {@link #mWorkDir}, which may itself contain temporary blackbox files (being currently
     * archived to persistent storage) and finalized blackbox files (that are ready to be uploaded),</li>
     * <li> previous work directories, that may themselves contain finalized blackbox files, or temporary ones that
     * failed to be finalized. </li>
     * </ul>
     * When the engine starts, all finalized blackbox files from all work directories are listed and queued for upload;
     * temporary blackbox files in previous work directories (other than {@code mWorkDir}) are deleted. Temporary
     * blackbox files in {@code mWorkDir} are left untouched.
     */
    @NonNull
    private final File mEngineDir;

    /** Public directory where blackboxes are copied, {@code null} if copy is disabled. */
    @Nullable
    private final File mPublicDir;

    /** Current work directory where new blackbox are archived to. */
    @Nullable
    private File mWorkDir;

    /** HTTP blackbox client, for uploading blackbox files to remote server. */
    @Nullable
    private HttpBlackBoxClient mHttpClient;

    /** Current blackbox upload request, {@code null} when no upload is ongoing. */
    @Nullable
    private HttpRequest mCurrentUploadRequest;

    /**
     * Constructor.
     *
     * @param controller provides access to the engine's controller; opaque to subclasses, which should forward it
     *                   directly through {@code super(controller)}
     */
    public BlackBoxEngine(@NonNull Controller controller) {
        super(controller);
        mBlackBoxReporter = new BlackBoxReporterCore(getFacilityPublisher());
        mEngineDir = new File(ApplicationStorageProvider.getInstance().getInternalAppFileCache(), "blackbox");
        String folder = GroundSdkConfig.get(getContext()).getBlackBoxPublicFolder();
        mPublicDir = folder == null ? null : new File(getContext().getExternalFilesDir(null), folder);
        mPendingBlackBoxes = new LinkedList<>();
        mTasks = new TaskGroup();
        publishUtility(BlackBoxStorage.class, new BlackBoxStorageCore(this));
    }

    @Override
    public void onStart() {
        mTasks.add(launchCollectJob());
        getUtilityOrThrow(SystemConnectivity.class).monitorWith(mInternetMonitor);
        getUtilityOrThrow(UserAccountInfo.class).monitorWith(mAccountMonitor);
        mBlackBoxReporter.publish();
    }

    @Override
    protected void onStopRequested() {
        acknowledgeStopRequest();
        getUtilityOrThrow(UserAccountInfo.class).disposeMonitor(mAccountMonitor);
        getUtilityOrThrow(SystemConnectivity.class).disposeMonitor(mInternetMonitor);
        mBlackBoxReporter.unpublish();
        mTasks.cancelAll();
        if (mHttpClient != null) {
            mHttpClient.dispose();
            mHttpClient = null;
        }
        mWorkDir = null;
        mPendingBlackBoxes.clear();
    }

    /**
     * Retrieves the root directory where all blackbox files are stored on the device's file system.
     *
     * @return engine directory
     */
    @NonNull
    File getEngineDirectory() {
        return mEngineDir;
    }

    /**
     * Retrieves the directory where new blackboxes may be archived on the device's file system.
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
     * Queues blackboxes to be uploaded.
     *
     * @param blackboxes blackboxes to be queued
     */
    void queueForUpload(@NonNull Collection<File> blackboxes) {
        mPendingBlackBoxes.addAll(blackboxes);
        uploadNextBlackBox();
    }

    /**
     * Archives the given black box data to a local black box report file.
     *
     * @param data black box data to archive
     */
    void archiveBlackBox(@NonNull BlackBoxStorage.BlackBox data) {
        mTasks.add(launchArchiveJob(data));
    }

    /**
     * Copies the given black box file to the public folder if it is configured.
     *
     * @param blackbox black box file to copy
     *
     * @throws InterruptedException if the current thread is interrupted while this method executes
     */
    void copyToPublicFolder(@NonNull File blackbox) throws InterruptedException {
        if (mPublicDir != null) {
            try {
                Files.copyFile(blackbox, new File(mPublicDir, blackbox.getName()));
            } catch (IOException e) {
                ULog.w(TAG_BLACKBOX, "Could not copy blackbox to public folder: " + blackbox, e);
            }
        }
    }

    /**
     * Tries to uploads the next blackbox file in the pending queue, if any.
     */
    private void uploadNextBlackBox() {
        mBlackBoxReporter.updatePendingCount(mPendingBlackBoxes.size());
        if (mCurrentUploadRequest != null) {
            mBlackBoxReporter.notifyUpdated();
            return;
        }

        UserAccountInfo accountInfo = getUtilityOrThrow(UserAccountInfo.class);
        String userAccount = accountInfo.getAccountIdentifier();

        File blackBox = mPendingBlackBoxes
                .stream()
                .filter(it -> it.lastModified() >= accountInfo.getPersonalDataAllowanceDate().getTime())
                .findFirst().orElse(null);

        if (userAccount == null || mHttpClient == null || blackBox == null) {
            mBlackBoxReporter.updateUploadingFlag(false).notifyUpdated();
            return;
        }

        mBlackBoxReporter.updateUploadingFlag(true).notifyUpdated();

        mCurrentUploadRequest = mHttpClient.upload(blackBox, userAccount, status -> {
            mCurrentUploadRequest = null;
            switch (status) {
                case SUCCESS:
                case BAD_BLACKBOX:
                    mPendingBlackBoxes.remove(blackBox);
                    deleteBlackBox(blackBox);
                    uploadNextBlackBox();
                    break;
                case BAD_REQUEST:
                    ULog.e(TAG_BLACKBOX, "Bad request sent to the server");
                    // delete file and stop uploading to avoid multiple errors
                    mPendingBlackBoxes.remove(blackBox);
                    deleteBlackBox(blackBox);
                    mBlackBoxReporter.updatePendingCount(mPendingBlackBoxes.size());
                    mBlackBoxReporter.updateUploadingFlag(false).notifyUpdated();
                    break;
                case SERVER_ERROR:
                case CANCELED:
                case UNKNOWN_ERROR:
                    // stop uploading
                    mBlackBoxReporter.updateUploadingFlag(false).notifyUpdated();
                    break;
            }
        });
    }

    /**
     * Deletes a blackbox file from internal storage.
     *
     * @param blackbox blackbox file to delete
     */
    private static void deleteBlackBox(@NonNull File blackbox) {
        if (blackbox.exists() && !blackbox.delete() && ULog.w(TAG_BLACKBOX)) {
            ULog.w(TAG_BLACKBOX, "Could not delete blackbox: " + blackbox);
        }
    }

    /** Listens to internet connection availability changes. */
    @NonNull
    private final SystemConnectivity.Monitor mInternetMonitor = available -> {
        if (available) {
            mHttpClient = createHttpClient();
            uploadNextBlackBox();
        } else if (mHttpClient != null) {
            mHttpClient.dispose();
            mHttpClient = null;
        }
    };

    /** Listens to user account changes. */
    @NonNull
    private final UserAccountInfo.Monitor mAccountMonitor = userAccountInfo -> {
        if (userAccountInfo.getAccountIdentifier() != null) {
            uploadNextBlackBox();
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
     * Launches the report archive background task.
     * <p>
     * Only used by tests to mock the archive task.
     *
     * @param data black box data to archive
     *
     * @return the running upload background task
     */
    @VisibleForTesting
    Task<File> launchArchiveJob(@NonNull BlackBoxStorage.BlackBox data) {
        return new ArchiveJob(this, data).launch();
    }

    /**
     * Creates the HTTP BlackBox client.
     * <p>
     * Only used by tests to mock the HTTP client
     *
     * @return HTTP BlackBox client
     */
    @VisibleForTesting
    @NonNull
    HttpBlackBoxClient createHttpClient() {
        return new HttpBlackBoxClient(getContext());
    }
}
