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

import com.parrot.drone.groundsdk.device.peripheral.CopterMotors;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.motor.MotorError;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/**
 * Core class for CopterMotors.
 */
public final class CopterMotorsCore extends SingletonComponentCore implements CopterMotors {

    /** Description of CopterMotors. */
    private static final ComponentDescriptor<Peripheral, CopterMotors> DESC =
            ComponentDescriptor.of(CopterMotors.class);

    /**
     * Current motor error, by motor. Only motors currently in error have an entry in this map and their associated
     * error is never {@link MotorError#NONE}.
     */
    private final EnumMap<Motor, MotorError> mCurrentMotorErrors;

    /**
     * Latest motor error, by motor. All supported motors have an entry in this map, possibly associated with
     * {@link MotorError#NONE} in case the latest motor error is not known.
     */
    private final EnumMap<Motor, MotorError> mLatestMotorErrors;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     */
    public CopterMotorsCore(@NonNull ComponentStore<Peripheral> peripheralStore) {
        super(DESC, peripheralStore);
        mCurrentMotorErrors = new EnumMap<>(Motor.class);
        mLatestMotorErrors = new EnumMap<>(Motor.class);
        for (Motor motor : Motor.values()) {
            mLatestMotorErrors.put(motor, MotorError.NONE);
        }
    }

    @NonNull
    @Override
    public MotorError getLatestError(@NonNull Motor motor) {
        MotorError error = mCurrentMotorErrors.get(motor);
        if (error == null) {
            error = mLatestMotorErrors.get(motor);
        }
        //noinspection ConstantConditions : mLatestMotorErrors has values for all Motors
        return error;
    }

    @NonNull
    @Override
    public Set<Motor> getMotorsCurrentlyInError() {
        return mCurrentMotorErrors.isEmpty() ? Collections.emptySet()
                : EnumSet.copyOf(mCurrentMotorErrors.keySet());
    }

    /**
     * Updates a motor's current error.
     *
     * @param motor motor whose error status must be updated
     * @param error new motor error status
     *
     * @return this, to allow call chaining
     */
    public CopterMotorsCore updateCurrentError(@NonNull Motor motor, @NonNull MotorError error) {
        if (error == MotorError.NONE) {
            mChanged |= mCurrentMotorErrors.remove(motor) != null;
        } else {
            mChanged |= mCurrentMotorErrors.put(motor, error) != error;
        }
        return this;
    }

    /**
     * Updates a motor's latest error.
     *
     * @param motor motor whose error status must be updated
     * @param error new motor error status
     *
     * @return this, to allow call chaining
     */
    public CopterMotorsCore updateLatestError(@NonNull Motor motor, @NonNull MotorError error) {
        // only report a change if the motor is not currently in error.
        mChanged |= mLatestMotorErrors.put(motor, error) != error && !mCurrentMotorErrors.containsKey(motor);
        return this;
    }
}

