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

import com.parrot.drone.groundsdk.device.peripheral.TargetTracker;

import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;

public final class TargetTrajectoryMatcher {

    public static Matcher<TargetTracker.TargetTrajectory> targetTrajectoryIs(double latitude, double longitude,
                                                                             double altitude, double northSpeed,
                                                                             double eastSpeed, double downSpeed) {
        return allOf(
                targetTrajectoryLatitudeIs(latitude),
                targetTrajectoryLongitudeIs(longitude),
                targetTrajectoryAltitudeIs(altitude),
                targetTrajectoryNorthSpeedIs(northSpeed),
                targetTrajectoryEastSpeedIs(eastSpeed),
                targetTrajectoryDownSpeedIs(downSpeed));
    }

    public static Matcher<TargetTracker.TargetTrajectory> targetTrajectoryLatitudeIs(double latitude) {
        return MatcherBuilders.featureMatcher(equalTo(latitude), "altitude",
                TargetTracker.TargetTrajectory::getLatitude);
    }

    public static Matcher<TargetTracker.TargetTrajectory> targetTrajectoryLongitudeIs(double longitude) {
        return MatcherBuilders.featureMatcher(equalTo(longitude), "longitude",
                TargetTracker.TargetTrajectory::getLongitude);
    }

    public static Matcher<TargetTracker.TargetTrajectory> targetTrajectoryAltitudeIs(double altitude) {
        return MatcherBuilders.featureMatcher(equalTo(altitude), "altitude",
                TargetTracker.TargetTrajectory::getAltitude);
    }

    public static Matcher<TargetTracker.TargetTrajectory> targetTrajectoryNorthSpeedIs(double northSpeed) {
        return MatcherBuilders.featureMatcher(equalTo(northSpeed), "northSpeed",
                TargetTracker.TargetTrajectory::getNorthSpeed);
    }

    public static Matcher<TargetTracker.TargetTrajectory> targetTrajectoryEastSpeedIs(double eastSpeed) {
        return MatcherBuilders.featureMatcher(equalTo(eastSpeed), "eastSpeed",
                TargetTracker.TargetTrajectory::getEastSpeed);
    }

    public static Matcher<TargetTracker.TargetTrajectory> targetTrajectoryDownSpeedIs(double downSpeed) {
        return MatcherBuilders.featureMatcher(equalTo(downSpeed), "downSpeed",
                TargetTracker.TargetTrajectory::getDownSpeed);
    }

    private TargetTrajectoryMatcher() {
    }
}
