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

package com.parrot.drone.groundsdk.device.instrument;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;

/**
 * Instrument that informs about alarms.
 * <p>
 * This instrument can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getInstrument(Alarms.class)}</pre>
 *
 * @see Drone#getInstrument(Class)
 * @see Drone#getInstrument(Class, Ref.Observer)
 */
public interface Alarms extends Instrument {

    /**
     * Alarm object which has a level.
     */
    abstract class Alarm {

        /** Kind of an alarm. */
        public enum Kind {

            /** The drone power is low. */
            POWER,

            /** Motors have been cut out. */
            MOTOR_CUT_OUT,

            /** Emergency due to user's demand. */
            USER_EMERGENCY,

            /** Motor error. */
            MOTOR_ERROR,

            /** Battery is too hot. */
            BATTERY_TOO_HOT,

            /** Battery is too cold. */
            BATTERY_TOO_COLD,

            /** Hovering is difficult due to a lack of GPS positioning and not enough light to use vertical camera. */
            HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK,

            /** Hovering is difficult due to a lack of GPS positioning and drone is too high to use vertical camera. */
            HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH,

            /**
             * Drone will soon forcefully and automatically land because some battery issue (e.g. low power, low or high
             * temperature...) does not allow to continue flying safely.
             * <p>
             * When at level <ul>
             * <li>{@link Level#OFF}, battery is OK and no automatic landing is scheduled;</li>
             * <li>{@link Level#WARNING}, some battery issues have been detected, which will soon prevent the drone
             * from flying safely. Automatic landing is scheduled;</li>
             * <li>{@link Level#CRITICAL}, battery issues are now so critical that the drone cannot continue flying
             * safely. Automatic landing is about to start in a matter of seconds.</li>
             * </ul>
             * Remaining delay before automatic landing begins (when scheduled both at WARNING and CRITICAL levels),
             * is accessible through method {@link #automaticLandingDelay()} and the instrument is updated each time
             * this value changes.
             */
            AUTOMATIC_LANDING_BATTERY_ISSUE,

            /**
             * Wind strength alters the drone ability to fly properly.
             * <p>
             * When at level <ul>
             * <li>{@link Level#OFF}, wind is not strong enough to have significant impact on drone flight;</li>
             * <li>{@link Level#WARNING}, wind is strong enough to alter the drone ability to fly properly;</li>
             * <li>{@link Level#CRITICAL}, wind is so strong that the drone is completely unable to fly
             * properly.</li>
             * </ul>
             */
            STRONG_WIND,

            /**
             * A vertical camera sensor defect alters the drone ability to fly safely.
             * <p>
             * When at level <ul>
             * <li>{@link Level#OFF}, no defect detected on the vertical camera;</li>
             * <li>{@link Level#CRITICAL}, defect detected on the vertical camera altering flight stabilization.
             * Flying is not recommended.</li>
             * </ul>
             */
            VERTICAL_CAMERA,

            /**
             * Vibrations alters the drone ability to fly properly.
             * <p>
             * When at level <ul>
             * <li>{@link Level#OFF}, detected vibration level is normal and has no impact on drone flight;</li>
             * <li>{@link Level#WARNING}, detected vibration level is strong enough to alter the drone ability to fly
             * properly, potentially because propellers are not tightly screwed;</li>
             * <li>{@link Level#CRITICAL}, detected vibration level is so strong that the drone is completely unable to
             * fly properly, indicating a serious drone malfunction.</li>
             * </ul>
             */
            STRONG_VIBRATIONS,

            /**
             * A magnetic element disturbs the drone's magnetometer and alters the drone ability to fly safely.
             *
             * @deprecated use {@link #HEADING_LOCK} instead
             */
            @Deprecated
            MAGNETOMETER_PERTURBATION,

            /**
             * The local terrestrial magnetic field is too weak to allow to fly safely.
             *
             * @deprecated use {@link #HEADING_LOCK} instead
             */
            @Deprecated
            MAGNETOMETER_LOW_EARTH_FIELD,

            /**
             * Drone heading lock is altered by magnetic perturbations.
             * <p>
             * When at level <ul>
             * <li>{@link Level#OFF}, magnetometer state allows heading lock;</li>
             * <li>{@link Level#WARNING}, magnetometer detects a weak magnetic field (close to Earth pole), or a
             * disturbed local magnetic field; magnetometer has not lost heading lock yet;</li>
             * <li>{@link Level#CRITICAL}, magnetometer lost heading lock.</li>
             * </ul>
             */
            HEADING_LOCK,

            /** Location information sent by the controller is unreliable. */
            UNRELIABLE_CONTROLLER_LOCATION
        }

        /** Level of an alarm. */
        public enum Level {

            /** Alarm not supported on the drone. */
            NOT_SUPPORTED,

            /** Alarm is off. */
            OFF,

            /** Alarm is at warning level. */
            WARNING,

            /** Alarm is at critical level. */
            CRITICAL,
        }

        /**
         * Gets the kind of the alarm.
         *
         * @return the kind of the alarm
         */
        @NonNull
        public abstract Kind getKind();

        /**
         * Gets the level of the alarm.
         *
         * @return the level of the alarm
         */
        @NonNull
        public abstract Level getLevel();
    }

    /**
     * Gets the alarm of a given kind.
     *
     * @param kind the kind of the alarm
     *
     * @return an alarm of the requested kind.
     */
    @NonNull
    Alarm getAlarm(@NonNull Alarm.Kind kind);

    /**
     * Delay before automatic landing, in seconds.
     * <p>
     * The actual reason why automatic landing is scheduled depends on which of the following alarms is currently
     * 'on' ({@link Alarm.Level#WARNING} or {@link Alarm.Level#CRITICAL}): <ul>
     * <li>{@link Alarm.Kind#AUTOMATIC_LANDING_BATTERY_ISSUE}</li>
     * </ul>
     * When one of those alarms is in such a state, then this method tells when automatic landing procedure
     * is about to start. <br>
     * Otherwise (when all those alarms are {@link Alarm.Level#OFF off}), no automatic landing procedure is
     * currently scheduled and this method consequently returns {@code 0}.
     *
     * @return delay before automatic landing, in seconds; {@code 0} if no automatic landing is scheduled currently
     */
    @IntRange(from = 0)
    int automaticLandingDelay();
}
