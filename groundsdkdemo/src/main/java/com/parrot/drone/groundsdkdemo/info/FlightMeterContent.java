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
import com.parrot.drone.groundsdk.device.instrument.FlightMeter;
import com.parrot.drone.groundsdkdemo.R;

import java.util.Locale;

class FlightMeterContent extends InstrumentContent<Drone, FlightMeter> {

    // We don't use SimpleDateFormat as we want to display something like 29 h 50 m 21 s which isn't a date
    private static final String HOUR_MINUTE_SECOND_FORMAT = "%d h %d m %d s";

    private static final String MINUTE_SECOND_FORMAT = "%d m %d s";

    private static final String SECOND_FORMAT = "%d s";

    FlightMeterContent(@NonNull Drone drone) {
        super(R.layout.flight_meter_info, drone, FlightMeter.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends InstrumentContent.ViewHolder<FlightMeterContent, FlightMeter> {

        @NonNull
        private final TextView mLastFlightDurationText;

        @NonNull
        private final TextView mTotalFlightDurationText;

        @NonNull
        private final TextView mTotalFlightCountText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mLastFlightDurationText = findViewById(R.id.last_flight_duration);
            mTotalFlightDurationText = findViewById(R.id.total_flight_duration);
            mTotalFlightCountText = findViewById(R.id.total_flight_count);
        }

        @Override
        void onBind(@NonNull FlightMeterContent content, @NonNull FlightMeter flightMeter) {
            mLastFlightDurationText.setText(formatDuration(flightMeter.getLastFlightDuration()));
            mTotalFlightDurationText.setText(formatDuration(flightMeter.getTotalFlightDuration()));
            mTotalFlightCountText.setText(String.valueOf(flightMeter.getTotalFlightCount()));
        }

        @NonNull
        private static String formatDuration(long duration) {
            int hours = (int) (duration / 3600);
            int minutes = (int) (duration / 60) % 60;
            int seconds = (int) duration % 60;
            if (hours > 0) {
                return String.format(Locale.US, HOUR_MINUTE_SECOND_FORMAT, hours, minutes, seconds);
            } else if (minutes > 0) {
                return String.format(Locale.US, MINUTE_SECOND_FORMAT, minutes, seconds);
            } else {
                return String.format(Locale.US, SECOND_FORMAT, seconds);
            }
        }
    }
}