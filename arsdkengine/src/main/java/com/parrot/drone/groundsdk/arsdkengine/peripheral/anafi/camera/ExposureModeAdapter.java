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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposure;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;

import java.util.EnumSet;

/**
 * Utility class to adapt {@link ArsdkFeatureCamera.ExposureMode camera feature} to {@link CameraExposure.Mode
 * groundsdk} exposure modes.
 */
final class ExposureModeAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.ExposureMode} to its {@code CameraExposure.Mode} equivalent.
     *
     * @param mode camera feature exposure mode to convert
     *
     * @return the groundsdk exposure mode equivalent
     */
    @NonNull
    static CameraExposure.Mode from(@NonNull ArsdkFeatureCamera.ExposureMode mode) {
        switch (mode) {
            case AUTOMATIC:
                return CameraExposure.Mode.AUTOMATIC;
            case AUTOMATIC_PREFER_ISO_SENSITIVITY:
                return CameraExposure.Mode.AUTOMATIC_PREFER_ISO_SENSITIVITY;
            case AUTOMATIC_PREFER_SHUTTER_SPEED:
                return CameraExposure.Mode.AUTOMATIC_PREFER_SHUTTER_SPEED;
            case MANUAL_ISO_SENSITIVITY:
                return CameraExposure.Mode.MANUAL_ISO_SENSITIVITY;
            case MANUAL_SHUTTER_SPEED:
                return CameraExposure.Mode.MANUAL_SHUTTER_SPEED;
            case MANUAL:
                return CameraExposure.Mode.MANUAL;
        }
        return null;
    }

    /**
     * Converts a {@code CameraExposure.Mode} to its {@code ArsdkFeatureCamera.ExposureMode} equivalent.
     *
     * @param mode groundsdk exposure mode to convert
     *
     * @return the camera feature exposure mode equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.ExposureMode from(@NonNull CameraExposure.Mode mode) {
        switch (mode) {
            case AUTOMATIC:
                return ArsdkFeatureCamera.ExposureMode.AUTOMATIC;
            case AUTOMATIC_PREFER_ISO_SENSITIVITY:
                return ArsdkFeatureCamera.ExposureMode.AUTOMATIC_PREFER_ISO_SENSITIVITY;
            case AUTOMATIC_PREFER_SHUTTER_SPEED:
                return ArsdkFeatureCamera.ExposureMode.AUTOMATIC_PREFER_SHUTTER_SPEED;
            case MANUAL_ISO_SENSITIVITY:
                return ArsdkFeatureCamera.ExposureMode.MANUAL_ISO_SENSITIVITY;
            case MANUAL_SHUTTER_SPEED:
                return ArsdkFeatureCamera.ExposureMode.MANUAL_SHUTTER_SPEED;
            case MANUAL:
                return ArsdkFeatureCamera.ExposureMode.MANUAL;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.ExposureMode} to its equivalent set of
     * {@code CameraExposure.Mode}.
     *
     * @param bitfield bitfield representation of camera feature exposure modes to convert
     *
     * @return the equivalent set of groundsdk exposure modes
     */
    @NonNull
    static EnumSet<CameraExposure.Mode> from(int bitfield) {
        EnumSet<CameraExposure.Mode> modes = EnumSet.noneOf(CameraExposure.Mode.class);
        ArsdkFeatureCamera.ExposureMode.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private ExposureModeAdapter() {
    }
}
