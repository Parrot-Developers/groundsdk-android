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

import com.parrot.drone.groundsdk.device.Drone;
import com.parrot.drone.groundsdk.facility.firmware.FirmwareIdentifier;
import com.parrot.drone.groundsdk.internal.http.HttpRequest;
import com.parrot.drone.groundsdk.internal.http.HttpUpdateClient;
import com.parrot.drone.groundsdk.internal.utility.FirmwareDownloader;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

import static com.parrot.drone.groundsdk.internal.engine.firmware.TaskMatcher.firmwareDownloaderTaskCurrentCountIs;
import static com.parrot.drone.groundsdk.internal.engine.firmware.TaskMatcher.firmwareDownloaderTaskCurrentDownloadProgressIs;
import static com.parrot.drone.groundsdk.internal.engine.firmware.TaskMatcher.firmwareDownloaderTaskCurrentIs;
import static com.parrot.drone.groundsdk.internal.engine.firmware.TaskMatcher.firmwareDownloaderTaskDownloaded;
import static com.parrot.drone.groundsdk.internal.engine.firmware.TaskMatcher.firmwareDownloaderTaskOverallProgressIs;
import static com.parrot.drone.groundsdk.internal.engine.firmware.TaskMatcher.firmwareDownloaderTaskRemainIs;
import static com.parrot.drone.groundsdk.internal.engine.firmware.TaskMatcher.firmwareDownloaderTaskStateIs;
import static com.parrot.drone.groundsdk.internal.engine.firmware.TaskMatcher.firmwareDownloaderTaskTotalCountIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class FirmwareDownloaderTest {

    private static final FirmwareStoreEntry[] ENTRIES = new FirmwareStoreEntry[] {
            new FirmwareStoreEntryBuilder()
                    .model(Drone.Model.ANAFI_4K)
                    .version("0.0.1")
                    .size(200)
                    .build(),
            new FirmwareStoreEntryBuilder()
                    .model(Drone.Model.ANAFI_4K)
                    .version("0.0.2")
                    .size(300)
                    .build(),
            new FirmwareStoreEntryBuilder()
                    .model(Drone.Model.ANAFI_4K)
                    .version("0.0.3")
                    .size(500)
                    .build()
    };

    private static final FirmwareIdentifier[] FIRMWARES = new FirmwareIdentifier[] {
            ENTRIES[0].getFirmwareInfo().getFirmware(),
            ENTRIES[1].getFirmwareInfo().getFirmware(),
            ENTRIES[2].getFirmwareInfo().getFirmware()
    };

    private FirmwareEngine mEngine;

    private FirmwareStoreCore mStore;

    private HttpUpdateClient mHttpClient;

    private ArgumentCaptor<HttpRequest.ProgressStatusCallback> mHttpCallback;

    private FirmwareDownloaderCore mDownloader;

    private FirmwareDownloader.Task.Observer mObserver;

    @Before
    public void setUp() {
        mEngine = mock(FirmwareEngine.class);
        mStore = mock(FirmwareStoreCore.class);
        mHttpClient = mock(HttpUpdateClient.class);
        Persistence persistence = mock(Persistence.class);
        mObserver = mock(FirmwareDownloader.Task.Observer.class);
        mDownloader = new FirmwareDownloaderCore(mEngine);
        mHttpCallback = ArgumentCaptor.forClass(
                HttpRequest.ProgressStatusCallback.class);

        doAnswer(invocation -> new File(invocation.<URI>getArgument(1).getPath()))
                .when(persistence).makeLocalFirmwarePath(any(), any());

        doReturn(mStore).when(mEngine).firmwareStore();
        doReturn(persistence).when(mEngine).persistence();

        doAnswer(invocation -> {
            Stream.of(ENTRIES)
                  .filter(it -> it.getFirmwareInfo().getFirmware().equals(invocation.getArgument(0)))
                  .findFirst().ifPresent(it -> it.setUri(invocation.getArgument(1)));
            return null;
        }).when(mStore).addLocalFirmware(any(), any());

        for (FirmwareStoreEntry entry : ENTRIES) {
            entry.clearLocalUri();
            entry.clearRemoteUri();
        }
    }

    @Test
    public void testNullTaskIfAnyUnknownFirmware() {
        // store only knows FIRMWARES[0]
        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        // should fail since FIRMWARES[1] is unknown
        assertThat(mDownloader.downloadFromIds(
                new LinkedHashSet<>(Arrays.asList(FIRMWARES[0], FIRMWARES[1])), mObserver),
                nullValue());

        // store knows no firmware
        doReturn(null).when(mStore).getEntry(any());

        // should fail since no firmware is known
        assertThat(mDownloader.downloadFromIds(
                new LinkedHashSet<>(Arrays.asList(FIRMWARES[0], FIRMWARES[1])), mObserver),
                nullValue());


        // store only knows FIRMWARES[1]
        doReturn(ENTRIES[1]).when(mStore).getEntry(FIRMWARES[1]);

        // should fail since FIRMWARES[0] is unknown
        assertThat(mDownloader.downloadFromIds(
                new LinkedHashSet<>(Arrays.asList(FIRMWARES[0], FIRMWARES[1])), mObserver),
                nullValue());
    }

    @Test
    public void testNonNullTaskWhenAllFirmwareKnown() {
        // store knows both FIRMWARES[0] and FIRMWARES[1]
        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);
        doReturn(ENTRIES[1]).when(mStore).getEntry(FIRMWARES[1]);

        // should not fail since all firmwares are known
        assertThat(mDownloader.downloadFromIds(
                new LinkedHashSet<>(Arrays.asList(FIRMWARES[0], FIRMWARES[1])), mObserver),
                notNullValue());
    }

    @Test
    public void testTaskFailsIfNoUri() {
        // mock internet available, but no uri
        doReturn(mHttpClient).when(mEngine).httpClient();

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task, notNullValue());

        // observer should have been called only once
        verify(mObserver, times(1)).onChange(task);

        // should failed since FIRMWARES[0] has neither local nor remote URI
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));
    }

    @Test
    public void testTaskFailsIfNoInternet() {
        // mock remote uri, but no internet
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task, notNullValue());

        // observer should have been called only once
        verify(mObserver, times(1)).onChange(task);

        // should failed since FIRMWARES[0] has neither local nor remote URI
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));
    }

    @Test
    public void testTaskCompletesIfLocalUri() {
        ENTRIES[0].setUri(URI.create("file://firmware1"));
        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task, notNullValue());

        // observer should have been called only once
        verify(mObserver, times(1)).onChange(task);

        // should succeed since FIRMWARES[0] already has local URI
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.SUCCESS),
                firmwareDownloaderTaskRemainIs(Collections.emptySet()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(100),
                firmwareDownloaderTaskOverallProgressIs(100)));
    }

    @Test
    public void testTaskDownloadsIfInternetAndRemoteUri() {
        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task, notNullValue());

        // observer should have been called only once
        verify(mObserver, times(1)).onChange(task);

        // should be downloading since FIRMWARES[0] has remote URI and internet is available
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));
    }

    @Test
    public void testTaskQueuedIfAnotherIsDownloading() {
        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        ENTRIES[1].setUri(URI.create("https://server/firmware2"));

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);
        doReturn(ENTRIES[1]).when(mStore).getEntry(FIRMWARES[1]);

        // queue a first task
        mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        // queue a second task
        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[1]), mObserver);

        assertThat(task, notNullValue());

        // observer should have been called only once
        verify(mObserver, times(1)).onChange(task);

        // should be queued since FIRMWARES[0] is being downloaded currently
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.QUEUED),
                firmwareDownloaderTaskRemainIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));
    }

    @Test
    public void testDownloadProgress() {
        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        assert ENTRIES[0].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task, notNullValue());

        clearInvocations(mObserver);

        verify(mHttpClient).download(eq(ENTRIES[0].getRemoteUri().toString()), any(), mHttpCallback.capture());
        // mock progress
        mHttpCallback.getValue().onRequestProgress(20);

        // observer should have been called
        verify(mObserver, times(1)).onChange(task);

        // task progress should have updated
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(20),
                firmwareDownloaderTaskOverallProgressIs(20)));

        // mock further progress
        mHttpCallback.getValue().onRequestProgress(50);

        // observer should have been called again
        verify(mObserver, times(2)).onChange(task);

        // task progress should have updated again
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(50),
                firmwareDownloaderTaskOverallProgressIs(50)));
    }

    @Test
    public void testDownloadSuccess() {
        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        assert ENTRIES[0].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task, notNullValue());

        clearInvocations(mObserver);

        verify(mHttpClient).download(eq(ENTRIES[0].getRemoteUri().toString()), any(), mHttpCallback.capture());

        // mock success
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // observer should have been called
        verify(mObserver, times(1)).onChange(task);

        // task should be successful
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.SUCCESS),
                firmwareDownloaderTaskRemainIs(Collections.emptySet()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(100),
                firmwareDownloaderTaskOverallProgressIs(100)));
    }

    @Test
    public void testDownloadFailure() {
        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        assert ENTRIES[0].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task, notNullValue());

        clearInvocations(mObserver);

        verify(mHttpClient).download(eq(ENTRIES[0].getRemoteUri().toString()), any(), mHttpCallback.capture());

        // mock some progress first
        mHttpCallback.getValue().onRequestProgress(75);

        clearInvocations(mObserver);

        // mock failure
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        // observer should have been called
        verify(mObserver, times(1)).onChange(task);

        // task should be failed
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(75),
                firmwareDownloaderTaskOverallProgressIs(75)));
    }

    @Test
    public void testDownloadCancel() {
        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        assert ENTRIES[0].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task, notNullValue());

        clearInvocations(mObserver);

        verify(mHttpClient).download(eq(ENTRIES[0].getRemoteUri().toString()), any(), mHttpCallback.capture());

        // mock some progress first
        mHttpCallback.getValue().onRequestProgress(75);

        clearInvocations(mObserver);

        // mock cancel
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);

        // observer should have been called
        verify(mObserver, times(1)).onChange(task);

        // task should be canceled
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.CANCELED),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(75),
                firmwareDownloaderTaskOverallProgressIs(75)));
    }

    @Test
    public void testTaskCancelWhenQueued() {
        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        ENTRIES[1].setUri(URI.create("https://server/firmware2"));

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);
        doReturn(ENTRIES[1]).when(mStore).getEntry(FIRMWARES[1]);

        // queue a first task
        mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        // queue a second task
        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[1]), mObserver);

        assertThat(task, notNullValue());

        clearInvocations(mObserver);

        // cancel task
        task.cancel();

        // observer should have been called
        verify(mObserver, times(1)).onChange(task);

        // task should be canceled
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.CANCELED),
                firmwareDownloaderTaskRemainIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));
    }

    @Test
    public void testTaskCancelWhenDownloading() {
        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        assert ENTRIES[0].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        HttpRequest request = mock(HttpRequest.class);

        doReturn(request).when(mHttpClient)
                         .download(eq(ENTRIES[0].getRemoteUri().toString()), any(), mHttpCallback.capture());

        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task, notNullValue());

        // mock some progress first
        mHttpCallback.getValue().onRequestProgress(75);

        clearInvocations(mObserver);

        // cancel task
        task.cancel();

        // http request should have been canceled
        verify(request).cancel();

        // observer should have been called
        verify(mObserver, times(1)).onChange(task);

        // task should be canceled
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.CANCELED),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(75),
                firmwareDownloaderTaskOverallProgressIs(75)));

        // mock http request cancel
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);

        // task should not change any further
        verify(mObserver, times(1)).onChange(task);

        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.CANCELED),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(75),
                firmwareDownloaderTaskOverallProgressIs(75)));
    }

    @Test
    public void testTaskCancelAfterFailed() {
        // mock internet unavailable / no remote uri, so that task fails immediately
        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task, notNullValue());

        clearInvocations(mObserver);

        // cancel task
        task.cancel();

        // observer should not be called
        verifyZeroInteractions(mObserver);

        // state should remain failed
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));
    }

    @Test
    public void testTaskCancelAfterSuccess() {
        // mock local uri, so that task succeeds immediately
        ENTRIES[0].setUri(URI.create("file://firmware1"));
        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        FirmwareDownloader.Task task = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task, notNullValue());

        clearInvocations(mObserver);

        // cancel task
        task.cancel();

        // observer should not be called
        verifyZeroInteractions(mObserver);

        // state should remain success
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.SUCCESS),
                firmwareDownloaderTaskRemainIs(Collections.emptySet()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(100),
                firmwareDownloaderTaskOverallProgressIs(100)));
    }

    @Test
    public void testTaskCancelDoesNotCancelSiblingTask() {
        // by 'sibling task', we mean a task that is currently downloading the same firmware has another

        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        assert ENTRIES[0].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        HttpRequest request = mock(HttpRequest.class);

        doReturn(request).when(mHttpClient)
                         .download(eq(ENTRIES[0].getRemoteUri().toString()), any(), mHttpCallback.capture());

        // launch a first download task
        FirmwareDownloader.Task task1 = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task1, notNullValue());

        // task should be downloading
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // launch a second task  (we use same observer in the test but it does not matter
        FirmwareDownloader.Task task2 = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task2, notNullValue());

        // task should also be downloading
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        clearInvocations(mObserver);

        // cancel first task
        task1.cancel();

        // http request should not have been canceled
        verifyZeroInteractions(request);

        // observer should have been called for task 1
        verify(mObserver, times(1)).onChange(task1);

        // task 1 should be canceled
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.CANCELED),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // observer should not have been called for task 2
        verify(mObserver, never()).onChange(task2);

        // task 2 should not have changed
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));
    }

    @Test
    public void testSiblingTaskProceedsWhenDownloadCompletes() {
        // by 'sibling task', we mean a task that is currently downloading the same firmware has another (but in the
        // present case, that did not 'initiate' the download).
        // tests that when a download initiated by a first task completes, a sibling tasks proceeds with next firmware

        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        ENTRIES[1].setUri(URI.create("https://server/firmware2"));
        assert ENTRIES[0].getRemoteUri() != null;
        assert ENTRIES[1].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);
        doReturn(ENTRIES[1]).when(mStore).getEntry(FIRMWARES[1]);

        HttpRequest request = mock(HttpRequest.class);

        doReturn(request).when(mHttpClient)
                         .download(eq(ENTRIES[0].getRemoteUri().toString()), any(), mHttpCallback.capture());
        doReturn(request).when(mHttpClient).download(eq(ENTRIES[1].getRemoteUri().toString()), any(), any());

        // launch a first download task
        FirmwareDownloader.Task task1 = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task1, notNullValue());

        // mock progress on task 1
        mHttpCallback.getValue().onRequestProgress(50);

        // launch a second download task with first same firmware as task 1 and another firmware next
        FirmwareDownloader.Task task2 = mDownloader.downloadFromIds(
                new LinkedHashSet<>(Arrays.asList(FIRMWARES[0], FIRMWARES[1])), mObserver);

        assertThat(task2, notNullValue());

        // task 2 observer should be notified
        verify(mObserver).onChange(task2);

        // task 2 should be downloading
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo(), ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(2),
                firmwareDownloaderTaskCurrentDownloadProgressIs(50),
                firmwareDownloaderTaskOverallProgressIs(20)));

        // mock completion of firmware 1 download
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // second firmware download should start
        verify(mHttpClient).download(eq(ENTRIES[1].getRemoteUri().toString()), any(), any());

        // task 2 observer should be notified
        verify(mObserver, times(2)).onChange(task2);

        // task 2 should be downloading firmware 2 now
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(2),
                firmwareDownloaderTaskTotalCountIs(2),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(40)));
    }

    @Test
    public void testSiblingTaskCurrentProgress() {
        // test that, when created, a sibling task gets proper current progress immediately

        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        assert ENTRIES[0].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        HttpRequest request = mock(HttpRequest.class);

        doReturn(request).when(mHttpClient)
                         .download(eq(ENTRIES[0].getRemoteUri().toString()), any(), mHttpCallback.capture());

        // launch a first download task
        FirmwareDownloader.Task task1 = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task1, notNullValue());

        // mock some progress
        mHttpCallback.getValue().onRequestProgress(30);

        // task should be downloading
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(30),
                firmwareDownloaderTaskOverallProgressIs(30)));

        clearInvocations(mObserver);

        // launch a second task  (we use same observer in the test but it does not matter)
        FirmwareDownloader.Task task2 = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task2, notNullValue());

        // observer for second task should have been called
        verify(mObserver, times(1)).onChange(task2);

        // second task should be downloading we same progress as first task
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(30),
                firmwareDownloaderTaskOverallProgressIs(30)));

        // mock progress again
        mHttpCallback.getValue().onRequestProgress(60);

        // both observer should have been called
        verify(mObserver, times(1)).onChange(task1);
        verify(mObserver, times(2)).onChange(task2);

        // both task should have same state, with updated progress
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(60),
                firmwareDownloaderTaskOverallProgressIs(60)));

        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(60),
                firmwareDownloaderTaskOverallProgressIs(60)));
    }

    @Test
    public void testMultiFirmwareTaskSuccess() {
        // test a regular download scenario for a multi firmware task, where everything goes well.
        // also test that already downloaded firmwares are not downloaded again and that the task proceeds normally.

        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        ENTRIES[1].setUri(URI.create("file://firmware2")); // second firmware is already downloaded
        ENTRIES[2].setUri(URI.create("https://server/firmware3"));
        assert ENTRIES[0].getRemoteUri() != null;
        assert ENTRIES[2].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);
        doReturn(ENTRIES[1]).when(mStore).getEntry(FIRMWARES[1]);
        doReturn(ENTRIES[2]).when(mStore).getEntry(FIRMWARES[2]);

        HttpRequest request = mock(HttpRequest.class);

        doReturn(request).when(mHttpClient).download(any(), any(), mHttpCallback.capture());

        // launch  download task with multiple firmwares
        FirmwareDownloader.Task task = mDownloader.downloadFromIds(
                new LinkedHashSet<>(Arrays.asList(FIRMWARES)), mObserver);

        assertThat(task, notNullValue());

        // first firmware download should start
        verify(mHttpClient).download(eq(ENTRIES[0].getRemoteUri().toString()), any(), any());

        // task observer should be notified
        verify(mObserver).onChange(task);

        // task should be downloading first firmware
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // mock some progress
        mHttpCallback.getValue().onRequestProgress(50);

        // task observer should be notified
        verify(mObserver, times(2)).onChange(task);

        // task progress should update
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(50),
                firmwareDownloaderTaskOverallProgressIs(10)));

        // mock download success
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // third firmware download should start (since second is already local)
        verify(mHttpClient).download(eq(ENTRIES[2].getRemoteUri().toString()), any(), any());

        // task observer should be notified
        verify(mObserver, times(3)).onChange(task);

        // task should be downloading third firmware
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(50)));

        // mock some progress
        mHttpCallback.getValue().onRequestProgress(50);

        // task observer should be notified
        verify(mObserver, times(4)).onChange(task);

        // task progress should update
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(50),
                firmwareDownloaderTaskOverallProgressIs(75)));

        // mock download success
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // task observer should be notified
        verify(mObserver, times(5)).onChange(task);

        // task should be successful
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.SUCCESS),
                firmwareDownloaderTaskRemainIs(Collections.emptySet()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(100),
                firmwareDownloaderTaskOverallProgressIs(100)));
    }

    @Test
    public void testMultiFirmwareTaskFailure() {
        // test a download scenario for a multi firmware task, where some download in the middle of the task fails.

        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        ENTRIES[1].setUri(URI.create("https://server/firmware2"));
        ENTRIES[2].setUri(URI.create("https://server/firmware3"));
        assert ENTRIES[0].getRemoteUri() != null;
        assert ENTRIES[1].getRemoteUri() != null;
        assert ENTRIES[2].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);
        doReturn(ENTRIES[1]).when(mStore).getEntry(FIRMWARES[1]);
        doReturn(ENTRIES[2]).when(mStore).getEntry(FIRMWARES[2]);

        HttpRequest request = mock(HttpRequest.class);

        doReturn(request).when(mHttpClient).download(any(), any(), mHttpCallback.capture());

        // launch  download task with multiple firmwares
        FirmwareDownloader.Task task = mDownloader.downloadFromIds(
                new LinkedHashSet<>(Arrays.asList(FIRMWARES)), mObserver);

        assertThat(task, notNullValue());

        // first firmware download should start
        verify(mHttpClient).download(eq(ENTRIES[0].getRemoteUri().toString()), any(), any());

        // task observer should be notified
        verify(mObserver).onChange(task);

        // task should be downloading first firmware
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // mock download success
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // second firmware download should start
        verify(mHttpClient).download(eq(ENTRIES[1].getRemoteUri().toString()), any(), any());

        // task observer should be notified
        verify(mObserver, times(2)).onChange(task);

        // task should be downloading second firmware
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(2),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(20)));

        // mock some progress
        mHttpCallback.getValue().onRequestProgress(50);

        // task observer should be notified
        verify(mObserver, times(3)).onChange(task);

        // task progress should update
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(2),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(50),
                firmwareDownloaderTaskOverallProgressIs(35)));

        // mock task failure
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        // task observer should be notified
        verify(mObserver, times(4)).onChange(task);

        // task should be failed
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(2),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(50),
                firmwareDownloaderTaskOverallProgressIs(35)));

        // third firmware should not be downloaded
        verify(mHttpClient, never()).download(eq(ENTRIES[2].getRemoteUri().toString()), any(), any());
    }

    @Test
    public void testMultiFirmwareTaskCancel() {
        // test a download scenario for a multi firmware task, where client aborts in the middle of the task.

        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        ENTRIES[1].setUri(URI.create("https://server/firmware2"));
        ENTRIES[2].setUri(URI.create("https://server/firmware3"));
        assert ENTRIES[0].getRemoteUri() != null;
        assert ENTRIES[1].getRemoteUri() != null;
        assert ENTRIES[2].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);
        doReturn(ENTRIES[1]).when(mStore).getEntry(FIRMWARES[1]);
        doReturn(ENTRIES[2]).when(mStore).getEntry(FIRMWARES[2]);

        HttpRequest request = mock(HttpRequest.class);

        doReturn(request).when(mHttpClient).download(any(), any(), mHttpCallback.capture());

        // launch  download task with multiple firmwares
        FirmwareDownloader.Task task = mDownloader.downloadFromIds(
                new LinkedHashSet<>(Arrays.asList(FIRMWARES)), mObserver);

        assertThat(task, notNullValue());

        // first firmware download should start
        verify(mHttpClient).download(eq(ENTRIES[0].getRemoteUri().toString()), any(), any());

        // task observer should be notified
        verify(mObserver).onChange(task);

        // task should be downloading first firmware
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // mock download success
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // second firmware download should start
        verify(mHttpClient).download(eq(ENTRIES[1].getRemoteUri().toString()), any(), any());

        // task observer should be notified
        verify(mObserver, times(2)).onChange(task);

        // task should be downloading second firmware
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(2),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(20)));

        // mock some progress
        mHttpCallback.getValue().onRequestProgress(50);

        // task observer should be notified
        verify(mObserver, times(3)).onChange(task);

        // task progress should update
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(2),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(50),
                firmwareDownloaderTaskOverallProgressIs(35)));

        // cancel task
        task.cancel();

        // http request should have been canceled
        verify(request).cancel();

        // task observer should be notified
        verify(mObserver, times(4)).onChange(task);

        // task should be canceled
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.CANCELED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(2),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(50),
                firmwareDownloaderTaskOverallProgressIs(35)));

        // mock http request cancel
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);

        // third firmware should not be downloaded
        verify(mHttpClient, never()).download(eq(ENTRIES[2].getRemoteUri().toString()), any(), any());

        // task should not change any further
        verify(mObserver, times(4)).onChange(task);
        assertThat(task, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.CANCELED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(2),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(50),
                firmwareDownloaderTaskOverallProgressIs(35)));
    }

    @Test
    public void testInternetGoesUnavailable() {
        // test that, when internet goes down, all tasks fail, and current download stops.

        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        ENTRIES[1].setUri(URI.create("https://server/firmware2"));
        assert ENTRIES[0].getRemoteUri() != null;
        assert ENTRIES[1].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);
        doReturn(ENTRIES[1]).when(mStore).getEntry(FIRMWARES[1]);

        HttpRequest request = mock(HttpRequest.class);

        doReturn(request).when(mHttpClient)
                         .download(eq(ENTRIES[0].getRemoteUri().toString()), any(), mHttpCallback.capture());

        // launch a first task
        FirmwareDownloader.Task task1 = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        assertThat(task1, notNullValue());

        // observer for task 1 should be notified
        verify(mObserver).onChange(task1);

        // task should be downloading
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // launch a second task, different firmware
        FirmwareDownloader.Task task2 = mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[1]), mObserver);

        assertThat(task2, notNullValue());

        // observer for task 2 should be notified
        verify(mObserver).onChange(task2);

        // task should be queued
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.QUEUED),
                firmwareDownloaderTaskRemainIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // mock internet unavailable
        doReturn(null).when(mEngine).httpClient();
        // this mocks HttpClient.dispose() when internet goes unavailable
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.CANCELED, HttpRequest.STATUS_CODE_UNKNOWN);

        // observer for task 1 should be notified
        verify(mObserver, times(2)).onChange(task1);

        // task 1 should be canceled
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.CANCELED),
                firmwareDownloaderTaskRemainIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // second firmware download should not start
        verify(mHttpClient, never()).download(eq(ENTRIES[1].getRemoteUri().toString()), any(), any());

        // observer for task2 should be notified
        verify(mObserver, times(2)).onChange(task2);

        // task 2 should be failed (no internet when processed from queue => failure)
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(1),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));
    }

    @Test
    public void testSuccessfulDownloadedFirmwareIsAddedToStore() {
        // test that, after a firmware is successfully downloaded, its local uri is added in the store.

        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        assert ENTRIES[0].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);

        HttpRequest request = mock(HttpRequest.class);

        doReturn(request).when(mHttpClient)
                         .download(eq(ENTRIES[0].getRemoteUri().toString()), any(), mHttpCallback.capture());

        // launch a download task
        mDownloader.downloadFromIds(Collections.singleton(FIRMWARES[0]), mObserver);

        // mock successful download
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(mStore).addLocalFirmware(eq(FIRMWARES[0]), uriCaptor.capture());

        assertThat(ENTRIES[0].getLocalUri(), is(uriCaptor.getValue()));
    }

    @Test
    public void testMultiTasksMultiFirmwares() {
        // a complex test case with multiple tasks each downloading multiple firmwares

        // task 1 will download firmware 1, then 2, then 3
        // task 2 will download firmware 3, which will fail, (then 2, then 1, which won't be processed for this task)
        // task 3 will download firmware 2, then 1, then 3, where task will be canceled

        // all tasks are registered simultaneously.

        // firmwares should be downloaded in the following order:
        // 1. firmware 1 (queued by  task 1)
        // 2. firmware 3 (queued by task 2 while firmware 1 was being processed)
        // 3. firmware 2 (queued by task 3 while firmware 1 was being processed)
        // 4. firmware 3 (queued by task 1 while firmware 2 was being processed)

        // mock both internet and remote uri
        doReturn(mHttpClient).when(mEngine).httpClient();
        ENTRIES[0].setUri(URI.create("https://server/firmware1"));
        ENTRIES[1].setUri(URI.create("https://server/firmware2"));
        ENTRIES[2].setUri(URI.create("https://server/firmware3"));
        assert ENTRIES[0].getRemoteUri() != null;
        assert ENTRIES[1].getRemoteUri() != null;
        assert ENTRIES[2].getRemoteUri() != null;

        doReturn(ENTRIES[0]).when(mStore).getEntry(FIRMWARES[0]);
        doReturn(ENTRIES[1]).when(mStore).getEntry(FIRMWARES[1]);
        doReturn(ENTRIES[2]).when(mStore).getEntry(FIRMWARES[2]);

        HttpRequest request = mock(HttpRequest.class);

        doReturn(request).when(mHttpClient).download(any(), any(), mHttpCallback.capture());

        // launch tasks
        FirmwareDownloader.Task task1 = mDownloader.downloadFromIds(new LinkedHashSet<>(Arrays.asList(
                FIRMWARES[0], FIRMWARES[1], FIRMWARES[2])), mObserver);

        assertThat(task1, notNullValue());

        FirmwareDownloader.Task task2 = mDownloader.downloadFromIds(new LinkedHashSet<>(Arrays.asList(
                FIRMWARES[2], FIRMWARES[1], FIRMWARES[0])), mObserver);

        assertThat(task2, notNullValue());

        FirmwareDownloader.Task task3 = mDownloader.downloadFromIds(new LinkedHashSet<>(Arrays.asList(
                FIRMWARES[1], FIRMWARES[0], FIRMWARES[2])), mObserver);

        assertThat(task3, notNullValue());

        // first firmware download should start
        verify(mHttpClient).download(eq(ENTRIES[0].getRemoteUri().toString()), any(), any());

        // all task observers should be notified
        verify(mObserver, times(1)).onChange(task1);
        verify(mObserver, times(1)).onChange(task2);
        verify(mObserver, times(1)).onChange(task3);

        // task 1 should be downloading first firmware
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // task 2 should be queued on firmware 3
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.QUEUED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[2].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // task 3 should be queued on firmware 2
        assertThat(task3, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.QUEUED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // mock firmware 1 download completion
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // third firmware download should start
        verify(mHttpClient).download(eq(ENTRIES[2].getRemoteUri().toString()), any(), any());

        // task 1 observer should be notified
        verify(mObserver, times(2)).onChange(task1);

        // task 1 should be queued on second firmware
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.QUEUED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(2),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(20)));

        // task 2 observer should be notified
        verify(mObserver, times(2)).onChange(task2);

        // task 2 should be downloading third firmware
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[2].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // task 3 observer should not be notified
        verify(mObserver, times(1)).onChange(task3);

        // task 3 should not have changed
        assertThat(task3, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.QUEUED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // mock firmware 3 download failure
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.FAILED, 500);

        // second firmware download should start
        verify(mHttpClient).download(eq(ENTRIES[1].getRemoteUri().toString()), any(), any());

        // task 1 observer should be notified
        verify(mObserver, times(3)).onChange(task1);

        // task 1 should be downloading second firmware
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(2),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(20)));

        // task 2 observer should be notified
        verify(mObserver, times(3)).onChange(task2);

        // task 2 should be failed
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[2].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // task 3 observer should be notified
        verify(mObserver, times(2)).onChange(task3);

        // task 3 should be downloading second firmware
        assertThat(task3, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // mock progress on second firmware download
        mHttpCallback.getValue().onRequestProgress(50);

        // task 1 observer should be notified
        verify(mObserver, times(4)).onChange(task1);

        // task 1 progress should update
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(2),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(50),
                firmwareDownloaderTaskOverallProgressIs(35)));

        // task 2 observer should not be notified
        verify(mObserver, times(3)).onChange(task2);

        // task 2 should remain failed
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[2].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // task 3 observer should be notified
        verify(mObserver, times(3)).onChange(task3);

        // task 3 progress should update
        assertThat(task3, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(50),
                firmwareDownloaderTaskOverallProgressIs(15)));

        // mock firmware 2 download completion
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // third firmware download should start (again)
        verify(mHttpClient, times(2)).download(eq(ENTRIES[2].getRemoteUri().toString()), any(), any());

        // task 1 observer should be notified
        verify(mObserver, times(5)).onChange(task1);

        // task 1 should be downloading firmware 3
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(50)));

        // task 2 observer should not be notified
        verify(mObserver, times(3)).onChange(task2);

        // task 2 should remain failed
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[2].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // task 3 observer should be notified
        verify(mObserver, times(4)).onChange(task3);

        // task 3 should be downloading firmware 3 (and have firmware 1 downloaded, since downloaded previously)
        assertThat(task3, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(50)));

        // mock progress on third firmware download
        mHttpCallback.getValue().onRequestProgress(20);

        // task 1 observer should be notified
        verify(mObserver, times(6)).onChange(task1);

        // task 1 progress should update
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(20),
                firmwareDownloaderTaskOverallProgressIs(60)));

        // task 2 observer should not be notified
        verify(mObserver, times(3)).onChange(task2);

        // task 2 should remain failed
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[2].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // task 3 observer should be notified
        verify(mObserver, times(5)).onChange(task3);

        // task 3 progress should update
        assertThat(task3, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(20),
                firmwareDownloaderTaskOverallProgressIs(60)));

        // cancel task 3
        task3.cancel();

        // download should not be canceled, since task 1 still needs it
        verify(request, never()).cancel();

        // task 1 observer should not be notified
        verify(mObserver, times(6)).onChange(task1);

        // task 1 should remain downloading
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(20),
                firmwareDownloaderTaskOverallProgressIs(60)));

        // task 2 observer should not be notified
        verify(mObserver, times(3)).onChange(task2);

        // task 2 should remain failed
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[2].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // task 3 observer should be notified
        verify(mObserver, times(6)).onChange(task3);

        // task 3 should be canceled
        assertThat(task3, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.CANCELED),
                firmwareDownloaderTaskRemainIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(20),
                firmwareDownloaderTaskOverallProgressIs(60)));

        // mock progress on third firmware download
        mHttpCallback.getValue().onRequestProgress(80);

        // task 1 observer should be notified
        verify(mObserver, times(7)).onChange(task1);

        // task 1 progress should update
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.DOWNLOADING),
                firmwareDownloaderTaskRemainIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(80),
                firmwareDownloaderTaskOverallProgressIs(90)));

        // task 2 observer should not be notified
        verify(mObserver, times(3)).onChange(task2);

        // task 2 should remain failed
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[2].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // task 3 observer should not be notified
        verify(mObserver, times(6)).onChange(task3);

        // task 3 should remain canceled
        assertThat(task3, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.CANCELED),
                firmwareDownloaderTaskRemainIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(20),
                firmwareDownloaderTaskOverallProgressIs(60)));

        // mock third firmware download success
        mHttpCallback.getValue().onRequestComplete(HttpRequest.Status.SUCCESS, 200);

        // task 1 observer should be notified
        verify(mObserver, times(8)).onChange(task1);

        // task 1 should be successful
        assertThat(task1, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.SUCCESS),
                firmwareDownloaderTaskRemainIs(Collections.emptySet()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[0].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(100),
                firmwareDownloaderTaskOverallProgressIs(100)));

        // task 2 observer should not be notified
        verify(mObserver, times(3)).onChange(task2);

        // task 2 should remain failed
        assertThat(task2, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.FAILED),
                firmwareDownloaderTaskRemainIs(
                        ENTRIES[2].getFirmwareInfo(),
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(Collections.emptySet()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(1),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(0),
                firmwareDownloaderTaskOverallProgressIs(0)));

        // task 3 observer should not be notified
        verify(mObserver, times(6)).onChange(task3);

        // task 3 should remain canceled
        assertThat(task3, allOf(
                firmwareDownloaderTaskStateIs(FirmwareDownloader.Task.State.CANCELED),
                firmwareDownloaderTaskRemainIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskDownloaded(
                        ENTRIES[1].getFirmwareInfo(),
                        ENTRIES[0].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentIs(ENTRIES[2].getFirmwareInfo()),
                firmwareDownloaderTaskCurrentCountIs(3),
                firmwareDownloaderTaskTotalCountIs(3),
                firmwareDownloaderTaskCurrentDownloadProgressIs(20),
                firmwareDownloaderTaskOverallProgressIs(60)));
    }
}
