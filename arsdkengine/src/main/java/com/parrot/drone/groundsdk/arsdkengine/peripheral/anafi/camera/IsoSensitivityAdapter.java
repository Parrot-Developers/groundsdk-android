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
 * Utility class to adapt {@link ArsdkFeatureCamera.IsoSensitivity camera feature} to {@link
 * CameraExposure.IsoSensitivity groundsdk} ISO sensitivities.
 */
public final class IsoSensitivityAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.IsoSensitivity} to its {@code CameraExposure.IsoSensitivity} equivalent.
     *
     * @param iso camera feature ISO sensitivity to convert
     *
     * @return the groundsdk ISO sensitivity equivalent
     */
    @NonNull
    public static CameraExposure.IsoSensitivity from(@NonNull ArsdkFeatureCamera.IsoSensitivity iso) {
        switch (iso) {
            case ISO_50:
                return CameraExposure.IsoSensitivity.ISO_50;
            case ISO_64:
                return CameraExposure.IsoSensitivity.ISO_64;
            case ISO_80:
                return CameraExposure.IsoSensitivity.ISO_80;
            case ISO_100:
                return CameraExposure.IsoSensitivity.ISO_100;
            case ISO_125:
                return CameraExposure.IsoSensitivity.ISO_125;
            case ISO_160:
                return CameraExposure.IsoSensitivity.ISO_160;
            case ISO_200:
                return CameraExposure.IsoSensitivity.ISO_200;
            case ISO_250:
                return CameraExposure.IsoSensitivity.ISO_250;
            case ISO_320:
                return CameraExposure.IsoSensitivity.ISO_320;
            case ISO_400:
                return CameraExposure.IsoSensitivity.ISO_400;
            case ISO_500:
                return CameraExposure.IsoSensitivity.ISO_500;
            case ISO_640:
                return CameraExposure.IsoSensitivity.ISO_640;
            case ISO_800:
                return CameraExposure.IsoSensitivity.ISO_800;
            case ISO_1200:
                return CameraExposure.IsoSensitivity.ISO_1200;
            case ISO_1600:
                return CameraExposure.IsoSensitivity.ISO_1600;
            case ISO_2500:
                return CameraExposure.IsoSensitivity.ISO_2500;
            case ISO_3200:
                return CameraExposure.IsoSensitivity.ISO_3200;
        }
        return null;
    }

    /**
     * Converts a {@code ArsdkFeatureCamera.IsoSensitivity} to its {@code ArsdkFeatureCamera.IsoSensitivity}
     * equivalent.
     *
     * @param iso groundsdk ISO sensitivity to convert
     *
     * @return the camera feature ISO sensitivity equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.IsoSensitivity from(@NonNull CameraExposure.IsoSensitivity iso) {
        switch (iso) {
            case ISO_50:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_50;
            case ISO_64:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_64;
            case ISO_80:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_80;
            case ISO_100:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_100;
            case ISO_125:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_125;
            case ISO_160:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_160;
            case ISO_200:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_200;
            case ISO_250:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_250;
            case ISO_320:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_320;
            case ISO_400:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_400;
            case ISO_500:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_500;
            case ISO_640:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_640;
            case ISO_800:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_800;
            case ISO_1200:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_1200;
            case ISO_1600:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_1600;
            case ISO_2500:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_2500;
            case ISO_3200:
                return ArsdkFeatureCamera.IsoSensitivity.ISO_3200;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.IsoSensitivity} to its equivalent set of
     * {@code CameraExposure.IsoSensitivity}.
     *
     * @param bitfield bitfield representation of camera feature ISO sensitivities to convert
     *
     * @return the equivalent set of groundsdk ISO sensitivities
     */
    @NonNull
    static EnumSet<CameraExposure.IsoSensitivity> from(int bitfield) {
        EnumSet<CameraExposure.IsoSensitivity> isos = EnumSet.noneOf(CameraExposure.IsoSensitivity.class);
        ArsdkFeatureCamera.IsoSensitivity.each(bitfield, arsdk -> isos.add(from(arsdk)));
        return isos;
    }

    /**
     * Private constructor for static utility class.
     */
    private IsoSensitivityAdapter() {
    }
}
