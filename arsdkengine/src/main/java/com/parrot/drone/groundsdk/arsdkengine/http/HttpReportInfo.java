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

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/** Report as received from the drone HTTP report service. */
public class HttpReportInfo {

    /** Report name. */
    @Nullable
    @SerializedName("name")
    private final String mName;

    /** Creation date of the report, parsed from ISO 8601 format. */
    @Nullable
    @SerializedName("date")
    private final Date mDate;

    /** Relative url to be used in a GET request to download the report. */
    @Nullable
    @SerializedName("url")
    private final String mUrl;

    /**
     * Constructor.
     *
     * @param name report name
     * @param date creation date of the report
     * @param url  relative url to be used in a GET request to download the report
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public HttpReportInfo(@Nullable String name, @Nullable Date date, @Nullable String url) {
        mName = name;
        mDate = date;
        mUrl = url;
    }

    /**
     * Gets report name.
     *
     * @return report name
     */
    @Nullable
    public String getName() {
        return mName;
    }

    /**
     * Gets creation date of report.
     *
     * @return creation date
     */
    @Nullable
    public Date getDate() {
        return mDate;
    }

    /**
     * Gets relative url to be used in a GET request to download the report.
     *
     * @return report url
     */
    @Nullable
    public String getUrl() {
        return mUrl;
    }

    /**
     * Validates a report info.
     *
     * @param report report info to validate.
     *
     * @return {@code true} if the given report info is valid, otherwise {@code false}
     */
    public static boolean isValid(@Nullable HttpReportInfo report) {
        return report != null && report.mName != null && report.mDate != null && report.mUrl != null;
    }
}
