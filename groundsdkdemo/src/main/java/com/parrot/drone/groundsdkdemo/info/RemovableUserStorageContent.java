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

import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.RemovableUserStorage;
import com.parrot.drone.groundsdkdemo.FormatDialogFragment;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.StoragePasswordFragment;

import java.util.EnumSet;

class RemovableUserStorageContent extends PeripheralContent<Drone, RemovableUserStorage> {

    interface OnUserStorageFormatRequestListener {

        void onUserStorageFormatRequest(@Nullable FormatDialogFragment.Listener listener,
                                        @NonNull EnumSet<RemovableUserStorage.FormattingType> supportedTypes,
                                        boolean isEncryptionSupported);
    }

    interface OnUserStorageEnterPasswordListener {

        void onUserStorageEnterPassword(@Nullable StoragePasswordFragment.Listener listener);
    }

    RemovableUserStorageContent(@NonNull Drone drone) {
        super(R.layout.user_storage_info, drone, RemovableUserStorage.class);
    }

    @Override
    Content.ViewHolder<?> onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder
            extends PeripheralContent.ViewHolder<RemovableUserStorageContent, RemovableUserStorage> {

        @NonNull
        private final TextView mFileSystemStateView;

        @NonNull
        private final TextView mPhysicalStateView;

        @NonNull
        private final TextView mNameView;

        @NonNull
        private final TextView mCapacityView;

        @NonNull
        private final TextView mAvailableSpaceView;

        @NonNull
        private final TextView mIsEncryptedView;

        @NonNull
        private final TextView mFormattingTypesView;

        @NonNull
        private final TextView mFormattingStateView;

        @NonNull
        private final Button mFormatButton;

        @NonNull
        private final Button mEnterPasswordButton;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mFileSystemStateView = findViewById(R.id.user_storage_fs_state);
            mPhysicalStateView = findViewById(R.id.user_storage_phy_state);
            mNameView = findViewById(R.id.user_storage_name);
            mCapacityView = findViewById(R.id.user_storage_capacity);
            mAvailableSpaceView = findViewById(R.id.user_storage_available);
            mIsEncryptedView = findViewById(R.id.user_storage_encrypted);
            mFormattingTypesView = findViewById(R.id.user_storage_formatting_types);
            mFormattingStateView = findViewById(R.id.user_storage_formatting_state);
            mFormatButton = findViewById(R.id.btn_format);
            mEnterPasswordButton = findViewById(R.id.btn_enter_password);
            RefContent.ViewHolder<RemovableUserStorageContent, RemovableUserStorage>.OnClickListener mClickListener =
                    new OnClickListener() {
                        @Override
                        void onClick(View v, @NonNull RemovableUserStorageContent content,
                                     @NonNull RemovableUserStorage userStorage) {
                            if (v == mFormatButton) {
                                if (mContext instanceof OnUserStorageFormatRequestListener) {
                                    ((OnUserStorageFormatRequestListener) mContext).onUserStorageFormatRequest(
                                            getFormatAcceptListener(userStorage),
                                            userStorage.supportedFormattingTypes(),
                                            userStorage.isEncryptionSupported());
                                }
                            } else if (v == mEnterPasswordButton) {
                                if (mContext instanceof OnUserStorageEnterPasswordListener) {
                                    ((OnUserStorageEnterPasswordListener) mContext).onUserStorageEnterPassword(
                                            userStorage::sendPassword);
                                }
                            }
                        }
                    };
            mFormatButton.setOnClickListener(mClickListener);
            mEnterPasswordButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull RemovableUserStorageContent content, @NonNull RemovableUserStorage userStorage) {
            String noValue = mContext.getString(R.string.no_value);
            mPhysicalStateView.setText(userStorage.getPhysicalState().name());
            mFileSystemStateView.setText(userStorage.getFileSystemState().name());
            RemovableUserStorage.MediaInfo mediaInfo = userStorage.getMediaInfo();
            if (mediaInfo != null) {
                mNameView.setText(mediaInfo.getName());
                long capacity = mediaInfo.getCapacity();
                mCapacityView.setText(Formatter.formatFileSize(mContext, capacity));
            } else {
                mNameView.setText(noValue);
                mCapacityView.setText(noValue);
            }
            long availableSpace = userStorage.getAvailableSpace();
            if (availableSpace > 0) {
                mAvailableSpaceView.setText(Formatter.formatFileSize(mContext, availableSpace));
            } else {
                mAvailableSpaceView.setText(noValue);
            }
            mIsEncryptedView.setText(Boolean.toString(userStorage.isEncrypted()));
            mFormattingTypesView.setText(userStorage.supportedFormattingTypes().toString());
            RemovableUserStorage.FormattingState state = userStorage.formattingState();
            if (state != null) {
                mFormattingStateView.setText(mContext.getString(R.string.user_storage_formatting_state_format,
                        state.step().toString(), state.progress()));
            } else {
                mFormattingStateView.setText(noValue);
            }
            mFormatButton.setEnabled(userStorage.canFormat());
            mEnterPasswordButton.setEnabled(userStorage.getFileSystemState() == RemovableUserStorage.FileSystemState.PASSWORD_NEEDED);
        }

        private static FormatDialogFragment.Listener getFormatAcceptListener(
                @NonNull RemovableUserStorage userStorage) {
            return new FormatDialogFragment.Listener() {
                @Override
                public void onFormatAccept(@NonNull RemovableUserStorage.FormattingType type, @NonNull String name) {
                    userStorage.format(type, name);
                }

                @Override
                public void onFormatWithEncryptionAccept(@NonNull String password,
                                                         @NonNull RemovableUserStorage.FormattingType type,
                                                         @NonNull String name) {
                    userStorage.formatWithEncryption(password, type, name);
                }
            };
        }
    }
}
