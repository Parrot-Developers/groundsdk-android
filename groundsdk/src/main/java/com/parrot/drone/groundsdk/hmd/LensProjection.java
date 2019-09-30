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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.parrot.drone.groundsdk.R;
import com.parrot.drone.groundsdk.internal.io.Files;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.opengl.GLES30.*;

/**
 * Allows to render a given source texture into a render-buffer backed framebuffer, projecting the texture to compensate
 * for the HMD lenses distortion.
 */
final class LensProjection {

    /** Lens projection vertex shader. */
    private static final Shader.Descriptor VERTEX_SHADER_DESC = Shader.descriptor(
            GL_VERTEX_SHADER, R.raw.gsdk_internal_hmd_lens_vertex);

    /** Lens projection fragment shader. */
    private static final Shader.Descriptor FRAGMENT_SHADER_DESC = Shader.descriptor(
            GL_FRAGMENT_SHADER, R.raw.gsdk_internal_hmd_lens_fragment);

    /** GL program handle. */
    private final int mGlProgram;

    /** GL Frame Buffer Object. */
    private final int mGlFBo;

    /** GL Render Buffer Object. */
    private final int mGlRbo;

    /** GL Vertex Array Object. */
    private final int mGlVao;

    /** Count of elements to draw. */
    private final int mGlElementCount;

    /** Render width, in pixels. */
    @IntRange(from = 0)
    private int mWidth;

    /** Render height, in pixels. */
    @IntRange(from = 0)
    private int mHeight;

    /** Horizontal scale factor to render the lens mesh at constant width on every device. */
    private float mMeshScaleX;

    /** Vertical scale factor to render the lens mesh at constant height on every device. */
    private float mMeshScaleY;

    /** Horizontal scale factor to fit projected content to the available render width. */
    private float mTexScaleX;

    /** Vertical scale factor to fit projected content to the available render height. */
    private float mTexScaleY;

    /**
     * Constructor.
     *
     * @param resources android resources
     */
    LensProjection(@NonNull Resources resources) {
        mGlProgram = Shader.makeProgramFromResources(resources, VERTEX_SHADER_DESC, FRAGMENT_SHADER_DESC);

        int[] names = new int[4];

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

        glGenVertexArrays(1, names, 0);
        mGlVao = names[0];

        glBindVertexArray(mGlVao);

        ByteBuffer buffer;

        glGenBuffers(4, names, 0);

        buffer = loadFloatBuffer(resources, R.raw.gsdk_internal_hmd_lens_positions);

        glBindBuffer(GL_ARRAY_BUFFER, names[0]);
        glBufferData(GL_ARRAY_BUFFER, buffer.limit(), buffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        buffer = loadFloatBuffer(resources, R.raw.gsdk_internal_hmd_lens_texcoords);

        glBindBuffer(GL_ARRAY_BUFFER, names[1]);
        glBufferData(GL_ARRAY_BUFFER, buffer.limit(), buffer, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);

        buffer = loadFloatBuffer(resources, R.raw.gsdk_internal_hmd_lens_fade);

        glBindBuffer(GL_ARRAY_BUFFER, names[2]);
        glBufferData(GL_ARRAY_BUFFER, buffer.limit(), buffer, GL_STATIC_DRAW);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(2);

        buffer = loadIntBuffer(resources, R.raw.gsdk_internal_hmd_lens_indices);
        mGlElementCount = buffer.asIntBuffer().limit();

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, names[3]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer.limit(), buffer, GL_STATIC_DRAW);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    /**
     * Configures lens projection according to the given rendering geometry data.
     *
     * @param geometry rendering geometry data
     */
    void resize(@NonNull Geometry geometry) {
        if (mWidth == geometry.lensRenderWidthPx && mHeight == geometry.lensRenderHeightPx) {
            return;
        }

        mWidth = geometry.lensRenderWidthPx;
        mHeight = geometry.lensRenderHeightPx;

        // compute mesh scale so that mesh always has the same physical size on any device
        mMeshScaleX = 2 / geometry.lensRenderWidthMm;
        mMeshScaleY = 2 / geometry.lensRenderHeightMm;

        // compute tex scale so that rendered texture fits precisely inside render zone
        mTexScaleX = (float) geometry.lensMeshWidthPx / mWidth;
        mTexScaleY = (float) geometry.lensMeshHeightPx / mHeight;

        // correct projection bias on texture scale factors
        // TODO: use proper formula, derived from lens K factors.
        //       For now, use an empirical formula with acceptable results.
        mTexScaleX += 0.5f - 0.5f / mTexScaleX;
        mTexScaleY += 0.5f - 0.5f / mTexScaleY;

        glBindRenderbuffer(GL_RENDERBUFFER, mGlRbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_RGB8, mWidth, mHeight);

        glBindFramebuffer(GL_FRAMEBUFFER, mGlFBo);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, mGlRbo);

        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Projects the given source texture to the internal framebuffer.
     *
     * @param glSrcTexture source texture to project
     */
    void draw(int glSrcTexture) {
        if (mWidth <= 0 || mHeight <= 0) {
            return;
        }

        glUseProgram(mGlProgram);

        glBindFramebuffer(GL_FRAMEBUFFER, mGlFBo);

        glBindVertexArray(mGlVao);

        // configure meshScale uniform
        glUniform2f(1, mMeshScaleX, mMeshScaleY);

        // configure texScale uniform
        glUniform2f(2, mTexScaleX, mTexScaleY);

        // configure texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, glSrcTexture);

        glViewport(0, 0, mWidth, mHeight);

        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT);

        glDrawElements(GL_TRIANGLES, mGlElementCount, GL_UNSIGNED_INT, 0);

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glUseProgram(0);
    }

