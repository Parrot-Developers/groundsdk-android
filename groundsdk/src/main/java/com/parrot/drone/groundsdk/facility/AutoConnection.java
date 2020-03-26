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

package com.parrot.drone.groundsdk.facility;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;

/**
 * Facility that provides control of automatic connection for {@link Drone drone} and
 * {@link RemoteControl remote control} devices.
 * <p>
 * The goal of auto-connection is to ensure that exactly one remote control and exactly one drone are connected
 * whenever it is possible to do so, based on rules as described below.
 * <p>
 * GroundSdk may be configured so that auto-connection is started automatically once the first
 * {@link GroundSdk session} is opened. <br>
 * This behaviour is enabled by setting {@code auto_connection_at_startup} configuration flag to {@code true}, and
 * disabled (which is the default) by setting this flag to {@code false}.
 * <p>
 * Auto-connection may also be {@link #start started} and {@link #stop stopped} manually though this {@code Facility}
 * API. <br>
 * Note however that, in any case, auto-connection is stopped as soon as the last opened GroundSdk session is closed.
 * <h2>Remote control auto-connection</h2>
 * When started, auto-connection will try to always maintain one, and only one remote control connected. It will pick
 * one remote control amongst all currently visible devices and ensure it is connecting or connected, and will connect
 * it otherwise. <br>
 * All other connected remote control devices are forcefully disconnected.
 * <p>
 * To chose which device will get connected, visible remote control devices are sorted by connector technology:
 * {@link DeviceConnector.Technology#USB USB} is considered better than {@link DeviceConnector.Technology#WIFI Wifi},
 * which is considered better than {@link DeviceConnector.Technology#BLE Bluetooth Low-Energy}. The best available
 * remote control is picked up according to this criteria and will be auto-connected.
 * <p>
 * Also, if the best available remote control is currently connected (or connecting) and an even better connector
 * becomes available for it, then it will be auto-reconnected (that is, disconnected, then connected again) using this
 * better connector.
 *
 * <h2>Drone auto-connection</h2>
 * When started, auto-connection will try to always maintain at least one, and only one drone connected.
 * <p>
 * Two different cases must be distinguished: <ul>
 * <li><u>When no remote control is currently connected</u>, then drone auto-connection behaves like remote
 * control auto-connection: drones are sorted by connector technology and the drone with best technology is
 * elected for auto-connection. Auto-reconnection using an even better connector may also happen, as for remote
 * control devices.</li>
 * <li><u>When a remote control is connected (or connecting)</u>, then auto-connection will ensure that no drones
 * are connected through any other connector (including local ones: WIFI or BLE) than this remote control.
 * Any drone that is currently connected or connecting though one of these connectors will get forcefully
 * disconnected. <br>
 * If, by the time the remote control gets auto-connected, some drone is already connected through a local
 * connector, then auto-connection will try to connect it through the remote control if the latter also knows
 * and sees that drone; otherwise auto-connection lets the remote control decide which drone to connect.</li>
 * </ul>
 */
public interface AutoConnection extends Facility {

    /**
     * Starts automatic devices connection.
     *
     * @return {@code true} if auto-connection will effectively start, otherwise {@code false}
     */
    boolean start();

    /**
     * Stops automatic devices connection.
     *
     * @return {@code true} if auto-connection will effectively stop, otherwise {@code false}
     */
    boolean stop();

    /**
     * Automatic connection status.
     */
    enum Status {

        /** Automatic connection is stopped. */
        STOPPED,

        /** Automatic connection is started, and may spontaneously connect or disconnect any device. */
        STARTED
    }

    /**
     * Retrieves current automatic connection status.
     *
     * @return current status
     */
    @NonNull
    Status getStatus();

    /**
     * Retrieves the remote control device currently elected for automatic connection.
     *
     * @return currently elected remote control, or {@code null} if none is
     */
    @Nullable
    RemoteControl getRemoteControl();

    /**
     * Retrieves the drone device currently elected for automatic connection.
     *
     * @return currently elected drone, or {@code null} if none is
     */
    @Nullable
    Drone getDrone();
}
