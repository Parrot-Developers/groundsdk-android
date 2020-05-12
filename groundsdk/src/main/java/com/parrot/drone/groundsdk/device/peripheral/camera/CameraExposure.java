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
 * Scoping class for camera exposure related types and settings.
 */
public final class CameraExposure {

    /**
     * Camera exposure mode.
     */
    public enum Mode {

        /**
         * Automatic exposure mode.
         * <p>
         * Both shutter speed and ISO sensitivity are automatically configured by the camera, with respect to some
         * manually configured maximum ISO sensitivity value.
         */
        AUTOMATIC,

        /**
         * Automatic exposure mode, prefer increasing ISO sensitivity.
         * <p>
         * Both shutter speed and ISO sensitivity are automatically configured by the camera, with respect to some
         * manually configured maximum ISO sensitivity value. Prefer increasing ISO sensitivity over using low
         * shutter speed. This mode provides better results when the drone is moving dynamically.
         */
        AUTOMATIC_PREFER_ISO_SENSITIVITY,

        /**
         * Automatic exposure mode, prefer reducing shutter speed.
         * <p>
         * Both shutter speed and ISO sensitivity are automatically configured by the camera, with respect to some
         * manually configured maximum ISO sensitivity value. Prefer reducing shutter speed over using high ISO
         * sensitivity. This mode provides better results when the when the drone is moving slowly.
         */
        AUTOMATIC_PREFER_SHUTTER_SPEED,

        /**
         * Manual ISO sensitivity mode.
         * <p>
         * Allows to configure ISO sensitivity manually. Shutter speed is automatically configured by the camera
         * accordingly.
         */
        MANUAL_ISO_SENSITIVITY,

        /**
         * Manual shutter speed mode.
         * <p>
         * Allows to configure shutter speed manually. ISO sensitivity is automatically configured by the camera
         * accordingly.
         */
        MANUAL_SHUTTER_SPEED,

        /**
         * Manual mode.
         * <p>
         * Allows to manually configure both the camera's shutter speed and the ISO sensitivity.
         */
        MANUAL
    }

    /**
     * Camera auto exposure metering mode.
     */
    public enum AutoExposureMeteringMode {

        /** Default Auto Exposure metering mode. */
        STANDARD,

        /** Auto Exposure metering mode which favours the center top of the matrix. */
        CENTER_TOP
    }

    /**
     * Camera shutter speed.
     */
    public enum ShutterSpeed {

        /** 1/10000 s. */
        ONE_OVER_10000,

        /** 1/8000 s. */
        ONE_OVER_8000,

        /** 1/6400 s. */
        ONE_OVER_6400,

        /** 1/5000 s. */
        ONE_OVER_5000,

        /** 1/4000 s. */
        ONE_OVER_4000,

        /** 1/3200 s. */
        ONE_OVER_3200,

        /** 1/2500 s. */
        ONE_OVER_2500,

        /** 1/2000 s. */
        ONE_OVER_2000,

        /** 1/1600 s. */
        ONE_OVER_1600,

        /** 1/1250 s. */
        ONE_OVER_1250,

        /** 1/1000 s. */
        ONE_OVER_1000,

        /** 1/800 s. */
        ONE_OVER_800,

        /** 1/640 s. */
        ONE_OVER_640,

        /** 1/500 s. */
        ONE_OVER_500,

        /** 1/400 s. */
        ONE_OVER_400,

        /** 1/320 s. */
        ONE_OVER_320,

        /** 1/240 s. */
        ONE_OVER_240,

        /** 1/200 s. */
        ONE_OVER_200,

        /** 1/160 s. */
        ONE_OVER_160,

        /** 1/120 s. */
        ONE_OVER_120,

        /** 1/100 s. */
        ONE_OVER_100,

        /** 1/80 s. */
        ONE_OVER_80,

        /** 1/60 s. */
        ONE_OVER_60,

        /** 1/50 s. */
        ONE_OVER_50,

        /** 1/40 s. */
        ONE_OVER_40,

        /** 1/30 s. */
        ONE_OVER_30,

        /** 1/25 s. */
        ONE_OVER_25,

        /** 1/15 s. */
        ONE_OVER_15,

        /** 1/10 s. */
        ONE_OVER_10,

        /** 1/8 s. */
        ONE_OVER_8,

        /** 1/6 s. */
        ONE_OVER_6,

