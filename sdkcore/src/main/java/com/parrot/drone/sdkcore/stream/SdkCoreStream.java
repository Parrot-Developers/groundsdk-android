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

package com.parrot.drone.sdkcore.stream;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.SdkCore;
import com.parrot.drone.sdkcore.TimeProvider;
import com.parrot.drone.sdkcore.pomp.PompLoop;

import java.io.File;

/**
 * Allows to stream some media source and control playback.
 */
public class SdkCoreStream {

    /**
     * Creates and opens a new stream instance for streaming the given local media file.
     *
     * @param file   file to open
     * @param track  stream track to select, {@code null} to select default track, if any
     * @param client stream client, notified of stream lifecycle events
     *
     * @return a new open stream instance
     */
    @NonNull
    public static SdkCoreStream openFromFile(@NonNull File file, @Nullable String track,
                                             @NonNull SdkCoreStream.Client client) {
        PompLoop pomp = PompLoop.createOnNewThread("stream:" + file.getAbsolutePath());
        SdkCoreStream stream = new SdkCoreStream(pomp, s -> pomp.dispose(), client);
        stream.internalOpen(() -> stream.nativeOpenFile(pomp.nativePtr(), file.getAbsolutePath(), track));
        return stream;
    }

    /** Reason why the stream has been closed. */
    public enum CloseReason {

        /** Unspecified reason. */
        UNSPECIFIED,

        /** Closed by user request. */
        USER_REQUESTED,

        /** Closed by interruption by another stream open request. */
        INTERRUPTED,

        /** Closed for internal reasons. */
        INTERNAL
    }

    /** Aggregates playback state data. */
    public static class PlaybackState {

        /** Playback duration, in milliseconds. {@code 0} if irrelevant */
        @IntRange(from = 0)
        private long mDuration;

        /** Playback speed multiplier. When {@code 0}, playback is paused. */
        @FloatRange(from = 0)
        private double mSpeed;

        /** Playback position, in milliseconds. */
        @IntRange(from = 0)
        private long mPosition;

        /** Time at which playback state was collected. Base is {@link TimeProvider#elapsedRealtime()}. */
        @IntRange(from = 0)
        private long mTimestamp;

        /**
         * Gives playback duration.
         *
         * @return playback duration, in milliseconds, or {@code 0} if irrelevant
         */
        @IntRange(from = 0)
        public long duration() {
            return mDuration;
        }

        /**
         * Gives playback speed.
         *
         * @return playback speed multiplier, {@code 0} when playback is paused
         */
        @FloatRange(from = 0)
        public double speed() {
            return mSpeed;
        }

        /**
         * Gives playback position.
         *
         * @return playback position, in milliseconds
         */
        @IntRange(from = 0)
        public long position() {
            return mPosition;
        }

        /**
         * Gives playback state timestamp.
         *
         * @return playback state timestamp, base is {@link TimeProvider#elapsedRealtime()}
         */
        @IntRange(from = 0)
        public long timestamp() {
            return mTimestamp;
        }

        /**
         * Updates playback state.
         *
         * @param duration  playback duration, in milliseconds, {@code 0} when irrelevant
         * @param speed     playback speed multiplier
         * @param position  playback position, in milliseconds
         * @param timestamp state collection timestamp, base is {@link TimeProvider#elapsedRealtime()}
         */
        void update(@IntRange(from = 0) long duration, @FloatRange(from = 0) double speed,
                    @IntRange(from = 0) long position, @IntRange(from = 0) long timestamp) {
            mDuration = duration;
            mSpeed = speed;
            mPosition = position;
            mTimestamp = timestamp;
        }
    }

    /**
     * Stream client interface.
     * <p>
     * Allows to listen to stream lifecycle, playback state changes, and receive available media notifications.
     * <p>
     * All methods are called on main thread.
     */
    public interface Client {

        /**
         * Notifies that the stream is opened and that playback control is available.
         * <p>
         * Called on main thread.
         *
         * @param stream        opened stream
         * @param playbackState initial stream playback state
         */
        void onStreamOpened(@NonNull SdkCoreStream stream, @NonNull PlaybackState playbackState);

        /**
         * Notifies that the stream is closed.
         * <p>
         * Stream instance must not be used as soon as this method is called.
         * <p>
         * Called on main thread.
         *
         * @param stream closed stream
         * @param reason reason why the stream was closed
         */
        void onStreamClosed(@NonNull SdkCoreStream stream, @NonNull CloseReason reason);

        /**
         * Notifies that the playback state of this stream changed.
         *
         * @param stream        stream whose playback state did change
         * @param playbackState new playback state
         */
        void onPlaybackStateChanged(@NonNull SdkCoreStream stream, @NonNull PlaybackState playbackState);

        /**
         * Notifies that a new media is available for this stream.
         *
         * @param stream    stream for which a new media is available
         * @param mediaInfo information concerning the new media
         */
        void onMediaAdded(@NonNull SdkCoreStream stream, @NonNull SdkCoreMediaInfo mediaInfo);

