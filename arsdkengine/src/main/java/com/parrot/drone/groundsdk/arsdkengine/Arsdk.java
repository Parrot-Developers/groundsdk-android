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
import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.internal.device.DeviceConnectorCore;
import com.parrot.drone.groundsdk.internal.device.DeviceModels;
import com.parrot.drone.sdkcore.arsdk.ArsdkCore;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.blackbox.ArsdkBlackBoxRequest;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkNoAckCmdEncoder;
import com.parrot.drone.sdkcore.arsdk.crashml.ArsdkCrashmlDownloadRequest;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkDevice;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkRequest;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkTcpProxy;
import com.parrot.drone.sdkcore.arsdk.firmware.ArsdkFirmwareUploadRequest;
import com.parrot.drone.sdkcore.arsdk.flightlog.ArsdkFlightLogDownloadRequest;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Set;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG;

/**
 * Delegate of arsdk engine that specifically manages devices connected directly through arsdk backends (WIFI, BLE,
 * MUX), versus devices connected using a proxy device (RC).
 */
class Arsdk {

    /** Arsdk engine instance. */
    @NonNull
    private final ArsdkEngine mEngine;

    /** Arsdk control instance. */
    @NonNull
    private final ArsdkCore mArsdkCore;

    /**
     * Constructor.
     *
     * @param engine arsdk engine instance
     */
    Arsdk(@NonNull ArsdkEngine engine) {
        mEngine = engine;
        mArsdkCore = mEngine.createArsdkCore(mArsdkCoreListener);
    }

    /**
     * Starts arsdk.
     */
    void start() {
        mArsdkCore.start();
    }

    /**
     * Stops arsdk.
     */
    void stop() {
        mArsdkCore.stop();
    }

    /** Arsdk device listener. */
    @SuppressWarnings("FieldCanBeLocal")
    private final ArsdkCore.Listener mArsdkCoreListener = new ArsdkCore.Listener() {

        @Override
        public void onDeviceAdded(@NonNull ArsdkDevice device) {
            @DeviceModel.Id int modelId = device.getType();
            DeviceModel model = DeviceModels.model(modelId);
            if (model != null) {
                LocalDeviceProvider provider = getProviderForBackendType(device.getBackendType());
                if (provider != null) {
                    DeviceController controller = mEngine.getOrCreateDeviceController(
                            device.getUid(), model, device.getName());
                    provider.add(controller, new DeviceCtrlBackend(controller, device));
                    controller.addDeviceProvider(provider);
                }
            }
        }

        @Override
        public void onDeviceRemoved(@NonNull ArsdkDevice device) {
            DeviceController controller = mEngine.getExistingDeviceController(device.getUid());
            if (controller != null) {
                LocalDeviceProvider provider = getProviderForBackendType(device.getBackendType());
                if (provider != null) {
                    provider.remove(controller);
                    controller.removeDeviceProvider(provider);
                }
            }
        }
    };

    /**
     * Gets the provider for a given backend type.
     *
     * @param backendType type of backend
     *
     * @return an instance of LocalDeviceProvider or null if the backend type is unknown.
     */
    @Nullable
    private LocalDeviceProvider getProviderForBackendType(@Backend.Type int backendType) {
        switch (backendType) {
            case Backend.TYPE_NET:
                return mLocalWifiDeviceProvider;
            case Backend.TYPE_MUX:
                return mLocalUsbDeviceProvider;
            case Backend.TYPE_BLE:
                return mLocalBleDeviceProvider;
            case Backend.TYPE_UNKNOWN:
                ULog.w(TAG, "Backend type is unknown, this is unexpected.");
                return null;
        }
        return null;
    }

    /** Local device provider, that uses Wifi technology. */
    private final LocalDeviceProvider mLocalWifiDeviceProvider =
            new LocalDeviceProvider(DeviceConnector.Technology.WIFI);

    /** Local device provider, that uses usb technology. */
    private final LocalDeviceProvider mLocalUsbDeviceProvider = new LocalDeviceProvider(DeviceConnector.Technology.USB);

    /** Local device provider, that uses BLE technology. */
    private final LocalDeviceProvider mLocalBleDeviceProvider = new LocalDeviceProvider(DeviceConnector.Technology.BLE);

    /** Local device provider, that arsdk implements. */
    private static final class LocalDeviceProvider extends DeviceProvider {

        /** Dictionary that associates a device controller to its backend. */
        private final HashMap<DeviceController, DeviceCtrlBackend> backends = new HashMap<>();

