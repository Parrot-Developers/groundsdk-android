/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

package com.parrot.drone.groundsdk.internal.device;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Hold an observable device board identifier instance.
 */
final class DeviceBoardIdHolder {

    /** Observer notified when the held device board id changes. */
    public interface Observer {

        /**
         * Notifies that the device board id has changed.

         * @param boardId new board id, or null if holder disposed
         */
        void onChange(@NonNull String boardId);
    }

    /** Device board id, {@code null} if not queried yet, empty if known to be unavailable. */
    @Nullable
    private String mBoardId;

    /** Observers. */
    @NonNull
    private final Set<Observer> mObservers;

    /**
     * Constructor.
     */
    DeviceBoardIdHolder() {
        mObservers = new CopyOnWriteArraySet<>();
    }

    /**
     * Registers an observer.
     *
     * @param observer observer to register
     */
    void registerObserver(@NonNull Observer observer) {
        mObservers.add(observer);
    }

    /**
     * Unregisters an observer.
     *
     * @param observer observer to unregister
     */
    @SuppressWarnings("unused")
    void unregisterObserver(@NonNull Observer observer) {
        mObservers.remove(observer);
    }

    /**
     * Gets the held board identifier.
     *
     * @return device board id
     */
    @Nullable
    String get() {
        return mBoardId;
    }

    /**
     * Updates the device board id.
     *
     * @param boardId new board id
     */
    void update(@NonNull String boardId) {
        if (!Objects.equals(mBoardId, boardId)) {
            mBoardId = boardId;
            for (Observer observer : mObservers) {
                observer.onChange(mBoardId);
            }
        }
    }

    /**
     * Destroys the holder.
     */
    void destroy() {
        mObservers.clear();
    }
}
