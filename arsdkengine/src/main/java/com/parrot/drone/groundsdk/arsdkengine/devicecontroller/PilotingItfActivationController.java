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

package com.parrot.drone.groundsdk.arsdkengine.devicecontroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.ActivablePilotingItfController;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.PilotingCommand;
import com.parrot.drone.groundsdk.device.pilotingitf.Activable;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.io.PrintWriter;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG_PITF;

/**
 * Coordinates activation and deactivation of the various piloting interfaces of a drone.
 * <p>
 * Basically a delegate of {@link DroneController} that encapsulates all PilotingItf-related management.
 */
public class PilotingItfActivationController {

    /** Drone controller that uses this activation controller. */
    @NonNull
    private final DroneController mDroneController;

    /** Used to encode piloting commands to send in the non-ack command loop. */
    @NonNull
    private final PilotingCommand.Encoder mPilotingCommandEncoder;

    /** Default piloting interface. */
    @NonNull
    private final ActivablePilotingItfController mDefaultPilotingItf;

    /** Currently active piloting interface. {@code null} when the drone is not connected. */
    @Nullable
    private ActivablePilotingItfController mCurrentPilotingItf;

    /** Piloting interface to activate after deactivation of the current one. */
    @Nullable
    private ActivablePilotingItfController mNextPilotingItf;

    /** {@code true} when drone protocol-level connection is complete, otherwise {@code false}. */
    private boolean mConnected;

    /**
     * Constructor.
     *
     * @param droneController           drone controller that uses this activation controller
     * @param pilotingCommandEncoder    piloting command encoder
     * @param defaultPilotingItfFactory factory used to build the default piloting interface of this drone controller
     */
    PilotingItfActivationController(@NonNull DroneController droneController,
                                    @NonNull PilotingCommand.Encoder pilotingCommandEncoder,
                                    @NonNull ActivablePilotingItfController.Factory defaultPilotingItfFactory) {
        mDroneController = droneController;
        mPilotingCommandEncoder = pilotingCommandEncoder;
        mDefaultPilotingItf = defaultPilotingItfFactory.create(this);
        mDroneController.registerComponentControllers(mDefaultPilotingItf);
    }

    /**
     * Gets the drone controller that owns this piloting interfaces activation controller.
     *
     * @return the drone controller
     */
    @NonNull
    public final DroneController getDroneController() {
        return mDroneController;
    }

    /**
     * Activates the piloting interface of the given controller.
     *
     * @param pilotingItf piloting interface controller whose interface must be activated
     *
     * @return {@code true} if the operation could be initiated, otherwise {@code false}
     */
    public boolean activate(@NonNull ActivablePilotingItfController pilotingItf) {
        if (ULog.d(TAG_PITF)) {
            ULog.d(TAG_PITF, this + " Received activation request for " + pilotingItf);
        }

        if (pilotingItf != mCurrentPilotingItf && pilotingItf.canActivate()) {
            if (mCurrentPilotingItf == null) {
                pilotingItf.requestActivation();
                return true;
            } else if (mCurrentPilotingItf.canDeactivate()) {
                mNextPilotingItf = pilotingItf;
                mCurrentPilotingItf.requestDeactivation();
                return true;
            }
        }
        return false;
    }

    /**
     * Deactivates the piloting interface of the given controller.
     * <p>
     * Only the current, non-default, piloting interface can be deactivated.
     *
     * @param pilotingItf piloting interface controller whose interface must be deactivated
     *
     * @return {@code true} if the operation could be initiated, otherwise {@code false}
     */
    public boolean deactivate(@NonNull ActivablePilotingItfController pilotingItf) {
        if (ULog.d(TAG_PITF)) {
            ULog.d(TAG_PITF, this + " Received deactivation request for " + pilotingItf);
        }

        if (pilotingItf == mCurrentPilotingItf && pilotingItf != mDefaultPilotingItf && pilotingItf.canDeactivate()) {
            mCurrentPilotingItf.requestDeactivation();
            return true;
        }
        return false;
    }

    /**
     * Called back when the drone has connected.
     */
    public void onConnected() {
        mConnected = true;
        // reset piloting command values and sequence number
        mPilotingCommandEncoder.reset();
        // if no piloting itf is activated when connection is over, then fallback to the default one
        if (mCurrentPilotingItf == null) {
            if (ULog.d(TAG_PITF)) {
                ULog.d(TAG_PITF, this + " Activating default piloting itf after drone connection");
            }
            mDefaultPilotingItf.requestActivation();
        }
    }

    /**
     * Called back when the drone has disconnected.
     */
    public void onDisconnected() {
        mConnected = false;
        mCurrentPilotingItf = mNextPilotingItf = null;
    }

