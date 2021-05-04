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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.ulog.ULog;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG_STREAM;

/**
 * Renderer overlayer.
 * <p>
 * Allows to draw on top of a rendered frame.
 */
public final class SdkCoreOverlayer {

    /**
     * Callback notified of overlay requests.
     */
    public interface Callback {

        /**
         * Notifies that frame overlay may be drawn.
         * <p>
         * Called on the rendering thread.
         *
         * @param overlayer SdkCoreOverlayer instance, for accessing current {@link #renderZone() render} and
         *                  {@link #contentZone() content} zone information, as well as
         *                  {@link #redChannelHistogram() red}, {@link #greenChannelHistogram() green},
         *                  {@link #blueChannelHistogram() blue} and {@link #luminanceChannelHistogram() luminance}
         *                  histograms
         */
        void onOverlay(@NonNull SdkCoreOverlayer overlayer);
    }

    /**
     * Creates an {@code SdkCoreOverlayer} instance.
     *
     * @param callback callback notified of overlay requests
     *
     * @return an {@code SdkCoreOverlayer} instance if successful, otherwise {@code null}
     */
    @Nullable
    static SdkCoreOverlayer create(@NonNull Callback callback) {
        SdkCoreOverlayer overlayer = new SdkCoreOverlayer(callback);
        if (overlayer.mNativePtr == 0) {
            ULog.e(TAG_STREAM, "Failed to create SdkCoreOverlayer");
            return null;
        }
        return overlayer;
    }


    /** Render zone. Updated from native on the rendering thread right before {@link #onOverlay()} is called. */
    @NonNull
    private final Rect mRenderZone;

    /** Content zone. Updated from native on the rendering thread right before {@link #onOverlay()} is called. */
    @NonNull
    private final Rect mContentZone;

    /** Native pointer on the video session info. */
    private long mSessionInfoNativePtr;

    /** Native pointer on the video session metadata. */
    private long mSessionMetadataNativePtr;

    /** Native pointer on the current frame metadata. */
    private long mFrameMetadataNativePtr;

    /**
     * Red color channel histogram data. Updated from native on the rendering thread right before {@link #onOverlay()}
     * is called.
     */
    @NonNull
    private final float[] mHistogramRed;

    /**
     * Green color channel histogram data. Updated from native on the rendering thread right before {@link #onOverlay()}
     * is called.
     */
    @NonNull
    private final float[] mHistogramGreen;

    /**
     * Blue color channel histogram data. Updated from native on the rendering thread right before {@link #onOverlay()}
     * is called.
     */
    @NonNull
    private final float[] mHistogramBlue;

    /**
     * Luminance channel histogram data. Updated from native on the rendering thread right before {@link #onOverlay()}
     * is called.
     */
    @NonNull
    private final float[] mHistogramLuma;

    /** Callback notified of overlay requests. */
    @NonNull
    private final Callback mCallback;

    /** SdkCoreOverlayer native backend pointer. */
    private long mNativePtr;

    /**
     * Constructor.
     *
     * @param callback callback notified of overlay requests
     */
    private SdkCoreOverlayer(@NonNull Callback callback) {
        mCallback = callback;
        mRenderZone = new Rect();
        mContentZone = new Rect();
        mHistogramRed = new float[0];
        mHistogramGreen = new float[0];
        mHistogramBlue = new float[0];
        mHistogramLuma = new float[0];

        mNativePtr = nativeInit();
    }

    /**
     * Gives access to SdkCoreOverlayer native backend handle.
     *
     * @return SdkCoreOverlayer native backend handle
     *
     * @throws IllegalStateException in case SdkCoreOverlayer is {@link #dispose() disposed}
     */
    long getNativePtr() {
        if (mNativePtr == 0) {
            throw new IllegalStateException("SdkCoreOverlayer is destroyed");
        }
        return mNativePtr;
    }

    /**
     * Gives access to render zone.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onOverlay overlay callback}.
     *
     * @return current render zone
     */
    @NonNull
    public Rect renderZone() {
        return mRenderZone;
    }

    /**
     * Gives access to content zone.
     * <p>
     * This informs about the actual stream content zone, after scaling and excluding padding.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onOverlay overlay callback}.
     *
     * @return current content zone
     */
    @NonNull
    public Rect contentZone() {
        return mContentZone;
    }

    /**
     * Gives access to the video session info.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onOverlay overlay callback}.
     *
     * @return handle to the video session info.
     */
    public long sessionInfoHandle() {
        return mSessionInfoNativePtr;
    }

    /**
     * Gives access to the video session metadata.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onOverlay overlay callback}.
     *
     * @return handle to the video session metadata.
     */
    public long sessionMetadataHandle() {
        return mSessionMetadataNativePtr;
    }

    /**
     * Gives access to current frame metadata.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onOverlay overlay callback}.
     *
     * @return handle to the current frame metadata.
     */
    public long frameMetadataHandle() {
        return mFrameMetadataNativePtr;
    }

    /**
     * Gives access to red color channel histogram.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onOverlay overlay callback}, when
     * {@link SdkCoreRenderer#enableHistogram histogram computation} is enabled.
     *
     * @return histogram for red color channel
     */
    @NonNull
    public float[] redChannelHistogram() {
        return mHistogramRed;
    }

    /**
     * Gives access to green color channel histogram.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onOverlay overlay callback}, when
     * {@link SdkCoreRenderer#enableHistogram histogram computation} is enabled.
     *
     * @return histogram for green color channel
     */
    @NonNull
    public float[] greenChannelHistogram() {
        return mHistogramGreen;
    }

    /**
     * Gives access to blue color channel histogram.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onOverlay overlay callback}, when
     * {@link SdkCoreRenderer#enableHistogram histogram computation} is enabled.
     *
     * @return histogram for blue color channel
     */
    @NonNull
    public float[] blueChannelHistogram() {
        return mHistogramBlue;
    }

    /**
     * Gives access to luminance channel histogram.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onOverlay overlay callback}, when
     * {@link SdkCoreRenderer#enableHistogram histogram computation} is enabled.
     *
     * @return histogram for luminance channel
     */
    @NonNull
    public float[] luminanceChannelHistogram() {
        return mHistogramLuma;
    }

    /**
     * Disposes overlayer.
     *
     * @throws IllegalStateException in case SdkCoreOverlayer is already disposed
     */
    void dispose() {
        if (mNativePtr == 0) {
            throw new IllegalStateException("SdkCoreOverlayer is destroyed");
        }
        nativeDestroy(mNativePtr);
        mNativePtr = 0;
    }

    /**
     * Called back when overlay may be drawn.
     * <p>
     * Called from native on the rendering thread.
     * <p>
     * @param sessionInfo PDRAW session info
     * @param sessionMetadata session metadata
     * @param frameMetadata frame metadata
     * <p>
     * {@link #mRenderZone}, {@link #mContentZone}, {@link #mHistogramRed}, {@link #mHistogramGreen},
     * {@link #mHistogramBlue} and {@link #mHistogramLuma} are updated from native right before this method is called.
     */
    @SuppressWarnings("unused") /* native callback */
    private void onOverlay(long sessionInfo, long sessionMetadata, long frameMetadata) {
        mSessionInfoNativePtr = sessionInfo;
        mSessionMetadataNativePtr = sessionMetadata;
        mFrameMetadataNativePtr = frameMetadata;

        mCallback.onOverlay(this);
    }

    /* JNI declarations and setup */
    private native long nativeInit();

    private static native void nativeDestroy(long nativePtr);

    private static native void nativeClassInit();

    static {
        nativeClassInit();
    }
}
