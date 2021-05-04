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

package com.parrot.drone.sdkcore.arsdk.device;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.backend.ArsdkBackendController;
import com.parrot.drone.sdkcore.arsdk.backend.net.ArsdkWifiBackendController;
import com.parrot.drone.sdkcore.arsdk.blackbox.ArsdkBlackBoxRequest;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkNoAckCmdEncoder;
import com.parrot.drone.sdkcore.arsdk.crashml.ArsdkCrashmlDownloadRequest;
import com.parrot.drone.sdkcore.arsdk.firmware.ArsdkFirmwareUploadRequest;
import com.parrot.drone.sdkcore.arsdk.flightlog.ArsdkFlightLogDownloadRequest;
import com.parrot.drone.sdkcore.arsdk.stream.ArsdkDeviceStreamController;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.net.SocketFactory;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG_DEVICE;

/**
 * Device object.
 * <p>
 * Handles device connection and command handling.
 */
public class ArsdkDevice {

    /** Special unique identifier for devices running in the simulator. */
    public static final String SIMULATOR_UID = "000000000000000000";

    /** Camera live stream url. TODO: until we support multiple cameras with ids */
    public static final String LIVE_URL = "live";

    /** Int definition of a device type. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef
    public @interface Type {}

    /** Value for an unknown device type. */
    @Type
    public static final int TYPE_UNKNOWN = -1;

    /** Int definition of connection cancel reasons. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REASON_CANCELED_LOCALLY, REASON_CANCELED_BY_REMOTE, REASON_REJECTED_BY_REMOTE})
    public @interface ConnectionCancelReason {}

    /* Numerical device type values MUST be kept in sync with C enum arsdk_conn_cancel_reason */

    /** Connection cancelled on local request. */
    public static final int REASON_CANCELED_LOCALLY = 0;

    /** Remote cancelled the connection request. */
    public static final int REASON_CANCELED_BY_REMOTE = 1;

    /** Remote rejected the connection request. */
    public static final int REASON_REJECTED_BY_REMOTE = 2;

    /** Int definition of API capabilities. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({API_UNKNOWN, API_FULL, API_UPDATE_ONLY})
    public @interface Api {}

    /* Numerical device type values MUST be kept in sync with C enum arsdk_device_api */

    /** API capabilities unknown. */
    public static final int API_UNKNOWN = 0;

    /** Full API supported. */
    public static final int API_FULL = 1;

    /** Update API only. */
    public static final int API_UPDATE_ONLY = 2;

    /**
     * Device listener, notified of device event.
     */
    public interface Listener {

        /**
         * Called when the device is connecting (i.e sending connection json).
         * <p>
         * Called on <strong>MAIN</strong> thread.
         */
        void onConnecting();

        /**
         * Called when the device is connected (i.e device json has be received). At this time the command interface
         * has been created and the device may start to send commands.
         * <p>
         * Called on <strong>MAIN</strong> thread.
         */
        void onConnected();

        /**
         * Device has been disconnected.
         * <p>
         * Called on <strong>MAIN</strong> thread.
         *
         * @param removing {@code true} when the device has been disconnected because it is about to be removed from
         *                 arsdk, otherwise {@code false}
         */
        void onDisconnected(boolean removing);

        /**
         * Connection process has been canceled.
         * <p>
         * Called on <strong>MAIN</strong> thread.
         *
         * @param reason   reason why the connection process has been canceled
         * @param removing {@code true} when the connection is canceled because the device is about to be removed from
         *                 arsdk, otherwise {@code false}
         */
        void onConnectionCanceled(@ConnectionCancelReason int reason, boolean removing);

        /**
         * Notify that the link is down, command cannot be sent/received.
         * <p>
         * Called on <strong>MAIN</strong> thread.
         */
        void onLinkDown();

        /**
         * A command has been received.
         * <p>
         * Called on <strong>MAIN</strong> thread.
         *
         * @param command received command
         */
        void onCommandReceived(@NonNull ArsdkCommand command);
    }

