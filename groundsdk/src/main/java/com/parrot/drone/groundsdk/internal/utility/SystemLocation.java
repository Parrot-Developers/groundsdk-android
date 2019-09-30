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

package com.parrot.drone.groundsdk.internal.utility;

import android.Manifest;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.internal.Monitorable;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;

/**
 * Utility interface allowing to monitor the system's geographical location.
 * <p>
 * This utility may be unavailable if the underlying system does not provide location information.
 * <p>
 * This utility may be obtained after engine startup using:
 * <pre>{@code SystemLocation location = getUtility(SystemLocation.class);}</pre>
 *
 * @see EngineBase#getUtility(Class)
 */
public interface SystemLocation extends Monitorable<SystemLocation.Monitor>, Utility {

    /**
     * Callback interface for receiving system location information actively.
     * <p>
     * This interface allows to monitor system location actively. When {@link #monitorWith registered}, it will request
     * periodic updates of the device's geographical location.
     * <p>
     * It is however possible to monitor system location passively, by registering a {@link Passive passive monitor}
     * instead.
     */
    interface Monitor {

        /**
         * Callback interface for receiving system location information passively.
         * <p>
         * This interface allows to monitor system location passively. When {@link #monitorWith registered}, it will
         * only receive location updates when they're available, for instance when active monitors are also registered,
         * but it will never request updates of the device's geographical location from the system by itself.
         * <p>
         * It is however possible to monitor system location actively, by registering an {@link Monitor active monitor}
         * instead.
         */
        interface Passive extends Monitor {}

        /**
         * Called back when the system geographical location changes.
         * <p>
         * Note: the location altitude returned by {@link Location#getAltitude()} is the altitude above mean sea level.
         *
         * @param location up-to-date device geographical location
         */
        void onLocationChanged(@NonNull Location location);

        /**
         * Called back when the authorization changes.
         *
         * @param authorized whether or not the system location is enabled and permission is granted
         */
        default void onAuthorizationChanged(boolean authorized) {

        }
    }

    /**
     * Gets the last known system geographical location.
     * <p>
     * Note: the location altitude returned by {@link Location#getAltitude()} is the altitude above mean sea level.
     *
     * @return last known system location if available, otherwise {@code null}
     */
    @Nullable
    Location lastKnownLocation();

    /**
     * Checks if location service is authorized. In order to retrieve system location, the appropriate
     * {@link Manifest.permission#ACCESS_FINE_LOCATION} permission must have been granted to the application,
     * and the user must have enabled location service in system settings.
     *
     * @return true if location service is authorized at both application and system level
     */
    boolean isAuthorized();

    /**
     * Starts location updates if SystemLocation is already monitored and all authorizations are granted.
     * <p>
     * If this utility is already monitored, but location monitoring could not start because system location is disabled
     * or permission is not granted yet, this method allows trying again to start location updates once all
     * authorizations are granted.
     * <p>
     * Calling this method while location monitoring is already started has no effect.
     */
    void restartLocationUpdates();

    /**
     * Forces WIFI network usage for location monitoring.
     * <p>
     * Forcing WIFI network usage through this method supersedes any {@link #denyWifiUsage WIFI usage denial request}
     * that may currently be honored, that is, if location monitoring is started and WIFI network is currently not used
     * or its usage is disallowed, then location monitoring will be forcefully restarted in order to also use WIFI
     * network.
     * <p>
     * The provided token uniquely identifies one enforcement request and allows to revoke the enforcement when
     * necessary. WIFI network will forcefully be used for location monitoring until all enforcement requests have been
     * revoked.
     * <p>
     * Note that usage of WIFI network for location monitoring may, and most probably will create perturbations or gaps
     * to the drone video stream, in case the latter is directly connected to through WIFI.
     *
     * @param token token that uniquely identifies and allows to revoke this request when necessary
     *
     * @see #revokeWifiUsageEnforcement(Object)
     */
    void enforceWifiUsage(@NonNull Object token);

    /**
     * Revokes a WIFI network usage enforcement request.
     * <p>
     * Once all outstanding enforcement requests have been revoked through this method, then outstanding
     * {@link #denyWifiUsage WIFI usage denial requests} are not superseded anymore, that is, if location monitoring
     * is started and WIFI network usage is currently disallowed, then location monitoring will be forcefully restarted
     * in order not to use WIFI network anymore.
     *
     * @param token token that uniquely identifies the request to revoke
     *
     * @see #enforceWifiUsage(Object)
     */
    void revokeWifiUsageEnforcement(@NonNull Object token);

    /**
     * Denies WIFI network usage for location monitoring.
     * <p>
     * Disallowing WIFI network usage through this method is superseded by any
     * {@link #enforceWifiUsage WIFI usage enforcement request} that may currently honored, that is, it won't have any
     * effect until all outstanding enforcement requests have been revoked.
     * <p>
     * Otherwise, if location monitoring is started and WIFI network is currently used, then location monitoring will be
     * forcefully restarted in order not to use WIFI network anymore.
     * <p>
     * The provided token uniquely identifies one denial request and allows to revoke the denial when necessary.
     * If there currently exist no outstanding enforcement requests, then WIFI network will not be used for location
     * monitoring until all denial requests have been revoked.
     *
     * @param token token that uniquely identifies and allows to revoke this request when necessary
     *
     * @see #revokeWifiUsageDenial(Object)
     */
    void denyWifiUsage(@NonNull Object token);

    /**
     * Revokes a WIFI network usage denial request.
     * <p>
     * Once all outstanding denial requests have been revoked through this method, then WIFI becomes usable for location
     * monitoring, that is, if location monitoring is started and WIFI network usage is not currently used, then
     * location monitoring will be forcefully restarted in order to also use WIFI network.
     * <p>
     * Note that usage of WIFI network for location monitoring may, and most probably will create perturbations or gaps
     * to the drone video stream, in case the latter is directly connected to through WIFI.
     *
     * @param token token that uniquely identifies the request to revoke
     *
     * @see #denyWifiUsage(Object)
     */
    void revokeWifiUsageDenial(@NonNull Object token);

    /**
     * Requests system geographical location immediately.
     * <p>
     * This should be used when system location is passively monitored. In this case location monitoring is stopped
     * as soon as location is obtained. If active monitoring is started, this method as no effect.
     */
    void requestOneLocation();
}
