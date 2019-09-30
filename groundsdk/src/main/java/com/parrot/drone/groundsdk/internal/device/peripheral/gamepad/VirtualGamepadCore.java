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

package com.parrot.drone.groundsdk.internal.device.peripheral.gamepad;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.VirtualGamepad;
import com.parrot.drone.groundsdk.device.peripheral.gamepad.ButtonsMappableAction;
import com.parrot.drone.groundsdk.internal.ApplicationNotifier;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/**
 * Core class for the VirtualGamepad.
 */
public final class VirtualGamepadCore extends SingletonComponentCore implements VirtualGamepad {

    /** Description of SkyController2Gamepad. */
    private static final ComponentDescriptor<Peripheral, VirtualGamepad> DESC =
            ComponentDescriptor.of(VirtualGamepad.class);

    /** Engine-specific backend for the VirtualGamepad. */
    public interface Backend {

        /**
         * Grabs navigation inputs.
         *
         * @return {@code true} if navigation inputs could be grabbed, otherwise {@code false}
         */
        boolean grabNavigation();

        /**
         * Release navigation inputs.
         */
        void releaseNavigation();
    }

    /** Engine peripheral backend. */
    @NonNull
    private final Backend mBackend;

    /** Application navigation event listener. */
    @Nullable
    private Event.Listener mListener;

    /**
     * {@code true} when the navigation inputs are grabbed, or when navigation is preempted but must be grabbed back
     * when the preemption ends.
     */
    private boolean mGrabbed;

    /** {@code true} when a more specialized gamepad interface has grabbed inputs and preempts virtual gamepad. */
    private boolean mPreempted;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public VirtualGamepadCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
    }

    @Override
    public void unpublish() {
        mListener = null;
        mGrabbed = mPreempted = false;
        super.unpublish();
    }

    @NonNull
    @Override
    public State getState() {
        return mGrabbed ? mPreempted ? State.PREEMPTED : State.GRABBED : State.RELEASED;
    }

    @Override
    public boolean canGrab() {
        return !mGrabbed && !mPreempted;
    }

    @Override
    public boolean grab(@NonNull Event.Listener listener) {
        if (!mGrabbed && !mPreempted) {
            mListener = listener;
            return mBackend.grabNavigation();
        }
        return false;
    }

    @Override
    public void release() {
        if (mGrabbed) {
            mListener = null;
            mBackend.releaseNavigation();
        }
    }

    /**
     * Updates the grab flag.
     *
     * @param grabbed {@code true} to mark the gamepad grabbed, otherwise {@code false}
     *
     * @return this, to allow call chaining
     */
    public VirtualGamepadCore updateGrabbed(boolean grabbed) {
        if (mGrabbed != grabbed) {
            mGrabbed = grabbed;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the preempted flag.
     *
     * @param preempted {@code true} to mark the gamepad preempted, otherwise {@code false}
     *
     * @return this, to allow call chaining
     */
    public VirtualGamepadCore updatePreempted(boolean preempted) {
        if (mPreempted != preempted) {
            mPreempted = preempted;
            mChanged = true;
        }
        return this;
    }

    /**
     * Forwards a navigation event to the application.
     *
     * @param event navigation event to forward
     * @param state navigation event state
     */
    public void notifyNavigationEvent(@NonNull Event event, @NonNull Event.State state) {
        if (mListener != null) {
            mListener.onEvent(event, state);
        }
    }

    /**
     * Forwards a gamepad application event to the application.
     *
     * @param appAction the application event action to forward
     */
    public static void notifyAppEvent(@NonNull ButtonsMappableAction appAction) {
        ApplicationNotifier.getInstance().broadcastIntent(
                new Intent(ACTION_GAMEPAD_APP_EVENT).putExtra(EXTRA_GAMEPAD_APP_EVENT_ACTION, appAction.ordinal()));
    }
}
