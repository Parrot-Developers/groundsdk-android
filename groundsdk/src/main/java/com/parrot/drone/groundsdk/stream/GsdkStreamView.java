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

package com.parrot.drone.groundsdk.stream;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.PixelCopy;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.R;
import com.parrot.drone.groundsdk.internal.stream.GlRenderSink;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Custom android view capable of rendering a {@link Stream}.
 */
public class GsdkStreamView extends FrameLayout {

    /**
     * Scales the stream so that its largest dimension spans the whole view; its smallest dimension is scaled
     * accordingly to respect original aspect ratio, centered in the render zone; introduced padding, if any, is
     * rendered according to {@link #setPaddingFill(int) padding fill configuration}.
     * <p>
     * Also corresponds to XML attribute {@code fit} in {@code gsdk_scaleType}.
     *
     * @see #setScaleType(int)
     */
    public static final int SCALE_TYPE_FIT = 0;

    /**
     * Scales the stream so that its smallest dimension spans the whole view; its largest dimension is scaled
     * accordingly to respect original aspect ratio, and cropped to the render zone; no padding is introduced.
     * <p>
     * Also corresponds to XML attribute {@code crop} in {@code gsdk_scaleType}.
     *
     * @see #setScaleType(int)
     */
    public static final int SCALE_TYPE_CROP = 1;

    /** Int definition for scale types. */
    @IntDef({SCALE_TYPE_FIT, SCALE_TYPE_CROP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScaleType {}

    /**
     * Padding introduced by {@link #SCALE_TYPE_FIT fit scale type} is filled with default reset color.
     * <p>
     * Also corresponds to XML attribute {@code none} in {@code gsdk_paddingFill}.
     *
     * @see #setPaddingFill(int)
     */
    public static final int PADDING_FILL_NONE = 0;

    /**
     * Padding introduced by {@link #SCALE_TYPE_FIT fit scale type} is filled by first rendering the current stream
     * frame using {@link #SCALE_TYPE_CROP crop scale type}, blurred, then overlaying the scaled frame on top of it.
     * <p>
     * Also corresponds to XML attribute {@code blur_crop} in {@code gsdk_paddingFill}.
     *
     * @see #setPaddingFill(int)
     */
    public static final int PADDING_FILL_BLUR_CROP = 1;

    /**
     * Padding introduced by {@link #SCALE_TYPE_FIT fit scale type} is filled by repeating current frame borders,
     * blurred.
     * <p>
     * Also corresponds to XML attribute {@code blur_extend} in {@code gsdk_paddingFill}.
     *
     * @see #setPaddingFill(int)
     */
    public static final int PADDING_FILL_BLUR_EXTEND = 2;

    /** Int definition for padding fills. */
    @IntDef({PADDING_FILL_NONE, PADDING_FILL_BLUR_CROP, PADDING_FILL_BLUR_EXTEND})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PaddingFill {}

    /** Android GL surface view, hosting rendering. */
    @NonNull
    private final GLSurfaceView mView;

    /** GL surface view renderer , used entirely on rendering thread. */
    @NonNull
    private final Renderer mViewRenderer;

    /** Stream content zone, after scaling and excluding padding. */
    @NonNull
    private final Rect mContentZone;

    /** Configured scale type. Modified from main thread, volatile read from rendering thread. */
    @ScaleType
    private volatile int mScaleType;

    /** Configured padding fill. Modified from main thread, volatile read from rendering thread. */
    @PaddingFill
    private volatile int mPaddingFill;

    /** Configured zebras rendering. Modified from main thread, volatile read from rendering thread. */
    private volatile boolean mZebrasEnabled;

    /** Configured zebra threshold. Modified from main thread, volatile read from rendering thread. */
    @FloatRange(from = 0, to = 1)
    private volatile double mZebraThreshold;

    /** Configured histogram computation. Modified from main thread, volatile read from rendering thread. */
    private volatile boolean mHistogramEnabled;

    /** Configured as overlay of another SurfaceView. Modified from main thread. */
    private boolean mIsMediaOverlay;

    /** Configured rendering overlayer. Modified from main thread, volatile read from rendering thread. */
    @Nullable
    private volatile Overlayer2 mOverlayer;

    /** Configured rendering texture loader. Modified from main thread, volatile read from rendering thread. */
    @Nullable
    private volatile TextureLoader mTextureLoader;

    /** Stream to render, {@code null} when no stream is attached. */
    @Nullable
    private Stream mStream;

    /** GL Rendering sink obtained from {@link #mStream}, closed/reopen upon stream changes. */
    @Nullable
    private Stream.Sink mSink;

    /** Client listener notified of {@link #mContentZone} changes. */
    @Nullable
    private OnContentZoneChangeListener mContentZoneChangeListener;

    /**
     * Constructor.
     *
     * @param context android context
     */
    public GsdkStreamView(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Constructor.
     *
     * @param context android context
     * @param attrs   configured view attributes
     */
    public GsdkStreamView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor.
     *
     * @param context      android context
     * @param attrs        configured view attributes
     * @param defStyleAttr default style attribute, used to fallback on default attribute values
     */
    public GsdkStreamView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.GsdkStreamView, defStyleAttr, 0);

        try {
            mScaleType = a.getInteger(R.styleable.GsdkStreamView_gsdk_scaleType, SCALE_TYPE_FIT);
            mPaddingFill = a.getInteger(R.styleable.GsdkStreamView_gsdk_paddingFill, PADDING_FILL_NONE);
            mZebrasEnabled = a.getBoolean(R.styleable.GsdkStreamView_gsdk_zebrasEnabled, false);
            mZebraThreshold = a.getFraction(R.styleable.GsdkStreamView_gsdk_zebraThreshold, 1, 1, 0);
            mIsMediaOverlay = a.getBoolean(R.styleable.GsdkStreamView_gsdk_isMediaOverlay, false);
        } finally {
            a.recycle();
        }

        mContentZone = new Rect();

        mViewRenderer = new Renderer();

        mView = new GLSurfaceView(context) {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                queueEvent(mViewRenderer::onSurfaceDestroyed);
                super.surfaceDestroyed(holder);
            }
        };

        mView.setEGLContextClientVersion(2);
        mView.setRenderer(mViewRenderer);
        mView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mView.onPause();

        mView.setZOrderMediaOverlay(mIsMediaOverlay);
        addView(mView);
    }

