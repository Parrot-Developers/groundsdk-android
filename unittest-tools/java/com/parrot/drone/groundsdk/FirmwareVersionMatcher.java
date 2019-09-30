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

import androidx.annotation.IntRange;

import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;

/**
 * Drone matcher
 */
@SuppressWarnings({"unused", "UtilityClassWithoutPrivateConstructor"})
public final class FirmwareVersionMatcher {

    public static Matcher<FirmwareVersion> hasType(FirmwareVersion.Type type) {
        return new FeatureMatcher<FirmwareVersion, FirmwareVersion.Type>(equalTo(type), "Type", "Type") {

            @Override
            protected FirmwareVersion.Type featureValueOf(FirmwareVersion actual) {
                return actual.getType();
            }
        };
    }

    public static Matcher<FirmwareVersion> hasMajor(@IntRange(from = 0) int major) {
        return new FeatureMatcher<FirmwareVersion, Integer>(equalTo(major), "Major", "Major") {

            @Override
            protected Integer featureValueOf(FirmwareVersion actual) {
                return actual.getMajor();
            }
        };
    }

    public static Matcher<FirmwareVersion> hasMinor(@IntRange(from = 0) int minor) {
        return new FeatureMatcher<FirmwareVersion, Integer>(equalTo(minor), "Minor", "Minor") {

            @Override
            protected Integer featureValueOf(FirmwareVersion actual) {
                return actual.getMinor();
            }
        };
    }

    public static Matcher<FirmwareVersion> hasPatch(@IntRange(from = 0) int patch) {
        return new FeatureMatcher<FirmwareVersion, Integer>(equalTo(patch), "Patch", "Patch") {

            @Override
            protected Integer featureValueOf(FirmwareVersion actual) {
                return actual.getPatchLevel();
            }
        };
    }

    public static Matcher<FirmwareVersion> isRelease() {
        return both(hasType(FirmwareVersion.Type.RELEASE)).and(hasBuild(0));
    }

    public static Matcher<FirmwareVersion> isDev() {
        return allOf(hasType(FirmwareVersion.Type.DEVELOPMENT), hasMajor(0), hasMinor(0), hasPatch(0), hasBuild(0));
    }

    public static Matcher<FirmwareVersion> isRC(@IntRange(from = 0) int build) {
        return both(hasType(FirmwareVersion.Type.RELEASE_CANDIDATE)).and(hasBuild(build));
    }

    public static Matcher<FirmwareVersion> isBeta(@IntRange(from = 0) int build) {
        return both(hasType(FirmwareVersion.Type.BETA)).and(hasBuild(build));
    }

    public static Matcher<FirmwareVersion> isAlpha(@IntRange(from = 0) int build) {
        return both(hasType(FirmwareVersion.Type.ALPHA)).and(hasBuild(build));
    }

    private static Matcher<FirmwareVersion> hasBuild(@IntRange(from = 0) int build) {
        return new FeatureMatcher<FirmwareVersion, Integer>(equalTo(build), "Build", "Build") {

            @Override
            protected Integer featureValueOf(FirmwareVersion actual) {
                return actual.getBuildNumber();
            }
        };
    }

}
