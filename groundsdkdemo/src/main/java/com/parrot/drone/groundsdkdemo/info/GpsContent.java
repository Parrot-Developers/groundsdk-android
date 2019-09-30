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

import android.location.Location;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.Gps;
import com.parrot.drone.groundsdk.value.OptionalDouble;
import com.parrot.drone.groundsdkdemo.R;

class GpsContent extends InstrumentContent<Drone, Gps> {

    GpsContent(@NonNull Drone drone) {
        super(R.layout.gps_info, drone, Gps.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends InstrumentContent.ViewHolder<GpsContent, Gps> {

        @NonNull
        private final TextView mFixText;

        @NonNull
        private final TextView mLocationText;

        @NonNull
        private final TextView mSatellitesText;

        @NonNull
        private final TextView mVerticalAccuracyText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mFixText = findViewById(R.id.fix);
            mLocationText = findViewById(R.id.location);
            mSatellitesText = findViewById(R.id.satellites);
            mVerticalAccuracyText = findViewById(R.id.vertical_accuracy);
        }

        @Override
        void onBind(@NonNull GpsContent content, @NonNull Gps gps) {
            mFixText.setText(Boolean.toString(gps.isFixed()));

            Location location = gps.lastKnownLocation();
            mLocationText.setText(location == null ? mContext.getString(R.string.no_value) : location.toString());

            mSatellitesText.setText(mContext.getString(R.string.gps_satellites_format, gps.getSatelliteCount()));

            OptionalDouble verticalAccuracy = gps.getVerticalAccuracy();
            mVerticalAccuracyText.setText(mContext.getString(
                    verticalAccuracy.isAvailable() ? R.string.gps_vertical_accuracy_format : R.string.no_value,
                    verticalAccuracy.getValue()));
        }
    }
}
