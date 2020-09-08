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
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureThermal;

import java.util.EnumSet;

/**
 * Utility class to adapt {@link ArsdkFeatureThermal.Mode thermal feature} to {@link ThermalControl.Mode groundsdk}
 * thermal modes.
 */
final class ModeAdapter {

    /**
     * Converts an {@code ArsdkFeatureThermal.Mode} to its {@code ThermalControl.Mode} equivalent.
     *
     * @param mode thermal feature mode to convert
     *
     * @return the groundsdk thermal mode equivalent
     */
    @NonNull
    static ThermalControl.Mode from(@NonNull ArsdkFeatureThermal.Mode mode) {
        switch (mode) {
            case DISABLED:
                return ThermalControl.Mode.DISABLED;
            case STANDARD:
                return ThermalControl.Mode.STANDARD;
            case BLENDED:
                return ThermalControl.Mode.EMBEDDED;
        }
        return null;
    }

    /**
     * Converts a {@code ThermalControl.Mode} to its {@code ArsdkFeatureThermal.Mode} equivalent.
     *
     * @param mode groundsdk thermal mode to convert
     *
     * @return thermal feature mode equivalent of the given value
     */
    @NonNull
    static ArsdkFeatureThermal.Mode from(@NonNull ThermalControl.Mode mode) {
        switch (mode) {
            case DISABLED:
                return ArsdkFeatureThermal.Mode.DISABLED;
            case STANDARD:
                return ArsdkFeatureThermal.Mode.STANDARD;
            case EMBEDDED:
                return ArsdkFeatureThermal.Mode.BLENDED;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureThermal.Mode} to its equivalent set of
     * {@code ThermalControl.Mode}.
     *
     * @param bitfield bitfield representation of thermal feature modes to convert
     *
     * @return the equivalent set of groundsdk thermal modes
     */
    @NonNull
    static EnumSet<ThermalControl.Mode> from(int bitfield) {
        EnumSet<ThermalControl.Mode> modes = EnumSet.noneOf(ThermalControl.Mode.class);
        ArsdkFeatureThermal.Mode.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private ModeAdapter() {
    }
}
