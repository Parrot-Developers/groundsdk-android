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
 * Utility class to adapt {@link ArsdkFeatureCamera.WhiteBalanceTemperature camera feature} to {@link
 * CameraWhiteBalance.Temperature groundsdk} white balance temperatures.
 */
final class TemperatureAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.WhiteBalanceTemperature} to its {@code CameraWhiteBalance.Temperature}
     * equivalent.
     *
     * @param temperature camera feature white balance temperature to convert
     *
     * @return the groundsdk white balance temperature equivalent
     */
    @NonNull
    static CameraWhiteBalance.Temperature from(@NonNull ArsdkFeatureCamera.WhiteBalanceTemperature temperature) {
        switch (temperature) {
            case T_1500:
                return CameraWhiteBalance.Temperature.K_1500;
            case T_1750:
                return CameraWhiteBalance.Temperature.K_1750;
            case T_2000:
                return CameraWhiteBalance.Temperature.K_2000;
            case T_2250:
                return CameraWhiteBalance.Temperature.K_2250;
            case T_2500:
                return CameraWhiteBalance.Temperature.K_2500;
            case T_2750:
                return CameraWhiteBalance.Temperature.K_2750;
            case T_3000:
                return CameraWhiteBalance.Temperature.K_3000;
            case T_3250:
                return CameraWhiteBalance.Temperature.K_3250;
            case T_3500:
                return CameraWhiteBalance.Temperature.K_3500;
            case T_3750:
                return CameraWhiteBalance.Temperature.K_3750;
            case T_4000:
                return CameraWhiteBalance.Temperature.K_4000;
            case T_4250:
                return CameraWhiteBalance.Temperature.K_4250;
            case T_4500:
                return CameraWhiteBalance.Temperature.K_4500;
            case T_4750:
                return CameraWhiteBalance.Temperature.K_4750;
            case T_5000:
                return CameraWhiteBalance.Temperature.K_5000;
            case T_5250:
                return CameraWhiteBalance.Temperature.K_5250;
            case T_5500:
                return CameraWhiteBalance.Temperature.K_5500;
            case T_5750:
                return CameraWhiteBalance.Temperature.K_5750;
            case T_6000:
                return CameraWhiteBalance.Temperature.K_6000;
            case T_6250:
                return CameraWhiteBalance.Temperature.K_6250;
            case T_6500:
                return CameraWhiteBalance.Temperature.K_6500;
            case T_6750:
                return CameraWhiteBalance.Temperature.K_6750;
            case T_7000:
                return CameraWhiteBalance.Temperature.K_7000;
            case T_7250:
                return CameraWhiteBalance.Temperature.K_7250;
            case T_7500:
                return CameraWhiteBalance.Temperature.K_7500;
            case T_7750:
                return CameraWhiteBalance.Temperature.K_7750;
            case T_8000:
                return CameraWhiteBalance.Temperature.K_8000;
            case T_8250:
                return CameraWhiteBalance.Temperature.K_8250;
            case T_8500:
                return CameraWhiteBalance.Temperature.K_8500;
            case T_8750:
                return CameraWhiteBalance.Temperature.K_8750;
            case T_9000:
                return CameraWhiteBalance.Temperature.K_9000;
            case T_9250:
                return CameraWhiteBalance.Temperature.K_9250;
            case T_9500:
                return CameraWhiteBalance.Temperature.K_9500;
            case T_9750:
                return CameraWhiteBalance.Temperature.K_9750;
            case T_10000:
                return CameraWhiteBalance.Temperature.K_10000;
            case T_10250:
                return CameraWhiteBalance.Temperature.K_10250;
            case T_10500:
                return CameraWhiteBalance.Temperature.K_10500;
            case T_10750:
                return CameraWhiteBalance.Temperature.K_10750;
            case T_11000:
                return CameraWhiteBalance.Temperature.K_11000;
            case T_11250:
                return CameraWhiteBalance.Temperature.K_11250;
            case T_11500:
                return CameraWhiteBalance.Temperature.K_11500;
            case T_11750:
                return CameraWhiteBalance.Temperature.K_11750;
            case T_12000:
                return CameraWhiteBalance.Temperature.K_12000;
            case T_12250:
                return CameraWhiteBalance.Temperature.K_12250;
            case T_12500:
                return CameraWhiteBalance.Temperature.K_12500;
            case T_12750:
                return CameraWhiteBalance.Temperature.K_12750;
            case T_13000:
                return CameraWhiteBalance.Temperature.K_13000;
            case T_13250:
                return CameraWhiteBalance.Temperature.K_13250;
            case T_13500:
                return CameraWhiteBalance.Temperature.K_13500;
            case T_13750:
                return CameraWhiteBalance.Temperature.K_13750;
            case T_14000:
                return CameraWhiteBalance.Temperature.K_14000;
            case T_14250:
                return CameraWhiteBalance.Temperature.K_14250;
            case T_14500:
                return CameraWhiteBalance.Temperature.K_14500;
            case T_14750:
                return CameraWhiteBalance.Temperature.K_14750;
            case T_15000:
                return CameraWhiteBalance.Temperature.K_15000;
        }
        return null;
    }

    /**
     * Converts a {@code CameraWhiteBalance.Temperature} to its {@code ArsdkFeatureCamera.WhiteBalanceTemperature}
     * equivalent.
     *
     * @param temperature groundsdk white balance temperature to convert
     *
     * @return the camera feature white balance temperature equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.WhiteBalanceTemperature from(@NonNull CameraWhiteBalance.Temperature temperature) {
        switch (temperature) {
            case K_1500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_1500;
            case K_1750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_1750;
            case K_2000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_2000;
            case K_2250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_2250;
            case K_2500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_2500;
            case K_2750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_2750;
            case K_3000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_3000;
            case K_3250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_3250;
            case K_3500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_3500;
            case K_3750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_3750;
            case K_4000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_4000;
            case K_4250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_4250;
            case K_4500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_4500;
            case K_4750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_4750;
            case K_5000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_5000;
            case K_5250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_5250;
            case K_5500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_5500;
            case K_5750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_5750;
            case K_6000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_6000;
            case K_6250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_6250;
            case K_6500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_6500;
            case K_6750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_6750;
            case K_7000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_7000;
            case K_7250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_7250;
            case K_7500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_7500;
            case K_7750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_7750;
            case K_8000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_8000;
            case K_8250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_8250;
            case K_8500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_8500;
            case K_8750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_8750;
            case K_9000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_9000;
            case K_9250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_9250;
            case K_9500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_9500;
            case K_9750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_9750;
            case K_10000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_10000;
            case K_10250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_10250;
            case K_10500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_10500;
            case K_10750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_10750;
            case K_11000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_11000;
            case K_11250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_11250;
            case K_11500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_11500;
            case K_11750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_11750;
            case K_12000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_12000;
            case K_12250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_12250;
            case K_12500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_12500;
            case K_12750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_12750;
            case K_13000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_13000;
            case K_13250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_13250;
            case K_13500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_13500;
            case K_13750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_13750;
            case K_14000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_14000;
            case K_14250:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_14250;
            case K_14500:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_14500;
            case K_14750:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_14750;
            case K_15000:
                return ArsdkFeatureCamera.WhiteBalanceTemperature.T_15000;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.WhiteBalanceTemperature} to its
     * equivalent set of {@code CameraWhiteBalance.Temperature}.
     *
     * @param bitfield bitfield representation of camera feature white balance temperatures to convert
     *
     * @return the equivalent set of groundsdk white balance temperatures
     */
    @NonNull
    static EnumSet<CameraWhiteBalance.Temperature> from(long bitfield) {
        EnumSet<CameraWhiteBalance.Temperature> modes = EnumSet.noneOf(CameraWhiteBalance.Temperature.class);
        ArsdkFeatureCamera.WhiteBalanceTemperature.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private TemperatureAdapter() {
    }
}
