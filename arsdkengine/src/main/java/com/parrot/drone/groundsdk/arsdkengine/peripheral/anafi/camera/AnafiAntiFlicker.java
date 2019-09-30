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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.camera;

import android.location.Address;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.device.peripheral.AntiFlicker;
import com.parrot.drone.groundsdk.internal.device.peripheral.AntiFlickerCore;
import com.parrot.drone.groundsdk.internal.utility.ReverseGeocoderUtility;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** AntiFlicker peripheral controller for Anafi family drones. */
public final class AnafiAntiFlicker extends DronePeripheralController {

    /** Key used to access preset and range dictionaries for this peripheral controller. */
    private static final String SETTINGS_KEY = "antiFlicker";

    // preset store bindings

    /** Anti-flickering mode preset entry. */
    private static final StorageEntry<AntiFlicker.Mode> MODE_PRESET =
            StorageEntry.ofEnum("mode", AntiFlicker.Mode.class);

    // device specific store bindings

    /** Supported anti-flickering modes device setting. */
    private static final StorageEntry<EnumSet<AntiFlicker.Mode>> SUPPORTED_MODES_SETTING =
            StorageEntry.ofEnumSet("supportedModes", AntiFlicker.Mode.class);

    /** Countries with 60 Hz electrical network. Assume that all other countries have 50 Hz electrical network. */
    private static final Set<String> COUNTRIES_60HZ = new HashSet<>(
            Arrays.asList("DO", "BM", "HT", "KN", "HN", "BR", "BS", "FM", "BZ", "PR", "NI", "PW", "TW", "TT", "PA",
                    "PF", "PE", "LR", "PH", "GU", "GT", "CO", "VE", "AG", "VG", "AI", "VI", "CA", "GY", "AS", "EC",
                    "AW", "CR", "SA", "CU", "MF", "SR", "SV", "US", "KR", "KP", "MS", "KY", "MX"));

    /** AntiFlicker peripheral for which this object is the backend. */
    @NonNull
    private final AntiFlickerCore mAntiFlicker;

    /** Dictionary containing device specific values for this peripheral, such as settings ranges, supported status. */
    @Nullable
    private final PersistentStore.Dictionary mDeviceDict;

    /** Dictionary containing current preset values for this peripheral. */
    @Nullable
    private PersistentStore.Dictionary mPresetDict;

    /** Anti-flickering mode. */
    @Nullable
    private AntiFlicker.Mode mMode;

    /** Reverse geocoder for location based auto mode. */
    @Nullable
    private final ReverseGeocoderUtility mReverseGeocoder;

    /** Reverse geocoder monitor. */
    @Nullable
    private ReverseGeocoderUtility.Monitor mReverseGeocoderMonitor;

    /** Country electric frequency. */
    protected enum CountryFrequency {

        /** 50 Hz electric frequency. */
        HZ_50,

        /** 60 Hz electric frequency. */
        HZ_60,
    }

    /** Latest geo-localized country frequency. */
    @Nullable
    private CountryFrequency mLocationFreq;

    /** {@code true} if drone does not support a _real_ auto mode, but is emulated by geo-monitoring frequency. */
    private boolean mAutoModeEmulation;

    /** Latest anti-flicker mode received from drone. */
    @Nullable
    private ArsdkFeatureCamera.AntiflickerMode mReceivedMode;

