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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposure;
import com.parrot.drone.groundsdk.internal.value.SettingController;

import java.util.Collection;
import java.util.EnumSet;

/** Core class for CameraExposure.Setting. */
public final class CameraExposureSettingCore extends CameraExposure.Setting {

    /** Setting backend interface, used to delegate value change processing. */
    interface Backend {

        /**
         * Sets exposure settings.
         *
         * @param mode                      exposure mode to set
         * @param manualShutterSpeed        manual shutter speed to set
         * @param manualIsoSensitivity      manual ISO sensitivity to set
         * @param maxIsoSensitivity         maximum ISO sensitivity to set
         * @param autoExposureMeteringMode  auto exposure metering mode to set
         *
         * @return {@code true} to make the setting update to the requested values and switch to the updating state
         *         now, otherwise {@code false}
         */
        boolean setExposure(@NonNull CameraExposure.Mode mode,
                            @NonNull CameraExposure.ShutterSpeed manualShutterSpeed,
                            @NonNull CameraExposure.IsoSensitivity manualIsoSensitivity,
                            @NonNull CameraExposure.IsoSensitivity maxIsoSensitivity,
                            @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode);
    }

    /** Backend that processes value changes from the user. */
    @NonNull
    private final Backend mBackend;

    /** Setting controller, managing updating flag and timeout/rollbacks. */
    @NonNull
    private final SettingController mController;

    /** Supported exposure modes. */
    @NonNull
    private final EnumSet<CameraExposure.Mode> mSupportedModes;

    /** Supported manual shutter speeds. */
    @NonNull
    private final EnumSet<CameraExposure.ShutterSpeed> mSupportedShutterSpeeds;

    /** Supported manual ISO sensitivities. */
    @NonNull
    private final EnumSet<CameraExposure.IsoSensitivity> mSupportedIsoSensitivities;

    /** Supported auto maximum ISO sensitivities. */
    @NonNull
    private final EnumSet<CameraExposure.IsoSensitivity> mMaximumIsoSensitivities;

    /** Supported auto exposure metering modes. */
    @NonNull
    private final EnumSet<CameraExposure.AutoExposureMeteringMode> mSupportedAutoExposureMeteringModes;

    /** Current exposure mode. */
    @NonNull
    private CameraExposure.Mode mMode;

    /** Current manual shutter speed. */
    @NonNull
    private CameraExposure.ShutterSpeed mShutterSpeed;

    /** Current manual iso sensitivity. */
    @NonNull
    private CameraExposure.IsoSensitivity mIsoSensitivity;

    /** Current auto maximum iso sensitivity. */
    @NonNull
    private CameraExposure.IsoSensitivity mMaxIsoSensitivity;

    /** Current auto exposure metering mode. */
    @NonNull
    private CameraExposure.AutoExposureMeteringMode mAutoExposureMeteringMode;

    /** Default mode. */
    private static final CameraExposure.Mode DEFAULT_MODE = CameraExposure.Mode.AUTOMATIC;

    /** Default manual shutter speed. */
    private static final CameraExposure.ShutterSpeed DEFAULT_SHUTTER_SPEED = CameraExposure.ShutterSpeed.ONE;

    /** Default manual ISO sensitivity. */
    private static final CameraExposure.IsoSensitivity DEFAULT_ISO_SENSITIVITY = CameraExposure.IsoSensitivity.ISO_50;

    /** Default maximum automatic ISO sensitivity. */
    private static final CameraExposure.IsoSensitivity DEFAULT_MAX_ISO_SENSITIVITY =
            CameraExposure.IsoSensitivity.ISO_3200;

    /** Default auto exposure metering mode. */
    private static final CameraExposure.AutoExposureMeteringMode DEFAULT_AUTO_EXPOSURE_METERING_MODE
            = CameraExposure.AutoExposureMeteringMode.STANDARD;

