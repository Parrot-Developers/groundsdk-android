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

package com.parrot.drone.groundsdk.device.peripheral;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.value.DoubleSetting;
import com.parrot.drone.groundsdk.value.EnumSetting;

/**
 * Geofence peripheral interface.
 * <p>
 * Provides access to geofencing settings, which prevent the drone from flying over the given altitude and distance.
 * <p>
 * This peripheral can be obtained from a {@link Drone drone} using:
 * <pre>{@code drone.getPeripheral(Geofence.class)}</pre>
 *
 * @see Drone#getPeripheral(Class)
 * @see Drone#getPeripheral(Class, Ref.Observer)
 */
public interface Geofence extends Peripheral {

    /** Geofencing mode, indicating the zone type where the drone is able to fly. */
    enum Mode {

        /** The drone flying zone is only bounded by the maximum altitude setting. */
        ALTITUDE,

        /** The drone flying zone is bounded by the cylinder defined by the maximum altitude and distance settings. */
        CYLINDER
    }

    /**
     * Gives access to the maximum altitude setting.
     * <p>
     * This setting allows to define the maximum altitude relative to the takeoff altitude, in meters. <br>
     * The drone won't go higher than this maximum altitude.
     *
     * @return the maximum altitude setting
     */
    @NonNull
    DoubleSetting maxAltitude();

    /**
     * Gives access to the maximum distance setting.
     * <p>
     * This setting allows to define the maximum distance relative to the {@link #getCenter()} geofence center},
     * in meters. <br>
     * If current {@link #mode() geofencing mode} is {@link Mode#CYLINDER cylinder}, the drone won't fly over
     * the given distance, otherwise this setting is ignored.
     *
     * @return the maximum distance setting
     */
    @NonNull
    DoubleSetting maxDistance();

    /**
     * Gives access to the geofencing mode setting.
     * <p>
     * If this setting is set to {@link Mode#CYLINDER cylinder}, the drone won't fly over the {@link #maxDistance()
     * maximum distance}.
     *
     * @return the geofencing mode setting
     */
    @NonNull
    EnumSetting<Mode> mode();

    /**
     * Gets the geofence center location.
     * <p>
     * This location represents the center of the geofence zone. This can be either the controller position, or the home
     * location.
     *
     * @return the geofence center, or {@code null} if unknown presently
     */
    @Nullable
    Location getCenter();
}
