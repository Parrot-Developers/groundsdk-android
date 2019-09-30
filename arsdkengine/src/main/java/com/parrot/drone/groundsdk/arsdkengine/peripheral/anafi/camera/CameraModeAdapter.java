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

import com.parrot.drone.groundsdk.device.peripheral.camera.Camera;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;

import java.util.EnumSet;

/**
 * Utility class to adapt {@link ArsdkFeatureCamera.CameraMode camera feature} to {@link Camera.Mode groundsdk} camera
 * modes.
 */
final class CameraModeAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.CameraMode} to its {@code Camera.Mode} equivalent.
     *
     * @param mode camera feature mode to convert
     *
     * @return the groundsdk camera mode equivalent
     */
    @NonNull
    static Camera.Mode from(@NonNull ArsdkFeatureCamera.CameraMode mode) {
        switch (mode) {
            case RECORDING:
                return Camera.Mode.RECORDING;
            case PHOTO:
                return Camera.Mode.PHOTO;
        }
        return null;
    }

    /**
     * Converts a {@code Camera.Mode} to its {@code ArsdkFeatureCamera.CameraMode} equivalent.
     *
     * @param mode groundsdk camera mode to convert
     *
     * @return camera feature mode equivalent of the given value
     */
    @NonNull
    static ArsdkFeatureCamera.CameraMode from(@NonNull Camera.Mode mode) {
        switch (mode) {
            case RECORDING:
                return ArsdkFeatureCamera.CameraMode.RECORDING;
            case PHOTO:
                return ArsdkFeatureCamera.CameraMode.PHOTO;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.CameraMode} to its equivalent set of
     * {@code Camera.Mode}.
     *
     * @param bitfield bitfield representation of camera feature modes to convert
     *
     * @return the equivalent set of groundsdk camera modes
     */
    @NonNull
    static EnumSet<Camera.Mode> from(int bitfield) {
        EnumSet<Camera.Mode> modes = EnumSet.noneOf(Camera.Mode.class);
        ArsdkFeatureCamera.CameraMode.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private CameraModeAdapter() {
    }
}
