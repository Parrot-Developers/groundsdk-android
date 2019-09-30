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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.PooledObject;
import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG_BLE;

/**
 * Manages one BLE connection to a bluetooth device.
 */
final class ArsdkBleConnection {

    /** Interface called back when the device connection state changes. */
    interface Listener {

        /**
         * Notifies that the device managed by the connection is disconnected.
         *
         * @param deviceAddress address of the BLE device
         */
        void onDeviceDisconnection(@NonNull String deviceAddress);
    }

    /* As defined in Delos_BLE_config.h, send/receive inverted */

    /** Sending service identifier. */
    @NonNull
    private static final String ARCOMMAND_SENDING_SERVICE = "fa00";

    /** Receiving service identifier. */
    @NonNull
    private static final String ARCOMMAND_RECEIVING_SERVICE = "fb00";

    /** List of receive channel identifiers (in ascending order for binary search). */
    @NonNull
    private static final byte[] RECEIVE_CHARACTERISTIC_IDS = new byte[] {14, 15, 27, 28};

    /** List of send channel identifiers (in ascending order for binary search). */
    @NonNull
    private static final byte[] SEND_CHARACTERISTIC_IDS = new byte[] {10, 11, 12, 30};

    /** Characteristic configuration descriptor identifier, used to enable receive characteristics notification. */
    @NonNull
    private static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR = UUID.fromString(
            "00002902-0000-1000-8000-00805f9b34fb");

    /** Arsdk ctl instance owning this discovery handler. */
    @NonNull
    private final ArsdkCore mArsdkCore;

    /** Android application context. */
    @NonNull
    private final Context mContext;

    /** Bluetooth device managed by this connection. */
    @NonNull
    private final BluetoothDevice mBtDevice;

    /** Listener notified of device connection state changes. */
    @NonNull
    private final Listener mListener;

    /** Pointer to the native connection handler. */
    private long mNativePtr;

    /** BLE GATT interface used for (dis/)connection. */
    @Nullable
    private BluetoothGatt mGatt;

    /** Maps sending channel identifiers to their corresponding GATT characteristic. */
    @Nullable
    private SparseArray<BluetoothGattCharacteristic> mSendCharacteristics;

    /** Maps receiving channel identifiers to their corresponding GATT characteristic. */
    @Nullable
    private SparseArray<BluetoothGattCharacteristic> mReceiveCharacteristics;

    /** Internal connection state. */
    private enum State {

        /** Device is disconnected. */
        DISCONNECTED,

        /** Device is connecting. */
        CONNECTING,

        /** Device is connected. */
        CONNECTED,

        /** Device connection failed. */
        CONNECTION_FAILED
    }

    /** Current connection state. */
    @NonNull
    private State mState;

    /**
     * Constructor.
     *
     * @param backend   BLE backend owning this connection
     * @param address   address of the bluetooth device managed by this connection
     * @param nativePtr pointer to the native connection handler
     * @param listener  listener notified of connection state changes
     */
    ArsdkBleConnection(@NonNull ArsdkBleBackend backend, @NonNull String address, long nativePtr,
                       @NonNull Listener listener) {
        mNativePtr = nativeInit(nativePtr);
        if (mNativePtr == 0) {
            throw new AssertionError();
        }
        mArsdkCore = backend.getArsdkCore();
        mContext = backend.getContext();
        mBtDevice = backend.getBluetoothManager().getAdapter().getRemoteDevice(address);
        mListener = listener;
        mState = State.DISCONNECTED;
    }

    /**
     * Closes the connection. <br/>
     * <strong>Connection must be considered destroyed, and not reused after this method is called.</strong>
     */
    void close() {
        nativeRelease(mNativePtr);
        mNativePtr = 0;
    }

    /**
     * Starts the connection.
     */
    void start() {
        if (mNativePtr == 0) {
            throw new AssertionError();
        }

        if (ULog.i(TAG_BLE)) {
            ULog.i(TAG_BLE, "Starting connection: " + mBtDevice);
        }

        assert mGatt == null;
        mGatt = mBtDevice.connectGatt(mContext, false, mDeviceConnectionListener);
        mState = State.CONNECTING;
        nativeConnecting(mNativePtr);
    }

