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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.media;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.MediaStore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureMediastore;

/**
 * Utility class to adapt {@link ArsdkFeatureMediastore.State arsdk} to {@link MediaStore.IndexingState groundsdk} media
 * store indexing states.
 */
final class IndexingStateAdapter {

    /**
     * Converts an {@code ArsdkFeatureMediastore.State} to its {@code MediaStore.IndexingState} equivalent.
     *
     * @param state arsdk indexing state to convert
     *
     * @return the groundsdk media store indexing state equivalent
     */
    @NonNull
    static MediaStore.IndexingState from(@NonNull ArsdkFeatureMediastore.State state) {
        switch (state) {
            case NOT_AVAILABLE:
                return MediaStore.IndexingState.UNAVAILABLE;
            case INDEXING:
                return MediaStore.IndexingState.INDEXING;
            case INDEXED:
                return MediaStore.IndexingState.INDEXED;
        }
        return null;
    }

    /**
     * Private constructor for static utility class.
     */
    private IndexingStateAdapter() {
    }
}
