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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.FloatRange;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.parrot.drone.groundsdk.R;
import com.parrot.drone.groundsdk.internal.stream.GlRenderSink;
import com.parrot.drone.groundsdk.internal.view.GlView;
import com.parrot.drone.groundsdk.stream.Overlayer;
import com.parrot.drone.groundsdk.stream.Overlayer2;
import com.parrot.drone.groundsdk.stream.Stream;

/**
 * Custom android view which renders content in a way suitable for use as Head Mounted Display (using Virtual reality
 * glasses or headsets).
 * <p>
 * Rendered content is composed of <ul>
 * <li>a video background, either a drone video {@link Stream stream} or see-through video (the device's rear-facing
 * camera's video stream), and</li>
 * <li>an Android UI layout, rendered on top of the video background. </li>
 * </ul>
 */
public final class GsdkHmdView extends FrameLayout {

    /** Layout containing offscreen custom UI overlay. */
    @NonNull
    private final FrameLayout mOverlayView;

    /** GL view renderer. */
    @NonNull
    private final Renderer mRenderer;

    /** Left & right lenses vertical offset from view center, in millimeters. */
    private double mLensesVerticalOffset;

    /** Drone stream to render. {@code null} when no stream is attached. */
    @Nullable
    private Stream mStream;

    /**
     * GL Rendering sink obtained from {@link #mStream}, closed/reopen upon stream changes. Only opened when a window
     * is attached.
     */
    @Nullable
    private Stream.Sink mSink;

    /** Configured drone stream zebras rendering. */
    private boolean mStreamZebrasEnabled;

    /** Configured drone stream zebra threshold. */
    private double mStreamZebraThreshold;

    /** Configured drone stream histogram computation. */
    private boolean mStreamHistogram;

    /** Configured drone stream rendering overlayer. */
    @Nullable
    private Overlayer2 mStreamOverlayer;

    /**
     * {@code true} when see-through (device rear camera) is enabled. See through supersedes any attached drone
     * stream.
     */
    private boolean mSeeThrough;

    /** Client listener notified of see-through camera field of view changes. */
    @Nullable
    private OnFieldOfViewChangeListener mFovListener;

    /**
     * Constructor.
     *
     * @param context android context
     */
    public GsdkHmdView(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Constructor.
     *
     * @param context android context
     * @param attrs   configured view attributes
     */
    public GsdkHmdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor.
     *
     * @param context      android context
     * @param attrs        configured view attributes
     * @param defStyleAttr default style attribute, used to fallback on default attribute values
     */
    public GsdkHmdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * Constructor.
     *
     * @param context      android context
     * @param attrs        configured view attributes
     * @param defStyleAttr default style attribute, used to fallback on default attribute values
     * @param defStyleRes  default style resource, used to fallback on default attribute values
     */
    public GsdkHmdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.GsdkHmdView, defStyleAttr,
                defStyleRes);
        @LayoutRes int overlayLayout;
        @RawRes int hmdDataPack;
        String hmdModelName;
        try {
            overlayLayout = a.getResourceId(R.styleable.GsdkHmdView_gsdk_overlay, 0);
            mLensesVerticalOffset = a.getFloat(R.styleable.GsdkHmdView_gsdk_lensesVerticalOffset, 0);
            hmdDataPack = a.getResourceId(R.styleable.GsdkHmdView_gsdk_hmdDataPack, 0);
            hmdModelName = a.getString(R.styleable.GsdkHmdView_gsdk_hmdModelName);
        } finally {
            a.recycle();
        }

        mOverlayView = new FrameLayout(context) {

            {
                setWillNotDraw(false);
            }

            @Override
            public void onDescendantInvalidated(@NonNull View child, @NonNull View target) {
                invalidate();
                super.onDescendantInvalidated(child, target);
            }

            @SuppressWarnings("deprecation") // for legacy APIs support
            @Override
            public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
                invalidate();
                return super.invalidateChildInParent(location, dirty);
            }

