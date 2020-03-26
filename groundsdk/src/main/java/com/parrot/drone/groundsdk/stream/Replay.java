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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;

/**
 * Base replay stream interface.
 * <p>
 * This interface exposes primitives for controlling media streams, which have a known duration, as well as a current
 * position, which may furthermore be controlled by {@link #seekTo(long) seeking} through the stream.
 **/
public interface Replay extends Stream {

    /** Media replay stream playback state. */
    enum PlayState {

        /** Stream is {@link State#STOPPED}. */
        NONE,

        /**
         * Stream is either {@link State#STARTING} in which case this indicates that playback will start once the
         * stream is started, or {@link State#STARTED}, in which case this indicates that playback is currently
         * ongoing.
         */
        PLAYING,

        /**
         * Stream is either {@link State#STARTING} in which case this indicates that playback will pause once the
         * stream is started, or {@link State#STARTED}, in which case this indicates that playback is currently
         * paused.
         */
        PAUSED
    }

    /**
     * Informs about current playback state.
     *
     * @return current playback state
     */
    @NonNull
    PlayState playState();

    /**
     * Informs about total playback duration.
     *
     * @return total playback duration, in milliseconds
     */
    @IntRange(from = 0)
    long duration();

    /**
     * Informs about current playback position.
     * <p>
     * Note that position changes are <strong>NOT</strong> notified through any registered
     * {@link Ref.Observer observer} of that stream. <br>
     * The application may poll this value at the appropriate rate, depending on its use case.
     *
     * @return current playback position, in milliseconds
     */
    @IntRange(from = 0)
    long position();

    /**
     * Requests playback to start.
     * <p>
     * This method will try and start the stream if necessary.
     *
     * @return {@code true} if playback request was sent, otherwise {@code false}
     */
    boolean play();

    /**
     * Requests playback to pause.
     * <p>
     * This method will try and start the stream if necessary.
     *
     * @return {@code true} if playback request was sent, otherwise {@code false}
     */
    boolean pause();

    /**
     * Requests playback position change.
     * <p>
     * This method will try and start the stream if necessary.
     *
     * @param position position to seek to
     *
     * @return {@code true} if seek request was sent, otherwise {@code false}
     */
    boolean seekTo(@IntRange(from = 0) long position);

    /**
     * Stops the stream.
     * <p>
     * This method will bring the stream to the {@link State#STOPPED} state.
     */
    void stop();
}
