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

import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.blackbox.BlackBoxRecorder;
import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.AnafiFamilyDroneController;
import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.SkyControllerFamilyController;
import com.parrot.drone.groundsdk.arsdkengine.ephemeris.EphemerisStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.AppDefaults;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.device.DeviceModels;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.groundsdk.internal.utility.BlackBoxStorage;
import com.parrot.drone.groundsdk.internal.utility.RcUsbAccessoryManager;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.arsdk.backend.ArsdkBackendController;
import com.parrot.drone.sdkcore.arsdk.backend.ble.ArsdkBleBackendController;
import com.parrot.drone.sdkcore.arsdk.backend.mux.ArsdkDebugMuxBackendController;
import com.parrot.drone.sdkcore.arsdk.backend.mux.ArsdkUsbMuxBackendController;
import com.parrot.drone.sdkcore.arsdk.backend.net.ArsdkWifiBackendController;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Ground sdk engine implementation for arsdk.
 */
public class ArsdkEngine extends EngineBase implements RcUsbAccessoryManager {

    /** Store of known devices. */
    @NonNull
    private final PersistentStore mPersistentStore;

    /** GPS Ephemeris store. */
    @Nullable
    private final EphemerisStore mEphemerisStore;

    /** Map of device controller, by uid. There is one device controller for each device in created by the engine */
    @NonNull
    private final Map<String, DeviceController> mDeviceControllers;

    /** Shared arsdk facade. */
    @NonNull
    private final Arsdk mArsdk;

    /** Configured Arsdk backend controllers. */
    @NonNull
    private final ArsdkBackendController[] mBackendControllers;

    /** USB Mux backend controller, {@code null} if configured out. */
    @Nullable
    private final ArsdkUsbMuxBackendController mUsbBackendController;

    /** Black box recorder. */
    @Nullable
    private BlackBoxRecorder mBlackBoxRecorder;

    /**
     * Constructor.
     *
     * @param controller opaque engine's controller, forwarded to super class
     */
    public ArsdkEngine(@NonNull Controller controller) {
        super(controller);
        // TODO: add arsdkengine configuration API. For now enable log on all builds
        ArsdkCore.setCommandLogLevel(ArsdkCommand.LOG_LEVEL_ACKNOWLEDGED_WITHOUT_FREQUENT);
        publishUtility(RcUsbAccessoryManager.class, this);
        mPersistentStore = AppDefaults.importTo(new PersistentStore(getContext()));

        mEphemerisStore = EphemerisStore.get(getContext());
        mDeviceControllers = new HashMap<>();

        GroundSdkConfig config = GroundSdkConfig.get(getContext());
        Set<DeviceModel> supportedDevices = config.getSupportedDevices();
        // Create all configured backend controllers
        Set<ArsdkBackendController> backendControllers = new HashSet<>();
        // add WIFI backend controller
        if (config.isWifiEnabled()) {
            ArsdkBackendController wifiController = ArsdkWifiBackendController.create(getContext(),
                    DeviceModels.identifiers(DeviceModels.supportingTechnology(supportedDevices,
                            DeviceConnector.Technology.WIFI)));
            backendControllers.add(wifiController);
        }
        // try to add BLE backend controller if available on device
        if (config.isBleEnabled()) {
            ArsdkBackendController bleController = ArsdkBleBackendController.create(getContext(),
                    DeviceModels.identifiers(DeviceModels.supportingTechnology(supportedDevices,
                            DeviceConnector.Technology.BLE)));
            if (bleController != null) {
                backendControllers.add(bleController);
            }
        }
        // try to add MUX USB accessory backend controller if supported on device
        ArsdkUsbMuxBackendController usbMuxController = null;
        if (config.isUsbEnabled()) {
            usbMuxController = ArsdkUsbMuxBackendController.create(getContext(), DeviceModels.identifiers(
                    DeviceModels.supportingTechnology(supportedDevices, DeviceConnector.Technology.USB)));
            if (usbMuxController != null) {
                backendControllers.add(usbMuxController);
            }
        }
        mUsbBackendController = usbMuxController;
        // try to add USB Debug Bridge MUX controller if config is set properly
        if (config.isUsbDebugEnabled()) {
            ArsdkDebugMuxBackendController debugMuxController = ArsdkDebugMuxBackendController.create(getContext(),
                    DeviceModels.identifiers(DeviceModels.supportingTechnology(supportedDevices,
                            DeviceConnector.Technology.USB)));
            if (debugMuxController != null) {
                backendControllers.add(debugMuxController);
            }
        }
        mBackendControllers = backendControllers.toArray(new ArsdkBackendController[0]);

        // finally create arsdk
        mArsdk = new Arsdk(this);
    }

