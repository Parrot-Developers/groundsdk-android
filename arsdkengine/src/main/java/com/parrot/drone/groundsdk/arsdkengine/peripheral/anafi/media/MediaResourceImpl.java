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

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.http.HttpMediaItem;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaResourceCore;
import com.parrot.drone.sdkcore.stream.SdkCoreTracks;

import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;

/**
 * Implementation of {@code MediaResourceCore} over {@link HttpMediaItem.Resource}.
 */
final class MediaResourceImpl implements MediaResourceCore {

    /**
     * Unwraps a media resource to its internal {@code MediaResourceImpl} representation.
     * <p>
     * This method assumes that the specified media resource is based upon {@code MediaResourceImpl}.
     *
     * @param resource media resource to unwrap
     *
     * @return internal {@code MediaResourceImpl} representation of the specified media resource
     */
    @NonNull
    static MediaResourceImpl unwrap(@NonNull MediaResourceCore resource) {
        return (MediaResourceImpl) resource;
    }

    /** Media item parent of this resource. */
    @NonNull
    private final MediaItemImpl mMedia;

    /** HTTP media resource that backs this media. */
    @NonNull
    private final HttpMediaItem.Resource mHttpResource;

    /** Available metadata types for this resource. */
    @NonNull
    private final EnumSet<MediaItem.MetadataType> mMetadataTypes;

    /** Available stream track identifiers, by track kind. */
    @NonNull
    private final EnumMap<MediaItem.Track, String> mTracks;

    /**
     * Constructor.
     *
     * @param media    media item parent
     * @param resource HTTP media resource backend
     */
    MediaResourceImpl(@NonNull MediaItemImpl media, @NonNull HttpMediaItem.Resource resource) {
        if (!resource.isValid()) {
            throw new IllegalArgumentException("Invalid HttpMediaItem.Resource: " + resource);
        }

        mMedia = media;
        mHttpResource = resource;

        mMetadataTypes = EnumSet.noneOf(MediaItem.MetadataType.class);

        boolean thermal = mHttpResource.hasThermalMetadata();
        if (thermal) {
            mMetadataTypes.add(MediaItem.MetadataType.THERMAL);
        }

        mTracks = new EnumMap<>(MediaItem.Track.class);
        if (mHttpResource.getStreamUrl() != null) {
            mTracks.put(MediaItem.Track.DEFAULT_VIDEO, null);
            if (thermal) {
                mTracks.put(MediaItem.Track.THERMAL_UNBLENDED, SdkCoreTracks.TRACK_THERMAL_UNBLENDED);
            }
        }
    }

    @NonNull
    @Override
    public String getUid() {
        assert mHttpResource.getId() != null;
        return mHttpResource.getId();
    }

    @NonNull
    @Override
    public MediaItemImpl getMedia() {
        return mMedia;
    }

    @NonNull
    @Override
    public Format getFormat() {
        assert mHttpResource.getFormat() != null;
        return FormatAdapter.from(mHttpResource.getFormat());
    }

    @Override
    public long getSize() {
        return mHttpResource.getSize();
    }

    @Override
    public long getDuration() {
        return mHttpResource.getDuration();
    }

    @NonNull
    @Override
    public Date getCreationDate() {
        assert mHttpResource.getDate() != null;
        return new Date(mHttpResource.getDate().getTime());
    }

    @Nullable
    @Override
    public Location getLocation() {
        HttpMediaItem.Location location = mHttpResource.getLocation();
        return location == null ? null : LocationAdapter.from(location);
    }

    @NonNull
    @Override
    public EnumSet<MediaItem.MetadataType> getAvailableMetadata() {
        return EnumSet.copyOf(mMetadataTypes);
    }

    @NonNull
    @Override
    public EnumSet<MediaItem.Track> getAvailableTracks() {
        return mTracks.isEmpty() ? EnumSet.noneOf(MediaItem.Track.class) : EnumSet.copyOf(mTracks.keySet());
    }

    @Nullable
    @Override
    public String getStreamUrl() {
        return mHttpResource.getStreamUrl();
    }

    @Nullable
    @Override
    public String getStreamTrackIdFor(@NonNull MediaItem.Track track) {
        if (mTracks.containsKey(track)) {
            return mTracks.get(track);
        }
        throw new IllegalArgumentException("No such track in resource [track: " + track + ", uid: " + getUid() + "]");
    }

    /**
     * Retrieves the URL to use to download this resource.
     *
     * @return resource download URL
     */
    @NonNull
    String getDownloadUrl() {
        assert mHttpResource.getUrl() != null;
        return mHttpResource.getUrl();
    }

    /**
     * Retrieves the URL to use to fetch the thumbnail for this resource.
     *
     * @return thumbnail URL, if available, otherwise {@code null}
     */
    @Nullable
    String getThumbnailUrl() {
        return mHttpResource.getThumbnailUrl();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaResourceImpl resource = (MediaResourceImpl) o;
        return getUid().equals(resource.getUid());
    }

    @Override
    public int hashCode() {
        return getUid().hashCode();
    }

    //region Parcelable

    /**
     * Parcel deserializer.
     */
    public static final Parcelable.Creator<MediaResourceImpl> CREATOR = new Parcelable.Creator<MediaResourceImpl>() {

        @Override
        public MediaResourceImpl createFromParcel(@NonNull Parcel src) {
            MediaItemImpl media = src.readTypedObject(MediaItemImpl.CREATOR);
            return media == null ? null : media.getResources().get(src.readInt());
        }

        @Override
        public MediaResourceImpl[] newArray(int size) {
            return new MediaResourceImpl[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dst, int flags) {
        dst.writeTypedObject(mMedia, flags);
        dst.writeInt(mMedia.getResources().indexOf(this));
    }

    //endregion
}
