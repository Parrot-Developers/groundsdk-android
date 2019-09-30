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

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.pilotingitf.animation.Animation;
import com.parrot.drone.groundsdk.device.pilotingitf.animation.Vertigo;

/**
 * Implementation class for the {@code Vertigo} animation.
 */
public class VertigoCore extends AnimationCore implements Vertigo {

    /** Animation duration, in seconds. */
    @FloatRange(from = 0)
    private final double mDuration;

    /** Maximum zoom level. */
    @FloatRange(from = 0)
    private final double mMaxZoomLevel;

    /** Action executed at the end of the animation. */
    @NonNull
    private final Vertigo.FinishAction mFinishAction;

    /** Animation execution mode. */
    @NonNull
    private final Mode mMode;

    /**
     * Constructor.
     *
     * @param duration     animation duration, in seconds
     * @param maxZoomLevel maximum zoom level
     * @param finishAction action executed at the end of the animation
     * @param mode         animation execution mode
     */
    public VertigoCore(@FloatRange(from = 0) double duration, @FloatRange(from = 0) double maxZoomLevel,
                       @NonNull Vertigo.FinishAction finishAction, @NonNull Mode mode) {
        super(Type.VERTIGO);
        mDuration = duration;
        mMaxZoomLevel = maxZoomLevel;
        mFinishAction = finishAction;
        mMode = mode;
    }

    @Override
    public double getDuration() {
        return mDuration;
    }

    @Override
    public double getMaxZoomLevel() {
        return mMaxZoomLevel;
    }

    @NonNull
    @Override
    public FinishAction getFinishAction() {
        return mFinishAction;
    }

    @NonNull
    @Override
    public Mode getMode() {
        return mMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        VertigoCore that = (VertigoCore) o;

        return Double.compare(that.mDuration, mDuration) == 0
               && Double.compare(that.mMaxZoomLevel, mMaxZoomLevel) == 0
               && mFinishAction == that.mFinishAction
               && mMode == that.mMode;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp = Double.doubleToLongBits(mDuration);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mMaxZoomLevel);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + mFinishAction.hashCode();
        result = 31 * result + mMode.hashCode();
        return result;
    }

    @Override
    public boolean matchesConfig(@NonNull Animation.Config config) {
        if (config.getAnimationType() == Type.VERTIGO) {
            Vertigo.Config vertigoConfig = (Vertigo.Config) config;
            return (!vertigoConfig.usesCustomDuration()
                    || almostEqual(vertigoConfig.getDuration(), mDuration))
                   && (!vertigoConfig.usesCustomMaxZoomLevel()
                       || almostEqual(vertigoConfig.getMaxZoomLevel(), mMaxZoomLevel))
                   && (vertigoConfig.getFinishAction() == null
                       || vertigoConfig.getFinishAction() == mFinishAction)
                   && (vertigoConfig.getMode() == null
                       || vertigoConfig.getMode() == mMode);
        }
        return false;
    }
}