        /** 1/4 s. */
        ONE_OVER_4,

        /** 1/3 s. */
        ONE_OVER_3,

        /** 1/2 s. */
        ONE_OVER_2,

        /** 1/1.5 s. */
        ONE_OVER_1_5,

        /** 1 s. */
        ONE
    }

    /**
     * Camera ISO sensitivity.
     */
    public enum IsoSensitivity {

        /** 50 ISO. */
        ISO_50,

        /** 64 ISO. */
        ISO_64,

        /** 80 ISO. */
        ISO_80,

        /** 100 ISO. */
        ISO_100,

        /** 125 ISO. */
        ISO_125,

        /** 160 ISO. */
        ISO_160,

        /** 200 ISO. */
        ISO_200,

        /** 250 ISO. */
        ISO_250,

        /** 320 ISO. */
        ISO_320,

        /** 400 ISO. */
        ISO_400,

        /** 500 ISO. */
        ISO_500,

        /** 640 ISO. */
        ISO_640,

        /** 800 ISO. */
        ISO_800,

        /** 1200 ISO. */
        ISO_1200,

        /** 1600 ISO. */
        ISO_1600,

        /** 2500 ISO. */
        ISO_2500,

        /** 3200 ISO. */
        ISO_3200,
    }

    /**
     * Camera exposure setting.
     * <p>
     * Allows to configure the exposure mode and parameters, such as: <ul>
     * <li>ISO sensitivity,</li>
     * <li>Shutter speed.</li>
     * </ul>
     */
    public abstract static class Setting extends com.parrot.drone.groundsdk.value.Setting {

        /**
         * Retrieves the currently supported exposure modes.
         * <p>
         * An empty set means that the whole setting is currently unsupported. <br>
         * A set containing a single value means that the setting is supported, yet the application is not allowed to
         * change the exposure mode.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported exposure modes
         */
        @NonNull
        public abstract EnumSet<Mode> supportedModes();

        /**
         * Retrieves the current exposure mode.
         * <p>
         * Return value should be considered meaningless in case the set of {@link #supportedModes() supported modes}
         * is empty.
         *
         * @return current exposure mode
         */
        @NonNull
        public abstract Mode mode();

        /**
         * Sets the exposure mode.
         * <p>
         * The provided value must be present in the set of {@link #supportedModes() supported modes}, otherwise
         * this method does nothing.
         *
         * @param mode mode value to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setMode(@NonNull Mode mode);

        /**
         * Retrieves the currently supported shutter speeds for use in {@link Mode#MANUAL_SHUTTER_SPEED} and
         * {@link Mode#MANUAL} modes.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported manual shutter speeds
         */
        @NonNull
        public abstract EnumSet<ShutterSpeed> supportedManualShutterSpeeds();

        /**
         * Retrieves the shutter speed value applied in {@link Mode#MANUAL_SHUTTER_SPEED} and {@link Mode#MANUAL} modes.
         * <p>
         * Return value should be considered meaningless in case the set of
         * {@link #supportedManualShutterSpeeds() supported manual shutter speeds} is empty.
         *
         * @return manual shutter speed
         */
        @NonNull
        public abstract ShutterSpeed manualShutterSpeed();

        /**
         * Sets the shutter speed value to be applied in {@link Mode#MANUAL_SHUTTER_SPEED} and {@link Mode#MANUAL}
         * modes.
         * <p>
         * The provided value must be present in the set of
         * {@link #supportedManualShutterSpeeds() supported manual shutter speeds}, otherwise this method does nothing.
         *
         * @param shutterSpeed shutter speed value to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setManualShutterSpeed(@NonNull ShutterSpeed shutterSpeed);

        /**
         * Retrieves the currently supported ISO sensitivities for use in {@link Mode#MANUAL_ISO_SENSITIVITY} and
         * {@link Mode#MANUAL} modes.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported manual ISO sensitivities
         */
        @NonNull
        public abstract EnumSet<IsoSensitivity> supportedManualIsoSensitivities();

        /**
         * Retrieves the ISO sensitivity value applied in {@link Mode#MANUAL_ISO_SENSITIVITY} and{@link Mode#MANUAL}
         * modes.
         * <p>
         * Return value should be considered meaningless in case the set of
         * {@link #supportedManualIsoSensitivities()  supported manual ISO sensitivities} is empty.
         *
         * @return manual ISO sensitivity
         */
        @NonNull
        public abstract IsoSensitivity manualIsoSensitivity();

