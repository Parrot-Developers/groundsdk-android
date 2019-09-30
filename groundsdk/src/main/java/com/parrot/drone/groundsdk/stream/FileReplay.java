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

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.internal.stream.FileSourceCore;

import java.io.File;

/**
 * Local file replay stream control interface.
 * <p>
 * Provides control over some local file stream, allowing to control playback.
 * <p>
 * Every client that requests a {@link GroundSdk#replay reference} on a local file replay stream is given its own
 * dedicated instance of that stream. Multiple, independent stream may thus be open from the same file.
 * <p>
 * The stream is stopped and released as soon as the reference that provides it is closed. All open sinks are closed as
 * a consequence.
 */
public interface FileReplay extends Replay {

    /** Name for the thermal unblended video track in thermal media files. */
    String TRACK_THERMAL_UNBLENDED = FileSourceCore.TRACK_THERMAL_UNBLENDED;

    /**
     * Creates a source for streaming a specific video track from a local file.
     *
     * @param file      local file to stream
     * @param trackName name of the track to select for streaming
     *
     * @return a new {@code Source} instance, configured for streaming the specified local file and track
     */
    @NonNull
    static Source videoTrackOf(@NonNull File file, @NonNull String trackName) {
        return new FileSourceCore(file, trackName);
    }

    /**
     * Creates a source for streaming the default video track from a local file.
     *
     * @param file local file to stream
     *
     * @return a new {@code Source} instance, configured for streaming the default video track from the specified local
     *         file
     */
    @NonNull
    static Source defaultVideoTrackOf(@NonNull File file) {
        return new FileSourceCore(file, null);
    }

    /**
     * Identifies a source for media replay.
     */
    interface Source extends Parcelable {

        /**
         * Retrieves source local file.
         *
         * @return local file for this source
         */
        @NonNull
        File file();

        /**
         * Retrieves selected track for this source.
         *
         * @return name of selected track for this source, {@code null} for default track
         */
        @Nullable
        String trackName();
    }

    /**
     * Informs about the configured source for this file replay stream.
     *
     * @return file replay source
     */
    @NonNull
    Source source();
}
