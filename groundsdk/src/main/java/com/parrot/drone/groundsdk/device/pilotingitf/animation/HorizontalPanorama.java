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

/**
 * Horizontal panorama animation interface.
 * <p>
 * This animation instructs the drone to rotate horizontally.
 */
public interface HorizontalPanorama extends Animation {

    /**
     * Horizontal panorama animation configuration class.
     * <p>
     * Allows to configure the following parameters for this animation:
     * <ul>
     * <li><strong>rotation angle:</strong> angle of the rotation the drone should perform, in degrees. Positive
     * values make the drone rotate clockwise, negative values make it rotate counter-clockwise. Absolute value may
     * be greater than 360 degrees to perform more than one complete rotation. If
     * {@link #withRotationAngle rotation angle} is not customized, then the drone will apply its own default value
     * for this parameter. </li>
     * <li><strong>rotation speed:</strong> angular speed of the rotation, in degrees per second. If
     * {@link #withRotationSpeed rotation speed} is not customized, then the drone will apply its own default value
     * for this parameter. </li>
     * </ul>
     */
    final class Config extends Animation.Config {

        /**
         * Constructor.
         */
        public Config() {
            super(Type.HORIZONTAL_PANORAMA);
        }

        /** {@code true} when {@link #withRotationAngle} has been called once. */
        private boolean mCustomRotationAngle;

        /** Configured custom rotation angle, in degrees. */
        private double mRotationAngle;

        /**
         * Configures a custom animation rotation angle.
         *
         * @param angle custom animation rotation angle, in degrees
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
    }

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
}
