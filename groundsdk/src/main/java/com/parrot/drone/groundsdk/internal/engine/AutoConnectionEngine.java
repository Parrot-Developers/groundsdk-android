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

package com.parrot.drone.groundsdk.internal.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.facility.AutoConnection;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.device.DeviceCore;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.facility.AutoConnectionCore;
import com.parrot.drone.groundsdk.internal.utility.DeviceStore;
import com.parrot.drone.groundsdk.internal.utility.DroneStore;
import com.parrot.drone.groundsdk.internal.utility.RemoteControlStore;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Engine that provides automatic devices connection capabilities.
 */
public class AutoConnectionEngine extends EngineBase {

    /** AutoConnection facility for which this object is the backend. */
    @NonNull
    private final AutoConnectionCore mAutoConnection;

    /** Drone store. Effectively non-{@code null} after {@link #onStart}. */
    private DroneStore mDroneStore;

    /** RemoteControl store. Effectively non-{@code null} after {@link #onStart}. */
    private RemoteControlStore mRemoteControlStore;

    /** Current remote control elected for auto-connection, {@code null} if none. */
    @Nullable
    private RemoteControlCore mCurrentRc;

    /** Current drone for auto-connection, {@code null} if none. */
    @Nullable
    private DroneCore mCurrentDrone;

    /** Drone that the auto-connection should try to reconnect, {@code null} if none. */
    @Nullable
    private DroneCore mDroneToReconnect;

    /**
     * Constructor.
     *
     * @param controller provides access to the engine's controller; opaque to subclasses, which should forward it
     *                   directly through {@code super(controller)}
     */
    AutoConnectionEngine(@NonNull Controller controller) {
        super(controller);
        mAutoConnection = new AutoConnectionCore(getFacilityPublisher(), mBackend);
    }

    @Override
    protected final void onStart() {
        mDroneStore = getUtilityOrThrow(DroneStore.class);
        mRemoteControlStore = getUtilityOrThrow(RemoteControlStore.class);
        // start auto-connection if required by config
        if (GroundSdkConfig.get().shouldAutoConnectAtStartup()) {
            mBackend.startAutoConnection();
        }
        // publish facility
        mAutoConnection.publish();
    }

    @Override
    protected final void onStopRequested() {
        // unpublish facility
        mAutoConnection.unpublish();
        // stop auto connection
        mBackend.stopAutoConnection();
        // tell we are prepared to stop
        acknowledgeStopRequest();
    }

