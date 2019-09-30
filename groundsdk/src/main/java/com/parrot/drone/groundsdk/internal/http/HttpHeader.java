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

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.GroundSdkConfig;

/**
 * Data class combining an http header with its associated value.
 */
public final class HttpHeader {

    /** HTTP account header. */
    static final String ACCOUNT = "x-account";

    /**
     * Obtains an HTTP header to use for providing the default user agent in HTTP requests.
     *
     * @param context application context
     *
     * @return default user agent HTTP header
     */
    @NonNull
    static HttpHeader defaultUserAgent(@NonNull Context context) {
        return new HttpHeader("User-Agent", getUserAgent(context));
    }

    /**
     * Obtains an HTTP header to use for providing the application key in HTTP requests.
     *
     * @param context application context
     *
     * @return application key HTTP header
     */
    @NonNull
    static HttpHeader appKey(@NonNull Context context) {
        return new HttpHeader("x-api-key", GroundSdkConfig.get(context).getApplicationKey());
    }

    /** Header name. */
    @NonNull
    private final String mHeader;

    /** Associated value. */
    @NonNull
    private final String mValue;

    /**
     * Constructor.
     *
     * @param header header name
     * @param value  associated value
     */
    private HttpHeader(@NonNull String header, @NonNull String value) {
        mHeader = header;
        mValue = value;
    }

    /**
     * Retrieves the header name.
     *
     * @return header name
     */
    @NonNull
    public String getHeader() {
        return mHeader;
    }

    /**
     * Retrieves the header's associated value.
     *
     * @return associated value
     */
    @NonNull
    public String getValue() {
        return mValue;
    }

    /**
     * Builds the default user agent string to inject in 'User-Agent' header for HTTP requests.
     *
     * @param context application context
     *
     * @return user agent string
     */
    @NonNull
    private static String getUserAgent(@NonNull Context context) {
        GroundSdkConfig config = GroundSdkConfig.get(context);
        return config.getApplicationPackage() + "/" + config.getApplicationVersion() +
               " (Android; " + Build.MANUFACTURER + " " + Build.MODEL + "; " + Build.VERSION.RELEASE + ") " +
               GroundSdkConfig.getSdkPackage() + "/" + GroundSdkConfig.getSdkVersion();
    }
}
