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

package com.parrot.drone.groundsdk.stream;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive;

/** Base stream interface. */
public interface Stream {

    /** Stream state. */
    enum State {

        /**
         * Stream is stopped.
         * <p>
         * In this state, specific stream child interfaces do not provide any meaningful playback state
         * information.
         */
        STOPPED,

        /**
         * Stream is suspended.
         * <p>
         * In this state, specific stream child interfaces inform about the playback state that the stream will try
         * to recover once it can start again.
         * <p>
         * Note that only {@link CameraLive camera live stream} supports suspension.
         */
        SUSPENDED,

        /**
         * Stream is starting.
         * <p>
         * In this state, specific stream child interfaces inform about the playback state that the stream will try
         * to apply once it is fully started.
         */
        STARTING,

        /**
         * Stream is started.
         * <p>
         * In this state, specific stream child interfaces inform about the stream's current playback state.
         */
        STARTED
    }

    /**
     * Informs about current stream state.
     *
     * @return current stream state
     */
    @NonNull
    State state();

    /**
     * Base sink interface.
     * <p>
     * A sink allows to receive stream data, such as encoded H.264 or decoded YUV frames, as it flows through the
     * stream.
     */
    interface Sink {

        /**
         * Sink configuration interface.
         * <p>
         * Defines what kind of data some sink will provide, and how it will be provided.
         */
        interface Config {}

        /**
         * Closes the sink.
         * <p>
         * Once closed, this sink cannot be used anymore.
         * <p>
         * Note that all open sinks are closed when a stream closes.
         */
        void close();
    }

    /**
     * Opens a sink from this stream.
     *
     * @param config configuration of the sink to be opened
     *
     * @return a new sink instance, configured as specified
     */
    @NonNull
    Sink openSink(@NonNull Sink.Config config);
}
