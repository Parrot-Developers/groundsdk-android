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

public final class NavigateToWaypointCommandMatcher {

    public static Matcher<NavigateToWaypointCommand> navigateToWaypointCommandIs(double latitude, double longitude,
                                                                                 double altitude, double yaw,
                                                                                 double holdTime,
                                                                                 double acceptanceRadius) {
        return allOf(
                latitudeIs(latitude),
                longitudeIs(longitude),
                altitudeIs(altitude),
                yawIs(yaw),
                holdTimeIs(holdTime),
                acceptanceRadiusIs(acceptanceRadius));
    }

    private static Matcher<NavigateToWaypointCommand> latitudeIs(double latitude) {
        return new FeatureMatcher<NavigateToWaypointCommand, Double>(equalTo(latitude), "latitude is", "latitude") {

            @Override
            protected Double featureValueOf(NavigateToWaypointCommand actual) {
                return actual.getLatitude();
            }
        };
    }

    private static Matcher<NavigateToWaypointCommand> longitudeIs(double longitude) {
        return new FeatureMatcher<NavigateToWaypointCommand, Double>(equalTo(longitude), "longitude is", "longitude") {

            @Override
            protected Double featureValueOf(NavigateToWaypointCommand actual) {
                return actual.getLongitude();
            }
        };
    }

    private static Matcher<NavigateToWaypointCommand> altitudeIs(double altitude) {
        return new FeatureMatcher<NavigateToWaypointCommand, Double>(equalTo(altitude), "altitude is", "altitude") {

            @Override
            protected Double featureValueOf(NavigateToWaypointCommand actual) {
                return actual.getAltitude();
            }
        };
    }

    private static Matcher<NavigateToWaypointCommand> yawIs(double yaw) {
        return new FeatureMatcher<NavigateToWaypointCommand, Double>(equalTo(yaw), "yaw is", "yaw") {

            @Override
            protected Double featureValueOf(NavigateToWaypointCommand actual) {
                return actual.getYaw();
            }
        };
    }

    private static Matcher<NavigateToWaypointCommand> holdTimeIs(double holdTime) {
        return new FeatureMatcher<NavigateToWaypointCommand, Double>(equalTo(holdTime), "hold time is", "hold time") {

            @Override
            protected Double featureValueOf(NavigateToWaypointCommand actual) {
                return actual.getHoldTime();
            }
        };
    }

    private static Matcher<NavigateToWaypointCommand> acceptanceRadiusIs(double acceptanceRadius) {
        return new FeatureMatcher<NavigateToWaypointCommand, Double>(equalTo(acceptanceRadius), "acceptance radius is",
                "acceptance radius") {

            @Override
            protected Double featureValueOf(NavigateToWaypointCommand actual) {
                return actual.getAcceptanceRadius();
            }
        };
    }

    private NavigateToWaypointCommandMatcher() {
    }
}
