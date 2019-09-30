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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.DroneFinder;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Core class for the DroneFinder. */
public class DroneFinderCore extends SingletonComponentCore implements DroneFinder {

    /** Description of DroneFinder. */
    private static final ComponentDescriptor<Peripheral, DroneFinder> DESC = ComponentDescriptor.of(DroneFinder.class);

    /** Engine-specific backend for the DroneFinder. */
    public interface Backend {

        /**
         * Starts visible drones discovery.
         */
        void discoverDrones();

        /**
         * Connects a remote drone.
         *
         * @param uid      uid of the drone to connect
         * @param password password to use for connection. Use {@code null} if the connection is not secured or to
         *                 use the RC's stored password for that drone, if any.
         *
         * @return {@code true} if the connection process has started, {@code false} otherwise.
         */
        boolean connectDrone(@NonNull String uid, @Nullable String password);
    }

    /** Core class for DiscoveredDrone. */
    public static final class DiscoveredDroneCore extends DiscoveredDrone {

        /** Discovered drone uid. */
        @NonNull
        private final String mUid;

        /** Discovered drone model. */
        @NonNull
        private final Drone.Model mModel;

        /** Discovered drone name. */
        @NonNull
        private final String mName;

        /** Discovered drone connection security. */
        @NonNull
        private final ConnectionSecurity mConnectionSecurity;

        /** Discovered drone RSSI. */
        private final int mRssi;

        /** {@code true} if the drone is known by the RC, otherwise {@code false}. */
        private final boolean mKnown;

        /**
         * Constructor.
         *
         * @param uid                drone uid
         * @param model              drone model
         * @param name               drone name
         * @param connectionSecurity drone connection security
         * @param rssi               drone RSSI
         * @param known              {@code true} if the drone is known, otherwise {@code false}
         */
        public DiscoveredDroneCore(@NonNull String uid, @NonNull Drone.Model model, @NonNull String name,
                                   @NonNull ConnectionSecurity connectionSecurity, int rssi, boolean known) {
            mUid = uid;
            mModel = model;
            mName = name;
            mConnectionSecurity = connectionSecurity;
            mRssi = rssi;
            mKnown = known;
        }

        @NonNull
        @Override
        public String getUid() {
            return mUid;
        }

        @NonNull
        @Override
        public Drone.Model getModel() {
            return mModel;
        }

        @NonNull
        @Override
        public String getName() {
            return mName;
        }

        @NonNull
        @Override
        public ConnectionSecurity getConnectionSecurity() {
            return mConnectionSecurity;
        }

        @Override
        public int getRssi() {
            return mRssi;
        }

        @Override
        public boolean isKnown() {
            return mKnown;
        }
    }

    /** Empty discovered drone array. */
    private static final DiscoveredDroneCore[] NO_DISCOVERED_DRONES = new DiscoveredDroneCore[0];

    /** Engine peripheral backend. */
    @NonNull
    private final Backend mBackend;

    /** Current drone finder state. */
    @NonNull
    private State mState;

    /** Current list of discovered drones. */
    @NonNull
    private DiscoveredDroneCore[] mDiscoveredDrones;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public DroneFinderCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
        mState = State.IDLE;
        mDiscoveredDrones = NO_DISCOVERED_DRONES;
    }

    @NonNull
    @Override
    public final State getState() {
        return mState;
    }

    @NonNull
    @Override
    public final List<DiscoveredDrone> getDiscoveredDrones() {
        return Arrays.asList(mDiscoveredDrones);
    }

    @Override
    public final void clear() {
        updateDiscoveredDrones(NO_DISCOVERED_DRONES).notifyUpdated();
    }

    @Override
    public final void refresh() {
        mBackend.discoverDrones();
    }

    @Override
    public boolean connect(@NonNull DiscoveredDrone drone) {
        return mBackend.connectDrone(drone.getUid(), null);
    }

    @Override
    public boolean connect(@NonNull DiscoveredDrone drone, @NonNull String password) {
        return mBackend.connectDrone(drone.getUid(), password);
    }

    /**
     * Updates current discovered drones array.
     * <p>
     * Note that changes are not notified until {@link #notifyUpdated()} is called
     * <p>
     * The received array is a copy that can safely be owned by the DroneFinderCore receiver.
     *
     * @param discoveredDrones new discovered drone array
     *
     * @return this, to allow call chaining
     */
    public final DroneFinderCore updateDiscoveredDrones(@NonNull DiscoveredDroneCore[] discoveredDrones) {
        mDiscoveredDrones = discoveredDrones;
        Arrays.sort(mDiscoveredDrones, COMPARATOR);
        mChanged = true;
        return this;
    }

    /**
     * Updates current drone finder state.
     * <p>
     * Note that changes are not notified until {@link #notifyUpdated()} is called
     *
     * @param state new state
     *
     * @return this, to allow call chaining
     */
    public final DroneFinderCore updateState(@NonNull State state) {
        if (state != mState) {
            mState = state;
            mChanged = true;
        }
        return this;
    }

    /**
     * Comparator used to order drones.
     * <p>
     * Drones are ordered first by signal level (with high signal level drones at the beginning of the list), then
     * by name (for drone with equal signal levels).
     */
    private static final Comparator<DiscoveredDroneCore> COMPARATOR = (lhs, rhs) -> {
        int signalDiff = rhs.getRssi() - lhs.getRssi();
        return signalDiff == 0 ? lhs.getName().compareTo(rhs.getName()) : signalDiff;
    };
}
