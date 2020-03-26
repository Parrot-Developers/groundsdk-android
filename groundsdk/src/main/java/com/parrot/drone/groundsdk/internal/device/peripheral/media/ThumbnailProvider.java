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

/**
 * Represents an entity that can provide a thumbnail.
 * <p>
 * Abstracts over either a media item or a resource, both of which can provides thumbnails.
 */
abstract class ThumbnailProvider {

    /**
     * Wraps a media item into a {@code ThumbnailProvider}.
     *
     * @param media media item to wrap
     *
     * @return wrapped media item, as a {@code ThumbnailProvider}
     */
    @NonNull
    static ThumbnailProvider wrap(@NonNull MediaItemCore media) {
        return new ThumbnailProvider(media) {

            @Nullable
            @Override
            MediaRequest fetch(@NonNull MediaThumbnailCache.Backend backend,
                               @NonNull MediaRequest.ResultCallback<Bitmap> callback) {
                return backend.fetchThumbnail(media, callback);
            }
        };
    }

    /**
     * Wraps a media resource into a {@code ThumbnailProvider}.
     *
     * @param resource media resource to wrap
     *
     * @return wrapped media resource, as a {@code ThumbnailProvider}
     */
    @NonNull
    static ThumbnailProvider wrap(@NonNull MediaResourceCore resource) {
        return new ThumbnailProvider(resource) {

            @Nullable
            @Override
            MediaRequest fetch(@NonNull MediaThumbnailCache.Backend backend,
                               @NonNull MediaRequest.ResultCallback<Bitmap> callback) {
                return backend.fetchThumbnail(resource, callback);
            }
        };
    }

    /**
     * Requests the thumbnail from this provider.
     * <p>
     * {@code callback} is always called, either after success or failure. <br>
     * In case the callback is invoked directly by this method, then this method returns {@code null}. Otherwise
     * this method returns a {@code MediaRequest} object, which can be used to cancel the request, and means that the
     * callback will be invoked at a later time.
     * <p>
     * Note that even in case of {@link MediaRequest.Status#SUCCESS}, the provided bitmap may be {@code null}, which
     * means that it is known that this media does not provide a thumbnail.
     *
     * @param backend  backend allowing to fetch thumbnails
     * @param callback callback notified of request result
     *
     * @return a request that can be canceled, or {@code null} if the request was processed directly
     */
    @Nullable
    abstract MediaRequest fetch(@NonNull MediaThumbnailCache.Backend backend,
                                @NonNull MediaRequest.ResultCallback<Bitmap> callback);

    /** Wrapped item, used for object identity. */
    @NonNull
    private final Object mItem;

    /**
     * Constructor.
     *
     * @param item wrapped item
     */
    private ThumbnailProvider(@NonNull Object item) {
        mItem = item;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThumbnailProvider provider = (ThumbnailProvider) o;
        return mItem.equals(provider.mItem);
    }

    @Override
    public final int hashCode() {
        return mItem.hashCode();
    }
}