    /**
     * Obtains an ArsdkDevice.
     * <p>
     * Must be called on <strong>POMP</strong> thread.
     *
     * @param arsdkCore    ArsdkCore instance
     * @param deviceHandle native device handle
     * @param uid          device uid
     * @param type         device type
     * @param name         device name
     * @param backendType  device backend type
     * @param api          device API capabilities
     *
     * @return a new ArsdkDevice instance that represents the native device with the given handle
     */
    @NonNull
    public static ArsdkDevice obtain(@NonNull ArsdkCore arsdkCore, short deviceHandle, @NonNull String uid,
                                     @ArsdkDevice.Type int type, @NonNull String name,
                                     @Backend.Type int backendType, @ArsdkDevice.Api int api) {
        return new ArsdkDevice(arsdkCore, deviceHandle, uid, type, name, backendType, api);
    }

    /** ArsdkCore instance. */
    @NonNull
    private final ArsdkCore mArsdkCore;

    /** Device uid. */
    @NonNull
    private final String mUid;

    /** Device type. */
    @ArsdkDevice.Type
    private final int mType;

    /** Backend type. */
    @Backend.Type
    private final int mBackendType;

    /** API capabilities. */
    @ArsdkDevice.Api
    private int mApi;

    /** Device name. */
    @NonNull
    private final String mName;

    /** Device native handle. */
    private final short mNativeHandle;

    /** ArsdkDevice native backend pointer. */
    private long mNativePtr;

    /** Device event listener. */
    @VisibleForTesting
    @Nullable
    Listener mListener;

    /** Non-acknowledged command encoders, run on POMP thread. */
    @NonNull
    private final Set<ArsdkNoAckCmdEncoder> mNoAckEncoders;

    /** Provides and manges access to video streams. */
    @NonNull
    private final ArsdkDeviceStreamController mStreamController;

    /** Current non-acknowledged loop period, in milliseconds. {@code 0} when disabled. */
    private int mNoAckLoopPeriod;

    /**
     * Constructor.
     * <p>
     * Must be called on <strong>POMP</strong> thread.
     *
     * @param arsdkCore    ArsdkCore instance
     * @param nativeHandle device native handle
     * @param uid          device uid
     * @param type         device type
     * @param name         device name
     * @param backendType  device backend type
     * @param api          device API capabilities
     */
    private ArsdkDevice(@NonNull ArsdkCore arsdkCore, short nativeHandle, @NonNull String uid,
                        @ArsdkDevice.Type int type, @NonNull String name,
                        @Backend.Type int backendType, @ArsdkDevice.Api int api) {
        mArsdkCore = arsdkCore;
        mNativeHandle = nativeHandle;
        mUid = uid;
        mType = type;
        mName = name;
        mBackendType = backendType;
        mApi = api;
        mNativePtr = nativeInit(mArsdkCore.getNativePtr(), mNativeHandle);
        if (mNativePtr == 0) {
            throw new AssertionError("Failed to create ArsdkDevice native backend");
        }
        mNoAckEncoders = new CopyOnWriteArraySet<>();
        mStreamController = new ArsdkDeviceStreamController(mArsdkCore, mNativeHandle);
        if (ULog.d(TAG_DEVICE)) {
            ULog.d(TAG_DEVICE, "ArsdkDevice init [handle: " + this + " ,uid: " + mUid
                               + ", type: " + mType + ", name: " + mName + "]");
        }
    }

    /**
     * Gets device uid.
     *
     * @return the device uid
     */
    @NonNull
    public final String getUid() {
        return mUid;
    }

    /**
     * Gets device type.
     *
     * @return the device type
     */
    @ArsdkDevice.Type
    public final int getType() {
        return mType;
    }

    /**
     * Gets device name.
     *
     * @return the device name
     */
    @NonNull
    public final String getName() {
        return mName;
    }

    /**
     * Gets the backend type.
     *
     * @return the backend type
     */
    @Backend.Type
    public int getBackendType() {
        return mBackendType;
    }

    /**
     * Gets device API capabilities.
     *
     * @return the device type
     */
    @ArsdkDevice.Api
    public int getApiCapabilities() {
        return mApi;
    }

