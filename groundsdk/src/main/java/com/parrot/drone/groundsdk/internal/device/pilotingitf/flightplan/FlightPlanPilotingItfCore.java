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

package com.parrot.drone.groundsdk.internal.device.pilotingitf.flightplan;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.ActivablePilotingItfCore;

import java.io.File;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Core class for FlightPlanPilotingItf.
 */
public class FlightPlanPilotingItfCore extends ActivablePilotingItfCore implements FlightPlanPilotingItf {

    /** Description of FlightPlanPilotingItf. */
    private static final ComponentDescriptor<PilotingItf, FlightPlanPilotingItf> DESC =
            ComponentDescriptor.of(FlightPlanPilotingItf.class);

    /** Engine-specific backend for FlightPlanPilotingItf. */
    public interface Backend extends ActivablePilotingItfCore.Backend {

        /**
         * Uploads a flight plan to the drone.
         * <p>
         * File is expected to be a mavlink file, but no check is done whatsoever.
         *
         * @param flightPlan flightplan file to upload
         */
        void uploadFlightPlan(@NonNull File flightPlan);

        /**
         * Activates this piloting interface and starts executing the uploaded flight plan.
         *
         * @param restart {@code true} to force restarting the flight plan
         *
         * @return {@code true} on success, {@code false} in case the piloting interface cannot be activated
         */
        boolean activate(boolean restart);

        /**
         * Sets return home on disconnect behavior.
         *
         * @param enable {@code true} to enable return home on disconnect, {@code false} to disable it.
         *
         * @return {@code true} if the value could successfully be set or sent to the device, {@code false} otherwise
         */
        boolean setReturnHomeOnDisconnect(boolean enable);
    }

    /** Backend of this interface. */
    @NonNull
    private final Backend mBackend;

    /** Set of reasons why this piloting interface is currently unavailable. */
    @NonNull
    private final EnumSet<UnavailabilityReason> mUnavailabilityReasons;

    /** Latest activation error. */
    @NonNull
    private ActivationError mActivationError;

    /** Latest Flight Plan mavlink file upload state. */
    @NonNull
    private UploadState mUploadState;

    /** Whether drone flight plan is known. */
    private boolean mFlightPlanKnown;

    /** Whether the current flight plan is paused. */
    private boolean mPaused;

    /** Latest mission item executed index. */
    private int mMissionItemExecuted;

    /** Return Home on Disconnect Setting. */
    @NonNull
    private final ReturnHomeOnDisconnectSettingCore mReturnHomeOnDisconnectSetting;

    /**
     * Constructor.
     *
     * @param pilotingItfStore store where this piloting interface belongs.
     * @param backend          backend used to forward actions to the engine
     */
    public FlightPlanPilotingItfCore(@NonNull ComponentStore<PilotingItf> pilotingItfStore, @NonNull Backend backend) {
        super(DESC, pilotingItfStore, backend);
        mBackend = backend;
        mUnavailabilityReasons = EnumSet.of(UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE);
        mActivationError = ActivationError.NONE;
        mUploadState = UploadState.NONE;
        mMissionItemExecuted = -1;
        mReturnHomeOnDisconnectSetting = new ReturnHomeOnDisconnectSettingCore(this::onSettingChange,
                mBackend::setReturnHomeOnDisconnect);
    }

    @Override
    public void unpublish() {
        super.unpublish();
        cancelSettingsRollbacks();
    }

    @Override
    public void uploadFlightPlan(@NonNull File flightPlanFile) {
        mBackend.uploadFlightPlan(flightPlanFile);
    }

    @Override
    public boolean activate(boolean restart) {
        return getState() == State.IDLE && mBackend.activate(restart);
    }

    @NonNull
    @Override
    public Set<UnavailabilityReason> getUnavailabilityReasons() {
        return Collections.unmodifiableSet(mUnavailabilityReasons);
    }

    @NonNull
    @Override
    public ActivationError getLatestActivationError() {
        return mActivationError;
    }

    @NonNull
    @Override
    public UploadState getLatestUploadState() {
        return mUploadState;
    }

    @Override
    public boolean isFlightPlanFileKnown() {
        return mFlightPlanKnown;
    }

    @Override
    public boolean isPaused() {
        return mPaused;
    }

    @Override
    public int getLatestMissionItemExecuted() {
        return mMissionItemExecuted;
    }

    @NonNull
    @Override
    public ReturnHomeOnDisconnectSettingCore getReturnHomeOnDisconnect() {
        return mReturnHomeOnDisconnectSetting;
    }

    /**
     * Adds a reason to the set of unavailability reasons.
     *
     * @param reason unavailability reason to add
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public FlightPlanPilotingItfCore addUnavailabilityReason(@NonNull UnavailabilityReason reason) {
        mChanged |= mUnavailabilityReasons.add(reason);
        return this;
    }

    /**
     * Removes a reason from the set of unavailability reasons.
     *
     * @param reason unavailability reason to remove
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public FlightPlanPilotingItfCore removeUnavailabilityReason(@NonNull UnavailabilityReason reason) {
        mChanged |= mUnavailabilityReasons.remove(reason);
        return this;
    }

    /**
     * Resets the set of unavailability reasons.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public FlightPlanPilotingItfCore resetUnavailabilityReasons() {
        EnumSet<UnavailabilityReason> reasons = EnumSet.of(UnavailabilityReason.MISSING_FLIGHT_PLAN_FILE);
        mChanged |= mUnavailabilityReasons.retainAll(reasons) | mUnavailabilityReasons.addAll(reasons);
        return this;
    }

    /**
     * Updates the activation error.
     *
     * @param error activation error to set
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public FlightPlanPilotingItfCore updateActivationError(@NonNull ActivationError error) {
        if (mActivationError != error) {
            mActivationError = error;
            mChanged = true;
        }
        return this;
    }

    /**
     * Clears the activation error if it matches the given one.
     *
     * @param error activation error to clear
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public FlightPlanPilotingItfCore clearActivationError(@NonNull ActivationError error) {
        if (mActivationError == error && error != ActivationError.NONE) {
            mActivationError = ActivationError.NONE;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the latest Flight Plan mavlink file upload state.
     *
     * @param state new file upload state to set
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public FlightPlanPilotingItfCore updateUploadState(@NonNull UploadState state) {
        if (mUploadState != state) {
            mUploadState = state;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the flight plan known value.
     *
     * @param known new flight plan known value
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public FlightPlanPilotingItfCore updateFlightPlanKnown(boolean known) {
        if (mFlightPlanKnown != known) {
            mFlightPlanKnown = known;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates paused value.
     *
     * @param paused new paused value
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public FlightPlanPilotingItfCore updatePaused(boolean paused) {
        if (mPaused != paused) {
            mPaused = paused;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the latest mission item executed.
     *
     * @param index new mission item executed index
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public FlightPlanPilotingItfCore updateMissionItemExecuted(int index) {
        if (mMissionItemExecuted != index) {
            mMissionItemExecuted = index;
            mChanged = true;
        }
        return this;
    }

    /**
     * Cancels all pending settings rollbacks.
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public FlightPlanPilotingItfCore cancelSettingsRollbacks() {
        mReturnHomeOnDisconnectSetting.cancelRollback();
        return this;
    }

    /**
     * Notified when an user setting changes.
     * <p>
     * In case the change originates from the user modifying the setting value, updates the store to show the setting
     * is updating.
     *
     * @param fromUser {@code true} if the change originates from the user, otherwise {@code false}
     */
    private void onSettingChange(boolean fromUser) {
        mChanged = true;
        if (fromUser) {
            notifyUpdated();
        }
    }
}
