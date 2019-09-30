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

package com.parrot.drone.sdkcore.stream;

import android.graphics.Rect;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.pomp.PompLoop;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Stream renderer.
 * <p>
 * Allows to render a video stream. All SdkCoreRenderer API <strong>MUST</strong> be called from the same thread; this
 * thread <strong>MUST</strong> also be GL-capable (such as the internal thread used by android
 * {@link android.opengl.GLSurfaceView}).
 * <p>
 * Note however that {@link Listener} callbacks are always called on <strong>MAIN</strong> thread.
 */
public class SdkCoreRenderer {

    /**
     * Listener notified of rendering events.
     * <p>
     * All listener methods are called back on <strong>MAIN</strong> thread.
     */
    public interface Listener {

        /**
         * Notifies that a frame is ready to be rendered.
         * <p>
         * Client has then the responsibility to switch back to the rendering thread and call
         * {@link #renderFrame()} to effectively render the frame.
         */
        void onFrameReady();

        /**
         * Notifies that the content zone has changed.
         * <p>
         * The content zone is a zone inside the render zone, where the actual frame content has been rendered,
         * excluding additional padding that may have been introduced to respect original aspect ratio.
         *
         * @param contentZone new content zone
         */
        default void onContentZoneChanged(@NonNull Rect contentZone) {
        }
    }

    /** Int definition of a renderer fill mode. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FILL_MODE_FIT, FILL_MODE_CROP, FILL_MODE_FIT_PAD_BLUR_CROP, FILL_MODE_FIT_PAD_BLUR_EXTEND})
    public @interface FillMode {
    }

    /* Numerical fill mode values MUST be kept in sync with C enum pdraw_video_renderer_fill_mode */

    /**
     * Scales the stream so that its largest dimension spans the whole render zone; its smallest dimension is scaled
     * accordingly to respect original aspect ratio, centered in the render zone; introduced padding, if any, has
     * default clear color.
     */
    public static final int FILL_MODE_FIT = 0;

    /**
     * Scales the stream so that its smallest dimension spans the whole render zone; its largest dimension is scaled
     * accordingly to respect original aspect ratio, and cropped to the render zone.
     */
    public static final int FILL_MODE_CROP = 1;

    /**
     * Same as {@link #FILL_MODE_FIT}, padded by first drawing the frame in {@link #FILL_MODE_CROP} mode, blurred, then
     * overlaying the scaled frame on top of it.
     */
    public static final int FILL_MODE_FIT_PAD_BLUR_CROP = 2;

    /**
     * Same as {@link #FILL_MODE_FIT}, padded by repeating content borders, blurred.
     */
    public static final int FILL_MODE_FIT_PAD_BLUR_EXTEND = 3;

    /** Rendering thread identifier. Used for sanity checks. */
    private final long mThreadId;

    /** Zone where the stream must be rendered. */
    @NonNull
    private final Rect mRenderZone;

    /** Actual stream content zone, after scaling and excluding padding. */
    @NonNull
    private final Rect mContentZone;

    /** A temporary, reused rect, used on render thread for rect computations, in order to reduce allocations. */
    @NonNull
    private final Rect mScratchRect;

    /**
     * Pomp loop. {@code null} unless rendering is started.
     * Modified on rendering thread. Volatile access on pomp thread.
     */
    @Nullable
    private volatile PompLoop mPomp;

    /**
     * Rendering listener. {@code null} unless rendering is started.
     * Modified on rendering thread. Volatile access on main thread.
     */
    @Nullable
    private volatile Listener mListener;

    /** Rendering overlayer. Called back on rendering thread after a frame is rendered. */
    @Nullable
    private SdkCoreOverlayer mOverlayer;

    /** Rendering texture loader. Called back on rendering thread before a frame is rendered. */
    @Nullable
    private SdkCoreTextureLoader mTextureLoader;

    /** SdkCoreRenderer native backend pointer. */
    private long mNativePtr;

    /**
     * Constructor.
     * <p>
     * Thread calling this method is assumed to be the rendering thread. It must be GL-capable. <br/>
     * Other methods from this API must be called on that particular thread.
     */
    public SdkCoreRenderer() {
        mThreadId = Thread.currentThread().getId();
        mRenderZone = new Rect();
        mContentZone = new Rect();
        mScratchRect = new Rect();
        mNativePtr = nativeInit();
        if (mNativePtr == 0) {
            throw new Error("Failed to create SdkCoreRenderer native backend");
        }
    }

    /**
     * Configures render zone.
     * <p>
     * Must be called on the rendering thread.
     *
     * @param renderZone render zone to configure
     *
     * @return {@code true} if successful, otherwise {@code false}
     */
    public boolean setRenderZone(@NonNull Rect renderZone) {
        assertThread();
        assertNativePtr();

        mScratchRect.set(renderZone);
        mScratchRect.sort();

        if (!nativeSetRenderZone(mNativePtr, mScratchRect.left, mScratchRect.top, mScratchRect.width(),
                mScratchRect.height())) {
            return false;
        }

        mRenderZone.set(mScratchRect);

        return true;
    }

