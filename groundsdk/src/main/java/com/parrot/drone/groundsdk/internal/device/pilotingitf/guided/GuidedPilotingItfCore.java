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

package com.parrot.drone.groundsdk.internal.device.pilotingitf.guided;

import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.ActivablePilotingItfCore;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Core class for GuidedPilotingItf.
 */
public final class GuidedPilotingItfCore extends ActivablePilotingItfCore implements GuidedPilotingItf {

    /** Description of GuidedPilotingItf. */
    private static final ComponentDescriptor<PilotingItf, GuidedPilotingItf> DESC =
            ComponentDescriptor.of(GuidedPilotingItf.class);

    /** Backend of a GuidedPilotingItfCore which handles the messages. */
    public interface Backend extends ActivablePilotingItfCore.Backend {

        /**
         * Requests the drone to apply the specified movement directive.
         *
         * @param directive directive to apply
         */
        void move(@NonNull Directive directive);
    }

    /** Backend of this interface. */
    @NonNull
    private final Backend mBackend;

    /** Set of reasons why this piloting interface is currently unavailable. */
    @NonNull
    private final EnumSet<UnavailabilityReason> mUnavailabilityReasons;

    /** Current guided flight directive. */
    @Nullable
    private Directive mCurrentDirective;

    /** Latest terminated guided flight information. */
    @Nullable
    private FinishedFlightInfo mLatestFinishedFlightInfo;

    /**
     * Constructor.
     *
     * @param pilotingItfStore store where this piloting interface belongs
     * @param backend          backend used to forward actions to the engine
     */
    public GuidedPilotingItfCore(@NonNull ComponentStore<PilotingItf> pilotingItfStore, @NonNull Backend backend) {
        super(DESC, pilotingItfStore, backend);
        mBackend = backend;
        mUnavailabilityReasons = EnumSet.noneOf(UnavailabilityReason.class);
    }

    @NonNull
    @Override
    public Set<UnavailabilityReason> getUnavailabilityReasons() {
        return Collections.unmodifiableSet(mUnavailabilityReasons);
    }

    @Override
    public void move(@NonNull Directive directive) {
        mBackend.move(directive);
    }

    @Nullable
    @Override
    public Directive getCurrentDirective() {
        return mCurrentDirective;
    }

    @Nullable
    @Override
    public FinishedFlightInfo getLatestFinishedFlightInfo() {
        return mLatestFinishedFlightInfo;
    }

    /**
     * Updates current set of unavailability reasons.
     *
     * @param reasons new set of unavailability reasons
     *
     * @return {@code this}, to allow chained calls
     */
    @NonNull
    public GuidedPilotingItfCore updateUnavailabilityReasons(@NonNull Collection<UnavailabilityReason> reasons) {
        mChanged |= mUnavailabilityReasons.retainAll(reasons) | mUnavailabilityReasons.addAll(reasons);
        return this;
    }

    /**
     * Updates the current directive.
     *
     * @param directive the new directive
     *
     * @return the object, to allow chain calls
     */
    public GuidedPilotingItfCore updateCurrentDirective(@Nullable Directive directive) {
        if ((directive == null && mCurrentDirective != null) ||
            (directive != null && !directive.equals(mCurrentDirective))) {
            mCurrentDirective = directive;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the latest finished flight info.
     *
     * @param flightInfo the finished flight info to update
     *
     * @return the object, to allow chain calls
     */
    public GuidedPilotingItfCore updateLatestFinishedFlightInfo(@NonNull FinishedFlightInfo flightInfo) {
        if (!flightInfo.equals(mLatestFinishedFlightInfo)) {
            mLatestFinishedFlightInfo = flightInfo;
            mChanged = true;
        }
        return this;
    }
}
