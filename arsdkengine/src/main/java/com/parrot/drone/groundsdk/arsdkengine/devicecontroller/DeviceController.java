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

import android.text.TextUtils;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngine;
import com.parrot.drone.groundsdk.arsdkengine.DeviceProvider;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.BlackBoxRecorder;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.BlackBoxSession;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.device.DeviceConnectorCore;
import com.parrot.drone.groundsdk.internal.device.DeviceCore;
import com.parrot.drone.groundsdk.internal.device.DeviceStateCore;
import com.parrot.drone.groundsdk.internal.http.HttpClient;
import com.parrot.drone.groundsdk.internal.http.HttpSession;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
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

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_CTRL;

/**
 * Base class for a device controller.
 *
 * @param <D> type of the controlled device
 */
public abstract class DeviceController<D extends DeviceCore> {

    /** Interface called back when the device controller state changes. */
    public interface OnStateChangedListener {

        /**
         * Notifies that the device controller state has changed.
         *
         * @param uid   controller device uid
         * @param state new controller state
         */
        void onStateChanged(@NonNull String uid, @NonNull State state);
    }

    /** Device controller state. */
    public enum State {

        /** Controller is stopped. */
        STOPPED,

        /** Controller is idle, it is started but there is no active device connection. */
        IDLE,

        /** Controller is active, a device is connected or connecting. */
        ACTIVE
    }

    /** Connection state of the controller. */
    enum ControllerConnectionState {
        /** Controller is fully disconnected. */
        DISCONNECTED,

        /** Controller has initiated the connection. */
        CONNECTING,

        /** Controller is setting up TCP Proxy. */
        CREATING_TCP_PROXY,

        /** Controller is getting all settings of the drone. */
        GETTING_ALL_SETTINGS,

        /** Controller is getting all states of the drone. */
        GETTING_ALL_STATES,

        /** Controller is fully connected to the drone. */
        CONNECTED,

        /** Controller is disconnecting the drone. */
        DISCONNECTING
    }

    /**
     * Device controller protocol backend.
     * <p>
     * Used by the controller, after link connection is established, in order to send commands to the associated
     * device.
     */
    public interface Backend {

        /**
         * Creates a new backend that proxies all requests to this backend on behalf ot the given device controller.
         *
         * @param deviceController device controller that will use the proxy backend
         *
         * @return a new proxy backend instance for the given controller
         */
        @NonNull
        Backend asProxyFor(@NonNull DeviceController deviceController);

        /**
         * Sends a command to the controlled device.
         *
         * @param command command to send
         *
         * @return {@code true} if the command could be sent, otherwise {@code false}
         */
        boolean sendCommand(@NonNull ArsdkCommand command);

        /**
         * Configures the non-acknowledged command loop period.
         * <p>
         * If the period changes, and encoders are currently {@link #registerNoAckCommandEncoders registered}, the
         * command loop is stopped if it was started, then started again (if the new period is strictly positive).
         * <p>
         * Loop period is reset to zero when the connection to the device closes for any reason.
         *
         * @param period loop period, in milliseconds. {@code 0} to stop the loop
         */
        void setNoAckCommandLoopPeriod(int period);

        /**
         * Registers encoders to be executed in the non-acknowledged command loop.
         * <p>
         * If a strictly positive loop period is currently {@link #setNoAckCommandLoopPeriod setup}, and these are the
         * first registered encoders, the loop is started.
         * <p>
         * All registered encoders are unregistered when the connection to the device closes for any reason.
         *
         * @param encoders non-acknowledged command encoders to register
         */
        void registerNoAckCommandEncoders(@NonNull ArsdkNoAckCmdEncoder... encoders);

        /**
         * Unregisters encoders from being executed in the non-acknowledged command loop.
         * <p>
         * If the loop is started and these are the last registered encoders, the loop is stopped.
         * <p>
         * All registered encoders are unregistered when the connection to the device closes for any reason.
         *
         * @param encoders non-acknowledged command encoders to unregister
         */
        void unregisterNoAckCommandEncoders(@NonNull ArsdkNoAckCmdEncoder... encoders);

        /**
         * Requests a video stream to be opened from the controlled device.
         *
         * @param url    video stream URL
         * @param track  stream track to select, {@code null} to select default track, if any
         * @param client client notified of video stream events
         *
         * @return a new, opening video stream instance
         */
        @NonNull
        SdkCoreStream openVideoStream(@NonNull String url, @Nullable String track,
                                      @NonNull SdkCoreStream.Client client);

        /**
         * Requests to download crashml reports from the controlled device.
         *
         * @param dstPath  directory path where store downloaded crashml
         * @param listener listener that will be called back upon request progress and completion
         *
         * @return an ArsdkRequest, that can be canceled.
         */
        @NonNull
        ArsdkRequest downloadCrashml(@NonNull String dstPath, @NonNull ArsdkCrashmlDownloadRequest.Listener listener);

