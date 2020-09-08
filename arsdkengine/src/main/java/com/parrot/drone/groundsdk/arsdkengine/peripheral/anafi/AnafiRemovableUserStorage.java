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

    /** {@code true} if formatting is allowed in state {@link RemovableUserStorage.FileSystemState#READY}. */
    private boolean mFormatWhenReadyAllowed;

    /** {@code true} if formatting result event is supported by the drone. */
    private boolean mFormatResultEvtSupported;

    /** {@code true} if formatting progress event is supported. */
    private boolean mFormattingProgressSupported;

    /** {@code true} if removable storage encryption is supported. */
    private boolean mEncryptionSupported;

    /** {@code true} when a format request was sent and a formatting result event is expected. */
    private boolean mWaitingFormatResult;

    /** {@code true} if format command allowing selection of formatting type is supported. */
    private boolean mFormattingTypeSupported;

    /** Latest state received from device. */
    @Nullable
    private RemovableUserStorage.FileSystemState mLatestState;

    /** State received during formatting, that will be notified after formatting result. */
    @Nullable
    private RemovableUserStorage.FileSystemState mPendingState;

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
     * Updates the media file system state.
     *
     * @param state new media file system state
     */
    private void updateFileSystemState(@NonNull RemovableUserStorage.FileSystemState state) {
        mRemovableUserStorage.updateFileSystemState(state)
                             .updateCanFormat(state == RemovableUserStorage.FileSystemState.NEED_FORMAT
                                              || (mFormatWhenReadyAllowed
                                                  && (state == RemovableUserStorage.FileSystemState.READY
                                                      || state == RemovableUserStorage.FileSystemState.PASSWORD_NEEDED)));
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
            mEncryptionSupported =
                    ArsdkFeatureUserStorage.Feature.ENCRYPTION_SUPPORTED.inBitField(supportedFeaturesBitField);
            mRemovableUserStorage.updateIsEncryptionSupported(mEncryptionSupported);
            if (mLatestState != null) {
                updateFileSystemState(mLatestState);
            }
            mRemovableUserStorage.notifyUpdated();
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
            if (physicalState != null) {
                mRemovableUserStorage.updatePhysicalState(convert(physicalState));
            }
            RemovableUserStorage.FileSystemState fsState = null;
            if (fileSystemState != null) {
                fsState = convert(fileSystemState);
            }

            boolean encrypted = ArsdkFeatureUserStorage.Attribute.fromBitfield(attributeBitField)
                                                                 .contains(ArsdkFeatureUserStorage.Attribute.ENCRYPTED);
            mRemovableUserStorage.updateIsEncrypted(encrypted);

            if (fsState == null) {
                // Ignore event
                ULog.w(TAG, "Unknown user storage state fileSystemState: " + fileSystemState);
            } else {
                if (!mFormatResultEvtSupported && mLatestState == RemovableUserStorage.FileSystemState.FORMATTING) {
                    // format result when the drone does not support the format result event
                    if (fsState == RemovableUserStorage.FileSystemState.READY) {
                        updateFileSystemState(RemovableUserStorage.FileSystemState.FORMATTING_SUCCEEDED);
                        mRemovableUserStorage.notifyUpdated();
                    } else if (fsState == RemovableUserStorage.FileSystemState.NEED_FORMAT
                               || fsState == RemovableUserStorage.FileSystemState.ERROR) {
                        updateFileSystemState(RemovableUserStorage.FileSystemState.FORMATTING_FAILED);
                        mRemovableUserStorage.notifyUpdated();
                    }
                }
                if (mWaitingFormatResult && fsState != RemovableUserStorage.FileSystemState.FORMATTING) {
                    // new state will be notified after reception of formatting result
                    mPendingState = fsState;
                } else {
                    updateFileSystemState(fsState);
                }
                mLatestState = fsState;
                // memory monitoring
                if ((fsState == RemovableUserStorage.FileSystemState.READY)
                    && (monitorEnabled == 0)) {
                    // Start free memory monitoring when media is ready
                    sendCommand(ArsdkFeatureUserStorage.encodeStartMonitoring(0));
                } else if ((fsState != RemovableUserStorage.FileSystemState.READY)
                           && (monitorEnabled == 1)) {
                    // Stop free memory monitoring when media is not ready
                    sendCommand(ArsdkFeatureUserStorage.encodeStopMonitoring());
                }
            }
            mRemovableUserStorage.notifyUpdated();
        }

        @Override
        public void onSdcardUuid(@Nullable String uuid) {
            if (uuid == null) {
                return;
            }
            mRemovableUserStorage.updateUuid(uuid).notifyUpdated();
        }

        @Override
        public void onFormatResult(@Nullable ArsdkFeatureUserStorage.FormattingResult result) {
            if (result == null) {
                return;
            }
            switch (result) {
                case ERROR:
                    updateFileSystemState(RemovableUserStorage.FileSystemState.FORMATTING_FAILED);
                    mRemovableUserStorage.notifyUpdated();
                    break;
                case DENIED:
                    updateFileSystemState(RemovableUserStorage.FileSystemState.FORMATTING_DENIED);
                    mRemovableUserStorage.notifyUpdated();
                    if (mLatestState != null) {
                        // since in that case the device will not send another state,
                        // restore latest state received before formatting
                        updateFileSystemState(mLatestState);
                        mRemovableUserStorage.notifyUpdated();
                    }
                    break;
                case SUCCESS:
                    updateFileSystemState(RemovableUserStorage.FileSystemState.FORMATTING_SUCCEEDED);
                    mRemovableUserStorage.notifyUpdated();
                    break;
            }
            if (mPendingState != null) {
                // notify pending state received before formatting result
                updateFileSystemState(mPendingState);
                mRemovableUserStorage.notifyUpdated();
                mPendingState = null;
            }
            mWaitingFormatResult = false;
        }

        @Override
        public void onDecryption(@Nullable ArsdkFeatureUserStorage.PasswordResult result) {
            if (result == null) {
                return;
            }
            switch (result) {
                case WRONG_USAGE:
                    updateFileSystemState(RemovableUserStorage.FileSystemState.DECRYPTION_WRONG_USAGE);
                    mRemovableUserStorage.notifyUpdated();
                    if (mLatestState != null) {
                        // since in that case the device will not send another state,
                        // restore latest state received before decryption
                        updateFileSystemState(mLatestState);
                        mRemovableUserStorage.notifyUpdated();
                    }
                    break;
                case WRONG_PASSWORD:
                    updateFileSystemState(RemovableUserStorage.FileSystemState.DECRYPTION_WRONG_PASSWORD);
                    mRemovableUserStorage.notifyUpdated();
                    if (mLatestState != null) {
                        // since in that case the device will not send another state,
                        // restore latest state received before decryption
                        updateFileSystemState(mLatestState);
                        mRemovableUserStorage.notifyUpdated();
                    }
                    break;
                case SUCCESS:
                    updateFileSystemState(RemovableUserStorage.FileSystemState.DECRYPTION_SUCCEEDED);
                    mRemovableUserStorage.notifyUpdated();
                    break;
            }
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
                formattingInitiated();
            }
            return sent;
        }

        @Override
        public boolean formatWithEncryption(@NonNull String password, @NonNull RemovableUserStorage.FormattingType type,
                                            @NonNull String name) {
            boolean sent = false;
            if (mEncryptionSupported) {
                sent = sendCommand(
                        ArsdkFeatureUserStorage.encodeFormatWithEncryption(name, password,
                                FormattingTypeAdapter.from(type)));
            }
            if (sent) {
                formattingInitiated();
            }
            return sent;
        }

        @Override
        public boolean sendPassword(@NonNull String password, @NonNull RemovableUserStorage.PasswordUsage usage) {
            boolean sent = false;
            if (mEncryptionSupported && mLatestState == RemovableUserStorage.FileSystemState.PASSWORD_NEEDED) {
                sent = sendCommand(ArsdkFeatureUserStorage.encodeEncryptionPassword(password,
                        PasswordUsageAdapter.from(usage)));
            }
            return sent;
        }
    };

    private void formattingInitiated() {
        if (mFormattingProgressSupported) {
            mRemovableUserStorage.initFormattingState();
        }
        if (mFormatResultEvtSupported) {
            mWaitingFormatResult = true;
            updateFileSystemState(RemovableUserStorage.FileSystemState.FORMATTING);
        }
        mRemovableUserStorage.notifyUpdated();
    }

    /**
     * Utility method, transforms arsdk's {@link ArsdkFeatureUserStorage.PhyState PhyState}
     * into a groundsdk {@link RemovableUserStorage.PhysicalState}.
     *
     * @param physicalState state of the physical media
     *
     * @return the groundsdk equivalent state
     */
    private static RemovableUserStorage.PhysicalState convert(@Nullable ArsdkFeatureUserStorage.PhyState physicalState) {
        if (physicalState != null) switch (physicalState) {
            case UNDETECTED:
                return RemovableUserStorage.PhysicalState.NO_MEDIA;
            case TOO_SMALL:
                return RemovableUserStorage.PhysicalState.MEDIA_TOO_SMALL;
            case TOO_SLOW:
                return RemovableUserStorage.PhysicalState.MEDIA_TOO_SLOW;
            case USB_MASS_STORAGE:
                return RemovableUserStorage.PhysicalState.USB_MASS_STORAGE;
            case AVAILABLE:
                return RemovableUserStorage.PhysicalState.AVAILABLE;
        }
        return null;
    }

    /**
     * Utility method, transforms arsdk's {@link ArsdkFeatureUserStorage.FsState FsState} into a groundsdk
     * {@link RemovableUserStorage.FileSystemState}.
     *
     * @param fileSystemState state of the data stored on media
     *
     * @return the groundsdk equivalent state
     */
    private static RemovableUserStorage.FileSystemState convert(@Nullable ArsdkFeatureUserStorage.FsState fileSystemState) {
        if (fileSystemState != null) switch (fileSystemState) {
            case UNKNOWN:
                return RemovableUserStorage.FileSystemState.MOUNTING;
            case FORMAT_NEEDED:
                return RemovableUserStorage.FileSystemState.NEED_FORMAT;
            case FORMATTING:
                return RemovableUserStorage.FileSystemState.FORMATTING;
            case READY:
                return RemovableUserStorage.FileSystemState.READY;
            case ERROR:
                return RemovableUserStorage.FileSystemState.ERROR;
            case PASSWORD_NEEDED:
                return RemovableUserStorage.FileSystemState.PASSWORD_NEEDED;
            case CHECKING:
                return RemovableUserStorage.FileSystemState.CHECKING;
            case EXTERNAL_ACCESS_OK:
                return RemovableUserStorage.FileSystemState.EXTERNAL_ACCESS_OK;
        }
        return null;
    }

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

    /**
     * Utility class to adapt {@link ArsdkFeatureUserStorage.PasswordUsage user storage feature} to {@link
     * RemovableUserStorage.PasswordUsage groundsdk} password usage.
     */
    private static final class PasswordUsageAdapter {

        /**
         * Converts a {@code ArsdkFeatureUserStorage.PasswordUsage} to its {@code RemovableUserStorage.PasswordUsage}
         * equivalent.
         *
         * @param usage password usage to convert
         *
         * @return the groundsdk password usage equivalent
         */
        @NonNull
        static RemovableUserStorage.PasswordUsage from(@NonNull ArsdkFeatureUserStorage.PasswordUsage usage) {
            switch (usage) {
                case RECORD:
                    return RemovableUserStorage.PasswordUsage.RECORD;
                case USB:
                    return RemovableUserStorage.PasswordUsage.USB;
            }
            return null;
        }

        /**
         * Converts a {@code RemovableUserStorage.PasswordUsage} to its {@code ArsdkFeatureUserStorage.PasswordUsage}
         * equivalent.
         *
         * @param usage groundsdk password usage to convert
         *
         * @return user storage feature password usage equivalent of the given value
         */
        @NonNull
        static ArsdkFeatureUserStorage.PasswordUsage from(@NonNull RemovableUserStorage.PasswordUsage usage) {
            switch (usage) {
                case RECORD:
                    return ArsdkFeatureUserStorage.PasswordUsage.RECORD;
                case USB:
                    return ArsdkFeatureUserStorage.PasswordUsage.USB;
            }
            return null;
        }
    }
}
