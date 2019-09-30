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

package com.parrot.drone.groundsdk.device.peripheral.camera;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;

/**
 * Camera exposure lock.
 * <p>
 * Allows to lock/unlock the exposure according to a given mode.
 */
public interface CameraExposureLock {

    /** Exposure lock mode. */
    enum Mode {

        /** No exposure lock. */
        NONE,

        /** Lock current exposure values. */
        CURRENT_VALUES,

        /** Lock exposure on a given region of interest. */
        REGION
    }

    /**
     * Tells whether mode has been changed and is waiting for change confirmation.
     *
     * @return {@code true} when mode has been changed and is waiting for confirmation
     */
    boolean isUpdating();

    /**
     * Gets current lock mode.
     *
     * @return current lock mode
     */
    @NonNull
    Mode mode();

    /**
     * Horizontal center of region of interest where exposure is locked.
     * <p>
     * Relative position, from left (0.0) to right (1.0).
     * <p>
     * Only when mode is {@link Mode#REGION}.
     *
     * @return horizontal center of region of interest where exposure is locked
     */
    @FloatRange(from = 0.0, to = 1.0)
    double getRegionCenterX();

    /**
     * Vertical center of region of interest where exposure is locked.
     * <p>
     * Relative position, from bottom (0.0) to top (1.0).
     * <p>
     * Only when mode is {@link Mode#REGION}.
     *
     * @return vertical center of region of interest where exposure is locked
     */
    @FloatRange(from = 0.0, to = 1.0)
    double getRegionCenterY();

    /**
     * Width of region of interest where exposure is locked.
     * <p>
     * Relative to video width, from 0.0 to 1.0.
     * <p>
     * Only when mode is {@link Mode#REGION}.
     *
     * @return width of region of interest where exposure is locked
     */
    @FloatRange(from = 0.0, to = 1.0)
    double getRegionWidth();

    /**
     * Height of region of interest where exposure is locked.
     * <p>
     * Relative to video height, from 0.0 to 1.0.
     * <p>
     * Only when mode is {@link Mode#REGION}.
     *
     * @return height of region of interest where exposure is locked
     */
    @FloatRange(from = 0.0, to = 1.0)
    double getRegionHeight();

    /**
     * Requests lock of exposure on current exposure values.
     */
    void lockCurrentValues();

    /**
     * Requests lock of exposure on a given region of interest (taken from the video stream).
     *
     * @param centerX horizontal center in the video (relative position, from left (0.0) to right (1.0))
     * @param centerY vertical center in the video (relative position, from bottom (0.0) to top (1.0))
     */
    void lockOnRegion(@FloatRange(from = 0.0, to = 1.0) double centerX,
                      @FloatRange(from = 0.0, to = 1.0) double centerY);

    /**
     * Requests exposure unlock.
     */
    void unlock();
}
