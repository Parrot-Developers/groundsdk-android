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

import com.parrot.drone.groundsdk.device.peripheral.AntiFlicker;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;

import java.util.EnumSet;

/**
 * Utility class to adapt {@link ArsdkFeatureCamera.AntiflickerMode camera feature} to {@link AntiFlicker.Mode
 * groundsdk} anti-flicker modes.
 */
final class AntiflickerModeAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.AntiflickerMode} to its {@code AntiFlicker.Mode} equivalent.
     *
     * @param mode camera feature anti-flicker mode to convert
     *
     * @return the groundsdk anti-flicker mode equivalent, {@code null} otherwise
     */
    @NonNull
    static AntiFlicker.Mode from(@NonNull ArsdkFeatureCamera.AntiflickerMode mode) {
        switch (mode) {
            case OFF:
                return AntiFlicker.Mode.OFF;
            case AUTO:
                return AntiFlicker.Mode.AUTO;
            case MODE_50HZ:
                return AntiFlicker.Mode.HZ_50;
            case MODE_60HZ:
                return AntiFlicker.Mode.HZ_60;
        }
        return null;
    }

    /**
     * Converts an {@code ArsdkFeatureCamera.AntiflickerMode} to its {@code AntiFlicker.Value} equivalent.
     *
     * @param mode camera feature anti-flicker mode to convert
     *
     * @return the groundsdk anti-flicker value equivalent, {@code null} otherwise
     */
    @NonNull
    static AntiFlicker.Value toValue(@NonNull ArsdkFeatureCamera.AntiflickerMode mode) {
        switch (mode) {
            case OFF:
                return AntiFlicker.Value.OFF;
            case AUTO:
                throw new IllegalArgumentException();
            case MODE_50HZ:
                return AntiFlicker.Value.HZ_50;
            case MODE_60HZ:
                return AntiFlicker.Value.HZ_60;
        }
        return null;
    }

    /**
     * Converts a {@code AntiFlicker.Mode} to its {@code ArsdkFeatureCamera.AntiflickerMode} equivalent.
     *
     * @param mode groundsdk anti-flicker mode to convert
     *
     * @return the camera feature anti-flicker mode equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.AntiflickerMode from(@NonNull AntiFlicker.Mode mode) {
        switch (mode) {
            case OFF:
                return ArsdkFeatureCamera.AntiflickerMode.OFF;
            case AUTO:
                return ArsdkFeatureCamera.AntiflickerMode.AUTO;
            case HZ_50:
                return ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ;
            case HZ_60:
                return ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.AntiflickerMode} to its equivalent set
     * of {@code AntiFlicker.Mode}.
     *
     * @param bitfield bitfield representation of camera feature anti-flicker mode to convert
     *
     * @return the equivalent set of groundsdk anti-flicker mode
     */
    @NonNull
    static EnumSet<AntiFlicker.Mode> from(int bitfield) {
        EnumSet<AntiFlicker.Mode> modes = EnumSet.noneOf(AntiFlicker.Mode.class);
        ArsdkFeatureCamera.AntiflickerMode.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private AntiflickerModeAdapter() {
    }
}
