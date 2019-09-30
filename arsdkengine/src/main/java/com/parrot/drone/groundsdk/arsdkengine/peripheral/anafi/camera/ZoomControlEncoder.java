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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.camera.CameraZoom;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkNoAckCmdEncoder;

/** Manages zoom control and encodes zoom control commands to be sent by no-ack command loop. */
final class ZoomControlEncoder implements ArsdkNoAckCmdEncoder {

    /** Zoom control encoder backend, which encodes zoom commands to be sent. */
    interface CommandEncoder {

        /**
         * Encodes zoom control parameters into a command to be sent to the drone.
         *
         * @param controlMode zoom control mode
         * @param target      zoom control target
         *
         * @return specified parameters properly encoded in an {@code ArsdkCommand}
         */
        @NonNull
        ArsdkCommand encodeZoomControl(@NonNull CameraZoom.ControlMode controlMode, double target);
    }

    /** Maximum number of time the zoom command should be sent with the same value. */
    private static final int ZOOM_COMMANDS_REPETITIONS = 10;

    /** Encodes zoom control parameters as commands to be sent to the drone. */
    @NonNull
    private final CommandEncoder mEncoder;

    /** Requested control mode. Access from main and pomp thread, under {@link ZoomControlEncoder} instance lock. */
    @NonNull
    private CameraZoom.ControlMode mControlMode;

    /** Requested target. Access from main and pomp thread, under {@link ZoomControlEncoder} instance lock. */
    private double mTarget;

    /** Latest sent control mode. Accessed only from pomp thread. */
    @NonNull
    private CameraZoom.ControlMode mLatestControlMode;

    /** Latest sent target. Accessed only from pomp thread. */
    private double mLatestTarget;

    /** Remaining command repetition count. Accessed only from pomp thread, except for resetting. */
    private int mRepetitions;

    /**
     * Constructor.
     *
     * @param encoder encodes zoom commands to be sent to the drone
     */
    ZoomControlEncoder(@NonNull CommandEncoder encoder) {
        mEncoder = encoder;
        mControlMode = mLatestControlMode = CameraZoom.ControlMode.LEVEL;
    }

    @Override
    @Nullable
    public ArsdkCommand encodeNoAckCmd() {
        CameraZoom.ControlMode controlMode;
        double target;
        synchronized (this) {
            controlMode = mControlMode;
            target = mTarget;
        }

        // if control has changed or target has changed
        if (mLatestControlMode != controlMode || Double.compare(mLatestTarget, target) != 0) {
            mLatestControlMode = controlMode;
            mLatestTarget = target;
            mRepetitions = ZOOM_COMMANDS_REPETITIONS;
        }

        // only decrement the counter if the control is in level,
        // or, if the control is in velocity and target is zero
        if (controlMode == CameraZoom.ControlMode.LEVEL || target == 0) {
            mRepetitions--;
        }

        return mRepetitions < 0 ? null : mEncoder.encodeZoomControl(controlMode, target);
    }

    /**
     * Controls the zoom.
     *
     * @param mode   control mode
     * @param target the target to use
     */
    void control(@NonNull CameraZoom.ControlMode mode, double target) {
        synchronized (this) {
            mControlMode = mode;
            mTarget = target;
        }
    }

    /**
     * Resets the encoder.
     * <p>
     * This method will simply avoid generating future encoders.
     */
    void reset() {
        // we allow to modify this var outside of the pomp loop without synchronizing it because we just don't
        // want to send data anymore.
        mRepetitions = 0;
    }
}
