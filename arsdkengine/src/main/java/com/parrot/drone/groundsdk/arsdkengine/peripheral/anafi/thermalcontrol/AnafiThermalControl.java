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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.thermalcontrol;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.device.peripheral.ThermalControl;
import com.parrot.drone.groundsdk.internal.device.peripheral.ThermalControlCore;
import com.parrot.drone.groundsdk.internal.value.EnumSettingCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureThermal;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/** ThermalControl peripheral controller for Anafi family drones. */
public class AnafiThermalControl extends DronePeripheralController {

    /** Key used to access preset and range dictionaries for this peripheral controller. */
    private static final String SETTINGS_KEY = "thermal";

    // preset store bindings

    /** Thermal mode preset entry. */
    private static final StorageEntry<ThermalControl.Mode> MODE_PRESET =
            StorageEntry.ofEnum("mode", ThermalControl.Mode.class);

    /** Thermal camera sensitivity preset entry. */
    private static final StorageEntry<ThermalControl.Sensitivity> SENSITIVITY_PRESET =
            StorageEntry.ofEnum("sensitivity", ThermalControl.Sensitivity.class);

    /** Thermal camera calibration mode preset entry. */
    private static final StorageEntry<ThermalControl.Calibration.Mode> CALIBRATION_MODE_PRESET =
            StorageEntry.ofEnum("calibrationMode", ThermalControl.Calibration.Mode.class);

    // device specific store bindings

    /** Supported thermal modes device setting. */
    private static final StorageEntry<EnumSet<ThermalControl.Mode>> SUPPORTED_MODES_SETTING =
            StorageEntry.ofEnumSet("supportedModes", ThermalControl.Mode.class);

    /** Supported thermal camera calibration modes device setting. */
    private static final StorageEntry<EnumSet<ThermalControl.Calibration.Mode>> SUPPORTED_CALIBRATION_MODES_SETTING =
            StorageEntry.ofEnumSet("supportedCalibrationModes", ThermalControl.Calibration.Mode.class);

    /** ThermalControl peripheral for which this object is the backend. */
    @NonNull
    private final ThermalControlCore mThermal;

    /** Dictionary containing device specific values for this peripheral, such as settings ranges, supported status. */
    @Nullable
    private final PersistentStore.Dictionary mDeviceDict;

    /** Dictionary containing current preset values for this peripheral. */
    @Nullable
    private PersistentStore.Dictionary mPresetDict;

    /** Thermal mode. */
    @Nullable
    private ThermalControl.Mode mMode;

    /** Thermal camera sensitivity. */
    @Nullable
    private ThermalControl.Sensitivity mSensitivity;

    /** Thermal camera calibration mode. */
    @Nullable
    private ThermalControl.Calibration.Mode mCalibrationMode;

    /** Emissivity value. */
    @Nullable
    private Float mEmissivity;

    /** Background temperature. */
    @Nullable
    private Float mBackgroundTemperature;

    /** Palette settings. */
    @Nullable
    private PaletteSettings mPaletteSettings;

    /** Sorted list of palette colors. */
    @NonNull
    private List<Color> mColors;

