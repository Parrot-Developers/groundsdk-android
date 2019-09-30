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

import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.FinishedRelativeMoveFlightInfo;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static org.hamcrest.core.IsEqual.equalTo;

public final class FinishedRelativeMoveFlightInfoMatcher {

    public static Matcher<FinishedRelativeMoveFlightInfo> matchesFinishedRelativeMoveFlightInfo(boolean success,
                                                                                                double actualForwardComponent,
                                                                                                double actualRightComponent,
                                                                                                double actualDownwardComponent,
                                                                                                double actualHeadingRotation) {
        return Matchers.allOf(
                successIs(success),
                actualForwardComponentIs(actualForwardComponent),
                actualRightComponentIs(actualRightComponent),
                actualDownwardComponentIs(actualDownwardComponent),
                actualHeadingRotationIs(actualHeadingRotation)
        );
    }

    @NonNull
    private static Matcher<FinishedRelativeMoveFlightInfo> successIs(boolean success) {
        return new FeatureMatcher<FinishedRelativeMoveFlightInfo, Boolean>(equalTo(success), "success is",
                "success") {

            @Override
            protected Boolean featureValueOf(FinishedRelativeMoveFlightInfo actual) {
                return actual.wasSuccessful();
            }
        };
    }

    @NonNull
    private static Matcher<FinishedRelativeMoveFlightInfo> actualForwardComponentIs(double actualForwardComponent) {
        return new FeatureMatcher<FinishedRelativeMoveFlightInfo, Double>(equalTo(actualForwardComponent),
                "forward component is", "forward component") {

            @Override
            protected Double featureValueOf(FinishedRelativeMoveFlightInfo actual) {
                return actual.getActualForwardComponent();
            }
        };
    }

    @NonNull
    private static Matcher<FinishedRelativeMoveFlightInfo> actualRightComponentIs(double actualRightComponent) {
        return new FeatureMatcher<FinishedRelativeMoveFlightInfo, Double>(equalTo(actualRightComponent),
                "right component is", "right component") {

            @Override
            protected Double featureValueOf(FinishedRelativeMoveFlightInfo actual) {
                return actual.getActualRightComponent();
            }
        };
    }

    @NonNull
    private static Matcher<FinishedRelativeMoveFlightInfo> actualDownwardComponentIs(double actualDownwardComponent) {
        return new FeatureMatcher<FinishedRelativeMoveFlightInfo, Double>(equalTo(actualDownwardComponent),
                "downward component is", "downward component") {

            @Override
            protected Double featureValueOf(FinishedRelativeMoveFlightInfo actual) {
                return actual.getActualDownwardComponent();
            }
        };
    }

    @NonNull
    private static Matcher<FinishedRelativeMoveFlightInfo> actualHeadingRotationIs(double actualHeadingRotation) {
        return new FeatureMatcher<FinishedRelativeMoveFlightInfo, Double>(equalTo(actualHeadingRotation),
                "heading rotation is", "heading rotation") {

            @Override
            protected Double featureValueOf(FinishedRelativeMoveFlightInfo actual) {
                return actual.getActualHeadingRotation();
            }
        };
    }

    private FinishedRelativeMoveFlightInfoMatcher() {
    }
}
