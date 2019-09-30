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

import androidx.annotation.CallSuper;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.stream.Replay;
import com.parrot.drone.sdkcore.TimeProvider;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

/** Core base class for Replay streams. */
public abstract class ReplayCore extends StreamCore implements Replay {

    /** Current media playback state. */
    @NonNull
    private PlayState mPlayState;

    /** Playback duration. */
    @IntRange(from = 0)
    private long mDuration;

    /** Current playback speed. */
    @FloatRange(from = 0)
    private double mSpeed;

    /** Current playback position. */
    @IntRange(from = 0)
    private long mPosition;

    /** Latest playback update timestamp. Base is {@link TimeProvider#elapsedRealtime()}. */
    @IntRange(from = 0)
    private long mTimestamp;

    /**
     * Constructor.
     */
    protected ReplayCore() {
        mPlayState = PlayState.NONE;
    }

    @NonNull
    @Override
    public final PlayState playState() {
        return mPlayState;
    }

    @Override
    public final long duration() {
        return mDuration;
    }

    @Override
    public final long position() {
        return mSpeed == 0 ? mPosition : mPosition + Math.round(mSpeed * (TimeProvider.elapsedRealtime() - mTimestamp));
    }

    @Override
    public final boolean play() {
        return mPlayState != PlayState.PLAYING && queueCommand(PLAY);
    }

    @Override
    public final boolean pause() {
        return mPlayState != PlayState.PAUSED && queueCommand(PAUSE);
    }

    @Override
    public final boolean seekTo(long position) {
        return queueCommand(seek(position));
    }

    @Override
    protected final void onPlaybackStateChange(@NonNull SdkCoreStream.PlaybackState state) {
        long duration = state.duration();
        if (mDuration != duration) {
            mDuration = duration;
            markChanged();
        }

        double speed = state.speed();
        if (Double.compare(mSpeed, speed) != 0) {
            mSpeed = speed;
            markChanged();
        }

        long position = Math.min(state.position(), mDuration);
        if (mPosition != position) {
            mPosition = position;
            markChanged();
        }

        long timestamp = state.timestamp();
        if (mTimestamp != timestamp) {
            mTimestamp = timestamp;
            markChanged();
        }

        PlayState playState = mSpeed == 0 ? PlayState.PAUSED : PlayState.PLAYING;
        if (mPlayState != playState) {
            mPlayState = playState;
            markChanged();
        }
    }

    @CallSuper
    @Override
    protected void onStop() {
        super.onStop();
        mTimestamp = 0;
        mSpeed = 0;
        mPosition = 0;
        mDuration = 0;
        mPlayState = PlayState.NONE;
    }

    /**
     * Playback start command.
     */
    private static final Command PLAY = new Command() {

        @Override
        public void execute(@NonNull SdkCoreStream stream) {
            stream.play();
        }

        @Override
        public String toString() {
            return "PLAY";
        }
    };

    /**
     * Playback pause command.
     */
    private static final Command PAUSE = new Command() {

        @Override
        public void execute(@NonNull SdkCoreStream stream) {
            stream.pause();
        }

        @Override
        public String toString() {
            return "PAUSE";
        }
    };

    /**
     * Creates a new playback seek command.
     *
     * @param position position to seek to
     *
     * @return a new playback seek command
     */
    @NonNull
    private static Command seek(@IntRange(from = 0) long position) {
        return new Command() {

            @Override
            public void execute(@NonNull SdkCoreStream stream) {
                stream.seek(position);
            }

            @NonNull
            @Override
            public String toString() {
                return "SEEK [to: " + position + "]";
            }
        };
    }
}
