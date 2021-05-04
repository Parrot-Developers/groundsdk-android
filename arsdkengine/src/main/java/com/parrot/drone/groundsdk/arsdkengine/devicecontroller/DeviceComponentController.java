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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;

/**
 * Device component controller.
 *
 * @param <CTRL> type of the device controller managing this component controller
 */
public abstract class DeviceComponentController<TYPE, CTRL extends DeviceController> {

    /** The device controller that owns this component controller. */
    @NonNull
    protected final CTRL mDeviceController;

    /** The store where the component will be published. */
    @NonNull
    protected final ComponentStore<TYPE> mComponentStore;

    /**
     * Constructor.
     *
     * @param deviceController the device controller that owns this component controller.
     * @param componentStore   the store where this component will be published
     */
    protected DeviceComponentController(@NonNull CTRL deviceController, @NonNull ComponentStore<TYPE> componentStore) {
        mDeviceController = deviceController;
        mComponentStore = componentStore;
    }

    /**
     * Tells whether the component controller is connected.
     *
     * @return {@code true} if the controller is connected, {@code false} otherwise
     */
    protected final boolean isConnected() {
        return mDeviceController.getConnectionState() == DeviceController.ControllerConnectionState.CONNECTED;
    }

    /**
     * Called when the managed device will be forgotten.
     * <p>
     * May be overridden by sub classes. Default implementation does nothing.
     */
    protected void onForgetting() {

    }

    /**
     * Called when values in the preset currently used by the managed device change.
     * <p>
     * May be overridden by sub classes. Default implementation does nothing.
     */
    protected void onPresetChange() {

    }

    /**
     * Called right before the connection to the managed device.
     * <p>
     * May be overridden by sub classes. Default implementation does nothing.
     */
    protected void onConnecting() {

    }

    /**
     * Called right after the connection to the managed device.
     * <p>
     * May be overridden by sub classes. Default implementation does nothing.
     */
    protected void onConnected() {

    }

    /**
     * Data synchronization allowance changed.
     * <p>
     * Note: this function is only called while the device is connected (i.e. after {@link #onConnected()}). If the data
     * sync was allowed, this callback will be called one last time right after the {@link #onDisconnected()}.
     * <p>
     * May be overridden by sub classes, Default implementation does nothing.
     *
     * @param allowed {@code true} means it is acceptable to perform data synchronization operation with the managed
     *                device, otherwise any ongoing data synchronization operation must stop
     */
    protected void onDataSyncAllowanceChanged(boolean allowed) {

    }

    /**
     * Called right before the disconnection to the managed device.
     * <p>
     * May be overridden by sub classes. Default implementation does nothing.
     */
    protected void onDisconnecting() {

    }

    /**
     * Called right after the disconnection to the managed device.
     * <p>
     * May be overridden by sub classes. Default implementation does nothing.
     */
    protected void onDisconnected() {

    }

    /**
     * Called when the connection to the managed device has been lost.
     * <p>
     * May be overridden by sub classes. Default implementation does nothing.
     */
    protected void onLinkLost() {

    }

    /**
     * Called when a command has been received from the managed device.
     * <p>
     * May be overridden by sub classes. Default implementation does nothing.
     *
     * @param command the command received
     */
    protected void onCommandReceived(@NonNull ArsdkCommand command) {

    }

    /**
     * Called when API capabilities of the managed device are known.
     * <p>
     * May be overridden by sub classes. Default implementation does nothing.
     *
     * @param api the API capabilities received
     */
    protected void onApiCapabilities(@ArsdkDevice.Api int api) {

    }

    /**
     * Called when the owning controller is stopped.
     * <p>
     * Component controller should here release all acquired resources, such as utility monitors.
     */
    protected void onDispose() {

    }

    /**
     * Send a command to the managed device.
     *
     * @param command the command to send
     *
     * @return {@code true} if the command was sent, otherwise {@code false}
     */
    protected final boolean sendCommand(@NonNull ArsdkCommand command) {
        return mDeviceController.sendCommand(command);
    }

    /**
     * Tells if component settings can be changed while not connected and must restored when connected.
     *
     * @return true if offline settings configuration is on
     */
    protected static boolean offlineSettingsEnabled() {
        return GroundSdkConfig.get().getOfflineSettingsMode() != GroundSdkConfig.OfflineSettingsMode.OFF;
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
