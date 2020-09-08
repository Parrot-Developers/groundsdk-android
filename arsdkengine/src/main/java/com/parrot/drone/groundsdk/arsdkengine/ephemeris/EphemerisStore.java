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
import android.content.SharedPreferences;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.R;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_EPHEMERIS;

/**
 * Centralizes access to and provides ability to download GPS ephemerides.
 */
public final class EphemerisStore {

    /** Shared preference where ephemerides meta data are stored. */
    private final SharedPreferences mPrefs;

    /** Ephemeris store shared preferences file name. */
    private static final String EPHEMERIS_PREF_FILE = "ephemerides";

    /** Key for accessing ephemeris shared preferences version. Value is int. */
    private static final String PREF_KEY_VERSION = "version";

    /**
     * Key prefix for accessing ephemeris download date. Suffix is the ephemeris downloader name. Value is long
     * (date/time of download in milliseconds as given by {@link System#currentTimeMillis()}).
     */
    private static final String PREF_KEY_PREFIX_DOWNLOAD_TIMESTAMP = "timestamp-";

    /** Name of the directory where ephemeris files are stored. */
    private static final String EPHEMERIDES_DIR = "ephemerides";

    /**
     * Default ephemeris validity period, in ms. A downloaded ephemeris older than this value cannot be uploaded to a
     * drone. Default value that may be overridden by specific ephemeris downloaders.
     */
    private final long mValidityPeriod;

    /**
     * Default minimal time interval to respect between two downloads of the same kind of ephemeris, in ms. Default
     * value that may be overridden by specific ephemeris downloaders.
     */
    private final long mMinDownloadInterval;

    /** u-blox ephemeris. Defines how u-blox ephemerides are downloaded and where they are stored. */
    @NonNull
    private final Ephemeris mUbloxEphemeris;

    /** Path to the directory where all ephemeris files are stored. */
    @NonNull
    private final File mEphemerisDir;

    /**
     * Gets the ephemeris store, if available.
     *
     * @param context android application context
     *
     * @return a new instance of {@code EphemerisStore} if ephemeris feature is enabled, otherwise {@code null}
     */
    @Nullable
    public static EphemerisStore get(@NonNull Context context) {
        return GroundSdkConfig.get(context).isEphemerisSyncEnabled() ? new EphemerisStore(context) : null;
    }

    /**
     * Constructor.
     *
     * @param context android application context
     */
    private EphemerisStore(@NonNull Context context) {
        mPrefs = context.getSharedPreferences(EPHEMERIS_PREF_FILE, Context.MODE_PRIVATE);
        int version = mPrefs.getInt(PREF_KEY_VERSION, 0);
        if (version == 0) {
            version = 1;
            mPrefs.edit().putInt(PREF_KEY_VERSION, version).apply();
        }
        mEphemerisDir = new File(ApplicationStorageProvider.getInstance().getInternalAppFileCache(),
                EPHEMERIDES_DIR);
        mValidityPeriod = TimeUnit.HOURS.toMillis(
                context.getResources().getInteger(R.integer.ephemeris_validity_period));
        mMinDownloadInterval = TimeUnit.HOURS.toMillis(
                context.getResources().getInteger(R.integer.ephemeris_download_interval));
        mUbloxEphemeris = new UBloxEphemeris(this, context);
    }

    /**
     * Retrieves the path of an ephemeris file suitable for upload on the given drone model.
     * <p>
     * Note that no check is done to ensure that the returned file physically exists on the device storage. It is
     * supposed that it exists by trusting ephemeris meta data as loaded by the {@code EphemerisStore}, but the user or
     * another trusted agent may have deleted those files. <br/>
     * The ephemeris uploading process should check (on a background thread) that the file exists before upload.
     *
     * @param droneModel model of the drone to query an ephemeris for
     *
     * @return a file pointing to a suitable ephemeris to be uploaded on the given drone model, or {@code null} if it is
     *         known that no such ephemeris has been downloaded yet, or if the last downloaded ephemeris is too old to
     *         be considered valid anymore
     */
    @Nullable
    public File getEphemeris(@NonNull Drone.Model droneModel) {
        File ephemeris = null;
        switch (droneModel) {
            case ANAFI_4K:
            case ANAFI_THERMAL:
            case ANAFI_UA:
            case ANAFI_USA:
                ephemeris = mUbloxEphemeris.getValidEphemeris();
                break;
        }
        return ephemeris;
    }

