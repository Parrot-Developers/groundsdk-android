/*
 *     Copyright (C) 2020 Parrot Drones SAS
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
import com.parrot.drone.groundsdk.device.peripheral.LogControl;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSecurityEdition;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import static com.parrot.drone.sdkcore.arsdk.ArsdkEncoder.encodeSecurityEditionCapabilities;
import static com.parrot.drone.sdkcore.arsdk.ArsdkEncoder.encodeSecurityEditionLogStorageState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiLogControlTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private LogControl mLogControl;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();

        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mLogControl = mDrone.getPeripheralStore().get(mMockSession, LogControl.class);
        mDrone.getPeripheralStore().registerObserver(LogControl.class, () -> {
            mLogControl = mDrone.getPeripheralStore().get(mMockSession, LogControl.class);
            mChangeCnt++;

        });
        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when drone has never been connected
        assertThat(mChangeCnt, is(0));
        assertThat(mLogControl, nullValue());

        // Connect drone with no capabilities received
        // connect drone, mocking receiving online only parameters, so something changes on disconnection
        connectDrone(mDrone, 1);

        // component should not be published if the drone does not send supported capabilities
        assertThat(mChangeCnt, is(0));
        assertThat(mLogControl, nullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // nothing should change, component is not published
        assertThat(mChangeCnt, is(0));
        assertThat(mLogControl, nullValue());

        // Connect drone with capabilities received

        // connect drone, receiving supported capabilities
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(
                1,
                encodeSecurityEditionCapabilities(
                        ArsdkFeatureSecurityEdition.SupportedCapabilities.toBitField(
                                ArsdkFeatureSecurityEdition.SupportedCapabilities.values()
                        )
                ))
        );

        // component should be published
        assertThat(mChangeCnt, is(1));
        assertThat(mLogControl, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        // component should be unpublished
        assertThat(mChangeCnt, is(2));
        assertThat(mLogControl, nullValue());
    }

    @Test
    public void testDeactivateLogs() {
        // Connecting drone supporting Logs deactivation
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                encodeSecurityEditionCapabilities(
                        ArsdkFeatureSecurityEdition.SupportedCapabilities.toBitField(
                                ArsdkFeatureSecurityEdition.SupportedCapabilities.values()))));

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mLogControl.areLogsEnabled(), is(true));
        assertThat(mLogControl.canDeactivateLogs(), is(true));

        // Deactivate logs when they are enabled send command
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.securityEditionDeactivateLogs()));
        mLogControl.deactivateLogs();

        assertThat(mChangeCnt, is(1));
        assertThat(mLogControl.areLogsEnabled(), is(true));

        // Drone update logs state
        mMockArsdkCore.commandReceived(1,
                encodeSecurityEditionLogStorageState(ArsdkFeatureSecurityEdition.LogStorageState.DISABLED));

        // Deactivate logs when they already are disabled do nothing
        assertThat(mChangeCnt, is(2));
        assertThat(mLogControl.areLogsEnabled(), is(false));

        mLogControl.deactivateLogs();

        // Drone update logs state
        mMockArsdkCore.commandReceived(1,
                encodeSecurityEditionLogStorageState(ArsdkFeatureSecurityEdition.LogStorageState.ENABLED));
        assertThat(mChangeCnt, is(3));
        assertThat(mLogControl.areLogsEnabled(), is(true));

        disconnectDrone(mDrone, 1);

        mMockArsdkCore.assertNoExpectation();
    }
}
