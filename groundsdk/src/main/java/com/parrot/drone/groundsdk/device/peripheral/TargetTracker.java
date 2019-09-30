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

package com.parrot.drone.groundsdk.device.peripheral;

import android.os.SystemClock;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.pilotingitf.FollowMePilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.LookAtPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.value.Setting;

/**
 * Target peripheral interface for Drones.
 * <p>
 * This peripheral allows to: <ul>
 * <li>control whether user device/controller barometer and location are actively monitored and
 * sent to the connected drone, in order to allow the latter to track the user and/or controller,</li>
 * <li>forward external target detection information to the drone, in order to allow the latter to track a given
 * target,</li>
 * <li>configure the tracked target desired position (framing) in the video stream.</li>
 * </ul>
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(TargetTracker.class)}</pre>
 *
 * @see Peripheral.Provider#getPeripheral(Class)
 * @see Peripheral.Provider#getPeripheral(Class, Ref.Observer)
 */
public interface TargetTracker extends Peripheral {

    /**
     * Target framing setting.
     * <p>
     * Allows to configure positioning of the tracked target in the drone video stream.
     */
    abstract class FramingSetting extends Setting {

        /**
         * Retrieves current target horizontal framing position.
         * <p>
         * The return value is a double in range [0, 1], which indicates where the tracked target should be positioned
         * in the drone stream, relatively to the frame width. For instance, 0 indicates that the target will be
         * positioned at the left-most position in the frame, 1 indicates that it will be positioned at the right-most
         * position in the frame.
         *
         * @return target horizontal framing position
         */
        @FloatRange(from = 0, to = 1)
        public abstract double getHorizontalPosition();

        /**
         * Retrieves current target vertical framing position.
         * <p>
         * The return value is a double in range [0, 1], which indicates where the tracked target should be positioned
         * in the drone video stream, relatively to the frame height. For instance, 0 indicates that the target will be
         * positioned at the top-most position in the frame, 1 indicates that it will be positioned at the bottom-most
         * position in the frame.
         *
         * @return target vertical framing position
         */
        @FloatRange(from = 0, to = 1)
        public abstract double getVerticalPosition();

        /**
         * Sets target framing position.
         * <p>
         * Parameter values are double in range [0, 1] which specify where the tracked target should be positioned
         * in the drone video stream, relatively to the frame width and height.
         *
         * @param horizontalPosition target horizontal framing position to set
         * @param verticalPosition   target vertical framing position to set
         */
        public abstract void setPosition(@FloatRange(from = 0, to = 1) double horizontalPosition,
                                         @FloatRange(from = 0, to = 1) double verticalPosition);
    }

    /**
     * Gives access to the target framing setting.
     * <p>
     * This setting allows to control the position at which the tracked target should appear in the drone video stream.
     *
     * @return framing setting
     */
    @NonNull
    FramingSetting framing();

    /**
     * Enables tracking of the controller (user device or remote-control) as the current target.
     * <p>
     * Calling this method enables forwarding of controller barometer and location information to the connected drone,
     * so that it may track the user (or controller).
     * <p>
     * This method should be called prior to activating {@link PilotingItf piloting interface} that track the user
     * movements, such as {@link LookAtPilotingItf} and {@link FollowMePilotingItf}
     * <p>
     * Tracking should be disabled once such piloting interface are not used anymore, to stop forwarding location
     * and barometer information to the drone, as monitoring such information is actively consuming battery and network.
     *
     * @see #disableControllerTracking()
     */
    void enableControllerTracking();

    /**
     * Disables tracking of the controller (user device or remote-control) as the current target.
     * <p>
     * Calling this method disables forwarding of controller barometer and location information to the connected drone.
     * <p>
     * This method should be called once controller barometer and location info are not required to pilot the drone
     * (for example using {@link LookAtPilotingItf}), as monitoring the device barometer and location is actively
     * consuming battery and network.
     *
     * @see #enableControllerTracking()
     */
    void disableControllerTracking();

    /**
     * Tells whether controller tracking is currently enabled.
     *
     * @return {@code true} if controller tracking is enabled, otherwise {@code false}
     */
    boolean isControllerTrackingEnabled();

    /**
     * Represents target detection information.
     */
    interface TargetDetectionInfo {

        /**
         * Gives the current target azimuth, which is the horizontal north-drone-target angle in radians.
         *
         * @return target azimuth
         */
        double getTargetAzimuth();

        /**
         * Gives the current target elevation, which is the vertical horizon-drone-target angle in radians.
         *
         * @return target elevation
         */
        double getTargetElevation();

        /**
         * Gives the current target rate of change in scale, which is the normalized radial speed of the target
         * relatively to the drone, in hertz.
         *
         * @return target change of scale
         */
        double getChangeOfScale();

        /**
         * Tells whether this piece of target information concerns a new target that was sent before.
         *
         * @return {@code true} if this gives the information of a new target, otherwise {@code false}
         */
        boolean isNewTarget();

        /**
         * Gives the level of confidence attached to the provided piece of information.
         * <p>
         * Return value is a double in range [0, 1], where 0 means that the provided information is most probably
         * inaccurate (or cannot be considered as such) and 1 means that the provided information is most probably
         * accurate.
         *
         * @return confidence level
         */
        @FloatRange(from = 0, to = 1)
        double getConfidenceLevel();

        /**
         * Gives the timestamp of the provided target information, in milliseconds since epoch.
         * <p>
         * This value should come from a monotonic time provider, such as {@link SystemClock#elapsedRealtime()}
         *
         * @return information timestamp
         */
        @IntRange(from = 0)
        long getTimestamp();
    }

    /**
     * Forwards external target detection information to the drone.
     * <p>
     * Such information allows the drone to locate and follow a specific target. This may for example come from
     * image processing results from the drone video stream.
     *
     * @param info target detection information to send
     */
    void sendTargetDetectionInfo(@NonNull TargetDetectionInfo info);

    /**
     * Informs about tracked target trajectory.
     */
    abstract class TargetTrajectory {

        /**
         * Retrieves target latitude.
         *
         * @return target latitude, in degrees
         */
        public abstract double getLatitude();

        /**
         * Retrieves target longitude.
         *
         * @return target longitude, in degrees
         */
        public abstract double getLongitude();

        /**
         * Retrieves target altitude.
         *
         * @return target altitude, in meters, relative to sea level
         */
        public abstract double getAltitude();

        /**
         * Retrieves target speed towards North .
         *
         * @return target north speed, in meters per second
         */
        public abstract double getNorthSpeed();

        /**
         * Retrieves target speed towards East .
         *
         * @return target north speed, in meters per second
         */
        public abstract double getEastSpeed();

        /**
         * Retrieves target speed towards ground .
         *
         * @return target down speed, in meters per second
         */
        public abstract double getDownSpeed();
    }

    /**
     * Informs about tracked target trajectory.
     *
     * @return tracked target trajectory, if available, otherwise {@code null}
     */
    @Nullable
    TargetTrajectory getTargetTrajectory();
}