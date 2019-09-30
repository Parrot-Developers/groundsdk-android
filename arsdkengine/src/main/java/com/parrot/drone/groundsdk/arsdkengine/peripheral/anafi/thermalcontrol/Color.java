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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.ThermalControl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Class allowing to store palette colors sent to drone or received from drone. */
final class Color implements Comparable<Color> {

    /**
     * Converts a {@code Collection} of {@code ThermalControl.Palette.Color} to its {@code List} of {@code Color}
     * equivalent.
     *
     * @param colors colors to convert
     *
     * @return {@code List} of {@code Color} equivalent of the given value.
     */
    @NonNull
    static List<Color> from(@NonNull Collection<ThermalControl.Palette.Color> colors) {
        List<Color> anafiColors = new ArrayList<>();
        for (ThermalControl.Palette.Color color : colors) {
            anafiColors.add(new Color((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue(),
                    (float) color.getPosition()));
        }
        Collections.sort(anafiColors);
        return anafiColors;
    }

    /** Red component. */
    final float mRed;

    /** Green component. */
    final float mGreen;

    /** Blue component. */
    final float mBlue;

    /** Position in palette. */
    final float mPosition;

    /**
     * Constructor.
     *
     * @param red      red component
     * @param green    green component
     * @param blue     blue component
     * @param position position in palette
     */
    Color(float red, float green, float blue, float position) {
        mRed = red;
        mGreen = green;
        mBlue = blue;
        mPosition = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Color)) {
            return false;
        }
        Color that = (Color) o;
        return Float.compare(mRed, that.mRed) == 0
               && Float.compare(mGreen, that.mGreen) == 0
               && Float.compare(mBlue, that.mBlue) == 0
               && Float.compare(mPosition, that.mPosition) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(mRed);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mGreen);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mBlue);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mPosition);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public int compareTo(Color o) {
        if (mPosition < o.mPosition) {
            return -1;
        } else if (mPosition > o.mPosition) {
            return 1;
        } else if (mRed < o.mRed) {
            return -1;
        } else if (mRed > o.mRed) {
            return 1;
        } else if (mGreen < o.mGreen) {
            return -1;
        } else if (mGreen > o.mGreen) {
            return 1;
        } else if (mBlue < o.mBlue) {
            return -1;
        } else if (mBlue > o.mBlue) {
            return 1;
        }
        return 0;
    }
}