    /**
     * Processes the current set of devices whenever it changes and take appropriate auto-connection measures.
     */
    private void processDevices() {
        // RC auto-connection
        NavigableSet<RemoteControlCore> rcs = mRemoteControlStore
                .all().stream()
                .filter(AutoConnectionEngine::isVisible)
                .collect(Collectors.toCollection(() -> new TreeSet<>(mAutoConnectionSort)));

        if (rcs.isEmpty()) {
            mCurrentRc = null;
        } else {
            mCurrentRc = rcs.first();
            // this is the best rc, ensure it is connecting or connected
            if (!isAtLeastConnecting(mCurrentRc)) {
                // request connection now if rc is not connected
                if (mCurrentRc.getDeviceStateCore().canBeConnected()) {
                    mCurrentRc.connect(null, null);
                }
            } else if (!usesBestConnector(mCurrentRc) && mCurrentRc.getDeviceStateCore().canBeDisconnected()) {
                // disconnect rc if not connected with best connector. Next pass will reconnect with proper connector
                mCurrentRc.disconnect();
            }
            // ensure all other rcs are disconnected
            for (RemoteControlCore rcToDisconnect : rcs.tailSet(mCurrentRc, false)) {
                if (rcToDisconnect.getDeviceStateCore().canBeDisconnected()) {
                    if (mDroneToReconnect == null) {
                        mDroneToReconnect = findDroneConnectedWith(rcToDisconnect);
                    }
                    rcToDisconnect.disconnect();
                }
            }
        }
        // Drones auto-connection
        if (mCurrentRc != null) {  // Drone with RC auto-connection
            // first disconnect all drones that are not connected or connecting to the RC
            mDroneStore.all().stream()
                       .filter(drone -> connectedButNotWith(drone, mCurrentRc))
                       .sorted(mAutoConnectionSort).forEachOrdered((drone) -> {
                if (drone.getDeviceStateCore().canBeDisconnected()) {
                    if (mDroneToReconnect == null) {
                        mDroneToReconnect = drone;
                    }
                    drone.disconnect();
                }
            });

            // then check whether the RC is connected to some drone.
            DroneCore rcConnectedDrone = findDroneConnectedWith(mCurrentRc);
            if (rcConnectedDrone != null) { // nothing to do, just forget the drone to reconnect
                mCurrentDrone = rcConnectedDrone;
                mDroneToReconnect = null;
            } else if (mCurrentRc.getDeviceStateCore().getConnectionState() == DeviceState.ConnectionState.CONNECTED) {
                // here we have waited until the rc is fully connected, to be sure that we respect its choice to
                // connect to a drone by itself, the case being.
                // Now we may try the drone to reconnect
                if (mDroneToReconnect != null) {
                    mCurrentDrone = mDroneToReconnect; // reset to null if we the drone is not visible by the rc
                    DeviceConnector connector = getDroneRemoteConnector(mDroneToReconnect, mCurrentRc);
                    if (connector != null) {
                        mDroneToReconnect.connect(connector, null);
                    } else {
                        mCurrentDrone = null;
                    }
                }
            }
        } else { // Drones without RC auto-connection
            NavigableSet<DroneCore> drones = mDroneStore
                    .all().stream()
                    .filter(AutoConnectionEngine::isVisible)
                    .collect(Collectors.toCollection(() -> new TreeSet<>(mAutoConnectionSort)));
            if (drones.isEmpty()) {
                mCurrentDrone = null;
            } else {
                mCurrentDrone = drones.first();
                // this is the best drone, ensure it is connecting or connected
                if (!isAtLeastConnecting(mCurrentDrone)) {
                    // request connection now if drone is not connected
                    if (mCurrentDrone.getDeviceStateCore().canBeConnected()) {
                        mCurrentDrone.connect(null, null);
                    }
                } else if (!usesBestConnector(mCurrentDrone) &&
                           mCurrentDrone.getDeviceStateCore().canBeDisconnected()) {
                    // disconnect drone if not connected with best connector.
                    // Next pass will reconnect with proper connector
                    mDroneToReconnect = mCurrentDrone;
                    mCurrentDrone.disconnect();
                } else {
                    mDroneToReconnect = null;
                }
                // ensure all other drones are disconnected
                for (DroneCore droneToDisconnect : drones.tailSet(mCurrentDrone, false)) {
                    if (droneToDisconnect.getDeviceStateCore().canBeDisconnected()) {
                        droneToDisconnect.disconnect();
                    }
                }
            }
        }
        // publish currently auto-connected devices
        mAutoConnection.updateRemoteControl(mCurrentRc).updateDrone(mCurrentDrone).notifyUpdated();
    }

    /**
     * Search for a drone that is currently connected (or connecting) through the given remote control.
     *
     * @param rc remote control that the drone should be connected through
     *
     * @return the uid of such a drone, if found, otherwise {@code null}
     */
    @Nullable
    private DroneCore findDroneConnectedWith(@NonNull RemoteControlCore rc) {
        return mDroneStore.all().stream().filter(droneCore -> {
            DeviceConnector connector = droneCore.getDeviceStateCore().getActiveConnector();
            return connector != null && rc.getUid().equals(connector.getUid());
        }).findFirst().orElse(null);
    }

    /**
     * A comparator used to sort devices in appropriate auto-connection order.
     * <p>
     * Device are sorted by connector technology rank first, then in case of ambiguity, by connection rank,
     * then if still ambiguous, the {@link #mDroneToReconnect drone to reconnect} is preferred from other drones. <br>
     * Finally, if two different devices still compare to equal by the aforementioned rules, one of them is chosen
     * randomly to be superior to the other (this point is required because a comparator used with a
     * {@link java.util.TreeSet} must be consistent with equals.
     * <p>
     * A set of devices sorted with this comparator will have the device that is the most eligible for auto-connection
     * first, then other devices in descending order of auto-connection importance.
     */
    private final Comparator<DeviceCore> mAutoConnectionSort = new Comparator<DeviceCore>() {

        @Override
        public int compare(@NonNull DeviceCore lhs, @NonNull DeviceCore rhs) {
            int techDiff = getTechnologyRank(rhs) - getTechnologyRank(lhs);
            if (techDiff != 0) {
                return techDiff;
            }
            int cnxDiff = getConnectionRank(rhs) - getConnectionRank(lhs);
            if (cnxDiff != 0) {
                return cnxDiff;
            }
            int reconnectionDiff = getReconnectionRank(rhs) - getReconnectionRank(lhs);
            if (reconnectionDiff != 0) {
                return reconnectionDiff;
            }
            // TreeSet comparator needs to be consistent with equals, so if both objects are different but yield a
            // 0 diff, we still need to return a value != 0.
            return rhs.equals(lhs) ? 0 : 1;
        }

        /**
         * Ranks the given device higher if it happens to be the
         * {@link AutoConnectionEngine#mDroneToReconnect drone to reconnect}.
         *
         * @param device device to rank
         *
         * @return {@code 1} if the device is the drone to reconnect, otherwise {@code 0}
         */
        private int getReconnectionRank(@NonNull DeviceCore device) {
            return device.equals(mDroneToReconnect) ? 1 : 0;
        }
    };

