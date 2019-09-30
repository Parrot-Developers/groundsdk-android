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
import com.parrot.drone.groundsdk.device.RemoteControl;

public class MockRC extends RemoteControlCore implements MockDevice<MockRC> {

    @NonNull
    private static final RemoteControl.Model DEFAULT_MODEL = RemoteControl.Model.SKY_CONTROLLER_3;

    public MockRC(@NonNull String uid) {
        this(uid, DEFAULT_MODEL);
    }

    @SuppressWarnings("WeakerAccess")
    public MockRC(@NonNull String uid, @NonNull RemoteControl.Model model) {
        this(uid, model, "rc-" + uid, new MockDevice.Delegate());
    }

    @NonNull
    private final MockDevice.Impl<MockRC> mImpl;

    @NonNull
    private final DeviceConnector mAsConnector;

    private MockRC(@NonNull String uid, @NonNull RemoteControl.Model model,
                   @NonNull String name, @NonNull MockDevice.Delegate delegate) {
        super(uid, model, name, delegate);
        mImpl = new MockDevice.Impl<>(this, delegate);
        mAsConnector = DeviceConnectorCore.createRCConnector(uid);
    }

    @NonNull
    public final DeviceConnector asConnector() {
        return mAsConnector;
    }

    @NonNull
    @Override
    public MockRC addConnectors(@NonNull DeviceConnector... connectors) {
        return mImpl.addConnectors(connectors);
    }

    @NonNull
    @Override
    public MockRC removeConnectors(@NonNull DeviceConnector... connectors) {
        return mImpl.addConnectors(connectors);
    }

    @NonNull
    @Override
    public MockRC mockPersisted() {
        return mImpl.mockPersisted();
    }

    @NonNull
    @Override
    public MockRC mockConnecting(@NonNull DeviceConnector connector) {
        return mImpl.mockConnecting(connector);
    }

    @NonNull
    @Override
    public MockRC mockConnected() {
        return mImpl.mockConnected();
    }

    @NonNull
    @Override
    public MockRC mockDisconnecting() {
        return mImpl.mockDisconnecting();
    }

    @NonNull
    @Override
    public MockRC mockDisconnected() {
        return mImpl.mockDisconnected();
    }

    @NonNull
    @Override
    public Expectation.Forget<MockRC> expectForget() {
        return mImpl.expectForget();
    }

    @NonNull
    @Override
    public Expectation.Connect<MockRC> expectConnectOn(@NonNull DeviceConnector connector) {
        return mImpl.expectConnectOn(connector);
    }

    @NonNull
    @Override
    public Expectation.Disconnect<MockRC> expectDisconnect() {
        return mImpl.expectDisconnect();
    }

    @NonNull
    @Override
    public MockRC revokeLastExpectation() {
        return mImpl.revokeLastExpectation();
    }

    @NonNull
    @Override
    public MockRC assertNoExpectations() {
        return mImpl.assertNoExpectations();
    }
}
