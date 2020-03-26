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
 * Scoping class for camera white balance related types and settings.
 */
public final class CameraWhiteBalance {

    /**
     * Camera white balance mode.
     */
    public enum Mode {

        /** White balance is automatically configured based on the current environment. */
        AUTOMATIC,

        /** Predefined white balance mode for environments lighted by candles. */
        CANDLE,

        /** Predefined white balance mode for use sunset lighted environments. */
        SUNSET,

        /** Predefined white balance mode for environments lighted by incandescent light. */
        INCANDESCENT,

        /** Predefined white balance mode for environments lighted by warm white fluorescent light. */
        WARM_WHITE_FLUORESCENT,

        /** Predefined white balance mode for environments lighted by halogen light. */
        HALOGEN,

        /** Predefined white balance mode for environments lighted by fluorescent light. */
        FLUORESCENT,

        /** Predefined white balance mode for environments lighted by cool white fluorescent light. */
        COOL_WHITE_FLUORESCENT,

        /** Predefined white balance mode for environments lighted by a flash light. */
        FLASH,

        /** Predefined white balance mode for use in day light. */
        DAYLIGHT,

        /** Predefined white balance mode for use in sunny weather. */
        SUNNY,

        /** Predefined white balance mode for use in cloudy weather. */
        CLOUDY,

        /** Predefined white balance mode for use in snowy environment. */
        SNOW,

        /** Predefined white balance mode for use in hazy environment. */
        HAZY,

        /** Predefined white balance mode for use in shaded environment. */
        SHADED,

        /** Predefined white balance mode for green foliage images. */
        GREEN_FOLIAGE,

        /** Predefined white balance mode for blue sky images. */
        BLUE_SKY,

        /** Custom white balance. White temperature can be configured manually in this mode. */
        CUSTOM
    }

    /**
     * Custom white balance temperature.
     */
    public enum Temperature {

        /** 1500 Kelvin. */
        K_1500(1500),

        /** 1750 Kelvin. */
        K_1750(1750),

        /** 2000 Kelvin. */
        K_2000(2000),

        /** 2250 Kelvin. */
        K_2250(2250),

        /** 2500 Kelvin. */
        K_2500(2500),

        /** 2750 Kelvin. */
        K_2750(2750),

        /** 3000 Kelvin. */
        K_3000(3000),

        /** 3250 Kelvin. */
        K_3250(3250),

        /** 3500 Kelvin. */
        K_3500(3500),

        /** 3750 Kelvin. */
        K_3750(3750),

        /** 4000 Kelvin. */
        K_4000(4000),

        /** 4250 Kelvin. */
        K_4250(4250),

        /** 4500 Kelvin. */
        K_4500(4500),

        /** 4750 Kelvin. */
        K_4750(4750),

        /** 5000 Kelvin. */
        K_5000(5000),

        /** 5250 Kelvin. */
        K_5250(5250),

        /** 5500 Kelvin. */
        K_5500(5500),

        /** 5750 Kelvin. */
        K_5750(5750),

        /** 6000 Kelvin. */
        K_6000(6000),

        /** 6250 Kelvin. */
        K_6250(6250),

        /** 6500 Kelvin. */
        K_6500(6500),

        /** 6750 Kelvin. */
        K_6750(6750),

        /** 7000 Kelvin. */
        K_7000(7000),

        /** 7250 Kelvin. */
        K_7250(7250),

        /** 7500 Kelvin. */
        K_7500(7500),

        /** 7750 Kelvin. */
        K_7750(7750),

        /** 8000 Kelvin. */
        K_8000(8000),

        /** 8250 Kelvin. */
        K_8250(8250),

        /** 8500 Kelvin. */
        K_8500(8500),

        /** 8750 Kelvin. */
        K_8750(8750),

        /** 9000 Kelvin. */
        K_9000(9000),

        /** 9250 Kelvin. */
        K_9250(9250),

        /** 9500 Kelvin. */
        K_9500(9500),

        /** 9750 Kelvin. */
        K_9750(9750),

        /** 10000 Kelvin. */
        K_10000(10000),

        /** 10250 Kelvin. */
        K_10250(10250),

        /** 10500 Kelvin. */
        K_10500(10500),

        /** 10750 Kelvin. */
        K_10750(10750),

        /** 11000 Kelvin. */
        K_11000(11000),

