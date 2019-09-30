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

package com.parrot.drone.groundsdk.internal.value;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.value.DoubleRange;

/** Range of double. */
public final class DoubleRangeCore implements DoubleRange {

    /** An immutable [0.0, 1.0] range. */
    public static final DoubleRange RATIO = DoubleRange.of(0, 1);

    /** An immutable [-1.0, 1.0] range. */
    public static final DoubleRange SIGNED_RATIO = DoubleRange.of(-1, 1);

    /** Range lower bound. */
    private double mLower;

    /** Range upper bound. */
    private double mUpper;

    /**
     * Creates a new range.
     * <p>
     * The bounds are {@code [lower, upper]};
     * {@code lower} must be lower than or equal {@code upper}.
     *
     * @param lower The lower endpoint (inclusive)
     * @param upper The upper endpoint (inclusive)
     */
    public DoubleRangeCore(double lower, double upper) {
        if (lower > upper) {
            throw new IllegalArgumentException("lower must be less than or equal to upper");
        }
        mLower = lower;
        mUpper = upper;
    }

    @Override
    public double getLower() {
        return mLower;
    }

    @Override
    public double getUpper() {
        return mUpper;
    }

    /**
     * Updates range bounds.
     *
     * @param lowerBound new lower bound
     * @param upperBound new upper bound
     *
     * @return {@code true} if the range bounds changed, otherwise {@code false}
     *
     * @throws IllegalArgumentException when {@code lowerBound > upperBound}
     */
    public boolean updateBounds(double lowerBound, double upperBound) {
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Lower bound [" + lowerBound + "] must not be greater than upper bound ["
                                               + upperBound + "]");
        }

        return updateBoundsUnchecked(lowerBound, upperBound);
    }

    /**
     * Updates range bounds.
     *
     * @param range range defining new bounds
     *
     * @return {@code true} if the range bounds changed, otherwise {@code false}
     */
    public boolean updateBounds(@NonNull DoubleRange range) {
        return updateBoundsUnchecked(range.getLower(), range.getUpper());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoubleRangeCore that = (DoubleRangeCore) o;
        return Double.compare(that.mLower, mLower) == 0
               && Double.compare(that.mUpper, mUpper) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(mLower);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mUpper);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "[" + mLower + ", " + mUpper + "]";
    }

    /**
     * Updates range bounds.
     * <p>
     * Assumes {@code lowerBound <= upperBound}
     *
     * @param lowerBound new lower bound
     * @param upperBound new upper bound
     *
     * @return {@code true} if the range bounds changed, otherwise {@code false}
     */
    private boolean updateBoundsUnchecked(double lowerBound, double upperBound) {
        if (Double.compare(mLower, lowerBound) != 0 || Double.compare(mUpper, upperBound) != 0) {
            mLower = lowerBound;
            mUpper = upperBound;
            return true;
        }
        return false;
    }
}