    /**
     * Constructor.
     *
     * @param listener setting change listener
     * @param backend  backend that will process value changes
     */
    CameraExposureSettingCore(@NonNull SettingController.ChangeListener listener, @NonNull Backend backend) {
        mBackend = backend;
        mController = new SettingController(listener);
        mSupportedModes = EnumSet.noneOf(CameraExposure.Mode.class);
        mSupportedShutterSpeeds = EnumSet.noneOf(CameraExposure.ShutterSpeed.class);
        mSupportedIsoSensitivities = EnumSet.noneOf(CameraExposure.IsoSensitivity.class);
        mMaximumIsoSensitivities = EnumSet.noneOf(CameraExposure.IsoSensitivity.class);
        mSupportedAutoExposureMeteringModes = EnumSet.noneOf(CameraExposure.AutoExposureMeteringMode.class);
        mMode = DEFAULT_MODE;
        mShutterSpeed = DEFAULT_SHUTTER_SPEED;
        mIsoSensitivity = DEFAULT_ISO_SENSITIVITY;
        mMaxIsoSensitivity = DEFAULT_MAX_ISO_SENSITIVITY;
        mAutoExposureMeteringMode = DEFAULT_AUTO_EXPOSURE_METERING_MODE;
    }

    @Override
    public boolean isUpdating() {
        return mController.hasPendingRollback();
    }

    @NonNull
    @Override
    public EnumSet<CameraExposure.Mode> supportedModes() {
        return EnumSet.copyOf(mSupportedModes);
    }

    @NonNull
    @Override
    public CameraExposure.Mode mode() {
        return mMode;
    }

    @NonNull
    @Override
    public CameraExposure.Setting setMode(@NonNull CameraExposure.Mode mode) {
        if (mMode != mode && mSupportedModes.contains(mode)) {
            sendSettings(mode, null, null, null, null);
        }
        return this;
    }

    @NonNull
    @Override
    public EnumSet<CameraExposure.ShutterSpeed> supportedManualShutterSpeeds() {
        return EnumSet.copyOf(mSupportedShutterSpeeds);
    }

    @NonNull
    @Override
    public CameraExposure.ShutterSpeed manualShutterSpeed() {
        return mShutterSpeed;
    }

    @NonNull
    @Override
    public CameraExposure.Setting setManualShutterSpeed(@NonNull CameraExposure.ShutterSpeed shutterSpeed) {
        if (mShutterSpeed != shutterSpeed && mSupportedShutterSpeeds.contains(shutterSpeed)) {
            sendSettings(null, shutterSpeed, null, null, null);
        }
        return this;
    }

    @NonNull
    @Override
    public EnumSet<CameraExposure.IsoSensitivity> supportedManualIsoSensitivities() {
        return EnumSet.copyOf(mSupportedIsoSensitivities);
    }

    @NonNull
    @Override
    public CameraExposure.IsoSensitivity manualIsoSensitivity() {
        return mIsoSensitivity;
    }

    @NonNull
    @Override
    public CameraExposure.Setting setManualIsoSensitivity(@NonNull CameraExposure.IsoSensitivity isoSensitivity) {
        if (mIsoSensitivity != isoSensitivity && mSupportedIsoSensitivities.contains(isoSensitivity)) {
            sendSettings(null, null, isoSensitivity, null, null);
        }
        return this;
    }

    @NonNull
    @Override
    public EnumSet<CameraExposure.IsoSensitivity> supportedMaximumIsoSensitivities() {
        return EnumSet.copyOf(mMaximumIsoSensitivities);
    }

    @NonNull
    @Override
    public CameraExposure.IsoSensitivity maxIsoSensitivity() {
        return mMaxIsoSensitivity;
    }

    @NonNull
    @Override
    public CameraExposure.Setting setMaxIsoSensitivity(@NonNull CameraExposure.IsoSensitivity maxIsoSensitivity) {
        if (mMaxIsoSensitivity != maxIsoSensitivity && mMaximumIsoSensitivities.contains(maxIsoSensitivity)) {
            sendSettings(null, null, null, maxIsoSensitivity, null);
        }
        return this;
    }

    @NonNull
    @Override
    public EnumSet<CameraExposure.AutoExposureMeteringMode> supportedAutoExposureMeteringModes() {
        return EnumSet.copyOf(mSupportedAutoExposureMeteringModes);
    }

    @NonNull
    @Override
    public CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode() {
        return mAutoExposureMeteringMode;
    }

    @NonNull
    @Override
    public CameraExposure.Setting setAutoExposureMeteringMode(
            @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
        if (mAutoExposureMeteringMode != autoExposureMeteringMode
            && mSupportedAutoExposureMeteringModes.contains(autoExposureMeteringMode)) {
            sendSettings(null, null, null, null, autoExposureMeteringMode);
        }
        return this;
    }

