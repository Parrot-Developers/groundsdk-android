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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.wifi;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.device.peripheral.WifiAccessPoint;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Band;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Channel;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.device.peripheral.wifi.WifiAccessPointCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.wifi.WifiScannerCore;
import com.parrot.drone.groundsdk.internal.utility.ReverseGeocoderUtility;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCommon;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureWifi;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_WIFI;

/** WifiAccessPoint peripheral controller for Anafi family drones. */
public class AnafiWifiAccessPoint extends DronePeripheralController {

    /** All country codes defined in ISO 3166-1 alpha-2. */
    private static final Set<String> ISO_COUNTRY_CODES = new HashSet<>(Arrays.asList(Locale.getISOCountries()));

    /** Reverse geocoder utility, {@code null} if not available. */
    @Nullable
    private final ReverseGeocoderUtility mReverseGeocoder;

    /** Monitor of reverse geocoder utility. */
    @NonNull
    private final ReverseGeocoderUtility.Monitor mReverseGeocoderMonitor = this::sendDetectedCountry;

    /** WifiAccessPoint peripheral for which this object is the backend. */
    @NonNull
    private final WifiAccessPointCore mWifiAccessPoint;

    /** WifiScanner peripheral for which this object is the backend. */
    @NonNull
    private final WifiScannerCore mWifiScanner;

    /** Map of occupation rate, by channel. {@code null} when not scanning */
    @Nullable
    private Map<Channel, Integer> mScannedChannels;

    /** Current access point environment. */
    @Nullable
    private WifiAccessPoint.Environment mEnvironment;

    /** Set of channels currently available for outdoor mode use. */
    @NonNull
    private final Set<Channel> mOutdoorChannels;

    /** Set of channels currently available for indoor mode use. */
    @NonNull
    private final Set<Channel> mIndoorChannels;

    /** Set of available country codes. */
    @NonNull
    private final Set<String> mAvailableCodes;

    /** Current access point country code. */
    @Nullable
    private String mCountryCode;

    /** {@code true} when the access point is in automatic country selection mode, otherwise {@code false}. */
    private boolean mAutoCountry;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiWifiAccessPoint(@NonNull DroneController droneController) {
        super(droneController);
        mReverseGeocoder = mDeviceController.getEngine().getUtility(ReverseGeocoderUtility.class);
        mWifiAccessPoint = new WifiAccessPointCore(mComponentStore, mAccessPointBackend);
        mWifiScanner = new WifiScannerCore(mComponentStore, mScannerBackend);
        mOutdoorChannels = EnumSet.noneOf(Channel.class);
        mIndoorChannels = EnumSet.noneOf(Channel.class);
        mAvailableCodes = new HashSet<>();
    }

    @Override
    protected void onConnected() {
        if (GroundSdkConfig.get().shouldAutoSelectWifiCountry()) {
            // force country to the one found by reverse geocoding location
            sendDetectedCountry();
            if (mReverseGeocoder != null) {
                mReverseGeocoder.monitorWith(mReverseGeocoderMonitor);
            }
            mWifiAccessPoint.country().updateAvailableCodes(Collections.singleton(mCountryCode));
            // force environment to outdoor
            if (mEnvironment == WifiAccessPoint.Environment.INDOOR) {
                mAccessPointBackend.setEnvironment(WifiAccessPoint.Environment.OUTDOOR);
            }
            mWifiAccessPoint.environment().updateAvailableValues(EnumSet.of(WifiAccessPoint.Environment.OUTDOOR));
        }
        mWifiAccessPoint.publish();
        mWifiScanner.publish();
    }

    @Override
    protected void onDisconnected() {
        mWifiScanner.unpublish();
        mWifiAccessPoint.unpublish();
        if (mReverseGeocoder != null) {
            mReverseGeocoder.disposeMonitor(mReverseGeocoderMonitor);
        }
        mScannedChannels = null;
        mEnvironment = null;
        mCountryCode = null;
        mOutdoorChannels.clear();
        mIndoorChannels.clear();
        mAvailableCodes.clear();
        mAutoCountry = false;
    }

