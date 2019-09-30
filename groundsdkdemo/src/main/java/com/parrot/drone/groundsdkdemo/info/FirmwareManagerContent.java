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

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.facility.FirmwareManager;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.facility.FirmwareManagerActivity;

import java.util.Set;

class FirmwareManagerContent extends FacilityContent<FirmwareManager> {

    FirmwareManagerContent(@NonNull GroundSdk provider) {
        super(R.layout.firmware_manager_info, provider, FirmwareManager.class);
    }

    @Override
    Content.ViewHolder<?> onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends FacilityContent.ViewHolder<FirmwareManagerContent, FirmwareManager> {

        @NonNull
        private final Button mBrowseBtn;

        @NonNull
        private final TextView mFirmwareCountText;

        @NonNull
        private final TextView mLocalCountText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mBrowseBtn = findViewById(R.id.btn_browse);
            mBrowseBtn.setOnClickListener(mClickListener);
            mFirmwareCountText = findViewById(R.id.all_count);
            mLocalCountText = findViewById(R.id.local_count);
        }

        @Override
        void onBind(@NonNull FirmwareManagerContent content, @NonNull FirmwareManager manager) {
            Set<FirmwareManager.Entry> firmwares = manager.firmwares();
            int allCount = firmwares.size();
            int localCount = 0;
            for (FirmwareManager.Entry entry : firmwares) {
                if (entry.state() == FirmwareManager.Entry.State.DOWNLOADED) {
                    localCount++;
                }
            }
            mFirmwareCountText.setText(mContext.getString(R.string.int_value_format, allCount));
            mLocalCountText.setText(mContext.getString(R.string.int_value_format, localCount));
            mBrowseBtn.setEnabled(allCount > 0);
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View view, @NonNull FirmwareManagerContent content, @NonNull FirmwareManager manager) {
                mContext.startActivity(new Intent(mContext, FirmwareManagerActivity.class));
            }
        };
    }
}
