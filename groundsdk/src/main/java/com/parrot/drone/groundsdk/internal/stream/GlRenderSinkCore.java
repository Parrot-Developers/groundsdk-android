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

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.stream.Overlayer;
import com.parrot.drone.groundsdk.stream.Overlayer2;
import com.parrot.drone.groundsdk.stream.TextureLoader;
import com.parrot.drone.sdkcore.stream.SdkCoreOverlayer;
import com.parrot.drone.sdkcore.stream.SdkCoreRenderer;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;
import com.parrot.drone.sdkcore.stream.SdkCoreTextureLoader;

/** Core class for GlRenderSink. */
final class GlRenderSinkCore extends StreamCore.Sink implements GlRenderSink {

    /** Core class for GlRenderSink config. */
    static final class Config implements StreamCore.Sink.Config {

        /** Callback notified of sink events. */
        @NonNull
        private final Callback mCallback;

        /**
         * Constructor.
         *
         * @param callback callback notified of stream events
         */
        Config(@NonNull Callback callback) {
            mCallback = callback;
        }

        @Override
        @NonNull
        public GlRenderSinkCore newSink(@NonNull StreamCore stream) {
            return new GlRenderSinkCore(stream, this);
        }
    }

    /** Sink config. */
    @NonNull
    private final Config mConfig;

    /** SdkCoreStream instance. Set from main thread, accessed from renderer thread when starting renderer. */
    @Nullable
    private volatile SdkCoreStream mSdkCoreStream;

    /**
     * Constructor.
     *
     * @param stream sink's stream
     * @param config sink config
     */
    private GlRenderSinkCore(@NonNull StreamCore stream, @NonNull Config config) {
        super(stream);
        mConfig = config;
    }

    @Override
    public void onSdkCoreStreamAvailable(@NonNull SdkCoreStream stream) {
        mSdkCoreStream = stream;
        mConfig.mCallback.onRenderingMayStart(mRenderer);
    }

    @Override
    public void onSdkCoreStreamUnavailable() {
        mSdkCoreStream = null;
        mConfig.mCallback.onRenderingMustStop(mRenderer);
    }

