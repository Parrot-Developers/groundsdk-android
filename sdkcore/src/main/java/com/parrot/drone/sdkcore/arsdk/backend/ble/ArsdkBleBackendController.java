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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.arsdk.backend.ArsdkBackendController;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.util.Set;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG_BLE;

/**
 * Bluetooth Low-Energy backend controller.
 * <p>
 * This implementation uses {@link BluetoothLeScanner} android API to discover BLE devices
 */
public final class ArsdkBleBackendController extends ArsdkBackendController {

    /**
     * Creates a new ArsdkBleBackendController instance. <br/>
     *
     * @param appContext         android application context
     * @param discoverableModels list of discoverable models
     *
     * @return a new ArsdkBleBackendController instance, or {@code null} if the device does not support it
     */
    public static ArsdkBackendController create(@NonNull Context appContext,
                                                @ArsdkDevice.Type int[] discoverableModels) {
        ArsdkBackendController controller = null;
        BluetoothManager btManager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager == null ? null : btManager.getAdapter();
        // some android ROMs report having Bluetooth/BLE system feature, yet its false
        if (appContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            && btManager != null && btAdapter != null && discoverableModels.length > 0) {
            controller = new ArsdkBleBackendController(appContext, btManager, discoverableModels);
        }
        return controller;
    }

    /** Android application context. */
    @NonNull
    private final Context mContext;

    /** Android Bluetooth manager. */
    @NonNull
    private final BluetoothManager mBtManager;

    /** {@code true} when bluetooth is on, {@code false} otherwise. */
    private boolean mBtEnabled;

    /** Backend managing BLE discovery and device connections. */
    @Nullable
    private ArsdkBleBackend mBackend;

    /** list of BLE discoverable models. */
    @ArsdkDevice.Type
    private final int[] mDiscoverableModels;

    /**
     * Constructor. <br/>
     * Call {@link ArsdkBleBackendController#create(Context, int[])} instead.
     *
     * @param appContext         android application context
     * @param btManager          android bluetooth manager
     * @param discoverableModels list of discoverable models
     */
    @MainThread
    private ArsdkBleBackendController(@NonNull Context appContext, @NonNull BluetoothManager btManager,
                                      @ArsdkDevice.Type int[] discoverableModels) {
        mContext = appContext;
        mBtManager = btManager;
        mDiscoverableModels = discoverableModels;
    }

    @Override
    protected void onStart() {
        mContext.registerReceiver(mBtAdapterStateListener, BT_STATE_FILTER);
        if (mBtManager.getAdapter().getState() == BluetoothAdapter.STATE_ON) {
            mBtEnabled = true;
            onBluetoothAvailable();
        }
    }

    @Override
    protected void onStop() {
        mContext.unregisterReceiver(mBtAdapterStateListener);
        if (mBtEnabled) {
            onBluetoothLost();
            mBtEnabled = false;
        }
    }

    /**
     * Called back when bluetooth adapter state gets on. <br/>
     * Creates BLE backend and starts to discover BLE devices.
     */
    private void onBluetoothAvailable() {
        if (ULog.i(TAG_BLE)) {
            ULog.i(TAG_BLE, "Bluetooth on, starting BLE backend and discovery");
        }

        // should never happen, receiver is unregistered in onStop
        assert mArsdkCore != null;

        // create backend and discovery
        assert mBackend == null;
        mBackend = new ArsdkBleBackend(mArsdkCore, mContext, mBtManager, mDiscoverableModels);
        mBackend.startDiscovery();
    }

    /**
     * Called back when bluetooth adapters state gets off. <br/>
     * Stops BLE devices discovery and destroys BLE backend.
     */
    private void onBluetoothLost() {
        if (ULog.i(TAG_BLE)) {
            ULog.i(TAG_BLE, "Bluetooth off, stopping BLE backend and discovery");
        }

        assert mBackend != null;
        mBackend.stopDiscovery();
        mBackend.destroy();
        mBackend = null;
    }

    /** IntentFilter to listen to bluetooth adapter state changes. */
    private static final IntentFilter BT_STATE_FILTER = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

    /** Broadcast receiver listening on bluetooth adapter state changes. */
    private final BroadcastReceiver mBtAdapterStateListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            if (mArsdkCore == null) {
                return;
            }
            mArsdkCore.dispatchToPomp(() -> {
                if (mArsdkCore == null) {
                    return;
                }
                if (state == BluetoothAdapter.STATE_OFF && mBtEnabled) {
                    mBtEnabled = false;
                    onBluetoothLost();
                } else if (state == BluetoothAdapter.STATE_ON && !mBtEnabled) {
                    mBtEnabled = true;
                    onBluetoothAvailable();
                }
            });
        }
    };

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     * @param prefix prefix string (usually indent) to prepend to each written dump line
     */
    @Override
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args, @NonNull String prefix) {
        writer.write(prefix + "BLE Backend controller\n");
        writer.write(prefix + "\tState: " + (mBtEnabled ? "STARTED" : "STOPPED") + "\n");
        if (mBackend != null) {
            mBackend.dump(writer, args, prefix + "\t");
        }
    }
}
