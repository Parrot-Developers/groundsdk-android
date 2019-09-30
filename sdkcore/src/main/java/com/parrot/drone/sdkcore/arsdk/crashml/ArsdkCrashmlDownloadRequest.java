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

package com.parrot.drone.sdkcore.arsdk.crashml;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkRequest;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A request to download crashml reports.
 * <p>
 * Cancellation has a best-effort behavior (see {@link ArsdkRequest#cancel()}.
 */
public final class ArsdkCrashmlDownloadRequest extends ArsdkRequest {

    /** Int definition for the request completion status. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_OK, STATUS_CANCELED, STATUS_FAILED, STATUS_ABORTED})
    public @interface Status {}

    /* Numerical status values MUST be kept in sync with C enum arsdk_crashml_req_status. */

    /** Request completed successfully. */
    public static final int STATUS_OK = 0;

    /** Request did not complete because it was canceled by caller. */
    public static final int STATUS_CANCELED = 1;

    /** Request failed for an unknown reason. */
    public static final int STATUS_FAILED = 2;

    /** Request failed because it was aborted due to device disconnection. */
    public static final int STATUS_ABORTED = 3;

    /**
     * Listener notified of request failure and results.
     */
    public interface Listener {

        /**
         * Called back each time one crash report has been successfully downloaded.
         * <p>
         * Called back on <strong>MAIN</strong> thread.
         *
         * @param report downloaded report path
         */
        void onReportDownloaded(@NonNull File report);

        /**
         * Called back when the request completes.
         * <p>
         * Called back on <strong>MAIN</strong> thread.
         *
         * @param status final request status
         */
        void onRequestComplete(@Status int status);
    }

    /**
     * Creates and runs a new crashml download request.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     * <p>
     * Note that {@code deviceHandle} is the handle of the device that will handle the request, which is not
     * necessarily the device where reports are stored (for instance the device handle can be an RC handle, yet
     * reports will come from drone currently connected to that RC). <br/>
     * On the other hand, {@code deviceType} is the type of the device where reports are stored.
     *
     * @param arsdkCore    ArsdkCore instance
     * @param deviceHandle handle of the device that will process the request
     * @param deviceType   type of the device where reports are stored
     * @param destDir      path of the directory where to download reports
     * @param listener     listener to be notified of request failure or result
     *
     * @return a new ArsdkRequest instance
     */
    @NonNull
    public static ArsdkRequest create(@NonNull ArsdkCore arsdkCore, short deviceHandle,
                                      @ArsdkDevice.Type int deviceType, @NonNull String destDir,
                                      @NonNull Listener listener) {
        return new ArsdkCrashmlDownloadRequest(arsdkCore, deviceHandle, deviceType, destDir, listener);
    }

    /** ArsdkCore instance. */
    @NonNull
    private final ArsdkCore mArsdkCore;

    /** Handle of the device which processes the request. */
    private final short mDeviceHandle;

    /** Native type of the device on which reports are stored. */
    @ArsdkDevice.Type
    private final int mDeviceType;

    /** Path of the directory where reports must be downloaded. */
    @NonNull
    private final String mDestDir;

    /** Listener notified of request status. */
    @NonNull
    private final Listener mListener;

    /** {@code true} when the request has been canceled by the caller. */
    private boolean mCanceled;

    /** Native request pointer. */
    private long mNativePtr;

    /**
     * Constructor.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     * <p>
     * Ensures that destination directory exists beforehand or otherwise tries to create it.
     *
     * @param arsdkCore    ArsdkCore instance
     * @param deviceHandle handle of the device that will process the request
     * @param deviceType   type of the device where reports are stored
     * @param destDir      path of the directory where to download reports
     * @param listener     listener to be notified of request failure or result
     */
    private ArsdkCrashmlDownloadRequest(@NonNull ArsdkCore arsdkCore, short deviceHandle,
                                        @ArsdkDevice.Type int deviceType, @NonNull String destDir,
                                        @NonNull Listener listener) {
        mArsdkCore = arsdkCore;
        mDeviceHandle = deviceHandle;
        mDeviceType = deviceType;
        mDestDir = destDir;
        mListener = listener;

        mArsdkCore.dispatchToPomp(() -> {
            if (!mCanceled) {
                // ensure destination dir exists
                File dest = new File(destDir);
                if (dest.exists() || dest.mkdirs()) {
                    mNativePtr = nativeCreate(mArsdkCore.getNativePtr(), mDeviceHandle, mDeviceType,
                            mDestDir + "/");
                }
            }
            if (mNativePtr == 0) {
                onRequestStatus(mCanceled ? STATUS_CANCELED : STATUS_FAILED);
            }
        });
    }

    @Override
    public void cancel() {
        if (!mCanceled) {
            mCanceled = true;
            mArsdkCore.dispatchToPomp(() -> {
                if (mNativePtr != 0) {
                    nativeCancel(mNativePtr);
                }
            });
        }
    }

    /**
     * Called back each time a report has been processed.
     * <p>
     * Called back on <strong>POMP</strong> thread.
     *
     * @param path   report path on local storage, meaningless unless {@code status == STATUS_OK}
     * @param status current request status
     */
    @SuppressWarnings("unused") /* native-cb */
    private void onRequestProgress(@NonNull String path, @Status int status) {
        if (status == STATUS_OK) {
            mArsdkCore.dispatchToMain(() -> mListener.onReportDownloaded(new File(path)));
        }
    }

    /**
     * Called back when the request either fails or completes successfully.
     * <p>
     * Called back on <strong>POMP</strong> thread.
     * <p>
     * The request is over in any case after this method has been called back and must be disposed.
     *
     * @param status final request status
     */
    @SuppressWarnings("unused") /* native-cb */
    private void onRequestStatus(@Status int status) {
        mNativePtr = 0;
        mArsdkCore.dispatchToMain(() -> mListener.onRequestComplete(status));
    }

    /* JNI declarations and setup */
    private native long nativeCreate(long arsdkNativePtr, short deviceHandle, @ArsdkDevice.Type int deviceType,
                                     @NonNull String destDir);

    private static native void nativeClassInit();

    private static native void nativeCancel(long nativePtr);

    static {
        nativeClassInit();
    }
}
