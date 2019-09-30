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

package com.parrot.drone.groundsdk.internal.engine.flightdata;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.utility.FlightDataStorage;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.util.Collections;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_FLIGHTDATA;

/**
 * Implementation class for the {@code FlightDataStorage} utility.
 */
final class FlightDataStorageCore implements FlightDataStorage {

    /** Engine that acts as a backend for this utility. */
    @NonNull
    private final FlightDataEngine mEngine;

    /**
     * Constructor.
     *
     * @param engine flight data engine
     */
    FlightDataStorageCore(@NonNull FlightDataEngine engine) {
        mEngine = engine;
    }

    @NonNull
    @Override
    public File getWorkDir() {
        return mEngine.getWorkDirectory();
    }

    @Override
    public void notifyFlightDataFileReady(@NonNull File file) {
        if (file.isFile() && !file.getName().endsWith(TMP_FILE_EXT) && file.getParentFile().equals(getWorkDir())) {
            mEngine.addLocalFiles(Collections.singleton(file));
        } else {
            if (ULog.w(TAG_FLIGHTDATA)) {
                ULog.w(TAG_FLIGHTDATA, "Invalid flight data file: " + file);
            }
            if (file.exists() && !file.delete() && ULog.w(TAG_FLIGHTDATA)) {
                ULog.w(TAG_FLIGHTDATA, "Could not delete invalid flight data file:" + file);
            }
        }
    }
}
