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
 * Utility class to adapt {@link ArsdkFeatureCamera.PhotoFormat camera feature} to {@link CameraPhoto.Format groundsdk}
 * photo formats.
 */
final class FormatAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.PhotoFormat} to its {@code CameraPhoto.Format} equivalent.
     *
     * @param format camera feature photo format to convert
     *
     * @return the groundsdk photo format equivalent
     */
    @NonNull
    static CameraPhoto.Format from(@NonNull ArsdkFeatureCamera.PhotoFormat format) {
        switch (format) {
            case FULL_FRAME:
                return CameraPhoto.Format.FULL_FRAME;
            case RECTILINEAR:
                return CameraPhoto.Format.RECTILINEAR;
        }
        return null;
    }

    /**
     * Converts a {@code CameraPhoto.Format} to its {@code ArsdkFeatureCamera.PhotoFormat} equivalent.
     *
     * @param format groundsdk photo format to convert
     *
     * @return the camera feature photo format equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.PhotoFormat from(@NonNull CameraPhoto.Format format) {
        switch (format) {
            case FULL_FRAME:
                return ArsdkFeatureCamera.PhotoFormat.FULL_FRAME;
            case LARGE:
                throw new IllegalArgumentException(); // only supported on ardrone3
            case RECTILINEAR:
                return ArsdkFeatureCamera.PhotoFormat.RECTILINEAR;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.PhotoFormat} to its equivalent set of
     * {@code CameraPhoto.Format}.
     *
     * @param bitfield bitfield representation of camera feature photo formats to convert
     *
     * @return the equivalent set of groundsdk photo formats
     */
    @NonNull
    static EnumSet<CameraPhoto.Format> from(int bitfield) {
        EnumSet<CameraPhoto.Format> modes = EnumSet.noneOf(CameraPhoto.Format.class);
        ArsdkFeatureCamera.PhotoFormat.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private FormatAdapter() {
    }
}