        /**
         * Notifies that some media became unavailable to this stream.
         *
         * @param stream  stream for which a media became available
         * @param mediaId media {@link SdkCoreMediaInfo#mediaId() identifier}
         */
        void onMediaRemoved(@NonNull SdkCoreStream stream, long mediaId);
    }

    /**
     * Stream controller interface.
     * <p>
     * Notified when {@code SdkCoreStream} instances are completely closed and must be disposed.
     */
    protected interface Controller {

        /**
         * Notifies that the stream is completely closed.
         *
         * @param stream closed stream
         */
        void onStreamClosed(@NonNull SdkCoreStream stream);
    }

    /** Pomp loop. */
    @NonNull
    private final PompLoop mPomp;

    /** Stream controller. */
    @NonNull
    private final Controller mController;

    /** Stream client. */
    @NonNull
    private final Client mClient;

    /** Stream client state. */
    private enum State {

        /** Stream has not been opened yet. */
        IDLE,

        /** Stream open has been requested. */
        OPENING,

        /** Stream is open. Playback control is available. */
        OPEN,

        /** Stream is closed. Cannot be used any further. */
        CLOSED,
    }

    /** Client stream state. Only accessed from main thread. */
    @NonNull
    private State mState;

    /** Stream playback state, non-{@code null} while stream is OPEN. Only accessed from main thread. */
    @Nullable
    private PlaybackState mPlaybackState;

    /** Lock protecting concurrent accesses to {@link #mNativePtr SdkCoreStream native pointer}. */
    @NonNull
    private final Object mNativePtrLock;

    /**
     * Stream close reason. Only modified from pomp thread.
     * When {@code null}, indicates that native close has not been requested yet.
     */
    @Nullable
    private volatile CloseReason mCloseReason;

    /** SdkCoreStream native pointer. */
    private long mNativePtr;

    /**
     * Constructor.
     * <p>
     * Must be called on main thread.
     *
     * @param pomp       pomp loop
     * @param controller stream controller
     * @param client     stream client
     */
    protected SdkCoreStream(@NonNull PompLoop pomp, @NonNull Controller controller, @NonNull Client client) {
        mPomp = pomp;
        mController = controller;
        mClient = client;
        mNativePtrLock = new Object();
        mState = State.IDLE;
    }

    /**
     * Provides access to the stream's native backend.
     *
     * @return native pointer on the stream backend
     *
     * @throws IllegalStateException if not called from the stream's pomp loop
     */
    final long nativePtr() {
        if (!mPomp.inPomp()) {
            throw new IllegalStateException("Not on pomp");
        }
        return mNativePtr;
    }

    /**
     * Resumes stream playback.
     * <p>
     * Must be called on main thread.
     *
     * @throws IllegalStateException in case the stream is not opened
     */
    public void play() {
        if (mState != State.OPEN) {
            throw new IllegalStateException("Stream not open");
        }
        mPomp.onPomp(() -> {
            if (mNativePtr != 0 && mCloseReason == null) {
                nativePlay(mNativePtr);
            }
        });
    }

    /**
     * Pauses stream playback.
     * <p>
     * Must be called on main thread.
     *
     * @throws IllegalStateException in case the stream is not opened
     */
    public void pause() {
        if (mState != State.OPEN) {
            throw new IllegalStateException("Stream not open");
        }
        mPomp.onPomp(() -> {
            if (mNativePtr != 0 && mCloseReason == null) {
                nativePause(mNativePtr);
            }
        });
    }

    /**
     * Seeks to some position in the stream.
     *
     * @param position position to seek to, in milliseconds
     *
     * @throws IllegalStateException in case the stream is not opened
     */
    public void seek(@IntRange(from = 0) long position) {
        if (mState != State.OPEN) {
            throw new IllegalStateException("Stream not open");
        }
        assert mPlaybackState != null;
        if (position > mPlaybackState.duration()) {
            throw new IllegalArgumentException("Tried to seek past stream duration");
        }
        mPomp.onPomp(() -> {
            if (mNativePtr != 0 && mCloseReason == null) {
                nativeSeek(mNativePtr, position);
            }
        });
    }

    /**
     * Starts stream renderer.
     * <p>
     * Must be called on rendering thread. Stream must be opened
     *
     * @param renderer renderer to start
     * @param listener listener notified of rendering events
     *
     * @return {@code true} in case rendering started, otherwise {@code false}
     */
    public boolean startRenderer(@NonNull SdkCoreRenderer renderer, @NonNull SdkCoreRenderer.Listener listener) {
        synchronized (mNativePtrLock) {
            return mNativePtr != 0 && mCloseReason == null && renderer.start(mNativePtr, mPomp, listener);
        }
    }

    /**
     * Starts stream sink.
     * <p>
     * Must be called on main thread. Stream must be opened.
     *
     * @param sink    sink to start
     * @param mediaId identifies the stream media to deliver to the sink
     *
     * @throws IllegalStateException in case the stream is not opened
     */
    public void startSink(@NonNull SdkCoreSink sink, long mediaId) {
        if (mState != State.OPEN) {
            throw new IllegalStateException("Stream not open");
        }
        sink.start(this, mPomp, mediaId);
    }

