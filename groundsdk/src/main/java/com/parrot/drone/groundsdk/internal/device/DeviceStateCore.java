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

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceState;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Mutable DeviceState implementation.
 */
public final class DeviceStateCore extends DeviceState {

    /**
     * Listener notified when the state is mutated.
     */
    public interface Listener {

        /**
         * Notifies that device state has been mutated.
         */
        void onUpdated();
    }

    /** Listener notified when the state is mutated. */
    @NonNull
    private final Listener mListener;

    /** Connection state. */
    @NonNull
    private ConnectionState mConnectionState;

    /** Connection state cause. */
    @NonNull
    private ConnectionStateCause mConnectionStateCause;

    /** Available device connectors. */
    @NonNull
    private Set<DeviceConnectorCore> mDeviceConnectors;

    /** Currently active device connector, {@code null} if disconnected. */
    @Nullable
    private DeviceConnectorCore mActiveConnector;

    /** {@code true} when device info is recorded in persistent storage. */
    private boolean mPersisted;

    /** {@code true} when one of the available device connectors supports the forget operation. */
    private boolean mAnyConnectorSupportsForget;

    /** Has pending changes waiting for {@link #notifyUpdated()} call. */
    private boolean mChanged;

    /**
     * Constructor.
     *
     * @param listener notified when state is mutated
     */
    DeviceStateCore(@NonNull Listener listener) {
        mListener = listener;
        mConnectionState = ConnectionState.DISCONNECTED;
        mConnectionStateCause = ConnectionStateCause.NONE;
        mDeviceConnectors = Collections.emptySet();
    }

    @NonNull
    @Override
    public ConnectionState getConnectionState() {
        return mConnectionState;
    }

    @Override
    @NonNull
    public ConnectionStateCause getConnectionStateCause() {
        return mConnectionStateCause;
    }

    @Override
    @NonNull
    public DeviceConnector[] getConnectors() {
        return mDeviceConnectors.toArray(new DeviceConnector[0]);
    }

    /**
     * Gets the list of available connectors for this device, filtered as specified.
     *
     * @param filter filter used to select connectors to include in the returned list
     *
     * @return the filtered list of available device connectors
     */
    @NonNull
    DeviceConnector[] getConnectors(@NonNull Predicate<DeviceConnector> filter) {
        ArrayList<DeviceConnector> connectors = new ArrayList<>();
        for (DeviceConnector connector : mDeviceConnectors) {
            if (filter.test(connector)) {
                connectors.add(connector);
            }
        }
        return connectors.toArray(new DeviceConnector[0]);
    }

    @Nullable
    @Override
    public DeviceConnector getActiveConnector() {
        return mActiveConnector;
    }

    @Override
    public boolean canBeForgotten() {
        return mPersisted || mAnyConnectorSupportsForget;
    }

    @Override
    public boolean canBeDisconnected() {
        return mActiveConnector != null && mActiveConnector.supportsDisconnect()
               && mConnectionState != ConnectionState.DISCONNECTED
               && mConnectionState != ConnectionState.DISCONNECTING;
    }

    @Override
    public boolean canBeConnected() {
        return mConnectionState == ConnectionState.DISCONNECTED && !mDeviceConnectors.isEmpty();
    }

    /**
     * Tells whether device info is recorded in persistent storage.
     *
     * @return {@code true} if device info is recorded in persistent storage, {@code false} otherwise
     */
    public boolean isPersisted() {
        return mPersisted;
    }

    /**
     * Changes device connection state.
     * <p>
     * Note that changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param state new State
     *
     * @return this, to allow call chaining
     */
    public DeviceStateCore updateConnectionState(@NonNull ConnectionState state) {
        if (mConnectionState != state) {
            mConnectionState = state;
            mChanged = true;
        }
        return this;
    }

    /**
     * Changes device connection state and cause.
     * <p>
     * Note that changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param state new State
     * @param cause new connections state cause
     *
     * @return this, to allow call chaining
     */
    public DeviceStateCore updateConnectionState(@NonNull ConnectionState state, @Nullable ConnectionStateCause cause) {
        mChanged = (mConnectionState != state) || (mConnectionStateCause != cause);
        mConnectionState = state;
        if (cause != null) {
            mConnectionStateCause = cause;
        }
        return this;
    }

    /**
     * Updates the list of currently available device connectors.
     *
     * @param connectors new list of connectors
     *
     * @return this, to allow call chaining
     */
    public DeviceStateCore updateConnectors(@NonNull Set<DeviceConnectorCore> connectors) {
        mDeviceConnectors = connectors;
        mAnyConnectorSupportsForget = false;
        for (DeviceConnectorCore connector : mDeviceConnectors) {
            if (connector.supportsForget()) {
                mAnyConnectorSupportsForget = true;
                break;
            }
        }
        mChanged = true;
        return this;
    }

    /**
     * Updates the currently active device connector.
     *
     * @param activeConnector new active device connector, {@code null} if no active connector
     *
     * @return this, to allow call chaining
     */
    public DeviceStateCore updateActiveConnector(@Nullable DeviceConnectorCore activeConnector) {
        if (activeConnector != mActiveConnector) {
            mChanged = true;
            mActiveConnector = activeConnector;
        }
        return this;
    }

    /**
     * Updates current persisted status of the device.
     *
     * @param persisted {@code true} if the device is recorded in persistent storage, otherwise {@code false}
     *
     * @return this, to allow call chaining
     */
    public DeviceStateCore updatePersisted(boolean persisted) {
        if (persisted != mPersisted) {
            mChanged = true;
            mPersisted = persisted;
        }
        return this;
    }

    /**
     * Notifies changes made by previously called setters.
     */
    public void notifyUpdated() {
        if (mChanged) {
            mChanged = false;
            mListener.onUpdated();
        }
    }

    @Override
    public String toString() {
        String activeConnectorStr = (mActiveConnector != null) ? " [active = " + mActiveConnector + "]" : "";
        return mConnectionState + " " + mConnectionStateCause + " " + (mPersisted ? "KNOWN" : "UNKNOWN") +
               " [" + TextUtils.join(", ", mDeviceConnectors) + "]" + activeConnectorStr;
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param prefix prefix string (usually indent) to prepend to each written dump line
     */
    public void dump(@NonNull PrintWriter writer, @NonNull String prefix) {
        writer.write(prefix + "State: " + mConnectionState + " [cause: " + mConnectionStateCause + "]\n");
        List<String> operations = new ArrayList<>();
        if (canBeConnected()) {
            operations.add("CONNECT");
        }
        if (canBeDisconnected()) {
            operations.add("DISCONNECT");
        }
        if (canBeForgotten()) {
            operations.add("FORGET");
        }
        writer.write(prefix + "Available operations: " + TextUtils.join(", ", operations) + "\n");
        writer.write(prefix + "Connectors: " + TextUtils.join(", ", mDeviceConnectors)
                     + " [active: " + mActiveConnector + "]\n");
    }
}
