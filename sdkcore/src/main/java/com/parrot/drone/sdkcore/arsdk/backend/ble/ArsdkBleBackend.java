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

package com.parrot.drone.sdkcore.arsdk.backend.ble;

import android.bluetooth.BluetoothManager;
import android.content.Context;

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG_BLE;

/**
 * Wrapper on native arsdk backend BLE.
 */
final class ArsdkBleBackend {

    /** Pointer to native backend. */
    private long mNativePtr;

    /** Main Arsdk controller. */
    @NonNull
    private final ArsdkCore mArsdkCore;

    /** Android application context. */
    @NonNull
    private final Context mContext;

    /** Android Bluetooth manager. */
    @NonNull
    private final BluetoothManager mBtManager;

    /** Map of BLE device connections, by device bluetooth address. Managed on arsdkctl loop thread. */
    @NonNull
    private final Map<String, ArsdkBleConnection> mConnections;

    /** BLE discovery handler. */
    @NonNull
    private final ArsdkBleDiscovery mDiscovery;

    /**
     * Constructor.
     *
     * @param arsdkCore          arsdk ctrl instance owning this backend
     * @param context            android application context
     * @param btManager          android bluetooth manager
     * @param discoverableModels list of discoverable models
     */
    ArsdkBleBackend(@NonNull ArsdkCore arsdkCore, @NonNull Context context, @NonNull BluetoothManager btManager,
                    @ArsdkDevice.Type int[] discoverableModels) {
        mNativePtr = nativeInit(arsdkCore.getNativePtr());
        if (mNativePtr == 0) {
            throw new AssertionError();
        }
        mArsdkCore = arsdkCore;
        mContext = context;
        mBtManager = btManager;
        mDiscovery = new ArsdkBleDiscovery(this, discoverableModels);
        mConnections = new HashMap<>();
    }

    /**
     * Destructor.
     */
    void destroy() {
        nativeRelease(mNativePtr);
        mNativePtr = 0;
    }

    /**
     * Starts BLE discovery.
     */
    void startDiscovery() {
        mDiscovery.start();
    }

    /**
     * Stops BLE discovery.
     */
    void stopDiscovery() {
        mDiscovery.stop();
    }

    /**
     * Gets parent native backend pointer.
     *
     * @return parent native backend pointer
     */
    long getParentNativePtr() {
        if (mNativePtr == 0) {
            throw new AssertionError();
        }
        return nativeGetParent(mNativePtr);
    }

    /**
     * Gets the android application context.
     *
     * @return the android application context
     */
    @NonNull
    Context getContext() {
        return mContext;
    }

    /**
     * Gets the android bluetooth manager instance.
     *
     * @return the bluetooth manager instance
     */
    @NonNull
    BluetoothManager getBluetoothManager() {
        return mBtManager;
    }

    /**
     * Gets the arsdk controller instance.
     *
     * @return the arsdk controller instance
     */
    @NonNull
    ArsdkCore getArsdkCore() {
        return mArsdkCore;
    }

    /**
     * Called back from native, when a BLE connection with the given device must be created.
     *
     * @param address   MAC address of the bluetooth device to connect with
     * @param nativePtr pointer to the native connection to link with
     *
     * @return {@code true} if the connection could be created, {@code false otherwise}
     */
    @SuppressWarnings("unused") /* native-cb */
    private boolean openConnection(@NonNull String address, long nativePtr) {
        if (mConnections.containsKey(address)) {
            ULog.w(TAG_BLE, "Tried to open an existing connection: " + address);
            return false;
        }

        if (ULog.i(TAG_BLE)) {
            ULog.i(TAG_BLE, "Opening new connection: " + address);
        }

        ArsdkBleConnection connection = new ArsdkBleConnection(this, address, nativePtr, mConnectionListener);
        mConnections.put(address, connection);
        connection.start();
        return true;
    }

    /**
     * Called back from native, when a BLE connection with the given device must be closed.
     *
     * @param address MAC address of the bluetooth device whose connection must be closed
     */
    @SuppressWarnings("unused") /* native-cb */
    private void closeConnection(@NonNull String address) {
        ArsdkBleConnection connection = mConnections.remove(address);
        if (connection == null) {
            ULog.w(TAG_BLE, "Tried to close non-existing connection: " + address);
            return;
        }

        if (ULog.i(TAG_BLE)) {
            ULog.i(TAG_BLE, "Closing connection: " + address);
        }

        connection.stop();
        connection.close();
    }

    /**
     * Called back by connections to notify that the connection to the device is lost. <br/>
     * Allows proper bookkeeping of connections in the backend. MUST be called on arsdk loop thread
     */
    private final ArsdkBleConnection.Listener mConnectionListener = this::closeConnection;

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     * @param prefix prefix string (usually indent) to prepend to each written dump line
     */
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args, @NonNull String prefix) {
        mDiscovery.dump(writer, args, prefix);
        writer.write(prefix + "Connections: " + mConnections.size() + "\n");
        for (ArsdkBleConnection connection : mConnections.values()) {
            writer.write(prefix + "\t" + connection + "\n");
        }
    }

    /* JNI declarations and setup */

    private native long nativeInit(long arsdkNativePtr);

    private native void nativeRelease(long nativePtr);

    private native long nativeGetParent(long nativePtr);

    private static native void nativeClassInit();

    static {
        nativeClassInit();
    }
}
