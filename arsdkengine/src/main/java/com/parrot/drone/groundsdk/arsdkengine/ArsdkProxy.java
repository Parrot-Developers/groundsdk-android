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

package com.parrot.drone.groundsdk.arsdkengine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.ProxyDeviceController;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.internal.device.DeviceConnectorCore;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Delegate of arsdk engine that specifically manages devices connected through proxy devices (RC), versus devices
 * connected locally through arsdk backends (WIFI, BLE, MUX).
 */
public class ArsdkProxy {

    /** Arsdk engine instance. */
    @NonNull
    private final ArsdkEngine mEngine;

    /** Device controller acting as a proxy. */
    @NonNull
    private final ProxyDeviceController mProxyDevice;

    /** Device provider that this proxy implements. */
    @NonNull
    private final ProxyDeviceProvider mDeviceProvider;

    /** Devices known by the proxy device, by device uid. */
    @NonNull
    private final Map<String, DeviceController> mKnownDevices;

    /** Currently active device on the proxy. */
    @Nullable
    private DeviceController mActiveDevice;

    /** Devices artificially maintained as 'known' because of failed authentication failure. */
    private final Set<DeviceController> mAuthFailedDevices;

    /**
     * Constructor.
     *
     * @param engine      arsdk engine instance
     * @param proxyDevice device controller acting as a proxy
     */
    public ArsdkProxy(@NonNull ArsdkEngine engine, @NonNull ProxyDeviceController proxyDevice) {
        mEngine = engine;
        mProxyDevice = proxyDevice;
        mKnownDevices = new HashMap<>();
        mAuthFailedDevices = new HashSet<>();
        mDeviceProvider = new ProxyDeviceProvider(proxyDevice.getUid());
    }

    /**
     * Retrieves the currently active device controller on this proxy.
     *
     * @return currently active device controller, otherwise {@code null}
     */
    @Nullable
    public final DeviceController getActiveDevice() {
        return mActiveDevice;
    }

    /**
     * Connects an existing or new device.
     * <p>
     * First tries to fetch an existing device controller featuring the specified uid, otherwise tries to create a new
     * device controller with the provided info. <br/>
     * If successful (an existing controller was found or a new one could be instantiated), this proxy's device provider
     * is registered in the obtained controller, then the controller is asked to connect the device (as if the user
     * itself asked to do so).
     * <p>
     * This method is used to connect scanned drones (i.e. drones visible by, but usually not known by the proxy
     * device).
     *
     * @param deviceUid uid of the proxied device
     * @param model     model of the proxied device
     * @param name      name of the proxied device
     * @param password  password to use for authentication. Use {@code null} if the device connection is not secured, or
     *                  to use the proxy's saved password
     *
     * @return {@code true} if the connection could be initiated, otherwise {@code false}
     */
    public final boolean connectRemoteDevice(@NonNull String deviceUid, @NonNull DeviceModel model,
                                             @NonNull String name, @Nullable String password) {
        DeviceController controller = mEngine.getOrCreateDeviceController(deviceUid, model, name);
        controller.addDeviceProvider(mDeviceProvider);
        return controller.connectDevice(mDeviceProvider, password, DeviceState.ConnectionStateCause.USER_REQUESTED);
    }

    /**
     * Registers a new device provided by this proxy.
     * <p>
     * First tries to fetch an existing device controller featuring the specified uid, otherwise tries to create a new
     * device controller with the provided info. <br/>
     * If successful (an existing controller was found or a new one could be instantiated), this proxy's device provider
     * is registered in the obtained controller.
     *
     * @param deviceUid uid of the proxied device
     * @param model     model of the proxied device
     * @param name      name of the proxied device
     */
    public final void addRemoteDevice(@NonNull String deviceUid, @NonNull DeviceModel model, @NonNull String name) {
        DeviceController controller = mEngine.getOrCreateDeviceController(deviceUid, model, name);
        mKnownDevices.put(deviceUid, controller);
        controller.addDeviceProvider(mDeviceProvider);
    }

