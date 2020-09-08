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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.value.OptionalInt;

/**
 * Instrument that informs about a device's battery.
 * <p>
 * This instrument can be obtained from a {@link Instrument.Provider instrument providing device} (such as a drone or a
 * remote control) using:
 * <pre>{@code device.getInstrument(BatteryInfo.class)}</pre>
 *
 * @see Instrument.Provider#getInstrument(Class)
 * @see Instrument.Provider#getInstrument(Class, Ref.Observer)
 */
public interface BatteryInfo extends Instrument {

    /**
     * Retrieves the device's current battery charge level, as an integer percentage of full charge.
     *
     * @return current battery charge level
     */
    @IntRange(from = 0, to = 100)
    int getBatteryLevel();

    /**
     * Tells whether the device is currently charging.
     *
     * @return {@code true} if the device is charging, {@code false} otherwise
     */
    boolean isCharging();

    /**
     * Retrieves the device's current battery state of health, as an integer percentage of full health.
     * <p>
     * Battery health may be unsupported depending on the drone model and/or firmware versions. <br>
     * Hence, clients of this API should call {@link OptionalInt#isAvailable() isAvailable} method on the returned
     * value to check whether it can be considered valid before use.
     *
     * @return current battery state of health
     *
     * @see OptionalInt
     */
    @NonNull
    OptionalInt getBatteryHealth();

    /**
     * Retrieves the device's current battery cycle count.
     * <p>
     * Battery cycle count may be unsupported depending on the drone model and/or firmware versions. <br>
     * Hence, clients of this API should call {@link OptionalInt#isAvailable() isAvailable} method on the returned
     * value to check whether it can be considered valid before use.
     *
     * @return current battery cycle count
     *
     * @see OptionalInt
     */
    @NonNull
    OptionalInt getBatteryCycleCount();

    /**
     * Retrieves the battery serial number.
     *
     * Battery serial may be unsupported depending on the drone model and/or firmware versions, in which case this
     * method returns {@code null}.
     *
     * @return battery serial, or {@code null} if unsupported
     */
    @Nullable
    String getSerial();
}
