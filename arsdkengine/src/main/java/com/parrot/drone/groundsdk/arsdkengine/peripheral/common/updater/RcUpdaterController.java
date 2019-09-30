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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common.updater;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DeviceController;
import com.parrot.drone.groundsdk.device.peripheral.Updater;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;
import com.parrot.drone.groundsdk.internal.utility.FirmwareStore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSkyctrl;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/**
 * Remote-control-specific updater controller implementation.
 */
final class RcUpdaterController extends UpdaterController {

    /**
     * Constructor.
     *
     * @param deviceController   the device controller that owns this peripheral controller.
     * @param firmwareStore      firmware store providing remotely and locally available firmwares
     * @param firmwareDownloader firmware downloader allowing to download remote firmware to local storage
     * @param updater            firmware updater service, used to apply firmware updates to the device
     */
    RcUpdaterController(@NonNull DeviceController deviceController, @NonNull FirmwareStore firmwareStore,
                        @NonNull FirmwareDownloader firmwareDownloader,
                        @NonNull FirmwareUpdaterProtocol updater) {
        super(deviceController, firmwareStore, firmwareDownloader, updater);
    }

    @Override
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        super.onCommandReceived(command);
        if (command.getFeatureId() == ArsdkFeatureSkyctrl.SkyControllerState.UID) {
            ArsdkFeatureSkyctrl.SkyControllerState.decode(command, mSkyControllerStateCallbacks);
        }
    }

    /**
     * Callbacks called when a command of the feature ArsdkFeatureSkyctrl.SkyControllerState is decoded.
     */
    private final ArsdkFeatureSkyctrl.SkyControllerState.Callback mSkyControllerStateCallbacks
            = new ArsdkFeatureSkyctrl.SkyControllerState.Callback() {

        @Override
        public void onBatteryChanged(int percent) {
            onUnavailabilityReason(Updater.Update.UnavailabilityReason.NOT_ENOUGH_BATTERY, percent < 40);
            notifyComponentChange();
        }
    };
}
