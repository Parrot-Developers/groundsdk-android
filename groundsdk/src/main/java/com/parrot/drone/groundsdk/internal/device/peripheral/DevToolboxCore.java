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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.DevToolbox;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

import java.util.Collections;
import java.util.List;

/** Core class for the DevToolbox. */
public class DevToolboxCore extends SingletonComponentCore implements DevToolbox {

    /**
     * Core implementation of a {@link DebugSetting}.
     */
    public abstract class DebugSettingCore implements DebugSetting {

        /** Unique identifier of this setting. */
        private final int mUid;

        /** Type of the setting. */
        @NonNull
        private final Type mType;

        /** Name of the setting. */
        @NonNull
        private final String mName;

        /** Whether or not the setting can be modified. */
        final boolean mIsReadOnly;

        /**
         * Whether or not the setting is updating (i.e. its value has been changed but the device has not confirmed
         * the change yet).
         */
        boolean mIsUpdating;

        /**
         * Constructor.
         *
         * @param uid        unique identifier of the setting
         * @param type       type of the setting
         * @param name       name of the setting
         * @param isReadOnly whether or not the setting is read only
         */
        DebugSettingCore(int uid, @NonNull Type type, @NonNull String name, boolean isReadOnly) {
            mUid = uid;
            mType = type;
            mName = name;
            mIsReadOnly = isReadOnly;
        }

        @NonNull
        @Override
        public Type getType() {
            return mType;
        }

        @NonNull
        @Override
        public String getName() {
            return mName;
        }

        @Override
        public boolean isReadOnly() {
            return mIsReadOnly;
        }

        @Override
        public boolean isUpdating() {
            return mIsUpdating;
        }

        @Override
        public <SETTING extends DebugSetting> SETTING as(@NonNull Class<SETTING> settingClass) {
            try {
                return settingClass.cast(this);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Setting is not a " + settingClass.getSimpleName()
                                                   + " [type: " + getType() + "]");
            }
        }

        /**
         * Gets the UID of the setting.
         *
         * @return a unique identifier of this setting.
         */
        public int getUid() {
            return mUid;
        }

