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

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.device.peripheral.camera.Camera;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraEvCompensation;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposure;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposureLock;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraRecording;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraStyle;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalance;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraZoom;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.BlendedThermalCameraCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraAlignmentSettingCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraExposureLockCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraExposureSettingCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraPhotoSettingCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraPhotoStateCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraRecordingSettingCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraRecordingStateCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraStyleSettingCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraWhiteBalanceLockCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraWhiteBalanceSettingCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.CameraZoomCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.MainCameraCore;
import com.parrot.drone.groundsdk.internal.device.peripheral.camera.ThermalCameraCore;
import com.parrot.drone.groundsdk.internal.value.DoubleSettingCore;
import com.parrot.drone.groundsdk.internal.value.OptionalBooleanSettingCore;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdk.value.IntegerRange;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;

/** Camera controller implementation. */
final class CameraController extends AnafiCameraRouter.CameraControllerBase {

    //region Preset store bindings {...}

    /** Camera mode preset entry. */
    private static final StorageEntry<Camera.Mode> MODE_PRESET = StorageEntry.ofEnum("mode", Camera.Mode.class);

    /** Photo mode preset entry. */
    private static final StorageEntry<CameraPhoto.Mode>
            PHOTO_MODE_PRESET = StorageEntry.ofEnum("photoMode", CameraPhoto.Mode.class);

    /** Photo formats preset entry. */
    private static final StorageEntry<EnumMap<CameraPhoto.Mode, CameraPhoto.Format>>
            FORMATS_PRESET = StorageEntry.ofEnumMap(
            "photoFormats", CameraPhoto.Mode.class, CameraPhoto.Format.class);

    /** Photo file formats preset entry. */
    private static final StorageEntry<EnumMap<CameraPhoto.Mode, CameraPhoto.FileFormat>>
            FILE_FORMATS_PRESET = StorageEntry.ofEnumMap(
            "photoFileFormats", CameraPhoto.Mode.class, CameraPhoto.FileFormat.class);

    /** Photo burst value preset entry. */
    private static final StorageEntry<CameraPhoto.BurstValue>
            BURST_PRESET = StorageEntry.ofEnum("photoBurst", CameraPhoto.BurstValue.class);

    /** Photo bracketing value preset entry. */
    private static final StorageEntry<CameraPhoto.BracketingValue>
            BRACKETING_PRESET = StorageEntry.ofEnum("photoBracketing", CameraPhoto.BracketingValue.class);

    /** Photo time-lapse interval value preset entry. */
    private static final StorageEntry<Double> TIMELAPSE_PRESET = StorageEntry.ofDouble("photoTimelapse");

    /** Photo GPS-lapse interval value preset entry. */
    private static final StorageEntry<Double> GPSLAPSE_PRESET = StorageEntry.ofDouble("photoGpslapse");

    /** Recording mode preset entry. */
    private static final StorageEntry<CameraRecording.Mode>
            RECORDING_MODE_PRESET = StorageEntry.ofEnum("recordingMode", CameraRecording.Mode.class);

    /** Recording resolutions preset entry. */
    private static final StorageEntry<EnumMap<CameraRecording.Mode, CameraRecording.Resolution>>
            RESOLUTIONS_PRESET = StorageEntry.ofEnumMap(
            "recordingResolutions", CameraRecording.Mode.class, CameraRecording.Resolution.class);

    /** Recording framerates preset entry. */
    private static final StorageEntry<EnumMap<CameraRecording.Mode, CameraRecording.Framerate>>
            FRAMERATES_PRESET = StorageEntry.ofEnumMap(
            "recordingFramerates", CameraRecording.Mode.class, CameraRecording.Framerate.class);

    /** Recording hyperlapse value preset entry. */
    private static final StorageEntry<CameraRecording.HyperlapseValue>
            HYPERLAPSE_PRESET = StorageEntry.ofEnum("recordingHyperlapse", CameraRecording.HyperlapseValue.class);

    /** Exposure mode preset entry. */
    private static final StorageEntry<CameraExposure.Mode>
            EXPOSURE_MODE_PRESET = StorageEntry.ofEnum("exposureMode", CameraExposure.Mode.class);

    /** Shutter speed preset entry. */
    private static final StorageEntry<CameraExposure.ShutterSpeed>
            SHUTTER_SPEED_PRESET = StorageEntry.ofEnum("shutterSpeed", CameraExposure.ShutterSpeed.class);

    /** Manual ISO sensitivity preset entry. */
    private static final StorageEntry<CameraExposure.IsoSensitivity>
            ISO_SENSITIVITY_PRESET = StorageEntry.ofEnum("isoSensitivity", CameraExposure.IsoSensitivity.class);

    /** Auto maximum ISO sensitivity preset entry. */
    private static final StorageEntry<CameraExposure.IsoSensitivity>
            MAX_ISO_SENSITIVITY_PRESET = StorageEntry.ofEnum("maxIsoSensitivity", CameraExposure.IsoSensitivity.class);

    /** Auto exposure metering mode preset entry. */
    private static final StorageEntry<CameraExposure.AutoExposureMeteringMode>
            AUTO_EXPOSURE_METERING_MODE_PRESET = StorageEntry.ofEnum(
            "autoExposureMeteringMode", CameraExposure.AutoExposureMeteringMode.class);

    /** EV compensation preset entry. */
    private static final StorageEntry<CameraEvCompensation>
            EV_COMPENSATION_PRESET = StorageEntry.ofEnum("evCompensation", CameraEvCompensation.class);

    /** White balance mode preset entry. */
    private static final StorageEntry<CameraWhiteBalance.Mode>
            WHITE_BALANCE_MODE_PRESET = StorageEntry.ofEnum("whiteBalanceMode", CameraWhiteBalance.Mode.class);

    /** White balance temperature preset entry. */
    private static final StorageEntry<CameraWhiteBalance.Temperature>
            WHITE_BALANCE_TEMPERATURE_PRESET = StorageEntry.ofEnum(
            "whiteBalanceTemperature", CameraWhiteBalance.Temperature.class);

    /** Image style preset entry. */
    private static final StorageEntry<CameraStyle.Style>
            STYLE_PRESET = StorageEntry.ofEnum("style", CameraStyle.Style.class);

    /** Saturation preset entry. */
    private static final StorageEntry<Integer> SATURATION_PRESET = StorageEntry.ofInteger("saturation");

    /** Contrast preset entry. */
    private static final StorageEntry<Integer> CONTRAST_PRESET = StorageEntry.ofInteger("contrast");

    /** Sharpness preset entry. */
    private static final StorageEntry<Integer> SHARPNESS_PRESET = StorageEntry.ofInteger("sharpness");

    /** Auto-record enable preset entry. */
    private static final StorageEntry<Boolean> AUTO_RECORD_ENABLE_PRESET = StorageEntry.ofBoolean("autoRecord");

    /** Auto-HDR enable preset entry. */
    private static final StorageEntry<Boolean> AUTO_HDR_ENABLE_PRESET = StorageEntry.ofBoolean("autoHdr");

    /** Maximum zoom speed preset entry. */
    private static final StorageEntry<Double> MAX_ZOOM_SPEED_PRESET = StorageEntry.ofDouble("zoomMaxSpeed");

    /** Quality degradation allowance preset entry. */
    private static final StorageEntry<Boolean>
            QUALITY_DEGRADATION_ALLOWANCE_PRESET = StorageEntry.ofBoolean("zoomQualityDegradationAllowance");

    //endregion

    //region Device specific store bindings {...}

    /** Supported photo modes device setting. */
    private static final StorageEntry<EnumSet<Camera.Mode>>
            SUPPORTED_MODES_SETTING = StorageEntry.ofEnumSet("supportedModes", Camera.Mode.class);

    /** Photo mode capabilities device setting. */
    private static final StorageEntry<Collection<CameraPhotoSettingCore.Capability>>
            PHOTO_CAPS_SETTING = new PhotoCapabilitiesStorageEntry("photoCapabilities");

    /** Photo mode supported burst values device setting. */
    private static final StorageEntry<EnumSet<CameraPhoto.BurstValue>>
            SUPPORTED_BURSTS_SETTING = StorageEntry.ofEnumSet(
            "photoSupportedBursts", CameraPhoto.BurstValue.class);

    /** Photo mode supported bracketing values device setting. */
    private static final StorageEntry<EnumSet<CameraPhoto.BracketingValue>>
            SUPPORTED_BRACKETINGS_SETTING = StorageEntry.ofEnumSet(
            "photoSupportedBracketings", CameraPhoto.BracketingValue.class);

    /** Photo mode supported time-lapse interval range device setting. */
    private static final StorageEntry<DoubleRange>
            TIMELAPSE_RANGE_SETTING = StorageEntry.ofDoubleRange("photoTimelapseRange");

    /** Photo mode supported GPS-lapse interval range device setting. */
    private static final StorageEntry<DoubleRange>
            GPSLAPSE_RANGE_SETTING = StorageEntry.ofDoubleRange("photoGpslapseRange");

    /** Recording mode capabilities device setting. */
    private static final StorageEntry<Collection<CameraRecordingSettingCore.Capability>>
            RECORDING_CAPS_SETTING = new RecordingCapabilitiesStorageEntry("recordingCapabilities");

    /** Recording mode supported hyperlapse values device setting. */
    private static final StorageEntry<EnumSet<CameraRecording.HyperlapseValue>>
            SUPPORTED_HYPERLAPSES_SETTING = StorageEntry.ofEnumSet(
            "recordingSupportedHyperlapses", CameraRecording.HyperlapseValue.class);

    /** Supported exposure modes device setting. */
    private static final StorageEntry<EnumSet<CameraExposure.Mode>>
            SUPPORTED_EXPOSURE_MODES_SETTING = StorageEntry.ofEnumSet(
            "supportedExposureModes", CameraExposure.Mode.class);

    /** Supported shutter speeds device setting. */
    private static final StorageEntry<EnumSet<CameraExposure.ShutterSpeed>>
            SUPPORTED_SHUTTER_SPEEDS_SETTING = StorageEntry.ofEnumSet(
            "supportedShutterSpeeds", CameraExposure.ShutterSpeed.class);

    /** Supported manual iso sensitivities device setting. */
    private static final StorageEntry<EnumSet<CameraExposure.IsoSensitivity>>
            SUPPORTED_ISO_SENSITIVITIES_SETTING = StorageEntry.ofEnumSet(
            "supportedIsoSensitivities", CameraExposure.IsoSensitivity.class);

    /** Supported auto maximum iso sensitivities device setting. */
    private static final StorageEntry<EnumSet<CameraExposure.IsoSensitivity>>
            SUPPORTED_MAX_ISO_SENSITIVITIES_SETTING = StorageEntry.ofEnumSet(
            "maxIsoSensitivities", CameraExposure.IsoSensitivity.class);

    /** Supported auto exposure metering modes device setting. */
    private static final StorageEntry<EnumSet<CameraExposure.AutoExposureMeteringMode>>
            SUPPORTED_AUTO_EXPOSURE_METERING_MODES_SETTING = StorageEntry.ofEnumSet(
            "supportedAutoExposureMeteringModes", CameraExposure.AutoExposureMeteringMode.class);

    /** Supported EV compensation values device setting. */
    private static final StorageEntry<EnumSet<CameraEvCompensation>>
            SUPPORTED_EV_COMPENSATIONS_SETTING = StorageEntry.ofEnumSet(
            "supportedEvCompensations", CameraEvCompensation.class);

    /** Supported white balance modes device setting. */
    private static final StorageEntry<EnumSet<CameraWhiteBalance.Mode>>
            SUPPORTED_WHITE_BALANCE_MODES_SETTING = StorageEntry.ofEnumSet(
            "supportedWhiteBalanceModes", CameraWhiteBalance.Mode.class);

    /** Supported white balance temperatures device setting. */
    private static final StorageEntry<EnumSet<CameraWhiteBalance.Temperature>>
            SUPPORTED_WHITE_BALANCE_TEMPERATURES_SETTING = StorageEntry.ofEnumSet(
            "supportedWhiteBalanceTemperatures", CameraWhiteBalance.Temperature.class);

    /** Supported styles device setting. */
    private static final StorageEntry<EnumSet<CameraStyle.Style>>
            SUPPORTED_STYLES_SETTING = StorageEntry.ofEnumSet("supportedStyles", CameraStyle.Style.class);

    /** Saturation range setting. */
    private static final StorageEntry<IntegerRange>
            SATURATION_RANGE_SETTING = StorageEntry.ofIntegerRange("saturationRange");

