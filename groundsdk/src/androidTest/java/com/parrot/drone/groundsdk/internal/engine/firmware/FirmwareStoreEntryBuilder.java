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
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;

import java.net.URI;
import java.util.EnumSet;
import java.util.Set;

final class FirmwareStoreEntryBuilder {

    @Nullable
    private DeviceModel mModel;

    @Nullable
    private FirmwareVersion mVersion;

    private long mSize;

    private URI mRemoteUri;

    private FirmwareVersion mMinVersion;

    private FirmwareVersion mMaxVersion;

    @NonNull
    private final Set<FirmwareInfo.Attribute> mAttributes = EnumSet.noneOf(FirmwareInfo.Attribute.class);

    @NonNull
    FirmwareStoreEntryBuilder firmware(@NonNull FirmwareIdentifier firmware) {
        mModel = firmware.getDeviceModel();
        mVersion = firmware.getVersion();
        return this;
    }

    @NonNull
    FirmwareStoreEntryBuilder model(@NonNull DeviceModel model) {
        mModel = model;
        return this;
    }

    @NonNull
    FirmwareStoreEntryBuilder version(@NonNull String version) {
        mVersion = FirmwareVersion.parse(version);
        return this;
    }

    @NonNull
    FirmwareStoreEntryBuilder size(long size) {
        mSize = size;
        return this;
    }

    @NonNull
    FirmwareStoreEntryBuilder remoteUri(String uri) {
        mRemoteUri = uri == null ? null : URI.create(uri);
        return this;
    }


    @NonNull
    FirmwareStoreEntryBuilder minVersion(@Nullable FirmwareIdentifier firmware) {
        mMinVersion = firmware == null ? null : firmware.getVersion();
        return this;
    }


    @NonNull
    FirmwareStoreEntryBuilder maxVersion(@Nullable FirmwareIdentifier firmware) {
        mMaxVersion = firmware == null ? null : firmware.getVersion();
        return this;
    }


    @NonNull
    FirmwareStoreEntry build() {
        if (mModel == null || mVersion == null) {
            throw new AssertionError();
        }
        return new FirmwareStoreEntry(new FirmwareInfoCore(new FirmwareIdentifier(mModel, mVersion),
                mSize, null, mAttributes), null, mRemoteUri, mMinVersion, mMaxVersion, false);
    }
}
