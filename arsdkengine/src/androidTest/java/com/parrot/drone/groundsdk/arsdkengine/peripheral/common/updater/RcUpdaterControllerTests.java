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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common.updater;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.peripheral.Updater;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;
import com.parrot.drone.groundsdk.internal.utility.FirmwareStore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

public class RcUpdaterControllerTests extends ArsdkEngineTestBase {

    private RemoteControlCore mRc;

    private Updater mUpdater;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();

        mUtilities.registerUtility(FirmwareStore.class, mock(FirmwareStore.class))
                  .registerUtility(FirmwareDownloader.class, mock(FirmwareDownloader.class));

        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", RemoteControl.Model.SKY_CONTROLLER_3.id(), "Rc1", 1, Backend.TYPE_MUX);
        mRc = mRCStore.get("123");
        assert mRc != null;

        mUpdater = mRc.getPeripheralStore().get(mMockSession, Updater.class);
        mRc.getPeripheralStore().registerObserver(Updater.class, () -> {
            mUpdater = mRc.getPeripheralStore().get(mMockSession, Updater.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testNotEnoughBatteryUnavailability() {
        assertThat(mChangeCnt, is(0));
        assertThat(mUpdater, nullValue());

        // mock rc connect, with enough battery
        connectRemoteControl(mRc, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeSkyctrlSkyControllerStateBatteryChanged(40)));

        // unavailability reasons should be cleared
        assertThat(mChangeCnt, is(1));
        assertThat(mUpdater.updateUnavailabilityReasons(), empty());

        // mock battery goes low
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSkyControllerStateBatteryChanged(39));

        // unavailability reasons should report NOT_ENOUGH_BATTERY
        assertThat(mChangeCnt, is(2));
        assertThat(mUpdater.updateUnavailabilityReasons(), contains(
                Updater.Update.UnavailabilityReason.NOT_ENOUGH_BATTERY));

        // disconnect
        disconnectRemoteControl(mRc, 1);

        // unavailability reasons should report NOT_CONNECTED
        assertThat(mChangeCnt, is(3));
        assertThat(mUpdater.updateUnavailabilityReasons(), contains(
                Updater.Update.UnavailabilityReason.NOT_CONNECTED));

        // mock rc connect, without enough battery
        connectRemoteControl(mRc, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeSkyctrlSkyControllerStateBatteryChanged(29)));

        // unavailability reasons should report NOT_ENOUGH_BATTERY
        assertThat(mChangeCnt, is(4));
        assertThat(mUpdater.updateUnavailabilityReasons(), contains(
                Updater.Update.UnavailabilityReason.NOT_ENOUGH_BATTERY));
    }
}
