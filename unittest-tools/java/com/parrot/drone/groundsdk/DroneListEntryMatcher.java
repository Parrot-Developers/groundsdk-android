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
import com.parrot.drone.groundsdk.device.DroneListEntry;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.equalTo;

/**
 * Drone list entry Matcher, to be used with hasItem or hasItems
 */
@SuppressWarnings({"unused", "UtilityClassWithoutPrivateConstructor"})
public final class DroneListEntryMatcher {

    public static Matcher<DroneListEntry> hasUid(String uid) {
        return new FeatureMatcher<DroneListEntry, String>(equalTo(uid), "Uid", "Uid") {

            @Override
            protected String featureValueOf(DroneListEntry actual) {
                return actual.getUid();
            }
        };
    }

    public static Matcher<DroneListEntry> hasName(String name) {
        return new FeatureMatcher<DroneListEntry, String>(equalTo(name), "Name", "Name") {

            @Override
            protected String featureValueOf(DroneListEntry actual) {
                return actual.getName();
            }
        };
    }

    public static Matcher<DroneListEntry> isModel(Drone.Model model) {
        return new FeatureMatcher<DroneListEntry, Drone.Model>(equalTo(model), "Model", "Model") {

            @Override
            protected Drone.Model featureValueOf(DroneListEntry actual) {
                return actual.getModel();
            }
        };
    }

}
