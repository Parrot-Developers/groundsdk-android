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

package com.parrot.drone.groundsdk.internal;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.BuildConfig;
import com.parrot.drone.groundsdk.R;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.FlightDataDownloader;
import com.parrot.drone.groundsdk.device.peripheral.Updater;
import com.parrot.drone.groundsdk.facility.BlackBoxReporter;
import com.parrot.drone.groundsdk.facility.CrashReporter;
import com.parrot.drone.groundsdk.facility.FirmwareManager;
import com.parrot.drone.groundsdk.facility.FlightDataManager;
import com.parrot.drone.groundsdk.facility.FlightLogReporter;
import com.parrot.drone.groundsdk.facility.GutmaLogManager;
import com.parrot.drone.groundsdk.internal.device.DeviceModels;
import com.parrot.drone.groundsdk.internal.utility.BlackBoxStorage;
import com.parrot.drone.groundsdk.internal.utility.CrashReportStorage;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;
import com.parrot.drone.groundsdk.internal.utility.FirmwareStore;
import com.parrot.drone.groundsdk.internal.utility.FlightDataStorage;
import com.parrot.drone.groundsdk.internal.utility.FlightLogStorage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * GroundSdk Global Configuration.
 * <p>
 * Allows internal configuration of GroundSdk features. This configuration allows overriding settings in
 * {@code config.xml} resource file for test purposes.
 * <p>
 * Settings configured through this API override defaults set in {@code config.xml} resource file.
 */
public final class GroundSdkConfig {

    /** A megabyte, in bytes. */
    private static final long MEGABYTE = 1024 * 1024;

    /** Exception thrown when the configuration is detected to be erroneous. */
    private static final class ConfigurationError extends Error {

        /**
         * Constructor.
         *
         * @param detailMessage the detail message for this error
         */
        ConfigurationError(String detailMessage) {
            super(detailMessage + ". Fix GroundSDK configuration");
        }
    }

    /**
     * Offline settings mode.
     * <p>
     * Tells if settings are locally stored and sent to the drone during connection. <br>
     * Possible values are: <ul>
     * <li>{@code OFF} to disable local settings storage, </li>
     * <li>{@code MODEL} to share local settings between all drone of the same model. </li>
     * </ul>
     */
    public enum OfflineSettingsMode {

        /** Don't store offline settings. */
        OFF("OFF"),

        /** Store offline settings, settings are shared by drone model. */
        MODEL("MODEL");

        /** String used in resources for this setting value. */
        @NonNull
        private final String mKey;

        /**
         * Constructor.
         *
         * @param key String used in resources for this setting value
         */
        OfflineSettingsMode(@NonNull String key) {
            mKey = key;
        }

        /**
         * Gets the storage string key.
         *
         * @return storage string key
         */
        @NonNull
        String getKey() {
            return mKey;
        }
    }

    /** Singleton instance. */
    @Nullable
    private static GroundSdkConfig sInstance;

    /** Application key, empty if application doesn't have a key. */
    @NonNull
    private String mApplicationKey;

    /** {@code true} if support of Wifi connection is enabled. */
    private boolean mWifiEnabled;

    /** {@code true} if support for Usb connection is enabled (SkyController 2). */
    private boolean mUsbEnabled;

    /**
     * {@code true} if support for Usb debug connection is enabled (to connect to a remote control through USB Debug
     * Bridge application).
     */
    private boolean mUsbDebugEnabled;

    /** {@code true} if support of Bluetooth Low Energy is enabled. */
    private boolean mBleEnabled;

    /** {@code true} if support of DevToolbox is enabled. */
    private boolean mDevToolboxEnabled;

    /** {@code true} if support of crash reporter is enabled. */
    private boolean mCrashReportEnabled;

    /** {@code true} if firmware synchronization is enabled. */
    private boolean mFirmwareEnabled;

    /** URL of an alternate firmware server. Empty to use the default server */
    @NonNull
    private final String mAlternateFirmwareServer;

    /** {@code true} if black box synchronization is enabled. */
    private boolean mBlackBoxEnabled;

    /** Black box public folder. Empty to disable copy to public folder. */
    @NonNull
    private String mBlackBoxPublicFolder;

    /** {@code true} if GPS ephemerides sync is enabled. */
    private boolean mEphemeridesEnabled;

