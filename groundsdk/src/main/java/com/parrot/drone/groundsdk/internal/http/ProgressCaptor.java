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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSink;
import okio.ForwardingSource;
import okio.Okio;

/**
 * Allows to track progress of data flowing through {@link RequestBody} and {@link ResponseBody}.
 */
public final class ProgressCaptor {

    /**
     * Interface of listener receiving progress notifications.
     */
    public interface Listener {

        /**
         * Called when current progress updates.
         *
         * @param percent progress
         */
        void onProgress(@IntRange(from = 0, to = 100) int percent);
    }

    /**
     * Captures progress of data sent to the given request body.
     * <p>
     * Listener is notified on the same thread that writes to this request body.
     *
     * @param requestBody request body to capture progress from
     * @param listener    listener that will receive progress notifications
     *
     * @return a new request body that wraps the given one and supports progress capture
     */
    @NonNull
    public static RequestBody captureOf(@NonNull RequestBody requestBody, @NonNull Listener listener) {
        return new ProgressRequestBody(requestBody, listener);
    }

    /**
     * Captures progress of data received from the given response body.
     * <p>
     * Listener is notified on the same thread that reads from this response body.
     *
     * @param responseBody response body to capture progress from
     * @param listener     listener that will receive progress notifications
     *
     * @return a new response body that wraps the given one and supports progress capture
     */
    @NonNull
    public static ResponseBody captureOf(@NonNull ResponseBody responseBody, @NonNull Listener listener) {
        return new ProgressResponseBody(responseBody, listener);
    }

    /** Implementation of a {@link RequestBody} wrapper that keeps track of data sent through itself. */
    private static final class ProgressRequestBody extends ForwardingRequestBody
            implements RequestBodyInterceptor.CapturableRequestBody {

        /** Listener notified of progress events. */
        @NonNull
        private final Listener mListener;

        private ProgressRequestBody(@NonNull RequestBody delegate, @NonNull Listener listener) {
            super(delegate);
            mListener = listener;
        }

        @NonNull
        @Override
        public RequestBody onCapture() {
            return new ForwardingRequestBody(mDelegate) {

                @Override
                public void writeTo(@NonNull BufferedSink sink) throws IOException {
                    long contentLength = contentLength();
                    sink = Okio.buffer(new ForwardingSink(sink) {

                        /** Sent length so far, in bytes. */
                        private long mCurrentLength;

                        /** Last notified progress. */
                        private int mLastProgress;

                        @Override
                        public void write(@NonNull Buffer source, long byteCount) throws IOException {
                            super.write(source, byteCount);
                            mCurrentLength += byteCount;
                            int progress = Math.round(mCurrentLength * 100f / contentLength);
                            if (progress > mLastProgress) {
                                mLastProgress = progress;
                                mListener.onProgress(progress);
                            }
                        }
                    });
                    mDelegate.writeTo(sink);
                    sink.flush();
                }
            };
        }
    }

    /** Implementation of a {@link ResponseBody} wrapper that keeps track of data received through itself. */
    private static final class ProgressResponseBody extends ResponseBody {

        /** Wrapped response body to track progress of. */
        @NonNull
        private final ResponseBody mDelegate;

        /** Listener notified of progress. */
        @NonNull
        private final Listener mListener;

        /** Source wrapping the original response body source. */
        @Nullable
        private BufferedSource mSource;

        /**
         * Constructor.
         *
         * @param delegate wrapped response body
         * @param listener listener notified of progress
         */
        ProgressResponseBody(@NonNull ResponseBody delegate, @NonNull Listener listener) {
            mDelegate = delegate;
            mListener = listener;
        }

        @Override
        public MediaType contentType() {
            return mDelegate.contentType();
        }

        @Override
        public long contentLength() {
            return mDelegate.contentLength();
        }

        @Override
        @NonNull
        public BufferedSource source() {
            long contentLength = contentLength();
            if (mSource == null) {
                mSource = Okio.buffer(new ForwardingSource(mDelegate.source()) {

                    /** Received length so far, in bytes. */
                    private long mCurrentLength;

                    /** Last notified progress. */
                    private int mLastProgress;

                    @Override
                    public long read(@NonNull Buffer sink, long byteCount) throws IOException {
                        long readLength = super.read(sink, byteCount);
                        if (readLength > 0) {
                            mCurrentLength += readLength;
                            int progress = Math.round(mCurrentLength * 100f / contentLength);
                            if (progress > mLastProgress) {
                                mLastProgress = progress;
                                mListener.onProgress(progress);
                            }
                        }
                        return readLength;
                    }
                });
            }
            return mSource;
        }
    }

    /** A {@link RequestBody} which forwards calls to another, for easier subclassing. */
    private static class ForwardingRequestBody extends RequestBody {

        /** Request body delegate. */
        @NonNull
        final RequestBody mDelegate;

        ForwardingRequestBody(@NonNull RequestBody delegate) {
            mDelegate = delegate;
        }

        @Override
        public MediaType contentType() {
            return mDelegate.contentType();
        }

        @Override
        public long contentLength() throws IOException {
            return mDelegate.contentLength();
        }

        @Override
        public void writeTo(@NonNull BufferedSink sink) throws IOException {
            mDelegate.writeTo(sink);
        }
    }

    /**
     * Private constructor for static utility class.
     */
    private ProgressCaptor() {
    }
}
