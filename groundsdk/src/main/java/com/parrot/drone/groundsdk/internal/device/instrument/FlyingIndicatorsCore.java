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

package com.parrot.drone.groundsdk.internal.device.instrument;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.instrument.FlyingIndicators;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/** Core class for the FlyingIndicators instrument. */
public final class FlyingIndicatorsCore extends SingletonComponentCore implements FlyingIndicators {

    /** Description of FlyingIndicators. */
    private static final ComponentDescriptor<Instrument, FlyingIndicators> DESC =
            ComponentDescriptor.of(FlyingIndicators.class);

    /** Current state. */
    @NonNull
    private State mState;

    /** Current landed state. */
    @NonNull
    private LandedState mLandedState;

    /** Current flying state. */
    @NonNull
    private FlyingState mFlyingState;

    /**
     * Constructor.
     *
     * @param instrumentStore Store where this instrument belongs.
     */
    public FlyingIndicatorsCore(@NonNull ComponentStore<Instrument> instrumentStore) {
        super(DESC, instrumentStore);
        mState = State.LANDED;
        mLandedState = LandedState.INITIALIZING;
        mFlyingState = FlyingState.NONE;
    }

    @NonNull
    @Override
    public State getState() {
        return mState;
    }

    @NonNull
    @Override
    public LandedState getLandedState() {
        return mLandedState;
    }

    @NonNull
    @Override
    public FlyingState getFlyingState() {
        return mFlyingState;
    }

    /**
     * Updates the current state.
     *
     * @param newState new state
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public FlyingIndicatorsCore updateState(@NonNull State newState) {
        if (newState != mState) {
            mChanged = true;
            mState = newState;
            if (mState != State.LANDED) {
                mLandedState = LandedState.NONE;
            }
            if (mState != State.FLYING) {
                mFlyingState = FlyingState.NONE;
            }
        }
        return this;
    }

    /**
     * Updates the current landed state.
     * <p>
     * If the state if not landed, this will pass it to {@link State#LANDED}.
     *
     * @param newLandedState new landed state
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public FlyingIndicatorsCore updateLandedState(@NonNull LandedState newLandedState) {
        if ((newLandedState != LandedState.NONE) && (mState != State.LANDED)) {
            mChanged = true;
            mState = State.LANDED;
            mFlyingState = FlyingState.NONE;
        }
        if (newLandedState != mLandedState) {
            mChanged = true;
            mLandedState = newLandedState;
        }
        return this;
    }

    /**
     * Updates the current flying state.
     * <p>
     * If the state if not flying, this will pass it to {@link State#FLYING}.
     *
     * @param newFlyingState new flying state
     *
     * @return this object to allow chain calls
     */
    @NonNull
    public FlyingIndicatorsCore updateFlyingState(@NonNull FlyingState newFlyingState) {
        if ((newFlyingState != FlyingState.NONE) && (mState != State.FLYING)) {
            mChanged = true;
            mState = State.FLYING;
            mLandedState = LandedState.NONE;
        }
        if (newFlyingState != mFlyingState) {
            mChanged = true;
            mFlyingState = newFlyingState;
        }
        return this;
    }
}
