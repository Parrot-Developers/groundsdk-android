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

import androidx.annotation.NonNull;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

/**
 * Utility class to build matcher in a more concise way.
 */
public final class MatcherBuilders {

    public interface ValueExtractor<T, U> {

        U featureValueOf(T actual);
    }

    public static <T, U> Matcher<T> featureMatcher(@NonNull Matcher<? super U> subMatcher, @NonNull String desc,
                                                   @NonNull ValueExtractor<T, U> extractor) {
        return new FeatureMatcher<T, U>(subMatcher, desc, desc) {

            @Override
            protected U featureValueOf(T actual) {
                return extractor.featureValueOf(actual);
            }
        };
    }

    public static <T, U> Matcher<T> valueMatcher(U value, @NonNull String desc,
                                                 @NonNull ValueExtractor<T, U> extractor) {
        return featureMatcher(is(value), desc, extractor);
    }

    public static <T, U extends Enum<U>> Matcher<T> enumSetFeatureMatcher(
            @NonNull Matcher<? super Set<U>> subMatcher,
            @NonNull String desc,
            @NonNull ValueExtractor<T, Set<U>> extractor) {
        return featureMatcher(subMatcher, desc, extractor);
    }

    public static <T, U extends Enum<U>> Matcher<T> enumSetFeatureMatcher(
            @NonNull Set<U> items,
            @NonNull String desc,
            @NonNull ValueExtractor<T, Set<U>> extractor) {
        return enumSetFeatureMatcher(containsInAnyOrder(items.toArray(new Object[0])), desc, extractor);
    }

    private MatcherBuilders() {
    }
}
