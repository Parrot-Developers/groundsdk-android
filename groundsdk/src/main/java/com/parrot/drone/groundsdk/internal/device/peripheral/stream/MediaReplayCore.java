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
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.stream.MediaReplay;
import com.parrot.drone.groundsdk.internal.stream.ReplayCore;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

/**
 * Core class for MediaReplay.
 */
final class MediaReplayCore extends ReplayCore implements MediaReplay {

    /** Stream server managing this stream. */
    @NonNull
    private final StreamServerCore mServer;

    /** Media source. */
    @NonNull
    private final MediaSourceCore mSource;

    /**
     * Constructor.
     *
     * @param server stream server
     * @param source identifies stream source
     */
    MediaReplayCore(@NonNull StreamServerCore server, @NonNull MediaSourceCore source) {
        mServer = server;
        mSource = source;
        mServer.registerStream(this);
    }

    @NonNull
    @Override
    public Source source() {
        return mSource;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mServer.onStreamStopped(this);
    }

    @Override
    protected void onRelease() {
        mServer.unregisterStream(this);
    }

    @Nullable
    @Override
    protected SdkCoreStream openStream(@NonNull SdkCoreStream.Client client) {
        return mSource.openStream(mServer, client);
    }
}