        /**
         * Sets the ISO sensitivity value to be applied in {@link Mode#MANUAL_ISO_SENSITIVITY} and{@link Mode#MANUAL}
         * modes.
         * <p>
         * The provided value must be present in the set of
         * {@link #supportedManualIsoSensitivities() supported manual ISO sensitivities}, otherwise this method does
         * nothing.
         *
         * @param isoSensitivity ISO sensitivity value to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setManualIsoSensitivity(@NonNull IsoSensitivity isoSensitivity);

        /**
         * Retrieves the currently supported maximum ISO sensitivities for use in {@link Mode#AUTOMATIC} mode.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported maximum ISO sensitivities
         */
        @NonNull
        public abstract EnumSet<IsoSensitivity> supportedMaximumIsoSensitivities();

        /**
         * Retrieves the maximum ISO sensitivity value applied in {@link Mode#AUTOMATIC},
         * {@link Mode#AUTOMATIC_PREFER_ISO_SENSITIVITY} and {@link Mode#AUTOMATIC_PREFER_SHUTTER_SPEED} modes.
         * <p>
         * Return value should be considered meaningless in case the set of
         * {@link #supportedMaximumIsoSensitivities() supported maximum ISO sensitivities} is empty.
         *
         * @return maximum ISO sensitivity
         */
        @NonNull
        public abstract IsoSensitivity maxIsoSensitivity();

        /**
         * Sets the maximum ISO sensitivity value to be applied in {@link Mode#AUTOMATIC},
         * {@link Mode#AUTOMATIC_PREFER_ISO_SENSITIVITY} and {@link Mode#AUTOMATIC_PREFER_SHUTTER_SPEED} modes.
         * <p>
         * The provided value must be present in the set of
         * {@link #supportedMaximumIsoSensitivities() supported maximum ISO sensitivities}, otherwise this method does
         * nothing.
         *
         * @param maxIsoSensitivity maximum ISO sensitivity value to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setMaxIsoSensitivity(@NonNull IsoSensitivity maxIsoSensitivity);

        /**
         * Retrieves the currently supported auto exposure metering modes for use in {@link Mode#AUTOMATIC} mode.
         * <p>
         * The returned set is owned by the caller and can be freely modified.
         *
         * @return supported auto exposure metering modes
         */
        @NonNull
        public abstract EnumSet<AutoExposureMeteringMode> supportedAutoExposureMeteringModes();

        /**
         * Retrieves the current auto exposure metering mode.
         * <p>
         *
         * @return current auto exposure metering mode
         */
        @NonNull
        public abstract AutoExposureMeteringMode autoExposureMeteringMode();

        /**
         * Sets the auto exposure metering mode to be applied in {@link Mode#AUTOMATIC},
         * {@link Mode#AUTOMATIC_PREFER_ISO_SENSITIVITY} and {@link Mode#AUTOMATIC_PREFER_SHUTTER_SPEED} modes.
         * <p>
         *
         * @param autoExposureMeteringMode auto exposure metering mode value to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setAutoExposureMeteringMode(@NonNull AutoExposureMeteringMode autoExposureMeteringMode);

        /**
         * Switches to {@link Mode#AUTOMATIC automatic} mode and applies the given maximum ISO sensitivity value at the
         * same time.
         * <p>
         * {@link Mode#AUTOMATIC} mode must be present in the set of {@link #supportedModes() supported modes}, and the
         * provided maximum ISO sensitivity value must be present in the set of
         * {@link #supportedMaximumIsoSensitivities() supported maximum ISO sensitivities}, otherwise this method does
         * nothing.
         *
         * @param maxIsoSensitivity maximum ISO sensitivity value to set.
         */
        public abstract void setAutoMode(@NonNull IsoSensitivity maxIsoSensitivity);

        /**
         * Switches to {@link Mode#AUTOMATIC automatic} mode and applies the given auto exposure metering mode at the
         * same time.
         * <p>
         * {@link Mode#AUTOMATIC} mode must be present in the set of {@link #supportedModes() supported modes},
         * and the provided auto exposure metering mode must be present in the set of
         * {@link #supportedAutoExposureMeteringModes() supported auto exposure metering modes},
         * otherwise this method does nothing.
         *
         * @param autoExposureMeteringMode auto exposure metering mode value to set.
         */
        public abstract void setAutoMode(@NonNull AutoExposureMeteringMode autoExposureMeteringMode);