        /**
         * Constructor.
         *
         * @param techno the technology use by this provider
         */
        LocalDeviceProvider(@NonNull DeviceConnector.Technology techno) {
            super(DeviceConnectorCore.createLocalConnector(techno));
        }

        /**
         * adds a device controller to this provider and associate it to its backend.
         *
         * @param deviceController the device controller to add
         * @param backend          the backend associated to the given device controller
         */
        private void add(@NonNull DeviceController deviceController, @NonNull DeviceCtrlBackend backend) {
            backends.put(deviceController, backend);
        }

        /**
         * Removes a device controller from this provider.
         *
         * @param deviceController the device controller to remove
         */
        private void remove(@NonNull DeviceController deviceController) {
            backends.remove(deviceController);
        }

        @Override
        public boolean connectDevice(@NonNull DeviceController deviceController, @Nullable String password) {
            DeviceCtrlBackend backend = backends.get(deviceController);
            return backend != null && backend.connect();
        }

        @Override
        public boolean disconnectDevice(@NonNull DeviceController deviceController) {
            DeviceCtrlBackend backend = backends.get(deviceController);
            return backend != null && backend.disconnect();
        }

        @Override
        public String toString() {
            return "ArsdkDeviceProvider-" + getConnector();
        }
    }

    /** Device controller backend implementation for devices connected through arsdk local provider. */
    private final class DeviceCtrlBackend implements DeviceController.Backend, ArsdkDevice.Listener {

        /** Device controller for which this is the backend. */
        @NonNull
        final DeviceController mDeviceController;

        /** Native arsdk handle used to communicate with this device controller. */
        @NonNull
        final ArsdkDevice mDevice;

        /**
         * Constructor.
         *
         * @param deviceController device controller for which this is the backend
         * @param device           device that this backend is a facade for
         */
        DeviceCtrlBackend(@NonNull DeviceController deviceController, @NonNull ArsdkDevice device) {
            mDeviceController = deviceController;
            mDevice = device;
        }

        @NonNull
        @Override
        public DeviceController.Backend asProxyFor(@NonNull DeviceController deviceController) {
            return new DeviceCtrlBackend(deviceController, mDevice);
        }

        @Override
        public ArsdkTcpProxy createTcpProxy(int port, @NonNull ArsdkTcpProxy.Listener listener) {
            @ArsdkDevice.Type int type = mDeviceController.getDevice().getModel().id();
            return mDevice.createTcpProxy(type, port, listener);
        }

        @Override
        public boolean sendCommand(@NonNull ArsdkCommand command) {
            mDevice.sendCommand(command);
            return true;
        }

        @Override
        public void setNoAckCommandLoopPeriod(int period) {
            mDevice.setNoAckCommandLoopPeriod(period);
        }

        @Override
        public void registerNoAckCommandEncoders(@NonNull ArsdkNoAckCmdEncoder... encoders) {
            for (ArsdkNoAckCmdEncoder encoder : encoders) {
                mDevice.registerNoAckCommandEncoder(encoder);
            }
        }

        @Override
        public void unregisterNoAckCommandEncoders(@NonNull ArsdkNoAckCmdEncoder... encoders) {
            for (ArsdkNoAckCmdEncoder encoder : encoders) {
                mDevice.unregisterNoAckCommandEncoder(encoder);
            }
        }

        @NonNull
        @Override
        public SdkCoreStream openVideoStream(@NonNull String url, @Nullable String track,
                                             @NonNull SdkCoreStream.Client client) {
            return mDevice.openVideoStream(url, track, client);
        }

        @NonNull
        @Override
        public ArsdkRequest downloadCrashml(@NonNull String dstPath,
                                            @NonNull ArsdkCrashmlDownloadRequest.Listener listener) {
            @ArsdkDevice.Type int type = mDeviceController.getDevice().getModel().id();
            return mDevice.downloadCrashml(type, dstPath, listener);
        }

        @NonNull
        @Override
        public ArsdkRequest downloadFlightLog(@NonNull String dstPath,
                                              @NonNull ArsdkFlightLogDownloadRequest.Listener listener) {
            @ArsdkDevice.Type int type = mDeviceController.getDevice().getModel().id();
            return mDevice.downloadFlightLog(type, dstPath, listener);
        }

