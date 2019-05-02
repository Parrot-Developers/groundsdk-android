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
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;

import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class SystemConnectivityMonitorTests {

    private ConnectivityManager mConnectivityManager;

    private SystemConnectivity.Monitor mMonitor1, mMonitor2;

    private SystemConnectivity mSystemConnectivity;

    @Before
    public void setUp() {
        assertThat(Build.VERSION.SDK_INT, greaterThanOrEqualTo(Build.VERSION_CODES.M));

        TestExecutor.setup();

        mConnectivityManager = mock(ConnectivityManager.class);
        mMonitor1 = mock(SystemConnectivity.Monitor.class);
        mMonitor2 = mock(SystemConnectivity.Monitor.class);

        Context context = mock(Context.class);
        doReturn(mConnectivityManager).when(context).getSystemService(Context.CONNECTIVITY_SERVICE);
        SystemEngine systemEngine = new MockSystemEngine(context);

        mSystemConnectivity = new SystemConnectivityCore(systemEngine);
        systemEngine.start();
    }

    @After
    public void teardown() {
        TestExecutor.teardown();
    }

    @Test
    public void testStartStopMonitoring() {
        // ensure that the first observer registration triggers a monitoring start
        mSystemConnectivity.monitorWith(mMonitor1);

        ArgumentCaptor<ConnectivityManager.NetworkCallback> callbackCaptor = ArgumentCaptor.forClass(
                ConnectivityManager.NetworkCallback.class);
        Mockito.verify(mConnectivityManager, Mockito.times(1)).registerNetworkCallback(
                Mockito.notNull(), callbackCaptor.capture());
        ConnectivityManager.NetworkCallback networkCallback = callbackCaptor.getValue();
        assertThat(networkCallback, notNullValue());


        // ensure that the second observer registration does not trigger a start
        mSystemConnectivity.monitorWith(mMonitor2);
        Mockito.verify(mConnectivityManager, Mockito.times(1)).registerNetworkCallback(
                Mockito.any(), Mockito.<ConnectivityManager.NetworkCallback>any());

        // ensure that unregistering an observer, if not last, does not trigger neither a start nor a stop
        mSystemConnectivity.disposeMonitor(mMonitor1);
        Mockito.verify(mConnectivityManager, Mockito.never()).unregisterNetworkCallback(
                Mockito.<ConnectivityManager.NetworkCallback>any());

        // ensure that unregistering the last observer does trigger a monitoring stop
        mSystemConnectivity.disposeMonitor(mMonitor2);
        Mockito.verify(mConnectivityManager, Mockito.times(1)).unregisterNetworkCallback(networkCallback);
    }

    @Test
    public void testSameObserver() {
        // mock available internet connectivity
        mockInternetAvailable();

        // first observer should receive notification
        mSystemConnectivity.monitorWith(mMonitor1);
        // registering the same observer should not trigger a notification
        mSystemConnectivity.monitorWith(mMonitor1);

        Mockito.verify(mMonitor1, Mockito.times(1)).onInternetAvailabilityChanged(true);
        Mockito.verify(mMonitor1, Mockito.never()).onInternetAvailabilityChanged(false);
    }

    @Test
    public void testNoActiveNetworkOnRegister() {
        mockNoActiveNetwork();

        // first observer should not receive any notification
        mSystemConnectivity.monitorWith(mMonitor1);
        // neither should second observer
        mSystemConnectivity.monitorWith(mMonitor2);

        Mockito.verify(mMonitor1, Mockito.never()).onInternetAvailabilityChanged(Mockito.anyBoolean());
        Mockito.verify(mMonitor2, Mockito.never()).onInternetAvailabilityChanged(Mockito.anyBoolean());
    }

    @Test
    public void testInternetAvailableOnRegister() {
        mockInternetAvailable();

        // first observer should receive notification
        mSystemConnectivity.monitorWith(mMonitor1);
        // and so should second observer
        mSystemConnectivity.monitorWith(mMonitor2);

        Mockito.verify(mMonitor1, Mockito.times(1)).onInternetAvailabilityChanged(true);
        Mockito.verify(mMonitor1, Mockito.never()).onInternetAvailabilityChanged(false);
        Mockito.verify(mMonitor2, Mockito.times(1)).onInternetAvailabilityChanged(true);
        Mockito.verify(mMonitor2, Mockito.never()).onInternetAvailabilityChanged(false);
    }

    @Test
    public void testInternetBecomesAvailable() {
        mSystemConnectivity.monitorWith(mMonitor1);
        mSystemConnectivity.monitorWith(mMonitor2);

        // since the observers where registered before internet becomes available, no notification should fire yet
        Mockito.verify(mMonitor1, Mockito.never()).onInternetAvailabilityChanged(Mockito.anyBoolean());
        Mockito.verify(mMonitor2, Mockito.never()).onInternetAvailabilityChanged(Mockito.anyBoolean());

        mockActiveNetworkConnects();

        // all observers should have received notification now
        Mockito.verify(mMonitor1, Mockito.times(1)).onInternetAvailabilityChanged(true);
        Mockito.verify(mMonitor1, Mockito.never()).onInternetAvailabilityChanged(false);
        Mockito.verify(mMonitor2, Mockito.times(1)).onInternetAvailabilityChanged(true);
        Mockito.verify(mMonitor2, Mockito.never()).onInternetAvailabilityChanged(false);
    }

    @Test
    public void testActiveNetworkDisconnects() {
        mockInternetAvailable();

        mSystemConnectivity.monitorWith(mMonitor1);
        mSystemConnectivity.monitorWith(mMonitor2);

        Mockito.reset(mMonitor1, mMonitor2);

        mockActiveNetworkDisconnects();

        // all observers should receive notification
        Mockito.verify(mMonitor1, Mockito.times(1)).onInternetAvailabilityChanged(false);
        Mockito.verify(mMonitor1, Mockito.never()).onInternetAvailabilityChanged(true);
        Mockito.verify(mMonitor2, Mockito.times(1)).onInternetAvailabilityChanged(false);
        Mockito.verify(mMonitor2, Mockito.never()).onInternetAvailabilityChanged(true);
    }

    private void mockNoActiveNetwork() {
        Mockito.doNothing().when(mConnectivityManager).registerNetworkCallback(Mockito.any(),
                Mockito.<ConnectivityManager.NetworkCallback>any());
    }

    private void mockActiveNetworkConnects() {
        ArgumentCaptor<ConnectivityManager.NetworkCallback> callbackCaptor = ArgumentCaptor.forClass(
                ConnectivityManager.NetworkCallback.class);
        Mockito.verify(mConnectivityManager, Mockito.times(1)).registerNetworkCallback(
                Mockito.notNull(), callbackCaptor.capture());
        ConnectivityManager.NetworkCallback networkCallback = callbackCaptor.getValue();
        assertThat(networkCallback, notNullValue());
        networkCallback.onAvailable(mock(Network.class));
    }

    private void mockActiveNetworkDisconnects() {
        ArgumentCaptor<ConnectivityManager.NetworkCallback> callbackCaptor = ArgumentCaptor.forClass(
                ConnectivityManager.NetworkCallback.class);
        Mockito.verify(mConnectivityManager, Mockito.times(1)).registerNetworkCallback(
                Mockito.notNull(), callbackCaptor.capture());
        ConnectivityManager.NetworkCallback networkCallback = callbackCaptor.getValue();
        assertThat(networkCallback, notNullValue());
        Mockito.when(mConnectivityManager.getActiveNetworkInfo()).thenReturn(null);
        networkCallback.onLost(mock(Network.class));
    }

    private void mockInternetAvailable() {
        Mockito.doAnswer((Answer<Void>) invocationOnMock -> {
            ConnectivityManager.NetworkCallback callback = invocationOnMock.getArgument(1);
            callback.onAvailable(mock(Network.class));
            return null;
        }).when(mConnectivityManager).registerNetworkCallback(Mockito.any(),
                Mockito.<ConnectivityManager.NetworkCallback>any());
    }

}
