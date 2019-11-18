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
import com.parrot.drone.groundsdk.device.pilotingitf.Activable.State;
import com.parrot.drone.groundsdk.device.pilotingitf.PointOfInterestPilotingItf;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.edit.PointOfInterestEditActivity;

import java.util.Locale;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class PointOfInterestContent extends ActivablePilotingItfContent<Drone, PointOfInterestPilotingItf> {

    PointOfInterestContent(@NonNull Drone drone) {
        super(R.layout.poi_info, drone, PointOfInterestPilotingItf.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends ActivablePilotingItfContent.ViewHolder<PointOfInterestContent, PointOfInterestPilotingItf> {

        @NonNull
        private final TextView mCurrentPOI;

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mEditSettingsButton;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mCurrentPOI = findViewById(R.id.current_poi);
            mEditSettingsButton = findViewById(android.R.id.edit);
            mEditSettingsButton.setOnClickListener(mClickListener);
            mActivateBtn.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull PointOfInterestContent content,
                    @NonNull PointOfInterestPilotingItf pointOfInterestPilotingItf) {
            super.onBind(content, pointOfInterestPilotingItf);
            if (pointOfInterestPilotingItf.getState() == State.ACTIVE) {
                mActivateBtn.setVisibility(View.VISIBLE);
                mActivateBtn.setText(R.string.action_deactivate);
            } else {
                mActivateBtn.setVisibility(View.GONE);
            }
            PointOfInterestPilotingItf.PointOfInterest pointOfInterest =
                    pointOfInterestPilotingItf.getCurrentPointOfInterest();
            String displayedPOI;
            if (pointOfInterest == null) {
                displayedPOI = mContext.getString(R.string.no_value);
            } else {
                displayedPOI = String.format(Locale.US, "%.6f, %.6f, alt=%.2f, %s",
                        pointOfInterest.getLatitude(), pointOfInterest.getLongitude(), pointOfInterest.getAltitude(),
                        pointOfInterest.getMode());
            }
            mCurrentPOI.setText(displayedPOI);
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull PointOfInterestContent content, @NonNull
                    PointOfInterestPilotingItf pointOfInterestPilotingItf) {
                switch (v.getId()) {
                    case android.R.id.edit:
                        Intent intent = new Intent(mContext, PointOfInterestEditActivity.class);
                        intent.putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid());
                        mContext.startActivity(intent);
                        break;
                    case R.id.activate:
                        pointOfInterestPilotingItf.deactivate();
                        break;
                }
            }
        };
    }
}
