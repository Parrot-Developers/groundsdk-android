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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.stream.Stream;
import com.parrot.drone.sdkcore.stream.SdkCoreMediaInfo;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Core base class for Stream. */
public abstract class StreamCore implements Stream {

    /** Interface for receiving stream change notifications. */
    public interface Observer {

        /**
         * Called back when the stream changes.
         */
        void onChange();
    }

    /** Stream observers. */
    @NonNull
    private final Set<Observer> mObservers;

    /**
     * Core base for Stream.Sink.
     */
    abstract static class Sink implements Stream.Sink {

        /**
         * Core base for Stream.Sink.Config.
         */
        interface Config extends Stream.Sink.Config {

            /**
             * Unwraps a sink config to its internal {@code StreamCore.Sink.Config} representation.
             *
             * @param config sink config to unwrap
             *
             * @return internal {@code StreamCore.Sink.Config} representation of the specified sink config
             *
             * @throws IllegalArgumentException in case the provided sink config is not based upon
             *                                  {@code StreamCore.Sink.Config}
             */
            @NonNull
            static Config unwrap(@NonNull Stream.Sink.Config config) {
                try {
                    return (Config) config;
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Invalid Stream.Sink.Config: " + config, e);
                }
            }

            /**
             * Creates a new sink instance for this config.
             *
             * @param streamCore sink's stream
             *
             * @return a new sink instance
             */
            @NonNull
            Sink newSink(@NonNull StreamCore streamCore);
        }

        /** Sink's stream. */
        @NonNull
        final StreamCore mStream;

        /**
         * Constructor.
         *
         * @param stream sink stream
         */
        Sink(@NonNull StreamCore stream) {
            mStream = stream;
            mStream.mSinks.add(this);
        }

        /**
         * Notifies that {@code SdkCoreStream} is available to the sink.
         *
         * @param stream {@code SdkCoreStream} instance
         */
        abstract void onSdkCoreStreamAvailable(@NonNull SdkCoreStream stream);

        /**
         * Notifies that {@code SdkCoreStream} is not available to the sink anymore.
         */
        abstract void onSdkCoreStreamUnavailable();

        @CallSuper
        @Override
        public void close() {
            if (mStream.mCoreStreamOpen) {
                onSdkCoreStreamUnavailable();
            }
            mStream.mSinks.remove(this);
        }
    }

    /** Media registry. */
    @NonNull
    private final MediaRegistry mMedias;

    /** Stream sinks. */
    @NonNull
    private final Set<Sink> mSinks;

    /** Current stream state. */
    @NonNull
    private State mState;

    /**
     * SdkCoreStream instance. {@code null} when closed.
     */
    @Nullable
    private SdkCoreStream mSdkCoreStream;

    /**
     * {@code true} when {@link #mSdkCoreStream} is completely open.
     */
    private boolean mCoreStreamOpen;

    /**
     * A playback command that can be forwarded to the {@link SdkCoreStream}.
     */
    protected interface Command {

        /**
         * Forwards this command to the given {@code SdkCoreStream} for execution.
         *
         * @param stream {@code SdkCoreStream} to forward the command to
         */
        void execute(@NonNull SdkCoreStream stream);
    }

    /** Latest requested command. {@code null} if none. */
    @Nullable
    private Command mCommand;

    /** {@code true} when this stream has pending changes waiting for {@link #notifyUpdated()} call. */
    private boolean mChanged;

    /** {@code true} when this stream has been released. */
    private boolean mReleased;

    /**
     * Constructor.
     */
    protected StreamCore() {
        mObservers = new CopyOnWriteArraySet<>();
        mMedias = new MediaRegistry();
        mSinks = new HashSet<>();
        mState = State.STOPPED;
    }

    @NonNull
    @Override
    public final State state() {
        return mState;
    }

    @NonNull
    @Override
    public final Sink openSink(@NonNull Stream.Sink.Config config) {
        assertNotReleased();

        Sink sink = Sink.Config.unwrap(config).newSink(this);
        if (mCoreStreamOpen) {
            assert mSdkCoreStream != null;
            sink.onSdkCoreStreamAvailable(mSdkCoreStream);
        }
        return sink;
    }

