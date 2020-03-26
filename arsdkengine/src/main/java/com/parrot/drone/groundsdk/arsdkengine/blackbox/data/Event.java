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

package com.parrot.drone.groundsdk.arsdkengine.blackbox.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.parrot.drone.sdkcore.TimeProvider;

import androidx.annotation.NonNull;

/**
 * Black box event factory.
 */
public class Event {

    /**
     * Obtains an alert state change event.
     *
     * @param state alert state
     *
     * @return alert state change event
     */
    @NonNull
    public static Event alertStateChange(int state) {
        return new IntEvent("product_alert", state);
    }

    /**
     * Obtains a hovering warning event.
     *
     * @param tooDark {@code true} if the reason is darkness, {@code false} if it's the drone height
     *
     * @return hovering warning event
     */
    @NonNull
    public static Event hoveringWarning(boolean tooDark) {
        return new StringEvent("product_hovering_warning", tooDark ? "no_gps_too_dark" : "no_gps_too_high");
    }

    /**
     * Obtains a forced landing event.
     *
     * @param reason forced landing reason
     *
     * @return forced landing event
     */
    @NonNull
    public static Event forcedLanding(int reason) {
        return new IntEvent("product_forced_landing", reason);
    }

    /**
     * Obtains a wind state change event.
     *
     * @param state wind state
     *
     * @return wind state change event
     */
    @NonNull
    public static Event windStateChange(int state) {
        return new IntEvent("product_wind", state);
    }

    /**
     * Obtains a vibration level change event.
     *
     * @param state vibration level state
     *
     * @return vibration level change event
     */
    @NonNull
    public static Event vibrationLevelChange(int state) {
        return new IntEvent("product_vibration_level", state);
    }

    /**
     * Obtains a motor error event.
     *
     * @param error motor error
     *
     * @return motor error event
     */
    @NonNull
    public static Event motorError(int error) {
        return new IntEvent("product_motor_error", error);
    }

    /**
     * Obtains a battery alert event.
     *
     * @param critical {@code true} if the alert is critical, {@code false} if it's a warning
     * @param type     alert type
     *
     * @return battery alert event
     */
    @NonNull
    public static Event batteryAlert(boolean critical, int type) {
        return new IntEvent("product_battery_" + (critical ? "critical" : "warning"), type);
    }

    /**
     * Obtains a sensor error event.
     *
     * @param sensor sensor
     *
     * @return sensor error event
     */
    @NonNull
    public static Event sensorError(int sensor) {
        return new IntEvent("product_sensor_error", sensor);
    }

    /**
     * Obtains a battery level change event.
     *
     * @param level battery level
     *
     * @return battery level change event
     */
    @NonNull
    public static Event batteryLevelChange(int level) {
        return new IntEvent("product_battery", level);
    }

    /**
     * Obtains a country change event.
     *
     * @param countryCode country code
     *
     * @return country change event
     */
    @NonNull
    public static Event countryChange(@NonNull String countryCode) {
        return new StringEvent("wifi_country", countryCode);
    }

    /**
     * Obtains a flight plan state change event.
     *
     * @param state flight plan state
     *
     * @return flight plan state change event
     */
    @NonNull
    public static Event flightPlanStateChange(int state) {
        return new IntEvent("product_fp_state", state);
    }

    /**
     * Obtains a flying state change event.
     *
     * @param state flying state
     *
     * @return flying state change event
     */
    @NonNull
    public static Event flyingStateChange(int state) {
        return new IntEvent("product_flying_state", state);
    }

    /**
     * Obtains a follow-me mode change event.
     *
     * @param mode follow-me mode
     *
     * @return follow-me mode change event
     */
    @NonNull
    public static Event followMeModeChange(int mode) {
        return new IntEvent("product_followme_state", mode);
    }

    /**
     * Obtains a gps fix change event.
     *
     * @param fix gps fix (1 if fixed, 0 if not)
     *
     * @return gps fix change event
     */
    // TODO: a boolean would be more appropriate
    @NonNull
    public static Event gpsFixChange(int fix) {
        return new IntEvent("product_gps_fix", fix);
    }

