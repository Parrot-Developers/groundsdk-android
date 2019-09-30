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

package com.parrot.drone.groundsdk.internal.utility;

import android.location.Address;

import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.internal.Monitorable;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;

/**
 * Utility interface providing reverse geocoding service.
 * <p>
 * This utility may be unavailable if there is no backend service for reverse geocoding on this device.
 * <p>
 * This utility may be obtained after engine startup using:
 * <pre>{@code ReverseGeocoderUtility reverseGeocoder = getUtility(ReverseGeocoderUtility.class);}</pre>
 *
 * @see EngineBase#getUtility(Class)
 */
public interface ReverseGeocoderUtility extends Monitorable<ReverseGeocoderUtility.Monitor>, Utility {

    /**
     * Callback interface receiving reverse geocoding notifications.
     */
    interface Monitor {

        /**
         * Called back when a new address has been determined.
         */
        void onChange();
    }

    /**
     * Gets the address matching the current user location.
     *
     * @return the address matching user location, or {@code null} if it could not be determined
     */
    @Nullable
    Address getAddress();
}
