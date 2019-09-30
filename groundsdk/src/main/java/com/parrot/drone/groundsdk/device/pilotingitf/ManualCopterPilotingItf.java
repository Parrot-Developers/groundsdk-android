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

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.value.DoubleSetting;
import com.parrot.drone.groundsdk.value.OptionalBooleanSetting;
import com.parrot.drone.groundsdk.value.OptionalDoubleSetting;

/**
 * Manual piloting interface for copters.
 * <p>
 * This piloting interface can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPilotingItf(ManualCopterPilotingItf.class)}</pre>
 *
 * @see Drone#getPilotingItf(Class)
 * @see Drone#getPilotingItf(Class, Ref.Observer)
 */
public interface ManualCopterPilotingItf extends PilotingItf, Activable {

    /**
     * Action performed when {@link #smartTakeOffLand()} is called.
     *
     * @see #getSmartTakeOffLandAction()
     */
    enum SmartTakeOffLandAction {

        /** No action. */
        NONE,

        /** Requests the copter to take off. */
        TAKE_OFF,

        /**
         * Requests the copter to get prepared for a thrown take-off.
         * <p>
         * {@link #smartTakeOffLand()} will initialize a thrown take-off, if the following conditions are met: <ul>
         * <li>the thrown take-off feature is both supported and enabled,</li>
         * <li>the drone is in a state allowing a take-off (i.e. {@link #canTakeOff()} returns {@code true}),</li>
         * <li>the drone detects motion (i.e. the drone is held by the user).</li>
         * </ul>
         * Use {@link #getThrownTakeOffMode()} to check availability and to activate thrown take-off.
         */
        THROWN_TAKE_OFF,

        /** Requests the copter to land or to cancel a thrown take-off. */
        LAND
    }

    /**
     * Activates this piloting interface.
     * <p>
     * If successful, the currently active piloting interface (if any) is deactivated and this one is activated.
     * </p>
     *
     * @return {@code true} on success, {@code false} in case the piloting interface cannot be activated at this point
     */
    boolean activate();

    /**
     * Sets the current pitch value.
     * <p>
     * {@code value} is expressed as a signed percentage of the {@link #getMaxPitchRoll() max pitch/roll setting},
     * in range [-100, 100]. <br>
     * -100 corresponds to a pitch angle of max pitch/roll towards ground (copter will fly forward), 100 corresponds to
     * a pitch angle of max pitch/roll towards sky (copter will fly backward).
     * <p>
     * Note: {@code value} may be clamped if necessary, in order to respect the maximum supported physical tilt of the
     * copter.
     *
     * @param value the new pitch value to set
     *
     * @see #getMaxPitchRoll()
     */
    void setPitch(@IntRange(from = -100, to = 100) int value);

    /**
     * Sets the current roll value.
     * <p>
     * {@code value} is expressed as a signed percentage of the {@link #getMaxPitchRoll() max pitch/roll setting},
     * in range [-100, 100]. <br>
     * -100 corresponds to a roll angle of max pitch/roll to the left (copter will fly left), 100 corresponds to a roll
     * angle of max pitch/roll to the right (copter will fly right).
     * <p>
     * Note: {@code value} may be clamped if necessary, in order to respect the maximum supported physical tilt of the
     * copter.
     *
     * @param value the new pitch roll to set
     *
     * @see #getMaxPitchRoll()
     */
    void setRoll(@IntRange(from = -100, to = 100) int value);

    /**
     * Set the current yaw rotation speed value.
     * <p>
     * {@code value } is expressed as as a signed percentage of the
     * {@link #getMaxYawRotationSpeed() max yaw rotation speed setting}, in range [-100, 100]. <br>
     * -100 corresponds to a counter-clockwise rotation of max yaw rotation speed, 100 corresponds to a clockwise
     * rotation of max yaw rotation speed.
     *
     * @param value the new yaw rotation speed value to set
     *
     * @see #getMaxYawRotationSpeed()
     */
    void setYawRotationSpeed(@IntRange(from = -100, to = 100) int value);

