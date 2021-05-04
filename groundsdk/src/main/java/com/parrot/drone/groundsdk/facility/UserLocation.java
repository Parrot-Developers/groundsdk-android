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

package com.parrot.drone.groundsdk.facility;

import android.Manifest;
import android.location.Location;

import androidx.annotation.Nullable;

/**
 * Facility that informs about the user (phone) location.
 */
public interface UserLocation extends Facility {

    /**
     * Gets the last known user location.
     * <p>
     * Note: the location altitude returned by {@link Location#getAltitude()} is the altitude above mean sea level.
     *
     * @return last known user location if available, otherwise {@code null}
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
     * Starts location updates if UserLocation is already observed and all authorizations are granted.
     * <p>
     * If the application observes this facility, but either system location is disabled or permission is not
     * granted yet, location monitoring is not started. The application should ask the user to enable location and
     * grant permission. System location changes are automatically detected, but not permission changes, so the
     * application should call this method once permission is granted so that location monitoring starts.
     * <p>
     * Calling this method while location monitoring is already started has no effect.
     */
    void restartLocationUpdates();
}