    /**
     * Tells whether a device is visible, i.e. it has at least one connector.
     *
     * @return {@code true} if the device is visible, otherwise {@code false}
     */
    private static boolean isVisible(@NonNull DeviceCore device) {
        return device.getDeviceStateCore().getConnectors().length > 0;
    }

    /**
     * Tells whether a device is currently connected (or connecting) using a connector
     * different than the specified remote control.
     *
     * @param rc remote control to check against
     *
     * @return {@code true} if the device is connected but with a different connector than specified,
     *         otherwise {@code false}
     */
    private static boolean connectedButNotWith(@NonNull DeviceCore device, @NonNull RemoteControlCore rc) {
        DeviceConnector activeConnector = device.getDeviceStateCore().getActiveConnector();
        return activeConnector != null && !rc.getUid().equals(activeConnector.getUid());
    }

    /**
     * Tells whether a device is at least connecting, or already connected.
     *
     * @return {@code} true if the device is {@link DeviceState.ConnectionState#CONNECTING connecting} or
     *         {@link DeviceState.ConnectionState#CONNECTED connected}, otherwise false
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isAtLeastConnecting(@NonNull DeviceCore device) {
        DeviceState.ConnectionState state = device.getDeviceStateCore().getConnectionState();
        return state == DeviceState.ConnectionState.CONNECTING || state == DeviceState.ConnectionState.CONNECTED;
    }

    /**
     * Tells whether a device is currently connecting or is already connected using the best available
     * {@link DeviceConnector connector}.
     * <p>
     * The best connector is the connector featuring the highest rank as per
     * {@link #getTechnologyRank(DeviceConnector)} method.
     *
     * @param device device to query
     *
     * @return {@code true} if the device uses the best available connector, otherwise {@code false}
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean usesBestConnector(@NonNull DeviceCore device) {
        DeviceConnector activeConnector = device.getDeviceStateCore().getActiveConnector();
        int activeRank = activeConnector == null ? -1 : getTechnologyRank(activeConnector);
        return activeRank >= getTechnologyRank(device);
    }

    /**
     * Searches for a connectable remote control {@link DeviceConnector connector} for a drone.
     * <p>
     * Note that for this method to return a non-{@code null} connector, the device must be currently
     * {@link DeviceState#canBeConnected() connectable}, that is, it must be possible to connect the device immediately
     * with the returned connector.
     *
     * @param drone drone to search a connector of
     * @param rc    remote control connector to search
     *
     * @return the device connector corresponding to the given remote control if the drone is connectable using such
     *         connector, otherwise {@code null}
     */
    @Nullable
    private static DeviceConnector getDroneRemoteConnector(@NonNull DroneCore drone, @NonNull RemoteControlCore rc) {
        DeviceState state = drone.getDeviceStateCore();
        if (state.canBeConnected()) {
            for (DeviceConnector connector : state.getConnectors()) {
                if (rc.getUid().equals(connector.getUid())) {
                    return connector;
                }
            }
        }
        return null;
    }

