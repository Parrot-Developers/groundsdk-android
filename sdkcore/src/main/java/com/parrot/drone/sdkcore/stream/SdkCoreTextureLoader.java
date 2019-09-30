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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.ulog.ULog;

import static com.parrot.drone.sdkcore.arsdk.Logging.TAG_STREAM;

/**
 * Renderer texture loader.
 * <p>
 * Allows to process current frame and load a custom texture, bypassing the renderer. The renderer will then still
 * process the texture to apply scaling and padding.
 */
public final class SdkCoreTextureLoader {

    /**
     * Callback notified of texture load requests.
     */
    public interface Callback {

        /**
         * Notifies that texture must be loaded.
         * <p>
         * Called on the rendering thread.
         *
         * @param textureLoader SdkCoreTextureLoader instance, for accessing current {@link #frameHandle() frame} and
         *                      {@link #frameUserDataHandle() user data}
         *
         * @return {@code true} if texture loading was successful, otherwise {@code false}
         */
        boolean onLoadTexture(@NonNull SdkCoreTextureLoader textureLoader);
    }

    /**
     * Creates an {@code SdkCoreTextureLoader} instance.
     *
     * @param width             texture width, in pixels, {@code 0} if unspecified
     * @param aspectRatioWidth  texture aspect ration width factor, {@code 0} if unspecified
     * @param aspectRatioHeight texture aspect ration height factor, {@code 0} if unspecified
     * @param callback          callback notified of texture load requests
     *
     * @return an {@code SdkCoreTextureLoader} instance if successful, otherwise {@code null}
     */
    @Nullable
    static SdkCoreTextureLoader create(@IntRange(from = 0) int width, @IntRange(from = 0) int aspectRatioWidth,
                                       @IntRange(from = 0) int aspectRatioHeight, @NonNull Callback callback) {
        SdkCoreTextureLoader textureLoader = new SdkCoreTextureLoader(width, aspectRatioWidth, aspectRatioHeight,
                callback);
        if (textureLoader.mNativePtr == 0) {
            ULog.e(TAG_STREAM, "Failed to create SdkCoreTextureLoader");
            return null;
        }
        return textureLoader;
    }

    /** Callback notified of texture load requests. */
    @NonNull
    private final Callback mCallback;

    /** SdkCoreTextureLoader native backend pointer. */
    private long mNativePtr;

    /** Texture width, in pixels. Only meaningful during {@link #onLoadTexture} call. */
    @IntRange(from = 0)
    private int mTextureWidth;

    /** Texture height, in pixels. Only meaningful during {@link #onLoadTexture} call. */
    @IntRange(from = 0)
    private int mTextureHeight;

    /** Native pointer on the current frame. Only meaningful during {@link #onLoadTexture} call. */
    private long mFrameNativePtr;

    /** Native pointer on the current frame user data. Only meaningful during {@link #onLoadTexture} call. */
    private long mUserDataNativePtr;

    /** Current frame user data size, in bytes. Only meaningful during {@link #onLoadTexture} call. */
    @IntRange(from = 0)
    private long mUserDataSize;

    /** Native pointer on streaming session metadata. Only meaningful during {@link #onLoadTexture} call. */
    private long mSessionMetaNativePtr;

    /**
     * Constructor.
     *
     * @param width             texture width, in pixels, {@code 0} if unspecified
     * @param aspectRatioWidth  texture aspect ration width factor, {@code 0} if unspecified
     * @param aspectRatioHeight texture aspect ration height factor, {@code 0} if unspecified
     * @param callback          callback notified of overlay requests
     */
    private SdkCoreTextureLoader(@IntRange(from = 0) int width, @IntRange(from = 0) int aspectRatioWidth,
                                 @IntRange(from = 0) int aspectRatioHeight, @NonNull Callback callback) {
        mCallback = callback;
        mNativePtr = nativeInit(width, aspectRatioWidth, aspectRatioHeight);
    }

    /**
     * Retrieves texture width.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onLoadTexture load texture callback}.
     *
     * @return texture width, in pixels
     */
    @IntRange(from = 0)
    public int textureWidth() {
        return mTextureWidth;
    }

    /**
     * Retrieves texture height.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onLoadTexture load texture callback}.
     *
     * @return texture height, in pixels
     */
    @IntRange(from = 0)
    public int textureHeight() {
        return mTextureHeight;
    }

    /**
     * Gives access to current frame.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onLoadTexture load texture callback}.
     *
     * @return handle to the current frame
     */
    public long frameHandle() {
        return mFrameNativePtr;
    }

    /**
     * Gives access to current frame user data.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onLoadTexture load texture callback}.
     *
     * @return handle to the current frame user data
     */
    public long frameUserDataHandle() {
        return mUserDataNativePtr;
    }

    /**
     * Gives access to current frame user data size.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onLoadTexture load texture callback}.
     *
     * @return current frame user data size, in bytes
     */
    @IntRange(from = 0)
    public long frameUserDataSize() {
        return mUserDataSize;
    }

    /**
     * Gives access to streaming session metadata.
     * <p>
     * Returned value is only meaningful during a call to {@link Callback#onLoadTexture load texture callback}.
     *
     * @return handle to streaming session metadata
     */
    public long sessionMetadataHandle() {
        return mSessionMetaNativePtr;
    }

    /**
     * Gives access to SdkCoreTextureLoader native backend handle.
     *
     * @return SdkCoreTextureLoader native backend handle
     *
     * @throws IllegalStateException in case SdkCoreTextureLoader is {@link #dispose() disposed}
     */
    long getNativePtr() {
        if (mNativePtr == 0) {
            throw new IllegalStateException("SdkCoreTextureLoader is destroyed");
        }
        return mNativePtr;
    }

    /**
     * Disposes texture loader.
     *
     * @throws IllegalStateException in case SdkCoreTextureLoader is already disposed
     */
    void dispose() {
        if (mNativePtr == 0) {
            throw new IllegalStateException("SdkCoreTextureLoader is destroyed");
        }
        nativeDestroy(mNativePtr);
        mNativePtr = 0;
    }

    /**
     * Called back when texture may be loaded.
     * <p>
     * Called from native on the rendering thread.
     *
     * @param textureWidth           texture width, in pixels
     * @param textureHeight          texture height, in pixels
     * @param frameNativePtr         native handle on the frame to be loaded
     * @param frameUserDataNativePtr native handle on opaque frame user data
     * @param frameUserDataSize      frame user data size, in bytes
     * @param sessionMetaNativePtr   native handle on streaming session metadata
     *
     * @return {@code true} in case load was successful, otherwise {@code false}
     */
    @SuppressWarnings("unused") /* native callback */
    private boolean onLoadTexture(@IntRange(from = 0) int textureWidth, @IntRange(from = 0) int textureHeight,
                                  long frameNativePtr, long frameUserDataNativePtr, long frameUserDataSize,
                                  long sessionMetaNativePtr) {
        mTextureWidth = textureWidth;
        mTextureHeight = textureHeight;
        mFrameNativePtr = frameNativePtr;
        mUserDataNativePtr = frameUserDataNativePtr;
        mUserDataSize = frameUserDataSize;
        mSessionMetaNativePtr = sessionMetaNativePtr;

        return mCallback.onLoadTexture(this);
    }

    /* JNI declarations and setup */
    private native long nativeInit(@IntRange(from = 0) int width, @IntRange(from = 0) int aspectRatioWidth,
                                   @IntRange(from = 0) int aspectRatioHeight);

    private static native void nativeDestroy(long nativePtr);

    private static native void nativeClassInit();

    static {
        nativeClassInit();
    }
}
