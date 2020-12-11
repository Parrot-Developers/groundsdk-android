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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parrot.drone.groundsdk.arsdkengine.devicecontroller.DroneController;
import com.parrot.drone.groundsdk.arsdkengine.instrument.DroneInstrumentController;
import com.parrot.drone.groundsdk.device.instrument.Alarms;
import com.parrot.drone.groundsdk.internal.device.instrument.AlarmsCore;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureBattery;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureCommon;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureControllerInfo;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureGeneric;
import com.parrot.drone.sdkcore.arsdk.command.ArsdkCommand;

/** Alarms instrument controller for the Anafi family drones. */
public class AnafiAlarms extends DroneInstrumentController {

    /** The alarms from which this object is the backend. */
    @NonNull
    private final AlarmsCore mAlarms;

    /** {@code true} if the drone has sent alarm with the feature ArsdkFeatureBattery. */
    private boolean mBatteryFeatureSupported;

    /** {@code true} if the drone is flying, otherwise {@code false}. */
    private boolean mIsFlying;

    /** Latest {@link Alarms.Alarm.Kind#HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK} alarm level received from the drone. */
    @NonNull
    private Alarms.Alarm.Level mDroneHoveringTooDarkAlarmLevel;

    /** Latest {@link Alarms.Alarm.Kind#HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH} alarm level received from the drone. */
    @NonNull
    private Alarms.Alarm.Level mDroneHoveringTooHighAlarmLevel;

    /**
     * Constructor.
     *
     * @param droneController The drone controller that owns this component controller.
     */
    public AnafiAlarms(@NonNull DroneController droneController) {
        super(droneController);
        mAlarms = new AlarmsCore(mComponentStore)
                .updateAlarmsLevel(Alarms.Alarm.Level.OFF, Alarms.Alarm.Kind.values());
        mDroneHoveringTooDarkAlarmLevel = Alarms.Alarm.Level.OFF;
        mDroneHoveringTooHighAlarmLevel = Alarms.Alarm.Level.OFF;
    }

    @Override
    public void onConnected() {
        mAlarms.publish();
    }

    @Override
    public void onDisconnected() {
        mAlarms.unpublish();
    }

    @Override
    public void onCommandReceived(@NonNull ArsdkCommand command) {
        int featureId = command.getFeatureId();
        if (featureId == ArsdkFeatureArdrone3.PilotingState.UID) {
            ArsdkFeatureArdrone3.PilotingState.decode(command, mPilotingStateCallback);
        } else if (featureId == ArsdkFeatureArdrone3.SettingsState.UID) {
            ArsdkFeatureArdrone3.SettingsState.decode(command, mSettingStateCallback);
        } else if (featureId == ArsdkFeatureBattery.UID) {
            ArsdkFeatureBattery.decode(command, mBatteryCallback);
        } else if (featureId == ArsdkFeatureCommon.CommonState.UID) {
            ArsdkFeatureCommon.CommonState.decode(command, mCommonStateCallback);
        } else if (featureId == ArsdkFeatureControllerInfo.UID) {
            ArsdkFeatureControllerInfo.decode(command, mControllerInfoCallback);
        }
    }

