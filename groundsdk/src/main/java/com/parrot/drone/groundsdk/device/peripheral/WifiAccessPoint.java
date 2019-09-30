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

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Band;
import com.parrot.drone.groundsdk.device.peripheral.wifi.Channel;
import com.parrot.drone.groundsdk.value.EnumSetting;
import com.parrot.drone.groundsdk.value.Setting;
import com.parrot.drone.groundsdk.value.StringSetting;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * WifiScanner peripheral interface for Drone and RemoteControl devices.
 * <p>
 * Allows to configure various parameters of the device's Wifi access point, such as: <ul>
 * <li>Environment (indoor/outdoor) setup,</li>
 * <li>Country,</li>
 * <li>Channel</li>
 * <li>SSID,</li>
 * <li>Security.</li>
 * </ul>
 * This peripheral can be obtained from a {@link Peripheral.Provider peripheral providing device} (such as a drone or a
 * remote control) using:
 * <pre>{@code device.getPeripheral(WifiAccessPoint.class)}</pre>
 *
 * @see Peripheral.Provider#getPeripheral(Class)
 * @see Peripheral.Provider#getPeripheral(Class, Ref.Observer)
 */
public interface WifiAccessPoint extends Peripheral {


    /** Access point indoor/outdoor environment modes. */
    enum Environment {

        /** Wifi access point is configured for indoor use. */
        INDOOR,

        /** Wifi access point is configured for outdoor use. */
        OUTDOOR
    }

    /**
     * Retrieves the access point indoor/outdoor environment setting.
     * <p>
     * Note that altering this setting may change the set of available channels, and even result in a device
     * disconnection since the channel currently in use might not be allowed with the new environment setup.
     *
     * @return environment setting
     */
    @NonNull
    EnumSetting<Environment> environment();

    /**
     * Setting providing access to the Wifi access point country setup.
     * <p>
     * This setting uses country codes in ISO 3166-1 alpha-2 format. In order to retrieve country name from country
     * code, use:
     * <pre>new Locale("", code).getDisplayCountry()</pre>
     */
    abstract class CountrySetting extends Setting {

        /**
         * Tells whether the country has been automatically selected by the drone AND can be modified.
         *
         * @return {@code true} if the current country is the default one and can be modified
         */
        public abstract boolean isDefaultCountryUsed();

        /**
         * Retrieves the set of country codes to which the access point may be configured.
         * <p>
         * The returned country code set cannot be modified.
         *
         * @return available country code set
         */
        @NonNull
        public abstract Set<String> getAvailableCodes();

        /**
         * Gets the current country code.
         *
         * @return current country code
         */
        @NonNull
        public abstract String getCode();

        /**
         * Changes the access point country according to the given country code.
         * <p>
         * The given country code should be one of the {@link #getAvailableCodes() available codes}, otherwise the
         * country won't change.
         *
         * @param code new country code
         */
        public abstract void select(@NonNull String code);
    }

    /**
     * Retrieves the access point country setting.
     * <p>
     * Note that altering this setting may change the set of available channels, and even result in a device
     * disconnection since the channel currently in use might not be allowed with the new country setup.
     *
     * @return environment setting
     */
    @NonNull
    CountrySetting country();

    /**
     * Setting providing access to the Wifi access point channel setup.
     */
    abstract class ChannelSetting extends Setting {

        /** Wifi access point channel selection mode. */
        public enum SelectionMode {

            /** Channel has been selected manually. */
            MANUAL,

            /** Channel has been selected automatically on the 2.4 GHz band. */
            AUTO_2_4_GHZ_BAND,

            /** Channel has been selected automatically on the 5 GHz band. */
            AUTO_5_GHZ_BAND,

            /** Channel has been selected automatically on either the 2.4 or the 5 Ghz band. */
            AUTO_ANY_BAND
        }

        /**
         * Retrieves the current selection mode of the access point channel.
         *
         * @return channel selection mode
         */
        @NonNull
        public abstract SelectionMode getSelectionMode();

        /**
         * Retrieves the set of channels to which the access point may be configured.
         * <p>
         * The returned channel set cannot be modified.
         *
         * @return available channel set
         */
        @NonNull
        public abstract Set<Channel> getAvailableChannels();

        /**
         * Retrieves the access point's current channel.
         *
         * @return current channel
         */
        @NonNull
        public abstract Channel get();

        /**
         * Tells whether automatic channel selection on any frequency band is available
         * <p>
         * Some devices, for instance remote controls, don't support auto-selection.
         *
         * @return {@code true} if the application can request auto-selection on any band, otherwise {@code false}
         */
        public abstract boolean canAutoSelect();

