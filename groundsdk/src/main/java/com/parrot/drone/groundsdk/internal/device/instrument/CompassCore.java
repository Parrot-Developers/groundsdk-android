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

import com.parrot.drone.groundsdk.device.instrument.Compass;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/** Core class for the Compass instrument. */
public final class CompassCore extends SingletonComponentCore implements Compass {

    /** Description of Compass. */
    private static final ComponentDescriptor<Instrument, Compass> DESC = ComponentDescriptor.of(Compass.class);

    /** Current heading, relative to GPS north (in degrees). */
    private double mHeading;

    /**
     * Constructor.
     *
     * @param instrumentStore store where this instrument belongs.
     */
    public CompassCore(@NonNull ComponentStore<Instrument> instrumentStore) {
        super(DESC, instrumentStore);
    }

    @Override
    public double getHeading() {
        return mHeading;
    }

    /**
     * Updates the current heading.
     *
     * @param heading new heading, relative to GPS North, in degrees in range [0, 360[
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public CompassCore updateHeading(double heading) {
        if (Double.compare(mHeading, heading) != 0) {
            mHeading = heading;
            mChanged = true;
        }
        return this;
    }
}
