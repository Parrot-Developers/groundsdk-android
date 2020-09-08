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

package com.parrot.drone.groundsdk.device.pilotingitf;

import android.location.Location;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.value.BooleanSetting;
import com.parrot.drone.groundsdk.value.EnumSetting;
import com.parrot.drone.groundsdk.value.IntSetting;
import com.parrot.drone.groundsdk.value.OptionalDoubleSetting;

/**
 * Piloting interface used to make the drone return to home.
 * <p>
 * This piloting interface can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPilotingItf(ReturnHomePilotingItf.class)}</pre>
 *
 * @see Drone#getPilotingItf(Class)
 * @see Drone#getPilotingItf(Class, Ref.Observer)
 */
public interface ReturnHomePilotingItf extends PilotingItf, Activable {

    /**
     * Return home destination targets.
     */
    enum Target {

        /** No target. This might be because the drone does not have a gps fix. */
        NONE,

        /** Return to take-off position. */
        TAKE_OFF_POSITION,

        /**
         * Return to latest tracked target position after FollowMe piloting interface has been activated.
         *
         * @see com.parrot.drone.groundsdk.device.peripheral.TargetTracker
         * @see com.parrot.drone.groundsdk.device.pilotingitf.FollowMePilotingItf
         */
        TRACKED_TARGET_POSITION,

        /** Return to custom location. */
        CUSTOM_LOCATION,

        /** Return to current controller position. */
        CONTROLLER_POSITION,
    }

    /**
     * Reason why return home piloting interface was activated or deactivated.
     */
    enum Reason {

        /** Return home is not active. */
        NONE,

        /** Return home was requested by the user. */
        USER_REQUESTED,

        /** Returning home because the connection to the drone was lost. */
        CONNECTION_LOST,

        /** Returning home because the drone battery level is low. */
        POWER_LOW,

        /** Return home is finished and is not active anymore. */
        FINISHED,
    }

    /**
     * Describes whether the return point can be reached by the drone or not.
     */
    enum Reachability {

        /** Home reachability is unknown. */
        UNKNOWN,

        /** Home is reachable. */
        REACHABLE,

        /**
         * The drone has planned an automatic safety return.
         * <p>
         * Return home will start after the delay returned by {@link #getAutoTriggerDelay()}.
         * The start date of the RTH is calculated so that the return trip can be made before the
         * battery is empty.
         */
        WARNING,

        /**
         * Home is still reachable but won't be if return home is not triggered now.
         * <p>
         * If return home is active, cancelling it will probably make the home not reachable.
         */
        CRITICAL,

        /** Home is not reachable. */
        NOT_REACHABLE
    }

    /**
     * Describes the drone behavior at the end of the RTH.
     */
    enum EndingBehavior {

        /** The drone terminates the RTH hovering. */
        HOVERING,

        /** The drone terminates the RTH landing. */
        LANDING
    }

    /**
     * Activates this piloting interface.
     * <p>
     * If successful, the currently active piloting interface (if any) is deactivated and this one is activated.
     *
     * @return {@code true} on success, {@code false} in case the piloting interface cannot be activated at this point
     */
    boolean activate();

    /**
     * Gets the reason why return home is active or not.
     *
     * @return the reason why return home is active or not. In particular, returns {@link Reason#NONE} or
     *         {@link Reason#FINISHED} if the interface is not currently active.
     */
    @NonNull
    Reason getReason();

    /**
     * Gets the current home location.
     * <p>
     * The location altitude is relative to the take off point.
     *
     * @return the current home location, or {@code null} if unknown presently.
     */
    @Nullable
    Location getHomeLocation();

    /**
     * Gets the current return home target.
     * <p>
     * Current target may be different from {@link #getPreferredTarget() prefered target} if the requirements of the
     * selected target are not met.
     *
     * @return the current target.
     *
     * @see #getPreferredTarget()
     */
    @NonNull
    Target getCurrentTarget();

