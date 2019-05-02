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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposureLock;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static com.parrot.drone.groundsdk.MatcherBuilders.valueMatcher;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Camera exposure lock matcher
 */
public final class CameraExposureLockMatcher {

    private static Matcher<CameraExposureLock> exposureLockModeIs(CameraExposureLock.Mode mode,
                                                                  double centerX, double centerY,
                                                                  double width, double height) {
        return allOf(
                notNullValue(),
                valueMatcher(mode, "mode", CameraExposureLock::mode),
                valueMatcher(centerX, "centerX", CameraExposureLock::getRegionCenterX),
                valueMatcher(centerY, "centerY", CameraExposureLock::getRegionCenterY),
                valueMatcher(width, "width", CameraExposureLock::getRegionWidth),
                valueMatcher(height, "height", CameraExposureLock::getRegionHeight));
    }

    private static Matcher<CameraExposureLock> exposureLockModeIs(CameraExposureLock.Mode mode) {
        return allOf(
                notNullValue(),
                valueMatcher(mode, "mode", CameraExposureLock::mode));
    }

    public static Matcher<CameraExposureLock> exposureLockModeIsUpdatingTo(CameraExposureLock.Mode mode,
                                                                           double centerX, double centerY,
                                                                           double width, double height) {
        return allOf(
                exposureLockModeIs(mode, centerX, centerY, width, height),
                exposureLockModeIsUpdating());
    }

    public static Matcher<CameraExposureLock> exposureLockModeIsUpdatingTo(CameraExposureLock.Mode mode) {
        return allOf(
                exposureLockModeIs(mode),
                exposureLockModeIsUpdating());
    }

    public static Matcher<CameraExposureLock> exposureLockModeIsUpdatedAt(CameraExposureLock.Mode mode,
                                                                          double centerX, double centerY,
                                                                          double width, double height) {
        return allOf(
                exposureLockModeIs(mode, centerX, centerY, width, height),
                exposureLockModeIsUpToDate());
    }

    public static Matcher<CameraExposureLock> exposureLockModeIsUpdatedAt(CameraExposureLock.Mode mode) {
        return allOf(
                exposureLockModeIs(mode),
                exposureLockModeIsUpToDate());
    }

    private static Matcher<CameraExposureLock> exposureLockModeIsUpdating() {
        return new FeatureMatcher<CameraExposureLock, Boolean>(equalTo(true), "updating is", "updating") {

            @Override
            protected Boolean featureValueOf(CameraExposureLock actual) {
                return actual.isUpdating();
            }
        };
    }

    private static Matcher<CameraExposureLock> exposureLockModeIsUpToDate() {
        return new FeatureMatcher<CameraExposureLock, Boolean>(equalTo(false), "updating is", "updating") {

            @Override
            protected Boolean featureValueOf(CameraExposureLock actual) {
                return actual.isUpdating();
            }
        };
    }

    private CameraExposureLockMatcher() {
    }
}