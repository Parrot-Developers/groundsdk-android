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

package com.parrot.drone.groundsdk.device.instrument;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.value.OptionalDouble;

/**
 * Instrument that informs about speed.
 * <p>
 * This instrument can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getInstrument(Speedometer.class)}</pre>
 *
 * @see Drone#getInstrument(Class)
 * @see Drone#getInstrument(Class, Ref.Observer)
 */
public interface Speedometer extends Instrument {

    /**
     * Gets the drone current speed on the horizontal plane relative to the ground, in meters/second.
     *
     * @return the current speed on the horizontal plane relative to the ground
     */
    double getGroundSpeed();

    /**
     * Gets the drone current speed along the north axis, relative to the ground, in meters/second.
     * <p>
     * A negative value means that the drone moves to the south.
     *
     * @return the current speed along the north axis
     */
    double getNorthSpeed();

    /**
     * Gets the drone current speed along the east axis, relative to the ground, in meters/second.
     * <p>
     * A negative value means that the drone moves to the west.
     *
     * @return the current speed along the east axis
     */
    double getEastSpeed();

    /**
     * Gets the drone current speed along the down axis, relative to the ground, in meters/second.
     * <p>
     * A negative value means that the drone moves upward.
     *
     * @return the current speed along the down axis
     */
    double getDownSpeed();

    /**
     * Gets the drone current speed along its front axis on the horizontal plane, relative to the ground,
     * in meters/second.
     * <p>
     * A negative value means that the drone moves backward.
     *
     * @return the current speed along the drone front axis
     */
    double getForwardSpeed();

    /**
     * Gets the drone current speed along its right axis on the horizontal plane, relative to the ground,
     * in meters/second.
     * <p>
     * A negative value means that the drone moves to the left.
     *
     * @return the current speed along the drone right axis
     */
    double getRightSpeed();

    /**
     * Gets the drone current speed on the horizontal plane relative to the air, in meters/second.
     * <p>
     * Air speed may be unsupported depending on the drone model and/or firmware versions. <br>
     * Hence, clients of this API should call {@link OptionalDouble#isAvailable() isAvailable} method on the returned
     * value to check whether it can be considered valid before use.
     *
     * @return the current speed on the horizontal plane relative to the air
     *
     * @see OptionalDouble
     */
    @NonNull
    OptionalDouble getAirSpeed();
}
