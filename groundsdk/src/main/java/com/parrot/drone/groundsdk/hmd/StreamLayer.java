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

package com.parrot.drone.groundsdk.hmd;

import android.graphics.Rect;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.internal.stream.GlRenderSink;
import com.parrot.drone.groundsdk.stream.Overlayer2;

import static android.opengl.GLES30.*;

/**
 * Drone video stream layer.
 * <p>
 * This layers manages the GL {@link GlRenderSink renderer} from a drone video stream and allows to render the latter to
 * the {@link Compositor} framebuffer.
 */
final class StreamLayer extends Compositor.Layer {

    /**
     * Stream GL renderer.
     * <p>
     * Note: Under some circumstances, the stream renderer does not render anything at all. To avoid a black layer in
     * that case, we use an intermediate fbo, that we never clear, which will always contain the latest rendered frame,
     * so that we always have something to display.
     */
    private final class GlRenderer {

        /** GL Frame Buffer Object. */
        private final int mGlFBo;

        /** GL Render Buffer Object. */
        private final int mGlRbo;

        /** Area where video stream must be rendered. */
        private final Rect mRenderZone;

        /**
         * Constructor.
         */
        GlRenderer() {
            int[] names = new int[1];

            glGenRenderbuffers(1, names, 0);
            mGlRbo = names[0];

            glBindRenderbuffer(GL_RENDERBUFFER, mGlRbo);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_RGB8, 0, 0);

            glGenFramebuffers(1, names, 0);
            mGlFBo = names[0];

            glBindFramebuffer(GL_FRAMEBUFFER, mGlFBo);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, mGlRbo);

            glBindRenderbuffer(GL_RENDERBUFFER, 0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);

            mRenderZone = new Rect();
        }

        /**
         * Sets rendering size.
         *
         * @param width  rendering width, in pixels
         * @param height rendering height in pixels
         */
        void resize(@IntRange(from = 0) int width, @IntRange(from = 0) int height) {
            mRenderZone.set(0, 0, width, height);
            mConfig.mConfigure = true; // mark config change to reconfigure render zone

            glBindRenderbuffer(GL_RENDERBUFFER, mGlRbo);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_RGB8, width, height);

            glBindFramebuffer(GL_FRAMEBUFFER, mGlFBo);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, mGlRbo);

            glBindRenderbuffer(GL_RENDERBUFFER, 0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }

        /**
         * Renders the layer.
         */
        void render() {
            // render stream to intermediate framebuffer
            glBindFramebuffer(GL_FRAMEBUFFER, mGlFBo);

            if (mConfig.mConfigure) {
                mConfig.mConfigure = false;

                if (mStreamRenderer.start(null)) {
                    mStreamRenderer.setScaleType(GlRenderSink.Renderer.ScaleType.FIT);
                    mStreamRenderer.setPaddingFill(GlRenderSink.Renderer.PaddingFill.NONE);
                }

                mStreamRenderer.enableZebras(mConfig.mZebrasEnabled);
                mStreamRenderer.setZebraThreshold(mConfig.mZebraThreshold);
                mStreamRenderer.enableHistogram(mConfig.mHistogram);
                mStreamRenderer.setOverlayer(mConfig.mOverlayer);
                mStreamRenderer.setRenderZone(mRenderZone);
            }

            mStreamRenderer.renderFrame();

            glBindFramebuffer(GL_FRAMEBUFFER, compositorFbo());

            // render intermediate framebuffer to compositor framebuffer
            glBindFramebuffer(GL_READ_FRAMEBUFFER, mGlFBo);

            glBlitFramebuffer(mRenderZone.left, mRenderZone.top, mRenderZone.right, mRenderZone.bottom,
                    mRenderZone.left, mRenderZone.top, mRenderZone.right, mRenderZone.bottom,
                    GL_COLOR_BUFFER_BIT, GL_LINEAR);

            glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        }

        /**
         * Destroys renderer, freeing all allocated GL resources.
         */
        void destroy() {
            mStreamRenderer.stop();

            int[] names = new int[1];

            names[0] = mGlFBo;
            glDeleteFramebuffers(1, names, 0);

            names[0] = mGlRbo;
            glDeleteRenderbuffers(1, names, 0);
        }
    }

    /** Device stream renderer. */
    @NonNull
    private final GlRenderSink.Renderer mStreamRenderer;

    /**
     * Collects drone video stream configuration.
     */
    static final class Config {

        /** {@code true} when zebras rendering is enabled. */
        private boolean mZebrasEnabled;

        /** Zebra rendering threshold. */
        private double mZebraThreshold;

        /** {@code true} when color histogram computation is enabled. */
        private boolean mHistogram;

        /** Video stream overlayer, {@code null} when disabled. */
        @Nullable
        private Overlayer2 mOverlayer;

        /** {@code true} when configuration changed and the stream renderer must be reconfigured. */
        boolean mConfigure;

        /**
         * Configures overexposure zebras rendering.
         *
         * @param enable {@code true} to enable zebras rendering, {@code false} to disable it
         */
        void enableZebras(boolean enable) {
            mZebrasEnabled = enable;
            mConfigure = true;
        }

        /**
         * Configures overexposure zebra threshold.
         *
         * @param threshold desired zebra threshold
         */
        void setZebraThreshold(@FloatRange(from = 0, to = 1) double threshold) {
            mZebraThreshold = threshold;
            mConfigure = true;
        }

        /**
         * Configures color histogram computation.
         *
         * @param enable {@code true} to enable histogram computation, {@code false} to disable it
         */
        void enableHistogram(boolean enable) {
            mHistogram = enable;
            mConfigure = true;
        }

        /**
         * Configures rendering overlayer.
         *
         * @param overlayer overlayer to configure, {@code null} to disable rendering overlay
         */
        void setOverlayer(@Nullable Overlayer2 overlayer) {
            mOverlayer = overlayer;
            mConfigure = true;
        }
    }

    /** Video stream configuration. */
    @NonNull
    private final Config mConfig;

    /** Stream layer GL renderer. */
    @Nullable
    private GlRenderer mGlRenderer;

    /**
     * Constructor.
     *
     * @param renderer stream renderer
     * @param config   stream config
     */
    StreamLayer(@NonNull GlRenderSink.Renderer renderer, @NonNull Config config) {
        mStreamRenderer = renderer;
        mConfig = config;
    }

    @Override
    void resize(int width, int height) {
        if (width > 0 && height > 0) {
            if (mGlRenderer == null) {
                mGlRenderer = new GlRenderer();
            }
            mGlRenderer.resize(width, height);
        } else if (mGlRenderer != null) {
            mGlRenderer.destroy();
            mGlRenderer = null;
        }
    }

    @Override
    public void render() {
        if (mGlRenderer != null) {
            mGlRenderer.render();
        }
    }

    @Override
    void onDispose() {
        if (mGlRenderer != null) {
            mGlRenderer.destroy();
            mGlRenderer = null;
        }
    }
}