    /**
     * Tries and download all kind of managed ephemerides from external sources.
     * <p>
     * This method is blocking for the duration of the download, and as such must be called from a background thread.
     */
    public void downloadEphemerides() {
        mUbloxEphemeris.download();
    }

    /**
     * Represents a kind of ephemeris.
     */
    abstract static class Ephemeris {

        /** {@code EphemerisStore} that manages this ephemeris kind. */
        @NonNull
        private final EphemerisStore mStore;

        /** Name of this kind of ephemeris. <strong>MUST</strong> be unique among all kind of ephemerides. */
        @NonNull
        private final String mName;

        /** Path of the stored ephemeris file. */
        @NonNull
        private final File mEphemeris;

        /** Full key for accessing the last download date for this kind of ephemeris. */
        private final String mTimestampPrefKey;

        /**
         * Constructor.
         *
         * @param store ephemeris store
         * @param name  unique name for this kind of ephemeris
         */
        Ephemeris(@NonNull EphemerisStore store, @NonNull String name) {
            mStore = store;
            mName = name;
            mEphemeris = new File(mStore.mEphemerisDir, mName);
            mTimestampPrefKey = PREF_KEY_PREFIX_DOWNLOAD_TIMESTAMP + mName;
        }

        /**
         * Retrieves the path of the ephemeris file on the local storage.
         *
         * @return the path to the last downloaded ephemeris file if still valid, otherwise {@code null}
         */
        @Nullable
        final File getValidEphemeris() {
            long lastDownload = mStore.mPrefs.getLong(PREF_KEY_PREFIX_DOWNLOAD_TIMESTAMP + mName, -1);
            return lastDownload == -1 || System.currentTimeMillis() - lastDownload > getValidityPeriod() ? null
                    : mEphemeris;
        }

        /**
         * Downloads new ephemeris from the appropriate external source.
         *
         * @param ephemerisDst file on the local storage where to write the ephemeris data
         *
         * @return {@code true} if the download completed successfully, otherwise {@code false}
         */
        abstract boolean download(@NonNull File ephemerisDst);

        /**
         * Retrieves minimal time interval to respect between two downloads of this kind of ephemeris, in ms.
         * <p>
         * Defaults to the {@code EphemerisStore} default value, but may be overridden by specific ephemerides
         * implementations.
         *
         * @return minimal download interval, in milliseconds
         */
        long getMinimalDownloadInterval() {
            return mStore.mMinDownloadInterval;
        }

        /**
         * Retrieves validity period for this kind of ephemeris, in ms.
         * <p>
         * Defaults to the {@code EphemerisStore} default value, but may be overridden by specific ephemerides
         * implementations.
         *
         * @return ephemeris validity period, in milliseconds
         */
        long getValidityPeriod() {
            return mStore.mValidityPeriod;
        }

        /**
         * Downloads up-to-date data for this kind of ephemeris, if minimal download interval constraint allows it.
         */
        private void download() {
            long remainingTime = getMinimalDownloadInterval() - System.currentTimeMillis()
                                 + mStore.mPrefs.getLong(mTimestampPrefKey, 0);
            if (remainingTime <= 0) {
                // create ephemeris parent directory if needed
                File dir = mEphemeris.getParentFile();
                if (dir.exists() || dir.mkdirs()) {
                    if (download(mEphemeris)) {
                        mStore.mPrefs.edit().putLong(mTimestampPrefKey, System.currentTimeMillis()).apply();
                    }
                } else {
                    ULog.e(TAG_EPHEMERIS, "Failed to create ephemerides directory");
                }
            } else if (ULog.i(TAG_EPHEMERIS)) {
                ULog.i(TAG_EPHEMERIS, "Skipping " + mName + " ephemeris download, too soon [remaining: "
                                      + DateUtils.formatElapsedTime(TimeUnit.MILLISECONDS.toSeconds(remainingTime))
                                      + "]");
            }
        }
    }
}
