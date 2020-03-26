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

package com.parrot.drone.groundsdk.internal.utility;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareInfo;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.Monitorable;
import com.parrot.drone.groundsdk.internal.engine.EngineBase;
import com.parrot.drone.groundsdk.internal.tasks.Task;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * Utility interface providing access to available device firmwares.
 * <p>
 * This utility may be unavailable if firmware support is disabled in GroundSdk configuration. It may be obtained
 * after engine startup using:
 * <pre>{@code FirmwareStore store = getUtility(FirmwareStore.class);}</pre>
 *
 * @see EngineBase#getUtility(Class)
 * @see GroundSdkConfig#isFirmwareEnabled()
 */
public interface FirmwareStore extends Monitorable<FirmwareStore.Monitor>, Utility {

    /**
     * Callback interface receiving store modification notifications.
     */
    interface Monitor {

        /**
         * Called back when any modification occurs on the store.
         */
        void onChange();
    }

    /**
     * Provides all firmwares that can be applied in order to update a given firmware.
     * <p>
     * This method returns a list containing infos about all firmwares that can be applied to update the specified
     * firmware towards the latest known version, <strong>AND</strong> that have been downloaded.
     * <p>
     * Firmwares in the returned list are sorted by application order, i.e. first firmwares, once downloaded, must be
     * applied to the device before subsequent ones. <br>
     * Only first firmware from the returned list, if any, can be applied immediately to update the device;
     * subsequent firmwares are only given as indication as for what should follow next in the updating process.
     *
     * @param firmware firmware to list applicable updates for
     *
     * @return a list of firmwares that can be applied in order to update the specified firmware towards the latest
     *         known version
     */
    @NonNull
    List<FirmwareInfo> applicableUpdatesFor(@NonNull FirmwareIdentifier firmware);

    /**
     * Provides all firmwares that must be downloaded in order to update a given firmware.
     * <p>
     * This method returns a list containing infos about all firmwares that would need to be applied to update the
     * specified firmware to the latest known version, <strong>AND</strong> that have not already been downloaded yet.
     * <p>
     * Firmwares in the returned list are sorted by application order, i.e. first firmwares, once downloaded, must be
     * applied to the device before subsequent ones.
     *
     * @param firmware firmware to list downloadable updates for
     *
     * @return a list of firmwares that have to be downloaded in order to update the specified firmware
     */
    @NonNull
    List<FirmwareInfo> downloadableUpdatesFor(@NonNull FirmwareIdentifier firmware);

    /**
     * Retrieves the ideal firmware that might be used to update the device with.
     * <p>
     * It is the version that the device will reach if all downloadable firmwares are downloaded and if all applicable
     * updates are applied.<br>
     * This firmware might not be local nor be directly applicable. If the returned value is {@code null}, the device is
     * up to date.
     * <p>
     * <strong>Note:</strong> this version might differ from the greater version of all downloadable and applicable
     * firmwares if, and only if, the ideal firmware is local but cannot be applied because an intermediate, not
     * downloaded firmware is required first.
     *
     * @param firmware firmware to find the ideal update for
     *
     * @return the ideal version, or {@code null} if the device is up to date
     */
    @Nullable
    FirmwareInfo idealUpdateFor(@NonNull FirmwareIdentifier firmware);

    /**
     * Retrieves an {@code InputStream} that provides update file data for a given firmware.
     *
     * @param firmware identifies the firmware to obtain update file data from
     *
     * @return an {@code InputStream} allowing to read content from the firmware update file, or {@code null} if no
     *         update file is available locally for the specified firmware
     */
    @Nullable
    InputStream getFirmwareStream(@NonNull FirmwareIdentifier firmware);

    /**
     * Retrieves the update file for a given firmware.
     * <p>
     * This method exists for legacy use by drone FTP update mechanism, that really needs a File object to operate
     * properly.
     * <p>
     * Since there are case where the update file content does not exist locally as a File object (asset/obb
     * application provided firmwares), it might be needed to extract the content to a file. As a consequence, this
     * method is asynchronous and returns a task that provides the requested file upon successful completion.
     *
     * @param firmware identifies the firmware whose update file is requested
     *
     * @return an asynchronous task, that provides the requested file upon successful completion and may be canceled.
     */
    @NonNull
    Task<File> getFirmwareFile(@NonNull FirmwareIdentifier firmware);
}
