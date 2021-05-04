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

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Dri;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.settings.DriSettingsActivity;

import androidx.annotation.NonNull;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class DriContent extends PeripheralContent<Drone, Dri> {

    DriContent(@NonNull Drone drone) {
        super(R.layout.dri_info, drone, Dri.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends PeripheralContent.ViewHolder<DriContent, Dri> {

        @NonNull
        private final Button mEditButton;

        @NonNull
        private final TextView mStateText;

        @NonNull
        private final TextView mTypeIdText;

        @NonNull
        private final TextView mIdText;

        @NonNull
        private final TextView mTypeState;

        @NonNull
        private final TextView mTypeConfig;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mEditButton = findViewById(R.id.btn_edit);
            mEditButton.setOnClickListener(mClickListener);
            mStateText = findViewById(R.id.state);
            mTypeIdText = findViewById(R.id.id_type);
            mIdText = findViewById(R.id.id);
            mTypeState = findViewById(R.id.type_state);
            mTypeConfig = findViewById(R.id.type_config);
        }

        @Override
        void onBind(@NonNull DriContent content, @NonNull Dri dri) {
            mStateText.setText(dri.state().isEnabled() ?
                    R.string.boolean_setting_enabled : R.string.boolean_setting_disabled);
            String idType = mContext.getString(R.string.no_value);
            String id = mContext.getString(R.string.no_value);
            Dri.DroneId idInfo = dri.getDroneId();
            if (idInfo != null) {
                idType = idInfo.getType().name();
                id = idInfo.getId();
            }
            mTypeIdText.setText(idType);
            mIdText.setText(id);
            mTypeState.setText(dri.getTypeConfigState() != null ? dri.getTypeConfigState().getState().toString()
                    : mContext.getString(R.string.no_value));
            mTypeConfig.setText(dri.getTypeConfig() != null ? dri.getTypeConfig().getType().toString()
                    : mContext.getString(R.string.no_value));
        }

        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull DriContent content, @NonNull Dri dri) {
                mContext.startActivity(new Intent(mContext, DriSettingsActivity.class)
                        .putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid()));
            }
        };
    }
}
