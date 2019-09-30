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
 * Utility class to adapt {@link ArsdkFeatureThermal.ShutterTrigger thermal feature} to
 * {@link ThermalControl.Calibration.Mode groundsdk} thermal camera calibration modes.
 */
final class CalibrationModeAdapter {

    /**
     * Converts an {@code ArsdkFeatureThermal.ShutterTrigger} to its {@code ThermalControl.Calibration.Mode}
     * equivalent.
     *
     * @param mode thermal feature calibration mode to convert
     *
     * @return the groundsdk thermal calibration mode equivalent
     */
    @NonNull
    static ThermalControl.Calibration.Mode from(@NonNull ArsdkFeatureThermal.ShutterTrigger mode) {
        switch (mode) {
            case AUTO:
                return ThermalControl.Calibration.Mode.AUTOMATIC;
            case MANUAL:
                return ThermalControl.Calibration.Mode.MANUAL;
        }
        return null;
    }

    /**
     * Converts a {@code ThermalControl.Calibration.Mode} to its {@code ArsdkFeatureThermal.ShutterTrigger}
     * equivalent.
     *
     * @param mode groundsdk thermal calibration mode to convert
     *
     * @return thermal feature calibration mode equivalent of the given value
     */
    @NonNull
    static ArsdkFeatureThermal.ShutterTrigger from(@NonNull ThermalControl.Calibration.Mode mode) {
        switch (mode) {
            case AUTOMATIC:
                return ArsdkFeatureThermal.ShutterTrigger.AUTO;
            case MANUAL:
                return ArsdkFeatureThermal.ShutterTrigger.MANUAL;
        }
        return null;
    }

    /**
     * Private constructor for static utility class.
     */
    private CalibrationModeAdapter() {
    }
}
