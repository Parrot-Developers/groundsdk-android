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

import java.io.File;


/**
 * Utility interface allowing to access flight data engine internal storage.
 * <p>
 * This mainly allows flight data downloaders to query the location where they should store downloaded flight data files
 * and to notify the engine when new flight data files have been downloaded.
 * <p>
 * This utility may be unavailable if flight data support is disabled in GroundSdk configuration. It may be obtained
 * after engine startup using:
 * <pre>{@code FlightDataStorage storage = getUtility(FlightDataStorage.class);}</pre>
 *
 * @see EngineBase#getUtility(Class)
 * @see GroundSdkConfig#isFlightDataEnabled()
 */
public interface FlightDataStorage extends Utility {

    /** Extension that may be used to create temporary files for download. */
    String TMP_FILE_EXT = ".tmp";

    /**
     * Retrieves the directory where new flight data files may be downloaded.
     * <p>
     * Inside this directory, downloaders may create temporary files, that have a {@link #TMP_FILE_EXT}
     * suffix to their name, for any purpose they see fit. Those files will be cleaned up by the engine when
     * appropriate. <br>
     * Any file with another name is considered to be a valid flight data file by the engine, which may publish it to
     * the application at some point.<br>
     * Any directory in this directory will be considered garbage by the engine, which may delete them at some
     * point.
     * <p>
     * Multiple downloaders may be assigned the same download directory. As a consequence, files that a downloader may
     * create should have a name as unique as possible to avoid collision.
     * <p>
     * The directory in question might not be existing, and the caller has the responsibility to create it if necessary,
     * but should ensure to do so on a background thread.
     *
     * @return a file pointing to a directory where flight data files may be downloaded.
     */
    @NonNull
    File getWorkDir();

    /**
     * Notifies the engine that a new flight data file as been downloaded.
     *
     * @param file downloaded flight data file
     */
    void notifyFlightDataFileReady(@NonNull File file);
}