        /**
         * Requests to download flight logs from the controlled device.
         *
         * @param dstPath  directory path where store downloaded flight logs
         * @param listener listener that will be called back upon request progress and completion
         *
         * @return an ArsdkRequest, that can be canceled.
         */
        @NonNull
        ArsdkRequest downloadFlightLog(@NonNull String dstPath,
                                       @NonNull ArsdkFlightLogDownloadRequest.Listener listener);

        /**
         * Requests a firmware to be uploaded on the controlled device.
         *
         * @param srcPath  absolute local path of the firmware file to be uploaded
         * @param listener listener that will be called back upon request progress and completion
         *
         * @return an ArsdkRequest, that can be canceled
         */
        @NonNull
        ArsdkRequest uploadFirmware(@NonNull String srcPath, @NonNull ArsdkFirmwareUploadRequest.Listener listener);

        /**
         * Requests to receive black box data from the controlled device.
         *
         * @param listener listener that will be called back each time new black box data is available
         *
         * @return an ArsdkRequest, that can be canceled
         */
        @NonNull
        ArsdkRequest subscribeToBlackBox(@NonNull ArsdkBlackBoxRequest.Listener listener);

        /**
         * Creates a TCP proxy with the device.
         *
         * @param port     port to access
         * @param listener completion listener
         *
         * @return a new TCP proxy instance
         */
        ArsdkTcpProxy createTcpProxy(int port, @NonNull ArsdkTcpProxy.Listener listener);
    }

    /** Factory provided by subclasses. Used to create the controlled device associated with the given delegate. */
    interface DeviceFactory<D extends DeviceCore> {

        /**
         * Creates a device linked to the given delegate.
         *
         * @param delegate the device delegate to link the device with
         *
         * @return the created device instance
         */
        @NonNull
        D create(@NonNull DeviceCore.Delegate delegate);
    }

    /** Timeout waiting either the all states or all settings from the device. */
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(20);

    /** ArsdkEngine managing this controller. */
    private final ArsdkEngine mEngine;

    /** Device controlled by this controller. */
    @NonNull
    private final D mDevice;

    /** Persistent dictionary containing device specific info: name, device type, current preset... */
    @NonNull
    private final PersistentStore.Dictionary mDeviceDict;

    /** Device current preset dictionary, containing all preset setting values. */
    @NonNull
    private final PersistentStore.Dictionary mPresetDict;

    /** List of all components of the managed device. */
    @NonNull
    private final List<DeviceComponentController<?, ?>> mComponentControllers;

    /** Registered providers for this device controller, by connector. */
    @NonNull
    private final Map<DeviceConnector, DeviceProvider> mDeviceProviders;

    /** Non-acknowledged command loop period, in milliseconds. {@code 0} if disabled. */
    private final int mNoAckLoopPeriod;

    /** Currently active provider for this controller, {@code null} when the device controller is disconnected. */
    @Nullable
    private DeviceProvider mActiveProvider;

    /** Current controller state. */
    @NonNull
    private State mState;

    /** Connection state of the controller. */
    @NonNull
    private ControllerConnectionState mConnectionState;

    /** Called back when the device controller state changes. */
    @Nullable
    private OnStateChangedListener mStateChangeListener;

    /** Black box session. {@code null} if black box support is disabled or device is not protocol-connected. */
    @Nullable
    private BlackBoxSession mBlackBoxSession;

    /**
     * HTTP proxy, {@code null} until the device controller is successfully link-connected and after disconnection.
     */
    @Nullable
    private ArsdkTcpProxy mHttpProxy;

    /**
     * HTTP session, {@code null} until the device controller is successfully link-connected and after disconnection.
     */
    @Nullable
    private HttpSession mHttpSession;

    /** Device controller protocol backend, {@code null} until the device controller is successfully link-connected. */
    @Nullable
    private Backend mBackend;

    /** {@code true} when the controller must attempt to reconnect the device after disconnection. */
    private boolean mAutoReconnect;

    /** Memorizes the previous data sync allowance value in order to notify only if it has changed. */
    private boolean mPreviousDataSyncAllowed;

    /** API capabilities. */
    @ArsdkDevice.Api
    private int mApiCapabilities;

