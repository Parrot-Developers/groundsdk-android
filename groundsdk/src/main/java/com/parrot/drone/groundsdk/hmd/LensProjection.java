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

import com.parrot.drone.groundsdk.R;
import com.parrot.drone.groundsdk.internal.io.Files;

import java.nio.ByteBuffer;

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

    /** Mesh positions GL Vertex Buffer Object. */
    private final int mGlVboMeshPositions;

    /** Texture coordinates GL Vertex Buffer Object. */
    private final int mGlVboTexCoords;

    /** Color filter data GL Vertex Buffer Object. */
    private final int mGlVboColorFilter;

    /** Triangle draw indices GL Vertex Buffer Object. */
    private final int mGlVboIndices;

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

    /** Chromatic aberration correction factor for red color channel. */
    private float mChromaCorrectionRed;

    /** Chromatic aberration correction factor for green color channel. */
    private float mChromaCorrectionGreen;

    /** Chromatic aberration correction factor for blue color channel. */
    private float mChromaCorrectionBlue;

    /** Count of elements to draw. */
    private int mGlElementCount;

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

        glGenBuffers(4, names, 0);
        mGlVboMeshPositions = names[0];
        mGlVboTexCoords = names[1];
        mGlVboColorFilter = names[2];
        mGlVboIndices = names[3];

        mChromaCorrectionRed = mChromaCorrectionGreen = mChromaCorrectionBlue = 1.0f;
    }

    /**
     * Sets HMD model to use for projection.
     *
     * @param model HMD model
     */
    void setHmdModel(@NonNull HmdModel model) {
        glBindVertexArray(mGlVao);

        ByteBuffer buffer;

        glBindBuffer(GL_ARRAY_BUFFER, mGlVboMeshPositions);
        buffer = model.loadMeshPositions();
        if (buffer == null) {
            glBufferData(GL_ARRAY_BUFFER, 0, null, GL_STATIC_DRAW);
            glDisableVertexAttribArray(0);
        } else {
            glBufferData(GL_ARRAY_BUFFER, buffer.limit(), buffer, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0);
        }

        glBindBuffer(GL_ARRAY_BUFFER, mGlVboTexCoords);
        buffer = model.loadTexCoords();
        if (buffer == null) {
            glBufferData(GL_ARRAY_BUFFER, 0, null, GL_STATIC_DRAW);
            glDisableVertexAttribArray(1);
        } else {
            glBufferData(GL_ARRAY_BUFFER, buffer.limit(), buffer, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(1);
        }

        glBindBuffer(GL_ARRAY_BUFFER, mGlVboColorFilter);
        buffer = model.loadColorFilter();
        if (buffer == null) {
            glBufferData(GL_ARRAY_BUFFER, 0, null, GL_STATIC_DRAW);
            glDisableVertexAttribArray(2);
        } else {
            glBufferData(GL_ARRAY_BUFFER, buffer.limit(), buffer, GL_STATIC_DRAW);
            glVertexAttribPointer(2, 4, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(2);
        }

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mGlVboIndices);
        buffer = model.loadIndices();
        if (buffer == null) {
            mGlElementCount = 0;
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, 0, null, GL_STATIC_DRAW);
        } else {
            mGlElementCount = buffer.asIntBuffer().limit();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer.limit(), buffer, GL_STATIC_DRAW);
        }

        glBindVertexArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        mChromaCorrectionRed = model.chromaCorrection().red();
        mChromaCorrectionGreen = model.chromaCorrection().green();
        mChromaCorrectionBlue = model.chromaCorrection().blue();
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

        // configure default colorFilter (no filter)
        glVertexAttrib4f(2, 1, 1, 1, 1);

        // configure meshScale uniform
        glUniform2f(1, mMeshScaleX, mMeshScaleY);

        // configure texScale uniform
        glUniform2f(2, mTexScaleX, mTexScaleY);

        // configure chroma correction
        glUniform3f(3, mChromaCorrectionRed, mChromaCorrectionGreen, mChromaCorrectionBlue);

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
}
