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

package com.parrot.drone.groundsdk.facility.firmware;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.DeviceModel;

/**
 * Uniquely identifies a device firmware.
 */
public final class FirmwareIdentifier {

    /** Device model onto which this firmware applies. */
    @NonNull
    private final DeviceModel mModel;

    /** Firmware version. */
    @NonNull
    private final FirmwareVersion mVersion;

    /**
     * Constructor.
     *
     * @param model   device model onto which this firmware applies
     * @param version firmware version
     */
    public FirmwareIdentifier(@NonNull DeviceModel model, @NonNull FirmwareVersion version) {
        mModel = model;
        mVersion = version;
    }

    /**
     * Retrieves the model of device onto which this firmware applies.
     *
     * @return device model
     */
    @NonNull
    public DeviceModel getDeviceModel() {
        return mModel;
    }

    /**
     * Retrieves this firmware version.
     *
     * @return firmware version
     */
    @NonNull
    public FirmwareVersion getVersion() {
        return mVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FirmwareIdentifier that = (FirmwareIdentifier) o;

        return mModel.equals(that.mModel) && mVersion.equals(that.mVersion);
    }

    @Override
    public int hashCode() {
        int result = mModel.hashCode();
        result = 31 * result + mVersion.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return mModel.name() + " " + mVersion;
    }
}
