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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.value.BooleanSetting;
import com.parrot.drone.groundsdk.value.DoubleSetting;

/**
 * Scoping class for camera zoom.
 * <p>
 * Provides access to the camera zoom in order to get its properties and change the zoom level.
 */
public interface CameraZoom {

    /** Way of controlling the gimbal. */
    enum ControlMode {

        /** Controls the zoom giving level targets. */
        LEVEL,

        /** Controls the zoom giving velocity targets. */
        VELOCITY
    }

    /**
     * Gives access to the maximum zoom speed setting, in tan(deg)/second.
     *
     * @return maximum zoom speed setting
     */
    @NonNull
    DoubleSetting maxSpeed();

    /**
     * Gives access to the zoom velocity quality degradation allowance setting.
     * <p>
     * This setting allows to set whether or not zoom level change using zoom velocity will stop at the
     * {@link #getMaxLossyLevel() max lossy zoom level} or at the {@link #getMaxLossLessLevel() max loss less
     * zoom level} to avoid image quality degradation. If quality degradation is not allowed, it will stop at
     * {@link #getMaxLossLessLevel() max loss less zoom level}.
     *
     * @return zoom velocity quality degradation allowance setting
     */
    @NonNull
    BooleanSetting velocityQualityDegradationAllowance();

    /**
     * Gets the current zoom level, in focal length factor.
     * <p>
     * The returned value ranges from 1 (no zoom) to {@link #getMaxLossyLevel() max lossy zoom level}.
     * <p>
     * <strong>Note:</strong> zoom level can be changed either by specifying a new factor
     * or by specifying a zoom change velocity with {@link #control(ControlMode, double)}.
     *
     * @return current zoom level
     */
    double getCurrentLevel();

    /**
     * Gets the maximum zoom level available on the device.
     * <p>
     * <strong>Note:</strong> from {@link #getMaxLossLessLevel() max loss less zoom level} to this value, image
     * quality is deteriorated.
     *
     * @return maximum (lossy) zoom level
     */
    double getMaxLossyLevel();

    /**
     * Gets the maximum zoom level to keep image quality at its best.
     * <p>
     * <strong>Note:</strong> if zoom level is greater than this value, image quality will be deteriorated.
     *
     * @return maximum loss less zoom level
     */
    double getMaxLossLessLevel();

    /**
     * Tells whether zoom is available.
     *
     * @return {@code true} if zoom is available, otherwise {@code false}
     */
    boolean isAvailable();

    /**
     * Controls the zoom.
     * <p>
     * Unit of the `target` depends on the value of the `mode` parameter:
     * <ul>
     * <li>LEVEL: target is in zoom level. 1 means no zoom.</li>
     * <li>VELOCITY: value is in signed ratio (from -1 to 1) of {@link #maxSpeed() maxSpeed setting } value.
     * Negative values will produce a zoom out, positive value will zoom in.</li>
     * </ul>
     *
     * @param mode   the mode that should be used to control the zoom.
     * @param target either level or velocity zoom target.
     */
    void control(@NonNull ControlMode mode, double target);
}
