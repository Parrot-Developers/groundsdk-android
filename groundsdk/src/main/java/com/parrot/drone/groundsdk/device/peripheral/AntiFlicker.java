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
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.value.EnumSetting;

/**
 * Peripheral managing anti-flickering.
 * <p>
 * Anti-flickering is a global setting of a drone and is used by all drone cameras.
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(AntiFlicker.class)}</pre>
 *
 * @see Drone#getPeripheral(Class)
 * @see Drone#getPeripheral(Class, Ref.Observer)
 */
public interface AntiFlicker extends Peripheral {

    /** Anti-flickering mode. */
    enum Mode {

        /** Anti-flickering is disabled. */
        OFF,

        /** Anti-flickering set to 50Hz. */
        HZ_50,

        /** Anti-flickering set to 60Hz. */
        HZ_60,

        /** Anti-flickering is automatically either managed by the drone or based on the location. */
        AUTO
    }

    /** Anti-flickering value. */
    enum Value {

        /** Unknown anti-flickering value. */
        UNKNOWN,

        /** Anti-flickering is disabled. */
        OFF,

        /** Anti-flickering set to 50Hz. */
        HZ_50,

        /** Anti-flickering set to 60Hz. */
        HZ_60,
    }

    /**
     * Retrieves the anti-flickering mode.
     *
     * @return current anti-flickering mode
     */
    @NonNull
    EnumSetting<Mode> mode();

    /**
     * Retrieves the actual anti-flickering value.
     * <p>
     * Useful when mode is {@link Mode#AUTO}.
     *
     * @return actual anti-flickering value
     */
    @NonNull
    Value value();
}
