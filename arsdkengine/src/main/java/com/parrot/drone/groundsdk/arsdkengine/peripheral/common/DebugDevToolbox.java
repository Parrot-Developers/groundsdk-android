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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.device.peripheral.DevToolbox;
import com.parrot.drone.groundsdk.internal.device.peripheral.DevToolboxCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureDebug;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.ArrayList;
import java.util.List;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG;

/** DevToolbox peripheral controller for Debug feature supporting drones. */
public class DebugDevToolbox extends DronePeripheralController {

    /** The DevToolbox peripheral for which this object is the backend. */
    @NonNull
    private final DevToolboxCore mDevToolbox;

    /** Map of debug settings indexed by their id. */
    @NonNull
    private final SparseArray<DevToolboxCore.DebugSettingCore> mSettings;

    /**
     * Constructor.
     *
     * @param droneController The drone controller that owns this component controller.
     */
    public DebugDevToolbox(@NonNull DroneController droneController) {
        super(droneController);
        mDevToolbox = new DevToolboxCore(mComponentStore, mBackend);
        mSettings = new SparseArray<>();
    }

    @Override
    protected final void onConnected() {
        // when connected, ask all debug settings
        sendCommand(ArsdkFeatureDebug.encodeGetAllSettings());
        mDevToolbox.publish();
    }