    /**
     * Called back when a piloting interface declares itself inactive (either idle or unavailable).
     *
     * @param pilotingItf piloting interface controller whose interface is now idle
     */
    public void onInactive(@NonNull ActivablePilotingItfController pilotingItf) {
        if (pilotingItf == mCurrentPilotingItf) {
            mCurrentPilotingItf = null;
        }
        if (mConnected && mCurrentPilotingItf == null) {
            if (mNextPilotingItf != null) {
                mNextPilotingItf.requestActivation();
                mNextPilotingItf = null;
            } else { // fallback on the default interface
                mDefaultPilotingItf.requestActivation();
            }
        }
    }

    /**
     * Called back when a piloting interface declares itself active.
     *
     * @param pilotingItf              piloting interface controller whose interface is now active
     * @param needsPilotingCommandLoop {@code true} if the piloting interface needs the piloting command loop to be
     *                                 started, otherwise {@code false}
     */
    public void onActive(@NonNull ActivablePilotingItfController pilotingItf, boolean needsPilotingCommandLoop) {
        if (pilotingItf != mCurrentPilotingItf) {
            if (mCurrentPilotingItf != null) {
                mCurrentPilotingItf.getPilotingItf().updateState(Activable.State.IDLE).notifyUpdated();
            }
            mCurrentPilotingItf = pilotingItf;
            if (needsPilotingCommandLoop) {
                mPilotingCommandEncoder.reset();
                startPilotingCommandLoop();
            } else {
                stopPilotingCommandLoop();
            }
        }
    }

    /**
     * Called back when a piloting interface forwards a piloting command roll change.
     *
     * @param pilotingItf piloting interface from which the change originates
     * @param roll        new roll value
     */
    public void onRoll(@NonNull ActivablePilotingItfController pilotingItf, int roll) {
        if (pilotingItf == mCurrentPilotingItf && mPilotingCommandEncoder.setRoll(roll)) {
            mDroneController.onPilotingCommandChanged(mPilotingCommandEncoder.getPilotingCommand());
        }
    }

    /**
     * Called back when a piloting interface forwards a piloting command pitch change.
     *
     * @param pilotingItf piloting interface from which the change originates
     * @param pitch       new pitch value
     */
    public void onPitch(@NonNull ActivablePilotingItfController pilotingItf, int pitch) {
        if (pilotingItf == mCurrentPilotingItf && mPilotingCommandEncoder.setPitch(pitch)) {
            mDroneController.onPilotingCommandChanged(mPilotingCommandEncoder.getPilotingCommand());
        }
    }

    /**
     * Called back when a piloting interface forwards a piloting command yaw change.
     *
     * @param pilotingItf piloting interface from which the change originates
     * @param yaw         new yaw value
     */
    public void onYaw(@NonNull ActivablePilotingItfController pilotingItf, int yaw) {
        if (pilotingItf == mCurrentPilotingItf && mPilotingCommandEncoder.setYaw(yaw)) {
            mDroneController.onPilotingCommandChanged(mPilotingCommandEncoder.getPilotingCommand());
        }
    }

    /**
     * Called back when a piloting interface forwards a piloting command gaz change.
     *
     * @param pilotingItf piloting interface from which the change originates
     * @param gaz         new gaz value
     */
    public void onGaz(@NonNull ActivablePilotingItfController pilotingItf, int gaz) {
        if (pilotingItf == mCurrentPilotingItf && mPilotingCommandEncoder.setGaz(gaz)) {
            mDroneController.onPilotingCommandChanged(mPilotingCommandEncoder.getPilotingCommand());
        }
    }

    /**
     * Starts the piloting command loop.
     */
    private void startPilotingCommandLoop() {
        DeviceController.Backend backend = mDroneController.getProtocolBackend();
        if (backend != null) {
            backend.registerNoAckCommandEncoders(mPilotingCommandEncoder);
        }
    }

    /**
     * Stops the piloting command loop.
     */
    private void stopPilotingCommandLoop() {
        DeviceController.Backend backend = mDroneController.getProtocolBackend();
        if (backend != null) {
            backend.unregisterNoAckCommandEncoders(mPilotingCommandEncoder);
        }
        mPilotingCommandEncoder.reset();
    }

    @NonNull
    @Override
    public String toString() {
        return "PilotingItfActivationController [device:" + mDroneController.getUid() + "]";
    }

    /**
     * Debug dump.
     *
     * @param writer writer to dump to
     * @param prefix prefix string (usually indent) to prepend to each written dump line
     */
    public void dump(@NonNull PrintWriter writer, @NonNull String prefix) {
        writer.write(prefix + "PilotingItfPolicy:\n");
        writer.write(prefix + "\t- Default PilotingItf: " + mDefaultPilotingItf + "\n");
        writer.write(prefix + "\t- Active  PilotingItf: " + mCurrentPilotingItf + "\n");
        writer.write(prefix + "\t- Next    PilotingItf: " + mNextPilotingItf + "\n");
    }
}