    /**
     * Constructor.
     *
     * @param engine           arsdk engine instance
     * @param deviceFactory    factory used to create the controlled device
     * @param nonAckLoopPeriod non-acknowledged command loop period, {@code 0} to disable
     */
    DeviceController(@NonNull ArsdkEngine engine, @NonNull DeviceFactory<D> deviceFactory, int nonAckLoopPeriod) {
        mEngine = engine;
        mState = State.STOPPED;
        mConnectionState = ControllerConnectionState.DISCONNECTED;
        mDevice = deviceFactory.create(mDeviceDelegate);
        mComponentControllers = new ArrayList<>();
        mDeviceProviders = new HashMap<>();
        mNoAckLoopPeriod = nonAckLoopPeriod;
        PersistentStore persistentStore = engine.getPersistentStore();
        mDeviceDict = persistentStore.getDevice(mDevice.getUid());
        mDevice.getDeviceStateCore().updatePersisted(!mDeviceDict.isNew()).notifyUpdated();
        String firmwareVersionStr = mDeviceDict.getString(PersistentStore.KEY_DEVICE_FIRMWARE_VERSION);
        FirmwareVersion version = firmwareVersionStr == null ? null : FirmwareVersion.parse(firmwareVersionStr);
        if (version != null) {
            mDevice.updateFirmwareVersion(version);
        }
        String boardId = mDeviceDict.getString(PersistentStore.KEY_DEVICE_BOARD_ID);
        if (boardId != null) {
            mDevice.updateBoardId(boardId);
        }
        String presetUid = mDeviceDict.getString(PersistentStore.KEY_DEVICE_PRESET_KEY);
        if (presetUid == null) {
            presetUid = PersistentStore.getDefaultPresetKey(mDevice.getModel());
        }
        mPresetDict = persistentStore.getPreset(presetUid, mPresetObserver);
        mApiCapabilities = ArsdkDevice.API_UNKNOWN;
    }

    /**
     * Retrieves the engine instance that manages this controller.
     *
     * @return the arsdk engine instance
     */
    @NonNull
    public final ArsdkEngine getEngine() {
        return mEngine;
    }

    /**
     * Gets the device controller by this controller.
     *
     * @return the controlled device
     */
    @NonNull
    public final D getDevice() {
        return mDevice;
    }

    /**
     * Gets the device controller uid.
     * <p>
     * The device controller uid is the controlled device's uid.
     *
     * @return the controller uid
     */
    @NonNull
    public final String getUid() {
        return mDevice.getUid();
    }

    /**
     * Gets the device dictionary.
     *
     * @return the device dictionary
     */
    @NonNull
    public final PersistentStore.Dictionary getDeviceDict() {
        return mDeviceDict;
    }

    /**
     * Gets the device current preset dictionary.
     *
     * @return the device current preset dictionary
     */
    @NonNull
    public final PersistentStore.Dictionary getPresetDict() {
        return mPresetDict;
    }

    /**
     * Starts the controller.
     *
     * @param listener listener called if the device controller changes state
     */
    public final void start(@Nullable OnStateChangedListener listener) {
        mStateChangeListener = listener;
        setState(State.IDLE);
        onStarted();
    }

    /**
     * Gets current controller state.
     *
     * @return current controller state
     */
    @NonNull
    public final State getState() {
        return mState;
    }

    /**
     * Gets current black box session.
     *
     * @return current black box session if any, otherwise {@code null}
     */
    @Nullable
    BlackBoxSession getBlackBoxSession() {
        return mBlackBoxSession;
    }

    /**
     * Registers a provider for the controlled device.
     * <p>
     * Proxy device controllers may spontaneously (without user action) connect a proxied device, and so may register
     * their device provider as the active one by arming the setActive flag.
     *
     * @param deviceProvider device provider to register
     */
    public final void addDeviceProvider(@NonNull DeviceProvider deviceProvider) {
        if (mDeviceProviders.put(deviceProvider.getConnector(), deviceProvider) != deviceProvider) {
            onProvidersChanged();
        }
        if (mAutoReconnect && mActiveProvider == null) {
            connectDevice(deviceProvider, null, DeviceState.ConnectionStateCause.CONNECTION_LOST);
        }
        mDevice.getDeviceStateCore().notifyUpdated();
    }

    /**
     * Unregisters a provider of the controlled device.
     * <p>
     * When all registered providers have been removed, and the associated device info is not recorded in persistent
     * storage, then this controller stops itself.
     *
     * @param deviceProvider device provider to unregister
     */
    public final void removeDeviceProvider(@NonNull DeviceProvider deviceProvider) {
        DeviceProvider removedProvider = mDeviceProviders.remove(deviceProvider.getConnector());
        if (removedProvider != null) {
            if (removedProvider == mActiveProvider) {
                mActiveProvider = null;
                mAutoReconnect = true;
                handleDisconnection(DeviceState.ConnectionStateCause.CONNECTION_LOST);
            }
            onProvidersChanged();

            if (mDeviceProviders.isEmpty() && mDeviceDict.isNew()) {
                stopSelf();
            }

            mDevice.getDeviceStateCore().notifyUpdated();
        }
    }

    /**
     * Gets currently active provider for this controller.
     *
     * @return the active provider, or {@code null} if the device controller is disconnected
     */
    @Nullable
    public DeviceProvider getActiveProvider() {
        return mActiveProvider;
    }

    /**
     * Gets the protocol backend of the controller.
     *
     * @return the controller protocol backend if link-connected, otherwise {@code null}
     */
    @Nullable
    public final Backend getProtocolBackend() {
        return mBackend;
    }

