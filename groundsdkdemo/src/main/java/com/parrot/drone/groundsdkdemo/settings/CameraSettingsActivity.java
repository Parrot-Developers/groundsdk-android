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

package com.parrot.drone.groundsdkdemo.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.camera.Camera;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraAlignment;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraEvCompensation;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposure;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposureLock;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraRecording;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraStyle;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalance;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalanceLock;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraZoom;
import com.parrot.drone.groundsdk.value.DoubleRange;
import com.parrot.drone.groundsdkdemo.Extras;
import com.parrot.drone.groundsdkdemo.GroundSdkActivityBase;
import com.parrot.drone.groundsdkdemo.R;

import java.util.EnumSet;

import static com.parrot.drone.groundsdkdemo.Extras.EXTRA_DEVICE_UID;
import static com.parrot.drone.groundsdkdemo.settings.SettingViewAdapters.updateSetting;

public class CameraSettingsActivity<C extends Camera & Peripheral> extends GroundSdkActivityBase {

    private static final String EXTRA_CAMERA_CLASS = Extras.withKey("CAMERA_CLASS");

    public static <C extends Camera & Peripheral> void launch(@NonNull Context context, @NonNull Drone drone,
                                                              @NonNull Class<C> cameraClass) {
        context.startActivity(new Intent(context, CameraSettingsActivity.class)
                .putExtra(EXTRA_DEVICE_UID, drone.getUid())
                .putExtra(EXTRA_CAMERA_CLASS, cameraClass.getName()));
    }

    private MultiChoiceSettingView<Camera.Mode> mModeView;

    private MultiChoiceSettingView<CameraExposure.Mode> mExposureModeView;

    private MultiChoiceSettingView<CameraExposure.IsoSensitivity> mMaxIsoView;

    private MultiChoiceSettingView<CameraExposure.IsoSensitivity> mManualIsoView;

    private MultiChoiceSettingView<CameraExposure.ShutterSpeed> mManualShutterSpeedView;

    private MultiChoiceSettingView<CameraExposure.AutoExposureMeteringMode> mAutoExposureMeteringModeView;

    private CardView mExposureLockTitleView;

    private CardView mExposureLockCardView;

    private MultiChoiceSettingView<CameraExposureLock.Mode> mExposureLockModeView;

    private RangedSettingView mExposureLockRegionCenterXView;

    private RangedSettingView mExposureLockRegionCenterYView;

    private MultiChoiceSettingView<CameraEvCompensation> mEvCompensationView;

    private MultiChoiceSettingView<CameraWhiteBalance.Mode> mWhiteBalanceModeView;

    private MultiChoiceSettingView<CameraWhiteBalance.Temperature> mWhiteBalanceTemperatureView;

    private CardView mWhiteBalanceLockTitleView;

    private CardView mWhiteBalanceLockCardView;

    private ToggleSettingView mWhiteBalanceLockEnableView;

    private MultiChoiceSettingView<CameraPhoto.Mode> mPhotoModeView;

    private MultiChoiceSettingView<CameraPhoto.Format> mPhotoFormatView;

    private MultiChoiceSettingView<CameraPhoto.FileFormat> mPhotoFileFormatView;

    private MultiChoiceSettingView<CameraPhoto.BurstValue> mPhotoBurstView;

    private MultiChoiceSettingView<CameraPhoto.BracketingValue> mPhotoBracketingView;

    private RangedSettingView mPhotoTimelapseIntervalView;

    private RangedSettingView mPhotoGpslapseIntervalView;

    private MultiChoiceSettingView<CameraRecording.Mode> mRecordingModeView;

    private MultiChoiceSettingView<CameraRecording.Resolution> mRecordingResolutionView;

    private MultiChoiceSettingView<CameraRecording.Framerate> mRecordingFramerateView;

    private MultiChoiceSettingView<CameraRecording.HyperlapseValue> mRecordingHyperlapseView;

    private ToggleSettingView mAutoRecordView;

    private ToggleSettingView mHdrEnableView;

    private MultiChoiceSettingView<CameraStyle.Style> mStyleView;

    private RangedSettingView mStyleSaturationView;

    private RangedSettingView mStyleContrastView;

    private RangedSettingView mStyleSharpnessView;

    private CardView mAlignmentTitleView;

    private CardView mAlignmentCardView;

    private RangedSettingView mAlignmentYawView;

    private RangedSettingView mAlignmentPitchView;

    private RangedSettingView mAlignmentRollView;

    private Button mAlignmentResetButton;

    private CardView mZoomTitleView;

    private CardView mZoomCardView;

    private RangedSettingView mMaxZoomSpeedView;

    private ToggleSettingView mQualityDegradationAllowanceView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String deviceUid = getIntent().getStringExtra(EXTRA_DEVICE_UID);
        Drone drone = groundSdk().getDrone(deviceUid);
        if (drone == null) {
            finish();
            return;
        }

