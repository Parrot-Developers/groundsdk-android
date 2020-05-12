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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.camera;

import android.util.SparseArray;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.camera.Camera;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraEvCompensation;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposure;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposureLock;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraRecording;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraStyle;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalance;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraZoom;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraPhotoSettingCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraRecordingSettingCore;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdk.value.IntegerRange;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkListFlags;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkNoAckCmdEncoder;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.function.Consumer;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_CAMERA;

/** Camera peripheral(s) controller for Anafi family drones. */
public final class AnafiCameraRouter extends DronePeripheralController {

    /** Key prefix used to access preset and range dictionaries for cameras settings. */
    private static final String SETTINGS_KEY_PREFIX = "camera";

    /** Main camera identifier. */
    private static final int CAMERA_ID_MAIN = 0;

    /** Camera model setting. */
    private static final StorageEntry<Model> MODEL_SETTING = StorageEntry.ofEnum("model", Model.class);

    /** Known camera controllers, by camera identifier. */
    @NonNull
    private final SparseArray<CameraControllerBase> mCameraControllers;

    /** {@code true} when camera states were received during connection. */
    private boolean mCameraStatesReceived;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiCameraRouter(@NonNull DroneController droneController) {
        super(droneController);
        mCameraControllers = new SparseArray<>();

        if (offlineSettingsEnabled()) {
            // load persisted camera controllers
            for (String key : mDeviceController.getDeviceDict().keys()) {
                if (!key.startsWith(SETTINGS_KEY_PREFIX)) {
                    continue;
                }

                String suffix = key.substring(SETTINGS_KEY_PREFIX.length());
                boolean legacySettings = suffix.isEmpty();

                int id;
                if (legacySettings) {
                    id = CAMERA_ID_MAIN; // legacy settings did not have any id, so fallback to main camera
                } else try {
                    id = Integer.parseInt(suffix);
                } catch (NumberFormatException e) {
                    if (ULog.e(TAG_CAMERA)) {
                        ULog.e(TAG_CAMERA, "Could not parse camera settings key: " + key);
                    }
                    continue;
                }

                PersistentStore.Dictionary settingsDict = mDeviceController.getDeviceDict().getDictionary(key);
                Model model;
                try {
                    model = MODEL_SETTING.loadOrThrow(settingsDict, () -> {
                        if (legacySettings) {
                            return Model.MAIN; // legacy setting did not have any model, so fallback to main camera
                        } else throw new IllegalArgumentException("Missing required camera model");
                    });
                } catch (IllegalArgumentException e) {
                    if (ULog.e(TAG_CAMERA)) {
                        ULog.e(TAG_CAMERA, "Could not load stored camera settings [id: " + id + "]", e);
                    }
                    continue;
                }

                mCameraControllers.put(id, new CameraController(new CameraInfo(id, model, key)));
            }
        }
    }

    // region Lifecycle { ... }

    @Override
    protected void onConnecting() {
        mCameraStatesReceived = false;
    }

    @Override
    protected void onConnected() {
        if (!mCameraStatesReceived) {
            CameraControllerBase controller = mCameraControllers.get(CAMERA_ID_MAIN);
            if (controller != null) {
                ULog.w(TAG_CAMERA, "Cameras state not received, assuming main camera is active");
                controller.onActivationState(true);
            }
        }
        forEachCameraController(CameraControllerBase::onConnected);
    }

    @Override
    protected void onDisconnected() {
        forEachCameraController(CameraControllerBase::onDisconnected);
    }

    @Override
    protected void onForgetting() {
        forEachCameraController(CameraControllerBase::onForgetting);
    }

