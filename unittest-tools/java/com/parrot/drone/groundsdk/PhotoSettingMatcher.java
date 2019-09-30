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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto;
import com.parrot.drone.groundsdk.value.DoubleRange;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.HashSet;
import java.util.Set;

import static com.parrot.drone.groundsdk.MatcherBuilders.enumSetFeatureMatcher;
import static com.parrot.drone.groundsdk.MatcherBuilders.valueMatcher;

public final class PhotoSettingMatcher {

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsModes(
            @NonNull Set<CameraPhoto.Mode> modes) {
        return enumSetFeatureMatcher(modes, "modes", CameraPhoto.Setting::supportedModes);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsFormats(
            @NonNull Set<CameraPhoto.Format> formats) {
        return enumSetFeatureMatcher(formats, "formats", CameraPhoto.Setting::supportedFormats);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsFormats(
            @NonNull CameraPhoto.Mode mode, @NonNull Set<CameraPhoto.Format> formats) {
        return enumSetFeatureMatcher(formats, "formats", it -> it.supportedFormatsFor(mode));
    }

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsFormats(
            @NonNull Set<CameraPhoto.Mode> modes, @NonNull Set<CameraPhoto.Format> formats) {
        Set<Matcher<? super CameraPhoto.Setting>> matchers = new HashSet<>();
        for (CameraPhoto.Mode mode : modes) {
            matchers.add(photoSettingSupportsFormats(mode, formats));
        }
        return Matchers.allOf(matchers);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsFileFormats(
            @NonNull Set<CameraPhoto.FileFormat> fileFormats) {
        return enumSetFeatureMatcher(fileFormats, "fileFormats", CameraPhoto.Setting::supportedFileFormats);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsFileFormats(
            @NonNull CameraPhoto.Format format, @NonNull Set<CameraPhoto.FileFormat> fileFormats) {
        return enumSetFeatureMatcher(fileFormats, "fileFormats", it -> it.supportedFileFormatsFor(format));
    }

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsFileFormats(
            @NonNull Set<CameraPhoto.Format> formats, @NonNull Set<CameraPhoto.FileFormat> fileFormats) {
        Set<Matcher<? super CameraPhoto.Setting>> matchers = new HashSet<>();
        for (CameraPhoto.Format format : formats) {
            matchers.add(photoSettingSupportsFileFormats(format, fileFormats));
        }
        return Matchers.allOf(matchers);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsFileFormats(
            @NonNull CameraPhoto.Mode mode, @NonNull CameraPhoto.Format format,
            @NonNull Set<CameraPhoto.FileFormat> fileFormats) {
        return enumSetFeatureMatcher(fileFormats, "fileFormats", it -> it.supportedFileFormatsFor(mode, format));
    }

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsFileFormats(
            @NonNull Set<CameraPhoto.Mode> modes, @NonNull CameraPhoto.Format format,
            @NonNull Set<CameraPhoto.FileFormat> fileFormats) {
        Set<Matcher<? super CameraPhoto.Setting>> matchers = new HashSet<>();
        for (CameraPhoto.Mode mode : modes) {
            matchers.add(photoSettingSupportsFileFormats(mode, format, fileFormats));
        }
        return Matchers.allOf(matchers);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsFileFormats(
            @NonNull CameraPhoto.Mode mode, @NonNull Set<CameraPhoto.Format> formats,
            @NonNull Set<CameraPhoto.FileFormat> fileFormats) {
        Set<Matcher<? super CameraPhoto.Setting>> matchers = new HashSet<>();
        for (CameraPhoto.Format format : formats) {
            matchers.add(photoSettingSupportsFileFormats(mode, format, fileFormats));
        }
        return Matchers.allOf(matchers);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsFileFormats(
            @NonNull Set<CameraPhoto.Mode> modes, @NonNull Set<CameraPhoto.Format> formats,
            @NonNull Set<CameraPhoto.FileFormat> fileFormats) {
        Set<Matcher<? super CameraPhoto.Setting>> matchers = new HashSet<>();
        for (CameraPhoto.Mode mode : modes) {
            matchers.add(photoSettingSupportsFileFormats(mode, formats, fileFormats));
        }
        return Matchers.allOf(matchers);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsBurstValues(
            @NonNull Set<CameraPhoto.BurstValue> burstValues) {
        return enumSetFeatureMatcher(burstValues, "burstValues", CameraPhoto.Setting::supportedBurstValues);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingSupportsBracketingValues(
            @NonNull Set<CameraPhoto.BracketingValue> bracketingValues) {
        return enumSetFeatureMatcher(bracketingValues, "bracketingValues",
                CameraPhoto.Setting::supportedBracketingValues);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingTimelapseIntervalRangeIs(DoubleRange range) {
        return valueMatcher(range, "timelapseIntervalRange", CameraPhoto.Setting::timelapseIntervalRange);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingGpslapseIntervalRangeIs(DoubleRange range) {
        return valueMatcher(range, "gpslapseIntervalRange", CameraPhoto.Setting::gpslapseIntervalRange);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingModeIs(
            @NonNull CameraPhoto.Mode mode) {
        return valueMatcher(mode, "mode", CameraPhoto.Setting::mode);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingFormatIs(
            @NonNull CameraPhoto.Format format) {
        return valueMatcher(format, "format", CameraPhoto.Setting::format);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingFileFormatIs(
            @NonNull CameraPhoto.FileFormat fileFormat) {
        return valueMatcher(fileFormat, "fileFormat", CameraPhoto.Setting::fileFormat);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingBurstValueIs(
            @NonNull CameraPhoto.BurstValue burst) {
        return valueMatcher(burst, "burstValue", CameraPhoto.Setting::burstValue);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingBracketingValueIs(
            @NonNull CameraPhoto.BracketingValue bracketing) {
        return valueMatcher(bracketing, "bracketingValue", CameraPhoto.Setting::bracketingValue);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingTimelapseIntervalIs(double interval) {
        return valueMatcher(interval, "timelapseInterval", CameraPhoto.Setting::timelapseInterval);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingGpslapseIntervalIs(double interval) {
        return valueMatcher(interval, "gpslapseInterval", CameraPhoto.Setting::gpslapseInterval);
    }

    public static Matcher<CameraPhoto.Setting> photoSettingHdrAvailableIs(
            @NonNull CameraPhoto.Mode mode, @NonNull CameraPhoto.Format format,
            @NonNull CameraPhoto.FileFormat fileFormat, boolean hdr) {
        return valueMatcher(hdr, "hdr", it -> it.isHdrAvailable(mode, format, fileFormat));
    }

    public static Matcher<CameraPhoto.Setting> photoSettingHdrAvailableIs(boolean hdr) {
        return valueMatcher(hdr, "hdr", CameraPhoto.Setting::isHdrAvailable);
    }

    private PhotoSettingMatcher() {
    }
}
