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

import android.util.DisplayMetrics;
import android.util.SizeF;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * HMD lens rendering geometry data.
 */
final class Geometry {

    /**
     * Allows to compute rendering geometry data based on various input variables.
     */
    static final class Computer {

        /** An inch, in millimeters. */
        private static final float MILLIMETERS_PER_INCH = 25.4f;

        /** Device display metrics. Used to query pixel density. */
        @NonNull
        private final DisplayMetrics mDisplayMetrics;

        /** Head Mounted Display model to compute geometry for. */
        @Nullable
        private HmdModel mHmdModel;

        /** Total available rendering width, in pixels. */
        @IntRange(from = 0)
        private int mSurfaceWidthPx;

        /** Total available rendering height, in pixels. */
        @IntRange(from = 0)
        private int mSurfaceHeightPx;

        /** Both lenses vertical offset from rendering surface center, in pixels. */
        private int mLensVerticalOffsetPx;

        /**
         * Constructor.
         *
         * @param displayMetrics    device display metrics
         */
        Computer(@NonNull DisplayMetrics displayMetrics) {
            mDisplayMetrics = displayMetrics;
        }

        /**
         * Configures HMD model to compute geometry for.
         *
         * @param model HMD model
         */
        void setHmdModel(@NonNull HmdModel model) {
            mHmdModel = model;
        }

        /**
         * Configures both lenses center vertical offset from rendering surface center.
         *
         * @param offset lenses vertical offset, in millimeters
         */
        void setVerticalLensesOffset(float offset) {
            mLensVerticalOffsetPx = mmToVerticalPx(offset);
        }

        /**
         * Configures total available rendering dimensions.
         *
         * @param width  total available rendering width, in pixels
         * @param height total available rendering height, in pixels
         */
        void setSurfaceDimensions(int width, int height) {
            mSurfaceWidthPx = width;
            mSurfaceHeightPx = height;
        }

        /**
         * Compute current rendering geometry.
         *
         * @return rendering geometry data
         */
        @NonNull
        Geometry compute() {
            return new Geometry(this);
        }

        /**
         * Converts millimeters to screen horizontal pixels.
         *
         * @param millimeters value to convert, in millimeters
         *
         * @return value equivalent, in screen horizontal pixels
         */
        private int mmToHorizontalPx(float millimeters) {
            return Math.round(millimeters * mDisplayMetrics.xdpi / MILLIMETERS_PER_INCH);
        }

        /**
         * Converts millimeters to screen vertical pixels.
         *
         * @param millimeters value to convert, in millimeters
         *
         * @return value equivalent, in screen vertical pixels
         */
        private int mmToVerticalPx(float millimeters) {
            return Math.round(millimeters * mDisplayMetrics.ydpi / MILLIMETERS_PER_INCH);
        }

        /**
         * Converts screen horizontal pixels to millimeters.
         *
         * @param pixels value to convert, in screen horizontal pixels
         *
         * @return value equivalent, in millimeters
         */
        private float pxToHorizontalMm(int pixels) {
            return pixels * MILLIMETERS_PER_INCH / mDisplayMetrics.xdpi;
        }

        /**
         * Converts screen vertical pixels to millimeters.
         *
         * @param pixels value to convert, in screen vertical pixels
         *
         * @return value equivalent, in millimeters
         */
        private float pxToVerticalMm(int pixels) {
            return pixels * MILLIMETERS_PER_INCH / mDisplayMetrics.ydpi;
        }
    }

    /** Total available rendering width, in pixels. */
    @IntRange(from = 0)
    final int surfaceWidthPx;

    /** Total available rendering height, in pixels. */
    @IntRange(from = 0)
    final int surfaceHeightPx;

    /** Computed rendering width for a single lens, in pixels. */
    @IntRange(from = 0)
    final int lensRenderWidthPx;

    /** Computed rendering height for a single lens, in pixels. */
    @IntRange(from = 0)
    final int lensRenderHeightPx;

