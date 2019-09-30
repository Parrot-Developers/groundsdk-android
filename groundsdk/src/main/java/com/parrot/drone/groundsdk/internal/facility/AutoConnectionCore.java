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

package com.parrot.drone.groundsdk.internal.facility;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.facility.AutoConnection;
import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.internal.component.ComponentCore;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.device.DroneProxy;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.device.RemoteControlProxy;
import com.parrot.drone.groundsdk.internal.session.Session;

/** Core class for the {@link AutoConnection} facility. */
public final class AutoConnectionCore extends ComponentCore {

    /** Description of AutoConnection. */
    private static final ComponentDescriptor<Facility, AutoConnection> DESC =
            ComponentDescriptor.of(AutoConnection.class);

    /** Engine-specific backend for the AutoConnection. */
    public interface Backend {

        /**
         * Requests auto-connection to start.
         *
         * @return {@code true} if auto-connection did start, otherwise {@code false}
         */
        boolean startAutoConnection();

        /**
         * Requests auto-connection to stop.
         *
         * @return {@code true} if auto-connection did stop, otherwise {@code false}
         */
        boolean stopAutoConnection();
    }

    /** Engine facility backend. */
    @NonNull
    private final Backend mBackend;

    /** Current status. */
    @NonNull
    private AutoConnection.Status mStatus;

    /** Currently auto-connected remote control. */
    @Nullable
    private RemoteControlCore mRemoteControl;

    /** Currently auto-connected drone. */
    @Nullable
    private DroneCore mDrone;

    /**
     * Constructor.
     *
     * @param facilityStore store where this facility belongs
     * @param backend       backend used to forward actions to the engine
     */
    public AutoConnectionCore(@NonNull ComponentStore<Facility> facilityStore, @NonNull Backend backend) {
        super(DESC, facilityStore);
        mBackend = backend;
        mStatus = AutoConnection.Status.STOPPED;
    }

    /**
     * Updates current status.
     *
     * @param status new status
     *
     * @return {@code this}, to allow call chaining
     */
    @NonNull
    public AutoConnectionCore updateStatus(@NonNull AutoConnection.Status status) {
        if (mStatus != status) {
            mStatus = status;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates current auto-connected remote control.
     *
     * @param remoteControl new auto-connected remote control, or {@code null} if none
     *
     * @return {@code this}, to allow call chaining
     */
    @NonNull
    public AutoConnectionCore updateRemoteControl(@Nullable RemoteControlCore remoteControl) {
        if (mRemoteControl != remoteControl) {
            mChanged = true;
            mRemoteControl = remoteControl;
        }
        return this;
    }

    /**
     * Updates current auto-connected drone.
     *
     * @param drone new auto-connected drone, or {@code null} if none
     *
     * @return {@code this}, to allow call chaining
     */
    @NonNull
    public AutoConnectionCore updateDrone(@Nullable DroneCore drone) {
        if (mDrone != drone) {
            mDrone = drone;
            mChanged = true;
        }
        return this;
    }

    /**
     * Notifies that the state of the auto-connected drone changed.
     */
    public void notifyDroneStateChange() {
        if (mDrone != null) {
            mChanged = true;
            notifyUpdated();
        }
    }

    /**
     * Notifies that the state of the auto-connected remote control changed.
     */
    public void notifyRcStateChange() {
        if (mRemoteControl != null) {
            mChanged = true;
            notifyUpdated();
        }
    }

    @Override
    @NonNull
    protected AutoConnection getProxy(@NonNull Session session) {
        return new AutoConnection() {

            @Override
            public boolean start() {
                return mBackend.startAutoConnection();
            }

            @Override
            public boolean stop() {
                return mBackend.stopAutoConnection();
            }

            @NonNull
            @Override
            public Status getStatus() {
                return mStatus;
            }

            @Nullable
            @Override
            public RemoteControl getRemoteControl() {
                return mRemoteControl == null ? null : new RemoteControlProxy(session, mRemoteControl);
            }

            @Nullable
            @Override
            public Drone getDrone() {
                return mDrone == null ? null : new DroneProxy(session, mDrone);
            }
        };
    }
}
