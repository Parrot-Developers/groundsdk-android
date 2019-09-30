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

package com.parrot.drone.groundsdk.arsdkengine.http;

import android.os.Parcel;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.parrot.drone.groundsdk.arsdkengine.Iso8601;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A media item, as received from the drone HTTP media service.
 * <p>
 * Field names do not respect coding rules to map exactly the field names in the received JSON from which they are
 * parsed.
 * <p>
 * Private default constructors must be kept as they are used by GSON parser.
 */
@SuppressWarnings("unused")
public final class HttpMediaItem implements Iterable<HttpMediaItem.Resource> {

    /** Type of the media item. */
    public enum Type {

        /** Media is a photo. */
        PHOTO,

        /** Media is a video. */
        VIDEO
    }

    /** Photo mode of the media item. */
    public enum PhotoMode {

        /** Single shot mode. */
        SINGLE,

        /** Bracketing mode (burst of 3 or 5 frames with different exposures). */
        BRACKETING,

        /** Burst mode. */
        BURST,

        /** Panorama mode (set of photos taken with different yaw angles and camera tilt angles). */
        PANORAMA,

        /** Time-lapse mode (photos taken at regular time intervals). */
        TIMELAPSE,

        /** GPS-lapse mode (photos taken at regular GPS position intervals). */
        GPSLAPSE
    }

    /** Panorama type of a panorama photo item. */
    public enum PanoramaType {

        /** Horizontal 180° panorama type. */
        HORIZONTAL_180,

        /** Vertical 180° panorama type. */
        VERTICAL_180,

        /** Spherical panorama type. */
        SPHERICAL
    }

    /** Media unique identifier. */
    @Nullable
    private String mediaId;

    /** Media item type. */
    @Nullable
    private Type type;

    /** Media creation date and time. ISO 8601 base format. */
    @JsonAdapter(DateTimeParser.class)
    @Nullable
    private Date datetime;

    /** Total media size, in bytes. Aggregates all media resources sizes. */
    @IntRange(from = 0)
    private long size;

    /** Run identifier of the media. */
    @Nullable
    private String runId;

    /** Thumbnail download relative URL. May be {@code null} if not available. */
    @Nullable
    private String thumbnail;

    /** Expected number of resources in the media. {@code 0} if unknown */
    @IntRange(from = 0)
    private int expectedCount;

    /** Url used to stream the media from the device. May be {@code null} if not available. */
    @Nullable
    private String replayUrl;

    /** Location where the media was created. */
    public static final class Location {

        /** Location latitude. */
        private double latitude;

        /** Location longitude. */
        private double longitude;

        /** Location altitude. */
        private double altitude;

        /**
         * Retrieves location latitude.
         *
         * @return location latitude
         */
        public double getLatitude() {
            return latitude;
        }

        /**
         * Retrieves location longitude.
         *
         * @return location longitude
         */
        public double getLongitude() {
            return longitude;
        }

        /**
         * Retrieves location altitude.
         *
         * @return location altitude
         */
        public double getAltitude() {
            return altitude;
        }

        /**
         * Private, default constructor used by GSON deserializer.
         */
        private Location() {
        }

        /**
         * Constructor for use in tests.
         *
         * @param latitude  media location latitude
         * @param longitude media location longitude
         * @param altitude  media location altitude
         */
        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        Location(double latitude, double longitude, double altitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
        }

        //region Parcelable

        /**
         * Constructor for parcel deserialization.
         *
         * @param src parcel to deserialize from
         */
        Location(@NonNull Parcel src) {
            latitude = src.readDouble();
            longitude = src.readDouble();
            altitude = src.readDouble();
        }

        /**
         * Serializes this location to a parcel.
         *
         * @param dst parcel to serialize to
         */
        void writeToParcel(@NonNull Parcel dst) {
            dst.writeDouble(latitude);
            dst.writeDouble(longitude);
            dst.writeDouble(altitude);
        }

