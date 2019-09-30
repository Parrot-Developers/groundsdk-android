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
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.Alarms;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.format.Html;

class AlarmsContent extends InstrumentContent<Drone, Alarms> {

    AlarmsContent(@NonNull Drone drone) {
        super(R.layout.alarms_info, drone, Alarms.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    static class ViewHolder extends InstrumentContent.ViewHolder<AlarmsContent, Alarms> {

        @NonNull
        private final TextView mAlarmsText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mAlarmsText = findViewById(R.id.alarms);
        }

        @Override
        void onBind(@NonNull AlarmsContent content, @NonNull Alarms alarms) {
            String[] alarmStrings = new String[Alarms.Alarm.Kind.values().length];
            for (int i = 0; i < alarmStrings.length; i++) {
                alarmStrings[i] = formatAlarm(alarms.getAlarm(Alarms.Alarm.Kind.values()[i]));
            }
            mAlarmsText.setText(Html.fromHtml(TextUtils.join(", ", alarmStrings)));
        }

        @NonNull
        private String formatAlarm(@NonNull Alarms.Alarm alarm) {
            Alarms.Alarm.Kind kind = alarm.getKind();
            switch (alarm.getLevel()) {
                case CRITICAL:
                    return mContext.getString(R.string.critical_alarm_format, kind);
                case WARNING:
                    return mContext.getString(R.string.warning_alarm_format, kind);
                case OFF:
                    return mContext.getString(R.string.off_alarm_format, kind);
                case NOT_SUPPORTED:
                    return mContext.getString(R.string.not_supported_alarm_format, kind);
            }
            throw new IllegalArgumentException("Unsupported alarm " + alarm);
        }
    }
}
