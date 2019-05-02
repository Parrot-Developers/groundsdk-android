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
import com.parrot.drone.groundsdk.device.peripheral.SystemInfo;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareVersion;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AnafiSystemInfoTests extends ArsdkEngineTestBase {

    private final FirmwareStore mMockFirmwareStore = Mockito.mock(FirmwareStore.class);

    private final FirmwareBlackList mMockFirmwareBlackList = Mockito.mock(FirmwareBlackList.class);

    private DroneCore mDrone;

    private SystemInfo mSysInfo;

    private int mSysInfoChangeCnt;

    @Override
    public void setUp() {
        super.setUp();

        mUtilities.registerUtility(FirmwareStore.class, mMockFirmwareStore);
        mUtilities.registerUtility(FirmwareBlackList.class, mMockFirmwareBlackList);
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

        mSysInfo = mDrone.getPeripheralStore().get(mMockSession, SystemInfo.class);
        mDrone.getPeripheralStore().registerObserver(SystemInfo.class, () -> {
            mSysInfo = mDrone.getPeripheralStore().get(mMockSession, SystemInfo.class);
            mSysInfoChangeCnt++;
        });

        mSysInfoChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mSysInfo, nullValue());
        assertThat(mSysInfoChangeCnt, is(0));

        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock receiving settings from drone so that the component is persisted
                ArsdkEncoder.encodeCommonSettingsStateProductVersionChanged("soft", "hard")));

        assertThat(mSysInfo, notNullValue());
        assertThat(mSysInfoChangeCnt, is(1));

        disconnectDrone(mDrone, 1);

        // should still be available when the drone is disconnected
        assertThat(mSysInfo, notNullValue());
        assertThat(mSysInfoChangeCnt, is(1));

        // should be unavailable after forgetting the drone
        mDrone.forget();
        assertThat(mSysInfo, nullValue());
        assertThat(mSysInfoChangeCnt, is(2));
    }

    @Test
    public void testFactoryReset() {
        connectDrone(mDrone, 1);

        assertThat(mSysInfoChangeCnt, is(1));
        assertThat(mSysInfo.isFactoryResetInProgress(), is(false));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonFactoryReset(), false));
        assertThat(mSysInfo.factoryReset(), is(true));

        assertThat(mSysInfoChangeCnt, is(2));
        assertThat(mSysInfo.isFactoryResetInProgress(), is(true));
    }

    @Test
    public void testSystemInfoValues() {
        connectDrone(mDrone, 1);

        assertThat(mSysInfoChangeCnt, is(1));
        assertThat(mSysInfo.getFirmwareVersion(), emptyString());
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(false));
        assertThat(mSysInfo.getHardwareVersion(), emptyString());
        assertThat(mSysInfo.getSerialNumber(), emptyString());
        assertThat(mSysInfo.getCpuIdentifier(), emptyString());
        assertThat(mSysInfo.getBoardIdentifier(), emptyString());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateProductVersionChanged("1.2.3-rc2",
                "HW11"));
        assertThat(mSysInfoChangeCnt, is(2));
        assertThat(mSysInfo.getFirmwareVersion(), is("1.2.3-rc2"));
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(false));
        assertThat(mSysInfo.getHardwareVersion(), is("HW11"));
        assertThat(mSysInfo.getSerialNumber(), emptyString());
        assertThat(mSysInfo.getCpuIdentifier(), emptyString());
        assertThat(mSysInfo.getBoardIdentifier(), emptyString());

        // change firmware version to a blacklisted version
        FirmwareVersion firmwareVersion = FirmwareVersion.parse("1.2.3-rc3");
        assert firmwareVersion != null;
        FirmwareIdentifier firmwareIdentifier = new FirmwareIdentifier(Drone.Model.ANAFI_4K, firmwareVersion);
        when(mMockFirmwareBlackList.isFirmwareBlacklisted(firmwareIdentifier)).thenReturn(true);
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateProductVersionChanged("1.2.3-rc3",
                "HW11"));
        assertThat(mSysInfoChangeCnt, is(3));
        assertThat(mSysInfo.getFirmwareVersion(), is("1.2.3-rc3"));
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(true));
        assertThat(mSysInfo.getHardwareVersion(), is("HW11"));
        assertThat(mSysInfo.getSerialNumber(), emptyString());
        assertThat(mSysInfo.getCpuIdentifier(), emptyString());
        assertThat(mSysInfo.getBoardIdentifier(), emptyString());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateProductSerialLowChanged("low1"));
        assertThat(mSysInfoChangeCnt, is(3));
        assertThat(mSysInfo.getSerialNumber(), emptyString());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateProductSerialHighChanged("high1"));
        assertThat(mSysInfoChangeCnt, is(4));
        assertThat(mSysInfo.getFirmwareVersion(), is("1.2.3-rc3"));
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(true));
        assertThat(mSysInfo.getHardwareVersion(), is("HW11"));
        assertThat(mSysInfo.getSerialNumber(), is("high1low1"));
        assertThat(mSysInfo.getCpuIdentifier(), emptyString());
        assertThat(mSysInfo.getBoardIdentifier(), emptyString());

        // check receiving high serial first, then low, is also working
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateProductSerialHighChanged("high2"));
        assertThat(mSysInfoChangeCnt, is(4));
        assertThat(mSysInfo.getSerialNumber(), is("high1low1"));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateProductSerialLowChanged("low2"));
        assertThat(mSysInfoChangeCnt, is(5));
        assertThat(mSysInfo.getFirmwareVersion(), is("1.2.3-rc3"));
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(true));
        assertThat(mSysInfo.getHardwareVersion(), is("HW11"));
        assertThat(mSysInfo.getSerialNumber(), is("high2low2"));
        assertThat(mSysInfo.getCpuIdentifier(), emptyString());
        assertThat(mSysInfo.getBoardIdentifier(), emptyString());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateCPUID("P7XXXX"));
        assertThat(mSysInfoChangeCnt, is(6));
        assertThat(mSysInfo.getFirmwareVersion(), is("1.2.3-rc3"));
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(true));
        assertThat(mSysInfo.getHardwareVersion(), is("HW11"));
        assertThat(mSysInfo.getSerialNumber(), is("high2low2"));
        assertThat(mSysInfo.getCpuIdentifier(), is("P7XXXX"));
        assertThat(mSysInfo.getBoardIdentifier(), emptyString());

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateBoardIdChanged("board id"));
        assertThat(mSysInfoChangeCnt, is(7));
        assertThat(mSysInfo.getFirmwareVersion(), is("1.2.3-rc3"));
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(true));
        assertThat(mSysInfo.getHardwareVersion(), is("HW11"));
        assertThat(mSysInfo.getSerialNumber(), is("high2low2"));
        assertThat(mSysInfo.getCpuIdentifier(), is("P7XXXX"));
        assertThat(mSysInfo.getBoardIdentifier(), is("board id"));

        // disconnect drone and reset engine
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check system info data still available
        assertThat(mSysInfoChangeCnt, is(0));
        assertThat(mSysInfo.getFirmwareVersion(), is("1.2.3-rc3"));
        assertThat(mSysInfo.isFirmwareBlacklisted(), is(true));
        assertThat(mSysInfo.getHardwareVersion(), is("HW11"));
        assertThat(mSysInfo.getSerialNumber(), is("high2low2"));
        assertThat(mSysInfo.getCpuIdentifier(), is("P7XXXX"));
        assertThat(mSysInfo.getBoardIdentifier(), is("board id"));
    }

    @Test
    public void testResetSettings() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock receiving settings from drone so that the component is persisted
                ArsdkEncoder.encodeCommonSettingsStateProductVersionChanged("soft", "hard")));

        assertThat(mSysInfoChangeCnt, is(1));
        assertThat(mSysInfo.isResetSettingsInProgress(), is(false));

        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonSettingsReset(), false));
        assertThat(mSysInfo.resetSettings(), is(true));

        assertThat(mSysInfoChangeCnt, is(2));
        assertThat(mSysInfo.isResetSettingsInProgress(), is(true));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonSettingsStateResetChanged());
        assertThat(mSysInfoChangeCnt, is(3));
        assertThat(mSysInfo.isResetSettingsInProgress(), is(false));

        // check disconnection clears ongoing reset settings flag
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.commonSettingsReset(), false));
        assertThat(mSysInfo.resetSettings(), is(true));

        assertThat(mSysInfoChangeCnt, is(4));
        assertThat(mSysInfo.isResetSettingsInProgress(), is(true));

        disconnectDrone(mDrone, 1);

        assertThat(mSysInfoChangeCnt, is(5));
        assertThat(mSysInfo.isResetSettingsInProgress(), is(false));
    }

    @Test
    public void testBlackListMonitor() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                // mock receiving settings from drone so that the component is persisted
                ArsdkEncoder.encodeCommonSettingsStateProductVersionChanged("soft", "hard")));
        assertThat(mSysInfoChangeCnt, is(1));

        verify(mMockFirmwareBlackList, times(1)).monitorWith(Mockito.any());
        verify(mMockFirmwareBlackList, times(0)).disposeMonitor(Mockito.any());

        disconnectDrone(mDrone, 1);
        assertThat(mSysInfoChangeCnt, is(1));

        verify(mMockFirmwareBlackList, times(1)).monitorWith(Mockito.any());
        verify(mMockFirmwareBlackList, times(0)).disposeMonitor(Mockito.any());

        mDrone.forget();
        assertThat(mSysInfoChangeCnt, is(2));

        verify(mMockFirmwareBlackList, times(1)).monitorWith(Mockito.any());
        verify(mMockFirmwareBlackList, times(1)).disposeMonitor(Mockito.any());
    }
}