        /**
         * Switches to {@link Mode#AUTOMATIC automatic} mode and applies the given maximum ISO sensitivity and auto
         * exposure metering mode values at the same time.
         * <p>
         * {@link Mode#AUTOMATIC} mode must be present in the set of {@link #supportedModes() supported modes}, the
         * provided maximum ISO sensitivity value must be present in the set of
         * {@link #supportedMaximumIsoSensitivities() supported maximum ISO sensitivities}, and the provided
         * auto exposure metering mode must be present in the set of {@link #supportedAutoExposureMeteringModes()
         * supported auto exposure metering modes}, otherwise this method does nothing.
         *
         * @param maxIsoSensitivity         maximum ISO sensitivity value to set.
         * @param autoExposureMeteringMode  auto exposure metering mode value to set.
         */
        public abstract void setAutoMode(@NonNull IsoSensitivity maxIsoSensitivity,
                                         @NonNull AutoExposureMeteringMode autoExposureMeteringMode);

        /**
         * Switches to {@link Mode#AUTOMATIC_PREFER_SHUTTER_SPEED automatic} mode and applies the given maximum ISO
         * sensitivity value at the same time.
         * <p>
         * {@link Mode#AUTOMATIC_PREFER_SHUTTER_SPEED} mode must be present in the set of {@link #supportedModes()
         * supported modes}, and the provided maximum ISO sensitivity value must be present in the set of
         * {@link #supportedMaximumIsoSensitivities() supported maximum ISO sensitivities}, otherwise this method does
         * nothing.
         *
         * @param maxIsoSensitivity maximum ISO sensitivity value to set.
         */
        public abstract void setAutoPreferShutterSpeedMode(@NonNull IsoSensitivity maxIsoSensitivity);

        /**
         * Switches to {@link Mode#AUTOMATIC_PREFER_SHUTTER_SPEED automatic} mode and applies the given auto exposure
         * metering mode at the same time.
         * <p>
         * {@link Mode#AUTOMATIC_PREFER_SHUTTER_SPEED} mode must be present in the set of {@link #supportedModes()
         * supported modes}, and the provided auto exposure metering mode must be present in the set of
         * {@link #supportedAutoExposureMeteringModes()} () supported auto exposure metering mode},
         * otherwise this method does nothing.
         *
         * @param autoExposureMeteringMode auto exposure metering mode value to set.
         */
        public abstract void setAutoPreferShutterSpeedMode(@NonNull AutoExposureMeteringMode autoExposureMeteringMode);

        /**
         * Switches to {@link Mode#AUTOMATIC_PREFER_SHUTTER_SPEED automatic} mode and applies the given maximum ISO
         * sensitivity and auto exposure metering mode values at the same time.
         * <p>
         * {@link Mode#AUTOMATIC_PREFER_SHUTTER_SPEED} mode must be present in the set of {@link #supportedModes()
         * supported modes}, the provided maximum ISO sensitivity value must be present in the set of
         * {@link #supportedMaximumIsoSensitivities() supported maximum ISO sensitivities}, and the provided
         * auto exposure metering mode must be present in the set of {@link #supportedAutoExposureMeteringModes()
         * supported auto exposure metering modes}, otherwise this method does nothing.
         *
         * @param maxIsoSensitivity         maximum ISO sensitivity value to set.
         * @param autoExposureMeteringMode  auto exposure metering mode value to set.
         */
        public abstract void setAutoPreferShutterSpeedMode(@NonNull IsoSensitivity maxIsoSensitivity,
                                                           @NonNull AutoExposureMeteringMode autoExposureMeteringMode);

        /**
         * Switches to {@link Mode#AUTOMATIC_PREFER_ISO_SENSITIVITY automatic} mode and applies the given maximum ISO
         * sensitivity value at the same time.
         * <p>
         * {@link Mode#AUTOMATIC_PREFER_ISO_SENSITIVITY} mode must be present in the set of {@link #supportedModes()
         * supported modes}, and the provided maximum ISO sensitivity value must be present in the set of
         * {@link #supportedMaximumIsoSensitivities() supported maximum ISO sensitivities}, otherwise this method does
         * nothing.
         *
         * @param maxIsoSensitivity maximum ISO sensitivity value to set.
         */
        public abstract void setAutoPreferIsoSensitivityMode(@NonNull IsoSensitivity maxIsoSensitivity);

