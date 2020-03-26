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
 * Spiral animation interface.
 * <p>
 * This animation instructs the drone to circle around the target, possibly in a spiral shape and possibly also while
 * flying vertically (up or down). <br>
 * The target in question depends on the currently active
 * {@link com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf piloting interface}.
 */
public interface Spiral extends Animation {

    /**
     * Spiral animation configuration class.
     * <p>
     * Allows to configure the following parameters for this animation:
     * <ul>
     * <li><strong>speed:</strong> animation execution speed, in meters per second. If {@link #withSpeed speed} is
     * not customized, then the drone will apply its own default value for this parameter. </li>
     * <li><strong>radius variation:</strong> animation radius variation.
     * <ul>
     * <li>{@code 2} means that the ending radius will be twice as big as the starting radius.</li>
     * <li>{@code -2} means that the ending radius will be half of the size of the starting radius.</li>
     * <li>{@code 1} means that the radius will not change during the animation.</li>
     * </ul>
     * If {@link #withRadiusVariation radius variation} is not customized, then the drone will apply its own
     * default value for this parameter.</li>
     * <li><strong>vertical distance:</strong> distance the drone will fly vertically, in meters. Positive values
     * instruct the drone to fly up, negative value instructs to fly down. If
     * {@link #withVerticalDistance vertical distance} is not customized, then the drone will apply its own default
     * value for this parameter.</li>
     * <li><strong>revolution amount:</strong> number of revolutions the drone will perform around the target.
     * Positive values instruct the drone to circle clockwise, negative values instruct to circle
     * counter-clockwise. If {@link #withRevolutionAmount revolution amount} is not customized, then the drone will
     * apply its own default value for this parameter.
     * <li><strong>mode:</strong> animation execution {@link Mode mode}. If {@link #withMode mode} is not
     * customized, then the drone will apply its own default value for this parameter: {@link Mode#ONCE}.</li>
     * </ul>
     */
    final class Config extends Animation.Config {

        /**
         * Constructor.
         */
        public Config() {
            super(Type.SPIRAL);
        }

        /** {@code true} when {@link #withSpeed} has been called once. */
        private boolean mCustomSpeed;

        /** Configured custom animation speed, in meters per second. */
        private double mSpeed;

        /**
         * Configures a custom animation speed.
         *
         * @param speed custom animation speed, in meters per second
         *
         * @return this, to allow call chaining
         */
        @NonNull
        public Config withSpeed(@FloatRange(from = 0) double speed) {
            mCustomSpeed = true;
            mSpeed = speed;
            return this;
        }

        /**
         * Tells whether animation speed parameter has been customized in this configuration.
         *
         * @return {@code true} if animation speed parameter has been customized, otherwise {@code false}
         */
        public boolean usesCustomSpeed() {
            return mCustomSpeed;
        }

        /**
         * Retrieves customized animation speed.
         * <p>
         * Value is meaningless if {@link #withSpeed} has not been called previously.
         *
         * @return customized animation speed, in meters per second
         */
        @FloatRange(from = 0)
        public double getSpeed() {
            return mSpeed;
        }

        /** {@code true} when {@link #withRadiusVariation} has been called once. */
        private boolean mCustomRadiusVariation;

        /** Configured custom animation radius variation. */
        private double mRadiusVariation;

        /**
         * Configures a custom animation radius variation.
         *
         * @param radiusVariation custom animation radius variation
         *
         * @return this, to allow call chaining
         */
        @NonNull
        public Config withRadiusVariation(double radiusVariation) {
            mCustomRadiusVariation = true;
            mRadiusVariation = radiusVariation;
            return this;
        }

        /**
         * Tells whether animation radius variation parameter has been customized in this configuration.
         *
         * @return {@code true} if animation radius variation parameter has been customized, otherwise {@code false}
         */
        public boolean usesCustomRadiusVariation() {
            return mCustomRadiusVariation;
        }

        /**
         * Retrieves customized animation radius variation.
         * <p>
         * Value is meaningless if {@link #withRadiusVariation} has not been called previously.
         *
         * @return customized animation radius variation
         */
        public double getRadiusVariation() {
            return mRadiusVariation;
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
        public Config withVerticalDistance(double distance) {
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
        public double getVerticalDistance() {
            return mVerticalDistance;
        }

        /** {@code true} when {@link #withRevolutionAmount} has been called once. */
        private boolean mCustomRevolutionAmount;

        /** Configured custom revolution amount. */
        private double mRevolutionAmount;

        /**
         * Configures a custom animation revolution amount.
         *
         * @param revolutionAmount custom vertical revolution amount
         *
         * @return this, to allow call chaining
         */
        @NonNull
        public Config withRevolutionAmount(double revolutionAmount) {
            mCustomRevolutionAmount = true;
            mRevolutionAmount = revolutionAmount;
            return this;
        }

        /**
         * Tells whether animation revolution amount parameter has been customized in this configuration.
         *
         * @return {@code true} if animation revolution amount parameter has been customized, otherwise {@code false}
         */
        public boolean usesCustomRevolutionAmount() {
            return mCustomRevolutionAmount;
        }

        /**
         * Retrieves customized animation revolution amount.
         * <p>
         * Value is meaningless if {@link #withRevolutionAmount} has not been called previously.
         *
         * @return customized animation revolution amount
         */
        public double getRevolutionAmount() {
            return mRevolutionAmount;
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
     * Retrieves currently applied animation speed.
     *
     * @return current animation speed, in meters per second
     */
    @FloatRange(from = 0)
    double getSpeed();

    /**
     * Retrieves currently applied animation radius variation.
     *
     * @return current animation radius variation
     */
    double getRadiusVariation();

    /**
     * Retrieves currently applied animation vertical distance.
     *
     * @return current animation vertical distance, in meters
     */
    double getVerticalDistance();

    /**
     * Retrieves currently applied animation revolution amount.
     *
     * @return current animation revolution amount
     */
    double getRevolutionAmount();

    /**
     * Retrieves currently applied animation execution mode.
     *
     * @return current animation execution mode
     */
    @NonNull
    Mode getMode();
}
