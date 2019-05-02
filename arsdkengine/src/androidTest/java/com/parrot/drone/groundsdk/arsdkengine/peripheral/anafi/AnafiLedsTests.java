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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.Leds;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureLeds;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsDisabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabled;
import static com.parrot.drone.groundsdk.BooleanSettingMatcher.booleanSettingIsEnabling;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiLedsTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private Leds mLeds;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        setupDrone();
    }

    @Override
    protected void resetEngine() {
        super.resetEngine();
        setupDrone();
    }

    private void setupDrone() {
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mLeds = mDrone.getPeripheralStore().get(mMockSession, Leds.class);
        mDrone.getPeripheralStore().registerObserver(Leds.class, () -> {
            mLeds = mDrone.getPeripheralStore().get(mMockSession, Leds.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when drone has never been connected
        assertThat(mChangeCnt, is(0));
        assertThat(mLeds, nullValue());

        // connect drone
        connectDrone(mDrone, 1);

        // component should not be published if the drone does not send supported capabilities
        assertThat(mChangeCnt, is(0));
        assertThat(mLeds, nullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is not published
        assertThat(mChangeCnt, is(0));
        assertThat(mLeds, nullValue());

        // connect drone, receiving supported capabilities
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeLedsCapabilities(
                ArsdkFeatureLeds.SupportedCapabilities.toBitField(ArsdkFeatureLeds.SupportedCapabilities.values()))));

        // component should be published
        assertThat(mChangeCnt, is(1));
        assertThat(mLeds, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is now persisted
        assertThat(mChangeCnt, is(1));
        assertThat(mLeds, notNullValue());

        // connect drone, not receiving supported modes
        connectDrone(mDrone, 1);

        // component should be unpublished
        assertThat(mChangeCnt, is(2));
        assertThat(mLeds, nullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is not published
        assertThat(mChangeCnt, is(2));
        assertThat(mLeds, nullValue());

        // connect drone, receiving supported capabilities
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeLedsCapabilities(
                ArsdkFeatureLeds.SupportedCapabilities.toBitField(ArsdkFeatureLeds.SupportedCapabilities.values()))));

        // component should be published
        assertThat(mChangeCnt, is(3));
        assertThat(mLeds, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is now persisted
        assertThat(mChangeCnt, is(3));
        assertThat(mLeds, notNullValue());

        // forget drone
        mDrone.forget();

        // component should be unpublished
        assertThat(mChangeCnt, is(4));
        assertThat(mLeds, nullValue());
    }

    @Test
    public void testState() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeLedsCapabilities(
                        ArsdkFeatureLeds.SupportedCapabilities.toBitField(
                                ArsdkFeatureLeds.SupportedCapabilities.values())),
                ArsdkEncoder.encodeLedsSwitchState(ArsdkFeatureLeds.SwitchState.OFF)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mLeds.state(), booleanSettingIsDisabled());

        // change switch state
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.ledsActivate()));
        mLeds.state().toggle();

        assertThat(mChangeCnt, is(2));
        assertThat(mLeds.state(), booleanSettingIsEnabling());

        // mock drone ack
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeLedsSwitchState(ArsdkFeatureLeds.SwitchState.ON));

        assertThat(mChangeCnt, is(3));
        assertThat(mLeds.state(), booleanSettingIsEnabled());

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check still enabled
        assertThat(mChangeCnt, is(0));
        assertThat(mLeds.state(), booleanSettingIsEnabled());

        // change mode offline
        mLeds.state().toggle();

        assertThat(mChangeCnt, is(1));
        assertThat(mLeds.state(), booleanSettingIsDisabled());

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1,
                        ArsdkEncoder.encodeLedsCapabilities(
                                ArsdkFeatureLeds.SupportedCapabilities.toBitField(
                                        ArsdkFeatureLeds.SupportedCapabilities.values())),
                        ArsdkEncoder.encodeLedsSwitchState(ArsdkFeatureLeds.SwitchState.ON))
                // leds deactivation should be sent to drone
                .expect(new Expectation.Command(1, ExpectedCmd.ledsDeactivate())));

        mMockArsdkCore.assertNoExpectation();
        assertThat(mChangeCnt, is(1));
        assertThat(mLeds.state(), booleanSettingIsDisabled());
    }

    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeLedsCapabilities(
                        ArsdkFeatureLeds.SupportedCapabilities.toBitField(
                                ArsdkFeatureLeds.SupportedCapabilities.values())),
                ArsdkEncoder.encodeLedsSwitchState(ArsdkFeatureLeds.SwitchState.OFF)));

        assertThat(mChangeCnt, is(1));
        assertThat(mLeds.state(), booleanSettingIsDisabled());

        // mock user modifies settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.ledsActivate()));
        mLeds.state().toggle();

        assertThat(mChangeCnt, is(2));
        assertThat(mLeds.state(), booleanSettingIsEnabling());

        // disconnect
        disconnectDrone(mDrone, 1);

        // setting should be updated to user value
        assertThat(mChangeCnt, is(3));

        assertThat(mLeds.state(), booleanSettingIsEnabled());
    }
}
