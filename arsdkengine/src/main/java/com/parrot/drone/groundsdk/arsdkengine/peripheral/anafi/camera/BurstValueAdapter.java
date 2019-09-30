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
 * Utility class to adapt {@link ArsdkFeatureCamera.BurstValue camera feature} to {@link CameraPhoto.BurstValue
 * groundsdk} burst values.
 */
final class BurstValueAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.BurstValue} to its {@code CameraPhoto.BurstValue} equivalent.
     *
     * @param burst camera feature burst value to convert
     *
     * @return the groundsdk burst value equivalent
     */
    @NonNull
    static CameraPhoto.BurstValue from(@NonNull ArsdkFeatureCamera.BurstValue burst) {
        switch (burst) {
            case BURST_14_OVER_4S:
                return CameraPhoto.BurstValue.BURST_14_OVER_4S;
            case BURST_14_OVER_2S:
                return CameraPhoto.BurstValue.BURST_14_OVER_2S;
            case BURST_14_OVER_1S:
                return CameraPhoto.BurstValue.BURST_14_OVER_1S;
            case BURST_10_OVER_4S:
                return CameraPhoto.BurstValue.BURST_10_OVER_4S;
            case BURST_10_OVER_2S:
                return CameraPhoto.BurstValue.BURST_10_OVER_2S;
            case BURST_10_OVER_1S:
                return CameraPhoto.BurstValue.BURST_10_OVER_1S;
            case BURST_4_OVER_4S:
                return CameraPhoto.BurstValue.BURST_4_OVER_4S;
            case BURST_4_OVER_2S:
                return CameraPhoto.BurstValue.BURST_4_OVER_2S;
            case BURST_4_OVER_1S:
                return CameraPhoto.BurstValue.BURST_4_OVER_1S;
        }
        return null;
    }

    /**
     * Converts a {@code CameraPhoto.BurstValue} to its {@code ArsdkFeatureCamera.BurstValue} equivalent.
     *
     * @param burst groundsdk burst value to convert
     *
     * @return the camera feature burst value equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.BurstValue from(@NonNull CameraPhoto.BurstValue burst) {
        switch (burst) {
            case BURST_14_OVER_4S:
                return ArsdkFeatureCamera.BurstValue.BURST_14_OVER_4S;
            case BURST_14_OVER_2S:
                return ArsdkFeatureCamera.BurstValue.BURST_14_OVER_2S;
            case BURST_14_OVER_1S:
                return ArsdkFeatureCamera.BurstValue.BURST_14_OVER_1S;
            case BURST_10_OVER_4S:
                return ArsdkFeatureCamera.BurstValue.BURST_10_OVER_4S;
            case BURST_10_OVER_2S:
                return ArsdkFeatureCamera.BurstValue.BURST_10_OVER_2S;
            case BURST_10_OVER_1S:
                return ArsdkFeatureCamera.BurstValue.BURST_10_OVER_1S;
            case BURST_4_OVER_4S:
                return ArsdkFeatureCamera.BurstValue.BURST_4_OVER_4S;
            case BURST_4_OVER_2S:
                return ArsdkFeatureCamera.BurstValue.BURST_4_OVER_2S;
            case BURST_4_OVER_1S:
                return ArsdkFeatureCamera.BurstValue.BURST_4_OVER_1S;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.BurstValue} to its equivalent set of
     * {@code CameraPhoto.BurstValue}.
     *
     * @param bitfield bitfield representation of camera feature burst values to convert
     *
     * @return the equivalent set of groundsdk burst values
     */
    @NonNull
    static EnumSet<CameraPhoto.BurstValue> from(int bitfield) {
        EnumSet<CameraPhoto.BurstValue> values = EnumSet.noneOf(CameraPhoto.BurstValue.class);
        ArsdkFeatureCamera.BurstValue.each(bitfield, arsdk -> values.add(from(arsdk)));
        return values;
    }

    /**
     * Private constructor for static utility class.
     */
    private BurstValueAdapter() {
    }
}
