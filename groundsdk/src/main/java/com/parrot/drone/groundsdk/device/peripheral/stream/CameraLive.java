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

package com.parrot.drone.groundsdk.device.peripheral.stream;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdk.stream.Stream;

/**
 * Camera live stream control interface.
 * <p>
 * Provides control over the drone camera live stream, allowing to pause, resume or stop playback.
 * <p>
 * There is only one instance of this interface that is shared amongst all clients that have an
 * {@link StreamServer#live(Ref.Observer) open reference} on this stream.
 * <p>
 * This stream supports {@link Stream.State#SUSPENDED suspension}. When it is started and gets interrupted because
 * another stream starts, or because streaming gets {@link StreamServer#enableStreaming(boolean) disabled} globally,
 * then it will move to the SUSPENDED {@link #state() state}. <br>
 * Once the interrupting stream stops, or streaming gets enabled, then it will try and be resumed in the state it
 * was before suspension. <br>
 * Also, this implies that this stream may be started even while streaming is globally disabled. In such a case,
 * it will move to the SUSPENDED state until either it is {@link State#STOPPED} by client request, or streaming
 * gets enabled.
 * <p>
 * The stream is neither stopped nor released when the reference that provides it is closed.
 */
public interface CameraLive extends Stream {

    /** Camera live stream playback state. */
    enum PlayState {

        /** Stream is {@link State#STOPPED}. */
        NONE,

        /**
         * Stream is either {@link State#STARTING} or {@link State#SUSPENDED}, in which case this indicates
         * that playback will start once the stream is started, or {@link State#STARTED}, in which case this
         * indicates that playback is currently ongoing.
         */
        PLAYING,

        /**
         * Stream is either {@link State#STARTING} or {@link State#SUSPENDED}, in which case this indicates
         * that playback will pause once the stream is started, or {@link State#STARTED}, in which case this
         * indicates that playback is currently paused.
         */
        PAUSED,
    }

    /**
     * Informs about current playback state.
     *
     * @return current playback state
     */
    @NonNull
    PlayState playState();

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
     * Stops the stream.
     * <p>
     * This method will bring the stream to the {@link State#STOPPED} state.*
     */
    void stop();
}
