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

import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.SystemInfo;
import com.parrot.drone.groundsdkdemo.R;

class SysInfoContent extends PeripheralContent<Peripheral.Provider, SystemInfo> {

    SysInfoContent(@NonNull Peripheral.Provider device) {
        super(R.layout.system_info, device, SystemInfo.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    static class ViewHolder extends PeripheralContent.ViewHolder<SysInfoContent, SystemInfo> {

        @NonNull
        private final TextView mFirmwareVersionText;

        @NonNull
        private final TextView mFirmwareBlacklistedText;

        @NonNull
        private final TextView mUpdateRequired;

        @NonNull
        private final TextView mHardwareVersionText;

        @NonNull
        private final TextView mSerialNumberText;

        @NonNull
        private final TextView mCpuIdText;

        @NonNull
        private final TextView mBoardIdText;

        @NonNull
        private final Button mResetSettingsButton;

        @NonNull
        private final ProgressBar mResetSettingsProgress;

        @NonNull
        private final Button mFactoryResetButton;

        @NonNull
        private final ProgressBar mFactoryResetProgress;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mFirmwareVersionText = findViewById(R.id.firmware);
            mFirmwareBlacklistedText = findViewById(R.id.firmware_blacklisted);
            mUpdateRequired = findViewById(R.id.update_required);
            mHardwareVersionText = findViewById(R.id.hardware);
            mSerialNumberText = findViewById(R.id.serial);
            mCpuIdText = findViewById(R.id.cpu_id);
            mBoardIdText = findViewById(R.id.board_id);
            mResetSettingsButton = findViewById(R.id.btn_reset_settings);
            mResetSettingsButton.setOnClickListener(mClickListener);
            mResetSettingsProgress = findViewById(R.id.progress_reset_settings);
            mFactoryResetButton = findViewById(R.id.btn_factory_reset);
            mFactoryResetButton.setOnClickListener(mClickListener);
            mFactoryResetProgress = findViewById(R.id.progress_factory_reset);
        }

        @Override
        void onBind(@NonNull SysInfoContent content, @NonNull SystemInfo sysInfo) {
            mFirmwareVersionText.setText(sysInfo.getFirmwareVersion());
            mFirmwareBlacklistedText.setText(Boolean.toString(sysInfo.isFirmwareBlacklisted()));
            mUpdateRequired.setText(Boolean.toString(sysInfo.isUpdateRequired()));
            mHardwareVersionText.setText(sysInfo.getHardwareVersion());
            mSerialNumberText.setText(sysInfo.getSerialNumber());
            mCpuIdText.setText(sysInfo.getCpuIdentifier());
            mBoardIdText.setText(sysInfo.getBoardIdentifier());
            mResetSettingsButton.setEnabled(!sysInfo.isResetSettingsInProgress());
            mResetSettingsProgress.setVisibility(sysInfo.isResetSettingsInProgress() ? View.VISIBLE : View.GONE);
            mFactoryResetButton.setEnabled(!sysInfo.isFactoryResetInProgress());
            mFactoryResetProgress.setVisibility(sysInfo.isFactoryResetInProgress() ? View.VISIBLE : View.GONE);
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View button, @NonNull SysInfoContent content, @NonNull SystemInfo sysInfo) {
                if (button == mResetSettingsButton) {
                    sysInfo.resetSettings();
                } else if (button == mFactoryResetButton) {
                    sysInfo.factoryReset();
                }
            }
        };
    }
}
