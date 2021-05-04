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

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngine;
import com.parrot.drone.groundsdk.arsdkengine.DeviceProvider;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.BlackBoxDroneSession;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.BlackBoxRecorder;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.BlackBoxSession;
import com.parrot.drone.groundsdk.arsdkengine.ephemeris.EphemerisStore;
import com.parrot.drone.groundsdk.arsdkengine.ephemeris.EphemerisUploadProtocol;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.ActivablePilotingItfController;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.PilotingCommand;
import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.utility.DroneStore;
import com.parrot.drone.groundsdk.internal.utility.SystemBarometer;
import com.parrot.drone.groundsdk.internal.utility.SystemLocation;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCommon;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureControllerInfo;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.PrintWriter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_CTRL;

/**
 * Device controller for a Drone.
 */
public abstract class DroneController extends DeviceController<DroneCore> {

    /** Activation controller managing piloting interfaces. */
    @NonNull
    final PilotingItfActivationController mActivationController;

    /** Protocol specific ephemeris uploader. */
    @NonNull
    private final EphemerisUploadProtocol mEphemerisUploadProtocol;

    /** {@code true} when the controlled drone is landed or in emergency state. */
    private boolean mLanded;

    /**
     * Constructor.
     *
     * @param engine                    arsdk engine instance
     * @param uid                       controlled device uid
     * @param model                     controlled device model
     * @param name                      controlled device initial name
     * @param pcmdEncoder               piloting command encoder
     * @param defaultPilotingItfFactory factory used to build the default piloting interface
     * @param ephemerisUploadProtocol   protocol specific ephemeris uploader
     */
    DroneController(@NonNull ArsdkEngine engine, @NonNull String uid, @NonNull Drone.Model model,
                    @NonNull String name, @NonNull PilotingCommand.Encoder pcmdEncoder,
                    @NonNull ActivablePilotingItfController.Factory defaultPilotingItfFactory,
                    @NonNull EphemerisUploadProtocol ephemerisUploadProtocol) {
        super(engine, delegate -> new DroneCore(uid, model, name, delegate),
                pcmdEncoder.getPilotingCommandLoopPeriod());
        mActivationController = new PilotingItfActivationController(this, pcmdEncoder, defaultPilotingItfFactory);
        mEphemerisUploadProtocol = ephemerisUploadProtocol;
        mLanded = true;
    }