        @NonNull
        @Override
        public ArsdkRequest uploadFirmware(@NonNull String srcPath,
                                           @NonNull ArsdkFirmwareUploadRequest.Listener listener) {
            @ArsdkDevice.Type int type = mDeviceController.getDevice().getModel().id();
            return mDevice.uploadFirmware(type, srcPath, listener);
        }

        @NonNull
        @Override
        public ArsdkRequest subscribeToBlackBox(@NonNull ArsdkBlackBoxRequest.Listener listener) {
            return mDevice.subscribeToBlackBox(listener);
        }

        @Override
        public void onConnecting() {
            LocalDeviceProvider provider = getProviderForBackendType(mDevice.getBackendType());
            assert provider != null;
            mDeviceController.onLinkConnecting(provider);
        }

        @Override
        public void onConnected() {
            LocalDeviceProvider provider = getProviderForBackendType(mDevice.getBackendType());
            assert provider != null;

            mDeviceController.onApiCapabilities(mDevice.getApiCapabilities());
            mDeviceController.onLinkConnected(provider, this);
        }

        @Override
        public void onDisconnected(boolean removing) {
            mDeviceController.onLinkDisconnected(removing);
            if (removing) {
                // disconnected because the device is about to be removed, remove the provider
                // to make the device not connectable with this provider
                LocalDeviceProvider provider = getProviderForBackendType(mDevice.getBackendType());
                assert provider != null;
                mDeviceController.removeDeviceProvider(provider);
            }
        }

        @Override
        public void onConnectionCanceled(@ArsdkDevice.ConnectionCancelReason int reason, boolean removing) {
            DeviceState.ConnectionStateCause cause = DeviceState.ConnectionStateCause.NONE;
            switch (reason) {
                case ArsdkDevice.REASON_CANCELED_LOCALLY:
                    cause = DeviceState.ConnectionStateCause.USER_REQUESTED;
                    break;
                case ArsdkDevice.REASON_CANCELED_BY_REMOTE:
                    cause = DeviceState.ConnectionStateCause.FAILURE;
                    break;
                case ArsdkDevice.REASON_REJECTED_BY_REMOTE:
                    cause = DeviceState.ConnectionStateCause.REFUSED;
                    break;
            }
            mDeviceController.onLinkConnectionCanceled(cause, removing);
            if (removing) {
                // connection canceled because the device is about to be removed, remove the provider
                // to make the device not connectable with this provider
                LocalDeviceProvider provider = getProviderForBackendType(mDevice.getBackendType());
                assert provider != null;
                mDeviceController.removeDeviceProvider(provider);
            }
        }

        @Override
        public void onLinkDown() {
            mDeviceController.onLinkLost();
        }

        @Override
        public void onCommandReceived(@NonNull ArsdkCommand command) {
            mDeviceController.onCommandReceived(command);
        }

        /**
         * Connects the device managed by the associated controller.
         *
         * @return {@code true} if the connection could be successfully initiated, otherwise {@code false}
         */
        boolean connect() {
            mDevice.connect(this);
            return true;
        }

        /**
         * Disconnects the device managed by the associated controller.
         *
         * @return {@code true} if the disconnection could be successfully initiated, otherwise {@code false}
         */
        boolean disconnect() {
            mDevice.disconnect();
            return true;
        }

        @Override
        public String toString() {
            return mDeviceController + " [handle: " + mDevice + "]";
        }
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     */
    void dump(@NonNull PrintWriter writer, @NonNull Set<String> args) {
        if (args.isEmpty() || args.contains("--help")) {
            writer.write("\t--local-backends: dumps local provider backends\n");
        } else if (args.contains("--local-backends") || args.contains("--all")) {
            writer.write("Local WIFI provider backends: " + mLocalWifiDeviceProvider.backends.size() + "\n");
            for (DeviceCtrlBackend backend : mLocalWifiDeviceProvider.backends.values()) {
                writer.write("\t" + backend + "\n");
            }
            writer.write("Local USB provider backends: " + mLocalUsbDeviceProvider.backends.size() + "\n");
            for (DeviceCtrlBackend backend : mLocalUsbDeviceProvider.backends.values()) {
                writer.write("\t" + backend + "\n");
            }
            writer.write("Local BLE provider backends: " + mLocalBleDeviceProvider.backends.size() + "\n");
            for (DeviceCtrlBackend backend : mLocalBleDeviceProvider.backends.values()) {
                writer.write("\t" + backend + "\n");
            }
        }
        mArsdkCore.dump(writer, args);
    }
}
