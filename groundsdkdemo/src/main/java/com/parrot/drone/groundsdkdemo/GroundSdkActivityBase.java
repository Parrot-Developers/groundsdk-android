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

package com.parrot.drone.groundsdkdemo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.snackbar.Snackbar;
import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.ManagedGroundSdk;
import com.parrot.drone.groundsdk.device.peripheral.VirtualGamepad;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.sdkcore.ulog.ULog;
import com.parrot.drone.sdkcore.ulog.ULogTag;

import java.util.HashSet;
import java.util.Set;

/**
 * Base for an activity that uses GroundSdk. Manages GroundSdk lifecycle properly.
 */
public abstract class GroundSdkActivityBase extends AppCompatActivity {

    /** Logging tag. */
    private static final ULogTag TAG = new ULogTag("GSDKDemo");

    /** List of runtime permission we need. */
    private static final String[] PERMISSIONS_NEEDED = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE, /* for ULog Recorder. */
            Manifest.permission.ACCESS_COARSE_LOCATION, /* to access BLE discovery results. */
            Manifest.permission.ACCESS_FINE_LOCATION,   /* for GPS location updates. */
            Manifest.permission.CAMERA, /* For HMD see-through. */
    };

    /** Code for permission request result handling. */
    private static final int REQUEST_CODE_PERMISSIONS_REQUEST = 1;

    /** Ground SDK interface. */
    private GroundSdk mGroundSdk;

    /**
     * Gets GroundSDK interface.
     *
     * @return GroundSDK interface
     */
    @NonNull
    protected final GroundSdk groundSdk() {
        return mGroundSdk;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGroundSdk = ManagedGroundSdk.obtainSession(this);

        Set<String> permissionsToRequest = new HashSet<>();
        for (String permission : PERMISSIONS_NEEDED) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    ULog.w(TAG, "User has not allowed permission " + permission);
                    Toast.makeText(this, "Please allow permission " + permission, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                } else {
                    permissionsToRequest.add(permission);
                }
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]),
                    REQUEST_CODE_PERMISSIONS_REQUEST);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(mGamepadEventReceiver, FILTER_GAMEPAD_EVENT);
    }

    private static final IntentFilter FILTER_GAMEPAD_EVENT = new IntentFilter(
            VirtualGamepad.ACTION_GAMEPAD_APP_EVENT);

    private final BroadcastReceiver mGamepadEventReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int actionOrdinal = intent.getIntExtra(VirtualGamepad.EXTRA_GAMEPAD_APP_EVENT_ACTION, -1);
            if (actionOrdinal != -1) {
                Snackbar.make(getContentView(), "Gamepad app event [action: "
                                                + ButtonsMappableAction.values()[actionOrdinal] + "]",
                        Snackbar.LENGTH_SHORT).show();
            }
        }

        @NonNull
        private View getContentView() {
            return ((ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content)).getChildAt(0);
        }
    };

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGamepadEventReceiver);
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean denied = false;
        if (permissions.length == 0) {
            // canceled, finish
            ULog.w(TAG, "User canceled permission(s) request");
            denied = true;
        } else {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    ULog.w(TAG, "User denied permission: " + permissions[i]);
                    denied = true;
                }
            }
        }

        if (denied) {
            finish();
        }
    }
}
