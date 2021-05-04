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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.internal.engine.system.SystemLocationCore;
import com.parrot.drone.groundsdk.internal.facility.UserHeadingCore;
import com.parrot.drone.groundsdk.internal.facility.UserLocationCore;
import com.parrot.drone.groundsdk.internal.utility.SystemHeading;
import com.parrot.drone.groundsdk.internal.utility.SystemLocation;

/**
 * Engine that manages user location.
 * <p>
 * Allows the application to query user location and heading.
 */
public class UserLocationEngine extends EngineBase {

    /** UserLocation facility for which this object is the backend. */
    @NonNull
    private final UserLocationCore mUserLocation;

    /** UserHeading facility for which this object is the backend. */
    @NonNull
    private final UserHeadingCore mUserHeading;

    /**
     * System location utility. Effectively non-{@code null} after {@link #onStart} if the device supports the
     * {@link PackageManager#FEATURE_LOCATION} feature.
     */
    @Nullable
    private final SystemLocation mSystemLocation;

    /**
     * System heading utility. Effectively non-{@code null} after {@link #onStart} if the device supports the
     * {@link Sensor#TYPE_ROTATION_VECTOR} sensor.
     */
    @Nullable
    private SystemHeading mSystemHeading;

    /**
     * Constructor.
     *
     * @param controller provides access to the engine's controller; opaque to subclasses, which should forward it
     *                   directly through {@code super(controller)}
     */
    UserLocationEngine(@NonNull Controller controller) {
        super(controller);

        mUserLocation = new UserLocationCore(getFacilityPublisher(), mLocationBackend);
        mUserHeading = new UserHeadingCore(getFacilityPublisher(), mHeadingBackend);
        mSystemLocation = SystemLocationCore.create(getContext());
        if (mSystemLocation != null) {
            publishUtility(SystemLocation.class, mSystemLocation);
        }
    }

    @Override
    protected void onStart() {
        mSystemHeading = getUtility(SystemHeading.class);
        // publish facility
        mUserLocation.publish();
        mUserHeading.publish();
        getContext().registerReceiver(mLocationProviderListener, PROVIDERS_FILTER);
    }

    @Override
    protected final void onStopRequested() {
        getContext().unregisterReceiver(mLocationProviderListener);
        // unpublish facility
        mUserLocation.unpublish();
        mUserHeading.unpublish();
        // tell we are prepared to stop
        acknowledgeStopRequest();
    }

    /** Backend of UserLocationCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final UserLocationCore.Backend mLocationBackend = new UserLocationCore.Backend() {

        /** Monitors system location changes. */
        @NonNull
        private final SystemLocation.Monitor mMonitor = new SystemLocation.Monitor() {

            @Override
            public void onLocationChanged(@NonNull Location location) {
                mUserLocation.updateLocation(location).notifyUpdated();
            }

            @Override
            public void onAuthorizationChanged(boolean authorized) {
                mUserLocation.updateAuthorization(authorized).notifyUpdated();
            }
        };

        @Override
        public void startMonitoringLocation() {
            if (mSystemLocation != null) {
                mSystemLocation.monitorWith(mMonitor);
                // Update facility with current utility values
                Location location = mSystemLocation.lastKnownLocation();
                if (location != null) {
                    mUserLocation.updateLocation(location);
                }
                mUserLocation.updateAuthorization(mSystemLocation.isAuthorized()).notifyUpdated();
            }
        }

        @Override
        public void stopMonitoringLocation() {
            if (mSystemLocation != null) {
                mSystemLocation.disposeMonitor(mMonitor);
            }
        }

        @Override
        public void restartLocationUpdates() {
            if (mSystemLocation != null) {
                mSystemLocation.restartLocationUpdates();
            }
        }
    };

    /** Backend of UserHeadingCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final UserHeadingCore.Backend mHeadingBackend = new UserHeadingCore.Backend() {

        /** Monitors system heading changes. */
        @NonNull
        private final SystemHeading.Monitor mMonitor = new SystemHeading.Monitor() {

            @Override
            public void onHeadingChanged(double heading) {
                mUserHeading.updateHeading(heading).notifyUpdated();
            }
        };

        @Override
        public void startMonitoringHeading() {
            if (mSystemHeading != null) {
                mSystemHeading.monitorWith(mMonitor);
            }
        }

        @Override
        public void stopMonitoringHeading() {
            if (mSystemHeading != null) {
                mSystemHeading.disposeMonitor(mMonitor);
            }
        }
    };

    /** IntentFilter to listen to location providers changes. */
    private static final IntentFilter PROVIDERS_FILTER = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);

    /** Broadcast receiver listening on location providers changes. */
    private final BroadcastReceiver mLocationProviderListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mLocationBackend.restartLocationUpdates();
        }
    };
}
