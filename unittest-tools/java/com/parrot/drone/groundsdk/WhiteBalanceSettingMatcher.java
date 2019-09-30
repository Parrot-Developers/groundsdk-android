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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalance;

import org.hamcrest.Matcher;

import java.util.Set;

import static com.parrot.drone.groundsdk.MatcherBuilders.enumSetFeatureMatcher;

public final class WhiteBalanceSettingMatcher {

    public static Matcher<CameraWhiteBalance.Setting> whiteBalanceSettingSupportsModes(
            @NonNull Set<CameraWhiteBalance.Mode> modes) {
        return enumSetFeatureMatcher(modes, "modes", CameraWhiteBalance.Setting::supportedModes);
    }

    public static Matcher<CameraWhiteBalance.Setting> whiteBalanceSettingSupportsTemperatures(
            @NonNull Set<CameraWhiteBalance.Temperature> temperatures) {
        return enumSetFeatureMatcher(temperatures, "customTemperatures",
                CameraWhiteBalance.Setting::supportedCustomTemperatures);
    }

    public static Matcher<CameraWhiteBalance.Setting> whiteBalanceSettingModeIs(
            @NonNull CameraWhiteBalance.Mode mode) {
        return MatcherBuilders.valueMatcher(mode, "mode", CameraWhiteBalance.Setting::mode);
    }

    public static Matcher<CameraWhiteBalance.Setting> whiteBalanceSettingTemperatureIs(
            @NonNull CameraWhiteBalance.Temperature temperature) {
        return MatcherBuilders.valueMatcher(temperature, "customTemperature",
                CameraWhiteBalance.Setting::customTemperature);
    }

    private WhiteBalanceSettingMatcher() {
    }
}
