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
import androidx.annotation.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An LRU cache of media thumbnails.
 */
final class MediaThumbnailCache {

    /**
     * A request for a thumbnail, that may be canceled.
     */
    interface ThumbnailRequest {

        /**
         * Allows to be notified when the thumbnail has been fetched.
         */
        interface Callback {

            /**
             * Called back when the requested thumbnail is available.
             * <p>
             * May be called immediately if the cache already contains the thumbnail
             *
             * @param thumbnail requested thumbnail, {@code null} if there is no thumbnail for the requested item
             */
            void onThumbnailAvailable(@Nullable Bitmap thumbnail);
        }

        /**
         * Cancels the request. Callback won't be called.
         */
        void cancel();
    }

    /** Backend providing thumbnail fetch primitives. */
    interface Backend {

        /**
         * Requests a media item thumbnail
         * <p>
         * {@code callback} is always called, either after success or failure. <br>
         * In case the callback is invoked directly by this method, then this method returns {@code null}. Otherwise
         * this method returns a {@code MediaRequest} object, which can be used to cancel the request, and means that
         * the callback will be invoked at a later time.
         * <p>
         * Note that even in case of {@link MediaRequest.Status#SUCCESS}, the provided bitmap may be {@code null},
         * which means that it is known that this media does not provide a thumbnail.
         *
         * @param media    media providing the thumbnail
         * @param callback callback notified of request result
         *
         * @return a request that can be canceled, or {@code null} if the request was processed directly
         */
        @Nullable
        MediaRequest fetchThumbnail(@NonNull MediaItemCore media,
                                    @NonNull MediaRequest.ResultCallback<Bitmap> callback);

        /**
         * Requests a media resource thumbnail
         * <p>
         * {@code callback} is always called, either after success or failure. <br>
         * In case the callback is invoked directly by this method, then this method returns {@code null}. Otherwise
         * this method returns a {@code MediaRequest} object, which can be used to cancel the request, and means that
         * the callback will be invoked at a later time.
         * <p>
         * Note that even in case of {@link MediaRequest.Status#SUCCESS}, the provided bitmap may be {@code null},
         * which means that it is known that this media does not provide a thumbnail.
         *
         * @param resource resource providing the thumbnail
         * @param callback callback notified of request result
         *
         * @return a request that can be canceled, or {@code null} if the request was processed directly
         */
        @Nullable
        MediaRequest fetchThumbnail(@NonNull MediaResourceCore resource,
                                    @NonNull MediaRequest.ResultCallback<Bitmap> callback);
    }

    /** Cache entries, by corresponding media item. */
    @NonNull
    private final LinkedHashMap<ThumbnailProvider, Entry> mCache;

    /** Backend allowing to fetch thumbnails. */
    @NonNull
    private final Backend mBackend;

    /** Rest of media requests to be processed. Does not contain the item being currently processed. */
    @NonNull
    private final Queue<ThumbnailProvider> mPendingRequests;

    /**
     * Maximum size of the cache, in bytes. If the cache grows above this limit, eldest cache entries that contains
     * a bitmap are pruned until the size gets below this limit.
     */
    private final long mCacheMaxSize;

    /** Current size of the cache. This is the sum of the size of the bitmap in each completed cache entry. */
    private long mCacheSize;

    /** Current thumbnail request being processed. */
    @Nullable
    private MediaRequest mCurrentRequest;

    /**
     * Constructor.
     *
     * @param backend   backend allowing to fetch thumbnails
     * @param cacheSize maximum cache size, in bytes
     */
    MediaThumbnailCache(@NonNull Backend backend, long cacheSize) {
        mBackend = backend;
        mCache = new LinkedHashMap<>(); // with insertion order
        mCacheMaxSize = cacheSize;
        mPendingRequests = new LinkedList<>();
    }

    /**
     * Clears the cache, discards all pending requests and cancels any ongoing request.
     */
    void clear() {
        mPendingRequests.clear();
        // complete pending entries to signal listeners that they won't get any thumbnail
        for (Entry entry : mCache.values()) {
            entry.complete(null);
        }
        mCache.clear();
        mCacheSize = 0;
        if (mCurrentRequest != null) {
            mCurrentRequest.cancel();
            mCurrentRequest = null;
        }
    }

    /**
     * Gets a thumbnail from the cache.
     * <p>
     * If the thumbnail bitmap is present in the corresponding cache entry, the callback is immediately passed the
     * thumbnail. <br>
     * Otherwise, the callback is remembered in the associated cache entry to be notified later, once the request
     * for this thumbnail completes. If no such request exists yet, it is created and queued in the list
     * of requests to be processed.
     *
     * @param provider media or resource providing the thumbnail
     * @param callback callback to notify once the thumbnail is available.
     *
     * @return a request for a thumbnail, that can be canceled.
     */
    @Nullable
    ThumbnailRequest getThumbnail(@NonNull ThumbnailProvider provider, @NonNull ThumbnailRequest.Callback callback) {
        Entry entry = mCache.remove(provider);
        if (entry == null) {
            entry = new Entry(provider);
        }

        mCache.put(provider, entry); // make it a recently accessed cache entry
        return entry.addRequest(callback);
    }

