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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.skycontroller;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.Copilot;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureSkyctrl;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class SkyControllerCopilotTests extends ArsdkEngineTestBase {

    private RemoteControlCore mRemoteControl;

    private Copilot mCopilot;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        setupRemoteControl();
    }

    @Override
    protected void resetEngine() {
        super.resetEngine();
        setupRemoteControl();
    }

    private void setupRemoteControl() {
        mMockArsdkCore.addDevice("456", RemoteControl.Model.SKY_CONTROLLER_3.id(), "RC", 1, Backend.TYPE_MUX);
        mRemoteControl = mRCStore.get("456");
        assert mRemoteControl != null;

        mCopilot = mRemoteControl.getPeripheralStore().get(mMockSession, Copilot.class);
        mRemoteControl.getPeripheralStore().registerObserver(Copilot.class, () -> {
            mCopilot = mRemoteControl.getPeripheralStore().get(mMockSession, Copilot.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when remote control has never been connected
        assertThat(mChangeCnt, is(0));
        assertThat(mCopilot, nullValue());

        // connect remote control
        connectRemoteControl(mRemoteControl, 1);
        assertThat(mCopilot, notNullValue());
        assertThat(mChangeCnt, is(1));

        // disconnect remote control
        disconnectRemoteControl(mRemoteControl, 1);
        assertThat(mCopilot, notNullValue());
        assertThat(mChangeCnt, is(1));

        // forget remote control
        mRemoteControl.forget();

        assertThat(mChangeCnt, is(2));
        assertThat(mCopilot, nullValue());
    }

    @Test
    public void testMode() {
        // connect remote control
        connectRemoteControl(mRemoteControl, 1);

        // check initial value
        assertThat(mChangeCnt, is(1));
        assertThat(mCopilot.source(), enumSettingIsUpToDateAt(Copilot.Source.REMOTE_CONTROL));

        // change value from api
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.skyctrlCoPilotingSetPilotingSource(
                ArsdkFeatureSkyctrl.CopilotingSetpilotingsourceSource.CONTROLLER)));
        mCopilot.source().setValue(Copilot.Source.APPLICATION);
        assertThat(mChangeCnt, is(2));
        assertThat(mCopilot.source(), enumSettingIsUpdatingTo(Copilot.Source.APPLICATION));

        // update value from backend
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCoPilotingStatePilotingSource(
                ArsdkFeatureSkyctrl.CopilotingstatePilotingsourceSource.CONTROLLER));
        assertThat(mChangeCnt, is(3));
        assertThat(mCopilot.source(), enumSettingIsUpToDateAt(Copilot.Source.APPLICATION));

        // disconnect
        disconnectRemoteControl(mRemoteControl, 1);
        resetEngine();

        // check setting didn't change
        assertThat(mChangeCnt, is(0));
        assertThat(mCopilot.source(), enumSettingIsUpToDateAt(Copilot.Source.APPLICATION));

        // change setting offline
        mCopilot.source().setValue(Copilot.Source.REMOTE_CONTROL);
        assertThat(mChangeCnt, is(1));
        assertThat(mCopilot.source(), enumSettingIsUpToDateAt(Copilot.Source.REMOTE_CONTROL));

        // reconnect
        connectRemoteControl(mRemoteControl, 1, () -> {
            // received current remote control setting differs from what is saved
            mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlCoPilotingStatePilotingSource(
                    ArsdkFeatureSkyctrl.CopilotingstatePilotingsourceSource.CONTROLLER));
            // connect should send the saved setting
            mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.skyctrlCoPilotingSetPilotingSource(
                    ArsdkFeatureSkyctrl.CopilotingSetpilotingsourceSource.SKYCONTROLLER), true));
        });

        // check setting didn't change
        assertThat(mChangeCnt, is(1));
        assertThat(mCopilot.source(), enumSettingIsUpToDateAt(Copilot.Source.REMOTE_CONTROL));
    }

    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectRemoteControl(mRemoteControl, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mCopilot.source(), allOf(
                enumSettingValueIs(Copilot.Source.REMOTE_CONTROL),
                settingIsUpToDate()));

        // mock user modifies settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.skyctrlCoPilotingSetPilotingSource(
                ArsdkFeatureSkyctrl.CopilotingSetpilotingsourceSource.CONTROLLER)));
        mCopilot.source().setValue(Copilot.Source.APPLICATION);

        assertThat(mChangeCnt, is(2));
        assertThat(mCopilot.source(), allOf(
                enumSettingValueIs(Copilot.Source.APPLICATION),
                settingIsUpdating()));

        // disconnect
        disconnectRemoteControl(mRemoteControl, 1);

        // setting should be updated to user value
        assertThat(mChangeCnt, is(3));

        assertThat(mCopilot.source(), allOf(
                enumSettingValueIs(Copilot.Source.APPLICATION),
                settingIsUpToDate()));
    }
}
