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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.peripheral.ThermalStreamActivity;
import com.parrot.drone.groundsdkdemo.peripheral.VideoStreamActivity;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class LiveStreamContent extends PeripheralContent<Drone, StreamServer> {

    LiveStreamContent(@NonNull Drone drone) {
        super(R.layout.stream_info, drone, StreamServer.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends PeripheralContent.ViewHolder<LiveStreamContent, StreamServer> {

        @NonNull
        private final Button mEnableButton;

        @NonNull
        private final Button mSeeStreamButton;

        @NonNull
        private final Button mSeeThermalStreamButton;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mEnableButton = findViewById(R.id.enable_stream);
            mEnableButton.setOnClickListener(mClickListener);
            mSeeStreamButton = findViewById(R.id.see_stream);
            mSeeStreamButton.setOnClickListener(mClickListener);
            mSeeThermalStreamButton = findViewById(R.id.see_thermal_stream);
            mSeeThermalStreamButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull LiveStreamContent content, @NonNull StreamServer streamServer) {
            mEnableButton.setText(Boolean.toString(streamServer.streamingEnabled()));
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View view, @NonNull LiveStreamContent content, @NonNull StreamServer streamServer) {
                if (view == mEnableButton) {
                    streamServer.enableStreaming(!streamServer.streamingEnabled());
                } else if (view == mSeeStreamButton) {
                    Intent intent = new Intent(mContext, VideoStreamActivity.class);
                    intent.putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid());
                    mContext.startActivity(intent);
                } else if (view == mSeeThermalStreamButton) {
                    Intent intent = new Intent(mContext, ThermalStreamActivity.class);
                    intent.putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid());
                    mContext.startActivity(intent);
                }
            }
        };
    }
}
