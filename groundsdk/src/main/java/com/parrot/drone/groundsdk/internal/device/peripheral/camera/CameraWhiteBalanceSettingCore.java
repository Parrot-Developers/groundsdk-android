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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalance;
import com.parrot.drone.groundsdk.internal.value.SettingController;

import java.util.Collection;
import java.util.EnumSet;

/** Core class for CameraWhiteBalance.Setting. */
public class CameraWhiteBalanceSettingCore extends CameraWhiteBalance.Setting {

    /** Setting backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets white balance settings.
         *
         * @param mode        white balance mode to set
         * @param temperature custom white balance temperature to set
         *
         * @return {@code true} to make the setting update to the requested values and switch to the updating state
         *         now, otherwise {@code false}
         */
        boolean setWhiteBalance(@NonNull CameraWhiteBalance.Mode mode,
                                @NonNull CameraWhiteBalance.Temperature temperature);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Supported white balance modes. */
    @NonNull
    private final EnumSet<CameraWhiteBalance.Mode> mSupportedModes;

    /** Supported custom white balance temperatures. */
    @NonNull
    private final EnumSet<CameraWhiteBalance.Temperature> mSupportedTemperatures;

    /** Current white balance mode. */
    @NonNull
    private CameraWhiteBalance.Mode mMode;

    /** Current custom white balance temperature. */
    @NonNull
    private CameraWhiteBalance.Temperature mTemperature;

    /** Default mode. */
    private static final CameraWhiteBalance.Mode DEFAULT_MODE = CameraWhiteBalance.Mode.AUTOMATIC;

    /** Default custom temperature. */
    private static final CameraWhiteBalance.Temperature DEFAULT_TEMPERATURE = CameraWhiteBalance.Temperature.K_1500;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    CameraWhiteBalanceSettingCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
        mSupportedModes = EnumSet.noneOf(CameraWhiteBalance.Mode.class);
        mSupportedTemperatures = EnumSet.noneOf(CameraWhiteBalance.Temperature.class);
        mMode = DEFAULT_MODE;
        mTemperature = DEFAULT_TEMPERATURE;
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @NonNull
    @Override
    public EnumSet<CameraWhiteBalance.Mode> supportedModes() {
        return EnumSet.copyOf(mSupportedModes);
    }

    @NonNull
    @Override
    public CameraWhiteBalance.Mode mode() {
        return mMode;
    }

    @NonNull
    @Override
    public CameraWhiteBalance.Setting setMode(@NonNull CameraWhiteBalance.Mode mode) {
        if (mMode != mode && mSupportedModes.contains(mode)) {
            sendSettings(mode, null);
        }
        return this;
    }

    @NonNull
    @Override
    public EnumSet<CameraWhiteBalance.Temperature> supportedCustomTemperatures() {
        return EnumSet.copyOf(mSupportedTemperatures);
    }

    @NonNull
    @Override
    public CameraWhiteBalance.Temperature customTemperature() {
        return mTemperature;
    }

    @NonNull
    @Override
    public CameraWhiteBalance.Setting setCustomTemperature(@NonNull CameraWhiteBalance.Temperature temperature) {
        if (mTemperature != temperature && mSupportedTemperatures.contains(temperature)) {
            sendSettings(null, temperature);
        }
        return this;
    }

    @Override
    public void setCustomMode(@NonNull CameraWhiteBalance.Temperature temperature) {
        if ((mMode != CameraWhiteBalance.Mode.CUSTOM || mTemperature != temperature)
            && mSupportedModes.contains(CameraWhiteBalance.Mode.CUSTOM)
            && mSupportedTemperatures.contains(temperature)) {
            sendSettings(CameraWhiteBalance.Mode.CUSTOM, temperature);
        }
    }

    /**
     * Updates supported white balance modes.
     *
     * @param modes supported white balance modes
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraWhiteBalanceSettingCore updateSupportedModes(@NonNull Collection<CameraWhiteBalance.Mode> modes) {
        if (mSupportedModes.retainAll(modes) | mSupportedModes.addAll(modes)) {
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates supported custom white balance temperatures.
     *
     * @param temperatures supported custom white balance temperatures
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraWhiteBalanceSettingCore updateSupportedTemperatures(
            @NonNull Collection<CameraWhiteBalance.Temperature> temperatures) {
        if (mSupportedTemperatures.retainAll(temperatures) | mSupportedTemperatures.addAll(temperatures)) {
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current white balance mode.
     *
     * @param mode white balance mode
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraWhiteBalanceSettingCore updateMode(@NonNull CameraWhiteBalance.Mode mode) {
        if (mController.cancelRollback() || mMode != mode) {
            mMode = mode;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current custom white balance temperature.
     *
     * @param temperature custom white balance temperature
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraWhiteBalanceSettingCore updateTemperature(@NonNull CameraWhiteBalance.Temperature temperature) {
        if (mController.cancelRollback() || mTemperature != temperature) {
            mTemperature = temperature;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Cancels any pending rollback.
     */
    void cancelRollback() {
        if (mController.cancelRollback()) {
            mController.notifyChange(false);
        }
    }

    /**
     * Sends white balance settings to backend.
     *
     * @param mode        white balance mode to set, {@code null} to use {@link #mMode current mode}
     * @param temperature custom temperature to set, {@code null} to use
     *                    {@link #mTemperature current custom temperature}
     */
    private void sendSettings(@Nullable CameraWhiteBalance.Mode mode,
                              @Nullable CameraWhiteBalance.Temperature temperature) {
        CameraWhiteBalance.Mode rollbackMode = mMode;
        CameraWhiteBalance.Temperature rollbackTemperature = mTemperature;
        if (mBackend.setWhiteBalance(mode == null ? mMode : mode, temperature == null ? mTemperature : temperature)) {
            if (mode != null) {
                mMode = mode;
            }
            if (temperature != null) {
                mTemperature = temperature;
            }
            mController.postRollback(() -> {
                mMode = rollbackMode;
                mTemperature = rollbackTemperature;
            });
        }
    }
}
