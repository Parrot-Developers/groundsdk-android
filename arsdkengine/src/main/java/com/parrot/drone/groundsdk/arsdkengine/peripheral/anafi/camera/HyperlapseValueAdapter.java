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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraRecording;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;

import java.util.EnumSet;

/**
 * Utility class to adapt {@link ArsdkFeatureCamera.HyperlapseValue camera feature} to {@link
 * CameraRecording.HyperlapseValue groundsdk} hyperlapse values.
 */
final class HyperlapseValueAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.HyperlapseValue} to its {@code CameraRecording.HyperlapseValue}
     * equivalent.
     *
     * @param hyperlapse camera feature hyperlapse value to convert
     *
     * @return the groundsdk hyperlapse value equivalent
     */
    @NonNull
    static CameraRecording.HyperlapseValue from(@NonNull ArsdkFeatureCamera.HyperlapseValue hyperlapse) {
        switch (hyperlapse) {
            case RATIO_15:
                return CameraRecording.HyperlapseValue.RATIO_15;
            case RATIO_30:
                return CameraRecording.HyperlapseValue.RATIO_30;
            case RATIO_60:
                return CameraRecording.HyperlapseValue.RATIO_60;
            case RATIO_120:
                return CameraRecording.HyperlapseValue.RATIO_120;
            case RATIO_240:
                return CameraRecording.HyperlapseValue.RATIO_240;
        }
        return null;
    }

    /**
     * Converts a {@code CameraRecording.HyperlapseValue} to its {@code ArsdkFeatureCamera.HyperlapseValue} equivalent.
     *
     * @param hyperlapse groundsdk hyperlapse value to convert
     *
     * @return the camera feature hyperlapse value equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.HyperlapseValue from(@NonNull CameraRecording.HyperlapseValue hyperlapse) {
        switch (hyperlapse) {
            case RATIO_15:
                return ArsdkFeatureCamera.HyperlapseValue.RATIO_15;
            case RATIO_30:
                return ArsdkFeatureCamera.HyperlapseValue.RATIO_30;
            case RATIO_60:
                return ArsdkFeatureCamera.HyperlapseValue.RATIO_60;
            case RATIO_120:
                return ArsdkFeatureCamera.HyperlapseValue.RATIO_120;
            case RATIO_240:
                return ArsdkFeatureCamera.HyperlapseValue.RATIO_240;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.HyperlapseValue} to its equivalent set
     * of {@code CameraRecording.HyperlapseValue}.
     *
     * @param bitfield bitfield representation of camera feature hyperlapse values to convert
     *
     * @return the equivalent set of groundsdk hyperlapse values
     */
    @NonNull
    static EnumSet<CameraRecording.HyperlapseValue> from(int bitfield) {
        EnumSet<CameraRecording.HyperlapseValue> values = EnumSet.noneOf(CameraRecording.HyperlapseValue.class);
        ArsdkFeatureCamera.HyperlapseValue.each(bitfield, arsdk -> values.add(from(arsdk)));
        return values;
    }

    /**
     * Private constructor for static utility class.
     */
    private HyperlapseValueAdapter() {
    }
}
