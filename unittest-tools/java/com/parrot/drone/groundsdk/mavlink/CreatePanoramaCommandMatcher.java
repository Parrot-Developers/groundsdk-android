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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;

public final class CreatePanoramaCommandMatcher {

    public static Matcher<CreatePanoramaCommand> createPanoramaCommandIs(double horizontalAngle, double horizontalSpeed,
                                                                         double verticalAngle, double verticalSpeed) {
        return allOf(
                horizontalAngleIs(horizontalAngle),
                horizontalSpeedIs(horizontalSpeed),
                verticalAngleIs(verticalAngle),
                verticalSpeedIs(verticalSpeed));
    }

    private static Matcher<CreatePanoramaCommand> horizontalAngleIs(double horizontalAngle) {
        return new FeatureMatcher<CreatePanoramaCommand, Double>(equalTo(horizontalAngle), "horizontal angle is",
                "horizontal angle") {

            @Override
            protected Double featureValueOf(CreatePanoramaCommand actual) {
                return actual.getHorizontalAngle();
            }
        };
    }

    private static Matcher<CreatePanoramaCommand> horizontalSpeedIs(double horizontalSpeed) {
        return new FeatureMatcher<CreatePanoramaCommand, Double>(equalTo(horizontalSpeed), "horizontal speed is",
                "horizontal speed") {

            @Override
            protected Double featureValueOf(CreatePanoramaCommand actual) {
                return actual.getHorizontalSpeed();
            }
        };
    }

    private static Matcher<CreatePanoramaCommand> verticalAngleIs(double verticalAngle) {
        return new FeatureMatcher<CreatePanoramaCommand, Double>(equalTo(verticalAngle), "vertical angle is",
                "vertical angle") {

            @Override
            protected Double featureValueOf(CreatePanoramaCommand actual) {
                return actual.getVerticalAngle();
            }
        };
    }

    private static Matcher<CreatePanoramaCommand> verticalSpeedIs(double verticalSpeed) {
        return new FeatureMatcher<CreatePanoramaCommand, Double>(equalTo(verticalSpeed), "vertical speed is",
                "vertical speed") {

            @Override
            protected Double featureValueOf(CreatePanoramaCommand actual) {
                return actual.getVerticalSpeed();
            }
        };
    }

    private CreatePanoramaCommandMatcher() {
    }
}