    /** Computed rendering width for a single lens, in millimeters. */
    @FloatRange(from = 0)
    final float lensRenderWidthMm;

    /** Computed rendering height for a single lens, in millimeters. */
    @FloatRange(from = 0)
    final float lensRenderHeightMm;

    /** Lens fixed render mesh size, in pixels. */
    @IntRange(from = 0)
    final int lensMeshWidthPx;

    /** Lens fixed render mesh size, in pixels. */
    @IntRange(from = 0)
    final int lensMeshHeightPx;

    /** Top coordinate, in pixels, of the rect where a lens should be rendered. */
    final int lensRenderTop;

    /** Bottom coordinate, in pixels, of the rect where a lens should be rendered. */
    final int lensRenderBottom;

    /** Left coordinate, in pixels, of the rect where left lens should be rendered. */
    final int leftLensRenderLeft;

    /** Right coordinate, in pixels, of the rect where left lens should be rendered. */
    final int leftLensRenderRight;

    /** Left coordinate, in pixels, of the rect where right lens should be rendered. */
    final int rightLensRenderLeft;

    /** Right coordinate, in pixels, of the rect where right lens should be rendered. */
    final int rightLensRenderRight;

    /**
     * Constructor.
     *
     * @param computer computer for this geometry data
     */
    private Geometry(@NonNull Computer computer) {
        surfaceWidthPx = computer.mSurfaceWidthPx;
        surfaceHeightPx = computer.mSurfaceHeightPx;

        HmdModel hmdModel = computer.mHmdModel;

        int leftOffsetPx = 0, rightOffsetPx = 0;
        int maxRenderWidthPx = Integer.MAX_VALUE, maxRenderHeightPx = Integer.MAX_VALUE;
        float meshSizeMm = 0;

        if (hmdModel != null) {
            leftOffsetPx = computer.mmToHorizontalPx(hmdModel.defaultIpd() / 2);
            rightOffsetPx = leftOffsetPx;

            SizeF maxRenderSize = hmdModel.maxRenderSize();
            maxRenderWidthPx = computer.mmToHorizontalPx(maxRenderSize.getWidth());
            maxRenderHeightPx = computer.mmToVerticalPx(maxRenderSize.getHeight());

            meshSizeMm = hmdModel.meshSize();
        }

        lensMeshWidthPx = computer.mmToHorizontalPx(meshSizeMm);
        lensMeshHeightPx = computer.mmToVerticalPx(meshSizeMm);

        maxRenderWidthPx = Math.min(maxRenderWidthPx, lensMeshWidthPx);
        maxRenderHeightPx = Math.min(maxRenderHeightPx, lensMeshHeightPx);

        int verticalOffsetPx = computer.mLensVerticalOffsetPx;
        int halfSurfaceWidthPx = surfaceWidthPx / 2;

        lensRenderWidthPx = Math.max(0, Math.min(maxRenderWidthPx, halfSurfaceWidthPx - Math.max(
                        Math.abs(2 * leftOffsetPx - halfSurfaceWidthPx),
                        Math.abs(2 * rightOffsetPx - halfSurfaceWidthPx))));
        lensRenderHeightPx = Math.max(0, Math.min(maxRenderHeightPx, surfaceHeightPx));

        lensRenderWidthMm = computer.pxToHorizontalMm(lensRenderWidthPx);
        lensRenderHeightMm = computer.pxToVerticalMm(lensRenderHeightPx);

        leftLensRenderLeft = halfSurfaceWidthPx - lensRenderWidthPx / 2 - leftOffsetPx;
        leftLensRenderRight = leftLensRenderLeft + lensRenderWidthPx;

        rightLensRenderLeft = halfSurfaceWidthPx - lensRenderWidthPx / 2 + rightOffsetPx;
        rightLensRenderRight = rightLensRenderLeft + lensRenderWidthPx;

        lensRenderTop = surfaceHeightPx / 2 - lensRenderHeightPx / 2 + verticalOffsetPx;
        lensRenderBottom = lensRenderTop + lensRenderHeightPx;
    }
}
