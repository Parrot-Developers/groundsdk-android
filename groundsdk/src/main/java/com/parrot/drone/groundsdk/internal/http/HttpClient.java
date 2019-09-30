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

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.Cancelable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import okhttp3.MediaType;

/**
 * Abstract base for HttpClient implementations.
 * <p>
 * Provides outstanding request bookkeeping and cancellation on disposal.
 */
public abstract class HttpClient {

    /** application/gzip media type. */
    static final MediaType MEDIA_TYPE_APPLICATION_GZIP = MediaType.parse("application/gzip");

    /**
     * An exception that can be used to wrap an HTTP response message and status code.
     */
    protected static final class HttpException extends IOException {

        /** HTTP status code. */
        private final int mCode;

        /**
         * Constructor.
         *
         * @param message HTTP status message
         * @param code    HTTP status code
         */
        public HttpException(@NonNull String message, int code) {
            super(message);
            mCode = code;
        }

        /**
         * Retrieves the associated HTTP status code.
         *
         * @return HTTP status code
         */
        public int getCode() {
            return mCode;
        }
    }

    /** Tracks outstanding requests in order to cancel them altogether when the client is disposed. */
    @NonNull
    private final Set<Cancelable> mCurrentRequests;

    /**
     * Constructor.
     */
    protected HttpClient() {
        mCurrentRequests = new HashSet<>();
    }

    /**
     * Disposes the client, canceling all outstanding requests.
     */
    @CallSuper
    public void dispose() {
        for (Cancelable request : mCurrentRequests) {
            request.cancel();
        }
        mCurrentRequests.clear();
    }

    /**
     * Puts a request in the book of outstanding request.
     * <p>
     * This request will be automatically canceled when {@link #dispose()} is called.
     *
     * @param request request to book
     *
     * @return the input request, now booked
     */
    @NonNull
    protected final HttpRequest bookRequest(@NonNull HttpRequest request) {
        mCurrentRequests.add(request);
        return request;
    }
}
