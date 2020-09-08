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

package com.parrot.drone.groundsdk.device.peripheral;

import android.graphics.Bitmap;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDeleter;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDownloader;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaStoreWiper;

import java.util.Collection;
import java.util.List;

/**
 * MediaStore peripheral interface for {@link com.parrot.drone.groundsdk.device.Drone} devices.
 * <p>
 * Aggregates information on all medias stored on a device, allowing the application to browse such media, to
 * delete them physically from the drone where they are stored, as well as to download them locally on the device from
 * the drone where they are stored.
 * <p>
 * This peripheral can be obtained from a {@code Drone} using:
 * <pre>{@code drone.getPeripheral(MediaStore.class)}</pre>
 *
 * @see Peripheral.Provider#getPeripheral(Class)
 * @see Peripheral.Provider#getPeripheral(Class, Ref.Observer)
 */
public interface MediaStore extends Peripheral {

    /** Indexing state of the media store. */
    enum IndexingState {

        /** The media store is not available yet. */
        UNAVAILABLE,

        /** The media store is indexing. */
        INDEXING,

        /** The media store is indexed and ready to be used. */
        INDEXED
    }

    /** Storage type. */
    enum StorageType {

        /** Internal storage. */
        INTERNAL,

        /** Removable user storage (usually a sdcard). */
        REMOVABLE
    }

    /**
     * Retrieves the current indexing state of the media store.
     *
     * @return current indexing state
     */
    @NonNull
    IndexingState getIndexingState();

    /**
     * Retrieves the total amount of photo media items in the media store.
     *
     * @return total photo count
     */
    @IntRange(from = 0)
    int getPhotoMediaCount();

    /**
     * Retrieves the total amount of video media items in the media store.
     *
     * @return total video count
     */
    @IntRange(from = 0)
    int getVideoMediaCount();

    /**
     * Retrieves the total amount of photo resources in the media store.
     *
     * @return total photo resource count
     */
    @IntRange(from = 0)
    int getPhotoResourceCount();

    /**
     * Retrieves the total amount of video resources in the media store.
     *
     * @return total video resource count
     */
    @IntRange(from = 0)
    int getVideoResourceCount();

    /**
     * Creates a new media list for browsing media on every storage of the drone.
     * <p>
     * This is an asynchronous operation. The provided observer is notified with the resulting list of media items
     * when it has been first loaded and each time the content changes. <br>
     * This list may be closed or the operation may be aborted early by {@link Ref#close() closing} the returned
     * reference.
     *
     * @param observer observer notified when the media list has been loaded and when its content changes
     *
     * @return a reference on a list of {@link MediaItem}
     */
    @NonNull
    Ref<List<MediaItem>> browse(@NonNull Ref.Observer<List<MediaItem>> observer);

    /**
     * Creates a new media list for browsing media on a specific storage.
     * <p>
     * This is an asynchronous operation. The provided observer is notified with the resulting list of media items
     * when it has been first loaded and each time the content changes. <br>
     * This list may be closed or the operation may be aborted early by {@link Ref#close() closing} the returned
     * reference.
     *
     * @param storageType   targeted storage type
     * @param observer      observer notified when the media list has been loaded and when its content changes
     *
     * @return a reference on a list of {@link MediaItem}
     */
    @NonNull
    Ref<List<MediaItem>> browse(@NonNull StorageType storageType, @NonNull Ref.Observer<List<MediaItem>> observer);

