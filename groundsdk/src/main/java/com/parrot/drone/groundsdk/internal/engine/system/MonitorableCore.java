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

import com.parrot.drone.groundsdk.internal.Monitorable;

/**
 * Abstract base for monitorable component implementation.
 *
 * @param <M> type of monitor interface
 */
public abstract class MonitorableCore<M> implements Monitorable<M> {

    /** Engine that manages this monitorable component. */
    @NonNull
    private final SystemEngine mEngine;

    /**
     * Constructor.
     *
     * @param engine engine that manages this monitorable component
     */
    protected MonitorableCore(@NonNull SystemEngine engine) {
        mEngine = engine;
    }

    @Override
    public final void monitorWith(@NonNull M monitor) {
        mEngine.registerMonitor(this, monitor);
    }

    @Override
    public final void disposeMonitor(@NonNull M monitor) {
        mEngine.unregisterMonitor(this, monitor);
    }

    /**
     * Called after a new monitor is registered and none other already is.
     * <p>
     * Subclasses should override this method to start monitoring events for the registered monitor an all subsequent
     * ones.
     *
     * @param monitor monitor that has just been registered
     */
    protected abstract void onFirstMonitor(@NonNull M monitor);

    /**
     * Called after a new monitor is registered and some others already are.
     * <p>
     * Subclasses may override this method. Default implementation does nothing.
     *
     * @param monitor monitor that has just been registered
     */
    protected void onAnotherMonitor(@NonNull M monitor) {

    }

    /**
     * Called after the last registered monitor is unregistered.
     * <p>
     * Subclasses should override this method to stop monitoring events.
     */
    protected abstract void onNoMoreMonitors();

    /**
     * Dispatches a change notification to all registered monitors.
     */
    protected void dispatchNotification() {
        for (M monitor : mEngine.getMonitors(this)) {
            notifyMonitor(monitor);
        }
    }

    /**
     * Notifies a change to a specific monitor.
     * <p>
     * Subclass must override this method to call the appropriate notification callback on the monitor.
     *
     * @param monitor monitor to notify
     */
    protected abstract void notifyMonitor(@NonNull M monitor);
}
