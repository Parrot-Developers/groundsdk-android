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

import com.parrot.drone.groundsdk.internal.Monitorable;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;

/**
 * Utility interface allowing to monitor the operating system connectivity and be notified of internet availability
 * changes.
 * <p>
 * This utility is always available and can be safely requested after engine startup using:
 * <pre>{@code SystemConnectivity connectivity = getUtilityOrThrow(SystemConnectivity.class);}</pre>
 *
 * @see EngineBase#getUtilityOrThrow(Class)
 */
public interface SystemConnectivity extends Monitorable<SystemConnectivity.Monitor>, Utility {

    /**
     * Callback interface receiving system connectivity information.
     */
    interface Monitor {

        /**
         * Called back when internet availability changes.
         *
         * @param availableNow {@code true} if internet connectivity became available, {@code false} if it became
         *                     unavailable
         */
        void onInternetAvailabilityChanged(boolean availableNow);
    }

    /**
     * Tells whether internet connectivity is currently available.
     *
     * @return {@code true} if internet connectivity is available, otherwise {@code false}
     */
    boolean isInternetAvailable();
}
