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
import android.widget.TextView;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith3StepCalibration;
import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith3StepCalibration.CalibrationProcessState;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import java.util.Set;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;

public class Magnetometer3StepCalibrationActivity extends GroundSdkActivityBase {

    private Ref<MagnetometerWith3StepCalibration> mMagnetometer;

    private TextView mCurrentAxis;

    private TextView mRollState;

    private TextView mPitchState;

    private TextView mYawState;

    private TextView mFailed;

    private boolean mCalibrationStarted;

    private boolean mLastCalibrationFailed;

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

        setContentView(R.layout.activity_magneto_3step_calibration);

        mCurrentAxis = findViewById(R.id.current_axis);
        mRollState = findViewById(R.id.roll_status);
        mPitchState = findViewById(R.id.pitch_status);
        mYawState = findViewById(R.id.yaw_status);
        mFailed = findViewById(R.id.calibration_failed);

        findViewById(R.id.cancel_calibration).setOnClickListener(mCancelClickListener);

        mMagnetometer = provider.getPeripheral(MagnetometerWith3StepCalibration.class,
                magnetometer -> {
                    if (magnetometer != null) {

                        CalibrationProcessState calibProcessState = magnetometer.getCalibrationProcessState();
                        if (calibProcessState == null) {
                            if (mCalibrationStarted && !mLastCalibrationFailed) {
                                finish();
                            }
                            return;
                        }

                        mCurrentAxis.setText(getString(R.string.axis_to_calibrate, calibProcessState.getCurrentAxis()));

                        Set<CalibrationProcessState.Axis> calibratedAxes = calibProcessState.getCalibratedAxes();
                        boolean rollCalibrated = calibratedAxes.contains(CalibrationProcessState.Axis.ROLL);
                        boolean pitchCalibrated = calibratedAxes.contains(CalibrationProcessState.Axis.PITCH);
                        boolean yawCalibrated = calibratedAxes.contains(CalibrationProcessState.Axis.YAW);
                        mRollState.setText(rollCalibrated ? R.string.calibration_OK : R.string.calibration_KO);
                        mPitchState.setText(pitchCalibrated ? R.string.calibration_OK : R.string.calibration_KO);
                        mYawState.setText(yawCalibrated ? R.string.calibration_OK : R.string.calibration_KO);
                        mFailed.setText(Boolean.toString(calibProcessState.failed()));
                        mLastCalibrationFailed = calibProcessState.failed();
                    } else {
                        finish();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MagnetometerWith3StepCalibration magnetometer = mMagnetometer.get();
        if (magnetometer != null) {
            magnetometer.startCalibrationProcess();
            mCalibrationStarted = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MagnetometerWith3StepCalibration magnetometer = mMagnetometer.get();
        if (magnetometer != null) {
            magnetometer.cancelCalibrationProcess();
        }
    }

    private final View.OnClickListener mCancelClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            MagnetometerWith3StepCalibration magnetometer = mMagnetometer.get();
            if (magnetometer != null) {
                magnetometer.cancelCalibrationProcess();
            }
        }
    };
}
