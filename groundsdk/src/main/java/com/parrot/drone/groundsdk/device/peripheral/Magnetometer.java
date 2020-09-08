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

package com.parrot.drone.groundsdk.device.peripheral;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;

/**
 * Magnetometer peripheral interface.
 * <p>
 * Base class telling whether the magnetometer is calibrated or not.
 * <p>
 * A subclass shall be used to control the calibration process, depending on the device, for instance
 * {@link MagnetometerWith1StepCalibration} or {@link MagnetometerWith3StepCalibration}.
 * <p>
 * This peripheral can be obtained from a {@link Provider peripheral providing device} (such as a drone or a
 * remote control) using:
 * <br><pre>    {@code device.getPeripheral(Magnetometer.class)}</pre>
 *
 * @see Provider#getPeripheral(Class)
 * @see Provider#getPeripheral(Class, Ref.Observer)
 */
public interface Magnetometer extends Peripheral {

    /** Magnetometer calibration state. */
    enum MagnetometerCalibrationState {

        /** Magnetometer is calibrated. */
        CALIBRATED,

        /** Magnetometer calibration is required. */
        REQUIRED,

        /** Magnetometer calibration is recommended. */
        RECOMMENDED
    }

    /**
     * The magnetometer calibration state.
     * <p>
     * Note: the magnetometer should be calibrated to make positioning related actions, such as ReturnToHome,
     * FlightPlan...
     *
     * @return the magnetometer calibration state.
     */
    @NonNull
    MagnetometerCalibrationState calibrationState();
}
