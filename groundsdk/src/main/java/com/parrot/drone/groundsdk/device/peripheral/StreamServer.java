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

package com.parrot.drone.groundsdk.device.peripheral;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive;
import com.parrot.drone.groundsdk.device.peripheral.stream.MediaReplay;
import com.parrot.drone.groundsdk.stream.Stream;

/**
 * StreamServer peripheral interface.
 * <p>
 * This peripheral allows streaming of live camera video and replay of video files stored in drone memory.
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(StreamServer.class)}</pre>
 *
 * @see Drone#getPeripheral(Class)
 * @see Drone#getPeripheral(Class, Ref.Observer)
 */
public interface StreamServer extends Peripheral {

    /**
     * Drone streaming control.
     * <p>
     * Provides global control of the ability to start streams.
     * <p>
     * When streaming gets enabled, currently {@link Stream.State#SUSPENDED suspended} stream will be resumed. <br>
     * When streaming is enabled, streams can be started.
     * <p>
     * When streaming gets disabled, currently started stream gets suspended, in case it supports being resumed, or
     * {@link Stream.State#STOPPED stopped} otherwise. <br>
     * When streaming is disabled, no stream can be started.
     *
     * @param enable {@code true} to enable streaming, {@code false} to disable it
     */
    void enableStreaming(boolean enable);

    /**
     * Tells whether streaming is currently enabled.
     *
     * @return {@code true} if streaming is enabled, otherwise {@code false}
     */
    boolean streamingEnabled();

    /**
     * Provides access to the drone camera live stream.
     * <p>
     * There is only one live stream instance that is shared amongst all open references.
     * <p>
     * Closing the returned reference does <strong>NOT</strong> automatically stops the referenced camera live stream.
     *
     * @param observer observer notified when the stream state changes
     *
     * @return a reference onto the camera live stream interface
     */
    @NonNull
    Ref<CameraLive> live(@NonNull Ref.Observer<CameraLive> observer);

    /**
     * Creates a new replay stream for some remote media.
     * <p>
     * This method creates a new replay stream instance for the given media stream identifier that must be disposed by
     * closing the returned reference once that stream is not needed.
     * <p>
     * Closing the returned reference automatically stops the referenced media replay stream.
     *
     * @param source   identifies remote media stream
     * @param observer observer notified when the stream state changes
     *
     * @return a reference onto a new replay stream for the given media stream identifier
     */
    @NonNull
    Ref<MediaReplay> replay(@NonNull MediaReplay.Source source, @NonNull Ref.Observer<MediaReplay> observer);
}