        //endregion
    }

    /** Media creation location. May be {@code null} if unknown. */
    @Nullable
    private Location gps;

    /** Media item photo mode (if media is a photo). */
    @Nullable
    private PhotoMode photoMode;

    /** Media item panorama type (if media is a photo with panorama mode). */
    @Nullable
    private PanoramaType panoramaType;

    /** {@code true} when the media contains thermal metadata, otherwise {@code false}. */
    private boolean thermal;

    /** A media resource. */
    public static final class Resource {

        /** Unique identifier of the media that owns this resource. */
        @Nullable
        private String mediaId;

        /** Unique identifier of the resource. */
        @Nullable
        private String resourceId;

        /** Media resource type. */
        public enum Type {

            /** Resource is a photo. */
            PHOTO,

            /** Resource is a video. */
            VIDEO
        }

        /** Resource type. */
        @Nullable
        private Resource.Type type;

        /** Media resource format. */
        public enum Format {

            /** Resource is a JPEG file. */
            JPG,

            /** Resource is a DNG file. */
            DNG,

            /** Resource is a MP4 file. */
            MP4
        }

        /** Resource format. */
        @Nullable
        private Resource.Format format;

        /** Resource creation date and time. ISO 8601 base format. */
        @JsonAdapter(DateTimeParser.class)
        @Nullable
        private Date datetime;

        /** Resource size, in bytes. */
        @IntRange(from = 0)
        private long size;

        /** Resource duration, in milliseconds (for video). */
        @IntRange(from = 0)
        private int duration;

        /** Resource download relative URL. */
        @Nullable
        private String url;

        /** Resource thumbnail download relative URL. May be {@code null} if not available. */
        @Nullable
        private String thumbnail;

        /** Url used to stream the resource from the device. May be {@code null} if not available. */
        @Nullable
        private String replayUrl;

        /** Resource creation location. May be {@code null} if unknown. */
        @Nullable
        private Location gps;

        /** Resource width. */
        @IntRange(from = 0)
        private int width;

        /** Resource height. */
        @IntRange(from = 0)
        private int height;

        /** {@code true} when the resource contains thermal metadata, otherwise {@code false}. */
        private boolean thermal;

        /**
         * Private, default constructor used by GSon deserializer.
         */
        private Resource() {
        }

        /**
         * Constructor for use in tests.
         *
         * @param mediaId      media identifier
         * @param resourceId   resource identifier
         * @param type         resource type
         * @param format       resource format
         * @param date         resource creation date
         * @param size         resource size in bytes
         * @param duration     resource duration in milliseconds (for video)
         * @param url          resource download url
         * @param thumbnailUrl resource thumbnail url
         * @param replayUrl    url used to stream the resource from the device
         * @param location     resource creation location
         * @param width        resource width in pixels
         * @param height       resource height in pixels
         * @param thermal      thermal metadata presence
         */
        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        Resource(@Nullable String mediaId, @Nullable String resourceId, @Nullable Type type, @Nullable Format format,
                 @Nullable Date date, @IntRange(from = 0) long size, @IntRange(from = 0) int duration,
                 @Nullable String url, @Nullable String thumbnailUrl, @Nullable String replayUrl,
                 @Nullable Location location, @IntRange(from = 0) int width, @IntRange(from = 0) int height,
                 boolean thermal) {
            this.mediaId = mediaId;
            this.resourceId = resourceId;
            this.type = type;
            this.format = format;
            this.datetime = date;
            this.size = size;
            this.duration = duration;
            this.url = url;
            this.thumbnail = thumbnailUrl;
            this.replayUrl = replayUrl;
            this.gps = location;
            this.width = width;
            this.height = height;
            this.thermal = thermal;
        }

        /**
         * Retrieves the identifier of the media that owns this resource.
         *
         * @return resource media identifier
         */
        @Nullable
        public String getMediaId() {
            return mediaId;
        }

