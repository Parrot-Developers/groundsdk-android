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

import com.parrot.drone.groundsdk.value.DoubleSetting;

import org.hamcrest.Matcher;

import static com.parrot.drone.groundsdk.MatcherBuilders.valueMatcher;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.Matchers.allOf;

/**
 * Double setting matcher
 */
public final class DoubleSettingMatcher {

    public static Matcher<DoubleSetting> doubleSettingValueIs(double min, double value, double max) {
        return allOf(
                valueMatcher(min, "min", DoubleSetting::getMin),
                valueMatcher(value, "value", DoubleSetting::getValue),
                valueMatcher(max, "max", DoubleSetting::getMax));
    }

    public static Matcher<DoubleSetting> doubleSettingIsUpdatingTo(double min, double value, double max) {
        return allOf(
                settingIsUpdating(),
                doubleSettingValueIs(min, value, max));
    }

    public static Matcher<DoubleSetting> doubleSettingIsUpToDateAt(double min, double value, double max) {
        return allOf(
                settingIsUpToDate(),
                doubleSettingValueIs(min, value, max));
    }

    private DoubleSettingMatcher() {
    }
}