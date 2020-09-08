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

import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.LocationDirective;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.LocationDirective.Orientation;
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.LocationDirective.Orientation.Mode;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static org.hamcrest.core.IsEqual.equalTo;

public final class LocationDirectiveMatcher {

    @NonNull
    public static Matcher<LocationDirective> matchesLocationDirective(double latitude, double longitude,
                                                                      double altitude,
                                                                      @NonNull Orientation orientation) {
        return Matchers.allOf(latitudeIs(latitude), longitudeIs(longitude), altitudeIs(altitude),
                orientationIs(orientation));
    }

    @NonNull
    private static Matcher<LocationDirective> latitudeIs(double latitude) {
        return new FeatureMatcher<LocationDirective, Double>(equalTo(latitude), "latitude is",
                "latitude") {

            @Override
            protected Double featureValueOf(LocationDirective actual) {
                return actual.getLatitude();
            }
        };
    }

    @NonNull
    private static Matcher<LocationDirective> longitudeIs(double longitude) {
        return new FeatureMatcher<LocationDirective, Double>(equalTo(longitude), "longitude is",
                "longitude") {

            @Override
            protected Double featureValueOf(LocationDirective actual) {
                return actual.getLongitude();
            }
        };
    }

    @NonNull
    private static Matcher<LocationDirective> altitudeIs(double altitude) {
        return new FeatureMatcher<LocationDirective, Double>(equalTo(altitude), "altitude is",
                "altitude") {

            @Override
            protected Double featureValueOf(LocationDirective actual) {
                return actual.getAltitude();
            }
        };
    }

    @NonNull
    private static Matcher<LocationDirective> orientationIs(@NonNull Orientation orientation) {
        return Matchers.allOf(
                new FeatureMatcher<LocationDirective, Mode>(equalTo(orientation.getMode()),
                        "orientation mode is", "orientation mode") {

                    @Override
                    protected Mode featureValueOf(LocationDirective actual) {
                        return actual.getOrientation().getMode();
                    }
                },
                new FeatureMatcher<LocationDirective, Double>(equalTo(orientation.getHeading()),
                        "orientation heading is", "orientation heading") {

                    @Override
                    protected Double featureValueOf(LocationDirective actual) {
                        return actual.getOrientation().getHeading();
                    }
                }
        );
    }

    private LocationDirectiveMatcher() {
    }
}
