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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.Updater;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;

import org.hamcrest.Matcher;

import static com.parrot.drone.groundsdk.MatcherBuilders.valueMatcher;

final class DownloadMatcher {

    @NonNull
    static Matcher<Updater.Download> firmwareDownloadStateIs(@NonNull Updater.Download.State state) {
        return valueMatcher(state, "state", Updater.Download::state);
    }

    @NonNull
    static Matcher<Updater.Download> firmwareDownloadCurrentFirmwareIs(@NonNull FirmwareInfo firmware) {
        return valueMatcher(firmware, "currentFirmware", Updater.Download::currentFirmware);
    }

    @NonNull
    static Matcher<Updater.Download> firmwareDownloadIndexIs(int index) {
        return valueMatcher(index, "index", Updater.Download::currentFirmwareIndex);
    }

    @NonNull
    static Matcher<Updater.Download> firmwareDownloadCurrentProgressIs(int progress) {
        return valueMatcher(progress, "progress", Updater.Download::currentFirmwareProgress);
    }

    @NonNull
    static Matcher<Updater.Download> firmwareDownloadTotalCountIs(int total) {
        return valueMatcher(total, "total", Updater.Download::totalFirmwareCount);
    }

    @NonNull
    static Matcher<Updater.Download> firmwareDownloadTotalProgressIs(int progress) {
        return valueMatcher(progress, "totalProgress", Updater.Download::totalProgress);
    }

    private DownloadMatcher() {
    }
}
