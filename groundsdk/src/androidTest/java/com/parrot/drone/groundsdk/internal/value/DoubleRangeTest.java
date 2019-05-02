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

import com.parrot.drone.groundsdk.value.DoubleRange;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class DoubleRangeTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithInvertedBoundsException() {
        DoubleRange.of(100, 0);
        fail("method should have thrown IllegalArgumentException");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testScaleFromEqualBoundsException() {
        DoubleRangeCore.RATIO.scaleFrom(0, DoubleRange.of(100.0, 100.0));
        fail("method should have thrown IllegalArgumentException");
    }

    @Test
    public void testScaleFrom() {
        DoubleRange range = DoubleRangeCore.RATIO;

        DoubleRange srcRange = DoubleRange.of(0, 10);
        assertEquals(0.0, range.scaleFrom(0, srcRange));
        assertEquals(0.5, range.scaleFrom(5, srcRange));
        assertEquals(1.0, range.scaleFrom(10, srcRange));
        assertEquals(0.0, range.scaleFrom(-1, srcRange));
        assertEquals(1.0, range.scaleFrom(11, srcRange));

        srcRange = DoubleRange.of(-10, 10);
        assertEquals(0.0, range.scaleFrom(-10, srcRange));
        assertEquals(0.5, range.scaleFrom(0, srcRange));
        assertEquals(1.0, range.scaleFrom(10, srcRange));
        assertEquals(0.0, range.scaleFrom(-12, srcRange));
        assertEquals(1.0, range.scaleFrom(11, srcRange));

        srcRange = DoubleRange.of(-1000, 1000);
        assertEquals(0.0, range.scaleFrom(-1000, srcRange));
        assertEquals(0.5, range.scaleFrom(0, srcRange));
        assertEquals(1.0, range.scaleFrom(1000, srcRange));
        assertEquals(0.0, range.scaleFrom(-1001, srcRange));
        assertEquals(1.0, range.scaleFrom(1001, srcRange));

        range = DoubleRange.of(-1000, 1000);

        srcRange = DoubleRange.of(0, 10);
        assertEquals(-1000.0, range.scaleFrom(0, srcRange));
        assertEquals(0.0, range.scaleFrom(5, srcRange));
        assertEquals(1000.0, range.scaleFrom(10, srcRange));
        assertEquals(-1000.0, range.scaleFrom(-1, srcRange));
        assertEquals(1000.0, range.scaleFrom(11, srcRange));

        srcRange = DoubleRange.of(-10, 10);
        assertEquals(-1000.0, range.scaleFrom(-10, srcRange));
        assertEquals(0.0, range.scaleFrom(0, srcRange));
        assertEquals(1000.0, range.scaleFrom(10, srcRange));
        assertEquals(-1000.0, range.scaleFrom(-11, srcRange));
        assertEquals(1000.0, range.scaleFrom(11, srcRange));

        srcRange = DoubleRange.of(-1000, 1000);
        assertEquals(-1000.0, range.scaleFrom(-1000, srcRange));
        assertEquals(0.0, range.scaleFrom(0, srcRange));
        assertEquals(1000.0, range.scaleFrom(1000, srcRange));
        assertEquals(-1000.0, range.scaleFrom(-1001, srcRange));
        assertEquals(1000.0, range.scaleFrom(1001, srcRange));
    }
}
