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

import com.parrot.drone.groundsdk.GroundSdk;
import com.parrot.drone.groundsdk.internal.session.Session;
import com.parrot.drone.groundsdk.internal.utility.DeviceStore;

/**
 * A ref for a listener notified when some device is removed from a store.
 */
public final class DeviceRemovedListenerRef extends Session.RefBase<Void> {

    /**
     * Registers a listener to be notified when a device is removed.
     *
     * @param session  session that will manage the ref associated to the listener
     * @param store    store where the device is registered
     * @param uid      uid of the device to observe
     * @param listener listener to be notified when the device is removed
     *
     * @return a ref for the registered listener
     */
    @NonNull
    public static DeviceRemovedListenerRef register(@NonNull Session session, @NonNull DeviceStore<?> store,
                                                    @NonNull String uid,
                                                    @NonNull GroundSdk.OnDeviceRemovedListener listener) {
        return new DeviceRemovedListenerRef(session, store, uid, listener);
    }

    /** Store where the device is registered. */
    @NonNull
    private final DeviceStore<?> mStore;

    /** Uid of the device to observe. */
    @NonNull
    private final String mDeviceUid;

    /**
     * Constructor.
     *
     * @param session  session that will manage this ref
     * @param store    device store where the device may be found
     * @param uid      uid of the device to observe
     * @param listener listener to notify when the device is removed
     */
    private DeviceRemovedListenerRef(@NonNull Session session, @NonNull DeviceStore<?> store,
                                     @NonNull String uid,
                                     @NonNull GroundSdk.OnDeviceRemovedListener listener) {
        super(session, obj -> listener.onDeviceRemoved(uid));
        mStore = store;
        mDeviceUid = uid;
        mStore.monitorWith(mStoreMonitor);
    }

    @Override
    protected void release() {
        mStore.disposeMonitor(mStoreMonitor);
        super.release();
    }

    /** Watches the device store to figure whether the device is removed. */
    private final DeviceStore.Monitor<DeviceCore> mStoreMonitor = new DeviceStore.Monitor<DeviceCore>() {

        @Override
        public void onDeviceRemoved(@NonNull DeviceCore deviceCore) {
            if (mDeviceUid.equals(deviceCore.getUid())) {
                update(null);
                close();
            }
        }
    };
}
