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

package com.parrot.drone.groundsdk.value;

import androidx.annotation.NonNull;

import java.util.EnumSet;

/**
 * Represents a setting with a current value amongst a valid set of choices.
 */
public abstract class EnumSetting<E extends Enum<E>> extends Setting {

    /**
     * Retrieves the current setting value.
     * <p>
     * Return value should be considered meaningless in case the set of {@link #getAvailableValues() available values}
     * is empty.
     *
     * @return current setting value
     */
    @NonNull
    public abstract E getValue();

    /**
     * Sets the current setting value.
     * <p>
     * The provided value must be present in the set of {@link #getAvailableValues() available values}, otherwise
     * this method does nothing.
     *
     * @param value setting value to set
     */
    public abstract void setValue(@NonNull E value);

    /**
     * Retrieves the set of available choices for the setting.
     * <p>
     * An empty set means that the whole setting is currently unsupported. <br>
     * A set containing a single value means that the setting is supported, yet not mutable by the application.
     * <p>
     * The returned set is owned by the caller and can be freely modified.
     *
     * @return the set of available values that are currently applicable to this setting
     */
    @NonNull
    public abstract EnumSet<E> getAvailableValues();
}
