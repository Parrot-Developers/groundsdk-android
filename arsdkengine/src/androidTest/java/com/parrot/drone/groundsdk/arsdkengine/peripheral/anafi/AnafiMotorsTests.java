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
import com.parrot.drone.groundsdk.device.peripheral.CopterMotors;
import com.parrot.drone.groundsdk.device.peripheral.motor.MotorError;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiMotorsTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private CopterMotors mCopterMotors;

    private int mCopterMotorsChangeCnt;

    @Override
    public void setUp() {
        super.setUp();

        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mCopterMotors = mDrone.getPeripheralStore().get(mMockSession, CopterMotors.class);
        mDrone.getPeripheralStore().registerObserver(CopterMotors.class, () -> {
            mCopterMotors = mDrone.getPeripheralStore().get(mMockSession, CopterMotors.class);
            mCopterMotorsChangeCnt++;
        });

        mCopterMotorsChangeCnt = 0;
    }

    @Test
    public void testPublication() {
        assertThat(mCopterMotors, nullValue());
        assertThat(mCopterMotorsChangeCnt, is(0));

        connectDrone(mDrone, 1);

        assertThat(mCopterMotors, notNullValue());
        assertThat(mCopterMotorsChangeCnt, is(1));

        disconnectDrone(mDrone, 1);

        assertThat(mCopterMotors, nullValue());
        assertThat(mCopterMotorsChangeCnt, is(2));
    }

    @Test
    public void testMotorState() {
        connectDrone(mDrone, 1);

        assertThat(mCopterMotorsChangeCnt, is(1));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), empty());
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.NONE));
        }

        // check motor bitfield translation for current error callback
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORMOTORSTALLED));
        assertThat(mCopterMotorsChangeCnt, is(2));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), containsInAnyOrder(CopterMotors.Motor.FRONT_LEFT));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(2,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(3));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), containsInAnyOrder(CopterMotors.Motor.FRONT_LEFT,
                CopterMotors.Motor.FRONT_RIGHT));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(4,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORRCEMERGENCYSTOP));
        assertThat(mCopterMotorsChangeCnt, is(4));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), containsInAnyOrder(CopterMotors.Motor.FRONT_LEFT,
                CopterMotors.Motor.FRONT_RIGHT, CopterMotors.Motor.REAR_RIGHT));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(8,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORLIPOCELLS));
        assertThat(mCopterMotorsChangeCnt, is(5));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), containsInAnyOrder(CopterMotors.Motor.FRONT_LEFT,
                CopterMotors.Motor.FRONT_RIGHT, CopterMotors.Motor.REAR_LEFT, CopterMotors.Motor.REAR_RIGHT));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(15,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.NOERROR));
        assertThat(mCopterMotorsChangeCnt, is(6));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), empty());

        // check error enum translation for current error callback
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERROREEPROM));
        assertThat(mCopterMotorsChangeCnt, is(7));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.OTHER));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORMOTORSTALLED));
        assertThat(mCopterMotorsChangeCnt, is(8));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.STALLED));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORCOMMLOST));
        assertThat(mCopterMotorsChangeCnt, is(9));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.OTHER));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORRCEMERGENCYSTOP));
        assertThat(mCopterMotorsChangeCnt, is(10));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.EMERGENCY_STOP));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORREALTIME));
        assertThat(mCopterMotorsChangeCnt, is(11));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.OTHER));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(12));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.SECURITY_MODE));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORMOTORSETTING));
        assertThat(mCopterMotorsChangeCnt, is(13));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.OTHER));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(14));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.SECURITY_MODE));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORTEMPERATURE));
        assertThat(mCopterMotorsChangeCnt, is(15));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.TEMPERATURE));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(16));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.SECURITY_MODE));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORBATTERYVOLTAGE));
        assertThat(mCopterMotorsChangeCnt, is(17));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.BATTERY_VOLTAGE));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(18));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.SECURITY_MODE));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORLIPOCELLS));
        assertThat(mCopterMotorsChangeCnt, is(19));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.LIPO_CELLS));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORMOSFET));
        assertThat(mCopterMotorsChangeCnt, is(20));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.MOSFET));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(21));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.SECURITY_MODE));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORBOOTLOADER));
        assertThat(mCopterMotorsChangeCnt, is(22));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.OTHER));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(23));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.SECURITY_MODE));

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORASSERT));
        assertThat(mCopterMotorsChangeCnt, is(24));
        assertThat(mCopterMotors.getLatestError(CopterMotors.Motor.FRONT_LEFT), is(MotorError.OTHER));

        assertThat(mCopterMotors.getMotorsCurrentlyInError(), containsInAnyOrder(CopterMotors.Motor.FRONT_LEFT));
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(1,
                ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.NOERROR));
        assertThat(mCopterMotorsChangeCnt, is(25));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), empty());
    }

    @Test
    public void testLastMotorError() {
        connectDrone(mDrone, 1);

        assertThat(mCopterMotorsChangeCnt, is(1));
        assertThat(mCopterMotors.getMotorsCurrentlyInError(), empty());
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.NONE));
        }

        // check motor bitfield translation for current error callback
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERROREEPROM));
        assertThat(mCopterMotorsChangeCnt, is(2));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.OTHER));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORMOTORSTALLED));
        assertThat(mCopterMotorsChangeCnt, is(3));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.STALLED));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORCOMMLOST));
        assertThat(mCopterMotorsChangeCnt, is(4));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.OTHER));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORRCEMERGENCYSTOP));
        assertThat(mCopterMotorsChangeCnt, is(5));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.EMERGENCY_STOP));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORREALTIME));
        assertThat(mCopterMotorsChangeCnt, is(6));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.OTHER));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(7));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.SECURITY_MODE));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORMOTORSETTING));
        assertThat(mCopterMotorsChangeCnt, is(8));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.OTHER));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(9));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.SECURITY_MODE));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORTEMPERATURE));
        assertThat(mCopterMotorsChangeCnt, is(10));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.TEMPERATURE));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(11));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.SECURITY_MODE));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORBATTERYVOLTAGE));
        assertThat(mCopterMotorsChangeCnt, is(12));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.BATTERY_VOLTAGE));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(13));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.SECURITY_MODE));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORLIPOCELLS));
        assertThat(mCopterMotorsChangeCnt, is(14));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.LIPO_CELLS));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORMOSFET));
        assertThat(mCopterMotorsChangeCnt, is(15));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.MOSFET));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(16));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.SECURITY_MODE));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORBOOTLOADER));
        assertThat(mCopterMotorsChangeCnt, is(17));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.OTHER));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORPROPELLERSECURITY));
        assertThat(mCopterMotorsChangeCnt, is(18));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.SECURITY_MODE));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.ERRORASSERT));
        assertThat(mCopterMotorsChangeCnt, is(19));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.OTHER));
        }

        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorLastErrorChanged(
                ArsdkFeatureArdrone3.SettingsstateMotorerrorlasterrorchangedMotorerror.NOERROR));
        assertThat(mCopterMotorsChangeCnt, is(20));
        for (CopterMotors.Motor motor : CopterMotors.Motor.values()) {
            assertThat(mCopterMotors.getLatestError(motor), is(MotorError.NONE));
        }
    }
}
