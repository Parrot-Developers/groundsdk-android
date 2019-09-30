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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static com.parrot.drone.groundsdk.MatcherBuilders.featureMatcher;
import static com.parrot.drone.groundsdk.MatcherBuilders.valueMatcher;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;

@SuppressWarnings("WeakerAccess")
final class HttpMediaItemMatcher {

    static Matcher<HttpMediaItem> mediaTypeIs(@Nullable HttpMediaItem.Type type) {
        return valueMatcher(type, "mediaType", HttpMediaItem::getType);
    }

    static Matcher<HttpMediaItem> mediaIdIs(@Nullable String id) {
        return valueMatcher(id, "mediaId", HttpMediaItem::getId);
    }

    static Matcher<HttpMediaItem> mediaRunIdIs(@Nullable String id) {
        return valueMatcher(id, "runId", HttpMediaItem::getRunId);
    }

    static Matcher<HttpMediaItem> mediaDateIs(@Nullable Date date) {
        return valueMatcher(date, "date", HttpMediaItem::getDate);
    }

    static Matcher<HttpMediaItem> mediaExpectedCountIs(@IntRange(from = 0) int count) {
        return valueMatcher(count, "expectedCount", HttpMediaItem::getExpectedCount);
    }

    static Matcher<HttpMediaItem> mediaThumbnailUrlIs(@Nullable String thumbnailUrl) {
        return valueMatcher(thumbnailUrl, "thumbnailUrl", HttpMediaItem::getThumbnailUrl);
    }

    static Matcher<HttpMediaItem> mediaStreamUrlIs(@Nullable String streamUrl) {
        return valueMatcher(streamUrl, "streamUrl", HttpMediaItem::getStreamUrl);
    }

    static Matcher<HttpMediaItem> mediaThermalPresenceIs(boolean present) {
        return valueMatcher(present, "thermal", HttpMediaItem::hasThermalMetadata);
    }

    static Matcher<HttpMediaItem> mediaLocationMatching(
            @NonNull Matcher<? super HttpMediaItem.Location> locationMatcher) {
        return featureMatcher(locationMatcher, "location", HttpMediaItem::getLocation);
    }

    static Matcher<HttpMediaItem> mediaLocationIs(@Nullable HttpMediaItem.Location location) {
        return mediaLocationMatching(locationEquals(location));
    }

    static Matcher<HttpMediaItem> mediaResourcesMatching(
            @NonNull Collection<Matcher<? super HttpMediaItem.Resource>> resourcesMatchers) {
        return featureMatcher(containsInAnyOrder(resourcesMatchers), "resources", it -> it);
    }

    static Matcher<HttpMediaItem> mediaEquals(@NonNull HttpMediaItem media) {
        Collection<Matcher<? super HttpMediaItem.Resource>> resourcesMatchers = new ArrayList<>();
        for (HttpMediaItem.Resource resource : media) {
            resourcesMatchers.add(mediaResourceEquals(resource));
        }
        return allOf(
                mediaTypeIs(media.getType()),
                mediaIdIs(media.getId()),
                mediaRunIdIs(media.getRunId()),
                mediaDateIs(media.getDate()),
                mediaExpectedCountIs(media.getExpectedCount()),
                mediaThumbnailUrlIs(media.getThumbnailUrl()),
                mediaStreamUrlIs(media.getStreamUrl()),
                mediaLocationIs(media.getLocation()),
                mediaThermalPresenceIs(media.hasThermalMetadata()),
                mediaResourcesMatching(resourcesMatchers));
    }

