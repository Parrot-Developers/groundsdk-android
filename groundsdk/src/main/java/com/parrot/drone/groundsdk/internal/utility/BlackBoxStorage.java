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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Utility interface allowing to access black box engine internal storage.
 * <p>
 * This mainly allows black box recorders to forward recorded black box data to be archived in storage.
 * <p>
 * This utility may be unavailable if black box support is disabled in GroundSdk configuration. It may be obtained
 * after engine startup using:
 * <pre>{@code BlackBoxStorage storage = getUtility(BlackBoxStorage.class);}</pre>
 *
 * @see EngineBase#getUtility(Class)
 * @see GroundSdkConfig#isBlackBoxEnabled()
 */
public interface BlackBoxStorage extends Utility {

    /**
     * Collected black box report data.
     */
    interface BlackBox {

        /**
         * Writes JSON serialized black box report directly to the given output stream.
         * <p>
         * This method should be called from a background thread.
         *
         * @param stream stream to write to
         *
         * @throws IOException in case writing report data failed for any reason
         */
        void writeTo(@NonNull OutputStream stream) throws IOException;
    }

    /**
     * Notifies the black box engine that black box data as been collected and that a new report may be archived.
     *
     * @param blackBox collected black box data
     */
    void notifyBlackBoxReady(@NonNull BlackBox blackBox);
}
