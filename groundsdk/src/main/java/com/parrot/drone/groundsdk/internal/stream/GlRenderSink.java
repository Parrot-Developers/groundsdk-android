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

package com.parrot.drone.groundsdk.internal.stream;

import android.graphics.Rect;
import android.opengl.GLSurfaceView;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.stream.Overlayer2;
import com.parrot.drone.groundsdk.stream.Stream;
import com.parrot.drone.groundsdk.stream.TextureLoader;

/**
 * A sink that allows stream video to be rendered on a {@link GLSurfaceView}.
 */
public interface GlRenderSink extends Stream.Sink {

    /**
     * Video stream renderer.
     * <p>
     * Allows to configure rendering and request frame to be rendered on the current GL Surface.
     * <p>
     * All renderer API <strong>MUST</strong> be accessed from the same GL-capable thread, such as the one provided by
     * a {@link GLSurfaceView}.
     */
    interface Renderer {

        /**
         * Defines how the video stream must be scaled with regard to the original aspect ratio and the render zone.
         */
        enum ScaleType {

            /**
             * Scales the stream so that its largest dimension spans the whole render zone; its smallest dimension is
             * scaled accordingly to respect original aspect ratio, centered in the render zone; introduced padding,
             * if any, is rendered according to {@link #setPaddingFill(PaddingFill) padding fill configuration}.
             *
             * @see #setScaleType(ScaleType)
             */
            FIT,

            /**
             * Scales the stream so that its smallest dimension spans the whole render zone; its largest dimension is
             * scaled accordingly to respect original aspect ratio, and cropped to the render zone; no padding is
             * introduced.
             *
             * @see #setScaleType(ScaleType)
             */
            CROP
        }

        /**
         * Defines how padding introduced by {@link ScaleType#FIT fit scale type} must be rendered.
         */
        enum PaddingFill {

            /**
             * Padding introduced by {@link ScaleType#FIT fit scale type} is filled with default reset color.
             *
             * @see #setPaddingFill(PaddingFill)
             */
            NONE,

            /**
             * Padding introduced by {@link ScaleType#FIT fit scale type} is filled by first rendering the current
             * stream frame using {@link ScaleType#CROP crop scale type}, blurred, then overlaying the scaled frame on
             * top of it.
             *
             * @see #setPaddingFill(PaddingFill)
             */
            BLUR_CROP,

            /**
             * Padding introduced by {@link ScaleType#FIT fit scale type} is filled by repeating current frame borders,
             * blurred.
             *
             * @see #setPaddingFill(PaddingFill)
             */
            BLUR_EXTEND,
        }

        /**
         * Starts the renderer.
         *
         * @param textureLoader texture loader to use for custom texture loading, {@code null} to disable custom texture
         *                      loading
         *
         * @return {@code true} if the renderer did start, otherwise {@code false}
         *
         * @see #stop()
         */
        boolean start(@Nullable TextureLoader textureLoader);

        /**
         * Configures the render zone.
         *
         * @param renderZone zone where the stream must be rendered, including padding introduced by any configured
         *                   {@link Renderer#setScaleType(Renderer.ScaleType) scale type}.
         *
         * @return {@code true} if render zone could be configured, otherwise {@code false}
         */
        boolean setRenderZone(@NonNull Rect renderZone);

        /**
         * Configures stream scale type.
         *
         * @param scaleType desired scale type
         *
         * @return {@code true} if scale type could be configured, otherwise {@code false}
         *
         * @see #setPaddingFill(PaddingFill)
         */
        boolean setScaleType(@NonNull ScaleType scaleType);

        /**
         * Configures stream padding fill.
         *
         * @param paddingFill desired padding fill
         *
         * @return {@code true} if padding fill could be configured, otherwise {@code false}
         *
         * @see #setScaleType(ScaleType)
         */
        boolean setPaddingFill(@NonNull PaddingFill paddingFill);

        /**
         * Configures overexposure zebras rendering.
         * <p>
         * When enabled, overexposure zebras are rendered on the video stream as per current
         * {@link #setZebraThreshold(double) zebra threshold configuration}.
         *
         * @param enable {@code true} to enable overexposure zebras rendering, {@code false} to disable it
         *
         * @return {@code true} if overexposure zebras rendering could be configured, otherwise {@code false}
         *
         * @see #setZebraThreshold(double)
         */
        boolean enableZebras(boolean enable);

        /**
         * Configures overexposure zebra threshold.
         *
         * @param threshold zebra threshold to configure
         *
         * @return {@code true} if overexposure zebra threshold could be configured, otherwise {@code false}
         *
         * @see #enableZebras(boolean)
         */
        boolean setZebraThreshold(@FloatRange(from = 0, to = 1) double threshold);

        /**
         * Configures color histogram computation.
         * <p>
         * When enabled, color histogram statistics are computed for each rendered frame. <br>
         * Those computation can be retrieved through the configured {@link Overlayer2}, if any.
         *
         * @param enable {@code true} to enable color histogram computation, {@code false} to disable it
         *
         * @return {@code true} if color histogram computation could be enabled, otherwise {@code false}
         *
         * @see #setOverlayer(Overlayer2)
         */
        boolean enableHistogram(boolean enable);

        /**
         * Configures rendering overlayer.
         *
         * @param overlayer overlayer to configure, {@code null} to disable rendering overlay
         *
         * @return {@code true} if rendering overlayer could be configured, otherwise {@code false}
         */
        boolean setOverlayer(@Nullable Overlayer2 overlayer);

        /**
         * Renders current frame.
         *
         * @return {@code true} if current frame could be rendered, otherwise {@code false}
         */
        boolean renderFrame();

        /**
         * Stops the renderer.
         *
         * @return {@code true} if the renderer could be stopped, otherwise {@code false}
         *
         * @see #start(TextureLoader)
         */
        boolean stop();
    }

    /**
     * Sink event callbacks.
     * <p>
     * All methods are called on the <strong>MAIN</strong> thread.
     */
    interface Callback {

        /**
         * Notifies that rendering is ready to start.
         * <p>
         * Note that the provided renderer <strong>MUST</strong> only be used from a GL-capable rendering thread,
         * such as the one provided by a {@link GLSurfaceView}.
         *
         * @param renderer video stream renderer
         */
        void onRenderingMayStart(@NonNull Renderer renderer);

        /**
         * Notifies that rendering must stop immediately.
         * <p>
         * When this method is called, {@code GlRenderSink} client <strong>MUST</strong> take immediate action to stop
         * the provided renderer. Failure to do so will make it impossible for the associated stream to close.
         *
         * @param renderer video stream renderer
         */
        void onRenderingMustStop(@NonNull Renderer renderer);

        /**
         * Notifies that the next video frame is ready to be rendered.
         *
         * @param renderer video stream renderer
         */
        default void onFrameReady(@NonNull Renderer renderer) {
        }

        /**
         * Notifies that the video stream content zone has changed.
         * <p>
         * Content zone defines the area where actual stream content is rendered, excluding padding introduced by any
         * configured {@link Renderer#setScaleType(Renderer.ScaleType) scale type}.
         *
         * @param contentZone new content zone
         */
        default void onContentZoneChange(@NonNull Rect contentZone) {
        }
    }

    /**
     * Creates a new {@code GlRenderSink} config.
     *
     * @param callback callback notified of sink events.
     *
     * @return a new {@code GlRenderSink} config.
     */
    @NonNull
    static Stream.Sink.Config config(@NonNull Callback callback) {
        return new GlRenderSinkCore.Config(callback);
    }
}
