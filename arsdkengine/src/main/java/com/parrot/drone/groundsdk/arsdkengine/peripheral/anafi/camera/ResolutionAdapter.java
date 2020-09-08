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
 * Utility class to adapt {@link ArsdkFeatureCamera.Resolution camera feature} to {@link CameraRecording.Resolution
 * groundsdk} recording resolutions.
 */
final class ResolutionAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.Resolution} to its {@code CameraRecording.Resolution} equivalent.
     *
     * @param resolution camera feature recording resolution to convert
     *
     * @return the groundsdk recording resolution equivalent
     */
    @NonNull
    static CameraRecording.Resolution from(@NonNull ArsdkFeatureCamera.Resolution resolution) {
        switch (resolution) {
            case RES_DCI_4K:
                return CameraRecording.Resolution.RES_DCI_4K;
            case RES_UHD_4K:
                return CameraRecording.Resolution.RES_UHD_4K;
            case RES_2_7K:
                return CameraRecording.Resolution.RES_2_7K;
            case RES_1080P:
                return CameraRecording.Resolution.RES_1080P;
            case RES_1080P_SD:
                return CameraRecording.Resolution.RES_1080P_4_3;
            case RES_720P:
                return CameraRecording.Resolution.RES_720P;
            case RES_720P_SD:
                return CameraRecording.Resolution.RES_720P_4_3;
            case RES_480P:
                return CameraRecording.Resolution.RES_480P;
            case RES_UHD_8K:
                return CameraRecording.Resolution.RES_UHD_8K;
            case RES_5K:
                return CameraRecording.Resolution.RES_5K;
        }
        return null;
    }

    /**
     * Converts a {@code CameraRecording.Resolution} to its {@code ArsdkFeatureCamera.Resolution} equivalent.
     *
     * @param resolution groundsdk recording resolution to convert
     *
     * @return the camera feature recording resolution equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.Resolution from(@NonNull CameraRecording.Resolution resolution) {
        switch (resolution) {
            case RES_DCI_4K:
                return ArsdkFeatureCamera.Resolution.RES_DCI_4K;
            case RES_UHD_4K:
                return ArsdkFeatureCamera.Resolution.RES_UHD_4K;
            case RES_2_7K:
                return ArsdkFeatureCamera.Resolution.RES_2_7K;
            case RES_1080P:
                return ArsdkFeatureCamera.Resolution.RES_1080P;
            case RES_1080P_4_3:
                return ArsdkFeatureCamera.Resolution.RES_1080P_SD;
            case RES_720P:
                return ArsdkFeatureCamera.Resolution.RES_720P;
            case RES_720P_4_3:
                return ArsdkFeatureCamera.Resolution.RES_720P_SD;
            case RES_480P:
                return ArsdkFeatureCamera.Resolution.RES_480P;
            case RES_UHD_8K:
                return ArsdkFeatureCamera.Resolution.RES_UHD_8K;
            case RES_5K:
                return ArsdkFeatureCamera.Resolution.RES_5K;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.Resolution} to its equivalent set of
     * {@code CameraRecording.Resolution}.
     *
     * @param bitfield bitfield representation of camera feature recording resolutions to convert
     *
     * @return the equivalent set of groundsdk recording resolutions
     */
    @NonNull
    static EnumSet<CameraRecording.Resolution> from(int bitfield) {
        EnumSet<CameraRecording.Resolution> modes = EnumSet.noneOf(CameraRecording.Resolution.class);
        ArsdkFeatureCamera.Resolution.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private ResolutionAdapter() {
    }
}
