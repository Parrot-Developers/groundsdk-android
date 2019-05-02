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

package com.parrot.drone.groundsdkdemo.settings;

import android.os.Bundle;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.TargetTracker;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class TargetTrackerSettingsActivity extends GroundSdkActivityBase {

    private RangedSettingView mHorizontalFramingPositionView;

    private RangedSettingView mVerticalFramingPositionView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Drone drone = groundSdk().getDrone(getIntent().getStringExtra(EXTRA_DEVICE_UID), uid -> finish());

        if (drone == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_target_tracker_settings);

        mHorizontalFramingPositionView = findViewById(R.id.horizontal_framing_position);
        mVerticalFramingPositionView = findViewById(R.id.vertical_framing_position);

        drone.getPeripheral(TargetTracker.class, tracker -> {
            assert tracker != null;
            TargetTracker.FramingSetting framing = tracker.framing();
            boolean updating = framing.isUpdating();
            double horizontalPos = framing.getHorizontalPosition();
            double verticalPos = framing.getVerticalPosition();
            mHorizontalFramingPositionView
                    .setValue(0, horizontalPos, 1)
                    .setUpdating(updating)
                    .setListener(newHorizontal -> framing.setPosition(newHorizontal, verticalPos));
            mVerticalFramingPositionView
                    .setValue(0, framing.getVerticalPosition(), 1)
                    .setUpdating(updating)
                    .setListener(newVertical -> framing.setPosition(horizontalPos, newVertical));
        });
    }
}