    /**
     * Obtains a home location change event.
     *
     * @param latitude  home latitude
     * @param longitude home longitude
     * @param altitude  home altitude
     *
     * @return home location change event
     */
    @NonNull
    public static Event homeLocationChange(double latitude, double longitude, double altitude) {
        return new LocationEvent("product_home", new LocationInfo(latitude, longitude, altitude));
    }

    /**
     * Obtains a landing event.
     *
     * @return landing event
     */
    @NonNull
    public static Event landing() {
        return StringEvent.LANDING;
    }

    /**
     * Obtains a remote controller button action event.
     *
     * @param action button action code
     *
     * @return remote controller button action
     */
    @NonNull
    public static Event rcButtonAction(int action) {
        return new IntEvent("mpp_button", action);
    }

    /**
     * Obtains a return-home state change event.
     *
     * @param state return-home state
     *
     * @return return-home state change event
     */
    @NonNull
    public static Event returnHomeStateChange(int state) {
        return new IntEvent("product_rth_state", state);
    }

    /**
     * Obtains a run identifier change event.
     *
     * @param id run identifier
     *
     * @return run identifier change event
     */
    @NonNull
    public static Event runIdChange(@NonNull String id) {
        return new StringEvent("product_run_id", id);
    }

    /**
     * Obtains a take-off location event.
     *
     * @param location take-off location
     *
     * @return take-off location event
     */
    @NonNull
    public static Event takeOffLocation(@NonNull LocationInfo location) {
        return new LocationEvent("product_gps_takingoff", location);
    }

    /**
     * Obtains a wifi band change event.
     *
     * @param band wifi band
     *
     * @return wifi band change event
     */
    @NonNull
    public static Event wifiBandChange(int band) {
        return new IntEvent("wifi_band", band);
    }

    /**
     * Obtains a wifi channel change event.
     *
     * @param channel wifi channel
     *
     * @return wifi channel change event
     */
    @NonNull
    public static Event wifiChannelChange(int channel) {
        return new IntEvent("wifi_channel", channel);
    }

    /** Event timestamp, in seconds. TODO: this should be long milliseconds... */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("timestamp")
    private final double mTimeStamp;

    /** Event type. */
    @SuppressWarnings("unused") // read when serialized to json
    @Expose
    @SerializedName("type")
    private final String mType;

    /**
     * Constructor.
     *
     * @param type event type
     */
    private Event(@NonNull String type) {
        mTimeStamp = TimeProvider.elapsedRealtime() / 1000.0;
        mType = type;
    }

    /**
     * An event with an integer data value.
     */
    private static final class IntEvent extends Event {

        /** Event data. */
        @SuppressWarnings("unused") // read when serialized to json
        @Expose
        @SerializedName("datas")
        private final int mValue;

        /**
         * Constructor.
         *
         * @param type  event type
         * @param value event value
         */
        IntEvent(@NonNull String type, int value) {
            super(type);
            mValue = value;
        }
    }

    /**
     * An event with a string data value.
     */
    private static final class StringEvent extends Event {

        /** Event data. */
        @SuppressWarnings("unused") // read when serialized to json
        @Expose
        @SerializedName("datas")
        @NonNull
        private final String mValue;

        /**
         * Constructor.
         *
         * @param type  event type
         * @param value event value
         */
        StringEvent(@NonNull String type, @NonNull String value) {
            super(type);
            mValue = value;
        }

        /** Landing event immutable singleton. */
        static final Event LANDING = new StringEvent("app_command", "landing");
    }

    /**
     * An event with a geo location data value.
     */
    private static final class LocationEvent extends Event {

        /** Event data. */
        @SuppressWarnings("unused") // read when serialized to json
        @Expose
        @SerializedName("datas")
        @NonNull
        private final LocationInfo mValue;

        /**
         * Constructor.
         *
         * @param type  event type
         * @param value event value
         */
        LocationEvent(@NonNull String type, @NonNull LocationInfo value) {
            super(type);
            mValue = value;
        }
    }
}
