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
 * Utility class to adapt {@link ArsdkFeatureCamera.BracketingPreset camera feature} to {@link
 * CameraPhoto.BracketingValue groundsdk } bracketing values.
 */
final class BracketingValueAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.BracketingPreset} to its {@code CameraPhoto.BracketingValue} equivalent.
     *
     * @param bracketing camera feature bracketing value to convert
     *
     * @return the groundsdk bracketing value equivalent
     */
    @NonNull
    static CameraPhoto.BracketingValue from(@NonNull ArsdkFeatureCamera.BracketingPreset bracketing) {
        switch (bracketing) {
            case PRESET_1EV:
                return CameraPhoto.BracketingValue.EV_1;
            case PRESET_2EV:
                return CameraPhoto.BracketingValue.EV_2;
            case PRESET_3EV:
                return CameraPhoto.BracketingValue.EV_3;
            case PRESET_1EV_3EV:
                return CameraPhoto.BracketingValue.EV_1_3;
            case PRESET_2EV_3EV:
                return CameraPhoto.BracketingValue.EV_2_3;
            case PRESET_1EV_2EV:
                return CameraPhoto.BracketingValue.EV_1_2;
            case PRESET_1EV_2EV_3EV:
                return CameraPhoto.BracketingValue.EV_1_2_3;
        }
        return null;
    }

    /**
     * Converts a {@code CameraPhoto.BracketingValue} to its {@code ArsdkFeatureCamera.BracketingPreset} equivalent.
     *
     * @param bracketing groundsdk bracketing value to convert
     *
     * @return the camera feature bracketing value equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.BracketingPreset from(@NonNull CameraPhoto.BracketingValue bracketing) {
        switch (bracketing) {
            case EV_1:
                return ArsdkFeatureCamera.BracketingPreset.PRESET_1EV;
            case EV_2:
                return ArsdkFeatureCamera.BracketingPreset.PRESET_2EV;
            case EV_3:
                return ArsdkFeatureCamera.BracketingPreset.PRESET_3EV;
            case EV_1_2:
                return ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV;
            case EV_1_3:
                return ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_3EV;
            case EV_2_3:
                return ArsdkFeatureCamera.BracketingPreset.PRESET_2EV_3EV;
            case EV_1_2_3:
                return ArsdkFeatureCamera.BracketingPreset.PRESET_1EV_2EV_3EV;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.BracketingPreset} to its equivalent set
     * of {@code CameraPhoto.BracketingValue}.
     *
     * @param bitfield bitfield representation of camera feature bracketing values to convert
     *
     * @return the equivalent set of groundsdk bracketing values
     */
    @NonNull
    static EnumSet<CameraPhoto.BracketingValue> from(int bitfield) {
        EnumSet<CameraPhoto.BracketingValue> values = EnumSet.noneOf(CameraPhoto.BracketingValue.class);
        ArsdkFeatureCamera.BracketingPreset.each(bitfield, arsdk -> values.add(from(arsdk)));
        return values;
    }

    /**
     * Private constructor for static utility class.
     */
    private BracketingValueAdapter() {
    }
}
