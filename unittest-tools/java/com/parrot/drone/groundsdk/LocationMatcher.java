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

import android.location.Location;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;

public final class LocationMatcher {

    public static Matcher<Location> locationIsUnavailable() {
        return nullValue(Location.class);
    }

    public static Matcher<Location> locationIs(double latitude, double longitude, double altitude) {
        return Matchers.both(locationIs(latitude, longitude)).and(nonNullAltitudeIs(altitude));
    }

    public static Matcher<Location> locationIs(double latitude, double longitude) {
        return Matchers.both(notNullValue(Location.class)).and(nonNullLocationIs(latitude, longitude));
    }

    public static Matcher<Location> altitudeIs(double altitude) {
        return Matchers.both(notNullValue(Location.class)).and(nonNullAltitudeIs(altitude));
    }

    public static Matcher<Location> accuracyIs(double accuracy) {
        return Matchers.both(notNullValue(Location.class)).and(nonNullAccuracyIs(accuracy));
    }

    private static Matcher<Location> nonNullLocationIs(double latitude, double longitude) {
        return Matchers.allOf(
                new FeatureMatcher<Location, Double>(equalTo(latitude), "latitude is", "latitude") {

                    @Override
                    protected Double featureValueOf(Location actual) {
                        return actual.getLatitude();
                    }
                },
                new FeatureMatcher<Location, Double>(equalTo(longitude), "longitude is", "longitude") {

                    @Override
                    protected Double featureValueOf(Location actual) {
                        return actual.getLongitude();
                    }
                }
        );
    }

    private static Matcher<Location> nonNullAltitudeIs(double altitude) {
        return Matchers.allOf(
                new FeatureMatcher<Location, Boolean>(equalTo(true), "altitude is available", "altitude available") {

                    @Override
                    protected Boolean featureValueOf(Location actual) {
                        return actual.hasAltitude();
                    }
                },
                new FeatureMatcher<Location, Double>(equalTo(altitude), "altitude is", "altitude") {

                    @Override
                    protected Double featureValueOf(Location actual) {
                        return actual.getAltitude();
                    }
                }
        );
    }

    private static Matcher<Location> nonNullAccuracyIs(double accuracy) {
        return Matchers.allOf(
                new FeatureMatcher<Location, Boolean>(equalTo(true), "accuracy is available", "accuracy available") {

                    @Override
                    protected Boolean featureValueOf(Location actual) {
                        return actual.hasAccuracy();
                    }
                },
                new FeatureMatcher<Location, Double>(equalTo(accuracy), "accuracy is", "accuracy") {

                    @Override
                    protected Double featureValueOf(Location actual) {
                        return (double) actual.getAccuracy();
                    }
                }
        );
    }

    private LocationMatcher() {
    }
}
