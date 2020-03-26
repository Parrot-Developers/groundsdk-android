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

package com.parrot.drone.groundsdk.device.peripheral.camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.value.EnumSetting;
import com.parrot.drone.groundsdk.value.OptionalBooleanSetting;

/**
 * Base camera interface.
 * <p>
 * Provides control of drone camera in order to take pictures and to record videos. <br>
 * Also provides access to various camera settings, such as:
 * <ul>
 * <li>Exposure,</li>
 * <li>EV compensation,</li>
 * <li>White balance,</li>
 * <li>Zoom,</li>
 * <li>Recording mode, resolution and framerate selection,</li>
 * <li>Photo mode, format and file format selection.</li>
 * </ul>
 */
public interface Camera {

    /** Camera operating mode. */
    enum Mode {

        /**
         * Camera mode that is best suited to record videos.
         * <p>
         * Note that, depending on the device, it may also be possible to {@link #startPhotoCapture()} take photos}
         * while in this mode.
         */
        RECORDING,

        /**
         * Camera mode that is best suited to take photos.
         */
        PHOTO,
    }

    /**
     * Tells whether the camera is active or not.
     *
     * @return {@code true} if this camera is active, otherwise {@code false}
     */
    boolean isActive();

    /**
     * Gives access to the camera mode setting.
     * <p>
     * This setting allows to change the camera's current operating mode.
     * <p>
     * This setting remains available when the drone is not connected.
     *
     * @return camera mode setting
     */
    @NonNull
    EnumSetting<Mode> mode();

    /**
     * Gives access to the camera exposure setting.
     * <p>
     * This setting allows to configure the camera's current {@link CameraExposure.Mode exposure mode},
     * {@link CameraExposure.ShutterSpeed shutter speed} and {@link CameraExposure.IsoSensitivity iso sensitivity}
     * parameters.
     * <p>
     * This setting remains available when the drone is not connected.
     *
     * @return camera exposure settings
     */
    @NonNull
    CameraExposure.Setting exposure();

    /**
     * Gives access to the camera exposure lock setting.
     * <p>
     * This setting allows to configure the camera's current {@link CameraExposureLock.Mode exposure lock mode}.
     * <p>
     * This setting is only available when the drone is connected.
     *
     * @return camera exposure lock setting, or {@code null} if exposure lock is not available or when the drone is not
     *         connected.
     */
    @Nullable
    CameraExposureLock exposureLock();

    /**
     * Gives access to the camera EV (Exposure Value) compensation setting.
     * <p>
     * This setting allows to configure the camera's current {@link CameraEvCompensation ev compensation} when current
     * exposure mode in not {@link CameraExposure.Mode#MANUAL}.
     * <p>
     * This setting is not available when: <ul>
     * <li>{@link CameraExposure.Setting#mode() exposure mode} is {@link CameraExposure.Mode#MANUAL},</li>
     * <li>{@link #exposureLock() exposure lock} is active ({@link CameraExposureLock#mode() mode} is different from
     * {@link CameraExposureLock.Mode#NONE}).
     * </li>
     * </ul>
     * This setting remains available when the drone is not connected.
     *
     * @return camera EV compensation setting
     */
    @NonNull
    EnumSetting<CameraEvCompensation> exposureCompensation();

    /**
     * Gives access to the camera white balance setting.
     * <p>
     * This setting allows to configure the camera's current {@link CameraWhiteBalance.Mode white balance mode} and
     * {@link CameraWhiteBalance.Temperature temperature} parameters.
     * <p>
     * This setting remains available when the drone is not connected.
     *
     * @return camera white balance setting
     */
    @NonNull
    CameraWhiteBalance.Setting whiteBalance();

    /**
     * Gives access to the camera white balance lock setting.
     * <p>
     * This setting allows to configure the camera's current
     * {@link CameraWhiteBalanceLock#isLocked()} white balance lock}.
     * <p>
     * This setting is only available when the drone is connected.
     *
     * @return camera white balance lock setting, or {@code null} if white balance lock is not supported or when the
     *         drone is not connected
     */
    @Nullable
    CameraWhiteBalanceLock whiteBalanceLock();

    /**
     * Gives access to the camera automatic High Dynamic Range (HDR) setting.
     * <p>
     * Tells if HDR must be activated automatically when available in current recording/photo setting.
     * <p>
     * HDR may be unsupported depending on the camera. Hence, clients of this API should call
     * {@link OptionalBooleanSetting#isAvailable() isAvailable} method on the returned value to check whether it can be
     * considered valid before use.
     * <p>
     * This setting remains available when the drone is not connected.
     *
     * @return HDR setting
     */
    @NonNull
    OptionalBooleanSetting autoHdr();

    /**
     * Gives access to the camera image style setting.
     * <p>
     * This setting allows to select and configure the camera's current {@link CameraStyle.Style image style}.
     * <p>
     * This setting remains available when the drone is not connected.
     *
     * @return camera image style setting
     */
    @NonNull
    CameraStyle.Setting style();