    @Override
    protected void onPresetChange() {
        forEachCameraController(CameraControllerBase::onPresetChange);
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureCamera.UID) {
            ArsdkFeatureCamera.decode(command, mCameraCallbacks);
        }
    }

    // endregion

    /**
     * Dispatches some method to all known camera controllers.
     *
     * @param method method to apply to all known camera controllers
     */
    private void forEachCameraController(@NonNull Consumer<CameraControllerBase> method) {
        for (int i = 0, N = mCameraControllers.size(); i < N; i++) {
            CameraControllerBase controller = mCameraControllers.valueAt(i);
            method.accept(controller);
        }
    }

    /**
     * Aggregates static information on a camera, such as its unique identifier and model.
     */
    final class CameraInfo {

        /** Camera unique identifier. */
        final int mId;

        /** Camera model. */
        @NonNull
        final Model mModel;

        /** Key to this camera persisted settings and presets. */
        @NonNull
        private final String mSettingsKey;

        /** Links to the router. */
        @NonNull
        private final AnafiCameraRouter mRouter;

        /**
         * Constructor.
         *
         * @param id          camera identifier
         * @param model       camera model
         * @param settingsKey camera settings/presets key
         */
        private CameraInfo(int id, @NonNull Model model, @NonNull String settingsKey) {
            mModel = model;
            mId = id;
            mSettingsKey = settingsKey;
            mRouter = AnafiCameraRouter.this;
        }
    }

    /**
     * Abstract base for camera controllers.
     * <p>
     * This class provides all command primitives and defines camera event callbacks.
     */
    abstract static class CameraControllerBase {

        /** Camera info. */
        @NonNull
        final CameraInfo mInfo;

        /** Peripheral store where the camera component may be published. */
        @NonNull
        final ComponentStore<Peripheral> mPeripheralStore;

        /** Dictionary containing device specific values for the camera, such as settings ranges, supported status. */
        @Nullable
        final PersistentStore.Dictionary mSettingsDict;

        /**
         * Constructor.
         *
         * @param info camera info
         */
        CameraControllerBase(@NonNull CameraInfo info) {
            mInfo = info;
            mPeripheralStore = mInfo.mRouter.mComponentStore;
            mSettingsDict = offlineSettingsEnabled() ?
                    mInfo.mRouter.mDeviceController.getDeviceDict().getDictionary(mInfo.mSettingsKey) : null;
        }

        /**
         * Loads up-to-date presets for this camera.
         *
         * @return camera presets dictionary, if presets persistence is enabled, otherwise {@code null}
         */
        @Nullable
        final PersistentStore.Dictionary loadPresets() {
            return offlineSettingsEnabled() ?
                    mInfo.mRouter.mDeviceController.getPresetDict().getDictionary(mInfo.mSettingsKey) : null;
        }

        /**
         * Registers an encoder to be executed in the non-acknowledged command loop.
         *
         * @param encoder encoder to register
         */
        final void registerNoAckCmdEncoder(@NonNull ArsdkNoAckCmdEncoder encoder) {
            DeviceController.Backend backend = mInfo.mRouter.mDeviceController.getProtocolBackend();
            if (backend != null) {
                backend.registerNoAckCommandEncoders(encoder);
            }
        }

        /**
         * Unregisters an encoder from being executed in the non-acknowledged command loop.
         *
         * @param encoder encoder to unregister
         */
        final void unregisterNoAckCmdEncoder(@NonNull ArsdkNoAckCmdEncoder encoder) {
            DeviceController.Backend backend = mInfo.mRouter.mDeviceController.getProtocolBackend();
            if (backend != null) {
                backend.unregisterNoAckCommandEncoders(encoder);
            }
        }

        /**
         * Sends selected camera mode to the device.
         *
         * @param mode camera mode to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendCameraMode(@NonNull Camera.Mode mode) {
            return sendCommand(ArsdkFeatureCamera.encodeSetCameraMode(mInfo.mId, CameraModeAdapter.from(mode)));
        }

        /**
         * Sends selected photo mode settings to the device.
         *
         * @param mode            photo mode to send
         * @param format          photo format to send
         * @param fileFormat      photo file format to send
         * @param burst           photo burst value to send
         * @param bracketing      photo bracketing value to send
         * @param captureInterval photo capture interval to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendPhotoSettings(@NonNull CameraPhoto.Mode mode, @NonNull CameraPhoto.Format format,
                                        @NonNull CameraPhoto.FileFormat fileFormat,
                                        @NonNull CameraPhoto.BurstValue burst,
                                        @NonNull CameraPhoto.BracketingValue bracketing, double captureInterval) {
            return sendCommand(ArsdkFeatureCamera.encodeSetPhotoMode(mInfo.mId, PhotoModeAdapter.from(mode),
                    FormatAdapter.from(format), FileFormatAdapter.from(fileFormat), BurstValueAdapter.from(burst),
                    BracketingValueAdapter.from(bracketing), (float) captureInterval));
        }

        /**
         * Sends selected recording mode settings to the device.
         *
         * @param mode       recording mode to send
         * @param resolution recording resolution to send
         * @param framerate  recording framerate to send
         * @param hyperlapse recording hyperlapse value to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendRecordingSettings(@NonNull CameraRecording.Mode mode,
                                            @NonNull CameraRecording.Resolution resolution,
                                            @NonNull CameraRecording.Framerate framerate,
                                            @NonNull CameraRecording.HyperlapseValue hyperlapse) {
            return sendCommand(ArsdkFeatureCamera.encodeSetRecordingMode(mInfo.mId, RecordingModeAdapter.from(mode),
                    ResolutionAdapter.from(resolution), FramerateAdapter.from(framerate),
                    HyperlapseValueAdapter.from(hyperlapse)));
        }

        /**
         * Sends selected exposure settings to the device.
         *
         * @param mode                      exposure mode to send
         * @param manualShutterSpeed        manual shutter speed to send
         * @param manualIsoSensitivity      manual ISO sensitivity to send
         * @param maxIsoSensitivity         maximum ISO sensitivity to send
         * @param autoExposureMeteringMode  auto exposure metering mode to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendExposureSettings(@NonNull CameraExposure.Mode mode,
                                           @NonNull CameraExposure.ShutterSpeed manualShutterSpeed,
                                           @NonNull CameraExposure.IsoSensitivity manualIsoSensitivity,
                                           @NonNull CameraExposure.IsoSensitivity maxIsoSensitivity,
                                           @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
            return sendCommand(ArsdkFeatureCamera.encodeSetExposureSettings(mInfo.mId,
                    ExposureModeAdapter.from(mode), ShutterSpeedAdapter.from(manualShutterSpeed),
                    IsoSensitivityAdapter.from(manualIsoSensitivity),
                    IsoSensitivityAdapter.from(maxIsoSensitivity),
                    AutoExposureMeteringModeAdapter.from(autoExposureMeteringMode)));
        }

        /**
         * Sends selected exposure lock mode to the device.
         *
         * @param mode    exposure lock mode to send
         * @param centerX exposure lock region horizontal center to send
         * @param centerY exposure lock region vertical center to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendExposureLock(@NonNull CameraExposureLock.Mode mode, double centerX, double centerY) {
            switch (mode) {
                case NONE:
                    return sendCommand(ArsdkFeatureCamera.encodeUnlockExposure(mInfo.mId));
                case CURRENT_VALUES:
                    return sendCommand(ArsdkFeatureCamera.encodeLockExposure(mInfo.mId));
                case REGION:
                    return sendCommand(ArsdkFeatureCamera.encodeLockExposureOnRoi(mInfo.mId, (float) centerX,
                            (float) centerY));
            }
            return false;
        }

        /**
         * Sends selected exposure compensation value to the device.
         *
         * @param ev exposure compensation value to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendEvCompensation(@NonNull CameraEvCompensation ev) {
            return sendCommand(ArsdkFeatureCamera.encodeSetEvCompensation(mInfo.mId, EvCompensationAdapter.from(ev)));
        }

        /**
         * Sends selected white balance settings to the device.
         *
         * @param mode        white balance mode to send
         * @param temperature custom temperature to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendWhiteBalance(@NonNull CameraWhiteBalance.Mode mode,
                                       @NonNull CameraWhiteBalance.Temperature temperature) {
            return sendCommand(ArsdkFeatureCamera.encodeSetWhiteBalance(mInfo.mId, WhiteBalanceModeAdapter.from(mode),
                    TemperatureAdapter.from(temperature)));
        }

        /**
         * Sends white balance lock value to the device.
         *
         * @param locked white balance lock value to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendWhiteBalanceLock(boolean locked) {
            return sendCommand(ArsdkFeatureCamera.encodeSetWhiteBalanceLock(mInfo.mId,
                    locked ? ArsdkFeatureCamera.State.ACTIVE : ArsdkFeatureCamera.State.INACTIVE));
        }

        /**
         * Sends image style setting to the device.
         *
         * @param style style to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendStyle(@NonNull CameraStyle.Style style) {
            return sendCommand(ArsdkFeatureCamera.encodeSetStyle(mInfo.mId, StyleAdapter.from(style)));
        }

        /**
         * Sends current image style parameters to the device.
         *
         * @param saturation saturation parameter to send
         * @param contrast   contrast parameter to send
         * @param sharpness  sharpness parameter to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendStyleParameters(int saturation, int contrast, int sharpness) {
            return sendCommand(ArsdkFeatureCamera.encodeSetStyleParams(mInfo.mId, saturation, contrast, sharpness));
        }

        /**
         * Sends alignment on each axis to the device.
         *
         * @param yaw   yaw offset to send
         * @param pitch pitch offset to send
         * @param roll  roll offset to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendAlignment(double yaw, double pitch, double roll) {
            return sendCommand(ArsdkFeatureCamera.encodeSetAlignmentOffsets(mInfo.mId, (float) yaw, (float) pitch,
                    (float) roll));
        }

        /**
         * Sends alignment reset request to the device.
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendAlignmentReset() {
            return sendCommand(ArsdkFeatureCamera.encodeResetAlignmentOffsets(mInfo.mId));
        }

        /**
         * Sends automatic HDR setting to the device.
         *
         * @param enable HDR setting to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendAutoHdr(boolean enable) {
            return sendCommand(ArsdkFeatureCamera.encodeSetHdrSetting(mInfo.mId,
                    enable ? ArsdkFeatureCamera.State.ACTIVE : ArsdkFeatureCamera.State.INACTIVE));
        }

        /**
         * Sends auto-record setting to the device.
         *
         * @param enable auto-record setting to send
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendAutoRecord(boolean enable) {
            return sendCommand(ArsdkFeatureCamera.encodeSetAutorecord(mInfo.mId,
                    enable ? ArsdkFeatureCamera.State.ACTIVE : ArsdkFeatureCamera.State.INACTIVE));
        }

        /**
         * Sends max zoom speed setting to the device.
         *
         * @param speed maximum zoom speed to send, in tan(deg) / sec
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendMaxZoomSpeed(double speed) {
            return sendCommand(ArsdkFeatureCamera.encodeSetMaxZoomSpeed(mInfo.mId, (float) speed));
        }

        /**
         * Sends quality degradation allowance setting to the device.
         *
         * @param allowed {@code true} to allow quality degradation, otherwise {@code false}
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendZoomQualityDegradationAllowance(boolean allowed) {
            return sendCommand(ArsdkFeatureCamera.encodeSetZoomVelocityQualityDegradation(mInfo.mId, allowed ? 1 : 0));
        }

        /**
         * Encodes zoom control parameters into a command to be sent to the drone.
         *
         * @param mode   zoom control mode
         * @param target zoom control target
         *
         * @return specified parameters properly encoded in an {@code ArsdkCommand}
         */
        @NonNull
        final ArsdkCommand encodeZoomControl(@NonNull CameraZoom.ControlMode mode, double target) {
            ArsdkFeatureCamera.ZoomControlMode arsdkMode = null;
            switch (mode) {
                case LEVEL:
                    arsdkMode = ArsdkFeatureCamera.ZoomControlMode.LEVEL;
                    break;
                case VELOCITY:
                    arsdkMode = ArsdkFeatureCamera.ZoomControlMode.VELOCITY;
            }
            return ArsdkFeatureCamera.encodeSetZoomTarget(mInfo.mId, arsdkMode, (float) target);
        }

        /**
         * Requests the device to start taking photo(s).
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendStartPhotoCapture() {
            return sendCommand(ArsdkFeatureCamera.encodeTakePhoto(mInfo.mId));
        }

        /**
         * Requests the device to stop taking photo(s).
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendStopPhotoCapture() {
            return sendCommand(ArsdkFeatureCamera.encodeStopPhoto(mInfo.mId));
        }

        /**
         * Requests the device to start recording a video.
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendStartRecording() {
            return sendCommand(ArsdkFeatureCamera.encodeStartRecording(mInfo.mId));
        }

        /**
         * Requests the device to stop recording any video.
         *
         * @return {@code true} if any command was sent to the device, otherwise false
         */
        final boolean sendStopRecording() {
            return sendCommand(ArsdkFeatureCamera.encodeStopRecording(mInfo.mId));
        }

        /**
         * Notifies successful protocol connection with the drone.
         */
        abstract void onConnected();

        /**
         * Notifies camera activation state change.
         *
         * @param active {@code true} if the camera is now active, otherwise {@code false}
         */
        abstract void onActivationState(boolean active);

        /**
         * Notifies disconnection from the drone.
         */
        abstract void onDisconnected();

        /**
         * Notifies that camera settings must be cleared.
         */
        abstract void onForgetting();

        /**
         * Notifies camera presets change.
         */
        abstract void onPresetChange();

        /**
         * Notifies reception of camera capabilities.
         *
         * @param modes                      supported camera modes
         * @param exposureCompensationValues supported exposure compensation values
         * @param exposureModes              supported exposure modes
         * @param autoExposureMeteringModes  supported auto exposure metering modes
         * @param exposureLock               {@code true} if current-values exposure lock is supported
         * @param exposureRoiLock            {@code true} if region exposure lock is supported
         * @param whiteBalanceModes          supported white balance modes
         * @param whiteBalanceTemperatures   supported white balance temperatures
         * @param whiteBalanceLock           {@code true} if white balance lock is supported
         * @param styles                     supported styles
         * @param hyperlapseValues           supported hyperlapse values
         * @param burstValues                supported burst values
         * @param bracketingValues           supported bracketing values
         * @param timeLapseIntervalRange     supported time lapse interval range
         * @param gpsLapseIntervalRange      supported GPS lapse interval range
         */
        abstract void onCameraCapabilities(@NonNull EnumSet<Camera.Mode> modes,
                                           @NonNull EnumSet<CameraEvCompensation> exposureCompensationValues,
                                           @NonNull EnumSet<CameraExposure.Mode> exposureModes,
                                           @NonNull EnumSet<CameraExposure.AutoExposureMeteringMode>
                                                   autoExposureMeteringModes,
                                           boolean exposureLock, boolean exposureRoiLock,
                                           @NonNull EnumSet<CameraWhiteBalance.Mode> whiteBalanceModes,
                                           @NonNull EnumSet<CameraWhiteBalance.Temperature> whiteBalanceTemperatures,
                                           boolean whiteBalanceLock, @NonNull EnumSet<CameraStyle.Style> styles,
                                           @NonNull EnumSet<CameraRecording.HyperlapseValue> hyperlapseValues,
                                           @NonNull EnumSet<CameraPhoto.BurstValue> burstValues,
                                           @NonNull EnumSet<CameraPhoto.BracketingValue> bracketingValues,
                                           @NonNull DoubleRange timeLapseIntervalRange,
                                           @NonNull DoubleRange gpsLapseIntervalRange);

        /**
         * Notifies reception of photo mode capabilities.
         *
         * @param capabilities photo mode capabilities
         */
        abstract void onPhotoCapabilities(@NonNull Collection<CameraPhotoSettingCore.Capability> capabilities);

        /**
         * Notifies reception of recording mode capabilities.
         *
         * @param capabilities recording mode capabilities
         */
        abstract void onRecordingCapabilities(@NonNull Collection<CameraRecordingSettingCore.Capability> capabilities);


        /**
         * Notifies reception of camera mode.
         *
         * @param mode camera mode
         */
        abstract void onCameraMode(@NonNull Camera.Mode mode);

        /**
         * Notifies reception of photo mode and settings.
         *
         * @param mode            photo mode
         * @param format          photo format
         * @param fileFormat      photo file format
         * @param burst           photo burst value
         * @param bracketing      photo bracketing value
         * @param captureInterval photo capture interval
         */
        abstract void onPhotoMode(@NonNull CameraPhoto.Mode mode, @NonNull CameraPhoto.Format format,
                                  @NonNull CameraPhoto.FileFormat fileFormat, @NonNull CameraPhoto.BurstValue burst,
                                  @NonNull CameraPhoto.BracketingValue bracketing, double captureInterval);

        /**
         * Notifies reception of recording mode and settings.
         *
         * @param mode       recording mode
         * @param resolution recording resolution
         * @param framerate  recording framerate
         * @param hyperlapse recording hyperlapse value
         * @param bitrate    recording bitrate
         */
        abstract void onRecordingMode(@NonNull CameraRecording.Mode mode,
                                      @NonNull CameraRecording.Resolution resolution,
                                      @NonNull CameraRecording.Framerate framerate,
                                      @NonNull CameraRecording.HyperlapseValue hyperlapse, int bitrate);

        /**
         * Notifies reception of exposure compensation value.
         *
         * @param evCompensation exposure compensation value
         */
        abstract void onEvCompensation(@NonNull CameraEvCompensation evCompensation);

        /**
         * Notifies reception of exposure capabilities and settings.
         *
         * @param supportedShutterSpeeds          supported manual shutter speed values
         * @param supportedManualIsoSensitivities supported manual ISO sensitivity values
         * @param supportedMaxIsoSensitivities    supported maximum ISO sensitivity values
         * @param mode                            exposure mode
         * @param shutterSpeed                    manual shutter speed
         * @param manualIsoSensitivity            manual ISO sensitivity
         * @param maxIsoSensitivity               maximum ISO sensitivity
         * @param autoExposureMeteringMode        auto exposure metering mode
         */
        abstract void onExposureSettings(@NonNull EnumSet<CameraExposure.ShutterSpeed> supportedShutterSpeeds,
                                         @NonNull EnumSet<CameraExposure.IsoSensitivity> supportedManualIsoSensitivities,
                                         @NonNull EnumSet<CameraExposure.IsoSensitivity> supportedMaxIsoSensitivities,
                                         @NonNull CameraExposure.Mode mode,
                                         @NonNull CameraExposure.ShutterSpeed shutterSpeed,
                                         @NonNull CameraExposure.IsoSensitivity manualIsoSensitivity,
                                         @NonNull CameraExposure.IsoSensitivity maxIsoSensitivity,
                                         @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode);

        /**
         * Notifies reception of exposure lock state.
         *
         * @param mode    exposure lock mode
         * @param centerX exposure lock region horizontal center
         * @param centerY exposure lock region vertical center
         * @param width   exposure lock region width
         * @param height  exposure lock region height
         */
        abstract void onExposureLock(@NonNull CameraExposureLock.Mode mode,
                                     @FloatRange(from = 0, to = 1) double centerX,
                                     @FloatRange(from = 0, to = 1) double centerY,
                                     @FloatRange(from = 0, to = 1) double width,
                                     @FloatRange(from = 0, to = 1) double height);

        /**
         * Notifies reception of white balance settings.
         *
         * @param mode        white balance mode
         * @param temperature white balance custom temperature
         * @param locked      white balance lock state
         */
        abstract void onWhiteBalance(@NonNull CameraWhiteBalance.Mode mode,
                                     @NonNull CameraWhiteBalance.Temperature temperature, boolean locked);

        /**
         * Notifies reception of auto record state.
         *
         * @param active {@code true} when auto record is enabled, otherwise {@code false}
         */
        abstract void onAutoRecord(boolean active);

        /**
         * Notifies reception of auto HDR state.
         *
         * @param active {@code true} when auto HDR is enabled, otherwise {@code false}
         */
        abstract void onAutoHdr(boolean active);

        /**
         * Notifies reception of style settings.
         *
         * @param saturationRange supported saturation value range
         * @param contrastRange   supported contrast value range
         * @param sharpnessRange  supported sharpness value range
         * @param style           style
         * @param saturation      saturation value
         * @param contrast        contrast value
         * @param sharpness       sharpness value
         */
        abstract void onStyle(@NonNull IntegerRange saturationRange, @NonNull IntegerRange contrastRange,
                              @NonNull IntegerRange sharpnessRange, @NonNull CameraStyle.Style style,
                              int saturation, int contrast, int sharpness);

        /**
         * Notifies reception of alignment settings.
         *
         * @param yawRange   supported yaw offset range
         * @param pitchRange supported pitch offset range
         * @param rollRange  supported roll offset range
         * @param yaw        yaw offset value
         * @param pitch      pitch offset value
         * @param roll       roll offset value
         */
        abstract void onAlignment(@NonNull DoubleRange yawRange, @NonNull DoubleRange pitchRange,
                                  @NonNull DoubleRange rollRange, double yaw, double pitch, double roll);

        /**
         * Notifies reception of HDR state.
         *
         * @param active {@code true} when HDR is active, otherwise {@code false}
         */
        abstract void onHdr(boolean active);

        /**
         * Notifies reception of maximum zoom speed setting.
         *
         * @param maxSpeedRange supported maximum zoom speed range
         * @param maxSpeed      maximum zoom speed
         */
        abstract void onMaxZoomSpeed(@NonNull DoubleRange maxSpeedRange, double maxSpeed);

        /**
         * Notifies reception of zoom velocity quality degradation allowance state.
         *
         * @param allowed {@code true} when zoom velocity quality degradation is allowed, otherwise {@code false}
         */
        abstract void onZoomVelocityQualityDegradation(boolean allowed);

        /**
         * Notifies reception of zoom level.
         *
         * @param level zoom level
         */
        abstract void onZoomLevel(double level);

        /**
         * Notifies reception of zoom info.
         *
         * @param maxLossyLevel    maximum zoom level
         * @param maxLossLessLevel maximum zoom level without quality degradation
         * @param available        {@code true} when zoom is available, otherwise {@code false}
         */
        abstract void onZoomInfo(double maxLossyLevel, double maxLossLessLevel, boolean available);

        /**
         * Notifies reception of photo capture progress.
         *
         * @param result     photo capture progress state
         * @param photoCount count of photos taken so far, if {@code result == PHOTO_TAKEN}
         * @param mediaId    identifies the produced media item if {@code result == PHOTO_SAVED}, otherwise {@code null}
         */
        abstract void onPhotoProgress(@NonNull ArsdkFeatureCamera.PhotoResult result, int photoCount,
                                      @NonNull String mediaId);

        /**
         * Notifies reception of photo capture feature state.
         *
         * @param available {@code true} if photo capture is available, otherwise {@code false}
         * @param active    {@code true} if photo capture is active, otherwise {@code false}
         */
        abstract void onPhotoState(boolean available, boolean active);

        /**
         * Notifies reception of video recording progress.
         *
         * @param result  video recording progress state
         * @param mediaId identifies the produced media item if {@code result == STOPPED_*}, otherwise {@code null}
         */
        abstract void onRecordingProgress(@NonNull ArsdkFeatureCamera.RecordingResult result, @NonNull String mediaId);

        /**
         * Notifies reception of video recording feature state.
         *
         * @param available      {@code true} if video recording is available, otherwise {@code false}
         * @param active         {@code true} if video recording is active, otherwise {@code false}
         * @param startTimestamp date/time when video recording did start, in milliseconds since epoch, if active,
         *                       otherwise meaningless
         */
        abstract void onRecordingState(boolean available, boolean active, long startTimestamp);

        /**
         * Sends a command to the drone.
         *
         * @param command command to send
         *
         * @return {@code true} if the command was sent, otherwise {@code false}
         */
        private boolean sendCommand(@NonNull ArsdkCommand command) {
            return mInfo.mRouter.sendCommand(command);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureCamera is decoded. */
    private final ArsdkFeatureCamera.Callback mCameraCallbacks = new ArsdkFeatureCamera.Callback() {

        /** Collects received photo mode capabilities. */
        private final SparseArray<Collection<CameraPhotoSettingCore.Capability>> mPhotoCaps;

        /** Collects received recording mode capabilities. */
        private final SparseArray<Collection<CameraRecordingSettingCore.Capability>> mRecordingCaps;

        {
            mPhotoCaps = new SparseArray<>();
            mRecordingCaps = new SparseArray<>();
        }

        @Override
        public void onCameraCapabilities(int camId, @Nullable ArsdkFeatureCamera.Model camModel,
                                         int exposureModesBitField,
                                         @Nullable ArsdkFeatureCamera.Supported exposureLockSupported,
                                         @Nullable ArsdkFeatureCamera.Supported exposureRoiLockSupported,
                                         long evCompensationsBitField, int whiteBalanceModesBitField,
                                         long customWhiteBalanceTemperaturesBitField,
                                         @Nullable ArsdkFeatureCamera.Supported whiteBalanceLockSupported,
                                         int stylesBitField, int cameraModesBitField, int hyperlapseValuesBitField,
                                         int bracketingPresetsBitField, int burstValuesBitField,
                                         int streamingModesBitField, float timelapseIntervalMin,
                                         float gpslapseIntervalMin, int autoExposureMeteringModesBitField) {
            if (camModel == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid camera model");
            }


            CameraControllerBase controller = mCameraControllers.get(camId);
            if (controller == null) {
                String settingsKey = SETTINGS_KEY_PREFIX + camId;
                Model model = Model.from(camModel);
                // new drone-announced camera, create a controller for it
                controller = new CameraController(new CameraInfo(camId, model, settingsKey));
                mCameraControllers.put(camId, controller);
                MODEL_SETTING.save(controller.mSettingsDict, model);
            }

            controller.onCameraCapabilities(
                    CameraModeAdapter.from(cameraModesBitField),
                    EvCompensationAdapter.from((int) evCompensationsBitField),
                    ExposureModeAdapter.from(exposureModesBitField),
                    AutoExposureMeteringModeAdapter.from(autoExposureMeteringModesBitField),
                    exposureLockSupported == ArsdkFeatureCamera.Supported.SUPPORTED,
                    exposureRoiLockSupported == ArsdkFeatureCamera.Supported.SUPPORTED,
                    WhiteBalanceModeAdapter.from(whiteBalanceModesBitField),
                    TemperatureAdapter.from(customWhiteBalanceTemperaturesBitField),
                    whiteBalanceLockSupported == ArsdkFeatureCamera.Supported.SUPPORTED,
                    StyleAdapter.from(stylesBitField),
                    HyperlapseValueAdapter.from(hyperlapseValuesBitField),
                    BurstValueAdapter.from(burstValuesBitField),
                    BracketingValueAdapter.from(bracketingPresetsBitField),
                    DoubleRange.of(timelapseIntervalMin, Double.MAX_VALUE),
                    DoubleRange.of(gpslapseIntervalMin, Double.MAX_VALUE));
        }


        @Override
        public void onPhotoCapabilities(int id, int photoModesBitField, int photoFormatsBitField,
                                        int photoFileFormatsBitField, @Nullable ArsdkFeatureCamera.Supported hdr,
                                        int listFlagsBitField) {
            int camId = id >>> 8;

            ArsdkListFlags.process(listFlagsBitField,
                    () -> mPhotoCaps.put(camId, new ArrayList<>()),
                    () -> mPhotoCaps.get(camId).add(CameraPhotoSettingCore.Capability.of(
                            PhotoModeAdapter.from(photoModesBitField), FormatAdapter.from(photoFormatsBitField),
                            FileFormatAdapter.from(photoFileFormatsBitField),
                            hdr == ArsdkFeatureCamera.Supported.SUPPORTED)),
                    () -> {
                        controllerFor(camId).onPhotoCapabilities(mPhotoCaps.get(camId));
                        mPhotoCaps.remove(camId);
                    });
        }

        @Override
        public void onRecordingCapabilities(int id, int recordingModesBitField, int resolutionsBitField,
                                            int frameratesBitField, @Nullable ArsdkFeatureCamera.Supported hdr,
                                            int listFlagsBitField) {
            int camId = id >>> 8;

            ArsdkListFlags.process(listFlagsBitField,
                    () -> mRecordingCaps.put(camId, new ArrayList<>()),
                    () -> mRecordingCaps.get(camId).add(CameraRecordingSettingCore.Capability.of(
                            RecordingModeAdapter.from(recordingModesBitField),
                            ResolutionAdapter.from(resolutionsBitField),
                            FramerateAdapter.from(frameratesBitField),
                            hdr == ArsdkFeatureCamera.Supported.SUPPORTED)),
                    () -> {
                        controllerFor(camId).onRecordingCapabilities(mRecordingCaps.get(camId));
                        mRecordingCaps.remove(camId);
                    });
        }

        @Override
        public void onCameraStates(long activeCameras) {
            mCameraStatesReceived = true;
            for (int i = 0, N = mCameraControllers.size(); i < N; i++) {
                int camId = mCameraControllers.keyAt(i);
                mCameraControllers.get(camId).onActivationState((activeCameras & (1L << camId)) != 0);
            }
        }

        @Override
        public void onCameraMode(int camId, @Nullable ArsdkFeatureCamera.CameraMode mode) {
            if (mode == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid camera mode");
            }

            controllerFor(camId).onCameraMode(CameraModeAdapter.from(mode));
        }

        @Override
        public void onPhotoMode(int camId, @Nullable ArsdkFeatureCamera.PhotoMode mode,
                                @Nullable ArsdkFeatureCamera.PhotoFormat format,
                                @Nullable ArsdkFeatureCamera.PhotoFileFormat fileFormat,
                                @Nullable ArsdkFeatureCamera.BurstValue burst,
                                @Nullable ArsdkFeatureCamera.BracketingPreset bracketing, float captureInterval) {
            if (mode == null || format == null || fileFormat == null || burst == null || bracketing == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid photo settings");
            }

            controllerFor(camId).onPhotoMode(PhotoModeAdapter.from(mode), FormatAdapter.from(format),
                    FileFormatAdapter.from(fileFormat), BurstValueAdapter.from(burst),
                    BracketingValueAdapter.from(bracketing), captureInterval);
        }

        @Override
        public void onRecordingMode(int camId, @Nullable ArsdkFeatureCamera.RecordingMode mode,
                                    @Nullable ArsdkFeatureCamera.Resolution resolution,
                                    @Nullable ArsdkFeatureCamera.Framerate framerate,
                                    @Nullable ArsdkFeatureCamera.HyperlapseValue hyperlapse, long bitrate) {
            if (mode == null || resolution == null || framerate == null || hyperlapse == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid recording settings");
            }

            controllerFor(camId).onRecordingMode(RecordingModeAdapter.from(mode),
                    ResolutionAdapter.from(resolution), FramerateAdapter.from(framerate),
                    HyperlapseValueAdapter.from(hyperlapse), (int) bitrate);
        }


        @Override
        public void onEvCompensation(int camId, @Nullable ArsdkFeatureCamera.EvCompensation value) {
            if (value == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid EV compensation value");
            }

            controllerFor(camId).onEvCompensation(EvCompensationAdapter.from(value));
        }

        @Override
        public void onExposureSettings(int camId, @Nullable ArsdkFeatureCamera.ExposureMode mode,
                                       @Nullable ArsdkFeatureCamera.ShutterSpeed manualShutterSpeed,
                                       long manualShutterSpeedCapabilitiesBitField,
                                       @Nullable ArsdkFeatureCamera.IsoSensitivity manualIsoSensitivity,
                                       long manualIsoSensitivityCapabilitiesBitField,
                                       @Nullable ArsdkFeatureCamera.IsoSensitivity maxIsoSensitivity,
                                       long maxIsoSensitivitiesCapabilitiesBitField,
                                       @Nullable ArsdkFeatureCamera.AutoExposureMeteringMode autoExposureMeteringMode) {
            if (mode == null || manualShutterSpeed == null || manualIsoSensitivity == null
                || maxIsoSensitivity == null || autoExposureMeteringMode == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid exposure settings");
            }

            controllerFor(camId).onExposureSettings(
                    ShutterSpeedAdapter.from(manualShutterSpeedCapabilitiesBitField),
                    IsoSensitivityAdapter.from((int) manualIsoSensitivityCapabilitiesBitField),
                    IsoSensitivityAdapter.from((int) maxIsoSensitivitiesCapabilitiesBitField),
                    ExposureModeAdapter.from(mode), ShutterSpeedAdapter.from(manualShutterSpeed),
                    IsoSensitivityAdapter.from(manualIsoSensitivity), IsoSensitivityAdapter.from(maxIsoSensitivity),
                    AutoExposureMeteringModeAdapter.from(autoExposureMeteringMode)
                    );
        }


        @Override
        public void onExposure(int camId, @Nullable ArsdkFeatureCamera.ShutterSpeed shutterSpeed,
                               @Nullable ArsdkFeatureCamera.IsoSensitivity isoSensitivity,
                               @Nullable ArsdkFeatureCamera.State lock, float centerX, float centerY, float width,
                               float height) {
            if (lock == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid exposure lock state");
            }

            CameraControllerBase controller = controllerFor(camId);

            if (lock == ArsdkFeatureCamera.State.INACTIVE) {
                controller.onExposureLock(CameraExposureLock.Mode.NONE, 0, 0, 0, 0);
            } else if (centerX >= 0.0 && centerX <= 1.0
                       && centerY >= 0.0 && centerY <= 1.0
                       && width >= 0.0 && width <= 1.0
                       && height >= 0.0 && height <= 1.0) {
                controller.onExposureLock(CameraExposureLock.Mode.REGION, centerX, centerY, width, height);
            } else {
                controller.onExposureLock(CameraExposureLock.Mode.CURRENT_VALUES, 0, 0, 0, 0);
            }
        }

        @Override
        public void onWhiteBalance(int camId, @Nullable ArsdkFeatureCamera.WhiteBalanceMode mode,
                                   @Nullable ArsdkFeatureCamera.WhiteBalanceTemperature temperature,
                                   @Nullable ArsdkFeatureCamera.State lock) {
            if (mode == null || temperature == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid white balance settings");
            }

            controllerFor(camId).onWhiteBalance(WhiteBalanceModeAdapter.from(mode),
                    TemperatureAdapter.from(temperature), lock == ArsdkFeatureCamera.State.ACTIVE);
        }

        @Override
        public void onAutorecord(int camId, @Nullable ArsdkFeatureCamera.State state) {
            if (state == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid auto-record state");
            }

            controllerFor(camId).onAutoRecord(state == ArsdkFeatureCamera.State.ACTIVE);
        }

        @Override
        public void onHdrSetting(int camId, @Nullable ArsdkFeatureCamera.State state) {
            if (state == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid auto-HDR state");
            }

            controllerFor(camId).onAutoHdr(state == ArsdkFeatureCamera.State.ACTIVE);
        }

        @Override
        public void onStyle(int camId, @Nullable ArsdkFeatureCamera.Style style, int saturation, int saturationMin,
                            int saturationMax, int contrast, int contrastMin, int contrastMax, int sharpness,
                            int sharpnessMin, int sharpnessMax) {
            if (style == null || saturationMin > saturationMax || contrastMin > contrastMax
                || sharpnessMin > sharpnessMax) {
                throw new ArsdkCommand.RejectedEventException("Invalid style settings");
            }

            controllerFor(camId).onStyle(IntegerRange.of(saturationMin, saturationMax),
                    IntegerRange.of(contrastMin, contrastMax), IntegerRange.of(sharpnessMin, sharpnessMax),
                    StyleAdapter.from(style), saturation, contrast, sharpness);
        }

        @Override
        public void onAlignmentOffsets(int camId, float minBoundYaw, float maxBoundYaw, float currentYaw,
                                       float minBoundPitch, float maxBoundPitch, float currentPitch, float minBoundRoll,
                                       float maxBoundRoll, float currentRoll) {
            if (minBoundYaw > maxBoundYaw || minBoundPitch > maxBoundPitch || minBoundRoll > maxBoundRoll) {
                throw new ArsdkCommand.RejectedEventException("Invalid alignment settings");
            }

            controllerFor(camId).onAlignment(DoubleRange.of(minBoundYaw, maxBoundYaw),
                    DoubleRange.of(minBoundPitch, maxBoundPitch), DoubleRange.of(minBoundRoll, maxBoundRoll),
                    currentYaw, currentPitch, currentRoll);
        }

        @Override
        public void onHdr(int camId, @Nullable ArsdkFeatureCamera.Availability available,
                          @Nullable ArsdkFeatureCamera.State state) {

            if (available == null || state == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid HDR parameters");
            }

            controllerFor(camId).onHdr(state == ArsdkFeatureCamera.State.ACTIVE);
        }

        @Override
        public void onMaxZoomSpeed(int camId, float min, float max, float current) {
            if (min > max) {
                throw new ArsdkCommand.RejectedEventException("Invalid max zoom speed parameters");
            }

            controllerFor(camId).onMaxZoomSpeed(DoubleRange.of(min, max), current);
        }

        @Override
        public void onZoomVelocityQualityDegradation(int camId, int allowed) {
            controllerFor(camId).onZoomVelocityQualityDegradation(allowed == 1);
        }

        @Override
        public void onZoomLevel(int camId, float level) {
            controllerFor(camId).onZoomLevel(level);
        }

        @Override
        public void onZoomInfo(int camId, @Nullable ArsdkFeatureCamera.Availability availability,
                               float highQualityMaximumLevel, float maximumLevel) {
            if (availability == null || (availability == ArsdkFeatureCamera.Availability.AVAILABLE
                                         && (highQualityMaximumLevel < 1 || maximumLevel < 1))) {
                throw new ArsdkCommand.RejectedEventException("Invalid zoom info");
            }

            controllerFor(camId).onZoomInfo(maximumLevel, highQualityMaximumLevel,
                    availability == ArsdkFeatureCamera.Availability.AVAILABLE);
        }

        @Override
        public void onPhotoProgress(int camId, @Nullable ArsdkFeatureCamera.PhotoResult result, int photoCount,
                                    String mediaId) {
            if (result == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid photo progress");
            }

            controllerFor(camId).onPhotoProgress(result, photoCount, mediaId);
        }

        @Override
        public void onPhotoState(int camId, @Nullable ArsdkFeatureCamera.Availability available,
                                 @Nullable ArsdkFeatureCamera.State state) {
            if (available == null || state == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid photo state");
            }

            controllerFor(camId).onPhotoState(available == ArsdkFeatureCamera.Availability.AVAILABLE,
                    state == ArsdkFeatureCamera.State.ACTIVE);
        }

        @Override
        public void onRecordingProgress(int camId, @Nullable ArsdkFeatureCamera.RecordingResult result,
                                        String mediaId) {
            if (result == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid recording progress");
            }

            controllerFor(camId).onRecordingProgress(result, mediaId);
        }

        @Override
        public void onRecordingState(int camId, @Nullable ArsdkFeatureCamera.Availability available,
                                     @Nullable ArsdkFeatureCamera.State state, long startTimestamp) {
            if (available == null || state == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid recording state");
            }

            controllerFor(camId).onRecordingState(available == ArsdkFeatureCamera.Availability.AVAILABLE,
                    state == ArsdkFeatureCamera.State.ACTIVE, startTimestamp);
        }

        /**
         * Fetches known camera controller for a given camera.
         *
         * @param cameraId unique identifier of the camera
         *
         * @return camera controller for this camera
         *
         * @throws ArsdkCommand.RejectedEventException in case no known controller exists for the specified camera
         */
        @NonNull
        private CameraControllerBase controllerFor(int cameraId) {
            CameraControllerBase controller = mCameraControllers.get(cameraId);
            if (controller == null) {
                throw new ArsdkCommand.RejectedEventException("No such camera [id: " + cameraId + "]");
            }
            return controller;
        }
    };
}
