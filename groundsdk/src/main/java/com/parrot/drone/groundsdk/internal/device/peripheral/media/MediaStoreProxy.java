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

package com.parrot.drone.groundsdk.internal.device.peripheral.media;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.peripheral.MediaStore;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDeleter;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDownloader;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaStoreWiper;
import com.parrot.drone.groundsdk.internal.session.Session;

import java.util.Collection;
import java.util.List;

/**
 * Implementation of the MediaStore interface which delegates calls to an underlying MediaStoreCore.
 */
class MediaStoreProxy implements MediaStore {

    /** Session managing the lifecycle of all refs issued by this proxy instance. */
    @NonNull
    private final Session mSession;

    /** MediaStoreCore delegate. */
    @NonNull
    private final MediaStoreCore mStore;

    /**
     * Constructor.
     *
     * @param session  session that will manage issued refs
     * @param delegate media store delegate to forward calls to
     */
    MediaStoreProxy(@NonNull Session session, @NonNull MediaStoreCore delegate) {
        mSession = session;
        mStore = delegate;
    }

    @NonNull
    @Override
    public IndexingState getIndexingState() {
        return mStore.getIndexingState();
    }

    @Override
    public int getPhotoMediaCount() {
        return mStore.getPhotoMediaCount();
    }

    @Override
    public int getVideoMediaCount() {
        return mStore.getVideoMediaCount();
    }

    @Override
    public int getPhotoResourceCount() {
        return mStore.getPhotoResourceCount();
    }

    @Override
    public int getVideoResourceCount() {
        return mStore.getVideoResourceCount();
    }

    @NonNull
    @Override
    public Ref<List<MediaItem>> browse(@NonNull Ref.Observer<List<MediaItem>> observer) {
        return new MediaListRef(mSession, observer, mStore, null);
    }

    @NonNull
    @Override
    public Ref<List<MediaItem>> browse(@NonNull StorageType storageType,
                                       @NonNull Ref.Observer<List<MediaItem>> observer) {
        return new MediaListRef(mSession, observer, mStore, storageType);
    }

    @NonNull
    @Override
    public Ref<Bitmap> fetchThumbnailOf(@NonNull MediaItem media, @NonNull Ref.Observer<Bitmap> observer) {
        return new MediaThumbnailRef(mSession, observer, mStore.mMediaThumbnailCache,
                ThumbnailProvider.wrap(MediaItemCore.unwrap(media)));
    }

    @NonNull
    @Override
    public Ref<Bitmap> fetchThumbnailOf(@NonNull MediaItem.Resource resource, @NonNull Ref.Observer<Bitmap> observer) {
        return new MediaThumbnailRef(mSession, observer, mStore.mMediaThumbnailCache,
                ThumbnailProvider.wrap(MediaResourceCore.unwrap(resource)));
    }

    @NonNull
    @Override
    public Ref<MediaDeleter> delete(@NonNull Collection<MediaItem.Resource> resources,
                                    @NonNull Ref.Observer<MediaDeleter> observer) {
        return new MediaDeleterRef(mSession, observer, resources, mStore);
    }


    @NonNull
    @Override
    public Ref<MediaDownloader> download(@NonNull Collection<MediaItem.Resource> resources,
                                         @NonNull MediaDestination destination,
                                         @NonNull Ref.Observer<MediaDownloader> observer) {
        return new MediaDownloaderRef(mSession, observer, resources, MediaDestinationCore.unwrap(destination),
                mStore);
    }


    @NonNull
    @Override
    public Ref<MediaStoreWiper> wipe(@NonNull Ref.Observer<MediaStoreWiper> observer) {
        return new MediaStoreWiperRef(mSession, observer, mStore);
    }
}
