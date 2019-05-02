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
import com.parrot.drone.groundsdk.device.peripheral.PreciseHome;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeaturePreciseHome;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import java.util.EnumSet;

import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingSupports;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiPreciseHomeTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private PreciseHome mPreciseHome;

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

        mPreciseHome = mDrone.getPeripheralStore().get(mMockSession, PreciseHome.class);
        mDrone.getPeripheralStore().registerObserver(PreciseHome.class, () -> {
            mPreciseHome = mDrone.getPeripheralStore().get(mMockSession, PreciseHome.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when drone has never been connected
        assertThat(mChangeCnt, is(0));
        assertThat(mPreciseHome, nullValue());

        // connect drone
        connectDrone(mDrone, 1);

        // component should not be published if the drone does not send supported modes
        assertThat(mChangeCnt, is(0));
        assertThat(mPreciseHome, nullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is not published
        assertThat(mChangeCnt, is(0));
        assertThat(mPreciseHome, nullValue());

        // connect drone, receiving supported modes
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodePreciseHomeCapabilities(
                ArsdkFeaturePreciseHome.Mode.toBitField(ArsdkFeaturePreciseHome.Mode.values()))));

        // component should be published
        assertThat(mChangeCnt, is(1));
        assertThat(mPreciseHome, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is now persisted
        assertThat(mChangeCnt, is(1));
        assertThat(mPreciseHome, notNullValue());

        // connect drone, not receiving supported modes
        connectDrone(mDrone, 1);

        // component should be unpublished
        assertThat(mChangeCnt, is(2));
        assertThat(mPreciseHome, nullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is not published
        assertThat(mChangeCnt, is(2));
        assertThat(mPreciseHome, nullValue());

        // connect drone, receiving supported modes
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodePreciseHomeCapabilities(
                ArsdkFeaturePreciseHome.Mode.toBitField(ArsdkFeaturePreciseHome.Mode.values()))));

        // component should be published
        assertThat(mChangeCnt, is(3));
        assertThat(mPreciseHome, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is now persisted
        assertThat(mChangeCnt, is(3));
        assertThat(mPreciseHome, notNullValue());

        // forget drone
        mDrone.forget();

        // component should be unpublished
        assertThat(mChangeCnt, is(4));
        assertThat(mPreciseHome, nullValue());
    }

    @Test
    public void testMode() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodePreciseHomeCapabilities(
                        ArsdkFeaturePreciseHome.Mode.toBitField(ArsdkFeaturePreciseHome.Mode.values())),
                ArsdkEncoder.encodePreciseHomeMode(ArsdkFeaturePreciseHome.Mode.DISABLED)));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(PreciseHome.Mode.class)),
                enumSettingValueIs(PreciseHome.Mode.DISABLED),
                settingIsUpToDate()));

        // change mode
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.preciseHomeSetMode(ArsdkFeaturePreciseHome.Mode.STANDARD)));
        mPreciseHome.mode().setValue(PreciseHome.Mode.STANDARD);

        assertThat(mChangeCnt, is(2));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(PreciseHome.Mode.class)),
                enumSettingValueIs(PreciseHome.Mode.STANDARD),
                settingIsUpdating()));

        // mock drone ack
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodePreciseHomeMode(
                ArsdkFeaturePreciseHome.Mode.STANDARD));

        assertThat(mChangeCnt, is(3));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(PreciseHome.Mode.class)),
                enumSettingValueIs(PreciseHome.Mode.STANDARD),
                settingIsUpToDate()));

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check still in standard mode
        assertThat(mChangeCnt, is(0));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(PreciseHome.Mode.class)),
                enumSettingValueIs(PreciseHome.Mode.STANDARD),
                settingIsUpToDate()));

        // change mode offline
        mPreciseHome.mode().setValue(PreciseHome.Mode.DISABLED);

        assertThat(mChangeCnt, is(1));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(PreciseHome.Mode.class)),
                enumSettingValueIs(PreciseHome.Mode.DISABLED),
                settingIsUpToDate()));

        // reconnect
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1,
                        ArsdkEncoder.encodePreciseHomeCapabilities(
                                ArsdkFeaturePreciseHome.Mode.toBitField(ArsdkFeaturePreciseHome.Mode.values())),
                        ArsdkEncoder.encodePreciseHomeMode(ArsdkFeaturePreciseHome.Mode.STANDARD))
                // disabled mode should be sent to drone
                .expect(new Expectation.Command(1,
                        ExpectedCmd.preciseHomeSetMode(ArsdkFeaturePreciseHome.Mode.DISABLED))));

        mMockArsdkCore.assertNoExpectation();
        assertThat(mChangeCnt, is(1));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingSupports(EnumSet.allOf(PreciseHome.Mode.class)),
                enumSettingValueIs(PreciseHome.Mode.DISABLED),
                settingIsUpToDate()));
    }

    @Test
    public void testState() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodePreciseHomeCapabilities(
                        ArsdkFeaturePreciseHome.Mode.toBitField(ArsdkFeaturePreciseHome.Mode.values()))));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mPreciseHome.state(), is(PreciseHome.State.UNAVAILABLE));

        // mock drone sends AVAILABLE value
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodePreciseHomeState(
                ArsdkFeaturePreciseHome.State.AVAILABLE));

        assertThat(mChangeCnt, is(2));
        assertThat(mPreciseHome.state(), is(PreciseHome.State.AVAILABLE));

        // mock drone sends UNAVAILABLE value
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodePreciseHomeState(
                ArsdkFeaturePreciseHome.State.UNAVAILABLE));

        assertThat(mChangeCnt, is(3));
        assertThat(mPreciseHome.state(), is(PreciseHome.State.UNAVAILABLE));

        // mock drone sends ACTIVE value
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodePreciseHomeState(
                ArsdkFeaturePreciseHome.State.ACTIVE));

        assertThat(mChangeCnt, is(4));
        assertThat(mPreciseHome.state(), is(PreciseHome.State.ACTIVE));

        // disconnect
        disconnectDrone(mDrone, 1);

        // value should be reset to UNAVAILABLE
        assertThat(mChangeCnt, is(5));
        assertThat(mPreciseHome.state(), is(PreciseHome.State.UNAVAILABLE));
    }

    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodePreciseHomeCapabilities(
                        ArsdkFeaturePreciseHome.Mode.toBitField(ArsdkFeaturePreciseHome.Mode.values())),
                ArsdkEncoder.encodePreciseHomeMode(ArsdkFeaturePreciseHome.Mode.DISABLED)));

        assertThat(mChangeCnt, is(1));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingValueIs(PreciseHome.Mode.DISABLED),
                settingIsUpToDate()));

        // mock user modifies settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.preciseHomeSetMode(
                ArsdkFeaturePreciseHome.Mode.STANDARD)));
        mPreciseHome.mode().setValue(PreciseHome.Mode.STANDARD);

        assertThat(mChangeCnt, is(2));
        assertThat(mPreciseHome.mode(), allOf(
                enumSettingValueIs(PreciseHome.Mode.STANDARD),
                settingIsUpdating()));

        // disconnect
        disconnectDrone(mDrone, 1);

        // setting should be updated to user value
        assertThat(mChangeCnt, is(3));

        assertThat(mPreciseHome.mode(), allOf(
                enumSettingValueIs(PreciseHome.Mode.STANDARD),
                settingIsUpToDate()));

        // test other values are reset as they should
        assertThat(mPreciseHome.state(), is(PreciseHome.State.UNAVAILABLE));
    }
}
