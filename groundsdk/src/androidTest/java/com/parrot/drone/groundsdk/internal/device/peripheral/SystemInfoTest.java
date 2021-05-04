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

package com.parrot.drone.groundsdk.internal.device.peripheral;

import com.parrot.drone.groundsdk.device.peripheral.Peripheral;
import com.parrot.drone.groundsdk.device.peripheral.SystemInfo;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class SystemInfoTest {

    private MockComponentStore<Peripheral> mStore;

    private SystemInfoCore mSystemInfoImpl;

    private SystemInfo mSystemInfo;

    private Backend mBackend;

    private int mComponentChangeCnt;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mBackend = new Backend();
        mSystemInfoImpl = new SystemInfoCore(mStore, mBackend);
        mSystemInfo = mStore.get(SystemInfo.class);
        mStore.registerObserver(SystemInfo.class, () -> {
            mComponentChangeCnt++;
            mSystemInfo = mStore.get(SystemInfo.class);
        });
        mComponentChangeCnt = 0;
        mBackend.reset();
    }

    @Test
    public void testPublication() {
        assertThat(mSystemInfo, nullValue());
        assertThat(mComponentChangeCnt, is(0));

        mSystemInfoImpl.publish();
        assertThat(mSystemInfo, notNullValue());
        assertThat(mComponentChangeCnt, is(1));

        mSystemInfoImpl.unpublish();
        assertThat(mSystemInfo, nullValue());
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testFirmwareVersion() {
        mSystemInfoImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mSystemInfo.getFirmwareVersion(), emptyString());

        mSystemInfoImpl.updateFirmwareVersion("1.3.5-alpha7").notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mSystemInfo.getFirmwareVersion(), is("1.3.5-alpha7"));

        // check same version does not trigger a change
        mSystemInfoImpl.updateFirmwareVersion("1.3.5-alpha7").notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testFirmwareVersionBlacklisted() {
        mSystemInfoImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mSystemInfo.isFirmwareBlacklisted(), is(false));

        mSystemInfoImpl.updateFirmwareBlacklisted(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mSystemInfo.isFirmwareBlacklisted(), is(true));

        // check value does not trigger a change
        mSystemInfoImpl.updateFirmwareBlacklisted(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testIsUpdateRequired() {
        mSystemInfoImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mSystemInfo.isUpdateRequired(), is(false));

        mSystemInfoImpl.updateIsUpdateRequired(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mSystemInfo.isUpdateRequired(), is(true));

        // check value does not trigger a change
        mSystemInfoImpl.updateIsUpdateRequired(true).notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testHardwareVersion() {
        mSystemInfoImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mSystemInfo.getHardwareVersion(), emptyString());

        mSystemInfoImpl.updateHardwareVersion("HW42").notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mSystemInfo.getHardwareVersion(), is("HW42"));

        // check same version does not trigger a change
        mSystemInfoImpl.updateHardwareVersion("HW42").notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testSerialNumber() {
        mSystemInfoImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mSystemInfo.getSerialNumber(), emptyString());

        mSystemInfoImpl.updateSerial("SERIAL_NUMBER").notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mSystemInfo.getSerialNumber(), is("SERIAL_NUMBER"));

        // check same serial does not trigger a change
        mSystemInfoImpl.updateSerial("SERIAL_NUMBER").notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testCpuIdentifier() {
        mSystemInfoImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mSystemInfo.getCpuIdentifier(), emptyString());

        mSystemInfoImpl.updateCpuId("P42XXXXX").notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mSystemInfo.getCpuIdentifier(), is("P42XXXXX"));

        // check same id does not trigger a change
        mSystemInfoImpl.updateCpuId("P42XXXXX").notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testBoardIdentifier() {
        mSystemInfoImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mSystemInfo.getBoardIdentifier(), emptyString());

        mSystemInfoImpl.updateBoardId("board id").notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mSystemInfo.getBoardIdentifier(), is("board id"));

        // check same id does not trigger a change
        mSystemInfoImpl.updateBoardId("board id").notifyUpdated();
        assertThat(mComponentChangeCnt, is(2));
    }

    @Test
    public void testFactoryReset() {
        mSystemInfoImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mSystemInfo.isFactoryResetInProgress(), is(false));
        assertThat(mBackend.mFactoryResetCnt, is(0));

        assertThat(mSystemInfo.factoryReset(), is(true));
        assertThat(mBackend.mFactoryResetCnt, is(1));
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mSystemInfo.isFactoryResetInProgress(), is(true));

        // do it again, should still return true, but no change notified
        assertThat(mSystemInfo.factoryReset(), is(true));
        assertThat(mBackend.mFactoryResetCnt, is(2));
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mSystemInfo.isFactoryResetInProgress(), is(true));
    }

    @Test
    public void testResetSettings() {
        mSystemInfoImpl.publish();
        assertThat(mComponentChangeCnt, is(1));

        assertThat(mSystemInfo.isResetSettingsInProgress(), is(false));
        assertThat(mBackend.mResetSettingsCnt, is(0));

        assertThat(mSystemInfo.resetSettings(), is(true));
        assertThat(mBackend.mResetSettingsCnt, is(1));
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mSystemInfo.isResetSettingsInProgress(), is(true));

        // do it again, should still return true, but no change notified
        assertThat(mSystemInfo.resetSettings(), is(true));
        assertThat(mBackend.mResetSettingsCnt, is(2));
        assertThat(mComponentChangeCnt, is(2));
        assertThat(mSystemInfo.isResetSettingsInProgress(), is(true));
    }

    private static final class Backend implements SystemInfoCore.Backend {

        int mFactoryResetCnt;

        int mResetSettingsCnt;

        void reset() {
            mFactoryResetCnt = mResetSettingsCnt = 0;
        }

        @Override
        public boolean factoryReset() {
            mFactoryResetCnt++;
            return true;
        }

        @Override
        public boolean resetSettings() {
            mResetSettingsCnt++;
            return true;
        }
    }
}
