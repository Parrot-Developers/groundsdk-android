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

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.DroneFinder;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.equalTo;

/**
 * Drone matcher
 */
@SuppressWarnings({"unused", "UtilityClassWithoutPrivateConstructor"})
public final class DiscoveredDroneMatcher {

    public static Matcher<DroneFinder.DiscoveredDrone> hasUid(String uid) {
        return new FeatureMatcher<DroneFinder.DiscoveredDrone, String>(equalTo(uid), "Uid", "Uid") {

            @Override
            protected String featureValueOf(DroneFinder.DiscoveredDrone actual) {
                return actual.getUid();
            }
        };
    }

    public static Matcher<DroneFinder.DiscoveredDrone> isModel(Drone.Model model) {
        return new FeatureMatcher<DroneFinder.DiscoveredDrone, Drone.Model>(equalTo(model), "Model", "Model") {

            @Override
            protected Drone.Model featureValueOf(DroneFinder.DiscoveredDrone actual) {
                return actual.getModel();
            }
        };
    }

    public static Matcher<DroneFinder.DiscoveredDrone> hasName(String name) {
        return new FeatureMatcher<DroneFinder.DiscoveredDrone, String>(equalTo(name), "Name", "Name") {

            @Override
            protected String featureValueOf(DroneFinder.DiscoveredDrone actual) {
                return actual.getName();
            }
        };
    }

    public static Matcher<DroneFinder.DiscoveredDrone> hasSecurity(
            DroneFinder.DiscoveredDrone.ConnectionSecurity security) {
        return new FeatureMatcher<DroneFinder.DiscoveredDrone, DroneFinder.DiscoveredDrone.ConnectionSecurity>(
                equalTo(security), "Security", "Security") {

            @Override
            protected DroneFinder.DiscoveredDrone.ConnectionSecurity featureValueOf(
                    DroneFinder.DiscoveredDrone actual) {
                return actual.getConnectionSecurity();
            }
        };
    }

    public static Matcher<DroneFinder.DiscoveredDrone> hasRssi(int rssi) {
        return new FeatureMatcher<DroneFinder.DiscoveredDrone, Integer>(equalTo(rssi), "RSSI", "RSSI") {

            @Override
            protected Integer featureValueOf(DroneFinder.DiscoveredDrone actual) {
                return actual.getRssi();
            }
        };
    }

    public static Matcher<DroneFinder.DiscoveredDrone> isKnown(boolean known) {
        return new FeatureMatcher<DroneFinder.DiscoveredDrone, Boolean>(equalTo(known), "Known", "Known") {

            @Override
            protected Boolean featureValueOf(DroneFinder.DiscoveredDrone actual) {
                return actual.isKnown();
            }
        };
    }
}
