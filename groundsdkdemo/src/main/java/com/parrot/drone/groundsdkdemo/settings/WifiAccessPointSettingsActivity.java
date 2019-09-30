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

package com.parrot.drone.groundsdkdemo.settings;

import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.WifiAccessPoint;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Band;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Channel;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.PasswordDialogFragment;
import com.parrot.drone.groundsdkdemo.R;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;
import static com.parrot.drone.groundsdkdemo.settings.SettingViewAdapters.updateSetting;

public class WifiAccessPointSettingsActivity extends GroundSdkActivityBase
        implements WifiAutoChannelModeChoiceDialogFragment.ModeSelectionListener,
                   PasswordDialogFragment.PasswordAcquiredListener {

    private MultiChoiceSettingView<WifiAccessPoint.Environment> mEnvironmentView;

    private MultiChoiceSettingView<String> mCountryView;

    private MultiChoiceSettingView<Channel> mChannelView;

    private Button mAutoChannelButton;

    private TextSettingView mSsidView;

    private MultiChoiceSettingView<WifiAccessPoint.SecuritySetting.Mode> mSecurityView;

    private Button mChangePasswordButton;

    private WifiAccessPoint mWifiAccessPoint;

    private WifiAccessPoint.SecuritySetting.Mode mSelectedSecurityMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String deviceUid = getIntent().getStringExtra(EXTRA_DEVICE_UID);
        Peripheral.Provider provider = groundSdk().getDrone(deviceUid);
        if (provider == null) {
            provider = groundSdk().getRemoteControl(deviceUid);
            if (provider == null) {
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_wifi_ap_settings);

        mEnvironmentView = findViewById(R.id.environment);
        mCountryView = findViewById(R.id.country);
        mChannelView = findViewById(R.id.channel);
        mAutoChannelButton = findViewById(R.id.autoselect);
        mSsidView = findViewById(R.id.ssid);
        mSecurityView = findViewById(R.id.security);
        mChangePasswordButton = findViewById(R.id.password);

        mAutoChannelButton.setOnClickListener(v -> {
            DialogFragment modeRequest = new WifiAutoChannelModeChoiceDialogFragment();
            modeRequest.show(getSupportFragmentManager(), null);
        });

        mChangePasswordButton.setOnClickListener(v -> {
            mSelectedSecurityMode = mWifiAccessPoint.security().getMode();
            DialogFragment passwordRequest = new PasswordDialogFragment();
            passwordRequest.show(getSupportFragmentManager(), null);
        });

        provider.getPeripheral(WifiAccessPoint.class, wifiAp -> {
            mWifiAccessPoint = wifiAp;
            if (wifiAp != null) {
                updateSetting(mEnvironmentView, wifiAp.environment());
                updateSetting(mSsidView, wifiAp.ssid());
                updateCountry();
                updateChannel();
                updateSecurity();
            } else {
                finish();
            }
        });
    }

    @Override
    public void onModeSelected(@NonNull WifiAccessPoint.ChannelSetting.SelectionMode mode) {
        WifiAccessPoint.ChannelSetting channel = mWifiAccessPoint.channel();
        switch (mode) {
            case AUTO_ANY_BAND:
                channel.autoSelect();
                break;
            case AUTO_2_4_GHZ_BAND:
                channel.autoSelect(Band.B_2_4_GHZ);
                break;
            case AUTO_5_GHZ_BAND:
                channel.autoSelect(Band.B_5_GHZ);
                break;
            case MANUAL:
                break;
        }
    }

    @Override
    public void onPasswordAcquired(@NonNull String password) {
        switch (mSelectedSecurityMode) {
            case WPA2_SECURED:
                mWifiAccessPoint.security().secureWithWPA2(password);
                break;
            case OPEN:
                break;
        }
    }

    private void updateCountry() {
        WifiAccessPoint.CountrySetting setting = mWifiAccessPoint.country();
        mCountryView.setChoices(setting.getAvailableCodes())
                    .setSelection(setting.getCode())
                    .setUpdating(setting.isUpdating())
                    .setListener(setting::select);
    }

    private void updateChannel() {
        WifiAccessPoint.ChannelSetting setting = mWifiAccessPoint.channel();
        mAutoChannelButton.setEnabled(!setting.isUpdating());
        mChannelView.setChoices(setting.getAvailableChannels()).setSelection(setting.get())
                    .setUpdating(setting.isUpdating()).setListener(setting::select);
    }

    private void updateSecurity() {
        WifiAccessPoint.SecuritySetting setting = mWifiAccessPoint.security();

        mChangePasswordButton.setEnabled(!setting.isUpdating()
                                         && setting.getMode() != WifiAccessPoint.SecuritySetting.Mode.OPEN);

        mSecurityView.setChoices(setting.getSupportedModes())
                     .setSelection(setting.getMode())
                     .setUpdating(setting.isUpdating())
                     .setListener(mode -> {
                         mSelectedSecurityMode = mode;
                         switch (mode) {
                             case OPEN:
                                 setting.open();
                                 break;
                             case WPA2_SECURED:
                                 // restore previous mode until dialog validation/update
                                 mSecurityView.setSelection(setting.getMode());
                                 DialogFragment passwordRequest = new PasswordDialogFragment();
                                 passwordRequest.show(getSupportFragmentManager(), null);
                                 break;
                         }
                     });
    }

}
