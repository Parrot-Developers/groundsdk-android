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

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.arsdk.backend.ArsdkDiscovery;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG_BLE;

/**
 * BLE devices discovery.
 */
final class ArsdkBleDiscovery extends ArsdkDiscovery {

    /** Discovery round timeout, in milliseconds. */
    private static final long DISCOVERY_TIMEOUT = TimeUnit.SECONDS.toMillis(5);

    /** Parrot bluetooth manufacturer unique identifier. */
    private static final int PARROT_MANUFACTURER_ID = 0x0043;

    /** Parrot usb vendor unique identifier. */
    private static final int PARROT_USB_VENDOR = 0x19cf;

    /** Settings for BLE discovery. */
    @NonNull
    private static final ScanSettings SCAN_SETTINGS = new ScanSettings.Builder().build();

    /** arsdk ctl instance owning this discovery handler. */
    @NonNull
    private final ArsdkCore mArsdkCore;

    /** Android Bluetooth manager. */
    @NonNull
    private final BluetoothManager mBtManager;

    /** Bluetooth adapter frontend. */
    @NonNull
    private final BluetoothLeScanner mBleScanner;

    /** Map of discovered BLE devices, by device address. */
    @NonNull
    private final Map<String, DeviceInfo> mDiscoveredDevices;

    /** main thread handler used to manage discovery timeouts. */
    @NonNull
    private final Handler mHandler;

    /** List of scan filters for all supported BLE devices. */
    private final ScanFilter[] mSupportedDevicesFilter;

    /**
     * Constructor.
     *
     * <b>Should only be called if {@code bluetoothManager.getAdapter().getState() == BluetoothAdapter.STATE_ON}</b>
     *
     * @param backend            BLE backend owning this discovery
     * @param discoverableModels list of discoverable models
     */
    ArsdkBleDiscovery(@NonNull ArsdkBleBackend backend, @ArsdkDevice.Type int[] discoverableModels) {
        super(backend.getArsdkCore(), backend.getParentNativePtr(), "ble");
        mArsdkCore = backend.getArsdkCore();
        mBtManager = backend.getBluetoothManager();
        mBleScanner = mBtManager.getAdapter().getBluetoothLeScanner();
        mDiscoveredDevices = new HashMap<>();
        mHandler = new Handler(Looper.getMainLooper());

        mSupportedDevicesFilter = new ScanFilter[discoverableModels.length];
        int i = 0;
        for (@ArsdkDevice.Type int deviceType : discoverableModels) {
            mSupportedDevicesFilter[i] = makeScanFilterFor(deviceType);
            i++;
        }
    }

    @Override
    protected void onStart() {
        if (ULog.i(TAG_BLE)) {
            ULog.i(TAG_BLE, "Starting BLE discovery");
        }
        startScan();
    }

    @Override
    protected void onStop() {
        if (ULog.i(TAG_BLE)) {
            ULog.i(TAG_BLE, "Stopping BLE discovery");
        }
        stopScan();
    }

    /**
     * Starts scanning for devices.
     */
    private void startScan() {
        if (mBtManager.getAdapter().getState() == BluetoothAdapter.STATE_ON) {
            mBleScanner.startScan(
                    Arrays.asList(mSupportedDevicesFilter), SCAN_SETTINGS, mScanCallback);
        }
        // stop scan after timeout
        mHandler.postDelayed(mTimeoutHandler, DISCOVERY_TIMEOUT);
    }

    /**
     * Stops scanning for devices.
     */
    private void stopScan() {
        if (mBtManager.getAdapter().getState() == BluetoothAdapter.STATE_ON) {
            // BLE Scanner doesn't like it when bluetooth isn't ON
            mBleScanner.stopScan(mScanCallback);
        }
        mHandler.removeCallbacks(mTimeoutHandler);
    }

    /** Runnable executed upon discovery round timeout. */
    private final Runnable mTimeoutHandler = new Runnable() {

        @Override
        public void run() {
            // stop current scan round
            stopScan();
            // adjust device discovery counters to see if any device is lost
            Iterator<String> iterator = mDiscoveredDevices.keySet().iterator();
            while (iterator.hasNext()) {
                String address = iterator.next();
                DeviceInfo device = mDiscoveredDevices.get(address);
                assert device != null;
                if (device.onScanRoundFinished()) {
                    if (ULog.i(TAG_BLE)) {
                        ULog.i(TAG_BLE, "Device lost: " + device);
                    }
                    // device is lost, remove it
                    iterator.remove();
                    // notify low level
                    mArsdkCore.dispatchToPomp(() -> removeDevice(device.mName, device.mType));
                }
            }
            // start another scan round
            startScan();
        }
    };

