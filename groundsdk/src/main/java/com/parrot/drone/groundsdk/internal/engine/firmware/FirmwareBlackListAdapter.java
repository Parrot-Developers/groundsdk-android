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

import androidx.annotation.NonNull;

import com.google.gson.JsonParseException;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.internal.http.HttpFirmwaresInfo;

import java.util.Set;

/**
 * Utility class to adapt {@link HttpFirmwaresInfo.BlackListEntry} to {@link FirmwareBlackListCore} entries.
 */
final class FirmwareBlackListAdapter {

    /**
     * Converts and add a {@code HttpFirmwaresInfo.BlackListEntry} to a set of {@link FirmwareIdentifier}.
     *
     * @param blacklist          destination set of versions
     * @param httpBlackListEntry HTTP firmware blacklist to convert
     *
     * @throws JsonParseException in case conversion fails
     */
    static void addVersions(@NonNull Set<FirmwareIdentifier> blacklist,
                            @NonNull HttpFirmwaresInfo.BlackListEntry httpBlackListEntry) {
        Validation.require("product", httpBlackListEntry.productId);
        Validation.require("versions", httpBlackListEntry.versions);

        DeviceModel product = Validation.validateModel("product", httpBlackListEntry.productId);
        for (String version : httpBlackListEntry.versions) {
            blacklist.add(new FirmwareIdentifier(product, Validation.validateVersion("versions", version)));
        }
    }

    /**
     * Private constructor for static utility class.
     */
    private FirmwareBlackListAdapter() {
    }
}
