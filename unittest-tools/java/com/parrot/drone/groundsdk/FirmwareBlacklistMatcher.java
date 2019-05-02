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

import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.engine.firmware.FirmwareBlackListCore;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.equalTo;

/**
 * FirmwareBlackListCore matcher
 */

public final class FirmwareBlacklistMatcher {

    public static Matcher<FirmwareBlackListCore> isBlacklisted(DeviceModel product, String version) {
        FirmwareVersion firmwareVersion = FirmwareVersion.parse(version);
        assert firmwareVersion != null;
        FirmwareIdentifier firmwareIdentifier = new FirmwareIdentifier(product, firmwareVersion);
        return new FeatureMatcher<FirmwareBlackListCore, Boolean>(equalTo(true), "blacklisted", "blacklisted") {

            @Override
            protected Boolean featureValueOf(FirmwareBlackListCore actual) {
                return actual.isFirmwareBlacklisted(firmwareIdentifier);
            }
        };
    }

    public static Matcher<FirmwareBlackListCore> notBlacklisted(DeviceModel product, String version) {
        FirmwareVersion firmwareVersion = FirmwareVersion.parse(version);
        assert firmwareVersion != null;
        FirmwareIdentifier firmwareIdentifier = new FirmwareIdentifier(product, firmwareVersion);
        return new FeatureMatcher<FirmwareBlackListCore, Boolean>(equalTo(false), "blacklisted", "blacklisted") {

            @Override
            protected Boolean featureValueOf(FirmwareBlackListCore actual) {
                return actual.isFirmwareBlacklisted(firmwareIdentifier);
            }
        };
    }

    private FirmwareBlacklistMatcher() {
    }
}
