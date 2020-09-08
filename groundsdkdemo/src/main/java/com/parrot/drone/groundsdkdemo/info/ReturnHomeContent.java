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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.ReturnHomePilotingItf;
import com.parrot.drone.groundsdk.value.IntSetting;
import com.parrot.drone.groundsdk.value.OptionalDoubleSetting;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.format.Html;
import com.parrot.drone.groundsdkdemo.settings.ReturnHomeSettingsActivity;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class ReturnHomeContent extends ActivablePilotingItfContent<Drone, ReturnHomePilotingItf> {

    ReturnHomeContent(@NonNull Drone drone) {
        super(R.layout.return_home_info, drone, ReturnHomePilotingItf.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends ActivablePilotingItfContent.ViewHolder<ReturnHomeContent, ReturnHomePilotingItf> {

        @NonNull
        private final TextView mAutoTriggerSwitchStateText;

        @NonNull
        private final TextView mReasonText;

        @NonNull
        private final TextView mLocationText;

        @NonNull
        private final TextView mCurrentTargetText;

        @NonNull
        private final TextView mFixedOnTakeOffText;

        @NonNull
        private final TextView mPreferredTargetText;

        @NonNull
        private final TextView mEndingBehaviour;

        @NonNull
        private final TextView mAutostartOnDisconnectDelayText;

        @NonNull
        private final TextView mMinAltitudeText;

        @NonNull
        private final TextView mHoveringAltitude;

        @NonNull
        private final TextView mHomeReachabilityText;

        @NonNull
        private final TextView mWarningTriggerDelayText;

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mEditSettingsButton;

        @SuppressLint("SimpleDateFormat")
        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mReasonText = findViewById(R.id.reason);
            mAutoTriggerSwitchStateText = findViewById(R.id.autoTriggerSwitch);
            mLocationText = findViewById(R.id.location);
            mCurrentTargetText = findViewById(R.id.currentTarget);
            mFixedOnTakeOffText = findViewById(R.id.fixed);
            mPreferredTargetText = findViewById(R.id.preferredTarget);
            mEndingBehaviour = findViewById(R.id.endingBehavior);
            mAutostartOnDisconnectDelayText = findViewById(R.id.delay);
            mMinAltitudeText = findViewById(R.id.min_altitude);
            mHoveringAltitude = findViewById(R.id.hovering_altitude);
            mHomeReachabilityText = findViewById(R.id.reachability);
            mWarningTriggerDelayText = findViewById(R.id.trigger_date);
            mEditSettingsButton = findViewById(android.R.id.edit);
            mEditSettingsButton.setOnClickListener(mClickListener);
            mActivateBtn.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull ReturnHomeContent content, @NonNull ReturnHomePilotingItf returnHome) {
            super.onBind(content, returnHome);
            mReasonText.setText(returnHome.getReason().toString());
            mAutoTriggerSwitchStateText.setText(mContext.getString(returnHome.autoTrigger().isEnabled()
                    ? R.string.boolean_setting_enabled : R.string.boolean_setting_disabled));
            Location location = returnHome.getHomeLocation();
            mLocationText.setText(location == null ? mContext.getString(R.string.no_value) : location.toString());
            mCurrentTargetText.setText(returnHome.getCurrentTarget().toString());
            mFixedOnTakeOffText.setText(Boolean.toString(returnHome.gpsWasFixedOnTakeOff()));
            mPreferredTargetText.setText(returnHome.getPreferredTarget().getValue().toString());
            mEndingBehaviour.setText(returnHome.getEndingBehavior().getValue().toString());
            setIntSetting(mAutostartOnDisconnectDelayText, returnHome.getAutoStartOnDisconnectDelay());
            setDoubleSetting(mMinAltitudeText, returnHome.getMinAltitude());
            setDoubleSetting(mHoveringAltitude, returnHome.getEndingHoveringAltitude());
            mHomeReachabilityText.setText(returnHome.getHomeReachability().name());
            long delay = returnHome.getAutoTriggerDelay();
            mWarningTriggerDelayText.setText(
                    delay == 0 ? mContext.getString(R.string.no_value) : Long.toString(delay));
        }

        private void setIntSetting(@NonNull TextView view, @NonNull IntSetting setting) {
            view.setText(Html.fromHtml(mContext.getString(R.string.int_setting_format,
                    setting.getMin(), setting.getValue(), setting.getMax())));
        }

        private void setDoubleSetting(@NonNull TextView view, @NonNull OptionalDoubleSetting setting) {
            view.setText(Html.fromHtml(mContext.getString(
                    setting.isAvailable() ? R.string.double_setting_format : R.string.no_value,
                    setting.getMin(), setting.getValue(), setting.getMax())));
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull ReturnHomeContent content, @NonNull ReturnHomePilotingItf returnHome) {
                switch (v.getId()) {
                    case android.R.id.edit:
                        Intent intent = new Intent(mContext, ReturnHomeSettingsActivity.class);
                        intent.putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid());
                        mContext.startActivity(intent);
                        break;
                    case R.id.activate:
                        Activable.State state = returnHome.getState();
                        if (state == Activable.State.IDLE) {
                            returnHome.activate();
                        } else if (state == Activable.State.ACTIVE) {
                            returnHome.deactivate();
                        }
                        break;
                }
            }
        };
    }
}
