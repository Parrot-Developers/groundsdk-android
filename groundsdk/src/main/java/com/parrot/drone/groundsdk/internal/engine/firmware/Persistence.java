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

package com.parrot.drone.groundsdk.internal.engine.firmware;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.parrot.drone.groundsdk.R;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.io.IoStreams;
import com.parrot.drone.groundsdk.internal.obb.Obb;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_FIRMWARE;

/**
 * Manages the persistence of firmware update data.
 */
class Persistence {

    /** Firmware store shared preferences file name. */
    @VisibleForTesting
    static final String PREF_FILE = "firmwares";

    /** Key for accessing firmware shared preferences version. Value is int. */
    private static final String PREF_KEY_VERSION = "version";

    /** Key for accessing firmware list. Value is a JSON list of firmwares. */
    private static final String PREF_KEY_FIRMWARES = "firmwares";

    /** Key for accessing firmware blacklist. Value is a JSON list of firmware versions. */
    private static final String PREF_KEY_BLACKLIST = "blacklist";

    /** Key for accessing time of last successful query of update information on remote server. Value is long. */
    private static final String PREF_KEY_LAST_REMOTE_QUERY = "remote_query_time";

    /** JSON serializer and deserializer. */
    private static final Gson GSON = new Gson();

    /** Gson generic type to deserialize firmware record list. */
    private static final Type FIRMWARE_RECORD_LIST_TYPE = new TypeToken<ArrayList<FirmwareRecord>>() {}.getType();

    /** Gson generic type to deserialize firmware blacklist from persistent storage. */
    private static final Type BLACKLIST_STORED_TYPE = new TypeToken<ArrayList<StoredBlackListEntry>>() {}.getType();

    /** Gson generic type to deserialize firmware blacklist from presets. */
    private static final Type BLACKLIST_PRESET_TYPE = new TypeToken<ArrayList<PresetBlackListEntry>>() {}.getType();

    /** Empty JSON array. */
    private static final String EMPTY_JSON_ARRAY = GSON.toJson(Collections.emptyList());

    /** Android application context. */
    @NonNull
    private final Context mContext;

    /** Local firmware directory on the user's device. */
    @NonNull
    private final File mFirmwaresDirectory;

    /** Shared preference where firmware meta data are stored. */
    @NonNull
    private final SharedPreferences mPrefs;

    /**
     * Constructor.
     *
     * @param context android application context
     */
    Persistence(@NonNull Context context) {
        mContext = context;
        mFirmwaresDirectory = new File(ApplicationStorageProvider.getInstance().getInternalAppFileCache(), "firmwares");
        mPrefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        int version = mPrefs.getInt(PREF_KEY_VERSION, 0);
        if (version == 0) {
            version = 1;
            mPrefs.edit().putInt(PREF_KEY_VERSION, version).apply();
        }
    }

    /**
     * Loads all known firmwares.
     * <p>
     * This loads stored firmware info, plus merges in preset firmwares from the application, if any.
     * Preset firmware info always overrides stored firmware info.
     *
     * @return all known firmware entries, by firmware identifier
     */
    @NonNull
    Map<FirmwareIdentifier, FirmwareStoreEntry> loadFirmwares() {
        Map<FirmwareIdentifier, FirmwareStoreEntry> entries = new HashMap<>();
        for (FirmwareStoreEntry entry : loadStoredFirmwareEntries()) {
            entries.put(entry.getFirmwareInfo().getFirmware(), entry);
        }
        // presets always override existing data
        for (FirmwareStoreEntry entry : loadPresetFirmwareEntries()) {
            entries.put(entry.getFirmwareInfo().getFirmware(), entry);
        }
        return entries;
    }

    /**
     * Persists firmware info.
     * <p>
     * The given collection of firmwares completely overrides currently persisted info. Preset firmware entries are not
     * persisted.
     *
     * @param firmwareEntries collection of firmware info entries to persist
     */
    void saveFirmwares(@NonNull Collection<FirmwareStoreEntry> firmwareEntries) {
        List<FirmwareRecord> records = new ArrayList<>();
        for (FirmwareStoreEntry entry : firmwareEntries) {
            if (!entry.isPreset()) { // don't store presets
                records.add(new FirmwareRecord(entry));
            }
        }
        mPrefs.edit().putString(PREF_KEY_FIRMWARES, GSON.toJson(records)).apply();
    }

