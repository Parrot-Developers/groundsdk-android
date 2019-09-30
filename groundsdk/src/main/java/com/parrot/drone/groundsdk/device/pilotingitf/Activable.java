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

package com.parrot.drone.groundsdk.device.pilotingitf;

import androidx.annotation.NonNull;

/**
 * Interface for {@link PilotingItf} components that require activation.
 * <p>
 * An {@code Activable PilotingItf} component needs to be activated before most of its API can be
 * operated (except stated otherwise in the component documentation. This mainly concerns settings APIs).
 * <p>
 * Only one {@code Activable PilotingItf} component may be active at a time when the drone is connected, and all drone
 * provide a default component that is always activated when no other component is active anymore.
 */
public interface Activable {

    /**
     * Piloting interface state.
     * <p>
     * There is only one active piloting interface at a time on a drone.
     */
    enum State {

        /** Piloting interface is available but is not the active one. */
        IDLE,

        /** Piloting interface is the active one. */
        ACTIVE,

        /** Piloting interface is not available at that time. */
        UNAVAILABLE
    }

    /**
     * Gets the piloting interface's current state.
     * <p>
     * There is only one active piloting interface at a time on a drone.
     *
     * @return this piloting interface's current state
     */
    @NonNull
    State getState();

    /**
     * Deactivates this piloting interface.
     * <p>
     * This will activate another piloting interface (usually the default one).
     * </p>
     *
     * @return {@code true} on success, {@code false} in case the piloting interface cannot be deactivated at this point
     */
    boolean deactivate();
}