    @Override
    public void setAutoMode(@NonNull CameraExposure.IsoSensitivity maxIsoSensitivity) {
        if ((mMode != CameraExposure.Mode.AUTOMATIC || mMaxIsoSensitivity != maxIsoSensitivity)
            && mSupportedModes.contains(CameraExposure.Mode.AUTOMATIC)
            && mMaximumIsoSensitivities.contains(maxIsoSensitivity)) {
            sendSettings(CameraExposure.Mode.AUTOMATIC, null, null, maxIsoSensitivity, null);
        }
    }

    @Override
    public void setAutoMode(@NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
        if ((mMode != CameraExposure.Mode.AUTOMATIC || mAutoExposureMeteringMode != autoExposureMeteringMode)
            && mSupportedModes.contains(CameraExposure.Mode.AUTOMATIC)
            && mSupportedAutoExposureMeteringModes.contains(autoExposureMeteringMode)) {
            sendSettings(CameraExposure.Mode.AUTOMATIC, null, null, null, autoExposureMeteringMode);
        }
    }

    @Override
    public void setAutoMode(@NonNull CameraExposure.IsoSensitivity maxIsoSensitivity,
                            @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
        if((mMode != CameraExposure.Mode.AUTOMATIC || mMaxIsoSensitivity != maxIsoSensitivity
            || mAutoExposureMeteringMode != autoExposureMeteringMode)
           && mSupportedModes.contains(CameraExposure.Mode.AUTOMATIC)
           && mMaximumIsoSensitivities.contains(maxIsoSensitivity)
           && mSupportedAutoExposureMeteringModes.contains(autoExposureMeteringMode)) {
            sendSettings(CameraExposure.Mode.AUTOMATIC, null, null, maxIsoSensitivity, autoExposureMeteringMode);
        }
    }

    @Override
    public void setAutoPreferShutterSpeedMode(@NonNull CameraExposure.IsoSensitivity maxIsoSensitivity) {
        if ((mMode != CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED || mMaxIsoSensitivity != maxIsoSensitivity)
            && mSupportedModes.contains(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED)
            && mMaximumIsoSensitivities.contains(maxIsoSensitivity)) {
            sendSettings(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED, null, null, maxIsoSensitivity,
                    mAutoExposureMeteringMode);
        }
    }

    @Override
    public void setAutoPreferShutterSpeedMode(
            @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
        if ((mMode != CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED
             || mAutoExposureMeteringMode != autoExposureMeteringMode)
            && mSupportedModes.contains(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED)
            && mSupportedAutoExposureMeteringModes.contains(autoExposureMeteringMode)) {
            sendSettings(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED, null, null, null, autoExposureMeteringMode);
        }
    }

    @Override
    public void setAutoPreferShutterSpeedMode(
            @NonNull CameraExposure.IsoSensitivity maxIsoSensitivity,
            @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
        if ((mMode != CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED || mMaxIsoSensitivity != maxIsoSensitivity
             || mAutoExposureMeteringMode != autoExposureMeteringMode)
            && mSupportedModes.contains(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED)
            && mMaximumIsoSensitivities.contains(maxIsoSensitivity)
            && mSupportedAutoExposureMeteringModes.contains(autoExposureMeteringMode)) {
            sendSettings(CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED, null, null, maxIsoSensitivity,
                    autoExposureMeteringMode);
        }
    }

    @Override
    public void setAutoPreferIsoSensitivityMode(@NonNull CameraExposure.IsoSensitivity maxIsoSensitivity) {
        if ((mMode != CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY || mMaxIsoSensitivity != maxIsoSensitivity)
            && mSupportedModes.contains(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY)
            && mMaximumIsoSensitivities.contains(maxIsoSensitivity)) {
            sendSettings(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY, null, null, maxIsoSensitivity, null);
        }
    }

    @Override
    public void setAutoPreferIsoSensitivityMode(
            @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
        if ((mMode != CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY
             || mAutoExposureMeteringMode != autoExposureMeteringMode)
            && mSupportedModes.contains(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY)
            && mSupportedAutoExposureMeteringModes.contains(autoExposureMeteringMode)) {
            sendSettings(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY, null, null, null,
                    autoExposureMeteringMode);
        }
    }

