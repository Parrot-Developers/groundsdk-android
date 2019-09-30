package com.parrot.drone.groundsdk.hmd;

import android.content.Context;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Consumer;

/**
 * Monitors device display to keep track of orientation changes.
 */
final class Display {

    /** Portrait: 0 degrees device orientation. */
    static final int ORIENTATION_0 = 0;

    /** Landscape: 90 degrees device orientation. */
    static final int ORIENTATION_90 = 90;

    /** Reverse portrait: 180 degrees device orientation. */
    static final int ORIENTATION_180 = 180;

    /** Revers landscape: 270 degrees device orientation. */
    static final int ORIENTATION_270 = 270;

    /** Supported device orientation values. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ORIENTATION_0, ORIENTATION_90, ORIENTATION_180, ORIENTATION_270})
    @interface Orientation {}

    /**
     * Allows to listen to device orientation changes.
     */
    interface OrientationListener {

        /**
         * Notified when the device's orientation changes.
         *
         * @param orientation latest device orientation, in degrees
         */
        void onOrientationChanged(@Orientation int orientation);
    }

    /** Android orientation change listener. */
    @NonNull
    private final OrientationEventListener mOrientationMonitor;

    /** Android device default display. */
    @NonNull
    private final android.view.Display mDisplay;

    /** Client orientation listener, {@code null} when no listener is registered. */
    @Nullable
    private OrientationListener mOrientationListener;

    /** Current device orientation. */
    @Orientation
    private int mOrientation;

    /**
     * Constructor.
     *
     * @param context          android context
     * @param threadDispatcher allows to dispatch orientation changes to some custom thread
     */
    Display(@NonNull Context context, @NonNull Consumer<Runnable> threadDispatcher) {
        mDisplay = context.getSystemService(WindowManager.class).getDefaultDisplay();
        updateOrientation();
        mOrientationMonitor = new OrientationEventListener(context) {

            @Override
            public void onOrientationChanged(int orientation) {
                threadDispatcher.accept(Display.this::updateOrientation);
            }
        };
    }

    /**
     * Registers a listener notified of device orientation changes.
     *
     * @param listener listener to register, {@code null} to unregister any previous listener.
     */
    void setOrientationListener(@Nullable OrientationListener listener) {
        mOrientationListener = listener;
        if (mOrientationListener == null) {
            mOrientationMonitor.disable();
        } else {
            mOrientationMonitor.enable();
        }
    }

    /**
     * Retrieves current device orientation.
     *
     * @return device orientation
     */
    @Orientation
    int getOrientation() {
        return mOrientation;
    }

    /**
     * Computes and updates current device orientation.
     * <p>
     * Notifies any registered listener of orientation change if appropriate.
     */
    private void updateOrientation() {
        int orientation = ORIENTATION_0;
        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                orientation = ORIENTATION_0;
                break;
            case Surface.ROTATION_90:
                orientation = ORIENTATION_90;
                break;
            case Surface.ROTATION_180:
                orientation = ORIENTATION_180;
                break;
            case Surface.ROTATION_270:
                orientation = ORIENTATION_270;
                break;
        }

        if (mOrientation != orientation) {
            mOrientation = orientation;
            if (mOrientationListener != null) {
                mOrientationListener.onOrientationChanged(mOrientation);
            }
        }
    }
}