    /**
     * Sends a command to the managed device.
     *
     * @param command the command to send.
     *
     * @return {@code true} when the command could be sent, otherwise {@code false}
     */
    public final boolean sendCommand(@NonNull ArsdkCommand command) {
        if (mBackend != null) {
            return mBackend.sendCommand(command);
        } else {
            command.release();
        }
        return false;
    }

    /**
     * Updates the managed device's firmware.
     *
     * @param firmwareFile path of the firmware file to update the device with
     * @param listener     listener that will be called back upon request progress and completion
     *
     * @return an ArsdkRequest, that can be canceled, {@code null} if the device is not connected.
     */
    @Nullable
    public final ArsdkRequest updateFirmware(@NonNull File firmwareFile,
                                             @NonNull ArsdkFirmwareUploadRequest.Listener listener) {
        return mBackend == null ? null : mBackend.uploadFirmware(firmwareFile.getAbsolutePath(), listener);
    }

    /**
     * Downloads crashml reports from the controlled device.
     *
     * @param path     directory path where download crashml reports
     * @param listener listener that will be called back upon request progress and completion
     *
     * @return an ArsdkRequest, that can be canceled, {@code null} if the device is not connected.
     */
    @Nullable
    public final ArsdkRequest downloadCrashml(@NonNull File path,
                                              @NonNull ArsdkCrashmlDownloadRequest.Listener listener) {
        return mBackend == null ? null : mBackend.downloadCrashml(path.getPath(), listener);
    }

    /**
     * Downloads flight logs from the controlled device.
     *
     * @param path     directory path where download flight logs
     * @param listener listener that will be called back upon request progress and completion
     *
     * @return an ArsdkRequest, that can be canceled, {@code null} if the device is not connected.
     */
    @Nullable
    public final ArsdkRequest downloadFlightLog(@NonNull File path,
                                                @NonNull ArsdkFlightLogDownloadRequest.Listener listener) {
        return mBackend == null ? null : mBackend.downloadFlightLog(path.getPath(), listener);
    }

