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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common.flightlog;

import com.parrot.drone.groundsdk.DateParser;
import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpFdrClient;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpFdrInfo;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.FlightLogDownloader;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.groundsdk.internal.utility.FlightLogStorage;
import com.parrot.drone.groundsdk.internal.utility.GutmaLogStorage;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState;
import com.parrot.drone.sdkcore.arsdk.Backend;
import com.parrot.drone.sdkcore.flightlogconverter.FlightLogConverter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.parrot.drone.groundsdk.FlightLogDownloaderMatcher.hasDownloadedSuccessfully;
import static com.parrot.drone.groundsdk.FlightLogDownloaderMatcher.isDownloading;
import static com.parrot.drone.groundsdk.FlightLogDownloaderMatcher.isIdle;
import static com.parrot.drone.groundsdk.FlightLogDownloaderMatcher.wasInterruptedAfter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HttpFlightLogDownloaderTests extends ArsdkEngineTestBase {

    private static final Date DATE_RECORD_1 = DateParser.parse("19700103T182145+0100");

    private static final Date DATE_RECORD_3 = DateParser.parse("19700105T182145+0100");

    private static final List<HttpFdrInfo> RECORD_LIST_1 = Collections.singletonList(
            new HttpFdrInfo(
                    "log-1.bin",
                    DATE_RECORD_1,
                    599989501,
                    "/data/fdr/log-1.bin"));

    private static final List<HttpFdrInfo> RECORD_LIST_2 = Arrays.asList(
            new HttpFdrInfo(
                    "log-1.bin",
                    DATE_RECORD_1,
                    599989501,
                    "/data/fdr/log-1.bin"),

            new HttpFdrInfo(
                    "log-2.bin",
                    null, // invalid date to test deletion
                    22075792,
                    "/data/fdr/log-2.bin"),

            new HttpFdrInfo(
                    "log-3.bin",
                    DATE_RECORD_3,
                    123456789,
                    "/data/fdr/log-3.bin"));

    private static final HttpRequest DUMMY_REQUEST = () -> {};


    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private static File mRecordStorage;

    private static File mGutmaLogStorage;

    private DroneCore mDrone;

    private FlightLogDownloader mFlightLogDownloader;

    private HttpFdrClient mClient;

    private int mChangeCnt;

    @Captor
    private ArgumentCaptor<HttpRequest.ResultCallback<List<HttpFdrInfo>>> mResultCallbackCaptor;

    @Captor
    private ArgumentCaptor<HttpRequest.StatusCallback> mStatusCallbackCaptor;

    @Mock
    private GutmaLogStorage mMockGutmaLogStorage;

    @Mock
    private FlightLogStorage mMockRecordStorage;

    @Mock
    private FlightLogConverter mFlightLogConverter;

    @Override
    public void setUp() {
        super.setUp();

        mRecordStorage = new File(mTemporaryFolder.getRoot(), "record");
        mGutmaLogStorage = new File(mTemporaryFolder.getRoot(), "gutma");

        doReturn(mRecordStorage).when(mMockRecordStorage).getWorkDir();
        mUtilities.registerUtility(FlightLogStorage.class, mMockRecordStorage);

        doReturn(mGutmaLogStorage).when(mMockGutmaLogStorage).getWorkDir();
        mUtilities.registerUtility(GutmaLogStorage.class, mMockGutmaLogStorage);

        mClient = mock(HttpFdrClient.class);
        doReturn(DUMMY_REQUEST).when(mClient).listRecords(any());
        doReturn(DUMMY_REQUEST).when(mClient).downloadRecord(any(), any(), any());
        doReturn(DUMMY_REQUEST).when(mClient).deleteRecord(any(), any());
        MockHttpSession.registerOnly(mClient);

        doReturn(true).when(mFlightLogConverter).convertToGutma(any(), any());
        FlightLogConverter.setInstance(mFlightLogConverter);

        mArsdkEngine.start();

        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mFlightLogDownloader = mDrone.getPeripheralStore().get(mMockSession, FlightLogDownloader.class);
        mDrone.getPeripheralStore().registerObserver(FlightLogDownloader.class, () -> {
            mFlightLogDownloader = mDrone.getPeripheralStore().get(mMockSession, FlightLogDownloader.class);
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
        assertThat(mFlightLogDownloader, nullValue());

        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, notNullValue());

        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, nullValue());
    }

    @Test
    public void testListingError() {
        connectDrone(mDrone, 1);

        // should be idle until the record list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, isIdle());

        verify(mClient).listRecords(mResultCallbackCaptor.capture());

        // mock listing error
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500, null);

        // nothing should change
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, isIdle());
    }

    @Test
    public void testDownloadError() {
        connectDrone(mDrone, 1);

        // should be idle until the record list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, isIdle());

        verify(mClient).listRecords(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, RECORD_LIST_1);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        verify(mClient).downloadRecord(eq("/data/fdr/log-1.bin"), eq(new File(mRecordStorage, "123_log-1.bin")),
                mStatusCallbackCaptor.capture());

        // mock download error
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        // even after download failure, deletion should be requested
        verify(mClient).deleteRecord(eq("log-1.bin"), mStatusCallbackCaptor.capture());

        // nothing should change while delete request does not complete
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        // mock delete completion with success
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // since there was only one record to download, the peripheral should record that it has successfully downloaded
        // 0 record
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightLogDownloader, hasDownloadedSuccessfully(0));
    }

    @Test
    public void testDeleteError() {
        connectDrone(mDrone, 1);

        // should be idle until the record list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, isIdle());

        verify(mClient).listRecords(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, RECORD_LIST_1);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        verify(mClient).downloadRecord(eq("/data/fdr/log-1.bin"),
                eq(new File(mRecordStorage, "123_log-1.bin")),
                mStatusCallbackCaptor.capture());

        // mock download completion
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // deletion request should have been sent
        verify(mClient).deleteRecord(eq("log-1.bin"), mStatusCallbackCaptor.capture());

        // nothing should change while delete request does not complete
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        // mock delete completion with error flag
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        // since there was only one record to download, the peripheral should record that it has successfully downloaded
        // 1 record
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightLogDownloader, hasDownloadedSuccessfully(1));
    }

    @Test
    public void testListingCancel() {
        connectDrone(mDrone, 1);

        // should be idle until the record list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, isIdle());

        verify(mClient).listRecords(mResultCallbackCaptor.capture());

        // mock listing cancel
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN,
                null);

        // nothing should change
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, isIdle());
    }

    @Test
    public void testDownloadCancel() {
        connectDrone(mDrone, 1);

        // should be idle until the record list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, isIdle());

        verify(mClient).listRecords(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, RECORD_LIST_1);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        verify(mClient).downloadRecord(eq("/data/fdr/log-1.bin"),
                eq(new File(mRecordStorage, "123_log-1.bin")),
                mStatusCallbackCaptor.capture());

        // mock download cancel
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.CANCELED,
                HttpRequest.STATUS_CODE_UNKNOWN);

        // after a download cancel, the overall task should be interrupted
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightLogDownloader, wasInterruptedAfter(0));
    }

    @Test
    public void testDeleteCancel() {
        connectDrone(mDrone, 1);

        // should be idle until the record list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, isIdle());

        verify(mClient).listRecords(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, RECORD_LIST_1);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        verify(mClient).downloadRecord(eq("/data/fdr/log-1.bin"),
                eq(new File(mRecordStorage, "123_log-1.bin")),
                mStatusCallbackCaptor.capture());

        // mock download completion
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // delete should be started
        verify(mClient).deleteRecord(eq("log-1.bin"), mStatusCallbackCaptor.capture());

        // mock delete cancel
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.CANCELED,
                HttpRequest.STATUS_CODE_UNKNOWN);

        // the overall task should be successful
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightLogDownloader, hasDownloadedSuccessfully(1));
    }

    @Test
    public void testDownloadMultipleFiles() {
        connectDrone(mDrone, 1);

        // should be idle until the record list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, isIdle());

        verify(mClient).listRecords(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, RECORD_LIST_2);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        File downloadedRecord = new File(mRecordStorage, "123_log-1.bin");
        File gutmaLog = new File(mGutmaLogStorage, "123_log-1.gutma");

        verify(mClient).downloadRecord(eq("/data/fdr/log-1.bin"),
                eq(downloadedRecord),
                mStatusCallbackCaptor.capture());

        // mock 1st download completion
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        verify(mClient).deleteRecord(eq("log-1.bin"), mStatusCallbackCaptor.capture());

        // nothing should change while delete request does not complete
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        // mock delete completion with success
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        verify(mMockGutmaLogStorage).notifyGutmaLogFileReady(gutmaLog);
        verify(mMockRecordStorage).notifyFlightLogReady(downloadedRecord);

        assertThat(mChangeCnt, is(3));
        assertThat(mFlightLogDownloader, isDownloading(1));

        // 2nd record should not be downloaded as it is invalid
        verify(mClient, never()).downloadRecord(eq("log-2.bin"), any(), any());

        downloadedRecord = new File(mRecordStorage, "123_log-3.bin");
        gutmaLog = new File(mGutmaLogStorage, "123_log-3.gutma");

        verify(mClient).downloadRecord(eq("/data/fdr/log-3.bin"),
                eq(downloadedRecord),
                mStatusCallbackCaptor.capture());

        // mock 3nd download completion
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        verify(mClient).deleteRecord(eq("log-3.bin"), mStatusCallbackCaptor.capture());

        // nothing should change while delete request does not complete
        assertThat(mChangeCnt, is(3));
        assertThat(mFlightLogDownloader, isDownloading(1));

        // mock delete completion with success
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        verify(mMockGutmaLogStorage).notifyGutmaLogFileReady(gutmaLog);
        verify(mMockRecordStorage).notifyFlightLogReady(downloadedRecord);

        assertThat(mChangeCnt, is(4));
        assertThat(mFlightLogDownloader, hasDownloadedSuccessfully(2));
    }

    @Test
    public void testDataSyncUnavailableDuringDownload() {
        connectDrone(mDrone, 1);

        // should be idle until the record list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, isIdle());

        verify(mClient).listRecords(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, RECORD_LIST_2);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        verify(mClient).downloadRecord(eq("/data/fdr/log-1.bin"),
                eq(new File(mRecordStorage, "123_log-1.bin")),
                mStatusCallbackCaptor.capture());

        // mock 1st download completion
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        verify(mClient).deleteRecord(eq("log-1.bin"), mStatusCallbackCaptor.capture());

        // mock delete completion with success
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(3));
        assertThat(mFlightLogDownloader, isDownloading(1));

        // 2nd record should not be downloaded as it is invalid
        verify(mClient, never()).downloadRecord(eq("log-2.bin"), any(), any());

        verify(mClient).downloadRecord(eq("/data/fdr/log-3.bin"),
                eq(new File(mRecordStorage, "123_log-3.bin")),
                mStatusCallbackCaptor.capture());

        // mock data sync unavailable - make drone flying
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                PilotingstateFlyingstatechangedState.FLYING));

        // all tasks should be canceled
        verify(mClient).dispose();

        // mock download cancel
        mStatusCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.CANCELED,
                HttpRequest.STATUS_CODE_UNKNOWN);

        assertThat(mChangeCnt, is(4));
        assertThat(mFlightLogDownloader, wasInterruptedAfter(1));
    }

    @Test
    public void testDataSyncAllowanceChange() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                        PilotingstateFlyingstatechangedState.FLYING)));

        // initial state, data sync is not allowed
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, isIdle());

        verify(mClient, never()).listRecords(any());

        // mock piloting state change to allow data sync
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                PilotingstateFlyingstatechangedState.EMERGENCY));

        // now data sync is allowed, but downloader should be idle until the record list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mFlightLogDownloader, isIdle());

        verify(mClient).listRecords(mResultCallbackCaptor.capture());

        // mock listing successful result
        mResultCallbackCaptor.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, RECORD_LIST_1);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mFlightLogDownloader, isDownloading(0));

        // mock piloting state change to disallow data sync
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                PilotingstateFlyingstatechangedState.TAKINGOFF));

        // download is interrupted
        verify(mClient).dispose();
    }
}
