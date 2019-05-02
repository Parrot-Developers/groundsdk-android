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
import com.parrot.drone.groundsdk.device.instrument.PhotoProgressIndicator;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static com.parrot.drone.groundsdk.OptionalDoubleMatcher.optionalDoubleValueIs;
import static com.parrot.drone.groundsdk.OptionalValueMatcher.optionalValueIsUnavailable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiPhotoProgressIndicatorTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private PhotoProgressIndicator mPhotoProgressIndicator;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mPhotoProgressIndicator = mDrone.getInstrumentStore().get(mMockSession, PhotoProgressIndicator.class);
        mDrone.getInstrumentStore().registerObserver(PhotoProgressIndicator.class, () -> {
            mPhotoProgressIndicator = mDrone.getInstrumentStore().get(mMockSession, PhotoProgressIndicator.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when the drone is not connected
        assertThat(mPhotoProgressIndicator, is(nullValue()));

        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mPhotoProgressIndicator, is(notNullValue()));

        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(2));
        assertThat(mPhotoProgressIndicator, is(nullValue()));
    }

    @Test
    public void testValues() {
        connectDrone(mDrone, 1);

        // check default values
        assertThat(mChangeCnt, is(1));
        assertThat(mPhotoProgressIndicator.getRemainingTime(), optionalValueIsUnavailable());
        assertThat(mPhotoProgressIndicator.getRemainingDistance(), optionalValueIsUnavailable());

        // change remaining time
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraNextPhotoDelay(
                ArsdkFeatureCamera.PhotoMode.TIME_LAPSE, 2.5f));
        assertThat(mChangeCnt, is(2));
        assertThat(mPhotoProgressIndicator.getRemainingTime(), optionalDoubleValueIs(2.5));
        assertThat(mPhotoProgressIndicator.getRemainingDistance(), optionalValueIsUnavailable());

        // time-lapse is stopped
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraPhotoState(0,
                        ArsdkFeatureCamera.Availability.AVAILABLE, ArsdkFeatureCamera.State.INACTIVE));
        assertThat(mChangeCnt, is(3));
        assertThat(mPhotoProgressIndicator.getRemainingTime(), optionalValueIsUnavailable());
        assertThat(mPhotoProgressIndicator.getRemainingDistance(), optionalValueIsUnavailable());

        // change remaining distance
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraNextPhotoDelay(
                ArsdkFeatureCamera.PhotoMode.GPS_LAPSE, 5.0f));
        assertThat(mChangeCnt, is(4));
        assertThat(mPhotoProgressIndicator.getRemainingTime(), optionalValueIsUnavailable());
        assertThat(mPhotoProgressIndicator.getRemainingDistance(), optionalDoubleValueIs(5.0));

        // photo becomes unavailable
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraPhotoState(0,
                        ArsdkFeatureCamera.Availability.NOT_AVAILABLE, ArsdkFeatureCamera.State.ACTIVE));
        assertThat(mChangeCnt, is(5));
        assertThat(mPhotoProgressIndicator.getRemainingTime(), optionalValueIsUnavailable());
        assertThat(mPhotoProgressIndicator.getRemainingDistance(), optionalValueIsUnavailable());
    }
}
