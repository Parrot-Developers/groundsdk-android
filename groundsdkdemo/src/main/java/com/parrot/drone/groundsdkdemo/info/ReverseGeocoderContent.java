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

import android.location.Address;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.facility.ReverseGeocoder;
import com.parrot.drone.groundsdkdemo.R;

import java.util.ArrayList;
import java.util.List;

class ReverseGeocoderContent extends FacilityContent<ReverseGeocoder> {

    ReverseGeocoderContent(@NonNull GroundSdk provider) {
        super(R.layout.reverse_geocoder_info, provider, ReverseGeocoder.class);
    }

    @Override
    Content.ViewHolder<?> onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends FacilityContent.ViewHolder<ReverseGeocoderContent, ReverseGeocoder> {

        @NonNull
        private final TextView mAddressText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mAddressText = findViewById(R.id.address);
        }

        @Override
        void onBind(@NonNull ReverseGeocoderContent content, @NonNull ReverseGeocoder reverseGeocoder) {
            Address address = reverseGeocoder.getAddress();
            if (address == null) {
                mAddressText.setText(R.string.no_value);
            } else {
                List<String> fragments = new ArrayList<>();
                for (int i = 0, N = address.getMaxAddressLineIndex(); i <= N; i++) {
                    fragments.add(address.getAddressLine(i));
                }
                mAddressText.setText(TextUtils.join(" ", fragments));
            }
        }
    }
}
