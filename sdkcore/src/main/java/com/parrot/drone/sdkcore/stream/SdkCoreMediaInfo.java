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

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;

/**
 * Information upon a media supported by a stream.
 */
public class SdkCoreMediaInfo {

    /** Media unique identifier. */
    private final long mMediaId;

    /**
     * Constructor.
     * <p>
     * Called from native code.
     *
     * @param mediaId media unique identifier
     */
    private SdkCoreMediaInfo(long mediaId) {
        mMediaId = mediaId;
    }

    /**
     * Tells the unique identifier of the media.
     *
     * @return media unique identifier
     */
    public long mediaId() {
        return mMediaId;
    }

    /**
     * Information upon a video media supported by a stream.
     */
    public static class Video extends SdkCoreMediaInfo {

        /** Int definition of a video media source. */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(SOURCE_FRONT_CAMERA)
        @interface Source {}

        /* Numerical Source values MUST be kept in sync with C enum pdraw_video_type */

        /** Device front camera is the source of the video media. */
        public static final int SOURCE_FRONT_CAMERA = 0;

        /** Video media source. */
        @Source
        private final int mSource;

        /** Video width. */
        @IntRange(from = 0)
        private final int mWidth;

        /** Video height. */
        @IntRange(from = 0)
        private final int mHeight;

        /**
         * Constructor
         * <p>
         * Called from native code.
         *
         * @param mediaId video media unique identifier
         * @param source  video media source
         * @param width   video width, in pixels
         * @param height  video height, in pixels
         */
        private Video(long mediaId, @Source int source, @IntRange(from = 0) int width, @IntRange(from = 0) int height) {
            super(mediaId);
            mSource = source;
            mWidth = width;
            mHeight = height;
        }

        /**
         * Tells the source of the video media.
         *
         * @return media source
         */
        @Source
        public int source() {
            return mSource;
        }

        /**
         * Tells video width.
         *
         * @return video width, in pixels
         */
        @IntRange(from = 0)
        public int width() {
            return mWidth;
        }

        /**
         * Tells video height.
         *
         * @return video height, in pixels
         */
        @IntRange(from = 0)
        public int height() {
            return mHeight;
        }

        /**
         * Information upon a H.264 video media supported by a stream.
         */
        public static final class H264 extends Video {

            /** H.264 video stream Sequence Parameter Set. */
            @NonNull
            private final ByteBuffer mSps;

            /** H.264 video stream Picture Parameter Set. */
            @NonNull
            private final ByteBuffer mPps;

            /**
             * Constructor.
             * <p>
             * Called from native code.
             *
             * @param mediaId video media unique identifier
             * @param source  video media source
             * @param width   video width, in pixels
             * @param height  video height, in pixels
             * @param sps     H.264 video stream Sequence Parameter Set
             * @param pps     H.264 video stream Picture Parameter Set
             */
            private H264(long mediaId, int source, @IntRange(from = 0) int width, @IntRange(from = 0) int height,
                         @NonNull byte[] sps, @NonNull byte[] pps) {
                super(mediaId, source, width, height);
                mSps = ByteBuffer.wrap(sps);
                mPps = ByteBuffer.wrap(pps);
            }

            /**
             * Provides H.264 stream's Sequence Parameter Set.
             * <p>
             * Returned {@code ByteBuffer} is {@link ByteBuffer#asReadOnlyBuffer() read-only}.
             *
             * @return H.264 stream SPS
             */
            @NonNull
            public ByteBuffer sps() {
                return mSps.asReadOnlyBuffer();
            }

            /**
             * Provides H.264 stream's Picture Parameter Set.
             * <p>
             * Returned {@code ByteBuffer} is {@link ByteBuffer#asReadOnlyBuffer() read-only}.
             *
             * @return H.264 stream PPS
             */
            @NonNull
            public ByteBuffer pps() {
                return mPps.asReadOnlyBuffer();
            }

            /* JNI declarations and setup */
            private static native void nativeClassInit();

            //region Testing

            /**
             * Constructor.
             *
             * @param mediaId media unique identifier
             */
            @VisibleForTesting(otherwise = VisibleForTesting.NONE)
            public H264(long mediaId) {
                this(mediaId, SOURCE_FRONT_CAMERA, 0, 0, new byte[0], new byte[0]);
            }

            //endregion
        }

        /**
         * Information upon a YUV video media supported by a stream.
         */
        public static final class Yuv extends Video {

            /**
             * Constructor
             * <p>
             * Called from native code.
             *
             * @param mediaId video media unique identifier
             * @param source  video media source
             * @param width   video width, in pixels
             * @param height  video height, in pixels
             */
            private Yuv(long mediaId, int source, int width, int height) {
                super(mediaId, source, width, height);
            }

            /* JNI declarations and setup */
            private static native void nativeClassInit();

            //region Testing

            /**
             * Constructor.
             *
             * @param mediaId media unique identifier
             */
            @VisibleForTesting(otherwise = VisibleForTesting.NONE)
            public Yuv(long mediaId) {
                this(mediaId, SOURCE_FRONT_CAMERA, 0, 0);
            }

            //endregion
        }
    }

    /**
     * Initializes native SdkCoreMediaInfo concrete classes cache.
     * <p>
     * Called from {@link SdkCoreStream} {@code nativeClassInit()} since instance(s) of those class will be created from
     * native stream code.
     */
    static void nativeClassesInit() {
        Video.H264.nativeClassInit();
        Video.Yuv.nativeClassInit();
    }
}
