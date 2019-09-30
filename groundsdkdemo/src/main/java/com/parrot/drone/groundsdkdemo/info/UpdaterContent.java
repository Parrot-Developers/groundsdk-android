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

import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.Updater;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdkdemo.R;

import java.util.ArrayList;
import java.util.List;

class UpdaterContent extends PeripheralContent<Peripheral.Provider, Updater> {

    private String mLatestEvent;

    UpdaterContent(@NonNull Peripheral.Provider device) {
        super(R.layout.updater_info, device, Updater.class);
    }

    @Override
    Content.ViewHolder<?> onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    @Override
    void onContentChanged(@NonNull Updater updater, boolean becameAvailable) {
        Updater.Download download = updater.currentDownload();
        Updater.Download.State downloadState = download == null ? null : download.state();
        if (downloadState == Updater.Download.State.FAILED
            || downloadState == Updater.Download.State.CANCELED
            || downloadState == Updater.Download.State.SUCCESS) {
            mLatestEvent = downloadState.toString();
        }

        Updater.Update update = updater.currentUpdate();
        Updater.Update.State updateState = update == null ? null : update.state();
        if (updateState == Updater.Update.State.FAILED
            || updateState == Updater.Update.State.CANCELED
            || updateState == Updater.Update.State.SUCCESS) {
            mLatestEvent = updateState.toString();
        }
    }

    private static final class ViewHolder extends RefContent.ViewHolder<UpdaterContent, Updater> {

        @NonNull
        private final TextView mDownloadableFirmwaresText;

        @NonNull
        private final TextView mDownloadUnavailabilitiesText;

        @NonNull
        private final TextView mDownloadStateText;

        @NonNull
        private final Button mDownloadButton;

        @NonNull
        private final TextView mApplicableFirmwaresText;

        @NonNull
        private final TextView mUpdateUnavailabilitiesText;

        @NonNull
        private final TextView mUpdateStateText;

        @NonNull
        private final TextView mIdealVersionText;

        @NonNull
        private final Button mUpdateButton;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mDownloadableFirmwaresText = findViewById(R.id.downloadable_firmwares);
            mDownloadUnavailabilitiesText = findViewById(R.id.download_unavailabilities);
            mDownloadStateText = findViewById(R.id.download_state);
            mDownloadButton = findViewById(R.id.btn_download);
            mDownloadButton.setOnClickListener(mClickListener);
            mApplicableFirmwaresText = findViewById(R.id.applicable_firmwares);
            mUpdateUnavailabilitiesText = findViewById(R.id.update_unavailabilities);
            mUpdateStateText = findViewById(R.id.update_state);
            mIdealVersionText = findViewById(R.id.ideal_version);
            mUpdateButton = findViewById(R.id.btn_update);
            mUpdateButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull UpdaterContent content, @NonNull Updater updater) {
            List<FirmwareInfo> downloadableFirmwares = updater.downloadableFirmwares();
            mDownloadableFirmwaresText.setText(firmwareListStringOf(downloadableFirmwares));

            mDownloadUnavailabilitiesText.setText(TextUtils.join("\n", updater.downloadUnavailabilityReasons()));

            Updater.Download download = updater.currentDownload();
            if (download == null) {
                mDownloadStateText.setText(R.string.no_value);
            } else {
                mDownloadStateText.setText(mContext.getString(R.string.firmware_download_state_format,
                        download.currentFirmwareIndex(),
                        download.currentFirmware().getFirmware().getVersion().toString(),
                        download.currentFirmwareProgress(), download.totalFirmwareCount(), download.totalProgress()));
            }

            mDownloadButton.setEnabled(updater.downloadUnavailabilityReasons().isEmpty()
                                       && !downloadableFirmwares.isEmpty() || download != null);
            mDownloadButton.setText(download == null ? R.string.action_download : R.string.action_cancel);

            List<FirmwareInfo> applicableFirmwares = updater.applicableFirmwares();
            mApplicableFirmwaresText.setText(firmwareListStringOf(applicableFirmwares));

            mUpdateUnavailabilitiesText.setText(TextUtils.join("\n", updater.updateUnavailabilityReasons()));

            Updater.Update update = updater.currentUpdate();
            if (update == null) {
                mUpdateStateText.setText(R.string.no_value);
            } else {
                mUpdateStateText.setText(mContext.getString(R.string.firmware_update_state_format,
                        update.state(), update.currentFirmwareIndex(),
                        update.currentFirmware().getFirmware().getVersion().toString(),
                        update.currentFirmwareProgress(), update.totalFirmwareCount(), update.totalProgress()));
            }

            FirmwareVersion idealVersion = updater.idealVersion();
            if (idealVersion == null) {
                mIdealVersionText.setText(R.string.no_value);
            } else {
                mIdealVersionText.setText(idealVersion.toString());
            }

            mUpdateButton.setEnabled(updater.updateUnavailabilityReasons().isEmpty()
                                     && !applicableFirmwares.isEmpty() || update != null);
            mUpdateButton.setText(update == null ? R.string.action_update : R.string.action_cancel);

            if (content.mLatestEvent != null) {
                Snackbar.make(itemView, content.mLatestEvent, Snackbar.LENGTH_SHORT).show();
                content.mLatestEvent = null;
            }
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View button, @NonNull UpdaterContent content, @NonNull Updater manager) {
                if (button == mDownloadButton) {
                    if (manager.currentDownload() != null) {
                        manager.cancelDownload();
                    } else {
                        manager.downloadAllFirmwares();
                    }
                } else if (button == mUpdateButton) {
                    if (manager.currentUpdate() != null) {
                        manager.cancelUpdate();
                    } else {
                        manager.updateToLatestFirmware();
                    }
                }
            }
        };

        @NonNull
        private String firmwareListStringOf(@NonNull List<FirmwareInfo> firmwares) {
            List<String> firmwaresInfos = new ArrayList<>();
            for (FirmwareInfo info : firmwares) {
                firmwaresInfos.add(info.getFirmware().getVersion()
                                   + "\u00a0(" + Formatter.formatFileSize(mContext, info.getSize()) + ")");
            }
            return TextUtils.join("\n", firmwaresInfos);
        }
    }
}
