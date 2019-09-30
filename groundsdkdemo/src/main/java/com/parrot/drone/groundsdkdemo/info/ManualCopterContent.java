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

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf;
import com.parrot.drone.groundsdk.value.DoubleSetting;
import com.parrot.drone.groundsdk.value.OptionalBooleanSetting;
import com.parrot.drone.groundsdk.value.OptionalDoubleSetting;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.format.Html;
import com.parrot.drone.groundsdkdemo.settings.ManualCopterSettingsActivity;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class ManualCopterContent extends ActivablePilotingItfContent<Drone, ManualCopterPilotingItf> {

    ManualCopterContent(@NonNull Drone drone) {
        super(R.layout.manual_copter_info, drone, ManualCopterPilotingItf.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends ActivablePilotingItfContent.ViewHolder<ManualCopterContent, ManualCopterPilotingItf> {

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mEditSettingsButton;

        @NonNull
        private final TextView mSmartTakeOffLandText;

        @NonNull
        private final TextView mTakeOffLandText;

        @NonNull
        private final TextView mMaxPitchRollText;

        @NonNull
        private final TextView mMaxPitchRollVelocityText;

        @NonNull
        private final TextView mMaxVerticalSpeedText;

        @NonNull
        private final TextView mMaxYawSpeedText;

        @NonNull
        private final TextView mBankedTurnModeText;

        @NonNull
        private final TextView mThrownTakeOffSettingText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mEditSettingsButton = findViewById(android.R.id.edit);
            mSmartTakeOffLandText = findViewById(R.id.smart_takeoffland);
            mTakeOffLandText = findViewById(R.id.can_takeoffland);
            mMaxPitchRollText = findViewById(R.id.maxPitchRoll);
            mMaxPitchRollVelocityText = findViewById(R.id.maxPitchRollVelocity);
            mMaxVerticalSpeedText = findViewById(R.id.maxVerticalSpeed);
            mMaxYawSpeedText = findViewById(R.id.maxYawSpeed);
            mBankedTurnModeText = findViewById(R.id.bankedTurnMode);
            mThrownTakeOffSettingText = findViewById(R.id.thrownTakeOffSetting);
            mEditSettingsButton.setOnClickListener(mClickListener);
            mActivateBtn.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull ManualCopterContent content, @NonNull ManualCopterPilotingItf manualCopter) {
            super.onBind(content, manualCopter);

            mSmartTakeOffLandText.setText(manualCopter.getSmartTakeOffLandAction().name());
            mTakeOffLandText.setText(manualCopter.canTakeOff() ? R.string.can_take_off :
                    manualCopter.canLand() ? R.string.can_land : R.string.no_value);

            setDoubleSetting(mMaxPitchRollText, manualCopter.getMaxPitchRoll());
            setDoubleSetting(mMaxPitchRollVelocityText, manualCopter.getMaxPitchRollVelocity());
            setDoubleSetting(mMaxVerticalSpeedText, manualCopter.getMaxVerticalSpeed());
            setDoubleSetting(mMaxYawSpeedText, manualCopter.getMaxYawRotationSpeed());
            setBooleanSetting(mBankedTurnModeText, manualCopter.getBankedTurnMode());
            setBooleanSetting(mThrownTakeOffSettingText, manualCopter.getThrownTakeOffMode());
        }

        private void setDoubleSetting(@NonNull TextView view, @NonNull OptionalDoubleSetting setting) {
            view.setText(Html.fromHtml(mContext.getString(
                    setting.isAvailable() ? R.string.double_setting_format : R.string.no_value,
                    setting.getMin(), setting.getValue(), setting.getMax())));
        }

        private void setDoubleSetting(@NonNull TextView view, @NonNull DoubleSetting setting) {
            view.setText(Html.fromHtml(mContext.getString(R.string.double_setting_format,
                    setting.getMin(), setting.getValue(), setting.getMax())));
        }

        private static void setBooleanSetting(@NonNull TextView view, @NonNull OptionalBooleanSetting setting) {
            view.setText(setting.isAvailable()
                    ? setting.isEnabled() ? R.string.boolean_setting_enabled : R.string.boolean_setting_disabled
                    : R.string.no_value);
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull ManualCopterContent content, @NonNull ManualCopterPilotingItf manualCopter) {
                switch (v.getId()) {
                    case android.R.id.edit:
                        Intent intent = new Intent(mContext, ManualCopterSettingsActivity.class);
                        intent.putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid());
                        mContext.startActivity(intent);
                        break;
                    case R.id.activate:
                        Activable.State state = manualCopter.getState();
                        if (state == Activable.State.IDLE) {
                            manualCopter.activate();
                        } else if (state == Activable.State.ACTIVE) {
                            manualCopter.deactivate();
                        }
                        break;
                }
            }
        };
    }
}
