/*
 *     Copyright (C) 2020 Parrot Drones SAS
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
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.LogControl;
import com.parrot.drone.groundsdkdemo.R;

class LogControlContent extends PeripheralContent<Drone, LogControl> {

    LogControlContent(@NonNull Drone drone) {
        super(R.layout.log_control_info, drone, LogControl.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends PeripheralContent.ViewHolder<LogControlContent, LogControl> {

        @NonNull
        private final TextView mLogsStorageStateText;

        @NonNull
        private final Button mDeactivateLogsButton;


        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mLogsStorageStateText = findViewById(R.id.log_storage_state);
            mDeactivateLogsButton = findViewById(R.id.btn_deactivate_log_storage);
            mDeactivateLogsButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull LogControlContent content, @NonNull LogControl logControl) {
            mLogsStorageStateText.setText(logControl.areLogsEnabled() ? R.string.boolean_setting_enabled
                    : R.string.boolean_setting_disabled);
            mDeactivateLogsButton.setEnabled(logControl.canDeactivateLogs());
        }

        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull LogControlContent content, @NonNull LogControl logControl) {
                switch (v.getId()) {
                    case R.id.btn_deactivate_log_storage:
                        logControl.deactivateLogs();
                        break;
                }
            }
        };
    }
}
