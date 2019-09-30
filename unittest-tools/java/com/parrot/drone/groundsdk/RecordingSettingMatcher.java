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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraRecording;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static com.parrot.drone.groundsdk.MatcherBuilders.enumSetFeatureMatcher;
import static com.parrot.drone.groundsdk.MatcherBuilders.valueMatcher;

public final class RecordingSettingMatcher {

    public static Matcher<CameraRecording.Setting> recordingSettingSupportsModes(
            @NonNull Set<CameraRecording.Mode> modes) {
        return enumSetFeatureMatcher(modes, "modes", CameraRecording.Setting::supportedModes);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingSupportsResolutions(
            @NonNull Set<CameraRecording.Resolution> resolutions) {
        return enumSetFeatureMatcher(resolutions, "resolutions",
                CameraRecording.Setting::supportedResolutions);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingSupportsResolutions(
            @NonNull CameraRecording.Mode mode, @NonNull Set<CameraRecording.Resolution> resolutions) {
        return enumSetFeatureMatcher(resolutions, "resolutions", it -> it.supportedResolutionsFor(mode));
    }

    public static Matcher<CameraRecording.Setting> recordingSettingSupportsResolutions(
            @NonNull EnumSet<CameraRecording.Mode> modes, @NonNull Set<CameraRecording.Resolution> resolutions) {
        Set<Matcher<? super CameraRecording.Setting>> matchers = new HashSet<>();
        for (CameraRecording.Mode mode : modes) {
            matchers.add(recordingSettingSupportsResolutions(mode, resolutions));
        }
        return Matchers.allOf(matchers);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingSupportsFramerates(
            @NonNull Set<CameraRecording.Framerate> framerates) {
        return enumSetFeatureMatcher(framerates, "framerates", CameraRecording.Setting::supportedFramerates);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingSupportsFramerates(
            @NonNull CameraRecording.Resolution resolution, @NonNull Set<CameraRecording.Framerate> framerates) {
        return enumSetFeatureMatcher(framerates, "framerates", it -> it.supportedFrameratesFor(resolution));
    }

    public static Matcher<CameraRecording.Setting> recordingSettingSupportsFramerates(
            @NonNull Set<CameraRecording.Resolution> resolutions, @NonNull Set<CameraRecording.Framerate> framerates) {
        Set<Matcher<? super CameraRecording.Setting>> matchers = new HashSet<>();
        for (CameraRecording.Resolution resolution : resolutions) {
            matchers.add(recordingSettingSupportsFramerates(resolution, framerates));
        }
        return Matchers.allOf(matchers);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingSupportsFramerates(
            @NonNull CameraRecording.Mode mode, @NonNull CameraRecording.Resolution resolution,
            @NonNull Set<CameraRecording.Framerate> framerates) {
        return enumSetFeatureMatcher(framerates, "framerates", it -> it.supportedFrameratesFor(mode, resolution));
    }

    public static Matcher<CameraRecording.Setting> recordingSettingSupportsFramerates(
            @NonNull Set<CameraRecording.Mode> modes, @NonNull CameraRecording.Resolution resolution,
            @NonNull Set<CameraRecording.Framerate> framerates) {
        Set<Matcher<? super CameraRecording.Setting>> matchers = new HashSet<>();
        for (CameraRecording.Mode mode : modes) {
            matchers.add(recordingSettingSupportsFramerates(mode, resolution, framerates));
        }
        return Matchers.allOf(matchers);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingSupportsFramerates(
            @NonNull CameraRecording.Mode mode, @NonNull Set<CameraRecording.Resolution> resolutions,
            @NonNull Set<CameraRecording.Framerate> framerates) {
        Set<Matcher<? super CameraRecording.Setting>> matchers = new HashSet<>();
        for (CameraRecording.Resolution resolution : resolutions) {
            matchers.add(recordingSettingSupportsFramerates(mode, resolution, framerates));
        }
        return Matchers.allOf(matchers);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingSupportsFramerates(
            @NonNull Set<CameraRecording.Mode> modes, @NonNull Set<CameraRecording.Resolution> resolutions,
            @NonNull Set<CameraRecording.Framerate> framerates) {
        Set<Matcher<? super CameraRecording.Setting>> matchers = new HashSet<>();
        for (CameraRecording.Mode mode : modes) {
            recordingSettingSupportsFramerates(mode, resolutions, framerates);
        }
        return Matchers.allOf(matchers);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingSupportsHyperlapseValues(
            @NonNull Set<CameraRecording.HyperlapseValue> hyperlapseValues) {
        return enumSetFeatureMatcher(hyperlapseValues, "hyperlapseValues",
                CameraRecording.Setting::supportedHyperlapseValues);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingModeIs(
            @NonNull CameraRecording.Mode mode) {
        return valueMatcher(mode, "mode", CameraRecording.Setting::mode);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingResolutionIs(
            @NonNull CameraRecording.Resolution resolution) {
        return valueMatcher(resolution, "resolution", CameraRecording.Setting::resolution);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingFramerateIs(
            @NonNull CameraRecording.Framerate framerate) {
        return valueMatcher(framerate, "framerate", CameraRecording.Setting::framerate);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingHyperlapseValueIs(
            @NonNull CameraRecording.HyperlapseValue hyperlapse) {
        return valueMatcher(hyperlapse, "hyperlapseValue", CameraRecording.Setting::hyperlapseValue);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingHdrAvailableIs(
            @NonNull CameraRecording.Mode mode, @NonNull CameraRecording.Resolution resolution,
            @NonNull CameraRecording.Framerate framerate, boolean hdr) {
        return valueMatcher(hdr, "hdr", it -> it.isHdrAvailable(mode, resolution, framerate));
    }

    public static Matcher<CameraRecording.Setting> recordingSettingHdrAvailableIs(boolean hdr) {
        return valueMatcher(hdr, "hdr", CameraRecording.Setting::isHdrAvailable);
    }

    public static Matcher<CameraRecording.Setting> recordingSettingBitrateIs(int bitrate) {
        return valueMatcher(bitrate, "bitrate", CameraRecording.Setting::bitrate);
    }

    private RecordingSettingMatcher() {
    }
}
