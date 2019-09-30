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

package com.parrot.drone.groundsdkdemo.info;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.WifiScanner;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Channel;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.format.Html;

import java.util.LinkedHashSet;
import java.util.Set;

class WifiScannerContent extends PeripheralContent<Peripheral.Provider, WifiScanner> {

    WifiScannerContent(@NonNull Peripheral.Provider device) {
        super(R.layout.wifi_scanner_info, device, WifiScanner.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends PeripheralContent.ViewHolder<WifiScannerContent, WifiScanner> {

        @NonNull
        private final Button mScanButton;

        @NonNull
        private final TextView mOccupationText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mScanButton = findViewById(R.id.btn_scan);
            mScanButton.setOnClickListener(mClickListener);
            mOccupationText = findViewById(R.id.occupation);
        }

        @Override
        void onBind(@NonNull WifiScannerContent content, @NonNull WifiScanner wifiScanner) {
            mScanButton.setText(wifiScanner.isScanning() ? R.string.action_stop : R.string.action_scan);
            Set<String> rates = new LinkedHashSet<>();
            for (Channel channel : Channel.values()) {
                int rate = wifiScanner.getChannelOccupationRate(channel);
                if (rate > 0) {
                    rates.add(mContext.getString(R.string.channel_occupation_format, channel, rate));
                }
            }
            mOccupationText.setText(Html.fromHtml(TextUtils.join(", ", rates)));
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull WifiScannerContent content, @NonNull WifiScanner wifiScanner) {
                if (wifiScanner.isScanning()) {
                    wifiScanner.stopScan();
                } else {
                    wifiScanner.startScan();
                }
            }
        };
    }
}
