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

package com.parrot.drone.groundsdk.arsdkengine.http;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/** Record information received from the drone HTTP FDR service. */
public class HttpFdrInfo {

    /** Record name. */
    @Nullable
    @SerializedName("name")
    private final String mName;

    /** Creation date of the record, parsed from ISO 8601 format. */
    @Nullable
    @SerializedName("date")
    private final Date mDate;

    /** Size of the record file, in bytes. */
    @IntRange(from = 0)
    @SerializedName("size")
    private final long mSize;

    /** Relative url to be used in a GET request to download the record. */
    @Nullable
    @SerializedName("url")
    private final String mUrl;

    /**
     * Constructor.
     *
     * @param name record name
     * @param date creation date of the record
     * @param size record size, in bytes
     * @param url  relative url to be used in a GET request to download the record
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public HttpFdrInfo(@Nullable String name, @Nullable Date date, long size, @Nullable String url) {
        mName = name;
        mDate = date;
        mSize = size;
        mUrl = url;
    }

    /**
     * Gets record name.
     *
     * @return record name
     */
    @Nullable
    public String getName() {
        return mName;
    }

    /**
     * Gets creation date of record.
     *
     * @return creation date
     */
    @Nullable
    public Date getDate() {
        return mDate;
    }

    /**
     * Gets record size.
     *
     * @return record size in bytes
     */
    @IntRange(from = 0)
    public long getSize() {
        return mSize;
    }

    /**
     * Gets relative url to be used in a GET request to download the record.
     *
     * @return record url
     */
    @Nullable
    public String getUrl() {
        return mUrl;
    }

    /**
     * Validates a record info.
     *
     * @param record record info to validate.
     *
     * @return {@code true} if the given record info is valid, otherwise {@code false}
     */
    public static boolean isValid(@Nullable HttpFdrInfo record) {
        return record != null && record.mName != null && record.mDate != null && record.mSize >= 0
               && record.mUrl != null;
    }
}