    /**
     * Stops the connection.
     */
    void stop() {
        if (mNativePtr == 0) {
            throw new AssertionError();
        }

        if (ULog.i(TAG_BLE)) {
            ULog.i(TAG_BLE, "Stopping connection: " + mBtDevice);
        }

        assert mGatt != null;
        // Connection will be destroyed immediately, we can't afford here to wait for the GATT disconnection callback,
        // which may or might not be called anyway.
        mGatt.close();

        mGatt = null;
        mSendCharacteristics = null;
        mReceiveCharacteristics = null;

        if (mState == State.CONNECTION_FAILED) {
            nativeConnectionFailed(mNativePtr);
        } else {
            nativeDisconnected(mNativePtr);
            mState = State.DISCONNECTED;
        }
    }

    /**
     * Enable notification for the next receive characteristic. <br/>
     * When all receive characteristics are enabled, state transits to {@link State#CONNECTED}
     */
    private void enableNextNotification() {
        assert mGatt != null;
        assert mReceiveCharacteristics != null;

        if (mReceiveCharacteristics.size() > 0) {
            // enable next characteristic
            BluetoothGattCharacteristic characteristic = mReceiveCharacteristics.valueAt(0);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mGatt.writeDescriptor(descriptor);
            mGatt.setCharacteristicNotification(characteristic, true);
            // characteristic will be popped out when writes complete
        } else if (mSendCharacteristics != null) {
            // receive characteristics are all enabled, and send characteristics are all found, we are ready.
            mArsdkCore.dispatchToPomp(mConnectedNotification);
        }
    }

