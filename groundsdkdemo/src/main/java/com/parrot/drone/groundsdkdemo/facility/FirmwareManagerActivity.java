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

package com.parrot.drone.groundsdkdemo.facility;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.parrot.drone.groundsdk.facility.FirmwareManager;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FirmwareManagerActivity extends GroundSdkActivityBase {

    private FirmwareManager mManager;

    private Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_firmware_manager);

        mAdapter = new Adapter();
        RecyclerView recyclerView = findViewById(android.R.id.list);
        DefaultItemAnimator animator = new DefaultItemAnimator();
        // item change animations are a bit too flashy, disable them
        animator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(animator);
        recyclerView.setAdapter(mAdapter);

        groundSdk().getFacility(FirmwareManager.class, manager -> {
            if (manager == null) {
                finish();
                return;
            }
            if (mManager == null) {
                mManager = manager;
                mManager.queryRemoteFirmwares();
            }
            List<FirmwareManager.Entry> entries = new ArrayList<>(mManager.firmwares());
            Collections.sort(entries, SORT_BY_NAME_THEN_VERSION);
            mAdapter.submitList(entries);
        });
    }

    private final class FirmwareEntryHolder extends RecyclerView.ViewHolder {

        @NonNull
        private final TextView mFirmwareIdText;

        @NonNull
        private final TextView mFirmwareInfoText;

        @NonNull
        private final Button mActionBtn;

        @NonNull
        private final ProgressBar mProgressBar;

        FirmwareEntryHolder(ViewGroup parent) {
            super(LayoutInflater.from(parent.getContext()).inflate(R.layout.firmware_entry, parent, false));
            mFirmwareIdText = itemView.findViewById(R.id.identifier);
            mFirmwareInfoText = itemView.findViewById(R.id.info);
            mActionBtn = itemView.findViewById(R.id.action);
            mProgressBar = itemView.findViewById(R.id.progress);
            mActionBtn.setOnClickListener(mClickListener);
        }

        void bind(@NonNull FirmwareManager.Entry entry) {
            FirmwareInfo info = entry.info();
            mFirmwareIdText.setText(info.getFirmware().toString());
            List<String> infoStrs = new ArrayList<>();
            infoStrs.add(Formatter.formatFileSize(itemView.getContext(), info.getSize()));
            for (FirmwareInfo.Attribute attribute : info.getAttributes()) {
                infoStrs.add(attribute.toString());
            }
            mFirmwareInfoText.setText(TextUtils.join(" ", infoStrs));
            FirmwareManager.Entry.State state = entry.state();
            mActionBtn.setText(state == FirmwareManager.Entry.State.DOWNLOADED ?
                    R.string.action_delete : state == FirmwareManager.Entry.State.DOWNLOADING ?
                    R.string.action_cancel : R.string.action_download);
            mActionBtn.setEnabled(state != FirmwareManager.Entry.State.DOWNLOADED || entry.canDelete());
            mProgressBar.setVisibility(state == FirmwareManager.Entry.State.DOWNLOADING ? View.VISIBLE : View.GONE);
            int progress = entry.downloadProgress();
            mProgressBar.setIndeterminate(progress == 0);
            mProgressBar.setProgress(progress);
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final View.OnClickListener mClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                FirmwareManager.Entry entry = mAdapter.getItem(getAdapterPosition());
                FirmwareManager.Entry.State state = entry.state();
                if (state == FirmwareManager.Entry.State.DOWNLOADED) {
                    entry.delete();
                } else if (state == FirmwareManager.Entry.State.NOT_DOWNLOADED) {
                    entry.download();
                } else if (state == FirmwareManager.Entry.State.DOWNLOADING) {
                    entry.cancelDownload();
                }
            }
        };
    }

    private class Adapter extends ListAdapter<FirmwareManager.Entry, FirmwareEntryHolder> {

        Adapter() {
            super(DIFF_CB);
        }

        @NonNull
        @Override
        public FirmwareEntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new FirmwareEntryHolder(parent);
        }

        @Override
        public FirmwareManager.Entry getItem(int position) {
            return super.getItem(position);
        }

        @Override
        public void onBindViewHolder(@NonNull FirmwareEntryHolder holder, int position) {
            holder.bind(getItem(position));
        }
    }

    private static final Comparator<FirmwareManager.Entry> SORT_BY_NAME_THEN_VERSION = (lhs, rhs) -> {
        FirmwareIdentifier lFirmware = lhs.info().getFirmware();
        FirmwareIdentifier rFirmware = rhs.info().getFirmware();

        int nameDiff = lFirmware.getDeviceModel().name().compareTo(rFirmware.getDeviceModel().name());
        if (nameDiff != 0) {
            return nameDiff;
        }
        return lFirmware.getVersion().compareTo(rFirmware.getVersion());
    };

    private static final DiffUtil.ItemCallback<FirmwareManager.Entry> DIFF_CB =
            new DiffUtil.ItemCallback<FirmwareManager.Entry>() {

                @Override
                public boolean areItemsTheSame(FirmwareManager.Entry oldItem, FirmwareManager.Entry newItem) {
                    return oldItem.info().getFirmware().equals(newItem.info().getFirmware());
                }

                @Override
                public boolean areContentsTheSame(@NonNull FirmwareManager.Entry oldItem,
                                                  @NonNull FirmwareManager.Entry newItem) {
                    return false;
                }
            };
}