            @Override
            protected void dispatchDraw(Canvas canvas) {
                mRenderer.withOverlayCanvas(glCanvas -> {
                    // scaling will only happen when texture size change on the GL thread races with
                    // onOverlaySizeChanged callback on the UI thread
                    glCanvas.scale(glCanvas.getWidth() / (float) canvas.getWidth(),
                            glCanvas.getHeight() / (float) canvas.getHeight());
                    glCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    super.dispatchDraw(glCanvas);
                });
            }
        };

        setOverlay(overlayLayout);

        GlView renderView = new GlView(context, attrs);

        mRenderer = new Renderer(renderView) {

            @Override
            void onOverlaySizeChanged(int width, int height) {
                updateViewLayout(mOverlayView, new LayoutParams(width, height));
            }

            @Override
            void onCameraGeometryChanged(float horizontalFov, float verticalFov, int width, int height) {
                if (mFovListener != null) {
                    mFovListener.onFieldOfViewChange(GsdkHmdView.this, horizontalFov, verticalFov, width, height);
                }
            }
        };

        mRenderer.setLensesVerticalOffset((float) mLensesVerticalOffset);

        if (hmdDataPack != 0 && hmdModelName != null) {
            setHmdModel(hmdDataPack, hmdModelName);
        }

        addView(renderView);
        addView(mOverlayView, new LayoutParams(0, 0));
    }

    /**
     * Configures HMD model to be used for rendering.
     *
     * @param hmdDataPack resource containing HMD model definitions
     * @param modelName   identifies the model to use from the provided data pack
     */
    public void setHmdModel(@RawRes int hmdDataPack, @NonNull String modelName) {
        mRenderer.setHmdModel(hmdDataPack, modelName);
    }

    /**
     * Configures left and right lenses vertical offset.
     * <p>
     * Defines how to vertically offset both lenses projection centers from the center of GsdkHmdView. Value in
     * millimeters.
     *
     * @param offset vertical lenses offset
     *
     * @see #getLensesVerticalOffset()
     */
    public void setLensesVerticalOffset(double offset) {
        if (Double.compare(mLensesVerticalOffset, offset) == 0) {
            return;
        }
        mLensesVerticalOffset = offset;
        mRenderer.setLensesVerticalOffset((float) mLensesVerticalOffset);
    }

    /**
     * Returns configured lenses vertical offset.
     * <p>
     * Value in millimeters.
     *
     * @return configured right lens horizontal offset
     *
     * @see #setLensesVerticalOffset(double)
     */
    public double getLensesVerticalOffset() {
        return mLensesVerticalOffset;
    }

    /**
     * Sets custom UI overlay to be rendered.
     *
     * @param layoutId identifies the layout resource that defines the custom UI overlay to render, {@code 0} to disable
     *                 UI overlay rendering
     *
     * @see #setOverlay(View)
     */
    public void setOverlay(@LayoutRes int layoutId) {
        mOverlayView.removeAllViews();
        if (layoutId != 0) {
            View.inflate(getContext(), layoutId, mOverlayView);
        }
    }

    /**
     * Sets custom UI overlay to be rendered.
     *
     * @param view custom UI overlay to render, {@code null} to disable UI overlay rendering
     *
     * @see #setOverlay(int)
     */
    public void setOverlay(@Nullable View view) {
        mOverlayView.removeAllViews();
        if (view != null) {
            mOverlayView.addView(view);
        }
    }

    /**
     * Attaches drone stream to be rendered.
     * <p>
     * Drone video stream is rendered as a background on each lens, fitting the available space, possibly introducing
     * some black padding so that the source stream aspect ratio is respected.
     * <p>
     * {@link #enableSeeThrough(boolean) See-through} supersedes drone stream rendering; in case both see-through is
     * enabled and a drone stream is attached, then see-through is rendered instead of drone stream, as long as
     * see-through remains enabled.
     * <p>
     * Client is responsible to detach (call {@code setStream(null)}) any stream before the view is disposed, otherwise,
     * leak may occur.
     *
     * @param stream stream to render, {@code null} to detach stream
     */
    public void setStream(@Nullable Stream stream) {
        if (mStream == stream) {
            return;
        }

        if (mSink != null) {
            mSink.close();
            mSink = null;
        }

        mStream = stream;

        if (mStream != null && !mSeeThrough && isAttachedToWindow()) {
            mSink = mStream.openSink(GlRenderSink.config(mSinkCallback));
        }
    }

    /**
     * Configures drone stream overexposure zebras rendering.
     *
     * @param enable {@code true} to enable zebras rendering, {@code false} to disable it
     *
     * @see #areStreamZebrasEnabled()
     */
    public void enableStreamZebras(boolean enable) {
        if (mStreamZebrasEnabled == enable) {
            return;
        }
        mStreamZebrasEnabled = enable;
        mRenderer.enableStreamZebras(mStreamZebrasEnabled);
    }

    /**
     * Tells whether drone stream overexposure zebras rendering is enabled.
     *
     * @return {@code true} if zebras rendering is enabled, otherwise {@code false}
     *
     * @see #enableStreamZebras(boolean)
     */
    public boolean areStreamZebrasEnabled() {
        return mStreamZebrasEnabled;
    }

    /**
     * Configures drone stream overexposure zebra threshold.
     *
     * @param threshold desired zebra threshold
     *
     * @see #areStreamZebrasEnabled()
     */
    public void setStreamZebraThreshold(@FloatRange(from = 0, to = 1) double threshold) {
        if (Double.compare(mStreamZebraThreshold, threshold) == 0) {
            return;
        }
        mStreamZebraThreshold = threshold;
        mRenderer.setStreamZebraThreshold(mStreamZebraThreshold);
    }

    /**
     * Returns configured drone stream overexposure zebra threshold.
     *
     * @return configured zebra threshold
     *
     * @see #setStreamZebraThreshold(double)
     */
    @FloatRange(from = 0, to = 1)
    public double getStreamZebraThreshold() {
        return mStreamZebraThreshold;
    }

    /**
     * Configures drone stream color histogram computation.
     *
     * @param enable {@code true} to enable histogram computation, {@code false} to disable it
     *
     * @see #isStreamHistogramEnabled()
     */
    public void enableStreamHistogram(boolean enable) {
        if (mStreamHistogram == enable) {
            return;
        }
        mStreamHistogram = enable;
        mRenderer.enableStreamHistogram(mStreamHistogram);
    }

    /**
     * Tells whether drone stream color histogram computation is enabled.
     *
     * @return {@code true} if histogram computation is enabled, otherwise {@code false}
     */
    public boolean isStreamHistogramEnabled() {
        return mStreamHistogram;
    }

    /**
     * Configures drone stream rendering overlayer.
     *
     * @param overlayer overlayer to configure, {@code null} to disable rendering overlay
     *
     * @deprecated use #setStreamOverlayer2(Overlayer2) instead.
     */
    @Deprecated
    public void setStreamOverlayer(@Nullable Overlayer overlayer) {
        setStreamOverlayer2(overlayer == null ? null : frameContext ->
                overlayer.overlay(frameContext.renderZone(), frameContext.contentZone(),
                        (Overlayer.Histogram) frameContext.histogram()));
    }

    /**
     * Configures drone stream rendering overlayer.
     *
     * @param overlayer overlayer to configure, {@code null} to disable rendering overlay
     */
    public void setStreamOverlayer2(@Nullable Overlayer2 overlayer) {
        if (mStreamOverlayer == overlayer) {
            return;
        }
        mStreamOverlayer = overlayer;
        mRenderer.setStreamOverlayer(mStreamOverlayer);
    }

    /**
     * Enables see-through rendering.
     * <p>
     * Video stream from the device's rear camera is rendered as a background on each lens, fitting the available space,
     * possibly introducing some black padding so that the source stream aspect ratio is respected.
     * <p>
     * See-through supersedes {@link #setStream(Stream) drone stream} rendering; in case both see-through is
     * enabled and a drone stream is attached, then see-through is rendered instead of drone stream, as long as
     * see-through remains enabled.
     *
     * @param enable {@code true} to enable see-through, {@code false} to disable it
     *
     * @see #isSeeThroughEnabled()
     */
    public void enableSeeThrough(boolean enable) {
        if (mSeeThrough != enable) {
            mSeeThrough = enable;
            if (mSink != null && mSeeThrough) {
                mSink.close();
                mSink = null;
            }
            if (isAttachedToWindow()) {
                mRenderer.enableCamera(mSeeThrough);
                if (mStream != null & !mSeeThrough) {
                    mSink = mStream.openSink(GlRenderSink.config(mSinkCallback));
                }
            }
        }
    }

    /**
     * Tells whether see-through rendering is enabled.
     *
     * @return {@code true} when see-through is enabled, otherwise {@code false}
     *
     * @see #enableSeeThrough(boolean)
     */
    public boolean isSeeThroughEnabled() {
        return mSeeThrough;
    }

    /**
     * Allows to listen to see-through camera field of view changes.
     *
     * @see #setOnFieldOfViewChangeListener(OnFieldOfViewChangeListener)
     */
    public interface OnFieldOfViewChangeListener {

        /**
         * Notifies that see-through (video stream from device's rear camera) field of view properties did change.
         *
         * @param view          {@code GsdkHmdView} whose see-through field of view properties did change
         * @param horizontalFov see-through horizontal field of view, in degrees
         * @param verticalFov   see-through vertical field of view, in degrees
         * @param width         width, in pixels, that the horizontal field of view physically covers
         * @param height        height, in pixels, that the vertical field of view physically covers
         */
        void onFieldOfViewChange(@NonNull GsdkHmdView view, float horizontalFov, float verticalFov, int width,
                                 int height);
    }

    /**
     * Registers a listener notified of see-through camera field of view changes.
     * <p>
     * Only one listener may be registered at a time; registering another listener automatically unregisters any
     * previously registered one.
     *
     * @param listener listener to register, {@code null} to unregister any previously registered listener
     */
    public void setOnFieldOfViewChangeListener(@Nullable OnFieldOfViewChangeListener listener) {
        mFovListener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mRenderer.enableCamera(mSeeThrough);
        if (mStream != null && mSink == null && !mSeeThrough) {
            mSink = mStream.openSink(GlRenderSink.config(mSinkCallback));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mRenderer.enableCamera(false);
        if (mSink != null) {
            mSink.close();
            mSink = null;
        }
        super.onDetachedFromWindow();
    }

    /** Listens to GL render sink events. Callbacks are called on main thread. */
    private final GlRenderSink.Callback mSinkCallback = new GlRenderSink.Callback() {

        @Override
        public void onRenderingMayStart(@NonNull GlRenderSink.Renderer renderer) {
            mRenderer.setStreamRenderer(renderer);
        }

        @Override
        public void onRenderingMustStop(@NonNull GlRenderSink.Renderer renderer) {
            mRenderer.setStreamRenderer(null);
        }
    };
}
