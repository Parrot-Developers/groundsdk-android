/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.CertificateUploader;
import com.parrot.drone.groundsdkdemo.R;

class CertificateUploaderContent extends PeripheralContent<Drone, CertificateUploader> {

    CertificateUploaderContent(@NonNull Drone drone) {
        super(R.layout.certificate_uploader_info, drone, CertificateUploader.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends PeripheralContent.ViewHolder<CertificateUploaderContent, CertificateUploader> {

        @NonNull
        private final TextView mUploadingText;

        @NonNull
        private final Button mUploadButton;
        @NonNull
        private final Button mCancelButton;


        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mUploadingText = findViewById(R.id.is_uploading);
            mUploadButton = findViewById(R.id.upload);
            mCancelButton = findViewById(R.id.cancel_upload);
            mUploadButton.setOnClickListener(mClickListener);
            mCancelButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull CertificateUploaderContent content, @NonNull CertificateUploader certificateUploader) {
            mUploadingText.setText(certificateUploader.isUploading() ? R.string.yes : R.string.no);
            mCancelButton.setEnabled(certificateUploader.isUploading());
        }

        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull CertificateUploaderContent content,
                         @NonNull CertificateUploader certificateUploader) {
                switch (v.getId()) {
                    case R.id.upload:
                        ((OnPickFileRequestListener) mContext).onPickFileRequest(
                                R.string.title_dialog_pick_certificate_file,
                                "sig", certificateUploader::upload);
                        break;
                    case R.id.cancel_upload:
                        certificateUploader.cancel();
                        break;
                }
            }
        };
    }
}
