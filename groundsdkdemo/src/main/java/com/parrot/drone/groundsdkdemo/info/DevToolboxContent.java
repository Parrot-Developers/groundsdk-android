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
import com.parrot.drone.groundsdk.device.peripheral.DevToolbox;
import com.parrot.drone.groundsdkdemo.DebugTagDialogFragment;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.peripheral.DebugSettingsActivity;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class DevToolboxContent extends PeripheralContent<Drone, DevToolbox> {

    interface OnDebugTagRequestListener {

        void onDebugTagRequest(@NonNull DebugTagDialogFragment.Listener listener);
    }

    DevToolboxContent(@NonNull Drone drone) {
        super(R.layout.dev_toolbox_info, drone, DevToolbox.class);
    }

    @Override
    Content.ViewHolder<?> onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends PeripheralContent.ViewHolder<DevToolboxContent, DevToolbox> {

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mDebugSettingsButton;

        @NonNull
        private final Button mSendDebugTagButton;

        @NonNull
        private final TextView mLatestDebugTagIdView;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mDebugSettingsButton = findViewById(R.id.btn_debug_settings);
            mSendDebugTagButton = findViewById(R.id.btn_debug_tag);
            mLatestDebugTagIdView = findViewById(R.id.debug_tag_id);

            mDebugSettingsButton.setOnClickListener(mClickListener);
            mSendDebugTagButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull DevToolboxContent content, @NonNull DevToolbox devToolbox) {
            String latestDebugTagId = devToolbox.getLatestDebugTagId();
            if (latestDebugTagId != null) {
                mLatestDebugTagIdView.setText(latestDebugTagId);
            } else {
                mLatestDebugTagIdView.setText(R.string.no_value);
            }
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View view, @NonNull DevToolboxContent content, @NonNull DevToolbox devToolbox) {
                if (view == mDebugSettingsButton) {
                    mContext.startActivity(new Intent(mContext, DebugSettingsActivity.class)
                            .putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid()));
                } else if (mContext instanceof OnDebugTagRequestListener) {
                    ((OnDebugTagRequestListener) mContext).onDebugTagRequest(devToolbox::sendDebugTag);
                }
            }
        };
    }
}
