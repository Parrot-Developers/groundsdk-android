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

package com.parrot.drone.groundsdk.internal.engine.activation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.internal.Cancelable;
import com.parrot.drone.groundsdk.internal.device.DeviceCore;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;
import com.parrot.drone.groundsdk.internal.http.HttpActivationClient;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.utility.DeviceStore;
import com.parrot.drone.groundsdk.internal.utility.DroneStore;
import com.parrot.drone.groundsdk.internal.utility.RemoteControlStore;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_ENGINE;

/**
 * Registers known devices the activation server.
 * <p>
 * Drones and remote control are registered on the activation server.
 * <p>
 * Every time Internet becomes available or a new drone is added to drone store, this engine computes the list of
 * devices to register. If this list is not empty, it sends a register request to the activation server.
 */
public class ActivationEngine extends EngineBase {

    /** Drone store. Effectively non-{@code null} after {@link #onStart}. */
    private DroneStore mDroneStore;

    /** RemoteControl store. Effectively non-{@code null} after {@link #onStart}. */
    private RemoteControlStore mRemoteControlStore;

    /** System connectivity, monitor Internet availability. Effectively non-{@code null} after {@link #onStart}. */
    private SystemConnectivity mSystemConnectivity;

    /** HTTP Activation client, to register devices on remote server. */
    @Nullable
    private HttpActivationClient mHttpClient;

    /** Current cancellable request of devices register, {@code null} if no request ongoing. */
    @Nullable
    private Cancelable mCurrentRequest;

    /** Persistence layer. */
    @NonNull
    private final Persistence mPersistence;

    /** Set of devices already registered. */
    @NonNull
    private final Set<String> mRegisteredDevices;

    /**
     * Constructor.
     *
     * @param controller provides access to the engine's controller; opaque to subclasses, which should forward it
     *                   directly through {@code super(controller)}
     */
    public ActivationEngine(@NonNull Controller controller) {
        super(controller);
        mPersistence = new Persistence(getContext());
        mRegisteredDevices = mPersistence.loadRegisteredDevices();
    }

    @Override
    public void onStart() {
        mDroneStore = getUtilityOrThrow(DroneStore.class);
        mRemoteControlStore = getUtilityOrThrow(RemoteControlStore.class);
        mSystemConnectivity = getUtilityOrThrow(SystemConnectivity.class);
        mDroneStore.monitorWith(mDroneStoreMonitor);
        mSystemConnectivity.monitorWith(mInternetMonitor);
    }

    @Override
    protected void onStopRequested() {
        mDroneStore.disposeMonitor(mDroneStoreMonitor);
        mSystemConnectivity.disposeMonitor(mInternetMonitor);
        acknowledgeStopRequest();
    }

    @Override
    protected void onStopRequestCanceled() {
        mDroneStore.monitorWith(mDroneStoreMonitor);
        mSystemConnectivity.monitorWith(mInternetMonitor);
    }

    @Override
    protected void onStop() {
        cancelCurrentUpload();
        if (mHttpClient != null) {
            mHttpClient.dispose();
            mHttpClient = null;
        }
    }

    /**
     * Called back when request to register some devices completed.
     *
     * @param devices registered devices
     */
    private void onRegisterDeviceComplete(@NonNull Map<String, String> devices) {
        // update set of registered devices
        mRegisteredDevices.addAll(devices.keySet());
        // save new set of registered devices
        mPersistence.saveRegisteredDevices(mRegisteredDevices);
        // register other devices if needed
        registerDevices();
    }

    /**
     * Tells whether a device needs to be registered or not.
     *
     * @param device the device
     *
     * @return {@code true} if the device needs to be registered (i.e. has not been already registered by this app,
     *         on this phone)
     */
    @VisibleForTesting
    boolean deviceNeedRegister(@NonNull DeviceCore device) {
        String uid = device.getUid();
        return device.getDeviceStateCore().isPersisted()
               && !mRegisteredDevices.contains(uid)
               && !uid.equals(device.getModel().defaultDeviceUid())
               && !uid.equals(ArsdkDevice.SIMULATOR_UID)
               && hasRegistrableBoardId(device);
    }

