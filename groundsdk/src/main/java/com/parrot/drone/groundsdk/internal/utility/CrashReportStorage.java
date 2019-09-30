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
import java.util.Collection;

/**
 * Utility interface allowing to access crash report engine internal storage.
 * <p>
 * This mainly allows crash report downloaders to query the location where they should store downloaded reports and
 * to notify the engine when new reports have been downloaded.
 * <p>
 * This utility may be unavailable if crash report support is disabled in GroundSdk configuration. It may be obtained
 * after engine startup using:
 * <pre>{@code CrashReportStorage storage = getUtility(CrashReportStorage.class);}</pre>
 *
 * @see EngineBase#getUtility(Class)
 * @see GroundSdkConfig#isCrashReportEnabled()
 */
public interface CrashReportStorage extends Utility {

    /** Extension that may be used to create temporary files for download. */
    String TMP_FILE_EXT = ".tmp";

    /** Extension that must be used to denote anonymous reports. */
    String ANONYMOUS_REPORT_EXT = ".anon";

    /**
     * Retrieves the directory where new crash report files may be downloaded.
     * <p>
     * Inside this directory, the following rules apply: <ul>
     * <li> Downloaders may create temporary files or directories, that have a {@link #TMP_FILE_EXT} suffix
     * to their name, for any purpose they see fit. Those files will be cleaned up by the engine when
     * appropriate. </li>
     * <li> Sub-directories with any other name will be considered garbage; the engine may delete them at any
     * time. </li>
     * <li> Downloaders may also create files with a {@link #ANONYMOUS_REPORT_EXT} suffix to their name. Such files
     * will be treated as anonymous crash reports by the engine. </li>
     * <li> Files with other names are treated as personal-data-disclosing crash report files by the engine. </li>
     * </ul>
     * Both anonymous and personal-data-disclosing crash reports may be collected by the engine for later upload.
     * </p>
     * Multiple downloaders may be assigned the same download directory. As a consequence, files that a downloader may
     * create should have a name as unique as possible to avoid collision.
     * <p>
     * The directory in question might not be existing, and the caller as the responsibility to create it if necessary,
     * but should ensure to do so on a background thread.
     *
     * @return a file pointing to a directory where crash report files may be downloaded.
     */
    @NonNull
    File getWorkDir();

    /**
     * Notifies the crash report engine that new report files have been downloaded and are ready to be uploaded.
     *
     * @param reports downloaded crash report files
     */
    void notifyReportsReady(@NonNull Collection<File> reports);
}
