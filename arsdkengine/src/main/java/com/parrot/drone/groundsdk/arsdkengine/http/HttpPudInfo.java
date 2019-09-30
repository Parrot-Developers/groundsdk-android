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

/** PUD as received from the drone HTTP PUD service. */
public final class HttpPudInfo {

    /** PUD name. */
    @Nullable
    @SerializedName("name")
    private final String mName;

    /** Creation date of the PUD, in ISO 8601 format. */
    @Nullable
    @SerializedName("date")
    private final String mDate;

    /** Size of the PUD file, in bytes. */
    @IntRange(from = 0)
    @SerializedName("size")
    private final long mSize;

    /** Relative URL to be used in a GET request to download the PUD. */
    @Nullable
    @SerializedName("url")
    private final String mUrl;

    /**
     * Constructor.
     *
     * @param name PUD name
     * @param date PUD creation date, in ISO 8601 format
     * @param size PUD size, in bytes
     * @param url  PUD relative download URL
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    HttpPudInfo(@Nullable String name, @Nullable String date, @IntRange(from = 0) long size, @Nullable String url) {
        mName = name;
        mDate = date;
        mSize = size;
        mUrl = url;
    }

    /**
     * Retrieves PUD name.
     *
     * @return PUD name
     */
    @Nullable
    public String getName() {
        return mName;
    }

    /**
     * Retrieves PUD creation date.
     *
     * @return creation date in ISO 8601 format
     */
    @Nullable
    public String getDate() {
        return mDate;
    }

    /**
     * Retrieves PUD file size.
     *
     * @return file size in bytes
     */
    @IntRange(from = 0)
    public long getSize() {
        return mSize;
    }

    /**
     * Retrieves PUD relative download URL.
     *
     * @return download URL
     */
    @Nullable
    public String getUrl() {
        return mUrl;
    }

    /**
     * Validates a PUD info.
     *
     * @param pud PUD info to validate.
     *
     * @return {@code true} if the given PUD info is valid, otherwise {@code false}
     */
    public static boolean isValid(@Nullable HttpPudInfo pud) {
        return pud != null && pud.mName != null && pud.mDate != null && pud.mSize >= 0 && pud.mUrl != null;
    }
}