    /**
     * Configures fill mode.
     * <p>
     * Must be called on the rendering thread.
     *
     * @param fillMode fill mode to configure
     *
     * @return {@code true} if successful, otherwise {@code false}
     */
    public boolean setFillMode(@FillMode int fillMode) {
        assertThread();
        assertNativePtr();

        return nativeSetFillMode(mNativePtr, fillMode);
    }

    /**
     * Configures overexposure zebras rendering.
     * <p>
     * Must be called on the rendering thread.
     *
     * @param enable {@code true} to enable zebras rendering, {@code false} to disable it
     *
     * @return {@code true} if successful, otherwise {@code false}
     */
    public boolean enableZebras(boolean enable) {
        assertThread();
        assertNativePtr();

        return nativeEnableZebras(mNativePtr, enable);
    }

    /**
     * Configures overexposure zebra threshold.
     * <p>
     * Must be called on the rendering thread.
     *
     * @param threshold zebra threshold to configure
     *
     * @return {@code true} if successful, otherwise {@code false}
     */
    public boolean setZebraThreshold(@FloatRange(from = 0, to = 1) double threshold) {
        assertThread();
        assertNativePtr();

        return nativeSetZebraThreshold(mNativePtr, threshold);
    }

    /**
     * Configures color histogram computation.
     * <p>
     * Must be called on the rendering thread.
     *
     * @param enable {@code true} to enable histogram computation, {@code false} to disable it
     *
     * @return {@code true} if successful, otherwise {@code false}
     */
    public boolean enableHistogram(boolean enable) {
        assertThread();
        assertNativePtr();

        return nativeEnableHistogram(mNativePtr, enable);
    }

    /**
     * Sets up rendering overlay callback.
     * <p>
     * Must be called on the rendering thread.
     * <p>
     * {@code callback} is called on the rendering thread, after frame has been rendered consequently to a call to
     * {@link #renderFrame()}.
     *
     * @param callback overlay callback to set, {@code null} to disable rendering overlay
     *
     * @return {@code true} if successful, otherwise {@code false}
     */
    public boolean setOverlayCallback(@Nullable SdkCoreOverlayer.Callback callback) {
        assertThread();
        assertNativePtr();

        SdkCoreOverlayer overlayer = null;
        if (callback != null) {
            overlayer = SdkCoreOverlayer.create(callback);
            if (overlayer == null) {
                return false;
            }
        }

        if (!nativeSetOverlayer(mNativePtr, overlayer == null ? 0 : overlayer.getNativePtr())) {
            if (overlayer != null) {
                overlayer.dispose();
            }
            return false;
        }

        if (mOverlayer != null) {
            mOverlayer.dispose();
        }
        mOverlayer = overlayer;

        return true;
    }

    /**
     * Sets up rendering texture load callback.
     * <p>
     * Must be called on the rendering thread. <br/>
     * Texture loader callback cannot be changed while the renderer is started.
     * <p>
     * {@code callback} is called on the rendering thread, before a frame is about to be rendered consequently to a call
     * to {@link #renderFrame()}.
     *
     * @param width             texture width, in pixels, {@code 0} if unspecified
     * @param aspectRatioWidth  texture aspect ration width factor, {@code 0} if unspecified
     * @param aspectRatioHeight texture aspect ration height factor, {@code 0} if unspecified
     * @param callback          texture loader callback to set, {@code null} to disable texture loading (in which case,
     *                          other parameters are meaningless
     *
     * @return {@code true} if successful, otherwise {@code false}
     */
    public boolean setTextureLoaderCallback(@IntRange(from = 0) int width, @IntRange(from = 0) int aspectRatioWidth,
                                            @IntRange(from = 0) int aspectRatioHeight,
                                            @Nullable SdkCoreTextureLoader.Callback callback) {
        assertThread();
        assertNativePtr();

        SdkCoreTextureLoader textureLoader = null;
        if (callback != null) {
            textureLoader = SdkCoreTextureLoader.create(width, aspectRatioWidth, aspectRatioHeight, callback);
            if (textureLoader == null) {
                return false;
            }
        }

        if (!nativeSetTextureLoader(mNativePtr, textureLoader == null ? 0 : textureLoader.getNativePtr())) {
            if (textureLoader != null) {
                textureLoader.dispose();
            }
            return false;
        }

        if (mTextureLoader != null) {
            mTextureLoader.dispose();
        }
        mTextureLoader = textureLoader;

        return true;
    }

    /**
     * Starts renderer.
     * <p>
     * Must be called on the rendering thread. <br/>
     * {@code listener} is called on main thread.
     *
     * @param nativeStreamPtr SdkCoreStream native backend handle
     * @param pomp            pomp loop
     * @param listener        listener notified of rendering events
     *
     * @return {@code true} if the renderer could be started, otherwise {@code false}
     */
    boolean start(long nativeStreamPtr, @NonNull PompLoop pomp, @NonNull Listener listener) {
        if (mListener != null) {
            return false; // already started
        }

        assertThread();
        assertNativePtr();

        mPomp = pomp;
        mListener = listener;

        if (nativeStart(mNativePtr, nativeStreamPtr)) {
            return true;
        }

        mPomp = null;
        mListener = null;

        return false;
    }