    /**
     * Computes a path for storing a firmware update file on the local file system.
     *
     * @param firmware  firmware to store
     * @param sourceUri firmware update file source URI, used to keep proper file name/extension
     *
     * @return an appropriate path where the firmware can be stored
     */
    @NonNull
    File makeLocalFirmwarePath(@NonNull FirmwareIdentifier firmware, @NonNull URI sourceUri) {
        String name = new File(sourceUri.getPath()).getName();
        if (name.endsWith(".tgz")) { // '.tar.gz' in assets are renamed as '.tgz': recover proper extension
            name = name.substring(0, name.length() - 4) + ".tar.gz";
        }
        return new File(mFirmwaresDirectory, TextUtils.join(File.separator, Arrays.asList(
                firmware.getDeviceModel().name().toLowerCase(Locale.ROOT), firmware.getVersion().toString(), name)));
    }

    /**
     * Obtains an input stream to read a firmware update file's content.
     * <p>
     * The returned input stream will be opened lazily upon first access. As a consequence, this method
     * does not perform I/O per se and can be called from main thread. However accessing the stream should be done on a
     * background thread.
     *
     * @param firmwareUri URI of the firmware file to read.
     *
     * @return an input stream for reading the update file, if available, otherwise {@code null}
     */
    @Nullable
    InputStream getFirmwareStream(@NonNull URI firmwareUri) {
        String path = firmwareUri.getPath();
        if (!TextUtils.isEmpty(path)) switch (firmwareUri.getScheme()) {
            case Schemes.ASSET:
                return IoStreams.lazy(() -> mContext.getAssets().open(path.substring(1))); // remove heading slash
            case Schemes.OBB:
                return IoStreams.lazy(() -> Obb.openFile(mContext, path.substring(1)));  // remove heading slash
            case Schemes.FILE:
                return IoStreams.lazy(() -> new FileInputStream(firmwareUri.getPath()));
        }
        return null;
    }

