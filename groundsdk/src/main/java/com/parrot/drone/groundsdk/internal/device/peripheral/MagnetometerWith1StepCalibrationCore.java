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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith1StepCalibration;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;

/** Core class for the MagnetometerWith1StepCalibration. */
public class MagnetometerWith1StepCalibrationCore extends MagnetometerCore implements MagnetometerWith1StepCalibration {

    /** Description of MagnetometerWith1StepCalibration. */
    private static final ComponentDescriptor<Peripheral, MagnetometerWith1StepCalibration> DESC =
            ComponentDescriptor.of(MagnetometerWith1StepCalibration.class, MagnetometerCore.DESC);

    /**
     * State of the calibration process.<br>
     * Null only if calibration process has not started.
     */
    @Nullable
    private CalibrationProcessStateCore mCalibrationProcessState;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend that should be used to handle changes
     */
    public MagnetometerWith1StepCalibrationCore(@NonNull ComponentStore<Peripheral> peripheralStore,
                                                @NonNull Backend backend) {
        super(DESC, peripheralStore, backend);
    }

    @Nullable
    @Override
    public CalibrationProcessState getCalibrationProcessState() {
        return mCalibrationProcessState;
    }

    @Override
    public void startCalibrationProcess() {
        if (mCalibrationProcessState == null) {
            mCalibrationProcessState = new CalibrationProcessStateCore();
            mBackend.startCalibrationProcess();

            // notify the changes
            mChanged = true;
            notifyUpdated();
        }
    }

    @Override
    public void cancelCalibrationProcess() {
        if (mCalibrationProcessState != null) {
            mBackend.cancelCalibrationProcess();
            mCalibrationProcessState = null;

            // notify the changes
            mChanged = true;
            notifyUpdated();
        }
    }

    //region backend methods

    /**
     * Updates the calibration progress of the magnetometer.<br>
     * <p>
     * Note: changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param roll  calibration progress of roll axis as a percentage, in range [0, 100]
     * @param pitch calibration progress of pitch axis as a percentage, in range [0, 100]
     * @param yaw   calibration progress of yaw axis as a percentage, in range [0, 100]
     *
     * @return the object, to allow chain calls
     */
    @NonNull
    public MagnetometerWith1StepCalibrationCore updateCalibrationProgress(
            @IntRange(from = 0, to = 100) int roll,
            @IntRange(from = 0, to = 100) int pitch,
            @IntRange(from = 0, to = 100) int yaw) {
        if (mCalibrationProcessState != null && mCalibrationProcessState.updateCalibrationProgress(roll, pitch, yaw)) {
            mChanged = true;
        }

        return this;
    }

    //endregion backend methods

    /** Core class for the calibration process state. */
    private static class CalibrationProcessStateCore implements CalibrationProcessState {

        /** Progress of calibration on roll axis. */
        @IntRange(from = 0, to = 100)
        private int mRoll;

        /** Progress of calibration on pitch axis. */
        @IntRange(from = 0, to = 100)
        private int mPitch;

        /** Progress of calibration on yaw axis. */
        @IntRange(from = 0, to = 100)
        private int mYaw;

        @IntRange(from = 0, to = 100)
        @Override
        public int rollProgress() {
            return mRoll;
        }

        @IntRange(from = 0, to = 100)
        @Override
        public int pitchProgress() {
            return mPitch;
        }

        @IntRange(from = 0, to = 100)
        @Override
        public int yawProgress() {
            return mYaw;
        }

        /**
         * Updates the calibration progress.
         *
         * @param roll  calibration progress of roll axis as a percentage, in range [0, 100]
         * @param pitch calibration progress of pitch axis as a percentage, in range [0, 100]
         * @param yaw   calibration progress of yaw axis as a percentage, in range [0, 100]
         *
         * @return true if the calibration progress has changed, false otherwise
         */
        private boolean updateCalibrationProgress(@IntRange(from = 0, to = 100) int roll,
                                                  @IntRange(from = 0, to = 100) int pitch,
                                                  @IntRange(from = 0, to = 100) int yaw) {
            if (mRoll != roll
                || mPitch != pitch
                || mYaw != yaw) {
                mRoll = roll;
                mPitch = pitch;
                mYaw = yaw;
                return true;
            }

            return false;
        }
    }
}
