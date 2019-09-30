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

package com.parrot.drone.groundsdk.device;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Device state information.
 */
public abstract class DeviceState {

    /** Connection state. */
    public enum ConnectionState {

        /** Device is not connected. */
        DISCONNECTED,

        /** Device is connecting. Request to connect to the device has been sent, waiting for confirmation */
        CONNECTING,

        /** Device is connected. */
        CONNECTED,

        /** Device is disconnecting following a user request. */
        DISCONNECTING,
    }

    /** Reason why the device is in the current state. */
    public enum ConnectionStateCause {

        /** No specific cause. Valid for all states. */
        NONE,

        /** Due to an explicit user request. Valid on all states. */
        USER_REQUESTED,

        /**
         * Because the connection with the device has been lost. Valid in {@link ConnectionState#CONNECTING} state when
         * trying to reconnect to the device and {@link ConnectionState#DISCONNECTED} state.
         */
        CONNECTION_LOST,

        /**
         * Device refused the connection because it's already connected to a controller. Only for {@link
         * ConnectionState#DISCONNECTED}
         */
        REFUSED,

        /**
         * Connection failed due to a bad password.
         * <p>
         * Only valid for Disconnected, when connecting using a `RemoteControl` connector.
         */
        BAD_PASSWORD,

        /** Connection has failure. Only for {@link ConnectionState#DISCONNECTED} */
        FAILURE
    }

    /**
     * Gets the device connection state.
     *
     * @return drone connection state
     */
    @NonNull
    public abstract ConnectionState getConnectionState();

    /**
     * Gets the reason why device is in the current state.
     *
     * @return current connection state cause
     */
    @NonNull
    public abstract ConnectionStateCause getConnectionStateCause();

    /**
     * Gets the list of available connectors for this device.
     *
     * @return the device connectors
     */
    @NonNull
    public abstract DeviceConnector[] getConnectors();

    /**
     * Gets the currently active connector.
     * <p>
     * This is the connector using which the current device connection has been established.
     *
     * @return the currently active connector, or {@code null} if the device is not currently connected
     */
    @Nullable
    public abstract DeviceConnector getActiveConnector();

    /**
     * Tells whether the device can be forgotten.
     * <p>
     * A device may be forgotten if it has been connected once using the
     * {@link DeviceConnector.Type#LOCAL local connector} or if any of is available connectors is a
     * {@link DeviceConnector.Type#REMOTE_CONTROL remote control}. <br>
     * In that last case, the device will also be forgotten by the remote control device in question.
     *
     * @return {@code true} if the device can be forgotten, otherwise {@code false}
     */
    public abstract boolean canBeForgotten();

    /**
     * Tells whether the device can be disconnected.
     * <p>
     * A device may be disconnected if it is currently connected through the
     * {@link DeviceConnector.Type#LOCAL local connector}. <br>
     * Devices currently connected through {@link DeviceConnector.Type#REMOTE_CONTROL remote controls}
     * <strong>CANNOT</strong> be disconnected (instead, the remote control itself has to be disconnected, or another
     * device must be connected using that remote control).
     *
     * @return {@code true} if the device can be disconnected, otherwise {@code false}
     */
    public abstract boolean canBeDisconnected();

    /**
     * Tells whether the device can be connected.
     * <p>
     * A device may be connected as long as it is currently disconnected and has available
     * {@link DeviceConnector device connectors} through which to connect.
     *
     * @return {@code true} if the device can be connected, otherwise {@code false}
     */
    public abstract boolean canBeConnected();
}
