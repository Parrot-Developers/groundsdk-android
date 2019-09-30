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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

/**
 * A media event, as received from the drone HTTP media service.
 * <p>
 * Field names do not respect coding rules to map exactly the field names in the received JSON from which they are
 * parsed.
 * <p>
 * Private default constructors must be kept as they are used by GSON parser.
 */
@SuppressWarnings({"NullableProblems", "unused"})
class HttpMediaEvent {

    /** Media event type. */
    public enum Type {

        /** A media has been created. */
        @SerializedName("media_created")
        MEDIA_CREATED,

        /** A media has been deleted. */
        @SerializedName("media_removed")
        MEDIA_REMOVED,

        /** All media have been deleted. */
        @SerializedName("all_media_removed")
        ALL_MEDIA_REMOVED,

        /** A resource has been created. */
        @SerializedName("resource_created")
        RESOURCE_CREATED,

        /** A resource has been deleted. */
        @SerializedName("resource_removed")
        RESOURCE_REMOVED,

        /** Indexing state changed. */
        @SerializedName("indexing_state_changed")
        INDEXING_STATE_CHANGED
    }

    /** Event type. */
    @Nullable
    private Type name;

    /**
     * Retrieves the event type.
     *
     * @return event type
     */
    @Nullable
    final Type getType() {
        return name;
    }

    /**
     * Private, default constructor used by GSON deserializer.
     */
    private HttpMediaEvent() {
    }

    /**
     * Media created event, as received from the drone HTTP media service.
     */
    static final class MediaCreated extends HttpMediaEvent {

        /** Event data, containing the created media item. */
        private static final class Data {

            /** Created media item. */
            @NonNull
            private HttpMediaItem media;
        }

        /** Event data. */
        @NonNull
        private Data data;

        /**
         * Private, default constructor used by GSON deserializer.
         */
        private MediaCreated() {
        }

        /**
         * Constructor for use in tests.
         *
         * @param item media item
         */
        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        MediaCreated(@NonNull HttpMediaItem item) {
            ((HttpMediaEvent) this).name = Type.MEDIA_CREATED;
            data = new Data();
            data.media = item;
        }

        /**
         * Retrieves the created media item.
         *
         * @return the created media item
         */
        @NonNull
        public HttpMediaItem getMedia() {
            return data.media;
        }
    }

    /**
     * Media deleted event, as received from the drone HTTP media service.
     */
    static final class MediaDeleted extends HttpMediaEvent {

        /** Event data, containing the deleted media id. */
        private static final class Data {

            /** Deleted media identifier. */
            @NonNull
            private String mediaId;
        }

        /** Event data. */
        @NonNull
        private Data data;

        /**
         * Private, default constructor used by GSON deserializer.
         */
        private MediaDeleted() {
        }

        /**
         * Constructor for use in tests.
         *
         * @param id media item identifier
         */
        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        MediaDeleted(@NonNull String id) {
            ((HttpMediaEvent) this).name = Type.MEDIA_REMOVED;
            data = new Data();
            data.mediaId = id;
        }

        /**
         * Retrieves the unique identifier of the deleted media.
         *
         * @return the deleted media identifier
         */
        @NonNull
        public String getId() {
            return data.mediaId;
        }
    }

    /**
     * All media deleted event, for use in tests.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    static final class AllMediaDeleted extends HttpMediaEvent {

        /**
         * Constructor.
         */
        AllMediaDeleted() {
            ((HttpMediaEvent) this).name = Type.ALL_MEDIA_REMOVED;
        }
    }

    /**
     * Resource created event, as received from the drone HTTP media service.
     */
    static final class ResourceCreated extends HttpMediaEvent {

        /** Event data, containing the created resource item. */
        private static final class Data {

            /** Created resource. */
            @NonNull
            private HttpMediaItem.Resource resource;
        }

        /** Event data. */
        @NonNull
        private Data data;

        /**
         * Private, default constructor used by GSON deserializer.
         */
        private ResourceCreated() {
        }

        /**
         * Constructor for use in tests.
         *
         * @param resource the resource
         */
        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        ResourceCreated(@NonNull HttpMediaItem.Resource resource) {
            ((HttpMediaEvent) this).name = Type.RESOURCE_CREATED;
            data = new Data();
            data.resource = resource;
        }

        /**
         * Retrieves the created resource.
         *
         * @return the created resource
         */
        @NonNull
        public HttpMediaItem.Resource getResource() {
            return data.resource;
        }
    }

    /**
     * Resource deleted event, as received from the drone HTTP media service.
     */
    static final class ResourceDeleted extends HttpMediaEvent {

        /** Event data, containing the deleted resource id. */
        private static final class Data {

            /** Deleted resource identifier. */
            @NonNull
            private String resourceId;
        }

        /** Event data. */
        @NonNull
        private Data data;

        /**
         * Private, default constructor used by GSON deserializer.
         */
        private ResourceDeleted() {
        }

        /**
         * Constructor for use in tests.
         *
         * @param id resource identifier
         */
        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        ResourceDeleted(@NonNull String id) {
            ((HttpMediaEvent) this).name = Type.RESOURCE_REMOVED;
            data = new Data();
            data.resourceId = id;
        }

        /**
         * Retrieves the unique identifier of the deleted resource.
         *
         * @return the deleted resource identifier
         */
        @NonNull
        public String getId() {
            return data.resourceId;
        }
    }

    /**
     * Media indexing state change event, as received from the drone HTTP media service.
     */
    static final class IndexingStateChanged extends HttpMediaEvent {

        /** Event data, containing the former and new indexing states. */
        private static final class Data {

            /** Former indexing state. */
            @NonNull
            private HttpMediaIndexingState oldState;

            /** New indexing state. */
            @NonNull
            private HttpMediaIndexingState newState;
        }

        /** Event data. */
        @NonNull
        private final Data data;

        /**
         * Constructor for use in tests.
         *
         * @param formerState former indexing state
         * @param newState    new indexing state
         */
        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        IndexingStateChanged(@NonNull HttpMediaIndexingState formerState, @NonNull HttpMediaIndexingState newState) {
            ((HttpMediaEvent) this).name = Type.INDEXING_STATE_CHANGED;
            data = new IndexingStateChanged.Data();
            data.oldState = formerState;
            data.newState = newState;
        }


        /**
         * Retrieves the current indexing state.
         *
         * @return current indexing state
         */
        @NonNull
        public HttpMediaIndexingState getCurrentState() {
            return data.newState;
        }

        /**
         * Retrieves the former indexing state.
         *
         * @return former indexing state
         */
        @NonNull
        public HttpMediaIndexingState getFormerState() {
            return data.oldState;
        }
    }
}
