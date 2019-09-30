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

package com.parrot.drone.groundsdk.device.pilotingitf;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.value.BooleanSetting;

import java.io.File;
import java.util.Set;

/**
 * Flight Plan piloting interface for drones.
 * <p>
 * Allows to make the drone execute predefined flight plans. <br>
 * A flight plan is defined using a file in Mavlink format. For further information, please refer to
 * <a href="https://developer.parrot.com/docs/mavlink-flightplan">Parrot FlightPlan Mavlink documentation</a>.
 * <p>
 * This interface remains {@link State#UNAVAILABLE unavailable} until all {@link #getUnavailabilityReasons()
 * unavailability reasons} are cleared: <ul>
 * <li>A Flight Plan file has been {@link #uploadFlightPlan(File) uploaded} to the drone, and </li>
 * <li>the drone GPS location has been acquired, and </li>
 * <li>the drone is properly calibrated, and </li>
 * <li>the drone can take off (good battery level...). </li>
 * </ul>
 * Then, when all those conditions hold, the interface becomes {@link State#IDLE idle} and can be
 * {@link #activate(boolean) activated} to begin or resume Flight Plan execution, which can be paused by
 * {@link #deactivate() deactivating} the interface.
 * <p>
 * This piloting interface can be obtained from a {@code Drone} using:
 * <pre>{@code drone.getPilotingItf(FlightPlan.class)}</pre>
 *
 * @see PilotingItf.Provider#getPilotingItf(Class)
 * @see PilotingItf.Provider#getPilotingItf(Class, Ref.Observer)
 */
public interface FlightPlanPilotingItf extends PilotingItf, Activable {

    /**
     * Uploads a Flight Plan file to the drone.
     * <p>
     * If all other necessary conditions hold (GPS location acquired, drone properly calibrated), then the interface
     * becomes {@link State#IDLE idle} and the Flight Plan is ready to be {@link #activate(boolean) executed}.
     *
     * @param flightPlanFile file to upload
     *
     * @see <a href="https://developer.parrot.com/docs/mavlink-flightplan">Parrot FlightPlan Mavlink documentation</a>
     */
    void uploadFlightPlan(@NonNull File flightPlanFile);

    /**
     * Activates this piloting interface and starts executing the uploaded flight plan.
     * <p>
     * The interface should be {@link State#IDLE idle} for this method to have effect.<br>
     * If successful, the currently active piloting interface (if any) is deactivated and this one is activated.
     * <p>
     * The flight plan is resumed if it's currently {@link #isPaused() paused} and the {@code restart} parameter is
     * {@code false}; otherwise the flight plan is started from the beginning.
     *
     * @param restart {@code true} to force restarting the flight plan
     *
     * @return {@code true} on success, {@code false} in case the piloting interface cannot be activated at this point
     */
    boolean activate(boolean restart);

    /**
     * Reason why this piloting interface is currently unavailable.
     */
    enum UnavailabilityReason {

        /** Drone GPS accuracy is too weak. */
        DRONE_GPS_INFO_INACCURATE,

        /** Drone needs to be calibrated. */
        DRONE_NOT_CALIBRATED,

        /** Drone cannot take-off, for instance because it's in EMERGENCY state or battery is too low. */
        CANNOT_TAKE_OFF,

        /** No flight plan file uploaded. */
        MISSING_FLIGHT_PLAN_FILE,

        /** Drone camera is not available. */
        CAMERA_UNAVAILABLE
    }

    /**
     * Tells why this piloting interface may currently be unavailable.
     * <p>
     * The returned set may contain values only if the interface is {@link State#UNAVAILABLE unavailable}; it cannot
     * be modified.
     *
     * @return the set of reasons that restrain this piloting interface from being available at present
     */
    @NonNull
    Set<UnavailabilityReason> getUnavailabilityReasons();

    /**
     * Activation error.
     */
    enum ActivationError {

        /** No error. */
        NONE,

        /** Incorrect flight plan file. */
        INCORRECT_FLIGHT_PLAN_FILE,

        /** One or more waypoints are beyond the geofence. */
        WAYPOINT_BEYOND_GEOFENCE
    }

    /**
     * Gets the error raised during the latest activation.
     *
     * @return the latest activation error
     */
    @NonNull
    ActivationError getLatestActivationError();

    /**
     * Flight Plan file upload state.
     */
    enum UploadState {

        /** No flight plan file has been uploaded yet. */
        NONE,

        /** The flight plan file is currently uploading to the drone. */
        UPLOADING,

        /** The flight plan file has been successfully uploaded to the drone. */
        UPLOADED,

        /** The flight plan file upload has failed. */
        FAILED
    }

    /**
     * Gets the latest Flight Plan file upload state.
     *
     * @return the latest file upload state
     */
    @NonNull
    UploadState getLatestUploadState();

    /**
     * Tells whether the current flight plan on the drone is the latest one that has been uploaded from the application.
     *
     * @return {@code true} if the drone flight plan is the latest known one, otherwise {@code false}
     */
    boolean isFlightPlanFileKnown();

    /**
     * Tells whether the uploaded flight plan is currently paused.
     * <p>
     * A flight plan can be resumed if its execution has previously been paused, and not stopped for any reason (like
     * another automatic flying mode).
     *
     * @return {@code true} if flight plan is paused, otherwise {@code false}
     */
    boolean isPaused();

    /**
     * Gets the index of the latest mission item completed.
     *
     * @return the latest mission item executed index, or {@code -1} if no item has been executed yet
     */
    int getLatestMissionItemExecuted();

    /**
     * Setting for drone behavior upon disconnection during execution of a Flight Plan.
     */
    abstract class ReturnHomeOnDisconnectSetting extends BooleanSetting {

        /**
         * Tells whether the setting is mutable.
         *
         * @return {@code true} if the setting can be modified, otherwise {@code false}
         */
        public abstract boolean isMutable();
    }

    /**
     * Gets current setting for the drone behavior upon disconnection during execution of a Flight Plan.
     * <p>
     * When {@link ReturnHomeOnDisconnectSetting#isEnabled() enabled}, the drone returns to home upon disconnection
     * during execution of a Flight Plan. Otherwise, the drone will continue executing the Flight Plan even after
     * controller disconnection.
     * <p>
     * Depending on the drone configuration, this setting may be read-only and cannot be changed by the application.
     * This can be checked using {@link ReturnHomeOnDisconnectSetting#isMutable()} method.
     *
     * @return the return home on disconnect setting
     */
    @NonNull
    ReturnHomeOnDisconnectSetting getReturnHomeOnDisconnect();
}
