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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.internal.Logging;
import com.parrot.drone.groundsdk.internal.tasks.Executor;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.sdkcore.ulog.ULog;

/**
 * {@code SystemConnectivity} monitoring utility implementation.
 */
final class SystemConnectivityCore extends MonitorableCore<SystemConnectivity.Monitor>
        implements SystemConnectivity {

    /** Android connectivity manager, used to request network availability info. */
    @NonNull
    private final ConnectivityManager mConnectivityManager;

    /** {@code true} when internet connectivity is known to be available, otherwise {@code false}. */
    private boolean mInternetAvailable;

    /**
     * Constructor.
     *
     * @param engine engine that manages this monitorable component
     */
    SystemConnectivityCore(@NonNull SystemEngine engine) {
        super(engine);
        mConnectivityManager = (ConnectivityManager) engine.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public boolean isInternetAvailable() {
        return mInternetAvailable;
    }

    @Override
    protected void onFirstMonitor(@NonNull Monitor monitor) {
        mConnectivityManager.registerNetworkCallback(new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build(), mNetworkCallback);
    }

    @Override
    protected void onAnotherMonitor(@NonNull Monitor monitor) {
        if (mInternetAvailable) {
            notifyMonitor(monitor);
        }
    }

    @Override
    protected void onNoMoreMonitors() {
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        mInternetAvailable = false;
    }

    @Override
    protected void notifyMonitor(@NonNull Monitor monitor) {
        monitor.onInternetAvailabilityChanged(mInternetAvailable);
    }

    /** Notified by android (on a random thread) when the network request is fulfilled or does not hold anymore. */
    private final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {

        @Override
        public void onAvailable(Network network) {
            postAvailabilityChange(true);
        }

        @Override
        public void onLost(Network network) {
            postAvailabilityChange(false);
        }

        /**
         * Posts internet availability status back onto the main thread.
         *
         * @param available {@code true} when internet became available, {@code false} otherwise
         */
        private void postAvailabilityChange(boolean available) {
            Executor.postOnMainThread(new Runnable() {

                @Override
                public void run() {
                    if (mInternetAvailable != available) {
                        if (ULog.i(Logging.TAG_MONITOR)) {
                            ULog.i(Logging.TAG_MONITOR, "Internet is now " + (available ? "available" : "unavailable"));
                        }
                        mInternetAvailable = available;
                        dispatchNotification();
                    }
                }

                @NonNull
                @Override
                public String toString() {
                    return "Network " + (available ? "available" : "unavailable") + " notification";
                }
            });
        }
    };
}
