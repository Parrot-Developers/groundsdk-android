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

package com.parrot.drone.groundsdk.device.peripheral.camera;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.value.DoubleRange;

/**
 * Scoping class for camera alignment related types and settings.
 */
public final class CameraAlignment {

    /**
     * Camera alignment setting.
     * <p>
     * Allows to set camera alignment offsets applied to each axis.
     */
    public abstract static class Setting extends com.parrot.drone.groundsdk.value.Setting {

        /**
         * Retrieves the supported alignment offset range to be applied to the yaw axis, in degrees.
         *
         * @return supported yaw offset range
         */
        @NonNull
        public abstract DoubleRange supportedYawRange();

        /**
         * Retrieves the current alignment offset applied to the yaw axis, in degrees.
         *
         * @return yaw alignment offset
         */
        public abstract double yaw();

        /**
         * Sets the alignment offset to be applied to the yaw axis, in degrees.
         *
         * @param offset alignment offset to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setYaw(double offset);

        /**
         * Retrieves the supported alignment offset range to be applied to the pitch axis, in degrees.
         *
         * @return supported pitch offset range
         */
        @NonNull
        public abstract DoubleRange supportedPitchRange();

        /**
         * Retrieves the current alignment offset applied to the pitch axis, in degrees.
         *
         * @return pitch alignment offset
         */
        public abstract double pitch();

        /**
         * Sets the alignment offset to be applied to the pitch axis, in degrees.
         *
         * @param offset alignment offset to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setPitch(double offset);

        /**
         * Retrieves the supported alignment offset range to be applied to the roll axis, in degrees.
         *
         * @return supported roll offset range
         */
        @NonNull
        public abstract DoubleRange supportedRollRange();

        /**
         * Retrieves the current alignment offset applied to the roll axis, in degrees.
         *
         * @return roll alignment offset
         */
        public abstract double roll();

        /**
         * Sets the alignment offset to be applied to the roll axis, in degrees.
         *
         * @param offset alignment offset to set
         *
         * @return {@code this}, to allow chained calls
         */
        @NonNull
        public abstract Setting setRoll(double offset);

        /**
         * Resets camera alignment to factory values.
         *
         * @return {@code true} if the factory reset has begun, otherwise {@code false}
         */
        public abstract boolean reset();
    }

    /**
     * Private constructor for static scoping class.
     */
    private CameraAlignment() {
    }
}
