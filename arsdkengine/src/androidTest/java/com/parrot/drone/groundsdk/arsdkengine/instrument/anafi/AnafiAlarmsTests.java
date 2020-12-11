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
import com.parrot.drone.groundsdk.device.instrument.Alarms;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureBattery;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCommon;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class AnafiAlarmsTests extends ArsdkEngineTestBase {

    private DroneCore mDrone;

    private Alarms mAlarms;

    private int mChangeCnt;

    @Override
    public void setUp() {
        super.setUp();
        mArsdkEngine.start();
        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mAlarms = mDrone.getInstrumentStore().get(mMockSession, Alarms.class);
        mDrone.getInstrumentStore().registerObserver(Alarms.class, () -> {
            mAlarms = mDrone.getInstrumentStore().get(mMockSession, Alarms.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Test
    public void testPublishUnpublish() {
        // should be unavailable when the drone is not connected
        assertThat(mAlarms, is(nullValue()));

        connectDrone(mDrone, 1);
        assertThat(mAlarms, is(notNullValue()));
        assertThat(mChangeCnt, is(1));

        disconnectDrone(mDrone, 1);
        assertThat(mAlarms, is(nullValue()));
        assertThat(mChangeCnt, is(2));
    }

    @Test
    public void testAlarmsWithoutFeatureBattery() {
        connectDrone(mDrone, 1);
        // check default values
        for (Alarms.Alarm.Kind kind : Alarms.Alarm.Kind.values()) {
            Alarms.Alarm alarm = mAlarms.getAlarm(kind);
            assertThat(alarm.getLevel(), is(Alarms.Alarm.Level.OFF));
        }
        assertThat(mChangeCnt, is(1));

        // Low battery
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.LOW_BATTERY));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(2));

        // UserEmergency
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.USER));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(3));

        // Cut out
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.CUT_OUT));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(4));

        // Critical battery
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.CRITICAL_BATTERY));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(5));

        // Motor error
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(
                1, ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORBATTERYVOLTAGE));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(6));

        // Alert none
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.NONE));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(7));

        // Motor error gone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(
                1, ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.NOERROR));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(8));

        // Too much angle should not change any alarms
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.TOO_MUCH_ANGLE));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(8));

        // Almost empty battery
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.ALMOST_EMPTY_BATTERY));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(9));

        // Magnetometer perturbation
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.MAGNETO_PERTUBATION));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(10));

        // Magnetometer low earth field
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.MAGNETO_LOW_EARTH_FIELD));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mChangeCnt, is(11));

        // Alert none
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.NONE));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(12));
    }

    @Test
    public void testAlarmsWithFeatureBattery() {
        connectDrone(mDrone, 1);
        // check default values
        for (Alarms.Alarm.Kind kind : Alarms.Alarm.Kind.values()) {
            Alarms.Alarm alarm = mAlarms.getAlarm(kind);

            assertThat(alarm.getLevel(), is(Alarms.Alarm.Level.OFF));
        }
        assertThat(mChangeCnt, is(1));

        // Empty battery alarm
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeBatteryAlert(ArsdkFeatureBattery.Alert.POWER_LEVEL,
                ArsdkFeatureBattery.AlertLevel.NONE,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.EMPTY)));
        assertThat(mChangeCnt, is(1));

        // Low battery
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.LOW_BATTERY));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(1));

        // Almost empty battery
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.ALMOST_EMPTY_BATTERY));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(1));

        // UserEmergency
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.USER));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(2));

        // Cut out
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.CUT_OUT));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(3));

        // Motor error
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(
                1, ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.ERRORBATTERYVOLTAGE));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(4));

        // Warning battery low (as first element of the map, should not trigger any update)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeBatteryAlert(ArsdkFeatureBattery.Alert.POWER_LEVEL,
                ArsdkFeatureBattery.AlertLevel.WARNING,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST)));
        assertThat(mChangeCnt, is(4));

        // Critical battery too cold
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeBatteryAlert(ArsdkFeatureBattery.Alert.TOO_COLD,
                ArsdkFeatureBattery.AlertLevel.CRITICAL,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(5));

        // Remove battery too cold
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeBatteryAlert(ArsdkFeatureBattery.Alert.TOO_COLD,
                ArsdkFeatureBattery.AlertLevel.CRITICAL,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.REMOVE,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(6));

        // Add as first and last battery too hot
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeBatteryAlert(ArsdkFeatureBattery.Alert.TOO_HOT,
                ArsdkFeatureBattery.AlertLevel.WARNING,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.FIRST,
                        ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(7));

        // Add as last battery level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeBatteryAlert(ArsdkFeatureBattery.Alert.POWER_LEVEL,
                ArsdkFeatureBattery.AlertLevel.CRITICAL,
                ArsdkFeatureGeneric.ListFlags.toBitField(ArsdkFeatureGeneric.ListFlags.LAST)));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(8));

        // Alert none
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.NONE));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(9));

        // Motor error gone
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3SettingsStateMotorErrorStateChanged(
                1, ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.NOERROR));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(10));

        // Too much angle should not change any alarms
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.TOO_MUCH_ANGLE));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(10));

        // Magnetometer perturbation
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.MAGNETO_PERTUBATION));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(11));

        // Magnetometer low earth field
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.MAGNETO_LOW_EARTH_FIELD));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mChangeCnt, is(12));

        // Alert none
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateAlertStateChanged(
                ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState.NONE));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(13));
    }

    @Test
    public void testAlarmsHovering() {
        connectDrone(mDrone, 1);
        // check default values
        for (Alarms.Alarm.Kind kind : Alarms.Alarm.Kind.values()) {
            Alarms.Alarm alarm = mAlarms.getAlarm(kind);

            assertThat(alarm.getLevel(), is(Alarms.Alarm.Level.OFF));
        }
        assertThat(mChangeCnt, is(1));

        // hovering warning event, too high and too dark, landed
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateHoveringWarning(1, 1));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(1));

        // hovering warning event, too high and too dark, flying
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(2));

        // hovering warning event, too high and too dark, hovering
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.HOVERING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(2));

        // hovering warning event, too high and too dark, landing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.LANDING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(3));

        // hovering warning event, too dark only, landing
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateHoveringWarning(1, 0));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(3));

        // hovering warning event, too dark only, hovering
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.HOVERING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(4));

        // hovering warning event, too dark only, flying
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(4));

        // hovering warning event, no more warning
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateHoveringWarning(0, 0));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.POWER).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_ERROR).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MOTOR_CUT_OUT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.USER_EMERGENCY).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_HOT).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.BATTERY_TOO_COLD).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mChangeCnt, is(5));
    }

    @Test
    public void testAutomaticLanding() {
        connectDrone(mDrone, 1);

        // check default value
        assertThat(mChangeCnt, is(1));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.AUTOMATIC_LANDING_BATTERY_ISSUE).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.automaticLandingDelay(), is(0));

        // drone sends auto landing warning
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateForcedLandingAutoTrigger(
                ArsdkFeatureArdrone3.PilotingstateForcedlandingautotriggerReason.BATTERY_CRITICAL_SOON, 60));

        assertThat(mChangeCnt, is(2));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.AUTOMATIC_LANDING_BATTERY_ISSUE).getLevel(),
                is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.automaticLandingDelay(), is(60));

        // drone sends auto landing warning + delay <= 3sec
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateForcedLandingAutoTrigger(
                ArsdkFeatureArdrone3.PilotingstateForcedlandingautotriggerReason.BATTERY_CRITICAL_SOON, 3));

        assertThat(mChangeCnt, is(3));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.AUTOMATIC_LANDING_BATTERY_ISSUE).getLevel(),
                is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.automaticLandingDelay(), is(3));

        // drone sends auto landing warning + delay 0 (auto landing starts)
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateForcedLandingAutoTrigger(
                ArsdkFeatureArdrone3.PilotingstateForcedlandingautotriggerReason.BATTERY_CRITICAL_SOON, 0));

        assertThat(mChangeCnt, is(4));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.AUTOMATIC_LANDING_BATTERY_ISSUE).getLevel(),
                is(Alarms.Alarm.Level.CRITICAL));
        assertThat(mAlarms.automaticLandingDelay(), is(0));

        // drone sends auto landing warning + delay > 3
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateForcedLandingAutoTrigger(
                ArsdkFeatureArdrone3.PilotingstateForcedlandingautotriggerReason.BATTERY_CRITICAL_SOON, 10));

        assertThat(mChangeCnt, is(5));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.AUTOMATIC_LANDING_BATTERY_ISSUE).getLevel(),
                is(Alarms.Alarm.Level.WARNING));
        assertThat(mAlarms.automaticLandingDelay(), is(10));

        // drone sends auto landing warning off
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateForcedLandingAutoTrigger(
                ArsdkFeatureArdrone3.PilotingstateForcedlandingautotriggerReason.NONE, 10));

        assertThat(mChangeCnt, is(6));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.AUTOMATIC_LANDING_BATTERY_ISSUE).getLevel(),
                is(Alarms.Alarm.Level.OFF));
        assertThat(mAlarms.automaticLandingDelay(), is(0));
    }

    @Test
    public void testStrongWind() {
        connectDrone(mDrone, 1);

        // check default value
        assertThat(mChangeCnt, is(1));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.STRONG_WIND).getLevel(), is(Alarms.Alarm.Level.OFF));

        // drone sends strong wind warning
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateWindStateChanged(
                ArsdkFeatureArdrone3.PilotingstateWindstatechangedState.WARNING));

        assertThat(mChangeCnt, is(2));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.STRONG_WIND).getLevel(), is(Alarms.Alarm.Level.WARNING));

        // drone sends wind ok
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateWindStateChanged(
                ArsdkFeatureArdrone3.PilotingstateWindstatechangedState.OK));

        assertThat(mChangeCnt, is(3));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.STRONG_WIND).getLevel(), is(Alarms.Alarm.Level.OFF));

        // drone sends strong wind at the critical level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateWindStateChanged(
                ArsdkFeatureArdrone3.PilotingstateWindstatechangedState.CRITICAL));

        assertThat(mChangeCnt, is(4));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.STRONG_WIND).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
    }

    @Test
    public void testVerticalCamera() {
        connectDrone(mDrone, 1);

        // check default value
        assertThat(mChangeCnt, is(1));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));

        // drones sends vertical camera KO
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateSensorsStatesListChanged(
                ArsdkFeatureCommon.CommonstateSensorsstateslistchangedSensorname.VERTICAL_CAMERA, 0));

        assertThat(mChangeCnt, is(2));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.CRITICAL));

        // drones sends vertical camera OK
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeCommonCommonStateSensorsStatesListChanged(
                ArsdkFeatureCommon.CommonstateSensorsstateslistchangedSensorname.VERTICAL_CAMERA, 1));

        assertThat(mChangeCnt, is(3));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.VERTICAL_CAMERA).getLevel(), is(Alarms.Alarm.Level.OFF));
    }

    @Test
    public void testStrongVibrations() {
        connectDrone(mDrone, 1);

        // check default value
        assertThat(mChangeCnt, is(1));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.STRONG_VIBRATIONS).getLevel(), is(Alarms.Alarm.Level.OFF));

        // drone sends strong vibrations warning
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateVibrationLevelChanged(
                ArsdkFeatureArdrone3.PilotingstateVibrationlevelchangedState.WARNING));

        assertThat(mChangeCnt, is(2));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.STRONG_VIBRATIONS).getLevel(), is(Alarms.Alarm.Level.WARNING));

        // drone sends vibrations ok
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateVibrationLevelChanged(
                ArsdkFeatureArdrone3.PilotingstateVibrationlevelchangedState.OK));

        assertThat(mChangeCnt, is(3));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.STRONG_VIBRATIONS).getLevel(), is(Alarms.Alarm.Level.OFF));

        // drone sends strong vibrations at the critical level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateVibrationLevelChanged(
                ArsdkFeatureArdrone3.PilotingstateVibrationlevelchangedState.CRITICAL));

        assertThat(mChangeCnt, is(4));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.STRONG_VIBRATIONS).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
    }

    @Test
    public void testControllerLocationUnreliable() {
        connectDrone(mDrone, 1);

        // check default value
        assertThat(mChangeCnt, is(1));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.UNRELIABLE_CONTROLLER_LOCATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));

        // drone sends controller location unreliable
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeControllerInfoValidityFromDrone(0));

        assertThat(mChangeCnt, is(2));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.UNRELIABLE_CONTROLLER_LOCATION).getLevel(),
                is(Alarms.Alarm.Level.WARNING));

        // drone sends controller location reliable
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeControllerInfoValidityFromDrone(1));

        assertThat(mChangeCnt, is(3));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.UNRELIABLE_CONTROLLER_LOCATION).getLevel(),
                is(Alarms.Alarm.Level.OFF));
    }

    @Test
    public void testHeadingLock() {
        connectDrone(mDrone, 1);

        // check default value
        assertThat(mChangeCnt, is(1));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HEADING_LOCK).getLevel(), is(Alarms.Alarm.Level.OFF));

        // drone sends heading lock warning
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateHeadingLockedStateChanged(
                ArsdkFeatureArdrone3.PilotingstateHeadinglockedstatechangedState.WARNING));

        assertThat(mChangeCnt, is(2));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HEADING_LOCK).getLevel(), is(Alarms.Alarm.Level.WARNING));

        // drone sends heading lock ok
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateHeadingLockedStateChanged(
                ArsdkFeatureArdrone3.PilotingstateHeadinglockedstatechangedState.OK));

        assertThat(mChangeCnt, is(3));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HEADING_LOCK).getLevel(), is(Alarms.Alarm.Level.OFF));

        // drone sends heading lock at the critical level
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateHeadingLockedStateChanged(
                ArsdkFeatureArdrone3.PilotingstateHeadinglockedstatechangedState.CRITICAL));

        assertThat(mChangeCnt, is(4));
        assertThat(mAlarms.getAlarm(Alarms.Alarm.Kind.HEADING_LOCK).getLevel(), is(Alarms.Alarm.Level.CRITICAL));
    }
}
