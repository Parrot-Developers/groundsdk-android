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

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.stream.SdkCoreMediaInfo;
import com.parrot.drone.sdkcore.stream.SdkCoreSink;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

/** Core class for YUVSink. */
final class YUVSinkCore extends StreamCore.Sink implements YUVSink {

    /** Core class for YUVSink config. */
    static final class Config implements StreamCore.Sink.Config {

        /** Looper onto which callbacks are dispatched. */
        @NonNull
        private final Looper mLooper;

        /** Callback notified of sink events. */
        @NonNull
        private final Callback mCallback;

        /**
         * Constructor.
         *
         * @param looper   looper onto which callback are dispatched
         * @param callback callback notified of stream events
         */
        Config(@NonNull Looper looper, @NonNull Callback callback) {
            mLooper = looper;
            mCallback = callback;
        }

        @NonNull
        @Override
        public StreamCore.Sink newSink(@NonNull StreamCore streamCore) {
            return new YUVSinkCore(streamCore, this);
        }
    }

    /** Sink config. */
    @NonNull
    private final Config mConfig;

    /** Handler used to dispatch callbacks onto the configured looper. */
    @NonNull
    private final Handler mListenerHandler;

    /** SdkCoreStream instance. {@code null} unless the stream is opened. */
    @Nullable
    private SdkCoreStream mSdkCoreStream;

    /** Internal SdkCoreSink instance. */
    @NonNull
    private final SdkCoreSink mSdkCoreSink;

    /**
     * Constructor.
     *
     * @param stream sink's stream
     * @param config sink config
     */
    private YUVSinkCore(@NonNull StreamCore stream, @NonNull Config config) {
        super(stream);
        mConfig = config;
        mListenerHandler = new Handler(mConfig.mLooper);
        mSdkCoreSink = new SdkCoreSink(mConfig.mLooper, mSinkListener)
                .setQueueSize(1)
                .setQueueFullPolicy(SdkCoreSink.QUEUE_FULL_POLICY_DROP_ELDEST);
    }

    @Override
    void onSdkCoreStreamAvailable(@NonNull SdkCoreStream stream) {
        mSdkCoreStream = stream;
        mStream.subscribeToMedia(SdkCoreMediaInfo.Video.Yuv.class, mMediaListener);
    }

    @Override
    void onSdkCoreStreamUnavailable() {
        mStream.unsubscribeFromMedia(mMediaListener);
    }

    /** Listener notified of stream YUV media availability. */
    private final MediaListener<SdkCoreMediaInfo.Video.Yuv> mMediaListener =
            new MediaListener<SdkCoreMediaInfo.Video.Yuv>() {

                @Override
                public void onMediaAvailable(@NonNull SdkCoreMediaInfo.Video.Yuv mediaInfo) {
                    assert mSdkCoreStream != null;
                    mListenerHandler.post(() -> mConfig.mCallback.onStart(YUVSinkCore.this));
                    mSdkCoreStream.startSink(mSdkCoreSink, mediaInfo.mediaId());
                }

                @Override
                public void onMediaUnavailable() {
                    mSdkCoreSink.stop();
                }
            };

    /** Listens to internal sink events. */
    @SuppressWarnings("FieldCanBeLocal")
    private final SdkCoreSink.Listener mSinkListener = new SdkCoreSink.Listener() {

        @Override
        public void onFrame(@SdkCoreSink.Frame.Handle long frameHandle) {
            // TODO : reduce gc pressure.
            // TODO   One solution is to say that Frame frame in onFrame(frame) is only valid for the duration of the
            // TODO   callback, so we can reuse a single Frame instance. Then we should provide copy semantics for
            // TODO   Frame and also a way to allocate 'empty' Frame(s) so that the client can implement a pool if
            // TODO   necessary.
            // TODO   Another solution is to pool frames ourselves directly.
            mConfig.mCallback.onFrame(YUVSinkCore.this, new Frame() {

                /** {@code true} when the frame has been released. */
                private boolean mReleased;

                @Override
                public long nativePtr() {
                    return mReleased ? 0 : SdkCoreSink.Frame.nativePtr(frameHandle);
                }

                @Override
                public void release() {
                    if (!mReleased) {
                        SdkCoreSink.Frame.release(frameHandle);
                        mReleased = true;
                    }
                }
            });
        }

        @Override
        public void onStop() {
            mConfig.mCallback.onStop(YUVSinkCore.this);
        }
    };
}
