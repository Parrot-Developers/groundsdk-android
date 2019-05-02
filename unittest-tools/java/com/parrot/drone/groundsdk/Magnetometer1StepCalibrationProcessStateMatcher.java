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

import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith1StepCalibration.CalibrationProcessState;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.IsEqual.equalTo;

public final class Magnetometer1StepCalibrationProcessStateMatcher {

    public static Matcher<CalibrationProcessState> is(int rollProgress,
                                                      int pitchProgress,
                                                      int yawProgress) {
        return Matchers.allOf(
                new FeatureMatcher<CalibrationProcessState, Integer>(equalTo(rollProgress),
                        "roll progress is", "roll progress") {

                    @Override
                    protected Integer featureValueOf(CalibrationProcessState state) {
                        return state.rollProgress();
                    }
                },
                new FeatureMatcher<CalibrationProcessState, Integer>(equalTo(pitchProgress),
                        "pitch progress is", "pitch progress") {

                    @Override
                    protected Integer featureValueOf(CalibrationProcessState state) {
                        return state.pitchProgress();
                    }
                },
                new FeatureMatcher<CalibrationProcessState, Integer>(equalTo(yawProgress),
                        "yaw progress is", "yaw progress") {

                    @Override
                    protected Integer featureValueOf(CalibrationProcessState state) {
                        return state.yawProgress();
                    }
                }
        );
    }

    public static Matcher<CalibrationProcessState> isInRange() {
        return Matchers.allOf(
                new FeatureMatcher<CalibrationProcessState, Integer>(
                        both(greaterThanOrEqualTo(0)).and(lessThanOrEqualTo(100)),
                        "roll progress is", "roll progress") {

                    @Override
                    protected Integer featureValueOf(CalibrationProcessState state) {
                        return state.rollProgress();
                    }
                },
                new FeatureMatcher<CalibrationProcessState, Integer>(
                        both(greaterThanOrEqualTo(0)).and(lessThanOrEqualTo(100)),
                        "pitch progress is", "pitch progress") {

                    @Override
                    protected Integer featureValueOf(CalibrationProcessState state) {
                        return state.pitchProgress();
                    }
                },
                new FeatureMatcher<CalibrationProcessState, Integer>(
                        both(greaterThanOrEqualTo(0)).and(lessThanOrEqualTo(100)),
                        "yaw progress is", "yaw progress") {

                    @Override
                    protected Integer featureValueOf(CalibrationProcessState state) {
                        return state.yawProgress();
                    }
                }
        );
    }

    private Magnetometer1StepCalibrationProcessStateMatcher() {
    }
}