    /**
     * Computes and updates current level of hovering difficulties alarms.
     * <p>
     * When the drone is not flying, hovering difficulties alarms level is always {@code OFF}.
     * <p>
     * When the drone is flying, the latest alarms levels received from the drone are applied.
     */
    private void computeHoveringDifficulties() {
        if (mIsFlying) {
            mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK,
                    mDroneHoveringTooDarkAlarmLevel)
                   .updateAlarmLevel(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH,
                           mDroneHoveringTooHighAlarmLevel)
                   .notifyUpdated();
        } else {
            mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_DARK, Alarms.Alarm.Level.OFF)
                   .updateAlarmLevel(Alarms.Alarm.Kind.HOVERING_DIFFICULTIES_NO_GPS_TOO_HIGH, Alarms.Alarm.Level.OFF)
                   .notifyUpdated();
        }
    }

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.PilotingState is decoded. */
    private final ArsdkFeatureArdrone3.PilotingState.Callback mPilotingStateCallback =
            new ArsdkFeatureArdrone3.PilotingState.Callback() {

                @Override
                public void onFlyingStateChanged(
                        @Nullable ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState state) {
                    boolean isFlying = state == ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.HOVERING
                                       || state == ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING;
                    if (mIsFlying != isFlying) {
                        mIsFlying = isFlying;
                        computeHoveringDifficulties();
                    }
                }

                @Override
                public void onAlertStateChanged(
                        @Nullable ArsdkFeatureArdrone3.PilotingstateAlertstatechangedState state) {
                    if (state != null) switch (state) {
                        case NONE:
                            // remove all alarms linked to this command
                            if (!mBatteryFeatureSupported) {
                                // the drone has not sent alarms using battery feature, so manage battery alarms here
                                mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.POWER, Alarms.Alarm.Level.OFF);
                            }
                            mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.MOTOR_CUT_OUT, Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.USER_EMERGENCY, Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION,
                                           Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD,
                                           Alarms.Alarm.Level.OFF)
                                   .notifyUpdated();
                            break;
                        case CRITICAL_BATTERY:
                        case ALMOST_EMPTY_BATTERY:
                            if (!mBatteryFeatureSupported) {
                                // the drone has not sent alarms using battery feature, so manage battery alarms here
                                mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.MOTOR_CUT_OUT, Alarms.Alarm.Level.OFF)
                                       .updateAlarmLevel(Alarms.Alarm.Kind.USER_EMERGENCY, Alarms.Alarm.Level.OFF)
                                       .updateAlarmLevel(Alarms.Alarm.Kind.POWER, Alarms.Alarm.Level.CRITICAL)
                                       .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION,
                                               Alarms.Alarm.Level.OFF)
                                       .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD,
                                               Alarms.Alarm.Level.OFF)
                                       .notifyUpdated();
                            } // else, do nothing, the drone as sent battery alarms using battery feature
                            break;
                        case LOW_BATTERY:
                            if (!mBatteryFeatureSupported) {
                                // the drone has not sent alarms using battery feature, so manage battery alarms here
                                mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.MOTOR_CUT_OUT, Alarms.Alarm.Level.OFF)
                                       .updateAlarmLevel(Alarms.Alarm.Kind.USER_EMERGENCY, Alarms.Alarm.Level.OFF)
                                       .updateAlarmLevel(Alarms.Alarm.Kind.POWER, Alarms.Alarm.Level.WARNING)
                                       .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION,
                                               Alarms.Alarm.Level.OFF)
                                       .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD,
                                               Alarms.Alarm.Level.OFF)
                                       .notifyUpdated();
                            } // else, do nothing, the drone as sent battery alarms using battery feature
                            break;
                        case CUT_OUT:
                            // remove only non-persistent alarms
                            mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.USER_EMERGENCY, Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MOTOR_CUT_OUT, Alarms.Alarm.Level.CRITICAL)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION,
                                           Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD,
                                           Alarms.Alarm.Level.OFF)
                                   .notifyUpdated();
                            break;
                        case TOO_MUCH_ANGLE:
                            // Nothing to do since we don't provide an alarm in the API for this alert
                            break;
                        case USER:
                            // remove only non-persistent alarms
                            mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.MOTOR_CUT_OUT, Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.USER_EMERGENCY, Alarms.Alarm.Level.CRITICAL)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION,
                                           Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD,
                                           Alarms.Alarm.Level.OFF)
                                   .notifyUpdated();
                            break;
                        case MAGNETO_PERTUBATION:
                            // remove only non-persistent alarms
                            mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.USER_EMERGENCY, Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MOTOR_CUT_OUT, Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD,
                                           Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION,
                                           Alarms.Alarm.Level.CRITICAL)
                                   .notifyUpdated();
                            break;
                        case MAGNETO_LOW_EARTH_FIELD:
                            // remove only non-persistent alarms
                            mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.USER_EMERGENCY, Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MOTOR_CUT_OUT, Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_PERTURBATION,
                                           Alarms.Alarm.Level.OFF)
                                   .updateAlarmLevel(Alarms.Alarm.Kind.MAGNETOMETER_LOW_EARTH_FIELD,
                                           Alarms.Alarm.Level.CRITICAL)
                                   .notifyUpdated();
                            break;
                    }
                }

                @Override
                public void onHoveringWarning(int noGpsTooDark, int noGpsTooHigh) {
                    mDroneHoveringTooDarkAlarmLevel = noGpsTooDark == 0 ?
                            Alarms.Alarm.Level.OFF : Alarms.Alarm.Level.WARNING;
                    mDroneHoveringTooHighAlarmLevel = noGpsTooHigh == 0 ?
                            Alarms.Alarm.Level.OFF : Alarms.Alarm.Level.WARNING;
                    computeHoveringDifficulties();
                }

                /** Automatic landing delay, in seconds, below which the alarm is CRITICAL. */
                private static final int AUTO_LANDING_DELAY_CRITICAL = 3;

                @Override
                public void onForcedLandingAutoTrigger(
                        @Nullable ArsdkFeatureArdrone3.PilotingstateForcedlandingautotriggerReason reason,
                        long delay) {
                    if (reason != null) {
                        switch (reason) {
                            case NONE:
                                mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.AUTOMATIC_LANDING_BATTERY_ISSUE,
                                        Alarms.Alarm.Level.OFF)
                                       .updateAutoLandingDelay(0);
                                break;
                            case BATTERY_CRITICAL_SOON:
                                mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.AUTOMATIC_LANDING_BATTERY_ISSUE,
                                        delay > AUTO_LANDING_DELAY_CRITICAL ?
                                                Alarms.Alarm.Level.WARNING : Alarms.Alarm.Level.CRITICAL)
                                       .updateAutoLandingDelay((int) delay);
                                break;
                        }
                        mAlarms.notifyUpdated();
                    }
                }

                @Override
                public void onWindStateChanged(
                        @Nullable ArsdkFeatureArdrone3.PilotingstateWindstatechangedState state) {
                    if (state != null) {
                        Alarms.Alarm.Level level = null;
                        switch (state) {
                            case OK:
                                level = Alarms.Alarm.Level.OFF;
                                break;
                            case WARNING:
                                level = Alarms.Alarm.Level.WARNING;
                                break;
                            case CRITICAL:
                                level = Alarms.Alarm.Level.CRITICAL;
                                break;
                        }
                        mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.STRONG_WIND, level).notifyUpdated();
                    }
                }

                @Override
                public void onVibrationLevelChanged(
                        @Nullable ArsdkFeatureArdrone3.PilotingstateVibrationlevelchangedState state) {
                    if (state != null) {
                        Alarms.Alarm.Level level = null;
                        switch (state) {
                            case OK:
                                level = Alarms.Alarm.Level.OFF;
                                break;
                            case WARNING:
                                level = Alarms.Alarm.Level.WARNING;
                                break;
                            case CRITICAL:
                                level = Alarms.Alarm.Level.CRITICAL;
                                break;
                        }
                        mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.STRONG_VIBRATIONS, level).notifyUpdated();
                    }
                }

                @Override
                public void onHeadingLockedStateChanged(
                        @Nullable ArsdkFeatureArdrone3.PilotingstateHeadinglockedstatechangedState state) {
                    if (state != null) {
                        Alarms.Alarm.Level level = null;
                        switch (state) {
                            case OK:
                                level = Alarms.Alarm.Level.OFF;
                                break;
                            case WARNING:
                                level = Alarms.Alarm.Level.WARNING;
                                break;
                            case CRITICAL:
                                level = Alarms.Alarm.Level.CRITICAL;
                                break;
                        }
                        mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.HEADING_LOCK, level).notifyUpdated();
                    }
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureArdrone3.SettingsState is decoded. */
    private final ArsdkFeatureArdrone3.SettingsState.Callback mSettingStateCallback =
            new ArsdkFeatureArdrone3.SettingsState.Callback() {

                @Override
                public void onMotorErrorStateChanged(
                        int motorIds,
                        @Nullable ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror motorError) {
                    if (motorError == ArsdkFeatureArdrone3.SettingsstateMotorerrorstatechangedMotorerror.NOERROR) {
                        mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.MOTOR_ERROR, Alarms.Alarm.Level.OFF);
                    } else {
                        mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.MOTOR_ERROR, Alarms.Alarm.Level.CRITICAL);
                    }
                    mAlarms.notifyUpdated();
                }
            };

    /** Callbacks called when a command of the feature ArsdkFeatureBattery is decoded. */
    private final ArsdkFeatureBattery.Callback mBatteryCallback = new ArsdkFeatureBattery.Callback() {

        @Override
        public void onAlert(@Nullable ArsdkFeatureBattery.Alert alert, @Nullable ArsdkFeatureBattery.AlertLevel level,
                            int listFlagsBitField) {
            // declare that the drone supports the battery feature
            mBatteryFeatureSupported = true;

            if (ArsdkFeatureGeneric.ListFlags.EMPTY.inBitField(listFlagsBitField)) {
                // remove all and notify
                mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.POWER, Alarms.Alarm.Level.OFF)
                       .updateAlarmLevel(Alarms.Alarm.Kind.BATTERY_TOO_COLD, Alarms.Alarm.Level.OFF)
                       .updateAlarmLevel(Alarms.Alarm.Kind.BATTERY_TOO_HOT, Alarms.Alarm.Level.OFF)
                       .notifyUpdated();
            } else if (alert != null) {
                Alarms.Alarm.Kind alarm = null;
                switch (alert) {
                    case POWER_LEVEL:
                        alarm = Alarms.Alarm.Kind.POWER;
                        break;
                    case TOO_COLD:
                        alarm = Alarms.Alarm.Kind.BATTERY_TOO_COLD;
                        break;
                    case TOO_HOT:
                        alarm = Alarms.Alarm.Kind.BATTERY_TOO_HOT;
                        break;
                }
                if (ArsdkFeatureGeneric.ListFlags.REMOVE.inBitField(listFlagsBitField)) {
                    // remove
                    mAlarms.updateAlarmLevel(alarm, Alarms.Alarm.Level.OFF);
                } else {
                    // first, remove all
                    if (ArsdkFeatureGeneric.ListFlags.FIRST.inBitField(listFlagsBitField)) {
                        mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.POWER, Alarms.Alarm.Level.OFF)
                               .updateAlarmLevel(Alarms.Alarm.Kind.BATTERY_TOO_COLD, Alarms.Alarm.Level.OFF)
                               .updateAlarmLevel(Alarms.Alarm.Kind.BATTERY_TOO_HOT, Alarms.Alarm.Level.OFF);
                    }
                    if (level != null) {
                        Alarms.Alarm.Level alarmLevel = null;
                        switch (level) {
                            case NONE:
                                alarmLevel = Alarms.Alarm.Level.OFF;
                                break;
                            case WARNING:
                                alarmLevel = Alarms.Alarm.Level.WARNING;
                                break;
                            case CRITICAL:
                                alarmLevel = Alarms.Alarm.Level.CRITICAL;
                                break;
                        }
                        // add
                        mAlarms.updateAlarmLevel(alarm, alarmLevel);
                    }
                }
                if (ArsdkFeatureGeneric.ListFlags.LAST.inBitField(listFlagsBitField)) {
                    // notify
                    mAlarms.notifyUpdated();
                }
            }
        }
    };

    /** Callbacks called when a command of the feature ArsdkFeatureCommon.CommonState is decoded. */
    private final ArsdkFeatureCommon.CommonState.Callback mCommonStateCallback
            = new ArsdkFeatureCommon.CommonState.Callback() {

        @Override
        public void onSensorsStatesListChanged(
                @Nullable ArsdkFeatureCommon.CommonstateSensorsstateslistchangedSensorname sensorName,
                int sensorState) {
            if (sensorName == ArsdkFeatureCommon.CommonstateSensorsstateslistchangedSensorname.VERTICAL_CAMERA) {
                Alarms.Alarm.Level alarmLevel = sensorState == 1 ?
                        Alarms.Alarm.Level.OFF : Alarms.Alarm.Level.CRITICAL;
                mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.VERTICAL_CAMERA, alarmLevel).notifyUpdated();
            }
        }
    };

    /** Callbacks called when a command of the feature ArsdkFeatureControllerInfo is decoded. */
    private final ArsdkFeatureControllerInfo.Callback mControllerInfoCallback =
            new ArsdkFeatureControllerInfo.Callback() {

        @Override
        public void onValidityFromDrone(int isValid) {
            mAlarms.updateAlarmLevel(Alarms.Alarm.Kind.UNRELIABLE_CONTROLLER_LOCATION,
                    isValid == 1 ? Alarms.Alarm.Level.OFF : Alarms.Alarm.Level.WARNING)
                   .notifyUpdated();
        }
    };
}
