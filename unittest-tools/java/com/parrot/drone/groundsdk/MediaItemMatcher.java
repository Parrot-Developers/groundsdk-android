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

package com.parrot.drone.groundsdk;

import android.location.Location;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;

import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;

import static com.parrot.drone.groundsdk.LocationMatcher.locationIs;
import static com.parrot.drone.groundsdk.MatcherBuilders.enumSetFeatureMatcher;
import static com.parrot.drone.groundsdk.MatcherBuilders.featureMatcher;
import static com.parrot.drone.groundsdk.MatcherBuilders.valueMatcher;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;

public final class MediaItemMatcher {

    public static Matcher<MediaItem> hasUid(@NonNull String uid) {
        return valueMatcher(uid, "Uid", MediaItem::getUid);
    }

    public static Matcher<MediaItem> hasType(@NonNull MediaItem.Type type) {
        return valueMatcher(type, "Type", MediaItem::getType);
    }

    public static Matcher<MediaItem> hasName(@NonNull String name) {
        return valueMatcher(name, "Name", MediaItem::getName);
    }

    public static Matcher<MediaItem> hasRunId(@Nullable String runId) {
        return valueMatcher(runId, "RunId", MediaItem::getRunUid);
    }

    public static Matcher<MediaItem> hasExpectedCount(@IntRange(from = 0) int count) {
        return valueMatcher(count, "ExpectedCount", MediaItem::getExpectedResourceCount);
    }

    public static Matcher<MediaItem> hasDate(@NonNull Date date) {
        return valueMatcher(date, "CreationDate", MediaItem::getCreationDate);
    }

    public static Matcher<MediaItem> hasPhotoMode(@Nullable MediaItem.PhotoMode photoMode) {
        return valueMatcher(photoMode, "PhotoMode", MediaItem::getPhotoMode);
    }

    public static Matcher<MediaItem> hasPanoramaType(@Nullable MediaItem.PanoramaType panoramaType) {
        return valueMatcher(panoramaType, "PanoramaType", MediaItem::getPanoramaType);
    }

    public static Matcher<MediaItem> containsMetadata(@NonNull EnumSet<MediaItem.MetadataType> metadata) {
        return enumSetFeatureMatcher(metadata, "Metadata", MediaItem::getAvailableMetadata);
    }

    @SafeVarargs
    public static Matcher<MediaItem> containsResources(Matcher<MediaItem.Resource>... resourceMatchers) {
        return featureMatcher(containsInAnyOrder(resourceMatchers), "Resources", MediaItem::getResources);
    }

    public static Matcher<MediaItem> containsResources(
            @NonNull Collection<Matcher<? super MediaItem.Resource>> resourceMatchers) {
        return featureMatcher(containsInAnyOrder(resourceMatchers), "Resources", MediaItem::getResources);
    }

    public static Matcher<MediaItem.Resource> resourceHasUid(@NonNull String uid) {
        return valueMatcher(uid, "Uid", MediaItem.Resource::getUid);
    }

    public static Matcher<MediaItem.Resource> hasParent(@NonNull MediaItem parent) {
        return valueMatcher(parent, "Parent", MediaItem.Resource::getMedia);
    }

    public static Matcher<MediaItem.Resource> hasFormat(@NonNull MediaItem.Resource.Format format) {
        return valueMatcher(format, "Format", MediaItem.Resource::getFormat);
    }

    public static Matcher<MediaItem.Resource> hasSize(long size) {
        return valueMatcher(size, "Size", MediaItem.Resource::getSize);
    }

    public static Matcher<MediaItem.Resource> hasDuration(long duration) {
        return valueMatcher(duration, "Duration", MediaItem.Resource::getDuration);
    }

    public static Matcher<MediaItem.Resource> resourceHasDate(@NonNull Date date) {
        return valueMatcher(date, "CreationDate", MediaItem.Resource::getCreationDate);
    }

    public static Matcher<MediaItem.Resource> resourceHasLocationSuchAs(
            @NonNull Matcher<? super Location> locationMatcher) {
        return featureMatcher(locationMatcher, "Location", MediaItem.Resource::getLocation);
    }

    public static Matcher<MediaItem.Resource> resourceContainsTracks(@NonNull EnumSet<MediaItem.Track> tracks) {
        return enumSetFeatureMatcher(tracks, "Tracks", MediaItem.Resource::getAvailableTracks);
    }

    public static Matcher<MediaItem.Resource> resourceContainsMetadata(
            @NonNull EnumSet<MediaItem.MetadataType> metadata) {
        return enumSetFeatureMatcher(metadata, "Metadata", MediaItem.Resource::getAvailableMetadata);
    }

    public static Matcher<MediaItem.Resource> resourceEquals(@NonNull MediaItem.Resource resource) {
        Location location = resource.getLocation();
        return allOf(
                resourceHasUid(resource.getUid()),
                hasParent(resource.getMedia()),
                hasFormat(resource.getFormat()),
                hasSize(resource.getSize()),
                hasDuration(resource.getDuration()),
                resourceHasDate(resource.getCreationDate()),
                resourceHasLocationSuchAs(location == null ? nullValue() :
                        locationIs(location.getLatitude(), location.getLongitude(), location.getAltitude())),
                resourceContainsMetadata(resource.getAvailableMetadata()),
                resourceContainsTracks(resource.getAvailableTracks()));
    }

    public static Matcher<MediaItem> mediaEquals(@NonNull MediaItem media) {
        Collection<Matcher<? super MediaItem.Resource>> resourcesMatchers = new ArrayList<>();
        for (MediaItem.Resource resource : media.getResources()) {
            resourcesMatchers.add(resourceEquals(resource));
        }
        return allOf(
                hasUid(media.getUid()),
                hasType(media.getType()),
                hasName(media.getName()),
                hasRunId(media.getRunUid()),
                hasDate(media.getCreationDate()),
                hasExpectedCount(media.getExpectedResourceCount()),
                hasPhotoMode(media.getPhotoMode()),
                hasPanoramaType(media.getPanoramaType()),
                containsResources(resourcesMatchers));
    }

    private MediaItemMatcher() {
    }
}
