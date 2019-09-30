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
import android.widget.Button;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.Beeper;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdkdemo.R;

class BeeperContent extends PeripheralContent<Peripheral.Provider, Beeper> {

    BeeperContent(@NonNull Peripheral.Provider device) {
        super(R.layout.beeper_info, device, Beeper.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    static class ViewHolder extends PeripheralContent.ViewHolder<BeeperContent, Beeper> {

        @NonNull
        private final Button mAlertSoundButton;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mAlertSoundButton = findViewById(R.id.alert_sound_button);
            mAlertSoundButton.setOnClickListener(mClickListener);
        }

        @Override
        void onBind(@NonNull BeeperContent content, @NonNull Beeper beeper) {
            mAlertSoundButton.setText(beeper.isAlertSoundPlaying() ?
                    R.string.action_stop_alert_sound : R.string.action_start_alert_sound);
        }

        @SuppressWarnings("FieldCanBeLocal")
        private final OnClickListener mClickListener = new OnClickListener() {

            @Override
            void onClick(View button, @NonNull BeeperContent content, @NonNull Beeper beeper) {
                if (button == mAlertSoundButton) {
                    if (beeper.isAlertSoundPlaying()) {
                        beeper.stopAlertSound();
                    } else {
                        beeper.startAlertSound();
                    }
                }
            }
        };
    }
}
