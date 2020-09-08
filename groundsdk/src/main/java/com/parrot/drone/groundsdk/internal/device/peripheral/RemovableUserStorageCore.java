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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.RemovableUserStorage;
import com.parrot.drone.groundsdk.internal.component.ComponentDescriptor;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.component.SingletonComponentCore;

import java.util.EnumSet;

/** Core class for RemovableUserStorage. */
public class RemovableUserStorageCore extends SingletonComponentCore implements RemovableUserStorage {

    /** Description of RemovableUserStorage. */
    private static final ComponentDescriptor<Peripheral, RemovableUserStorage> DESC =
            ComponentDescriptor.of(RemovableUserStorage.class);

    /** Engine-specific backend for RemovableUserStorage. */
    public interface Backend {

        /**
         * Formats the media.
         *
         * @param type formatting type
         * @param name new name given to the media. If empty, the media name is set to the product name.
         *
         * @return {@code true} if the operation could be initiated, otherwise {@code false}
         */
        boolean format(@NonNull FormattingType type, @NonNull String name);

        /**
         * Formats the media with encryption.
         *
         * @param password encryption password
         * @param type formatting type
         * @param name new name given to the media. If empty, the media name is set to the product name.
         *
         * @return {@code true} if the operation could be initiated, otherwise {@code false}
         */
        boolean formatWithEncryption(@NonNull String password, @NonNull FormattingType type, @NonNull String name);

        /**
         * Sends the password in order to decrypt the media.
         *
         * @param password encryption password
         * @param usage encryption usage
         *
         * @return {@code true} if the password has been sent, otherwise {@code false}
         */
        boolean sendPassword(@NonNull String password, @NonNull PasswordUsage usage);
    }

    /** Engine peripheral backend. */
    @NonNull
    private final Backend mBackend;

    /** Current user storage file system state. */
    @NonNull
    private FileSystemState mFileSystemState;

    /** Current user storage physical state. */
    @NonNull
    private PhysicalState mPhysicalState;

    /** User storage UUID. */
    @Nullable
    private String mUuid;

    /** Current media information or {@code null} if there is no media. */
    @Nullable
    private MediaInfoCore mMediaInfo;

    /** Current available space on media in bytes or {@code -1} if this information is not available. */
    private long mAvailableSpace;

    /** Current ability to format the media. */
    private boolean mCanFormat;

    /** Ability to encrypt decrypt the media. */
    private boolean mEncryptionSupported;

    /** Whether media is encrypted. */
    private boolean mEncrypted;

    /** Supported formatting types. */
    @NonNull
    private final EnumSet<FormattingType> mSupportedFormattingTypes;

    /** Current formatting state. */
    @Nullable
    private FormattingStateCore mFormattingState;

    /**
     * Constructor.
     *
     * @param peripheralStore store where this peripheral belongs
     * @param backend         backend used to forward actions to the engine
     */
    public RemovableUserStorageCore(@NonNull ComponentStore<Peripheral> peripheralStore, @NonNull Backend backend) {
        super(DESC, peripheralStore);
        mBackend = backend;
        mPhysicalState = PhysicalState.NO_MEDIA;
        mFileSystemState = FileSystemState.READY;
        mAvailableSpace = -1;
        mSupportedFormattingTypes = EnumSet.of(FormattingType.FULL);
    }

    @NonNull
    @Override
    public FileSystemState getFileSystemState() {
        return mFileSystemState;
    }

    @NonNull
    @Override
    public PhysicalState getPhysicalState() {
        return mPhysicalState;
    }

    @Nullable
    @Override
    public String getUuid() {
        return mUuid;
    }

    @Nullable
    @Override
    public MediaInfo getMediaInfo() {
        return mMediaInfo;
    }

    @Override
    public long getAvailableSpace() {
        return mAvailableSpace;
    }

    @Override
    public boolean canFormat() {
        return mCanFormat;
    }

    @Override
    public boolean isEncryptionSupported() {
        return mEncryptionSupported;
    }

    @Override
    public boolean isEncrypted() {
        return mEncrypted;
    }

    @NonNull
    @Override
    public EnumSet<FormattingType> supportedFormattingTypes() {
        return EnumSet.copyOf(mSupportedFormattingTypes);
    }

    @Nullable
    @Override
    public FormattingState formattingState() {
        return mFormattingState;
    }

    @Override
    public boolean format(@NonNull FormattingType type) {
        return format(type, "");
    }

    @Override
    public boolean format(@NonNull FormattingType type, @NonNull String name) {
        return mCanFormat && mSupportedFormattingTypes.contains(type) && mBackend.format(type, name);
    }

    @Override
    public boolean formatWithEncryption(@NonNull String password, @NonNull FormattingType type, @Nullable String name) {
        return mCanFormat && mSupportedFormattingTypes.contains(type) && mEncryptionSupported &&
                mBackend.formatWithEncryption(password, type, name != null ? name : "");
    }

    @Override
    public boolean sendPassword(@NonNull String password, @NonNull PasswordUsage usage) {
        return mBackend.sendPassword(password, usage);
    }