    /**
     * An entry in the cache.
     * <p>
     * An entry contains either: <ul>
     * <li>A non-null list of callbacks, which means that the thumbnail bitmap for this cache entry has
     * not been fetched yet. A request exists in the list of pending request or is currently being processed to
     * obtain the thumbnail bitmap.</li>
     * <li>A null list of callbacks, which means that the thumbnail bitmap for this cache entry has
     * been fetched already. At that point the cache entry is considered valid. The bitmap may be {@code null}
     * if the request failed, but it will be returned for any request on the corresponding media item. </li>
     * </ul>
     */
    private final class Entry {

        /** Callbacks to notify when the entry completes. {@code null} when the corresponding request has completed. */
        @Nullable
        private List<ThumbnailRequest.Callback> mCallbacks;

        /** The media or resource this entry caches a thumbnail for. */
        private final ThumbnailProvider mProvider;

        /** Thumbnail bitmap, {@code null} if the thumbnail request has not been processed yet or failed. */
        @Nullable
        private Bitmap mThumbnail;

        /**
         * Constructor.
         *
         * @param provider media or resource providing the thumbnail
         */
        Entry(@NonNull ThumbnailProvider provider) {
            mProvider = provider;
            mCallbacks = new CopyOnWriteArrayList<>();
        }

        /**
         * Completes the entry.
         * <p>
         * This marks the entry valid. Any request on the corresponding media item will be given the entry bitmap,
         * even if the latter is {@code null}.
         *
         * @param thumbnail the thumbnail bitmap for this entry
         */
        void complete(@Nullable Bitmap thumbnail) {
            if (mCallbacks != null) {
                mThumbnail = thumbnail;
                for (ThumbnailRequest.Callback callback : mCallbacks) {
                    callback.onThumbnailAvailable(mThumbnail);
                }
                mCallbacks = null; // this is over, we won't create requests for this entry anymore
            }
        }

        /**
         * Tells whether an entry may be pruned from the cache.
         * <p>
         * An entry may be pruned if either it has completed, or it has empty callbacks (which means nobody will miss
         * this entry).
         *
         * @return {@code true} if the entry is prunable, otherwise {@code false}
         */
        boolean isPrunable() {
            return mCallbacks == null || mCallbacks.isEmpty();
        }

        /**
         * Gets the size of this cache entry.
         *
         * @return cache entry size
         */
        int size() {
            return mThumbnail == null ? 0 : mThumbnail.getAllocationByteCount();
        }

        /**
         * Registers a callback to be notified when the cache entry completes.
         *
         * @param callback callback to notify when the entry gets completed
         *
         * @return a request that, when canceled, will remove the callback from the list of callbacks to be notified
         */
        @Nullable
        ThumbnailRequest addRequest(@NonNull ThumbnailRequest.Callback callback) {
            ThumbnailRequest request = null;
            if (mCallbacks == null) {
                callback.onThumbnailAvailable(mThumbnail);
            } else {
                if (mCallbacks.isEmpty() & mCallbacks.add(callback)) {
                    mPendingRequests.add(mProvider);
                    processNextRequest();
                }
                request = () -> {
                    if (mCallbacks != null && mCallbacks.remove(callback) && mCallbacks.isEmpty()) {
                        // nobody is interested anymore: remove pending request
                        mPendingRequests.remove(mProvider);
                    }
                };
            }

            return request;
        }
    }

    /**
     * Processes the next pending request, if there isn't a request being process currently.
     */
    private void processNextRequest() {
        if (mCurrentRequest == null && !mPendingRequests.isEmpty()) {
            ThumbnailProvider next = mPendingRequests.remove();
            mCurrentRequest = next.fetch(mBackend, (status, thumbnail) -> {
                mCurrentRequest = null;
                Entry entry = mCache.get(next);
                if (entry != null) {
                    entry.complete(thumbnail);
                    if (status == MediaRequest.Status.SUCCESS) {
                        updateCache(entry);
                    } else {
                        mCache.remove(next);
                    }
                }
                processNextRequest();
            });
        }
    }

    /**
     * Computes new cache size, trimming the cache if it goes beyond limit.
     * <p>
     * If the new size of the cache is higher than the specified limit, eldest completed cache entries are removed
     * one after another until the size goes below the limit. Non-completed cache entries are never removed by this
     * method.
     *
     * @param updatedEntry the cache entry that caused this computation to be needed
     */
    private void updateCache(@NonNull Entry updatedEntry) {
        mCacheSize += updatedEntry.size();
        Iterator<Entry> iter = mCache.values().iterator();
        while (mCacheSize > mCacheMaxSize && iter.hasNext()) {
            Entry entry = iter.next();
            if (entry.isPrunable()) {
                //release any bitmap and media. No callback will be called since the entry is prunable
                entry.complete(null);
                iter.remove();
                mCacheSize -= entry.size();
            }
        }
    }
}
