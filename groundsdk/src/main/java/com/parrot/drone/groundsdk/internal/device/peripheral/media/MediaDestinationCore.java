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

package com.parrot.drone.groundsdk.internal.device.peripheral.media;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.File;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_API;

/**
 * Implementation of {@code MediaDestination}.
 */
public abstract class MediaDestinationCore extends MediaDestination {

    /**
     * A {@code MediaDestination} to the platform shared media directory.
     */
    public static final class PlatformMediaStore extends MediaDestinationCore {

        /**
         * Constructor.
         *
         * @param albumName media album name
         */
        public PlatformMediaStore(@Nullable String albumName) {
            super("Platform media store", albumName);
        }

        @Nullable
        @Override
        File getBaseDirectory() {
            return ApplicationStorageProvider.getInstance().getPlatformMediaStore();
        }

        @Override
        void notifyFileAdded(@NonNull File file) {
            ApplicationStorageProvider.getInstance().notifyFileAdded(file);
        }
    }

    /**
     * A {@code MediaDestination} to the application private media directory.
     */
    public static final class AppPrivateFiles extends MediaDestinationCore {

        /**
         * Constructor.
         *
         * @param subDirectory sub-directory in application private media directory
         */
        public AppPrivateFiles(@Nullable String subDirectory) {
            super("Private application media store", subDirectory);
        }

        @Nullable
        @Override
        File getBaseDirectory() {
            return ApplicationStorageProvider.getInstance().getPrivateMediaStore();
        }
    }

    /**
     * A {@code MediaDestination} to the application private file cache.
     */
    public static final class AppFilesCache extends MediaDestinationCore {

        /**
         * Constructor.
         */
        public AppFilesCache() {
            super("Private application file cache", null);
        }

        @Nullable
        @Override
        File getBaseDirectory() {
            return ApplicationStorageProvider.getInstance().getTemporaryFileCache();
        }
    }

    /**
     * A {@code MediaDestination} to a given directory.
     */
    public static final class Path extends MediaDestinationCore {

        /** Destination directory where to download the resources. */
        @NonNull
        private final File mDirectory;

        /**
         * Constructor.
         *
         * @param directory destination directory where to download the resources
         */
        public Path(@NonNull File directory) {
            super("Custom path: " + directory.getAbsolutePath(), null);
            mDirectory = directory;
        }

        @NonNull
        @Override
        File getBaseDirectory() {
            return mDirectory;
        }
    }

    /**
     * Unwraps a download destination to its internal {@code MediaDestinationCore} representation.
     *
     * @param dest download destination to unwrap
     *
     * @return internal {@code MediaDestinationCore} representation of the specified download destination
     *
     * @throws IllegalArgumentException in case the provided download destination is not based upon {@code
     *                                  MediaDestinationCore}
     */
    @NonNull
    static MediaDestinationCore unwrap(@NonNull MediaDestination dest) {
        try {
            return (MediaDestinationCore) dest;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid MediaDestination: " + dest, e);
        }
    }

    /** Name of the download destination. Used for access failure logs. */
    @NonNull
    private final String mDestinationName;

    /** Destination subdirectory, {@code null} if none. */
    @Nullable
    private final String mSubDirectory;

    /**
     * Constructor.
     *
     * @param destinationName name of the download destination
     * @param subDirectory    destination subdirectory, {@code null} if none
     */
    private MediaDestinationCore(@NonNull String destinationName, @Nullable String subDirectory) {
        mDestinationName = destinationName;
        mSubDirectory = subDirectory;
    }

    /**
     * Computes the destination path for this {@code MediaDestination}, ensuring it exists and creating
     * intermediate directories if appropriate.
     *
     * @return the destination path if accessible, otherwise {@code null}
     */
    @Nullable
    final String ensurePath() {
        String path = null;
        File dir = getBaseDirectory();
        if (dir == null) {
            ULog.e(TAG_API, "Unavailable storage destination: " + mDestinationName);
        } else {
            if (mSubDirectory != null) {
                dir = new File(dir, mSubDirectory);
            }
            path = dir.getAbsolutePath();
            if (!dir.isDirectory() && !dir.mkdirs()) {
                ULog.e(TAG_API, "Could not create directory: " + path);
                path = null;
            }
        }
        return path;
    }

    /**
     * Gets the base directory for this {@code MediaDestination}.
     *
     * @return the base directory, {@code null} if currently inaccessible
     */
    @Nullable
    abstract File getBaseDirectory();

    /**
     * Notifies that a file has been added to the destination underlying storage.
     * <p>
     * Default implementation does nothing.
     *
     * @param file file to notify about
     */
    void notifyFileAdded(@NonNull File file) {

    }
}