    /**
     * Unregisters a device from this proxy.
     * <p>
     * Removes the proxy device provider from the controller if a known controller with such an uid exists.
     * <p>
     * Note that this method does not perform any bookkeeping of the active device, i.e. it is expected that the proxy
     * device also calls the appropriate disconnection callbacks before or after removing the active proxied device.
     *
     * @param deviceUid uid of the proxied device to unregister
     */
    public final void removeRemoteDevice(@NonNull String deviceUid) {
        DeviceController controller = mKnownDevices.remove(deviceUid);
        if (controller != null) {
            controller.removeDeviceProvider(mDeviceProvider);
        }
    }

    /**
     * Unregisters all devices provided by this proxy.
     * <p>
     * Removes the proxy device provider from all known device controllers.
     * <p>
     * Note that this method does not perform any bookkeeping of the active device, i.e. it is expected that the proxy
     * device also calls the appropriate disconnection callbacks before or after removing the active proxied device.
     */
    public final void clearRemoteDevices() {
        Iterator<DeviceController> iterator = mKnownDevices.values().iterator();
        while (iterator.hasNext()) {
            DeviceController controller = iterator.next();
            controller.removeDeviceProvider(mDeviceProvider);
            iterator.remove();
        }
    }

    /**
     * Forwards a command to be sent by the controller to the proxy device.
     *
     * @param command command to forward
     *
     * @return {@code true} if the command could be sent, otherwise {@code false}
     */
    public final boolean sendCommand(@NonNull ArsdkCommand command) {
        return mProxyDevice.sendCommand(command);
    }

    /**
     * Called when the proxy device controller start disconnecting.
     * <p>
     * Resets the active device and clears all devices known by the proxy, removing the proxy device provider from their
     * controllers.
     */
    public final void onProxyDeviceDisconnecting() {
        changeActiveDevice(null);
        clearRemoteDevices();
        for (DeviceController deviceController : mAuthFailedDevices) {
            deviceController.removeDeviceProvider(mDeviceProvider);
        }
        mAuthFailedDevices.clear();
    }

    /**
     * Called when the connection process with a proxied device starts.
     * <p>
     * Disconnects the active device first, if any, then registers the proxy device provider with the device controller
     * that handles the given device uid and notifies the controller that the link-level connection has started. The
     * device controller is now considered the active device.
     * <p>
     * In case no device controller with the specified uid exists, tries to instantiate a new one using the provided
     * device uid, type and name. If the new controller was created successfully, it is registered in the known
     * controllers list and started.
     *
     * @param deviceUid uid of the connecting device
     * @param model     model of the connecting device
     * @param name      name of the connecting device
     */
    public final void onRemoteDeviceConnecting(@NonNull String deviceUid, @NonNull DeviceModel model,
                                               @NonNull String name) {
        DeviceController device = mEngine.getOrCreateDeviceController(deviceUid, model, name);
        changeActiveDevice(device);
        device.onLinkConnecting(mDeviceProvider);
    }

    /**
     * Called when the a proxied device is connected.
     * <p>
     * Disconnects the active device first, if any, then registers the proxy device provider with the device controller
     * that handles the given device uid and notifies the controller that link-level connection is complete, by
     * providing a protocol backend to be used to send commands to the proxied device. The device controller is now
     * considered the active device.
     * <p>
     * In case no device controller with the specified uid exists, tries to instantiate a new one using the provided
     * device uid, type and name. If the new controller was created successfully, it is registered in the known
     * controllers list and started.
     *
     * @param deviceUid uid of the connecting device
     * @param model     model of the connecting device
     * @param name      name of the connecting device
     */
    public final void onRemoteDeviceConnected(@NonNull String deviceUid, @NonNull DeviceModel model,
                                              @NonNull String name) {
        DeviceController device = mEngine.getOrCreateDeviceController(deviceUid, model, name);
        changeActiveDevice(device);
        DeviceController.Backend proxyBackend = mProxyDevice.getProtocolBackend();
        assert proxyBackend != null;
        device.onLinkConnected(mDeviceProvider, proxyBackend.asProxyFor(device));
    }

    /**
     * Called when the disconnection of a proxied device starts.
     * <p>
     * If the provided device uid matches, notifies the active device controller that link-level disconnection starts.
     *
     * @param deviceUid uid of the disconnecting device
     */
    public final void onRemoteDeviceDisconnecting(@NonNull String deviceUid) {
        if (mActiveDevice != null && mActiveDevice.getUid().equals(deviceUid)) {
            onActiveDeviceDisconnecting();
        }
    }

