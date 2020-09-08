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

package com.parrot.drone.groundsdk.device.pilotingitf.tracking;

import com.parrot.drone.groundsdk.device.pilotingitf.FollowMePilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.LookAtPilotingItf;

/**
 * Issues that may either render a tracking piloting interface (such as {@link LookAtPilotingItf} or
 * {@link FollowMePilotingItf}) unavailable or hinder its optimal behavior.
 */
public enum TrackingIssue {

    /** Drone is not currently flying. */
    DRONE_NOT_FLYING,

    /** Drone is not properly calibrated. */
    DRONE_NOT_CALIBRATED,

    /** Drone GPS information is not accurate enough. */
    DRONE_GPS_INFO_INACCURATE,

    /** Drone is currently too close to the tracked target. */
    DRONE_TOO_CLOSE_TO_TARGET,

    /** Drone is too far from target. */
    DRONE_TOO_FAR_FROM_TARGET,

    /** Drone is currently too close to the ground. */
    DRONE_TOO_CLOSE_TO_GROUND,

    /** Forwarded target GPS location information is not accurate enough. */
    TARGET_GPS_INFO_INACCURATE,

    /** Forwarded target barometer information is not accurate enough. */
    TARGET_BAROMETER_INFO_INACCURATE,

    /** External target detection information is not being forwarded. */
    TARGET_DETECTION_INFO_MISSING,

    /** Target horizontal speed is too high. */
    TARGET_HORIZONTAL_SPEED_TOO_HIGH,

    /** Target vertical speed is too high. */
    TARGET_VERTICAL_SPEED_TOO_HIGH
}
