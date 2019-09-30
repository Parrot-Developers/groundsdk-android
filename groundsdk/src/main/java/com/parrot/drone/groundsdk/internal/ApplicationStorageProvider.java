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

package com.parrot.drone.groundsdk.internal;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.File;

/**
 * Component that provides access to various application storage paths.
 */
public abstract class ApplicationStorageProvider {

    /** Singleton instance. */
    private static ApplicationStorageProvider sInstance;

    /**
     * Sets the default implementation as the singleton instance of {@code ApplicationStorageProvider}.
     * <p>
     * The default implementation is backed by the provided application context.
     * <p>
     * This method is called when GroundSdkCore singleton instance is created.
     *
     * @param appContext android application context.
     */
    public static void setDefault(@NonNull Context appContext) {
        synchronized (ApplicationStorageProvider.class) {
            sInstance = new ApplicationStorageProvider() {

                @Override
                public File getPlatformMediaStore() {
                    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                }

                @Override
                public File getPrivateMediaStore() {
                    return appContext.getExternalFilesDir(Environment.DIRECTORY_DCIM);
                }

                @Override
                public File getTemporaryFileCache() {
                    return appContext.getExternalCacheDir();
                }

                @NonNull
                @Override
                public File getInternalAppFileCache() {
                    return appContext.getFilesDir();
                }

                @Override
                public void notifyFileAdded(@NonNull File file) {
                    appContext.sendBroadcast(
                            new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).setData(Uri.fromFile(file)));
                }
            };
        }
    }

    /**
     * Forces the provided {@code ApplicationStorageProvider} instance to be the singleton
     * {@code ApplicationStorageProvider} instance.
     * <p>
     * This method is used by tests to provide custom storage paths if so needed.
     *
     * @param instance the application storage provider instance to set
     */
    @VisibleForTesting
    public static void setInstance(@Nullable ApplicationStorageProvider instance) {
        synchronized (ApplicationNotifier.class) {
            sInstance = instance;
        }
    }

    /**
     * Gets the {@code ApplicationStorageProvider} singleton.
     *
     * @return the {@code ApplicationStorageProvider}
     *
     * @throws IllegalStateException in case the application storage provider instance has not been set
     */
    @NonNull
    public static ApplicationStorageProvider getInstance() {
        synchronized (ApplicationStorageProvider.class) {
            if (sInstance == null) {
                throw new IllegalStateException("No ApplicationStorageProvider instance");
            }
            return sInstance;
        }
    }

    /**
     * Gets the path to the shared, platform storage for media such as pictures and videos.
     *
     * @return the path to the platform media store, or {@code null} if currently not available
     */
    @Nullable
    public abstract File getPlatformMediaStore();

    /**
     * Gets the path to the private application storage for media such as pictures and videos.
     *
     * @return the path to the private application media store, or {@code null} if currently not available
     */
    @Nullable
    public abstract File getPrivateMediaStore();

    /**
     * Gets the path to the sdcard application temporary file cache.
     *
     * @return the path to the sdcard application temporary file cache or {@code null} if currently not available
     */
    @Nullable
    public abstract File getTemporaryFileCache();

    /**
     * Gets the path to the private application temporary file cache.
     *
     * @return the path to the private application temporary file cache
     */
    @NonNull
    public abstract File getInternalAppFileCache();

    /**
     * Notify the platform that a file has been added on the device's storage.
     *
     * @param file the added file
     */
    public abstract void notifyFileAdded(@NonNull File file);
}