    @Override
    public void onStart() {
        getUtilityOrThrow(SystemConnectivity.class).monitorWith(mInternetMonitor);

        BlackBoxStorage blackBoxStorage = getUtility(BlackBoxStorage.class);
        if (blackBoxStorage != null) {
            mBlackBoxRecorder = new BlackBoxRecorder(this, blackBoxStorage);
        }

        // create all known devices
        for (String uid : mPersistentStore.getDevicesUid()) {
            PersistentStore.Dictionary deviceDict = mPersistentStore.getDevice(uid);
            Integer modelId = deviceDict.getInt(PersistentStore.KEY_DEVICE_MODEL);
            DeviceModel model = modelId == null ? null : DeviceModels.model(modelId);
            String name = deviceDict.getString(PersistentStore.KEY_DEVICE_NAME);
            if (model != null && name != null) {
                DeviceController deviceController = createDeviceController(uid, model, name);
                mDeviceControllers.put(uid, deviceController);
                deviceController.start(mDeviceControllerStateListener);
            }
        }

        mArsdk.start();
    }

    /** Observes internet connectivity availability to launch external data sync. */
    private final SystemConnectivity.Monitor mInternetMonitor = new SystemConnectivity.Monitor() {

        @Override
        public void onInternetAvailabilityChanged(boolean availableNow) {
            if (availableNow) {
                Executor.runInBackground(mSync);
            }
        }

        /** External data sync background task. */
        private final Callable<Void> mSync = new Callable<Void>() {

            @Override
            public Void call() {
                if (mEphemerisStore != null) {
                    mEphemerisStore.downloadEphemerides();
                }
                return null;
            }

            @Override
            public String toString() {
                return "Data synchronization";
            }
        };
    };

    @Override
    public void onStopRequested() {
        // try to stop now, may not be effective until all controllers are either STOPPED or IDLE
        tryStop();
    }

    @Override
    public void manageRcAccessory(@NonNull UsbAccessory rcAccessory) {
        if (mUsbBackendController != null) {
            mUsbBackendController.setRcAccessory(rcAccessory);
        }
    }

    /**
     * Gets the engine persistent storage.
     *
     * @return engine persistent store
     */
    @NonNull
    public final PersistentStore getPersistentStore() {
        return mPersistentStore;
    }

    /**
     * Retrieves the ephemeris store.
     *
     * @return engine ephemeris store
     */
    @Nullable
    public final EphemerisStore getEphemerisStore() {
        return mEphemerisStore;
    }

    /**
     * Retrieves the black box recorder.
     *
     * @return black box recorder
     */
    @Nullable
    public final BlackBoxRecorder getBlackBoxRecorder() {
        return mBlackBoxRecorder;
    }

    /**
     * Factory function to create arsdk controller.
     * <p>
     * This is to allow mocking arsdk controller for unit tests.
     *
     * @param listener arsdkcore device listener
     *
     * @return a new arsdk controller instance
     */
    @NonNull
    ArsdkCore createArsdkCore(@NonNull ArsdkCore.Listener listener) {
        Context context = getContext();
        return new ArsdkCore(mBackendControllers, listener, getControllerDescriptor(),
                getControllerVersion(context), GroundSdkConfig.get().isVideoDecodingEnabled());
    }

    /**
     * Gets a known device controller or tries to create and start one in case it does not exists.
     * <p>
     * Looks up known device controllers to find one with the specified device uid. In case no such controller exists,
     * tries to instantiate a new one using the provided device uid, type and name. If the new controller was created
     * successfully, it is registered in the known controllers list and started.
     *
     * @param uid   controlled device uid
     * @param model controlled device model
     * @param name  controlled device name
     *
     * @return the known device controller with the specified uid, or a new, registered and started device controller if
     *         it does not exist, or {@code null} if no controller with such an uid exists and the creation of a new
     *         controller using the provided parameters failed
     */
    @NonNull
    final DeviceController getOrCreateDeviceController(@NonNull String uid, @NonNull DeviceModel model,
                                                       @NonNull String name) {
        DeviceController controller = mDeviceControllers.get(uid);
        if (controller == null) {
            controller = createDeviceController(uid, model, name);
            mDeviceControllers.put(uid, controller);
            controller.start(mDeviceControllerStateListener);
        }
        return controller;
    }

