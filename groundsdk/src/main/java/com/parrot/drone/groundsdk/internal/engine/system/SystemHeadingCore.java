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
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.parrot.drone.groundsdk.internal.utility.SystemHeading;
import com.parrot.drone.sdkcore.ulog.ULog;

import java.util.concurrent.TimeUnit;

import static com.parrot.drone.groundsdk.internal.Logging.TAG_MONITOR;

/**
 * Implementation class for {@code SystemHeading} monitoring utility.
 */
public class SystemHeadingCore extends MonitorableCore<SystemHeading.Monitor> implements SystemHeading {

    /** Default rate at which rotation sensor is measured, in milliseconds. */
    private static final int DEFAULT_SAMPLE_RATE = 200;

    /**
     * Creates a {@code SystemHeadingCore}.
     *
     * @param engine     engine that manages this monitorable component
     * @param sampleRate rate at which rotation sensor shall be measured, in milliseconds
     *
     * @return a new {@code SystemHeadingCore} instance if the user device provides a suitable rotation sensor (fusion
     *         of accelerometer, gyroscope and magnetometer hardware sensors), otherwise {@code null}
     */
    @Nullable
    public static SystemHeadingCore create(@NonNull SystemEngine engine, int sampleRate) {
        SensorManager sensorManager = (SensorManager) engine.getContext().getSystemService(Context.SENSOR_SERVICE);
        WindowManager windowManager = (WindowManager) engine.getContext().getSystemService(Context.WINDOW_SERVICE);
        if (sensorManager != null && windowManager != null) {
            Sensor rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (rotationSensor != null) {
                return new SystemHeadingCore(engine, sensorManager, rotationSensor, windowManager, sampleRate);
            }
        }
        return null;
    }

    /**
     * Creates a {@code SystemHeadingCore} working at {@link #DEFAULT_SAMPLE_RATE default sample rate}.
     *
     * @param engine engine that manages this monitorable component
     *
     * @return a new {@code SystemHeadingCore} instance if the user device provides a suitable rotation sensor (fusion
     *         of accelerometer, gyroscope and magnetometer hardware sensors), otherwise {@code null}
     */
    @Nullable
    public static SystemHeadingCore create(@NonNull SystemEngine engine) {
        return create(engine, DEFAULT_SAMPLE_RATE);
    }

    /** Android sensor manager. */
    @NonNull
    private final SensorManager mSensorManager;

    /** Rotation vector sensor. */
    @NonNull
    private final Sensor mRotationSensor;

    /** Sensor measurement rate, in microseconds. */
    private final int mSampleRate;

    /** Orientation event listener. */
    @NonNull
    private final OrientationEventListener mOrientationEventListener;

    /** Current screen rotation. */
    private int mScreenRotation;

    /**
     * Axis of the coordinate system of the current orientation that coincide with the X axis of the coordinate system
     * of the default device orientation.
     * <p>
     * This is computed when screen rotation changes, and cached to avoid computing it on every sensor change as this
     * occurs very often compared to screen rotation.
     */
    private int mAxisX;

    /**
     * Axis of the coordinate system of the current orientation that coincide with the Y axis of the coordinate system
     * of the default device orientation.
     * <p>
     * This is computed when screen rotation changes, and cached to avoid computing it on every sensor change as this
     * occurs very often compared to screen rotation.
     */
    private int mAxisY;

    /** Latest heading measure, in degrees. */
    private double mHeading;

    /**
     * Constructor.
     *
     * @param engine         engine that manages this monitorable component
     * @param sensorManager  android sensor manager
     * @param rotationSensor rotation vector sensor
     * @param windowManager  android window manager
     * @param sampleRate     rate at which rotation sensor shall be measured, in milliseconds
     */
    @VisibleForTesting
    SystemHeadingCore(@NonNull SystemEngine engine, @NonNull SensorManager sensorManager,
                      @NonNull Sensor rotationSensor, @NonNull WindowManager windowManager, int sampleRate) {
        super(engine);
        mSensorManager = sensorManager;
        mRotationSensor = rotationSensor;
        mSampleRate = (int) TimeUnit.MILLISECONDS.toMicros(sampleRate);
        mScreenRotation = Surface.ROTATION_0;
        computeAxes(mScreenRotation);

        mOrientationEventListener = new OrientationEventListener(engine.getContext()) {

            @Override
            public void onOrientationChanged(int orientation) {
                int rotation = windowManager.getDefaultDisplay().getRotation();
                if (mScreenRotation != rotation) {
                    mScreenRotation = rotation;
                    computeAxes(mScreenRotation);
                }
            }
        };
    }

    @Override
    protected void onFirstMonitor(@NonNull Monitor monitor) {
        ULog.d(TAG_MONITOR, "Start monitoring device heading");
        mSensorManager.registerListener(mRotationSensorListener, mRotationSensor, mSampleRate);
        mOrientationEventListener.enable();
    }

    @Override
    protected void onNoMoreMonitors() {
        mSensorManager.unregisterListener(mRotationSensorListener);
        mOrientationEventListener.disable();
        ULog.d(TAG_MONITOR, "Stopped monitoring device heading");
    }

    @Override
    protected void notifyMonitor(@NonNull Monitor monitor) {
        monitor.onHeadingChanged(mHeading);
    }

    /**
     * Updates the axes used to compute heading according to the current screen rotation.
     *
     * @param screenRotation current screen rotation
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void computeAxes(int screenRotation) {
        switch (screenRotation) {
            case Surface.ROTATION_90:
                mAxisX = SensorManager.AXIS_Y;
                mAxisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
                mAxisX = SensorManager.AXIS_MINUS_X;
                mAxisY = SensorManager.AXIS_MINUS_Y;
                break;
            case Surface.ROTATION_270:
                mAxisX = SensorManager.AXIS_MINUS_Y;
                mAxisY = SensorManager.AXIS_X;
                break;
            case Surface.ROTATION_0:
            default:
                mAxisX = SensorManager.AXIS_X;
                mAxisY = SensorManager.AXIS_Y;
                break;
        }
    }

    /** Listens to rotation sensor measurements. */
    private final SensorEventListener mRotationSensorListener = new SensorEventListener() {

        /** A 4x4 rotation matrix used to compute heading according to the current screen rotation. */
        @NonNull
        private final float[] mRotationMatrix = new float[16];

        /** Vector containing the latest device orientation values (azimuth, pitch and roll), in radians. */
        @NonNull
        private final float[] mOrientation = new float[3];

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR &&
                event.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
                SensorManager.remapCoordinateSystem(mRotationMatrix, mAxisX, mAxisY, mRotationMatrix);
                SensorManager.getOrientation(mRotationMatrix, mOrientation);
                mHeading = (Math.toDegrees(mOrientation[0]) + 360) % 360;
                dispatchNotification();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
}