    /** Internal video renderer. */
    private final Renderer mRenderer = new Renderer() {

        /** Configured scale type. */
        @NonNull
        private ScaleType mScaleType = ScaleType.FIT;

        /** Configured padding fill. */
        @NonNull
        private PaddingFill mPaddingFill = PaddingFill.NONE;

        /** Internal SdkCoreRenderer instance. {@code null} unless rendering is started. */
        @Nullable
        private SdkCoreRenderer mSdkCoreRenderer;

        @Override
        public boolean start(@Nullable TextureLoader textureLoader) {
            if (mSdkCoreRenderer != null) {
                return false;
            }

            SdkCoreRenderer renderer = new SdkCoreRenderer();
            TextureLoader.TextureSpec textureSpec = textureLoader == null ? null : textureLoader.getTextureSpec();
            if (textureSpec == null
                || renderer.setTextureLoaderCallback(textureSpec.getWidth(), textureSpec.getAspectRatioNumerator(),
                    textureSpec.getAspectRatioDenominator(), newTextureLoaderCallback(textureLoader))) {
                SdkCoreStream stream = mSdkCoreStream;
                if (stream != null && stream.startRenderer(renderer, mRendererListener)) {
                    mSdkCoreRenderer = renderer;
                    return true;
                }
            }

            renderer.dispose();
            return false;
        }

        @Override
        public boolean setRenderZone(@NonNull Rect renderZone) {
            return mSdkCoreRenderer != null && mSdkCoreRenderer.setRenderZone(renderZone);
        }

        @Override
        public boolean setScaleType(@NonNull ScaleType scaleType) {
            mScaleType = scaleType;
            return mSdkCoreRenderer != null && mSdkCoreRenderer.setFillMode(fillModeFrom(mScaleType, mPaddingFill));
        }

        @Override
        public boolean setPaddingFill(@NonNull PaddingFill paddingFill) {
            mPaddingFill = paddingFill;
            return mSdkCoreRenderer != null && mSdkCoreRenderer.setFillMode(fillModeFrom(mScaleType, mPaddingFill));
        }

        @Override
        public boolean enableZebras(boolean enable) {
            return mSdkCoreRenderer != null && mSdkCoreRenderer.enableZebras(enable);
        }

        @Override
        public boolean setZebraThreshold(double threshold) {
            return mSdkCoreRenderer != null && mSdkCoreRenderer.setZebraThreshold(threshold);
        }

        @Override
        public boolean enableHistogram(boolean enable) {
            return mSdkCoreRenderer != null && mSdkCoreRenderer.enableHistogram(enable);
        }

        @Override
        public boolean setOverlayer(@Nullable Overlayer2 overlayer) {
            return mSdkCoreRenderer != null && mSdkCoreRenderer.setOverlayCallback(
                    overlayer == null ? null : newOverlayerCallback(overlayer));
        }

        @Override
        public boolean renderFrame() {
            return mSdkCoreRenderer != null && mSdkCoreRenderer.renderFrame();
        }

        @Override
        public boolean stop() {
            if (mSdkCoreRenderer != null && mSdkCoreRenderer.stop()) {
                mSdkCoreRenderer.dispose();
                mSdkCoreRenderer = null;
                return true;
            }
            return false;
        }

        /**
         * Creates an {@code SdkCoreOverlayer.Callback} that forwards events to the given overlayer.
         *
         * @param overlayer overlayer to forward events to
         *
         * @return a new {@code SdkCoreOverlayer.Callback} instance
         */
        @NonNull
        private SdkCoreOverlayer.Callback newOverlayerCallback(@NonNull Overlayer2 overlayer) {
            return new SdkCoreOverlayer.Callback() {

                /** Adapts {@code SdkCoreOverlayer} contextual information. */
                final class Context implements Overlayer2.OverlayContext,
                        Overlayer2.Histogram, Overlayer.Histogram {

                    /** SdkCoreOverlayer delegate. */
                    @SuppressWarnings("NullableProblems") // always set before Context is forwarded
                    @NonNull
                    SdkCoreOverlayer mSdkCoreOverlayer;

                    @NonNull
                    @Override
                    public Rect renderZone() {
                        return mSdkCoreOverlayer.renderZone();
                    }

                    @NonNull
                    @Override
                    public Rect contentZone() {
                        return mSdkCoreOverlayer.contentZone();
                    }

                    @Override
                    public long sessionInfoHandle() {
                        return mSdkCoreOverlayer.sessionInfoHandle();
                    }

                    @Override
                    public long sessionMetadataHandle() {
                        return mSdkCoreOverlayer.sessionMetadataHandle();
                    }

                    @Override
                    public long frameMetadataHandle() {
                        return mSdkCoreOverlayer.frameMetadataHandle();
                    }

                    @NonNull
                    @Override
                    public Overlayer2.Histogram histogram() {
                        return this;
                    }

                    @NonNull
                    @Override
                    public float[] redChannel() {
                        return mSdkCoreOverlayer.redChannelHistogram();
                    }

                    @NonNull
                    @Override
                    public float[] greenChannel() {
                        return mSdkCoreOverlayer.greenChannelHistogram();
                    }

                    @NonNull
                    @Override
                    public float[] blueChannel() {
                        return mSdkCoreOverlayer.blueChannelHistogram();
                    }

                    @NonNull
                    @Override
                    public float[] luminanceChannel() {
                        return mSdkCoreOverlayer.luminanceChannelHistogram();
                    }
                }

                /** Latest context state. Reused across {@link #onOverlay} calls to reduce allocations. */
                @NonNull
                private final Context mContext = new Context();

                @Override
                public void onOverlay(@NonNull SdkCoreOverlayer sdkCoreOverlayer) {
                    mContext.mSdkCoreOverlayer = sdkCoreOverlayer;
                    overlayer.overlay(mContext);
                }
            };
        }

        /**
         * Creates an {@code SdkCoreTextureLoader.Callback} that forwards events to the given texture loader.
         *
         * @param textureLoader texture loader to forward events to
         *
         * @return a new {@code SdkCoreTextureLoader.Callback} instance
         */
        @NonNull
        private SdkCoreTextureLoader.Callback newTextureLoaderCallback(@NonNull TextureLoader textureLoader) {
            return new SdkCoreTextureLoader.Callback() {

                /** Adapts {@code SdkCoreTextureLoader} contextual information. */
                final class Context implements TextureLoader.FrameContext, TextureLoader.TextureContext {

                    /** SdkCoreTextureLoader delegate. */
                    @SuppressWarnings("NullableProblems") // always set before FrameContext is forwarded
                    @NonNull
                    SdkCoreTextureLoader mSdkCoreTextureLoader;

                    @Override
                    public long frameHandle() {
                        return mSdkCoreTextureLoader.frameHandle();
                    }

                    @Override
                    public long frameUserDataHandle() {
                        return mSdkCoreTextureLoader.frameUserDataHandle();
                    }

                    @Override
                    public long frameUserDataSize() {
                        return mSdkCoreTextureLoader.frameUserDataSize();
                    }

                    @Override
                    public long sessionMetadataHandle() {
                        return mSdkCoreTextureLoader.sessionMetadataHandle();
                    }

                    @Override
                    public int textureWidth() {
                        return mSdkCoreTextureLoader.textureWidth();
                    }

                    @Override
                    public int textureHeight() {
                        return mSdkCoreTextureLoader.textureHeight();
                    }
                }

                /** Latest frame & texture context. Reused across {@link #onLoadTexture} calls to reduce allocations. */
                @NonNull
                private final Context mContext = new Context();

                @Override
                public boolean onLoadTexture(@NonNull SdkCoreTextureLoader sdkCoreTextureLoader) {
                    mContext.mSdkCoreTextureLoader = sdkCoreTextureLoader;
                    return textureLoader.loadTexture(mContext, mContext);
                }
            };
        }

        /** Listens to internal renderer events. */
        private final SdkCoreRenderer.Listener mRendererListener = new SdkCoreRenderer.Listener() {

            @Override
            public void onFrameReady() {
                mConfig.mCallback.onFrameReady(mRenderer);
            }

            @Override
            public void onContentZoneChanged(@NonNull Rect contentZone) {
                mConfig.mCallback.onContentZoneChange(contentZone);
            }
        };
    };

    /**
     * Adapts both a scale type and a padding fill to its {@link SdkCoreRenderer.FillMode} equivalent.
     *
     * @param scaleType   scale type to adapt
     * @param paddingFill padding fill to adapt
     *
     * @return corresponding {@code SdkCoreRenderer.FillMode}
     */
    @SdkCoreRenderer.FillMode
    private static int fillModeFrom(@NonNull Renderer.ScaleType scaleType, @NonNull Renderer.PaddingFill paddingFill) {
        switch (scaleType) {
            case FIT:
                switch (paddingFill) {
                    case NONE:
                        return SdkCoreRenderer.FILL_MODE_FIT;
                    case BLUR_CROP:
                        return SdkCoreRenderer.FILL_MODE_FIT_PAD_BLUR_CROP;
                    case BLUR_EXTEND:
                        return SdkCoreRenderer.FILL_MODE_FIT_PAD_BLUR_EXTEND;
                }
            case CROP:
                return SdkCoreRenderer.FILL_MODE_CROP;
        }
        throw new IllegalArgumentException();
    }
}
