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
 * Utility class to adapt {@link ArsdkFeatureCamera.ShutterSpeed camera feature} to {@link CameraExposure.ShutterSpeed
 * groundsdk} shutter speeds.
 */
public final class ShutterSpeedAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.ShutterSpeed} to its {@code CameraExposure.ShutterSpeed} equivalent.
     *
     * @param speed camera feature shutter speed to convert
     *
     * @return the groundsdk shutter speed equivalent
     */
    @NonNull
    public static CameraExposure.ShutterSpeed from(@NonNull ArsdkFeatureCamera.ShutterSpeed speed) {
        switch (speed) {
            case SHUTTER_1_OVER_10000:
                return CameraExposure.ShutterSpeed.ONE_OVER_10000;
            case SHUTTER_1_OVER_8000:
                return CameraExposure.ShutterSpeed.ONE_OVER_8000;
            case SHUTTER_1_OVER_6400:
                return CameraExposure.ShutterSpeed.ONE_OVER_6400;
            case SHUTTER_1_OVER_5000:
                return CameraExposure.ShutterSpeed.ONE_OVER_5000;
            case SHUTTER_1_OVER_4000:
                return CameraExposure.ShutterSpeed.ONE_OVER_4000;
            case SHUTTER_1_OVER_3200:
                return CameraExposure.ShutterSpeed.ONE_OVER_3200;
            case SHUTTER_1_OVER_2500:
                return CameraExposure.ShutterSpeed.ONE_OVER_2500;
            case SHUTTER_1_OVER_2000:
                return CameraExposure.ShutterSpeed.ONE_OVER_2000;
            case SHUTTER_1_OVER_1600:
                return CameraExposure.ShutterSpeed.ONE_OVER_1600;
            case SHUTTER_1_OVER_1250:
                return CameraExposure.ShutterSpeed.ONE_OVER_1250;
            case SHUTTER_1_OVER_1000:
                return CameraExposure.ShutterSpeed.ONE_OVER_1000;
            case SHUTTER_1_OVER_800:
                return CameraExposure.ShutterSpeed.ONE_OVER_800;
            case SHUTTER_1_OVER_640:
                return CameraExposure.ShutterSpeed.ONE_OVER_640;
            case SHUTTER_1_OVER_500:
                return CameraExposure.ShutterSpeed.ONE_OVER_500;
            case SHUTTER_1_OVER_400:
                return CameraExposure.ShutterSpeed.ONE_OVER_400;
            case SHUTTER_1_OVER_320:
                return CameraExposure.ShutterSpeed.ONE_OVER_320;
            case SHUTTER_1_OVER_240:
                return CameraExposure.ShutterSpeed.ONE_OVER_240;
            case SHUTTER_1_OVER_200:
                return CameraExposure.ShutterSpeed.ONE_OVER_200;
            case SHUTTER_1_OVER_160:
                return CameraExposure.ShutterSpeed.ONE_OVER_160;
            case SHUTTER_1_OVER_120:
                return CameraExposure.ShutterSpeed.ONE_OVER_120;
            case SHUTTER_1_OVER_100:
                return CameraExposure.ShutterSpeed.ONE_OVER_100;
            case SHUTTER_1_OVER_80:
                return CameraExposure.ShutterSpeed.ONE_OVER_80;
            case SHUTTER_1_OVER_60:
                return CameraExposure.ShutterSpeed.ONE_OVER_60;
            case SHUTTER_1_OVER_50:
                return CameraExposure.ShutterSpeed.ONE_OVER_50;
            case SHUTTER_1_OVER_40:
                return CameraExposure.ShutterSpeed.ONE_OVER_40;
            case SHUTTER_1_OVER_30:
                return CameraExposure.ShutterSpeed.ONE_OVER_30;
            case SHUTTER_1_OVER_25:
                return CameraExposure.ShutterSpeed.ONE_OVER_25;
            case SHUTTER_1_OVER_15:
                return CameraExposure.ShutterSpeed.ONE_OVER_15;
            case SHUTTER_1_OVER_10:
                return CameraExposure.ShutterSpeed.ONE_OVER_10;
            case SHUTTER_1_OVER_8:
                return CameraExposure.ShutterSpeed.ONE_OVER_8;
            case SHUTTER_1_OVER_6:
                return CameraExposure.ShutterSpeed.ONE_OVER_6;
            case SHUTTER_1_OVER_4:
                return CameraExposure.ShutterSpeed.ONE_OVER_4;
            case SHUTTER_1_OVER_3:
                return CameraExposure.ShutterSpeed.ONE_OVER_3;
            case SHUTTER_1_OVER_2:
                return CameraExposure.ShutterSpeed.ONE_OVER_2;
            case SHUTTER_1_OVER_1_5:
                return CameraExposure.ShutterSpeed.ONE_OVER_1_5;
            case SHUTTER_1:
                return CameraExposure.ShutterSpeed.ONE;
        }
        return null;
    }

    /**
     * Converts a {@code CameraExposure.ShutterSpeed} to its {@code ArsdkFeatureCamera.ShutterSpeed} equivalent.
     *
     * @param speed groundsdk shutter speed to convert
     *
     * @return the camera feature shutter speed equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.ShutterSpeed from(@NonNull CameraExposure.ShutterSpeed speed) {
        switch (speed) {
            case ONE_OVER_10000:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10000;
            case ONE_OVER_8000:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_8000;
            case ONE_OVER_6400:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_6400;
            case ONE_OVER_5000:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_5000;
            case ONE_OVER_4000:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_4000;
            case ONE_OVER_3200:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_3200;
            case ONE_OVER_2500:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_2500;
            case ONE_OVER_2000:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_2000;
            case ONE_OVER_1600:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_1600;
            case ONE_OVER_1250:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_1250;
            case ONE_OVER_1000:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_1000;
            case ONE_OVER_800:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_800;
            case ONE_OVER_640:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_640;
            case ONE_OVER_500:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_500;
            case ONE_OVER_400:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_400;
            case ONE_OVER_320:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_320;
            case ONE_OVER_240:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_240;
            case ONE_OVER_200:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_200;
            case ONE_OVER_160:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_160;
            case ONE_OVER_120:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_120;
            case ONE_OVER_100:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_100;
            case ONE_OVER_80:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_80;
            case ONE_OVER_60:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_60;
            case ONE_OVER_50:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_50;
            case ONE_OVER_40:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_40;
            case ONE_OVER_30:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_30;
            case ONE_OVER_25:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_25;
            case ONE_OVER_15:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_15;
            case ONE_OVER_10:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_10;
            case ONE_OVER_8:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_8;
            case ONE_OVER_6:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_6;
            case ONE_OVER_4:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_4;
            case ONE_OVER_3:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_3;
            case ONE_OVER_2:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_2;
            case ONE_OVER_1_5:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_1_5;
            case ONE:
                return ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.ShutterSpeed} to its equivalent set of
     * {@code CameraExposure.ShutterSpeed}.
     *
     * @param bitfield bitfield representation of camera feature shutter speeds to convert
     *
     * @return the equivalent set of groundsdk shutter speeds
     */
    @NonNull
    static EnumSet<CameraExposure.ShutterSpeed> from(long bitfield) {
        EnumSet<CameraExposure.ShutterSpeed> speeds = EnumSet.noneOf(CameraExposure.ShutterSpeed.class);
        ArsdkFeatureCamera.ShutterSpeed.each(bitfield, arsdk -> speeds.add(from(arsdk)));
        return speeds;
    }

    /**
     * Private constructor for static utility class.
     */
    private ShutterSpeedAdapter() {
    }
}
