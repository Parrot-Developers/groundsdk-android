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

package com.parrot.drone.groundsdk.internal.engine.firmware;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.parrot.drone.groundsdk.MockAppStorageProvider;
import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.facility.FirmwareManager;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.device.DeviceStoreCore;
import com.parrot.drone.groundsdk.internal.engine.MockEngineController;
import com.parrot.drone.groundsdk.internal.http.HttpFirmwaresInfo;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.HttpUpdateClient;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.internal.utility.DroneStore;
import com.parrot.drone.groundsdk.internal.utility.RemoteControlStore;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public class FirmwareEngineTest {

    private static final HttpFirmwaresInfo EMPTY_HTTP_FIRMWARE_INFO
            = new HttpFirmwaresInfo(new HashSet<>(), new HashSet<>());

    private FirmwareManager mFirmwareManager;

    private FirmwareEngine mEngine;

    private SystemConnectivity mMockConnectivity;

    private HttpUpdateClient mMockHttpClient;

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<HttpRequest.ResultCallback<HttpFirmwaresInfo>> mCbCaptor =
            ArgumentCaptor.forClass(HttpRequest.ResultCallback.class);

    @BeforeClass
    public static void load() {
        TestExecutor.setup();
        ApplicationStorageProvider.setInstance(new MockAppStorageProvider());
    }

    @AfterClass
    public static void unload() {
        ApplicationStorageProvider.setInstance(null);
        TestExecutor.teardown();
    }

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteSharedPreferences(Persistence.PREF_FILE);

        MockComponentStore<Facility> facilityStore = new MockComponentStore<>();
        facilityStore.registerObserver(FirmwareManager.class, () ->
                mFirmwareManager = facilityStore.get(FirmwareManager.class));

        mMockConnectivity = mock(SystemConnectivity.class);
        DroneStore droneStore = new DeviceStoreCore.Drone();
        RemoteControlStore rcStore = new DeviceStoreCore.RemoteControl();

        UtilityRegistry utilities = new UtilityRegistry();
        mEngine = mock(FirmwareEngine.class, withSettings()
                .useConstructor(MockEngineController.create(context,
                        utilities.registerUtility(SystemConnectivity.class, mMockConnectivity)
                                 .registerUtility(DroneStore.class, droneStore)
                                 .registerUtility(RemoteControlStore.class, rcStore), facilityStore))
                .defaultAnswer(CALLS_REAL_METHODS));

        mMockHttpClient = mock(HttpUpdateClient.class);
        doReturn((HttpRequest) () -> {}).when(mMockHttpClient).listAvailableFirmwares(any(), any());
        doReturn(mMockHttpClient).when(mEngine).createHttpClient();

        mEngine.persistence().saveLastRemoteUpdateTime(0);
    }

    @After
    public void teardown() {
        MockHttpSession.resetDefaultClients();
    }

    @Test
    public void testStart() {
        mEngine.start();
        mEngine.onAllEnginesStarted();

        assertThat(mFirmwareManager, notNullValue());
        Mockito.verify(mMockConnectivity, Mockito.times(1)).monitorWith(any());
    }

    @Test
    public void testStop() {
        ArgumentCaptor<SystemConnectivity.Monitor> monitorListenerCaptor =
                ArgumentCaptor.forClass(SystemConnectivity.Monitor.class);

        mEngine.start();

        Mockito.verify(mMockConnectivity, Mockito.times(1)).monitorWith(monitorListenerCaptor.capture());

        mEngine.requestStop(null);
        mEngine.stop();

        Mockito.verify(mMockConnectivity, Mockito.times(1)).disposeMonitor(monitorListenerCaptor.getValue());
    }

    @Test
    public void testAutoQueryUpdateInformation() {
        mEngine.start();

        // query update information on remote server should not be triggered yet
        Mockito.verify(mMockHttpClient, Mockito.times(0)).listAvailableFirmwares(any(), any());

        // mock Internet available
        mockInternetAvailable();

        // query update information on remote server should be triggered
        Mockito.verify(mMockHttpClient, Mockito.times(1)).listAvailableFirmwares(any(), mCbCaptor.capture());

        // mock a request failure
        mCbCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500, null);

        // mock Internet availability changes
        mockInternetUnavailable();
        mockInternetAvailable();

        // query update information on remote server should be triggered again, as previous request failed
        Mockito.verify(mMockHttpClient, Mockito.times(2)).listAvailableFirmwares(any(), mCbCaptor.capture());

        // mock a request success
        mCbCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, EMPTY_HTTP_FIRMWARE_INFO);

        // mock Internet availability changes
        mockInternetUnavailable();
        mockInternetAvailable();

        // query update information on remote server should not be triggered
        Mockito.verify(mMockHttpClient, Mockito.times(2)).listAvailableFirmwares(any(), any());

        // mock that latest request is 7 day-old
        mEngine.persistence().saveLastRemoteUpdateTime(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7) - 1);

        // mock Internet availability changes
        mockInternetUnavailable();
        mockInternetAvailable();

        // query update information should be triggered, as automatic query period is reached
        Mockito.verify(mMockHttpClient, Mockito.times(3)).listAvailableFirmwares(any(), any());
    }

    @Test
    public void testQueryUpdateInformationTimeLimit() {
        mEngine.start();
        mEngine.onAllEnginesStarted();

        mockInternetAvailable();

        assertThat(mFirmwareManager, notNullValue());

        // query remote update for first time should trigger a request on server
        mFirmwareManager.queryRemoteFirmwares();
        Mockito.verify(mMockHttpClient, Mockito.times(1)).listAvailableFirmwares(any(), mCbCaptor.capture());

        // mock a request failure
        mCbCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500, null);

        // query remote update should trigger a request on server, as first request failed
        mFirmwareManager.queryRemoteFirmwares();
        Mockito.verify(mMockHttpClient, Mockito.times(2)).listAvailableFirmwares(any(), mCbCaptor.capture());

        // mock a request success
        mCbCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, EMPTY_HTTP_FIRMWARE_INFO);

        // query remote update within time limitation should not trigger a request on server
        mFirmwareManager.queryRemoteFirmwares();
        Mockito.verify(mMockHttpClient, Mockito.times(2)).listAvailableFirmwares(any(), any());

        // query remote update beyond time limitation should trigger a request on server
        mEngine.persistence().saveLastRemoteUpdateTime(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1) - 1);
        mFirmwareManager.queryRemoteFirmwares();
        Mockito.verify(mMockHttpClient, Mockito.times(3)).listAvailableFirmwares(any(), mCbCaptor.capture());
    }

    private void mockInternetAvailable() {
        mockInternetAvailabilityChange(true);
    }

    private void mockInternetUnavailable() {
        mockInternetAvailabilityChange(false);
    }

    private void mockInternetAvailabilityChange(boolean available) {
        Mockito.when(mMockConnectivity.isInternetAvailable()).thenReturn(available);
        ArgumentCaptor<SystemConnectivity.Monitor> monitorListenerCaptor =
                ArgumentCaptor.forClass(SystemConnectivity.Monitor.class);
        Mockito.verify(mMockConnectivity, Mockito.times(1)).monitorWith(monitorListenerCaptor.capture());
        monitorListenerCaptor.getValue().onInternetAvailabilityChanged(available);
    }
}
