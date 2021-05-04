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
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.view.Surface;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.parrot.drone.groundsdk.internal.stream.GlRenderSink;
import com.parrot.drone.groundsdk.internal.view.GlView;
import com.parrot.drone.groundsdk.stream.Overlayer2;

import java.util.function.Consumer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES30.*;

/**
 * Custom {@link GlView.Renderer} which handles rendering of both lenses of the HMD view.
 */
abstract class Renderer extends GlView.Renderer {

    /** {@code true} to enable debug mode where lens projection is disabled (renders to flat squares). */
    private static final boolean DEBUG_FLAT_RENDERING = false;

    /** Android context. */
    @NonNull
    private final Context mContext;

    /** Renders multiple layers into a single texture-backed framebuffer. Used on GL thread. */
    @NonNull
    private final Compositor mCompositor;

    /**
     * Computes geometry changes following surface dimension changes and provides geometry data for rendering.
     * Modified on main thread. Geometry results are computed on GL thread.
     */
    @NonNull
    private final Geometry.Computer mGeometryComputer;

    /** Collects drone video stream specific configuration. Used on GL thread. */
    @NonNull
    private final StreamLayer.Config mStreamConfig;

    /** Current rendering geometry data. Used on GL thread. */
    @NonNull
    private Geometry mGeometry;

    /**
     * {@code true} when geometry inputs have been modified and geometry data needs to be recomputed.
     * Armed on main thread when geometry inputs (lens offsets) change by user request, read and reset on GL thread
     * when geometry is to be computed.
     */
    private volatile boolean mComputeGeometry;

    /**
     * Projects composed texture, as required to compensate VR glasses lens distortion, to a render-buffer-backed
     * framebuffer. Used on GL thread.
     */
    @Nullable
    private LensProjection mLensProjection;

    /** UI Overlay layer. Used on GL thread. */
    @Nullable
    private Overlay mOverlay;

    /** Drone video stream layer. Used on GL thread. */
    @Nullable
    private StreamLayer mStreamLayer;

    /** See-through, device rear camera layer. Used on GL thread. */
    @Nullable
    private CameraLayer mCameraLayer;

    /**
     * Surface linked to the overlay layer. Client overlay layout is drawn using a canvas obtained from this surface.
     * Created and used on main thread.
     */
    @Nullable
    private Surface mOverlaySurface;

    /** HMD model used for rendering. */
    @Nullable
    private HmdModel mHmdModel;

    /**
     * Constructor.
     *
     * @param view GL view to render to
     */
    Renderer(@NonNull GlView view) {
        super(GLSurfaceView.RENDERMODE_CONTINUOUSLY, GLES_V3);
        mContext = view.getContext();
        mCompositor = new Compositor();
        mGeometryComputer = new Geometry.Computer(mContext.getResources().getDisplayMetrics());
        mGeometryComputer.setVerticalLensesOffset(0);
        mGeometry = mGeometryComputer.compute();
        mStreamConfig = new StreamLayer.Config();
        view.launchRendering(this);
    }

    /**
     * Configures HMD model to be used for rendering.
     *
     * @param dataPackRes resource containing HMD model definitions
     * @param modelName   identifies the model to use from the provided data pack
     */
    final void setHmdModel(@RawRes int dataPackRes, @NonNull String modelName) {
        runOnGlThread(() -> {
            mHmdModel = HmdModel.DataPack.fromResources(mContext.getResources(), dataPackRes).loadModel(modelName);

            mGeometryComputer.setHmdModel(mHmdModel);
            mComputeGeometry = true;
            computeGeometry();

            if (mLensProjection != null) {
                mLensProjection.setHmdModel(mHmdModel);
            }
        });
    }

    /**
     * Configures left and right lenses vertical offset.
     * <p>
     * Defines how to vertically offset both lenses projection centers from the center of GsdkHmdView. Value in
     * millimeters.
     *
     * @param offset vertical lenses offset
     */
    final void setLensesVerticalOffset(float offset) {
        mGeometryComputer.setVerticalLensesOffset(offset);
        mComputeGeometry = true;
        runOnGlThread(this::computeGeometry);
    }

