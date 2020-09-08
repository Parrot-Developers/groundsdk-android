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
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;
import com.parrot.drone.groundsdk.internal.session.Session;

import java.util.Collections;
import java.util.List;

/**
 * A reference on a list of media items.
 */
final class MediaListRef extends Session.RefBase<List<MediaItem>> {

    /** Media store used to request the list of media. */
    @NonNull
    private final MediaStoreCore mStore;

    /** Current media list request. */
    @Nullable
    private MediaRequest mRequest;

    /** Storage type targeted. */
    @Nullable
    private final MediaStore.StorageType mStorageType;

    /**
     * Constructor.
     *
     * @param session       session that will manage this ref
     * @param observer      observer that will be notified when the referenced object is updated
     * @param store         media store to query the media list from
     * @param storageType   storage type targeted
     */
    MediaListRef(@NonNull Session session, @NonNull Observer<? super List<MediaItem>> observer,
                 @NonNull MediaStoreCore store, @Nullable MediaStore.StorageType storageType) {
        super(session, observer);
        mStore = store;
        mStore.registerObserver(mStoreObserver);
        mStorageType = storageType;

        requestList();
    }

    @Override
    protected void release() {
        mStore.unregisterObserver(mStoreObserver);
        if (mRequest != null) {
            mRequest.cancel();
            mRequest = null;
        }
        update(Collections.emptyList());
        super.release();
    }

    /**
     * Requests a new list of media items, canceling the current list request if necessary.
     */
    private void requestList() {
        if (mRequest != null) {
            mRequest.cancel();
        }
        MediaRequest.ResultCallback<List<? extends MediaItemCore>> callback = (status, list) -> {
            if (status != MediaRequest.Status.CANCELED) {
                update(list == null ? Collections.emptyList() : Collections.unmodifiableList(list));
            }
        };
        mRequest = mStore.mBackend.browse(mStorageType, callback);
    }

    /** Notified when the media store changes, triggers a new list request. */
    private final MediaStoreCore.Observer mStoreObserver = this::requestList;
}
