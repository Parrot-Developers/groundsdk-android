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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.Magnetometer;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/** Core class for the Magnetometer. */
public abstract class MagnetometerCore extends SingletonComponentCore implements Magnetometer {

    /** Description of Magnetometer. */
    static final ComponentDescriptor<Peripheral, Magnetometer> DESC = ComponentDescriptor.of(Magnetometer.class);

    /** Backend of a MagnetometerCore which handles the messages. */
    public interface Backend {

        /**
         * Ask to the drone to start the calibration process.
         */
        void startCalibrationProcess();

        /**
         * Ask to the drone to cancel the current calibration process.
         */
        void cancelCalibrationProcess();
    }

    /** Backend of this peripheral. */
    @NonNull
    final Backend mBackend;

    /** The magnetometer calibration state. */
    @NonNull
    private MagnetometerCalibrationState mCalibrationState;

    /**
     * Constructor.
     *
     * @param descriptor      specific descriptor of the provided component
     * @param peripheralStore store where this component provider belongs
     * @param backend         backend that should be used to handle changes
     */
    MagnetometerCore(@NonNull ComponentDescriptor<Peripheral, ? extends Magnetometer> descriptor,
                     @NonNull ComponentStore<Peripheral> peripheralStore,
                     @NonNull Backend backend) {
        super(descriptor, peripheralStore);
        mBackend = backend;
        mCalibrationState = MagnetometerCalibrationState.REQUIRED;
    }

    @Override
    @NonNull
    public MagnetometerCalibrationState calibrationState() {
        return mCalibrationState;
    }

    /**
     * Updates the calibration state of the magnetometer.<br>
     * <p>
     * Note: changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param calibrationState calibration state
     *
     * @return the object, to allow chain calls
     */
    @NonNull
    public MagnetometerCore updateCalibrationState(@NonNull MagnetometerCalibrationState calibrationState) {
        if (mCalibrationState != calibrationState) {
            mCalibrationState = calibrationState;
            mChanged = true;
        }

        return this;
    }

}