    /**
     * Updates the user storage physical state.
     *
     * @param physicalState new storage physical state
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public RemovableUserStorageCore updatePhysicalState(@NonNull PhysicalState physicalState) {
        if (physicalState != mPhysicalState) {
            mPhysicalState = physicalState;
            if (physicalState == PhysicalState.NO_MEDIA) {
                // Reset information when there is no media
                mMediaInfo = null;
                mUuid = null;
                mAvailableSpace = -1;
                mCanFormat = false;
            }
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the user storage file system state.
     *
     * @param fileSystemState new storage file system state
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public RemovableUserStorageCore updateFileSystemState(@NonNull FileSystemState fileSystemState) {
        if (fileSystemState != mFileSystemState) {
            mFileSystemState = fileSystemState;
            if (fileSystemState != FileSystemState.FORMATTING) {
                mFormattingState = null;
            }
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the user storage UUID.
     *
     * @param uuid new UUID
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public RemovableUserStorageCore updateUuid(@NonNull String uuid) {
        if (!uuid.equals(mUuid)) {
            mUuid = uuid;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the media information.
     *
     * @param name     new name
     * @param capacity new capacity in bytes
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public RemovableUserStorageCore updateMediaInfo(@NonNull String name, long capacity) {
        if (mMediaInfo == null) {
            mMediaInfo = new MediaInfoCore(name, capacity);
            mChanged = true;
        } else if (mMediaInfo.updateName(name)
                   || mMediaInfo.updateCapacity(capacity)) {
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates the available space on media.
     *
     * @param availableSpace new available space in bytes
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public RemovableUserStorageCore updateAvailableSpace(long availableSpace) {
        if (availableSpace != mAvailableSpace) {
            mAvailableSpace = availableSpace;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates current ability to format the media.
     *
     * @param canFormat {@code true} if the media can be formatted
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public RemovableUserStorageCore updateCanFormat(boolean canFormat) {
        if (canFormat != mCanFormat) {
            mCanFormat = canFormat;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates current ability to encrypt the media.
     *
     * @param encryptionSupported {@code true} if the media can be encrypted
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public RemovableUserStorageCore updateIsEncryptionSupported(boolean encryptionSupported) {
        if (encryptionSupported != mEncryptionSupported) {
            mEncryptionSupported = encryptionSupported;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates media encryption information.
     *
     * @param encrypted {@code true} if the media is encrypted
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public RemovableUserStorageCore updateIsEncrypted(boolean encrypted) {
        if (encrypted != mEncrypted) {
            mEncrypted = encrypted;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates supported formatting types.
     *
     * @param types new supported formatting types
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public RemovableUserStorageCore updateSupportedFormattingTypes(@NonNull EnumSet<FormattingType> types) {
        mChanged |= mSupportedFormattingTypes.retainAll(types) | mSupportedFormattingTypes.addAll(types);
        return this;
    }

    /**
     * Initializes the current formatting state.
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public RemovableUserStorageCore initFormattingState() {
        mFormattingState = new FormattingStateCore();
        mChanged = true;
        return this;
    }

    /**
     * Updates current formatting step.
     *
     * @param step new formatting step
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public RemovableUserStorageCore updateFormattingStep(@NonNull FormattingState.Step step) {
        if (mFormattingState != null && mFormattingState.mStep != step) {
            mFormattingState.mStep = step;
            mChanged = true;
        }
        return this;
    }

    /**
     * Updates current formatting progress.
     *
     * @param progress new formatting progress
     *
     * @return this, to allow call chaining
     */
    @NonNull
    public RemovableUserStorageCore updateFormattingProgress(@IntRange(from = 0, to = 100) int progress) {
        if (mFormattingState != null && mFormattingState.mProgress != progress) {
            mFormattingState.mProgress = progress;
            mChanged = true;
        }
        return this;
    }

    /** Core class for media information. */
    private static final class MediaInfoCore implements RemovableUserStorage.MediaInfo {

        /** Label of media. */
        @NonNull
        private String mName;

        /** Capacity of media in bytes. */
        private long mCapacity;

        /**
         * Constructor.
         *
         * @param name     name
         * @param capacity capacity in bytes
         */
        private MediaInfoCore(@NonNull String name, long capacity) {
            mName = name;
            mCapacity = capacity;
        }

        @NonNull
        @Override
        public String getName() {
            return mName;
        }

        @Override
        public long getCapacity() {
            return mCapacity;
        }

        /**
         * Updates the media name.
         *
         * @param name new name
         *
         * @return true if name has changed, false otherwise
         */
        boolean updateName(@NonNull String name) {
            if (!name.equals(mName)) {
                mName = name;
                return true;
            }

            return false;
        }

        /**
         * Updates the media capacity.
         *
         * @param capacity new capacity
         *
         * @return true if capacity has changed, false otherwise
         */
        boolean updateCapacity(long capacity) {
            if (capacity != mCapacity) {
                mCapacity = capacity;
                return true;
            }

            return false;
        }
    }

    /**
     * Core class for the formatting process state.
     */
    private static final class FormattingStateCore implements FormattingState {

        /** Current formatting step. */
        @NonNull
        private Step mStep;

        /** Formatting progress of the current step. */
        @IntRange(from = 0, to = 100)
        private int mProgress;

        /**
         * Constructor.
         */
        private FormattingStateCore() {
            mStep = Step.PARTITIONING;
            mProgress = 0;
        }

        @NonNull
        @Override
        public Step step() {
            return mStep;
        }

        @Override
        public int progress() {
            return mProgress;
        }
    }
}
