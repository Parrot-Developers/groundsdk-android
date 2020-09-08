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

package com.parrot.drone.groundsdk.arsdkengine.ephemeris;

import android.content.Context;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.R;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_EPHEMERIS;

/**
 * Implementation class for u-blox ephemerides.
 * <p>
 * U-blox ephemerides are uploaded to the following drone models: <ul>
 *     <li>{@link Drone.Model#ANAFI_4K},</li>
 *     <li>{@link Drone.Model#ANAFI_THERMAL},</li>
 *     <li>{@link Drone.Model#ANAFI_UA},</li>
 *     <li>{@link Drone.Model#ANAFI_USA}.</li>
 * </ul>
 */
class UBloxEphemeris extends EphemerisStore.Ephemeris {

    /** URL of main u-blox ephemeris download server. */
    @NonNull
    private final URL mMainServer;

    /**
     * URL of backup u-blox ephemeris download server. Used when downloading from main server fails for server (not
     * filesystem/local) reasons.
     */
    @NonNull
    private final URL mBackupServer;

    /** Both connection & read timeout used when downloading from u-blox servers, in milliseconds. */
    private static final int CONNECTION_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

    /** Format of the temporary file where ephemeris is downloaded before being moved to the final file. */
    private static final String TEMP_FILE_FORMAT = ".%s.tmp";

    /**
     * Constructor.
     *
     * @param store   ephemeris store
     * @param context android application context
     */
    UBloxEphemeris(@NonNull EphemerisStore store, @NonNull Context context) {
        super(store, "ublox");
        String apiToken = context.getString(R.string.ublox_api_token);
        try {
            mMainServer = new URL(context.getString(R.string.ublox_ephemeris_main_server, apiToken));
            mBackupServer = new URL(context.getString(R.string.ublox_ephemeris_backup_server, apiToken));
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean download(@NonNull File ephemeris) {
        boolean success = false;
        File tempFile = new File(ephemeris.getParentFile(),
                String.format(Locale.US, TEMP_FILE_FORMAT, ephemeris.getName()));
        try {
            if (tempFile.createNewFile()) {
                try {
                    if (download(mMainServer, tempFile) || download(mBackupServer, tempFile)) {
                        //noinspection ResultOfMethodCallIgnored
                        ephemeris.delete();
                        if (tempFile.renameTo(ephemeris)) {
                            ULog.i(TAG_EPHEMERIS, "Successfully downloaded ublox ephemeris");
                            success = true;
                        } else {
                            ULog.e(TAG_EPHEMERIS, "Failed to move ublox ephemeris from temporary to final file");
                        }
                    } else {
                        ULog.e(TAG_EPHEMERIS, "Failed to download from ublox servers");
                    }
                } finally {
                    if (tempFile.exists() && !tempFile.delete()) {
                        ULog.e(TAG_EPHEMERIS, "Failed to cleanup temporary ublox ephemeris file");
                    }
                }
            } else {
                ULog.i(TAG_EPHEMERIS, "Skipping ublox ephemeris download, already in progress");
            }
        } catch (IOException e) {
            ULog.e(TAG_EPHEMERIS, "Failed to write ublox ephemeris", e);
        }
        return success;
    }

    /**
     * Downloads ephemeris data.
     *
     * @param srcServer url of the server to download data from
     * @param dstFile   file where to write downloaded data
     *
     * @return {@code true} if the download completed successfully, {@code false} if the download failed due to a
     *         server/remote reason
     *
     * @throws IOException if the download failed due to a local/filesystem issue
     */
    private static boolean download(@NonNull URL srcServer, @NonNull File dstFile) throws IOException {
        HttpURLConnection connection = null;
        InputStream srcStream = null;
        try {
            try {
                connection = (HttpURLConnection) srcServer.openConnection();
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(CONNECTION_TIMEOUT);
                srcStream = connection.getInputStream();
            } catch (IOException e) {
                ULog.w(TAG_EPHEMERIS, "Could not connect to ublox server:" + srcServer.getHost());
                return false;
            }

            try (OutputStream dstStream = new FileOutputStream(dstFile)) {
                byte[] buffer = new byte[4096];
                int len;
                do {
                    try {
                        len = srcStream.read(buffer);
                    } catch (IOException e) {
                        ULog.w(TAG_EPHEMERIS, "Could not read from ublox server: " + srcServer.getHost());
                        return false;
                    }
                    if (len > 0) {
                        dstStream.write(buffer, 0, len);
                    }
                } while (len != -1);
            }
        } finally {
            if (srcStream != null) {
                try {
                    srcStream.close();
                } catch (IOException ignored) {
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return true;
    }
}
