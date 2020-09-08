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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.RCController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.RCPeripheralController;
import com.parrot.drone.groundsdk.device.peripheral.Magnetometer;
import com.parrot.drone.groundsdk.internal.device.peripheral.MagnetometerWith1StepCalibrationCore;
import com.parrot.drone.groundsdk.internal.value.IntegerRangeCore;
import com.parrot.drone.groundsdk.value.IntegerRange;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSkyctrl;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSkyctrl.CalibrationstateMagnetocalibrationstateStatus;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** Magnetometer peripheral controller for SkyController family remote controls. */
public class SkyControllerMagnetometer extends RCPeripheralController {

    /** The magnetometer peripheral from which this object is the backend. */
    @NonNull
    private final MagnetometerWith1StepCalibrationCore mMagnetometer;

    /**
     * Constructor.
     *
     * @param rcController The device controller that owns this component controller.
     */
    public SkyControllerMagnetometer(@NonNull RCController rcController) {
        super(rcController);
        mMagnetometer = new MagnetometerWith1StepCalibrationCore(mComponentStore, mBackend);
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
        if (command.getFeatureId() == ArsdkFeatureSkyctrl.CalibrationState.UID) {
            ArsdkFeatureSkyctrl.CalibrationState.decode(command, mCalibrationStateCallback);
        }
    }

    /** Axis calibration quality range supported by remote controller. */
    private static final IntegerRange ARSDK_CALIBRATION_QUALITY_RANGE = IntegerRange.of(0, 255);

    /** Callbacks called when a command of the feature ArsdkFeatureSkyctrl.CalibrationState is decoded. */
    private final ArsdkFeatureSkyctrl.CalibrationState.Callback mCalibrationStateCallback =
            new ArsdkFeatureSkyctrl.CalibrationState.Callback() {

                @Override
                public void onMagnetoCalibrationState(@Nullable CalibrationstateMagnetocalibrationstateStatus status,
                                                      int xQuality, int yQuality, int zQuality) {
                    xQuality = IntegerRangeCore.PERCENTAGE.scaleFrom(xQuality, ARSDK_CALIBRATION_QUALITY_RANGE);
                    yQuality = IntegerRangeCore.PERCENTAGE.scaleFrom(yQuality, ARSDK_CALIBRATION_QUALITY_RANGE);
                    zQuality = IntegerRangeCore.PERCENTAGE.scaleFrom(zQuality, ARSDK_CALIBRATION_QUALITY_RANGE);
                    mMagnetometer.updateCalibrationProgress(xQuality, yQuality, zQuality).notifyUpdated();
                    if (status != null) {
                        Magnetometer.MagnetometerCalibrationState calibrationState =
                                status == CalibrationstateMagnetocalibrationstateStatus.CALIBRATED
                                        ? Magnetometer.MagnetometerCalibrationState.CALIBRATED
                                        : Magnetometer.MagnetometerCalibrationState.REQUIRED;
                        mMagnetometer.updateCalibrationState(calibrationState).notifyUpdated();
                    }
                }
            };

    /** Backend of MagnetometerWith1StepCalibrationCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final MagnetometerWith1StepCalibrationCore.Backend mBackend =
            new MagnetometerWith1StepCalibrationCore.Backend() {

                @Override
                public void startCalibrationProcess() {
                    sendCommand(ArsdkFeatureSkyctrl.Calibration.encodeEnableMagnetoCalibrationQualityUpdates(1));
                }

                @Override
                public void cancelCalibrationProcess() {
                    sendCommand(ArsdkFeatureSkyctrl.Calibration.encodeEnableMagnetoCalibrationQualityUpdates(0));
                }
            };
}
