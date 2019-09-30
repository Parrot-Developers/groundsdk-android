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
import com.parrot.drone.groundsdk.device.peripheral.CopterMotors;
import com.parrot.drone.groundsdk.device.peripheral.motor.MotorError;
import com.parrot.drone.groundsdkdemo.R;
import com.parrot.drone.groundsdkdemo.format.Html;

class CopterMotorsContent extends PeripheralContent<Drone, CopterMotors> {

    CopterMotorsContent(@NonNull Drone drone) {
        super(R.layout.copter_motors_info, drone, CopterMotors.class);
    }

    @Override
    ViewHolder onCreateViewHolder(@NonNull View rootView) {
        return new ViewHolder(rootView);
    }

    private static final class ViewHolder extends PeripheralContent.ViewHolder<CopterMotorsContent, CopterMotors> {

        private final TextView mFrontLeftErrorText;

        private final TextView mFrontRightErrorText;

        private final TextView mRearLeftErrorText;

        private final TextView mRearRightErrorText;

        ViewHolder(@NonNull View rootView) {
            super(rootView);
            mFrontLeftErrorText = findViewById(R.id.front_left_error);
            mFrontRightErrorText = findViewById(R.id.front_right_error);
            mRearLeftErrorText = findViewById(R.id.rear_left_error);
            mRearRightErrorText = findViewById(R.id.rear_right_error);
        }

        @Override
        void onBind(@NonNull CopterMotorsContent content, @NonNull CopterMotors motors) {
            setErrorText(motors, CopterMotors.Motor.FRONT_LEFT, mFrontLeftErrorText);
            setErrorText(motors, CopterMotors.Motor.FRONT_RIGHT, mFrontRightErrorText);
            setErrorText(motors, CopterMotors.Motor.REAR_LEFT, mRearLeftErrorText);
            setErrorText(motors, CopterMotors.Motor.REAR_RIGHT, mRearRightErrorText);
        }

        private void setErrorText(@NonNull CopterMotors motors, @NonNull CopterMotors.Motor motor,
                                  @NonNull TextView view) {
            MotorError error = motors.getLatestError(motor);
            String errStr;
            if (error == MotorError.NONE) {
                errStr = mContext.getString(R.string.no_error);
            } else {
                errStr = error.toString();
                if (motors.getMotorsCurrentlyInError().contains(motor)) {
                    errStr = mContext.getString(R.string.motor_in_error_format, errStr);
                }
            }
            view.setText(Html.fromHtml(errStr));
        }
    }
}
