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
import com.parrot.drone.groundsdk.device.peripheral.SystemInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.utility.FirmwareBlackList;
import com.parrot.drone.groundsdk.internal.utility.FirmwareStore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

public class SkyControllerSystemInfoTests extends ArsdkEngineTestBase {

    private final FirmwareStore mMockFirmwareStore = Mockito.mock(FirmwareStore.class);

    private final FirmwareBlackList mMockFirmwareBlackList = Mockito.mock(FirmwareBlackList.class);

    private RemoteControlCore mRemoteControl;

    private SystemInfo mSysInfo;

    private int mSysInfoChangeCnt;

    @Override
    public void setUp() {
        super.setUp();

        mUtilities.registerUtility(FirmwareStore.class, mMockFirmwareStore);
        mUtilities.registerUtility(FirmwareBlackList.class, mMockFirmwareBlackList);
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

        mSysInfo = mRemoteControl.getPeripheralStore().get(mMockSession, SystemInfo.class);
        mRemoteControl.getPeripheralStore().registerObserver(SystemInfo.class, () -> {
            mSysInfo = mRemoteControl.getPeripheralStore().get(mMockSession, SystemInfo.class);
            mSysInfoChangeCnt++;
        });

        mSysInfoChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mSysInfo, nullValue());
        assertThat(mSysInfoChangeCnt, is(0));

        connectRemoteControl(mRemoteControl, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock receiving settings from drone so that the component is persisted
                ArsdkEncoder.encodeSkyctrlSettingsStateProductVersionChanged("soft", "hard")));

        assertThat(mSysInfo, notNullValue());
        assertThat(mSysInfoChangeCnt, is(1));

        disconnectRemoteControl(mRemoteControl, 1);

        // should still be available when the remote control is disconnected
        assertThat(mSysInfo, notNullValue());
        assertThat(mSysInfoChangeCnt, is(1));

        // should be unavailable after forgetting the remote control
        mRemoteControl.forget();
        assertThat(mSysInfo, nullValue());
        assertThat(mSysInfoChangeCnt, is(2));
    }

    @Test
    public void testFactoryReset() {
        connectRemoteControl(mRemoteControl, 1);

        assertThat(mSysInfoChangeCnt, is(1));
        assertThat(mSysInfo.isFactoryResetInProgress(), is(false));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.skyctrlFactoryReset(), false));
        assertThat(mSysInfo.factoryReset(), is(true));

        assertThat(mSysInfoChangeCnt, is(2));
        assertThat(mSysInfo.isFactoryResetInProgress(), is(true));
    }

    @Test
    public void testResetSettings() {
        connectRemoteControl(mRemoteControl, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock receiving settings from drone so that the component is persisted
                ArsdkEncoder.encodeSkyctrlSettingsStateProductVersionChanged("soft", "hard")));

        assertThat(mSysInfoChangeCnt, is(1));
        assertThat(mSysInfo.isResetSettingsInProgress(), is(false));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.skyctrlSettingsReset(), false));
        assertThat(mSysInfo.resetSettings(), is(true));

        assertThat(mSysInfoChangeCnt, is(2));
        assertThat(mSysInfo.isResetSettingsInProgress(), is(true));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSettingsStateResetChanged());
        assertThat(mSysInfoChangeCnt, is(3));
        assertThat(mSysInfo.isResetSettingsInProgress(), is(false));

        // check disconnection clears ongoing reset settings flag
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.skyctrlSettingsReset(), false));
        assertThat(mSysInfo.resetSettings(), is(true));

        assertThat(mSysInfoChangeCnt, is(4));
        assertThat(mSysInfo.isResetSettingsInProgress(), is(true));

        disconnectRemoteControl(mRemoteControl, 1);

        assertThat(mSysInfoChangeCnt, is(5));
        assertThat(mSysInfo.isResetSettingsInProgress(), is(false));
    }

    @Test
    public void testSystemInfoValues() {
        connectRemoteControl(mRemoteControl, 1);

        assertThat(mSysInfoChangeCnt, is(1));
        assertThat(mSysInfo.getFirmwareVersion(), emptyString());
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(false));
        assertThat(mSysInfo.getHardwareVersion(), emptyString());
        assertThat(mSysInfo.getSerialNumber(), emptyString());
        assertThat(mSysInfo.getCpuIdentifier(), emptyString());
        assertThat(mSysInfo.getBoardIdentifier(), emptyString());

        // receive firmware and hardware version
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSettingsStateProductVersionChanged("1.2.3-beta1",
                "HW11"));
        assertThat(mSysInfoChangeCnt, is(2));
        assertThat(mSysInfo.getFirmwareVersion(), is("1.2.3-beta1"));
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(false));
        assertThat(mSysInfo.getHardwareVersion(), is("HW11"));
        assertThat(mSysInfo.getSerialNumber(), emptyString());
        assertThat(mSysInfo.getCpuIdentifier(), emptyString());
        assertThat(mSysInfo.getBoardIdentifier(), emptyString());

        // change firmware version to a blacklisted version
        FirmwareVersion firmwareVersion = FirmwareVersion.parse("1.0.0");
        assert firmwareVersion != null;
        FirmwareIdentifier firmwareIdentifier =
                new FirmwareIdentifier(RemoteControl.Model.SKY_CONTROLLER_3, firmwareVersion);
        when(mMockFirmwareBlackList.isFirmwareBlacklisted(firmwareIdentifier)).thenReturn(true);
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSettingsStateProductVersionChanged("1.0.0",
                "HW11"));
        assertThat(mSysInfoChangeCnt, is(3));
        assertThat(mSysInfo.getFirmwareVersion(), is("1.0.0"));
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(true));
        assertThat(mSysInfo.getHardwareVersion(), is("HW11"));
        assertThat(mSysInfo.getSerialNumber(), emptyString());
        assertThat(mSysInfo.getCpuIdentifier(), emptyString());
        assertThat(mSysInfo.getBoardIdentifier(), emptyString());

        // receive serial number
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSettingsStateProductSerialChanged("serial1"));
        assertThat(mSysInfoChangeCnt, is(4));
        assertThat(mSysInfo.getFirmwareVersion(), is("1.0.0"));
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(true));
        assertThat(mSysInfo.getHardwareVersion(), is("HW11"));
        assertThat(mSysInfo.getSerialNumber(), is("serial1"));
        assertThat(mSysInfo.getCpuIdentifier(), emptyString());
        assertThat(mSysInfo.getBoardIdentifier(), emptyString());

        // disconnect drone and reset engine
        disconnectRemoteControl(mRemoteControl, 1);
        resetEngine();

        // check system info data still available
        assertThat(mSysInfoChangeCnt, is(0));
        assertThat(mSysInfo.getFirmwareVersion(), is("1.0.0"));
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(true));
        assertThat(mSysInfo.getHardwareVersion(), is("HW11"));
        assertThat(mSysInfo.getSerialNumber(), is("serial1"));
        assertThat(mSysInfo.getCpuIdentifier(), emptyString());
        assertThat(mSysInfo.getBoardIdentifier(), emptyString());

        // TODO : add cpu identifier and board identifier tests once there are events for them.
    }
}