    @Override
    public void setAutoPreferIsoSensitivityMode(
            @NonNull CameraExposure.IsoSensitivity maxIsoSensitivity,
            @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
        if((mMode != CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY || mMaxIsoSensitivity != maxIsoSensitivity
            || mAutoExposureMeteringMode != autoExposureMeteringMode)
        && mSupportedModes.contains(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY)
        && mMaximumIsoSensitivities.contains(maxIsoSensitivity)
        && mSupportedAutoExposureMeteringModes.contains(autoExposureMeteringMode)) {
            sendSettings(CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY, null, null, maxIsoSensitivity,
                    autoExposureMeteringMode);
        }
    }

    @Override
    public void setManualMode(@NonNull CameraExposure.ShutterSpeed shutterSpeed) {
        if ((mMode != CameraExposure.Mode.MANUAL_SHUTTER_SPEED || mShutterSpeed != shutterSpeed)
            && mSupportedModes.contains(CameraExposure.Mode.MANUAL_SHUTTER_SPEED)
            && mSupportedShutterSpeeds.contains(shutterSpeed)) {
            sendSettings(CameraExposure.Mode.MANUAL_SHUTTER_SPEED, shutterSpeed, null, null, null);
        }
    }

    @Override
    public void setManualMode(@NonNull CameraExposure.IsoSensitivity isoSensitivity) {
        if ((mMode != CameraExposure.Mode.MANUAL_ISO_SENSITIVITY || mIsoSensitivity != isoSensitivity)
            && mSupportedModes.contains(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY)
            && mSupportedIsoSensitivities.contains(isoSensitivity)) {
            sendSettings(CameraExposure.Mode.MANUAL_ISO_SENSITIVITY, null, isoSensitivity, null, null);
        }
    }

    @Override
    public void setManualMode(@NonNull CameraExposure.ShutterSpeed shutterSpeed,
                              @NonNull CameraExposure.IsoSensitivity isoSensitivity) {
        if ((mMode != CameraExposure.Mode.MANUAL || mIsoSensitivity != isoSensitivity || mShutterSpeed != shutterSpeed)
            && mSupportedModes.contains(CameraExposure.Mode.MANUAL)
            && mSupportedShutterSpeeds.contains(shutterSpeed)
            && mSupportedIsoSensitivities.contains(isoSensitivity)) {
            sendSettings(CameraExposure.Mode.MANUAL, shutterSpeed, isoSensitivity, null, null);
        }
    }

