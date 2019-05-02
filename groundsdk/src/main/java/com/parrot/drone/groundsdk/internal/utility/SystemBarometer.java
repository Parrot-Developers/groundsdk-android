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
 * Utility interface allowing to monitor the operating system barometer, if available.
 * <p>
 * This utility may be unavailable if the underlying system does not provide a barometer sensor. It may be obtained
 * after engine startup using:
 * <pre>{@code SystemBarometer barometer = getUtility(SystemBarometer.class);}</pre>
 *
 * @see EngineBase#getUtility(Class)
 */
public interface SystemBarometer extends Monitorable<SystemBarometer.Monitor>, Utility {

    /**
     * Callback interface receiving barometer information.
     */
    interface Monitor {

        /**
         * Called back when the system barometer sensor provides a new atmospheric pressure sample.
         *
         * @param pressure         measured atmospheric pressure, in Pa
         * @param measureTimeStamp timestamp of the provided pressure sample, in nanoseconds
         */
        void onAirPressureMeasure(double pressure, long measureTimeStamp);
    }
}
