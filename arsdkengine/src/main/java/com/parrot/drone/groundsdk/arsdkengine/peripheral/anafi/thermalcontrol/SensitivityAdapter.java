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

/**
 * Utility class to adapt {@link ArsdkFeatureThermal.Range thermal feature} to {@link ThermalControl.Sensitivity
 * groundsdk} thermal sensitivities.
 */
final class SensitivityAdapter {

    /**
     * Converts an {@code ArsdkFeatureThermal.Range} to its {@code ThermalControl.Sensitivity} equivalent.
     *
     * @param sensitivity thermal feature sensitivity to convert
     *
     * @return the groundsdk thermal sensitivity equivalent
     */
    @NonNull
    static ThermalControl.Sensitivity from(@NonNull ArsdkFeatureThermal.Range sensitivity) {
        switch (sensitivity) {
            case HIGH:
                return ThermalControl.Sensitivity.HIGH_RANGE;
            case LOW:
                return ThermalControl.Sensitivity.LOW_RANGE;
        }
        return null;
    }

    /**
     * Converts a {@code ThermalControl.Sensitivity} to its {@code ArsdkFeatureThermal.Range} equivalent.
     *
     * @param sensitivity groundsdk thermal sensitivity to convert
     *
     * @return thermal feature sensitivity equivalent of the given value
     */
    @NonNull
    static ArsdkFeatureThermal.Range from(@NonNull ThermalControl.Sensitivity sensitivity) {
        switch (sensitivity) {
            case HIGH_RANGE:
                return ArsdkFeatureThermal.Range.HIGH;
            case LOW_RANGE:
                return ArsdkFeatureThermal.Range.LOW;
        }
        return null;
    }

    /**
     * Private constructor for static utility class.
     */
    private SensitivityAdapter() {
    }
}
