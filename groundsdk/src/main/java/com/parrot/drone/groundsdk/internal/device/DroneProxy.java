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
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.Ref;
import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceState;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.pilotingitf.PilotingItf;
import com.parrot.drone.groundsdk.internal.component.ComponentRef;
import com.parrot.drone.groundsdk.internal.session.Session;

/**
 * Implementation of the public {@link Drone} API.
 * <p>
 * Basically delegates public API calls down to a {@link DroneCore} delegate.
 */
public final class DroneProxy extends Drone {

    /** Session managing the lifecycle of all refs issued by this proxy instance. */
    @NonNull
    private final Session mSession;

    /** DroneCore delegate. */
    @NonNull
    private final DroneCore mDrone;

    /**
     * Constructor.
     *
     * @param session  session that will manage issued refs
     * @param delegate drone delegate to forward calls to
     */
    public DroneProxy(@NonNull Session session, @NonNull DroneCore delegate) {
        mSession = session;
        mDrone = delegate;
    }

    @NonNull
    @Override
    public String getUid() {
        return mDrone.getUid();
    }

    @NonNull
    @Override
    public Model getModel() {
        return mDrone.getModel();
    }

    @NonNull
    @Override
    public String getName() {
        return mDrone.getName();
    }

    @NonNull
    @Override
    public Ref<String> getName(@NonNull Ref.Observer<String> observer) {
        return new DeviceNameRef(mSession, observer, mDrone.getNameHolder());
    }

    @NonNull
    @Override
    public DeviceState getState() {
        return mDrone.getDeviceStateCore();
    }

    @NonNull
    @Override
    public Ref<DeviceState> getState(@NonNull Ref.Observer<DeviceState> observer) {
        return new DeviceStateRef(mSession, observer, mDrone.getStateHolder());
    }

    @Nullable
    @Override
    public <T extends Instrument> T getInstrument(@NonNull Class<T> instrumentClass) {
        return mDrone.getInstrumentStore().get(mSession, instrumentClass);
    }

    @NonNull
    @Override
    public <T extends Instrument> Ref<T> getInstrument(@NonNull Class<T> instrumentClass,
                                                       @NonNull Ref.Observer<T> observer) {
        return new ComponentRef<>(mSession, observer, mDrone.getInstrumentStore(), instrumentClass);
    }

    @Nullable
    @Override
    public <T extends PilotingItf> T getPilotingItf(@NonNull Class<T> pilotingItfClass) {
        return mDrone.getPilotingItfStore().get(mSession, pilotingItfClass);
    }

    @NonNull
    @Override
    public <T extends PilotingItf> Ref<T> getPilotingItf(@NonNull Class<T> pilotingItfClass,
                                                         @NonNull Ref.Observer<T> observer) {
        return new ComponentRef<>(mSession, observer, mDrone.getPilotingItfStore(), pilotingItfClass);
    }

    @Nullable
    @Override
    public <T extends Peripheral> T getPeripheral(@NonNull Class<T> peripheralClass) {
        return mDrone.getPeripheralStore().get(mSession, peripheralClass);
    }

    @NonNull
    @Override
    public <T extends Peripheral> Ref<T> getPeripheral(@NonNull Class<T> peripheralClass,
                                                       @NonNull Ref.Observer<T> observer) {
        return new ComponentRef<>(mSession, observer, mDrone.getPeripheralStore(), peripheralClass);
    }

    @Override
    public boolean forget() {
        return mDrone.forget();
    }

    @Override
    public boolean connect() {
        return mDrone.connect(null, null);
    }

    @Override
    public boolean connect(@NonNull DeviceConnector connector) {
        return mDrone.connect(connector, null);
    }

    @Override
    public boolean connect(@NonNull DeviceConnector connector, @NonNull String password) {
        return mDrone.connect(connector, password);
    }

    @Override
    public boolean disconnect() {
        return mDrone.disconnect();
    }
}
