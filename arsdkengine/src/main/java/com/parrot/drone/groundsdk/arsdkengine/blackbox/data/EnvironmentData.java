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

import android.location.Location;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Black box environment data sample.
 * <p>
 * Contains information such as current drone geo location, controller geo location, controller piloting command, wifi
 * signal level.
 */
public final class EnvironmentData extends TimeStampedData {

    /**
     * Allows to generate environment data samples.
     */
    public static final class Builder {

        /** Mutable environment data, serves as a base to produce immutable samples. */
        @NonNull
        private final EnvironmentData mTemplate;

        /**
         * Constructor.
         */
        public Builder() {
            mTemplate = new EnvironmentData();
        }

        /**
         * Updates current drone geo location.
         *
         * @param location drone geo location
         */
        public void setDroneLocation(@NonNull LocationInfo location) {
            if (mTemplate.mDroneLocation.update(location)) {
                mTemplate.stamp();
            }
        }

        /**
         * Updates current controller geo location.
         *
         * @param location controller geo location
         */
        public void setControllerLocation(@NonNull Location location) {
            if (mTemplate.mControllerLocation.update(location)) {
                mTemplate.stamp();
            }
        }

        /**
         * Updates current controller piloting command.
         *
         * @param roll   controller piloting command roll
         * @param pitch  controller piloting command pitch
         * @param yaw    controller piloting command yaw
         * @param gaz    controller piloting command gaz
         * @param source controller piloting command source
         */
        public void setRemotePilotingCommand(int roll, int pitch, int yaw, int gaz, int source) {
            if (mTemplate.mRemotePcmd.update(roll, pitch, yaw, gaz, source)) {
                mTemplate.stamp();
            }
        }

        /**
         * Updates current wifi signal level.
         *
         * @param rssi wifi signal level
         */
        public void setWifiSignal(int rssi) {
            if (mTemplate.mWifiSignal != rssi) {
                mTemplate.mWifiSignal = rssi;
                mTemplate.stamp();
            }
        }

        /**
         * Updates current battery voltage.
         *
         * @param voltage battery voltage
         */
        public void setBatteryVoltage(int voltage) {
            if (mTemplate.mBatteryVoltage != voltage) {
                mTemplate.mBatteryVoltage = voltage;
                mTemplate.stamp();
            }
        }

        /**
         * Generates a new sample from current data.
         *
         * @return a new immutable environment data sample.
         */
        @NonNull
        public EnvironmentData build() {
            return new EnvironmentData(mTemplate);
        }
    }


    /** Drone geo location. */
    @Expose
    @SerializedName("product_gps")
    @NonNull
    private final LocationInfo mDroneLocation;

    /** Controller geo location. */
    @Expose
    @SerializedName("device_gps")
    @NonNull
    private final LocationInfo mControllerLocation;

    /**
     * Remote control piloting command info.
     */
    private static final class RcPilotingCommand extends PilotingCommandInfo {

        /** Piloting command source. */
        @Expose
        @SerializedName("source")
        private int mSource;

        /**
         * Updates current piloting command values.
         *
         * @param roll   piloting command roll
         * @param pitch  piloting command pitch
         * @param yaw    piloting command yaw
         * @param gaz    piloting command gaz
         * @param source piloting command source
         *
         * @return {@code true} if the current piloting command changed, otherwise {@code false}
         */
        boolean update(int roll, int pitch, int yaw, int gaz, int source) {
            boolean changed = update(roll, pitch, yaw, gaz);
            if (mSource != source) {
                mSource = source;
                changed = true;
            }
            return changed;
        }

        /**
         * Default constructor.
         */
        RcPilotingCommand() {
        }

        /**
         * Copy constructor.
         *
         * @param other piloting command info to copy data from
         */
        RcPilotingCommand(@NonNull RcPilotingCommand other) {
            super(other);
            mSource = other.mSource;
        }
    }

    /** Controller piloting command. */
    @Expose
    @SerializedName("mpp_pcmd")
    @NonNull
    private final RcPilotingCommand mRemotePcmd;

    /** Wifi signal level. */
    @Expose
    @SerializedName("wifi_rssi")
    private int mWifiSignal;

    /** Battery voltage. */
    @Expose
    @SerializedName("product_battery_voltage")
    private int mBatteryVoltage;

    /**
     * Default constructor.
     */
    private EnvironmentData() {
        mDroneLocation = new LocationInfo();
        mControllerLocation = new LocationInfo();
        mRemotePcmd = new RcPilotingCommand();
    }

    /**
     * Copy constructor.
     *
     * @param other environment info to copy data from
     */
    private EnvironmentData(@NonNull EnvironmentData other) {
        super(other);
        mWifiSignal = other.mWifiSignal;
        mBatteryVoltage = other.mBatteryVoltage;
        mDroneLocation = new LocationInfo(other.mDroneLocation);
        mControllerLocation = new LocationInfo(other.mControllerLocation);
        mRemotePcmd = new RcPilotingCommand(other.mRemotePcmd);
    }
}
