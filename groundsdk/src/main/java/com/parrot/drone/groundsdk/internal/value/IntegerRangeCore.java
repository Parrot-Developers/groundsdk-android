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

import com.parrot.drone.groundsdk.value.IntegerRange;

/** Range of int. */
public final class IntegerRangeCore implements IntegerRange {

    /** An immutable [0, 100] range. */
    public static final IntegerRange PERCENTAGE = IntegerRange.of(0, 100);

    /** An immutable [-100, 100] range. */
    public static final IntegerRange SIGNED_PERCENTAGE = IntegerRange.of(-100, 100);

    /** Range lower bound. */
    private int mLower;

    /** Range upper bound. */
    private int mUpper;

    /**
     * Creates a new range.
     * <p>
     * The bounds are {@code [lower, upper]};
     * {@code lower} must be lower than or equal {@code upper}.
     *
     * @param lower The lower endpoint (inclusive)
     * @param upper The upper endpoint (inclusive)
     */
    public IntegerRangeCore(int lower, int upper) {
        if (lower > upper) {
            throw new IllegalArgumentException("lower must be less than or equal to upper");
        }
        mLower = lower;
        mUpper = upper;
    }

    @Override
    public int getLower() {
        return mLower;
    }

    @Override
    public int getUpper() {
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
    public boolean updateBounds(int lowerBound, int upperBound) {
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
    public boolean updateBounds(@NonNull IntegerRange range) {
        return updateBoundsUnchecked(range.getLower(), range.getUpper());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntegerRangeCore that = (IntegerRangeCore) o;
        return mLower == that.mLower
               && mUpper == that.mUpper;
    }

    @Override
    public int hashCode() {
        int result = mLower;
        result = 31 * result + mUpper;
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
    private boolean updateBoundsUnchecked(int lowerBound, int upperBound) {
        if (mLower != lowerBound || mUpper != upperBound) {
            mLower = lowerBound;
            mUpper = upperBound;
            return true;
        }
        return false;
    }
}
