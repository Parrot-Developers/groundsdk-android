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

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.pomp.PompLoop;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Allows to receive frames from a stream.
 */
public class SdkCoreSink {

    /**
     * Scoping class for frame management.
     */
    public static final class Frame {

        /**
         * An opaque handle on a frame received by the sink.
         */
        @Retention(RetentionPolicy.SOURCE)
        @LongDef
        public @interface Handle {}

        /** Invalid frame handle. */
        @Handle
        public static final long INVALID_HANDLE = 0;

        /**
         * Provides access to a frame's native backend.
         *
         * @param handle handle on a frame
         *
         * @return native pointer onto the given frame's backend
         */
        public static long nativePtr(@Handle long handle) {
            return handle;
        }

        /**
         * Releases the frame.
         *
         * @param handle handle on the frame to release
         */
        public static void release(@Handle long handle) {
            nativeRelease(handle);
        }

        /**
         * Private constructor for static utility class.
         */
        private Frame() {
        }

        /* JNI declarations and setup */
        private static native void nativeRelease(long frameNativePtr);
    }

    /**
     * Listener notified of sink events.
     * <p>
     * All listener methods are called back on configured looper.
     */
    public interface Listener {

        /**
         * Notifies that a new frame is available from the sink.
         *
         * @param frameHandle opaque handle onto the received frame
         */
        void onFrame(@Frame.Handle long frameHandle);

        /**
         * Notifies that the sink has stopped.
         */
        void onStop();
    }

    /** Handler that dispatches listener callbacks on the client looper. */
    @NonNull
    private final Handler mListenerHandler;

    /** Listener notified of sink events. */
    @NonNull
    private final Listener mListener;

    /** Configures queue size. */
    @IntRange(from = 1)
    private int mQueueSize;

    /** Int definition of a sink queue policy. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({QUEUE_FULL_POLICY_DROP_ELDEST, QUEUE_FULL_POLICY_DROP_NEW})
    @interface QueueFullPolicy {}

    /* Numerical QueueFullPolicy values MUST be kept in sync with C enum sdkcore_sink_queue_full_policy */

    /** When a new frame is received but the queue is full, drop the eldest frame in the queue to add the new one. */
    public static final int QUEUE_FULL_POLICY_DROP_ELDEST = 0;

    /** When a new frame is received but the queue is full, drop the new frame. */
    public static final int QUEUE_FULL_POLICY_DROP_NEW = 1;

    /** Configured queue policy. */
    @QueueFullPolicy
    private int mQueueFullPolicy;

    /** Int definition of a frame format. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FRAME_FORMAT_UNSPECIFIED, FRAME_FORMAT_H264_BYTE_STREAM, FRAME_FORMAT_H264_AVCC})
    public @interface FrameFormat {}

    /* Numerical FrameFormat values MUST be kept in sync with C enum pdraw_h264_format */

    /** Unspecified frame format. Let the implementation decide how delivered frames must be formatted. */
    public static final int FRAME_FORMAT_UNSPECIFIED = 0;

    /** Byte stream H.264 frame format. Received H.264 frames are prefixed with annex-b 0x00000001 start code. */
    public static final int FRAME_FORMAT_H264_BYTE_STREAM = 1;

    /** AVCC H.264 frame format. Received H.264 frames are prefixed with the following frame length, in bytes. */
    public static final int FRAME_FORMAT_H264_AVCC = 2;

    /** Configured frame format. */
    @FrameFormat
    private int mFrameFormat;

    /** Stream pomp loop. {@code null} until the sink is started. */
    @Nullable
    private PompLoop mPomp;

    /** SdkCoreSink native backend pointer. */
    private long mNativePtr;

    /**
     * Constructor.
     * <p>
     * By default, queue size is, queue policy is {@link #QUEUE_FULL_POLICY_DROP_ELDEST}, and frame format is
     * {@link #FRAME_FORMAT_UNSPECIFIED}.
     *
     * @param looper   looper onto which listener callbacks will be executed
     * @param listener listener notified of sink events
     */
    public SdkCoreSink(@NonNull Looper looper, @NonNull Listener listener) {
        mListenerHandler = new Handler(looper, msg -> {
            // recompose frame long handle by merging both message int arguments.
            @SuppressLint("WrongConstant")
            long frameHandle = (((long) msg.arg2) << 32) | (msg.arg1 & 0xFFFFFFFFL);
            listener.onFrame(frameHandle);
            return true;
        });
        mListener = listener;
        mQueueSize = 1;
        mQueueFullPolicy = QUEUE_FULL_POLICY_DROP_ELDEST;
        mFrameFormat = FRAME_FORMAT_UNSPECIFIED;
    }

