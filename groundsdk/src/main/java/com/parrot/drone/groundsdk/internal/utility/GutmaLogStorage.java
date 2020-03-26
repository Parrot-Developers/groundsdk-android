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

import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;

import java.io.File;

import androidx.annotation.NonNull;


/**
 * Utility interface allowing to access GUTMA logs engine internal storage.
 * <p>
 * This mainly allows GUTMA logs providers to query the location where they should store GUTMA log files and to notify
 * the engine when new files have been provided.
 * <p>
 * This utility may be unavailable if GUTMA logs support is disabled in GroundSdk configuration. It may be obtained
 * after engine startup using:
 * <pre>{@code GutmaLogStorage storage = getUtility(GutmaLogStorage.class);}</pre>
 *
 * @see EngineBase#getUtility(Class)
 * @see GroundSdkConfig#isGutmaLogEnabled()
 */
public interface GutmaLogStorage extends Utility {

    /**
     * Retrieves the directory where new GUTMA log files may be provided.
     * <p>
     * Any directory in this directory will be considered garbage by the engine, which may delete them at some point.
     * <p>
     * Multiple providers may be assigned the same directory. As a consequence, files that a provider may create should
     * have a name as unique as possible to avoid collision.
     * <p>
     * The directory in question might not be existing, and the caller has the responsibility to create it if necessary,
     * but should ensure to do so on a background thread.
     *
     * @return a file pointing to a directory where GUTMA log files may be provided
     */
    @NonNull
    File getWorkDir();

    /**
     * Notifies the engine that a new GUTMA log file has been provided.
     *
     * @param file provided GUTMA log file
     */
    void notifyGutmaLogFileReady(@NonNull File file);
}
