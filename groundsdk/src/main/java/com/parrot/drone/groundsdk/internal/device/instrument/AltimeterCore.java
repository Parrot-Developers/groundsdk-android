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

import com.parrot.drone.groundsdk.device.instrument.Altimeter;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.groundsdk.internal.value.OptionalDoubleCore;
import com.parrot.drone.groundsdk.value.OptionalDouble;

/** Core class for the Altimeter instrument. */
public final class AltimeterCore extends SingletonComponentCore implements Altimeter {

    /** Description of Altimeter. */
    private static final ComponentDescriptor<Instrument, Altimeter> DESC = ComponentDescriptor.of(Altimeter.class);

    /** Current altitude of the drone, relative to take off altitude (in meters). */
    private double mTakeOffAltitude;

    /** Current altitude of the drone, relative to the ground (in meters). */
    @NonNull
    private final OptionalDoubleCore mGroundAltitude;

    /** Current absolute altitude of the drone, i.e. relative to sea-level (in meters). */
    @NonNull
    private final OptionalDoubleCore mAbsoluteAltitude;

    /** Current vertical speed of the drone (in meters/second). Positive when the drone goes up, negative otherwise. */
    private double mVerticalSpeed;

    /**
     * Constructor.
     *
     * @param instrumentStore store where this instrument belongs.
     */
    public AltimeterCore(@NonNull ComponentStore<Instrument> instrumentStore) {
        super(DESC, instrumentStore);
        mGroundAltitude = new OptionalDoubleCore();
        mAbsoluteAltitude = new OptionalDoubleCore();
    }

    @Override
    public double getTakeOffRelativeAltitude() {
        return mTakeOffAltitude;
    }

    @Override
    @NonNull
    public OptionalDouble getGroundRelativeAltitude() {
        return mGroundAltitude;
    }

    @Override
    @NonNull
    public OptionalDouble getAbsoluteAltitude() {
        return mAbsoluteAltitude;
    }

    @Override
    public double getVerticalSpeed() {
        return mVerticalSpeed;
    }

    /**
     * Updates the current altitude, relative to take off.
     *
     * @param takeOffAltitude new altitude, in meters
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public AltimeterCore updateTakeOffRelativeAltitude(double takeOffAltitude) {
        if (Double.compare(mTakeOffAltitude, takeOffAltitude) != 0) {
            mTakeOffAltitude = takeOffAltitude;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the current altitude, relative to ground.
     *
     * @param groundAltitude new altitude, in meters
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public AltimeterCore updateGroundRelativeAltitude(double groundAltitude) {
        mChanged |= mGroundAltitude.setValue(groundAltitude);
        return this;
    }

    /**
     * Updates the current absolute altitude, i.e. relative to sea level.
     *
     * @param absoluteAltitude new altitude to set, in meters
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public AltimeterCore updateAbsoluteAltitude(double absoluteAltitude) {
        mChanged |= mAbsoluteAltitude.setValue(absoluteAltitude);
        return this;
    }

    /**
     * Resets the current absolute altitude, marking it invalid.
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public AltimeterCore resetAbsoluteAltitude() {
        mChanged |= mAbsoluteAltitude.resetValue();
        return this;
    }

    /**
     * Sets the current vertical speed.
     *
     * @param verticalSpeed new vertical speed, in meters/seconds
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public AltimeterCore updateVerticalSpeed(double verticalSpeed) {
        if (Double.compare(mVerticalSpeed, verticalSpeed) != 0) {
            mVerticalSpeed = verticalSpeed;
            mChanged = true;
        }
        return this;
    }
}
