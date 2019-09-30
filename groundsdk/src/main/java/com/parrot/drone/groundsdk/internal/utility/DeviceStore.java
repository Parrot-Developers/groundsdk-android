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

package com.parrot.drone.groundsdk.internal.utility;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.internal.Monitorable;
import com.parrot.drone.groundsdk.internal.device.DeviceCore;

import java.util.Collection;

/**
 * Interface for an utility providing access to a device store.
 */
public interface DeviceStore<D extends DeviceCore> extends Monitorable<DeviceStore.Monitor<? super D>> {

    /**
     * Callback interface receiving store modification notifications.
     *
     * @param <D> type of devices contained in the monitored store
     */
    interface Monitor<D extends DeviceCore> {

        /**
         * Called back when a device is added to the store.
         *
         * @param device the device that was added
         */
        default void onDeviceAdded(@NonNull D device) {
        }

        /**
         * Called back when a device present in the store changes.
         *
         * @param device the device that did change
         */
        default void onDeviceChanged(@NonNull D device) {
        }

        /**
         * Called back when a device is removed from the store.
         *
         * @param device the device that was removed
         */
        default void onDeviceRemoved(@NonNull D device) {
        }

        /**
         * Called back after any modification to the store occurs.
         * <p>
         * This method is always called back after either {@link #onDeviceAdded} or {@link #onDeviceChanged}
         * or {@link #onDeviceRemoved} method is called.
         */
        default void onChange() {
        }
    }

    /**
     * Retrieves a device from the store.
     *
     * @param uid uid of the device to retrieve
     *
     * @return the device with corresponding uid, or {@code null} if no device with such an uid is present in the store
     */
    @Nullable
    D get(@NonNull String uid);

    /**
     * Retrieves all devices present in the store.
     * <p>
     * The returned collection is an immutable view of the devices present in the store. It may change whenever some
     * device changes, is added to, or is removed from the store.
     *
     * @return all devices in the store
     */
    @NonNull
    Collection<D> all();

    /**
     * Adds a device to the store.
     *
     * @param device device to add
     *
     * @return {@code true} if the device was added to the store, {@code false} if a device with the same uid was
     *         already present in the store
     */
    boolean add(@NonNull D device);

    /**
     * Removes a device from the store.
     *
     * @param uid uid of the device to remove
     *
     * @return {@code true} if the device was removed from the store, {@code false} if no device with such an uid was
     *         present in the store
     */
    boolean remove(@NonNull String uid);
}