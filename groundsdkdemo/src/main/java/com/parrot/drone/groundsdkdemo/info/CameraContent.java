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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.BlendedThermalCamera;
import com.parrot.drone.groundsdk.device.peripheral.MainCamera;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.ThermalCamera;
import com.parrot.drone.groundsdk.device.peripheral.camera.Camera;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraAlignment;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposureLock;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraRecording;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraStyle;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalance;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalanceLock;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraZoom;
import com.parrot.drone.groundsdk.value.OptionalBooleanSetting;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.settings.CameraSettingsActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class CameraContent<C extends Camera & Peripheral> extends PeripheralContent<Drone, C> {

    private final Handler mRecordDurationRefresh = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            contentChanged();
            sendEmptyMessageDelayed(0, TimeUnit.SECONDS.toMillis(1));
        }
    };

    @NonNull
    private final Class<C> mCameraClass;

    @Nullable
    private Enum<?> mLatestError;

    @NonNull
    static CameraContent<MainCamera> main(@NonNull Drone drone) {
        return new CameraContent<>(drone, MainCamera.class);
    }

    @NonNull
    static CameraContent<ThermalCamera> thermal(@NonNull Drone drone) {
        return new CameraContent<>(drone, ThermalCamera.class);
    }

    @NonNull
    static CameraContent<BlendedThermalCamera> blendedThermal(@NonNull Drone drone) {
        return new CameraContent<>(drone, BlendedThermalCamera.class);
    }

    private CameraContent(@NonNull Drone drone, @NonNull Class<C> cameraClass) {
        super(R.layout.camera_info, drone, cameraClass);
        mCameraClass = cameraClass;
    }

    @Override
    void onContentChanged(@NonNull Camera camera, boolean becameAvailable) {
        // process transient errors
        CameraRecording.State.FunctionState recordingState = camera.recordingState().get();
        if (recordingState == CameraRecording.State.FunctionState.ERROR_INSUFFICIENT_STORAGE_SPACE
            || recordingState == CameraRecording.State.FunctionState.ERROR_INSUFFICIENT_STORAGE_SPEED
            || recordingState == CameraRecording.State.FunctionState.ERROR_INTERNAL) {
            mLatestError = recordingState;
        }
        CameraPhoto.State.FunctionState photoState = camera.photoState().get();
        if (photoState == CameraPhoto.State.FunctionState.ERROR_INSUFFICIENT_STORAGE
            || photoState == CameraPhoto.State.FunctionState.ERROR_INTERNAL) {
            mLatestError = photoState;
        }

        // refresh record duration depending on state
        refreshRecordDuration(camera.recordingState().get() == CameraRecording.State.FunctionState.STARTED);
    }

    @Override
    void onContentUnavailable() {
        refreshRecordDuration(false);
    }

    private void refreshRecordDuration(boolean start) {
        if (start && !mRecordDurationRefresh.hasMessages(0)) {
            mRecordDurationRefresh.sendEmptyMessage(0);
        } else if (!start && mRecordDurationRefresh.hasMessages(0)) {
            mRecordDurationRefresh.removeMessages(0);
        }
    }

    @Override
    ViewHolder<C> onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder<>(rootView);
    }

    private static final class ViewHolder<C extends Camera & Peripheral>
            extends PeripheralContent.ViewHolder<CameraContent<C>, C> {

        @NonNull
        private final TextView mCameraNameText;

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mEditButton;

        @NonNull
        private final TextView mActiveStateText;

        @NonNull
        private final TextView mModeText;

        @NonNull
        private final TextView mExposureText;

        @NonNull
        private final TextView mExposureLockText;

        @NonNull
        private final TextView mEvCompensationText;

        @NonNull
        private final TextView mWhiteBalanceText;

        @NonNull
        private final TextView mWhiteBalanceLockText;

        @NonNull
        private final TextView mPhotoStateText;

        @NonNull
        private final TextView mRecordingStateText;

        @NonNull
        private final TextView mAutoRecordText;

        @NonNull
        private final TextView mHdrText;

        @NonNull
        private final TextView mStyleText;

        @NonNull
        private final TextView mAlignmentText;

        @NonNull
        private final TextView mZoomInfoText;

        @NonNull
        private final Button mPhotoButton;

        @NonNull
        private final Button mRecordButton;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mCameraNameText = findViewById(android.R.id.title);
            mEditButton = findViewById(R.id.btn_edit);
            mEditButton.setOnClickListener(mClickListener);
            mActiveStateText = findViewById(R.id.active_state);
            mModeText = findViewById(R.id.mode);
            mExposureText = findViewById(R.id.exposure);
            mExposureLockText = findViewById(R.id.exposure_lock);
            mEvCompensationText = findViewById(R.id.ev);
            mWhiteBalanceText = findViewById(R.id.white_balance);
            mWhiteBalanceLockText = findViewById(R.id.white_balance_lock);
            mPhotoStateText = findViewById(R.id.photo_state);
            mRecordingStateText = findViewById(R.id.recording_state);
            mAutoRecordText = findViewById(R.id.auto_record);
            mHdrText = findViewById(R.id.hdr);
            mStyleText = findViewById(R.id.style);
            mAlignmentText = findViewById(R.id.alignment);
            mZoomInfoText = findViewById(R.id.zoom_info);
            mPhotoButton = findViewById(R.id.btn_photo);
            mRecordButton = findViewById(R.id.btn_record);
            mPhotoButton.setOnClickListener(mClickListener);
            mRecordButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull CameraContent<C> content, @NonNull C camera) {
            bindName(content.mCameraClass);
            mActiveStateText.setText(Boolean.toString(camera.isActive()));
            bindMode(camera);
            mExposureText.setText(camera.exposure().mode().toString());
            bindExposureLock(camera.exposureLock());
            mEvCompensationText.setText(camera.exposureCompensation().getValue().toString());
            bindWhiteBalance(camera.whiteBalance());
            bindWhiteBalanceLock(camera.whiteBalanceLock());
            bindPhotoState(camera.photoState());
            bindRecordingState(camera.recordingState());
            mAutoRecordText.setText(camera.autoRecord().isEnabled() ?
                    R.string.boolean_setting_enabled : R.string.boolean_setting_disabled);
            bindHdr(camera);
            bindStyle(camera.style());
            bindAlignment(camera.alignment());
            bindZoom(camera.zoom());
            mPhotoButton.setText(camera.canStopPhotoCapture() ?
                    R.string.action_stop_photo_capture : R.string.action_start_photo_capture);
            mPhotoButton.setEnabled(camera.canStartPhotoCapture() || camera.canStopPhotoCapture());
            mRecordButton.setText(camera.canStopRecording() ?
                    R.string.action_stop_recording : R.string.action_start_recording);
            mRecordButton.setEnabled(camera.canStartRecording() || camera.canStopRecording());
            if (content.mLatestError != null) {
                Snackbar.make(itemView, content.mLatestError.toString(), Snackbar.LENGTH_SHORT).show();
                content.mLatestError = null;
            }
        }

        private void bindName(@NonNull Class<C> cameraClass) {
            @StringRes int nameRes;
            if (cameraClass == MainCamera.class) {
                nameRes = R.string.peripheral_camera_main;
            } else if (cameraClass == ThermalCamera.class) {
                nameRes = R.string.peripheral_camera_thermal;
            } else if (cameraClass == BlendedThermalCamera.class) {
                nameRes = R.string.peripheral_camera_thermal_blended;
            } else {
                throw new IllegalArgumentException("Unsupported camera class");
            }
            mCameraNameText.setText(nameRes);
        }

        private void bindMode(@NonNull Camera camera) {
            List<String> modes = new ArrayList<>();
            Camera.Mode mode = camera.mode().getValue();
            modes.add(mode.toString());
            switch (mode) {
                case RECORDING:
                    CameraRecording.Setting recording = camera.recording();
                    CameraRecording.Mode recordingMode = recording.mode();
                    modes.add(recordingMode.toString());
                    modes.add(recording.resolution().toString());
                    modes.add(recording.framerate().toString());
                    if (recordingMode == CameraRecording.Mode.HYPERLAPSE) {
                        modes.add(recording.hyperlapseValue().toString());
                    }
                    modes.add(mContext.getString(R.string.bitrate_format, recording.bitrate()));
                    break;
                case PHOTO:
                    CameraPhoto.Setting photo = camera.photo();
                    CameraPhoto.Mode photoMode = photo.mode();
                    modes.add(photoMode.toString());
                    modes.add(photo.format().toString());
                    modes.add(photo.fileFormat().toString());
                    if (photoMode == CameraPhoto.Mode.BRACKETING) {
                        modes.add(photo.bracketingValue().toString());
                    } else if (photoMode == CameraPhoto.Mode.BURST) {
                        modes.add(photo.burstValue().toString());
                    } else if (photoMode == CameraPhoto.Mode.TIME_LAPSE) {
                        modes.add(mContext.getString(R.string.double_value_format, photo.timelapseInterval()));
                    } else if (photoMode == CameraPhoto.Mode.GPS_LAPSE) {
                        modes.add(mContext.getString(R.string.double_value_format, photo.gpslapseInterval()));
                    }
                    break;
            }
            mModeText.setText(TextUtils.join(" ", modes));
        }

        private void bindExposureLock(@Nullable CameraExposureLock cameraExposureLock) {
            if (cameraExposureLock != null) {
                if (cameraExposureLock.mode() == CameraExposureLock.Mode.REGION) {
                    mExposureLockText.setText(mContext.getString(R.string.exposure_lock_region_format,
                            cameraExposureLock.mode().toString(),
                            cameraExposureLock.getRegionCenterX(),
                            cameraExposureLock.getRegionCenterY(),
                            cameraExposureLock.getRegionWidth(),
                            cameraExposureLock.getRegionHeight()));
                } else {
                    mExposureLockText.setText(cameraExposureLock.mode().toString());
                }
            } else {
                mExposureLockText.setText(R.string.no_value);
            }
        }

        private void bindWhiteBalance(@NonNull CameraWhiteBalance.Setting whiteBalance) {
            CameraWhiteBalance.Mode mode = whiteBalance.mode();
            String whiteBalanceText = mode.toString();
            if (mode == CameraWhiteBalance.Mode.CUSTOM) {
                whiteBalanceText += " " + whiteBalance.customTemperature();
            }
            mWhiteBalanceText.setText(whiteBalanceText);
        }

        private void bindWhiteBalanceLock(@Nullable CameraWhiteBalanceLock whiteBalanceLock) {
            if (whiteBalanceLock != null) {
                mWhiteBalanceLockText.setText(mContext.getString(R.string.white_balance_lock_format,
                        whiteBalanceLock.isLockable(), whiteBalanceLock.isLocked()));
            } else {
                mWhiteBalanceLockText.setText(R.string.no_value);
            }
        }

        private void bindPhotoState(@NonNull CameraPhoto.State state) {

            mPhotoStateText.setText(mContext.getString(R.string.photo_state_format, state.get().toString(),
                    state.latestMediaId(), state.photoCount()));

        }

        private void bindRecordingState(@NonNull CameraRecording.State state) {
            Date startTime = state.recordStartTime();
            long duration = state.recordDuration();
            mRecordingStateText.setText(mContext.getString(R.string.recording_state_format, state.get().toString(),
                    state.latestMediaId(),
                    startTime == null ? mContext.getString(R.string.no_value) : startTime.toString(),
                    duration == 0 ? mContext.getString(R.string.no_value) : DateUtils.formatElapsedTime(
                            TimeUnit.MILLISECONDS.toSeconds(duration))));
        }

        private void bindStyle(@NonNull CameraStyle.Setting style) {
            mStyleText.setText(
                    mContext.getString(R.string.style_format, style.style().name(), style.saturation().getValue(),
                            style.contrast().getValue(), style.sharpness().getValue()));
        }

        private void bindAlignment(@Nullable CameraAlignment.Setting alignment) {
            if (alignment != null) {
                mAlignmentText.setText(
                        mContext.getString(R.string.alignment_format, alignment.yaw(), alignment.pitch(),
                                alignment.roll()));
            } else {
                mAlignmentText.setText(R.string.no_value);
            }
        }

        private void bindHdr(@NonNull Camera camera) {
            OptionalBooleanSetting hdr = camera.autoHdr();
            if (hdr.isAvailable()) {
                mHdrText.setText(mContext.getString(R.string.hdr_format, hdr.isEnabled(), camera.isHdrAvailable(),
                        camera.isHdrActive()));
            } else {
                mHdrText.setText(R.string.no_value);
            }
        }

        private void bindZoom(@Nullable CameraZoom zoom) {
            if (zoom != null) {
                mZoomInfoText.setText(mContext.getString(R.string.zoom_info_format,
                        mContext.getString(zoom.isAvailable() ?
                                R.string.property_camera_zoom_available : R.string.property_camera_zoom_unavailable),
                        zoom.getCurrentLevel(), zoom.getMaxLossLessLevel(), zoom.getMaxLossyLevel()));
            } else {
                mZoomInfoText.setText(R.string.no_value);
            }
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull CameraContent<C> content, @NonNull C camera) {
                switch (v.getId()) {
                    case R.id.btn_photo:
                        if (camera.canStartPhotoCapture()) {
                            camera.startPhotoCapture();
                        } else if (camera.canStopPhotoCapture()) {
                            camera.stopPhotoCapture();
                        }
                        break;
                    case R.id.btn_record:
                        if (camera.canStartRecording()) {
                            camera.startRecording();
                        } else if (camera.canStopRecording()) {
                            camera.stopRecording();
                        }
                        break;
                    case R.id.btn_edit:
                        CameraSettingsActivity.launch(mContext, content.mDevice, content.mCameraClass);
                        break;
                }
            }
        };
    }
}