    /**
     * Gets devices that have to be registered.
     *
     * @return devices that have to be registered, map key is device uid and value is firmware version.
     */
    @NonNull
    private Map<String, String> listDevicesToRegister() {
        Map<String, String> devices = new HashMap<>();
        // drones to register
        for (DeviceCore device : mDroneStore.all()) {
            if (deviceNeedRegister(device)) {
                devices.put(device.getUid(), device.getFirmwareVersion().toString());
            }
        }
        // remote controls to register
        for (DeviceCore device : mRemoteControlStore.all()) {
            if (deviceNeedRegister(device)) {
                devices.put(device.getUid(), device.getFirmwareVersion().toString());
            }
        }
        return devices;
    }

    /**
     * Registers devices that are not yet registered.
     * <p>
     * Registers devices only if Internet is available or if there is no ongoing register request.
     */
    private void registerDevices() {
        // check that Internet is available and that there is not ongoing request
        if (mSystemConnectivity.isInternetAvailable()
            && mHttpClient != null
            && mCurrentRequest == null) {
            // get devices to register
            Map<String, String> devices = listDevicesToRegister();
            if (!devices.isEmpty()) {
                // send register request
                mCurrentRequest = mHttpClient.register(devices, (status, code) -> {
                    mCurrentRequest = null;
                    if (code == HttpRequest.STATUS_CODE_UNKNOWN ||
                        code == HttpRequest.STATUS_CODE_TOO_MANY_REQUESTS ||
                        code >= HttpRequest.STATUS_CODE_SERVER_ERROR) {
                        // if request failed due to server error or connection error, or if request was cancelled,
                        // retry later
                        ULog.d(TAG_ENGINE, "Failed to register devices, retry later: " + devices);
                    } else {
                        // if request succeed or if request failed to due to another error,
                        // mark the devices as 'registered' to not try to register again them later
                        onRegisterDeviceComplete(devices);
                    }
                });
            }
        }
    }

    /**
     * Cancels any ongoing register request.
     */
    private void cancelCurrentUpload() {
        if (mCurrentRequest != null) {
            mCurrentRequest.cancel();
        }
    }

    /** Listens to internet connection availability changes. */
    @NonNull
    private final SystemConnectivity.Monitor mInternetMonitor = available -> {
        if (available) {
            mHttpClient = createHttpClient();
            registerDevices();
        } else if (mHttpClient != null) {
            mHttpClient.dispose();
            mHttpClient = null;
        }
    };

    /**
     * Tells whether the given device may be registered based on his board id.
     *
     * @param device device to test
     *
     * @return {@code true} if the device may be registered, otherwise {@code false}
     */
    private static boolean hasRegistrableBoardId(@NonNull DeviceCore device) {
        String boardId = device.getBoardId();
        if (boardId == null) return false;
        if (!boardId.startsWith("0x")) return true;
        try {
            return Integer.parseInt(boardId.substring(2), 16) == 0;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /** Listens to changes on known drones. */
    @NonNull
    private final DeviceStore.Monitor<DeviceCore> mDroneStoreMonitor = new DeviceStore.Monitor<DeviceCore>() {

        @Override
        public void onDeviceChanged(@NonNull DeviceCore device) {
            registerDevices();
        }
    };

    /**
     * Creates the HTTP Activation client.
     * <p>
     * Only used by tests to mock the HTTP client
     *
     * @return HTTP Activation client
     */
    @VisibleForTesting
    @NonNull
    HttpActivationClient createHttpClient() {
        return new HttpActivationClient(getContext());
    }

    @Override
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
        mPersistence.dump(writer, args);
    }
}
