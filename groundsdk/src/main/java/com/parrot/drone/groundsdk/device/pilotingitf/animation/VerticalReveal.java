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

package com.parrot.drone.groundsdk.device.pilotingitf.animation;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Vertical reveal animation interface.
 * <p>
 * This animation instructs the drone to start looking down, then to fly up slowly looking at the horizon. Once the
 * drone reaches its target altitude, it rotates on itself to perform an horizontal panorama.
 */
public interface VerticalReveal extends Animation {

    /**
     * Vertical reveal animation configuration class.
     * <p>
     * Allows to configure the following parameters for this animation:
     * <ul>
     * <li><strong>vertical speed:</strong> animation vertical speed, in meters per second. If
     * {@link #withVerticalSpeed(double)} speed} is not customized, then the drone will apply its own default value
     * for this parameter. </li>
     * <li><strong>vertical distance:</strong> distance the drone will fly up, in meters. If
     * {@link #withVerticalDistance vertical distance} is not customized, then the drone will apply its own default
     * value for this parameter.</li>
     * <li><strong>rotation angle:</strong> angle for the panorama the drone will perform after having flown up,
     * in degrees. If {@link #withRotationAngle rotation angle} is not customized, then the drone will apply its
     * own default value for this parameter.</li>
     * <li><strong>rotation speed:</strong> angular speed of the panorama, in degrees per second. If
     * {@link #withRotationSpeed rotation speed} is not customized, then the drone will apply its own default value
     * for this parameter. </li>
     * <li><strong>mode:</strong> animation execution {@link Mode mode}. If {@link #withMode mode} is not
     * customized, then the drone will apply its own default value for this parameter: {@link Mode#ONCE}.</li>
     * </ul>
     */
    final class Config extends Animation.Config {

        /**
         * Constructor.
         */
        public Config() {
            super(Type.VERTICAL_REVEAL);
        }

        /** {@code true} when {@link #withVerticalSpeed} has been called once. */
        private boolean mCustomVerticalSpeed;

        /** Configured custom animation vertical speed, in meters per second. */
        private double mVerticalSpeed;

        /**
         * Configures a custom animation vertical speed.
         *
         * @param speed custom animation vertical speed, in meters per second
         *
         * @return this, to allow call chaining
         */
        @NonNull
        public Config withVerticalSpeed(@FloatRange(from = 0) double speed) {
            mCustomVerticalSpeed = true;
            mVerticalSpeed = speed;
            return this;
        }

        /**
         * Tells whether animation vertical speed parameter has been customized in this configuration.
         *
         * @return {@code true} if animation vertical speed parameter has been customized, otherwise {@code false}
         */
        public boolean usesCustomVerticalSpeed() {
            return mCustomVerticalSpeed;
        }

        /**
         * Retrieves customized animation vertical speed.
         * <p>
         * Value is meaningless if {@link #withVerticalSpeed} has not been called previously.
         *
         * @return customized animation vertical speed, in meters per second
         */
        @FloatRange(from = 0)
        public double getVerticalSpeed() {
            return mVerticalSpeed;
        }

        /** {@code true} when {@link #withVerticalDistance} has been called once. */
        private boolean mCustomVerticalDistance;

        /** Configured custom animation vertical distance, in meters. */
        private double mVerticalDistance;

        /**
         * Configures a custom animation vertical distance.
         *
         * @param distance custom vertical distance, in meters
         *
         * @return this, to allow call chaining
         */
        @NonNull
        public Config withVerticalDistance(@FloatRange(from = 0) double distance) {
            mCustomVerticalDistance = true;
            mVerticalDistance = distance;
            return this;
        }

        /**
         * Tells whether animation vertical distance parameter has been customized in this configuration.
         *
         * @return {@code true} if animation vertical distance parameter has been customized, otherwise {@code false}
         */
        public boolean usesCustomVerticalDistance() {
            return mCustomVerticalDistance;
        }

