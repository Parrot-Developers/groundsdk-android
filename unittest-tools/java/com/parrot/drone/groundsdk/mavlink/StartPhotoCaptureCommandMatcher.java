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

package com.parrot.drone.groundsdk.mavlink;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;

public final class StartPhotoCaptureCommandMatcher {

    public static Matcher<StartPhotoCaptureCommand> startPhotoCaptureCommandIs(
            double interval, @IntRange(from = 0) int count, @NonNull StartPhotoCaptureCommand.Format format) {
        return allOf(intervalIs(interval), countIs(count), formatIs(format));
    }

    private static Matcher<StartPhotoCaptureCommand> intervalIs(double interval) {
        return new FeatureMatcher<StartPhotoCaptureCommand, Double>(equalTo(interval), "interval is", "interval") {

            @Override
            protected Double featureValueOf(StartPhotoCaptureCommand actual) {
                return actual.getInterval();
            }
        };
    }

    private static Matcher<StartPhotoCaptureCommand> countIs(int count) {
        return new FeatureMatcher<StartPhotoCaptureCommand, Integer>(equalTo(count), "count is", "count") {

            @Override
            protected Integer featureValueOf(StartPhotoCaptureCommand actual) {
                return actual.getCount();
            }
        };
    }

    private static Matcher<StartPhotoCaptureCommand> formatIs(@NonNull StartPhotoCaptureCommand.Format format) {
        return new FeatureMatcher<StartPhotoCaptureCommand, StartPhotoCaptureCommand.Format>(equalTo(format),
                "format is", "format") {

            @Override
            protected StartPhotoCaptureCommand.Format featureValueOf(StartPhotoCaptureCommand actual) {
                return actual.getFormat();
            }
        };
    }

    private StartPhotoCaptureCommandMatcher() {
    }
}
