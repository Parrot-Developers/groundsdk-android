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

import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Gimbal;
import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import java.util.Set;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;
import static com.parrot.drone.groundsdkdemo.settings.SettingViewAdapters.updateSetting;

public class GimbalOffsetsCorrectionActivity extends GroundSdkActivityBase {

    @Nullable
    private Drone mDrone;

    private Ref<Gimbal> mGimbal;

    private RangedSettingView mOffsetYawView;

    private RangedSettingView mOffsetPitchView;

    private RangedSettingView mOffsetRollView;

    private boolean mProcessStarted;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String deviceUid = getIntent().getStringExtra(EXTRA_DEVICE_UID);
        mDrone = groundSdk().getDrone(deviceUid, uid -> finish());
        if (mDrone == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_gimbal_offests_correction);
        mOffsetYawView = findViewById(R.id.offset_yaw);
        mOffsetPitchView = findViewById(R.id.offset_pitch);
        mOffsetRollView = findViewById(R.id.offset_roll);

        mGimbal = mDrone.getPeripheral(Gimbal.class, this::updateGimbal);
    }

    private void updateGimbal(@Nullable Gimbal gimbal) {
        if (gimbal == null) {
            finish();
            return;
        }

        Gimbal.OffsetCorrectionProcess offsetCorrectionProcess = gimbal.getOffsetCorrectionProcess();
        if (offsetCorrectionProcess == null) {
            if (mProcessStarted) {
                finish();
            }
            return;
        }

        Set<Gimbal.Axis> calibratableAxes = offsetCorrectionProcess.getCorrectableAxes();
        if (calibratableAxes.contains(Gimbal.Axis.YAW)) {
            updateSetting(mOffsetYawView, offsetCorrectionProcess.getOffset(Gimbal.Axis.YAW));
        } else {
            mOffsetYawView.setAvailable(false);
        }
        if (calibratableAxes.contains(Gimbal.Axis.PITCH)) {
            updateSetting(mOffsetPitchView, offsetCorrectionProcess.getOffset(Gimbal.Axis.PITCH));
        } else {
            mOffsetPitchView.setAvailable(false);
        }
        if (calibratableAxes.contains(Gimbal.Axis.ROLL)) {
            updateSetting(mOffsetRollView, offsetCorrectionProcess.getOffset(Gimbal.Axis.ROLL));
        } else {
            mOffsetRollView.setAvailable(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop offset correction process
        Gimbal gimbal = mGimbal.get();
        if (gimbal != null) {
            gimbal.stopOffsetsCorrectionProcess();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // start offset correction process
        Gimbal gimbal = mGimbal.get();
        if (gimbal != null) {
            gimbal.startOffsetsCorrectionProcess();
            mProcessStarted = true;
        }


        if (mDrone != null) {
            mDrone.getPeripheral(StreamServer.class, streamServer -> {
                if (streamServer == null) {
                    finish();
                } else {
                    streamServer.enableStreaming(true);
                }
            });
        }
    }
}
