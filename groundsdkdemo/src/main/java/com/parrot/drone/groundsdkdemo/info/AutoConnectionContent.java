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

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.facility.AutoConnection;
import com.parrot.drone.groundsdkdemo.R;

class AutoConnectionContent extends FacilityContent<AutoConnection> {

    AutoConnectionContent(@NonNull GroundSdk provider) {
        super(R.layout.auto_connection_info, provider, AutoConnection.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends FacilityContent.ViewHolder<AutoConnectionContent, AutoConnection> {

        @NonNull
        private final Button mStartButton;

        @NonNull
        private final TextView mDroneText;

        @NonNull
        private final TextView mRemoteControlText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mStartButton = findViewById(R.id.btn_start);
            mDroneText = findViewById(R.id.drone);
            mRemoteControlText = findViewById(R.id.remote_control);
            mStartButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull AutoConnectionContent content, @NonNull AutoConnection autoConnection) {
            mStartButton.setText(autoConnection.getStatus() == AutoConnection.Status.STOPPED ?
                    R.string.action_start : R.string.action_stop);
            Drone drone = autoConnection.getDrone();
            mDroneText.setText(drone == null ? mContext.getString(R.string.no_value) : drone.getName());
            RemoteControl remoteControl = autoConnection.getRemoteControl();
            mRemoteControlText.setText(remoteControl == null ?
                    mContext.getString(R.string.no_value) : remoteControl.getName());
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View view, @NonNull AutoConnectionContent content, @NonNull AutoConnection autoConnection) {
                if (autoConnection.getStatus() == AutoConnection.Status.STOPPED) {
                    autoConnection.start();
                } else {
                    autoConnection.stop();
                }
            }
        };
    }
}
