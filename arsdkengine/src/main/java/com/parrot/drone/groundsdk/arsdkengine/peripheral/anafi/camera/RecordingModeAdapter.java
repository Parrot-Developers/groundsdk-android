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
 * Utility class to adapt {@link ArsdkFeatureCamera.RecordingMode camera feature} to {@link CameraRecording.Mode
 * groundsdk} recording modes.
 */
final class RecordingModeAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.RecordingMode} to its {@code CameraRecording.Mode} equivalent.
     *
     * @param mode camera feature recording mode to convert
     *
     * @return the groundsdk recording mode equivalent
     */
    @NonNull
    static CameraRecording.Mode from(@NonNull ArsdkFeatureCamera.RecordingMode mode) {
        switch (mode) {
            case STANDARD:
                return CameraRecording.Mode.STANDARD;
            case HYPERLAPSE:
                return CameraRecording.Mode.HYPERLAPSE;
            case SLOW_MOTION:
                return CameraRecording.Mode.SLOW_MOTION;
            case HIGH_FRAMERATE:
                return CameraRecording.Mode.HIGH_FRAMERATE;
        }
        return null;
    }

    /**
     * Converts a {@code CameraRecording.Mode} to its {@code ArsdkFeatureCamera.RecordingMode} equivalent.
     *
     * @param mode groundsdk recording mode to convert
     *
     * @return the camera feature recording mode equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.RecordingMode from(@NonNull CameraRecording.Mode mode) {
        switch (mode) {
            case STANDARD:
                return ArsdkFeatureCamera.RecordingMode.STANDARD;
            case HYPERLAPSE:
                return ArsdkFeatureCamera.RecordingMode.HYPERLAPSE;
            case SLOW_MOTION:
                return ArsdkFeatureCamera.RecordingMode.SLOW_MOTION;
            case HIGH_FRAMERATE:
                return ArsdkFeatureCamera.RecordingMode.HIGH_FRAMERATE;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.RecordingMode} to its equivalent set of
     * {@code CameraRecording.Mode}.
     *
     * @param bitfield bitfield representation of camera feature recording modes to convert
     *
     * @return the equivalent set of groundsdk recording modes
     */
    @NonNull
    static EnumSet<CameraRecording.Mode> from(int bitfield) {
        EnumSet<CameraRecording.Mode> modes = EnumSet.noneOf(CameraRecording.Mode.class);
        ArsdkFeatureCamera.RecordingMode.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private RecordingModeAdapter() {
    }
}
