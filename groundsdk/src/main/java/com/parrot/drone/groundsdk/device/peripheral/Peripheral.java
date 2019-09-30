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

package com.parrot.drone.groundsdk.device.peripheral;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;

/**
 * Interface for a Peripheral.
 */
public interface Peripheral {

    /**
     * Interface for an object capable of providing a {@link Peripheral}.
     */
    interface Provider {

        /**
         * Gets a peripheral.
         *
         * @param peripheralClass class of the peripheral
         * @param <P>             type of the peripheral class
         *
         * @return requested peripheral, or {@code null} if it's not present
         */
        @Nullable
        <P extends Peripheral> P getPeripheral(@NonNull Class<P> peripheralClass);

        /**
         * Gets a peripheral and registers an observer notified each time it changes.
         *
         * @param peripheralClass class of the peripheral
         * @param observer        observer to notify when the peripheral changes
         * @param <P>             type of the peripheral class
         *
         * @return reference to the requested peripheral
         */
        @NonNull
        <P extends Peripheral> Ref<P> getPeripheral(@NonNull Class<P> peripheralClass,
                                                    @NonNull Ref.Observer<P> observer);
    }
}
