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

package com.parrot.drone.groundsdkdemo.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Dri;
import com.parrot.drone.groundsdk.device.peripheral.Leds;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import androidx.annotation.NonNull;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;
import static com.parrot.drone.groundsdkdemo.settings.SettingViewAdapters.updateSetting;

public class DriSettingsActivity extends GroundSdkActivityBase {

    private ToggleSettingView mStateView;

    private MultiChoiceSettingView<Dri.TypeConfig.Type> mTypeConfig;

    private TextSettingView mTypeConfigOperator;

    private TextView mTypeState;

    private TextView mTypeCleanBtn;

    private Dri.TypeConfig.Type mType;

    private String mOperatorId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String deviceUid = getIntent().getStringExtra(EXTRA_DEVICE_UID);
        Drone drone = groundSdk().getDrone(deviceUid, uid -> finish());

        if (drone == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_dri_settings);

        mStateView = findViewById(R.id.state);
        mTypeConfig = findViewById(R.id.type_config);
        mTypeConfigOperator = findViewById(R.id.operator);
        mTypeState = findViewById(R.id.type_state);
        mTypeCleanBtn = findViewById(R.id.btn_reset);

        drone.getPeripheral(Dri.class, dri -> {
            if (dri != null) {
                mType = dri.getTypeConfig() != null ? dri.getTypeConfig().getType() : null;
                mOperatorId = dri.getTypeConfig() != null ? dri.getTypeConfig().getOperatorId() : "";
                updateSetting(mStateView, dri.state());
                updateTypeConfig(dri);
                updateTypeConfigOperator(dri);
                updateTypeConfigState(dri);
                updateTypeClean(dri);
            } else {
                finish();
            }
        });
    }

    private void updateTypeConfig(@NonNull Dri dri) {
        mTypeConfig.setChoices(dri.supportedTypes())
                   .setSelection(dri.getTypeConfig() != null ? dri.getTypeConfig().getType() : null)
                   .setUpdating(dri.getTypeConfigState() != null &&
                                dri.getTypeConfigState().getState() == Dri.TypeConfigState.State.UPDATING)
                   .setListener(type -> {
                       mType = type;
                       setTypeConfig(dri);
                       updateTypeConfigOperator(dri);
                   });
    }

    private void updateTypeConfigOperator(@NonNull Dri dri) {
        mTypeConfigOperator.setAvailable(mType == Dri.TypeConfig.Type.EN4709_002)
                           .setText(dri.getTypeConfig() != null ? dri.getTypeConfig().getOperatorId() : "")
                           .setUpdating(dri.getTypeConfigState() != null &&
                                        dri.getTypeConfigState().getState() == Dri.TypeConfigState.State.UPDATING)
                           .setListener(operatorId -> {
                               mOperatorId = operatorId;
                               setTypeConfig(dri);
                           });
    }

    private void setTypeConfig(@NonNull Dri dri) {
        if (mType == null) {
            return;
        }
        switch (mType) {
            case FRENCH:
                dri.setTypeConfig(Dri.TypeConfig.ofFrench());
                break;
            case EN4709_002:
                if (!TextUtils.isEmpty(mOperatorId)) {
                    dri.setTypeConfig(Dri.TypeConfig.ofEn4709002(mOperatorId));
                }
                break;
        }
    }

    private void updateTypeConfigState(@NonNull Dri dri) {
        Dri.TypeConfigState state = dri.getTypeConfigState();
        if (state == null) {
            mTypeState.setText(R.string.no_value);
        } else {
            mTypeState.setText(getString(R.string.property_dri_type_state_detail,
                    state.getState().toString(),
                    state.getConfig() != null ? state.getConfig().getType().toString() : getString(R.string.no_value),
                    state.getConfig() != null ? state.getConfig().getOperatorId() : getString(R.string.no_value)));
        }
    }

    private void updateTypeClean(@NonNull Dri dri) {
        mTypeCleanBtn.setEnabled(dri.getTypeConfig() != null);
        mTypeCleanBtn.setOnClickListener(v -> dri.setTypeConfig(null));
    }
}
