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

import com.parrot.drone.groundsdk.device.instrument.FlightMeter;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/** Core class for the FlightMeter instrument. */
public final class FlightMeterCore extends SingletonComponentCore implements FlightMeter {

    /** Description of FlightMeter. */
    private static final ComponentDescriptor<Instrument, FlightMeter> DESC = ComponentDescriptor.of(FlightMeter.class);

    /** Last flight duration, in seconds. */
    private int mLastFlightDuration;

    /** Total flight duration, in seconds. */
    private long mTotalFlightDuration;

    /** Total flight count. */
    private int mTotalFlightCount;

    /**
     * Constructor.
     *
     * @param componentStore store where this component provider belongs
     */
    public FlightMeterCore(@NonNull ComponentStore<Instrument> componentStore) {
        super(DESC, componentStore);
    }

    @Override
    public int getLastFlightDuration() {
        return mLastFlightDuration;
    }

    @Override
    public long getTotalFlightDuration() {
        return mTotalFlightDuration;
    }

    @Override
    public int getTotalFlightCount() {
        return mTotalFlightCount;
    }

    /**
     * Updates the last flight duration.
     *
     * @param duration new last flight duration, in seconds
     *
     * @return this object, to allow chain calls
     */
    public FlightMeterCore updateLastFlightDuration(int duration) {
        if (mLastFlightDuration != duration) {
            mLastFlightDuration = duration;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the total flight duration.
     *
     * @param duration new total flight duration, in seconds
     *
     * @return this object, to allow chain calls
     */
    public FlightMeterCore updateTotalFlightDuration(long duration) {
        if (mTotalFlightDuration != duration) {
            mTotalFlightDuration = duration;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the total flight count.
     *
     * @param count new total flight count
     *
     * @return this object, to allow chain calls
     */
    public FlightMeterCore updateTotalFlightCount(int count) {
        if (mTotalFlightCount != count) {
            mTotalFlightCount = count;
            mChanged = true;
        }
        return this;
    }
}
