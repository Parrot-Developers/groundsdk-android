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

package com.parrot.drone.sdkcore.arsdk.backend;

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;

/**
 * Wrapper on native arsdk discovery.
 */
public abstract class ArsdkDiscovery {

    /** Pointer to native discovery . */
    private long mNativePtr;

    /**
     * Constructor.
     *
     * @param arsdkCore        arsdk ctrl instance owning this discovery
     * @param backendNativePtr pointer to native backend to run this discovery on
     * @param name             discovery name
     */
    protected ArsdkDiscovery(@NonNull ArsdkCore arsdkCore, long backendNativePtr, @NonNull String name) {
        mNativePtr = nativeNew(arsdkCore.getNativePtr(), name, backendNativePtr);
        if (mNativePtr == 0) {
            throw new RuntimeException("native create fail");
        }
    }

    /**
     * Destructor.
     */
    public void destroy() {
        nativeRelease(mNativePtr);
        mNativePtr = 0;
    }

    /**
     * Starts the discovery.
     */
    public void start() {
        if (mNativePtr == 0) {
            throw new RuntimeException("Destroyed");
        }
        nativeStart(mNativePtr);
        onStart();
    }

    /**
     * Stops the discovery.
     */
    public void stop() {
        if (mNativePtr == 0) {
            throw new RuntimeException("Destroyed");
        }
        onStop();
        nativeStop(mNativePtr);
    }

    /**
     * Called when the discovery is started.
     * <p>
     * Implementation must start searching for devices
     */
    protected abstract void onStart();

    /**
     * Called when the discovery is stopped.
     * <p>
     * Implementation must stop searching for devices.
     */
    protected abstract void onStop();

    /**
     * Notify that a device has been discovered.
     *
     * @param name    device name
     * @param type    device type
     * @param address device address
     * @param port    device port
     * @param id      device id
     */
    protected void addDevice(@NonNull String name, @ArsdkDevice.Type int type, @NonNull String address, int port,
                             @NonNull String id) {
        nativeAddDevice(mNativePtr, name, type, address, port, id);
    }

    /**
     * Notify that a previously discovered device has been removed.
     *
     * @param name device name
     * @param type device type
     */
    protected void removeDevice(@NonNull String name, @ArsdkDevice.Type int type) {
        nativeRemoveDevice(mNativePtr, name, type);
    }

    private native long nativeNew(long arsdkCoreNativePtr, @NonNull String name, long backendNativePtr);

    private native void nativeRelease(long nativePtr);

    private native void nativeStart(long nativePtr);

    private native void nativeStop(long nativePtr);

    private native void nativeAddDevice(long nativePtr, @NonNull String name, int type, @NonNull String addr, int port,
                                        @NonNull String id);

    private native void nativeRemoveDevice(long nativePtr, @NonNull String name, int type);

}