    /** Contrast range setting. */
    private static final StorageEntry<IntegerRange>
            CONTRAST_RANGE_SETTING = StorageEntry.ofIntegerRange("contrastRange");

    /** Sharpness range setting. */
    private static final StorageEntry<IntegerRange>
            SHARPNESS_RANGE_SETTING = StorageEntry.ofIntegerRange("sharpnessRange");

    /** Auto-record support device setting. */
    private static final StorageEntry<Boolean> AUTO_RECORD_SUPPORT_SETTING =
            StorageEntry.ofBoolean("autoRecordSupport");

    /** Auto-HDR support device setting. */
    private static final StorageEntry<Boolean> AUTO_HDR_SUPPORT_SETTING = StorageEntry.ofBoolean("autoHdrSupport");

    /** Maximum zoom speed range setting. */
    private static final StorageEntry<DoubleRange>
            MAX_ZOOM_SPEED_RANGE_SETTING = StorageEntry.ofDoubleRange("zoomMaxSpeedRange");

    //endregion

    /** Camera peripheral for which this object is the backend. */
    @NonNull
    private final CameraCore mCamera;

    /** Zoom no-ack command encoder. */
    @NonNull
    private final ZoomControlEncoder mZoomController;

    /** Dictionary containing current preset values for the camera. */
    @Nullable
    private PersistentStore.Dictionary mPresetDict;

    // region Camera state {...}

    /** {@code true} when connected to the drone and protocol connection is complete. */
    private boolean mConnected;

    /** Camera global activation state. */
    private boolean mActive;

    /** Camera mode. */
    @Nullable
    private Camera.Mode mMode;

    /** Camera photo mode. */
    @Nullable
    private CameraPhoto.Mode mPhotoMode;

    /** Photo formats, by photo mode. */
    @NonNull
    private EnumMap<CameraPhoto.Mode, CameraPhoto.Format> mFormats;

    /** Photo file formats, by photo mode. */
    @NonNull
    private EnumMap<CameraPhoto.Mode, CameraPhoto.FileFormat> mFileFormats;

    /** Photo burst value. */
    @Nullable
    private CameraPhoto.BurstValue mBurst;

    /** Photo bracketing value. */
    @Nullable
    private CameraPhoto.BracketingValue mBracketing;

    /** Photo time-lapse interval, in seconds. */
    @Nullable
    private Double mTimelapseInterval;

    /** Photo GPS-lapse interval, in meters. */
    @Nullable
    private Double mGpslapseInterval;

    /** Recording mode. */
    @Nullable
    private CameraRecording.Mode mRecordingMode;

    /** Recording resolutions, by recording mode. */
    @NonNull
    private EnumMap<CameraRecording.Mode, CameraRecording.Resolution> mResolutions;

    /** Recording framerates, by recording mode. */
    @NonNull
    private EnumMap<CameraRecording.Mode, CameraRecording.Framerate> mFramerates;

    /** Recording hyperlapse value. */
    @Nullable
    private CameraRecording.HyperlapseValue mHyperlapse;

    /** EV compensation. */
    @Nullable
    private CameraEvCompensation mEvCompensation;

    /** Exposure mode. */
    @Nullable
    private CameraExposure.Mode mExposureMode;

    /** Auto exposure metering mode. */
    @Nullable
    private CameraExposure.AutoExposureMeteringMode mAutoExposureMeteringMode;

    /** Manual shutter speed. */
    @Nullable
    private CameraExposure.ShutterSpeed mShutterSpeed;

    /** Manual iso sensitivity. */
    @Nullable
    private CameraExposure.IsoSensitivity mIsoSensitivity;

    /** Auto maximum iso sensitivity. */
    @Nullable
    private CameraExposure.IsoSensitivity mMaxIsoSensitivity;

    /** White balance mode. */
    @Nullable
    private CameraWhiteBalance.Mode mWhiteBalanceMode;

    /** Custom white balance temperature. */
    @Nullable
    private CameraWhiteBalance.Temperature mWhiteBalanceTemperature;

    /** Image style. */
    @Nullable
    private CameraStyle.Style mStyle;

    /** Image saturation. */
    @Nullable
    private Integer mSaturation;

    /** Image contrast. */
    @Nullable
    private Integer mContrast;

    /** Image sharpness. */
    @Nullable
    private Integer mSharpness;

    /** Auto-record enable setting value. */
    @Nullable
    private Boolean mAutoRecord;

    /** Auto-HDR enable setting value. */
    @Nullable
    private Boolean mAutoHdr;

    /** Maximum zoom speed value. */
    @Nullable
    private Double mMaxZoomSpeed;

    /** Zoom velocity quality degradation allowance value. */
    @Nullable
    private Boolean mZoomQualityDegradationAllowed;

    /**
     * Exposure lock mode that has been requested.
     * <p>
     * This is kept because drone exposure lock mode event is non acknowledged so it might be received right after a
     * changed has been requested but before this change has been applied. Hence, thanks to this variable, we can
     * skip such an event and avoid updating the component with an outdated lock mode.
     */
    @Nullable
    private CameraExposureLock.Mode mRequestedExposureLockMode;

    /** Horizontal center of exposure lock region that has been requested. */
    private double mRequestedExposureLockCenterX;

    /** Vertical center of exposure lock region that has been requested. */
    private double mRequestedExposureLockCenterY;

    /**
     * Current drone recording bitrate, {@code 0} if not available. Kept for reference because camera component bitrate
     * is reset to 0 when the camera moves to inactive state, and this value is restored once it moves back to active
     * state.
     */
    private int mRecordingBitrate;

    /**
     * Constructor.
     *
     * @param info camera info
     */
    CameraController(@NonNull AnafiCameraRouter.CameraInfo info) {
        super(info);
        mPresetDict = loadPresets();

        mResolutions = new EnumMap<>(CameraRecording.Mode.class);
        mFramerates = new EnumMap<>(CameraRecording.Mode.class);
        mFormats = new EnumMap<>(CameraPhoto.Mode.class);
        mFileFormats = new EnumMap<>(CameraPhoto.Mode.class);

        mZoomController = new ZoomControlEncoder(this::encodeZoomControl);

        mCamera = createCamera();

        loadPersistedData();

        if (mSettingsDict != null && !mSettingsDict.isNew()) {
            mCamera.publish();
        }
    }

    @Override
    public void onConnected() {
        mConnected = true;

        applyPresets();

        onActivationState(mActive);
    }

    @Override
    public void onActivationState(boolean active) {
        mActive = active;

        if (!mConnected) {
            return;
        }

        if (active) {
            registerNoAckCmdEncoder(mZoomController);

            mCamera.recording().updateBitrate(mRecordingBitrate);
        } else {
            unregisterNoAckCmdEncoder(mZoomController);

            mZoomController.reset();

            CameraZoomCore zoom = mCamera.zoom();
            if (zoom != null) {
                zoom.reset();
            }

            mRequestedExposureLockMode = null;

            mCamera.recording().updateBitrate(0);
            mCamera.photoState().updateState(CameraPhoto.State.FunctionState.UNAVAILABLE);
            mCamera.recordingState().updateState(CameraRecording.State.FunctionState.UNAVAILABLE);
        }

        mCamera.updateActiveFlag(active).publish();
    }

    @Override
    public void onDisconnected() {
        mCamera.cancelSettingsRollbacks();

        onActivationState(false);

        mConnected = false;
    }

    @Override
    public void onForgetting() {
        if (mSettingsDict != null) {
            mSettingsDict.clear().commit();
        }

        mCamera.unpublish();
    }

    @Override
    public void onPresetChange() {
        mPresetDict = loadPresets();

        if (mConnected) {
            applyPresets();

            mCamera.notifyUpdated();
        }
    }

    // region Event callbacks {...}

    @Override
    public void onCameraCapabilities(@NonNull EnumSet<Camera.Mode> modes,
                                     @NonNull EnumSet<CameraEvCompensation> exposureCompensationValues,
                                     @NonNull EnumSet<CameraExposure.Mode> exposureModes,
                                     @NonNull EnumSet<CameraExposure.AutoExposureMeteringMode>
                                                 autoExposureMeteringModes,
                                     boolean exposureLock,
                                     boolean exposureRoiLock,
                                     @NonNull EnumSet<CameraWhiteBalance.Mode> whiteBalanceModes,
                                     @NonNull EnumSet<CameraWhiteBalance.Temperature> whiteBalanceTemperatures,
                                     boolean whiteBalanceLock, @NonNull EnumSet<CameraStyle.Style> styles,
                                     @NonNull EnumSet<CameraRecording.HyperlapseValue> hyperlapseValues,
                                     @NonNull EnumSet<CameraPhoto.BurstValue> burstValues,
                                     @NonNull EnumSet<CameraPhoto.BracketingValue> bracketingValues,
                                     @NonNull DoubleRange timeLapseIntervalRange,
                                     @NonNull DoubleRange gpsLapseIntervalRange) {
        SUPPORTED_MODES_SETTING.save(mSettingsDict, modes);
        SUPPORTED_EV_COMPENSATIONS_SETTING.save(mSettingsDict, exposureCompensationValues);
        SUPPORTED_EXPOSURE_MODES_SETTING.save(mSettingsDict, exposureModes);
        SUPPORTED_AUTO_EXPOSURE_METERING_MODES_SETTING.save(mSettingsDict, autoExposureMeteringModes);
        SUPPORTED_WHITE_BALANCE_MODES_SETTING.save(mSettingsDict, whiteBalanceModes);
        SUPPORTED_WHITE_BALANCE_TEMPERATURES_SETTING.save(mSettingsDict, whiteBalanceTemperatures);
        SUPPORTED_STYLES_SETTING.save(mSettingsDict, styles);
        SUPPORTED_BURSTS_SETTING.save(mSettingsDict, burstValues);
        SUPPORTED_BRACKETINGS_SETTING.save(mSettingsDict, bracketingValues);
        SUPPORTED_HYPERLAPSES_SETTING.save(mSettingsDict, hyperlapseValues);
        TIMELAPSE_RANGE_SETTING.save(mSettingsDict, timeLapseIntervalRange);
        GPSLAPSE_RANGE_SETTING.save(mSettingsDict, gpsLapseIntervalRange);

        mCamera.mode()
               .updateAvailableValues(modes);

        updateCameraSupportedEVCompensationValues();

        mCamera.exposure().updateSupportedModes(exposureModes);
        mCamera.exposure().updateSupportedAutoExposureMeteringModes(autoExposureMeteringModes);

        // TODO : exposure/ROI lock support

        mCamera.whiteBalance()
               .updateSupportedModes(whiteBalanceModes)
               .updateSupportedTemperatures(whiteBalanceTemperatures);
        if (whiteBalanceLock) {
            mCamera.createWhiteBalanceLockIfNeeded();
        }

        mCamera.style().updateSupportedStyles(styles);

        mCamera.photo()
               .updateSupportedBurstValues(burstValues)
               .updateSupportedBracketingValues(bracketingValues)
               .updateTimelapseIntervalRange(timeLapseIntervalRange)
               .updateGpslapseIntervalRange(gpsLapseIntervalRange);

        mCamera.recording().updateSupportedHyperlapseValues(hyperlapseValues);
        mCamera.notifyUpdated();
    }

    @Override
    public void onPhotoCapabilities(@NonNull Collection<CameraPhotoSettingCore.Capability> capabilities) {
        PHOTO_CAPS_SETTING.save(mSettingsDict, capabilities);

        mCamera.photo().updateCapabilities(capabilities);
        mCamera.notifyUpdated();
    }

    @Override
    public void onRecordingCapabilities(@NonNull Collection<CameraRecordingSettingCore.Capability> capabilities) {
        RECORDING_CAPS_SETTING.save(mSettingsDict, capabilities);

        mCamera.recording().updateCapabilities(capabilities);
        mCamera.notifyUpdated();
    }

    @Override
    public void onCameraMode(@NonNull Camera.Mode mode) {
        mMode = mode;

        if (!mConnected) {
            return;
        }

        mCamera.mode().updateValue(mMode);
        mCamera.notifyUpdated();
    }

