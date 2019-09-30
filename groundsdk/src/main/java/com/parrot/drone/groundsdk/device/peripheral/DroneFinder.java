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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;

import java.util.List;

/**
 * DroneFinder peripheral interface for RemoteControl devices.
 * <p>
 * Allows scanning for visible drones and provides a way to connect to such discovered drones.
 * <p>
 * This peripheral can be obtained from a {@link RemoteControl remote control} using:
 * <pre>{@code remoteControl.getPeripheral(DroneFinder.class)}</pre>
 *
 * @see RemoteControl#getPeripheral(Class)
 * @see RemoteControl#getPeripheral(Class, Ref.Observer)
 */
public interface DroneFinder extends Peripheral {

    /**
     * Represents a remote drone seen during discovery.
     */
    abstract class DiscoveredDrone {

        /**
         * Drone connection security type.
         */
        public enum ConnectionSecurity {

            /** Drone is not secured, i.e. it can be connected to without password. */
            NONE,

            /** Drone is secured, i.e. the user is required to provide a password for connection. */
            PASSWORD,

            /**
             * Drone is secured, yet the {@link RemoteControl} device that discovered it has a stored password to
             * use for connection, so the user is not required to provide a password for connection.
             * <p>
             * Note however, that the RC's saved password might be wrong and the user might need to fallback providing
             * a proper password for connection.
             */
            SAVED_PASSWORD
        }

        /**
         * Gets the drone uid.
         *
         * @return discovered drone uid
         */
        @NonNull
        public abstract String getUid();

        /**
         * Gets the drone model.
         *
         * @return discovered drone model
         */
        @NonNull
        public abstract Drone.Model getModel();

        /**
         * Gets the drone name.
         *
         * @return discovered drone name
         */
        @NonNull
        public abstract String getName();

        /**
         * Gets the security the drone connection is secured with.
         *
         * @return discovered drone connection security
         */
        @NonNull
        public abstract ConnectionSecurity getConnectionSecurity();

        /**
         * Gets the Receives Signal Strength Indicator (RSSI) observed when the drone was discovered, expressed in dBm.
         *
         * @return discovered drone RSSI
         */
        @IntRange(to = 0)
        public abstract int getRssi();

        /**
         * Tells whether the discovered drone is known by the remote control.
         *
         * @return {@code true} if the discovered drone is known, otherwise {@code false}
         */
        public abstract boolean isKnown();
    }

    /**
     * DroneFinder state.
     */
    enum State {
        /** Not scanning for drone at the moment. */
        IDLE,

        /** Currently scanning for visible drones. */
        SCANNING
    }

    /**
     * Gets the current state.
     *
     * @return current state
     */
    @NonNull
    State getState();

    /**
     * Gets the list of drones discovered during last discovery.
     * <p>
     * List of discovered drones is initially empty and can by populated using {@link #refresh()}
     * or cleared again using {@link #clear()}
     *
     * @return the current list of discovered drones
     *
     * @see #refresh()
     * @see #clear()
     */
    @NonNull
    List<DiscoveredDrone> getDiscoveredDrones();

    /**
     * Clears the current list of discovered drones.
     * <p>
     * After calling this method, {@link #getDiscoveredDrones()} will return an empty list
     *
     * @see #getDiscoveredDrones()
     */
    void clear();

    /**
     * Asks for an update of the list of discovered drones.
     * <p>
     * Calling this method will start the discovery process, which begins with the {@link #getState() state} transiting
     * to {@link State#SCANNING}. <br>
     * A soon as the state transits beak to {@link State#IDLE}, {@link #getDiscoveredDrones()} can be used to retrieve
     * the updated list of discovered drones.
     *
     * @see #getDiscoveredDrones()
     */
    void refresh();

    /**
     * Connects a discovered drone.
     *
     * @param drone discovered drone to connect
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the drone
     *         is not visible anymore.
     */
    boolean connect(@NonNull DiscoveredDrone drone);

    /**
     * Connects a discovered drone using a password.
     *
     * @param drone    discovered drone to connect
     * @param password password to use for connection
     *
     * @return {@code true} if the connection process has started, otherwise {@code false}, for example if the drone
     *         is not visible anymore.
     */
    boolean connect(@NonNull DiscoveredDrone drone, @NonNull String password);
}