    @Override
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureCommon.SettingsState.UID) {
            ArsdkFeatureCommon.SettingsState.decode(command, mSettingsStateCallback);
        } else if (featureId == ArsdkFeatureCommon.CommonState.UID) {
            ArsdkFeatureCommon.CommonState.decode(command, mCommonStateCallback);
        } else if (featureId == ArsdkFeatureCommon.NetworkEvent.UID) {
            ArsdkFeatureCommon.NetworkEvent.decode(command, mNetworkEventCallback);
        }
        super.onCommandReceived(command);
    }

    /**
     * Requests a video stream to be opened from the controlled drone.
     *
     * @param url    video stream URL
     * @param track  stream track to select, {@code null} to select default track, if any
     * @param client client notified of video stream events
     *
     * @return a new, opening video stream instance
     */
    @Nullable
    public SdkCoreStream openVideoStream(@NonNull String url, @Nullable String track,
                                         @NonNull SdkCoreStream.Client client) {
        Backend backend = getProtocolBackend();
        return backend == null ? null : backend.openVideoStream(url, track, client);
    }

    @Nullable
    @Override
    public final BlackBoxDroneSession getBlackBoxSession() {
        return (BlackBoxDroneSession) super.getBlackBoxSession();
    }

    @NonNull
    @Override
    protected final ArsdkCommand obtainGetAllSettingsCommand() {
        return ArsdkFeatureCommon.Settings.encodeAllSettings();
    }

    @NonNull
    @Override
    protected final ArsdkCommand obtainGetAllStatesCommand() {
        return ArsdkFeatureCommon.Common.encodeAllStates();
    }

    @Override
    void onProtocolConnected() {
        mActivationController.onConnected();

        SystemLocation location = getEngine().getUtility(SystemLocation.class);
        if (location != null) {
            location.monitorWith(mLocationMonitor);
        }

        SystemBarometer barometer = getEngine().getUtility(SystemBarometer.class);
        if (barometer != null) {
            barometer.monitorWith(mBarometerMonitor);
        }

        if (isDataSyncAllowed()) {
            uploadEphemeris();
        }

        super.onProtocolConnected();
    }

    @Override
    void onProtocolDisconnected() {
        mLanded = true;

        SystemLocation location = getEngine().getUtility(SystemLocation.class);
        if (location != null) {
            location.disposeMonitor(mLocationMonitor);
            location.revokeWifiUsageDenial(this);
        }

        SystemBarometer barometer = getEngine().getUtility(SystemBarometer.class);
        if (barometer != null) {
            barometer.disposeMonitor(mBarometerMonitor);
        }

        mActivationController.onDisconnected();
        // activation controller must be notified of disconnection before all piloting interfaces are notified
        super.onProtocolDisconnected();
    }

    @Nullable
    @Override
    final BlackBoxDroneSession openBlackBoxSession(@NonNull BlackBoxRecorder blackBoxRecorder,
                                                   @Nullable String providerUid,
                                                   @NonNull BlackBoxSession.CloseListener closeListener) {
        return blackBoxRecorder.openDroneSession(getDevice(), providerUid, closeListener);
    }

    @Override
    final void onStarted() {
        getEngine().getUtilityOrThrow(DroneStore.class).add(getDevice());
    }

    @Override
    final void onStopped() {
        getEngine().getUtilityOrThrow(DroneStore.class).remove(getUid());
        super.onStopped();
    }

    @Override
    boolean isDataSyncAllowed() {
        return super.isDataSyncAllowed() && mLanded;
    }

    /**
     * Called back when the current piloting command sent to the drone changes.
     *
     * @param pilotingCommand up-to-date piloting command
     */
    void onPilotingCommandChanged(@NonNull PilotingCommand pilotingCommand) {
        BlackBoxDroneSession blackBoxSession = getBlackBoxSession();
        if (blackBoxSession != null) {
            blackBoxSession.onPilotingCommandChanged(pilotingCommand);
        }
    }

    /**
     * Updates current controlled drone landing state.
     *
     * @param landed {@code true} if the drone is landed or in emergency state, otherwise {@code false}
     */
    void updateLandedState(boolean landed) {
        if (mLanded != landed) {
            mLanded = landed;
            notifyDataSyncConditionsChanged();
            if (mLanded) {
                // When the drone is landed, video streaming is not critical, so we can use fused location
                SystemLocation location = getEngine().getUtility(SystemLocation.class);
                if (location != null) {
                    location.revokeWifiUsageDenial(this);
                }
            } else if (doesLocalConnectionUseWifi()) {
                // When the drone is flying, we don't want to interfere with video streaming, so we force GPS location
                // if the phone is connected to the drone or the remote control via wifi
                SystemLocation location = getEngine().getUtility(SystemLocation.class);
                if (location != null) {
                    location.denyWifiUsage(this);
                }
            }
        }
    }

    /**
     * Checks if the local connection uses wifi.
     *
     * @return {@code true} if the local connection uses wifi.
     */
    private boolean doesLocalConnectionUseWifi() {
        DeviceProvider provider = getActiveProvider();
        while (provider != null && provider.getConnector().getType() != DeviceConnector.Type.LOCAL) {
            provider = provider.getParent();
        }
        return provider != null && provider.getConnector().getTechnology() == DeviceConnector.Technology.WIFI;
    }

    /**
     * Uploads on the drone a GPS ephemeris file if such file is available.
     */
    private void uploadEphemeris() {
        EphemerisStore store = getEngine().getEphemerisStore();
        if (store != null) {
            File ephemeris = store.getEphemeris(getDevice().getModel());
            if (ephemeris != null) {
                mEphemerisUploadProtocol.upload(this, ephemeris, success -> {
                    if (ULog.d(TAG_CTRL)) {
                        ULog.d(TAG_CTRL, "GPS ephemeris file upload, success: " + success);
                    }
                });
            }
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureCommon.SettingsState is decoded. */
    private final ArsdkFeatureCommon.SettingsState.Callback mSettingsStateCallback =
            new ArsdkFeatureCommon.SettingsState.Callback() {

                @Override
                public void onProductNameChanged(String name) {
                    getDevice().updateName(name);
                    getDeviceDict().put(PersistentStore.KEY_DEVICE_NAME, name).commit();
                }

                @Override
                public void onProductVersionChanged(String software, String hardware) {
                    FirmwareVersion version = FirmwareVersion.parse(software);
                    if (version != null) {
                        getDevice().updateFirmwareVersion(version);
                        getDeviceDict().put(PersistentStore.KEY_DEVICE_FIRMWARE_VERSION, software).commit();
                    }
                }

                @Override
                public void onBoardIdChanged(@NonNull String id) {
                    getDevice().updateBoardId(id);
                    getDeviceDict().put(PersistentStore.KEY_DEVICE_BOARD_ID, id).commit();
                }

                @Override
                public void onAllSettingsChanged() {
                    handleAllSettingsReceived();
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureCommon.CommonState is decoded. */
    private final ArsdkFeatureCommon.CommonState.Callback mCommonStateCallback =
            new ArsdkFeatureCommon.CommonState.Callback() {

                @Override
                public void onAllStatesChanged() {
                    handleAllStatesReceived();
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureCommon.NetworkEvent is decoded. */
    private final ArsdkFeatureCommon.NetworkEvent.Callback mNetworkEventCallback =
            new ArsdkFeatureCommon.NetworkEvent.Callback() {

                @Override
                public void onDisconnection(@Nullable ArsdkFeatureCommon.NetworkeventDisconnectionCause cause) {
                    if (cause == ArsdkFeatureCommon.NetworkeventDisconnectionCause.OFF_BUTTON) {
                        handleDevicePowerOff();
                    }
                }
            };

    /** Processes system atmospheric pressure measurements and sends them to the drone. */
    private final SystemBarometer.Monitor mBarometerMonitor = (pressure, measureTimeStamp) -> {
        sendCommand(ArsdkFeatureControllerInfo.encodeBarometer((float) pressure,
                TimeUnit.NANOSECONDS.toMillis(measureTimeStamp)));
    };

    /** Processes system geographic location changes and sends them to the drone. */
    private final SystemLocation.Monitor mLocationMonitor = location -> {
        double northSpeed = 0, eastSpeed = 0;
        if (location.hasSpeed() && location.hasBearing()) {
            double speed = location.getSpeed(), bearing = Math.toRadians(location.getBearing());
            northSpeed = Math.cos(bearing) * speed;
            eastSpeed = Math.sin(bearing) * speed;
        }
        sendCommand(ArsdkFeatureControllerInfo.encodeGps(location.getLatitude(), location.getLongitude(),
                (float) location.getAltitude(), location.getAccuracy(), -1, (float) northSpeed,
                (float) eastSpeed, 0, TimeUnit.NANOSECONDS.toMillis(location.getElapsedRealtimeNanos())));
    };

    @Override
    public void dump(@NonNull PrintWriter writer, @NonNull Set<String> args, @NonNull String prefix) {
        super.dump(writer, args, prefix);
        mActivationController.dump(writer, prefix + "\t");
    }
}
