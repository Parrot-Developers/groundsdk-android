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

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.Speedometer;
import com.parrot.drone.groundsdk.value.OptionalDouble;
import com.parrot.drone.groundsdkdemo.R;

class SpeedometerContent extends InstrumentContent<Drone, Speedometer> {

    SpeedometerContent(@NonNull Drone drone) {
        super(R.layout.speedometer_info, drone, Speedometer.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends InstrumentContent.ViewHolder<SpeedometerContent, Speedometer> {

        @NonNull
        private final TextView mGroundSpeedText;

        @NonNull
        private final TextView mNedSpeedText;

        @NonNull
        private final TextView mDroneSpeedText;

        @NonNull
        private final TextView mAirSpeedText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mGroundSpeedText = findViewById(R.id.ground_speed);
            mNedSpeedText = findViewById(R.id.ned_speed);
            mDroneSpeedText = findViewById(R.id.drone_speed);
            mAirSpeedText = findViewById(R.id.air_speed);
        }

        @Override
        void onBind(@NonNull SpeedometerContent content, @NonNull Speedometer speedometer) {
            mGroundSpeedText.setText(mContext.getString(R.string.speed_format, speedometer.getGroundSpeed()));
            mNedSpeedText.setText(mContext.getString(R.string.speed_ned_format, speedometer.getNorthSpeed(),
                    speedometer.getEastSpeed(), speedometer.getDownSpeed()));
            mDroneSpeedText.setText(mContext.getString(R.string.speed_drone_format, speedometer.getForwardSpeed(),
                    speedometer.getRightSpeed()));
            OptionalDouble airSpeed = speedometer.getAirSpeed();
            mAirSpeedText.setText(mContext.getString(airSpeed.isAvailable() ? R.string.speed_format : R.string.no_value,
                    airSpeed.getValue()));
        }
    }
}