    /** {@code true} if flight data synchronization is enabled. */
    private boolean mFlightDataEnabled;

    /** {@code true} if support of flight log is enabled. */
    private boolean mFlightLogEnabled;

    /** {@code true} if GUTMA log synchronization is enabled. */
    private boolean mGutmaLogEnabled;

    /** {@code true} if video decoding is enabled. */
    private boolean mVideoDecodingEnabled;

    /** {@code true} if auto-connection should start immediately when the first session is opened. */
    private boolean mAutoConnectionAtStartup;

    /** {@code true} if the wifi access point country should automatically be selected from system location. */
    private boolean mAutoSelectWifiCountry;

    /** Default country code returned by the reverse geocoder. Empty to not return a default country code. */
    @NonNull
    private final String mReverseGeocoderDefaultCountryCode;

    /** Offline settings mode. */
    @NonNull
    private OfflineSettingsMode mOfflineSettingsMode;

    /** Supported device models. */
    @NonNull
    private Set<DeviceModel> mSupportedDevices;

    /** Application package name. */
    @NonNull
    private final String mApplicationPackage;

    /** Application version. */
    @NonNull
    private String mApplicationVersion;

    /** Crash report storage space quota, in bytes. */
    @IntRange(from = 0)
    private long mCrashReportQuota;

    /** Blackbox storage space quota, in bytes. */
    @IntRange(from = 0)
    private long mBlackboxQuota;

    /** Flight data storage space quota, in bytes. */
    @IntRange(from = 0)
    private long mFlightDataQuota;

    /** GUTMA log storage space quota, in bytes. */
    @IntRange(from = 0)
    private long mGutmaLogQuota;

    /** Flight log storage space quota, in bytes. */
    @IntRange(from = 0)
    private long mFlightLogQuota;

    /** Media thumbnail max cache size, in bytes. */
    @IntRange(from = 0)
    private final long mThumbnailCacheSize;

    /** True if the config has been locked and cannot be change anymore. */
    private boolean mLocked;

    /**
     * Load the configuration if required and mark it as locked.
     *
     * @param context application context.
     */
    static void lock(@NonNull Context context) {
        get(context).mLocked = true;
    }

