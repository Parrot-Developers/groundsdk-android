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

package com.parrot.drone.groundsdk.internal.device.pilotingitf.animation;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.pilotingitf.AnimationItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

import java.util.EnumSet;

/** Core class for AnimationItf. */
public final class AnimationItfCore extends SingletonComponentCore implements AnimationItf {

    /** Description of AnimationItf. */
    private static final ComponentDescriptor<PilotingItf, AnimationItf> DESC =
            ComponentDescriptor.of(AnimationItf.class);

    /** Engine-specific backend for AnimationItf. */
    public interface Backend {

        /**
         * Starts an animation.
         *
         * @param animationConfig configuration of the animation to start
         *
         * @return {@code true} if an animation start request was sent to the drone, otherwise {@code null}
         */
        boolean startAnimation(@NonNull Animation.Config animationConfig);

        /**
         * Aborts any ongoing animation.
         *
         * @return {@code true} if an animation cancellation request was sent to the drone, otherwise {@code null}
         */
        boolean abortCurrentAnimation();
    }

    /** Engine piloting interface backend. */
    @NonNull
    private final Backend mBackend;

    /** Currently supported animation types. */
    @NonNull
    private EnumSet<Animation.Type> mAvailableAnimations;

    /** Currently executing animation, {@code null} if none. */
    @Nullable
    private AnimationCore mCurrentAnimation;

    /**
     * Constructor.
     *
     * @param pilotingItfStore store where this piloting interface belongs
     * @param backend          backend used to forward actions to the engine
     */
    public AnimationItfCore(@NonNull ComponentStore<PilotingItf> pilotingItfStore, @NonNull Backend backend) {
        super(DESC, pilotingItfStore);
        mBackend = backend;
        mAvailableAnimations = EnumSet.noneOf(Animation.Type.class);
    }

    @Override
    @NonNull
    public EnumSet<Animation.Type> getAvailableAnimations() {
        return mAvailableAnimations;
    }

    @Override
    @Nullable
    public Animation getCurrentAnimation() {
        return mCurrentAnimation;
    }

    @Override
    public boolean startAnimation(@NonNull Animation.Config animConfig) {
        return mAvailableAnimations.contains(animConfig.getAnimationType()) && mBackend.startAnimation(animConfig);
    }

    @Override
    public boolean abortCurrentAnimation() {
        return mCurrentAnimation != null && mCurrentAnimation.getStatus() == Animation.Status.ANIMATING
               && mBackend.abortCurrentAnimation();
    }

    /**
     * Updates currently available animation types.
     * <p>
     * {@code animations} parameter ownership is transferred to this method.
     *
     * @param animations set of available animation types
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public AnimationItfCore updateAvailableAnimations(@NonNull EnumSet<Animation.Type> animations) {
        if (!mAvailableAnimations.equals(animations)) {
            mAvailableAnimations = animations;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates currently playing animation.
     * <p>
     * {@code animation} parameter ownership is transferred to this method.
     *
     * @param animation currently playing animation, or {@code null} if none
     * @param status    current animation status
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public AnimationItfCore updateAnimation(@NonNull AnimationCore animation, @NonNull Animation.Status status) {
        if (animation.equals(mCurrentAnimation)) {
            mChanged |= mCurrentAnimation.updateStatus(status);
        } else {
            animation.updateStatus(status);
            mCurrentAnimation = animation;
            mChanged = true;
        }
        return this;
    }

    /**
     * Clears currently playing animation.
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public AnimationItfCore clearAnimation() {
        if (mCurrentAnimation != null) {
            mCurrentAnimation = null;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates current animation progress.
     *
     * @param progress up-to-date animation progress
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public AnimationItfCore updateCurrentAnimationProgress(@IntRange(from = 0, to = 100) int progress) {
        mChanged |= mCurrentAnimation != null && mCurrentAnimation.updateProgress(progress);
        return this;
    }
}