    /**
     * Called when a command is received from the controlled device.
     * <p>
     * Forwards the command to all controller's component controllers. <br/>
     * Subclasses may override this method to perform further processing, but <strong>MUST</strong> call super in that
     * case.
     *
     * @param command received command
     */
    @CallSuper
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        // Note: intentional classic for-loop, otherwise an iterator is allocated for each received command.
        for (int i = 0, N = mComponentControllers.size(); i < N; i++) {
            mComponentControllers.get(i).onCommandReceived(command);
        }
        if (mBlackBoxSession != null) {
            mBlackBoxSession.onCommandReceived(command);
        }
    }

    /**
     * Forces the controller to stop.
     * <p>
     * Called when the engine finally comes to a full stop (when no other controller remains active) to move all {@link
     * State#IDLE idle} controllers to the {@link State#STOPPED} state.
     * <p>
     * This method guarantees to bring the controller to the {@link State#STOPPED} state. As a consequence, state change
     * listener, if any, is unregistered before stopping, and won't be called.
     */
    public final void forceStop() {
        if (mState == State.ACTIVE) {
            ULog.w(TAG_CTRL, "Forced stopped " + this + " while still active");
        }
        mStateChangeListener = null;
        stopSelf();
    }

    /**
     * Called when API capabilities of the managed device are known.
     * @param api API capabilities
     */
    public final void onApiCapabilities(@ArsdkDevice.Api int api) {
        if (api != mApiCapabilities) {
            if (api == ArsdkDevice.API_UNKNOWN) throw new IllegalArgumentException();
            mApiCapabilities = api;
            mComponentControllers.forEach( component -> component.onApiCapabilities(api));
        }
    }

    /**
     * Called when link-level connection with the controlled device begins.
     *
     * @param provider {@code DeviceProvider} that engaged link-level connection
     */
    public final void onLinkConnecting(@NonNull DeviceProvider provider) {
        if (mActiveProvider == null || mActiveProvider == provider) {
            mActiveProvider = provider;
            mAutoReconnect = false;
            mConnectionState = ControllerConnectionState.CONNECTING;
            setState(State.ACTIVE);
            mDevice.getDeviceStateCore().updateConnectionState(DeviceState.ConnectionState.CONNECTING)
                   .updateActiveConnector(mActiveProvider.getConnector()).notifyUpdated();
        }
    }

    /**
     * Called when link-level connection with the controlled device has been successfully established.
     *
     * @param provider {@code DeviceProvider} that competed link-level connection
     * @param backend  protocol backend to use to communicate with the controlled device
     */
    public final void onLinkConnected(@NonNull DeviceProvider provider, @NonNull Backend backend) {
        // a proxy device controller may callback directly here (without calling onLinkConnecting), so make sure to
        // pass through connecting state
        if (mConnectionState != ControllerConnectionState.CONNECTING) {
            onLinkConnecting(provider);
        }

        mConnectionState = ControllerConnectionState.CREATING_TCP_PROXY;
        mBackend = backend;

        mBackend.setNoAckCommandLoopPeriod(mNoAckLoopPeriod);
        onProtocolConnecting();

        sendDate(new Date());

        // create HTTP proxy
        mHttpProxy = mBackend.createTcpProxy(80, this::onCreateTcpProxyCompleted);
        postConnectionTimeout();
    }

    /**
     * Called when link-level disconnection from the controlled device occurs.
     *
     * @param removing flag to indicate that the device is about to be removed
     */
    public final void onLinkDisconnected(boolean removing) {
        mAutoReconnect |= removing;
        handleDisconnection(removing ? DeviceState.ConnectionStateCause.CONNECTION_LOST : null);
        mDevice.getDeviceStateCore().notifyUpdated();
    }

    /**
     * Called when link-level connection to the controlled device gets canceled.
     *
     * @param cause    reason why connection has been canceled
     * @param removing flag to indicate that the device is about to be removed
     */
    public final void onLinkConnectionCanceled(@NonNull DeviceState.ConnectionStateCause cause, boolean removing) {
        mAutoReconnect |= removing;
        handleDisconnection(removing ? DeviceState.ConnectionStateCause.CONNECTION_LOST : cause);
        mDevice.getDeviceStateCore().notifyUpdated();
    }

    /**
     * Called when link to the controlled device is lost.
     */
    public final void onLinkLost() {
        for (DeviceComponentController componentController : mComponentControllers) {
            componentController.onLinkLost();
        }
        mAutoReconnect = true;
        disconnectDevice(DeviceState.ConnectionStateCause.CONNECTION_LOST);
    }

    /**
     * Connects the controlled device using the specified provider and password.
     * <p>
     * Checks that the given provider uid is in the list of the controller's device providers, then tries to connect the
     * device using that provider, otherwise does nothing and return {@code false}.
     * <p>If the connection could be successfully initiated, then the specified provider becomes the active
     * device provider for that controller.
     *
     * @param provider the device provider
     * @param password password to use for authentication. Use {@code null} if the device connection is not secured, or
     *                 to use the provider's saved password, if any (for RC providers)
     * @param cause    cause of this connection request
     *
     * @return {@code true} if the connection could be successfully initiated, otherwise {@code false}
     */
    public final boolean connectDevice(@NonNull DeviceProvider provider, @Nullable String password,
                                       @NonNull DeviceState.ConnectionStateCause cause) {
        if (mConnectionState == ControllerConnectionState.DISCONNECTED) {
            if (provider.connectDevice(this, password)) {
                mConnectionState = ControllerConnectionState.CONNECTING;
                mActiveProvider = provider;
                setState(State.ACTIVE);
                mDevice.getDeviceStateCore().updateConnectionState(DeviceState.ConnectionState.CONNECTING, cause)
                       .updateActiveConnector(mActiveProvider.getConnector()).notifyUpdated();
                return true;
            }
        }
        return false;
    }

    /**
     * Disconnects the controlled device.
     *
     * @param cause cause for disconnection
     *
     * @return {@code true} if the disconnection could be successfully initiated, otherwise {@code false}
     */
    private boolean disconnectDevice(@NonNull DeviceState.ConnectionStateCause cause) {
        if (mActiveProvider != null && mActiveProvider.disconnectDevice(this)) {
            mConnectionState = ControllerConnectionState.DISCONNECTING;
            mDevice.getDeviceStateCore().updateConnectionState(DeviceState.ConnectionState.DISCONNECTING, cause)
                   .notifyUpdated();
            onProtocolDisconnecting();
            return true;
        }

        return false;
    }

    /**
     * Called when started.
     * <p>
     * Subclasses must override this method and publish the controlled device to the appropriate store.
     */
    abstract void onStarted();

    /**
     * Called when the controller must stop. Unpublishes the controlled device from groundsdk store.
     * <p>
     * Subclasses must override this method and unpublish the controlled device from the appropriate store.
     */
    @CallSuper
    void onStopped() {
        for (DeviceComponentController componentController : mComponentControllers) {
            componentController.onDispose();
        }
    }

    /**
     * Called when failed or succeed to create a TCP proxy with the controlled device.
     *
     * @param address       proxy address or {@code null} if failed to create proxy
     * @param port          proxy port
     * @param socketFactory factory for creating sockets bound to the network through which this proxy communicates, may
     *                      be {@code null} in case network-bound sockets are not relevant or not supported by the
     *                      device backend
     */
    private void onCreateTcpProxyCompleted(@Nullable String address, int port, @Nullable SocketFactory socketFactory) {
        ULog.i(TAG_CTRL, "TCP proxy created [address: " + address + ", port: " + port + "]");
        if (address != null && port != 0) {
            // Create HttpSession
            mHttpSession = new HttpSession(address, port, socketFactory);
        }
        mConnectionState = ControllerConnectionState.GETTING_ALL_SETTINGS;
        sendGetAllSettings();
    }

    /**
     * Called when link-level connection is established and the controller can further proceed with protocol-level
     * connection.
     * <p>
     * Forwards the state to all controller's component controllers. <br/>
     * Subclasses may override this method to perform further processing, but <strong>MUST</strong> call super in that
     * case.
     */
    @CallSuper
    void onProtocolConnecting() {
        BlackBoxRecorder recorder = getEngine().getBlackBoxRecorder();
        if (recorder != null) {
            assert mActiveProvider != null; // if the controller is connecting, then there is an active connector
            mBlackBoxSession = openBlackBoxSession(recorder, mActiveProvider.getConnector().getUid(),
                    () -> mBlackBoxSession = null);
        }

        for (DeviceComponentController<?, ?> controller : mComponentControllers) {
            controller.onConnecting();
        }
    }

    /**
     * Called when protocol-level connection with the device has been fully established.
     * <p>
     * Forwards the state to all controller's component controllers. <br/>
     * Subclasses may override this method to perform further processing, but <strong>MUST</strong> call super in that
     * case.
     */
    @CallSuper
    void onProtocolConnected() {
        for (DeviceComponentController<?, ?> controller : mComponentControllers) {
            controller.onConnected();
        }

        notifyDataSyncConditionsChanged();
    }

    /**
     * Called when disconnection of the controlled device starts.
     * <p>
     * Forwards the state to all controller's component controllers. <br/>
     * Subclasses may override this method to perform further processing, but <strong>MUST</strong> call super in that
     * case.
     */
    @CallSuper
    void onProtocolDisconnecting() {
        for (DeviceComponentController<?, ?> controller : mComponentControllers) {
            controller.onDisconnecting();
        }
    }

    /**
     * Called when the controlled device is disconnected.
     * <p>
     * Forwards the state to all controller's component controllers. <br/>
     * Subclasses may override this method to perform further processing, but <strong>MUST</strong> call super in that
     * case.
     */
    @CallSuper
    void onProtocolDisconnected() {
        for (DeviceComponentController<?, ?> controller : mComponentControllers) {
            controller.onDisconnected();
        }
        if (mBlackBoxSession != null) {
            mBlackBoxSession.close();
        }

        notifyDataSyncConditionsChanged();
    }

    /**
     * Tells whether it is currently acceptable to perform any device data synchronization.
     * <p>
     * Subclasses should override this method to impose further restriction as to when device data synchronization is
     * permitted. <br/>
     * Implementations at any subclass level <strong>MUST</strong> always return {@code false} if
     * {@code super.isDataSyncAllowed} returns false. <br/>
     * By default, this method enforces that the device should be at least {@link ControllerConnectionState#CONNECTED}
     * for data synchronization to be allowed.
     *
     * @return {@code true} if data sync is allowed, otherwise {@code false}
     */
    @CallSuper
    boolean isDataSyncAllowed() {
        return mConnectionState == ControllerConnectionState.CONNECTED;
    }

    /**
     * Notifies that some conditions that control device data synchronization allowance have changed.
     * <p>
     * When appropriate, this method notifies in turn all device component controller that data synchronization may be
     * started or must be stopped.
     */
    @CallSuper
    void notifyDataSyncConditionsChanged() {
        boolean dataSyncAllowed = isDataSyncAllowed();
        if (dataSyncAllowed != mPreviousDataSyncAllowed) {
            if (mActiveProvider != null) {
                mActiveProvider.onDeviceDataSyncConditionChanged(this);
            }
            for (DeviceComponentController<?, ?> controller : mComponentControllers) {
                controller.onDataSyncAllowanceChanged(dataSyncAllowed);
            }
            mPreviousDataSyncAllowed = dataSyncAllowed;
        }
    }

    /**
     * Obtains an instance of the specific GetAllSettings command implementation to be sent to the device.
     *
     * @return the device specific GetAllSettings command
     */
    @NonNull
    protected abstract ArsdkCommand obtainGetAllSettingsCommand();

    /**
     * Obtains an instance of the specific GetAllStates command implementation to be sent to the device.
     *
     * @return the device specific GetAllStates command
     */
    @NonNull
    protected abstract ArsdkCommand obtainGetAllStatesCommand();

    /**
     * Opens a black box recording session for the device.
     *
     * @param blackBoxRecorder black box recorder allowing to create sessions
     * @param providerUid      uid of the recorded device's active provider
     * @param closeListener    listener notified when the session closes
     *
     * @return a session to record device black box data to
     */
    @Nullable
    abstract BlackBoxSession openBlackBoxSession(@NonNull BlackBoxRecorder blackBoxRecorder,
                                                 @Nullable String providerUid,
                                                 @NonNull BlackBoxSession.CloseListener closeListener);

    /**
     * Gets the controller connection state.
     *
     * @return the controller current connection state
     */
    @NonNull
    final ControllerConnectionState getConnectionState() {
        return mConnectionState;
    }

    /**
     * Registers the given component controllers.
     *
     * @param controllers component controllers to register
     */
    final void registerComponentControllers(@NonNull DeviceComponentController<?, ?>... controllers) {
        mComponentControllers.addAll(Arrays.asList(controllers));
        mComponentControllers.removeAll(Collections.<DeviceComponentController<?, ?>>singleton(null));
    }

    /**
     * Handles an AllSettings change notification received from the device.
     */
    final void handleAllSettingsReceived() {
        if (mConnectionState == ControllerConnectionState.GETTING_ALL_SETTINGS) {
            mConnectionState = ControllerConnectionState.GETTING_ALL_STATES;
            sendGetAllStates();
        }
    }

    /**
     * Handles an AllStates change notification received from the device.
     */
    final void handleAllStatesReceived() {
        if (mConnectionState == ControllerConnectionState.GETTING_ALL_STATES) {
            mConnectionState = ControllerConnectionState.CONNECTED;

            clearConnectionTimeout();
            onProtocolConnected();

            mDevice.getDeviceStateCore().updateConnectionState(DeviceState.ConnectionState.CONNECTED)
                   .updatePersisted(true).notifyUpdated();

            // in case board id was not received during connection
            if (mDevice.getBoardId() == null) {
                mDevice.updateBoardId(""); // now we know board id to be unavailable
            }

            // store the device
            mDeviceDict.put(PersistentStore.KEY_DEVICE_NAME, mDevice.getName())
                       .put(PersistentStore.KEY_DEVICE_PRESET_KEY, mPresetDict.getKey())
                       .put(PersistentStore.KEY_DEVICE_MODEL, mDevice.getModel().id())
                       .commit();
        }
    }

    /**
     * Handles an device power off Event received from the device.
     */
    final void handleDevicePowerOff() {
        mAutoReconnect = false;
        disconnectDevice(DeviceState.ConnectionStateCause.USER_REQUESTED);
    }

    /**
     * Stops the controller.
     * <p>
     * <strong>IMPORTANT:</strong> Controller must not be used after this point.
     */
    private void stopSelf() {
        setState(State.STOPPED);
        mStateChangeListener = null;
        mPresetDict.unregisterObserver();
        onStopped();
    }

    /**
     * Sets the controller state. Notifies the registered listener (if any) if the state changes.
     *
     * @param state new controller state
     */
    private void setState(@NonNull State state) {
        if (mState != state) {
            mState = state;
            if (mStateChangeListener != null) {
                mStateChangeListener.onStateChanged(mDevice.getUid(), state);
            }
        }
    }

    /**
     * Sends the current date to the controlled device.
     *
     * @param currentDate current date to send
     */
    abstract void sendDate(@NonNull Date currentDate);

    /**
     * Retrieves a {@link HttpClient}.
     *
     * @param clientType class of {@code HttpClient}
     * @param <H>        type of {@code HttpClient}
     *
     * @return an instance of a {@code HttpClient} of the required type, or {@code null} if no such client exists
     */
    @Nullable
    public <H extends HttpClient> H getHttpClient(@NonNull Class<H> clientType) {
        return mHttpSession == null ? null : mHttpSession.client(clientType);
    }

    /**
     * Asks the managed device to get all its settings.
     * <p>
     * This step is ended when AllSettingsChanged event is received.
     */
    private void sendGetAllSettings() {
        sendCommand(obtainGetAllSettingsCommand());
        postConnectionTimeout();
    }

    /**
     * Asks the managed device to get all its states.
     * <p>
     * This step is ended when AllStatesChanged event is received.
     */
    private void sendGetAllStates() {
        sendCommand(obtainGetAllStatesCommand());
        postConnectionTimeout();
    }

    /**
     * Post an action that will disconnect the device after {@link #TIMEOUT} milliseconds have elapsed or the action is
     * removed using {@link #clearConnectionTimeout()}
     * <p>
     * This clears any previously set connection timeout.
     */
    private void postConnectionTimeout() {
        clearConnectionTimeout();
        Executor.schedule(mDisconnectionSignal, TIMEOUT);
    }

    /**
     * Clears any currently pending connection timeout.
     */
    private void clearConnectionTimeout() {
        Executor.unschedule(mDisconnectionSignal);
    }

    /** Runnable posted by {@link #postConnectionTimeout()} that will disconnect the device when run. */
    private final Runnable mDisconnectionSignal = new Runnable() {

        @Override
        public void run() {
            if (mActiveProvider != null) {
                if (ULog.w(TAG_CTRL)) {
                    ULog.w(TAG_CTRL, "Device connection timed out [uid: " + mDevice.getUid() + "]");
                }
                mActiveProvider.disconnectDevice(DeviceController.this);
            }
        }

        @Override
        public String toString() {
            return "Device connection timeout [uid: " + mDevice.getUid() + "]";
        }
    };

    /** Monitors the device current preset for changes. */
    @SuppressWarnings("FieldCanBeLocal")
    private final PersistentStore.Dictionary.Observer mPresetObserver = new PersistentStore.Dictionary.Observer() {

        @Override
        public void onChange() {
            for (DeviceComponentController controller : mComponentControllers) {
                controller.onPresetChange();
            }
        }
    };

    /** Device delegate. */
    @SuppressWarnings("FieldCanBeLocal")
    private final DeviceCore.Delegate mDeviceDelegate = new DeviceCore.Delegate() {

        @Override
        public boolean forget() {
            if (mConnectionState != ControllerConnectionState.DISCONNECTED) {
                disconnect();
            }

            mDevice.getDeviceStateCore().updatePersisted(false).notifyUpdated();

            for (DeviceComponentController controller : mComponentControllers) {
                controller.onForgetting();
            }

            for (DeviceProvider provider : mDeviceProviders.values()) {
                provider.forgetDevice(DeviceController.this);
            }

            mDeviceDict.clear().commit();

            if (mDeviceProviders.isEmpty()) {
                stopSelf();
            }

            return true;
        }

        @Override
        public boolean connect(@NonNull DeviceConnector connector, @Nullable String password) {
            DeviceProvider provider = mDeviceProviders.get(connector);
            return provider != null && connectDevice(provider, password,
                    DeviceState.ConnectionStateCause.USER_REQUESTED);
        }

        @Override
        public boolean disconnect() {
            mAutoReconnect = false;
            return disconnectDevice(DeviceState.ConnectionStateCause.USER_REQUESTED);
        }
    };

    /**
     * Handles disconnection notification from the device.
     * <p>
     * Note that this method does not publish changes made to the device state. Caller has the responsibility to call
     * {@link DeviceStateCore#notifyUpdated()} when appropriate.
     *
     * @param cause cause of the disconnection
     */
    private void handleDisconnection(@Nullable DeviceState.ConnectionStateCause cause) {
        if (mConnectionState == ControllerConnectionState.CONNECTED) {
            onProtocolDisconnecting();
        }
        if (mConnectionState != ControllerConnectionState.DISCONNECTED) {
            mConnectionState = ControllerConnectionState.DISCONNECTED;
            mBackend = null;
            if (mHttpSession != null) {
                mHttpSession.dispose();
                mHttpSession = null;
            }
            if (mHttpProxy != null) {
                mHttpProxy.close();
                mHttpProxy = null;
            }
            clearConnectionTimeout();
            onProtocolDisconnected();

            if (!mAutoReconnect || mActiveProvider == null || !connectDevice(mActiveProvider, null,
                    DeviceState.ConnectionStateCause.CONNECTION_LOST)) {
                mActiveProvider = null;
                mDevice.getDeviceStateCore().updateConnectionState(DeviceState.ConnectionState.DISCONNECTED, cause)
                       .updateActiveConnector(null);
                setState(State.IDLE);
            }
        }
    }

    /**
     * Called when the list of device providers changes.
     * <p>
     * Updates the associated device's list of connectors and active connector.
     * <p>
     * Note that this method does not publish changes made to the device state. Caller has the responsibility to call
     * {@link DeviceStateCore#notifyUpdated()} when appropriate.
     */
    private void onProvidersChanged() {
        Set<DeviceConnectorCore> connectors = new HashSet<>();
        for (DeviceProvider provider : mDeviceProviders.values()) {
            connectors.add(provider.getConnector());
        }
        getDevice().getDeviceStateCore().updateConnectors(connectors)
                   .updateActiveConnector(mActiveProvider == null ? null : mActiveProvider.getConnector());
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param args   command line arguments to process
     * @param prefix prefix string (usually indent) to prepend to each written dump line
     */
    public void dump(@NonNull PrintWriter writer, Set<String> args, @NonNull String prefix) {
        writer.write(prefix + getClass().getSimpleName() + ":\n");
        writer.write(prefix + "\tDevice: " + mDevice.getUid() + " [model: " + mDevice.getModel() + "]\n");
        writer.write(prefix + "\tState: " + mState + "\n");
        writer.write(prefix + "\tConnection state: " + mConnectionState + "\n");
        writer.write(prefix + "\tDevice dict: " + mDeviceDict.getKey() + "\n");
        writer.write(prefix + "\tPreset dict: " + mPresetDict.getKey() + "\n");
        writer.write(prefix + "\tBackend: " + mBackend + "\n");
        writer.write(prefix + "\tProviders: "
                     + (mDeviceProviders.isEmpty() ? "None" : TextUtils.join(", ", mDeviceProviders.values()))
                     + " [active: " + mActiveProvider + "]\n");
    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [uid: " + mDevice.getUid() + "]";
    }
}
