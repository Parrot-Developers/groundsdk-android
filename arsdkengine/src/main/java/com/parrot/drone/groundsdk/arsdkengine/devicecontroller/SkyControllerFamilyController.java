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

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngine;
import com.parrot.drone.groundsdk.arsdkengine.Iso8601;
import com.parrot.drone.groundsdk.arsdkengine.instrument.skycontroller.SkyControllerBatteryInfo;
import com.parrot.drone.groundsdk.arsdkengine.instrument.skycontroller.SkyControllerCompass;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.PeripheralController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.crashml.FtpReportDownloader;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.flightlog.FtpFlightLogDownloader;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.updater.FirmwareUpdaterProtocol;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.common.updater.UpdaterController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.SkyControllerCopilot;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.SkyControllerDroneFinder;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.SkyControllerMagnetometer;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.SkyControllerSystemInfo;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.Sc3Gamepad;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller.gamepad.ScUaGamepad;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSkyctrl;

import java.util.Date;

/** RC controller for all remote controls of the SkyController family (starting from Skycontroller 3). */
public class SkyControllerFamilyController extends RCController {

    /**
     * Constructor.
     *
     * @param engine arsdk engine instance
     * @param uid    controlled device uid
     * @param model  controlled device model
     * @param name   controlled device initial name
     */
    public SkyControllerFamilyController(@NonNull ArsdkEngine engine, @NonNull String uid,
                                         @NonNull RemoteControl.Model model, @NonNull String name) {
        super(engine, uid, model, name);

        PeripheralController<RCController> gamepadCtrl = null;
        switch (model) {
            case SKY_CONTROLLER_3:
                gamepadCtrl = new Sc3Gamepad(this);
                break;
            case SKY_CONTROLLER_UA:
                gamepadCtrl = new ScUaGamepad(this);
                break;
        }

        registerComponentControllers(
                // instruments
                new SkyControllerBatteryInfo(this),
                new SkyControllerCompass(this),
                // peripherals
                new SkyControllerDroneFinder(this),
                gamepadCtrl,
                new SkyControllerSystemInfo(this),
                new SkyControllerMagnetometer(this),
                new SkyControllerCopilot(this),
                UpdaterController.create(this, FirmwareUpdaterProtocol.Ftp::new),
                FtpReportDownloader.create(this),
                FtpFlightLogDownloader.create(this)
        );
    }

    @Override
    void sendDate(@NonNull Date currentDate) {
        sendCommand(ArsdkFeatureSkyctrl.Common.encodeCurrentDateTime(Iso8601.toBaseDateAndTimeFormat(currentDate)));
    }
}
