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

import com.parrot.drone.groundsdk.device.peripheral.media.MediaStoreWiper;
import com.parrot.drone.groundsdk.device.peripheral.media.MediaTaskStatus;

/**
 * Implementation of the MediaStoreWiper task.
 */
public class MediaStoreWiperCore implements MediaStoreWiper {

    /**
     * Allows to observe the store eraser task changes.
     */
    interface Observer {

        /**
         * Called back when the store eraser task changes.
         *
         * @param allMediaDeleter deletion task which changed
         */
        void onChanged(@NonNull MediaStoreWiperCore allMediaDeleter);
    }

    /** Store to wipe all media from. */
    @NonNull
    private final MediaStoreCore mStore;

    /** Observer notified when the status changes. */
    @NonNull
    private final Observer mObserver;

    /** Current delete request being processed. */
    @Nullable
    private MediaRequest mCurrentRequest;

    /** Current status. */
    @NonNull
    private MediaTaskStatus mStatus;

    /** Has pending changes waiting for {@link #notifyUpdated()} call. */
    private boolean mChanged;

    /**
     * Constructor.
     *
     * @param store    media store to delete all media from
     * @param observer observer notified of status changes
     */
    MediaStoreWiperCore(@NonNull MediaStoreCore store, @NonNull Observer observer) {
        mStore = store;
        mObserver = observer;
        mStatus = MediaTaskStatus.RUNNING;
    }

    @NonNull
    @Override
    public MediaTaskStatus getStatus() {
        return mStatus;
    }

    /**
     * Executes the deletion task.
     */
    void execute() {
        mChanged = true;
        mCurrentRequest = mStore.mBackend.wipe(mRequestCallback);
        notifyUpdated();
    }

    /**
     * Cancels the whole delete operation.
     */
    void cancel() {
        if (mCurrentRequest != null) {
            mCurrentRequest.cancel();
            mCurrentRequest = null;
        }
    }

    /** Called back when the current request completes successfully or fails. */
    private final MediaRequest.StatusCallback mRequestCallback = new MediaRequest.StatusCallback() {

        @Override
        public void onRequestComplete(@NonNull MediaRequest.Status status) {
            mCurrentRequest = null;
            updateStatus(status == MediaRequest.Status.SUCCESS ? MediaTaskStatus.COMPLETE : MediaTaskStatus.ERROR);
            notifyUpdated();
        }
    };

    /**
     * Updates the current status.
     *
     * @param newStatus new status to set
     */
    private void updateStatus(@NonNull MediaTaskStatus newStatus) {
        if (mStatus != newStatus) {
            mStatus = newStatus;
            mChanged = true;
        }
    }

    /**
     * Notifies changes made by previously called setters.
     */
    private void notifyUpdated() {
        if (mChanged) {
            mChanged = false;
            mObserver.onChanged(this);
        }
    }
}
