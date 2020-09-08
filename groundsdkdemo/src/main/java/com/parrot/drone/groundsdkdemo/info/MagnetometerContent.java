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
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.Magnetometer;
import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith1StepCalibration;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.peripheral.Magnetometer1StepCalibrationActivity;
import com.parrot.drone.groundsdkdemo.peripheral.Magnetometer3StepCalibrationActivity;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class MagnetometerContent extends PeripheralContent<Peripheral.Provider, Magnetometer> {

    MagnetometerContent(@NonNull Peripheral.Provider device) {
        super(R.layout.magnetometer_info, device, Magnetometer.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends PeripheralContent.ViewHolder<MagnetometerContent, Magnetometer> {

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mCalibrationButton;

        @NonNull
        private final TextView mCalibratedText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mCalibrationButton = findViewById(R.id.calibration_button);
            mCalibratedText = findViewById(R.id.sensor_status_text);
            mCalibrationButton.setOnClickListener(mCalibrationClickListener);
        }

        @Override
        void onBind(@NonNull MagnetometerContent content, @NonNull Magnetometer magnetometer) {
            mCalibratedText.setText(magnetometer.calibrationState().name());
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mCalibrationClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull MagnetometerContent content, @NonNull Magnetometer magnetometer) {
                Intent intent;
                if (content.mDevice.getPeripheral(MagnetometerWith1StepCalibration.class) != null) {
                    intent = new Intent(mContext, Magnetometer1StepCalibrationActivity.class);
                } else {
                    intent = new Intent(mContext, Magnetometer3StepCalibrationActivity.class);
                }
                String uuid = null;
                if (content.mDevice instanceof Drone) {
                    uuid = ((Drone) content.mDevice).getUid();
                } else if (content.mDevice instanceof RemoteControl) {
                    uuid = ((RemoteControl) content.mDevice).getUid();
                }
                if (uuid != null) {
                    intent.putExtra(EXTRA_DEVICE_UID, uuid);
                }
                mContext.startActivity(intent);
            }
        };
    }
}
