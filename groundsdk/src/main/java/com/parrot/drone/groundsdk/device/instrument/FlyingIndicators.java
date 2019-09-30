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

/**
 * Instrument that informs about flying states.
 * <p>
 * This instrument can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getInstrument(FlyingIndicators.class)}</pre>
 *
 * @see Drone#getInstrument(Class)
 * @see Drone#getInstrument(Class, Ref.Observer)
 */
public interface FlyingIndicators extends Instrument {

    /**
     * Flying indicators state.
     */
    enum State {

        /**
         * Drone is landed.
         * <p>
         * It can be in initialization state or it can be waiting for a command or a user action to takeoff.
         *
         * @see LandedState
         */
        LANDED,

        /**
         * Drone is flying.
         *
         * @see FlyingState
         */
        FLYING,

        /** Drone has detected defective sensor(s) and is doing an emergency landing. */
        EMERGENCY_LANDING,

        /** Drone stopped flying due to an emergency. */
        EMERGENCY
    }

    /**
     * Detailed state when the main state is {@link State#LANDED}.
     */
    enum LandedState {

        /** Main state is not {@link State#LANDED}. */
        NONE,

        /**
         * Drone is initializing and not ready to takeoff,
         * for instance because it's waiting for some peripheral calibration.
         * Drone motors are not running.
         */
        INITIALIZING,

        /**
         * Drone is ready to initialize a take-off, by requesting either: <ul>
         * <li>a take-off for a copter,</li>
         * <li>a thrown take-off for a copter,</li>
         * <li>a take-off arming for a fixed wings drone.</li>
         * </ul>
         * On some drones, motors may be running.
         */
        IDLE,

        /**
         * Motors are ramping, the drone is going to take off.
         */
        MOTOR_RAMPING,

        /**
         * Drone is waiting for a user action to takeoff.
         * It's waiting to be thrown.
         * Drone motors are running.
         */
        WAITING_USER_ACTION
    }

    /**
     * Detailed state when the main state is {@link State#FLYING}.
     */
    enum FlyingState {

        /** Main state is not {@link State#FLYING}. */
        NONE,

        /** Drone is taking off. */
        TAKING_OFF,

        /** Drone is landing. */
        LANDING,

        /**
         * Drone is waiting for piloting orders. A copter is waiting at its current position, a fix wings is loitering
         * around its current position.
         */
        WAITING,

        /** Drone has piloting orders and is flying. */
        FLYING
    }

    /**
     * Gets the current state.
     *
     * @return the current state
     */
    @NonNull
    State getState();

    /**
     * Gets the current detailed landed state.
     *
     * @return the current landed state
     */
    @NonNull
    LandedState getLandedState();

    /**
     * Gets the current detailed flying state.
     *
     * @return the current flying state
     */
    @NonNull
    FlyingState getFlyingState();
}
