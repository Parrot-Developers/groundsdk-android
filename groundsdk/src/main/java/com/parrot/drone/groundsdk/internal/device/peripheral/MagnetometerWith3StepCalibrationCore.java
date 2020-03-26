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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith3StepCalibration;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Core class for the MagnetometerWith3StepCalibration. */
public class MagnetometerWith3StepCalibrationCore extends MagnetometerCore implements MagnetometerWith3StepCalibration {

    /** Description of MagnetometerWith3StepCalibration. */
    private static final ComponentDescriptor<Peripheral, MagnetometerWith3StepCalibration> DESC =
            ComponentDescriptor.of(MagnetometerWith3StepCalibration.class, MagnetometerCore.DESC);

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
    public MagnetometerWith3StepCalibrationCore(@NonNull ComponentStore<Peripheral> peripheralStore,
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
     * Updates the current axis to calibrate in the current calibration process.<br>
     * <p>
     * No effect if the calibration process has not been started.<br>
     * Note: changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param axis the axis to calibrate
     *
     * @return the object, to allow chain calls
     */
    @NonNull
    public MagnetometerWith3StepCalibrationCore updateCalibProcessCurrentAxis(CalibrationProcessState.Axis axis) {
        if (mCalibrationProcessState != null && mCalibrationProcessState.updateCurrentAxis(axis)) {
            mChanged = true;
        }

        return this;
    }

    /**
     * Updates the set of calibrated axes in the current calibration process.<br>
     * <p>
     * No effect if the calibration process has not been started.<br>
     * Note: changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param axes the axes that are calibrated
     *
     * @return the object, to allow chain calls
     */
    @NonNull
    public MagnetometerWith3StepCalibrationCore updateCalibProcessCalibratedAxes(
            Set<CalibrationProcessState.Axis> axes) {
        if (mCalibrationProcessState != null && mCalibrationProcessState.updateCalibratedAxes(axes)) {
            mChanged = true;
        }

        return this;
    }

    /**
     * Updates the failed status in the current calibration process.<br>
     * <p>
     * No effect if the calibration process has not been started.<br>
     * Note: changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param failed {@code true} if the calibration process failed, {@code false} otherwise
     *
     * @return the object, to allow chain calls
     */
    public MagnetometerWith3StepCalibrationCore updateCalibProcessFailed(boolean failed) {
        if (mCalibrationProcessState != null && mCalibrationProcessState.updateFailed(failed)) {
            mChanged = true;
        }

        return this;
    }

    /**
     * Ends the calibration process.<br>
     * <p>
     * No effect if the calibration process has not been started.<br>
     * Note: changes are not notified until {@link #notifyUpdated()} is called
     *
     * @return the object, to allow chain calls
     */
    public MagnetometerWith3StepCalibrationCore calibrationProcessStopped() {
        if (mCalibrationProcessState != null) {
            mCalibrationProcessState = null;
            mChanged = true;
        }

        return this;
    }

    //endregion backend methods

    /** Core class for the calibration process state. */
    private static final class CalibrationProcessStateCore implements CalibrationProcessState {

        /** Current axis to calibrate. */
        @NonNull
        private Axis mCurrentAxis;

        /** List of calibrated axes. */
        @NonNull
        private Set<Axis> mCalibratedAxes;

        /** {@code true} if the calibration process failed. */
        private boolean mFailed;

        /**
         * Constructor.
         */
        private CalibrationProcessStateCore() {
            mCalibratedAxes = EnumSet.noneOf(Axis.class);
            mCurrentAxis = Axis.NONE;
        }

        @NonNull
        @Override
        public Axis getCurrentAxis() {
            return mCurrentAxis;
        }

        @NonNull
        @Override
        public Set<Axis> getCalibratedAxes() {
            return Collections.unmodifiableSet(mCalibratedAxes);
        }

        @Override
        public boolean failed() {
            return mFailed;
        }

        /**
         * Updates the current axis to calibrate.
         *
         * @param axis the axis to calibrate
         *
         * @return {@code true} if current axis has changed, {@code false} otherwise
         */
        private boolean updateCurrentAxis(@NonNull Axis axis) {
            if (mCurrentAxis != axis) {
                mCurrentAxis = axis;
                return true;
            }

            return false;
        }

        /**
         * Updates the list of calibrated axes.
         *
         * @param calibratedAxes the list of calibrated axes
         *
         * @return {@code true if list has changed, {@code false} otherwise
         */
        private boolean updateCalibratedAxes(@NonNull Set<Axis> calibratedAxes) {
            if (!mCalibratedAxes.equals(calibratedAxes)) {
                mCalibratedAxes = calibratedAxes;
                return true;
            }

            return false;
        }

        /**
         * Updates the failure flag.
         *
         * @param failed {@code true} if the calibration process failed, {@code false} otherwise
         *
         * @return {@code true} if failure flag has changed, {@code false} otherwise
         */
        private boolean updateFailed(boolean failed) {
            if (mFailed != failed) {
                mFailed = failed;
                return true;
            }

            return false;
        }
    }
}
