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
import com.parrot.drone.groundsdk.device.peripheral.TargetTracker;
import com.parrot.drone.groundsdk.device.pilotingitf.tracking.TrackingIssue;
import com.parrot.drone.groundsdk.value.EnumSetting;

import java.util.EnumSet;

/**
 * Piloting interface used to make the drone follow some (possibly moving) target.
 *
 * <h2>Availability</h2>
 * <p>
 * This interface will be {@link State#UNAVAILABLE unavailable} until a specific set of conditions are met:
 * <ul>
 * <li>the drone must be flying,</li>
 * <li>the drone must be properly calibrated,</li>
 * <li>the drone must have sufficient GPS location information,</li>
 * <li>the drone must be sufficiently far above the ground,</li>
 * <li>the drone must be sufficiently far away from the target,</li>
 * <li>the application must provide sufficient information to the drone so that it can identify and track the
 * target (see <a href="#target_selection">Target selection</a> for further information on how to provide target
 * tracking information.)</li>
 * </ul>
 * All {@link #getAvailabilityIssues() unsatisfied conditions} are reported by this interface, when unavailable, so
 * that the application can inform the user and take appropriate measures to satisfy those conditions as appropriate.
 * Once all conditions are satisfied, then the interface will become {@link State#IDLE available} and can
 * be {@link #activate() activated} so that the drone starts following the target.
 * <p>
 * Note that this interface, even while available or {@link State#ACTIVE active} may become unavailable back
 * again, as soon as any of those conditions is not satisfied anymore.
 *
 * <h2>Mode</h2>
 * This interface supports different {@link Mode modes} of operation. This may be configured through the
 * {@link #mode() mode} setting, regardless of interface availability. Mode changes will be applied immediately in case
 * the interface is currently active, without the need to activate it again. <br>
 * Note also that this setting is not persisted.
 *
 * <h2>Alerts and behavior</h2>
 * <p>
 * When available or active, this interface may also alert about specific {@link #getQualityIssues() conditions}
 * that hinders optimally accurate tracking of the target, although tracking remains feasible under such conditions:
 * <ul>
 * <li>TODO : refine and list exactly which cases can be forwarded as an 'alert' from the drone.</li>
 * </ul>
 * The application may use such alerts to inform the user and take appropriate measures to improve tracking performance.
 * <p>
 * This interface will also report when such conditions actually force the drone to adopt a downgraded
 * {@link #getCurrentBehavior() behavior}. For example, some conditions may force the drone to remain only
 * {@link Behavior#STATIONARY stationary} instead of following the target when it moves.
 *
 * <h2><a id="movement">Movement</a></h2>
 * <p>
 * When this interface is active, the drone will fly autonomously to follow the moving target as the current mode
 * permits it, unless manually instructed to move using this interface's {@link #setPitch}, {@link #setRoll} or
 * {@link #setVerticalSpeed} piloting commands. <br>
 * In any case, the drone will always try to follow the target and may thus move independently of the provided piloting
 * commands to do so.
 *
 * <h2><a id="target_selection">Target selection</a></h2>
 * <p>
 * In order for this interface to be available, the application must instruct the drone how to identify and track the
 * desired target. To do so, the {@link TargetTracker} peripheral must be used.
 * <p>
 * Using this peripheral, the application may:
 * <ul>
 * <li>Require controller (remote control, or user device) barometer and location information to be sent regularly
 * to the drone. In case sent information is sufficiently accurate, it may be sufficient to allow the drone to
 * track the controller, in which case, provided other requirements are satisfied, the interface will become
 * available and, when active, will make the drone follow the controller.</li>
 * <li>Provide external information about where the desired target is located, its direction and current movement.
 * Such information may for example come from results of image processing applied to the drone video stream. In case
 * sent information is sufficiently accurate and coherent, it may be sufficient to allow the drone to track the
 * desired target, in which case, provided other requirements are satisfied, the interface will become available
 * and, when active, will make the drone follow the desired target.</li>
 * </ul>
 * <p>
 * Note that both controller barometer/location information and external target information may be sent to the drone at
 * the same time, provided they give coherent positioning information on the <strong>SAME</strong> target. <br>
 * For instance, it is possible to perform image processing on the drone video stream to identify the controller and
 * send the results to the drone, along with controller barometer/location updates, for better tracking performance.
 * <br>
 * However, care must be taken not to provide incoherent information, such as forwarding controller/barometer location
 * and, as the same time, external information on a different target than the controller; behavior is undefined in such
 * a case.
 *
 * <p>
 * This piloting interface can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPilotingItf(FollowMePilotingItf.class)}</pre>
 *
 * @see Drone#getPilotingItf(Class)
 * @see Drone#getPilotingItf(Class, Ref.Observer)
 */
public interface FollowMePilotingItf extends PilotingItf, Activable {

    /** Follow-me operating mode. */
    enum Mode {

