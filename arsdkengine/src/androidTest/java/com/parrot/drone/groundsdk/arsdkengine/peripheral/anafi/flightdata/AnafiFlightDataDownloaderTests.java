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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.anafi.flightdata;

import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpPudClient;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpPudInfo;
import com.parrot.drone.groundsdk.arsdkengine.http.MockHttpPud;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.FlightDataDownloader;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.groundsdk.internal.utility.FlightDataStorage;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.parrot.drone.groundsdk.FlightDataDownloaderMatcher.hasDownloadedSuccessfully;
import static com.parrot.drone.groundsdk.FlightDataDownloaderMatcher.isDownloading;
import static com.parrot.drone.groundsdk.FlightDataDownloaderMatcher.isIdle;
import static com.parrot.drone.groundsdk.FlightDataDownloaderMatcher.wasInterruptedAfter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AnafiFlightDataDownloaderTests extends ArsdkEngineTestBase {

    private static final List<HttpPudInfo> REPORT_LIST_1 = Collections.singletonList(
            MockHttpPud.info(
                    "pud_1",
                    "19700103T182145+0100",
                    100,
                    "/data/pud/pud_1"));

    private static final List<HttpPudInfo> REPORT_LIST_2 = Arrays.asList(
            MockHttpPud.info(
                    "pud_1",
                    "19700103T182145+0100",
                    100,
                    "/data/pud/pud_1"),
            MockHttpPud.info(
                    "pud_2",
                    "19700104T182145+0100",
                    2000,
                    "/data/pud/pud_2"));

    private static final HttpRequest DUMMY_REQUEST = () -> {};

    private static final File FLIGHTDATA_STORAGE = new File("/tmp");

    private DroneCore mDrone;

    private FlightDataDownloader mFlightDataDownloader;

    private HttpPudClient mClient;

    private int mChangeCnt;

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<HttpRequest.ResultCallback<List<HttpPudInfo>>> mResultCallbackCaptor =
            ArgumentCaptor.forClass(HttpRequest.ResultCallback.class);

    private final ArgumentCaptor<HttpRequest.StatusCallback> mStatusCallbackCaptor =
            ArgumentCaptor.forClass(HttpRequest.StatusCallback.class);

    @Override
    public void setUp() {
        super.setUp();

        FlightDataStorage mockStorage = mock(FlightDataStorage.class);
        doReturn(FLIGHTDATA_STORAGE).when(mockStorage).getWorkDir();
        mUtilities.registerUtility(FlightDataStorage.class, mockStorage);

        mClient = mock(HttpPudClient.class);
        doReturn(DUMMY_REQUEST).when(mClient).listPuds(any());
        doReturn(DUMMY_REQUEST).when(mClient).downloadPud(any(), any(), any(), any());
        doReturn(DUMMY_REQUEST).when(mClient).deletePud(any(), any());
        MockHttpSession.registerOnly(mClient);

        mArsdkEngine.start();

        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mFlightDataDownloader = mDrone.getPeripheralStore().get(mMockSession, FlightDataDownloader.class);
        mDrone.getPeripheralStore().registerObserver(FlightDataDownloader.class, () -> {
            mFlightDataDownloader = mDrone.getPeripheralStore().get(mMockSession, FlightDataDownloader.class);
            mChangeCnt++;
        });

        mChangeCnt = 0;
    }

    @Override
    public void teardown() {
        MockHttpSession.resetDefaultClients();
        super.teardown();
    }

    @Test
    public void testPublication() {
        assertThat(mChangeCnt, is(0));
        assertThat(mFlightDataDownloader, nullValue());

        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mFlightDataDownloader, notNullValue());

        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(2));
        assertThat(mFlightDataDownloader, nullValue());
    }

    @Test
    public void testListingError() {
        connectDrone(mDrone, 1);

        // should be idle until the pud list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightDataDownloader, isIdle());

        verify(mClient).listPuds(mResultCallbackCaptor.capture());

        // mock listing error
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500, null);

        // nothing should change
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightDataDownloader, isIdle());
    }

    @Test
    public void testDownloadError() {
        connectDrone(mDrone, 1);

        // should be idle until the pud list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightDataDownloader, isIdle());

        verify(mClient).listPuds(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_1);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightDataDownloader, isDownloading(0));

        verify(mClient).downloadPud(eq("/data/pud/pud_1"), eq(new File(FLIGHTDATA_STORAGE, "pud_1")), any(),
                mStatusCallbackCaptor.capture());

        // mock download error
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        // evan after download failure, deletion should be requested,
        verify(mClient).deletePud(eq("pud_1"), any());

        // However the overall task should continue; since there was only one pud to download,
        // the peripheral should report that it has successfully downloaded 0 flight data.
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightDataDownloader, hasDownloadedSuccessfully(0));
    }

    @Test
    public void testDeleteError() {
        connectDrone(mDrone, 1);

        // should be idle until the pud list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightDataDownloader, isIdle());

        verify(mClient).listPuds(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_1);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightDataDownloader, isDownloading(0));

        verify(mClient).downloadPud(eq("/data/pud/pud_1"), eq(new File(FLIGHTDATA_STORAGE, "pud_1")), any(),
                mStatusCallbackCaptor.capture());

        // mock download completion
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // deletion request should have been sent
        verify(mClient).deletePud(eq("pud_1"), mStatusCallbackCaptor.capture());

        // The overall task should continue immediately; since there was only one pud to download,
        // the peripheral should report that it has successfully downloaded 0 flight data.
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightDataDownloader, hasDownloadedSuccessfully(1));

        // mock delete completion with error flag
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        // nothing should change
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightDataDownloader, hasDownloadedSuccessfully(1));
    }

    @Test
    public void testListingCancel() {
        connectDrone(mDrone, 1);

        // should be idle until the pud list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightDataDownloader, isIdle());

        verify(mClient).listPuds(mResultCallbackCaptor.capture());

        // mock listing cancel
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN,
                null);

        // nothing should change
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightDataDownloader, isIdle());
    }

    @Test
    public void testDownloadCancel() {
        connectDrone(mDrone, 1);

        // should be idle until the pud list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightDataDownloader, isIdle());

        verify(mClient).listPuds(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_1);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightDataDownloader, isDownloading(0));

        verify(mClient).downloadPud(eq("/data/pud/pud_1"), eq(new File(FLIGHTDATA_STORAGE, "pud_1")), any(),
                mStatusCallbackCaptor.capture());

        // mock download cancel
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.CANCELED,
                HttpRequest.STATUS_CODE_UNKNOWN);

        // after a download cancel, the overall task should be interrupted
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightDataDownloader, wasInterruptedAfter(0));
    }

    @Test
    public void testDeleteCancel() {
        connectDrone(mDrone, 1);

        // should be idle until the pud list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightDataDownloader, isIdle());

        verify(mClient).listPuds(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_1);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightDataDownloader, isDownloading(0));

        verify(mClient).downloadPud(eq("/data/pud/pud_1"), eq(new File(FLIGHTDATA_STORAGE, "pud_1")), any(),
                mStatusCallbackCaptor.capture());

        // mock download completion
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // overall task should be successful
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightDataDownloader, hasDownloadedSuccessfully(1));

        // delete should be started
        verify(mClient).deletePud(eq("pud_1"), mStatusCallbackCaptor.capture());

        // mock delete cancel
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.CANCELED,
                HttpRequest.STATUS_CODE_UNKNOWN);

        // nothing should change
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightDataDownloader, hasDownloadedSuccessfully(1));
    }

    @Test
    public void testDownloadMultipleFiles() {
        connectDrone(mDrone, 1);

        // should be idle until the pud list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightDataDownloader, isIdle());

        verify(mClient).listPuds(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_2);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightDataDownloader, isDownloading(0));

        verify(mClient).downloadPud(eq("/data/pud/pud_1"), eq(new File(FLIGHTDATA_STORAGE, "pud_1")), any(),
                mStatusCallbackCaptor.capture());

        // mock 1st download completion
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(3));
        assertThat(mFlightDataDownloader, isDownloading(1));

        verify(mClient).deletePud(eq("pud_1"), any());

        verify(mClient).downloadPud(eq("/data/pud/pud_2"), eq(new File(FLIGHTDATA_STORAGE, "pud_2")), any(),
                mStatusCallbackCaptor.capture());

        // mock 2nd download completion
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(4));
        assertThat(mFlightDataDownloader, hasDownloadedSuccessfully(2));

        verify(mClient).deletePud(eq("pud_2"), mStatusCallbackCaptor.capture());
    }

    @Test
    public void testDataSyncUnavailableDuringDownload() {
        connectDrone(mDrone, 1);

        // should be idle until the pud list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightDataDownloader, isIdle());

        verify(mClient).listPuds(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_2);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightDataDownloader, isDownloading(0));

        verify(mClient).downloadPud(eq("/data/pud/pud_1"), eq(new File(FLIGHTDATA_STORAGE, "pud_1")), any(),
                mStatusCallbackCaptor.capture());

        // mock 1st download completion
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(3));
        assertThat(mFlightDataDownloader, isDownloading(1));

        verify(mClient).deletePud(eq("pud_1"), any());

        verify(mClient).downloadPud(eq("/data/pud/pud_2"), eq(new File(FLIGHTDATA_STORAGE, "pud_2")), any(),
                mStatusCallbackCaptor.capture());

        // mock data sync unavailable - make drone flying
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING));

        // all tasks should be canceled
        verify(mClient).dispose();

        // mock download cancel
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.CANCELED,
                HttpRequest.STATUS_CODE_UNKNOWN);

        assertThat(mChangeCnt, is(4));
        assertThat(mFlightDataDownloader, wasInterruptedAfter(1));
    }
}
