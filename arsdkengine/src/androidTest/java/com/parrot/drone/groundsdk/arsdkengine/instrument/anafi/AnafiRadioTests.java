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

package com.parrot.drone.groundsdk.arsdkengine.instrument.anafi;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.Radio;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiRadioTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private Radio mRadio;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mRadio = mDrone.getInstrumentStore().get(mMockSession, Radio.class);
        mDrone.getInstrumentStore().registerObserver(Radio.class, () -> {
            mRadio = mDrone.getInstrumentStore().get(mMockSession, Radio.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the drone is not connected
        assertThat(mRadio, is(nullValue()));

        connectDrone(mDrone, 1);
        assertThat(mRadio, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectDrone(mDrone, 1);
        assertThat(mRadio, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testRssi() {
        connectDrone(mDrone, 1);

        // check default value
        assertThat(mRadio.getRssi(), is(0));
        assertThat(mChangeCnt, is(1));

        // check rssi
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeWifiRssiChanged(-35));
        assertThat(mRadio.getRssi(), is(-35));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testLinkSignalQuality() {
        connectDrone(mDrone, 1);

        // check default values
        assertThat(mRadio.getLinkSignalQuality(), is(-1));
        assertThat(mRadio.isLinkPerturbed(), is(false));
        assertThat(mRadio.is4GInterfering(), is(false));
        assertThat(mChangeCnt, is(1));

        // minimal link quality and signal not perturbed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateLinkSignalQuality(1));
        assertThat(mRadio.getLinkSignalQuality(), is(0));
        assertThat(mRadio.isLinkPerturbed(), is(false));
        assertThat(mRadio.is4GInterfering(), is(false));
        assertThat(mChangeCnt, is(2));

        // maximal link quality and signal not perturbed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateLinkSignalQuality(5));
        assertThat(mRadio.getLinkSignalQuality(), is(4));
        assertThat(mRadio.isLinkPerturbed(), is(false));
        assertThat(mRadio.is4GInterfering(), is(false));
        assertThat(mChangeCnt, is(3));

        // high link quality and signal perturbed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateLinkSignalQuality(4 | (1 << 7)));
        assertThat(mRadio.getLinkSignalQuality(), is(3));
        assertThat(mRadio.isLinkPerturbed(), is(true));
        assertThat(mRadio.is4GInterfering(), is(false));
        assertThat(mChangeCnt, is(4));

        // high link quality, signal perturbed and 4G interfering
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCommonCommonStateLinkSignalQuality(4 | (1 << 6) | (1 << 7)));
        assertThat(mRadio.getLinkSignalQuality(), is(3));
        assertThat(mRadio.isLinkPerturbed(), is(true));
        assertThat(mRadio.is4GInterfering(), is(true));
        assertThat(mChangeCnt, is(5));

        // invalid signal quality should be ignored, but other infos should be updated
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateLinkSignalQuality(6));
        assertThat(mRadio.getLinkSignalQuality(), is(3));
        assertThat(mRadio.isLinkPerturbed(), is(false));
        assertThat(mRadio.is4GInterfering(), is(false));
        assertThat(mChangeCnt, is(6));
    }
}
