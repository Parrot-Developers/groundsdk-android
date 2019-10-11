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

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.PilotingCommand;

/**
 * Black box flight data sample.
 * <p>
 * Contains information such as current drone speed, attitude, altitude and piloting command.
 */
public final class FlightData extends TimeStampedData {

    /**
     * Allows to generate flight data samples.
     */
    public static final class Builder {

        /** Mutable flight data, serves as a base to produce immutable samples. */
        @NonNull
        private final FlightData mTemplate;

        /**
         * Constructor.
         */
        public Builder() {
            mTemplate = new FlightData();
        }

        /**
         * Updates current drone speed.
         *
         * @param speedX drone speed X component
         * @param speedY drone speed Y component
         * @param speedZ drone speed Z component
         */
        public void setSpeed(float speedX, float speedY, float speedZ) {
            if (mTemplate.mSpeed.update(speedX, speedY, speedZ)) {
                mTemplate.stamp();
            }
        }

        /**
         * Updates current drone attitude.
         *
         * @param roll  drone roll
         * @param pitch drone pitch
         * @param yaw   drone yaw
         */
        public void setAttitude(float roll, float pitch, float yaw) {
            if (mTemplate.mAttitude.update(roll, pitch, yaw)) {
                mTemplate.stamp();
            }
        }

        /**
         * Updates current drone altitude.
         *
         * @param altitude drone altitude
         */
        public void setAltitude(double altitude) {
            if (Double.compare(mTemplate.mAltitude, altitude) != 0) {
                mTemplate.mAltitude = altitude;
                mTemplate.stamp();
            }
        }

        /**
         * Updates current drone height above ground level.
         *
         * @param height drone height above ground
         */
        public void setHeightAboveGround(float height) {
            if (Double.compare(mTemplate.mHeightAboveGround, height) != 0) {
                mTemplate.mHeightAboveGround = height;
                mTemplate.stamp();
            }
        }

        /**
         * Updates current drone piloting command.
         *
         * @param pcmd drone piloting command
         */
        public void setDronePilotingCommand(@NonNull PilotingCommand pcmd) {
            if (mTemplate.mDronePcmd.update(
                    pcmd.getRoll(), pcmd.getPitch(), pcmd.getYaw(), pcmd.getGaz(), pcmd.getFlag())) {
                mTemplate.stamp();
            }
        }

        /**
         * Generates a new sample from current data.
         *
         * @return a new immutable flight data sample.
         */
        @NonNull
        public FlightData build() {
            return new FlightData(mTemplate);
        }
    }

    /**
     * Drone speed info.
     */
    private static final class SpeedInfo {

        /** Drone speed X component. */
        @Expose
        @SerializedName("vx")
        private float mX;

        /** Drone speed Y component. */
        @Expose
        @SerializedName("vy")
        private float mY;

        /** Drone speed Z component. */
        @Expose
        @SerializedName("vz")
        private float mZ;

        /**
         * Updates current drone speed values.
         *
         * @param x drone speed X component
         * @param y drone speed Y component
         * @param z drone speed Z component
         *
         * @return {@code true} if the current drone speed changed, otherwise {@code false}
         */
        boolean update(float x, float y, float z) {
            boolean changed = false;
            if (Double.compare(mX, x) != 0) {
                mX = x;
                changed = true;
            }
            if (Double.compare(mY, y) != 0) {
                mY = y;
                changed = true;
            }
            if (Double.compare(mZ, z) != 0) {
                mZ = z;
                changed = true;
            }
            return changed;
        }

        /**
         * Default constructor.
         */
        SpeedInfo() {
        }

        /**
         * Copy constructor.
         *
         * @param other speed info to copy data from
         */
        SpeedInfo(@NonNull SpeedInfo other) {
            mX = other.mX;
            mY = other.mY;
            mZ = other.mZ;
        }
    }