    /**
     * Closes the stream, without any specified reason.
     * <p>
     * Must be called on main thread.
     *
     * @throws IllegalStateException in case the stream is already closed or has been requested to close
     * @see #close(CloseReason)
     */
    public void close() {
        close(CloseReason.UNSPECIFIED);
    }

    /**
     * Closes the stream.
     * <p>
     * Must be called on main thread.
     *
     * @param reason reason why this stream has to be closed
     */
    public void close(@NonNull CloseReason reason) {
        switch (mState) {
            case IDLE:
                mState = State.CLOSED;
                mClient.onStreamClosed(this, reason);
                mController.onStreamClosed(this);
                break;
            case OPEN:
            case OPENING:
                mState = State.CLOSED;
                mPomp.onPomp(() -> {
                    if (mNativePtr != 0 && mCloseReason == null) {
                        mCloseReason = reason;
                        nativeClose(mNativePtr);
                    } // otherwise, stream close race. Ignore
                });
                break;
            case CLOSED:
                break;
        }
    }

    /** Allows to abstract how the stream is to be opened. */
    protected interface OpenMethod {

        /**
         * Opens the stream.
         *
         * @return SdkCoreStream native pointer; {@code 0} in case of failure
         */
        long open();
    }

    /**
     * Opens the stream.
     * <p>
     * Must be called on main thread.
     * <p>
     * Subclasses must call this method with the appropriate {@link OpenMethod} to open the stream.
     *
     * @param openMethod method to use to open the stream
     *
     * @throws IllegalStateException in case the stream is already opening, opened, or has already been closed
     */
    protected final void internalOpen(@NonNull OpenMethod openMethod) {
        switch (mState) {
            case IDLE:
                mState = State.OPENING;
                mPomp.onPomp(() -> {
                    mNativePtr = openMethod.open();
                    if (mNativePtr == 0) {
                        onClosing();
                        onClosed();
                    }
                });
                break;
            case OPENING:
            case OPEN:
                throw new IllegalStateException("Stream already opened");
            case CLOSED:
                throw new IllegalStateException("Cannot reopen closed stream");
        }
    }

    /**
     * Notifies that stream playback state has changed.
     *
     * @param duration playback duration, in milliseconds, 0 when irrelevant
     * @param position playback position, in milliseconds
     * @param speed    playback speed multiplier, 0 when paused
     */
    @SuppressWarnings("unused") /* native callback */
    private void onPlaybackState(@IntRange(from = 0) long duration, @IntRange(from = 0) long position,
                                 @FloatRange(from = 0) double speed) {
        long timestamp = TimeProvider.elapsedRealtime();
        mPomp.onMain(() -> {
            if (mState == State.OPENING) {
                mState = State.OPEN;
                mPlaybackState = new PlaybackState();
                mPlaybackState.update(duration, speed, position, timestamp);
                mClient.onStreamOpened(this, mPlaybackState);
            } else if (mState == State.OPEN) {
                assert mPlaybackState != null;
                mPlaybackState.update(duration, speed, position, timestamp);
                mClient.onPlaybackStateChanged(this, mPlaybackState);
            }
        });
    }

    /**
     * Notifies that the stream has been requested to close and closing procedure did start.
     */
    @SuppressWarnings("unused") /* native callback */
    private void onClosing() {
        if (mCloseReason == null) {
            mCloseReason = CloseReason.INTERNAL;
        }
        mPomp.onMain(() -> {
            mState = State.CLOSED;
            mPlaybackState = null;
            //noinspection ConstantConditions: mCloseReason is never changed once set
            mClient.onStreamClosed(this, mCloseReason);
        });
    }

    /**
     * Notifies that the stream is closed.
     */
    @SuppressWarnings("unused") /* native callback */
    private void onClosed() {
        synchronized (mNativePtrLock) {
            mNativePtr = 0;
        }
        mPomp.onMain(() -> mController.onStreamClosed(this));
    }

    /**
     * Notifies that a new media is available for the stream.
     *
     * @param mediaInfo information concerning the new media
     */
    @SuppressWarnings("unused") /* native callback */
    private void onMediaAdded(@NonNull SdkCoreMediaInfo mediaInfo) {
        mPomp.onMain(() -> mClient.onMediaAdded(this, mediaInfo));
    }

    /**
     * Notifies that some media became unavailable to the stream.
     *
     * @param mediaId media {@link SdkCoreMediaInfo#mediaId() identifier}
     */
    @SuppressWarnings("unused") /* native callback */
    private void onMediaRemoved(long mediaId) {
        mPomp.onMain(() -> mClient.onMediaRemoved(this, mediaId));
    }

    /* JNI declarations and setup */
    private native long nativeOpenFile(long pompNativePtr, @NonNull String path, @Nullable String track);

    private static native void nativePlay(long nativePtr);

    private static native void nativePause(long nativePtr);

    private static native void nativeSeek(long nativePtr, long position);

    private static native void nativeClose(long nativePtr);

    private static native void nativeClassInit();

    static {
        SdkCore.init();
        nativeClassInit();
        SdkCoreMediaInfo.nativeClassesInit(); // class instance(s) created from native
    }
}