    /**
     * Controls whether the GsdkStreamView's surface is placed on top of another regular surface view in the window
     * (but still behind the window itself).
     * This is typically used to place overlays on top of an underlying media surface view.
     *
     * Enabling ZOrderMediaOverlay may also be configured using XML view attribute {@code gsdk_isMediaOverlay}.
     *
     * @param isMediaOverlay {@code true} to place
     */
    public final void setZOrderMediaOverlay(boolean isMediaOverlay) {
        if (mIsMediaOverlay != isMediaOverlay) {
            mIsMediaOverlay = isMediaOverlay;
            removeView(mView);
            mView.setZOrderMediaOverlay(isMediaOverlay);
            addView(mView);
        }
    }

    /**
     * Tells whether GsdkStreamView's surface is configured to be placed on top of another regular surface view.
     *
     * @return {@code true} if configured to be placed on top of another regular surface view, otherwise {@code false}
     *
     * @see #setZOrderMediaOverlay(boolean)
     */
    public final boolean isZOrderMediaOverlay() {
        return mIsMediaOverlay;
    }

    /**
     * Configures stream scale type.
     * <p>
     * Scale type may also be configured using XML view attribute {@code gsdk_scaleType}.
     *
     * @param scaleType desired scale type
     *
     * @see #getScaleType()
     */
    public final void setScaleType(@ScaleType int scaleType) {
        if (mScaleType == scaleType) {
            return;
        }
        mScaleType = scaleType;
        mView.queueEvent(mViewRenderer::applyScaleType);
    }

