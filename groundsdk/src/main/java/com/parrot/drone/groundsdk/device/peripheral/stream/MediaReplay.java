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

package com.parrot.drone.groundsdk.device.peripheral.stream;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.StreamServer;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaResourceCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.stream.MediaSourceCore;
import com.parrot.drone.groundsdk.stream.Replay;
import com.parrot.drone.groundsdk.stream.Stream;

/**
 * Media replay stream control interface.
 * <p>
 * Provides control over some remote media stream, allowing to control playback.
 * <p>
 * Every client that requests a {@link StreamServer#replay reference} on a media replay stream is given its own
 * dedicated instance of that stream. Multiple, independent stream may thus be open from the same media.
 * <p>
 * This stream does not support {@link Stream.State#SUSPENDED suspension}. When it gets interrupted because another
 * stream starts, or because streaming gets {@link StreamServer#enableStreaming(boolean) disabled} globally, then it
 * will move to the {@link Stream.State#STOPPED} {@link #state() state} and will remain in that state even once the
 * interrupting stream stops, or streaming gets enabled.
 * Also, this implies that this stream cannot be started while streaming is globally disabled.
 * <p>
 * The stream is stopped and released as soon as the reference that provides it is closed. All open sinks are closed as
 * a consequence.
 */
public interface MediaReplay extends Replay {

    /**
     * Creates a source for streaming a media resource.
     *
     * @param resource media resource to stream
     * @param track    media track to select for streaming
     *
     * @return a new {@code Source} instance, configured for streaming the specified media resource and track
     */
    @NonNull
    static Source videoTrackOf(@NonNull MediaItem.Resource resource, @NonNull MediaItem.Track track) {
        return new MediaSourceCore(MediaResourceCore.unwrap(resource), track);
    }

    /**
     * Identifies a source for media replay.
     */
    interface Source extends Parcelable {

        /**
         * Retrieves source {@link MediaItem} {@link MediaItem#getUid() identifier}.
         *
         * @return media identifier for this source
         */
        @NonNull
        String mediaUid();

        /**
         * Retrieves source {@link MediaItem.Resource} {@link MediaItem.Resource#getUid() identifier}.
         *
         * @return resource identifier for this source, {@code null} if this source streams a whole {@link MediaItem}.
         */
        @Nullable
        String resourceUid();

        /**
         * Retrieves source {@link MediaItem.Track}.
         *
         * @return selected media track for this source
         */
        @NonNull
        MediaItem.Track track();
    }

    /**
     * Informs about the configured source for this media replay stream.
     *
     * @return media replay source
     */
    @NonNull
    Source source();
}
