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
 * Instrument that informs about the drone current altitude and vertical speed.
 * <p>
 * This instrument can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getInstrument(Altimeter.class)}</pre>
 *
 * @see Drone#getInstrument(Class)
 * @see Drone#getInstrument(Class, Ref.Observer)
 */
public interface Altimeter extends Instrument {

    /**
     * Gets the current altitude of the drone, relative to the take off altitude, in meters.
     *
     * @return the current altitude, relative to take off
     */
    double getTakeOffRelativeAltitude();

    /**
     * Gets the current altitude of the drone, relative to the ground, in meters.
     * <p>
     * Ground-relative altitude may be unsupported depending on the drone model and/or firmware versions. <br>
     * Hence, clients of this API should call {@link OptionalDouble#isAvailable() isAvailable} method on the returned
     * value to check whether it can be considered valid before use.
     * <p>
     * <strong>IMPORTANT:</strong> value may be wrong at high altitudes and jump brutally when the drone gets closer to
     * the ground.
     *
     * @return the current altitude, relative to the ground
     *
     * @see OptionalDouble
     */
    @NonNull
    OptionalDouble getGroundRelativeAltitude();

    /**
     * Gets the current absolute altitude of the drone, i.e. relative to sea-level, in meters.
     * <p>
     * Absolute altitude may not be available at all time. For instance, it is unavailable if the GPS fix is lost. <br>
     * Absolute altitude may also be unsupported depending on the drone model and/or firmware versions. <br>
     * Hence, clients of this API should call {@link OptionalDouble#isAvailable() isAvailable} method on the returned
     * value to check whether it can be considered valid before use.
     *
     * @return the current absolute altitude
     *
     * @see OptionalDouble
     */
    @NonNull
    OptionalDouble getAbsoluteAltitude();

    /**
     * Gets the current vertical speed of the drone, in meters/seconds. Positive when the drone is going up, negative
     * when the drone is going down.
     *
     * @return the current vertical speed
     */
    double getVerticalSpeed();
}
