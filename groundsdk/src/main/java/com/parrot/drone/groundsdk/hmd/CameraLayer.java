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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.R;
import com.parrot.drone.groundsdk.internal.Logging;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.function.Consumer;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES30.*;

/**
 * See-through, device's rear-facing camera video layer.
 * <p>
 * This layers manages the device's rear-facing camera and allows to render its preview video stream to the
 * {@link Compositor} framebuffer.
 * <p>
 * We use deprecated android Camera (1) API for its simplicity of use. Once support gets effectively dropped, we will
 * move to Camera2 APIs.
 */
@SuppressWarnings("deprecation")
class CameraLayer extends Compositor.Layer {

    /** Android resources. */
    @NonNull
    private final Resources mResources;

    /** Device display orientation. */
    @NonNull
    private final Display mDisplay;

    /** Device rear-facing camera. */
    @Nullable
    private android.hardware.Camera mCamera;

    /** Current camera parameters. Non-{@code null} when {@link #mCamera} is non-{@code null}. */
    @Nullable
    private android.hardware.Camera.Parameters mCameraParams;

    /** Camera layer GL renderer. */
    @Nullable
    private GlRenderer mGlRenderer;

    /**
     * Camera layer GL renderer.
     */
    private static final class GlRenderer {

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
                GL_VERTEX_SHADER, R.raw.gsdk_internal_hmd_camera_vertex);

        /** Fragment shader for drawing an external OES texture onto a full viewport quad. */
        private static final Shader.Descriptor FRAGMENT_SHADER_DESC = Shader.descriptor(
                GL_FRAGMENT_SHADER, R.raw.gsdk_internal_hmd_camera_fragment);

        /** GL program handle. */
        private final int mGlProgram;

        /** GL Vertex Array Object. */
        private final int mGlVao;

        /** GL Vertex Buffer Object. */
        private final int mGlVbo;

        /** GL Element Buffer Object. */
        private final int mGlEbo;

        /** GL texture to render, linked to an android SurfaceTexture where the camera renders its preview stream. */
        private final int mGlTexture;

        /** Android Surface Texture where the camera stream is drawn. */
        @NonNull
        private final SurfaceTexture mSurfaceTexture;

        /** Transform matrix from the camera SurfaceTexture. */
        private final float[] mCameraTransform = new float[4 * 4];

        /** Horizontal scale factor to fit camera stream in available render width, keeping original aspect ratio. */
        private float mCameraScaleX;

        /** Vertical scale factor to fit camera stream in available render height, keeping original aspect ratio. */
        private float mCameraScaleY;

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
         * Sets scaling factors.
         *
         * @param scaleX horizontal scale factor
         * @param scaleY vertical scale factor
         */
        void scale(@FloatRange(from = 0) float scaleX, @FloatRange(from = 0) float scaleY) {
            mCameraScaleX = scaleX;
            mCameraScaleY = scaleY;
        }

        /**
         * Renders the layer.
         */
        void render() {
            glUseProgram(mGlProgram);

            glBindVertexArray(mGlVao);

            glActiveTexture(GL_TEXTURE0);
            mSurfaceTexture.updateTexImage(); // implicit texture bind
            mSurfaceTexture.getTransformMatrix(mCameraTransform);
            glUniformMatrix4fv(1, 1, false, mCameraTransform, 0);

            glUniform2f(2, mCameraScaleX, mCameraScaleY);

            glDrawElements(GL_TRIANGLE_FAN, ELEMENTS.limit(), GL_UNSIGNED_BYTE, 0);

            glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
            glBindVertexArray(0);

            glUseProgram(0);
        }

        /**
         * Destroys renderer, freeing all allocated GL resources.
         */
        void destroy() {
            mSurfaceTexture.release();

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
    }

