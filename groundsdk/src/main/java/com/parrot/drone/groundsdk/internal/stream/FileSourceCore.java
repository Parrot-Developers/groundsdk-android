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

package com.parrot.drone.groundsdk.internal.stream;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.stream.FileReplay;
import com.parrot.drone.sdkcore.stream.SdkCoreStream;
import com.parrot.drone.sdkcore.stream.SdkCoreTracks;

import java.io.File;

/** Core class for {@link FileReplay.Source}. */
public final class FileSourceCore implements FileReplay.Source {

    /** Name for the thermal unblended video track in thermal media files. */
    @NonNull
    public static final String TRACK_THERMAL_UNBLENDED = SdkCoreTracks.TRACK_THERMAL_UNBLENDED;

    /** Source local file. */
    @NonNull
    private final File mFile;

    /** Source track name, {@code null} for default track, if any. */
    @Nullable
    private final String mTrackName;

    /**
     * Constructor.
     *
     * @param file      local file to stream
     * @param trackName name of the track to select for streaming, {@code null} for default track, if any
     */
    public FileSourceCore(@NonNull File file, @Nullable String trackName) {
        mFile = file;
        mTrackName = trackName;
    }

    @NonNull
    @Override
    public File file() {
        return mFile;
    }

    @Nullable
    @Override
    public String trackName() {
        return mTrackName;
    }

    /**
     * Opens a stream for this source.
     *
     * @param client stream client, notified of stream lifecycle events
     *
     * @return a new {@code SdkCoreStream} instance
     */
    @NonNull
    SdkCoreStream openStream(@NonNull SdkCoreStream.Client client) {
        return SdkCoreStream.openFromFile(mFile, mTrackName, client);
    }

    //region Parcelable

    /** Parcelable static factory. */
    @NonNull
    public static final Creator<FileSourceCore> CREATOR = new Creator<FileSourceCore>() {

        @Override
        @NonNull
        public FileSourceCore createFromParcel(@NonNull Parcel src) {
            return new FileSourceCore(src);
        }

        @Override
        @NonNull
        public FileSourceCore[] newArray(int size) {
            return new FileSourceCore[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dst, int flags) {
        dst.writeString(mFile.getAbsolutePath());
        dst.writeString(mTrackName);
    }

    /**
     * Constructor for Parcel deserialization.
     *
     * @param src parcel to deserialize from
     */
    private FileSourceCore(@NonNull Parcel src) {
        mFile = new File(src.readString());
        mTrackName = src.readString();
    }

    //endregion
}