        /**
         * Changes the access point current channel.
         * <p>
         * The channel can only be configured to one of the {@link #getAvailableChannels()} available channels}.
         *
         * @param channel new channel to use
         */
        public abstract void select(@NonNull Channel channel);

        /**
         * Requests the device to select the most appropriate channel for the access point automatically.
         * <p>
         * The device will run its auto-selection process and eventually may change the current channel.
         * <p>
         * The device will also remain in this auto-selection mode, that is, it will run auto-selection to setup
         * the channel on subsequent boots, until the application selects a channel {@link #select(Channel) manually}.
         */
        public abstract void autoSelect();

        /**
         * Tells whether automatic channel selection restricted to a given frequency band is currently applicable.
         * <p>
         * Depending on the country and environment setup, and the currently allowed channels, some auto-selection
         * modes may not be available to the application.
         * <p>
         * Also, some devices, for instance remote controls, don't support auto-selection.
         *
         * @param band frequency band to test
         *
         * @return {@code true} if the application can request auto-selection restricted on the given frequency band,
         *         otherwise {@code false}
         */
        public abstract boolean canAutoSelect(@NonNull Band band);

        /**
         * Requests the device to select the most appropriate channel in a given frequency band for the access point
         * automatically.
         * <p>
         * The device will run its auto-selection process and eventually may change the current channel.
         * <p>
         * The device will also remain in this auto-selection mode, that is, it will run auto-selection on the
         * specified frequency band to setup the channel on subsequent boots, until the application selects a channel
         * {@link #select(Channel) manually}.
         *
         * @param band frequency band to restrict auto-selection to
         */
        public abstract void autoSelect(@NonNull Band band);
    }

    /**
     * Retrieves the access point channel setting.
     * <p>
     * Note that changing the channel (either manually or through auto-selection) may result in a device disconnection.
     *
     * @return channel setting
     */
    @NonNull
    ChannelSetting channel();

    /**
     * Retrieves the access point Service Set IDentifier (SSID) setting.
     * <p>
     * Note that the device needs to be rebooted for the access point SSID to effectively change.
     *
     * @return SSID setting
     */
    @NonNull
    StringSetting ssid();

    /**
     * Setting providing access to the Wifi access point security setup.
     */
    abstract class SecuritySetting extends Setting {

        /** Wifi access point security mode. */
        public enum Mode {

            /** Access point is open and allows connection without any security check. */
            OPEN,

            /** Access point is secured using WPA2 authentication and requires a password for connection. */
            WPA2_SECURED
        }

        /**
         * Retrieves the set of supported modes.
         * <p>
         * The returned mode set cannot be modified.
         *
         * @return supported mode set
         */
        @NonNull
        public abstract Set<Mode> getSupportedModes();

        /**
         * Retrieves the access point current security mode.
         *
         * @return security mode
         */
        @NonNull
        public abstract Mode getMode();

        /**
         * Sets the security mode to {@link Mode#OPEN}, disabling any security checks.
         * <p>
         * <strong>Note:</strong> this will only apply if {@code OPEN} mode is {@link #getSupportedModes() supported}.
         */
        public abstract void open();

        /**
         * Sets the security mode to {@link Mode#WPA2_SECURED}, and secures connection to the access point using a
         * password.
         * <p>
         * Password validation is checked first, and nothing is done if password is not valid.
         * <p>
         * <strong>Note:</strong> the change only applies if {@code WPA2_SECURED} mode is {@link #getSupportedModes()
         * supported}.
         *
         * @param password password to secure the access point with
         *
         * @return {@code true} if password is valid, otherwise {@code false}
         */
        public abstract boolean secureWithWPA2(@NonNull String password);

        /** Password validating pattern. */
        private static final Pattern PASSWORD_PATTERN = Pattern.compile("[\\x20-\\x7E]{8,63}");

        /**
         * Checks wifi password validity.
         * <p>
         * A valid wifi password contains from 8 to 63 printable ASCII characters.
         *
         * @param password the password to validate
         *
         * @return {@code true} if the given password is valid, otherwise {@code false}
         */
        public static boolean isPasswordValid(@NonNull String password) {
            return PASSWORD_PATTERN.matcher(password).matches();
        }
    }

    /**
     * Retrieves the access point security setting.
     * <p>
     * Note that changing the security mode only takes effect after the device is rebooted.
     *
     * @return security setting
     */
    @NonNull
    SecuritySetting security();
}

