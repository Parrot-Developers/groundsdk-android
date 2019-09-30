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

package com.parrot.drone.groundsdk.device.pilotingitf;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;

import java.util.EnumSet;

/**
 * Piloting interface capable of commanding a drone to execute automatic animations.
 */
public interface AnimationItf extends PilotingItf {

    /**
     * Retrieves currently executable animation types.
     *
     * @return a set of animation types that can currently be executed by the drone
     */
    @NonNull
    EnumSet<Animation.Type> getAvailableAnimations();

    /**
     * Retrieves currently executing animation.
     *
     * @return current animation, or {@code null} if no animation is currently executing
     */
    @Nullable
    Animation getCurrentAnimation();

    /**
     * Executes an animation.
     *
     * @param animConfig configuration of the animation to execute
     *
     * @return {@code true} if an animation execution request was sent to the drone, otherwise {@code false}
     */
    boolean startAnimation(@NonNull Animation.Config animConfig);

    /**
     * Aborts any currently executing animation.
     *
     * @return {@code true} if an animation cancellation request was sent to the drone, otherwise {@code false}
     */
    boolean abortCurrentAnimation();
}
