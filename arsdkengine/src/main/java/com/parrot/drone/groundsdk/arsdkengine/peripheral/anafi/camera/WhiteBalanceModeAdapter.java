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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraWhiteBalance;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;

import java.util.EnumSet;

/**
 * Utility class to adapt {@link ArsdkFeatureCamera.WhiteBalanceMode camera feature} to {@link CameraWhiteBalance.Mode
 * groundsdk} white balance modes.
 */
final class WhiteBalanceModeAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.WhiteBalanceMode} to its {@code CameraWhiteBalance.Mode} equivalent.
     *
     * @param mode camera feature white balance mode to convert
     *
     * @return the groundsdk white balance mode equivalent
     */
    @NonNull
    static CameraWhiteBalance.Mode from(@NonNull ArsdkFeatureCamera.WhiteBalanceMode mode) {
        switch (mode) {
            case AUTOMATIC:
                return CameraWhiteBalance.Mode.AUTOMATIC;
            case CANDLE:
                return CameraWhiteBalance.Mode.CANDLE;
            case SUNSET:
                return CameraWhiteBalance.Mode.SUNSET;
            case INCANDESCENT:
                return CameraWhiteBalance.Mode.INCANDESCENT;
            case WARM_WHITE_FLUORESCENT:
                return CameraWhiteBalance.Mode.WARM_WHITE_FLUORESCENT;
            case HALOGEN:
                return CameraWhiteBalance.Mode.HALOGEN;
            case FLUORESCENT:
                return CameraWhiteBalance.Mode.FLUORESCENT;
            case COOL_WHITE_FLUORESCENT:
                return CameraWhiteBalance.Mode.COOL_WHITE_FLUORESCENT;
            case FLASH:
                return CameraWhiteBalance.Mode.FLASH;
            case DAYLIGHT:
                return CameraWhiteBalance.Mode.DAYLIGHT;
            case SUNNY:
                return CameraWhiteBalance.Mode.SUNNY;
            case CLOUDY:
                return CameraWhiteBalance.Mode.CLOUDY;
            case SNOW:
                return CameraWhiteBalance.Mode.SNOW;
            case HAZY:
                return CameraWhiteBalance.Mode.HAZY;
            case SHADED:
                return CameraWhiteBalance.Mode.SHADED;
            case GREEN_FOLIAGE:
                return CameraWhiteBalance.Mode.GREEN_FOLIAGE;
            case BLUE_SKY:
                return CameraWhiteBalance.Mode.BLUE_SKY;
            case CUSTOM:
                return CameraWhiteBalance.Mode.CUSTOM;
        }
        return null;
    }

    /**
     * Converts a {@code CameraWhiteBalance.Mode} to its {@code ArsdkFeatureCamera.WhiteBalanceMode} equivalent.
     *
     * @param mode groundsdk white balance mode to convert
     *
     * @return the camera feature white balance mode equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.WhiteBalanceMode from(@NonNull CameraWhiteBalance.Mode mode) {
        switch (mode) {
            case AUTOMATIC:
                return ArsdkFeatureCamera.WhiteBalanceMode.AUTOMATIC;
            case CANDLE:
                return ArsdkFeatureCamera.WhiteBalanceMode.CANDLE;
            case SUNSET:
                return ArsdkFeatureCamera.WhiteBalanceMode.SUNSET;
            case INCANDESCENT:
                return ArsdkFeatureCamera.WhiteBalanceMode.INCANDESCENT;
            case WARM_WHITE_FLUORESCENT:
                return ArsdkFeatureCamera.WhiteBalanceMode.WARM_WHITE_FLUORESCENT;
            case HALOGEN:
                return ArsdkFeatureCamera.WhiteBalanceMode.HALOGEN;
            case FLUORESCENT:
                return ArsdkFeatureCamera.WhiteBalanceMode.FLUORESCENT;
            case COOL_WHITE_FLUORESCENT:
                return ArsdkFeatureCamera.WhiteBalanceMode.COOL_WHITE_FLUORESCENT;
            case FLASH:
                return ArsdkFeatureCamera.WhiteBalanceMode.FLASH;
            case DAYLIGHT:
                return ArsdkFeatureCamera.WhiteBalanceMode.DAYLIGHT;
            case SUNNY:
                return ArsdkFeatureCamera.WhiteBalanceMode.SUNNY;
            case CLOUDY:
                return ArsdkFeatureCamera.WhiteBalanceMode.CLOUDY;
            case SNOW:
                return ArsdkFeatureCamera.WhiteBalanceMode.SNOW;
            case HAZY:
                return ArsdkFeatureCamera.WhiteBalanceMode.HAZY;
            case SHADED:
                return ArsdkFeatureCamera.WhiteBalanceMode.SHADED;
            case GREEN_FOLIAGE:
                return ArsdkFeatureCamera.WhiteBalanceMode.GREEN_FOLIAGE;
            case BLUE_SKY:
                return ArsdkFeatureCamera.WhiteBalanceMode.BLUE_SKY;
            case CUSTOM:
                return ArsdkFeatureCamera.WhiteBalanceMode.CUSTOM;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.WhiteBalanceMode} to its equivalent set
     * of {@code CameraWhiteBalance.Mode}.
     *
     * @param bitfield bitfield representation of camera feature white balance modes to convert
     *
     * @return the equivalent set of groundsdk white balance modes
     */
    @NonNull
    static EnumSet<CameraWhiteBalance.Mode> from(int bitfield) {
        EnumSet<CameraWhiteBalance.Mode> modes = EnumSet.noneOf(CameraWhiteBalance.Mode.class);
        ArsdkFeatureCamera.WhiteBalanceMode.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private WhiteBalanceModeAdapter() {
    }
}
