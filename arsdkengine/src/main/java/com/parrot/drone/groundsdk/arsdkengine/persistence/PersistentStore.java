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

package com.parrot.drone.groundsdk.arsdkengine.persistence;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.device.DeviceModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A persistent store backed by a shared preferences file storing json data.
 */
public class PersistentStore {

    /** Json key of the device model id (int, value of {@link DeviceModel.Id} int definition). */
    public static final String KEY_DEVICE_MODEL = "model";

    /** Json key of the device name (String). */
    public static final String KEY_DEVICE_NAME = "name";

    /** Json key of the device firmware version (String). */
    public static final String KEY_DEVICE_FIRMWARE_VERSION = "firmware";

    /** Json key of the device board identifier (String). */
    public static final String KEY_DEVICE_BOARD_ID = "boardId";

    /** Json key of the device preset key (String). */
    public static final String KEY_DEVICE_PRESET_KEY = "preset";

    /** A key/value dictionary. */
    public static class Dictionary {

        /**
         * Interface allowing to observe a dictionary for changes.
         */
        public interface Observer {

            /**
             * Called when a change occurs on the dictionary.
             */
            void onChange();
        }

        /** Dictionary key. */
        @NonNull
        final String mKey;

        /** Dictionary value. can be null if the entry doesn't exists or has been cleared. */
        @Nullable
        JSONObject mJson;

        /** Dictionary containing this dictionary, null for the root dictionary. */
        @Nullable
        private final Dictionary mParent;

        /** {@code true} when the dictionary is new, i.e. it has not been persisted to the store. */
        private boolean mNew;

        /**
         * Constructor.
         *
         * @param key    dictionary key
         * @param json   dictionary content
         * @param parent parent dictionary
         */
        Dictionary(@NonNull String key, @Nullable JSONObject json, @Nullable Dictionary parent) {
            mKey = key;
            mJson = json;
            mParent = parent;
            mNew = mJson == null;
        }

        /**
         * Gets the root key for this dictionary.
         * <p>
         * For child dictionaries, this is the key where this dictionary is stored in the parent. <br/>
         * For root dictionaries, this is the key in the backing persistent storage where the dictionary is stored.
         *
         * @return the dictionary root key
         */
        @NonNull
        public final String getKey() {
            return mKey;
        }

        /**
         * Checks if this is a new dictionary that has not been saved to the store.
         *
         * @return {@code true} if the dictionary is new, {@code false} otherwise
         */
        public final boolean isNew() {
            return mNew;
        }

        /**
         * Gets all keys contained in this dictionary.
         *
         * @return a mutable set containing all keys present in the dictionary
         */
        @NonNull
        public Set<String> keys() {
            HashSet<String> keys = new HashSet<>();
            if (mJson != null) for (Iterator<String> iter = mJson.keys(); iter.hasNext(); ) {
                keys.add(iter.next());
            }
            return keys;
        }

