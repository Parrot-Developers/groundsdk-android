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

import com.parrot.drone.groundsdk.device.pilotingitf.PointOfInterestPilotingItf;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static org.hamcrest.core.IsEqual.equalTo;

public final class PointOfInterestMatcher {

    @NonNull
    public static Matcher<PointOfInterestPilotingItf.PointOfInterest> matchesDirective(
            double latitude, double longitude, double altitude, @NonNull PointOfInterestPilotingItf.Mode mode) {
        return Matchers.allOf(latitudeIs(latitude), longitudeIs(longitude), altitudeIs(altitude), modeIs(mode));
    }

    @NonNull
    private static Matcher<PointOfInterestPilotingItf.PointOfInterest> latitudeIs(double latitude) {
        return new FeatureMatcher<PointOfInterestPilotingItf.PointOfInterest, Double>(equalTo(latitude), "latitude is",
                "latitude") {

            @Override
            protected Double featureValueOf(PointOfInterestPilotingItf.PointOfInterest actual) {
                return actual.getLatitude();
            }
        };
    }

    @NonNull
    private static Matcher<PointOfInterestPilotingItf.PointOfInterest> longitudeIs(double longitude) {
        return new FeatureMatcher<PointOfInterestPilotingItf.PointOfInterest, Double>(equalTo(longitude),
                "longitude is",
                "longitude") {

            @Override
            protected Double featureValueOf(PointOfInterestPilotingItf.PointOfInterest actual) {
                return actual.getLongitude();
            }
        };
    }

    @NonNull
    private static Matcher<PointOfInterestPilotingItf.PointOfInterest> altitudeIs(double altitude) {
        return new FeatureMatcher<PointOfInterestPilotingItf.PointOfInterest, Double>(equalTo(altitude), "altitude is",
                "altitude") {

            @Override
            protected Double featureValueOf(PointOfInterestPilotingItf.PointOfInterest actual) {
                return actual.getAltitude();
            }
        };
    }

    @NonNull
    private static Matcher<PointOfInterestPilotingItf.PointOfInterest> modeIs(PointOfInterestPilotingItf.Mode mode) {
        return new FeatureMatcher<PointOfInterestPilotingItf.PointOfInterest, PointOfInterestPilotingItf.Mode>(
                equalTo(mode), "mode is", "mode") {

            @Override
            protected PointOfInterestPilotingItf.Mode featureValueOf(
                    PointOfInterestPilotingItf.PointOfInterest actual) {
                return actual.getMode();
            }
        };
    }

    private PointOfInterestMatcher() {
    }
}