        /**
         * Called by subclasses when their value has changed.
         */
        void onValueChanged() {
            mIsUpdating = true;
            onDebugSettingValueChanged(this);
        }
    }

    /** Implementation of a DebugSetting that has a numeric value. */
    public class NumericDebugSettingCore extends DebugSettingCore implements NumericDebugSetting {

        /** Whether or not the setting has a range. */
        private final boolean mHasRange;

        /** Lower bound of the range. */
        private final double mRangeMin;

        /** Upper bound of the range. */
        private final double mRangeMax;

        /** Value of the setting. */
        private double mValue;

        /** Step of the setting. Negative or null if there is no step. */
        private final double mStep;

        /**
         * Constructor.
         *
         * @param uid        unique identifier of the setting
         * @param name       name of the setting
         * @param isReadOnly whether or not the setting can be modified
         * @param value      numeric value of the setting
         * @param hasRange   whether or not the setting has a range
         * @param rangeMin   lower bound of the range
         * @param rangeMax   upper bound of the range
         * @param step       step of the setting (negative or null if no step)
         */
        NumericDebugSettingCore(int uid, @NonNull String name, boolean isReadOnly, double value,
                                boolean hasRange, double rangeMin, double rangeMax, double step) {
            super(uid, Type.NUMERIC, name, isReadOnly);
            mValue = value;
            mHasRange = hasRange;
            mRangeMin = rangeMin;
            mRangeMax = rangeMax;
            mStep = step;
        }

        @Override
        public double getValue() {
            return mValue;
        }

        @Override
        public boolean hasRange() {
            return mHasRange;
        }

        @Override
        public double getRangeMin() {
            return mRangeMin;
        }

        @Override
        public double getRangeMax() {
            return mRangeMax;
        }

        @Override
        public boolean hasStep() {
            return mStep > 0;
        }

        @Override
        public double getStep() {
            return mStep;
        }

        @Override
        public boolean setValue(double newVal) {
            if (mIsReadOnly) {
                return false;
            }

            if (Double.compare(mValue, newVal) != 0) {
                mValue = newVal;
                onValueChanged();
            }
            return true;
        }

        /**
         * Updates the value of the setting.
         * Should only be called if the update comes from the device, not the user.
         *
         * @param value the new value
         *
         * @return true if the value should be updated.
         *
         * @see NumericDebugSettingCore#setValue(double) to set the value from the user input.
         */
        boolean updateValue(double value) {
            if (mIsUpdating || Double.compare(mValue, value) != 0) {
                mIsUpdating = false;
                mValue = value;
                return true;
            }
            return false;
        }
    }

    /** Implementation of a DebugSetting that has a boolean value. */
    public class BooleanDebugSettingCore extends DebugSettingCore implements BooleanDebugSetting {

        /** Value of the setting. */
        private boolean mValue;

        /**
         * Constructor.
         *
         * @param uid        unique identifier of the setting
         * @param name       name of the setting
         * @param isReadOnly whether or not the setting can be modified
         * @param value      boolean value of the setting
         */
        BooleanDebugSettingCore(int uid, @NonNull String name, boolean isReadOnly, boolean value) {
            super(uid, Type.BOOLEAN, name, isReadOnly);
            mValue = value;
        }

        @Override
        public boolean getValue() {
            return mValue;
        }

        @Override
        public boolean setValue(boolean newVal) {
            if (mIsReadOnly) {
                return false;
            }

            if (mValue != newVal) {
                mValue = newVal;
                onValueChanged();
            }
            return true;
        }

        /**
         * Updates the value of the setting.
         * Should only be called if the update comes from the device, not the user.
         *
         * @param value the new value
         *
         * @return true if the value should be updated.
         *
         * @see BooleanDebugSettingCore#setValue(boolean) to set the value from the user input.
         */
        boolean updateValue(boolean value) {
            if (mIsUpdating || mValue != value) {
                mIsUpdating = false;
                mValue = value;
                return true;
            }
            return false;
        }
    }

    /** Implementation of a DebugSetting that has a String value. */
    public class TextDebugSettingCore extends DebugSettingCore implements TextDebugSetting {

        /** Value of the setting. */
        @NonNull
        private String mValue;

        /**
         * Constructor.
         *
         * @param uid        unique identifier of the setting
         * @param name       name of the setting
         * @param isReadOnly whether or not the setting can be modified
         * @param value      String value of the setting
         */
        TextDebugSettingCore(int uid, @NonNull String name, boolean isReadOnly, @NonNull String value) {
            super(uid, Type.TEXT, name, isReadOnly);
            mValue = value;
        }

        @NonNull
        @Override
        public String getValue() {
            return mValue;
        }

        @Override
        public boolean setValue(@NonNull String newVal) {
            if (mIsReadOnly) {
                return false;
            }

            if (!mValue.equals(newVal)) {
                mValue = newVal;
                onValueChanged();
            }
            return true;
        }

        /**
         * Updates the value of the setting.
         * Should only be called if the update comes from the device, not the user.
         *
         * @param value the new value
         *
         * @return true if the value should be updated.
         *
         * @see TextDebugSettingCore#setValue(String) to set the value from the user input.
         */
        boolean updateValue(String value) {
            if (mIsUpdating || !mValue.equals(value)) {
                mIsUpdating = false;
                mValue = value;
                return true;
            }
            return false;
        }
    }

    /** Description of {@link DevToolbox}. */
    private static final ComponentDescriptor<Peripheral, DevToolbox> DESC = ComponentDescriptor.of(DevToolbox.class);

    /** Backend of a {@link DevToolboxCore} which handles the messages. */
    public interface Backend {

        /**
         * Update the given debug setting.
         *
         * @param setting the setting to update
         */
        void updateDebugSetting(DebugSettingCore setting);

        /**
         * Sends a debug tag to the drone.
         *
         * @param tag debug tag to send
         */
        void sendDebugTag(@NonNull String tag);
    }

    /** Backend of this peripheral. */
    @NonNull
    private final Backend mBackend;

    /** List of all debug settings. */
    @NonNull
    private List<DebugSetting> mDebugSettings;

    /** Latest debug tag id generated by the drone at reception of a debug tag. */
    @Nullable
    private String mLatestDebugTagId;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public DevToolboxCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mDebugSettings = Collections.emptyList();
        mBackend = backend;
    }

    @NonNull
    @Override
    public List<DebugSetting> getDebugSettings() {
        return mDebugSettings;
    }

    @Nullable
    @Override
    public String getLatestDebugTagId() {
        return mLatestDebugTagId;
    }

    @Override
    public void sendDebugTag(@NonNull String tag) {
        mBackend.sendDebugTag(tag);
    }

    /**
     * Called when the value of a given setting has been changed by the user.
     *
     * @param setting the setting
     */
    private void onDebugSettingValueChanged(DebugSettingCore setting) {
        mChanged = true;
        notifyUpdated();

        mBackend.updateDebugSetting(setting);
    }

    //region backend methods

    /**
     * Creates a numeric debug setting.
     *
     * @param uid        unique identifier of the setting
     * @param name       name of the setting
     * @param isReadOnly whether or not the setting can be modified
     * @param value      numeric value of the setting
     * @param hasRange   whether or not the setting has a range
     * @param rangeMin   lower bound of the range
     * @param rangeMax   upper bound of the range
     * @param step       step of the setting (negative or null if no step)
     *
     * @return an instance of a numeric debug setting
     */
    @NonNull
    public NumericDebugSettingCore createDebugSetting(int uid, @NonNull String name, boolean isReadOnly, double value,
                                                      boolean hasRange, double rangeMin, double rangeMax, double step) {
        return new NumericDebugSettingCore(uid, name, isReadOnly, value, hasRange, rangeMin, rangeMax, step);
    }

    /**
     * Creates a text debug setting.
     *
     * @param uid        unique identifier of the setting
     * @param name       name of the setting
     * @param isReadOnly whether or not the setting can be modified
     * @param value      String value of the setting
     *
     * @return an instance of a text debug setting
     */
    @NonNull
    public TextDebugSettingCore createDebugSetting(int uid, @NonNull String name, boolean isReadOnly,
                                                   @NonNull String value) {
        return new TextDebugSettingCore(uid, name, isReadOnly, value);
    }

    /**
     * Creates a boolean debug setting.
     *
     * @param uid        unique identifier of the setting
     * @param name       name of the setting
     * @param isReadOnly whether or not the setting can be modified
     * @param value      boolean value of the setting
     *
     * @return an instance of a boolean debug setting
     */
    @NonNull
    public BooleanDebugSettingCore createDebugSetting(int uid, @NonNull String name, boolean isReadOnly,
                                                      boolean value) {
        return new BooleanDebugSettingCore(uid, name, isReadOnly, value);
    }

    /**
     * Updates the list of the debug settings.
     *
     * @param debugSettings the new list
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public DevToolboxCore updateDebugSettings(@NonNull List<DebugSetting> debugSettings) {
        if (mDebugSettings != debugSettings) {
            mDebugSettings = debugSettings;
            mChanged = true;
        }

        return this;
    }

    /**
     * Updates the value of a given numeric debug setting.
     *
     * @param debugSettings the debug setting
     * @param value         the new value
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public DevToolboxCore updateDebugSettingValue(@NonNull NumericDebugSettingCore debugSettings, double value) {
        if (debugSettings.updateValue(value)) {
            mChanged = true;
        }

        return this;
    }

    /**
     * Updates the value of a given text debug setting.
     *
     * @param debugSettings the debug setting
     * @param value         the new value
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public DevToolboxCore updateDebugSettingValue(@NonNull TextDebugSettingCore debugSettings, String value) {
        if (debugSettings.updateValue(value)) {
            mChanged = true;
        }

        return this;
    }

    /**
     * Updates the value of a given boolean debug setting.
     *
     * @param debugSettings the debug setting
     * @param value         the new value
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public DevToolboxCore updateDebugSettingValue(@NonNull BooleanDebugSettingCore debugSettings, boolean value) {
        if (debugSettings.updateValue(value)) {
            mChanged = true;
        }

        return this;
    }

    /**
     * Updates the latest debug tag id generated by the drone reception of a debug tag.
     *
     * @param debugTagId the new debug tag id
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public DevToolboxCore updateDebugTagId(@NonNull String debugTagId) {
        if (!debugTagId.equals(mLatestDebugTagId)) {
            mLatestDebugTagId = debugTagId;
            mChanged = true;
        }

        return this;
    }
    //endregion backend methods
}
