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

package com.parrot.drone.groundsdk;

import androidx.annotation.NonNull;

import com.parrot.drone.groundsdk.device.DeviceConnector;
import com.parrot.drone.groundsdk.device.DeviceState;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public final class DeviceStateMatcher {

    public static Matcher<DeviceState> stateIs(DeviceState.ConnectionState state) {
        return new FeatureMatcher<DeviceState, DeviceState.ConnectionState>(equalTo(state),
                "ConnectionState", "value") {

            @Override
            protected DeviceState.ConnectionState featureValueOf(DeviceState actual) {
                return actual.getConnectionState();
            }
        };
    }

    public static Matcher<DeviceState> causeIs(DeviceState.ConnectionStateCause cause) {
        return new FeatureMatcher<DeviceState, DeviceState.ConnectionStateCause>(equalTo(cause),
                "ConnectionStateCause", "value") {

            @Override
            protected DeviceState.ConnectionStateCause featureValueOf(DeviceState actual) {
                return actual.getConnectionStateCause();
            }
        };
    }

    public static Matcher<DeviceState> hasNoConnectors() {
        return new FeatureMatcher<DeviceState, Boolean>(equalTo(true), "hasNoConnectors", "hasNoConnectors") {

            @Override
            protected Boolean featureValueOf(DeviceState actual) {
                return actual.getConnectors().length == 0;
            }
        };
    }

    public static Matcher<DeviceState> hasConnectors(@NonNull DeviceConnector... connectors) {
        return new FeatureMatcher<DeviceState, DeviceConnector[]>(arrayContainingInAnyOrder(connectors),
                "hasConnectors", "connectors") {

            @Override
            protected DeviceConnector[] featureValueOf(DeviceState actual) {
                return actual.getConnectors();
            }
        };
    }

    public static Matcher<DeviceState> hasNoActiveConnector() {
        return new FeatureMatcher<DeviceState, DeviceConnector>(nullValue(), "hasNoActiveConnector",
                "activeConnector") {

            @Override
            protected DeviceConnector featureValueOf(DeviceState actual) {
                return actual.getActiveConnector();
            }
        };
    }

    public static Matcher<DeviceState> activeConnector(@NonNull DeviceConnector connector) {
        return new FeatureMatcher<DeviceState, DeviceConnector>(equalTo(connector), "activeConnector",
                "activeConnector") {

            @Override
            protected DeviceConnector featureValueOf(DeviceState actual) {
                return actual.getActiveConnector();
            }
        };
    }

    public static Matcher<DeviceState> canBeForgotten(boolean val) {
        return new FeatureMatcher<DeviceState, Boolean>(equalTo(val), "canBeForgotten", "value") {

            @Override
            protected Boolean featureValueOf(DeviceState actual) {
                return actual.canBeForgotten();
            }
        };
    }

    public static Matcher<DeviceState> canBeConnected(boolean val) {
        return new FeatureMatcher<DeviceState, Boolean>(equalTo(val), "canBeConnected", "value") {

            @Override
            protected Boolean featureValueOf(DeviceState actual) {
                return actual.canBeConnected();
            }
        };
    }

    public static Matcher<DeviceState> canBeDisconnected(boolean val) {
        return new FeatureMatcher<DeviceState, Boolean>(equalTo(val), "canBeDisconnected", "value") {

            @Override
            protected Boolean featureValueOf(DeviceState actual) {
                return actual.canBeDisconnected();
            }
        };
    }

    @SuppressWarnings("ConstantConditions")
    static Matcher<DeviceState> matchesState(@NonNull DeviceState other) {
        return Matchers.allOf(stateIs(other.getConnectionState()), causeIs(other.getConnectionStateCause()),
                hasConnectors(other.getConnectors()), activeConnector(other.getActiveConnector()),
                canBeConnected(other.canBeConnected()), canBeDisconnected(other.canBeDisconnected()),
                canBeForgotten(other.canBeForgotten()));
    }

    private DeviceStateMatcher() {
    }
}
