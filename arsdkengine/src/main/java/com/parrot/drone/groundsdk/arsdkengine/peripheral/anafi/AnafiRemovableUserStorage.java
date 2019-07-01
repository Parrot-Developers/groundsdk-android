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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.peripheral.DronePeripheralController;
import com.parrot.drone.groundsdk.device.peripheral.RemovableUserStorage;
import com.parrot.drone.groundsdk.internal.device.peripheral.RemovableUserStorageCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureUserStorage;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.EnumSet;

import static com.parrot.drone.groundsdk.arsdkengine.Logging.TAG;

/** RemovableUserStorage peripheral controller for Anafi family drones. */
public class AnafiRemovableUserStorage extends DronePeripheralController {

    /** The removable user storage from which this object is the backend. */
    @NonNull
    private final RemovableUserStorageCore mRemovableUserStorage;

    /** {@code true} if formatting is allowed in state {@link RemovableUserStorage.State#READY}. */
    private boolean mFormatWhenReadyAllowed;

    /** {@code true} if formatting result event is supported by the drone. */
    private boolean mFormatResultEvtSupported;

    /** {@code true} if formatting progress event is supported. */
    private boolean mFormattingProgressSupported;

    /** {@code true} when a format request was sent and a formatting result event is expected. */
    private boolean mWaitingFormatResult;

    /** {@code true} if format command allowing selection of formatting type is supported. */
    private boolean mFormattingTypeSupported;

    /** Latest state received from device. */
    @Nullable
    private RemovableUserStorage.State mLatestState;

    /** State received during formatting, that will be notified after formatting result. */
    @Nullable
    private RemovableUserStorage.State mPendingState;

    /**
     * Constructor.
     *
     * @param droneController the drone controller that owns this peripheral controller.
     */
    public AnafiRemovableUserStorage(@NonNull DroneController droneController) {
        super(droneController);
        mRemovableUserStorage = new RemovableUserStorageCore(mComponentStore, mBackend);
    }

    @Override
    protected void onConnected() {
        mRemovableUserStorage.publish();
    }

    @Override
    protected void onDisconnected() {
        mRemovableUserStorage.unpublish();
        mWaitingFormatResult = false;
        mPendingState = null;
    }