    /**
     * Returns configured stream scale type.
     *
     * @return configured scale type
     *
     * @see #setScaleType(int)
     */
    @ScaleType
    public final int getScaleType() {
        return mScaleType;
    }

    /**
     * Configures stream padding fill.
     * <p>
     * Padding fill may also be configured using XML view attribute {@code gsdk_paddingFill}.
     *
     * @param paddingFill desired padding fill
     *
     * @see #getPaddingFill()
     */
    public final void setPaddingFill(@PaddingFill int paddingFill) {
        if (mPaddingFill == paddingFill) {
            return;
        }
        mPaddingFill = paddingFill;
        mView.queueEvent(mViewRenderer::applyPaddingFill);
    }

    /**
     * Returns configured stream padding fill.
     *
     * @return configured padding fill
     *
     * @see #setPaddingFill(int)
     */
    @PaddingFill
    public final int getPaddingFill() {
        return mPaddingFill;
    }

    /**
     * Configures overexposure zebras rendering.
     * <p>
     * Zebras rendering may also be configured using XML view attribute {@code gsdk_zebrasEnabled}.
     *
     * @param enable {@code true} to enable zebras rendering, {@code false} to disable it
     *
     * @see #areZebrasEnabled()
     */
    public final void enableZebras(boolean enable) {
        if (mZebrasEnabled == enable) {
            return;
        }
        mZebrasEnabled = enable;
        mView.queueEvent(mViewRenderer::applyZebrasEnable);
    }

    /**
     * Tells whether overexposure zebras rendering is enabled.
     *
     * @return {@code true} if zebras rendering is enabled, otherwise {@code false}
     *
     * @see #enableZebras(boolean)
     */
    public final boolean areZebrasEnabled() {
        return mZebrasEnabled;
    }

    /**
     * Configures overexposure zebra threshold.
     * <p>
     * Zebra threshold may also be configured using XML view attribute {@code gsdk_zebraThreshold}.
     *
     * @param threshold desired zebra threshold
     *
     * @see #areZebrasEnabled()
     */
    public final void setZebraThreshold(@FloatRange(from = 0, to = 1) double threshold) {
        if (Double.compare(mZebraThreshold, threshold) == 0) {
            return;
        }
        mZebraThreshold = threshold;
        mView.queueEvent(mViewRenderer::applyZebraThreshold);
    }

    /**
     * Returns configured overexposure zebra threshold.
     *
     * @return configured zebra threshold
     *
     * @see #setZebraThreshold(double)
     */
    @FloatRange(from = 0, to = 1)
    public final double getZebraThreshold() {
        return mZebraThreshold;
    }

    /**
     * Configure color histogram computation.
     *
     * @param enable {@code true} to enable histogram computation, {@code false} to disable it
     *
     * @see #isHistogramEnabled()
     */
    public final void enableHistogram(boolean enable) {
        if (mHistogramEnabled == enable) {
            return;
        }
        mHistogramEnabled = enable;
        mView.queueEvent(mViewRenderer::applyHistogramEnable);
    }

    /**
     * Tells whether color histogram computation is enabled.
     *
     * @return {@code true} if histogram computation is enabled, otherwise {@code false}
     */
    public final boolean isHistogramEnabled() {
        return mHistogramEnabled;
    }

    /**
     * Configures rendering overlayer.
     *
     * @param overlayer overlayer to configure, {@code null} to disable rendering overlay
     *
     * @deprecated use #setOverlayer2(Overlayer2) instead.
     */
    @Deprecated
    public final void setOverlayer(@Nullable Overlayer overlayer) {
        setOverlayer2(overlayer == null ? null : frameContext -> overlayer.overlay(
                frameContext.renderZone(), frameContext.contentZone(),
                (Overlayer.Histogram) frameContext.histogram()));
    }

    /**
     * Configures rendering overlayer.
     *
     * @param overlayer overlayer to configure, {@code null} to disable rendering overlay
     */
    public final void setOverlayer2(@Nullable Overlayer2 overlayer) {
        if (mOverlayer == overlayer) {
            return;
        }
        mOverlayer = overlayer;
        mView.queueEvent(mViewRenderer::applyOverlayer);
    }

