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

import android.content.res.Resources;
import android.graphics.SurfaceTexture;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.R;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES30.*;

/**
 * UI overlay layer.
 * <p>
 * This layers manages the UI overlay and allows to render the latter to the {@link Compositor} framebuffer.
 */
abstract class Overlay extends Compositor.Layer {

    /** Android resources. */
    @NonNull
    private final Resources mResources;

    /** Overlay GL renderer. */
    @Nullable
    private GlRenderer mGlRenderer;

    /**
     * Constructor.
     *
     * @param resources android resources
     */
    Overlay(@NonNull Resources resources) {
        mResources = resources;
    }

    /**
     * Overlay GL renderer.
     */
    private abstract static class GlRenderer {

        /** Buffer containing vertex and texture coordinates for a full viewport quad. */
        private static final FloatBuffer COORDS = FloatBuffer.wrap(new float[] {
                // vertex X, Y | texture X, Y
                -1, 1, 0, 0, // 0: upper left
                -1, -1, 0, 1, // 1: bottom left
                1, -1, 1, 1, // 2: bottom right
                1, 1, 1, 0  // 3: upper right
        });

        /** Buffer giving vertex indices for drawing the quad in TRIANGLE_FAN mode. */
        private static final ByteBuffer ELEMENTS = ByteBuffer.wrap(new byte[] {
                0, 1, 2, 3
        });

        /** Vertex shader for drawing an external OES texture onto a full viewport quad. */
        private static final Shader.Descriptor VERTEX_SHADER_DESC = Shader.descriptor(
                GL_VERTEX_SHADER, R.raw.gsdk_internal_hmd_overlay_vertex);

        /** Fragment shader for drawing an external OES texture onto a full viewport quad. */
        private static final Shader.Descriptor FRAGMENT_SHADER_DESC = Shader.descriptor(
                GL_FRAGMENT_SHADER, R.raw.gsdk_internal_hmd_overlay_fragment);


        /** GL program handle. */
        private final int mGlProgram;

        /** GL Vertex Array Object. */
        private final int mGlVao;

        /** GL Vertex Buffer Object. */
        private final int mGlVbo;

        /** GL Element Buffer Object. */
        private final int mGlEbo;

        /** GL texture to render, linked to an android SurfaceTexture. */
        private final int mGlTexture;

        /** Android Surface Texture where the UI content is drawn. */
        @NonNull
        private final SurfaceTexture mSurfaceTexture;

        /**
         * Constructor.
         *
         * @param resources android resources
         */
        GlRenderer(@NonNull Resources resources) {
            mGlProgram = Shader.makeProgramFromResources(resources, VERTEX_SHADER_DESC, FRAGMENT_SHADER_DESC);

            int[] names = new int[2];
            glGenVertexArrays(1, names, 0);
            mGlVao = names[0];

            glBindVertexArray(mGlVao);

            glGenBuffers(2, names, 0);

            mGlVbo = names[0];
            glBindBuffer(GL_ARRAY_BUFFER, mGlVbo);
            glBufferData(GL_ARRAY_BUFFER, COORDS.limit() * Float.BYTES, COORDS, GL_STATIC_DRAW);

            glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);

            glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
            glEnableVertexAttribArray(1);

            mGlEbo = names[1];
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mGlEbo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, ELEMENTS.limit(), ELEMENTS, GL_STATIC_DRAW);

            glBindVertexArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

            glGenTextures(1, names, 0);

            mGlTexture = names[0];
            mSurfaceTexture = new SurfaceTexture(mGlTexture);

        }

        /**
         * Sets rendering size.
         *
         * @param width  rendering width, in pixels
         * @param height rendering height in pixels
         */
        void resize(@IntRange(from = 0) int width, @IntRange(from = 0) int height) {
            mSurfaceTexture.setDefaultBufferSize(width, height);
            onSurfaceTexture(mSurfaceTexture, width, height);
        }

        /**
         * Renders the layer.
         */
        void render() {
            glUseProgram(mGlProgram);

            glBindVertexArray(mGlVao);

            glEnable(GL_BLEND);
            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

            glActiveTexture(GL_TEXTURE0);
            mSurfaceTexture.updateTexImage(); // implicit texture bind

            glDrawElements(GL_TRIANGLE_FAN, ELEMENTS.limit(), GL_UNSIGNED_BYTE, 0);

            glDisable(GL_BLEND);

            glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
            glBindVertexArray(0);

            glUseProgram(0);
        }

        /**
         * Destroys renderer, freeing all allocated GL resources.
         */
        void destroy() {
            mSurfaceTexture.release();
            onSurfaceTexture(null, 0, 0);

            int[] names = new int[2];

            names[0] = mGlTexture;
            glDeleteTextures(1, names, 0);

            names[0] = mGlVbo;
            names[1] = mGlEbo;
            glDeleteBuffers(2, names, 0);

            names[0] = mGlVao;
            glDeleteVertexArrays(1, names, 0);

            glDeleteProgram(mGlProgram);
        }

        /**
         * Notifies surface texture availability and/or size change.
         *
         * @param surfaceTexture surface texture to be used to draw the UI overlay, or {@code null} if overlay
         *                       rendering is unavailable
         * @param width          available rendering width for overlay, in pixels
         * @param height         available rendering height for overlay, in pixels
         */
        abstract void onSurfaceTexture(@Nullable SurfaceTexture surfaceTexture, @IntRange(from = 0) int width,
                                       @IntRange(from = 0) int height);
    }

    @Override
    final void resize(int width, int height) {
        if (width > 0 && height > 0) {
            if (mGlRenderer == null) {
                mGlRenderer = new GlRenderer(mResources) {

                    @Override
                    void onSurfaceTexture(@Nullable SurfaceTexture surfaceTexture, @IntRange(from = 0) int width,
                                          @IntRange(from = 0) int height) {
                        Overlay.this.onSurfaceTexture(surfaceTexture, width, height);
                    }
                };
            }
            mGlRenderer.resize(width, height);
        } else if (mGlRenderer != null) {
            mGlRenderer.destroy();
            mGlRenderer = null;
        }
    }

    @Override
    final void render() {
        if (mGlRenderer != null) {
            mGlRenderer.render();
        }
    }

    @Override
    final void onDispose() {
        if (mGlRenderer != null) {
            mGlRenderer.destroy();
            mGlRenderer = null;
        }
    }

    /**
     * Notifies surface texture availability and/or size change.
     *
     * @param surfaceTexture surface texture to be used to draw the UI overlay, or {@code null} if overlay
     *                       rendering is unavailable
     * @param width          available rendering width for overlay, in pixels
     * @param height         available rendering height for overlay, in pixels
     */
    abstract void onSurfaceTexture(@Nullable SurfaceTexture surfaceTexture, @IntRange(from = 0) int width,
                                   @IntRange(from = 0) int height);
}
