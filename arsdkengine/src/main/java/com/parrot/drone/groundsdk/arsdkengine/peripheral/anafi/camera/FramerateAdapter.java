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
 * Utility class to adapt {@link ArsdkFeatureCamera.Framerate camera feature} to {@link CameraRecording.Framerate
 * groundsdk} recording framerates.
 */
final class FramerateAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.Framerate} to its {@code CameraRecording.Framerate} equivalent.
     *
     * @param framerate camera feature recording framerate to convert
     *
     * @return the groundsdk recording framerate equivalent
     */
    @NonNull
    static CameraRecording.Framerate from(@NonNull ArsdkFeatureCamera.Framerate framerate) {
        switch (framerate) {
            case FPS_8_6:
                return CameraRecording.Framerate.FPS_8_6;
            case FPS_9:
                return CameraRecording.Framerate.FPS_9;
            case FPS_10:
                return CameraRecording.Framerate.FPS_10;
            case FPS_15:
                return CameraRecording.Framerate.FPS_15;
            case FPS_20:
                return CameraRecording.Framerate.FPS_20;
            case FPS_24:
                return CameraRecording.Framerate.FPS_24;
            case FPS_25:
                return CameraRecording.Framerate.FPS_25;
            case FPS_30:
                return CameraRecording.Framerate.FPS_30;
            case FPS_48:
                return CameraRecording.Framerate.FPS_48;
            case FPS_50:
                return CameraRecording.Framerate.FPS_50;
            case FPS_60:
                return CameraRecording.Framerate.FPS_60;
            case FPS_96:
                return CameraRecording.Framerate.FPS_96;
            case FPS_100:
                return CameraRecording.Framerate.FPS_100;
            case FPS_120:
                return CameraRecording.Framerate.FPS_120;
            case FPS_192:
                return CameraRecording.Framerate.FPS_192;
            case FPS_200:
                return CameraRecording.Framerate.FPS_200;
            case FPS_240:
                return CameraRecording.Framerate.FPS_240;
        }
        return null;
    }

    /**
     * Converts a {@code CameraRecording.Framerate} to its {@code ArsdkFeatureCamera.Framerate} equivalent.
     *
     * @param framerate groundsdk recording framerate to convert
     *
     * @return the camera feature recording framerate equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.Framerate from(@NonNull CameraRecording.Framerate framerate) {
        switch (framerate) {
            case FPS_8_6:
                return ArsdkFeatureCamera.Framerate.FPS_8_6;
            case FPS_9:
                return ArsdkFeatureCamera.Framerate.FPS_9;
            case FPS_10:
                return ArsdkFeatureCamera.Framerate.FPS_10;
            case FPS_15:
                return ArsdkFeatureCamera.Framerate.FPS_15;
            case FPS_20:
                return ArsdkFeatureCamera.Framerate.FPS_20;
            case FPS_24:
                return ArsdkFeatureCamera.Framerate.FPS_24;
            case FPS_25:
                return ArsdkFeatureCamera.Framerate.FPS_25;
            case FPS_30:
                return ArsdkFeatureCamera.Framerate.FPS_30;
            case FPS_48:
                return ArsdkFeatureCamera.Framerate.FPS_48;
            case FPS_50:
                return ArsdkFeatureCamera.Framerate.FPS_50;
            case FPS_60:
                return ArsdkFeatureCamera.Framerate.FPS_60;
            case FPS_96:
                return ArsdkFeatureCamera.Framerate.FPS_96;
            case FPS_100:
                return ArsdkFeatureCamera.Framerate.FPS_100;
            case FPS_120:
                return ArsdkFeatureCamera.Framerate.FPS_120;
            case FPS_192:
                return ArsdkFeatureCamera.Framerate.FPS_192;
            case FPS_200:
                return ArsdkFeatureCamera.Framerate.FPS_200;
            case FPS_240:
                return ArsdkFeatureCamera.Framerate.FPS_240;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.Framerate} to its equivalent set of
     * {@code CameraRecording.Framerate}.
     *
     * @param bitfield bitfield representation of camera feature recording framerates to convert
     *
     * @return the equivalent set of groundsdk recording framerates
     */
    @NonNull
    static EnumSet<CameraRecording.Framerate> from(int bitfield) {
        EnumSet<CameraRecording.Framerate> modes = EnumSet.noneOf(CameraRecording.Framerate.class);
        ArsdkFeatureCamera.Framerate.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private FramerateAdapter() {
    }
}
