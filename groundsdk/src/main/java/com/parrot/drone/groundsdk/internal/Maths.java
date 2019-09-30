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

package com.parrot.drone.groundsdk.internal;

import androidx.annotation.IntRange;

import java.math.BigInteger;

/** Utilities to manage math computation. */
public final class Maths {

    /**
     * Converts the given angle from radians to degrees in range [0, 360[.
     *
     * @param angleRadians angle to convert, in radians
     *
     * @return converted angle value, in degrees, in range [0, 360[
     */
    public static double radiansToBoundedDegrees(double angleRadians) {
        double angleDegrees = java.lang.Math.toDegrees(angleRadians) % 360;
        if (angleDegrees < 0) {
            angleDegrees += 360;
        }
        return angleDegrees;
    }

    /**
     * Computes the greatest common denominator (GCD) of two integers.
     * <p>
     * Both integers must be strictly positive; this guarantees a strictly positive output.
     * <p>
     * For reference, given {@code x > 0, gcd(0, x) == gcd(x, 0) == x}; Mathematically speaking, {@code gcd(0,0)} is
     * undefined, although most gcd implementations (like {@link java.math.BigInteger#gcd(BigInteger) this one}) give
     * {@code gcd(0, 0) == 0}.
     *
     * @param lhs first of two integers to compute GCD of
     * @param rhs second of two integers to compute GCD of
     *
     * @return greatest common denominator of given integers
     */
    @IntRange(from = 1)
    public static int gcd(@IntRange(from = 1) int lhs, @IntRange(from = 1) int rhs) {
        do {
            int tmp = lhs % rhs;
            lhs = rhs;
            rhs = tmp;
        } while (rhs > 0);

        return lhs;
    }

    /**
     * Private constructor for static utility class.
     */
    private Maths() {
    }
}
