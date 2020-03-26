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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * Base interface for an Animation.
 */
public interface Animation {

    /** Type of an animation. */
    enum Type {

        /** Unidentified animations. Animations of this type cannot be cast. */
        UNIDENTIFIED,

        /** Candle animation. Animations of this type can be cast to {@link Candle}. */
        CANDLE,

        /** Dolly slide animation. Animations of this type can be cast to {@link DollySlide}. */
        DOLLY_SLIDE,

        /** Dronie animation. Animations of this type can be cast to {@link Dronie}. */
        DRONIE,

        /** Flip animation. Animations of this type can be cast to {@link Flip}. */
        FLIP,

        /**
         * Horizontal 180 photo panorama animation.
         * Animations of this type can be safely cast to {@link Horizontal180PhotoPanorama}.
         */
        HORIZONTAL_180_PHOTO_PANORAMA,

        /** Horizontal panorama animation. Animations of this type can be cast to {@link HorizontalPanorama}. */
        HORIZONTAL_PANORAMA,

        /** Horizontal reveal animation. Animations of this type can be cast to {@link HorizontalReveal}. */
        HORIZONTAL_REVEAL,

        /** Parabola animation. Animations of this type can be cast to {@link Parabola}. */
        PARABOLA,

        /** Position twist-up animation. Animations of this type can be safely cast to {@link GenericTwistUp}. */
        POSITION_TWIST_UP,

        /**
         * Spherical photo panorama animation.
         * Animations of this type can be safely cast to {@link SphericalPhotoPanorama}.
         */
        SPHERICAL_PHOTO_PANORAMA,

        /** Spiral animation. Animations of this type can be safely cast to {@link Spiral}. */
        SPIRAL,

        /** Twist-up animation. Animations of this type can be safely cast to {@link GenericTwistUp}. */
        TWIST_UP,

        /**
         * Vertical 180 photo panorama animation.
         * Animations of this type can be safely cast to {@link Vertical180PhotoPanorama}.
         */
        VERTICAL_180_PHOTO_PANORAMA,

        /** Vertical reveal animation. Animations of this type can be cast to {@link VerticalReveal}. */
        VERTICAL_REVEAL,

        /** Vertigo animation. Animations of this type can be cast to {@link Vertigo}. */
        VERTIGO
    }

    /**
     * Retrieves the animation type.
     *
     * @return animation type
     */
    @NonNull
    Type getType();

    /** Animation execution status. */
    enum Status {

        /** The drone is currently executing the animation. */
        ANIMATING,

        /** The drone is currently in the process of aborting execution of the animation. */
        ABORTING
    }

    /**
     * Retrieves current animation execution status.
     *
     * @return current animation execution status
     */
    @NonNull
    Status getStatus();

    /**
     * Retrieves current animation progress.
     * <p>
     * Progress is expressed as a percentage of current animation completion.
     *
     * @return current animation progress
     */
    @IntRange(from = 0, to = 100)
    int getProgress();

    /**
     * Execution mode used by some animations.
     */
    enum Mode {

        /** Execute animation only once. */
        ONCE,

        /** Execute animation once, then a second time in reverse. */
        ONCE_THEN_MIRRORED
    }

    /**
     * Abstract base for an {@code Animation} configuration.
     * <p>
     * Concrete {@code Config} sub-classes constructor allows to build a configuration that instructs the drone to play
     * the animation with whatever default values it deems appropriate, depending on the current context and/or its own
     * defaults.
     * <p>
     * Concrete sub-classes may also have mandatory parameters in their constructor.
     * <p>
     * Each method starting with '{@code with}' in concrete sub-classes allows to customize a different animation
     * parameter. When one of this method is called, the drone will use the provided parameter value instead of its own
     * default, if possible.
     */
    abstract class Config {

        /** Type of the configured animation . */
        @NonNull
        private final Type mType;

        /**
         * Constructor.
         *
         * @param type animation type
         */
        Config(@NonNull Type type) {
            mType = type;
        }

        /**
         * Retrieves configured animation type.
         *
         * @return the animation type
         */
        @NonNull
        public final Type getAnimationType() {
            return mType;
        }
    }

    /**
     * Tells whether the animation matches some configuration.
     * <p>
     * Mandatory parameters are matched exactly to the corresponding animation current parameters. <br>
     * Optional parameters that have been forcefully customized in the provided configuration are matched exactly to
     * the corresponding animation current parameters. <br>
     * Optional parameters that have been left to their defaults in the provided configuration are not matched.
     *
     * @param config configuration to match this animation against
     *
     * @return {@code true} if the animation matches the given configuration, otherwise {@code false}
     */
    boolean matchesConfig(@NonNull Animation.Config config);
}