    /**
     * Ranks a device according to its connection status.
     * <p>
     * The order is as follow (from highest rank to lowest): <ul>
     * <li>{@link DeviceState.ConnectionState#CONNECTED Connected}</li>
     * <li>{@link DeviceState.ConnectionState#CONNECTING Connecting}</li>
     * <li>{@link DeviceState.ConnectionState#DISCONNECTING Disconnecting}</li>
     * <li>{@link DeviceState.ConnectionState#DISCONNECTED Disconnected}</li>
     * <li>Any other (unsupported) {@link DeviceState.ConnectionState status}</li>
     * </ul>
     *
     * @param device device to rank
     *
     * @return device connection rank
     */
    private static int getConnectionRank(@NonNull DeviceCore device) {
        DeviceState.ConnectionState state = device.getDeviceStateCore().getConnectionState();
        switch (state) {
            case CONNECTED:
                return 4;
            case CONNECTING:
                return 3;
            case DISCONNECTING:
                return 2;
            case DISCONNECTED:
                return 1;
        }
        return 0;
    }

    /**
     * Ranks a device according to its best device connector technology rank.
     *
     * @param device device to rank
     *
     * @return device technology rank
     */
    private static int getTechnologyRank(@NonNull DeviceCore device) {
        int bestRank = -1;
        for (DeviceConnector connector : device.getDeviceStateCore().getConnectors()) {
            int rank = getTechnologyRank(connector);
            if (rank > bestRank) {
                bestRank = rank;
            }
        }
        return bestRank;
    }

    /**
     * Ranks a device connector according to its technology.
     * <p>
     * The order is as follow (from highest rank to lowest): <ul>
     * <li>{@link DeviceConnector.Technology#USB USB}</li>
     * <li>{@link DeviceConnector.Technology#WIFI Wifi}</li>
     * <li>{@link DeviceConnector.Technology#BLE BLE}</li>
     * <li>Any other (unsupported) {@link DeviceConnector.Technology technology}</li>
     * </ul>
     *
     * @param connector device connector to rank
     *
     * @return connector technology rank
     */
    private static int getTechnologyRank(@NonNull DeviceConnector connector) {
        switch (connector.getTechnology()) {
            case USB:
                return 3;
            case WIFI:
                return 2;
            case BLE:
                return 1;
        }
        return 0;
    }

    /** Backend of AutoConnectionCore implementation. */
    private final AutoConnectionCore.Backend mBackend = new AutoConnectionCore.Backend() {

        /** {@code true} when auto-connection is started. */
        private boolean mStarted;

        @Override
        public boolean startAutoConnection() {
            if (mStarted) {
                return false;
            }
            mCurrentDrone = null;
            mCurrentRc = null;
            mDroneToReconnect = null;
            mStarted = true;
            mAutoConnection.updateStatus(AutoConnection.Status.STARTED);
            mDroneStore.monitorWith(mStoreMonitor);
            mRemoteControlStore.monitorWith(mStoreMonitor);
            // mock a change on the monitor so that auto-connection starts
            mStoreMonitor.onChange();
            mAutoConnection.notifyUpdated();
            return true;
        }

        @Override
        public boolean stopAutoConnection() {
            if (!mStarted) {
                return false;
            }
            mDroneStore.disposeMonitor(mStoreMonitor);
            mRemoteControlStore.disposeMonitor(mStoreMonitor);
            mStarted = false;
            mAutoConnection.updateRemoteControl(null).updateDrone(null)
                           .updateStatus(AutoConnection.Status.STOPPED).notifyUpdated();
            return true;
        }
    };

    /**
     * Notified when a device is either added, removed or changes. Triggers an auto-connection pass.
     * <p>
     * Note: this implementation is merely an optimization to prevent immediate recursion into
     * {@link #processDevices} while devices are connected or disconnected by that method. <br>
     * The auto-connection would (and should) work the same if {@code processDevices} was called directly.
     */
    private final DeviceStore.Monitor<DeviceCore> mStoreMonitor = new DeviceStore.Monitor<DeviceCore>() {

        /** {@code true} when {@link AutoConnectionEngine#processDevices()} is executing. */
        private boolean mInProcess;

        /** {@code true} when a device list change notification was received while {@code mInProcess == true}. */
        private boolean mProcessAgain;

        @Override
        public void onDeviceChanged(@NonNull DeviceCore device) {
            if (device == mCurrentDrone) {
                mAutoConnection.notifyDroneStateChange();
            } else if (device == mCurrentRc) {
                mAutoConnection.notifyRcStateChange();
            }
        }

        @Override
        public void onChange() {
            mProcessAgain = true;
            while (!mInProcess && mProcessAgain) {
                mInProcess = true;
                mProcessAgain = false;
                processDevices();
                mInProcess = false;
            }
        }
    };
}
