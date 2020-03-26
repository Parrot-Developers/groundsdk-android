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

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.MediaStore;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;

/**
 * A media item in a {@link MediaStore}.
 * <p>
 * A media item instance is a parcelable object, and as such may be used with android's parcelable-supporting utilities,
 * such as conveying media items in {@link Intent} (see {@link Intent#putExtra(String, Parcelable)}) or in
 * {@link Bundle} (see {@link Bundle#putParcelable(String, Parcelable)} objects.
 */
public interface MediaItem extends Parcelable {

    /**
     * Type of media.
     */
    enum Type {

        /** Media is a photo. */
        PHOTO,

        /** Media is a video. */
        VIDEO
    }

    /** Photo mode of the media. */
    enum PhotoMode {

        /** Single shot mode. */
        SINGLE,

        /** Bracketing mode (burst of 3 or 5 frames with different exposures). */
        BRACKETING,

        /** Burst mode. */
        BURST,

        /** Panorama mode (set of photos taken with different yaw angles and camera tilt angles). */
        PANORAMA,

        /** Time-lapse mode (photos taken at regular time intervals). */
        TIME_LAPSE,

        /** GPS-lapse mode (photos taken at regular GPS position intervals). */
        GPS_LAPSE
    }

    /** Panorama type of a panorama photo. */
    enum PanoramaType {

        /** Horizontal 180° panorama type. */
        HORIZONTAL_180,

        /** Vertical 180° panorama type. */
        VERTICAL_180,

        /** Spherical panorama type. */
        SPHERICAL
    }

    /** Available metadata types. */
    enum MetadataType {

        /** Media contains thermal metadata. */
        THERMAL
    }

    /** Available media tracks. */
    enum Track {

        /**
         * Default video track.
         * <p>
         * Note that for thermal medias, this is actually the recorded video stream blended with thermal data.
         */
        DEFAULT_VIDEO,

        /** Thermal raw video, not blended with thermal data. */
        THERMAL_UNBLENDED
    }

    /**
     * A resource in a media item.
     * <p>
     * A media resource instance is a parcelable object, and as such may be used with android's parcelable-supporting
     * utilities, such as conveying media items in {@link Intent} (see {@link Intent#putExtra(String, Parcelable)}) or
     * in {@link Bundle} (see {@link Bundle#putParcelable(String, Parcelable)} objects.
     * <p>
     * Note however that, since a media resource is linked to its media item parent, serializing a media resource to
     * a parcel amounts to serializing its media item parent, plus an index to the resource in that parent. As a
     * consequence, serializing multiple resources from the same media is rather expensive memory-wise; for this use
     * case, consider using a {@link MediaResourceList} instance, which provides an optimized way to serialize
     * an heterogeneous collection of media resources.
     */
    interface Resource extends Parcelable {

        /**
         * Format of a resource.
         */
        enum Format {

            /** JPEG photo. */
            JPG,

            /** Digital Negative (DNG) photo. */
            DNG,

            /** MP4 video. */
            MP4
        }

        /**
         * Retrieves the unique identifier of this resource.
         *
         * @return resource unique identifier
         */
        @NonNull
        String getUid();

        /**
         * Retrieves this resource's media item parent.
         *
         * @return resource media item parent
         */
        @NonNull
        MediaItem getMedia();

        /**
         * Retrieves the format of this resource.
         *
         * @return resource format
         */
        @NonNull
        Format getFormat();

        /**
         * Retrieves the size of this resource, in bytes.
         *
         * @return resource size
         */
        @IntRange(from = 0)
        long getSize();

        /**
         * Retrieves the video duration, in milliseconds.
         *
         * @return resource duration (for video)
         */
        @IntRange(from = 0)
        long getDuration();

        /**
         * Retrieves the creation date of this resource.
         *
         * @return resource creation date
         */
        @NonNull
        Date getCreationDate();

        /**
         * Retrieves the creation location of this resource
         * <p>
         * Note that the returned {@code Location} object, if any, provides relevant information on
         * latitude, longitude and altitude only.
         *
         * @return resource creation location, or {@code null} if unavailable
         */
        @Nullable
        Location getLocation();

        /**
         * Lists available metadata type(s) in this resource.
         * <p>
         * Returned set is owned by the caller and can be freely modified.
         *
         * @return available metadata types
         */
        @NonNull
        EnumSet<MetadataType> getAvailableMetadata();

        /**
         * Lists media tracks available from this resource.
         * <p>
         * Returned set is owned by the caller and can be freely modified.
         *
         * @return available tracks
         */
        @NonNull
        EnumSet<Track> getAvailableTracks();
    }

    /**
     * Retrieves the unique identifier of this media.
     *
     * @return media unique identifier
     */
    @NonNull
    String getUid();

    /**
     * Retrieves the name of this media.
     *
     * @return media name
     */
    @NonNull
    String getName();

    /**
     * Retrieves the type of this media.
     *
     * @return media type
     */
    @NonNull
    Type getType();

    /**
     * Retrieves the identifier of the run associated to this media.
     *
     * @return media run identifier, {@code null} if none
     */
    @Nullable
    String getRunUid();

    /**
     * Retrieves the creation date of this media.
     *
     * @return media creation date
     */
    @NonNull
    Date getCreationDate();

    /**
     * Retrieves the expected count of resources in this media.
     * <p>
     * Note that it may be different from the actual count of resources provided by this media. <br>
     * This indicates that some resources failed to be created during the production of this media.
     *
     * @return expected resource count, or {@code 0} if unknown
     */
    @IntRange(from = 0)
    int getExpectedResourceCount();

    /**
     * Retrieves the photo mode of this media.
     *
     * @return photo mode, {@code null} if not available or if media is a video
     */
    @Nullable
    PhotoMode getPhotoMode();

    /**
     * Retrieves the panorama type of this media.
     *
     * @return the panorama type, if the media is a photo with {@link #getPhotoMode() photo mode} set to
     *         {@link PhotoMode#PANORAMA PANORAMA}, otherwise {@code null}
     */
    @Nullable
    PanoramaType getPanoramaType();

    /**
     * Lists available metadata type(s) in this media.
     * <p>
     * Returned set is owned by the caller and can be freely modified.
     *
     * @return available metadata types
     */
    @NonNull
    EnumSet<MetadataType> getAvailableMetadata();

    /**
     * Retrieves the resources associated to this media.
     * <p>
     * Returned list cannot be modified.
     *
     * @return media resources
     */
    @NonNull
    List<? extends Resource> getResources();
}