    /**
     * Updates supported exposure modes.
     *
     * @param modes supported exposure modes
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraExposureSettingCore updateSupportedModes(@NonNull Collection<CameraExposure.Mode> modes) {
        if (mSupportedModes.retainAll(modes) | mSupportedModes.addAll(modes)) {
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates supported manual shutter speeds.
     *
     * @param speeds supported manual shutter speeds
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraExposureSettingCore updateSupportedShutterSpeeds(
            @NonNull Collection<CameraExposure.ShutterSpeed> speeds) {
        if (mSupportedShutterSpeeds.retainAll(speeds) | mSupportedShutterSpeeds.addAll(speeds)) {
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates supported manual ISO sensitivities.
     *
     * @param isos supported manual ISO sensitivities
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraExposureSettingCore updateSupportedIsoSensitivities(
            @NonNull Collection<CameraExposure.IsoSensitivity> isos) {
        if (mSupportedIsoSensitivities.retainAll(isos) | mSupportedIsoSensitivities.addAll(isos)) {
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates supported auto maximum ISO sensitivities.
     *
     * @param isos supported auto maximum ISO sensitivities
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraExposureSettingCore updateMaximumIsoSensitivities(
            @NonNull Collection<CameraExposure.IsoSensitivity> isos) {
        if (mMaximumIsoSensitivities.retainAll(isos) | mMaximumIsoSensitivities.addAll(isos)) {
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates supported auto exposure metering modes.
     *
     * @param autoExposureMeteringModes supported auto exposure metering modes
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraExposureSettingCore updateSupportedAutoExposureMeteringModes(
            @NonNull Collection<CameraExposure.AutoExposureMeteringMode> autoExposureMeteringModes) {
        if (mSupportedAutoExposureMeteringModes.retainAll(autoExposureMeteringModes)
            | mSupportedAutoExposureMeteringModes.addAll(autoExposureMeteringModes)) {
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current exposure mode.
     *
     * @param mode exposure mode
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraExposureSettingCore updateMode(@NonNull CameraExposure.Mode mode) {
        if (mController.cancelRollback() || mMode != mode) {
            mMode = mode;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current manual shutter speed.
     *
     * @param shutterSpeed manual shutter speed
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraExposureSettingCore updateShutterSpeed(@NonNull CameraExposure.ShutterSpeed shutterSpeed) {
        if (mController.cancelRollback() || mShutterSpeed != shutterSpeed) {
            mShutterSpeed = shutterSpeed;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current manual ISO sensitivity.
     *
     * @param isoSensitivity manual ISO sensitivity
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraExposureSettingCore updateIsoSensitivity(@NonNull CameraExposure.IsoSensitivity isoSensitivity) {
        if (mController.cancelRollback() || mIsoSensitivity != isoSensitivity) {
            mIsoSensitivity = isoSensitivity;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current auto maximum ISO sensitivity.
     *
     * @param maxIsoSensitivity auto maximum ISO sensitivity
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraExposureSettingCore updateMaxIsoSensitivity(
            @NonNull CameraExposure.IsoSensitivity maxIsoSensitivity) {
        if (mController.cancelRollback() || mMaxIsoSensitivity != maxIsoSensitivity) {
            mMaxIsoSensitivity = maxIsoSensitivity;
            mController.notifyChange(false);
        }
        return this;
    }

    /**
     * Updates current auto exposure metering mode.
     *
     * @param autoExposureMeteringMode auto exposure metering mode
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public CameraExposureSettingCore updateAutoExposureMeteringMode(
            @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
        if (mController.cancelRollback() || mAutoExposureMeteringMode != autoExposureMeteringMode) {
            mAutoExposureMeteringMode = autoExposureMeteringMode;
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
     * Sends exposure settings to backend.
     *
     * @param mode                      exposure mode to set, {@code null} to use {@link #mMode current mode}
     * @param shutterSpeed              manual shutter speed to set, {@code null} to use
     *                                  {@link #mShutterSpeed current manual shutter speed}
     * @param isoSensitivity            manual ISO sensitivity to set, {@code null} to use
     *                                  {@link #mIsoSensitivity current manual ISO sensitivity}
     * @param maxIsoSensitivity         maximum ISO sensitivity to set, {@code null} to use
     *                                  {@link #mMaxIsoSensitivity current maximum ISO sensitivity}
     * @param autoExposureMeteringMode  auto exposure metering mode to set, {@code null} to use
     *                                  {@link #mAutoExposureMeteringMode current auto exposure metering mode}
     */
    private void sendSettings(@Nullable CameraExposure.Mode mode,
                              @Nullable CameraExposure.ShutterSpeed shutterSpeed,
                              @Nullable CameraExposure.IsoSensitivity isoSensitivity,
                              @Nullable CameraExposure.IsoSensitivity maxIsoSensitivity,
                              @Nullable CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
        CameraExposure.Mode rollbackMode = mMode;
        CameraExposure.AutoExposureMeteringMode rollbackAutoExposureMeteringMode = mAutoExposureMeteringMode;
        CameraExposure.ShutterSpeed rollbackShutterSpeed = mShutterSpeed;
        CameraExposure.IsoSensitivity rollbackIsoSensitivity = mIsoSensitivity;
        CameraExposure.IsoSensitivity rollbackMaxIsoSensitivity = mMaxIsoSensitivity;
        if (mBackend.setExposure(mode == null ? mMode : mode,
                shutterSpeed == null ? mShutterSpeed : shutterSpeed,
                isoSensitivity == null ? mIsoSensitivity : isoSensitivity,
                maxIsoSensitivity == null ? mMaxIsoSensitivity : maxIsoSensitivity,
                autoExposureMeteringMode == null ? mAutoExposureMeteringMode : autoExposureMeteringMode)) {
            if (mode != null) {
                mMode = mode;
            }
            if (shutterSpeed != null) {
                mShutterSpeed = shutterSpeed;
            }
            if (isoSensitivity != null) {
                mIsoSensitivity = isoSensitivity;
            }
            if (maxIsoSensitivity != null) {
                mMaxIsoSensitivity = maxIsoSensitivity;
            }
            if (autoExposureMeteringMode != null) {
                mAutoExposureMeteringMode = autoExposureMeteringMode;
            }
            mController.postRollback(() -> {
                mMode = rollbackMode;
                mShutterSpeed = rollbackShutterSpeed;
                mIsoSensitivity = rollbackIsoSensitivity;
                mMaxIsoSensitivity = rollbackMaxIsoSensitivity;
                mAutoExposureMeteringMode = rollbackAutoExposureMeteringMode;
            });
        }
    }
}
