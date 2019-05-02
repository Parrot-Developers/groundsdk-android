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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.camera;

import android.location.Address;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.AntiFlicker;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCamera;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.arsdk.Expectation;
import com.parrot.drone.sdkcore.arsdk.ExpectedCmd;

import org.junit.Test;

import java.util.EnumSet;

import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpToDateAt;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingIsUpdatingTo;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingSupports;
import static com.parrot.drone.groundsdk.EnumSettingMatcher.enumSettingValueIs;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpToDate;
import static com.parrot.drone.groundsdk.SettingMatcher.settingIsUpdating;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiAntiFlickerTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private AntiFlicker mAntiFlicker;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
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

        mAntiFlicker = mDrone.getPeripheralStore().get(mMockSession, AntiFlicker.class);
        mDrone.getPeripheralStore().registerObserver(AntiFlicker.class, () -> {
            mAntiFlicker = mDrone.getPeripheralStore().get(mMockSession, AntiFlicker.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        // should be unavailable when drone has never been connected
        assertThat(mChangeCnt, is(0));
        assertThat(mAntiFlicker, nullValue());

        // connect drone
        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mAntiFlicker, notNullValue());

        // disconnect drone
        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mAntiFlicker, notNullValue());

        // forget drone
        mDrone.forget();

        assertThat(mChangeCnt, is(2));
        assertThat(mAntiFlicker, nullValue());
    }

    @Test
    public void testModeAndValue() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCameraAntiflickerCapabilities(
                        ArsdkFeatureCamera.AntiflickerMode.toBitField(
                                ArsdkFeatureCamera.AntiflickerMode.OFF,
                                ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ,
                                ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ)))
                .commandReceived(1, ArsdkEncoder.encodeCameraAntiflickerMode(
                        ArsdkFeatureCamera.AntiflickerMode.OFF, ArsdkFeatureCamera.AntiflickerMode.OFF)));

        // initial values
        assertThat(mChangeCnt, is(1));
        assertThat(mAntiFlicker, notNullValue());
        assertThat(mAntiFlicker.mode(), enumSettingSupports(
                EnumSet.of(AntiFlicker.Mode.OFF, AntiFlicker.Mode.HZ_50, AntiFlicker.Mode.HZ_60,
                        AntiFlicker.Mode.AUTO)));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.OFF));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.OFF));

        // set mode to 50Hz
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.cameraSetAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ)));
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.HZ_50);
        assertThat(mChangeCnt, is(2));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpdatingTo(AntiFlicker.Mode.HZ_50));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.OFF));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder
                .encodeCameraAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ,
                        ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ));
        assertThat(mChangeCnt, is(3));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.HZ_50));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_50));

        // set mode to 60Hz
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.cameraSetAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ)));
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.HZ_60);
        assertThat(mChangeCnt, is(4));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpdatingTo(AntiFlicker.Mode.HZ_60));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_50));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder
                .encodeCameraAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ,
                        ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ));
        assertThat(mChangeCnt, is(5));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.HZ_60));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_60));

        // set mode to off
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.cameraSetAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.OFF)));
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.OFF);
        assertThat(mChangeCnt, is(6));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpdatingTo(AntiFlicker.Mode.OFF));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_60));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder
                .encodeCameraAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.OFF,
                        ArsdkFeatureCamera.AntiflickerMode.OFF));
        assertThat(mChangeCnt, is(7));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.OFF));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.OFF));
    }

    @Test
    public void testAutoLocation() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCameraAntiflickerCapabilities(
                        ArsdkFeatureCamera.AntiflickerMode.toBitField(
                                ArsdkFeatureCamera.AntiflickerMode.OFF,
                                ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ,
                                ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ)))
                .commandReceived(1, ArsdkEncoder.encodeCameraAntiflickerMode(
                        ArsdkFeatureCamera.AntiflickerMode.OFF, ArsdkFeatureCamera.AntiflickerMode.OFF)));

        // initial values
        assertThat(mChangeCnt, is(1));
        assertThat(mAntiFlicker, notNullValue());
        assertThat(mAntiFlicker.mode(), enumSettingSupports(
                EnumSet.of(AntiFlicker.Mode.OFF, AntiFlicker.Mode.HZ_50, AntiFlicker.Mode.HZ_60,
                        AntiFlicker.Mode.AUTO)));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.OFF));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.OFF));

        // set mode to auto mode
        // since the reverse-geocoder did not provide any location, expect no command to be sent; setting should switch
        // directly to up to date.
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.AUTO);
        assertThat(mChangeCnt, is(2));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.AUTO));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.OFF));

        // change to France (50Hz), based on country code
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.cameraSetAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ)));
        Address address = new Address(null);
        address.setCountryCode("fr");
        mReverseGeocoderUtility.updateAddress(address);
        mMockArsdkCore.commandReceived(1, ArsdkEncoder
                .encodeCameraAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ,
                        ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ));
        assertThat(mChangeCnt, is(3));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.AUTO));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_50));

        // change to USA (60Hz), based on country code
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.cameraSetAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ)));
        address = new Address(null);
        address.setCountryCode("us");
        mReverseGeocoderUtility.updateAddress(address);
        mMockArsdkCore.commandReceived(1, ArsdkEncoder
                .encodeCameraAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ,
                        ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ));
        assertThat(mChangeCnt, is(4));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.AUTO));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_60));

        // set to fixed 60 Hz.
        // here, since on the drone the mode is already 60 Hz, we don't expect any command to be sent; setting should
        // become directly up to date.
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.HZ_60);
        assertThat(mChangeCnt, is(5));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.HZ_60));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_60));

        // changing current country does not change frequency
        address.setCountryCode("fr");
        mReverseGeocoderUtility.updateAddress(address);
        assertThat(mChangeCnt, is(5));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.HZ_60));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_60));

        // set mode to auto
        // here, since current country (fr) is 50 Hz, expect mode to be sent to drone
        mMockArsdkCore.expect(new Expectation.Command(1,
                ExpectedCmd.cameraSetAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ)));
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.AUTO);
        assertThat(mAntiFlicker.mode(), enumSettingIsUpdatingTo(AntiFlicker.Mode.AUTO));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_60));
        assertThat(mChangeCnt, is(6));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder
                .encodeCameraAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ,
                        ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ));
        assertThat(mChangeCnt, is(7));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.AUTO));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_50));

        // disconnect
        disconnectDrone(mDrone, 1);
        resetEngine();

        // check auto mode has been saved
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.AUTO));
    }

    @Test
    public void tesOfflineAccess() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore
                .commandReceived(1, ArsdkEncoder.encodeCameraAntiflickerCapabilities(
                        ArsdkFeatureCamera.AntiflickerMode.toBitField(
                                ArsdkFeatureCamera.AntiflickerMode.OFF,
                                ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ,
                                ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ)))
                .commandReceived(1, ArsdkEncoder
                        .encodeCameraAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ,
                                ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ)));

        // init
        assertThat(mChangeCnt, is(1));
        assertThat(mAntiFlicker, notNullValue());
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.HZ_50));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_50));

        // user changes mode
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd
                .cameraSetAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ)));
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.HZ_60);
        assertThat(mChangeCnt, is(2));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpdatingTo(AntiFlicker.Mode.HZ_60));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.HZ_50));

        // disconnect drone
        disconnectDrone(mDrone, 1);
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.HZ_60));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.UNKNOWN));

        resetEngine();
        assertThat(mChangeCnt, is(0));
        assertThat(mAntiFlicker, notNullValue());

        // check settings are loaded correctly
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.HZ_60));
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.UNKNOWN));

        // change to 50Hz while disconnected
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.HZ_50);
        assertThat(mChangeCnt, is(1));
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.HZ_50));

        // connect
        connectDrone(mDrone, 1, () -> {
            mMockArsdkCore.commandReceived(1, ArsdkEncoder
                    .encodeCameraAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ,
                            ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ));
            mMockArsdkCore.expect(new Expectation.Command(1,
                    ExpectedCmd.cameraSetAntiflickerMode(ArsdkFeatureCamera.AntiflickerMode.MODE_50HZ)));
        });
        assertThat(mAntiFlicker.mode(), enumSettingIsUpToDateAt(AntiFlicker.Mode.HZ_50));
    }

    @Test
    public void testResetOnDisconnect() {
        // tests that all values are reset properly and rollbacks are canceled upon disconnection
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeCameraAntiflickerCapabilities(ArsdkFeatureCamera.AntiflickerMode.toBitField(
                        ArsdkFeatureCamera.AntiflickerMode.OFF,
                        ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ))));

        assertThat(mChangeCnt, is(1));
        assertThat(mAntiFlicker.mode(), allOf(
                enumSettingValueIs(AntiFlicker.Mode.OFF),
                settingIsUpToDate()));

        // mock user modifies settings
        mMockArsdkCore.expect(new Expectation.Command(1, ExpectedCmd.cameraSetAntiflickerMode(
                ArsdkFeatureCamera.AntiflickerMode.MODE_60HZ)));
        mAntiFlicker.mode().setValue(AntiFlicker.Mode.HZ_60);

        assertThat(mChangeCnt, is(2));
        assertThat(mAntiFlicker.mode(), allOf(
                enumSettingValueIs(AntiFlicker.Mode.HZ_60),
                settingIsUpdating()));

        // disconnect
        disconnectDrone(mDrone, 1);

        // setting should be updated to user value
        assertThat(mChangeCnt, is(3));

        assertThat(mAntiFlicker.mode(), allOf(
                enumSettingValueIs(AntiFlicker.Mode.HZ_60),
                settingIsUpToDate()));

        // test other values are reset as they should
        assertThat(mAntiFlicker.value(), is(AntiFlicker.Value.UNKNOWN));
    }
}
