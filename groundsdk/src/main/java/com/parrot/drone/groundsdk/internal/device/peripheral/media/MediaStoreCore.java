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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.MediaStore;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.component.ComponentCore;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.session.Session;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Core class for the {@link MediaStore}. */
public final class MediaStoreCore extends ComponentCore {

    /** Description of MediaStore. */
    private static final ComponentDescriptor<Peripheral, MediaStore> DESC = ComponentDescriptor.of(MediaStore.class);

    /** Engine-specific backend for the MediaStore. */
    public interface Backend extends MediaThumbnailCache.Backend {

        /** Starts watching media store content. */
        void startWatchingContentChange();

        /** Stops watching media store content. */
        void stopWatchingContentChange();

        /**
         * Requests a list of the available media for a specific storage in the store.
         * <p>
         * {@code callback} is always called, either after success or failure. <br>
         * In case the callback is invoked directly by this method, then this method returns {@code null}. Otherwise
         * this method returns a {@code MediaRequest} object, which can be used to cancel the request, and means that
         * the callback will be invoked at a later time.
         *
         * @param storageType   targeted storage type
         * @param callback      callback notified when the list is available
         *
         * @return a request that can be canceled, or {@code null} if the request was processed directly
         */
        @Nullable
        MediaRequest browse(@Nullable MediaStore.StorageType storageType,
                            @NonNull MediaRequest.ResultCallback<List<? extends MediaItemCore>> callback);

        /**
         * Requests download of a media resource.
         * <p>
         * {@code callback} is always called, either after success or failure. <br>
         * In case the callback is invoked directly by this method, then this method returns {@code null}. Otherwise
         * this method returns a {@code MediaRequest} object, which can be used to cancel the request, and means that
         * the callback will be invoked at a later time.
         *
         * @param resource media resource to download
         * @param destDir  directory where the resource will be downloaded
         * @param callback callback notified of request progress and completion
         *
         * @return a request that can be canceled, or {@code null} if the request was processed directly
         */
        @Nullable
        MediaRequest download(@NonNull MediaResourceCore resource, @NonNull String destDir,
                              @NonNull MediaRequest.ProgressResultCallback<File> callback);

        /**
         * Requests deletion of a media item from device storage.
         * <p>
         * {@code callback} is always called, either after success or failure. <br>
         * In case the callback is invoked directly by this method, then this method returns {@code null}. Otherwise
         * this method returns a {@code MediaRequest} object, which can be used to cancel the request, and means that
         * the callback will be invoked at a later time.
         *
         * @param media    media item to delete
         * @param callback callback notified when the request completes, either successfully or with failure
         *
         * @return a request that can be canceled, or {@code null} if the request was processed directly
         */
        @Nullable
        MediaRequest delete(@NonNull MediaItemCore media, @NonNull MediaRequest.StatusCallback callback);

        /**
         * Requests deletion of a media resource from device storage.
         * <p>
         * {@code callback} is always called, either after success or failure. <br>
         * In case the callback is invoked directly by this method, then this method returns {@code null}. Otherwise
         * this method returns a {@code MediaRequest} object, which can be used to cancel the request, and means that
         * the callback will be invoked at a later time.
         *
         * @param resource resource to delete
         * @param callback callback notified when the request completes, either successfully or with failure
         *
         * @return a request that can be canceled, or {@code null} if the request was processed directly
         */
        @Nullable
        MediaRequest delete(@NonNull MediaResourceCore resource, @NonNull MediaRequest.StatusCallback callback);

        /**
         * Requests deletion of all media from the device storage.
         * <p>
         * {@code callback} is always called, either after success or failure. <br>
         * In case the callback is invoked directly by this method, then this method returns {@code null}. Otherwise
         * this method returns a {@code MediaRequest} object, which can be used to cancel the request, and means that
         * the callback will be invoked at a later time.
         *
         * @param callback callback notified when the request completes, either successfully or with failure
         *
         * @return a request that can be canceled, or {@code null} if the request was processed directly
         */
        @Nullable
        MediaRequest wipe(@NonNull MediaRequest.StatusCallback callback);
    }

    /**
     * Allows to observe store content changes.
     */
    interface Observer {

        /**
         * Called back when the content of the media store changes.
         */
        void onChanged();
    }

