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

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Data class internally representing a device.
 */
public class DeviceCore {

    /** Delegate executing action on Device. */
    public interface Delegate {

        /**
         * Removes the device from known devices list and clear all its stored data.
         *
         * @return {@code true} if the device has been forgotten, {@code false} otherwise.
         */
        boolean forget();

        /**
         * Connects the device.
         *
         * @param connector connector to use to establish the connection
         * @param password  password to use for authentication. Use {@code null} if the device connection is not
         *                  secured, or to use the provider's saved password, if any (for RC providers)
         *
         * @return {@code true} if the connection process has started, {@code false} otherwise.
         */
        boolean connect(@NonNull DeviceConnector connector, @Nullable String password);

        /**
         * Disconnects the device.
         * <p>
         * This method can be used to disconnect the device when connected or to cancel the connection process if the
         * device is currently connecting.
         *
         * @return {@code true} if the disconnection process has started, {@code false} otherwise.
         */
        boolean disconnect();
    }

    /** Device uid. */
    @NonNull
    private final String mUid;

    /** Device model. */
    @NonNull
    private final DeviceModel mModel;

    /** Engine specific delegate. */
    @NonNull
    private final Delegate mDelegate;

    /** Device name holder. */
    @NonNull
    private final DeviceNameHolder mName;

    /** Device firmware version holder. */
    @NonNull
    private final DeviceFirmwareVersionHolder mFirmwareVersion;

    /** Device board identifier holder. */
    @NonNull
    private final DeviceBoardIdHolder mBoardId;

    /** Device state holder. */
    @NonNull
    private final DeviceStateHolder mState;

    /** Instruments store. */
    @NonNull
    private final ComponentStore<Instrument> mInstruments;

    /** Peripheral store. */
    @NonNull
    private final ComponentStore<Peripheral> mPeripherals;

    /**
     * Constructor.
     *
     * @param uid      device unique identifier
     * @param model    device model
     * @param name     device name
     * @param delegate device engine-specific delegate
     */
    DeviceCore(@NonNull String uid, @NonNull DeviceModel model, @NonNull String name,
               @NonNull Delegate delegate) {
        mUid = uid;
        mModel = model;
        mName = new DeviceNameHolder(name);
        mFirmwareVersion = new DeviceFirmwareVersionHolder();
        mBoardId = new DeviceBoardIdHolder();
        mState = new DeviceStateHolder();
        mDelegate = delegate;
        mInstruments = new ComponentStore<>();
        mPeripherals = new ComponentStore<>();
    }

    /**
     * Destroys the device.
     */
    @CallSuper
    void destroy() {
        mName.destroy();
        mFirmwareVersion.destroy();
        mBoardId.destroy();
        mState.destroy();
        mInstruments.destroy();
        mPeripherals.destroy();
    }

    /**
     * Gets the device uid.
     * <p>
     * Device uid uniquely identify a device, and is persistent between sessions.
     *
     * @return device uid
     */
    @NonNull
    public final String getUid() {
        return mUid;
    }

    /**
     * Gets the device model.
     * <p>
     * Model is set when the device instance is created and never changes.
     *
     * @return drone model
     */
    @NonNull
    public DeviceModel getModel() {
        return mModel;
    }

    /**
     * Gets the device name.
     *
     * @return device name
     */
    @NonNull
    public final String getName() {
        return mName.get();
    }

    /**
     * Gets the device firmware version.
     *
     * @return device firmware version
     */
    @NonNull
    public final FirmwareVersion getFirmwareVersion() {
        return mFirmwareVersion.get();
    }

    /**
     * Gets the device board id.
     *
     * @return device board id if available, otherwise {@code null} if not queried yet or empty if known to be
     *         unavailable
     */
    @Nullable
    public final String getBoardId() {
        return mBoardId.get();
    }

    /**
     * Gets the device state core.
     *
     * @return device state core
     */
    @NonNull
    public final DeviceStateCore getDeviceStateCore() {
        return mState.get();
    }

    /**
     * Gets the instrument store.
     *
     * @return the instrument store
     */
    @NonNull
    public final ComponentStore<Instrument> getInstrumentStore() {
        return mInstruments;
    }

    /**
     * Gets the peripheral store.
     *
     * @return the peripheral store
     */
    @NonNull
    public final ComponentStore<Peripheral> getPeripheralStore() {
        return mPeripherals;
    }

    /**
     * Forgets the device.
     * <p>
     * Persisted device data are deleted and the device is removed if it's not visible.
     *
     * @return {@code true} if the device has been forgotten, {@code false} otherwise.
     */
    public final boolean forget() {
        return mDelegate.forget();
    }

    /** A filter that accepts only {@link DeviceConnector.Type#REMOTE_CONTROL remote control} connectors. */
    private static final Predicate<DeviceConnector> RC_CONNECTORS = connector ->
            connector.getType() == DeviceConnector.Type.REMOTE_CONTROL;

