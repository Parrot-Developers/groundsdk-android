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

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdk.internal.component.ComponentCore;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.session.Session;
import com.parrot.drone.groundsdk.internal.stream.StreamCore;
import com.parrot.drone.groundsdk.stream.Stream;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

import java.util.HashSet;
import java.util.Set;

/** Core class for StreamServer. */
public final class StreamServerCore extends ComponentCore {

    /** Description of StreamServer. */
    private static final ComponentDescriptor<Peripheral, StreamServer> DESC =
            ComponentDescriptor.of(StreamServer.class);

    /** Backend of a StreamServerCore which handles the messages. */
    public interface Backend {

        /**
         * Opens an internal {@code SdkCoreStream} instance.
         *
         * @param url    url of the remote stream to open
         * @param track  stream track to select, {@code null} to select default track, if any
         * @param client stream client, notified of stream lifecycle events
         *
         * @return a new {@code SdkCoreStream} instance if successful, otherwise {@code null}
         */
        @Nullable
        SdkCoreStream openStream(@NonNull String url, @Nullable String track, @NonNull SdkCoreStream.Client client);
    }

    /** Backend of this peripheral. */
    @NonNull
    private final Backend mBackend;

    /**
     * Base implementation for {@code Ref} issued by this component.
     * <p>
     * This takes care of bookkeeping all issued refs so that they can be closed upon {@link #unpublish()}.
     *
     * @param <T> type of referenced object
     */
    abstract class Ref<T> extends Session.RefBase<T> {

        /**
         * Constructor.
         *
         * @param session  session that will manage this ref
         * @param observer observer that will be notified when the referenced object is updated
         */
        Ref(@NonNull Session session, @NonNull Observer<? super T> observer) {
            super(session, observer);
            mRefs.add(this);
        }

        @CallSuper
        @Override
        protected void release() {
            mRefs.remove(this);
            super.release();
        }
    }

    /** Stores all references issued by this component. */
    @NonNull
    private final Set<Ref<?>> mRefs;

    /** All currently maintained streams. */
    @NonNull
    private final Set<StreamCore> mStreams;

    /** {@code true} when streaming is enabled. */
    private boolean mStreamingEnabled;

    /** Camera live stream shared instance. */
    @Nullable
    private CameraLiveCore mCameraLive;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public StreamServerCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
        mRefs = new HashSet<>();
        mStreams = new HashSet<>();
    }

    @Override
    public void publish() {
        mStreamingEnabled = true;
        super.publish();
    }

    @Override
    public void unpublish() {
        if (mCameraLive != null) {
            mCameraLive.release();
            mCameraLive = null;
        }
        while (!mRefs.isEmpty()) {
            mRefs.iterator().next().close();
        }
        super.unpublish();
    }

    @Override
    @NonNull
    protected StreamServer getProxy(@NonNull Session session) {
        return new StreamServerProxy(session, this);
    }

    /**
     * Controls global streaming capability.
     * <p>
     * All open streams are interrupted when streaming gets disabled. <br>
     * Live stream is resumed (if appropriate) when streaming is enabled.
     *
     * @param enable {@code true} to enable streaming, {@code false} to disable it
     */
    void enableStreaming(boolean enable) {
        if (enable != mStreamingEnabled) {
            mStreamingEnabled = enable;
            mChanged = true;
            if (mStreamingEnabled) {
                resumeLive();
            } else for (StreamCore stream : mStreams) {
                stream.interrupt();
            }
            notifyUpdated();
        }
    }

    /**
     * Tells whether streaming is enabled.
     *
     * @return {@code true} if streaming is enabled, otherwise {@code false}
     */
    boolean streamingEnabled() {
        return mStreamingEnabled;
    }

    /**
     * Gets shared camera live stream.
     * <p>
     * Stream is instantiated if necessary.
     *
     * @return shared camera live stream instance
     */
    @NonNull
    CameraLiveCore getCameraLive() {
        if (mCameraLive == null) {
            mCameraLive = new CameraLiveCore(this);
        }
        return mCameraLive;
    }

    /**
     * Creates a new media replay stream.
     *
     * @param source identifies the media to stream
     *
     * @return a new media replay stream instance
     */
    @NonNull
    MediaReplayCore newMediaReplay(@NonNull MediaSourceCore source) {
        return new MediaReplayCore(this, source);
    }

    /**
     * Opens an internal {@code SdkCoreStream} instance.
     * <p>
     * Returns {@code null} in case streaming is currently {@link #streamingEnabled() disabled}.
     *
     * @param url    url of the remote stream to open
     * @param track  stream track to select, {@code null} to select default track, if any
     * @param client stream client, notified of stream lifecycle events
     *
     * @return a new {@code SdkCoreStream} instance if successful, otherwise {@code null}
     */
    @Nullable
    SdkCoreStream openStream(@NonNull String url, @Nullable String track,
                             @NonNull SdkCoreStream.Client client) {
        return mStreamingEnabled ? mBackend.openStream(url, track, client) : null;
    }

    /**
     * Registers a stream.
     *
     * @param stream stream to register
     */
    void registerStream(@NonNull StreamCore stream) {
        mStreams.add(stream);
    }

    /**
     * Unregisters a stream.
     *
     * @param stream stream to unregister
     */
    void unregisterStream(@NonNull StreamCore stream) {
        mStreams.remove(stream);
    }

    /**
     * Notifies that a given stream did stop.
     * <p>
     * In case all other streams are stopped, resumes interrupted live stream if appropriate.
     *
     * @param stoppedStream stream that stopped
     */
    void onStreamStopped(@SuppressWarnings("unused") @NonNull StreamCore stoppedStream) {
        for (StreamCore stream : mStreams) {
            Stream.State state = stream.state();
            if (state != Stream.State.SUSPENDED && state != Stream.State.STOPPED) {
                return;
            }
        }
        resumeLive();
    }

    /**
     * Resumes live stream in case it is interrupted.
     */
    private void resumeLive() {
        if (mCameraLive != null) {
            mCameraLive.resume();
        }
    }
}
