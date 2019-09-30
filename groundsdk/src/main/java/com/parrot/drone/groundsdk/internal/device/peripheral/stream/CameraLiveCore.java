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

import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive;
import com.parrot.drone.groundsdk.internal.stream.StreamCore;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

/**
 * Core class for CameraLive.
 */
final class CameraLiveCore extends StreamCore implements CameraLive {

    /** Stream server managing this stream. */
    @NonNull
    private final StreamServerCore mServer;

    /** Current camera live playback state. */
    @NonNull
    private PlayState mPlayState;

    /**
     * Constructor.
     *
     * @param server stream server
     */
    CameraLiveCore(@NonNull StreamServerCore server) {
        mServer = server;
        mPlayState = PlayState.NONE;
        mServer.registerStream(this);
    }

    @NonNull
    @Override
    public PlayState playState() {
        return mPlayState;
    }

    @Override
    public boolean play() {
        return mPlayState != PlayState.PLAYING && queueCommand(Command.PLAY);
    }

    @Override
    public boolean pause() {
        return mPlayState != PlayState.PAUSED && queueCommand(Command.PAUSE);
    }

    @Nullable
    @Override
    protected SdkCoreStream openStream(@NonNull SdkCoreStream.Client client) {
        return mServer.openStream(ArsdkDevice.LIVE_URL, null, client);
    }

    @Override
    protected void onPlaybackStateChange(@NonNull SdkCoreStream.PlaybackState state) {
        updatePlayState(state.speed() == 0 ? PlayState.PAUSED : PlayState.PLAYING);
    }

    @Override
    protected boolean onSuspension(@NonNull StreamCore.Command command) {
        if (command == Command.PLAY) {
            updatePlayState(CameraLive.PlayState.PLAYING);
        } else if (command == Command.PAUSE) {
            updatePlayState(CameraLive.PlayState.PAUSED);
        }
        // TODO: when multi cameras are supported, return false if cam is inactive.
        return true;
    }

    @Override
    protected void onStop() {
        mPlayState = PlayState.NONE;
        markChanged();
        mServer.onStreamStopped(this);
    }

    @Override
    protected void onRelease() {
        mServer.unregisterStream(this);
    }

    /**
     * Resumes live stream if interrupted.
     */
    void resume() {
        queueCommand(null);
    }

    /**
     * Updates current playback state.
     *
     * @param state new playback state
     */
    private void updatePlayState(@NonNull PlayState state) {
        if (mPlayState != state) {
            mPlayState = state;
            markChanged();
        }
    }

    /** Playback commands. */
    private enum Command implements StreamCore.Command {

        /** Playback start command. */
        PLAY {

            @Override
            public void execute(@NonNull SdkCoreStream stream) {
                stream.play();
            }
        },

        /** Playback pause command. */
        PAUSE {

            @Override
            public void execute(@NonNull SdkCoreStream stream) {
                stream.pause();
            }
        }
    }
}
