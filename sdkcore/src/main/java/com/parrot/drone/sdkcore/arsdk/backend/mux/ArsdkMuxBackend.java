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

package com.parrot.drone.sdkcore.arsdk.backend.mux;

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;

/**
 * Wrapper on native arsdk MUX backend.
 */
final class ArsdkMuxBackend {

    /** Notifies when an EOF or error event occurs on the mux fd. */
    interface EofListener {

        /**
         * Called back when an EOF or an error condition has been detected on the mux fd.
         */
        void onEof();
    }

    /** Pointer to native backend. */
    private long mNativePtr;

    /** Listener notified about mux EOF. */
    @NonNull
    private final EofListener mListener;

    /**
     * Constructor.
     *
     * @param arsdkCore          arsdk ctrl instance owning this backend
     * @param fd                 mux fd
     * @param discoverableModels list of discoverable models
     * @param listener           listener notified of mux error and EOF
     */
    ArsdkMuxBackend(@NonNull ArsdkCore arsdkCore, int fd, @ArsdkDevice.Type int[] discoverableModels,
                    @NonNull EofListener listener) {
        mNativePtr = nativeInit(arsdkCore.getNativePtr(), discoverableModels, fd);
        if (mNativePtr == 0) {
            throw new AssertionError();
        }
        mListener = listener;
    }

    /**
     * Starts MUX discovery.
     */
    void startDiscovery() {
        if (mNativePtr == 0) {
            throw new AssertionError();
        }
        nativeStartDiscovery(mNativePtr);
    }

    /**
     * Stops MUX discovery.
     */
    void stopDiscovery() {
        if (mNativePtr == 0) {
            throw new AssertionError();
        }
        nativeStopDiscovery(mNativePtr);
    }

    /**
     * Destructor.
     */
    void destroy() {
        if (mNativePtr == 0) {
            throw new AssertionError();
        }
        nativeRelease(mNativePtr);
        mNativePtr = 0;
    }

    @SuppressWarnings("unused") /* native-cb */
    private void onEof() {
        mListener.onEof();
    }

    /* JNI declarations and setup */
    private native long nativeInit(long arsdkNativePtr, int[] discoverableModels, int fd);

    private native void nativeRelease(long nativePtr);

    private native void nativeStartDiscovery(long nativePtr);

    private native void nativeStopDiscovery(long nativePtr);

    private static native void nativeClassInit();

    static {
        nativeClassInit();
    }
}
