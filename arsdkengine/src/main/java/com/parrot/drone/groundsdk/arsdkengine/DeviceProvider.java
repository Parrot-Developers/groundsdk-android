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
import com.parrot.drone.groundsdk.internal.device.DeviceConnectorCore;

/**
 * Provides connection for a device.
 */
public abstract class DeviceProvider {

    /** GroundSdk API connector that this provider represents. */
    @NonNull
    private final DeviceConnectorCore mConnector;

    /**
     * Constructor.
     *
     * @param connector device connector that this provider represents
     */
    DeviceProvider(@NonNull DeviceConnectorCore connector) {
        mConnector = connector;
    }

    /**
     * Gets the associated device connector.
     *
     * @return device connector
     */
    @NonNull
    public final DeviceConnectorCore getConnector() {
        return mConnector;
    }

    /**
     * Gets the parent connection provider of this connection provider.
     *
     * @return the parent connection provider, or {@code null} if there is no parent connection provider
     */
    @Nullable
    public DeviceProvider getParent() {
        return null;
    }

    /**
     * Connects the device managed by the given controller.
     *
     * @param deviceController device controller whose device must be connected
     * @param password         password to use for authentication. Use {@code null} if the device connection is not
     *                         secured, or to use the provider's saved password, if any (for RC providers)
     *
     * @return {@code true} if the connect operation was successfully initiated, otherwise {@code false}
     */
    public abstract boolean connectDevice(@NonNull DeviceController deviceController, @Nullable String password);

    /**
     * Disconnects the device managed by the given controller.
     * <p>
     * As a provider may not support the disconnect operation, this method provides a default implementation that return
     * {@code false}. Subclasses that need to support the disconnect operation may override this method to do so.
     *
     * @param deviceController device controller whose device must be disconnected
     *
     * @return {@code true} if the disconnect operation was successfully initiated, otherwise {@code false}
     */
    public boolean disconnectDevice(@NonNull DeviceController deviceController) {
        return false;
    }

    /**
     * Forgets the device managed by the given controller.
     * <p>
     * As a provider may not support the forget operation, this method provides a default implementation that does
     * nothing. Subclasses that need to support the forget operation may override this method to do so.
     *
     * @param deviceController device controller whose device must be forgotten
     */
    public void forgetDevice(@NonNull DeviceController deviceController) {

    }

    /**
     * Notifies that some conditions that control device data synchronization allowance have changed.
     * <p>
     * This method allows proxy device providers to know when data synchronization allowance conditions concerning the
     * device they proxy change, and take appropriate measures.
     * <p>
     * Default implementation does nothing.
     *
     * @param deviceController device controller whose synchronization allowance conditions changed
     */
    public void onDeviceDataSyncConditionChanged(@NonNull DeviceController deviceController) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeviceProvider)) {
            return false;
        }

        DeviceProvider that = (DeviceProvider) o;

        return mConnector.equals(that.mConnector);

    }

    @Override
    public int hashCode() {
        return mConnector.hashCode();
    }
}
