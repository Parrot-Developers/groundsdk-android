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
import com.parrot.drone.groundsdk.arsdkengine.blackbox.BlackBoxRcSession;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.BlackBoxRecorder;
import com.parrot.drone.groundsdk.arsdkengine.blackbox.BlackBoxSession;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.device.DeviceModel;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.utility.RemoteControlStore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSkyctrl;
import com.parrot.drone.sdkcore.arsdk.blackbox.ArsdkBlackBoxRequest;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.arsdk.device.ArsdkRequest;

/**
 * Base class for a RC (Remote Control) controller.
 */
public abstract class RCController extends ProxyDeviceController<RemoteControlCore> {

    /** Drone manager feature implementation. */
    @NonNull
    private final DroneManagerFeature mDroneManagerFeature;

    /** Listens to RC black box info and forwards them the recording session. */
    @Nullable
    private BlackBoxListener mBlackBoxListener;

    /**
     * Constructor.
     *
     * @param engine arsdk engine instance
     * @param uid    controlled device uid
     * @param model  controlled device model
     * @param name   controlled device initial name
     */
    RCController(@NonNull ArsdkEngine engine, @NonNull String uid, @NonNull RemoteControl.Model model,
                 @NonNull String name) {
        super(engine, delegate -> new RemoteControlCore(uid, model, name, delegate));
        mDroneManagerFeature = new DroneManagerFeature(mArsdkProxy);
    }

    @Override
    final void onStarted() {
        getEngine().getUtilityOrThrow(RemoteControlStore.class).add(getDevice());
    }

    @Override
    final void onStopped() {
        getEngine().getUtilityOrThrow(RemoteControlStore.class).remove(getUid());
        super.onStopped();
    }

    @Override
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureSkyctrl.SettingsState.UID) {
            ArsdkFeatureSkyctrl.SettingsState.decode(command, mSettingsStateCallback);
        } else if (featureId == ArsdkFeatureSkyctrl.CommonState.UID) {
            ArsdkFeatureSkyctrl.CommonState.decode(command, mCommonStateCallback);
        } else if (featureId == ArsdkFeatureSkyctrl.CommonEventState.UID) {
            ArsdkFeatureSkyctrl.CommonEventState.decode(command, mCommonEventStateCallback);
        }
        mDroneManagerFeature.onCommandReceived(command);
        super.onCommandReceived(command);
    }

    @Override
    protected void onProtocolConnecting() {
        super.onProtocolConnecting();
        BlackBoxRcSession blackBoxSession = getBlackBoxSession();
        if (blackBoxSession != null) {
            Backend backend = getProtocolBackend();
            assert backend != null;
            mBlackBoxListener = new BlackBoxListener(backend, blackBoxSession);
        }
    }

    @Override
    protected void onProtocolDisconnected() {
        if (mBlackBoxListener != null) {
            mBlackBoxListener.close();
            mBlackBoxListener = null;
        }
        super.onProtocolDisconnected();
    }

    @Override
    protected void onProtocolDisconnecting() {
        mDroneManagerFeature.onProtocolDisconnecting();
        super.onProtocolDisconnecting();
    }

    @Nullable
    @Override
    final BlackBoxRcSession openBlackBoxSession(@NonNull BlackBoxRecorder blackBoxRecorder,
                                                @Nullable String providerUid,
                                                @NonNull BlackBoxSession.CloseListener closeListener) {
        return blackBoxRecorder.openRemoteControlSession(getDevice(), closeListener);
    }

    @Nullable
    @Override
    final BlackBoxRcSession getBlackBoxSession() {
        return (BlackBoxRcSession) super.getBlackBoxSession();
    }

    @Override
    public boolean connectRemoteDevice(@NonNull String deviceUid, @Nullable String password) {
        return mDroneManagerFeature.connectDrone(deviceUid, password);
    }

    @Override
    public boolean connectDiscoveredDevice(@NonNull String deviceUid, @NonNull DeviceModel model, @NonNull String name,
                                           @Nullable String password) {
        return mArsdkProxy.connectRemoteDevice(deviceUid, model, name, password);
    }

    @Override
    public void forgetRemoteDevice(@NonNull String deviceUid) {
        mDroneManagerFeature.forgetDrone(deviceUid);
    }

    @NonNull
    @Override
    protected ArsdkCommand obtainGetAllSettingsCommand() {
        return ArsdkFeatureSkyctrl.Settings.encodeAllSettings();
    }

    @NonNull
    @Override
    protected ArsdkCommand obtainGetAllStatesCommand() {
        return ArsdkFeatureSkyctrl.Common.encodeAllStates();
    }

    /** Called back when a command of the feature ArsdkFeatureSkyctrl.SettingsState is decoded. */
    private final ArsdkFeatureSkyctrl.SettingsState.Callback mSettingsStateCallback =
            new ArsdkFeatureSkyctrl.SettingsState.Callback() {

                @Override
                public void onAllSettingsChanged() {
                    handleAllSettingsReceived();
                }

                @Override
                public void onProductVersionChanged(String software, String hardware) {
                    FirmwareVersion version = FirmwareVersion.parse(software);
                    if (version != null) {
                        getDevice().updateFirmwareVersion(version);
                        getDeviceDict().put(PersistentStore.KEY_DEVICE_FIRMWARE_VERSION, software).commit();
                    }
                }
            };

    /** Called back when a command of the feature ArsdkFeatureSkyctrl.CommonState is decoded. */
    private final ArsdkFeatureSkyctrl.CommonState.Callback mCommonStateCallback =
            new ArsdkFeatureSkyctrl.CommonState.Callback() {

                @Override
                public void onAllStatesChanged() {
                    handleAllStatesReceived();
                }
            };

    /** Implementation callback of ArsdkFeatureSkyctrl.CommonEventState. */
    private final ArsdkFeatureSkyctrl.CommonEventState.Callback mCommonEventStateCallback =
            new ArsdkFeatureSkyctrl.CommonEventState.Callback() {

                @Override
                public void onShutdown(@Nullable ArsdkFeatureSkyctrl.CommoneventstateShutdownReason reason) {
                    if (reason == ArsdkFeatureSkyctrl.CommoneventstateShutdownReason.POWEROFF_BUTTON) {
                        handleDevicePowerOff();
                    }
                }
            };

    /**
     * Subscribes to and forwards black box info to the current recording session.
     */
    private static final class BlackBoxListener {

        /** Black box recording session. */
        @NonNull
        private final BlackBoxRcSession mSession;

        /** Black box info subscription. */
        @NonNull
        private final ArsdkRequest mSubscription;

        /**
         * Constructor.
         *
         * @param backend device controller backend
         * @param session black box session
         */
        private BlackBoxListener(@NonNull Backend backend, @NonNull BlackBoxRcSession session) {
            mSession = session;
            mSubscription = backend.subscribeToBlackBox(new ArsdkBlackBoxRequest.Listener() {

                @Override
                public void onRemoteControlButtonAction(int action) {
                    mSession.onButtonAction(action);
                }

                @Override
                public void onRemoteControlPilotingInfo(int roll, int pitch, int yaw, int gaz, int source) {
                    mSession.onPilotingInfo(roll, pitch, yaw, gaz, source);
                }
            });
        }

        /**
         * Unsubscribes from black box info.
         */
        private void close() {
            mSubscription.cancel();
        }
    }
}
