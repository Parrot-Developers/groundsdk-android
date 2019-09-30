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
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.settings.FlightPlanSettingsActivity;

import java.io.File;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class FlightPlanContent extends PilotingItfContent<Drone, FlightPlanPilotingItf> {

    FlightPlanContent(@NonNull Drone drone) {
        super(R.layout.flight_plan_info, drone, FlightPlanPilotingItf.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends PilotingItfContent.ViewHolder<FlightPlanContent, FlightPlanPilotingItf> {

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mEditSettingsButton;

        @NonNull
        private final TextView mStateText;

        @NonNull
        private final TextView mUnavailabilityReasonsText;

        @NonNull
        private final TextView mLatestActivationErrorText;

        @NonNull
        private final TextView mLatestUploadStateText;

        @NonNull
        private final TextView mFlightPlanKnownText;

        @NonNull
        private final TextView mPausedText;

        @NonNull
        private final TextView mLatestMissionItemExecutedText;

        @NonNull
        private final TextView mReturnHomeOnDisconnectText;

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mUploadButton;

        @NonNull
        private final Button mStartButton;

        @NonNull
        private final Button mRestartButton;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mEditSettingsButton = findViewById(android.R.id.edit);
            mEditSettingsButton.setOnClickListener(mClickListener);
            mStateText = findViewById(R.id.state);
            mUnavailabilityReasonsText = findViewById(R.id.reasons);
            mLatestActivationErrorText = findViewById(R.id.activation_error);
            mLatestUploadStateText = findViewById(R.id.upload_state);
            mFlightPlanKnownText = findViewById(R.id.flight_plan_known);
            mPausedText = findViewById(R.id.paused);
            mLatestMissionItemExecutedText = findViewById(R.id.item_executed);
            mReturnHomeOnDisconnectText = findViewById(R.id.returnHomeOnDisconnect);
            mUploadButton = findViewById(R.id.upload);
            mUploadButton.setOnClickListener(mClickListener);
            mStartButton = findViewById(R.id.start);
            mStartButton.setOnClickListener(mClickListener);
            mRestartButton = findViewById(R.id.restart);
            mRestartButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull FlightPlanContent content, @NonNull FlightPlanPilotingItf flightPlan) {
            Activable.State state = flightPlan.getState();
            mStateText.setText(state.toString());
            mUnavailabilityReasonsText.setText(TextUtils.join("\n", flightPlan.getUnavailabilityReasons()));
            mLatestActivationErrorText.setText(flightPlan.getLatestActivationError().toString());
            mLatestUploadStateText.setText(flightPlan.getLatestUploadState().toString());
            mFlightPlanKnownText.setText(String.valueOf(flightPlan.isFlightPlanFileKnown()));
            mPausedText.setText(String.valueOf(flightPlan.isPaused()));
            mLatestMissionItemExecutedText.setText(String.valueOf(flightPlan.getLatestMissionItemExecuted()));
            mReturnHomeOnDisconnectText.setText(flightPlan.getReturnHomeOnDisconnect().isEnabled() ?
                    R.string.boolean_setting_enabled : R.string.boolean_setting_disabled);
            mStartButton.setEnabled(state != Activable.State.UNAVAILABLE);
            mStartButton.setText(state == Activable.State.ACTIVE ? R.string.action_pause : R.string.action_start);
            mRestartButton.setEnabled(state == Activable.State.IDLE);
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull FlightPlanContent content, @NonNull FlightPlanPilotingItf flightPlan) {
                switch (v.getId()) {
                    case android.R.id.edit:
                        mContext.startActivity(new Intent(mContext, FlightPlanSettingsActivity.class)
                                .putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid()));
                        break;
                    case R.id.upload:
                        File mavlinkFile = new File(mContext.getExternalCacheDir(), "flightplan.mavlink");
                        if (mavlinkFile.exists()) {
                            flightPlan.uploadFlightPlan(mavlinkFile);
                        } else {
                            Toast.makeText(mContext, mContext.getString(R.string.toast_missing_flight_plan_file,
                                    mavlinkFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.start:
                        Activable.State state = flightPlan.getState();
                        if (state == Activable.State.IDLE) {
                            flightPlan.activate(false);
                        } else if (state == Activable.State.ACTIVE) {
                            flightPlan.deactivate();
                        }
                        break;
                    case R.id.restart:
                        flightPlan.activate(true);
                        break;
                }
            }
        };
    }
}