    /**
     * Configures rendering texture loader.
     * <p>
     * Note that configuring a texture loader will forcefully stop and restart rendering, when currently started.
     *
     * @param textureLoader texture loader to configure, {@code null} to disable texture loading
     */
    public final void setTextureLoader(@Nullable TextureLoader textureLoader) {
        if (mTextureLoader == textureLoader) {
            return;
        }
        mTextureLoader = textureLoader;
        mView.queueEvent(mViewRenderer::applyTextureLoader);
    }

    /**
     * Attaches stream to be rendered.
     * <p>
     * Client is responsible to detach (call setStream(null)) any stream before the view is disposed, otherwise, leak
     * may occur.
     *
     * @param stream stream to render, {@code null} to detach stream
     */
    public final void setStream(@Nullable Stream stream) {
        if (mStream == stream) {
            return;
        }

        if (mSink != null) {
            mSink.close();
            mSink = null;
        }

        mStream = stream;

        if (mStream != null) {
            mSink = mStream.openSink(GlRenderSink.config(mSinkCallback));
        }
    }

    /**
     * Allows to receives latest rendered frame capture bitmap.
     */
    public interface CaptureCallback {

        /**
         * Called back to provide the result of latest frame capture.
         *
         * @param bitmap latest rendered frame, if capture was successful, otherwise {@code null}
         */
        void onCapture(@Nullable Bitmap bitmap);
    }

    /**
     * Captures latest rendered frame.
     * <p>
     * Frame is captured to the provided {@code bitmap}, then forwarded asynchronously to the provided {@code callback}.
     *
     * @param bitmap   bitmap to copy captured frame to; capture will be scaled tp match the width, height, and format
     *                 of this bitmap
     * @param callback callback notified when the capture is complete, either successfully, or because of any error
     */
    public final void capture(@NonNull Bitmap bitmap, @NonNull CaptureCallback callback) {
        PixelCopy.request(mView, bitmap,
                copyResult -> callback.onCapture(copyResult == PixelCopy.SUCCESS ? bitmap : null),
                new Handler());
    }

    /**
     * Captures latest rendered frame.
     * <p>
     * Frame is captured to a bitmap with the same size as this view, configured as {@link Bitmap.Config#ARGB_8888},
     * then forwarded asynchronously to the provided {@code callback}.
     *
     * @param callback callback notified when the capture is complete, either successfully, or because of any error
     */
    public final void capture(@NonNull CaptureCallback callback) {
        capture(Bitmap.createBitmap(mView.getWidth(), mView.getHeight(), Bitmap.Config.ARGB_8888), callback);
    }

    /**
     * Allows to listen to content zone changes.
     *
     * @see #setOnContentZoneChangeListener
     */
    public interface OnContentZoneChangeListener {

        /**
         * Notifies that the current content zone did change.
         *
         * @param view        the {@code GsdkStreamView} whose content zone did change
         * @param contentZone content zone after change
         */
        void onContentZoneChange(@NonNull GsdkStreamView view, @NonNull Rect contentZone);
    }

    /**
     * Registers a listener notified of content zone changes.
     * <p>
     * Only one listener may be registered at a time; registering another listener automatically unregisters any
     * previously registered one.
     *
     * @param listener listener to register, {@code null} to unregister any previously registered listener
     *
     * @see #getContentZone
     */
    public final void setOnContentZoneChangeListener(@Nullable OnContentZoneChangeListener listener) {
        mContentZoneChangeListener = listener;
    }

    /**
     * Gives access to rendered stream content zone.
     * <p>
     * Content zone defines the area where actual stream content is rendered, excluding padding introduced by any
     * configured {@link #setScaleType(int) scale type}.
     * <p>
     * Content zone is updated after each frame is rendered.
     *
     * @return current rendered stream content zone, {@link Rect#isEmpty() empty} when rendering is stopped and until
     *         first frame has been rendered
     */
    @NonNull
    public final Rect getContentZone() {
        return mContentZone;
    }