    /** Drone speed. */
    @Expose
    @SerializedName("product_speed")
    @NonNull
    private final SpeedInfo mSpeed;

    /**
     * Drone attitude info.
     */
    private static final class AttitudeInfo {

        /** Drone attitude roll. */
        @Expose
        @SerializedName("roll")
        private float mRoll;

        /** Drone attitude pitch. */
        @Expose
        @SerializedName("pitch")
        private float mPitch;

        /** Drone attitude yaw. */
        @Expose
        @SerializedName("yaw")
        private float mYaw;

        /**
         * Updates current drone attitude values.
         *
         * @param roll  drone attitude roll
         * @param pitch drone attitude pitch
         * @param yaw   drone attitude yaw
         *
         * @return {@code true} if the current drone attitude changed, otherwise {@code false}
         */
        boolean update(float roll, float pitch, float yaw) {
            boolean changed = false;
            if (Double.compare(mRoll, roll) != 0) {
                mRoll = roll;
                changed = true;
            }
            if (Double.compare(mPitch, pitch) != 0) {
                mPitch = pitch;
                changed = true;
            }
            if (Double.compare(mYaw, yaw) != 0) {
                mYaw = yaw;
                changed = true;
            }
            return changed;
        }

        /**
         * Default constructor.
         */
        AttitudeInfo() {
        }

        /**
         * Copy constructor.
         *
         * @param other attitude info to copy data from
         */
        AttitudeInfo(@NonNull AttitudeInfo other) {
            mRoll = other.mRoll;
            mPitch = other.mPitch;
            mYaw = other.mYaw;
        }
    }

    /** Drone attitude. */
    @Expose
    @SerializedName("product_angles")
    @NonNull
    private final AttitudeInfo mAttitude;

    /** Drone altitude. */
    @Expose
    @SerializedName("product_alt")
    private double mAltitude;

    /** Drone height above ground level. */
    @Expose
    @SerializedName("product_height_above_ground")
    private float mHeightAboveGround;

    /**
     * Drone piloting command info.
     */
    private static final class DronePilotingCommand extends PilotingCommandInfo {

        /** Piloting command flag. */
        @Expose
        @SerializedName("flag")
        private int mFlag;

        /**
         * Updates current piloting command values.
         *
         * @param roll  piloting command roll
         * @param pitch piloting command pitch
         * @param yaw   piloting command yaw
         * @param gaz   piloting command gaz
         * @param flag  piloting command flag
         *
         * @return {@code true} if the current piloting command changed, otherwise {@code false}
         */
        boolean update(int roll, int pitch, int yaw, int gaz, int flag) {
            boolean changed = update(roll, pitch, yaw, gaz);
            if (mFlag != flag) {
                mFlag = flag;
                changed = true;
            }
            return changed;
        }

        /**
         * Default constructor.
         */
        DronePilotingCommand() {
        }

        /**
         * Copy constructor.
         *
         * @param other piloting command info to copy data from
         */
        DronePilotingCommand(@NonNull DronePilotingCommand other) {
            super(other);
            mFlag = other.mFlag;
        }
    }

    /** Drone piloting command. */
    @Expose
    @SerializedName("device_pcmd")
    @NonNull
    private final DronePilotingCommand mDronePcmd;

    /**
     * Default constructor.
     */
    private FlightData() {
        mSpeed = new SpeedInfo();
        mAttitude = new AttitudeInfo();
        mDronePcmd = new DronePilotingCommand();
    }

    /**
     * Copy constructor.
     *
     * @param other flight info to copy data from
     */
    private FlightData(@NonNull FlightData other) {
        super(other);
        mSpeed = new SpeedInfo(other.mSpeed);
        mAttitude = new AttitudeInfo(other.mAttitude);
        mAltitude = other.mAltitude;
        mHeightAboveGround = other.mHeightAboveGround;
        mDronePcmd = new DronePilotingCommand(other.mDronePcmd);
    }
}