        /**
         * Retrieves customized animation vertical distance.
         * <p>
         * Value is meaningless if {@link #withVerticalDistance} has not been called previously.
         *
         * @return customized animation vertical distance, in meters
         */
        @FloatRange(from = 0)
        public double getVerticalDistance() {
            return mVerticalDistance;
        }

        /** {@code true} when {@link #withRotationAngle} has been called once. */
        private boolean mCustomRotationAngle;

        /** Configured custom animation rotation angle, in meters. */
        private double mRotationAngle;

        /**
         * Configures a custom animation rotation angle.
         *
         * @param angle custom animation rotation angle, in meters
         *
         * @return this, to allow call chaining
         */
        @NonNull
        public Config withRotationAngle(double angle) {
            mCustomRotationAngle = true;
            mRotationAngle = angle;
            return this;
        }

        /**
         * Tells whether animation rotation angle parameter has been customized in this configuration.
         *
         * @return {@code true} if animation rotation angle parameter has been customized, otherwise {@code false}
         */
        public boolean usesCustomRotationAngle() {
            return mCustomRotationAngle;
        }

        /**
         * Retrieves customized animation rotation angle.
         * <p>
         * Value is meaningless if {@link #withRotationAngle} has not been called previously.
         *
         * @return customized animation rotation angle, in degrees
         */
        public double getRotationAngle() {
            return mRotationAngle;
        }

        /** {@code true} when {@link #withRotationSpeed} has been called once. */
        private boolean mCustomRotationSpeed;

        /** Configured custom rotation angular speed, in degrees per second. */
        private double mRotationSpeed;

        /**
         * Configures a custom animation rotation angular speed.
         *
         * @param speed custom animation rotation angular speed, in degrees per second
         *
         * @return this, to allow call chaining
         */
        @NonNull
        public Config withRotationSpeed(@FloatRange(from = 0) double speed) {
            mCustomRotationSpeed = true;
            mRotationSpeed = speed;
            return this;
        }

        /**
         * Tells whether animation rotation angular speed parameter has been customized in this configuration.
         *
         * @return {@code true} if animation rotation angular speed parameter has been customized,
         *         otherwise {@code false}
         */
        public boolean usesCustomRotationSpeed() {
            return mCustomRotationSpeed;
        }

        /**
         * Retrieves customized animation rotation angular speed.
         * <p>
         * Value is meaningless if {@link #withRotationSpeed} has not been called previously.
         *
         * @return customized animation rotation angular speed, in degrees per second
         */
        @FloatRange(from = 0)
        public double getRotationSpeed() {
            return mRotationSpeed;
        }

        /** Configured custom animation execution mode. {@code null} if not customized. */
        @Nullable
        private Mode mMode;

        /**
         * Configures a custom animation execution mode.
         *
         * @param mode custom execution mode
         *
         * @return this, to allow call chaining
         */
        @NonNull
        public Config withMode(@NonNull Mode mode) {
            mMode = mode;
            return this;
        }

        /**
         * Retrieves customized animation execution mode.
         * <p>
         * Value is {@code null} if {@link #withMode} has not been called previously.
         *
         * @return customized animation execution mode
         */
        @Nullable
        public Mode getMode() {
            return mMode;
        }
    }

    /**
     * Retrieves currently applied animation vertical speed.
     *
     * @return current animation vertical speed, in meters per second
     */
    @FloatRange(from = 0)
    double getVerticalSpeed();

    /**
     * Retrieves currently applied animation vertical distance.
     *
     * @return current animation vertical distance, in meters
     */
    @FloatRange(from = 0)
    double getVerticalDistance();

    /**
     * Retrieves currently applied animation rotation angle.
     *
     * @return current animation rotation angle, in degrees
     */
    double getRotationAngle();

    /**
     * Retrieves currently applied animation rotation angular speed.
     *
     * @return current animation rotation angular speed, in degrees per second
     */
    @FloatRange(from = 0)
    double getRotationSpeed();

    /**
     * Retrieves currently applied animation execution mode.
     *
     * @return current animation execution mode
     */
    @NonNull
    Mode getMode();
}
