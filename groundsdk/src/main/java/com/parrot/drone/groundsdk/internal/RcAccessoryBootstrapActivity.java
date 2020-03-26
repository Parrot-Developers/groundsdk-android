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

package com.parrot.drone.groundsdk.internal;

import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.sdkcore.ulog.ULog;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_INTERNAL;

/**
 * Activity that catches android USB Accessory plugged intent.
 * <p>
 * This activity is registered in GroundSdk manifest and enabled by default; it is configured to be started when an USB
 * accessory that matches some supported {@link RemoteControl.Model remote control} as defined in
 * {@code res/xml/gsdk_usb_rc_filter.xml} is plugged to the device.
 * <p>
 * When this activity starts due to such an event, it forwards plugged RC accessory information to {@code GroundSdk} so
 * that it can be monitored properly, and then, if the application is not currently in foreground, tries to launch the
 * default main activity of the application (otherwise, nothing more happens, as the application is already in
 * foreground; it remains in the same state}.
 * <p>
 * The application may override declarations in {@code res/xml/gsdk_usb_rc_filter.xml} to extend or restrict the list
 * of RC devices that should trigger this activity. <br>
 * Note however that the device(s) in question should be in the list of {@link RemoteControl.Model} supported by
 * GroundSdk.
 * <p>
 * The application may disable this activity by overriding flag {@code gsdk_rc_accessory_bootstrap_activity_enabled} in
 * {@code res/values/config.xml} to {@code false}. <br>
 * Note that by doing so, the application fully becomes responsible to handle android USB accessory plugged events
 * by itself. Most importantly, the application should forward the accessory contained in the
 * {@link UsbManager#EXTRA_ACCESSORY intent extra} of such events to {@code GroundSdk} using
 * {@link GroundSdk#manageRcAccessory} method, so that it can monitors the RC devices properly. <br>
 * Failure to do so will result in {@code GroundSdk} only being able to detect a plugged RC accessory when the
 * first session starts, and <strong>NOT</strong> whenever some RC device gets plugged while it is already started.
 */
public class RcAccessoryBootstrapActivity extends Activity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UsbAccessory accessory = getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (ULog.i(TAG_INTERNAL)) {
            ULog.i(TAG_INTERNAL, "RcAccessoryBootstrapActivity received accessory: " + accessory);
        }
        GroundSdk.manageRcAccessory(this, accessory);
        Intent intent = getPackageManager().getLaunchIntentForPackage(getApplicationContext().getPackageName());
        if (intent != null) {
            startActivity(intent.setPackage(null));
        } else {
            ULog.i(TAG_INTERNAL, "Could not find default main activity to start");
        }
        finish();
    }
}