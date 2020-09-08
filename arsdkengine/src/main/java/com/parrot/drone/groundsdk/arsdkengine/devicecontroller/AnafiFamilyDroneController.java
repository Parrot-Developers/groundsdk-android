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

package com.parrot.drone.groundsdk.arsdkengine.devicecontroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngine;
import com.parrot.drone.groundsdk.arsdkengine.Iso8601;
import com.parrot.drone.groundsdk.arsdkengine.ephemeris.EphemerisUploadProtocol;
import com.parrot.drone.groundsdk.arsdkengine.instrument.anafi.AnafiAlarms;
import com.parrot.drone.groundsdk.arsdkengine.instrument.anafi.AnafiAltimeter;
import com.parrot.drone.groundsdk.arsdkengine.instrument.anafi.AnafiAttitudeIndicator;
import com.parrot.drone.groundsdk.arsdkengine.instrument.anafi.AnafiBatteryInfo;
import com.parrot.drone.groundsdk.arsdkengine.instrument.anafi.AnafiCameraExposure;
import com.parrot.drone.groundsdk.arsdkengine.instrument.anafi.AnafiCompass;
import com.parrot.drone.groundsdk.arsdkengine.instrument.anafi.AnafiFlightMeter;
import com.parrot.drone.groundsdk.arsdkengine.instrument.anafi.AnafiFlyingIndicators;
import com.parrot.drone.groundsdk.arsdkengine.instrument.anafi.AnafiGps;
import com.parrot.drone.groundsdk.arsdkengine.instrument.anafi.AnafiPhotoProgressIndicator;
import com.parrot.drone.groundsdk.arsdkengine.instrument.anafi.AnafiRadio;
import com.parrot.drone.groundsdk.arsdkengine.instrument.anafi.AnafiSpeedometer;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiBatteryGaugeUpdater;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiBeeper;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiCertificateUploader;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiDri;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiGeofence;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiLeds;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiMagnetometer;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiMotors;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiPilotingControl;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiPreciseHome;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiRemovableUserStorage;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiLogControl;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiStreamServer;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiSystemInfo;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.AnafiTargetTracker;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.camera.AnafiAntiFlicker;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.camera.AnafiCameraRouter;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.flightdata.AnafiFlightDataDownloader;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.gimbal.AnafiGimbal;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.media.AnafiMediaStore;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.thermalcontrol.AnafiThermalControl;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.wifi.AnafiWifiAccessPoint;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.DebugDevToolbox;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.crashml.HttpReportDownloader;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.flightlog.HttpFlightLogDownloader;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.gutmalog.GutmaLogProducer;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.updater.FirmwareUpdaterProtocol;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.updater.UpdaterController;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.PilotingCommand;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi.AnafiAnimationPilotingItf;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi.AnafiFlightPlanPilotingItf;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi.AnafiFollowMePilotingItf;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi.AnafiGuidedPilotingItf;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi.AnafiLookAtPilotingItf;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi.AnafiManualPilotingItf;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi.AnafiPointOfInterestPilotingItf;
import com.parrot.drone.groundsdk.arsdkengine.pilotingitf.anafi.AnafiReturnHomePilotingItf;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCommon;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.util.Date;

/** Drone controller for all drones of the Anafi family. */
public class AnafiFamilyDroneController extends DroneController {

    /**
     * Constructor.
     *
     * @param engine arsdk engine instance
     * @param uid    controlled device uid
     * @param model  controlled device model
     * @param name   controlled device initial name
     */
    public AnafiFamilyDroneController(@NonNull ArsdkEngine engine, @NonNull String uid, @NonNull Drone.Model model,
                                      @NonNull String name) {
        /* Manual piloting interface, also the default interface when no other interface is active. */
        super(engine, uid, model, name, new PilotingCommand.Encoder.Anafi(),
                AnafiManualPilotingItf::new, EphemerisUploadProtocol::httpUpload);

        registerComponentControllers(
                // always active piloting interfaces
                new AnafiAnimationPilotingItf(this),
                // non-default piloting interfaces
                new AnafiReturnHomePilotingItf(mActivationController),
                new AnafiFlightPlanPilotingItf(mActivationController),
                new AnafiLookAtPilotingItf(mActivationController),
                new AnafiFollowMePilotingItf(mActivationController),
                new AnafiPointOfInterestPilotingItf(mActivationController),
                new AnafiGuidedPilotingItf(mActivationController),
                // instruments
                new AnafiAlarms(this),
                new AnafiAltimeter(this),
                new AnafiAttitudeIndicator(this),
                new AnafiCompass(this),
                new AnafiFlyingIndicators(this),
                new AnafiGps(this),
                new AnafiSpeedometer(this),
                new AnafiRadio(this),
                new AnafiBatteryInfo(this),
                new AnafiFlightMeter(this),
                new AnafiCameraExposure(this),
                new AnafiPhotoProgressIndicator(this),
                // peripherals
                new AnafiMagnetometer(this),
                new AnafiSystemInfo(this),
                new AnafiBeeper(this),
                new AnafiMotors(this),
                new AnafiGeofence(this),
                new AnafiMediaStore(this),
                new AnafiRemovableUserStorage(this),
                GroundSdkConfig.get().isDevToolboxEnabled() ? new DebugDevToolbox(this) : null,
                UpdaterController.create(this, FirmwareUpdaterProtocol.Http::new),
                HttpReportDownloader.create(this),
                AnafiFlightDataDownloader.create(this),
                HttpFlightLogDownloader.create(this, GutmaLogProducer.create(this)),
                new AnafiWifiAccessPoint(this),
                new AnafiCameraRouter(this),
                new AnafiAntiFlicker(this),
                new AnafiGimbal(this),
                new AnafiTargetTracker(this),
                new AnafiPreciseHome(this),
                new AnafiThermalControl(this),
                new AnafiStreamServer(this),
                new AnafiLeds(this),
                new AnafiPilotingControl(this),
                new AnafiBatteryGaugeUpdater(this),
                new AnafiDri(this),
                new AnafiLogControl(this),
                new AnafiCertificateUploader(this)
        );
    }

    @Override
    void sendDate(@NonNull Date currentDate) {
        sendCommand(ArsdkFeatureCommon.Common.encodeCurrentDateTime(Iso8601.toBaseDateAndTimeFormat(currentDate)));
    }

    @Override
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        super.onCommandReceived(command);
        if (command.getFeatureId() == ArsdkFeatureArdrone3.PilotingState.UID) {
            ArsdkFeatureArdrone3.PilotingState.decode(command, mPilotingStateCallback);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.PilotingState is decoded. */
    private final ArsdkFeatureArdrone3.PilotingState.Callback mPilotingStateCallback =
            new ArsdkFeatureArdrone3.PilotingState.Callback() {

                @Override
                public void onFlyingStateChanged(
                        @Nullable ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState state) {
                    updateLandedState(state == ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDED ||
                                      state == ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.EMERGENCY);
                }
            };
}
