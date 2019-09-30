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

package com.parrot.drone.groundsdk.internal.engine.system;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.internal.Monitorable;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;
import com.parrot.drone.groundsdk.internal.utility.SystemBarometer;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.groundsdk.internal.utility.SystemHeading;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Engine providing monitorable system information, such as:
 * <ul>
 * <li>Internet connection availability.</li>
 * <li>Controller geographical location.</li>
 * <li>Barometer information.</li>
 * </ul>
 */
public class SystemEngine extends EngineBase {

    /** All registered monitors, by monitored component. */
    private final Map<Monitorable<?>, Set<?>> mMonitors;

    /**
     * Constructor.
     *
     * @param controller provides access to the engine's controller; opaque to subclasses, which should forward it
     *                   directly through {@code super(controller)}
     */
    public SystemEngine(@NonNull Controller controller) {
        super(controller);
        mMonitors = new HashMap<>();
        publishMonitorables();
    }

    /**
     * Creates and publishes all available monitorable components.
     */
    @VisibleForTesting
    void publishMonitorables() {
        publishUtility(SystemConnectivity.class, new SystemConnectivityCore(this));

        SystemBarometerCore barometerMonitor = SystemBarometerCore.create(this);
        if (barometerMonitor != null) {
            publishUtility(SystemBarometer.class, barometerMonitor);
        }

        SystemHeadingCore systemHeading = SystemHeadingCore.create(this);
        if (systemHeading != null) {
            publishUtility(SystemHeading.class, systemHeading);
        }
    }

    /**
     * Registers a monitor.
     *
     * @param monitorable component to register a monitor onto
     * @param monitor     monitor to register
     * @param <M>         type of the monitor interface
     */
    <M> void registerMonitor(@NonNull MonitorableCore<M> monitorable, @NonNull M monitor) {
        if (!isStoppedOrAcknowledged()) {
            @SuppressWarnings("unchecked")
            Set<M> monitors = (Set<M>) mMonitors.get(monitorable);
            if (monitors == null) {
                monitors = new CopyOnWriteArraySet<>();
                mMonitors.put(monitorable, monitors);
            }
            boolean first = monitors.isEmpty();
            boolean newMonitor = monitors.add(monitor);
            if (first) {
                monitorable.onFirstMonitor(monitor);
            } else if (newMonitor) {
                monitorable.onAnotherMonitor(monitor);
            }
        }
    }

    /**
     * Unregisters a monitor.
     *
     * @param monitorable component to unregister a monitor from
     * @param monitor     monitor to unregister
     * @param <M>         type of the monitor interface
     */
    <M> void unregisterMonitor(@NonNull MonitorableCore<M> monitorable, @NonNull M monitor) {
        @SuppressWarnings("unchecked")
        Set<M> monitors = (Set<M>) mMonitors.get(monitorable);
        if (monitors != null) {
            if (monitors.remove(monitor) && monitors.isEmpty()) {
                mMonitors.remove(monitorable);
                monitorable.onNoMoreMonitors();
                if (isRequestedToStop() && mMonitors.isEmpty()) {
                    acknowledgeStopRequest();
                }
            }
        }
    }

    /**
     * Get all monitors registered onto a specific component.
     *
     * @param monitorable component to get registered monitors from
     * @param <M>         type of the monitor interface
     *
     * @return a set of all registered monitors.
     */
    @NonNull
    <M> Set<M> getMonitors(@NonNull MonitorableCore<M> monitorable) {
        @SuppressWarnings("unchecked")
        Set<M> monitors = (Set<M>) mMonitors.get(monitorable);
        return monitors == null ? Collections.emptySet() : Collections.unmodifiableSet(monitors);
    }

    @Override
    protected void onStopRequested() {
        if (mMonitors.isEmpty()) {
            acknowledgeStopRequest();
        }
    }
}
