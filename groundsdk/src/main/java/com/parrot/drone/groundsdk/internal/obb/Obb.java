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

package com.parrot.drone.groundsdk.internal.obb;

import android.content.Context;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.GroundSdkConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides access to APK expansion files content.
 */
public final class Obb {

    /**
     * Regex that matches the obb file pattern, as defined by google. %s must be replaced by app package name before
     * use.
     */
    private static final String OBB_FILE_PATTERN = "(?:main|patch)\\.(?:0|[1-9]\\d*)\\Q.%s.obb\\E";

    /**
     * Opens a file contained in APK expansion files.
     * <p>
     * This method performs I/O and as such should be called from a background thread.
     *
     * @param context android application context, used to obtain APK expansion files location
     * @param path    path of the file inside the APK expansion files, <strong>MUST NOT</strong> start with a slash
     *                character
     *
     * @return an open {@code InputStream} that allows to read the given file from the APK expansion files.
     *
     * @throws IOException in case the given file could not be opened for any reason
     */
    @NonNull
    public static InputStream openFile(@NonNull Context context, @NonNull String path) throws IOException {
        File obbDir = context.getObbDir();
        if (obbDir == null) {
            throw new IOException("No accessible OBB directory");
        }
        String pattern = String.format(OBB_FILE_PATTERN, GroundSdkConfig.get(context).getApplicationPackage());
        Set<String> obbFiles = new HashSet<>();
        //noinspection ResultOfMethodCallIgnored
        obbDir.listFiles(file -> {
            if (file.isFile() && file.getName().matches(pattern)) {
                obbFiles.add(file.getAbsolutePath());
            }
            return false;
        });
        if (obbFiles.isEmpty()) {
            throw new IOException("Could not find any obb file");
        }
        return APKExpansionSupport.getResourceZipFile(obbFiles.toArray(new String[0])).getInputStream(path);
    }

    /**
     * Private constructor for static utility class.
     */
    private Obb() {
    }
}