    /**
     * Registers an observer of this stream.
     *
     * @param observer observer to register
     */
    public final void registerObserver(@NonNull Observer observer) {
        mObservers.add(observer);
    }

    /**
     * Unregisters an observer from this stream.
     *
     * @param observer observer to unregister
     */
    public final void unregisterObserver(@NonNull Observer observer) {
        mObservers.remove(observer);
    }

    /**
     * Opens a new {@code SdkCoreStream} instance for this stream.
     *
     * @param client listener that will receive {@code SdkCoreStream} lifecycle events
     *
     * @return a new {@code SdkCoreStream} instance if successful, otherwise {@code null}
     */
    @Nullable
    protected abstract SdkCoreStream openStream(@NonNull SdkCoreStream.Client client);

    /**
     * Notifies that the stream playback state changes.
     * <p>
     * Subclasses may override this method to properly update their own state. <br>
     * An update to the component will be notified after this method returns.
     *
     * @param state stream playback state
     */
    protected abstract void onPlaybackStateChange(@NonNull SdkCoreStream.PlaybackState state);

    /**
     * Notifies that this stream is about to be suspended.
     * <p>
     * Default implementation does not support suspension and returns {@code false}.
     * <p>
     * An update to the component will be notified after this method returns.
     *
     * @param suspendedCommand command that will be executed upon resuming
     *
     * @return {@code true} to proceed with suspension, {@code false} to stop the stream instead
     */
    protected boolean onSuspension(@NonNull Command suspendedCommand) {
        return false;
    }

    /**
     * Notifies that the stream stops.
     * <p>
     * Subclasses may override this method to properly update their own state. <br>
     * Default implementation does nothing.
     * <p>
     * An update to the component will be notified after this method returns.
     */
    protected void onStop() {
    }

    /**
     * Notifies that the stream has been released.
     * <p>
     * Subclasses may override this method to perform appropriate stream management. <br>
     * Default implementation does nothing.
     */
    protected void onRelease() {
    }

    /**
     * Marks that the stream state has changed and that next call to {@link #notifyUpdated()} must notify registered
     * observers.
     */
    protected final void markChanged() {
        mChanged = true;
    }

    /**
     * Queue a playback command for execution on this stream.
     *
     * @param command command to execute, {@code null} to re-execute latest command, if any
     *
     * @return {@code true} if the command could be queued, otherwise {@code false}
     */
    protected final boolean queueCommand(@Nullable Command command) {
        assertNotReleased();

        if (command != null) {
            mCommand = command;
        }

        if (mCommand == null) {
            return false; // TODO : cannot remember the use case here (why not assert ?)
        }

        if (mSdkCoreStream == null) {
            mSdkCoreStream = openStream(mListener);
            if (mSdkCoreStream == null) {
                return trySuspend();
            }
            updateState(State.STARTING);
            notifyUpdated();
        } else if (mCoreStreamOpen) {
            mCommand.execute(mSdkCoreStream);
        }

        return true;
    }

    /**
     * Interrupts the stream, allowing it (in case supported) to be resumed automatically at a later time.
     */
    public final void interrupt() {
        stop(SdkCoreStream.CloseReason.INTERRUPTED);
    }

    /**
     * Stops the stream.
     */
    public final void stop() {
        stop(SdkCoreStream.CloseReason.USER_REQUESTED);
    }

    /**
     * Releases the stream, stopping it if required.
     * <p>
     * Stream must not be used after this method is called.
     */
    public final void release() {
        assertNotReleased();
        stop(SdkCoreStream.CloseReason.USER_REQUESTED);
        mReleased = true;
        mSinks.clear();
        mObservers.clear();
        onRelease();
    }

    /**
     * Subscribes to stream media availability changes.
     * <p>
     * In case a media of the requested kind is available when this method is called,
     * {@code listener.}{@link MediaListener#onMediaAvailable(SdkCoreMediaInfo) onMediaAvailable()} is called
     * immediately.
     *
     * @param mediaKind kind of media to subscribe to
     * @param listener  listener notified of media availability changes
     * @param <T>       type of media class
     */
    final <T extends SdkCoreMediaInfo> void subscribeToMedia(@NonNull Class<T> mediaKind,
                                                             @NonNull MediaListener<T> listener) {
        mMedias.registerListener(mediaKind, listener);
    }

