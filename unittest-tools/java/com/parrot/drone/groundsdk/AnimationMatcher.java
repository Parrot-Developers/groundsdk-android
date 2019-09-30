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

package com.parrot.drone.groundsdk;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.is;

public final class AnimationMatcher {

    public static Matcher<Animation> animConfigMatches(@NonNull Animation.Config config) {
        return new FeatureMatcher<Animation, Boolean>(is(true), "config match", "config match") {

            @Override
            protected Boolean featureValueOf(Animation actual) {
                return actual.matchesConfig(config);
            }
        };
    }

    public static Matcher<Animation> animTypeIs(@NonNull Animation.Type type) {
        return new FeatureMatcher<Animation, Animation.Type>(is(type), "type", "type") {

            @Override
            protected Animation.Type featureValueOf(Animation actual) {
                return actual.getType();
            }
        };
    }

    public static Matcher<Animation> unidentifiedAnimation() {
        return animTypeIs(Animation.Type.UNIDENTIFIED);
    }

    public static Matcher<Animation> animStatusIs(@NonNull Animation.Status status) {
        return new FeatureMatcher<Animation, Animation.Status>(is(status), "status", "status") {

            @Override
            protected Animation.Status featureValueOf(Animation actual) {
                return actual.getStatus();
            }
        };
    }

    public static Matcher<Animation> animating() {
        return animStatusIs(Animation.Status.ANIMATING);
    }

    public static Matcher<Animation> abortingAnimation() {
        return animStatusIs(Animation.Status.ABORTING);
    }

    public static Matcher<Animation> animProgressIs(@IntRange(from = 0, to = 100) int progress) {
        return new FeatureMatcher<Animation, Integer>(is(progress), "progress", "progress") {

            @Override
            protected Integer featureValueOf(Animation actual) {
                return actual.getProgress();
            }
        };
    }

    private AnimationMatcher() {
    }
}