    /**
     * Retrieves the handle of the GL framebuffer where the projection is drawn.
     *
     * @return projection framebuffer handle
     */
    int getFrameBuffer() {
        return mGlFBo;
    }

    /**
     * Loads an integer buffer from a binary raw resource.
     * <p>
     * Stored data is expected to have big endian ordering.
     *
     * @param resources   android resources
     * @param bufferResId raw resource id of the binary buffer to be loaded
     *
     * @return a byte buffer containing loaded data, with native endianness
     */
    @NonNull
    private static ByteBuffer loadIntBuffer(@NonNull Resources resources, @RawRes int bufferResId) {
        ByteBuffer buffer = loadRawBuffer(resources, bufferResId);
        buffer.order(ByteOrder.nativeOrder()).asIntBuffer().put(buffer.duplicate().asIntBuffer()); // fix endianness
        return buffer;
    }

    /**
     * Loads a float buffer from a binary raw resource.
     * <p>
     * Stored data is expected to have big endian ordering.
     *
     * @param resources   android resources
     * @param bufferResId raw resource id of the binary buffer to be loaded
     *
     * @return a byte buffer containing loaded data, with native endianness
     */
    @NonNull
    private static ByteBuffer loadFloatBuffer(@NonNull Resources resources, @RawRes int bufferResId) {
        ByteBuffer buffer = loadRawBuffer(resources, bufferResId);
        buffer.order(ByteOrder.nativeOrder()).asFloatBuffer().put(buffer.duplicate().asFloatBuffer()); // fix endianness
        return buffer;
    }

    /**
     * Loads a buffer from a binary raw resource.
     * <p>
     * Stored data is expected to have big endian ordering.
     *
     * @param resources   android resources
     * @param bufferResId raw resource id of the binary buffer to be loaded
     *
     * @return a byte buffer containing loaded data
     */
    @NonNull
    private static ByteBuffer loadRawBuffer(@NonNull Resources resources, @RawRes int bufferResId) {
        try {
            return Files.readRawResource(resources, bufferResId).order(ByteOrder.BIG_ENDIAN);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
