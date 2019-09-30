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

import com.parrot.drone.groundsdk.device.peripheral.Gimbal;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.value.BooleanSettingCore;
import com.parrot.drone.groundsdk.internal.value.DoubleSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;
import com.parrot.drone.groundsdk.value.BooleanSetting;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdk.value.DoubleSetting;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/** Core class for Gimbal. */
public class GimbalCore extends SingletonComponentCore implements Gimbal {

    /** Description of Gimbal. */
    private static final ComponentDescriptor<Peripheral, Gimbal> DESC = ComponentDescriptor.of(Gimbal.class);

    /** Default value for attitude bounds. */
    private static final DoubleRange DEFAULT_ATTITUDE_BOUNDS = DoubleRange.of(0, 0);

    /** Engine-specific backend for Gimbal. */
    public interface Backend {

        /**
         * Sets maximum speed on the given axis.
         *
         * @param axis  the axis to which the new max speed will apply
         * @param speed maximum speed to set, in degrees per second
         *
         * @return {@code true} if the setting could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setMaxSpeed(@NonNull Axis axis, double speed);

        /**
         * Sets stabilization on the given axis.
         *
         * @param axis       the axis to which the new stabilization will apply
         * @param stabilized {@code true} to stabilize gimbal on this axis, otherwise {@code false}
         *
         * @return {@code true} if the setting could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setStabilization(@NonNull Axis axis, boolean stabilized);

        /**
         * Sets offset on the given axis.
         *
         * @param axis   the axis to which the new offset will apply
         * @param offset offset to set, in degrees
         *
         * @return {@code true} if the setting could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setOffset(@NonNull Axis axis, double offset);

        /**
         * Controls the gimbal.
         *
         * @param mode  the mode that should be used to move the gimbal
         * @param yaw   target on the yaw axis, or {@code null} if you want to keep the current value
         * @param pitch target on the pitch axis, or {@code null} if you want to keep the current value
         * @param roll  target on the roll axis, or {@code null} if you want to keep the current value
         */
        void control(@NonNull ControlMode mode, @Nullable Double yaw, @Nullable Double pitch, @Nullable Double roll);

        /**
         * Starts the offsets correction process.
         *
         * @return {@code true} if the command could successfully be sent to the device, {@code false} otherwise
         */
        boolean startOffsetCorrectionProcess();

        /**
         * Stops the offsets correction process.
         *
         * @return {@code true} if the command could successfully be sent to the device, {@code false} otherwise
         */
        boolean stopOffsetCorrectionProcess();

        /**
         * Starts calibration process.
         *
         * @return {@code true} if the command could successfully be sent to the device, {@code false} otherwise
         */
        boolean startCalibration();

        /**
         * Cancels the current calibration process.
         *
         * @return {@code true} if the command could successfully be sent to the device, {@code false} otherwise
         */
        boolean cancelCalibration();
    }

    /** Engine peripheral backend. */
    @NonNull
    private final Backend mBackend;

    /** All supported axes. */
    @NonNull
    private final EnumSet<Axis> mSupportedAxes;

    /** Attitude bounds by axis, in degrees. Non null for all supported axes. */
    @NonNull
    private final EnumMap<Axis, DoubleRange> mAttitudeBounds;

    /** Maximum speed setting by axis, in degrees per second. Non null for all supported axes. */
    @NonNull
    private final EnumMap<Axis, DoubleSettingCore> mMaxSpeeds;

    /** Currently locked axes. */
    @NonNull
    private final EnumSet<Axis> mLockedAxes;

    /** Stabilization setting by axis. Non null for all supported axes. */
    @NonNull
    private final EnumMap<Axis, BooleanSettingCore> mStabilizedAxes;

    /** Current absolute attitude for each axis, in degrees. Non null for all supported axes. */
    @NonNull
    private final EnumMap<Axis, Double> mAbsoluteAttitude;

    /** Current relative attitude for each axis, in degrees. Non null for all supported axes. */
    @NonNull
    private final EnumMap<Axis, Double> mRelativeAttitude;

    /** Current gimbal errors. */
    @NonNull
    private final EnumSet<Error> mErrors;

    /**
     * Offsets correction process.<br>
     * {@code null} only if offsets correction process has not started.
     */
    @Nullable
    private OffsetCorrectionProcessCore mOffsetCorrectionProcess;

    /** Whether gimbal is calibrated. */
    private boolean mIsCalibrated;

