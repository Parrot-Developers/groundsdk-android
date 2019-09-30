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

package com.parrot.drone.groundsdk.internal.engine;

import android.content.Context;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.internal.component.ComponentStore;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.utility.DroneStore;
import com.parrot.drone.groundsdk.internal.utility.RemoteControlStore;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;

@SuppressWarnings("unused")
public class MockEngine extends EngineBase {

    private DroneStore mDroneStore;

    private RemoteControlStore mRcStore;

    private boolean mStarted;

    public MockEngine(@NonNull Context context, @NonNull UtilityRegistry utilities,
                      @NonNull ComponentStore<Facility> facilityStore) {
        super(MockEngineController.create(context, utilities, facilityStore));
    }

    public boolean isStarted() {
        return mStarted;
    }

    @Override
    protected void onStart() {
        mDroneStore = getUtilityOrThrow(DroneStore.class);
        mRcStore = getUtilityOrThrow(RemoteControlStore.class);
        mStarted = true;
    }

    @Override
    protected void onStop() {
        mStarted = false;
    }

    public void addDrone(@NonNull DroneCore drone) {
        mDroneStore.add(drone);
    }

    public void removeDrone(DroneCore drone) {
        mDroneStore.remove(drone.getUid());
    }

    public void addRemoteControl(@NonNull RemoteControlCore rc) {
        mRcStore.add(rc);
    }

    public void removeRemoteControl(RemoteControlCore rc) {
        mRcStore.remove(rc.getUid());
    }
}