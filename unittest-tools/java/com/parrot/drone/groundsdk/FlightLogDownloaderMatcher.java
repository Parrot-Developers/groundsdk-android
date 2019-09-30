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

import com.parrot.drone.groundsdk.device.peripheral.FlightLogDownloader;

import org.hamcrest.Matcher;

import static com.parrot.drone.groundsdk.MatcherBuilders.featureMatcher;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * FlightLogDownloader matcher
 */
public final class FlightLogDownloaderMatcher {


    public static Matcher<FlightLogDownloader> isIdle() {
        return allOf(not(isDownloading()), hasCompletionStatus(FlightLogDownloader.CompletionStatus.NONE));
    }

    public static Matcher<FlightLogDownloader> isDownloading(int downloadedCount) {
        return allOf(isDownloading(), hasCompletionStatus(FlightLogDownloader.CompletionStatus.NONE),
                hasDownloadedCount(downloadedCount));
    }

    public static Matcher<FlightLogDownloader> hasDownloadedSuccessfully(int downloadedCount) {
        return allOf(not(isDownloading()),
                hasCompletionStatus(FlightLogDownloader.CompletionStatus.SUCCESS),
                hasDownloadedCount(downloadedCount));
    }

    public static Matcher<FlightLogDownloader> wasInterruptedAfter(int downloadedCount) {
        return allOf(not(isDownloading()),
                hasCompletionStatus(FlightLogDownloader.CompletionStatus.INTERRUPTED),
                hasDownloadedCount(downloadedCount));
    }

    private static Matcher<FlightLogDownloader> isDownloading() {
        return featureMatcher(equalTo(true), "isDownloading", FlightLogDownloader::isDownloading);
    }

    private static Matcher<FlightLogDownloader> hasCompletionStatus(
            @NonNull FlightLogDownloader.CompletionStatus status) {
        return featureMatcher(equalTo(status), "completionStatus", FlightLogDownloader::getCompletionStatus);
    }

    private static Matcher<FlightLogDownloader> hasDownloadedCount(int downloadedCount) {
        return featureMatcher(equalTo(downloadedCount), "downloadCount", FlightLogDownloader::getLatestDownloadCount);
    }

    private FlightLogDownloaderMatcher() {
    }

}
