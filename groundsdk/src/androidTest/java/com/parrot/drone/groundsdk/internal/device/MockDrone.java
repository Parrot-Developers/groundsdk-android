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

import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.Drone;

public class MockDrone extends DroneCore implements MockDevice<MockDrone> {

    @NonNull
    private static final Drone.Model DEFAULT_MODEL = Drone.Model.ANAFI_4K;

    public MockDrone(@NonNull String uid) {
        this(uid, DEFAULT_MODEL);
    }

    public MockDrone(@NonNull String uid, @NonNull Drone.Model model) {
        this(uid, model, "drone-" + uid, new MockDevice.Delegate());
    }

    @NonNull
    private final MockDevice.Impl<MockDrone> mImpl;

    private MockDrone(@NonNull String uid, @NonNull Drone.Model model,
                      @NonNull String name, @NonNull MockDevice.Delegate delegate) {
        super(uid, model, name, delegate);
        mImpl = new MockDevice.Impl<>(this, delegate);
    }

    @NonNull
    @Override
    public MockDrone addConnectors(@NonNull DeviceConnector... connectors) {
        return mImpl.addConnectors(connectors);
    }

    @NonNull
    @Override
    public MockDrone removeConnectors(@NonNull DeviceConnector... connectors) {
        return mImpl.addConnectors(connectors);
    }

    @NonNull
    @Override
    public MockDrone mockPersisted() {
        return mImpl.mockPersisted();
    }

    @NonNull
    @Override
    public MockDrone mockConnecting(@NonNull DeviceConnector connector) {
        return mImpl.mockConnecting(connector);
    }

    @NonNull
    @Override
    public MockDrone mockConnected() {
        return mImpl.mockConnected();
    }

    @NonNull
    @Override
    public MockDrone mockDisconnecting() {
        return mImpl.mockDisconnecting();
    }

    @NonNull
    @Override
    public MockDrone mockDisconnected() {
        return mImpl.mockDisconnected();
    }

    @NonNull
    @Override
    public Expectation.Forget<MockDrone> expectForget() {
        return mImpl.expectForget();
    }

    @NonNull
    @Override
    public Expectation.Connect<MockDrone> expectConnectOn(@NonNull DeviceConnector connector) {
        return mImpl.expectConnectOn(connector);
    }

    @NonNull
    @Override
    public Expectation.Disconnect<MockDrone> expectDisconnect() {
        return mImpl.expectDisconnect();
    }

    @NonNull
    @Override
    public MockDrone revokeLastExpectation() {
        return mImpl.revokeLastExpectation();
    }

    @NonNull
    @Override
    public MockDrone assertNoExpectations() {
        return mImpl.assertNoExpectations();
    }
}