    /**
     * Gets configuration shared instance, creating it if required.
     *
     * @param context application context
     *
     * @return configuration shared instance
     */
    @NonNull
    public static GroundSdkConfig get(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new GroundSdkConfig(context);
        }
        return sInstance;
    }

    /**
     * Gets current configuration instance.
     *
     * @return configuration current instance
     */
    @NonNull
    public static GroundSdkConfig get() {
        assert sInstance != null;
        return sInstance;
    }

    /**
     * Load singleton with default values.
     *
     * @return {@code GroundSdkConfig} singleton loaded with default values
     */
    @VisibleForTesting
    public static GroundSdkConfig loadDefaults() {
        sInstance = new GroundSdkConfig();
        return sInstance;
    }

    /**
     * Clears current singleton. This bypass singleton pattern for unit tests.
     */
    @VisibleForTesting
    static void close() {
        sInstance = null;
    }

    /**
     * Constructor.
     *
     * @param context android context
     */
    private GroundSdkConfig(@NonNull Context context) {
        Resources resources = context.getResources();
        mApplicationKey = resources.getString(R.string.gsdk_application_key);
        mWifiEnabled = resources.getBoolean(R.bool.gsdk_wifi_enabled);
        mUsbEnabled = resources.getBoolean(R.bool.gsdk_usb_enabled);
        mUsbDebugEnabled = resources.getBoolean(R.bool.gsdk_usb_debug_enabled);
        mBleEnabled = resources.getBoolean(R.bool.gsdk_ble_enabled);
        mDevToolboxEnabled = resources.getBoolean(R.bool.gsdk_dev_toolbox_enabled);
        mCrashReportEnabled = resources.getBoolean(R.bool.gsdk_crash_report_enabled);
        mFlightLogEnabled = resources.getBoolean(R.bool.gsdk_flight_log_enabled);
        mVideoDecodingEnabled = resources.getBoolean(R.bool.gsdk_video_decoding_enabled);
        mFirmwareEnabled = resources.getBoolean(R.bool.gsdk_firmware_enabled);
        mAlternateFirmwareServer = resources.getString(R.string.gsdk_firmware_server);
        mBlackBoxEnabled = resources.getBoolean(R.bool.gsdk_blackbox_enabled);
        mBlackBoxPublicFolder = resources.getString(R.string.gsdk_blackbox_public_folder);
        mEphemeridesEnabled = resources.getBoolean(R.bool.gsdk_ephemeris_sync_enabled);
        mFlightDataEnabled = resources.getBoolean(R.bool.gsdk_flight_data_enabled);
        mGutmaLogEnabled = resources.getBoolean(R.bool.gsdk_gutma_log_enabled);
        mAutoConnectionAtStartup = resources.getBoolean(R.bool.gsdk_auto_connection_at_startup);
        mAutoSelectWifiCountry = resources.getBoolean(R.bool.gsdk_auto_select_wifi_country);
        mReverseGeocoderDefaultCountryCode = resources.getString(R.string.gsdk_reverse_geocoder_default_country_code);
        mOfflineSettingsMode = offlineSettingsModeFromString(resources.getString(R.string.gsdk_offline_settings_mode));
        mSupportedDevices = deviceModelsFromStringArray(resources.getStringArray(R.array.gsdk_supported_devices));
        mApplicationPackage = context.getPackageName();
        try {
            mApplicationVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            mApplicationVersion = "0.0.0";
        }

        int quota = resources.getInteger(R.integer.gsdk_crash_report_quota);
        if (quota < 0) {
            throw new ConfigurationError("gsdk_crash_report_quota must be positive");
        }
        mCrashReportQuota = quota == 0 ? Long.MAX_VALUE : quota;

        quota = resources.getInteger(R.integer.gsdk_blackbox_quota);
        if (quota < 0) {
            throw new ConfigurationError("gsdk_blackbox_quota must be positive");
        }
        mBlackboxQuota = quota == 0 ? Long.MAX_VALUE : quota;

        quota = resources.getInteger(R.integer.gsdk_flight_data_quota);
        if (quota < 0) {
            throw new ConfigurationError("gsdk_flight_data_quota must be positive");
        }
        mFlightDataQuota = quota == 0 ? Long.MAX_VALUE : quota;

        quota = resources.getInteger(R.integer.gsdk_flight_log_quota);
        if (quota < 0) {
            throw new ConfigurationError("gsdk_flight_log_quota must be positive");
        }
        mFlightLogQuota = quota == 0 ? Long.MAX_VALUE : quota;

        quota = resources.getInteger(R.integer.gsdk_gutma_log_quota);
        if (quota < 0) {
            throw new ConfigurationError("gsdk_gutma_log_quota must be positive");
        }
        mGutmaLogQuota = quota == 0 ? Long.MAX_VALUE : quota;

        mThumbnailCacheSize = resources.getInteger(R.integer.gsdk_media_thumbnail_cache_size);
        if (mThumbnailCacheSize < 0) {
            throw new ConfigurationError("gsdk_media_thumbnail_cache_size must be positive");
        }
    }

    /**
     * Constructor used for testing. Load default values for tests.
     */
    private GroundSdkConfig() {
        mApplicationKey = "key";
        mWifiEnabled = false;
        mUsbEnabled = false;
        mUsbDebugEnabled = false;
        mBleEnabled = false;
        mDevToolboxEnabled = false;
        mCrashReportEnabled = false;
        mFlightLogEnabled = false;
        mVideoDecodingEnabled = true;
        mFirmwareEnabled = false;
        mAlternateFirmwareServer = "";
        mBlackBoxEnabled = false;
        mBlackBoxPublicFolder = "";
        mEphemeridesEnabled = false;
        mFlightDataEnabled = false;
        mGutmaLogEnabled = false;
        mAutoConnectionAtStartup = false;
        mAutoSelectWifiCountry = false;
        mReverseGeocoderDefaultCountryCode = "";
        mOfflineSettingsMode = OfflineSettingsMode.MODEL;
        mSupportedDevices = DeviceModels.ALL;
        mApplicationPackage = "test";
        mApplicationVersion = "0.0.0";
        mCrashReportQuota = 0;
        mBlackboxQuota = 0;
        mFlightDataQuota = 0;
        mFlightLogQuota = 0;
        mGutmaLogQuota = 0;
        mThumbnailCacheSize = 0;
    }

    /**
     * Gets {@link OfflineSettingsMode} enum value from a string.
     *
     * @param offlineSettingsModeStr string to parse
     *
     * @return Corresponding {@link OfflineSettingsMode} enum value, or (@link OfflineSettingsMode#MODEL} if not found.
     */
    private static OfflineSettingsMode offlineSettingsModeFromString(@NonNull String offlineSettingsModeStr) {
        for (OfflineSettingsMode mode : OfflineSettingsMode.values()) {
            if (mode.getKey().equals(offlineSettingsModeStr)) {
                return mode;
            }
        }
        return OfflineSettingsMode.MODEL;
    }

    /**
     * Builds a set of {@code DeviceModel} from a string array.
     * <p>
     * Each string in the array will be used to build a corresponding {@code DeviceModel} as per the underlying enum
     * name from {@link Drone.Model#name() Drone.Model} or {@link RemoteControl.Model#name() RemoteControl.Model}. <br>
     * Any unmatched string is ignored.
     * <p>
     * In case the provided array is empty, all known device models are returned from this method.
     *
     * @param models an array of device model names
     *
     * @return a set of {@code DeviceModel} corresponding to the names in the provided array, or a set containing all
     *         known device models in case the provided array is empty.
     */
    private static Set<DeviceModel> deviceModelsFromStringArray(@NonNull String[] models) {
        if (models.length > 0) {
            Set<DeviceModel> devices = new HashSet<>();
            for (String modelStr : models) {
                DeviceModel model = DeviceModels.fromName(modelStr);
                if (model == null) {
                    throw new ConfigurationError("Invalid device model name: " + modelStr);
                }
                devices.add(model);
            }
            return Collections.unmodifiableSet(devices);
        } else {
            return DeviceModels.ALL;
        }
    }

    /**
     * Tells whether an application key has been set.
     *
     * @return {@code true} if an application key has been set.
     */
    public boolean hasApplicationKey() {
        return !mApplicationKey.isEmpty();
    }

    /**
     * Gets the application key.
     *
     * @return application key, empty if no application key has been set.
     */
    @NonNull
    public String getApplicationKey() {
        return mApplicationKey;
    }

    /**
     * Tells whether support of Wifi connection is enabled.
     *
     * @return {@code true} if Wifi support is enabled, {@code false} otherwise
     */
    public boolean isWifiEnabled() {
        return mWifiEnabled;
    }

    /**
     * Tells whether support of Usb connection (i.e. SkyController) is enabled.
     *
     * @return {@code true} if Usb support is enabled, {@code false} otherwise
     */
    public boolean isUsbEnabled() {
        return mUsbEnabled;
    }

    /**
     * Tells whether support of Usb Debug Bridge connection is enabled.
     *
     * @return {@code true} if Usb Debug Bridge support is enabled, {@code false} otherwise
     */
    public boolean isUsbDebugEnabled() {
        return mUsbDebugEnabled;
    }

    /**
     * Tells whether support of BLE connection is enabled.
     *
     * @return {@code true} if Ble support is enabled, {@code false} otherwise
     */
    public boolean isBleEnabled() {
        return mBleEnabled;
    }

    /**
     * Tells whether support of dev toolbox is enabled.
     *
     * @return {@code true} if dev tool box is enabled, {@code false} otherwise
     */
    public boolean isDevToolboxEnabled() {
        return mDevToolboxEnabled;
    }

    /**
     * Tells whether support of crash report is enabled.
     *
     * @return {@code true} if crash report support is enabled, {@code false} otherwise
     */
    public boolean isCrashReportEnabled() {
        return mCrashReportEnabled;
    }

    /**
     * Tells whether firmwares synchronization is enabled.
     *
     * @return {@code true} if firmware synchronization is enabled, {@code false} otherwise
     */
    public boolean isFirmwareEnabled() {
        return mFirmwareEnabled;
    }

    /**
     * Gets alternate firmware server url.
     *
     * @return alternate firmware server url, {@code null} if there is no alternate firmware server configured.
     */
    @Nullable
    public String getAlternateFirmwareServer() {
        if (!mAlternateFirmwareServer.isEmpty()) {
            return mAlternateFirmwareServer;
        }
        return null;
    }

    /**
     * Tells whether black box synchronization is enabled.
     *
     * @return {@code true} if black box synchronization is enabled, {@code false} otherwise
     */
    public boolean isBlackBoxEnabled() {
        return mBlackBoxEnabled;
    }

    /**
     * Gets the folder in the application directory on external storage where black boxes are copied. The path to this
     * folder can be retrieved with {@link Context#getExternalFilesDir(String) getExternalFilesDir(null)}.
     *
     * @return black box public folder, {@code null} if copy is disabled
     */
    @Nullable
    public String getBlackBoxPublicFolder() {
        if (!mBlackBoxPublicFolder.isEmpty()) {
            return mBlackBoxPublicFolder;
        }
        return null;
    }

    /**
     * Tells whether GPS ephemerides synchronization is enabled.
     *
     * @return {@code true} if GPS ephemerides synchronization is enabled, {@code false} otherwise
     */
    public boolean isEphemerisSyncEnabled() {
        return mEphemeridesEnabled;
    }

    /**
     * Tells whether flight data synchronization is enabled.
     *
     * @return {@code true} if flight data synchronization is enabled, {@code false} otherwise
     */
    public boolean isFlightDataEnabled() {
        return mFlightDataEnabled;
    }

    /**
     * Tells whether support of flight log is enabled.
     *
     * @return {@code true} if flight log support is enabled, {@code false} otherwise
     */
    public boolean isFlightLogEnabled() {
        return mFlightLogEnabled;
    }

    /**
     * Tells whether GUTMA log synchronization is enabled.
     *
     * @return {@code true} if GUTMA log synchronization is enabled, {@code false} otherwise
     */
    public boolean isGutmaLogEnabled() {
        return mGutmaLogEnabled;
    }

    /**
     * Tells whether video decoding is enabled.
     *
     * @return {@code true} if video decoding is enabled, {@code false} otherwise
     */
    public boolean isVideoDecodingEnabled() {
        return mVideoDecodingEnabled;
    }

    /**
     * Tells whether auto-connection should start automatically when first session is opened.
     *
     * @return {@code true} if auto-connection should start automatically, {@code false} otherwise
     */
    public boolean shouldAutoConnectAtStartup() {
        return mAutoConnectionAtStartup;
    }

    /**
     * Tells whether wifi access point country should automatically be selected from system location.
     *
     * @return {@code true} if wifi country should be selected automatically, {@code false} otherwise
     */
    public boolean shouldAutoSelectWifiCountry() {
        return mAutoSelectWifiCountry;
    }

    /**
     * Gets default country code that will be returned by the reverse geocoder.
     *
     * @return default country code that will be returned by the reverse geocoder, {@code null} if there is no default
     *         country code.
     */
    @Nullable
    public String getReverseGeocoderDefaultCountryCode() {
        return mReverseGeocoderDefaultCountryCode.isEmpty() ? null : mReverseGeocoderDefaultCountryCode;
    }

    /**
     * Tells if and how settings are locally stored and sent to the drone during connection.
     *
     * @return current offline settings mode
     */
    @NonNull
    public OfflineSettingsMode getOfflineSettingsMode() {
        return mOfflineSettingsMode;
    }

    /**
     * Retrieves the set of supported device models.
     *
     * @return an unmodifiable set of all supported devices
     */
    @NonNull
    public Set<DeviceModel> getSupportedDevices() {
        return mSupportedDevices;
    }

    /**
     * Gets application package.
     *
     * @return application package
     */
    @NonNull
    public String getApplicationPackage() {
        return mApplicationPackage;
    }

    /**
     * Gets application version.
     *
     * @return application version
     */
    @NonNull
    public String getApplicationVersion() {
        return mApplicationVersion;
    }

    /**
     * Gets sdk package.
     *
     * @return application package
     */
    @NonNull
    public static String getSdkPackage() {
        return BuildConfig.LIBRARY_PACKAGE_NAME;
    }

    /**
     * Gets sdk version.
     *
     * @return sdk version
     */
    @NonNull
    public static String getSdkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Gives crash report storage space quota, in bytes.
     *
     * @return crash report storage space quota
     */
    @IntRange(from = 0)
    public long getCrashReportQuota() {
        return mCrashReportQuota;
    }

    /**
     * Gives black box storage space quota, in bytes.
     *
     * @return blackbox storage space quota
     */
    @IntRange(from = 0)
    public long getBlackBoxQuota() {
        return mBlackboxQuota;
    }

    /**
     * Gives flight data storage space quota, in bytes.
     *
     * @return flight data storage space quota
     */
    @IntRange(from = 0)
    public long getFlightDataQuota() {
        return mFlightDataQuota;
    }

    /**
     * Gives flight log storage space quota, in bytes.
     *
     * @return flight log storage space quota
     */
    @IntRange(from = 0)
    public long getFlightLogQuota() {
        return mFlightLogQuota;
    }

    /**
     * Gives GUTMA log storage space quota, in bytes.
     *
     * @return GUTMA log storage space quota
     */
    @IntRange(from = 0)
    public long getGutmaLogQuota() {
        return mGutmaLogQuota;
    }

    /**
     * Gives media thumbnails cache maximum allowed size, in bytes.
     *
     * @return thumbnail cache size
     */
    @IntRange(from = 0)
    public long getThumbnailCacheSize() {
        return mThumbnailCacheSize;
    }

    /**
     * Sets the application key.
     *
     * @param applicationKey application key
     */
    public void setApplicationKey(String applicationKey) {
        checkLocked();
        mApplicationKey = applicationKey;
    }

    /**
     * Enables Wifi Connection Support.
     *
     * @param enable {@code true} to enable Wifi support, {@code false} to disable it.
     */
    public void enableWifiSupport(boolean enable) {
        checkLocked();
        mWifiEnabled = enable;
    }

    /**
     * Enables Usb Connection Support (for SkyController).
     *
     * @param enable {@code true} to enable Usb support, {@code false} to disable it.
     */
    public void enableUsbSupport(boolean enable) {
        checkLocked();
        mUsbEnabled = enable;
    }

    /**
     * Enables Usb debug connection Connection Support (Using Usb Debug Bridge).
     *
     * @param enable {@code true} to enable Usb debug support, {@code false} to disable it.
     */
    public void enableUsbDebugSupport(boolean enable) {
        checkLocked();
        mUsbDebugEnabled = enable;
    }

    /**
     * Enables Ble connection Connection Support.
     *
     * @param enable {@code true} to enable Usb debug support, {@code false} to disable it.
     */
    public void enableBleSupport(boolean enable) {
        checkLocked();
        mBleEnabled = enable;
    }

    /**
     * Enables DevToolbox peripheral support.
     * <p>
     * If enabled, dev tool box will be published if the connected devices allows it, otherwise it won't be
     * published.
     *
     * @param enable {@code true} to enable the DevToolbox peripheral support, {@code false} to disable it.
     */
    public void enableDevToolboxSupport(boolean enable) {
        checkLocked();
        mDevToolboxEnabled = enable;
    }

    /**
     * Enables crash report synchronization.
     * <p>
     * If enabled, {@link CrashReporter} public facility and {@link CrashReportStorage} internal
     * utility will be published.
     *
     * @param enable {@code true} to enable crash report synchronization, {@code false} to disable it.
     * @param quota  crash report storage space quota, in bytes
     */
    public void enableCrashReportSupport(boolean enable, @IntRange(from = 0) long quota) {
        checkLocked();
        mCrashReportEnabled = enable;
        mCrashReportQuota = quota;
    }

    /**
     * Enables firmwares synchronization.
     * <p>
     * If enabled, {@link FirmwareManager} public facility and {@link FirmwareStore}, {@link FirmwareDownloader}
     * internal utilities will be published. <br>
     * On devices, {@link Updater} peripheral will be available.
     *
     * @param enable {@code true} to enable the firmware synchronization, {@code false} to disable it.
     */
    public void enableFirmwareSupport(boolean enable) {
        checkLocked();
        mFirmwareEnabled = enable;
    }

    /**
     * Enables black box synchronization.
     * <p>
     * If enabled, {@link BlackBoxReporter} public facility and {@link BlackBoxStorage} internal utility will be
     * published.
     *
     * @param enable       {@code true} to enable the black box synchronization, {@code false} to disable it.
     * @param quota        black box storage space quota, in bytes
     * @param publicFolder black box public folder
     */
    public void enableBlackBoxSupport(boolean enable, @IntRange(from = 0) long quota, @NonNull String publicFolder) {
        checkLocked();
        mBlackBoxEnabled = enable;
        mBlackboxQuota = quota;
        mBlackBoxPublicFolder = publicFolder;
    }

    /**
     * Enables GPS ephemerides synchronization.
     * <p>
     * If enabled, GPS ephemerides will be fetched from external servers and uploaded to the connected
     * drones when appropriate.
     *
     * @param enable {@code true} to enable periodic synchronization, {@code false} to disable it.
     */
    public void enableEphemerides(boolean enable) {
        checkLocked();
        mEphemeridesEnabled = enable;
    }

    /**
     * Enables flight data synchronization.
     * <p>
     * If enabled, {@link FlightDataManager} public facility and {@link FlightDataStorage} internal utility will be
     * published. {@link FlightDataDownloader} peripheral will also be published on connected devices.
     *
     * @param enable {@code true} to enable flight data synchronization, {@code false} to disable it
     * @param quota  flight data storage space quota, in bytes
     */
    public void enableFlightDataSupport(boolean enable, @IntRange(from = 0) long quota) {
        checkLocked();
        mFlightDataEnabled = enable;
        mFlightDataQuota = quota;
    }

    /**
     * Enables flight log synchronization.
     * <p>
     * If enabled, {@link FlightLogReporter} public facility and {@link FlightLogStorage} internal
     * utility will be published.
     *
     * @param enable {@code true} to enable flight log synchronization, {@code false} to disable it.
     * @param quota  flight log storage space quota, in bytes
     */
    public void enableFlightLogSupport(boolean enable, @IntRange(from = 0) long quota) {
        checkLocked();
        mFlightLogEnabled = enable;
        mFlightLogQuota = quota;
    }

    /**
     * Enables GUTMA log synchronization.
     * <p>
     * If enabled, {@link GutmaLogManager} public facility and {@link GutmaLogStorage} internal utility
     * will be published.
     *
     * @param enable {@code true} to enable GUTMA log synchronization, {@code false} to disable it
     * @param quota  GUTMA log storage space quota, in bytes
     */
    public void enableGutmaLogSupport(boolean enable, @IntRange(from = 0) long quota) {
        checkLocked();
        mGutmaLogEnabled = enable;
        mGutmaLogQuota = quota;
    }

    /**
     * Enables video decoding.
     * <p>
     * If disabled, groundsdk will never decode video streams by itself.
     *
     * @param enable {@code true} to enable video decoding, {@code false} to disable it.
     */
    public void enableVideoDecoding(boolean enable) {
        checkLocked();
        mVideoDecodingEnabled = enable;
    }

    /**
     * Configures whether auto-connection should be started automatically when the first session starts.
     *
     * @param enable {@code true} to make auto-connection start automatically when the first session start,
     *               otherwise {@code false} to disable this behavior.
     */
    public void autoConnectAtStartup(boolean enable) {
        checkLocked();
        mAutoConnectionAtStartup = enable;
    }

    /**
     * Configures whether wifi access point country should automatically be selected from system location.
     *
     * @param enable {@code true} to make wifi country be automatically selected from system location,
     *               otherwise {@code false} to disable this behavior.
     */
    public void autoSelectWifiCountry(boolean enable) {
        checkLocked();
        mAutoSelectWifiCountry = enable;
    }

    /**
     * Configures offline settings mode.
     *
     * @param mode new offline settings mode.
     */
    public void setOfflineSettingsMode(@NonNull OfflineSettingsMode mode) {
        checkLocked();
        mOfflineSettingsMode = mode;
    }

    /**
     * Configures supported device models.
     *
     * @param devices set of device models to support, or an {@link Collections#emptySet() empty set} to support all
     *                known models
     */
    public void setSupportedDevices(@NonNull Set<DeviceModel> devices) {
        if (devices.isEmpty()) {
            mSupportedDevices = DeviceModels.ALL;
        } else {
            mSupportedDevices = Collections.unmodifiableSet(new HashSet<>(devices));
        }
    }

    /**
     * Throw a runtime exception if configuration is locked.
     */
    private void checkLocked() {
        if (mLocked) {
            throw new RuntimeException("GroundSdkConfig must be set before starting the first session.");
        }
    }
}
