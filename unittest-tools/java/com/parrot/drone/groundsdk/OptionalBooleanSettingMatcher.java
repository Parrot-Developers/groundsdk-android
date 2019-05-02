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

import com.parrot.drone.groundsdk.value.OptionalBooleanSetting;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static com.parrot.drone.groundsdk.MatcherBuilders.valueMatcher;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;

/**
 * Optional boolean setting matcher
 */
public final class OptionalBooleanSettingMatcher {

    public static Matcher<OptionalBooleanSetting> optionalBooleanSettingValueIs(boolean enabled) {
        return valueMatcher(enabled, "enabled", OptionalBooleanSetting::isEnabled);
    }

    public static Matcher<OptionalBooleanSetting> optionalBooleanSettingIsDisabled() {
        return Matchers.both(optionalBooleanSettingValueIs(false)).and(settingIsUpToDate());
    }

    public static Matcher<OptionalBooleanSetting> optionalBooleanSettingIsDisabling() {
        return Matchers.both(optionalBooleanSettingValueIs(false)).and(settingIsUpdating());
    }

    public static Matcher<OptionalBooleanSetting> optionalBooleanSettingIsEnabling() {
        return Matchers.both(optionalBooleanSettingValueIs(true)).and(settingIsUpdating());
    }

    public static Matcher<OptionalBooleanSetting> optionalBooleanSettingIsEnabled() {
        return Matchers.both(optionalBooleanSettingValueIs(true)).and(settingIsUpToDate());
    }

    private OptionalBooleanSettingMatcher() {
    }
}
