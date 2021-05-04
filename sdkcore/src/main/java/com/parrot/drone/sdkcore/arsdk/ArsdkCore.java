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

package com.parrot.drone.sdkcore.arsdk;

import android.graphics.Rect;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.SdkCore;
import com.parrot.drone.sdkcore.arsdk.backend.ArsdkBackendController;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.pomp.PompLoop;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.util.Set;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG;

/**
 * Arsdk controller. Provides a front-end to access arsdk.
 * <p>
 * All public methods must be called from the same thread (the main thread), all callback will be called into this
 * thread.
 */
public class ArsdkCore {

    /**
     * ArsdkCore listener, notified when a device has been added/removed form arsdk.
     */
    public interface Listener {

        /**
         * Notify that a device has been added.
         *
         * @param device added device
         */
        void onDeviceAdded(@NonNull ArsdkDevice device);

        /**
         * Notify that a device has been removed.
         * <p>
         * At that point the device instance <strong>MUST NOT</strong> be used anymore.
         *
         * @param device removed device
         */
        void onDeviceRemoved(@NonNull ArsdkDevice device);
    }

    /** Backend controllers. */
    @NonNull
    private final ArsdkBackendController[] mBackendControllers;

    /** Listener notified of added/removed devices. */
    final Listener mListener;

    /** Devices, by native device handle. */
    @NonNull
    private final SparseArray<ArsdkDevice> mDevices;

    /** Controller application descriptor, used to provide user-agent info to arsdkcore. */
    @NonNull
    private final String mControllerDescriptor;

    /** Controller application version, used to provide user-agent info to arsdkcore. */
    @NonNull
    private final String mControllerVersion;

    /** True if the video decoding is enabled. */
    private final boolean mVideoDecodingEnabled;

    /** Pomp loop. */
    @Nullable
    private PompLoop mPompLoop;

    /** Arsdkcore native backend pointer. */
    private long mNativePtr;

    /**
     * Set command logging level.
     *
     * @param level requested level
     */
    public static void setCommandLogLevel(@ArsdkCommand.LogLevel int level) {
        nativeSetCommandLogLevel(level);
    }

    /**
     * Constructor.
     *
     * @param backendControllers   array of backend controller to use
     * @param listener             listener notifying device added/removed
     * @param controllerDescriptor controller descriptor formatted to be send during connection.
     * @param controllerVersion    controller application version, formatted to be send during connection.
     * @param videoDecodingEnabled {@code true} to enable the video decoding
     */
    public ArsdkCore(@NonNull ArsdkBackendController[] backendControllers, @NonNull Listener listener,
                     @NonNull String controllerDescriptor, @NonNull String controllerVersion,
                     boolean videoDecodingEnabled) {
        mBackendControllers = backendControllers;
        mListener = listener;
        mControllerDescriptor = controllerDescriptor;
        mControllerVersion = controllerVersion;
        mDevices = new SparseArray<>();
        mVideoDecodingEnabled = videoDecodingEnabled;
        installExceptionHandler();
    }

    /**
     * Starts Arsdkcore.
     */
    public void start() {
        if (mPompLoop == null) {
            mPompLoop = PompLoop.createOnNewThread("arsdkcore-loop");
            mPompLoop.onPomp(() -> {
                mNativePtr = nativeInit(mPompLoop.nativePtr());
                if (mNativePtr == 0) {
                    throw new Error("Failed to create ArsdkCore native backend");
                }
                nativeSetUserAgent(mNativePtr, mControllerDescriptor, mControllerVersion);
                nativeEnableVideoDecoding(mNativePtr, mVideoDecodingEnabled);
                // start all backend controllers
                for (ArsdkBackendController controller : mBackendControllers) {
                    controller.start(this);
                }
            });
        }
    }

    /**
     * Stops ArsdkCore.
     */
    public void stop() {
        if (mPompLoop != null) {
            mPompLoop.onPomp(() -> {
                // stop all backend controllers
                for (ArsdkBackendController controller : mBackendControllers) {
                    controller.stop();
                }
                nativeDispose(mNativePtr);
                mNativePtr = 0;
            });
            mPompLoop.dispose();
            mPompLoop = null;
        }
    }

    /**
     * Provides access to ArsdkCore pomp loop.
     *
     * @return pomp loop
     */
    @NonNull
    public final PompLoop pomp() {
        if (mPompLoop == null) {
            throw new IllegalStateException("Arsdkcore stopped");
        }
        return mPompLoop;
    }

    /**
     * Gets arsdk ctrl native pointer. Used by backend and discovery implementations
     * <p>
     * Must be called on <strong>POMP</strong> thread.
     *
     * @return arsdk ctrl native pointer
     */
    public final long getNativePtr() {
        assertInPompLoop();
        return mNativePtr;
    }