    /** Current calibration process state. */
    @NonNull
    private CalibrationProcessState mCalibrationProcessState;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this component provider belongs
     * @param backend         backend used to forward actions to the engine
     */
    public GimbalCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
        mSupportedAxes = EnumSet.noneOf(Axis.class);
        mAttitudeBounds = new EnumMap<>(Axis.class);
        mMaxSpeeds = new EnumMap<>(Axis.class);
        mLockedAxes = EnumSet.noneOf(Axis.class);
        mStabilizedAxes = new EnumMap<>(Axis.class);
        mAbsoluteAttitude = new EnumMap<>(Axis.class);
        mRelativeAttitude = new EnumMap<>(Axis.class);
        mErrors = EnumSet.noneOf(Error.class);
        mCalibrationProcessState = CalibrationProcessState.NONE;
    }

    @Override
    public void unpublish() {
        super.unpublish();
        cancelSettingsRollbacks();
    }

    @NonNull
    @Override
    public Set<Axis> getSupportedAxes() {
        return Collections.unmodifiableSet(mSupportedAxes);
    }

    @NonNull
    @Override
    public DoubleRange getAttitudeBounds(@NonNull Axis axis) {
        checkAxisSupport(axis);
        //noinspection ConstantConditions: validated by checkAxisSupport
        return mAttitudeBounds.get(axis);
    }

    @NonNull
    @Override
    public DoubleSetting getMaxSpeed(@NonNull Axis axis) {
        checkAxisSupport(axis);
        //noinspection ConstantConditions: validated by checkAxisSupport
        return mMaxSpeeds.get(axis);
    }

    @NonNull
    @Override
    public Set<Axis> getLockedAxes() {
        return Collections.unmodifiableSet(mLockedAxes);
    }

    @NonNull
    @Override
    public BooleanSetting getStabilization(@NonNull Axis axis) {
        checkAxisSupport(axis);
        //noinspection ConstantConditions: validated by checkAxisSupport
        return mStabilizedAxes.get(axis);
    }

    @Override
    public double getAttitude(@NonNull Axis axis) {
        checkAxisSupport(axis);
        //noinspection ConstantConditions: validated by checkAxisSupport
        return mStabilizedAxes.get(axis).isEnabled() ? mAbsoluteAttitude.get(axis) : mRelativeAttitude.get(axis);
    }

    @Override
    public double getAttitude(@NonNull Axis axis, @NonNull FrameOfReference frame) {
        checkAxisSupport(axis);
        //noinspection ConstantConditions: validated by checkAxisSupport
        return frame == FrameOfReference.ABSOLUTE ? mAbsoluteAttitude.get(axis) : mRelativeAttitude.get(axis);
    }

    @Override
    public void control(@NonNull ControlMode mode, @Nullable Double yaw, @Nullable Double pitch,
                        @Nullable Double roll) {
        mBackend.control(mode,
                mSupportedAxes.contains(Axis.YAW) ? yaw : null,
                mSupportedAxes.contains(Axis.PITCH) ? pitch : null,
                mSupportedAxes.contains(Axis.ROLL) ? roll : null);
    }

    @Nullable
    @Override
    public OffsetCorrectionProcess getOffsetCorrectionProcess() {
        return mOffsetCorrectionProcess;
    }

    @Override
    public void startOffsetsCorrectionProcess() {
        if (mOffsetCorrectionProcess == null) {
            mBackend.startOffsetCorrectionProcess();
        }
    }

    @Override
    public void stopOffsetsCorrectionProcess() {
        if (mOffsetCorrectionProcess != null) {
            mBackend.stopOffsetCorrectionProcess();
        }
    }

    @Override
    public boolean isCalibrated() {
        return mIsCalibrated;
    }

    @NonNull
    @Override
    public CalibrationProcessState getCalibrationProcessState() {
        return mCalibrationProcessState;
    }

    @Override
    public void startCalibration() {
        if (mCalibrationProcessState != CalibrationProcessState.CALIBRATING) {
            mBackend.startCalibration();
        }
    }

    @Override
    public void cancelCalibration() {
        if (mCalibrationProcessState == CalibrationProcessState.CALIBRATING) {
            mBackend.cancelCalibration();
        }
    }

    @NonNull
    @Override
    public EnumSet<Error> currentErrors() {
        return EnumSet.copyOf(mErrors);
    }

    /**
     * Checks that the given axis is supported.
     *
     * @param axis the axis to check
     *
     * @throws IllegalArgumentException in case the specified axis is not a supported axis
     */
    private void checkAxisSupport(@NonNull Axis axis) {
        if (!mSupportedAxes.contains(axis)) {
            throw new IllegalArgumentException("Unsupported axis: " + axis);
        }
    }

    /**
     * Updates supported axes.
     * <p>
     * <strong>Note:</strong> this will also remove all axes that are not supported from the other gimbal attributes
     * (like max speeds, locked axes...), and add all supported axes to locked axes attribute.
     *
     * @param axes new supported axes
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateSupportedAxes(@NonNull EnumSet<Axis> axes) {
        if (mSupportedAxes.retainAll(axes) | mSupportedAxes.addAll(axes)) {
            // remove all axes that are not supported from the other gimbal attributes
            mAttitudeBounds.keySet().retainAll(axes);
            mLockedAxes.retainAll(axes);
            mAbsoluteAttitude.keySet().retainAll(axes);
            mRelativeAttitude.keySet().retainAll(axes);
            for (Axis unsupportedAxis : EnumSet.complementOf(axes)) {
                DoubleSettingCore maxSpeedSetting = mMaxSpeeds.remove(unsupportedAxis);
                if (maxSpeedSetting != null) {
                    maxSpeedSetting.cancelRollback();
                }
                BooleanSettingCore stabilizedAxisSetting = mStabilizedAxes.remove(unsupportedAxis);
                if (stabilizedAxisSetting != null) {
                    stabilizedAxisSetting.cancelRollback();
                }
            }
            // add values for new supported axes
            for (Axis axis : axes) {
                if (mAttitudeBounds.get(axis) == null) {
                    mAttitudeBounds.put(axis, DEFAULT_ATTITUDE_BOUNDS);
                }
                DoubleSettingCore doubleSetting = mMaxSpeeds.get(axis);
                if (doubleSetting == null) {
                    doubleSetting = new DoubleSettingCore(new SettingController(this::onSettingChange),
                            speed -> mBackend.setMaxSpeed(axis, speed));
                    mMaxSpeeds.put(axis, doubleSetting);
                }
                BooleanSettingCore booleanSetting = mStabilizedAxes.get(axis);
                if (booleanSetting == null) {
                    booleanSetting = new BooleanSettingCore(new SettingController(this::onSettingChange),
                            value -> mBackend.setStabilization(axis, value));
                    mStabilizedAxes.put(axis, booleanSetting);
                }
                if (mAbsoluteAttitude.get(axis) == null) {
                    mAbsoluteAttitude.put(axis, 0d);
                }
                if (mRelativeAttitude.get(axis) == null) {
                    mRelativeAttitude.put(axis, 0d);
                }
            }

            // by default all supported axes are locked
            mLockedAxes.addAll(axes);

            mChanged = true;
        }
        return this;
    }

    /**
     * Updates attitude bounds on the given axis.
     * <p>
     * <strong>Note:</strong> this will only apply the update if the axis is supported.
     *
     * @param axis   the axis to which the new bounds will apply
     * @param bounds the new bounds (in degrees), or {@code null} to reset bounds to the default value
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateAttitudeBounds(@NonNull Axis axis, @Nullable DoubleRange bounds) {
        if (bounds == null) {
            bounds = DEFAULT_ATTITUDE_BOUNDS;
        }
        if (mSupportedAxes.contains(axis) && !bounds.equals(mAttitudeBounds.get(axis))) {
            mAttitudeBounds.put(axis, bounds);
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates maximum speed setting values.
     * <p>
     * <strong>Note:</strong> this will apply the update on supported axes only.
     *
     * @param maxSpeeds the new maximum speed setting values
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateMaxSpeeds(@NonNull EnumMap<Axis, Double> maxSpeeds) {
        for (Axis axis : maxSpeeds.keySet()) {
            Double value = maxSpeeds.get(axis);
            if (mSupportedAxes.contains(axis) && value != null) {
                //noinspection ConstantConditions:
                mMaxSpeeds.get(axis).updateValue(value);
            }
        }
        return this;
    }

    /**
     * Updates maximum speed setting ranges.
     * <p>
     * <strong>Note:</strong> this will apply the update on supported axes only.
     *
     * @param maxSpeedRanges the new maximum speed setting ranges
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateMaxSpeedRanges(@NonNull EnumMap<Axis, DoubleRange> maxSpeedRanges) {
        for (Axis axis : maxSpeedRanges.keySet()) {
            DoubleRange range = maxSpeedRanges.get(axis);
            if (mSupportedAxes.contains(axis) && range != null) {
                //noinspection ConstantConditions: validated by mSupportedAxes.contains
                mMaxSpeeds.get(axis).updateBounds(range);
            }
        }
        return this;
    }

    /**
     * Updates temporarily locked axes.
     * <p>
     * <strong>Note:</strong> only supported axes will be updated.
     *
     * @param axes new set of locked axes
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateLockedAxes(@NonNull EnumSet<Axis> axes) {
        axes.retainAll(mSupportedAxes);
        mChanged |= mLockedAxes.retainAll(axes) | mLockedAxes.addAll(axes);
        return this;
    }

    /**
     * Updates stabilization setting.
     * <p>
     * <strong>Note:</strong> this will apply the update on supported axes only.
     *
     * @param stabilizedAxes the new stabilization status by axis
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateStabilization(@NonNull EnumSet<Axis> stabilizedAxes) {
        for (Axis axis : mSupportedAxes) {
            //noinspection ConstantConditions: validated because in mSupportedAxes
            mStabilizedAxes.get(axis).updateValue(stabilizedAxes.contains(axis));
        }
        return this;
    }

    /**
     * Updates current absolute attitude on the given axis.
     * <p>
     * <strong>Note:</strong> this will only apply the update if the axis is supported.
     *
     * @param axis     the axis to which the new attitude will apply
     * @param attitude the new attitude on the given axis, in degrees
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateAbsoluteAttitude(@NonNull Axis axis, double attitude) {
        if (mSupportedAxes.contains(axis)) {
            //noinspection ConstantConditions: validated because in mSupportedAxes
            if (Double.compare(mAbsoluteAttitude.get(axis), attitude) != 0) {
                mAbsoluteAttitude.put(axis, attitude);
                mChanged = true;
            }
        }
        return this;
    }

    /**
     * Updates current relative attitude on the given axis.
     * <p>
     * <strong>Note:</strong> this will only apply the update if the axis is supported.
     *
     * @param axis     the axis to which the new attitude will apply
     * @param attitude the new attitude on the given axis, in degrees
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateRelativeAttitude(@NonNull Axis axis, double attitude) {
        if (mSupportedAxes.contains(axis)) {
            //noinspection ConstantConditions: validated because in mSupportedAxes
            if (Double.compare(mRelativeAttitude.get(axis), attitude) != 0) {
                mRelativeAttitude.put(axis, attitude);
                mChanged = true;
            }
        }
        return this;
    }

    /**
     * Updates the offsets correction process state.
     *
     * @param started {@code trues} when offsets correction process is started, {@code false} otherwise
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateOffsetCorrectionProcessState(boolean started) {
        if (started && (mOffsetCorrectionProcess == null)) {
            mOffsetCorrectionProcess = new OffsetCorrectionProcessCore();
            mChanged = true;
        } else if (!started && (mOffsetCorrectionProcess != null)) {
            mOffsetCorrectionProcess.cancelRollbacks();
            mOffsetCorrectionProcess = null;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates correctable axes.
     * <p>
     * <strong>Note:</strong> this will also remove all axes that are not correctable from offsets attributes.
     *
     * @param axes new correctable axes
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateCorrectableAxes(@NonNull EnumSet<Axis> axes) {
        if (mOffsetCorrectionProcess != null && (mOffsetCorrectionProcess.mCorrectableAxes.retainAll(axes)
                                                 | mOffsetCorrectionProcess.mCorrectableAxes.addAll(axes))) {
            // remove all axes that are not correctable from the offsets attributes
            for (Axis uncorrectableAxis : EnumSet.complementOf(axes)) {
                DoubleSettingCore offsetSetting = mOffsetCorrectionProcess.mOffsets.remove(uncorrectableAxis);
                if (offsetSetting != null) {
                    offsetSetting.cancelRollback();
                }
            }
            // add values for new correctable axes
            for (Axis axis : axes) {
                DoubleSettingCore offsetSetting = mOffsetCorrectionProcess.mOffsets.get(axis);
                if (offsetSetting == null) {
                    offsetSetting = new DoubleSettingCore(new SettingController(this::onSettingChange),
                            offset -> mBackend.setOffset(axis, offset));
                    mOffsetCorrectionProcess.mOffsets.put(axis, offsetSetting);
                }
            }

            mChanged = true;

        }
        return this;
    }

    /**
     * Updates offset settings values.
     * <p>
     * <strong>Note:</strong> this will apply the update on correctable axes only.
     *
     * @param offsets the new offset settings values
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateOffsets(@NonNull EnumMap<Axis, Double> offsets) {
        if (mOffsetCorrectionProcess != null) {
            for (Axis axis : offsets.keySet()) {
                Double value = offsets.get(axis);
                if (mOffsetCorrectionProcess.mCorrectableAxes.contains(axis) && value != null) {
                    //noinspection ConstantConditions: validated by mCorrectableAxes.contains
                    mOffsetCorrectionProcess.mOffsets.get(axis).updateValue(value);
                }
            }
        }
        return this;
    }

    /**
     * Updates offset settings ranges.
     * <p>
     * <strong>Note:</strong> this will apply the update on correctable axes only.
     *
     * @param offsetRanges the new offset settings ranges
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateOffsetsRanges(@NonNull EnumMap<Axis, DoubleRange> offsetRanges) {
        if (mOffsetCorrectionProcess != null) {
            for (Axis axis : offsetRanges.keySet()) {
                DoubleRange range = offsetRanges.get(axis);
                if (mOffsetCorrectionProcess.mCorrectableAxes.contains(axis) && range != null) {
                    //noinspection ConstantConditions: validated by mCorrectableAxes.contains
                    mOffsetCorrectionProcess.mOffsets.get(axis).updateBounds(range);
                }
            }
        }
        return this;
    }

    /**
     * Updates calibrated state.
     *
     * @param isCalibrated the new calibrated state
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateIsCalibrated(boolean isCalibrated) {
        if (mIsCalibrated != isCalibrated) {
            mIsCalibrated = isCalibrated;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates calibrated process state.
     *
     * @param state the new calibrated process state
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateCalibrationProcessState(@NonNull CalibrationProcessState state) {
        if (mCalibrationProcessState != state) {
            mCalibrationProcessState = state;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates gimbal errors.
     *
     * @param errors current gimbal errors
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore updateErrors(@NonNull Collection<Error> errors) {
        mChanged |= mErrors.retainAll(errors) | mErrors.addAll(errors);
        return this;
    }

    /**
     * Cancels all pending settings rollbacks.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GimbalCore cancelSettingsRollbacks() {
        for (DoubleSettingCore setting : mMaxSpeeds.values()) {
            setting.cancelRollback();
        }
        for (BooleanSettingCore setting : mStabilizedAxes.values()) {
            setting.cancelRollback();
        }
        if (mOffsetCorrectionProcess != null) {
            mOffsetCorrectionProcess.cancelRollbacks();
        }
        return this;
    }

    /**
     * Notified when an user setting changes.
     * <p>
     * In case the change originates from the user modifying the setting value, updates the store to show the setting
     * is updating.
     *
     * @param fromUser {@code true} if the change originates from the user, otherwise {@code false}
     */
    private void onSettingChange(boolean fromUser) {
        mChanged = true;
        if (fromUser) {
            notifyUpdated();
        }
    }

    /** Core class for the offsets correction process. */
    private static final class OffsetCorrectionProcessCore implements OffsetCorrectionProcess {

        /** All correctable axes. */
        @NonNull
        private final EnumSet<Axis> mCorrectableAxes;

        /** Offset setting by axis, in degrees. Non null for all supported axes. */
        @NonNull
        private final EnumMap<Axis, DoubleSettingCore> mOffsets;

        /**
         * Constructor.
         */
        private OffsetCorrectionProcessCore() {
            mCorrectableAxes = EnumSet.noneOf(Axis.class);
            mOffsets = new EnumMap<>(Axis.class);
        }

        @NonNull
        @Override
        public Set<Axis> getCorrectableAxes() {
            return Collections.unmodifiableSet(mCorrectableAxes);
        }

        @NonNull
        @Override
        public DoubleSetting getOffset(@NonNull Axis axis) {
            checkAxisCorrectable(axis);
            //noinspection ConstantConditions: validated by checkAxisCorrectable
            return mOffsets.get(axis);
        }

        /**
         * Checks that the given axis is correctable.
         *
         * @param axis the axis to check
         *
         * @throws IllegalArgumentException in case the specified axis is not correctable
         */
        private void checkAxisCorrectable(@NonNull Axis axis) {
            if (!mCorrectableAxes.contains(axis)) {
                throw new IllegalArgumentException("Axis not correctable: " + axis);
            }
        }

        /**
         * Cancels all pending settings rollbacks.
         */
        void cancelRollbacks() {
            for (DoubleSettingCore setting : mOffsets.values()) {
                setting.cancelRollback();
            }
        }
    }
}
