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
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkProxy;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.internal.device.DeviceModels;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureDroneManager;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/**
 * Implementation of DroneManager feature commands and callbacks.
 */
class DroneManagerFeature {

    /** Special password string used to tell drone manager to use its saved password for connection. */
    private static final String USE_SAVED_PASSWORD = "";

    /** Arsdk proxy instance. */
    @NonNull
    private final ArsdkProxy mArsdkProxy;

    /**
     * Constructor.
     *
     * @param arsdkProxy arsdk proxy instance
     */
    DroneManagerFeature(@NonNull ArsdkProxy arsdkProxy) {
        mArsdkProxy = arsdkProxy;
    }

    /**
     * Connects a drone.
     *
     * @param droneUid uid of the drone to connect.
     * @param password password to use for authentication. Use {@code null} if the device connection is not secured, or
     *                 to use drone manager saved password for the drone, if any
     *
     * @return {@code true} if the connection has started, otherwise {@code false}
     */
    boolean connectDrone(@NonNull String droneUid, @Nullable String password) {
        return mArsdkProxy.sendCommand(ArsdkFeatureDroneManager.encodeConnect(droneUid,
                password == null ? USE_SAVED_PASSWORD : password));
    }

    /**
     * Forgets a drone.
     *
     * @param droneUid uid of the drone to forget.
     */
    void forgetDrone(@NonNull String droneUid) {
        mArsdkProxy.sendCommand(ArsdkFeatureDroneManager.encodeForget(droneUid));
    }

    /**
     * Called when the device managed by the controller starts disconnecting.
     */
    void onProtocolDisconnecting() {
        mArsdkProxy.onProxyDeviceDisconnecting();
    }

    /**
     * Called when the controller receives a command.
     *
     * @param command received command
     */
    void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureDroneManager.UID) {
            ArsdkFeatureDroneManager.decode(command, mDroneManagerCallbacks);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureDroneManager is decoded. */
    private final ArsdkFeatureDroneManager.Callback mDroneManagerCallbacks = new ArsdkFeatureDroneManager.Callback() {

        @Override
        public void onConnectionState(@Nullable ArsdkFeatureDroneManager.ConnectionState state, String serial,
                                      @DeviceModel.Id int modelId, String name) {
            if (state != null) switch (state) {
                case IDLE:
                case SEARCHING:
                    mArsdkProxy.onActiveDeviceDisconnecting();
                    break;
                case CONNECTING: {
                    DeviceModel model = DeviceModels.model(modelId);
                    if (model != null) {
                        mArsdkProxy.onRemoteDeviceConnecting(serial, model, name);
                    }
                    break;
                }
                case CONNECTED: {
                    DeviceModel model = DeviceModels.model(modelId);
                    if (model != null) {
                        mArsdkProxy.onRemoteDeviceConnected(serial, model, name);
                    }
                    break;
                }
                case DISCONNECTING:
                    mArsdkProxy.onRemoteDeviceDisconnecting(serial);
                    break;
            }
        }

        @Override
        public void onAuthenticationFailed(String serial, @DeviceModel.Id int modelId, String name) {
            mArsdkProxy.onRemoteDeviceAuthenticationFailed(serial);
        }

        @Override
        public void onConnectionRefused(String serial, @DeviceModel.Id int modelId, String name) {
            // nothing special to do here, RC will continue trying to connect the same device
        }

        @Override
        public void onKnownDroneItem(String serial, @DeviceModel.Id int modelId, String name,
                                     @Nullable ArsdkFeatureDroneManager.Security security, int hasSavedKey,
                                     int listFlagsBitField) {
            if (ArsdkFeatureGeneric.ListFlags.EMPTY.inBitField(listFlagsBitField)) {
                mArsdkProxy.clearRemoteDevices();
            } else if (ArsdkFeatureGeneric.ListFlags.REMOVE.inBitField(listFlagsBitField)) {
                mArsdkProxy.removeRemoteDevice(serial);
            } else {
                if (ArsdkFeatureGeneric.ListFlags.FIRST.inBitField(listFlagsBitField)) {
                    mArsdkProxy.clearRemoteDevices();
                }
                DeviceModel model = DeviceModels.model(modelId);
                if (model != null) {
                    mArsdkProxy.addRemoteDevice(serial, model, name);
                }
            }
        }
    };
}
