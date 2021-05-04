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

package com.parrot.drone.groundsdk.device.peripheral;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;

import java.util.EnumSet;

/**
 * Removable user storage interface.
 * <p>
 * This peripheral can be obtained from a {@code Drone} using:
 * <pre>{@code drone.getPeripheral(RemovableUserStorage.class)}</pre>
 *
 * @see Peripheral.Provider#getPeripheral(Class)
 * @see Peripheral.Provider#getPeripheral(Class, Ref.Observer)
 */
public interface RemovableUserStorage extends Peripheral {

    /**
     * File system state of the user storage.
     */
    enum FileSystemState {
        /** Media is being mounted. */
        MOUNTING,

        /**
         * Media has to be formatted.
         * <p>
         * The file system is not supported, or the partition is not formatted, or the capacity is too low.
         * <p>
         * The media will not be usable until it's formatted.
         *
         * @see #format(FormattingType)
         * @see #format(FormattingType, String)
         */
        NEED_FORMAT,

        /** Media is getting formatted. */
        FORMATTING,

        /** Media is being checked. */
        CHECKING,

        /** Media is ready to be used. */
        READY,

        /** Media is cyphered and needs a password in order to be accessed. */
        PASSWORD_NEEDED,

        /**
         * The latest try to decrypt the media failed because of a wrong password.
         * <p>
         * This state indicates the result of decryption and is transient. The state will change to another state
         * quickly after decryption result is notified.
         */
        DECRYPTION_WRONG_PASSWORD,

        /**
         * The latest try to decrypt the media failed because of a wrong usage.
         * <p>
         * The usage specified with the password does not match with the current drone context
         * {@link PasswordUsage#USB USB} or {@link PasswordUsage#RECORD RECORD}
         * <p>
         * This state indicates the result of decryption and is transient. The state will change to another state
         * quickly after decryption result is notified.
         */
        DECRYPTION_WRONG_USAGE,

        /**
         * The latest try to decrypt the media succeeded.
         * <p>
         * This state indicates the result of decryption and is transient. The state will change to another state
         * quickly after decryption result is notified.
         */
        DECRYPTION_SUCCEEDED,

        /**
         * The latest try to format the media succeeded.
         * <p>
         * This state indicates the result of formatting and is transient. The state will change to another state
         * quickly after formatting result is notified.
         */
        FORMATTING_SUCCEEDED,

        /**
         * The latest try to format the media failed.
         * <p>
         * This state indicates the result of formatting and is transient. The state will change to another state
         * quickly after formatting result is notified.
         */
        FORMATTING_FAILED,

        /**
         * The latest try to format the media was denied.
         * <p>
         * This state indicates the result of formatting and is transient. The state will change back to
         * {@link FileSystemState#NEED_FORMAT} or {@link FileSystemState#READY} immediately after formatting result is
         * notified.
         */
        FORMATTING_DENIED,

        /** An error occurred, media cannot be used. */
        ERROR,

        /** Media is not managed by the drone itself but accessible by external means. */
        EXTERNAL_ACCESS_OK
    }


    /**
     * Physical state of the user storage.
     */
    enum PhysicalState {

        /** No media detected. */
        NO_MEDIA,

        /** Media rejected since it's too small for operation. */
        MEDIA_TOO_SMALL,

        /** Media rejected since it's too slow for operation. */
        MEDIA_TOO_SLOW,

        /** Media cannot be mounted since the drone acts as a USB mass-storage device. */
        USB_MASS_STORAGE,

        /** Media is available. */
        AVAILABLE
    }

    /**
     * Type of formatting.
     */
    enum FormattingType {

        /**
         * Full formatting type, that includes low level format operation which can take a lot of time but optimizes
         * performance.
         */
        FULL,

        /** Quick formatting type, that just removes content of the media. */
        QUICK
    }

    /**
     * Usage for decryption.
     */
    enum PasswordUsage {

        /**
         * Standard usage. If the connection succeeded, the drone can write on the removable storage and the user can
         * read and write its content through the sdk.
         */
        RECORD,

        /**
         * USB Mass Storage usage. The user can access to the removable storage through USB directly.
         */
        USB
    }

    /**
     * Information about media.
     */
    interface MediaInfo {

        /**
         * Gets name of the media.
         *
         * @return name of the media
         */
        @NonNull
        String getName();

        /**
         * Gets the capacity of the media.
         *
         * @return the capacity in bytes
         */
        long getCapacity();
    }

    /**
     * Progress state of the formatting process.
     */
    interface FormattingState {

        /**
         * Formatting step.
         */
        enum Step {

            /** The drone is currently partitioning the media. */
            PARTITIONING,

            /** The drone is currently wiping data on the media in order to optimize performance. */
            CLEARING_DATA,

            /** The drone is creating a file system on the media. */
            CREATING_FILE_SYSTEM
        }

        /**
         * Retrieves the current formatting step.
         *
         * @return the formatting step
         */
        @NonNull
        Step step();

        /**
         * Retrieves the formatting progress of the current step, in percent.
         *
         * @return the current formatting progress
         */
        @IntRange(from = 0, to = 100)
        int progress();
    }

    /**
     * Gets the current physical state of the user storage.
     *
     * @return the current physical state
     */
    @NonNull
    PhysicalState getPhysicalState();

    /**
     * Gets the current file system state of the user storage.
     *
     * @return the current file system state
     */
    @NonNull
    FileSystemState getFileSystemState();

    /**
     * Gets the UUID of removable user storage.
     *
     * @return the uuid if available, {@code null} otherwise
     */
    @Nullable
    String getUuid();