    /**
     * Gets a known device controller.
     * <p>
     * Looks up known device controllers to find one with the specified device uid.
     *
     * @param deviceUid device controller uid
     *
     * @return the known device controller with the specified uid, otherwise {@code null}
     */
    @Nullable
    final DeviceController getExistingDeviceController(@NonNull String deviceUid) {
        return mDeviceControllers.get(deviceUid);
    }

    /**
     * Creates a device controller for a device.
     *
     * @param uid   device uid
     * @param model device model
     * @param name  device name
     *
     * @return a new Device controller instance, or null if the type is invalid
     */
    @NonNull
    private DeviceController createDeviceController(@NonNull String uid, DeviceModel model, @NonNull String name) {
        if (model instanceof Drone.Model) {
            Drone.Model droneModel = (Drone.Model) model;
            switch (droneModel) {
                case ANAFI_4K:
                case ANAFI_THERMAL:
                case ANAFI_UA:
                case ANAFI_USA:
                    return new AnafiFamilyDroneController(this, uid, droneModel, name);
            }
        } else if (model instanceof RemoteControl.Model) {
            RemoteControl.Model rcModel = (RemoteControl.Model) model;
            switch (rcModel) {
                case SKY_CONTROLLER_3:
                case SKY_CONTROLLER_UA:
                    return new SkyControllerFamilyController(this, uid, rcModel, name);
            }
        }
        throw new IllegalArgumentException("Unsupported device model: " + model);
    }

    /** Called back when a device controller closes itself. */
    private final DeviceController.OnStateChangedListener mDeviceControllerStateListener =
            new DeviceController.OnStateChangedListener() {

                @Override
                public void onStateChanged(@NonNull String uid, @NonNull DeviceController.State state) {
                    if (state != DeviceController.State.ACTIVE) {
                        if (state == DeviceController.State.STOPPED) {
                            mDeviceControllers.remove(uid);
                        }
                        // if we have been requested to stop, try now
                        if (isRequestedToStop()) {
                            tryStop();
                        }
                    }
                }
            };

    /**
     * Checks that all device controllers are idle, and stops the engine the case being.
     */
    private void tryStop() {
        for (DeviceController controller : mDeviceControllers.values()) {
            if (controller.getState() == DeviceController.State.ACTIVE) {
                return;
            }
        }

        acknowledgeStopRequest();

        getUtilityOrThrow(SystemConnectivity.class).disposeMonitor(mInternetMonitor);

        // all remaining controllers are idle: stop arsdk
        mArsdk.stop();

        // stop (and remove) all idle controllers, to remove published devices from groundSdk stores
        Iterator<DeviceController> iterator = mDeviceControllers.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().forceStop();
            iterator.remove();
        }
    }

    /**
     * Helper function that return the controller descriptor, formatted to be sent during connection.
     *
     * @return controller descriptor
     */
    @NonNull
    private static String getControllerDescriptor() {
        return "APP,Android," + Build.MODEL + "," + Build.VERSION.RELEASE;
    }

    /**
     * Helper function that return the controller version, formatted to be sent during connection.
     *
     * @param context application context
     *
     * @return controller version
     */
    @NonNull
    private static String getControllerVersion(@NonNull Context context) {
        GroundSdkConfig config = GroundSdkConfig.get(context);
        return config.getApplicationPackage() + "," + config.getApplicationVersion()
               + "," + GroundSdkConfig.getSdkPackage() + "," + GroundSdkConfig.getSdkVersion();
    }

    @Override
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
        if (args.isEmpty() || args.contains("--help")) {
            writer.write("\t--controllers: dumps devices controllers\n");
        } else {
            if (args.contains("--controllers") || args.contains("--all")) {
                writer.write("Device controllers: " + mDeviceControllers.size() + "\n");
                for (DeviceController<?> controller : mDeviceControllers.values()) {
                    controller.dump(writer, args, "\t");
                }
            }
        }
        mPersistentStore.dump(writer, args);
        mArsdk.dump(writer, args);
    }
}
