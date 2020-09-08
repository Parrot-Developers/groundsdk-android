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

import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import android.util.SizeF;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * A Head Mounted Display device model.
 */
interface HmdModel {

    /**
     * Gives HMD model display name.
     *
     * @return HMD model display name
     */
    @NonNull
    String name();

    /**
     * Gives the size of the projection mesh used for this model.
     *
     * @return projection mesh size, in millimeters
     */
    float meshSize();

    /**
     * Gives the maximal allowed render size for each lens.
     *
     * @return lens maximal render size. Width and height values are in millimeters
     */
    @NonNull
    SizeF maxRenderSize();

    /**
     * Gives the default interpupillary distance used by this model.
     *
     * @return default interpupillary distance, in millimeters
     */
    float defaultIpd();

    /**
     * Chromatic aberration correction factors.
     */
    interface ChromaCorrection {

        /**
         * Gives chromatic aberration correction factor for red color channel.
         *
         * @return red channel correction factor
         */
        float red();

        /**
         * Gives chromatic aberration correction factor for green color channel.
         *
         * @return green channel correction factor
         */
        float green();

        /**
         * Gives chromatic aberration correction factor for blue color channel.
         *
         * @return blue channel correction factor
         */
        float blue();
    }

    /**
     * Gives chromatic aberration correction factors for this model.
     *
     * @return chromatic aberration correction factors
     */
    @NonNull
    ChromaCorrection chromaCorrection();

    /**
     * Loads mesh positions GL buffer.
     *
     * @return loaded buffer, or null in case loading failed
     */
    @Nullable
    ByteBuffer loadMeshPositions();

    /**
     * Loads texture coordinates GL buffer.
     *
     * @return loaded buffer, or null in case loading failed
     */
    @Nullable
    ByteBuffer loadTexCoords();

    /**
     * Loads color filter GL buffer.
     *
     * @return loaded buffer, or null in case loading failed
     */
    @Nullable
    ByteBuffer loadColorFilter();

    /**
     * Loads draw indices GL buffer.
     *
     * @return loaded buffer, or null in case loading failed
     */
    @Nullable
    ByteBuffer loadIndices();

    /**
     * A pack containing multiple HMD models.
     */
    interface DataPack {

        /**
         * Loads a model pack from an android raw resource.
         *
         * @param resources android resources
         * @param resId     resource to load the pack from
         *
         * @return a model pack loaded from the given resource
         */
        @NonNull
        static DataPack fromResources(@NonNull Resources resources, @RawRes int resId) {
            return new ResourceHmdModelPack(resources, resId);
        }

        /**
         * Loads a HMD model from the pack.
         *
         * @param id identifies the model to load
         *
         * @return loaded model
         *
         * @throws IllegalArgumentException in case no such model exists in the pack
         */
        @NonNull
        HmdModel loadModel(@NonNull String id);

        /**
         * Lists all available models.
         *
         * @return a set of all available model identifiers.
         */
        @NonNull
        Set<String> listModels();
    }
}