        /** 11250 Kelvin. */
        K_11250(11250),

        /** 11500 Kelvin. */
        K_11500(11500),

        /** 11750 Kelvin. */
        K_11750(11750),

        /** 12000 Kelvin. */
        K_12000(12000),

        /** 12250 Kelvin. */
        K_12250(12250),

        /** 12500 Kelvin. */
        K_12500(12500),

        /** 12750 Kelvin. */
        K_12750(12750),

        /** 13000 Kelvin. */
        K_13000(13000),

        /** 13250 Kelvin. */
        K_13250(13250),

        /** 13500 Kelvin. */
        K_13500(13500),

        /** 13750 Kelvin. */
        K_13750(13750),

        /** 14000 Kelvin. */
        K_14000(14000),

        /** 14250 Kelvin. */
        K_14250(14250),

        /** 14500 Kelvin. */
        K_14500(14500),

        /** 14750 Kelvin. */
        K_14750(14750),

        /** 15000 Kelvin. */
        K_15000(15000);

        /** Temperature value in Kelvin. */
        private final int value;

        /**
         * Constructor.
         *
         * @param value: temperature value in Kelvin.
         */
        Temperature(int value) {
            this.value = value;
        }

        /**
         * Gets the temperature value in Kelvin.
         *
         * @return temperature in Kelvin
         */
        public int getValue() {
            return value;
        }
    }

    /**
     * Camera white balance setting.
     * <p>
     * Allows to configure the white balance mode and custom temperature.
     */
    public abstract static class Setting extends com.parrot.drone.groundsdk.value.Setting {

        /**
         * Retrieves the currently supported white balance modes.
         * <p>
         * An empty set means that the whole setting is currently unsupported. <br>
         * A set containing a single value means that the setting is supported, yet the application is not allowed to
         * change the white balance mode.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported white balance modes
         */
        @NonNull
        public abstract EnumSet<Mode> supportedModes();

        /**
         * Retrieves the current white balance mode.
         * <p>
         * Return value should be considered meaningless in case the set of {@link #supportedModes() supported modes}
         * is empty.
         *
         * @return current white balance mode
         */
        @NonNull
        public abstract Mode mode();

        /**
         * Sets the white balance mode.
         * <p>
         * The provided value must be present in the set of {@link #supportedModes() supported modes}, otherwise
         * this method does nothing.
         * <p>
         * <strong>Note:</strong> setting a non-automatic mode disables white balance
         * {@link CameraWhiteBalanceLock#isLocked() lock}.
         *
         * @param mode mode value to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setMode(@NonNull Mode mode);

        /**
         * Retrieves the currently supported custom white balance temperatures for use in {@link Mode#CUSTOM} mode.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported custom white balance temperatures
         */
        @NonNull
        public abstract EnumSet<Temperature> supportedCustomTemperatures();

        /**
         * Retrieves the custom white balance temperature value applied in {@link Mode#CUSTOM} mode.
         * <p>
         * Return value should be considered meaningless in case the set of
         * {@link #supportedCustomTemperatures() supported custom white balance temperatures} is empty.
         *
         * @return custom white balance temperature
         */
        @NonNull
        public abstract Temperature customTemperature();

        /**
         * Sets the custom white balance temperature value to be applied in {@link Mode#CUSTOM} mode.
         * <p>
         * The provided value must be present in the set of
         * {@link #supportedCustomTemperatures() supported custom white balance temperatures}, otherwise this method
         * does nothing.
         *
         * @param temperature custom white balance temperature to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setCustomTemperature(@NonNull Temperature temperature);

        /**
         * Switches to {@link Mode#CUSTOM custom} mode and applies the given custom white balance temperature value at
         * the same time.
         * <p>
         * {@link Mode#CUSTOM} mode must be present in the set of {@link #supportedModes() supported modes} and the
         * provided temperature must be present in the set of
         * {@link #supportedCustomTemperatures() supported custom white balance temperatures}, otherwise this method
         * does nothing.
         * <p>
         * <strong>Note:</strong> setting custom mode disables white balance
         * {@link CameraWhiteBalanceLock#isLocked() lock}.
         *
         * @param temperature custom white balance temperature to set.
         */
        public abstract void setCustomMode(@NonNull Temperature temperature);
    }

    /**
     * Private constructor for static scoping class.
     */
    private CameraWhiteBalance() {
    }
}
