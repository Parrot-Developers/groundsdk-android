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

package com.parrot.drone.groundsdk.internal.engine.blackbox;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.parrot.drone.groundsdk.MockAppStorageProvider;
import com.parrot.drone.groundsdk.facility.BlackBoxReporter;
import com.parrot.drone.groundsdk.facility.Facility;
import com.parrot.drone.groundsdk.internal.ApplicationStorageProvider;
import com.parrot.drone.groundsdk.internal.GroundSdkConfig;
import com.parrot.drone.groundsdk.internal.MockComponentStore;
import com.parrot.drone.groundsdk.internal.engine.MockEngineController;
import com.parrot.drone.groundsdk.internal.http.HttpBlackBoxClient;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.MockHttpSession;
import com.parrot.drone.groundsdk.internal.tasks.MockTask;
import com.parrot.drone.groundsdk.internal.tasks.TestExecutor;
import com.parrot.drone.groundsdk.internal.utility.BlackBoxStorage;
import com.parrot.drone.groundsdk.internal.utility.SystemConnectivity;
import com.parrot.drone.groundsdk.internal.utility.UserAccountInfo;
import com.parrot.drone.groundsdk.internal.utility.UtilityRegistry;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class BlackBoxEngineTest {

    private static final File
            BLACKBOX_A = new File("/tmp/blackbox/workdir", "blackbox_a"),
            BLACKBOX_B = new File("/tmp/blackbox/workdir", "blackbox_b");

    private static final String MOCK_ACCOUNT = "mock-account";

    @Rule
    public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private BlackBoxEngine mEngine;

    private BlackBoxStorage mBlackBoxStorage;

    private MockComponentStore<Facility> mFacilityStore;

    private BlackBoxReporter mReporter;

    private int mFacilityChangeCnt;

    private SystemConnectivity mMockConnectivity;

    private UserAccountInfo mMockUserAccountInfo;

    private MockTask<Collection<File>> mMockCollectTask;

    private MockTask<File> mMockArchiveTask;

    private HttpBlackBoxClient mMockHttpClient;

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
        GroundSdkConfig.loadDefaults();
        GroundSdkConfig.get().enableBlackBoxSupport(true, 0, "blackbox");
        mMockConnectivity = mock(SystemConnectivity.class);
        mMockUserAccountInfo = mock(UserAccountInfo.class);
        doReturn(MOCK_ACCOUNT).when(mMockUserAccountInfo).getAccountIdentifier();
        doReturn(new Date(0)).when(mMockUserAccountInfo).getPersonalDataAllowanceDate();

        mMockCollectTask = spy(new MockTask<>());
        mMockArchiveTask = spy(new MockTask<>());

        mFacilityStore = new MockComponentStore<>();
        mFacilityStore.registerObserver(BlackBoxReporter.class, () -> {
            mReporter = mFacilityStore.get(BlackBoxReporter.class);
            mFacilityChangeCnt++;
        });
        mFacilityChangeCnt = 0;

        UtilityRegistry utilities = new UtilityRegistry();
        mEngine = mock(BlackBoxEngine.class, withSettings()
                .useConstructor(MockEngineController.create(mContext,
                        utilities.registerUtility(SystemConnectivity.class, mMockConnectivity)
                                 .registerUtility(UserAccountInfo.class, mMockUserAccountInfo), mFacilityStore))
                .defaultAnswer(CALLS_REAL_METHODS));

        mMockHttpClient = mock(HttpBlackBoxClient.class);
        doReturn((HttpRequest) () -> {}).when(mMockHttpClient).upload(any(), any(), any());
        doReturn(mMockHttpClient).when(mEngine).createHttpClient();

        // engine should publish its utility
        mBlackBoxStorage = utilities.getUtility(BlackBoxStorage.class);
        assertThat(mBlackBoxStorage, notNullValue());

        doReturn(mMockCollectTask).when(mEngine).launchCollectJob();
        doReturn(mMockArchiveTask).when(mEngine).launchArchiveJob(any());
    }

    @After
    public void teardown() {
        MockHttpSession.resetDefaultClients();
    }

    @Test
    public void testDirectories() {
        assertThat(mEngine.getEngineDirectory(), is(
                new File(ApplicationStorageProvider.getInstance().getInternalAppFileCache(), "blackbox")));
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

        // mock an archive task starts
        BlackBoxStorage.BlackBox blackBox = mock(BlackBoxStorage.BlackBox.class);
        mBlackBoxStorage.notifyBlackBoxReady(blackBox);

        verify(mEngine).launchArchiveJob(blackBox);

        // add a blackbox and mock internet available so that an upload task is launched
        mEngine.queueForUpload(Collections.singleton(BLACKBOX_A));
        mockInternetAvailable();

        verify(mMockHttpClient).upload(eq(BLACKBOX_A), eq(MOCK_ACCOUNT), any());
        mEngine.requestStop(null);
        mEngine.stop();

        verify(mMockConnectivity).disposeMonitor(monitorListenerCaptor.getValue());

        verify(mMockCollectTask).cancel();
        verify(mMockArchiveTask).cancel();
        verify(mMockHttpClient).dispose();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testCopyToPublicFolder() {
        File srcFile = new File(mTemporaryFolder.getRoot(), "blackbox_a");
        File dstDir = new File(mContext.getExternalFilesDir(null), "blackbox");
        File dstFile = new File(dstDir, "blackbox_a");
        try {
            // create blackbox file
            new FileOutputStream(srcFile).close();

            // copy to public folder
            mEngine.copyToPublicFolder(srcFile);

            // check that file has been copied
            assertThat(dstFile.exists(), is(true));
        } catch (IOException | InterruptedException ignored) {
        } finally {
            dstFile.delete();
            dstDir.delete();
        }
    }

    @Test
    public void testQueueForUpload() {
        mEngine.start();
        assertThat(mFacilityChangeCnt, is(1));

        // add blackbox without internet being available
        mEngine.queueForUpload(Collections.singleton(BLACKBOX_A));

        // pending count should update
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(1));
        // but upload should not start yet
        assertThat(mReporter.isUploading(), is(false));
        verify(mMockHttpClient, never()).upload(any(), any(), any());

        // make internet available
        mockInternetAvailable();

        // since internet is available, expect upload to start
        verify(mMockHttpClient).upload(eq(BLACKBOX_A), eq(MOCK_ACCOUNT), any());
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(true));

        // verify that pending count updates properly when blackboxes are queued
        mEngine.queueForUpload(Collections.singleton(BLACKBOX_B));

        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mReporter.getPendingCount(), is(2));
    }

    @Test
    public void testUploadSuccess() {
        mEngine.start();
        assertThat(mFacilityChangeCnt, is(1));

        // queue some blackboxes
        mEngine.queueForUpload(Arrays.asList(BLACKBOX_A, BLACKBOX_B));
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(2));
        assertThat(mReporter.isUploading(), is(false));

        verify(mMockHttpClient, never()).upload(any(), any(), any());

        // make internet available
        mockInternetAvailable();

        // since internet is available, expect upload to start
        ArgumentCaptor<HttpBlackBoxClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpBlackBoxClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(BLACKBOX_A), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // uploading flag should have been armed
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(true));

        // mock a successful upload
        cbCaptor.getValue().onRequestComplete(HttpBlackBoxClient.UploadCallback.Status.SUCCESS);

        // pending count should update
        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mReporter.getPendingCount(), is(1));
        assertThat(mReporter.isUploading(), is(true));

        // next upload should start
        verify(mMockHttpClient).upload(eq(BLACKBOX_B), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // mock a successful upload
        cbCaptor.getValue().onRequestComplete(HttpBlackBoxClient.UploadCallback.Status.SUCCESS);

        // pending count should update
        assertThat(mFacilityChangeCnt, is(5));
        assertThat(mReporter.getPendingCount(), is(0));
        // since there are no more blackboxes to upload, uploading flag should go off
        assertThat(mReporter.isUploading(), is(false));

        verifyNoMoreInteractions(mMockHttpClient);
    }

    @Test
    public void testUploadBadReportError() {
        mEngine.start();
        assertThat(mFacilityChangeCnt, is(1));

        // queue some blackboxes
        mEngine.queueForUpload(Arrays.asList(BLACKBOX_A, BLACKBOX_B));
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(2));
        assertThat(mReporter.isUploading(), is(false));

        verify(mMockHttpClient, never()).upload(any(), any(), any());

        // make internet available
        mockInternetAvailable();

        // since internet is available, expect upload to start
        ArgumentCaptor<HttpBlackBoxClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpBlackBoxClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(BLACKBOX_A), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // uploading flag should have been armed
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(true));

        // mock a bad report error
        cbCaptor.getValue().onRequestComplete(HttpBlackBoxClient.UploadCallback.Status.BAD_BLACKBOX);

        // pending count should update
        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mReporter.getPendingCount(), is(1));
        assertThat(mReporter.isUploading(), is(true));

        // next report should upload
        verify(mMockHttpClient).upload(eq(BLACKBOX_B), eq(MOCK_ACCOUNT), cbCaptor.capture());
    }

    @Test
    public void testUploadBadRequestError() {
        mEngine.start();

        assertThat(mFacilityChangeCnt, is(1));

        // queue some blackboxes
        mEngine.queueForUpload(Arrays.asList(BLACKBOX_A, BLACKBOX_B));
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(2));
        assertThat(mReporter.isUploading(), is(false));

        verify(mMockHttpClient, never()).upload(any(), any(), any());

        // make internet available
        mockInternetAvailable();

        // since internet is available, expect upload to start
        ArgumentCaptor<HttpBlackBoxClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpBlackBoxClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(BLACKBOX_A), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // uploading flag should have been armed
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(true));

        // mock upload failure
        cbCaptor.getValue().onRequestComplete(HttpBlackBoxClient.UploadCallback.Status.BAD_REQUEST);

        assertThat(mFacilityChangeCnt, is(4));
        // pending count should update
        assertThat(mReporter.getPendingCount(), is(1));
        // uploading flag should have been disarmed
        assertThat(mReporter.isUploading(), is(false));

        verifyNoMoreInteractions(mMockHttpClient);

        // mock internet available again (to trigger the upload queue)
        mockInternetAvailable();

        // upload should resume from the failed report
        verify(mMockHttpClient, times(1)).upload(eq(BLACKBOX_B), eq(MOCK_ACCOUNT), cbCaptor.capture());

        assertThat(mFacilityChangeCnt, is(5));
        assertThat(mReporter.isUploading(), is(true));
        assertThat(mReporter.getPendingCount(), is(1));
    }

    @Test
    public void testUploadOtherError() {
        mEngine.start();

        assertThat(mFacilityChangeCnt, is(1));

        // queue some blackboxes
        mEngine.queueForUpload(Arrays.asList(BLACKBOX_A, BLACKBOX_B));
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(2));
        assertThat(mReporter.isUploading(), is(false));

        verify(mMockHttpClient, never()).upload(any(), any(), any());

        // make internet available
        mockInternetAvailable();

        // since internet is available, expect upload to start
        ArgumentCaptor<HttpBlackBoxClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpBlackBoxClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(BLACKBOX_A), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // uploading flag should have been armed
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(true));

        // mock upload failure
        cbCaptor.getValue().onRequestComplete(HttpBlackBoxClient.UploadCallback.Status.SERVER_ERROR);

        // uploading flag should have been disarmed
        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mReporter.isUploading(), is(false));
        assertThat(mReporter.getPendingCount(), is(2));

        verifyNoMoreInteractions(mMockHttpClient);

        // mock internet available again (to trigger the upload queue)
        mockInternetAvailable();

        // upload should resume from the failed blackbox
        verify(mMockHttpClient, times(2)).upload(eq(BLACKBOX_A), eq(MOCK_ACCOUNT), cbCaptor.capture());

        assertThat(mFacilityChangeCnt, is(5));
        assertThat(mReporter.isUploading(), is(true));
        assertThat(mReporter.getPendingCount(), is(2));
    }

    @Test
    public void testInternetBecomesUnavailable() {
        mEngine.start();
        // make internet available
        mockInternetAvailable();

        // queue some blackboxes
        mEngine.queueForUpload(Collections.singleton(BLACKBOX_A));

        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(1));
        assertThat(mReporter.isUploading(), is(true));

        // since internet is available, expect upload to start
        ArgumentCaptor<HttpBlackBoxClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpBlackBoxClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(BLACKBOX_A), eq(MOCK_ACCOUNT), cbCaptor.capture());

        mEngine.queueForUpload(Collections.singleton(BLACKBOX_B));

        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.getPendingCount(), is(2));

        // make internet unavailable
        mockInternetUnavailable();

        // task should have been canceled
        verify(mMockHttpClient).dispose();

        // mock request failure
        cbCaptor.getValue().onRequestComplete(HttpBlackBoxClient.UploadCallback.Status.CANCELED);
        // uploading flag should have been disarmed
        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mReporter.isUploading(), is(false));
        assertThat(mReporter.getPendingCount(), is(2));
    }

    @Test
    public void testUserAccountSet() {
        File blackboxA = spy(BLACKBOX_A);

        // mock no user account
        doReturn(null).when(mMockUserAccountInfo).getAccountIdentifier();

        mEngine.start();
        // make internet available
        mockInternetAvailable();

        // queue some blackboxes
        mEngine.queueForUpload(Collections.singleton(blackboxA));
        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.getPendingCount(), is(1));

        // since there is no user account yet, should not be uploading
        assertThat(mReporter.isUploading(), is(false));
        verify(mMockHttpClient, never()).upload(any(), any(), any());

        // mock user account change
        mockUserAccountSet();

        // uploading flag should have been armed
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(true));
        assertThat(mReporter.getPendingCount(), is(1));

        // since internet is available, expect upload to start
        ArgumentCaptor<HttpBlackBoxClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpBlackBoxClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(blackboxA), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // mock upload completes
        cbCaptor.getValue().onRequestComplete(HttpBlackBoxClient.UploadCallback.Status.SUCCESS);

        // uploading flag should have been disarmed
        assertThat(mFacilityChangeCnt, is(4));
        assertThat(mReporter.isUploading(), is(false));
        assertThat(mReporter.getPendingCount(), is(0));

        // no other report should be uploaded
        verifyNoMoreInteractions(mMockHttpClient);
    }

    @Test
    public void testNoUploadWhenNoUserAccount() {
        File blackboxA = spy(BLACKBOX_A);

        // mock no user account
        doReturn(null).when(mMockUserAccountInfo).getAccountIdentifier();

        mEngine.start();

        // make internet available
        mockInternetAvailable();

        // collect task should start
        verify(mEngine).launchCollectJob();

        // mock an new black box is ready to be archived
        BlackBoxStorage.BlackBox blackBox = mock(BlackBoxStorage.BlackBox.class);
        mBlackBoxStorage.notifyBlackBoxReady(blackBox);

        // blackbox should be archived
        verify(mEngine).launchArchiveJob(blackBox);

        // mock archive task result
        mEngine.queueForUpload(Collections.singleton(blackboxA));

        // blackbox should not be uploaded
        verify(mMockHttpClient, never()).upload(any(), any(), any());
    }

    @Test
    public void testUploadSkipsBlackboxesToDrop() {
        File blackboxA = spy(BLACKBOX_A), blackboxB = spy(BLACKBOX_B);

        long dropRequest = TimeUnit.DAYS.toMillis(10);

        // mock first blackbox recorded before user account drop request date
        //noinspection ResultOfMethodCallIgnored
        doReturn(dropRequest - TimeUnit.HOURS.toMillis(1)).when(blackboxA).lastModified();
        // mock second blackbox recorded after user account drop request date
        //noinspection ResultOfMethodCallIgnored
        doReturn(dropRequest + TimeUnit.HOURS.toMillis(1)).when(blackboxB).lastModified();

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
        mEngine.queueForUpload(Arrays.asList(blackboxA, blackboxB));

        assertThat(mFacilityChangeCnt, is(2));
        assertThat(mReporter.isUploading(), is(true));
        assertThat(mReporter.getPendingCount(), is(2));

        // first blackbox should not be uploaded
        verify(mMockHttpClient, never()).upload(eq(blackboxA), any(), any());

        // second blackbox should be uploaded
        ArgumentCaptor<HttpBlackBoxClient.UploadCallback> cbCaptor
                = ArgumentCaptor.forClass(HttpBlackBoxClient.UploadCallback.class);
        verify(mMockHttpClient).upload(eq(blackboxB), eq(MOCK_ACCOUNT), cbCaptor.capture());

        // mock successful upload of second blackbox
        cbCaptor.getValue().onRequestComplete(HttpBlackBoxClient.UploadCallback.Status.SUCCESS);

        // upload should stop
        assertThat(mFacilityChangeCnt, is(3));
        assertThat(mReporter.isUploading(), is(false));
        assertThat(mReporter.getPendingCount(), is(1));

        // first blackbox should still not be uploaded
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
        verify(mMockConnectivity).monitorWith(monitorListenerCaptor.capture());
        monitorListenerCaptor.getValue().onInternetAvailabilityChanged(available);
    }

    private void mockUserAccountSet() {
        doReturn(MOCK_ACCOUNT).when(mMockUserAccountInfo).getAccountIdentifier();
        ArgumentCaptor<UserAccountInfo.Monitor> monitorListenerCaptor =
                ArgumentCaptor.forClass(UserAccountInfo.Monitor.class);
        verify(mMockUserAccountInfo, times(1)).monitorWith(monitorListenerCaptor.capture());
        monitorListenerCaptor.getValue().onChange(mMockUserAccountInfo);
    }
}
