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
import android.location.Location;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Geofence;
import com.parrot.drone.groundsdk.value.DoubleSetting;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.format.Html;
import com.parrot.drone.groundsdkdemo.settings.GeofenceSettingsActivity;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

class GeofenceContent extends PeripheralContent<Drone, Geofence> {

    GeofenceContent(@NonNull Drone drone) {
        super(R.layout.geofence_info, drone, Geofence.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends PeripheralContent.ViewHolder<GeofenceContent, Geofence> {

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final Button mEditButton;

        @NonNull
        private final TextView mMaxAltitudeText;

        @NonNull
        private final TextView mMaxDistanceText;

        @NonNull
        private final TextView mModeText;

        @NonNull
        private final TextView mCenterText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mEditButton = findViewById(R.id.btn_edit);
            mEditButton.setOnClickListener(mClickListener);
            mMaxAltitudeText = findViewById(R.id.max_altitude);
            mMaxDistanceText = findViewById(R.id.max_distance);
            mModeText = findViewById(R.id.mode);
            mCenterText = findViewById(R.id.center);
        }

        @Override
        void onBind(@NonNull GeofenceContent content, @NonNull Geofence geofence) {
            Location location = geofence.getCenter();
            setDoubleSetting(mMaxAltitudeText, geofence.maxAltitude());
            setDoubleSetting(mMaxDistanceText, geofence.maxDistance());
            mModeText.setText(geofence.mode().getValue().toString());
            mCenterText.setText(location == null ? mContext.getString(R.string.no_value) : location.toString());
        }

        private void setDoubleSetting(@NonNull TextView view, @NonNull DoubleSetting setting) {
            view.setText(Html.fromHtml(mContext.getString(R.string.double_setting_format,
                    setting.getMin(), setting.getValue(), setting.getMax())));
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View v, @NonNull GeofenceContent content, @NonNull Geofence object) {
                switch (v.getId()) {
                    case R.id.btn_edit:
                        Intent intent = new Intent(mContext, GeofenceSettingsActivity.class);
                        intent.putExtra(EXTRA_DEVICE_UID, content.mDevice.getUid());
                        mContext.startActivity(intent);
                        break;
                }
            }
        };
    }
}
