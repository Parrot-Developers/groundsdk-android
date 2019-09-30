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

package com.parrot.drone.groundsdk.internal.engine.firmware;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.internal.utility.FirmwareBlackList;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@code FirmwareBlackList}.
 * <p>
 * Firmware blacklist is saved in shared preferences.
 * <p>
 * It can be fetched from versions server. The engine managing versions shall call {@link #addToBlackList(Set)} to
 * update the blacklist with fetched data.
 * <p>
 * It can also be defined in application resources.
 */
public class FirmwareBlackListCore implements FirmwareBlackList {

    /** Firmware engine. */
    @NonNull
    private final Persistence mPersistence;

    /** All registered monitors of the store. */
    @NonNull
    private final Set<Monitor> mMonitors;

    /** Firmware blacklist. */
    @NonNull
    private final Set<FirmwareIdentifier> mBlackList;

    /**
     * Constructor.
     *
     * @param persistence persistence layer of firmware engine
     */
    FirmwareBlackListCore(@NonNull Persistence persistence) {
        mPersistence = persistence;
        mMonitors = new HashSet<>();
        mBlackList = mPersistence.loadBlackList();
    }

    @Override
    public void monitorWith(@NonNull Monitor monitor) {
        mMonitors.add(monitor);
    }

    @Override
    public void disposeMonitor(@NonNull Monitor monitor) {
        mMonitors.remove(monitor);
    }

    @Override
    public boolean isFirmwareBlacklisted(@NonNull FirmwareIdentifier version) {
        return mBlackList.contains(version);
    }

    /**
     * Adds firmware versions to firmware blacklist if they're not already present.
     *
     * @param versions firmware versions to add to blacklist
     */
    void addToBlackList(@NonNull Set<FirmwareIdentifier> versions) {
        mBlackList.addAll(versions);
        mPersistence.saveBlackList(mBlackList);
        for (FirmwareBlackList.Monitor monitor : mMonitors) {
            monitor.onChange();
        }
    }
}
