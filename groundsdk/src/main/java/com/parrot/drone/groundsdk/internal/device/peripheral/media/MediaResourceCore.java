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

import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core extension for {@code MediaItem.Resource}.
 */
public interface MediaResourceCore extends MediaItem.Resource {

    /**
     * Unwraps a media resource to its internal {@code MediaResourceCore} representation.
     *
     * @param resource media resource to unwrap
     *
     * @return internal {@code MediaResourceCore} representation of the specified media resource
     *
     * @throws IllegalArgumentException in case the provided media resource is not based upon {@code MediaResourceCore}
     */
    @NonNull
    static MediaResourceCore unwrap(@NonNull MediaItem.Resource resource) {
        try {
            return (MediaResourceCore) resource;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid MediaItem.Resource: " + resource, e);
        }
    }

    /**
     * Unwraps a collection of media resource(s) to their {@code MediaResourceCore} representation.
     * <p>
     * The unwrapped resource collection is then further organized into a map that associates each resource's media
     * item parent to the subset of resource(s) from the provided collection that this media item contains.
     * <p>
     * Grouping requirements aside, map keys (media items) respect the order in which consecutive media items appear
     * in the provided collection; map values (media resource sets) respect the order in which consecutive media
     * resources appear in the provided collection.
     *
     * @param resources media resources to unwrap
     *
     * @return a map associating unwrapped media items to the subset of unwrapped media resources from the provided
     *         collection that each media contains
     *
     * @throws IllegalArgumentException in case any of the provided media resource(s) is not based upon
     *                                  {@code MediaResourceCore}
     */
    @NonNull
    static Map<MediaItemCore, Set<MediaResourceCore>> unwrapAsMap(@NonNull Collection<MediaItem.Resource> resources) {
        return resources.stream().map(MediaResourceCore::unwrap).collect(Collectors.groupingBy(
                MediaResourceCore::getMedia, LinkedHashMap::new, Collectors.toCollection(LinkedHashSet::new)));
    }

    /**
     * Retrieves the resource's media item parent.
     *
     * @return resource media item parent
     */
    @Override
    @NonNull
    MediaItemCore getMedia();

    /**
     * Retrieves the resource's stream URL.
     *
     * @return resource stream URL, if any, otherwise {@code null}
     */
    @Nullable
    String getStreamUrl();

    /**
     * Retrieves the stream identifier of a track from this resource.
     *
     * @param track track to get an identifier for
     *
     * @return track stream identifier, may be {@code null} for {@link MediaItem.Track#DEFAULT_VIDEO}
     *
     * @throws IllegalArgumentException in case this resource does not contain specified track
     */
    @Nullable
    String getStreamTrackIdFor(@NonNull MediaItem.Track track);
}