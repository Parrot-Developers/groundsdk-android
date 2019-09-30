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

import com.parrot.drone.groundsdk.device.peripheral.DevToolbox;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static org.hamcrest.Matchers.equalTo;

/**
 * DebugSetting matcher
 */
@SuppressWarnings({"unused", "UtilityClassWithoutPrivateConstructor"})
public final class DebugSettingMatcher {

    public static Matcher<DevToolbox.DebugSetting> hasName(@NonNull String name) {
        return new FeatureMatcher<DevToolbox.DebugSetting, String>(equalTo(name), "name", "name") {

            @Override
            protected String featureValueOf(DevToolbox.DebugSetting debugSetting) {
                return debugSetting.getName();
            }
        };
    }

    public static Matcher<DevToolbox.DebugSetting> isReadOnly(boolean readOnly) {
        return new FeatureMatcher<DevToolbox.DebugSetting, Boolean>(equalTo(readOnly), "readOnly", "readOnly") {

            @Override
            protected Boolean featureValueOf(DevToolbox.DebugSetting debugSetting) {
                return debugSetting.isReadOnly();
            }
        };
    }

    public static Matcher<DevToolbox.DebugSetting> isUpdating(boolean updating) {
        return new FeatureMatcher<DevToolbox.DebugSetting, Boolean>(equalTo(updating), "updating", "updating") {

            @Override
            protected Boolean featureValueOf(DevToolbox.DebugSetting debugSetting) {
                return debugSetting.isUpdating();
            }
        };
    }

    public static Matcher<DevToolbox.DebugSetting> is(@NonNull DevToolbox.DebugSetting.Type type) {
        return new FeatureMatcher<DevToolbox.DebugSetting, DevToolbox.DebugSetting.Type>(
                equalTo(type), "type", "type") {

            @Override
            protected DevToolbox.DebugSetting.Type featureValueOf(DevToolbox.DebugSetting debugSetting) {
                return debugSetting.getType();
            }
        };
    }

    public static Matcher<DevToolbox.DebugSetting> hasValue(boolean value) {
        return Matchers.allOf(
                is(DevToolbox.DebugSetting.Type.BOOLEAN),
                new FeatureMatcher<DevToolbox.DebugSetting, Boolean>(equalTo(value),
                        "value", "value") {

                    @Override
                    protected Boolean featureValueOf(DevToolbox.DebugSetting debugSetting) {
                        return debugSetting.as(DevToolbox.BooleanDebugSetting.class).getValue();
                    }
                }
        );
    }

    public static Matcher<DevToolbox.DebugSetting> hasValue(double value) {
        return Matchers.allOf(
                is(DevToolbox.DebugSetting.Type.NUMERIC),
                new FeatureMatcher<DevToolbox.DebugSetting, Double>(equalTo(value),
                        "value", "value") {

                    @Override
                    protected Double featureValueOf(DevToolbox.DebugSetting debugSetting) {
                        return debugSetting.as(DevToolbox.NumericDebugSetting.class).getValue();
                    }
                }
        );
    }

    public static Matcher<DevToolbox.DebugSetting> hasValue(@NonNull String value) {
        return Matchers.allOf(
                is(DevToolbox.DebugSetting.Type.TEXT),
                new FeatureMatcher<DevToolbox.DebugSetting, String>(equalTo(value),
                        "value", "value") {

                    @Override
                    protected String featureValueOf(DevToolbox.DebugSetting debugSetting) {
                        return debugSetting.as(DevToolbox.TextDebugSetting.class).getValue();
                    }
                }
        );
    }

    public static Matcher<DevToolbox.DebugSetting> hasRange(boolean hasRange) {
        return Matchers.allOf(
                is(DevToolbox.DebugSetting.Type.NUMERIC),
                new FeatureMatcher<DevToolbox.DebugSetting, Boolean>(equalTo(hasRange),
                        "hasRange", "hasRange") {

                    @Override
                    protected Boolean featureValueOf(DevToolbox.DebugSetting debugSetting) {
                        return debugSetting.as(DevToolbox.NumericDebugSetting.class).hasRange();
                    }
                }
        );
    }

    public static Matcher<DevToolbox.DebugSetting> hasRange(double rangeMin, double rangeMax) {
        return Matchers.allOf(
                hasRange(true),
                new FeatureMatcher<DevToolbox.DebugSetting, Double>(equalTo(rangeMin),
                        "has range min", "has range min") {

                    @Override
                    protected Double featureValueOf(DevToolbox.DebugSetting debugSetting) {
                        return debugSetting.as(DevToolbox.NumericDebugSetting.class).getRangeMin();
                    }
                },
                new FeatureMatcher<DevToolbox.DebugSetting, Double>(equalTo(rangeMax),
                        "has range max", "has range max") {

                    @Override
                    protected Double featureValueOf(DevToolbox.DebugSetting debugSetting) {
                        return debugSetting.as(DevToolbox.NumericDebugSetting.class).getRangeMax();
                    }
                }
        );
    }

    public static Matcher<DevToolbox.DebugSetting> hasStep(boolean hasStep) {
        return Matchers.allOf(
                is(DevToolbox.DebugSetting.Type.NUMERIC),
                new FeatureMatcher<DevToolbox.DebugSetting, Boolean>(equalTo(hasStep),
                        "hasStep", "hasStep") {

                    @Override
                    protected Boolean featureValueOf(DevToolbox.DebugSetting debugSetting) {
                        return debugSetting.as(DevToolbox.NumericDebugSetting.class).hasStep();
                    }
                }
        );
    }

    public static Matcher<DevToolbox.DebugSetting> hasStep(double step) {
        return Matchers.allOf(
                hasStep(true),
                new FeatureMatcher<DevToolbox.DebugSetting, Double>(equalTo(step),
                        "step", "step") {

                    @Override
                    protected Double featureValueOf(DevToolbox.DebugSetting debugSetting) {
                        return debugSetting.as(DevToolbox.NumericDebugSetting.class).getStep();
                    }
                }
        );
    }
}