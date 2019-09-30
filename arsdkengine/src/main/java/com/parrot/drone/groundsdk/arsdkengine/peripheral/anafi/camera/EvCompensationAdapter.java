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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraEvCompensation;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;

import java.util.EnumSet;

/**
 * Utility class to adapt {@link ArsdkFeatureCamera.EvCompensation camera feature} to {@link CameraEvCompensation
 * groundsdk} exposure compensation values.
 */
final class EvCompensationAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.EvCompensation} to its {@code CameraExposure.Value} equivalent.
     *
     * @param ev camera feature exposure compensation value to convert
     *
     * @return the groundsdk exposure compensation value equivalent
     */
    @NonNull
    static CameraEvCompensation from(@NonNull ArsdkFeatureCamera.EvCompensation ev) {
        switch (ev) {
            case EV_MINUS_3_00:
                return CameraEvCompensation.EV_MINUS_3;
            case EV_MINUS_2_67:
                return CameraEvCompensation.EV_MINUS_2_67;
            case EV_MINUS_2_33:
                return CameraEvCompensation.EV_MINUS_2_33;
            case EV_MINUS_2_00:
                return CameraEvCompensation.EV_MINUS_2;
            case EV_MINUS_1_67:
                return CameraEvCompensation.EV_MINUS_1_67;
            case EV_MINUS_1_33:
                return CameraEvCompensation.EV_MINUS_1_33;
            case EV_MINUS_1_00:
                return CameraEvCompensation.EV_MINUS_1;
            case EV_MINUS_0_67:
                return CameraEvCompensation.EV_MINUS_0_67;
            case EV_MINUS_0_33:
                return CameraEvCompensation.EV_MINUS_0_33;
            case EV_0_00:
                return CameraEvCompensation.EV_0;
            case EV_0_33:
                return CameraEvCompensation.EV_0_33;
            case EV_0_67:
                return CameraEvCompensation.EV_0_67;
            case EV_1_00:
                return CameraEvCompensation.EV_1;
            case EV_1_33:
                return CameraEvCompensation.EV_1_33;
            case EV_1_67:
                return CameraEvCompensation.EV_1_67;
            case EV_2_00:
                return CameraEvCompensation.EV_2;
            case EV_2_33:
                return CameraEvCompensation.EV_2_33;
            case EV_2_67:
                return CameraEvCompensation.EV_2_67;
            case EV_3_00:
                return CameraEvCompensation.EV_3;
        }
        return null;
    }

    /**
     * Converts a {@code CameraExposure.Value} to its {@code ArsdkFeatureCamera.EvCompensation} equivalent.
     *
     * @param ev groundsdk exposure compensation value to convert
     *
     * @return the camera feature exposure compensation value equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.EvCompensation from(@NonNull CameraEvCompensation ev) {
        switch (ev) {
            case EV_MINUS_3:
                return ArsdkFeatureCamera.EvCompensation.EV_MINUS_3_00;
            case EV_MINUS_2_67:
                return ArsdkFeatureCamera.EvCompensation.EV_MINUS_2_67;
            case EV_MINUS_2_33:
                return ArsdkFeatureCamera.EvCompensation.EV_MINUS_2_33;
            case EV_MINUS_2:
                return ArsdkFeatureCamera.EvCompensation.EV_MINUS_2_00;
            case EV_MINUS_1_67:
                return ArsdkFeatureCamera.EvCompensation.EV_MINUS_1_67;
            case EV_MINUS_1_33:
                return ArsdkFeatureCamera.EvCompensation.EV_MINUS_1_33;
            case EV_MINUS_1:
                return ArsdkFeatureCamera.EvCompensation.EV_MINUS_1_00;
            case EV_MINUS_0_67:
                return ArsdkFeatureCamera.EvCompensation.EV_MINUS_0_67;
            case EV_MINUS_0_33:
                return ArsdkFeatureCamera.EvCompensation.EV_MINUS_0_33;
            case EV_0:
                return ArsdkFeatureCamera.EvCompensation.EV_0_00;
            case EV_0_33:
                return ArsdkFeatureCamera.EvCompensation.EV_0_33;
            case EV_0_67:
                return ArsdkFeatureCamera.EvCompensation.EV_0_67;
            case EV_1:
                return ArsdkFeatureCamera.EvCompensation.EV_1_00;
            case EV_1_33:
                return ArsdkFeatureCamera.EvCompensation.EV_1_33;
            case EV_1_67:
                return ArsdkFeatureCamera.EvCompensation.EV_1_67;
            case EV_2:
                return ArsdkFeatureCamera.EvCompensation.EV_2_00;
            case EV_2_33:
                return ArsdkFeatureCamera.EvCompensation.EV_2_33;
            case EV_2_67:
                return ArsdkFeatureCamera.EvCompensation.EV_2_67;
            case EV_3:
                return ArsdkFeatureCamera.EvCompensation.EV_3_00;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.EvCompensation} to its equivalent set of
     * {@code CameraExposure.Value}.
     *
     * @param bitfield bitfield representation of camera feature exposure compensation values to convert
     *
     * @return the equivalent set of groundsdk exposure compensation values
     */
    @NonNull
    static EnumSet<CameraEvCompensation> from(int bitfield) {
        EnumSet<CameraEvCompensation> values = EnumSet.noneOf(CameraEvCompensation.class);
        ArsdkFeatureCamera.EvCompensation.each(bitfield, arsdk -> values.add(from(arsdk)));
        return values;
    }

    /**
     * Private constructor for static utility class.
     */
    private EvCompensationAdapter() {
    }
}