    /**
     * Configures drone stream overexposure zebras rendering.
     *
     * @param enable {@code true} to enable zebras rendering, {@code false} to disable it
     */
    final void enableStreamZebras(boolean enable) {
        runOnGlThread(() -> mStreamConfig.enableZebras(enable));
    }

    /**
     * Configures drone stream overexposure zebra threshold.
     *
     * @param threshold desired zebra threshold
     */
    final void setStreamZebraThreshold(@FloatRange(from = 0, to = 1) double threshold) {
        runOnGlThread(() -> mStreamConfig.setZebraThreshold(threshold));
    }

    /**
     * Configures drone stream color histogram computation.
     *
     * @param enable {@code true} to enable histogram computation, {@code false} to disable it
     */
    final void enableStreamHistogram(boolean enable) {
        runOnGlThread(() -> mStreamConfig.enableHistogram(enable));
    }

    /**
     * Configures drone stream rendering overlayer.
     *
     * @param overlayer overlayer to configure, {@code null} to disable rendering overlay
     */
    final void setStreamOverlayer(@Nullable Overlayer2 overlayer) {
        runOnGlThread(() -> mStreamConfig.setOverlayer(overlayer));
    }

    /**
     * Attaches a drone video stream GL renderer.
     *
     * @param renderer stream GL renderer, {@code null} to disable drone video stream layer
     */
    final void setStreamRenderer(@Nullable GlRenderSink.Renderer renderer) {
        runOnGlThread(() -> {
            if (mStreamLayer != null) {
                mStreamLayer.dispose();
                mStreamLayer = null;
            }
            if (renderer != null) {
                mStreamLayer = new StreamLayer(renderer, mStreamConfig);
                mCompositor.addLayer(mStreamLayer, mOverlay);
            }
        });
    }

    /**
     * Enables device rear-facing camera see-through.
     *
     * @param enable {@code true} to enable device camera see-through, {@code false} to disable it
     */
    final void enableCamera(boolean enable) {
        runOnGlThread(() -> {
            if ((mCameraLayer != null) == enable) {
                return;
            }

            if (mCameraLayer == null) {
                mCameraLayer = new CameraLayer(mContext, this::runOnGlThread) {

                    @Override
                    void onCameraGeometry(float horizontalFov, float verticalFov, int imageWidth, int imageHeight) {
                        runOnMainThread(() -> onCameraGeometryChanged(
                                horizontalFov, verticalFov, imageWidth, imageHeight));
                    }
                };
                mCompositor.addLayer(mCameraLayer, mOverlay);
            } else {
                mCameraLayer.dispose();
                mCameraLayer = null;
            }
        });
    }

    /**
     * Gives access to the overlay layer canvas, for drawing client UI overlay.
     * <p>
     * Overlay surface is locked to obtain a canvas that is passed to {@code canvasClient} function, for drawing,
     * then unlocked and changes are posted for update on the GL thread on the function returns. Client function
     * <strong>MUST NOT</strong> keep any reference to the provided canvas, as it will become invalid after the
     * method returns.
     *
     * @param canvasClient client function that will draw to the canvas
     */
    final void withOverlayCanvas(@NonNull Consumer<Canvas> canvasClient) {
        if (mOverlaySurface == null) {
            return;
        }

        Canvas canvas;
        try {
            canvas = mOverlaySurface.lockCanvas(null);
        } catch (RuntimeException e) {
            // race: surface texture release on GL thread. Release and drop surface
            mOverlaySurface.release();
            mOverlaySurface = null;
            return;
        }

        try {
            canvasClient.accept(canvas);
        } finally {
            try {
                mOverlaySurface.unlockCanvasAndPost(canvas);
            } catch (RuntimeException e) {
                // race: surface texture release on GL thread. Release and drop surface.
                mOverlaySurface.release();
                mOverlaySurface = null;
            }
        }
    }

    /**
     * Notifies that the dimensions available for the overlay content have changed.
     *
     * @param width  available overlay width, in pixels
     * @param height available overlay height, in pixels
     */
    abstract void onOverlaySizeChanged(int width, int height);

