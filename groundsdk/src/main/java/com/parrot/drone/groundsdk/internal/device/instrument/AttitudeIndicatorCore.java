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

package com.parrot.drone.groundsdk.internal.device.instrument;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.instrument.AttitudeIndicator;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/** Core class for the AttitudeIndicator instrument. */
public final class AttitudeIndicatorCore extends SingletonComponentCore implements AttitudeIndicator {

    /** Description of AttitudeIndicator. */
    private static final ComponentDescriptor<Instrument, AttitudeIndicator> DESC =
            ComponentDescriptor.of(AttitudeIndicator.class);

    /** Current pitch angle of the drone, in degrees. */
    private double mPitch;

    /** Current roll angle of the drone, in degrees. */
    private double mRoll;

    /**
     * Constructor.
     *
     * @param instrumentStore store where this instrument belongs.
     */
    public AttitudeIndicatorCore(@NonNull ComponentStore<Instrument> instrumentStore) {
        super(DESC, instrumentStore);
    }

    @Override
    public double getPitch() {
        return mPitch;
    }

    @Override
    public double getRoll() {
        return mRoll;
    }

    /**
     * Updates the current pitch angle. <br>
     *
     * @param pitch new pitch angle, in degrees
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public AttitudeIndicatorCore updatePitch(double pitch) {
        if (Double.compare(mPitch, pitch) != 0) {
            mPitch = pitch;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the current roll angle. <br>
     *
     * @param roll new roll angle, in degrees
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public AttitudeIndicatorCore updateRoll(double roll) {
        if (Double.compare(mRoll, roll) != 0) {
            mRoll = roll;
            mChanged = true;
        }
        return this;
    }
}
