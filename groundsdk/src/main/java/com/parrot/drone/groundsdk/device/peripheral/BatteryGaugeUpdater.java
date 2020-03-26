/*
 *     Copyright (C) 2020 Parrot Drones SAS
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

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;

import java.util.Collection;
import java.util.EnumSet;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * Updater peripheral interface for battery gauge.
 * <p>
 * It indicates if the battery gauge firmware should be updated and allows to apply the update.
 * <p>
 * This peripheral is only available if the drone is connected and the battery gauge firmware should be updated.
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(BatteryGaugeUpdater.class)}</pre>
 *
 * @see Drone#getPeripheral(Class)
 * @see Drone#getPeripheral(Class, Ref.Observer)
 */
public interface BatteryGaugeUpdater extends Peripheral {

    /**
     * Update state.
     */
    enum State {

        /** Drone is ready to prepare update. */
        READY_TO_PREPARE,

        /** Drone is preparing the update. */
        PREPARING_UPDATE,

        /** Drone is ready to apply the update. */
        READY_TO_UPDATE,

        /** Update is in progress. */
        UPDATING,

        /**
         * An error occurred during the preparation or the update.
         * <p>
         * This state is temporary, it will quickly change to {@link #READY_TO_PREPARE} afterwards.
         */
        ERROR
    }

    /**
     * Gives current update state.
     *
     * @return current update state
     */
    @NonNull
    State state();

    /**
     * Reason that makes preparing or applying firmware update impossible.
     */
    enum UnavailabilityReason {

        /** Update is impossible if drone is not landed. */
        DRONE_NOT_LANDED,

        /** Update is impossible if USB power is not provided. */
        NOT_USB_POWERED,

        /** Update is impossible if battery charge is too low. */
        INSUFFICIENT_CHARGE
    }

    /**
     * Tells why it is currently impossible to prepare or apply firmware update.
     * <p>
     * If the returned set is not {@link Collection#isEmpty() empty}, then {@link #prepareUpdate()} and
     * {@link #update()} methods won't do anything but return {@code false}.
     * <p>
     * In case updating becomes unavailable for some reason while an update operation is ongoing
     * ({@link State#PREPARING_UPDATE preparing update} or {@link State#UPDATING updating}), then the update will be
     * forcefully canceled.
     *
     * @return current update unavailability reasons
     */
    @NonNull
    EnumSet<UnavailabilityReason> unavailabilityReasons();

    /**
     * Gives current prepare progress, in percent.
     * <p>
     * Note: update progress can not be accessed as drone is disconnected during update.
     *
     * @return current prepare progress
     */
    @IntRange(from = 0, to = 100)
    int currentProgress();

    /**
     * Requests preparing battery gauge firmware update.
     * <p>
     * This method does nothing but returns {@code false} if either:<ul>
     * <li>the current {@link #state() update state} is not {@link State#READY_TO_PREPARE ready to prepare}, or</li>
     * <li>some {@link #unavailabilityReasons() reason} exists that makes it impossible to prepare update currently.
     * </li>
     * </ul>
     * The {@link #state() update state} will change to {@link State#PREPARING_UPDATE PREPARING_UPDATE} as soon as the
     * prepare update process is started.
     *
     * @return {@code true} if the prepare update operation could be initiated, otherwise {@code false}
     */
    boolean prepareUpdate();

    /**
     * Requests battery gauge firmware update.
     * <p>
     * This method does nothing but returns {@code false} if either:<ul>
     * <li>the current {@link #state() update state} is not {@link State#READY_TO_UPDATE ready to update}, or</li>
     * <li>some {@link #unavailabilityReasons() reason} exists that makes it impossible to update currently.
     * </li>
     * </ul>
     * The {@link #state() update state} will change to {@link State#UPDATING UPDATING} as soon as the update process
     * is started, and then the drone will immediately reboot, making this peripheral unavailable.
     * <p>
     * <strong>Note:</strong> the update should first be {@link #prepareUpdate() prepared} before it could be applied.
     *
     * @return {@code true} if the update operation could be initiated, otherwise {@code false}
     */
    boolean update();
}
