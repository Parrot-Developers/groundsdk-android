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

import com.parrot.drone.groundsdk.arsdkengine.http.HttpMediaItem;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;

/**
 * Utility class to adapt {@link HttpMediaItem.Resource.Format drone http} to {@link MediaItem.Resource.Format
 * groundsdk} resource formats.
 */
final class FormatAdapter {

    /**
     * Converts an {@code HttpMediaItem.Resource.Format} to its {@code MediaItem.Resource.Format} equivalent.
     *
     * @param format http resource format to convert
     *
     * @return the groundsdk media resource format equivalent
     */
    @NonNull
    static MediaItem.Resource.Format from(@NonNull HttpMediaItem.Resource.Format format) {
        switch (format) {
            case JPG:
                return MediaItem.Resource.Format.JPG;
            case DNG:
                return MediaItem.Resource.Format.DNG;
            case MP4:
                return MediaItem.Resource.Format.MP4;
        }
        return null;
    }

    /**
     * Private constructor for static utility class.
     */
    private FormatAdapter() {
    }
}
