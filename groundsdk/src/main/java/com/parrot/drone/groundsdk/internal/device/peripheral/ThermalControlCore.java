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

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.ThermalControl;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.value.DoubleRangeCore;
import com.parrot.drone.groundsdk.internal.value.EnumSettingCore;
import com.parrot.drone.groundsdk.internal.value.SettingController;

/** Core class for ThermalControl. */
public final class ThermalControlCore extends SingletonComponentCore implements ThermalControl {

    /** Description of ThermalControl. */
    private static final ComponentDescriptor<Peripheral, ThermalControl> DESC =
            ComponentDescriptor.of(ThermalControl.class);

    /** Engine-specific backend for ThermalControl. */
    public interface Backend {

        /**
         * Sets thermal mode.
         *
         * @param mode thermal mode to set
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setMode(@NonNull Mode mode);

        /**
         * Sets thermal camera sensitivity.
         *
         * @param sensitivity sensitivity to set
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setSensitivity(@NonNull Sensitivity sensitivity);

        /**
         * Sets thermal camera calibration mode.
         *
         * @param mode calibration mode to set
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setCalibrationMode(@NonNull Calibration.Mode mode);

        /**
         * Sends the current emissivity value to the drone.
         *
         * @param value emissivity value to send
         */
        void sendEmissivity(double value);

        /**
         * Sends the background temperature value to the drone.
         *
         * @param value background temperature to send, in Kelvin
         */
        void sendBackgroundTemperature(@FloatRange(from = 0) double value);

        /**
         * Sends the thermal palette configuration to the drone.
         *
         * @param palette palette configuration to send
         */
        void sendPalette(@NonNull Palette palette);

        /**
         * Sends the thermal rendering configuration to the drone.
         *
         * @param rendering rendering configuration to send
         */
        void sendRendering(@NonNull Rendering rendering);

        /**
         * Requests thermal camera calibration.
         *
         * @return {@code true} if the operation could be initiated, otherwise {@code false}
         */
        boolean calibrate();
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Thermal mode setting. */
    @NonNull
    private final EnumSettingCore<Mode> mModeSetting;

    /** Thermal camera sensitivity setting. */
    @NonNull
    private final EnumSettingCore<Sensitivity> mSensitivitySetting;

    /** Thermal camera calibration mode setting. */
    @NonNull
    private final EnumSettingCore<Calibration.Mode> mCalibrationModeSetting;

    /** Thermal camera calibration interface, {@code null} if not supported by the drone. */
    @Nullable
    private CalibrationCore mCalibration;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public ThermalControlCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
        mModeSetting = new EnumSettingCore<>(Mode.class, new SettingController(this::onSettingChange),
                backend::setMode);
        mSensitivitySetting = new EnumSettingCore<>(Sensitivity.HIGH_RANGE,
                new SettingController(this::onSettingChange), backend::setSensitivity);
        mCalibrationModeSetting = new EnumSettingCore<>(Calibration.Mode.class,
                new SettingController(this::onSettingChange), backend::setCalibrationMode);
    }

    @Override
    public void unpublish() {
        super.unpublish();
        mCalibration = null;
    }

    @NonNull
    @Override
    public EnumSettingCore<Mode> mode() {
        return mModeSetting;
    }

    @NonNull
    @Override
    public EnumSettingCore<Sensitivity> sensitivity() {
        return mSensitivitySetting;
    }

    @Nullable
    @Override
    public CalibrationCore calibration() {
        return mCalibration;
    }

    @Override
    public void sendEmissivity(@FloatRange(from = 0, to = 1) double value) {
        mBackend.sendEmissivity(DoubleRangeCore.RATIO.clamp(value));
    }

    @Override
    public void sendBackgroundTemperature(@FloatRange(from = 0) double value) {
        mBackend.sendBackgroundTemperature(value);
    }

    @Override
    public void sendPalette(@NonNull Palette palette) {
        mBackend.sendPalette(palette);
    }

    @Override
    public void sendRendering(@NonNull Rendering rendering) {
        mBackend.sendRendering(rendering);
    }

    /**
     * Cancels all pending settings rollbacks.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public ThermalControlCore cancelSettingsRollbacks() {
        mModeSetting.cancelRollback();
        mSensitivitySetting.cancelRollback();
        mCalibrationModeSetting.cancelRollback();
        return this;
    }

    /**
     * Notified when a user setting changes.
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

    /** Core class for ThermalControl.Calibration. */
    public final class CalibrationCore implements Calibration {

        @NonNull
        @Override
        public EnumSettingCore<Mode> mode() {
            return mCalibrationModeSetting;
        }

        @Override
        public boolean calibrate() {
            return mBackend.calibrate();
        }
    }

    /**
     * Creates thermal camera calibration interface if it doesn't exist yet and returns it.
     *
     * @return thermal camera calibration interface
     */
    @NonNull
    public CalibrationCore createCalibrationIfNeeded() {
        if (mCalibration == null) {
            mCalibration = new CalibrationCore();
        }
        return mCalibration;
    }
}