    /**
     * Initiates connection with the device.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     *
     * @param listener listener to forward device events to
     */
    public void connect(@NonNull Listener listener) {
        if (ULog.d(TAG_DEVICE)) {
            ULog.d(TAG_DEVICE, "ArsdkDevice connect [handle: " + this + "]");
        }
        if (mListener == null) {
            mListener = listener;
            mArsdkCore.dispatchToPomp(() -> {
                if (mNativePtr != 0) {
                    if (!nativeConnect(mNativePtr)) {
                        onConnectionCanceled(REASON_CANCELED_LOCALLY, false);
                    }
                } else if (ULog.i(TAG_DEVICE)) {
                    ULog.i(TAG_DEVICE, "Device destroyed");
                }
            });
        } else if (ULog.w(TAG_DEVICE)) {
            ULog.w(TAG_DEVICE, "Device already connected or connecting");
        }
    }

    /**
     * Initiates disconnection from the device.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     */
    public void disconnect() {
        if (ULog.d(TAG_DEVICE)) {
            ULog.d(TAG_DEVICE, "ArsdkDevice disconnect [handle: " + this + "]");
        }
        if (mListener != null) {
            mArsdkCore.dispatchToPomp(() -> {
                if (mNativePtr != 0) {
                    nativeDisconnect(mNativePtr);
                } else if (ULog.i(TAG_DEVICE)) {
                    ULog.i(TAG_DEVICE, "Device destroyed");
                }
            });
        } else if (ULog.w(TAG_DEVICE)) {
            ULog.w(TAG_DEVICE, "Device not connected");
        }
    }

    /**
     * Sends a command to the device.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     *
     * @param command command to send
     */
    public void sendCommand(@NonNull ArsdkCommand command) {
        mArsdkCore.dispatchToPomp(mSendCommandRunnablePool.obtainEntry().init(command));
    }

    /**
     * Configures the non-acknowledged command loop period.
     * <p>
     * If the period changes, and encoders are currently {@link #registerNoAckCommandEncoder registered}, the command
     * loop is stopped if it was started, then started again (if the new period is strictly positive).
     * <p>
     * Loop period is reset to zero when the connection to the device closes for any reason.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     *
     * @param period loop period, in milliseconds. {@code 0} to stop the loop
     */
    public void setNoAckCommandLoopPeriod(int period) {
        if (mNoAckLoopPeriod != period) {
            if (mNoAckLoopPeriod != 0 && !mNoAckEncoders.isEmpty()) {
                stopNoAckCommandLoop();
            }
            mNoAckLoopPeriod = period;
            if (mNoAckLoopPeriod > 0 && !mNoAckEncoders.isEmpty()) {
                startNoAckCommandLoop(mNoAckLoopPeriod);
            }
        }
    }

    /**
     * Registers an encoder to be executed in the non-acknowledged command loop.
     * <p>
     * If a strictly positive loop period is currently {@link #setNoAckCommandLoopPeriod setup}, and this is the first
     * registered encoder, the loop is started.
     * <p>
     * All registered encoders are unregistered when the connection to the device closes for any reason.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     *
     * @param encoder non-acknowledged command encoder to register
     */
    public void registerNoAckCommandEncoder(@NonNull ArsdkNoAckCmdEncoder encoder) {
        if (mNoAckEncoders.add(encoder)) {
            if (mNoAckEncoders.size() == 1 && mNoAckLoopPeriod > 0) {
                startNoAckCommandLoop(mNoAckLoopPeriod);
            }
        }
    }

    /**
     * Unregisters an encoder from being executed in the non-acknowledged command loop.
     * <p>
     * If the loop is started and this is the last registered encoder, the loop is stopped.
     * <p>
     * All registered encoders are unregistered when the connection to the device closes for any reason.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     *
     * @param encoder non-acknowledged command encoder to unregister
     */
    public void unregisterNoAckCommandEncoder(@NonNull ArsdkNoAckCmdEncoder encoder) {
        if (mNoAckEncoders.remove(encoder)) {
            if (mNoAckEncoders.isEmpty() && mNoAckLoopPeriod > 0) {
                stopNoAckCommandLoop();
            }
        }
    }