    /** Listens to GATT events, such as connection state changes and service discovery updates. */
    private final BluetoothGattCallback mDeviceConnectionListener = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                // continue connection process
                gatt.discoverServices();
            } else {
                // notify connection failure or disconnection
                mArsdkCore.dispatchToPomp(status == BluetoothGatt.GATT_SUCCESS
                        ? mDisconnectedNotification
                        : mConnectionFailureNotification);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }
            // check if we can find our services of interest
            for (BluetoothGattService service : gatt.getServices()) {
                String servicePostfix = service.getUuid().toString().substring(4);
                if (servicePostfix.startsWith(ARCOMMAND_SENDING_SERVICE) && mSendCharacteristics == null) {
                    mSendCharacteristics = buildCharacteristicsMap(service, SEND_CHARACTERISTIC_IDS);
                    if (mReceiveCharacteristics != null && mReceiveCharacteristics.size() == 0) {
                        // receive characteristics are all enabled, and send characteristics are all found, we are ready
                        mArsdkCore.dispatchToPomp(mConnectedNotification);
                    }
                } else if (servicePostfix.startsWith(ARCOMMAND_RECEIVING_SERVICE) && mReceiveCharacteristics == null) {
                    mReceiveCharacteristics = buildCharacteristicsMap(service, RECEIVE_CHARACTERISTIC_IDS);
                    // enable notification on receive characteristics
                    enableNextNotification();
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // pick-up the proper channel
            byte id = getCharacteristicId(characteristic);
            // read data from the characteristic
            byte[] data = characteristic.getValue();
            // JNI implementation requires that we use a direct byte buffer to transmit data
            ByteBuffer buffer = ByteBuffer.allocateDirect(data.length).put(data);
            // send data to low level
            mRecvDataPool.obtainEntry().dispatch(id, buffer);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            assert mReceiveCharacteristics != null;

            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                mReceiveCharacteristics.removeAt(mReceiveCharacteristics.indexOfValue(characteristic));
            }

            enableNextNotification();
        }
    };

    /**
     * Builds a map of characteristics for each of the given communication channels.
     *
     * @param service  discovered GATT service
     * @param channels list of channels to find corresponding characteristics for
     *
     * @return a map of GATT characteristics, by channel id
     */
    private static SparseArray<BluetoothGattCharacteristic> buildCharacteristicsMap(
            @NonNull BluetoothGattService service, @NonNull byte[] channels) {
        SparseArray<BluetoothGattCharacteristic> characteristics = new SparseArray<>();

        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
            byte id = getCharacteristicId(characteristic);
            if (Arrays.binarySearch(channels, id) >= 0) {
                characteristics.append(id, characteristic);
            }
        }

        return characteristics;
    }

    /**
     * Gets the channel identifier from the given characteristic.
     *
     * @param characteristic the characteristic to extract the identifier from
     *
     * @return the channel identifier
     */
    private static byte getCharacteristicId(@NonNull BluetoothGattCharacteristic characteristic) {
        return Byte.parseByte(characteristic.getUuid().toString().substring(6, 8), 16);
    }

    /** Posted to arsdk control pomp loop to notify a connection event. */
    private final Runnable mConnectedNotification = new Runnable() {

        @Override
        public void run() {
            if (mNativePtr == 0) {
                throw new AssertionError();
            }
            if (ULog.i(TAG_BLE)) {
                ULog.i(TAG_BLE, "Device connected: " + mBtDevice);
            }
            mState = State.CONNECTED;
            nativeConnected(mNativePtr);
        }
    };

    /** Posted to arsdk control pomp loop to notify a disconnection event. */
    private final Runnable mDisconnectedNotification = new Runnable() {

        @Override
        public void run() {
            if (ULog.i(TAG_BLE)) {
                ULog.i(TAG_BLE, "Device disconnected: " + mBtDevice);
            }
            mState = State.DISCONNECTED;
            mListener.onDeviceDisconnection(mBtDevice.getAddress());
        }
    };

    /** Posted to arsdk control pomp loop to notify a connection failure event. */
    private final Runnable mConnectionFailureNotification = new Runnable() {

        @Override
        public void run() {
            if (ULog.i(TAG_BLE)) {
                ULog.i(TAG_BLE, "Device connection failed: " + mBtDevice);
            }
            mState = State.CONNECTION_FAILED;
            mListener.onDeviceDisconnection(mBtDevice.getAddress());
        }
    };

    /**
     * Sends data to the remote device.
     *
     * @param id          channel identifier for the data to send
     * @param type        data type
     * @param seq         data sequence number
     * @param payload     data payload
     * @param extraHeader extra data header
     */
    @SuppressWarnings("unused") /* native-cb */
    private void sendData(byte id, byte type, byte seq, @NonNull ByteBuffer payload, @NonNull ByteBuffer extraHeader) {
        assert mGatt != null;
        assert mSendCharacteristics != null;

        // pick-up the proper characteristic
        BluetoothGattCharacteristic characteristic = mSendCharacteristics.get(id);
        // write the data
        characteristic.setValue(
                ByteBuffer.allocate(payload.remaining() + extraHeader.remaining() + 2)
                          .put(type).put(seq).put(extraHeader).put(payload).array());
        // send via bluetooth
        mGatt.writeCharacteristic(characteristic);

    }

    /**
     * Pooled container used to dispatch received data to the arsdk pomp loop.
     */
    private final class RecvData extends PooledObject {

        /** Received data channel identifier. */
        private byte mId;

        /** Received data buffer. */
        private ByteBuffer mBuffer;

        /**
         * Constructor.
         *
         * @param pool pool owning the entry
         */
        RecvData(@NonNull Pool pool) {
            super(pool);
        }

        /**
         * Dispatches received data to the arsdk pomp loop.
         *
         * @param id     received data channel identifier
         * @param buffer received data buffer
         */
        void dispatch(byte id, @NonNull ByteBuffer buffer) {
            mId = id;
            mBuffer = buffer;
            mArsdkCore.dispatchToPomp(mRunnable);
        }

        @Override
        protected void doRelease() {
            mBuffer = null;
        }

        /** Runnable executed on arsdk pomp loop. Sends received data to low-level. */
        private final Runnable mRunnable = new Runnable() {

            @Override
            public void run() {
                if (mNativePtr == 0) {
                    throw new AssertionError();
                }
                nativeReceiveData(mNativePtr, mId, mBuffer);
                release();
            }
        };
    }

    /** {@link RecvData} container pool. */
    private final PooledObject.Pool<RecvData> mRecvDataPool = new PooledObject.Pool<RecvData>(
            "BLE.RecvData", 1, PooledObject.Pool.DEFAULT_POOL_MAX_SIZE) {

        @NonNull
        @Override
        protected RecvData createEntry() {
            return new RecvData(this);
        }
    };

    @NonNull
    @Override
    public String toString() {
        return mState + " " + mBtDevice;
    }

    /* JNI declarations and setup */

    private native long nativeInit(long nativePtr);

    private native void nativeRelease(long nativePtr);

    private native void nativeDisconnected(long nativePtr);

    private native void nativeConnecting(long nativePtr);

    private native void nativeConnected(long nativePtr);

    private native void nativeConnectionFailed(long nativePtr);

    private native void nativeReceiveData(long nativePtr, byte id, ByteBuffer data);

    private static native void nativeClassInit();

    static {
        nativeClassInit();
    }
}