    /** Called back when discovery scan results are available. */
    private final ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice btDevice = result.getDevice();
            String address = btDevice.getAddress();

            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord == null) {
                if (ULog.d(TAG_BLE)) {
                    ULog.d(TAG_BLE, "No scan result. Dropping discovery result for device " + address);
                }
                return;
            }

            @ArsdkDevice.Type int deviceType = getDeviceType(scanRecord);
            DeviceInfo device = mDiscoveredDevices.get(address);
            if (device == null) {
                if (ULog.i(TAG_BLE)) {
                    ULog.i(TAG_BLE, "Discovered new device: " + result);
                }
                mDiscoveredDevices.put(address, new DeviceInfo(btDevice, deviceType));
                // publish device to low level
                mArsdkCore.dispatchToPomp(() -> addDevice(btDevice.getName(), deviceType, address, 0, address));
            } else {
                // reset the discovery counter, device is still visible
                device.onSeenDuringScan();
            }
        }
    };

    /**
     * Represents a BLE device discovered during the scan process. <br/>
     * This is mainly a struct holding fields of interest, hence the accessible members.
     */
    private class DeviceInfo {

        /** Max amount of discovery rounds a device can be 'unseen' until considered lost. */
        private static final int DISCOVERY_COUNT = 2;

        /** Discovered device name. */
        @NonNull
        final String mName;

        /** Discovered device type. */
        @ArsdkDevice.Type
        final int mType;

        /** Discovered bluetooth device. */
        @NonNull
        private final BluetoothDevice mBtDevice;

        /** Counts for how many rounds this device was not seen during scanning. */
        private int mDiscoveryCounter;

        /**
         * Constructor.
         *
         * @param btDevice discovered bluetooth device
         * @param type     discovered device type
         */
        DeviceInfo(@NonNull BluetoothDevice btDevice, @ArsdkDevice.Type int type) {
            mBtDevice = btDevice;
            mType = type;
            mName = btDevice.getName();
            mDiscoveryCounter = DISCOVERY_COUNT;
        }

        /**
         * Marks that the device has been seen during scan round. <br/>
         * This resets the device discovery counter.
         */
        void onSeenDuringScan() {
            mDiscoveryCounter = DISCOVERY_COUNT;
        }

        /**
         * Called when a scan round has finished. <br/>
         * Based on the device connection state, and the current discovery counter value, this method returns
         * whether the device can be considered 'lost' from the discovery point of view.
         *
         * @return {@code true} when the device is lost, {@code false} otherwise
         */
        boolean onScanRoundFinished() {
            int state = mBtManager.getConnectionState(mBtDevice, BluetoothProfile.GATT);
            // don't count when the device is connected or connecting, as it is invisible during scanning
            if (state != BluetoothProfile.STATE_CONNECTED && state != BluetoothProfile.STATE_CONNECTING) {
                mDiscoveryCounter--;
            }
            return mDiscoveryCounter == 0;
        }

        @NonNull
        @Override
        public String toString() {
            return mName + " " + mType + " " + mBtDevice + " [cnt: " + mDiscoveryCounter + "]";
        }
    }

    /**
     * Creates a scan filter for the given device type.
     *
     * @param deviceType the device type to create a scan filter for
     *
     * @return a new scan filter matching the device
     */
    @NonNull
    private static ScanFilter makeScanFilterFor(@ArsdkDevice.Type int deviceType) {
        return new ScanFilter.Builder().setManufacturerData(PARROT_MANUFACTURER_ID,
                new byte[] {
                        (byte) (PARROT_USB_VENDOR & 0xff),
                        (byte) ((PARROT_USB_VENDOR >> 8) & 0xff),
                        (byte) (deviceType & 0xff),
                        (byte) ((deviceType >> 8) & 0xff)
                }
        ).build();
    }

    /**
     * Gets the device type matched by the given scan record.
     *
     * @param scanRecord the scan record to retrieve the device type from
     *
     * @return the scanned device type
     */
    @SuppressLint("WrongConstant")
    @ArsdkDevice.Type
    private static int getDeviceType(@NonNull ScanRecord scanRecord) {
        byte[] manufacturerData = scanRecord.getManufacturerSpecificData(PARROT_MANUFACTURER_ID);
        assert manufacturerData != null; // we only scan for parrot devices, so this should always match
        return manufacturerData[3] << 8 | manufacturerData[2];
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     * @param prefix prefix string (usually indent) to prepend to each written dump line
     */
    public void dump(@NonNull PrintWriter writer, @SuppressWarnings("unused") @NonNull Set<String> args,
                     @NonNull String prefix) {
        writer.write(prefix + "Discovered devices: " + mDiscoveredDevices.size() + "\n");
        for (DeviceInfo device : mDiscoveredDevices.values()) {
            writer.write(prefix + "\t" + device + "\n");
        }
    }
}