        /**
         * Retrieves the resource unique identifier.
         *
         * @return resource identifier
         */
        @Nullable
        public String getId() {
            return resourceId;
        }

        /**
         * Retrieves the resource type.
         *
         * @return resource type
         */
        @Nullable
        public Resource.Type getType() {
            return type;
        }

        /**
         * Retrieves the resource format.
         *
         * @return resource format
         */
        @Nullable
        public Resource.Format getFormat() {
            return format;
        }

        /**
         * Retrieves the resource creation date.
         *
         * @return resource creation date, in ISO 8601 base format
         */
        @Nullable
        public Date getDate() {
            return datetime;
        }

        /**
         * Retrieves the resource size.
         *
         * @return resource size, in bytes
         */
        @IntRange(from = 0)
        public long getSize() {
            return size;
        }

        /**
         * Retrieves the video duration.
         *
         * @return resource duration, in milliseconds (for video)
         */
        @IntRange(from = 0)
        public int getDuration() {
            return duration;
        }

        /**
         * Retrieves the resource relative URL for download.
         *
         * @return resource URL
         */
        @Nullable
        public String getUrl() {
            return url;
        }

        /**
         * Retrieves the resource thumbnail relative URL for download.
         *
         * @return thumbnail URL
         */
        @Nullable
        public String getThumbnailUrl() {
            return thumbnail;
        }

        /**
         * Retrieves the url used to stream the resource from the device.
         *
         * @return url used to stream the resource from the device if available, otherwise {@code null}
         */
        @Nullable
        public String getStreamUrl() {
            return replayUrl;
        }

        /**
         * Retrieves the resource creation location.
         *
         * @return resource creation location if known, otherwise {@code null}
         */
        @Nullable
        public Location getLocation() {
            return gps;
        }

        /**
         * Retrieves the resource width.
         *
         * @return resource width, in pixels
         */
        @IntRange(from = 0)
        public int getWidth() {
            return width;
        }

        /**
         * Retrieves the resource height.
         *
         * @return resource height, in pixels
         */
        @IntRange(from = 0)
        public int getHeight() {
            return height;
        }

        /**
         * Tells whether the resource contains thermal metadata.
         *
         * @return {@code true} in case the resource contains thermal metadata, otherwise {@code false}
         */
        public boolean hasThermalMetadata() {
            return thermal;
        }

        /**
         * Checks that this resource is valid.
         *
         * @return {@code true} if this resource is valid, otherwise {@code false}
         */
        public boolean isValid() {
            return mediaId != null && resourceId != null && type != null && format != null && datetime != null &&
                   size >= 0 && duration >= 0 && url != null && width >= 0 && height >= 0;
        }

        //region Parcelable

        /**
         * Constructor for parcel deserialization.
         *
         * @param parent media item parent
         * @param src    parcel to deserialize from
         */
        Resource(@NonNull HttpMediaItem parent, @NonNull Parcel src) {
            mediaId = parent.mediaId;
            resourceId = src.readString();
            type = Type.values()[src.readInt()];
            format = Format.values()[src.readInt()];
            datetime = new Date(src.readLong());
            size = src.readLong();
            duration = src.readInt();
            url = src.readString();
            thumbnail = src.readString();
            gps = src.readInt() == 0 ? null : new Location(src);
            width = src.readInt();
            height = src.readInt();
            replayUrl = src.readString();
            thermal = src.readInt() != 0;
        }

        /**
         * Serializes this resource to a parcel.
         *
         * @param dst parcel to serialize to
         */
        void writeToParcel(@NonNull Parcel dst) {
            dst.writeString(resourceId);
            assert type != null;
            dst.writeInt(type.ordinal());
            assert format != null;
            dst.writeInt(format.ordinal());
            assert datetime != null;
            dst.writeLong(datetime.getTime());
            dst.writeLong(size);
            dst.writeInt(duration);
            dst.writeString(url);
            dst.writeString(thumbnail);
            if (gps == null) {
                dst.writeInt(0);
            } else {
                dst.writeInt(1);
                gps.writeToParcel(dst);
            }
            dst.writeInt(width);
            dst.writeInt(height);
            dst.writeString(replayUrl);
            dst.writeInt(thermal ? 1 : 0);
        }

