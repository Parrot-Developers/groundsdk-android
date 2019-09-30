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
 * Utility class to adapt {@link ArsdkFeatureCamera.PhotoFileFormat camera feature} to {@link CameraPhoto.FileFormat
 * groundsdk} photo file formats.
 */
final class FileFormatAdapter {

    /**
     * Converts an {@code ArsdkFeatureCamera.PhotoFileFormat} to its {@code CameraPhoto.FileFormat} equivalent.
     *
     * @param fileFormat camera feature photo file format to convert
     *
     * @return the groundsdk photo file format equivalent
     */
    @NonNull
    static CameraPhoto.FileFormat from(@NonNull ArsdkFeatureCamera.PhotoFileFormat fileFormat) {
        switch (fileFormat) {
            case JPEG:
                return CameraPhoto.FileFormat.JPEG;
            case DNG:
                return CameraPhoto.FileFormat.DNG;
            case DNG_JPEG:
                return CameraPhoto.FileFormat.DNG_AND_JPEG;
        }
        return null;
    }

    /**
     * Converts a {@code CameraPhoto.FileFormat} to its {@code ArsdkFeatureCamera.PhotoFileFormat} equivalent.
     *
     * @param fileFormat groundsdk photo file format to convert
     *
     * @return the camera feature photo file format equivalent
     */
    @NonNull
    static ArsdkFeatureCamera.PhotoFileFormat from(@NonNull CameraPhoto.FileFormat fileFormat) {
        switch (fileFormat) {
            case JPEG:
                return ArsdkFeatureCamera.PhotoFileFormat.JPEG;
            case DNG:
                return ArsdkFeatureCamera.PhotoFileFormat.DNG;
            case DNG_AND_JPEG:
                return ArsdkFeatureCamera.PhotoFileFormat.DNG_JPEG;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureCamera.PhotoFileFormat} to its equivalent set
     * of {@code CameraPhoto.FileFormat}.
     *
     * @param bitfield bitfield representation of camera feature photo file formats to convert
     *
     * @return the equivalent set of groundsdk photo file formats
     */
    @NonNull
    static EnumSet<CameraPhoto.FileFormat> from(int bitfield) {
        EnumSet<CameraPhoto.FileFormat> modes = EnumSet.noneOf(CameraPhoto.FileFormat.class);
        ArsdkFeatureCamera.PhotoFileFormat.each(bitfield, arsdk -> modes.add(from(arsdk)));
        return modes;
    }

    /**
     * Private constructor for static utility class.
     */
    private FileFormatAdapter() {
    }
}