    /**
     * Queues a runnable to be executed on the loop thread.
     *
     * @param runnable runnable to run on the loop thread
     */
    public final void dispatchToPomp(@NonNull Runnable runnable) {
        if (mPompLoop == null) {
            throw new IllegalStateException("Arsdkcore stopped");
        }
        mPompLoop.onPomp(runnable);
    }

    /**
     * Queues a runnable to be executed on the main thread.
     *
     * @param runnable runnable to run on the main thread
     */
    public final void dispatchToMain(@NonNull Runnable runnable) {
        if (mPompLoop == null) {
            throw new IllegalStateException("ArsdkCore stopped");
        }
        mPompLoop.onMain(runnable);
    }

    /**
     * Ensures calling code is executing in the main loop.
     *
     * @throws IllegalStateException in case either this Arsdkcore instance is closed or called from outside of the
     *                               main loop (which is defined as the loop of the looper thread from which this
     *                               Arsdkcore instance was created)
     */
    public final void assertInMainLoop() {
        if (mPompLoop == null || !mPompLoop.inMain()) {
            throw new IllegalStateException("Must be called on main loop");
        }
    }

    /**
     * Ensures calling code is executing in the pomp loop.
     *
     * @throws IllegalStateException in case either this Arsdkcore instance is closed or called from outside of the
     *                               pomp loop
     */
    public final void assertInPompLoop() {
        if (mPompLoop == null || !mPompLoop.inPomp()) {
            throw new IllegalStateException("Must be called on pomp loop");
        }
    }

    /**
     * Installs an uncaught exception handler on main thread in order to force stop pomp loop when an exception
     * crawls up uncaught on the main thread.
     * <p>
     * This avoids sending any more commands to the drone when the application crashes and also allocating pool
     * resources for commands/frames/etc. that won't be released since the main/consumer thread is dying.
     */
    private void installExceptionHandler() {
        Thread currentThread = Thread.currentThread();
        Thread.UncaughtExceptionHandler previousHandler = currentThread.getUncaughtExceptionHandler();
        //noinspection JavaDoc: infortunately, it seems impossible to document a method-inner class.
        final class ExceptionHandler implements Thread.UncaughtExceptionHandler {

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                try {
                    ULog.c(TAG, "Stopping pomp loop due to uncaught " + ex.getClass().getSimpleName()
                                + " on " + thread);
                    stop();
                } finally {
                    if (previousHandler != null) {
                        previousHandler.uncaughtException(thread, ex);
                    }
                }
            }
        }

        if (!(previousHandler instanceof ExceptionHandler)) {
            currentThread.setUncaughtExceptionHandler(new ExceptionHandler());
        }
    }

    @SuppressWarnings("unused") /* native-cb */
    private void onDeviceAdded(short deviceHandle, @NonNull String uid, @ArsdkDevice.Type int type,@NonNull String name,
                               @Backend.Type int backendType, @ArsdkDevice.Api int api) {
        ArsdkDevice device = ArsdkDevice.obtain(this, deviceHandle, uid, type, name, backendType, api);
        if (ULog.i(TAG)) {
            ULog.i(TAG, "Device added: " + device);
        }
        mDevices.put(deviceHandle, device);
        dispatchToMain(() -> mListener.onDeviceAdded(device));
    }

    @SuppressWarnings("unused") /* native-cb */
    private void onDeviceRemoved(short deviceHandle) {
        ArsdkDevice device = mDevices.get(deviceHandle);
        if (ULog.i(TAG)) {
            ULog.i(TAG, "Device removed: " + device);
        }
        mDevices.remove(deviceHandle);
        assert device != null;
        device.dispose();
        dispatchToMain(() -> mListener.onDeviceRemoved(device));
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     */
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
        if (args.isEmpty() || args.contains("--help")) {
            writer.write("\t--arsdkctl: dumps arsdkcore\n");
        } else if (args.contains("--arsdkctl") || args.contains("--all")) {
            writer.write("Arsdkctl:\n");
            writer.write("\tState: " + (mPompLoop == null ? "STOPPED" : "STARTED") + "\n");
            for (ArsdkBackendController controller : mBackendControllers) {
                controller.dump(writer, args, "\t");
            }
            writer.write("\tDevices:\n");
            for (int i = 0, N = mDevices.size(); i < N; i++) {
                ArsdkDevice device = mDevices.valueAt(i);
                writer.write("\t\t" + device + "[" + device.getUid() + "]\n");
                device.dump(writer, args, "\t\t\t");
            }
        }
    }

    /* JNI declarations and setup */
    private native long nativeInit(long pompNativePtr);

    private static native void nativeSetCommandLogLevel(int level);

    private static native void nativeSetUserAgent(long nativePtr, @NonNull String type, @NonNull String name);

    private static native void nativeEnableVideoDecoding(long nativePtr, boolean enable);

    private static native void nativeDispose(long nativePtr);

    private static native void nativeClassInit(@NonNull Class<Rect> rectClass);

    static {
        SdkCore.init();
        nativeClassInit(Rect.class);
    }
}