    /**
     * Tells whether first GPS fix was obtained before or after take-off.
     * <p>
     * When {@code Target.TAKE_OFF_POSITION} is selected, in case the first fix was obtained after take-off, the drone
     * will return at this first fix position, which may be different from the take-off position.
     * <p>
     * Note that the returned value <strong>MUST ONLY</strong> be considered relevant when the current target is
     * {@code Target.TAKE_OFF_POSITION}. Returned value is undefined in other modes.
     *
     * @return {@code true} if GPS was fixed before taking off, {@code false} otherwise.
     *
     * @see #getCurrentTarget()
     */
    boolean gpsWasFixedOnTakeOff();

    /**
     * Gets the return home target user setting.
     * <p>
     * This setting allows the user to select whether the drone should return to its take-off position ot to the
     * current pilot position.
     * <p>
     * Note that this should be considered only as a preference. In any case, if the requirement are not met to honor
     * the selected preferred target, the drone may ignore that setting and chose a different target instead.
     *
     * @return the preferred target setting
     *
     * @see #getCurrentTarget()
     */
    @NonNull
    EnumSetting<Target> getPreferredTarget();

    /**
     * Gets the return home ending behavior setting.
     * <p>
     * This setting allows the user to select whether the drone should land or stay hovering
     * after returning home.
     *
     * @return the ending behavior
     */
    @NonNull
    EnumSetting<EndingBehavior> getEndingBehavior();

    /**
     * Gets the return home automatic activation upon disconnection delay setting (value in seconds).
     * <p>
     * This setting allows the user to setup the delay the drone will wait before activating return home, when the
     * connection with the controller is lost.
     *
     * @return the delay setting
     */
    @NonNull
    IntSetting getAutoStartOnDisconnectDelay();

    /**
     * Gets the return home ending hovering altitude setting, above ground level, in meters.
     * <p>
     * This setting is used only if {@link #getEndingBehavior() ending behavior} is set to
     * {@link EndingBehavior#HOVERING HOVERING}.
     *
     * @return the hovering altitude setting
     */
    @NonNull
    OptionalDoubleSetting getEndingHoveringAltitude();

    /**
     * Gets the return home minimum altitude setting, relative to the take off point, in meters.
     * <p>
     * If the drone is below this altitude when starting its return home, it will first reach the minimum altitude.
     * If the drone is higher than this minimum altitude, it will operate its return home at its current altitude.
     *
     * @return the minimum altitude setting.
     */
    @NonNull
    OptionalDoubleSetting getMinAltitude();

    /**
     * Gets an estimation of the possibility for the drone to reach its return point.
     *
     * @return return point reachability
     */
    @NonNull
    Reachability getHomeReachability();

    /**
     * Gets delay before an automatic return home planned by the drone.
     * <p>
     * The delay is calculated so that the return travel can be made before the battery is empty.
     * <p>
     * The returned delay is not valid when reachability returned by {@link #getHomeReachability()} is
     * {@link Reachability#WARNING}. In the other cases, the returned delay is zero.
     *
     * @return delay in seconds before return home if an automatic return home is planned, zero otherwise
     */
    @IntRange(from = 0)
    long getAutoTriggerDelay();

    /**
     * Cancels any current auto trigger.
     * <p>
     * If {@link #getHomeReachability() home reachability} is {@link Reachability#WARNING}, this
     * cancels the planned return home.
     */
    void cancelAutoTrigger();

    /**
     * Gets the return home auto trigger setting.
     * <p>
     * This setting allows the user to enable or disable the automatic trigger of a return to home.
     * <p>
     * Note that even if this setting is set to {@code false}, the user can ask for a manual return to home.
     *
     * @return the auto trigger switch setting
     */
    @NonNull
    BooleanSetting autoTrigger();

    /**
     * Sets the custom home location.
     * <p>
     * This location is used only if {@link #getCurrentTarget() current target} is on
     * {@link Target#CUSTOM_LOCATION CUSTOM_LOCATION}.
     *
     * @param latitude latitude of the custom location
     * @param longitude longitude of the custom location
     * @param altitude altitude of the custom location, relative to the take off point
     */
    void setCustomLocation(double latitude, double longitude, double altitude);
}
