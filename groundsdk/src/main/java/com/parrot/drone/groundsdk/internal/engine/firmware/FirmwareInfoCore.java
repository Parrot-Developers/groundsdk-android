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

import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;

import java.util.EnumSet;
import java.util.Set;

/**
 * Implementation class of {@code FirmwareInfo}.
 */
final class FirmwareInfoCore extends FirmwareInfo {

    /** Firmware identifier. */
    @NonNull
    private final FirmwareIdentifier mFirmware;

    /** Firmware update file size. */
    @IntRange(from = 0)
    private final long mSize;

    /** Firmware update file checksum. */
    @Nullable
    private final String mChecksum;

    /** Firmware attributes. */
    @NonNull
    private final EnumSet<Attribute> mAttributes;

    /**
     * Constructor.
     *
     * @param firmware   firmware identifier
     * @param attributes firmware attributes
     * @param size       update file size
     * @param checksum   update file checksum, may be {@code null}
     */
    FirmwareInfoCore(@NonNull FirmwareIdentifier firmware, @IntRange(from = 0) long size, @Nullable String checksum,
                     @NonNull Set<Attribute> attributes) {
        mFirmware = firmware;
        mAttributes = attributes.isEmpty() ? EnumSet.noneOf(Attribute.class) : EnumSet.copyOf(attributes);
        mSize = size;
        mChecksum = checksum;
    }

    @NonNull
    @Override
    public FirmwareIdentifier getFirmware() {
        return mFirmware;
    }

    @Override
    public long getSize() {
        return mSize;
    }

    /**
     * Retrieves the checksum of the associated update file, if known.
     *
     * @return update file md5 checksum if available, otherwise {@code null}
     */
    @Nullable
    String getChecksum() {
        return mChecksum;
    }

    @NonNull
    @Override
    public EnumSet<Attribute> getAttributes() {
        return EnumSet.copyOf(mAttributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FirmwareInfoCore that = (FirmwareInfoCore) o;

        return mFirmware.equals(that.mFirmware);
    }

    @Override
    public int hashCode() {
        return mFirmware.hashCode();
    }
}
