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

package com.parrot.drone.groundsdk.internal.engine.reversegeocoder;

import android.location.Address;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.internal.utility.ReverseGeocoderUtility;

import java.util.HashSet;
import java.util.Set;

/** Implementation class for {@code ReverseGeocoderUtility}. */
public class ReverseGeocoderUtilityCore implements ReverseGeocoderUtility {

    /** All registered monitors of this utility. */
    @NonNull
    private final Set<ReverseGeocoderUtility.Monitor> mMonitors;

    /** Detected address. */
    @Nullable
    private
    Address mAddress;

    /**
     * Constructor.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public ReverseGeocoderUtilityCore() {
        mMonitors = new HashSet<>();
    }

    @Override
    public void monitorWith(@NonNull Monitor monitor) {
        mMonitors.add(monitor);
    }

    @Override
    public void disposeMonitor(@NonNull Monitor monitor) {
        mMonitors.remove(monitor);
    }

    @Nullable
    @Override
    public Address getAddress() {
        return mAddress;
    }

    /**
     * Sets a new address.
     *
     * @param address the address to set
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void updateAddress(@Nullable Address address) {
        mAddress = address;
        for (Monitor monitor : mMonitors) {
            monitor.onChange();
        }
    }
}
