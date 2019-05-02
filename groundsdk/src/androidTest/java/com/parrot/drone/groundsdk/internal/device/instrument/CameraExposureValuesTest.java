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

package com.parrot.drone.groundsdk.internal.device.instrument;

import com.parrot.drone.groundsdk.device.instrument.CameraExposureValues;
import com.parrot.drone.groundsdk.device.instrument.Instrument;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposure;
import com.parrot.drone.groundsdk.internal.MockComponentStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class CameraExposureValuesTest {

    private MockComponentStore<Instrument> mStore;

    private int mChangeCnt;

    private CameraExposureValuesCore mImpl;

    @Before
    public void setUp() {
        mStore = new MockComponentStore<>();
        mChangeCnt = 0;
        mImpl = new CameraExposureValuesCore(mStore);
        mStore.registerObserver(CameraExposureValues.class, () -> mChangeCnt++);
    }

    @After
    public void tearDown() {
        mStore.destroy();
        mStore = null;
        mImpl = null;
    }

    /**
     * Test that publishing the component will add it to the store and unpublishing it will remove it.
     */
    @Test
    public void testPublication() {
        mImpl.publish();
        assertThat(mImpl, is(mStore.get(CameraExposureValues.class)));
        assertThat(mChangeCnt, is(1));

        mImpl.unpublish();
        assertThat(mStore.get(CameraExposureValues.class), is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testNotification() {
        mImpl.publish();
        assertThat(mChangeCnt, is(1));

        CameraExposureValues instrument = mStore.get(CameraExposureValues.class);
        assert instrument != null;

        // test notify without changes
        mImpl.notifyUpdated();
        assertThat(mChangeCnt, is(1));

        // test grouped change with one notify
        mImpl.updateShutterSpeed(CameraExposure.ShutterSpeed.ONE_OVER_100)
             .updateIsoSensitivity(CameraExposure.IsoSensitivity.ISO_100)
             .notifyUpdated();
        assertThat(mChangeCnt, is(2));

        // test setting the same value does not trigger notification
        mImpl.updateShutterSpeed(CameraExposure.ShutterSpeed.ONE_OVER_100).notifyUpdated();
        mImpl.updateIsoSensitivity(CameraExposure.IsoSensitivity.ISO_100).notifyUpdated();
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testShutterSpeed() {
        mImpl.publish();

        CameraExposureValues instrument = mStore.get(CameraExposureValues.class);
        assert instrument != null;

        // test initial values
        assertThat(mChangeCnt, is(1));
        assertThat(instrument.getShutterSpeed(), is(CameraExposure.ShutterSpeed.ONE_OVER_1000));

        // test value change
        mImpl.updateShutterSpeed(CameraExposure.ShutterSpeed.ONE_OVER_100).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getShutterSpeed(), is(CameraExposure.ShutterSpeed.ONE_OVER_100));
    }

    @Test
    public void testIsoSensitivity() {
        mImpl.publish();

        CameraExposureValues instrument = mStore.get(CameraExposureValues.class);
        assert instrument != null;

        // test initial values
        assertThat(mChangeCnt, is(1));
        assertThat(instrument.getIsoSensitivity(), is(CameraExposure.IsoSensitivity.ISO_50));

        // test value change
        mImpl.updateIsoSensitivity(CameraExposure.IsoSensitivity.ISO_100).notifyUpdated();
        assertThat(mChangeCnt, is(2));
        assertThat(instrument.getIsoSensitivity(), is(CameraExposure.IsoSensitivity.ISO_100));
    }
}
