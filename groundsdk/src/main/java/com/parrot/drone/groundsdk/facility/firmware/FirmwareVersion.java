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

package com.parrot.drone.groundsdk.facility.firmware;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.internal.engine.firmware.FirmwareVersionCore;

/**
 * Represents a device's firmware version.
 */
public abstract class FirmwareVersion implements Comparable<FirmwareVersion> {

    /**
     * Converts a formatted version string to a FirmwareVersion.
     *
     * @param versionString formatted version string to parse
     *
     * @return a new FirmwareVersion instance corresponding to the provided version string, or {@code null} if the
     *         version string couldn't be parsed
     */
    @Nullable
    public static FirmwareVersion parse(@NonNull String versionString) {
        return FirmwareVersionCore.parse(versionString);
    }

    /**
     * Firmware version type.
     * <p>
     * The ordinal defines an order of priority on version types, in terms of comparing two firmware versions. <br>
     * Higher type ordinals are considered superior than lower ordinals.
     */
    public enum Type {

        /** Development version. */
        DEVELOPMENT,

        /** Alpha version. */
        ALPHA,

        /** Beta version. */
        BETA,

        /** Release candidate version. */
        RELEASE_CANDIDATE,

        /** Release version. */
        RELEASE
    }

    /**
     * Gets the firmware version type.
     *
     * @return firmware version type
     */
    @NonNull
    public abstract Type getType();


    /**
     * Gets the major identifier of the firmware version.
     *
     * @return firmware version major identifier
     */
    @IntRange(from = 0)
    public abstract int getMajor();

    /**
     * Gets the minor identifier of the firmware version.
     *
     * @return firmware version minor identifier
     */
    @IntRange(from = 0)
    public abstract int getMinor();

    /**
     * Gets the patch level of the firmware version.
     *
     * @return firmware version patch level
     */
    @IntRange(from = 0)
    public abstract int getPatchLevel();

    /**
     * Gets the build number of the firmware version.
     * <p>
     * This is meaningless for {@link Type#RELEASE} and {@link Type#DEVELOPMENT} versions, for which 0 will be returned.
     *
     * @return firmware version build number
     */
    @IntRange(from = 0)
    public abstract int getBuildNumber();

    /**
     * Constructor.
     * <p>
     * Only for internal GroundSdk use. Application <strong>MUST NOT</strong> override {@code FirmwareVersion} class.
     */
    protected FirmwareVersion() {
    }
}
