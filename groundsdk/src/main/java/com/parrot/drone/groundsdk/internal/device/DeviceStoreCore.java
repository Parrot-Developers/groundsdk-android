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

import com.parrot.drone.groundsdk.internal.utility.DeviceStore;
import com.parrot.drone.groundsdk.internal.utility.DroneStore;
import com.parrot.drone.groundsdk.internal.utility.RemoteControlStore;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Stores all known devices of a specific type.
 *
 * @param <D> type of devices in that store
 */
public class DeviceStoreCore<D extends DeviceCore> implements DeviceStore<D> {

    /** Specialization of a {@code DeviceStoreCore} that contains {@code DroneCore} devices. */
    public static final class Drone extends DeviceStoreCore<DroneCore> implements DroneStore {}

    /** Specialization of a {@code DeviceStoreCore} that contains {@code RemoteControlCore} devices. */
    public static final class RemoteControl extends DeviceStoreCore<RemoteControlCore> implements RemoteControlStore {}

    /** Map of stored devices, by uid. */
    @NonNull
    private final Map<String, D> mDevices;

    /** Listeners list. */
    @NonNull
    private final Set<Monitor<? super D>> mMonitors;

    /**
     * Constructor.
     */
    private DeviceStoreCore() {
        mDevices = new HashMap<>();
        mMonitors = new CopyOnWriteArraySet<>();
    }

    @Override
    public final void monitorWith(@NonNull DeviceStore.Monitor<? super D> monitor) {
        mMonitors.add(monitor);
    }

    @Override
    public final void disposeMonitor(@NonNull DeviceStore.Monitor<? super D> monitor) {
        mMonitors.remove(monitor);
    }

    @Override
    @Nullable
    public final D get(@NonNull String uid) {
        return mDevices.get(uid);
    }

    @NonNull
    @Override
    public final Collection<D> all() {
        return Collections.unmodifiableCollection(mDevices.values());
    }

    /**
     * Adds a device to the store.
     *
     * @param device the device to add
     */
    @Override
    public final boolean add(@NonNull D device) {
        String uid = device.getUid();
        if (mDevices.containsKey(uid)) {
            return false;
        }
        // add the device in the store
        mDevices.put(uid, device);
        // notify listeners
        mMonitors.forEach(monitor -> {
            // ensure monitor has not been removed while iterating
            if (mMonitors.contains(monitor)) {
                monitor.onDeviceAdded(device);
                monitor.onChange();
            }
        });

        // observe the device for name change
        device.getNameHolder().registerObserver(it -> notifyDeviceChanged(device));
        // observe the device for firmware version change
        device.getFirmwareVersionHolder().registerObserver(it -> notifyDeviceChanged(device));
        // observe the device for board identifier change
        device.getBoardIdHolder().registerObserver(it -> notifyDeviceChanged(device));
        // observe the device for state change
        device.getStateHolder().registerObserver(it -> notifyDeviceChanged(device));
        return true;
    }

    @Override
    public final boolean remove(@NonNull String uid) {
        D device = mDevices.remove(uid);
        // remove the device from the store
        if (device == null) {
            return false;
        }
        // notify listeners
        mMonitors.forEach(monitor -> {
            // ensure monitor has not been removed while iterating
            if (mMonitors.contains(monitor)) {
                monitor.onDeviceRemoved(device);
                monitor.onChange();
            }
        });

        // unregister all device observers (including the store's own name/state observers)
        device.destroy();
        return true;
    }

    /**
     * Notifies all registered listeners that the given device state or name has changed.
     *
     * @param device the device that changed
     */
    private void notifyDeviceChanged(@NonNull D device) {
        mMonitors.forEach(monitor -> {
            // ensure monitor has not been removed while iterating
            if (mMonitors.contains(monitor)) {
                monitor.onDeviceChanged(device);
                monitor.onChange();
            }
        });
    }

    /**
     * Debug dump.
     *
     * @param writer   writer to dump to
     * @param typeName display name representing the type of device in the store
     */
    public final void dump(@NonNull PrintWriter writer, @NonNull String typeName) {
        writer.write(typeName + ": " + mDevices.size() + "\n");
        for (D device : mDevices.values()) {
            device.dump(writer, "\t");
        }
    }
}
