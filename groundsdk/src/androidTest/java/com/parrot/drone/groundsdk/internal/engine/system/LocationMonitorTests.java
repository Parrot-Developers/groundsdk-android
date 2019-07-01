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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.parrot.drone.groundsdk.internal.utility.SystemLocation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class LocationMonitorTests {

    private static final int MOCK_PREFERRED_TIME_INTERVAL = 500;

    private static final int MOCK_FASTEST_TIME_INTERVAL = 100;

    private static final double MOCK_MIN_SPACE_INTERVAL = 1;

    private Context mMockContext;

    private LocationManager mMockLocationManager;

    private FusedLocationProviderClient mFusedLocationClient;

    private SystemLocation mSystemLocation;

    private SystemLocation.Monitor mMonitor1, mMonitor2;

    private Object mToken;

    @Before
    public void setup() {
        mMockContext = Mockito.mock(Context.class);
        mMockLocationManager = Mockito.mock(LocationManager.class);
        mMonitor1 = Mockito.mock(SystemLocation.Monitor.class);
        mMonitor2 = Mockito.mock(SystemLocation.Monitor.class);
        mToken = new Object();

        mFusedLocationClient = Mockito.mock(FusedLocationProviderClient.class);
        mSystemLocation = new SystemLocationCore(mMockContext, mMockLocationManager, mFusedLocationClient,
                MOCK_PREFERRED_TIME_INTERVAL, MOCK_FASTEST_TIME_INTERVAL, MOCK_MIN_SPACE_INTERVAL);
    }

    @Test
    public void testAuthorization() {
        mSystemLocation.denyWifiUsage(mToken);

        // when fine location permission is denied, monitoring couldn't start
        Mockito.when(mMockContext.checkCallingOrSelfPermission(ACCESS_FINE_LOCATION)).thenReturn(PERMISSION_DENIED);
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);

        mSystemLocation.monitorWith(mMonitor1);

        verify(mMonitor1, Mockito.never()).onAuthorizationChanged(true);
        verify(mMockLocationManager, Mockito.never()).requestLocationUpdates(
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyFloat(), Mockito.<LocationListener>any());

        // when device location service is disabled, monitoring couldn't start
        Mockito.when(mMockContext.checkCallingOrSelfPermission(ACCESS_FINE_LOCATION)).thenReturn(PERMISSION_GRANTED);
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false);
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(false);

        mSystemLocation.restartLocationUpdates();

        verify(mMonitor1, Mockito.never()).onAuthorizationChanged(true);
        verify(mMockLocationManager, Mockito.never()).requestLocationUpdates(
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyFloat(), Mockito.<LocationListener>any());

        // once permission is granted and device location is enabled, monitoring could start
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        mSystemLocation.restartLocationUpdates();

        verify(mMonitor1, Mockito.times(1)).onAuthorizationChanged(true);
        verify(mMockLocationManager, Mockito.times(1)).requestLocationUpdates(
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyFloat(), Mockito.<LocationListener>any());

        // trying to restart location updates while already started, nothing happens
        mSystemLocation.restartLocationUpdates();

        verify(mMockLocationManager, Mockito.times(1)).requestLocationUpdates(
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyFloat(), Mockito.<LocationListener>any());
    }

    @Test
    public void testMonitoringFusedLocation() {
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        ArgumentCaptor<LocationRequest> locationRequestCaptor = ArgumentCaptor.forClass(LocationRequest.class);
        ArgumentCaptor<LocationCallback> locationCallbackCaptor = ArgumentCaptor.forClass(LocationCallback.class);

        mSystemLocation.monitorWith(mMonitor1);

        verify(mFusedLocationClient, Mockito.times(1)).requestLocationUpdates(
                locationRequestCaptor.capture(), locationCallbackCaptor.capture(), eq(null));
        assertThat(locationRequestCaptor.getValue(), notNullValue());
        assertThat(locationRequestCaptor.getValue().getInterval(), is((long) MOCK_PREFERRED_TIME_INTERVAL));
        assertThat(locationRequestCaptor.getValue().getFastestInterval(), is((long) MOCK_FASTEST_TIME_INTERVAL));
        assertThat(locationRequestCaptor.getValue().getPriority(), is(LocationRequest.PRIORITY_HIGH_ACCURACY));
        assertThat(locationCallbackCaptor.getValue(), notNullValue());
    }

    @Test
    public void testMonitoringGpsLocation() {
        SystemLocation systemLocation = new SystemLocationCore(mMockContext, mMockLocationManager, null,
                MOCK_PREFERRED_TIME_INTERVAL, MOCK_FASTEST_TIME_INTERVAL, MOCK_MIN_SPACE_INTERVAL);

        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);
        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);

        systemLocation.monitorWith(mMonitor1);

        verify(mMockLocationManager, Mockito.times(1)).requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER), eq((long) MOCK_PREFERRED_TIME_INTERVAL),
                eq((float) MOCK_MIN_SPACE_INTERVAL), locationListenerCaptor.capture());
        assertThat(locationListenerCaptor.getValue(), notNullValue());
    }

    @Test
    public void testDenyWifiUsage() {
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);
        ArgumentCaptor<LocationRequest> locationRequestCaptor = ArgumentCaptor.forClass(LocationRequest.class);
        ArgumentCaptor<LocationCallback> locationCallbackCaptor = ArgumentCaptor.forClass(LocationCallback.class);
        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);
        Object token2 = new Object();

        // wifi usage is allowed, location monitoring uses fused provider
        mSystemLocation.monitorWith(mMonitor1);

        verify(mFusedLocationClient, Mockito.times(1)).requestLocationUpdates(
                locationRequestCaptor.capture(), locationCallbackCaptor.capture(), eq(null));

        // wifi usage is disallowed, location monitoring is restarted with GPS provider
        mSystemLocation.denyWifiUsage(mToken);

        verify(mMockLocationManager, Mockito.times(1)).requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER), eq((long) MOCK_PREFERRED_TIME_INTERVAL),
                eq((float) MOCK_MIN_SPACE_INTERVAL), locationListenerCaptor.capture());

        // wifi usage is disallowed for a second token, nothing happens
        mSystemLocation.denyWifiUsage(token2);

        verify(mMockLocationManager, Mockito.times(1)).requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER), eq((long) MOCK_PREFERRED_TIME_INTERVAL),
                eq((float) MOCK_MIN_SPACE_INTERVAL), locationListenerCaptor.capture());

        // wifi usage is forced, location monitoring is restarted with fused provider
        mSystemLocation.enforceWifiUsage(mToken);

        verify(mFusedLocationClient, Mockito.times(2)).requestLocationUpdates(
                locationRequestCaptor.capture(), locationCallbackCaptor.capture(), eq(null));

        // wifi usage not forced anymore, location monitoring is restarted with GPS provider
        mSystemLocation.revokeWifiUsageEnforcement(mToken);

        verify(mMockLocationManager, Mockito.times(2)).requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER), eq((long) MOCK_PREFERRED_TIME_INTERVAL),
                eq((float) MOCK_MIN_SPACE_INTERVAL), locationListenerCaptor.capture());

        // wifi usage is allowed for a token, nothing happens
        mSystemLocation.revokeWifiUsageDenial(mToken);

        verify(mMockLocationManager, Mockito.times(2)).requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER), eq((long) MOCK_PREFERRED_TIME_INTERVAL),
                eq((float) MOCK_MIN_SPACE_INTERVAL), locationListenerCaptor.capture());

        // wifi usage is allowed for all token, location monitoring is restarted with fused provider
        mSystemLocation.revokeWifiUsageDenial(token2);

        verify(mFusedLocationClient, Mockito.times(3)).requestLocationUpdates(
                locationRequestCaptor.capture(), locationCallbackCaptor.capture(), eq(null));
    }

    @Test
    public void testForceWifiUsage() {
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);
        ArgumentCaptor<LocationRequest> locationRequestCaptor = ArgumentCaptor.forClass(LocationRequest.class);
        ArgumentCaptor<LocationCallback> locationCallbackCaptor = ArgumentCaptor.forClass(LocationCallback.class);
        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);
        Object token2 = new Object();

        // wifi usage is allowed, location monitoring uses fused provider
        mSystemLocation.monitorWith(mMonitor1);

        verify(mFusedLocationClient, Mockito.times(1)).requestLocationUpdates(
                locationRequestCaptor.capture(), locationCallbackCaptor.capture(), eq(null));

        // wifi usage is forced, nothing happens
        mSystemLocation.enforceWifiUsage(mToken);

        verify(mFusedLocationClient, Mockito.times(1)).requestLocationUpdates(
                locationRequestCaptor.capture(), locationCallbackCaptor.capture(), eq(null));

        // wifi usage is forced for a second token, nothing happens
        mSystemLocation.enforceWifiUsage(token2);

        verify(mFusedLocationClient, Mockito.times(1)).requestLocationUpdates(
                locationRequestCaptor.capture(), locationCallbackCaptor.capture(), eq(null));

        // wifi usage is denied, nothing happens
        mSystemLocation.denyWifiUsage(mToken);

        verify(mFusedLocationClient, Mockito.times(1)).requestLocationUpdates(
                locationRequestCaptor.capture(), locationCallbackCaptor.capture(), eq(null));

        // wifi enforcement is revoked for a token, nothing happens
        mSystemLocation.revokeWifiUsageEnforcement(mToken);

        verify(mFusedLocationClient, Mockito.times(1)).requestLocationUpdates(
                locationRequestCaptor.capture(), locationCallbackCaptor.capture(), eq(null));

        // wifi enforcement is revoked for second token, location monitoring is restarted with GPS provider
        mSystemLocation.revokeWifiUsageEnforcement(token2);

        verify(mMockLocationManager, Mockito.times(1)).requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER), eq((long) MOCK_PREFERRED_TIME_INTERVAL),
                eq((float) MOCK_MIN_SPACE_INTERVAL), locationListenerCaptor.capture());

        // wifi usage is forced again, location monitoring is restarted with fused provider
        mSystemLocation.enforceWifiUsage(mToken);

        verify(mFusedLocationClient, Mockito.times(2)).requestLocationUpdates(
                locationRequestCaptor.capture(), locationCallbackCaptor.capture(), eq(null));

        // wifi denial is revoked, nothing happens
        mSystemLocation.revokeWifiUsageDenial(mToken);

        verify(mFusedLocationClient, Mockito.times(2)).requestLocationUpdates(
                locationRequestCaptor.capture(), locationCallbackCaptor.capture(), eq(null));

        // wifi enforcement is revoked, nothing happens
        mSystemLocation.revokeWifiUsageEnforcement(mToken);

        verify(mFusedLocationClient, Mockito.times(2)).requestLocationUpdates(
                locationRequestCaptor.capture(), locationCallbackCaptor.capture(), eq(null));

        // wifi usage is denied, location monitoring is restarted with GPS provider
        mSystemLocation.denyWifiUsage(mToken);

        verify(mMockLocationManager, Mockito.times(2)).requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER), eq((long) MOCK_PREFERRED_TIME_INTERVAL),
                eq((float) MOCK_MIN_SPACE_INTERVAL), locationListenerCaptor.capture());
    }


    @Test
    public void testStartStopMonitoring() {
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);
        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);

        mSystemLocation.denyWifiUsage(mToken);

        // ensure that the first observer registration triggers a monitoring start
        mSystemLocation.monitorWith(mMonitor1);

        verify(mMockLocationManager, Mockito.times(1)).requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER), eq((long) MOCK_PREFERRED_TIME_INTERVAL),
                eq((float) MOCK_MIN_SPACE_INTERVAL), locationListenerCaptor.capture());
        assertThat(locationListenerCaptor.getValue(), notNullValue());

        // ensure that the second observer registration does not trigger a start
        mSystemLocation.monitorWith(mMonitor2);

        verify(mMockLocationManager, Mockito.times(1)).requestLocationUpdates(
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyFloat(), Mockito.<LocationListener>any());

        // ensure that unregistering an observer, if not last, does not trigger neither a start nor a stop
        mSystemLocation.disposeMonitor(mMonitor1);

        verify(mMockLocationManager, Mockito.never()).removeUpdates(Mockito.<LocationListener>any());

        // ensure that unregistering the last observer does trigger a monitoring stop
        mSystemLocation.disposeMonitor(mMonitor2);

        verify(mMockLocationManager, Mockito.times(1)).removeUpdates(locationListenerCaptor.getValue());
        assertThat(locationListenerCaptor.getValue(), notNullValue());
    }

    @Test
    public void testPassiveMonitor() {
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);
        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);

        mSystemLocation.denyWifiUsage(mToken);

        SystemLocation.Monitor monitor3 = Mockito.mock(SystemLocation.Monitor.Passive.class);
        SystemLocation.Monitor monitor4 = Mockito.mock(SystemLocation.Monitor.Passive.class);

        // ensure that a passive monitor registration does not trigger a start
        mSystemLocation.monitorWith(monitor3);

        verify(mMockLocationManager, Mockito.never()).requestLocationUpdates(
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyFloat(), Mockito.<LocationListener>any());

        // ensure that a new passive monitor registration does not trigger a start
        mSystemLocation.monitorWith(monitor4);

        verify(mMockLocationManager, Mockito.never()).requestLocationUpdates(
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyFloat(), Mockito.<LocationListener>any());

        // ensure that the first active monitor registration triggers a monitoring start
        mSystemLocation.monitorWith(mMonitor1);

        verify(mMockLocationManager, Mockito.times(1)).requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER), eq((long) MOCK_PREFERRED_TIME_INTERVAL),
                eq((float) MOCK_MIN_SPACE_INTERVAL), locationListenerCaptor.capture());
        assertThat(locationListenerCaptor.getValue(), notNullValue());

        // ensure that unregistering a passive monitor does not trigger a stop
        mSystemLocation.disposeMonitor(monitor3);

        verify(mMockLocationManager, Mockito.never()).removeUpdates(Mockito.<LocationListener>any());

        // ensure that unregistering the last active monitor triggers a stop
        mSystemLocation.disposeMonitor(mMonitor1);

        verify(mMockLocationManager, Mockito.times(1)).removeUpdates(locationListenerCaptor.getValue());
        assertThat(locationListenerCaptor.getValue(), notNullValue());

        // ensure that unregistering the last passive monitor does not trigger a stop
        mSystemLocation.disposeMonitor(monitor4);

        verify(mMockLocationManager, Mockito.times(1)).removeUpdates(Mockito.<LocationListener>any());
    }

    @Test
    public void testSameObserver() {
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);
        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);

        mSystemLocation.denyWifiUsage(mToken);

        // register twice the same observer
        mSystemLocation.monitorWith(mMonitor1);
        mSystemLocation.monitorWith(mMonitor1);

        verify(mMockLocationManager, Mockito.times(1)).requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER), eq((long) MOCK_PREFERRED_TIME_INTERVAL),
                eq((float) MOCK_MIN_SPACE_INTERVAL), locationListenerCaptor.capture());
        assertThat(locationListenerCaptor.getValue(), notNullValue());

        Location location = new Location("provider");

        locationListenerCaptor.getValue().onLocationChanged(location);

        // the listener should be notified only once
        verify(mMonitor1, Mockito.times(1)).onLocationChanged(location);
    }

    @Test
    public void testMultipleObservers() {
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);
        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);

        mSystemLocation.denyWifiUsage(mToken);

        // register two observers
        mSystemLocation.monitorWith(mMonitor1);
        mSystemLocation.monitorWith(mMonitor2);

        verify(mMockLocationManager, Mockito.times(1)).requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER), eq((long) MOCK_PREFERRED_TIME_INTERVAL),
                eq((float) MOCK_MIN_SPACE_INTERVAL), locationListenerCaptor.capture());
        assertThat(locationListenerCaptor.getValue(), notNullValue());

        Location location = new Location("provider");

        locationListenerCaptor.getValue().onLocationChanged(location);

        // each listener should be notified
        verify(mMonitor1, Mockito.times(1)).onLocationChanged(location);
        verify(mMonitor2, Mockito.times(1)).onLocationChanged(location);
    }

    @Test
    public void testRegisterUnregister() {
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        Mockito.when(mMockLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);
        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);

        mSystemLocation.denyWifiUsage(mToken);

        // register an observer
        mSystemLocation.monitorWith(mMonitor1);

        verify(mMockLocationManager, Mockito.times(1)).requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER), eq((long) MOCK_PREFERRED_TIME_INTERVAL),
                eq((float) MOCK_MIN_SPACE_INTERVAL), locationListenerCaptor.capture());
        assertThat(locationListenerCaptor.getValue(), notNullValue());

        // mock a location update
        Location location = new Location("provider");
        locationListenerCaptor.getValue().onLocationChanged(location);

        // the listener should be notified
        verify(mMonitor1, Mockito.times(1)).onLocationChanged(location);

        // register another observer
        mSystemLocation.monitorWith(mMonitor2);
        // unregister the first one
        mSystemLocation.disposeMonitor(mMonitor1);

        // mock another update
        locationListenerCaptor.getValue().onLocationChanged(location);

        // the first listener should not have been notified
        verify(mMonitor1, Mockito.times(1)).onLocationChanged(location);
        // and the second one should have been notified
        verify(mMonitor2, Mockito.times(1)).onLocationChanged(location);

    }
}
