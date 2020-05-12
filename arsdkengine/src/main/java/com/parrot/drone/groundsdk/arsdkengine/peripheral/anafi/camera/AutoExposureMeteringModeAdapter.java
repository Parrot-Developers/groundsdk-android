/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposure;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;

import java.util.EnumSet;

import androidx.annotation.NonNull;

/**
 * Utility class to adapt {@link ArsdkFeatureCamera.AutoExposureMeteringMode camera feature} to
 * {@link CameraExposure.AutoExposureMeteringMode groundsdk} auto exposure metering modes.
 */
final class AutoExposureMeteringModeAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.AutoExposureMeteringMode} to its
     * {@code CameraExposure.AutoExposureMeteringMode} equivalent.
     *
     * @param autoExposureMeteringMode camera feature auto exposure metering mode to convert
     *
     * @return the groundsdk auto exposure metering mode equivalent
     */
    @NonNull
    static CameraExposure.AutoExposureMeteringMode from(
            @NonNull ArsdkFeatureCamera.AutoExposureMeteringMode autoExposureMeteringMode) {
        switch (autoExposureMeteringMode) {
            case STANDARD:
                return CameraExposure.AutoExposureMeteringMode.STANDARD;
            case CENTER_TOP:
                return CameraExposure.AutoExposureMeteringMode.CENTER_TOP;
        }
        return null;
    }

    /**
     * Converts a {@code CameraExposure.AutoExposureMeteringMode} to its
     * {@code ArsdkFeatureCamera.AutoExposureMeteringMode} equivalent.
     *
     * @param autoExposureMeteringMode groundsdk auto exposure metering mode to convert
     *
     * @return the camera feature auto exposure metering mode equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.AutoExposureMeteringMode from(
            @NonNull CameraExposure.AutoExposureMeteringMode autoExposureMeteringMode) {
        switch (autoExposureMeteringMode) {
            case STANDARD:
                return ArsdkFeatureCamera.AutoExposureMeteringMode.STANDARD;
            case CENTER_TOP:
                return ArsdkFeatureCamera.AutoExposureMeteringMode.CENTER_TOP;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.AutoExposureMeteringMode}
     * to its equivalent set of {@code CameraExposure.AutoExposureMeteringMode}.
     *
     * @param bitfield bitfield representation of camera feature auto exposure metering modes to convert
     *
     * @return the equivalent set of groundsdk auto exposure metering modes
     */
    @NonNull
    static EnumSet<CameraExposure.AutoExposureMeteringMode> from(int bitfield) {
        EnumSet<CameraExposure.AutoExposureMeteringMode> modes =
                EnumSet.noneOf(CameraExposure.AutoExposureMeteringMode.class);
        ArsdkFeatureCamera.AutoExposureMeteringMode.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private AutoExposureMeteringModeAdapter() {
    }
}
