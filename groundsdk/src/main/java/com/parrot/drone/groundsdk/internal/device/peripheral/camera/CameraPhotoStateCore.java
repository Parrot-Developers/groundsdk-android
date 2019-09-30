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

package com.parrot.drone.groundsdk.internal.device.peripheral.camera;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto;
import com.parrot.drone.groundsdk.internal.component.ComponentCore;

/** Core class for CameraPhoto.State. */
public final class CameraPhotoStateCore implements CameraPhoto.State {

    /** Notified upon state changes. */
    private final ComponentCore.ChangeListener mChangeListener;

    /** Current photo function state. */
    @NonNull
    private FunctionState mState;

    /** Identifier of the latest saved photo media. */
    @Nullable
    private String mMediaId;

    /** Current amount of taken photos. */
    @IntRange(from = 0)
    private int mPhotoCount;

    /**
     * Constructor.
     *
     * @param changeListener listener notified upon state changes
     */
    CameraPhotoStateCore(@NonNull ComponentCore.ChangeListener changeListener) {
        mChangeListener = changeListener;
        mState = FunctionState.UNAVAILABLE;
    }

    @NonNull
    @Override
    public FunctionState get() {
        return mState;
    }

    @Nullable
    @Override
    public String latestMediaId() {
        return mMediaId;
    }

    @Override
    public int photoCount() {
        return mPhotoCount;
    }

    /**
     * Updates current photo function state.
     * <p>
     * Note that the state must pass a validation process, otherwise it is not applied.
     * <p>
     * In case the state is successfully applied, current photo count is reset to {@code 0} and latest media id
     * is reset to {@code null}.
     *
     * @param state state to update to
     *
     * @return {@code this}, to allow chained calls
     */
    public CameraPhotoStateCore updateState(@NonNull FunctionState state) {
        if (mState != state) {
            mState = state;
            mPhotoCount = 0;
            mMediaId = null;
            mChangeListener.onChange();
        }
        return this;
    }

    /**
     * Updates the identifier of the latest saved photo media.
     * <p>
     * Note that this action does nothing unless current state is {@link FunctionState#STOPPED}.
     *
     * @param mediaId new media identifier
     *
     * @return {@code this}, to allow chained calls
     */
    public CameraPhotoStateCore updateMediaId(@NonNull String mediaId) {
        if (mState == FunctionState.STOPPED && !mediaId.equals(mMediaId)) {
            mMediaId = mediaId;
            mChangeListener.onChange();
        }
        return this;
    }

    /**
     * Updates the count of taken photos.
     * <p>
     * Note that this action does nothing unless current state is {@link FunctionState#STARTED}.
     *
     * @param photoCount new photo count
     *
     * @return {@code this}, to allow chained calls
     */
    public CameraPhotoStateCore updatePhotoCount(@IntRange(from = 0) int photoCount) {
        if (mState == FunctionState.STARTED && mPhotoCount != photoCount) {
            mPhotoCount = photoCount;
            mChangeListener.onChange();
        }
        return this;
    }
}