    /**
     * Notifies that rendering starts.
     * <p>
     * Called on a dedicated GL rendering thread
     * <p>
     * Subclasses may override this method to implement any custom behavior that must happen on the GL rendering thread
     * when rendering starts. <br>
     * Default implementation does nothing.
     */
    protected void onStartRendering() {
    }

    /**
     * Notifies that rendering stops.
     * <p>
     * Called on a dedicated GL rendering thread
     * <p>
     * Subclasses may override this method to implement any custom behavior that must happen on the GL rendering thread
     * when rendering stops. <br>
     * Default implementation does nothing.
     */
    protected void onStopRendering() {
    }

    /** Listens to GL render sink events. Callbacks are called on main thread. */
    private final GlRenderSink.Callback mSinkCallback = new GlRenderSink.Callback() {

        @Override
        public void onRenderingMayStart(@NonNull GlRenderSink.Renderer renderer) {
            mView.onResume();
            mView.queueEvent(() -> mViewRenderer.setStreamRenderer(renderer));
        }

        @Override
        public void onRenderingMustStop(@NonNull GlRenderSink.Renderer renderer) {
            mView.queueEvent(() -> mViewRenderer.setStreamRenderer(null));
            mView.onPause();
        }

        @Override
        public void onFrameReady(@NonNull GlRenderSink.Renderer renderer) {
            mView.requestRender();
        }

        @Override
        public void onContentZoneChange(@NonNull Rect contentZone) {
            mContentZone.set(contentZone);
            if (mContentZoneChangeListener != null) {
                mContentZoneChangeListener.onContentZoneChange(GsdkStreamView.this, mContentZone);
            }
        }
    };

    /** Custom GL surface view renderer. Manages surface lifecycle and rendering on a dedicated GL rendering thread. */
    private final class Renderer implements GLSurfaceView.Renderer {

        /** Stream renderer. */
        @Nullable
        private GlRenderSink.Renderer mStreamRenderer;

        /** Rendering surface area. Also acts as ready indicator: when non-{@code null}, rendering may start. */
        @Nullable
        private Rect mSurfaceZone;

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            onSurfaceReset();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Rect prevZone = mSurfaceZone;
            mSurfaceZone = new Rect(0, 0, width, height);
            if (mStreamRenderer != null) {
                if (prevZone == null) {
                    startRenderer();
                } else {
                    mStreamRenderer.setRenderZone(mSurfaceZone);
                }
            }
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (mStreamRenderer != null) {
                mStreamRenderer.renderFrame();
            }
        }

        /**
         * Installs stream renderer.
         * <p>
         * Once a renderer is installed, rendering starts as soon as the surface is ready.
         * <p>
         * Any previously installed renderer is stopped beforehand.
         *
         * @param renderer stream renderer, {@code null} to stop rendering
         */
        void setStreamRenderer(@Nullable GlRenderSink.Renderer renderer) {
            if (mStreamRenderer != null) {
                stopRenderer();
            }
            mStreamRenderer = renderer;
            if (mStreamRenderer != null && mSurfaceZone != null) {
                startRenderer();
            }
        }

        /**
         * Applies configured scale type if possible now.
         */
        void applyScaleType() {
            if (mStreamRenderer != null) {
                mStreamRenderer.setScaleType(fromViewScaleType(mScaleType));
            }
        }

        /**
         * Applies configured padding fill if possible now.
         */
        void applyPaddingFill() {
            if (mStreamRenderer != null) {
                mStreamRenderer.setPaddingFill(fromViewPaddingFill(mPaddingFill));
            }
        }

        /**
         * Applies configured zebras rendering if possible now.
         */
        void applyZebrasEnable() {
            if (mStreamRenderer != null) {
                mStreamRenderer.enableZebras(mZebrasEnabled);
            }
        }

