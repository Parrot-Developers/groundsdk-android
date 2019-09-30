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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.thermalcontrol;

import androidx.annotation.Nullable;

import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureThermal;

/** Class allowing to store palette settings sent to drone or received from drone. */
final class PaletteSettings {

    /** Palette mode. */
    @Nullable
    private final ArsdkFeatureThermal.PaletteMode mMode;

    /** Lowest temperature, in Kelvin. */
    private final float mLowestTemp;

    /** Highest temperature, in Kelvin. */
    private final float mHighestTemp;

    /** Colorization mode outside bounds in {@link ArsdkFeatureThermal.PaletteMode#ABSOLUTE absolute} mode. */
    @Nullable
    private final ArsdkFeatureThermal.ColorizationMode mOutsideColorization;

    /** Relative range in {@link ArsdkFeatureThermal.PaletteMode#RELATIVE relative} mode. */
    @Nullable
    private final ArsdkFeatureThermal.RelativeRangeMode mRelativeRange;

    /** Temperature type to highlight in {@link ArsdkFeatureThermal.PaletteMode#SPOT spot} mode. */
    @Nullable
    private final ArsdkFeatureThermal.SpotType mSpotType;

    /** Threshold for highlighting in {@link ArsdkFeatureThermal.PaletteMode#SPOT spot} mode. */
    private final float mSpotThreshold;

    /**
     * Constructor.
     *
     * @param mode                palette mode
     * @param lowestTemp          lowest temperature, in Kelvin
     * @param highestTemp         highest temperature, in Kelvin
     * @param outsideColorization colorization mode outside palette bounds
     *                            in {@link ArsdkFeatureThermal.PaletteMode#ABSOLUTE absolute} mode
     * @param relativeRange       relative range in {@link ArsdkFeatureThermal.PaletteMode#RELATIVE relative} mode
     * @param spotType            temperature type to highlight in {@link ArsdkFeatureThermal.PaletteMode#SPOT spot}
     *                            mode
     * @param spotThreshold       threshold for highlighting in {@link ArsdkFeatureThermal.PaletteMode#SPOT spot}
     *                            mode
     */
    PaletteSettings(@Nullable ArsdkFeatureThermal.PaletteMode mode,
                    float lowestTemp, float highestTemp,
                    @Nullable ArsdkFeatureThermal.ColorizationMode outsideColorization,
                    @Nullable ArsdkFeatureThermal.RelativeRangeMode relativeRange,
                    @Nullable ArsdkFeatureThermal.SpotType spotType, float spotThreshold) {
        mMode = mode;
        mLowestTemp = lowestTemp;
        mHighestTemp = highestTemp;
        mOutsideColorization = outsideColorization;
        mRelativeRange = relativeRange;
        mSpotType = spotType;
        mSpotThreshold = spotThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaletteSettings)) {
            return false;
        }
        PaletteSettings that = (PaletteSettings) o;
        return mMode == that.mMode
               && Float.compare(mLowestTemp, that.mLowestTemp) == 0
               && Float.compare(mHighestTemp, that.mHighestTemp) == 0
               && mOutsideColorization == that.mOutsideColorization
               && mRelativeRange == that.mRelativeRange
               && mSpotType == that.mSpotType
               && Float.compare(mSpotThreshold, that.mSpotThreshold) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(mLowestTemp);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mHighestTemp);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mSpotThreshold);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        if (mMode != null) {
            result = 31 * result + mMode.hashCode();
        }
        if (mOutsideColorization != null) {
            result = 31 * result + mOutsideColorization.hashCode();
        }
        if (mRelativeRange != null) {
            result = 31 * result + mRelativeRange.hashCode();
        }
        if (mSpotType != null) {
            result = 31 * result + mSpotType.hashCode();
        }
        return result;
    }
}
