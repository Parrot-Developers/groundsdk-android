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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.arsdkengine.R;
import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceComponentController;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.device.DeviceModels;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Utility class for importing application default device(s) and model preset(s) definitions.
 * <p>
 * Application provides those default by overriding gsdk_app_defaults resource file (in raw resources). <br/>
 * By default this file is empty, which means no defaults will be imported whatsoever.
 * <p>
 * gsdk_app_defaults should be formatted as follows:
 * <pre>
 * {
 *   "modelA": {
 *     "device": {
 *       "defaults": {
 *         "componentA": {
 *           "setting1": value1,
 *           "setting2": value2,
 *           ...
 *         },
 *         "componentB": {
 *           "setting1": value,
 *           ...
 *         }
 *         ...
 *       }
 *     }
 *     "preset": {
 *       "overrides": {
 *         "componentC": {
 *           "setting1": overriddenValue,
 *           ...
 *         },
 *         ...
 *       },
 *       "defaults": {
 *         "componentA": {
 *           "setting1": value1,
 *           "setting2": value2
 *           ...
 *         },
 *         "componentB": {
 *           "setting2": value,
 *           ...
 *         },
 *         ...
 *       }
 *     }
 *   },
 *   "modelB": {
 *     ...
 *   },
 *   ...
 * }
 * </pre>
 * <p>
 * 'modelX' blocks allow to define: <ul>
 * <li>a default device for that model, using a 'device' block, and/or</li>
 * <li>a default preset for that model, using a 'preset' block.</li>
 * </ul>
 * 'modelX' key should be a lower-case {@link DeviceModel model} {@link DeviceModel#name() name}.
 * <p>
 * A device or preset block allow to: <ul>
 * <li>define default components' settings values, using a 'defaults' block, and/or</li>
 * <li>override stored component's settings values, using an 'overrides' block.</li>
 * </ul>
 * 'defaults' values are merged into persistent storage if and only if no value already exists in the persistent
 * storage. <br/>
 * 'overrides' values overwrite existing values into persistent storage. A 'null' value in an overrides block clears the
 * corresponding value in the persistent store, if it exists.
 * <p>
 * A 'defaults' or 'overrides' block may contains multiple 'componentX' blocks, each addressing a specific arsdkengine
 * component; a 'componentX' block may contain multiple 'settingX' blocks, each addressing a specific setting value from
 * the enclosing component. <br/>
 * Components and setting keys are defined in arsdkengine {@link DeviceComponentController implementation classes} for
 * those components.
 * <p>
 * Application defaults are loaded, processed and merged according to the aforementioned rules into arsdkengine's
 * persistent store each time the engine is created.
 */
public class AppDefaults {

    /**
     * Imports application defaults into the given store.
     *
     * @param store persistent store to import to
     *
     * @return provided {@code store}, to allow chained calls
     */
    @NonNull
    public static PersistentStore importTo(@NonNull PersistentStore store) {
        try {
            return new AppDefaults(store).importAll();
        } catch (Exception e) {
            throw new Error("Failed to load application defaults, "
                            + "check gsdk_app_defaults[.*] file in your application raw folder", e);
        }
    }

    /** Store to import to. */
    @NonNull
    private final PersistentStore mStore;

    /** JSON declaration of application defaults. */
    @NonNull
    private final JSONObject mAppDefaults;

    /**
     * Constructor.
     *
     * @param store store to import to
     *
     * @throws IOException   in case reading application defaults resource file failed for some reason
     * @throws JSONException in case parsing application defaults resource JSON content failed for some reason
     */
    private AppDefaults(@NonNull PersistentStore store) throws IOException, JSONException {
        mStore = store;
        try (InputStream src = mStore.mContext.getResources().openRawResource(R.raw.gsdk_app_defaults)) {
            byte[] buffer = new byte[src.available()];
            //noinspection ResultOfMethodCallIgnored
            src.read(buffer);
            String data = new String(buffer, StandardCharsets.UTF_8);
            mAppDefaults = data.isEmpty() ? new JSONObject() : new JSONObject(data);
        }
    }

    /**
     * Imports application defaults to the store.
     *
     * @return the store where defaults have been imported
     *
     * @throws JSONException in case parsing the application defaults JSON content or merging failed
     */
    @VisibleForTesting
    @NonNull
    PersistentStore importAll() throws JSONException {
        Set<DeviceModel> supportedModels = GroundSdkConfig.get(mStore.mContext).getSupportedDevices();
        for (Iterator<String> iter = mAppDefaults.keys(); iter.hasNext(); ) {
            String modelKey = iter.next();
            // validate model
            DeviceModel model = DeviceModels.fromName(modelKey.toUpperCase(Locale.ROOT));
            if (model == null || !supportedModels.contains(model)) {
                throw new IllegalArgumentException("Unsupported device model: " + modelKey);
            }

            String defaultPresetKey = PersistentStore.getDefaultPresetKey(model);
            JSONObject modelDefaults = mAppDefaults.getJSONObject(modelKey);

            JSONObject deviceDefaults = modelDefaults.optJSONObject("device");
            if (deviceDefaults != null) {
                String defaultDeviceKey = PersistentStore.getDefaultDeviceKey(model);
                JSONObject mergedDevice = mergeAll(mStore.loadContent(defaultDeviceKey), deviceDefaults);
                if (mergedDevice != null) {
                    mergedDevice.put(PersistentStore.KEY_DEVICE_MODEL, model.id());
                    mergedDevice.put(PersistentStore.KEY_DEVICE_PRESET_KEY, defaultPresetKey);
                    mStore.storeContent(defaultDeviceKey, mergedDevice);
                }
            }

            JSONObject presetDefaults = modelDefaults.optJSONObject("preset");
            if (presetDefaults != null) {
                JSONObject mergedPreset = mergeAll(mStore.loadContent(defaultPresetKey), presetDefaults);
                if (mergedPreset != null) {
                    mStore.storeContent(defaultPresetKey, mergedPreset);
                }
            }
        }
        return mStore;
    }

    /**
     * Merges both application defaults and overrides declaration to a specific stored entry (device or preset).
     *
     * @param base    stored JSON to merge to, may be {@code null}
     * @param toMerge defaults and overrides JSON to merge
     *
     * @return merged JSON result
     *
     * @throws JSONException in case parsing the application defaults JSON content or merging failed
     */
    @Nullable
    private static JSONObject mergeAll(@Nullable JSONObject base, @NonNull JSONObject toMerge) throws JSONException {
        // import and apply overrides
        JSONObject overrides = toMerge.optJSONObject("overrides");
        if (overrides != null) {
            if (base == null) {
                base = new JSONObject();
            }
            merge(base, overrides, true);
        }

        // import and apply defaults
        JSONObject defaults = toMerge.optJSONObject("defaults");
        if (defaults != null) {
            if (base == null) {
                base = new JSONObject();
            }
            merge(base, defaults, false);
        }

        return base;
    }

    /**
     * Merges a single section (either defaults or overrides) of application defaults declaration to a specific stored
     * entry (device or preset).
     * <p>
     * This method is recursive.
     *
     * @param base     stored JSON to merge to
     * @param toMerge  defaults or overrides JSON to merge
     * @param override {@code true} to allow {@code toMerge} values to override {@code base} values, {@code false} to
     *                 disable this behavior
     *
     * @throws JSONException in case parsing the application defaults JSON content or merging failed
     */
    private static void merge(@NonNull JSONObject base, @NonNull JSONObject toMerge, boolean override)
            throws JSONException {
        for (Iterator<String> iter = toMerge.keys(); iter.hasNext(); ) {
            String key = iter.next();
            Object baseChild = base.opt(key);
            Object mergeChild = toMerge.get(key);
            if (baseChild instanceof JSONObject && mergeChild instanceof JSONObject) {
                merge((JSONObject) baseChild, (JSONObject) mergeChild, override);
            } else if (override || baseChild == null) {
                if (mergeChild == JSONObject.NULL) {
                    base.remove(key);
                } else {
                    base.put(key, mergeChild);
                }
            }
        }
    }

    /**
     * Constructor, for use in tests.
     *
     * @param store       store to import to
     * @param appDefaults JSON defaults declaration to import
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    AppDefaults(@NonNull PersistentStore store, @NonNull JSONObject appDefaults) {
        mStore = store;
        mAppDefaults = appDefaults;
    }
}
