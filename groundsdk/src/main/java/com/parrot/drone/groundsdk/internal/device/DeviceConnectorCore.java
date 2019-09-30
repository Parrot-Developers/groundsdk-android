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

package com.parrot.drone.groundsdk.internal.device;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.device.DeviceConnector;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * DeviceConnector implementation.
 */
public class DeviceConnectorCore extends DeviceConnector {

    /**
     * Specific operations supported by the connector.
     */
    private enum Capabilities {

        /** Connector is able to disconnect a device. */
        DISCONNECT,

        /** Connector is able to forget a device. */
        FORGET
    }

    /**
     * Singleton for local connection through USB.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static final DeviceConnectorCore LOCAL_USB = createLocalConnector(Technology.USB);

    /**
     * Singleton for local connection through Wifi.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static final DeviceConnectorCore LOCAL_WIFI = createLocalConnector(Technology.WIFI);

    /**
     * Singleton for local connection through BLE.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static final DeviceConnectorCore LOCAL_BLE = createLocalConnector(Technology.BLE);

    /**
     * Factory for building RemoteControl connectors.
     *
     * @param uid uid of the remote control device
     *
     * @return a new remote control connector
     */
    @NonNull
    public static DeviceConnectorCore createRCConnector(@NonNull String uid) {
        // RC connectors are (for the moment) always using Wifi technology
        return new DeviceConnectorCore(Type.REMOTE_CONTROL, Technology.WIFI, uid, EnumSet.of(Capabilities.FORGET)) {

            @NonNull
            @Override
            public String toString() {
                return getTechnology() + "-RC [uid: " + uid + "]";
            }
        };
    }

    /**
     * Factory for building a local connector.
     *
     * @param techno technology used.
     *
     * @return a new local connector
     */
    @NonNull
    public static DeviceConnectorCore createLocalConnector(@NonNull Technology techno) {
        return new DeviceConnectorCore(Type.LOCAL, techno, null, EnumSet.of(Capabilities.DISCONNECT)) {

            @NonNull
            @Override
            public String toString() {
                return techno + "-Local";
            }
        };
    }

    /** Type of the connector. */
    @NonNull
    private final Type mType;

    /** Technology used by this connector. */
    private final Technology mTechnology;

    /** Uid of the connector. {@code null} for local connector singleton, RC uid for remote control connectors. */
    @Nullable
    private final String mUid;

    /** Optional operations supported by the connector. */
    @NonNull
    private final Set<Capabilities> mCapabilities;

    /**
     * Constructor.
     *
     * @param type         connector type
     * @param techno       connector technology
     * @param uid          connector uid
     * @param capabilities connector capabilities
     */
    private DeviceConnectorCore(@NonNull Type type, @NonNull Technology techno, @Nullable String uid,
                                @NonNull Set<Capabilities> capabilities) {
        mType = type;
        mTechnology = techno;
        mUid = uid;
        mCapabilities = capabilities;
    }

    @NonNull
    @Override
    public final Type getType() {
        return mType;
    }

    @NonNull
    @Override
    public Technology getTechnology() {
        return mTechnology;
    }

    @Nullable
    @Override
    public final String getUid() {
        return mUid;
    }

    /**
     * Tells whether this connector supports the forget operation.
     *
     * @return {@code true} if forget operation is supported, otherwise {@code false}
     */
    final boolean supportsForget() {
        return mCapabilities.contains(Capabilities.FORGET);
    }

    /**
     * Tells whether this connector supports the disconnect operation.
     *
     * @return {@code true} if disconnect operation is supported, otherwise {@code false}
     */
    final boolean supportsDisconnect() {
        return mCapabilities.contains(Capabilities.DISCONNECT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DeviceConnectorCore that = (DeviceConnectorCore) o;

        return mType == that.mType && Objects.equals(mUid, that.mUid) && mTechnology == that.mTechnology;
    }

    @Override
    public int hashCode() {
        int result = mType.hashCode();
        result = 31 * result + (mUid != null ? mUid.hashCode() : 0) * 7 + mTechnology.hashCode();
        return result;
    }
}
