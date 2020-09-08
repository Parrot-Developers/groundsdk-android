/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

package com.parrot.drone.groundsdk.device.peripheral;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.value.BooleanSetting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * DRI peripheral interface.
 *
 * The DRI or Drone Remote ID is a protocol that sends periodic broadcasts of some identification data
 * during the flight for safety, security, and compliance purposes.
 * <p>
 * This peripheral allows to enable or disable the DRI.
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(Dri.class)}</pre>
 *
 * @see Drone#getPeripheral(Class)
 * @see Drone#getPeripheral(Class, Ref.Observer)
 */
public interface Dri extends Peripheral {

    /**
     * Type of ID that can be sent in the DRI.
     */
    enum IdType {
        /** French 30-byte format. */
        FR_30_OCTETS,

        /** ANSI CTA 2063 format on 40 bytes. */
        ANSI_CTA_2063
    }

    /**
     * DRI ID.
     */
    interface DroneId {

        /**
         * Gets the type of the ID.
         *
         * @return ID type
         */
        @NonNull
        IdType getType();

        /**
         * Gets the ID.
         *
         * @return ID
         */
        @NonNull
        String getId();
    }

    /**
     * Gets information about the ID.
     *
     * @return information about the ID if available, {@code null} otherwise
     */
    @Nullable
    DroneId getDroneId();

    /**
     * Gives access to the DRI state setting.
     * <p>
     * This setting allows to enable or disable the drone DRI.
     *
     * @return the DRI state setting
     */
    @NonNull
    BooleanSetting state();
}
