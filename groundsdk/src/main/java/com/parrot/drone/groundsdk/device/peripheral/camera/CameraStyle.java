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

package com.parrot.drone.groundsdk.device.peripheral.camera;

import androidx.annotation.NonNull;

import java.util.EnumSet;

/**
 * Scoping class for camera style related types and settings.
 */
public final class CameraStyle {

    /**
     * Camera image style.
     */
    public enum Style {

        /** Natural look style. */
        STANDARD,

        /** Parrot Log, produce flat and desaturated images, best for post-processing. */
        PLOG,

        /** Intense look style, providing bright colors, warm shade and high contrast. */
        INTENSE,

        /** Pastel look style, providing soft colors, cold shade and low contrast. */
        PASTEL
    }

    /**
     * Camera image style parameter.
     */
    public interface StyleParameter {

        /**
         * Gets the minimal possible value of the parameter.
         *
         * @return the minimal possible value of the parameter
         */
        int getMin();

        /**
         * Gets the maximal possible value of the parameter.
         *
         * @return the maximal possible value of parameter
         */
        int getMax();

        /**
         * Gets the current value.
         *
         * @return the current value
         */
        int getValue();

        /**
         * Sets the current parameter value.
         *
         * @param value parameter value to set
         */
        void setValue(int value);
    }

    /**
     * Camera style setting.
     * <p>
     * Allows to set active image style and customize its parameters.
     */
    public abstract static class Setting extends com.parrot.drone.groundsdk.value.Setting {

        /**
         * Retrieves the currently supported image styles.
         * <p>
         * An empty set means that the whole setting is currently unsupported. <br>
         * A set containing a single value means that the setting is supported, yet the application is not allowed to
         * change the style.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported image styles
         */
        @NonNull
        public abstract EnumSet<Style> supportedStyles();

        /**
         * Retrieves the active style.
         * <p>
         * Return value should be considered meaningless in case the set of {@link #supportedStyles() supported modes}
         * is empty.
         *
         * @return current exposure mode
         */
        @NonNull
        public abstract Style style();

        /**
         * Sets the active style.
         * <p>
         * The provided value must be present in the set of {@link #supportedStyles() supported modes}, otherwise
         * this method does nothing.
         *
         * @param style style value to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setStyle(@NonNull Style style);

        /**
         * Gets the current style saturation.
         *
         * @return current style saturation
         */
        @NonNull
        public abstract StyleParameter saturation();

        /**
         * Gets the current style contrast.
         *
         * @return current style contrast
         */
        @NonNull
        public abstract StyleParameter contrast();

        /**
         * Gets the current style sharpness.
         *
         * @return current style sharpness
         */
        @NonNull
        public abstract StyleParameter sharpness();
    }

    /**
     * Private constructor for static scoping class.
     */
    private CameraStyle() {
    }
}