    /**
     * Sets the access point country to the one found by reverse geocoding location, if it differs from the current
     * one.
     */
    private void sendDetectedCountry() {
        if (mReverseGeocoder != null && mReverseGeocoder.getAddress() != null) {
            String countryCode = mReverseGeocoder.getAddress().getCountryCode();
            if (countryCode != null && !countryCode.equals(mCountryCode)) {
                mAccessPointBackend.setCountry(countryCode);
            }
        }
    }

    @Override
    protected final void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureWifi.UID) {
            ArsdkFeatureWifi.decode(command, mWifiCallback);
        } else if (featureId == ArsdkFeatureCommon.SettingsState.UID) {
            ArsdkFeatureCommon.SettingsState.decode(command, mSettingsStateCallback);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureCommon.WifiSettingsState is decoded. */
    private final ArsdkFeatureWifi.Callback mWifiCallback = new ArsdkFeatureWifi.Callback() {

        @Override
        public void onScannedItem(String ssid, int rssi, @Nullable ArsdkFeatureWifi.Band band, int channelId,
                                  int listFlags) {
            if (ULog.d(TAG_WIFI)) {
                ULog.d(TAG_WIFI, "onScannedItem [ssid: " + ssid + ", rssi: " + rssi + ", band: " + band
                                 + ", channel: " + channelId
                                 + ", listFlags: " + ArsdkFeatureGeneric.ListFlags.fromBitfield(listFlags) + "]");
            }

            if (mScannedChannels != null) {
                boolean clearList = ArsdkFeatureGeneric.ListFlags.EMPTY.inBitField(listFlags);
                if (clearList) {
                    mScannedChannels.clear();
                } else {
                    boolean first = ArsdkFeatureGeneric.ListFlags.FIRST.inBitField(listFlags);
                    // we don't add items (except the first one) if the map is empty, because this means those
                    // are from a previous scan request and another one is pending
                    if (first || !mScannedChannels.isEmpty()) {
                        if (first) {
                            mScannedChannels.clear();
                        }
                        Channel channel = Channels.get(band, channelId);
                        if (channel != null) {
                            Integer currentCount = mScannedChannels.get(channel);
                            if (ArsdkFeatureGeneric.ListFlags.REMOVE.inBitField(listFlags) && currentCount != null) {
                                mScannedChannels.put(channel, currentCount - 1);
                            } else {
                                mScannedChannels.put(channel, (currentCount == null ? 0 : currentCount) + 1);
                            }
                        }
                    }
                }

                if (clearList || ArsdkFeatureGeneric.ListFlags.LAST.inBitField(listFlags)) {
                    mWifiScanner.updateScannedChannels(mScannedChannels).notifyUpdated();
                    // scan again
                    sendCommand(ArsdkFeatureWifi.encodeScan(
                            ArsdkFeatureWifi.Band.toBitField(ArsdkFeatureWifi.Band.values())));
                }
            }
        }

        @Override
        public void onAuthorizedChannel(@Nullable ArsdkFeatureWifi.Band band, int channelId, int environmentBitField,
                                        int listFlags) {
            if (ULog.d(TAG_WIFI)) {
                ULog.d(TAG_WIFI, "onAuthorizedChannel [band: " + band + ", channel: " + channelId
                                 + ", environmentBitField: "
                                 + ArsdkFeatureWifi.Environment.fromBitfield(environmentBitField)
                                 + ", listFlagsBitField: " + ArsdkFeatureGeneric.ListFlags.fromBitfield(listFlags)
                                 + "]");
            }

            boolean clearList = ArsdkFeatureGeneric.ListFlags.EMPTY.inBitField(listFlags);
            if (clearList) {
                mOutdoorChannels.clear();
                mIndoorChannels.clear();
            } else {
                if (ArsdkFeatureGeneric.ListFlags.FIRST.inBitField(listFlags)) {
                    mOutdoorChannels.clear();
                    mIndoorChannels.clear();
                }

                Channel channel = Channels.get(band, channelId);
                if (channel != null) {
                    EnumSet<ArsdkFeatureWifi.Environment> envs =
                            ArsdkFeatureWifi.Environment.fromBitfield(environmentBitField);
                    if (envs.contains(ArsdkFeatureWifi.Environment.INDOOR)) {
                        if (ArsdkFeatureGeneric.ListFlags.REMOVE.inBitField(listFlags)) {
                            mIndoorChannels.remove(channel);
                        } else {
                            mIndoorChannels.add(channel);
                        }
                    }
                    if (envs.contains(ArsdkFeatureWifi.Environment.OUTDOOR)) {
                        if (ArsdkFeatureGeneric.ListFlags.REMOVE.inBitField(listFlags)) {
                            mOutdoorChannels.remove(channel);
                        } else {
                            mOutdoorChannels.add(channel);
                        }
                    }
                }
            }

            if (mEnvironment != null && (clearList || ArsdkFeatureGeneric.ListFlags.LAST.inBitField(listFlags))) {
                switch (mEnvironment) {
                    case INDOOR:
                        mWifiAccessPoint.channel().updateAvailableChannels(mIndoorChannels);
                        break;
                    case OUTDOOR:
                        mWifiAccessPoint.channel().updateAvailableChannels(mOutdoorChannels);
                        break;
                }
                mWifiAccessPoint.notifyUpdated();
            }
        }

        @Override
        public void onApChannelChanged(@Nullable ArsdkFeatureWifi.SelectionType type,
                                       @Nullable ArsdkFeatureWifi.Band band, int channelId) {
            if (ULog.d(TAG_WIFI)) {
                ULog.d(TAG_WIFI, "onApChannelChanged [type: " + type + ", band: " + band + ", channel: " + channelId
                                 + "]");
            }

            if (type != null) {
                WifiAccessPoint.ChannelSetting.SelectionMode selectionMode = null;
                switch (type) {
                    case AUTO_ALL:
                        selectionMode = WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_ANY_BAND;
                        break;
                    case AUTO_2_4_GHZ:
                        selectionMode = WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_2_4_GHZ_BAND;
                        break;
                    case AUTO_5_GHZ:
                        selectionMode = WifiAccessPoint.ChannelSetting.SelectionMode.AUTO_5_GHZ_BAND;
                        break;
                    case MANUAL:
                        selectionMode = WifiAccessPoint.ChannelSetting.SelectionMode.MANUAL;
                        break;
                }
                mWifiAccessPoint.channel().updateChannel(selectionMode, Channels.obtain(band, channelId));
                mWifiAccessPoint.notifyUpdated();
            }
        }

        @Override
        public void onSecurityChanged(@Nullable ArsdkFeatureWifi.SecurityType type, String key,
                                      @Nullable ArsdkFeatureWifi.SecurityKeyType keyType) {
            if (ULog.d(TAG_WIFI)) {
                ULog.d(TAG_WIFI, "onSecurityChanged [key: " + key + ", keyType: " + keyType + "]");
            }

            if (type != null) {
                switch (type) {
                    case OPEN:
                        mWifiAccessPoint.security().updateMode(WifiAccessPoint.SecuritySetting.Mode.OPEN);
                        break;
                    case WPA2:
                        mWifiAccessPoint.security().updateMode(WifiAccessPoint.SecuritySetting.Mode.WPA2_SECURED);
                        break;
                }
                mWifiAccessPoint.notifyUpdated();
            }
        }

        @Override
        public void onCountryChanged(@Nullable ArsdkFeatureWifi.CountrySelection selectionMode, String code) {
            if (ULog.d(TAG_WIFI)) {
                ULog.d(TAG_WIFI, "onCountryChanged [selectionMode: " + selectionMode + ", code: " + code + "]");
            }

            mCountryCode = code;
            mAutoCountry = selectionMode == ArsdkFeatureWifi.CountrySelection.AUTO;

            sendCommand(ArsdkFeatureWifi.encodeUpdateAuthorizedChannels());

            mWifiAccessPoint.country()
                            .updateCode(code)
                            .updateDefaultCountryUsed(mAutoCountry && mAvailableCodes.size() > 1);
            if (GroundSdkConfig.get().shouldAutoSelectWifiCountry()) {
                mWifiAccessPoint.country().updateAvailableCodes(Collections.singleton(code));
            }
            mWifiAccessPoint.notifyUpdated();
        }

        @Override
        public void onEnvironmentChanged(@Nullable ArsdkFeatureWifi.Environment environment) {
            if (ULog.d(TAG_WIFI)) {
                ULog.d(TAG_WIFI, "onEnvironmentChanged [environment: " + environment + "]");
            }

            if (environment != null) {
                switch (environment) {
                    case INDOOR:
                        mEnvironment = WifiAccessPoint.Environment.INDOOR;
                        mWifiAccessPoint.channel().updateAvailableChannels(mIndoorChannels);
                        break;
                    case OUTDOOR:
                        mEnvironment = WifiAccessPoint.Environment.OUTDOOR;
                        mWifiAccessPoint.channel().updateAvailableChannels(mOutdoorChannels);
                        break;
                }

                mWifiAccessPoint.environment().updateValue(mEnvironment);
                mWifiAccessPoint.notifyUpdated();
            }
        }

        @Override
        public void onSupportedCountries(String countryCodes) {
            if (ULog.d(TAG_WIFI)) {
                ULog.d(TAG_WIFI, "onSupportedCountries [countryCodes: " + countryCodes + "]");
            }

            mAvailableCodes.clear();

            for (String code : countryCodes.trim().toUpperCase(Locale.ROOT).split(";")) {
                if (ISO_COUNTRY_CODES.contains(code)) {
                    mAvailableCodes.add(code);
                } else if (ULog.w(TAG_WIFI)) {
                    ULog.w(TAG_WIFI, "Unknown country code: " + code);
                }
            }
            mWifiAccessPoint.country()
                            .updateAvailableCodes(mAvailableCodes)
                            .updateDefaultCountryUsed(mAutoCountry && mAvailableCodes.size() > 1);
            mWifiAccessPoint.notifyUpdated();
        }

        @Override
        public void onSupportedSecurityTypes(int typesBitField) {
            if (ULog.d(TAG_WIFI)) {
                ULog.d(TAG_WIFI, "onSupportedSecurityTypes [typesBitField: " +
                                 ArsdkFeatureWifi.SecurityType.fromBitfield(typesBitField) + "]");
            }

            EnumSet<WifiAccessPoint.SecuritySetting.Mode> modes = EnumSet.noneOf(
                    WifiAccessPoint.SecuritySetting.Mode.class);

            for (ArsdkFeatureWifi.SecurityType type : ArsdkFeatureWifi.SecurityType.fromBitfield(typesBitField)) {
                switch (type) {
                    case OPEN:
                        modes.add(WifiAccessPoint.SecuritySetting.Mode.OPEN);
                        break;
                    case WPA2:
                        modes.add(WifiAccessPoint.SecuritySetting.Mode.WPA2_SECURED);
                        break;
                }
            }
            mWifiAccessPoint.security().updateSupportedModes(modes);
            mWifiAccessPoint.notifyUpdated();
        }
    };

    /** Callbacks called when a command of the feature ArsdkFeatureCommon.SettingsState is decoded. */
    private final ArsdkFeatureCommon.SettingsState.Callback mSettingsStateCallback =
            new ArsdkFeatureCommon.SettingsState.Callback() {

                @Override
                public void onProductNameChanged(String name) {
                    if (ULog.d(TAG_WIFI)) {
                        ULog.d(TAG_WIFI, "onProductNameChanged [name: " + name + "]");
                    }
                    mWifiAccessPoint.ssid().updateValue(name);
                    mWifiAccessPoint.notifyUpdated();
                }
            };

    /** Backend of WifiAccessPointCore implementation. */
    private final WifiAccessPointCore.Backend mAccessPointBackend = new WifiAccessPointCore.Backend() {

        @Override
        public boolean setEnvironment(@NonNull WifiAccessPoint.Environment environment) {
            switch (environment) {
                case INDOOR:
                    sendCommand(ArsdkFeatureWifi.encodeSetEnvironment(ArsdkFeatureWifi.Environment.INDOOR));
                    return true;
                case OUTDOOR:
                    sendCommand(ArsdkFeatureWifi.encodeSetEnvironment(ArsdkFeatureWifi.Environment.OUTDOOR));
                    return true;
            }
            return false;
        }

        @Override
        public boolean setCountry(@NonNull String code) {
            sendCommand(ArsdkFeatureWifi.encodeSetCountry(ArsdkFeatureWifi.CountrySelection.MANUAL, code));
            return true;
        }

        @Override
        public boolean setSsid(@NonNull String ssid) {
            sendCommand(ArsdkFeatureCommon.Settings.encodeProductName(ssid));
            return true;
        }

        @Override
        public boolean selectChannel(@NonNull Channel channel) {
            sendCommand(ArsdkFeatureWifi.encodeSetApChannel(ArsdkFeatureWifi.SelectionType.MANUAL,
                    toArsdkBand(channel.getBand()), channel.getChannelId()));
            return true;
        }

        @Override
        public boolean autoSelectChannel(@Nullable Band band) {
            ArsdkFeatureWifi.SelectionType selectionType = ArsdkFeatureWifi.SelectionType.AUTO_ALL;
            if (band != null) {
                switch (band) {
                    case B_2_4_GHZ:
                        selectionType = ArsdkFeatureWifi.SelectionType.AUTO_2_4_GHZ;
                        break;
                    case B_5_GHZ:
                        selectionType = ArsdkFeatureWifi.SelectionType.AUTO_5_GHZ;
                        break;
                }
            }
            sendCommand(ArsdkFeatureWifi.encodeSetApChannel(selectionType, ArsdkFeatureWifi.Band.E2_4_GHZ, 0));
            return true;
        }

        @Override
        public boolean setSecurity(@NonNull WifiAccessPoint.SecuritySetting.Mode mode, @Nullable String password) {
            switch (mode) {
                case OPEN:
                    sendCommand(ArsdkFeatureWifi.encodeSetSecurity(ArsdkFeatureWifi.SecurityType.OPEN, "",
                            ArsdkFeatureWifi.SecurityKeyType.PLAIN));
                    return true;
                case WPA2_SECURED:
                    if (!TextUtils.isEmpty(password)) {
                        sendCommand(ArsdkFeatureWifi.encodeSetSecurity(ArsdkFeatureWifi.SecurityType.WPA2, password,
                                ArsdkFeatureWifi.SecurityKeyType.PLAIN));
                        return true;
                    }
                    break;
            }
            return false;
        }

        /**
         * Converts the given frequency {@code Band} to its corresponding Arsdk band counterpart.
         *
         * @param band frequency band to convert
         *
         * @return corresponding Arsdk band
         */
        private ArsdkFeatureWifi.Band toArsdkBand(@NonNull Band band) {
            switch (band) {
                case B_2_4_GHZ:
                    return ArsdkFeatureWifi.Band.E2_4_GHZ;
                case B_5_GHZ:
                    return ArsdkFeatureWifi.Band.E5_GHZ;
            }
            throw new IllegalArgumentException("Unsupported wifi band: " + band);
        }
    };

    /** Backend of WifiScannerCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final WifiScannerCore.Backend mScannerBackend = new WifiScannerCore.Backend() {

        @Override
        public void startScan() {
            if (mScannedChannels == null) {
                mScannedChannels = new EnumMap<>(Channel.class);
                mWifiScanner.updateScannedChannels(mScannedChannels).updateScanningFlag(true).notifyUpdated();
                sendCommand(ArsdkFeatureWifi.encodeScan(
                        ArsdkFeatureWifi.Band.toBitField(ArsdkFeatureWifi.Band.values())));
            }
        }

        @Override
        public void stopScan() {
            if (mScannedChannels != null) {
                mScannedChannels = null;
                mWifiScanner.updateScannedChannels(Collections.emptyMap())
                            .updateScanningFlag(false)
                            .notifyUpdated();
            }
        }
    };
}
