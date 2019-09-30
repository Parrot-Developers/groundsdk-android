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

import com.google.gson.JsonParseException;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.http.HttpFirmwaresInfo;

import java.net.URI;
import java.util.EnumSet;

/**
 * A firmware entry in the {@link FirmwareStoreCore}.
 */
final class FirmwareStoreEntry {

    /** Firmware information. */
    @NonNull
    private final FirmwareInfoCore mFirmware;

    /**
     * Indicates where the firmware update file can be found when provided as an application preset.
     * {@code null} if this entry does not represent an application preset.
     */
    @Nullable
    private final URI mPresetUri;

    /**
     * Indicates where the firmware update file is stored locally. {@code null} if no update file is available
     * locally.
     */
    @Nullable
    private URI mLocalUri;

    /**
     * Indicates where the firmware update file is available remotely. {@code null} if no update file is available
     * remotely.
     */
    @Nullable
    private URI mRemoteUri;

    /** Minimal device firmware version required to apply the update. {@code null} if no such constraint. */
    @Nullable
    private final FirmwareVersion mMinVersion;

    /** Maximal device firmware version onto which the update can be applied. {@code null} if no such constraint. */
    @Nullable
    private final FirmwareVersion mMaxVersion;


    /**
     * Constructor.
     *
     * @param firmware   firmware info
     * @param localUri   local firmware update file URI
     * @param remoteUri  remote firmware update file URI
     * @param minVersion minimal firmware update version
     * @param maxVersion maximal firmware update version
     * @param preset     {@code true} if application preset, otherwise {@code false}
     */
    FirmwareStoreEntry(@NonNull FirmwareInfoCore firmware, @Nullable URI localUri, @Nullable URI remoteUri,
                       @Nullable FirmwareVersion minVersion, @Nullable FirmwareVersion maxVersion, boolean preset) {
        mFirmware = firmware;
        mPresetUri = preset ? localUri : null;
        mMinVersion = minVersion;
        mMaxVersion = maxVersion;
        mLocalUri = preset ? null : localUri;
        mRemoteUri = remoteUri;
    }

    /**
     * Retrieves firmware info.
     *
     * @return firmware info
     */
    @NonNull
    FirmwareInfoCore getFirmwareInfo() {
        return mFirmware;
    }

    /**
     * Retrieves local firmware update file URI.
     *
     * @return local firmware update file URI if exists, otherwise {@code null}
     */
    @Nullable
    URI getLocalUri() {
        return mLocalUri != null ? mLocalUri : mPresetUri;
    }

    /**
     * Retrieves remote firmware update file URI.
     *
     * @return remote firmware update file URI if exists, otherwise {@code null}
     */
    @Nullable
    URI getRemoteUri() {
        return mRemoteUri;
    }

    /**
     * Retrieves minimal firmware update version.
     *
     * @return minimal firmware update version if any such constraint exists, otherwise {@code null}
     */
    @Nullable
    FirmwareVersion getMinApplicableVersion() {
        return mMinVersion;
    }

    /**
     * Retrieves maximal firmware update version.
     *
     * @return maximal firmware update version if any such constraint exists, otherwise {@code null}
     */
    @Nullable
    FirmwareVersion getMaxApplicableVersion() {
        return mMaxVersion;
    }

    /**
     * Tells whether this entry is an application preset.
     *
     * @return {@code true} if this entry is an application preset, otherwise {@code false}
     */
    boolean isPreset() {
        return mPresetUri != null;
    }

    /**
     * Attaches an URI to this entry.
     * <p>
     * The given URI indicates where a firmware update file may be found for this entry. It replace any previously set
     * URI of
     * the same type, either {@link Schemes#LOCAL local} (see {@link #getLocalUri()}) or {@link Schemes#REMOTE remote}
     * (see
     * {@link #getRemoteUri()}).
     *
     * @param uri uri to attach
     *
     * @return {@code true} in case the given URI replaces a different or non-existing previous URI, otherwise {@code
     *         false}
     */
    boolean setUri(@NonNull URI uri) {
        String scheme = uri.getScheme();
        if (Schemes.LOCAL.contains(scheme) && !uri.equals(mLocalUri)) {
            mLocalUri = uri;
            return true;
        } else if (Schemes.REMOTE.contains(scheme) && !uri.equals(mRemoteUri)) {
            mRemoteUri = uri;
            return true;
        }
        return false;
    }

    /**
     * Clears this entry local firmware file URI.
     *
     * @return {@code true} if this entry does not contain any URI ({@link #getLocalUri() local} or {@link
     *         #getRemoteUri() remote})
     *         anymore, otherwise {@code false}
     */
    boolean clearLocalUri() {
        mLocalUri = null;
        return mPresetUri == null && mRemoteUri == null;
    }

    /**
     * Clears this entry remote firmware file URI.
     *
     * @return {@code true} if this entry does not contain any URI ({@link #getLocalUri() local} or {@link
     *         #getRemoteUri() remote})
     *         anymore, otherwise {@code false}
     */
    boolean clearRemoteUri() {
        mRemoteUri = null;
        return mPresetUri == null && mLocalUri == null;
    }

    /**
     * Converts a {@code HttpFirmwaresInfo.Firmware} to a {@code FirmwareStoreEntry}.
     *
     * @param httpFirmware HTTP firmware info to convert
     *
     * @return the firmware store entry equivalent
     *
     * @throws JsonParseException in case conversion fails
     */
    @NonNull
    static FirmwareStoreEntry from(@NonNull HttpFirmwaresInfo.Firmware httpFirmware) {
        Validation.require("product", httpFirmware.productId);
        Validation.require("version", httpFirmware.version);
        FirmwareIdentifier identifier = new FirmwareIdentifier(
                Validation.validateModel("product", httpFirmware.productId),
                Validation.validateVersion("version", httpFirmware.version));

        Validation.require("url", httpFirmware.url);
        URI remoteUri = Validation.validateUri("url", Schemes.REMOTE, httpFirmware.url);

        long size = Validation.validateSize("size", httpFirmware.size);

        EnumSet<FirmwareInfo.Attribute> attributes = EnumSet.noneOf(FirmwareInfo.Attribute.class);
        if (httpFirmware.flags != null) {
            attributes = Validation.validateAttributes("flags", httpFirmware.flags, false);
        }

        FirmwareVersion minVersion = null;
        if (httpFirmware.minVersion != null) {
            minVersion = Validation.validateVersion("required_version", httpFirmware.minVersion);
        }
        FirmwareVersion maxVersion = null;
        if (httpFirmware.maxVersion != null) {
            maxVersion = Validation.validateVersion("max_version", httpFirmware.maxVersion);
        }

        return new FirmwareStoreEntry(new FirmwareInfoCore(identifier, size, httpFirmware.checksum, attributes),
                null, remoteUri, minVersion, maxVersion, false);
    }
}

