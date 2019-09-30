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

package com.parrot.drone.groundsdk.internal.stream;

import android.os.Looper;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.stream.Stream;

/**
 * A sink that delivers YUV frames.
 */
public interface YUVSink extends Stream.Sink {

    /**
     * A YUV frame.
     */
    interface Frame {

        /**
         * Provides access to a frame's native backend.
         * <p>
         * The returned value, if valid, can be used in native code as a pointer onto a {@code struct sdkcore_frame}.
         *
         * @return native pointer onto the frame's backend.
         *
         * @throws IllegalStateException in case the frame has been released
         */
        long nativePtr();

        /**
         * Releases the frame.
         *
         * @throws IllegalStateException in case the frame has already been released
         */
        void release();
    }

    /**
     * Sink event callbacks.
     * <p>
     * All methods are called on the configured looper thread for that sink.
     */
    interface Callback {

        /**
         * Notifies that the sink starts.
         *
         * @param sink sink that did start
         */
        void onStart(@NonNull YUVSink sink);

        /**
         * Delivers a frame from the sink.
         * <p>
         * Client owns the delivered frame and must {@link Frame#release() release} it when no longer needed, otherwise
         * leaks may occur.
         *
         * @param sink  sink that did deliver the frame
         * @param frame delivered frame
         */
        void onFrame(@NonNull YUVSink sink, @NonNull Frame frame);

        /**
         * Notifies that the sink stops.
         *
         * @param sink sink that did stop
         */
        void onStop(@NonNull YUVSink sink);
    }

    /**
     * Creates a new {@code YUVSink} config.
     *
     * @param looper   looper onto which callbacks will be invoked
     * @param callback callback notified of sink events.
     *
     * @return a new {@code YUVSink} config.
     */
    @NonNull
    static Config config(@NonNull Looper looper, @NonNull Callback callback) {
        return new YUVSinkCore.Config(looper, callback);
    }
}
