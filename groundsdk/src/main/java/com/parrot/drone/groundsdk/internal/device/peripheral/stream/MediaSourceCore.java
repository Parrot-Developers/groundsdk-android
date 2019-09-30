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

package com.parrot.drone.groundsdk.internal.device.peripheral.stream;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.device.peripheral.stream.MediaReplay;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaResourceCore;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;

import java.util.Objects;

/** Core class for {@link MediaReplay.Source}. */
public final class MediaSourceCore implements MediaReplay.Source {

    /** Source media item uid. */
    @NonNull
    private final String mMediaUid;

    /** Source media resource uid, {@code null} when the source represents a whole media item. */
    @Nullable
    private final String mResourceUid;

    /** Source media track. */
    @NonNull
    private final MediaItem.Track mTrack;

    /** Source stream URL. */
    @NonNull
    private final String mStreamUrl;

    /** Source stream track identifier. {@code null} is a valid track identifier for the default track. */
    @Nullable
    private final String mStreamTrackId;

    /**
     * Constructor for streaming a single media resource.
     *
     * @param resource resource to stream
     * @param track    track to select for streaming
     */
    public MediaSourceCore(@NonNull MediaResourceCore resource, @NonNull MediaItem.Track track) {
        String streamUrl = resource.getStreamUrl();
        if (streamUrl == null) {
            throw new IllegalArgumentException("Resource cannot be streamed [uid: " + resource.getUid() + "]");
        }

        mMediaUid = resource.getMedia().getUid();
        mResourceUid = resource.getUid();
        mTrack = track;
        mStreamUrl = streamUrl;
        mStreamTrackId = resource.getStreamTrackIdFor(track);
    }

    @NonNull
    @Override
    public String mediaUid() {
        return mMediaUid;
    }

    @Nullable
    @Override
    public String resourceUid() {
        return mResourceUid;
    }

    @NonNull
    @Override
    public MediaItem.Track track() {
        return mTrack;
    }

    /**
     * Opens a stream for this source.
     *
     * @param server server that provides and manages the stream
     * @param client stream client, notified of stream lifecycle events
     *
     * @return a new {@code SdkCoreStream} instance if successful, otherwise {@code null}
     */
    @Nullable
    SdkCoreStream openStream(@NonNull StreamServerCore server, @NonNull SdkCoreStream.Client client) {
        return server.openStream(mStreamUrl, mStreamTrackId, client);
    }

    //region Parcelable

    /** Parcelable static factory. */
    @NonNull
    public static final Creator<MediaReplay.Source> CREATOR = new Creator<MediaReplay.Source>() {

        @Override
        @NonNull
        public MediaReplay.Source createFromParcel(@NonNull Parcel src) {
            return new MediaSourceCore(src);
        }

        @Override
        @NonNull
        public MediaReplay.Source[] newArray(int size) {
            return new MediaSourceCore[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dst, int flags) {
        dst.writeString(mMediaUid);
        dst.writeString(mResourceUid);
        dst.writeInt(mTrack.ordinal());
        dst.writeString(mStreamUrl);
        dst.writeString(mStreamTrackId);
    }

    /**
     * Constructor for Parcel deserialization.
     *
     * @param src parcel to deserialize from
     */
    private MediaSourceCore(@NonNull Parcel src) {
        mMediaUid = Objects.requireNonNull(src.readString());
        mResourceUid = src.readString();
        mTrack = MediaItem.Track.values()[src.readInt()];
        mStreamUrl = Objects.requireNonNull(src.readString());
        mStreamTrackId = src.readString();
    }

    //endregion
}