    /**
     * Gets information on current media.
     *
     * @return information on current media if available, {@code null} otherwise
     */
    @Nullable
    MediaInfo getMediaInfo();

    /**
     * Gets available free space on current media.
     *
     * @return available free space in bytes or {@code -1} if unknown
     */
    long getAvailableSpace();

    /**
     * Tells whether the media can be formatted.
     * <p>
     * With some drones, the media formatting is allowed only when the {@link #getFileSystemState() state} is
     * {@link FileSystemState#NEED_FORMAT NEED_FORMAT}. With some other drones, it is allowed in both
     * {@link FileSystemState#NEED_FORMAT NEED_FORMAT}
     * and {@link FileSystemState#READY READY} states.
     *
     * @return {@code true} if the media can be formatted, otherwise {@code false}
     *
     * @see #format(FormattingType)
     * @see #format(FormattingType, String)
     */
    boolean canFormat();

    /**
     * Tells whether the media can be encrypted.
     *
     * @return {@code true} if the media can be encrypted, otherwise {@code false}
     */
    boolean isEncryptionSupported();

    /**
     * Tells whether the media is encrypted.
     *
     * @return {@code true} if the media is encrypted, otherwise {@code false}
     */
    boolean isEncrypted();

    /**
     * Retrieves the supported formatting types.
     *
     * @return supported formatting types
     */
    @NonNull
    EnumSet<FormattingType> supportedFormattingTypes();

    /**
     * Gets the progress state of the formatting process.
     * <p>
     * <strong>Note:</strong> the formatting progress state may not be supported by the drone, in which case this
     * method returns {@code null} at any time.
     *
     * @return the formatting state if current {@link #getFileSystemState() state} is
     *         {@link FileSystemState#FORMATTING FORMATTING} and progress is supported, otherwise {@code null}
     *
     * @see #format(FormattingType)
     * @see #format(FormattingType, String)
     */
    @Nullable
    FormattingState formattingState();

    /**
     * Requests a format of the media.
     * The media name is set to the product name.
     * <p>
     * Should be called only when {@link #canFormat media formatting is allowed}.
     * <p>
     * When formatting starts, the current state becomes {@link FileSystemState#FORMATTING FORMATTING}.
     * <p>
     * The formatting result is indicated with the transient state
     * {@link FileSystemState#FORMATTING_SUCCEEDED FORMATTING_SUCCEEDED},
     * {@link FileSystemState#FORMATTING_FAILED FORMATTING_FAILED},
     * or {@link FileSystemState#FORMATTING_DENIED FORMATTING_DENIED}.
     *
     * @param type requested formatting type
     *
     * @return {@code false} if the request was not sent to the device, for instance because the state is not
     *         {@link FileSystemState#NEED_FORMAT NEED_FORMAT} or {@link FileSystemState#READY READY},
     *         {@code true} otherwise
     */
    boolean format(@NonNull FormattingType type);

    /**
     * Requests a format of the media.
     * <p>
     * Should be called only when {@link #canFormat media formatting is allowed}.
     * <p>
     * When formatting starts, the current state becomes {@link FileSystemState#FORMATTING FORMATTING}.
     * <p>
     * The formatting result is indicated with the transient state
     * {@link FileSystemState#FORMATTING_SUCCEEDED FORMATTING_SUCCEEDED},
     * {@link FileSystemState#FORMATTING_FAILED FORMATTING_FAILED},
     * or {@link FileSystemState#FORMATTING_DENIED FORMATTING_DENIED}.
     *
     * @param type requested formatting type
     * @param name new name given to the media. If empty, the media name is set to the product name.
     *
     * @return {@code false} if the request was not sent to the device, for instance because the state is not
     *         {@link FileSystemState#NEED_FORMAT NEED_FORMAT} or {@link FileSystemState#READY READY},
     *         {@code true} otherwise
     */
    boolean format(@NonNull FormattingType type, @NonNull String name);

    /**
     * Requests a format of the media with encryption.
     * <p>
     * Should be called only when {@link #canFormat media formatting is allowed}.
     * <p>
     * When formatting starts, the current state becomes {@link FileSystemState#FORMATTING FORMATTING}.
     * <p>
     * The formatting result is indicated with the transient state
     * {@link FileSystemState#FORMATTING_SUCCEEDED FORMATTING_SUCCEEDED},
     * {@link FileSystemState#FORMATTING_FAILED FORMATTING_FAILED},
     * or {@link FileSystemState#FORMATTING_DENIED FORMATTING_DENIED}.
     *
     * @param password requested password for encryption
     * @param type requested formatting type
     * @param name new name given to the media. If empty, the media name is set to the product name.
     *
     * @return {@code false} if the request was not sent to the device, for instance because the state is not
     *         {@link FileSystemState#NEED_FORMAT NEED_FORMAT} or {@link FileSystemState#READY READY}, {@code true}
     *         otherwise
     */
    boolean formatWithEncryption(@NonNull String password, @NonNull FormattingType type, @Nullable String name);

    /**
     * Sends a password in order to decrypt the media.
     * <p>
     * Should be called only when the current state is {@link FileSystemState#PASSWORD_NEEDED PASSWORD_NEEDED}.
     *
     * @param password requested password to decrypt the media
     * @param usage requested password usage
     *
     * @return {@code false} if the password was not sent to the device, for instance because the state is not
     *         {@link FileSystemState#PASSWORD_NEEDED PASSWORD_NEEDED}, {@code true} otherwise
     */
    boolean sendPassword(@NonNull String password, @NonNull PasswordUsage usage);
}