    /** Stores actual mode sent to drone when emulating {@link AntiFlicker.Mode#AUTO} mode. */
    @Nullable
    private ArsdkFeatureCamera.AntiflickerMode mSentEmulationMode;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiAntiFlicker(@NonNull DroneController droneController) {
        super(droneController);
        mAntiFlicker = new AntiFlickerCore(mComponentStore, mBackend);
        mPresetDict = offlineSettingsEnabled() ? mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY) : null;
        mDeviceDict = offlineSettingsEnabled() ? mDeviceController.getDeviceDict().getDictionary(SETTINGS_KEY) : null;
        mReverseGeocoder = mDeviceController.getEngine().getUtility(ReverseGeocoderUtility.class);
        loadPersistedData();
        if (isPersisted()) {
            mAntiFlicker.publish();
        }
    }

    @Override
    protected void onConnecting() {
        mReceivedMode = null;
        mSentEmulationMode = null;
    }

    @Override
    protected void onConnected() {
        applyPresets();
        mAntiFlicker.publish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureCamera.UID) {
            ArsdkFeatureCamera.decode(command, mArsdkFeatureCameraCallback);
        }
    }

    @Override
    protected void onDisconnected() {
        // clear all non saved settings
        mAntiFlicker.cancelSettingsRollbacks()
                    .updateValue(AntiFlicker.Value.UNKNOWN);

        if (offlineSettingsEnabled()) {
            mAntiFlicker.notifyUpdated();
        } else {
            stopCountryMonitoring();
            mAntiFlicker.unpublish();
        }
    }

    @Override
    protected void onPresetChange() {
        mPresetDict = mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY);
        if (isConnected()) {
            applyPresets();
        }
        mAntiFlicker.notifyUpdated();
    }

    @Override
    protected void onForgetting() {
        if (mDeviceDict != null) {
            mDeviceDict.clear().commit();
        }
        stopCountryMonitoring();
        mAntiFlicker.unpublish();
    }

    @Override
    protected void onDispose() {
        stopCountryMonitoring();
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
        EnumSet<AntiFlicker.Mode> supportedModes = SUPPORTED_MODES_SETTING.load(mDeviceDict);
        if (supportedModes != null) {
            mAntiFlicker.mode().updateAvailableValues(supportedModes);
        }
        applyPresets();
    }

    /**
     * Applies component's persisted presets.
     */
    private void applyPresets() {
        applyMode(MODE_PRESET.load(mPresetDict));
    }

    /**
     * Applies anti-flicker mode
     * <ul>
     * <li>Finds an appropriate fallback value if the given value is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the component's setting accordingly.</li>
     * </ul>
     * <p>
     * Controls monitoring of the current location electric frequency depending on the requested {@code mode} and
     * whether {@link AntiFlicker.Mode#AUTO} mode {@link #mAutoModeEmulation emulation} is required.
     *
     * @param mode value to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applyMode(@Nullable AntiFlicker.Mode mode) {
        if (mode == null || !mAntiFlicker.mode().getAvailableValues().contains(mode)) {
            if (mMode == null) {
                return false;
            }
            mode = mMode;
        }

        CountryFrequency locationFreq = null;
        if (mode == AntiFlicker.Mode.AUTO && mAutoModeEmulation) {
            startCountryMonitoring();
            locationFreq = getCountryFrequency();
        } else {
            stopCountryMonitoring();
        }

        boolean updating = (mMode != mode
                            || mLocationFreq != locationFreq)
                           && sendMode(mode, locationFreq);

        mMode = mode;
        mLocationFreq = locationFreq;
        mAntiFlicker.mode().updateValue(mMode);
        return updating;
    }

    /**
     * Sends selected anti-flickering mode to the device.
     *
     * @param mode             anti-flickering mode to set
     * @param geoLocalizedFreq when {@code mode} is {@code AUTO} and {@link #mAutoModeEmulation auto mode is
     *                         emulated}, gives the appropriate frequency to use from current geolocation. May be {@code
     *                         null}, in which case the implementation may try to apply a suitable fallback by itself
     *
     * @return {@code true} if any command was sent to the device, otherwise {@code false}
     */
    private boolean sendMode(@NonNull AntiFlicker.Mode mode, @Nullable CountryFrequency geoLocalizedFreq) {
        ArsdkFeatureCamera.AntiflickerMode arsdkMode = null;
        mSentEmulationMode = null;
        if (mode != AntiFlicker.Mode.AUTO || !mAutoModeEmulation) {
            arsdkMode = AntiflickerModeAdapter.from(mode);
        } else if (geoLocalizedFreq != null) {
            switch (geoLocalizedFreq) {
                case HZ_50:
                    arsdkMode = ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ;
                    break;
                case HZ_60:
                    arsdkMode = ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ;
                    break;
            }
            mSentEmulationMode = arsdkMode;
        }

        return arsdkMode != null
               && arsdkMode != mReceivedMode
               && sendCommand(ArsdkFeatureCamera.encodeSetAntiflickerMode(arsdkMode));
    }

    /**
     * Starts monitoring current country changes in case it is stopped.
     */
    private void startCountryMonitoring() {
        if (mReverseGeocoder != null && mReverseGeocoderMonitor == null) {
            mReverseGeocoderMonitor = () -> applyMode(AntiFlicker.Mode.AUTO);
            mReverseGeocoder.monitorWith(mReverseGeocoderMonitor);
        }
    }

    /**
     * Stops monitoring current country changes in case it is started.
     */
    private void stopCountryMonitoring() {
        if (mReverseGeocoder != null && mReverseGeocoderMonitor != null) {
            mReverseGeocoder.disposeMonitor(mReverseGeocoderMonitor);
        }
    }

    /**
     * Retrieves the electric frequency in use in the current country.
     *
     * @return detected country electric frequency if available, otherwise {@code null}
     */
    @Nullable
    private CountryFrequency getCountryFrequency() {
        if (mReverseGeocoder == null) {
            return null;
        }
        Address address = mReverseGeocoder.getAddress();
        if (address == null) {
            return null;
        }
        String country = address.getCountryCode();
        if (country == null) {
            return null;
        }
        return COUNTRIES_60HZ.contains(country.toUpperCase(Locale.ROOT)) ?
                CountryFrequency.HZ_60 : CountryFrequency.HZ_50;
    }

    /** Callbacks called when a command of the feature ArsdkFeatureCamera is decoded. */
    private final ArsdkFeatureCamera.Callback mArsdkFeatureCameraCallback = new ArsdkFeatureCamera.Callback() {

        @Override
        public void onAntiflickerCapabilities(int supportedModesBitField) {
            EnumSet<AntiFlicker.Mode> supportedModes = AntiflickerModeAdapter.from(supportedModesBitField);

            // In case the device does not support a _real_ autonomous antiflicker selection mode, but support multiple
            // manual modes and reverse geo-coding is available, then AUTO mode is forcefully added to the peripheral's
            //supported modes. AUTO mode is then emulated by monitoring and reverse geo-coding current location to
            // determine the appropriate antiflicker mode to use based on the location known electric frequency.
            mAutoModeEmulation = !supportedModes.contains(AntiFlicker.Mode.AUTO)
                                 && supportedModes.contains(AntiFlicker.Mode.HZ_50)
                                 && supportedModes.contains(AntiFlicker.Mode.HZ_60)
                                 && mReverseGeocoder != null;

            if (mAutoModeEmulation) {
                supportedModes.add(AntiFlicker.Mode.AUTO);
            }

            SUPPORTED_MODES_SETTING.save(mDeviceDict, supportedModes);
            mAntiFlicker.mode().updateAvailableValues(supportedModes);

            mAntiFlicker.notifyUpdated();
        }

        @Override
        public void onAntiflickerMode(@Nullable ArsdkFeatureCamera.AntiflickerMode mode,
                                      @Nullable ArsdkFeatureCamera.AntiflickerMode value) {
            if (mode == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid antiflicker mode");
            }
            if (value == null || value == ArsdkFeatureCamera.AntiflickerMode.AUTO) {
                throw new ArsdkCommand.RejectedEventException("Invalid antiflicker value");
            }

            mReceivedMode = mode;
            mMode = mSentEmulationMode == mode ? AntiFlicker.Mode.AUTO : AntiflickerModeAdapter.from(mode);
            if (isConnected()) {
                mAntiFlicker.mode()
                            .updateValue(mMode);
            }

            mAntiFlicker.updateValue(AntiflickerModeAdapter.toValue(value));
            mAntiFlicker.notifyUpdated();
        }
    };

    /** Backend of AntiFlickerCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final AntiFlickerCore.Backend mBackend = new AntiFlickerCore.Backend() {

        @Override
        public boolean setMode(@NonNull AntiFlicker.Mode mode) {
            boolean updating = applyMode(mode);
            MODE_PRESET.save(mPresetDict, mode);
            if (!updating) {
                mAntiFlicker.notifyUpdated();
            }
            return updating;
        }
    };
}
