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

package com.parrot.drone.sdkcore.arsdk.blackbox;

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkRequest;

/**
 * A request to subscribe to device black box events.
 * <p>
 * Cancellation has a guaranteed behavior (see {@link ArsdkRequest#cancel()}.
 */
public final class ArsdkBlackBoxRequest extends ArsdkRequest {

    /**
     * Listener notified each time black box data is available.
     */
    public interface Listener {

        /**
         * Called back when a button action is triggered on the remote control.
         *
         * @param action button action identifier
         */
        void onRemoteControlButtonAction(int action);

        /**
         * Called back when piloting command sent through the remote control changes.
         *
         * @param roll   piloting command roll value
         * @param pitch  piloting command pitch value
         * @param yaw    piloting command yaw value
         * @param gaz    piloting command gaz value
         * @param source identifier of the source that triggered the piloting command change
         */
        void onRemoteControlPilotingInfo(int roll, int pitch, int yaw, int gaz, int source);
    }

    /**
     * Subscribes to device black box events.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     *
     * @param arsdkCore    ArsdkCore instance
     * @param deviceHandle handle of the device that will process the request
     * @param listener     listener to be notified of black box events
     *
     * @return a new ArsdkRequest instance
     */
    @NonNull
    public static ArsdkRequest create(@NonNull ArsdkCore arsdkCore, short deviceHandle, @NonNull Listener listener) {
        return new ArsdkBlackBoxRequest(arsdkCore, deviceHandle, listener);
    }

    /** ArsdkCore instance. */
    @NonNull
    private final ArsdkCore mArsdkCore;

    /** Handle of the device which processes the request. */
    private final short mDeviceHandle;

    /** Listener notified of black box events. */
    @NonNull
    private final Listener mListener;

    /** {@code true} when the request has been canceled by the caller. */
    private boolean mCanceled;

    /** Native request pointer. */
    private long mNativePtr;

    /**
     * Constructor.
     *
     * @param arsdkCore    ArsdkCore instance
     * @param deviceHandle handle of the device that will process the request
     * @param listener     listener to be notified of black box events
     */
    private ArsdkBlackBoxRequest(@NonNull ArsdkCore arsdkCore, short deviceHandle, @NonNull Listener listener) {
        mArsdkCore = arsdkCore;
        mDeviceHandle = deviceHandle;
        mListener = listener;
        mArsdkCore.dispatchToPomp(() -> {
            if (!mCanceled) {
                mNativePtr = nativeCreate(mArsdkCore.getNativePtr(), mDeviceHandle);
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
     * Called back when some button action is triggered on the remote control device.
     * <p>
     * Called back on <strong>POMP</strong> thread.
     *
     * @param action button action identifier
     */
    @SuppressWarnings("unused") /* native-cb */
    private void onRcButtonAction(int action) {
        mArsdkCore.dispatchToMain(() -> {
            if (!mCanceled) {
                mListener.onRemoteControlButtonAction(action);
            }
        });
    }

    /**
     * Called back when piloting command sent through the remote control changes.
     * <p>
     * Called back on <strong>POMP</strong> thread.
     *
     * @param roll   piloting command roll value
     * @param pitch  piloting command pitch value
     * @param yaw    piloting command yaw value
     * @param gaz    piloting command gaz value
     * @param source identifier of the source that triggered the piloting command change
     */
    @SuppressWarnings("unused") /* native-cb */
    private void onRcPilotingInfo(int roll, int pitch, int yaw, int gaz, int source) {
        mArsdkCore.dispatchToMain(() -> {
            if (!mCanceled) {
                mListener.onRemoteControlPilotingInfo(roll, pitch, yaw, gaz, source);
            }
        });
    }

    /**
     * Called back when the subscription gets revoked for any reason.
     */
    @SuppressWarnings("unused") /* native-cb */
    private void onUnregistered() {
        mNativePtr = 0;
        mCanceled = true;
    }

    /* JNI declarations and setup */
    private native long nativeCreate(long arsdkNativePtr, short deviceHandle);

    private static native void nativeClassInit();

    private static native void nativeCancel(long nativePtr);

    static {
        nativeClassInit();
    }
}
