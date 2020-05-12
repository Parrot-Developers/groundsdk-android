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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposure;

import org.hamcrest.Matcher;

import java.util.Set;

import static com.parrot.drone.groundsdk.MatcherBuilders.enumSetFeatureMatcher;

public final class ExposureSettingMatcher {

    public static Matcher<CameraExposure.Setting> exposureSettingSupportsModes(
            @NonNull Set<CameraExposure.Mode> modes) {
        return enumSetFeatureMatcher(modes, "modes", CameraExposure.Setting::supportedModes);
    }

    public static Matcher<CameraExposure.Setting> exposureSettingSupportsManualShutterSpeeds(
            @NonNull Set<CameraExposure.ShutterSpeed> speeds) {
        return enumSetFeatureMatcher(speeds, "manualShutterSpeeds",
                CameraExposure.Setting::supportedManualShutterSpeeds);
    }

    public static Matcher<CameraExposure.Setting> exposureSettingSupportsManualIsos(
            @NonNull Set<CameraExposure.IsoSensitivity> isos) {
        return enumSetFeatureMatcher(isos, "manualIsos",
                CameraExposure.Setting::supportedManualIsoSensitivities);
    }

    public static Matcher<CameraExposure.Setting> exposureSettingSupportsMaxIsos(
            @NonNull Set<CameraExposure.IsoSensitivity> isos) {
        return enumSetFeatureMatcher(isos, "maxIsos",
                CameraExposure.Setting::supportedMaximumIsoSensitivities);
    }

    public static Matcher<CameraExposure.Setting> exposureSettingSupportsAutoExposureMeteringMode(
            @NonNull Set<CameraExposure.AutoExposureMeteringMode> modes) {
        return enumSetFeatureMatcher(modes, "autoExposureMeteringModes",
                CameraExposure.Setting::supportedAutoExposureMeteringModes);
    }

    public static Matcher<CameraExposure.Setting> exposureSettingModeIs(
            @NonNull CameraExposure.Mode mode) {
        return MatcherBuilders.valueMatcher(mode, "mode", CameraExposure.Setting::mode);
    }

    public static Matcher<CameraExposure.Setting> exposureSettingManualShutterSpeedIs(
            @NonNull CameraExposure.ShutterSpeed speed) {
        return MatcherBuilders.valueMatcher(speed, "manualShutterSpeed", CameraExposure.Setting::manualShutterSpeed);
    }

    public static Matcher<CameraExposure.Setting> exposureSettingManualIsoIs(
            @NonNull CameraExposure.IsoSensitivity iso) {
        return MatcherBuilders.valueMatcher(iso, "manualIso", CameraExposure.Setting::manualIsoSensitivity);
    }

    public static Matcher<CameraExposure.Setting> exposureSettingMaxIsoIs(
            @NonNull CameraExposure.IsoSensitivity iso) {
        return MatcherBuilders.valueMatcher(iso, "maxIso", CameraExposure.Setting::maxIsoSensitivity);
    }

    public static Matcher<CameraExposure.Setting> exposureSettingAutoExposureMeteringModeIs(
            @NonNull CameraExposure.AutoExposureMeteringMode mode) {
        return MatcherBuilders.valueMatcher(mode, "autoExposureMeteringMode",
                CameraExposure.Setting::autoExposureMeteringMode);
    }

    private ExposureSettingMatcher() {
    }
}