    /**
     * A filter that accepts only {@link DeviceConnector.Type#LOCAL local}
     * {@link DeviceConnector.Technology#USB USB} connectors.
     */
    private static final Predicate<DeviceConnector> LOCAL_USB_CONNECTORS = connector ->
            connector.getType() == DeviceConnector.Type.LOCAL
            && connector.getTechnology() == DeviceConnector.Technology.USB;

    /**
     * A filter that accepts only {@link DeviceConnector.Type#LOCAL local}
     * {@link DeviceConnector.Technology#WIFI WIFI} connectors.
     */
    private static final Predicate<DeviceConnector> LOCAL_WIFI_CONNECTORS = connector ->
            connector.getType() == DeviceConnector.Type.LOCAL
            && connector.getTechnology() == DeviceConnector.Technology.WIFI;

    /**
     * Connects the device.
     *
     * @param connector the connector through which to establish the connection. Use {@code null} to use the best
     *                  available connector
     * @param password  password to use for authentication. Use {@code null} if the device connection is not
     *                  secured, or to use the provider's saved password, if any (for RC providers)
     *
     * @return {@code true} if the connection process has started, {@code false} otherwise, for example if the device is
     *         no more visible.
     */
    public final boolean connect(@Nullable DeviceConnector connector, @Nullable String password) {
        if (connector == null) {
            // select best connector
            DeviceStateCore state = getDeviceStateCore();
            DeviceConnector[] connectors = state.getConnectors();
            // if there is only one available connector, use this one
            if (connectors.length == 1) {
                connector = connectors[0];
            } else {
                // otherwise, search for a single connector of a given kind: first among RC connectors, then
                // LOCAL USB connectors, and finally LOCAL WIFI connectors, in that order.
                for (Predicate<DeviceConnector> filter : Arrays.asList(
                        RC_CONNECTORS, LOCAL_USB_CONNECTORS, LOCAL_WIFI_CONNECTORS)) {
                    connectors = state.getConnectors(filter);
                    if (connectors.length > 0) {
                        // if there is exactly one connector of a kind, then use it
                        connector = connectors.length == 1 ? connectors[0] : null;
                        // in any case, connector search is over
                        break;
                    }
                    // if there is no connector of a given kind, proceed with next kind
                }
            }
        }
        return connector != null && mDelegate.connect(connector, password);
    }

    /**
     * Disconnects the device.
     * <p>
     * This method can be use to disconnect the device when connected or to cancel the connection process if the device
     * is connecting.
     *
     * @return {@code true} if the disconnection process has started, {@code false} otherwise.
     */
    public final boolean disconnect() {
        return mDelegate.disconnect();
    }

    /**
     * Updates device name.
     *
     * @param newName new name
     */
    public final void updateName(@NonNull String newName) {
        mName.update(newName);
    }

    /**
     * Updates device firmware version.
     *
     * @param newVersion new firmware version
     */
    public final void updateFirmwareVersion(@NonNull FirmwareVersion newVersion) {
        mFirmwareVersion.update(newVersion);
    }

    /**
     * Updates device board identifier.
     *
     * @param boardId new board id
     */
    public final void updateBoardId(@NonNull String boardId) {
        mBoardId.update(boardId);
    }

    /**
     * Gets the holder for the device name.
     * <p>
     * Used to get the current device name, and to register to device name updates when the latter changes.
     *
     * @return the holder for the device name
     */
    @NonNull
    final DeviceNameHolder getNameHolder() {
        return mName;
    }

    /**
     * Gets the holder for the device firmware version.
     * <p>
     * Used to get the current device firmware version, and to register to device firmware version updates when the
     * latter changes.
     *
     * @return the holder for the device firmware version
     */
    @NonNull
    final DeviceFirmwareVersionHolder getFirmwareVersionHolder() {
        return mFirmwareVersion;
    }

    /**
     * Gets the holder for the device board identifier.
     * <p>
     * Used to get the current device board id, and to register to device board id updates when the latter changes.
     *
     * @return the holder for the device board id
     */
    @NonNull
    final DeviceBoardIdHolder getBoardIdHolder() {
        return mBoardId;
    }

    /**
     * Gets the holder for the device state.
     * <p>
     * Used to get the current device state, and to register to device state updates when the latter changes.
     *
     * @return the holder for the device state
     */
    @NonNull
    final DeviceStateHolder getStateHolder() {
        return mState;
    }

    @Override
    public final String toString() {
        return mUid + " [name: " + getName() + ", model: " + mModel + ", state: " + getDeviceStateCore() + "]";
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param prefix prefix string (usually indent) to prepend to each written dump line
     */
    final void dump(@NonNull PrintWriter writer, @NonNull String prefix) {
        writer.write(prefix + mUid + "\n");
        writer.write(prefix + "\tModel: " + mModel + "\n");
        writer.write(prefix + "\tName: " + mName.get() + "\n");
        mState.get().dump(writer, prefix + "\t");
    }
}
