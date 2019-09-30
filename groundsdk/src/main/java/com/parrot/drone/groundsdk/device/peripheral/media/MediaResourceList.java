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

package com.parrot.drone.groundsdk.device.peripheral.media;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaItemCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaResourceCore;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A parcelable list of media resources.
 * <p>
 * Allows to write some collection of media resources to an android {@link Parcel} in an optimized manner; see
 * {@link MediaItem.Resource} for an explanation why it is more efficient to use this class to serialize multiple
 * resources into a {@code Parcel}.
 */
public final class MediaResourceList extends AbstractList<MediaItem.Resource> implements Parcelable {

    /** Contained media resources. */
    @NonNull
    private final List<MediaItem.Resource> mResources;

    /**
     * Constructor.
     * <p>
     * Creates an empty {@code MediaResourceList} instance
     */
    public MediaResourceList() {
        mResources = new ArrayList<>();
    }

    /**
     * Constructor.
     * <p>
     * Creates an {@code MediaResourceList} instance containing all resources from the provided collection
     *
     * @param resources collection of resources to place in the created {@code MediaResourceList}
     */
    public MediaResourceList(@NonNull Collection<? extends MediaItem.Resource> resources) {
        mResources = new ArrayList<>(resources);
    }

    @Override
    public MediaItem.Resource get(@IntRange(from = 0) int index) {
        return mResources.get(index);
    }

    @Override
    public MediaItem.Resource set(@IntRange(from = 0) int index, @NonNull MediaItem.Resource element) {
        return mResources.set(index, element);
    }

    @Override
    public boolean add(@NonNull MediaItem.Resource resource) {
        return mResources.add(resource);
    }

    @Override
    @IntRange(from = 0)
    public int size() {
        return mResources.size();
    }

    //region Parcelable

    /**
     * Parcel deserializer.
     */
    public static final Creator<MediaResourceList> CREATOR = new Creator<MediaResourceList>() {

        @Override
        @NonNull
        public MediaResourceList createFromParcel(@NonNull Parcel src) {
            return new MediaResourceList(src);
        }

        @Override
        @NonNull
        public MediaResourceList[] newArray(int size) {
            return new MediaResourceList[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dst, int flags) {
        Map<MediaItemCore, Set<MediaResourceCore>> resourcesByMedia = MediaResourceCore.unwrapAsMap(mResources);
        dst.writeInt(resourcesByMedia.size());
        resourcesByMedia.forEach((media, resources) -> {
            dst.writeParcelable(media, flags);
            BitSet mask = new BitSet();
            resources.forEach(it -> mask.set(media.getResources().indexOf(it)));
            dst.writeByteArray(mask.toByteArray());
        });
    }

    /**
     * Constructor for parcel deserialization.
     *
     * @param src parcel to deserialize from
     */
    private MediaResourceList(@NonNull Parcel src) {
        mResources = new ArrayList<>();
        for (int i = 0, N = src.readInt(); i < N; i++) {
            MediaItem media = src.readParcelable(getClass().getClassLoader());
            assert media != null;
            BitSet.valueOf(src.createByteArray())
                  .stream()
                  .mapToObj(index -> media.getResources().get(index))
                  .forEach(mResources::add);
        }
    }

    //endregion
}