        Class<C> cameraClass = getCameraClass();
        if (cameraClass == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_camera_settings);

        mModeView = findViewById(R.id.mode);

        mExposureModeView = findViewById(R.id.exposure_mode);
        mMaxIsoView = findViewById(R.id.exposure_max_iso);
        mManualIsoView = findViewById(R.id.exposure_manual_iso);
        mManualShutterSpeedView = findViewById(R.id.exposure_manual_shutter);
        mAutoExposureMeteringModeView = findViewById(R.id.exposure_metering_mode);

        mExposureLockTitleView = findViewById(R.id.exposure_lock_title);
        mExposureLockCardView = findViewById(R.id.exposure_lock_card);
        mExposureLockModeView = findViewById(R.id.exposure_lock_mode);
        mExposureLockRegionCenterXView = findViewById(R.id.exposure_lock_region_center_x);
        mExposureLockRegionCenterYView = findViewById(R.id.exposure_lock_region_center_y);

        mEvCompensationView = findViewById(R.id.ev_compensation);

        mWhiteBalanceModeView = findViewById(R.id.white_balance_mode);
        mWhiteBalanceTemperatureView = findViewById(R.id.white_balance_temperature);

        mWhiteBalanceLockTitleView = findViewById(R.id.white_balance_lock_title);
        mWhiteBalanceLockCardView = findViewById(R.id.white_balance_lock_card);
        mWhiteBalanceLockEnableView = findViewById(R.id.white_balance_lock);

        mPhotoModeView = findViewById(R.id.photo_mode);
        mPhotoFormatView = findViewById(R.id.photo_format);
        mPhotoFileFormatView = findViewById(R.id.photo_file_format);
        mPhotoBurstView = findViewById(R.id.photo_burst);
        mPhotoBracketingView = findViewById(R.id.photo_bracketing);
        mPhotoTimelapseIntervalView = findViewById(R.id.photo_timelapse_interval);
        mPhotoGpslapseIntervalView = findViewById(R.id.photo_gpslapse_interval);

        mRecordingModeView = findViewById(R.id.recording_mode);
        mRecordingResolutionView = findViewById(R.id.recording_resolution);
        mRecordingFramerateView = findViewById(R.id.recording_framerate);
        mRecordingHyperlapseView = findViewById(R.id.recording_hyperlapse);

        mAutoRecordView = findViewById(R.id.auto_record);

        mHdrEnableView = findViewById(R.id.hdr);

        mStyleView = findViewById(R.id.style);
        mStyleSaturationView = findViewById(R.id.saturation);
        mStyleContrastView = findViewById(R.id.contrast);
        mStyleSharpnessView = findViewById(R.id.sharpness);

        mAlignmentTitleView = findViewById(R.id.alignment_title);
        mAlignmentCardView = findViewById(R.id.alignment_card);
        mAlignmentYawView = findViewById(R.id.alignment_yaw);
        mAlignmentPitchView = findViewById(R.id.alignment_pitch);
        mAlignmentRollView = findViewById(R.id.alignment_roll);
        mAlignmentResetButton = findViewById(R.id.btn_alignment_reset);

        mZoomTitleView = findViewById(R.id.zoom_title);
        mZoomCardView = findViewById(R.id.zoom_card);
        mMaxZoomSpeedView = findViewById(R.id.zoom_max_speed);
        mQualityDegradationAllowanceView = findViewById(R.id.zoom_quality_degradation);