        /**
         * Gets a String entry.
         *
         * @param key key of the requested entry
         *
         * @return entry content, null if the key is not in the dictionary
         *
         * @throws IllegalArgumentException in case the entry content could not be interpreted as a String
         */
        @Nullable
        public String getString(@NonNull String key) {
            if (mJson != null && mJson.has(key)) {
                try {
                    return mJson.getString(key);
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return null;
        }

        /**
         * Gets an Integer entry.
         *
         * @param key key of the requested entry
         *
         * @return entry content, null if the key is not in the dictionary
         *
         * @throws IllegalArgumentException in case the entry content could not be interpreted as an Integer
         */
        @Nullable
        public Integer getInt(@NonNull String key) {
            if (mJson != null && mJson.has(key)) {
                try {
                    return mJson.getInt(key);
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return null;
        }

        /**
         * Gets a Long entry.
         *
         * @param key key of the requested entry
         *
         * @return entry content, null if the key is not in the dictionary
         *
         * @throws IllegalArgumentException in case the entry content could not be interpreted as a Long
         */
        @Nullable
        public Long getLong(@NonNull String key) {
            if (mJson != null && mJson.has(key)) {
                try {
                    return mJson.getLong(key);
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return null;
        }

        /**
         * Gets a Double entry.
         *
         * @param key key of the requested entry
         *
         * @return entry content, null if the key is not in the dictionary
         *
         * @throws IllegalArgumentException in case the entry content could not be interpreted as a Double
         */
        @Nullable
        public Double getDouble(@NonNull String key) {
            if (mJson != null && mJson.has(key)) {
                try {
                    return mJson.getDouble(key);
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return null;
        }

        /**
         * Gets a Boolean entry.
         *
         * @param key key of the requested entry
         *
         * @return entry content, null if the key is not in the dictionary
         *
         * @throws IllegalArgumentException in case the entry content could not be interpreted as a Boolean
         */
        @Nullable
        public Boolean getBoolean(@NonNull String key) {
            if (mJson != null && mJson.has(key)) {
                try {
                    return mJson.getBoolean(key);
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return null;
        }

        /**
         * Gets arbitrary json content from the store.
         *
         * @param key key of the requested entry
         *
         * @return entry content, {@code null} if the key is not in the dictionary
         *
         * @throws IllegalArgumentException in case the entry content could not be interpreted as json
         */
        @Nullable
        Object getObject(@NonNull String key) {
            if (mJson != null && mJson.has(key)) {
                try {
                    return mJson.get(key);
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return null;
        }

        /**
         * Gets an entry containing a sub dictionary.
         * <p>
         * <strong>NOTE:</strong> this method always return a new Dictionary instance wrapping the content.
         *
         * @param key key of the requested entry
         *
         * @return entry content, possibly an empty sub-dictionary if the key is not in this dictionary
         *
         * @throws IllegalArgumentException in case the entry content could not be interpreted as a sub dictionary
         */
        @NonNull
        public Dictionary getDictionary(@NonNull String key) {
            JSONObject content = null;
            if (mJson != null && mJson.has(key)) {
                try {
                    content = mJson.getJSONObject(key);
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return new Dictionary(key, content, this);
        }

        /**
         * Updates an entry with arbitrary json content.
         * <p>
         * The {@code value} object must be a valid json entity, see {@link JSONObject#put(String, Object)} for
         * restrictions.
         *
         * @param key   key of the entry to update
         * @param value key value, null to remove this key from the dictionary
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public Dictionary put(@NonNull String key, @Nullable Object value) {
            doUpdate(key, value);
            return this;
        }

        /**
         * Removes all content of this dictionary and remove it from its parent dictionary.
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public Dictionary clear() {
            if (mJson != null) {
                mJson = null;
                if (mParent != null) {
                    mParent.doUpdate(mKey, null);
                }
            }
            return this;
        }

        /**
         * Commits all modification made in the dictionary tree.
         */
        public void commit() {
            mNew = mJson == null;
            if (mParent != null) {
                mParent.commit();
            }
        }

        /**
         * Unregisters any observer watching this dictionary for changes.
         */
        public void unregisterObserver() {
            // no observers can be registered on child dictionaries
        }

        /**
         * Updates an entry.
         *
         * @param key   key of the entry to update
         * @param value key value, null to remove this key from the dictionary
         */
        void doUpdate(@NonNull String key, @Nullable Object value) {
            Object previousContent = mJson == null ? null : mJson.opt(key);
            if (!Objects.equals(previousContent, value)) {
                if (mJson == null) {
                    mJson = new JSONObject();
                }
                try {
                    mJson.put(key, value);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                if (mJson.length() == 0) {
                    mJson = null;
                }
                if (mParent != null) {
                    mParent.doUpdate(mKey, mJson);
                }
            }
        }
    }

    /** Shared preferences file name. */
    private static final String STORE_NAME = "arsdkenginestore";

    /** Shared preferences key for version (int). */
    private static final String KEY_VERSION = "version";

    /** shared preferences key prefix for each device (content: json as String). */
    private static final String KEY_DEVICE_PREFIX = "device-";

    /** Shared preferences key prefix for each preset (content: json as String). */
    private static final String KEY_PRESET_PREFIX = "preset-";

    /** Application context. */
    @NonNull
    final Context mContext;

    /** Shared preferences backing the preference store. */
    @NonNull
    private final SharedPreferences mPrefs;

    /** Dictionary observers, by root dictionary. */
    @NonNull
    private final Map<RootDictionary, Dictionary.Observer> mObservers;

    /**
     * Constructor.
     *
     * @param context application context
     */
    public PersistentStore(@NonNull Context context) {
        mContext = context;
        mPrefs = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE);
        mObservers = new HashMap<>();
        int version = mPrefs.getInt(KEY_VERSION, 0);
        if (version == 0) {
            version = 1;
            mPrefs.edit().putInt(KEY_VERSION, version).apply();
        }
    }

    /**
     * Gets the list of stored device uids.
     *
     * @return set of stored device uids
     */
    @NonNull
    public final Set<String> getDevicesUid() {
        Set<String> uids = new HashSet<>();
        for (String key : mPrefs.getAll().keySet()) {
            if (key.startsWith(KEY_DEVICE_PREFIX)) {
                uids.add(keyToDeviceUid(key));
            }
        }
        return uids;
    }

    /**
     * Gets a device dictionary.
     * <p>
     * Always return a new dictionary instance, possibly empty and new in case the device is not persisted yet.
     *
     * @param deviceUid uid of the device for which to retrieve a dictionary
     *
     * @return a new dictionary instance for the device
     */
    @NonNull
    public final Dictionary getDevice(@NonNull String deviceUid) {
        return new RootDictionary(deviceUidToKey(deviceUid), this);
    }

    /**
     * Retrieves the key to access the default preset for a given device model.
     *
     * @param model device model
     *
     * @return key to the default preset of the specified model
     *
     * @see #getPreset(String, Dictionary.Observer)
     */
    @NonNull
    public static String getDefaultPresetKey(@NonNull DeviceModel model) {
        return KEY_PRESET_PREFIX + model.defaultDeviceUid();
    }

    /**
     * Gets a preset dictionary.
     * <p>
     * Always return a new dictionary instance, possibly empty and new in case the preset is not persisted yet.
     * <p>
     * <strong>IMPORTANT:</strong> in case a non-null {@link Dictionary.Observer} is provided, it <strong>MUST</strong>
     * be unregistered from obtained dictionary at some point using {@link Dictionary#unregisterObserver()}.
     *
     * @param presetUid uid of the preset for which to retrieve a dictionary
     * @param observer  observer to be notified for changes in the dictionary
     *
     * @return a new dictionary instance for the preset
     *
     * @see Dictionary#unregisterObserver()
     */
    @NonNull
    public final Dictionary getPreset(@NonNull String presetUid, @Nullable Dictionary.Observer observer) {
        RootDictionary dict = new RootDictionary(presetUid, this);
        if (observer != null) {
            mObservers.put(dict, observer);
        }
        return dict;
    }

    /**
     * Retrieves the key to access the default device for a given model.
     *
     * @param model device model
     *
     * @return key to the default device of the specified model
     *
     * @see #getDevice(String)
     */
    @NonNull
    static String getDefaultDeviceKey(@NonNull DeviceModel model) {
        return KEY_DEVICE_PREFIX + model.defaultDeviceUid();
    }

    /**
     * Loads the JSON content stored at the given key in the persistent store.
     *
     * @param key the key to the content to load
     *
     * @return the content, as a JSONObject, or null if nothing it stored for that key or it could not be parsed
     */
    @Nullable
    JSONObject loadContent(@NonNull String key) {
        String content = mPrefs.getString(key, null);
        JSONObject json = null;
        if (content != null) {
            try {
                json = new JSONObject(content);
            } catch (JSONException e) {
                // ignore
            }
        }
        return json;
    }

    /**
     * Stores the given JSON content at the given key in the persistent store.
     *
     * @param key     the key where to store the content
     * @param content JSON content to store
     */
    void storeContent(@NonNull String key, @Nullable JSONObject content) {
        SharedPreferences.Editor editor = mPrefs.edit();
        if (content == null) {
            editor.remove(key);
        } else {
            editor.putString(key, content.toString());
        }
        editor.apply();
    }

    /**
     * Notifies observers that the dictionary they are registered onto has changed.
     * <p>
     * The dictionary from which the change originates is <strong>NOT</strong> notified.
     *
     * @param changedDict the dictionary that triggered the change
     */
    private void notifyDictionaryChange(@NonNull RootDictionary changedDict) {
        for (RootDictionary dict : mObservers.keySet()) {
            if (dict.mKey.equals(changedDict.mKey) && dict != changedDict) {
                //noinspection ConstantConditions: mObservers values never null
                mObservers.get(dict.reload()).onChange();
            }
        }
    }

    /**
     * Unregisters any observer registered on the given dictionary.
     *
     * @param dict the dictionary from which to unregister observers
     */
    private void unregisterDictionaryObserver(@NonNull RootDictionary dict) {
        mObservers.remove(dict);
    }

    /**
     * Gets a persistent storage key for the given device uid.
     *
     * @param uid uid of the device to obtain a storage key for
     *
     * @return a storage key for that device dictionary
     */
    @NonNull
    private static String deviceUidToKey(@NonNull String uid) {
        return KEY_DEVICE_PREFIX + uid;
    }

    /**
     * Retrieves the device uid from a given device persistent storage key.
     *
     * @param key the storage key to process
     *
     * @return the device uid for that storage key
     */
    @NonNull
    private static String keyToDeviceUid(@NonNull String key) {
        return key.substring(KEY_DEVICE_PREFIX.length());
    }

    /** Implementation of a root Dictionary backed by a persistent store. */
    private static final class RootDictionary extends Dictionary {

        /** Persistent store backing this dictionary. */
        @NonNull
        private final PersistentStore mPersistentStore;

        /** {@code true} when the dictionary has been changed. */
        private boolean mChanged;

        /**
         * Constructor.
         *
         * @param key             dictionary root key
         * @param persistentStore persistent store backing the dictionary
         */
        RootDictionary(@NonNull String key, @NonNull PersistentStore persistentStore) {
            super(key, persistentStore.loadContent(key), null);
            mPersistentStore = persistentStore;
        }

        @Override
        public void commit() {
            super.commit();
            if (mChanged) {
                mPersistentStore.storeContent(mKey, mJson);
                mChanged = false;
                mPersistentStore.notifyDictionaryChange(this);
            }
        }

        @NonNull
        @Override
        public Dictionary clear() {
            mChanged = true;
            return super.clear();
        }

        @Override
        public void unregisterObserver() {
            mPersistentStore.unregisterDictionaryObserver(this);
        }

        @Override
        protected void doUpdate(@NonNull String key, @Nullable Object value) {
            super.doUpdate(key, value);
            mChanged = true;
        }

        /**
         * Reloads dictionary content from backing persistent store.
         *
         * @return {@code this}, to allow chained calls
         */
        RootDictionary reload() {
            mJson = mPersistentStore.loadContent(mKey);
            return this;
        }
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     */
    public final void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
        if (args.isEmpty() || args.contains("--help")) {
            writer.write("\t--persistent-store: dumps the persistent store\n");
            writer.write("\t--fake: dumps drones as fake drones\n");
        } else if (args.contains("--persistent-store") || args.contains("--all")) {
            writer.write("Persistent store:\n");
            writer.write("\t" + mPrefs.getAll() + "\n");
        }
    }


    /**
     * Retrieves store content.
     * <p>
     * This method is used in tests.
     *
     * @return store content, a map of each key to the corresponding content
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @NonNull
    Map<String, ?> content() {
        return mPrefs.getAll();
    }
}