    /**
     * Constructor.
     *
     * @param context            android context.
     * @param glThreadDispatcher allows to dispatch orientation changes to the GL thread
     */
    CameraLayer(@NonNull Context context, @NonNull Consumer<Runnable> glThreadDispatcher) {
        mResources = context.getResources();
        mDisplay = new Display(context, glThreadDispatcher);

        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        for (int camId = 0, N = android.hardware.Camera.getNumberOfCameras(); camId < N; camId++) {
            android.hardware.Camera.getCameraInfo(camId, info);
            if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCamera = android.hardware.Camera.open(camId);
                // prepare default parameters
                mCameraParams = mCamera.getParameters();
                mCameraParams.setZoom(0);
                mCameraParams.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

                // setup display orientation
                Display.OrientationListener orientationConfigurator = orientation ->
                        mCamera.setDisplayOrientation((info.orientation - orientation + 360) % 360);

                mDisplay.setOrientationListener(orientationConfigurator);
                orientationConfigurator.onOrientationChanged(mDisplay.getOrientation());
                break;
            }
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    final void resize(int width, int height) {
        if (width > 0 && height > 0 && mCamera != null) {
            assert mCameraParams != null;
            android.hardware.Camera.Size previewSize =
                    mCameraParams.getSupportedPreviewSizes()
                                 .stream()
                                 .min((lhs, rhs) -> Double.compare(
                                         quantifyRectSimilarity(rhs.width, rhs.height, width, height),
                                         quantifyRectSimilarity(lhs.width, lhs.height, width, height)))
                                 .orElse(null);

            if (previewSize == null) {
                return;
            }

            // update camera parameters
            android.hardware.Camera.Size currentSize = mCameraParams.getPreviewSize();

            if (!previewSize.equals(currentSize)) {
                mCameraParams.setPreviewSize(previewSize.width, previewSize.height);
                try {
                    mCamera.setParameters(mCameraParams);
                } catch (RuntimeException e) { // TODO : double check proper exception
                    // some devices don't support parameters changes while preview is running. Stop preview first.
                    // TODO: this a bit risky, though. What if setParams fails for another reason? Maybe not worth
                    //  the pain...
                    ULog.w(Logging.TAG_INTERNAL, "Unsupported live preview size modification. Stopping preview first",
                            e);
                    mCamera.stopPreview();
                    mCamera.setParameters(mCameraParams);
                    mCamera.startPreview();
                }
            }

            if (mGlRenderer == null) {
                mGlRenderer = new GlRenderer(mResources);
                try {
                    mCamera.setPreviewTexture(mGlRenderer.mSurfaceTexture);
                } catch (IOException e) {
                    throw new Error(e); // should never happen
                }
                mCamera.startPreview();
            }

            if (mDisplay.getOrientation() % Display.ORIENTATION_180 == 0) {
                // in portrait, width/height are actually reversed
                int tmp = previewSize.width;
                previewSize.width = previewSize.height;
                previewSize.height = tmp;
            }

            // compute scale, with respect to original aspect ratio, filling whole texture, possibly cropped
            float scale = previewSize.width * height / (float) (previewSize.height * width);
            float scaleX = 1, scaleY = 1;
            if (Float.compare(scale, 1) <= 0) {
                scaleX = scale;
            } else {
                scaleY = 1 / scale;
            }

            mGlRenderer.scale(scaleX, scaleY);

            onCameraGeometry(mCameraParams.getHorizontalViewAngle(), mCameraParams.getVerticalViewAngle(),
                    Math.round(width * scaleX), Math.round(height * scaleY));
        } else {
            if (mCamera != null) {
                mCamera.stopPreview();
            }
            if (mGlRenderer != null) {
                mGlRenderer.destroy();
                mGlRenderer = null;
            }
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
        mDisplay.setOrientationListener(null);

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            mCameraParams = null;
        }

        if (mGlRenderer != null) {
            mGlRenderer.destroy();
            mGlRenderer = null;
        }
    }

    /**
     * Notifies that geometry data from the see-through rear facing camera have changed.
     *
     * @param horizontalFov see-through horizontal field of view, in degrees
     * @param verticalFov   see-through vertical field of view, in degrees
     * @param imageWidth    width, in pixels, that the horizontal field of view physically covers
     * @param imageHeight   height, in pixels, that the vertical field of view physically covers
     */
    void onCameraGeometry(float horizontalFov, float verticalFov, int imageWidth, int imageHeight) {
    }

    /**
     * Computes a similarity index between two different rectangles.
     * <p>
     * This tries to take both aspect ratio and area similarity into account.
     *
     * @param sourceWidth  source rectangle width
     * @param sourceHeight source rectangle height
     * @param targetWidth  target rectangle width
     * @param targetHeight target rectangle width
     *
     * @return similarity index, in [0, 1] range; the higher the value, the more similar input rectangles are
     */
    private static double quantifyRectSimilarity(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        // compute similarity value [0, 1] between source aspect ratio and target aspect ratio
        double aspectRatioSimilarity = sourceWidth * targetHeight / (double) (sourceHeight * targetWidth);
        if (Double.compare(aspectRatioSimilarity, 1) > 0) {
            aspectRatioSimilarity = 1 / aspectRatioSimilarity;
        }

        // compute similarity value between source area and target area
        double areaSimilarity = sourceWidth * sourceHeight / (double) (targetWidth * targetHeight);
        if (Double.compare(areaSimilarity, 1) > 0) {
            areaSimilarity = 1 / areaSimilarity;
        }

        return Math.sqrt(Math.pow(aspectRatioSimilarity, 2) + Math.pow(areaSimilarity, 2));
    }
}