    /** Engine peripheral backend. */
    @NonNull
    final Backend mBackend;

    /** Media item thumbnails cache. */
    @NonNull
    final MediaThumbnailCache mMediaThumbnailCache;

    /** Media store observers, notified when the store content changes. */
    @NonNull
    private final List<Observer> mObservers;

    /** Indexing state of the store. */
    private MediaStore.IndexingState mIndexingState;

    /** Amount of picture media items in the store. */
    private int mPhotoMediaCount;

    /** Amount of video media items in the store. */
    private int mVideoMediaCount;

    /** Amount of picture resources in the store. */
    private int mPhotoResourceCount;

    /** Amount of video resources in the store. */
    private int mVideoResourceCount;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public MediaStoreCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
        mObservers = new CopyOnWriteArrayList<>();
        mIndexingState = MediaStore.IndexingState.UNAVAILABLE;
        mMediaThumbnailCache = new MediaThumbnailCache(mBackend, GroundSdkConfig.get().getThumbnailCacheSize());
    }

    @Override
    public void unpublish() {
        mIndexingState = MediaStore.IndexingState.UNAVAILABLE;
        mPhotoMediaCount = mVideoMediaCount = mPhotoResourceCount = mVideoResourceCount = 0;
        notifyObservers();
        mObservers.clear();
        mMediaThumbnailCache.clear();
        super.unpublish();
    }

    /**
     * Updates current indexing state.
     * <p>
     * Note that changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param state new indexing state
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public MediaStoreCore updateIndexingState(MediaStore.IndexingState state) {
        if (mIndexingState != state) {
            mIndexingState = state;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates current photo media count.
     * <p>
     * Note that changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param count new picture media count
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public MediaStoreCore updatePhotoMediaCount(int count) {
        if (mPhotoMediaCount != count) {
            mPhotoMediaCount = count;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates current video media count.
     * <p>
     * Note that changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param count new video media count
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public MediaStoreCore updateVideoMediaCount(int count) {
        if (mVideoMediaCount != count) {
            mVideoMediaCount = count;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates current photo resource count.
     * <p>
     * Note that changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param count new picture resource count
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public MediaStoreCore updatePhotoResourceCount(int count) {
        if (mPhotoResourceCount != count) {
            mPhotoResourceCount = count;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates current video resource count.
     * <p>
     * Note that changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param count new video resource count
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public MediaStoreCore updateVideoResourceCount(int count) {
        if (mVideoResourceCount != count) {
            mVideoResourceCount = count;
            mChanged = true;
        }
        return this;
    }

    /**
     * Retrieves the current indexing state.
     *
     * @return current indexing state
     */
    @NonNull
    MediaStore.IndexingState getIndexingState() {
        return mIndexingState;
    }

    /**
     * Retrieves the current picture media count.
     *
     * @return current picture media count
     */
    int getPhotoMediaCount() {
        return mPhotoMediaCount;
    }

    /**
     * Retrieves the current video media count.
     *
     * @return current video media count
     */
    int getVideoMediaCount() {
        return mVideoMediaCount;
    }

    /**
     * Retrieves the total amount of photo resources.
     *
     * @return total photo resource count
     */
    int getPhotoResourceCount() {
        return mPhotoResourceCount;
    }

    /**
     * Retrieves the total amount of video resources.
     *
     * @return total video resource count
     */
    int getVideoResourceCount() {
        return mVideoResourceCount;
    }

    /**
     * Registers an observer to be notified of store content changes.
     *
     * @param observer observer to register
     */
    void registerObserver(@NonNull Observer observer) {
        if (mObservers.isEmpty() & mObservers.add(observer)) {
            mBackend.startWatchingContentChange();
        }
    }

    /**
     * Unregisters an observer from being notified of store content changes.
     *
     * @param observer observer to unregister
     */
    void unregisterObserver(@NonNull Observer observer) {
        if (mObservers.remove(observer) && mObservers.isEmpty()) {
            mBackend.stopWatchingContentChange();
        }
    }

    @Override
    @NonNull
    protected MediaStore getProxy(@NonNull Session session) {
        return new MediaStoreProxy(session, this);
    }

    /**
     * Notifies all registered observers of a store content change.
     */
    public void notifyObservers() {
        for (Observer observer : mObservers) {
            observer.onChanged();
        }
    }
}
