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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.stream.SdkCoreMediaInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Performs bookkeeping of available media kinds on a stream and allows to subscribe to such media availability events.
 */
final class MediaRegistry {

    /** Media infos, by media id. */
    @NonNull
    private final Map<Long, SdkCoreMediaInfo> mMedias;

    /** Media listeners, by media kink. */
    @NonNull
    private final Map<Class<? extends SdkCoreMediaInfo>, Set<MediaListener<? extends SdkCoreMediaInfo>>> mListeners;

    /**
     * Constructor.
     */
    @SuppressLint("UseSparseArrays")
    MediaRegistry() {
        mMedias = new HashMap<>();
        mListeners = new HashMap<>();
    }

    /**
     * Adds a media.
     * <p>
     * Only one media of each kind may be registered at any time. In case a media of the same kind is already
     * registered, then it is removed, subscribed listeners being notified of that event, and finally the new media
     * is added.
     * <p>
     * Subscribed listeners are notified that a new media is available.
     *
     * @param info info on the added media
     */
    void addMedia(@NonNull SdkCoreMediaInfo info) {
        Class<? extends SdkCoreMediaInfo> kind = info.getClass();

        if (mMedias.values().removeIf(kind::isInstance)) {
            notifyMediaUnavailable(kind);
        }

        mMedias.put(info.mediaId(), info);
        notifyMediaAvailable(kind, info);
    }

    /**
     * Removes a media.
     * <p>
     * Subscribed listener are notified that the media is not available anymore.
     *
     * @param mediaId identifies the removed media
     */
    void removeMedia(long mediaId) {
        SdkCoreMediaInfo mediaInfo = mMedias.remove(mediaId);
        if (mediaInfo != null) {
            notifyMediaUnavailable(mediaInfo.getClass());
        }
    }

    /**
     * Registers a media kind listener.
     * <p>
     * In case a media of the requested kind is available when this method is called,
     * {@code listener.}{@link MediaListener#onMediaAvailable(SdkCoreMediaInfo) onMediaAvailable()} is called
     * immediately.
     *
     * @param mediaKind kind of media to subscribe to
     * @param listener  listener notified of media availability changes
     * @param <T>       type of media class
     */
    <T extends SdkCoreMediaInfo> void registerListener(@NonNull Class<T> mediaKind, MediaListener<? super T> listener) {
        Set<MediaListener<? extends SdkCoreMediaInfo>> listeners = mListeners.get(mediaKind);
        if (listeners == null) {
            listeners = new CopyOnWriteArraySet<>();
            mListeners.put(mediaKind, listeners);
        }

        listeners.add(listener);

        mMedias.values()
               .stream()
               .filter(mediaKind::isInstance)
               .findFirst()
               .ifPresent(it -> {
                   @SuppressWarnings("unchecked") T media = (T) it;
                   listener.onMediaAvailable(media);
               });
    }

    /**
     * Unregisters a media listener.
     * <p>
     * In case a media of the subscribed kind is still available when this method is called,
     * {@code listener.}{@link MediaListener#onMediaUnavailable()} onMediaUnavailable()} is called immediately.
     *
     * @param listener listener to unsubscribe
     */
    void unregisterListener(@NonNull MediaListener<?> listener) {
        for (Iterator<Class<? extends SdkCoreMediaInfo>> iter = mListeners.keySet().iterator(); iter.hasNext(); ) {
            Class<? extends SdkCoreMediaInfo> kind = iter.next();
            Set<MediaListener<?>> listeners = mListeners.get(kind);
            assert listeners != null; // we never put null values in this map
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                iter.remove();
            }

            // TODO: not sure if appropriate
            if (mMedias.values().stream().anyMatch(kind::isInstance)) {
                listener.onMediaUnavailable();
            }
        }
    }

    /**
     * Notifies subscribed listeners that a media is available.
     *
     * @param kind kind of available media
     * @param info info on the available media
     * @param <T>  type of media class
     */
    private <T extends SdkCoreMediaInfo> void notifyMediaAvailable(@NonNull Class<? extends T> kind, T info) {
        // listeners always map to the proper kind
        @SuppressWarnings("unchecked")
        Set<MediaListener<T>> listeners = (Set) mListeners.get(kind);
        if (listeners != null) for (MediaListener<T> listener : listeners) {
            listener.onMediaAvailable(info);
        }
    }

    /**
     * Notifies subscribed listeners that a media is unavailable.
     *
     * @param kind kind of unavailable media
     */
    private void notifyMediaUnavailable(@NonNull Class<? extends SdkCoreMediaInfo> kind) {
        Set<MediaListener<?>> listeners = mListeners.get(kind);
        if (listeners != null) for (MediaListener<?> listener : listeners) {
            listener.onMediaUnavailable();
        }
    }
}