    /**
     * Called when the connection of a proxied device fails because authentication failed.
     * <p>
     * If the provided device uid matches, notifies the active device controller that link connection was canceled
     * because of authentication failure.
     * <p>
     * Remembers that the reason why the active device will disconnect is an authentication failure, in order to keep
     * the proxy as a provider of that device for as long as no other device is connected through the proxy. This allows
     * to keep the device with the BAD_PASSWORD state in the user's device list so that it can try to connect the device
     * again with a proper password.
     *
     * @param deviceUid uid of the device for which authentication failed
     */
    public final void onRemoteDeviceAuthenticationFailed(@NonNull String deviceUid) {
        if (mActiveDevice != null && mActiveDevice.getUid().equals(deviceUid)) {
            mActiveDevice.onLinkConnectionCanceled(DeviceState.ConnectionStateCause.BAD_PASSWORD, false);
            mAuthFailedDevices.add(mActiveDevice);
        }
    }

    /**
     * Called when the active device disconnects.
     * <p>
     * Resets the active device.
     */
    public final void onActiveDeviceDisconnecting() {
        changeActiveDevice(null);
    }

    /**
     * Forwards a command received from the device to the active device controller, if any, for processing.
     *
     * @param command received command to forward
     */
    public final void onCommandReceived(@NonNull ArsdkCommand command) {
        if (mActiveDevice != null) {
            mActiveDevice.onCommandReceived(command);
        }
    }

    /**
     * Changes the currently active device.
     * <p>
     * If the provided device differs from the current active device, then the active device controller, if any, gets
     * notified of link-level disconnection, and, in case it is not in this proxy's known device list, the proxy
     * provider gets removed from the list of its device providers.
     * <p>
     * Then, the provided device controller, if any, is considered the active device and the proxy provider is added to
     * the list of its device providers, as the active device provider.
     *
     * @param newActiveDevice device controller to set as the active device, or {@code null} to reset the active device
     *                        if any
     */
    private void changeActiveDevice(@Nullable DeviceController newActiveDevice) {
        // disconnect current active device if different
        if (newActiveDevice != mActiveDevice) {
            if (mActiveDevice != null) {
                mActiveDevice.onLinkDisconnected(false);
                if (!mKnownDevices.containsKey(mActiveDevice.getUid()) && !mAuthFailedDevices.contains(mActiveDevice)) {
                    // just a scanned device that is not in failed authentication state, remove provider
                    mActiveDevice.removeDeviceProvider(mDeviceProvider);
                }
                mActiveDevice = null;
            }
            if (newActiveDevice != null) {
                mActiveDevice = newActiveDevice;
                mActiveDevice.addDeviceProvider(mDeviceProvider);
            }
            mProxyDevice.onActiveDeviceChanged();
        }
    }

    /** Proxy device provider, that this proxy implements. */
    private final class ProxyDeviceProvider extends DeviceProvider {

        /**
         * Constructor.
         *
         * @param uid uid of the device controller acting as the proxy
         */
        private ProxyDeviceProvider(@NonNull String uid) {
            super(DeviceConnectorCore.createRCConnector(uid));
        }

        @Override
        @Nullable
        public DeviceProvider getParent() {
            return mProxyDevice.getActiveProvider();
        }

        @Override
        public boolean connectDevice(@NonNull DeviceController deviceController, @Nullable String password) {
            return mProxyDevice.connectRemoteDevice(deviceController.getUid(), password);
        }

        @Override
        public void forgetDevice(@NonNull DeviceController deviceController) {
            String uid = deviceController.getUid();
            mProxyDevice.forgetRemoteDevice(uid);
            if (mAuthFailedDevices.remove(deviceController)) {
                deviceController.removeDeviceProvider(mDeviceProvider);
            }
        }

        @Override
        public void onDeviceDataSyncConditionChanged(@NonNull DeviceController deviceController) {
            if (deviceController == mActiveDevice) {
                mProxyDevice.onActiveDeviceDataSyncConditionChanged();
            }
        }

        @Override
        public String toString() {
            return "ProxyDeviceProvider [uid: " + getConnector().getUid() + "]";
        }
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param prefix prefix string (usually indent) to prepend to each written dump line
     */
    public void dump(@NonNull PrintWriter writer, @NonNull String prefix) {
        writer.write(prefix + "Active device: " + mActiveDevice + "\n");
    }
}
