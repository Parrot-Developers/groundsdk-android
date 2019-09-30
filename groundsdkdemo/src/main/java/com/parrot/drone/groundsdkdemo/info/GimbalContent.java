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
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Gimbal;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdk.value.DoubleSetting;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.format.Html;
import com.parrot.drone.groundsdkdemo.peripheral.GimbalCalibrationActivity;
import com.parrot.drone.groundsdkdemo.settings.GimbalOffsetsCorrectionActivity;
import com.parrot.drone.groundsdkdemo.settings.GimbalSettingsActivity;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class GimbalContent extends PeripheralContent<Drone, Gimbal> {

    GimbalContent(@NonNull Drone drone) {
        super(R.layout.gimbal_info, drone, Gimbal.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends PeripheralContent.ViewHolder<GimbalContent, Gimbal> {

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mEditButton;

        @NonNull
        private final TextView mSupportedAxesText;

        @NonNull
        private final TextView mLockedAxesText;

        @NonNull
        private final TextView mStabilizedAxesText;

        @NonNull
        private final TextView mBoundsText;

        @NonNull
        private final TextView mMaxSpeedsText;

        @NonNull
        private final TextView mAbsoluteAttitudeText;

        @NonNull
        private final TextView mRelativeAttitudeText;

        @NonNull
        private final TextView mCalibratedText;

        @NonNull
        private final TextView mErrorsText;

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mOffsetsCorrectionBtn;

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mCalibrateBtn;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mEditButton = findViewById(R.id.btn_edit);
            mEditButton.setOnClickListener(mClickListener);
            mSupportedAxesText = findViewById(R.id.supported_axes);
            mLockedAxesText = findViewById(R.id.locked_axes);
            mStabilizedAxesText = findViewById(R.id.stabilized_axes);
            mBoundsText = findViewById(R.id.bounds);
            mMaxSpeedsText = findViewById(R.id.max_speeds);
            mAbsoluteAttitudeText = findViewById(R.id.absolute_attitude);
            mRelativeAttitudeText = findViewById(R.id.relative_attitude);
            mCalibratedText = findViewById(R.id.calibrated);
            mErrorsText = findViewById(R.id.errors);
            mOffsetsCorrectionBtn = findViewById(R.id.btn_offsets_correction);
            mOffsetsCorrectionBtn.setOnClickListener(mClickListener);
            mCalibrateBtn = findViewById(R.id.btn_calibrate);
            mCalibrateBtn.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull GimbalContent content, @NonNull Gimbal gimbal) {
            mSupportedAxesText.setText(gimbal.getSupportedAxes().toString());
            mLockedAxesText.setText(gimbal.getLockedAxes().toString());
            bindStabilizedAxes(gimbal);
            bindBounds(gimbal);
            bindMaxSpeeds(gimbal);
            bindAttitude(gimbal, Gimbal.FrameOfReference.ABSOLUTE);
            bindAttitude(gimbal, Gimbal.FrameOfReference.RELATIVE);
            mCalibratedText.setText(Boolean.toString(gimbal.isCalibrated()));
            mErrorsText.setText(TextUtils.join(" ", gimbal.currentErrors()));
        }

        private void bindStabilizedAxes(@NonNull Gimbal gimbal) {
            StringBuilder builder = new StringBuilder("[");
            for (Gimbal.Axis axis : gimbal.getSupportedAxes()) {
                if (gimbal.getStabilization(axis).isEnabled()) {
                    if (builder.length() > 1) {
                        builder.append(", ");
                    }
                    builder.append(axis);
                }
            }
            builder.append("]");
            mStabilizedAxesText.setText(builder.toString());
        }

        private void bindBounds(@NonNull Gimbal gimbal) {
            StringBuilder builder = new StringBuilder();
            for (Gimbal.Axis axis : gimbal.getSupportedAxes()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                DoubleRange range = gimbal.getAttitudeBounds(axis);
                builder.append(axis)
                       .append("=")
                       .append(mContext.getString(R.string.double_range_format, range.getLower(), range.getUpper()));
            }
            mBoundsText.setText(builder.toString());
        }

        private void bindMaxSpeeds(@NonNull Gimbal gimbal) {
            StringBuilder builder = new StringBuilder();
            for (Gimbal.Axis axis : gimbal.getSupportedAxes()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                DoubleSetting setting = gimbal.getMaxSpeed(axis);
                builder.append(axis)
                       .append("=")
                       .append(mContext.getString(R.string.double_setting_format,
                               setting.getMin(), setting.getValue(), setting.getMax()));
            }
            mMaxSpeedsText.setText(Html.fromHtml(builder.toString()));
        }

        private void bindAttitude(@NonNull Gimbal gimbal, @NonNull Gimbal.FrameOfReference frame) {
            StringBuilder builder = new StringBuilder();
            for (Gimbal.Axis axis : gimbal.getSupportedAxes()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(axis)
                       .append("=")
                       .append(mContext.getString(R.string.double_value_format, gimbal.getAttitude(axis, frame)));
            }
            if (frame == Gimbal.FrameOfReference.ABSOLUTE) {
                mAbsoluteAttitudeText.setText(builder.toString());
            } else {
                mRelativeAttitudeText.setText(builder.toString());
            }
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull GimbalContent content, @NonNull Gimbal object) {
                switch (v.getId()) {
                    case R.id.btn_edit:
                        mContext.startActivity(new Intent(mContext, GimbalSettingsActivity.class)
                                .putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid()));
                        break;
                    case R.id.btn_offsets_correction:
                        mContext.startActivity(new Intent(mContext, GimbalOffsetsCorrectionActivity.class)
                                .putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid()));
                        break;
                    case R.id.btn_calibrate:

                        mContext.startActivity(new Intent(mContext, GimbalCalibrationActivity.class)
                                .putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid()));
                        break;
                }
            }
        };
    }
}