    /**
     * Temporary content zone storage, updated by {@link #nativeRenderFrame}.
     * <p>
     * Zone x coordinate at index 0; zone y coordinate at index 1; zone width at index 2; zone height at index 3.
     */
    private final int[] mTmpContentZone = new int[4];

    /**
     * Requests rendering of current frame.
     * <p>
     * Must be called on the rendering thread. <br/>
     *
     * @return {@code true} if current frame could be rendered, otherwise {@code false}
     */
    public boolean renderFrame() {
        assertThread();
        assertNativePtr();

        // backup current content zone
        int x = mTmpContentZone[0], y = mTmpContentZone[1], width = mTmpContentZone[2], height = mTmpContentZone[3];

        if (!nativeRenderFrame(mNativePtr, mTmpContentZone)) {
            return false;
        }

        // check content zone change
        if (mTmpContentZone[0] != x || mTmpContentZone[1] != y
            || mTmpContentZone[2] != width || mTmpContentZone[3] != height) {
            // compute new content zone
            int left = mRenderZone.left + mTmpContentZone[0];
            int top = mRenderZone.top + mTmpContentZone[1];
            int right = left + mTmpContentZone[2];
            int bottom = top + mTmpContentZone[3];

            // forward content zone change.
            Listener listener = mListener;
            assert listener != null;
            //noinspection ConstantConditions: mPomp is only set to null on the rendering thread
            mPomp.onMain(() -> {
                mContentZone.set(left, top, right, bottom);
                listener.onContentZoneChanged(mContentZone);
            });
        }

        return true;
    }

    /**
     * Stops renderer.
     * <p>
     * Must be called on the rendering thread.
     *
     * @return {@code true} if the renderer could be stopped, otherwise {@code false}
     */
    public boolean stop() {
        assertThread();
        assertNativePtr();

        if (nativeStop(mNativePtr)) {
            mPomp = null;
            mListener = null;
            return true;
        }

        return false;
    }

    /**
     * Disposes renderer.
     * <p>
     * Must be called on the rendering thread.
     * <p>
     * This {@code SdkCoreRenderer} instance must not be used after this method returns.
     */
    public void dispose() {
        assertThread();
        assertNativePtr();

        if (!nativeDestroy(mNativePtr)) {
            throw new AssertionError("Failed to destroy SdkCoreRenderer native backend");
        }
        mNativePtr = 0;

        if (mOverlayer != null) {
            mOverlayer.dispose();
        }

        if (mTextureLoader != null) {
            mTextureLoader.dispose();
        }
    }

    /**
     * Ensures caller thread is the rendering thread.
     *
     * @throws IllegalStateException in case calling thread is not the rendering thread
     */
    private void assertThread() {
        if (Thread.currentThread().getId() != mThreadId) {
            throw new IllegalStateException("Not on renderer thread [id: " + mThreadId + "]");
        }
    }

    /**
     * Ensures renderer has not been {@link #dispose() disposed}.
     *
     * @throws IllegalStateException in case renderer is disposed
     */
    private void assertNativePtr() {
        if (mNativePtr == 0) {
            throw new IllegalStateException("SdkCoreRenderer is destroyed");
        }
    }

    /**
     * Called back when a frame is ready to be rendered.
     * <p>
     * Called from native on POMP context.
     */
    @SuppressWarnings("unused") /* native callback */
    private void onFrameReady() {
        PompLoop pomp = mPomp;
        if (pomp != null) {
            pomp.onMain(mFrameNotification);
        }
    }

    /**
     * Runnable notifying frame ready event on main thread.
     * <p>
     * Allows not to allocate a lambda each time {@link PompLoop#onMain} is called.
     */
    private final Runnable mFrameNotification = () -> {
        Listener listener = mListener;
        if (listener != null) {
            listener.onFrameReady();
        }
    };

    /* JNI declarations and setup */
    private native long nativeInit();

    private static native boolean nativeSetRenderZone(long nativePtr, int x, int y, @IntRange(from = 0) int width,
                                                      @IntRange(from = 0) int height);

    private static native boolean nativeSetFillMode(long nativePtr, @FillMode int fillMode);

    private static native boolean nativeEnableZebras(long nativePtr, boolean enable);

    private static native boolean nativeSetZebraThreshold(long nativePtr,
                                                          @FloatRange(from = 0, to = 1) double threshold);

    private static native boolean nativeEnableHistogram(long nativePtr, boolean enable);

    private static native boolean nativeSetOverlayer(long nativePtr, long overlayerNativePtr);

    private static native boolean nativeSetTextureLoader(long nativePtr, long textureLoaderNativePtr);

    private static native boolean nativeStart(long nativePtr, long streamNativePtr);

    private static native boolean nativeRenderFrame(long nativePtr, @Nullable int[] zone);

    private static native boolean nativeStop(long nativePtr);

    private static native boolean nativeDestroy(long mNativePtr);

    private static native void nativeClassInit();

    static {
        nativeClassInit();
    }
}
