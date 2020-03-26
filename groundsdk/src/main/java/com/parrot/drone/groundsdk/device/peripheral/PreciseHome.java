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
 * Precise Home peripheral interface for drones.
 * <p>
 * Precise home allows the drone to more precisely reach the take-off location when landing or returning home.<br>
 * To do so, {@code PreciseHome} peripheral must be {@link PreciseHome#mode() enabled}.
 * As precise home is not always practically feasible, this peripheral reports a current state which
 * indicates whether precise home will be activated upon landing, or if it is currently active (in
 * case the drone is currently landing).
 * <p>
 * Note that this peripheral may be unsupported, depending on the drone model and firmware version.
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(PreciseHome.class)}</pre>
 *
 * @see Drone#getPeripheral(Class)
 * @see Drone#getPeripheral(Class, Ref.Observer)
 */
public interface PreciseHome extends Peripheral {

    /** Precise home mode. */
    enum Mode {

        /** Precise home is disabled. */
        DISABLED,

        /** Precise home is enabled, in standard mode. */
        STANDARD
    }

    /**
     * Gives access to the precise home mode setting.
     * <p>
     * This setting allows to change the current precise home mode.
     *
     * @return precise home mode setting
     */
    @NonNull
    EnumSetting<Mode> mode();

    /** Precise home state. */
    enum State {

        /** Precise home will not be activated if the drone lands now. */
        UNAVAILABLE,

        /** Precise home will be activated if the drone lands now. */
        AVAILABLE,

        /** Precise home is currently happening. */
        ACTIVE
    }

    /**
     * Informs about current precise home state.
     *
     * @return current precise home state
     */
    @NonNull
    State state();
}
