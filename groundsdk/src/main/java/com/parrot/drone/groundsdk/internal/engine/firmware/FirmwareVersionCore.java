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

import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.sdkcore.arsdk.firmware.ArsdkFirmwareVersion;

/**
 * Implementation class for FirmwareVersion.
 */
public final class FirmwareVersionCore extends FirmwareVersion {

    /** Place holder value for use when a firmware version is unknown. */
    @SuppressWarnings("ConstantConditions")
    @NonNull
    public static final FirmwareVersionCore UNKNOWN = parse("0.0.0");

    /**
     * Converts a formatted version string to a FirmwareVersion.
     *
     * @param versionString formatted version string to parse
     *
     * @return a new FirmwareVersion instance corresponding to the provided version string, or {@code null} if the
     *         version string couldn't be parsed
     */
    @Nullable
    public static FirmwareVersionCore parse(@NonNull String versionString) {
        ArsdkFirmwareVersion version = ArsdkFirmwareVersion.parse(versionString);
        return version == null ? null : new FirmwareVersionCore(version);
    }

    @Override
    @NonNull
    public Type getType() {
        return mType;
    }

    @Override
    @IntRange(from = 0)
    public int getMajor() {
        return mArsdkVersion.getMajor();
    }

    @Override
    @IntRange(from = 0)
    public int getMinor() {
        return mArsdkVersion.getMinor();
    }

    @Override
    @IntRange(from = 0)
    public int getPatchLevel() {
        return mArsdkVersion.getPatch();
    }

    @Override
    @IntRange(from = 0)
    public int getBuildNumber() {
        return mArsdkVersion.getBuild();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(mArsdkVersion.getMajor()).append('.').append(mArsdkVersion.getMinor()).append('.')
               .append(mArsdkVersion.getPatch());
        switch (mType) {
            case ALPHA:
                builder.append("-alpha").append(mArsdkVersion.getBuild());
                break;
            case BETA:
                builder.append("-beta").append(mArsdkVersion.getBuild());
                break;
            case RELEASE_CANDIDATE:
                builder.append("-rc").append(mArsdkVersion.getBuild());
                break;
            case RELEASE:
            case DEVELOPMENT:
            default:
                break;
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FirmwareVersionCore that = (FirmwareVersionCore) o;

        return mArsdkVersion.equals(that.mArsdkVersion);
    }

    @Override
    public int hashCode() {
        return mArsdkVersion.hashCode();
    }

    @Override
    public int compareTo(@NonNull FirmwareVersion another) {
        return mArsdkVersion.compareTo(((FirmwareVersionCore) another).mArsdkVersion);
    }

    /** {@code ArsdkFirmwareVersion} delegate that this {@code FirmwareVersion} represents. */
    @NonNull
    private final ArsdkFirmwareVersion mArsdkVersion;

    /** Version type identifier. */
    @NonNull
    private final Type mType;

    /**
     * Constructor.
     *
     * @param version {@code ArsdkFirmwareVersion} object to build this {@code FirmwareVersion} from
     */
    private FirmwareVersionCore(@NonNull ArsdkFirmwareVersion version) {
        mArsdkVersion = version;
        mType = convertVersionType(version.getType());
    }

    /**
     * Converts an {@code ArsdkFirmwareVersion} type int into a {@code FirmwareVersion.Type}
     *
     * @param versionType type int to convert
     *
     * @return the corresponding {@code FirmwareVersion.Type}
     */
    @NonNull
    private static Type convertVersionType(@ArsdkFirmwareVersion.Type int versionType) {
        switch (versionType) {
            case ArsdkFirmwareVersion.TYPE_DEV:
                return FirmwareVersion.Type.DEVELOPMENT;
            case ArsdkFirmwareVersion.TYPE_ALPHA:
                return FirmwareVersion.Type.ALPHA;
            case ArsdkFirmwareVersion.TYPE_BETA:
                return FirmwareVersion.Type.BETA;
            case ArsdkFirmwareVersion.TYPE_RELEASE:
                return FirmwareVersion.Type.RELEASE;
            case ArsdkFirmwareVersion.TYPE_RC:
                return FirmwareVersion.Type.RELEASE_CANDIDATE;
        }
        throw new IllegalArgumentException("Unhandled version type " + versionType);
    }
}