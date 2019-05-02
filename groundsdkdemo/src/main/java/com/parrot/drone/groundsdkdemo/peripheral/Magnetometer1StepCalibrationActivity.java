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

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith1StepCalibration;
import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith1StepCalibration.CalibrationProcessState;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class Magnetometer1StepCalibrationActivity extends GroundSdkActivityBase {

    private Ref<MagnetometerWith1StepCalibration> mMagnetometer;

    private ProgressBar mRollState;

    private ProgressBar mPitchState;

    private ProgressBar mYawState;

    private boolean mCalibrationStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String deviceUid = getIntent().getStringExtra(EXTRA_DEVICE_UID);
        Peripheral.Provider provider = groundSdk().getDrone(deviceUid);
        if (provider == null) {
            provider = groundSdk().getRemoteControl(deviceUid);
            if (provider == null) {
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_magneto_1step_calibration);

        mRollState = findViewById(R.id.roll_status);
        mPitchState = findViewById(R.id.pitch_status);
        mYawState = findViewById(R.id.yaw_status);

        findViewById(R.id.cancel_calibration).setOnClickListener(mCancelClickListener);

        mMagnetometer = provider.getPeripheral(MagnetometerWith1StepCalibration.class,
                magnetometer -> {
                    if (magnetometer != null) {

                        CalibrationProcessState calibProcessState = magnetometer.getCalibrationProcessState();
                        if (calibProcessState == null) {
                            if (mCalibrationStarted) {
                                finish();
                            }
                            return;
                        }

                        mRollState.setProgress(calibProcessState.rollProgress());
                        mPitchState.setProgress(calibProcessState.pitchProgress());
                        mYawState.setProgress(calibProcessState.yawProgress());
                    } else {
                        finish();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MagnetometerWith1StepCalibration magnetometer = mMagnetometer.get();
        if (magnetometer != null) {
            magnetometer.startCalibrationProcess();
            mCalibrationStarted = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MagnetometerWith1StepCalibration magnetometer = mMagnetometer.get();
        if (magnetometer != null) {
            magnetometer.cancelCalibrationProcess();
        }
    }

    private final View.OnClickListener mCancelClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            MagnetometerWith1StepCalibration magnetometer = mMagnetometer.get();
            if (magnetometer != null) {
                magnetometer.cancelCalibrationProcess();
            }
        }
    };
}
