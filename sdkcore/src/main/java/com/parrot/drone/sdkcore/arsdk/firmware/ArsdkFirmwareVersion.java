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

package com.parrot.drone.sdkcore.arsdk.firmware;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.SdkCore;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a device's firmware version.
 */
public final class ArsdkFirmwareVersion implements Comparable<ArsdkFirmwareVersion> {

    /**
     * Creates a new {@code ArsdkFirmwareVersion} instance from parsing the given version string.
     *
     * @param versionStr version string to parse
     *
     * @return a new {@code ArsdkFirmwareVersion} instance if the given version string could be parsed successfully,
     *         otherwise {@code null}
     */
    @Nullable
    public static ArsdkFirmwareVersion parse(@NonNull String versionStr) {
        return nativeFromString(versionStr);
    }

    /** Int definition of firmware version types. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_DEV, TYPE_ALPHA, TYPE_BETA, TYPE_RC, TYPE_RELEASE})
    public @interface Type {}

    /* Numerical device type values MUST be kept in sync with C enum puf_version_type */

    /** Dev version. */
    public static final int TYPE_DEV = 0;

    /** Alpha version. */
    public static final int TYPE_ALPHA = 1;

    /** Beta version. */
    public static final int TYPE_BETA = 2;

    /** Release candidate version. */
    public static final int TYPE_RC = 3;

    /** Release version. */
    public static final int TYPE_RELEASE = 4;

    /** Version type. */
    @Type
    private final int mType;

    /** Version major identifier. */
    private final int mMajor;

    /** Version minor identifier. */
    private final int mMinor;

    /** Version patch level. */
    private final int mPatch;

    /** Version build number. */
    private final int mBuild;

    /**
     * Retrieves the version type.
     *
     * @return firmware version type
     */
    @Type
    public int getType() {
        return mType;
    }

    /**
     * Retrieves the version major identifier.
     *
     * @return firmware version major
     */
    public int getMajor() {
        return mMajor;
    }

    /**
     * Retrieves the version minor identifier.
     *
     * @return firmware version minor
     */
    public int getMinor() {
        return mMinor;
    }

    /**
     * Retrieves the version patch level.
     *
     * @return firmware version patch
     */
    public int getPatch() {
        return mPatch;
    }

    /**
     * Retrieves the version build number.
     *
     * @return firmware version build
     */
    public int getBuild() {
        return mBuild;
    }

    @Override
    public int compareTo(@NonNull ArsdkFirmwareVersion another) {
        // On app point of view, development version is greater than any version
        if (mType == TYPE_DEV && another.mType != TYPE_DEV) {
            return 1;
        }
        if (mType != TYPE_DEV && another.mType == TYPE_DEV) {
            return -1;
        }
        return nativeCompare(mType, mMajor, mMinor, mPatch, mBuild, another.mType,
                another.mMajor, another.mMinor, another.mPatch, another.mBuild);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArsdkFirmwareVersion version = (ArsdkFirmwareVersion) o;

        return compareTo(version) == 0;
    }

    @Override
    public int hashCode() {
        int result = mType;
        result = 31 * result + mMajor;
        result = 31 * result + mMinor;
        result = 31 * result + mPatch;
        result = 31 * result + mBuild;
        return result;
    }

    /**
     * Constructor.
     * <p>
     * Called from native.
     *
     * @param type  version type
     * @param major version major
     * @param minor version minor
     * @param patch version patch
     * @param build version build
     */
    @SuppressWarnings("unused") /* native-cb */
    private ArsdkFirmwareVersion(@Type int type, int major, int minor, int patch, int build) {
        mType = type;
        mMajor = major;
        mMinor = minor;
        mPatch = patch;
        mBuild = build;
    }

    /* JNI declarations and setup */

    /**
     * Creates an {@code ArsdkFirmwareVersion} from a native firmware version pointer.
     *
     * @param nativePtr native firmware version pointer backing this {@code ArsdkFirmwareVersion} instance.
     *
     * @return a new {@code ArsdkFirmwareVersion} instance if successful, otherwise {@code null}
     */
    static native ArsdkFirmwareVersion nativeCreate(long nativePtr);

    @Nullable
    private static native ArsdkFirmwareVersion nativeFromString(@NonNull String versionStr);

    private static native int nativeCompare(@Type int lhsType, int lhsMajor, int lhsMinor, int lhsPatch, int lhsBuild,
                                            @Type int rhsType, int rhsMajor, int rhsMinor, int rhsPatch, int rhsBuild);

    private static native void nativeClassInit();

    static {
        SdkCore.init();
        nativeClassInit();
    }
}