        /**
         * Applies configured zebra threshold if possible now.
         */
        void applyZebraThreshold() {
            if (mStreamRenderer != null) {
                mStreamRenderer.setZebraThreshold(mZebraThreshold);
            }
        }

        /**
         * Applies configured histogram computation if possible now.
         */
        void applyHistogramEnable() {
            if (mStreamRenderer != null) {
                mStreamRenderer.enableHistogram(mHistogramEnabled);
            }
        }

        /**
         * Applies configured rendering overlayer if possible now.
         */
        void applyOverlayer() {
            if (mStreamRenderer != null) {
                mStreamRenderer.setOverlayer(mOverlayer);
            }
        }

        /**
         * Applies configured texture loader if possible now.
         * <p>
         * Stops and restarts rendering if started.
         */
        void applyTextureLoader() {
            if (mStreamRenderer != null && mSurfaceZone != null) {
                stopRenderer();
                startRenderer();
            }
        }

        /**
         * Called when the surface is about to be destroyed.
         */
        void onSurfaceDestroyed() {
            onSurfaceReset();
        }

        /**
         * Called when the GL surface becomes invalid.
         */
        private void onSurfaceReset() {
            mSurfaceZone = null;
            if (mStreamRenderer != null) {
                stopRenderer();
            }
        }

        /**
         * Stops rendering.
         * <p>
         * {@link #mStreamRenderer} must be non-{@code null} before calling this method.
         */
        private void stopRenderer() {
            assert mStreamRenderer != null;
            if (mStreamRenderer.stop()) {
                mView.post(mContentZone::setEmpty);
                onStopRendering();
            }
        }

        /**
         * Starts rendering.
         * <p>
         * Both {@link #mSurfaceZone} and {@link #mStreamRenderer} must be non-{@code null} before calling this method.
         */
        private void startRenderer() {
            assert mStreamRenderer != null && mSurfaceZone != null;
            if (mStreamRenderer.start(mTextureLoader)) {
                mStreamRenderer.setScaleType(fromViewScaleType(mScaleType));
                mStreamRenderer.setPaddingFill(fromViewPaddingFill(mPaddingFill));
                mStreamRenderer.enableZebras(mZebrasEnabled);
                mStreamRenderer.setZebraThreshold(mZebraThreshold);
                mStreamRenderer.enableHistogram(mHistogramEnabled);
                mStreamRenderer.setOverlayer(mOverlayer);
                mStreamRenderer.setRenderZone(mSurfaceZone);
                onStartRendering();
            }
        }
    }

    /**
     * Converts a view scale type to its stream renderer equivalent.
     *
     * @param scaleType view scale type to convert
     *
     * @return stream renderer scale type equivalent
     */
    @NonNull
    private static GlRenderSink.Renderer.ScaleType fromViewScaleType(@ScaleType int scaleType) {
        switch (scaleType) {
            case SCALE_TYPE_CROP:
                return GlRenderSink.Renderer.ScaleType.CROP;
            case SCALE_TYPE_FIT:
                return GlRenderSink.Renderer.ScaleType.FIT;
        }
        throw new IllegalArgumentException("Unsupported scale type: " + scaleType);
    }

    /**
     * Converts a view padding fill to its stream renderer equivalent.
     *
     * @param paddingFill view padding fill to convert
     *
     * @return stream renderer padding fill equivalent
     */
    @NonNull
    private static GlRenderSink.Renderer.PaddingFill fromViewPaddingFill(@PaddingFill int paddingFill) {
        switch (paddingFill) {
            case PADDING_FILL_BLUR_CROP:
                return GlRenderSink.Renderer.PaddingFill.BLUR_CROP;
            case PADDING_FILL_BLUR_EXTEND:
                return GlRenderSink.Renderer.PaddingFill.BLUR_EXTEND;
            case PADDING_FILL_NONE:
                return GlRenderSink.Renderer.PaddingFill.NONE;
        }
        throw new IllegalArgumentException("Unsupported padding fill: " + paddingFill);
    }
}