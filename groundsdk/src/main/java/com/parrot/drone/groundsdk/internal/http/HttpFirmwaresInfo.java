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

package com.parrot.drone.groundsdk.internal.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_HTTP;

/**
 * Data received from the HTTP update server.
 */
public class HttpFirmwaresInfo {

    /** Information on an remotely available firmware update. */
    public static class Firmware {

        /** Identifier of the product onto which the update applies. */
        @SerializedName("product")
        @Nullable
        public final String productId;

        /** Version of the update firmware. */
        @SerializedName("version")
        @Nullable
        public final String version;

        /** URL for downloading the firmware update file. */
        @SerializedName("url")
        @Nullable
        public final String url;

        /** Size of the firmware update file, in bytes. */
        @SerializedName("size")
        public final long size;

        /** Md5 checksum of the firmware update file. */
        @SerializedName("md5")
        @Nullable
        public final String checksum;

        /** Required minimal version of the device firmware onto which the update can be applied. */
        @SerializedName("required_version")
        @Nullable
        public final String minVersion;

        /** Maximal version of the device firmware onto which the update can be applied. */
        @SerializedName("max_version")
        @Nullable
        public final String maxVersion;

        /** Firmware flags. */
        @SerializedName("flags")
        @Nullable
        public final Set<String> flags;

        /**
         * Constructor.
         *
         * @param productId       product identifier
         * @param version         firmware version
         * @param url             firmware file download url
         * @param size            firmware file size
         * @param checksum        firmware file checksum
         * @param requiredVersion minimal required device firmware version
         * @param maxVersion      maximal applicable device firmware version
         * @param flags           special firmware flags
         */
        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        Firmware(@Nullable String productId, @Nullable String version, @Nullable String url, long size,
                 @Nullable String checksum, @Nullable String requiredVersion, @Nullable String maxVersion,
                 @Nullable Set<String> flags) {
            this.productId = productId;
            this.version = version;
            this.url = url;
            this.size = size;
            this.checksum = checksum;
            this.minVersion = requiredVersion;
            this.maxVersion = maxVersion;
            this.flags = flags;
        }
    }

    /** Set of remotely available firmwares. */
    @SerializedName("firmware")
    @Nullable
    private final Set<Firmware> mFirmwares;

    /** Defines a blacklisted device firmware. */
    public static final class BlackListEntry {

        /** Identifier of the blacklisted product. */
        @SerializedName("product")
        @Nullable
        public final String productId;

        /** Blacklisted firmware versions for this product. */
        @SerializedName("versions")
        @Nullable
        public final Set<String> versions;

        /**
         * Constructor.
         *
         * @param productId product identifier
         * @param versions  firmware versions
         */
        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        BlackListEntry(@Nullable String productId, @Nullable Set<String> versions) {
            this.productId = productId;
            this.versions = versions;
        }
    }

    /** Firmware blacklist. */
    @SerializedName("blacklist")
    @Nullable
    private final Set<BlackListEntry> mBlackList;


    /**
     * Constructor.
     *
     * @param firmwares remotely available firmwares
     * @param blacklist firmware blacklist
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public HttpFirmwaresInfo(@Nullable Set<Firmware> firmwares, @Nullable Set<BlackListEntry> blacklist) {
        mFirmwares = firmwares;
        mBlackList = blacklist;
    }

    /**
     * Retrieves the set of remotely available firmwares.
     *
     * @return remotely available firmwares
     */
    @NonNull
    public final Collection<Firmware> getFirmwares() {
        Collection<Firmware> firmwares = new ArrayList<>();
        if (mFirmwares != null) for (Firmware firmware : mFirmwares) {
            if (firmware != null) {
                firmwares.add(firmware);
            } else {
                ULog.w(TAG_HTTP, "Dropping null entry in received firmwares list");
            }
        }
        return firmwares;
    }

    /**
     * Retrieves the firmware blacklist.
     *
     * @return firmware blacklist
     */
    @NonNull
    public final Collection<BlackListEntry> getBlacklist() {
        Collection<BlackListEntry> blacklist = new ArrayList<>();
        if (mBlackList != null) for (BlackListEntry blackListEntry : mBlackList) {
            if (blackListEntry != null) {
                blacklist.add(blackListEntry);
            } else {
                ULog.w(TAG_HTTP, "Dropping null entry in received blacklist");
            }
        }
        return blacklist;
    }
}
