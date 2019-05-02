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

import android.location.Location;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.instrument.Gps;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static com.parrot.drone.groundsdk.OptionalDoubleMatcher.optionalDoubleValueIs;
import static com.parrot.drone.groundsdk.OptionalValueMatcher.optionalValueIsUnavailable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiGpsTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private Gps mGps;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mGps = mDrone.getInstrumentStore().get(mMockSession, Gps.class);
        mDrone.getInstrumentStore().registerObserver(Gps.class, () -> {
            mGps = mDrone.getInstrumentStore().get(mMockSession, Gps.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the drone is not connected and no location has been saved yet
        assertThat(mGps, is(nullValue()));

        connectDrone(mDrone, 1);
        assertThat(mGps, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectDrone(mDrone, 1);
        assertThat(mGps, is(nullValue()));
        assertThat(mChangeCnt, is(2));

        // should still be available when the drone is disconnected and a location has been saved
        connectDrone(mDrone, 1);
        assertThat(mGps, is(notNullValue()));
        assertThat(mChangeCnt, is(3));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePositionChanged(1.2, 2.3, 56.0));
        assertThat(mChangeCnt, is(4));
        disconnectDrone(mDrone, 1);
        assertThat(mGps, is(notNullValue()));
        assertThat(mChangeCnt, is(4));
    }

    @Test
    public void testValue() {
        connectDrone(mDrone, 1);

        // check default values
        assertThat(mGps.isFixed(), is(false));
        assertThat(mGps.lastKnownLocation(), nullValue());
        assertThat(mGps.getVerticalAccuracy(), optionalValueIsUnavailable());
        assertThat(mGps.getSatelliteCount(), is(0));
        assertThat(mChangeCnt, is(1));

        // Receive gps position without having fixed should set the location anyway
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStatePositionChanged(1.2, 2.3, 56.0));
        assertThat(mGps.isFixed(), is(false));
        Location location = mGps.lastKnownLocation();
        assertThat(location, notNullValue());
        assertThat(location.getLatitude(), is(1.2));
        assertThat(location.getLongitude(), is(2.3));
        assertThat(location.hasAltitude(), is(true));
        assertThat(location.getAltitude(), is(56.0));
        assertThat(location.hasAccuracy(), is(false));
        assertThat(location.getAccuracy(), is(0.0f));
        assertThat(mGps.getVerticalAccuracy(), optionalValueIsUnavailable());
        assertThat(mGps.getSatelliteCount(), is(0));
        assertThat(mChangeCnt, is(2));

        // Fix changed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3GPSSettingsStateGPSFixStateChanged(1));
        assertThat(mGps.isFixed(), is(true));
        location = mGps.lastKnownLocation();
        assertThat(location, notNullValue());
        assertThat(location.getLatitude(), is(1.2));
        assertThat(location.getLongitude(), is(2.3));
        assertThat(location.hasAltitude(), is(true));
        assertThat(location.getAltitude(), is(56.0));
        assertThat(location.hasAccuracy(), is(false));
        assertThat(location.getAccuracy(), is(0.0f));
        assertThat(mGps.getVerticalAccuracy(), optionalValueIsUnavailable());
        assertThat(mGps.getSatelliteCount(), is(0));
        assertThat(mChangeCnt, is(3));

        // nb satellite changed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3GPSStateNumberOfSatelliteChanged(2));
        assertThat(mGps.isFixed(), is(true));
        location = mGps.lastKnownLocation();
        assertThat(location, notNullValue());
        assertThat(location.getLatitude(), is(1.2));
        assertThat(location.getLongitude(), is(2.3));
        assertThat(location.hasAltitude(), is(true));
        assertThat(location.getAltitude(), is(56.0));
        assertThat(location.hasAccuracy(), is(false));
        assertThat(location.getAccuracy(), is(0.0f));
        assertThat(mGps.getVerticalAccuracy(), optionalValueIsUnavailable());
        assertThat(mGps.getSatelliteCount(), is(2));
        assertThat(mChangeCnt, is(4));

        // Not fixed should not erase the location values and the satellite number value
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3GPSSettingsStateGPSFixStateChanged(0));
        assertThat(mGps.isFixed(), is(false));
        location = mGps.lastKnownLocation();
        assertThat(location, notNullValue());
        assertThat(location.getLatitude(), is(1.2));
        assertThat(location.getLongitude(), is(2.3));
        assertThat(location.hasAltitude(), is(true));
        assertThat(location.getAltitude(), is(56.0));
        assertThat(location.hasAccuracy(), is(false));
        assertThat(location.getAccuracy(), is(0.0f));
        assertThat(mGps.getVerticalAccuracy(), optionalValueIsUnavailable());
        // but the satellite count should roll back to default - 0
        assertThat(mGps.getSatelliteCount(), is(0));
        assertThat(mChangeCnt, is(5));

        // update accuracies. Use GpsLocationChanged command
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingStateGpsLocationChanged(2.3, 4.5, 6.7, 8, 9, 10));
        assertThat(mGps.isFixed(), is(false));
        location = mGps.lastKnownLocation();
        assertThat(location, notNullValue());
        assertThat(location.getLatitude(), is(2.3));
        assertThat(location.getLongitude(), is(4.5));
        assertThat(location.hasAltitude(), is(true));
        assertThat(location.getAltitude(), is(6.7));
        assertThat(location.hasAccuracy(), is(true));
        assertThat(location.getAccuracy(), is(9.0f));
        assertThat(mGps.getVerticalAccuracy(), optionalDoubleValueIs(10.0));
        assertThat(mGps.getSatelliteCount(), is(0));
        assertThat(mChangeCnt, is(6));

        // check that from now on PositionChanged command is ignored
        mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingStatePositionChanged(1.2, 2.3, 56.0));
        assertThat(mGps.isFixed(), is(false));
        location = mGps.lastKnownLocation();
        assertThat(location, notNullValue());
        assertThat(location.getLatitude(), is(2.3));
        assertThat(location.getLongitude(), is(4.5));
        assertThat(location.hasAltitude(), is(true));
        assertThat(location.getAltitude(), is(6.7));
        assertThat(location.hasAccuracy(), is(true));
        assertThat(location.getAccuracy(), is(9.0f));
        assertThat(mGps.getVerticalAccuracy(), optionalDoubleValueIs(10.0));
        assertThat(mGps.getSatelliteCount(), is(0));
        assertThat(mChangeCnt, is(6));

        // disconnect drone, last known location is still available
        disconnectDrone(mDrone, 1);
        assertThat(mGps, is(notNullValue()));
        assertThat(mGps.isFixed(), is(false));
        location = mGps.lastKnownLocation();
        assertThat(location, notNullValue());
        assertThat(location.getLatitude(), is(2.3));
        assertThat(location.getLongitude(), is(4.5));
        assertThat(location.hasAltitude(), is(true));
        assertThat(location.getAltitude(), is(6.7));
        assertThat(location.hasAccuracy(), is(true));
        assertThat(location.getAccuracy(), is(9.0f));
        assertThat(mGps.getVerticalAccuracy(), optionalDoubleValueIs(10.0));
        assertThat(mGps.getSatelliteCount(), is(0));
        assertThat(mChangeCnt, is(6));
    }
}
