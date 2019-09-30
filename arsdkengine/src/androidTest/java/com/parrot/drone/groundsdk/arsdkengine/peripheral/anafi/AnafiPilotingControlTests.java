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
import com.parrot.drone.groundsdk.device.peripheral.PilotingControl;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeaturePilotingStyle;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import java.util.EnumSet;

import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingSupports;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiPilotingControlTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private PilotingControl mPilotingControl;

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

        mPilotingControl = mDrone.getPeripheralStore().get(mMockSession, PilotingControl.class);
        mDrone.getPeripheralStore().registerObserver(PilotingControl.class, () -> {
            mPilotingControl = mDrone.getPeripheralStore().get(mMockSession, PilotingControl.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mChangeCnt, is(0));
        assertThat(mPilotingControl, nullValue());

        // connect drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingControl, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingControl, nullValue());
    }

    @Test
    public void testModeAndValue() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodePilotingStyleCapabilities(
                        ArsdkFeaturePilotingStyle.Style.toBitField(
                                ArsdkFeaturePilotingStyle.Style.STANDARD,
                                ArsdkFeaturePilotingStyle.Style.CAMERA_OPERATED)))
                .commandReceived(1, ArsdkEncoder.encodePilotingStyleStyle(
                        ArsdkFeaturePilotingStyle.Style.STANDARD)));

        // initial values
        assertThat(mChangeCnt, is(1));
        assertThat(mPilotingControl, notNullValue());
        assertThat(mPilotingControl.behavior(), enumSettingSupports(
                EnumSet.of(PilotingControl.Behavior.STANDARD, PilotingControl.Behavior.CAMERA_OPERATED)));
        assertThat(mPilotingControl.behavior(), enumSettingIsUpToDateAt(PilotingControl.Behavior.STANDARD));

        // set behavior to CAMERA_OPERATED
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.pilotingStyleSetStyle(ArsdkFeaturePilotingStyle.Style.CAMERA_OPERATED)));
        mPilotingControl.behavior().setValue(PilotingControl.Behavior.CAMERA_OPERATED);
        assertThat(mChangeCnt, is(2));
        assertThat(mPilotingControl.behavior(), enumSettingIsUpdatingTo(PilotingControl.Behavior.CAMERA_OPERATED));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder
                .encodePilotingStyleStyle(ArsdkFeaturePilotingStyle.Style.CAMERA_OPERATED));
        assertThat(mChangeCnt, is(3));
        assertThat(mPilotingControl.behavior(), enumSettingIsUpToDateAt(PilotingControl.Behavior.CAMERA_OPERATED));

        // mock drone change to STANDARD
        mMockArsdkCore.commandReceived(1, ArsdkEncoder
                .encodePilotingStyleStyle(ArsdkFeaturePilotingStyle.Style.STANDARD));
        assertThat(mChangeCnt, is(4));
        assertThat(mPilotingControl.behavior(), enumSettingIsUpToDateAt(PilotingControl.Behavior.STANDARD));

        // disconnect drone
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(5));
        assertThat(mPilotingControl, nullValue());

        // connect drone
        connectDrone(mDrone, 1);

        // if drone does not send capabilities at connection, only STANDARD is supported
        assertThat(mChangeCnt, is(6));
        assertThat(mPilotingControl, notNullValue());
        assertThat(mPilotingControl.behavior(), enumSettingSupports(
                EnumSet.of(PilotingControl.Behavior.STANDARD)));
        assertThat(mPilotingControl.behavior(), enumSettingIsUpToDateAt(PilotingControl.Behavior.STANDARD));
    }
}
