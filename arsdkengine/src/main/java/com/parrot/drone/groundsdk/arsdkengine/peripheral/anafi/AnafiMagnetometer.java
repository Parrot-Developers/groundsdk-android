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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.device.peripheral.Magnetometer;
import com.parrot.drone.groundsdk.device.peripheral.MagnetometerWith3StepCalibration;
import com.parrot.drone.groundsdk.internal.device.peripheral.MagnetometerWith3StepCalibrationCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCommon;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.util.EnumSet;
import java.util.Set;

/** Magnetometer peripheral controller for Anafi family drones. */
public final class AnafiMagnetometer extends DronePeripheralController {

    /** The magnetometer peripheral from which this object is the backend. */
    @NonNull
    private final MagnetometerWith3StepCalibrationCore mMagnetometer;

    /**
     * {@code true} if the current calibration process failed.
     * <p>
     * The interface will be notified when the calibration process stops.
     */
    private boolean mCalibrationFailed;

    /**
     * Constructor.
     *
     * @param droneController The drone controller that owns this component controller.
     */
    public AnafiMagnetometer(@NonNull DroneController droneController) {
        super(droneController);
        mMagnetometer = new MagnetometerWith3StepCalibrationCore(mComponentStore, mBackend);
    }

    @Override
    public void onConnected() {
        mMagnetometer.publish();
    }

    @Override
    public void onDisconnected() {
        mMagnetometer.unpublish();
    }

    @Override
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureCommon.CalibrationState.UID) {
            ArsdkFeatureCommon.CalibrationState.decode(command, mCalibrationStateCallback);
        }
    }

    /**
     * Translates an integer into its groundsdk {@link Magnetometer.MagnetometerCalibrationState} representation.
     *
     * @param calibrationState represents if the calibration is required
     *
     * @return the corresponding Magnetometer.MagnetometerCalibrationState
     */
    @NonNull
    static Magnetometer.MagnetometerCalibrationState from(int calibrationState) {
        switch (calibrationState) {
            case 0:
                return Magnetometer.MagnetometerCalibrationState.CALIBRATED;
            case 1:
                return Magnetometer.MagnetometerCalibrationState.REQUIRED;
            case 2:
                return Magnetometer.MagnetometerCalibrationState.RECOMMENDED;
        }
        return Magnetometer.MagnetometerCalibrationState.REQUIRED;
    }


    /** Callbacks called when a command of the feature ArsdkFeatureCommon.CalibrationState is decoded. */
    private final ArsdkFeatureCommon.CalibrationState.Callback mCalibrationStateCallback =
            new ArsdkFeatureCommon.CalibrationState.Callback() {

                @Override
                public void onMagnetoCalibrationStateChanged(int xAxisCalibration, int yAxisCalibration,
                                                             int zAxisCalibration, int calibrationFailed) {
                    // the failed status is not immediately updated in the interface. We keep this value (which can
                    // change) and it will be updated in the interface when the calibration process stops. The failed
                    // state can only be considered when the device has indicated the end of the validation process
                    mCalibrationFailed = calibrationFailed == 1;
                    Set<MagnetometerWith3StepCalibration.CalibrationProcessState.Axis> calibratedAxes =
                            EnumSet.noneOf(MagnetometerWith3StepCalibration.CalibrationProcessState.Axis.class);
                    if (calibrationFailed == 0) {
                        if (xAxisCalibration == 1) {
                            calibratedAxes.add(MagnetometerWith3StepCalibration.CalibrationProcessState.Axis.ROLL);
                        }
                        if (yAxisCalibration == 1) {
                            calibratedAxes.add(MagnetometerWith3StepCalibration.CalibrationProcessState.Axis.PITCH);
                        }
                        if (zAxisCalibration == 1) {
                            calibratedAxes.add(MagnetometerWith3StepCalibration.CalibrationProcessState.Axis.YAW);
                        }
                    }
                    mMagnetometer.updateCalibProcessCalibratedAxes(calibratedAxes).notifyUpdated();
                }

                @Override
                public void onMagnetoCalibrationRequiredState(int required) {
                    mMagnetometer.updateCalibrationState(from(required)).notifyUpdated();
                }

                @Override
                public void onMagnetoCalibrationAxisToCalibrateChanged(
                        @Nullable
                                ArsdkFeatureCommon.CalibrationstateMagnetocalibrationaxistocalibratechangedAxis axis) {
                    if (axis != null) {
                        switch (axis) {
                            case XAXIS:
                                mMagnetometer.updateCalibProcessCurrentAxis(
                                        MagnetometerWith3StepCalibration.CalibrationProcessState.Axis.ROLL)
                                             .notifyUpdated();
                                break;
                            case YAXIS:
                                mMagnetometer.updateCalibProcessCurrentAxis(
                                        MagnetometerWith3StepCalibration.CalibrationProcessState.Axis.PITCH)
                                             .notifyUpdated();
                                break;
                            case ZAXIS:
                                mMagnetometer.updateCalibProcessCurrentAxis(
                                        MagnetometerWith3StepCalibration.CalibrationProcessState.Axis.YAW)
                                             .notifyUpdated();
                                break;
                            case NONE:
                                mMagnetometer.updateCalibProcessCurrentAxis(
                                        MagnetometerWith3StepCalibration.CalibrationProcessState.Axis.NONE)
                                             .notifyUpdated();
                                break;
                        }
                    }
                }

                @Override
                public void onMagnetoCalibrationStartedChanged(int started) {
                    if (started == 0) {
                        // calibration process stopped
                        mMagnetometer.updateCalibProcessFailed(mCalibrationFailed).notifyUpdated();
                        mMagnetometer.calibrationProcessStopped().notifyUpdated();
                    }
                    // reset the failure indicator when starting or stopping the calibration process
                    mCalibrationFailed = false;
                }
            };

    /** Backend of MagnetometerCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final MagnetometerWith3StepCalibrationCore.Backend mBackend =
            new MagnetometerWith3StepCalibrationCore.Backend() {

                @Override
                public void startCalibrationProcess() {
                    sendCommand(ArsdkFeatureCommon.Calibration.encodeMagnetoCalibration(1));
                }

                @Override
                public void cancelCalibrationProcess() {
                    sendCommand(ArsdkFeatureCommon.Calibration.encodeMagnetoCalibration(0));
                }
            };
}
