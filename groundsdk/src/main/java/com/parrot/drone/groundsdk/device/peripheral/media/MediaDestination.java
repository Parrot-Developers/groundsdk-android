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

package com.parrot.drone.groundsdk.device.peripheral.media;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.internal.device.peripheral.media.MediaDestinationCore;

import java.io.File;

/**
 * A destination for downloaded media resources.
 */
public abstract class MediaDestination {

    /**
     * Creates a new {@code MediaDestination} for downloading media resources inside the platform shared
     * media store, optionally in a dedicated album.
     * <p>
     * The platform media scanner is triggered each time a new file is downloaded to this destination, so that
     * the downloaded media appears automatically in external gallery/media applications.
     *
     * @param albumName name of the album in the platform media store where to download the resources. Created if it
     *                  does not exist. May be {@code null}, in which case downloaded resources are placed at the root
     *                  of the platform media store.
     *
     * @return a {@code MediaDestination} configured for downloading resources in the platform media store
     */
    @NonNull
    public static MediaDestination platformMediaStore(@Nullable String albumName) {
        return new MediaDestinationCore.PlatformMediaStore(albumName);
    }

    /**
     * Creates a new {@code MediaDestination} for downloading media resources inside the application private
     * files directory, optionally in a dedicated sub directory.
     *
     * @param directoryName name of the subdirectory in the application private files directory where to download the
     *                      resources. Created if it does not exist. May be {@code null}, in which case downloaded
     *                      resources are placed at the root of the application private files directory.
     *
     * @return a {@code MediaDestination} configured for downloading resources in the application private files
     */
    @NonNull
    public static MediaDestination appPrivateFiles(@Nullable String directoryName) {
        return new MediaDestinationCore.AppPrivateFiles(directoryName);
    }

    /**
     * Creates a new (@code MediaDestination} for downloading media resources inside the application temporary file
     * cache.
     *
     * @return a {@code MediaDestination} configured for downloading resources in the private application files
     *         cache
     */
    @NonNull
    public static MediaDestination temporary() {
        return new MediaDestinationCore.AppFilesCache();
    }

    /**
     * Creates a new (@code MediaDestination} for downloading media resources inside a given directory.
     *
     * @param directory destination directory where to download the resources
     *
     * @return a {@code MediaDestination} configured for downloading resources in the given directory
     */
    @NonNull
    public static MediaDestination path(@NonNull File directory) {
        return new MediaDestinationCore.Path(directory);
    }

    /**
     * Constructor.
     * <p>
     * Application <strong>MUST NOT</strong> override this class.
     */
    protected MediaDestination() {

    }
}
