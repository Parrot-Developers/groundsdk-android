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

package com.parrot.drone.groundsdk.internal.engine.crashreport;

import android.content.Context;

import com.parrot.drone.groundsdk.MockAppStorageProvider;
import com.parrot.drone.groundsdk.facility.CrashReporter;
import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.engine.MockEngineController;
import com.parrot.drone.groundsdk.internal.http.HttpCrashMlClient;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.groundsdk.internal.tasks.MockTask;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.internal.utility.CrashReportStorage;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.groundsdk.internal.utility.UserAccountInfo;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class CrashReportEngineTest {

    private static final String MOCK_ACCOUNT = "mock-account";

    private static final File
            REPORT_A = new File("/tmp/crash/workdir", "report_a"),
            REPORT_A_ANON = new File("/tmp/crash/workdir", "report_a" + CrashReportStorage.ANONYMOUS_REPORT_EXT),
            REPORT_B = new File("/tmp/crash/workdir", "report_b");

    private CrashReportEngine mEngine;

    private MockComponentStore<Facility> mFacilityStore;

    private CrashReporter mReporter;

    private int mFacilityChangeCnt;

    private SystemConnectivity mMockConnectivity;

    private UserAccountInfo mMockUserAccountInfo;

    private MockTask<Collection<File>> mMockCollectReportsTask;

    private HttpCrashMlClient mMockHttpClient;

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
        mMockConnectivity = mock(SystemConnectivity.class);
        mMockUserAccountInfo = mock(UserAccountInfo.class);
        doReturn(MOCK_ACCOUNT).when(mMockUserAccountInfo).getAccountIdentifier();
        doReturn(new Date(0)).when(mMockUserAccountInfo).getPersonalDataAllowanceDate();

        mMockCollectReportsTask = spy(new MockTask<>());

        mFacilityStore = new MockComponentStore<>();
        mFacilityStore.registerObserver(CrashReporter.class, () -> {
            mReporter = mFacilityStore.get(CrashReporter.class);
            mFacilityChangeCnt++;
        });
        mFacilityChangeCnt = 0;

        UtilityRegistry utilities = new UtilityRegistry();
        mEngine = mock(CrashReportEngine.class, withSettings()
                .useConstructor(MockEngineController.create(mock(Context.class),
                        utilities.registerUtility(SystemConnectivity.class, mMockConnectivity)
                                 .registerUtility(UserAccountInfo.class, mMockUserAccountInfo), mFacilityStore))
                .defaultAnswer(CALLS_REAL_METHODS));

        mMockHttpClient = mock(HttpCrashMlClient.class);
        doReturn((HttpRequest) () -> {}).when(mMockHttpClient).upload(any(), any(), any());
        doReturn(mMockHttpClient).when(mEngine).createHttpClient();

        // engine should publish its utility
        assertThat(utilities.getUtility(CrashReportStorage.class), notNullValue());

        doReturn(mMockCollectReportsTask).when(mEngine).launchCollectJob();
    }

    @After
    public void teardown() {
        MockHttpSession.resetDefaultClients();
    }

    @Test
    public void testDirectories() {
        assertThat(mEngine.getEngineDirectory(), is(
                new File(ApplicationStorageProvider.getInstance().getInternalAppFileCache(), "crash")));
        assertThat(mEngine.getWorkDirectory().getParentFile(), is(mEngine.getEngineDirectory()));
    }

    @Test
    public void testStart() {
        mEngine.start();

        assertThat(mFacilityChangeCnt, is(1));
        assertThat(mReporter, notNullValue());
        assertThat(mReporter.isUploading(), is(false));
        assertThat(mReporter.getPendingCount(), is(0));

        verify(mMockConnectivity).monitorWith(any());
        verify(mEngine).launchCollectJob();
    }

    @Test
    public void testStop() {
        ArgumentCaptor<SystemConnectivity.Monitor> monitorListenerCaptor =
                ArgumentCaptor.forClass(SystemConnectivity.Monitor.class);

        mEngine.start();

        // collect task should start
        verify(mEngine).launchCollectJob();

        assertThat(mFacilityChangeCnt, is(1));
        verify(mMockConnectivity).monitorWith(monitorListenerCaptor.capture());

        // add a report and mock internet available so that an upload task is launched
        mEngine.queueForUpload(Collections.singleton(REPORT_A));
        mockInternetAvailable();

        verify(mMockHttpClient).upload(eq(REPORT_A), eq(MOCK_ACCOUNT), any());
        mEngine.requestStop(null);
        mEngine.stop();

        verify(mMockConnectivity).disposeMonitor(monitorListenerCaptor.getValue());

        verify(mMockCollectReportsTask).cancel();
        verify(mMockHttpClient).dispose();
    }

    @Test
    public void testQueueReport() {
        mEngine.start();
        assertThat(mFacilityChangeCnt, is(1));

        // add report without internet being available
        mEngine.queueForUpload(Collections.singleton(REPORT_A));

        // pending count should update
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(1));
        // but upload should not start yet
        assertThat(mReporter.isUploading(), is(false));
        verify(mMockHttpClient, never()).upload(any(), any(), any());

        // make internet available
        mockInternetAvailable();

        // since internet is available, expect upload to start
        verify(mMockHttpClient).upload(eq(REPORT_A), eq(MOCK_ACCOUNT), any());
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(true));

        // verify that pending count updates properly when reports are queued
        mEngine.queueForUpload(Collections.singleton(REPORT_B));

        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mReporter.getPendingCount(), is(2));
    }

    @Test
    public void testUploadSuccess() {
        mEngine.start();
        assertThat(mFacilityChangeCnt, is(1));

        // queue some reports
        mEngine.queueForUpload(Collections.singleton(REPORT_A));
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(1));

        mEngine.queueForUpload(Collections.singleton(REPORT_B));
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.getPendingCount(), is(2));

        assertThat(mReporter.isUploading(), is(false));
        verify(mMockHttpClient, never()).upload(any(), any(), any());

        // make internet available
        mockInternetAvailable();

        // since internet is available, expect upload to start
        ArgumentCaptor<HttpCrashMlClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpCrashMlClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(REPORT_A), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // uploading flag should have been armed
        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mReporter.isUploading(), is(true));

        // mock a successful upload
        cbCaptor.getValue().onRequestComplete(HttpCrashMlClient.UploadCallback.Status.SUCCESS);

        // pending count should update
        assertThat(mFacilityChangeCnt, is(5));
        assertThat(mReporter.getPendingCount(), is(1));
        assertThat(mReporter.isUploading(), is(true));

        // next report should upload
        verify(mMockHttpClient).upload(eq(REPORT_B), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // mock a successful upload
        cbCaptor.getValue().onRequestComplete(HttpCrashMlClient.UploadCallback.Status.SUCCESS);

        // pending count should update
        assertThat(mFacilityChangeCnt, is(6));
        assertThat(mReporter.getPendingCount(), is(0));
        // since there are no more reports to upload, uploading flag should go off
        assertThat(mReporter.isUploading(), is(false));

        verifyNoMoreInteractions(mMockHttpClient);
    }

    @Test
    public void testUploadBadReportError() {
        mEngine.start();
        assertThat(mFacilityChangeCnt, is(1));

        // queue some reports
        mEngine.queueForUpload(Collections.singleton(REPORT_A));
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(1));

        mEngine.queueForUpload(Collections.singleton(REPORT_B));
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.getPendingCount(), is(2));

        assertThat(mReporter.isUploading(), is(false));
        verify(mMockHttpClient, never()).upload(any(), any(), any());

        // make internet available
        mockInternetAvailable();

        // since internet is available, expect upload to start
        ArgumentCaptor<HttpCrashMlClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpCrashMlClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(REPORT_A), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // uploading flag should have been armed
        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mReporter.isUploading(), is(true));

        // mock a bad report error
        cbCaptor.getValue().onRequestComplete(HttpCrashMlClient.UploadCallback.Status.BAD_REPORT);

        // pending count should update
        assertThat(mFacilityChangeCnt, is(5));
        assertThat(mReporter.getPendingCount(), is(1));
        assertThat(mReporter.isUploading(), is(true));

        // next report should upload
        verify(mMockHttpClient).upload(eq(REPORT_B), eq(MOCK_ACCOUNT), cbCaptor.capture());
    }

    @Test
    public void testUploadBadRequestError() {
        mEngine.start();

        assertThat(mFacilityChangeCnt, is(1));

        // queue some reports
        mEngine.queueForUpload(Collections.singleton(REPORT_A));
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(1));

        mEngine.queueForUpload(Collections.singleton(REPORT_B));
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.getPendingCount(), is(2));

        assertThat(mReporter.isUploading(), is(false));
        verify(mMockHttpClient, never()).upload(any(), any(), any());

        // make internet available
        mockInternetAvailable();

        // since internet is available, expect upload to start
        ArgumentCaptor<HttpCrashMlClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpCrashMlClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(REPORT_A), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // uploading flag should have been armed
        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mReporter.isUploading(), is(true));

        // mock upload failure
        cbCaptor.getValue().onRequestComplete(HttpCrashMlClient.UploadCallback.Status.BAD_REQUEST);

        assertThat(mFacilityChangeCnt, is(5));
        // pending count should update
        assertThat(mReporter.getPendingCount(), is(1));
        // uploading flag should have been disarmed
        assertThat(mReporter.isUploading(), is(false));

        verifyNoMoreInteractions(mMockHttpClient);

        // mock internet available again (to trigger the upload queue)
        mockInternetAvailable();

        // upload should resume from the failed report
        verify(mMockHttpClient, times(1)).upload(eq(REPORT_B), eq(MOCK_ACCOUNT), cbCaptor.capture());

        assertThat(mFacilityChangeCnt, is(6));
        assertThat(mReporter.isUploading(), is(true));
        assertThat(mReporter.getPendingCount(), is(1));
    }

    @Test
    public void testUploadOtherError() {
        mEngine.start();

        assertThat(mFacilityChangeCnt, is(1));

        // queue some reports
        mEngine.queueForUpload(Collections.singleton(REPORT_A));
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(1));

        mEngine.queueForUpload(Collections.singleton(REPORT_B));
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.getPendingCount(), is(2));

        assertThat(mReporter.isUploading(), is(false));
        verify(mMockHttpClient, never()).upload(any(), any(), any());

        // make internet available
        mockInternetAvailable();

        // since internet is available, expect upload to start
        ArgumentCaptor<HttpCrashMlClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpCrashMlClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(REPORT_A), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // uploading flag should have been armed
        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mReporter.isUploading(), is(true));

        // mock upload failure
        cbCaptor.getValue().onRequestComplete(HttpCrashMlClient.UploadCallback.Status.SERVER_ERROR);

        // uploading flag should have been disarmed
        assertThat(mFacilityChangeCnt, is(5));
        assertThat(mReporter.isUploading(), is(false));
        assertThat(mReporter.getPendingCount(), is(2));

        verifyNoMoreInteractions(mMockHttpClient);

        // mock internet available again (to trigger the upload queue)
        mockInternetAvailable();

        // upload should resume from the failed report
        verify(mMockHttpClient, times(2)).upload(eq(REPORT_A), eq(MOCK_ACCOUNT), cbCaptor.capture());

        assertThat(mFacilityChangeCnt, is(6));
        assertThat(mReporter.isUploading(), is(true));
        assertThat(mReporter.getPendingCount(), is(2));
    }

    @Test
    public void testInternetBecomesUnavailable() {
        mEngine.start();
        // make internet available
        mockInternetAvailable();

        // queue some reports
        mEngine.queueForUpload(Collections.singleton(REPORT_A));
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(1));

        // uploading flag should have been armed
        assertThat(mReporter.isUploading(), is(true));

        // since internet is available, expect upload to start
        ArgumentCaptor<HttpCrashMlClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpCrashMlClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(REPORT_A), eq(MOCK_ACCOUNT), cbCaptor.capture());

        mEngine.queueForUpload(Collections.singleton(REPORT_B));
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.getPendingCount(), is(2));

        // make internet unavailable
        mockInternetUnavailable();

        // task should have been canceled
        verify(mMockHttpClient).dispose();

        // mock request failure
        cbCaptor.getValue().onRequestComplete(HttpCrashMlClient.UploadCallback.Status.CANCELED);
        // uploading flag should have been disarmed
        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mReporter.isUploading(), is(false));
        assertThat(mReporter.getPendingCount(), is(2));
    }

    @Test
    public void testUploadWithUserAccount() {
        File personalReport = spy(REPORT_A);
        File anonymousReport = spy(REPORT_A_ANON);

        // pretend report files exists so that they can be deleted
        //noinspection ResultOfMethodCallIgnored
        doReturn(true).when(personalReport).exists();
        //noinspection ResultOfMethodCallIgnored
        doReturn(true).when(anonymousReport).exists();

        mEngine.start();
        // make internet available
        mockInternetAvailable();

        mEngine.queueForUpload(Arrays.asList(personalReport, anonymousReport));
        assertThat(mFacilityChangeCnt, is(2));
        // this is a single report with both anonymous and personal data
        assertThat(mReporter.getPendingCount(), is(1));
        // uploading flag should have been armed
        assertThat(mReporter.isUploading(), is(true));

        // since internet is available, expect upload to start
        // since an user account is available, expect personal report upload
        ArgumentCaptor<HttpCrashMlClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpCrashMlClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(personalReport), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // mock upload success
        cbCaptor.getValue().onRequestComplete(HttpCrashMlClient.UploadCallback.Status.SUCCESS);

        // both personal and anonymous reports should be deleted
        //noinspection ResultOfMethodCallIgnored
        verify(personalReport).delete();
        //noinspection ResultOfMethodCallIgnored
        verify(anonymousReport).delete();

        // no other upload should occur
        verifyNoMoreInteractions(mMockHttpClient);

        // uploading flag should have been disarmed
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(false));
        assertThat(mReporter.getPendingCount(), is(0));
    }

    @Test
    public void testUploadWithNoUserAccountYetAnonymousData() {
        File personalReport = spy(REPORT_A);
        File anonymousReport = spy(REPORT_A_ANON);

        // pretend report files exist so that they can be deleted
        //noinspection ResultOfMethodCallIgnored
        doReturn(true).when(personalReport).exists();
        //noinspection ResultOfMethodCallIgnored
        doReturn(true).when(anonymousReport).exists();

        mEngine.start();

        // make internet available
        mockInternetAvailable();
        // make anonymous data allowed (no user account)
        mockUserAccountClear(true);

        mEngine.queueForUpload(Arrays.asList(personalReport, anonymousReport));
        assertThat(mFacilityChangeCnt, is(2));
        // this is a single report with both anonymous and personal data
        assertThat(mReporter.getPendingCount(), is(1));

        // uploading flag should have been armed
        assertThat(mReporter.isUploading(), is(true));

        // since internet is available, expect upload to start
        // since no account is available, but anonymous data are allowed, expect anonymous report upload
        ArgumentCaptor<HttpCrashMlClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpCrashMlClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(anonymousReport), eq(null), cbCaptor.capture());

        // mock upload success
        cbCaptor.getValue().onRequestComplete(HttpCrashMlClient.UploadCallback.Status.SUCCESS);

        // anonymous report should be deleted
        //noinspection ResultOfMethodCallIgnored
        verify(anonymousReport).delete();
        // but personal report should be kept
        //noinspection ResultOfMethodCallIgnored
        verify(personalReport, never()).delete();

        // no other upload should occur
        verifyNoMoreInteractions(mMockHttpClient);

        // uploading flag should have been disarmed
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(false));
        // there should still be a report to upload, since the personal report remains
        assertThat(mReporter.getPendingCount(), is(1));
    }

    @Test
    public void testNoUploadWhenNeitherUserAccountNorAnonymousData() {
        File personalReport = spy(REPORT_A);
        File anonymousReport = spy(REPORT_A_ANON);

        // pretend report files exist so that they can be deleted
        //noinspection ResultOfMethodCallIgnored
        doReturn(true).when(personalReport).exists();
        //noinspection ResultOfMethodCallIgnored
        doReturn(true).when(anonymousReport).exists();

        mEngine.start();

        assertThat(mFacilityChangeCnt, is(1));
        assertThat(mReporter.isUploading(), is(false));
        assertThat(mReporter.getPendingCount(), is(0));

        // make internet available
        mockInternetAvailable();

        // make anonymous data denied (no user account)
        mockUserAccountClear(false);

        mEngine.queueForUpload(Arrays.asList(personalReport, anonymousReport));

        // no other upload should occur
        verifyNoMoreInteractions(mMockHttpClient);

        // uploading flag should not be armed
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.isUploading(), is(false));
        // report should be pending
        assertThat(mReporter.getPendingCount(), is(1));
    }

    @Test
    public void testUserAccountSet() {
        File personalReport = spy(REPORT_A);

        mEngine.start();

        // make internet available
        mockInternetAvailable();
        // make anonymous data allowed (no user account)
        mockUserAccountClear(true);

        mEngine.queueForUpload(Collections.singletonList(personalReport));

        // since no account is available, personal report should not be uploaded
        verifyZeroInteractions(mMockHttpClient);

        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(1));
        assertThat(mReporter.isUploading(), is(false));

        // now mock user account set
        mockUserAccountSet();

        // now upload should start
        ArgumentCaptor<HttpCrashMlClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpCrashMlClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(personalReport), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // uploading flag should have been armed
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(true));
        assertThat(mReporter.getPendingCount(), is(1));
    }

    @Test
    public void testUserAccountClearAllowAnonymousData() {
        File anonymousReport = spy(REPORT_A_ANON);

        mEngine.start();

        // make internet available
        mockInternetAvailable();

        mEngine.queueForUpload(Collections.singletonList(anonymousReport));

        // since an account is available, anonymous report should not be uploaded
        verifyZeroInteractions(mMockHttpClient);

        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(1));
        assertThat(mReporter.isUploading(), is(false));

        // now mock user account clear, allowing anonymous data
        mockUserAccountClear(true);

        // now upload should start
        ArgumentCaptor<HttpCrashMlClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpCrashMlClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(anonymousReport), eq(null), cbCaptor.capture());

        // uploading flag should have been armed
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(true));
        assertThat(mReporter.getPendingCount(), is(1));
    }

    @Test
    public void testUploadSkipsReportsToDrop() {
        File reportA = spy(REPORT_A), reportB = spy(REPORT_B);

        long dropRequest = TimeUnit.DAYS.toMillis(10);

        // mock first report recorded before user account drop request date
        //noinspection ResultOfMethodCallIgnored
        doReturn(dropRequest - TimeUnit.HOURS.toMillis(1)).when(reportA).lastModified();
        // mock second report recorded after user account drop request date
        //noinspection ResultOfMethodCallIgnored
        doReturn(dropRequest + TimeUnit.HOURS.toMillis(1)).when(reportB).lastModified();

        // mock some user account, with drop request
        doReturn(MOCK_ACCOUNT).when(mMockUserAccountInfo).getAccountIdentifier();
        doReturn(new Date(dropRequest)).when(mMockUserAccountInfo).getPersonalDataAllowanceDate();

        mEngine.start();

        // make internet available
        mockInternetAvailable();

        assertThat(mFacilityChangeCnt, is(1));
        assertThat(mReporter.isUploading(), is(false));
        assertThat(mReporter.getPendingCount(), is(0));

        // collect task should start
        verify(mEngine).launchCollectJob();

        // mock collect task result
        mEngine.queueForUpload(Arrays.asList(reportA, reportB));

        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.isUploading(), is(true));
        assertThat(mReporter.getPendingCount(), is(2));

        // first report should not be uploaded
        verify(mMockHttpClient, never()).upload(eq(reportA), any(), any());

        // second report should be uploaded
        ArgumentCaptor<HttpCrashMlClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpCrashMlClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(reportB), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // mock successful upload of second report
        cbCaptor.getValue().onRequestComplete(HttpCrashMlClient.UploadCallback.Status.SUCCESS);

        // upload should stop
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(false));
        assertThat(mReporter.getPendingCount(), is(1));

        // first report should still not be uploaded
        verifyNoMoreInteractions(mMockHttpClient);
    }

    private void mockInternetAvailable() {
        mockInternetAvailabilityChange(true);
    }

    private void mockInternetUnavailable() {
        mockInternetAvailabilityChange(false);
    }

    private void mockInternetAvailabilityChange(boolean available) {
        when(mMockConnectivity.isInternetAvailable()).thenReturn(available);
        ArgumentCaptor<SystemConnectivity.Monitor> monitorListenerCaptor =
                ArgumentCaptor.forClass(SystemConnectivity.Monitor.class);
        verify(mMockConnectivity, times(1)).monitorWith(monitorListenerCaptor.capture());
        monitorListenerCaptor.getValue().onInternetAvailabilityChanged(available);
    }

    private void mockUserAccountSet() {
        doReturn(MOCK_ACCOUNT).when(mMockUserAccountInfo).getAccountIdentifier();
        doReturn(false).when(mMockUserAccountInfo).isAnonymousDataUploadAllowed();
        ArgumentCaptor<UserAccountInfo.Monitor> monitorListenerCaptor =
                ArgumentCaptor.forClass(UserAccountInfo.Monitor.class);
        verify(mMockUserAccountInfo, times(1)).monitorWith(monitorListenerCaptor.capture());
        monitorListenerCaptor.getValue().onChange(mMockUserAccountInfo);
    }

    private void mockUserAccountClear(boolean allowAnonymousData) {
        doReturn(null).when(mMockUserAccountInfo).getAccountIdentifier();
        doReturn(allowAnonymousData).when(mMockUserAccountInfo).isAnonymousDataUploadAllowed();
        ArgumentCaptor<UserAccountInfo.Monitor> monitorListenerCaptor =
                ArgumentCaptor.forClass(UserAccountInfo.Monitor.class);
        verify(mMockUserAccountInfo, times(1)).monitorWith(monitorListenerCaptor.capture());
        monitorListenerCaptor.getValue().onChange(mMockUserAccountInfo);
    }

}