    /**
     * Requests a video stream to be opened from the connected device.
     *
     * @param url    video stream URL
     * @param track  stream track to select, {@code null} to select default track, if any
     * @param client client notified of video stream events
     *
     * @return a new, opening video stream instance
     */
    @NonNull
    public SdkCoreStream openVideoStream(@NonNull String url, @Nullable String track,
                                         @NonNull SdkCoreStream.Client client) {
        return mStreamController.openStream(url, track, client);
    }

    /**
     * Requests download of the connected device's crashml reports.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     *
     * @param deviceType type of device whose crashmls must be downloaded.
     * @param path       path of the crashmls folder where download.
     * @param listener   listener that will be called back upon request progress and completion
     *
     * @return an ArsdkRequest, that can be canceled.
     */
    @NonNull
    public ArsdkRequest downloadCrashml(@Type int deviceType, @NonNull String path,
                                        @NonNull ArsdkCrashmlDownloadRequest.Listener listener) {
        mArsdkCore.assertInMainLoop();
        return ArsdkCrashmlDownloadRequest.create(mArsdkCore, mNativeHandle, deviceType, path, listener);
    }

    /**
     * Requests download of the connected device's flight logs.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     *
     * @param deviceType type of device whose flight logs must be downloaded
     * @param path       destination folder where flight logs are downloaded
     * @param listener   listener that will be called back upon request progress and completion
     *
     * @return an ArsdkRequest, that can be canceled.
     */
    @NonNull
    public ArsdkRequest downloadFlightLog(@Type int deviceType, @NonNull String path,
                                          @NonNull ArsdkFlightLogDownloadRequest.Listener listener) {
        mArsdkCore.assertInMainLoop();
        return ArsdkFlightLogDownloadRequest.create(mArsdkCore, mNativeHandle, deviceType, path, listener);
    }

    /**
     * Requests upload of a firmware file to the connected device.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     *
     * @param deviceType   type of device where firmware file must be uploaded
     * @param firmwarePath absolute path to the firmware file to upload
     * @param listener     listener that will be called back upon request progress and completion
     *
     * @return an ArsdkRequest, that can be canceled.
     */
    @NonNull
    public ArsdkRequest uploadFirmware(@Type int deviceType, @NonNull String firmwarePath,
                                       @NonNull ArsdkFirmwareUploadRequest.Listener listener) {
        mArsdkCore.assertInMainLoop();
        return ArsdkFirmwareUploadRequest.create(mArsdkCore, mNativeHandle, deviceType, firmwarePath, listener);
    }

    /**
     * Requests to receive black box data from the controlled device.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     *
     * @param listener listener that will be called back each time new black box data is available
     *
     * @return an ArsdkRequest, that can be canceled
     */
    @NonNull
    public ArsdkRequest subscribeToBlackBox(@NonNull ArsdkBlackBoxRequest.Listener listener) {
        mArsdkCore.assertInMainLoop();
        return ArsdkBlackBoxRequest.create(mArsdkCore, mNativeHandle, listener);
    }

    /**
     * Creates a TCP proxy with the device.
     * <p>
     * When complete, {@link ArsdkTcpProxy.Listener#onComplete} is called on the main thread.
     * <p>
     * Must be called on <strong>MAIN</strong> thread.
     *
     * @param deviceType device type
     * @param port       port to access
     * @param listener   completion listener
     *
     * @return a new TCP proxy instance
     */
    public ArsdkTcpProxy createTcpProxy(@Type int deviceType, int port, @NonNull ArsdkTcpProxy.Listener listener) {
        SocketFactory socketFactory = null;
        ArsdkBackendController controller = nativeGetBackendController(mNativePtr);
        if (controller instanceof ArsdkWifiBackendController) {
            socketFactory = ((ArsdkWifiBackendController) controller).getSocketFactory();
        }
        mArsdkCore.assertInMainLoop();
        return ArsdkTcpProxy.create(mArsdkCore, mNativeHandle, deviceType, port, listener, socketFactory);
    }

