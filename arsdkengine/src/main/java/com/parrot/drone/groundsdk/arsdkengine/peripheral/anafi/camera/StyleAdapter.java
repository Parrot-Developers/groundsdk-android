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

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraStyle;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;

import java.util.EnumSet;

/**
 * Utility class to adapt {@link ArsdkFeatureCamera.Style camera feature} to {@link CameraStyle.Style groundsdk}
 * styles.
 */
final class StyleAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.Style} to its {@code CameraStyle.Style} equivalent.
     *
     * @param style camera image style to convert
     *
     * @return the groundsdk image style equivalent
     */
    @NonNull
    static CameraStyle.Style from(@NonNull ArsdkFeatureCamera.Style style) {
        switch (style) {
            case STANDARD:
                return CameraStyle.Style.STANDARD;
            case PLOG:
                return CameraStyle.Style.PLOG;
            case INTENSE:
                return CameraStyle.Style.INTENSE;
            case PASTEL:
                return CameraStyle.Style.PASTEL;
        }
        return null;
    }

    /**
     * Converts a {@code CameraStyle.Style} to its {@code ArsdkFeatureCamera.Style} equivalent.
     *
     * @param style groundsdk style to convert
     *
     * @return the camera feature image style equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.Style from(@NonNull CameraStyle.Style style) {
        switch (style) {
            case STANDARD:
                return ArsdkFeatureCamera.Style.STANDARD;
            case PLOG:
                return ArsdkFeatureCamera.Style.PLOG;
            case INTENSE:
                return ArsdkFeatureCamera.Style.INTENSE;
            case PASTEL:
                return ArsdkFeatureCamera.Style.PASTEL;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.Style} to its equivalent set of {@code
     * CameraStyle.Style}.
     *
     * @param bitfield bitfield representation of camera feature styles to convert
     *
     * @return the equivalent set of groundsdk styles
     */
    @NonNull
    static EnumSet<CameraStyle.Style> from(int bitfield) {
        EnumSet<CameraStyle.Style> styles = EnumSet.noneOf(CameraStyle.Style.class);
        ArsdkFeatureCamera.Style.each(bitfield, arsdk -> styles.add(from(arsdk)));
        return styles;
    }

    /**
     * Private constructor for static utility class.
     */
    private StyleAdapter() {
    }
}
