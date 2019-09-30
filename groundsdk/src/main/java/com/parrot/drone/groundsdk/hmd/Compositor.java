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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static android.opengl.GLES30.*;

/**
 * Allows to compose multiple layers into a texture-backed GL framebuffer.
 */
final class Compositor {

    /** Handle for invalid GL texture. */
    static final int INVALID_TEXTURE = 0;

    /** Handle for invalid GL framebuffer. */
    static final int INVALID_FRAMEBUFFER = 0;

    /** A layer to compose. */
    abstract static class Layer {

        /** Compositor that will compose this layer. */
        @Nullable
        private Compositor mCompositor;

        /**
         * Configures layer dimensions.
         * <p>
         * Subclasses must take appropriate measures to prepare rendering with the given size constraints.
         *
         * @param width  layer width, in pixels
         * @param height layer height, in pixels
         */
        abstract void resize(@IntRange(from = 0) int width, @IntRange(from = 0) int height);

        /**
         * Renders the layer.
         */
        abstract void render();

        /**
         * Disposes the layer.
         */
        final void dispose() {
            if (mCompositor != null) {
                mCompositor.mLayers.remove(this);
                mCompositor = null;
                onDispose();
            }
        }

        /**
         * Gives access to the compositor GL frame buffer.
         *
         * @return compositor frame buffer handle
         */
        final int compositorFbo() {
            if (mCompositor == null || mCompositor.mGlRenderer == null) {
                throw new IllegalStateException();
            }
            return mCompositor.mGlRenderer.mGlFBo;
        }

        /**
         * Called when the layer is disposed.
         * <p>
         * Subclasses must take appropriate measures to deallocate all resources allocated for rendering purposes.
         * <p>
         * Default implementation does nothing.
         */
        void onDispose() {
        }
    }

    /** Layers to be composed. Layers are composed in order, starting from the first up to the last in that list. */
    private final List<Layer> mLayers;

    /** Render width, in pixels. */
    @IntRange(from = 0)
    private int mWidth;

    /** Render height, in pixels. */
    @IntRange(from = 0)
    private int mHeight;

    /** {@code true} when a GL context is currently available and GL operations can be requested. */
    private boolean mGlContext;

    /** Compositor GL renderer. */
    @Nullable
    private GlRenderer mGlRenderer;

    /**
     * Constructor.
     */
    Compositor() {
        mLayers = new ArrayList<>();
    }

    /**
     * Adds a layer to be composed.
     *
     * @param layer layer to add
     * @param below layer below which the added layer should be rendered; {@code null} to insert the layer last in list
     */
    void addLayer(@NonNull Layer layer, @Nullable Layer below) {
        if (mLayers.contains(layer)) {
            throw new IllegalStateException("Layer already added: " + layer);
        }

        int insertionIndex = below == null ? -1 : mLayers.indexOf(below);
        if (insertionIndex == -1) {
            mLayers.add(layer);
        } else {
            mLayers.add(insertionIndex, layer);
        }

        layer.mCompositor = this;
        if (mGlContext) {
            layer.resize(mWidth, mHeight);
        }
    }

    /**
     * Notifies that a GL context currently exists, and that GL operations may be requested.
     */
    void onGlContextCreate() {
        mGlContext = true;
        resize(mWidth, mHeight);
    }

    /**
     * Notifies that the current GL context has been destroyed, that GL resources shall be de-allocated, and that no
     * further GL operations may be requested at that point.
     */
    void onGlContextDestroy() {
        while (!mLayers.isEmpty()) {
            mLayers.get(0).dispose();
        }

        if (mGlRenderer != null) {
            mGlRenderer.destroy();
            mGlRenderer = null;
        }

        mGlContext = false;
    }

    /**
     * Sets the compositor render dimensions.
     *
     * @param width  render width, in pixels
     * @param height render height, in pixels
     */
    void resize(@IntRange(from = 0) int width, @IntRange(from = 0) int height) {
        if (mWidth == width && mHeight == height) {
            return;
        }

        mWidth = width;
        mHeight = height;

        if (!mGlContext) {
            return;
        }

        if (mWidth > 0 && mHeight > 0) {
            if (mGlRenderer == null) {
                mGlRenderer = new GlRenderer();
            }
            mGlRenderer.resize(mWidth, mHeight);
        } else if (mGlRenderer != null) {
            mGlRenderer.destroy();
            mGlRenderer = null;
        }

        mLayers.forEach((layer -> layer.resize(mWidth, mHeight)));
    }

    /**
     * Compose all layers and renders them to the compositor framebuffer.
     */
    void render() {
        if (mGlRenderer == null || mWidth <= 0 || mHeight <= 0) {
            return;
        }

        glBindFramebuffer(GL_FRAMEBUFFER, mGlRenderer.mGlFBo);

        glViewport(0, 0, mWidth, mHeight);

        // clear color buffer
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT);

        mLayers.forEach(layer -> {
            glViewport(0, 0, mWidth, mHeight);
            layer.render();
        });

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Retrieves the GL handle of the texture that backs the compositor framebuffer.
     * <p>
     * Such texture handle can then be used further down the chain to apply transforms to the compositor output.
     *
     * @return compositor GL texture handler.
     */
    int getOutputTexture() {
        return mGlRenderer == null ? INVALID_TEXTURE : mGlRenderer.mGlTexture;
    }

    /**
     * Retrieves the GL handle of the compositor framebuffer.
     *
     * @return compositor GL framebuffer handle
     */
    int getFrameBuffer() {
        return mGlRenderer == null ? INVALID_FRAMEBUFFER : mGlRenderer.mGlFBo;
    }

    /**
     * Compositor GL renderer.
     */
    private static final class GlRenderer {

        /** GL Frame Buffer Object. */
        private final int mGlFBo;

        /** GL texture that backs the compositor framebuffer. */
        private final int mGlTexture;

        /**
         * Constructor.
         */
        GlRenderer() {
            int[] names = new int[1];

            glGenTextures(1, names, 0);
            mGlTexture = names[0];

            glGenFramebuffers(1, names, 0);
            mGlFBo = names[0];

            glBindTexture(GL_TEXTURE_2D, mGlTexture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        /**
         * Sets rendering size.
         *
         * @param width  rendering width, in pixels
         * @param height rendering height in pixels
         */
        void resize(@IntRange(from = 0) int width, @IntRange(from = 0) int height) {
            glBindTexture(GL_TEXTURE_2D, mGlTexture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, null);

            glBindFramebuffer(GL_FRAMEBUFFER, mGlFBo);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mGlTexture, 0);

            glBindTexture(GL_TEXTURE_2D, 0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }

        /**
         * Destroys renderer, freeing all allocated GL resources.
         */
        void destroy() {
            int[] names = new int[1];

            names[0] = mGlTexture;
            glDeleteTextures(1, names, 0);

            names[0] = mGlFBo;
            glDeleteFramebuffers(1, names, 0);
        }
    }
}
