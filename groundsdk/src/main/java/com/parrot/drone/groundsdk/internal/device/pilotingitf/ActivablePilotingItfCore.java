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

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;
import com.parrot.drone.sdkcore.ulog.ULog;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_INTERNAL;

/**
 * Base implementation of a {@link PilotingItf} that supports the {@link Activable} interface.
 * <p>
 * Handles piloting interface activation
 */
public abstract class ActivablePilotingItfCore extends SingletonComponentCore implements PilotingItf, Activable {

    /** Base backend of a piloting interface, handles activation. */
    public interface Backend {

        /**
         * Activates the interface.
         * <p>
         * Most piloting interfaces can simply be activated with this method. But some piloting interfaces like
         * {@link com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf} should be activated using a specific
         * method, because they need parameters for activation.
         *
         * @return {@code true} if successful, {@code false} in case the piloting interface could not be activated
         */
        boolean activate();

        /**
         * Deactivates the interface.
         *
         * @return {@code true} if successful, {@code false} in case the piloting interface could not be deactivated
         */
        boolean deactivate();
    }

    /** Backend handling piloting interface activation. */
    @NonNull
    private final Backend mBackend;

    /** Piloting interface state. */
    @NonNull
    private State mState;

    /**
     * Constructor.
     *
     * @param descriptor       specific descriptor of the implemented piloting interface
     * @param pilotingItfStore store where this piloting interface belongs
     * @param backend          backend used to forward actions to the engine
     */
    protected ActivablePilotingItfCore(@NonNull ComponentDescriptor<PilotingItf, ? extends PilotingItf> descriptor,
                                       @NonNull ComponentStore<PilotingItf> pilotingItfStore,
                                       @NonNull Backend backend) {
        super(descriptor, pilotingItfStore);
        mBackend = backend;
        mState = State.UNAVAILABLE;
    }

    @Override
    public void unpublish() {
        mState = State.UNAVAILABLE;
        super.unpublish();
    }

    @NonNull
    @Override
    public final State getState() {
        return mState;
    }

    @Override
    public final boolean deactivate() {
        return mState == State.ACTIVE && mBackend.deactivate();
    }

    /**
     * Updates the internal activation state of this piloting interface.
     *
     * @param newState new activation state to set
     *
     * @return this, to allow call chaining
     */
    public final ActivablePilotingItfCore updateState(@NonNull State newState) {
        if (mState != newState) {
            if (ULog.i(TAG_INTERNAL)) {
                ULog.i(TAG_INTERNAL, "PilotingItf " + getClass().getSimpleName() + " new state: " + newState);
            }
            mState = newState;
            mChanged = true;
        }
        return this;
    }
}