    /**
     * Retrieves a media thumbnail.
     * <p>
     * This is an asynchronous operation. The provided observer is notified when the media thumbnail is available.<br>
     * The operation may be aborted early by {@link Ref#close() closing} the returned reference.
     * <p>
     * Please note that media thumbnails are internally cached by GroundSdk. As a consequence, the application must be
     * prepared to receive the thumbnail in the provided observer has soon as this method is called, since the
     * thumbnail may be directly available from the cache.
     * <p>
     * <strong>IMPORTANT:</strong> The provided thumbnail is an android {@link Bitmap} that <strong>MUST NOT</strong>
     * be {@link Bitmap#recycle() recycled} by the application.
     *
     * @param media    media item whose thumbnail must be downloaded
     * @param observer observer notified when the media thumbnail is available
     *
     * @return a reference on the thumbnail bitmap
     */
    @NonNull
    Ref<Bitmap> fetchThumbnailOf(@NonNull MediaItem media, @NonNull Ref.Observer<Bitmap> observer);

    /**
     * Retrieves a resource thumbnail.
     * <p>
     * This is an asynchronous operation. The provided observer is notified when the resource thumbnail is available.
     * <br>
     * The operation may be aborted early by {@link Ref#close() closing} the returned reference.
     * <p>
     * Please note that resource thumbnails are internally cached by GroundSdk. As a consequence, the application must
     * be prepared to receive the thumbnail in the provided observer has soon as this method is called, since the
     * thumbnail may be directly available from the cache.
     * <p>
     * <strong>IMPORTANT:</strong> The provided thumbnail is an android {@link Bitmap} that <strong>MUST NOT</strong>
     * be {@link Bitmap#recycle() recycled} by the application.
     *
     * @param resource media resource whose thumbnail must be downloaded
     * @param observer observer notified when the resource thumbnail is available
     *
     * @return a reference on the thumbnail bitmap
     */
    @NonNull
    Ref<Bitmap> fetchThumbnailOf(@NonNull MediaItem.Resource resource, @NonNull Ref.Observer<Bitmap> observer);

    /**
     * Downloads media resources from the device's internal storage.
     * <p>
     * This is an asynchronous operation. The provided observer is notified with a {@link MediaDownloader} object that
     * reports current download progress. <br>
     * The operation may be aborted by {@link Ref#close() closing} the returned reference.
     * <p>
     * Resources belonging to the same media will be grouped together and downloaded sequentially; aside from these
     * grouping requirements, resources will be downloaded in the order defined by the specified {@code resources}.
     *
     * @param resources   media resources to download
     * @param destination destination where the resource files must be downloaded
     * @param observer    observer notified on download progress and status
     *
     * @return a reference on {@code MediaDownloader} that allows to track download progress
     */
    @NonNull
    Ref<MediaDownloader> download(@NonNull Collection<MediaItem.Resource> resources,
                                  @NonNull MediaDestination destination,
                                  @NonNull Ref.Observer<MediaDownloader> observer);

    /**
     * Deletes resources from the device's internal storage.
     * <p>
     * This is an asynchronous operation. The provided observer is notified with a {@link MediaDeleter} object that
     * reports current deletion progress. <br>
     * The operation may be aborted by {@link Ref#close() closing} the returned reference.
     * <p>
     * Resources belonging to the same media will be grouped together and deleted sequentially; aside from these
     * grouping requirements, resources will be deleted in the order defined by the specified {@code resources}.
     *
     * @param resources media resources to delete
     * @param observer  observer notified of deletion progress and status
     *
     * @return a reference on a {@code MediaDeleter} that allows to track deletion progress
     */
    @NonNull
    Ref<MediaDeleter> delete(@NonNull Collection<MediaItem.Resource> resources,
                             @NonNull Ref.Observer<MediaDeleter> observer);

    /**
     * Wipes all media from the device's internal storage.
     * <p>
     * This is an asynchronous operation. The provided observer is notified with a {@link MediaStoreWiper} object that
     * reports current deletion status. <br>
     * The operation may be aborted by {@link Ref#close() closing} the returned reference.
     *
     * @param observer observer notified of deletion status
     *
     * @return a reference on a {@code MediaStoreWiper} that allows to track deletion status
     */
    @NonNull
    Ref<MediaStoreWiper> wipe(@NonNull Ref.Observer<MediaStoreWiper> observer);
}