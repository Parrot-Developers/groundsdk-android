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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.gimbal;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.peripheral.Gimbal;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGimbal;

import java.util.EnumSet;

/**
 * Utility class to adapt {@link ArsdkFeatureGimbal.Axis gimbal feature} to {@link Gimbal.Axis groundsdk} gimbal axis.
 */
final class GimbalAxisAdapter {

    /**
     * Converts a {@code ArsdkFeatureGimbal.Axis} to its {@code Gimbal.Axis} equivalent.
     *
     * @param axis gimbal feature axis to convert
     *
     * @return groundsdk gimbal axis equivalent of the given value
     */
    @NonNull
    static Gimbal.Axis from(@NonNull ArsdkFeatureGimbal.Axis axis) {
        switch (axis) {
            case YAW:
                return Gimbal.Axis.YAW;
            case PITCH:
                return Gimbal.Axis.PITCH;
            case ROLL:
                return Gimbal.Axis.ROLL;
        }
        return null;
    }

    /**
     * Converts a bitfield representation of multiple {@code ArsdkFeatureGimbal.Axis} to its equivalent set of {@code
     * Gimbal.Axis}.
     *
     * @param bitfield bitfield representation of gimbal feature axes to convert
     *
     * @return the equivalent set of groundsdk gimbal axes
     */
    @NonNull
    static EnumSet<Gimbal.Axis> from(int bitfield) {
        EnumSet<Gimbal.Axis> axes = EnumSet.noneOf(Gimbal.Axis.class);
        ArsdkFeatureGimbal.Axis.each(bitfield, axis -> axes.add(from(axis)));
        return axes;
    }

    /**
     * Private constructor for static utility class.
     */
    private GimbalAxisAdapter() {
    }
}
