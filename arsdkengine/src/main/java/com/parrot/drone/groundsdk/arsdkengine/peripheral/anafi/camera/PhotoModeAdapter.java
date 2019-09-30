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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;

import java.util.EnumSet;

/**
 * Utility class to adapt {@link ArsdkFeatureCamera.PhotoMode camera feature} to {@link CameraPhoto.Mode groundsdk}
 * photo modes.
 */
final class PhotoModeAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.PhotoMode} to its {@code CameraPhoto.Mode} equivalent.
     *
     * @param mode camera feature photo mode to convert
     *
     * @return the groundsdk photo mode equivalent
     */
    @NonNull
    static CameraPhoto.Mode from(@NonNull ArsdkFeatureCamera.PhotoMode mode) {
        switch (mode) {
            case SINGLE:
                return CameraPhoto.Mode.SINGLE;
            case BRACKETING:
                return CameraPhoto.Mode.BRACKETING;
            case BURST:
                return CameraPhoto.Mode.BURST;
            case TIME_LAPSE:
                return CameraPhoto.Mode.TIME_LAPSE;
            case GPS_LAPSE:
                return CameraPhoto.Mode.GPS_LAPSE;
        }
        return null;
    }

    /**
     * Converts a {@code CameraPhoto.Mode} to its {@code ArsdkFeatureCamera.PhotoMode} equivalent.
     *
     * @param mode groundsdk photo mode to convert
     *
     * @return the camera feature photo mode equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.PhotoMode from(@NonNull CameraPhoto.Mode mode) {
        switch (mode) {
            case SINGLE:
                return ArsdkFeatureCamera.PhotoMode.SINGLE;
            case BRACKETING:
                return ArsdkFeatureCamera.PhotoMode.BRACKETING;
            case BURST:
                return ArsdkFeatureCamera.PhotoMode.BURST;
            case TIME_LAPSE:
                return ArsdkFeatureCamera.PhotoMode.TIME_LAPSE;
            case GPS_LAPSE:
                return ArsdkFeatureCamera.PhotoMode.GPS_LAPSE;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.PhotoMode} to its equivalent set of
     * {@code CameraPhoto.Mode}.
     *
     * @param bitfield bitfield representation of camera feature photo modes to convert
     *
     * @return the equivalent set of groundsdk photo modes
     */
    @NonNull
    static EnumSet<CameraPhoto.Mode> from(int bitfield) {
        EnumSet<CameraPhoto.Mode> modes = EnumSet.noneOf(CameraPhoto.Mode.class);
        ArsdkFeatureCamera.PhotoMode.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private PhotoModeAdapter() {
    }
}
