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

package com.parrot.drone.groundsdk.internal.device;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.RemoteControlListEntry;

/** Remote control list entry provided to the client for filtering purposes. */
public final class RemoteControlListEntryCore extends RemoteControlListEntry {

    /** Remote control uid. */
    @NonNull
    private final String mUid;

    /** Remote control model. */
    @NonNull
    private final RemoteControl.Model mModel;

    /** Remote control name. */
    @NonNull
    private final String mName;

    /** Remote control state. */
    @NonNull
    private final DeviceState mState;

    /**
     * Constructor.
     *
     * @param remoteControl remote control that this entry represents
     */
    public RemoteControlListEntryCore(@NonNull RemoteControlCore remoteControl) {
        mUid = remoteControl.getUid();
        mModel = remoteControl.getModel();
        mName = remoteControl.getName();
        mState = remoteControl.getDeviceStateCore();
    }

    @NonNull
    @Override
    public String getUid() {
        return mUid;
    }

    @NonNull
    @Override
    public RemoteControl.Model getModel() {
        return mModel;
    }

    @NonNull
    @Override
    public String getName() {
        return mName;
    }

    @NonNull
    @Override
    public DeviceState getState() {
        return mState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RemoteControlListEntryCore that = (RemoteControlListEntryCore) o;

        return mUid.equals(that.mUid);
    }

    @Override
    public int hashCode() {
        return mUid.hashCode();
    }
}