        drone.getPeripheral(cameraClass, this::updateCamera);
    }

    private void updateCamera(@Nullable Camera camera) {
        if (camera == null) {
            finish();
            return;
        }

        updateSetting(mModeView, camera.mode());

        CameraExposure.Setting exposure = camera.exposure();

        mExposureModeView.setChoices(exposure.supportedModes()).setSelection(exposure.mode())
                         .setUpdating(exposure.isUpdating()).setListener(exposure::setMode);
        mMaxIsoView.setChoices(exposure.supportedMaximumIsoSensitivities())
                   .setSelection(exposure.maxIsoSensitivity()).setUpdating(exposure.isUpdating())
                   .setListener(exposure::setMaxIsoSensitivity);
        mManualIsoView.setChoices(exposure.supportedManualIsoSensitivities())
                      .setSelection(exposure.manualIsoSensitivity()).setUpdating(exposure.isUpdating())
                      .setListener(exposure::setManualIsoSensitivity);
        mManualShutterSpeedView.setChoices(exposure.supportedManualShutterSpeeds())
                               .setSelection(exposure.manualShutterSpeed()).setUpdating(exposure.isUpdating())
                               .setListener(exposure::setManualShutterSpeed);
        mAutoExposureMeteringModeView.setChoices(exposure.supportedAutoExposureMeteringModes())
                                     .setSelection(exposure.autoExposureMeteringMode())
                                     .setUpdating(exposure.isUpdating())
                                     .setListener(exposure::setAutoExposureMeteringMode);

        CameraExposureLock exposureLock = camera.exposureLock();

        if (exposureLock != null) {
            mExposureLockTitleView.setVisibility(View.VISIBLE);
            mExposureLockCardView.setVisibility(View.VISIBLE);
            updateExposureLockSettings(exposureLock);
        } else {
            mExposureLockTitleView.setVisibility(View.GONE);
            mExposureLockCardView.setVisibility(View.GONE);
        }

        updateSetting(mEvCompensationView, camera.exposureCompensation());

        CameraWhiteBalance.Setting whiteBalance = camera.whiteBalance();

        mWhiteBalanceModeView.setChoices(whiteBalance.supportedModes())
                             .setSelection(whiteBalance.mode()).setUpdating(whiteBalance.isUpdating())
                             .setListener(whiteBalance::setMode);
        mWhiteBalanceTemperatureView.setChoices(whiteBalance.supportedCustomTemperatures())
                                    .setSelection(whiteBalance.customTemperature()).setUpdating(
                whiteBalance.isUpdating())
                                    .setListener(whiteBalance::setCustomTemperature);

        CameraWhiteBalanceLock whiteBalanceLock = camera.whiteBalanceLock();

        if (whiteBalanceLock != null) {
            mWhiteBalanceLockTitleView.setVisibility(View.VISIBLE);
            mWhiteBalanceLockCardView.setVisibility(View.VISIBLE);
            boolean locked = whiteBalanceLock.isLocked();
            mWhiteBalanceLockEnableView.setAvailable(whiteBalanceLock.isLockable())
                                       .setToggled(locked)
                                       .setUpdating(whiteBalanceLock.isUpdating())
                                       .setListener(() -> whiteBalanceLock.lockCurrentValue(!locked));
        } else {
            mWhiteBalanceLockTitleView.setVisibility(View.GONE);
            mWhiteBalanceLockCardView.setVisibility(View.GONE);
        }

        CameraPhoto.Setting photo = camera.photo();

        mPhotoModeView.setChoices(photo.supportedModes()).setSelection(photo.mode()).setUpdating(photo.isUpdating())
                      .setListener(photo::setMode);
        mPhotoFormatView.setChoices(photo.supportedFormats()).setSelection(photo.format())
                        .setUpdating(photo.isUpdating()).setListener(photo::setFormat);
        mPhotoFileFormatView.setChoices(photo.supportedFileFormats()).setSelection(photo.fileFormat())
                            .setUpdating(photo.isUpdating()).setListener(photo::setFileFormat);
        mPhotoBurstView.setChoices(photo.supportedBurstValues()).setSelection(photo.burstValue())
                       .setUpdating(photo.isUpdating()).setListener(photo::setBurstValue);
        mPhotoBracketingView.setChoices(photo.supportedBracketingValues()).setSelection(photo.bracketingValue())
                            .setUpdating(photo.isUpdating()).setListener(photo::setBracketingValue);
        mPhotoTimelapseIntervalView.setValue(photo.timelapseIntervalRange().getLower(), photo.timelapseInterval(), 60)
                                   .setAvailable(photo.supportedModes().contains(CameraPhoto.Mode.TIME_LAPSE))
                                   .setUpdating(photo.isUpdating()).setListener(photo::setTimelapseInterval);
        mPhotoGpslapseIntervalView.setValue(photo.gpslapseIntervalRange().getLower(), photo.gpslapseInterval(), 50)
                                  .setAvailable(photo.supportedModes().contains(CameraPhoto.Mode.GPS_LAPSE))
                                  .setUpdating(photo.isUpdating()).setListener(photo::setGpslapseInterval);

        CameraRecording.Setting recording = camera.recording();

        mRecordingModeView.setChoices(recording.supportedModes()).setSelection(recording.mode())
                          .setUpdating(recording.isUpdating()).setListener(recording::setMode);
        mRecordingResolutionView.setChoices(recording.supportedResolutions()).setSelection(recording.resolution())
                                .setUpdating(recording.isUpdating()).setListener(recording::setResolution);
        mRecordingFramerateView.setChoices(recording.supportedFramerates()).setSelection(recording.framerate())
                               .setUpdating(recording.isUpdating()).setListener(recording::setFramerate);
        mRecordingHyperlapseView.setChoices(recording.supportedHyperlapseValues())
                                .setSelection(recording.hyperlapseValue()).setUpdating(recording.isUpdating())
                                .setListener(recording::setHyperlapseValue);

        updateSetting(mAutoRecordView, camera.autoRecord());

        updateSetting(mHdrEnableView, camera.autoHdr());

        CameraStyle.Setting style = camera.style();

        mStyleView.setChoices(style.supportedStyles()).setSelection(style.style())
                  .setUpdating(style.isUpdating()).setListener(style::setStyle);
        updateStyleSetting(mStyleSaturationView, style.saturation(), style.isUpdating());
        updateStyleSetting(mStyleContrastView, style.contrast(), style.isUpdating());
        updateStyleSetting(mStyleSharpnessView, style.sharpness(), style.isUpdating());

        CameraAlignment.Setting alignment = camera.alignment();
        if (alignment != null) {
            mAlignmentTitleView.setVisibility(View.VISIBLE);
            mAlignmentCardView.setVisibility(View.VISIBLE);
            boolean updating = alignment.isUpdating();
            DoubleRange yawRange = alignment.supportedYawRange();
            DoubleRange pitchRange = alignment.supportedPitchRange();
            DoubleRange rollRange = alignment.supportedRollRange();
            mAlignmentYawView.setAvailable(true)
                             .setValue(yawRange.getLower(), alignment.yaw(), yawRange.getUpper())
                             .setUpdating(updating)
                             .setListener(alignment::setYaw);
            mAlignmentPitchView.setAvailable(true)
                               .setValue(pitchRange.getLower(), alignment.pitch(), pitchRange.getUpper())
                               .setUpdating(updating)
                               .setListener(alignment::setPitch);
            mAlignmentRollView.setAvailable(true)
                              .setValue(rollRange.getLower(), alignment.roll(), rollRange.getUpper())
                              .setUpdating(updating)
                              .setListener(alignment::setRoll);
            mAlignmentResetButton.setOnClickListener(v -> alignment.reset());
        } else {
            mAlignmentTitleView.setVisibility(View.GONE);
            mAlignmentCardView.setVisibility(View.GONE);
        }

        CameraZoom zoom = camera.zoom();

        if (zoom != null) {
            mZoomTitleView.setVisibility(View.VISIBLE);
            mZoomCardView.setVisibility(View.VISIBLE);
            updateSetting(mMaxZoomSpeedView, zoom.maxSpeed());
            updateSetting(mQualityDegradationAllowanceView, zoom.velocityQualityDegradationAllowance());
        }
    }

    private static void updateStyleSetting(@NonNull RangedSettingView view, @NonNull CameraStyle.StyleParameter setting,
                                           boolean isUpdating) {
        view.setListener(newValue -> setting.setValue((int) newValue))
            .setValue(setting.getMin(), setting.getValue(), setting.getMax())
            .setAvailable(true)
            .setUpdating(isUpdating);
    }

    private void updateExposureLockSettings(@NonNull CameraExposureLock exposureLock) {
        mExposureLockModeView.setChoices(EnumSet.allOf(CameraExposureLock.Mode.class))
                             .setSelection(exposureLock.mode()).setUpdating(exposureLock.isUpdating())
                             .setListener(chosenMode -> {
                                 switch (chosenMode) {
                                     case NONE:
                                         exposureLock.unlock();
                                         break;
                                     case CURRENT_VALUES:
                                         exposureLock.lockCurrentValues();
                                         break;
                                     case REGION:
                                         exposureLock.lockOnRegion(mExposureLockRegionCenterXView.getValue(),
                                                 mExposureLockRegionCenterYView.getValue());
                                         break;
                                 }
                             });

        mExposureLockRegionCenterXView.setAvailable(exposureLock.mode() == CameraExposureLock.Mode.REGION)
                                      .setUpdating(exposureLock.isUpdating())
                                      .setValue(0, exposureLock.getRegionCenterX(), 1)
                                      .setListener(newValue -> {
                                          if (exposureLock.mode() == CameraExposureLock.Mode.REGION) {
                                              exposureLock.lockOnRegion(newValue,
                                                      mExposureLockRegionCenterYView.getValue());
                                          }
                                      });

        mExposureLockRegionCenterYView.setAvailable(exposureLock.mode() == CameraExposureLock.Mode.REGION)
                                      .setUpdating(exposureLock.isUpdating())
                                      .setValue(0, exposureLock.getRegionCenterY(), 1)
                                      .setListener(newValue -> {
                                          if (exposureLock.mode() == CameraExposureLock.Mode.REGION) {
                                              exposureLock.lockOnRegion(mExposureLockRegionCenterXView.getValue(),
                                                      newValue);
                                          }
                                      });
    }

    @SuppressWarnings("unchecked") // asSubclass(es) will throw if it does not conform to C
    @Nullable
    private Class<C> getCameraClass() {
        try {
            //noinspection unchecked :
            return (Class<C>) Class.forName(getIntent().getStringExtra(EXTRA_CAMERA_CLASS))
                                   .asSubclass(Camera.class)
                                   .asSubclass(Peripheral.class);
        } catch (Exception e) {
            return null;
        }
    }
}
