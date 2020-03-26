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

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parrot.drone.groundsdk.device.peripheral.BatteryGaugeUpdater;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdkdemo.R;

import androidx.annotation.NonNull;

class BatteryGaugeUpdaterContent extends PeripheralContent<Peripheral.Provider, BatteryGaugeUpdater> {

    BatteryGaugeUpdaterContent(@NonNull Peripheral.Provider device) {
        super(R.layout.battery_gauge_updater_info, device, BatteryGaugeUpdater.class);
    }

    @Override
    Content.ViewHolder<?> onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends RefContent.ViewHolder<BatteryGaugeUpdaterContent, BatteryGaugeUpdater> {

        @NonNull
        private final TextView mStateText;

        @NonNull
        private final TextView mUnavailabilityReasonsText;

        @NonNull
        private final TextView mCurrentProgressText;

        @NonNull
        private final Button mUpdateButton;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mStateText = findViewById(R.id.state);
            mUnavailabilityReasonsText = findViewById(R.id.unavailability_reasons);
            mCurrentProgressText = findViewById(R.id.current_progress);
            mUpdateButton = findViewById(R.id.btn_update);
            mUpdateButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull BatteryGaugeUpdaterContent content, @NonNull BatteryGaugeUpdater updater) {
            mStateText.setText(updater.state().toString());
            mUnavailabilityReasonsText.setText(TextUtils.join("\n", updater.unavailabilityReasons()));
            mCurrentProgressText.setText(mContext.getString(R.string.percent_format, updater.currentProgress()));

            switch (updater.state()) {
                case READY_TO_PREPARE:
                    mUpdateButton.setText(R.string.action_prepare_update);
                    mUpdateButton.setEnabled(updater.unavailabilityReasons().isEmpty());
                    break;
                case PREPARING_UPDATE:
                    mUpdateButton.setText(R.string.action_prepare_update);
                    mUpdateButton.setEnabled(false);
                    break;
                case READY_TO_UPDATE:
                    mUpdateButton.setText(R.string.action_update);
                    mUpdateButton.setEnabled(updater.unavailabilityReasons().isEmpty());
                    break;
                case UPDATING:
                    mUpdateButton.setText(R.string.action_update);
                    mUpdateButton.setEnabled(false);
                    break;
                case ERROR:
                    mUpdateButton.setEnabled(false);
                    break;
            }
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View button, @NonNull BatteryGaugeUpdaterContent content,
                         @NonNull BatteryGaugeUpdater updater) {
                switch (updater.state()) {
                    case READY_TO_PREPARE:
                        updater.prepareUpdate();
                        break;
                    case READY_TO_UPDATE:
                        updater.update();
                        break;
                    case PREPARING_UPDATE:
                    case UPDATING:
                    case ERROR:
                        break;
                }
            }
        };
    }
}
