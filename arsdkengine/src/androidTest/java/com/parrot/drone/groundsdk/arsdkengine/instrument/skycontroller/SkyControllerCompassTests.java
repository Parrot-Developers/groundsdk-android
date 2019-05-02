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

package com.parrot.drone.groundsdk.arsdkengine.instrument.skycontroller;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.RemoteControl;
import com.parrot.drone.groundsdk.device.instrument.Compass;
import com.parrot.drone.groundsdk.internal.device.RemoteControlCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class SkyControllerCompassTests extends ArsdkEngineTestBase {

    private RemoteControlCore mRc;

    private Compass mCompass;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", RemoteControl.Model.SKY_CONTROLLER_3.id(), "Rc1", 1, Backend.TYPE_MUX);
        mRc = mRCStore.get("123");
        assert mRc != null;

        mCompass = mRc.getInstrumentStore().get(mMockSession, Compass.class);
        mRc.getInstrumentStore().registerObserver(Compass.class, () -> {
            mCompass = mRc.getInstrumentStore().get(mMockSession, Compass.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the drone is not connected
        assertThat(mCompass, is(nullValue()));

        connectRemoteControl(mRc, 1);
        assertThat(mCompass, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectRemoteControl(mRc, 1);
        assertThat(mCompass, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testValue() {
        connectRemoteControl(mRc, 1);

        // check default values
        assertThat(mCompass.getHeading(), is(0.0));
        assertThat(mChangeCnt, is(1));

        // check heading
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSkyControllerStateAttitudeChanged(
                0.707106781186548f, 0.0f, 0.0f, 0.707106781186547f));
        assertThat(mCompass.getHeading(), closeTo(90, 0.0001));
        assertThat(mChangeCnt, is(2));

        // check bounds
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSkyControllerStateAttitudeChanged(
                0.0f, 0.0f, 0.0f, 0.0f));
        assertThat(mCompass.getHeading(), is(0.0));
        assertThat(mChangeCnt, is(3));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeSkyctrlSkyControllerStateAttitudeChanged(
                0.707106781186548f, 0.0f, 0.0f, -0.707106781186547f));
        assertThat(mCompass.getHeading(), closeTo(270, 0.0001));
        assertThat(mChangeCnt, is(4));
    }
}
