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
 * Identifies device connection providers.
 * <p>
 * Device connectors are used when connecting devices, allowing to specify using which connection provider the device
 * must be connected.
 * <p>
 * The list of available connectors for a device can be retrieved from the device state.
 */
public abstract class DeviceConnector {

    /**
     * Device connector types.
     */
    public enum Type {

        /** Connect using local connectivity. */
        LOCAL,

        /** Connect using a remote control. */
        REMOTE_CONTROL
    }

    /**
     * Technology used to connect.
     */
    public enum Technology {

        /** Connect using Wifi. */
        WIFI,

        /** Connect using Usb. */
        USB,

        /** Connect using Bluetooth Low Energy. */
        BLE
    }

    /**
     * Gets the connector type.
     *
     * @return connector type
     */
    @NonNull
    public abstract Type getType();

    /**
     * Gets the technology type.
     *
     * @return the technology of this connector
     */
    @NonNull
    public abstract Technology getTechnology();

    /**
     * Gets the connector uid.
     * <ul>
     * <li>For {@link Type#REMOTE_CONTROL} connectors, this is the uid of the corresponding {@link RemoteControl}.</li>
     * <li>For the {@link Type#LOCAL} connector, this is {@code null}.</li>
     * </ul>
     *
     * @return connector uid
     */
    @Nullable
    public abstract String getUid();
}
