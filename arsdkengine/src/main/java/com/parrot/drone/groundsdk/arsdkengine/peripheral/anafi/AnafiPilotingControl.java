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
import com.parrot.drone.groundsdk.device.peripheral.PilotingControl;
import com.parrot.drone.groundsdk.internal.device.peripheral.PilotingControlCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeaturePilotingStyle;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

import java.util.EnumSet;

/** PilotingControl peripheral controller for Anafi family drones. */
public class AnafiPilotingControl extends DronePeripheralController {

    /** The Copilot peripheral for which this object is the backend. */
    @NonNull
    private final PilotingControlCore mPilotingControl;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiPilotingControl(@NonNull DroneController droneController) {
        super(droneController);
        mPilotingControl = new PilotingControlCore(mComponentStore, mBackend);
    }

    @Override
    protected final void onConnecting() {
        mPilotingControl.behavior()
                        .updateAvailableValues(EnumSet.of(PilotingControl.Behavior.STANDARD))
                        .updateValue(PilotingControl.Behavior.STANDARD);
    }

    @Override
    protected final void onConnected() {
        mPilotingControl.publish();
    }

    @Override
    protected void onDisconnected() {
        mPilotingControl.cancelSettingsRollbacks();
        mPilotingControl.unpublish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeaturePilotingStyle.UID) {
            ArsdkFeaturePilotingStyle.decode(command, mPilotingStyleCallback);
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeaturePilotingStyle is decoded. */
    private final ArsdkFeaturePilotingStyle.Callback mPilotingStyleCallback =
            new ArsdkFeaturePilotingStyle.Callback() {

                @Override
                public void onCapabilities(int stylesBitField) {
                    EnumSet<PilotingControl.Behavior> supportedModes = BehaviorAdapter.from(stylesBitField);

                    mPilotingControl.behavior().updateAvailableValues(supportedModes);
                    mPilotingControl.notifyUpdated();
                }

                @Override
                public void onStyle(@Nullable ArsdkFeaturePilotingStyle.Style style) {
                    if (style == null) {
                        return;
                    }
                    mPilotingControl.behavior().updateValue(BehaviorAdapter.from(style));
                    if (isConnected()) {
                        mPilotingControl.notifyUpdated();
                    }
                }
            };

    /** Backend of PilotingControlCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final PilotingControlCore.Backend mBackend = behavior -> {
        ArsdkFeaturePilotingStyle.Style style = BehaviorAdapter.from(behavior);
        return sendCommand(ArsdkFeaturePilotingStyle.encodeSetStyle(style));
    };

    /**
     * Utility class to adapt {@link ArsdkFeaturePilotingStyle.Style piloting style feature} to
     * {@link PilotingControl.Behavior groundsdk} piloting behaviors.
     */
    private static final class BehaviorAdapter {

        /**
         * Converts an {@code ArsdkFeaturePilotingStyle.Style} to its {@code PilotingControl.Behavior}
         * equivalent.
         *
         * @param style piloting style feature style to convert
         *
         * @return the groundsdk piloting behavior equivalent
         */
        @NonNull
        static PilotingControl.Behavior from(@NonNull ArsdkFeaturePilotingStyle.Style style) {
            switch (style) {
                case STANDARD:
                    return PilotingControl.Behavior.STANDARD;
                case CAMERA_OPERATED:
                    return PilotingControl.Behavior.CAMERA_OPERATED;
            }
            return null;
        }

        /**
         * Converts a {@code PilotingControl.Behavior} to its {@code ArsdkFeaturePilotingStyle.Style}
         * equivalent.
         *
         * @param behavior groundsdk piloting behavior to convert
         *
         * @return piloting style feature style equivalent of the given value
         */
        @NonNull
        static ArsdkFeaturePilotingStyle.Style from(@NonNull PilotingControl.Behavior behavior) {
            switch (behavior) {
                case STANDARD:
                    return ArsdkFeaturePilotingStyle.Style.STANDARD;
                case CAMERA_OPERATED:
                    return ArsdkFeaturePilotingStyle.Style.CAMERA_OPERATED;
            }
            return null;
        }

        /**
         * Converts a bitfield representation of multiple {@code ArsdkFeaturePilotingStyle.Style} to
         * its equivalent set of {@code PilotingControl.Behavior}.
         *
         * @param bitfield bitfield representation of piloting style feature styles to convert
         *
         * @return the equivalent set of groundsdk piloting behaviors
         */
        @NonNull
        static EnumSet<PilotingControl.Behavior> from(int bitfield) {
            EnumSet<PilotingControl.Behavior> behaviors = EnumSet.noneOf(PilotingControl.Behavior.class);
            ArsdkFeaturePilotingStyle.Style.each(bitfield, arsdk -> behaviors.add(from(arsdk)));
            return behaviors;
        }
    }
}
