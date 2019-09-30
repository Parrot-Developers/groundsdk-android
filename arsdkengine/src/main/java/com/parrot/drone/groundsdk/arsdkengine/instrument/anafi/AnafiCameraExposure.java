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
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.instrument.DroneInstrumentController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.camera.IsoSensitivityAdapter;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.camera.ShutterSpeedAdapter;
import com.parrot.drone.groundsdk.internal.device.instrument.CameraExposureValuesCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/**
 * Camera exposure instrument controller for Anafi family drones.
 * <p>
 * The component will be published as soon as the device is connected if exposure values have been received during the
 * connection process. If it is not the case, it will be published as soon as the first exposure values are received
 * after the connection process.
 * <p>
 * The component is unpublished when the device is disconnected.
 */
public final class AnafiCameraExposure extends DroneInstrumentController {

    /** Camera exposure values instrument for which this object is the backend. */
    @NonNull
    private final CameraExposureValuesCore mCameraExposure;

    /**
     * Whether values have been received at least once during this connection session.
     * <p>
     * This is kept because the event used is non-ack so we are not sure that the event will be received before the end
     * of the connection process.
     */
    private boolean mHasReceivedValues;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiCameraExposure(@NonNull DroneController droneController) {
        super(droneController);
        mCameraExposure = new CameraExposureValuesCore(mComponentStore);
    }

    @Override
    public void onConnected() {
        if (mHasReceivedValues) {
            mCameraExposure.publish();
        }
    }

    @Override
    protected void onDisconnected() {
        mCameraExposure.unpublish();
        mHasReceivedValues = false;
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureCamera.UID) {
            ArsdkFeatureCamera.decode(command, mArsdkFeatureCameraCallback);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureCamera is decoded. */
    private final ArsdkFeatureCamera.Callback mArsdkFeatureCameraCallback = new ArsdkFeatureCamera.Callback() {

        @Override
        public void onExposure(int camId, @Nullable ArsdkFeatureCamera.ShutterSpeed shutterSpeed,
                               @Nullable ArsdkFeatureCamera.IsoSensitivity isoSensitivity,
                               @Nullable ArsdkFeatureCamera.State lock,
                               float lockRoiX, float lockRoiY, float lockRoiWidth, float lockRoiHeight) {
            if (camId != 0 || shutterSpeed == null || isoSensitivity == null) {
                return;
            }

            mCameraExposure.updateShutterSpeed(ShutterSpeedAdapter.from(shutterSpeed))
                           .updateIsoSensitivity(IsoSensitivityAdapter.from(isoSensitivity))
                           .notifyUpdated();

            if (!mHasReceivedValues) {
                mHasReceivedValues = true;
                // if it's the first time we receive values and we are connected, publish the component
                // (if we are not connected, the publication will be done in the onConnected callback)
                if (isConnected()) {
                    mCameraExposure.publish();
                }
            }
        }
    };
}
