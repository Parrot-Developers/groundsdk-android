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

package com.parrot.drone.groundsdk.device.peripheral;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Development toolbox peripheral.
 * <p>
 * This peripheral is a debugging peripheral. It gives access to debugging settings.
 * It is accessible only if the config enables it <b>and</b> if the device publishes at least one debug setting.
 * <p>
 * To enable it through the configuration, use the boolean resource {@code gsdk_dev_toolbox_enabled}.
 */
public interface DevToolbox extends Peripheral {

    /**
     * Debug setting.
     */
    interface DebugSetting {

        /** Type of a debug setting. */
        enum Type {
            /**
             * Debug setting is a {@link BooleanDebugSetting} and {@code setting.as(BooleanDebugSetting.class)} can be
             * safely used.
             */
            BOOLEAN,
            /**
             * Debug setting is a {@link NumericDebugSetting} and {@code setting.as(NumericDebugSetting.class)} can be
             * safely used.
             */
            NUMERIC,
            /**
             * Debug setting is a {@link TextDebugSetting} and {@code setting.as(TextDebugSetting.class)} can be
             * safely used.
             */
            TEXT
        }

        /**
         * Gets the type of the setting.
         *
         * @return the type of the setting
         */
        @NonNull
        Type getType();

        /**
         * Gets the name of the setting.
         *
         * @return the name of the setting
         */
        @NonNull
        String getName();

        /**
         * Whether or not the value of this setting is read only.
         *
         * @return true if the setting is read only, false if it is writable
         */
        boolean isReadOnly();

        /**
         * Whether or not the value of this setting is currently updating (i.e. it has been changed but the device has
         * not confirmed the change yet).
         *
         * @return true if the setting is currently updating, false otherwise.
         */
        boolean isUpdating();

        /**
         * Casts the setting into the given class.
         *
         * @param settingClass the desired class.
         * @param <SETTING>    type of setting class
         *
         * @return the casted object. If the object could not be casted in the given class, an
         *         {@link IllegalArgumentException} will be issued.
         */
        <SETTING extends DebugSetting> SETTING as(@NonNull Class<SETTING> settingClass);
    }

    /** DebugSetting that has a boolean value. */
    interface BooleanDebugSetting extends DebugSetting {

        /**
         * Gets the value of the setting.
         *
         * @return the value
         */
        boolean getValue();

        /**
         * Sets the value of the setting.
         * After this call, if {@code true} is returned, {@code isUpdating()} should be true.
         *
         * @param newVal the new value
         *
         * @return true if the value will be submitted.
         */
        boolean setValue(boolean newVal);
    }

    /** DebugSetting that has a textual value. */
    interface TextDebugSetting extends DebugSetting {

        /**
         * Gets the value of the setting.
         *
         * @return the value
         */
        @NonNull
        String getValue();

        /**
         * Sets the value of the setting.
         * After this call, if {@code true} is returned, {@code isUpdating()} should be true.
         *
         * @param newVal the new value
         *
         * @return true if the value will be submitted.
         */
        boolean setValue(@NonNull String newVal);
    }

    /** DebugSetting that has a numerical value. */
    interface NumericDebugSetting extends DebugSetting {

        /**
         * Gets the value of the setting.
         *
         * @return the value
         */
        double getValue();

        /**
         * Whether or not the setting has a range.
         *
         * @return true if the value of this setting is bounded.
         */
        boolean hasRange();

        /**
         * Gets the lower bound of the range.
         * Before getting this value, you should check if the setting has a range with {@code hasRange()}
         *
         * @return the lower bound of the range
         */
        double getRangeMin();

        /**
         * Gets the upper bound of the range.
         * Before getting this value, you should check if the setting has a range with {@code hasRange()}
         *
         * @return the upper bound of the range
         */
        double getRangeMax();

        /**
         * Whether or not the setting has a step to increment or decrement the value.
         *
         * @return true if the value of this setting has a step.
         */
        boolean hasStep();

        /**
         * Gets the step of the setting.
         * Before getting this value, you should check if the setting has a step with {@code hasStep()}
         *
         * @return the step
         */
        double getStep();

        /**
         * Sets the value of the setting.
         * After this call, if {@code true} is returned, {@code isUpdating()} should be true.
         *
         * @param newVal the new value
         *
         * @return true if the value will be submitted.
         */
        boolean setValue(double newVal);
    }

    /**
     * Gets the list of the debug settings.
     *
     * @return the list of the debug settings
     */
    @NonNull
    List<DebugSetting> getDebugSettings();

    /**
     * Gets the latest debug tag id generated by the drone at reception of a debug tag.
     *
     * @return latest debug tag id, {@code null} if none
     */
    @Nullable
    String getLatestDebugTagId();

    /**
     * Sends a debug tag to the drone.
     *
     * The drone will write this tag in its debug logs. It will also write a debug tag id in its debug logs. This debug
     * tag id can be retrieved by calling {@link #getLatestDebugTagId()}.
     *
     * @param tag debug tag to send, shall be a single-line string
     */
    void sendDebugTag(@NonNull String tag);
}