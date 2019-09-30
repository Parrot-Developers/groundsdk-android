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
import com.parrot.drone.groundsdk.internal.device.instrument.PhotoProgressIndicatorCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** Photo progress indicator instrument controller for Anafi family drones. */
public class AnafiPhotoProgressIndicator extends DroneInstrumentController {

    /** PhotoProgressIndicator instrument for which this object is the backend. */
    @NonNull
    private final PhotoProgressIndicatorCore mPhotoProgressIndicator;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this instrument controller.
     */
    public AnafiPhotoProgressIndicator(@NonNull DroneController droneController) {
        super(droneController);
        mPhotoProgressIndicator = new PhotoProgressIndicatorCore(mComponentStore);
    }

    @Override
    public void onConnected() {
        mPhotoProgressIndicator.publish();
    }

    @Override
    public void onDisconnected() {
        mPhotoProgressIndicator.resetRemainingTime()
                               .resetRemainingDistance()
                               .unpublish();
    }

    @Override
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureCamera.UID) {
            ArsdkFeatureCamera.decode(command, mCameraCallbacks);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureCamera is decoded. */
    private final ArsdkFeatureCamera.Callback mCameraCallbacks = new ArsdkFeatureCamera.Callback() {

        @Override
        public void onPhotoState(int camId, @Nullable ArsdkFeatureCamera.Availability available,
                                 @Nullable ArsdkFeatureCamera.State state) {
            if (available == ArsdkFeatureCamera.Availability.NOT_AVAILABLE
                || state == ArsdkFeatureCamera.State.INACTIVE) {
                mPhotoProgressIndicator.resetRemainingTime()
                                       .resetRemainingDistance()
                                       .notifyUpdated();
            }
        }

        @Override
        public void onNextPhotoDelay(@Nullable ArsdkFeatureCamera.PhotoMode mode, float remaining) {
            if (mode == null) {
                throw new ArsdkCommand.RejectedEventException("Invalid photo mode");
            }

            switch (mode) {
                case SINGLE:
                case BRACKETING:
                case BURST:
                    break;
                case TIME_LAPSE:
                    mPhotoProgressIndicator.updateRemainingTime(remaining).notifyUpdated();
                    break;
                case GPS_LAPSE:
                    mPhotoProgressIndicator.updateRemainingDistance(remaining).notifyUpdated();
                    break;
            }
        }
    };
}
