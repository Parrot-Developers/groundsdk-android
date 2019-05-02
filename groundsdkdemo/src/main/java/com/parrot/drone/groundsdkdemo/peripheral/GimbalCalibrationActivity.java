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

package com.parrot.drone.groundsdkdemo.peripheral;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Gimbal;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class GimbalCalibrationActivity extends GroundSdkActivityBase {

    private Ref<Gimbal> mGimbal;

    private ProgressBar mCalibrationProgress;

    private TextView mCalibrationState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String deviceUid = getIntent().getStringExtra(EXTRA_DEVICE_UID);
        Drone drone = groundSdk().getDrone(deviceUid);
        if (drone == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_gimbal_calibration);

        mCalibrationProgress = findViewById(R.id.calibration_progress);
        mCalibrationState = findViewById(R.id.calibration_state);

        findViewById(R.id.cancel_calibration).setOnClickListener(mCancelClickListener);

        mGimbal = drone.getPeripheral(Gimbal.class,
                gimbal -> {
                    if (gimbal != null) {

                        Gimbal.CalibrationProcessState calibProcessState = gimbal.getCalibrationProcessState();
                        mCalibrationState.setText(calibProcessState.toString());
                        mCalibrationProgress.setVisibility(
                                calibProcessState == Gimbal.CalibrationProcessState.CALIBRATING ?
                                        View.VISIBLE : View.INVISIBLE);
                        if (calibProcessState == Gimbal.CalibrationProcessState.SUCCESS
                            || calibProcessState == Gimbal.CalibrationProcessState.FAILURE) {
                            // message for format result
                            Toast.makeText(this,
                                    calibProcessState == Gimbal.CalibrationProcessState.SUCCESS ?
                                            R.string.gimbal_calibration_succeed : R.string.gimbal_calibration_failed,
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        finish();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Gimbal gimbal = mGimbal.get();
        if (gimbal != null) {
            gimbal.startCalibration();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Gimbal gimbal = mGimbal.get();
        if (gimbal != null) {
            gimbal.cancelCalibration();
        }
    }

    private final View.OnClickListener mCancelClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Gimbal gimbal = mGimbal.get();
            if (gimbal != null) {
                gimbal.cancelCalibration();
            }
        }
    };
}