    /**
     * Notifies that geometry data from the see-through rear facing camera have changed.
     *
     * @param horizontalFov see-through horizontal field of view, in degrees
     * @param verticalFov   see-through vertical field of view, in degrees
     * @param width         width, in pixels, that the horizontal field of view physically covers
     * @param height        height, in pixels, that the vertical field of view physically covers
     */
    abstract void onCameraGeometryChanged(float horizontalFov, float verticalFov, int width, int height);

    @Override
    public final void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Resources resources = mContext.getResources();

        mOverlay = new Overlay(resources) {

            @Override
            void onSurfaceTexture(@Nullable SurfaceTexture overlayTexture, @IntRange(from = 0) int width,
                                  @IntRange(from = 0) int height) {
                runOnMainThread(() -> {
                    if (overlayTexture == null && mOverlaySurface != null) {
                        mOverlaySurface.release();
                        mOverlaySurface = null;
                    } else if (overlayTexture != null && mOverlaySurface == null) {
                        try {
                            mOverlaySurface = new Surface(overlayTexture);
                        } catch (Surface.OutOfResourcesException ignored) {
                            // race: overlayTexture has already been disposed on GL thread, ignore
                        }
                    }
                    onOverlaySizeChanged(width, height);
                });
            }
        };
        mCompositor.addLayer(mOverlay, null);

        mCompositor.onGlContextCreate();

        mLensProjection = new LensProjection(resources);
        if (mHmdModel != null) {
            mLensProjection.setHmdModel(mHmdModel);
        }
    }

    @Override
    public final void onSurfaceChanged(GL10 gl, int width, int height) {
        mGeometryComputer.setSurfaceDimensions(width, height);
        mComputeGeometry = true;
        computeGeometry();
    }

    @Override
    public final void onDrawFrame(GL10 gl) {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);

        // prepare screen
        glViewport(0, 0, mGeometry.surfaceWidthPx, mGeometry.surfaceHeightPx);
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT);

        if (mGeometry.lensRenderWidthPx <= 0 || mGeometry.lensRenderHeightPx <= 0) {
            return;
        }

        mCompositor.render();

        int glFbo;
        if (DEBUG_FLAT_RENDERING) {
            glFbo = mCompositor.getFrameBuffer();
            if (glFbo == Compositor.INVALID_FRAMEBUFFER) {
                return;
            }
        } else {
            assert mLensProjection != null;
            int texture = mCompositor.getOutputTexture();
            if (texture == Compositor.INVALID_TEXTURE) {
                return;
            }
            mLensProjection.draw(texture);
            glFbo = mLensProjection.getFrameBuffer();
        }

        glBindFramebuffer(GL_READ_FRAMEBUFFER, glFbo);

        // blit left lens
        glBlitFramebuffer(0, 0, mGeometry.lensRenderWidthPx, mGeometry.lensRenderHeightPx,
                mGeometry.leftLensRenderLeft, mGeometry.lensRenderTop, mGeometry.leftLensRenderRight,
                mGeometry.lensRenderBottom, GL_COLOR_BUFFER_BIT, GL_LINEAR);

        // blit right lens
        glBlitFramebuffer(0, 0, mGeometry.lensRenderWidthPx, mGeometry.lensRenderHeightPx,
                mGeometry.rightLensRenderLeft, mGeometry.lensRenderTop, mGeometry.rightLensRenderRight,
                mGeometry.lensRenderBottom, GL_COLOR_BUFFER_BIT, GL_LINEAR);

        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
    }

    @Override
    protected final void onSurfaceDestroyed() {
        mCompositor.onGlContextDestroy();
        mOverlay = null;
        mCameraLayer = null;
        mStreamLayer = null;
        mLensProjection = null;
    }

    /**
     * Recomputes rendering geometry if appropriate.
     * <p>
     * Must be called on GL thread.
     */
    private void computeGeometry() {
        if (!mComputeGeometry) {
            return;
        }
        mComputeGeometry = false;

        mGeometry = mGeometryComputer.compute();

        mCompositor.resize(mGeometry.lensRenderWidthPx, mGeometry.lensRenderHeightPx);

        if (mLensProjection != null) {
            mLensProjection.resize(mGeometry);
        }
    }
}
