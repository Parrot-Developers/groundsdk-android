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

package com.parrot.drone.groundsdk.internal.device.pilotingitf.animation;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;

/**
 * Abstract base for an {@code Animation} implementation.
 */
public abstract class AnimationCore implements Animation {

    /**
     * Creates an unidentified animation.
     *
     * @return an unidentified animation instance
     */
    @NonNull
    public static AnimationCore unidentified() {
        return new AnimationCore(Type.UNIDENTIFIED) {

            @Override
            public boolean matchesConfig(@NonNull Config config) {
                return false;
            }
        };
    }

    /** Animation type. */
    @NonNull
    private final Type mType;

    /** Animation status. */
    @NonNull
    private Status mStatus;

    /** Animation progress. */
    @IntRange(from = 0, to = 100)
    private int mProgress;

    /**
     * Constructor.
     *
     * @param type animation type
     */
    AnimationCore(@NonNull Type type) {
        mType = type;
        mStatus = Status.ANIMATING;
        mProgress = 0;
    }

    @NonNull
    @Override
    public final Type getType() {
        return mType;
    }

    @Override
    @NonNull
    public final Status getStatus() {
        return mStatus;
    }

    @Override
    public final int getProgress() {
        return mProgress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AnimationCore that = (AnimationCore) o;

        return mType == that.mType;
    }

    @Override
    public int hashCode() {
        return mType.hashCode();
    }

    /**
     * Updates current animation status.
     *
     * @param status up-to-date status value
     *
     * @return {@code true} if the current status changed, otherwise {@code false}
     */
    boolean updateStatus(@NonNull Status status) {
        if (mStatus != status) {
            mStatus = status;
            return true;
        }
        return false;
    }

    /**
     * Updates current animation progress.
     *
     * @param progress up-to-date progress value
     *
     * @return {@code true} if the current progress changed, otherwise {@code false}
     */
    boolean updateProgress(@IntRange(from = 0, to = 100) int progress) {
        if (mProgress != progress) {
            mProgress = progress;
            return true;
        }
        return false;
    }

    /**
     * Tells whether the two specified {@code double} values are close with a maximum delta of 0.01.
     *
     * @param d1 the first {@code double} to compare
     * @param d2 the second {@code double} to compare
     *
     * @return {@code true} if the two specified {@code double} values are close with a maximum delta of 0.01,
     *         otherwise {@code false}
     */
    static boolean almostEqual(double d1, double d2) {
        return Math.abs(d2 - d1) < 0.01;
    }
}
