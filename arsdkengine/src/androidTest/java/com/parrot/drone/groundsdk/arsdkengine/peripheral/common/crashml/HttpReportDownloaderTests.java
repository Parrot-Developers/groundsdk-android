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

package com.parrot.drone.groundsdk.arsdkengine.peripheral.common.crashml;

import com.parrot.drone.groundsdk.DateParser;
import com.parrot.drone.groundsdk.arsdkengine.ArsdkEngineTestBase;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpReportClient;
import com.parrot.drone.groundsdk.arsdkengine.http.HttpReportInfo;
import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.device.peripheral.CrashReportDownloader;
import com.parrot.drone.groundsdk.internal.device.DroneCore;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.groundsdk.internal.utility.CrashReportStorage;
import com.parrot.drone.sdkcore.arsdk.ArsdkEncoder;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3;
import com.parrot.drone.sdkcore.arsdk.ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState;
import com.parrot.drone.sdkcore.arsdk.Backend;

import org.junit.Test;
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

import static com.parrot.drone.groundsdk.CrashReportDownloaderMatcher.hasDownloadedSuccessfully;
import static com.parrot.drone.groundsdk.CrashReportDownloaderMatcher.isDownloading;
import static com.parrot.drone.groundsdk.CrashReportDownloaderMatcher.isIdle;
import static com.parrot.drone.groundsdk.CrashReportDownloaderMatcher.wasInterruptedAfter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HttpReportDownloaderTests extends ArsdkEngineTestBase {

    private static final Date DATE_REPORT_1 = DateParser.parse("19700103T182145+0100");

    private static final Date DATE_REPORT_2 = DateParser.parse("19700104T182145+0100");

    private static final List<HttpReportInfo> REPORT_LIST_1 = Collections.singletonList(
            new HttpReportInfo(
                    "report_001.tar.gz",
                    DATE_REPORT_1,
                    "/data/report/report_001.tar.gz"));

    private static final List<HttpReportInfo> REPORT_LIST_2 = Arrays.asList(
            new HttpReportInfo(
                    "report_001.tar.gz",
                    DATE_REPORT_1,
                    "/data/report/report_001.tar.gz"),

            new HttpReportInfo(
                    "report_002.tar.gz",
                    DATE_REPORT_2,
                    "/data/report/report_002.tar.gz"));

    private static final HttpRequest DUMMY_REQUEST = () -> {};

    private static final File REPORT_STORAGE = new File("/tmp");

    private DroneCore mDrone;

    private CrashReportDownloader mCrashReportDownloader;

    private int mChangeCnt;

    @Mock
    private HttpReportClient mClient;

    @Mock
    private CrashReportStorage mStorage;

    @Captor
    private ArgumentCaptor<HttpRequest.ResultCallback<List<HttpReportInfo>>> mResultCb;

    @Captor
    private ArgumentCaptor<HttpRequest.StatusCallback> mStatusCb;

    @Override
    public void setUp() {
        super.setUp();

        doReturn(REPORT_STORAGE).when(mStorage).getWorkDir();
        mUtilities.registerUtility(CrashReportStorage.class, mStorage);

        doReturn(DUMMY_REQUEST).when(mClient).listReports(any());
        doReturn(DUMMY_REQUEST).when(mClient).downloadReport(any(), any(), any(), any());
        doReturn(DUMMY_REQUEST).when(mClient).deleteReport(any(), any());
        MockHttpSession.registerOnly(mClient);

        mArsdkEngine.start();

        mMockArsdkCore.addDevice("123", Drone.Model.ANAFI_4K.id(), "Drone1", 1, Backend.TYPE_NET);
        mDrone = mDroneStore.get("123");
        assert mDrone != null;

        mCrashReportDownloader = mDrone.getPeripheralStore().get(mMockSession, CrashReportDownloader.class);
        mDrone.getPeripheralStore().registerObserver(CrashReportDownloader.class, () -> {
            mCrashReportDownloader = mDrone.getPeripheralStore().get(mMockSession, CrashReportDownloader.class);
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
        assertThat(mCrashReportDownloader, nullValue());

        connectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, notNullValue());

        disconnectDrone(mDrone, 1);

        assertThat(mChangeCnt, is(2));
        assertThat(mCrashReportDownloader, nullValue());
    }

    @Test
    public void testListingError() {
        connectDrone(mDrone, 1);

        // should be idle until the report list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());

        verify(mClient).listReports(mResultCb.capture());

        // mock listing error
        mResultCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500, null);

        // nothing should change
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());
    }

    @Test
    public void testDownloadError() {
        connectDrone(mDrone, 1);

        // should be idle until the report list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());

        verify(mClient).listReports(mResultCb.capture());

        // mock listing successful result
        mResultCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_1);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mCrashReportDownloader, isDownloading(0));

        // personal report download should start
        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz")), eq(HttpReportClient.ReportType.FULL),
                mStatusCb.capture());

        // mock download error
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        // anonymous report download should start anyway
        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz" + CrashReportStorage.ANONYMOUS_REPORT_EXT)),
                eq(HttpReportClient.ReportType.LIGHT),
                mStatusCb.capture());

        // mock download error
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        // evan after download failure, deletion should be requested,
        verify(mClient).deleteReport(eq("report_001.tar.gz"), any());

        // However the overall task should continue; since there was only one report to download,
        // the peripheral should report that it has successfully downloaded 0 report.
        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, hasDownloadedSuccessfully(0));
    }

    @Test
    public void testDeleteError() {
        connectDrone(mDrone, 1);

        // should be idle until the report list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());

        verify(mClient).listReports(mResultCb.capture());

        // mock listing successful result
        mResultCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_1);

        // personal report download should start
        assertThat(mChangeCnt, is(2));
        assertThat(mCrashReportDownloader, isDownloading(0));

        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz")), eq(HttpReportClient.ReportType.FULL),
                mStatusCb.capture());

        // mock download completion
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // anonymous download should start
        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz" + CrashReportStorage.ANONYMOUS_REPORT_EXT)),
                eq(HttpReportClient.ReportType.LIGHT),
                mStatusCb.capture());

        // mock download completion
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // deletion request should have been sent
        verify(mClient).deleteReport(eq("report_001.tar.gz"), mStatusCb.capture());

        // The overall task should continue immediately; since there was only one report to download,
        // the peripheral should report that it has successfully downloaded 0 report.
        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, hasDownloadedSuccessfully(1));

        // mock delete completion with error flag
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        // nothing should change
        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, hasDownloadedSuccessfully(1));
    }

    @Test
    public void testListingCancel() {
        connectDrone(mDrone, 1);

        // should be idle until the report list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());

        verify(mClient).listReports(mResultCb.capture());

        // mock listing cancel
        mResultCb.getValue().onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN,
                null);

        // nothing should change
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());
    }

    @Test
    public void testCancelDuringPersonalReportDownload() {
        connectDrone(mDrone, 1);

        // should be idle until the report list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());

        verify(mClient).listReports(mResultCb.capture());

        // mock listing successful result
        mResultCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_1);

        // full report download should start
        assertThat(mChangeCnt, is(2));
        assertThat(mCrashReportDownloader, isDownloading(0));

        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz")), eq(HttpReportClient.ReportType.FULL),
                mStatusCb.capture());

        // mock download cancel
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.CANCELED,
                HttpRequest.STATUS_CODE_UNKNOWN);

        // after a download cancel, the overall task should be interrupted
        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, wasInterruptedAfter(0));
    }

    @Test
    public void testCancelDuringAnonymousReportDownload() {
        connectDrone(mDrone, 1);

        // should be idle until the report list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());

        verify(mClient).listReports(mResultCb.capture());

        // mock listing successful result
        mResultCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_1);

        // full report download should start
        assertThat(mChangeCnt, is(2));
        assertThat(mCrashReportDownloader, isDownloading(0));

        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz")), eq(HttpReportClient.ReportType.FULL),
                mStatusCb.capture());

        // mock download completion
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // anonymous download should start
        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz" + CrashReportStorage.ANONYMOUS_REPORT_EXT)),
                eq(HttpReportClient.ReportType.LIGHT),
                mStatusCb.capture());

        // mock download cancel
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.CANCELED,
                HttpRequest.STATUS_CODE_UNKNOWN);

        // after a download cancel, the overall task should be interrupted
        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, wasInterruptedAfter(0));
    }

    @Test
    public void testDeleteCancel() {
        connectDrone(mDrone, 1);

        // should be idle until the report list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());

        verify(mClient).listReports(mResultCb.capture());

        // mock listing successful result
        mResultCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_1);

        // personal report download should start
        assertThat(mChangeCnt, is(2));
        assertThat(mCrashReportDownloader, isDownloading(0));

        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz")), eq(HttpReportClient.ReportType.FULL),
                mStatusCb.capture());

        // mock download completion
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // anonymous download should start
        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz" + CrashReportStorage.ANONYMOUS_REPORT_EXT)),
                eq(HttpReportClient.ReportType.LIGHT),
                mStatusCb.capture());

        // mock download completion
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // overall task should be successful
        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, hasDownloadedSuccessfully(1));

        // delete should be started
        verify(mClient).deleteReport(eq("report_001.tar.gz"), mStatusCb.capture());

        // mock delete cancel
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.CANCELED,
                HttpRequest.STATUS_CODE_UNKNOWN);

        // nothing should change
        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, hasDownloadedSuccessfully(1));
    }

    @Test
    public void testDownloadMultipleFiles() {
        connectDrone(mDrone, 1);

        // should be idle until the report list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());

        verify(mClient).listReports(mResultCb.capture());

        // mock listing successful result
        mResultCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_2);

        // first personal report download should start
        assertThat(mChangeCnt, is(2));
        assertThat(mCrashReportDownloader, isDownloading(0));

        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz")), eq(HttpReportClient.ReportType.FULL),
                mStatusCb.capture());

        // mock download completion
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // first anonymous report download should start
        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz" + CrashReportStorage.ANONYMOUS_REPORT_EXT)),
                eq(HttpReportClient.ReportType.LIGHT),
                mStatusCb.capture());

        // mock download completion
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // first report should be deleted from remote device
        verify(mClient).deleteReport(eq("report_001.tar.gz"), any());

        // second personal report download should start
        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, isDownloading(1));

        verify(mClient).downloadReport(eq("/data/report/report_002.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_002.tar.gz")), eq(HttpReportClient.ReportType.FULL),
                mStatusCb.capture());

        // mock download completion
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // second anonymous report download should start
        verify(mClient).downloadReport(eq("/data/report/report_002.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_002.tar.gz" + CrashReportStorage.ANONYMOUS_REPORT_EXT)),
                eq(HttpReportClient.ReportType.LIGHT),
                mStatusCb.capture());

        // mock download completion
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        assertThat(mChangeCnt, is(4));
        assertThat(mCrashReportDownloader, hasDownloadedSuccessfully(2));

        // second report should then be deleted from remote device
        verify(mClient).deleteReport(eq("report_002.tar.gz"), mStatusCb.capture());
    }

    @Test
    public void testDataSyncUnavailableDuringDownload() {
        connectDrone(mDrone, 1);

        // should be idle until the report list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());

        verify(mClient).listReports(mResultCb.capture());

        // mock listing successful result
        mResultCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_2);

        // first personal report download should start
        assertThat(mChangeCnt, is(2));
        assertThat(mCrashReportDownloader, isDownloading(0));

        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz")), eq(HttpReportClient.ReportType.FULL),
                mStatusCb.capture());

        // mock download completion
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // first anonymous report download should start
        assertThat(mChangeCnt, is(2));
        assertThat(mCrashReportDownloader, isDownloading(0));

        verify(mClient).downloadReport(eq("/data/report/report_001.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_001.tar.gz" + CrashReportStorage.ANONYMOUS_REPORT_EXT)),
                eq(HttpReportClient.ReportType.LIGHT),
                mStatusCb.capture());

        // mock download completion
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // first report should be deleted from remote device
        verify(mClient).deleteReport(eq("report_001.tar.gz"), any());

        assertThat(mChangeCnt, is(3));
        assertThat(mCrashReportDownloader, isDownloading(1));

        // second personal report download should start
        verify(mClient).downloadReport(eq("/data/report/report_002.tar.gz"),
                eq(new File(REPORT_STORAGE, "report_002.tar.gz")),
                eq(HttpReportClient.ReportType.FULL),
                mStatusCb.capture());

        // mock data sync unavailable - make drone flying
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                ArsdkFeatureArdrone3.PilotingstateFlyingstatechangedState.FLYING));

        // all tasks should be canceled
        verify(mClient).dispose();

        // mock download cancel
        mStatusCb.getValue().onRequestComplete(HttpRequest.Status.CANCELED,
                HttpRequest.STATUS_CODE_UNKNOWN);

        assertThat(mChangeCnt, is(4));
        assertThat(mCrashReportDownloader, wasInterruptedAfter(1));
    }

    @Test
    public void testDataSyncAllowanceChange() {
        connectDrone(mDrone, 1, () -> mMockArsdkCore.commandReceived(1,
                ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                        PilotingstateFlyingstatechangedState.FLYING)));

        // initial state, data sync is not allowed
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());

        verify(mClient, never()).listReports(any());

        // mock piloting state change to allow data sync
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                PilotingstateFlyingstatechangedState.EMERGENCY));

        // now data sync is allowed, but downloader should be idle until the report list is received
        assertThat(mChangeCnt, is(1));
        assertThat(mCrashReportDownloader, isIdle());

        verify(mClient).listReports(mResultCb.capture());

        // mock listing successful result
        mResultCb.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200, REPORT_LIST_1);

        // download should be started
        assertThat(mChangeCnt, is(2));
        assertThat(mCrashReportDownloader, isDownloading(0));

        // mock piloting state change to disallow data sync
        mMockArsdkCore.commandReceived(1, ArsdkEncoder.encodeArdrone3PilotingStateFlyingStateChanged(
                PilotingstateFlyingstatechangedState.TAKINGOFF));

        // download is interrupted
        verify(mClient).dispose();
    }
}
