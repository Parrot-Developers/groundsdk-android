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

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.camera.Camera;
import com.parrot.drone.groundsdk.value.EnumSetting;

import java.util.Collection;

/**
 * Thermal Control peripheral interface for drones.
 * <p>
 * This peripheral allows to control the drone's thermal feature.<br>
 * When thermal mode is {@link ThermalControl#mode() enabled}, the drone sends the thermal camera video stream.
 * <p>
 * Note that this peripheral may be unsupported, depending on the drone model and firmware version.
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(ThermalControl.class)}</pre>
 *
 * @see Drone#getPeripheral(Class)
 * @see Drone#getPeripheral(Class, Ref.Observer)
 */
public interface ThermalControl extends Peripheral {

    /** Thermal mode. */
    enum Mode {

        /** Thermal is disabled. */
        DISABLED,

        /** Thermal is enabled, blending on device. */
        STANDARD,

        /** Thermal is enabled, blending on drone.  */
        EMBEDDED
    }

    /**
     * Thermal camera sensitivity.
     */
    enum Sensitivity {

        /** High range (from -10 to 400 °C). */
        HIGH_RANGE,

        /** Low range (from -10 to 140 °C). */
        LOW_RANGE
    }

    /** Thermal palette. */
    interface Palette {

        /** Color for thermal palette. */
        interface Color {

            /**
             * Retrieves the red component value.
             *
             * @return the red value
             */
            @FloatRange(from = 0, to = 1)
            double getRed();

            /**
             * Retrieves the green component value.
             *
             * @return the green value
             */
            @FloatRange(from = 0, to = 1)
            double getGreen();

            /**
             * Retrieves the blue component value.
             *
             * @return the blue value
             */
            @FloatRange(from = 0, to = 1)
            double getBlue();

            /**
             * Retrieves the position in the palette where given color should be applied.
             *
             * @return the position
             */
            @FloatRange(from = 0, to = 1)
            double getPosition();
        }

        /**
         * Retrieves the palette colors.
         *
         * @return the palette colors
         */
        @NonNull
        Collection<Color> getColors();
    }

    /** Absolute thermal palette. */
    interface AbsolutePalette extends Palette {

        /**
         * Thermal palette colorization mode.
         */
        enum ColorizationMode {

            /** Use black if temperature is outside palette bounds. */
            LIMITED,

            /** Use boundaries colors if temperature is outside palette bounds. */
            EXTENDED
        }

        /**
         * Retrieves the temperature associated to the lower boundary of the palette, in Kelvin.
         *
         * @return the lowest palette temperature
         */
        double getLowestTemperature();

        /**
         * Retrieves the temperature associated to the higher boundary of the palette, in Kelvin.
         *
         * @return the highest palette temperature
         */
        double getHighestTemperature();

        /**
         * Retrieves the colorization mode outside palette bounds.
         *
         * @return the colorization mode
         */
        @NonNull
        ColorizationMode getColorizationMode();
    }

    /** Relative thermal palette. */
    interface RelativePalette extends Palette {

        /**
         * Retrieves the temperature associated to the lower boundary of the palette, in Kelvin.
         *
         * @return the lowest palette temperature
         */
        double getLowestTemperature();

        /**
         * Retrieves the temperature associated to the higher boundary of the palette, in Kelvin.
         *
         * @return the highest palette temperature
         */
        double getHighestTemperature();

        /**
         * Tells whether the palette is locked.
         *
         * @return {@code true} if the palette is locked, otherwise {@code false}
         */
        boolean isLocked();
    }

    /**
     * Spot thermal palette.
     */
    interface SpotPalette extends Palette {

        /**
         * Thermal spot palette type.
         */
        enum SpotType {

            /** Colorize only if temperature is below threshold. */
            COLD,

            /** Colorize only if temperature is above threshold. */
            HOT
        }

        /**
         * Retrieves the temperature type to highlight.
         *
         * @return the spot palette type
         */
        @NonNull
        SpotType getType();

        /**
         * Retrieves the threshold palette index for highlighting.
         *
         * @return the spot palette threshold
         */
        @FloatRange(from = 0, to = 1)
        double getThreshold();
    }

    /**
     * Thermal rendering configuration.
     */
    interface Rendering {

        /**
         * Rendering mode.
         */
        enum Mode {
            /** Visible image only. */
            VISIBLE,

            /** Thermal image only. */
            THERMAL,

            /** Blending between visible and thermal images. */
            BLENDED,

            /** Visible image in black and white. */
            MONOCHROME
        }

        /**
         * Retrieves the rendering mode.
         *
         * @return the rendering mode
         */
        @NonNull
        Mode getMode();

        /**
         * Retrieves the blending rate.
         * <p>
         * Used only in {@link Mode#BLENDED blended mode}.
         *
         * @return the blending rate
         */
        @FloatRange(from = 0, to = 1)
        double getBlendingRate();
    }

    /**
     * Gives access to the thermal mode setting.
     * <p>
     * This setting allows to change the current thermal mode. Setting it to {@link Mode#STANDARD STANDARD} will
     * {@link Camera#isActive() activate} the {@link ThermalCamera thermal camera}.
     *
     * @return thermal mode setting
     */
    @NonNull
    EnumSetting<Mode> mode();

    /**
     * Gives access to the thermal camera sensitivity setting.
     * <p>
     * This setting allows to change the current sensitivity.
     *
     * @return sensitivity setting
     */
    @NonNull
    EnumSetting<Sensitivity> sensitivity();

    /**
     * Thermal camera calibration interface.
     * <p>
     * Allows to setup thermal calibration mode, as well as to trigger manual calibration.
     */
    interface Calibration {

        /** Thermal camera calibration mode. */
        enum Mode {

            /** Calibration is managed automatically by the drone and may occur at any time, when required. */
            AUTOMATIC,

            /**
             * Calibration is never triggered automatically by the drone but only upon explicit user request.
             *
             * @see #calibrate()
             */
            MANUAL
        }

        /**
         * Gives access to the thermal camera calibration mode setting.
         * <p>
         * This setting allows to change the mode used for thermal camera calibration.
         * <p>
         * This setting remains available when the drone is not connected.
         *
         * @return calibration mode setting
         */
        @NonNull
        EnumSetting<Mode> mode();

        /**
         * Triggers thermal camera calibration.
         *
         * @return {@code true} if the calibration request was sent to the drone, otherwise {@code false}
         */
        boolean calibrate();
    }

    /**
     * Gives access to the thermal camera calibration interface.
     *
     * @return thermal camera calibration interface, or {@code null} in case the drone does not provide calibration
     *         control
     */
    @Nullable
    Calibration calibration();

    /**
     * Sends the current emissivity value to the drone.
     *
     * @param value emissivity value to send, in range [0, 1]
     */
    void sendEmissivity(@FloatRange(from = 0, to = 1) double value);

    /**
     * Sends the background temperature value to the drone.
     *
     * @param value background temperature to send, in Kelvin
     */
    void sendBackgroundTemperature(@FloatRange(from = 0) double value);

    /**
     * Sends the thermal palette configuration to the drone.
     *
     * @param palette palette configuration to send
     */
    void sendPalette(@NonNull Palette palette);

    /**
     * Sends the thermal rendering configuration to the drone.
     *
     * @param rendering thermal rendering configuration to send
     */
    void sendRendering(@NonNull Rendering rendering);
}
