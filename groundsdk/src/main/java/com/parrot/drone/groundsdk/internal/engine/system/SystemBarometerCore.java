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

package com.parrot.drone.groundsdk.internal.engine.system;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.internal.utility.SystemBarometer;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_MONITOR;

/**
 * Implementation class for {@code SystemBarometer} monitoring utility.
 */
final class SystemBarometerCore extends MonitorableCore<SystemBarometer.Monitor> implements SystemBarometer {

    /** Default rate at which barometer sensor is measured, in milliseconds. */
    @SuppressWarnings("WeakerAccess")
    public static final int DEFAULT_SAMPLE_RATE = 200;

    /**
     * Creates a {@code SystemBarometerCore}.
     *
     * @param engine     engine that manages this monitorable component
     * @param sampleRate rate at which barometer sensor shall be measured, in milliseconds
     *
     * @return a new {@code SystemBarometerCore} instance if the user device provides a suitable barometer sensor,
     *         otherwise {@code null}
     */
    @Nullable
    public static SystemBarometerCore create(@NonNull SystemEngine engine, int sampleRate) {
        SensorManager sensorManager = (SensorManager) engine.getContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor pressureSensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        return pressureSensor == null ? null : new SystemBarometerCore(
                engine, sensorManager, pressureSensor, sampleRate);
    }

    /**
     * Creates a {@code SystemBarometerCore} working at {@link #DEFAULT_SAMPLE_RATE default sample rate}.
     *
     * @param engine engine that manages this monitorable component
     *
     * @return a new {@code SystemBarometerCore} instance if the user device provides a suitable barometer sensor,
     *         otherwise {@code null}
     */
    @Nullable
    public static SystemBarometerCore create(@NonNull SystemEngine engine) {
        return create(engine, DEFAULT_SAMPLE_RATE);
    }

    /** Android sensor manager. */
    @NonNull
    private final SensorManager mSensorManager;

    /** Atmospheric pressure sensor. */
    @NonNull
    private final Sensor mPressureSensor;

    /** Sensor measurement rate, in microseconds. */
    private final int mSampleRate;

    /** Latest air pressure measure, in Pa. */
    private double mPressure;

    /** Latest measure timestamp, in nanoseconds. */
    private long mTimestamp;

    /**
     * Constructor.
     *
     * @param engine         engine that manages this monitorable component
     * @param sensorManager  android sensor manager
     * @param pressureSensor atmospheric pressure sensor
     * @param sampleRate     rate at which barometer sensor shall be measured, in milliseconds
     */
    @VisibleForTesting
    SystemBarometerCore(@NonNull SystemEngine engine, @NonNull SensorManager sensorManager,
                        @NonNull Sensor pressureSensor, int sampleRate) {
        super(engine);
        mSensorManager = sensorManager;
        mPressureSensor = pressureSensor;
        mSampleRate = (int) TimeUnit.MILLISECONDS.toMicros(sampleRate);
    }

    @Override
    protected void onFirstMonitor(@NonNull Monitor monitor) {
        ULog.d(TAG_MONITOR, "Starting to monitor device barometer sensor");
        mSensorManager.registerListener(mPressureSensorListener, mPressureSensor, mSampleRate);
    }

    @Override
    protected void onNoMoreMonitors() {
        mSensorManager.unregisterListener(mPressureSensorListener);
        ULog.d(TAG_MONITOR, "Stopped monitoring device barometer sensor");
    }

    @Override
    protected void notifyMonitor(@NonNull Monitor monitor) {
        monitor.onAirPressureMeasure(mPressure, mTimestamp);
    }

    /** Listens to barometer sensor measurements. */
    private final SensorEventListener mPressureSensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO: what is a proper accuracy level ?
            if (event.accuracy > SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                // android sensor measurements are in hPa. Convert them to Pa.
                mPressure = event.values[0] * 100;
                mTimestamp = event.timestamp;
                dispatchNotification();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
}