    /**
     * Unsubscribes from stream media availability changes.
     * <p>
     * In case a media of the subscribed kind is still available when this method is called,
     * {@code listener.}{@link MediaListener#onMediaUnavailable()} onMediaUnavailable()} is called immediately.
     *
     * @param listener listener to unsubscribe
     */
    final void unsubscribeFromMedia(@NonNull MediaListener<?> listener) {
        mMedias.unregisterListener(listener);
    }

    /**
     * Stops the stream, closing internal {@code SdkCoreStream} instance if necessary.
     *
     * @param reason reason why the {@code SdkCoreStream} instance is closed
     */
    private void stop(@NonNull SdkCoreStream.CloseReason reason) {
        assertNotReleased();

        if (mSdkCoreStream != null) {
            mSdkCoreStream.close(reason);
        }

        handleSdkCoreStreamClose(reason);
    }

    /**
     * Called whenever the {@code SdkCoreStream} instance closes, either spontaneously or by direct request from this
     * stream.
     *
     * @param reason reason why this stream is closed
     */
    private void handleSdkCoreStreamClose(@NonNull SdkCoreStream.CloseReason reason) {
        mSdkCoreStream = null;
        if (mCoreStreamOpen) {
            mCoreStreamOpen = false;
            for (Sink sink : mSinks) {
                sink.onSdkCoreStreamUnavailable();
            }
        }
        if (reason != SdkCoreStream.CloseReason.INTERRUPTED || mCommand == null || !trySuspend()) {
            mCommand = null;
            updateState(State.STOPPED);
            notifyUpdated();
        }
    }

    /**
     * Tries to move the stream to SUSPENDED state in case it supports suspension.
     *
     * @return {@code true} if the stream could be suspended, otherwise {@code false}
     */
    private boolean trySuspend() {
        assert mCommand != null;
        if (onSuspension(mCommand)) {
            updateState(State.SUSPENDED);
            notifyUpdated();
            return true;
        }
        return false;
    }

    /**
     * Updates current stream state.
     *
     * @param state new stream state
     */
    private void updateState(@NonNull State state) {
        if (mState != state) {
            mState = state;
            mChanged = true;
            if (mState == State.STOPPED) {
                onStop();
            }
        }
    }

    /**
     * Notifies changes made by previously called update methods.
     */
    private void notifyUpdated() {
        if (mChanged) {
            mChanged = false;
            for (Observer observer : mObservers) {
                observer.onChange();
            }
        }
    }

    /**
     * Asserts that this stream instance is not released.
     *
     * @throws IllegalStateException in case this stream is already released
     */
    private void assertNotReleased() {
        if (mReleased) {
            throw new IllegalStateException("Stream released: " + this);
        }
    }

    /** Listens to SkdCoreStream lifecycle. */
    private final SdkCoreStream.Client mListener = new SdkCoreStream.Client() {

        @Override
        public void onStreamOpened(@NonNull SdkCoreStream stream, @NonNull SdkCoreStream.PlaybackState playbackState) {
            if (mCommand == null) {
                throw new IllegalStateException("No command set before starting stream");
            }
            mCoreStreamOpen = true;
            assert mSdkCoreStream != null;
            for (Sink sink : mSinks) {
                sink.onSdkCoreStreamAvailable(mSdkCoreStream);
            }
            mCommand.execute(stream);
        }

        @Override
        public void onStreamClosed(@NonNull SdkCoreStream stream, @NonNull SdkCoreStream.CloseReason reason) {
            if (stream == mSdkCoreStream) { // another stream may have been open in the meantime
                handleSdkCoreStreamClose(reason);
            }
        }

        @Override
        public void onPlaybackStateChanged(@NonNull SdkCoreStream stream, @NonNull SdkCoreStream.PlaybackState state) {
            onPlaybackStateChange(state);
            updateState(Stream.State.STARTED);
            notifyUpdated();
        }

        @Override
        public void onMediaAdded(@NonNull SdkCoreStream stream, @NonNull SdkCoreMediaInfo mediaInfo) {
            mMedias.addMedia(mediaInfo);
        }

        @Override
        public void onMediaRemoved(@NonNull SdkCoreStream stream, long mediaId) {
            mMedias.removeMedia(mediaId);
        }
    };
}
