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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.ProxyDeviceController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.PeripheralController;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.DroneFinder;
import com.parrot.drone.groundsdk.internal.device.DeviceModels;
import com.parrot.drone.groundsdk.internal.device.peripheral.DroneFinderCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureDroneManager;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.util.HashMap;
import java.util.Map;

/** DroneFinder peripheral controller for SkyController family remote controls. */
public class SkyControllerDroneFinder extends PeripheralController<ProxyDeviceController<?>> {

    /** The DroneFinder peripheral for which this object is the backend. */
    @NonNull
    private final DroneFinderCore mDroneFinder;

    /** Drones seen during discovery, by drone uid. */
    @NonNull
    private final Map<String, DroneFinderCore.DiscoveredDroneCore> mDiscoveredDrones;

    /**
     * Constructor.
     *
     * @param proxyController proxy device controller that owns this component controller.
     */
    public SkyControllerDroneFinder(@NonNull ProxyDeviceController proxyController) {
        super(proxyController);
        mDroneFinder = new DroneFinderCore(mComponentStore, mBackend);
        mDiscoveredDrones = new HashMap<>();
    }

    @Override
    protected void onConnected() {
        mDroneFinder.publish();
    }

    @Override
    protected void onDisconnected() {
        mDroneFinder.unpublish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureDroneManager.UID) {
            ArsdkFeatureDroneManager.decode(command, mDroneManagerCallbacks);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureDroneManager is decoded. */
    private final ArsdkFeatureDroneManager.Callback mDroneManagerCallbacks = new ArsdkFeatureDroneManager.Callback() {

        @Override
        public void onDroneListItem(String serial, @DeviceModel.Id int model, String name, int connectionOrder,
                                    int active, int visible, @Nullable ArsdkFeatureDroneManager.Security security,
                                    int hasSavedKey, int rssi, int listFlagsBitField) {
            boolean clearList = ArsdkFeatureGeneric.ListFlags.EMPTY.inBitField(listFlagsBitField);
            if (clearList) {
                mDiscoveredDrones.clear();
            } else if (ArsdkFeatureGeneric.ListFlags.REMOVE.inBitField(listFlagsBitField)) {
                mDiscoveredDrones.remove(serial);
            } else {
                if (ArsdkFeatureGeneric.ListFlags.FIRST.inBitField(listFlagsBitField)) {
                    mDiscoveredDrones.clear();
                }
                if (visible == 1) {
                    Drone.Model droneModel = DeviceModels.droneModel(model);
                    if (droneModel != null) {
                        DroneFinder.DiscoveredDrone.ConnectionSecurity connectionSecurity = null;
                        if (security == null) {
                            // unsupported value from the drone, we assume no security.
                            connectionSecurity = DroneFinder.DiscoveredDrone.ConnectionSecurity.NONE;
                        } else switch (security) {
                            case NONE:
                                connectionSecurity = DroneFinder.DiscoveredDrone.ConnectionSecurity.NONE;
                                break;
                            case WPA2:
                                connectionSecurity = hasSavedKey == 1
                                        ? DroneFinder.DiscoveredDrone.ConnectionSecurity.SAVED_PASSWORD
                                        : DroneFinder.DiscoveredDrone.ConnectionSecurity.PASSWORD;
                                break;
                        }
                        mDiscoveredDrones.put(serial, new DroneFinderCore.DiscoveredDroneCore(serial, droneModel,
                                name, connectionSecurity, rssi, connectionOrder != 0));
                    }
                }
            }

            if (clearList || ArsdkFeatureGeneric.ListFlags.LAST.inBitField(listFlagsBitField)) {
                mDroneFinder.updateDiscoveredDrones(mDiscoveredDrones.values().toArray(
                        new DroneFinderCore.DiscoveredDroneCore[0]))
                            .updateState(DroneFinder.State.IDLE).notifyUpdated();
            }
        }
    };

    /** Backend of DroneFinderCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final DroneFinderCore.Backend mBackend = new DroneFinderCore.Backend() {

        @Override
        public void discoverDrones() {
            sendCommand(ArsdkFeatureDroneManager.encodeDiscoverDrones());
            mDroneFinder.updateState(DroneFinder.State.SCANNING).notifyUpdated();
        }

        @Override
        public boolean connectDrone(@NonNull String uid, @Nullable String password) {
            DroneFinderCore.DiscoveredDroneCore drone = mDiscoveredDrones.get(uid);
            return drone != null && mDeviceController.connectDiscoveredDevice(
                    drone.getUid(), drone.getModel(), drone.getName(), password);
        }
    };
}
