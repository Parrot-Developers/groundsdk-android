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

package com.parrot.drone.groundsdk.internal.device.pilotingitf;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.pilotingitf.FollowMePilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.LookAtPilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.device.pilotingitf.tracking.TrackingIssue;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;

import java.util.Collection;
import java.util.EnumSet;

import static com.parrot.drone.groundsdk.internal.value.IntegerRangeCore.SIGNED_PERCENTAGE;

/**
 * Base implementation for {@link PilotingItf piloting interfaces} that feature target tracking, such as
 * {@link LookAtPilotingItf} and {@link FollowMePilotingItf}.
 */
public class TrackingPilotingItfCore extends ActivablePilotingItfCore {

    /** Backend of a tracking piloting interface. */
    public interface Backend extends ActivablePilotingItfCore.Backend {

        /**
         * Sets the piloting command pitch value.
         *
         * @param pitch piloting command pitch
         */
        void setPitch(int pitch);

        /**
         * Sets the piloting command roll value.
         *
         * @param roll piloting command pitch
         */
        void setRoll(int roll);

        /**
         * Sets the piloting command vertical speed value.
         *
         * @param verticalSpeed piloting command vertical speed
         */
        void setVerticalSpeed(int verticalSpeed);
    }

    /** Backend of this interface. */
    @NonNull
    private final Backend mBackend;

    /** Set of issues that renders this piloting interface currently unavailable. */
    @NonNull
    private final EnumSet<TrackingIssue> mAvailabilityIssues;

    /** Set of issues that hinders optimal behavior of this piloting interface. */
    @NonNull
    private final EnumSet<TrackingIssue> mQualityIssues;

    /**
     * Constructor.
     *
     * @param descriptor       specific descriptor of the implemented piloting interface
     * @param pilotingItfStore store where this piloting interface belongs
     * @param backend          backend used to forward actions to the engine
     */
    TrackingPilotingItfCore(@NonNull ComponentDescriptor<PilotingItf, ? extends PilotingItf> descriptor,
                            @NonNull ComponentStore<PilotingItf> pilotingItfStore, @NonNull Backend backend) {
        super(descriptor, pilotingItfStore, backend);
        mBackend = backend;
        mAvailabilityIssues = EnumSet.noneOf(TrackingIssue.class);
        mQualityIssues = EnumSet.noneOf(TrackingIssue.class);
    }

    /**
     * Activates the piloting interface.
     *
     * @return {@code true} on success, {@code false} in case the piloting interface cannot be activated at this point
     */
    public boolean activate() {
        return getState() == State.IDLE && mBackend.activate();
    }

    /**
     * Retrieves the current set of unavailability issues.
     *
     * @return set of unavailability issues
     */
    @NonNull
    public EnumSet<TrackingIssue> getAvailabilityIssues() {
        return EnumSet.copyOf(mAvailabilityIssues);
    }

    /**
     * Retrieves the current set of quality issues.
     *
     * @return set of quality issues
     */
    @NonNull
    public EnumSet<TrackingIssue> getQualityIssues() {
        return EnumSet.copyOf(mQualityIssues);
    }

    /**
     * Sets the current pitch value.
     *
     * @param pitch the new pitch value to set
     */
    public void setPitch(@IntRange(from = -100, to = 100) int pitch) {
        mBackend.setPitch(SIGNED_PERCENTAGE.clamp(pitch));
    }

    /**
     * Sets the current roll value.
     *
     * @param roll the new roll value to set
     */
    public void setRoll(@IntRange(from = -100, to = 100) int roll) {
        mBackend.setRoll(SIGNED_PERCENTAGE.clamp(roll));
    }

    /**
     * Sets the current vertical speed value.
     *
     * @param speed the new vertical speed value to set
     */
    public void setVerticalSpeed(@IntRange(from = -100, to = 100) int speed) {
        mBackend.setVerticalSpeed(SIGNED_PERCENTAGE.clamp(speed));
    }

    /**
     * Updates current set of availability issues.
     *
     * @param issues new set of availability issues
     *
     * @return {@code this}, to allow call chaining
     */
    public TrackingPilotingItfCore updateAvailabilityIssues(@NonNull Collection<TrackingIssue> issues) {
        mChanged |= mAvailabilityIssues.retainAll(issues) | mAvailabilityIssues.addAll(issues);
        return this;
    }

    /**
     * Updates current set of quality issues.
     *
     * @param issues new set of quality issues
     *
     * @return {@code this}, to allow call chaining
     */
    public TrackingPilotingItfCore updateQualityIssues(@NonNull Collection<TrackingIssue> issues) {
        mChanged |= mQualityIssues.retainAll(issues) | mQualityIssues.addAll(issues);
        return this;
    }
}
