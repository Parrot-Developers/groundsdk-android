package com.parrot.drone.groundsdk.internal.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.ConditionVariable;
import android.os.Looper;
import android.util.AttributeSet;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A GLSurfaceView with additional behavior: <ul>
 * <li>The attached {@link Renderer} defines the render mode and the GLES version.</li>
 * <li>The renderer base class provides methods to communicate between the main and GL threads.</li>
 * <li>The renderer has an additional callback telling when the surface gets destroyed.</li>
 * </ul>
 */
public final class GlView extends GLSurfaceView {

    /** Renderer. {@code null} until a renderer is {@link #launchRendering(Renderer) attached} */
    @Nullable
    private Renderer mRenderer;

    /**
     * Base for GlView Renderers.
     */
    public abstract static class Renderer implements GLSurfaceView.Renderer {

        /** GLES version 2. */
        protected static final int GLES_V2 = 2;

        /** GLES version 3. */
        protected static final int GLES_V3 = 3;

        /**
         * Defines supported GLES versions.
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({GLES_V2, GLES_V3})
        @interface GlesVersion {}

        /** Configured GLES version. */
        @GlesVersion
        private final int mGlesVersion;

        /**
         * Defines supported render modes.
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({RENDERMODE_CONTINUOUSLY, RENDERMODE_WHEN_DIRTY})
        @interface RenderMode {}

        /** Configured render mode. */
        @RenderMode
        private final int mRenderMode;

        /** Attached render view. {@code null} until rendering is {@link #launchRendering(Renderer) launched}. */
        @Nullable
        private GlView mView;

        /**
         * Constructor.
         *
         * @param mode        operating render mode
         * @param glesVersion operating GLES version
         */
        protected Renderer(@RenderMode int mode, @GlesVersion int glesVersion) {
            mRenderMode = mode;
            mGlesVersion = glesVersion;
        }

        /**
         * Executes a runnable on the GL rendering thread.
         * <p>
         * Does nothing in case rendering has not been {@link #launchRendering(Renderer) launched}.
         *
         * @param runnable runnable to execute.
         *
         * @throws IllegalStateException if not called from main thread
         */
        protected final void runOnGlThread(@NonNull Runnable runnable) {
            if (!Looper.getMainLooper().isCurrentThread()) {
                throw new IllegalStateException("Not on main thread");
            }

            if (mView != null) {
                mView.queueEvent(runnable);
            }
        }

        /**
         * Executes a runnable on the main UI thread.
         * <p>
         * Does nothing in case rendering has not been {@link #launchRendering(Renderer) launched}.
         *
         * @param runnable runnable to execute.
         *
         * @throws IllegalStateException if called from main thread
         */
        protected final void runOnMainThread(@NonNull Runnable runnable) {
            if (Looper.getMainLooper().isCurrentThread()) {
                throw new IllegalStateException("Already on main thread");
            }

            if (mView != null) {
                mView.post(runnable);
            }
        }

        /**
         * Notifies that the rendering surface is about to be destroyed.
         */
        protected abstract void onSurfaceDestroyed();
    }

    /**
     * Constructor.
     *
     * @param context android context
     */
    public GlView(Context context) {
        super(context);
    }

    /**
     * Constructor.
     *
     * @param context android context
     * @param attrs   configured view attributes
     */
    public GlView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Launches rendering.
     *
     * @param renderer renderer to use for rendering
     */
    public void launchRendering(@NonNull Renderer renderer) {
        if (mRenderer != null) {
            throw new IllegalStateException("Renderer already set");
        }

        mRenderer = renderer;
        mRenderer.mView = this;

        super.setEGLContextClientVersion(renderer.mGlesVersion);
        super.setRenderer(renderer);
        super.setRenderMode(mRenderer.mRenderMode);
    }

    @Override
    protected void onDetachedFromWindow() {
        Renderer renderer = mRenderer;

        if (renderer != null) {
            ConditionVariable lock = new ConditionVariable();
            queueEvent(() -> {
                lock.open(); // we are sure that the callback will be invoked now, allow UI thread to continue
                renderer.onSurfaceDestroyed();
            });
            lock.block(); // block UI thread to ensure the message is processed before GL thread exits
        }

        super.onDetachedFromWindow(); // GL thread will have exited after returning from this function
    }

    @Deprecated
    @Override
    public void setRenderer(GLSurfaceView.Renderer renderer) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void setRenderMode(int renderMode) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void setEGLContextClientVersion(int version) {
        throw new UnsupportedOperationException();
    }
}
