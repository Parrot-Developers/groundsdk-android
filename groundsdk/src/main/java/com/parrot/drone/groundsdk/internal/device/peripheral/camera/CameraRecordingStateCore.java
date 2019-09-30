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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraRecording;
import com.parrot.drone.groundsdk.internal.component.ComponentCore;

import java.util.Date;

/** Core class for CameraRecording.State. */
public final class CameraRecordingStateCore implements CameraRecording.State {

    /** Notified upon state changes. */
    private final ComponentCore.ChangeListener mChangeListener;

    /** Current recording function state. */
    @NonNull
    private FunctionState mState;

    /** Identifier of the latest saved video media. */
    @Nullable
    private String mMediaId;

    /** Start time of the video currently being recorded. {@code 0} if none. */
    @IntRange(from = 0)
    private long mStartTime;

    /**
     * Constructor.
     *
     * @param changeListener listener notified upon state changes
     */
    CameraRecordingStateCore(@NonNull ComponentCore.ChangeListener changeListener) {
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

    @Nullable
    @Override
    public Date recordStartTime() {
        return mStartTime == 0 ? null : new Date(mStartTime);
    }

    @Override
    public long recordDuration() {
        return mStartTime == 0 ? 0 : System.currentTimeMillis() - mStartTime;
    }

    /**
     * Updates current recording function state.
     * <p>
     * Note that the state must pass a validation process, otherwise it is not applied.
     * <p>
     * In case the state is successfully applied, video record start time is reset to {@code 0} and latest media id
     * is reset to {@code null}.
     *
     * @param state state to update to
     *
     * @return {@code this}, to allow chained calls
     */
    public CameraRecordingStateCore updateState(@NonNull FunctionState state) {
        if (mState != state) {
            mState = state;
            mStartTime = 0;
            if (mState != FunctionState.STOPPED) {
                mMediaId = null;
            }
            mChangeListener.onChange();
        }
        return this;
    }

    /**
     * Updates the identifier of the latest saved video media.
     * <p>
     * Note that this action does nothing unless current state is either {@link FunctionState#STOPPED},
     * {@link FunctionState#ERROR_INSUFFICIENT_STORAGE_SPACE}, {@link FunctionState#ERROR_INSUFFICIENT_STORAGE_SPEED}
     * or {@link FunctionState#CONFIGURATION_CHANGE}.
     *
     * @param mediaId new media identifier
     *
     * @return {@code this}, to allow chained calls
     */
    public CameraRecordingStateCore updateMediaId(@NonNull String mediaId) {
        if ((mState == FunctionState.STOPPED
             || mState == FunctionState.ERROR_INSUFFICIENT_STORAGE_SPACE
             || mState == FunctionState.ERROR_INSUFFICIENT_STORAGE_SPEED
             || mState == FunctionState.CONFIGURATION_CHANGE)
            && !mediaId.equals(mMediaId)) {
            mMediaId = mediaId;
            mChangeListener.onChange();
        }
        return this;
    }

    /**
     * Updates the start time of the video being recorded.
     * <p>
     * Note that this action does nothing unless current state is {@link FunctionState#STARTED}.
     *
     * @param startTime new video record start time
     *
     * @return {@code this}, to allow chained calls
     */
    public CameraRecordingStateCore updateStartTime(@IntRange(from = 0) long startTime) {
        if (mState == FunctionState.STARTED && mStartTime != startTime) {
            mStartTime = startTime;
            mChangeListener.onChange();
        }
        return this;
    }
}