        //endregion
    }

    /** Media resources. */
    @Nullable
    private Resource[] resources;

    /**
     * Private, default constructor used by GSON deserializer.
     */
    private HttpMediaItem() {
    }

    /**
     * Constructor for use in tests.
     *
     * @param id            media id
     * @param type          media type
     * @param date          media creation date
     * @param size          media size
     * @param runId         media run id
     * @param expectedCount media expected resources count
     * @param thumbnailUrl  media thumbnail url
     * @param replayUrl     url used to stream the media from the device
     * @param location      media location
     * @param photoMode     media photo mode
     * @param panoramaType  media panorama type
     * @param thermal       thermal metadata presence
     * @param resources     media resources
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    HttpMediaItem(@Nullable String id, @Nullable Type type, @Nullable Date date, @IntRange(from = 0) long size,
                  @Nullable String runId, @IntRange(from = 0) int expectedCount, @Nullable String thumbnailUrl,
                  @Nullable String replayUrl, @Nullable Location location, @Nullable PhotoMode photoMode,
                  @Nullable PanoramaType panoramaType, boolean thermal, @NonNull List<Resource> resources) {
        this.mediaId = id;
        this.type = type;
        this.datetime = date;
        this.size = size;
        this.runId = runId;
        this.expectedCount = expectedCount;
        this.thumbnail = thumbnailUrl;
        this.replayUrl = replayUrl;
        this.gps = location;
        this.photoMode = photoMode;
        this.panoramaType = panoramaType;
        this.thermal = thermal;
        this.resources = resources.toArray(new Resource[0]);
    }

    /**
     * Retrieves the unique identifier of this media.
     *
     * @return media identifier
     */
    @Nullable
    public String getId() {
        return mediaId;
    }

    /**
     * Retrieves the media type.
     *
     * @return media type
     */
    @Nullable
    public Type getType() {
        return type;
    }

    /**
     * Retrieves the media creation date.
     *
     * @return media creation date, in ISO 8601 base format
     */
    @Nullable
    public Date getDate() {
        return datetime;
    }

    /**
     * Retrieves the media size.
     *
     * @return media size, in bytes
     */
    @IntRange(from = 0)
    public long getSize() {
        return size;
    }

    /**
     * Retrieves the run identifier of this media.
     *
     * @return media run identifier
     */
    @Nullable
    public String getRunId() {
        return runId;
    }

    /**
     * Retrieves the thumbnail relative URL for download.
     *
     * @return thumbnail URL
     */
    @Nullable
    public String getThumbnailUrl() {
        return thumbnail;
    }

    /**
     * Retrieves the url used to stream the media from the device.
     *
     * @return url used to stream the media from the device if available, otherwise {@code null}
     */
    @Nullable
    public String getStreamUrl() {
        return replayUrl;
    }

    /**
     * Retrieves the media creation location.
     *
     * @return media creation location if known, otherwise {@code null}
     */
    @Nullable
    public Location getLocation() {
        return gps;
    }

    /**
     * Retrieves the photo mode.
     *
     * @return photo mode
     */
    @Nullable
    public PhotoMode getPhotoMode() {
        return photoMode;
    }

    /**
     * Retrieves the panorama type.
     *
     * @return panorama type
     */
    @Nullable
    public PanoramaType getPanoramaType() {
        return panoramaType;
    }

    /**
     * Retrieves the expected number of resources in the media.
     *
     * @return expected resource count, {@code 0} if unknown
     */
    @IntRange(from = 0)
    public int getExpectedCount() {
        return expectedCount;
    }