    /**
     * Destroys the ArsdkDevice native backend.
     * <p>
     * Must be called on <strong>POMP</strong> thread.
     * <p>
     * ArsdkDevice instance MUST be considered destroyed and not used anymore after calling this method.
     */
    public final void dispose() {
        if (ULog.d(TAG_DEVICE)) {
            ULog.d(TAG_DEVICE, "Dispose ArsdkDevice [handle: " + this + "]");
        }
        if (mNativePtr == 0) {
            throw new IllegalStateException("Device already destroyed");
        }
        nativeDispose(mNativePtr);
        mNativePtr = 0;
        mArsdkCore.dispatchToMain(() -> {
            if (mListener != null) {
                mListener.onConnectionCanceled(REASON_CANCELED_LOCALLY, true);
                mListener = null;
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArsdkDevice that = (ArsdkDevice) o;

        return mNativeHandle == that.mNativeHandle;
    }

    @Override
    public int hashCode() {
        return mNativeHandle;
    }

    @NonNull
    @Override
    public String toString() {
        return Integer.toString(mNativeHandle & 0xFFFF);
    }

    /**
     * Gets the native device handle.
     *
     * @return the native device handle
     */
    short getHandle() {
        return mNativeHandle;
    }

    /** Pool of runnables used to dispatch commands to send on the pomp thread. */
    private final CommandRunnablePool mSendCommandRunnablePool = new CommandRunnablePool("SendCmdPool") {

        @Override
        void doWithCommand(@NonNull ArsdkCommand command) {
            if (mNativePtr != 0) {
                nativeSendCommand(mNativePtr, command.getNativePtr());
            } else if (ULog.i(TAG_DEVICE)) {
                ULog.i(TAG_DEVICE, "Device destroyed");
            }
        }
    };

    /** Pool of runnables used to dispatch received commands on the main thread. */
    private final CommandRunnablePool mRecvCommandRunnablePool = new CommandRunnablePool("RecvCmdPool") {

        @Override
        void doWithCommand(@NonNull ArsdkCommand command) {
            assert mListener != null;
            mListener.onCommandReceived(command);
        }
    };

    /**
     * Starts the non-acknowledged command loop.
     * <p>
     * This starts an internal loop that will call {@link #onNoAckCmdTimerTick()} method regularly, based on the
     * given period.
     *
     * @param periodMs period of the command loop, in milliseconds
     */
    private void startNoAckCommandLoop(int periodMs) {
        mArsdkCore.dispatchToPomp(() -> {
            if (mNativePtr == 0) {
                ULog.i(TAG_DEVICE, "Device destroyed");
            } else {
                nativeStartNoAckCmdTimer(mNativePtr, periodMs);
            }
        });
    }

    /**
     * Stops the non-acknowledged command loop.
     */
    private void stopNoAckCommandLoop() {
        mArsdkCore.dispatchToPomp(() -> {
            if (mNativePtr != 0) {
                nativeStopNoAckCmdTimer(mNativePtr);
            } else if (ULog.i(TAG_DEVICE)) {
                ULog.i(TAG_DEVICE, "Device destroyed");
            }
        });
    }

    @SuppressWarnings("unused") /* native-cb */
    private void onConnecting() {
        if (ULog.d(TAG_DEVICE)) {
            ULog.d(TAG_DEVICE, "ArsdkDevice connecting [handle: " + this + "]");
        }
        mArsdkCore.dispatchToMain(() -> {
            assert mListener != null;
            mListener.onConnecting();
        });
    }

    @SuppressWarnings("unused") /* native-cb */
    private void onConnected(int api) {
        if (ULog.d(TAG_DEVICE)) {
            ULog.d(TAG_DEVICE, "ArsdkDevice connected [handle: " + this + "]");
        }
        mArsdkCore.dispatchToMain(() -> {
            /* Update device info. */
            mApi = api;

            assert mListener != null;
            mListener.onConnected();
        });
    }

    @SuppressWarnings("unused") /* native-cb */
    private void onDisconnected(boolean removing) {
        if (ULog.d(TAG_DEVICE)) {
            ULog.d(TAG_DEVICE, "ArsdkDevice disconnected [handle: " + this + ", removing: " + removing + "]");
        }
        mArsdkCore.dispatchToMain(() -> {
            mStreamController.closeStreams();
            mNoAckEncoders.clear();
            mNoAckLoopPeriod = 0;
            assert mListener != null;
            Listener listener = mListener;
            mListener = null;
            listener.onDisconnected(removing);
        });
    }

    @SuppressWarnings("unused") /* native-cb */
    private void onConnectionCanceled(@ConnectionCancelReason int reason, boolean removing) {
        if (ULog.d(TAG_DEVICE)) {
            ULog.d(TAG_DEVICE, "ArsdkDevice connection canceled [handle: " + this + ", reason: " + reason
                               + ", removing: " + removing + "]");
        }
        mArsdkCore.dispatchToMain(() -> {
            mStreamController.closeStreams();
            mNoAckEncoders.clear();
            mNoAckLoopPeriod = 0;
            assert mListener != null;
            Listener listener = mListener;
            mListener = null;
            listener.onConnectionCanceled(reason, removing);
        });
    }

    @SuppressWarnings("unused") /* native-cb */
    private void onLinkDown() {
        if (ULog.d(TAG_DEVICE)) {
            ULog.d(TAG_DEVICE, "ArsdkDevice link down [handle: " + this + "]");
        }
        mArsdkCore.dispatchToMain(() -> {
            mStreamController.closeStreams();
            mNoAckEncoders.clear();
            mNoAckLoopPeriod = 0;
            assert mListener != null;
            mListener.onLinkDown();
        });
    }

    @SuppressWarnings("unused") /* native-cb */
    private void onCommandReceived(long cmdNativePtr) {
        mArsdkCore.dispatchToMain(
                mRecvCommandRunnablePool.obtainEntry().init(ArsdkCommand.Pool.DEFAULT.obtain(cmdNativePtr)));
    }

    @SuppressWarnings("unused") /* native-cb */
    private void onNoAckCmdTimerTick() {
        // TODO: better use classic for-loop, otherwise an iterator is allocated for each tick.
        // TODO  However, to do this we need to abandon COWArraySet and synchronize access manually.
        for (ArsdkNoAckCmdEncoder encoder : mNoAckEncoders) {
            ArsdkCommand cmd = encoder.encodeNoAckCmd();
            if (cmd != null) {
                nativeSendCommand(mNativePtr, cmd.getNativePtr());
                cmd.release();
            }
        }
    }

    /* JNI declarations and setup */
    private native long nativeInit(long arsdkNativePtr, short deviceHandle);

    private static native void nativeClassInit();

    private static native boolean nativeConnect(long nativePtr);

    private static native void nativeSendCommand(long nativePtr, long cmdNativePtr);

    private static native void nativeStartNoAckCmdTimer(long nativePtr, int periodMs);

    private static native void nativeStopNoAckCmdTimer(long nativePtr);

    private static native ArsdkBackendController nativeGetBackendController(long nativePtr);

    private static native void nativeDisconnect(long nativePtr);

    private static native void nativeDispose(long nativePtr);

    static {
        nativeClassInit();
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     * @param prefix prefix string (usually indent) to prepend to each written dump line
     */
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args, @NonNull String prefix) {
        mStreamController.dump(writer, args, prefix);
    }

    /**
     * Constructor for test mocks.
     *
     * @param nativeHandle device native handle
     * @param uid          device uid
     * @param type         device type
     * @param name         device name
     * @param backendType  device backend type
     * @param api          device API capabilities
     */
    @SuppressWarnings("ConstantConditions")
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    ArsdkDevice(short nativeHandle, @NonNull String uid, @ArsdkDevice.Type int type, @NonNull String name,
                @Backend.Type int backendType, @ArsdkDevice.Api int api) {
        mArsdkCore = null;
        mNativeHandle = nativeHandle;
        mUid = uid;
        mType = type;
        mName = name;
        mBackendType = backendType;
        mApi = api;
        mNoAckEncoders = new CopyOnWriteArraySet<>();
        mStreamController = new ArsdkDeviceStreamController(mArsdkCore, nativeHandle);
    }
}
