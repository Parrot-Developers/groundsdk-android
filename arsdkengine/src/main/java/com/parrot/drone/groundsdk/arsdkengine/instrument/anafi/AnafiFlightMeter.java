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

package com.parrot.drone.groundsdk.arsdkengine.instrument.anafi;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.instrument.DroneInstrumentController;
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.internal.device.instrument.FlightMeterCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** Flight meter instrument controller for Anafi family drones. */
public class AnafiFlightMeter extends DroneInstrumentController {

    /** Key used to access device specific dictionary for this component's settings. */
    private static final String SETTINGS_KEY = "flightMeter";

    /** Last flight duration setting. */
    private static final StorageEntry<Integer> LAST_DURATION_SETTING = StorageEntry.ofInteger("lastDuration");

    /** Total flight duration setting. */
    private static final StorageEntry<Long> TOTAL_DURATION_SETTING = StorageEntry.ofLong("totalDuration");

    /** Total flight count setting. */
    private static final StorageEntry<Integer> TOTAL_COUNT_SETTING = StorageEntry.ofInteger("totalCount");

    /** The flight meter from which this object is the backend. */
    @NonNull
    private final FlightMeterCore mFlightMeter;

    /** Dictionary containing device specific values for this component. */
    @NonNull
    private final PersistentStore.Dictionary mDeviceDict;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this instrument controller.
     */
    public AnafiFlightMeter(@NonNull DroneController droneController) {
        super(droneController);
        mDeviceDict = mDeviceController.getDeviceDict().getDictionary(SETTINGS_KEY);
        mFlightMeter = new FlightMeterCore(mComponentStore);

        if (!mDeviceDict.isNew()) {
            loadPersistedData();
            mFlightMeter.publish();
        }
    }

    @Override
    protected void onConnected() {
        mFlightMeter.publish();
    }

    @Override
    protected void onDisconnected() {
        if (mDeviceDict.isNew()) {
            mFlightMeter.unpublish();
        }
    }

    @Override
    protected void onForgetting() {
        mDeviceDict.clear();
        mFlightMeter.unpublish();
    }

    @Override
    protected final void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureArdrone3.SettingsState.UID) {
            ArsdkFeatureArdrone3.SettingsState.decode(command, mArdrone3SettingsStateCallbacks);
        }
    }

    /**
     * Loads the flight meter values, from persistent storage.
     * <p>
     * This updates the flight meter instrument accordingly.
     * <p>
     * <strong>NOTE:</strong> Caller is still responsible to call {@code ComponentCore.notifyUpdated()} to publish
     * the change up to the user.
     */
    private void loadPersistedData() {
        Integer lastFlightDuration = LAST_DURATION_SETTING.load(mDeviceDict);
        if (lastFlightDuration != null) {
            mFlightMeter.updateLastFlightDuration(lastFlightDuration);
        }
        Long totalFlightDuration = TOTAL_DURATION_SETTING.load(mDeviceDict);
        if (totalFlightDuration != null) {
            mFlightMeter.updateTotalFlightDuration(totalFlightDuration);
        }
        Integer totalFlightCount = TOTAL_COUNT_SETTING.load(mDeviceDict);
        if (totalFlightCount != null) {
            mFlightMeter.updateTotalFlightCount(totalFlightCount);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.SettingsState is decoded. */
    private final ArsdkFeatureArdrone3.SettingsState.Callback mArdrone3SettingsStateCallbacks =
            new ArsdkFeatureArdrone3.SettingsState.Callback() {

                @Override
                public void onMotorFlightsStatusChanged(int nbFlights, int lastFlightDuration,
                                                        long totalFlightDuration) {
                    mFlightMeter.updateLastFlightDuration(lastFlightDuration)
                                .updateTotalFlightDuration(totalFlightDuration)
                                .updateTotalFlightCount(nbFlights)
                                .notifyUpdated();
                    LAST_DURATION_SETTING.save(mDeviceDict, lastFlightDuration);
                    TOTAL_DURATION_SETTING.save(mDeviceDict, totalFlightDuration);
                    TOTAL_COUNT_SETTING.save(mDeviceDict, nbFlights);
                }
            };
}