    static Matcher<Iterable<? extends HttpMediaItem>> mediaListEquals(@NonNull Collection<HttpMediaItem> list) {
        Collection<Matcher<? super HttpMediaItem>> matchers = new ArrayList<>();
        for (HttpMediaItem media : list) {
            matchers.add(mediaEquals(media));
        }
        return containsInAnyOrder(matchers);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceTypeIs(@Nullable HttpMediaItem.Resource.Type type) {
        return valueMatcher(type, "resourceType", HttpMediaItem.Resource::getType);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceFormatIs(
            @Nullable HttpMediaItem.Resource.Format format) {
        return valueMatcher(format, "format", HttpMediaItem.Resource::getFormat);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceMediaIdIs(@Nullable String id) {
        return valueMatcher(id, "resourceMediaId", HttpMediaItem.Resource::getMediaId);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceIdIs(@Nullable String id) {
        return valueMatcher(id, "resourceId", HttpMediaItem.Resource::getId);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceSizeIs(long size) {
        return valueMatcher(size, "size", HttpMediaItem.Resource::getSize);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceDurationIs(int duration) {
        return valueMatcher(duration, "duration", HttpMediaItem.Resource::getDuration);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceDateIs(@Nullable Date date) {
        return valueMatcher(date, "date", HttpMediaItem.Resource::getDate);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceUrlIs(@Nullable String url) {
        return valueMatcher(url, "url", HttpMediaItem.Resource::getUrl);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceThumbnailUrlIs(@Nullable String thumbnailUrl) {
        return valueMatcher(thumbnailUrl, "thumbnailUrl", HttpMediaItem.Resource::getThumbnailUrl);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceLocationMatching(
            @NonNull Matcher<? super HttpMediaItem.Location> locationMatcher) {
        return featureMatcher(locationMatcher, "location", HttpMediaItem.Resource::getLocation);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceLocationIs(@Nullable HttpMediaItem.Location location) {
        return mediaResourceLocationMatching(locationEquals(location));
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceStreamUrlIs(@Nullable String streamUrl) {
        return valueMatcher(streamUrl, "streamUrl", HttpMediaItem.Resource::getStreamUrl);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceWidthIs(int width) {
        return valueMatcher(width, "width", HttpMediaItem.Resource::getWidth);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceHeightIs(int height) {
        return valueMatcher(height, "height", HttpMediaItem.Resource::getHeight);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceThermalPresenceIs(boolean present) {
        return valueMatcher(present, "thermal", HttpMediaItem.Resource::hasThermalMetadata);
    }

    static Matcher<HttpMediaItem.Resource> mediaResourceEquals(@NonNull HttpMediaItem.Resource resource) {
        return allOf(
                mediaResourceTypeIs(resource.getType()),
                mediaResourceFormatIs(resource.getFormat()),
                mediaResourceMediaIdIs(resource.getMediaId()),
                mediaResourceIdIs(resource.getId()),
                mediaResourceSizeIs(resource.getSize()),
                mediaResourceDurationIs(resource.getDuration()),
                mediaResourceDateIs(resource.getDate()),
                mediaResourceLocationIs(resource.getLocation()),
                mediaResourceUrlIs(resource.getUrl()),
                mediaResourceThumbnailUrlIs(resource.getThumbnailUrl()),
                mediaResourceStreamUrlIs(resource.getStreamUrl()),
                mediaResourceWidthIs(resource.getWidth()),
                mediaResourceHeightIs(resource.getHeight()),
                mediaResourceThermalPresenceIs(resource.hasThermalMetadata()));
    }

    private static Matcher<HttpMediaItem.Location> locationEquals(@Nullable HttpMediaItem.Location location) {
        return location == null ? nullValue(HttpMediaItem.Location.class) : allOf(
                locationLatitudeIs(location.getLatitude()),
                locationLongitudeIs(location.getLongitude()),
                locationAltitudeIs(location.getAltitude()));
    }

    private static Matcher<HttpMediaItem.Location> locationLatitudeIs(double latitude) {
        return valueMatcher(latitude, "latitude", HttpMediaItem.Location::getLatitude);
    }

    private static Matcher<HttpMediaItem.Location> locationLongitudeIs(double longitude) {
        return valueMatcher(longitude, "longitude", HttpMediaItem.Location::getLongitude);
    }

    private static Matcher<HttpMediaItem.Location> locationAltitudeIs(double altitude) {
        return valueMatcher(altitude, "altitude", HttpMediaItem.Location::getAltitude);
    }

    private HttpMediaItemMatcher() {
    }
}