    /**
     * {@code true} if connected drone supports thermal. This means that supported modes, other than only {@link
     * ThermalControl.Mode#DISABLED}, were received during connection.
     */
    private boolean mSupported;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiThermalControl(@NonNull DroneController droneController) {
        super(droneController);
        mThermal = new ThermalControlCore(mComponentStore, mBackend);
        mPresetDict = offlineSettingsEnabled() ? mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY) : null;
        mDeviceDict = offlineSettingsEnabled() ? mDeviceController.getDeviceDict().getDictionary(SETTINGS_KEY) : null;
        mColors = new ArrayList<>();
        loadPersistedData();
        if (isPersisted()) {
            mThermal.publish();
        }
    }

    @Override
    protected final void onConnected() {
        applyPresets();
        if (mSupported) {
            mThermal.publish();
        } else {
            forget();
        }
    }

    @Override
    protected final void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureThermal.UID) {
            ArsdkFeatureThermal.decode(command, mThermalCallback);
        }
    }

    @Override
    protected final void onDisconnected() {
        mThermal.cancelSettingsRollbacks();

        mSupported = false;
        mEmissivity = null;
        mBackgroundTemperature = null;
        mPaletteSettings = null;
        mColors.clear();

        if (isPersisted()) {
            mThermal.notifyUpdated();
        } else {
            mThermal.unpublish();
        }
    }

    @Override
    protected final void onPresetChange() {
        mPresetDict = mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY);
        if (isConnected()) {
            applyPresets();
        }
        mThermal.notifyUpdated();
    }

    @Override
    protected final void onForgetting() {
        forget();
    }

    /**
     * Sends palette colors to the drone.
     *
     * @param colors palette colors to send
     */
    private void sendPaletteColors(@NonNull List<Color> colors) {
        if (colors.isEmpty()) {
            int listFlags = ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.EMPTY);
            sendCommand(ArsdkFeatureThermal.encodeSetPalettePart(0f, 0f, 0f, 0f, listFlags));
        } else {
            int index = 0;
            for (Color color : colors) {
                int listFlags = 0;
                if (index == 0) {
                    listFlags = ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST);
                }
                if (index == colors.size() - 1) {
                    listFlags |= ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST);
                }
                sendCommand(ArsdkFeatureThermal.encodeSetPalettePart(color.mRed, color.mGreen,
                        color.mBlue, color.mPosition, listFlags));
                index++;
            }
        }
    }

    /**
     * Sends palette settings to the drone.
     *
     * @param palette palette to send
     */
    private void sendPaletteSettings(@NonNull ThermalControl.Palette palette) {
        ArsdkFeatureThermal.PaletteMode mode;
        float lowestTemperature = 0f;
        float highestTemperature = 0f;
        ArsdkFeatureThermal.ColorizationMode colorizationMode = ArsdkFeatureThermal.ColorizationMode.EXTENDED;
        ArsdkFeatureThermal.RelativeRangeMode relativeRange = ArsdkFeatureThermal.RelativeRangeMode.UNLOCKED;
        ArsdkFeatureThermal.SpotType spotType = ArsdkFeatureThermal.SpotType.HOT;
        float spotThreshold = 0f;

        if (palette instanceof ThermalControl.AbsolutePalette) {
            mode = ArsdkFeatureThermal.PaletteMode.ABSOLUTE;
            ThermalControl.AbsolutePalette absolutePalette = (ThermalControl.AbsolutePalette) palette;
            lowestTemperature = (float) absolutePalette.getLowestTemperature();
            highestTemperature = (float) absolutePalette.getHighestTemperature();
            switch (absolutePalette.getColorizationMode()) {
                case LIMITED:
                    colorizationMode = ArsdkFeatureThermal.ColorizationMode.LIMITED;
                    break;
                case EXTENDED:
                    colorizationMode = ArsdkFeatureThermal.ColorizationMode.EXTENDED;
                    break;
            }
        } else if (palette instanceof ThermalControl.RelativePalette) {
            mode = ArsdkFeatureThermal.PaletteMode.RELATIVE;
            ThermalControl.RelativePalette relativePalette = (ThermalControl.RelativePalette) palette;
            lowestTemperature = (float) relativePalette.getLowestTemperature();
            highestTemperature = (float) relativePalette.getHighestTemperature();
            relativeRange = relativePalette.isLocked() ? ArsdkFeatureThermal.RelativeRangeMode.LOCKED
                    : ArsdkFeatureThermal.RelativeRangeMode.UNLOCKED;
        } else {
            mode = ArsdkFeatureThermal.PaletteMode.SPOT;
            ThermalControl.SpotPalette spotPalette = (ThermalControl.SpotPalette) palette;
            switch (spotPalette.getType()) {
                case COLD:
                    spotType = ArsdkFeatureThermal.SpotType.COLD;
                    break;
                case HOT:
                    spotType = ArsdkFeatureThermal.SpotType.HOT;
                    break;
            }
            spotThreshold = (float) spotPalette.getThreshold();
        }

        PaletteSettings settings = new PaletteSettings(mode, lowestTemperature, highestTemperature,
                colorizationMode, relativeRange, spotType, spotThreshold);
        if (!settings.equals(mPaletteSettings)) {
            mPaletteSettings = settings;
            sendCommand(ArsdkFeatureThermal.encodeSetPaletteSettings(mode, lowestTemperature, highestTemperature,
                    colorizationMode, relativeRange, spotType, spotThreshold));
        }
    }

    /**
     * Tells whether device specific settings are persisted for this component.
     *
     * @return {@code true} if the component has persisted device settings, otherwise {@code false}
     */
    private boolean isPersisted() {
        return mDeviceDict != null && !mDeviceDict.isNew();
    }

    /**
     * Loads presets and settings from persistent storage and updates the component accordingly.
     */
    private void loadPersistedData() {
        EnumSet<ThermalControl.Mode> supportedModes = SUPPORTED_MODES_SETTING.load(mDeviceDict);
        if (supportedModes != null) {
            mThermal.mode().updateAvailableValues(supportedModes);
        }

        EnumSet<ThermalControl.Calibration.Mode> supportedCalibrationModes =
                SUPPORTED_CALIBRATION_MODES_SETTING.load(mDeviceDict);
        if (supportedCalibrationModes != null) {
            mThermal.createCalibrationIfNeeded()
                    .mode()
                    .updateAvailableValues(supportedCalibrationModes);
        }

        applyPresets();
    }

    /**
     * Applies component's persisted presets.
     */
    private void applyPresets() {
        applyMode(MODE_PRESET.load(mPresetDict));
        applySensitivity(SENSITIVITY_PRESET.load(mPresetDict));
        applyCalibrationMode(CALIBRATION_MODE_PRESET.load(mPresetDict));
    }

    /**
     * Forgets the component.
     */
    private void forget() {
        if (mDeviceDict != null) {
            mDeviceDict.clear().commit();
        }
        mThermal.unpublish();
    }

    /**
     * Applies thermal mode.
     * <ul>
     * <li>Finds an appropriate fallback value if the given value is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param mode value to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyMode(@Nullable ThermalControl.Mode mode) {
        if (mode == null || !mThermal.mode().getAvailableValues().contains(mode)) {
            if (mMode == null) {
                return false;
            }
            mode = mMode;
        }

        boolean updating = mMode != mode
                           && sendCommand(ArsdkFeatureThermal.encodeSetMode(ModeAdapter.from(mode)));

        mMode = mode;
        mThermal.mode().updateValue(mMode);
        return updating;
    }

    /**
     * Applies thermal camera sensitivity.
     * <ul>
     * <li>Finds an appropriate fallback value if the given value is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param sensitivity value to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applySensitivity(@Nullable ThermalControl.Sensitivity sensitivity) {
        if (sensitivity == null || !mThermal.sensitivity().getAvailableValues().contains(sensitivity)) {
            if (mSensitivity == null) {
                return false;
            }
            sensitivity = mSensitivity;
        }

        boolean updating = mSensitivity != sensitivity
                           && sendCommand(ArsdkFeatureThermal.encodeSetSensitivity(
                                   SensitivityAdapter.from(sensitivity)));

        mSensitivity = sensitivity;
        mThermal.sensitivity().updateValue(mSensitivity);
        return updating;
    }


    /**
     * Applies thermal camera calibration mode.
     * <ul>
     * <li>Finds an appropriate fallback value if the given value is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param mode value to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyCalibrationMode(@Nullable ThermalControl.Calibration.Mode mode) {
        ThermalControlCore.CalibrationCore calibration = mThermal.calibration();
        if (calibration == null) {
            return false;
        }

        if (mode == null || !calibration.mode().getAvailableValues().contains(mode)) {
            if (mCalibrationMode == null) {
                return false;
            }
            mode = mCalibrationMode;
        }

        boolean updating = mCalibrationMode != mode
                           && sendCommand(ArsdkFeatureThermal.encodeSetShutterMode(CalibrationModeAdapter.from(mode)));

        mCalibrationMode = mode;
        calibration.mode().updateValue(mCalibrationMode);
        return updating;
    }

    /** Backend of ThermalControlCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final ThermalControlCore.Backend mBackend = new ThermalControlCore.Backend() {

        @Override
        public boolean setMode(@NonNull ThermalControl.Mode mode) {
            boolean updating = applyMode(mode);
            MODE_PRESET.save(mPresetDict, mode);
            if (!updating) {
                mThermal.notifyUpdated();
            }
            return updating;
        }

        @Override
        public boolean setSensitivity(@NonNull ThermalControl.Sensitivity sensitivity) {
            boolean updating = applySensitivity(sensitivity);
            SENSITIVITY_PRESET.save(mPresetDict, sensitivity);
            if (!updating) {
                mThermal.notifyUpdated();
            }
            return updating;
        }

        @Override
        public boolean setCalibrationMode(@NonNull ThermalControl.Calibration.Mode mode) {
            boolean updating = applyCalibrationMode(mode);
            CALIBRATION_MODE_PRESET.save(mPresetDict, mode);
            if (!updating) {
                mThermal.notifyUpdated();
            }
            return updating;
        }

        @Override
        public void sendEmissivity(double value) {
            float emissivity = (float) value;
            if (mEmissivity == null || Float.compare(emissivity, mEmissivity) != 0) {
                mEmissivity = emissivity;
                sendCommand(ArsdkFeatureThermal.encodeSetEmissivity(emissivity));
            }
        }

        @Override
        public void sendBackgroundTemperature(@FloatRange(from = 0) double value) {
            float backgroundTemperature = (float) value;
            if (mBackgroundTemperature == null
                || Float.compare(backgroundTemperature, mBackgroundTemperature) != 0) {
                mBackgroundTemperature = backgroundTemperature;
                sendCommand(ArsdkFeatureThermal.encodeSetBackgroundTemperature(backgroundTemperature));
            }
        }

        @Override
        public void sendPalette(@NonNull ThermalControl.Palette palette) {
            List<Color> colors = Color.from(palette.getColors());
            if (!colors.equals(mColors)) {
                mColors = colors;
                sendPaletteColors(colors);
            }
            sendPaletteSettings(palette);
        }

        @Override
        public void sendRendering(@NonNull ThermalControl.Rendering rendering) {
            ArsdkFeatureThermal.RenderingMode mode = null;
            switch (rendering.getMode()) {
                case VISIBLE:
                    mode = ArsdkFeatureThermal.RenderingMode.VISIBLE;
                    break;
                case THERMAL:
                    mode = ArsdkFeatureThermal.RenderingMode.THERMAL;
                    break;
                case BLENDED:
                    mode = ArsdkFeatureThermal.RenderingMode.BLENDED;
                    break;
                case MONOCHROME:
                    mode = ArsdkFeatureThermal.RenderingMode.MONOCHROME;
                    break;
            }

            sendCommand(ArsdkFeatureThermal.encodeSetRendering(mode, (float) rendering.getBlendingRate()));
        }

        @Override
        public boolean calibrate() {
            return sendCommand(ArsdkFeatureThermal.encodeTriggShutter());
        }
    };

    /** Callbacks called when a command of the feature ArsdkFeatureThermal is decoded. */
    private final ArsdkFeatureThermal.Callback mThermalCallback = new ArsdkFeatureThermal.Callback() {

        /** Palette colors being received from drone. */
        @Nullable
        private List<Color> mPaletteParts;

        @Override
        public void onCapabilities(int modesBitField) {
            EnumSet<ThermalControl.Mode> supportedModes = ModeAdapter.from(modesBitField);
            if (supportedModes.stream().anyMatch(mode -> mode != ThermalControl.Mode.DISABLED)) {
                // other modes than 'DISABLED' exists, peripheral is supported
                SUPPORTED_MODES_SETTING.save(mDeviceDict, supportedModes);
                mThermal.mode().updateAvailableValues(supportedModes);
                mSupported = true;
            }
        }

        @Override
        public void onMode(@Nullable ArsdkFeatureThermal.Mode mode) {
            if (mode == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid thermal mode");
            }

            mMode = ModeAdapter.from(mode);

            if (isConnected()) {
                mThermal.mode().updateValue(mMode);
                mThermal.notifyUpdated();
            }
        }

        @Override
        public void onEmissivity(float emissivity) {
            mEmissivity = emissivity;
        }

        @Override
        public void onBackgroundTemperature(float backgroundTemperature) {
            mBackgroundTemperature = backgroundTemperature;
        }

        @Override
        public void onPalettePart(float red, float green, float blue, float index, int listFlagsBitField) {
            if (ArsdkFeatureGeneric.ListFlags.EMPTY.inBitField(listFlagsBitField)) {
                mColors.clear();
            } else {
                Color color = new Color(red, green, blue, index);
                if (ArsdkFeatureGeneric.ListFlags.REMOVE.inBitField(listFlagsBitField)) {
                    mColors.remove(color);
                } else {
                    if (ArsdkFeatureGeneric.ListFlags.FIRST.inBitField(listFlagsBitField)
                        || mPaletteParts == null) {
                        mPaletteParts = new ArrayList<>();
                    }
                    mPaletteParts.add(color);
                    if (ArsdkFeatureGeneric.ListFlags.LAST.inBitField(listFlagsBitField)) {
                        Collections.sort(mPaletteParts);
                        mColors = mPaletteParts;
                    }
                }
            }
        }

        @Override
        public void onPaletteSettings(@Nullable ArsdkFeatureThermal.PaletteMode mode,
                                      float lowestTemp, float highestTemp,
                                      @Nullable ArsdkFeatureThermal.ColorizationMode outsideColorization,
                                      @Nullable ArsdkFeatureThermal.RelativeRangeMode relativeRange,
                                      @Nullable ArsdkFeatureThermal.SpotType spotType, float spotThreshold) {
            mPaletteSettings = new PaletteSettings(mode, lowestTemp, highestTemp, outsideColorization,
                    relativeRange, spotType, spotThreshold);
        }

        @Override
        public void onSensitivity(@Nullable ArsdkFeatureThermal.Range currentRange) {
            if (currentRange == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid thermal camera sensitivity");
            }

            mSensitivity = SensitivityAdapter.from(currentRange);

            if (isConnected()) {
                mThermal.sensitivity().updateValue(mSensitivity);
                mThermal.notifyUpdated();
            }
        }

        @Override
        public void onShutterMode(@Nullable ArsdkFeatureThermal.ShutterTrigger currentTrigger) {
            if (currentTrigger == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid thermal camera shutter mode");
            }

            // assume all known modes are supported
            EnumSet<ThermalControl.Calibration.Mode> supportedModes =
                    EnumSet.allOf(ThermalControl.Calibration.Mode.class);
            SUPPORTED_CALIBRATION_MODES_SETTING.save(mDeviceDict, supportedModes);

            EnumSettingCore<ThermalControl.Calibration.Mode> setting = mThermal.createCalibrationIfNeeded().mode();

            setting.updateAvailableValues(supportedModes);

            mCalibrationMode = CalibrationModeAdapter.from(currentTrigger);

            if (isConnected()) {
                setting.updateValue(mCalibrationMode);
                mThermal.notifyUpdated();
            }
        }
    };
}
