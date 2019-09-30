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

package com.parrot.drone.groundsdk.internal.device.peripheral.camera;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraZoom;
import com.parrot.drone.groundsdk.internal.value.BooleanSettingCore;
import com.parrot.drone.groundsdk.internal.value.DoubleRangeCore;
import com.parrot.drone.groundsdk.internal.value.DoubleSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;

/** Core class for CameraZoom. */
public class CameraZoomCore implements CameraZoom {

    /** Zoom backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets maximum zoom velocity.
         *
         * @param speed maximum zoom speed to set
         *
         * @return {@code true} if the setting could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setMaxZoomSpeed(double speed);

        /**
         * Sets the quality degradation allowance during zoom change with velocity.
         *
         * @param allowed {@code true} to allow quality degradation, otherwise {@code false}
         *
         * @return {@code true} if the setting could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setQualityDegradationAllowance(boolean allowed);

        /**
         * Controls the zoom.
         * <p>
         * Unit of the `target` depends on the value of the `mode` parameter:
         * <ul>
         * <li>LEVEL: target is in zoom level. 1 means no zoom.</li>
         * <li>VELOCITY: value is in signed ratio (from -1 to 1) of {@link #maxSpeed() maxSpeed setting } value.
         * Negative values will produce a zoom out, positive value will zoom in.</li>
         * </ul>
         *
         * @param mode   the mode that should be used to control the zoom.
         * @param target either level or velocity zoom target, clamped in the correct range.
         */
        void control(@NonNull ControlMode mode, double target);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Settings change listener. */
    @NonNull
    private final SettingController.ChangeListener mListener;

    /** Maximum zoom velocity setting. */
    @NonNull
    private final DoubleSettingCore mMaxVelocity;

    /** Zoom velocity quality degradation allowance setting. */
    @NonNull
    private final BooleanSettingCore mQualityDegradationAllowance;

    /** Zoom availability. */
    private boolean mAvailable;

    /** Current zoom level. */
    private double mCurrentLevel;

    /** Maximum lossy zoom level. */
    private double mMaxLossyLevel;

    /** Maximum loss less zoom level. */
    private double mMaxLossLessLevel;

    /** Zoom level range. */
    @NonNull
    private final DoubleRangeCore mLevelRange;

    /** Default level. */
    private static final double DEFAULT_LEVEL = 1;

    /**
     * Constructor.
     *
     * @param listener settings change listener
     * @param backend  backend that will process value changes
     */
    CameraZoomCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mListener = listener;
        mMaxVelocity = new DoubleSettingCore(new SettingController(listener), mBackend::setMaxZoomSpeed);
        mQualityDegradationAllowance = new BooleanSettingCore(new SettingController(listener),
                mBackend::setQualityDegradationAllowance);
        mCurrentLevel = DEFAULT_LEVEL;
        mMaxLossyLevel = DEFAULT_LEVEL;
        mMaxLossLessLevel = DEFAULT_LEVEL;
        mLevelRange = new DoubleRangeCore(DEFAULT_LEVEL, DEFAULT_LEVEL);
    }

    @NonNull
    @Override
    public DoubleSettingCore maxSpeed() {
        return mMaxVelocity;
    }

    @NonNull
    @Override
    public BooleanSettingCore velocityQualityDegradationAllowance() {
        return mQualityDegradationAllowance;
    }

    @Override
    public double getCurrentLevel() {
        return mCurrentLevel;
    }

    @Override
    public double getMaxLossyLevel() {
        return mMaxLossyLevel;
    }

    @Override
    public double getMaxLossLessLevel() {
        return mMaxLossLessLevel;
    }

    @Override
    public boolean isAvailable() {
        return mAvailable;
    }

    @Override
    public void control(@NonNull ControlMode mode, double target) {
        double clampedTarget = 0;
        switch (mode) {
            case LEVEL:
                clampedTarget = mLevelRange.clamp(target);
                break;
            case VELOCITY:
                clampedTarget = DoubleRangeCore.SIGNED_RATIO.clamp(target);
        }
        mBackend.control(mode, clampedTarget);
    }

    /**
     * Updates the zoom availability.
     *
     * @param available new availability
     *
     * @return this, to allow call chaining
     */
    public final CameraZoomCore updateAvailability(boolean available) {
        if (mAvailable != available) {
            mAvailable = available;
            mListener.onChange(false);
        }
        return this;
    }

    /**
     * Updates the current zoom level.
     *
     * @param level new zoom level
     *
     * @return this, to allow call chaining
     */
    public final CameraZoomCore updateCurrentLevel(double level) {
        if (Double.compare(mCurrentLevel, level) != 0) {
            mCurrentLevel = level;
            mListener.onChange(false);
        }
        return this;
    }

    /**
     * Updates the maximum lossy zoom level.
     *
     * @param level new maximum lossy zoom level
     *
     * @return this, to allow call chaining
     */
    public final CameraZoomCore updateMaxLossyLevel(@FloatRange(from = 1) double level) {
        if (Double.compare(mMaxLossyLevel, level) != 0) {
            mMaxLossyLevel = level;
            mLevelRange.updateBounds(1, level);
            mListener.onChange(false);
        }
        return this;
    }

    /**
     * Updates the maximum loss less zoom level.
     *
     * @param level new maximum loss less zoom level
     *
     * @return this, to allow call chaining
     */
    public final CameraZoomCore updateMaxLossLessLevel(double level) {
        if (Double.compare(mMaxLossLessLevel, level) != 0) {
            mMaxLossLessLevel = level;
            mListener.onChange(false);
        }
        return this;
    }

    /**
     * Resets setting values to defaults.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public final CameraZoomCore reset() {
        mAvailable = false;
        mCurrentLevel = DEFAULT_LEVEL;
        mMaxLossyLevel = DEFAULT_LEVEL;
        mMaxLossLessLevel = DEFAULT_LEVEL;
        mListener.onChange(false);
        return this;
    }

    /**
     * Cancels any pending rollback.
     */
    void cancelRollback() {
        mMaxVelocity.cancelRollback();
        mQualityDegradationAllowance.cancelRollback();
    }
}
