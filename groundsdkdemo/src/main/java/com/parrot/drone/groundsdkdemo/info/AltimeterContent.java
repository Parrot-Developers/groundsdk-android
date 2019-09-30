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
import com.parrot.drone.groundsdk.device.instrument.Altimeter;
import com.parrot.drone.groundsdk.value.OptionalDouble;
import com.parrot.drone.groundsdkdemo.R;

class AltimeterContent extends InstrumentContent<Drone, Altimeter> {

    AltimeterContent(@NonNull Drone drone) {
        super(R.layout.altimeter_info, drone, Altimeter.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends InstrumentContent.ViewHolder<AltimeterContent, Altimeter> {

        @NonNull
        private final TextView mTakeOffAltitudeText;

        @NonNull
        private final TextView mGroundAltitudeText;

        @NonNull
        private final TextView mAbsoluteAltitudeText;

        @NonNull
        private final TextView mVerticalSpeedText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mTakeOffAltitudeText = findViewById(R.id.takeoff_altitude);
            mGroundAltitudeText = findViewById(R.id.ground_altitude);
            mAbsoluteAltitudeText = findViewById(R.id.absolute_altitude);
            mVerticalSpeedText = findViewById(R.id.vertical_speed);
        }

        @Override
        void onBind(@NonNull AltimeterContent content, @NonNull Altimeter altimeter) {
            double takeOffAltitude = altimeter.getTakeOffRelativeAltitude();
            mTakeOffAltitudeText.setText(mContext.getString(
                    Double.isNaN(takeOffAltitude) ? R.string.no_value : R.string.altitude_format, takeOffAltitude));

            OptionalDouble groundAltitude = altimeter.getGroundRelativeAltitude();
            mGroundAltitudeText.setText(mContext.getString(
                    groundAltitude.isAvailable() ? R.string.altitude_format : R.string.no_value,
                    groundAltitude.getValue()));

            OptionalDouble absoluteAltitude = altimeter.getAbsoluteAltitude();
            mAbsoluteAltitudeText.setText(mContext.getString(
                    absoluteAltitude.isAvailable() ? R.string.no_value : R.string.altitude_format,
                    absoluteAltitude.getValue()));

            double verticalSpeed = altimeter.getVerticalSpeed();
            mVerticalSpeedText.setText(mContext.getString(
                    Double.isNaN(verticalSpeed) ? R.string.no_value : R.string.speed_format, verticalSpeed));
        }
    }
}
