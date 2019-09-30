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

package com.parrot.drone.groundsdk.arsdkengine.devicecontroller;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngine;
import com.parrot.drone.groundsdk.arsdkengine.ArsdkProxy;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.internal.device.DeviceCore;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.io.PrintWriter;
import java.util.Set;

/**
 * Base class for a device controller (e.g. a RemoteControl) that can act as a proxy to other devices, providing access
 * to those devices.
 *
 * @param <D> type of the controlled device
 */
public abstract class ProxyDeviceController<D extends DeviceCore> extends DeviceController<D> {

    /** Arsdk proxy instance. */
    @NonNull
    final ArsdkProxy mArsdkProxy;

    /**
     * Constructor.
     *
     * @param engine        arsdk engine instance
     * @param deviceFactory factory used to create the controlled device
     */
    ProxyDeviceController(@NonNull ArsdkEngine engine, @NonNull DeviceFactory<D> deviceFactory) {
        super(engine, deviceFactory, 0 /* for now, disable no-ack loop  for all proxy devices. */);
        mArsdkProxy = new ArsdkProxy(engine, this);
    }

    @CallSuper
    @Override
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        mArsdkProxy.onCommandReceived(command);
        super.onCommandReceived(command);
    }

    /**
     * Notified by {@code ArsdkProxy} when the active device controller changes.
     */
    public final void onActiveDeviceChanged() {
        notifyDataSyncConditionsChanged();
    }

    /**
     * Notified by {@code ArsdkProxy} when data sync conditions on the active device controller change.
     */
    public final void onActiveDeviceDataSyncConditionChanged() {
        notifyDataSyncConditionsChanged();
    }

    @Override
    public boolean isDataSyncAllowed() {
        DeviceController activeController = mArsdkProxy.getActiveDevice();
        return super.isDataSyncAllowed() && (activeController == null || activeController.isDataSyncAllowed());
    }

    /**
     * Connects a proxied device.
     *
     * @param deviceUid uid of the device to connect
     * @param password  password to use for authentication. Use {@code null} if the device connection is not secured, or
     *                  to use the provider's saved password, if any
     *
     * @return {@code true} if the connection to the proxied device could be initiated, otherwise {@code false}
     */
    public abstract boolean connectRemoteDevice(@NonNull String deviceUid, @Nullable String password);

    /**
     * Connects to a not-yet proxied device, visible through discovery
     * <p>
     * This method ensures that a device controller is created for the discovered device, before requesting the
     * connection to be made, so that the whole connection process can appear as-if the user requested it itself.
     *
     * @param deviceUid uid of the discovered device
     * @param model     model of the discovered device
     * @param name      name of the discovered device
     * @param password  password to use for authentication. Use {@code null} if the device connection is not secured, or
     *                  to use the proxy's saved password
     *
     * @return {@code true} if the connection could be initiated, otherwise {@code false}
     */
    public abstract boolean connectDiscoveredDevice(@NonNull String deviceUid, @NonNull DeviceModel model,
                                                    @NonNull String name, @Nullable String password);

    /**
     * Forgets a proxied device.
     *
     * @param deviceUid uid of the device to forget
     */
    public abstract void forgetRemoteDevice(@NonNull String deviceUid);

    /**
     * Gets arsdk proxy delegate.
     * <p/>
     * Used only by unit tests to mock creating remote devices from a proxy device controller.
     *
     * @return this device controller arsdk proxy instance
     */
    @VisibleForTesting
    @NonNull
    public ArsdkProxy getArsdkProxy() {
        return mArsdkProxy;
    }

    @Override
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args, @NonNull String prefix) {
        super.dump(writer, args, prefix);
        mArsdkProxy.dump(writer, prefix + "\t");
    }
}
