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

package com.parrot.drone.groundsdk.internal.engine.firmware;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;

import org.hamcrest.Matcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static com.parrot.drone.groundsdk.MatcherBuilders.featureMatcher;
import static com.parrot.drone.groundsdk.MatcherBuilders.valueMatcher;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

final class TaskMatcher {

    static Matcher<FirmwareDownloader.Task> firmwareDownloaderTaskStateIs(
            @NonNull FirmwareDownloader.Task.State state) {
        return valueMatcher(state, "state", FirmwareDownloader.Task::state);
    }

    static Matcher<FirmwareDownloader.Task> firmwareDownloaderTaskRemainIs(
            @NonNull Collection<FirmwareInfo> firmwares) {
        return featureMatcher(firmwares.isEmpty() ?
                        empty() : contains(firmwares.toArray(new FirmwareInfo[0])),
                "remaining", FirmwareDownloader.Task::remaining);
    }

    static Matcher<FirmwareDownloader.Task> firmwareDownloaderTaskRemainIs(FirmwareInfo... firmwares) {
        return firmwareDownloaderTaskRemainIs(firmwares == null ? Collections.emptyList() : Arrays.asList(firmwares));
    }

    static Matcher<FirmwareDownloader.Task> firmwareDownloaderTaskDownloaded(
            @NonNull Collection<FirmwareInfo> firmwares) {
        return featureMatcher(firmwares.isEmpty() ?
                        empty() : contains(firmwares.toArray(new FirmwareInfo[0])),
                "downloaded", FirmwareDownloader.Task::downloaded);
    }

    static Matcher<FirmwareDownloader.Task> firmwareDownloaderTaskDownloaded(FirmwareInfo... firmwares) {
        return firmwareDownloaderTaskDownloaded(firmwares == null ? Collections.emptyList() : Arrays.asList(firmwares));
    }

    static Matcher<FirmwareDownloader.Task> firmwareDownloaderTaskCurrentIs(@Nullable FirmwareInfo firmware) {
        return valueMatcher(firmware, "current", FirmwareDownloader.Task::current);
    }

    static Matcher<FirmwareDownloader.Task> firmwareDownloaderTaskCurrentDownloadProgressIs(
            @IntRange(from = 0, to = 100) int progress) {
        return valueMatcher(progress, "currentProgress", FirmwareDownloader.Task::currentProgress);
    }

    static Matcher<FirmwareDownloader.Task> firmwareDownloaderTaskOverallProgressIs(
            @IntRange(from = 0, to = 100) int progress) {
        return valueMatcher(progress, "overallProgress", FirmwareDownloader.Task::overallProgress);
    }

    static Matcher<FirmwareDownloader.Task> firmwareDownloaderTaskCurrentCountIs(int count) {
        return valueMatcher(count, "currentCount", FirmwareDownloader.Task::currentCount);
    }

    static Matcher<FirmwareDownloader.Task> firmwareDownloaderTaskTotalCountIs(int count) {
        return valueMatcher(count, "totalCount", FirmwareDownloader.Task::totalCount);
    }

    private TaskMatcher() {
    }
}
