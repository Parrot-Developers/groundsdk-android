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
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.Directive;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.FinishedFlightInfo;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.FinishedLocationFlightInfo;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.FinishedRelativeMoveFlightInfo;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.LocationDirective;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.RelativeMoveDirective;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.Type;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.edit.GuidedEditActivity;

import java.util.Locale;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class GuidedContent extends ActivablePilotingItfContent<Drone, GuidedPilotingItf> {

    GuidedContent(@NonNull Drone drone) {
        super(R.layout.guided_info, drone, GuidedPilotingItf.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends ActivablePilotingItfContent.ViewHolder<GuidedContent, GuidedPilotingItf> {

        @NonNull
        private final TextView mCurrentDirective;

        @NonNull
        private final TextView mLatestFinishedFlight;

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mEditSettingsButton;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mCurrentDirective = findViewById(R.id.current_directive);
            mLatestFinishedFlight = findViewById(R.id.latest_finished_flight);
            mEditSettingsButton = findViewById(android.R.id.edit);
            mEditSettingsButton.setOnClickListener(mClickListener);
            mActivateBtn.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull GuidedContent content, @NonNull GuidedPilotingItf guidedPilotingItf) {
            super.onBind(content, guidedPilotingItf);
            if (guidedPilotingItf.getState() == State.ACTIVE) {
                mActivateBtn.setVisibility(View.VISIBLE);
                mActivateBtn.setText(R.string.action_stop_current_move);
            } else {
                mActivateBtn.setVisibility(View.GONE);
            }
            Directive directive = guidedPilotingItf.getCurrentDirective();
            String noValue = mContext.getString(R.string.no_value);
            mCurrentDirective.setText(directive == null ? noValue : getDirectiveAsString(directive));
            FinishedFlightInfo flightInfo = guidedPilotingItf.getLatestFinishedFlightInfo();
            mLatestFinishedFlight.setText(flightInfo == null ? noValue : getFlightInfoAsString(flightInfo));
        }

        private static String getDirectiveAsString(@NonNull Directive directive) {
            if (directive.getType() == Type.ABSOLUTE_LOCATION) {
                LocationDirective locDir = (LocationDirective) directive;
                return String.format(Locale.US, "LocationDirective[%.6f, %.6f, alt=%.2f, Orientation[%s, %.2f]]",
                        locDir.getLatitude(), locDir.getLongitude(), locDir.getAltitude(),
                        locDir.getOrientation().getMode(), locDir.getOrientation().getHeading());
            } else {
                RelativeMoveDirective relDir = (RelativeMoveDirective) directive;
                return String.format(Locale.US, "RelativeMoveDirective[%.2f, %.2f, %.2f, rot=%.2f]",
                        relDir.getForwardComponent(), relDir.getRightComponent(), relDir.getDownwardComponent(),
                        relDir.getHeadingRotation());
            }
        }

        private static String getFlightInfoAsString(@NonNull FinishedFlightInfo flightInfo) {
            if (flightInfo.getType() == Type.ABSOLUTE_LOCATION) {
                FinishedLocationFlightInfo locInfo = (FinishedLocationFlightInfo) flightInfo;
                return String.format(Locale.US, "FinishedLocationFlightInfo[%s, success=%b]",
                        getDirectiveAsString(locInfo.getDirective()), locInfo.wasSuccessful());
            } else {
                FinishedRelativeMoveFlightInfo relInfo = (FinishedRelativeMoveFlightInfo) flightInfo;
                return String.format(Locale.US,
                        "FinishedRelativeMoveFlightInfo[%s, success=%b, actual=[%.2f, %.2f, %.2f, rot=%.2f]]",
                        getDirectiveAsString(relInfo.getDirective()), relInfo.wasSuccessful(),
                        relInfo.getActualForwardComponent(), relInfo.getActualRightComponent(),
                        relInfo.getActualDownwardComponent(), relInfo.getActualHeadingRotation());
            }
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull GuidedContent content, @NonNull GuidedPilotingItf guidedPilotingItf) {
                switch (v.getId()) {
                    case android.R.id.edit:
                        Intent intent = new Intent(mContext, GuidedEditActivity.class);
                        intent.putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid());
                        mContext.startActivity(intent);
                        break;
                    case R.id.activate:
                        guidedPilotingItf.deactivate();
                        break;
                }
            }
        };
    }
}
