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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.FollowMePilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.tracking.TrackingIssue;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.settings.FollowMeSettingsActivity;

import java.util.EnumSet;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class FollowMeContent extends ActivablePilotingItfContent<Drone, FollowMePilotingItf> {

    FollowMeContent(@NonNull Drone drone) {
        super(R.layout.follow_me_info, drone, FollowMePilotingItf.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends ActivablePilotingItfContent.ViewHolder<FollowMeContent, FollowMePilotingItf> {

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mEditSettingsButton;

        @NonNull
        private final TextView mModeText;

        @NonNull
        private final TextView mBehaviorText;

        @NonNull
        private final TextView mTrackingIssuesText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mEditSettingsButton = findViewById(android.R.id.edit);
            mEditSettingsButton.setOnClickListener(mClickListener);
            mModeText = findViewById(R.id.mode);
            mBehaviorText = findViewById(R.id.behavior);
            mTrackingIssuesText = findViewById(R.id.issues);
            mActivateBtn.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull FollowMeContent content, @NonNull FollowMePilotingItf followMe) {
            super.onBind(content, followMe);
            mModeText.setText(followMe.mode().getValue().toString());
            mBehaviorText.setText(followMe.getCurrentBehavior().toString());
            EnumSet<TrackingIssue> issues = followMe.getState() == Activable.State.UNAVAILABLE ?
                    followMe.getAvailabilityIssues() : followMe.getQualityIssues();
            mTrackingIssuesText.setText(TextUtils.join("\n", issues));
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View view, @NonNull FollowMeContent content, @NonNull FollowMePilotingItf followMe) {
                switch (view.getId()) {
                    case android.R.id.edit:
                        mContext.startActivity(new Intent(mContext, FollowMeSettingsActivity.class)
                                .putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid()));
                        break;
                    case R.id.activate:
                        Activable.State state = followMe.getState();
                        if (state == Activable.State.IDLE) {
                            followMe.activate();
                        } else if (state == Activable.State.ACTIVE) {
                            followMe.deactivate();
                        }
                        break;
                }
            }
        };
    }
}
