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

package com.parrot.drone.groundsdk.arsdkengine.pilotingitf;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.PilotingItfActivationController;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.groundsdk.internal.device.pilotingitf.ActivablePilotingItfCore;
import com.parrot.drone.sdkcore.ulog.ULog;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_PITF;

/**
 * Specialization of a PilotingItfController for activable piloting interface components.
 */
public abstract class ActivablePilotingItfController extends PilotingItfController {

    /** Interface for creating an activable piloting interface controller. */
    public interface Factory {

        /**
         * Creates a new {@code PilotingItfActivationController} instance.
         *
         * @param controller the activation controller that owns the created piloting interface controller.
         *
         * @return a new {@code PilotingItfActivationController} instance
         */
        @NonNull
        ActivablePilotingItfController create(@NonNull PilotingItfActivationController controller);
    }

    /** Activation controller. */
    @NonNull
    private final PilotingItfActivationController mActivationController;

    /** {@code true} if this controller requires a piloting command loop when active. */
    private final boolean mSendsPilotingCommands;

    /**
     * Constructor.
     *
     * @param activationController  activation controller that owns this piloting interface controller
     * @param sendsPilotingCommands {@code true} if this piloting interface needs to send piloting commands when active
     */
    protected ActivablePilotingItfController(@NonNull PilotingItfActivationController activationController,
                                             boolean sendsPilotingCommands) {
        super(activationController.getDroneController());
        mActivationController = activationController;
        mSendsPilotingCommands = sendsPilotingCommands;
    }

    /**
     * Retrieves the piloting interface managed by this controller.
     *
     * @return the managed piloting interface
     */
    @NonNull
    public abstract ActivablePilotingItfCore getPilotingItf();

    @CallSuper
    @Override
    protected void onDisconnected() {
        notifyUnavailable();
    }

    /**
     * Tells whether the managed piloting interface may be activated currently.
     * <p>
     * This base implementation only ensures that the piloting interface is {@link Activable.State#IDLE idle}. <br/>
     * More specific {@code PilotingItfActivationController} implementations may override this method to perform
     * additional checks, but should at least call back through the default check first.
     *
     * @return {@code true} if the piloting interface may be activated, otherwise {@code false}
     */
    public boolean canActivate() {
        return getPilotingItf().getState() == Activable.State.IDLE;
    }

    /**
     * Requests activation of the managed piloting interface.
     * <p>
     * Implementation <strong>MUST NOT</strong> check whether it is currently appropriate to activate the interface, but
     * <strong>MUST</strong> take immediate action to activate it.
     */
    @CallSuper
    public void requestActivation() {
        if (ULog.d(TAG_PITF)) {
            ULog.d(TAG_PITF, "Sending activation request for " + this);
        }
    }

    /**
     * Tells whether the managed piloting interface may be deactivated currently.
     * <p>
     * This base implementation only ensures that the piloting interface is {@link Activable.State#ACTIVE active}. <br/>
     * More specific {@code PilotingItfActivationController} implementations may override this method to perform
     * additional checks, but should at least call back through the default check first.
     *
     * @return {@code true} if the piloting interface may be deactivated, otherwise {@code false}
     */
    public boolean canDeactivate() {
        return getPilotingItf().getState() == Activable.State.ACTIVE;
    }

    /**
     * Requests deactivation of the managed piloting interface.
     * <p>
     * Implementation <strong>MUST NOT</strong> check whether it is currently appropriate to deactivate the interface,
     * but <strong>MUST</strong> take immediate action to deactivate it.
     * </p>
     */
    @CallSuper
    public void requestDeactivation() {
        if (ULog.d(TAG_PITF)) {
            ULog.d(TAG_PITF, "Sending deactivation request for " + this);
        }
    }

    /**
     * Notifies that the managed piloting interface is currently unavailable.
     * <p>
     * Calling this method always triggers a {@link ActivablePilotingItfCore#notifyUpdated()} call on the controlled
     * piloting interface.
     */
    protected final void notifyUnavailable() {
        mActivationController.onInactive(this);
        getPilotingItf().updateState(Activable.State.UNAVAILABLE).notifyUpdated();
    }

    /**
     * Notifies that the managed piloting interface is currently available.
     * <p>
     * Calling this method always triggers a {@link ActivablePilotingItfCore#notifyUpdated()} call on the controlled
     * piloting interface.
     */
    protected final void notifyIdle() {
        mActivationController.onInactive(this);
        getPilotingItf().updateState(Activable.State.IDLE).notifyUpdated();
    }

    /**
     * Notifies that the managed piloting interface is currently active.
     * <p>
     * Calling this method always triggers a {@link ActivablePilotingItfCore#notifyUpdated()} call on the controlled
     * piloting interface.
     */
    protected final void notifyActive() {
        mActivationController.onActive(this, mSendsPilotingCommands);
        getPilotingItf().updateState(Activable.State.ACTIVE).notifyUpdated();
    }

    /**
     * Sets the current piloting command pitch value for this piloting interface.
     *
     * @param pitch piloting command pitch
     *
     * @return {@code this}, to allow call chaining
     */
    protected final ActivablePilotingItfController setPitch(int pitch) {
        mActivationController.onPitch(this, pitch);
        return this;
    }

    /**
     * Sets the current piloting command roll value for this piloting interface.
     *
     * @param roll piloting command roll
     *
     * @return {@code this}, to allow call chaining
     */
    protected final ActivablePilotingItfController setRoll(int roll) {
        mActivationController.onRoll(this, roll);
        return this;
    }

    /**
     * Sets the current piloting command yaw value for this piloting interface.
     *
     * @param yaw piloting command yaw
     *
     * @return {@code this}, to allow call chaining
     */
    protected final ActivablePilotingItfController setYaw(int yaw) {
        mActivationController.onYaw(this, yaw);
        return this;
    }

    /**
     * Sets the current piloting command gaz value for this piloting interface.
     *
     * @param gaz piloting command gaz
     *
     * @return {@code this}, to allow call chaining
     */
    protected final ActivablePilotingItfController setGaz(int gaz) {
        mActivationController.onGaz(this, gaz);
        return this;
    }

    /** ActivablePilotingItfCore base backend implementation. */
    protected class Backend implements ActivablePilotingItfCore.Backend {

        @Override
        public final boolean activate() {
            return mActivationController.activate(ActivablePilotingItfController.this);
        }

        @Override
        public final boolean deactivate() {
            return mActivationController.deactivate(ActivablePilotingItfController.this);
        }

    }

    @NonNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [device: " + mDeviceController.getUid()
               + ", state: " + getPilotingItf().getState() + "]";
    }
}
