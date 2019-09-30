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

package com.parrot.drone.groundsdk.internal.device.peripheral.stream;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive;
import com.parrot.drone.groundsdk.device.peripheral.stream.MediaReplay;
import com.parrot.drone.groundsdk.internal.session.Session;

/**
 * Implementation of the StreamServer interface which delegates calls to an underlying StreamServerCore.
 */
final class StreamServerProxy implements StreamServer {

    /** Session managing the lifecycle of all refs issued by this proxy instance. */
    @NonNull
    private final Session mSession;

    /** StreamServerCore delegate. */
    @NonNull
    private final StreamServerCore mServer;

    /**
     * Constructor.
     *
     * @param session session that will manage issued refs
     * @param server  stream server delegate to forward calls to
     */
    StreamServerProxy(@NonNull Session session, @NonNull StreamServerCore server) {
        mSession = session;
        mServer = server;
    }

    @Override
    public void enableStreaming(boolean enable) {
        mServer.enableStreaming(enable);
    }

    @Override
    public boolean streamingEnabled() {
        return mServer.streamingEnabled();
    }

    @NonNull
    @Override
    public Ref<CameraLive> live(@NonNull Ref.Observer<CameraLive> observer) {
        return new CameraLiveRef(mServer, mSession, observer);
    }

    @NonNull
    @Override
    public Ref<MediaReplay> replay(@NonNull MediaReplay.Source source, @NonNull Ref.Observer<MediaReplay> observer) {
        try {
            return new MediaReplayRef(mServer, mSession, observer, (MediaSourceCore) source);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid source: " + source, e);
        }
    }
}
