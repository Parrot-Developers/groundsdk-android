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

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.facility.UserLocation;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

/** Core class for the {@link UserLocation} facility. */
public class UserLocationCore extends SingletonComponentCore implements UserLocation {

    /** Description of UserLocation. */
    private static final ComponentDescriptor<Facility, UserLocation> DESC =
            ComponentDescriptor.of(UserLocation.class);

    /** Backend of a UserLocationCore which handles the messages. */
    public interface Backend {

        /** Starts monitoring location if all authorizations are granted. */
        void startMonitoringLocation();

        /** Stops monitoring location if started. */
        void stopMonitoringLocation();

        /** Restarts location updates. */
        void restartLocationUpdates();
    }

    /** Backend of this facility. */
    @NonNull
    private final Backend mBackend;

    /** Latest known geographical location. */
    @Nullable
    private Location mLocation;

    /** Whether or not the system location is enabled and permission is granted. */
    private boolean mAuthorized;

    /**
     * Constructor.
     *
     * @param facilityStore store where this component provider belongs
     * @param backend       backend used to forward actions to the engine
     */
    public UserLocationCore(@NonNull ComponentStore<Facility> facilityStore, @NonNull Backend backend) {
        super(DESC, facilityStore);
        mBackend = backend;
    }

    @Nullable
    @Override
    public Location lastKnownLocation() {
        return mLocation;
    }

    @Override
    public boolean isAuthorized() {
        return mAuthorized;
    }

    @Override
    public void restartLocationUpdates() {
        mBackend.restartLocationUpdates();
    }

    @Override
    protected void onObserved() {
        mBackend.startMonitoringLocation();
    }

    @Override
    protected void onNoMoreObserved() {
        mBackend.stopMonitoringLocation();
    }

    /**
     * Updates current location.
     *
     * @param location new location
     *
     * @return {@code this}, to allow call chaining
     */
    @NonNull
    public UserLocationCore updateLocation(@NonNull Location location) {
        if (mLocation != location) {
            mLocation = location;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates device location authorization status.
     *
     * @param authorized boolean indicating if location service is authorized
     *
     * @return {@code this}, to allow call chaining
     */
    @NonNull
    public UserLocationCore updateAuthorization(boolean authorized) {
        if (mAuthorized != authorized) {
            mAuthorized = authorized;
            mChanged = true;
        }
        return this;
    }
}