    /**
     * Set the current vertical speed value.
     * <p>
     * {@code value} is expressed as as a signed percentage of the
     * {@link #getMaxVerticalSpeed() max vertical speed setting}, in range [-100, 100]. <br>
     * -100 corresponds to max vertical speed towards ground,100 corresponds to max vertical speed towards sky.
     *
     * @param value the new vertical speed value to set
     *
     * @see #getMaxVerticalSpeed()
     */
    void setVerticalSpeed(@IntRange(from = -100, to = 100) int value);

    /**
     * Requests the copter to hover.
     * <p>
     * Puts pitch and roll to 0.
     */
    void hover();

    /**
     * Tells whether the copter can take off.
     *
     * @return {@code true} if the copter can take off, otherwise {@code false}
     */
    boolean canTakeOff();

    /**
     * Tells whether the copter can land.
     *
     * @return {@code true} if the copter can land, otherwise {@code false}
     */
    boolean canLand();

    /**
     * Requests the copter to take off.
     */
    void takeOff();

    /**
     * Requests the copter to get prepared for a thrown take-off.
     */
    void thrownTakeOff();

    /**
     * Requests the copter to either take off, or get prepared for a thrown take-off, or cancel a thrown take-off,
     * or land, depending on its state and on the thrown take-off setting.
     *
     * @see #getSmartTakeOffLandAction()
     * @see #getThrownTakeOffMode()
     */
    void smartTakeOffLand();

    /**
     * Tells which action will be performed when {@link #smartTakeOffLand()} is called.
     *
     * @return action performed when {@link #smartTakeOffLand()} is called
     */
    @NonNull
    SmartTakeOffLandAction getSmartTakeOffLandAction();

    /**
     * Requests the copter to land.
     */
    void land();

    /**
     * Requests emergency motor cut out.
     */
    void emergencyCutOut();

    /**
     * Gets the maximum roll and pitch angle setting, in degrees.
     * <p>
     * Defines the range used by {@link #setPitch} and {@link #setRoll} methods. For instance, setting pitch to 100
     * corresponds to an angle of {@code getMaxPitchRoll().getValue()} degrees.
     *
     * @return the maximum roll/pitch setting
     */
    @NonNull
    DoubleSetting getMaxPitchRoll();

    /**
     * Gets the maximum roll and pitch velocity setting, in degrees/second.
     * <p>
     * Defines the copter dynamic by changing the speed by which the copter will move to the requested roll/pitch
     * angle.
     *
     * @return the maximum roll/pitch velocity setting
     */
    @NonNull
    OptionalDoubleSetting getMaxPitchRollVelocity();

    /**
     * Gets the maximum vertical speed setting, in meters/second.
     * <p>
     * Defines the range used by {@link #setVerticalSpeed} method. For instance, setting vertical speed to 100
     * corresponds to a vertical speed of {@code getMaxVerticalSpeed().getValue()} meters/second.
     *
     * @return the maximum vertical speed setting
     */
    @NonNull
    DoubleSetting getMaxVerticalSpeed();

    /**
     * Gets the maximum yaw rotation speed setting in degrees/second.
     * <p>
     * Defines the range used by {@link #setYawRotationSpeed}. For instance, setting yaw rotation speed to 100
     * corresponds to a yaw rotation speed of {@code getMaxYawRotationSpeed().getValue()} degrees/second.
     *
     * @return the maximum yaw rotation speed setting
     */
    @NonNull
    DoubleSetting getMaxYawRotationSpeed();

    /**
     * Gets the current banked-turn mode.
     * <p>
     * If available, banked-turn mode can either be enabled or disabled. <br>
     * When enabled, the drone will use yaw values from the piloting command to infer with roll and pitch  when the
     * horizontal speed is not null.
     *
     * @return the banked-turn mode setting
     */
    @NonNull
    OptionalBooleanSetting getBankedTurnMode();

    /**
     * Gets the current thrown take-off mode.
     * <p>
     * If available, thrown take-off mode can either be enabled or disabled. <br>
     * When enabled, a call to {@link #smartTakeOffLand()} may send a request to copter
     * to get prepared for a thrown take-off, depending on copter's state.
     *
     * @return the thrown take-off mode setting
     */
    @NonNull
    OptionalBooleanSetting getThrownTakeOffMode();
}