    /**
     * Loads persisted firmware entries.
     *
     * @return a collection containing all persisted firmware entries.
     */
    @NonNull
    private Collection<FirmwareStoreEntry> loadStoredFirmwareEntries() {
        Collection<FirmwareRecord> records = GSON.fromJson(
                mPrefs.getString(PREF_KEY_FIRMWARES, EMPTY_JSON_ARRAY), FIRMWARE_RECORD_LIST_TYPE);
        Collection<FirmwareStoreEntry> firmwares = new ArrayList<>();

        boolean storeInvalid = false; // armed if some record is invalid

        assert records != null; // fallback is an empty JSON list, so records is never null
        for (FirmwareRecord record : records) {
            try {
                firmwares.add(record.validate(false));
            } catch (JsonParseException e) {
                storeInvalid = true;
                // drop this record
                ULog.w(TAG_FIRMWARE, "Invalid firmware record", e);
                // try to delete any associated local firmware file
                if (record.mLocalUri != null) {
                    try {
                        File firmware = new File(URI.create(record.mLocalUri));
                        if (!firmware.delete() && firmware.exists() && ULog.w(TAG_FIRMWARE)) {
                            ULog.w(TAG_FIRMWARE, "Failed to delete local firmware of invalid record: " + firmware);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }

        if (storeInvalid) {
            // save back a valid list in the store
            saveFirmwares(firmwares);
        }

        return firmwares;
    }

    /**
     * Loads preset firmware entries.
     *
     * @return a collection containing all declared preset firmware entries, empty if the application did not provide
     *         any preset declaration
     *
     * @throws Error in case the application did provide a preset declaration file and this method failed to parse it
     */
    @NonNull
    private Collection<FirmwareStoreEntry> loadPresetFirmwareEntries() {
        try (InputStream res = mContext.getResources().openRawResource(R.raw.gsdk_preset_updates)) {
            byte[] buffer = new byte[res.available()];
            //noinspection ResultOfMethodCallIgnored
            res.read(buffer);
            String json = new String(buffer, StandardCharsets.UTF_8);
            if (json.isEmpty()) {
                ULog.d(TAG_FIRMWARE, "No preset updates defined");
                return Collections.emptySet();
            }

            return GSON.<Collection<FirmwareRecord>>fromJson(json, FIRMWARE_RECORD_LIST_TYPE)
                    .stream()
                    .map(record -> record.validate(true))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new Error("Failed to load preset updates, check gsdk_preset_updates[.*] file in your application raw "
                            + "folder", e);
        }
    }

    /**
     * Loads time of last successful query on remote server to get update information.
     *
     * @return time of last successful query on remote server, in milliseconds since the epoch
     */
    long loadLastRemoteUpdateTime() {
        return mPrefs.getLong(PREF_KEY_LAST_REMOTE_QUERY, 0);
    }

    /**
     * Saves time of last successful query on remote server to get update information.
     *
     * @param time time of last successful query on remote server, in milliseconds since the epoch
     */
    void saveLastRemoteUpdateTime(long time) {
        mPrefs.edit().putLong(PREF_KEY_LAST_REMOTE_QUERY, time).apply();
    }

    /**
     * A firmware entry record, as parsed from or serialized to persistent storage.
     */
    private static final class FirmwareRecord {

        /**
         * Firmware product identifier. An hexadecimal integer formatted as a 4 character string (e.g. '0901', or
         * '090C').
         */
        @SerializedName("product")
        @Nullable
        private final String mProduct;

        /** Firmware version. A string formatted so that it can be parsed to a {@link FirmwareVersion}. */
        @SerializedName("version")
        @Nullable
        private final String mVersion;

        /**
         * URI indicating where any associated firmware update file can be found locally. Must be parsable as an {@link
         * URI}.
         */
        @SerializedName("path")
        @Nullable
        private final String mLocalUri;

        /**
         * URI indicating where any associated firmware update file can be found remotely. Must be parsable as an {@link
         * URI}.
         */
        @SerializedName("remote_url")
        @Nullable
        private final String mRemoteUri;

        /** Associated firmware file size. Must be positive. */
        @SerializedName("size")
        private final long mSize;

        /** Associated firmware file MD5 checksum. */
        @SerializedName("md5")
        @Nullable
        private final String mChecksum;

        /**
         * Special firmware flags. A list of strings that can be parsed as {@link FirmwareInfo.Attribute) by name. May
         * be lower or upper case. May not contain any {@code null} value.
         */
        @SerializedName("flags")
        @Nullable
        private final Set<String> mFlags;

        /** Minimal required device firmware version for this update to be applicable. May be {@code null}. */
        @SerializedName("required_version")
        @Nullable
        private final String mMinVersion;

        /** Maximal device firmware version onto which this update is applicable. May be {@code null}. */
        @SerializedName("max_version")
        @Nullable
        private final String mMaxVersion;

        /**
         * Constructor.
         * <p>
         * This constructor is used when serializing {@link FirmwareStoreEntry firmware entries} before being persisted.
         *
         * @param entry firmware entry to build this record from
         */
        FirmwareRecord(@NonNull FirmwareStoreEntry entry) {
            FirmwareInfoCore info = entry.getFirmwareInfo();

            FirmwareIdentifier firmware = info.getFirmware();
            mProduct = String.format("%04x", firmware.getDeviceModel().id());
            mVersion = firmware.getVersion().toString();

            URI localUri = entry.getLocalUri();
            mLocalUri = localUri == null ? null : localUri.toString();

            URI remoteUri = entry.getRemoteUri();
            mRemoteUri = remoteUri == null ? null : remoteUri.toString();

            mSize = info.getSize();
            mChecksum = info.getChecksum();
            mFlags = info.getAttributes().stream().map(FirmwareInfo.Attribute::name).collect(Collectors.toSet());

            FirmwareVersion minVersion = entry.getMinApplicableVersion();
            mMinVersion = minVersion == null ? null : minVersion.toString();

            FirmwareVersion maxVersion = entry.getMaxApplicableVersion();
            mMaxVersion = maxVersion == null ? null : maxVersion.toString();
        }

        /**
         * Validates this record and returns a corresponding {@link FirmwareStoreEntry firmware entry} from it.
         *
         * @param asPreset {@code true} to mark the returned entry as preset, {@code false} otherwise
         *
         * @return a validated firmware entry from this record
         *
         * @throws JsonParseException in case this record fails to be validated
         */
        @NonNull
        FirmwareStoreEntry validate(boolean asPreset) {
            Validation.require("product", mProduct);
            Validation.require("version", mVersion);
            FirmwareIdentifier identifier = new FirmwareIdentifier(
                    Validation.validateModel("product", mProduct), Validation.validateVersion("version", mVersion));

            URI localUri = null;
            if (mLocalUri != null) {
                localUri = Validation.validateUri("path", Schemes.LOCAL, mLocalUri);
            }

            URI remoteUri = null;
            if (mRemoteUri != null) {
                remoteUri = Validation.validateUri("remote_url", Schemes.REMOTE, mRemoteUri);
            }

            long size = Validation.validateSize("size", mSize);

            EnumSet<FirmwareInfo.Attribute> attributes = EnumSet.noneOf(FirmwareInfo.Attribute.class);
            if (mFlags != null) {
                attributes = Validation.validateAttributes("flags", mFlags, true);
            }

            FirmwareVersion minVersion = null;
            if (mMinVersion != null) {
                minVersion = Validation.validateVersion("required_version", mMinVersion);
            }

            FirmwareVersion maxVersion = null;
            if (mMaxVersion != null) {
                maxVersion = Validation.validateVersion("max_version", mMaxVersion);
            }

            return new FirmwareStoreEntry(new FirmwareInfoCore(identifier, size, mChecksum, attributes),
                    localUri, remoteUri, minVersion, maxVersion, asPreset);
        }
    }

    /**
     * Loads firmware blacklist.
     * <p>
     * This loads stored blacklist, plus merges in preset blacklist from the application, if any.
     *
     * @return all blacklisted firmware versions
     */
    @NonNull
    Set<FirmwareIdentifier> loadBlackList() {
        Set<FirmwareIdentifier> entries = loadStoredBlackListEntries();
        entries.addAll(loadPresetBlackListEntries());
        return entries;
    }

    /**
     * Persists firmware blacklist.
     * <p>
     * The given set of firmware versions completely overrides currently persisted blacklist. Preset firmware blacklist
     * entries are not persisted.
     *
     * @param blackList set of blacklisted firmware versions to persist
     */
    void saveBlackList(@NonNull Set<FirmwareIdentifier> blackList) {
        List<StoredBlackListEntry> records = new ArrayList<>();
        for (FirmwareIdentifier entry : blackList) {
            records.add(new StoredBlackListEntry(entry));
        }
        mPrefs.edit().putString(PREF_KEY_BLACKLIST, GSON.toJson(records)).apply();
    }

    /**
     * Loads persisted blacklist entries.
     *
     * @return a set containing all persisted blacklisted firmwares.
     */
    @NonNull
    private Set<FirmwareIdentifier> loadStoredBlackListEntries() {
        Collection<StoredBlackListEntry> entries = GSON.fromJson(
                mPrefs.getString(PREF_KEY_BLACKLIST, EMPTY_JSON_ARRAY), BLACKLIST_STORED_TYPE);
        Set<FirmwareIdentifier> blackList = new HashSet<>();

        boolean storeInvalid = false; // armed if some entry is invalid

        assert entries != null; // fallback is an empty JSON list, so entries is never null
        for (StoredBlackListEntry entry : entries) {
            try {
                blackList.add(entry.validate());
            } catch (JsonParseException e) {
                storeInvalid = true;
                // drop this entry
                ULog.w(TAG_FIRMWARE, "Invalid blacklist entry", e);
            }
        }

        if (storeInvalid) {
            // save back a valid list in the store
            saveBlackList(blackList);
        }

        return blackList;
    }

    /**
     * Loads preset blacklist entries.
     *
     * @return a set containing all declared preset firmware blacklist, empty if the application did not provide any
     *         preset declaration
     *
     * @throws Error in case the application did provide a preset declaration file and this method failed to parse it
     */
    @NonNull
    private Set<FirmwareIdentifier> loadPresetBlackListEntries() {
        try (InputStream res = mContext.getResources().openRawResource(R.raw.gsdk_preset_blacklist)) {
            byte[] buffer = new byte[res.available()];
            //noinspection ResultOfMethodCallIgnored
            res.read(buffer);
            String json = new String(buffer, StandardCharsets.UTF_8);
            if (json.isEmpty()) {
                ULog.d(TAG_FIRMWARE, "No preset blacklist defined");
                return Collections.emptySet();
            }

            return GSON.<Collection<PresetBlackListEntry>>fromJson(json, BLACKLIST_PRESET_TYPE)
                    .stream()
                    .flatMap(entry -> entry.validate().stream())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw new Error("Failed to load preset blacklist, check gsdk_preset_blacklist[.*] file in your application "
                            + "raw folder", e);
        }
    }

    /**
     * A firmware blacklist entry record, as parsed from or serialized to persistent storage.
     */
    private static final class StoredBlackListEntry {

        /**
         * Firmware product identifier. An hexadecimal integer formatted as a 4 character string
         * (e.g. '0901', or '090C').
         */
        @SerializedName("product")
        @Nullable
        private final String mProduct;

        /** Firmware version. A string formatted so that it can be parsed to a {@link FirmwareVersion}. */
        @SerializedName("version")
        @Nullable
        private final String mVersion;

        /**
         * Constructor.
         * <p>
         * This constructor is used when serializing {@link FirmwareIdentifier firmware blacklist entries} before being
         * persisted.
         *
         * @param firmware firmware entry to build this record from
         */
        StoredBlackListEntry(@NonNull FirmwareIdentifier firmware) {
            mProduct = String.format("%04x", firmware.getDeviceModel().id());
            mVersion = firmware.getVersion().toString();
        }

        /**
         * Validates this record and returns a corresponding {@link FirmwareIdentifier firmware entry} from it.
         *
         * @return a validated firmware entry from this record
         *
         * @throws JsonParseException in case this record fails to be validated
         */
        @NonNull
        FirmwareIdentifier validate() {
            Validation.require("product", mProduct);
            Validation.require("version", mVersion);
            return new FirmwareIdentifier(Validation.validateModel("product", mProduct),
                    Validation.validateVersion("version", mVersion));
        }
    }

    /**
     * A firmware blacklist entry, as parsed from presets.
     */
    @SuppressWarnings("unused")
    private static final class PresetBlackListEntry {

        /**
         * Firmware product identifier. An hexadecimal integer formatted as a 4 character string
         * (e.g. '0901', or '090C').
         */
        @SerializedName("product")
        @Nullable
        private String mProduct;

        /** Firmware versions. A array of strings formatted so that they can be parsed to {@link FirmwareVersion}. */
        @SerializedName("versions")
        @Nullable
        private String[] mVersions;

        /**
         * Validates this record and returns a corresponding {@link FirmwareIdentifier firmware entry} from it.
         *
         * @return a validated firmware entry from this record
         *
         * @throws JsonParseException in case this record fails to be validated
         */
        @NonNull
        Set<FirmwareIdentifier> validate() {
            Validation.require("product", mProduct);
            Validation.require("versions", mVersions);
            DeviceModel product = Validation.validateModel("product", mProduct);
            Set<FirmwareIdentifier> versions = new HashSet<>();
            for (String version : mVersions) {
                versions.add(new FirmwareIdentifier(product, Validation.validateVersion("versions", version)));
            }
            return versions;
        }
    }
}
