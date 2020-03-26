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
 * Vertigo animation interface.
 * <p>
 * This animation instructs the drone to zoom in on the subject and to move away from it at same time. <br>
 * The target in question depends on the currently active
 * {@link com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf piloting interface}.
 */
public interface Vertigo extends Animation {

    /** Action to execute at the end of a Vertigo. */
    enum FinishAction {

        /** Do nothing. */
        NONE,

        /** Move zoom level back to x1. */
        UNZOOM
    }

    /**
     * Vertigo animation configuration class.
     * <p>
     * Allows to configure the following parameters for this animation:
     * <ul>
     * <li><strong>duration:</strong> animation duration, in seconds. If {@link #withDuration duration} is
     * not customized, then the drone will apply its own default value for this parameter. </li>
     * <li><strong>maximum zoom level:</strong> maximum zoom level. If {@link #withMaxZoomLevel maximum zoom level}
     * is not customized, then the drone will apply its own default value for this parameter.</li>
     * <li><strong>finish action:</strong> action to executed at the end of the animation, {@link FinishAction}.
     * If {@link #withFinishAction finish action} is not customized, then the drone will apply its own default value
     * for this parameter.</li>
     * <li><strong>mode:</strong> animation execution {@link Mode mode}. If {@link #withMode mode} is not
     * customized, then the drone will apply its own default value for this parameter: {@link Mode#ONCE}.</li>
     * </ul>
     */
    final class Config extends Animation.Config {

        /**
         * Constructor.
         */
        public Config() {
            super(Type.VERTIGO);
        }

        /** {@code true} when {@link #withDuration} has been called once. */
        private boolean mCustomDuration;

        /** Configured custom animation duration, in seconds. */
        private double mDuration;

        /**
         * Configures a custom animation duration.
         *
         * @param duration custom animation duration, in seconds
         *
         * @return this, to allow call chaining
         */
        @NonNull
        public Config withDuration(@FloatRange(from = 0) double duration) {
            mCustomDuration = true;
            mDuration = duration;
            return this;
        }

        /**
         * Tells whether animation duration parameter has been customized in this configuration.
         *
         * @return {@code true} if animation duration parameter has been customized, otherwise {@code false}
         */
        public boolean usesCustomDuration() {
            return mCustomDuration;
        }

        /**
         * Retrieves customized animation duration.
         * <p>
         * Value is meaningless if {@link #withDuration} has not been called previously.
         *
         * @return customized animation duration, in seconds
         */
        @FloatRange(from = 0)
        public double getDuration() {
            return mDuration;
        }

        /** {@code true} when {@link #withMaxZoomLevel} has been called once. */
        private boolean mCustomMaxZoomLevel;

        /** Configured custom maximum zoom level. */
        private double mMaxZoomLevel;

        /**
         * Configures a custom maximum zoom level.
         *
         * @param maxZoomLevel custom maximum zoom level
         *
         * @return this, to allow call chaining
         */
        @NonNull
        public Config withMaxZoomLevel(@FloatRange(from = 0) double maxZoomLevel) {
            mCustomMaxZoomLevel = true;
            mMaxZoomLevel = maxZoomLevel;
            return this;
        }

        /**
         * Tells whether maximum zoom level parameter has been customized in this configuration.
         *
         * @return {@code true} if maximum zoom level parameter has been customized, otherwise {@code false}
         */
        public boolean usesCustomMaxZoomLevel() {
            return mCustomMaxZoomLevel;
        }

        /**
         * Retrieves customized maximum zoom level.
         * <p>
         * Value is meaningless if {@link #withMaxZoomLevel} has not been called previously.
         *
         * @return customized maximum zoom level
         */
        @FloatRange(from = 0)
        public double getMaxZoomLevel() {
            return mMaxZoomLevel;
        }

        /** Action executed at the end of the animation. */
        @Nullable
        private FinishAction mFinishAction;

        /**
         * Configures a customized action executed at the end of the animation.
         *
         * @param finishAction custom finish action
         *
         * @return this, to allow call chaining
         */
        @NonNull
        public Config withFinishAction(@NonNull FinishAction finishAction) {
            mFinishAction = finishAction;
            return this;
        }

        /**
         * Retrieves customized action executed at the end of the animation.
         * <p>
         * Value is {@code null} if {@link #withFinishAction} has not been called previously.
         *
         * @return customized action executed at the end of the animation
         */
        @Nullable
        public FinishAction getFinishAction() {
            return mFinishAction;
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
     * Retrieves currently applied animation duration.
     *
     * @return current animation duration, in seconds
     */
    @FloatRange(from = 0)
    double getDuration();

    /**
     * Retrieves currently applied maximum zoom level.
     *
     * @return current maximum zoom level
     */
    @FloatRange(from = 0)
    double getMaxZoomLevel();

    /**
     * Retrieves currently applied action executed at the end of the animation.
     *
     * @return current action executed at the end of the animation
     */
    @NonNull
    FinishAction getFinishAction();

    /**
     * Retrieves currently applied animation execution mode.
     *
     * @return current animation execution mode
     */
    @NonNull
    Mode getMode();
}
