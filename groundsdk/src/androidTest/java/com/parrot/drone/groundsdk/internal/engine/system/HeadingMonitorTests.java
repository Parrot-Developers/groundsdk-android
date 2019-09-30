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
import android.hardware.MockSensorEvent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.WindowManager;

import androidx.test.core.app.ApplicationProvider;

import com.parrot.drone.groundsdk.internal.utility.SystemHeading;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class HeadingMonitorTests {

    private static final int MOCK_SAMPLE_RATE = 100;

    private Context mMockContext;

    private SensorManager mMockSensorManager;

    private WindowManager mMockWindowManager;

    private Sensor mRotationSensor;

    private SystemEngine mSystemEngine;

    private SystemHeading mSystemHeading;

    private SystemHeading.Monitor mMonitor1, mMonitor2;

    @Before
    public void setup() {
        mMockContext = Mockito.mock(Context.class);
        mMockSensorManager = Mockito.mock(SensorManager.class);
        mMockWindowManager = Mockito.mock(WindowManager.class);
        // unfortunately we cannot mock sensor easily on android 28+ (internal API restricted to dark greylist)
        // so we use a real sensor from the device, which must support such sensor
        mRotationSensor = ApplicationProvider.getApplicationContext().getSystemService(SensorManager.class)
                                             .getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mMonitor1 = Mockito.mock(SystemHeading.Monitor.class);
        mMonitor2 = Mockito.mock(SystemHeading.Monitor.class);

        Mockito.when(mMockContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mMockSensorManager);

        mSystemEngine = new MockSystemEngine(mMockContext);
        mSystemHeading = new SystemHeadingCore(mSystemEngine, mMockSensorManager, mRotationSensor,
                mMockWindowManager, MOCK_SAMPLE_RATE);
        mSystemEngine.start();
    }

    @Test
    public void testNoSensor() {
        Mockito.when(mMockContext.getSystemService(Context.WINDOW_SERVICE)).thenReturn(mMockWindowManager);
        Mockito.when(mMockSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)).thenReturn(null);

        SystemHeading monitor = SystemHeadingCore.create(mSystemEngine);

        assertThat(monitor, nullValue());
    }

    @Test
    public void testWithSensor() {
        Mockito.when(mMockContext.getSystemService(Context.WINDOW_SERVICE)).thenReturn(mMockWindowManager);
        Mockito.when(mMockSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)).thenReturn(mRotationSensor);

        SystemHeading monitor = SystemHeadingCore.create(mSystemEngine);

        assertThat(monitor, notNullValue());
    }

    @Test
    public void testStartStopMonitoring() {
        ArgumentCaptor<SensorEventListener> sensorListenerCaptor = ArgumentCaptor.forClass(SensorEventListener.class);

        // ensure that the first observer registration triggers a monitoring start
        mSystemHeading.monitorWith(mMonitor1);

        Mockito.verify(mMockSensorManager, Mockito.times(1)).registerListener(sensorListenerCaptor.capture(),
                Mockito.eq(mRotationSensor), Mockito.eq((int) TimeUnit.MILLISECONDS.toMicros(MOCK_SAMPLE_RATE)));
        assertThat(sensorListenerCaptor.getValue(), notNullValue());

        // ensure that the second observer registration does not trigger a start
        mSystemHeading.monitorWith(mMonitor2);

        Mockito.verify(mMockSensorManager, Mockito.times(1)).registerListener(
                Mockito.any(), Mockito.any(), Mockito.anyInt());

        // ensure that unregistering an observer, if not last, does not trigger neither a start nor a stop
        mSystemHeading.disposeMonitor(mMonitor1);

        Mockito.verify(mMockSensorManager, Mockito.never()).unregisterListener(Mockito.<SensorEventListener>any());

        // ensure that unregistering the last observer does trigger a monitoring stop
        mSystemHeading.disposeMonitor(mMonitor2);

        Mockito.verify(mMockSensorManager, Mockito.times(1)).unregisterListener(sensorListenerCaptor.getValue());
    }

    @Test
    public void testSameObserver() {
        ArgumentCaptor<SensorEventListener> sensorListenerCaptor = ArgumentCaptor.forClass(SensorEventListener.class);

        // register twice the same observer
        mSystemHeading.monitorWith(mMonitor1);
        mSystemHeading.monitorWith(mMonitor1);

        Mockito.verify(mMockSensorManager, Mockito.times(1)).registerListener(sensorListenerCaptor.capture(),
                Mockito.eq(mRotationSensor), Mockito.eq((int) TimeUnit.MILLISECONDS.toMicros(MOCK_SAMPLE_RATE)));
        assertThat(sensorListenerCaptor.getValue(), notNullValue());

        // mock a notification
        SensorEvent event = MockSensorEvent.createWithSize(3);
        event.sensor = mRotationSensor;
        event.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM;
        event.values[0] = 0.3377787f;
        event.values[1] = 0.23961903f;
        event.values[2] = 0.54913014f;
        sensorListenerCaptor.getValue().onSensorChanged(event);

        // the listener should be notified only once
        Mockito.verify(mMonitor1, Mockito.times(1)).onHeadingChanged(Mockito.anyDouble());
    }

    @Test
    public void testMultipleObservers() {
        ArgumentCaptor<SensorEventListener> sensorListenerCaptor = ArgumentCaptor.forClass(SensorEventListener.class);

        // register two observers
        mSystemHeading.monitorWith(mMonitor1);
        mSystemHeading.monitorWith(mMonitor2);

        Mockito.verify(mMockSensorManager, Mockito.times(1)).registerListener(sensorListenerCaptor.capture(),
                Mockito.eq(mRotationSensor), Mockito.eq((int) TimeUnit.MILLISECONDS.toMicros(MOCK_SAMPLE_RATE)));
        assertThat(sensorListenerCaptor.getValue(), notNullValue());

        // mock a notification
        SensorEvent event = MockSensorEvent.createWithSize(3);
        event.sensor = mRotationSensor;
        event.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM;
        event.values[0] = 0.3377787f;
        event.values[1] = 0.23961903f;
        event.values[2] = 0.54913014f;
        sensorListenerCaptor.getValue().onSensorChanged(event);

        // both listener should be notified
        Mockito.verify(mMonitor1, Mockito.times(1)).onHeadingChanged(Mockito.anyDouble());
        Mockito.verify(mMonitor2, Mockito.times(1)).onHeadingChanged(Mockito.anyDouble());
    }

    @Test
    public void testRegisterUnregister() {
        ArgumentCaptor<SensorEventListener> sensorListenerCaptor = ArgumentCaptor.forClass(SensorEventListener.class);

        // register an observer
        mSystemHeading.monitorWith(mMonitor1);

        Mockito.verify(mMockSensorManager, Mockito.times(1)).registerListener(sensorListenerCaptor.capture(),
                Mockito.eq(mRotationSensor), Mockito.eq((int) TimeUnit.MILLISECONDS.toMicros(MOCK_SAMPLE_RATE)));
        assertThat(sensorListenerCaptor.getValue(), notNullValue());

        // mock a notification
        SensorEvent event = MockSensorEvent.createWithSize(3);
        event.sensor = mRotationSensor;
        event.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM;
        event.values[0] = 0.3377787f;
        event.values[1] = 0.23961903f;
        event.values[2] = 0.54913014f;
        sensorListenerCaptor.getValue().onSensorChanged(event);

        // listener should be notified
        Mockito.verify(mMonitor1, Mockito.times(1)).onHeadingChanged(Mockito.anyDouble());

        // register another observer
        mSystemHeading.monitorWith(mMonitor2);
        // unregister the first one
        mSystemHeading.disposeMonitor(mMonitor1);

        // mock another notification
        sensorListenerCaptor.getValue().onSensorChanged(event);

        // the first listener should not have been notified
        Mockito.verify(mMonitor1, Mockito.times(1)).onHeadingChanged(Mockito.anyDouble());
        // and the second one should have been notified
        Mockito.verify(mMonitor2, Mockito.times(1)).onHeadingChanged(Mockito.anyDouble());

    }

    @Test
    public void testImproperAccuracy() {
        ArgumentCaptor<SensorEventListener> sensorListenerCaptor = ArgumentCaptor.forClass(SensorEventListener.class);

        // register an observer
        mSystemHeading.monitorWith(mMonitor1);

        Mockito.verify(mMockSensorManager, Mockito.times(1)).registerListener(sensorListenerCaptor.capture(),
                Mockito.eq(mRotationSensor), Mockito.eq((int) TimeUnit.MILLISECONDS.toMicros(MOCK_SAMPLE_RATE)));
        assertThat(sensorListenerCaptor.getValue(), notNullValue());

        // mock a low event accuracy
        SensorEvent event = MockSensorEvent.createWithSize(3);
        event.sensor = mRotationSensor;
        event.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_LOW;
        event.values[0] = 0.3377787f;
        event.values[1] = 0.23961903f;
        event.values[2] = 0.54913014f;
        sensorListenerCaptor.getValue().onSensorChanged(event);

        // listener should not be notified
        Mockito.verify(mMonitor1, Mockito.never()).onHeadingChanged(Mockito.anyDouble());

        // mock an unreliable event accuracy
        event.accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
        sensorListenerCaptor.getValue().onSensorChanged(event);

        // listener should not be notified
        Mockito.verify(mMonitor1, Mockito.never()).onHeadingChanged(Mockito.anyDouble());
    }

    @Test
    public void testProperAccuracy() {
        ArgumentCaptor<SensorEventListener> sensorListenerCaptor = ArgumentCaptor.forClass(SensorEventListener.class);

        // register an observer
        mSystemHeading.monitorWith(mMonitor1);

        Mockito.verify(mMockSensorManager, Mockito.times(1)).registerListener(sensorListenerCaptor.capture(),
                Mockito.eq(mRotationSensor), Mockito.eq((int) TimeUnit.MILLISECONDS.toMicros(MOCK_SAMPLE_RATE)));
        assertThat(sensorListenerCaptor.getValue(), notNullValue());

        // mock a low event accuracy
        SensorEvent event = MockSensorEvent.createWithSize(3);
        event.sensor = mRotationSensor;
        event.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
        event.values[0] = 0f;
        event.values[1] = 1f;
        event.values[2] = 0f;
        sensorListenerCaptor.getValue().onSensorChanged(event);

        // listener should be notified
        Mockito.verify(mMonitor1, Mockito.times(1)).onHeadingChanged(0);
    }
}