    /**
     * Gives access to the camera alignment setting.
     * <p>
     * This setting allows to set camera alignment offsets applied to each axis.
     * <p>
     * This setting is only available when the drone is connected.
     *
     * @return camera alignment setting, or {@code null} if alignment is not supported or when the drone is not
     *         connected
     */
    @Nullable
    CameraAlignment.Setting alignment();

    /**
     * Retrieves the camera zoom sub-peripheral.
     *
     * @return the camera zoom, or {@code null} if the camera doesn't support this feature
     */
    @Nullable
    CameraZoom zoom();

    /**
     * Gives access to the camera photo mode setting.
     * <p>
     * This setting allows to configure the camera's photo {@link CameraPhoto.Mode mode},
     * {@link CameraPhoto.Format format} and {@link CameraPhoto.FileFormat file format} parameters.
     * <p>
     * This setting remains available when the drone is not connected.
     *
     * @return camera photo mode setting
     */
    @NonNull
    CameraPhoto.Setting photo();

    /**
     * Retrieves the current state of the photo function.
     *
     * @return current photo function state
     */
    @NonNull
    CameraPhoto.State photoState();

    /**
     * Gives access to the camera recording mode setting.
     * <p>
     * This setting allows to configure the camera's recording {@link CameraRecording.Mode mode},
     * {@link CameraRecording.Resolution resolution} and {@link CameraRecording.Framerate framerate} parameters.
     * <p>
     * This setting remains available when the drone is not connected.
     *
     * @return camera recording mode setting
     */
    @NonNull
    CameraRecording.Setting recording();

    /**
     * Gives access to the auto-record setting.
     * <p>
     * When auto-record is enabled, if the drone is in recording mode, recording starts when taking-off and stops after
     * landing.
     * <p>
     * Auto-record may be unsupported depending on the camera. Hence, clients of this API should call
     * {@link OptionalBooleanSetting#isAvailable() isAvailable} method on the returned value to check whether it can be
     * considered valid before use.
     * <p>
     * This setting remains available when the drone is not connected.
     *
     * @return auto-record setting
     */
    @NonNull
    OptionalBooleanSetting autoRecord();

    /**
     * Retrieves the current state of the recording function.
     *
     * @return current recording function state
     */
    @NonNull
    CameraRecording.State recordingState();

    /**
     * Tells if HDR is currently active.
     *
     * @return {@code true} if HDR is currently active, otherwise {@code false}
     */
    boolean isHdrActive();

    /**
     * Tells if HDR is available in the current mode and configuration.
     *
     * @return {@code true} if HDR is available, otherwise {@code false}
     */
    boolean isHdrAvailable();

    /**
     * Tells whether it is currently possible to start photo(s) capture.
     *
     * @return {@code true} if {@link CameraPhoto.State#get() photo function state} is
     *         {@link CameraPhoto.State.FunctionState#STOPPED ready}, otherwise {@code false}
     */
    boolean canStartPhotoCapture();

    /**
     * Requests photo capture to start.
     * <p>
     * This method does nothing if {@link #canStartPhotoCapture()} returns {@code false}.
     */
    void startPhotoCapture();

    /**
     * Tells whether it is currently possible to stop photo(s) capture.
     *
     * @return {@code true} if {@link CameraPhoto.State#get() photo function state} is
     *         {@link CameraPhoto.State.FunctionState#STARTED taking photos}, and
     *         {@link CameraPhoto.Setting#mode() photo mode} is {@link CameraPhoto.Mode#TIME_LAPSE time-lapse}
     *         or {@link CameraPhoto.Mode#GPS_LAPSE GPS-lapse}, otherwise {@code false}
     */
    boolean canStopPhotoCapture();

    /**
     * Requests photo capture to stop.
     * <p>
     * This method does nothing if {@link #canStopPhotoCapture()} returns {@code false}.
     */
    void stopPhotoCapture();

    /**
     * Tells whether it is currently possible to start recording a video.
     *
     * @return {@code true} if it is currently possible to start recording a video, otherwise {@code false}
     */
    boolean canStartRecording();

    /**
     * Requests video recording to start.
     * <p>
     * This method does nothing unless {@link CameraRecording.State#get()  recording function state} is
     * {@link CameraRecording.State.FunctionState#STOPPED stopped}.
     */
    void startRecording();

    /**
     * Tells whether it is currently possible to stop recording a video.
     *
     * @return {@code true} if it is currently possible to stop recording a video, otherwise {@code false}
     */
    boolean canStopRecording();

    /**
     * Requests video recording to stop.
     * <p>
     * This method does nothing unless {@link CameraRecording.State#get() recording function state} is
     * {@link CameraRecording.State.FunctionState#STARTING starting} or
     * {@link CameraRecording.State.FunctionState#STARTING started}.
     */
    void stopRecording();
}