    @Override
    public void onPhotoMode(@NonNull CameraPhoto.Mode mode, @NonNull CameraPhoto.Format format,
                            @NonNull CameraPhoto.FileFormat fileFormat, @NonNull CameraPhoto.BurstValue burst,
                            @NonNull CameraPhoto.BracketingValue bracketing, double captureInterval) {
        mPhotoMode = mode;
        mFormats.put(mode, format);
        mFileFormats.put(mode, fileFormat);
        switch (mode) {
            case BURST:
                mBurst = burst;
                break;
            case SINGLE:
                break;
            case BRACKETING:
                mBracketing = bracketing;
                break;
            case TIME_LAPSE:
                mTimelapseInterval = captureInterval;
                break;
            case GPS_LAPSE:
                mGpslapseInterval = captureInterval;
                break;
        }

        if (mConnected) {
            CameraPhotoSettingCore setting = mCamera.photo();
            setting.updateMode(mode).updateFormat(format).updateFileFormat(fileFormat);
            switch (mode) {
                case BURST:
                    setting.updateBurstValue(burst);
                    break;
                case SINGLE:
                    break;
                case BRACKETING:
                    setting.updateBracketingValue(bracketing);
                    break;
                case TIME_LAPSE:
                    setting.updateTimelapseInterval(captureInterval);
                    break;
                case GPS_LAPSE:
                    setting.updateGpslapseInterval(captureInterval);
                    break;
            }
        }

        mCamera.notifyUpdated();
    }

    @Override
    public void onRecordingMode(@NonNull CameraRecording.Mode mode, @NonNull CameraRecording.Resolution resolution,
                                @NonNull CameraRecording.Framerate framerate,
                                @NonNull CameraRecording.HyperlapseValue hyperlapse, int bitrate) {
        mRecordingMode = mode;
        mResolutions.put(mode, resolution);
        mFramerates.put(mode, framerate);
        if (mode == CameraRecording.Mode.HYPERLAPSE) {
            mHyperlapse = hyperlapse;
        }
        mRecordingBitrate = bitrate;

        CameraRecordingSettingCore setting = mCamera.recording();
        setting.updateBitrate(bitrate);

        if (mConnected) {
            setting.updateMode(mode).updateResolution(resolution).updateFramerate(framerate);
            if (mode == CameraRecording.Mode.HYPERLAPSE) {
                setting.updateHyperlapseValue(hyperlapse);
            }
        }

        mCamera.notifyUpdated();
    }

    @Override
    public void onEvCompensation(@NonNull CameraEvCompensation evCompensation) {
        mEvCompensation = evCompensation;

        if (!mConnected) {
            return;
        }

        mCamera.exposureCompensation().updateValue(evCompensation);
        mCamera.notifyUpdated();
    }