    /**
     * Configures the sink queue size.
     * <p>
     * Configuration must happen before the sink is started.
     *
     * @param size desired queue size
     *
     * @return {@code this}, to allow chained calls
     *
     * @throws IllegalStateException in case the sink is already started
     */
    @NonNull
    public SdkCoreSink setQueueSize(@IntRange(from = 1) int size) {
        if (mPomp != null) {
            throw new IllegalStateException("Sink started");
        }

        mQueueSize = size;

        return this;
    }

    /**
     * Configures the sink queue policy.
     * <p>
     * Configuration must happen before the sink is started.
     *
     * @param policy desired queue policy
     *
     * @return {@code this}, to allow chained calls
     *
     * @throws IllegalStateException in case the sink is already started
     */
    @NonNull
    public SdkCoreSink setQueueFullPolicy(@QueueFullPolicy int policy) {
        if (mPomp != null) {
            throw new IllegalStateException("Sink started");
        }

        mQueueFullPolicy = policy;

        return this;
    }

    /**
     * Configures the sink frame format.
     * <p>
     * This has effect only when the receives H.264 encoded frames.
     * <p>
     * Configuration must happen before the sink is started.
     *
     * @param frameFormat desired frame format
     *
     * @return {@code this}, to allow chained calls
     *
     * @throws IllegalStateException in case the sink is already started
     */
    @NonNull
    public SdkCoreSink setFrameFormat(@FrameFormat int frameFormat) {
        if (mPomp != null) {
            throw new IllegalStateException("Sink started");
        }

        mFrameFormat = frameFormat;

        return this;
    }

    /**
     * Starts the sink.
     *
     * @param stream  stream that will deliver frames to the sink
     * @param pomp    stream pomp loop
     * @param mediaId identifies the stream media to be delivered to the sink
     */
    void start(@NonNull SdkCoreStream stream, @NonNull PompLoop pomp, long mediaId) {
        if (mPomp != null) {
            throw new IllegalStateException("Sink started");
        }

        mPomp = pomp;
        mPomp.onPomp(() -> {
            long streamNativePtr = stream.nativePtr();
            mNativePtr = streamNativePtr == 0 ? 0 : nativeStart(streamNativePtr, mPomp.nativePtr(), mediaId, mQueueSize,
                    mQueueFullPolicy, mFrameFormat);
            if (mNativePtr == 0) {
                mPomp.onMain(() -> {
                    mPomp = null;
                    mListenerHandler.post(mListener::onStop);
                });
            }
        });
    }

    /**
     * Resynchronizes the sink.
     * <p>
     * TODO: document usage
     */
    public void resynchronize() {
        if (mPomp == null) {
            throw new IllegalStateException("Sink stopped");
        }
        mPomp.onPomp(() -> {
            if (mNativePtr != 0) {
                nativeResynchronize(mNativePtr);
            }
        });
    }

    /**
     * Stops the sink.
     *
     * @throws IllegalStateException in case the stream is already stopped
     */
    public void stop() {
        if (mPomp == null) {
            throw new IllegalStateException("Sink stopped");
        }

        PompLoop pomp = mPomp;
        mPomp = null;
        mListenerHandler.post(mListener::onStop);
        pomp.onPomp(() -> {
            if (mNativePtr != 0 && nativeStop(mNativePtr)) {
                mNativePtr = 0;
            }
        });
    }

    /**
     * Called back when the sink receives a frame
     * <p>
     * Called from native on pomp loop.
     * <p>
     * Produced frame ownership is transferred to this method; frame must be {@link Frame#release(long) released} once
     * no longer needed.
     */
    @SuppressWarnings("unused") /* native callback */
    private void onFrame(long frameNativePtr) {
        // use android Message, which are pooled, to reduce GC pressure.
        // Native frame long handle is split into two ints that are forwarded in the message's arguments.
        Message msg = mListenerHandler.obtainMessage();
        msg.arg1 = (int) frameNativePtr;
        msg.arg2 = (int) (frameNativePtr >>> 32);
        msg.sendToTarget();
    }

    /* JNI declarations and setup */
    private native long nativeStart(long streamNativePtr, long pompNativePtr, long mediaId,
                                    @IntRange(from = 1) int queueSize, @QueueFullPolicy int queueFullPolicy,
                                    @FrameFormat int frameFormat);

    private static native boolean nativeResynchronize(long nativePtr);

    private static native boolean nativeStop(long nativePtr);

    private static native void nativeClassInit();

    static {
        nativeClassInit();
    }
}
