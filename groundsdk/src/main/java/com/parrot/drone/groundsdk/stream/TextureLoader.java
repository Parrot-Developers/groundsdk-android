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

import android.annotation.SuppressLint;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.Maths;

/**
 * Allows to bypass default GL texture loading step when rendering a stream using {@link GsdkStreamView}.
 * <p>
 * Client code must {@link TextureSpec specify dimensions} for the GL texture that the renderer will provide. <br>
 * Then, for each frame to be rendered, {@link #loadTexture} will be called; client code must override this callback
 * to fill the GL texture according to its needs. <br>
 * The renderer will then scale, pad and render the loaded texture as appropriate.
 */
public abstract class TextureLoader {

    /** Allows to specify GL texture dimensions. */
    public static final class TextureSpec {

        /**
         * Requests a GL texture with specific dimensions.
         *
         * @param width  texture width
         * @param height texture height
         *
         * @return a new {@code TextureSpec} instance
         */
        @NonNull
        public static TextureSpec fixedSize(@IntRange(from = 1) int width, @IntRange(from = 1) int height) {
            return new TextureSpec(width, width, height);
        }

        /**
         * Requests a GL texture with specific aspect ratio.
         * <p>
         * Texture final dimensions are unspecified but will respect given aspect ratio.
         *
         * @param ratioNumerator   texture aspect ratio numerator value
         * @param ratioDenominator texture aspect ratio denominator value
         *
         * @return a new {@code TextureSpec} instance
         */
        @NonNull
        public static TextureSpec fixedAspectRatio(@IntRange(from = 1) int ratioNumerator,
                                                   @IntRange(from = 1) int ratioDenominator) {
            return new TextureSpec(0, ratioNumerator, ratioDenominator);
        }

        /**
         * Requests a GL texture with the same aspect ratio as the source.
         * <p>
         * Texture final width will be {@code width} value; final texture height will be so that the source's aspect
         * ratio is respected.
         *
         * @param width texture width, in pixels
         *
         * @return a new {@code TextureSpec} instance
         */
        @NonNull
        public static TextureSpec sourceAspectRatio(@IntRange(from = 1) int width) {
            return new TextureSpec(width, 0, 0);
        }

        /** Requests a GL texture with same dimensions as the source. */
        public static final TextureSpec SOURCE_DIMENSIONS = new TextureSpec(0, 0, 0);

        /** Requests a GL texture with 4/3 aspect ratio. */
        public static final TextureSpec ASPECT_RATIO_4_3 = new TextureSpec(0, 4, 3);

        /** Requests a GL texture with 16/9 aspect ratio. */
        public static final TextureSpec ASPECT_RATIO_16_9 = new TextureSpec(0, 16, 9);

        /** Texture width, in pixels, {@code 0} if not specified. */
        @IntRange(from = 0)
        private final int mWidth;

        /** Texture height, in pixels, {@code 0} if not specified. Derived from other parameters. */
        @IntRange(from = 0)
        private final int mHeight;

        /** Texture aspect ratio width, {@code 0} if not specified. */
        @IntRange(from = 0)
        private final int mRatioNumerator;

        /** Texture aspect ratio height, {@code 0} if not specified. */
        @IntRange(from = 0)
        private final int mRatioDenominator;

        /**
         * Constructor.
         *
         * @param width            texture width, in pixels, {@code 0} if unspecified
         * @param ratioNumerator   texture aspect ratio numerator, {@code 0} if unspecified
         * @param ratioDenominator texture aspect ratio denominator, {@code} if unspecified
         */
        private TextureSpec(@IntRange(from = 0) int width, @IntRange(from = 0) int ratioNumerator,
                            @IntRange(from = 0) int ratioDenominator) {
            mWidth = width;
            if (ratioNumerator != 0 && ratioDenominator != 0) {
                @SuppressLint("Range") int gcd = Maths.gcd(ratioNumerator, ratioDenominator);
                ratioNumerator /= gcd;
                ratioDenominator /= gcd;
            }
            mRatioNumerator = ratioNumerator;
            mRatioDenominator = ratioDenominator;
            mHeight = mRatioNumerator == 0 ? 0 : (mWidth * mRatioDenominator / mRatioNumerator);
        }

        /**
         * Retrieves specified texture width.
         *
         * @return specified texture width, in pixels, {@code 0} if not specified
         */
        @IntRange(from = 0)
        public int getWidth() {
            return mWidth;
        }

        /**
         * Retrieves specified texture height.
         *
         * @return specified texture height, in pixels, {@code 0} if not specified
         */
        @IntRange(from = 0)
        public int getHeight() {
            return mHeight;
        }

        /**
         * Retrieves specified aspect ratio numerator.
         *
         * @return specified aspect ratio numerator, {@code 0} if not specified
         */
        @IntRange(from = 0)
        public int getAspectRatioNumerator() {
            return mRatioNumerator;
        }

        /**
         * Retrieves specified aspect ratio denominator.
         *
         * @return specified aspect ratio denominator, {@code 0} if not specified
         */
        @IntRange(from = 0)
        public int getAspectRatioDenominator() {
            return mRatioDenominator;
        }
    }

    /** Texture specification. */
    @NonNull
    private final TextureSpec mTextureSpec;

    /**
     * Constructor.
     *
     * @param textureSpec texture specification
     */
    protected TextureLoader(@NonNull TextureSpec textureSpec) {
        mTextureSpec = textureSpec;
    }

    /**
     * Retrieves configured texture specification.
     *
     * @return texture spec
     */
    @NonNull
    public final TextureSpec getTextureSpec() {
        return mTextureSpec;
    }

    /**
     * Contextual information on the texture.
     */
    public interface TextureContext {

        /**
         * Gives texture width.
         *
         * @return texture width, in pixels
         */
        @IntRange(from = 0)
        int textureWidth();

        /**
         * Gives texture height.
         *
         * @return texture height, in pixels
         */
        @IntRange(from = 0)
        int textureHeight();
    }

    /**
     * Contextual information on a frame.
     */
    public interface FrameContext {

        /**
         * Gives access to current frame.
         *
         * @return handle to the current frame
         */
        long frameHandle();

        /**
         * Gives access to current frame user data.
         *
         * @return handle to the current frame user data
         */
        long frameUserDataHandle();

        /**
         * Gives access to current frame user data size.
         *
         * @return current frame user data size, in bytes
         */
        @IntRange(from = 0)
        long frameUserDataSize();

        /**
         * Gives access to streaming session metadata.
         *
         * @return handle to streaming session metadata
         */
        long sessionMetadataHandle();
    }

    /**
     * Loads GL texture.
     * <p>
     * Called on {@link GsdkStreamView} GL rendering thread.
     *
     * @param textureContext contextual information about the texture where the frame is rendered, invalid after this
     *                       method returns
     * @param frameContext   contextual information about the frame to be rendered, invalid after this method returns
     *
     * @return {@code true} to indicate that texture loading was successful, otherwise {@code false}
     */
    public abstract boolean loadTexture(@NonNull TextureContext textureContext, @NonNull FrameContext frameContext);
}