    @Override
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        if (command.getFeatureId() == ArsdkFeatureUserStorage.UID) {
            ArsdkFeatureUserStorage.decode(command, mUserStorageCallback);
        }
    }

    /**
     * Updates the media state.
     *
     * @param state new media state
     */
    private void updateState(@NonNull RemovableUserStorage.State state) {
        mRemovableUserStorage.updateState(state)
                             .updateCanFormat(state == RemovableUserStorage.State.NEED_FORMAT
                                              || (mFormatWhenReadyAllowed && state == RemovableUserStorage.State.READY))
                             .notifyUpdated();
    }

    /** Callbacks called when a command of the feature ArsdkFeatureUserStorage is decoded. */
    private final ArsdkFeatureUserStorage.Callback mUserStorageCallback = new ArsdkFeatureUserStorage.Callback() {

        @Override
        public void onCapabilities(int supportedFeaturesBitField) {
            mFormatResultEvtSupported =
                    ArsdkFeatureUserStorage.Feature.FORMAT_RESULT_EVT_SUPPORTED.inBitField(supportedFeaturesBitField);
            mFormatWhenReadyAllowed =
                    ArsdkFeatureUserStorage.Feature.FORMAT_WHEN_READY_ALLOWED.inBitField(supportedFeaturesBitField);
            mFormattingProgressSupported =
                    ArsdkFeatureUserStorage.Feature.FORMAT_PROGRESS_EVT_SUPPORTED.inBitField(supportedFeaturesBitField);
            if (mLatestState != null) {
                updateState(mLatestState);
            }
        }

        @Override
        public void onSupportedFormattingTypes(int supportedTypesBitField) {
            mFormattingTypeSupported = true;
            mRemovableUserStorage.updateSupportedFormattingTypes(FormattingTypeAdapter.from(supportedTypesBitField))
                                 .notifyUpdated();
        }

        @Override
        public void onInfo(String name, long capacity) {
            if (name != null) {
                mRemovableUserStorage.updateMediaInfo(name, capacity).notifyUpdated();
            }
        }

        @Override
        public void onMonitor(long available_bytes) {
            mRemovableUserStorage.updateAvailableSpace(available_bytes).notifyUpdated();
        }

        @Override
        public void onState(@Nullable ArsdkFeatureUserStorage.PhyState physicalState,
                            @Nullable ArsdkFeatureUserStorage.FsState fileSystemState, int attributeBitField,
                            int monitorEnabled, int monitorPeriod) {
            RemovableUserStorage.State state = null;
            if (physicalState != null) switch (physicalState) {
                case UNDETECTED:
                    state = RemovableUserStorage.State.NO_MEDIA;
                    break;
                case TOO_SMALL:
                    state = RemovableUserStorage.State.MEDIA_TOO_SMALL;
                    break;
                case TOO_SLOW:
                    state = RemovableUserStorage.State.MEDIA_TOO_SLOW;
                    break;
                case USB_MASS_STORAGE:
                    state = RemovableUserStorage.State.USB_MASS_STORAGE;
                    break;
                case AVAILABLE:
                    if (fileSystemState != null) switch (fileSystemState) {
                        case UNKNOWN:
                            state = RemovableUserStorage.State.MOUNTING;
                            break;
                        case FORMAT_NEEDED:
                            state = RemovableUserStorage.State.NEED_FORMAT;
                            break;
                        case FORMATTING:
                            state = RemovableUserStorage.State.FORMATTING;
                            break;
                        case READY:
                            state = RemovableUserStorage.State.READY;
                            break;
                        case ERROR:
                            state = RemovableUserStorage.State.ERROR;
                            break;
                    }
                    break;
            }
            if (state == null) {
                // Ignore event
                ULog.w(TAG, "Unknown user storage state physicalState: " + physicalState
                            + " fileSystemState: " + fileSystemState);
            } else {
                if (!mFormatResultEvtSupported && mLatestState == RemovableUserStorage.State.FORMATTING) {
                    // format result when the drone does not support the format result event
                    if (state == RemovableUserStorage.State.READY) {
                        updateState(RemovableUserStorage.State.FORMATTING_SUCCEEDED);
                    } else if (state == RemovableUserStorage.State.NEED_FORMAT
                               || state == RemovableUserStorage.State.ERROR) {
                        updateState(RemovableUserStorage.State.FORMATTING_FAILED);
                    }
                }
                if (mWaitingFormatResult && state != RemovableUserStorage.State.FORMATTING) {
                    // new state will be notified after reception of formatting result
                    mPendingState = state;
                } else {
                    updateState(state);
                }
                mLatestState = state;
                // memory monitoring
                if ((state == RemovableUserStorage.State.READY)
                    && (monitorEnabled == 0)) {
                    // Start free memory monitoring when media is ready
                    sendCommand(ArsdkFeatureUserStorage.encodeStartMonitoring(0));
                } else if ((state != RemovableUserStorage.State.READY)
                           && (monitorEnabled == 1)) {
                    // Stop free memory monitoring when media is not ready
                    sendCommand(ArsdkFeatureUserStorage.encodeStopMonitoring());
                }
            }
        }

        @Override
        public void onFormatResult(@Nullable ArsdkFeatureUserStorage.FormattingResult result) {
            if (result == null) {
                return;
            }
            switch (result) {
                case ERROR:
                    updateState(RemovableUserStorage.State.FORMATTING_FAILED);
                    break;
                case DENIED:
                    updateState(RemovableUserStorage.State.FORMATTING_DENIED);
                    if (mLatestState != null) {
                        // since in that case the device will not send another state,
                        // restore latest state received before formatting
                        updateState(mLatestState);
                    }
                    break;
                case SUCCESS:
                    updateState(RemovableUserStorage.State.FORMATTING_SUCCEEDED);
                    break;
            }
            if (mPendingState != null) {
                // notify pending state received before formatting result
                updateState(mPendingState);
                mPendingState = null;
            }
            mWaitingFormatResult = false;
        }

        @Override
        public void onFormatProgress(@Nullable ArsdkFeatureUserStorage.FormattingStep step, int percentage) {
            if (step != null) switch (step) {
                case PARTITIONING:
                    mRemovableUserStorage.updateFormattingStep(RemovableUserStorage.FormattingState.Step.PARTITIONING);
                    break;
                case CLEARING_DATA:
                    mRemovableUserStorage.updateFormattingStep(RemovableUserStorage.FormattingState.Step.CLEARING_DATA);
                    break;
                case CREATING_FS:
                    mRemovableUserStorage.updateFormattingStep(
                            RemovableUserStorage.FormattingState.Step.CREATING_FILE_SYSTEM);
                    break;
            }
            mRemovableUserStorage.updateFormattingProgress(percentage).notifyUpdated();
        }
    };

    /** Backend of RemovableUserStorageCore implementation. */
    @SuppressWarnings("FieldCanBeLocal")
    private final RemovableUserStorageCore.Backend mBackend = new RemovableUserStorageCore.Backend() {

        @Override
        public boolean format(@NonNull RemovableUserStorage.FormattingType type, @NonNull String name) {
            boolean sent;
            if (mFormattingTypeSupported) {
                sent = sendCommand(
                        ArsdkFeatureUserStorage.encodeFormatWithType(name, FormattingTypeAdapter.from(type)));
            } else {
                sent = sendCommand(ArsdkFeatureUserStorage.encodeFormat(name));
            }

            if (sent) {
                if (mFormattingProgressSupported) {
                    mRemovableUserStorage.initFormattingState();
                }
                if (mFormatResultEvtSupported) {
                    mWaitingFormatResult = true;
                    updateState(RemovableUserStorage.State.FORMATTING);
                }
                mRemovableUserStorage.notifyUpdated();
            }
            return sent;
        }
    };

    /**
     * Utility class to adapt {@link ArsdkFeatureUserStorage.FormattingType user storage feature} to {@link
     * RemovableUserStorage.FormattingType groundsdk} formatting types.
     */
    private static final class FormattingTypeAdapter {

        /**
         * Converts a {@code ArsdkFeatureUserStorage.FormattingType} to its {@code RemovableUserStorage.FormattingType}
         * equivalent.
         *
         * @param type user storage feature formatting type to convert
         *
         * @return the groundsdk formatting type equivalent
         */
        @NonNull
        static RemovableUserStorage.FormattingType from(@NonNull ArsdkFeatureUserStorage.FormattingType type) {
            switch (type) {
                case FULL:
                    return RemovableUserStorage.FormattingType.FULL;
                case QUICK:
                    return RemovableUserStorage.FormattingType.QUICK;
            }
            return null;
        }

        /**
         * Converts a {@code RemovableUserStorage.FormattingType} to its {@code ArsdkFeatureUserStorage.FormattingType}
         * equivalent.
         *
         * @param type groundsdk formatting type to convert
         *
         * @return user storage feature formatting type equivalent of the given value
         */
        @NonNull
        static ArsdkFeatureUserStorage.FormattingType from(@NonNull RemovableUserStorage.FormattingType type) {
            switch (type) {
                case FULL:
                    return ArsdkFeatureUserStorage.FormattingType.FULL;
                case QUICK:
                    return ArsdkFeatureUserStorage.FormattingType.QUICK;
            }
            return null;
        }

        /**
         * Converts a bitfield representation of multiple {@code ArsdkFeatureUserStorage.FormattingType} to its
         * equivalent set of {@code RemovableUserStorage.FormattingType}.
         *
         * @param bitfield bitfield representation of user storage feature formatting types to convert
         *
         * @return the equivalent set of groundsdk formatting types
         */
        @NonNull
        static EnumSet<RemovableUserStorage.FormattingType> from(int bitfield) {
            EnumSet<RemovableUserStorage.FormattingType> types =
                    EnumSet.noneOf(RemovableUserStorage.FormattingType.class);
            ArsdkFeatureUserStorage.FormattingType.each(bitfield, arsdk -> types.add(from(arsdk)));
            return types;
        }
    }
}