    @Override
    protected final void onDisconnected() {
        mDevToolbox.unpublish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureDebug.UID) {
            ArsdkFeatureDebug.decode(command, mDebugCallbacks);
        }
    }

    /**
     * Creates a debug setting object.
     *
     * @param id        id of the setting
     * @param label     name of the setting
     * @param type      type of the setting
     * @param mode      mode of the setting
     * @param rangeMin  lower bound of the setting (String formatted, only useful when mode is {@link
     *                  com.parrot.drone.sdkcore.arsdk.ArsdkFeatureDebug.SettingType#DECIMAL})
     * @param rangeMax  upper bound of the setting (String formatted, only useful when mode is {@link
     *                  com.parrot.drone.sdkcore.arsdk.ArsdkFeatureDebug.SettingType#DECIMAL})
     * @param rangeStep step of the setting (String formatted, only useful when mode is {@link
     *                  com.parrot.drone.sdkcore.arsdk.ArsdkFeatureDebug.SettingType#DECIMAL})
     * @param value     value of the setting (String formatted)
     *
     * @return a debug setting
     */
    private DevToolboxCore.DebugSettingCore createDebugSetting(int id, @NonNull String label,
                                                               @NonNull ArsdkFeatureDebug.SettingType type,
                                                               @NonNull ArsdkFeatureDebug.SettingMode mode,
                                                               @NonNull String rangeMin, @NonNull String rangeMax,
                                                               @NonNull String rangeStep, @NonNull String value) {
        switch (type) {
            case DECIMAL: {
                boolean hasRange = false;
                double min = 0;
                double max = 0;
                double step = -1;
                if (!rangeMin.isEmpty() && !rangeMax.isEmpty()) {
                    hasRange = true;
                    min = Double.valueOf(rangeMin);
                    max = Double.valueOf(rangeMax);
                }
                if (!rangeStep.isEmpty()) {
                    step = Double.valueOf(rangeStep);
                }
                return mDevToolbox.createDebugSetting(id, label,
                        mode == ArsdkFeatureDebug.SettingMode.READ_ONLY, Double.valueOf(value), hasRange, min, max,
                        step);
            }
            case BOOL: {
                return mDevToolbox.createDebugSetting(id, label, mode == ArsdkFeatureDebug.SettingMode.READ_ONLY,
                        value.equals("1"));
            }
            case TEXT: {
                return mDevToolbox.createDebugSetting(id, label, mode == ArsdkFeatureDebug.SettingMode.READ_ONLY,
                        value);
            }
        }
        return null;
    }

    /** Callbacks called when a command of the feature ArsdkFeatureCommon.SettingsState is decoded. */
    private final ArsdkFeatureDebug.Callback mDebugCallbacks = new ArsdkFeatureDebug.Callback() {

        @Override
        public void onSettingsInfo(int listFlags, int id, String label, @Nullable ArsdkFeatureDebug.SettingType type,
                                   @Nullable ArsdkFeatureDebug.SettingMode mode, String rangeMin, String rangeMax,
                                   String rangeStep, String value) {
            if (ArsdkFeatureGeneric.ListFlags.EMPTY.inBitField(listFlags)) {
                mSettings.clear();
                mDevToolbox.updateDebugSettings(new ArrayList<>()).notifyUpdated();
            } else {
                if (ArsdkFeatureGeneric.ListFlags.FIRST.inBitField(listFlags)) {
                    mSettings.clear();
                }

                // creates the setting if all values are correct
                if (label != null && type != null && mode != null && rangeMin != null && rangeMax != null
                    && rangeStep != null && value != null) {
                    DevToolboxCore.DebugSettingCore debugSetting = createDebugSetting(id, label, type, mode, rangeMin,
                            rangeMax, rangeStep, value);

                    if (debugSetting != null) {
                        mSettings.put(id, debugSetting);
                    }
                }

                if (ArsdkFeatureGeneric.ListFlags.LAST.inBitField(listFlags) && mSettings.size() > 0) {
                    List<DevToolbox.DebugSetting> settingsArray = new ArrayList<>(mSettings.size());
                    for (int i = 0; i < mSettings.size(); i++) {
                        settingsArray.add(mSettings.valueAt(i));
                    }
                    mDevToolbox.updateDebugSettings(settingsArray).notifyUpdated();
                }
            }
        }


        @Override
        public void onSettingsList(int id, String value) {
            DevToolboxCore.DebugSettingCore debugSetting = mSettings.get(id);
            if (debugSetting != null) {
                switch (debugSetting.getType()) {
                    case NUMERIC:
                        DevToolboxCore.NumericDebugSettingCore doubleDebugSetting = debugSetting.as(
                                DevToolboxCore.NumericDebugSettingCore.class);
                        mDevToolbox.updateDebugSettingValue(doubleDebugSetting,
                                Double.parseDouble(value)).notifyUpdated();
                        break;
                    case TEXT:
                        DevToolboxCore.TextDebugSettingCore stringDebugSetting = debugSetting.as(
                                DevToolboxCore.TextDebugSettingCore.class);
                        mDevToolbox.updateDebugSettingValue(stringDebugSetting, value).notifyUpdated();
                        break;
                    case BOOLEAN:
                        DevToolboxCore.BooleanDebugSettingCore booleanDebugSetting = debugSetting.as(
                                DevToolboxCore.BooleanDebugSettingCore.class);
                        mDevToolbox.updateDebugSettingValue(booleanDebugSetting, value.equals("1"))
                                   .notifyUpdated();
                        break;
                }
            } else {
                ULog.w(TAG, "OnSettingList: id " + id + " not known. Ignoring this setting.");
            }
        }

        @Override
        public void onTagNotify(String id) {
            if (id != null) {
                mDevToolbox.updateDebugTagId(id).notifyUpdated();
            } else if (ULog.w(TAG)) {
                ULog.w(TAG, "onTagNotify: invalid id");
            }
        }
    };

    /** Backend of DroneFinderCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final DevToolboxCore.Backend mBackend = new DevToolboxCore.Backend() {

        @Override
        public void updateDebugSetting(DevToolboxCore.DebugSettingCore setting) {
            String strValue = null;
            switch (setting.getType()) {
                case NUMERIC:
                    strValue = Double.toString(setting.as(DevToolboxCore.NumericDebugSettingCore.class).getValue());
                    break;
                case BOOLEAN:
                    strValue = setting.as(DevToolboxCore.BooleanDebugSettingCore.class).getValue() ? "1" : "0";
                    break;
                case TEXT:
                    strValue = setting.as(DevToolboxCore.TextDebugSettingCore.class).getValue();
                    break;
            }
            sendCommand(ArsdkFeatureDebug.encodeSetSetting(setting.getUid(), strValue));
        }

        @Override
        public void sendDebugTag(@NonNull String tag) {
            sendCommand(ArsdkFeatureDebug.encodeTag(tag));
        }
    };
}
