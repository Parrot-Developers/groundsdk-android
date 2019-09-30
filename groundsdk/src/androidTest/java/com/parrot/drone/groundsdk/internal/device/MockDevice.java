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

import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceState;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public interface MockDevice<D extends MockDevice<D>> {

    @NonNull
    D addConnectors(@NonNull DeviceConnector... connectors);

    @NonNull
    D removeConnectors(@NonNull DeviceConnector... connectors);

    @NonNull
    D mockPersisted();

    @NonNull
    D mockConnecting(@NonNull DeviceConnector connector);

    @NonNull
    D mockConnected();

    @NonNull
    D mockDisconnecting();

    @NonNull
    D mockDisconnected();

    @NonNull
    Expectation.Forget<D> expectForget();

    @NonNull
    Expectation.Connect<D> expectConnectOn(@NonNull DeviceConnector connector);

    @NonNull
    Expectation.Disconnect<D> expectDisconnect();

    @NonNull
    D revokeLastExpectation();

    @NonNull
    D assertNoExpectations();

    abstract class Expectation<D extends MockDevice> {

        @NonNull
        private final D mDevice;

        private Expectation(@NonNull D device) {
            mDevice = device;
        }

        @NonNull
        public D device() {
            return mDevice;
        }

        public static final class Forget<D extends MockDevice> extends Expectation<D> {

            public interface Action<D extends MockDevice> {

                Action<MockDevice> ACCEPT = device -> true;

                Action<MockDevice> REJECT = device -> false;

                boolean execute(@NonNull D device);
            }

            @NonNull
            private Action<? super D> mAction = Action.ACCEPT;

            private Forget(@NonNull D device) {
                super(device);
            }

            public Forget<D> andThen(@NonNull Action<D> action) {
                mAction = action;
                return this;
            }

            private boolean process() {
                return mAction.execute(((Expectation<D>) this).mDevice);
            }
        }

        public static final class Connect<D extends MockDevice> extends Expectation<D> {

            public interface Action<D extends MockDevice> {

                Action<MockDevice> ACCEPT = (device, connector, password) -> {
                    device.mockConnecting(connector);
                    return true;
                };

                Action<MockDevice> REJECT = (device, connector, password) -> false;

                boolean execute(@NonNull D device, @NonNull DeviceConnector connector, @Nullable String password);
            }

            @NonNull
            private final DeviceConnector mConnector;

            @NonNull
            private Action<? super D> mAction = Action.ACCEPT;

            @Nullable
            private String mPassword;

            private boolean mVerifyPassword;

            private Connect(@NonNull D device, @NonNull DeviceConnector connector) {
                super(device);
                mConnector = connector;
            }

            public Connect<D> withPassword(@Nullable String password) {
                mVerifyPassword = true;
                mPassword = password;
                return this;
            }

            public Connect<D> andThen(@NonNull Action<D> action) {
                mAction = action;
                return this;
            }

            private boolean process(@NonNull DeviceConnector connector, @Nullable String password) {
                assertThat(connector, is(mConnector));
                if (mVerifyPassword) {
                    assertThat(password, is(mPassword));
                }
                return mAction.execute(((Expectation<D>) this).mDevice, connector, password);
            }
        }

        public static final class Disconnect<D extends MockDevice> extends Expectation<D> {

            public interface Action<D extends MockDevice> {

                Action<MockDevice> ACCEPT = device -> {
                    device.mockDisconnecting();
                    return true;
                };

                Action<MockDevice> REJECT = device -> false;

                boolean execute(@NonNull D device);
            }

            @NonNull
            private Action<? super D> mAction = Action.ACCEPT;

            private Disconnect(@NonNull D device) {
                super(device);
            }

            public Disconnect<D> andThen(@NonNull Action<D> action) {
                mAction = action;
                return this;
            }

            private boolean process() {
                return mAction.execute(((Expectation<D>) this).mDevice);
            }
        }
    }

    final class Impl<D extends DeviceCore & MockDevice<D>> implements MockDevice<D> {

        @NonNull
        private final D mDevice;

        @NonNull
        private final Delegate mDelegate;

        Impl(@NonNull D device, @NonNull Delegate delegate) {
            mDevice = device;
            mDelegate = delegate;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public D addConnectors(@NonNull DeviceConnector... connectors) {
            DeviceStateCore state = mDevice.getDeviceStateCore();
            Set<DeviceConnector> currentConnectors = new HashSet<>(Arrays.asList(state.getConnectors()));
            currentConnectors.addAll(Arrays.asList(connectors));
            state.updateConnectors((Set) currentConnectors).notifyUpdated();
            return mDevice;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public D removeConnectors(@NonNull DeviceConnector... connectors) {
            DeviceStateCore state = mDevice.getDeviceStateCore();
            Set<DeviceConnector> currentConnectors = new HashSet<>(Arrays.asList(state.getConnectors()));
            currentConnectors.removeAll(Arrays.asList(connectors));
            state.updateConnectors((Set) currentConnectors).notifyUpdated();
            return mDevice;
        }

        @NonNull
        @Override
        public D mockPersisted() {
            mDevice.getDeviceStateCore().updatePersisted(true).notifyUpdated();
            return mDevice;
        }

        @NonNull
        @Override
        public D mockConnecting(@NonNull DeviceConnector connector) {
            mDevice.getDeviceStateCore()
                   .updateConnectionState(DeviceState.ConnectionState.CONNECTING)
                   .updateActiveConnector((DeviceConnectorCore) connector)
                   .notifyUpdated();
            return mDevice;
        }

        @NonNull
        @Override
        public D mockConnected() {
            mDevice.getDeviceStateCore().updateConnectionState(DeviceState.ConnectionState.CONNECTED).notifyUpdated();
            return mDevice;
        }

        @NonNull
        @Override
        public D mockDisconnecting() {
            mDevice.getDeviceStateCore().updateConnectionState(DeviceState.ConnectionState.DISCONNECTING)
                   .notifyUpdated();
            return mDevice;
        }

        @NonNull
        @Override
        public D mockDisconnected() {
            mDevice.getDeviceStateCore().updateConnectionState(DeviceState.ConnectionState.DISCONNECTED)
                   .updateActiveConnector(null).notifyUpdated();
            return mDevice;
        }

        @NonNull
        @Override
        public Expectation.Forget<D> expectForget() {
            return mDelegate.queue(new Expectation.Forget<>(mDevice));
        }

        @NonNull
        @Override
        public Expectation.Connect<D> expectConnectOn(@NonNull DeviceConnector connector) {
            return mDelegate.queue(new Expectation.Connect<>(mDevice, connector));
        }

        @NonNull
        @Override
        public Expectation.Disconnect<D> expectDisconnect() {
            return mDelegate.queue(new Expectation.Disconnect<>(mDevice));
        }

        @NonNull
        @Override
        public D revokeLastExpectation() {
            mDelegate.poll(Expectation.class);
            return mDevice;
        }

        @NonNull
        @Override
        public D assertNoExpectations() {
            assertThat(mDelegate.mExpectations, empty());
            return mDevice;
        }

    }

    final class Delegate implements DeviceCore.Delegate {

        @NonNull
        private final Queue<Expectation<?>> mExpectations = new LinkedList<>();

        @Override
        public boolean forget() {
            return poll(Expectation.Forget.class).process();
        }

        @Override
        public boolean connect(@NonNull DeviceConnector connector, @Nullable String password) {
            return poll(Expectation.Connect.class).process(connector, password);
        }

        @Override
        public boolean disconnect() {
            return poll(Expectation.Disconnect.class).process();
        }

        private <E extends Expectation> E queue(@NonNull E expectation) {
            mExpectations.add(expectation);
            return expectation;
        }

        @SuppressWarnings("unchecked")
        private <E extends Expectation> E poll(@NonNull Class<E> expectationClass) {
            assertThat(mExpectations.peek(), instanceOf(expectationClass));
            return (E) mExpectations.poll();
        }
    }
}
