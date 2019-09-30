/*
 * Copyright (C) 2019 Parrot Drones SAS
 */

package com.parrot.drone.groundsdk.internal.stream;

import androidx.annotation.NonNull;

import com.parrot.drone.sdkcore.stream.SdkCoreMediaInfo;

/**
 * Allows to receive notification upon a stream's media info availability changes.
 *
 * @param <T> type of media info to listen to.
 */
interface MediaListener<T extends SdkCoreMediaInfo> {

    /**
     * Called back when the required media becomes available.
     *
     * @param mediaInfo info upon available media
     */
    void onMediaAvailable(@NonNull T mediaInfo);

    /**
     * Called back when the required media becomes unavailable.
     */
    void onMediaUnavailable();
}