    @Override
    public void onExposureSettings(@NonNull EnumSet<CameraExposure.ShutterSpeed> supportedShutterSpeeds,
                                   @NonNull EnumSet<CameraExposure.IsoSensitivity> supportedManualIsoSensitivities,
                                   @NonNull EnumSet<CameraExposure.IsoSensitivity> supportedMaxIsoSensitivities,
                                   @NonNull CameraExposure.Mode mode,
                                   @NonNull CameraExposure.ShutterSpeed manualShutterSpeed,
                                   @NonNull CameraExposure.IsoSensitivity manualIsoSensitivity,
                                   @NonNull CameraExposure.IsoSensitivity maxIsoSensitivity,
                                   @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {

        SUPPORTED_SHUTTER_SPEEDS_SETTING.save(mSettingsDict, supportedShutterSpeeds);
        SUPPORTED_ISO_SENSITIVITIES_SETTING.save(mSettingsDict, supportedManualIsoSensitivities);
        SUPPORTED_MAX_ISO_SENSITIVITIES_SETTING.save(mSettingsDict, supportedMaxIsoSensitivities);

        CameraExposureSettingCore setting = mCamera.exposure();
        setting.updateSupportedShutterSpeeds(supportedShutterSpeeds);
        setting.updateSupportedIsoSensitivities(supportedManualIsoSensitivities);
        setting.updateMaximumIsoSensitivities(supportedMaxIsoSensitivities);

        mExposureMode = mode;
        mAutoExposureMeteringMode = autoExposureMeteringMode;
        mShutterSpeed = manualShutterSpeed;
        mIsoSensitivity = manualIsoSensitivity;
        mMaxIsoSensitivity = maxIsoSensitivity;

        if (mConnected) {
            setting.updateMode(mode);
            switch (mode) {
                case MANUAL:
                    setting.updateShutterSpeed(manualShutterSpeed).updateIsoSensitivity(manualIsoSensitivity);
                    break;
                case MANUAL_SHUTTER_SPEED:
                    setting.updateShutterSpeed(manualShutterSpeed);
                    break;
                case MANUAL_ISO_SENSITIVITY:
                    setting.updateIsoSensitivity(manualIsoSensitivity);
                    break;
                case AUTOMATIC:
                case AUTOMATIC_PREFER_SHUTTER_SPEED:
                case AUTOMATIC_PREFER_ISO_SENSITIVITY:
                    setting.updateMaxIsoSensitivity(maxIsoSensitivity)
                           .updateAutoExposureMeteringMode(autoExposureMeteringMode);
                    break;
            }
            updateCameraSupportedEVCompensationValues();
        }

        mCamera.notifyUpdated();
    }

    @Override
    public void onExposureLock(@NonNull CameraExposureLock.Mode mode, @FloatRange(from = 0, to = 1) double centerX,
                               @FloatRange(from = 0, to = 1) double centerY, @FloatRange(from = 0, to = 1) double width,
                               @FloatRange(from = 0, to = 1) double height) {
        // if there is no pending request or if the requested lock mode matches the received mode
        if (mRequestedExposureLockMode == null || CameraExposureLockCore.isSameRequest(mode, centerX, centerY,
                mRequestedExposureLockMode, mRequestedExposureLockCenterX, mRequestedExposureLockCenterY)) {
            mRequestedExposureLockMode = null;

            mCamera.updateExposureLock(mode, centerX, centerY, width, height);

            updateCameraSupportedEVCompensationValues();

            mCamera.notifyUpdated();
        }
    }

    @Override
    public void onWhiteBalance(@NonNull CameraWhiteBalance.Mode mode,
                               @NonNull CameraWhiteBalance.Temperature temperature, boolean locked) {
        mWhiteBalanceMode = mode;
        mWhiteBalanceTemperature = temperature;

        if (!mConnected) {
            return;
        }

        CameraWhiteBalanceSettingCore setting = mCamera.whiteBalance();
        setting.updateMode(mode);
        if (mode == CameraWhiteBalance.Mode.CUSTOM) {
            setting.updateTemperature(temperature);
        }

        CameraWhiteBalanceLockCore whiteBalanceLock = mCamera.whiteBalanceLock();
        if (whiteBalanceLock != null) {
            whiteBalanceLock.updateLockable(mode == CameraWhiteBalance.Mode.AUTOMATIC).updateLocked(locked);
        }

        mCamera.notifyUpdated();
    }

    @Override
    public void onAutoRecord(boolean active) {
        AUTO_RECORD_SUPPORT_SETTING.save(mSettingsDict, true);

        OptionalBooleanSettingCore setting = mCamera.autoRecord();
        setting.updateSupportedFlag(true);

        mAutoRecord = active;

        if (mConnected) {
            setting.updateValue(active);
        }

        mCamera.notifyUpdated();
    }

    @Override
    public void onAutoHdr(boolean active) {
        AUTO_HDR_SUPPORT_SETTING.save(mSettingsDict, true);

        OptionalBooleanSettingCore setting = mCamera.autoHdr();
        setting.updateSupportedFlag(true);

        mAutoHdr = active;

        if (mConnected) {
            setting.updateValue(active);
        }

        mCamera.notifyUpdated();
    }

    @Override
    public void onStyle(@NonNull IntegerRange saturationRange, @NonNull IntegerRange contrastRange,
                        @NonNull IntegerRange sharpnessRange, @NonNull CameraStyle.Style style, int saturation,
                        int contrast, int sharpness) {
        SATURATION_RANGE_SETTING.save(mSettingsDict, saturationRange);
        CONTRAST_RANGE_SETTING.save(mSettingsDict, contrastRange);
        SHARPNESS_RANGE_SETTING.save(mSettingsDict, sharpnessRange);

        CameraStyleSettingCore setting = mCamera.style();
        setting.saturation().updateBounds(saturationRange);
        setting.contrast().updateBounds(contrastRange);
        setting.sharpness().updateBounds(sharpnessRange);

        mStyle = style;
        mSaturation = saturation;
        mContrast = contrast;
        mSharpness = sharpness;

        if (mConnected) {
            setting.updateStyle(style);
            setting.saturation().updateValue(saturation);
            setting.contrast().updateValue(contrast);
            setting.sharpness().updateValue(sharpness);
        }

        mCamera.notifyUpdated();
    }

    @Override
    void onAlignment(@NonNull DoubleRange yawRange, @NonNull DoubleRange pitchRange, @NonNull DoubleRange rollRange,
                     double yaw, double pitch, double roll) {
        CameraAlignmentSettingCore setting = mCamera.createAlignmentIfNeeded();
        setting.updateSupportedYawRange(yawRange).updateYaw(yaw);
        setting.updateSupportedPitchRange(pitchRange).updatePitch(pitch);
        setting.updateSupportedRollRange(rollRange).updateRoll(roll);
        mCamera.notifyUpdated();
    }

    @Override
    public void onHdr(boolean active) {
        mCamera.updateHdrActive(active).notifyUpdated();
    }

    @Override
    public void onMaxZoomSpeed(@NonNull DoubleRange maxSpeedRange, double maxSpeed) {
        MAX_ZOOM_SPEED_RANGE_SETTING.save(mSettingsDict, maxSpeedRange);

        DoubleSettingCore setting = mCamera.createZoomIfNeeded().maxSpeed();
        setting.updateBounds(maxSpeedRange);

        mMaxZoomSpeed = maxSpeed;

        if (mConnected) {
            setting.updateValue(maxSpeed);
        }

        mCamera.notifyUpdated();
    }

    @Override
    public void onZoomVelocityQualityDegradation(boolean allowed) {
        mZoomQualityDegradationAllowed = allowed;

        if (!mConnected) {
            return;
        }

        mCamera.createZoomIfNeeded().velocityQualityDegradationAllowance().updateValue(allowed);
        mCamera.notifyUpdated();
    }

    @Override
    public void onZoomLevel(double level) {
        mCamera.createZoomIfNeeded().updateCurrentLevel(level);
        mCamera.notifyUpdated();
    }

    @Override
    public void onZoomInfo(double maxLossyLevel, double maxLossLessLevel, boolean available) {
        CameraZoomCore zoom = mCamera.createZoomIfNeeded();
        zoom.updateAvailability(available);

        if (available) {
            zoom.updateMaxLossLessLevel(maxLossLessLevel).updateMaxLossyLevel(maxLossyLevel);
        }

        mCamera.notifyUpdated();
    }

    @Override
    public void onPhotoProgress(@NonNull ArsdkFeatureCamera.PhotoResult result, int photoCount,
                                @NonNull String mediaId) {
        CameraPhotoStateCore state = mCamera.photoState();

        switch (result) {
            case TAKING_PHOTO:
                break;
            case PHOTO_SAVED:
                state.updateState(CameraPhoto.State.FunctionState.STOPPED);
                break;
            case PHOTO_TAKEN:
                state.updatePhotoCount(photoCount);
                break;
            case ERROR_NO_STORAGE_SPACE:
                state.updateState(CameraPhoto.State.FunctionState.ERROR_INSUFFICIENT_STORAGE);
                break;
            case ERROR_BAD_STATE:
                // This occurs when we try to take a photo with remote control while a photo is in progress (or when
                // camera mode is changing). We ignore it because the camera is still taking the photo normally.
                break;
            case ERROR:
                state.updateState(CameraPhoto.State.FunctionState.ERROR_INTERNAL);
                break;
        }

        if (!mediaId.isEmpty()) {
            state.updateMediaId(mediaId);
        }

        mCamera.notifyUpdated();
    }

    @Override
    public void onPhotoState(boolean available, boolean active) {
        CameraPhoto.State.FunctionState photoState;

        if (!available) {
            photoState = CameraPhoto.State.FunctionState.UNAVAILABLE;
        } else if (active) {
            photoState = CameraPhoto.State.FunctionState.STARTED;
        } else {
            photoState = CameraPhoto.State.FunctionState.STOPPED;
        }

        mCamera.photoState().updateState(photoState);
        mCamera.notifyUpdated();
    }

    @Override
    public void onRecordingProgress(@NonNull ArsdkFeatureCamera.RecordingResult result, @NonNull String mediaId) {
        CameraRecordingStateCore state = mCamera.recordingState();

        switch (result) {
            case STARTED:
                break;
            case STOPPED:
                state.updateState(CameraRecording.State.FunctionState.STOPPED);
                break;
            case STOPPED_NO_STORAGE_SPACE:
                state.updateState(CameraRecording.State.FunctionState.ERROR_INSUFFICIENT_STORAGE_SPACE);
                break;
            case STOPPED_STORAGE_TOO_SLOW:
                state.updateState(CameraRecording.State.FunctionState.ERROR_INSUFFICIENT_STORAGE_SPEED);
                break;
            case ERROR_BAD_STATE:
                // This occurs when we try to start recording with application and remote control at the same time, or
                // when we start recording while camera mode is changing. This is not a real error and can be ignored.
                break;
            case ERROR:
                state.updateState(CameraRecording.State.FunctionState.ERROR_INTERNAL);
                break;
            case STOPPED_RECONFIGURED:
                state.updateState(CameraRecording.State.FunctionState.CONFIGURATION_CHANGE);
                break;
        }

        if (!mediaId.isEmpty()) {
            state.updateMediaId(mediaId);
        }

        mCamera.notifyUpdated();
    }

    @Override
    public void onRecordingState(boolean available, boolean active, long startTimestamp) {
        CameraRecording.State.FunctionState recordingState;

        if (!available) {
            recordingState = CameraRecording.State.FunctionState.UNAVAILABLE;
        } else if (active) {
            recordingState = CameraRecording.State.FunctionState.STARTED;
        } else {
            recordingState = CameraRecording.State.FunctionState.STOPPED;
        }

        mCamera.recordingState().updateState(recordingState).updateStartTime(startTimestamp);
        mCamera.notifyUpdated();
    }

    // endregion

    /**
     * Loads presets and settings from persistent storage and updates the component accordingly.
     */
    private void loadPersistedData() {
        EnumSet<Camera.Mode> supportedModes = SUPPORTED_MODES_SETTING.load(mSettingsDict);
        if (supportedModes != null) {
            mCamera.mode().updateAvailableValues(supportedModes);
        }

        Collection<CameraPhotoSettingCore.Capability> photoCaps = PHOTO_CAPS_SETTING.load(mSettingsDict);
        if (photoCaps != null) {
            mCamera.photo().updateCapabilities(photoCaps);
        }

        EnumSet<CameraPhoto.BurstValue> supportedBurstValues = SUPPORTED_BURSTS_SETTING.load(mSettingsDict);
        if (supportedBurstValues != null) {
            mCamera.photo().updateSupportedBurstValues(supportedBurstValues);
        }

        EnumSet<CameraPhoto.BracketingValue> supportedBracketingValues =
                SUPPORTED_BRACKETINGS_SETTING.load(mSettingsDict);
        if (supportedBracketingValues != null) {
            mCamera.photo().updateSupportedBracketingValues(supportedBracketingValues);
        }

        DoubleRange timelapseRange = TIMELAPSE_RANGE_SETTING.load(mSettingsDict);
        if (timelapseRange != null) {
            mCamera.photo().updateTimelapseIntervalRange(timelapseRange);
        }

        DoubleRange gpslapseRange = GPSLAPSE_RANGE_SETTING.load(mSettingsDict);
        if (gpslapseRange != null) {
            mCamera.photo().updateGpslapseIntervalRange(gpslapseRange);
        }

        Collection<CameraRecordingSettingCore.Capability> recordingCaps = RECORDING_CAPS_SETTING.load(mSettingsDict);
        if (recordingCaps != null) {
            mCamera.recording().updateCapabilities(recordingCaps);
        }

        EnumSet<CameraRecording.HyperlapseValue> supportedHyperlapseValues =
                SUPPORTED_HYPERLAPSES_SETTING.load(mSettingsDict);
        if (supportedHyperlapseValues != null) {
            mCamera.recording().updateSupportedHyperlapseValues(supportedHyperlapseValues);
        }

        EnumSet<CameraExposure.Mode> supportedExposureModes = SUPPORTED_EXPOSURE_MODES_SETTING.load(mSettingsDict);
        if (supportedExposureModes != null) {
            mCamera.exposure().updateSupportedModes(supportedExposureModes);
        }

        EnumSet<CameraExposure.ShutterSpeed> supportedShutterSpeeds =
                SUPPORTED_SHUTTER_SPEEDS_SETTING.load(mSettingsDict);
        if (supportedShutterSpeeds != null) {
            mCamera.exposure().updateSupportedShutterSpeeds(supportedShutterSpeeds);
        }

        EnumSet<CameraExposure.IsoSensitivity> supportedIsoSensitivities =
                SUPPORTED_ISO_SENSITIVITIES_SETTING.load(mSettingsDict);
        if (supportedIsoSensitivities != null) {
            mCamera.exposure().updateSupportedIsoSensitivities(supportedIsoSensitivities);
        }

        EnumSet<CameraExposure.IsoSensitivity> maxIsoSensitivities =
                SUPPORTED_MAX_ISO_SENSITIVITIES_SETTING.load(mSettingsDict);
        if (maxIsoSensitivities != null) {
            mCamera.exposure().updateMaximumIsoSensitivities(maxIsoSensitivities);
        }

        EnumSet<CameraExposure.AutoExposureMeteringMode> autoExposureMeteringModes =
                SUPPORTED_AUTO_EXPOSURE_METERING_MODES_SETTING.load(mSettingsDict);
        if (autoExposureMeteringModes != null) {
            mCamera.exposure().updateSupportedAutoExposureMeteringModes(autoExposureMeteringModes);
        }

        EnumSet<CameraEvCompensation> supportedEvCompensations = SUPPORTED_EV_COMPENSATIONS_SETTING.load(mSettingsDict);
        if (supportedEvCompensations != null) {
            mCamera.exposureCompensation().updateAvailableValues(supportedEvCompensations);
        }

        EnumSet<CameraWhiteBalance.Mode> supportedWhiteBalanceModes =
                SUPPORTED_WHITE_BALANCE_MODES_SETTING.load(mSettingsDict);
        if (supportedWhiteBalanceModes != null) {
            mCamera.whiteBalance().updateSupportedModes(supportedWhiteBalanceModes);
        }

        EnumSet<CameraWhiteBalance.Temperature> supportedWhiteBalanceTemperatures =
                SUPPORTED_WHITE_BALANCE_TEMPERATURES_SETTING.load(mSettingsDict);
        if (supportedWhiteBalanceTemperatures != null) {
            mCamera.whiteBalance().updateSupportedTemperatures(supportedWhiteBalanceTemperatures);
        }

        EnumSet<CameraStyle.Style> supportedStyles = SUPPORTED_STYLES_SETTING.load(mSettingsDict);
        if (supportedStyles != null) {
            mCamera.style().updateSupportedStyles(supportedStyles);
        }

        IntegerRange saturationRange = SATURATION_RANGE_SETTING.load(mSettingsDict);
        if (saturationRange != null) {
            mCamera.style().saturation().updateBounds(saturationRange);
        }

        IntegerRange contrastRange = CONTRAST_RANGE_SETTING.load(mSettingsDict);
        if (contrastRange != null) {
            mCamera.style().contrast().updateBounds(contrastRange);
        }

        IntegerRange sharpnessRange = SHARPNESS_RANGE_SETTING.load(mSettingsDict);
        if (sharpnessRange != null) {
            mCamera.style().sharpness().updateBounds(sharpnessRange);
        }

        mCamera.autoRecord()
               .updateSupportedFlag(Boolean.TRUE.equals(AUTO_RECORD_SUPPORT_SETTING.load(mSettingsDict)));

        mCamera.autoHdr()
               .updateSupportedFlag(Boolean.TRUE.equals(AUTO_HDR_SUPPORT_SETTING.load(mSettingsDict)));

        DoubleRange maxSpeedRange = MAX_ZOOM_SPEED_RANGE_SETTING.load(mSettingsDict);
        if (maxSpeedRange != null) {
            mCamera.createZoomIfNeeded()
                   .maxSpeed()
                   .updateBounds(maxSpeedRange);
        }

        applyPresets();
    }

    /**
     * Applies component's persisted presets.
     */
    private void applyPresets() {
        // NOTE: due to possible race condition on the firmware side, apply auto HDR first,
        //       before any photo and (in particular) recording configuration.
        applyAutoHdr(AUTO_HDR_ENABLE_PRESET.load(mPresetDict));

        Camera.Mode modeBeforeSwitch = mMode;
        // first configure settings for the mode the drone is NOT in, to avoid extraneous pipeline reconfiguration
        if (modeBeforeSwitch != Camera.Mode.PHOTO) {
            applyPhotoPresets();
        }
        if (modeBeforeSwitch != Camera.Mode.RECORDING) {
            applyRecordingPresets();
        }

        // then switch to preset camera mode
        applyCameraMode(MODE_PRESET.load(mPresetDict));

        // apply rest of configuration
        if (modeBeforeSwitch == Camera.Mode.PHOTO) {
            applyPhotoPresets();
        } else if (modeBeforeSwitch == Camera.Mode.RECORDING) {
            applyRecordingPresets();
        }

        applyEvCompensation(EV_COMPENSATION_PRESET.load(mPresetDict));
        applyExposureSettings(EXPOSURE_MODE_PRESET.load(mPresetDict),
                SHUTTER_SPEED_PRESET.load(mPresetDict), ISO_SENSITIVITY_PRESET.load(mPresetDict),
                MAX_ISO_SENSITIVITY_PRESET.load(mPresetDict), AUTO_EXPOSURE_METERING_MODE_PRESET.load(mPresetDict));
        applyWhiteBalanceSettings(WHITE_BALANCE_MODE_PRESET.load(mPresetDict),
                WHITE_BALANCE_TEMPERATURE_PRESET.load(mPresetDict));
        applyStyle(STYLE_PRESET.load(mPresetDict));
        applyStyleParameters(SATURATION_PRESET.load(mPresetDict), CONTRAST_PRESET.load(mPresetDict),
                SHARPNESS_PRESET.load(mPresetDict));
        applyAutoRecord(AUTO_RECORD_ENABLE_PRESET.load(mPresetDict));
        applyMaxZoomSpeed(MAX_ZOOM_SPEED_PRESET.load(mPresetDict));
        applyZoomQualityDegradationAllowance(QUALITY_DEGRADATION_ALLOWANCE_PRESET.load(mPresetDict));
    }

    /**
     * Applies persisted photo mode presets.
     */
    private void applyPhotoPresets() {
        CameraPhoto.Mode storedPhotoMode = PHOTO_MODE_PRESET.load(mPresetDict);

        EnumMap<CameraPhoto.Mode, CameraPhoto.Format> storedFormats =
                FORMATS_PRESET.load(mPresetDict, () -> new EnumMap<>(CameraPhoto.Mode.class));

        EnumMap<CameraPhoto.Mode, CameraPhoto.FileFormat> storedFileFormats =
                FILE_FORMATS_PRESET.load(mPresetDict, () -> new EnumMap<>(CameraPhoto.Mode.class));

        applyPhotoSettings(
                storedPhotoMode,
                storedFormats.get(storedPhotoMode),
                storedFileFormats.get(storedPhotoMode),
                BURST_PRESET.load(mPresetDict),
                BRACKETING_PRESET.load(mPresetDict),
                TIMELAPSE_PRESET.load(mPresetDict),
                GPSLAPSE_PRESET.load(mPresetDict));

        CameraPhoto.Format currentFormat = mFormats.get(mPhotoMode);
        if (currentFormat != null) {
            storedFormats.put(mPhotoMode, currentFormat);
        }
        mFormats = storedFormats;

        CameraPhoto.FileFormat currentFileFormat = mFileFormats.get(mPhotoMode);
        if (currentFileFormat != null) {
            storedFileFormats.put(mPhotoMode, currentFileFormat);
        }
        mFileFormats = storedFileFormats;
    }

    /**
     * Applies persisted recording mode presets.
     */
    private void applyRecordingPresets() {
        CameraRecording.Mode storedRecordingMode = RECORDING_MODE_PRESET.load(mPresetDict);

        EnumMap<CameraRecording.Mode, CameraRecording.Resolution> storedResolutions =
                RESOLUTIONS_PRESET.load(mPresetDict, () -> new EnumMap<>(CameraRecording.Mode.class));

        EnumMap<CameraRecording.Mode, CameraRecording.Framerate> storedFramerates =
                FRAMERATES_PRESET.load(mPresetDict, () -> new EnumMap<>(CameraRecording.Mode.class));

        applyRecordingSettings(
                storedRecordingMode,
                storedResolutions.get(storedRecordingMode),
                storedFramerates.get(storedRecordingMode),
                HYPERLAPSE_PRESET.load(mPresetDict));


        CameraRecording.Resolution currentResolution = mResolutions.get(mRecordingMode);
        if (currentResolution != null) {
            storedResolutions.put(mRecordingMode, currentResolution);
        }
        mResolutions = storedResolutions;

        CameraRecording.Framerate currentFramerate = mFramerates.get(mRecordingMode);
        if (currentFramerate != null) {
            storedFramerates.put(mRecordingMode, currentFramerate);
        }
        mFramerates = storedFramerates;
    }

    /**
     * Computes and updates supported exposure compensation values.
     * <p>
     * In manual exposure mode and in exposure lock mode, EV compensation cannot be modified, but the drone does not
     * tell this. <br/>
     * So in that cases, the component's list of supported exposure compensation values is cleared.
     */
    private void updateCameraSupportedEVCompensationValues() {
        EnumSet<CameraEvCompensation> evs = EnumSet.noneOf(CameraEvCompensation.class);

        CameraExposureLock exposureLock = mCamera.exposureLock();
        if (mCamera.exposure().mode() != CameraExposure.Mode.MANUAL
            && (exposureLock == null || exposureLock.mode() == CameraExposureLock.Mode.NONE)) {
            evs = SUPPORTED_EV_COMPENSATIONS_SETTING.load(mSettingsDict, evs);
        }

        mCamera.exposureCompensation().updateAvailableValues(evs);
    }

    /**
     * Creates appropriate camera component from a given model.
     *
     * @return a new camera component dedicated to the specified model
     *
     * @throws IllegalArgumentException in case of unsupported model
     */
    @NonNull
    private CameraCore createCamera() {
        switch (mInfo.mModel) {
            case MAIN:
                return new MainCameraCore(mPeripheralStore, mBackend);
            case THERMAL:
                return new ThermalCameraCore(mPeripheralStore, mBackend);
            case THERMAL_BLENDED:
                return new BlendedThermalCameraCore(mPeripheralStore, mBackend);
        }
        return null;
    }

    // region Apply methods { ... }

    /**
     * Applies a camera mode.
     * <ul>
     * <li>Finds an appropriate fallback value if the given mode is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param mode camera mode to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyCameraMode(@Nullable Camera.Mode mode) {
        if ((mode = validateCameraMode(mode)) == null) {
            return false;
        }

        boolean updating = mode != mMode
                           && sendCameraMode(mode);

        mMode = mode;

        mCamera.mode().updateValue(mode);
        return updating;
    }

    /**
     * Applies photo mode settings.
     * <ul>
     * <li>Finds appropriate fallback values in replacement of null or unsupported ones;</li>
     * <li>Sends the computed values to the drone in case any one differs from the last received values;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param mode              photo mode to apply
     * @param format            photo format to apply
     * @param fileFormat        photo file format to apply
     * @param burst             photo burst value to apply
     * @param bracketing        photo bracketing value to apply
     * @param timelapseInterval photo time-lapse interval to apply
     * @param gpslapseInterval  photo GPS-lapse interval to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyPhotoSettings(@Nullable CameraPhoto.Mode mode, @Nullable CameraPhoto.Format format,
                                       @Nullable CameraPhoto.FileFormat fileFormat,
                                       @Nullable CameraPhoto.BurstValue burst,
                                       @Nullable CameraPhoto.BracketingValue bracketing,
                                       @Nullable Double timelapseInterval, @Nullable Double gpslapseInterval) {
        if ((mode = validatePhotoMode(mode)) == null
            || (format = validateFormat(mode, format)) == null
            || (fileFormat = validateFileFormat(mode, format, fileFormat)) == null
            || (burst = validateBurst(mode, burst)) == null
            || (bracketing = validateBracketing(mode, bracketing)) == null) {
            return false;
        }
        timelapseInterval = validateTimelapseInterval(timelapseInterval);
        gpslapseInterval = validateGpslapseInterval(gpslapseInterval);
        double interval = mode == CameraPhoto.Mode.TIME_LAPSE ? timelapseInterval : gpslapseInterval;

        boolean updating = (mode != mPhotoMode
                            || format != mFormats.get(mode)
                            || fileFormat != mFileFormats.get(mode)
                            || (mode == CameraPhoto.Mode.BURST && burst != mBurst)
                            || (mode == CameraPhoto.Mode.BRACKETING && bracketing != mBracketing)
                            || (mode == CameraPhoto.Mode.TIME_LAPSE && !timelapseInterval.equals(mTimelapseInterval))
                            || (mode == CameraPhoto.Mode.GPS_LAPSE && !gpslapseInterval.equals(mGpslapseInterval)))
                           && sendPhotoSettings(mode, format, fileFormat, burst, bracketing, interval);

        mPhotoMode = mode;
        mFormats.put(mode, format);
        mFileFormats.put(mode, fileFormat);
        mBurst = burst;
        mBracketing = bracketing;
        mTimelapseInterval = timelapseInterval;
        mGpslapseInterval = gpslapseInterval;

        mCamera.photo()
               .updateMode(mode)
               .updateFormat(format)
               .updateFileFormat(fileFormat)
               .updateBurstValue(burst)
               .updateBracketingValue(bracketing)
               .updateTimelapseInterval(timelapseInterval)
               .updateGpslapseInterval(gpslapseInterval);

        return updating;
    }

    /**
     * Applies recording mode settings.
     * <ul>
     * <li>Finds appropriate fallback values in replacement of null or unsupported ones;</li>
     * <li>Sends the computed values to the drone in case any one differs from the last received values;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param mode       recording mode to apply
     * @param resolution recording resolution to apply
     * @param framerate  recording framerate to apply
     * @param hyperlapse recording hyperlapse value to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyRecordingSettings(@Nullable CameraRecording.Mode mode,
                                           @Nullable CameraRecording.Resolution resolution,
                                           @Nullable CameraRecording.Framerate framerate,
                                           @Nullable CameraRecording.HyperlapseValue hyperlapse) {
        if ((mode = validateRecordingMode(mode)) == null
            || (resolution = validateResolution(mode, resolution)) == null
            || (framerate = validateFramerate(mode, resolution, framerate)) == null
            || (hyperlapse = validateHyperlapse(mode, hyperlapse)) == null) {
            return false;
        }

        boolean updating = (mode != mRecordingMode
                            || resolution != mResolutions.get(mode)
                            || framerate != mFramerates.get(mode)
                            || (mode == CameraRecording.Mode.HYPERLAPSE && hyperlapse != mHyperlapse))
                           && sendRecordingSettings(mode, resolution, framerate, hyperlapse);

        mRecordingMode = mode;
        mResolutions.put(mode, resolution);
        mFramerates.put(mode, framerate);
        mHyperlapse = hyperlapse;

        mCamera.recording()
               .updateMode(mode)
               .updateResolution(resolution)
               .updateFramerate(framerate)
               .updateHyperlapseValue(hyperlapse);

        return updating;
    }

    /**
     * Applies an EV compensation value.
     * <ul>
     * <li>Finds an appropriate fallback value if the given value is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param ev EV compensation value to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyEvCompensation(@Nullable CameraEvCompensation ev) {
        if ((ev = validateEvCompensation(ev)) == null) {
            return false;
        }

        boolean updating = ev != mEvCompensation
                           && sendEvCompensation(ev);

        mEvCompensation = ev;

        mCamera.exposureCompensation().updateValue(ev);

        return updating;
    }

    /**
     * Applies exposure settings.
     * <ul>
     * <li>Finds appropriate fallback values in replacement of null or unsupported ones;</li>
     * <li>Sends the computed values to the drone in case any one differs from the last received values;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param mode                      exposure mode to apply
     * @param shutterSpeed              shutter speed to apply
     * @param isoSensitivity            manual ISO sensitivity to apply
     * @param maxIsoSensitivity         auto maximum ISO sensitivity to apply
     * @param autoExposureMeteringMode  auto exposure metering mode to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyExposureSettings(@Nullable CameraExposure.Mode mode,
                                          @Nullable CameraExposure.ShutterSpeed shutterSpeed,
                                          @Nullable CameraExposure.IsoSensitivity isoSensitivity,
                                          @Nullable CameraExposure.IsoSensitivity maxIsoSensitivity,
                                          @Nullable CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
        if ((mode = validateExposureMode(mode)) == null
            || (shutterSpeed = validateShutterSpeed(mode, shutterSpeed)) == null
            || (isoSensitivity = validateIsoSensitivity(mode, isoSensitivity)) == null
            || (maxIsoSensitivity = validateMaximumIsoSensitivity(mode, maxIsoSensitivity)) == null
            || (autoExposureMeteringMode = validateAutoExposureMeteringMode(mode, autoExposureMeteringMode)) == null) {
            return false;
        }

        boolean updating = (mode != mExposureMode
                            || ((mode == CameraExposure.Mode.MANUAL
                                 || mode == CameraExposure.Mode.MANUAL_SHUTTER_SPEED)
                                && shutterSpeed != mShutterSpeed)
                            || ((mode == CameraExposure.Mode.MANUAL
                                 || mode == CameraExposure.Mode.MANUAL_ISO_SENSITIVITY)
                                && isoSensitivity != mIsoSensitivity)
                            || ((mode == CameraExposure.Mode.AUTOMATIC
                                 || mode == CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED
                                 || mode == CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY)
                                && (maxIsoSensitivity != mMaxIsoSensitivity
                                || autoExposureMeteringMode != mAutoExposureMeteringMode)
                            ))
                           && sendExposureSettings(mode, shutterSpeed, isoSensitivity, maxIsoSensitivity,
                autoExposureMeteringMode);

        mExposureMode = mode;
        mShutterSpeed = shutterSpeed;
        mIsoSensitivity = isoSensitivity;
        mMaxIsoSensitivity = maxIsoSensitivity;
        mAutoExposureMeteringMode = autoExposureMeteringMode;

        mCamera.exposure()
               .updateMode(mode)
               .updateShutterSpeed(shutterSpeed)
               .updateIsoSensitivity(isoSensitivity)
               .updateMaxIsoSensitivity(maxIsoSensitivity)
               .updateAutoExposureMeteringMode(autoExposureMeteringMode);

        updateCameraSupportedEVCompensationValues();

        return updating;
    }

    /**
     * Applies white balance settings.
     * <ul>
     * <li>Finds appropriate fallback values in replacement of null or unsupported ones;</li>
     * <li>Sends the computed values to the drone in case any one differs from the last received values;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param mode        white balance mode to apply
     * @param temperature white balance temperature to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyWhiteBalanceSettings(@Nullable CameraWhiteBalance.Mode mode,
                                              @Nullable CameraWhiteBalance.Temperature temperature) {
        if ((mode = validateWhiteBalanceMode(mode)) == null
            || (temperature = validateWhiteBalanceTemperature(mode, temperature)) == null) {
            return false;
        }

        boolean updating = (mode != mWhiteBalanceMode
                            || (mode == CameraWhiteBalance.Mode.CUSTOM && temperature != mWhiteBalanceTemperature))
                           && sendWhiteBalance(mode, temperature);

        mWhiteBalanceMode = mode;
        mWhiteBalanceTemperature = temperature;

        mCamera.whiteBalance()
               .updateMode(mode)
               .updateTemperature(temperature);
        CameraWhiteBalanceLockCore whiteBalanceLock = mCamera.whiteBalanceLock();
        if (whiteBalanceLock != null) {
            whiteBalanceLock.updateLockable(mode == CameraWhiteBalance.Mode.AUTOMATIC);
        }

        return updating;
    }

    /**
     * Applies an image style.
     * <ul>
     * <li>Finds an appropriate fallback value if the given style is null, or unsupported;</li>
     * <li>Sends the computed value to the drone in case it differs from the last received value;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param style image style to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyStyle(@Nullable CameraStyle.Style style) {
        if ((style = validateStyle(style)) == null) {
            return false;
        }

        boolean updating = style != mStyle
                           && sendStyle(style);

        mStyle = style;

        mCamera.style().updateStyle(style);

        return updating;
    }

    /**
     * Applies style parameters.
     * <ul>
     * <li>Finds appropriate fallback values in replacement of null ones;</li>
     * <li>Sends the computed values to the drone in case any one differs from the last received values;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param saturation saturation to apply
     * @param contrast   contrast to apply
     * @param sharpness  sharpness to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyStyleParameters(@Nullable Integer saturation, @Nullable Integer contrast,
                                         @Nullable Integer sharpness) {
        // Validating given values
        if (saturation == null) {
            saturation = mSaturation;
        }
        if (contrast == null) {
            contrast = mContrast;
        }
        if (sharpness == null) {
            sharpness = mSharpness;
        }
        if (saturation == null || contrast == null || sharpness == null) {
            return false;
        }

        boolean updating = (!saturation.equals(mSaturation)
                            || !contrast.equals(mContrast)
                            || !sharpness.equals(mSharpness))
                           && sendStyleParameters(saturation, contrast, sharpness);

        mSaturation = saturation;
        mContrast = contrast;
        mSharpness = sharpness;

        mCamera.style().saturation().updateValue(saturation);
        mCamera.style().contrast().updateValue(contrast);
        mCamera.style().sharpness().updateValue(sharpness);

        return updating;
    }

    /**
     * Applies auto-record setting value.
     * <ul>
     * <li>Gets the last received value if the given one is null;</li>
     * <li>Sends the obtained value to the drone in case it differs from the last received value;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param enable auto-record setting value to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyAutoRecord(Boolean enable) {
        // Validating given value
        if (enable == null) {
            enable = mAutoRecord;
            if (enable == null) {
                return false;
            }
        }

        boolean updating = !enable.equals(mAutoRecord)
                           && sendAutoRecord(enable);

        mAutoRecord = enable;

        mCamera.autoRecord().updateValue(enable);
        return updating;
    }

    /**
     * Applies automatic HDR enable setting value.
     * <ul>
     * <li>Gets the last received value if the given one is null;</li>
     * <li>Sends the obtained value to the drone in case it differs from the last received value;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param enable automatic HDR enable setting value to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyAutoHdr(Boolean enable) {
        // Validating given value
        if (enable == null) {
            enable = mAutoHdr;
            if (enable == null) {
                return false;
            }
        }

        boolean updating = !enable.equals(mAutoHdr)
                           && sendAutoHdr(enable);

        mAutoHdr = enable;

        mCamera.autoHdr().updateValue(enable);
        return updating;
    }

    /**
     * Applies maximum zoom speed.
     * <ul>
     * <li>Gets the last received value if the given one is null;</li>
     * <li>Sends the obtained value to the drone in case it differs from the last received value;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param speed maximum zoom speed to apply
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyMaxZoomSpeed(@Nullable Double speed) {
        // Validating given value
        if (speed == null) {
            speed = mMaxZoomSpeed;
            if (speed == null) {
                return false;
            }
        }

        boolean updating = !speed.equals(mMaxZoomSpeed)
                           && sendMaxZoomSpeed(speed);

        mMaxZoomSpeed = speed;

        mCamera.createZoomIfNeeded()
               .maxSpeed()
               .updateValue(speed);
        return updating;
    }

    /**
     * Applies zoom velocity quality degradation allowance.
     * <ul>
     * <li>Gets the last received value if the given one is null;</li>
     * <li>Sends the obtained value to the drone in case it differs from the last received value;</li>
     * <li>Updates the peripheral's setting accordingly.</li>
     * </ul>
     *
     * @param allowed {@code true} to allow quality degradation, otherwise {@code false}
     *
     * @return {@code true} if a command was sent to the device and the peripheral's setting should arm its updating
     *         flag
     */
    private boolean applyZoomQualityDegradationAllowance(@Nullable Boolean allowed) {
        // Validating given value
        if (allowed == null) {
            allowed = mZoomQualityDegradationAllowed;
            if (allowed == null) {
                return false;
            }
        }

        boolean updating = !allowed.equals(mZoomQualityDegradationAllowed)
                           && sendZoomQualityDegradationAllowance(allowed);

        mZoomQualityDegradationAllowed = allowed;

        mCamera.createZoomIfNeeded()
               .velocityQualityDegradationAllowance()
               .updateValue(allowed);
        return updating;
    }

    // endregion

    // region Validation methods {...}

    /**
     * Validates a camera mode.
     * <p>
     * In case the provided mode is {@code null}, fallbacks on the current {@link #mMode mode}. <br/>
     * The resulting value is checked to be in the current set of modes supported by the component. In case not, then it
     * fallbacks to the first mode supported by the component, if any, otherwise validation fails.
     *
     * @param mode camera mode to validate
     *
     * @return the given mode, if valid, otherwise a usable fallback value, or {@code null} in case the camera does not
     *         support any mode currently
     */
    @Nullable
    private Camera.Mode validateCameraMode(@Nullable Camera.Mode mode) {
        EnumSet<Camera.Mode> supportedModes = mCamera.mode().getAvailableValues();
        if (supportedModes.isEmpty()) {
            return null;
        }
        if (mode == null) {
            mode = mMode;
        }
        if (mode == null || !supportedModes.contains(mode)) {
            return supportedModes.iterator().next();
        }
        return mode;
    }

    /**
     * Validates a photo mode.
     * <p>
     * In case the provided photo mode is {@code null}, fallbacks on the current {@link #mPhotoMode photo mode}. <br/>
     * The resulting value is checked to be in the current set of photo modes supported by the component. In case not,
     * then it fallbacks to the first photo mode supported by the component, if any, otherwise validation fails.
     *
     * @param mode photo mode to validate
     *
     * @return the given photo mode, if valid, otherwise a usable fallback value, or {@code null} in case the camera
     *         does not support any photo mode currently
     */
    @Nullable
    private CameraPhoto.Mode validatePhotoMode(@Nullable CameraPhoto.Mode mode) {
        EnumSet<CameraPhoto.Mode> supportedModes = mCamera.photo().supportedModes();
        if (supportedModes.isEmpty()) {
            return null;
        }
        if (mode == null) {
            mode = mPhotoMode;
        }
        if (mode == null || !supportedModes.contains(mode)) {
            mode = supportedModes.iterator().next();
        }
        return mode;
    }

    /**
     * Validates a photo format.
     * <p>
     * In case the provided format is {@code null}, fallbacks on the current {@link #mFormats format for the given
     * mode}. <br/>
     * The resulting value is checked to be in the current set of formats supported by the component for the given mode.
     * In case not, then it fallbacks to the first photo mode supported by the component in the given mode, if any,
     * otherwise validation fails.
     *
     * @param mode   photo mode for which to validate the format
     * @param format photo format to validate
     *
     * @return the given photo format, if valid, otherwise a usable fallback value, or {@code null} in case the camera
     *         does not support any photo format in the given mode
     */
    @Nullable
    private CameraPhoto.Format validateFormat(@NonNull CameraPhoto.Mode mode, @Nullable CameraPhoto.Format format) {
        EnumSet<CameraPhoto.Format> supportedFormats = mCamera.photo().supportedFormatsFor(mode);
        if (supportedFormats.isEmpty()) {
            return null;
        }
        if (format == null) {
            format = mFormats.get(mode);
        }
        if (format == null || !supportedFormats.contains(format)) {
            format = supportedFormats.iterator().next();
        }
        return format;
    }

    /**
     * Validates a photo file format.
     * <p>
     * In case the provided file format is {@code null}, fallbacks on the current {@link #mFileFormats file format for
     * the given mode}. <br/>
     * The resulting value is checked to be in the current set of file formats supported by the component for the given
     * mode and format. In case not, then it fallbacks to the first file format supported by the component for the given
     * mode and format, if any, otherwise validation fails.
     *
     * @param mode       photo mode for which to validate the file format
     * @param format     photo format for which to validate the file format
     * @param fileFormat photo file format to validate
     *
     * @return the given photo file format, if valid, otherwise a usable fallback value, or {@code null} in case the
     *         camera does not support any photo file format in the given mode and format
     */
    @Nullable
    private CameraPhoto.FileFormat validateFileFormat(@NonNull CameraPhoto.Mode mode,
                                                      @NonNull CameraPhoto.Format format,
                                                      @Nullable CameraPhoto.FileFormat fileFormat) {
        EnumSet<CameraPhoto.FileFormat> supportedFileFormats =
                mCamera.photo().supportedFileFormatsFor(mode, format);
        if (supportedFileFormats.isEmpty()) {
            return null;
        }
        if (fileFormat == null) {
            fileFormat = mFileFormats.get(mode);
        }
        if (fileFormat == null || !supportedFileFormats.contains(fileFormat)) {
            fileFormat = supportedFileFormats.iterator().next();
        }
        return fileFormat;
    }

    /**
     * Validates a photo burst value.
     * <p>
     * In case the provided burst value is {@code null}, fallbacks on the current {@link #mBurst burst} if not {@code
     * null}, or else on the setting's current value. <br/>
     * If the given mode is {@link CameraPhoto.Mode#BURST}, then the resulting value is checked to be in the current set
     * of burst values supported by the component, otherwise validation fails. For any other given mode, then any given
     * value or any computed fallback will not be checked against the current set of available values and will pass
     * validation as-is.
     *
     * @param mode  photo mode for which to validate the burst value
     * @param burst photo burst value to validate
     *
     * @return the given photo burst value, if valid, or a usable fallback value in case it's {@code null}, if the given
     *         value is valid or if the given mode is not {@code BURST}, otherwise {@code null}
     */
    @Nullable
    private CameraPhoto.BurstValue validateBurst(@NonNull CameraPhoto.Mode mode,
                                                 @Nullable CameraPhoto.BurstValue burst) {
        if (burst == null) {
            if (mBurst != null) {
                return mBurst;
            }
            burst = mCamera.photo().burstValue();
        }
        if (mode == CameraPhoto.Mode.BURST && !mCamera.photo().supportedBurstValues().contains(burst)) {
            burst = null;
        }
        return burst;
    }

    /**
     * Validates a photo bracketing value.
     * <p>
     * In case the provided bracketing value is {@code null}, fallbacks on the current {@link #mBracketing bracketing}
     * if not {@code null}, or else on the setting's current value. <br/>
     * If the given mode is {@link CameraPhoto.Mode#BRACKETING}, then the resulting value is checked to be in the
     * current set of bracketing values supported by the component, otherwise validation fails. For any other given
     * mode, then any given value or any computed fallback will not be checked against the current set of available
     * values and will pass validation as-is.
     *
     * @param mode       photo mode for which to validate the bracketing value
     * @param bracketing photo bracketing value to validate
     *
     * @return the given photo bracketing value, or a usable fallback value in case it's {@code null}, if the given
     *         value is valid or if the given mode is not {@code BRACKETING}, otherwise {@code null}
     */
    @Nullable
    private CameraPhoto.BracketingValue validateBracketing(@NonNull CameraPhoto.Mode mode,
                                                           @Nullable CameraPhoto.BracketingValue bracketing) {
        if (bracketing == null) {
            if (mBracketing != null) {
                return mBracketing;
            }
            bracketing = mCamera.photo().bracketingValue();
        }
        if (mode == CameraPhoto.Mode.BRACKETING && !mCamera.photo().supportedBracketingValues().contains(bracketing)) {
            bracketing = null;
        }
        return bracketing;
    }

    /**
     * Validates a time-lapse interval.
     * <p>
     * In case the provided time-lapse interval is {@code null}, fallbacks on the current {@link #mTimelapseInterval
     * time-lapse interval} if not {@code null}, or else on the setting's current value.<br/>
     *
     * @param interval time-lapse interval to validate
     *
     * @return the given time-lapse interval, or a usable fallback value in case it's {@code null}
     */
    @NonNull
    private Double validateTimelapseInterval(@Nullable Double interval) {
        if (interval == null) {
            interval = mTimelapseInterval != null ? mTimelapseInterval : mCamera.photo().timelapseInterval();
        }
        return interval;
    }

    /**
     * Validates a GPS-lapse interval.
     * <p>
     * In case the provided GPS-lapse interval is {@code null}, fallbacks on the current {@link #mGpslapseInterval
     * GPS-lapse interval} if not {@code null}, or else on the setting's current value.<br/>
     *
     * @param interval GPS-lapse interval to validate
     *
     * @return the given GPS-lapse interval, or a usable fallback value in case it's {@code null}
     */
    @NonNull
    private Double validateGpslapseInterval(@Nullable Double interval) {
        if (interval == null) {
            interval = mGpslapseInterval != null ? mGpslapseInterval : mCamera.photo().gpslapseInterval();
        }
        return interval;
    }

    /**
     * Validates a recording mode.
     * <p>
     * In case the provided recording mode is {@code null}, fallbacks on the current {@link #mRecordingMode recording
     * mode}. <br/>
     * The resulting value is checked to be in the current set of recording modes supported by the component. In case
     * not, then it fallbacks to the first recording mode supported by the component, if any, otherwise validation
     * fails.
     *
     * @param mode recording mode to validate
     *
     * @return the given recording mode, if valid, otherwise a usable fallback value, or {@code null} in case the camera
     *         does not support any recording mode currently
     */
    @Nullable
    private CameraRecording.Mode validateRecordingMode(@Nullable CameraRecording.Mode mode) {
        EnumSet<CameraRecording.Mode> supportedModes = mCamera.recording().supportedModes();
        if (supportedModes.isEmpty()) {
            return null;
        }
        if (mode == null) {
            mode = mRecordingMode;
        }
        if (mode == null || !supportedModes.contains(mode)) {
            mode = supportedModes.iterator().next();
        }
        return mode;
    }

    /**
     * Validates a recording resolution.
     * <p>
     * In case the provided resolution is {@code null}, fallbacks on the current {@link #mResolutions resolution for the
     * given mode}. <br/>
     * The resulting value is checked to be in the current set of resolutions supported by the component for the given
     * mode. In case  not, then it fallbacks to the first recording resolution supported by the component in the given
     * mode, if any, otherwise validation fails.
     *
     * @param mode       recording mode for which to validate the resolution
     * @param resolution recording resolution to validate
     *
     * @return the given recording resolution, if valid, otherwise a usable fallback value, or {@code null} in case the
     *         camera does not support any recording resolution in the given mode
     */
    @Nullable
    private CameraRecording.Resolution validateResolution(@NonNull CameraRecording.Mode mode,
                                                          @Nullable CameraRecording.Resolution resolution) {
        EnumSet<CameraRecording.Resolution> supportedResolutions = mCamera.recording().supportedResolutionsFor(mode);
        if (supportedResolutions.isEmpty()) {
            return null;
        }
        if (resolution == null) {
            resolution = mResolutions.get(mode);
        }
        if (resolution == null || !supportedResolutions.contains(resolution)) {
            resolution = supportedResolutions.iterator().next();
        }
        return resolution;
    }

    /**
     * Validates a recording framerate.
     * <p>
     * In case the provided framerate is {@code null}, fallbacks on the current {@link #mFramerates framerate for the
     * given mode}. <br/>
     * The resulting value is checked to be in the current set of framerates supported by the component for the given
     * mode and resolution. In case  not, then it fallbacks to the first framerate supported by the component for the
     * given mode and resolution, if any, otherwise validation fails.
     *
     * @param mode       recording mode for which to validate the framerate
     * @param resolution recording resolution for which to validate the framerate
     * @param framerate  recording framerate to validate
     *
     * @return the given recording framerate, if valid, otherwise a usable fallback value, or {@code null} in case the
     *         camera does not support any recording framerate in the given mode and resolution
     */
    @Nullable
    private CameraRecording.Framerate validateFramerate(@NonNull CameraRecording.Mode mode,
                                                        @NonNull CameraRecording.Resolution resolution,
                                                        @Nullable CameraRecording.Framerate framerate) {
        EnumSet<CameraRecording.Framerate> supportedFramerates =
                mCamera.recording().supportedFrameratesFor(mode, resolution);
        if (supportedFramerates.isEmpty()) {
            return null;
        }
        if (framerate == null) {
            framerate = mFramerates.get(mode);
        }
        if (framerate == null || !supportedFramerates.contains(framerate)) {
            framerate = supportedFramerates.iterator().next();
        }
        return framerate;
    }

    /**
     * Validates a recording hyperlapse value.
     * <p>
     * In case the provided hyperlapse value is {@code null}, fallbacks on the current {@link #mHyperlapse hyperlapse}
     * if not {@code null}, or else on the setting's current value. <br/>
     * If the given mode is {@link CameraRecording.Mode#HYPERLAPSE}, then the resulting value is checked to be in the
     * current set of hyperlapse values supported by the component, otherwise validation fails. For any other given
     * mode, then any given value any computed fallback will not be checked against the current set of available values
     * and will pass validation as-is.
     *
     * @param mode       recording mode for which to validate the hyperlapse value
     * @param hyperlapse recording hyperlapse value to validate
     *
     * @return the given recording hyperlapse value, or a usable fallback value in case it's {@code null}, if the given
     *         value is valid or if the given mode is not {@code HYPERLAPSE}, otherwise {@code null}
     */
    @Nullable
    private CameraRecording.HyperlapseValue validateHyperlapse(@NonNull CameraRecording.Mode mode,
                                                               @Nullable CameraRecording.HyperlapseValue hyperlapse) {
        if (hyperlapse == null) {
            if (mHyperlapse != null) {
                return mHyperlapse;
            }
            hyperlapse = mCamera.recording().hyperlapseValue();
        }
        if (mode == CameraRecording.Mode.HYPERLAPSE
            && !mCamera.recording().supportedHyperlapseValues().contains(hyperlapse)) {
            hyperlapse = null;
        }
        return hyperlapse;
    }

    /**
     * Validates an EV compensation value.
     * <p>
     * In case the provided EV compensation value is {@code null}, fallbacks on the current {@link #mEvCompensation EV
     * compensation value}. <br/>
     * The resulting value is checked to be in the current set of EV compensation values supported by the component.
     * In case not, then it fallbacks to the first EV compensation value supported by the component, if any, otherwise
     * validation fails.
     *
     * @param ev EV compensation value to validate
     *
     * @return the given EV compensation value, if valid, otherwise a usable fallback value, or {@code null} in case the
     *         camera does not support any EV compensation value currently
     */
    @Nullable
    private CameraEvCompensation validateEvCompensation(@Nullable CameraEvCompensation ev) {
        EnumSet<CameraEvCompensation> supportedEvCompensations = mCamera.exposureCompensation().getAvailableValues();
        if (supportedEvCompensations.isEmpty()) {
            return null;
        }
        if (ev == null) {
            ev = mEvCompensation;
        }
        if (!supportedEvCompensations.contains(ev)) {
            return supportedEvCompensations.iterator().next();
        }
        return ev;
    }

    /**
     * Validates an exposure mode.
     * <p>
     * In case the provided exposure mode is {@code null}, fallbacks on the current {@link #mExposureMode exposure
     * mode}. <br/>
     * The resulting value is checked to be in the current set of exposure modes supported by the component. In case
     * not, then it fallbacks to the first exposure mode supported by the component, if any, otherwise validation fails.
     *
     * @param mode exposure mode to validate
     *
     * @return the given exposure mode, if valid, otherwise a usable fallback value, or {@code null} in case the camera
     *         does not support any exposure mode currently
     */
    @Nullable
    private CameraExposure.Mode validateExposureMode(@Nullable CameraExposure.Mode mode) {
        EnumSet<CameraExposure.Mode> supportedModes = mCamera.exposure().supportedModes();
        if (supportedModes.isEmpty()) {
            return null;
        }
        if (mode == null) {
            mode = mExposureMode;
        }
        if (!supportedModes.contains(mode)) {
            return supportedModes.iterator().next();
        }
        return mode;
    }

    /**
     * Validates a shutter speed value.
     * <p>
     * In case the provided shutter speed value is {@code null}, fallbacks on the current {@link #mShutterSpeed shutter
     * speed} if not {@code null}, or else on the setting's current value. <br/>
     * If the given mode is {@link CameraExposure.Mode#MANUAL} or {@link CameraExposure.Mode#MANUAL_SHUTTER_SPEED},
     * then the resulting value is checked to be in the current set of shutter speed values supported by the component,
     * otherwise validation fails. For any other given mode, then any given value or any computed fallback will not be
     * checked against the current set of available values and will pass validation as-is.
     *
     * @param mode         exposure mode for which to validate the shutter speed value
     * @param shutterSpeed shutter speed value to validate
     *
     * @return the given shutter speed value, or a usable fallback value in case it's {@code null}, if the given value
     *         is valid or if the given mode is neither {@code MANUAL} nor {@code MANUAL_SHUTTER_SPEED}, otherwise
     *         {@code null}
     */
    @Nullable
    private CameraExposure.ShutterSpeed validateShutterSpeed(@NonNull CameraExposure.Mode mode,
                                                             @Nullable CameraExposure.ShutterSpeed shutterSpeed) {
        if (shutterSpeed == null) {
            if (mShutterSpeed != null) {
                return mShutterSpeed;
            }
            shutterSpeed = mCamera.exposure().manualShutterSpeed();
        }
        if ((mode == CameraExposure.Mode.MANUAL || mode == CameraExposure.Mode.MANUAL_SHUTTER_SPEED)
            && !mCamera.exposure().supportedManualShutterSpeeds().contains(shutterSpeed)) {
            shutterSpeed = null;
        }
        return shutterSpeed;
    }

    /**
     * Validates a manual ISO sensitivity value.
     * <p>
     * In case the provided ISO sensitivity value is {@code null}, fallbacks on the current {@link #mIsoSensitivity ISO
     * sensitivity} if not {@code null}, or else on the setting's current value. <br/>
     * If the given mode is {@link CameraExposure.Mode#MANUAL} or {@link CameraExposure.Mode#MANUAL_ISO_SENSITIVITY},
     * then the resulting value is checked to be in the current set of ISO sensitivity values supported by the
     * component, otherwise validation fails. For any other given mode, then any given value or any computed fallback
     * will not be checked against the current set of available values and will pass validation as-is.
     *
     * @param mode           exposure mode for which to validate the ISO sensitivity value
     * @param isoSensitivity ISO sensitivity value to validate
     *
     * @return the given ISO sensitivity value, or a usable fallback value in case it's {@code null}, if the given value
     *         is valid or if the given mode is neither {@code MANUAL} nor {@code MANUAL_ISO_SENSITIVITY}, otherwise
     *         {@code null}
     */
    @Nullable
    private CameraExposure.IsoSensitivity validateIsoSensitivity(
            @NonNull CameraExposure.Mode mode, @Nullable CameraExposure.IsoSensitivity isoSensitivity) {
        if (isoSensitivity == null) {
            if (mIsoSensitivity != null) {
                return mIsoSensitivity;
            }
            isoSensitivity = mCamera.exposure().manualIsoSensitivity();
        }
        if ((mode == CameraExposure.Mode.MANUAL || mode == CameraExposure.Mode.MANUAL_ISO_SENSITIVITY)
            && !mCamera.exposure().supportedManualIsoSensitivities().contains(isoSensitivity)) {
            isoSensitivity = null;
        }
        return isoSensitivity;
    }

    /**
     * Validates a maximum ISO sensitivity value.
     * <p>
     * In case the provided maximum ISO sensitivity value is {@code null}, fallbacks on the current {@link
     * #mMaxIsoSensitivity maximum ISO sensitivity} if not {@code null}, or else on the setting's current value. <br/>
     * If the given mode is {@link CameraExposure.Mode#AUTOMATIC},
     * {@link CameraExposure.Mode#AUTOMATIC_PREFER_SHUTTER_SPEED}
     * or {@link CameraExposure.Mode#AUTOMATIC_PREFER_ISO_SENSITIVITY}, then the resulting value is checked to be in the
     * current set of maximum ISO sensitivity values supported by the component, otherwise validation fails. For any
     * other given mode, then any given value or any computed fallback will not be checked against the current set of
     * available values and will pass validation as-is.
     *
     * @param mode              exposure mode for which to validate the maximum ISO sensitivity value
     * @param maxIsoSensitivity maximum ISO sensitivity value to validate
     *
     * @return the given maximum ISO sensitivity value, or a usable fallback value in case it's {@code null}, if the
     *         given value is valid or if the given mode is neither {@code AUTOMATIC} nor {@code
     *         AUTOMATIC_PREFER_ISO_SENSITIVITY} nor {@code AUTOMATIC_PREFER_SHUTTER_SPEED}, otherwise {@code null}
     */
    @Nullable
    private CameraExposure.IsoSensitivity validateMaximumIsoSensitivity(
            @NonNull CameraExposure.Mode mode, @Nullable CameraExposure.IsoSensitivity maxIsoSensitivity) {
        if (maxIsoSensitivity == null) {
            if (mMaxIsoSensitivity != null) {
                return mMaxIsoSensitivity;
            }
            maxIsoSensitivity = mCamera.exposure().maxIsoSensitivity();
        }
        if ((mode == CameraExposure.Mode.AUTOMATIC || mode == CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED
             || mode == CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY)
            && !mCamera.exposure().supportedMaximumIsoSensitivities().contains(maxIsoSensitivity)) {
            maxIsoSensitivity = null;
        }
        return maxIsoSensitivity;
    }

    /**
     * Validates an auto exposure metering mode.
     * <p>
     * In case the provided auto exposure metering mode is {@code null}, fallbacks on the current {@link
     * #mAutoExposureMeteringMode auto exposure metering mode} if not {@code null}, or else on the setting's current
     * value. <br/> If the given mode is {@link CameraExposure.Mode#AUTOMATIC},
     * {@link CameraExposure.Mode#AUTOMATIC_PREFER_SHUTTER_SPEED}
     * or {@link CameraExposure.Mode#AUTOMATIC_PREFER_ISO_SENSITIVITY}, then the resulting value is checked to be in the
     * current set of auto exposure metering modes supported by the component, otherwise validation fails. For any
     * other given mode, then any given value or any computed fallback will not be checked against the current set of
     * available values and will pass validation as-is.
     *
     * @param mode                      exposure mode for which to validate the auto exposure metering mode
     * @param autoExposureMeteringMode  auto exposure metering mode to validate
     *
     * @return the given auto exposure metering mode, or a usable fallback value in case it's {@code null}, if the
     *         given value is valid or if the given mode is neither {@code AUTOMATIC} nor {@code
     *         AUTOMATIC_PREFER_ISO_SENSITIVITY} nor {@code AUTOMATIC_PREFER_SHUTTER_SPEED}, otherwise {@code null}
     */
    @Nullable
    private CameraExposure.AutoExposureMeteringMode validateAutoExposureMeteringMode(
            @NonNull CameraExposure.Mode mode,
            @Nullable CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
        if (autoExposureMeteringMode == null) {
            if (mAutoExposureMeteringMode != null) {
                return mAutoExposureMeteringMode;
            }
            autoExposureMeteringMode = mCamera.exposure().autoExposureMeteringMode();
        }
        if ((mode == CameraExposure.Mode.AUTOMATIC || mode == CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED
             || mode == CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY)
            && !mCamera.exposure().supportedAutoExposureMeteringModes().contains(autoExposureMeteringMode)) {
            autoExposureMeteringMode = null;
        }
        return autoExposureMeteringMode;
    }

    /**
     * Validates a white balance mode.
     * <p>
     * In case the provided white balance mode is {@code null}, fallbacks on the current {@link #mWhiteBalanceMode white
     * balance mode}. <br/>
     * The resulting value is checked to be in the current set of white balance modes supported by the component. In
     * case not, then it fallbacks to the first white balance mode supported by the component, if any, otherwise
     * validation fails.
     *
     * @param mode white balance mode to validate
     *
     * @return the given white balance mode, if valid, otherwise a usable fallback value, or {@code null} in case the
     *         camera does not support any white balance mode currently
     */
    @Nullable
    private CameraWhiteBalance.Mode validateWhiteBalanceMode(@Nullable CameraWhiteBalance.Mode mode) {
        EnumSet<CameraWhiteBalance.Mode> supportedModes = mCamera.whiteBalance().supportedModes();
        if (supportedModes.isEmpty()) {
            return null;
        }
        if (mode == null) {
            mode = mWhiteBalanceMode;
        }
        if (!supportedModes.contains(mode)) {
            return supportedModes.iterator().next();
        }
        return mode;
    }

    /**
     * Validates a white balance temperature value.
     * <p>
     * In case the provided white balance temperature is {@code null}, fallbacks on the current
     * {@link #mWhiteBalanceTemperature white balance temperature} if not {@code null}, or else on the setting's current
     * value. <br/>
     * If the given mode is {@link CameraWhiteBalance.Mode#CUSTOM}, then the resulting value is checked to be in the
     * current set of white balance temperature values supported by the component, otherwise validation fails. For any
     * other given mode, then any given value or any computed fallback will not be checked against the current set of
     * available values and will pass validation as-is.
     *
     * @param mode        white balance mode for which to validate the white balance temperature value
     * @param temperature white balance temperature value to validate
     *
     * @return the given white balance temperature value, or a usable fallback value in case it's {@code null}, if the
     *         given value is valid or if the given mode is not {@code CUSTOM}, otherwise {@code null}
     */
    @Nullable
    private CameraWhiteBalance.Temperature validateWhiteBalanceTemperature(
            @NonNull CameraWhiteBalance.Mode mode, @Nullable CameraWhiteBalance.Temperature temperature) {
        if (temperature == null) {
            if (mWhiteBalanceTemperature != null) {
                return mWhiteBalanceTemperature;
            }
            temperature = mCamera.whiteBalance().customTemperature();
        }
        if (mode == CameraWhiteBalance.Mode.CUSTOM
            && !mCamera.whiteBalance().supportedCustomTemperatures().contains(temperature)) {
            temperature = null;
        }
        return temperature;
    }

    /**
     * Validates an image style.
     * <p>
     * In case the provided style is {@code null}, fallbacks on the current {@link #mStyle style}. <br/>
     * The resulting value is checked to be in the current set of styles supported by the component. In case not, then
     * it fallbacks to the first style supported by the component, if any, otherwise validation fails.
     *
     * @param style image style to validate
     *
     * @return the given style, if valid, otherwise a usable fallback value, or {@code null} in case the camera does not
     *         support any style currently
     */
    @Nullable
    private CameraStyle.Style validateStyle(@Nullable CameraStyle.Style style) {
        EnumSet<CameraStyle.Style> supportedStyles = mCamera.style().supportedStyles();
        if (supportedStyles.isEmpty()) {
            return null;
        }
        if (style == null) {
            style = mStyle;
        }
        if (!supportedStyles.contains(style)) {
            return supportedStyles.iterator().next();
        }
        return style;
    }

    // endregion

    /** Backend of CameraCore implementation. */
    private final CameraCore.Backend mBackend = new CameraCore.Backend() {

        @Override
        public boolean setMode(@NonNull Camera.Mode mode) {
            boolean updating = applyCameraMode(mode);
            MODE_PRESET.save(mPresetDict, mMode);

            if (!updating) {
                mCamera.notifyUpdated();
            }

            return updating;
        }

        @Override
        public boolean setPhoto(@NonNull CameraPhoto.Mode mode, @Nullable CameraPhoto.Format format,
                                @Nullable CameraPhoto.FileFormat fileFormat, @Nullable CameraPhoto.BurstValue burst,
                                @Nullable CameraPhoto.BracketingValue bracketing, @Nullable Double timelapseInterval,
                                @Nullable Double gpslapseInterval) {
            boolean updating = applyPhotoSettings(mode, format, fileFormat, burst, bracketing, timelapseInterval,
                    gpslapseInterval);

            PHOTO_MODE_PRESET.save(mPresetDict, mPhotoMode);
            FORMATS_PRESET.save(mPresetDict, mFormats);
            FILE_FORMATS_PRESET.save(mPresetDict, mFileFormats);
            BURST_PRESET.save(mPresetDict, mBurst);
            BRACKETING_PRESET.save(mPresetDict, mBracketing);
            TIMELAPSE_PRESET.save(mPresetDict, mTimelapseInterval);
            GPSLAPSE_PRESET.save(mPresetDict, mGpslapseInterval);

            if (!updating) {
                mCamera.notifyUpdated();
            }

            return updating;
        }

        @Override
        public boolean setRecording(@NonNull CameraRecording.Mode mode, @Nullable CameraRecording.Resolution resolution,
                                    @Nullable CameraRecording.Framerate framerate,
                                    @Nullable CameraRecording.HyperlapseValue hyperlapse) {
            boolean updating = applyRecordingSettings(mode, resolution, framerate, hyperlapse);

            RECORDING_MODE_PRESET.save(mPresetDict, mRecordingMode);
            RESOLUTIONS_PRESET.save(mPresetDict, mResolutions);
            FRAMERATES_PRESET.save(mPresetDict, mFramerates);
            HYPERLAPSE_PRESET.save(mPresetDict, mHyperlapse);

            if (!updating) {
                mCamera.notifyUpdated();
            }

            return updating;
        }

        @Override
        public boolean setEvCompensation(@NonNull CameraEvCompensation ev) {
            boolean updating = applyEvCompensation(ev);

            EV_COMPENSATION_PRESET.save(mPresetDict, ev);

            if (!updating) {
                mCamera.notifyUpdated();
            }

            return updating;
        }

        @Override
        public boolean setExposure(@NonNull CameraExposure.Mode mode,
                                   @NonNull CameraExposure.ShutterSpeed manualShutterSpeed,
                                   @NonNull CameraExposure.IsoSensitivity manualIsoSensitivity,
                                   @NonNull CameraExposure.IsoSensitivity maxIsoSensitivity,
                                   @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
            boolean updating = applyExposureSettings(mode, manualShutterSpeed, manualIsoSensitivity, maxIsoSensitivity,
                    autoExposureMeteringMode);

            EXPOSURE_MODE_PRESET.save(mPresetDict, mode);
            SHUTTER_SPEED_PRESET.save(mPresetDict, manualShutterSpeed);
            ISO_SENSITIVITY_PRESET.save(mPresetDict, manualIsoSensitivity);
            MAX_ISO_SENSITIVITY_PRESET.save(mPresetDict, maxIsoSensitivity);
            AUTO_EXPOSURE_METERING_MODE_PRESET.save(mPresetDict, autoExposureMeteringMode);

            if (!updating) {
                mCamera.notifyUpdated();
            }

            return updating;
        }

        @Override
        public boolean setExposureLock(@NonNull CameraExposureLock.Mode mode, double centerX, double centerY) {
            mRequestedExposureLockMode = mode;
            mRequestedExposureLockCenterX = centerX;
            mRequestedExposureLockCenterY = centerY;
            return mActive && sendExposureLock(mode, centerX, centerY);
        }

        @Override
        public boolean setWhiteBalance(@NonNull CameraWhiteBalance.Mode mode,
                                       @NonNull CameraWhiteBalance.Temperature temperature) {
            boolean updating = applyWhiteBalanceSettings(mode, temperature);

            WHITE_BALANCE_MODE_PRESET.save(mPresetDict, mode);
            WHITE_BALANCE_TEMPERATURE_PRESET.save(mPresetDict, temperature);

            if (!updating) {
                mCamera.notifyUpdated();
            }

            return updating;
        }

        @Override
        public boolean setWhiteBalanceLock(boolean locked) {
            return mActive && sendWhiteBalanceLock(locked);
        }

        @Override
        public boolean setStyle(@NonNull CameraStyle.Style style) {
            boolean updating = applyStyle(style);

            STYLE_PRESET.save(mPresetDict, style);

            if (!updating) {
                mCamera.notifyUpdated();
            }

            return updating;
        }

        @Override
        public boolean setStyleParameters(int saturation, int contrast, int sharpness) {
            boolean updating = applyStyleParameters(saturation, contrast, sharpness);

            SATURATION_PRESET.save(mPresetDict, saturation);
            CONTRAST_PRESET.save(mPresetDict, contrast);
            SHARPNESS_PRESET.save(mPresetDict, sharpness);

            if (!updating) {
                mCamera.notifyUpdated();
            }

            return updating;
        }

        @Override
        public boolean setAlignment(double yaw, double pitch, double roll) {
            return mActive && sendAlignment(yaw, pitch, roll);
        }

        @Override
        public boolean resetAlignment() {
            return mActive && sendAlignmentReset();
        }

        @Override
        public boolean setAutoHdr(boolean enable) {
            boolean updating = applyAutoHdr(enable);

            AUTO_HDR_ENABLE_PRESET.save(mPresetDict, mAutoHdr);

            if (!updating) {
                mCamera.notifyUpdated();
            }

            return updating;
        }

        @Override
        public boolean setAutoRecord(boolean enable) {
            boolean updating = applyAutoRecord(enable);

            AUTO_RECORD_ENABLE_PRESET.save(mPresetDict, mAutoRecord);

            if (!updating) {
                mCamera.notifyUpdated();
            }

            return updating;
        }

        @Override
        public boolean setMaxZoomSpeed(double speed) {
            boolean updating = applyMaxZoomSpeed(speed);

            MAX_ZOOM_SPEED_PRESET.save(mPresetDict, mMaxZoomSpeed);

            if (!updating) {
                mCamera.notifyUpdated();
            }

            return updating;
        }

        @Override
        public boolean setQualityDegradationAllowance(boolean allowed) {
            boolean updating = applyZoomQualityDegradationAllowance(allowed);

            QUALITY_DEGRADATION_ALLOWANCE_PRESET.save(mPresetDict, mZoomQualityDegradationAllowed);

            if (!updating) {
                mCamera.notifyUpdated();
            }

            return updating;
        }

        @Override
        public void control(@NonNull CameraZoom.ControlMode mode, double target) {
            if (mActive) {
                mZoomController.control(mode, target);
            }
        }

        @Override
        public boolean startPhotoCapture() {
            return mActive && sendStartPhotoCapture();
        }

        @Override
        public boolean stopPhotoCapture() {
            return mActive && sendStopPhotoCapture();
        }

        @Override
        public boolean startRecording() {
            return mActive && sendStartRecording();
        }

        @Override
        public boolean stopRecording() {
            return mActive && sendStopRecording();
        }
    };
}