        /**
         * Mode instructing the drone to only track the position of the target, regardless of its orientation or any
         * change in its direction. <br>
         * The drone will move so that the north-drone-target angle and the distance to the target both remain constant.
         */
        GEOGRAPHIC,

        /**
         * Mode instructing the drone to both track the target position and changes in the direction of the target,
         * relatively to the initial direction of the target (that is, the direction the target had when the interface
         * was activated or after manually moving the drone, using any of {@link #setPitch}, {@link #setRoll} or
         * {@link #setVerticalSpeed} methods). <br>
         * The drone will move so that the angle formed by the target direction vector, and the drone-target vector
         * remains constant.
         */
        RELATIVE,

        /**
         * Mode instructing the drone to both track the target position and changes in the direction of the target,
         * as if it was held by the target by a leash.
         */
        LEASH
    }

    /**
     * Gives access to the operating mode setting.
     * <p>
     * This setting allows to select the operating {@link Mode mode} of this interface.
     *
     * @return operating mode setting
     */
    @NonNull
    EnumSetting<Mode> mode();

    /**
     * Activates this piloting interface.
     * <p>
     * If successful, the currently active piloting interface (if any) is deactivated and this one is activated.
     * </p>
     *
     * @return {@code true} on success, {@code false} in case the piloting interface cannot be activated at this point
     */
    boolean activate();

    /** Follow-me actual behavior. */
    enum Behavior {

        /** Follow-me piloting interface is not currently active. */
        INACTIVE,

        /**
         * Some {@link #getQualityIssues() tracking issues} make the drone unable to follow the target; it is
         * currently only able to rotate to keep itself oriented towards the target.
         */
        STATIONARY,

        /**
         * The drone is currently following the target.
         */
        FOLLOWING
    }

    /**
     * Informs about the current behavior of the follow-me piloting interface.
     *
     * @return current behavior, always {@link Behavior#INACTIVE} unless this interface is
     *         {@link Activable.State#ACTIVE active}
     */
    @NonNull
    Behavior getCurrentBehavior();

    /**
     * Tells why this piloting interface is currently be unavailable.
     * <p>
     * The returned set may contain values only if the interface is {@link State#UNAVAILABLE unavailable}.
     *
     * @return the set of reasons that preclude this piloting interface from being available at present
     */
    @NonNull
    EnumSet<TrackingIssue> getAvailabilityIssues();

    /**
     * Alerts about issues that currently hinders optimal behavior of this interface.
     * <p>
     * The returned set may contain values only if the interface is not {@link State#UNAVAILABLE}.
     *
     * @return a set of issues that hinders optimal behavior of this interface
     */
    @NonNull
    EnumSet<TrackingIssue> getQualityIssues();

    /**
     * Sets the current pitch value.
     * <p>
     * The drone tries to both conform to the requested pitch command and keep directed towards the target.
     * <p>
     * {@code value} is expressed as a signed percentage of the {@link ManualCopterPilotingItf#getMaxPitchRoll() max
     * pitch/roll setting}, in range [-100, 100]. <br>
     * -100 corresponds to a pitch angle of max pitch/roll towards ground (drone will fly forward), 100 corresponds to
     * a pitch angle of max pitch/roll towards sky (copter will fly backward).
     * <p>
     * Note: {@code value} may be clamped if necessary, in order to respect the maximum supported physical tilt of the
     * copter.
     *
     * @param pitch the new pitch value to set
     */
    void setPitch(@IntRange(from = -100, to = 100) int pitch);

    /**
     * Sets the current roll value.
     * <p>
     * The drone tries to both conform to the requested roll command and keep directed towards the target.
     * <p>
     * {@code value} is expressed as a signed percentage of the {@link ManualCopterPilotingItf#getMaxPitchRoll() max
     * pitch/roll setting}, in range [-100, 100]. <br>
     * -100 corresponds to a roll angle of max pitch/roll to the left (copter will fly left), 100 corresponds to a roll
     * angle of max pitch/roll to the right (copter will fly right).
     * <p>
     * Note: {@code value} may be clamped if necessary, in order to respect the maximum supported physical tilt of the
     * copter.
     *
     * @param roll the new pitch roll to set
     *
     * @see ManualCopterPilotingItf#getMaxPitchRoll()
     */
    void setRoll(@IntRange(from = -100, to = 100) int roll);

    /**
     * Set the current vertical speed value.
     * <p>
     * The drone tries to both conform to the requested vertical speed command and keep directed towards the target.
     * <p>
     * {@code value} is expressed as as a signed percentage of the
     * {@link ManualCopterPilotingItf#getMaxVerticalSpeed() max vertical speed setting}, in range [-100, 100]. <br>
     * -100 corresponds to max vertical speed towards ground,100 corresponds to max vertical speed towards sky.
     *
     * @param verticalSpeed the new vertical speed value to set
     *
     * @see ManualCopterPilotingItf#getMaxVerticalSpeed()
     */
    void setVerticalSpeed(@IntRange(from = -100, to = 100) int verticalSpeed);
}
