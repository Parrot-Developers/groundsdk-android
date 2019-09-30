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
import com.parrot.drone.groundsdk.device.pilotingitf.animation.VerticalReveal;

/**
 * Implementation class for the {@code VerticalReveal} animation.
 */
public class VerticalRevealCore extends AnimationCore implements VerticalReveal {

    /** Animation vertical speed, in meters per second. */
    @FloatRange(from = 0)
    private final double mVerticalSpeed;

    /** Animation vertical distance, in meters. */
    @FloatRange(from = 0)
    private final double mVerticalDistance;

    /** Animation rotation angle, in degrees. */
    private final double mRotationAngle;

    /** Animation rotation speed, in degrees per second. */
    @FloatRange(from = 0)
    private final double mRotationSpeed;

    /** Animation execution mode. */
    @NonNull
    private final Mode mMode;

    /**
     * Constructor.
     *
     * @param verticalSpeed    animation vertical speed, in meters per second
     * @param verticalDistance animation vertical distance, in meters
     * @param rotationAngle    animation rotation angle, in degrees
     * @param rotationSpeed    animation rotation speed, in degrees per second
     * @param mode             animation execution mode
     */
    public VerticalRevealCore(@FloatRange(from = 0) double verticalSpeed, @FloatRange(from = 0) double verticalDistance,
                              double rotationAngle, @FloatRange(from = 0) double rotationSpeed, @NonNull Mode mode) {
        super(Type.VERTICAL_REVEAL);
        mVerticalSpeed = verticalSpeed;
        mVerticalDistance = verticalDistance;
        mRotationAngle = rotationAngle;
        mRotationSpeed = rotationSpeed;
        mMode = mode;
    }

    @Override
    public double getVerticalSpeed() {
        return mVerticalSpeed;
    }

    @Override
    public double getVerticalDistance() {
        return mVerticalDistance;
    }

    @Override
    public double getRotationAngle() {
        return mRotationAngle;
    }

    @Override
    public double getRotationSpeed() {
        return mRotationSpeed;
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

        VerticalRevealCore that = (VerticalRevealCore) o;

        return Double.compare(that.mVerticalSpeed, mVerticalSpeed) == 0
               && Double.compare(that.mVerticalDistance, mVerticalDistance) == 0
               && Double.compare(that.mRotationAngle, mRotationAngle) == 0
               && Double.compare(that.mRotationSpeed, mRotationSpeed) == 0
               && mMode == that.mMode;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(mVerticalSpeed);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mVerticalDistance);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mRotationAngle);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mRotationSpeed);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + mMode.hashCode();
        return result;
    }

    @Override
    public boolean matchesConfig(@NonNull Animation.Config config) {
        if (config.getAnimationType() == Type.VERTICAL_REVEAL) {
            VerticalReveal.Config verticalRevealConfig = (VerticalReveal.Config) config;
            return (!verticalRevealConfig.usesCustomVerticalSpeed()
                    || almostEqual(verticalRevealConfig.getVerticalSpeed(), mVerticalSpeed))
                   && (!verticalRevealConfig.usesCustomVerticalDistance()
                       || almostEqual(verticalRevealConfig.getVerticalDistance(), mVerticalDistance))
                   && (!verticalRevealConfig.usesCustomRotationAngle()
                       || almostEqual(verticalRevealConfig.getRotationAngle(), mRotationAngle))
                   && (!verticalRevealConfig.usesCustomRotationSpeed()
                       || almostEqual(verticalRevealConfig.getRotationSpeed(), mRotationSpeed))
                   && (verticalRevealConfig.getMode() == null
                       || verticalRevealConfig.getMode() == mMode);
        }
        return false;
    }
}