        /**
         * Switches to {@link Mode#AUTOMATIC_PREFER_ISO_SENSITIVITY automatic} mode and applies the given auto exposure
         * metering mode at the same time.
         * <p>
         * {@link Mode#AUTOMATIC_PREFER_ISO_SENSITIVITY} mode must be present in the set of {@link #supportedModes()
         * supported modes}, and the provided auto exposure metering mode must be present in the set of
         * {@link #supportedAutoExposureMeteringModes()} () supported auto exposure metering modes},
         * otherwise this method does nothing.
         *
         * @param autoExposureMeteringMode auto exposure metering mode to set.
         */
        public abstract void setAutoPreferIsoSensitivityMode(
                @NonNull AutoExposureMeteringMode autoExposureMeteringMode);

        /**
         * Switches to {@link Mode#AUTOMATIC_PREFER_ISO_SENSITIVITY automatic} mode and applies the given maximum ISO
         * sensitivity and auto exposure metering mode values at the same time.
         * <p>
         * {@link Mode#AUTOMATIC_PREFER_ISO_SENSITIVITY} mode must be present in the set of {@link #supportedModes()
         * supported modes}, the provided maximum ISO sensitivity value must be present in the set of
         * {@link #supportedMaximumIsoSensitivities() supported maximum ISO sensitivities}, and the provided
         * auto exposure metering mode must be present in the set of {@link #supportedAutoExposureMeteringModes()
         * supported auto exposure metering modes}, otherwise this method does nothing.
         *
         * @param maxIsoSensitivity         maximum ISO sensitivity value to set.
         * @param autoExposureMeteringMode  auto exposure metering mode to set.
         */
        public abstract void setAutoPreferIsoSensitivityMode(
                @NonNull IsoSensitivity maxIsoSensitivity,
                @NonNull AutoExposureMeteringMode autoExposureMeteringMode);

        /**
         * Switches to {@link Mode#MANUAL_SHUTTER_SPEED manual shutter speed} mode and applies the given shutter speed
         * value at the same time.
         * <p>
         * {@link Mode#MANUAL_SHUTTER_SPEED} mode must be present in the set of
         * {@link #supportedModes() supported modes}, and the provided shutter speed value must be present in the set
         * of {@link #supportedManualShutterSpeeds() supported manual shutter speeds}, otherwise this method does
         * nothing.
         *
         * @param shutterSpeed shutter speed value to set.
         */
        public abstract void setManualMode(@NonNull ShutterSpeed shutterSpeed);

        /**
         * Switches to {@link Mode#MANUAL_ISO_SENSITIVITY manual ISO sensitivity} mode and applies the given ISO
         * sensitivity value at the same time.
         * <p>
         * {@link Mode#MANUAL_ISO_SENSITIVITY} mode must be present in the set of
         * {@link #supportedModes() supported modes}, and the provided ISO sensitivity value must be present in the set
         * of {@link #supportedManualIsoSensitivities() supported manual ISO sensitivities}, otherwise this method
         * does nothing.
         *
         * @param isoSensitivity ISO sensitivity value to set.
         */
        public abstract void setManualMode(@NonNull IsoSensitivity isoSensitivity);

        /**
         * Switches to {@link Mode#MANUAL manual} mode and applies the given shutter speed and ISO sensitivity values
         * at the same time.
         * <p>
         * {@link Mode#MANUAL} mode must be present in the set of {@link #supportedModes() supported modes}, provided
         * shutter speed value must be present in the set of
         * {@link #supportedManualShutterSpeeds() supported manual shutter speeds} and provided ISO sensitivity value
         * must be present in the set of {@link #supportedManualIsoSensitivities() supported manual ISO sensitivities},
         * otherwise this method does nothing.
         *
         * @param shutterSpeed   shutter speed value to set
         * @param isoSensitivity ISO sensitivity value to set.
         */
        public abstract void setManualMode(@NonNull ShutterSpeed shutterSpeed,
                                           @NonNull IsoSensitivity isoSensitivity);
    }

    /**
     * Private constructor for static scoping class.
     */
    private CameraExposure() {
    }
}
