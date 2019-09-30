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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.arsdk.backend.ArsdkBackendController;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.IOException;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG_MUX;

/**
 * USB accessory MUX backend controller.
 * <p>
 * This backend controller opens the plugged USB Remote Control Accessory and forwards the connection to a MUX backend.
 */
public final class ArsdkUsbMuxBackendController extends ArsdkBackendController {

    /**
     * Creates a new ArsdkUsbMuxBackendController instance.
     * <p>
     * Controller instance is created only if the device supports the USB accessory feature.
     *
     * @param context            android application context
     * @param discoverableModels list of discoverable models
     *
     * @return a new ArsdkDebugMuxBackendController instance, or {@code null} if unsupported on the device
     */
    public static ArsdkUsbMuxBackendController create(@NonNull Context context,
                                                      @ArsdkDevice.Type int[] discoverableModels) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY)) {
            return null;
        }
        return new ArsdkUsbMuxBackendController(context, discoverableModels);
    }

    /** Android context. */
    @NonNull
    private final Context mContext;

    /** Android USB Manager, used to work with the USB RC accessory. */
    @NonNull
    private final UsbManager mUsbManager;

    /** list of Mux discoverable models. */
    @ArsdkDevice.Type
    private final int[] mDiscoverableModels;

    /** USB RC accessory, obtained from accessory plugged intent. */
    @Nullable
    private UsbAccessory mRcAccessory;

    /** RC accessory file descriptor, used by MUX backend to communicate with the accessory. */
    @Nullable
    private ParcelFileDescriptor mAccessoryFd;

    /** MUX backend. */
    @Nullable
    private ArsdkMuxBackend mBackend;

    /** True when this backend controller is started, false otherwise. */
    private boolean mStarted;

    /** Set when waiting for user permission to use the accessory, otherwise {@code null}. */
    @Nullable
    private BroadcastReceiver mPermissionReceiver;

    /** Only accessories with this manufacturer name are allowed. */
    private static final String MANUFACTURER_NAME = "Parrot";

    /**
     * Constructor.
     *
     * @param context            android application context
     * @param discoverableModels list of discoverable models
     */
    private ArsdkUsbMuxBackendController(@NonNull Context context, @ArsdkDevice.Type int[] discoverableModels) {
        mContext = context;
        mDiscoverableModels = discoverableModels;
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        ULog.i(TAG_MUX, "Created USB accessory MUX backend controller");
    }

    /**
     * Sets the current USB RC accessory to be managed by the controller.
     * <p>
     * In case the controller is not started yet, then the accessory is simply cached to be opened once the
     * controller starts. Otherwise, it is opened right away (which may result in a no-op in case the controller
     * has already opened an accessory.
     *
     * @param accessory usb accessory to manage.
     */
    public void setRcAccessory(@NonNull UsbAccessory accessory) {
        if (ULog.i(TAG_MUX)) {
            ULog.i(TAG_MUX, "Received RC accessory from application: " + accessory);
        }
        if (MANUFACTURER_NAME.equals(accessory.getManufacturer())) {
            if (mUsbManager.hasPermission(accessory)) {
                installRc(accessory);
            } else {
                requestRcPermission(accessory);
            }
        }
    }

    @Override
    protected void onStart() {
        ULog.i(TAG_MUX, "Starting USB accessory MUX backend controller");
        boolean rcInstalled;
        synchronized (this) {
            mStarted = true;
            rcInstalled = mRcAccessory != null;
        }
        if (rcInstalled) {
            openRcConnection();
        } else {
            assert mArsdkCore != null;
            mArsdkCore.dispatchToMain(mRcDiscovery);
        }
    }

    @Override
    protected void onStop() {
        ULog.i(TAG_MUX, "Stopping USB accessory MUX backend controller");
        synchronized (this) {
            mStarted = false;
        }
        closeRcConnection();
    }

    /**
     * Installs an USB RC accessory and opens it if the controller is started.
     *
     * @param rc USB RC accessory to install
     */
    private void installRc(@NonNull UsbAccessory rc) {
        if (ULog.i(TAG_MUX)) {
            ULog.i(TAG_MUX, "Installing RC: " + rc);
        }
        synchronized (this) {
            mRcAccessory = rc;
            if (mStarted) {
                assert mArsdkCore != null;
                mArsdkCore.dispatchToPomp(this::openRcConnection);
            }
        }
    }

    /** Intent sent to provide permission request results. */
    private static final String ACTION_USB_ACCESSORY_PERMISSION =
            "com.parrot.drone.sdkcore.arsdk.backend.mux.ACTION_USB_ACCESSORY_PERMISSION";

    /**
     * Requests user permission to use an USB RC accessory.
     *
     * @param rc USB RC accessory to request permission for
     */
    private void requestRcPermission(@NonNull UsbAccessory rc) {
        if (mPermissionReceiver == null) {
            if (ULog.i(TAG_MUX)) {
                ULog.i(TAG_MUX, "Requesting user permission to use " + rc);
            }
            mPermissionReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        installRc(rc);
                    }
                    mContext.unregisterReceiver(this);
                    mPermissionReceiver = null;
                }
            };
            mContext.registerReceiver(mPermissionReceiver, new IntentFilter(ACTION_USB_ACCESSORY_PERMISSION));
            mUsbManager.requestPermission(rc,
                    PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_ACCESSORY_PERMISSION), 0));
        }
    }

    /** Discovers and install any available RC. */
    private final Runnable mRcDiscovery = new Runnable() {

        @Override
        public void run() {
            ULog.i(TAG_MUX, "Trying to discover a plugged RC accessory");

            UsbAccessory[] accessories = mUsbManager.getAccessoryList();
            UsbAccessory rc = accessories != null && accessories.length > 0
                              && MANUFACTURER_NAME.equals(accessories[0].getManufacturer()) ? accessories[0] : null;

            if (rc == null) {
                ULog.i(TAG_MUX, "No RC discovered");
            } else if (mUsbManager.hasPermission(rc)) {
                installRc(rc);
            } else {
                requestRcPermission(rc);
            }
        }
    };

    /**
     * Opens the currently setup RC USB accessory.
     * <p>
     * If {@code mRcAccessory} is set, and could be successfully opened, then a new MUX backend is created to manage the
     * communication with it.
     */
    private void openRcConnection() {
        if (mAccessoryFd == null) {
            UsbAccessory rc;
            synchronized (this) {
                rc = mRcAccessory;
                mRcAccessory = null;
            }
            if (rc != null) {
                if (ULog.i(TAG_MUX)) {
                    ULog.i(TAG_MUX, "Opening RC accessory:" + rc);
                }
                try {
                    mAccessoryFd = mUsbManager.openAccessory(rc);
                } catch (IllegalArgumentException e) {
                    ULog.e(TAG_MUX, "Could not open RC accessory", e);
                }

                if (mAccessoryFd != null) {
                    int fd = mAccessoryFd.getFd();
                    if (ULog.i(TAG_MUX)) {
                        ULog.i(TAG_MUX, "RC accessory opened [fd: " + fd + "]");
                    }
                    assert mArsdkCore != null;
                    mBackend = new ArsdkMuxBackend(mArsdkCore, fd, mDiscoverableModels, mMuxEofListener);
                    mBackend.startDiscovery();
                }
            }
        } else {
            ULog.w(TAG_MUX, "RC accessory already open, ignoring plug");
        }

    }

    /**
     * Called back when the RC USB accessory has been unplugged from the device.
     * <p>
     * Stops MUX discovery, destroys MUX backend and closes the accessory FD (hopefully) properly
     */
    private void closeRcConnection() {
        if (mBackend != null) {
            assert mAccessoryFd != null;
            int fd = mAccessoryFd.getFd();
            try {
                mAccessoryFd.close();
                if (ULog.i(TAG_MUX)) {
                    ULog.i(TAG_MUX, "RC accessory closed [fd: " + fd + "]");
                }
            } catch (IOException e) {
                ULog.e(TAG_MUX, "Could not close RC accessory [fd: " + fd + "]", e);
                // TODO: what to do actually ? accessory may remain stuck until our process dies
            } finally {
                mAccessoryFd = null;
            }
            mBackend.stopDiscovery();
            mBackend.destroy();
            mBackend = null;
        }
    }

    /** Called back by MUX backend when an EOF or error event occurs on the accessory fd. */
    private final ArsdkMuxBackend.EofListener mMuxEofListener = () -> {
        ULog.i(TAG_MUX, "RC accessory unplugged");
        closeRcConnection();
    };
}
