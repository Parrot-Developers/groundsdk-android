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
import com.parrot.drone.groundsdk.device.instrument.CameraExposureValues;
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraExposure;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiCameraExposureTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private CameraExposureValues mCameraFeatureValues;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mCameraFeatureValues = mDrone.getInstrumentStore().get(mMockSession, CameraExposureValues.class);
        mDrone.getInstrumentStore().registerObserver(CameraExposureValues.class, () -> {
            mCameraFeatureValues = mDrone.getInstrumentStore().get(mMockSession, CameraExposureValues.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the drone is not connected
        assertThat(mCameraFeatureValues, is(nullValue()));

        // values should be received during the connection for the component to be published
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraExposure(0,
                        ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_100, ArsdkFeatureCamera.IsoSensitivity.ISO_125,
                        ArsdkFeatureCamera.State.INACTIVE, 0, 0, 0, 0)));

        assertThat(mChangeCnt, is(1));
        assertThat(mCameraFeatureValues, is(notNullValue()));

        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(2));
        assertThat(mCameraFeatureValues, is(nullValue()));

        // if values are not received during connection, component should remain unpublished
        connectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(2));
        assertThat(mCameraFeatureValues, is(nullValue()));

        // as soon as the first event is received, component should be published
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_100, ArsdkFeatureCamera.IsoSensitivity.ISO_125,
                ArsdkFeatureCamera.State.INACTIVE, 0, 0, 0, 0));
        assertThat(mChangeCnt, is(3));
        assertThat(mCameraFeatureValues, is(notNullValue()));

        disconnectDrone(mDrone, 1);
        assertThat(mChangeCnt, is(4));
        assertThat(mCameraFeatureValues, is(nullValue()));
    }

    @Test
    public void testValues() {
        connectDrone(mDrone, 1);
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_100, ArsdkFeatureCamera.IsoSensitivity.ISO_100,
                ArsdkFeatureCamera.State.INACTIVE, 0, 0, 0, 0));
        assertThat(mCameraFeatureValues.getShutterSpeed(), is(CameraExposure.ShutterSpeed.ONE_OVER_100));
        assertThat(mCameraFeatureValues.getIsoSensitivity(), is(CameraExposure.IsoSensitivity.ISO_100));
        assertThat(mChangeCnt, is(1));

        // change shutter speed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_200, ArsdkFeatureCamera.IsoSensitivity.ISO_100,
                ArsdkFeatureCamera.State.INACTIVE, 0, 0, 0, 0));
        assertThat(mCameraFeatureValues.getShutterSpeed(), is(CameraExposure.ShutterSpeed.ONE_OVER_200));
        assertThat(mCameraFeatureValues.getIsoSensitivity(), is(CameraExposure.IsoSensitivity.ISO_100));
        assertThat(mChangeCnt, is(2));

        // change iso sensitivity
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_200, ArsdkFeatureCamera.IsoSensitivity.ISO_200,
                ArsdkFeatureCamera.State.INACTIVE, 0, 0, 0, 0));
        assertThat(mCameraFeatureValues.getShutterSpeed(), is(CameraExposure.ShutterSpeed.ONE_OVER_200));
        assertThat(mCameraFeatureValues.getIsoSensitivity(), is(CameraExposure.IsoSensitivity.ISO_200));
        assertThat(mChangeCnt, is(3));

        // change both shutter speed and iso sensitivity
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_400, ArsdkFeatureCamera.IsoSensitivity.ISO_400,
                ArsdkFeatureCamera.State.INACTIVE, 0, 0, 0, 0));
        assertThat(mCameraFeatureValues.getShutterSpeed(), is(CameraExposure.ShutterSpeed.ONE_OVER_400));
        assertThat(mCameraFeatureValues.getIsoSensitivity(), is(CameraExposure.IsoSensitivity.ISO_400));
        assertThat(mChangeCnt, is(4));

        // same values
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCameraExposure(0,
                ArsdkFeatureCamera.ShutterSpeed.SHUTTER_1_OVER_400, ArsdkFeatureCamera.IsoSensitivity.ISO_400,
                ArsdkFeatureCamera.State.INACTIVE, 0, 0, 0, 0));
        assertThat(mCameraFeatureValues.getShutterSpeed(), is(CameraExposure.ShutterSpeed.ONE_OVER_400));
        assertThat(mCameraFeatureValues.getIsoSensitivity(), is(CameraExposure.IsoSensitivity.ISO_400));
        assertThat(mChangeCnt, is(4));
    }
}
