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
import com.parrot.drone.groundsdk.arsdkengine.persistence.PersistentStore;
import com.parrot.drone.groundsdk.arsdkengine.persistence.StorageEntry;
import com.parrot.drone.groundsdk.device.peripheral.Copilot;
import com.parrot.drone.groundsdk.internal.device.peripheral.CopilotCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSkyctrl;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** Copilot peripheral controller for SkyController family remote controls. */
public class SkyControllerCopilot extends RCPeripheralController {

    /** Key used to access preset and range dictionaries for this peripheral controller. */
    private static final String SETTINGS_KEY = "copilot";

    // preset store bindings

    /** Piloting source preset entry. */
    private static final StorageEntry<Copilot.Source> SOURCE_PRESET =
            StorageEntry.ofEnum("source", Copilot.Source.class);

    /** The Copilot peripheral for which this object is the backend. */
    @NonNull
    private final CopilotCore mCopilot;

    /** Global dictionary containing device specific values for all components. */
    @Nullable
    private final PersistentStore.Dictionary mGlobalDeviceDict;

    /** Dictionary containing current preset values for this peripheral. */
    @Nullable
    private PersistentStore.Dictionary mPresetDict;

    /** Piloting source. */
    @Nullable
    private Copilot.Source mSource;

    /**
     * Constructor.
     *
     * @param rcController the remote control controller that owns this peripheral controller.
     */
    public SkyControllerCopilot(@NonNull RCController rcController) {
        super(rcController);
        mCopilot = new CopilotCore(mComponentStore, mBackend);
        mGlobalDeviceDict = offlineSettingsEnabled() ? mDeviceController.getDeviceDict() : null;
        mPresetDict = offlineSettingsEnabled() ? mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY) : null;
        loadPersistedData();
        if (isDevicePersisted()) {
            mCopilot.publish();
        }
    }

    @Override
    protected final void onConnected() {
        applyPresets();
        mCopilot.publish();
    }

    @Override
    protected void onDisconnected() {
        mCopilot.cancelSettingsRollbacks();

        if (isDevicePersisted()) {
            mCopilot.notifyUpdated();
        } else {
            mCopilot.unpublish();
        }
    }

    @Override
    protected void onPresetChange() {
        mPresetDict = mDeviceController.getPresetDict().getDictionary(SETTINGS_KEY);
        if (isConnected()) {
            applyPresets();
        }
        mCopilot.notifyUpdated();
    }

    @Override
    protected void onForgetting() {
        mCopilot.unpublish();
    }

    @Override
    protected void onCommandReceived(@NonNull ArsdkCommand command) {
        switch (command.getFeatureId()) {
            case ArsdkFeatureSkyctrl.CoPilotingState.UID:
                ArsdkFeatureSkyctrl.CoPilotingState.decode(command, mCopilotingStateCallback);
                break;
        }
    }

    /**
     * Tells whether device specific settings are persisted for this device.
     *
     * @return {@code true} if the device has persisted device settings, otherwise {@code false}
     */
    private boolean isDevicePersisted() {
        return mGlobalDeviceDict != null && !mGlobalDeviceDict.isNew();
    }

    /**
     * Loads presets and settings from persistent storage and updates the component accordingly.
     */
    private void loadPersistedData() {
        applyPresets();
    }

    /**
     * Applies component's persisted presets.
     */
    private void applyPresets() {
        applySource(SOURCE_PRESET.load(mPresetDict));
    }

    /**
     * Applies piloting source.
     * <ul>
     * <li>Gets the last received value if the given one is null;</li>
     * <li>Sends the obtained value to the drone in case it differs from the last received value;</li>
     * <li>Updates the component's setting accordingly.</li>
     * </ul>
     *
     * @param source piloting source to apply
     *
     * @return {@code true} if a command was sent to the device and the component's setting should arm its updating
     *         flag
     */
    private boolean applySource(@Nullable Copilot.Source source) {
        // Validating given value
        if (source == null) {
            source = mSource;
        }
        if (source == null) {
            return false;
        }

        boolean updating = source != mSource
                           && sendSource(source);

        mSource = source;
        mCopilot.source()
                .updateValue(source);

        return updating;
    }

    /**
     * Sends piloting source to the device.
     *
     * @param source source to set
     *
     * @return {@code true} if any command was sent to the device, otherwise false
     */
    private boolean sendSource(@NonNull Copilot.Source source) {
        ArsdkFeatureSkyctrl.CopilotingSetpilotingsourceSource arsdkSource = null;
        switch (source) {
            case REMOTE_CONTROL:
                arsdkSource = ArsdkFeatureSkyctrl.CopilotingSetpilotingsourceSource.SKYCONTROLLER;
                break;
            case APPLICATION:
                arsdkSource = ArsdkFeatureSkyctrl.CopilotingSetpilotingsourceSource.CONTROLLER;
                break;
        }
        return sendCommand(ArsdkFeatureSkyctrl.CoPiloting.encodeSetPilotingSource(arsdkSource));
    }

    /** Callbacks called when a command of the feature ArsdkFeatureSkyctrl.CoPilotingState is decoded. */
    private final ArsdkFeatureSkyctrl.CoPilotingState.Callback mCopilotingStateCallback =
            new ArsdkFeatureSkyctrl.CoPilotingState.Callback() {

                @Override
                public void onPilotingSource(@Nullable ArsdkFeatureSkyctrl.CopilotingstatePilotingsourceSource source) {
                    if (source == null) {
                        return;
                    }

                    switch (source) {
                        case SKYCONTROLLER:
                            mSource = Copilot.Source.REMOTE_CONTROL;
                            break;
                        case CONTROLLER:
                            mSource = Copilot.Source.APPLICATION;
                            break;
                    }

                    if (isConnected()) {
                        mCopilot.source().updateValue(mSource);
                        mCopilot.notifyUpdated();
                    }
                }
            };

    /** Backend of CopilotCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final CopilotCore.Backend mBackend = new CopilotCore.Backend() {

        @Override
        public boolean setSource(@NonNull Copilot.Source source) {
            boolean updating = applySource(source);
            SOURCE_PRESET.save(mPresetDict, source);
            if (!updating) {
                mCopilot.notifyUpdated();
            }
            return updating;
        }
    };
}