    /**
     * Tells whether the media contains thermal metadata.
     *
     * @return {@code true} in case the media contains thermal metadata, otherwise {@code false}
     */
    public boolean hasThermalMetadata() {
        return thermal;
    }

    @NonNull
    @Override
    public Iterator<Resource> iterator() {
        return resources == null ?
                Collections.emptyIterator() : Stream.of(resources).filter(Objects::nonNull).iterator();
    }

    /**
     * Checks that this media item is valid.
     * <p>
     * An invalid item will be rejected.
     *
     * @return {@code true} if this item is valid, otherwise {@code false}
     */
    public boolean isValid() {
        boolean valid = mediaId != null && type != null && datetime != null && size >= 0 && runId != null
                        && resources != null && (type != Type.PHOTO ^ photoMode != null)
                        && (photoMode != PhotoMode.PANORAMA ^ panoramaType != null);
        if (valid) {
            for (int i = 0; i < resources.length && valid; i++) {
                Resource resource = resources[i];
                valid = resource == null || resource.isValid();
            }
        }
        return valid;
    }

    /** GSON adapter to convert between ISO 8601 base format and {@link java.util.Date}. */
    private static final class DateTimeParser extends TypeAdapter<Date> {

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        @Override
        public void write(@NonNull JsonWriter out, @NonNull Date value) throws IOException {
            out.value(Iso8601.toBaseDateAndTimeFormat(value));
        }

        @Override
        public Date read(@NonNull JsonReader in) throws IOException {
            try {
                return Iso8601.fromBaseDateAndTimeFormat(in.nextString());
            } catch (ParseException e) {
                return null;
            }
        }
    }

    //region Parcelable

    /**
     * Constructor for parcel deserialization.
     *
     * @param src parcel to deserialize from
     */
    public HttpMediaItem(@NonNull Parcel src) {
        mediaId = src.readString();
        type = Type.values()[src.readInt()];
        datetime = new Date(src.readLong());
        size = src.readLong();
        runId = src.readString();
        thumbnail = src.readString();
        gps = src.readInt() == 0 ? null : new Location(src);
        photoMode = type == Type.PHOTO ? PhotoMode.values()[src.readInt()] : null;
        panoramaType = photoMode == PhotoMode.PANORAMA ? PanoramaType.values()[src.readInt()] : null;
        expectedCount = src.readInt();
        replayUrl = src.readString();
        thermal = src.readInt() != 0;
        resources = new HttpMediaItem.Resource[src.readInt()];
        for (int i = 0; i < resources.length; i++) {
            resources[i] = new HttpMediaItem.Resource(this, src);
        }
    }

    /**
     * Serializes this media to a parcel.
     *
     * @param dst parcel to serialize to
     */
    public void writeToParcel(@NonNull Parcel dst) {
        dst.writeString(mediaId);
        assert type != null;
        dst.writeInt(type.ordinal());
        assert datetime != null;
        dst.writeLong(datetime.getTime());
        dst.writeLong(size);
        dst.writeString(runId);
        dst.writeString(thumbnail);
        if (gps == null) {
            dst.writeInt(0);
        } else {
            dst.writeInt(1);
            gps.writeToParcel(dst);
        }
        if (type == Type.PHOTO) {
            assert photoMode != null;
            dst.writeInt(photoMode.ordinal());
            if (photoMode == PhotoMode.PANORAMA) {
                assert panoramaType != null;
                dst.writeInt(panoramaType.ordinal());
            }
        }
        dst.writeInt(expectedCount);
        dst.writeString(replayUrl);
        dst.writeInt(thermal ? 1 : 0);

        Collection<Resource> nonNullResources = new ArrayList<>();
        forEach(nonNullResources::add);
        dst.writeInt(nonNullResources.size());
        nonNullResources.forEach(it -> it.writeToParcel(dst));
    }

    //endregion
}
