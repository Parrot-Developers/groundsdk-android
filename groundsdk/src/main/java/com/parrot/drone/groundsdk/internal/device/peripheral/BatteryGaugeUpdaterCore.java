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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import com.parrot.drone.groundsdk.device.peripheral.BatteryGaugeUpdater;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

import java.util.Collection;
import java.util.EnumSet;

import androidx.annotation.NonNull;

/** Core class for BatteryGaugeUpdater. */
public class BatteryGaugeUpdaterCore extends SingletonComponentCore implements BatteryGaugeUpdater {

    /** Description of BatteryGaugeUpdater. */
    private static final ComponentDescriptor<Peripheral, BatteryGaugeUpdater> DESC =
            ComponentDescriptor.of(BatteryGaugeUpdater.class);

    /** Engine-specific backend for BatteryGaugeUpdater. */
    public interface Backend {

        /**
         * Requests preparing battery gauge firmware update.
         */
        void prepareUpdate();

        /**
         * Requests battery gauge firmware update.
         */
        void update();
    }

    /** Engine peripheral backend. */
    private final Backend mBackend;

    /** Current update state. */
    @NonNull
    private State mState;

    /** Set of reasons why applying firmware updates is currently impossible. */
    @NonNull
    private final EnumSet<UnavailabilityReason> mUnavailabilityReasons;

    /** Current prepare progress. */
    private int mProgress;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public BatteryGaugeUpdaterCore(@NonNull ComponentStore<Peripheral> peripheralStore,
                                   @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
        mState = State.READY_TO_PREPARE;
        mUnavailabilityReasons = EnumSet.noneOf(UnavailabilityReason.class);
    }

    @NonNull
    @Override
    public State state() {
        return mState;
    }

    @NonNull
    @Override
    public EnumSet<UnavailabilityReason> unavailabilityReasons() {
        return mUnavailabilityReasons;
    }

    @Override
    public int currentProgress() {
        return mProgress;
    }

    @Override
    public boolean prepareUpdate() {
        if (mState == State.READY_TO_PREPARE && mUnavailabilityReasons.isEmpty()) {
            mBackend.prepareUpdate();
            return true;
        }
        return false;
    }

    @Override
    public boolean update() {
        if (mState == State.READY_TO_UPDATE && mUnavailabilityReasons.isEmpty()) {
            mBackend.update();
            return true;
        }
        return false;
    }

    /**
     * Updates the update state.
     *
     * @param state new update state
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public BatteryGaugeUpdaterCore updateState(@NonNull State state) {
        if (mState != state) {
            mState = state;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the set of unavailability reasons for battery gauge firmware update.
     *
     * @param reasons new unavailability reasons
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public BatteryGaugeUpdaterCore updateUnavailabilityReasons(
            @NonNull Collection<UnavailabilityReason> reasons) {
        mChanged |= mUnavailabilityReasons.retainAll(reasons) | mUnavailabilityReasons.addAll(reasons);
        return this;
    }

    /**
     * Updates the prepare progress.
     *
     * @param progress new progress
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public BatteryGaugeUpdaterCore updateProgress(int progress) {
        if (mProgress != progress) {
            mProgress = progress;
            mChanged = true;
        }
        return this;
    }
}
